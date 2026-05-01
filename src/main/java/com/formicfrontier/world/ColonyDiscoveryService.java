package com.formicfrontier.world;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.sim.ColonyData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public final class ColonyDiscoveryService {
	public static final int DISCOVERY_CHECK_INTERVAL_TICKS = 20 * 45;
	public static final int DISCOVERY_DISTANCE_MIN = 56;
	public static final int DISCOVERY_DISTANCE_RANGE = 40;
	public static final int PLAYER_CLEAR_RADIUS = 144;
	public static final int SITE_CLEAR_RADIUS = 96;
	private static final int MAX_DISCOVERED_COLONIES = 6;
	private static final int ATTEMPTS = 12;
	private static final int REGION_SIZE = 128;

	private ColonyDiscoveryService() {
	}

	public static boolean tick(ServerLevel level, ColonySavedState savedState) {
		if (Boolean.getBoolean("formic.visualQa") || level.getGameTime() % DISCOVERY_CHECK_INTERVAL_TICKS != 0) {
			return false;
		}
		boolean changed = false;
		for (ServerPlayer player : level.players()) {
			if (shouldSkip(player) || savedState.colonies().size() >= MAX_DISCOVERED_COLONIES) {
				continue;
			}
			changed |= spawnEncounter(level, savedState, player.blockPosition(), playerRegionSeed(player)).isPresent();
		}
		return changed;
	}

	public static Optional<ColonyData> spawnEncounter(ServerLevel level, ColonySavedState savedState, BlockPos playerPos, long seed) {
		return findEncounterSite(level, savedState, playerPos, seed)
				.map(site -> {
					ColonyData colony = spawnEncounterAt(level, savedState, site, cultureFor(seed, site), "Discovered wild colony near " + playerPos.toShortString());
					placeApproachTrail(level, playerPos, colony);
					colony.addEvent("Surface trail points back toward nearby foragers");
					return colony;
				});
	}

	public static ColonyData spawnEncounterAt(ServerLevel level, ColonySavedState savedState, BlockPos site, ColonyCulture culture, String event) {
		ColonyData colony = ColonyService.createWildColony(level, site, culture);
		colony.setCurrentTask("Watching nearby trails");
		placeSurfaceLandmarks(level, colony);
		colony.addEvent(event);
		colony.addEvent("Surface landmarks mark foraging grounds");
		colony.addEvent("Collapsed scout nest marks an old surface claim");
		savedState.setDirty();
		return colony;
	}

	public static void placeSurfaceLandmarks(ServerLevel level, ColonyData colony) {
		BlockPos origin = colony.origin();
		ColonyCulture culture = colony.progress().culture();
		BlockPos northForage = origin.offset(0, 0, -58);
		BlockPos northBend = origin.offset(-12, 0, -18);
		BlockPos northGate = origin.offset(-12, 0, -48);
		BlockPos westForage = origin.offset(-48, 0, -34);
		BlockPos eastBorder = origin.offset(54, 0, -16);
		BlockPos ruinedScoutNest = origin.offset(38, 0, 22);

		placeLandmarkTrail(level, origin.offset(0, 0, -15), northBend, culture);
		placeLandmarkTrail(level, northBend, northGate, culture);
		placeLandmarkTrail(level, northGate, northForage, culture);
		placeLandmarkTrail(level, origin.offset(-12, 0, -12), westForage, culture);
		placeLandmarkTrail(level, origin.offset(12, 0, 10), ruinedScoutNest, culture);
		placeForagingPatch(level, northForage, culture);
		placeForagingPatch(level, westForage, culture);
		placeBoundaryMarker(level, eastBorder, culture);
		placeBoundaryMarker(level, origin.offset(-58, 0, 4), culture);
		placeRuinedScoutNest(level, ruinedScoutNest, culture);
	}

	public static Optional<BlockPos> findEncounterSite(ServerLevel level, ColonySavedState savedState, BlockPos playerPos, long seed) {
		if (savedState.nearestColony(playerPos, PLAYER_CLEAR_RADIUS).isPresent()) {
			return Optional.empty();
		}
		for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
			long mixed = mix(seed + attempt * 0x9E3779B97F4A7C15L);
			int distance = DISCOVERY_DISTANCE_MIN + positiveInt(mixed, DISCOVERY_DISTANCE_RANGE + 1);
			double angle = Math.PI * 2.0 * positiveInt(mixed >>> 11, 360) / 360.0;
			int dx = (int) Math.round(Math.cos(angle) * distance);
			int dz = (int) Math.round(Math.sin(angle) * distance);
			BlockPos candidate = ColonyService.anchorToSurface(level, playerPos.offset(dx, 0, dz));
			if (isViableSite(level, candidate) && savedState.nearestColony(candidate, SITE_CLEAR_RADIUS).isEmpty()) {
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}

	private static boolean shouldSkip(ServerPlayer player) {
		return player.isCreative() || player.isSpectator();
	}

	private static boolean isViableSite(ServerLevel level, BlockPos pos) {
		if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinY() + 2) {
			return false;
		}
		if (!isClearSurface(level, pos)) {
			return false;
		}
		for (BlockPos sample : new BlockPos[] {
				pos.offset(8, 0, 0),
				pos.offset(-8, 0, 0),
				pos.offset(0, 0, 8),
				pos.offset(0, 0, -8)
		}) {
			BlockPos surface = ColonyService.anchorToSurface(level, sample);
			if (Math.abs(surface.getY() - pos.getY()) > 2 || !isClearSurface(level, surface)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isClearSurface(ServerLevel level, BlockPos pos) {
		var ground = level.getBlockState(pos);
		return !ground.isAir()
				&& !ground.is(Blocks.WATER)
				&& !ground.is(Blocks.LAVA)
				&& level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos.above(2)).isAir();
	}

	private static void placeApproachTrail(ServerLevel level, BlockPos playerPos, ColonyData colony) {
		BlockPos start = ColonyService.anchorToSurface(level, playerPos);
		BlockPos end = colony.origin();
		BlockPos current = start;
		BlockPos trailHead = null;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 160) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, start) < 8 * 8 || horizontalDistanceSquared(current, end) < 13 * 13) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (!isTrailSurface(level, ground)) {
				continue;
			}
			if (StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT) && trailHead == null) {
				trailHead = ground;
			}
			if (steps % 11 == 0 && horizontalDistanceSquared(current, start) >= 15 * 15) {
				StructurePlacer.safeSet(level, ground.above(), trailMarker(colony.progress().culture()));
			}
		}
		if (trailHead != null) {
			placeApproachTrailHead(level, trailHead, end, colony.progress().culture());
		}
	}

	private static void placeApproachTrailHead(ServerLevel level, BlockPos trailHead, BlockPos end, ColonyCulture culture) {
		int stepX = Integer.compare(end.getX(), trailHead.getX());
		int stepZ = Integer.compare(end.getZ(), trailHead.getZ());
		BlockPos forward = trailHeadSurface(level, trailHead.offset(stepX, 0, stepZ));
		BlockPos back = trailHeadSurface(level, trailHead.offset(-stepX, 0, -stepZ));
		BlockPos left = trailHeadSurface(level, trailHead.offset(-stepZ, 0, stepX));
		BlockPos right = trailHeadSurface(level, trailHead.offset(stepZ, 0, -stepX));

		StructurePlacer.safeSet(level, back, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, trailHead, Blocks.COARSE_DIRT);
		StructurePlacer.safeSet(level, forward, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, left, Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, right, trailHeadDressing(culture));
		for (BlockPos pos : new BlockPos[] {back, trailHead, forward, left, right}) {
			StructurePlacer.safeSet(level, pos.above(), Blocks.AIR);
		}
	}

	private static BlockPos trailHeadSurface(ServerLevel level, BlockPos pos) {
		return ColonyService.anchorToSurface(level, pos);
	}

	private static void placeLandmarkTrail(ServerLevel level, BlockPos start, BlockPos end, ColonyCulture culture) {
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 72) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (!isTrailSurface(level, ground)) {
				continue;
			}
			StructurePlacer.safeSet(level, ground, steps % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
			if (steps % 8 == 0) {
				StructurePlacer.safeSet(level, ground.above(), trailMarker(culture));
			}
		}
	}

	private static void placeForagingPatch(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance > 4) {
					continue;
				}
				BlockPos ground = ColonyService.anchorToSurface(level, center.offset(x, 0, z));
				if (!isTrailSurface(level, ground)) {
					continue;
				}
				Block groundBlock = x == 0 && z == 0 ? ModBlocks.FOOD_NODE : forageGround(culture, distance);
				StructurePlacer.safeSet(level, ground, groundBlock);
				if (distance == 2 && Math.floorMod(x * 3 + z * 5, 3) == 0) {
					StructurePlacer.safeSet(level, ground.above(), forageMarker(culture));
				}
			}
		}
	}

	private static void placeBoundaryMarker(ServerLevel level, BlockPos pos, ColonyCulture culture) {
		BlockPos ground = ColonyService.anchorToSurface(level, pos);
		if (!isTrailSurface(level, ground)) {
			return;
		}
		StructurePlacer.safeSet(level, ground, Blocks.COARSE_DIRT);
		StructurePlacer.safeSet(level, ground.above(), trailMarker(culture));
		StructurePlacer.safeSet(level, ground.above(2), boundaryTop(culture));
	}

	private static void placeRuinedScoutNest(ServerLevel level, BlockPos center, ColonyCulture culture) {
		BlockPos anchored = ColonyService.anchorToSurface(level, center);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance > 5) {
					continue;
				}
				BlockPos ground = ColonyService.anchorToSurface(level, anchored.offset(x, 0, z));
				if (!isTrailSurface(level, ground)) {
					continue;
				}
				StructurePlacer.safeSet(level, ground, ruinedNestGround(culture, x, z, distance));
				StructurePlacer.safeSet(level, ground.above(), Blocks.AIR);
				StructurePlacer.safeSet(level, ground.above(2), Blocks.AIR);
			}
		}
		StructurePlacer.safeSet(level, anchored, ModBlocks.NEST_MOUND);
		StructurePlacer.safeSet(level, anchored.below(), ModBlocks.NEST_CORE);
		StructurePlacer.safeSet(level, anchored.offset(-2, 1, 0), ModBlocks.CHITIN_NODE);
		StructurePlacer.safeSet(level, anchored.offset(2, 1, 0), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, anchored.offset(0, 1, -2), Blocks.MANGROVE_ROOTS);
		StructurePlacer.safeSet(level, anchored.offset(0, 1, 2), boundaryTop(culture));
		StructurePlacer.safeSet(level, anchored.offset(0, 0, -4), Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, anchored.offset(-1, 0, -4), trailMarker(culture));
		StructurePlacer.safeSet(level, anchored.offset(1, 0, -4), trailHeadDressing(culture));
	}

	private static boolean isTrailSurface(ServerLevel level, BlockPos pos) {
		var ground = level.getBlockState(pos);
		return !ground.isAir()
				&& !ground.is(Blocks.WATER)
				&& !ground.is(Blocks.LAVA)
				&& level.getBlockState(pos.above()).isAir();
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static Block forageGround(ColonyCulture culture, int distance) {
		return switch (culture) {
			case LEAFCUTTER -> distance <= 2 ? Blocks.MOSS_BLOCK : Blocks.PODZOL;
			case FIRE -> distance <= 2 ? Blocks.RED_TERRACOTTA : Blocks.COARSE_DIRT;
			case CARPENTER -> distance <= 2 ? Blocks.ROOTED_DIRT : Blocks.PODZOL;
			case AMBER -> distance <= 2 ? Blocks.HONEYCOMB_BLOCK : Blocks.ROOTED_DIRT;
		};
	}

	private static Block forageMarker(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.BROWN_MUSHROOM_BLOCK;
			case FIRE -> Blocks.BLACKSTONE;
			case CARPENTER -> Blocks.MANGROVE_ROOTS;
			case AMBER -> Blocks.HONEYCOMB_BLOCK;
		};
	}

	private static Block trailMarker(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.MOSS_BLOCK;
			case FIRE -> Blocks.RED_TERRACOTTA;
			case CARPENTER -> Blocks.MANGROVE_ROOTS;
			case AMBER -> Blocks.CHISELED_TUFF;
		};
	}

	private static Block trailHeadDressing(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.PODZOL;
			case FIRE -> Blocks.COARSE_DIRT;
			case CARPENTER -> Blocks.ROOTED_DIRT;
			case AMBER -> Blocks.PACKED_MUD;
		};
	}

	private static Block ruinedNestGround(ColonyCulture culture, int x, int z, int distance) {
		if (distance == 0) {
			return ModBlocks.NEST_MOUND;
		}
		if (distance == 1) {
			return Blocks.COARSE_DIRT;
		}
		if (distance == 2 && Math.floorMod(x - z, 2) == 0) {
			return trailMarker(culture);
		}
		if (distance <= 3) {
			return Blocks.ROOTED_DIRT;
		}
		return Math.floorMod(x + z, 2) == 0 ? Blocks.PODZOL : Blocks.COARSE_DIRT;
	}

	private static Block boundaryTop(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.BROWN_MUSHROOM_BLOCK;
			case FIRE -> Blocks.BLACKSTONE;
			case CARPENTER -> Blocks.MANGROVE_PLANKS;
			case AMBER -> Blocks.AMETHYST_BLOCK;
		};
	}

	private static ColonyCulture cultureFor(long seed, BlockPos site) {
		ColonyCulture[] cultures = ColonyCulture.values();
		long mixed = mix(seed ^ site.asLong());
		return cultures[positiveInt(mixed, cultures.length)];
	}

	private static long playerRegionSeed(ServerPlayer player) {
		BlockPos pos = player.blockPosition();
		int regionX = Math.floorDiv(pos.getX(), REGION_SIZE);
		int regionZ = Math.floorDiv(pos.getZ(), REGION_SIZE);
		return mix(player.getUUID().getLeastSignificantBits()
				^ player.getUUID().getMostSignificantBits()
				^ (long) regionX * 341873128712L
				^ (long) regionZ * 132897987541L);
	}

	private static int positiveInt(long value, int bound) {
		return (int) Math.floorMod(value, bound);
	}

	private static long mix(long value) {
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53L;
		value ^= value >>> 33;
		return value;
	}
}
