package com.formicfrontier.sim;

public final class TaskPlanner {
	private TaskPlanner() {
	}

	public static String describeNextTask(ColonyData colony) {
		for (TaskPriority priority : colony.prioritiesView()) {
			switch (priority) {
				case FOOD -> {
					if (colony.resource(ResourceType.FOOD) < colony.upkeepPerEconomyTick() * 4) {
						return "Foraging for food";
					}
				}
				case ORE -> {
					if (colony.resource(ResourceType.ORE) < 20) {
						return "Mining ore node";
					}
				}
				case CHITIN -> {
					if (colony.resource(ResourceType.CHITIN) < 20) {
						return "Cultivating chitin nursery";
					}
				}
				case DEFENSE -> {
					if (colony.casteCount(AntCaste.SOLDIER) + colony.casteCount(AntCaste.MAJOR) < 2) {
						return "Raising defenders";
					}
				}
			}
		}
		return "Balanced maintenance";
	}

	public static AntCaste preferredWorkerFor(ResourceType resourceType) {
		return switch (resourceType) {
			case FOOD -> AntCaste.WORKER;
			case ORE -> AntCaste.MINER;
			case CHITIN, RESIN, FUNGUS, VENOM, KNOWLEDGE -> AntCaste.WORKER;
		};
	}
}
