package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ColonyProgress {
	public static final Codec<ColonyProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ColonyCulture.CODEC.optionalFieldOf("culture", ColonyCulture.AMBER).forGetter(ColonyProgress::culture),
			Codec.STRING.optionalFieldOf("faction", "wild").forGetter(ColonyProgress::faction),
			Codec.STRING.optionalFieldOf("name", "Unnamed Colony").forGetter(ColonyProgress::name),
			Codec.INT.optionalFieldOf("color", 0x8a5b32).forGetter(ColonyProgress::color),
			Codec.BOOL.optionalFieldOf("playerAllied", false).forGetter(ColonyProgress::playerAllied),
			Codec.INT.optionalFieldOf("reputation", 0).forGetter(ColonyProgress::reputation),
			Codec.INT.optionalFieldOf("claimRadius", 18).forGetter(ColonyProgress::claimRadius),
			ColonyBuilding.CODEC.listOf().optionalFieldOf("buildings", List.of()).forGetter(ColonyProgress::buildingsView),
			BuildingType.CODEC.listOf().optionalFieldOf("buildQueue", List.of()).forGetter(ColonyProgress::buildQueueView),
			Codec.unboundedMap(Codec.STRING, DiplomacyState.CODEC).optionalFieldOf("knownColonies", Map.of()).forGetter(ColonyProgress::knownColoniesView),
			RaidPlan.CODEC.listOf().optionalFieldOf("raidPlans", List.of()).forGetter(ColonyProgress::raidPlansView),
			Codec.INT.optionalFieldOf("raidCooldown", 0).forGetter(ColonyProgress::raidCooldown),
			ColonyEvent.CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(ColonyProgress::eventsView),
			ColonyRequest.CODEC.listOf().optionalFieldOf("requests", List.of()).forGetter(ColonyProgress::requestsView),
			Codec.STRING.listOf().optionalFieldOf("completedResearch", List.of()).forGetter(ColonyProgress::completedResearchView),
			ResearchState.CODEC.optionalFieldOf("activeResearch").forGetter(ColonyProgress::activeResearch)
	).apply(instance, ColonyProgress::new));

	private final ColonyCulture culture;
	private final String faction;
	private final String name;
	private final int color;
	private final boolean playerAllied;
	private int reputation;
	private int claimRadius;
	private int raidCooldown;
	private final List<ColonyBuilding> buildings = new ArrayList<>();
	private final List<BuildingType> buildQueue = new ArrayList<>();
	private final Map<String, DiplomacyState> knownColonies = new LinkedHashMap<>();
	private final List<RaidPlan> raidPlans = new ArrayList<>();
	private final List<ColonyEvent> events = new ArrayList<>();
	private final List<ColonyRequest> requests = new ArrayList<>();
	private final List<String> completedResearch = new ArrayList<>();
	private Optional<ResearchState> activeResearch = Optional.empty();

	public ColonyProgress(ColonyCulture culture, String faction, String name, int color, boolean playerAllied, int reputation, int claimRadius, List<ColonyBuilding> buildings, List<BuildingType> buildQueue, Map<String, DiplomacyState> knownColonies, List<RaidPlan> raidPlans, int raidCooldown, List<ColonyEvent> events, List<ColonyRequest> requests, List<String> completedResearch, Optional<ResearchState> activeResearch) {
		this.culture = culture;
		this.faction = faction;
		this.name = name;
		this.color = color;
		this.playerAllied = playerAllied;
		this.reputation = clampReputation(reputation);
		this.claimRadius = Math.max(18, Math.min(48, claimRadius));
		this.buildings.addAll(buildings);
		this.buildQueue.addAll(buildQueue);
		this.knownColonies.putAll(knownColonies);
		this.raidPlans.addAll(raidPlans);
		this.raidCooldown = Math.max(0, raidCooldown);
		this.events.addAll(events);
		this.requests.addAll(requests);
		this.completedResearch.addAll(completedResearch);
		this.activeResearch = activeResearch;
	}

	public static ColonyProgress allied(int id) {
		return new ColonyProgress(ColonyCulture.AMBER, "allied", "Amber Burrow " + id, ColonyCulture.AMBER.color(), true, 0, 18, List.of(), List.of(), Map.of(), List.of(), 0, List.of(), List.of(), List.of(), Optional.empty());
	}

	public static ColonyProgress rival(int id) {
		return rival(id, ColonyCulture.rivalFor(id));
	}

	public static ColonyProgress rival(int id, ColonyCulture culture) {
		return new ColonyProgress(culture, "rival", culture.displayName() + " " + id, culture.color(), false, -10, 18, List.of(), List.of(), Map.of(), List.of(), culture == ColonyCulture.FIRE ? 120 : 200, List.of(), List.of(), List.of(), Optional.empty());
	}

	public static ColonyProgress wild(int id, ColonyCulture culture) {
		return new ColonyProgress(culture, "wild", culture.displayName() + " Wild Nest " + id, culture.color(), false, -2, 18, List.of(), List.of(), Map.of(), List.of(), 240, List.of(), List.of(), List.of(), Optional.empty());
	}

	public ColonyCulture culture() {
		return culture;
	}

	public String faction() {
		return faction;
	}

	public String name() {
		return name;
	}

	public int color() {
		return color;
	}

	public boolean playerAllied() {
		return playerAllied;
	}

	public int reputation() {
		return reputation;
	}

	public void addReputation(int delta) {
		reputation = clampReputation(reputation + delta);
	}

	public int claimRadius() {
		return claimRadius;
	}

	public void setClaimRadius(int claimRadius) {
		this.claimRadius = Math.max(18, Math.min(48, claimRadius));
	}

	public int raidCooldown() {
		return raidCooldown;
	}

	public void setRaidCooldown(int raidCooldown) {
		this.raidCooldown = Math.max(0, raidCooldown);
	}

	public void tickRaidCooldown() {
		if (raidCooldown > 0) {
			raidCooldown--;
		}
	}

	public List<ColonyBuilding> buildingsView() {
		return List.copyOf(buildings);
	}

	public List<BuildingType> buildQueueView() {
		return List.copyOf(buildQueue);
	}

	public Map<String, DiplomacyState> knownColoniesView() {
		return Map.copyOf(knownColonies);
	}

	public List<RaidPlan> raidPlansView() {
		return List.copyOf(raidPlans);
	}

	public List<ColonyEvent> eventsView() {
		return List.copyOf(events);
	}

	public List<ColonyRequest> requestsView() {
		return List.copyOf(requests);
	}

	public List<String> completedResearchView() {
		return List.copyOf(completedResearch);
	}

	public Optional<ResearchState> activeResearch() {
		return activeResearch;
	}

	public List<ColonyBuilding> buildings() {
		return buildings;
	}

	public List<BuildingType> buildQueue() {
		return buildQueue;
	}

	public List<RaidPlan> raidPlans() {
		return raidPlans;
	}

	public List<ColonyRequest> requests() {
		return requests;
	}

	public void setActiveResearch(Optional<ResearchState> activeResearch) {
		this.activeResearch = activeResearch;
	}

	public boolean hasResearch(String nodeId) {
		return completedResearch.contains(nodeId);
	}

	public void completeResearch(String nodeId) {
		if (!completedResearch.contains(nodeId)) {
			completedResearch.add(nodeId);
		}
	}

	public void addEvent(int ageTicks, String message) {
		events.add(0, new ColonyEvent(ageTicks, message));
		while (events.size() > 10) {
			events.remove(events.size() - 1);
		}
	}

	public void addBuilding(ColonyBuilding building) {
		buildings.add(building);
	}

	public boolean hasCompleted(BuildingType type) {
		return buildings.stream().anyMatch(building -> building.type() == type && building.complete());
	}

	public Optional<ColonyBuilding> firstIncomplete() {
		return buildings.stream().filter(building -> !building.complete()).findFirst();
	}

	public DiplomacyState relationTo(int colonyId) {
		return knownColonies.getOrDefault(Integer.toString(colonyId), DiplomacyState.NEUTRAL);
	}

	public void setRelation(int colonyId, DiplomacyState state) {
		knownColonies.put(Integer.toString(colonyId), state);
	}

	private static int clampReputation(int value) {
		return Math.max(-100, Math.min(100, value));
	}
}
