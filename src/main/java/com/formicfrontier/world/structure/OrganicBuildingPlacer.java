package com.formicfrontier.world.structure;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * Catalog and deterministic variant selector for the colony's single-storey
 * organic buildings.
 *
 * <p>Each family reuses the validated tier/chamber/mouth vocabulary of the
 * central mound. A family may expose several compact JSON silhouettes; the
 * world position selects one stably so repeated buildings do not look cloned.</p>
 */
public final class OrganicBuildingPlacer {
	public static final String FOOD_STORE_A_RESOURCE = "formic_blueprints/food_store_a.json";
	public static final String FOOD_STORE_B_RESOURCE = "formic_blueprints/food_store_b.json";
	public static final String NURSERY_A_RESOURCE = "formic_blueprints/nursery_a.json";
	public static final String NURSERY_B_RESOURCE = "formic_blueprints/nursery_b.json";
	public static final String MINE_A_RESOURCE = "formic_blueprints/mine_a.json";
	public static final String MINE_B_RESOURCE = "formic_blueprints/mine_b.json";
	public static final String CHITIN_FARM_A_RESOURCE = "formic_blueprints/chitin_farm_a.json";
	public static final String CHITIN_FARM_B_RESOURCE = "formic_blueprints/chitin_farm_b.json";
	public static final String CHITIN_FARM_C_RESOURCE = "formic_blueprints/chitin_farm_c.json";
	public static final String BARRACKS_A_RESOURCE = "formic_blueprints/barracks_a.json";
	public static final String BARRACKS_B_RESOURCE = "formic_blueprints/barracks_b.json";
	public static final String MARKET_A_RESOURCE = "formic_blueprints/market_a.json";
	public static final String MARKET_B_RESOURCE = "formic_blueprints/market_b.json";

	private static final Map<BuildingType, Family> FAMILIES = Map.of(
			BuildingType.FOOD_STORE,
			new Family(List.of(
					TieredMoundBlueprint.load(FOOD_STORE_A_RESOURCE),
					TieredMoundBlueprint.load(FOOD_STORE_B_RESOURCE)
			)),
			BuildingType.NURSERY,
			new Family(List.of(
					TieredMoundBlueprint.load(NURSERY_A_RESOURCE),
					TieredMoundBlueprint.load(NURSERY_B_RESOURCE)
			)),
			BuildingType.MINE,
			new Family(List.of(
					TieredMoundBlueprint.load(MINE_A_RESOURCE),
					TieredMoundBlueprint.load(MINE_B_RESOURCE)
			)),
			BuildingType.CHITIN_FARM,
			new Family(List.of(
					TieredMoundBlueprint.load(CHITIN_FARM_A_RESOURCE),
					TieredMoundBlueprint.load(CHITIN_FARM_B_RESOURCE),
					TieredMoundBlueprint.load(CHITIN_FARM_C_RESOURCE)
			)),
			BuildingType.BARRACKS,
			new Family(List.of(
					TieredMoundBlueprint.load(BARRACKS_A_RESOURCE),
					TieredMoundBlueprint.load(BARRACKS_B_RESOURCE)
			)),
			BuildingType.MARKET,
			new Family(List.of(
					TieredMoundBlueprint.load(MARKET_A_RESOURCE),
					TieredMoundBlueprint.load(MARKET_B_RESOURCE)
			))
	);

	private OrganicBuildingPlacer() {
	}

	public static boolean supports(BuildingType type) {
		return FAMILIES.containsKey(type);
	}

	public static List<TieredMoundBlueprint> variants(BuildingType type) {
		return family(type).variants();
	}

	public static TieredMoundBlueprint blueprintFor(BuildingType type, BlockPos center) {
		List<TieredMoundBlueprint> variants = variants(type);
		return variants.get(variantIndex(center, variants.size()));
	}

	public static void place(ServerLevel level, BlockPos center, BuildingType type) {
		TieredMoundPlacer.place(level, center, blueprintFor(type, center));
		StructurePlacer.safeSet(level, center, functionalBlock(type));
	}

	static int variantIndex(BlockPos center, int variantCount) {
		if (variantCount < 1) {
			throw new IllegalArgumentException("An organic building family needs at least one variant");
		}
		// Deliberately simple and inspectable: colony layout coordinates can be
		// authored so neighbouring repeated sites select different variants.
		return Math.floorMod(center.getX() * 31 + center.getZ() * 17, variantCount);
	}

	private static Family family(BuildingType type) {
		Family family = FAMILIES.get(type);
		if (family == null) {
			throw new IllegalArgumentException("No organic building family registered for " + type);
		}
		return family;
	}

	private static Block functionalBlock(BuildingType type) {
		return switch (type) {
			case FOOD_STORE -> ModBlocks.FOOD_CHAMBER;
			case NURSERY -> ModBlocks.NURSERY_CHAMBER;
			case MINE -> ModBlocks.MINE_CHAMBER;
			case CHITIN_FARM -> ModBlocks.CHITIN_BED;
			case BARRACKS -> ModBlocks.BARRACKS_CHAMBER;
			case MARKET -> ModBlocks.MARKET_CHAMBER;
			default -> throw new IllegalArgumentException("No functional block registered for " + type);
		};
	}

	private record Family(List<TieredMoundBlueprint> variants) {
		private Family {
			variants = List.copyOf(variants);
			if (variants.isEmpty()) {
				throw new IllegalArgumentException("An organic building family needs at least one variant");
			}
		}
	}
}
