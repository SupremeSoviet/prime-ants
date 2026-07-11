package com.formicfrontier.world.structure;

import com.formicfrontier.sim.BuildingType;
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
