package com.formicfrontier.sim;

import com.formicfrontier.network.ColonyUiSnapshot;
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
		colony.progress().requests().add(new ColonyRequest(BuildingType.ARMORY, ResourceType.RESIN, 10, 4, "test request"));
		colony.progress().completeResearch(ResearchNode.CHITIN_CULTIVATION.id());
		colony.addEvent("Test event");

		var encoded = ColonyData.CODEC.encodeStart(JsonOps.INSTANCE, colony).getOrThrow();
		ColonyData decoded = ColonyData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

		Assertions.assertEquals(colony.id(), decoded.id());
		Assertions.assertEquals(colony.resource(ResourceType.FOOD), decoded.resource(ResourceType.FOOD));
		Assertions.assertEquals(colony.casteCount(AntCaste.WORKER), decoded.casteCount(AntCaste.WORKER));
		Assertions.assertEquals(colony.chambersView().size(), decoded.chambersView().size());
		Assertions.assertTrue(decoded.progress().hasCompleted(BuildingType.MARKET));
		Assertions.assertEquals(ColonyCulture.AMBER, decoded.progress().culture());
		Assertions.assertEquals(1, decoded.progress().requestsView().size());
		Assertions.assertTrue(decoded.progress().hasResearch(ResearchNode.CHITIN_CULTIVATION.id()));
		Assertions.assertEquals("Test event", decoded.progress().eventsView().getFirst().message());
	}

	@Test
	void colonyUiSnapshotExposesStructuredStateWithoutStatusParsing() {
		ColonyData colony = baseColony();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_CHAMBER, new BlockPos(0, 64, 0)));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, new BlockPos(22, 64, -20)));
		ColonyLogistics.requestResource(colony, BuildingType.ARMORY, ResourceType.RESIN, 12, "unit ui");
		colony.progress().completeResearch(ResearchNode.CHITIN_CULTIVATION.id());

		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Requests", "hello");

		Assertions.assertEquals(colony.id(), snapshot.colonyId());
		Assertions.assertEquals("Requests", snapshot.initialTab());
		Assertions.assertEquals("hello", snapshot.feedbackMessage());
		Assertions.assertEquals(ResourceType.values().length, snapshot.resources().size());
		Assertions.assertEquals(AntCaste.values().length, snapshot.population().size());
		Assertions.assertFalse(snapshot.overview().isEmpty());
		Assertions.assertTrue(snapshot.overview().stream().anyMatch(entry -> entry.labelKey().equals("formic_frontier.ui.top_need")));
		Assertions.assertFalse(snapshot.buildings().isEmpty());
		Assertions.assertFalse(snapshot.requests().isEmpty());
		Assertions.assertTrue(snapshot.research().stream().anyMatch(entry -> entry.nodeId().equals(ResearchNode.CHITIN_CULTIVATION.id()) && entry.complete()));
		Assertions.assertFalse(snapshot.currentTask().contains("==="));
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
}
