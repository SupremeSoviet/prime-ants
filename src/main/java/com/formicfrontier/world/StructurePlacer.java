package com.formicfrontier.world;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.BuildingType;
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
		switch (type) {
			case QUEEN_CHAMBER -> placeQueenHall(level, center);
			case FOOD_STORE, NURSERY, MINE, BARRACKS, MARKET, RESIN_DEPOT, PHEROMONE_ARCHIVE, VENOM_PRESS, ARMORY -> placeCampusBuilding(level, center, type);
			case CHITIN_FARM -> placeChitinFarm(level, center);
			case DIPLOMACY_SHRINE -> placeDiplomacyShrine(level, center);
			case WATCH_POST -> placeWatchPost(level, center);
			case FUNGUS_GARDEN -> placeFungusGarden(level, center);
			case ROAD -> placeRoadPatch(level, center);
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
				|| block == Blocks.MUD_BRICKS
				|| block == Blocks.CUT_COPPER
				|| block == Blocks.CHISELED_TUFF
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
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				boolean edge = Math.abs(x) == 6 || Math.abs(z) == 6;
				safeSet(level, center.offset(x, 0, z), edge ? Blocks.MUD_BRICKS : Blocks.PACKED_MUD);
				for (int y = 1; y <= 4; y++) {
					if (edge) {
						safeSet(level, center.offset(x, y, z), (x + z + y) % 3 == 0 ? ModBlocks.NEST_MOUND : Blocks.MUD_BRICKS);
					} else {
						safeSet(level, center.offset(x, y, z), Blocks.AIR);
					}
				}
				safeSet(level, center.offset(x, 5, z), edge || Math.abs(x) + Math.abs(z) <= 7 ? Blocks.ROOTED_DIRT : Blocks.AIR);
			}
		}
		for (int x = -1; x <= 1; x++) {
			for (int y = 1; y <= 3; y++) {
				safeSet(level, center.offset(x, y, -6), Blocks.AIR);
			}
		}
		for (BlockPos column : new BlockPos[] {center.offset(-5, 1, -5), center.offset(5, 1, -5), center.offset(-5, 1, 5), center.offset(5, 1, 5)}) {
			for (int y = 0; y <= 3; y++) {
				safeSet(level, column.above(y), Blocks.CHISELED_TUFF);
			}
			safeSet(level, column.above(4), Blocks.OCHRE_FROGLIGHT);
		}
		safeSet(level, center.below(), ModBlocks.NEST_CORE);
		safeSet(level, center, ModBlocks.NEST_MOUND);
		placeColonyLedger(level, center.offset(3, 1, 0));
		safeSet(level, center.offset(0, 4, 0), Blocks.OCHRE_FROGLIGHT);
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type) {
		Block core = coreBlock(type);
		Block shell = shellBlock(type);
		Block accent = accentBlock(type);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				boolean edge = Math.abs(x) == 4 || Math.abs(z) == 4;
				safeSet(level, center.offset(x, 0, z), x == 0 && z == 0 ? core : shell);
				for (int y = 1; y <= 3; y++) {
					if (edge) {
						safeSet(level, center.offset(x, y, z), (x + z + y) % 4 == 0 ? accent : shell);
					} else {
						safeSet(level, center.offset(x, y, z), Blocks.AIR);
					}
				}
				safeSet(level, center.offset(x, 4, z), edge || Math.abs(x) + Math.abs(z) <= 5 ? accent : Blocks.AIR);
			}
		}
		for (int x = -1; x <= 1; x++) {
			for (int y = 1; y <= 2; y++) {
				safeSet(level, center.offset(x, y, -4), Blocks.AIR);
			}
		}
		safeSet(level, center.above(2), Blocks.OCHRE_FROGLIGHT);
		if (type == BuildingType.MARKET) {
			placeColonyLedger(level, center.offset(1, 1, 1));
		}
	}

	public static void placeColonyLedger(ServerLevel level, BlockPos pos) {
		safeSet(level, pos, ModBlocks.COLONY_LEDGER);
	}

	private static void placeChitinFarm(ServerLevel level, BlockPos center) {
		for (int x = -5; x <= 5; x++) {
			for (int z = -3; z <= 3; z++) {
				BlockPos pos = center.offset(x, 0, z);
				if (Math.abs(x) == 5 || Math.abs(z) == 3) {
					safeSet(level, pos, Blocks.ROOTED_DIRT);
				} else {
					safeSet(level, pos, ModBlocks.CHITIN_BED);
				}
			}
		}
		safeSet(level, center.offset(0, 0, -4), ModBlocks.NURSERY_CHAMBER);
	}

	private static void placeWatchPost(ServerLevel level, BlockPos center) {
		safeSet(level, center, ModBlocks.WATCH_POST);
		safeSet(level, center.above(), Blocks.COBBLED_DEEPSLATE_WALL);
		safeSet(level, center.above(2), Blocks.OCHRE_FROGLIGHT);
		for (BlockPos pos : new BlockPos[] {center.north(), center.south(), center.east(), center.west()}) {
			safeSet(level, pos, Blocks.BONE_BLOCK);
		}
	}

	private static void placeDiplomacyShrine(ServerLevel level, BlockPos center) {
		placeCampusBuilding(level, center, BuildingType.DIPLOMACY_SHRINE);
		for (BlockPos pos : new BlockPos[] {center.north(2), center.south(2), center.east(2), center.west(2)}) {
			safeSet(level, pos, Blocks.CHISELED_TUFF);
			safeSet(level, pos.above(), Blocks.CANDLE);
		}
		safeSet(level, center.above(), Blocks.HONEY_BLOCK);
	}

	private static void placeFungusGarden(ServerLevel level, BlockPos center) {
		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance <= 7) {
					safeSet(level, center.offset(x, 0, z), distance <= 1 ? ModBlocks.FUNGUS_GARDEN : Blocks.MYCELIUM);
				}
				if (distance == 5) {
					safeSet(level, center.offset(x, 1, z), (x + z) % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
				}
			}
		}
		safeSet(level, center.above(), Blocks.OCHRE_FROGLIGHT);
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
			default -> ModBlocks.NEST_MOUND;
		};
	}

	private static Block shellBlock(BuildingType type) {
		return switch (type) {
			case MINE, ARMORY -> Blocks.COBBLED_DEEPSLATE;
			case BARRACKS, VENOM_PRESS -> Blocks.MUD_BRICKS;
			case PHEROMONE_ARCHIVE, DIPLOMACY_SHRINE -> Blocks.CHISELED_TUFF;
			case NURSERY -> Blocks.HONEYCOMB_BLOCK;
			default -> Blocks.PACKED_MUD;
		};
	}

	private static Block accentBlock(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> Blocks.BROWN_MUSHROOM_BLOCK;
			case NURSERY -> Blocks.BONE_BLOCK;
			case MINE -> Blocks.IRON_ORE;
			case BARRACKS, ARMORY -> Blocks.POLISHED_DEEPSLATE;
			case MARKET -> Blocks.OCHRE_FROGLIGHT;
			case DIPLOMACY_SHRINE, PHEROMONE_ARCHIVE -> Blocks.AMETHYST_BLOCK;
			case RESIN_DEPOT -> Blocks.HONEY_BLOCK;
			case VENOM_PRESS -> Blocks.SLIME_BLOCK;
			default -> Blocks.ROOTED_DIRT;
		};
	}
}
