package dev.primeants.founding;

import dev.primeants.founding.Findings.Verdict;
import dev.primeants.founding.NestBlueprint.Cell;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The dug-space scan shared by brood care and the stage (DugSpace), on a table world in plan cells: an unloaded dug
 * cell never hides a loaded broken face beside it, at any dig step of any placement. */
class DugSpaceTest {
    /** Plan cells: dug cells are the colony's openings, the founding chamber is open, everything else is solid ground
     * unless chosen broken; chosen cells are unloaded. */
    private record Table(Collection<Cell> dug, Set<Cell> unloaded, Set<Cell> broken) implements DugSpace.Ground<Cell> {
        @Override public boolean loaded(Cell c) { return !unloaded.contains(c); }
        @Override public boolean open(Cell c) { return dug.contains(c); }
        @Override public boolean closed(Cell c) { return !dug.contains(c) && !broken.contains(c) && !NestBlueprint.FOUNDING_CHAMBER.contains(c); }
        @Override public Cell face(Cell c, int i) { return c.neighbors().get(i); }
    }
    private static Findings scan(NestBlueprint.Plan plan, int removed, Set<Cell> unloaded, Set<Cell> broken) {
        var dug = plan.dig().subList(0, removed); var r = new Findings();
        DugSpace.scan(new Table(dug, unloaded, broken), dug, plan.connections(), plan.dig(), DugSpace.Labels.of(plan.placement().room().id()), r);
        return r;
    }
    /** The T03 habitat scan, kept here only to show the defect: an unloaded completed cell skipped all of its faces. */
    private static Verdict t03(NestBlueprint.Plan plan, int removed, Set<Cell> unloaded, Set<Cell> broken) {
        var dug = plan.dig().subList(0, removed); var g = new Table(dug, unloaded, broken); var r = new Findings();
        for (var p : dug) {
            if (!r.cell(g.loaded(p), "unavailable")) continue;
            for (var n : p.neighbors()) if (!dug.contains(n) && !plan.connections().contains(n) && r.cell(g.loaded(n), "unavailable") && !g.closed(n)) r.fault("open");
        }
        return r.verdict();
    }
    private static List<NestBlueprint.Plan> placements() {
        var out = new ArrayList<NestBlueprint.Plan>();
        for (var p : NestBlueprint.MATERIAL_STORE) out.add(NestBlueprint.plan(p.name()));
        for (var p : NestBlueprint.QUEENS_HALL) out.add(NestBlueprint.plan(p.name()));
        return out;
    }

    @Test
    void theReviewersCounterexampleIsLossNotUnknown() {
        // Right store placement after 12 removals: completed (9,2,-2) is unloaded; its only dug neighbour of shell (9,1,-2) is broken.
        var right = NestBlueprint.plan("right");
        var hidden = new Cell(9, 2, -2); var wall = new Cell(9, 1, -2);
        assertEquals(hidden, right.dig().get(10), "the 11th removal");
        assertEquals(Verdict.UNKNOWN, t03(right, 12, Set.of(hidden), Set.of(wall)), "T03 skipped the faces of the unloaded cell");
        var r = scan(right, 12, Set.of(hidden), Set.of(wall));
        assertEquals(Verdict.DAMAGED, r.verdict());
        assertEquals("material_store_shell_or_support_open", r.problem());
        var finished = scan(right, right.dig().size(), Set.of(hidden), Set.of(wall));
        assertEquals(Verdict.DAMAGED, finished.verdict(), "the finished room too");
    }

    @Test
    void theSameUnloadedCellWithAnIntactShellIsUnknown() {
        var right = NestBlueprint.plan("right"); var hidden = new Cell(9, 2, -2);
        for (int removed : new int[]{12, right.dig().size()}) {
            var r = scan(right, removed, Set.of(hidden), Set.of());
            assertEquals(Verdict.UNKNOWN, r.verdict());
            assertEquals("material_store_chunk_unavailable", r.problem());
        }
        assertEquals(Verdict.CLEAR, scan(right, 12, Set.of(), Set.of()).verdict(), "nothing unloaded, nothing broken");
    }

    @Test
    void everyLoadedBreachBesideAnyUnloadedCellIsLossAtEveryDigStep() {
        for (var plan : placements()) for (int removed = 1; removed <= plan.dig().size(); removed++) {
            var dug = new HashSet<>(plan.dig().subList(0, removed));
            var faces = new LinkedHashSet<Cell>();
            for (var p : dug) for (var n : p.neighbors()) if (!dug.contains(n) && !plan.connections().contains(n)) faces.add(n);
            var read = new LinkedHashSet<Cell>(dug); read.addAll(faces);
            for (var hidden : read) {
                assertEquals(Verdict.UNKNOWN, scan(plan, removed, Set.of(hidden), Set.of()).verdict(), plan.placement().name() + " " + removed + " unloaded " + hidden);
                for (var wall : faces) {
                    if (wall.equals(hidden)) continue;
                    var r = scan(plan, removed, Set.of(hidden), Set.of(wall));
                    assertEquals(Verdict.DAMAGED, r.verdict(), plan.placement().name() + " " + removed + " unloaded " + hidden + " broken " + wall);
                }
            }
        }
    }

    @Test
    void aPendingPlannedCellOpenedOutOfOrderIsAnUnauthorizedBreach() {
        var right = NestBlueprint.plan("right"); var pending = right.dig().get(12);
        var r = scan(right, 12, Set.of(), Set.of(pending));
        assertEquals(Verdict.DAMAGED, r.verdict());
        assertEquals("material_store_unauthorized_pending_breach", r.problem());
    }
}
