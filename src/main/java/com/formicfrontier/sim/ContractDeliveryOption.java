package com.formicfrontier.sim;

public record ContractDeliveryOption(ResourceType resource, String itemKey, int itemCount, int resourceAmount) {
	public static ContractDeliveryOption forResource(ResourceType resource) {
		return switch (resource) {
			case FOOD -> new ContractDeliveryOption(resource, "item.minecraft.wheat", 8, 12);
			case ORE -> new ContractDeliveryOption(resource, "item.minecraft.raw_iron", 4, 8);
			case CHITIN -> new ContractDeliveryOption(resource, "item.formic_frontier.chitin_shard", 2, 6);
			case RESIN -> new ContractDeliveryOption(resource, "item.formic_frontier.resin_glob", 2, 6);
			case FUNGUS -> new ContractDeliveryOption(resource, "item.formic_frontier.fungus_culture", 2, 6);
			case VENOM -> new ContractDeliveryOption(resource, "item.formic_frontier.venom_sac", 1, 5);
			case KNOWLEDGE -> new ContractDeliveryOption(resource, "item.formic_frontier.pheromone_dust", 4, 6);
		};
	}

	public int deliveredAmount(int missingResources) {
		return Math.max(0, Math.min(missingResources, resourceAmount));
	}

	public int itemCountFor(int deliveredResources) {
		if (deliveredResources <= 0 || resourceAmount <= 0) {
			return itemCount;
		}
		return Math.max(1, (int) Math.ceil(deliveredResources * (double) itemCount / resourceAmount));
	}
}
