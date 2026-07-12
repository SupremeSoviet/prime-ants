package com.formicfrontier.world;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.world.structure.OrganicBuildingPlacer;
import com.formicfrontier.world.structure.TieredMoundPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minimal structure baseline for the from-scratch anthill rebuild.
 *
 * <p>The previous procedural and schematic generators deliberately do not live
 * behind a feature flag: they were removed so new mound work cannot accidentally
 * inherit their geometry. Migrated families use the new validated blueprint
 * compiler; only not-yet-rebuilt families remain one-block functional
 * markers.</p>
 */
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
			safeSet(level, center, Blocks.DIRT_PATH);
			return;
		}
		if (stage == BuildingVisualStage.PLANNED) {
			// A plan is only a ground pin; the functional block appears once work starts.
			safeSet(level, center, Blocks.DIRT_PATH);
			return;
		}
		if (type == BuildingType.QUEEN_CHAMBER) {
			placeQueenHall(level, center, culture);
			return;
		}
		if (type == BuildingType.GREAT_MOUND) {
			placeGreatMoundProject(level, center, culture);
			return;
		}
		if (OrganicBuildingPlacer.supports(type)
				&& (stage == BuildingVisualStage.COMPLETE || stage == BuildingVisualStage.UPGRADED)) {
			OrganicBuildingPlacer.place(level, center, type);
			return;
		}
		safeSet(level, center, markerBlock(type));
	}

	public static boolean safeSet(ServerLevel level, BlockPos pos, Block block) {
		return safeSet(level, pos, block.defaultBlockState());
	}

	public static boolean safeSet(ServerLevel level, BlockPos pos, BlockState state) {
		if (!canReplace(level, pos)) {
			return false;
		}
		level.setBlockAndUpdate(pos, state);
		return true;
	}

	/**
	 * Shared replacement policy used by colony paths, event markers and the new
	 * placeholder sites. It intentionally preserves block entities.
	 */
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
				|| block == Blocks.MUD_BRICKS
				|| block == Blocks.MUD_BRICK_STAIRS
				|| block == Blocks.GRAVEL
				|| block == Blocks.STONE
				|| block == Blocks.COBBLESTONE
				|| block == Blocks.MOSSY_COBBLESTONE
				|| block == Blocks.DEEPSLATE
				|| block == Blocks.COBBLED_DEEPSLATE
				|| block == Blocks.MOSS_BLOCK
				|| block == Blocks.MYCELIUM
				|| block == Blocks.DIRT_PATH
				|| block == Blocks.MANGROVE_ROOTS
				|| block == Blocks.MUDDY_MANGROVE_ROOTS
				|| block == Blocks.MANGROVE_PLANKS
				|| block == Blocks.CUT_COPPER
				|| block == Blocks.TUFF
				|| block == Blocks.CHISELED_TUFF
				|| block == Blocks.RED_TERRACOTTA
				|| block == Blocks.BLACKSTONE
				|| block == Blocks.POLISHED_BLACKSTONE
				|| block == Blocks.POLISHED_DEEPSLATE
				|| block == Blocks.HONEYCOMB_BLOCK
				|| block == Blocks.HONEY_BLOCK
				|| block == Blocks.BROWN_MUSHROOM_BLOCK
				|| block == Blocks.RED_MUSHROOM_BLOCK
				|| block == Blocks.COBBLED_DEEPSLATE_WALL
				|| block == Blocks.IRON_ORE
				|| block == Blocks.DEEPSLATE_IRON_ORE
				|| block == Blocks.BONE_BLOCK
				|| block == Blocks.OCHRE_FROGLIGHT
				|| block == Blocks.AMETHYST_BLOCK
				|| block == Blocks.CANDLE
				|| block == Blocks.LANTERN
				|| block == Blocks.BARREL
				|| block == Blocks.CRAFTING_TABLE
				|| block == Blocks.COMPOSTER
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
		TieredMoundPlacer.placeQueenStageOne(level, center);
	}

	public static void placeGreatMoundProject(ServerLevel level, BlockPos center, ColonyCulture culture) {
		TieredMoundPlacer.placeQueenStageTwo(level, center);
	}

	public static void placeQueenVault(ServerLevel level, BlockPos center, ColonyCulture culture) {
		safeSet(level, center, ModBlocks.NEST_CORE);
	}

	public static void placeTradeHub(ServerLevel level, BlockPos center, ColonyCulture culture) {
		safeSet(level, center, ModBlocks.MARKET_CHAMBER);
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type) {
		placeCampusBuilding(level, center, type, ColonyCulture.AMBER);
	}

	public static void placeCampusBuilding(ServerLevel level, BlockPos center, BuildingType type, ColonyCulture culture) {
		safeSet(level, center, markerBlock(type));
	}

	public static void placeStagedBuilding(ServerLevel level, BlockPos center, BuildingType type, BuildingVisualStage stage) {
		placeStagedBuilding(level, center, type, stage, ColonyCulture.AMBER);
	}

	public static void placeStagedBuilding(ServerLevel level, BlockPos center, BuildingType type, BuildingVisualStage stage, ColonyCulture culture) {
		placeBuilding(level, center, type, stage, culture);
	}

	public static void placeColonyLedger(ServerLevel level, BlockPos pos) {
		safeSet(level, pos, ModBlocks.COLONY_LEDGER);
	}

	private static Block markerBlock(BuildingType type) {
		return switch (type) {
			case QUEEN_CHAMBER, GREAT_MOUND -> ModBlocks.NEST_MOUND;
			case FOOD_STORE -> ModBlocks.FOOD_CHAMBER;
			case NURSERY -> ModBlocks.NURSERY_CHAMBER;
			case MINE -> ModBlocks.MINE_CHAMBER;
			case CHITIN_FARM -> ModBlocks.CHITIN_BED;
			case BARRACKS -> ModBlocks.BARRACKS_CHAMBER;
			case MARKET, TRADE_HUB -> ModBlocks.MARKET_CHAMBER;
			case DIPLOMACY_SHRINE -> ModBlocks.DIPLOMACY_SHRINE;
			case WATCH_POST -> ModBlocks.WATCH_POST;
			case RESIN_DEPOT -> ModBlocks.RESIN_DEPOT;
			case PHEROMONE_ARCHIVE -> ModBlocks.PHEROMONE_ARCHIVE;
			case FUNGUS_GARDEN -> ModBlocks.FUNGUS_GARDEN;
			case VENOM_PRESS -> ModBlocks.VENOM_PRESS;
			case ARMORY -> ModBlocks.ARMORY;
			case QUEEN_VAULT -> ModBlocks.NEST_CORE;
			case ROAD -> Blocks.DIRT_PATH;
		};
	}
}
