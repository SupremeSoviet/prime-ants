package com.formicfrontier.sim;

import java.util.Locale;

public enum DiplomacyAction {
	ENVOY("envoy", "Send Envoy", 8, 1, 0, 0, 3, ColonyRank.OUTPOST, Direction.IMPROVE),
	TRIBUTE("tribute", "Pay Tribute", 16, 1, 1, 0, 8, ColonyRank.BURROW, Direction.IMPROVE_TWICE),
	TRUCE("truce", "Broker Truce", 24, 2, 1, 0, 6, ColonyRank.HIVE, Direction.NEUTRALIZE),
	INCITE("incite", "Incite War", 14, 3, 0, 1, -6, ColonyRank.BURROW, Direction.WORSEN),
	WAR_PACT("war_pact", "War Pact", 28, 4, 1, 1, -10, ColonyRank.CITADEL, Direction.WAR);

	private final String id;
	private final String label;
	private final int tokenCost;
	private final int dustCost;
	private final int sealCost;
	private final int bannerCost;
	private final int reputationDelta;
	private final ColonyRank minRank;
	private final Direction direction;

	DiplomacyAction(String id, String label, int tokenCost, int dustCost, int sealCost, int bannerCost, int reputationDelta, ColonyRank minRank, Direction direction) {
		this.id = id;
		this.label = label;
		this.tokenCost = tokenCost;
		this.dustCost = dustCost;
		this.sealCost = sealCost;
		this.bannerCost = bannerCost;
		this.reputationDelta = reputationDelta;
		this.minRank = minRank;
		this.direction = direction;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public int tokenCost() {
		return tokenCost;
	}

	public int dustCost() {
		return dustCost;
	}

	public int sealCost() {
		return sealCost;
	}

	public int bannerCost() {
		return bannerCost;
	}

	public int reputationDelta() {
		return reputationDelta;
	}

	public ColonyRank minRank() {
		return minRank;
	}

	public DiplomacyState apply(DiplomacyState state) {
		return switch (direction) {
			case IMPROVE -> state.improve();
			case IMPROVE_TWICE -> state.improve().improve();
			case NEUTRALIZE -> state == DiplomacyState.WAR || state == DiplomacyState.RIVAL ? DiplomacyState.NEUTRAL : state.improve();
			case WORSEN -> state.worsen();
			case WAR -> DiplomacyState.WAR;
		};
	}

	public static DiplomacyAction fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (DiplomacyAction action : values()) {
			if (action.id.equals(normalized)) {
				return action;
			}
		}
		throw new IllegalArgumentException("Unknown diplomacy action: " + id);
	}

	private enum Direction {
		IMPROVE,
		IMPROVE_TWICE,
		NEUTRALIZE,
		WORSEN,
		WAR
	}
}
