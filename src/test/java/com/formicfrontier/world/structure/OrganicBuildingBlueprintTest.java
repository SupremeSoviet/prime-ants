package com.formicfrontier.world.structure;

import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.world.ColonyBuilder;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class OrganicBuildingBlueprintTest {
	@Test
	void foodStoresAreLowSingleStoreyConnectedMoundsWithDistinctSilhouettes() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.FOOD_STORE);
		Assertions.assertEquals(2, variants.size(), "repeated food stores need two authored silhouettes");
		Assertions.assertEquals(Set.of("food_store_a", "food_store_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));

		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("food_store", blueprint.palette());
			Assertions.assertTrue(blueprint.maxY() <= 6, "food stores should stay visibly single-storey");
			Assertions.assertEquals(1, blueprint.chambers().size());
			Assertions.assertEquals("food_store", blueprint.chambers().getFirst().purpose());
			Assertions.assertTrue(blueprint.connections().isEmpty(), "a single-storey store needs no stairs");
			Assertions.assertEquals(1, blueprint.mouths().size());
			Assertions.assertFalse(blueprint.isSolid(0, 2, 1), "the granary interior must be carved");
			assertRearShell(blueprint, blueprint.chambers().getFirst());

			Set<Cell> solid = solidCells(blueprint);
			Assertions.assertFalse(solid.isEmpty());
			Set<Cell> connected = connectedCells(solid);
			Set<Cell> disconnected = new HashSet<>(solid);
			disconnected.removeAll(connected);
			Assertions.assertEquals(solid.size(), connected.size(),
					blueprint.name() + " must be one connected hill; disconnected cells: " + disconnected);
			Set<FootprintCell> footprint = solid.stream().filter(cell -> cell.y == 0)
					.map(cell -> new FootprintCell(cell.x, cell.z)).collect(Collectors.toSet());
			Assertions.assertTrue(footprint.size() >= 130, "food store should read as a substantial chamber");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "variants need genuinely different footprints, not only different seeds");
	}

	@Test
	void nurseriesAreWarmBulbousSingleStoreyMoundsWithDistinctSilhouettes() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.NURSERY);
		Assertions.assertEquals(2, variants.size(), "repeated nurseries need two authored silhouettes");
		Assertions.assertEquals(Set.of("nursery_a", "nursery_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));

		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("nursery", blueprint.palette());
			Assertions.assertTrue(blueprint.maxY() <= 7, "nurseries should remain single-storey brood domes");
			Assertions.assertEquals(1, blueprint.chambers().size());
			Assertions.assertEquals("nursery", blueprint.chambers().getFirst().purpose());
			Assertions.assertTrue(blueprint.connections().isEmpty(), "a single-storey nursery needs no stairs");
			Assertions.assertEquals(1, blueprint.mouths().size());
			Assertions.assertFalse(blueprint.isSolid(0, 2, 2), "the brood room interior must be carved");
			assertRearShell(blueprint, blueprint.chambers().getFirst());

			Set<Cell> solid = solidCells(blueprint);
			Assertions.assertFalse(solid.isEmpty());
			Set<Cell> connected = connectedCells(solid);
			Set<Cell> disconnected = new HashSet<>(solid);
			disconnected.removeAll(connected);
			Assertions.assertEquals(solid.size(), connected.size(),
					blueprint.name() + " must be one connected brood dome; disconnected cells: " + disconnected);
			Set<FootprintCell> footprint = solid.stream().filter(cell -> cell.y == 0)
					.map(cell -> new FootprintCell(cell.x, cell.z)).collect(Collectors.toSet());
			Assertions.assertTrue(footprint.size() >= 150, "nursery should be visibly fuller than a tiny marker hut");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "nursery variants need genuinely different footprints");
	}

	@Test
	void positionSelectorIsStableAndUsesBothFoodStoreVariants() {
		Set<String> selected = new HashSet<>();
		for (int x = 0; x < 64; x++) {
			BlockPos center = new BlockPos(x, 0, x / 3);
			String first = OrganicBuildingPlacer.blueprintFor(BuildingType.FOOD_STORE, center).name();
			String second = OrganicBuildingPlacer.blueprintFor(BuildingType.FOOD_STORE, center).name();
			Assertions.assertEquals(first, second, "the same site must keep its silhouette across rebuilds");
			selected.add(first);
		}
		Assertions.assertEquals(Set.of("food_store_a", "food_store_b"), selected);
	}

	@Test
	void positionSelectorIsStableAndUsesBothNurseryVariants() {
		Set<String> selected = new HashSet<>();
		for (int x = 0; x < 64; x++) {
			BlockPos center = new BlockPos(-x, 0, -x / 3);
			String first = OrganicBuildingPlacer.blueprintFor(BuildingType.NURSERY, center).name();
			String second = OrganicBuildingPlacer.blueprintFor(BuildingType.NURSERY, center).name();
			Assertions.assertEquals(first, second, "the same nursery site must keep its silhouette across rebuilds");
			selected.add(first);
		}
		Assertions.assertEquals(Set.of("nursery_a", "nursery_b"), selected);
	}

	@Test
	void minesAreLowRoundedMoundsWithAValidatedSteppedPit() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.MINE);
		Assertions.assertEquals(Set.of("mine_a", "mine_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("mine", blueprint.palette());
			Assertions.assertTrue(blueprint.maxY() <= 6, "mine should remain a low rounded mound");
			TieredMoundBlueprint.Chamber chamber = blueprint.chambers().getFirst();
			Assertions.assertEquals("mine", chamber.purpose());
			Assertions.assertEquals(1, blueprint.pits().size(), "every mine variant must declare its excavation");
			TieredMoundBlueprint.Pit pit = blueprint.pits().getFirst();
			Assertions.assertEquals(2, pit.depth());
			Assertions.assertEquals(2, pit.depthAt(pit.x(), pit.z()), "pit center should reach the full shallow depth");
			Assertions.assertEquals(-2, blueprint.minY());
			Assertions.assertFalse(blueprint.isSolid(pit.x(), chamber.floorY(), pit.z()),
					"mine chamber floor must open into the pit");
			assertRearShell(blueprint, chamber);
			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "mine mound");
			Set<FootprintCell> footprint = footprint(solid);
			Assertions.assertTrue(footprint.size() >= 130, "mine needs a substantial rounded footprint");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "mine variants need distinct footprints");
	}

	@Test
	void chitinFarmsReuseTheLowMoundLanguageWithoutAnyPit() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.CHITIN_FARM);
		Assertions.assertEquals(Set.of("chitin_farm_a", "chitin_farm_b", "chitin_farm_c"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("chitin_farm", blueprint.palette());
			Assertions.assertTrue(blueprint.maxY() <= 5, "chitin farm should stay lower than the nursery");
			TieredMoundBlueprint.Chamber chamber = blueprint.chambers().getFirst();
			Assertions.assertEquals("chitin_farm", chamber.purpose());
			Assertions.assertTrue(blueprint.pits().isEmpty(), "chitin farm must explicitly remain pit-free");
			Assertions.assertEquals(0, blueprint.minY());
			Assertions.assertTrue(blueprint.isSolid(0, 0, 4), "farm cultivation floor must remain continuous");
			assertRearShell(blueprint, chamber);
			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "chitin farm mound");
			Set<FootprintCell> footprint = footprint(solid);
			Assertions.assertTrue(footprint.size() >= 150, "chitin farm should be a broad cultivation chamber");
			footprints.add(footprint);
		}
		Assertions.assertEquals(3, footprints.size(), "all three farm sites need distinct authored footprints");
	}

	@Test
	void barracksAreWideElongatedFortifiedMounds() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.BARRACKS);
		Assertions.assertEquals(Set.of("barracks_a", "barracks_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("barracks", blueprint.palette());
			Assertions.assertTrue(blueprint.maxY() <= 7, "barracks should remain a broad single-storey mound");
			TieredMoundBlueprint.Chamber chamber = blueprint.chambers().getFirst();
			Assertions.assertEquals("barracks", chamber.purpose());
			Assertions.assertTrue(blueprint.pits().isEmpty());
			Assertions.assertTrue(blueprint.connections().isEmpty());
			Assertions.assertEquals(5, blueprint.mouths().getFirst().width(),
					"barracks needs a wider troop entrance than economy rooms");
			Assertions.assertFalse(blueprint.isSolid(0, 2, 1), "barracks hall must be carved");
			assertRearShell(blueprint, chamber);
			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "barracks mound");
			Set<FootprintCell> footprint = footprint(solid);
			int width = footprint.stream().mapToInt(FootprintCell::x).max().orElseThrow()
					- footprint.stream().mapToInt(FootprintCell::x).min().orElseThrow() + 1;
			int depth = footprint.stream().mapToInt(FootprintCell::z).max().orElseThrow()
					- footprint.stream().mapToInt(FootprintCell::z).min().orElseThrow() + 1;
			Assertions.assertTrue(width >= 22 && width >= depth + 5,
					"barracks should read as a long rounded rectangle, got " + width + "x" + depth);
			Assertions.assertTrue(footprint.size() >= 200, "barracks needs a substantial troop hall footprint");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "barracks variants need distinct asymmetrical footprints");
	}

	@Test
	void marketsAreLowRooflessCourtyardsWithDistinctPerimeters() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.MARKET);
		Assertions.assertEquals(Set.of("market_a", "market_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("market", blueprint.palette());
			Assertions.assertEquals(3, blueprint.maxY(), "market banks should stay low and visibly open");
			Assertions.assertEquals(1, blueprint.chambers().size());
			TieredMoundBlueprint.Chamber yard = blueprint.chambers().getFirst();
			Assertions.assertEquals("market", yard.purpose());
			Assertions.assertTrue(yard.openToSky(), "market courtyard must opt into a full-height carving");
			Assertions.assertEquals(blueprint.maxY(), yard.topY(), "courtyard opening must reach the mound top");
			Assertions.assertTrue(blueprint.pits().isEmpty());
			Assertions.assertTrue(blueprint.connections().isEmpty());
			Assertions.assertEquals(5, blueprint.mouths().getFirst().width(), "market needs a broad public entrance");
			Assertions.assertTrue(blueprint.isSolid(yard.x(), yard.floorY(), yard.z()),
					"market courtyard needs a continuous packed-earth floor");
			for (int y = yard.floorY() + 1; y <= blueprint.maxY(); y++) {
				Assertions.assertFalse(blueprint.isSolid(yard.x(), y, yard.z()),
						"market courtyard must stay roofless through y=" + y);
			}

			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "market perimeter");
			Set<FootprintCell> footprint = footprint(solid);
			Assertions.assertTrue(footprint.size() >= 180, "market needs a substantial public courtyard footprint");
			long topRing = solid.stream().filter(cell -> cell.y == blueprint.maxY()).count();
			Assertions.assertTrue(topRing >= 24, "roofless market still needs a readable upper perimeter ring");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "market variants need genuinely different perimeter silhouettes");
	}

	@Test
	void repeatedRoleSitesSelectEveryAuthoredVariant() {
		BlockPos origin = new BlockPos(11, 0, -7);
		Set<String> mineVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.MINE, existing);
			mineVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.MINE, site).name());
		}
		Assertions.assertEquals(Set.of("mine_a", "mine_b"), mineVariants);

		Set<String> farmVariants = new HashSet<>();
		for (int existing = 0; existing < 3; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.CHITIN_FARM, existing);
			farmVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.CHITIN_FARM, site).name());
		}
		Assertions.assertEquals(Set.of("chitin_farm_a", "chitin_farm_b", "chitin_farm_c"), farmVariants);

		Set<String> barracksVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, existing);
			barracksVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.BARRACKS, site).name());
		}
		Assertions.assertEquals(Set.of("barracks_a", "barracks_b"), barracksVariants);

		Set<String> marketVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.MARKET, existing);
			marketVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.MARKET, site).name());
		}
		Assertions.assertEquals(Set.of("market_a", "market_b"), marketVariants);
	}

	private static void assertConnected(TieredMoundBlueprint blueprint, Set<Cell> solid, String label) {
		Assertions.assertFalse(solid.isEmpty());
		Set<Cell> connected = connectedCells(solid);
		Set<Cell> disconnected = new HashSet<>(solid);
		disconnected.removeAll(connected);
		Assertions.assertEquals(solid.size(), connected.size(),
				blueprint.name() + " must be one connected " + label + "; disconnected cells: " + disconnected);
	}

	private static void assertRearShell(TieredMoundBlueprint blueprint, TieredMoundBlueprint.Chamber chamber) {
		for (int y = chamber.floorY() + 1; y <= chamber.topY(); y++) {
			double scale = y == chamber.topY() ? 0.72 : y == chamber.topY() - 1 ? 0.9 : 1.0;
			int wallZ = (int) Math.floor(chamber.z() + chamber.radiusZ() * scale) + 1;
			Assertions.assertTrue(blueprint.isSolid(chamber.x(), y, wallZ),
					blueprint.name() + " must retain a rear shell at y=" + y + ", z=" + wallZ);
		}
	}

	private static Set<FootprintCell> footprint(Set<Cell> solid) {
		return solid.stream().filter(cell -> cell.y == 0)
				.map(cell -> new FootprintCell(cell.x, cell.z)).collect(Collectors.toSet());
	}

	private static Set<Cell> solidCells(TieredMoundBlueprint blueprint) {
		Set<Cell> cells = new HashSet<>();
		for (int y = 0; y <= blueprint.maxY(); y++) {
			for (int x = blueprint.minX(); x <= blueprint.maxX(); x++) {
				for (int z = blueprint.minZ(); z <= blueprint.maxZ(); z++) {
					if (blueprint.isSolid(x, y, z)) {
						cells.add(new Cell(x, y, z));
					}
				}
			}
		}
		return cells;
	}

	private static Set<Cell> connectedCells(Set<Cell> solid) {
		ArrayDeque<Cell> queue = new ArrayDeque<>();
		Set<Cell> visited = new HashSet<>();
		Cell start = solid.iterator().next();
		queue.add(start);
		visited.add(start);
		int[][] directions = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		while (!queue.isEmpty()) {
			Cell cell = queue.removeFirst();
			for (int[] direction : directions) {
				Cell next = new Cell(cell.x + direction[0], cell.y + direction[1], cell.z + direction[2]);
				if (solid.contains(next) && visited.add(next)) {
					queue.addLast(next);
				}
			}
		}
		return visited;
	}

	private record Cell(int x, int y, int z) {
	}

	private record FootprintCell(int x, int z) {
	}
}
