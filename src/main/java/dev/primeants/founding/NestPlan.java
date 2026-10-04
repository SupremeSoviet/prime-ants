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
    public List<BlockPos> deposits() { return List.of(at(-3, 0, 1), at(-2, -1, 1), at(-2, 1, 1)); }
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
}
