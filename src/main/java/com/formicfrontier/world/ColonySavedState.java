package com.formicfrontier.world;

import com.formicfrontier.FormicFrontier;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyEconomy;
import com.formicfrontier.sim.ColonyLogistics;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ColonySavedState extends SavedData {
	private static final Codec<ColonySavedState> CODEC = ColonyData.CODEC.listOf().xmap(ColonySavedState::new, state -> List.copyOf(state.colonies.values()));
	private static final SavedDataType<ColonySavedState> TYPE = new SavedDataType<>(
			FormicFrontier.MOD_ID + "_colonies",
			ColonySavedState::new,
			CODEC,
			null
	);

	private final Map<Integer, ColonyData> colonies = new LinkedHashMap<>();
	private int nextId = 1;

	public ColonySavedState() {
	}

	public ColonySavedState(List<ColonyData> colonies) {
		for (ColonyData colony : colonies) {
			this.colonies.put(colony.id(), colony);
			nextId = Math.max(nextId, colony.id() + 1);
		}
	}

	public static ColonySavedState get(MinecraftServer server) {
		ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
		if (level == null) {
			return new ColonySavedState();
		}
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public int nextId() {
		return nextId++;
	}

	public void put(ColonyData colony) {
		colonies.put(colony.id(), colony);
		setDirty();
	}

	public Optional<ColonyData> firstColony() {
		return colonies.values().stream().findFirst();
	}

	public Optional<ColonyData> colony(int id) {
		return Optional.ofNullable(colonies.get(id));
	}

	public Optional<ColonyData> nearestColony(BlockPos pos, int maxDistance) {
		double maxDistanceSquared = (double) maxDistance * maxDistance;
		ColonyData best = null;
		double bestDistance = maxDistanceSquared;
		for (ColonyData colony : colonies.values()) {
			double distance = colony.origin().distSqr(pos);
			if (distance <= bestDistance) {
				bestDistance = distance;
				best = colony;
			}
		}
		return Optional.ofNullable(best);
	}

	public Collection<ColonyData> colonies() {
		return List.copyOf(colonies.values());
	}

	public boolean tickEconomy() {
		if (colonies.isEmpty()) {
			return false;
		}
		for (ColonyData colony : colonies.values()) {
			ColonyEconomy.tick(colony);
			ColonyLogistics.tick(colony);
		}
		return true;
	}

	public boolean tickWorld(ServerLevel level) {
		if (colonies.isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (ColonyData colony : colonies.values()) {
			changed |= ColonyBuilder.tick(level, colony);
		}
		changed |= RaidPlanner.tick(level, this);
		return changed;
	}
}
