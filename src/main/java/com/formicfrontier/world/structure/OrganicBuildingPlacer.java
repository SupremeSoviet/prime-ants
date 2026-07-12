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
 * Catalog and deterministic variant selector for the colony's organic role
 * buildings.
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
	public static final String PHEROMONE_ARCHIVE_A_RESOURCE = "formic_blueprints/pheromone_archive_a.json";
	public static final String PHEROMONE_ARCHIVE_B_RESOURCE = "formic_blueprints/pheromone_archive_b.json";
	public static final String ARMORY_A_RESOURCE = "formic_blueprints/armory_a.json";
	public static final String ARMORY_B_RESOURCE = "formic_blueprints/armory_b.json";
	public static final String DIPLOMACY_SHRINE_A_RESOURCE = "formic_blueprints/diplomacy_shrine_a.json";
	public static final String DIPLOMACY_SHRINE_B_RESOURCE = "formic_blueprints/diplomacy_shrine_b.json";
	public static final String RESIN_DEPOT_A_RESOURCE = "formic_blueprints/resin_depot_a.json";
	public static final String RESIN_DEPOT_B_RESOURCE = "formic_blueprints/resin_depot_b.json";
	public static final String FUNGUS_GARDEN_A_RESOURCE = "formic_blueprints/fungus_garden_a.json";
	public static final String FUNGUS_GARDEN_B_RESOURCE = "formic_blueprints/fungus_garden_b.json";
	public static final String VENOM_PRESS_A_RESOURCE = "formic_blueprints/venom_press_a.json";
	public static final String VENOM_PRESS_B_RESOURCE = "formic_blueprints/venom_press_b.json";
	public static final String WATCH_POST_A_RESOURCE = "formic_blueprints/watch_post_a.json";
	public static final String WATCH_POST_B_RESOURCE = "formic_blueprints/watch_post_b.json";
	public static final String WATCH_POST_C_RESOURCE = "formic_blueprints/watch_post_c.json";

	private static final Map<BuildingType, Family> FAMILIES = Map.ofEntries(
			Map.entry(BuildingType.FOOD_STORE, new Family(List.of(
					TieredMoundBlueprint.load(FOOD_STORE_A_RESOURCE),
					TieredMoundBlueprint.load(FOOD_STORE_B_RESOURCE)
			))),
			Map.entry(BuildingType.NURSERY, new Family(List.of(
					TieredMoundBlueprint.load(NURSERY_A_RESOURCE),
					TieredMoundBlueprint.load(NURSERY_B_RESOURCE)
			))),
			Map.entry(BuildingType.MINE, new Family(List.of(
					TieredMoundBlueprint.load(MINE_A_RESOURCE),
					TieredMoundBlueprint.load(MINE_B_RESOURCE)
			))),
			Map.entry(BuildingType.CHITIN_FARM, new Family(List.of(
					TieredMoundBlueprint.load(CHITIN_FARM_A_RESOURCE),
					TieredMoundBlueprint.load(CHITIN_FARM_B_RESOURCE),
					TieredMoundBlueprint.load(CHITIN_FARM_C_RESOURCE)
			))),
			Map.entry(BuildingType.BARRACKS, new Family(List.of(
					TieredMoundBlueprint.load(BARRACKS_A_RESOURCE),
					TieredMoundBlueprint.load(BARRACKS_B_RESOURCE)
			))),
			Map.entry(BuildingType.MARKET, new Family(List.of(
					TieredMoundBlueprint.load(MARKET_A_RESOURCE),
					TieredMoundBlueprint.load(MARKET_B_RESOURCE)
			))),
			Map.entry(BuildingType.PHEROMONE_ARCHIVE, new Family(List.of(
					TieredMoundBlueprint.load(PHEROMONE_ARCHIVE_A_RESOURCE),
					TieredMoundBlueprint.load(PHEROMONE_ARCHIVE_B_RESOURCE)
			))),
			Map.entry(BuildingType.ARMORY, new Family(List.of(
					TieredMoundBlueprint.load(ARMORY_A_RESOURCE),
					TieredMoundBlueprint.load(ARMORY_B_RESOURCE)
			))),
			Map.entry(BuildingType.DIPLOMACY_SHRINE, new Family(List.of(
					TieredMoundBlueprint.load(DIPLOMACY_SHRINE_A_RESOURCE),
					TieredMoundBlueprint.load(DIPLOMACY_SHRINE_B_RESOURCE)
			))),
			Map.entry(BuildingType.RESIN_DEPOT, new Family(List.of(
					TieredMoundBlueprint.load(RESIN_DEPOT_A_RESOURCE),
					TieredMoundBlueprint.load(RESIN_DEPOT_B_RESOURCE)
			))),
			Map.entry(BuildingType.FUNGUS_GARDEN, new Family(List.of(
					TieredMoundBlueprint.load(FUNGUS_GARDEN_A_RESOURCE),
					TieredMoundBlueprint.load(FUNGUS_GARDEN_B_RESOURCE)
			))),
			Map.entry(BuildingType.VENOM_PRESS, new Family(List.of(
					TieredMoundBlueprint.load(VENOM_PRESS_A_RESOURCE),
					TieredMoundBlueprint.load(VENOM_PRESS_B_RESOURCE)
			))),
			Map.entry(BuildingType.WATCH_POST, new Family(List.of(
					TieredMoundBlueprint.load(WATCH_POST_A_RESOURCE),
					TieredMoundBlueprint.load(WATCH_POST_B_RESOURCE),
					TieredMoundBlueprint.load(WATCH_POST_C_RESOURCE)
			)))
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
			case PHEROMONE_ARCHIVE -> ModBlocks.PHEROMONE_ARCHIVE;
			case ARMORY -> ModBlocks.ARMORY;
			case DIPLOMACY_SHRINE -> ModBlocks.DIPLOMACY_SHRINE;
			case RESIN_DEPOT -> ModBlocks.RESIN_DEPOT;
			case FUNGUS_GARDEN -> ModBlocks.FUNGUS_GARDEN;
			case VENOM_PRESS -> ModBlocks.VENOM_PRESS;
			case WATCH_POST -> ModBlocks.WATCH_POST;
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
