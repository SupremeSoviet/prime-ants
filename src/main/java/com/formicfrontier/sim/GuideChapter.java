package com.formicfrontier.sim;

public enum GuideChapter {
	FIRST_STEPS("first_steps", "formic_frontier.guide.first_steps", "formic_frontier.guide.first_steps.detail", "formic_frontier.guide.unlock.always", 0xF0C26E),
	CASTES("castes", "formic_frontier.guide.castes", "formic_frontier.guide.castes.detail", "formic_frontier.guide.unlock.always", 0xD8B57A),
	RESOURCES("resources", "formic_frontier.guide.resources", "formic_frontier.guide.resources.detail", "formic_frontier.guide.unlock.always", 0x91C46C),
	BUILDINGS("buildings", "formic_frontier.guide.buildings", "formic_frontier.guide.buildings.detail", "formic_frontier.guide.unlock.always", 0xD69042),
	CULTURES("cultures", "formic_frontier.guide.cultures", "formic_frontier.guide.cultures.detail", "formic_frontier.guide.unlock.always", 0xB58BFF),
	HELPING("helping", "formic_frontier.guide.helping", "formic_frontier.guide.helping.detail", "formic_frontier.guide.unlock.always", 0x6DD08E),
	RELATIONS("relations", "formic_frontier.guide.relations", "formic_frontier.guide.relations.detail", "formic_frontier.guide.relations.locked", 0x8EC8D6),
	RESEARCH("research", "formic_frontier.guide.research", "formic_frontier.guide.research.detail", "formic_frontier.guide.research.locked", 0xC9974B);

	private final String id;
	private final String titleKey;
	private final String detailKey;
	private final String lockedKey;
	private final int color;

	GuideChapter(String id, String titleKey, String detailKey, String lockedKey, int color) {
		this.id = id;
		this.titleKey = titleKey;
		this.detailKey = detailKey;
		this.lockedKey = lockedKey;
		this.color = color;
	}

	public String id() {
		return id;
	}

	public String titleKey() {
		return titleKey;
	}

	public String detailKey() {
		return detailKey;
	}

	public String lockedKey() {
		return lockedKey;
	}

	public int color() {
		return color;
	}

	public boolean unlocked(ColonyData colony) {
		return switch (this) {
			case FIRST_STEPS, CASTES, RESOURCES, BUILDINGS, CULTURES, HELPING -> true;
			case RELATIONS -> !colony.progress().knownColoniesView().isEmpty()
					|| colony.progress().reputation() != 0
					|| ColonyRank.current(colony).ordinal() >= ColonyRank.BURROW.ordinal();
			case RESEARCH -> colony.progress().hasCompleted(BuildingType.PHEROMONE_ARCHIVE)
					|| colony.progress().activeResearch().isPresent()
					|| !colony.progress().completedResearchView().isEmpty()
					|| colony.resource(ResourceType.KNOWLEDGE) > 0;
		};
	}
}
