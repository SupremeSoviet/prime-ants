package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ColonyEvent(int ageTicks, String message) {
	public static final Codec<ColonyEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("ageTicks", 0).forGetter(ColonyEvent::ageTicks),
			Codec.STRING.fieldOf("message").forGetter(ColonyEvent::message)
	).apply(instance, ColonyEvent::new));
}
