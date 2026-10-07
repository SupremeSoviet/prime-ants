package dev.primeants.brood;

import dev.primeants.colony.ColonyStage;
import dev.primeants.colony.StageRules;

/** Brood capacity (GDD v2 section 2, stage-1 T05): how many brood a colony's one nursery pile keeps at once (slots) and
 * how fast they develop (speed), by physical cause, and the population that sustains. Pure, so the unit test and the
 * pile share it.
 * <pre>
 * Built by   Cause                                     Slots  Speed  Workers sustained  Needed
 * Founding   founding chamber, earth walls (0.1.0)          3   x1.0       12           Young 5
 * Young      + the queen's hall beside her chamber (+4)     7   x1.0       28           Mature 25 (cap 30)
 * Mature     + packed-clay nursery walls, tier 2 (+3)      10   x1.5       60           Great 50 (cap 60)
 * Great      + nest-cut stone nursery walls, tier 3 (+8)   15   x2.0      120           its cap 120 (T06)
 * </pre>
 * Workers sustained = slots x speed x lifespan / brood time, but never more than lifespan / laying cadence (the hall's
 * laying limit, which T06 may raise): at default timing a worker lives 144,000 ticks, a brood takes 36,000 ticks (three
 * 12,000-tick stages) and the queen lays at most one egg per 1,200 ticks, so slots x speed must reach 6.25, 12.5 and 30
 * for 25, 50 and 120 workers. Every adult threshold is met by workers alone, without counting the queen. */
public final class BroodCapacity {
    /** The founding clutch and the 0.1.0 nursery's slots. */
    public static final int FOUNDING_SLOTS = 3;
    /** Room the queen's hall gives the brood around her: the hall enlarges her own chamber, where the pile lies, so the pile
     * and its eggs stay within her reach. */
    public static final int HALL_SLOTS = 4;
    /** Extra slots by nursery wall tier 1..3. */
    private static final int[] TIER_SLOTS = {0, 0, 3, 8};
    /** Development steps per SPEED_DIVISOR loaded ticks, by nursery wall tier 1..3: x1, x1.5, x2. */
    private static final int[] TIER_SPEED = {0, 2, 3, 4};
    public static final int SPEED_DIVISOR = 2, MAX_SLOTS = FOUNDING_SLOTS + HALL_SLOTS + 8;
    /** Default timing (decision 21): adult lifespan, one brood stage, and the queen's laying cadence, in loaded ticks. */
    public static final long LIFESPAN = dev.primeants.entity.AdultLife.DEFAULT_LIFESPAN, STAGE_TICKS = 12_000, LAYING_TICKS = BroodPile.BASE_LAYING_TICKS, STAGES = 3;
    public record Nursery(int slots, int speed) {
        public Nursery { if (slots < FOUNDING_SLOTS || slots > MAX_SLOTS || speed < SPEED_DIVISOR) throw new IllegalArgumentException("Invalid nursery " + slots + " x" + speed); }
        /** Development steps per loaded tick, on average. */
        public double rate() { return speed / (double) SPEED_DIVISOR; }
    }
    private BroodCapacity() { }
    /** A nursery with or without the queen's hall confirmed, whose chamber is confirmed at this wall tier (1..3). */
    public static Nursery nursery(boolean hall, int tier) {
        int t = Math.max(1, Math.min(3, tier));
        return new Nursery(FOUNDING_SLOTS + (hall ? HALL_SLOTS : 0) + TIER_SLOTS[t], TIER_SPEED[t]);
    }
    /** What a stage can physically build toward its next threshold: Young digs the queen's hall, Mature unlocks tier-2
     * walls and Great tier-3 walls (GDD v2 section 1). Each stage keeps what the lower stages built. */
    public static Nursery buildable(ColonyStage s) {
        return switch (s) { case FOUNDING -> nursery(false, 1); case YOUNG -> nursery(true, 1); case MATURE -> nursery(true, 2); case GREAT -> nursery(true, 3); };
    }
    /** Whole development steps a loaded tick takes, given the credit left from earlier ticks (0..SPEED_DIVISOR-1). */
    public static int steps(int credit, Nursery n) { return (credit + n.speed()) / SPEED_DIVISOR; }
    /** The credit left after this tick's steps. */
    public static int credit(int credit, Nursery n) { return (credit + n.speed()) % SPEED_DIVISOR; }
    /** Workers one nursery sustains: brood finished per lifespan, never more than the eggs the queen can lay in one. */
    public static double sustainedWorkers(Nursery n, long lifespan, long stageTicks, long layingTicks) {
        double broodTicks = STAGES * stageTicks / n.rate();
        return Math.min(n.slots() * lifespan / broodTicks, lifespan / (double) layingTicks);
    }
    public static double sustainedWorkers(Nursery n) { return sustainedWorkers(n, LIFESPAN, STAGE_TICKS, LAYING_TICKS); }
    /** The workers a stage must sustain: its next adult threshold, and for Great its own adult cap. */
    public static int target(ColonyStage s) { return s.next() == null ? s.adultCap() : StageRules.minAdults(s.next()); }
    static {
        // No brood deadlock: at default timing what each stage can build sustains its next threshold by workers alone
        // (T01's adult-cap check, StageRules, is the counterpart for caps).
        for (var s : ColonyStage.values()) if (sustainedWorkers(buildable(s)) < target(s))
            throw new IllegalStateException("A " + s + " colony's nursery cannot sustain " + target(s) + " workers");
    }
}
