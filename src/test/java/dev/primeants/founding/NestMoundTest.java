package dev.primeants.founding;

import dev.primeants.colony.ColonyStage;
import dev.primeants.founding.NestMound.Cell;
import dev.primeants.founding.NestMound.Read;
import dev.primeants.founding.NestMound.Slot;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The stage mound plan (stage-1 T06): its shape, its room for the colony's soil, and where each column's cells may take
 * a unit. */
class NestMoundTest {
    private static Set<Cell> cells(ColonyStage s) { return new HashSet<>(NestMound.plan(s)); }

    @Test
    void matureContainsYoungAndEveryPlanStaysInBoundsOffTheRoute() {
        assertTrue(NestMound.plan(ColonyStage.FOUNDING).isEmpty(), "a founding colony keeps only its 0.1.0 deposits");
        assertEquals(NestMound.plan(ColonyStage.MATURE), NestMound.plan(ColonyStage.GREAT), "Great keeps Mature's mound until a later plan");
        var young = cells(ColonyStage.YOUNG); var mature = cells(ColonyStage.MATURE);
        assertTrue(mature.containsAll(young), "the Mature mound contains the Young one");
        // The declared shapes (docs/00-decisions.md, T06): cells, columns and layers.
        assertEquals(List.of(214, 98, 3), List.of(young.size(), NestMound.heights(NestMound.plan(ColonyStage.YOUNG)).size(), 1 + young.stream().mapToInt(Cell::layer).max().orElseThrow()));
        assertEquals(List.of(798, 242, 5), List.of(mature.size(), NestMound.heights(NestMound.plan(ColonyStage.MATURE)).size(), 1 + mature.stream().mapToInt(Cell::layer).max().orElseThrow()));
        var youngHeights = NestMound.heights(NestMound.plan(ColonyStage.YOUNG)); var matureHeights = NestMound.heights(NestMound.plan(ColonyStage.MATURE));
        assertTrue(matureHeights.size() > youngHeights.size(), "wider");
        assertTrue(Collections.max(matureHeights.values()) > Collections.max(youngHeights.values()), "taller");
        for (var s : List.of(ColonyStage.YOUNG, ColonyStage.MATURE)) {
            var plan = NestMound.plan(s);
            assertEquals(plan.size(), new HashSet<>(plan).size(), "no cell twice");
            var seen = new HashSet<Cell>();
            for (var c : plan) {
                assertTrue(c.forward() <= -1 && c.forward() >= -NestMound.BACK && Math.abs(c.side()) <= NestMound.SIDE && c.layer() >= 0 && c.layer() < NestMound.LAYERS, "in bounds: " + c);
                assertFalse(NestMound.route(c.forward(), c.side()), "never on the route: " + c);
                assertNotEquals(0, c.side(), "the approach lane and the exterior standing spot (-2, 0) stay clear: " + c);
                if (c.layer() > 0) assertTrue(seen.contains(new Cell(c.forward(), c.side(), c.layer() - 1)), "deposited only after the cell beneath it: " + c);
                seen.add(c);
            }
        }
        // The stairs (forward 0..2) and everything ahead of the entrance stay clear.
        for (int f = 0; f <= NestMound.BACK; f++) for (int s = -NestMound.SIDE; s <= NestMound.SIDE; s++) assertTrue(NestMound.route(f, s));
        assertThrows(IllegalStateException.class, () -> NestMound.compile(List.of(new NestMound.Tier(0, 3, 9, 4, 5, 3, -NestMound.BACK, 0))), "a tier reaching past the declared bounds is refused");
    }

    @Test
    void theYoungMoundHoldsTheFoundingWideningStoreAndHallSoil() {
        int founding = NestBlueprint.STAIRS.size() + NestBlueprint.FOUNDING_CHAMBER.size() - NestBlueprint.PLUGS.size();
        int widening = NestBlueprint.WIDENINGS.size() / 2, store = NestBlueprint.plan("left").dig().size(), hall = NestBlueprint.plan("hall_left").dig().size();
        assertEquals(List.of(22, 12, 24, 12), List.of(founding, widening, store, hall));
        var young = NestMound.plan(ColonyStage.YOUNG);
        assertTrue(young.size() >= founding + widening + store + hall, "Young holds every unit dug before Mature: " + young.size());
        // The 0.1.0 deposits (five rows behind the entrance, one to four cells either side, two layers) lie inside the lobes,
        // so founding and widening soil is Young mound soil, and the rest of Young still takes the store and the hall.
        var heights = NestMound.heights(young); int zeroOneLayers = 0;
        for (int f = -5; f <= -1; f++) for (int s = -4; s <= 4; s++) if (s != 0) {
            assertTrue(heights.getOrDefault(List.of(f, s), 0) >= 1, "a 0.1.0 deposit anchor is a Young column: " + f + "," + s);
            zeroOneLayers += Math.min(2, heights.get(List.of(f, s)));
        }
        assertTrue(young.size() - zeroOneLayers >= store + hall, "beyond the 0.1.0 deposit layers Young takes the store and the hall: " + (young.size() - zeroOneLayers));
        assertTrue(NestMound.plan(ColonyStage.MATURE).size() >= 3 * young.size(), "Mature leaves room for later rooms: " + NestMound.plan(ColonyStage.MATURE).size());
    }

    /** A column read from a map of dy to block kind; anything unmapped is open air. */
    private static NestMound.Column column(int height, Map<Integer, Read> blocks) { return NestMound.column(dy -> blocks.getOrDefault(dy, Read.OPEN), height); }
    private static Map<Integer, Read> ground(int top, Map<Integer, Read> above) {
        var out = new HashMap<Integer, Read>(); for (int dy = top; dy >= -8; dy--) out.put(dy, Read.NATURAL_GROUND); out.putAll(above); return out;
    }

    @Test
    void eachColumnSitsOnItsOwnGroundAndTakesSoilOnlyWhereThePlacementRulesAllow() {
        assertEquals(new NestMound.Column(0, List.of(Slot.FREE, Slot.FREE, Slot.FREE)), column(3, ground(0, Map.of())), "flat natural ground");
        assertEquals(new NestMound.Column(2, List.of(Slot.FREE, Slot.FREE)), column(2, ground(2, Map.of())), "a column on a rise sits on its own ground");
        assertEquals(new NestMound.Column(-2, List.of(Slot.FREE, Slot.FREE)), column(2, ground(-2, Map.of())), "and in a dip");
        assertEquals(new NestMound.Column(null, List.of(Slot.BLOCKED, Slot.BLOCKED)), column(2, ground(-3, Map.of())), "ground below the range: the column is skipped");
        assertEquals(new NestMound.Column(null, List.of(Slot.BLOCKED, Slot.BLOCKED)), column(2, ground(3, Map.of())), "a hill above the range too");
        assertEquals(new NestMound.Column(0, List.of(Slot.FREE, Slot.FREE)), column(2, ground(0, Map.of(1, Read.PLANT))), "a witnessed short plant is buried");
        assertEquals(new NestMound.Column(0, List.of(Slot.FILLED, Slot.FILLED, Slot.FREE)), column(3, ground(0, Map.of(1, Read.MOUND, 2, Read.MOUND))), "the colony's own mound soil is filled, and supports the next layer");
        assertEquals(new NestMound.Column(null, List.of(Slot.BLOCKED, Slot.BLOCKED)), column(2, ground(0, Map.of(1, Read.OTHER))), "a log, leaves, a crop, a fluid, a block entity or a player's block on the ground: skipped, never covered");
        assertEquals(new NestMound.Column(0, List.of(Slot.FILLED, Slot.BLOCKED, Slot.BLOCKED)), column(3, ground(0, Map.of(1, Read.MOUND, 2, Read.OTHER))), "a player's block on the mound is never covered: the column stops beneath it");
        assertEquals(new NestMound.Column(0, List.of(Slot.FREE, Slot.BLOCKED, Slot.BLOCKED)), column(3, ground(0, Map.of(2, Read.OTHER))), "an overhanging block stops the column at its height");
        var sand = new HashMap<Integer, Read>(); for (int dy = 0; dy >= -8; dy--) sand.put(dy, Read.OTHER);
        assertEquals(new NestMound.Column(null, List.of(Slot.BLOCKED)), column(1, sand), "sand, gravel or snow is not natural ground");
        assertEquals(new NestMound.Column(null, List.of(Slot.UNKNOWN, Slot.UNKNOWN)), column(2, ground(0, Map.of(2, Read.UNLOADED))), "an unloaded ground search is unknown");
        assertEquals(new NestMound.Column(-2, List.of(Slot.FREE, Slot.FREE, Slot.FREE, Slot.FREE, Slot.UNKNOWN, Slot.UNKNOWN)), column(6, ground(-2, Map.of(3, Read.UNLOADED))), "an unloaded layer leaves it and those above unknown");
    }

    @Test
    void depositsGrowEachLobeAsADomeFromItsCentre() {
        var young = NestMound.plan(ColonyStage.YOUNG);
        for (var c : young.subList(0, 8)) assertTrue(c.layer() == 0 && Math.abs(c.forward() + 4) <= 1 && Math.abs(Math.abs(c.side()) - 3.5) <= 1, "the first deposits are at the lobes' centres: " + c);
        // Each lobe rises before it spreads to its rim: its centre's second layer comes before its outermost base cells.
        int centre = young.indexOf(new Cell(-4, 3, 1)), rim = young.indexOf(new Cell(-4, 7, 0));
        assertTrue(centre >= 0 && rim >= 0 && centre < rim, "dome order: " + centre + " < " + rim);
    }
}
