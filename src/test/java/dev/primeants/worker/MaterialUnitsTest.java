package dev.primeants.worker;

import dev.primeants.colony.ColonyStage;
import dev.primeants.colony.StageRules;
import dev.primeants.worker.MaterialUnits.Material;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Material accounting: one accepted item is one unit, the tier-1 store fits Mature's clay beside other stock, and an
 * unknown store only widens what the stage may possibly count. */
class MaterialUnitsTest {
    private static List<Material> units(Material m, int n) { return Collections.nCopies(n, m); }
    private static List<Material> join(List<Material> a, List<Material> b) { var out = new ArrayList<>(a); out.addAll(b); return out; }

    @Test
    void oneAcceptedItemIsOneUnitOfOneMaterial() {
        assertEquals(Map.of("minecraft:clay_ball", Material.CLAY, "minecraft:cobblestone", Material.STONE, "minecraft:stone", Material.STONE,
            "minecraft:gravel", Material.GRAVEL, "minecraft:sand", Material.SAND,
            "minecraft:coal", Material.ORE, "minecraft:raw_copper", Material.ORE, "minecraft:raw_iron", Material.ORE), MaterialUnits.ITEMS);
        for (var other : List.of("minecraft:clay", "minecraft:dirt", "minecraft:apple", "minecraft:iron_ingot", "minecraft:coal_block", "minecraft:raw_iron_block", "prime_ants:nest_soil"))
            assertNull(MaterialUnits.of(other), other + " is not a store unit");
    }

    @Test
    void theTierOneStoreFitsMaturesClayAlongsideOtherStock() {
        int clay = StageRules.requirements(ColonyStage.MATURE).stream().filter(r -> r.name().equals("clay")).findFirst().orElseThrow().need();
        assertEquals(16, clay);
        assertEquals(32, MaterialUnits.CAPACITY);
        assertEquals(clay, MaterialUnits.CLAY_SHARE, "the share kept for clay is exactly Mature's clay");
        var otherFull = join(units(Material.STONE, 10), units(Material.ORE, 6));
        assertFalse(MaterialUnits.room(otherFull, Material.GRAVEL), "sixteen other units fill their part");
        assertTrue(MaterialUnits.room(otherFull, Material.CLAY), "clay still has its sixteen places");
        var mature = join(otherFull, units(Material.CLAY, clay));
        assertEquals(MaterialUnits.CAPACITY, mature.size());
        assertFalse(MaterialUnits.room(mature, Material.CLAY), "full");
        assertTrue(MaterialUnits.room(units(Material.CLAY, 31), Material.CLAY), "clay may fill the whole store");
        assertFalse(MaterialUnits.room(units(Material.CLAY, 32), Material.CLAY));
        assertTrue(MaterialUnits.room(units(Material.CLAY, 16), Material.STONE), "other stock may still use its own part");
        var full = join(units(Material.CLAY, 17), units(Material.STONE, 15));
        assertFalse(MaterialUnits.room(full, Material.STONE)); assertFalse(MaterialUnits.room(full, Material.CLAY), "32 units fill the store");
        assertFalse(MaterialUnits.room(List.of(), null), "a non-material never has room");
    }

    @Test
    void clayAndStoneAreKnownOnlyInConfirmedStoresAndAnUnknownStoreWidensOnlyThePossibleCount() {
        var confirmed = List.of(join(units(Material.CLAY, 16), join(units(Material.STONE, 3), units(Material.ORE, 2))));
        assertEquals(new StageRules.Bound(16, 16), MaterialUnits.stock(confirmed, 0, Material.CLAY));
        assertEquals(new StageRules.Bound(3, 3), MaterialUnits.stock(confirmed, 0, Material.STONE), "cobblestone and stone are stone; ore is not");
        assertEquals(new StageRules.Bound(16, 48), MaterialUnits.stock(confirmed, 1, Material.CLAY), "an unknown store may hold 32 clay");
        assertEquals(new StageRules.Bound(3, 19), MaterialUnits.stock(confirmed, 1, Material.STONE), "or 16 stone");
        assertEquals(new StageRules.Bound(0, 0), MaterialUnits.stock(List.of(), 0, Material.CLAY), "no store, no clay");
    }

    @Test
    void theVisibleHeapsRiseOneLevelPerFourUnitsUpToTheShare() {
        assertEquals(List.of(0, 1, 1, 1, 1, 2, 3, 4, 4, 4), List.of(0, 1, 2, 3, 4, 5, 9, 13, 16, 32).stream().map(n -> MaterialUnits.level(n)).toList());
    }
}
