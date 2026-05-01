package com.formicfrontier.sim;

import java.util.Locale;

public enum ColonyPersonality {
	STEADY("steady", "formic_frontier.personality.steady", "formic_frontier.personality.steady.detail", 0xD8B57A),
	CURIOUS("curious", "formic_frontier.personality.curious", "formic_frontier.personality.curious.detail", 0x8EC8D6),
	INDUSTRIOUS("industrious", "formic_frontier.personality.industrious", "formic_frontier.personality.industrious.detail", 0xD69042),
	GUARDED("guarded", "formic_frontier.personality.guarded", "formic_frontier.personality.guarded.detail", 0xC15D48);

	private final String id;
	private final String labelKey;
	private final String detailKey;
	private final int color;

	ColonyPersonality(String id, String labelKey, String detailKey, int color) {
		this.id = id;
		this.labelKey = labelKey;
		this.detailKey = detailKey;
		this.color = color;
	}

	public String id() {
		return id;
	}

	public String labelKey() {
		return labelKey;
	}

	public String detailKey() {
		return detailKey;
	}

	public int color() {
		return color;
	}

	public static ColonyPersonality forColony(int id, ColonyCulture culture, boolean playerAllied) {
		int cultureSeed = switch (culture) {
			case AMBER -> 0;
			case LEAFCUTTER -> 1;
			case CARPENTER -> 2;
			case FIRE -> 3;
		};
		int alliedSeed = playerAllied ? 0 : 1;
		return values()[Math.floorMod(id + cultureSeed + alliedSeed - 1, values().length)];
	}

	public static ColonyPersonality fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (ColonyPersonality personality : values()) {
			if (personality.id.equals(normalized)) {
				return personality;
			}
		}
		throw new IllegalArgumentException("Unknown colony personality: " + id);
	}
}
