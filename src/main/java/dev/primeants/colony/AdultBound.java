package dev.primeants.colony;

/** prime_ants.colonyAdultCapacity is only an upper bound on the stage cap (decision 17); the stage cap owns the limit. */
public final class AdultBound {
    public static final String PROPERTY = "prime_ants.colonyAdultCapacity";
    public static final int MIN = 4, MAX = ColonyStage.MAX_ADULT_CAP, LEGACY_MAX = 30;
    private AdultBound() { }
    /** A queen selects her bound at birth from the property, defaulting to the full stage range. */
    public static boolean valid(int bound) { return bound >= MIN && bound <= MAX; }
    /** 0.1.0 saved its fixed cap, whose default and maximum was 30. A saved 30 therefore meant "the full cap" and
     * becomes the full bound, so such colonies can pass 30 once a later stage allows it. A saved 4..29 was a
     * deliberate operator reduction and is kept. Anything outside 0.1.0's own 4..30 stays invalid. */
    public static int fromLegacy(int saved) {
        if (saved < MIN || saved > LEGACY_MAX) throw new IllegalArgumentException("Invalid saved 0.1.0 adult capacity " + saved);
        return saved == LEGACY_MAX ? MAX : saved;
    }
    public static int effectiveCap(ColonyStage stage, int bound) {
        if (!valid(bound)) throw new IllegalArgumentException("Invalid adult bound " + bound);
        return Math.min(stage.adultCap(), bound);
    }
}
