package dev.primeants.founding;

import dev.primeants.colony.ColonyStage;
import java.util.*;
import java.util.function.IntFunction;

/** The colony's mound (GDD v2 section 2, stage-1 T06), its first plan-based surface structure: soil dug out of nest-plan
 * rooms is laid on a declarative surface plan that grows with the colony's stage. The plan reuses the old blueprint's
 * "tiers" primitive (docs/04-buildings-and-nest.md, blueprint section): stacked elliptical truncated cones. Pure, in plan
 * cells, so unit tests and the live binder (MoundSoil) share it.
 * <p>A cell is (forward, side, layer): forward and side as NestPlan.at, layer 0 the cell resting on its column's own local
 * ground. The mound lies behind the entrance in two lobes, one either side of the approach lane, which stays clear, as do
 * the stairs ahead of the entrance and the exterior standing spot in the lane. Young builds the small mound; Mature a
 * wider, taller one that contains it. Founding has none: its 0.1.0 deposits stay as they are, inside Young's lobes. Great
 * keeps Mature's spoil capacity; SurfacePlan describes its separately paid structural additions.
 * <pre>
 *   Young: layers per column (. lane, E entrance)       Mature: two wider lobes, 23 x 13 columns, 5 layers high
 *   f=-8        1 1 1 1   .   1 1 1 1
 *   f=-7      1 2 2 2 2 1 . 1 2 2 2 2 1
 *   f=-6      2 3 3 3 3 2 . 2 3 3 3 3 2
 *   f=-5    1 2 3 3 3 3 2 . 2 3 3 3 3 2 1
 *   f=-4    1 2 3 3 3 3 2 . 2 3 3 3 3 2 1
 *   f=-3    1 2 3 3 3 3 2 . 2 3 3 3 3 2 1
 *   f=-2      2 3 3 3 3 2 . 2 3 3 3 3 2
 *   f=-1      1 2 2 2 2 1 . 1 2 2 2 2 1
 *   f= 0                  E          s = -7 .. 7
 * </pre> */
public final class NestMound {
    /** One tier of the blueprint primitive: from layer baseY up height layers, an ellipse whose radii shrink linearly from
     * the base radii to the top radii (read at each layer's middle), centred offsetForward/offsetSide from the entrance. */
    public record Tier(int baseY, int height, double baseForward, double baseSide, double topForward, double topSide, double offsetForward, double offsetSide) {
        public Tier {
            if (baseY < 0 || height < 1 || topForward <= 0 || topSide <= 0 || topForward > baseForward || topSide > baseSide) throw new IllegalArgumentException("Invalid mound tier");
        }
        public boolean contains(int forward, int side, int layer) {
            if (layer < baseY || layer >= baseY + height) return false;
            double t = (layer - baseY + 0.5) / height, rf = baseForward + (topForward - baseForward) * t, rs = baseSide + (topSide - baseSide) * t;
            double df = (forward - offsetForward) / rf, ds = (side - offsetSide) / rs;
            return df * df + ds * ds <= 1;
        }
        /** Normalised distance of a cell from the tier's base centre: deposits grow each tier as a dome. */
        double distance(int forward, int side, int layer) {
            double df = (forward - offsetForward) / baseForward, ds = (side - offsetSide) / baseSide, dl = (layer - baseY + 0.5) / height;
            return df * df + ds * ds + dl * dl;
        }
    }
    public record Cell(int forward, int side, int layer) { }
    /** Declared bounds: the footprint lies within BACK cells behind the entrance and SIDE cells to either side of the lane,
     * at most LAYERS high. */
    public static final int BACK = 14, SIDE = 12, LAYERS = 6;
    /** A column's local ground is searched within this many cells above and below the entrance's level: the mound follows
     * the land near the nest, never a cliff top or a pit beyond it. */
    public static final int GROUND_RANGE = 2;
    public static final List<Tier> YOUNG = lobes(3, 4.8, 4.0, 2.6, 2.0, -4.0, 3.5), MATURE = lobes(5, 7.6, 6.6, 3.4, 2.8, -6.0, 5.0);
    private static List<Tier> lobes(int height, double baseForward, double baseSide, double topForward, double topSide, double forward, double side) {
        return List.of(new Tier(0, height, baseForward, baseSide, topForward, topSide, forward, -side), new Tier(0, height, baseForward, baseSide, topForward, topSide, forward, side));
    }
    public static List<Tier> tiers(ColonyStage s) { return switch (s) { case FOUNDING -> List.of(); case YOUNG -> YOUNG; case MATURE, GREAT -> MATURE; }; }
    /** The route the mound never covers: the approach lane behind the entrance (side 0), which holds the exterior standing
     * spot (-2, 0), and the stairs and everything ahead of the entrance (forward 0 and more). */
    public static boolean route(int forward, int side) { return side == 0 || forward >= 0; }
    private static final Map<ColonyStage, List<Cell>> PLANS = new EnumMap<>(ColonyStage.class);
    static { for (var s : ColonyStage.values()) PLANS.put(s, compile(tiers(s))); }
    private NestMound() { }
    /** The stage's compiled deposit cells, in deposit order. */
    public static List<Cell> plan(ColonyStage s) { return PLANS.get(s); }
    /** Compiles tiers into ordered deposit cells: every in-bounds cell outside the route that a tier contains, each
     * column filled from its ground up without a gap, ordered as growing domes (by each cell's least tier distance) and
     * never before the cell beneath it. */
    public static List<Cell> compile(List<Tier> tiers) {
        for (var t : tiers) {
            // A tier is widest in its bottom layer, read at that layer's middle.
            double rf = t.baseForward() + (t.topForward() - t.baseForward()) * 0.5 / t.height(), rs = t.baseSide() + (t.topSide() - t.baseSide()) * 0.5 / t.height();
            if (t.offsetForward() - rf <= -BACK - 1 || Math.abs(t.offsetSide()) + rs >= SIDE + 1 || t.baseY() + t.height() > LAYERS) throw new IllegalStateException("Mound tier outside its declared bounds: " + t);
        }
        var key = new HashMap<Cell, Double>(); var cells = new ArrayList<Cell>();
        for (int f = -BACK; f <= -1; f++) for (int s = -SIDE; s <= SIDE; s++) {
            if (route(f, s)) continue;
            double below = Double.NEGATIVE_INFINITY;
            for (int layer = 0; layer < LAYERS; layer++) {
                final int ff = f, ss = s, ll = layer;
                var d = tiers.stream().filter(t -> t.contains(ff, ss, ll)).mapToDouble(t -> t.distance(ff, ss, ll)).min();
                if (d.isEmpty()) break; // a column holds no cell above a gap
                var cell = new Cell(f, s, layer); below = Math.max(below, d.getAsDouble()); key.put(cell, below); cells.add(cell);
            }
        }
        cells.sort(Comparator.<Cell>comparingDouble(key::get).thenComparingInt(Cell::layer).thenComparingInt(c -> -c.forward()).thenComparingInt(c -> Math.abs(c.side())).thenComparingInt(Cell::side));
        return List.copyOf(cells);
    }
    /** What one block of a mound column is, read live. NATURAL_GROUND is generated soil or stone the colony has witnessed;
     * PLANT a witnessed short native plant, which a deposit buries; OTHER is anything else: a fluid, a player's block, a
     * block entity, a log, leaves, a crop, sand or snow. */
    public enum Read { NATURAL_GROUND, MOUND, OPEN, PLANT, OTHER, UNLOADED }
    /** A planned layer bound to its column: the colony's own soil (FILLED), room for a unit once the layers beneath are
     * filled (FREE), never usable while the column stays as it is (BLOCKED), or not loaded (UNKNOWN). */
    public enum Slot { FILLED, FREE, BLOCKED, UNKNOWN }
    /** One column on its own local ground: the ground's height relative to the entrance's level (null when the column has
     * none in range or is not loaded), and its planned layers. */
    public record Column(Integer ground, List<Slot> layers) { }
    /** Binds one planned column of this height to the land. read(dy) reads the block dy above the entrance's level. The
     * ground is the highest natural ground in range whose next block up is open, a plant or the colony's mound; the
     * layers above it take soil where they are open or a plant, and a block of any other kind stops the column there. */
    public static Column column(IntFunction<Read> read, int height) {
        Integer ground = null;
        for (int dy = GROUND_RANGE; dy >= -GROUND_RANGE; dy--) {
            var r = read.apply(dy);
            if (r == Read.UNLOADED) return new Column(null, Collections.nCopies(height, Slot.UNKNOWN));
            if (r != Read.NATURAL_GROUND) continue;
            var above = read.apply(dy + 1);
            if (above == Read.UNLOADED) return new Column(null, Collections.nCopies(height, Slot.UNKNOWN));
            if (above == Read.OPEN || above == Read.PLANT || above == Read.MOUND) { ground = dy; break; }
        }
        if (ground == null) return new Column(null, Collections.nCopies(height, Slot.BLOCKED));
        var layers = new ArrayList<Slot>(); boolean blocked = false, unknown = false;
        for (int layer = 0; layer < height; layer++) {
            var r = read.apply(ground + 1 + layer);
            if (r == Read.MOUND) layers.add(Slot.FILLED);
            else if (blocked) layers.add(Slot.BLOCKED);
            else if (r == Read.UNLOADED || unknown) { unknown = true; layers.add(Slot.UNKNOWN); }
            else if (r == Read.OPEN || r == Read.PLANT) layers.add(Slot.FREE);
            else { blocked = true; layers.add(Slot.BLOCKED); }
        }
        return new Column(ground, List.copyOf(layers));
    }
    /** Each planned column's height, by (forward, side). */
    public static Map<List<Integer>, Integer> heights(List<Cell> plan) {
        var out = new HashMap<List<Integer>, Integer>();
        for (var c : plan) out.merge(List.of(c.forward(), c.side()), c.layer() + 1, Math::max);
        return out;
    }
}
