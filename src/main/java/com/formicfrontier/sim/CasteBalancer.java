package com.formicfrontier.sim;

/**
 * Content pillar: ant castes and roles - auto-balancing population over time.
 * <p>
 * A real ant colony does not just grow castes; it reassigns workers toward the
 * colony's current need. The growth loop in {@link ColonyEconomy} only ever adds
 * castes, so without this pass a colony that over-produced miners while starving
 * for food, or that lost defenders to a raid, would keep drifting away from a
 * viable composition. This pass re-specialises a surplus caste toward a starving
 * priority, mirroring how worker ants change roles inside a nest.
 * <p>
 * The pass is deterministic and side-effect free apart from mutating
 * {@link ColonyData}, so a gametest can run it and assert the resulting caste
 * shift. It is wired into {@code ColonySavedState.tickEconomy()} alongside the
 * other sim passes, so it reaches normal play, not dead code.
 * <p>
 * Design rules so the pass never destabilises a colony:
 * <ul>
 *   <li>It only fires when a colony need is actually <b>starving</b> (a hard
 *       threshold is breached), not on every tick - so a healthy colony is a
 *       no-op and existing growth/economy deltas are untouched.</li>
 *   <li>It only converts a caste the colony has <b>in surplus</b> (above its
 *       minimum working headroom), so it never strips a colony below the count
 *       it needs to keep functioning.</li>
 *   <li>It never touches the {@link AntCaste#QUEEN} and never reduces a count
 *       below zero.</li>
 *   <li>At most {@link #REASSIGN_CAP_PER_PASS} reassignments happen per pass, so
 *       the composition shifts gradually over time rather than flipping in one
 *       tick.</li>
 * </ul>
 */
public final class CasteBalancer {
	/** A balancing pass runs alongside each economy tick. */
	public static final int BALANCER_TICK_INTERVAL = ColonyEconomy.ECONOMY_TICK_INTERVAL;

	/** Most reassignments a single pass will perform, so the shift is gradual. */
	static final int REASSIGN_CAP_PER_PASS = 2;
	/** FOOD stockpile (in units of one economy-tick upkeep) below which FOOD is starving. */
	static final int FOOD_STARVATION_UPKEEP_MULTIPLE = 2;
	/** Resource stockpile below which ORE / CHITIN needs are starving. */
	static final int RESOURCE_STARVATION_THRESHOLD = 20;
	/** Total soldier+major count below which DEFENSE is starving. */
	static final int DEFENSE_STARVATION_THRESHOLD = 2;
	/** A caste is a surplus source only above this many bodies of headroom. */
	static final int SURPLUS_HEADROOM = 2;

	private CasteBalancer() {
	}

	public static BalanceResult tick(ColonyData colony) {
		BalanceResult result = new BalanceResult();
		// A colony whose queen is lost cannot reorganise: it is frozen, like the
		// growth and progression passes.
		if (!colony.queenAlive()) {
			return result;
		}
		int remaining = REASSIGN_CAP_PER_PASS;
		for (TaskPriority priority : colony.prioritiesView()) {
			if (remaining <= 0) {
				break;
			}
			Need need = detectNeed(colony, priority);
			if (need == null) {
				continue;
			}
			AntCaste source = surplusSourceFor(need, colony);
			if (source == null) {
				continue;
			}
			int available = Math.min(remaining, surplusCount(source, colony));
			if (available <= 0) {
				continue;
			}
			colony.addCaste(source, -available);
			colony.addCaste(need.target, available);
			result.apply(need, source, available);
			remaining -= available;
			colony.setCurrentTask("Reassigned " + available + " " + source.id() + " to " + need.target.id());
		}
		if (result.anyChange()) {
			colony.addEvent("Colony rebalanced castes toward " + result.needId() + " need");
		}
		return result;
	}

	private static Need detectNeed(ColonyData colony, TaskPriority priority) {
		return switch (priority) {
			case FOOD -> colony.resource(ResourceType.FOOD) < colony.upkeepPerEconomyTick() * FOOD_STARVATION_UPKEEP_MULTIPLE
					? new Need("food", AntCaste.WORKER, priority)
					: null;
			case ORE -> colony.resource(ResourceType.ORE) < RESOURCE_STARVATION_THRESHOLD
					? new Need("ore", AntCaste.MINER, priority)
					: null;
			case CHITIN -> colony.resource(ResourceType.CHITIN) < RESOURCE_STARVATION_THRESHOLD
					? new Need("chitin", AntCaste.WORKER, priority)
					: null;
			case DEFENSE -> colony.casteCount(AntCaste.SOLDIER) + colony.casteCount(AntCaste.MAJOR) < DEFENSE_STARVATION_THRESHOLD
					? new Need("defense", AntCaste.SOLDIER, priority)
					: null;
		};
	}

	/**
	 * Pick the surplus caste to convert FROM for a given need. Food and chitin
	 * shortages pull workers back from the mining line (a miner becomes a general
	 * worker), while ore and defense shortages pull general workers into a
	 * specialist role. This keeps the worker pool as the flexible reserve, the way
	 * a real colony uses minor workers.
	 */
	private static AntCaste surplusSourceFor(Need need, ColonyData colony) {
		return switch (need.target) {
			case WORKER -> colony.casteCount(AntCaste.MINER) > SURPLUS_HEADROOM ? AntCaste.MINER : null;
			case MINER, SOLDIER -> colony.casteCount(AntCaste.WORKER) > SURPLUS_HEADROOM ? AntCaste.WORKER : null;
			default -> null;
		};
	}

	private static int surplusCount(AntCaste source, ColonyData colony) {
		return Math.max(0, colony.casteCount(source) - SURPLUS_HEADROOM);
	}

	private static final class Need {
		final String id;
		final AntCaste target;
		final TaskPriority priority;

		Need(String id, AntCaste target, TaskPriority priority) {
			this.id = id;
			this.target = target;
			this.priority = priority;
		}
	}

	/**
	 * Mutable accumulator returned to callers and asserted by the gametest.
	 */
	public static final class BalanceResult {
		private String needId = "";
		private AntCaste source;
		private AntCaste target;
		private int reassigned;

		void apply(Need need, AntCaste fromCaste, int count) {
			this.reassigned += count;
			this.source = fromCaste;
			this.target = need.target;
			this.needId = need.id;
		}

		public int reassigned() {
			return reassigned;
		}

		public AntCaste source() {
			return source;
		}

		public AntCaste target() {
			return target;
		}

		public String needId() {
			return needId;
		}

		boolean anyChange() {
			return reassigned > 0;
		}
	}
}
