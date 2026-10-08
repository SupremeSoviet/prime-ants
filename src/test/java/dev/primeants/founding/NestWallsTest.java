package dev.primeants.founding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.primeants.colony.ColonyStage;
import dev.primeants.founding.NestBlueprint.Cell;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** A chamber's wall cells, the wall-tier scan over them, the stage that unlocks each tier and the upgrade job's unit
 * accounting (NestWalls, ChamberUpgrade.Job). */
class NestWallsTest {
    private static final Set<Cell> HALL_LEFT = NestBlueprint.QUEENS_HALL.getFirst().room().cells();
    private static final Set<Cell> STORE_LEFT = NestBlueprint.MATERIAL_STORE.getFirst().room().cells();

    @Test
    void theFoundingChambersWallsAreItsFrontAndBackWallsTwoHigh() {
        var walls = NestWalls.walls(NestBlueprint.FOUNDING_CHAMBER);
        assertEquals(List.of(new Cell(6, -1, -2), new Cell(6, -1, -1), new Cell(6, 1, -2), new Cell(6, 1, -1),
            new Cell(2, -1, -2), new Cell(2, -1, -1), new Cell(2, 1, -2), new Cell(2, 1, -1)), walls, "back wall first, bottom first");
        for (var c : walls) {
            assertTrue(c.dy() == -2 || c.dy() == -1, "no floor or roof cell: " + c);
            assertFalse(NestBlueprint.foundingSpace().contains(c) || NestBlueprint.PLUGS.contains(c), "never stairs, plugs or a widening side: " + c);
        }
        // The widening sides, the plugs and the store passage are future or present openings, never walls.
        for (var opening : List.of(new Cell(4, 2, -2), new Cell(4, -2, -1), new Cell(2, 0, -2), new Cell(6, 0, -1)))
            assertFalse(walls.contains(opening), "opening " + opening);
        // T06's rooms get walls by the same rule: the hall beside the queen's chamber, the store behind it.
        assertEquals(14, NestWalls.walls(HALL_LEFT).size(), "hall_left: outer wall and both ends");
        assertFalse(NestWalls.walls(HALL_LEFT).stream().anyMatch(NestBlueprint.FOUNDING_CHAMBER::contains), "the hall opens into the queen's chamber");
        assertEquals(22, NestWalls.walls(STORE_LEFT).size(), "left store: its perimeter minus the passage column");
        assertFalse(NestWalls.walls(STORE_LEFT).contains(new Cell(7, -1, -2)), "the store passage is an opening");
    }

    /** The live scan's rule over a table of wall cells: each cell's block and whether the colony itself built it. */
    private static int scan(Map<Cell, String> blocks, Set<Cell> builtByColony) {
        var tiers = new ArrayList<Integer>();
        for (var c : NestWalls.walls(NestBlueprint.FOUNDING_CHAMBER)) tiers.add(NestWalls.tier(blocks.getOrDefault(c, "minecraft:dirt"), builtByColony.contains(c)));
        return NestWalls.chamberTier(tiers);
    }

    @Test
    void theWallTierScanTakesTheLowestCellAndOnlyColonyWork() {
        var walls = NestWalls.walls(NestBlueprint.FOUNDING_CHAMBER);
        var clay = new HashMap<Cell, String>(); walls.forEach(c -> clay.put(c, NestWalls.PACKED_CLAY));
        assertEquals(1, scan(Map.of(), Set.of()), "natural earth");
        assertEquals(1, scan(Map.of(walls.getFirst(), "prime_ants:nest_soil"), Set.of(walls.getFirst())), "colony earth is earth");
        assertEquals(2, scan(clay, Set.copyOf(walls)), "all of the colony's own packed clay");
        var partly = new HashSet<>(walls); partly.remove(walls.getLast());
        assertEquals(1, scan(clay, partly), "a block a player placed never raises a tier, even packed clay");
        var half = new HashMap<Cell, String>(); walls.subList(0, 4).forEach(c -> half.put(c, NestWalls.PACKED_CLAY));
        assertEquals(1, scan(half, Set.copyOf(walls.subList(0, 4))), "a half-rebuilt chamber stays at tier 1");
        var stone = new HashMap<Cell, String>(); walls.forEach(c -> stone.put(c, "prime_ants:nest_cut_stone"));
        assertEquals(3, scan(stone, Set.copyOf(walls)), "the colony's own nest-cut stone");
        stone.put(walls.getFirst(), "prime_ants:resin_masonry");
        assertEquals(2, scan(stone, Set.copyOf(walls)), "the lowest wall decides");
        stone.put(walls.getFirst(), "minecraft:stone_bricks");
        assertEquals(1, scan(stone, Set.copyOf(walls)), "a vanilla block is just a solid wall");
        assertEquals(1, NestWalls.chamberTier(List.of()), "a chamber without walls is earth");
    }

    @Test
    void matureUnlocksTierTwoAndGreatTierThree() {
        assertEquals(1, NestWalls.unlocked(ColonyStage.FOUNDING)); assertEquals(1, NestWalls.unlocked(ColonyStage.YOUNG));
        assertEquals(2, NestWalls.unlocked(ColonyStage.MATURE)); assertEquals(3, NestWalls.unlocked(ColonyStage.GREAT));
        assertEquals(1, NestWalls.CLAY_PER_CELL, "one clay ball rammed into the cell's own earth");
    }

    private static JsonArray pos(BlockPos p) { var a = new JsonArray(); a.add(p.getX()); a.add(p.getY()); a.add(p.getZ()); return a; }
    private static final BlockPos ENTRANCE = new BlockPos(100, 64, -40);
    private static List<BlockPos> walls() {
        var home = NestPlan.geometry(ENTRANCE, Direction.EAST);
        return NestWalls.walls(NestBlueprint.FOUNDING_CHAMBER).stream().map(c -> home.at(c.forward(), c.side(), c.dy())).toList();
    }
    private static ChamberUpgrade.Job job(List<BlockPos> cells, List<BlockPos> built, int taken, int released, int tier) { return job(cells, built, taken, released, tier, null); }
    private static ChamberUpgrade.Job job(List<BlockPos> cells, List<BlockPos> built, int taken, int released, int tier, JsonObject transfers) {
        var o = new JsonObject(); o.add("entrance", pos(ENTRANCE)); o.addProperty("direction", "east"); o.addProperty("chamber", "founding"); o.addProperty("tier", tier);
        if (transfers != null) o.add("released_transfers", transfers);
        var c = new JsonArray(); cells.forEach(p -> c.add(pos(p))); o.add("cells", c);
        var b = new JsonArray(); built.forEach(p -> b.add(pos(p))); o.add("built", b);
        o.addProperty("taken", taken); o.addProperty("released", released); o.addProperty("claim", ""); o.addProperty("ticks", 0L); o.addProperty("reason", "test");
        return ChamberUpgrade.Job.CODEC.parse(JsonOps.INSTANCE, o).getOrThrow();
    }

    @Test
    void unitsTakenAreCarriedBuiltOrInCustodyAndASaveCannotSayOtherwise() {
        var cells = walls(); var three = cells.subList(0, 3);
        // Saved states the work can reach: between cells, carrying the next cell's unit, after a dead builder's release.
        var between = job(cells, three, 3, 0, 2); assertEquals(new NestWalls.Ledger(3, 0, 3, 0), between.ledger());
        var carrying = job(cells, three, 4, 0, 2); assertEquals(new NestWalls.Ledger(4, 1, 3, 0), carrying.ledger());
        var released = job(cells, three, 5, 1, 2); assertEquals(new NestWalls.Ledger(5, 1, 3, 1), released.ledger());
        var done = job(cells, cells, 9, 1, 2); assertTrue(done.complete() && done.carried() == 0);
        for (var j : List.of(between, carrying, released, done)) assertTrue(j.ledger().exact(), "taken = carried + built + released: " + j.ledger());
        assertEquals(1, released.ledger().released(), "a dead builder's unit stays released for good, wherever custody has set it down since");
        assertEquals(cells.get(3), carrying.next(), "the next cell follows the built prefix");
        // Saves that break the accounting or the plan are refused on load.
        assertThrows(IllegalArgumentException.class, () -> job(cells, three, 5, 0, 2), "a builder carries one cell's clay at a time");
        assertThrows(IllegalArgumentException.class, () -> job(cells, three, 2, 0, 2), "more built than taken");
        assertThrows(IllegalArgumentException.class, () -> job(cells, List.of(cells.get(1)), 1, 0, 2), "built is a prefix of the plan");
        assertThrows(IllegalArgumentException.class, () -> job(List.of(ENTRANCE), List.of(), 0, 0, 2), "only the chamber's own wall cells");
        assertThrows(IllegalArgumentException.class, () -> job(cells, List.of(), 0, 0, 1), "tier 1 needs no upgrade");
        assertFalse(new NestWalls.Ledger(4, 0, 3, 0).exact(), "a unit missing from the ledger");
        // Released units name their transfer custody identities; a save naming more than were released is refused.
        var o = new JsonObject(); o.addProperty(java.util.UUID.randomUUID().toString(), 1);
        assertEquals(1, job(cells, three, 5, 1, 2, o).ledger().released());
        assertThrows(IllegalArgumentException.class, () -> job(cells, three, 4, 0, 2, o), "a transfer without a release");
        var two = new JsonObject(); two.addProperty(java.util.UUID.randomUUID().toString(), 2);
        assertThrows(IllegalArgumentException.class, () -> job(cells, three, 5, 1, 2, two), "more in transfers than released");
    }
    @Test
    void materialStoreAndHallJobsRestoreOnlyTheirOwnPlacementAndExactCargo() {
        var home=NestPlan.geometry(ENTRANCE,Direction.EAST);
        for(var function:List.of(dev.primeants.colony.ChamberFunction.MATERIAL_STORE,dev.primeants.colony.ChamberFunction.QUEENS_HALL))
            for(var placement:NestBlueprint.placements(function)){
                var cells=ChamberUpgrade.walls(home,placement.room().cells());
                var o=ChamberUpgrade.Job.CODEC.encodeStart(JsonOps.INSTANCE,job(walls(),List.of(),0,0,2)).getOrThrow().getAsJsonObject();
                o.addProperty("chamber",function==dev.primeants.colony.ChamberFunction.MATERIAL_STORE?ChamberExcavation.STORE:ChamberExcavation.HALL);
                var all=new JsonArray();cells.forEach(p->all.add(pos(p)));o.add("cells",all);
                var built=new JsonArray();cells.subList(0,2).forEach(p->built.add(pos(p)));o.add("built",built);o.addProperty("taken",3);
                var claim=UUID.randomUUID();o.addProperty("claim",claim.toString());
                var restored=ChamberUpgrade.Job.CODEC.parse(JsonOps.INSTANCE,o).getOrThrow();
                assertEquals(new NestWalls.Ledger(3,1,2,0),restored.ledger());assertEquals(claim,restored.claim);assertEquals(cells.get(2),restored.next());
                var again=ChamberUpgrade.Job.CODEC.parse(JsonOps.INSTANCE,ChamberUpgrade.Job.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow()).getOrThrow();
                assertEquals(restored.cells,again.cells);assertEquals(restored.built(),again.built());assertEquals(restored.ledger(),again.ledger());
                o.addProperty("chamber","founding");assertThrows(IllegalArgumentException.class,()->ChamberUpgrade.Job.CODEC.parse(JsonOps.INSTANCE,o).getOrThrow(),"Foreign room cells cannot load as a founding job");
            }
    }

}
