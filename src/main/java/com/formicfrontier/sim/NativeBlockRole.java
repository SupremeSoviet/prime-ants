package com.formicfrontier.sim;

/**
 * Content pillar: native blocks and materials.
 * <p>
 * Native Formic blocks must carry real gameplay roles, not decoration. This loop
 * gives the {@link BuildingType#FUNGUS_GARDEN} block an active colony-tick role
 * that is distinct from its passive income multiplier in {@link ColonyEconomy}:
 * each completed fungus garden <b>composts stored FOOD into FUNGUS</b>. Unlike a
 * passive income bonus, this is a real resource-to-resource conversion with an
 * input cost (FOOD is consumed) and a starvation guard (the garden never feeds
 * the colony below the food reserve, so it cannot starve the queen). The loop is
 * deterministic and side-effect free apart from mutating {@link ColonyData}, so a
 * gametest can run it and assert the resulting FOOD and FUNGUS deltas.
 * <p>
 * The role is reachable in normal play: it runs inside
 * {@code ColonySavedState.tickEconomy()} alongside the other sim passes, and the
 * FUNGUS_GARDEN block is craftable/placeable (see data/formic_frontier/recipe
 * and the blockstate/model/item assets).
 */
public final class NativeBlockRole {
	/** A native-block pass runs alongside each economy tick. */
	public static final int NATIVE_BLOCK_TICK_INTERVAL = ColonyEconomy.ECONOMY_TICK_INTERVAL;

	/** FOOD consumed per completed fungus garden per pass. */
	static final int FOOD_PER_GARDEN = 5;
	/** FUNGUS produced per completed fungus garden per pass. */
	static final int FUNGUS_PER_GARDEN = 8;
	/** Minimum FOOD the colony must keep in reserve; gardens will not compost below this. */
	static final int FOOD_RESERVE = 40;

	private NativeBlockRole() {
	}

	public static BlockRoleResult tick(ColonyData colony) {
		int gardens = completed(colony, BuildingType.FUNGUS_GARDEN);
		BlockRoleResult result = new BlockRoleResult();
		if (gardens <= 0) {
			return result;
		}
		// Each garden composts FOOD into FUNGUS, but never drains the food stockpile
		// below the reserve. The number of gardens that actually run this pass is the
		// limiting factor, so a hungry colony is protected from being starved by its
		// own gardens.
		int wantedFood = gardens * FOOD_PER_GARDEN;
		int availableAboveReserve = Math.max(0, colony.resource(ResourceType.FOOD) - FOOD_RESERVE);
		int gardensRun = Math.min(gardens, availableAboveReserve / FOOD_PER_GARDEN);
		if (gardensRun <= 0) {
			return result;
		}
		int foodConsumed = gardensRun * FOOD_PER_GARDEN;
		int fungusProduced = gardensRun * FUNGUS_PER_GARDEN;
		colony.addResource(ResourceType.FOOD, -foodConsumed);
		colony.addResource(ResourceType.FUNGUS, fungusProduced);
		result.gardensRun = gardensRun;
		result.foodComposted = foodConsumed;
		result.fungusCultivated = fungusProduced;
		return result;
	}

	private static int completed(ColonyData colony, BuildingType type) {
		return (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == type && building.complete())
				.count();
	}

	/**
	 * Mutable accumulator returned to callers and asserted by the gametest.
	 */
	public static final class BlockRoleResult {
		int gardensRun;
		int foodComposted;
		int fungusCultivated;

		public int gardensRun() {
			return gardensRun;
		}

		public int foodComposted() {
			return foodComposted;
		}

		public int fungusCultivated() {
			return fungusCultivated;
		}

		public boolean anyChange() {
			return gardensRun > 0;
		}
	}
}
