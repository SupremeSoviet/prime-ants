package dev.primeants.founding;

import dev.primeants.founding.Findings.Verdict;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confirmation precedence: a fault seen in loaded blocks beats unavailable terrain, whatever the check order. */
class FindingsTest {
    @Test
    void nothingObservedWrongAndEverythingLoadedIsClear() {
        var r = new Findings();
        assertTrue(r.cell(true, "enclosure_chunk_unavailable"));
        assertEquals(Verdict.CLEAR, r.verdict());
        assertNull(r.problem());
    }

    @Test
    void anUnloadedCellAloneIsUnknownAndIsNotRead() {
        var r = new Findings();
        assertFalse(r.cell(false, "enclosure_chunk_unavailable"), "an unloaded cell must not be read");
        assertEquals(Verdict.UNKNOWN, r.verdict());
        assertEquals("enclosure_chunk_unavailable", r.problem());
        assertNull(r.fault());
    }

    @Test
    void anObservedFaultBeatsAnEarlierUnavailableCell() {
        // The T02 limitation: an unloaded entrance checked first used to hide a broken wall checked later.
        var r = new Findings();
        r.cell(false, "enclosure_chunk_unavailable");
        r.fault("enclosure_shell_open");
        assertEquals(Verdict.DAMAGED, r.verdict());
        assertEquals("enclosure_shell_open", r.problem());
    }

    @Test
    void anObservedFaultBeatsALaterUnavailableCell() {
        var r = new Findings();
        r.fault("enclosure_chamber_obstructed");
        r.unavailable("extension_chunk_unavailable");
        assertEquals(Verdict.DAMAGED, r.verdict());
        assertEquals("enclosure_chamber_obstructed", r.problem());
    }

    @Test
    void theFirstLabelOfEachKindIsKeptInCheckOrder() {
        var r = new Findings();
        r.unavailable("enclosure_chunk_unavailable"); r.unavailable("extension_chunk_unavailable");
        r.fault("enclosure_route_obstructed"); r.fault("enclosure_shell_open");
        assertEquals("enclosure_route_obstructed", r.problem());
        assertEquals("enclosure_route_obstructed", r.fault());
    }

    @Test
    void mergedFindingsKeepThePrecedence() {
        var habitat = new Findings().unavailable("enclosure_chunk_unavailable");
        var room = new Findings().fault("material_store_shell_open");
        var merged = habitat.copy().add(room);
        assertEquals(Verdict.DAMAGED, merged.verdict());
        assertEquals("material_store_shell_open", merged.problem());
        assertEquals(Verdict.UNKNOWN, habitat.verdict(), "a copy never changes the shared habitat");
    }

    @Test
    void aDestroyedLoadedMarkerLosesOnlyItsFunctionBesideUnavailableTerrain() {
        // Unloaded entrance: the shared habitat is unknown. The loaded cache is gone; the loaded pile is owned.
        var habitat = new Findings();
        habitat.cell(false, "enclosure_chunk_unavailable");
        var foodStore = habitat.copy();
        if (foodStore.cell(true, "food_store_marker_chunk_unavailable")) foodStore.fault("food_store_marker_missing_or_foreign");
        var nursery = habitat.copy();
        nursery.cell(true, "nursery_marker_chunk_unavailable");
        assertEquals(Verdict.DAMAGED, foodStore.verdict());
        assertEquals("food_store_marker_missing_or_foreign", foodStore.problem());
        assertEquals(Verdict.UNKNOWN, nursery.verdict());
        assertEquals(Verdict.UNKNOWN, habitat.verdict());
    }

    @Test
    void anUnloadedMarkerInADamagedChamberIsStillLost() {
        var habitat = new Findings().fault("enclosure_shell_open");
        var check = habitat.copy();
        assertFalse(check.cell(false, "nursery_marker_chunk_unavailable"));
        assertEquals(Verdict.DAMAGED, check.verdict());
        assertEquals("enclosure_shell_open", check.problem());
    }
}
