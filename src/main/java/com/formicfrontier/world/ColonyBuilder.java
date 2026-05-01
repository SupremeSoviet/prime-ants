package com.formicfrontier.world;

import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ColonyProgress;
import com.formicfrontier.sim.ColonyRank;
import com.formicfrontier.sim.ColonyRequest;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public final class ColonyBuilder {
	private static final int REPAIR_START_PROGRESS = 55;
	private static final int MAX_AUTO_UPGRADE_LEVEL = 2;
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
	private static final BuildingType[] UPGRADE_ORDER = {
			BuildingType.MARKET,
			BuildingType.RESIN_DEPOT,
			BuildingType.PHEROMONE_ARCHIVE,
			BuildingType.FOOD_STORE,
			BuildingType.NURSERY,
			BuildingType.MINE,
			BuildingType.BARRACKS,
			BuildingType.QUEEN_CHAMBER
	};

	private ColonyBuilder() {
	}

	public static boolean tick(ServerLevel level, ColonyData colony) {
		ColonyProgress progress = colony.progress();
		progress.setClaimRadius(Math.max(18 + completedQueenLevel(colony) * 6, com.formicfrontier.sim.ColonyRank.current(colony).claimRadius()));
		if (repairDamagedBuilding(level, colony)) {
			return true;
		}
		BuildingType endgameProject = nextEndgameProject(colony);
		if (endgameProject == null && maybeStartUpgrade(level, colony)) {
			return true;
		}
		enqueueNextBuilding(colony, endgameProject);

		ColonyBuilding active = progress.firstIncomplete().orElse(null);
		if (active == null) {
			if (startQueuedBuilding(level, colony)) {
				return true;
			}
			active = progress.firstIncomplete().orElse(null);
		}
		if (active == null) {
			return false;
		}

		int builders = Math.max(1, colony.casteCount(com.formicfrontier.sim.AntCaste.WORKER));
		active.addConstructionProgress(8 + builders * 4 + colony.progress().culture().constructionBonus());
		colony.setCurrentTask("Building " + active.type().id() + " " + active.constructionProgress() + "%");
		if (active.complete()) {
			StructurePlacer.placeBuilding(level, active.pos(), active.type(), active.visualStage(), colony.progress().culture());
			ColonyLabelService.syncLabels(level, colony);
			colony.setCurrentTask("Completed " + active.type().id());
			colony.addEvent("Completed " + active.type().id() + " at " + active.pos().toShortString());
		} else {
			StructurePlacer.placeBuilding(level, active.pos(), active.type(), active.visualStage(), colony.progress().culture());
		}
		return true;
	}

	private static boolean repairDamagedBuilding(ServerLevel level, ColonyData colony) {
		ColonyBuilding damaged = colony.progress().buildings().stream()
				.filter(ColonyBuilding::damaged)
				.findFirst()
				.orElse(null);
		if (damaged == null) {
			return false;
		}

		if (damaged.complete()) {
			if (!repairMaterialsReady(colony, damaged)) {
				requestRepairCost(colony, damaged);
				colony.setCurrentTask("Waiting for chitin repair supplies for " + damaged.type().id());
				return true;
			}
			consumeRepairCost(colony, damaged);
			damaged.beginRepair(REPAIR_START_PROGRESS);
			colony.setCurrentTask("Repairing " + damaged.type().id() + " " + damaged.constructionProgress() + "%");
			colony.addEvent("Repair crew started on " + damaged.type().id());
			StructurePlacer.placeBuilding(level, damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());
			ColonyLabelService.syncLabels(level, colony);
			return true;
		}

		int repairers = Math.max(1, colony.casteCount(com.formicfrontier.sim.AntCaste.WORKER));
		boolean finished = damaged.repair(10 + repairers * 4 + colony.progress().culture().constructionBonus());
		if (finished) {
			colony.setCurrentTask("Repaired " + damaged.type().id());
			colony.addEvent("Repaired " + damaged.type().id() + " at " + damaged.pos().toShortString());
			StructurePlacer.placeBuilding(level, damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());
			ColonyLabelService.syncLabels(level, colony);
		} else {
			colony.setCurrentTask("Repairing " + damaged.type().id() + " " + damaged.constructionProgress() + "%");
			StructurePlacer.placeBuilding(level, damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());
		}
		return true;
	}

	private static boolean repairMaterialsReady(ColonyData colony, ColonyBuilding building) {
		return colony.resource(ResourceType.CHITIN) >= repairCost(building)
				|| repairRequest(colony, building).filter(ColonyRequest::complete).isPresent();
	}

	private static void consumeRepairCost(ColonyData colony, ColonyBuilding building) {
		int cost = repairCost(building);
		Optional<ColonyRequest> request = repairRequest(colony, building);
		if (colony.resource(ResourceType.CHITIN) >= cost) {
			colony.addResource(ResourceType.CHITIN, -cost);
			request.ifPresent(colony.progress().requests()::remove);
			return;
		}
		request.filter(ColonyRequest::complete)
				.ifPresent(colony.progress().requests()::remove);
	}

	private static void requestRepairCost(ColonyData colony, ColonyBuilding building) {
		int missing = Math.max(0, repairCost(building) - colony.resource(ResourceType.CHITIN));
		if (missing > 0 && repairRequest(colony, building).isEmpty()) {
			ColonyLogistics.requestResource(colony, building.type(), ResourceType.CHITIN, missing, repairReason(building));
		}
	}

	private static Optional<ColonyRequest> repairRequest(ColonyData colony, ColonyBuilding building) {
		String reason = repairReason(building);
		return colony.progress().requests().stream()
				.filter(request -> request.building() == building.type()
						&& request.resource() == ResourceType.CHITIN
						&& request.reason().equals(reason))
				.findFirst();
	}

	private static String repairReason(ColonyBuilding building) {
		return "repair " + building.type().id();
	}

	private static int repairCost(ColonyBuilding building) {
		return Math.max(4, building.type().chitinCost() / 2 + building.level() * 2);
	}

	private static boolean maybeStartUpgrade(ServerLevel level, ColonyData colony) {
		ColonyProgress progress = colony.progress();
		if (!progress.buildQueue().isEmpty() || progress.firstIncomplete().isPresent()) {
			return false;
		}
		ColonyBuilding candidate = upgradeCandidate(colony).orElse(null);
		if (candidate == null || !canUpgrade(colony, candidate)) {
			return false;
		}
		consumeUpgradeCost(colony, candidate);
		candidate.upgrade();
		StructurePlacer.placeBuilding(level, candidate.pos(), candidate.type(), candidate.visualStage(), colony.progress().culture());
		ColonyLabelService.syncLabels(level, colony);
		colony.setCurrentTask("Upgrading " + candidate.type().id() + " to level " + candidate.level());
		colony.addEvent("Started upgrade: " + candidate.type().id() + " level " + candidate.level());
		return true;
	}

	private static Optional<ColonyBuilding> upgradeCandidate(ColonyData colony) {
		for (BuildingType type : UPGRADE_ORDER) {
			Optional<ColonyBuilding> candidate = colony.progress().buildings().stream()
					.filter(building -> building.type() == type)
					.filter(ColonyBuilding::complete)
					.filter(building -> !building.damaged())
					.filter(building -> building.level() < MAX_AUTO_UPGRADE_LEVEL)
					.findFirst();
			if (candidate.isPresent()) {
				return candidate;
			}
		}
		return Optional.empty();
	}

	private static boolean canUpgrade(ColonyData colony, ColonyBuilding building) {
		for (ResourceType resource : ResourceType.values()) {
			if (colony.resource(resource) < upgradeCost(colony, building, resource)) {
				return false;
			}
		}
		return true;
	}

	private static void consumeUpgradeCost(ColonyData colony, ColonyBuilding building) {
		for (ResourceType resource : ResourceType.values()) {
			colony.addResource(resource, -upgradeCost(colony, building, resource));
		}
	}

	private static int upgradeCost(ColonyData colony, ColonyBuilding building, ResourceType resource) {
		int base = effectiveCost(colony, building.type(), resource);
		int cost = (int) Math.ceil(base * 0.6);
		if (resource == ResourceType.RESIN) {
			cost += 10 + building.level() * 4;
		}
		if (resource == ResourceType.KNOWLEDGE && building.type() == BuildingType.PHEROMONE_ARCHIVE) {
			cost += 8;
		}
		return cost;
	}

	private static void enqueueNextBuilding(ColonyData colony, BuildingType endgameProject) {
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
		if (endgameProject != null) {
			progress.buildQueue().add(endgameProject);
			colony.setCurrentTask("Planning endgame project: " + endgameProject.id().replace('_', ' '));
			colony.addEvent("Endgame project planned: " + endgameProject.id());
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

	private static boolean startQueuedBuilding(ServerLevel level, ColonyData colony) {
		ColonyProgress progress = colony.progress();
		if (progress.buildQueue().isEmpty()) {
			return false;
		}
		BuildingType type = progress.buildQueue().get(0);
		if (!canStart(colony, type)) {
			colony.setCurrentTask("Waiting for resources to build " + type.id());
			requestMissingCosts(colony, type);
			return true;
		}
		clearConstructionRequests(colony, type);
		consumeStartCost(colony, type);
		progress.buildQueue().remove(0);
		ColonyBuilding planned = ColonyBuilding.planned(type, siteFor(colony, type));
		progress.addBuilding(planned);
		StructurePlacer.placeBuilding(level, planned.pos(), planned.type(), planned.visualStage(), colony.progress().culture());
		ColonyLabelService.syncLabels(level, colony);
		colony.setCurrentTask("Started construction: " + type.id());
		colony.addEvent("Started construction: " + type.id());
		return true;
	}

	public static BlockPos siteFor(ColonyData colony, BuildingType type) {
		BlockPos origin = colony.origin();
		int existing = (int) colony.progress().buildingsView().stream().filter(building -> building.type() == type).count();
		return siteFor(origin, type, existing);
	}

	public static BlockPos siteFor(BlockPos origin, BuildingType type, int existing) {
		return switch (type) {
			case QUEEN_CHAMBER -> origin;
			case FOOD_STORE -> origin.offset(38 + existing * 20, 0, 0);
			case NURSERY -> origin.offset(-38 - existing * 20, 0, 0);
			case MINE -> origin.offset(0, 0, 38 + existing * 20);
			case CHITIN_FARM -> origin.offset(-38 - existing * 18, 0, 34 + existing * 12);
			case BARRACKS -> origin.offset(0, 0, -38 - existing * 20);
			case MARKET -> origin.offset(34 + existing * 18, 0, -34 - existing * 14);
			case DIPLOMACY_SHRINE -> origin.offset(-34 - existing * 18, 0, -34 - existing * 14);
			case WATCH_POST -> origin.offset(56 + existing * 14, 0, -46);
			case RESIN_DEPOT -> origin.offset(50 + existing * 18, 0, 28);
			case PHEROMONE_ARCHIVE -> origin.offset(-50 - existing * 18, 0, -8);
			case FUNGUS_GARDEN -> origin.offset(-50 - existing * 18, 0, 34);
			case VENOM_PRESS -> origin.offset(50 + existing * 18, 0, -8);
			case ARMORY -> origin.offset(6, 0, -56 - existing * 18);
			case GREAT_MOUND -> origin;
			case QUEEN_VAULT -> origin;
			case TRADE_HUB -> origin.offset(44 + existing * 18, 0, -32 - existing * 12);
			case ROAD -> origin.offset(existing * 7, 0, existing * 7);
		};
	}

	private static BuildingType nextEndgameProject(ColonyData colony) {
		if (shouldQueueGreatMound(colony)) {
			return BuildingType.GREAT_MOUND;
		}
		if (shouldQueueQueenVault(colony)) {
			return BuildingType.QUEEN_VAULT;
		}
		if (shouldQueueTradeHub(colony)) {
			return BuildingType.TRADE_HUB;
		}
		return null;
	}

	private static boolean shouldQueueGreatMound(ColonyData colony) {
		return completedCount(colony, BuildingType.GREAT_MOUND) < 1
				&& ColonyRank.current(colony).ordinal() >= ColonyRank.CITADEL.ordinal()
				&& completedCount(colony, BuildingType.QUEEN_CHAMBER) > 0
				&& completedCount(colony, BuildingType.CHITIN_FARM) > 0
				&& completedCount(colony, BuildingType.MARKET) > 0
				&& completedCount(colony, BuildingType.PHEROMONE_ARCHIVE) > 0
				&& completedCount(colony, BuildingType.ARMORY) > 0
				&& completedCount(colony, BuildingType.DIPLOMACY_SHRINE) > 0;
	}

	private static boolean shouldQueueQueenVault(ColonyData colony) {
		return completedCount(colony, BuildingType.QUEEN_VAULT) < 1
				&& ColonyRank.current(colony).ordinal() >= ColonyRank.CITADEL.ordinal()
				&& completedCount(colony, BuildingType.GREAT_MOUND) > 0
				&& completedCount(colony, BuildingType.NURSERY) > 0
				&& completedCount(colony, BuildingType.PHEROMONE_ARCHIVE) > 0;
	}

	private static boolean shouldQueueTradeHub(ColonyData colony) {
		return completedCount(colony, BuildingType.TRADE_HUB) < 1
				&& ColonyRank.current(colony).ordinal() >= ColonyRank.CITADEL.ordinal()
				&& completedCount(colony, BuildingType.GREAT_MOUND) > 0
				&& completedCount(colony, BuildingType.QUEEN_VAULT) > 0
				&& completedCount(colony, BuildingType.MARKET) > 0
				&& completedCount(colony, BuildingType.DIPLOMACY_SHRINE) > 0;
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

	private static void clearConstructionRequests(ColonyData colony, BuildingType type) {
		String reason = "construction " + type.id();
		colony.progress().requests().removeIf(request -> request.building() == type && request.reason().equals(reason));
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
