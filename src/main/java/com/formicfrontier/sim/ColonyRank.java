package com.formicfrontier.sim;

public enum ColonyRank {
	OUTPOST("outpost", "Outpost", 0, 18, 0),
	BURROW("burrow", "Burrow", 35, 24, 2),
	HIVE("hive", "Hive", 85, 36, 5),
	CITADEL("citadel", "Citadel", 155, 48, 9);

	private final String id;
	private final String displayName;
	private final int scoreThreshold;
	private final int claimRadius;
	private final int economyBonus;

	ColonyRank(String id, String displayName, int scoreThreshold, int claimRadius, int economyBonus) {
		this.id = id;
		this.displayName = displayName;
		this.scoreThreshold = scoreThreshold;
		this.claimRadius = claimRadius;
		this.economyBonus = economyBonus;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public int claimRadius() {
		return claimRadius;
	}

	public int economyBonus() {
		return economyBonus;
	}

	public static ColonyRank current(ColonyData colony) {
		int score = score(colony);
		ColonyRank rank = OUTPOST;
		for (ColonyRank candidate : values()) {
			if (score >= candidate.scoreThreshold) {
				rank = candidate;
			}
		}
		return rank;
	}

	public static int score(ColonyData colony) {
		long completedBuildings = colony.progress().buildingsView().stream().filter(ColonyBuilding::complete).count();
		long alliedRelations = colony.progress().knownColoniesView().values().stream().filter(state -> state == DiplomacyState.ALLY).count();
		return (int) completedBuildings * 10
				+ colony.population() * 2
				+ Math.max(0, colony.progress().reputation())
				+ colony.queenHealth() / 12
				+ (int) alliedRelations * 12;
	}
}
