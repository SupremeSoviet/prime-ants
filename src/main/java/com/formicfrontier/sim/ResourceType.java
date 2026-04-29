package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum ResourceType {
	FOOD("food"),
	ORE("ore"),
	CHITIN("chitin"),
	RESIN("resin"),
	FUNGUS("fungus"),
	VENOM("venom"),
	KNOWLEDGE("knowledge");

	public static final Codec<ResourceType> CODEC = Codec.STRING.xmap(ResourceType::fromId, ResourceType::id);

	private final String id;

	ResourceType(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static ResourceType fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (ResourceType type : values()) {
			if (type.id.equals(normalized)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown resource type: " + id);
	}
}
