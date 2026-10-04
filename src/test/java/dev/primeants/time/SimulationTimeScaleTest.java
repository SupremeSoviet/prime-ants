package dev.primeants.time;

import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationTimeScaleTest {
    @Test
    void oneGameDayAtNormalSpeedTakes24000ElapsedTicks() {
        assertEquals(24_000, new SimulationTimeScale(1).ticksForGameDays(1));
    }

    @Test
    void twoGameDaysAtTwentyTimesSpeedTake2400ElapsedTicks() {
        assertEquals(2_400, new SimulationTimeScale(20).ticksForGameDays(2));
    }

    @Test
    void fractionalTickDoesNotCompleteEarly() {
        assertEquals(1, new SimulationTimeScale(1).ticksForGameDays(0.00001));
    }

    @Test
    void zeroDurationNeedsNoElapsedTicks() {
        assertEquals(0, new SimulationTimeScale(20).ticksForGameDays(0));
    }

    @ParameterizedTest(name = "rejects multiplier {0}")
    @MethodSource("invalidMultipliers")
    void rejectsInvalidMultiplier(double multiplier) {
        assertThrows(IllegalArgumentException.class, () -> new SimulationTimeScale(multiplier));
    }

    static DoubleStream invalidMultipliers() {
        return DoubleStream.of(0, -0.0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @ParameterizedTest(name = "rejects duration {0}")
    @MethodSource("invalidDurations")
    void rejectsInvalidDuration(double duration) {
        assertThrows(IllegalArgumentException.class, () -> new SimulationTimeScale(1).ticksForGameDays(duration));
    }

    static DoubleStream invalidDurations() {
        return DoubleStream.of(-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE);
    }
}
