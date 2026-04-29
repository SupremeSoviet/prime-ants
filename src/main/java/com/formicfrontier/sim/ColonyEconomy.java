package com.formicfrontier.sim;

public final class ColonyEconomy {
	public static final int ECONOMY_TICK_INTERVAL = 20;

	private ColonyEconomy() {
	}

	public static EconomyResult tick(ColonyData colony) {
		colony.addAgeTicks(ECONOMY_TICK_INTERVAL);

		int workers = colony.casteCount(AntCaste.WORKER);
		int scouts = colony.casteCount(AntCaste.SCOUT);
		int miners = colony.casteCount(AntCaste.MINER);
		int soldiers = colony.casteCount(AntCaste.SOLDIER);
		int chitinFarms = completed(colony, BuildingType.CHITIN_FARM);
		int foodStores = completed(colony, BuildingType.FOOD_STORE);
		int mines = completed(colony, BuildingType.MINE);
		int nurseries = completed(colony, BuildingType.NURSERY);
		int barracks = completed(colony, BuildingType.BARRACKS);
		int markets = completed(colony, BuildingType.MARKET);
		int diplomacyShrines = completed(colony, BuildingType.DIPLOMACY_SHRINE);
		int resinDepots = completed(colony, BuildingType.RESIN_DEPOT);
		int archives = completed(colony, BuildingType.PHEROMONE_ARCHIVE);
		int fungusGardens = completed(colony, BuildingType.FUNGUS_GARDEN);
		int venomPresses = completed(colony, BuildingType.VENOM_PRESS);
		int armories = completed(colony, BuildingType.ARMORY);
		ColonyRank rank = ColonyRank.current(colony);
		ColonyCulture culture = colony.progress().culture();

		int foodIncome = 4 + workers * 3 + scouts + foodStores * 2 + markets + fungusGardens * 2 + rank.economyBonus() + culture.foodBonus();
		int oreIncome = miners * 2 + mines + rank.economyBonus() / 2;
		int chitinIncome = Math.max(0, colony.resource(ResourceType.FOOD) / 25) + workers / 2 + chitinFarms * 5 + nurseries + diplomacyShrines + rank.economyBonus() / 2;
		if (colony.progress().hasResearch(ResearchNode.CHITIN_CULTIVATION.id())) {
			chitinIncome += Math.max(1, chitinFarms * 2);
		}
		int resinIncome = resinDepots * 4 + workers / 3 + culture.resinBonus();
		int fungusIncome = fungusGardens * 4 + nurseries + culture.fungusBonus();
		if (colony.progress().hasResearch(ResearchNode.FUNGUS_SYMBIOSIS.id())) {
			foodIncome += Math.max(1, fungusGardens * 3);
			fungusIncome += 2;
		}
		int venomIncome = venomPresses * 3 + culture.venomBonus();
		if (colony.progress().hasResearch(ResearchNode.VENOM_DRILLS.id())) {
			venomIncome += Math.max(1, soldiers / 3 + armories);
		}
		int knowledgeIncome = archives > 0 && colony.progress().activeResearch().isEmpty() ? archives : 0;
		int upkeep = colony.upkeepPerEconomyTick();

		colony.addResource(ResourceType.FOOD, foodIncome - upkeep);
		colony.addResource(ResourceType.ORE, oreIncome);
		colony.addResource(ResourceType.CHITIN, chitinIncome);
		colony.addResource(ResourceType.RESIN, resinIncome);
		colony.addResource(ResourceType.FUNGUS, fungusIncome);
		colony.addResource(ResourceType.VENOM, venomIncome);
		colony.addResource(ResourceType.KNOWLEDGE, knowledgeIncome);

		if (!colony.queenAlive()) {
			colony.setCurrentTask("Queen lost: growth suspended");
			return new EconomyResult(foodIncome, oreIncome, chitinIncome, upkeep, null);
		}

		AntCaste grown = chooseGrowth(colony, soldiers, barracks);
		if (grown != null) {
			grown.consumeGrowthCost(colony);
			colony.addCaste(grown, 1);
			colony.setCurrentTask("Growing " + grown.id());
		} else {
			colony.setCurrentTask(TaskPlanner.describeNextTask(colony));
		}

		return new EconomyResult(foodIncome, oreIncome, chitinIncome, upkeep, grown);
	}

	private static AntCaste chooseGrowth(ColonyData colony, int soldiers, int barracks) {
		for (TaskPriority priority : colony.prioritiesView()) {
			AntCaste priorityGrowth = priorityGrowth(colony, priority, soldiers, barracks);
			if (priorityGrowth != null) {
				return priorityGrowth;
			}
		}
		if (colony.casteCount(AntCaste.WORKER) < 3 && AntCaste.WORKER.canGrowFrom(colony)) {
			return AntCaste.WORKER;
		}
		if (colony.casteCount(AntCaste.MINER) < 2 && AntCaste.MINER.canGrowFrom(colony)) {
			return AntCaste.MINER;
		}
		if (soldiers < 2 && AntCaste.SOLDIER.canGrowFrom(colony)) {
			return AntCaste.SOLDIER;
		}
		if (colony.resource(ResourceType.FOOD) > 250 && AntCaste.GIANT.canGrowFrom(colony)) {
			return AntCaste.GIANT;
		}
		if (colony.resource(ResourceType.CHITIN) > 30 && AntCaste.MAJOR.canGrowFrom(colony)) {
			return AntCaste.MAJOR;
		}
		if (colony.casteCount(AntCaste.SCOUT) < 1 && AntCaste.SCOUT.canGrowFrom(colony)) {
			return AntCaste.SCOUT;
		}
		return null;
	}

	private static AntCaste priorityGrowth(ColonyData colony, TaskPriority priority, int soldiers, int barracks) {
		return switch (priority) {
			case FOOD -> {
				int workerTarget = 8 + colony.progress().culture().workerBias();
				int scoutTarget = 3 + colony.progress().culture().scoutBias();
				if (colony.casteCount(AntCaste.WORKER) < workerTarget && AntCaste.WORKER.canGrowFrom(colony)) {
					yield AntCaste.WORKER;
				}
				if (colony.casteCount(AntCaste.SCOUT) < scoutTarget && AntCaste.SCOUT.canGrowFrom(colony)) {
					yield AntCaste.SCOUT;
				}
				yield null;
			}
			case ORE -> colony.casteCount(AntCaste.MINER) < 8 && AntCaste.MINER.canGrowFrom(colony) ? AntCaste.MINER : null;
			case CHITIN -> {
				if (colony.casteCount(AntCaste.MAJOR) < 2 && AntCaste.MAJOR.canGrowFrom(colony)) {
					yield AntCaste.MAJOR;
				}
				if (colony.casteCount(AntCaste.WORKER) < 6 && AntCaste.WORKER.canGrowFrom(colony)) {
					yield AntCaste.WORKER;
				}
				yield null;
			}
			case DEFENSE -> {
				int soldierTarget = 3 + barracks * 2;
				if (soldiers < soldierTarget && AntCaste.SOLDIER.canGrowFrom(colony)) {
					yield AntCaste.SOLDIER;
				}
				if (colony.casteCount(AntCaste.MAJOR) < 4 && AntCaste.MAJOR.canGrowFrom(colony)) {
					yield AntCaste.MAJOR;
				}
				if (colony.resource(ResourceType.FOOD) > 250 && AntCaste.GIANT.canGrowFrom(colony)) {
					yield AntCaste.GIANT;
				}
				yield null;
			}
		};
	}

	private static int completed(ColonyData colony, BuildingType type) {
		return (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == type && building.complete())
				.count();
	}

	public record EconomyResult(int foodIncome, int oreIncome, int chitinIncome, int upkeep, AntCaste grownCaste) {
	}
}
