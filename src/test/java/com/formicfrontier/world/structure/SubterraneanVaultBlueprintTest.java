package com.formicfrontier.world.structure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class SubterraneanVaultBlueprintTest {
	@Test
	void queenVaultIsAConnectedTwoDepthProtectedInterior() {
		SubterraneanVaultBlueprint blueprint = SubterraneanVaultBlueprint.load(
				SubterraneanVaultPlacer.QUEEN_VAULT_RESOURCE);
		SubterraneanVaultBlueprint.Chamber guard = blueprint.chamber("guard_vestibule");
		SubterraneanVaultBlueprint.Chamber treasury = blueprint.chamber("royal_treasury");
		SubterraneanVaultBlueprint.Chamber sanctum = blueprint.chamber("brood_sanctum");

		Assertions.assertEquals("queen_vault", blueprint.name());
		Assertions.assertEquals("queen_vault", blueprint.palette());
		Assertions.assertEquals(-12, blueprint.minY());
		Assertions.assertEquals(0, blueprint.maxY());
		Assertions.assertEquals(3, blueprint.chambers().size());
		Assertions.assertEquals(Set.of("vault_guard", "vault_treasury", "vault_sanctum"),
				blueprint.chambers().stream().map(SubterraneanVaultBlueprint.Chamber::purpose)
						.collect(Collectors.toSet()));
		Assertions.assertEquals(-6, guard.floorY());
		Assertions.assertEquals(-12, treasury.floorY());
		Assertions.assertEquals(-12, sanctum.floorY());

		SubterraneanVaultBlueprint.SurfaceAccess access = blueprint.surfaceAccess();
		Assertions.assertEquals(6, access.drop(guard));
		Assertions.assertEquals(2, access.landingX(guard));
		Assertions.assertEquals(3, access.landingZ(guard));
		SubterraneanVaultBlueprint.Descent lower = blueprint.connections().getFirst();
		Assertions.assertEquals(6, lower.drop(guard, treasury));
		Assertions.assertEquals(-2, lower.landingX(guard, treasury));
		Assertions.assertEquals(3, lower.landingZ(guard, treasury));

		Assertions.assertFalse(blueprint.isSolid(guard.x(), guard.floorY() + 2, guard.z()),
				"guard vestibule must be genuinely carved");
		Assertions.assertFalse(blueprint.isSolid(treasury.x(), treasury.floorY() + 2, treasury.z()),
				"treasury must be genuinely carved");
		Assertions.assertFalse(blueprint.isSolid(sanctum.x(), sanctum.floorY() + 2, sanctum.z()),
				"sanctum must be genuinely carved");
		Assertions.assertTrue(overlapsAtHeadHeight(treasury, sanctum),
				"the two lower chambers need a walkable opening instead of isolated bubbles");

		assertDescentHeadroom(blueprint, access.startX(), access.startZ(), access.surfaceFloorY(),
				access.drop(guard), access.dx(), access.dz());
		assertDescentHeadroom(blueprint, lower.startX(), lower.startZ(), guard.floorY(),
				lower.drop(guard, treasury), lower.dx(), lower.dz());
		Assertions.assertFalse(blueprint.isSolid(0, 1, 0), "the vault compiler must never add mass above the mound floor");

		Set<Cell> solid = solidCells(blueprint);
		Set<Cell> connected = connectedCells(solid);
		Set<Cell> disconnected = new HashSet<>(solid);
		disconnected.removeAll(connected);
		Assertions.assertEquals(solid.size(), connected.size(),
				"all chamber shells and stair envelopes must form one protected structure; disconnected=" + disconnected);
	}

	private static void assertDescentHeadroom(SubterraneanVaultBlueprint blueprint, int startX, int startZ,
			int startY, int drop, int dx, int dz) {
		for (int step = 0; step < drop; step++) {
			int x = startX + dx * step;
			int y = startY - step;
			int z = startZ + dz * step;
			Assertions.assertFalse(blueprint.isSolid(x, y + 1, z),
					"first headroom block must be clear at descent step " + step);
			Assertions.assertFalse(blueprint.isSolid(x, y + 2, z),
					"second headroom block must be clear at descent step " + step);
		}
	}

	private static boolean overlapsAtHeadHeight(SubterraneanVaultBlueprint.Chamber first,
			SubterraneanVaultBlueprint.Chamber second) {
		int y = first.floorY() + 1;
		for (int x = -10; x <= 10; x++) {
			for (int z = -6; z <= 10; z++) {
				if (first.carves(x, y, z) && second.carves(x, y, z)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Set<Cell> solidCells(SubterraneanVaultBlueprint blueprint) {
		Set<Cell> cells = new HashSet<>();
		for (int y = blueprint.minY(); y <= blueprint.maxY(); y++) {
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
		Assertions.assertFalse(solid.isEmpty());
		ArrayDeque<Cell> queue = new ArrayDeque<>();
		Set<Cell> visited = new HashSet<>();
		Cell start = solid.iterator().next();
		queue.add(start);
		visited.add(start);
		int[][] directions = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		while (!queue.isEmpty()) {
			Cell cell = queue.removeFirst();
			for (int[] direction : directions) {
				Cell next = new Cell(cell.x() + direction[0], cell.y() + direction[1], cell.z() + direction[2]);
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
