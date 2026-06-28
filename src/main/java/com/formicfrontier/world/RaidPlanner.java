package com.formicfrontier.world;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.ColonyArmory;
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
		RaidOutcome outcome = resolveCombat(attacker, defender, defensiveAlly);
		applyRaidOutcome(outcome);
		placeRaidTrail(level, attacker.origin(), defender.origin());
		if (defensiveAlly != null) {
			DiplomacyConsequences.placeDefensivePactResponse(level, defensiveAlly, defender, attacker);
		}
		damageDefenderBuilding(level, defender);
		ResourceType resource = outcome.resource();
		int stolen = outcome.stolen();
		attacker.setCurrentTask("Raided colony #" + defender.id() + " for " + stolen + " " + resource.id());
		String vaultProtection = outcome.absorbedQueenDamage() > 0 ? "; Queen Vault absorbed " + outcome.absorbedQueenDamage() + " queen damage" : "";
		String raidTask = defensiveAlly == null
				? "Raid by colony #" + attacker.id() + " stole " + stolen + " " + resource.id()
						+ "; " + outcome.attackerLosses() + " attackers lost, " + outcome.defenderLosses() + " defenders lost"
				: "Defensive pact: colony #" + defensiveAlly.id() + " answered raid by colony #" + attacker.id()
						+ "; " + outcome.attackerLosses() + " attackers lost, " + outcome.defenderLosses() + " defenders lost";
		defender.setCurrentTask(raidTask + vaultProtection);
		attacker.addEvent("Raid hit colony #" + defender.id() + " and stole " + stolen + " " + resource.id() + " (" + outcome.attackerLosses() + " losses)");
		defender.addEvent("Raid from colony #" + attacker.id() + " stole " + stolen + " " + resource.id() + " and dealt " + outcome.queenDamage() + " queen damage; " + outcome.defenderLosses() + " colony casualties");
		if (outcome.defenderLosses() > 0) {
			defender.addEvent("Raid casualties: lost " + outcome.defenderLosses() + " defenders");
		}
		if (outcome.attackerLosses() > 0) {
			attacker.addEvent("Raid casualties: lost " + outcome.attackerLosses() + " attackers");
		}
		if (outcome.relationShifted()) {
			defender.addEvent("Raid escalated relation with colony #" + attacker.id() + " to " + outcome.shiftedRelation().id());
		}
		if (outcome.absorbedQueenDamage() > 0) {
			defender.addEvent("Queen Vault absorbed " + outcome.absorbedQueenDamage() + " raid queen damage");
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

	/**
	 * Content row {@code defense_raid_outcome_changes_state}: resolve a raid's
	 * combat outcome deterministically from colony state. This is the pure,
	 * side-effect-free core of a raid (it does not touch the world). It computes
	 * the resources stolen, the queen damage, the <b>casualties</b> on both sides
	 * (combat castes lost), and the resulting <b>relation shift</b> (a failed
	 * defense escalates the relation toward WAR). {@link #applyRaidOutcome} then
	 * mutates the two colonies to realize those state changes. A JUnit gametest
	 * calls this directly to prove the combat outcome changes colony state.
	 */
	public static RaidOutcome resolveCombat(ColonyData attacker, ColonyData defender, ColonyData defensiveAlly) {
		ResourceType resource = richestResource(defender);
		int attackStrength = militaryStrength(attacker);
		int defenseRating = defenseRating(defender) + defensiveSupportRating(defensiveAlly);
		int stolen = Math.max(4, defender.resource(resource) / 5 - defenseRating / 3);
		stolen = Math.min(stolen, defender.resource(resource));
		int rawQueenDamage = Math.max(0, attackStrength / 30 - defenseRating / 10);
		int queenDamage = queenDamageAfterVault(defender, rawQueenDamage);
		int absorbedQueenDamage = rawQueenDamage - queenDamage;

		// CASUALTIES: combat is resolved by opposing strengths. The side that is
		// outmatched takes losses proportional to how badly it loses the strength
		// contest. Soldiers/majors/giants die in raids; losses are bounded so a
		// raid never wipes a caste below zero and a token raid costs little.
		int attackerCombatPool = attacker.casteCount(AntCaste.SOLDIER)
				+ attacker.casteCount(AntCaste.MAJOR)
				+ attacker.casteCount(AntCaste.GIANT);
		int defenderCombatPool = defender.casteCount(AntCaste.SOLDIER)
				+ defender.casteCount(AntCaste.MAJOR)
				+ defender.casteCount(AntCaste.GIANT);
		int attackerLosses = computeCasualties(defenseRating, attackStrength, attackerCombatPool);
		int defenderLosses = computeCasualties(attackStrength, defenseRating, defenderCombatPool);

		// RELATION SHIFT: a defender that fails to repel the raid (the attacker's
		// strength meets or exceeds the defense) escalates the conflict. The
		// recorded relation worsens toward WAR, which has mechanical consequences
		// (hostile trade gates, sustained raid windows). A defender that holds
		// (defense > attack) does not escalate.
		boolean defenderHeld = defenseRating > attackStrength;
		DiplomacyState priorRelation = defender.progress().relationTo(attacker.id());
		DiplomacyState shiftedRelation = defenderHeld ? priorRelation : priorRelation.worsen();
		boolean relationShifted = shiftedRelation != priorRelation;

		return new RaidOutcome(attacker, defender, defensiveAlly, resource, stolen, queenDamage, absorbedQueenDamage,
				attackerLosses, defenderLosses, relationShifted, shiftedRelation, attackStrength, defenseRating);
	}

	/**
	 * Apply a resolved {@link RaidOutcome} to colony state: move the stolen
	 * resource, apply queen damage, subtract <b>casualties</b> from both sides'
	 * combat castes, and record the <b>relation shift</b>. This is the state
	 * mutation half of the raid; it is what a gametest asserts changes.
	 */
	public static void applyRaidOutcome(RaidOutcome outcome) {
		ColonyData attacker = outcome.attacker();
		ColonyData defender = outcome.defender();
		defender.addResource(outcome.resource(), -outcome.stolen());
		attacker.addResource(outcome.resource(), outcome.stolen());
		defender.setQueenHealth(defender.queenHealth() - outcome.queenDamage());
		applyCasualties(attacker, outcome.attackerLosses());
		applyCasualties(defender, outcome.defenderLosses());
		if (outcome.relationShifted()) {
			defender.progress().setRelation(attacker.id(), outcome.shiftedRelation());
			if (outcome.shiftedRelation() == DiplomacyState.WAR && outcome.defensiveAlly() != null) {
				outcome.defensiveAlly().progress().setRelation(attacker.id(), DiplomacyState.WAR);
			}
		}
	}

	/**
	 * Losses a side takes in a raid. Casualties are driven by the OPPOSING force
	 * breaking through: a side loses combat castes proportional to how much the
	 * attacker's strength exceeds its own defense, so a well-defended colony
	 * takes FEWER losses while an outmatched one takes more. Capped so a single
	 * raid never destroys more than half the pool (a skirmish, not an annihilation).
	 *
	 * @param opposingStrength the attacker's strength bearing on this side
	 * @param ownStrength      this side's own defense/strength
	 * @param combatPool       how many combat castes this side can lose
	 */
	private static int computeCasualties(int opposingStrength, int ownStrength, int combatPool) {
		if (combatPool <= 0) {
			return 0;
		}
		// How badly this side is outmatched. When the side holds (own strength
		// meets or beats the attacker) the breakthrough is small; when it is
		// overwhelmed the breakthrough grows, capped at a full breakthrough.
		double breakthrough = Math.max(0.0, ((double) opposingStrength - (double) ownStrength) / (double) Math.max(1, opposingStrength));
		breakthrough = Math.min(1.0, breakthrough);
		// Translate the breakthrough into dead combat castes. A side that holds
		// loses few/none; an overwhelmed side loses more. A near-total
		// breakthrough (>=0.5) always costs at least one combat ant. Never more
		// than half the pool in a single raid (a skirmish, not an annihilation).
		int losses = (int) Math.round(breakthrough * combatPool / 3.0);
		if (breakthrough >= 0.5 && losses < 1) {
			losses = 1;
		}
		int maxLosses = Math.max(1, combatPool / 2);
		return Math.min(maxLosses, Math.max(0, losses));
	}

	/** Remove {@code losses} combat castes from a colony (soldiers first, then majors, then giants). */
	private static void applyCasualties(ColonyData colony, int losses) {
		int remaining = losses;
		remaining -= removeUpTo(colony, AntCaste.SOLDIER, remaining);
		remaining -= removeUpTo(colony, AntCaste.MAJOR, remaining);
		remaining -= removeUpTo(colony, AntCaste.GIANT, remaining);
	}

	private static int removeUpTo(ColonyData colony, AntCaste caste, int amount) {
		if (amount <= 0) {
			return 0;
		}
		int present = colony.casteCount(caste);
		int removed = Math.min(present, amount);
		if (removed > 0) {
			colony.addCaste(caste, -removed);
		}
		return removed;
	}

	/**
	 * Immutable, deterministic record of a resolved raid's combat outcome. A
	 * gametest asserts that casualties are inflicted and the relation shifts.
	 */
	public static final class RaidOutcome {
		private final ColonyData attacker;
		private final ColonyData defender;
		private final ColonyData defensiveAlly;
		private final ResourceType resource;
		private final int stolen;
		private final int queenDamage;
		private final int absorbedQueenDamage;
		private final int attackerLosses;
		private final int defenderLosses;
		private final boolean relationShifted;
		private final DiplomacyState shiftedRelation;
		private final int attackStrength;
		private final int defenseRating;

		RaidOutcome(ColonyData attacker, ColonyData defender, ColonyData defensiveAlly, ResourceType resource,
				int stolen, int queenDamage, int absorbedQueenDamage, int attackerLosses, int defenderLosses,
				boolean relationShifted, DiplomacyState shiftedRelation, int attackStrength, int defenseRating) {
			this.attacker = attacker;
			this.defender = defender;
			this.defensiveAlly = defensiveAlly;
			this.resource = resource;
			this.stolen = stolen;
			this.queenDamage = queenDamage;
			this.absorbedQueenDamage = absorbedQueenDamage;
			this.attackerLosses = attackerLosses;
			this.defenderLosses = defenderLosses;
			this.relationShifted = relationShifted;
			this.shiftedRelation = shiftedRelation;
			this.attackStrength = attackStrength;
			this.defenseRating = defenseRating;
		}

		public ColonyData attacker() {
			return attacker;
		}

		public ColonyData defender() {
			return defender;
		}

		public ColonyData defensiveAlly() {
			return defensiveAlly;
		}

		public ResourceType resource() {
			return resource;
		}

		public int stolen() {
			return stolen;
		}

		public int queenDamage() {
			return queenDamage;
		}

		public int absorbedQueenDamage() {
			return absorbedQueenDamage;
		}

		public int attackerLosses() {
			return attackerLosses;
		}

		public int defenderLosses() {
			return defenderLosses;
		}

		public boolean relationShifted() {
			return relationShifted;
		}

		public DiplomacyState shiftedRelation() {
			return shiftedRelation;
		}

		public int attackStrength() {
			return attackStrength;
		}

		public int defenseRating() {
			return defenseRating;
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
		// Content row weapon_soldier_combat_stats: armed soldiers fight harder.
		// The base caste contribution is the soldiers' own strength; an equipped
		// mandible saber / venom spear (derived in ColonyArmory) adds real weapon
		// attack on top, so the same garrison hits harder when armed.
		int base = colony.casteCount(AntCaste.SOLDIER) * 4
				+ colony.casteCount(AntCaste.MAJOR) * 8
				+ colony.casteCount(AntCaste.GIANT) * 16;
		return base + ColonyArmory.weaponAttack(colony);
	}

	private static int defenseRating(ColonyData colony) {
		int watchPosts = (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.WATCH_POST && building.complete())
				.count();
		int barracks = (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == BuildingType.BARRACKS && building.complete())
				.count();
		// Content row weapon_soldier_combat_stats: an armored garrison defends
		// better. Chitin / resin-chitin plate (derived in ColonyArmory) adds real
		// armor defense so the same garrison shrugs off more raid damage.
		return watchPosts * 6 + barracks * 4 + militaryStrength(colony) / 3 + ColonyArmory.armorDefense(colony);
	}
}
