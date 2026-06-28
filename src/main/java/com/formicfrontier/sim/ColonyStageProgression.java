package com.formicfrontier.sim;

/**
 * Content pillar: time-based settlement development.
 * <p>
 * Drives autonomous colony advancement: a colony with sufficient age and
 * resources advances to the next development stage without player input, and
 * each advancement unlocks a concrete new capability (its signature building,
 * enqueued so the existing builder loop will construct it). The runner is
 * deterministic and side-effect free apart from mutating {@link ColonyData}, so
 * a gametest can run it and assert the stage advance plus a real unlock.
 * <p>
 * Stages are monotonic and derive from colony state via {@link ColonyStage#earnedFrom},
 * so progression cannot be faked by a hidden counter and a colony never
 * regresses. Progression is wired into {@code ColonySavedState.tickEconomy()}
 * next to the economy, logistics, and caste job loops so it reaches normal play.
 */
public final class ColonyStageProgression {
	private ColonyStageProgression() {
	}

	public static ProgressionResult tick(ColonyData colony) {
		ColonyStage current = colony.stage();
		// A colony whose queen is lost is frozen at its current stage: it is no
		// longer a growing settlement. Progression resumes once she recovers.
		if (!colony.queenAlive()) {
			return new ProgressionResult(current, current, false, null);
		}
		ColonyStage earned = ColonyStage.earnedFrom(colony);
		if (earned.ordinal() <= current.ordinal()) {
			return new ProgressionResult(current, current, false, null);
		}

		// Monotonic advance through every stage between current and earned so each
		// intermediate signature building is unlocked exactly once.
		BuildingType unlocked = null;
		ColonyStage next = current;
		for (ColonyStage candidate : ColonyStage.values()) {
			if (candidate.ordinal() <= current.ordinal() || candidate.ordinal() > earned.ordinal()) {
				continue;
			}
			next = candidate;
			BuildingType signature = candidate.signatureBuilding();
			if (signature != null && unlockSignatureBuilding(colony, signature)) {
				unlocked = signature;
			}
			colony.addEvent("Colony advanced to the " + candidate.id() + " stage");
		}
		colony.setStage(next);
		return new ProgressionResult(current, next, true, unlocked);
	}

	/**
	 * Enqueue the signature building the stage just unlocked, unless it was
	 * already queued, planned, or completed. Returns true when this call actually
	 * unlocked it. The builder loop later starts and completes it from resources.
	 */
	private static boolean unlockSignatureBuilding(ColonyData colony, BuildingType signature) {
		ColonyProgress progress = colony.progress();
		if (progress.hasCompleted(signature)) {
			return false;
		}
		if (progress.buildQueueView().stream().anyMatch(type -> type == signature)) {
			return false;
		}
		if (progress.buildingsView().stream().anyMatch(building -> building.type() == signature)) {
			return false;
		}
		progress.buildQueue().add(signature);
		return true;
	}

	/**
	 * Mutable accumulator returned to callers and asserted by the gametest.
	 *
	 * @param previousStage stage before this tick
	 * @param stage         stage after this tick
	 * @param advanced      true when the stage increased this tick
	 * @param unlockedBuilding the concrete building type unlocked this tick, or null
	 */
	public record ProgressionResult(ColonyStage previousStage, ColonyStage stage, boolean advanced, BuildingType unlockedBuilding) {
	}
}
