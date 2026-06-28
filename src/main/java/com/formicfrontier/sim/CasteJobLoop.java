package com.formicfrontier.sim;

/**
 * Content pillar: ant castes and roles.
 * <p>
 * Each active caste owns a job loop that measurably changes colony state when the
 * colony tick advances, instead of only existing as a model/entity:
 * <ul>
 *   <li><b>gather</b> - workers forage food, miners haul ore, scouts collect chitin (resource deltas).</li>
 *   <li><b>build</b> - builder workers advance the active construction site (construction progress).</li>
 *   <li><b>patrol</b> - soldiers/majors restore raid-damaged defenses and stand down raid readiness (defense recovery).</li>
 *   <li><b>tend</b> - brood-tending workers nurse the queen back toward full health (queen recovery).</li>
 * </ul>
 * The loop is deterministic and side-effect free apart from mutating {@link ColonyData}, so a gametest
 * can run it and assert the resulting resource/construction/defense/queen change.
 */
public final class CasteJobLoop {
	/** A single job-loop pass runs alongside each economy tick. */
	public static final int JOB_LOOP_TICK_INTERVAL = ColonyEconomy.ECONOMY_TICK_INTERVAL;

	/** Cap so a huge worker swarm never finishes a site in a single pass. */
	static final int BUILD_PROGRESS_CAP_PER_LOOP = 10;
	/** Cap so a huge worker swarm never fully revives the queen in a single pass. */
	static final int TEND_HEALTH_CAP_PER_LOOP = 5;
	/** Patrol teams only need a few defenders to accelerate each building's recovery. */
	static final int PATROL_DEFENDERS_PER_BUILDING = 3;

	private CasteJobLoop() {
	}

	public static JobLoopResult tick(ColonyData colony) {
		int workers = colony.casteCount(AntCaste.WORKER);
		int miners = colony.casteCount(AntCaste.MINER);
		int scouts = colony.casteCount(AntCaste.SCOUT);
		int defenders = colony.casteCount(AntCaste.SOLDIER) + colony.casteCount(AntCaste.MAJOR);

		JobLoopResult result = new JobLoopResult();

		gather(colony, workers, miners, scouts, result);
		build(colony, workers, result);
		patrol(colony, defenders, result);
		tend(colony, workers, result);

		if (result.anyChange()) {
			colony.setCurrentTask(describeTask(result));
		}
		return result;
	}

	private static void gather(ColonyData colony, int workers, int miners, int scouts, JobLoopResult result) {
		// GATHER: foraging/mining/scouting castes haul resources from sources to the store.
		result.gatheredFood = workers;
		result.gatheredOre = miners * 2;
		result.gatheredChitin = scouts;
		colony.addResource(ResourceType.FOOD, result.gatheredFood);
		colony.addResource(ResourceType.ORE, result.gatheredOre);
		colony.addResource(ResourceType.CHITIN, result.gatheredChitin);
	}

	private static void build(ColonyData colony, int workers, JobLoopResult result) {
		// BUILD: builder workers carry material to the active construction site and raise it.
		if (workers <= 0) {
			return;
		}
		ColonyBuilding site = colony.progress().firstIncomplete().orElse(null);
		if (site == null) {
			return;
		}
		int contribution = Math.min(workers, BUILD_PROGRESS_CAP_PER_LOOP);
		int before = site.constructionProgress();
		site.addConstructionProgress(contribution);
		result.builtProgress = site.constructionProgress() - before;
		if (site.complete()) {
			colony.addEvent("Builder crew completed " + site.type().id());
		}
	}

	private static void patrol(ColonyData colony, int defenders, JobLoopResult result) {
		// PATROL: soldier/major teams restore raid-damaged defenses and stand down raid readiness.
		if (defenders <= 0) {
			return;
		}
		for (ColonyBuilding building : colony.progress().buildings()) {
			if (!building.damaged()) {
				continue;
			}
			int before = building.disabledTicks();
			building.tickDisabled();
			int patrolBoost = Math.min(defenders, PATROL_DEFENDERS_PER_BUILDING);
			for (int s = 0; s < patrolBoost; s++) {
				building.tickDisabled();
			}
			if (building.disabledTicks() < before) {
				result.patrolsResolved++;
			}
		}
		colony.progress().tickRaidCooldown();
	}

	private static void tend(ColonyData colony, int workers, JobLoopResult result) {
		// TEND: brood-tending workers nurse a wounded queen back toward full health.
		int maxQueen = (int) AntCaste.QUEEN.health();
		if (workers <= 0 || !colony.queenAlive() || colony.queenHealth() >= maxQueen) {
			return;
		}
		int before = colony.queenHealth();
		colony.setQueenHealth(Math.min(maxQueen, before + Math.min(workers, TEND_HEALTH_CAP_PER_LOOP)));
		result.queenTended = colony.queenHealth() - before;
	}

	private static String describeTask(JobLoopResult result) {
		StringBuilder task = new StringBuilder("Job loop:");
		if (result.gatheredFood + result.gatheredOre + result.gatheredChitin > 0) {
			task.append(" gathered ").append(result.gatheredFood).append(" food/")
					.append(result.gatheredOre).append(" ore/").append(result.gatheredChitin).append(" chitin");
		}
		if (result.builtProgress > 0) {
			task.append(" built ").append(result.builtProgress).append("%");
		}
		if (result.patrolsResolved > 0) {
			task.append(" patrols restored ").append(result.patrolsResolved).append(" defenses");
		}
		if (result.queenTended > 0) {
			task.append(" tended queen +").append(result.queenTended).append(" hp");
		}
		return task.toString();
	}

	/**
	 * Mutable accumulator populated as each job loop runs, returned to callers and
	 * asserted by the gametest. Fields are package-private so the loop helpers can
	 * write them without a builder.
	 */
	public static final class JobLoopResult {
		int gatheredFood;
		int gatheredOre;
		int gatheredChitin;
		int builtProgress;
		int patrolsResolved;
		int queenTended;

		public int gatheredFood() {
			return gatheredFood;
		}

		public int gatheredOre() {
			return gatheredOre;
		}

		public int gatheredChitin() {
			return gatheredChitin;
		}

		public int builtProgress() {
			return builtProgress;
		}

		public int patrolsResolved() {
			return patrolsResolved;
		}

		public int queenTended() {
			return queenTended;
		}

		boolean anyChange() {
			return gatheredFood + gatheredOre + gatheredChitin + builtProgress + patrolsResolved + queenTended > 0;
		}
	}
}
