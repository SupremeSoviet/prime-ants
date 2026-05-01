package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum BuildingType {
	QUEEN_CHAMBER("queen_chamber", 0, 0, 0),
	FOOD_STORE("food_store", 24, 0, 0),
	NURSERY("nursery", 32, 0, 8),
	MINE("mine", 20, 16, 0),
	CHITIN_FARM("chitin_farm", 24, 4, 12),
	BARRACKS("barracks", 36, 18, 16),
	MARKET("market", 28, 10, 6),
	DIPLOMACY_SHRINE("diplomacy_shrine", 40, 14, 24),
	WATCH_POST("watch_post", 18, 12, 8),
	RESIN_DEPOT("resin_depot", 32, 8, 8, 0, 0, 0, 0),
	PHEROMONE_ARCHIVE("pheromone_archive", 48, 16, 20, 8, 4, 0, 0),
	FUNGUS_GARDEN("fungus_garden", 28, 0, 8, 4, 0, 0, 0),
	VENOM_PRESS("venom_press", 36, 18, 18, 12, 6, 0, 0),
	ARMORY("armory", 42, 28, 24, 16, 0, 4, 0),
	GREAT_MOUND("great_mound", 120, 48, 96, 48, 24, 0, 36),
	QUEEN_VAULT("queen_vault", 80, 36, 120, 42, 18, 0, 48),
	TRADE_HUB("trade_hub", 96, 30, 72, 54, 12, 0, 54),
	ROAD("road", 4, 0, 0);

	public static final Codec<BuildingType> CODEC = Codec.STRING.xmap(BuildingType::fromId, BuildingType::id);

	private final String id;
	private final int foodCost;
	private final int oreCost;
	private final int chitinCost;
	private final int resinCost;
	private final int fungusCost;
	private final int venomCost;
	private final int knowledgeCost;

	BuildingType(String id, int foodCost, int oreCost, int chitinCost) {
		this(id, foodCost, oreCost, chitinCost, 0, 0, 0, 0);
	}

	BuildingType(String id, int foodCost, int oreCost, int chitinCost, int resinCost, int fungusCost, int venomCost, int knowledgeCost) {
		this.id = id;
		this.foodCost = foodCost;
		this.oreCost = oreCost;
		this.chitinCost = chitinCost;
		this.resinCost = resinCost;
		this.fungusCost = fungusCost;
		this.venomCost = venomCost;
		this.knowledgeCost = knowledgeCost;
	}

	public String id() {
		return id;
	}

	public int foodCost() {
		return foodCost;
	}

	public int oreCost() {
		return oreCost;
	}

	public int chitinCost() {
		return chitinCost;
	}

	public int cost(ResourceType type) {
		return switch (type) {
			case FOOD -> foodCost;
			case ORE -> oreCost;
			case CHITIN -> chitinCost;
			case RESIN -> resinCost;
			case FUNGUS -> fungusCost;
			case VENOM -> venomCost;
			case KNOWLEDGE -> knowledgeCost;
		};
	}

	public boolean canStart(ColonyData colony) {
		for (ResourceType type : ResourceType.values()) {
			if (colony.resource(type) < cost(type)) {
				return false;
			}
		}
		return true;
	}

	public void consumeStartCost(ColonyData colony) {
		for (ResourceType type : ResourceType.values()) {
			colony.addResource(type, -cost(type));
		}
	}

	public static BuildingType fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (BuildingType type : values()) {
			if (type.id.equals(normalized)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown building type: " + id);
	}
}
