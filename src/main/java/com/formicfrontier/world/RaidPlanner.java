package com.formicfrontier.world;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ResourceType;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RaidPlanner {
	private RaidPlanner() {
	}

	public static boolean tick(ServerLevel level, ColonySavedState savedState) {
		List<ColonyData> colonies = new ArrayList<>(savedState.colonies());
		if (colonies.size() < 2) {
			return false;
		}
		boolean changed = false;
		for (ColonyData colony : colonies) {
			colony.progress().tickRaidCooldown();
			for (ColonyData other : colonies) {
				if (colony.id() == other.id()) {
					continue;
				}
				ensureRelation(colony, other);
			}
			if (colony.progress().raidCooldown() == 0) {
				ColonyData target = findRaidTarget(colony, colonies);
				if (target != null && militaryStrength(colony) >= 16) {
					executeRaid(level, colony, target);
					colony.progress().setRaidCooldown(600);
					changed = true;
				}
			}
		}
		return changed;
	}

	public static boolean areHostile(ServerLevel level, int firstId, int secondId) {
		if (firstId <= 0 || secondId <= 0 || firstId == secondId) {
			return false;
		}
		ColonySavedState state = ColonySavedState.get(level.getServer());
		ColonyData first = state.colony(firstId).orElse(null);
		ColonyData second = state.colony(secondId).orElse(null);
		if (first == null || second == null) {
			return false;
		}
		return first.progress().relationTo(secondId).hostile() || second.progress().relationTo(firstId).hostile();
	}

	private static void ensureRelation(ColonyData colony, ColonyData other) {
		if (colony.progress().knownColoniesView().containsKey(Integer.toString(other.id()))) {
			return;
		}
		DiplomacyState state = colony.progress().playerAllied() == other.progress().playerAllied()
				? DiplomacyState.NEUTRAL
				: DiplomacyState.RIVAL;
		colony.progress().setRelation(other.id(), state);
	}

	private static ColonyData findRaidTarget(ColonyData colony, List<ColonyData> colonies) {
		return colonies.stream()
				.filter(other -> other.id() != colony.id())
				.filter(other -> colony.progress().relationTo(other.id()).hostile())
				.min(Comparator.comparingDouble(other -> other.origin().distSqr(colony.origin())))
				.orElse(null);
	}

	private static void executeRaid(ServerLevel level, ColonyData attacker, ColonyData defender) {
		ResourceType resource = richestResource(defender);
		int defenseRating = defenseRating(defender);
		int stolen = Math.max(4, defender.resource(resource) / 5 - defenseRating / 3);
		stolen = Math.min(stolen, defender.resource(resource));
		defender.addResource(resource, -stolen);
		attacker.addResource(resource, stolen);
		int queenDamage = Math.max(0, militaryStrength(attacker) / 30 - defenseRating / 10);
		defender.setQueenHealth(defender.queenHealth() - queenDamage);
		attacker.setCurrentTask("Raided colony #" + defender.id() + " for " + stolen + " " + resource.id());
		defender.setCurrentTask("Raid by colony #" + attacker.id() + " stole " + stolen + " " + resource.id());
		attacker.addEvent("Raid hit colony #" + defender.id() + " and stole " + stolen + " " + resource.id());
		defender.addEvent("Raid from colony #" + attacker.id() + " stole " + stolen + " " + resource.id() + " and dealt " + queenDamage + " queen damage");
		for (int i = 0; i < 3; i++) {
			ColonyService.spawnAnt(level, defender.origin().offset(2 + i, 0, -2), AntCaste.SOLDIER, attacker.id());
		}
	}

	private static ResourceType richestResource(ColonyData colony) {
		ResourceType richest = ResourceType.FOOD;
		for (ResourceType type : ResourceType.values()) {
			if (colony.resource(type) > colony.resource(richest)) {
				richest = type;
			}
		}
		return richest;
	}

	private static int militaryStrength(ColonyData colony) {
		return colony.casteCount(AntCaste.SOLDIER) * 4
				+ colony.casteCount(AntCaste.MAJOR) * 8
				+ colony.casteCount(AntCaste.GIANT) * 16;
	}

	private static int defenseRating(ColonyData colony) {
		int watchPosts = (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.WATCH_POST && building.complete())
				.count();
		int barracks = (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.BARRACKS && building.complete())
				.count();
		return watchPosts * 6 + barracks * 4 + militaryStrength(colony) / 3;
	}
}
