package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/** Read-only live terrain selection shared by capture and its server regression. */
public final class CaptureGround {
    public static void generateFootprint(ServerLevel level, int minX, int maxX, int minZ, int maxZ) {
        for (int cx = Math.floorDiv(minX, 16); cx <= Math.floorDiv(maxX, 16); cx++)
            for (int cz = Math.floorDiv(minZ, 16); cz <= Math.floorDiv(maxZ, 16); cz++) {
                var chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, true);
                if (!(chunk instanceof LevelChunk)) throw new AssertionError("Candidate footprint did not reach minecraft:full: " + cx + "," + cz);
                PrimeAnts.LOGGER.info("T04 capture ground generated chunk {},{} status={}", cx, cz, chunk.getPersistedStatus());
            }
    }

    public static boolean clear(ServerLevel level, BlockPos ground, int radius) {
        // Every intersected chunk must already be FULL before any block-state read.
        for (int cx = Math.floorDiv(ground.getX() - radius, 16); cx <= Math.floorDiv(ground.getX() + radius, 16); cx++)
            for (int cz = Math.floorDiv(ground.getZ() - radius, 16); cz <= Math.floorDiv(ground.getZ() + radius, 16); cz++)
                if (!(level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false) instanceof LevelChunk)) return false;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            BlockPos p = ground.offset(x, 0, z);
            if (!level.getBlockState(p).isSolidRender() || !level.getBlockState(p.above()).isAir()
                    || !level.getBlockState(p.above(2)).isAir()) return false;
        }
        return true;
    }

    public static BlockPos find(ServerLevel level, BlockPos origin, BlockPos other) {
        // Generate the whole search region including each candidate's 5x5 footprint.
        generateFootprint(level, origin.getX() - 42, origin.getX() + 42, origin.getZ() - 42, origin.getZ() + 42);
        for (int radius = 0; radius <= 40; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
            int x = origin.getX() + dx, z = origin.getZ() + dz;
            if (other != null && (x - other.getX()) * (x - other.getX()) + (z - other.getZ()) * (z - other.getZ()) < 49) continue;
            BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            var state = level.getBlockState(ground);
            if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE) || state.is(Blocks.SAND))) continue;
            if (clear(level, ground, 2)) {
                PrimeAnts.LOGGER.info("T04 selected FULL live ground {} state={} footprint=5x5 twoAirLayers=true", ground, state);
                return ground;
            }
        }
        throw new AssertionError("NO_SUITABLE_GROUND: fully generated search has no live unobstructed 5x5 natural footprint; no specimen created here");
    }
}
