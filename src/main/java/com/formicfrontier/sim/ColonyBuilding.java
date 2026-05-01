package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public final class ColonyBuilding {
	public static final Codec<ColonyBuilding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuildingType.CODEC.fieldOf("type").forGetter(ColonyBuilding::type),
			BlockPos.CODEC.fieldOf("pos").forGetter(ColonyBuilding::pos),
			Codec.INT.optionalFieldOf("level", 1).forGetter(ColonyBuilding::level),
			Codec.INT.optionalFieldOf("constructionProgress", 100).forGetter(ColonyBuilding::constructionProgress),
			Codec.INT.optionalFieldOf("disabledTicks", 0).forGetter(ColonyBuilding::disabledTicks)
	).apply(instance, ColonyBuilding::new));

	private final BuildingType type;
	private final BlockPos pos;
	private int level;
	private int constructionProgress;
	private int disabledTicks;

	public ColonyBuilding(BuildingType type, BlockPos pos, int level, int constructionProgress, int disabledTicks) {
		this.type = type;
		this.pos = pos.immutable();
		this.level = Math.max(1, level);
		this.constructionProgress = Math.max(0, Math.min(100, constructionProgress));
		this.disabledTicks = Math.max(0, disabledTicks);
	}

	public static ColonyBuilding complete(BuildingType type, BlockPos pos) {
		return new ColonyBuilding(type, pos, 1, 100, 0);
	}

	public static ColonyBuilding planned(BuildingType type, BlockPos pos) {
		return new ColonyBuilding(type, pos, 1, 0, 0);
	}

	public BuildingType type() {
		return type;
	}

	public BlockPos pos() {
		return pos;
	}

	public int level() {
		return level;
	}

	public int constructionProgress() {
		return constructionProgress;
	}

	public int disabledTicks() {
		return disabledTicks;
	}

	public boolean complete() {
		return constructionProgress >= 100;
	}

	public boolean damaged() {
		return disabledTicks > 0;
	}

	public boolean repairing() {
		return damaged() && !complete();
	}

	public BuildingVisualStage visualStage() {
		return BuildingVisualStage.from(this);
	}

	public void addConstructionProgress(int amount) {
		constructionProgress = Math.max(0, Math.min(100, constructionProgress + amount));
	}

	public void disableFor(int ticks) {
		disabledTicks = Math.max(disabledTicks, ticks);
	}

	public void beginRepair(int startingProgress) {
		if (damaged() && complete()) {
			constructionProgress = Math.max(0, Math.min(99, startingProgress));
		}
	}

	public boolean repair(int amount) {
		if (!damaged()) {
			return false;
		}
		addConstructionProgress(amount);
		if (complete()) {
			disabledTicks = 0;
			return true;
		}
		return false;
	}

	public void tickDisabled() {
		if (disabledTicks > 0) {
			disabledTicks--;
		}
	}

	public void upgrade() {
		level++;
		constructionProgress = 0;
	}
}
