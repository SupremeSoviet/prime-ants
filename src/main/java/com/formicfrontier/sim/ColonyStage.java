package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Content pillar: time-based settlement development.
 * <p>
 * A colony advances through development stages over time: founding -> growth ->
 * established -> mature. Each stage is reached autonomously once the colony has
 * lived long enough and stockpiled enough resources, and reaching a stage
 * unlocks a concrete new capability (building/caste/recipe) so progression has a
 * real mechanical effect rather than being a relabelled state field. Stages are
 * monotonic: a colony never regresses to an earlier stage.
 * <p>
 * The thresholds live here (not on a tick runner) so the stage can be derived
 * deterministically from colony state and asserted by a gametest. Age thresholds
 * are expressed in {@link ColonyEconomy#ECONOMY_TICK_INTERVAL}-aligned age ticks.
 */
public enum ColonyStage {
	/** Just founded. No stage-specific unlock. */
	FOUNDING("founding", 0, 0, 0, null),
	/** The colony is producing and growing its first work force. */
	GROWTH("growth", 3, 120, 40, BuildingType.BARRACKS),
	/** A settled, self-sustaining colony with specialized infrastructure. */
	ESTABLISHED("established", 6, 400, 180, BuildingType.PHEROMONE_ARCHIVE),
	/** A mature colony undertaking long-term and endgame projects. */
	MATURE("mature", 12, 900, 500, BuildingType.GREAT_MOUND);

	public static final Codec<ColonyStage> CODEC = Codec.STRING.xmap(ColonyStage::fromId, ColonyStage::id);

	private final String id;
	private final int economyTickThreshold;
	private final int foodThreshold;
	private final int compositeResourceThreshold;
	private final BuildingType signatureBuilding;

	ColonyStage(String id, int economyTickThreshold, int foodThreshold, int compositeResourceThreshold, BuildingType signatureBuilding) {
		this.id = id;
		this.economyTickThreshold = economyTickThreshold;
		this.foodThreshold = foodThreshold;
		this.compositeResourceThreshold = compositeResourceThreshold;
		this.signatureBuilding = signatureBuilding;
	}

	public String id() {
		return id;
	}

	/** Number of {@link ColonyEconomy#ECONOMY_TICK_INTERVAL}-scaled economy ticks the colony must survive. */
	public int economyTickThreshold() {
		return economyTickThreshold;
	}

	public int foodThreshold() {
		return foodThreshold;
	}

	/** Combined ore+chitin+resin stockpile required to prove diversified production. */
	public int compositeResourceThreshold() {
		return compositeResourceThreshold;
	}

	/**
	 * The building type this stage unlocks as a concrete progression effect. The
	 * progression runner enqueues it once the stage is reached; reaching a stage
	 * always has a mechanical consequence. {@code null} only for FOUNDING.
	 */
	public BuildingType signatureBuilding() {
		return signatureBuilding;
	}

	public boolean isStartStage() {
		return this == FOUNDING;
	}

	/**
	 * Resolve the highest stage a colony has earned from its age and resource
	 * stockpiles. Used both to advance the recorded stage and to assert that
	 * progression is driven by colony state, not by a hidden counter.
	 */
	public static ColonyStage earnedFrom(ColonyData colony) {
		int ageTicks = colony.ageTicks() / ColonyEconomy.ECONOMY_TICK_INTERVAL;
		int food = colony.resource(ResourceType.FOOD);
		int composite = colony.resource(ResourceType.ORE)
				+ colony.resource(ResourceType.CHITIN)
				+ colony.resource(ResourceType.RESIN);
		ColonyStage earned = FOUNDING;
		for (ColonyStage candidate : values()) {
			if (candidate.isStartStage()) {
				continue;
			}
			if (ageTicks >= candidate.economyTickThreshold()
					&& food >= candidate.foodThreshold()
					&& composite >= candidate.compositeResourceThreshold) {
				earned = candidate;
			}
		}
		return earned;
	}

	public static ColonyStage fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (ColonyStage stage : values()) {
			if (stage.id.equals(normalized)) {
				return stage;
			}
		}
		throw new IllegalArgumentException("Unknown colony stage: " + id);
	}
}
