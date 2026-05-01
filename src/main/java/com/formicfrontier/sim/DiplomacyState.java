package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum DiplomacyState {
	ALLY("ally"),
	NEUTRAL("neutral"),
	RIVAL("rival"),
	WAR("war");

	public static final Codec<DiplomacyState> CODEC = Codec.STRING.xmap(DiplomacyState::fromId, DiplomacyState::id);

	private final String id;

	DiplomacyState(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public boolean hostile() {
		return this == RIVAL || this == WAR;
	}

	public DiplomacyState improve() {
		return switch (this) {
			case WAR -> RIVAL;
			case RIVAL -> NEUTRAL;
			case NEUTRAL, ALLY -> ALLY;
		};
	}

	public DiplomacyState worsen() {
		return switch (this) {
			case ALLY -> NEUTRAL;
			case NEUTRAL -> RIVAL;
			case RIVAL, WAR -> WAR;
		};
	}

	public static DiplomacyState fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (DiplomacyState state : values()) {
			if (state.id.equals(normalized)) {
				return state;
			}
		}
		throw new IllegalArgumentException("Unknown diplomacy state: " + id);
	}
}
