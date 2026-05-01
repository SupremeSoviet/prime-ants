package com.formicfrontier.world;

import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ColonyProgress;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ColonyBuilder {
	private static final BuildingType[] STARTER_SEQUENCE = {
			BuildingType.QUEEN_CHAMBER,
			BuildingType.FOOD_STORE,
			BuildingType.NURSERY,
			BuildingType.MINE,
			BuildingType.CHITIN_FARM,
			BuildingType.BARRACKS,
			BuildingType.MARKET,
			BuildingType.PHEROMONE_ARCHIVE,
			BuildingType.ARMORY,
			BuildingType.DIPLOMACY_SHRINE
	};

	private ColonyBuilder() {
	}

	public static boolean tick(ServerLevel level, ColonyData colony) {
		ColonyProgress progress = colony.progress();
		for (ColonyBuilding building : progress.buildings()) {
			building.tickDisabled();
		}
		progress.setClaimRadius(Math.max(18 + completedQueenLevel(colony) * 6, com.formicfrontier.sim.ColonyRank.current(colony).claimRadius()));
		enqueueNextBuilding(colony);

		ColonyBuilding active = progress.firstIncomplete().orElse(null);
		if (active == null) {
			startQueuedBuilding(colony);
			active = progress.firstIncomplete().orElse(null);
		}
		if (active == null) {
			return false;
		}

		int builders = Math.max(1, colony.casteCount(com.formicfrontier.sim.AntCaste.WORKER));
		active.addConstructionProgress(8 + builders * 4 + colony.progress().culture().constructionBonus());
		colony.setCurrentTask("Building " + active.type().id() + " " + active.constructionProgress() + "%");
		if (active.complete()) {
			StructurePlacer.placeBuilding(level, active.pos(), active.type());
			ColonyLabelService.syncLabels(level, colony);
			colony.setCurrentTask("Completed " + active.type().id());
			colony.addEvent("Completed " + active.type().id() + " at " + active.pos().toShortString());
		}
		return true;
	}

	private static void enqueueNextBuilding(ColonyData colony) {
		ColonyProgress progress = colony.progress();
		if (!progress.buildQueue().isEmpty() || progress.firstIncomplete().isPresent()) {
			return;
		}
		for (BuildingType type : STARTER_SEQUENCE) {
			if (!progress.hasCompleted(type)) {
				progress.buildQueue().add(type);
				return;
			}
		}
		if (colony.progress().hasResearch(ResearchNode.RESIN_MASONRY.id()) && completedCount(colony, BuildingType.RESIN_DEPOT) < 1) {
			progress.buildQueue().add(BuildingType.RESIN_DEPOT);
			return;
		}
		if (colony.progress().hasResearch(ResearchNode.FUNGUS_SYMBIOSIS.id()) && completedCount(colony, BuildingType.FUNGUS_GARDEN) < 2) {
			progress.buildQueue().add(BuildingType.FUNGUS_GARDEN);
			return;
		}
		if (colony.progress().hasResearch(ResearchNode.VENOM_DRILLS.id()) && completedCount(colony, BuildingType.VENOM_PRESS) < 1) {
			progress.buildQueue().add(BuildingType.VENOM_PRESS);
			return;
		}
		BuildingType priorityBuilding = priorityExpansion(colony);
		if (priorityBuilding != null) {
			progress.buildQueue().add(priorityBuilding);
			return;
		}
		if (completedCount(colony, BuildingType.WATCH_POST) < 3) {
			progress.buildQueue().add(BuildingType.WATCH_POST);
			return;
		}
		progress.buildQueue().add(BuildingType.ROAD);
	}

	private static void startQueuedBuilding(ColonyData colony) {
		ColonyProgress progress = colony.progress();
		if (progress.buildQueue().isEmpty()) {
			return;
		}
		BuildingType type = progress.buildQueue().get(0);
		if (!canStart(colony, type)) {
			colony.setCurrentTask("Waiting for resources to build " + type.id());
			requestMissingCosts(colony, type);
			return;
		}
		consumeStartCost(colony, type);
		progress.buildQueue().remove(0);
		progress.addBuilding(ColonyBuilding.planned(type, siteFor(colony, type)));
		colony.addEvent("Started construction: " + type.id());
	}

	public static BlockPos siteFor(ColonyData colony, BuildingType type) {
		BlockPos origin = colony.origin();
		int existing = (int) colony.progress().buildingsView().stream().filter(building -> building.type() == type).count();
		return siteFor(origin, type, existing);
	}

	public static BlockPos siteFor(BlockPos origin, BuildingType type, int existing) {
		return switch (type) {
			case QUEEN_CHAMBER -> origin;
			case FOOD_STORE -> origin.offset(22 + existing * 12, 0, 0);
			case NURSERY -> origin.offset(-22 - existing * 12, 0, 0);
			case MINE -> origin.offset(0, 0, 22 + existing * 12);
			case CHITIN_FARM -> origin.offset(-22 - existing * 10, 0, 20 + existing * 7);
			case BARRACKS -> origin.offset(0, 0, -22 - existing * 12);
			case MARKET -> origin.offset(20 + existing * 10, 0, -20 - existing * 8);
			case DIPLOMACY_SHRINE -> origin.offset(-20 - existing * 10, 0, -20 - existing * 8);
			case WATCH_POST -> origin.offset(34 + existing * 8, 0, -28);
			case RESIN_DEPOT -> origin.offset(30 + existing * 10, 0, 16);
			case PHEROMONE_ARCHIVE -> origin.offset(-30 - existing * 10, 0, -4);
			case FUNGUS_GARDEN -> origin.offset(-30 - existing * 10, 0, 20);
			case VENOM_PRESS -> origin.offset(30 + existing * 10, 0, -4);
			case ARMORY -> origin.offset(4, 0, -34 - existing * 10);
			case ROAD -> origin.offset(existing * 4, 0, existing * 4);
		};
	}

	private static boolean canStart(ColonyData colony, BuildingType type) {
		if (type == BuildingType.VENOM_PRESS && !colony.progress().hasResearch(ResearchNode.VENOM_DRILLS.id())) {
			return false;
		}
		for (ResourceType resource : ResourceType.values()) {
			if (colony.resource(resource) < effectiveCost(colony, type, resource)) {
				return false;
			}
		}
		return true;
	}

	private static void consumeStartCost(ColonyData colony, BuildingType type) {
		for (ResourceType resource : ResourceType.values()) {
			colony.addResource(resource, -effectiveCost(colony, type, resource));
		}
	}

	private static void requestMissingCosts(ColonyData colony, BuildingType type) {
		for (ResourceType resource : ResourceType.values()) {
			int missing = effectiveCost(colony, type, resource) - colony.resource(resource);
			if (missing > 0) {
				ColonyLogistics.requestResource(colony, type, resource, missing, "construction " + type.id());
			}
		}
	}

	private static int effectiveCost(ColonyData colony, BuildingType type, ResourceType resource) {
		int cost = type.cost(resource);
		if (cost > 0 && colony.progress().hasResearch(ResearchNode.RESIN_MASONRY.id())) {
			cost = Math.max(1, (int) Math.ceil(cost * 0.9));
		}
		return cost;
	}

	private static int completedQueenLevel(ColonyData colony) {
		return colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.QUEEN_CHAMBER && building.complete())
				.mapToInt(ColonyBuilding::level)
				.max()
				.orElse(0);
	}

	private static BuildingType priorityExpansion(ColonyData colony) {
		return switch (colony.prioritiesView().getFirst()) {
			case FOOD -> completedCount(colony, BuildingType.FOOD_STORE) < 2 ? BuildingType.FOOD_STORE : BuildingType.CHITIN_FARM;
			case ORE -> completedCount(colony, BuildingType.MINE) < 2 ? BuildingType.MINE : BuildingType.ROAD;
			case CHITIN -> completedCount(colony, BuildingType.CHITIN_FARM) < 3 ? BuildingType.CHITIN_FARM : BuildingType.NURSERY;
			case DEFENSE -> completedCount(colony, BuildingType.WATCH_POST) < 4 ? BuildingType.WATCH_POST : BuildingType.BARRACKS;
		};
	}

	private static int completedCount(ColonyData colony, BuildingType type) {
		return (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == type && building.complete())
				.count();
	}
}
