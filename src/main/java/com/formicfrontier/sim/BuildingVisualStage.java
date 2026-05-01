package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum BuildingVisualStage {
	PLANNED("planned"),
	CONSTRUCTION("construction"),
	COMPLETE("complete"),
	UPGRADED("upgraded"),
	DAMAGED("damaged"),
	REPAIRING("repairing");

	public static final Codec<BuildingVisualStage> CODEC = Codec.STRING.xmap(BuildingVisualStage::fromId, BuildingVisualStage::id);

	private final String id;

	BuildingVisualStage(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public String statusKey() {
		return "formic_frontier.ui.status." + id;
	}

	public static BuildingVisualStage from(ColonyBuilding building) {
		if (building.disabledTicks() > 0 && !building.complete()) {
			return REPAIRING;
		}
		if (building.disabledTicks() > 0) {
			return DAMAGED;
		}
		if (building.constructionProgress() <= 0) {
			return building.level() > 1 ? CONSTRUCTION : PLANNED;
		}
		if (!building.complete()) {
			return CONSTRUCTION;
		}
		if (building.level() > 1) {
			return UPGRADED;
		}
		return COMPLETE;
	}

	public static BuildingVisualStage fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (BuildingVisualStage stage : values()) {
			if (stage.id.equals(normalized)) {
				return stage;
			}
		}
		throw new IllegalArgumentException("Unknown building visual stage: " + id);
	}
}
