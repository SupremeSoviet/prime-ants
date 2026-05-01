package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public final class ColonyData {
	public static final Codec<ColonyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("id").forGetter(ColonyData::id),
			BlockPos.CODEC.fieldOf("origin").forGetter(ColonyData::origin),
			Codec.unboundedMap(ResourceType.CODEC, Codec.INT).fieldOf("resources").forGetter(ColonyData::resourcesView),
			Codec.unboundedMap(AntCaste.CODEC, Codec.INT).fieldOf("castes").forGetter(ColonyData::castesView),
			NestChamber.CODEC.listOf().fieldOf("chambers").forGetter(ColonyData::chambersView),
			TaskPriority.CODEC.listOf().fieldOf("priorities").forGetter(ColonyData::prioritiesView),
			Codec.INT.fieldOf("queenHealth").forGetter(ColonyData::queenHealth),
			Codec.INT.fieldOf("ageTicks").forGetter(ColonyData::ageTicks),
			Codec.STRING.optionalFieldOf("currentTask").forGetter(data -> Optional.ofNullable(data.currentTask)),
			ColonyProgress.CODEC.optionalFieldOf("progress").forGetter(data -> Optional.ofNullable(data.progress))
	).apply(instance, ColonyData::fromCodec));

	private final int id;
	private final BlockPos origin;
	private final EnumMap<ResourceType, Integer> resources = new EnumMap<>(ResourceType.class);
	private final EnumMap<AntCaste, Integer> castes = new EnumMap<>(AntCaste.class);
	private final List<NestChamber> chambers = new ArrayList<>();
	private final List<TaskPriority> priorities = new ArrayList<>();
	private int queenHealth;
	private int ageTicks;
	private String currentTask = "Establishing nest";
	private ColonyProgress progress;

	public ColonyData(int id, BlockPos origin) {
		this.id = id;
		this.origin = origin.immutable();
		for (ResourceType type : ResourceType.values()) {
			resources.put(type, 0);
		}
		for (AntCaste caste : AntCaste.values()) {
			castes.put(caste, 0);
		}
		priorities.add(TaskPriority.FOOD);
		priorities.add(TaskPriority.ORE);
		priorities.add(TaskPriority.CHITIN);
		priorities.add(TaskPriority.DEFENSE);
		queenHealth = (int) AntCaste.QUEEN.health();
		progress = ColonyProgress.allied(id);
	}

	private static ColonyData fromCodec(int id, BlockPos origin, Map<ResourceType, Integer> resources, Map<AntCaste, Integer> castes, List<NestChamber> chambers, List<TaskPriority> priorities, int queenHealth, int ageTicks, Optional<String> currentTask, Optional<ColonyProgress> progress) {
		ColonyData data = new ColonyData(id, origin);
		data.resources.putAll(resources);
		data.castes.putAll(castes);
		data.chambers.addAll(chambers);
		data.priorities.clear();
		data.priorities.addAll(priorities);
		data.queenHealth = queenHealth;
		data.ageTicks = ageTicks;
		data.currentTask = currentTask.orElse("Idle");
		data.progress = progress.orElseGet(() -> ColonyProgress.allied(id));
		return data;
	}

	public int id() {
		return id;
	}

	public BlockPos origin() {
		return origin;
	}

	public int resource(ResourceType type) {
		return resources.getOrDefault(type, 0);
	}

	public void setResource(ResourceType type, int amount) {
		resources.put(type, Math.max(0, amount));
	}

	public void addResource(ResourceType type, int delta) {
		setResource(type, resource(type) + delta);
	}

	public int casteCount(AntCaste caste) {
		return castes.getOrDefault(caste, 0);
	}

	public void addCaste(AntCaste caste, int delta) {
		castes.put(caste, Math.max(0, casteCount(caste) + delta));
	}

	public int population() {
		int total = 0;
		for (int count : castes.values()) {
			total += count;
		}
		return total;
	}

	public int upkeepPerEconomyTick() {
		int total = 0;
		for (AntCaste caste : AntCaste.values()) {
			total += caste.foodUpkeep() * casteCount(caste);
		}
		return total;
	}

	public boolean queenAlive() {
		return queenHealth > 0 && casteCount(AntCaste.QUEEN) > 0;
	}

	public int queenHealth() {
		return queenHealth;
	}

	public void setQueenHealth(int queenHealth) {
		this.queenHealth = Math.max(0, queenHealth);
	}

	public int ageTicks() {
		return ageTicks;
	}

	public void addAgeTicks(int ticks) {
		ageTicks += ticks;
	}

	public String currentTask() {
		return currentTask;
	}

	public void setCurrentTask(String currentTask) {
		this.currentTask = currentTask;
	}

	public void addEvent(String message) {
		progress.addEvent(ageTicks, message);
	}

	public ColonyProgress progress() {
		return progress;
	}

	public void setProgress(ColonyProgress progress) {
		this.progress = progress;
	}

	public void addChamber(NestChamber chamber) {
		chambers.add(chamber);
	}

	public void clearChambers() {
		chambers.clear();
	}

	public List<NestChamber> chambersView() {
		return List.copyOf(chambers);
	}

	public List<TaskPriority> prioritiesView() {
		return List.copyOf(priorities);
	}

	public void setPriorities(List<TaskPriority> nextPriorities) {
		priorities.clear();
		priorities.addAll(nextPriorities);
	}

	public Map<ResourceType, Integer> resourcesView() {
		return Map.copyOf(resources);
	}

	public Map<AntCaste, Integer> castesView() {
		return Map.copyOf(castes);
	}

	public String statusText() {
		StringJoiner joiner = new StringJoiner("\n");
		joiner.add("Colony #" + id + " at " + origin.toShortString());
		joiner.add("Culture: " + progress.culture().displayName() + " (" + progress.culture().id() + ")");
		joiner.add("Resources: " + resourcesSummary());
		joiner.add("Queen: " + queenHealth + " hp, alive=" + queenAlive());
		joiner.add("Population: " + population() + " | upkeep/tick=" + upkeepPerEconomyTick());
		joiner.add("Faction: " + progress.name() + " | rank=" + ColonyRank.current(this).displayName() + " (" + ColonyRank.score(this) + ") | rep=" + progress.reputation() + " | claim=" + progress.claimRadius());
		joiner.add("Castes: " + castesSummary());
		joiner.add("Buildings: " + buildingsSummary());
		joiner.add("Build Queue: " + progress.buildQueueView());
		joiner.add("Relations: " + progress.knownColoniesView());
		joiner.add("Priorities: " + priorities);
		progress.eventsView().stream().findFirst().ifPresent(event -> joiner.add("Latest: " + event.message()));
		joiner.add("Task: " + currentTask);
		return joiner.toString();
	}

	private String resourcesSummary() {
		StringJoiner joiner = new StringJoiner(" ");
		for (ResourceType type : ResourceType.values()) {
			joiner.add(type.id() + "=" + resource(type));
		}
		return joiner.toString();
	}

	private String castesSummary() {
		StringJoiner joiner = new StringJoiner(", ");
		for (AntCaste caste : AntCaste.values()) {
			joiner.add(caste.id() + "=" + casteCount(caste));
		}
		return joiner.toString();
	}

	private String buildingsSummary() {
		StringJoiner joiner = new StringJoiner(", ");
		for (BuildingType type : BuildingType.values()) {
			long complete = progress.buildingsView().stream().filter(building -> building.type() == type && building.complete()).count();
			if (complete > 0) {
				joiner.add(type.id() + "=" + complete);
			}
		}
		return joiner.length() == 0 ? "none" : joiner.toString();
	}
}
