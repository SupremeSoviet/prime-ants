package com.formicfrontier.test;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.AntWorkState;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyContract;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ColonyRank;
import com.formicfrontier.sim.ColonyTradeCatalog;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.DiplomacyAction;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.qa.VisualQaScenes;
import com.formicfrontier.world.ColonyBuilder;
import com.formicfrontier.world.ColonyDiscoveryService;
import com.formicfrontier.world.ColonyRecurringEvents;
import com.formicfrontier.world.ColonySavedState;
import com.formicfrontier.world.ColonyService;
import com.formicfrontier.world.DiplomacyConsequences;
import com.formicfrontier.world.RaidPlanner;
import com.formicfrontier.world.StructurePlacer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class FormicFrontierGameTest {
	@GameTest

	public void createColonyPlacesCoreChambersAndResources(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		assertTieredMoundProfile(helper, origin, ModBlocks.NEST_MOUND, "queen chamber");
		assertFoodStoreProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0), "food store");
		assertNurseryProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0), "nursery");
		assertMineProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.MINE, 0), "mine");
		assertBarracksProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0), "barracks");
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, origin.offset(54, 0, 8));
		helper.assertBlockPresent(ModBlocks.ORE_NODE, origin.offset(8, 0, 54));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, origin.offset(-54, 0, 8));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, -10));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, -9));
		if (colony.casteCount(AntCaste.GIANT) != 0) {
			helper.fail("Starter colony should not begin with a giant.");
		}
		helper.succeed();
	}

	@GameTest
	public void starterColonyHasEconomyButNoFreeGiant(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		if (colony.resource(ResourceType.FOOD) <= 0 || colony.resource(ResourceType.ORE) <= 0 || colony.resource(ResourceType.CHITIN) <= 0) {
			helper.fail("Starter colony should seed food, ore, and chitin.");
		}
		if (AntCaste.GIANT.canGrowFrom(colony)) {
			helper.fail("Starter colony should not afford a giant immediately.");
		}
		helper.succeed();
	}

	@GameTest
	public void colonyCreationAnchorsHighRequestsToGround(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.above(8)));

		if (!colony.origin().equals(helper.absolutePos(origin))) {
			helper.fail("Colony origin should snap from an air request down to the surface.");
		}
		helper.assertBlockPresent(ModBlocks.NEST_MOUND, origin);
		if (helper.getLevel().getBlockState(helper.absolutePos(origin).below()).isAir()) {
			helper.fail("Starter colony floor should have solid support below it.");
		}
		helper.succeed();
	}

	@GameTest

	public void starterQueenChamberUsesTallTieredBlueprint(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		assertTieredMoundProfile(helper, origin, ModBlocks.NEST_MOUND, "queen chamber");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.MOUND_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.MOUND_STORAGE_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.MOUND_LOOKOUT_INTERIOR)) {
			helper.fail("Visual QA should expose a focused scene for every mound floor.");
		}
		helper.succeed();
	}

	@GameTest

	public void culturesShareTheOrganicQueenAndFoodBuildingLanguage(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 150);
		BlockPos amber = origin.offset(-84, 0, -10);
		BlockPos leafcutter = origin.offset(-28, 0, -10);
		BlockPos fire = origin.offset(28, 0, -10);
		BlockPos carpenter = origin.offset(84, 0, -10);
		List<BlockPos> queens = List.of(amber, leafcutter, fire, carpenter);
		List<ColonyCulture> cultures = List.of(ColonyCulture.AMBER, ColonyCulture.LEAFCUTTER, ColonyCulture.FIRE, ColonyCulture.CARPENTER);

		for (int i = 0; i < cultures.size(); i++) {
			BlockPos queen = queens.get(i);
			BlockPos food = queen.offset(0, 0, 28);
			StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(queen), BuildingType.QUEEN_CHAMBER, BuildingVisualStage.COMPLETE, cultures.get(i));
			StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(food), BuildingType.FOOD_STORE, BuildingVisualStage.COMPLETE, cultures.get(i));
			assertTieredMoundProfile(helper, queen, ModBlocks.NEST_MOUND, cultures.get(i).id() + " queen");
			assertFoodStoreProfile(helper, food, cultures.get(i).id() + " food store");
		}

		BlockPos amberSignature = amber.offset(0, 0, 56);
		BlockPos leafcutterSignature = leafcutter.offset(0, 0, 56);
		BlockPos fireSignature = fire.offset(0, 0, 56);
		BlockPos carpenterSignature = carpenter.offset(0, 0, 56);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(amberSignature), BuildingType.DIPLOMACY_SHRINE, BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(leafcutterSignature), BuildingType.FUNGUS_GARDEN, BuildingVisualStage.COMPLETE, ColonyCulture.LEAFCUTTER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(fireSignature), BuildingType.WATCH_POST, BuildingVisualStage.COMPLETE, ColonyCulture.FIRE);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(carpenterSignature), BuildingType.RESIN_DEPOT, BuildingVisualStage.COMPLETE, ColonyCulture.CARPENTER);
		assertDiplomacyShrineProfile(helper, amberSignature, "amber signature");
		assertMinimalBuildingMarker(helper, leafcutterSignature, ModBlocks.FUNGUS_GARDEN, "leafcutter signature");
		assertMinimalBuildingMarker(helper, fireSignature, ModBlocks.WATCH_POST, "fire signature");
		assertMinimalBuildingMarker(helper, carpenterSignature, ModBlocks.RESIN_DEPOT, "carpenter signature");
		helper.succeed();
	}

	@GameTest
	public void cultureStarterQueuesAreAppliedToCreatedColonies(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 150);
		ColonySavedState.get(helper.getLevel().getServer()).clearColonies();

		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(-72, 0, 0)), ColonyCulture.AMBER), BuildingType.DIPLOMACY_SHRINE);
		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(-24, 0, 0)), ColonyCulture.LEAFCUTTER), BuildingType.FUNGUS_GARDEN);
		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(24, 0, 0)), ColonyCulture.FIRE), BuildingType.WATCH_POST);
		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(72, 0, 0)), ColonyCulture.CARPENTER), BuildingType.RESIN_DEPOT);

		helper.succeed();
	}

	@GameTest

	public void starterColonyUsesOrganicEconomyAndBarracksBlueprints(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		assertFoodStoreProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0), "food store");
		assertNurseryProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0), "nursery");
		assertMineProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.MINE, 0), "mine");
		assertBarracksProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0), "barracks");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.FOOD_STORE_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.FOOD_STORE_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.NURSERY_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.NURSERY_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.MINE_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.MINE_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.CHITIN_FARM_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.CHITIN_FARM_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.BARRACKS_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.BARRACKS_INTERIOR)) {
			helper.fail("Visual QA should expose the organic starter families and their inhabited interiors.");
		}
		helper.succeed();
	}

	@GameTest
	public void mineAndChitinFarmCompileToDistinctInhabitedMounds(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		BlockPos mine = origin.offset(-22, 0, 0);
		BlockPos farm = origin.offset(22, 0, 0);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(mine), BuildingType.MINE,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(farm), BuildingType.CHITIN_FARM,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);

		assertMineProfile(helper, mine, "focused mine");
		assertChitinFarmProfile(helper, farm, "focused chitin farm");
		helper.succeed();
	}

	@GameTest
	public void marketCompilesToOpenInhabitedCourtyardVariants(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		BlockPos first = origin.offset(-18, 0, 0);
		BlockPos second = origin.offset(18, 0, 1);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(first), BuildingType.MARKET,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(second), BuildingType.MARKET,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);

		assertMarketProfile(helper, first, "first market variant");
		assertMarketProfile(helper, second, "second market variant");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.MARKET_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.MARKET_COURTYARD)) {
			helper.fail("Visual QA should expose market perimeter and courtyard scenes.");
		}
		helper.succeed();
	}

	@GameTest
	public void pheromoneArchiveCompilesToConnectedTwoFloorVariants(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		BlockPos first = origin.offset(-18, 0, 0);
		BlockPos second = origin.offset(18, 0, 1);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(first), BuildingType.PHEROMONE_ARCHIVE,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(second), BuildingType.PHEROMONE_ARCHIVE,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);

		assertPheromoneArchiveProfile(helper, first, "first archive variant");
		assertPheromoneArchiveProfile(helper, second, "second archive variant");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.ARCHIVE_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.ARCHIVE_HALL_INTERIOR)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.ARCHIVE_LOFT_INTERIOR)) {
			helper.fail("Visual QA should expose both archive floors and its variant silhouettes.");
		}
		helper.succeed();
	}

	@GameTest
	public void armoryCompilesToConnectedForgeAndWeaponVaultVariants(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		BlockPos first = origin.offset(-18, 0, 0);
		BlockPos second = origin.offset(18, 0, 1);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(first), BuildingType.ARMORY,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(second), BuildingType.ARMORY,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);

		assertArmoryProfile(helper, first, "first armory variant");
		assertArmoryProfile(helper, second, "second armory variant");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.ARMORY_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.ARMORY_INTERIOR)) {
			helper.fail("Visual QA should expose armory silhouettes and its connected interior.");
		}
		helper.succeed();
	}

	@GameTest
	public void diplomacyShrineCompilesToOpenFurnishedSanctumVariants(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		BlockPos first = origin.offset(-18, 0, 0);
		BlockPos second = origin.offset(18, 0, 1);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(first), BuildingType.DIPLOMACY_SHRINE,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(second), BuildingType.DIPLOMACY_SHRINE,
				BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);

		assertDiplomacyShrineProfile(helper, first, "first diplomacy shrine variant");
		assertDiplomacyShrineProfile(helper, second, "second diplomacy shrine variant");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.SHRINE_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.SHRINE_SANCTUM)) {
			helper.fail("Visual QA should expose diplomacy shrine crowns and its open sanctum.");
		}
		helper.succeed();
	}

	@GameTest
	public void resinDepotCompilesToConnectedWorkshopAndSealedVaultVariants(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		BlockPos first = origin.offset(-18, 0, 0);
		BlockPos second = origin.offset(18, 0, 1);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(first), BuildingType.RESIN_DEPOT,
				BuildingVisualStage.COMPLETE, ColonyCulture.CARPENTER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(second), BuildingType.RESIN_DEPOT,
				BuildingVisualStage.COMPLETE, ColonyCulture.CARPENTER);

		assertResinDepotProfile(helper, first, "first resin depot variant");
		assertResinDepotProfile(helper, second, "second resin depot variant");
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.RESIN_DEPOT_VARIANTS)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.RESIN_DEPOT_INTERIOR)) {
			helper.fail("Visual QA should expose resin cistern silhouettes and its connected workshop.");
		}
		helper.succeed();
	}

	@GameTest

	public void buildingVisualStagesUseBlueprintsOnlyWhenOperational(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		BlockPos planned = origin.offset(8, 0, 8);
		BlockPos construction = origin.offset(30, 0, 8);
		BlockPos complete = origin.offset(52, 0, 8);
		BlockPos upgraded = origin.offset(8, 0, 38);
		BlockPos damaged = origin.offset(30, 0, 38);
		BlockPos repairing = origin.offset(52, 0, 38);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(planned), BuildingType.MARKET, BuildingVisualStage.PLANNED);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(construction), BuildingType.MARKET, BuildingVisualStage.CONSTRUCTION);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(complete), BuildingType.MARKET, BuildingVisualStage.COMPLETE);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(upgraded), BuildingType.MARKET, BuildingVisualStage.UPGRADED);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(damaged), BuildingType.MARKET, BuildingVisualStage.DAMAGED);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(repairing), BuildingType.MARKET, BuildingVisualStage.REPAIRING);

		assertMinimalBuildingMarker(helper, planned, Blocks.DIRT_PATH, "planned market");
		for (BlockPos center : List.of(construction, damaged, repairing)) {
			assertMinimalBuildingMarker(helper, center, ModBlocks.MARKET_CHAMBER, "transitional market stage");
		}
		assertMarketProfile(helper, complete, "complete market");
		assertMarketProfile(helper, upgraded, "upgraded market");
		helper.succeed();
	}

	@GameTest

	public void constructionStageSceneShowsMaterialDelivery(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		VisualQaScenes.seedConstructionStages(helper.getLevel(), colony);
		List<BlockPos> centers = VisualQaScenes.constructionStageBuildingCenters(origin);
		BlockPos construction = centers.get(1);
		BlockPos damaged = centers.get(4);
		BlockPos repairing = centers.get(5);
		assertMinimalBuildingMarker(helper, centers.get(0), Blocks.DIRT_PATH, "planned QA market");
		for (int i : List.of(1, 4, 5)) {
			assertMinimalBuildingMarker(helper, centers.get(i), ModBlocks.MARKET_CHAMBER, "transitional QA market stage");
		}
		for (int i : List.of(2, 3)) {
			helper.assertBlockPresent(ModBlocks.MARKET_CHAMBER, centers.get(i));
			if (!helper.getLevel().getBlockState(helper.absolutePos(centers.get(i).offset(0, 4, 1))).isAir()) {
				helper.fail("Operational QA market should expose its roofless courtyard.");
			}
		}
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, construction.offset(-2, 0, -6));
		helper.assertBlockPresent(ModBlocks.RESIN_DEPOT, construction.offset(4, 0, -7));
		helper.assertBlockPresent(ModBlocks.RESIN_DEPOT, construction.offset(5, 1, -5));
		helper.assertBlockPresent(Blocks.BARREL, construction.offset(5, 1, -6));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(4, 1, -5));
		helper.assertBlockPresent(Blocks.BARREL, repairing.offset(4, 1, -6));

		BlockPos absoluteConstruction = helper.absolutePos(construction);
		AABB sceneBounds = new AABB(
				absoluteConstruction.getX() - 10, absoluteConstruction.getY() - 2, absoluteConstruction.getZ() - 12,
				absoluteConstruction.getX() + 10, absoluteConstruction.getY() + 8, absoluteConstruction.getZ() + 6
		);
		List<AntEntity> workers = helper.getLevel().getEntitiesOfClass(AntEntity.class, sceneBounds, ant -> ant.colonyId() == colony.id());
		if (workers.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| workers.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_RESIN)) {
			helper.fail("Construction stage should show a working builder and resin carrier.");
		}
		if (helper.getLevel().getEntitiesOfClass(Display.class, sceneBounds).size() < 2) {
			helper.fail("Construction stage should include visible carried-material markers.");
		}
		BlockPos absoluteDamaged = helper.absolutePos(damaged);
		BlockPos absoluteRepairing = helper.absolutePos(repairing);
		AABB lateStageBounds = new AABB(
				absoluteDamaged.getX() - 8, absoluteDamaged.getY() - 2, absoluteDamaged.getZ() - 10,
				absoluteRepairing.getX() + 8, absoluteRepairing.getY() + 8, absoluteRepairing.getZ() + 4
		);
		List<AntEntity> lateStageAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, lateStageBounds, ant -> ant.colonyId() == colony.id());
		if (lateStageAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.PATROLLING)
				|| lateStageAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| lateStageAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_CHITIN)) {
			helper.fail("Construction stage should show damaged inspection plus active chitin repair cues.");
		}
		helper.succeed();
	}

	@GameTest
	public void starterAntsSpawnAboveSupportedFloor(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		AABB area = new AABB(
				helper.absolutePos(origin).getX() - 16, helper.absolutePos(origin).getY() - 2, helper.absolutePos(origin).getZ() - 16,
				helper.absolutePos(origin).getX() + 16, helper.absolutePos(origin).getY() + 8, helper.absolutePos(origin).getZ() + 16
		);
		for (AntEntity ant : helper.getLevel().getEntitiesOfClass(AntEntity.class, area, ant -> ant.colonyId() == colony.id())) {
			if (helper.getLevel().getBlockState(ant.blockPosition().below()).isAir()) {
				helper.fail("Starter ant should stand above a solid floor at " + ant.blockPosition().toShortString() + ".");
			}
		}
		for (BlockPos spawn : List.of(
				origin.offset(0, 1, -11),
				origin.offset(2, 1, -11),
				origin.offset(-2, 1, -11),
				origin.offset(0, 1, -13),
				origin.offset(3, 1, -13),
				origin.offset(-4, 1, -12),
				origin.offset(4, 1, -12)
		)) {
			if (!helper.getLevel().getBlockState(helper.absolutePos(spawn)).isAir()
					|| !helper.getLevel().getBlockState(helper.absolutePos(spawn.above())).isAir()
					|| !helper.getLevel().getBlockState(helper.absolutePos(spawn.above(2))).isAir()) {
				helper.fail("Starter spawn point should keep open headroom at " + spawn.toShortString());
			}
		}
		helper.succeed();
	}

	@GameTest
	public void rivalColoniesUseStageFourCultures(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), false);

		if (rival.progress().culture() == ColonyCulture.AMBER) {
			helper.fail("Rival colony should use a non-amber culture.");
		}
		if (rival.resource(ResourceType.RESIN) <= 0 || rival.resource(ResourceType.FUNGUS) <= 0 || rival.resource(ResourceType.VENOM) <= 0) {
			helper.fail("Stage 4 colonies should seed advanced resources.");
		}
		helper.succeed();
	}

	@GameTest
	public void rivalRaidLeavesVisibleTrailAndDamagedTarget(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 104);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(-46, 0, 0)), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(46, 0, 0)), false);
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 2);

		if (!RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Rival raid should execute once cooldown and relation allow it.");
		}
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.DIPLOMACY_SCENE)) {
			helper.fail("Visual QA should expose a diplomacy_scene.");
		}

		boolean hasTrail = false;
		boolean hasWarningMarker = false;
		for (int x = -24; x <= 24; x++) {
			BlockPos trail = origin.offset(x, 0, 0);
			var ground = helper.getLevel().getBlockState(helper.absolutePos(trail));
			var marker = helper.getLevel().getBlockState(helper.absolutePos(trail.above()));
			hasTrail |= ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.COARSE_DIRT);
			hasWarningMarker |= marker.is(Blocks.RED_TERRACOTTA) || marker.is(Blocks.BLACKSTONE);
		}
		if (!hasTrail || !hasWarningMarker) {
			helper.fail("Rival raid should leave a readable trail with warning markers between colonies.");
		}
		if (allied.progress().buildingsView().stream().noneMatch(ColonyBuilding::damaged)) {
			helper.fail("Rival raid should visibly damage one allied non-queen building.");
		}
		helper.succeed();
	}

	@GameTest
	public void queenVaultAbsorbsRaidQueenDamage(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 104);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(-46, 0, 0)), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(46, 0, 0)), false);
		allied.progress().buildQueue().clear();
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.addCaste(AntCaste.GIANT, -allied.casteCount(AntCaste.GIANT));
		BlockPos vault = ColonyBuilder.siteFor(allied, BuildingType.QUEEN_VAULT);
		allied.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_VAULT, vault));
		StructurePlacer.placeBuilding(helper.getLevel(), vault, BuildingType.QUEEN_VAULT, BuildingVisualStage.COMPLETE, allied.progress().culture());
		int queenHealthBeforeRaid = allied.queenHealth();

		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 20);

		if (!RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Rival raid should execute so the Queen Vault protection can be observed.");
		}
		if (allied.queenHealth() != queenHealthBeforeRaid) {
			helper.fail("Completed Queen Vault should absorb raid queen damage; expected " + queenHealthBeforeRaid + ", got " + allied.queenHealth());
		}
		if (!allied.currentTask().contains("Queen Vault absorbed")) {
			helper.fail("Queen Vault protection should be visible in current task, got " + allied.currentTask());
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("Queen Vault absorbed"))) {
			helper.fail("Queen Vault protection should leave an event-log entry.");
		}
		helper.succeed();
	}

	@GameTest
	public void tributeDiplomacyPlacesVisiblePactMarkers(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 120);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData treaty = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), ColonyCulture.CARPENTER);
		allied.progress().setRelation(treaty.id(), DiplomacyState.ALLY);
		treaty.progress().setRelation(allied.id(), DiplomacyState.ALLY);

		if (!DiplomacyConsequences.apply(helper.getLevel(), allied, treaty, DiplomacyAction.TRIBUTE, DiplomacyState.ALLY)) {
			helper.fail("Tribute ending in alliance should place a visible diplomacy consequence.");
		}
		if (allied.progress().relationTo(treaty.id()) != DiplomacyState.ALLY || treaty.progress().relationTo(allied.id()) != DiplomacyState.ALLY) {
			helper.fail("Tribute pact test setup should expose an allied relation in both colonies.");
		}

		BlockPos sourceBeacon = origin.offset(12, 0, 0);
		BlockPos treatyBeacon = origin.offset(52, 0, 0);
		BlockPos cache = DiplomacyConsequences.pactCacheSite(origin, origin.offset(64, 0, 0));
		BlockPos caravan = origin.offset(32, 0, -18);
		assertBlockInColumn(helper, sourceBeacon, Blocks.HONEYCOMB_BLOCK, 1, 3, "source tribute beacon");
		assertBlockInColumn(helper, sourceBeacon, Blocks.CANDLE, 2, 4, "source tribute candle");
		assertBlockInColumn(helper, treatyBeacon, Blocks.HONEYCOMB_BLOCK, 1, 3, "target tribute beacon");
		assertBlockInColumn(helper, cache, Blocks.HONEYCOMB_BLOCK, 0, 2, "tribute cache base");
		assertBlockInColumn(helper, cache, Blocks.AMETHYST_BLOCK, 1, 3, "tribute cache gem");
		assertBlockInColumn(helper, cache, Blocks.CANDLE, 2, 4, "tribute cache candle");
		assertBlockInColumn(helper, caravan, Blocks.BARREL, 1, 2, "tribute caravan supply barrel");
		helper.assertBlockPresent(Blocks.HAY_BLOCK, caravan.offset(-1, 1, 0));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, caravan.offset(1, 1, 0));

		BlockPos absoluteCaravan = helper.absolutePos(caravan);
		AABB caravanBounds = new AABB(
				absoluteCaravan.getX() - 5, absoluteCaravan.getY(), absoluteCaravan.getZ() - 5,
				absoluteCaravan.getX() + 5, absoluteCaravan.getY() + 5, absoluteCaravan.getZ() + 5
		);
		List<AntEntity> caravanAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, caravanBounds, ant ->
				ant.caste() == AntCaste.WORKER && (ant.colonyId() == allied.id() || ant.colonyId() == treaty.id()));
		if (caravanAnts.stream().noneMatch(ant -> ant.colonyId() == allied.id() && ant.workState() == AntWorkState.CARRYING_RESIN)
				|| caravanAnts.stream().noneMatch(ant -> ant.colonyId() == treaty.id() && ant.workState() == AntWorkState.CARRYING_FUNGUS)) {
			helper.fail("Tribute pact should leave a visible two-colony caravan carrying resin and fungus.");
		}
		helper.succeed();
	}

	@GameTest
	public void alliedMarketTradeCaravanRunsOutsideTributeDiplomacy(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 120);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData tradeAlly = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), ColonyCulture.CARPENTER);
		for (ColonyData colony : List.of(allied, tradeAlly)) {
			BlockPos market = ColonyBuilder.siteFor(colony, BuildingType.MARKET);
			if (!colony.progress().hasCompleted(BuildingType.MARKET)) {
				colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, market));
				StructurePlacer.placeBuilding(helper.getLevel(), market, BuildingType.MARKET, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}
		allied.progress().setRelation(tradeAlly.id(), DiplomacyState.ALLY);
		tradeAlly.progress().setRelation(allied.id(), DiplomacyState.ALLY);
		allied.progress().requests().clear();
		allied.setResource(ResourceType.FOOD, 160);
		allied.setResource(ResourceType.CHITIN, 0);
		tradeAlly.setResource(ResourceType.RESIN, 40);
		int alliedFood = allied.resource(ResourceType.FOOD);
		int alliedResin = allied.resource(ResourceType.RESIN);
		int targetFood = tradeAlly.resource(ResourceType.FOOD);
		int targetResin = tradeAlly.resource(ResourceType.RESIN);
		allied.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Allied market colonies should trigger a standalone recurring trade caravan.");
		}
		if (!allied.currentTask().contains("trade caravan")) {
			helper.fail("Trade caravan should be visible in current task, got " + allied.currentTask());
		}
		if (allied.resource(ResourceType.FOOD) != alliedFood - 8 || allied.resource(ResourceType.RESIN) != alliedResin + 8) {
			helper.fail("Trade caravan should exchange allied food for carpenter resin.");
		}
		if (tradeAlly.resource(ResourceType.FOOD) != targetFood + 8 || tradeAlly.resource(ResourceType.RESIN) != targetResin - 8) {
			helper.fail("Trade caravan should update the partner colony resources too.");
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("trade caravan"))
				|| tradeAlly.progress().eventsView().stream().noneMatch(event -> event.message().contains("trade caravan"))) {
			helper.fail("Trade caravan should leave event-log proof on both colonies.");
		}
		ColonyUiSnapshot tradeSnapshot = ColonyUiSnapshot.from(allied, "Trade", "");
		if (!tradeSnapshot.tradeActivity().contains("8 Food -> 8 Resin")
				|| !tradeSnapshot.tradeActivity().contains("#" + tradeAlly.id())) {
			helper.fail("Trade tab should summarize the latest caravan payoff, got " + tradeSnapshot.tradeActivity());
		}

		BlockPos camp = ColonyRecurringEvents.tradeCaravanCamp(origin, origin.offset(64, 0, 0));
		assertBlockInColumn(helper, camp, Blocks.BARREL, 1, 2, "trade caravan supply barrel");
		helper.assertBlockPresent(Blocks.HAY_BLOCK, camp.offset(-1, 1, 0));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, camp.offset(1, 1, 0));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, camp.offset(0, 1, 1));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(8, 0, 0));

		BlockPos absoluteCamp = helper.absolutePos(camp);
		AABB caravanBounds = new AABB(
				absoluteCamp.getX() - 5, absoluteCamp.getY(), absoluteCamp.getZ() - 5,
				absoluteCamp.getX() + 5, absoluteCamp.getY() + 6, absoluteCamp.getZ() + 5
		);
		List<AntEntity> carriers = helper.getLevel().getEntitiesOfClass(AntEntity.class, caravanBounds, ant ->
				ant.caste() == AntCaste.WORKER
						&& (ant.workState() == AntWorkState.CARRYING_FOOD || ant.workState() == AntWorkState.CARRYING_RESIN));
		if (carriers.stream().noneMatch(ant -> ant.colonyId() == allied.id() && ant.workState() == AntWorkState.CARRYING_FOOD)
				|| carriers.stream().noneMatch(ant -> ant.colonyId() == tradeAlly.id() && ant.workState() == AntWorkState.CARRYING_RESIN)) {
			helper.fail("Trade caravan should show two market carriers with food and resin cargo.");
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Trade caravan should not repeat until the next recurring-event interval.");
		}
		helper.succeed();
	}

	@GameTest
	public void truceDiplomacyCoolsRaidRouteWithVisibleMarkers(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 120);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), false);
		allied.progress().setRelation(rival.id(), DiplomacyState.NEUTRAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.NEUTRAL);
		allied.progress().setRaidCooldown(0);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 4);

		if (!DiplomacyConsequences.apply(helper.getLevel(), allied, rival, DiplomacyAction.TRUCE, DiplomacyState.NEUTRAL)) {
			helper.fail("Truce ending in neutral relation should place a visible diplomacy consequence.");
		}
		if (allied.progress().raidCooldown() < DiplomacyConsequences.TRUCE_COOLDOWN_TICKS
				|| rival.progress().raidCooldown() < DiplomacyConsequences.TRUCE_COOLDOWN_TICKS) {
			helper.fail("Truce should cool down immediate raids for both colonies.");
		}

		BlockPos sourceSeal = origin.offset(12, 0, 0);
		BlockPos rivalSeal = origin.offset(52, 0, 0);
		BlockPos truceCache = DiplomacyConsequences.pactCacheSite(origin, origin.offset(64, 0, 0));
		assertBlockInColumn(helper, sourceSeal, Blocks.CHISELED_TUFF, 1, 3, "source truce seal");
		assertBlockInColumn(helper, rivalSeal, Blocks.CHISELED_TUFF, 1, 3, "target truce seal");
		assertBlockInColumn(helper, truceCache, Blocks.MOSS_BLOCK, 0, 2, "truce cache moss");
		assertBlockInColumn(helper, truceCache, Blocks.CANDLE, 2, 4, "truce cache candle");
		if (RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Neutral truce relation should prevent an immediate raid tick.");
		}
		helper.succeed();
	}

	@GameTest
	public void warPactDiplomacyPlacesVisibleMusterRoute(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 130);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), false);
		allied.progress().setRelation(rival.id(), DiplomacyState.WAR);
		rival.progress().setRelation(allied.id(), DiplomacyState.WAR);
		allied.progress().setRaidCooldown(600);
		rival.progress().setRaidCooldown(600);

		if (!DiplomacyConsequences.apply(helper.getLevel(), allied, rival, DiplomacyAction.WAR_PACT, DiplomacyState.WAR)) {
			helper.fail("War pact ending in war relation should place a visible diplomacy consequence.");
		}
		if (allied.progress().raidCooldown() != 0 || rival.progress().raidCooldown() != 0) {
			helper.fail("War pact should open an immediate raid window for both colonies.");
		}

		BlockPos sourceMuster = origin.offset(12, 0, -14);
		BlockPos rivalMuster = origin.offset(52, 0, -14);
		BlockPos warLine = origin.offset(32, 0, -14);
		assertBlockInColumn(helper, sourceMuster, Blocks.RED_TERRACOTTA, 1, 3, "source war beacon");
		assertBlockInColumn(helper, sourceMuster, Blocks.CANDLE, 3, 5, "source war candle");
		assertBlockInColumn(helper, rivalMuster, Blocks.RED_TERRACOTTA, 1, 3, "target war beacon");
		assertBlockInColumn(helper, warLine, Blocks.BONE_BLOCK, 1, 3, "war pact muster bone");
		assertBlockInColumn(helper, warLine, Blocks.BLACKSTONE, 0, 2, "war pact muster blackstone");
		AABB musterBounds = new AABB(
				helper.absolutePos(sourceMuster).getX() - 5, helper.absolutePos(sourceMuster).getY(), helper.absolutePos(sourceMuster).getZ() - 5,
				helper.absolutePos(sourceMuster).getX() + 5, helper.absolutePos(sourceMuster).getY() + 6, helper.absolutePos(sourceMuster).getZ() + 5
		);
		List<AntEntity> musteredAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, musterBounds, ant -> ant.colonyId() == allied.id() && ant.workState() == AntWorkState.PATROLLING);
		if (musteredAnts.size() < 4) {
			helper.fail("War pact should visibly muster four patrolling source-colony ants, got " + musteredAnts.size());
		}
		helper.succeed();
	}

	@GameTest
	public void alliedDefensivePactSendsVisibleGuardResponse(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 130);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(-46, 0, 0)), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(46, 0, 0)), false);
		ColonyData treaty = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(0, 0, 36)), ColonyCulture.CARPENTER);
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		allied.progress().setRelation(treaty.id(), DiplomacyState.ALLY);
		treaty.progress().setRelation(allied.id(), DiplomacyState.ALLY);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 2);

		if (!RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Rival raid should execute and allow the allied defensive pact to answer.");
		}

		BlockPos rally = origin.offset(-28, 0, 8);
		assertBlockInColumn(helper, rally, Blocks.POLISHED_DEEPSLATE, 1, 3, "defensive pact guard post");
		assertBlockInColumn(helper, rally, Blocks.HONEYCOMB_BLOCK, 2, 4, "defensive pact ally signal");
		assertBlockInColumn(helper, rally, Blocks.CANDLE, 3, 5, "defensive pact candle");
		helper.assertBlockPresent(Blocks.BONE_BLOCK, rally.north());
		helper.assertBlockPresent(Blocks.BONE_BLOCK, rally.south());
		AABB guardBounds = new AABB(
				helper.absolutePos(rally).getX() - 5, helper.absolutePos(rally).getY(), helper.absolutePos(rally).getZ() - 5,
				helper.absolutePos(rally).getX() + 5, helper.absolutePos(rally).getY() + 6, helper.absolutePos(rally).getZ() + 5
		);
		List<AntEntity> guardAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, guardBounds, ant -> ant.colonyId() == treaty.id() && ant.workState() == AntWorkState.PATROLLING);
		if (guardAnts.size() < 3) {
			helper.fail("Defensive pact should place three allied patrolling guards near the raid route, got " + guardAnts.size());
		}
		if (!allied.currentTask().contains("Defensive pact") || !treaty.currentTask().contains("Defensive pact")) {
			helper.fail("Defensive pact response should be surfaced in both allied colony tasks.");
		}
		helper.succeed();
	}

	@GameTest
	public void survivalDiscoveryFindsStableWildEncounterSite(GameTestHelper helper) {
		BlockPos playerPos = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, playerPos, 150);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		long seed = 0x46F06D1CL;

		Optional<BlockPos> first = ColonyDiscoveryService.findEncounterSite(helper.getLevel(), savedState, helper.absolutePos(playerPos), seed);
		Optional<BlockPos> second = ColonyDiscoveryService.findEncounterSite(helper.getLevel(), savedState, helper.absolutePos(playerPos), seed);
		if (first.isEmpty()) {
			helper.fail("Survival discovery should find a nearby flat encounter site.");
		}
		if (!first.equals(second)) {
			helper.fail("Survival discovery should choose the same site for the same seed and player region.");
		}
		double distance = first.get().distSqr(helper.absolutePos(playerPos));
		if (distance < ColonyDiscoveryService.DISCOVERY_DISTANCE_MIN * ColonyDiscoveryService.DISCOVERY_DISTANCE_MIN) {
			helper.fail("Survival discovery should not spawn directly on top of the player.");
		}

		ColonyData colony = ColonyDiscoveryService.spawnEncounter(helper.getLevel(), savedState, helper.absolutePos(playerPos), seed).orElse(null);
		if (colony == null) {
			helper.fail("Survival discovery should spawn the wild colony once a site is found.");
		}
		if (!"wild".equals(colony.progress().faction()) || colony.progress().playerAllied()) {
			helper.fail("Discovered colony should be a non-allied wild colony.");
		}
		if (!helper.getLevel().getBlockState(colony.origin()).is(ModBlocks.NEST_MOUND)) {
			helper.fail("Discovered wild colony should place a visible starter mound.");
		}
		if (!isLandmarkTrail(helper.getLevel().getBlockState(colony.origin().offset(-12, 0, -42)).getBlock())) {
			helper.fail("Discovered wild colony should mark a readable surface trail from the mound.");
		}
		if (!helper.getLevel().getBlockState(colony.origin().offset(0, 0, -58)).is(ModBlocks.FOOD_NODE)) {
			helper.fail("Discovered wild colony should expose a forage patch before the player opens UI.");
		}
		if (!helper.getLevel().getBlockState(colony.origin().offset(54, 1, -16)).is(landmarkMarker(colony.progress().culture()))) {
			helper.fail("Discovered wild colony should place a culture-colored boundary marker.");
		}
		BlockPos ruinedScoutNest = colony.origin().offset(38, 0, 22);
		if (!helper.getLevel().getBlockState(ruinedScoutNest).is(ModBlocks.NEST_MOUND)
				|| !helper.getLevel().getBlockState(ruinedScoutNest.below()).is(ModBlocks.NEST_CORE)
				|| !helper.getLevel().getBlockState(ruinedScoutNest.offset(-2, 1, 0)).is(ModBlocks.CHITIN_NODE)
				|| !helper.getLevel().getBlockState(ruinedScoutNest.offset(2, 1, 0)).is(Blocks.BONE_BLOCK)) {
			helper.fail("Discovered wild colony should place a readable collapsed scout nest landmark.");
		}
		if (!isLandmarkTrail(helper.getLevel().getBlockState(colony.origin().offset(24, 0, 10)).getBlock())) {
			helper.fail("Collapsed scout nest should be connected back to the mound by a surface trail.");
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("Collapsed scout nest"))) {
			helper.fail("Collapsed scout nest landmark should be surfaced in the colony event log.");
		}
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.WORLDGEN_ENCOUNTER)) {
			helper.fail("Visual QA should expose a worldgen_encounter scene.");
		}
		BlockPos qaOrigin = helper.absolutePos(playerPos);
		Vec3 camera = VisualQaScenes.worldgenEncounterCamera(qaOrigin);
		Vec3 target = VisualQaScenes.worldgenEncounterTarget(qaOrigin);
		BlockPos camp = VisualQaScenes.worldgenEncounterCamp(qaOrigin);
		if (Math.abs(camera.x - qaOrigin.getX()) > 22.0 || camera.y > qaOrigin.getY() + 19.0 || camera.z < qaOrigin.getZ() + 36.0 || camera.z > qaOrigin.getZ() + 44.0) {
			helper.fail("Worldgen encounter camera should stay centered and close enough to avoid empty foreground.");
		}
		if (target.z < qaOrigin.getZ() - 24.0 || target.z > qaOrigin.getZ() - 12.0) {
			helper.fail("Worldgen encounter camera target should keep the trail and wild colony in frame.");
		}
		if (camp.getZ() > camera.z - 10.0 || camp.getZ() < qaOrigin.getZ() + 18) {
			helper.fail("Worldgen encounter camp should sit far enough ahead of the camera to stay visible with the trail.");
		}
		BlockPos approachTrailHead = firstApproachTrailBlock(helper, helper.absolutePos(playerPos), colony.origin());
		if (approachTrailHead == null) {
			helper.fail("Discovered wild colony should place a visible approach trail from the player side.");
		}
		if (!helper.getLevel().getBlockState(approachTrailHead.above()).isAir()) {
			helper.fail("Approach trail head should be ground dressing, not an isolated upright marker.");
		}
		if (!hasGroundedTrailHeadDressing(helper, approachTrailHead, colony.origin(), colony.progress().culture())) {
			helper.fail("Approach trail head should be integrated with side dirt/root dressing.");
		}
		helper.succeed();
	}

	@GameTest
	public void colonyOverviewSceneKeepsPreparedGroundBeyondCamera(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.COLONY_OVERVIEW)) {
			helper.fail("Visual QA should expose a colony_overview scene.");
		}
		BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
		int radius = VisualQaScenes.qaRadius(VisualQaScenes.COLONY_OVERVIEW);
		Vec3 camera = VisualQaScenes.colonyOverviewCamera(origin);
		Vec3 target = VisualQaScenes.colonyOverviewTarget(origin);
		int cameraGroundDistance = Math.max(
				Math.abs((int) Math.round(camera.x) - origin.getX()),
				Math.abs((int) Math.round(camera.z) - origin.getZ())
		);
		if (radius - cameraGroundDistance < 40) {
			helper.fail("Colony overview camera needs enough prepared terrain beyond the foreground; radius "
					+ radius + " leaves only " + (radius - cameraGroundDistance) + " blocks.");
		}
		if (target.y < origin.getY() + 10.5 || target.y > origin.getY() + 12.5) {
			helper.fail("Colony overview target should frame the middle of the tall starter mound.");
		}
		for (BlockPos landmark : List.of(
				origin,
				ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0),
				ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0),
				ColonyBuilder.siteFor(origin, BuildingType.MINE, 0),
				ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0),
				origin.offset(54, 0, 8),
				origin.offset(8, 0, 54),
				origin.offset(-54, 0, 8)
		)) {
			int landmarkDistance = Math.max(Math.abs(landmark.getX() - origin.getX()), Math.abs(landmark.getZ() - origin.getZ()));
			if (radius - landmarkDistance < 18) {
				helper.fail("Colony overview prepared area should leave terrain beyond starter landmark "
						+ landmark.toShortString() + ".");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void settlementScaleSceneAndLayoutUseLargeVillageFootprint(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.SETTLEMENT_SCALE)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_RESEARCH_MAP)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_MARKET)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_REQUESTS)) {
			helper.fail("Visual QA should expose the settlement scale and dedicated tablet renovation scenes.");
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		BlockPos food = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0);
		BlockPos secondFood = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 1);
		BlockPos nursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0);
		BlockPos secondNursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 1);
		BlockPos mine = ColonyBuilder.siteFor(origin, BuildingType.MINE, 0);
		BlockPos secondMine = ColonyBuilder.siteFor(origin, BuildingType.MINE, 1);
		BlockPos farm = ColonyBuilder.siteFor(origin, BuildingType.CHITIN_FARM, 0);
		BlockPos secondFarm = ColonyBuilder.siteFor(origin, BuildingType.CHITIN_FARM, 1);
		BlockPos thirdFarm = ColonyBuilder.siteFor(origin, BuildingType.CHITIN_FARM, 2);
		BlockPos barracks = ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0);
		BlockPos secondBarracks = ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 1);
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		BlockPos armory = ColonyBuilder.siteFor(origin, BuildingType.ARMORY, 0);
		BlockPos secondArmory = ColonyBuilder.siteFor(origin, BuildingType.ARMORY, 1);
		BlockPos watch = ColonyBuilder.siteFor(origin, BuildingType.WATCH_POST, 0);
		if (Math.abs(food.getX() - origin.getX()) < 36 || Math.abs(nursery.getX() - origin.getX()) < 36) {
			helper.fail("Starter side chambers should move out to the large village ring.");
		}
		if (horizontalDistanceSquared(food, secondFood) < 28 * 28) {
			helper.fail("Repeated food stores need enough open ground for distinct mound silhouettes.");
		}
		if (horizontalDistanceSquared(nursery, secondNursery) < 28 * 28) {
			helper.fail("Repeated nurseries need enough open ground for distinct brood domes.");
		}
		if (horizontalDistanceSquared(mine, secondMine) < 28 * 28) {
			helper.fail("Repeated mines need enough open ground for separate excavation mounds.");
		}
		if (horizontalDistanceSquared(farm, secondFarm) < 28 * 28
				|| horizontalDistanceSquared(farm, thirdFarm) < 28 * 28
				|| horizontalDistanceSquared(secondFarm, thirdFarm) < 28 * 28) {
			helper.fail("All three chitin farms need enough open ground for distinct cultivation mounds.");
		}
		if (horizontalDistanceSquared(barracks, secondBarracks) < 32 * 32) {
			helper.fail("Repeated barracks need a wider gap for their elongated troop halls.");
		}
		if (Math.max(Math.abs(market.getX() - origin.getX()), Math.abs(market.getZ() - origin.getZ())) < 34) {
			helper.fail("Market should live in the larger diagonal village district.");
		}
		if (horizontalDistanceSquared(armory, barracks) < 30 * 30
				|| horizontalDistanceSquared(armory, secondArmory) < 34 * 34) {
			helper.fail("Armories need a separate outer district with open ground around their heavy shells.");
		}
		if (Math.max(Math.abs(watch.getX() - origin.getX()), Math.abs(watch.getZ() - origin.getZ())) < 50) {
			helper.fail("Watch posts should mark the outer claim edge in the scale pass.");
		}
		if (VisualQaScenes.qaRadius(VisualQaScenes.SETTLEMENT_SCALE) < 120) {
			helper.fail("Settlement scale scene needs a wide prepared area for the enlarged village.");
		}
		helper.succeed();
	}

	@GameTest
	public void archiveCanCompleteFirstResearch(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, helper.absolutePos(new BlockPos(4, 3, 4))));
		colony.setResource(ResourceType.KNOWLEDGE, 40);
		colony.setResource(ResourceType.RESIN, 40);
		colony.setResource(ResourceType.ORE, 40);

		if (!ColonyLogistics.startResearch(colony, ResearchNode.RESIN_MASONRY.id()).started()) {
			helper.fail("Archive should start Resin Masonry when resources are present.");
		}
		for (int i = 0; i < 6; i++) {
			ColonyLogistics.tick(colony);
		}
		if (!colony.progress().hasResearch(ResearchNode.RESIN_MASONRY.id())) {
			helper.fail("Research should complete after archive ticks.");
		}
		helper.succeed();
	}

	@GameTest

	public void colonyRenovatePreservesEconomyAndPlacesCampus(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.setResource(ResourceType.RESIN, 77);
		colony.progress().completeResearch(ResearchNode.CHITIN_CULTIVATION.id());

		ColonyService.renovateColony(helper.getLevel(), colony);

		if (colony.resource(ResourceType.RESIN) != 77) {
			helper.fail("Renovation should preserve resources.");
		}
		if (!colony.progress().hasResearch(ResearchNode.CHITIN_CULTIVATION.id())) {
			helper.fail("Renovation should preserve research.");
		}
		assertTieredMoundProfile(helper, origin, ModBlocks.NEST_MOUND, "renovated queen chamber");
		assertFoodStoreProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0), "renovated food store");
		assertNurseryProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0), "renovated nursery");
		assertMineProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.MINE, 0), "renovated mine");
		assertBarracksProfile(helper, ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0), "renovated barracks");
		helper.succeed();
	}

	@GameTest
	public void colonyLabelsAndImportantAntNamesAreVisible(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		AABB area = new AABB(
				helper.absolutePos(origin).getX() - 48, helper.absolutePos(origin).getY() - 4, helper.absolutePos(origin).getZ() - 48,
				helper.absolutePos(origin).getX() + 48, helper.absolutePos(origin).getY() + 16, helper.absolutePos(origin).getZ() + 48
		);
		boolean hasLabel = !helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, area, label -> label.getCustomName() != null).isEmpty();
		if (!hasLabel) {
			helper.fail("Colony should create visible building labels.");
		}
		boolean hasIdentityLabel = !helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, area, label -> label.getCustomName() != null && label.getCustomName().getString().contains(colony.progress().name())).isEmpty();
		if (!hasIdentityLabel) {
			helper.fail("Queen mound label should show the colony name.");
		}
		boolean hasQueenName = helper.getLevel().getEntitiesOfClass(AntEntity.class, area, ant -> ant.getCustomName() != null && ant.getCustomName().getString().contains("Queen")).stream().findAny().isPresent();
		if (!hasQueenName) {
			helper.fail("Queen should have a visible name.");
		}
		helper.succeed();
	}

	@GameTest
	public void workerAssignmentUsesDistantCampusTargets(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		Optional<BlockPos> target = AntEntity.debugWorkTarget(helper.getLevel(), AntCaste.WORKER, colony.id(), helper.absolutePos(origin));
		if (target.isEmpty()) {
			helper.fail("Worker should receive a colony-aware work target.");
		}
		if (target.get().distSqr(colony.origin()) <= 18 * 18) {
			helper.fail("Worker target should reach beyond the old 18 block scan radius.");
		}
		helper.succeed();
	}

	@GameTest
	public void minerAssignmentTargetsOreNodeBeyondOldRadius(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		BlockPos oreNode = helper.absolutePos(origin.offset(8, 0, 54));

		Optional<BlockPos> target = AntEntity.debugWorkTarget(helper.getLevel(), AntCaste.MINER, colony.id(), helper.absolutePos(origin));
		if (target.isEmpty()) {
			helper.fail("Miner should receive an ore node target.");
		}
		if (target.get().distSqr(oreNode) > 9) {
			helper.fail("Miner should target the campus ore node.");
		}
		helper.succeed();
	}

	@GameTest
	public void soldierAssignmentUsesCampusPatrolPoint(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		Optional<BlockPos> target = AntEntity.debugWorkTarget(helper.getLevel(), AntCaste.SOLDIER, colony.id(), helper.absolutePos(origin));
		if (target.isEmpty()) {
			helper.fail("Soldier should receive a patrol point.");
		}
		if (target.get().distSqr(colony.origin()) <= 18 * 18) {
			helper.fail("Soldier should prefer a campus patrol point, such as barracks or watch posts.");
		}
		if (helper.getLevel().getBlockState(target.get()).isAir()
				|| !helper.getLevel().getBlockState(target.get().above()).isAir()
				|| !helper.getLevel().getBlockState(target.get().above(2)).isAir()) {
			helper.fail("Soldier patrol target should be outside with clear headroom.");
		}
		helper.succeed();
	}

	@GameTest
	public void antSpawnPositionUsesRequestedGroundY(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		BlockPos spawn = helper.absolutePos(origin.offset(8, 1, 0));

		AntEntity ant = ColonyService.spawnAnt(helper.getLevel(), spawn, AntCaste.WORKER, colony.id());
		if (ant == null) {
			helper.fail("Worker ant should spawn.");
		}
		if (Math.abs(ant.getY() - spawn.getY()) > 0.01) {
			helper.fail("Ant spawn should use the provided Y without adding one block.");
		}
		helper.succeed();
	}

	@GameTest
	public void workerConstructionDeliveryAdvancesActiveBuilding(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		ColonyBuilding planned = ColonyBuilding.planned(BuildingType.MARKET, helper.absolutePos(ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0)));
		colony.progress().addBuilding(planned);

		boolean delivered = ColonyService.depositConstructionWork(helper.getLevel(), planned.pos(), colony.id(), AntCaste.WORKER, 9);
		if (!delivered) {
			helper.fail("Worker delivery should find the active construction.");
		}
		if (planned.constructionProgress() <= 0) {
			helper.fail("Worker delivery should advance construction progress.");
		}
		helper.succeed();
	}

	@GameTest

	public void damagedBuildingConsumesChitinAndRestoresMarketBlueprint(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.setResource(ResourceType.CHITIN, 40);
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding damaged = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		damaged.disableFor(120);
		colony.progress().addBuilding(damaged);
		StructurePlacer.placeBuilding(helper.getLevel(), damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());

		if (damaged.visualStage() != BuildingVisualStage.DAMAGED) {
			helper.fail("Damaged market should begin in the damaged visual stage.");
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		if (damaged.visualStage() != BuildingVisualStage.REPAIRING) {
			helper.fail("Chitin supplies should move a damaged market into the repairing stage.");
		}
		assertMinimalBuildingMarker(helper, market, ModBlocks.MARKET_CHAMBER, "repairing market");
		for (int i = 0; i < 4 && damaged.disabledTicks() > 0; i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (damaged.disabledTicks() != 0 || damaged.visualStage() != BuildingVisualStage.COMPLETE) {
			helper.fail("Repair ticks should restore the building to complete service, got "
					+ damaged.visualStage().id() + " progress " + damaged.constructionProgress()
					+ " disabled " + damaged.disabledTicks() + " task " + colony.currentTask());
		}
		assertMarketProfile(helper, market, "repaired market");
		helper.succeed();
	}

	@GameTest

	public void repairContractDeliveryStartsDamagedBuildingRepair(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.setResource(ResourceType.CHITIN, 0);
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding damaged = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		damaged.disableFor(120);
		colony.progress().addBuilding(damaged);
		StructurePlacer.placeBuilding(helper.getLevel(), damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());

		ColonyBuilder.tick(helper.getLevel(), colony);
		if (damaged.visualStage() != BuildingVisualStage.DAMAGED) {
			helper.fail("Damaged market should wait in the damaged stage while repair chitin is missing.");
		}
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(entry -> entry.reason().equals("repair market"))
				.findFirst()
				.orElse(null);
		if (contract == null || contract.resource() != ResourceType.CHITIN) {
			helper.fail("Missing repair supplies should open a chitin repair contract.");
		}

		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
		if (!result.success() || !result.complete()) {
			helper.fail("Player chitin delivery should complete the repair contract.");
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		if (damaged.visualStage() != BuildingVisualStage.REPAIRING) {
			helper.fail("Completed repair contract should move the building into repair, got " + damaged.visualStage().id());
		}
		if (colony.resource(ResourceType.CHITIN) != 0 || !colony.progress().requestsView().isEmpty()) {
			helper.fail("Repair should consume delivered chitin and clear the repair request.");
		}
		assertMinimalBuildingMarker(helper, market, ModBlocks.MARKET_CHAMBER, "contract-repair market");
		helper.succeed();
	}

	@GameTest

	public void constructionContractDeliveryStartsMinimalQueuedSite(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.progress().requests().clear();
		colony.progress().buildQueue().add(BuildingType.MARKET);
		colony.setResource(ResourceType.FOOD, BuildingType.MARKET.cost(ResourceType.FOOD));
		colony.setResource(ResourceType.ORE, 0);
		colony.setResource(ResourceType.CHITIN, BuildingType.MARKET.cost(ResourceType.CHITIN));

		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(entry -> entry.reason().equals("construction " + BuildingType.MARKET.id()))
				.filter(entry -> entry.resource() == ResourceType.ORE)
				.findFirst()
				.orElse(null);
		if (contract == null) {
			helper.fail("Missing queued market ore should open a construction contract.");
		}
		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
		if (!result.success() || !result.complete()) {
			helper.fail("Player ore delivery should complete the construction contract.");
		}
		if (colony.resource(ResourceType.ORE) < BuildingType.MARKET.cost(ResourceType.ORE)) {
			helper.fail("Construction contract delivery should place ore into colony stores for the queued build.");
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding active = colony.progress().firstIncomplete().orElse(null);
		if (active == null || active.type() != BuildingType.MARKET || active.visualStage() != BuildingVisualStage.PLANNED) {
			helper.fail("Completed construction contract should start a planned market site, got "
					+ (active == null ? "none" : active.type().id() + " " + active.visualStage().id()));
		}
		if (colony.progress().buildQueueView().contains(BuildingType.MARKET) || !colony.progress().requestsView().isEmpty()) {
			helper.fail("Started market site should consume the queue entry and clear the fulfilled construction request.");
		}
		assertMinimalBuildingMarker(helper, ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0), Blocks.DIRT_PATH, "planned contract market");
		helper.succeed();
	}

	@GameTest

	public void repairSceneKeepsGameplayCuesAroundMinimalMarkers(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.REPAIR_SCENE)) {
			helper.fail("Visual QA should expose a repair_scene.");
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		VisualQaScenes.seedRepairScene(helper.getLevel(), colony);
		List<BlockPos> centers = VisualQaScenes.repairSceneBuildingCenters(origin);
		BlockPos damaged = centers.get(0);
		BlockPos repairing = centers.get(1);
		BlockPos restored = centers.get(2);
		assertMinimalBuildingMarker(helper, damaged, ModBlocks.MARKET_CHAMBER, "damaged repair-scene market");
		assertMinimalBuildingMarker(helper, repairing, ModBlocks.MARKET_CHAMBER, "active repair-scene market");
		assertMarketProfile(helper, restored, "restored repair-scene market");
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(0, 1, -5));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, repairing.offset(-2, 1, -5));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(2, 1, -5));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, restored.offset(0, 1, -5));

		BlockPos absoluteOrigin = helper.absolutePos(origin);
		AABB sceneBounds = new AABB(
				absoluteOrigin.getX() - 36, absoluteOrigin.getY() - 2, absoluteOrigin.getZ() - 44,
				absoluteOrigin.getX() + 36, absoluteOrigin.getY() + 10, absoluteOrigin.getZ() - 8
		);
		List<AntEntity> repairAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, sceneBounds, ant -> ant.colonyId() == colony.id());
		if (repairAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| repairAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_CHITIN)) {
			helper.fail("Repair scene should show a working repair ant and a chitin carrier.");
		}
		helper.succeed();
	}

	@GameTest

	public void idleMatureColonyStartsAndCompletesMarketUpgrade(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 500);
		}
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding building = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		colony.progress().addBuilding(building);
		StructurePlacer.placeBuilding(helper.getLevel(), building.pos(), building.type(), building.visualStage(), colony.progress().culture());

		ColonyBuilder.tick(helper.getLevel(), colony);
		if (building.level() != 2 || building.constructionProgress() != 0 || building.visualStage() != BuildingVisualStage.CONSTRUCTION) {
			helper.fail("Idle mature colony should begin a level 2 upgrade, got level "
					+ building.level() + " progress " + building.constructionProgress()
					+ " stage " + building.visualStage().id() + " task " + colony.currentTask()
					+ " queue " + colony.progress().buildQueueView());
		}
		assertMarketProfile(helper, market, "market retained during upgrade");
		for (int i = 0; i < 5; i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (building.visualStage() != BuildingVisualStage.UPGRADED) {
			helper.fail("Upgrade construction should complete into the upgraded visual stage.");
		}
		assertMarketProfile(helper, market, "upgraded market");
		helper.succeed();
	}

	@GameTest

	public void queenBroodRecurringEventLeavesNurseryProof(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.setResource(ResourceType.FOOD, 260);
		colony.setResource(ResourceType.CHITIN, 80);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		int workers = colony.casteCount(AntCaste.WORKER);
		int food = colony.resource(ResourceType.FOOD);
		int chitin = colony.resource(ResourceType.CHITIN);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Ready mature colony should trigger or reschedule a recurring event.");
		}
		if (!colony.currentTask().contains("queen brood bloom")) {
			helper.fail("Recurring event should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("queen brood bloom"))) {
			helper.fail("Recurring event should be visible in the colony event log.");
		}
		if (colony.casteCount(AntCaste.WORKER) != workers + 2) {
			helper.fail("Queen brood bloom should add two workers.");
		}
		if (colony.resource(ResourceType.FOOD) >= food || colony.resource(ResourceType.CHITIN) >= chitin) {
			helper.fail("Queen brood bloom should consume food and chitin stores.");
		}
		BlockPos nursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0);
		assertNurseryProfile(helper, nursery, "event nursery");
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, nursery.offset(0, 1, -2));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, nursery.offset(-1, 1, -1));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, nursery.offset(0, 2, -1));
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Recurring event should not fire again until another interval passes.");
		}
		helper.succeed();
	}

	@GameTest

	public void famineRecurringEventMarksFoodStoreAndOpensContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().requests().clear();
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		colony.setResource(ResourceType.FOOD, 4);
		int foodBefore = colony.resource(ResourceType.FOOD);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Low-food mature colony should trigger a famine warning recurring event.");
		}
		if (!colony.currentTask().contains("famine warning")) {
			helper.fail("Famine event should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("famine warning"))) {
			helper.fail("Famine event should be visible in the colony event log.");
		}
		if (colony.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.FOOD_STORE
						&& request.resource() == ResourceType.FOOD
						&& request.reason().equals(ColonyRecurringEvents.FAMINE_REASON))) {
			helper.fail("Famine event should open a food-store player help contract.");
		}
		if (colony.resource(ResourceType.FOOD) != foodBefore) {
			helper.fail("Famine warning should not consume the colony's last food.");
		}
		BlockPos food = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0);
		assertFoodStoreProfile(helper, food, "famine food store");
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, food.offset(0, 1, -2));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, food.offset(1, 1, -1));
		helper.assertBlockPresent(Blocks.RED_MUSHROOM_BLOCK, food.offset(-2, 1, 0));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, food.offset(0, 2, -1));
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Open famine request should prevent duplicate famine warnings.");
		}
		helper.succeed();
	}

	@GameTest
	public void migrationRecurringEventMarksDaughterNestTrail(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding building = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		colony.progress().addBuilding(building);
		StructurePlacer.placeBuilding(helper.getLevel(), building.pos(), building.type(), building.visualStage(), colony.progress().culture());
		colony.setResource(ResourceType.FOOD, 240);
		colony.setResource(ResourceType.CHITIN, 90);
		colony.addCaste(AntCaste.WORKER, 26);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Overcrowded mature colony should trigger a migration preparation recurring event.");
		}
		if (!colony.currentTask().contains("migration preparation")) {
			helper.fail("Migration event should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("migration preparation"))) {
			helper.fail("Migration event should be visible in the colony event log.");
		}
		if (colony.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.MARKET
						&& request.resource() == ResourceType.FOOD
						&& request.reason().equals(ColonyRecurringEvents.MIGRATION_REASON))) {
			helper.fail("Migration event should open a market food contract for trail supplies.");
		}

		BlockPos camp = origin.offset(-34, 0, -30);
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-10, 0, -10));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, origin.offset(-14, 1, -14));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, camp.above());
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, camp.above(2));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, camp.north());
		helper.assertBlockPresent(Blocks.PACKED_MUD, camp.south());

		BlockPos absoluteCamp = helper.absolutePos(camp);
		AABB campBounds = new AABB(
				absoluteCamp.getX() - 4, absoluteCamp.getY(), absoluteCamp.getZ() - 4,
				absoluteCamp.getX() + 5, absoluteCamp.getY() + 6, absoluteCamp.getZ() + 5
		);
		List<AntEntity> scouts = helper.getLevel().getEntitiesOfClass(AntEntity.class, campBounds, ant ->
				ant.colonyId() == colony.id()
						&& ant.caste() == AntCaste.SCOUT
						&& ant.workState() == AntWorkState.PATROLLING);
		if (scouts.size() != 3) {
			helper.fail("Migration camp should show three patrolling scout ants, got " + scouts.size());
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Open migration request should prevent duplicate migration preparation events.");
		}
		helper.succeed();
	}

	@GameTest
	public void invasionWarningRecurringEventMarksApproachAndOpensDefenseContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 112);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), false);
		allied.progress().requests().clear();
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		rival.progress().setRaidCooldown(120);
		rival.addCaste(AntCaste.SOLDIER, 2);
		allied.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Rival raid window should trigger an invasion warning recurring event.");
		}
		if (!allied.currentTask().contains("invasion warning")) {
			helper.fail("Invasion warning should be visible in the current task, got " + allied.currentTask());
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("invasion warning"))) {
			helper.fail("Invasion warning should be visible in the colony event log.");
		}
		if (allied.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.BARRACKS
						&& request.resource() == ResourceType.CHITIN
						&& request.reason().equals(ColonyRecurringEvents.INVASION_WARNING_REASON))) {
			helper.fail("Invasion warning should open a barracks chitin defense contract.");
		}

		BlockPos rally = origin.offset(36, 0, 0);
		assertBlockInColumn(helper, rally, Blocks.RED_TERRACOTTA, 1, 3, "invasion warning signal");
		assertBlockInColumn(helper, rally, Blocks.BLACKSTONE, 2, 4, "invasion warning cap");
		assertBlockInColumn(helper, rally, Blocks.CANDLE, 3, 5, "invasion warning candle");
		helper.assertBlockPresent(Blocks.BONE_BLOCK, rally.south());
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, rally.offset(0, 0, 2));
		helper.assertBlockPresent(Blocks.PODZOL, rally.offset(3, 0, 0));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, rally.offset(3, 1, 0));

		AABB guardBounds = new AABB(
				helper.absolutePos(rally).getX() - 5, helper.absolutePos(rally).getY(), helper.absolutePos(rally).getZ() - 5,
				helper.absolutePos(rally).getX() + 5, helper.absolutePos(rally).getY() + 6, helper.absolutePos(rally).getZ() + 5
		);
		List<AntEntity> guardAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, guardBounds, ant ->
				ant.colonyId() == allied.id()
						&& ant.caste() == AntCaste.SOLDIER
						&& ant.workState() == AntWorkState.PATROLLING);
		if (guardAnts.size() != 3) {
			helper.fail("Invasion warning should place three allied guard ants at the approach marker, got " + guardAnts.size());
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Open invasion defense request should prevent duplicate invasion warnings.");
		}
		helper.succeed();
	}

	@GameTest
	public void treatyOpportunityRecurringEventMarksEnvoyRouteAndOpensResinContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 112);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData neutral = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), ColonyCulture.LEAFCUTTER);
		BlockPos shrine = ColonyBuilder.siteFor(allied, BuildingType.DIPLOMACY_SHRINE);
		allied.progress().addBuilding(ColonyBuilding.complete(BuildingType.DIPLOMACY_SHRINE, shrine));
		StructurePlacer.placeBuilding(helper.getLevel(), shrine, BuildingType.DIPLOMACY_SHRINE, BuildingVisualStage.COMPLETE, allied.progress().culture());
		allied.progress().requests().clear();
		allied.progress().setRelation(neutral.id(), DiplomacyState.NEUTRAL);
		neutral.progress().setRelation(allied.id(), DiplomacyState.NEUTRAL);
		allied.setResource(ResourceType.FOOD, 260);
		allied.setResource(ResourceType.CHITIN, 4);
		allied.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Neutral neighbor should trigger a treaty opportunity recurring event.");
		}
		if (!allied.currentTask().contains("treaty opportunity")) {
			helper.fail("Treaty opportunity should be visible in the current task, got " + allied.currentTask());
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("treaty opportunity"))) {
			helper.fail("Treaty opportunity should be visible in the allied colony event log.");
		}
		if (neutral.progress().eventsView().stream().noneMatch(event -> event.message().contains("treaty opportunity"))) {
			helper.fail("Treaty opportunity should leave evidence on the neutral colony too.");
		}
		if (allied.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.DIPLOMACY_SHRINE
						&& request.resource() == ResourceType.RESIN
						&& request.reason().equals(ColonyRecurringEvents.TREATY_OPPORTUNITY_REASON))) {
			helper.fail("Treaty opportunity should open a diplomacy-shrine resin contract.");
		}
		if (ColonyLogistics.contracts(allied).stream().noneMatch(contract ->
				contract.reason().equals(ColonyRecurringEvents.TREATY_OPPORTUNITY_REASON) && contract.priority() >= 4)) {
			helper.fail("Treaty opportunity contract should be prioritized for the player.");
		}

		BlockPos camp = ColonyRecurringEvents.treatyOpportunityCamp(origin, origin.offset(64, 0, 0));
		assertBlockInColumn(helper, camp, Blocks.HONEYCOMB_BLOCK, 1, 3, "treaty opportunity envoy cache");
		assertBlockInColumn(helper, camp, Blocks.AMETHYST_BLOCK, 2, 4, "treaty opportunity signal gem");
		assertBlockInColumn(helper, camp, Blocks.CANDLE, 3, 5, "treaty opportunity candle");
		helper.assertBlockPresent(Blocks.MOSS_BLOCK, camp.north());
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(8, 0, 0));

		AABB campBounds = new AABB(
				helper.absolutePos(camp).getX() - 5, helper.absolutePos(camp).getY(), helper.absolutePos(camp).getZ() - 5,
				helper.absolutePos(camp).getX() + 5, helper.absolutePos(camp).getY() + 6, helper.absolutePos(camp).getZ() + 5
		);
		List<AntEntity> envoys = helper.getLevel().getEntitiesOfClass(AntEntity.class, campBounds, ant ->
				ant.workState() == AntWorkState.CARRYING_RESIN || ant.workState() == AntWorkState.CARRYING_FUNGUS);
		if (envoys.size() != 2) {
			helper.fail("Treaty opportunity should show two visible envoy ants carrying supplies, got " + envoys.size());
		}
		if (envoys.stream().noneMatch(ant -> ant.colonyId() == allied.id() && ant.caste() == AntCaste.SCOUT && ant.workState() == AntWorkState.CARRYING_RESIN)
				|| envoys.stream().noneMatch(ant -> ant.colonyId() == neutral.id() && ant.caste() == AntCaste.WORKER && ant.workState() == AntWorkState.CARRYING_FUNGUS)) {
			helper.fail("Treaty opportunity should pair an allied resin scout with a neutral fungus worker.");
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Open treaty opportunity request should prevent duplicate treaty events.");
		}
		helper.succeed();
	}

	@GameTest
	public void expansionOpportunityRecurringEventMarksClaimEdgeOutpostAndOpensOreContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 96);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		colony.progress().buildQueue().clear();
		colony.progress().requests().clear();
		for (BuildingType type : List.of(BuildingType.MARKET, BuildingType.WATCH_POST)) {
			BlockPos site = ColonyBuilder.siteFor(colony, type);
			colony.progress().addBuilding(ColonyBuilding.complete(type, site));
			StructurePlacer.placeBuilding(helper.getLevel(), site, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
		}
		colony.setResource(ResourceType.FOOD, 160);
		colony.setResource(ResourceType.CHITIN, 4);
		colony.setResource(ResourceType.ORE, 0);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Mature watched colony should trigger an expansion opportunity recurring event.");
		}
		if (!colony.currentTask().contains("expansion opportunity")) {
			helper.fail("Expansion opportunity should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("expansion opportunity"))) {
			helper.fail("Expansion opportunity should be visible in the colony event log.");
		}
		if (colony.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.WATCH_POST
						&& request.resource() == ResourceType.ORE
						&& request.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON))) {
			helper.fail("Expansion opportunity should open a watch-post ore contract.");
		}
		if (ColonyLogistics.contracts(colony).stream().noneMatch(contract ->
				contract.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON) && contract.priority() >= 4)) {
			helper.fail("Expansion opportunity contract should be prioritized for the player.");
		}

		BlockPos outpost = ColonyRecurringEvents.expansionOutpost(origin, ColonyRank.HIVE.claimRadius());
		assertBlockInColumn(helper, outpost, ModBlocks.WATCH_POST, 1, 2, "expansion outpost post");
		assertBlockInColumn(helper, outpost, Blocks.COBBLED_DEEPSLATE_WALL, 2, 3, "expansion outpost brace");
		assertBlockInColumn(helper, outpost, Blocks.OCHRE_FROGLIGHT, 3, 4, "expansion outpost signal");
		if (!isLandmarkTrail(helper.getLevel().getBlockState(helper.absolutePos(origin.offset(8, 0, 8))).getBlock())) {
			helper.fail("Expansion opportunity should keep a readable trail out of the enlarged colony.");
		}
		helper.assertBlockPresent(Blocks.IRON_ORE, outpost.offset(-2, 1, -5));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, outpost.offset(2, 1, -5));
		helper.assertBlockPresent(Blocks.OAK_FENCE, outpost.offset(4, 1, 0));

		BlockPos absoluteOutpost = helper.absolutePos(outpost);
		AABB crewBounds = new AABB(
				absoluteOutpost.getX() - 7, absoluteOutpost.getY(), absoluteOutpost.getZ() - 7,
				absoluteOutpost.getX() + 7, absoluteOutpost.getY() + 8, absoluteOutpost.getZ() + 7
		);
		List<AntEntity> crew = helper.getLevel().getEntitiesOfClass(AntEntity.class, crewBounds, ant ->
				ant.colonyId() == colony.id()
						&& (ant.workState() == AntWorkState.CARRYING_ORE
						|| ant.workState() == AntWorkState.CARRYING_CHITIN
						|| ant.workState() == AntWorkState.PATROLLING));
		if (crew.size() != 3) {
			helper.fail("Expansion opportunity should place three visible outpost crew ants, got " + crew.size());
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Open expansion outpost request should prevent duplicate expansion opportunities.");
		}
		helper.succeed();
	}

	@GameTest

	public void completedExpansionOutpostContractSecuresClaimEdgeWatchPost(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 96);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		colony.progress().buildQueue().clear();
		colony.progress().requests().clear();
		for (BuildingType type : List.of(BuildingType.MARKET, BuildingType.WATCH_POST)) {
			BlockPos site = ColonyBuilder.siteFor(colony, type);
			colony.progress().addBuilding(ColonyBuilding.complete(type, site));
			StructurePlacer.placeBuilding(helper.getLevel(), site, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
		}
		colony.setResource(ResourceType.FOOD, 160);
		colony.setResource(ResourceType.CHITIN, 4);
		colony.setResource(ResourceType.ORE, 0);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		int oldClaim = colony.progress().claimRadius();

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Mature watched colony should open an expansion outpost contract before payoff.");
		}
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(candidate -> candidate.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON))
				.findFirst()
				.orElse(null);
		if (contract == null) {
			helper.fail("Expansion payoff needs the expansion outpost contract to exist.");
		}
		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
		if (!result.success() || !result.complete()) {
			helper.fail("Expansion outpost contract should complete from a full player delivery.");
		}
		if (!ColonyRecurringEvents.completeExpansionOutpost(helper.getLevel(), colony)) {
			helper.fail("Completed expansion outpost contract should secure the claim edge.");
		}

		BlockPos outpost = ColonyRecurringEvents.expansionOutpost(origin, ColonyRank.HIVE.claimRadius());
		assertMinimalBuildingMarker(helper, outpost, ModBlocks.WATCH_POST, "completed expansion outpost");
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, outpost.offset(6, 1, 0));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, outpost.offset(5, 1, 5));

		BlockPos absoluteOutpost = helper.absolutePos(outpost);
		if (colony.progress().buildingsView().stream().noneMatch(building ->
				building.type() == BuildingType.WATCH_POST
						&& building.complete()
						&& building.pos().equals(absoluteOutpost))) {
			helper.fail("Secured expansion outpost should be registered as a completed watch post.");
		}
		if (colony.progress().claimRadius() <= oldClaim || colony.progress().claimRadius() < 44) {
			helper.fail("Secured expansion outpost should expand claim radius beyond " + oldClaim + ", got " + colony.progress().claimRadius());
		}
		if (!colony.currentTask().contains("Expansion outpost secured")) {
			helper.fail("Expansion payoff should be visible in current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("Expansion complete"))) {
			helper.fail("Expansion payoff should leave an event-log entry.");
		}
		if (!colony.progress().requestsView().isEmpty()) {
			helper.fail("Completed expansion outpost contract should clear the open request.");
		}
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Secured expansion outpost should prevent reopening the same expansion opportunity.");
		}
		helper.succeed();
	}

	@GameTest

	public void citadelColonyCompletesMinimalGreatMoundProject(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 72);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 800);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 12);
		colony.addCaste(AntCaste.SOLDIER, 5);
		for (BuildingType type : List.of(BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE, BuildingType.ARMORY, BuildingType.DIPLOMACY_SHRINE)) {
			if (!colony.progress().hasCompleted(type)) {
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(helper.getLevel(), pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding greatMound = colony.progress().buildings().stream()
				.filter(building -> building.type() == BuildingType.GREAT_MOUND)
				.findFirst()
				.orElse(null);
		if (greatMound == null) {
			helper.fail("Citadel colony should start the great mound endgame project.");
		}
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.ENDGAME_PROJECT)) {
			helper.fail("Visual QA should expose an endgame_project scene.");
		}
		for (int i = 0; i < 4 && !greatMound.complete(); i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (!greatMound.complete()) {
			helper.fail("Great mound should complete from the prepared endgame resources, got " + greatMound.constructionProgress() + "%.");
		}
		assertTieredMoundProfile(helper, origin, ModBlocks.NEST_MOUND, "great mound");
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("great_mound"))) {
			helper.fail("Great mound project should leave a colony event for the player.");
		}
		helper.succeed();
	}

	@GameTest

	public void citadelColonyCompletesMinimalQueenVaultAfterGreatMound(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 76);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 900);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 12);
		colony.addCaste(AntCaste.SOLDIER, 5);
		for (BuildingType type : List.of(BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE, BuildingType.ARMORY, BuildingType.DIPLOMACY_SHRINE, BuildingType.GREAT_MOUND)) {
			if (!colony.progress().hasCompleted(type)) {
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(helper.getLevel(), pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding vault = colony.progress().buildings().stream()
				.filter(building -> building.type() == BuildingType.QUEEN_VAULT)
				.findFirst()
				.orElse(null);
		if (vault == null) {
			helper.fail("Citadel colony should start the queen vault after the Great Mound.");
		}
		for (int i = 0; i < 4 && !vault.complete(); i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (!vault.complete()) {
			helper.fail("Queen vault should complete from prepared endgame resources, got " + vault.constructionProgress() + "%.");
		}
		assertTieredMoundProfile(helper, origin, ModBlocks.NEST_CORE, "queen vault");
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("queen_vault"))) {
			helper.fail("Queen vault project should leave a colony event for the player.");
		}
		helper.succeed();
	}

	@GameTest

	public void citadelColonyCompletesMinimalTradeHubAfterQueenVault(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 90);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 1000);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 12);
		colony.addCaste(AntCaste.SOLDIER, 5);
		for (BuildingType type : List.of(BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE, BuildingType.ARMORY, BuildingType.DIPLOMACY_SHRINE, BuildingType.GREAT_MOUND, BuildingType.QUEEN_VAULT)) {
			if (!colony.progress().hasCompleted(type)) {
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(helper.getLevel(), pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding tradeHub = colony.progress().buildings().stream()
				.filter(building -> building.type() == BuildingType.TRADE_HUB)
				.findFirst()
				.orElse(null);
		if (tradeHub == null) {
			helper.fail("Citadel colony should start the trade hub after the Queen Vault.");
		}
		for (int i = 0; i < 4 && !tradeHub.complete(); i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (!tradeHub.complete()) {
			helper.fail("Trade hub should complete from prepared endgame resources, got " + tradeHub.constructionProgress() + "%.");
		}
		BlockPos hub = ColonyBuilder.siteFor(origin, BuildingType.TRADE_HUB, 0);
		assertMinimalBuildingMarker(helper, hub, ModBlocks.MARKET_CHAMBER, "trade hub");
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("trade_hub"))) {
			helper.fail("Trade hub project should leave a colony event for the player.");
		}
		helper.succeed();
	}

	@GameTest
	public void completedTradeHubImprovesTradeTerms(GameTestHelper helper) {
		ColonyData colony = new ColonyData(99, helper.absolutePos(new BlockPos(2, 3, 2)));
		ColonyTradeCatalog.Offer sellWheat = tradeOffer("sell_wheat");
		ColonyTradeCatalog.Offer buySeal = tradeOffer("buy_colony_seal");

		int starterTokenReward = ColonyTradeCatalog.outputCount(colony, sellWheat);
		int starterSealCost = ColonyTradeCatalog.inputCount(colony, buySeal);

		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, ColonyBuilder.siteFor(colony, BuildingType.TRADE_HUB)));
		colony.progress().addReputation(20);

		int hubTokenReward = ColonyTradeCatalog.outputCount(colony, sellWheat);
		int hubSealCost = ColonyTradeCatalog.inputCount(colony, buySeal);
		if (hubTokenReward != starterTokenReward + 1) {
			helper.fail("Trade Hub should add one pheromone token to normal sell offers.");
		}
		if (hubSealCost != 14 || hubSealCost >= starterSealCost) {
			helper.fail("Trade Hub should discount token purchases from 16 to 14, got " + hubSealCost + ".");
		}
		if (!ColonyTradeCatalog.availabilityText(colony, sellWheat).contains("Trade Hub")
				|| !ColonyTradeCatalog.availabilityText(colony, buySeal).contains("Trade Hub")) {
			helper.fail("Trade Hub terms should be visible in trade row status text.");
		}
		helper.succeed();
	}

	@GameTest
	public void tabletTradeSceneShowsTradeHubTerms(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_TRADE)) {
			helper.fail("Visual QA should expose a tablet_trade scene.");
		}
		ColonyData colony = new ColonyData(99, helper.absolutePos(new BlockPos(2, 3, 2)));
		colony.progress().addReputation(20);
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, ColonyBuilder.siteFor(colony, BuildingType.TRADE_HUB)));

		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Trade", "");
		if (!snapshot.initialTab().equals("Trade")) {
			helper.fail("Tablet trade scene should open the Trade tab.");
		}
		ColonyUiSnapshot.TradeEntry sellWheat = tradeEntry(snapshot, "sell_wheat");
		ColonyUiSnapshot.TradeEntry buySeal = tradeEntry(snapshot, "buy_colony_seal");
		if (sellWheat == null || sellWheat.outputCount() != 2 || !sellWheat.status().contains("Trade Hub")) {
			helper.fail("Trade Hub tablet should show sell_wheat as a +1 token offer.");
		}
		if (buySeal == null || buySeal.inputCount() != 14 || !buySeal.available() || !buySeal.status().contains("Trade Hub")) {
			helper.fail("Trade Hub tablet should show buy_colony_seal as an available discounted token purchase.");
		}
		colony.addEvent("Recurring event: trade caravan exchanged 8 food for 8 resin with colony #7");
		ColonyUiSnapshot payoffSnapshot = ColonyUiSnapshot.from(colony, "Trade", "");
		if (!payoffSnapshot.tradeActivity().contains("8 Food -> 8 Resin with #7")) {
			helper.fail("Tablet trade scene should expose the latest caravan payoff line, got " + payoffSnapshot.tradeActivity());
		}
		helper.succeed();
	}

	@GameTest
	public void workCycleSceneAndWorkStatesAreAvailable(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.WORK_CYCLE)) {
			helper.fail("Visual QA should expose a work_cycle scene.");
		}
		List<BlockPos> jobCenters = VisualQaScenes.workCycleJobCenters(helper.absolutePos(new BlockPos(2, 3, 2)));
		if (jobCenters.size() != 4) {
			helper.fail("Work-cycle scene should expose build, ore, patrol, and food logistics job centers.");
		}
		BlockPos focalPoint = helper.absolutePos(new BlockPos(2, 3, 2).offset(0, 0, -23));
		for (BlockPos jobCenter : jobCenters) {
			if (Math.abs(jobCenter.getX() - focalPoint.getX()) > 10 || Math.abs(jobCenter.getZ() - focalPoint.getZ()) > 4) {
				helper.fail("Work-cycle jobs should stay clustered in the central camera field.");
			}
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		VisualQaScenes.seedWorkCycle(helper.getLevel(), colony);
		AABB demoBounds = VisualQaScenes.workCycleActorCleanupBounds(colony.origin());
		List<AntEntity> demoAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, demoBounds, ant -> ant.colonyId() == colony.id());
		if (demoAnts.size() != 4) {
			helper.fail("Work-cycle scene should keep only the four purpose-built demo ants in frame, got " + demoAnts.size());
		}
		if (demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_ORE)
				|| demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_FOOD)
				|| demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.PATROLLING)) {
			helper.fail("Work-cycle ants should hold visible build, haul, forage, and patrol states.");
		}
		if (!helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, demoBounds).isEmpty()) {
			helper.fail("Work-cycle scene should remove colony text labels so job markers dominate.");
		}
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-8, 0, -29));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-8, 0, -28));
		helper.assertBlockPresent(Blocks.IRON_ORE, origin.offset(0, 0, -26));
		helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE, origin.offset(0, 0, -24));
		helper.assertBlockPresent(Blocks.POLISHED_DEEPSLATE, origin.offset(9, 0, -29));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, origin.offset(9, 0, -27));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, origin.offset(2, 0, -19));
		helper.assertBlockPresent(Blocks.COMPOSTER, origin.offset(0, 1, -18));
		helper.assertBlockPresent(Blocks.BARREL, origin.offset(4, 1, -20));
		helper.assertBlockPresent(Blocks.OAK_FENCE, origin.offset(2, 1, -17));
		helper.succeed();
	}

	@GameTest
	public void antLineupKeepsSmallCastesNearCameraFocus(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.ANT_LINEUP)) {
			helper.fail("Visual QA should expose an ant_lineup scene.");
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		// The lineup lives on a single clean row south of the mound (z = origin.z + 20)
		// so the small worker castes sit together in the camera focus instead of being
		// dwarfed by the central mound. All castes share the same row z and are spread
		// on x, so the small castes must stay in the central band of that row (|dx| <= 12)
		// no caste may drift north of the mound front (z must stay > origin.z, i.e. south).
		for (AntCaste caste : List.of(AntCaste.WORKER, AntCaste.SCOUT, AntCaste.MINER)) {
			BlockPos pos = VisualQaScenes.antLineupPosition(origin, caste);
			if (Math.abs(pos.getX() - origin.getX()) > 12 || pos.getZ() != origin.getZ() + 20) {
				helper.fail("Small caste " + caste.id() + " should stay on the central band of the south lineup row (origin.z + 20), got " + pos.toShortString());
			}
		}
		for (AntCaste first : AntCaste.values()) {
			BlockPos firstPos = VisualQaScenes.antLineupPosition(origin, first);
			if (Math.abs(firstPos.getX() - origin.getX()) > 18) {
				helper.fail("Ant lineup caste " + first.id() + " should stay inside the widened camera frame.");
			}
			for (AntCaste second : AntCaste.values()) {
				if (first.ordinal() >= second.ordinal()) {
					continue;
				}
				BlockPos secondPos = VisualQaScenes.antLineupPosition(origin, second);
				int dx = firstPos.getX() - secondPos.getX();
				int dz = firstPos.getZ() - secondPos.getZ();
				if (dx * dx + dz * dz < 25) {
					helper.fail("Ant lineup labels need at least five blocks of horizontal separation between "
							+ first.id() + " and " + second.id() + ".");
				}
			}
		}
		helper.succeed();
	}

	@GameTest
	public void tabletControlsChangeColonyState(GameTestHelper helper) {
		// Content row tablet_interactions_functional: prove the Colony Tablet control
		// entry points the player buttons actually invoke (ColonyService.startResearch
		// and ColonyService.completeContract) drive real, observable colony state
		// changes - research started with knowledge consumed, and a colony request
		// fulfilled with reward tokens granted and reputation raised - so the menus
		// are never "just buttons that do not clearly do anything."
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 96);
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// Stand inside the nearest-colony lookup radius so the tablet finds this colony.
		player.setPos(helper.absolutePos(origin).getX() + 0.5, helper.absolutePos(origin).getY() + 1,
				helper.absolutePos(origin).getZ() + 0.5);
		ColonyData colony = ColonyService.createColony(level, helper.absolutePos(origin));
		// Research control needs the pheromone archive + research costs available.
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, helper.absolutePos(origin).offset(6, 0, 6)));
		colony.setResource(ResourceType.KNOWLEDGE, 60);
		colony.setResource(ResourceType.RESIN, 40);
		colony.setResource(ResourceType.ORE, 40);
		int knowledgeBefore = colony.resource(ResourceType.KNOWLEDGE);
		if (!colony.progress().activeResearch().isEmpty()) {
			helper.fail("fixture: colony should start with no active research");
		}

		boolean started = ColonyService.startResearch(player, ResearchNode.RESIN_MASONRY.id());
		if (!started) {
			helper.fail("Tablet Research control should start research when the colony is eligible.");
		}
		if (colony.progress().activeResearch().isEmpty()
				|| !colony.progress().activeResearch().get().nodeId().equals(ResearchNode.RESIN_MASONRY.id())) {
			helper.fail("Research control should record an active research node the tablet can display.");
		}
		if (colony.resource(ResourceType.KNOWLEDGE) >= knowledgeBefore) {
			helper.fail("Research control should spend knowledge resources from the colony store.");
		}
		if (!colony.currentTask().toLowerCase(java.util.Locale.ROOT).contains("researching")) {
			helper.fail("Research control should surface a 'researching' task the tablet overview can show.");
		}

		// Request/contract control: open a player-supplied famine contract, give the
		// player the food item it asks for, then invoke the contract control and prove
		// the request is fulfilled, reputation rises, and the player earns reward tokens.
		ColonyLogistics.requestResource(colony, BuildingType.FOOD_STORE, ResourceType.FOOD, 24, ColonyRecurringEvents.FAMINE_REASON);
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(entry -> entry.reason().equals(ColonyRecurringEvents.FAMINE_REASON))
				.findFirst()
				.orElseThrow();
		int reputationBefore = colony.progress().reputation();
		int tokenStockBefore = countItem(player, com.formicfrontier.registry.ModItems.PHEROMONE_TOKEN);
		// FOOD contracts accept wheat; give enough wheat to satisfy the contract.
		player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT, 64));

		boolean accepted = ColonyService.completeContract(player, contract.id());
		if (!accepted) {
			helper.fail("Tablet Needs/contract control should accept a delivery that fulfills a colony request.");
		}
		if (colony.progress().requestsView().stream().anyMatch(request -> request.reason().equals(ColonyRecurringEvents.FAMINE_REASON))) {
			helper.fail("Contract control should close the fulfilled colony request.");
		}
		if (colony.progress().reputation() <= reputationBefore) {
			helper.fail("Contract control should raise colony reputation as the reward.");
		}
		if (countItem(player, com.formicfrontier.registry.ModItems.PHEROMONE_TOKEN) <= tokenStockBefore) {
			helper.fail("Contract control should grant the player reward pheromone tokens.");
		}

		// The same snapshot path the tablet renders must reflect both control effects:
		// active research present, and the famine request no longer open.
		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Research", "");
		if (snapshot.research().stream().noneMatch(entry -> entry.active() && entry.nodeId().equals(ResearchNode.RESIN_MASONRY.id()))) {
			helper.fail("Tablet snapshot should expose the research node the control started.");
		}
		if (snapshot.requests().stream().anyMatch(entry -> ColonyRecurringEvents.FAMINE_REASON.equals(entry.reason()))) {
			helper.fail("Tablet snapshot should no longer list the fulfilled famine request.");
		}
		helper.succeed();
	}

	private static void assertStarterQueueStarts(GameTestHelper helper, ColonyData colony, BuildingType expected) {
		if (colony.progress().buildQueueView().isEmpty() || colony.progress().buildQueueView().getFirst() != expected) {
			helper.fail("Expected " + colony.progress().culture().id() + " starter queue to begin with " + expected.id()
					+ ", got " + colony.progress().buildQueueView());
		}
	}

	private static ColonyTradeCatalog.Offer tradeOffer(String id) {
		return ColonyTradeCatalog.offersView().stream()
				.filter(offer -> offer.id().equals(id))
				.findFirst()
				.orElseThrow();
	}

	private static ColonyUiSnapshot.TradeEntry tradeEntry(ColonyUiSnapshot snapshot, String id) {
		return snapshot.trades().stream()
				.filter(entry -> entry.offerId().equals(id))
				.findFirst()
				.orElse(null);
	}

	private static net.minecraft.world.level.block.Block landmarkMarker(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.MOSS_BLOCK;
			case FIRE -> Blocks.RED_TERRACOTTA;
			case CARPENTER -> Blocks.MANGROVE_ROOTS;
			case AMBER -> Blocks.CHISELED_TUFF;
		};
	}

	private static boolean isLandmarkTrail(net.minecraft.world.level.block.Block block) {
		return block == Blocks.DIRT_PATH || block == Blocks.COARSE_DIRT;
	}

	private static BlockPos firstApproachTrailBlock(GameTestHelper helper, BlockPos start, BlockPos end) {
		BlockPos anchoredStart = ColonyService.anchorToSurface(helper.getLevel(), start);
		BlockPos current = anchoredStart;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 160) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, anchoredStart) < 8 * 8 || horizontalDistanceSquared(current, end) < 13 * 13) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(helper.getLevel(), current);
			if (isLandmarkTrail(helper.getLevel().getBlockState(ground).getBlock())) {
				return ground;
			}
		}
		return null;
	}

	private static boolean hasGroundedTrailHeadDressing(GameTestHelper helper, BlockPos trailHead, BlockPos end, ColonyCulture culture) {
		int stepX = Integer.compare(end.getX(), trailHead.getX());
		int stepZ = Integer.compare(end.getZ(), trailHead.getZ());
		BlockPos left = ColonyService.anchorToSurface(helper.getLevel(), trailHead.offset(-stepZ, 0, stepX));
		BlockPos right = ColonyService.anchorToSurface(helper.getLevel(), trailHead.offset(stepZ, 0, -stepX));
		return helper.getLevel().getBlockState(left).is(Blocks.ROOTED_DIRT)
				&& helper.getLevel().getBlockState(right).is(trailHeadDressingBlock(culture));
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static net.minecraft.world.level.block.Block trailHeadDressingBlock(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.PODZOL;
			case FIRE -> Blocks.COARSE_DIRT;
			case CARPENTER -> Blocks.ROOTED_DIRT;
			case AMBER -> Blocks.PACKED_MUD;
		};
	}

	private static void assertMinimalBuildingMarker(GameTestHelper helper, BlockPos center,
			net.minecraft.world.level.block.Block expected, String label) {
		helper.assertBlockPresent(expected, center);
		BlockPos absolute = helper.absolutePos(center);
		if (!helper.getLevel().getBlockState(absolute.above()).isAir()
				|| !helper.getLevel().getBlockState(absolute.above(2)).isAir()) {
			helper.fail(label + " should remain a one-block surface marker with no old mound mass above it.");
		}
	}

	private static void assertFoodStoreProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.FOOD_CHAMBER, center);
		if (helper.getLevel().getBlockState(helper.absolutePos(center.below())).is(ModBlocks.NEST_CORE)) {
			helper.fail(label + " should be a separate surface chamber, not another queen core.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()) {
			helper.fail(label + " should expose a south-facing mouth connected to its granary interior.");
		}
		helper.assertBlockPresent(Blocks.CHEST, center.offset(-4, 1, 1));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, center.offset(-3, 1, 4));
		helper.assertBlockPresent(Blocks.COMPOSTER, center.offset(3, 1, 4));
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, center.offset(0, 1, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-4, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(4, 2, 1));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 6, 1))).getBlock())) {
			helper.fail(label + " should have a low rounded crown at y=6.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, 1))).isAir()) {
			helper.fail(label + " should end after one seven-block storey.");
		}
		int base = countMoundLayer(helper, center, 0, 12);
		int shoulder = countMoundLayer(helper, center, 4, 12);
		// Colony worker routes intentionally replace part of the ground footprint
		// after buildings are placed; blueprint unit tests own the exact base mass.
		if (base < 90 || shoulder < 20 || base <= shoulder) {
			helper.fail(label + " should be a substantial low taper, got layer masses " + base + "/" + shoulder);
		}
	}

	private static void assertMineProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.MINE_CHAMBER, center);
		if (helper.getLevel().getBlockState(helper.absolutePos(center.below())).is(ModBlocks.NEST_CORE)) {
			helper.fail(label + " should be a separate excavation mound, not another queen core.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()) {
			helper.fail(label + " should expose a south-facing mouth connected to its work room.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 0, 3))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, -1, 3))).isAir()) {
			helper.fail(label + " should cut a visible two-step pit through the chamber floor.");
		}
		helper.assertBlockPresent(ModBlocks.ORE_NODE, center.offset(0, -2, 3));
		helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE, center.offset(-2, -1, 3));
		helper.assertBlockPresent(Blocks.CHEST, center.offset(-4, 1, 1));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE_WALL, center.offset(-3, 1, 4));
		helper.assertBlockPresent(Blocks.IRON_ORE, center.offset(3, 1, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-2, 2, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(2, 2, 4));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 6, 1))).getBlock())) {
			helper.fail(label + " should have a low mineral-streaked crown at y=6.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, 1))).isAir()) {
			helper.fail(label + " should end after one seven-block storey.");
		}
		int base = countMoundLayer(helper, center, 0, 12);
		int shoulder = countMoundLayer(helper, center, 4, 12);
		if (base < 100 || shoulder < 20 || base <= shoulder) {
			helper.fail(label + " should be a substantial low excavation mound, got layer masses " + base + "/" + shoulder);
		}
	}

	private static void assertChitinFarmProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center);
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()) {
			helper.fail(label + " should expose a south-facing mouth connected to its cultivation room.");
		}
		helper.assertBlockPresent(Blocks.PACKED_MUD, center.offset(0, 0, 4));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(-4, 1, 1));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, center.offset(-3, 1, 3));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, center.offset(3, 1, 3));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, center.offset(-1, 1, 4));
		helper.assertBlockPresent(Blocks.COMPOSTER, center.offset(1, 1, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-4, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(4, 2, 1));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 5, 1))).getBlock())) {
			helper.fail(label + " should have a broad low cultivation crown at y=5.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 6, 1))).isAir()) {
			helper.fail(label + " should remain a six-block single-storey mound.");
		}
		int base = countMoundLayer(helper, center, 0, 13);
		int shoulder = countMoundLayer(helper, center, 4, 13);
		if (base < 120 || shoulder < 20 || base <= shoulder) {
			helper.fail(label + " should be a broad pit-free cultivation mound, got layer masses " + base + "/" + shoulder);
		}
	}

	private static void assertBarracksProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.BARRACKS_CHAMBER, center);
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(-2, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(2, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()) {
			helper.fail(label + " should expose a five-block-wide entrance and clear central troop aisle.");
		}
		helper.assertBlockPresent(Blocks.PACKED_MUD, center.offset(0, 0, 4));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(-6, 1, 1));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(6, 1, 1));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(-5, 1, 4));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(5, 1, 4));
		helper.assertBlockPresent(Blocks.ANVIL, center.offset(-3, 1, 5));
		helper.assertBlockPresent(Blocks.SMITHING_TABLE, center.offset(0, 1, 5));
		helper.assertBlockPresent(Blocks.TARGET, center.offset(3, 1, 5));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-6, 3, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(6, 3, 1));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(-8, 2, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(8, 2, 1))).getBlock())) {
			helper.fail(label + " should keep broad fortified shoulders on both sides of the hall.");
		}
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, 1))).getBlock())) {
			helper.fail(label + " should have a long low crown at y=7.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 8, 1))).isAir()) {
			helper.fail(label + " should remain a single-storey eight-block troop hall.");
		}
		int base = countMoundLayer(helper, center, 0, 15);
		int shoulder = countMoundLayer(helper, center, 5, 15);
		if (base < 150 || shoulder < 35 || base <= shoulder) {
			helper.fail(label + " should be a substantial elongated taper, got layer masses " + base + "/" + shoulder);
		}
	}

	private static void assertMarketProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.MARKET_CHAMBER, center);
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(-2, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(2, 2, -5))).isAir()) {
			helper.fail(label + " should expose a five-block-wide public entrance.");
		}
		for (int y = 1; y <= 5; y++) {
			if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, y, 0))).isAir()) {
				helper.fail(label + " courtyard should remain open to the sky at y=" + y + ".");
			}
		}
		helper.assertBlockPresent(Blocks.PACKED_MUD, center.offset(0, 0, 3));
		helper.assertBlockPresent(Blocks.CHEST, center.offset(-4, 1, 0));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(4, 1, 0));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, center.offset(-3, 1, 3));
		helper.assertBlockPresent(Blocks.COMPOSTER, center.offset(3, 1, 3));
		helper.assertBlockPresent(Blocks.BELL, center.offset(0, 1, 4));
		helper.assertBlockPresent(Blocks.OAK_FENCE, center.offset(-3, 1, -2));
		helper.assertBlockPresent(Blocks.OAK_FENCE, center.offset(3, 1, -2));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-3, 2, -2));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(3, 2, -2));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(-6, 2, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(6, 2, 1))).getBlock())) {
			helper.fail(label + " should retain low earth banks around both sides of the courtyard.");
		}
		int base = countMoundLayer(helper, center, 0, 13);
		int bank = countMoundLayer(helper, center, 2, 13);
		int topRing = countMoundLayer(helper, center, 3, 13);
		if (base < 140 || bank < 45 || topRing < 20 || !(base > bank && bank > topRing)) {
			helper.fail(label + " should read as a broad low roofless enclosure, got layer masses "
					+ base + "/" + bank + "/" + topRing);
		}
	}

	private static void assertPheromoneArchiveProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.PHEROMONE_ARCHIVE, center);
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, -3))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, 1))).isAir()) {
			helper.fail(label + " should expose connected facade openings and rooms on both floors.");
		}
		helper.assertBlockPresent(Blocks.CHEST, center.offset(-4, 1, 1));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.CHISELED_BOOKSHELF, center.offset(-3, 1, 4));
		helper.assertBlockPresent(Blocks.LECTERN, center.offset(3, 1, 4));
		helper.assertBlockPresent(ModBlocks.PHEROMONE_ARCHIVE, center.offset(0, 1, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-4, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(4, 2, 1));

		int upperCenterX;
		if (helper.getLevel().getBlockState(helper.absolutePos(center.offset(2, 6, 3))).is(ModBlocks.PHEROMONE_ARCHIVE)) {
			upperCenterX = 1;
		} else if (helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 6, 3))).is(ModBlocks.PHEROMONE_ARCHIVE)) {
			upperCenterX = -1;
		} else {
			helper.fail(label + " should furnish a distinct upper catalog loft.");
			return;
		}
		helper.assertBlockPresent(Blocks.BOOKSHELF, center.offset(upperCenterX - 2, 6, 1));
		helper.assertBlockPresent(Blocks.CHISELED_BOOKSHELF, center.offset(upperCenterX + 2, 6, 1));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, center.offset(upperCenterX - 1, 6, 3));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(upperCenterX - 2, 7, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(upperCenterX + 2, 7, 1));

		int stairStartX = upperCenterX > 0 ? -3 : 3;
		int stairTopX = upperCenterX > 0 ? 1 : -1;
		helper.assertBlockPresent(Blocks.MUD_BRICK_STAIRS, center.offset(stairStartX, 0, 2));
		helper.assertBlockPresent(Blocks.MUD_BRICK_STAIRS, center.offset(stairTopX, 4, 2));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(-6, 3, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(6, 3, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 10, 1))).getBlock())
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 11, 1))).isAir()) {
			helper.fail(label + " should retain a grounded lower hall and compact ten-block catalog crown.");
		}
		int base = countMoundLayer(helper, center, 0, 13);
		int secondFloor = countMoundLayer(helper, center, 5, 11);
		int upper = countMoundLayer(helper, center, 8, 9);
		if (base < 140 || secondFloor < 55 || upper < 15 || !(base > secondFloor && secondFloor > upper)) {
			helper.fail(label + " should read as a compact two-storey taper, got layer masses "
					+ base + "/" + secondFloor + "/" + upper);
		}
	}

	private static void assertArmoryProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.ARMORY, center);
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -6))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()) {
			helper.fail(label + " should expose a narrow entrance connected to the forge hall.");
		}

		int forgeCenterX;
		if (helper.getLevel().getBlockState(helper.absolutePos(center.offset(3, 1, 1))).is(ModBlocks.ARMORY)) {
			forgeCenterX = -1;
		} else if (helper.getLevel().getBlockState(helper.absolutePos(center.offset(5, 1, 1))).is(ModBlocks.ARMORY)) {
			forgeCenterX = 1;
		} else {
			helper.fail(label + " should contain a furnished forge hall.");
			return;
		}
		helper.assertBlockPresent(Blocks.ANVIL, center.offset(forgeCenterX - 4, 1, 1));
		helper.assertBlockPresent(Blocks.SMITHING_TABLE, center.offset(forgeCenterX - 3, 1, 4));
		helper.assertBlockPresent(Blocks.BLAST_FURNACE, center.offset(forgeCenterX, 1, 4));
		helper.assertBlockPresent(Blocks.GRINDSTONE, center.offset(forgeCenterX + 3, 1, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(forgeCenterX - 4, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(forgeCenterX + 4, 2, 1));

		int vaultCenterX = forgeCenterX < 0 ? 4 : -4;
		helper.assertBlockPresent(Blocks.CHEST, center.offset(vaultCenterX - 2, 1, 3));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(vaultCenterX + 2, 1, 3));
		helper.assertBlockPresent(Blocks.IRON_BLOCK, center.offset(vaultCenterX - 1, 1, 5));
		helper.assertBlockPresent(Blocks.TARGET, center.offset(vaultCenterX + 1, 1, 5));
		helper.assertBlockPresent(Blocks.IRON_BARS, center.offset(vaultCenterX, 2, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(vaultCenterX, 2, 2));
		int passageX = forgeCenterX < 0 ? 1 : -1;
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(passageX, 1, 2))).isAir()) {
			helper.fail(label + " forge and weapon vault should share a walkable passage.");
		}

		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(-7, 3, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(7, 3, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 8, 1))).getBlock())
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 9, 1))).isAir()) {
			helper.fail(label + " should retain broad armored shoulders and an eight-block crown.");
		}
		int base = countMoundLayer(helper, center, 0, 14);
		int shoulder = countMoundLayer(helper, center, 5, 13);
		int crown = countMoundLayer(helper, center, 8, 9);
		if (base < 190 || shoulder < 45 || crown < 12 || !(base > shoulder && shoulder > crown)) {
			helper.fail(label + " should read as a massive fortified taper, got layer masses "
					+ base + "/" + shoulder + "/" + crown);
		}
	}

	private static void assertDiplomacyShrineProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.DIPLOMACY_SHRINE, center);
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -6))).isAir()) {
			helper.fail(label + " should expose a narrow ceremonial entrance.");
		}
		for (int y = 1; y <= 8; y++) {
			if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, y, 1))).isAir()) {
				helper.fail(label + " sanctum should remain open to the sky at y=" + y + ".");
			}
		}
		helper.assertBlockPresent(Blocks.CHISELED_TUFF, center.offset(-3, 1, 1));
		helper.assertBlockPresent(Blocks.CHISELED_TUFF, center.offset(3, 1, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-3, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(3, 2, 1));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, center.offset(-2, 1, 3));
		helper.assertBlockPresent(Blocks.GOLD_BLOCK, center.offset(2, 1, 3));
		helper.assertBlockPresent(Blocks.CANDLE, center.offset(-2, 2, 3));
		helper.assertBlockPresent(Blocks.CANDLE, center.offset(2, 2, 3));
		helper.assertBlockPresent(ModBlocks.DIPLOMACY_SHRINE, center.offset(0, 1, 3));
		helper.assertBlockPresent(Blocks.BELL, center.offset(0, 1, 4));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(-6, 2, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(6, 2, 1))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 4, 6))).getBlock())) {
			helper.fail(label + " should retain two side horns and a rear ritual horn around the open court.");
		}
		int base = countMoundLayer(helper, center, 0, 13);
		int upper = countMoundLayer(helper, center, 6, 12);
		int crown = countMoundLayer(helper, center, 7, 11);
		if (base < 170 || upper < 12 || crown < 4 || !(base > upper && upper >= crown)) {
			helper.fail(label + " should read as a grounded open ring with a broken horn crown, got layer masses "
					+ base + "/" + upper + "/" + crown);
		}
	}

	private static void assertResinDepotProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.RESIN_DEPOT, center);
		int workshopCenterX;
		if (helper.getLevel().getBlockState(helper.absolutePos(center.offset(-1, 1, 4))).is(ModBlocks.RESIN_DEPOT)) {
			workshopCenterX = -1;
		} else if (helper.getLevel().getBlockState(helper.absolutePos(center.offset(1, 1, 4))).is(ModBlocks.RESIN_DEPOT)) {
			workshopCenterX = 1;
		} else {
			helper.fail(label + " should contain a furnished resin workshop.");
			return;
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(workshopCenterX, 2, -6))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(workshopCenterX, 2, 1))).isAir()) {
			helper.fail(label + " should expose a controlled mouth connected to the workshop.");
		}
		helper.assertBlockPresent(Blocks.BARREL, center.offset(workshopCenterX - 4, 1, 1));
		helper.assertBlockPresent(Blocks.CHEST, center.offset(workshopCenterX + 4, 1, 1));
		helper.assertBlockPresent(Blocks.CAULDRON, center.offset(workshopCenterX - 3, 1, 4));
		helper.assertBlockPresent(Blocks.CRAFTING_TABLE, center.offset(workshopCenterX + 3, 1, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(workshopCenterX - 4, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(workshopCenterX + 4, 2, 1));

		int vaultCenterX = workshopCenterX < 0 ? 4 : -4;
		helper.assertBlockPresent(Blocks.BARREL, center.offset(vaultCenterX - 2, 1, 3));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(vaultCenterX + 2, 1, 3));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, center.offset(vaultCenterX - 1, 1, 5));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, center.offset(vaultCenterX + 1, 1, 5));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, center.offset(vaultCenterX, 2, 4));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(vaultCenterX, 2, 2));
		int passageX = workshopCenterX < 0 ? 1 : -1;
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(passageX, 1, 2))).isAir()) {
			helper.fail(label + " workshop and sealed vault should share a walkable throat.");
		}

		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(-8, 2, 3))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(8, 2, 3))).getBlock())
				|| !isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, 2))).getBlock())
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 8, 2))).isAir()) {
			helper.fail(label + " should retain twin storage shoulders and a sealed seven-block crown.");
		}
		int base = countMoundLayer(helper, center, 0, 14);
		int shoulder = countMoundLayer(helper, center, 5, 13);
		int crown = countMoundLayer(helper, center, 7, 10);
		if (base < 190 || shoulder < 38 || crown < 12 || !(base > shoulder && shoulder > crown)) {
			helper.fail(label + " should read as a low twin-pod cistern, got layer masses "
					+ base + "/" + shoulder + "/" + crown);
		}
	}

	private static void assertNurseryProfile(GameTestHelper helper, BlockPos center, String label) {
		helper.assertBlockPresent(ModBlocks.NURSERY_CHAMBER, center);
		if (helper.getLevel().getBlockState(helper.absolutePos(center.below())).is(ModBlocks.NEST_CORE)) {
			helper.fail(label + " should be a separate brood chamber, not another queen core.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 1))).isAir()) {
			helper.fail(label + " should expose a south-facing mouth connected to its brood room.");
		}
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(-4, 1, 1));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, center.offset(-3, 1, 4));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, center.offset(3, 1, 4));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(-2, 1, 5));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, center.offset(2, 1, 5));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-4, 2, 1));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(4, 2, 1));
		if (!isOrganicMoundShell(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 7, 1))).getBlock())) {
			helper.fail(label + " should have a rounded brood-dome crown at y=7.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 8, 1))).isAir()) {
			helper.fail(label + " should remain a single-storey eight-block dome.");
		}
		int base = countMoundLayer(helper, center, 0, 13);
		int shoulder = countMoundLayer(helper, center, 5, 13);
		if (base < 100 || shoulder < 20 || base <= shoulder) {
			helper.fail(label + " should be a substantial bulbous taper, got layer masses " + base + "/" + shoulder);
		}
	}

	private static boolean isOrganicMoundShell(net.minecraft.world.level.block.Block block) {
		return block == ModBlocks.NEST_MOUND
				|| block == Blocks.ROOTED_DIRT
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.MUD
				|| block == Blocks.PACKED_MUD
				|| block == Blocks.MOSS_BLOCK
				|| block == Blocks.MANGROVE_ROOTS
				|| block == Blocks.STONE
				|| block == Blocks.COBBLED_DEEPSLATE
				|| block == Blocks.DEEPSLATE
				|| block == Blocks.IRON_ORE
				|| block == Blocks.BONE_BLOCK
				|| block == Blocks.HONEYCOMB_BLOCK
				|| block == Blocks.MUD_BRICKS
				|| block == Blocks.CUT_COPPER
				|| block == Blocks.CHISELED_TUFF
				|| block == Blocks.AMETHYST_BLOCK
				|| block == Blocks.TUFF
				|| block == Blocks.POLISHED_DEEPSLATE
				|| block == Blocks.BLACKSTONE
				|| block == Blocks.DEEPSLATE_IRON_ORE
				|| block == Blocks.STRIPPED_MANGROVE_WOOD
				|| block == Blocks.GOLD_BLOCK;
	}

	private static void assertTieredMoundProfile(GameTestHelper helper, BlockPos center,
			net.minecraft.world.level.block.Block expectedCenter, String label) {
		helper.assertBlockPresent(expectedCenter, center);
		helper.assertBlockPresent(ModBlocks.NEST_CORE, center.below());
		helper.assertBlockPresent(ModBlocks.NEST_MOUND, center.offset(0, 23, 1));
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, -6))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(-2, 9, -4))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(2, 15, -3))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 2))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(-2, 9, 1))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(center.offset(2, 15, 1))).isAir()) {
			helper.fail(label + " should expose carved mouths on three vertical floors.");
		}
		helper.assertBlockPresent(Blocks.CHEST, center.offset(-4, 1, 2));
		helper.assertBlockPresent(Blocks.BARREL, center.offset(4, 1, 2));
		helper.assertBlockPresent(Blocks.CRAFTING_TABLE, center.offset(3, 1, 5));
		helper.assertBlockPresent(ModBlocks.CHITIN_BED, center.offset(0, 1, 5));
		helper.assertBlockPresent(Blocks.LANTERN, center.offset(-4, 2, 2));
		helper.assertBlockPresent(Blocks.CHEST, center.offset(-5, 8, 1));
		helper.assertBlockPresent(Blocks.COMPOSTER, center.offset(1, 8, 3));
		helper.assertBlockPresent(ModBlocks.PHEROMONE_ARCHIVE, center.offset(2, 14, 3));
		helper.assertBlockPresent(Blocks.BELL, center.offset(4, 14, 2));
		helper.assertBlockPresent(Blocks.MUD_BRICK_STAIRS, center.offset(4, 0, 3));
		helper.assertBlockPresent(Blocks.MUD_BRICK_STAIRS, center.offset(-2, 6, 3));
		helper.assertBlockPresent(Blocks.MUD_BRICK_STAIRS, center.offset(-4, 7, 0));
		helper.assertBlockPresent(Blocks.MUD_BRICK_STAIRS, center.offset(1, 12, 0));
		if (!helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 24, 1))).isAir()) {
			helper.fail(label + " should end at the declared 24-block height.");
		}
		int base = countMoundLayer(helper, center, 1, 12);
		int second = countMoundLayer(helper, center, 8, 10);
		int third = countMoundLayer(helper, center, 14, 8);
		int crown = countMoundLayer(helper, center, 20, 6);
		if (base < 120 || second < 70 || third < 35 || crown < 5
				|| !(base > second && second > third && third > crown)) {
			helper.fail(label + " should form a compact four-tier taper, got layer masses "
					+ base + "/" + second + "/" + third + "/" + crown);
		}
	}

	private static int countMoundLayer(GameTestHelper helper, BlockPos center, int y, int radius) {
		int count = 0;
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				var state = helper.getLevel().getBlockState(helper.absolutePos(center.offset(x, y, z)));
				if (state.is(ModBlocks.NEST_MOUND) || state.is(ModBlocks.NEST_CORE)
						|| state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.COARSE_DIRT)
						|| state.is(Blocks.MUD) || state.is(Blocks.PACKED_MUD)
						|| state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MANGROVE_ROOTS)
						|| state.is(Blocks.STONE) || state.is(Blocks.COBBLED_DEEPSLATE)
						|| state.is(Blocks.DEEPSLATE) || state.is(Blocks.IRON_ORE)
						|| state.is(Blocks.BONE_BLOCK) || state.is(Blocks.HONEYCOMB_BLOCK)
						|| state.is(Blocks.MUD_BRICKS) || state.is(Blocks.CUT_COPPER)
						|| state.is(Blocks.CHISELED_TUFF) || state.is(Blocks.AMETHYST_BLOCK)
						|| state.is(Blocks.TUFF)) {
					count++;
				}
			}
		}
		return count;
	}

	private static void assertBlockInColumn(GameTestHelper helper, BlockPos base, net.minecraft.world.level.block.Block block, int minOffset, int maxOffset, String label) {
		for (int y = minOffset; y <= maxOffset; y++) {
			if (helper.getLevel().getBlockState(helper.absolutePos(base.above(y))).is(block)) {
				return;
			}
		}
		helper.fail("Expected " + label + " column to contain " + block.getName().getString() + " near " + base.toShortString());
	}

	private static int countItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void prepareCampusArea(GameTestHelper helper, BlockPos origin) {
		prepareCampusArea(helper, origin, 72);
	}

	private static void prepareCampusArea(GameTestHelper helper, BlockPos origin, int radius) {
		BlockPos absolute = helper.absolutePos(origin);
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (x * x + z * z > radius * radius) {
					continue;
				}
				helper.getLevel().setBlock(absolute.offset(x, -2, z), Blocks.DIRT.defaultBlockState(), 3);
				helper.getLevel().setBlock(absolute.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
				helper.getLevel().setBlock(absolute.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 26; y++) {
					helper.getLevel().setBlock(absolute.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}
}
