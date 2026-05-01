package com.formicfrontier.world;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

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
					executeRaid(level, colony, target, findDefensiveAlly(target, colony, colonies));
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
		DiplomacyState state = isWild(colony) || isWild(other) || colony.progress().playerAllied() == other.progress().playerAllied()
				? DiplomacyState.NEUTRAL
				: DiplomacyState.RIVAL;
		colony.progress().setRelation(other.id(), state);
	}

	private static boolean isWild(ColonyData colony) {
		return "wild".equals(colony.progress().faction());
	}

	private static ColonyData findRaidTarget(ColonyData colony, List<ColonyData> colonies) {
		return colonies.stream()
				.filter(other -> other.id() != colony.id())
				.filter(other -> colony.progress().relationTo(other.id()).hostile())
				.min(Comparator.comparingDouble(other -> other.origin().distSqr(colony.origin())))
				.orElse(null);
	}

	private static ColonyData findDefensiveAlly(ColonyData defender, ColonyData attacker, List<ColonyData> colonies) {
		return colonies.stream()
				.filter(candidate -> candidate.id() != defender.id() && candidate.id() != attacker.id())
				.filter(candidate -> defender.progress().relationTo(candidate.id()) == DiplomacyState.ALLY)
				.filter(candidate -> candidate.progress().relationTo(defender.id()) == DiplomacyState.ALLY)
				.filter(candidate -> candidate.progress().relationTo(attacker.id()) != DiplomacyState.ALLY)
				.min(Comparator.comparingDouble(candidate -> candidate.origin().distSqr(defender.origin())))
				.orElse(null);
	}

	private static void executeRaid(ServerLevel level, ColonyData attacker, ColonyData defender, ColonyData defensiveAlly) {
		ResourceType resource = richestResource(defender);
		int defenseRating = defenseRating(defender) + defensiveSupportRating(defensiveAlly);
		int stolen = Math.max(4, defender.resource(resource) / 5 - defenseRating / 3);
		stolen = Math.min(stolen, defender.resource(resource));
		defender.addResource(resource, -stolen);
		attacker.addResource(resource, stolen);
		int rawQueenDamage = Math.max(0, militaryStrength(attacker) / 30 - defenseRating / 10);
		int queenDamage = queenDamageAfterVault(defender, rawQueenDamage);
		int absorbedQueenDamage = rawQueenDamage - queenDamage;
		defender.setQueenHealth(defender.queenHealth() - queenDamage);
		placeRaidTrail(level, attacker.origin(), defender.origin());
		if (defensiveAlly != null) {
			DiplomacyConsequences.placeDefensivePactResponse(level, defensiveAlly, defender, attacker);
		}
		damageDefenderBuilding(level, defender);
		attacker.setCurrentTask("Raided colony #" + defender.id() + " for " + stolen + " " + resource.id());
		String vaultProtection = absorbedQueenDamage > 0 ? "; Queen Vault absorbed " + absorbedQueenDamage + " queen damage" : "";
		String raidTask = defensiveAlly == null
				? "Raid by colony #" + attacker.id() + " stole " + stolen + " " + resource.id()
				: "Defensive pact: colony #" + defensiveAlly.id() + " answered raid by colony #" + attacker.id();
		defender.setCurrentTask(raidTask + vaultProtection);
		attacker.addEvent("Raid hit colony #" + defender.id() + " and stole " + stolen + " " + resource.id());
		defender.addEvent("Raid from colony #" + attacker.id() + " stole " + stolen + " " + resource.id() + " and dealt " + queenDamage + " queen damage");
		if (absorbedQueenDamage > 0) {
			defender.addEvent("Queen Vault absorbed " + absorbedQueenDamage + " raid queen damage");
		}
		if (defensiveAlly != null) {
			defender.addEvent("Defensive pact: colony #" + defensiveAlly.id() + " sent guard patrols");
			defensiveAlly.setCurrentTask("Defensive pact: guarding colony #" + defender.id());
			defensiveAlly.addEvent("Defensive pact answered raid against colony #" + defender.id());
		}
		for (int i = 0; i < 3; i++) {
			ColonyService.spawnAnt(level, defender.origin().offset(2 + i, 0, -2), AntCaste.SOLDIER, attacker.id());
		}
	}

	private static int defensiveSupportRating(ColonyData ally) {
		if (ally == null) {
			return 0;
		}
		return 6 + defenseRating(ally) / 2 + militaryStrength(ally) / 3;
	}

	private static void placeRaidTrail(ServerLevel level, BlockPos attacker, BlockPos defender) {
		BlockPos current = attacker;
		int steps = 0;
		while ((current.getX() != defender.getX() || current.getZ() != defender.getZ()) && steps < 160) {
			if (current.getX() != defender.getX()) {
				current = current.offset(Integer.compare(defender.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(defender.getZ(), current.getZ()));
			}
			steps++;
			if (nearEndpoint(current, attacker, defender)) {
				continue;
			}

			BlockPos ground = ColonyService.anchorToSurface(level, current);
			StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT);
			if (steps % 9 == 0) {
				StructurePlacer.safeSet(level, ground.above(), (steps / 9) % 2 == 0 ? Blocks.BLACKSTONE : Blocks.RED_TERRACOTTA);
			}
			if (steps % 11 == 0) {
				BlockPos shoulder = ColonyService.anchorToSurface(level, ground.offset(0, 0, steps % 22 == 0 ? 1 : -1));
				StructurePlacer.safeSet(level, shoulder, Blocks.PODZOL);
			}
		}
	}

	private static boolean nearEndpoint(BlockPos pos, BlockPos attacker, BlockPos defender) {
		return horizontalDistanceSquared(pos, attacker) < 12 * 12 || horizontalDistanceSquared(pos, defender) < 12 * 12;
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static void damageDefenderBuilding(ServerLevel level, ColonyData defender) {
		ColonyBuilding target = defender.progress().buildings().stream()
				.filter(ColonyBuilding::complete)
				.filter(building -> !building.damaged())
				.filter(building -> building.type() != BuildingType.QUEEN_CHAMBER)
				.findFirst()
				.orElse(null);
		if (target == null) {
			return;
		}
		target.disableFor(180);
		StructurePlacer.placeBuilding(level, target.pos(), target.type(), target.visualStage(), defender.progress().culture());
		ColonyLabelService.syncLabels(level, defender);
		defender.addEvent("Raid damaged " + target.type().id());
	}

	private static int queenDamageAfterVault(ColonyData defender, int queenDamage) {
		if (queenDamage <= 0 || !defender.progress().hasCompleted(BuildingType.QUEEN_VAULT)) {
			return queenDamage;
		}
		return Math.max(0, queenDamage - 3);
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
