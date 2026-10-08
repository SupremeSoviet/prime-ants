package dev.primeants.founding;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Describes actual work targets, never places geometry. Coordinates are bounded relative to the entrance. */
public record NestPlan(BlockPos entrance, Direction direction, List<BlockPos> tasks, List<BlockState> expected,
        List<BlockPos> plants, List<BlockState> plantExpected, List<BlockPos> surfaceDeposits, BlockPos exteriorStand) {
    public NestPlan(BlockPos entrance, Direction direction, List<BlockPos> tasks, List<BlockState> expected) {
        this(entrance,direction,tasks,expected,List.of(),List.of(),List.of(),null);
    }
    public static final int HARD_CAP = 24; // below the 48-block contract maximum
    public static final int MAX_RADIUS = 5, MAX_DEPTH = 3;
    public BlockPos at(int forward, int side, int dy) {
        return entrance.relative(direction, forward).relative(direction.getClockWise(), side).offset(0, dy, 0);
    }
    public BlockPos chamber() { return at(4, 0, -2); }
    public BlockPos outside() { return exteriorStand == null ? at(-2, 0, 1) : exteriorStand; }
    /** Bounded 40-cell area for 22 units: five exterior rows, four cells to either side; central route stays clear. */
    public List<BlockPos> deposits() {
        if (!surfaceDeposits.isEmpty()) return surfaceDeposits;
        List<BlockPos> result = new ArrayList<>();
        for (int f = -5; f <= -1; f++) for (int s : new int[]{-4, 4, -3, 3, -2, 2, -1, 1}) result.add(at(f, s, 1));
        return List.copyOf(result);
    }
    /** Worker routes retain the queen's declared exterior heights without acquiring plant/soil authority. */
    public NestPlan routeGeometry() {
        var g=geometry(entrance,direction);
        return new NestPlan(entrance,direction,g.tasks(),g.expected(),List.of(),List.of(),surfaceDeposits,exteriorStand);
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
    /** WORLD_SURFACE includes short plants. Descend at most two cells, never through trees/crops. */
    public static BlockPos soilSurface(ServerLevel level, BlockPos column) {
        int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,column.getX(),column.getZ())-1;
        var p=column.atY(y);
        for(int i=0;i<2 && NativeVegetation.material(level.getBlockState(p));i++) p=p.below();
        return p;
    }
    public static boolean traversable(ServerLevel level, BlockPos p) {
        var state=level.getBlockState(p);
        // Dry collision-free decorations occupy no walking space. Block entities still require
        // their canonical ownership exception; this grants no removal/soil permission.
        return level.getFluidState(p).isEmpty() && !state.hasBlockEntity()
            && state.getCollisionShape(level,p).isEmpty();
    }
    private static BlockPos exteriorFeet(ServerLevel level, BlockPos original) {
        for(int dy=1;dy>=-1;dy--) {
            var p=original.offset(0,dy,0);
            if (loaded(level,p) && NaturalSoil.get(level).eligible(level,p.below()) && walkable(level,p)) return p;
        }
        return null;
    }
    public static NestPlan candidate(ServerLevel level, BlockPos entrance, Direction direction) {
        NestPlan geometry=geometry(entrance,direction);var soil=NaturalSoil.get(level);
        for(var p:geometry.tasks) if(!soil.eligible(level,p)) return null;
        // Untouched witnessed mineral is allowed ONLY as support beneath the floor, never a target/roof/wall.
        for(int f=0;f<3;f++) if(!soil.floorSupport(level,geometry.at(f,0,-f-1))) return null;
        for(int f=3;f<=5;f++) for(int side=-1;side<=1;side++) {
            if(!soil.floorSupport(level,geometry.at(f,side,-3)) || !soil.eligible(level,geometry.at(f,side,0))) return null;
        }
        for(int f=3;f<=5;f++) for(int side=-1;side<=1;side++) for(int dy=-2;dy<=-1;dy++)
            for(var d:Direction.Plane.HORIZONTAL) {
                var n=geometry.at(f,side,dy).relative(d);
                if(!geometry.tasks.contains(n) && !soil.eligible(level,n)) return null;
            }
        var plants=new ArrayList<BlockPos>();var plantStates=new ArrayList<BlockState>();
        // Only plants directly supported by declared excavation soil are affected by that excavation.
        for(var p:geometry.tasks) {
            var above=p.above();if(geometry.tasks.contains(above)) continue;
            if(NativeVegetation.material(level.getBlockState(above))) {
                if(!NativeVegetation.get(level).eligible(level,above)) return null;
                plants.add(above);plantStates.add(level.getBlockState(above));
            } else if(p.getY()==entrance.getY() && !traversable(level,above)) return null;
        }
        var exterior=new LinkedHashSet<BlockPos>();
        for(int f=-5;f<=-1;f++) for(int side=-4;side<=4;side++) {
            var feet=exteriorFeet(level,geometry.at(f,side,1));if(feet!=null) exterior.add(feet);
        }
        // Connected supported stands at actual heights. No lane clearing or relaxed enclosure.
        var start=entrance.above();if(!walkable(level,start)) return null;
        var reached=new LinkedHashSet<BlockPos>();var queue=new java.util.ArrayDeque<BlockPos>();queue.add(start);
        while(!queue.isEmpty()) {
            var p=queue.remove();if(!reached.add(p)) continue;
            for(var n:exterior) if(Math.abs(p.getX()-n.getX())+Math.abs(p.getZ()-n.getZ())==1
                && Math.abs(p.getY()-n.getY())<=1 && !reached.contains(n)) queue.add(n);
        }
        for(int f=-3;f<=-1;f++) for(int side=-1;side<=1;side++) {
            var feet=exteriorFeet(level,geometry.at(f,side,1));if(feet==null || !reached.contains(feet)) return null;
        }
        var deposits=new ArrayList<BlockPos>();
        for(var anchor:geometry.deposits()) {
            var p=exteriorFeet(level,anchor);
            if(p!=null && level.getBlockState(p).isAir() && reached.contains(p)
                && reached.stream().anyMatch(n -> Math.abs(p.getX()-n.getX())+Math.abs(p.getZ()-n.getZ())==1 && Math.abs(p.getY()-n.getY())<=1)) deposits.add(p);
        }
        if(deposits.size()<HARD_CAP-2) return null;
        for(var p:geometry.tasks) for(var d:Direction.values()) if(!level.getFluidState(p.relative(d)).isEmpty()) return null;
        var work=bottomFirst(entrance,direction).tasks();
        return new NestPlan(entrance,direction,work,work.stream().map(level::getBlockState).toList(),
            List.copyOf(plants),List.copyOf(plantStates),List.copyOf(deposits),exteriorFeet(level,geometry.at(-2,0,1)));
    }
    /** Saved adaptations confer no permission: live origin/reach checks remain mandatory at each action. */
    public boolean validAdaptation() {
        if(plants.size()!=plantExpected.size() || plants.size()>3 || new java.util.HashSet<>(plants).size()!=plants.size()) return false;
        for(int i=0;i<plants.size();i++) if(!tasks.contains(plants.get(i).below()) || plants.get(i).getY()!=entrance.getY()+1 || !NativeVegetation.material(plantExpected.get(i))) return false;
        var legacy=new NestPlan(entrance,direction,List.of(),List.of()).deposits();
        if(surfaceDeposits.size()>40 || new java.util.HashSet<>(surfaceDeposits).size()!=surfaceDeposits.size()) return false;
        for(var p:surfaceDeposits) if(legacy.stream().noneMatch(a -> a.getX()==p.getX() && a.getZ()==p.getZ() && Math.abs(a.getY()-p.getY())<=1)) return false;
        var anchor=at(-2,0,1);
        return exteriorStand==null || exteriorStand.getX()==anchor.getX() && exteriorStand.getZ()==anchor.getZ() && Math.abs(exteriorStand.getY()-anchor.getY())<=1;
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
    /** Room columns open from their supported lower face, keeping a pending soil block covered by its roof.
     * Entrance stairs keep their required top-first order; the original serialized order remains readable. */
    public static NestPlan bottomFirst(BlockPos e,Direction d){
        var p=geometry(e,d);var tasks=new ArrayList<BlockPos>(p.tasks().subList(0,6));
        for(int side:new int[]{0,-1,1})for(int f=3;f<=5;f++)for(int y=-2;y<=-1;y++)tasks.add(p.at(f,side,y));
        return new NestPlan(e.immutable(),d,List.copyOf(tasks),List.of());
    }
    public static boolean walkable(ServerLevel level, BlockPos feet) {
        return traversable(level,feet) && traversable(level,feet.above())
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
    /** The chamber shell: every face is planned space, an authorized opening of the colony's own dig jobs, or solid. */
    private void enclosedChamber(ServerLevel level,java.util.UUID owner,Findings r,boolean footprint) {
        for (int f=3; f<=5; f++) for (int side=-1;side<=1;side++) for(int dy=-2;dy<=-1;dy++) {
            BlockPos p=at(f,side,dy);
            for(Direction d:Direction.values()) {
                BlockPos n=p.relative(d);
                if (tasks.contains(n) || !inside(level,n,footprint,r) || DigJob.anyOpening(level,n,owner)) continue;
                if (!level.getBlockState(n).isSolidRender()) r.fault("enclosure_shell_open");
            }
        }
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
    /** The first observed fault, else the first unavailable cell (nurseryFindings). */
    public String nurseryProblem(ServerLevel level, java.util.UUID owner, boolean operational) {
        return nurseryFindings(level, owner, operational).problem();
    }
    /** Every habitat check of the connected nest: corner chunks, plugs, entrance route, corridor and chamber shells, the
     * colony's dig jobs and the chamber floor. Each check runs, in this order, on the cells that are loaded; an unloaded
     * cell is recorded as unavailable instead of read. Observed faults therefore win over unavailable terrain, and the
     * founding stage confirmation and brood care see the same damage. */
    public Findings nurseryFindings(ServerLevel level, java.util.UUID owner, boolean operational) {
        var r = new Findings();
        boolean footprint = true;
        // The same chunk lookup as every other cell (loaded), so no corner is read differently from the cells it covers.
        for (int f : new int[]{2, 6}) for (int s : new int[]{-2, 2})
            if (!loaded(level, at(f, s, -3))) { r.unavailable("enclosure_chunk_unavailable"); footprint = false; }
        var plugs = ColonyPlugs.get(level);
        // Plug availability must be checked before opened(): false can mean unavailable, not missing.
        for (BlockPos p : plugs()) if (r.cell(loaded(level,p),"enclosure_chunk_unavailable")
                && !(operational && plugs.opened(level,p,owner)) && !ColonyPlugs.material(level.getBlockState(p))) r.fault("enclosure_plug_missing");
        if (operational) {
            for (BlockPos p : plugs()) if (loaded(level,p) && !plugs.opened(level,p,owner) && !plugs.owned(level,p,owner)) r.fault("enclosure_plug_ownership_revoked");
            // The route leaves the corner-checked footprint: an unloaded cell is unknown, never an observed obstruction.
            for (int f=0;f<2;f++) if (r.cell(loaded(level,at(f,0,-f)),"enclosure_chunk_unavailable") && !walkable(level,at(f,0,-f))) r.fault("enclosure_route_obstructed");
            if (plugs().stream().allMatch(p -> plugs.opened(level,p,owner)) && inside(level,at(2,0,-2),footprint,r) && !walkable(level,at(2,0,-2))) r.fault("enclosure_route_obstructed");
            // The recorded plugs are the only new opening. Retain the underground corridor's support/shell too.
            for (BlockPos p : undergroundSurfaces()) {
                if (!r.cell(loaded(level,p),"enclosure_chunk_unavailable") || DigJob.anyOpening(level,p,owner)) continue;
                if (!level.getBlockState(p).isSolidRender() || !level.getFluidState(p).isEmpty()) r.fault("enclosure_shell_open");
            }
        }
        enclosedChamber(level,owner,r,footprint);
        NestExpansion.get(level).findings(level,owner,r);
        ChamberExcavation.get(level).findings(level,owner,r);
        var mining=Mining.get(level).job(owner);if(mining!=null)mining.findings(level,owner,r);
        for (int f = 3; f <= 5; f++) for (int s = -1; s <= 1; s++) {
            BlockPos p = at(f, s, -2);
            if (!inside(level,p,footprint,r)) continue;
            boolean owned = p.equals(nursery()) && level.getBlockEntity(p) instanceof dev.primeants.brood.BroodPile pile
                    && pile.ownedBy(owner, this);
            owned |= p.equals(cache()) && level.getBlockEntity(p) instanceof dev.primeants.worker.NestCache cache && cache.ownedBy(owner, this);
            if ((!traversable(level,p) && !owned) || !traversable(level,p.above())
                    || !level.getBlockState(p.below()).isSolidRender() || !level.getFluidState(p).isEmpty()
                    || !level.getFluidState(p.below()).isEmpty()) r.fault("enclosure_chamber_obstructed");
        }
        return r;
    }
    /** A cell of the corner-checked footprint (forward 2..6, side -2..2) is loaded whenever all four corner chunks are:
     * a five-wide span crosses at most two chunks per axis. Otherwise each cell is checked and an unloaded one recorded. */
    private static boolean inside(ServerLevel level, BlockPos p, boolean footprint, Findings r) {
        return footprint || r.cell(loaded(level,p),"enclosure_chunk_unavailable");
    }
}
