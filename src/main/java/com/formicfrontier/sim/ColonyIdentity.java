package com.formicfrontier.sim;

public final class ColonyIdentity {
	private ColonyIdentity() {
	}

	public static ColonyPersonality personality(ColonyData colony) {
		return ColonyPersonality.forColony(colony.id(), colony.progress().culture(), colony.progress().playerAllied());
	}

	public static String relationshipKey(ColonyData colony) {
		return "formic_frontier.relationship." + relationshipId(colony);
	}

	public static String relationshipId(ColonyData colony) {
		if (!colony.progress().playerAllied()) {
			if ("wild".equals(colony.progress().faction())) {
				return "wild";
			}
			return "rival";
		}
		int reputation = colony.progress().reputation();
		if (reputation >= 50) {
			return "trusted";
		}
		if (reputation >= 15) {
			return "friendly";
		}
		if (reputation < 0) {
			return "strained";
		}
		return "new_allies";
	}

	public static int relationshipColor(ColonyData colony) {
		return switch (relationshipId(colony)) {
			case "trusted" -> 0x6DD08E;
			case "friendly" -> 0x91C46C;
			case "strained" -> 0xD69042;
			case "wild" -> 0xD8B57A;
			case "rival" -> 0xC15D48;
			default -> 0xC9974B;
		};
	}
}
