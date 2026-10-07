package dev.primeants.founding;

import dev.primeants.colony.ChamberFunction;
import dev.primeants.founding.NestBlueprint.Cell;
import dev.primeants.founding.NestBlueprint.Column;
import dev.primeants.founding.NestBlueprint.Placement;
import dev.primeants.founding.NestBlueprint.Room;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Shape tests of the queen's hall placements: an extension of the queen's own chamber on one 0.1.0 widening side,
 * clear of everything else the nest plan digs or keeps, and dug in an order that never needs the cache or pile cell. */
class QueensHallBlueprintTest {
    private static List<NestBlueprint.Plan> halls() { return NestBlueprint.QUEENS_HALL.stream().map(p -> NestBlueprint.plan(p.name())).toList(); }
    private static List<NestBlueprint.Plan> stores() { return NestBlueprint.MATERIAL_STORE.stream().map(p -> NestBlueprint.plan(p.name())).toList(); }
    private static Set<Cell> box(int f0, int f1, int s0, int s1, int dy0, int dy1) {
        var out = new HashSet<Cell>();
        for (int f = f0; f <= f1; f++) for (int s = s0; s <= s1; s++) for (int dy = dy0; dy <= dy1; dy++) out.add(new Cell(f, s, dy));
        return out;
    }
    private static Set<Cell> side(int sign) { return box(3, 5, Math.min(2 * sign, 3 * sign), Math.max(2 * sign, 3 * sign), -2, -1); }

    @Test
    void eachHallIsATwelveCellTwoHighExtensionOfTheQueensChamberOnOneWideningSide() {
        assertEquals(List.of("hall_left", "hall_right"), NestBlueprint.QUEENS_HALL.stream().map(Placement::name).toList());
        assertEquals(List.of(-1, 1), NestBlueprint.QUEENS_HALL.stream().map(Placement::widening).toList());
        for (var plan : halls()) {
            int sign = plan.placement().widening(); var room = plan.placement().room();
            assertEquals(ChamberFunction.QUEENS_HALL, room.function());
            assertEquals("queens_hall", room.id());
            assertNull(room.marker()); assertNull(plan.marker(), "no marker block: the hall's marker is the living queen");
            assertTrue(plan.placement().passage().isEmpty(), "no passage: the hall opens straight off the queen's chamber");
            assertEquals(12, plan.dig().size());
            assertEquals(side(sign), new HashSet<>(plan.dig()), "exactly the widening side the colony's widening did not take");
            assertEquals(box(3, 5, sign, sign, -2, -1), plan.connections(), "it adjoins the founding chamber along its whole side row");
            assertEquals(box(3, 5, 2 * sign, 2 * sign, -2, -1), plan.openings(), "only the founding side wall is opened");
            assertTrue(NestBlueprint.WIDENINGS.containsAll(plan.dig()));
        }
    }

    @Test
    void hallsAreOneWalkableTwoHighSpaceWithTheQueensChamberAndAClosedShell() {
        for (var plan : halls()) {
            var dig = new HashSet<>(plan.dig());
            var open = new HashSet<>(dig); open.addAll(NestBlueprint.FOUNDING_CHAMBER);
            for (var c : dig) {
                assertTrue(c.dy() == NestBlueprint.FLOOR || c.dy() == NestBlueprint.UPPER, "two high only " + c);
                if (c.dy() == NestBlueprint.FLOOR) {
                    assertTrue(dig.contains(new Cell(c.forward(), c.side(), NestBlueprint.UPPER)));
                    assertTrue(plan.shell().contains(new Cell(c.forward(), c.side(), NestBlueprint.FLOOR - 1)), "solid planned support under " + c);
                }
                for (var n : c.neighbors()) assertTrue(dig.contains(n) || plan.connections().contains(n) || plan.shell().contains(n), "every face is planned, declared or shell " + n);
            }
            var expected = new HashSet<Cell>();
            for (var c : dig) for (var n : c.neighbors()) if (!dig.contains(n) && !plan.connections().contains(n)) expected.add(n);
            assertEquals(expected, plan.shell());
            assertEquals(26, plan.shell().size(), "roof, floor, outer wall and both end walls");
            for (var c : plan.shell()) assertTrue(c.inBounds(), "shell in bounds " + c);
        }
    }

    @Test
    void hallsLeaveTheStairsPlugsDepositsTheOtherWideningSideAndEveryStoreUntouched() {
        for (var hall : halls()) {
            int sign = hall.placement().widening();
            var kept = new HashSet<Cell>(NestBlueprint.STAIRS); kept.addAll(NestBlueprint.PLUGS); kept.addAll(NestBlueprint.FOUNDING_CHAMBER);
            kept.addAll(NestBlueprint.DEPOSITS); kept.addAll(side(-sign));
            var touched = new HashSet<>(hall.dig()); touched.addAll(hall.shell());
            for (var c : touched) assertFalse(kept.contains(c), hall.placement().name() + " touches kept space " + c);
            for (var store : stores()) {
                var storeTouched = new HashSet<>(store.dig()); storeTouched.addAll(store.shell());
                for (var r : store.placement().reserved()) storeTouched.addAll(r.cells());
                for (var c : hall.dig()) assertFalse(storeTouched.contains(c), "hall dig cell in the " + store.placement().name() + " store " + c);
                for (var c : store.dig()) assertFalse(hall.shell().contains(c), "store dig cell in the hall shell " + c);
            }
        }
    }

    @Test
    void theHallDigOrderNeverNeedsTheCacheOrBroodPileColumnAsAStand() {
        assertEquals(Set.of(new Column(3, -1), new Column(4, 1)), NestBlueprint.HELD_COLUMNS, "NestPlan.cache (3,-1) and nursery (4,1)");
        for (var plan : halls()) {
            int sign = plan.placement().widening(); var dig = plan.dig();
            assertEquals(List.of(new Cell(5, 2 * sign, -2), new Cell(5, 2 * sign, -1)), dig.subList(0, 2), "the back-row column first, dug from the chamber's back corner");
            var open = new HashSet<Cell>(NestBlueprint.FOUNDING_CHAMBER);
            for (int i = 0; i < dig.size(); i++) {
                var target = dig.get(i);
                if (target.dy() == NestBlueprint.UPPER) assertEquals(new Cell(target.forward(), target.side(), NestBlueprint.FLOOR), dig.get(i - 1), "bottom first");
                boolean stand = target.neighbors().stream().filter(n -> n.dy() == target.dy()).anyMatch(n -> {
                    var column = new Column(n.forward(), n.side());
                    return !NestBlueprint.HELD_COLUMNS.contains(column) && open.contains(column.floor()) && open.contains(column.upper());
                });
                assertTrue(stand, "a free two-high stand beside " + target);
                open.add(target);
            }
            assertTrue(NestBlueprint.coveredOrder(dig, new HashSet<>(dig)));
        }
    }

    @Test
    void theCompilerKeepsWideningSidesForTheWideningOrExactlyOneHall() {
        var half = new Room("queens_hall", ChamberFunction.QUEENS_HALL, 3, -2, 3, 1, null);
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("half", List.of(), half, List.of(), -1)), "only a whole widening side");
        var left = NestBlueprint.QUEENS_HALL.getFirst().room();
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("unclaimed", List.of(), left, List.of(), 0)), "a passageless room must claim its side");
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("mirrored", List.of(), left, List.of(), 1)), "the claimed side must be the room's own");
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("tunnel", List.of(new Column(6, 0)), left, List.of(), -1)), "a passage room never takes a widening side");
        var onStore = new Room("material_store", ChamberFunction.MATERIAL_STORE, 3, 2, 3, 2, new Column(4, 2));
        assertThrows(IllegalStateException.class, () -> NestBlueprint.compile(new Placement("store", List.of(new Column(6, 0)), onStore, List.of())), "a store room still never takes a widening side");
    }
}
