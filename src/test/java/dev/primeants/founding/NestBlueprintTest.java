package dev.primeants.founding;

import dev.primeants.colony.ChamberFunction;
import dev.primeants.founding.NestBlueprint.Cell;
import dev.primeants.founding.NestBlueprint.Column;
import dev.primeants.founding.NestBlueprint.Placement;
import dev.primeants.founding.NestBlueprint.Room;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shape tests of the nest plan, independent of the compiler's own checks: connectivity, bounds, overlap, a closed
 * shell and a covered dig order, for every material-store placement. */
class NestBlueprintTest {
    private static List<NestBlueprint.Plan> plans() {
        return NestBlueprint.MATERIAL_STORE.stream().map(p -> NestBlueprint.plan(p.name())).toList();
    }
    private static Set<Cell> box(int f0, int f1, int s0, int s1, int dy0, int dy1) {
        var out = new HashSet<Cell>();
        for (int f = f0; f <= f1; f++) for (int s = s0; s <= s1; s++) for (int dy = dy0; dy <= dy1; dy++) out.add(new Cell(f, s, dy));
        return out;
    }
    private static Set<Column> columns(Collection<Cell> cells) {
        var out = new HashSet<Column>(); for (var c : cells) out.add(new Column(c.forward(), c.side())); return out;
    }

    @Test
    void theFoundingFrameMatchesTheZeroOneZeroNest() {
        assertEquals(Set.of(new Cell(0, 0, 0), new Cell(1, 0, 0), new Cell(1, 0, -1), new Cell(2, 0, 0), new Cell(2, 0, -1), new Cell(2, 0, -2)), NestBlueprint.STAIRS);
        assertTrue(NestBlueprint.STAIRS.containsAll(NestBlueprint.PLUGS));
        assertEquals(box(3, 5, -1, 1, -2, -1), NestBlueprint.FOUNDING_CHAMBER);
        var widenings = box(3, 5, 2, 3, -2, -1); widenings.addAll(box(3, 5, -3, -2, -2, -1));
        assertEquals(widenings, NestBlueprint.WIDENINGS);
        assertEquals(5 * 8 * 4, NestBlueprint.DEPOSITS.size());
    }

    @Test
    void eachPlacementDigsATwoHighMaterialStoreAndATunnelFromTheFoundingChamber() {
        assertEquals(List.of("left", "right"), NestBlueprint.MATERIAL_STORE.stream().map(Placement::name).toList());
        for (var plan : plans()) {
            var room = plan.placement().room();
            assertEquals(ChamberFunction.MATERIAL_STORE, room.function());
            assertEquals(3 * 3 * 2, room.cells().size(), "a 3x3 room, two blocks high");
            assertTrue(plan.dig().containsAll(room.cells()));
            assertEquals(new Column(6, 0), plan.placement().passage().getFirst(), "the tunnel leaves through the back wall's centre");
            assertEquals(Set.of(new Cell(6, 0, -2), new Cell(6, 0, -1)), plan.openings(), "the only founding cells opened");
            assertEquals(Set.of(new Cell(5, 0, -2), new Cell(5, 0, -1)), plan.connections());
            assertEquals(24, plan.dig().size());
            assertTrue(room.contains(new Column(plan.marker().forward(), plan.marker().side())) && plan.marker().dy() == NestBlueprint.FLOOR, "the marker sits on the room's floor");
        }
    }

    @Test
    void theStoreTunnelAndFoundingChamberFormOneConnectedWalkableTwoHighSpace() {
        for (var plan : plans()) {
            var dig = new HashSet<>(plan.dig());
            var open = new HashSet<>(dig); open.addAll(NestBlueprint.FOUNDING_CHAMBER);
            var floors = new HashSet<Column>();
            for (var c : open) if (c.dy() == NestBlueprint.FLOOR) {
                assertTrue(open.contains(new Cell(c.forward(), c.side(), NestBlueprint.UPPER)), "two high at " + c);
                var support = new Cell(c.forward(), c.side(), NestBlueprint.FLOOR - 1);
                assertFalse(open.contains(support), "solid support under " + c);
                if (dig.contains(c)) assertTrue(plan.shell().contains(support), "planned support stays solid under " + c);
                floors.add(new Column(c.forward(), c.side()));
            }
            for (var c : open) assertTrue(c.dy() == NestBlueprint.FLOOR || c.dy() == NestBlueprint.UPPER, "nothing but two-high space " + c);
            var reached = new HashSet<Column>(); var queue = new ArrayDeque<Column>(); queue.add(new Column(4, 0));
            while (!queue.isEmpty()) {
                var c = queue.remove(); if (!reached.add(c)) continue;
                for (var n : List.of(new Column(c.forward() + 1, c.side()), new Column(c.forward() - 1, c.side()), new Column(c.forward(), c.side() + 1), new Column(c.forward(), c.side() - 1)))
                    if (floors.contains(n)) queue.add(n);
            }
            assertEquals(floors, reached, "one walkable space from the founding chamber: " + plan.placement().name());
        }
    }

    @Test
    void everyTargetAndShellCellStaysInsideTheDeclaredBounds() {
        assertEquals(10, NestBlueprint.RADIUS);
        assertEquals(3, NestBlueprint.DEPTH);
        int maxForward = 0, maxSide = 0, minDy = 0, maxDy = -9;
        for (var plan : plans()) {
            var cells = new ArrayList<Cell>(plan.dig()); cells.addAll(plan.shell());
            for (var r : plan.placement().reserved()) cells.addAll(r.cells());
            for (var c : cells) {
                assertTrue(Math.abs(c.forward()) <= NestBlueprint.RADIUS && Math.abs(c.side()) <= NestBlueprint.RADIUS, "radius " + c);
                assertTrue(c.dy() >= -NestBlueprint.DEPTH && c.dy() <= 0, "depth " + c);
                maxForward = Math.max(maxForward, Math.abs(c.forward())); maxSide = Math.max(maxSide, Math.abs(c.side()));
                minDy = Math.min(minDy, c.dy()); maxDy = Math.max(maxDy, c.dy());
            }
        }
        assertEquals(List.of(10, 5, -3, 0), List.of(maxForward, maxSide, minDy, maxDy), "the plan's actual extent");
    }

    @Test
    void nothingOverlapsStairsPlugsFoundingChamberEitherWideningOrTheDeposits() {
        var occupied = new HashSet<Cell>(NestBlueprint.STAIRS);
        occupied.addAll(NestBlueprint.PLUGS); occupied.addAll(NestBlueprint.FOUNDING_CHAMBER); occupied.addAll(NestBlueprint.WIDENINGS);
        for (var plan : plans()) {
            for (var c : plan.dig()) {
                assertFalse(occupied.contains(c), "dig cell on existing or widening space " + c);
                assertFalse(NestBlueprint.DEPOSITS.contains(c), "dig cell on a deposit " + c);
            }
            for (var c : plan.shell()) assertFalse(occupied.contains(c) || NestBlueprint.DEPOSITS.contains(c), "shell cell that is or may be open " + c);
            for (var reserved : plan.placement().reserved()) {
                assertEquals(ChamberFunction.QUEENS_HALL, reserved.function());
                for (var c : reserved.cells())
                    assertFalse(plan.dig().contains(c) || plan.shell().contains(c) || occupied.contains(c) || NestBlueprint.DEPOSITS.contains(c), "reserved hall is free " + c);
            }
        }
    }

    @Test
    void theShellStaysSolidExceptAtDeclaredOpenings() {
        for (var plan : plans()) {
            var dig = new HashSet<>(plan.dig());
            for (var c : dig) for (var n : c.neighbors())
                assertTrue(dig.contains(n) || plan.connections().contains(n) || plan.shell().contains(n), "every face is planned, declared or shell: " + n);
            assertTrue(NestBlueprint.FOUNDING_CHAMBER.containsAll(plan.connections()), "openings lead only into the founding chamber");
            var expectedShell = new HashSet<Cell>();
            for (var c : dig) for (var n : c.neighbors()) if (!dig.contains(n) && !plan.connections().contains(n)) expectedShell.add(n);
            assertEquals(expectedShell, plan.shell());
            for (var c : plan.openings()) assertTrue(c.neighbors().stream().anyMatch(NestBlueprint.FOUNDING_CHAMBER::contains), "an opening is a founding wall cell " + c);
        }
    }

    @Test
    void theDigOrderKeepsEveryPendingCellCoveredBottomFirst() {
        for (var plan : plans()) {
            var dig = plan.dig();
            var open = new HashSet<Cell>(NestBlueprint.FOUNDING_CHAMBER);
            for (int i = 0; i < dig.size(); i++) {
                var target = dig.get(i);
                for (var pending : dig.subList(i, dig.size()))
                    assertFalse(open.contains(new Cell(pending.forward(), pending.side(), pending.dy() + 1)), "pending " + pending + " exposed from above before step " + i);
                if (target.dy() == NestBlueprint.UPPER) assertEquals(new Cell(target.forward(), target.side(), NestBlueprint.FLOOR), dig.get(i - 1), "bottom first, column by column");
                boolean stand = target.neighbors().stream().filter(n -> n.dy() == target.dy()).anyMatch(n ->
                    open.contains(new Cell(n.forward(), n.side(), NestBlueprint.FLOOR)) && open.contains(new Cell(n.forward(), n.side(), NestBlueprint.UPPER)));
                assertTrue(stand, "a supported two-high stand beside " + target);
                open.add(target);
            }
            assertTrue(NestBlueprint.coveredOrder(dig, new HashSet<>(dig)));
            var topFirst = new ArrayList<>(dig); Collections.swap(topFirst, 0, 1);
            assertFalse(NestBlueprint.coveredOrder(topFirst, new HashSet<>(dig)), "top-first order exposes the pending floor cell");
        }
    }

    @Test
    void placementsAreDistinctAndEachKeepsTheOtherRoomForTheQueensHall() {
        var left = NestBlueprint.plan("left").placement(); var right = NestBlueprint.plan("right").placement();
        assertTrue(Collections.disjoint(left.room().cells(), right.room().cells()), "room bounds identify the placement");
        assertEquals(right.room().cells(), left.reserved().getFirst().cells());
        assertEquals(left.room().cells(), right.reserved().getFirst().cells());
        assertTrue(columns(left.room().cells()).stream().allMatch(c -> c.side() < 0) && columns(right.room().cells()).stream().allMatch(c -> c.side() > 0));
    }

    @Test
    void theCompilerRejectsInvalidPlacements() {
        var store = new Room("material_store", ChamberFunction.MATERIAL_STORE, 7, 2, 3, 3, new Column(8, 3));
        var hall = new Room("queens_hall", ChamberFunction.QUEENS_HALL, 7, -4, 3, 3, new Column(8, -3));
        var tunnel = List.of(new Column(6, 0), new Column(7, 0), new Column(7, 1));
        NestBlueprint.compile(new Placement("valid", tunnel, store, List.of(hall)));
        var onWidening = new Room("material_store", ChamberFunction.MATERIAL_STORE, 3, 2, 3, 2, new Column(4, 2));
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("widening", List.of(new Column(6, 0)), onWidening, List.of())));
        var far = new Room("material_store", ChamberFunction.MATERIAL_STORE, 9, 2, 3, 3, new Column(10, 3));
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("bounds", List.of(new Column(6, 0), new Column(7, 0), new Column(8, 0), new Column(8, 1)), far, List.of())), "shell at forward 12 is out of bounds");
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("detached", List.of(new Column(6, 0)), store, List.of())), "a room the tunnel never reaches");
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("reserved", tunnel, store, List.of(store))), "a reserved room on dug space");
        assertThrows(IllegalArgumentException.class, () -> new Room("material_store", ChamberFunction.MATERIAL_STORE, 7, 2, 3, 3, new Column(6, 0)), "a marker outside its room");
    }
}
