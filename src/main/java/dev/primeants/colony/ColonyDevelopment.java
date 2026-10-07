package dev.primeants.colony;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.founding.ChamberExcavation;
import dev.primeants.founding.ChamberUpgrade;
import dev.primeants.founding.ColonyTerrain;
import dev.primeants.founding.Findings;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.NestWalls;
import dev.primeants.worker.AdultHistory;
import dev.primeants.worker.ColonyMembers;
import dev.primeants.worker.MaterialStore;
import dev.primeants.worker.MaterialUnits;
import dev.primeants.worker.NestCache;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Reads live bodies and blocks into StageRules inputs and records stage transitions. Saved chambers and stages are
 * claims only. The colony's nursery calls this at laying/emergence decisions and on a slow cadence, never per tick. */
public final class ColonyDevelopment {
    /** Loaded nursery ticks between slow-cadence evaluations. Membership changes re-evaluate at the next decision. */
    public static final int INTERVAL = 100;
    public enum Presence { CONFIRMED, ABSENT, UNKNOWN }
    /** Live tier is 0 unless the chamber is open and enclosed now; then it is the lowest tier of its walls (NestWalls),
     * read now. clay is the clay units in its walls that the colony itself built, counted only while confirmed. */
    public record ChamberState(String id, Map<ChamberFunction, Presence> functions, int tier, String problem, int clay) {
        @Override public String toString() { return id + functions + " tier=" + tier + (clay == 0 ? "" : " wallClay=" + clay) + (problem == null ? "" : " problem=" + problem); }
    }
    public record Evaluation(ColonyStage previous, StageRules.Result result, StageRules.Inputs inputs, List<ChamberState> chambers, int cap) {
        public ColonyStage stage() { return result.stage(); }
    }
    private ColonyDevelopment() { }
    /** now is the colony's clock for the owner's hysteresis (StageRules): its nursery's loaded ticks. */
    public static Evaluation evaluate(ServerLevel l, UUID queen, NestPlan plan, boolean operational, int bound, long now) {
        var registry = ChamberRegistry.get(l);
        boolean registered = registry.colony(queen) != null;
        var colony = registry.found(queen, plan);
        // A failed lookup is unknown, never dead (ColonyMembers' rule); only a recorded death removes an adult.
        int known = 0, unknown = 0; boolean queenDead = false;
        if (l.getEntity(queen) instanceof LasiusNigerEntity q) { if (q.isAlive() && !q.isRemoved()) known++; else queenDead = true; }
        else if (!AdultHistory.get(l).recorded(queen)) unknown++;
        else queenDead = true;
        for (var m : ColonyMembers.get(l).members(queen)) {
            if (m.dead()) continue;
            if (l.getEntity(m.worker()) instanceof LasiusNigerEntity w) { if (w.isAlive() && !w.isRemoved()) known++; }
            else unknown++;
        }
        var states = new ArrayList<ChamberState>();
        var tiers = new EnumMap<ChamberFunction, int[]>(ChamberFunction.class);
        int food = 0, foodUnknown = 0, storesUnknown = 0, wallClay = 0, wallClayUnknown = 0;
        var upgrades = ChamberUpgrade.get(l);
        var stores = new ArrayList<List<MaterialUnits.Material>>();
        // One connected nest: the founding chamber's habitat covers the stairs, the widening and every dug chamber.
        var habitat = plan.nurseryFindings(l, queen, operational);
        for (var chamber : colony.chambers()) {
            var state = confirm(l, queen, plan, habitat, chamber);
            states.add(state);
            state.functions().forEach((f, presence) -> {
                var t = tiers.computeIfAbsent(f, k -> new int[2]);
                if (presence == Presence.CONFIRMED) { t[0] = Math.max(t[0], state.tier()); t[1] = Math.max(t[1], state.tier()); }
                else if (presence == Presence.UNKNOWN) t[1] = Math.max(t[1], chamber.tier());
            });
            // Clay the colony built into a confirmed chamber's walls still counts; an unknown chamber may hold what it built.
            wallClay += state.clay();
            if (state.tier() == 0 && state.functions().containsValue(Presence.UNKNOWN)) wallClayUnknown += upgrades.builtClay(queen, chamber.id());
            var store = state.functions().get(ChamberFunction.FOOD_STORE);
            if (store == Presence.CONFIRMED && l.getBlockEntity(chamber.markers().get(ChamberFunction.FOOD_STORE)) instanceof NestCache cache) food += cache.size();
            else if (store == Presence.UNKNOWN) foodUnknown += NestCache.CAPACITY;
            // Clay and stone are known only in a confirmed store; an unknown one may hold its most of each.
            var materials = state.functions().get(ChamberFunction.MATERIAL_STORE);
            if (materials == Presence.CONFIRMED && l.getBlockEntity(chamber.markers().get(ChamberFunction.MATERIAL_STORE)) instanceof MaterialStore s)
                stores.add(s.contents().stream().map(MaterialStore::kind).toList());
            else if (materials == Presence.UNKNOWN) storesUnknown++;
        }
        var bounds = new EnumMap<ChamberFunction, StageRules.Bound>(ChamberFunction.class);
        tiers.forEach((f, t) -> bounds.put(f, new StageRules.Bound(t[0], t[1])));
        // Clay moved from the store toward the walls stays counted: units in the upgrade builder's mandibles are known
        // while it is seen carrying them, possible while it is unavailable (ChamberUpgrade.carried).
        var stored = MaterialUnits.stock(stores, storesUnknown, MaterialUnits.Material.CLAY); var carried = upgrades.carried(l, queen);
        var clay = new StageRules.Bound(stored.known() + wallClay + carried.known(), stored.possible() + wallClay + wallClayUnknown + carried.possible());
        var inputs = new StageRules.Inputs(new StageRules.Bound(known, known + unknown), bounds, new StageRules.Bound(food, food + foodUnknown),
            clay, MaterialUnits.stock(stores, storesUnknown, MaterialUnits.Material.STONE), queenDead);
        var result = StageRules.evaluate(colony.stage(), inputs, colony.unmetSince(), now);
        int cap = AdultBound.effectiveCap(result.stage(), bound);
        if (result.stage() != colony.stage() || !result.unmetSince().equals(colony.unmetSince())) registry.stage(queen, result.stage(), result.unmetSince());
        if (!registered || result.stage() != colony.stage()) {
            PrimeAnts.LOGGER.info("Colony stage queen={} from={} to={} certain={} possible={} adults={} chambers={} food={} clay={} stone={} queenDead={} cap={} bound={} pileTicks={} pending={} missing={}",
                queen, registered ? colony.stage().serializedName() : "unregistered", result.stage().serializedName(), result.certain().serializedName(), result.possible().serializedName(),
                inputs.adults(), states, inputs.food(), inputs.clay(), inputs.stone(), queenDead, cap, bound, now, result.pending(), result.missing());
        } else if (!result.unmetSince().keySet().equals(colony.unmetSince().keySet()))
            PrimeAnts.LOGGER.info("Colony stage held queen={} stage={} pileTicks={} pending={} cleared={}", queen, result.stage().serializedName(), now, result.pending(),
                colony.unmetSince().keySet().stream().filter(k -> !result.unmetSince().containsKey(k)).toList());
        return new Evaluation(colony.stage(), result, inputs, List.copyOf(states), cap);
    }
    /** One registered chamber's function by this same live rule, now: the forager's check before it hauls to or
     * stores in the colony's material store. */
    public static Presence presence(ServerLevel l, UUID queen, NestPlan plan, String chamberId, ChamberFunction f) {
        var colony = ChamberRegistry.get(l).colony(queen); var chamber = colony == null ? null : colony.chamber(chamberId);
        if (chamber == null || !chamber.functions().contains(f)) return Presence.ABSENT;
        return confirm(l, queen, plan, plan.nurseryFindings(l, queen, true), chamber).functions().get(f);
    }
    /** A function counts only with its owned marker in an open, enclosed chamber: the same live habitat predicate that
     * gates brood care (NestPlan.nurseryFindings), plus a dug chamber's own cells. Every check runs; a fault seen in
     * loaded blocks makes the function absent even beside unavailable terrain, which alone leaves it unknown. The
     * queen's hall has no marker block: its marker is the living queen, ready in her own space. */
    private static ChamberState confirm(ServerLevel l, UUID queen, NestPlan plan, Findings habitat, ChamberRegistry.Chamber chamber) {
        var functions = new EnumMap<ChamberFunction, Presence>(ChamberFunction.class);
        Findings space;
        if (chamber.id().equals(ChamberRegistry.FOUNDING)) space = habitat;
        else {
            var room = ChamberExcavation.roomFindings(l, queen, plan, chamber.min(), chamber.max());
            if (room == null) {
                for (var f : chamber.functions()) functions.put(f, Presence.ABSENT);
                return new ChamberState(chamber.id(), functions, 0, "no_live_confirmation_rule", 0);
            }
            space = habitat.copy().add(room);
        }
        var all = space.copy();
        for (var f : chamber.functions()) {
            var marker = chamber.markers().get(f);
            var check = space.copy();
            if (f == ChamberFunction.QUEENS_HALL) queen(l, queen, check);
            else if (marker == null) check.fault(f.serializedName() + "_marker_missing");
            else if (check.cell(NestPlan.loaded(l, marker), f.serializedName() + "_marker_chunk_unavailable") && !ownedMarker(l, queen, plan, f, marker))
                check.fault(f.serializedName() + "_marker_missing_or_foreign");
            functions.put(f, switch (check.verdict()) { case CLEAR -> Presence.CONFIRMED; case UNKNOWN -> Presence.UNKNOWN; case DAMAGED -> Presence.ABSENT; });
            all.add(check);
        }
        if (space.verdict() != Findings.Verdict.CLEAR) return new ChamberState(chamber.id(), functions, 0, all.problem(), 0);
        var walls = walls(l, queen, plan, chamber);
        return new ChamberState(chamber.id(), functions, walls[0], all.problem(), walls[1]);
    }
    /** A confirmed chamber's walls (NestWalls), read now: their lowest tier, and the clay units in the cells the colony
     * itself built of packed clay. A confirmed chamber's walls are loaded (its shell scan read them); an unloaded one
     * would only count as earth. */
    private static int[] walls(ServerLevel l, UUID queen, NestPlan plan, ChamberRegistry.Chamber chamber) {
        var terrain = ColonyTerrain.get(l); var tiers = new ArrayList<Integer>(); int clay = 0;
        for (var p : ChamberUpgrade.walls(plan, chamber)) {
            if (!NestPlan.loaded(l, p)) { tiers.add(1); continue; }
            tiers.add(terrain.wallTier(l, p, queen));
            if (terrain.built(l, p, queen, NurseryBlocks.PACKED_CLAY)) clay += NestWalls.CLAY_PER_CELL;
        }
        return new int[]{NestWalls.chamberTier(tiers), clay};
    }
    private static boolean ownedMarker(ServerLevel l, UUID queen, NestPlan plan, ChamberFunction f, BlockPos marker) {
        return switch (f) {
            case NURSERY -> l.getBlockEntity(marker) instanceof BroodPile pile && pile.ownedBy(queen, plan);
            case FOOD_STORE -> l.getBlockEntity(marker) instanceof NestCache cache && cache.ownedBy(queen, plan);
            case MATERIAL_STORE -> l.getBlockEntity(marker) instanceof MaterialStore store && store.ownedBy(queen, plan);
            // The queen's hall has no marker block (queen below).
            case QUEENS_HALL -> false;
        };
    }
    /** The living queen, settled with her body inside her room: observed, so a queen seen dead or outside is a loss even
     * beside unavailable terrain, and only a queen whose lookup fails without a recorded death is unknown. The habitat
     * part of her readiness is already in the chamber's findings. */
    private static void queen(ServerLevel l, UUID queen, Findings r) {
        if (l.getEntity(queen) instanceof LasiusNigerEntity q) {
            if (!q.isAlive() || q.isRemoved()) r.fault("queens_hall_queen_dead");
            else if (q.founding().occupancyProblem(l) instanceof String problem) r.fault("queens_hall_" + problem);
        } else if (AdultHistory.get(l).recorded(queen)) r.fault("queens_hall_queen_dead");
        else r.unavailable("queens_hall_queen_unavailable");
    }
}
