package dev.primeants.founding;

import dev.primeants.colony.ChamberFunction;
import java.util.*;

/** The Lasius niger nest plan (decisions 6 and 24): semantic rooms and passages in the founding nest's frame, compiled
 * into a dig-task queue that real workers carry out block by block. It never places blocks.
 * <p>A cell is (forward, side, dy) from the entrance, exactly as NestPlan.at; side is positive clockwise of the nest
 * direction. Every space is two high: floor cells at dy -2 under cells at dy -1, the founding chamber's depth.
 * <pre>
 *   f\s -4 -3 -2 -1  0  1  2  3  4
 *    3      w  w  F  F  F  w  w        F founding chamber, w either 0.1.0 widening side
 *    6               .  P  .           P passage through the founding back wall
 *    7   L  L  L  P  P  P  R  R  R     L left room, R right room
 *    8   L  L  L           R  R  R
 *    9   L  L  L           R  R  R
 * </pre>
 * A placement digs the passage and one room, and keeps the opposite room free for the queen's hall. */
public final class NestBlueprint {
    /** Floor and ceiling cells of every two-high space. */
    public static final int FLOOR = -2, UPPER = -1;
    /** Declared bounds for every planned cell and shell cell: at most RADIUS forward or sideways from the entrance,
     * from DEPTH below the entrance level up to the entrance level (the roof). */
    public static final int RADIUS = 10, DEPTH = 3;
    /** Hard cap on one placement's dig queue, below the 48-block contract maximum. */
    public static final int MAX_DIG = 32;
    public record Cell(int forward, int side, int dy) {
        public List<Cell> neighbors() {
            return List.of(new Cell(forward + 1, side, dy), new Cell(forward - 1, side, dy), new Cell(forward, side + 1, dy),
                new Cell(forward, side - 1, dy), new Cell(forward, side, dy + 1), new Cell(forward, side, dy - 1));
        }
        public boolean inBounds() { return Math.abs(forward) <= RADIUS && Math.abs(side) <= RADIUS && dy >= -DEPTH && dy <= 0; }
    }
    /** A floor column of a two-high space. */
    public record Column(int forward, int side) {
        public Cell floor() { return new Cell(forward, side, FLOOR); }
        public Cell upper() { return new Cell(forward, side, UPPER); }
        boolean adjacent(Column o) { return Math.abs(forward - o.forward) + Math.abs(side - o.side) == 1; }
    }
    /** A two-high room: a box of floor columns, what it is for, and the floor column that holds its marker block. */
    public record Room(String id, ChamberFunction function, int forward, int side, int length, int width, Column marker) {
        public Room {
            if (length < 1 || width < 1 || !contains(forward, side, length, width, marker)) throw new IllegalArgumentException("Invalid room " + id);
        }
        private static boolean contains(int f, int s, int length, int width, Column c) {
            return c.forward() >= f && c.forward() < f + length && c.side() >= s && c.side() < s + width;
        }
        public boolean contains(Column c) { return contains(forward, side, length, width, c); }
        public List<Column> columns() {
            var out = new ArrayList<Column>();
            for (int f = forward; f < forward + length; f++) for (int s = side; s < side + width; s++) out.add(new Column(f, s));
            return List.copyOf(out);
        }
        public Set<Cell> cells() {
            var out = new LinkedHashSet<Cell>();
            for (var c : columns()) { out.add(c.floor()); out.add(c.upper()); }
            return out;
        }
    }
    /** One placement: passage columns in dig order from the founding chamber, the room they open, and the rooms it
     * keeps free for later work. */
    public record Placement(String name, List<Column> passage, Room room, List<Room> reserved) { }
    /** A compiled placement in plan cells. dig is the queue: passage then room, each column bottom first. */
    public record Plan(Placement placement, List<Cell> dig, Set<Cell> shell, Set<Cell> connections, Set<Cell> openings, Cell marker) { }

    // The 0.1.0 founding nest and its possible widenings, as NestPlan.geometry and NestExpansion.targets declare them.
    public static final Set<Cell> STAIRS = stairs();
    public static final Set<Cell> PLUGS = Set.of(new Cell(2, 0, -2), new Cell(2, 0, -1));
    public static final Set<Cell> FOUNDING_CHAMBER = box(3, 5, -1, 1);
    public static final Set<Cell> WIDENINGS = union(box(3, 5, 2, 3), box(3, 5, -3, -2));
    /** Exterior soil deposits: five rows behind the entrance, one to four cells to either side, adapted by one block up
     * or down, plus the layer above them. */
    public static final Set<Cell> DEPOSITS = deposits();

    private static final Room LEFT = new Room("material_store", ChamberFunction.MATERIAL_STORE, 7, -4, 3, 3, new Column(8, -4));
    private static final Room RIGHT = new Room("material_store", ChamberFunction.MATERIAL_STORE, 7, 2, 3, 3, new Column(8, 4));
    private static Room hall(Room at) { return new Room("queens_hall", ChamberFunction.QUEENS_HALL, at.forward(), at.side(), at.length(), at.width(), at.marker()); }
    /** The material store's candidate placements, tried in this order against live terrain. */
    public static final List<Placement> MATERIAL_STORE = List.of(
        new Placement("left", List.of(new Column(6, 0), new Column(7, 0), new Column(7, -1)), LEFT, List.of(hall(RIGHT))),
        new Placement("right", List.of(new Column(6, 0), new Column(7, 0), new Column(7, 1)), RIGHT, List.of(hall(LEFT))));
    private static final Map<String, Plan> COMPILED = new LinkedHashMap<>();
    static { for (var p : MATERIAL_STORE) COMPILED.put(p.name(), compile(p)); }
    private NestBlueprint() { }

    public static List<Placement> placements(ChamberFunction function) {
        return function == ChamberFunction.MATERIAL_STORE ? MATERIAL_STORE : List.of();
    }
    public static Plan plan(String placement) {
        var plan = COMPILED.get(placement);
        if (plan == null) throw new IllegalArgumentException("Unknown nest plan placement " + placement);
        return plan;
    }
    /** Space that is open, or may become open, in any colony: stairs, founding chamber and both widening sides. */
    public static Set<Cell> foundingSpace() { return union(STAIRS, union(FOUNDING_CHAMBER, WIDENINGS)); }

    /** Validates a placement and compiles its dig queue. The plan must stay in bounds, dig only new cells, leave the
     * founding nest, both widening sides, the deposits and its reserved rooms untouched, form one walkable two-high
     * space with the founding chamber, and keep every pending cell covered while it waits (NestPlan.bottomFirst). */
    public static Plan compile(Placement p) {
        var room = p.room().columns();
        var columns = new ArrayList<Column>(p.passage());
        var start = p.passage().getLast();
        for (var c : serpentine(room, start)) columns.add(c);
        if (new HashSet<>(columns).size() != columns.size()) fail(p, "repeated column");
        var dig = new ArrayList<Cell>();
        for (var c : columns) { dig.add(c.floor()); dig.add(c.upper()); }
        if (dig.size() > MAX_DIG) fail(p, "dig queue above cap " + MAX_DIG);
        var digSet = new HashSet<>(dig);
        var connections = new LinkedHashSet<Cell>(); var shell = new LinkedHashSet<Cell>(); var openings = new LinkedHashSet<Cell>();
        for (var c : dig) for (var n : c.neighbors()) {
            if (digSet.contains(n)) continue;
            if (FOUNDING_CHAMBER.contains(n)) { connections.add(n); openings.add(c); }
            else shell.add(n);
        }
        var protectedCells = union(foundingSpace(), union(PLUGS, DEPOSITS));
        for (var c : dig) {
            if (!c.inBounds()) fail(p, "dig cell out of bounds " + c);
            if (protectedCells.contains(c)) fail(p, "dig cell overlaps the founding nest, a widening or a deposit " + c);
        }
        for (var c : shell) {
            if (!c.inBounds()) fail(p, "shell cell out of bounds " + c);
            if (protectedCells.contains(c)) fail(p, "shell cell would be open space " + c);
        }
        for (var r : p.reserved()) for (var c : r.cells()) {
            if (!c.inBounds() || protectedCells.contains(c) || digSet.contains(c) || shell.contains(c)) fail(p, "reserved " + r.id() + " is not free at " + c);
        }
        if (connections.isEmpty()) fail(p, "passage does not reach the founding chamber");
        if (!walkableFromFounding(columns)) fail(p, "not one walkable two-high space with the founding chamber");
        if (!coveredOrder(dig, digSet)) fail(p, "dig order exposes a pending cell or lacks a supported stand");
        return new Plan(p, List.copyOf(dig), Collections.unmodifiableSet(shell), Collections.unmodifiableSet(connections),
            Collections.unmodifiableSet(openings), p.room().marker().floor());
    }
    /** Rows of the room ordered away from the passage, alternating direction, so each column touches the last. */
    private static List<Column> serpentine(List<Column> room, Column from) {
        var sides = room.stream().map(Column::side).distinct().sorted(Comparator.comparingInt(s -> Math.abs(s - from.side()))).toList();
        var forwards = room.stream().map(Column::forward).distinct().sorted(Comparator.comparingInt(f -> Math.abs(f - from.forward()))).toList();
        var out = new ArrayList<Column>();
        for (int i = 0; i < sides.size(); i++) {
            var row = new ArrayList<>(forwards);
            if (i % 2 == 1) Collections.reverse(row);
            for (int f : row) out.add(new Column(f, sides.get(i)));
        }
        return out;
    }
    /** Every new floor column connects to the founding chamber's floor through two-high columns. */
    static boolean walkableFromFounding(List<Column> columns) {
        var open = new HashSet<Column>(columns);
        for (var c : FOUNDING_CHAMBER) open.add(new Column(c.forward(), c.side()));
        var reached = new HashSet<Column>(); var queue = new ArrayDeque<Column>(); queue.add(new Column(4, 0));
        while (!queue.isEmpty()) {
            var c = queue.remove();
            if (!reached.add(c)) continue;
            for (var n : open) if (n.adjacent(c) && !reached.contains(n)) queue.add(n);
        }
        return reached.containsAll(columns);
    }
    /** Replays the queue: before each removal every pending cell keeps a solid cell above it (no exposed pending soil,
     * NestPlan.bottomFirst) and the target has an exposed face beside an open two-high column on solid support. */
    static boolean coveredOrder(List<Cell> dig, Set<Cell> digSet) {
        var open = new HashSet<Cell>(FOUNDING_CHAMBER);
        for (int i = 0; i < dig.size(); i++) {
            for (var pending : dig.subList(i, dig.size())) if (open.contains(new Cell(pending.forward(), pending.side(), pending.dy() + 1))) return false;
            var target = dig.get(i);
            if (target.dy() == UPPER && !open.contains(new Cell(target.forward(), target.side(), FLOOR))) return false;
            boolean stand = false;
            for (var n : target.neighbors()) {
                if (n.dy() != target.dy()) continue;
                var floor = new Cell(n.forward(), n.side(), FLOOR);
                var upper = new Cell(n.forward(), n.side(), UPPER);
                var support = new Cell(n.forward(), n.side(), FLOOR - 1);
                stand |= open.contains(floor) && open.contains(upper) && !open.contains(support) && !digSet.contains(support);
            }
            if (!stand) return false;
            open.add(target);
        }
        return true;
    }
    private static void fail(Placement p, String why) { throw new IllegalStateException("Invalid nest plan placement " + p.name() + ": " + why); }
    private static Set<Cell> box(int f0, int f1, int s0, int s1) {
        var out = new LinkedHashSet<Cell>();
        for (int f = f0; f <= f1; f++) for (int s = s0; s <= s1; s++) for (int dy = FLOOR; dy <= UPPER; dy++) out.add(new Cell(f, s, dy));
        return Collections.unmodifiableSet(out);
    }
    private static Set<Cell> union(Set<Cell> a, Set<Cell> b) { var out = new LinkedHashSet<>(a); out.addAll(b); return Collections.unmodifiableSet(out); }
    private static Set<Cell> stairs() {
        var out = new LinkedHashSet<Cell>();
        for (int f = 0; f < 3; f++) for (int y = 0; y >= -f; y--) out.add(new Cell(f, 0, y));
        return Collections.unmodifiableSet(out);
    }
    private static Set<Cell> deposits() {
        var out = new LinkedHashSet<Cell>();
        for (int f = -5; f <= -1; f++) for (int s = -4; s <= 4; s++) if (s != 0) for (int dy = 0; dy <= 3; dy++) out.add(new Cell(f, s, dy));
        return Collections.unmodifiableSet(out);
    }
}
