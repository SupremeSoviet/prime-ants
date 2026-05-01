package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RaidPlan(int attackerId, int defenderId, int ticksRemaining, ResourceType targetResource, int amount) {
	public static final Codec<RaidPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("attackerId").forGetter(RaidPlan::attackerId),
			Codec.INT.fieldOf("defenderId").forGetter(RaidPlan::defenderId),
			Codec.INT.optionalFieldOf("ticksRemaining", 200).forGetter(RaidPlan::ticksRemaining),
			ResourceType.CODEC.optionalFieldOf("targetResource", ResourceType.FOOD).forGetter(RaidPlan::targetResource),
			Codec.INT.optionalFieldOf("amount", 0).forGetter(RaidPlan::amount)
	).apply(instance, RaidPlan::new));

	public RaidPlan tickDown() {
		return new RaidPlan(attackerId, defenderId, Math.max(0, ticksRemaining - 1), targetResource, amount);
	}
}
