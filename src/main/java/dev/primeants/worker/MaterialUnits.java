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
    public static final int CAPACITY = 32, CLAY_SHARE = 16;
    private MaterialUnits() { }
    /** The material one item of this id is, or null. */
    public static Material of(String item) { return ITEMS.get(item); }
    public static long count(Collection<Material> held, Material m) { return held.stream().filter(h -> h == m).count(); }
    /** Room for one more unit of this material beside the held units. */
    public static boolean room(Collection<Material> held, Material m) {
        if (m == null || held.size() >= CAPACITY) return false;
        return m == Material.CLAY || held.size() - count(held, Material.CLAY) < CAPACITY - CLAY_SHARE;
    }
    /** The most units of a material one store can hold. An unknown store adds this to the possible count only. */
    public static int most(Material m) { return m == Material.CLAY ? CAPACITY : CAPACITY - CLAY_SHARE; }
    /** A stage input: units held in confirmed stores are known; each unknown store may hold its most. */
    public static StageRules.Bound stock(Collection<? extends Collection<Material>> confirmed, int unknownStores, Material m) {
        int known = 0;
        for (var held : confirmed) known += (int)count(held, m);
        return new StageRules.Bound(known, known + unknownStores * most(m));
    }
    /** A visible heap level 0..4: one level per four units, full at the 16-unit share. */
    public static int level(long units) { return (int)Math.min(4, (units + 3) / 4); }
}
