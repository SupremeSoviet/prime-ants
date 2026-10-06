package dev.primeants.colony;

/** What a registered chamber is for. A function only counts while live blocks confirm it (see ColonyDevelopment). */
public enum ChamberFunction {
    NURSERY("nursery"), FOOD_STORE("food_store"), MATERIAL_STORE("material_store"), QUEENS_HALL("queens_hall");
    private final String serializedName;
    ChamberFunction(String serializedName) { this.serializedName = serializedName; }
    public String serializedName() { return serializedName; }
    public static ChamberFunction byName(String name) {
        for (var f : values()) if (f.serializedName.equals(name)) return f;
        throw new IllegalArgumentException("Unknown chamber function " + name);
    }
}
