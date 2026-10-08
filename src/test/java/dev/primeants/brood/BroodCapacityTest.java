package dev.primeants.brood;

import dev.primeants.colony.ColonyStage;
import dev.primeants.colony.StageRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The brood-capacity counterpart of T01's no-deadlock check: at default timing, what each stage can physically build
 * sustains its next adult threshold (and Great its own cap) by workers alone. */
class BroodCapacityTest {
    private static double workers(BroodCapacity.Nursery n) { return BroodCapacity.sustainedWorkers(n); }

    @Test
    void everyStagesNextThresholdIsSustainedByWhatThatStageCanBuild() {
        for (var s : ColonyStage.values()) {
            var n = BroodCapacity.buildable(s);
            assertTrue(workers(n) >= BroodCapacity.target(s), s + " builds " + n + ", sustaining " + workers(n) + " < " + BroodCapacity.target(s));
            // slots x speed against the brief's figure: target / (lifespan / brood time) = target / 4
            assertTrue(n.slots() * n.rate() >= BroodCapacity.target(s) / 4.0, s + ": slots x speed " + n.slots() * n.rate());
        }
        assertEquals(5, BroodCapacity.target(ColonyStage.FOUNDING)); assertEquals(25, BroodCapacity.target(ColonyStage.YOUNG));
        assertEquals(50, BroodCapacity.target(ColonyStage.MATURE)); assertEquals(120, BroodCapacity.target(ColonyStage.GREAT));
        assertEquals(StageRules.minAdults(ColonyStage.MATURE), BroodCapacity.target(ColonyStage.YOUNG), "thresholds come from the one stage table");
    }

    @Test
    void theModelsNumbersAtDefaultTiming() {
        assertEquals(144_000, BroodCapacity.LIFESPAN); assertEquals(36_000, BroodCapacity.STAGES * BroodCapacity.STAGE_TICKS); assertEquals(1_200, BroodCapacity.LAYING_TICKS);
        assertEquals(new BroodCapacity.Nursery(3, 2), BroodCapacity.buildable(ColonyStage.FOUNDING));
        assertEquals(new BroodCapacity.Nursery(7, 2), BroodCapacity.buildable(ColonyStage.YOUNG), "the queen's hall: +4 slots");
        assertEquals(new BroodCapacity.Nursery(10, 3), BroodCapacity.buildable(ColonyStage.MATURE), "tier-2 walls: +3 slots, x1.5");
        assertEquals(new BroodCapacity.Nursery(15, 4), BroodCapacity.buildable(ColonyStage.GREAT), "tier-3 walls: +8 slots, x2");
        assertEquals(12.0, workers(BroodCapacity.buildable(ColonyStage.FOUNDING)), 1e-9);
        assertEquals(28.0, workers(BroodCapacity.buildable(ColonyStage.YOUNG)), 1e-9);
        assertEquals(60.0, workers(BroodCapacity.buildable(ColonyStage.MATURE)), 1e-9);
        assertEquals(120.0, workers(BroodCapacity.buildable(ColonyStage.GREAT)), 1e-9);
        assertEquals(BroodCapacity.LIFESPAN / BroodCapacity.LAYING_TICKS, 120, "the laying limit binds only at the top tier");
    }

    @Test
    void eachCauseIsNeededForTheNextThreshold() {
        // Without the hall a Young colony stalls below Mature; without tier-2 walls a Mature one stalls below Great.
        assertTrue(workers(BroodCapacity.nursery(false, 1)) < 25);
        assertTrue(workers(BroodCapacity.nursery(true, 1)) < 50);
        assertTrue(workers(BroodCapacity.nursery(false, 2)) < 50, "tier-2 walls alone are not enough either");
        assertTrue(workers(BroodCapacity.nursery(true, 2)) < 120);
        assertEquals(BroodCapacity.nursery(false, 1), BroodCapacity.nursery(false, 0), "an unconfirmed nursery counts as earth");
    }

    @Test
    void aFasterNurseryTakesExactStepsPerTickForRunningAndNewStages() {
        // The pile's own arithmetic: a 120-step stage (brood x100) takes 120, 80 and 60 loaded ticks at tiers 1, 2 and 3.
        int[] expected = {0, 120, 80, 60};
        for (int tier = 1; tier <= 3; tier++) {
            var n = BroodCapacity.nursery(true, tier); int credit = 0, progress = 0, ticks = 0;
            while (progress < 120) { progress += BroodCapacity.steps(credit, n); credit = BroodCapacity.credit(credit, n); ticks++; }
            assertEquals(expected[tier], ticks, "tier " + tier);
            assertEquals(120, progress, "no step past the stage end");
        }
        // A running stage speeds up the moment the tier rises: 60 steps at tier 1, then the last 60 at tier 2 in 40 ticks.
        var slow = BroodCapacity.nursery(true, 1); var fast = BroodCapacity.nursery(true, 2); int credit = 0, progress = 0, ticks = 0;
        while (progress < 60) { progress += BroodCapacity.steps(credit, slow); credit = BroodCapacity.credit(credit, slow); ticks++; }
        while (progress < 120) { progress += BroodCapacity.steps(credit, fast); credit = BroodCapacity.credit(credit, fast); ticks++; }
        assertEquals(100, ticks);
    }
    @Test
    void theConfirmedHallOwnsTheLayingCadenceAndTheSustainedCalculationUsesIt() {
        assertEquals(1200,BroodCapacity.layingTicks(0));assertEquals(1200,BroodCapacity.layingTicks(1));assertEquals(600,BroodCapacity.layingTicks(2));
        var scale=new dev.primeants.time.SimulationTimeScale(100);
        assertEquals(12,scale.ticksForGameDays(BroodCapacity.layingTicks(1)/24000.0));
        assertEquals(6,scale.ticksForGameDays(BroodCapacity.layingTicks(2)/24000.0));
        var n=BroodCapacity.nursery(true,2);
        assertEquals(60,BroodCapacity.sustainedWorkers(n,2));
        assertEquals(120,BroodCapacity.sustainedWorkers(n,144000,1,BroodCapacity.layingTicks(1)));
        assertEquals(240,BroodCapacity.sustainedWorkers(n,144000,1,BroodCapacity.layingTicks(2)),"The cadence ceiling actually doubles when it binds");
        for(var s:ColonyStage.values())assertTrue(BroodCapacity.sustainedWorkers(BroodCapacity.buildable(s),BroodCapacity.buildableHallTier(s))>=BroodCapacity.target(s));
    }

}
