package dev.primeants.colony;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.ChamberExcavation;
import dev.primeants.founding.Findings;
import dev.primeants.founding.NestPlan;
import dev.primeants.worker.AdultHistory;
import dev.primeants.worker.ColonyMembers;
import dev.primeants.worker.MaterialStore;
import dev.primeants.worker.NestCache;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Reads live bodies and blocks into StageRules inputs and records stage transitions. Saved chambers and stages are
 * claims only. The colony's nursery calls this at laying/emergence decisions and on a slow cadence, never per tick. */
public final class ColonyDevelopment {
    /** Loaded nursery ticks between slow-cadence evaluations. Membership changes re-evaluate at the next decision. */
    public static final int INTERVAL = 100;
    /** Packed-earth walls. Tier 2 and 3 wall materials arrive with later upgrade work. */
    static final int EARTHEN = 1;
    public enum Presence { CONFIRMED, ABSENT, UNKNOWN }
    /** Live tier is 0 unless the chamber is open and enclosed now. */
    public record ChamberState(String id, Map<ChamberFunction, Presence> functions, int tier, String problem) {
        @Override public String toString() { return id + functions + " tier=" + tier + (problem == null ? "" : " problem=" + problem); }
    }
    public record Evaluation(ColonyStage previous, StageRules.Result result, StageRules.Inputs inputs, List<ChamberState> chambers, int cap) {
        public ColonyStage stage() { return result.stage(); }
    }
    private ColonyDevelopment() { }
    public static Evaluation evaluate(ServerLevel l, UUID queen, NestPlan plan, boolean operational, int bound) {
        var registry = ChamberRegistry.get(l);
        boolean registered = registry.colony(queen) != null;
        var colony = registry.found(queen, plan);
        // A failed lookup is unknown, never dead (ColonyMembers' rule); only a recorded death removes an adult.
        int known = 0, unknown = 0;
        if (l.getEntity(queen) instanceof LasiusNigerEntity q) { if (q.isAlive() && !q.isRemoved()) known++; }
        else if (!AdultHistory.get(l).recorded(queen)) unknown++;
        for (var m : ColonyMembers.get(l).members(queen)) {
            if (m.dead()) continue;
            if (l.getEntity(m.worker()) instanceof LasiusNigerEntity w) { if (w.isAlive() && !w.isRemoved()) known++; }
            else unknown++;
        }
        var states = new ArrayList<ChamberState>();
        var tiers = new EnumMap<ChamberFunction, int[]>(ChamberFunction.class);
        int food = 0, foodUnknown = 0;
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
            var store = state.functions().get(ChamberFunction.FOOD_STORE);
            if (store == Presence.CONFIRMED && l.getBlockEntity(chamber.markers().get(ChamberFunction.FOOD_STORE)) instanceof NestCache cache) food += cache.size();
            else if (store == Presence.UNKNOWN) foodUnknown += NestCache.CAPACITY;
        }
        var bounds = new EnumMap<ChamberFunction, StageRules.Bound>(ChamberFunction.class);
        tiers.forEach((f, t) -> bounds.put(f, new StageRules.Bound(t[0], t[1])));
        // Stores are set up empty and nothing fills them yet: clay and stone are known to be zero.
        var inputs = new StageRules.Inputs(new StageRules.Bound(known, known + unknown), bounds, new StageRules.Bound(food, food + foodUnknown),
            StageRules.Bound.NONE, StageRules.Bound.NONE);
        var result = StageRules.evaluate(colony.stage(), inputs);
        int cap = AdultBound.effectiveCap(result.stage(), bound);
        if (!registered || result.stage() != colony.stage()) {
            if (result.stage() != colony.stage()) registry.stage(queen, result.stage());
            PrimeAnts.LOGGER.info("Colony stage queen={} from={} to={} certain={} possible={} adults={} chambers={} food={} clay={} stone={} cap={} bound={} missing={}",
                queen, registered ? colony.stage().serializedName() : "unregistered", result.stage().serializedName(), result.certain().serializedName(), result.possible().serializedName(),
                inputs.adults(), states, inputs.food(), inputs.clay(), inputs.stone(), cap, bound, result.missing());
        }
        return new Evaluation(colony.stage(), result, inputs, List.copyOf(states), cap);
    }
    /** A function counts only with its owned marker in an open, enclosed chamber: the same live habitat predicate that
     * gates brood care (NestPlan.nurseryFindings), plus a dug chamber's own cells. Every check runs; a fault seen in
     * loaded blocks makes the function absent even beside unavailable terrain, which alone leaves it unknown. */
    private static ChamberState confirm(ServerLevel l, UUID queen, NestPlan plan, Findings habitat, ChamberRegistry.Chamber chamber) {
        var functions = new EnumMap<ChamberFunction, Presence>(ChamberFunction.class);
        Findings space;
        if (chamber.id().equals(ChamberRegistry.FOUNDING)) space = habitat;
        else {
            var room = ChamberExcavation.roomFindings(l, queen, plan, chamber.min(), chamber.max());
            if (room == null) {
                for (var f : chamber.functions()) functions.put(f, Presence.ABSENT);
                return new ChamberState(chamber.id(), functions, 0, "no_live_confirmation_rule");
            }
            space = habitat.copy().add(room);
        }
        var all = space.copy();
        for (var f : chamber.functions()) {
            var marker = chamber.markers().get(f);
            var check = space.copy();
            if (marker == null) check.fault(f.serializedName() + "_marker_missing");
            else if (check.cell(NestPlan.loaded(l, marker), f.serializedName() + "_marker_chunk_unavailable") && !ownedMarker(l, queen, plan, f, marker))
                check.fault(f.serializedName() + "_marker_missing_or_foreign");
            functions.put(f, switch (check.verdict()) { case CLEAR -> Presence.CONFIRMED; case UNKNOWN -> Presence.UNKNOWN; case DAMAGED -> Presence.ABSENT; });
            all.add(check);
        }
        return new ChamberState(chamber.id(), functions, space.verdict() == Findings.Verdict.CLEAR ? Math.min(chamber.tier(), EARTHEN) : 0, all.problem());
    }
    private static boolean ownedMarker(ServerLevel l, UUID queen, NestPlan plan, ChamberFunction f, BlockPos marker) {
        return switch (f) {
            case NURSERY -> l.getBlockEntity(marker) instanceof BroodPile pile && pile.ownedBy(queen, plan);
            case FOOD_STORE -> l.getBlockEntity(marker) instanceof NestCache cache && cache.ownedBy(queen, plan);
            case MATERIAL_STORE -> l.getBlockEntity(marker) instanceof MaterialStore store && store.ownedBy(queen, plan);
            // No queen's hall block exists yet.
            case QUEENS_HALL -> false;
        };
    }
}
