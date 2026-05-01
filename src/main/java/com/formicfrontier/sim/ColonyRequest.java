package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ColonyRequest(BuildingType building, ResourceType resource, int needed, int fulfilled, String reason) {
	public static final Codec<ColonyRequest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuildingType.CODEC.fieldOf("building").forGetter(ColonyRequest::building),
			ResourceType.CODEC.fieldOf("resource").forGetter(ColonyRequest::resource),
			Codec.INT.fieldOf("needed").forGetter(ColonyRequest::needed),
			Codec.INT.optionalFieldOf("fulfilled", 0).forGetter(ColonyRequest::fulfilled),
			Codec.STRING.optionalFieldOf("reason", "colony logistics").forGetter(ColonyRequest::reason)
	).apply(instance, ColonyRequest::new));

	public ColonyRequest {
		needed = Math.max(1, needed);
		fulfilled = Math.max(0, Math.min(needed, fulfilled));
	}

	public boolean complete() {
		return fulfilled >= needed;
	}

	public int missing() {
		return Math.max(0, needed - fulfilled);
	}

	public ColonyRequest addFulfilled(int amount) {
		return new ColonyRequest(building, resource, needed, fulfilled + amount, reason);
	}
}
