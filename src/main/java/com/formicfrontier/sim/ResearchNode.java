package com.formicfrontier.sim;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum ResearchNode {
	CHITIN_CULTIVATION("chitin_cultivation", "Chitin Cultivation", BuildingType.NURSERY, 120, List.of(), Map.of(ResourceType.KNOWLEDGE, 12, ResourceType.CHITIN, 16)),
	RESIN_MASONRY("resin_masonry", "Resin Masonry", BuildingType.PHEROMONE_ARCHIVE, 140, List.of(), Map.of(ResourceType.KNOWLEDGE, 16, ResourceType.RESIN, 12, ResourceType.ORE, 8)),
	FUNGUS_SYMBIOSIS("fungus_symbiosis", "Fungus Symbiosis", BuildingType.PHEROMONE_ARCHIVE, 160, List.of(), Map.of(ResourceType.KNOWLEDGE, 18, ResourceType.FUNGUS, 10, ResourceType.FOOD, 20)),
	VENOM_DRILLS("venom_drills", "Venom Drills", BuildingType.ARMORY, 180, List.of("fungus_symbiosis"), Map.of(ResourceType.KNOWLEDGE, 24, ResourceType.VENOM, 8, ResourceType.ORE, 16)),
	MANDIBLE_PLATING("mandible_plating", "Mandible Plating", BuildingType.ARMORY, 200, List.of("resin_masonry"), Map.of(ResourceType.KNOWLEDGE, 30, ResourceType.RESIN, 20, ResourceType.CHITIN, 24)),
	SCENTED_LEDGER("scented_ledger", "Scented Ledger", BuildingType.MARKET, 160, List.of(), Map.of(ResourceType.KNOWLEDGE, 18, ResourceType.RESIN, 8)),
	TREATY_SIGILS("treaty_sigils", "Treaty Sigils", BuildingType.DIPLOMACY_SHRINE, 180, List.of("scented_ledger"), Map.of(ResourceType.KNOWLEDGE, 26, ResourceType.FUNGUS, 8, ResourceType.RESIN, 8));

	private final String id;
	private final String label;
	private final BuildingType requiredBuilding;
	private final int durationTicks;
	private final List<String> prerequisites;
	private final EnumMap<ResourceType, Integer> costs = new EnumMap<>(ResourceType.class);

	ResearchNode(String id, String label, BuildingType requiredBuilding, int durationTicks, List<String> prerequisites, Map<ResourceType, Integer> costs) {
		this.id = id;
		this.label = label;
		this.requiredBuilding = requiredBuilding;
		this.durationTicks = durationTicks;
		this.prerequisites = List.copyOf(prerequisites);
		this.costs.putAll(costs);
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public BuildingType requiredBuilding() {
		return requiredBuilding;
	}

	public int durationTicks() {
		return durationTicks;
	}

	public List<String> prerequisites() {
		return prerequisites;
	}

	public int cost(ResourceType type) {
		return costs.getOrDefault(type, 0);
	}

	public Map<ResourceType, Integer> costsView() {
		return Map.copyOf(costs);
	}

	public boolean canStart(ColonyData colony) {
		if (colony.progress().hasResearch(id) || colony.progress().activeResearch().isPresent()) {
			return false;
		}
		if (!colony.progress().hasCompleted(BuildingType.PHEROMONE_ARCHIVE)) {
			return false;
		}
		if (!colony.progress().hasCompleted(requiredBuilding)) {
			return false;
		}
		for (String prerequisite : prerequisites) {
			if (!colony.progress().hasResearch(prerequisite)) {
				return false;
			}
		}
		for (Map.Entry<ResourceType, Integer> entry : costs.entrySet()) {
			if (colony.resource(entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	public void consumeCosts(ColonyData colony) {
		for (Map.Entry<ResourceType, Integer> entry : costs.entrySet()) {
			colony.addResource(entry.getKey(), -entry.getValue());
		}
	}

	public static ResearchNode fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (ResearchNode node : values()) {
			if (node.id.equals(normalized)) {
				return node;
			}
		}
		throw new IllegalArgumentException("Unknown research node: " + id);
	}
}
