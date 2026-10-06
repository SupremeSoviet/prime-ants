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
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, inputs(4, 1, 1, 0, 0, 0, 0, 0)), "Known loss below five adults regresses");
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
        assertEquals("[adults 5/50, nursery_tier 1/2, food_store_tier 1/2, material_store_tier 0/2, queens_hall_tier 0/2, stone 0/32]", result.missing(ColonyStage.GREAT).toString());
        var founding = StageRules.evaluate(ColonyStage.FOUNDING, inputs(1, 1, 0, 0, 0, 0, 0, 0));
        assertEquals("[adults 1/5, food_store 0/1]", founding.missing(ColonyStage.YOUNG).toString());
        assertTrue(StageRules.evaluate(ColonyStage.GREAT, meeting(ColonyStage.GREAT, 120)).missing().isEmpty(), "Nothing is missing at the top stage");
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
        assertEquals(ColonyStage.FOUNDING, stage(ColonyStage.YOUNG, inputs(new Bound(3, 4), 1, 1, 0, 0, Bound.NONE, 0, 0)), "Known deaths below the threshold still regress");
        assertEquals(ColonyStage.YOUNG, stage(ColonyStage.FOUNDING, inputs(new Bound(5, 9), 1, 1, 0, 0, Bound.NONE, 0, 0)), "Confirmed adults promote regardless of unknown extras");
        var partlyUnloaded = inputs(new Bound(20, 40), 1, 1, 1, 1, Bound.exactly(4), 16, 0);
        assertEquals(ColonyStage.MATURE, stage(ColonyStage.MATURE, partlyUnloaded), "Twenty known plus twenty unknown may still be a Mature colony");
        assertEquals(ColonyStage.MATURE, stage(ColonyStage.GREAT, partlyUnloaded), "Unknowns cannot hold a stage above what they could support");
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
            var current = stage(from, meeting(target, 1));
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

    @Test
    void legacy010CapacityMigratesToAnUpperBound() {
        assertEquals(120, AdultBound.fromLegacy(30), "0.1.0's default and maximum 30 meant the full cap");
        assertEquals(29, AdultBound.fromLegacy(29)); assertEquals(4, AdultBound.fromLegacy(4));
        assertEquals(60, AdultBound.effectiveCap(ColonyStage.MATURE, AdultBound.fromLegacy(30)), "A migrated 0.1.0 colony can pass 30 once Mature is reachable");
        assertThrows(IllegalArgumentException.class, () -> AdultBound.fromLegacy(31), "0.1.0's own validator range stays closed");
        assertThrows(IllegalArgumentException.class, () -> AdultBound.fromLegacy(3));
    }
}
