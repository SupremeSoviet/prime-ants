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
        long ticks = 0;
        if (last.isPresent() && last.getAsLong() >= 0 && gameTime > last.getAsLong()) {
            // Ordered nonnegative endpoints make subtraction safe, even at Long.MAX_VALUE.
            ticks = gameTime - last.getAsLong() - 1;
        }
        return new Interval(last, gameTime, ticks);
    }

    /** Called only after a completed ordinary nursery step. A reversed timeline rebases here, never on inspection. */
    public void completed(long gameTime) { last = OptionalLong.of(gameTime); }

    public void save(ValueOutput out) { last.ifPresent(value -> out.putLong(FIELD, value)); }
    public void load(ValueInput in) {
        var value = in.getLong(FIELD);
        last = value.isPresent() && value.get() >= 0 ? OptionalLong.of(value.get()) : OptionalLong.empty();
    }
}
