package dev.primeants.founding;

import dev.primeants.brood.BroodPile;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.colony.ColonyStage;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** The colony's mound in the world: NestMound's plan for the colony's current stage, each column bound to its own local
 * ground (NestMound.column). A planned cell takes a unit of soil only where it is air or a witnessed short native plant,
 * on natural ground or the colony's own mound soil, and only while vanilla support survives the change (SupportSurvival).
 * A plant there is buried: removed without drops, as the queen's excavation removes witnessed plants (NativeVegetation).
 * Fluids, player-placed blocks, block entities, logs, leaves, crops and every other block are never covered or removed:
 * their column stops beneath them. Each unit laid is the colony's own nest soil (ColonyTerrain "mound:nest_soil"), a
 * counted stock that later structure work can draw on; nothing removes it when the colony regresses to a smaller plan.
 * Soil from nest-plan rooms (ChamberExcavation) goes here; the 0.1.0 founding and widening deposits keep their lists. */
public final class MoundSoil {
    private MoundSoil() { }
    /** A planned cell where it lies now (null while its column has no ground in range or is not loaded), and its state. */
    public record Slot(NestMound.Cell cell, BlockPos pos, NestMound.Slot state) { }
    /** Planned cells holding the colony's soil, free for more, and not loaded. */
    public record Capacity(int filled, int free, int unknown) { }
    /** The stage whose plan takes the colony's soil now: its nursery's last evaluation, else the registry's last stage. */
    public static ColonyStage stage(ServerLevel l, NestPlan home, UUID owner) {
        if (l.getBlockEntity(home.nursery()) instanceof BroodPile pile && pile.ownedBy(owner, home) && pile.stageEvaluation() != null) return pile.stageEvaluation().stage();
        var colony = ChamberRegistry.get(l).colony(owner);
        return colony == null ? ColonyStage.FOUNDING : colony.stage();
    }
    /** One block of a mound column, read live (NestMound.Read). */
    public static NestMound.Read read(ServerLevel l, BlockPos p, UUID owner) {
        if (!NestPlan.loaded(l, p)) return NestMound.Read.UNLOADED;
        if (ColonyTerrain.get(l).mound(l, p, owner)) return NestMound.Read.MOUND;
        if (l.getBlockState(p).isAir() && l.getFluidState(p).isEmpty()) return NestMound.Read.OPEN;
        if (NativeVegetation.get(l).eligible(l, p)) return NestMound.Read.PLANT;
        if (NaturalSoil.get(l).floorSupport(l, p)) return NestMound.Read.NATURAL_GROUND;
        return NestMound.Read.OTHER;
    }
    /** The stage's planned cells in deposit order, bound to the land now. */
    public static List<Slot> slots(ServerLevel l, NestPlan home, UUID owner, ColonyStage stage) {
        var plan = NestMound.plan(stage); var heights = NestMound.heights(plan); var columns = new HashMap<List<Integer>, NestMound.Column>(); var out = new ArrayList<Slot>();
        for (var c : plan) {
            var key = List.of(c.forward(), c.side());
            var column = columns.computeIfAbsent(key, k -> NestMound.column(dy -> read(l, home.at(c.forward(), c.side(), dy), owner), heights.get(k)));
            out.add(new Slot(c, column.ground() == null ? null : home.at(c.forward(), c.side(), column.ground() + 1 + c.layer()), column.layers().get(c.layer())));
        }
        return out;
    }
    public static Capacity capacity(List<Slot> slots) {
        int filled = 0, free = 0, unknown = 0;
        for (var s : slots) switch (s.state()) { case FILLED -> filled++; case FREE -> free++; case UNKNOWN -> unknown++; default -> { } }
        return new Capacity(filled, free, unknown);
    }
    /** The free cells of the colony's current plan, in deposit order: where a builder looks for the next one that takes
     * a unit now. */
    public static List<BlockPos> free(ServerLevel l, NestPlan home, UUID owner) {
        return slots(l, home, owner, stage(l, home, owner)).stream().filter(s -> s.state() == NestMound.Slot.FREE).map(Slot::pos).toList();
    }
    /** A unit may go into this cell now: air or a witnessed short plant, dry, resting on natural ground or the colony's own
     * mound soil. */
    public static boolean takes(ServerLevel l, BlockPos p, UUID owner) {
        var here = read(l, p, owner); var below = read(l, p.below(), owner);
        return (here == NestMound.Read.OPEN || here == NestMound.Read.PLANT) && (below == NestMound.Read.NATURAL_GROUND || below == NestMound.Read.MOUND)
            && l.getBlockState(p.below()).isSolidRender();
    }
    /** Lays one unit on a cell that takes it now: a witnessed plant there is buried without drops, and the cell becomes the
     * colony's own mound soil. The caller is a claimed builder in reach. */
    public static boolean lay(ServerLevel l, BlockPos p, UUID owner) {
        if (!takes(l, p, owner) || SupportSurvival.problem(l, p, NurseryBlocks.NEST_SOIL.defaultBlockState()) != null) return false;
        var buried = l.getBlockState(p).isAir() ? null : l.getBlockState(p);
        if (!l.setBlock(p, NurseryBlocks.NEST_SOIL.defaultBlockState(), 3)) return false;
        ColonyTerrain.get(l).deposited(p, owner);
        if (buried != null) dev.primeants.PrimeAnts.LOGGER.info("Mound buried plant queen={} cell={} plant={} drops=0", owner, p, buried);
        return true;
    }
    /** Every cell a mound of this colony may hold, for accounting: the planned cells of the largest plan where they lie now,
     * and the 0.1.0 deposits (founding, widening). */
    public static Set<BlockPos> cells(ServerLevel l, NestPlan home, UUID owner) {
        var out = new LinkedHashSet<BlockPos>(NestExpansion.deposits(home));
        for (var s : slots(l, home, owner, ColonyStage.GREAT)) if (s.pos() != null) out.add(s.pos());
        return out;
    }
}
