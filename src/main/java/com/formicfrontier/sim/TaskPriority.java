package com.formicfrontier.sim;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum TaskPriority {
	FOOD("food"),
	ORE("ore"),
	CHITIN("chitin"),
	DEFENSE("defense");

	public static final Codec<TaskPriority> CODEC = Codec.STRING.xmap(TaskPriority::fromId, TaskPriority::id);

	private final String id;

	TaskPriority(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static TaskPriority fromId(String id) {
		String normalized = id.toLowerCase(Locale.ROOT);
		for (TaskPriority priority : values()) {
			if (priority.id.equals(normalized)) {
				return priority;
			}
		}
		throw new IllegalArgumentException("Unknown task priority: " + id);
	}
}
