package dev.primeants.worker;

import dev.primeants.colony.StageRules;
import java.util.Collection;
import java.util.Map;

/** Material accounting (GDD v2 section 3): one accepted item is one unit of one natural material, and a tier-1 store's
 * room rule. Pure, by item registry id, so unit tests and the store share it. */
public final class MaterialUnits {
    /** What a unit is. Ore waits in the store for stage 2's forge. */
    public enum Material { CLAY, STONE, GRAVEL, SAND, ORE }
    /** The accepted items. Nothing else is a material, and only dropped items are ever hauled. */
    public static final Map<String, Material> ITEMS = Map.of(
        "minecraft:clay_ball", Material.CLAY, "minecraft:cobblestone", Material.STONE, "minecraft:stone", Material.STONE,
        "minecraft:gravel", Material.GRAVEL, "minecraft:sand", Material.SAND,
        "minecraft:coal", Material.ORE, "minecraft:raw_copper", Material.ORE, "minecraft:raw_iron", Material.ORE);
    /** Tier-1 store capacity in units, and the part of it kept for clay: Mature's 16 clay always fit beside up to 16
     * units of other stock, so no other material can crowd the store's clay out. */
    public static final int CAPACITY = 32, CLAY_SHARE = 16, MAX_CAPACITY = 64;
    public static int capacity(int tier) { return tier >= 2 ? MAX_CAPACITY : CAPACITY; }
    public static int clayShare(int capacity) { return capacity / 2; }
    private MaterialUnits() { }
    /** The material one item of this id is, or null. */
    public static Material of(String item) { return ITEMS.get(item); }
    public static long count(Collection<Material> held, Material m) { return held.stream().filter(h -> h == m).count(); }
    /** Room for one more unit of this material beside the held units. */
    public static boolean room(Collection<Material> held, Material m) {
        return room(held, m, CAPACITY);
    }
    public static boolean room(Collection<Material> held, Material m, int capacity) {
        if (capacity != CAPACITY && capacity != MAX_CAPACITY) throw new IllegalArgumentException("Unsupported store capacity");
        if (m == null || held.size() >= capacity) return false;
        return m == Material.CLAY || held.size() - count(held, Material.CLAY) < capacity - clayShare(capacity);
    }
    /** The most units of a material one store can hold. An unknown store adds this to the possible count only. */
    public static int most(Material m) { return m == Material.CLAY ? CAPACITY : CAPACITY - CLAY_SHARE; }
    /** A stage input: units held in confirmed stores are known; each unknown store may hold its most. */
    public static StageRules.Bound stock(Collection<? extends Collection<Material>> confirmed, int unknownStores, Material m) {
        return stock(confirmed, unknownStores, m, CAPACITY);
    }
    public static StageRules.Bound stock(Collection<? extends Collection<Material>> confirmed, int unknownStores, Material m, int capacity) {
        int known = 0;
        for (var held : confirmed) known += (int)count(held, m);
        return new StageRules.Bound(known, known + unknownStores * (m == Material.CLAY ? capacity : capacity - clayShare(capacity)));
    }
    /** Units per visible heap level, and the levels of a full store's clay and of its other stock's full share. */
    public static final int LEVEL = 4, CLAY_LEVELS = MAX_CAPACITY / LEVEL, STOCK_LEVELS = (MAX_CAPACITY - clayShare(MAX_CAPACITY)) / LEVEL;
    /** One visible heap level per four units: tier one's 32 clay/eight levels and other share 16/four;
     * tier two's 64 clay/sixteen levels and other share 32/eight. */
    public static int level(long units) { return (int)((units + LEVEL - 1) / LEVEL); }
    /** What a store's block shows (MaterialStoreBlock): its clay heap's level, its other stock's level and which other
     * materials lie in it. Equal displays mean equal sets of materials and totals within seven units of each other, so
     * any two stores whose totals differ by eight or more, or whose materials differ, look different. */
    public record Display(int clay, int stock, boolean stone, boolean gravel, boolean sand, boolean ore) { }
    public static Display display(Collection<Material> held) {
        long clay = count(held, Material.CLAY);
        return new Display(level(clay), level(held.size() - clay), count(held, Material.STONE) > 0, count(held, Material.GRAVEL) > 0,
            count(held, Material.SAND) > 0, count(held, Material.ORE) > 0);
    }
}
