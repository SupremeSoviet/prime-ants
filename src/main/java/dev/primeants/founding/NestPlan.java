package dev.primeants.founding;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Describes actual work targets, never places geometry. Coordinates are bounded relative to the entrance. */
public record NestPlan(BlockPos entrance, Direction direction, List<BlockPos> tasks, List<BlockState> expected) {
    public static final int HARD_CAP = 24; // below the 48-block contract maximum
    public static final int MAX_RADIUS = 5, MAX_DEPTH = 3;
    public BlockPos at(int forward, int side, int dy) {
        return entrance.relative(direction, forward).relative(direction.getClockWise(), side).offset(0, dy, 0);
    }
    public BlockPos chamber() { return at(4, 0, -2); }
    public BlockPos outside() { return at(-2, 0, 1); }
    /** Bounded 40-cell area for 22 units: five exterior rows, four cells to either side; central route stays clear. */
    public List<BlockPos> deposits() {
        List<BlockPos> result = new ArrayList<>();
        for (int f = -5; f <= -1; f++) for (int s : new int[]{-4, 4, -3, 3, -2, 2, -1, 1}) result.add(at(f, s, 1));
        return List.copyOf(result);
    }
    public BlockPos nursery() { return at(4, 1, -2); }
    public BlockPos cache() { return at(3, -1, -2); }
    public static boolean loaded(ServerLevel level, BlockPos p) {
        return level.getChunkSource().getChunk(p.getX() >> 4, p.getZ() >> 4,
                net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) != null;
    }
    public List<BlockPos> undergroundSurfaces() {
        LinkedHashSet<BlockPos> result = new LinkedHashSet<>();
        for (BlockPos p : tasks) for (Direction d : Direction.values()) {
            BlockPos n = p.relative(d);
            if (n.getY() < entrance.getY() && !tasks.contains(n)) result.add(n);
        }
        return List.copyOf(result);
    }
    public List<BlockPos> plugs() { return List.of(at(2, 0, -2), at(2, 0, -1)); }
    public static NestPlan candidate(ServerLevel level, BlockPos entrance, Direction direction) {
        NestPlan geometry = geometry(entrance, direction);
        NaturalSoil soil = NaturalSoil.get(level);
        for (BlockPos p : geometry.tasks) if (!soil.eligible(level, p)) return null;
        // Natural roof and stable native floors; no fluids/trees/falling or unknown supports.
        for (int f = 0; f < 3; f++) if (!soil.eligible(level, geometry.at(f, 0, -f - 1))) return null;
        for (int f = 3; f <= 5; f++) for (int s = -1; s <= 1; s++) {
            if (!soil.eligible(level, geometry.at(f, s, -3)) || !soil.eligible(level, geometry.at(f, s, 0))) return null;
        }
        for (int f = 3; f <= 5; f++) for (int side = -1; side <= 1; side++) for (int dy = -2; dy <= -1; dy++) {
            BlockPos p = geometry.at(f, side, dy);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!geometry.tasks.contains(n) && !soil.eligible(level, n)) return null;
            }
        }
        for (int f = -3; f <= -1; f++) for (int s = -1; s <= 1; s++) {
            BlockPos p = geometry.at(f, s, 1);
            if (!soil.eligible(level, p.below()) || !walkable(level, p)) return null;
        }
        if (geometry.deposits().stream().filter(p -> soil.eligible(level, p.below()) && walkable(level, p)).count() < HARD_CAP - 2) return null;
        // Avoid leaking into pre-existing cavities or liquid adjacent to the envelope.
        for (BlockPos p : geometry.tasks) for (Direction d : Direction.values()) {
            BlockPos n = p.relative(d);
            if (!level.getFluidState(n).isEmpty()) return null;
        }
        return new NestPlan(entrance, direction, geometry.tasks,
                geometry.tasks.stream().map(level::getBlockState).toList());
    }
    public static NestPlan geometry(BlockPos e, Direction d) {
        NestPlan p = new NestPlan(e.immutable(), d, List.of(), List.of());
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
        for (int f = 0; f < 3; f++) for (int y = 0; y >= -f; y--) targets.add(p.at(f, 0, y));
        // Carve central row first so every side cell has an exposed working face.
        for (int s : new int[]{0, -1, 1}) for (int f = 3; f <= 5; f++)
            for (int y = -1; y >= -2; y--) targets.add(p.at(f, s, y));
        if (targets.size() > HARD_CAP) throw new IllegalStateException("Excavation cap exceeded");
        return new NestPlan(e.immutable(), d, List.copyOf(targets), List.of());
    }
    public static boolean walkable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.below()).isSolidRender()
                && level.getFluidState(feet.below()).isEmpty();
    }
    public boolean enclosedChamber(ServerLevel level) {
        for (int f=3; f<=5; f++) for (int side=-1;side<=1;side++) for(int dy=-2;dy<=-1;dy++) {
            BlockPos p=at(f,side,dy);
            for(Direction d:Direction.values()) {
                BlockPos n=p.relative(d);
                if (!tasks.contains(n) && !level.getBlockState(n).isSolidRender()) return false;
            }
        }
        return true;
    }
    public boolean openWalkable(ServerLevel level) {
        for (int f = 0; f < 3; f++) if (!walkable(level, at(f, 0, -f))) return false;
        for (int f = 3; f <= 5; f++) for (int s = -1; s <= 1; s++) if (!walkable(level, at(f, s, -2))) return false;
        return true;
    }
    /** Live habitat checks shared by founding and brood, even when the queen is absent. */
    public String nurseryProblem(ServerLevel level, java.util.UUID owner) {
        return nurseryProblem(level, owner, false);
    }
    public String nurseryProblem(ServerLevel level, java.util.UUID owner, boolean operational) {
        for (int f : new int[]{2, 6}) for (int s : new int[]{-2, 2}) {
            BlockPos p = at(f, s, -3);
            if (level.getChunkSource().getChunk(p.getX() >> 4, p.getZ() >> 4,
                    net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) == null) return "enclosure_chunk_unavailable";
        }
        if (plugs().stream().anyMatch(p -> !(operational && ColonyPlugs.get(level).opened(level,p,owner))
                && !level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.DIRT))) return "enclosure_plug_missing";
        if (operational) {
            if (plugs().stream().anyMatch(p -> !ColonyPlugs.get(level).opened(level,p,owner) && !ColonyPlugs.get(level).owned(level,p,owner))) return "enclosure_plug_ownership_revoked";
            for (int f=0;f<2;f++) if (!loaded(level,at(f,0,-f)) || !walkable(level,at(f,0,-f))) return "enclosure_route_obstructed";
            if (plugs().stream().allMatch(p -> ColonyPlugs.get(level).opened(level,p,owner)) && !walkable(level,at(2,0,-2))) return "enclosure_route_obstructed";
            // The recorded plugs are the only new opening. Retain the underground corridor's support/shell too.
            for (BlockPos p : undergroundSurfaces()) {
                if (!loaded(level,p)) return "enclosure_chunk_unavailable";
                if (!level.getBlockState(p).isSolidRender() || !level.getFluidState(p).isEmpty()) return "enclosure_shell_open";
            }
        }
        if (!enclosedChamber(level)) return "enclosure_shell_open";
        for (int f = 3; f <= 5; f++) for (int s = -1; s <= 1; s++) {
            BlockPos p = at(f, s, -2);
            boolean owned = p.equals(nursery()) && level.getBlockEntity(p) instanceof dev.primeants.brood.BroodPile pile
                    && pile.ownedBy(owner, this);
            owned |= p.equals(cache()) && level.getBlockEntity(p) instanceof dev.primeants.worker.NestCache cache && cache.ownedBy(owner, this);
            if ((!level.getBlockState(p).isAir() && !owned) || !level.getBlockState(p.above()).isAir()
                    || !level.getBlockState(p.below()).isSolidRender() || !level.getFluidState(p).isEmpty()
                    || !level.getFluidState(p.below()).isEmpty()) return "enclosure_chamber_obstructed";
        }
        return null;
    }
}
