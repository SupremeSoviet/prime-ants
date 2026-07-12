package com.formicfrontier.world.structure;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Deterministic compiler from a compact tier blueprint to Minecraft blocks. */
public final class TieredMoundPlacer {
	public static final String QUEEN_STAGE_ONE_RESOURCE = "formic_blueprints/queen_mound_stage_1.json";
	private static final TieredMoundBlueprint QUEEN_STAGE_ONE = TieredMoundBlueprint.load(QUEEN_STAGE_ONE_RESOURCE);
	private static final Block TUNNEL_BACK = Blocks.MUD;

	private TieredMoundPlacer() {
	}

	public static TieredMoundBlueprint queenStageOneBlueprint() {
		return QUEEN_STAGE_ONE;
	}

	public static void placeQueenStageOne(ServerLevel level, BlockPos center) {
		place(level, center, QUEEN_STAGE_ONE);
		StructurePlacer.safeSet(level, center.below(), ModBlocks.NEST_CORE);
		StructurePlacer.safeSet(level, center, ModBlocks.NEST_MOUND);
	}

	public static void place(ServerLevel level, BlockPos center, TieredMoundBlueprint blueprint) {
		for (int y = 0; y <= blueprint.maxY(); y++) {
			for (int x = blueprint.minX(); x <= blueprint.maxX(); x++) {
				for (int z = blueprint.minZ(); z <= blueprint.maxZ(); z++) {
					if (!blueprint.isSolid(x, y, z)) {
						continue;
					}
					StructurePlacer.safeSet(level, center.offset(x, y, z), materialFor(blueprint, x, y, z));
				}
			}
		}

		for (TieredMoundBlueprint.Mouth mouth : blueprint.mouths()) {
			carveMouth(level, center, blueprint, mouth);
		}
		for (TieredMoundBlueprint.Chamber chamber : blueprint.chambers()) {
			carveChamber(level, center, blueprint, chamber);
		}
		for (TieredMoundBlueprint.Pit pit : blueprint.pits()) {
			carvePit(level, center, blueprint, pit);
		}
		for (TieredMoundBlueprint.Connection connection : blueprint.connections()) {
			placeConnection(level, center, blueprint, connection);
		}
		for (TieredMoundBlueprint.Chamber chamber : blueprint.chambers()) {
			decorateChamber(level, center, chamber);
		}

	}

	private static void carveMouth(ServerLevel level, BlockPos center, TieredMoundBlueprint blueprint,
			TieredMoundBlueprint.Mouth mouth) {
		int halfWidth = mouth.width() / 2;
		for (int x = mouth.x() - halfWidth; x <= mouth.x() + halfWidth; x++) {
			for (int y = mouth.y(); y <= mouth.topY(); y++) {
				for (int z = mouth.frontZ(); z < mouth.rearZ(); z++) {
					if (mouth.carves(x, y, z)) {
						StructurePlacer.safeSet(level, center.offset(x, y, z), Blocks.AIR);
					}
				}
				if (blueprint.contains(x, y, mouth.rearZ())) {
					StructurePlacer.safeSet(level, center.offset(x, y, mouth.rearZ()), TUNNEL_BACK);
				}
			}
		}

		if (mouth.y() == 1) {
			for (int z = mouth.frontZ() - 3; z <= mouth.frontZ(); z++) {
				StructurePlacer.safeSet(level, center.offset(mouth.x(), 0, z), Blocks.DIRT_PATH);
				for (int y = 1; y <= 3; y++) {
					StructurePlacer.safeSet(level, center.offset(mouth.x(), y, z), Blocks.AIR);
				}
			}
		}
	}

	private static void placeConnection(ServerLevel level, BlockPos center, TieredMoundBlueprint blueprint,
			TieredMoundBlueprint.Connection connection) {
		TieredMoundBlueprint.Chamber from = chamber(blueprint, connection.from());
		TieredMoundBlueprint.Chamber to = chamber(blueprint, connection.to());
		int rise = to.floorY() - from.floorY();
		int sideX = -connection.dz();
		int sideZ = connection.dx();
		Direction facing = switch (connection.direction()) {
			case "east" -> Direction.EAST;
			case "west" -> Direction.WEST;
			case "north" -> Direction.NORTH;
			case "south" -> Direction.SOUTH;
			default -> throw new IllegalArgumentException("Unsupported stair direction " + connection.direction());
		};
		BlockState stair = Blocks.MUD_BRICK_STAIRS.defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, facing);
		for (int step = 0; step < rise; step++) {
			for (int lane = 0; lane < connection.width(); lane++) {
				int x = connection.startX() + connection.dx() * step + sideX * lane;
				int y = from.floorY() + step;
				int z = connection.startZ() + connection.dz() * step + sideZ * lane;
				if (y > from.floorY()) {
					StructurePlacer.safeSet(level, center.offset(x, y - 1, z), Blocks.PACKED_MUD);
				}
				StructurePlacer.safeSet(level, center.offset(x, y + 1, z), Blocks.AIR);
				StructurePlacer.safeSet(level, center.offset(x, y + 2, z), Blocks.AIR);
				StructurePlacer.safeSet(level, center.offset(x, y, z), stair);
			}
		}
		int landingX = connection.startX() + connection.dx() * rise;
		int landingZ = connection.startZ() + connection.dz() * rise;
		StructurePlacer.safeSet(level, center.offset(landingX, to.floorY() + 1, landingZ), Blocks.AIR);
		StructurePlacer.safeSet(level, center.offset(landingX, to.floorY() + 2, landingZ), Blocks.AIR);
	}

	private static TieredMoundBlueprint.Chamber chamber(TieredMoundBlueprint blueprint, String id) {
		return blueprint.chambers().stream().filter(chamber -> chamber.id().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown chamber " + id));
	}

	private static void carveChamber(ServerLevel level, BlockPos center, TieredMoundBlueprint blueprint,
			TieredMoundBlueprint.Chamber chamber) {
		int minX = (int) Math.floor(chamber.x() - chamber.radiusX()) - 1;
		int maxX = (int) Math.ceil(chamber.x() + chamber.radiusX()) + 1;
		int minZ = (int) Math.floor(chamber.z() - chamber.radiusZ()) - 1;
		int maxZ = (int) Math.ceil(chamber.z() + chamber.radiusZ()) + 1;
		for (int y = chamber.floorY() + 1; y <= chamber.topY(); y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (chamber.carves(x, y, z) && blueprint.contains(x, y, z)) {
						StructurePlacer.safeSet(level, center.offset(x, y, z), Blocks.AIR);
					}
				}
			}
		}
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (chamber.containsFloor(x, z) && blueprint.isSolid(x, chamber.floorY(), z)) {
					StructurePlacer.safeSet(level, center.offset(x, chamber.floorY(), z), Blocks.PACKED_MUD);
				}
			}
		}
	}

	private static void carvePit(ServerLevel level, BlockPos center, TieredMoundBlueprint blueprint,
			TieredMoundBlueprint.Pit pit) {
		TieredMoundBlueprint.Chamber owner = chamber(blueprint, pit.chamber());
		int minX = (int) Math.floor(pit.x() - pit.radiusX());
		int maxX = (int) Math.ceil(pit.x() + pit.radiusX());
		int minZ = (int) Math.floor(pit.z() - pit.radiusZ());
		int maxZ = (int) Math.ceil(pit.z() + pit.radiusZ());
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int depth = pit.depthAt(x, z);
				if (depth == 0) {
					continue;
				}
				for (int y = owner.floorY(); y > owner.floorY() - depth; y--) {
					StructurePlacer.safeSet(level, center.offset(x, y, z), Blocks.AIR);
				}
				int bottomY = pit.bottomY(x, z, owner);
				int roll = Math.floorMod(x * 31 + z * 17 + blueprint.seed(), 7);
				StructurePlacer.safeSet(level, center.offset(x, bottomY, z),
						roll == 0 ? Blocks.IRON_ORE : Blocks.COBBLED_DEEPSLATE);
			}
		}
		StructurePlacer.safeSet(level, center.offset(pit.x(), owner.floorY() - pit.depth(), pit.z()), ModBlocks.ORE_NODE);
	}

	private static void decorateChamber(ServerLevel level, BlockPos center, TieredMoundBlueprint.Chamber chamber) {
		switch (chamber.purpose()) {
			case "queen_hall" -> {
				placeDecoration(level, center, chamber, -4, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 4, 1, 0, Blocks.BARREL);
				placeDecoration(level, center, chamber, -3, 1, 3, Blocks.BARREL);
				placeDecoration(level, center, chamber, 3, 1, 3, Blocks.CRAFTING_TABLE);
				placeDecoration(level, center, chamber, 0, 1, 3, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, -4, 2, 0, Blocks.LANTERN);
			}
			case "storage" -> {
				placeDecoration(level, center, chamber, -3, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 3, 1, 0, Blocks.BARREL);
				placeDecoration(level, center, chamber, -3, 1, 2, Blocks.HAY_BLOCK);
				placeDecoration(level, center, chamber, 3, 1, 2, Blocks.COMPOSTER);
				placeDecoration(level, center, chamber, -3, 2, 0, Blocks.LANTERN);
			}
			case "lookout" -> {
				placeDecoration(level, center, chamber, 0, 1, 2, ModBlocks.PHEROMONE_ARCHIVE);
				placeDecoration(level, center, chamber, -2, 1, 1, Blocks.OAK_FENCE);
				placeDecoration(level, center, chamber, -2, 2, 1, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 2, 1, 1, Blocks.BELL);
			}
			case "food_store" -> {
				// Keep a clear central aisle from the south-facing mouth while both
				// walls read as a busy, inhabited granary rather than a marker room.
				placeDecoration(level, center, chamber, -4, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 4, 1, 0, Blocks.BARREL);
				placeDecoration(level, center, chamber, -3, 1, 3, Blocks.HAY_BLOCK);
				placeDecoration(level, center, chamber, 3, 1, 3, Blocks.COMPOSTER);
				placeDecoration(level, center, chamber, 0, 1, 3, ModBlocks.FOOD_NODE);
				placeDecoration(level, center, chamber, -4, 2, 0, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 4, 2, 0, Blocks.LANTERN);
			}
			case "nursery" -> {
				// The brood room uses paired resting alcoves and warm incubation
				// materials, leaving the entrance-to-back-wall axis unobstructed.
				placeDecoration(level, center, chamber, -4, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, 4, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, -3, 1, 3, Blocks.HONEYCOMB_BLOCK);
				placeDecoration(level, center, chamber, 3, 1, 3, Blocks.BONE_BLOCK);
				placeDecoration(level, center, chamber, -2, 1, 4, Blocks.BARREL);
				placeDecoration(level, center, chamber, 2, 1, 4, Blocks.OCHRE_FROGLIGHT);
				placeDecoration(level, center, chamber, -4, 2, 0, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 4, 2, 0, Blocks.LANTERN);
			}
			case "mine" -> {
				placeDecoration(level, center, chamber, -4, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 4, 1, 0, Blocks.BARREL);
				placeDecoration(level, center, chamber, -3, 1, 3, Blocks.COBBLED_DEEPSLATE_WALL);
				placeDecoration(level, center, chamber, 3, 1, 3, Blocks.IRON_ORE);
				placeDecoration(level, center, chamber, -2, 2, 3, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 2, 2, 3, Blocks.LANTERN);
			}
			case "chitin_farm" -> {
				placeDecoration(level, center, chamber, -4, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, 4, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, -3, 1, 2, Blocks.BONE_BLOCK);
				placeDecoration(level, center, chamber, 3, 1, 2, Blocks.HONEYCOMB_BLOCK);
				placeDecoration(level, center, chamber, -1, 1, 3, ModBlocks.CHITIN_NODE);
				placeDecoration(level, center, chamber, 1, 1, 3, Blocks.COMPOSTER);
				placeDecoration(level, center, chamber, -4, 2, 0, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 4, 2, 0, Blocks.LANTERN);
			}
			case "barracks" -> {
				placeDecoration(level, center, chamber, -6, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, 6, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, -5, 1, 3, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, 5, 1, 3, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, -3, 1, 4, Blocks.ANVIL);
				placeDecoration(level, center, chamber, 0, 1, 4, Blocks.SMITHING_TABLE);
				placeDecoration(level, center, chamber, 3, 1, 4, Blocks.TARGET);
				placeDecoration(level, center, chamber, -6, 3, 0, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 6, 3, 0, Blocks.LANTERN);
			}
			case "market" -> {
				placeDecoration(level, center, chamber, -4, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 4, 1, 0, Blocks.BARREL);
				placeDecoration(level, center, chamber, -3, 1, 3, Blocks.HAY_BLOCK);
				placeDecoration(level, center, chamber, 3, 1, 3, Blocks.COMPOSTER);
				placeDecoration(level, center, chamber, 0, 1, 4, Blocks.BELL);
				placeDecoration(level, center, chamber, -3, 1, -2, Blocks.OAK_FENCE);
				placeDecoration(level, center, chamber, -3, 2, -2, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 3, 1, -2, Blocks.OAK_FENCE);
				placeDecoration(level, center, chamber, 3, 2, -2, Blocks.LANTERN);
			}
			default -> throw new IllegalArgumentException("Unsupported chamber purpose " + chamber.purpose());
		}
	}

	private static void placeDecoration(ServerLevel level, BlockPos center, TieredMoundBlueprint.Chamber chamber,
			int offsetX, int offsetY, int offsetZ, Block block) {
		int x = chamber.x() + offsetX;
		int y = chamber.floorY() + offsetY;
		int z = chamber.z() + offsetZ;
		if (!chamber.carves(x, y, z)) {
			throw new IllegalArgumentException("Decoration " + block + " falls outside chamber " + chamber.id());
		}
		StructurePlacer.safeSet(level, center.offset(x, y, z), block);
	}

	private static Block materialFor(TieredMoundBlueprint blueprint, int x, int y, int z) {
		if (!isSurface(blueprint, x, y, z)) {
			return ModBlocks.NEST_MOUND;
		}
		int roll = Math.floorMod(x * 73428767 ^ y * 912931 ^ z * 43828933 ^ blueprint.seed() * 199999, 100);
		return switch (blueprint.palette()) {
			case "earth" -> {
				if (roll < 72) yield ModBlocks.NEST_MOUND;
				if (roll < 88) yield Blocks.ROOTED_DIRT;
				if (roll < 97) yield Blocks.COARSE_DIRT;
				yield Blocks.MANGROVE_ROOTS;
			}
			case "food_store" -> {
				if (roll < 62) yield ModBlocks.NEST_MOUND;
				if (roll < 78) yield Blocks.ROOTED_DIRT;
				if (roll < 88) yield Blocks.COARSE_DIRT;
				if (roll < 95) yield Blocks.MOSS_BLOCK;
				yield Blocks.MANGROVE_ROOTS;
			}
			case "nursery" -> {
				if (roll < 62) yield ModBlocks.NEST_MOUND;
				if (roll < 78) yield Blocks.ROOTED_DIRT;
				if (roll < 89) yield Blocks.PACKED_MUD;
				if (roll < 97) yield Blocks.MUD;
				yield Blocks.MANGROVE_ROOTS;
			}
			case "mine" -> {
				if (roll < 50) yield ModBlocks.NEST_MOUND;
				if (roll < 65) yield Blocks.ROOTED_DIRT;
				if (roll < 78) yield Blocks.STONE;
				if (roll < 89) yield Blocks.COBBLED_DEEPSLATE;
				if (roll < 97) yield Blocks.DEEPSLATE;
				yield Blocks.IRON_ORE;
			}
			case "chitin_farm" -> {
				if (roll < 60) yield ModBlocks.NEST_MOUND;
				if (roll < 75) yield Blocks.ROOTED_DIRT;
				if (roll < 86) yield Blocks.PACKED_MUD;
				if (roll < 94) yield Blocks.BONE_BLOCK;
				if (roll < 98) yield Blocks.HONEYCOMB_BLOCK;
				yield Blocks.MANGROVE_ROOTS;
			}
			case "barracks" -> {
				if (roll < 55) yield ModBlocks.NEST_MOUND;
				if (roll < 70) yield Blocks.ROOTED_DIRT;
				if (roll < 82) yield Blocks.PACKED_MUD;
				if (roll < 92) yield Blocks.MUD_BRICKS;
				if (roll < 98) yield Blocks.TUFF;
				yield Blocks.IRON_ORE;
			}
			case "market" -> {
				if (roll < 55) yield ModBlocks.NEST_MOUND;
				if (roll < 70) yield Blocks.ROOTED_DIRT;
				if (roll < 82) yield Blocks.COARSE_DIRT;
				if (roll < 90) yield Blocks.MOSS_BLOCK;
				if (roll < 96) yield Blocks.CUT_COPPER;
				yield Blocks.HONEYCOMB_BLOCK;
			}
			default -> throw new IllegalArgumentException("Unsupported material palette " + blueprint.palette());
		};
	}

	private static boolean isSurface(TieredMoundBlueprint blueprint, int x, int y, int z) {
		return !blueprint.isSolid(x + 1, y, z)
				|| !blueprint.isSolid(x - 1, y, z)
				|| !blueprint.isSolid(x, y + 1, z)
				|| !blueprint.isSolid(x, y - 1, z)
				|| !blueprint.isSolid(x, y, z + 1)
				|| !blueprint.isSolid(x, y, z - 1);
	}
}
