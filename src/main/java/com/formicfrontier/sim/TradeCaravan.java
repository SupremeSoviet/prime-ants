package com.formicfrontier.sim;

/**
 * Content row {@code trade_caravan_exchanges_resources}: a colony-to-colony
 * caravan / market exchange that moves resources between two colonies at a
 * rate that depends on their {@link DiplomacyState relation} and on resource
 * scarcity. The direction of the exchange is scarcity-driven (surplus colony
 * ships to the scarce colony) and the quantity shipped is relation-driven
 * (allies trade freely, neutrals trade less, rivals refuse to trade).
 *
 * <p>This is wired into the autonomous economy tick via
 * {@code ColonySavedState.tickEconomy()} so it reaches normal play, not dead
 * code. It operates purely on colony state (no server/level coupling) so the
 * deterministic economy-tick gametest can exercise it end to end.
 */
public final class TradeCaravan {
	/** Base amount a single caravan carries before relation/scarcity modifiers. */
	public static final int BASE_CARGO = 8;
	/** Surplus threshold: a colony is a "supplier" for a resource above this. */
	public static final int SURPLUS_THRESHOLD = 40;
	/** Scarcity threshold: a colony is "starving" for a resource below this. */
	public static final int SCARCITY_THRESHOLD = 20;

	private TradeCaravan() {
	}

	/**
	 * Attempt one caravan exchange between two colonies. The exchange only fires
	 * when (a) the {@code source} has a completed MARKET (the building that
	 * enables caravans in normal play) and (b) there exists a resource the
	 * source has in surplus that the target is scarce on. The shipped quantity is
	 * scaled by the source's relation toward the target.
	 *
	 * @return the result of the exchange (never null; {@link ExchangeResult#occurred()}
	 *         is false when no caravan ran).
	 */
	public static ExchangeResult exchange(ColonyData source, ColonyData target) {
		if (source == null || target == null || source.id() == target.id()) {
			return ExchangeResult.none();
		}
		if (!source.progress().hasCompleted(BuildingType.MARKET)) {
			return ExchangeResult.none();
		}
		DiplomacyState relation = source.progress().relationTo(target.id());
		double rate = relationRate(relation);
		if (rate <= 0.0) {
			// Rivals (and colonies at war) refuse to trade.
			return ExchangeResult.none();
		}
		ResourceType cargo = pickCargo(source, target);
		if (cargo == null) {
			return ExchangeResult.none();
		}
		int surplus = source.resource(cargo);
		int scarce = target.resource(cargo);
		int shipped = shippedAmount(surplus, scarce, rate);
		if (shipped <= 0) {
			return ExchangeResult.none();
		}
		source.addResource(cargo, -shipped);
		target.addResource(cargo, shipped);
		source.progress().addReputation(+1);
		source.setCurrentTask("Caravan shipped " + shipped + " " + cargo.id() + " to colony #" + target.id());
		source.addEvent("Trade caravan: sent " + shipped + " " + cargo.id() + " to colony #" + target.id()
				+ " (" + relation.id() + ", rate " + rate + ")");
		target.addEvent("Trade caravan: received " + shipped + " " + cargo.id() + " from colony #" + source.id());
		return new ExchangeResult(true, cargo, shipped, relation, rate);
	}

	/**
	 * Relation-based trade-rate multiplier. Allies trade most freely, neutrals
	 * less, and rivals/war refuse entirely. This is the mechanical link the
	 * content row requires: a diplomatic relation changes a trade outcome.
	 */
	public static double relationRate(DiplomacyState relation) {
		return switch (relation) {
			case ALLY -> 1.0;
			case NEUTRAL -> 0.5;
			case RIVAL, WAR -> 0.0;
		};
	}

	/**
	 * Pick the resource to ship: the first tradeable resource the source holds in
	 * surplus while the target is scarce on it. This is the scarcity-driven half
	 * of the rate (direction + eligibility).
	 */
	static ResourceType pickCargo(ColonyData source, ColonyData target) {
		for (ResourceType type : tradableResources()) {
			if (source.resource(type) > SURPLUS_THRESHOLD && target.resource(type) < SCARCITY_THRESHOLD) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Compute the shipped quantity from the raw surplus/scarce levels and the
	 * relation rate. Kept deterministic and independent of server randomness.
	 */
	static int shippedAmount(int surplus, int scarce, double rate) {
		// Carry a relation-scaled slice of the base cargo, capped so the source
		// stays above the surplus threshold and never ships more than it holds.
		int scaled = (int) Math.round(BASE_CARGO * rate);
		int capacityHeadroom = Math.max(0, surplus - SURPLUS_THRESHOLD);
		int shipped = Math.min(scaled, capacityHeadroom);
		return Math.max(0, shipped);
	}

	/**
	 * Resources a caravan can move between colonies. KNOWLEDGE is excluded: it is
	 * a research currency, not a bulk trade good.
	 */
	static ResourceType[] tradableResources() {
		return new ResourceType[] {
				ResourceType.FOOD,
				ResourceType.ORE,
				ResourceType.CHITIN,
				ResourceType.RESIN,
				ResourceType.FUNGUS,
				ResourceType.VENOM
		};
	}

	public record ExchangeResult(boolean occurred, ResourceType cargo, int shipped, DiplomacyState relation, double rate) {
		public static ExchangeResult none() {
			return new ExchangeResult(false, null, 0, null, 0.0);
		}
	}
}
