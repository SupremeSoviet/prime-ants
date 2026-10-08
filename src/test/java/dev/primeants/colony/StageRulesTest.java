package dev.primeants.colony;

import dev.primeants.colony.StageRules.Bound;
import dev.primeants.colony.StageRules.Inputs;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageRulesTest {
    private static Inputs inputs(Bound adults, int nursery, int foodStore, int materialStore, int queensHall, Bound food, int clay, int stone) {
        Map<ChamberFunction, Bound> tiers = new EnumMap<>(ChamberFunction.class);
        tiers.put(ChamberFunction.NURSERY, Bound.exactly(nursery)); tiers.put(ChamberFunction.FOOD_STORE, Bound.exactly(foodStore));
        tiers.put(ChamberFunction.MATERIAL_STORE, Bound.exactly(materialStore)); tiers.put(ChamberFunction.QUEENS_HALL, Bound.exactly(queensHall));
        return new Inputs(adults, tiers, food, Bound.exactly(clay), Bound.exactly(stone));
    }
    private static Inputs inputs(int adults, int nursery, int foodStore, int materialStore, int queensHall, int food, int clay, int stone) {
        return inputs(Bound.exactly(adults), nursery, foodStore, materialStore, queensHall, Bound.exactly(food), clay, stone);
    }
    /** Every requirement up to and including the target stage met, with the given adults. */
    private static Inputs meeting(ColonyStage target, int adults) {
        return switch (target) {
            case FOUNDING -> inputs(adults, 0, 0, 0, 0, 0, 0, 0);
            case YOUNG -> inputs(adults, 1, 1, 0, 0, 0, 0, 0);
            case MATURE -> inputs(adults, 1, 1, 1, 1, 4, 16, 0);
            case GREAT -> inputs(adults, 2, 2, 2, 2, 4, 16, 32);
        };
    }
    private static ColonyStage stage(ColonyStage previous, Inputs in) { return StageRules.evaluate(previous, in).stage(); }
    /** Every requirement's clock started at loaded tick 0. */
    private static Map<String, Long> allClocksAt(long since) {
        var clocks = new java.util.TreeMap<String, Long>(); for (var r : StageRules.TABLE) clocks.put(r.key(), since); return clocks;
    }
    /** The owner's rule once every certain shortfall has lasted exactly the grace (decision of 2026-10-07). */
    private static StageRules.Result expired(ColonyStage previous, Inputs in) { return StageRules.evaluate(previous, in, allClocksAt(0), StageRules.GRACE); }

    @Test
    void tableHoldsTheDecision23NumbersWithTheQueenCounted() {
        assertEquals(List.of(5, 30, 60, 120), List.of(ColonyStage.FOUNDING.adultCap(), ColonyStage.YOUNG.adultCap(), ColonyStage.MATURE.adultCap(), ColonyStage.GREAT.adultCap()));
        assertEquals(List.of(1, 5, 25, 50), List.of(StageRules.minAdults(ColonyStage.FOUNDING), StageRules.minAdults(ColonyStage.YOUNG), StageRules.minAdults(ColonyStage.MATURE), StageRules.minAdults(ColonyStage.GREAT)));
        assertTrue(StageRules.requirements(ColonyStage.FOUNDING).isEmpty(), "Founding is the floor: a registered founding chamber suffices");
        assertEquals(14, StageRules.TABLE.size());
    }

    @Test
    void youngNeedsFiveAdultsIncludingTheQueenANurseryAndAFoodStore() {
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.FOUNDING, inputs(5, 1, 1, 0, 0, 0, 0, 0)), "Queen plus four workers with both functions");
        var four = StageRules.evaluate(ColonyStage.FOUNDING, inputs(4, 1, 1, 0, 0, 0, 0, 0));
        assertEquals(ColonyStage.FOUNDING, four.stage());
        assertEquals("[adults 4/5]", four.missing(ColonyStage.YOUNG).toString());
        assertEquals("[nursery 0/1]", StageRules.evaluate(ColonyStage.FOUNDING, inputs(5, 0, 1, 0, 0, 0, 0, 0)).missing(ColonyStage.YOUNG).toString());
        assertEquals("[food_store 0/1]", StageRules.evaluate(ColonyStage.FOUNDING, inputs(5, 1, 0, 0, 0, 0, 0, 0)).missing(ColonyStage.YOUNG).toString());
        // Owner's hysteresis (2026-10-07): four adults are not below Founding's one, so the loss waits for its clock.
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.YOUNG, inputs(4, 1, 1, 0, 0, 0, 0, 0)), "Known loss below five adults holds Young while its clock runs");
        assertEquals(ColonyStage.FOUNDING, expired(ColonyStage.YOUNG, inputs(4, 1, 1, 0, 0, 0, 0, 0)).stage(), "Known loss below five adults regresses once it has lasted the grace");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, inputs(30, 1, 0, 0, 0, 0, 0, 0)), "Losing the food store regresses however many adults remain");
    }

    @Test
    void matureNeedsEveryOneOfItsRequirements() {
        assertEquals(ColonyStage.MATURE, stage(ColonyStage.YOUNG, meeting(ColonyStage.MATURE, 25)));
        Map<String, Inputs> lacking = Map.of(
            "adults 24/25", meeting(ColonyStage.MATURE, 24),
            "queens_hall 0/1", inputs(25, 1, 1, 1, 0, 4, 16, 0),
            "material_store 0/1", inputs(25, 1, 1, 0, 1, 4, 16, 0),
            "food 3/4", inputs(25, 1, 1, 1, 1, 3, 16, 0),
            "clay 15/16", inputs(25, 1, 1, 1, 1, 4, 15, 0));
        lacking.forEach((missing, in) -> {
            var result = StageRules.evaluate(ColonyStage.YOUNG, in);
            assertEquals(ColonyStage.YOUNG, result.stage(), missing);
            assertEquals("[" + missing + "]", result.missing(ColonyStage.MATURE).toString());
        });
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.MATURE, inputs(40, 0, 1, 1, 1, 4, 16, 0)), "Stages are cumulative: no nursery, no stage above founding");
    }

    @Test
    void greatNeedsEveryOneOfItsRequirements() {
        assertEquals(ColonyStage.GREAT, stage(ColonyStage.MATURE, meeting(ColonyStage.GREAT, 50)));
        Map<String, Inputs> lacking = Map.of(
            "adults 49/50", meeting(ColonyStage.GREAT, 49),
            "nursery_tier 1/2", inputs(50, 1, 2, 2, 2, 4, 16, 32),
            "food_store_tier 1/2", inputs(50, 2, 1, 2, 2, 4, 16, 32),
            "material_store_tier 1/2", inputs(50, 2, 2, 1, 2, 4, 16, 32),
            "queens_hall_tier 1/2", inputs(50, 2, 2, 2, 1, 4, 16, 32),
            "stone 31/32", inputs(50, 2, 2, 2, 2, 4, 16, 31));
        lacking.forEach((missing, in) -> {
            var result = StageRules.evaluate(ColonyStage.MATURE, in);
            assertEquals(ColonyStage.MATURE, result.stage(), missing);
            assertEquals("[" + missing + "]", result.missing(ColonyStage.GREAT).toString());
        });
    }

    @Test
    void missingRequirementsAreListedForEveryHigherStage() {
        // A recognized 0.1.0 colony: queen + four workers, earthen nursery and food store holding two units.
        var result = StageRules.evaluate(ColonyStage.FOUNDING, inputs(5, 1, 1, 0, 0, 2, 0, 0));
        assertEquals(ColonyStage.YOUNG, result.stage());
        assertEquals(List.of(ColonyStage.MATURE, ColonyStage.GREAT), List.copyOf(result.missing().keySet()));
        assertEquals("[adults 5/25, queens_hall 0/1, material_store 0/1, food 2/4, clay 0/16]", result.missing(ColonyStage.MATURE).toString());
        assertEquals("[adults 5/25, queens_hall 0/1, material_store 0/1, food 2/4, clay 0/16, adults 5/50, nursery_tier 1/2, food_store_tier 1/2, material_store_tier 0/2, queens_hall_tier 0/2, stone 0/32]", result.missing(ColonyStage.GREAT).toString());
        var founding = StageRules.evaluate(ColonyStage.FOUNDING, inputs(1, 1, 0, 0, 0, 0, 0, 0));
        assertEquals("[adults 1/5, food_store 0/1]", founding.missing(ColonyStage.YOUNG).toString());
        assertTrue(StageRules.evaluate(ColonyStage.GREAT, meeting(ColonyStage.GREAT, 120)).missing().isEmpty(), "Nothing is missing at the top stage");
    }

    @Test
    void aColonyWithEverythingMatureNeedsButNoNurseryIsFoundingAndMatureListsTheNursery() {
        // Review counterexample: 25 adults, food store, material store, queen's hall, 4 food and 16 clay, no nursery.
        var noNursery = inputs(25, 0, 1, 1, 1, 4, 16, 0);
        for (var saved : ColonyStage.values()) {
            var result = StageRules.evaluate(saved, noNursery);
            assertEquals(ColonyStage.FOUNDING, result.stage(), "Saved " + saved);
            assertEquals("[nursery 0/1]", result.missing(ColonyStage.YOUNG).toString());
            assertEquals("[nursery 0/1]", result.missing(ColonyStage.MATURE).toString(), "Mature's own rows are met, so the Young gap is all it lacks");
            assertEquals("[nursery 0/1, adults 25/50, nursery_tier 0/2, food_store_tier 1/2, material_store_tier 1/2, queens_hall_tier 1/2, stone 0/32]", result.missing(ColonyStage.GREAT).toString());
        }
    }

    @ParameterizedTest(name = "a gap below {0} leads its missing list")
    @EnumSource(value = ColonyStage.class, names = {"YOUNG", "MATURE", "GREAT"})
    void aLowerStageGapLeadsTheMissingListOfEveryHigherTarget(ColonyStage boundary) {
        // Each case meets every requirement of the boundary stage and above except one row below the boundary.
        var in = switch (boundary) {
            case YOUNG -> inputs(4, 2, 2, 2, 2, 4, 16, 32);   // a queen and three workers, every chamber, store and stock built
            case MATURE -> inputs(25, 1, 0, 1, 1, 4, 16, 0);  // Mature's own rows met, the Young food store missing
            case GREAT -> inputs(50, 2, 2, 2, 2, 3, 16, 32);  // Great's own rows met, Mature's food one unit short
            case FOUNDING -> throw new IllegalArgumentException();
        };
        var expected = switch (boundary) {
            case YOUNG -> Map.of(ColonyStage.YOUNG, "[adults 4/5]", ColonyStage.MATURE, "[adults 4/5, adults 4/25]", ColonyStage.GREAT, "[adults 4/5, adults 4/25, adults 4/50]");
            case MATURE -> Map.of(ColonyStage.YOUNG, "[food_store 0/1]", ColonyStage.MATURE, "[food_store 0/1]", ColonyStage.GREAT, "[food_store 0/1, adults 25/50, nursery_tier 1/2, food_store_tier 0/2, material_store_tier 1/2, queens_hall_tier 1/2, stone 0/32]");
            case GREAT -> Map.of(ColonyStage.MATURE, "[food 3/4]", ColonyStage.GREAT, "[food 3/4]");
            case FOUNDING -> Map.<ColonyStage, String>of();
        };
        var result = StageRules.evaluate(boundary, in);
        // Owner's hysteresis (2026-10-07): a lost food store applies at once; four adults (not below Founding's one) and
        // Mature's food one unit short wait for their clocks, then the lower gap decides the stage.
        assertEquals(boundary == ColonyStage.MATURE ? ColonyStage.FOUNDING : boundary, result.stage(), "A catastrophic lower gap decides the stage at once, any other holds it");
        assertEquals(boundary == ColonyStage.GREAT ? ColonyStage.YOUNG : ColonyStage.FOUNDING, expired(boundary, in).stage(), "The lower gap decides the stage");
        assertEquals(expected.keySet(), result.missing().keySet());
        expected.forEach((target, missing) -> assertEquals(missing, result.missing(target).toString(), "missing(" + target + ")"));
    }

    @Test
    void aStageHeldByUnknownsListsItsUnconfirmedRequirements() {
        Map<ChamberFunction, Bound> tiers = new EnumMap<>(ChamberFunction.class);
        tiers.put(ChamberFunction.NURSERY, new Bound(0, 1)); tiers.put(ChamberFunction.FOOD_STORE, new Bound(0, 1));
        var unloadedChamber = new Inputs(Bound.exactly(6), tiers, new Bound(0, 6), Bound.NONE, Bound.NONE);
        var held = StageRules.evaluate(ColonyStage.YOUNG, unloadedChamber);
        assertEquals(ColonyStage.YOUNG, held.stage());
        assertEquals("[nursery 0(+1?)/1, food_store 0(+1?)/1]", held.missing(ColonyStage.YOUNG).toString(), "Held, not confirmed");
        assertTrue(held.missing(ColonyStage.YOUNG).stream().allMatch(StageRules.Missing::unknown));
        assertEquals("[nursery 0(+1?)/1, food_store 0(+1?)/1, adults 6/25, queens_hall 0/1, material_store 0/1, food 0(+6?)/4, clay 0/16]", held.missing(ColonyStage.MATURE).toString());
        assertTrue(StageRules.evaluate(ColonyStage.YOUNG, inputs(6, 1, 1, 0, 0, 0, 0, 0)).missing(ColonyStage.YOUNG).isEmpty(), "A confirmed stage lacks nothing");
    }

    @Test
    void unknownMembersNeitherPromoteNorDemote() {
        var fourKnownOneUnloaded = inputs(new Bound(4, 5), 1, 1, 0, 0, Bound.NONE, 0, 0);
        var held = StageRules.evaluate(ColonyStage.FOUNDING, fourKnownOneUnloaded);
        assertEquals(ColonyStage.FOUNDING, held.stage(), "An unloaded worker cannot promote");
        assertEquals("[adults 4(+1?)/5]", held.missing(ColonyStage.YOUNG).toString());
        assertTrue(held.missing(ColonyStage.YOUNG).getFirst().unknown());
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.YOUNG, fourKnownOneUnloaded), "An unloaded worker cannot demote");
        var kept = StageRules.evaluate(ColonyStage.YOUNG, fourKnownOneUnloaded);
        assertEquals(ColonyStage.FOUNDING, kept.certain()); assertEquals(ColonyStage.YOUNG, kept.possible());
        // Owner's hysteresis (2026-10-07): certain losses that are not catastrophic regress once their clocks have run.
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.YOUNG, inputs(new Bound(3, 4), 1, 1, 0, 0, Bound.NONE, 0, 0)), "Known deaths below the threshold hold Young while their clock runs");
        assertEquals(ColonyStage.FOUNDING, expired(ColonyStage.YOUNG, inputs(new Bound(3, 4), 1, 1, 0, 0, Bound.NONE, 0, 0)).stage(), "Known deaths below the threshold still regress once they have lasted the grace");
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.FOUNDING, inputs(new Bound(5, 9), 1, 1, 0, 0, Bound.NONE, 0, 0)), "Confirmed adults promote regardless of unknown extras");
        var partlyUnloaded = inputs(new Bound(20, 40), 1, 1, 1, 1, Bound.exactly(4), 16, 0);
        assertEquals(ColonyStage.MATURE, stage(ColonyStage.MATURE, partlyUnloaded), "Twenty known plus twenty unknown may still be a Mature colony");
        assertEquals(ColonyStage.GREAT, stage(ColonyStage.GREAT, partlyUnloaded), "Great's certain shortfalls (40 possible adults, tier-1 chambers, no stone) hold it while their clocks run");
        assertEquals(ColonyStage.MATURE, expired(ColonyStage.GREAT, partlyUnloaded).stage(), "Unknowns cannot hold a stage above what they could support once its clocks have run");
    }

    @Test
    void unloadedChambersAndStoresNeitherPromoteNorDemote() {
        Map<ChamberFunction, Bound> tiers = new EnumMap<>(ChamberFunction.class);
        tiers.put(ChamberFunction.NURSERY, Bound.exactly(1)); tiers.put(ChamberFunction.FOOD_STORE, new Bound(0, 1));
        var unloadedStore = new Inputs(Bound.exactly(6), tiers, new Bound(0, 6), Bound.NONE, Bound.NONE);
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.FOUNDING, unloadedStore));
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.YOUNG, unloadedStore));
        assertEquals("[food_store 0(+1?)/1]", StageRules.evaluate(ColonyStage.FOUNDING, unloadedStore).missing(ColonyStage.YOUNG).toString());
    }

    @Test
    void staleSavedStageIsRecomputedWhenEverythingIsKnown() {
        var young = inputs(7, 1, 1, 0, 0, 3, 0, 0);
        for (var saved : ColonyStage.values()) assertEquals(ColonyStage.YOUNG, stage(saved, young), "Saved " + saved);
        assertEquals(ColonyStage.FOUNDING, stage(null, inputs(1, 1, 0, 0, 0, 0, 0, 0)), "A newly registered colony starts at founding");
    }

    @ParameterizedTest(name = "no deadlock under the {0} cap")
    @EnumSource(ColonyStage.class)
    void nextThresholdIsReachableUnderEveryCapIncludingRightAfterRegression(ColonyStage stage) {
        var target = stage.next();
        if (target == null) {
            assertEquals(AdultBound.MAX, stage.adultCap(), "The top cap is the configured maximum, so the default bound never hides a stage");
            assertNull(stage.next());
            return;
        }
        assertTrue(StageRules.minAdults(target) <= stage.adultCap(), target + " threshold must fit under the " + stage + " cap");
        assertEquals(target, stage(stage, meeting(target, stage.adultCap())), "A colony filled to the " + stage + " cap reaches " + target);
        // A regression from any higher stage down to the queen alone must regrow, one adult at a time, inside each cap.
        for (var from : ColonyStage.values()) {
            if (from.compareTo(stage) < 0) continue;
            // Owner's hysteresis (2026-10-07): the queen alone is not below Founding's one adult, so a Young colony holds
            // until its clock has run; from Mature or Great she is below Young's five and the loss applies at once.
            assertEquals(from == ColonyStage.YOUNG ? ColonyStage.YOUNG : ColonyStage.FOUNDING, stage(from, meeting(target, 1)), "Losing every worker at once from " + from);
            var current = expired(from, meeting(target, 1)).stage();
            assertEquals(ColonyStage.FOUNDING, current, "Losing every worker regresses " + from);
            int adults = 1;
            while (current.compareTo(target) < 0) {
                int cap = AdultBound.effectiveCap(current, AdultBound.MAX);
                assertTrue(adults < cap, current + " cap " + cap + " blocks regrowth to " + target + " at " + adults + " adults");
                adults++;
                current = stage(current, meeting(target, adults));
            }
            assertEquals(target, current);
        }
    }

    @Test
    void effectiveCapIsTheLowerOfStageCapAndConfiguredBound() {
        assertEquals(5, AdultBound.effectiveCap(ColonyStage.FOUNDING, AdultBound.MAX));
        assertEquals(30, AdultBound.effectiveCap(ColonyStage.YOUNG, AdultBound.MAX));
        assertEquals(60, AdultBound.effectiveCap(ColonyStage.MATURE, AdultBound.MAX));
        assertEquals(120, AdultBound.effectiveCap(ColonyStage.GREAT, AdultBound.MAX));
        assertEquals(4, AdultBound.effectiveCap(ColonyStage.FOUNDING, 4), "A configured bound below a threshold deliberately holds that stage");
        assertEquals(30, AdultBound.effectiveCap(ColonyStage.GREAT, 30));
        assertFalse(AdultBound.valid(3)); assertFalse(AdultBound.valid(121)); assertTrue(AdultBound.valid(4)); assertTrue(AdultBound.valid(120));
        assertThrows(IllegalArgumentException.class, () -> AdultBound.effectiveCap(ColonyStage.YOUNG, 121));
    }

    /** Inputs whose tiers are given per function as bounds (a chamber still confirmed at a lower tier, or unknown). */
    private static Inputs tiered(Bound adults, Bound nursery, Bound foodStore, Bound materialStore, Bound hall, Bound food, int clay, int stone, boolean queenDead) {
        Map<ChamberFunction, Bound> tiers = new EnumMap<>(ChamberFunction.class);
        tiers.put(ChamberFunction.NURSERY, nursery); tiers.put(ChamberFunction.FOOD_STORE, foodStore);
        tiers.put(ChamberFunction.MATERIAL_STORE, materialStore); tiers.put(ChamberFunction.QUEENS_HALL, hall);
        return new Inputs(adults, tiers, food, Bound.exactly(clay), Bound.exactly(stone), queenDead);
    }

    @Test
    void aShortfallThatIsNotCatastrophicDropsItsStageAfterExactly24000LoadedTicks() {
        assertEquals(24_000, StageRules.GRACE, "the owner's grace: one Minecraft day of loaded ticks");
        // Each delayed shortfall the owner listed, from the stage it holds: counts, stocks and tiers.
        record Case(String name, ColonyStage held, Inputs in, String key, ColonyStage after) { }
        var cases = List.of(
            new Case("food 3/4", ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 3, 16, 0), "mature:food", ColonyStage.YOUNG),
            new Case("clay 15/16", ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 4, 15, 0), "mature:clay", ColonyStage.YOUNG),
            new Case("stone 31/32", ColonyStage.GREAT, inputs(60, 2, 2, 2, 2, 4, 16, 31), "great:stone", ColonyStage.MATURE),
            new Case("nursery confirmed at tier 1", ColonyStage.GREAT, inputs(60, 1, 2, 2, 2, 4, 16, 32), "great:nursery_tier", ColonyStage.MATURE),
            new Case("hall confirmed at tier 1", ColonyStage.GREAT, inputs(60, 2, 2, 2, 1, 4, 16, 32), "great:queens_hall_tier", ColonyStage.MATURE),
            new Case("adults 24/25, not below Young's 5", ColonyStage.MATURE, inputs(24, 1, 1, 1, 1, 4, 16, 0), "mature:adults", ColonyStage.YOUNG),
            new Case("adults 5/25, Young's own 5", ColonyStage.MATURE, inputs(5, 1, 1, 1, 1, 4, 16, 0), "mature:adults", ColonyStage.YOUNG),
            new Case("adults 25/50, Mature's own 25", ColonyStage.GREAT, inputs(25, 2, 2, 2, 2, 4, 16, 32), "great:adults", ColonyStage.MATURE),
            new Case("adults 4/5, not below Founding's 1", ColonyStage.YOUNG, inputs(4, 1, 1, 0, 0, 0, 0, 0), "young:adults", ColonyStage.FOUNDING),
            new Case("the queen alone", ColonyStage.YOUNG, inputs(1, 1, 1, 0, 0, 0, 0, 0), "young:adults", ColonyStage.FOUNDING));
        for (var c : cases) {
            long start = 1_000;
            var first = StageRules.evaluate(c.held(), c.in(), Map.of(), start);
            assertEquals(c.held(), first.stage(), c.name() + ": held at its first observation");
            assertEquals(Map.of(c.key(), start), first.unmetSince(), c.name() + ": its clock starts at this loaded tick");
            assertEquals(List.of(c.key() + " "), first.pending().stream().map(p -> p.requirement().key() + " ").toList(), c.name());
            var before = StageRules.evaluate(c.held(), c.in(), first.unmetSince(), start + StageRules.GRACE - 1);
            assertEquals(c.held(), before.stage(), c.name() + ": still held one loaded tick before the grace");
            assertEquals(first.unmetSince(), before.unmetSince(), c.name() + ": the clock keeps its start");
            var at = StageRules.evaluate(c.held(), c.in(), before.unmetSince(), start + StageRules.GRACE);
            assertEquals(c.after(), at.stage(), c.name() + ": dropped at exactly the grace");
            assertFalse(at.unmetSince().containsKey(c.key()), c.name() + ": a lost stage's clock ends with it");
        }
        // Met again in between: the clock clears, and a new shortfall starts a new one.
        var short3 = inputs(30, 1, 1, 1, 1, 3, 16, 0); var full = inputs(30, 1, 1, 1, 1, 4, 16, 0);
        var dip = StageRules.evaluate(ColonyStage.MATURE, short3, Map.of(), 0);
        var refilled = StageRules.evaluate(ColonyStage.MATURE, full, dip.unmetSince(), 20_000);
        assertTrue(refilled.unmetSince().isEmpty() && refilled.pending().isEmpty() && refilled.stage() == ColonyStage.MATURE, "food back at four clears the clock");
        var again = StageRules.evaluate(ColonyStage.MATURE, short3, refilled.unmetSince(), 20_100);
        assertEquals(Map.of("mature:food", 20_100L), again.unmetSince());
        assertEquals(ColonyStage.MATURE, StageRules.evaluate(ColonyStage.MATURE, short3, again.unmetSince(), 24_000).stage(), "the earlier dip no longer counts");
        assertEquals(ColonyStage.YOUNG, StageRules.evaluate(ColonyStage.MATURE, short3, again.unmetSince(), 44_100).stage());
        // Two shortfalls: the earliest clock decides; a shortfall of a lower stage keeps running after the drop.
        var both = inputs(4, 1, 1, 1, 1, 3, 16, 0); // Mature short of food since 0, of adults (4 < 25 and < Young's 5: at once)
        assertEquals(ColonyStage.FOUNDING, StageRules.evaluate(ColonyStage.MATURE, both, Map.of(), 0).stage(), "below the lower stage's threshold is never delayed");
        var twoClocks = StageRules.evaluate(ColonyStage.MATURE, inputs(20, 1, 1, 1, 1, 3, 16, 0), Map.of("mature:food", 0L), 10_000);
        assertEquals(Map.of("mature:food", 0L, "mature:adults", 10_000L), twoClocks.unmetSince());
        assertEquals(ColonyStage.YOUNG, StageRules.evaluate(ColonyStage.MATURE, inputs(20, 1, 1, 1, 1, 3, 16, 0), twoClocks.unmetSince(), 24_000).stage(), "the food clock started first");
        var lower = StageRules.evaluate(ColonyStage.MATURE, inputs(4, 1, 1, 1, 1, 4, 16, 0), Map.of("young:adults", 0L, "mature:adults", 0L), 100);
        assertEquals(ColonyStage.FOUNDING, lower.stage(), "four adults are below Young's five: Mature's loss applies at once, to what four adults support");
    }

    @Test
    void everyCatastrophicLossAppliesAtOnce() {
        var one = Bound.exactly(1); var two = Bound.exactly(2); var none = Bound.NONE;
        // The queen observed dead is catastrophic on its own (the owner's rule, stage-1 T08): Founding at once, whatever
        // else still holds; Young counts her among its five adults.
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.YOUNG, tiered(Bound.exactly(4), one, one, none, none, none, 0, 0, false)));
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, tiered(Bound.exactly(4), one, one, none, none, none, 0, 0, true)), "four workers and a dead queen");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.MATURE, tiered(Bound.exactly(30), one, one, one, one, Bound.exactly(3), 16, 0, true)), "food short with the queen dead: Founding, not Young");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, tiered(Bound.exactly(5), one, one, none, none, none, 0, 0, true)), "a dead queen alone drops Young to Founding: five workers are not Young's queen and four workers");
        // A chamber function observed absent: no confirmed or unknown chamber holds it.
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, inputs(30, 1, 0, 0, 0, 0, 0, 0)), "the food store's cache destroyed");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, inputs(30, 0, 1, 0, 0, 0, 0, 0)), "the nursery's shell breached");
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.MATURE, inputs(30, 1, 1, 1, 0, 4, 16, 0)), "the hall without its living queen");
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.MATURE, inputs(30, 1, 1, 0, 1, 4, 16, 0)), "the material store's marker missing or foreign");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.GREAT, inputs(60, 0, 2, 2, 2, 4, 16, 32)), "a Great colony that loses its nursery drops to Founding");
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.GREAT, tiered(Bound.exactly(60), two, two, two, none, Bound.exactly(4), 16, 32, false)), "Great's hall absent: Mature is lost too");
        assertEquals(ColonyStage.GREAT, stage(ColonyStage.GREAT, tiered(Bound.exactly(60), two, two, two, one, Bound.exactly(4), 16, 32, false)), "a hall still confirmed at tier 1 is a tier shortfall, not a loss");
        // Adults certainly below the threshold of the stage beneath the one held: the colony drops to what they support.
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.MATURE, inputs(4, 1, 1, 1, 1, 4, 16, 0)), "Mature at four adults, below Young's five");
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.GREAT, inputs(24, 2, 2, 2, 2, 4, 16, 32)), "Great at 24 adults, below Mature's 25: Young supports them");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.GREAT, inputs(4, 2, 2, 2, 2, 4, 16, 32)), "Great at four adults");
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, inputs(Bound.NONE, 1, 1, 0, 0, Bound.NONE, 0, 0)), "Young with no adult at all, below Founding's one");
        assertEquals(ColonyStage.GREAT, stage(ColonyStage.GREAT, inputs(new Bound(20, 30), 2, 2, 2, 2, Bound.exactly(4), 16, 32)), "unknown adults that could reach Mature's 25 never apply at once");
        // Catastrophic means no clock: a running clock is never needed and none is kept for a lost stage.
        var lost = StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 0, 3, 16, 0), Map.of("mature:food", 5L), 10);
        assertTrue(lost.stage() == ColonyStage.YOUNG && lost.unmetSince().isEmpty() && lost.pending().isEmpty(), "Mature lost at once with its hall; its food clock ends with it: " + lost.unmetSince());
    }

    /** The same inputs with the queen observed dead. */
    private static Inputs deadQueen(Inputs in) { return new Inputs(in.adults(), in.tiers(), in.food(), in.clay(), in.stone(), true); }

    @Test
    void theQueensDeathAloneDropsEveryHeldStageToFoundingAtOnceAndNothingPromotesTheColony() {
        // Stage-1 T08: every requirement of the held stage still met, only the queen observed dead.
        for (var held : List.of(ColonyStage.YOUNG, ColonyStage.MATURE, ColonyStage.GREAT)) {
            int adults = held == ColonyStage.GREAT ? 60 : held == ColonyStage.MATURE ? 30 : 6;
            var alive = StageRules.evaluate(held, meeting(held, adults), Map.of(), 1_000);
            assertEquals(held, alive.stage(), held + " is met while the queen lives");
            var dead = StageRules.evaluate(held, deadQueen(meeting(held, adults)), allClocksAt(0), 1_000);
            assertEquals(ColonyStage.FOUNDING, dead.stage(), held + ": the queen's death drops the colony to Founding at once");
            assertEquals(ColonyStage.FOUNDING, dead.certain(), held + ": nothing above Founding is certain without the queen");
            assertEquals(ColonyStage.FOUNDING, dead.possible(), held + ": nor possible");
            assertTrue(dead.unmetSince().isEmpty() && dead.pending().isEmpty(), held + ": catastrophic, so no clock holds or survives: " + dead.unmetSince());
            assertEquals(ColonyStage.FOUNDING, StageRules.evaluate(held, deadQueen(meeting(held, adults))).stage(), held + ": at a first observation too");
        }
        // A Founding colony whose queen is dead is never promoted, however many workers, chambers and stocks it holds.
        for (var target : List.of(ColonyStage.YOUNG, ColonyStage.MATURE, ColonyStage.GREAT)) {
            var r = StageRules.evaluate(ColonyStage.FOUNDING, deadQueen(meeting(target, 120)), Map.of(), 0);
            assertEquals(ColonyStage.FOUNDING, r.stage(), "no promotion to " + target + " with the queen dead");
            assertTrue(r.unmetSince().isEmpty(), "no clock");
        }
        // The catastrophic predicate agrees for every row of the table.
        var in = deadQueen(meeting(ColonyStage.GREAT, 60));
        for (var r : StageRules.TABLE) assertTrue(StageRules.catastrophic(r, r.have(in), in, ColonyStage.GREAT), "the queen observed dead is catastrophic for " + r);
    }

    @Test
    void promotionStaysImmediateAndUnknownsNeitherDemoteNorRunAClock() {
        // Promotion: the first evaluation that certainly meets a stage, whatever clocks a lower shortfall left.
        var stale = Map.of("young:adults", 0L);
        var promoted = StageRules.evaluate(ColonyStage.YOUNG, meeting(ColonyStage.MATURE, 25), stale, 50_000);
        assertTrue(promoted.stage() == ColonyStage.MATURE && promoted.unmetSince().isEmpty(), "Young to Mature at once, clocks cleared: " + promoted.unmetSince());
        assertEquals(ColonyStage.GREAT, StageRules.evaluate(ColonyStage.FOUNDING, meeting(ColonyStage.GREAT, 50), Map.of(), 7).stage(), "every met stage at once");
        // A held stage's clock clears the moment the requirement is met again; nothing else changes.
        var dip = StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 3, 16, 0), Map.of(), 100);
        assertEquals(ColonyStage.MATURE, StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 4, 16, 0), dip.unmetSince(), 100 + StageRules.GRACE * 3).stage(), "a long-ago dip, met again, never demotes");
        // Unknown terrain or members never demote and run no clock; the clock starts once the shortfall is certain.
        Map<ChamberFunction, Bound> tiers = new EnumMap<>(ChamberFunction.class);
        for (var f : ChamberFunction.values()) tiers.put(f, Bound.exactly(1));
        var cacheUnloaded = new Inputs(Bound.exactly(30), tiers, new Bound(0, 6), Bound.exactly(16), Bound.NONE);
        var held = StageRules.evaluate(ColonyStage.MATURE, cacheUnloaded, Map.of("mature:food", 0L), 100_000);
        assertTrue(held.stage() == ColonyStage.MATURE && held.unmetSince().isEmpty(), "an unloaded cache neither demotes nor keeps a clock: " + held.unmetSince());
        var reloaded = StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 3, 16, 0), held.unmetSince(), 100_100);
        assertEquals(Map.of("mature:food", 100_100L), reloaded.unmetSince(), "the clock starts when the shortfall is certain again");
        assertEquals(ColonyStage.MATURE, StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 3, 16, 0), reloaded.unmetSince(), 100_100 + StageRules.GRACE - 1).stage());
        var someUnloaded = inputs(new Bound(20, 26), 1, 1, 1, 1, Bound.exactly(4), 16, 0);
        var away = StageRules.evaluate(ColonyStage.MATURE, someUnloaded, Map.of(), 0);
        assertTrue(away.stage() == ColonyStage.MATURE && away.unmetSince().isEmpty(), "20 known and 6 unknown adults may be Mature's 25: no clock");
        assertEquals(ColonyStage.MATURE, StageRules.evaluate(ColonyStage.MATURE, someUnloaded, away.unmetSince(), StageRules.GRACE * 10).stage(), "unknown adults never demote, however long");
        Map<ChamberFunction, Bound> unknownNursery = new EnumMap<>(ChamberFunction.class);
        unknownNursery.put(ChamberFunction.NURSERY, new Bound(0, 1)); unknownNursery.put(ChamberFunction.FOOD_STORE, Bound.exactly(1));
        var terrain = StageRules.evaluate(ColonyStage.YOUNG, new Inputs(Bound.exactly(4), unknownNursery, Bound.NONE, Bound.NONE, Bound.NONE), Map.of(), 0);
        assertEquals(Map.of("young:adults", 0L), terrain.unmetSince(), "only the certain shortfall runs a clock, never the unknown nursery");
        // A clock saved ahead of the loaded-tick counter (a replaced nursery) starts again now rather than demoting early.
        var ahead = StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 3, 16, 0), Map.of("mature:food", 90_000L), 500);
        assertEquals(Map.of("mature:food", 500L), ahead.unmetSince());
    }

    @Test
    void unmetSinceClocksSurviveSaveAndReload() {
        var queen = java.util.UUID.fromString("00000000-0000-0000-0000-000000000007");
        var plan = dev.primeants.founding.NestPlan.geometry(new net.minecraft.core.BlockPos(100, 64, -40), net.minecraft.core.Direction.EAST);
        var registry = new ChamberRegistry(); registry.found(queen, plan);
        var dip = StageRules.evaluate(ColonyStage.MATURE, inputs(30, 1, 1, 1, 1, 3, 16, 0), Map.of(), 1_000);
        registry.stage(queen, dip.stage(), dip.unmetSince());
        var json = ChamberRegistry.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, registry).getOrThrow();
        assertEquals(1_000L, json.getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("unmet_since").get("mature:food").getAsLong(), "saved as unmet_since: " + json);
        var loaded = ChamberRegistry.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow().colony(queen);
        assertEquals(registry.colony(queen), loaded, "the colony, its stage and its clocks reload exactly");
        // The reloaded clock keeps counting from its saved start in the nursery's loaded ticks.
        assertEquals(ColonyStage.MATURE, StageRules.evaluate(loaded.stage(), inputs(30, 1, 1, 1, 1, 3, 16, 0), loaded.unmetSince(), 1_000 + StageRules.GRACE - 1).stage());
        assertEquals(ColonyStage.YOUNG, StageRules.evaluate(loaded.stage(), inputs(30, 1, 1, 1, 1, 3, 16, 0), loaded.unmetSince(), 1_000 + StageRules.GRACE).stage());
        // A save from before the hysteresis has no clocks; a save naming an unknown requirement or a negative tick is refused.
        var old = json.deepCopy().getAsJsonArray(); old.get(0).getAsJsonObject().remove("unmet_since");
        assertTrue(ChamberRegistry.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow().colony(queen).unmetSince().isEmpty());
        for (var bad : List.of("{\"mature:honey\":5}", "{\"mature:food\":-1}")) {
            var broken = json.deepCopy().getAsJsonArray(); broken.get(0).getAsJsonObject().add("unmet_since", com.google.gson.JsonParser.parseString(bad));
            assertThrows(IllegalArgumentException.class, () -> ChamberRegistry.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, broken).getOrThrow(), bad);
        }
    }

    @Test
    void legacy010CapacityMigratesToAnUpperBound() {
        assertEquals(120, AdultBound.fromLegacy(30), "0.1.0's default and maximum 30 meant the full cap");
        assertEquals(29, AdultBound.fromLegacy(29)); assertEquals(4, AdultBound.fromLegacy(4));
        assertEquals(60, AdultBound.effectiveCap(ColonyStage.MATURE, AdultBound.fromLegacy(30)), "A migrated 0.1.0 colony can pass 30 once Mature is reachable");
        assertThrows(IllegalArgumentException.class, () -> AdultBound.fromLegacy(31), "0.1.0's own validator range stays closed");
        assertThrows(IllegalArgumentException.class, () -> AdultBound.fromLegacy(3));
    }
}
