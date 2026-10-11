package dev.primeants.time;

import java.util.OptionalLong;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A nursery's saved observation boundary, not proof that any gameplay catch-up was applied. */
public final class ColonyClock {
    public static final String FIELD = "LastSimulationGameTime";
    private OptionalLong last = OptionalLong.empty();

    /** Both endpoints are exclusive: the last ordinary step and the current live tick are excluded. */
    public record Interval(OptionalLong afterGameTime, long beforeGameTime, long ticks) { }

    public OptionalLong lastGameTime() { return last; }

    /** Legacy/new ownership baseline, once a server level is available; never waits for operational terrain. */
    public boolean initialize(long gameTime) {
        if (last.isPresent()) return false;
        last = OptionalLong.of(gameTime);
        return true;
    }

    /** Constant cost, with no mutation, replay or lifecycle inference. Negative/reversed clocks owe nothing. */
    public Interval before(long gameTime) {
        return new Interval(last, gameTime, last.isPresent() ? missedTicks(last.getAsLong(), gameTime) : 0);
    }

    public static long missedTicks(long after, long before) {
        // Ordered nonnegative endpoints make subtraction safe, even at Long.MAX_VALUE.
        return after >= 0 && before > after ? before - after - 1 : 0;
    }

    /** Called only after a completed ordinary nursery step. A reversed timeline rebases here, never on inspection. */
    public void completed(long gameTime) { last = OptionalLong.of(gameTime); }

    public void save(ValueOutput out) { last.ifPresent(value -> out.putLong(FIELD, value)); }
    public void load(ValueInput in) {
        var value = in.getLong(FIELD);
        last = value.isPresent() && value.get() >= 0 ? OptionalLong.of(value.get()) : OptionalLong.empty();
    }
}
