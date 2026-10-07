package dev.primeants.founding;

import dev.primeants.colony.ColonyStage;
import dev.primeants.founding.NestBlueprint.Cell;
import java.util.*;

/** A chamber's walls and their tier (GDD v2 section 2, stage-1 T05). Pure, in nest-plan cells, so unit tests and the
 * live scan share it.
 * <p>A chamber's wall cells are the horizontal faces of its open cells that the nest plan never opens: not the chamber's
 * own cells, the entrance stairs and plugs, either 0.1.0 widening side, or any placement's dug or reserved cells (a
 * passage, a room, or a room kept free for later). Floor and roof are not walls: founding allows witnessed stone under
 * the floor, which no upgrade may convert, and the roof is the ground's surface layer. For the founding chamber this is
 * eight cells, two high: the front wall beside the stairs and the back wall beside the store passage.
 * <p>A wall cell's tier is 2 for the colony's own packed clay or resin masonry, 3 for its own nest-cut stone, and 1 for
 * anything else solid: natural or colony earth, natural stone, or a block a player placed. A chamber's confirmed tier is
 * the lowest tier over its wall cells, read live. */
public final class NestWalls {
    /** Built wall blocks by registry id and the tier each gives when the colony itself built it. */
    public static final Map<String, Integer> TIERS = Map.of("prime_ants:packed_clay", 2, "prime_ants:resin_masonry", 2, "prime_ants:nest_cut_stone", 3);
    /** The tier-2 wall Lasius niger builds. Resin masonry is for resin-collecting species; nest-cut stone waits for T06. */
    public static final String PACKED_CLAY = "prime_ants:packed_clay";
    /** Clay units one packed-clay cell takes: the cell's own earth stays in place, rammed with one clay ball, so no soil
     * leaves the nest and nothing goes to the mound. */
    public static final int CLAY_PER_CELL = 1;
    /** Cells any nest-plan work may open, dug or not yet: they are future openings, never walls. */
    public static final Set<Cell> OPENABLE = openable();
    private NestWalls() { }
    private static Set<Cell> openable() {
        var out = new LinkedHashSet<Cell>(NestBlueprint.foundingSpace()); out.addAll(NestBlueprint.PLUGS);
        for (var placements : List.of(NestBlueprint.MATERIAL_STORE, NestBlueprint.QUEENS_HALL)) for (var p : placements) {
            out.addAll(NestBlueprint.plan(p.name()).dig());
            for (var r : p.reserved()) out.addAll(r.cells());
        }
        return Collections.unmodifiableSet(out);
    }
    /** The wall cells of a chamber with these open cells, in a fixed order: by forward, then side, bottom first. */
    public static List<Cell> walls(Set<Cell> room) {
        var out = new TreeSet<Cell>(Comparator.comparingInt(Cell::forward).reversed().thenComparingInt(Cell::side).thenComparingInt(Cell::dy));
        for (var c : room) for (var n : c.neighbors()) if (n.dy() == c.dy() && !room.contains(n) && !OPENABLE.contains(n)) out.add(n);
        return List.copyOf(out);
    }
    /** One wall cell's tier: a built wall block counts only when this colony built it. */
    public static int tier(String block, boolean builtByColony) {
        var tier = TIERS.get(block);
        return tier != null && builtByColony ? tier : 1;
    }
    /** A chamber's tier: the lowest of its wall cells' tiers (1 for a chamber without walls). */
    public static int chamberTier(Collection<Integer> wallTiers) { return wallTiers.stream().mapToInt(Integer::intValue).min().orElse(1); }
    /** The highest chamber tier a stage has unlocked (GDD v2 section 1): Mature unlocks tier 2, Great tier 3. */
    public static int unlocked(ColonyStage s) { return switch (s) { case FOUNDING, YOUNG -> 1; case MATURE -> 2; case GREAT -> 3; }; }
    /** Exact accounting of one upgrade job: units taken out of the store are carried, built into walls or in custody. */
    public record Ledger(int taken, int carried, int built, int custody) {
        public Ledger { if (taken < 0 || carried < 0 || built < 0 || custody < 0) throw new IllegalArgumentException("Negative upgrade units"); }
        public boolean exact() { return taken == carried + built + custody; }
    }
}
