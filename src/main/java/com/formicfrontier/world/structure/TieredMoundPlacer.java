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
		for (TieredMoundBlueprint.Connection connection : blueprint.connections()) {
			placeConnection(level, center, blueprint, connection);
		}
		for (TieredMoundBlueprint.Chamber chamber : blueprint.chambers()) {
			decorateChamber(level, center, chamber);
		}

		StructurePlacer.safeSet(level, center.below(), ModBlocks.NEST_CORE);
		StructurePlacer.safeSet(level, center, ModBlocks.NEST_MOUND);
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
				if (chamber.containsFloor(x, z) && blueprint.contains(x, chamber.floorY(), z)) {
					StructurePlacer.safeSet(level, center.offset(x, chamber.floorY(), z), Blocks.PACKED_MUD);
				}
			}
		}
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
		if (roll < 72) {
			return ModBlocks.NEST_MOUND;
		}
		if (roll < 88) {
			return Blocks.ROOTED_DIRT;
		}
		if (roll < 97) {
			return Blocks.COARSE_DIRT;
		}
		return Blocks.MANGROVE_ROOTS;
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
