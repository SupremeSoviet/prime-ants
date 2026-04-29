package com.formicfrontier.world;

import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.network.ColonyUiPayload;
import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.registry.ModEntities;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ColonyProgress;
import com.formicfrontier.sim.ColonyRequest;
import com.formicfrontier.sim.ColonyRank;
import com.formicfrontier.sim.ColonyTradeCatalog;
import com.formicfrontier.sim.ColonyTrades;
import com.formicfrontier.sim.DiplomacyAction;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.NestChamber;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResearchState;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.sim.TaskPriority;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public final class ColonyService {
	private ColonyService() {
	}

	public static ColonyData createColony(ServerLevel level, BlockPos origin) {
		return createColony(level, origin, true);
	}

	public static ColonyData createColony(ServerLevel level, BlockPos origin, boolean playerAllied) {
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = new ColonyData(savedState.nextId(), origin);
		colony.setProgress(playerAllied ? ColonyProgress.allied(colony.id()) : ColonyProgress.rival(colony.id(), ColonyCulture.rivalFor(colony.id())));
		seedEconomy(colony);
		colony.addEvent("Colony founded at " + origin.toShortString());
		placeNest(level, colony);
		spawnStarterCastes(level, colony);
		for (ColonyData existing : savedState.colonies()) {
			DiplomacyState relation = existing.progress().playerAllied() == colony.progress().playerAllied() ? DiplomacyState.NEUTRAL : DiplomacyState.RIVAL;
			existing.progress().setRelation(colony.id(), relation);
			colony.progress().setRelation(existing.id(), relation);
		}
		savedState.put(colony);
		ColonyLabelService.syncLabels(level, colony);
		return colony;
	}

	public static AntEntity spawnAnt(ServerLevel level, BlockPos pos, AntCaste caste) {
		return spawnAnt(level, pos, caste, nearestColony(level, pos, 96).map(ColonyData::id).orElse(0));
	}

	public static AntEntity spawnAnt(ServerLevel level, BlockPos pos, AntCaste caste, int colonyId) {
		AntEntity ant = ModEntities.ANT.create(level, EntitySpawnReason.TRIGGERED);
		if (ant == null) {
			return null;
		}
		ant.setCaste(caste);
		ant.setColonyId(colonyId);
		applyAntName(ant, caste, colonyId, false);
		ant.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		ant.setYRot(level.getRandom().nextFloat() * 360.0f);
		ant.setXRot(0.0f);
		level.addFreshEntity(ant);
		return ant;
	}

	public static Optional<ColonyData> colony(ServerLevel level, int colonyId) {
		if (colonyId <= 0) {
			return Optional.empty();
		}
		return ColonySavedState.get(level.getServer()).colony(colonyId);
	}

	public static Optional<ColonyData> nearestColony(ServerLevel level, BlockPos pos, int maxDistance) {
		return ColonySavedState.get(level.getServer()).nearestColony(pos, maxDistance);
	}

	public static boolean areHostile(ServerLevel level, int firstId, int secondId) {
		return RaidPlanner.areHostile(level, firstId, secondId);
	}

	public static boolean trade(ServerPlayer player, String offerId) {
		ServerLevel level = player.level();
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = savedState.nearestColony(player.blockPosition(), 96).orElse(null);
		if (colony == null) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("formic_frontier.feedback.no_colony"), true);
			return false;
		}
		ColonyTradeCatalog.TradeResult result = ColonyTradeCatalog.execute(player, colony, offerId);
		if (result.success()) {
			savedState.setDirty();
		}
		openColonyScreen(player, colony, "Trade", result.message());
		return result.success();
	}

	public static boolean setTopPriority(ServerPlayer player, String priorityId) {
		TaskPriority priority;
		try {
			priority = TaskPriority.fromId(priorityId);
		} catch (IllegalArgumentException exception) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unknown instinct: " + priorityId), true);
			return false;
		}

		ServerLevel level = player.level();
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = savedState.nearestColony(player.blockPosition(), 96).orElse(null);
		if (colony == null) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("formic_frontier.feedback.no_colony"), true);
			return false;
		}

		List<TaskPriority> priorities = new ArrayList<>(List.of(TaskPriority.values()));
		priorities.remove(priority);
		priorities.add(0, priority);
		colony.setPriorities(priorities);
		colony.setCurrentTask("Colony instinct biased toward " + priority.id());
		colony.addEvent("Instinct changed to " + priority.id() + " by " + player.getScoreboardName());
		savedState.setDirty();
		openColonyScreen(player, colony, "Instinct", "Top colony instinct: " + priority.id());
		return true;
	}

	public static boolean performDiplomacy(ServerPlayer player, String actionId) {
		return performDiplomacy(player, actionId, 0);
	}

	public static boolean performDiplomacy(ServerPlayer player, String actionId, int targetColonyId) {
		DiplomacyAction action;
		try {
			action = DiplomacyAction.fromId(actionId);
		} catch (IllegalArgumentException exception) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unknown diplomacy action: " + actionId), true);
			return false;
		}

		ServerLevel level = player.level();
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = savedState.nearestColony(player.blockPosition(), 128).orElse(null);
		if (colony == null) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("formic_frontier.feedback.no_colony"), true);
			return false;
		}
		if (ColonyRank.current(colony).ordinal() < action.minRank().ordinal()) {
			openColonyScreen(player, colony, "Diplomacy", action.label() + " requires colony rank " + action.minRank().displayName());
			return false;
		}

		ColonyData target = targetColonyId > 0
				? savedState.colony(targetColonyId).filter(other -> other.id() != colony.id()).orElse(null)
				: diplomacyTarget(savedState, colony).orElse(null);
		if (target == null) {
			openColonyScreen(player, colony, "Diplomacy", "No known colony to target.");
			return false;
		}
		int tokenCost = diplomacyTokenCost(colony, action);
		if (!hasItems(player, com.formicfrontier.registry.ModItems.PHEROMONE_TOKEN, tokenCost)
				|| !hasItems(player, com.formicfrontier.registry.ModItems.PHEROMONE_DUST, action.dustCost())
				|| !hasItems(player, com.formicfrontier.registry.ModItems.COLONY_SEAL, action.sealCost())
				|| !hasItems(player, com.formicfrontier.registry.ModItems.WAR_BANNER, action.bannerCost())) {
			openColonyScreen(player, colony, "Diplomacy", "Need " + diplomacyCostText(action));
			return false;
		}
		removeItems(player, com.formicfrontier.registry.ModItems.PHEROMONE_TOKEN, tokenCost);
		removeItems(player, com.formicfrontier.registry.ModItems.PHEROMONE_DUST, action.dustCost());
		removeItems(player, com.formicfrontier.registry.ModItems.COLONY_SEAL, action.sealCost());
		removeItems(player, com.formicfrontier.registry.ModItems.WAR_BANNER, action.bannerCost());

		DiplomacyState before = colony.progress().relationTo(target.id());
		DiplomacyState after = action.apply(before);
		colony.progress().setRelation(target.id(), after);
		target.progress().setRelation(colony.id(), after);
		colony.progress().addReputation(action.reputationDelta());
		colony.addEvent(player.getScoreboardName() + " performed " + action.id() + " toward colony #" + target.id() + ": " + before.id() + " -> " + after.id());
		target.addEvent("Diplomacy from colony #" + colony.id() + ": " + before.id() + " -> " + after.id());
		colony.setCurrentTask("Diplomacy: " + action.label() + " with colony #" + target.id());
		savedState.setDirty();
		openColonyScreen(player, colony, "Diplomacy", action.label() + ": colony #" + target.id() + " is now " + after.id());
		return true;
	}

	public static boolean startResearch(ServerPlayer player, String nodeId) {
		ServerLevel level = player.level();
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = savedState.nearestColony(player.blockPosition(), 128).orElse(null);
		if (colony == null) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("formic_frontier.feedback.no_colony"), true);
			return false;
		}
		ColonyLogistics.StartResearchResult result;
		try {
			result = ColonyLogistics.startResearch(colony, nodeId);
		} catch (IllegalArgumentException exception) {
			openColonyScreen(player, colony, "Research", "Unknown research: " + nodeId);
			return false;
		}
		savedState.setDirty();
		openColonyScreen(player, colony, "Research", result.message());
		return result.started();
	}

	public static boolean handleAntInteraction(ServerPlayer player, AntEntity ant, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ColonySavedState savedState = ColonySavedState.get(player.level().getServer());
		ColonyData colony = savedState.colony(ant.colonyId()).orElseGet(() -> savedState.nearestColony(ant.blockPosition(), 96).orElse(null));
		if (colony == null) {
			return false;
		}
		if (stack.isEmpty()) {
			openColonyScreen(player, colony, "Overview", "Inspecting " + ant.caste().id() + " ant.");
			return true;
		}

		if (stack.is(com.formicfrontier.registry.ModItems.ROYAL_JELLY) && ant.caste() == AntCaste.QUEEN) {
			consumeOne(player, stack);
			ant.heal(24.0f);
			colony.setQueenHealth(Math.max(colony.queenHealth(), Math.round(ant.getHealth())));
			colony.progress().addReputation(6);
			colony.addEvent(player.getScoreboardName() + " fed royal jelly to the queen");
			colony.setCurrentTask("Queen invigorated by royal jelly");
			savedState.setDirty();
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("The queen accepts the royal jelly. Reputation increased."), true);
			return true;
		}

		if (stack.is(com.formicfrontier.registry.ModItems.RAW_BIOMASS) || stack.is(Items.WHEAT)) {
			consumeOne(player, stack);
			colony.addResource(ResourceType.FOOD, stack.is(Items.WHEAT) ? 3 : 8);
			colony.progress().addReputation(1);
			ant.heal(2.0f);
			colony.addEvent(player.getScoreboardName() + " hand-fed workers");
			colony.setCurrentTask("Food donation received");
			savedState.setDirty();
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("The colony stores your food donation."), true);
			return true;
		}

		if (stack.is(com.formicfrontier.registry.ModItems.CHITIN_SHARD)) {
			consumeOne(player, stack);
			colony.addResource(ResourceType.CHITIN, 3);
			colony.progress().addReputation(1);
			ant.heal(4.0f);
			colony.addEvent(player.getScoreboardName() + " supplied chitin for repairs");
			colony.setCurrentTask("Chitin repairs underway");
			savedState.setDirty();
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("The ant folds the chitin into colony stores."), true);
			return true;
		}

		if (stack.is(com.formicfrontier.registry.ModItems.PHEROMONE_DUST)) {
			consumeOne(player, stack);
			TaskPriority priority = priorityForCaste(ant.caste());
			List<TaskPriority> priorities = new ArrayList<>(List.of(TaskPriority.values()));
			priorities.remove(priority);
			priorities.add(0, priority);
			colony.setPriorities(priorities);
			colony.addEvent(player.getScoreboardName() + " marked a " + ant.caste().id() + " with pheromone dust");
			colony.setCurrentTask("Pheromone instinct: " + priority.id());
			savedState.setDirty();
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Pheromone instinct set: " + priority.id()), true);
			return true;
		}

		return false;
	}

	public static void openColonyScreen(ServerPlayer player, ColonyData colony) {
		openColonyScreen(player, colony, "Overview", "");
	}

	public static void openColonyScreen(ServerPlayer player, ColonyData colony, String initialTab, String feedbackMessage) {
		ServerPlayNetworking.send(player, new ColonyUiPayload(ColonyUiSnapshot.from(colony, initialTab, feedbackMessage)));
	}

	public static String tabletText(ColonyData colony) {
		return "=== Overview ===\n" + colony.statusText()
				+ "\n\n=== Instinct ===\n" + instinctText(colony)
				+ "\n\n=== Buildings ===\n" + buildingsText(colony)
				+ "\n\n=== Requests ===\n" + requestsText(colony)
				+ "\n\n=== Research ===\n" + researchText(colony)
				+ "\n\n" + ColonyTrades.tradeText(colony)
				+ "\n=== Diplomacy ===\n" + diplomacyText(colony)
				+ "\n=== Relations ===\n" + relationsText(colony)
				+ "\n\n=== Events ===\n" + eventsText(colony);
	}

	public static void depositWorkedResource(ServerLevel level, BlockPos pos, AntCaste caste, ResourceType resourceType, int amount) {
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		depositWorkedResource(level, pos, 0, caste, resourceType, amount);
	}

	public static void depositWorkedResource(ServerLevel level, BlockPos pos, int colonyId, AntCaste caste, ResourceType resourceType, int amount) {
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		Optional<ColonyData> target = colonyId > 0 ? savedState.colony(colonyId) : Optional.empty();
		target.or(() -> savedState.nearestColony(pos, 96)).ifPresent(colony -> {
			colony.addResource(resourceType, amount);
			colony.setCurrentTask(caste.id() + " delivered " + amount + " " + resourceType.id());
			colony.addEvent(caste.id() + " delivered " + resourceType.id());
			savedState.setDirty();
		});
	}

	public static boolean depositConstructionWork(ServerLevel level, BlockPos pos, int colonyId, AntCaste caste, int amount) {
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = (colonyId > 0 ? savedState.colony(colonyId) : Optional.<ColonyData>empty())
				.or(() -> savedState.nearestColony(pos, 128))
				.orElse(null);
		if (colony == null) {
			return false;
		}
		ColonyBuilding active = colony.progress().firstIncomplete().orElse(null);
		if (active == null) {
			return false;
		}
		active.addConstructionProgress(Math.max(1, amount));
		colony.setCurrentTask(caste.id() + " carries resin to " + active.type().id() + " " + active.constructionProgress() + "%");
		if (active.complete()) {
			StructurePlacer.placeBuilding(level, active.pos(), active.type());
			ColonyLabelService.syncLabels(level, colony);
			colony.setCurrentTask("Completed " + active.type().id());
			colony.addEvent("Worker crew completed " + active.type().id());
		}
		savedState.setDirty();
		return true;
	}

	public static boolean renovateNearestColony(ServerLevel level, BlockPos pos) {
		ColonySavedState savedState = ColonySavedState.get(level.getServer());
		ColonyData colony = savedState.nearestColony(pos, 160).orElse(null);
		if (colony == null) {
			return false;
		}
		renovateColony(level, colony);
		savedState.setDirty();
		return true;
	}

	public static void renovateColony(ServerLevel level, ColonyData colony) {
		List<ColonyBuilding> oldBuildings = new ArrayList<>(colony.progress().buildingsView());
		if (oldBuildings.stream().noneMatch(building -> building.type() == BuildingType.QUEEN_CHAMBER)) {
			oldBuildings.add(0, ColonyBuilding.complete(BuildingType.QUEEN_CHAMBER, colony.origin()));
		}

		clearSettlement(level, colony.origin(), 46, 7);
		BlockPos foodNode = colony.origin().offset(32, 0, 4);
		BlockPos oreNode = colony.origin().offset(4, 0, 32);
		BlockPos chitinNode = colony.origin().offset(-32, 0, 4);
		placeResourceCluster(level, foodNode, ModBlocks.FOOD_NODE, Blocks.MOSS_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK);
		placeResourceCluster(level, oreNode, ModBlocks.ORE_NODE, Blocks.COBBLED_DEEPSLATE, Blocks.IRON_ORE);
		placeResourceCluster(level, chitinNode, ModBlocks.CHITIN_NODE, Blocks.BONE_BLOCK, Blocks.HONEYCOMB_BLOCK);
		placeWidePath(level, colony.origin(), foodNode);
		placeWidePath(level, colony.origin(), oreNode);
		placeWidePath(level, colony.origin(), chitinNode);

		colony.progress().buildings().clear();
		colony.clearChambers();
		EnumMap<BuildingType, Integer> counts = new EnumMap<>(BuildingType.class);
		for (ColonyBuilding old : oldBuildings) {
			int existing = counts.getOrDefault(old.type(), 0);
			counts.put(old.type(), existing + 1);
			BlockPos nextPos = ColonyBuilder.siteFor(colony.origin(), old.type(), existing);
			ColonyBuilding rebuilt = new ColonyBuilding(old.type(), nextPos, old.level(), old.constructionProgress(), old.disabledTicks());
			colony.progress().addBuilding(rebuilt);
			registerChamberForBuilding(colony, rebuilt);
			if (rebuilt.type() != BuildingType.QUEEN_CHAMBER) {
				placeWidePath(level, colony.origin(), nextPos);
			}
			if (rebuilt.complete()) {
				StructurePlacer.placeBuilding(level, nextPos, rebuilt.type());
			}
		}
		colony.setCurrentTask("Renovated colony campus");
		colony.addEvent("Colony renovated into Queen Hall campus");
		ColonyLabelService.syncLabels(level, colony);
	}

	private static void consumeOne(ServerPlayer player, ItemStack stack) {
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
	}

	private static boolean removeItems(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
		if (count <= 0 || player.getAbilities().instabuild) {
			return true;
		}
		if (!hasItems(player, item, count)) {
			return false;
		}
		int remaining = count;
		for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				int taken = Math.min(remaining, stack.getCount());
				stack.shrink(taken);
				remaining -= taken;
			}
		}
		return true;
	}

	private static boolean hasItems(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
		if (count <= 0 || player.getAbilities().instabuild) {
			return true;
		}
		int remaining = count;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				remaining -= stack.getCount();
				if (remaining <= 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static Optional<ColonyData> diplomacyTarget(ColonySavedState savedState, ColonyData colony) {
		return savedState.colonies().stream()
				.filter(other -> other.id() != colony.id())
				.sorted((first, second) -> {
					int firstScore = relationPriority(colony.progress().relationTo(first.id()));
					int secondScore = relationPriority(colony.progress().relationTo(second.id()));
					if (firstScore != secondScore) {
						return Integer.compare(firstScore, secondScore);
					}
					return Double.compare(first.origin().distSqr(colony.origin()), second.origin().distSqr(colony.origin()));
				})
				.findFirst();
	}

	private static int relationPriority(DiplomacyState state) {
		return switch (state) {
			case WAR -> 0;
			case RIVAL -> 1;
			case NEUTRAL -> 2;
			case ALLY -> 3;
		};
	}

	private static String diplomacyCostText(DiplomacyAction action) {
		return action.tokenCost() + " tokens, "
				+ action.dustCost() + " dust, "
				+ action.sealCost() + " seals, "
				+ action.bannerCost() + " war banners";
	}

	private static int diplomacyTokenCost(ColonyData colony, DiplomacyAction action) {
		if (colony.progress().hasResearch(ResearchNode.TREATY_SIGILS.id())
				&& (action == DiplomacyAction.ENVOY || action == DiplomacyAction.TRUCE)) {
			return Math.max(1, (int) Math.ceil(action.tokenCost() * 0.75));
		}
		return action.tokenCost();
	}

	private static TaskPriority priorityForCaste(AntCaste caste) {
		return switch (caste) {
			case MINER -> TaskPriority.ORE;
			case SOLDIER, MAJOR, GIANT -> TaskPriority.DEFENSE;
			case QUEEN -> TaskPriority.CHITIN;
			case WORKER, SCOUT -> TaskPriority.FOOD;
		};
	}

	private static void seedEconomy(ColonyData colony) {
		colony.setResource(ResourceType.FOOD, 120);
		colony.setResource(ResourceType.ORE, 20);
		colony.setResource(ResourceType.CHITIN, 24);
		colony.setResource(ResourceType.RESIN, 24);
		colony.setResource(ResourceType.FUNGUS, colony.progress().culture() == ColonyCulture.LEAFCUTTER ? 28 : 12);
		colony.setResource(ResourceType.VENOM, colony.progress().culture() == ColonyCulture.FIRE ? 16 : 4);
		colony.setResource(ResourceType.KNOWLEDGE, 8);
		colony.addCaste(AntCaste.QUEEN, 1);
		colony.addCaste(AntCaste.WORKER, 3 + colony.progress().culture().workerBias());
		colony.addCaste(AntCaste.SCOUT, 1 + colony.progress().culture().scoutBias());
		colony.addCaste(AntCaste.MINER, 2);
		colony.addCaste(AntCaste.SOLDIER, 2);
		colony.addCaste(AntCaste.MAJOR, 1);
	}

	private static void placeNest(ServerLevel level, ColonyData colony) {
		BlockPos origin = colony.origin();
		BlockPos food = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0);
		BlockPos nursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0);
		BlockPos mine = ColonyBuilder.siteFor(origin, BuildingType.MINE, 0);
		BlockPos barracks = ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0);
		BlockPos foodNode = origin.offset(32, 0, 4);
		BlockPos oreNode = origin.offset(4, 0, 32);
		BlockPos chitinNode = origin.offset(-32, 0, 4);

		clearSettlement(level, origin, 42, 7);
		placeWidePath(level, origin, foodNode);
		placeWidePath(level, origin, nursery);
		placeWidePath(level, origin, oreNode);
		placeWidePath(level, origin, barracks);
		placeWidePath(level, origin, food);
		placeWidePath(level, origin, mine);

		StructurePlacer.placeBuilding(level, origin, BuildingType.QUEEN_CHAMBER);
		StructurePlacer.placeBuilding(level, food, BuildingType.FOOD_STORE);
		StructurePlacer.placeBuilding(level, nursery, BuildingType.NURSERY);
		StructurePlacer.placeBuilding(level, mine, BuildingType.MINE);
		StructurePlacer.placeBuilding(level, barracks, BuildingType.BARRACKS);
		placeResourceCluster(level, foodNode, ModBlocks.FOOD_NODE, Blocks.MOSS_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK);
		placeResourceCluster(level, oreNode, ModBlocks.ORE_NODE, Blocks.COBBLED_DEEPSLATE, Blocks.IRON_ORE);
		placeResourceCluster(level, chitinNode, ModBlocks.CHITIN_NODE, Blocks.BONE_BLOCK, Blocks.HONEYCOMB_BLOCK);
		registerStarterBuildings(colony, origin, food, nursery, mine, barracks);
	}

	private static void registerStarterBuildings(ColonyData colony, BlockPos origin, BlockPos food, BlockPos nursery, BlockPos mine, BlockPos barracks) {
		colony.clearChambers();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_CHAMBER, origin));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.FOOD_STORE, food));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.NURSERY, nursery));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MINE, mine));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.BARRACKS, barracks));
		colony.addChamber(new NestChamber("queen_hall", origin, 1));
		colony.addChamber(new NestChamber("food_chamber", food, 1));
		colony.addChamber(new NestChamber("nursery", nursery, 1));
		colony.addChamber(new NestChamber("mine_chamber", mine, 1));
		colony.addChamber(new NestChamber("barracks", barracks, 1));
		colony.progress().buildQueue().add(BuildingType.CHITIN_FARM);
		colony.progress().buildQueue().add(BuildingType.MARKET);
		colony.progress().buildQueue().add(BuildingType.PHEROMONE_ARCHIVE);
	}

	private static void registerChamberForBuilding(ColonyData colony, ColonyBuilding building) {
		if (building.type() == BuildingType.ROAD || building.type() == BuildingType.WATCH_POST) {
			return;
		}
		colony.addChamber(new NestChamber(building.type().id(), building.pos(), building.level()));
	}

	private static void addChamber(ServerLevel level, ColonyData colony, String type, BlockPos pos, Block block) {
		set(level, pos, block);
		colony.addChamber(new NestChamber(type, pos, 1));
	}

	private static void set(ServerLevel level, BlockPos pos, Block block) {
		StructurePlacer.safeSet(level, pos, block);
	}

	private static void clearSettlement(ServerLevel level, BlockPos origin, int radius, int height) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (x * x + z * z > radius * radius) {
					continue;
				}
				for (int y = 1; y <= height; y++) {
					set(level, origin.offset(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	private static void placeCoreMound(ServerLevel level, ColonyData colony, BlockPos origin) {
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance <= 5) {
					set(level, origin.offset(x, 0, z), distance <= 2 ? ModBlocks.NEST_MOUND : Blocks.ROOTED_DIRT);
				}
				if (distance <= 3 && !(x == 0 && z == -1)) {
					set(level, origin.offset(x, 1, z), ModBlocks.NEST_MOUND);
				}
				if (distance <= 1) {
					set(level, origin.offset(x, 2, z), ModBlocks.NEST_MOUND);
				}
			}
		}
		set(level, origin, ModBlocks.NEST_MOUND);
		addChamber(level, colony, "nest_core", origin.below(), ModBlocks.NEST_CORE);
		set(level, origin.offset(0, 1, -1), Blocks.AIR);
		set(level, origin.offset(0, 0, -1), Blocks.DIRT_PATH);
		set(level, origin.offset(0, 2, 0), Blocks.OCHRE_FROGLIGHT);
	}

	private static void placeFoodChamber(ServerLevel level, ColonyData colony, BlockPos center) {
		placePod(level, center, ModBlocks.FOOD_CHAMBER, Blocks.PACKED_MUD, Blocks.BROWN_MUSHROOM_BLOCK);
		set(level, center.offset(-1, 1, 0), ModBlocks.FOOD_NODE);
		set(level, center.offset(1, 1, 0), Blocks.RED_MUSHROOM_BLOCK);
		set(level, center.offset(0, 1, 1), Blocks.MOSS_BLOCK);
		colony.addChamber(new NestChamber("food_chamber", center, 1));
	}

	private static void placeNursery(ServerLevel level, ColonyData colony, BlockPos center) {
		placePod(level, center, ModBlocks.NURSERY_CHAMBER, Blocks.HONEYCOMB_BLOCK, Blocks.BONE_BLOCK);
		set(level, center.offset(-1, 1, 0), ModBlocks.CHITIN_NODE);
		set(level, center.offset(1, 1, 0), Blocks.OCHRE_FROGLIGHT);
		set(level, center.offset(0, 1, 1), Blocks.BONE_BLOCK);
		colony.addChamber(new NestChamber("nursery", center, 1));
	}

	private static void placeMineChamber(ServerLevel level, ColonyData colony, BlockPos center) {
		placePod(level, center, ModBlocks.MINE_CHAMBER, Blocks.COBBLED_DEEPSLATE, Blocks.IRON_ORE);
		set(level, center.offset(-1, 1, 0), ModBlocks.ORE_NODE);
		set(level, center.offset(1, 1, 0), Blocks.DEEPSLATE_IRON_ORE);
		set(level, center.offset(0, 1, 1), Blocks.COBBLED_DEEPSLATE_WALL);
		colony.addChamber(new NestChamber("mine_chamber", center, 1));
	}

	private static void placeBarracks(ServerLevel level, ColonyData colony, BlockPos center) {
		placePod(level, center, ModBlocks.BARRACKS_CHAMBER, Blocks.MUD_BRICKS, Blocks.POLISHED_DEEPSLATE);
		set(level, center.offset(-1, 1, 0), Blocks.BONE_BLOCK);
		set(level, center.offset(1, 1, 0), Blocks.BONE_BLOCK);
		set(level, center.offset(0, 1, -1), Blocks.COBBLED_DEEPSLATE_WALL);
		colony.addChamber(new NestChamber("barracks", center, 1));
	}

	private static void placePod(ServerLevel level, BlockPos center, Block core, Block shell, Block accent) {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				BlockPos pos = center.offset(x, 0, z);
				if (x == 0 && z == 0) {
					set(level, pos, core);
				} else if (distance <= 3) {
					set(level, pos, shell);
				} else {
					set(level, pos, Blocks.ROOTED_DIRT);
				}
				if (distance == 4) {
					set(level, center.offset(x, 1, z), accent);
				}
			}
		}
		set(level, center.offset(0, 1, 0), Blocks.AIR);
		set(level, center.offset(0, 2, 0), Blocks.AIR);
	}

	private static void placeResourceCluster(ServerLevel level, BlockPos center, Block node, Block base, Block accent) {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance <= 3) {
					set(level, center.offset(x, 0, z), distance <= 1 ? node : base);
				}
			}
		}
		set(level, center, node);
		set(level, center.offset(1, 1, 0), accent);
		set(level, center.offset(-1, 1, 0), node);
		set(level, center.offset(0, 1, 1), accent);
	}

	private static void placePath(ServerLevel level, BlockPos start, BlockPos end) {
		int dx = Integer.compare(end.getX(), start.getX());
		int dz = Integer.compare(end.getZ(), start.getZ());
		BlockPos current = start;
		while (!current.equals(end)) {
			current = current.offset(dx, 0, dz);
			set(level, current, Blocks.DIRT_PATH);
			if ((Math.abs(current.getX() - start.getX()) + Math.abs(current.getZ() - start.getZ())) % 4 == 0) {
				set(level, current.offset(dz, 0, dx), Blocks.COARSE_DIRT);
				set(level, current.offset(-dz, 0, -dx), Blocks.PODZOL);
			}
		}
	}

	private static void placeWidePath(ServerLevel level, BlockPos start, BlockPos end) {
		BlockPos current = start;
		int step = 0;
		while (current.getX() != end.getX()) {
			int dx = Integer.compare(end.getX(), current.getX());
			current = current.offset(dx, 0, 0);
			paintWidePathTile(level, current, true, ++step);
		}
		while (current.getZ() != end.getZ()) {
			int dz = Integer.compare(end.getZ(), current.getZ());
			current = current.offset(0, 0, dz);
			paintWidePathTile(level, current, false, ++step);
		}
	}

	private static void paintWidePathTile(ServerLevel level, BlockPos current, boolean xAxis, int step) {
		set(level, current, Blocks.DIRT_PATH);
		if (xAxis) {
			set(level, current.north(), Blocks.DIRT_PATH);
			set(level, current.south(), Blocks.DIRT_PATH);
		} else {
			set(level, current.east(), Blocks.DIRT_PATH);
			set(level, current.west(), Blocks.DIRT_PATH);
		}
		if (step % 5 == 0) {
			if (xAxis) {
				set(level, current.north(2), Blocks.COARSE_DIRT);
				set(level, current.south(2), Blocks.PODZOL);
			} else {
				set(level, current.east(2), Blocks.COARSE_DIRT);
				set(level, current.west(2), Blocks.PODZOL);
			}
		}
	}

	private static void spawnStarterCastes(ServerLevel level, ColonyData colony) {
		BlockPos origin = colony.origin();
		spawnAnt(level, origin, AntCaste.QUEEN, colony.id());
		AntEntity foreman = spawnAnt(level, origin.offset(2, 0, -2), AntCaste.WORKER, colony.id());
		if (foreman != null) {
			applyAntName(foreman, AntCaste.WORKER, colony.id(), true);
		}
		spawnAnt(level, origin.offset(-2, 0, -2), AntCaste.WORKER, colony.id());
		spawnAnt(level, origin.offset(-1, 0, 0), AntCaste.WORKER, colony.id());
		spawnAnt(level, origin.offset(0, 0, 3), AntCaste.MINER, colony.id());
		spawnAnt(level, origin.offset(0, 0, -4), AntCaste.SOLDIER, colony.id());
		spawnAnt(level, origin.offset(4, 0, -3), AntCaste.MAJOR, colony.id());
	}

	private static void applyAntName(AntEntity ant, AntCaste caste, int colonyId, boolean foreman) {
		if (foreman) {
			ant.setCustomName(net.minecraft.network.chat.Component.translatable("formic_frontier.ant.worker_foreman", colonyId));
			ant.setCustomNameVisible(true);
			return;
		}
		if (caste == AntCaste.QUEEN) {
			ant.setCustomName(net.minecraft.network.chat.Component.translatable("formic_frontier.ant.queen", colonyId));
			ant.setCustomNameVisible(true);
		} else if (caste == AntCaste.MAJOR || caste == AntCaste.GIANT) {
			ant.setCustomName(net.minecraft.network.chat.Component.translatable("formic_frontier.ant.guard", caste.id(), colonyId));
			ant.setCustomNameVisible(true);
		}
	}

	private static String buildingsText(ColonyData colony) {
		StringBuilder builder = new StringBuilder();
		for (ColonyBuilding building : colony.progress().buildingsView()) {
			builder.append(building.type().id())
					.append(" @ ").append(building.pos().toShortString())
					.append(" lvl ").append(building.level())
					.append(" progress ").append(building.constructionProgress()).append("%");
			if (building.disabledTicks() > 0) {
				builder.append(" disabled ").append(building.disabledTicks()).append("t");
			}
			builder.append("\n");
		}
		if (!colony.progress().buildQueueView().isEmpty()) {
			builder.append("Queue: ").append(colony.progress().buildQueueView()).append("\n");
		}
		return builder.toString();
	}

	private static String instinctText(ColonyData colony) {
		return "Colony instinct biases autonomous growth."
				+ "\nTop instinct: " + colony.prioritiesView().getFirst().id()
				+ "\nInstinct queue: " + colony.prioritiesView()
				+ "\nPheromone Dust copies a caste focus into the colony instinct."
				+ "\nFood=workers/scouts, Ore=miners, Chitin=nursery/major, Defense=soldiers/watch posts.";
	}

	private static String diplomacyText(ColonyData colony) {
		StringBuilder builder = new StringBuilder();
		builder.append("Rank: ").append(ColonyRank.current(colony).displayName())
				.append(" | score ").append(ColonyRank.score(colony)).append("\n");
		builder.append("Select a relation target, then choose an action.\n");
		for (DiplomacyAction action : DiplomacyAction.values()) {
			builder.append(action.id())
					.append(": ").append(action.label())
					.append(" | cost ").append(diplomacyTokenCost(colony, action)).append(" tokens, ")
					.append(action.dustCost()).append(" dust, ")
					.append(action.sealCost()).append(" seals, ")
					.append(action.bannerCost()).append(" war banners")
					.append(" | rank ").append(action.minRank().displayName())
					.append("\n");
		}
		return builder.toString();
	}

	private static String requestsText(ColonyData colony) {
		StringBuilder builder = new StringBuilder();
		if (colony.progress().requestsView().isEmpty()) {
			return "No open logistics requests.";
		}
		for (ColonyRequest request : colony.progress().requestsView()) {
			builder.append(request.building().id())
					.append(" needs ").append(request.resource().id())
					.append(" ").append(request.fulfilled()).append("/").append(request.needed())
					.append(" | ").append(request.reason())
					.append("\n");
		}
		return builder.toString();
	}

	private static String researchText(ColonyData colony) {
		StringBuilder builder = new StringBuilder();
		Optional<ResearchState> active = colony.progress().activeResearch();
		builder.append("Completed: ").append(colony.progress().completedResearchView()).append("\n");
		if (active.isPresent()) {
			ResearchNode node = ResearchNode.fromId(active.get().nodeId());
			builder.append("Active: ").append(node.label())
					.append(" ").append(active.get().progressTicks())
					.append("/").append(node.durationTicks())
					.append("\n");
		} else {
			builder.append("Active: none\n");
		}
		for (ResearchNode node : ResearchNode.values()) {
			builder.append(node.id())
					.append(": ").append(node.label())
					.append(" | building ").append(node.requiredBuilding().id())
					.append(" | costs ").append(node.costsView());
			if (colony.progress().hasResearch(node.id())) {
				builder.append(" | complete");
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	private static String relationsText(ColonyData colony) {
		StringBuilder builder = new StringBuilder();
		if (colony.progress().knownColoniesView().isEmpty()) {
			return "No known colonies.";
		}
		for (var entry : colony.progress().knownColoniesView().entrySet()) {
			builder.append("#").append(entry.getKey()).append(" ").append(entry.getValue().id()).append("\n");
		}
		return builder.toString();
	}

	private static String eventsText(ColonyData colony) {
		StringBuilder builder = new StringBuilder();
		if (colony.progress().eventsView().isEmpty()) {
			return "No events yet.";
		}
		for (var event : colony.progress().eventsView()) {
			builder.append("t+").append(event.ageTicks()).append(": ").append(event.message()).append("\n");
		}
		return builder.toString();
	}
}
