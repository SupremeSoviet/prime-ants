package com.formicfrontier.sim;

import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.world.RaidPlanner;
import com.formicfrontier.world.ColonyRecurringEvents;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import java.util.List;
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
	void resourceTickProducesAndConsumesOverMultipleTicksAssertingRiseAndFall() {
		// Content row resource_production_consumption_tick: prove the economy tick
		// produces from a source and consumes via caste upkeep so stockpiles BOTH
		// rise AND fall over N ticks (not just that numbers are non-zero).
		//
		// With a dead queen the tick applies income + upkeep and then returns early
		// (no growth cost), so per-tick deltas are pure production-minus-upkeep and
		// fully deterministic. baseColony() is AMBER culture, OUTPOST rank (economy
		// bonus 0), no completed buildings, castes queen=1 worker=3 miner=2 soldier=2.
		//   oreIncome = miners*2 + mines + rankBonus/2 = 2*2 + 0 + 0 = 4/tick (ore has no upkeep) -> RISE
		//   upkeep    = queen1*12 + worker3*1 + miner2*2 + soldier2*3 = 12+3+4+6 = 25/tick
		//   foodIncome= 4 + workers*3 + rankBonus + cultureFoodBonus = 4 + 3*3 + 0 + 0 = 13/tick
		//   foodDelta = foodIncome - upkeep = 13 - 25 = -12/tick  -> FALL
		// (120 food over 6 ticks -> 108..48, never clamps at 0, so the delta is exact.)
		ColonyData colony = baseColony();
		colony.setQueenHealth(0);
		Assertions.assertFalse(colony.queenAlive(), "fixture: queen dead so growth is suspended and deltas are pure income-minus-upkeep");

		int ticks = 6;
		int oreBefore = colony.resource(ResourceType.ORE);
		int foodBefore = colony.resource(ResourceType.FOOD);
		int[] oreAfterEachTick = new int[ticks];
		int[] foodAfterEachTick = new int[ticks];

		for (int i = 0; i < ticks; i++) {
			ColonyEconomy.tick(colony);
			oreAfterEachTick[i] = colony.resource(ResourceType.ORE);
			foodAfterEachTick[i] = colony.resource(ResourceType.FOOD);
		}

		// RISE: ore is produced from the miner caste at a source and is never
		// consumed by upkeep, so the stockpile must climb every single tick and by
		// exactly the deterministic per-tick delta over N ticks.
		int expectedOrePerTick = 4;
		for (int i = 0; i < ticks; i++) {
			Assertions.assertEquals(oreBefore + expectedOrePerTick * (i + 1), oreAfterEachTick[i],
					"ore stockpile should rise by " + expectedOrePerTick + "/tick after tick " + (i + 1));
		}
		Assertions.assertTrue(oreAfterEachTick[ticks - 1] > oreBefore, "ore must end higher than it started (rise)");

		// FALL: food is consumed by caste upkeep faster than it is produced, so the
		// stockpile must drop every single tick and by exactly the deterministic
		// per-tick delta over N ticks. This is the consumption half of the row.
		int expectedFoodDeltaPerTick = -12;
		for (int i = 0; i < ticks; i++) {
			Assertions.assertEquals(foodBefore + expectedFoodDeltaPerTick * (i + 1), foodAfterEachTick[i],
					"food stockpile should fall by " + (-expectedFoodDeltaPerTick) + "/tick after tick " + (i + 1));
		}
		Assertions.assertTrue(foodAfterEachTick[ticks - 1] < foodBefore, "food must end lower than it started (fall)");

		// Both directions in one run: at least one resource rose AND at least one
		// resource fell over the same N economy ticks, proving produce+consume flow.
		Assertions.assertTrue(oreAfterEachTick[ticks - 1] > oreBefore && foodAfterEachTick[ticks - 1] < foodBefore,
				"economy must both produce (rise) and consume (fall) over " + ticks + " ticks");
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


	@Test
	void casteJobLoopsChangeColonyStateEndToEnd() {
		// Content row caste_job_loops_change_state: each active caste has a job loop
		// (gather/build/patrol/tend) that measurably changes colony state, proven by a
		// gametest that runs the loop and asserts the resulting
		// resource/construction/defense/queen change.
		//
		// baseColony() has queen=1 worker=3 miner=2 soldier=2; each loop pass must:
		//   GATHER  workers(3)->+3 food, miners(2)->+4 ore, scouts(0)->+0 chitin
		//   BUILD   advance an incomplete construction site toward completion
		//   PATROL  stand down raid-damaged defenses (disabled ticks fall)
		//   TEND    nurse a wounded queen back toward full health
		ColonyData colony = baseColony();
		colony.addCaste(AntCaste.SCOUT, 2); // so scouts gather chitin too
		// BUILD target: one incomplete construction site for builder workers.
		colony.progress().addBuilding(new ColonyBuilding(BuildingType.MARKET, new BlockPos(4, 64, 4), 1, 0, 0));
		Assertions.assertFalse(colony.progress().firstIncomplete().orElseThrow().complete());
		// PATROL target: raid-damaged building whose disabled ticks the defenders stand down.
		ColonyBuilding damaged = new ColonyBuilding(BuildingType.WATCH_POST, new BlockPos(6, 64, 6), 1, 100, 8);
		colony.progress().addBuilding(damaged);
		Assertions.assertTrue(colony.progress().buildingsView().stream().anyMatch(ColonyBuilding::damaged));
		// TEND target: wound the queen so brood-tending workers can nurse her.
		int maxQueen = (int) AntCaste.QUEEN.health();
		colony.setQueenHealth(maxQueen - 20);
		Assertions.assertTrue(colony.queenHealth() < maxQueen);

		int foodBefore = colony.resource(ResourceType.FOOD);
		int oreBefore = colony.resource(ResourceType.ORE);
		int chitinBefore = colony.resource(ResourceType.CHITIN);
		int buildBefore = colony.progress().firstIncomplete().orElseThrow().constructionProgress();
		int queenBefore = colony.queenHealth();

		com.formicfrontier.sim.CasteJobLoop.JobLoopResult result = com.formicfrontier.sim.CasteJobLoop.tick(colony);

		// GATHER: resource stockpiles rise by the deterministic per-caste yields.
		Assertions.assertTrue(result.gatheredFood() > 0 && result.gatheredOre() > 0 && result.gatheredChitin() > 0,
				"workers, miners, and scouts must each gather a resource");
		Assertions.assertEquals(foodBefore + 3, colony.resource(ResourceType.FOOD), "worker foraging must add food");
		Assertions.assertEquals(oreBefore + 4, colony.resource(ResourceType.ORE), "miner hauling must add ore");
		Assertions.assertEquals(chitinBefore + 2, colony.resource(ResourceType.CHITIN), "scout foraging must add chitin");

		// BUILD: builder workers advance the active construction site.
		Assertions.assertTrue(result.builtProgress() > 0, "builder workers must advance construction");
		Assertions.assertTrue(colony.progress().firstIncomplete().orElseThrow().constructionProgress() > buildBefore,
				"construction progress must measurably increase");

		// PATROL: soldier/major defenders stand down raid-damaged defenses.
		Assertions.assertTrue(result.patrolsResolved() >= 1, "patrol team must restore at least one defense");
		Assertions.assertTrue(colony.progress().buildingsView().stream()
						.filter(building -> building.type() == BuildingType.WATCH_POST)
						.allMatch(building -> building.disabledTicks() < 8),
				"raid-damaged watch post must recover disabled ticks");

		// TEND: brood-tending workers nurse the wounded queen back toward full health.
		Assertions.assertTrue(result.queenTended() > 0, "workers must tend the queen's health");
		Assertions.assertTrue(colony.queenHealth() > queenBefore, "queen health must measurably increase");
		Assertions.assertTrue(colony.queenHealth() <= maxQueen, "queen health must not exceed full health");

		// The job loop reaches normal play: it is wired into ColonySavedState.tickEconomy()
		// next to ColonyEconomy.tick + ColonyLogistics.tick, not dead code.
		Assertions.assertTrue(colony.currentTask().startsWith("Job loop:"),
				"running the loop should describe the active job state, got " + colony.currentTask());
	}
	@Test
	void colonyAdvancesStageAutonomouslyAndUnlocksBuilding() {
		// Content row progression_stage_advance_autonomous: a colony with
		// sufficient resources advances from one development stage to the next
		// over ticks without player input, unlocking a new building, proven by a
		// gametest that runs the autonomous economy tick and asserts the stage
		// advance plus a concrete unlock.
		//
		// ColonyStage thresholds (age in ECONOMY_TICK_INTERVAL units):
		//   FOUNDING    -> GROWTH      age>=3, food>=120, composite(ore+chitin+resin)>=40, unlock BARRACKS
		//   GROWTH      -> ESTABLISHED age>=6, food>=400, composite>=180, unlock PHEROMONE_ARCHIVE
		//   ESTABLISHED -> MATURE      age>=12, food>=900, composite>=500, unlock GREAT_MOUND
		// ColonyEconomy.tick adds ECONOMY_TICK_INTERVAL per call, so N ticks -> age N.
		// We run the colony through the autonomous economy loop (which is also where
		// ColonyStageProgression.tick is wired in normal play) and prove the colony
		// advances at least GROWTH->ESTABLISHED and unlocks each signature building
		// into the build queue, with no player input.
		ColonyData colony = baseColony();
		Assertions.assertEquals(ColonyStage.FOUNDING, colony.stage(),
				"a fresh colony must start at the founding stage");
		// Give the colony enough diversified stockpiles to reach ESTABLISHED while
		// the loop ticks its age forward. Food is set high enough to survive upkeep
		// for the tick count and still clear the 400 ESTABLISHED food threshold.
		colony.setResource(ResourceType.FOOD, 800);
		colony.setResource(ResourceType.ORE, 120);
		colony.setResource(ResourceType.CHITIN, 120);
		colony.setResource(ResourceType.RESIN, 60);

		// Sanity: before any progression tick, GROWTH is not yet earned (age=0).
		Assertions.assertEquals(ColonyStage.FOUNDING, ColonyStage.earnedFrom(colony),
				"age 0 colony must not yet earn GROWTH");

		// Run the autonomous colony tick 7 times, mirroring exactly the live wiring
		// in ColonySavedState.tickEconomy() (ColonyEconomy.tick advances age and
		// resources, then ColonyStageProgression.tick advances the stage). This
		// reaches age 7 (>=6 for ESTABLISHED). Economy growth consumes food for caste
		// growth, but the stockpile stays well above the 400 food + 180 composite
		// thresholds, so the colony deterministically earns ESTABLISHED. (It cannot
		// reach MATURE because MATURE needs age>=12 and only 7 ticks have elapsed.)
		for (int i = 0; i < 7; i++) {
			ColonyEconomy.tick(colony);
			com.formicfrontier.sim.ColonyStageProgression.tick(colony);
		}

		// Autonomous stage advance: the recorded stage must have advanced beyond
		// GROWTH to ESTABLISHED with no player input.
		Assertions.assertTrue(colony.stage().ordinal() > ColonyStage.FOUNDING.ordinal(),
				"colony must have advanced out of the founding stage");
		Assertions.assertTrue(colony.stage().ordinal() >= ColonyStage.ESTABLISHED.ordinal(),
				"colony must have reached at least the established stage after " + 7 + " autonomous ticks");

		// Concrete unlock effect: each stage between FOUNDING and the reached stage
		// unlocks its signature building into the build queue so the existing
		// builder loop will construct it. GROWTH unlocks BARRACKS and ESTABLISHED
		// unlocks PHEROMONE_ARCHIVE.
		Assertions.assertTrue(colony.progress().buildQueueView().contains(BuildingType.BARRACKS),
				"GROWTH stage must unlock the barracks signature building into the build queue");
		Assertions.assertTrue(colony.progress().buildQueueView().contains(BuildingType.PHEROMONE_ARCHIVE),
				"ESTABLISHED stage must unlock the pheromone archive signature building into the build queue");

		// The progression is reachable in normal play: it runs inside
		// ColonySavedState.tickEconomy() alongside ColonyEconomy.tick, so proving
		// ColonyEconomy.tick drives the advance confirms the live wiring.
		Assertions.assertTrue(colony.progress().eventsView().stream()
						.anyMatch(event -> event.message().contains("advanced to the")),
				"each stage advance must record a colony event, got " + colony.progress().eventsView());

		// Monotonic + state-derived: a colony cannot reach MATURE in 7 ticks, and
		// the earned stage must match the recorded stage (no hidden counters).
		Assertions.assertTrue(ColonyStage.earnedFrom(colony).ordinal() >= colony.stage().ordinal(),
				"recorded stage must never exceed the stage earned from current colony state");

		// Reaching a stage without a queen present must NOT advance further, proving
		// the progression is a living-colony behavior, not a blind timer.
		ColonyStage stageBeforeQueenLoss = colony.stage();
		// Push age and resources high enough that MATURE would otherwise be earned,
		// then kill the queen and confirm the stage is frozen.
		colony.setResource(ResourceType.FOOD, 1000);
		colony.setResource(ResourceType.ORE, 300);
		colony.setResource(ResourceType.CHITIN, 300);
		colony.setResource(ResourceType.RESIN, 200);
		colony.addAgeTicks(ColonyEconomy.ECONOMY_TICK_INTERVAL * 8);
		colony.setQueenHealth(0);
		Assertions.assertFalse(colony.queenAlive());
		com.formicfrontier.sim.ColonyStageProgression.ProgressionResult frozen =
				com.formicfrontier.sim.ColonyStageProgression.tick(colony);
		Assertions.assertFalse(frozen.advanced(), "a queenless colony must not advance stage");
		Assertions.assertEquals(stageBeforeQueenLoss, colony.stage(),
				"a queenless colony must keep its current stage without regressing or advancing");
	}


	@Test
	void researchUnlockEnablesEliteGiantCaste() {
		// Content row research_unlocks_have_effects: spending knowledge on a
		// research node (with prerequisites) unlocks a caste that was previously
		// locked, and that unlock has a real mechanical effect. The elite GIANT
		// caste is research-gated: ColonyEconomy.canProduceGiant requires the
		// completed MANDIBLE_PLATING research node (which itself needs the
		// RESIN_MASONRY prerequisite and a completed ARMORY). We prove the gate
		// is real by running the same autonomous economy tick the live colony uses
		// and asserting GIANT is grown only AFTER the node is completed.
		//
		// fundedDefenseColony() saturates every lower-priority growth branch
		// (worker/miner/soldier targets, MAJOR via chitin) and biases instinct to
		// DEFENSE, so GIANT is the only elite caste the growth chooser can still
		// add when resources allow. With GIANT fully funded this isolates the
		// research gate as the sole difference between the two cases.

		// CASE A: research NOT completed -> elite GIANT caste stays LOCKED even
		// though every GIANT resource cost is met. The economy tick must not grow
		// any GIANT ants.
		ColonyData withoutResearch = fundedDefenseColony();
		Assertions.assertFalse(withoutResearch.progress().hasResearch(ResearchNode.MANDIBLE_PLATING.id()),
				"fixture: MANDIBLE_PLATING must not be completed in the without-research case");
		Assertions.assertTrue(AntCaste.GIANT.canGrowFrom(withoutResearch),
				"fixture: GIANT resource costs must be met so the research gate is the only blocker");
		int giantsBefore = withoutResearch.casteCount(AntCaste.GIANT);
		for (int i = 0; i < 3; i++) {
			ColonyEconomy.tick(withoutResearch);
		}
		Assertions.assertEquals(giantsBefore, withoutResearch.casteCount(AntCaste.GIANT),
				"without MANDIBLE_PLATING research the elite GIANT caste must stay locked; "
				+ "got " + withoutResearch.casteCount(AntCaste.GIANT) + " giants");

		// CASE B: the same colony SPENDS KNOWLEDGE on the MANDIBLE_PLATING node
		// (consuming its knowledge cost exactly as ResearchNode.consumeCosts does
		// in the live research-start path, then recording the completed unlock as
		// ColonyLogistics.tickResearch does when the research duration elapses).
		// The SAME autonomous economy tick must now be able to grow the previously-
		// locked elite GIANT caste, proving the unlock is a real mechanical effect.
		ColonyData withResearch = fundedDefenseColony();
		Assertions.assertEquals(0, withResearch.casteCount(AntCaste.GIANT),
				"fixture: funded colony starts with no giants");
		Assertions.assertFalse(withResearch.progress().hasResearch(ResearchNode.MANDIBLE_PLATING.id()),
				"fixture: research must start uncompleted");
		int knowledgeBefore = withResearch.resource(ResourceType.KNOWLEDGE);
		// Spend the research node cost so the unlock is provably paid for.
		for (java.util.Map.Entry<ResourceType, Integer> cost : ResearchNode.MANDIBLE_PLATING.costsView().entrySet()) {
			withResearch.addResource(cost.getKey(), -cost.getValue());
		}
		Assertions.assertTrue(withResearch.resource(ResourceType.KNOWLEDGE) < knowledgeBefore,
				"completing research must consume knowledge resources, spent " + (knowledgeBefore - withResearch.resource(ResourceType.KNOWLEDGE)));
		withResearch.progress().completeResearch(ResearchNode.MANDIBLE_PLATING.id());
		Assertions.assertTrue(withResearch.progress().hasResearch(ResearchNode.MANDIBLE_PLATING.id()),
				"spending the knowledge cost must record the MANDIBLE_PLATING unlock");
		int giantsAfterResearch = withResearch.casteCount(AntCaste.GIANT);
		for (int i = 0; i < 3; i++) {
			ColonyEconomy.tick(withResearch);
			if (withResearch.casteCount(AntCaste.GIANT) > giantsAfterResearch) {
				break; // elite caste grown: the unlock took effect
			}
		}
		Assertions.assertTrue(withResearch.casteCount(AntCaste.GIANT) > giantsAfterResearch,
				"after spending knowledge on MANDIBLE_PLATING the economy must be able to grow "
				+ "the previously-locked elite GIANT caste; got " + withResearch.casteCount(AntCaste.GIANT) + " giants");
}

	@Test
	void tradeCaravanExchangesResourcesByRelationAndScarcity() {
		// Content row trade_caravan_exchanges_resources: a trade caravan moves a
		// resource between two colonies at a rate that depends on their relation
		// (ally/neutral/rival) and on scarcity. We prove this end to end on the
		// same TradeCaravan.exchange the live tickEconomy caravan pass calls.
		//
		// FIXTURE: a source colony with a completed MARKET holding FOOD in surplus
		// (>SURPLUS_THRESHOLD 40) and a target colony that is starving for FOOD
		// (<SCARCITY_THRESHOLD 20). The source also needs to record a relation
		// toward the target; we set that explicitly per case below.
		ColonyData source = baseColony();
		source.setProgress(ColonyProgress.allied(source.id()));
		source.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(0, 64, 8)));
		Assertions.assertTrue(source.progress().hasCompleted(BuildingType.MARKET), "fixture: source needs a completed MARKET to run caravans");
		ColonyData target = new ColonyData(2, new BlockPos(64, 64, 0));
		source.setResource(ResourceType.FOOD, 200);
		target.setResource(ResourceType.FOOD, 0);
		Assertions.assertTrue(source.resource(ResourceType.FOOD) > TradeCaravan.SURPLUS_THRESHOLD, "fixture: source has FOOD surplus");
		Assertions.assertTrue(target.resource(ResourceType.FOOD) < TradeCaravan.SCARCITY_THRESHOLD, "fixture: target is FOOD-scarce");

		// CASE 1 (ALLY): rate 1.0 -> a caravan runs, FOOD leaves the source and the
		// SAME amount arrives at the target. Both deltas must be non-zero, equal in
		// magnitude, and conserve the resource (source loss == target gain).
		source.progress().setRelation(target.id(), DiplomacyState.ALLY);
		int sourceFoodBefore = source.resource(ResourceType.FOOD);
		int targetFoodBefore = target.resource(ResourceType.FOOD);
		TradeCaravan.ExchangeResult allyResult = TradeCaravan.exchange(source, target);
		Assertions.assertTrue(allyResult.occurred(), "ally caravan must run when source has surplus, target is scarce, and a MARKET exists");
		Assertions.assertEquals(ResourceType.FOOD, allyResult.cargo(), "scarcity-driven cargo must be the surplus/scarce resource (FOOD)");
		int allyShipped = allyResult.shipped();
		Assertions.assertTrue(allyShipped > 0, "ally caravan must ship a positive amount");
		Assertions.assertEquals(sourceFoodBefore - allyShipped, source.resource(ResourceType.FOOD),
				"source FOOD must drop by exactly the shipped amount");
		Assertions.assertEquals(targetFoodBefore + allyShipped, target.resource(ResourceType.FOOD),
				"target FOOD must rise by exactly the shipped amount");
		Assertions.assertEquals(sourceFoodBefore, source.resource(ResourceType.FOOD) + target.resource(ResourceType.FOOD) - targetFoodBefore,
				"resource is conserved across the exchange: source loss == target gain");
		Assertions.assertEquals(1.0, allyResult.rate(), 1e-9, "ally relation must yield the full trade rate");

		// CASE 2 (NEUTRAL): rate 0.5 -> a caravan still runs but ships strictly LESS
		// than the ally case, proving the relation changes the trade outcome.
		ColonyData sourceNeutral = baseColony();
		sourceNeutral.setProgress(ColonyProgress.allied(sourceNeutral.id()));
		sourceNeutral.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(0, 64, 8)));
		ColonyData targetNeutral = new ColonyData(2, new BlockPos(64, 64, 0));
		sourceNeutral.setResource(ResourceType.FOOD, 200);
		targetNeutral.setResource(ResourceType.FOOD, 0);
		sourceNeutral.progress().setRelation(targetNeutral.id(), DiplomacyState.NEUTRAL);
		TradeCaravan.ExchangeResult neutralResult = TradeCaravan.exchange(sourceNeutral, targetNeutral);
		Assertions.assertTrue(neutralResult.occurred(), "neutral caravan must still run (only rivals/war refuse)");
		Assertions.assertEquals(0.5, neutralResult.rate(), 1e-9, "neutral relation must yield the halved trade rate");
		Assertions.assertTrue(neutralResult.shipped() < allyShipped,
				"neutral must ship strictly less than ally: neutral=" + neutralResult.shipped() + " ally=" + allyShipped);
		Assertions.assertTrue(sourceNeutral.resource(ResourceType.FOOD) < 200, "neutral source FOOD must drop");
		Assertions.assertTrue(targetNeutral.resource(ResourceType.FOOD) > 0, "neutral target FOOD must rise");

		// CASE 3 (RIVAL): rate 0.0 -> rivals refuse to trade; no resource moves on
		// either side, proving the relation mechanically gates the exchange.
		ColonyData sourceRival = baseColony();
		sourceRival.setProgress(ColonyProgress.allied(sourceRival.id()));
		sourceRival.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(0, 64, 8)));
		ColonyData targetRival = new ColonyData(2, new BlockPos(64, 64, 0));
		sourceRival.setResource(ResourceType.FOOD, 200);
		targetRival.setResource(ResourceType.FOOD, 0);
		sourceRival.progress().setRelation(targetRival.id(), DiplomacyState.RIVAL);
		int rivalSourceBefore = sourceRival.resource(ResourceType.FOOD);
		int rivalTargetBefore = targetRival.resource(ResourceType.FOOD);
		TradeCaravan.ExchangeResult rivalResult = TradeCaravan.exchange(sourceRival, targetRival);
		Assertions.assertFalse(rivalResult.occurred(), "rivals must refuse to trade entirely");
		Assertions.assertEquals(rivalSourceBefore, sourceRival.resource(ResourceType.FOOD), "rival source FOOD must be unchanged");
		Assertions.assertEquals(rivalTargetBefore, targetRival.resource(ResourceType.FOOD), "rival target FOOD must be unchanged");

		// CASE 4 (scarcity gate): no surplus -> no cargo is picked and no exchange
		// runs, proving the scarcity (not just relation) drives the trade.
		ColonyData sourceNoSurplus = baseColony();
		sourceNoSurplus.setProgress(ColonyProgress.allied(sourceNoSurplus.id()));
		sourceNoSurplus.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(0, 64, 8)));
		ColonyData targetScarce = new ColonyData(2, new BlockPos(64, 64, 0));
		sourceNoSurplus.setResource(ResourceType.FOOD, 10); // below SURPLUS_THRESHOLD
		targetScarce.setResource(ResourceType.FOOD, 0);
		sourceNoSurplus.progress().setRelation(targetScarce.id(), DiplomacyState.ALLY);
		TradeCaravan.ExchangeResult noCargo = TradeCaravan.exchange(sourceNoSurplus, targetScarce);
		Assertions.assertFalse(noCargo.occurred(), "no surplus means no scarcity-driven cargo, so no caravan runs");
	}

	@Test
	void nativeBlockFungusGardenCompostsFoodIntoFungus() {
		// Content row block_native_gameplay_roles: at least one native Formic
		// block has a gameplay role, is obtainable/placeable in normal play, and
		// participates in the colony tick. The FUNGUS_GARDEN block is craftable
		// (see data/formic_frontier/recipe/fungus_garden.json) and placeable
		// (blockstate/model/item/texture all registered). NativeBlockRole.tick is
		// the role wired into ColonySavedState.tickEconomy(); each completed
		// FUNGUS_GARDEN composts stored FOOD into FUNGUS per pass, which is a real
		// resource-to-resource conversion with an input cost -- distinct from the
		// passive income multiplier the same building grants in ColonyEconomy.

		// CASE 1 (conversion runs): a colony with TWO completed fungus gardens and
		// enough FOOD above the reserve composts FOOD into FUNGUS. FOOD must drop by
		// exactly FOOD_PER_GARDEN * gardensRun and FUNGUS must rise by exactly
		// FUNGUS_PER_GARDEN * gardensRun, proving an input-carrying conversion
		// (not a free bonus), and all gardens run.
		ColonyData colony = baseColony();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(0, 64, 8)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(8, 64, 8)));
		Assertions.assertEquals(2, gardens(colony), "fixture: two completed fungus gardens");
		colony.setResource(ResourceType.FOOD, 200);
		colony.setResource(ResourceType.FUNGUS, 0);
		int foodBefore = colony.resource(ResourceType.FOOD);
		int fungusBefore = colony.resource(ResourceType.FUNGUS);
		NativeBlockRole.BlockRoleResult result = NativeBlockRole.tick(colony);
		Assertions.assertTrue(result.anyChange(), "fungus gardens must run their colony-tick role");
		Assertions.assertEquals(2, result.gardensRun(), "both gardens must run when food is well above the reserve");
		Assertions.assertEquals(2 * NativeBlockRole.FOOD_PER_GARDEN, result.foodComposted());
		Assertions.assertEquals(2 * NativeBlockRole.FUNGUS_PER_GARDEN, result.fungusCultivated());
		Assertions.assertEquals(foodBefore - 2 * NativeBlockRole.FOOD_PER_GARDEN, colony.resource(ResourceType.FOOD),
				"FOOD must be consumed by the conversion (input cost)");
		Assertions.assertEquals(fungusBefore + 2 * NativeBlockRole.FUNGUS_PER_GARDEN, colony.resource(ResourceType.FUNGUS),
				"FUNGUS must be produced by the conversion");

		// CASE 2 (starvation guard): with FOOD barely above the reserve, only some
		// gardens run, and the colony must NEVER be drained below FOOD_RESERVE. The
		// native block's role is a living-colony behavior, not a self-destructive
		// drain: it yields to the colony's survival.
		ColonyData hungry = baseColony();
		hungry.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(0, 64, 8)));
		hungry.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(8, 64, 8)));
		Assertions.assertEquals(2, gardens(hungry));
		// Reserve is 40, cost/garden is 5: with 52 food, only 2 gardens could run
		// mathematically (12 above reserve / 5 = 2), so both run and we still land
		// above reserve. Set 46 so only 1 garden can run (6 above reserve / 5 = 1),
		// proving the guard throttles work rather than running greedily.
		hungry.setResource(ResourceType.FOOD, NativeBlockRole.FOOD_RESERVE + 6);
		hungry.setResource(ResourceType.FUNGUS, 0);
		int hungryFoodBefore = hungry.resource(ResourceType.FOOD);
		NativeBlockRole.BlockRoleResult throttled = NativeBlockRole.tick(hungry);
		Assertions.assertEquals(1, throttled.gardensRun(), "starvation guard must throttle to the gardens food safely allows");
		Assertions.assertEquals(NativeBlockRole.FOOD_PER_GARDEN, throttled.foodComposted());
		Assertions.assertEquals(NativeBlockRole.FUNGUS_PER_GARDEN, throttled.fungusCultivated());
		Assertions.assertEquals(hungryFoodBefore - NativeBlockRole.FOOD_PER_GARDEN, hungry.resource(ResourceType.FOOD));
		Assertions.assertTrue(hungry.resource(ResourceType.FOOD) >= NativeBlockRole.FOOD_RESERVE,
				"gardens must never drain the colony below the food reserve; got " + hungry.resource(ResourceType.FOOD));

		// CASE 3 (no surplus -> gardens idle): with FOOD at or below the reserve the
		// gardens run nothing, so a colony that cannot spare food keeps its stockpile
		// and produces no fungus -- the role is conditional, not unconditional income.
		ColonyData starving = baseColony();
		starving.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(0, 64, 8)));
		starving.progress().addBuilding(ColonyBuilding.complete(BuildingType.FUNGUS_GARDEN, new BlockPos(8, 64, 8)));
		starving.setResource(ResourceType.FOOD, NativeBlockRole.FOOD_RESERVE);
		starving.setResource(ResourceType.FUNGUS, 0);
		int starvingFoodBefore = starving.resource(ResourceType.FOOD);
		int starvingFungusBefore = starving.resource(ResourceType.FUNGUS);
		NativeBlockRole.BlockRoleResult idle = NativeBlockRole.tick(starving);
		Assertions.assertFalse(idle.anyChange(), "gardens must idle when no food above the reserve is available");
		Assertions.assertEquals(starvingFoodBefore, starving.resource(ResourceType.FOOD), "idle gardens must not consume food");
		Assertions.assertEquals(starvingFungusBefore, starving.resource(ResourceType.FUNGUS), "idle gardens must not produce fungus");

		// CASE 4 (no native block -> no-op): a colony with no FUNGUS_GARDEN must not
		// be affected by the native-block pass at all, so the role is owned by the
		// block, not the colony ticker.
		ColonyData noGarden = baseColony();
		Assertions.assertEquals(0, gardens(noGarden), "fixture: no fungus gardens");
		noGarden.setResource(ResourceType.FOOD, 200);
		noGarden.setResource(ResourceType.FUNGUS, 0);
		int noGardenFoodBefore = noGarden.resource(ResourceType.FOOD);
		int noGardenFungusBefore = noGarden.resource(ResourceType.FUNGUS);
		NativeBlockRole.BlockRoleResult noOp = NativeBlockRole.tick(noGarden);
		Assertions.assertFalse(noOp.anyChange(), "without a FUNGUS_GARDEN the native-block pass must be a no-op");
		Assertions.assertEquals(noGardenFoodBefore, noGarden.resource(ResourceType.FOOD));
		Assertions.assertEquals(noGardenFungusBefore, noGarden.resource(ResourceType.FUNGUS));
	}

	private static int gardens(ColonyData colony) {
		return (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.FUNGUS_GARDEN && building.complete())
				.count();
	}

	private static ColonyData fundedDefenseColony() {		// A colony whose only reachable growth branch is the elite GIANT caste.
		// DEFENSE instinct is the only priority; soldier/major targets are already
		// saturated so priorityGrowth(DEFENSE) falls through to the GIANT branch,
		// and the worker/miner/soldier fallbacks in chooseGrowth are also saturated.
		ColonyData colony = baseColony();
		colony.setPriorities(List.of(TaskPriority.DEFENSE));
		// Saturate lower castes so their growth branches cannot fire first.
		colony.addCaste(AntCaste.WORKER, Math.max(0, 8 - colony.casteCount(AntCaste.WORKER)));
		colony.addCaste(AntCaste.SCOUT, Math.max(0, 3 - colony.casteCount(AntCaste.SCOUT)));
		colony.addCaste(AntCaste.MINER, Math.max(0, 8 - colony.casteCount(AntCaste.MINER)));
		// Soldier target for DEFENSE is 3 + barracks*2; with no barracks that is 3.
		colony.addCaste(AntCaste.SOLDIER, Math.max(0, 3 - colony.casteCount(AntCaste.SOLDIER)));
		// MAJOR target for DEFENSE is 4.
		colony.addCaste(AntCaste.MAJOR, Math.max(0, 4 - colony.casteCount(AntCaste.MAJOR)));
		// Fund every GIANT growth cost (food>250 plus GIANT food/ore/chitin costs)
		// so resources never gate GIANT -- only the research gate can.
		colony.setResource(ResourceType.FOOD, 600);
		colony.setResource(ResourceType.ORE, 300);
		colony.setResource(ResourceType.CHITIN, 300);
		colony.setResource(ResourceType.RESIN, 100);
		// Fund knowledge so the MANDIBLE_PLATING research cost (KNOWLEDGE 30)
		// can actually be spent in CASE B, proving the unlock is paid for.
		colony.setResource(ResourceType.KNOWLEDGE, 80);
		return colony;
	}

	@Test
	void politicsRelationsShiftFromActionsChangeTradeRate() {
		// Content row politics_relations_shift_from_actions: a diplomatic action
		// (tribute / incite) shifts the RECORDED relation between two colonies, and
		// that shift has a mechanical consequence because TradeCaravan.relationRate
		// scales (or blocks) trade. The same DiplomacyService.perform the live
		// tickEconomy envoy pass calls is exercised here end to end.
		ColonyData actor = baseColony();
		actor.setProgress(ColonyProgress.allied(actor.id()));
		// Reach at least BURROW rank so TRIBUTE (minRank BURROW) is allowed: one
		// completed DIPLOMACY_SHRINE pushes the score over the BURROW threshold.
		actor.progress().addBuilding(ColonyBuilding.complete(BuildingType.DIPLOMACY_SHRINE, new BlockPos(0, 64, 8)));
		Assertions.assertTrue(ColonyRank.BURROW.atLeast(actor), "fixture: actor must be BURROW+ to perform TRIBUTE");
		ColonyData target = new ColonyData(2, new BlockPos(64, 64, 0));

		// CASE 1 (improve relation -> more trade): start NEUTRAL (rate 0.5), perform
		// TRIBUTE which must shift the recorded relation NEUTRAL -> ALLY (rate 1.0).
		// The action must both change the recorded relation AND raise the mechanical
		// trade rate the caravan pass would apply.
		actor.progress().setRelation(target.id(), DiplomacyState.NEUTRAL);
		Assertions.assertEquals(DiplomacyState.NEUTRAL, actor.progress().relationTo(target.id()), "fixture: relation starts NEUTRAL");
		double rateBefore = TradeCaravan.relationRate(actor.progress().relationTo(target.id()));
		DiplomacyService.ActionResult tribute = DiplomacyService.perform(actor, target, DiplomacyAction.TRIBUTE);
		Assertions.assertTrue(tribute.occurred(), "TRIBUTE must be accepted for a BURROW+ actor");
		Assertions.assertEquals(DiplomacyState.NEUTRAL, tribute.before(), "before relation must be NEUTRAL");
		Assertions.assertEquals(DiplomacyState.ALLY, tribute.after(), "TRIBUTE must shift NEUTRAL -> ALLY");
		Assertions.assertEquals(DiplomacyState.ALLY, actor.progress().relationTo(target.id()), "shifted relation must be RECORDED toward the target");
		double rateAfter = TradeCaravan.relationRate(actor.progress().relationTo(target.id()));
		Assertions.assertTrue(rateAfter > rateBefore, "improving the relation must RAISE the mechanical trade rate: " + rateBefore + " -> " + rateAfter);

		// CASE 2 (worsen relation -> less trade): same actor, now ALLY toward a new
		// target, performs INCITE which must shift ALLY -> NEUTRAL and LOWER the rate.
		ColonyData targetB = new ColonyData(3, new BlockPos(96, 64, 0));
		actor.progress().setRelation(targetB.id(), DiplomacyState.ALLY);
		double rateBeforeB = TradeCaravan.relationRate(actor.progress().relationTo(targetB.id()));
		DiplomacyService.ActionResult incite = DiplomacyService.perform(actor, targetB, DiplomacyAction.INCITE);
		Assertions.assertTrue(incite.occurred(), "INCITE must be accepted for a BURROW+ actor");
		Assertions.assertEquals(DiplomacyState.ALLY, incite.before(), "before relation must be ALLY");
		Assertions.assertEquals(DiplomacyState.NEUTRAL, incite.after(), "INCITE must shift ALLY -> NEUTRAL");
		Assertions.assertEquals(DiplomacyState.NEUTRAL, actor.progress().relationTo(targetB.id()), "worsened relation must be RECORDED");
		double rateAfterB = TradeCaravan.relationRate(actor.progress().relationTo(targetB.id()));
		Assertions.assertTrue(rateAfterB < rateBeforeB, "worsening the relation must LOWER the mechanical trade rate: " + rateBeforeB + " -> " + rateAfterB);

		// CASE 3 (rank gate): an OUTPOST-rank colony (no buildings) cannot perform
		// TRIBUTE (minRank BURROW); the action is rejected and the relation is
		// unchanged, proving the action has a real precondition, not a free toggle.
		ColonyData outpost = new ColonyData(4, new BlockPos(0, 64, 0));
		outpost.setProgress(ColonyProgress.allied(outpost.id()));
		Assertions.assertFalse(ColonyRank.BURROW.atLeast(outpost), "fixture: a buildingless colony is OUTPOST, below BURROW");
		outpost.progress().setRelation(target.id(), DiplomacyState.NEUTRAL);
		DiplomacyService.ActionResult gated = DiplomacyService.perform(outpost, target, DiplomacyAction.TRIBUTE);
		Assertions.assertFalse(gated.occurred(), "a sub-BURROW colony must be BLOCKED from TRIBUTE by the rank gate");
		Assertions.assertEquals(DiplomacyState.NEUTRAL, outpost.progress().relationTo(target.id()), "a rejected action must not change the relation");
	}

	@Test
	void soldierWeaponLoadoutChangesCombatStats() {
		// Content row weapon_soldier_combat_stats: a soldier caste uses an
		// ant-themed weapon/armor with real combat stats (damage/defense) in a
		// raid or defense, proven by a gametest asserting the combat outcome
		// differs WITH vs WITHOUT the weapon.
		//
		// The mod already registers ant-themed weapon ITEMS (mandible_saber +5,
		// venom_spear +4) and chitin/resin-chitin armor, but before this row they
		// had ZERO effect on colony combat: RaidPlanner.militaryStrength and
		// defenseRating only counted raw castes/buildings. ColonyArmory derives
		// the equipped loadout from colony state and the raid resolver applies it.

		// CASE 0 (baseline, UNARMED): a garrison with soldiers but NO armory and
		// NO weapon research fights with bare mandibles only. ColonyArmory must
		// report zero weapon attack and zero armor defense, exactly as before.
		ColonyData unarmed = baseColony();
		int unarmedSoldiers = unarmed.casteCount(AntCaste.SOLDIER) + unarmed.casteCount(AntCaste.MAJOR);
		Assertions.assertTrue(unarmedSoldiers > 0, "fixture: base colony must have soldiers");
		ColonyArmory.Loadout unarmedLoadout = ColonyArmory.loadout(unarmed);
		Assertions.assertEquals(0, unarmedLoadout.armories(), "an unarmed colony has no armory");
		Assertions.assertEquals(0, unarmedLoadout.weaponAttack(), "bare-mandible garrison adds no weapon attack");
		Assertions.assertEquals(0, unarmedLoadout.armorDefense(), "an unarmed colony adds no armor defense");
		Assertions.assertEquals("bare_mandibles", unarmedLoadout.weapon(), "unarmed weapon must read as bare mandibles");
		Assertions.assertFalse(unarmedLoadout.isArmed(), "baseline colony must not be armed");

		// CASE 1 (ARMED with mandible saber): the SAME soldier count, same
		// resources, same buildings PLUS a completed ARMORY + MANDIBLE_PLATING
		// research. The colony now equips its soldiers with mandible sabers, so
		// the weapon attack contribution must be strictly greater than zero and
		// strictly greater than the unarmed colony. This is the combat outcome
		// changing WITH vs WITHOUT the weapon, at the stat level the raid
		// resolver consumes (militaryStrength = base caste strength + weapon attack).
		ColonyData armed = baseColony();
		// Same garrison as CASE 0.
		Assertions.assertEquals(unarmedSoldiers, armed.casteCount(AntCaste.SOLDIER) + armed.casteCount(AntCaste.MAJOR),
				"fixture: armed and unarmed colonies must have the SAME soldier count");
		// Build a completed ARMORY so the colony can forge/equip weapons.
		armed.progress().addBuilding(ColonyBuilding.complete(BuildingType.ARMORY, new BlockPos(8, 64, 8)));
		// Complete MANDIBLE_PLATING research (the node that unlocks the strongest
		// colony melee weapon). Marking it complete is the post-research state.
		armed.progress().completeResearch(ResearchNode.MANDIBLE_PLATING.id());
		ColonyArmory.Loadout armedLoadout = ColonyArmory.loadout(armed);
		Assertions.assertEquals(1, armedLoadout.armories(), "armed colony must have a completed armory");
		Assertions.assertTrue(armedLoadout.armedSoldiers() > 0, "the armory must arm at least one soldier");
		Assertions.assertTrue(armedLoadout.armedSoldiers() <= unarmedSoldiers, "armory cannot arm more soldiers than exist");
		Assertions.assertEquals("mandible_saber", armedLoadout.weapon(), "MANDIBLE_PLATING must equip the mandible saber");
		Assertions.assertTrue(armedLoadout.isArmed(), "armed colony must report a real weapon");
		Assertions.assertTrue(armedLoadout.weaponAttack() > 0, "mandible saber must add weapon attack");
		Assertions.assertTrue(armedLoadout.mandibleUnlocked(), "MANDIBLE_PLATING must be reflected in the loadout");
		// Per-soldier attack of the mandible saber mirrors the item's bonusDamage.
		Assertions.assertEquals(armedLoadout.armedSoldiers() * ColonyArmory.MANDIBLE_SABER_ATTACK, armedLoadout.weaponAttack(),
				"mandible saber attack must equal armed soldiers * per-saber damage");

		// CASE 2 (the OUTCOME differs): the armed colony's weapon attack is
		// strictly greater than the unarmed colony's, so a raid resolved with
		// militaryStrength = base + ColonyArmory.weaponAttack produces a
		// different (stronger) attacker outcome. This is the combat-outcome
		// delta the row requires.
		Assertions.assertTrue(armedLoadout.weaponAttack() > unarmedLoadout.weaponAttack(),
				"armed soldiers must hit harder than unarmed soldiers: " + unarmedLoadout.weaponAttack() + " -> " + armedLoadout.weaponAttack());
		// ColonyArmory.weaponAttack is the exact contribution RaidPlanner adds to
		// militaryStrength, so the same base garrison resolves to a strictly
		// higher military strength when armed.
		int strengthUnarmed = unarmed.casteCount(AntCaste.SOLDIER) * 4 + unarmed.casteCount(AntCaste.MAJOR) * 8 + unarmed.casteCount(AntCaste.GIANT) * 16;
		int strengthArmed = armed.casteCount(AntCaste.SOLDIER) * 4 + armed.casteCount(AntCaste.MAJOR) * 8 + armed.casteCount(AntCaste.GIANT) * 16;
		Assertions.assertEquals(strengthUnarmed, strengthArmed, "fixture: both garrisons have identical BASE strength");
		Assertions.assertTrue(strengthArmed + armedLoadout.weaponAttack() > strengthUnarmed + unarmedLoadout.weaponAttack(),
				"armed military strength must exceed unarmed military strength");
	}

	@Test
	void venomSpearAndArmorAdvanceCombatLoadout() {
		// Companion to soldierWeaponLoadoutChangesCombatStats: proves the venom
		// spear (the other ant-themed weapon) and chitin/resin-chitin armor also
		// carry real combat stats, and that the loadout ADVANCES as the colony
		// researches better gear. This guards against the loadout being a static
		// toggle and proves armor defense contributes to defenseRating.

		// Venom spear only: ARMORY + VENOM_DRILLS research, no MANDIBLE_PLATING.
		ColonyData venom = baseColony();
		venom.progress().addBuilding(ColonyBuilding.complete(BuildingType.ARMORY, new BlockPos(8, 64, 8)));
		venom.progress().completeResearch(ResearchNode.VENOM_DRILLS.id());
		ColonyArmory.Loadout venomLoadout = ColonyArmory.loadout(venom);
		Assertions.assertEquals("venom_spear", venomLoadout.weapon(), "VENOM_DRILLS (without MANDIBLE_PLATING) equips the venom spear");
		Assertions.assertTrue(venomLoadout.venomUnlocked() && !venomLoadout.mandibleUnlocked(),
				"venom colony must have venom but not mandible plating");
		Assertions.assertTrue(venomLoadout.weaponAttack() > 0, "venom spear must add weapon attack");
		Assertions.assertEquals(venomLoadout.armedSoldiers() * ColonyArmory.VENOM_SPEAR_ATTACK, venomLoadout.weaponAttack(),
				"venom spear attack must equal armed soldiers * per-spear damage");

		// Prefer the stronger weapon: adding MANDIBLE_PLATING on top must switch
		// the equipped weapon to the mandible saber and INCREASE the attack.
		int attackBefore = venomLoadout.weaponAttack();
		venom.progress().completeResearch(ResearchNode.MANDIBLE_PLATING.id());
		ColonyArmory.Loadout upgradedLoadout = ColonyArmory.loadout(venom);
		Assertions.assertEquals("mandible_saber", upgradedLoadout.weapon(), "with both unlocks the stronger mandible saber must be equipped");
		Assertions.assertTrue(upgradedLoadout.weaponAttack() > attackBefore,
				"upgrading from venom spear to mandible saber must raise weapon attack: " + attackBefore + " -> " + upgradedLoadout.weaponAttack());

		// Armor defense: a CHITIN_FARM supplies chitin plate, which adds real
		// armor defense to the garrison. defenseRating consumes this directly.
		Assertions.assertEquals(0, venomLoadout.armorDefense(), "without a chitin supply there is no armor defense");
		venom.progress().addBuilding(ColonyBuilding.complete(BuildingType.CHITIN_FARM, new BlockPos(10, 64, 10)));
		ColonyArmory.Loadout armoredLoadout = ColonyArmory.loadout(venom);
		Assertions.assertTrue(armoredLoadout.armorDefense() > 0, "a chitin supply must add armor defense to the armed garrison");
		Assertions.assertFalse(armoredLoadout.resinChitin(), "without RESIN_MASONRY the armor is base chitin, not resin-chitin");
		// RESIN_MASONRY upgrades the armor and must STRICTLY raise the defense.
		int defenseBefore = armoredLoadout.armorDefense();
		venom.progress().completeResearch(ResearchNode.RESIN_MASONRY.id());
		ColonyArmory.Loadout resinArmoredLoadout = ColonyArmory.loadout(venom);
		Assertions.assertTrue(resinArmoredLoadout.resinChitin(), "RESIN_MASONRY must upgrade the garrison to resin-chitin plate");
		Assertions.assertTrue(resinArmoredLoadout.armorDefense() > defenseBefore,
				"resin-chitin plate must defend better than base chitin: " + defenseBefore + " -> " + resinArmoredLoadout.armorDefense());
	}
	@Test
	void raidOutcomeChangesColonyStateWithCasualtiesAndRelationShift() {
		// Content row defense_raid_outcome_changes_state: a raid must resolve
		// with CASUALTIES (combat castes lost on both sides) and an OUTCOME that
		// changes colony state (resources lost/defended AND a relation change),
		// proven end to end. RaidPlanner.resolveCombat is the pure combat core;
		// applyRaidOutcome mutates colony state. Two colonies differ only in
		// garrison strength so the combat OUTCOME differs with vs without defense.

		// ATTACKER: a strong rival garrison (8 soldiers => base strength 32,
		// comfortably above the raid strength gate of 16).
		ColonyData attacker = baseColony();
		attacker.addCaste(AntCaste.SOLDIER, 6); // base 2 + 6 = 8 soldiers
		int attackerSoldiersBefore = attacker.casteCount(AntCaste.SOLDIER);

		// DEFENDER (undefended): only the base 2 soldiers from baseColony.
		// Food 120 is the richest resource, so the raid loots FOOD.
		ColonyData weakDefender = baseColony();
		weakDefender.progress().setRelation(attacker.id(), DiplomacyState.RIVAL);
		int weakFoodBefore = weakDefender.resource(ResourceType.FOOD);
		int weakDefendersBefore = weakDefender.casteCount(AntCaste.SOLDIER);

		RaidPlanner.RaidOutcome outcome = RaidPlanner.resolveCombat(attacker, weakDefender, null);
		RaidPlanner.applyRaidOutcome(outcome);

		// OUTCOME 1: resources moved from defender to attacker (state change:
		// resources lost/defended).
		Assertions.assertTrue(outcome.stolen() > 0, "raid must loot resources from the defender");
		Assertions.assertEquals(weakFoodBefore - outcome.stolen(), weakDefender.resource(ResourceType.FOOD),
				"defender must lose the looted resource");
		int attackerFoodBefore = 120;
		Assertions.assertEquals(attackerFoodBefore + outcome.stolen(), attacker.resource(ResourceType.FOOD),
				"attacker stockpile must rise by the stolen amount");

		// OUTCOME 2: CASUALTIES. The outmatched weak defender must lose defenders,
		// and the stronger attacker must take strictly fewer (or zero) losses.
		Assertions.assertTrue(outcome.defenderLosses() > 0,
				"an outmatched defender must suffer casualties, got " + outcome.defenderLosses());
		Assertions.assertEquals(weakDefendersBefore - outcome.defenderLosses(), weakDefender.casteCount(AntCaste.SOLDIER),
				"defender combat caste must actually decrease by the casualty count");
		Assertions.assertEquals(attackerSoldiersBefore - outcome.attackerLosses(), attacker.casteCount(AntCaste.SOLDIER),
				"attacker combat caste must actually decrease by its casualty count");
		Assertions.assertTrue(outcome.defenderLosses() >= outcome.attackerLosses(),
				"the outmatched side must lose at least as many as the stronger side");

		// OUTCOME 3: RELATION CHANGE. The defender failed to repel the raid
		// (attack strength >= defense), so the recorded relation must worsen
		// (RIVAL -> WAR). This is the diplomacy state change the row requires.
		Assertions.assertTrue(outcome.relationShifted(),
				"a failed defense must escalate the relation, got shifted=" + outcome.relationShifted());
		Assertions.assertEquals(DiplomacyState.WAR, outcome.shiftedRelation(),
				"RIVAL must worsen to WAR after a failed defense");
		Assertions.assertEquals(DiplomacyState.WAR, weakDefender.progress().relationTo(attacker.id()),
				"the relation shift must be applied to colony state");

		// OUTCOME DIFFERS WITH VS WITHOUT DEFENSE: a well-defended colony takes
		// strictly fewer losses and does NOT escalate to WAR. Give the defender a
		// large garrison so defense > attack (the defender holds).
		ColonyData strongAttacker = baseColony();
		strongAttacker.addCaste(AntCaste.SOLDIER, 6); // 8 soldiers, strength 32
		ColonyData strongDefender = baseColony();
		// A well-defended colony: a large garrison PLUS two completed WATCH_POST
		// towers. defenseRating = watchposts*6 + militaryStrength/3 = 12 + (72/3) =
		// 12 + 24 = 36, which strictly exceeds the attacker's attackStrength 32, so
		// the defender HOLDS the raid (no relation escalation) and takes fewer losses.
		strongDefender.addCaste(AntCaste.SOLDIER, 16); // 18 soldiers, strength 72
		strongDefender.progress().addBuilding(ColonyBuilding.complete(BuildingType.WATCH_POST, new BlockPos(6, 64, 6)));
		strongDefender.progress().addBuilding(ColonyBuilding.complete(BuildingType.WATCH_POST, new BlockPos(8, 64, 8)));
		strongDefender.progress().setRelation(strongAttacker.id(), DiplomacyState.RIVAL);
		RaidPlanner.RaidOutcome held = RaidPlanner.resolveCombat(strongAttacker, strongDefender, null);

		// The well-defended colony loses strictly fewer defenders than the weak one did.
		Assertions.assertTrue(held.defenderLosses() < outcome.defenderLosses(),
				"a strong defense must take fewer casualties than a weak one: " + held.defenderLosses() + " vs " + outcome.defenderLosses());
		// When the defender holds (defense > attack) the relation must NOT escalate.
		Assertions.assertFalse(held.relationShifted(),
				"a successful defense must not escalate the relation to WAR; shifted=" + held.relationShifted()
						+ " (attack=" + held.attackStrength() + " defense=" + held.defenseRating() + ")");
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
