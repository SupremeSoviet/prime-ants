package com.formicfrontier.sim;

import java.util.Locale;

public enum AntWorkState {
	IDLE("idle"),
	WORKING("working"),
	CARRYING_FOOD("carrying_food"),
	CARRYING_ORE("carrying_ore"),
	CARRYING_CHITIN("carrying_chitin"),
	CARRYING_RESIN("carrying_resin"),
	CARRYING_FUNGUS("carrying_fungus"),
	CARRYING_VENOM("carrying_venom"),
	PATROLLING("patrolling");

	private final String id;

	AntWorkState(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static AntWorkState carrying(ResourceType type) {
		return switch (type) {
			case FOOD -> CARRYING_FOOD;
			case ORE -> CARRYING_ORE;
			case CHITIN -> CARRYING_CHITIN;
			case RESIN -> CARRYING_RESIN;
			case FUNGUS -> CARRYING_FUNGUS;
			case VENOM -> CARRYING_VENOM;
			case KNOWLEDGE -> WORKING;
		};
	}

	public static AntWorkState fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (AntWorkState state : values()) {
			if (state.id.equals(normalized)) {
				return state;
			}
		}
		return IDLE;
	}
}
