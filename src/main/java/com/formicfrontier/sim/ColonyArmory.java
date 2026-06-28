package com.formicfrontier.sim;

/**
 * Content row {@code weapon_soldier_combat_stats}: ant-themed weapons and armor
 * must have real combat stats used by soldier castes in raids/defense, not exist
 * only as Minecraft items.
 * <p>
 * The mod already registers ant-themed weapon items
 * ({@code mandible_saber} and {@code venom_spear}, see
 * {@link com.formicfrontier.item.FormicWeaponItem}) and chitin / resin-chitin
 * armor materials (see {@link com.formicfrontier.registry.ModItems}). Without
 * this class those items had <b>zero</b> effect on colony combat:
 * {@code RaidPlanner.militaryStrength} / {@code defenseRating} only counted raw
 * castes and buildings. {@code ColonyArmory} is the bridge: it derives the
 * colony's equipped loadout from colony state and exposes the combat-stat
 * contributions (soldier weapon attack + armor defense) that the raid resolver
 * applies.
 *
 * <h2>Loadout derivation</h2>
 * <ul>
 *   <li>A completed {@link BuildingType#ARMORY} is the forge that can equip any
 *       weapon at all. Each completed ARMORY can arm a limited number of soldiers
 *       per pass, so a huge army is not fully armed from a single armory.</li>
 *   <li>{@link ResearchNode#MANDIBLE_PLATING} unlocks the <b>mandible saber</b>
 *       (the strongest colony melee weapon). Its per-soldier attack contribution
 *       mirrors the item's {@code bonusDamage} (5.0).</li>
 *   <li>{@link ResearchNode#VENOM_DRILLS} unlocks the <b>venom spear</b>
 *       (per-soldier attack 4, mirroring the item's {@code bonusDamage}). When
 *       only venom is available, soldiers carry the spear; when both are
 *       available, soldiers carry the stronger mandible saber.</li>
 *   <li>Chitin armor defense is available once the colony has a completed
 *       {@link BuildingType#CHITIN_FARM} (a chitin supply). {@link ResearchNode#RESIN_MASONRY}
 *       upgrades the garrison to resin-chitin plate, which defends better
 *       (mirrors the {@code RESIN_CHITIN_ARMOR_MATERIAL} toughness bump).</li>
 * </ul>
 *
 * <p>The class is deterministic and side-effect free (it only reads
 * {@link ColonyData}), so a JUnit gametest can build two colonies that differ
 * only in armament and assert that the resolved combat outcome differs.
 */
public final class ColonyArmory {
	/** Soldiers (incl. majors/giants) one completed ARMORY can equip per pass. */
	public static final int ARMORY_ARMS_PER_BUILDING = 4;

	/** Per-armed-soldier attack added by the mandible saber (mirrors item bonusDamage 5.0). */
	static final int MANDIBLE_SABER_ATTACK = 5;
	/** Per-armed-soldier attack added by the venom spear (mirrors item bonusDamage 4.0). */
	static final int VENOM_SPEAR_ATTACK = 4;

	/** Base chitin-armor defense added per armed soldier when chitin supply exists. */
	static final int CHITIN_ARMOR_DEFENSE = 2;
	/** Extra resin-chitin plate defense added per armed soldier after RESIN_MASONRY. */
	static final int RESIN_CHITIN_DEFENSE_BONUS = 2;

	private ColonyArmory() {
	}

	/**
	 * Compute the colony's equipped loadout. Pure read of colony state; the raid
	 * resolver adds {@link Loadout#weaponAttack()} to the attacker's military
	 * strength and {@link Loadout#armorDefense()} to a defender's defense rating.
	 */
	public static Loadout loadout(ColonyData colony) {
		Loadout loadout = new Loadout();
		if (colony == null) {
			return loadout;
		}
		int armories = completed(colony, BuildingType.ARMORY);
		loadout.armories = armories;
		if (armories <= 0) {
			// No armory => no forging/equipping at all; the colony fights with bare
			// mandibles only, exactly as before this row existed.
			return loadout;
		}
		int combatCastes = colony.casteCount(AntCaste.SOLDIER)
				+ colony.casteCount(AntCaste.MAJOR)
				+ colony.casteCount(AntCaste.GIANT);
		// Throughput: each armory equips a bounded number of soldiers per pass.
		int armed = Math.min(combatCastes, armories * ARMORY_ARMS_PER_BUILDING);
		loadout.armedSoldiers = armed;
		if (armed <= 0) {
			return loadout;
		}

		// Weapon: prefer the strongest available ant-themed weapon per soldier.
		boolean hasMandible = colony.progress().hasResearch(ResearchNode.MANDIBLE_PLATING.id());
		boolean hasVenom = colony.progress().hasResearch(ResearchNode.VENOM_DRILLS.id());
		if (hasMandible) {
			loadout.weapon = "mandible_saber";
			loadout.weaponAttack = armed * MANDIBLE_SABER_ATTACK;
		} else if (hasVenom) {
			loadout.weapon = "venom_spear";
			loadout.weaponAttack = armed * VENOM_SPEAR_ATTACK;
		}
		loadout.mandibleUnlocked = hasMandible;
		loadout.venomUnlocked = hasVenom;

		// Armor: chitin plate once the colony has a chitin supply; resin-chitin
		// (better) after RESIN_MASONRY research. Armor defends the armed garrison.
		if (completed(colony, BuildingType.CHITIN_FARM) > 0) {
			int perSoldier = CHITIN_ARMOR_DEFENSE + (colony.progress().hasResearch(ResearchNode.RESIN_MASONRY.id()) ? RESIN_CHITIN_DEFENSE_BONUS : 0);
			loadout.armorDefense = armed * perSoldier;
			loadout.resinChitin = perSoldier > CHITIN_ARMOR_DEFENSE;
		}
		return loadout;
	}

	/** Convenience: the extra attack armed soldiers contribute to military strength. */
	public static int weaponAttack(ColonyData colony) {
		return loadout(colony).weaponAttack();
	}

	/** Convenience: the extra defense equipped armor contributes to the defense rating. */
	public static int armorDefense(ColonyData colony) {
		return loadout(colony).armorDefense();
	}

	private static int completed(ColonyData colony, BuildingType type) {
		return (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == type && building.complete())
				.count();
	}

	/**
	 * Immutable snapshot of the colony's equipped combat loadout. Asserted by the
	 * weapon_soldier_combat_stats gametest.
	 */
	public static final class Loadout {
		private int armories;
		private int armedSoldiers;
		private int weaponAttack;
		private int armorDefense;
		private boolean mandibleUnlocked;
		private boolean venomUnlocked;
		private boolean resinChitin;
		private String weapon = "bare_mandibles";

		public int armories() {
			return armories;
		}

		public int armedSoldiers() {
			return armedSoldiers;
		}

		public int weaponAttack() {
			return weaponAttack;
		}

		public int armorDefense() {
			return armorDefense;
		}

		public boolean mandibleUnlocked() {
			return mandibleUnlocked;
		}

		public boolean venomUnlocked() {
			return venomUnlocked;
		}

		public boolean resinChitin() {
			return resinChitin;
		}

		public String weapon() {
			return weapon;
		}

		public boolean isArmed() {
			return weaponAttack > 0;
		}
	}
}
