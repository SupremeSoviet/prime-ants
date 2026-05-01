package com.formicfrontier.sim;

import com.formicfrontier.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ColonyTradeCatalog {
	private static final double TRADE_HUB_TOKEN_COST_MULTIPLIER = 0.85;
	private static final int TRADE_HUB_TOKEN_REWARD_BONUS = 1;
	private static List<Offer> offers;

	private ColonyTradeCatalog() {
	}

	public static List<Offer> offersView() {
		if (offers == null) {
			offers = createOffers();
		}
		return offers;
	}

	private static List<Offer> createOffers() {
		return List.of(
			new Offer("sell_wheat", Items.WHEAT, 16, ModItems.PHEROMONE_TOKEN, 1, ResourceType.FOOD, 12, 1, 0),
			new Offer("sell_biomass", ModItems.RAW_BIOMASS, 4, ModItems.PHEROMONE_TOKEN, 1, ResourceType.FOOD, 16, 2, 0),
			new Offer("sell_raw_iron", Items.RAW_IRON, 8, ModItems.PHEROMONE_TOKEN, 2, ResourceType.ORE, 12, 2, 0),
			new Offer("sell_iron_ore", Items.IRON_ORE, 8, ModItems.PHEROMONE_TOKEN, 2, ResourceType.ORE, 12, 2, 0),
			new Offer("sell_chitin", ModItems.CHITIN_SHARD, 4, ModItems.PHEROMONE_TOKEN, 2, ResourceType.CHITIN, 8, 2, 0),
			new Offer("sell_resin", ModItems.RESIN_GLOB, 3, ModItems.PHEROMONE_TOKEN, 2, ResourceType.RESIN, 8, 2, 0),
			new Offer("sell_fungus", ModItems.FUNGUS_CULTURE, 3, ModItems.PHEROMONE_TOKEN, 2, ResourceType.FUNGUS, 9, 2, 0),
			new Offer("sell_venom", ModItems.VENOM_SAC, 2, ModItems.PHEROMONE_TOKEN, 3, ResourceType.VENOM, 7, 3, 0, "", ColonyCulture.FIRE),
			new Offer("sell_royal_jelly", ModItems.ROYAL_JELLY, 1, ModItems.PHEROMONE_TOKEN, 8, ResourceType.FOOD, 40, 5, 0),
			new Offer("buy_chitin_spore", ModItems.PHEROMONE_TOKEN, 2, ModItems.CHITIN_SPORE, 1, null, 0, 0, 0),
			new Offer("buy_pheromone_dust", ModItems.PHEROMONE_TOKEN, 4, ModItems.PHEROMONE_DUST, 1, null, 0, 0, 0),
			new Offer("buy_resin_glob", ModItems.PHEROMONE_TOKEN, 3, ModItems.RESIN_GLOB, 1, null, 0, 0, 0),
			new Offer("buy_fungus_culture", ModItems.PHEROMONE_TOKEN, 4, ModItems.FUNGUS_CULTURE, 1, null, 0, 0, 0, ResearchNode.FUNGUS_SYMBIOSIS.id(), null),
			new Offer("buy_venom_sac", ModItems.PHEROMONE_TOKEN, 5, ModItems.VENOM_SAC, 1, null, 0, 0, 0, ResearchNode.VENOM_DRILLS.id(), null),
			new Offer("buy_colony_seal", ModItems.PHEROMONE_TOKEN, 16, ModItems.COLONY_SEAL, 1, null, 0, 0, 20),
			new Offer("buy_war_banner", ModItems.PHEROMONE_TOKEN, 14, ModItems.WAR_BANNER, 1, null, 0, 0, 0),
			new Offer("buy_chitin_boots", ModItems.PHEROMONE_TOKEN, 10, ModItems.CHITIN_BOOTS, 1, null, 0, 0, 0),
			new Offer("buy_chitin_helmet", ModItems.PHEROMONE_TOKEN, 12, ModItems.CHITIN_HELMET, 1, null, 0, 0, 0),
			new Offer("buy_chitin_leggings", ModItems.PHEROMONE_TOKEN, 18, ModItems.CHITIN_LEGGINGS, 1, null, 0, 0, 0),
			new Offer("buy_chitin_chestplate", ModItems.PHEROMONE_TOKEN, 20, ModItems.CHITIN_CHESTPLATE, 1, null, 0, 0, 0),
			new Offer("buy_resin_chitin_boots", ModItems.PHEROMONE_TOKEN, 18, ModItems.RESIN_CHITIN_BOOTS, 1, null, 0, 0, 15, ResearchNode.MANDIBLE_PLATING.id(), null),
			new Offer("buy_resin_chitin_helmet", ModItems.PHEROMONE_TOKEN, 20, ModItems.RESIN_CHITIN_HELMET, 1, null, 0, 0, 15, ResearchNode.MANDIBLE_PLATING.id(), null),
			new Offer("buy_resin_chitin_leggings", ModItems.PHEROMONE_TOKEN, 30, ModItems.RESIN_CHITIN_LEGGINGS, 1, null, 0, 0, 20, ResearchNode.MANDIBLE_PLATING.id(), null),
			new Offer("buy_resin_chitin_chestplate", ModItems.PHEROMONE_TOKEN, 34, ModItems.RESIN_CHITIN_CHESTPLATE, 1, null, 0, 0, 20, ResearchNode.MANDIBLE_PLATING.id(), null),
			new Offer("buy_mandible_saber", ModItems.PHEROMONE_TOKEN, 24, ModItems.MANDIBLE_SABER, 1, null, 0, 0, 20, ResearchNode.MANDIBLE_PLATING.id(), null),
			new Offer("buy_venom_spear", ModItems.PHEROMONE_TOKEN, 22, ModItems.VENOM_SPEAR, 1, null, 0, 0, 15, ResearchNode.VENOM_DRILLS.id(), null),
			new Offer("buy_queen_egg", ModItems.PHEROMONE_TOKEN, 64, ModItems.QUEEN_EGG, 1, null, 0, 0, 40)
		);
	}

	public static TradeResult execute(ServerPlayer player, ColonyData colony, String offerId) {
		Offer offer = offersView().stream().filter(candidate -> candidate.id().equals(offerId)).findFirst().orElse(null);
		if (offer == null) {
			return new TradeResult(false, "Unknown trade: " + offerId);
		}
		if (colony.progress().reputation() < offer.minReputation()) {
			return new TradeResult(false, "This colony does not trust you enough yet.");
		}
		if (!isAvailable(colony, offer)) {
			return new TradeResult(false, availabilityText(colony, offer));
		}
		int inputCount = inputCount(colony, offer);
		int outputCount = outputCount(colony, offer);
		if (!removeItems(player, offer.input(), inputCount)) {
			return new TradeResult(false, "Need " + inputCount + " " + itemName(offer.input()));
		}
		give(player, new ItemStack(offer.output(), outputCount));
		if (offer.resourceType() != null) {
			colony.addResource(offer.resourceType(), offer.resourceDelta());
		}
		colony.progress().addReputation(offer.reputationDelta());
		colony.setCurrentTask("Traded with " + player.getScoreboardName() + ": " + offer.id());
		colony.addEvent("Trade: " + player.getScoreboardName() + " used " + offer.id());
		return new TradeResult(true, "Trade complete: " + offer.id());
	}

	public static int inputCount(ColonyData colony, Offer offer) {
		if (offer.input() != ModItems.PHEROMONE_TOKEN) {
			return offer.inputCount();
		}
		double multiplier = 1.0;
		if (colony.progress().hasResearch(ResearchNode.SCENTED_LEDGER.id())) {
			multiplier *= 0.85;
		}
		if (colony.progress().reputation() >= 60) {
			multiplier = 0.75;
		} else if (colony.progress().reputation() >= 25) {
			multiplier = 0.9;
		} else if (colony.progress().reputation() < 0) {
			multiplier = 1.25;
		}
		if (hasTradeHub(colony)) {
			multiplier *= TRADE_HUB_TOKEN_COST_MULTIPLIER;
		}
		return Math.max(1, (int) Math.ceil(offer.inputCount() * multiplier));
	}

	public static int outputCount(ColonyData colony, Offer offer) {
		if (offer.output() != ModItems.PHEROMONE_TOKEN) {
			return offer.outputCount();
		}
		int bonus = colony.progress().reputation() >= 50 && offer.outputCount() < 8 ? 1 : 0;
		if (hasTradeHub(colony) && offer.outputCount() < 8) {
			bonus += TRADE_HUB_TOKEN_REWARD_BONUS;
		}
		return offer.outputCount() + bonus;
	}

	private static boolean hasTradeHub(ColonyData colony) {
		return colony.progress().hasCompleted(BuildingType.TRADE_HUB);
	}

	public static boolean isVisible(ColonyData colony, Offer offer) {
		return offer.requiredCulture() == null
				|| offer.requiredCulture() == colony.progress().culture()
				|| colony.progress().hasResearch(ResearchNode.SCENTED_LEDGER.id());
	}

	public static boolean isAvailable(ColonyData colony, Offer offer) {
		if (!isVisible(colony, offer)) {
			return false;
		}
		return offer.requiredResearch().isEmpty() || colony.progress().hasResearch(offer.requiredResearch());
	}

	public static String availabilityText(ColonyData colony, Offer offer) {
		if (colony.progress().reputation() < offer.minReputation()) {
			return "Requires reputation " + offer.minReputation();
		}
		if (!offer.requiredResearch().isEmpty() && !colony.progress().hasResearch(offer.requiredResearch())) {
			return "Requires research " + offer.requiredResearch();
		}
		if (!isVisible(colony, offer)) {
			return "Requires " + offer.requiredCulture().id() + " culture";
		}
		String tradeHubText = tradeHubText(colony, offer);
		if (!tradeHubText.isEmpty()) {
			return tradeHubText;
		}
		return "Available";
	}

	private static String tradeHubText(ColonyData colony, Offer offer) {
		if (!hasTradeHub(colony)) {
			return "";
		}
		if (offer.output() == ModItems.PHEROMONE_TOKEN && offer.outputCount() < 8) {
			return "Trade Hub: +1 token";
		}
		if (offer.input() == ModItems.PHEROMONE_TOKEN) {
			return "Trade Hub: lower token cost";
		}
		return "";
	}

	private static boolean removeItems(ServerPlayer player, Item item, int count) {
		int remaining = count;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				remaining -= stack.getCount();
				if (remaining <= 0) {
					break;
				}
			}
		}
		if (remaining > 0) {
			return false;
		}
		remaining = count;
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

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
	}

	private static String itemName(Item item) {
		return item.getName().getString();
	}

	public record Offer(String id, Item input, int inputCount, Item output, int outputCount, ResourceType resourceType, int resourceDelta, int reputationDelta, int minReputation, String requiredResearch, ColonyCulture requiredCulture) {
		public Offer(String id, Item input, int inputCount, Item output, int outputCount, ResourceType resourceType, int resourceDelta, int reputationDelta, int minReputation) {
			this(id, input, inputCount, output, outputCount, resourceType, resourceDelta, reputationDelta, minReputation, "", null);
		}
	}

	public record TradeResult(boolean success, String message) {
	}
}
