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

	@Test
	void queenStageTwoGrowsUpwardAndAddsConnectedAsymmetricAnnexes() {
		TieredMoundBlueprint stageOne = TieredMoundBlueprint.load(TieredMoundPlacer.QUEEN_STAGE_ONE_RESOURCE);
		TieredMoundBlueprint stageTwo = TieredMoundBlueprint.load(TieredMoundPlacer.QUEEN_STAGE_TWO_RESOURCE);
		Set<Cell> stageOneSolid = solidCells(stageOne);
		Set<Cell> stageTwoSolid = solidCells(stageTwo);

		int minX = stageTwoSolid.stream().mapToInt(Cell::x).min().orElseThrow();
		int maxX = stageTwoSolid.stream().mapToInt(Cell::x).max().orElseThrow();
		int width = maxX - minX + 1;
		int height = stageTwoSolid.stream().mapToInt(Cell::y).max().orElseThrow() + 1;
		long stageOneBase = stageOneSolid.stream().filter(cell -> cell.y() == 0).count();
		long stageTwoBase = stageTwoSolid.stream().filter(cell -> cell.y() == 0).count();

		Assertions.assertEquals("queen_mound_stage_2", stageTwo.name());
		Assertions.assertEquals("great_mound", stageTwo.palette());
		Assertions.assertEquals(33, height, "Great Mound should add a clearly readable fourth occupied storey");
		Assertions.assertTrue(height > width,
				"the evolved centre must remain a steep landmark instead of becoming a broad dome, got " + width + "x" + height);
		Assertions.assertTrue(stageTwoSolid.size() > stageOneSolid.size() * 1.65,
				"stage two needs materially more authored mass than stage one");
		Assertions.assertTrue(stageTwoBase > stageOneBase * 1.35,
				"the two annex hillocks should visibly expand the occupied ground floor");
		Assertions.assertEquals(7, stageTwo.tiers().size(), "five central growth tiers plus two annex lobes are expected");
		Assertions.assertEquals(3, stageTwo.terraces().size(), "each elevated facade floor needs an attached earth shelf");
		Assertions.assertEquals(6, stageTwo.chambers().size(), "three inherited rooms, two annexes and a crown room are expected");
		Assertions.assertEquals(3, stageTwo.connections().size(), "four central floors require three internal stair passages");
		Assertions.assertEquals(6, stageTwo.mouths().size(), "central floors and both annex rooms need readable facade mouths");
		Assertions.assertEquals(Set.of("queen_hall", "great_larder", "great_workshop", "storage", "lookout", "great_crown"),
				stageTwo.chambers().stream().map(TieredMoundBlueprint.Chamber::purpose)
						.collect(java.util.stream.Collectors.toSet()),
				"every inherited and added room needs a distinct lived-in role");

		TieredMoundBlueprint.Chamber queenHall = chamber(stageTwo, "queen_hall");
		TieredMoundBlueprint.Chamber larder = chamber(stageTwo, "left_larder");
		TieredMoundBlueprint.Chamber workshop = chamber(stageTwo, "right_workshop");
		TieredMoundBlueprint.Chamber crown = chamber(stageTwo, "crown_chamber");
		Assertions.assertTrue(overlapsAtHeadHeight(queenHall, larder),
				"the left annex interior must open into the queen hall instead of becoming an isolated room");
		Assertions.assertTrue(overlapsAtHeadHeight(queenHall, workshop),
				"the right annex interior must open into the queen hall instead of becoming an isolated room");
		Assertions.assertTrue(stageTwo.isSolid(-11, 1, 4), "left annex needs visible earth mass beyond stage one");
		Assertions.assertTrue(stageTwo.isSolid(10, 1, 5), "right annex needs visible earth mass beyond stage one");
		Assertions.assertFalse(stageTwo.isSolid(-7, 2, 4), "the left larder must be carved inside its hillock");
		Assertions.assertFalse(stageTwo.isSolid(6, 2, 5), "the right workshop must be carved inside its hillock");
		Assertions.assertFalse(stageTwo.isSolid(-1, 22, 1), "the new crown floor must contain a real room");
		Assertions.assertTrue(stageTwo.isSolid(0, 32, 1), "the stage-two crown must reach its declared peak");
		Assertions.assertFalse(stageTwo.contains(0, 33, 1), "the stage-two crown must stop above the authored 33-block height");
		Assertions.assertEquals("crown_chamber", stageTwo.connections().getLast().to(),
				"the final internal stair must land in the new crown chamber");
		Assertions.assertEquals(20, crown.floorY());
		Set<Cell> connectedStageTwo = connectedCellSet(stageTwoSolid);
		Set<Cell> disconnectedStageTwo = new HashSet<>(stageTwoSolid);
		disconnectedStageTwo.removeAll(connectedStageTwo);
		Assertions.assertEquals(stageTwoSolid.size(), connectedStageTwo.size(),
				"central mass and both asymmetric annex hillocks must compile as one connected mound; disconnected="
						+ disconnectedStageTwo);
	}

	private static TieredMoundBlueprint.Chamber chamber(TieredMoundBlueprint blueprint, String id) {
		return blueprint.chambers().stream().filter(chamber -> chamber.id().equals(id)).findFirst().orElseThrow();
	}

	private static boolean overlapsAtHeadHeight(TieredMoundBlueprint.Chamber first,
			TieredMoundBlueprint.Chamber second) {
		int y = first.floorY() + 1;
		for (int x = -16; x <= 16; x++) {
			for (int z = -8; z <= 12; z++) {
				if (first.carves(x, y, z) && second.carves(x, y, z)) {
					return true;
				}
			}
		}
		return false;
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
		return connectedCellSet(solid).size();
	}

	private static Set<Cell> connectedCellSet(Set<Cell> solid) {
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
}
