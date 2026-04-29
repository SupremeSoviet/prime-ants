package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ResearchState(String nodeId, int progressTicks) {
	public static final Codec<ResearchState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("nodeId").forGetter(ResearchState::nodeId),
			Codec.INT.optionalFieldOf("progressTicks", 0).forGetter(ResearchState::progressTicks)
	).apply(instance, ResearchState::new));

	public ResearchState {
		progressTicks = Math.max(0, progressTicks);
	}

	public ResearchState advance(int ticks) {
		return new ResearchState(nodeId, progressTicks + Math.max(0, ticks));
	}
}
