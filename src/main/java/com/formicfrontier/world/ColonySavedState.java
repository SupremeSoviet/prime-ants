package com.formicfrontier.world;

import com.formicfrontier.FormicFrontier;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.DiplomacyService;
import com.formicfrontier.sim.ColonyEconomy;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.NativeBlockRole;
import com.formicfrontier.sim.CasteJobLoop;
import com.formicfrontier.sim.ColonyStageProgression;
import com.formicfrontier.sim.TradeCaravan;
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

	public void clearColonies() {
		colonies.clear();
		nextId = 1;
		setDirty();
	}

	public boolean tickEconomy() {
		if (colonies.isEmpty()) {
			return false;
		}
		for (ColonyData colony : colonies.values()) {
			ColonyEconomy.tick(colony);
			ColonyLogistics.tick(colony);
			CasteJobLoop.tick(colony);
			ColonyStageProgression.tick(colony);
			// Content row block_native_gameplay_roles: native blocks have active
			// gameplay roles in the colony tick, not decoration. The FUNGUS_GARDEN
			// block composts stored FOOD into FUNGUS each pass, gated by a food
			// reserve so it cannot starve the colony.
			NativeBlockRole.tick(colony);
		}
		// Content row politics_relations_shift_from_actions: run a recurring
		// autonomous diplomacy pass so relations move without the player. A colony
		// with a completed DIPLOMACY_SHRINE that has reached BURROW rank sends an
		// ENVOY toward its nearest non-allied known colony, recorded via
		// DiplomacyService.perform; the caravan pass below then trades at the rate
		// implied by the (possibly improved) relation.
		for (ColonyData envoy : List.copyOf(colonies.values())) {
			ColonyData target = nearestKnownNonAlly(envoy);
			if (target != null) {
				DiplomacyService.tick(envoy, target);
			}
		}
		// Content row trade_caravan_exchanges_resources: run a colony-to-colony
		// caravan pass so caravans reach normal play. Each colony with a MARKET
		// may send one scarcity-driven, relation-rated cargo to the nearest other
		// known colony it is not hostile toward.
		for (ColonyData source : List.copyOf(colonies.values())) {
			ColonyData partner = nearestKnownPartner(source);
			if (partner != null) {
				TradeCaravan.exchange(source, partner);
			}
		}
		return true;
	}

	private ColonyData nearestKnownPartner(ColonyData source) {
		ColonyData nearest = null;
		double bestSq = Double.MAX_VALUE;
		for (ColonyData other : colonies.values()) {
			if (other.id() == source.id()) {
				continue;
			}
			if (source.progress().relationTo(other.id()).hostile()) {
				continue;
			}
			double dsq = source.origin().distSqr(other.origin());
			if (dsq < bestSq) {
				bestSq = dsq;
				nearest = other;
			}
		}
		return nearest;
	}

	private ColonyData nearestKnownNonAlly(ColonyData source) {
		ColonyData nearest = null;
		double bestSq = Double.MAX_VALUE;
		for (ColonyData other : colonies.values()) {
			if (other.id() == source.id()) {
				continue;
			}
			if (source.progress().relationTo(other.id()) == com.formicfrontier.sim.DiplomacyState.ALLY) {
				continue;
			}
			double dsq = source.origin().distSqr(other.origin());
			if (dsq < bestSq) {
				bestSq = dsq;
				nearest = other;
			}
		}
		return nearest;
	}

	public boolean tickWorld(ServerLevel level) {
		boolean changed = ColonyDiscoveryService.tick(level, this);
		if (colonies.isEmpty()) {
			return changed;
		}
		for (ColonyData colony : List.copyOf(colonies.values())) {
			changed |= ColonyBuilder.tick(level, colony);
			changed |= ColonyRecurringEvents.tick(level, colony);
		}
		changed |= RaidPlanner.tick(level, this);
		return changed;
	}
}