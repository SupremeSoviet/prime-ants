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
	void pheromoneArchivesHaveTwoDistinctFloorsJoinedByAnInternalStair() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.PHEROMONE_ARCHIVE);
		Assertions.assertEquals(Set.of("pheromone_archive_a", "pheromone_archive_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		Set<String> stairDirections = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("pheromone_archive", blueprint.palette());
			Assertions.assertEquals(10, blueprint.maxY(), "archive should rise above single-storey economy mounds");
			Assertions.assertEquals(2, blueprint.chambers().size());
			TieredMoundBlueprint.Chamber hall = blueprint.chambers().stream()
					.filter(chamber -> chamber.id().equals("reading_hall")).findFirst().orElseThrow();
			TieredMoundBlueprint.Chamber loft = blueprint.chambers().stream()
					.filter(chamber -> chamber.id().equals("catalog_loft")).findFirst().orElseThrow();
			Assertions.assertEquals("archive_hall", hall.purpose());
			Assertions.assertEquals("archive_loft", loft.purpose());
			Assertions.assertEquals(0, hall.floorY());
			Assertions.assertEquals(5, loft.floorY());
			Assertions.assertFalse(hall.openToSky());
			Assertions.assertFalse(loft.openToSky());
			Assertions.assertEquals(2, blueprint.mouths().size(), "both archive floors need a readable facade opening");
			Assertions.assertEquals(1, blueprint.connections().size(), "archive floors need one authored stair passage");
			TieredMoundBlueprint.Connection stair = blueprint.connections().getFirst();
			Assertions.assertEquals(hall.id(), stair.from());
			Assertions.assertEquals(loft.id(), stair.to());
			Assertions.assertEquals(1, stair.width());
			stairDirections.add(stair.direction());
			int rise = loft.floorY() - hall.floorY();
			for (int step = 0; step < rise; step++) {
				int x = stair.startX() + stair.dx() * step;
				int z = stair.startZ() + stair.dz() * step;
				Assertions.assertFalse(blueprint.isSolid(x, hall.floorY() + step + 1, z),
						"archive stair must carve headroom at step " + step);
			}
			assertRearShell(blueprint, hall);
			assertRearShell(blueprint, loft);

			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "two-storey archive mound");
			Set<FootprintCell> footprint = footprint(solid);
			Assertions.assertTrue(footprint.size() >= 170, "archive needs a substantial grounded reading hall");
			long upperMass = solid.stream().filter(cell -> cell.y == loft.floorY()).count();
			Assertions.assertTrue(upperMass >= 45, "archive catalog floor needs visible upper-storey mass");
			footprints.add(footprint);
		}
		Assertions.assertEquals(Set.of("east", "west"), stairDirections,
				"archive variants should mirror neither silhouette nor stair circulation");
		Assertions.assertEquals(2, footprints.size(), "archive variants need distinct asymmetrical footprints");
	}

	@Test
	void armoriesAreHeavyFortifiedMoundsWithConnectedForgeAndWeaponVault() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.ARMORY);
		Assertions.assertEquals(Set.of("armory_a", "armory_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("armory", blueprint.palette());
			Assertions.assertEquals(8, blueprint.maxY(), "armory should stay lower than the archive but heavier than economy domes");
			Assertions.assertEquals(2, blueprint.chambers().size());
			TieredMoundBlueprint.Chamber forge = blueprint.chambers().stream()
					.filter(chamber -> chamber.id().equals("forge_hall")).findFirst().orElseThrow();
			TieredMoundBlueprint.Chamber vault = blueprint.chambers().stream()
					.filter(chamber -> chamber.id().equals("weapon_vault")).findFirst().orElseThrow();
			Assertions.assertEquals("armory_forge", forge.purpose());
			Assertions.assertEquals("armory_vault", vault.purpose());
			Assertions.assertEquals(0, forge.floorY());
			Assertions.assertEquals(0, vault.floorY());
			Assertions.assertTrue(blueprint.connections().isEmpty(), "same-floor armory rooms should connect through their overlap");
			Assertions.assertEquals(3, blueprint.mouths().getFirst().width(), "armory needs a narrow defensible entrance");

			boolean roomsOverlap = false;
			for (int x = blueprint.minX(); x <= blueprint.maxX() && !roomsOverlap; x++) {
				for (int z = blueprint.minZ(); z <= blueprint.maxZ(); z++) {
					if (forge.carves(x, 1, z) && vault.carves(x, 1, z)) {
						roomsOverlap = true;
						break;
					}
				}
			}
			Assertions.assertTrue(roomsOverlap, "forge hall and weapon vault need a walkable shared passage");
			assertRearShell(blueprint, forge);
			assertRearShell(blueprint, vault);
			assertEnclosedExceptMouths(blueprint, forge);
			assertEnclosedExceptMouths(blueprint, vault);

			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "armory mound");
			Set<FootprintCell> footprint = footprint(solid);
			int width = footprint.stream().mapToInt(FootprintCell::x).max().orElseThrow()
					- footprint.stream().mapToInt(FootprintCell::x).min().orElseThrow() + 1;
			int depth = footprint.stream().mapToInt(FootprintCell::z).max().orElseThrow()
					- footprint.stream().mapToInt(FootprintCell::z).min().orElseThrow() + 1;
			Assertions.assertTrue(footprint.size() >= 220, "armory needs a massive grounded shell");
			Assertions.assertTrue(width >= 19 && depth >= 17 && Math.abs(width - depth) <= 5,
					"armory should read as a broad armored dome, got " + width + "x" + depth);
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "armory variants need distinct asymmetrical shells");
	}

	@Test
	void diplomacyShrinesAreOpenThreeHornSanctumsWithDistinctSilhouettes() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.DIPLOMACY_SHRINE);
		Assertions.assertEquals(Set.of("diplomacy_shrine_a", "diplomacy_shrine_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("diplomacy_shrine", blueprint.palette());
			Assertions.assertEquals(7, blueprint.maxY());
			Assertions.assertEquals(1, blueprint.chambers().size());
			TieredMoundBlueprint.Chamber sanctum = blueprint.chambers().getFirst();
			Assertions.assertEquals("diplomacy_shrine", sanctum.purpose());
			Assertions.assertTrue(sanctum.openToSky(), "diplomacy sanctum should remain open under the horn crown");
			Assertions.assertEquals(blueprint.maxY(), sanctum.topY());
			Assertions.assertTrue(blueprint.pits().isEmpty());
			Assertions.assertTrue(blueprint.connections().isEmpty());
			Assertions.assertEquals(3, blueprint.mouths().getFirst().width(), "sanctum needs an intimate ceremonial entrance");
			for (int y = 1; y <= blueprint.maxY(); y++) {
				Assertions.assertFalse(blueprint.isSolid(sanctum.x(), y, sanctum.z()),
						"open sanctum must reach the sky at y=" + y);
			}

			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "diplomacy shrine ring");
			Assertions.assertTrue(solid.stream().anyMatch(cell -> cell.y >= 6 && cell.x <= -5),
					"shrine needs a tall left ritual horn");
			Assertions.assertTrue(solid.stream().anyMatch(cell -> cell.y >= 6 && cell.x >= 5),
					"shrine needs a tall right ritual horn");
			Assertions.assertTrue(solid.stream().anyMatch(cell -> cell.y >= 6 && cell.z >= 6),
					"shrine needs a tall rear ritual horn");
			long crownMass = solid.stream().filter(cell -> cell.y >= 6).count();
			Assertions.assertTrue(crownMass >= 12, "open sanctum still needs a readable three-part crown");
			Set<FootprintCell> footprint = footprint(solid);
			Assertions.assertTrue(footprint.size() >= 190, "shrine needs a grounded ceremonial ring");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "shrine variants need mirrored but non-identical rings");
	}

	@Test
	void resinDepotsAreLowTwinPodCisternsWithConnectedWorkshopAndVault() {
		var variants = OrganicBuildingPlacer.variants(BuildingType.RESIN_DEPOT);
		Assertions.assertEquals(Set.of("resin_depot_a", "resin_depot_b"), variants.stream()
				.map(TieredMoundBlueprint::name).collect(Collectors.toSet()));
		Set<Set<FootprintCell>> footprints = new HashSet<>();
		for (TieredMoundBlueprint blueprint : variants) {
			Assertions.assertEquals("resin_depot", blueprint.palette());
			Assertions.assertEquals(7, blueprint.maxY(), "resin depot should stay low and tank-like");
			Assertions.assertEquals(2, blueprint.chambers().size());
			TieredMoundBlueprint.Chamber workshop = blueprint.chambers().stream()
					.filter(chamber -> chamber.id().equals("workshop")).findFirst().orElseThrow();
			TieredMoundBlueprint.Chamber vault = blueprint.chambers().stream()
					.filter(chamber -> chamber.id().equals("sealed_vault")).findFirst().orElseThrow();
			Assertions.assertEquals("resin_workshop", workshop.purpose());
			Assertions.assertEquals("resin_vault", vault.purpose());
			Assertions.assertEquals(0, workshop.floorY());
			Assertions.assertEquals(0, vault.floorY());
			Assertions.assertFalse(workshop.openToSky());
			Assertions.assertFalse(vault.openToSky());
			Assertions.assertTrue(blueprint.pits().isEmpty());
			Assertions.assertTrue(blueprint.connections().isEmpty(), "same-floor resin rooms should meet directly");
			Assertions.assertEquals(3, blueprint.mouths().getFirst().width(), "resin stores need a controlled narrow mouth");

			boolean roomsOverlap = false;
			for (int x = blueprint.minX(); x <= blueprint.maxX() && !roomsOverlap; x++) {
				for (int z = blueprint.minZ(); z <= blueprint.maxZ(); z++) {
					if (workshop.carves(x, 1, z) && vault.carves(x, 1, z)) {
						roomsOverlap = true;
						break;
					}
				}
			}
			Assertions.assertTrue(roomsOverlap, "workshop and resin vault need a walkable shared throat");
			assertRearShell(blueprint, workshop);
			assertRearShell(blueprint, vault);
			assertEnclosedExceptMouths(blueprint, workshop);
			assertEnclosedExceptMouths(blueprint, vault);

			Set<Cell> solid = solidCells(blueprint);
			assertConnected(blueprint, solid, "resin cistern mound");
			Set<FootprintCell> footprint = footprint(solid);
			Assertions.assertTrue(footprint.size() >= 200, "resin depot needs two substantial grounded storage pods");
			Assertions.assertTrue(footprint.stream().anyMatch(cell -> cell.x <= -9), "resin depot needs a left cistern lobe");
			Assertions.assertTrue(footprint.stream().anyMatch(cell -> cell.x >= 9), "resin depot needs a right cistern lobe");
			long capMass = solid.stream().filter(cell -> cell.y >= 6).count();
			Assertions.assertTrue(capMass >= 18, "resin depot needs a readable sealed crown");
			footprints.add(footprint);
		}
		Assertions.assertEquals(2, footprints.size(), "resin depot variants need distinct asymmetric tanks");
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

		Set<String> archiveVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.PHEROMONE_ARCHIVE, existing);
			archiveVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.PHEROMONE_ARCHIVE, site).name());
		}
		Assertions.assertEquals(Set.of("pheromone_archive_a", "pheromone_archive_b"), archiveVariants);

		Set<String> armoryVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.ARMORY, existing);
			armoryVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.ARMORY, site).name());
		}
		Assertions.assertEquals(Set.of("armory_a", "armory_b"), armoryVariants);
		BlockPos firstArmory = ColonyBuilder.siteFor(origin, BuildingType.ARMORY, 0);
		BlockPos secondArmory = ColonyBuilder.siteFor(origin, BuildingType.ARMORY, 1);
		Assertions.assertTrue(firstArmory.distSqr(secondArmory) >= 34 * 34,
				"repeated armories need open ground around their heavy shells");

		Set<String> shrineVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.DIPLOMACY_SHRINE, existing);
			shrineVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.DIPLOMACY_SHRINE, site).name());
		}
		Assertions.assertEquals(Set.of("diplomacy_shrine_a", "diplomacy_shrine_b"), shrineVariants);
		BlockPos firstShrine = ColonyBuilder.siteFor(origin, BuildingType.DIPLOMACY_SHRINE, 0);
		BlockPos secondShrine = ColonyBuilder.siteFor(origin, BuildingType.DIPLOMACY_SHRINE, 1);
		Assertions.assertTrue(firstShrine.distSqr(secondShrine) >= 42 * 42,
				"repeated shrines need quiet open ground around their ritual crowns");

		Set<String> resinVariants = new HashSet<>();
		for (int existing = 0; existing < 2; existing++) {
			BlockPos site = ColonyBuilder.siteFor(origin, BuildingType.RESIN_DEPOT, existing);
			resinVariants.add(OrganicBuildingPlacer.blueprintFor(BuildingType.RESIN_DEPOT, site).name());
		}
		Assertions.assertEquals(Set.of("resin_depot_a", "resin_depot_b"), resinVariants);
		BlockPos firstResinDepot = ColonyBuilder.siteFor(origin, BuildingType.RESIN_DEPOT, 0);
		BlockPos secondResinDepot = ColonyBuilder.siteFor(origin, BuildingType.RESIN_DEPOT, 1);
		Assertions.assertTrue(firstResinDepot.distSqr(secondResinDepot) >= 42 * 42,
				"repeated resin depots need open working ground around their storage pods");
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

	private static void assertEnclosedExceptMouths(TieredMoundBlueprint blueprint,
			TieredMoundBlueprint.Chamber chamber) {
		Set<Cell> exposed = new HashSet<>();
		int minX = (int) Math.floor(chamber.x() - chamber.radiusX()) - 1;
		int maxX = (int) Math.ceil(chamber.x() + chamber.radiusX()) + 1;
		int minZ = (int) Math.floor(chamber.z() - chamber.radiusZ()) - 1;
		int maxZ = (int) Math.ceil(chamber.z() + chamber.radiusZ()) + 1;
		int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int y = chamber.floorY() + 1; y <= chamber.topY(); y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!chamber.carves(x, y, z)) {
						continue;
					}
					if (!blueprint.contains(x, y, z) && !isAuthoredMouth(blueprint, x, y, z)) {
						exposed.add(new Cell(x, y, z));
						continue;
					}
					for (int[] neighbor : neighbors) {
						int nx = x + neighbor[0];
						int ny = y + neighbor[1];
						int nz = z + neighbor[2];
						if (!blueprint.contains(nx, ny, nz)
								&& !isAuthoredMouth(blueprint, x, y, z)
								&& !isAuthoredMouth(blueprint, nx, ny, nz)) {
							exposed.add(new Cell(x, y, z));
							break;
						}
					}
				}
			}
		}
		Assertions.assertTrue(exposed.isEmpty(), blueprint.name() + " chamber " + chamber.id()
				+ " must be enclosed except at authored mouths; exposed cells: "
				+ exposed.stream().limit(24).toList());
	}

	private static boolean isAuthoredMouth(TieredMoundBlueprint blueprint, int x, int y, int z) {
		return blueprint.mouths().stream().anyMatch(mouth -> mouth.carves(x, y, z));
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
