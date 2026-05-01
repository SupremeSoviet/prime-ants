package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.List;
import java.util.Locale;

public enum ColonyCulture {
	AMBER("amber", "Amber Burrow", 0xD69A22, 0, 0, 0, 0, 0, 0, 0),
	LEAFCUTTER("leafcutter", "Leafcutter Choir", 0x5E8F3A, 2, 0, 2, 0, 1, 1, 0),
	FIRE("fire", "Fire Mandible", 0xB83224, 0, 0, 0, 2, 0, 0, 1),
	CARPENTER("carpenter", "Carpenter Resin", 0x9A6233, 0, 1, 0, 0, 0, 0, 2);

	public static final Codec<ColonyCulture> CODEC = Codec.STRING.xmap(ColonyCulture::fromId, ColonyCulture::id);

	private final String id;
	private final String displayName;
	private final int color;
	private final int foodBonus;
	private final int resinBonus;
	private final int fungusBonus;
	private final int venomBonus;
	private final int workerBias;
	private final int scoutBias;
	private final int constructionBonus;

	ColonyCulture(String id, String displayName, int color, int foodBonus, int resinBonus, int fungusBonus, int venomBonus, int workerBias, int scoutBias, int constructionBonus) {
		this.id = id;
		this.displayName = displayName;
		this.color = color;
		this.foodBonus = foodBonus;
		this.resinBonus = resinBonus;
		this.fungusBonus = fungusBonus;
		this.venomBonus = venomBonus;
		this.workerBias = workerBias;
		this.scoutBias = scoutBias;
		this.constructionBonus = constructionBonus;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public int color() {
		return color;
	}

	public int foodBonus() {
		return foodBonus;
	}

	public int resinBonus() {
		return resinBonus;
	}

	public int fungusBonus() {
		return fungusBonus;
	}

	public int venomBonus() {
		return venomBonus;
	}

	public int workerBias() {
		return workerBias;
	}

	public int scoutBias() {
		return scoutBias;
	}

	public int constructionBonus() {
		return constructionBonus;
	}

	public List<BuildingType> starterQueue() {
		return switch (this) {
			case AMBER -> List.of(BuildingType.DIPLOMACY_SHRINE, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE);
			case LEAFCUTTER -> List.of(BuildingType.FUNGUS_GARDEN, BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE);
			case FIRE -> List.of(BuildingType.WATCH_POST, BuildingType.ARMORY, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE);
			case CARPENTER -> List.of(BuildingType.RESIN_DEPOT, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE);
		};
	}

	public static ColonyCulture rivalFor(int id) {
		ColonyCulture[] rivals = {LEAFCUTTER, FIRE, CARPENTER};
		return rivals[Math.floorMod(id - 1, rivals.length)];
	}

	public static ColonyCulture fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (ColonyCulture culture : values()) {
			if (culture.id.equals(normalized)) {
				return culture;
			}
		}
		throw new IllegalArgumentException("Unknown colony culture: " + id);
	}
}
