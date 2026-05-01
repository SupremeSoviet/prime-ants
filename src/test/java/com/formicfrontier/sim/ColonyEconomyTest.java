package com.formicfrontier.sim;

import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.world.ColonyRecurringEvents;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ColonyEconomyTest {
	@Test
	void economyConsumesUpkeepAndProducesResources() {
		ColonyData colony = baseColony();
		int beforeFood = colony.resource(ResourceType.FOOD);
		int beforeOre = colony.resource(ResourceType.ORE);

		ColonyEconomy.tick(colony);

		Assertions.assertTrue(colony.resource(ResourceType.FOOD) < beforeFood + 20);
		Assertions.assertTrue(colony.resource(ResourceType.ORE) > beforeOre);
		Assertions.assertTrue(colony.ageTicks() >= ColonyEconomy.ECONOMY_TICK_INTERVAL);
	}

	@Test
	void giantRequiresLargeEconomy() {
		ColonyData poor = baseColony();
		poor.setResource(ResourceType.FOOD, 10);
		poor.setResource(ResourceType.ORE, 0);
		poor.setResource(ResourceType.CHITIN, 0);

		Assertions.assertFalse(AntCaste.GIANT.canGrowFrom(poor));

		ColonyData rich = baseColony();
		rich.setResource(ResourceType.FOOD, 500);
		rich.setResource(ResourceType.ORE, 100);
		rich.setResource(ResourceType.CHITIN, 100);

		Assertions.assertTrue(AntCaste.GIANT.canGrowFrom(rich));
	}

	@Test
	void smallCastesKeepReadableRenderScaleWithoutChangingGameplaySize() {
		Assertions.assertEquals(1.5f, AntCaste.WORKER.height());
		Assertions.assertEquals(1.55f, AntCaste.SCOUT.height());
		Assertions.assertEquals(1.7f, AntCaste.MINER.height());

		Assertions.assertTrue(AntCaste.WORKER.visualScale() > AntCaste.WORKER.height() / 1.5f);
		Assertions.assertTrue(AntCaste.SCOUT.visualScale() > AntCaste.WORKER.visualScale());
		Assertions.assertTrue(AntCaste.MINER.visualScale() > AntCaste.SCOUT.visualScale());
		Assertions.assertTrue(AntCaste.MINER.visualScale() < AntCaste.SOLDIER.visualScale());
	}

	@Test
	void queenDeathSuspendsGrowth() {
		ColonyData colony = baseColony();
		colony.setQueenHealth(0);
		colony.setResource(ResourceType.FOOD, 500);
		colony.setResource(ResourceType.ORE, 500);
		colony.setResource(ResourceType.CHITIN, 500);
		int before = colony.population();

		ColonyEconomy.tick(colony);

		Assertions.assertEquals(before, colony.population());
		Assertions.assertTrue(colony.currentTask().contains("Queen lost"));
	}

	@Test
	void colonyDataRoundTripsThroughCodec() {
		ColonyData colony = baseColony();
		colony.addChamber(NestChamber.core(new BlockPos(1, 63, 1)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(4, 64, 4)));
		colony.progress().addBuilding(new ColonyBuilding(BuildingType.BARRACKS, new BlockPos(8, 64, 4), 1, 100, 40));
		colony.progress().addBuilding(new ColonyBuilding(BuildingType.RESIN_DEPOT, new BlockPos(12, 64, 4), 1, 45, 20));
		colony.progress().addBuilding(new ColonyBuilding(BuildingType.MINE, new BlockPos(16, 64, 4), 2, 100, 0));
		colony.progress().requests().add(new ColonyRequest(BuildingType.ARMORY, ResourceType.RESIN, 10, 4, "test request"));
		colony.progress().completeResearch(ResearchNode.CHITIN_CULTIVATION.id());
		colony.progress().addReputation(20);
		colony.setCurrentTask("Raising a named market path");
		colony.addEvent("Test event");

		var encoded = ColonyData.CODEC.encodeStart(JsonOps.INSTANCE, colony).getOrThrow();
		ColonyData decoded = ColonyData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

		Assertions.assertEquals(colony.id(), decoded.id());
		Assertions.assertEquals(colony.resource(ResourceType.FOOD), decoded.resource(ResourceType.FOOD));
		Assertions.assertEquals(colony.casteCount(AntCaste.WORKER), decoded.casteCount(AntCaste.WORKER));
		Assertions.assertEquals(colony.chambersView().size(), decoded.chambersView().size());
		Assertions.assertTrue(decoded.progress().hasCompleted(BuildingType.MARKET));
		Assertions.assertTrue(decoded.progress().buildingsView().stream().anyMatch(building -> building.visualStage() == BuildingVisualStage.DAMAGED));
		Assertions.assertTrue(decoded.progress().buildingsView().stream().anyMatch(building -> building.visualStage() == BuildingVisualStage.REPAIRING));
		Assertions.assertTrue(decoded.progress().buildingsView().stream().anyMatch(building -> building.visualStage() == BuildingVisualStage.UPGRADED));
		Assertions.assertEquals(ColonyCulture.AMBER, decoded.progress().culture());
		Assertions.assertEquals(colony.progress().name(), decoded.progress().name());
		Assertions.assertEquals(ColonyIdentity.personality(colony), ColonyIdentity.personality(decoded));
		Assertions.assertEquals("friendly", ColonyIdentity.relationshipId(decoded));
		Assertions.assertEquals("Raising a named market path", decoded.currentTask());
		Assertions.assertEquals(1, decoded.progress().requestsView().size());
		Assertions.assertTrue(decoded.progress().hasResearch(ResearchNode.CHITIN_CULTIVATION.id()));
		Assertions.assertEquals("Test event", decoded.progress().eventsView().getFirst().message());
		Assertions.assertTrue(decoded.statusText().contains("Personality:"));
		Assertions.assertTrue(decoded.statusText().contains("Relationship: friendly"));
	}

	@Test
	void buildingVisualStageDerivesFromLifecycleState() {
		Assertions.assertEquals(BuildingVisualStage.PLANNED, ColonyBuilding.planned(BuildingType.MARKET, BlockPos.ZERO).visualStage());
		Assertions.assertEquals(BuildingVisualStage.CONSTRUCTION, new ColonyBuilding(BuildingType.MARKET, BlockPos.ZERO, 1, 50, 0).visualStage());
		Assertions.assertEquals(BuildingVisualStage.COMPLETE, ColonyBuilding.complete(BuildingType.MARKET, BlockPos.ZERO).visualStage());
		Assertions.assertEquals(BuildingVisualStage.UPGRADED, new ColonyBuilding(BuildingType.MARKET, BlockPos.ZERO, 2, 100, 0).visualStage());
		Assertions.assertEquals(BuildingVisualStage.DAMAGED, new ColonyBuilding(BuildingType.MARKET, BlockPos.ZERO, 1, 100, 20).visualStage());
		Assertions.assertEquals(BuildingVisualStage.REPAIRING, new ColonyBuilding(BuildingType.MARKET, BlockPos.ZERO, 1, 50, 20).visualStage());
	}

	@Test
	void damagedBuildingCanEnterAndFinishRepair() {
		ColonyBuilding building = new ColonyBuilding(BuildingType.MARKET, BlockPos.ZERO, 1, 100, 120);

		building.beginRepair(55);
		Assertions.assertEquals(BuildingVisualStage.REPAIRING, building.visualStage());
		Assertions.assertTrue(building.repair(80));

		Assertions.assertEquals(0, building.disabledTicks());
		Assertions.assertEquals(100, building.constructionProgress());
		Assertions.assertEquals(BuildingVisualStage.COMPLETE, building.visualStage());
	}

	@Test
	void colonyUiSnapshotExposesStructuredStateWithoutStatusParsing() {
		ColonyData colony = baseColony();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_CHAMBER, new BlockPos(0, 64, 0)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(22, 64, -20)));
		colony.progress().addReputation(20);
		ColonyLogistics.requestResource(colony, BuildingType.ARMORY, ResourceType.RESIN, 12, "unit ui");
		colony.progress().completeResearch(ResearchNode.CHITIN_CULTIVATION.id());

		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Requests", "hello");

		Assertions.assertEquals(colony.id(), snapshot.colonyId());
		Assertions.assertEquals("Requests", snapshot.initialTab());
		Assertions.assertEquals("hello", snapshot.feedbackMessage());
		Assertions.assertEquals(ColonyIdentity.personality(colony).labelKey(), snapshot.personalityKey());
		Assertions.assertEquals(ColonyIdentity.personality(colony).detailKey(), snapshot.personalityDetailKey());
		Assertions.assertEquals("formic_frontier.relationship.friendly", snapshot.relationshipKey());
		Assertions.assertEquals(ColonyIdentity.relationshipColor(colony), snapshot.relationshipColor());
		Assertions.assertEquals(ResourceType.values().length, snapshot.resources().size());
		Assertions.assertEquals(AntCaste.values().length, snapshot.population().size());
		Assertions.assertFalse(snapshot.overview().isEmpty());
		Assertions.assertTrue(snapshot.overview().stream().anyMatch(entry -> entry.labelKey().equals("formic_frontier.ui.top_need")));
		Assertions.assertFalse(snapshot.buildings().isEmpty());
		Assertions.assertFalse(snapshot.requests().isEmpty());
		Assertions.assertTrue(snapshot.research().stream().anyMatch(entry -> entry.nodeId().equals(ResearchNode.CHITIN_CULTIVATION.id()) && entry.complete()));
		Assertions.assertEquals(GuideChapter.values().length, snapshot.guide().size());
		Assertions.assertTrue(snapshot.guide().stream().anyMatch(entry -> entry.chapterId().equals(GuideChapter.FIRST_STEPS.id()) && entry.unlocked()));
		Assertions.assertFalse(snapshot.currentTask().contains("==="));
	}

	@Test
	void recurringEventsAppearInOverviewWithoutShowingFoundingNoise() {
		ColonyData colony = baseColony();
		colony.addEvent("Colony founded at 0, 64, 0");
		ColonyUiSnapshot quiet = ColonyUiSnapshot.from(colony, "Overview", "");
		Assertions.assertFalse(quiet.overview().stream().anyMatch(entry -> entry.labelKey().equals("formic_frontier.ui.tab.events")));

		colony.addEvent("Recurring event: queen brood bloom raised new workers");
		ColonyUiSnapshot active = ColonyUiSnapshot.from(colony, "Overview", "");
		Assertions.assertTrue(active.overview().stream().anyMatch(entry ->
				entry.labelKey().equals("formic_frontier.ui.tab.events")
						&& entry.value().contains("queen brood bloom")));
	}

	@Test
	void contractRowsExposeDeliveryCostAndUrgentOrder() {
		ColonyData colony = baseColony();
		ColonyLogistics.requestResource(colony, BuildingType.MARKET, ResourceType.FOOD, 24, "market build");
		ColonyLogistics.requestResource(colony, BuildingType.PHEROMONE_ARCHIVE, ResourceType.RESIN, 18, "research scented_ledger");

		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Needs", "");

		Assertions.assertEquals(2, snapshot.requests().size());
		ColonyUiSnapshot.RequestEntry urgent = snapshot.requests().getFirst();
		Assertions.assertEquals("resin", urgent.resourceId());
		Assertions.assertEquals("item.formic_frontier.resin_glob", urgent.deliveryItemKey());
		Assertions.assertEquals(2, urgent.deliveryItemCount());
		Assertions.assertEquals(6, urgent.deliveryAmount());
		Assertions.assertTrue(urgent.priority() > snapshot.requests().get(1).priority());

		ColonyUiSnapshot.RequestEntry food = snapshot.requests().get(1);
		Assertions.assertEquals("food", food.resourceId());
		Assertions.assertEquals("item.minecraft.wheat", food.deliveryItemKey());
		Assertions.assertEquals(8, food.deliveryItemCount());
		Assertions.assertEquals(12, food.deliveryAmount());
	}

	@Test
	void wildColoniesExposeDiscoverableRelationship() {
		ColonyData colony = new ColonyData(7, new BlockPos(0, 64, 0));
		colony.setProgress(ColonyProgress.wild(7, ColonyCulture.LEAFCUTTER));

		Assertions.assertEquals("wild", colony.progress().faction());
		Assertions.assertFalse(colony.progress().playerAllied());
		Assertions.assertEquals("wild", ColonyIdentity.relationshipId(colony));
		Assertions.assertEquals("formic_frontier.relationship.wild", ColonyIdentity.relationshipKey(colony));
		Assertions.assertTrue(colony.statusText().contains("Relationship: wild"));
	}

	@Test
	void guideChaptersTeachBasicsAndUnlockAdvancedTopics() {
		ColonyData colony = baseColony();
		ColonyUiSnapshot starter = ColonyUiSnapshot.from(colony, "Guide", "");

		Assertions.assertTrue(starter.guide().stream().anyMatch(entry -> entry.chapterId().equals(GuideChapter.CASTES.id()) && entry.unlocked()));
		Assertions.assertTrue(starter.guide().stream().anyMatch(entry -> entry.chapterId().equals(GuideChapter.RESOURCES.id()) && entry.unlocked()));
		Assertions.assertTrue(starter.guide().stream().anyMatch(entry -> entry.chapterId().equals(GuideChapter.BUILDINGS.id()) && entry.unlocked()));
		Assertions.assertTrue(starter.guide().stream().anyMatch(entry -> entry.chapterId().equals(GuideChapter.CULTURES.id()) && entry.unlocked()));
		Assertions.assertTrue(starter.guide().stream().anyMatch(entry -> entry.chapterId().equals(GuideChapter.HELPING.id()) && entry.unlocked()));
		ColonyUiSnapshot.GuideEntry lockedRelations = starter.guide().stream().filter(entry -> entry.chapterId().equals(GuideChapter.RELATIONS.id())).findFirst().orElseThrow();
		ColonyUiSnapshot.GuideEntry lockedResearch = starter.guide().stream().filter(entry -> entry.chapterId().equals(GuideChapter.RESEARCH.id())).findFirst().orElseThrow();
		Assertions.assertFalse(lockedRelations.unlocked());
		Assertions.assertFalse(lockedResearch.unlocked());
		Assertions.assertEquals(GuideChapter.RELATIONS.lockedKey(), lockedRelations.detailKey());
		Assertions.assertEquals(GuideChapter.RESEARCH.lockedKey(), lockedResearch.detailKey());

		colony.progress().setRelation(2, DiplomacyState.NEUTRAL);
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, new BlockPos(6, 64, 6)));
		ColonyUiSnapshot advanced = ColonyUiSnapshot.from(colony, "Guide", "");

		ColonyUiSnapshot.GuideEntry openRelations = advanced.guide().stream().filter(entry -> entry.chapterId().equals(GuideChapter.RELATIONS.id())).findFirst().orElseThrow();
		ColonyUiSnapshot.GuideEntry openResearch = advanced.guide().stream().filter(entry -> entry.chapterId().equals(GuideChapter.RESEARCH.id())).findFirst().orElseThrow();
		Assertions.assertTrue(openRelations.unlocked());
		Assertions.assertTrue(openResearch.unlocked());
		Assertions.assertEquals(GuideChapter.RELATIONS.detailKey(), openRelations.detailKey());
		Assertions.assertEquals(GuideChapter.RESEARCH.detailKey(), openResearch.detailKey());
	}

	@Test
	void defensePriorityRaisesSoldierBeforeBalancedGrowth() {
		ColonyData colony = baseColony();
		colony.setResource(ResourceType.FOOD, 200);
		colony.setResource(ResourceType.ORE, 80);
		colony.setResource(ResourceType.CHITIN, 80);
		colony.setPriorities(java.util.List.of(TaskPriority.DEFENSE, TaskPriority.FOOD, TaskPriority.ORE, TaskPriority.CHITIN));
		int soldiers = colony.casteCount(AntCaste.SOLDIER);

		ColonyEconomy.tick(colony);

		Assertions.assertEquals(soldiers + 1, colony.casteCount(AntCaste.SOLDIER));
	}

	@Test
	void rankReflectsBuildingsReputationAndPopulation() {
		ColonyData colony = baseColony();
		Assertions.assertEquals(ColonyRank.OUTPOST, ColonyRank.current(colony));

		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(3, 64, 3)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.DIPLOMACY_SHRINE, new BlockPos(4, 64, 3)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.WATCH_POST, new BlockPos(5, 64, 3)));
		colony.progress().addReputation(80);
		colony.addCaste(AntCaste.WORKER, 15);
		colony.addCaste(AntCaste.SOLDIER, 8);

		Assertions.assertTrue(ColonyRank.current(colony).ordinal() >= ColonyRank.HIVE.ordinal());
	}

	@Test
	void diplomacyActionsMoveRelationsWithCostsAndRankGates() {
		Assertions.assertEquals(DiplomacyState.RIVAL, DiplomacyAction.ENVOY.apply(DiplomacyState.WAR));
		Assertions.assertEquals(DiplomacyState.ALLY, DiplomacyAction.TRIBUTE.apply(DiplomacyState.NEUTRAL));
		Assertions.assertEquals(DiplomacyState.NEUTRAL, DiplomacyAction.TRUCE.apply(DiplomacyState.WAR));
		Assertions.assertEquals(DiplomacyState.NEUTRAL, DiplomacyAction.TRUCE.apply(DiplomacyState.RIVAL));
		Assertions.assertEquals(DiplomacyState.WAR, DiplomacyAction.WAR_PACT.apply(DiplomacyState.ALLY));
		Assertions.assertTrue(DiplomacyAction.TRUCE.tokenCost() > DiplomacyAction.ENVOY.tokenCost());
		Assertions.assertEquals(ColonyRank.CITADEL, DiplomacyAction.WAR_PACT.minRank());
	}

	@Test
	void logisticsRequestsConsumeResourcesUntilFulfilled() {
		ColonyData colony = baseColony();
		colony.setResource(ResourceType.RESIN, 6);
		ColonyLogistics.requestResource(colony, BuildingType.RESIN_DEPOT, ResourceType.RESIN, 3, "unit logistics");

		ColonyLogistics.tick(colony);

		Assertions.assertEquals(3, colony.resource(ResourceType.RESIN));
		Assertions.assertTrue(colony.progress().requestsView().getFirst().complete());
	}

	@Test
	void requestsExposePlayerContractsWithRewardsAndReputation() {
		ColonyData colony = baseColony();
		ColonyLogistics.requestResource(colony, BuildingType.PHEROMONE_ARCHIVE, ResourceType.RESIN, 12, "research scented_ledger");

		ColonyContract contract = ColonyLogistics.contracts(colony).getFirst();

		Assertions.assertFalse(contract.id().isBlank());
		Assertions.assertEquals(ResourceType.RESIN, contract.resource());
		Assertions.assertEquals(12, contract.resourceCost());
		Assertions.assertTrue(contract.priority() >= 3);
		Assertions.assertTrue(contract.rewardTokens() > 0);
		Assertions.assertTrue(contract.reputationDelta() > 0);

		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), 12);

		Assertions.assertTrue(result.success());
		Assertions.assertTrue(result.complete());
		Assertions.assertEquals(12, result.delivered());
		Assertions.assertTrue(result.rewardTokens() > 0);
		Assertions.assertTrue(colony.progress().reputation() > 0);
		Assertions.assertTrue(colony.progress().requestsView().isEmpty());
		Assertions.assertTrue(colony.currentTask().contains("Contract fulfilled"));
	}

	@Test
	void famineRequestsDoNotDrainEmergencyFoodStores() {
		ColonyData colony = baseColony();
		colony.setResource(ResourceType.FOOD, 4);
		ColonyLogistics.requestResource(colony, BuildingType.FOOD_STORE, ResourceType.FOOD, 36, ColonyRecurringEvents.FAMINE_REASON);

		ColonyLogistics.tick(colony);

		Assertions.assertEquals(4, colony.resource(ResourceType.FOOD));
		Assertions.assertEquals(0, colony.progress().requestsView().getFirst().fulfilled());
	}

	@Test
	void famineContractsRestoreFoodWhenPlayerHelps() {
		ColonyData colony = baseColony();
		colony.setResource(ResourceType.FOOD, 4);
		ColonyLogistics.requestResource(colony, BuildingType.FOOD_STORE, ResourceType.FOOD, 36, ColonyRecurringEvents.FAMINE_REASON);
		ColonyContract contract = ColonyLogistics.contracts(colony).getFirst();

		Assertions.assertTrue(contract.priority() >= 5);
		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), 12);

		Assertions.assertTrue(result.success());
		Assertions.assertFalse(result.complete());
		Assertions.assertEquals(12, result.delivered());
		Assertions.assertEquals(16, colony.resource(ResourceType.FOOD));
		Assertions.assertEquals(12, colony.progress().requestsView().getFirst().fulfilled());
	}

	@Test
	void completedResearchContractStartsResearchWithDeliveredMaterials() {
		ColonyData colony = baseColony();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, new BlockPos(6, 64, 6)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(22, 64, -20)));
		colony.setResource(ResourceType.KNOWLEDGE, ResearchNode.SCENTED_LEDGER.cost(ResourceType.KNOWLEDGE));
		colony.setResource(ResourceType.RESIN, 0);

		Assertions.assertFalse(ColonyLogistics.startResearch(colony, ResearchNode.SCENTED_LEDGER.id()).started());
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(entry -> entry.reason().equals("research " + ResearchNode.SCENTED_LEDGER.id()))
				.filter(entry -> entry.resource() == ResourceType.RESIN)
				.findFirst()
				.orElseThrow();

		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());

		Assertions.assertTrue(result.success());
		Assertions.assertTrue(result.complete());
		Assertions.assertEquals("Research started: Scented Ledger", result.payoffMessage());
		Assertions.assertTrue(colony.progress().activeResearch().isPresent());
		Assertions.assertEquals(ResearchNode.SCENTED_LEDGER.id(), colony.progress().activeResearch().get().nodeId());
		Assertions.assertEquals(0, colony.resource(ResourceType.RESIN));
		Assertions.assertEquals(0, colony.resource(ResourceType.KNOWLEDGE));
		Assertions.assertTrue(colony.progress().requestsView().isEmpty());
		Assertions.assertTrue(colony.currentTask().contains("Researching"));
	}

	@Test
	void researchRequiresArchiveResourcesAndCompletes() {
		ColonyData colony = baseColony();

		Assertions.assertFalse(ColonyLogistics.startResearch(colony, ResearchNode.RESIN_MASONRY.id()).started());

		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, new BlockPos(6, 64, 6)));
		colony.setResource(ResourceType.KNOWLEDGE, 40);
		colony.setResource(ResourceType.RESIN, 40);
		colony.setResource(ResourceType.ORE, 40);

		Assertions.assertTrue(ColonyLogistics.startResearch(colony, ResearchNode.RESIN_MASONRY.id()).started());
		for (int i = 0; i < 6; i++) {
			ColonyLogistics.tick(colony);
		}

		Assertions.assertTrue(colony.progress().hasResearch(ResearchNode.RESIN_MASONRY.id()));
		Assertions.assertTrue(colony.progress().activeResearch().isEmpty());
	}

	@Test
	void cultureModifiersAffectEconomyWithoutMoodState() {
		ColonyData amber = baseColony();
		ColonyData leafcutter = baseColony();
		leafcutter.setProgress(ColonyProgress.rival(2, ColonyCulture.LEAFCUTTER));
		leafcutter.addCaste(AntCaste.QUEEN, 1);
		leafcutter.addCaste(AntCaste.WORKER, 3);
		leafcutter.addCaste(AntCaste.MINER, 2);
		leafcutter.addCaste(AntCaste.SOLDIER, 2);
		leafcutter.setResource(ResourceType.FOOD, 120);
		leafcutter.setResource(ResourceType.ORE, 20);
		leafcutter.setResource(ResourceType.CHITIN, 24);

		ColonyEconomy.tick(amber);
		ColonyEconomy.tick(leafcutter);

		Assertions.assertTrue(leafcutter.resource(ResourceType.FUNGUS) > amber.resource(ResourceType.FUNGUS));
		Assertions.assertFalse(leafcutter.statusText().toLowerCase(java.util.Locale.ROOT).contains("mood"));
	}

	@Test
	void cultureSignatureBuildingsChangeEconomyOutputs() {
		ColonyData amber = cultureColony(ColonyCulture.AMBER);
		amber.progress().addBuilding(ColonyBuilding.complete(BuildingType.DIPLOMACY_SHRINE, new BlockPos(4, 64, 4)));
		amber.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(8, 64, 4)));
		ColonyEconomy.tick(amber);
		Assertions.assertTrue(amber.resource(ResourceType.KNOWLEDGE) >= 2);

		ColonyData leafcutter = cultureColony(ColonyCulture.LEAFCUTTER);
		ColonyData leafcutterGarden = cultureColony(ColonyCulture.LEAFCUTTER);
		leafcutterGarden.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(4, 64, 4)));
		ColonyEconomy.tick(leafcutter);
		ColonyEconomy.tick(leafcutterGarden);
		Assertions.assertTrue(leafcutterGarden.resource(ResourceType.FOOD) > leafcutter.resource(ResourceType.FOOD));
		Assertions.assertTrue(leafcutterGarden.resource(ResourceType.FUNGUS) > leafcutter.resource(ResourceType.FUNGUS));

		ColonyData fire = cultureColony(ColonyCulture.FIRE);
		fire.progress().addBuilding(ColonyBuilding.complete(BuildingType.ARMORY, new BlockPos(4, 64, 4)));
		fire.progress().addBuilding(ColonyBuilding.complete(BuildingType.WATCH_POST, new BlockPos(8, 64, 4)));
		ColonyEconomy.tick(fire);
		Assertions.assertTrue(fire.resource(ResourceType.VENOM) >= 4);

		ColonyData carpenter = cultureColony(ColonyCulture.CARPENTER);
		carpenter.progress().addBuilding(ColonyBuilding.complete(BuildingType.RESIN_DEPOT, new BlockPos(4, 64, 4)));
		carpenter.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, new BlockPos(8, 64, 4)));
		ColonyEconomy.tick(carpenter);
		Assertions.assertTrue(carpenter.resource(ResourceType.RESIN) >= 8);
	}

	@Test
	void cultureStarterQueuesExposeDistinctProgressionPaths() {
		Assertions.assertEquals(BuildingType.DIPLOMACY_SHRINE, ColonyCulture.AMBER.starterQueue().getFirst());
		Assertions.assertEquals(BuildingType.FUNGUS_GARDEN, ColonyCulture.LEAFCUTTER.starterQueue().getFirst());
		Assertions.assertEquals(BuildingType.WATCH_POST, ColonyCulture.FIRE.starterQueue().getFirst());
		Assertions.assertEquals(BuildingType.RESIN_DEPOT, ColonyCulture.CARPENTER.starterQueue().getFirst());
		Assertions.assertTrue(ColonyCulture.LEAFCUTTER.starterQueue().contains(BuildingType.PHEROMONE_ARCHIVE));
		Assertions.assertTrue(ColonyCulture.FIRE.starterQueue().contains(BuildingType.ARMORY));
	}

	@Test
	void endgameProjectsAppearAsNamedBuildingsInUiSnapshot() {
		ColonyData colony = baseColony();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.GREAT_MOUND, new BlockPos(0, 64, 0)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_VAULT, new BlockPos(0, 64, 0)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, new BlockPos(24, 64, -18)));

		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Buildings", "");

		Assertions.assertTrue(snapshot.buildings().stream().anyMatch(entry ->
				entry.typeId().equals("great_mound")
						&& entry.labelKey().equals("formic_frontier.building.great_mound")));
		Assertions.assertTrue(snapshot.buildings().stream().anyMatch(entry ->
				entry.typeId().equals("queen_vault")
						&& entry.labelKey().equals("formic_frontier.building.queen_vault")));
		Assertions.assertTrue(snapshot.buildings().stream().anyMatch(entry ->
				entry.typeId().equals("trade_hub")
						&& entry.labelKey().equals("formic_frontier.building.trade_hub")));
	}

	private static ColonyData baseColony() {
		ColonyData colony = new ColonyData(1, new BlockPos(0, 64, 0));
		colony.setResource(ResourceType.FOOD, 120);
		colony.setResource(ResourceType.ORE, 20);
		colony.setResource(ResourceType.CHITIN, 24);
		colony.addCaste(AntCaste.QUEEN, 1);
		colony.addCaste(AntCaste.WORKER, 3);
		colony.addCaste(AntCaste.MINER, 2);
		colony.addCaste(AntCaste.SOLDIER, 2);
		return colony;
	}

	private static ColonyData cultureColony(ColonyCulture culture) {
		ColonyData colony = baseColony();
		colony.setProgress(ColonyProgress.rival(culture.ordinal() + 2, culture));
		colony.setResource(ResourceType.FOOD, 120);
		colony.setResource(ResourceType.ORE, 20);
		colony.setResource(ResourceType.CHITIN, 24);
		return colony;
	}
}
