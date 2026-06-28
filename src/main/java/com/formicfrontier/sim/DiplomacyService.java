package com.formicfrontier.sim;

/**
 * Content row {@code politics_relations_shift_from_actions}: a diplomatic
 * action (envoy, tribute, truce, incite, war pact) shifts the recorded relation
 * between two colonies, and that shift has a mechanical consequence because
 * {@link TradeCaravan#relationRate(DiplomacyState)} scales (or blocks) trade.
 *
 * <p>The actions themselves are declared on {@link DiplomacyAction}; this class
 * performs one action against live colony state: it enforces the action's rank
 * gate, applies {@link DiplomacyAction#apply(DiplomacyState)} to the current
 * relation toward the target, records the shifted relation via
 * {@link ColonyProgress#setRelation(int, DiplomacyState)}, logs a colony event,
 * and reports the before/after state so a gametest can assert both the relation
 * change and its mechanical effect on trade.
 *
 * <p>This is wired into the autonomous economy tick via
 * {@code ColonySavedState.tickEconomy()} (a recurring envoy pass) so it reaches
 * normal play, not dead code. It operates purely on colony state (no
 * server/level coupling) so the deterministic economy-tick gametest can exercise
 * it end to end.
 */
public final class DiplomacyService {
	/** Fraction of a completed DIPLOMACY_SHRINE's capacity an autonomous envoy pass spends per economy tick. */
	public static final int ENVOY_PASS_INTERVAL_TICKS = 60;

	private DiplomacyService() {
	}

	/**
	 * Perform one diplomatic action of {@code actor} toward {@code target}.
	 *
	 * <p>The action's {@link DiplomacyAction#minRank()} rank gate is enforced
	 * against the actor's current rank: a colony cannot, for example, broker a
	 * TRUCE or declare a WAR_PACT before it has reached the required standing.
	 * On success the relation toward the target is shifted by
	 * {@link DiplomacyAction#apply(DiplomacyState)} and recorded.
	 *
	 * @return the result of the action (never null; {@link ActionResult#occurred()}
	 *         is false when it was rejected by the rank gate or bad input).
	 */
	public static ActionResult perform(ColonyData actor, ColonyData target, DiplomacyAction action) {
		if (actor == null || target == null || action == null || actor.id() == target.id()) {
			return ActionResult.none();
		}
		if (!action.minRank().atLeast(actor)) {
			return ActionResult.rejected(action, actor.progress().relationTo(target.id()), "rank gate");
		}
		DiplomacyState before = actor.progress().relationTo(target.id());
		DiplomacyState after = action.apply(before);
		actor.progress().setRelation(target.id(), after);
		actor.progress().addReputation(action.reputationDelta());
		actor.addEvent("Diplomacy: " + action.label() + " toward colony #" + target.id()
				+ " (" + before.id() + " -> " + after.id() + ")");
		return new ActionResult(true, action, before, after, null);
	}

	/**
	 * Recurring autonomous diplomacy pass run from the economy tick. A colony
	 * with a completed {@link BuildingType#DIPLOMACY_SHRINE} that has reached at
	 * least BURROW rank spends ENVOY (the lowest-rank improving action) toward
	 * its nearest non-allied known colony, gradually turning neutrals into
	 * allies - which the subsequent caravan pass then rewards with the full ally
	 * trade rate. This is the "world moves without the player" half of the row.
	 */
	public static void tick(ColonyData actor, ColonyData target) {
		if (actor == null || target == null || actor.id() == target.id()) {
			return;
		}
		if (!actor.progress().hasCompleted(BuildingType.DIPLOMACY_SHRINE)) {
			return;
		}
		if (!ColonyRank.BURROW.atLeast(actor)) {
			return;
		}
		// Only envoys are issued autonomously; aggressive actions stay player- or
		// raid-driven. No point env toward an existing ally.
		if (actor.progress().relationTo(target.id()) == DiplomacyState.ALLY) {
			return;
		}
		perform(actor, target, DiplomacyAction.ENVOY);
	}

	public record ActionResult(boolean occurred, DiplomacyAction action, DiplomacyState before, DiplomacyState after, String rejection) {
		public static ActionResult none() {
			return new ActionResult(false, null, null, null, "no-op");
		}

		public static ActionResult rejected(DiplomacyAction action, DiplomacyState relation, String reason) {
			return new ActionResult(false, action, relation, relation, reason);
		}
	}
}
