package com.formicfrontier.world;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.ColonyCulture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class StructurePlacer {
	private StructurePlacer() {
	}

	public static void placeBuilding(ServerLevel level, BlockPos center, BuildingType type) {
		placeBuilding(level, center, type, BuildingVisualStage.COMPLETE);
	}

	public static void placeBuilding(ServerLevel level, BlockPos center, BuildingType type, BuildingVisualStage stage) {
		placeBuilding(level, center, type, stage, ColonyCulture.AMBER);
	}

	public static void placeBuilding(ServerLevel level, BlockPos center, BuildingType type, BuildingVisualStage stage, ColonyCulture culture) {
		if (type == BuildingType.ROAD) {
			placeRoadPatch(level, center);
			return;
		}
		if (stage == BuildingVisualStage.PLANNED || stage == BuildingVisualStage.CONSTRUCTION || stage == BuildingVisualStage.REPAIRING) {
			placeStagedBuilding(level, center, type, stage, culture);
			return;
		}
		switch (type) {
			case QUEEN_CHAMBER -> placeQueenHall(level, center, culture);
			case FOOD_STORE, NURSERY, MINE, BARRACKS, MARKET, RESIN_DEPOT, PHEROMONE_ARCHIVE, VENOM_PRESS, ARMORY -> placeCampusBuilding(level, center, type, culture);
			case CHITIN_FARM -> placeChitinFarm(level, center, culture);
			case DIPLOMACY_SHRINE -> placeDiplomacyShrine(level, center, culture);
			case WATCH_POST -> placeWatchPost(level, center, culture);
			case FUNGUS_GARDEN -> placeFungusGarden(level, center, culture);
			case GREAT_MOUND -> placeGreatMoundProject(level, center, culture);
			case QUEEN_VAULT -> placeQueenVault(level, center, culture);
			case TRADE_HUB -> placeTradeHub(level, center, culture);
			case ROAD -> placeRoadPatch(level, center);
		}
		if (stage == BuildingVisualStage.COMPLETE) {
			placeCompleteOverlay(level, center, type, culture);
		} else if (stage == BuildingVisualStage.UPGRADED) {
			placeUpgradeOverlay(level, center, type, culture);
		} else if (stage == BuildingVisualStage.DAMAGED) {
			placeDamagedOverlay(level, center, type);
		}
	}

	public static boolean safeSet(ServerLevel level, BlockPos pos, Block block) {
		if (!canReplace(level, pos)) {
			return false;
		}
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		return true;
	}

	public static boolean canReplace(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (level.getBlockEntity(pos) != null) {
			return false;
		}
		Block block = state.getBlock();
		return state.isAir()
				|| state.is(BlockTags.FLOWERS)
				|| state.is(BlockTags.LEAVES)
				|| state.is(BlockTags.LOGS)
				|| state.is(BlockTags.REPLACEABLE_BY_TREES)
				|| block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.ROOTED_DIRT
				|| block == Blocks.PODZOL
				|| block == Blocks.MUD
				|| block == Blocks.PACKED_MUD
				|| block == Blocks.GRAVEL
				|| block == Blocks.STONE
				|| block == Blocks.COBBLESTONE
				|| block == Blocks.DEEPSLATE
				|| block == Blocks.COBBLED_DEEPSLATE
				|| block == Blocks.MOSS_BLOCK
				|| block == Blocks.MYCELIUM
				|| block == Blocks.DIRT_PATH
				|| block == Blocks.MANGROVE_ROOTS
				|| block == Blocks.MUDDY_MANGROVE_ROOTS
				|| block == Blocks.MANGROVE_PLANKS
				|| block == Blocks.MUD_BRICKS
				|| block == Blocks.CUT_COPPER
				|| block == Blocks.CHISELED_TUFF
				|| block == Blocks.RED_TERRACOTTA
				|| block == Blocks.BLACKSTONE
				|| block == Blocks.POLISHED_BLACKSTONE
				|| block == Blocks.HONEYCOMB_BLOCK
				|| block == Blocks.HONEY_BLOCK
				|| block == Blocks.BROWN_MUSHROOM_BLOCK
				|| block == Blocks.RED_MUSHROOM_BLOCK
				|| block == Blocks.POLISHED_DEEPSLATE
				|| block == Blocks.COBBLED_DEEPSLATE_WALL
				|| block == Blocks.IRON_ORE
				|| block == Blocks.DEEPSLATE_IRON_ORE
				|| block == Blocks.BONE_BLOCK
				|| block == Blocks.OCHRE_FROGLIGHT
				|| block == Blocks.AMETHYST_BLOCK
				|| block == Blocks.CANDLE
				|| block == Blocks.BARREL
				|| block == Blocks.BELL
				|| block == Blocks.OAK_FENCE
				|| block == Blocks.OAK_LOG
				|| block == Blocks.HAY_BLOCK
				|| block == Blocks.GOLD_BLOCK
				|| block == Blocks.SLIME_BLOCK
				|| block == ModBlocks.NEST_MOUND
				|| block == ModBlocks.NEST_CORE
				|| block == ModBlocks.COLONY_LEDGER
				|| block == ModBlocks.FOOD_CHAMBER
				|| block == ModBlocks.NURSERY_CHAMBER
				|| block == ModBlocks.MINE_CHAMBER
				|| block == ModBlocks.BARRACKS_CHAMBER
				|| block == ModBlocks.MARKET_CHAMBER
				|| block == ModBlocks.DIPLOMACY_SHRINE
				|| block == ModBlocks.WATCH_POST
				|| block == ModBlocks.RESIN_DEPOT
				|| block == ModBlocks.PHEROMONE_ARCHIVE
				|| block == ModBlocks.FUNGUS_GARDEN
				|| block == ModBlocks.VENOM_PRESS
				|| block == ModBlocks.ARMORY
				|| block == ModBlocks.FOOD_NODE
				|| block == ModBlocks.ORE_NODE
				|| block == ModBlocks.CHITIN_NODE
				|| block == ModBlocks.CHITIN_BED;
	}

	public static void placeQueenHall(ServerLevel level, BlockPos center) {
		placeQueenHall(level, center, ColonyCulture.AMBER);
	}

	public static void placeQueenHall(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -12; x <= 12; x++) {
			for (int z = -12; z <= 12; z++) {
				int score = queenMoundScore(x, z);
				if (score <= 300) {
					safeSet(level, center.offset(x, 0, z), queenMoundFloorBlock(x, z, score, culture));
				}
				for (int y = 1; y <= 7; y++) {
					if (score > queenMoundLayerLimit(y)) {
						continue;
					}
					BlockPos pos = center.offset(x, y, z);
					if (isQueenMoundChamber(x, z, y) || isQueenMoundEntrance(x, z, y)) {
						safeSet(level, pos, Blocks.AIR);
					} else {
						safeSet(level, pos, queenMoundShellBlock(x, z, y, score, culture));
					}
				}
			}
		}
		carveQueenMoundEntrances(level, center);
		placeQueenMoundSkirts(level, center, culture);
		placeQueenMoundRoots(level, center);
		placeQueenMoundEntranceDetails(level, center);
		placeQueenMoundSpoilRidges(level, center);
		placeQueenMoundTunnelThresholds(level, center);
		placeQueenMoundApproachAprons(level, center);
		placeQueenMoundFrontTrailFork(level, center);
		placeQueenMoundScentTrail(level, center, culture);
		placeQueenMoundEntranceWaystones(level, center, culture);
		placeQueenMoundErosionRunnels(level, center);
		placeQueenMoundForageTrailheads(level, center, culture);
		placeQueenMoundForageCrawls(level, center);
		placeQueenMoundScoutPorches(level, center);
		placeQueenMoundBroodCrawl(level, center, culture);
		placeQueenMoundOrganicBreaks(level, center);
		placeQueenMoundDiagonalClefts(level, center);
		placeQueenMoundVentChimneys(level, center, culture);
		placeQueenMoundRearVentApron(level, center);
		placeQueenMoundTunnelMouthRims(level, center);
		placeQueenMoundCrownTerraces(level, center);
		placeQueenMoundCultureMarkers(level, center, culture);
		safeSet(level, center.below(), ModBlocks.NEST_CORE);
		safeSet(level, center, ModBlocks.NEST_MOUND);
		placeColonyLedger(level, center.offset(3, 1, 0));
		safeSet(level, center.offset(0, 3, 0), Blocks.OCHRE_FROGLIGHT);
	}

	public static void placeGreatMoundProject(ServerLevel level, BlockPos center, ColonyCulture culture) {
		placeQueenHall(level, center, culture);
		for (int x = -14; x <= 14; x++) {
			for (int z = -14; z <= 14; z++) {
				if (isGreatMoundPath(x, z)) {
					safeSet(level, center.offset(x, 0, z), Blocks.DIRT_PATH);
					for (int y = 1; y <= 3; y++) {
						safeSet(level, center.offset(x, y, z), Blocks.AIR);
					}
					continue;
				}
				int score = greatMoundScore(x, z);
				boolean outsideStarter = Math.abs(x) > 8 || Math.abs(z) > 8;
				if (score <= 408 && outsideStarter) {
					safeSet(level, center.offset(x, 0, z), greatMoundSkirtBlock(x, z, score, culture));
				}
				for (int y = 1; y <= 7; y++) {
					if (!outsideStarter || score > greatMoundLayerLimit(y)) {
						continue;
					}
					BlockPos pos = center.offset(x, y, z);
					if (Math.floorMod(x - z + y, 9) == 0) {
						safeSet(level, pos, Blocks.MANGROVE_ROOTS);
					} else {
						safeSet(level, pos, greatMoundShellBlock(x, z, y, score, culture));
					}
				}
			}
		}
		placeGreatMoundCrown(level, center, culture);
		placeGreatMoundArchiveNetwork(level, center, culture);
		placeColonyLedger(level, center.offset(4, 1, 1));
	}

	public static void placeQueenVault(ServerLevel level, BlockPos center, ColonyCulture culture) {
		placeGreatMoundProject(level, center, culture);
		placeQueenVaultCore(level, center, culture);
		placeQueenVaultMouth(level, center.offset(0, 0, -12), 0, -1, culture);
		placeQueenVaultMouth(level, center.offset(12, 0, 0), 1, 0, culture);
		placeQueenVaultMouth(level, center.offset(0, 0, 12), 0, 1, culture);
		placeQueenVaultMouth(level, center.offset(-12, 0, 0), -1, 0, culture);
		placeColonyLedger(level, center.offset(-4, 1, 1));
	}

	public static void placeTradeHub(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		for (int x = -11; x <= 11; x++) {
			for (int z = -8; z <= 8; z++) {
				int score = x * x * 2 + z * z * 3;
				if (score > 310) {
					continue;
				}
				Block floor = Math.abs(x) <= 1 || Math.abs(z) <= 1 ? Blocks.DIRT_PATH : Blocks.PACKED_MUD;
				if (Math.abs(x) == 11 || Math.abs(z) == 8 || score > 230) {
					floor = Math.floorMod(x + z, 3) == 0 ? primary : Blocks.CHISELED_TUFF;
				} else if (Math.floorMod(x - z, 5) == 0) {
					floor = secondary;
				}
				safeSet(level, center.offset(x, 0, z), floor);
				for (int y = 1; y <= 4; y++) {
					safeSet(level, center.offset(x, y, z), Blocks.AIR);
				}
			}
		}
		placeTradeHubRoute(level, center);
		placeTradeHubStall(level, center.offset(-8, 0, -5), ModBlocks.FOOD_NODE, Blocks.HAY_BLOCK, primary);
		placeTradeHubStall(level, center.offset(8, 0, -5), ModBlocks.ORE_NODE, Blocks.IRON_ORE, secondary);
		placeTradeHubStall(level, center.offset(-8, 0, 5), ModBlocks.CHITIN_NODE, Blocks.BONE_BLOCK, secondary);
		placeTradeHubStall(level, center.offset(8, 0, 5), ModBlocks.RESIN_DEPOT, Blocks.HONEY_BLOCK, primary);
		safeSet(level, center, ModBlocks.MARKET_CHAMBER);
		placeColonyLedger(level, center.offset(0, 1, 1));
		safeSet(level, center.offset(0, 1, -1), Blocks.BELL);
		safeSet(level, center.offset(-1, 1, 0), Blocks.GOLD_BLOCK);
		safeSet(level, center.offset(1, 1, 0), Blocks.BARREL);
		safeSet(level, center.above(4), Blocks.OCHRE_FROGLIGHT);
	}

	private static void placeTradeHubRoute(ServerLevel level, BlockPos center) {
		for (int step = 1; step <= 28; step++) {
			int x = -step;
			int z = step * 18 / 28;
			safeSet(level, center.offset(x, 0, z), Blocks.DIRT_PATH);
			if (step % 4 == 0) {
				safeSet(level, center.offset(x, 0, z + 1), Blocks.COARSE_DIRT);
			}
		}
		for (int x = -22; x <= 22; x++) {
			if (Math.abs(x) <= 11) {
				continue;
			}
			safeSet(level, center.offset(x, 0, 0), Blocks.DIRT_PATH);
		}
	}

	private static void placeTradeHubStall(ServerLevel level, BlockPos base, Block marker, Block goods, Block accent) {
		safeSet(level, base, marker);
		safeSet(level, base.above(), goods);
		safeSet(level, base.offset(1, 1, 0), Blocks.BARREL);
		safeSet(level, base.offset(-1, 1, 0), accent);
		for (BlockPos post : new BlockPos[] {base.offset(-1, 1, -1), base.offset(1, 1, -1), base.offset(-1, 1, 1), base.offset(1, 1, 1)}) {
			safeSet(level, post, Blocks.OAK_FENCE);
		}
		safeSet(level, base.above(2), Blocks.OCHRE_FROGLIGHT);
	}

	private static void placeQueenVaultCore(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance > 6) {
					continue;
				}
				safeSet(level, center.offset(x, -2, z), distance <= 1 ? ModBlocks.NEST_CORE : Blocks.CHISELED_TUFF);
				if (distance <= 3 && Math.floorMod(x + z, 2) == 0) {
					safeSet(level, center.offset(x, -1, z), distance == 0 ? Blocks.AMETHYST_BLOCK : secondary);
				}
			}
		}
		safeSet(level, center.offset(-2, -1, 0), primary);
		safeSet(level, center.offset(2, -1, 0), primary);
		safeSet(level, center.offset(0, -1, -2), Blocks.HONEYCOMB_BLOCK);
		safeSet(level, center.offset(0, -1, 2), Blocks.HONEYCOMB_BLOCK);
	}

	private static void placeQueenVaultMouth(ServerLevel level, BlockPos base, int stepX, int stepZ, ColonyCulture culture) {
		int sideX = -stepZ;
		int sideZ = stepX;
		Block secondary = cultureSecondaryBlock(culture);

		for (int depth = -1; depth <= 2; depth++) {
			BlockPos path = base.offset(stepX * depth, 0, stepZ * depth);
			safeSet(level, path, depth == 0 ? Blocks.CHISELED_TUFF : Blocks.DIRT_PATH);
			safeSet(level, path.above(), Blocks.AIR);
			safeSet(level, path.above(2), Blocks.AIR);
		}

		safeSet(level, base, Blocks.CHISELED_TUFF);
		safeSet(level, base.offset(stepX, 0, stepZ), Blocks.DIRT_PATH);
		safeSet(level, base.offset(-stepX, 0, -stepZ), Blocks.CHISELED_TUFF);
		safeSet(level, base.offset(sideX, 0, sideZ), Blocks.HONEYCOMB_BLOCK);
		safeSet(level, base.offset(-sideX, 0, -sideZ), secondary);
		safeSet(level, base.offset(stepX + sideX, 0, stepZ + sideZ), Blocks.CHISELED_TUFF);
		safeSet(level, base.offset(stepX - sideX, 0, stepZ - sideZ), Blocks.CHISELED_TUFF);
		safeSet(level, base.offset(-stepX + sideX, 0, -stepZ + sideZ), Blocks.HONEYCOMB_BLOCK);
		safeSet(level, base.offset(-stepX - sideX, 0, -stepZ - sideZ), secondary);
		placeQueenVaultRib(level, base.offset(sideX, 0, sideZ), Blocks.CHISELED_TUFF, Blocks.AMETHYST_BLOCK);
		placeQueenVaultRib(level, base.offset(-sideX, 0, -sideZ), Blocks.CHISELED_TUFF, Blocks.AMETHYST_BLOCK);
		placeQueenVaultRib(level, base.offset(stepX + sideX, 0, stepZ + sideZ), Blocks.HONEYCOMB_BLOCK, secondary);
		placeQueenVaultRib(level, base.offset(stepX - sideX, 0, stepZ - sideZ), secondary, Blocks.HONEYCOMB_BLOCK);
		safeSet(level, base.offset(-stepX, 1, -stepZ), Blocks.CANDLE);
		safeSet(level, base.offset(stepX + sideX, 1, stepZ + sideZ), Blocks.OCHRE_FROGLIGHT);
		safeSet(level, base.offset(stepX - sideX, 1, stepZ - sideZ), Blocks.CANDLE);
	}

	private static void placeQueenVaultRib(ServerLevel level, BlockPos foot, Block lower, Block cap) {
		safeSet(level, foot.above(), lower);
		safeSet(level, foot.above(2), cap);
	}

	private static int greatMoundScore(int x, int z) {
		return x * x * 2 + z * z * 2 + Math.floorMod(x * 5 + z * 7, 9);
	}

	private static int greatMoundLayerLimit(int y) {
		return switch (y) {
			case 1 -> 386;
			case 2 -> 318;
			case 3 -> 252;
			case 4 -> 196;
			case 5 -> 148;
			case 6 -> 108;
			default -> 74;
		};
	}

	private static boolean isGreatMoundPath(int x, int z) {
		return Math.abs(x) <= 1 && z >= -17 && z <= -6
				|| Math.abs(z) <= 1 && x >= 6 && x <= 17
				|| Math.abs(x) <= 1 && z >= 6 && z <= 17
				|| Math.abs(z) <= 1 && x >= -17 && x <= -6;
	}

	private static Block greatMoundSkirtBlock(int x, int z, int score, ColonyCulture culture) {
		if (Math.floorMod(x + z, 8) == 0) {
			return culturePrimaryBlock(culture);
		}
		if (Math.floorMod(score, 11) == 0) {
			return cultureSecondaryBlock(culture);
		}
		if (Math.floorMod(x - z, 5) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		return score % 3 == 0 ? Blocks.PODZOL : Blocks.PACKED_MUD;
	}

	private static Block greatMoundShellBlock(int x, int z, int y, int score, ColonyCulture culture) {
		if (Math.floorMod(score + y, 13) == 0) {
			return culturePrimaryBlock(culture);
		}
		if (Math.floorMod(x + z + y, 7) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		return y >= 5 ? ModBlocks.NEST_MOUND : cultureShellBlock(BuildingType.QUEEN_CHAMBER, culture);
	}

	private static void placeGreatMoundCrown(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		for (BlockPos pos : new BlockPos[] {
				center.offset(0, 6, 0),
				center.offset(-1, 6, 0),
				center.offset(1, 6, 0),
				center.offset(0, 6, -1),
				center.offset(0, 6, 1)
		}) {
			safeSet(level, pos, primary);
		}
		safeSet(level, center.offset(0, 7, 0), Blocks.AMETHYST_BLOCK);
		safeSet(level, center.offset(0, 8, 0), Blocks.OCHRE_FROGLIGHT);
		safeSet(level, center.offset(-2, 6, -2), secondary);
		safeSet(level, center.offset(2, 6, 2), secondary);
		safeSet(level, center.offset(-2, 5, 2), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 5, -2), Blocks.MANGROVE_ROOTS);
	}

	private static void placeGreatMoundArchiveNetwork(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (BlockPos node : new BlockPos[] {
				center.offset(0, 0, -16),
				center.offset(16, 0, 0),
				center.offset(0, 0, 16),
				center.offset(-16, 0, 0)
		}) {
			placeGreatMoundArchiveNode(level, node, culture);
			placeRunicPath(level, center, node, culture);
		}
	}

	private static void placeGreatMoundArchiveNode(ServerLevel level, BlockPos node, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) + Math.abs(z) <= 3) {
					safeSet(level, node.offset(x, 0, z), Math.abs(x) + Math.abs(z) <= 1 ? Blocks.CHISELED_TUFF : Blocks.DIRT_PATH);
				}
			}
		}
		safeSet(level, node.above(), ModBlocks.PHEROMONE_ARCHIVE);
		safeSet(level, node.above(2), Blocks.AMETHYST_BLOCK);
		safeSet(level, node.above(3), Blocks.CANDLE);
		safeSet(level, node.north(), primary);
		safeSet(level, node.south(), primary);
	}

	private static void placeRunicPath(ServerLevel level, BlockPos start, BlockPos end, ColonyCulture culture) {
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 40) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (steps < 7 || current.equals(end)) {
				continue;
			}
			safeSet(level, current, Blocks.DIRT_PATH);
			if (steps % 4 == 0) {
				safeSet(level, current.above(), cultureSecondaryBlock(culture));
			}
		}
	}

	private static int queenMoundScore(int x, int z) {
		return x * x * 2 + z * z * 3 + Math.floorMod(x * 7 + z * 11, 5);
	}

	private static int queenMoundLayerLimit(int y) {
		return switch (y) {
			case 1 -> 296;
			case 2 -> 230;
			case 3 -> 168;
			case 4 -> 110;
			case 5 -> 66;
			case 6 -> 38;
			default -> 18;
		};
	}

	private static boolean isQueenMoundChamber(int x, int z, int y) {
		int ax = Math.abs(x);
		int az = Math.abs(z);
		return y <= 4 && ax <= 4 && az <= 4 && ax + az <= 7;
	}

	private static boolean isQueenMoundEntrance(int x, int z, int y) {
		return y <= 3 && Math.abs(x) <= 2 && z >= -11 && z <= -5
				|| y <= 3 && x >= 7 && x <= 12 && Math.abs(z) <= 1;
	}

	private static Block queenMoundFloorBlock(int x, int z, int score, ColonyCulture culture) {
		if (x == 0 && z == 0 || Math.abs(x) + Math.abs(z) <= 2) {
			return ModBlocks.NEST_MOUND;
		}
		if (culture == ColonyCulture.LEAFCUTTER && score % 11 == 0) {
			return Blocks.MOSS_BLOCK;
		}
		if (culture == ColonyCulture.FIRE && score % 13 == 0) {
			return Blocks.RED_TERRACOTTA;
		}
		if (culture == ColonyCulture.CARPENTER && score % 13 == 0) {
			return Blocks.MANGROVE_PLANKS;
		}
		if (Math.floorMod(x - z, 6) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		if (score % 7 == 0) {
			return Blocks.COARSE_DIRT;
		}
		return Blocks.PACKED_MUD;
	}

	private static Block queenMoundShellBlock(int x, int z, int y, int score, ColonyCulture culture) {
		Block cultureBlock = queenMoundCultureBlock(x, z, y, score, culture);
		if (cultureBlock != null) {
			return cultureBlock;
		}
		if (y <= 2 && Math.floorMod(x + z + y, 7) == 0) {
			return Blocks.MUDDY_MANGROVE_ROOTS;
		}
		if (Math.floorMod(score + y, 5) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		if (y >= 4 || Math.floorMod(x - z + y, 4) == 0) {
			return ModBlocks.NEST_MOUND;
		}
		return y == 1 ? Blocks.PACKED_MUD : Blocks.COARSE_DIRT;
	}

	private static Block queenMoundCultureBlock(int x, int z, int y, int score, ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Math.floorMod(score + y, 9) == 0 ? Blocks.MOSS_BLOCK : null;
			case FIRE -> Math.floorMod(x * 3 + z * 5 + y, 11) == 0 ? Blocks.RED_TERRACOTTA : null;
			case CARPENTER -> Math.floorMod(score + x - z + y, 10) == 0 ? Blocks.MANGROVE_PLANKS : null;
			case AMBER -> Math.floorMod(score + y, 13) == 0 ? Blocks.HONEYCOMB_BLOCK : null;
		};
	}

	private static void placeQueenMoundSkirts(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -16; x <= 16; x++) {
			for (int z = -16; z <= 16; z++) {
				if (isQueenMoundPath(x, z)) {
					continue;
				}
				int score = queenMoundScore(x, z);
				if (score > 300 && score <= 420) {
					safeSet(level, center.offset(x, 0, z), queenMoundSkirtBlock(x, z, score, culture));
				}
				if (score > 292 && score <= 360 && Math.abs(x) + Math.abs(z) >= 13 && Math.floorMod(x * 5 + z * 3, 4) == 0) {
					safeSet(level, center.offset(x, 1, z), Blocks.ROOTED_DIRT);
				}
			}
		}
		safeSet(level, center.offset(-3, 1, -12), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(3, 1, -12), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(12, 1, 2), Blocks.ROOTED_DIRT);
	}

	private static boolean isQueenMoundPath(int x, int z) {
		return Math.abs(x) <= 2 && z >= -15 && z <= -5
				|| x >= 7 && x <= 14 && Math.abs(z) <= 1;
	}

	private static Block queenMoundSkirtBlock(int x, int z, int score, ColonyCulture culture) {
		if (culture == ColonyCulture.LEAFCUTTER && Math.floorMod(x + z, 4) == 0) {
			return Blocks.MOSS_BLOCK;
		}
		if (culture == ColonyCulture.FIRE && Math.floorMod(x - z, 4) == 0) {
			return Blocks.RED_TERRACOTTA;
		}
		if (culture == ColonyCulture.CARPENTER && Math.floorMod(x + z, 4) == 0) {
			return Blocks.MANGROVE_PLANKS;
		}
		if (Math.floorMod(x + z, 5) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		if (score % 3 == 0) {
			return Blocks.COARSE_DIRT;
		}
		return Blocks.PODZOL;
	}

	private static void carveQueenMoundEntrances(ServerLevel level, BlockPos center) {
		for (int z = -10; z <= -3; z++) {
			for (int x = -1; x <= 1; x++) {
				safeSet(level, center.offset(x, 0, z), Blocks.DIRT_PATH);
				for (int y = 1; y <= 2; y++) {
					safeSet(level, center.offset(x, y, z), Blocks.AIR);
				}
			}
			if (z >= -7 && z <= -4) {
				safeSet(level, center.offset(-2, 1, z), Blocks.ROOTED_DIRT);
				safeSet(level, center.offset(2, 1, z), Blocks.ROOTED_DIRT);
			}
		}
		for (int x = 4; x <= 9; x++) {
			for (int z = 0; z <= 1; z++) {
				safeSet(level, center.offset(x, 0, z), Blocks.DIRT_PATH);
				for (int y = 1; y <= 2; y++) {
					safeSet(level, center.offset(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	private static void placeQueenMoundRoots(ServerLevel level, BlockPos center) {
		BlockPos[] roots = {
				center.offset(-6, 1, -1),
				center.offset(-5, 2, 2),
				center.offset(-3, 2, 5),
				center.offset(0, 2, 6),
				center.offset(4, 2, 3),
				center.offset(6, 1, -2),
				center.offset(2, 3, 4),
				center.offset(-2, 3, 3)
		};
		for (BlockPos root : roots) {
			safeSet(level, root, Blocks.MANGROVE_ROOTS);
		}
		safeSet(level, center.offset(-2, 1, -5), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 1, -5), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(5, 1, 2), Blocks.MUDDY_MANGROVE_ROOTS);
	}

	private static void placeQueenMoundEntranceDetails(ServerLevel level, BlockPos center) {
		for (int x = -1; x <= 1; x++) {
			safeSet(level, center.offset(x, 0, -6), Blocks.MUD);
		}
		safeSet(level, center.offset(-2, 2, -7), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 2, -7), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(0, 3, -7), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-3, 1, -9), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(3, 1, -9), Blocks.ROOTED_DIRT);

		safeSet(level, center.offset(5, 0, 2), Blocks.MUD);
		safeSet(level, center.offset(7, 0, 2), Blocks.MUD);
		safeSet(level, center.offset(5, 3, 0), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(7, 2, 2), Blocks.ROOTED_DIRT);
	}

	private static void placeQueenMoundSpoilRidges(ServerLevel level, BlockPos center) {
		for (int z = -10; z <= -4; z++) {
			Block leftFloor = Math.floorMod(z, 2) == 0 ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT;
			Block rightFloor = Math.floorMod(z, 2) == 0 ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT;
			safeSet(level, center.offset(-3, 0, z), leftFloor);
			safeSet(level, center.offset(3, 0, z), rightFloor);
			if (z >= -8 && z <= -5) {
				safeSet(level, center.offset(-3, 1, z), Blocks.ROOTED_DIRT);
				safeSet(level, center.offset(3, 1, z), Math.floorMod(z, 2) == 0 ? Blocks.MUDDY_MANGROVE_ROOTS : Blocks.ROOTED_DIRT);
			}
		}
		safeSet(level, center.offset(-4, 1, -7), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(4, 1, -6), Blocks.ROOTED_DIRT);

		safeSet(level, center.offset(6, 0, 2), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(8, 0, 2), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(9, 0, 2), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(6, 1, 3), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(9, 1, 2), Blocks.ROOTED_DIRT);
	}

	private static void placeQueenMoundTunnelThresholds(ServerLevel level, BlockPos center) {
		for (int z = -6; z <= -4; z++) {
			safeSet(level, center.offset(0, 0, z), Blocks.MUD);
		}
		safeSet(level, center.offset(0, 3, -5), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-1, 3, -6), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(1, 3, -6), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(6, 3, 1), Blocks.MANGROVE_ROOTS);

		BlockPos[] frontSpoil = {
				center.offset(-5, 0, -10),
				center.offset(-4, 0, -11),
				center.offset(-3, 0, -12),
				center.offset(3, 0, -12),
				center.offset(4, 0, -11),
				center.offset(5, 0, -10)
		};
		for (int i = 0; i < frontSpoil.length; i++) {
			Block block = i % 3 == 0 ? Blocks.ROOTED_DIRT : i % 3 == 1 ? Blocks.COARSE_DIRT : Blocks.PODZOL;
			safeSet(level, frontSpoil[i], block);
		}
	}

	private static void placeQueenMoundApproachAprons(ServerLevel level, BlockPos center) {
		BlockPos[] frontPath = {
				center.offset(0, 0, -11),
				center.offset(0, 0, -12),
				center.offset(0, 0, -13)
		};
		for (BlockPos path : frontPath) {
			safeSet(level, path, Blocks.DIRT_PATH);
			safeSet(level, path.above(), Blocks.AIR);
			safeSet(level, path.above(2), Blocks.AIR);
		}
		safeSet(level, center.offset(-1, 0, -11), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(1, 0, -11), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-2, 0, -12), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(2, 0, -12), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(-1, 1, -10), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(1, 1, -10), Blocks.MUDDY_MANGROVE_ROOTS);

		BlockPos[] sidePath = {
				center.offset(10, 0, 0),
				center.offset(11, 0, 0),
				center.offset(10, 0, 1),
				center.offset(11, 0, 1)
		};
		for (BlockPos path : sidePath) {
			safeSet(level, path, Blocks.DIRT_PATH);
			safeSet(level, path.above(), Blocks.AIR);
			safeSet(level, path.above(2), Blocks.AIR);
		}
		safeSet(level, center.offset(10, 0, -1), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(10, 0, 2), Blocks.MUD);
		safeSet(level, center.offset(11, 0, -1), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(11, 0, 2), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(9, 1, -1), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(9, 1, 2), Blocks.ROOTED_DIRT);
	}

	private static void placeQueenMoundFrontTrailFork(ServerLevel level, BlockPos center) {
		BlockPos[] spine = {
				center.offset(0, 0, -14),
				center.offset(0, 0, -15),
				center.offset(0, 0, -16)
		};
		for (BlockPos path : spine) {
			safeSet(level, path, Blocks.DIRT_PATH);
			safeSet(level, path.above(), Blocks.AIR);
			safeSet(level, path.above(2), Blocks.AIR);
		}

		safeSet(level, center.offset(-1, 0, -14), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(1, 0, -14), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-2, 0, -15), Blocks.DIRT_PATH);
		safeSet(level, center.offset(2, 0, -15), Blocks.DIRT_PATH);
		safeSet(level, center.offset(-3, 0, -16), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(3, 0, -16), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-1, 1, -15), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(1, 1, -15), Blocks.MUDDY_MANGROVE_ROOTS);
	}

	private static void placeQueenMoundScentTrail(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		safeSet(level, center.offset(-1, 0, -16), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(1, 0, -16), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-2, 0, -16), Blocks.MUD);
		safeSet(level, center.offset(2, 0, -16), Blocks.PODZOL);
		safeSet(level, center.offset(-2, 1, -16), primary);
		safeSet(level, center.offset(2, 1, -16), secondary);
		safeSet(level, center.offset(-3, 1, -16), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(3, 1, -16), Blocks.MUDDY_MANGROVE_ROOTS);
	}

	private static void placeQueenMoundEntranceWaystones(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		BlockPos[] primaryStones = {
				center.offset(-2, 0, -10),
				center.offset(9, 0, -2),
				center.offset(-2, 0, 8)
		};
		BlockPos[] secondaryStones = {
				center.offset(2, 0, -10),
				center.offset(9, 0, 3),
				center.offset(2, 0, 8)
		};
		for (BlockPos stone : primaryStones) {
			safeSet(level, stone, primary);
			safeSet(level, stone.above(), Blocks.MANGROVE_ROOTS);
		}
		for (BlockPos stone : secondaryStones) {
			safeSet(level, stone, secondary);
			safeSet(level, stone.above(), Blocks.ROOTED_DIRT);
		}
	}

	private static void placeQueenMoundErosionRunnels(ServerLevel level, BlockPos center) {
		placeQueenMoundErosionRunnel(level, center, -1);
		placeQueenMoundErosionRunnel(level, center, 1);
	}

	private static void placeQueenMoundErosionRunnel(ServerLevel level, BlockPos center, int side) {
		BlockPos[] floors = {
				center.offset(side * 3, 0, -12),
				center.offset(side * 4, 0, -13),
				center.offset(side * 5, 0, -14),
				center.offset(side * 6, 0, -15),
				center.offset(side * 7, 0, -16)
		};
		Block[] floorBlocks = {
				Blocks.PODZOL,
				Blocks.COARSE_DIRT,
				Blocks.MUD,
				Blocks.DIRT_PATH,
				Blocks.PODZOL
		};
		for (int i = 0; i < floors.length; i++) {
			BlockPos floor = floors[i];
			safeSet(level, floor, floorBlocks[i]);
			safeSet(level, floor.above(), Blocks.AIR);
			safeSet(level, floor.above(2), Blocks.AIR);
		}

		safeSet(level, center.offset(side * 3, 1, -14), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(side * 5, 1, -13), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(side * 7, 1, -15), Blocks.ROOTED_DIRT);
	}

	private static void placeQueenMoundForageTrailheads(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		for (int side : new int[] {-1, 1}) {
			safeSet(level, center.offset(side * 4, 0, -15), Blocks.DIRT_PATH);
			safeSet(level, center.offset(side * 5, 0, -16), Blocks.COARSE_DIRT);
			safeSet(level, center.offset(side * 6, 0, -17), Blocks.DIRT_PATH);
			safeSet(level, center.offset(side * 4, 1, -15), Blocks.AIR);
			safeSet(level, center.offset(side * 4, 2, -15), Blocks.AIR);
			safeSet(level, center.offset(side * 5, 1, -16), primary);
			safeSet(level, center.offset(side * 6, 1, -17), Blocks.ROOTED_DIRT);
		}
	}

	private static void placeQueenMoundForageCrawls(ServerLevel level, BlockPos center) {
		BlockPos[] crawlFloors = {
				center.offset(-4, 0, -9),
				center.offset(-5, 0, -9),
				center.offset(-6, 0, -8),
				center.offset(4, 0, -9),
				center.offset(5, 0, -9),
				center.offset(6, 0, -8)
		};
		for (int i = 0; i < crawlFloors.length; i++) {
			BlockPos floor = crawlFloors[i];
			safeSet(level, floor, i % 3 == 1 ? Blocks.MUD : Blocks.DIRT_PATH);
			safeSet(level, floor.above(), Blocks.AIR);
			safeSet(level, floor.above(2), Blocks.AIR);
		}

		BlockPos[] crawlRoots = {
				center.offset(-5, 1, -10),
				center.offset(-6, 1, -7),
				center.offset(-6, 2, -8),
				center.offset(5, 1, -10),
				center.offset(6, 1, -7),
				center.offset(6, 2, -8)
		};
		for (BlockPos root : crawlRoots) {
			safeSet(level, root, Blocks.MANGROVE_ROOTS);
		}

		BlockPos[] spoilFan = {
				center.offset(-7, 0, -8),
				center.offset(-8, 0, -7),
				center.offset(-7, 1, -7),
				center.offset(7, 0, -8),
				center.offset(8, 0, -7),
				center.offset(7, 1, -7)
		};
		for (int i = 0; i < spoilFan.length; i++) {
			Block block = i % 3 == 0 ? Blocks.ROOTED_DIRT : i % 3 == 1 ? Blocks.PODZOL : Blocks.COARSE_DIRT;
			safeSet(level, spoilFan[i], block);
		}
	}

	private static void placeQueenMoundScoutPorches(ServerLevel level, BlockPos center) {
		placeQueenMoundScoutPorch(level, center, -1);
		placeQueenMoundScoutPorch(level, center, 1);
	}

	private static void placeQueenMoundScoutPorch(ServerLevel level, BlockPos center, int side) {
		BlockPos[] floor = {
				center.offset(side * 8, 0, -6),
				center.offset(side * 9, 0, -5),
				center.offset(side * 10, 0, -4)
		};
		Block[] floorBlocks = {Blocks.ROOTED_DIRT, Blocks.DIRT_PATH, Blocks.MUD};
		for (int i = 0; i < floor.length; i++) {
			safeSet(level, floor[i], floorBlocks[i]);
			safeSet(level, floor[i].above(), Blocks.AIR);
			safeSet(level, floor[i].above(2), Blocks.AIR);
		}

		safeSet(level, center.offset(side * 8, 1, -7), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(side * 10, 1, -5), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(side * 9, 2, -6), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(side * 11, 0, -4), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(side * 11, 1, -4), Blocks.ROOTED_DIRT);
	}

	private static void placeQueenMoundBroodCrawl(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -11; x <= -4; x++) {
			for (int z = 0; z <= 1; z++) {
				Block floor = Math.floorMod(x + z, 3) == 0 ? Blocks.MUD : Blocks.DIRT_PATH;
				safeSet(level, center.offset(x, 0, z), floor);
				safeSet(level, center.offset(x, 1, z), Blocks.AIR);
				safeSet(level, center.offset(x, 2, z), Blocks.AIR);
			}
		}

		safeSet(level, center.offset(-12, 0, 0), Blocks.DIRT_PATH);
		safeSet(level, center.offset(-12, 0, 1), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(-13, 0, 0), Blocks.DIRT_PATH);
		safeSet(level, center.offset(-13, 0, 1), Blocks.PODZOL);
		safeSet(level, center.offset(-12, 1, 0), Blocks.AIR);
		safeSet(level, center.offset(-12, 2, 0), Blocks.AIR);

		safeSet(level, center.offset(-8, 1, -1), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-9, 2, 1), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(-10, 1, 2), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-11, 1, -1), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-12, 1, 2), culturePrimaryBlock(culture));
		safeSet(level, center.offset(-13, 1, -1), cultureSecondaryBlock(culture));
	}

	private static void placeQueenMoundOrganicBreaks(ServerLevel level, BlockPos center) {
		BlockPos[] shoulderCuts = {
				center.offset(-7, 2, 3),
				center.offset(-6, 3, 1),
				center.offset(6, 2, -4),
				center.offset(7, 1, -2),
				center.offset(3, 4, 2),
				center.offset(-4, 4, -1),
				center.offset(-1, 5, 2),
				center.offset(2, 5, -1)
		};
		for (BlockPos cut : shoulderCuts) {
			safeSet(level, cut, Blocks.AIR);
		}

		BlockPos[] vents = {
				center.offset(3, 2, -3),
				center.offset(-3, 2, 2),
				center.offset(1, 3, 4)
		};
		for (BlockPos vent : vents) {
			safeSet(level, vent, Blocks.AIR);
			safeSet(level, vent.above(), Blocks.AIR);
			safeSet(level, vent.below(), Blocks.COARSE_DIRT);
			safeSet(level, vent.north(), Blocks.ROOTED_DIRT);
			safeSet(level, vent.south(), Blocks.MUDDY_MANGROVE_ROOTS);
		}

		BlockPos[] buttresses = {
				center.offset(-9, 1, 5),
				center.offset(-8, 1, 6),
				center.offset(9, 1, -3),
				center.offset(8, 2, -2),
				center.offset(-6, 1, -7),
				center.offset(6, 1, -7)
		};
		for (BlockPos buttress : buttresses) {
			safeSet(level, buttress, Blocks.MANGROVE_ROOTS);
		}
	}

	private static void placeQueenMoundDiagonalClefts(ServerLevel level, BlockPos center) {
		for (int side : new int[] {-1, 1}) {
			BlockPos[] floors = {
					center.offset(side * 5, 0, -5),
					center.offset(side * 6, 0, -4),
					center.offset(side * 7, 0, -3)
			};
			Block[] floorBlocks = {
					Blocks.DIRT_PATH,
					Blocks.MUD,
					Blocks.COARSE_DIRT
			};
			for (int i = 0; i < floors.length; i++) {
				BlockPos floor = floors[i];
				safeSet(level, floor, floorBlocks[i]);
				safeSet(level, floor.above(), Blocks.AIR);
				safeSet(level, floor.above(2), Blocks.AIR);
			}
			safeSet(level, center.offset(side * 4, 1, -5), Blocks.MANGROVE_ROOTS);
			safeSet(level, center.offset(side * 6, 2, -5), Blocks.MUDDY_MANGROVE_ROOTS);
			safeSet(level, center.offset(side * 8, 1, -3), Blocks.ROOTED_DIRT);
		}
	}

	private static void placeQueenMoundVentChimneys(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block accent = culturePrimaryBlock(culture);
		BlockPos[] vents = {
				center.offset(-5, 1, 6),
				center.offset(4, 1, 6)
		};
		for (BlockPos vent : vents) {
			safeSet(level, vent.below(), Blocks.COARSE_DIRT);
			safeSet(level, vent, Blocks.AIR);
			safeSet(level, vent.above(), Blocks.AIR);
			safeSet(level, vent.north(), Blocks.MANGROVE_ROOTS);
			safeSet(level, vent.south(), Blocks.ROOTED_DIRT);
			safeSet(level, vent.east(), Blocks.MUDDY_MANGROVE_ROOTS);
			safeSet(level, vent.west(), accent);
		}
		safeSet(level, center.offset(-6, 0, 7), Blocks.PODZOL);
		safeSet(level, center.offset(-4, 0, 7), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(3, 0, 7), Blocks.MUD);
		safeSet(level, center.offset(5, 0, 6), Blocks.COARSE_DIRT);
	}

	private static void placeQueenMoundRearVentApron(ServerLevel level, BlockPos center) {
		safeSet(level, center.offset(0, 1, 8), Blocks.AIR);
		safeSet(level, center.offset(0, 2, 8), Blocks.AIR);
		safeSet(level, center.offset(-1, 1, 8), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(1, 1, 8), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(0, 3, 8), Blocks.MANGROVE_ROOTS);

		BlockPos[] trail = {
				center.offset(0, 0, 9),
				center.offset(0, 0, 10),
				center.offset(0, 0, 11)
		};
		for (BlockPos path : trail) {
			safeSet(level, path, Blocks.DIRT_PATH);
			safeSet(level, path.above(), Blocks.AIR);
			safeSet(level, path.above(2), Blocks.AIR);
		}

		safeSet(level, center.offset(-1, 0, 9), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(1, 0, 9), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-1, 0, 10), Blocks.MUD);
		safeSet(level, center.offset(1, 0, 10), Blocks.PODZOL);
		safeSet(level, center.offset(-2, 1, 9), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 1, 9), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(-2, 0, 11), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(2, 0, 11), Blocks.COARSE_DIRT);
	}

	private static void placeQueenMoundTunnelMouthRims(ServerLevel level, BlockPos center) {
		safeSet(level, center.offset(-2, 1, -6), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 1, -6), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(-2, 2, -6), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(2, 2, -6), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(0, 3, -6), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-1, 0, -4), Blocks.MUD);
		safeSet(level, center.offset(0, 0, -4), Blocks.PACKED_MUD);
		safeSet(level, center.offset(1, 0, -4), Blocks.MUD);

		safeSet(level, center.offset(8, 0, -1), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(8, 0, 2), Blocks.MUD);
		safeSet(level, center.offset(8, 1, -1), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(8, 1, 2), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(8, 2, 2), Blocks.MUDDY_MANGROVE_ROOTS);

		safeSet(level, center.offset(-1, 2, 8), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(1, 2, 8), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(0, 3, 9), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-1, 0, 11), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(1, 0, 11), Blocks.COARSE_DIRT);
	}

	private static void placeQueenMoundCrownTerraces(ServerLevel level, BlockPos center) {
		safeSet(level, center.offset(0, 5, 0), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(0, 6, 0), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-2, 5, -1), Blocks.MUDDY_MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 5, 1), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(0, 5, 2), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(-3, 4, 1), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(3, 4, -1), Blocks.ROOTED_DIRT);
	}

	private static void placeQueenMoundCultureMarkers(ServerLevel level, BlockPos center, ColonyCulture culture) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		safeSet(level, center.offset(-5, 1, 4), primary);
		safeSet(level, center.offset(5, 1, 4), primary);
		safeSet(level, center.offset(0, 0, 8), secondary);
		safeSet(level, center.offset(0, 4, 0), secondary);
		if (culture == ColonyCulture.LEAFCUTTER) {
			safeSet(level, center.offset(-4, 1, 5), Blocks.MOSS_BLOCK);
			safeSet(level, center.offset(4, 1, 5), Blocks.BROWN_MUSHROOM_BLOCK);
			safeSet(level, center.offset(-6, 1, 3), Blocks.MOSS_BLOCK);
			safeSet(level, center.offset(-6, 2, 3), Blocks.BROWN_MUSHROOM_BLOCK);
			safeSet(level, center.offset(6, 2, 3), Blocks.MOSS_BLOCK);
		} else if (culture == ColonyCulture.FIRE) {
			safeSet(level, center.offset(-4, 1, 5), Blocks.BLACKSTONE);
			safeSet(level, center.offset(4, 1, 5), Blocks.RED_TERRACOTTA);
			safeSet(level, center.offset(-6, 1, 3), Blocks.BLACKSTONE);
			safeSet(level, center.offset(-6, 2, 3), Blocks.RED_TERRACOTTA);
			safeSet(level, center.offset(6, 2, 3), Blocks.POLISHED_BLACKSTONE);
		} else if (culture == ColonyCulture.CARPENTER) {
			safeSet(level, center.offset(-4, 1, 5), Blocks.MANGROVE_ROOTS);
			safeSet(level, center.offset(4, 1, 5), Blocks.HONEYCOMB_BLOCK);
			safeSet(level, center.offset(-6, 1, 3), Blocks.MANGROVE_PLANKS);
			safeSet(level, center.offset(-6, 2, 3), Blocks.MANGROVE_ROOTS);
			safeSet(level, center.offset(6, 2, 3), Blocks.HONEYCOMB_BLOCK);
		} else {
			safeSet(level, center.offset(-4, 1, 5), Blocks.CHISELED_TUFF);
			safeSet(level, center.offset(4, 1, 5), Blocks.AMETHYST_BLOCK);
			safeSet(level, center.offset(-6, 1, 3), Blocks.HONEY_BLOCK);
			safeSet(level, center.offset(-6, 2, 3), Blocks.HONEYCOMB_BLOCK);
			safeSet(level, center.offset(6, 2, 3), Blocks.AMETHYST_BLOCK);
		}
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type) {
		placeCampusBuilding(level, center, type, ColonyCulture.AMBER);
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		Block core = coreBlock(type);
		for (int x = -9; x <= 9; x++) {
			for (int z = -9; z <= 9; z++) {
				int score = sideChamberScore(type, x, z);
				boolean floor = score <= sideChamberFloorLimit(type);
				boolean skirt = !floor && score <= sideChamberSkirtLimit(type);
				if (floor) {
					safeSet(level, center.offset(x, 0, z), sideChamberFloorBlock(type, culture, x, z, score));
				} else if (skirt) {
					safeSet(level, center.offset(x, 0, z), sideChamberSkirtBlock(culture, x, z, score));
				}
				for (int y = 1; y <= 5; y++) {
					BlockPos pos = center.offset(x, y, z);
					if (sideChamberEntrance(x, z, y) || sideChamberInterior(x, z, y)) {
						safeSet(level, pos, Blocks.AIR);
					} else if (floor && score <= sideChamberLayerLimit(type, y)) {
						safeSet(level, pos, sideChamberWallBlock(type, culture, x, z, y, score));
					} else {
						safeSet(level, pos, Blocks.AIR);
					}
				}
			}
		}
		safeSet(level, center, core);
		placeSideChamberDetails(level, center, type, culture);
		placeSideChamberSilhouetteBreaks(level, center, type, culture);
		placeSideChamberVillageYard(level, center, type, culture);
		safeSet(level, center.above(3), Blocks.OCHRE_FROGLIGHT);
		if (type == BuildingType.MARKET) {
			placeColonyLedger(level, center.offset(1, 1, 1));
		}
	}

	private static int sideChamberScore(BuildingType type, int x, int z) {
		return x * x * 3 + z * z * 2 + Math.floorMod(x * 7 + z * 11 + type.ordinal() * 3, 7);
	}

	private static int sideChamberFloorLimit(BuildingType type) {
		return switch (type) {
			case MINE, BARRACKS, ARMORY, VENOM_PRESS -> 262;
			case MARKET, PHEROMONE_ARCHIVE, DIPLOMACY_SHRINE -> 250;
			default -> 238;
		};
	}

	private static int sideChamberSkirtLimit(BuildingType type) {
		return sideChamberFloorLimit(type) + 78;
	}

	private static int sideChamberLayerLimit(BuildingType type, int y) {
		int bonus = type == BuildingType.MINE || type == BuildingType.BARRACKS || type == BuildingType.ARMORY ? 14 : 0;
		return switch (y) {
			case 1 -> 232 + bonus;
			case 2 -> 168 + bonus;
			case 3 -> 98 + bonus;
			case 4 -> 42 + bonus / 2;
			default -> -1;
		};
	}

	private static boolean sideChamberEntrance(int x, int z, int y) {
		return y <= 3 && (Math.abs(x) == 9 && Math.abs(z) <= 1 || Math.abs(z) == 9 && Math.abs(x) <= 1);
	}

	private static boolean sideChamberInterior(int x, int z, int y) {
		return y <= 3 && Math.abs(x) <= 4 && Math.abs(z) <= 4 && Math.abs(x) + Math.abs(z) <= 6;
	}

	private static Block sideChamberFloorBlock(BuildingType type, ColonyCulture culture, int x, int z, int score) {
		if (x == 0 && z == 0) {
			return coreBlock(type);
		}
		if (Math.abs(x) == 9 && Math.abs(z) <= 1 || Math.abs(z) == 9 && Math.abs(x) <= 1) {
			return Blocks.DIRT_PATH;
		}
		if (Math.abs(x) + Math.abs(z) <= 2) {
			return Blocks.PACKED_MUD;
		}
		if (Math.floorMod(score, 11) == 0) {
			return cultureAccentBlock(type, culture);
		}
		if (Math.floorMod(x - z, 5) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		return cultureShellBlock(type, culture);
	}

	private static Block sideChamberSkirtBlock(ColonyCulture culture, int x, int z, int score) {
		if (Math.abs(x) == 9 && Math.abs(z) <= 1 || Math.abs(z) == 9 && Math.abs(x) <= 1) {
			return Blocks.DIRT_PATH;
		}
		if (culture == ColonyCulture.LEAFCUTTER && Math.floorMod(score, 5) == 0) {
			return Blocks.MOSS_BLOCK;
		}
		if (culture == ColonyCulture.FIRE && Math.floorMod(x - z, 4) == 0) {
			return Blocks.RED_TERRACOTTA;
		}
		if (culture == ColonyCulture.CARPENTER && Math.floorMod(x + z, 4) == 0) {
			return Blocks.MANGROVE_PLANKS;
		}
		if (Math.floorMod(x + z, 4) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		return score % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.PODZOL;
	}

	private static Block sideChamberWallBlock(BuildingType type, ColonyCulture culture, int x, int z, int y, int score) {
		if (Math.floorMod(score + y, 7) == 0) {
			return cultureAccentBlock(type, culture);
		}
		if (y == 1 && Math.floorMod(x - z + y, 8) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		if (y >= 2 && Math.floorMod(x + z + y, 6) == 0) {
			return Blocks.MANGROVE_ROOTS;
		}
		return cultureShellBlock(type, culture);
	}

	private static void placeSideChamberDetails(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		Block accent = cultureAccentBlock(type, culture);
		for (BlockPos mouth : new BlockPos[] {
				center.offset(9, 0, 0),
				center.offset(-9, 0, 0),
				center.offset(0, 0, 9),
				center.offset(0, 0, -9)
		}) {
			safeSet(level, mouth, Blocks.DIRT_PATH);
			safeSet(level, mouth.above(), Blocks.AIR);
			safeSet(level, mouth.above(2), Blocks.AIR);
		}
		safeSet(level, center.offset(-4, 1, -2), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(4, 1, 2), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(-2, 2, 3), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(2, 2, -3), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-3, 1, 3), accent);
		safeSet(level, center.offset(3, 1, -3), accent);
		placeSideChamberTypeMarkers(level, center, type);
		placeSideChamberCultureMotif(level, center, culture);
	}

	private static void placeSideChamberVillageYard(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		Block accent = cultureAccentBlock(type, culture);
		for (int side = -1; side <= 1; side += 2) {
			for (int step = 8; step <= 13; step++) {
				safeSet(level, center.offset(side * step, 0, 0), Blocks.DIRT_PATH);
				safeSet(level, center.offset(0, 0, side * step), Blocks.DIRT_PATH);
				if (step % 2 == 0) {
					safeSet(level, center.offset(side * step, 0, 2), Blocks.COARSE_DIRT);
					safeSet(level, center.offset(2, 0, side * step), Blocks.PODZOL);
				}
			}
		}
		switch (type) {
			case FOOD_STORE -> {
				placeLowMarker(level, center.offset(-7, 1, 7), ModBlocks.FOOD_NODE, Blocks.BROWN_MUSHROOM_BLOCK);
				placeLowMarker(level, center.offset(7, 1, -6), Blocks.HAY_BLOCK, Blocks.COMPOSTER);
			}
			case NURSERY -> {
				placeLowMarker(level, center.offset(7, 1, -7), ModBlocks.CHITIN_NODE, Blocks.HONEYCOMB_BLOCK);
				placeLowMarker(level, center.offset(-7, 1, 6), Blocks.BONE_BLOCK, Blocks.OCHRE_FROGLIGHT);
			}
			case MINE -> {
				placeLowMarker(level, center.offset(-8, 1, 6), ModBlocks.ORE_NODE, Blocks.IRON_ORE);
				placeLowMarker(level, center.offset(7, 1, 7), Blocks.COBBLED_DEEPSLATE_WALL, Blocks.DEEPSLATE_IRON_ORE);
			}
			case BARRACKS, ARMORY -> {
				placeLowMarker(level, center.offset(-8, 1, -7), Blocks.BONE_BLOCK, Blocks.POLISHED_DEEPSLATE);
				placeLowMarker(level, center.offset(8, 1, -7), Blocks.COBBLED_DEEPSLATE_WALL, Blocks.OAK_FENCE);
			}
			case MARKET, TRADE_HUB -> {
				placeLowMarker(level, center.offset(-8, 1, 6), Blocks.BARREL, accent);
				placeLowMarker(level, center.offset(8, 1, 6), Blocks.BELL, Blocks.GOLD_BLOCK);
			}
			case PHEROMONE_ARCHIVE -> {
				placeLowMarker(level, center.offset(-8, 1, -6), Blocks.CHISELED_TUFF, Blocks.AMETHYST_BLOCK);
				placeLowMarker(level, center.offset(8, 1, 6), Blocks.CANDLE, accent);
			}
			case RESIN_DEPOT -> {
				placeLowMarker(level, center.offset(-8, 1, 6), Blocks.HONEY_BLOCK, Blocks.HONEYCOMB_BLOCK);
				placeLowMarker(level, center.offset(8, 1, -6), Blocks.BARREL, accent);
			}
			case VENOM_PRESS -> {
				placeLowMarker(level, center.offset(-8, 1, 6), Blocks.SLIME_BLOCK, Blocks.POLISHED_BLACKSTONE);
				placeLowMarker(level, center.offset(8, 1, -6), Blocks.CAULDRON, accent);
			}
			case DIPLOMACY_SHRINE -> {
				placeLowMarker(level, center.offset(-8, 1, 6), Blocks.CANDLE, Blocks.CHISELED_TUFF);
				placeLowMarker(level, center.offset(8, 1, -6), Blocks.AMETHYST_BLOCK, accent);
			}
			default -> {
			}
		}
	}

	private static void placeLowMarker(ServerLevel level, BlockPos base, Block lower, Block upper) {
		safeSet(level, base, lower);
		safeSet(level, base.above(), upper);
	}

	private static void placeSideChamberCultureMotif(ServerLevel level, BlockPos center, ColonyCulture culture) {
		switch (culture) {
			case AMBER -> {
				safeSet(level, center.offset(4, 1, 1), Blocks.HONEY_BLOCK);
				safeSet(level, center.offset(4, 2, 1), Blocks.HONEYCOMB_BLOCK);
			}
			case LEAFCUTTER -> {
				safeSet(level, center.offset(4, 1, 1), Blocks.MOSS_BLOCK);
				safeSet(level, center.offset(4, 2, 1), Blocks.BROWN_MUSHROOM_BLOCK);
			}
			case FIRE -> {
				safeSet(level, center.offset(4, 1, 1), Blocks.BLACKSTONE);
				safeSet(level, center.offset(4, 2, 1), Blocks.RED_TERRACOTTA);
			}
			case CARPENTER -> {
				safeSet(level, center.offset(4, 1, 1), Blocks.MANGROVE_PLANKS);
				safeSet(level, center.offset(4, 2, 1), Blocks.MANGROVE_ROOTS);
			}
		}
	}

	private static void placeSideChamberSilhouetteBreaks(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		Block accent = cultureAccentBlock(type, culture);
		carveSideChamberNotch(level, center.offset(4, 1, 3), 3);
		carveSideChamberNotch(level, center.offset(-4, 1, -3), 2);
		safeSet(level, center.offset(5, 1, 2), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(-5, 1, -2), Blocks.ROOTED_DIRT);

		switch (type) {
			case FOOD_STORE -> {
				carveSideChamberNotch(level, center.offset(3, 1, 4), 2);
				safeSet(level, center.offset(-3, 1, 4), Blocks.BROWN_MUSHROOM_BLOCK);
				safeSet(level, center.offset(3, 2, -3), Blocks.RED_MUSHROOM_BLOCK);
			}
			case NURSERY -> {
				carveSideChamberNotch(level, center.offset(-4, 1, 3), 2);
				safeSet(level, center.offset(4, 1, -3), Blocks.BONE_BLOCK);
				safeSet(level, center.offset(-3, 2, -3), Blocks.HONEYCOMB_BLOCK);
			}
			case MINE -> {
				carveSideChamberNotch(level, center.offset(2, 1, 5), 3);
				carveSideChamberNotch(level, center.offset(-2, 1, 5), 2);
				safeSet(level, center.offset(-5, 1, 2), Blocks.COBBLED_DEEPSLATE_WALL);
				safeSet(level, center.offset(3, 1, 4), Blocks.IRON_ORE);
			}
			case BARRACKS, ARMORY -> {
				carveSideChamberNotch(level, center.offset(3, 1, -4), 3);
				carveSideChamberNotch(level, center.offset(4, 1, -3), 2);
				carveSideChamberNotch(level, center.offset(2, 1, -5), 2);
				carveSideChamberNotch(level, center.offset(4, 1, -5), 2);
				carveSideChamberNotch(level, center.offset(-4, 2, -5), 2);
				safeSet(level, center.offset(-5, 1, -1), Blocks.BONE_BLOCK);
				safeSet(level, center.offset(5, 1, -1), Blocks.POLISHED_DEEPSLATE);
				safeSet(level, center.offset(-3, 1, -5), Blocks.COBBLED_DEEPSLATE_WALL);
				safeSet(level, center.offset(3, 0, -5), Blocks.DIRT_PATH);
			}
			case MARKET -> {
				carveSideChamberNotch(level, center.offset(-3, 1, 4), 2);
				safeSet(level, center.offset(5, 1, -2), Blocks.OCHRE_FROGLIGHT);
				safeSet(level, center.offset(-5, 1, 2), accent);
			}
			case RESIN_DEPOT -> {
				safeSet(level, center.offset(4, 1, -3), Blocks.HONEY_BLOCK);
				safeSet(level, center.offset(-4, 2, 3), Blocks.HONEYCOMB_BLOCK);
			}
			case PHEROMONE_ARCHIVE -> {
				safeSet(level, center.offset(4, 1, -3), Blocks.AMETHYST_BLOCK);
				safeSet(level, center.offset(-4, 2, 3), Blocks.CHISELED_TUFF);
			}
			case VENOM_PRESS -> {
				carveSideChamberNotch(level, center.offset(-3, 1, 4), 2);
				safeSet(level, center.offset(4, 1, -3), Blocks.SLIME_BLOCK);
				safeSet(level, center.offset(-5, 1, 2), Blocks.POLISHED_BLACKSTONE);
			}
			case DIPLOMACY_SHRINE -> {
				safeSet(level, center.offset(4, 1, -3), Blocks.CHISELED_TUFF);
				safeSet(level, center.offset(-4, 1, 3), Blocks.CANDLE);
			}
			default -> {
			}
		}
	}

	private static void carveSideChamberNotch(ServerLevel level, BlockPos base, int height) {
		for (int y = 0; y < height; y++) {
			safeSet(level, base.above(y), Blocks.AIR);
		}
	}

	private static void placeSideChamberTypeMarkers(ServerLevel level, BlockPos center, BuildingType type) {
		switch (type) {
			case FOOD_STORE -> {
				safeSet(level, center.offset(-2, 1, 1), ModBlocks.FOOD_NODE);
				safeSet(level, center.offset(2, 1, -1), Blocks.BROWN_MUSHROOM_BLOCK);
			}
			case NURSERY -> {
				safeSet(level, center.offset(-2, 1, 1), ModBlocks.CHITIN_NODE);
				safeSet(level, center.offset(2, 1, -1), Blocks.BONE_BLOCK);
			}
			case MINE -> {
				safeSet(level, center.offset(-2, 1, 1), ModBlocks.ORE_NODE);
				safeSet(level, center.offset(2, 1, -1), Blocks.DEEPSLATE_IRON_ORE);
			}
			case BARRACKS, ARMORY -> {
				safeSet(level, center.offset(-2, 1, 1), Blocks.BONE_BLOCK);
				safeSet(level, center.offset(2, 1, -1), Blocks.POLISHED_DEEPSLATE);
			}
			case RESIN_DEPOT -> safeSet(level, center.offset(-2, 1, 1), Blocks.HONEY_BLOCK);
			case PHEROMONE_ARCHIVE -> safeSet(level, center.offset(-2, 1, 1), Blocks.AMETHYST_BLOCK);
			case VENOM_PRESS -> safeSet(level, center.offset(-2, 1, 1), Blocks.SLIME_BLOCK);
			case MARKET, DIPLOMACY_SHRINE -> safeSet(level, center.offset(-2, 1, 1), Blocks.OCHRE_FROGLIGHT);
			default -> {
			}
		}
	}

	public static void placeStagedBuilding(ServerLevel level, BlockPos center, BuildingType type, BuildingVisualStage stage) {
		placeStagedBuilding(level, center, type, stage, ColonyCulture.AMBER);
	}

	public static void placeStagedBuilding(ServerLevel level, BlockPos center, BuildingType type, BuildingVisualStage stage, ColonyCulture culture) {
		Block core = coreBlock(type);
		Block shell = cultureShellBlock(type, culture);
		Block accent = cultureAccentBlock(type, culture);
		int radius = 7;
		int wallHeight = stage == BuildingVisualStage.PLANNED ? 0 : stage == BuildingVisualStage.REPAIRING ? 4 : 3;
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
				boolean corner = Math.abs(x) == radius && Math.abs(z) == radius;
				boolean entrance = Math.abs(x) <= 2 && z == -radius;
				Block floor = stagedFloorBlock(type, stage, x, z, edge, corner, culture);
				safeSet(level, center.offset(x, 0, z), floor);
				for (int y = 1; y <= 5; y++) {
					BlockPos pos = center.offset(x, y, z);
					if (stage == BuildingVisualStage.PLANNED) {
						safeSet(level, pos, corner && y == 1 ? Blocks.MANGROVE_ROOTS : Blocks.AIR);
					} else if (edge && !entrance && y <= wallHeight && (corner || Math.floorMod(x * 3 + z * 5 + y, 4) != 0)) {
						safeSet(level, pos, Math.floorMod(x + z + y, 3) == 0 ? accent : shell);
					} else if (stage == BuildingVisualStage.REPAIRING && y == 1 && Math.abs(x) + Math.abs(z) <= 2) {
						safeSet(level, pos, Math.floorMod(x - z, 2) == 0 ? Blocks.MANGROVE_ROOTS : Blocks.HONEYCOMB_BLOCK);
					} else {
						safeSet(level, pos, Blocks.AIR);
					}
				}
			}
		}
		if (stage != BuildingVisualStage.PLANNED) {
			safeSet(level, center, core);
			safeSet(level, center.above(3), Blocks.OCHRE_FROGLIGHT);
		}
		if (stage == BuildingVisualStage.CONSTRUCTION || stage == BuildingVisualStage.REPAIRING) {
			safeSet(level, center.offset(-7, 1, -7), Blocks.MANGROVE_ROOTS);
			safeSet(level, center.offset(7, 1, 7), Blocks.MANGROVE_ROOTS);
			safeSet(level, center.offset(-5, 1, 5), Blocks.ROOTED_DIRT);
			safeSet(level, center.offset(5, 1, -5), Blocks.ROOTED_DIRT);
		}
		if (stage == BuildingVisualStage.REPAIRING) {
			safeSet(level, center.offset(-1, 1, -4), Blocks.HONEYCOMB_BLOCK);
			safeSet(level, center.offset(1, 1, 4), Blocks.ROOTED_DIRT);
			safeSet(level, center.offset(-5, 1, -8), Blocks.HONEYCOMB_BLOCK);
			safeSet(level, center.offset(5, 1, -8), Blocks.BONE_BLOCK);
			safeSet(level, center.offset(-5, 2, -7), Blocks.HONEYCOMB_BLOCK);
			safeSet(level, center.offset(5, 2, -7), Blocks.BONE_BLOCK);
			safeSet(level, center.offset(-7, 2, -7), Blocks.OAK_FENCE);
			safeSet(level, center.offset(7, 2, -7), Blocks.OAK_FENCE);
		}
		placeStagedBuildingMarkers(level, center, stage);
	}

	public static void placeColonyLedger(ServerLevel level, BlockPos pos) {
		safeSet(level, pos, ModBlocks.COLONY_LEDGER);
	}

	private static void placeStagedBuildingMarkers(ServerLevel level, BlockPos center, BuildingVisualStage stage) {
		switch (stage) {
			case PLANNED -> {
				placeMarkerBlock(level, center.offset(-3, 1, -7), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(3, 1, -7), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(-7, 1, -3), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(7, 1, -3), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(-7, 1, 3), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(7, 1, 3), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(-3, 1, 7), Blocks.OAK_FENCE);
				placeMarkerBlock(level, center.offset(3, 1, 7), Blocks.OAK_FENCE);
				safeSet(level, center.offset(-5, 0, -8), Blocks.COARSE_DIRT);
				safeSet(level, center.offset(0, 0, -8), Blocks.DIRT_PATH);
				safeSet(level, center.offset(5, 0, -8), Blocks.COARSE_DIRT);
				safeSet(level, center.offset(-8, 0, 0), Blocks.DIRT_PATH);
				safeSet(level, center.offset(8, 0, 0), Blocks.DIRT_PATH);
				safeSet(level, center.offset(0, 0, 8), Blocks.DIRT_PATH);
			}
			case CONSTRUCTION -> {
				safeSet(level, center.offset(-7, 3, -7), Blocks.MANGROVE_ROOTS);
				safeSet(level, center.offset(7, 3, 7), Blocks.MANGROVE_ROOTS);
				safeSet(level, center.offset(-2, 1, -8), Blocks.ROOTED_DIRT);
				safeSet(level, center.offset(2, 1, -8), Blocks.PACKED_MUD);
				safeSet(level, center.offset(0, 0, -8), Blocks.DIRT_PATH);
			}
			case REPAIRING -> {
				safeSet(level, center.offset(-3, 1, -8), Blocks.HONEYCOMB_BLOCK);
				safeSet(level, center.offset(3, 1, -8), Blocks.BONE_BLOCK);
				safeSet(level, center.offset(-1, 1, -8), Blocks.HONEYCOMB_BLOCK);
				safeSet(level, center.offset(1, 1, -8), Blocks.BONE_BLOCK);
				safeSet(level, center.offset(0, 0, -8), Blocks.DIRT_PATH);
				safeSet(level, center.offset(-3, 0, -9), Blocks.DIRT_PATH);
				safeSet(level, center.offset(3, 0, -9), Blocks.DIRT_PATH);
			}
			default -> {
			}
		}
	}

	private static void placeMarkerBlock(ServerLevel level, BlockPos pos, Block block) {
		if (level.getBlockEntity(pos) == null) {
			level.setBlockAndUpdate(pos, block.defaultBlockState());
		}
	}

	private static Block stagedFloorBlock(BuildingType type, BuildingVisualStage stage, int x, int z, boolean edge, boolean corner, ColonyCulture culture) {
		if (corner) {
			return stage == BuildingVisualStage.PLANNED ? Blocks.MANGROVE_ROOTS : Blocks.ROOTED_DIRT;
		}
		if (edge) {
			return stage == BuildingVisualStage.PLANNED ? Blocks.COARSE_DIRT : cultureShellBlock(type, culture);
		}
		if (Math.abs(x) + Math.abs(z) <= 1 && stage != BuildingVisualStage.PLANNED) {
			return coreBlock(type);
		}
		if (stage == BuildingVisualStage.REPAIRING && Math.floorMod(x + z, 4) == 0) {
			return Blocks.HONEYCOMB_BLOCK;
		}
		return stage == BuildingVisualStage.PLANNED ? Blocks.DIRT_PATH : Blocks.PACKED_MUD;
	}

	private static void placeCompleteOverlay(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		if (type == BuildingType.QUEEN_CHAMBER || type == BuildingType.GREAT_MOUND || type == BuildingType.QUEEN_VAULT || type == BuildingType.TRADE_HUB) {
			return;
		}
		Block accent = cultureAccentBlock(type, culture);
		for (int x = -2; x <= 2; x++) {
			safeSet(level, center.offset(x, 0, -8), Blocks.DIRT_PATH);
		}
		safeSet(level, center.offset(-2, 1, -8), accent);
		safeSet(level, center.offset(2, 1, -8), accent);
	}

	private static void placeUpgradeOverlay(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		Block accent = cultureAccentBlock(type, culture);
		for (BlockPos pos : new BlockPos[] {
				center.offset(-8, 0, 0),
				center.offset(8, 0, 0),
				center.offset(0, 0, -8),
				center.offset(0, 0, 8)
		}) {
			safeSet(level, pos, Blocks.ROOTED_DIRT);
			safeSet(level, pos.above(), accent);
		}
		safeSet(level, center.above(6), accent);
		safeSet(level, center.offset(-5, 4, -5), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(5, 4, 5), Blocks.MANGROVE_ROOTS);
	}

	private static void placeDamagedOverlay(ServerLevel level, BlockPos center, BuildingType type) {
		safeSet(level, center.offset(-7, 4, -7), Blocks.AIR);
		safeSet(level, center.offset(-7, 5, -7), Blocks.AIR);
		safeSet(level, center.offset(-6, 3, -7), Blocks.AIR);
		safeSet(level, center.offset(-6, 4, -7), Blocks.AIR);
		safeSet(level, center.offset(-7, 3, -6), Blocks.AIR);
		safeSet(level, center.offset(-5, 2, -7), Blocks.AIR);
		safeSet(level, center.offset(-6, 2, -7), Blocks.AIR);
		safeSet(level, center.offset(6, 3, 7), Blocks.AIR);
		safeSet(level, center.offset(-5, 1, -8), Blocks.RED_TERRACOTTA);
		safeSet(level, center.offset(-3, 1, -8), Blocks.RED_TERRACOTTA);
		safeSet(level, center.offset(-1, 1, -8), Blocks.BLACKSTONE);
		safeSet(level, center.offset(1, 1, -8), Blocks.RED_TERRACOTTA);
		safeSet(level, center.offset(3, 1, -8), Blocks.BLACKSTONE);
		safeSet(level, center.offset(5, 1, -8), Blocks.BLACKSTONE);
		safeSet(level, center.offset(-1, 2, -8), Blocks.RED_TERRACOTTA);
		safeSet(level, center.offset(1, 2, -8), Blocks.BLACKSTONE);
		safeSet(level, center.offset(0, 1, -7), Blocks.BLACKSTONE);
		safeSet(level, center.offset(0, 0, -8), Blocks.GRAVEL);
		safeSet(level, center.offset(-2, 0, -9), Blocks.GRAVEL);
		safeSet(level, center.offset(2, 0, -9), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(-4, 0, 5), Blocks.GRAVEL);
		safeSet(level, center.offset(4, 1, 4), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(0, 1, -5), type == BuildingType.MINE ? Blocks.COBBLED_DEEPSLATE : Blocks.ROOTED_DIRT);
	}

	private static void placeChitinFarm(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -9; x <= 9; x++) {
			for (int z = -6; z <= 6; z++) {
				BlockPos pos = center.offset(x, 0, z);
				if (Math.abs(x) == 9 || Math.abs(z) == 6) {
					safeSet(level, pos, Blocks.ROOTED_DIRT);
				} else {
					safeSet(level, pos, ModBlocks.CHITIN_BED);
				}
			}
		}
		safeSet(level, center.offset(0, 0, -7), ModBlocks.NURSERY_CHAMBER);
		placeCultureSignatureThreshold(level, center, culture, -8);
	}

	private static void placeWatchPost(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (Math.abs(x) + Math.abs(z) <= 4) {
					safeSet(level, center.offset(x, 0, z), Math.abs(x) + Math.abs(z) <= 1 ? Blocks.BONE_BLOCK : Blocks.DIRT_PATH);
				}
			}
		}
		safeSet(level, center, ModBlocks.WATCH_POST);
		safeSet(level, center.above(), Blocks.COBBLED_DEEPSLATE_WALL);
		safeSet(level, center.above(2), Blocks.OCHRE_FROGLIGHT);
		for (BlockPos pos : new BlockPos[] {center.north(3), center.south(3), center.east(3), center.west(3)}) {
			safeSet(level, pos.above(), Blocks.BONE_BLOCK);
		}
		placeCultureSignatureThreshold(level, center, culture, -4);
	}

	private static void placeDiplomacyShrine(ServerLevel level, BlockPos center, ColonyCulture culture) {
		placeCampusBuilding(level, center, BuildingType.DIPLOMACY_SHRINE, culture);
		for (BlockPos pos : new BlockPos[] {center.north(2), center.south(2), center.east(2), center.west(2)}) {
			safeSet(level, pos, Blocks.CHISELED_TUFF);
			safeSet(level, pos.above(), Blocks.CANDLE);
		}
		safeSet(level, center.above(), Blocks.HONEY_BLOCK);
	}

	private static void placeFungusGarden(ServerLevel level, BlockPos center, ColonyCulture culture) {
		for (int x = -8; x <= 8; x++) {
			for (int z = -8; z <= 8; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance <= 12) {
					safeSet(level, center.offset(x, 0, z), distance <= 1 ? ModBlocks.FUNGUS_GARDEN : Blocks.MYCELIUM);
				}
				if (distance == 5 || distance == 9) {
					safeSet(level, center.offset(x, 1, z), (x + z) % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
				}
			}
		}
		safeSet(level, center.above(2), Blocks.OCHRE_FROGLIGHT);
		placeCultureSignatureThreshold(level, center, culture, -10);
	}

	private static void placeCultureSignatureThreshold(ServerLevel level, BlockPos center, ColonyCulture culture, int zOffset) {
		Block primary = culturePrimaryBlock(culture);
		Block secondary = cultureSecondaryBlock(culture);
		for (int x = -1; x <= 1; x++) {
			safeSet(level, center.offset(x, 0, zOffset), Blocks.DIRT_PATH);
		}
		safeSet(level, center.offset(-2, 0, zOffset), primary);
		safeSet(level, center.offset(2, 0, zOffset), primary);
		safeSet(level, center.offset(-2, 1, zOffset), secondary);
		safeSet(level, center.offset(2, 1, zOffset), secondary);
	}

	private static void placeRoadPatch(ServerLevel level, BlockPos center) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				safeSet(level, center.offset(x, 0, z), Blocks.DIRT_PATH);
			}
		}
	}

	private static Block coreBlock(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> ModBlocks.FOOD_CHAMBER;
			case NURSERY -> ModBlocks.NURSERY_CHAMBER;
			case MINE -> ModBlocks.MINE_CHAMBER;
			case BARRACKS -> ModBlocks.BARRACKS_CHAMBER;
			case MARKET -> ModBlocks.MARKET_CHAMBER;
			case DIPLOMACY_SHRINE -> ModBlocks.DIPLOMACY_SHRINE;
			case RESIN_DEPOT -> ModBlocks.RESIN_DEPOT;
			case PHEROMONE_ARCHIVE -> ModBlocks.PHEROMONE_ARCHIVE;
			case VENOM_PRESS -> ModBlocks.VENOM_PRESS;
			case ARMORY -> ModBlocks.ARMORY;
			case TRADE_HUB -> ModBlocks.MARKET_CHAMBER;
			case QUEEN_VAULT -> ModBlocks.NEST_CORE;
			default -> ModBlocks.NEST_MOUND;
		};
	}

	private static Block shellBlock(BuildingType type) {
		return switch (type) {
			case MINE, ARMORY -> Blocks.COBBLED_DEEPSLATE;
			case BARRACKS, VENOM_PRESS -> Blocks.MUD_BRICKS;
			case PHEROMONE_ARCHIVE, DIPLOMACY_SHRINE, QUEEN_VAULT, TRADE_HUB -> Blocks.CHISELED_TUFF;
			case NURSERY -> Blocks.HONEYCOMB_BLOCK;
			default -> Blocks.PACKED_MUD;
		};
	}

	private static Block cultureShellBlock(BuildingType type, ColonyCulture culture) {
		Block fallback = shellBlock(type);
		return switch (culture) {
			case AMBER -> fallback;
			case LEAFCUTTER -> type == BuildingType.MINE || type == BuildingType.ARMORY ? fallback : Blocks.MOSS_BLOCK;
			case FIRE -> type == BuildingType.MINE || type == BuildingType.ARMORY ? Blocks.BLACKSTONE : Blocks.RED_TERRACOTTA;
			case CARPENTER -> type == BuildingType.MINE || type == BuildingType.ARMORY ? Blocks.MANGROVE_PLANKS : Blocks.MANGROVE_PLANKS;
		};
	}

	private static Block accentBlock(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> Blocks.BROWN_MUSHROOM_BLOCK;
			case NURSERY -> Blocks.BONE_BLOCK;
			case MINE -> Blocks.IRON_ORE;
			case BARRACKS, ARMORY -> Blocks.POLISHED_DEEPSLATE;
			case MARKET -> Blocks.OCHRE_FROGLIGHT;
			case DIPLOMACY_SHRINE, PHEROMONE_ARCHIVE, QUEEN_VAULT, TRADE_HUB -> Blocks.AMETHYST_BLOCK;
			case RESIN_DEPOT -> Blocks.HONEY_BLOCK;
			case VENOM_PRESS -> Blocks.SLIME_BLOCK;
			default -> Blocks.ROOTED_DIRT;
		};
	}

	private static Block cultureAccentBlock(BuildingType type, ColonyCulture culture) {
		return switch (culture) {
			case AMBER -> accentBlock(type);
			case LEAFCUTTER -> type == BuildingType.MINE ? Blocks.IRON_ORE : Blocks.BROWN_MUSHROOM_BLOCK;
			case FIRE -> type == BuildingType.NURSERY ? Blocks.BONE_BLOCK : Blocks.POLISHED_BLACKSTONE;
			case CARPENTER -> type == BuildingType.RESIN_DEPOT ? Blocks.HONEY_BLOCK : Blocks.HONEYCOMB_BLOCK;
		};
	}

	private static Block culturePrimaryBlock(ColonyCulture culture) {
		return switch (culture) {
			case AMBER -> Blocks.HONEYCOMB_BLOCK;
			case LEAFCUTTER -> Blocks.MOSS_BLOCK;
			case FIRE -> Blocks.RED_TERRACOTTA;
			case CARPENTER -> Blocks.MANGROVE_PLANKS;
		};
	}

	private static Block cultureSecondaryBlock(ColonyCulture culture) {
		return switch (culture) {
			case AMBER -> Blocks.AMETHYST_BLOCK;
			case LEAFCUTTER -> Blocks.BROWN_MUSHROOM_BLOCK;
			case FIRE -> Blocks.BLACKSTONE;
			case CARPENTER -> Blocks.HONEYCOMB_BLOCK;
		};
	}
}
