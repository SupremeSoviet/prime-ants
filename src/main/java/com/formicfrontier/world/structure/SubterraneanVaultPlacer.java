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

/** Deterministic compiler for protected rooms beneath an existing queen mound. */
public final class SubterraneanVaultPlacer {
	public static final String QUEEN_VAULT_RESOURCE = "formic_blueprints/queen_vault.json";
	private static final SubterraneanVaultBlueprint QUEEN_VAULT = SubterraneanVaultBlueprint.load(QUEEN_VAULT_RESOURCE);

	private SubterraneanVaultPlacer() {
	}

	public static SubterraneanVaultBlueprint queenVaultBlueprint() {
		return QUEEN_VAULT;
	}

	public static void placeQueenVault(ServerLevel level, BlockPos center) {
		place(level, center, QUEEN_VAULT);
	}

	public static void place(ServerLevel level, BlockPos center, SubterraneanVaultBlueprint blueprint) {
		for (int y = blueprint.minY(); y <= blueprint.maxY(); y++) {
			for (int x = blueprint.minX(); x <= blueprint.maxX(); x++) {
				for (int z = blueprint.minZ(); z <= blueprint.maxZ(); z++) {
					if (blueprint.isSolid(x, y, z)) {
						placeStructuralBlock(level, center.offset(x, y, z), materialFor(blueprint, x, y, z).defaultBlockState());
					}
				}
			}
		}

		for (SubterraneanVaultBlueprint.Chamber chamber : blueprint.chambers()) {
			carveChamber(level, center, chamber);
		}
		SubterraneanVaultBlueprint.SurfaceAccess access = blueprint.surfaceAccess();
		SubterraneanVaultBlueprint.Chamber accessTarget = blueprint.chamber(access.to());
		placeDescent(level, center, access.startX(), access.startZ(), access.surfaceFloorY(),
				access.drop(accessTarget), access.dx(), access.dz(), access.width(), accessTarget.floorY());
		for (SubterraneanVaultBlueprint.Descent connection : blueprint.connections()) {
			SubterraneanVaultBlueprint.Chamber from = blueprint.chamber(connection.from());
			SubterraneanVaultBlueprint.Chamber to = blueprint.chamber(connection.to());
			placeDescent(level, center, connection.startX(), connection.startZ(), from.floorY(),
					connection.drop(from, to), connection.dx(), connection.dz(), connection.width(), to.floorY());
		}
		for (SubterraneanVaultBlueprint.Chamber chamber : blueprint.chambers()) {
			decorateChamber(level, center, chamber);
		}
		// A queued in-place project temporarily marks the shared centre with a dirt
		// path. Restore the Great Mound surface and its functional queen-core anchor
		// after the underground compiler finishes around them.
		placeStructuralBlock(level, center, ModBlocks.NEST_MOUND.defaultBlockState());
		placeStructuralBlock(level, center.below(), ModBlocks.NEST_CORE.defaultBlockState());
	}

	private static void carveChamber(ServerLevel level, BlockPos center, SubterraneanVaultBlueprint.Chamber chamber) {
		int minX = (int) Math.floor(chamber.x() - chamber.radiusX()) - 1;
		int maxX = (int) Math.ceil(chamber.x() + chamber.radiusX()) + 1;
		int minZ = (int) Math.floor(chamber.z() - chamber.radiusZ()) - 1;
		int maxZ = (int) Math.ceil(chamber.z() + chamber.radiusZ()) + 1;
		for (int y = chamber.floorY() + 1; y <= chamber.topY(); y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (chamber.carves(x, y, z)) {
						StructurePlacer.safeCarve(level, center.offset(x, y, z));
					}
				}
			}
		}
	}

	private static void placeDescent(ServerLevel level, BlockPos center, int startX, int startZ, int startY,
			int drop, int dx, int dz, int width, int landingY) {
		int sideX = -dz;
		int sideZ = dx;
		Direction movement = direction(dx, dz);
		BlockState stair = Blocks.MUD_BRICK_STAIRS.defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, movement.getOpposite());
		for (int step = 0; step < drop; step++) {
			for (int lane = 0; lane < width; lane++) {
				int x = startX + dx * step + sideX * lane;
				int y = startY - step;
				int z = startZ + dz * step + sideZ * lane;
				placeStructuralBlock(level, center.offset(x, y - 1, z), Blocks.PACKED_MUD.defaultBlockState());
				StructurePlacer.safeCarve(level, center.offset(x, y + 1, z));
				StructurePlacer.safeCarve(level, center.offset(x, y + 2, z));
				placeStructuralBlock(level, center.offset(x, y, z), stair);
			}
		}
		int landingX = startX + dx * drop;
		int landingZ = startZ + dz * drop;
		StructurePlacer.safeCarve(level, center.offset(landingX, landingY + 1, landingZ));
		StructurePlacer.safeCarve(level, center.offset(landingX, landingY + 2, landingZ));
	}

	private static void placeStructuralBlock(ServerLevel level, BlockPos pos, BlockState state) {
		// Underground routes may cross granite, ores or other natural blocks that
		// the conservative surface-building replacement policy intentionally leaves
		// alone. Excavation can replace all of them, but still never destroys block
		// entities or bedrock.
		if (StructurePlacer.safeCarve(level, pos)) {
			StructurePlacer.safeSet(level, pos, state);
		}
	}

	private static Direction direction(int dx, int dz) {
		if (dx > 0) return Direction.EAST;
		if (dx < 0) return Direction.WEST;
		if (dz > 0) return Direction.SOUTH;
		return Direction.NORTH;
	}

	private static void decorateChamber(ServerLevel level, BlockPos center,
			SubterraneanVaultBlueprint.Chamber chamber) {
		switch (chamber.purpose()) {
			case "vault_guard" -> {
				placeDecoration(level, center, chamber, -4, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 4, 1, 0, Blocks.BARREL);
				placeDecoration(level, center, chamber, -3, 1, 3, Blocks.TARGET);
				placeDecoration(level, center, chamber, 3, 1, 3, Blocks.SMITHING_TABLE);
				placeDecoration(level, center, chamber, -1, 1, 3, Blocks.IRON_BARS);
				placeDecoration(level, center, chamber, 1, 1, 3, Blocks.IRON_BARS);
				placeDecoration(level, center, chamber, 0, 1, 3, ModBlocks.NEST_CORE);
				placeDecoration(level, center, chamber, -4, 2, 0, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 4, 2, 0, Blocks.LANTERN);
			}
			case "vault_treasury" -> {
				placeDecoration(level, center, chamber, -3, 1, 0, Blocks.CHEST);
				placeDecoration(level, center, chamber, 2, 1, -1, Blocks.BARREL);
				placeDecoration(level, center, chamber, -2, 1, 2, Blocks.GOLD_BLOCK);
				placeDecoration(level, center, chamber, 0, 1, 2, ModBlocks.NEST_CORE);
				placeDecoration(level, center, chamber, 2, 1, 2, Blocks.AMETHYST_BLOCK);
				placeDecoration(level, center, chamber, -3, 2, 0, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 2, 2, -1, Blocks.LANTERN);
			}
			case "vault_sanctum" -> {
				placeDecoration(level, center, chamber, -2, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, 3, 1, 0, ModBlocks.CHITIN_BED);
				placeDecoration(level, center, chamber, -2, 1, 2, Blocks.BONE_BLOCK);
				placeDecoration(level, center, chamber, 2, 1, 2, Blocks.HONEYCOMB_BLOCK);
				placeDecoration(level, center, chamber, 0, 1, 2, Blocks.OCHRE_FROGLIGHT);
				placeDecoration(level, center, chamber, -2, 2, 2, Blocks.LANTERN);
				placeDecoration(level, center, chamber, 2, 2, 2, Blocks.LANTERN);
			}
			default -> throw new IllegalArgumentException("Unsupported vault purpose " + chamber.purpose());
		}
	}

	private static void placeDecoration(ServerLevel level, BlockPos center,
			SubterraneanVaultBlueprint.Chamber chamber, int offsetX, int offsetY, int offsetZ, Block block) {
		int x = chamber.x() + offsetX;
		int y = chamber.floorY() + offsetY;
		int z = chamber.z() + offsetZ;
		if (!chamber.carves(x, y, z)) {
			throw new IllegalArgumentException("Decoration " + block + " falls outside vault chamber " + chamber.id());
		}
		StructurePlacer.safeSet(level, center.offset(x, y, z), block);
	}

	private static Block materialFor(SubterraneanVaultBlueprint blueprint, int x, int y, int z) {
		if (!isSurface(blueprint, x, y, z)) {
			return ModBlocks.NEST_MOUND;
		}
		int roll = Math.floorMod(x * 73428767 ^ y * 912931 ^ z * 43828933 ^ blueprint.seed() * 199999, 100);
		if (roll < 45) return ModBlocks.NEST_MOUND;
		if (roll < 59) return Blocks.PACKED_MUD;
		if (roll < 71) return Blocks.MUD_BRICKS;
		if (roll < 82) return Blocks.TUFF;
		if (roll < 90) return Blocks.CHISELED_TUFF;
		if (roll < 96) return Blocks.POLISHED_DEEPSLATE;
		if (roll < 99) return Blocks.CUT_COPPER;
		return Blocks.AMETHYST_BLOCK;
	}

	private static boolean isSurface(SubterraneanVaultBlueprint blueprint, int x, int y, int z) {
		return !blueprint.isSolid(x + 1, y, z)
				|| !blueprint.isSolid(x - 1, y, z)
				|| !blueprint.isSolid(x, y + 1, z)
				|| !blueprint.isSolid(x, y - 1, z)
				|| !blueprint.isSolid(x, y, z + 1)
				|| !blueprint.isSolid(x, y, z - 1);
	}
}
