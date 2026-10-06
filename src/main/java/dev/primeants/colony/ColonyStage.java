package dev.primeants.colony;

/** Colony development stages (GDD v2 section 1, decision 23). Adult caps count the queen, as the 0.1.0 cap of 30 did. */
public enum ColonyStage {
    FOUNDING("founding", 5), YOUNG("young", 30), MATURE("mature", 60), GREAT("great", 120);
    public static final int MAX_ADULT_CAP = 120;
    private final String serializedName;
    private final int adultCap;
    ColonyStage(String serializedName, int adultCap) { this.serializedName = serializedName; this.adultCap = adultCap; }
    public String serializedName() { return serializedName; }
    /** Living adults including the queen plus brood reservations; existing adults above it are never removed. */
    public int adultCap() { return adultCap; }
    public ColonyStage next() { return this == GREAT ? null : values()[ordinal() + 1]; }
    public static ColonyStage byName(String name) {
        for (var s : values()) if (s.serializedName.equals(name)) return s;
        throw new IllegalArgumentException("Unknown colony stage " + name);
    }
}
