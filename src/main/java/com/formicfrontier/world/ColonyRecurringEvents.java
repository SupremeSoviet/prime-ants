package com.formicfrontier.world;

import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.AntWorkState;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ColonyRank;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.ResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public final class ColonyRecurringEvents {
	public static final int EVENT_INTERVAL_TICKS = 20 * 180;
	private static final String RECURRING_EVENT_PREFIX = "Recurring event:";
	private static final int BROOD_FOOD_COST = 45;
	private static final int BROOD_CHITIN_COST = 12;
	private static final int BROOD_POPULATION_LIMIT = 42;
	private static final String BROOD_EVENT_PREFIX = "Recurring event: queen brood bloom";
	private static final int FAMINE_MIN_HELP_AMOUNT = 36;
	private static final String FAMINE_EVENT_PREFIX = "Recurring event: famine warning";
	public static final String FAMINE_REASON = "famine food stores";
	private static final int MIGRATION_POPULATION_THRESHOLD = 34;
	private static final int MIGRATION_HELP_AMOUNT = 42;
	private static final String MIGRATION_EVENT_PREFIX = "Recurring event: migration preparation";
	public static final String MIGRATION_REASON = "migration trail supplies";
	private static final int INVASION_WARNING_WINDOW_TICKS = 20 * 12;
	private static final int INVASION_HELP_AMOUNT = 32;
	private static final String INVASION_WARNING_EVENT_PREFIX = "Recurring event: invasion warning";
	public static final String INVASION_WARNING_REASON = "invasion defense supplies";
	private static final int TREATY_HELP_AMOUNT = 18;
	private static final String TREATY_OPPORTUNITY_EVENT_PREFIX = "Recurring event: treaty opportunity";
	public static final String TREATY_OPPORTUNITY_REASON = "treaty envoy supplies";
	private static final int EXPANSION_HELP_AMOUNT = 30;
	private static final String EXPANSION_OPPORTUNITY_EVENT_PREFIX = "Recurring event: expansion opportunity";
	public static final String EXPANSION_OPPORTUNITY_REASON = "expansion outpost materials";
	private static final String TRADE_CARAVAN_EVENT_PREFIX = "Recurring event: trade caravan";
	private static final int TRADE_CARAVAN_MIN_AMOUNT = 8;

	private ColonyRecurringEvents() {
	}

	public static boolean tick(ServerLevel level, ColonyData colony) {
		if (Boolean.getBoolean("formic.visualQa") || !colony.progress().playerAllied()) {
			return false;
		}
		if (hasOpenRecurringEventRequest(colony)) {
			return false;
		}
		if (colony.ageTicks() < nextRecurringEventAgeTicks(colony)) {
			return false;
		}
		if (colony.ageTicks() >= nextFamineEventAgeTicks(colony) && canFamineWarning(colony)) {
			return triggerFamineWarning(level, colony);
		}
		if (colony.ageTicks() >= nextMigrationEventAgeTicks(colony) && canMigrationPreparation(colony)) {
			return triggerMigrationPreparation(level, colony);
		}
		if (colony.ageTicks() >= nextTradeCaravanEventAgeTicks(colony) && canTradeCaravan(level, colony)) {
			return triggerTradeCaravan(level, colony);
		}
		if (colony.ageTicks() >= nextTreatyOpportunityEventAgeTicks(colony) && canTreatyOpportunity(level, colony)) {
			return triggerTreatyOpportunity(level, colony);
		}
		if (colony.ageTicks() >= nextExpansionOpportunityEventAgeTicks(colony) && canExpansionOpportunity(colony)) {
			return triggerExpansionOpportunity(level, colony);
		}
		if (colony.ageTicks() >= nextBroodEventAgeTicks(colony) && canQueenBroodBloom(colony)) {
			return triggerQueenBroodBloom(level, colony);
		}
		if (colony.ageTicks() >= nextInvasionWarningEventAgeTicks(colony) && canInvasionWarning(level, colony)) {
			return triggerInvasionWarning(level, colony);
		}
		return false;
	}

	private static int nextRecurringEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(RECURRING_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	public static boolean triggerExpansionOpportunity(ServerLevel level, ColonyData colony) {
		if (!canExpansionOpportunity(colony)) {
			return false;
		}
		BlockPos outpost = expansionOutpostGround(level, colony);
		ColonyLogistics.requestResource(colony, BuildingType.WATCH_POST, ResourceType.ORE, EXPANSION_HELP_AMOUNT, EXPANSION_OPPORTUNITY_REASON);
		colony.setCurrentTask("Recurring event: expansion opportunity, outpost materials needed");
		colony.addEvent(EXPANSION_OPPORTUNITY_EVENT_PREFIX + " marked claim-edge outpost at " + outpost.toShortString());
		placeExpansionOpportunityMarkers(level, colony, outpost);
		spawnExpansionCrew(level, colony, outpost);
		ColonyLabelService.syncLabels(level, colony);
		return true;
	}

	public static boolean completeExpansionOutpost(ServerLevel level, ColonyData colony) {
		BlockPos outpost = expansionOutpostGround(level, colony);
		int expandedClaim = expandedClaimRadius(colony, outpost);
		boolean hadBuilding = hasCompletedBuildingNear(colony, BuildingType.WATCH_POST, outpost, 4);
		if (!hadBuilding) {
			colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.WATCH_POST, outpost));
		}
		colony.progress().setClaimRadius(expandedClaim);
		colony.setCurrentTask("Expansion outpost secured; claim radius now " + expandedClaim);
		colony.addEvent("Expansion complete: claim-edge watch post secured at " + outpost.toShortString());
		placeCompletedExpansionOutpost(level, colony, outpost);
		spawnExpansionCrew(level, colony, outpost);
		ColonyLabelService.syncLabels(level, colony);
		return true;
	}

	public static boolean triggerTradeCaravan(ServerLevel level, ColonyData colony) {
		Optional<ColonyData> partner = tradeCaravanPartner(level, colony);
		if (partner.isEmpty() || !canTradeCaravan(colony, partner.get())) {
			return false;
		}
		ColonyData target = partner.get();
		ResourceType export = tradeCaravanResource(colony, null);
		ResourceType imported = tradeCaravanResource(target, export);
		int exportAmount = tradeCaravanAmount(export);
		int importAmount = tradeCaravanAmount(imported);

		colony.addResource(export, -exportAmount);
		target.addResource(export, exportAmount);
		target.addResource(imported, -importAmount);
		colony.addResource(imported, importAmount);
		colony.setCurrentTask("Recurring event: trade caravan with colony #" + target.id());
		target.setCurrentTask("Trade caravan exchanged " + imported.id() + " with colony #" + colony.id());
		colony.addEvent(TRADE_CARAVAN_EVENT_PREFIX + " exchanged " + exportAmount + " " + export.id()
				+ " for " + importAmount + " " + imported.id() + " with colony #" + target.id());
		target.addEvent(TRADE_CARAVAN_EVENT_PREFIX + " exchanged " + importAmount + " " + imported.id()
				+ " for " + exportAmount + " " + export.id() + " with colony #" + colony.id());
		placeTradeCaravanMarkers(level, colony, target, export, imported);
		spawnTradeCaravan(level, colony, target, export, imported);
		ColonyLabelService.syncLabels(level, colony);
		ColonyLabelService.syncLabels(level, target);
		return true;
	}

	public static boolean triggerTreatyOpportunity(ServerLevel level, ColonyData colony) {
		Optional<ColonyData> candidate = treatyCandidate(level, colony);
		if (candidate.isEmpty() || !canTreatyOpportunity(colony, candidate.get())) {
			return false;
		}
		ColonyData target = candidate.get();
		ColonyLogistics.requestResource(colony, BuildingType.DIPLOMACY_SHRINE, ResourceType.RESIN, TREATY_HELP_AMOUNT, TREATY_OPPORTUNITY_REASON);
		colony.setCurrentTask("Recurring event: treaty opportunity, envoy supplies needed for colony #" + target.id());
		colony.addEvent(TREATY_OPPORTUNITY_EVENT_PREFIX + " opened envoy route to colony #" + target.id());
		target.addEvent(TREATY_OPPORTUNITY_EVENT_PREFIX + " received envoy route from colony #" + colony.id());
		placeTreatyOpportunityMarkers(level, colony, target);
		spawnTreatyEnvoys(level, colony, target);
		ColonyLabelService.syncLabels(level, colony);
		ColonyLabelService.syncLabels(level, target);
		return true;
	}

	public static boolean triggerInvasionWarning(ServerLevel level, ColonyData colony) {
		Optional<ColonyData> threat = incomingThreat(level, colony);
		if (threat.isEmpty() || !canInvasionWarning(colony, threat.get())) {
			return false;
		}
		ColonyLogistics.requestResource(colony, BuildingType.BARRACKS, ResourceType.CHITIN, INVASION_HELP_AMOUNT, INVASION_WARNING_REASON);
		colony.setCurrentTask("Recurring event: invasion warning, defense contract open");
		colony.addEvent(INVASION_WARNING_EVENT_PREFIX + " marked threat from colony #" + threat.get().id());
		placeInvasionWarningMarkers(level, colony, threat.get());
		spawnInvasionGuards(level, colony, threat.get());
		ColonyLabelService.syncLabels(level, colony);
		return true;
	}

	public static boolean triggerQueenBroodBloom(ServerLevel level, ColonyData colony) {
		if (!canQueenBroodBloom(colony)) {
			return false;
		}
		colony.addResource(ResourceType.FOOD, -BROOD_FOOD_COST);
		colony.addResource(ResourceType.CHITIN, -BROOD_CHITIN_COST);
		colony.addCaste(AntCaste.WORKER, 2);
		colony.setCurrentTask("Recurring event: queen brood bloom");
		colony.addEvent(BROOD_EVENT_PREFIX + " raised new workers");
		placeBroodMarkers(level, colony);
		spawnBroodWorkers(level, colony);
		ColonyLabelService.syncLabels(level, colony);
		return true;
	}

	public static boolean triggerMigrationPreparation(ServerLevel level, ColonyData colony) {
		if (!canMigrationPreparation(colony)) {
			return false;
		}
		ColonyLogistics.requestResource(colony, BuildingType.MARKET, ResourceType.FOOD, MIGRATION_HELP_AMOUNT, MIGRATION_REASON);
		colony.setCurrentTask("Recurring event: migration preparation, scout trail marked");
		colony.addEvent(MIGRATION_EVENT_PREFIX + " marked a daughter-nest trail");
		placeMigrationMarkers(level, colony);
		spawnMigrationScouts(level, colony);
		ColonyLabelService.syncLabels(level, colony);
		return true;
	}

	public static boolean triggerFamineWarning(ServerLevel level, ColonyData colony) {
		if (!canFamineWarning(colony)) {
			return false;
		}
		ColonyLogistics.requestResource(colony, BuildingType.FOOD_STORE, ResourceType.FOOD, famineHelpAmount(colony), FAMINE_REASON);
		colony.setCurrentTask("Recurring event: famine warning, food contract open");
		colony.addEvent(FAMINE_EVENT_PREFIX + " opened food help contract");
		placeFamineMarkers(level, colony);
		ColonyLabelService.syncLabels(level, colony);
		return true;
	}

	private static boolean canQueenBroodBloom(ColonyData colony) {
		return colony.queenAlive()
				&& colony.population() < BROOD_POPULATION_LIMIT
				&& colony.resource(ResourceType.FOOD) >= BROOD_FOOD_COST + colony.upkeepPerEconomyTick() * 4
				&& colony.resource(ResourceType.CHITIN) >= BROOD_CHITIN_COST + 16
				&& colony.progress().hasCompleted(BuildingType.NURSERY);
	}

	private static boolean canFamineWarning(ColonyData colony) {
		return colony.queenAlive()
				&& colony.progress().hasCompleted(BuildingType.FOOD_STORE)
				&& colony.resource(ResourceType.FOOD) < famineFoodThreshold(colony)
				&& !hasOpenFamineRequest(colony);
	}

	private static boolean canMigrationPreparation(ColonyData colony) {
		return colony.queenAlive()
				&& ColonyRank.current(colony).ordinal() >= ColonyRank.HIVE.ordinal()
				&& colony.population() >= MIGRATION_POPULATION_THRESHOLD
				&& colony.progress().hasCompleted(BuildingType.NURSERY)
				&& colony.progress().hasCompleted(BuildingType.MARKET)
				&& !hasOpenMigrationRequest(colony);
	}

	private static boolean canTreatyOpportunity(ServerLevel level, ColonyData colony) {
		Optional<ColonyData> candidate = treatyCandidate(level, colony);
		return candidate.isPresent() && canTreatyOpportunity(colony, candidate.get());
	}

	private static boolean canTreatyOpportunity(ColonyData colony, ColonyData target) {
		DiplomacyState relation = colony.progress().relationTo(target.id());
		return colony.queenAlive()
				&& colony.progress().playerAllied()
				&& ColonyRank.current(colony).ordinal() >= ColonyRank.BURROW.ordinal()
				&& colony.progress().hasCompleted(BuildingType.DIPLOMACY_SHRINE)
				&& (relation == DiplomacyState.NEUTRAL || relation == DiplomacyState.ALLY)
				&& !target.progress().relationTo(colony.id()).hostile()
				&& !hasOpenTreatyOpportunityRequest(colony);
	}

	private static boolean canInvasionWarning(ServerLevel level, ColonyData colony) {
		Optional<ColonyData> threat = incomingThreat(level, colony);
		return threat.isPresent() && canInvasionWarning(colony, threat.get());
	}

	private static boolean canInvasionWarning(ColonyData colony, ColonyData threat) {
		return colony.queenAlive()
				&& colony.progress().hasCompleted(BuildingType.BARRACKS)
				&& isIncomingThreat(colony, threat)
				&& !hasOpenInvasionRequest(colony);
	}

	private static boolean canExpansionOpportunity(ColonyData colony) {
		return colony.queenAlive()
				&& colony.progress().playerAllied()
				&& ColonyRank.current(colony).ordinal() >= ColonyRank.HIVE.ordinal()
				&& colony.progress().hasCompleted(BuildingType.MARKET)
				&& colony.progress().hasCompleted(BuildingType.WATCH_POST)
				&& !hasCompletedExpansionOutpost(colony)
				&& !hasOpenExpansionOpportunityRequest(colony);
	}

	private static boolean canTradeCaravan(ServerLevel level, ColonyData colony) {
		return tradeCaravanPartner(level, colony)
				.filter(target -> canTradeCaravan(colony, target))
				.isPresent();
	}

	private static boolean canTradeCaravan(ColonyData colony, ColonyData target) {
		ResourceType export = tradeCaravanResource(colony, null);
		ResourceType imported = tradeCaravanResource(target, export);
		return colony.queenAlive()
				&& target.queenAlive()
				&& colony.progress().playerAllied()
				&& colony.progress().relationTo(target.id()) == DiplomacyState.ALLY
				&& target.progress().relationTo(colony.id()) == DiplomacyState.ALLY
				&& hasCompleted(colony, BuildingType.MARKET)
				&& hasCompleted(target, BuildingType.MARKET)
				&& colony.resource(export) >= tradeCaravanAmount(export)
				&& target.resource(imported) >= tradeCaravanAmount(imported);
	}

	private static int famineFoodThreshold(ColonyData colony) {
		return Math.max(24, colony.upkeepPerEconomyTick() * 3);
	}

	private static int famineHelpAmount(ColonyData colony) {
		return Math.max(FAMINE_MIN_HELP_AMOUNT, famineFoodThreshold(colony) - colony.resource(ResourceType.FOOD));
	}

	private static boolean hasOpenFamineRequest(ColonyData colony) {
		return colony.progress().requestsView().stream()
				.anyMatch(request -> !request.complete()
						&& request.building() == BuildingType.FOOD_STORE
						&& request.resource() == ResourceType.FOOD
						&& request.reason().equals(FAMINE_REASON));
	}

	private static boolean hasOpenMigrationRequest(ColonyData colony) {
		return colony.progress().requestsView().stream()
				.anyMatch(request -> !request.complete()
						&& request.building() == BuildingType.MARKET
						&& request.resource() == ResourceType.FOOD
						&& request.reason().equals(MIGRATION_REASON));
	}

	private static boolean hasOpenInvasionRequest(ColonyData colony) {
		return colony.progress().requestsView().stream()
				.anyMatch(request -> !request.complete()
						&& request.building() == BuildingType.BARRACKS
						&& request.resource() == ResourceType.CHITIN
						&& request.reason().equals(INVASION_WARNING_REASON));
	}

	private static boolean hasOpenTreatyOpportunityRequest(ColonyData colony) {
		return colony.progress().requestsView().stream()
				.anyMatch(request -> !request.complete()
						&& request.building() == BuildingType.DIPLOMACY_SHRINE
						&& request.resource() == ResourceType.RESIN
						&& request.reason().equals(TREATY_OPPORTUNITY_REASON));
	}

	private static boolean hasOpenExpansionOpportunityRequest(ColonyData colony) {
		return colony.progress().requestsView().stream()
				.anyMatch(request -> !request.complete()
						&& request.building() == BuildingType.WATCH_POST
						&& request.resource() == ResourceType.ORE
						&& request.reason().equals(EXPANSION_OPPORTUNITY_REASON));
	}

	private static boolean hasOpenRecurringEventRequest(ColonyData colony) {
		return hasOpenFamineRequest(colony)
				|| hasOpenMigrationRequest(colony)
				|| hasOpenInvasionRequest(colony)
				|| hasOpenTreatyOpportunityRequest(colony)
				|| hasOpenExpansionOpportunityRequest(colony);
	}

	private static boolean hasCompletedExpansionOutpost(ColonyData colony) {
		BlockPos firstOutpost = expansionOutpost(colony.origin(), ColonyRank.HIVE.claimRadius());
		BlockPos currentOutpost = expansionOutpost(colony);
		return hasCompletedBuildingNear(colony, BuildingType.WATCH_POST, firstOutpost, 6)
				|| hasCompletedBuildingNear(colony, BuildingType.WATCH_POST, currentOutpost, 6);
	}

	private static boolean hasCompletedBuildingNear(ColonyData colony, BuildingType type, BlockPos pos, int radius) {
		int radiusSquared = radius * radius;
		return colony.progress().buildingsView().stream()
				.anyMatch(building -> building.type() == type
						&& building.complete()
						&& horizontalDistanceSquared(building.pos(), pos) <= radiusSquared);
	}

	private static int nextBroodEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(BROOD_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static int nextFamineEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(FAMINE_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static int nextMigrationEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(MIGRATION_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static int nextInvasionWarningEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(INVASION_WARNING_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static int nextTreatyOpportunityEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(TREATY_OPPORTUNITY_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static int nextExpansionOpportunityEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(EXPANSION_OPPORTUNITY_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static int nextTradeCaravanEventAgeTicks(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.filter(event -> event.message().startsWith(TRADE_CARAVAN_EVENT_PREFIX))
				.mapToInt(com.formicfrontier.sim.ColonyEvent::ageTicks)
				.max()
				.orElse(0) + EVENT_INTERVAL_TICKS;
	}

	private static void placeBroodMarkers(ServerLevel level, ColonyData colony) {
		BlockPos nursery = nurseryCenter(colony);
		placeEventAttachmentTrail(level, nursery, ModBlocks.CHITIN_NODE, Blocks.HONEYCOMB_BLOCK);
		placeMarker(level, nursery.offset(0, 1, -2), ModBlocks.CHITIN_NODE);
		placeMarker(level, nursery.offset(-1, 1, -1), Blocks.HONEYCOMB_BLOCK);
		placeMarker(level, nursery.offset(1, 1, -1), Blocks.BONE_BLOCK);
		placeMarker(level, nursery.offset(-2, 1, 0), Blocks.HONEYCOMB_BLOCK);
		placeMarker(level, nursery.offset(2, 1, 0), ModBlocks.CHITIN_NODE);
		placeMarker(level, nursery.offset(0, 2, -1), Blocks.OCHRE_FROGLIGHT);
	}

	private static void placeMarker(ServerLevel level, BlockPos pos, Block block) {
		if (level.getBlockState(pos).isAir()) {
			StructurePlacer.safeSet(level, pos, block);
		}
	}

	private static void spawnBroodWorkers(ServerLevel level, ColonyData colony) {
		BlockPos nursery = nurseryCenter(colony);
		spawnWorkerAtFirstOpen(level, colony, nursery.offset(-3, 1, -6), nursery.offset(3, 1, -6), nursery.offset(0, 1, -7));
		spawnWorkerAtFirstOpen(level, colony, nursery.offset(-4, 1, 1), nursery.offset(4, 1, 1), nursery.offset(0, 1, 5));
	}

	private static void placeFamineMarkers(ServerLevel level, ColonyData colony) {
		BlockPos food = foodStoreCenter(colony);
		placeEventAttachmentTrail(level, food, ModBlocks.FOOD_NODE, Blocks.BROWN_MUSHROOM_BLOCK);
		placeMarker(level, food.offset(0, 1, -2), ModBlocks.FOOD_NODE);
		placeMarker(level, food.offset(-1, 1, -1), Blocks.BROWN_MUSHROOM_BLOCK);
		placeMarker(level, food.offset(1, 1, -1), Blocks.HAY_BLOCK);
		placeMarker(level, food.offset(-2, 1, 0), Blocks.RED_MUSHROOM_BLOCK);
		placeMarker(level, food.offset(2, 1, 0), Blocks.OCHRE_FROGLIGHT);
		placeMarker(level, food.offset(0, 2, -1), Blocks.RED_TERRACOTTA);
	}

	private static void placeMigrationMarkers(ServerLevel level, ColonyData colony) {
		BlockPos camp = ColonyService.anchorToSurface(level, migrationCamp(colony));
		placeMigrationTrail(level, colony, camp);
		StructurePlacer.safeSet(level, camp, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, camp.above(), Blocks.HAY_BLOCK);
		StructurePlacer.safeSet(level, camp.above(2), Blocks.OCHRE_FROGLIGHT);
		StructurePlacer.safeSet(level, camp.north(), Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, camp.south(), Blocks.PACKED_MUD);
		StructurePlacer.safeSet(level, camp.east(), Blocks.COARSE_DIRT);
		StructurePlacer.safeSet(level, camp.west(), Blocks.DIRT_PATH);
		for (BlockPos post : migrationScoutPosts(camp)) {
			StructurePlacer.safeSet(level, post, Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, post.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, post.above(2), Blocks.AIR);
		}
	}

	private static void placeExpansionOpportunityMarkers(ServerLevel level, ColonyData colony, BlockPos outpost) {
		placeExpansionTrail(level, colony, outpost);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				BlockPos ground = outpost.offset(x, 0, z);
				boolean edge = Math.abs(x) == 4 || Math.abs(z) == 4;
				boolean corner = Math.abs(x) == 4 && Math.abs(z) == 4;
				if (edge) {
					StructurePlacer.safeSet(level, ground, corner ? Blocks.ROOTED_DIRT : Blocks.DIRT_PATH);
				}
				StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
				StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
				StructurePlacer.safeSet(level, ground.above(3), Blocks.AIR);
			}
		}
		StructurePlacer.safeSet(level, outpost, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, outpost.above(), ModBlocks.WATCH_POST);
		StructurePlacer.safeSet(level, outpost.above(2), Blocks.COBBLED_DEEPSLATE_WALL);
		StructurePlacer.safeSet(level, outpost.above(3), Blocks.OCHRE_FROGLIGHT);
		StructurePlacer.safeSet(level, outpost.offset(-2, 0, -5), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, outpost.offset(0, 0, -5), Blocks.COARSE_DIRT);
		StructurePlacer.safeSet(level, outpost.offset(2, 0, -5), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, outpost.offset(-2, 1, -5), Blocks.IRON_ORE);
		StructurePlacer.safeSet(level, outpost.offset(2, 1, -5), ModBlocks.CHITIN_NODE);
		StructurePlacer.safeSet(level, outpost.offset(0, 1, 5), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, outpost.offset(-4, 1, 0), Blocks.OAK_FENCE);
		StructurePlacer.safeSet(level, outpost.offset(4, 1, 0), Blocks.OAK_FENCE);
		StructurePlacer.safeSet(level, outpost.offset(0, 1, -4), Blocks.OAK_FENCE);
		StructurePlacer.safeSet(level, outpost.offset(0, 1, 4), Blocks.OAK_FENCE);
	}

	private static void placeCompletedExpansionOutpost(ServerLevel level, ColonyData colony, BlockPos outpost) {
		placeExpansionTrail(level, colony, outpost);
		StructurePlacer.safeSet(level, outpost.above(3), Blocks.AIR);
		StructurePlacer.placeBuilding(level, outpost, BuildingType.WATCH_POST, BuildingVisualStage.COMPLETE, colony.progress().culture());
		for (BlockPos corner : new BlockPos[] {
				outpost.offset(-5, 0, -5),
				outpost.offset(5, 0, -5),
				outpost.offset(-5, 0, 5),
				outpost.offset(5, 0, 5)
		}) {
			StructurePlacer.safeSet(level, corner, Blocks.ROOTED_DIRT);
			StructurePlacer.safeSet(level, corner.above(), Blocks.OCHRE_FROGLIGHT);
		}
		for (BlockPos marker : new BlockPos[] {
				outpost.offset(-6, 0, 0),
				outpost.offset(6, 0, 0),
				outpost.offset(0, 0, -6),
				outpost.offset(0, 0, 6)
		}) {
			StructurePlacer.safeSet(level, marker, Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, marker.above(), Blocks.HONEYCOMB_BLOCK);
		}
	}

	private static void placeTreatyOpportunityMarkers(ServerLevel level, ColonyData colony, ColonyData target) {
		BlockPos camp = ColonyService.anchorToSurface(level, treatyOpportunityCamp(colony.origin(), target.origin()));
		placeTreatyOpportunityTrail(level, colony, target, camp);
		StructurePlacer.safeSet(level, camp, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, camp.above(), Blocks.HONEYCOMB_BLOCK);
		StructurePlacer.safeSet(level, camp.above(2), Blocks.AMETHYST_BLOCK);
		StructurePlacer.safeSet(level, camp.above(3), Blocks.CANDLE);
		StructurePlacer.safeSet(level, camp.north(), Blocks.MOSS_BLOCK);
		StructurePlacer.safeSet(level, camp.south(), Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, camp.east(), Blocks.PACKED_MUD);
		StructurePlacer.safeSet(level, camp.west(), Blocks.DIRT_PATH);
		for (BlockPos post : treatyEnvoyPosts(camp)) {
			StructurePlacer.safeSet(level, post.below(), Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, post, Blocks.AIR);
			StructurePlacer.safeSet(level, post.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, post.above(2), Blocks.AIR);
		}
	}

	private static void placeTreatyOpportunityTrail(ServerLevel level, ColonyData colony, ColonyData target, BlockPos camp) {
		placeTreatyPath(level, colony.origin(), camp, colony, target);
		placeTreatyPath(level, camp, target.origin(), colony, target);
	}

	private static void placeTradeCaravanMarkers(ServerLevel level, ColonyData colony, ColonyData target, ResourceType export, ResourceType imported) {
		BlockPos camp = ColonyService.anchorToSurface(level, tradeCaravanCamp(colony.origin(), target.origin()));
		placeTradeCaravanTrail(level, colony, target, camp);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) + Math.abs(z) <= 3) {
					BlockPos ground = camp.offset(x, 0, z);
					StructurePlacer.safeSet(level, ground, Math.abs(x) + Math.abs(z) <= 1 ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT);
					StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
					StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
				}
			}
		}
		StructurePlacer.safeSet(level, camp.above(), Blocks.BARREL);
		StructurePlacer.safeSet(level, camp.offset(-1, 1, 0), tradeCaravanBlock(export));
		StructurePlacer.safeSet(level, camp.offset(1, 1, 0), tradeCaravanBlock(imported));
		StructurePlacer.safeSet(level, camp.offset(0, 1, 1), Blocks.OCHRE_FROGLIGHT);
		StructurePlacer.safeSet(level, camp.offset(0, 1, -1), Blocks.HAY_BLOCK);
	}

	private static void placeTradeCaravanTrail(ServerLevel level, ColonyData colony, ColonyData target, BlockPos camp) {
		placeTradeCaravanPath(level, colony.origin(), camp, colony, target);
		placeTradeCaravanPath(level, camp, target.origin(), colony, target);
	}

	private static void placeTradeCaravanPath(ServerLevel level, BlockPos start, BlockPos end, ColonyData colony, ColonyData target) {
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 120) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, start) < 8 * 8 || horizontalDistanceSquared(current, end) < 8 * 8) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(colony, ground) || nearColonyBuilding(target, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 3 == 0 ? Blocks.PODZOL : Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
			if (steps % 10 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 20 == 0 ? Blocks.HONEYCOMB_BLOCK : Blocks.HAY_BLOCK);
			}
		}
	}

	private static void placeTreatyPath(ServerLevel level, BlockPos start, BlockPos end, ColonyData colony, ColonyData target) {
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 120) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, start) < 8 * 8 || horizontalDistanceSquared(current, end) < 8 * 8) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(colony, ground) || nearColonyBuilding(target, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 3 == 0 ? Blocks.MOSS_BLOCK : Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
			if (steps % 9 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 18 == 0 ? Blocks.AMETHYST_BLOCK : Blocks.HONEYCOMB_BLOCK);
			}
		}
	}

	private static void placeInvasionWarningMarkers(ServerLevel level, ColonyData colony, ColonyData threat) {
		BlockPos rally = ColonyService.anchorToSurface(level, invasionRally(colony, threat));
		int dx = Integer.compare(threat.origin().getX(), colony.origin().getX());
		int dz = Integer.compare(threat.origin().getZ(), colony.origin().getZ());
		int sideX = dz == 0 ? 0 : 1;
		int sideZ = dx == 0 ? 0 : 1;
		if (sideX == 0 && sideZ == 0) {
			sideZ = 1;
		}

		placeInvasionTrail(level, colony, threat, rally);
		StructurePlacer.safeSet(level, rally, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, rally.above(), Blocks.RED_TERRACOTTA);
		StructurePlacer.safeSet(level, rally.above(2), Blocks.BLACKSTONE);
		StructurePlacer.safeSet(level, rally.above(3), Blocks.CANDLE);
		StructurePlacer.safeSet(level, rally.offset(sideX, 0, sideZ), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, rally.offset(-sideX, 0, -sideZ), Blocks.POLISHED_DEEPSLATE);
		StructurePlacer.safeSet(level, rally.offset(sideX * 2, 0, sideZ * 2), ModBlocks.CHITIN_NODE);
		StructurePlacer.safeSet(level, rally.offset(-sideX * 2, 0, -sideZ * 2), Blocks.ROOTED_DIRT);
	}

	private static void placeInvasionTrail(ServerLevel level, ColonyData colony, ColonyData threat, BlockPos rally) {
		BlockPos current = colony.origin();
		int steps = 0;
		while ((current.getX() != rally.getX() || current.getZ() != rally.getZ()) && steps < 96) {
			if (current.getX() != rally.getX()) {
				current = current.offset(Integer.compare(rally.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(rally.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, colony.origin()) < 10 * 10 || nearColonyBuilding(colony, current)) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT);
			StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
			if (steps % 8 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 16 == 0 ? Blocks.BLACKSTONE : Blocks.RED_TERRACOTTA);
			}
		}
		BlockPos directionProof = rally.offset(
				Integer.compare(threat.origin().getX(), colony.origin().getX()) * 3,
				0,
				Integer.compare(threat.origin().getZ(), colony.origin().getZ()) * 3
		);
		StructurePlacer.safeSet(level, directionProof, Blocks.PODZOL);
		StructurePlacer.safeSet(level, directionProof.above(), Blocks.RED_TERRACOTTA);
	}

	private static void placeMigrationTrail(ServerLevel level, ColonyData colony, BlockPos camp) {
		BlockPos current = colony.origin();
		int steps = 0;
		while ((current.getX() != camp.getX() || current.getZ() != camp.getZ()) && steps < 96) {
			if (current.getX() != camp.getX()) {
				current = current.offset(Integer.compare(camp.getX(), current.getX()), 0, 0);
			}
			if (current.getZ() != camp.getZ()) {
				current = current.offset(0, 0, Integer.compare(camp.getZ(), current.getZ()));
			}
			steps++;
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(colony, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
			if (steps % 7 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 14 == 0 ? Blocks.HAY_BLOCK : Blocks.ROOTED_DIRT);
			}
		}
	}

	private static void placeExpansionTrail(ServerLevel level, ColonyData colony, BlockPos outpost) {
		BlockPos current = colony.origin();
		int steps = 0;
		while ((current.getX() != outpost.getX() || current.getZ() != outpost.getZ()) && steps < 96) {
			if (current.getX() != outpost.getX()) {
				current = current.offset(Integer.compare(outpost.getX(), current.getX()), 0, 0);
			}
			if (current.getZ() != outpost.getZ()) {
				current = current.offset(0, 0, Integer.compare(outpost.getZ(), current.getZ()));
			}
			steps++;
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (horizontalDistanceSquared(ground, colony.origin()) < 10 * 10 || nearColonyBuilding(colony, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
			if (steps % 8 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 16 == 0 ? Blocks.IRON_ORE : Blocks.ROOTED_DIRT);
			}
		}
	}

	private static void placeEventAttachmentTrail(ServerLevel level, BlockPos center, Block primary, Block secondary) {
		for (int z = -5; z <= -2; z++) {
			BlockPos path = center.offset(0, 0, z);
			StructurePlacer.safeSet(level, path, z % 2 == 0 ? primary : Blocks.DIRT_PATH);
			StructurePlacer.safeSet(level, path.above(), Blocks.AIR);
			StructurePlacer.safeSet(level, path.above(2), Blocks.AIR);
			if (z == -4 || z == -2) {
				StructurePlacer.safeSet(level, center.offset(-1, 0, z), secondary);
				StructurePlacer.safeSet(level, center.offset(1, 0, z), Blocks.COARSE_DIRT);
			}
		}
	}

	private static void spawnWorkerAtFirstOpen(ServerLevel level, ColonyData colony, BlockPos... candidates) {
		for (BlockPos candidate : candidates) {
			BlockPos pos = ColonyService.anchorToSurface(level, candidate).above();
			if (isOpenSpawn(level, pos)) {
				ColonyService.spawnAnt(level, pos, AntCaste.WORKER, colony.id());
				return;
			}
		}
	}

	private static void spawnExpansionCrew(ServerLevel level, ColonyData colony, BlockPos outpost) {
		spawnExpansionAnt(level, outpost.offset(-2, 1, -5), colony, AntCaste.MINER, AntWorkState.CARRYING_ORE);
		spawnExpansionAnt(level, outpost.offset(2, 1, -5), colony, AntCaste.WORKER, AntWorkState.CARRYING_CHITIN);
		spawnExpansionAnt(level, outpost.offset(0, 1, 5), colony, AntCaste.SCOUT, AntWorkState.PATROLLING);
	}

	private static void spawnExpansionAnt(ServerLevel level, BlockPos post, ColonyData colony, AntCaste caste, AntWorkState state) {
		BlockPos pos = ColonyService.anchorToSurface(level, post).above();
		if (!isOpenSpawn(level, pos)) {
			return;
		}
		AntEntity ant = ColonyService.spawnAnt(level, pos, caste, colony.id());
		if (ant != null) {
			ant.setWorkState(state);
			int dx = Integer.compare(colony.origin().getX(), pos.getX());
			int dz = Integer.compare(colony.origin().getZ(), pos.getZ());
			ant.setYRot(yawToward(dx, dz));
			ant.setYHeadRot(yawToward(dx, dz));
		}
	}

	private static void spawnMigrationScouts(ServerLevel level, ColonyData colony) {
		BlockPos camp = ColonyService.anchorToSurface(level, migrationCamp(colony));
		for (BlockPos post : migrationScoutPosts(camp)) {
			BlockPos pos = ColonyService.anchorToSurface(level, post).above();
			if (isOpenSpawn(level, pos)) {
				AntEntity scout = ColonyService.spawnAnt(level, pos, AntCaste.SCOUT, colony.id());
				if (scout != null) {
					scout.setWorkState(AntWorkState.PATROLLING);
				}
			}
		}
	}

	private static void spawnTreatyEnvoys(ServerLevel level, ColonyData colony, ColonyData target) {
		BlockPos camp = ColonyService.anchorToSurface(level, treatyOpportunityCamp(colony.origin(), target.origin()));
		BlockPos[] posts = treatyEnvoyPosts(camp);
		spawnTreatyEnvoy(level, posts[0], AntCaste.SCOUT, colony.id(), AntWorkState.CARRYING_RESIN, target.origin());
		spawnTreatyEnvoy(level, posts[1], AntCaste.WORKER, target.id(), AntWorkState.CARRYING_FUNGUS, colony.origin());
	}

	private static void spawnTradeCaravan(ServerLevel level, ColonyData colony, ColonyData target, ResourceType export, ResourceType imported) {
		BlockPos camp = ColonyService.anchorToSurface(level, tradeCaravanCamp(colony.origin(), target.origin()));
		spawnTradeCarrier(level, camp.offset(-2, 1, -1), colony.id(), export, target.origin());
		spawnTradeCarrier(level, camp.offset(2, 1, 1), target.id(), imported, colony.origin());
	}

	private static void spawnTradeCarrier(ServerLevel level, BlockPos post, int colonyId, ResourceType resource, BlockPos lookTarget) {
		BlockPos pos = ColonyService.anchorToSurface(level, post).above();
		if (!isOpenSpawn(level, pos)) {
			return;
		}
		AntEntity carrier = ColonyService.spawnAnt(level, pos, AntCaste.WORKER, colonyId);
		if (carrier != null) {
			carrier.setWorkState(AntWorkState.carrying(resource));
			int dx = Integer.compare(lookTarget.getX(), pos.getX());
			int dz = Integer.compare(lookTarget.getZ(), pos.getZ());
			carrier.setYRot(yawToward(dx, dz));
			carrier.setYHeadRot(yawToward(dx, dz));
		}
	}

	private static void spawnTreatyEnvoy(ServerLevel level, BlockPos post, AntCaste caste, int colonyId, AntWorkState state, BlockPos lookTarget) {
		BlockPos pos = ColonyService.anchorToSurface(level, post).above();
		if (!isOpenSpawn(level, pos)) {
			return;
		}
		AntEntity envoy = ColonyService.spawnAnt(level, pos, caste, colonyId);
		if (envoy != null) {
			envoy.setWorkState(state);
			int dx = Integer.compare(lookTarget.getX(), pos.getX());
			int dz = Integer.compare(lookTarget.getZ(), pos.getZ());
			envoy.setYRot(yawToward(dx, dz));
			envoy.setYHeadRot(yawToward(dx, dz));
		}
	}

	private static void spawnInvasionGuards(ServerLevel level, ColonyData colony, ColonyData threat) {
		BlockPos rally = ColonyService.anchorToSurface(level, invasionRally(colony, threat));
		int dx = Integer.compare(threat.origin().getX(), rally.getX());
		int dz = Integer.compare(threat.origin().getZ(), rally.getZ());
		BlockPos[] posts = {
				rally.offset(-1, 1, -1),
				rally.offset(0, 1, -2),
				rally.offset(1, 1, -1)
		};
		for (BlockPos post : posts) {
			BlockPos pos = ColonyService.anchorToSurface(level, post).above();
			if (isOpenSpawn(level, pos)) {
				AntEntity guard = ColonyService.spawnAnt(level, pos, AntCaste.SOLDIER, colony.id());
				if (guard != null) {
					guard.setWorkState(AntWorkState.PATROLLING);
					guard.setYRot(yawToward(dx, dz));
					guard.setYHeadRot(yawToward(dx, dz));
				}
			}
		}
	}

	private static boolean isOpenSpawn(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos.below()).isAir()
				&& level.getBlockState(pos).isAir()
				&& level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos.above(2)).isAir();
	}

	private static BlockPos nurseryCenter(ColonyData colony) {
		Optional<ColonyBuilding> nursery = colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.NURSERY && building.complete())
				.findFirst();
		return nursery.map(ColonyBuilding::pos).orElseGet(() -> ColonyBuilder.siteFor(colony, BuildingType.NURSERY));
	}

	private static BlockPos foodStoreCenter(ColonyData colony) {
		Optional<ColonyBuilding> foodStore = colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.FOOD_STORE && building.complete())
				.findFirst();
		return foodStore.map(ColonyBuilding::pos).orElseGet(() -> ColonyBuilder.siteFor(colony, BuildingType.FOOD_STORE));
	}

	private static BlockPos migrationCamp(ColonyData colony) {
		return colony.origin().offset(-34, 0, -30);
	}

	private static BlockPos expansionOutpostGround(ServerLevel level, ColonyData colony) {
		BlockPos expected = expansionOutpost(colony);
		if (isExpansionOutpostGround(level, expected)
				|| (!level.getBlockState(expected).isAir() && level.getBlockState(expected.above()).isAir())) {
			return expected;
		}
		BlockPos anchored = ColonyService.anchorToSurface(level, expected);
		for (BlockPos scan = anchored; scan.getY() >= expected.getY() - 8 && scan.getY() >= level.getMinY(); scan = scan.below()) {
			if (isExpansionOutpostGround(level, scan)) {
				return scan;
			}
		}
		return anchored;
	}

	private static boolean isExpansionOutpostGround(ServerLevel level, BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		return block == Blocks.DIRT_PATH
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.ROOTED_DIRT
				|| block == Blocks.PACKED_MUD
				|| block == ModBlocks.WATCH_POST;
	}

	public static BlockPos expansionOutpost(ColonyData colony) {
		int claimRadius = Math.max(ColonyRank.current(colony).claimRadius(), colony.progress().claimRadius());
		return expansionOutpost(colony.origin(), claimRadius);
	}

	public static BlockPos expansionOutpost(BlockPos origin, int claimRadius) {
		int edge = Math.min(42, Math.max(34, claimRadius + 4));
		return origin.offset(edge, 0, 12);
	}

	private static int expandedClaimRadius(ColonyData colony, BlockPos outpost) {
		int claimEdge = Math.max(
				Math.abs(outpost.getX() - colony.origin().getX()),
				Math.abs(outpost.getZ() - colony.origin().getZ())
		);
		return Math.min(48, Math.max(colony.progress().claimRadius(), claimEdge + 4));
	}

	private static BlockPos[] migrationScoutPosts(BlockPos camp) {
		return new BlockPos[] {camp.offset(-2, 0, 0), camp.offset(2, 0, 0), camp.offset(0, 0, -2)};
	}

	private static Optional<ColonyData> incomingThreat(ServerLevel level, ColonyData colony) {
		return ColonySavedState.get(level.getServer()).colonies().stream()
				.filter(other -> other.id() != colony.id())
				.filter(other -> isIncomingThreat(colony, other))
				.min((first, second) -> Double.compare(first.origin().distSqr(colony.origin()), second.origin().distSqr(colony.origin())));
	}

	private static Optional<ColonyData> treatyCandidate(ServerLevel level, ColonyData colony) {
		return ColonySavedState.get(level.getServer()).colonies().stream()
				.filter(other -> other.id() != colony.id())
				.filter(other -> {
					DiplomacyState relation = colony.progress().relationTo(other.id());
					return relation == DiplomacyState.NEUTRAL || relation == DiplomacyState.ALLY;
				})
				.filter(other -> !other.progress().relationTo(colony.id()).hostile())
				.min((first, second) -> {
					int relationCompare = Integer.compare(treatyCandidatePriority(colony, first), treatyCandidatePriority(colony, second));
					if (relationCompare != 0) {
						return relationCompare;
					}
					return Double.compare(first.origin().distSqr(colony.origin()), second.origin().distSqr(colony.origin()));
				});
	}

	private static Optional<ColonyData> tradeCaravanPartner(ServerLevel level, ColonyData colony) {
		return ColonySavedState.get(level.getServer()).colonies().stream()
				.filter(other -> other.id() != colony.id())
				.filter(other -> colony.progress().relationTo(other.id()) == DiplomacyState.ALLY)
				.filter(other -> other.progress().relationTo(colony.id()) == DiplomacyState.ALLY)
				.filter(other -> !other.progress().relationTo(colony.id()).hostile())
				.min((first, second) -> Double.compare(first.origin().distSqr(colony.origin()), second.origin().distSqr(colony.origin())));
	}

	private static int treatyCandidatePriority(ColonyData colony, ColonyData target) {
		return colony.progress().relationTo(target.id()) == DiplomacyState.NEUTRAL ? 0 : 1;
	}

	private static boolean isIncomingThreat(ColonyData colony, ColonyData threat) {
		DiplomacyState relation = threat.progress().relationTo(colony.id());
		return relation.hostile()
				&& threat.progress().raidCooldown() > 0
				&& threat.progress().raidCooldown() <= INVASION_WARNING_WINDOW_TICKS
				&& militaryStrength(threat) >= 16;
	}

	private static int militaryStrength(ColonyData colony) {
		return colony.casteCount(AntCaste.SOLDIER) * 4
				+ colony.casteCount(AntCaste.MAJOR) * 8
				+ colony.casteCount(AntCaste.GIANT) * 16;
	}

	private static BlockPos invasionRally(ColonyData colony, ColonyData threat) {
		int dx = Integer.compare(threat.origin().getX(), colony.origin().getX());
		int dz = Integer.compare(threat.origin().getZ(), colony.origin().getZ());
		if (Math.abs(threat.origin().getX() - colony.origin().getX()) >= Math.abs(threat.origin().getZ() - colony.origin().getZ())) {
			dz = 0;
		} else {
			dx = 0;
		}
		if (dx == 0 && dz == 0) {
			dx = 1;
		}
		return colony.origin().offset(dx * 36, 0, dz * 36);
	}

	public static BlockPos treatyOpportunityCamp(BlockPos source, BlockPos target) {
		BlockPos midpoint = new BlockPos(
				(source.getX() + target.getX()) / 2,
				source.getY(),
				(source.getZ() + target.getZ()) / 2
		);
		int dx = Integer.compare(target.getX(), source.getX());
		int dz = Integer.compare(target.getZ(), source.getZ());
		int sideX = dz;
		int sideZ = dx == 0 && dz == 0 ? -1 : -dx;
		return midpoint.offset(sideX * 8, 0, sideZ * 8);
	}

	public static BlockPos tradeCaravanCamp(BlockPos source, BlockPos target) {
		BlockPos midpoint = new BlockPos(
				(source.getX() + target.getX()) / 2,
				source.getY(),
				(source.getZ() + target.getZ()) / 2
		);
		int dx = Integer.compare(target.getX(), source.getX());
		int dz = Integer.compare(target.getZ(), source.getZ());
		int sideX = dz;
		int sideZ = dx == 0 && dz == 0 ? -1 : -dx;
		return midpoint.offset(sideX * 11, 0, sideZ * 11);
	}

	private static BlockPos[] treatyEnvoyPosts(BlockPos camp) {
		return new BlockPos[] {camp.offset(-2, 1, 0), camp.offset(2, 1, 0)};
	}

	private static ResourceType tradeCaravanResource(ColonyData colony, ResourceType avoid) {
		ResourceType preferred = switch (colony.progress().culture()) {
			case AMBER -> ResourceType.FOOD;
			case LEAFCUTTER -> ResourceType.FUNGUS;
			case FIRE -> ResourceType.VENOM;
			case CARPENTER -> ResourceType.RESIN;
		};
		if (preferred != avoid) {
			return preferred;
		}
		return switch (preferred) {
			case FOOD -> ResourceType.RESIN;
			case RESIN -> ResourceType.CHITIN;
			case FUNGUS -> ResourceType.FOOD;
			case VENOM -> ResourceType.ORE;
			default -> ResourceType.FOOD;
		};
	}

	private static int tradeCaravanAmount(ResourceType resource) {
		return resource == ResourceType.VENOM ? TRADE_CARAVAN_MIN_AMOUNT / 2 : TRADE_CARAVAN_MIN_AMOUNT;
	}

	private static Block tradeCaravanBlock(ResourceType resource) {
		return switch (resource) {
			case FOOD -> Blocks.HAY_BLOCK;
			case ORE -> Blocks.IRON_ORE;
			case CHITIN -> ModBlocks.CHITIN_NODE;
			case RESIN -> Blocks.HONEY_BLOCK;
			case FUNGUS -> Blocks.BROWN_MUSHROOM_BLOCK;
			case VENOM -> Blocks.SLIME_BLOCK;
			case KNOWLEDGE -> Blocks.AMETHYST_BLOCK;
		};
	}

	private static boolean hasCompleted(ColonyData colony, BuildingType type) {
		return colony.progress().buildingsView().stream()
				.anyMatch(building -> building.type() == type && building.complete());
	}

	private static boolean nearColonyBuilding(ColonyData colony, BlockPos pos) {
		for (ColonyBuilding building : colony.progress().buildingsView()) {
			if (horizontalDistanceSquared(pos, building.pos()) < 7 * 7) {
				return true;
			}
		}
		return false;
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static float yawToward(int dx, int dz) {
		if (dx > 0) {
			return -90.0f;
		}
		if (dx < 0) {
			return 90.0f;
		}
		return dz > 0 ? 0.0f : 180.0f;
	}
}
