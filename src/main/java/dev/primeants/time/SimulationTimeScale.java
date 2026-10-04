package dev.primeants.time;

/**
 * Converts a duration in game days to elapsed simulation ticks.
 * This has no dependency on world daylight time or commands that change it.
 * Founding, brood and callow durations use this conversion independently.
 */
public record SimulationTimeScale(double multiplier) {
    public static final long TICKS_PER_GAME_DAY = 24_000;

    public SimulationTimeScale {
        if (!Double.isFinite(multiplier) || multiplier <= 0) {
            throw new IllegalArgumentException("Multiplier must be finite and positive");
        }
    }

    /** Rounds a fractional tick up so a duration never completes early. */
    public long ticksForGameDays(double gameDays) {
        if (!Double.isFinite(gameDays) || gameDays < 0) {
            throw new IllegalArgumentException("Duration must be finite and nonnegative");
        }
        double ticks = gameDays * TICKS_PER_GAME_DAY / multiplier;
        if (!Double.isFinite(ticks) || ticks >= Long.MAX_VALUE) {
            throw new IllegalArgumentException("Duration exceeds the elapsed tick range");
        }
        return (long) Math.ceil(ticks);
    }
}
