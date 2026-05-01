package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum AntCaste {
	WORKER("worker", 1.5f, 18.0, 0.28, 2.0, 1, 6, 0, 0),
	SCOUT("scout", 1.55f, 14.0, 0.34, 1.0, 1, 4, 1, 0),
	MINER("miner", 1.7f, 22.0, 0.24, 3.0, 2, 8, 4, 0),
	SOLDIER("soldier", 1.8f, 30.0, 0.26, 5.0, 3, 10, 2, 3),
	MAJOR("major", 2.25f, 48.0, 0.22, 8.0, 6, 24, 8, 10),
	GIANT("giant", 3.0f, 90.0, 0.18, 14.0, 24, 180, 45, 55),
	QUEEN("queen", 2.4f, 140.0, 0.08, 2.0, 12, 0, 0, 0);

	public static final Codec<AntCaste> CODEC = Codec.STRING.xmap(AntCaste::fromId, AntCaste::id);

	private final String id;
	private final float height;
	private final double health;
	private final double speed;
	private final double damage;
	private final int foodUpkeep;
	private final int foodCost;
	private final int oreCost;
	private final int chitinCost;

	AntCaste(String id, float height, double health, double speed, double damage, int foodUpkeep, int foodCost, int oreCost, int chitinCost) {
		this.id = id;
		this.height = height;
		this.health = health;
		this.speed = speed;
		this.damage = damage;
		this.foodUpkeep = foodUpkeep;
		this.foodCost = foodCost;
		this.oreCost = oreCost;
		this.chitinCost = chitinCost;
	}

	public String id() {
		return id;
	}

	public float height() {
		return height;
	}

	public float width() {
		return Math.max(0.85f, Math.min(1.65f, height * 0.62f));
	}

	public float visualScale() {
		return switch (this) {
			case WORKER -> 1.08f;
			case SCOUT -> 1.14f;
			case MINER -> 1.18f;
			default -> height / 1.5f;
		};
	}

	public double health() {
		return health;
	}

	public double speed() {
		return speed;
	}

	public double damage() {
		return damage;
	}

	public int foodUpkeep() {
		return foodUpkeep;
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

	public boolean canGrowFrom(ColonyData colony) {
		return colony.resource(ResourceType.FOOD) >= foodCost
				&& colony.resource(ResourceType.ORE) >= oreCost
				&& colony.resource(ResourceType.CHITIN) >= chitinCost;
	}

	public void consumeGrowthCost(ColonyData colony) {
		colony.addResource(ResourceType.FOOD, -foodCost);
		colony.addResource(ResourceType.ORE, -oreCost);
		colony.addResource(ResourceType.CHITIN, -chitinCost);
	}

	public static AntCaste fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (AntCaste caste : values()) {
			if (caste.id.equals(normalized)) {
				return caste;
			}
		}
		throw new IllegalArgumentException("Unknown ant caste: " + id);
	}
}
