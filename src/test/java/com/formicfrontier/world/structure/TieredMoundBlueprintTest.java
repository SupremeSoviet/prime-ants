package com.formicfrontier.world.structure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

final class TieredMoundBlueprintTest {
	@Test
	void queenStageOneIsCompactTallConnectedAndMultiLevel() {
		TieredMoundBlueprint blueprint = TieredMoundBlueprint.load(TieredMoundPlacer.QUEEN_STAGE_ONE_RESOURCE);
		Set<Cell> solid = solidCells(blueprint);
		Assertions.assertFalse(solid.isEmpty());

		int minX = solid.stream().mapToInt(Cell::x).min().orElseThrow();
		int maxX = solid.stream().mapToInt(Cell::x).max().orElseThrow();
		int minZ = solid.stream().mapToInt(Cell::z).min().orElseThrow();
		int maxZ = solid.stream().mapToInt(Cell::z).max().orElseThrow();
		int height = solid.stream().mapToInt(Cell::y).max().orElseThrow() + 1;
		int width = maxX - minX + 1;
		int depth = maxZ - minZ + 1;

		Assertions.assertEquals(24, height, "stage one should already read as a tall landmark");
		Assertions.assertTrue(width <= 21, "the main mound should stay compact in X, got " + width);
		Assertions.assertTrue(depth <= 18, "the main mound should stay compact in Z, got " + depth);
		Assertions.assertTrue(height > width, "the silhouette should be taller than its widest occupied layer");
		Assertions.assertEquals(4, blueprint.tiers().size(), "four offset tiers make the growth floors explicit");
		Assertions.assertEquals(2, blueprint.terraces().size(), "upper entrances need attached earth shelves");
		Assertions.assertEquals(3, blueprint.chambers().size(), "each facade floor should lead to an interior room");
		Assertions.assertEquals(2, blueprint.connections().size(), "all three floors need internal stair passages");
		Assertions.assertEquals(Set.of("queen_hall", "storage", "lookout"), blueprint.chambers().stream()
				.map(TieredMoundBlueprint.Chamber::purpose).collect(java.util.stream.Collectors.toSet()),
				"each floor should have a distinct furnishing purpose");
		Assertions.assertEquals(3, blueprint.mouths().size(), "three floor mouths should be readable on the facade");
		Assertions.assertEquals(Set.of(1, 8, 14), blueprint.mouths().stream().map(TieredMoundBlueprint.Mouth::y).collect(java.util.stream.Collectors.toSet()));
		Assertions.assertTrue(blueprint.isSolid(-2, 7, -6), "middle-floor terrace should project from the facade");
		Assertions.assertTrue(blueprint.isSolid(2, 13, -5), "upper-floor terrace should project from the facade");
		Assertions.assertFalse(blueprint.isSolid(0, 2, 2), "queen hall must be carved behind the ground entrance");
		Assertions.assertFalse(blueprint.isSolid(-2, 9, 1), "middle storage room must be carved behind its entrance");
		Assertions.assertFalse(blueprint.isSolid(2, 15, 1), "upper nook must be carved behind its entrance");
		Assertions.assertEquals(solid.size(), connectedCells(solid), "the compiler input must remain one connected mound");
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

	private static int connectedCells(Set<Cell> solid) {
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
		return visited.size();
	}

	private record Cell(int x, int y, int z) {
	}
}
