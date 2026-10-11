package dev.primeants.time;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Evidence of completed biology, not age or an applied catch-up ledger. No unloaded ticking. */
public final class AdultSimulation {
    public static final String FIELD = "AdultSimulation";
    public static final int MAX_RUNS = 32;
    public enum Origin { BIRTH, LEGACY_LOAD, REBASED }
    /** Inclusive completed game-time ranges; gaps are actual non-observation, never inferred continuous. */
    public record Run(long first, long last) {
        static final Codec<Run> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("first").forGetter(Run::first),
            Codec.LONG.fieldOf("last").forGetter(Run::last)).apply(i, Run::new));
    }
    public record Coverage(String status, OptionalLong alreadySimulated, OptionalLong missed, long beforeBoundary) {
        static Coverage unresolved(String reason) {
            return new Coverage(reason, OptionalLong.empty(), OptionalLong.empty(), 0);
        }
    }
    /** Immutable; evidence strictly before knownAfter is explicitly unavailable after compaction/rebase. */
    public record Snapshot(long boundary, Origin origin, long knownAfter, long latest, boolean invalidTime, List<Run> runs) {
        public Snapshot { runs = List.copyOf(runs); }
        public ColonyClock.Interval ownIntervalBefore(long now) {
            return new ColonyClock.Interval(OptionalLong.of(latest), now, ColonyClock.missedTicks(latest, now));
        }
        public Coverage reconcile(ColonyClock.Interval nursery) {
            if (nursery.afterGameTime().isEmpty()) return Coverage.unresolved("UNKNOWN_NURSERY_BOUNDARY");
            long after = nursery.afterGameTime().getAsLong(), before = nursery.beforeGameTime();
            long total = ColonyClock.missedTicks(after, before);
            if (total == 0) return new Coverage("RESOLVED", OptionalLong.of(0), OptionalLong.of(0), 0);
            if (invalidTime) return Coverage.unresolved("UNKNOWN_NEGATIVE_TIME_DOMAIN");
            long eligibleAfter = origin == Origin.BIRTH ? Math.max(after, boundary) : after;
            long eligible = ColonyClock.missedTicks(eligibleAfter, before);
            if (eligible > 0 && eligibleAfter < knownAfter) return Coverage.unresolved("UNKNOWN_EVIDENCE_BEFORE_" + knownAfter);
            long simulated = 0;
            // At most MAX_RUNS, regardless of missed duration. Current live tick C is always excluded.
            if (eligible > 0) for (Run run : runs) {
                long first = Math.max(eligibleAfter + 1, run.first()), last = Math.min(before - 1, run.last());
                if (last >= first) simulated += last - first + 1;
            }
            return new Coverage("RESOLVED", OptionalLong.of(simulated), OptionalLong.of(eligible - simulated), total - eligible);
        }
    }
    public record Boundary(ColonyClock.Interval ownInterval, Snapshot evidence) { }

    private long boundary, knownAfter;
    private Origin origin;
    private ColonyClock clock = new ColonyClock();
    private final ArrayList<Run> runs = new ArrayList<>();
    private Boundary lastBoundary;
    private boolean pendingBoundary = true;
    private boolean invalidTime;

    public void initialize(long now, Origin kind) {
        if (clock.lastGameTime().isPresent()) return;
        boundary = knownAfter = now; origin = kind; clock.initialize(now);
    }
    public Snapshot snapshot() {
        return new Snapshot(boundary, origin, knownAfter, clock.lastGameTime().orElseThrow(), invalidTime, runs);
    }
    public Boundary lastBoundary() { return lastBoundary; }
    public void beforeStep(long now) {
        var interval = clock.before(now);
        if (pendingBoundary || interval.ticks() > 0 || now < clock.lastGameTime().orElseThrow()) {
            lastBoundary = new Boundary(interval, snapshot()); pendingBoundary = false;
        }
    }
    /** Duplicate observations add no ticks. Reversal discards the old time domain explicitly. */
    public void completed(long now) {
        if (now < 0) { invalidTime = true; return; } // Persist uncertainty, never a fabricated nonnegative step.
        long latest = clock.lastGameTime().orElseThrow();
        if (invalidTime || now < latest) {
            runs.clear(); boundary = knownAfter = now; origin = Origin.REBASED;
            invalidTime = false;
        }
        if (!runs.isEmpty() && runs.getLast().last() == now) return;
        if (!runs.isEmpty() && now > 0 && runs.getLast().last() == now - 1) {
            Run old = runs.removeLast(); runs.add(new Run(old.first(), now));
        } else {
            if (runs.size() == MAX_RUNS) {
                knownAfter = Math.max(knownAfter, runs.removeFirst().last());
            }
            runs.add(new Run(now, now));
        }
        clock.completed(now);
    }
    public void save(ValueOutput out) {
        out.putInt("Version", 1); out.putLong("Boundary", boundary); out.putString("Origin", origin.name());
        out.putLong("KnownAfter", knownAfter); clock.save(out); out.store("Runs", Run.CODEC.listOf(), runs);
        out.putBoolean("InvalidTime", invalidTime);
    }
    public void load(ValueInput in, long now) {
        runs.clear(); lastBoundary = null; pendingBoundary = true; clock.load(in);
        invalidTime = in.getBooleanOr("InvalidTime", false);
        if (in.getIntOr("Version", 0) == 0) {
            // Entity load already has a ServerLevel. This does not touch any birth policy or biology.
            clock = new ColonyClock(); initialize(now, Origin.LEGACY_LOAD); return;
        }
        if (in.getIntOr("Version", 0) != 1 || clock.lastGameTime().isEmpty()) throw new IllegalArgumentException("Invalid adult simulation schema");
        boundary = in.getLongOr("Boundary", -1); knownAfter = in.getLongOr("KnownAfter", -1);
        origin = Origin.valueOf(in.getStringOr("Origin", ""));
        var saved = in.read("Runs", Run.CODEC.listOf()).orElseThrow();
        long previous = -1, latest = clock.lastGameTime().orElseThrow();
        if (boundary < 0 || knownAfter < boundary || latest < knownAfter || saved.size() > MAX_RUNS) throw new IllegalArgumentException("Invalid adult simulation boundaries");
        for (Run run : saved) {
            if (run.first() < 0 || run.last() < run.first() || run.last() > latest || run.first() <= previous) throw new IllegalArgumentException("Invalid adult simulation runs");
            previous = run.last(); runs.add(run);
        }
    }
}
