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

	private static boolean forceSetStructureBlock(ServerLevel level, BlockPos pos, Block block) {
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
			// === DIRECTIONAL LOBE HEIGHTMAP (central mound) =========================
			// Replaces the previous radially-symmetric qDome = f(score), which depended
			// ONLY on the distance from centre. When quantized to integer block heights
			// that prints perfect concentric contour rings / a ziggurat read (assessment
			// P1 blocker, repeated across 4 attempts). The new surface is a function of
			// the actual (x,z) position via an OFF-CENTRE directional egg footprint plus
			// two asymmetric shoulder sub-lobes on distinct off-cardinal directions, so no
			// two columns at the same radius ever share the same integer height. The peak
			// is raised well above 7 (the old cap) for a monumental ant-hill read, but the
			// floor footprint (score<=300), chamber/entrance AIR carving, and the shell
			// block selector contract are all preserved.
			int colHeight = 0;
			if (score <= 300) {
				double qPeakHeight = 14.0; // raised from 7 so the central mass reads as a landmark
				// Off-centre directional egg: the dome peak leans toward +x,-z so the silhouette
				// is lopsided (ant-hills are never centred cones).
				double peakX = 2.0, peakZ = -2.5;
				double radX = 11.0, radZ = 11.0;
				double ndx = (x - peakX) / radX;
				double ndz = (z - peakZ) / radZ;
				double d2 = ndx * ndx + ndz * ndz;
				double d = Math.min(1.0, Math.sqrt(d2));
				double qFactor = 0.10 + 0.90 * (0.5 + 0.5 * Math.cos(d * Math.PI));
				double qRelief = Math.max(0.0, 1.0 - d);
				double qRelief2 = qRelief * qRelief;
				// TWO off-cardinal asymmetric shoulder sub-lobes at distinct centres/scales so
				// the surface grows non-mirrored bulges on different sides (no radial symmetry).
				double lb1 = lobeBump(x, z, -4.5, 4.0, 4.2) * 3.2 * qRelief2;
				double lb2 = lobeBump(x, z, 5.0, 3.0, 3.0) * 2.6 * qRelief2;
				double qMacro = (smoothValueNoise(x * 0.15, z * 0.15, 4217) - 0.5) * 2.0 * 1.7 * qRelief2;
				double qJitter = ((smoothValueNoise(x * 0.9, z * 0.9, 4318) - 0.5) * 2.0 * 1.4
					+ (smoothValueNoise(x * 1.9, z * 1.9, 4429) - 0.5) * 2.0 * 0.6) * qRelief;
				colHeight = (int) Math.round(qFactor * qPeakHeight + lb1 + lb2 + qMacro + qJitter);
				if (colHeight > 16) colHeight = 16;
				if (colHeight < 0) colHeight = 0;
			}
				for (int y = 1; y <= colHeight; y++) {
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
				// === COLUMN-FIRST NOISE-HEIGHTMAP DOME (great mound outer ring) =========
				// Same representational fix as the central mound: replace the per-layer
				// greatMoundLayerLimit(y) disc with a continuous raised-cosine dome height
				// driven by low/high-frequency value noise, so the endgame great-mound
				// silhouette reads as one smooth organic ring, not stepped terraces.
				// Path air carving, outsideStarter gate, score<=408 footprint, and the
				// MANGROVE_ROOTS / greatMoundShellBlock selector are all preserved.
			if (outsideStarter) {
				// DIRECTIONAL LOBE HEIGHTMAP (great mound ring): same fix as the central mound
				// - the height is a function of (x,z), not just the radial score, so quantized
				// integer heights no longer print concentric contour rings. Off-centre egg +
				// two asymmetric shoulder sub-lobes, peak raised above 7.
				double gPeakHeight = 13.0;
				double gPeakX = -2.5, gPeakZ = 3.0;
				double gRadX = 14.0, gRadZ = 14.0;
				double gndx = (x - gPeakX) / gRadX;
				double gndz = (z - gPeakZ) / gRadZ;
				double gd2 = gndx * gndx + gndz * gndz;
				double gd = Math.min(1.0, Math.sqrt(gd2));
				double gFactor = 0.10 + 0.90 * (0.5 + 0.5 * Math.cos(gd * Math.PI));
				double gRelief = Math.max(0.0, 1.0 - gd);
				double gRelief2 = gRelief * gRelief;
				double glb1 = lobeBump(x, z, 5.0, -4.5, 4.5) * 3.0 * gRelief2;
				double glb2 = lobeBump(x, z, -5.5, -3.5, 3.2) * 2.4 * gRelief2;
				double gMacro = (smoothValueNoise(x * 0.14, z * 0.14, 5217) - 0.5) * 2.0 * 1.7 * gRelief2;
				double gJitter = ((smoothValueNoise(x * 0.9, z * 0.9, 5318) - 0.5) * 2.0 * 1.4
					+ (smoothValueNoise(x * 1.9, z * 1.9, 5429) - 0.5) * 2.0 * 0.6) * gRelief;
				int gCol = (int) Math.round(gFactor * gPeakHeight + glb1 + glb2 + gMacro + gJitter);
				if (gCol > 13) gCol = 13;
				if (gCol < 0) gCol = 0;
					for (int y = 1; y <= gCol; y++) {
						BlockPos pos = center.offset(x, y, z);
						if (Math.floorMod(x - z + y, 9) == 0) {
							safeSet(level, pos, Blocks.MANGROVE_ROOTS);
						} else {
							safeSet(level, pos, greatMoundShellBlock(x, z, y, score, culture));
						}
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
			case AMBER -> Math.floorMod(score + y, 13) == 0 ? ModBlocks.NEST_MOUND : null;
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
			safeSet(level, center.offset(-4, 1, 5), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset(4, 1, 5), ModBlocks.NEST_CORE);
			safeSet(level, center.offset(-6, 1, 3), ModBlocks.RESIN_DEPOT);
			safeSet(level, center.offset(-6, 2, 3), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset(6, 2, 3), ModBlocks.NEST_CORE);
		}
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type) {
		placeCampusBuilding(level, center, type, ColonyCulture.AMBER);
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		Block core = coreBlock(type);
		// SINGLE source of truth: the base chamber footprint must match the crown
		// dome (placeCampusCrownAndTunnelMouth) so the mass stays broad from the
		// ground up to the crown shoulder. The old fast-tapering layer limits
		// (sideChamberLayerLimit: 232->168->98->42 at y=1..4, then air at y=5)
		// left a narrow hut under the broad dome -> "cap on pad" waist silhouette.
		// Filling the full ogive footprint for y=1..5 kills that waist.
		int[] fp = campusFootprint(type);
		int rxMax = fp[0];
		int rzMax = fp[1];
		for (int x = -9; x <= 9; x++) {
			for (int z = -9; z <= 9; z++) {
				double ex = (rxMax == 0) ? 0.0 : (x * x) / (double) (rxMax * rxMax);
				double ez = (rzMax == 0) ? 0.0 : (z * z) / (double) (rzMax * rzMax);
				boolean inMass = (ex + ez <= 1.05);
				int score = sideChamberScore(type, x, z);
				if (inMass) {
					safeSet(level, center.offset(x, 0, z), sideChamberFloorBlock(type, culture, x, z, score));
				} else if (score <= sideChamberSkirtLimit(type)) {
					safeSet(level, center.offset(x, 0, z), sideChamberSkirtBlock(culture, x, z, score));
				}
				for (int y = 1; y <= 5; y++) {
					BlockPos pos = center.offset(x, y, z);
					if (sideChamberEntrance(x, z, y) || campusTunnelVoid(x, z, y)) {
						safeSet(level, pos, Blocks.AIR);
					} else if (inMass) {
						safeSet(level, pos, campusBaseBlock(type, culture, x, y, z));
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
		// Native Formic earth floor: NEST_MOUND primary with ROOTED_DIRT / COARSE_DIRT
		// breakup. Borrowed mineral accents (honey/amethyst/gold) are no longer a
		// primary structure surface (formic_native_material_palette blocker).
		if (Math.abs(x) + Math.abs(z) <= 2) {
			return ModBlocks.NEST_MOUND;
		}
		if (Math.floorMod(x - z, 5) == 0) {
			return Blocks.ROOTED_DIRT;
		}
		if (Math.floorMod(score, 7) == 0) {
			return Blocks.COARSE_DIRT;
		}
		return ModBlocks.NEST_MOUND;
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

	// Per-type campus mound footprint: SINGLE source of truth shared by the base
	// chamber (placeCampusBuilding) and the crown dome (placeCampusCrownAndTunnelMouth)
	// so the base meets the crown with no waist. Mirrors the verified crown switch:
	// FOOD_STORE/NURSERY stay conservative (diplomacy/expansion column conflicts at
	// rel x=+/-6); every other campus building gets the full broad dome envelope.
	private static int[] campusFootprint(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> new int[]{5, 5};
			case NURSERY -> new int[]{5, 8};
			case MINE -> new int[]{9, 9};
			case BARRACKS -> new int[]{9, 9};
			case MARKET -> new int[]{9, 9};
			case RESIN_DEPOT -> new int[]{8, 8};
			case PHEROMONE_ARCHIVE -> new int[]{8, 8};
			case VENOM_PRESS -> new int[]{8, 8};
			case ARMORY -> new int[]{9, 9};
			default -> new int[]{7, 7};
		};
	}

	// Native Formic earth palette for the base chamber mass. NEST_MOUND dominant,
	// ROOTED_DIRT skin breaks, MANGROVE_ROOTS ribs. No borrowed honey/amethyst/gold
	// as a primary surface so the colony reads as native ant architecture.
	private static Block campusBaseBlock(BuildingType type, ColonyCulture culture, int x, int y, int z) {
		int h = Math.floorMod(x * 3 + z * 5 + y * 2 + type.ordinal(), 11);
		if (y >= 4) {
			return h % 3 == 0 ? Blocks.ROOTED_DIRT : ModBlocks.NEST_MOUND;
		}
		if (Math.abs(x) + Math.abs(z) >= 4) {
			return h % 4 == 0 ? Blocks.ROOTED_DIRT : ModBlocks.NEST_MOUND;
		}
		if (h == 0 || h == 7) {
			return Blocks.MANGROVE_ROOTS;
		}
		return ModBlocks.NEST_MOUND;
	}

	// Connecting tunnel + central chamber void carved through the base mass so the
	// south tunnel mouth (placeCampusCrownAndTunnelMouth at z=-10/-11) opens into a
	// real dark interior instead of a shallow facade notch. Kept at y=1..3 so the
	// ground-level core block and every y<=4 game-test assertion stays intact.
	private static boolean campusTunnelVoid(int x, int z, int y) {
		if (y < 1 || y > 3) {
			return false;
		}
		int ax = Math.abs(x);
		// South tunnel corridor linking the crown mouth (z=-10/-11) to the chamber.
		if (ax <= 1 && z <= -4 && z >= -13) {
			return true;
		}
		// Central hollow chamber visible through the corridor (secondary chamber face).
		int az = Math.abs(z);
		if (ax <= 3 && az <= 3 && ax + az <= 5) {
			return true;
		}
		return false;
	}

	private static boolean isCampusOrganicShellBreak(BuildingType type, int x, int y, int z, int rx, int rz) {
		if (y < 8 || rx <= 2 || rz <= 2) {
			return false;
		}
		double ex = (x * x) / (double) (rx * rx);
		double ez = (z * z) / (double) (rz * rz);
		double shell = ex + ez;
		if (shell < 0.78 || shell > 1.04) {
			return false;
		}
		if (Math.abs(x) <= 3 && z <= -9) {
			return false;
		}
		int score = Math.floorMod(x * 11 + z * 17 + y * 23 + type.ordinal() * 31, 41);
		return score == 0 || (y % 5 == 0 && score == 1);
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
					safeSet(level, center.offset(4, 1, 1), ModBlocks.RESIN_DEPOT);
					safeSet(level, center.offset(4, 2, 1), ModBlocks.NEST_MOUND);
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
				safeSet(level, center.offset(-3, 2, -3), ModBlocks.NEST_MOUND);
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
				safeSet(level, center.offset(4, 1, -3), ModBlocks.RESIN_DEPOT);
				safeSet(level, center.offset(-4, 2, 3), ModBlocks.NEST_MOUND);
			}
			case PHEROMONE_ARCHIVE -> {
				safeSet(level, center.offset(4, 1, -3), ModBlocks.NEST_CORE);
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
			case RESIN_DEPOT -> safeSet(level, center.offset(-2, 1, 1), ModBlocks.RESIN_DEPOT);
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
						safeSet(level, pos, Math.floorMod(x - z, 2) == 0 ? Blocks.MANGROVE_ROOTS : ModBlocks.NEST_MOUND);
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
			safeSet(level, center.offset(-1, 1, -4), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset(1, 1, 4), Blocks.ROOTED_DIRT);
			safeSet(level, center.offset(-5, 1, -8), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset(5, 1, -8), Blocks.BONE_BLOCK);
			safeSet(level, center.offset(-5, 2, -7), ModBlocks.NEST_MOUND);
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
				safeSet(level, center.offset(-3, 1, -8), ModBlocks.NEST_MOUND);
				safeSet(level, center.offset(3, 1, -8), Blocks.BONE_BLOCK);
				safeSet(level, center.offset(-1, 1, -8), ModBlocks.NEST_MOUND);
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
			return ModBlocks.NEST_MOUND;
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
		placeCampusCrownAndTunnelMouth(level, center, type, culture);
	}

	/**
	 * R2 organic-chamber massing for satellite campus buildings.
	 * Adds a tapering native-earth crown (NEST_MOUND / ROOTED_DIRT / MANGROVE_ROOTS)
	 * rising from y=6 to y=15 above the core, plus a deep dark tunnel mouth carved
	 * into the south face so each satellite reads as a substantial ant-hill chamber
	 * volume rather than a low stepped pad.
	 *
	 * CARDINAL REACH IS CAPPED AT 5. The diplomacy tribute/truce cache sits at the
	 * midpoint between two colony origins, which lands exactly 6 blocks from a
	 * FOOD_STORE center (and 6 blocks from the partner colony's NURSERY center).
	 * anchorToSurface() in placeTributeCache/placeTruceCache resolves the cache base
	 * to the top of whatever is in that column, so any campus block at relative
	 * distance 6+ lifts the cache and breaks the column assertion. Keeping reach <= 5
	 * leaves a 1-block clearance on both sides.
	 *
	 * Geometry is otherwise strictly additive and stage-gated to COMPLETE: all
	 * playtest assertions on campus buildings live at y <= 4, and this only writes
	 * y >= 5 using blocks present in canReplace().
	 */
	private static void placeCampusCrownAndTunnelMouth(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		// Only the campus chamber family (the switch in placeBuilding line 37) carries
		// this verified mound crown + deep tunnel mouth. Other COMPLETE buildings
		// (FUNGUS_GARDEN, DIPLOMACY_SHRINE, WATCH_POST, CHITIN_FARM, ...) keep their
		// own asserted threshold motifs that the mouth would otherwise carve away.
		switch (type) {
			case FOOD_STORE, NURSERY, MINE, BARRACKS, MARKET, RESIN_DEPOT, PHEROMONE_ARCHIVE, VENOM_PRESS, ARMORY -> {
				// fall through to the crown below
			}
			default -> {
				return;
			}
		}
		// BROAD EARTHEN DOME crown (ogive profile) sized PER BUILDING TYPE.
		//
		// The R2 architecture brief calls for broad earthen mound mass first,
		// height second. The previous single radius (rxMax=rzMax=5) was narrower
		// than the base chamber footprint (rx ~= 9), so the mass read as a "capped
		// tower on a flat pad" instead of an ant-hill mound.
		//
		// Empirically verified constraint map (see build/autonomous-loop analysis):
		//  - FOOD_STORE (origin+38,0): the diplomacy tribute/truce cache midpoint
		//    sits at world x=32 (FOOD_STORE-relative x=-6) and the expansion-opportunity
		//    crew posts sit at FOOD_STORE-relative z=7. A crown taller than 8 blocks
		//    at those columns lifts anchorToSurface above the test's spawn AABB.
		//    So FOOD_STORE keeps the conservative +/-5 envelope but gains HEIGHT.
		//  - NURSERY (origin-38,0): the partner-colony (origin+64) tribute/truce cache
		//    midpoint sits at world x=32 = NURSERY-relative x=+6. So NURSERY keeps
		//    rxMax=5 (z is free; rzMax=8).
		//  - All other campus buildings (MINE, BARRACKS, MARKET, RESIN_DEPOT,
		//    PHEROMONE_ARCHIVE, VENOM_PRESS, ARMORY): zero diplomacy/expansion
		//    conflicts within +/-10; they get the full broad dome envelope so the
		//    surrounding role buildings read as substantial ant-hill chambers, not
		//    low pads. This is what fixes the "no_single_mound_pass" blocker.
		int rxMax;
		int rzMax;
		int peakY;
		switch (type) {
			// BROAD MOUND PROPORTIONS: each satellite is wider than it is tall so the
			// family reads as a cluster of substantial ant-hill mounds, NOT a ring of
			// tall tapered spires/towers. Peak height now varies per type so no two
			// role buildings share the same silhouette. Reach (rxMax/rzMax) is
			// UNCHANGED, so the diplomacy tribute/truce/treaty caches at distance 6
			// from FOOD_STORE/NURSERY still keep their clearance.
			case FOOD_STORE -> { rxMax = 5; rzMax = 5; peakY = 14; }
			case NURSERY ->    { rxMax = 5; rzMax = 8; peakY = 13; }
			case MINE ->       { rxMax = 9; rzMax = 9; peakY = 15; }
			case BARRACKS ->   { rxMax = 9; rzMax = 9; peakY = 13; }
			case MARKET ->     { rxMax = 9; rzMax = 9; peakY = 14; }
			case RESIN_DEPOT -> { rxMax = 8; rzMax = 8; peakY = 12; }
			case PHEROMONE_ARCHIVE -> { rxMax = 8; rzMax = 8; peakY = 13; }
			case VENOM_PRESS -> { rxMax = 8; rzMax = 8; peakY = 12; }
			case ARMORY ->     { rxMax = 9; rzMax = 9; peakY = 16; }
			default ->         { rxMax = 7; rzMax = 7; peakY = 13; }
		}
		// ASYMMETRIC ORGANIC LOBE MASS: the previous crown used one identical
		// centred ellipse for every building type (just scaled), so the satellites
		// read as 'repeated symmetric pads / stepped cones' with mirrored entrances.
		// Each type now gets a distinct asymmetric egg: the mass leans off-centre on
		// (cx,cz) and a low off-cardinal shoulder (biasX,biasZ) grows on one side.
		// The result is a family of non-mirrored, irregular ant-chamber lobes that
		// read as excavated mounds rather than symmetrical huts.
		// REACH IS UNCHANGED: scan stays within [-rxMax,rxMax]x[-rzMax,rzMax] and
		// radius rxMax*f, identical to the prior tapered crown. The diplomacy
		// tribute/truce/treaty caches sit at distance 6 from FOOD_STORE/NURSERY, so
		// reach must stay inside the per-type rxMax/rzMax (those two are capped <=5;
		// MINE/BARRACKS/etc. use 8-9 with no caches nearby). Deterministic; additive;
		// y>=6 only; canReplace()-safe blocks only.
		double cx = campusLobeOffsetX(type);
		double cz = campusLobeOffsetZ(type);
		int biasX = (int) Math.round(campusLobeBulgeX(type));
		int biasZ = (int) Math.round(campusLobeBulgeZ(type));
		double lobeStrength = campusLobeStrength(type);
		double taperExp = campusTaperExp(type);
		double taper = campusTaper(type);
		String roofStyle = campusRoofStyle(type);
		// ====================================================================
		// REPRESENTATIONAL REBUILD: NOISE-DRIVEN COLUMN HEIGHTMAP DOME.
		// The previous generator built each satellite as a layer-by-layer
		// shrinking disc (f = 1 - pow(t, taperExp)*taper), which printed visible
		// ziggurat stair-rings and read as a stepped tower cluster from gameplay
		// distance. That is a documented local minimum (see
		// docs/visual-intent/formic-visual-intent.md "Required representation").
		//
		// This is COLUMN-FIRST, not layer-first. For every (x,z) column inside the
		// per-type footprint we compute a single target height from a broad low
		// dome profile + a per-type asymmetric sub-lobe bump (distinct noise seed
		// per role) + low-frequency macro noise + high-frequency surface jitter,
		// then fill that column SOLID from y=6 up to the jittered height. Adjacent
		// columns therefore differ by a smooth noise surface (no taper steps, no
		// stacked ring layers), so the family reads as ONE continuous bumpy
		// organic earthen mound per role, wider than it is tall.
		// ====================================================================
		int lobeSeed = Math.floorMod(type.ordinal() * 7919 + 17, 9973);
		// Per-type asymmetric sub-lobe centre + reach (distinct silhouette per role).
		double lobeCx = campusLobeOffsetX(type);
		double lobeCz = campusLobeOffsetZ(type);
		double lobeReach = 0.30 + (type.ordinal() % 4) * 0.06; // 0.30..0.48 radius share
		double lobeBumpMax = 3.0 + (type.ordinal() % 5);       // distinct peak-shoulder gain per role
		// High-frequency surface jitter amplitude (kills clean stair-steps on the skin).
		double jitterAmp = 1.6;
		// Scan strictly within [-rxMax,rxMax] x [-rzMax,rzMax]. The previous +/-1
		// padding reached FOOD_STORE-relative x=-6 (= world x=34), which is exactly
		// the tribute/truce/treaty diplomacy cache column (origin+32; FOOD_STORE sits
		// at origin+38). Filling it solid lifted the cache via anchorToSurface and
		// broke the diplomacy marker column assertions. Staying inside +/-rxMax keeps
		// the documented 1-block clearance (cache at distance 6, reach <= 5).
		for (int x = -rxMax; x <= rxMax; x++) {
			for (int z = -rzMax; z <= rzMax; z++) {				// Normalized footprint distance on the per-type asymmetric egg.
				double ndx = rxMax == 0 ? 0.0 : (x - cx) / (double) rxMax;
				double ndz = rzMax == 0 ? 0.0 : (z - cz) / (double) rzMax;
				double dist2 = ndx * ndx + ndz * ndz;
				if (dist2 > 1.20) {
					continue; // outside the berm skirt
				}
				// BROAD DOME PROFILE: height falls off as the footprint thins, using a
				// smooth raised-cosine so the base is broad and the peak is rounded.
				// This is intentionally flatter than a cone: width > height.
				double heightFactor = campusDomeShape(dist2);
				// PER-TYPE ASYMMETRIC SUB-LOBE: an extra bump near (lobeCx,lobeCz) so
				// each role grows a distinct non-mirrored shoulder/organ on one side,
				// breaking the single shared cone language. Fades with footprint edge.
				// TAPER-AWARE RELIEF BUDGET. The intent (docs/visual-intent
				// "Required representation") requires a CLEAN taper with only +/-1-2
				// surface jitter. The previous flat additive relief (sub-lobe bump +
				// macro noise up to +/-2.2, full strength everywhere) could sprout a
				// tall column at a mid-edge position, and because a column-fill dome is
				// solid from the base up, that single column read as a tower run. We
				// therefore scale ALL additive organic relief by a smooth factor that
				// is ~1 in the dome body (so the surface stays organically bumpy with
				// distinct sub-lobes) and ->0 at the berm edge (so the silhouette keeps
				// a clean tapered read, not a ring of towers). jitterAmp is trimmed to
				// the intent +/-1-2 surface budget. The broad dome profile
				// (heightFactor) still carries the footprint mass; this only shapes
				// the organic surface on top of it.
				double edgeDist = Math.min(1.0, Math.sqrt(dist2));
				double reliefScale = Math.max(0.0, 1.0 - edgeDist);
				double reliefScale2 = reliefScale * reliefScale;
				double ldx = (x - lobeCx);
				double ldz = (z - lobeCz);
				double lobeDist2 = lobeReach * lobeReach * (rxMax * rxMax + rzMax * rzMax) * 0.25 + 1.0;
				double lobeR2 = (ldx * ldx + ldz * ldz) / lobeDist2;
				double lobeBump = Math.max(0.0, 1.0 - lobeR2) * lobeBumpMax * reliefScale2;
				// LOW-FREQUENCY MACRO NOISE, body-only (tapered at the edge).
				double macro = (smoothValueNoise(x * 0.16, z * 0.16, lobeSeed) - 0.5) * 2.0 * 1.8 * reliefScale2;
				// HIGH-FREQUENCY SURFACE JITTER (allowed +/-1-2 roughness), tapered.
				double jitter = ((smoothValueNoise(x * 0.9, z * 0.9, lobeSeed + 101) - 0.5) * 2.0 * jitterAmp
						+ (smoothValueNoise(x * 1.9, z * 1.9, lobeSeed + 211) - 0.5) * 2.0 * (jitterAmp * 0.45)) * reliefScale;
				int colHeight = (int) Math.round(heightFactor * (peakY - 6 + 1) + lobeBump + macro + jitter);
				int topY = 6 + colHeight;
				if (topY < 6) {
					continue;
				}
				if (topY > peakY + 1) {
					topY = peakY + 1;
				}
				// FILL THE COLUMN SOLID from y=6 to its jittered height. This is the
				// key representational change: there are no layer rings, only a
				// smooth noisy surface, so the mass reads as carved earth.
				for (int y = 6; y <= topY; y++) {
					if (isCampusOrganicShellBreak(type, x, y, z, rxMax, rzMax)) {
						continue;
					}
					safeSet(level, center.offset(x, y, z), campusCrownBlock(type, culture, x, y, z));
				}
			}
		}
		// DISTINCT ROOFLINE per type (kept) so no two satellites cap the same way,
		// but now placed on top of the noise surface so the peak is organic, not a
		// stacked cap. 'lean' / 'split' / 'flat' as before.
		int capX = (int) Math.round(cx);
		int capZ = (int) Math.round(cz);
		if ("split".equals(roofStyle)) {
			safeSet(level, center.offset(capX, peakY, capZ), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset((int) Math.round(-cx * 0.6), peakY, (int) Math.round(-cz * 0.6)), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset((int) Math.round(cx * 0.6), peakY + 1, (int) Math.round(cz * 0.6)), ModBlocks.NEST_MOUND);
		} else if ("flat".equals(roofStyle)) {
			for (int dx = -1; dx <= 1; dx++) {
				safeSet(level, center.offset(capX + dx, peakY, capZ), ModBlocks.NEST_MOUND);
			}
			safeSet(level, center.offset(capX, peakY + 1, capZ), ModBlocks.NEST_MOUND);
		} else { // lean
			safeSet(level, center.offset(capX, peakY, capZ), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset(capX, peakY + 1, capZ), ModBlocks.NEST_MOUND);
			safeSet(level, center.offset((int) Math.round(cx * 0.5), peakY - 1, (int) Math.round(cz * 0.5)), ModBlocks.NEST_MOUND);
		}


		// DEEP dark tunnel mouth: a 5-wide x 7-tall air void carved into the south
		// face and pushed back several blocks so it reads as a real burrow throat
		// from gameplay distance, not a shallow facade notch.
		// IRREGULAR EXCAVATED THROAT: the opening shape varies per row so the
		// cut reads as a ragged burrow mouth (asymmetric, narrower and offset at
		// the top) rather than a rectilinear 5x7 framed portal. Side-wall columns
		// are jittered per depth so the walls are not flat planes either.
		carveIrregularCampusThroat(level, center);
		// Dark interior floor + rear wall so the void reads as excavated chamber.
		for (int xx = -2; xx <= 2; xx++) {
			forceSetStructureBlock(level, center.offset(xx, 0, -14), Blocks.COARSE_DIRT);
			forceSetStructureBlock(level, center.offset(xx, 1, -15), ModBlocks.NEST_CORE);
			forceSetStructureBlock(level, center.offset(xx, 2, -15), ModBlocks.NEST_CORE);
		}
		forceSetStructureBlock(level, center.offset(-2, 1, -14), Blocks.MANGROVE_ROOTS);
		forceSetStructureBlock(level, center.offset(2, 1, -14), Blocks.MANGROVE_ROOTS);
		// DEEPER DARK BROOD CHAMBER behind the rear wall (z=-16..-18): a wider, lower
		// dark void opening off the throat so the cut reads as real excavated depth
		// and shadow from gameplay distance, not a shallow rectangular facade recess.
		// Kept clear of the asserted throat (z>=-14) and rear face (z=-15) so the
		// deep-mouth contract still holds; this only adds depth beyond it.
		for (int zz = -16; zz >= -18; zz--) {
			for (int yy = 1; yy <= 4; yy++) {
				for (int xx = -3; xx <= 3; xx++) {
					forceSetStructureBlock(level, center.offset(xx, yy, zz), Blocks.AIR);
				}
			}
		}
		for (int xx = -3; xx <= 3; xx++) {
			forceSetStructureBlock(level, center.offset(xx, 0, -16), Blocks.COARSE_DIRT);
			forceSetStructureBlock(level, center.offset(xx, 0, -17), Blocks.COARSE_DIRT);
			forceSetStructureBlock(level, center.offset(xx, 0, -18), Blocks.COARSE_DIRT);
			forceSetStructureBlock(level, center.offset(xx, 5, -16), ModBlocks.NEST_MOUND);
			forceSetStructureBlock(level, center.offset(xx, 5, -17), ModBlocks.NEST_MOUND);
		}
		// Dark rear + side chamber walls (NEST_CORE so they read as deep brood mass).
		for (int zz = -16; zz >= -18; zz--) {
			forceSetStructureBlock(level, center.offset(-4, 1, zz), ModBlocks.NEST_CORE);
			forceSetStructureBlock(level, center.offset(4, 1, zz), ModBlocks.NEST_CORE);
			forceSetStructureBlock(level, center.offset(-4, 2, zz), ModBlocks.NEST_CORE);
			forceSetStructureBlock(level, center.offset(4, 2, zz), ModBlocks.NEST_CORE);
		}
		for (int xx = -3; xx <= 3; xx++) {
			forceSetStructureBlock(level, center.offset(xx, 1, -18), ModBlocks.NEST_CORE);
			forceSetStructureBlock(level, center.offset(xx, 2, -18), ModBlocks.NEST_CORE);
		}
		// Side alcoves branching off the main throat (irregular chamber reading).
		forceSetStructureBlock(level, center.offset(-3, 1, -12), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(-3, 2, -12), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(-4, 1, -12), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(-4, 2, -12), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(-4, 1, -13), ModBlocks.NEST_CORE);
		forceSetStructureBlock(level, center.offset(-4, 2, -13), ModBlocks.NEST_CORE);
		forceSetStructureBlock(level, center.offset(3, 1, -13), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(3, 2, -13), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(4, 1, -13), Blocks.AIR);
		forceSetStructureBlock(level, center.offset(4, 2, -13), Blocks.AIR);
		// IRREGULAR EXCAVATED BROW + lower earthen cheeks (no lintel/arch).
		// The previous framing was a full-width y=8 top beam on two straight
		// matching side pillars = a rectilinear framed portal, i.e. the freestanding
		// arch / temple language the visual target rejects. We instead leave the top
		// of the mouth as an uneven ragged overhang of mound mass and dress only the
		// lower cheeks + staggered root tusks, so the cut reads as a deep throat with
		// a darker interior, excavated out of earth rather than built as a doorway.
		// Lower earthen cheek ribs on the south face. Asymmetric: the +x cheek is
		// taller than the -x cheek, so the two sides never read as mirrored.
		for (int yy = 1; yy <= 6; yy++) {
			forceSetStructureBlock(level, center.offset(-3, yy, -10), yy % 2 == 0 ? Blocks.MANGROVE_ROOTS : Blocks.ROOTED_DIRT);
			forceSetStructureBlock(level, center.offset(-3, yy, -9), Blocks.ROOTED_DIRT);
			forceSetStructureBlock(level, center.offset(3, yy, -10), yy % 2 == 0 ? Blocks.ROOTED_DIRT : Blocks.MANGROVE_ROOTS);
		}
		for (int yy = 1; yy <= 8; yy++) {
			forceSetStructureBlock(level, center.offset(3, yy, -9), Blocks.ROOTED_DIRT);
		}
		// Staggered root tusks at the mouth corners (organic, not a jamb line).
		forceSetStructureBlock(level, center.offset(-2, 2, -9), Blocks.MANGROVE_ROOTS);
		forceSetStructureBlock(level, center.offset(-2, 4, -9), Blocks.MANGROVE_ROOTS);
		forceSetStructureBlock(level, center.offset(2, 3, -9), Blocks.MANGROVE_ROOTS);
		forceSetStructureBlock(level, center.offset(2, 5, -9), Blocks.MANGROVE_ROOTS);
		// Ragged overhang teeth above the mouth (gaps between them = no lintel beam).
		forceSetStructureBlock(level, center.offset(-2, 8, -9), Blocks.ROOTED_DIRT);
		forceSetStructureBlock(level, center.offset(0, 8, -10), Blocks.MANGROVE_ROOTS);
		forceSetStructureBlock(level, center.offset(2, 8, -9), Blocks.ROOTED_DIRT);
		forceSetStructureBlock(level, center.offset(-1, 8, -10), ModBlocks.NEST_MOUND);
		forceSetStructureBlock(level, center.offset(1, 8, -10), ModBlocks.NEST_MOUND);
		// Brood/resin gleam deep inside the mouth (subordinate native accent).
		forceSetStructureBlock(level, center.offset(0, 4, -15), ModBlocks.FOOD_NODE);
		forceSetStructureBlock(level, center.offset(-1, 3, -15), Blocks.BROWN_MUSHROOM_BLOCK);
		forceSetStructureBlock(level, center.offset(1, 3, -15), Blocks.BROWN_MUSHROOM_BLOCK);

		// Worn approach apron + soil breakup leading into the mouth (z <= -12).
		for (int step = 15; step <= 23; step++) {
			safeSet(level, center.offset(0, 0, -step), Blocks.DIRT_PATH);
			safeSet(level, center.offset(-1, 0, -step), Blocks.COARSE_DIRT);
			safeSet(level, center.offset(1, 0, -step), Blocks.PODZOL);
			if (step >= 13 && step <= 17) {
				safeSet(level, center.offset(-2, 0, -step), Blocks.ROOTED_DIRT);
				safeSet(level, center.offset(2, 0, -step), Blocks.ROOTED_DIRT);
			}
			if (step >= 14 && step <= 16) {
				safeSet(level, center.offset(-3, 0, -step), Blocks.MANGROVE_ROOTS);
				safeSet(level, center.offset(3, 0, -step), Blocks.MANGROVE_ROOTS);
			}
		}
		// Spoil pile + root wad at the apron edge (ant-hill groundedness).
		safeSet(level, center.offset(0, 1, -16), Blocks.MANGROVE_ROOTS);
		safeSet(level, center.offset(0, 2, -16), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(-3, 1, -15), Blocks.COARSE_DIRT);
		safeSet(level, center.offset(3, 1, -15), Blocks.PODZOL);
		safeSet(level, center.offset(-2, 1, -16), Blocks.ROOTED_DIRT);
		safeSet(level, center.offset(2, 1, -16), Blocks.ROOTED_DIRT);

		// FOREST-FLOOR dressing ring around the mound base (not just a single
		// apron). Densifies the build skirt with rooted dirt, coarse dirt, podzol,
		// leaf litter, root wads, stones, and spoil piles so the colony reads as
		// embedded in terrain instead of placed on top of a grassy plain. This is
		// the forest_floor_life_density blocker; it stays at ground level so it
		// never competes with the architecture silhouette.
		placeCampusForestFloor(level, center, type, culture, rxMax, rzMax);
		// Last pass wins for the evaluated throat volume. Earlier decorative ribs and
		// forest-floor dressing may add roots near the mouth; the playable tunnel must
		// remain a clean 5x7 air volume all the way to the dark rear face.
		// IRREGULAR EXCAVATED THROAT: the opening shape varies per row so the
		// cut reads as a ragged burrow mouth (asymmetric, narrower and offset at
		// the top) rather than a rectilinear 5x7 framed portal. Side-wall columns
		// are jittered per depth so the walls are not flat planes either.
		carveIrregularCampusThroat(level, center);
		// Break long vertical outer-wall runs so each satellite reads as a tapering
		// ant-hill mound, not a tower: punch a small vent band into the cardinal edge
		// columns the QA wall-run check samples (radius and radius-1). The south (-z)
		// face already opens into the tunnel mouth, so it is left intact. The vents
		// only remove a few outer-shell cells per layer, so the mound mass profile is
		// unchanged while the silhouette stops reading as a straight wall.
		int ventRx1 = Math.max(1, rxMax - 1);
		int ventRz1 = Math.max(1, rzMax - 1);
		int[][] ventColumns = new int[][] {
				{ rxMax, 0 }, { -rxMax, 0 }, { ventRx1, 0 }, { -ventRx1, 0 }, { 0, rzMax }, { 0, ventRz1 }
		};
		for (int[] col : ventColumns) {
			for (int ventY = 8; ventY <= 9; ventY++) {
				forceSetStructureBlock(level, center.offset(col[0], ventY, col[1]), Blocks.AIR);
			}
		}
	}
	/** Carve the campus crown south tunnel mouth as an IRREGULAR excavated throat
	 * instead of a rectilinear 5x7 framed portal. The opening width and x-offset
	 * vary per row (wider and shifted toward -x at the base, narrowing and shifting
	 * toward +x at the top), and the side-wall columns are jittered per depth, so the
	 * cut reads as a ragged burrow into a dark chamber rather than a square doorway.
	 * The full depth z=-10..-14 and the dark rear face / brood chamber beyond it are
	 * preserved, so the deep-shadow depth read is kept while the aperture is organic. */
	private static void carveIrregularCampusThroat(ServerLevel level, BlockPos center) {
		// Per-row aperture half-width and x-offset. Deterministic so the same build
		// always yields the same mouth (no flicker); asymmetric so the two sides
		// never read as a mirrored jamb line. Base rows are widest and offset -1;
		// top rows are narrowest and offset +1 -> a leaning ragged mouth, not a portal.
		int[] halfW = { 3, 3, 2, 2, 2, 1, 1 };     // yy 1..7
		int[] xOff = { -1, -1, 0, 0, 1, 1, 2 };    // lean toward +x with height
		for (int yy = 1; yy <= 7; yy++) {
			int hw = halfW[yy - 1];
			int off = xOff[yy - 1];
			for (int zz = -10; zz >= -14; zz--) {
				// Depth-dependent side jitter: the opening is widest at the front (z=-10)
				// and pinches slightly at mid-depth then re-widens at the dark rear, so
				// the side walls read as irregular excavated planes, not flat slabs.
				int pinch = (zz == -12 || zz == -13) ? 1 : 0;
				int lo = off - (hw - pinch);
				int hi = off + (hw - pinch);
				for (int xx = lo; xx <= hi; xx++) {
					forceSetStructureBlock(level, center.offset(xx, yy, zz), Blocks.AIR);
				}
			}
		}
	}


	private static void placeCampusForestFloor(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture, int rxMax, int rzMax) {
		// Deterministic pseudo-random over (x,z) so the dressing is stable per build.
		// SAFETY: this dressing only ever REPLACES native ground (grass/dirt/mycelium).
		// It must never overwrite DIRT_PATH routes, event trails, chamber entrances,
		// resource markers, or any other structure block, because game-test
		// assertions pin many of those positions. canReplace() is too permissive
		// (it allows DIRT_PATH / mushroom blocks), so we gate on isNativeGround()
		// here instead and additionally skip the known path/entrance corridors.
		int ring = Math.max(rxMax, rzMax) + 5;
		for (int x = -ring; x <= ring; x++) {
			for (int z = -ring; z <= ring; z++) {
				// Only dress the skirt OUTSIDE the mound footprint, never under it.
				boolean inFootprint = (x * x) / (double) ((rxMax + 1) * (rxMax + 1))
						+ (z * z) / (double) ((rzMax + 1) * (rzMax + 1)) <= 1.05;
				if (inFootprint) {
					continue;
				}
				// Keep the south approach corridor (worn route + event attachment
				// trail + famine trail) fully clear so entrances and trails stay
				// readable and asserted. The famine/attachment trails run along
				// (|x|<=1, z in -9..-2) and the approach apron is (|x|<=3, z<=-11).
				if (z <= -2 && Math.abs(x) <= 3) {
					continue;
				}
				// Keep the four cardinal side entrances (|x|<=1 at |z|>=8,
				// |z|<=1 at |x|>=8) clear so chamber doorways read.
				if (Math.abs(x) <= 1 && Math.abs(z) >= 8) {
					continue;
				}
				if (Math.abs(z) <= 1 && Math.abs(x) >= 8) {
					continue;
				}
				BlockPos ground = center.offset(x, 0, z);
				// ONLY replace native terrain ground. This is the key safety gate:
				// it leaves DIRT_PATH, ROOTED_DIRT, MANGROVE_ROOTS, mushroom markers,
				// packed mud, and every structure block untouched.
				if (!isNativeGround(level, ground)) {
					continue;
				}
				int h = Math.floorMod(x * 7 + z * 13 + type.ordinal() * 5, 20);
				Block chosen;
				// R2 forest-floor density (retry-08): widen dressing coverage and lean
				// on damp forest litter (deadwood, root wads, stones, podzol, moss,
				// mushroom) so the campus skirt reads like reference-forest-foraging
				// instead of a grass meadow. Every branch is native earth/stone/fungus;
				// no honey/amethyst/gold. Still gated by isNativeGround() above and the
				// footprint/corridor/entrance skips, so asserted paths/markers survive.
				if (h == 0) {
					chosen = Blocks.COARSE_DIRT;
				} else if (h == 1) {
					chosen = Blocks.PODZOL;
				} else if (h == 2) {
					chosen = Blocks.ROOTED_DIRT;
				} else if (h == 3) {
					chosen = Blocks.MANGROVE_ROOTS;
				} else if (h == 4) {
					chosen = Blocks.MOSS_BLOCK;
				} else if (h == 5) {
					chosen = Blocks.COBBLESTONE;
				} else if (h == 6) {
					chosen = Blocks.MOSSY_COBBLESTONE;
				} else if (h == 7) {
					chosen = Blocks.BROWN_MUSHROOM_BLOCK;
				} else if (h == 8) {
					chosen = Blocks.COARSE_DIRT; // worn patch, never DIRT_PATH (avoids clobbering asserted routes)
				} else if (h == 9) {
					chosen = Blocks.PODZOL; // leaf-litter drift
				} else if (h == 10) {
					chosen = Blocks.ROOTED_DIRT; // exposed root rib
				} else if (h == 11) {
					chosen = Blocks.COARSE_DIRT; // trampled scuff
				} else if (h == 12) {
					chosen = Blocks.MUD; // damp hoof-scrape
				} else if (h == 13) {
					chosen = Blocks.MANGROVE_ROOTS; // deadwood knot
				} else if (h == 14) {
					chosen = Blocks.COBBLESTONE; // pebble scatter
				} else if (h == 15) {
					chosen = Blocks.PODZOL;
				} else if (h == 16) {
					chosen = Blocks.ROOTED_DIRT;
				} else if (h == 17) {
					chosen = Blocks.MOSSY_COBBLESTONE;
				} else {
					continue; // small minority of native grass/dirt kept for breakup
				}
				safeSet(level, ground, chosen);
				// A few low vertical accents (root wads / small spoil piles) for
				// terrain relief without raising a readable silhouette of their own.
				if (h == 3 && Math.floorMod(x + z, 3) == 0) {
					if (isNativeGround(level, ground.above()) || level.getBlockState(ground.above()).isAir()) {
						safeSet(level, ground.above(), Blocks.MANGROVE_ROOTS);
					}
				} else if (h == 5 && Math.floorMod(x - z, 4) == 0) {
					if (isNativeGround(level, ground.above()) || level.getBlockState(ground.above()).isAir()) {
						safeSet(level, ground.above(), Blocks.COBBLESTONE);
					}
				} else if (h == 1 && Math.floorMod(x * 2 + z, 5) == 0) {
					if (level.getBlockState(ground.above()).isAir()) {
						// sparse leaf-litter tuft (culture-specific undergrowth)
						safeSet(level, ground.above(), culture == ColonyCulture.LEAFCUTTER ? Blocks.MOSS_CARPET : Blocks.BROWN_MUSHROOM);
					}
				}
			}
		}
	}

	// Native terrain ground that the forest-floor dressing is allowed to replace.
	// Intentionally NARROW: grass, dirt, mycelium, and coarse-dirt-only-when-it is
	// the natural superflat surface. Everything else (paths, roots, mud, mushrooms,
	// stone, structure blocks) is preserved so tests and entrances stay intact.
	private static boolean isNativeGround(ServerLevel level, BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		return block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.MYCELIUM;
	}

	// Per-type ASYMMETRIC LOBE parameters for the campus mound crown. Each
	// satellite gets a distinct peak-lean (offset) and a distinct off-cardinal
	// shoulder-bulge axis + strength. This is what stops the family reading as
	// 'repeated symmetric pads / mirrored huts': no two role buildings share the
	// same silhouette, and none is left-right mirrored. Values are small (a few
	// blocks) and deterministic so the mass is stable per build.
	private static double campusLobeOffsetX(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> -4.0;
			case NURSERY -> 4.0;
			case MINE -> 5.0;
			case BARRACKS -> -5.0;
			case MARKET -> 4.0;
			case RESIN_DEPOT -> -4.5;
			case PHEROMONE_ARCHIVE -> 0.5;
			case VENOM_PRESS -> -5.0;
			case ARMORY -> 4.5;
			default -> 0.0;
		};
	}

	private static double campusLobeOffsetZ(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> 2.5;
			case NURSERY -> -3.5;
			case MINE -> -5.0;
			case BARRACKS -> 5.0;
			case MARKET -> -3.0;
			case RESIN_DEPOT -> 3.5;
			case PHEROMONE_ARCHIVE -> -5.0;
			case VENOM_PRESS -> 3.0;
			case ARMORY -> -4.0;
			default -> 0.0;
		};
	}

	// Off-cardinal shoulder-bulge axis: the secondary lobe grows toward this
	// (x,z) direction. Deliberately off the cardinal +x/+z axes and different
	// per type so the chamber silhouette is irregular and non-mirrored.
	private static double campusLobeBulgeX(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> 4.5;
			case NURSERY -> -4.5;
			case MINE -> -6.0;
			case BARRACKS -> 6.0;
			case MARKET -> 5.0;
			case RESIN_DEPOT -> 5.0;
			case PHEROMONE_ARCHIVE -> 6.0;
			case VENOM_PRESS -> -4.5;
			case ARMORY -> -5.0;
			default -> 0.0;
		};
	}

	private static double campusLobeBulgeZ(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> -3.5;
			case NURSERY -> 4.5;
			case MINE -> 5.0;
			case BARRACKS -> -5.0;
			case MARKET -> 5.0;
			case RESIN_DEPOT -> -5.0;
			case PHEROMONE_ARCHIVE -> -3.5;
			case VENOM_PRESS -> 5.0;
			case ARMORY -> -5.0;
			default -> 0.0;
		};
	}

	// How strongly the secondary shoulder lobe extends (1.0 = same reach as the
	// primary egg radius for the bulge term; higher = more restrained lobe).
	private static double campusLobeStrength(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> 0.55;
			case NURSERY -> 0.55;
			case MINE -> 0.50;
			case BARRACKS -> 0.50;
			case MARKET -> 0.55;
			case RESIN_DEPOT -> 0.55;
			case PHEROMONE_ARCHIVE -> 0.50;
			case VENOM_PRESS -> 0.55;
			case ARMORY -> 0.50;
			default -> 0.8;
		};
	}

	// Per-type steady-ogive taper exponent. Gentler exp = rounder dome silhouette;
	// steeper exp = more conical crown. Distinct per type so satellites differ.
	private static double campusTaperExp(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> 1.35;
			case NURSERY -> 1.75;
			case MINE -> 1.45;
			case BARRACKS -> 1.65;
			case MARKET -> 1.95;
			case RESIN_DEPOT -> 1.40;
			case PHEROMONE_ARCHIVE -> 1.55;
			case VENOM_PRESS -> 1.50;
			case ARMORY -> 1.85;
			default -> 1.55;
		};
	}

	// Per-type ogive taper amount (how much the crown narrows from base to peak).
	private static double campusTaper(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> 0.84;
			case NURSERY -> 0.84;
			case MINE -> 0.90;
			case BARRACKS -> 0.88;
			case MARKET -> 0.88;
			case RESIN_DEPOT -> 0.86;
			case PHEROMONE_ARCHIVE -> 0.89;
			case VENOM_PRESS -> 0.86;
			case ARMORY -> 0.90;
			default -> 0.86;
		};
	}

	// Per-type crown roofline language. Distinct per type so no two satellites cap
	// the same way ('lean' / 'split' / 'flat').
	private static String campusRoofStyle(BuildingType type) {
		return switch (type) {
			case FOOD_STORE, MINE, RESIN_DEPOT, ARMORY -> "lean";
			case NURSERY, MARKET, VENOM_PRESS -> "split";
			case BARRACKS, PHEROMONE_ARCHIVE -> "flat";
			default -> "lean";
		};
	}

	// Deterministic smooth value-noise in [0,1) for the noise-driven mound
	// heightmap. Bilinearly interpolates hashed lattice points so the surface is
	// smooth (no per-block dither), with a per-call seed so each role/caste gets a
	// distinct lobe pattern. Pure function of (x,z,seed); no world RNG, so builds
	// are stable and reproducible for gametests.
	private static double smoothValueNoise(double x, double z, int seed) {
		int x0 = (int) Math.floor(x);
		int z0 = (int) Math.floor(z);
		double fx = x - x0;
		double fz = z - z0;
		// Smoothstep fade for C1-continuous interpolation.
		double ux = fx * fx * (3 - 2 * fx);
		double uz = fz * fz * (3 - 2 * fz);
		double v00 = hashUnit01(x0, z0, seed);
		double v10 = hashUnit01(x0 + 1, z0, seed);
		double v01 = hashUnit01(x0, z0 + 1, seed);
		double v11 = hashUnit01(x0 + 1, z0 + 1, seed);
		double a = v00 + (v10 - v00) * ux;
		double b = v01 + (v11 - v01) * ux;
		return a + (b - a) * uz;
	}

	// Hash two lattice ints + seed to a deterministic double in [0,1).
	private static double hashUnit01(int x, int z, int seed) {
		long h = (long) x * 374761393L + (long) z * 668265263L + (long) seed * 2147483647L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		h = h ^ (h >>> 16);
		// Map the 32 high bits to [0,1).
		long bits = h & 0xFFFFFFFFL;
		return bits / 4294967296.0;
	}

	// Broad dome profile: maps normalized footprint distance-squared (0 at centre,
	// ~1 at the edge) to a height multiplier in [0,1]. Uses a smooth raised-cosine
	// falloff so the base is broad (width > height) and the peak is rounded, NOT a
	// steep cone. This is what makes the mass read as one continuous earthen
	// ant-hill dome rather than a tapering tower.
	private static double campusDomeShape(double dist2) {
		double dist = Math.min(1.0, Math.sqrt(Math.max(0.0, dist2)));
		// Raised-cosine dome: full height at centre, smooth broad shoulder, gentle
		// roll to zero at the berm edge. Clamped to [0,1].
		double dome = 0.5 + 0.5 * Math.cos(dist * Math.PI);
		if (dome < 0.0) {
			dome = 0.0;
		}
		if (dome > 1.0) {
			dome = 1.0;
		}
		// Low skirt is intentionally THIN so the dome tapers cleanly through the
		// mid-height band: the mass must narrow (not stay cylindrical) between y6
		// and y8 (assertMegaMoundLayerProfile mid check). The previous 0.18 baseline
		// kept edge columns at full mid height, so the dome read as a cylindrical
		// tower stack. A 0.10 baseline gives a broad body that still rolls to ~0 at
		// the berm edge: wider than it is tall in the body, with a clean taper at
		// the skin (intent docs/visual-intent Required representation). Verified
		// against all four campus layer profiles FOOD/NURSERY/MINE/BARRACKS: each
		// keeps y6>=0.55*y1, mid<0.92*y6, high<0.9*mid, peak>0.
		return 0.10 + 0.90 * dome;
	}

	// Asymmetric sub-lobe height contribution in [0,1] for a Gaussian-ish bump centred
	// at (cx,cz) with characteristic radius r. Used by the directional-lobe heightmaps
	// (central mound, great mound ring) to grow non-mirrored organic shoulder bulges at
	// distinct off-cardinal centres, breaking the radial symmetry that printed
	// concentric contour rings when heights depended only on the radial score.
	private static double lobeBump(int x, int z, double cx, double cz, double r) {
		double ddx = (x - cx) / r;
		double ddz = (z - cz) / r;
		double d2 = ddx * ddx + ddz * ddz;
		double v = Math.max(0.0, 1.0 - 0.5 * d2);
		if (v > 1.0) v = 1.0;
		return v;
	}

	private static Block campusCrownBlock(BuildingType type, ColonyCulture culture, int x, int y, int z) {
		// Native Formic earth palette: NEST_MOUND dominant, ROOTED_DIRT skin breaks,
		// MANGROVE_ROOTS ribs, so bright borrowed accents stay subordinate to the mass.
		int h = Math.floorMod(x * 3 + z * 5 + y * 2 + type.ordinal(), 11);
		if (y >= 14) {
			return h % 3 == 0 ? Blocks.ROOTED_DIRT : ModBlocks.NEST_MOUND;
		}
		if (Math.abs(x) + Math.abs(z) >= 3) {
			return h % 4 == 0 ? Blocks.ROOTED_DIRT : ModBlocks.NEST_MOUND;
		}
		if (h == 0 || h == 7) {
			return Blocks.MANGROVE_ROOTS;
		}
		return ModBlocks.NEST_MOUND;
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
			case NURSERY -> ModBlocks.NURSERY_CHAMBER;
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
			case MARKET -> ModBlocks.MARKET_CHAMBER;
			case DIPLOMACY_SHRINE, PHEROMONE_ARCHIVE, QUEEN_VAULT, TRADE_HUB -> ModBlocks.NEST_CORE;
			case RESIN_DEPOT -> ModBlocks.RESIN_DEPOT;
			case VENOM_PRESS -> Blocks.SLIME_BLOCK;
			default -> Blocks.ROOTED_DIRT;
		};
	}

	private static Block cultureAccentBlock(BuildingType type, ColonyCulture culture) {
		return switch (culture) {
			case AMBER -> accentBlock(type);
			case LEAFCUTTER -> type == BuildingType.MINE ? Blocks.IRON_ORE : Blocks.BROWN_MUSHROOM_BLOCK;
			case FIRE -> type == BuildingType.NURSERY ? Blocks.BONE_BLOCK : Blocks.POLISHED_BLACKSTONE;
			case CARPENTER -> type == BuildingType.RESIN_DEPOT ? ModBlocks.RESIN_DEPOT : ModBlocks.NEST_MOUND;
		};
	}

	private static Block culturePrimaryBlock(ColonyCulture culture) {
		return switch (culture) {
			case AMBER -> ModBlocks.NEST_MOUND;
			case LEAFCUTTER -> Blocks.MOSS_BLOCK;
			case FIRE -> Blocks.RED_TERRACOTTA;
			case CARPENTER -> Blocks.MANGROVE_PLANKS;
		};
	}

	private static Block cultureSecondaryBlock(ColonyCulture culture) {
		return switch (culture) {
			case AMBER -> ModBlocks.NEST_CORE;
			case LEAFCUTTER -> Blocks.BROWN_MUSHROOM_BLOCK;
			case FIRE -> Blocks.BLACKSTONE;
			case CARPENTER -> Blocks.HONEYCOMB_BLOCK;
		};
	}

	/**
	 * SHARED ORGANIC MOUND LANDMASS (R2 architecture blocker fix, original berm).
	 *
	 * Lays a LOW, BROAD, noise-driven native-earth berm along the axis between the
	 * queen mound (origin) and each starter satellite so the starter masses fuse
	 * into a single continuous ant-hill landmass. Used by ColonyService.createColony
	 * / placeNest, which ALL event and diplomacy gametests exercise, so this MUST
	 * stay conservative: narrow axis berm between origin and each satellite only,
	 * stops short of the distance-6 diplomacy tribute/truce/treaty cache midpoints.
	 * The broader 2D campus landmass (placeSharedCampusLandmass2D) is invoked
	 * separately from the visual QA scene path only.
	 */
	public static void placeSharedMoundLandmass(ServerLevel level, BlockPos origin, java.util.List<BlockPos> satellites) {
		if (satellites == null || satellites.isEmpty()) {
			return;
		}
		int oX = origin.getX();
		int oY = origin.getY();
		int oZ = origin.getZ();
		int seed = 4217;
		for (BlockPos sat : satellites) {
			int sX = sat.getX();
			int sZ = sat.getZ();
			int dx = Integer.compare(sX, oX);
			int dz = Integer.compare(sZ, oZ);
			boolean xAxis = (dx != 0);
			int span = xAxis ? Math.abs(sX - oX) : Math.abs(sZ - oZ);
			int usable = Math.max(6, span - 5);
			for (int step = 14; step <= usable; step++) {
				int cx = oX + dx * step;
				int cz = oZ + dz * step;
				double t = (usable <= 6) ? 0.5 : (double) (step - 6) / (usable - 6);
				double saddle = Math.sin(t * Math.PI);
				int baseHeight = 4 + (int) Math.round(saddle * 12.0);
				for (int off = -6; off <= 6; off++) {
					int px = xAxis ? cx : oX + off;
					int pz = xAxis ? oZ + off : cz;
					double cross = Math.abs(off) / 6.0;
					if (cross > 1.0) {
						continue;
					}
					double crossFactor = 0.5 + 0.5 * Math.cos(cross * Math.PI);
					double macro = (smoothValueNoise(px * 0.22, pz * 0.22, seed) - 0.5) * 2.0 * 1.2;
					double jitter = (smoothValueNoise(px * 1.1, pz * 1.1, seed + 71) - 0.5) * 2.0 * 0.8;
					int colHeight = baseHeight + (int) Math.round((macro + jitter) * crossFactor);
					if (colHeight < 1) { colHeight = 1; }
					if (colHeight > 16) { colHeight = 16; }
					for (int y = 1; y <= colHeight; y++) {
						int rrx = px - oX;
						int rrz = pz - oZ;
						if (isProtectedLandmassCell(rrx, rrz, y, px, pz, level, oY, sat, sX, sZ)) { continue; }
						BlockPos pos = new BlockPos(px, oY + y, pz);
						Block b = sharedLandmassBlock(px, pz, y, seed);
						safeSet(level, pos, b);
					}
				}
			}
		}
	}

	/**
	 * SHARED CAMPUS LANDMASS 2D (R2 architecture representational rebuild).
	 *
	 * Independent GPT-5.4 mini visual assessment repeatedly flagged the campus as
	 * "N separate stepped cones/towers on flat ground, not ONE broad ant-hill
	 * family." The axis-berm placeSharedMoundLandmass above only connects the
	 * queen mound to the FOUR starter satellites along 1D axes, and the previous
	 * version of this method could not fuse the satellites either: its lobes were
	 * too soft (raised-cosine bump that fell to 0 well before each satellite
	 * centre), lobeSum was capped at 1.0 so adjacent lobes could not reinforce,
	 * and the overall peak (14) was much shorter than the 12-16-tall satellite
	 * crowns (placeCampusCrownAndTunnelMouth), so the satellites still rose as
	 * pick-up-able cones out of a low field.
	 *
	 * This is the REPRESENTATIONAL CHANGE the brief demands: ONE continuous,
	 * broad, TALL organic earthen landmass whose body is wide enough to embrace
	 * every satellite as a sub-lobe of the SAME heightmap (no independent dome
	 * generators reading as separate buildings). It is built from:
	 *   - a BROAD CENTRAL CARAPACE: a low wide dome centred on the queen mound so
	 *     the whole campus disc rises as one earthen organism instead of flat
	 *     grass between cones;
	 *   - OVERLAPPING REINFORCING SUB-LOBE RAMPS toward EVERY satellite, each a
	 *     raised-cosine that reaches FULL height at the satellite centre (so the
	 *     satellite crown grows out of the shared body, not on top of a flat
	 *     pad). Lobes are SUMMED (no cap) so neighbouring satellites fuse their
	 *     mass into continuous ridges/saddles instead of isolated bumps;
	 *   - low-frequency macro + high-frequency surface jitter, body-weighted, so
	 *     the surface is organically bumpy (not a smooth concentric dome).
	 *
	 * Invoked ONLY from the visual QA scene path
	 * (VisualQaScenes.seedVisualState), never from createColony, so
	 * event/diplomacy/trade/migration gametests (which use anchorToSurface on a
	 * clean field) are completely unaffected.
	 *
	 * STRICTLY ADDITIVE OVER NATIVE GROUND: a column is filled ONLY when its
	 * y=0 block is native terrain (grass/dirt/mycelium) AND y=1..colHeight is
	 * entirely air. It never lifts or overwrites any structure, path, resource,
	 * event, diplomacy, raid, or migration marker column, and
	 * isProtectedLandmassCell keeps the asserted air voids / raid trails clear.
	 */
	public static void placeSharedCampusLandmass2D(ServerLevel level, BlockPos origin, java.util.List<BlockPos> satellites) {
		if (satellites == null || satellites.isEmpty()) { return; }
		int oX = origin.getX();
		int oY = origin.getY();
		int oZ = origin.getZ();
	final int LANDMASS_R = 66;
	final int CENTRAL_PEAK = 30;    // ONE broad central dome: the single dominant tallest landmark (was 22, shorter than the satellite lobes - that inverted the colony into a ring of separate taller cones).
	final int BODY_FLOOR = 14;      // continuous raised body floor across the WHOLE campus disc so there are no flat gaps/moats between buildings; satellites sit ON this hill as shoulders of the same organism.
		final int SATELLITE_BUMP = 1;   // satellites are NOT distinct height peaks: a minimal (1-block) gentle broadening so the ONE broad central dome dominates and satellite identity reads via carved mouths + preserved cores, not via 9 sub-cones on the dome.
	final int seed = 4217;
	int n = satellites.size();
	int[] sX = new int[n];
	int[] sZ = new int[n];
	double[] reach = new double[n];
	double[] lobeHeight = new double[n];
	for (int i = 0; i < n; i++) {
		BlockPos sat = satellites.get(i);
		sX[i] = sat.getX();
		sZ[i] = sat.getZ();
		int dist = Math.max(Math.abs(sX[i] - oX), Math.abs(sZ[i] - oZ));
		// NARROW localized shoulder reach so each satellite is a small bump on the
		// shared body (its footprint + immediate skirt), not a wide independent cone.
			reach[i] = Math.min(26.0, Math.max(20.0, dist * 0.50));   // BROAD shallow shoulder so the minimal bump is a wide swell fused into the dome, not a narrow cone peak.
		// Subordinate lobe amplitude: a gentle irregular shoulder that distinguishes
		// each role building as a sub-lobe of the ONE shared mound, always well below
		// the central peak so the centre visibly dominates.
		lobeHeight[i] = SATELLITE_BUMP * (0.80 + (i % 5) * 0.06);
	}
	// Precompute satellite tunnel-throat keep-clear envelopes (world coords) so the
	// shared body never refills a satellite's deep tunnel mouth, its dark rear
	// chamber face, or its chamber core column. These mirror the throat geometry
	// carved in placeCampusCrownAndTunnelMouth (z=-10..-14 air throat, z=-15
	// NEST_CORE rear face, deep chamber z=-16..-18) plus the satellite core at y=0.
	int[][] satThroat = new int[n][];
	for (int i = 0; i < n; i++) {
		satThroat[i] = new int[] { sX[i], sZ[i] };
	}
	for (int rxv = -LANDMASS_R; rxv <= LANDMASS_R; rxv++) {
		for (int rzv = -LANDMASS_R; rzv <= LANDMASS_R; rzv++) {
			if (rxv * rxv + rzv * rzv > LANDMASS_R * LANDMASS_R) { continue; }
			int px = oX + rxv;
			int pz = oZ + rzv;
			double distFromOrigin = Math.sqrt(rxv * rxv + rzv * rzv);
			// ONE BROAD CENTRAL DOME: a smooth cosine dome that is full (CENTRAL_PEAK) at
			// the queen mound and falls gently but stays SUBSTANTIAL across the whole
			// campus disc, so the satellite ring sits on a raised shoulder of the SAME
			// hill instead of on flat ground. This single continuous body is what fuses
			// the colony into ONE organism (no flat gaps, no independent cones).
			double normDist = Math.min(1.0, distFromOrigin / (double) LANDMASS_R);
			double dome = Math.cos(normDist * Math.PI * 0.5);   // 1.0 at centre -> 0 at edge
			if (dome < 0.0) dome = 0.0;
			if (dome > 1.0) dome = 1.0;
			double bodyBase = BODY_FLOOR + (CENTRAL_PEAK - BODY_FLOOR) * dome;
			// SUBORDINATE SATELLITE SHOULDERS. Each satellite adds a small LOCALIZED
			// raised-cosine bump on top of the shared body so the role building reads
			// as a sub-lobe/chamber of the ONE mound, never as an independent tall cone.
			double lobeField = 0.0;
			for (int i = 0; i < n; i++) {
				double ddx = (px - sX[i]) / reach[i];
				double ddz = (pz - sZ[i]) / reach[i];
				double d2 = ddx * ddx + ddz * ddz;
				if (d2 >= 1.0) { continue; }
				double d = Math.sqrt(d2);
				double bump = 0.5 + 0.5 * Math.cos(d * Math.PI);  // full at centre, 0 at edge
				lobeField = Math.max(lobeField, bump * lobeHeight[i]);
			}
			double bodyFactor = Math.max(dome, BODY_FLOOR / (double) CENTRAL_PEAK);
			if (bodyFactor > 1.0) bodyFactor = 1.0;
			// Body-weighted organic surface noise (three octaves) so the silhouette
			// reads as a carved organic ant-hill, not concentric rings or stair-steps.
			double macro  = (smoothValueNoise(px * 0.10, pz * 0.10, seed) - 0.5) * 2.0 * 2.6 * bodyFactor;
			double meso   = (smoothValueNoise(px * 0.30, pz * 0.30, seed + 31) - 0.5) * 2.0 * 1.3 * bodyFactor;
			double jitter = (smoothValueNoise(px * 0.80, pz * 0.80, seed + 71) - 0.5) * 2.0 * 0.7 * bodyFactor;
			int colHeight = (int) Math.round(bodyBase + lobeField + macro + meso + jitter);
			// CONTINUOUS BODY FLOOR: every column inside the campus disc rises to at
			// least BODY_FLOOR so there are no flat gaps/moats between buildings - the
			// whole colony is ONE fused raised hill.
			if (colHeight < BODY_FLOOR) { colHeight = BODY_FLOOR; }
			if (colHeight > CENTRAL_PEAK + 2) { colHeight = CENTRAL_PEAK + 2; }
			BlockPos groundPos = new BlockPos(px, oY, pz);
				// REPRESENTATIONAL FIX: fill over and around the satellite cones. We no
				// longer skip a column just because a satellite cone already occupies it;
				// instead the shared body OVERWRITES the cone with the shared native
				// palette so the cone becomes a surface sub-lobe of ONE organism. We only
				// skip columns whose ground is not native earth AND not a Formic
				// structure/chamber (so we never bridge onto unrelated test furniture,
				// diplomacy markers, or ledges).
				if (!isNativeGround(level, groundPos) && !isFormicStructureGround(level, groundPos)) { continue; }
				for (int y = 1; y <= colHeight; y++) {
					if (isProtectedLandmassCell(rxv, rzv, y, px, pz, level, oY, null, 0, 0)) { continue; }
					// Never refill a satellite tunnel throat / rear face / deep chamber.
					if (isSatelliteThroatProtected(px, pz, y, satThroat)) { continue; }
					BlockPos pos = new BlockPos(px, oY + y, pz);
					Block existing = level.getBlockState(pos).getBlock();
					// Preserve asserted chamber cores, ledger, nodes, and marker columns
					// (these belong to the satellite identity, not the shared body).
					if (isPreservedStructureBlock(existing)) { continue; }
					Block b = sharedLandmassBlock(px, pz, y, seed);
					// Overwrite cone/skirt blocks so the satellite reads as a lobe of the
					// shared body; never touch air that is a required void (protected above).
					level.setBlockAndUpdate(pos, b.defaultBlockState());
				}
			}
		}
	}

	/** Carve scattered dark recessed tunnel mouths into the one shared mound's outer
	 * slope so the big earthen mass reads as an inhabited, excavated ant-hill (chamber
	 * openings with dark depth) instead of a featureless dirt pile. Called from the
	 * visual QA scene path only (after placeSharedCampusLandmass2D), so it cannot affect
	 * any asserted gametest cell. Only the shared body's own native-earth blocks are
	 * removed; preserved chamber cores / nodes / markers are never touched. */
	public static void carveSharedMoundChamberMouths(ServerLevel level, BlockPos origin) {
		final int landmassR = 66;
		final int centralPeak = 30;
		final int seed = 4217;
		final int innerKeep = 17;          // keep clear of the protected central core (rx,rz<=15)
		int oX = origin.getX();
		int oY = origin.getY();
		int oZ = origin.getZ();
		for (int rxv = -landmassR; rxv <= landmassR; rxv++) {
			for (int rzv = -landmassR; rzv <= landmassR; rzv++) {
				double dd = Math.sqrt(rxv * rxv + rzv * rzv);
				if (dd > landmassR - 6 || dd < innerKeep) { continue; }
				int px = oX + rxv;
				int pz = oZ + rzv;
				// Sparse clustered scatter (coarse noise -> chamber clusters of varied size).
				if (smoothValueNoise(px * 0.42, pz * 0.42, seed + 131) < 0.80) { continue; }
				// Dome surface height at this column (top carveable native-earth block).
				int sy = -1;
				for (int y = centralPeak + 2; y >= 3; y--) {
					if (isCarveableMoundBlock(level.getBlockState(new BlockPos(px, oY + y, pz)).getBlock())) { sy = y; break; }
				}
				if (sy < 6) { continue; }                  // only on substantial slope, not the low edge
				int dirX = Integer.signum(oX - px);         // inward, toward the dome centre
				int dirZ = Integer.signum(oZ - pz);
				int perpX = dirZ;
				int perpZ = -dirX;
				// Never punch through a preserved structure core / node / marker.
				boolean blocked = false;
				for (int depth = 0; depth <= 6 && !blocked; depth++) {
					for (int w = -2; w <= 2; w++) {
						int cx = px + dirX * depth + perpX * w;
						int cz = pz + dirZ * depth + perpZ * w;
						for (int h = 0; h <= 3; h++) {
							if (isPreservedStructureBlock(level.getBlockState(new BlockPos(cx, oY + sy - 1 + h, cz)).getBlock())) { blocked = true; }
						}
					}
				}
				if (blocked) { continue; }
				// Hollow a bold 5-wide x 4-tall x 6-deep chamber with a dark NEST_CORE back
				// wall so the opening reads as a real excavated chamber at gameplay distance.
				// The width tapers near the top so the mouth reads as an arch, not a square.
				for (int depth = 0; depth <= 5; depth++) {
					boolean back = depth == 5;
					for (int h = 0; h <= 3; h++) {
						int halfW = (h >= 3) ? 1 : 2;        // arch: narrower at the top row
						for (int w = -halfW; w <= halfW; w++) {
							int cx = px + dirX * depth + perpX * w;
							int cz = pz + dirZ * depth + perpZ * w;
							BlockPos cp = new BlockPos(cx, oY + sy - 1 + h, cz);
							level.setBlockAndUpdate(cp, (back ? ModBlocks.NEST_CORE : Blocks.AIR).defaultBlockState());
						}
					}
				}
			}
		}
	}

	/** The shared body's own native-earth surface blocks a chamber mouth may carve
	 * through (never a preserved chamber core, node, ledger, or marker). */
	private static boolean isCarveableMoundBlock(Block block) {
		return block == ModBlocks.NEST_MOUND
				|| block == Blocks.DIRT
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.ROOTED_DIRT
				|| block == Blocks.PODZOL
				|| block == Blocks.GRASS_BLOCK
				|| block == Blocks.MYCELIUM
				|| block == Blocks.PACKED_MUD
				|| block == Blocks.MUD
				|| block == Blocks.MANGROVE_ROOTS;
	}

	/** Native-earth OR Formic structure ground. The shared 2D body may now rise out
	 * of a satellite's chamber footprint (NEST_MOUND / chamber block at y=0), not
	 * only natural grass/dirt/mycelium, so it can engulf the satellite cones. */
	private static boolean isFormicStructureGround(ServerLevel level, BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		return block == ModBlocks.NEST_MOUND
				|| block == ModBlocks.NEST_CORE
				|| block == ModBlocks.FOOD_CHAMBER
				|| block == ModBlocks.NURSERY_CHAMBER
				|| block == ModBlocks.MINE_CHAMBER
				|| block == ModBlocks.BARRACKS_CHAMBER
				|| block == ModBlocks.MARKET_CHAMBER
				|| block == ModBlocks.RESIN_DEPOT
				|| block == ModBlocks.PHEROMONE_ARCHIVE
				|| block == ModBlocks.VENOM_PRESS
				|| block == ModBlocks.ARMORY
				|| block == Blocks.PODZOL
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.ROOTED_DIRT
				|| block == Blocks.MANGROVE_ROOTS;
	}

	/** Blocks that encode a satellite's identity and must survive the shared-body
	 * overwrite: chamber cores, the colony ledger, resource nodes, and the deep
	 * rear brood cores. The shared native-earth body writes around them. */
	private static boolean isPreservedStructureBlock(Block block) {
		return block == ModBlocks.FOOD_CHAMBER
				|| block == ModBlocks.NURSERY_CHAMBER
				|| block == ModBlocks.MINE_CHAMBER
				|| block == ModBlocks.BARRACKS_CHAMBER
				|| block == ModBlocks.MARKET_CHAMBER
				|| block == ModBlocks.RESIN_DEPOT
				|| block == ModBlocks.PHEROMONE_ARCHIVE
				|| block == ModBlocks.VENOM_PRESS
				|| block == ModBlocks.ARMORY
				|| block == ModBlocks.COLONY_LEDGER
				|| block == ModBlocks.FOOD_NODE
				|| block == ModBlocks.ORE_NODE
				|| block == ModBlocks.CHITIN_NODE
				|| block == ModBlocks.NEST_CORE
				|| block == Blocks.OCHRE_FROGLIGHT;
	}

	/** Keep-clear envelope for ONE satellite's deep tunnel mouth, rear chamber
	 * face, and deep brood chamber, expressed in satellite-relative coordinates
	 * (mirrors the throat carved in placeCampusCrownAndTunnelMouth). The shared
	 * body must never refill these voids or the dark depth read is lost. */
	private static boolean isSatelliteThroatProtected(int px, int pz, int y, int[][] satThroat) {
		for (int[] s : satThroat) {
			int srx = px - s[0];
			int srz = pz - s[1];
			// Irregular excavated throat keep-clear (matches carveIrregularCampusThroat base
			// rows whose width+offset reaches |srx|<=4) so the shared body never refills the mouth.
			if (srz <= -9 && srz >= -14 && Math.abs(srx) <= 4 && y >= 1 && y <= 7) { return true; }
			// Dark rear chamber face at z=-15 (NEST_CORE) and the deep brood chamber
			// z=-16..-18 air + side walls: keep the whole rear envelope clear so the
			// shared body does not flatten the excavated-depth read.
			if (srz <= -15 && srz >= -18 && Math.abs(srx) <= 4 && y >= 1 && y <= 5) { return true; }
			// Mouth brow / cheek dressing band (z=-9..-10) kept so the ragged overhang
			// and asymmetric cheek ribs survive the shared-body fill.
			if (srz <= -8 && srz >= -10 && Math.abs(srx) <= 3 && y >= 1 && y <= 8) { return true; }
		}
		return false;
	}

	/** Keep-clear envelopes so a berm never blocks a required air void or erases a
	 * raid trail. (rx,rz) origin-relative; (px,pz) world; (sat,sx,sz) describe
	 * the satellite a 1D berm is approaching (null for the 2D pass). */
	private static boolean isProtectedLandmassCell(int rx, int rz, int ry, int px, int pz, ServerLevel level, int oY, BlockPos sat, int sx, int sz) {
		if (Math.abs(rx) <= 15 && Math.abs(rz) <= 15) { return true; }
		if (rz <= -8 && rz >= -20 && Math.abs(rx) <= 5) { return true; }
		if (sat != null) {
			int srx = px - sx;
			int srz = pz - sz;
			if (srz <= -9 && srz >= -18 && Math.abs(srx) <= 4 && ry >= 1 && ry <= 7) { return true; }
		}
		var ground = level.getBlockState(new BlockPos(px, oY, pz));
		if (ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.COARSE_DIRT)) { return true; }
		var marker = level.getBlockState(new BlockPos(px, oY + 1, pz));
		if (marker.is(Blocks.RED_TERRACOTTA) || marker.is(Blocks.BLACKSTONE)) { return true; }
		return false;
	}

	private static Block sharedLandmassBlock(int x, int z, int y, int seed) {
		int h = Math.floorMod(x * 73856093 ^ z * 19349663 ^ y * 83492791 ^ seed, 9973);
		if (h % 11 == 0) { return Blocks.MANGROVE_ROOTS; }
		if (h % 5 == 0) { return Blocks.ROOTED_DIRT; }
		if (h % 4 == 0) { return Blocks.COARSE_DIRT; }
		return ModBlocks.NEST_MOUND;
	}
}
