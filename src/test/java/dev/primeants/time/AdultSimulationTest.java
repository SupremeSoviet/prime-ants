package dev.primeants.time;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdultSimulationTest {
    private static ColonyClock.Interval interval(long after,long before) {
        return new ColonyClock.Interval(OptionalLong.of(after),before,ColonyClock.missedTicks(after,before));
    }
    private static AdultSimulation born(long time) {
        var evidence=new AdultSimulation();evidence.initialize(time,AdultSimulation.Origin.BIRTH);return evidence;
    }
    @Test
    void independentlyCompletedTicksAndCurrentTickAreExcludedInBothOrders() {
        var adult=born(100);
        for(long tick=101;tick<=106;tick++)adult.completed(tick);
        var before=adult.snapshot().reconcile(interval(100,107));
        assertEquals(6,before.alreadySimulated().orElseThrow());assertEquals(0,before.missed().orElseThrow());
        adult.completed(107);
        assertEquals(before,adult.snapshot().reconcile(interval(100,107)));
    }
    @Test
    void recentIntermittentObservationKeepsExactMissingHoles() {
        var adult=born(90);adult.completed(100);adult.completed(102);adult.completed(106);
        var result=adult.snapshot().reconcile(interval(100,107));
        assertEquals(2,result.alreadySimulated().orElseThrow());assertEquals(4,result.missed().orElseThrow());
        assertEquals(0,adult.snapshot().ownIntervalBefore(107).ticks());
    }
    @Test
    void boundedCompactionExposesUnknownAndStillResolvesCoveredSuffix() {
        var adult=born(0);for(long tick=2;tick<=68;tick+=2)adult.completed(tick);
        assertEquals(32,adult.snapshot().runs().size());assertEquals(4,adult.snapshot().knownAfter());
        var lost=adult.snapshot().reconcile(interval(0,69));assertTrue(lost.missed().isEmpty());assertTrue(lost.alreadySimulated().isEmpty());
        var suffix=adult.snapshot().reconcile(interval(4,69));
        assertEquals(32,suffix.alreadySimulated().orElseThrow());assertEquals(32,suffix.missed().orElseThrow());
    }
    @Test
    void birthClipsPreBirthTimeWhileLegacyLoadCannotResolveIt() {
        var birth=born(100).snapshot().reconcile(interval(90,110));
        assertEquals(10,birth.beforeBoundary());assertEquals(9,birth.missed().orElseThrow());
        var legacy=new AdultSimulation();legacy.initialize(100,AdultSimulation.Origin.LEGACY_LOAD);
        assertTrue(legacy.snapshot().reconcile(interval(90,110)).missed().isEmpty());
    }
    @Test
    void maximumEndpointCountsWithoutOverflowOrReplay() {
        var adult=born(0);adult.completed(Long.MAX_VALUE-1);
        var result=adult.snapshot().reconcile(interval(0,Long.MAX_VALUE));
        assertEquals(1,result.alreadySimulated().orElseThrow());assertEquals(Long.MAX_VALUE-2,result.missed().orElseThrow());
    }
    @Test
    void duplicateAndReversedTimeDoNotInventCompletedTicks() {
        var adult=born(10);adult.completed(11);var once=adult.snapshot();adult.completed(11);assertEquals(once,adult.snapshot());
        adult.completed(5);assertEquals(AdultSimulation.Origin.REBASED,adult.snapshot().origin());
        assertTrue(adult.snapshot().reconcile(interval(0,20)).missed().isEmpty());
        adult.completed(-1);assertTrue(adult.snapshot().invalidTime());assertTrue(adult.snapshot().reconcile(interval(5,8)).missed().isEmpty());
        adult.completed(7);assertEquals(7,adult.snapshot().knownAfter());assertFalse(adult.snapshot().invalidTime());
        assertEquals(0,adult.snapshot().reconcile(interval(7,8)).missed().orElseThrow());
    }
}
