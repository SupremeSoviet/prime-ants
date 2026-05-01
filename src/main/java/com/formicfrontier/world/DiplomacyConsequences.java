package com.formicfrontier.world;

import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.AntWorkState;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.DiplomacyAction;
import com.formicfrontier.sim.DiplomacyState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class DiplomacyConsequences {
	private static final int ENDPOINT_BUFFER = 12;
	private static final int WAR_PACT_ROUTE_OFFSET = 14;
	public static final int TRUCE_COOLDOWN_TICKS = 20 * 90;

	private DiplomacyConsequences() {
	}

	public static boolean apply(ServerLevel level, ColonyData source, ColonyData target, DiplomacyAction action, DiplomacyState finalState) {
		if (action == DiplomacyAction.TRIBUTE && finalState == DiplomacyState.ALLY) {
			placeTributePact(level, source, target);
			return true;
		}
		if (action == DiplomacyAction.TRUCE && finalState == DiplomacyState.NEUTRAL) {
			placeTruceLine(level, source, target);
			return true;
		}
		if (action == DiplomacyAction.WAR_PACT && finalState == DiplomacyState.WAR) {
			placeWarPact(level, source, target);
			return true;
		}
		return false;
	}

	public static String sourceEvent(DiplomacyAction action, int targetId) {
		return switch (action) {
			case TRIBUTE -> "Tribute pact route marked toward colony #" + targetId;
			case TRUCE -> "Truce line cooled raid route toward colony #" + targetId;
			case WAR_PACT -> "War pact muster marked toward colony #" + targetId;
			default -> action.label() + " consequence marked toward colony #" + targetId;
		};
	}

	public static String targetEvent(DiplomacyAction action, int sourceId) {
		return switch (action) {
			case TRIBUTE -> "Tribute pact route received from colony #" + sourceId;
			case TRUCE -> "Truce line accepted from colony #" + sourceId;
			case WAR_PACT -> "War pact threat mustered by colony #" + sourceId;
			default -> action.label() + " consequence received from colony #" + sourceId;
		};
	}

	public static String currentTask(DiplomacyAction action, int targetId) {
		return switch (action) {
			case TRIBUTE -> "Diplomacy: tribute pact marked with colony #" + targetId;
			case TRUCE -> "Diplomacy: truce line cooled with colony #" + targetId;
			case WAR_PACT -> "Diplomacy: war pact mustering against colony #" + targetId;
			default -> "Diplomacy: " + action.label() + " marked with colony #" + targetId;
		};
	}

	public static void placeTributePact(ServerLevel level, ColonyData source, ColonyData target) {
		placePactBeacon(level, source.origin(), target.origin());
		placePactBeacon(level, target.origin(), source.origin());
		placePactRoute(level, source, target);
		placeTributeCache(level, source.origin(), target.origin());
		placeTributeCaravan(level, source, target);
	}

	public static void placeTruceLine(ServerLevel level, ColonyData source, ColonyData target) {
		coolDownRaiders(source, target);
		placeTruceSeal(level, source.origin(), target.origin());
		placeTruceSeal(level, target.origin(), source.origin());
		placeTruceRoute(level, source, target);
		placeTruceCache(level, source.origin(), target.origin());
	}

	public static void placeWarPact(ServerLevel level, ColonyData source, ColonyData target) {
		openWarRaidWindow(source, target);
		int dx = Integer.compare(target.origin().getX(), source.origin().getX());
		int dz = Integer.compare(target.origin().getZ(), source.origin().getZ());
		int sideX = sideX(dx, dz);
		int sideZ = sideZ(dx, dz);
		BlockPos sourceMuster = ColonyService.anchorToSurface(level, warPactCamp(source.origin(), dx, dz, sideX, sideZ));
		BlockPos targetMuster = ColonyService.anchorToSurface(level, warPactCamp(target.origin(), -dx, -dz, sideX, sideZ));
		BlockPos midpoint = ColonyService.anchorToSurface(level, warPactMuster(source.origin(), target.origin()));
		placeWarBeacon(level, sourceMuster);
		placeWarBeacon(level, targetMuster);
		placeWarRoute(level, sourceMuster, targetMuster, source, target);
		placeWarMuster(level, midpoint);
		spawnWarPactPatrol(level, source, sourceMuster, target.origin());
	}

	public static BlockPos defensivePactRally(BlockPos ally, BlockPos defender, BlockPos attacker) {
		int raidDx = Integer.compare(attacker.getX(), defender.getX());
		int raidDz = Integer.compare(attacker.getZ(), defender.getZ());
		int allyDx = Integer.compare(ally.getX(), defender.getX());
		int allyDz = Integer.compare(ally.getZ(), defender.getZ());
		return defender.offset(raidDx * 14 + allyDx * 4, 0, raidDz * 14 + allyDz * 8);
	}

	public static void placeDefensivePactResponse(ServerLevel level, ColonyData ally, ColonyData defender, ColonyData attacker) {
		BlockPos rally = ColonyService.anchorToSurface(level, defensivePactRally(ally.origin(), defender.origin(), attacker.origin()));
		placeGuardBeacon(level, rally);
		placeGuardRoute(level, ally.origin(), rally, defender, attacker);
		spawnGuardPatrol(level, ally, rally, attacker.origin());
	}

	public static BlockPos warPactMuster(BlockPos source, BlockPos target) {
		BlockPos midpoint = new BlockPos(
				(source.getX() + target.getX()) / 2,
				source.getY(),
				(source.getZ() + target.getZ()) / 2
		);
		int dx = Integer.compare(target.getX(), source.getX());
		int dz = Integer.compare(target.getZ(), source.getZ());
		return midpoint.offset(sideX(dx, dz) * WAR_PACT_ROUTE_OFFSET, 0, sideZ(dx, dz) * WAR_PACT_ROUTE_OFFSET);
	}

	public static BlockPos tributeCaravanCamp(BlockPos source, BlockPos target) {
		BlockPos midpoint = new BlockPos(
				(source.getX() + target.getX()) / 2,
				source.getY(),
				(source.getZ() + target.getZ()) / 2
		);
		int dx = Integer.compare(target.getX(), source.getX());
		int dz = Integer.compare(target.getZ(), source.getZ());
		return midpoint.offset(sideX(dx, dz) * 18, 0, sideZ(dx, dz) * 18);
	}

	private static void placePactBeacon(ServerLevel level, BlockPos origin, BlockPos other) {
		int dx = Integer.compare(other.getX(), origin.getX());
		int dz = Integer.compare(other.getZ(), origin.getZ());
		BlockPos base = ColonyService.anchorToSurface(level, origin.offset(dx * ENDPOINT_BUFFER, 0, dz * ENDPOINT_BUFFER));
		StructurePlacer.safeSet(level, base, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.above(), Blocks.HONEYCOMB_BLOCK);
		StructurePlacer.safeSet(level, base.above(2), Blocks.CANDLE);
		StructurePlacer.safeSet(level, base.north(), Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, base.south(), Blocks.ROOTED_DIRT);
	}

	private static void placeTruceSeal(ServerLevel level, BlockPos origin, BlockPos other) {
		int dx = Integer.compare(other.getX(), origin.getX());
		int dz = Integer.compare(other.getZ(), origin.getZ());
		BlockPos base = ColonyService.anchorToSurface(level, origin.offset(dx * ENDPOINT_BUFFER, 0, dz * ENDPOINT_BUFFER));
		StructurePlacer.safeSet(level, base, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.above(), Blocks.CHISELED_TUFF);
		StructurePlacer.safeSet(level, base.above(2), Blocks.CANDLE);
		StructurePlacer.safeSet(level, base.north(), Blocks.MOSS_BLOCK);
		StructurePlacer.safeSet(level, base.south(), Blocks.ROOTED_DIRT);
	}

	private static BlockPos warPactCamp(BlockPos origin, int dx, int dz, int sideX, int sideZ) {
		return origin.offset(dx * ENDPOINT_BUFFER + sideX * WAR_PACT_ROUTE_OFFSET, 0, dz * ENDPOINT_BUFFER + sideZ * WAR_PACT_ROUTE_OFFSET);
	}

	private static int sideX(int dx, int dz) {
		return dz;
	}

	private static int sideZ(int dx, int dz) {
		return dx == 0 && dz == 0 ? -1 : -dx;
	}

	private static void placePactRoute(ServerLevel level, ColonyData source, ColonyData target) {
		BlockPos start = source.origin();
		BlockPos end = target.origin();
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 180) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (nearEndpoint(current, start, end)) {
				continue;
			}

			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(source, ground) || nearColonyBuilding(target, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.DIRT_PATH : Blocks.PODZOL);
			if (steps % 9 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 18 == 0 ? Blocks.AMETHYST_BLOCK : Blocks.HONEYCOMB_BLOCK);
			}
		}
	}

	private static void placeTruceRoute(ServerLevel level, ColonyData source, ColonyData target) {
		BlockPos start = source.origin();
		BlockPos end = target.origin();
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 180) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (nearEndpoint(current, start, end)) {
				continue;
			}

			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(source, ground) || nearColonyBuilding(target, ground)) {
				continue;
			}
			if (steps % 3 == 0) {
				StructurePlacer.safeSet(level, ground, Blocks.MOSS_BLOCK);
			} else if (steps % 3 == 1) {
				StructurePlacer.safeSet(level, ground, Blocks.DIRT_PATH);
			}
			if (steps % 10 == 0) {
				StructurePlacer.safeSet(level, ground.above(), Blocks.CHISELED_TUFF);
			}
		}
	}

	private static void placeWarRoute(ServerLevel level, BlockPos start, BlockPos end, ColonyData source, ColonyData target) {
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 180) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (nearEndpoint(current, start, end)) {
				continue;
			}

			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(source, ground) || nearColonyBuilding(target, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.BLACKSTONE : Blocks.RED_TERRACOTTA);
			if (steps % 8 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 16 == 0 ? Blocks.BONE_BLOCK : Blocks.POLISHED_BLACKSTONE);
			}
		}
	}

	private static void placeTributeCache(ServerLevel level, BlockPos first, BlockPos second) {
		BlockPos midpoint = new BlockPos(
				(first.getX() + second.getX()) / 2,
				first.getY(),
				(first.getZ() + second.getZ()) / 2
		);
		BlockPos base = ColonyService.anchorToSurface(level, midpoint);
		StructurePlacer.safeSet(level, base, Blocks.HONEYCOMB_BLOCK);
		StructurePlacer.safeSet(level, base.above(), Blocks.AMETHYST_BLOCK);
		StructurePlacer.safeSet(level, base.above(2), Blocks.CANDLE);
		StructurePlacer.safeSet(level, base.north(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.south(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.east(), Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, base.west(), Blocks.ROOTED_DIRT);
	}

	private static void placeTributeCaravan(ServerLevel level, ColonyData source, ColonyData target) {
		BlockPos camp = ColonyService.anchorToSurface(level, tributeCaravanCamp(source.origin(), target.origin()));
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) + Math.abs(z) <= 3) {
					StructurePlacer.safeSet(level, camp.offset(x, 0, z), Math.abs(x) + Math.abs(z) <= 1 ? Blocks.DIRT_PATH : Blocks.PODZOL);
					StructurePlacer.safeSet(level, camp.offset(x, 1, z), Blocks.AIR);
					StructurePlacer.safeSet(level, camp.offset(x, 2, z), Blocks.AIR);
				}
			}
		}
		StructurePlacer.safeSet(level, camp.above(), Blocks.BARREL);
		StructurePlacer.safeSet(level, camp.offset(-1, 1, 0), Blocks.HAY_BLOCK);
		StructurePlacer.safeSet(level, camp.offset(1, 1, 0), Blocks.HONEYCOMB_BLOCK);
		StructurePlacer.safeSet(level, camp.offset(0, 1, 1), Blocks.AMETHYST_BLOCK);
		spawnTributeCarrier(level, source, camp.offset(-2, 1, -1), target.origin(), AntWorkState.CARRYING_RESIN);
		spawnTributeCarrier(level, target, camp.offset(2, 1, 1), source.origin(), AntWorkState.CARRYING_FUNGUS);
	}

	private static void spawnTributeCarrier(ServerLevel level, ColonyData colony, BlockPos pos, BlockPos lookTarget, AntWorkState state) {
		AntEntity ant = ColonyService.spawnAnt(level, pos, AntCaste.WORKER, colony.id());
		if (ant == null) {
			return;
		}
		ant.setWorkState(state);
		int dx = Integer.compare(lookTarget.getX(), pos.getX());
		int dz = Integer.compare(lookTarget.getZ(), pos.getZ());
		ant.setYRot(yawToward(dx, dz));
		ant.setYHeadRot(yawToward(dx, dz));
	}

	private static void placeTruceCache(ServerLevel level, BlockPos first, BlockPos second) {
		BlockPos midpoint = new BlockPos(
				(first.getX() + second.getX()) / 2,
				first.getY(),
				(first.getZ() + second.getZ()) / 2
		);
		BlockPos base = ColonyService.anchorToSurface(level, midpoint);
		StructurePlacer.safeSet(level, base, Blocks.MOSS_BLOCK);
		StructurePlacer.safeSet(level, base.above(), Blocks.CHISELED_TUFF);
		StructurePlacer.safeSet(level, base.above(2), Blocks.CANDLE);
		StructurePlacer.safeSet(level, base.north(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.south(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.east(), Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, base.west(), Blocks.ROOTED_DIRT);
	}

	private static void placeWarBeacon(ServerLevel level, BlockPos base) {
		StructurePlacer.safeSet(level, base, Blocks.BLACKSTONE);
		StructurePlacer.safeSet(level, base.above(), Blocks.RED_TERRACOTTA);
		StructurePlacer.safeSet(level, base.above(2), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, base.above(3), Blocks.CANDLE);
		StructurePlacer.safeSet(level, base.north(), Blocks.POLISHED_BLACKSTONE);
		StructurePlacer.safeSet(level, base.south(), Blocks.POLISHED_BLACKSTONE);
		StructurePlacer.safeSet(level, base.east(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.west(), Blocks.DIRT_PATH);
	}

	private static void placeWarMuster(ServerLevel level, BlockPos base) {
		StructurePlacer.safeSet(level, base, Blocks.RED_TERRACOTTA);
		StructurePlacer.safeSet(level, base.above(), Blocks.BLACKSTONE);
		StructurePlacer.safeSet(level, base.above(2), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, base.north(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.south(), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, base.east(), Blocks.POLISHED_BLACKSTONE);
		StructurePlacer.safeSet(level, base.west(), Blocks.POLISHED_BLACKSTONE);
	}

	private static void placeGuardBeacon(ServerLevel level, BlockPos rally) {
		StructurePlacer.safeSet(level, rally, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, rally.above(), Blocks.POLISHED_DEEPSLATE);
		StructurePlacer.safeSet(level, rally.above(2), Blocks.HONEYCOMB_BLOCK);
		StructurePlacer.safeSet(level, rally.above(3), Blocks.CANDLE);
		StructurePlacer.safeSet(level, rally.north(), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, rally.south(), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, rally.east(), Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, rally.west(), Blocks.ROOTED_DIRT);
	}

	private static void placeGuardRoute(ServerLevel level, BlockPos allyOrigin, BlockPos rally, ColonyData defender, ColonyData attacker) {
		BlockPos current = allyOrigin;
		int steps = 0;
		while ((current.getX() != rally.getX() || current.getZ() != rally.getZ()) && steps < 140) {
			if (current.getX() != rally.getX()) {
				current = current.offset(Integer.compare(rally.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(rally.getZ(), current.getZ()));
			}
			steps++;
			if (nearEndpoint(current, allyOrigin, rally)) {
				continue;
			}

			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (nearColonyBuilding(defender, ground) || nearColonyBuilding(attacker, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.DIRT_PATH : Blocks.ROOTED_DIRT);
			if (steps % 8 == 0) {
				StructurePlacer.safeSet(level, ground.above(), steps % 16 == 0 ? Blocks.HONEYCOMB_BLOCK : Blocks.POLISHED_DEEPSLATE);
			}
		}
	}

	private static void spawnGuardPatrol(ServerLevel level, ColonyData ally, BlockPos rally, BlockPos attackerOrigin) {
		int dx = Integer.compare(attackerOrigin.getX(), rally.getX());
		int dz = Integer.compare(attackerOrigin.getZ(), rally.getZ());
		BlockPos[] posts = {
				rally.offset(-1, 1, -1),
				rally.offset(0, 1, -2),
				rally.offset(1, 1, -1)
		};
		for (BlockPos post : posts) {
			AntEntity guard = ColonyService.spawnAnt(level, post, AntCaste.SOLDIER, ally.id());
			if (guard != null) {
				guard.setWorkState(AntWorkState.PATROLLING);
				guard.setYRot(yawToward(dx, dz));
				guard.setYHeadRot(yawToward(dx, dz));
			}
		}
	}

	private static void spawnWarPactPatrol(ServerLevel level, ColonyData source, BlockPos muster, BlockPos targetOrigin) {
		int dx = Integer.compare(targetOrigin.getX(), muster.getX());
		int dz = Integer.compare(targetOrigin.getZ(), muster.getZ());
		BlockPos[] posts = {
				muster.offset(-2, 1, 1),
				muster.offset(0, 1, 2),
				muster.offset(2, 1, 1),
				muster.offset(0, 1, -2)
		};
		AntCaste[] castes = {
				AntCaste.SOLDIER,
				AntCaste.SOLDIER,
				AntCaste.MAJOR,
				AntCaste.SOLDIER
		};
		for (int i = 0; i < posts.length; i++) {
			AntEntity guard = ColonyService.spawnAnt(level, posts[i], castes[i], source.id());
			if (guard != null) {
				guard.setWorkState(AntWorkState.PATROLLING);
				guard.setYRot(yawToward(dx, dz));
				guard.setYHeadRot(yawToward(dx, dz));
			}
		}
	}

	private static void coolDownRaiders(ColonyData source, ColonyData target) {
		source.progress().setRaidCooldown(Math.max(source.progress().raidCooldown(), TRUCE_COOLDOWN_TICKS));
		target.progress().setRaidCooldown(Math.max(target.progress().raidCooldown(), TRUCE_COOLDOWN_TICKS));
	}

	private static void openWarRaidWindow(ColonyData source, ColonyData target) {
		source.progress().setRaidCooldown(0);
		target.progress().setRaidCooldown(0);
	}

	private static float yawToward(int dx, int dz) {
		if (dx > 0) {
			return -90.0f;
		}
		if (dx < 0) {
			return 90.0f;
		}
		return dz > 0 ? 0.0f : 180.0f;
	}

	private static boolean nearEndpoint(BlockPos pos, BlockPos first, BlockPos second) {
		return horizontalDistanceSquared(pos, first) <= ENDPOINT_BUFFER * ENDPOINT_BUFFER
				|| horizontalDistanceSquared(pos, second) <= ENDPOINT_BUFFER * ENDPOINT_BUFFER;
	}

	private static boolean nearColonyBuilding(ColonyData colony, BlockPos pos) {
		for (ColonyBuilding building : colony.progress().buildingsView()) {
			if (horizontalDistanceSquared(pos, building.pos()) < 8 * 8) {
				return true;
			}
		}
		return false;
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}
}
