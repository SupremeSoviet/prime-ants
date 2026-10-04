package dev.primeants.founding;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Positive generation observations only. Absent/revoked records are NEVER inferred from loading soil. */
public final class NaturalSoil extends SavedData {
    public static final Codec<NaturalSoil> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(NaturalSoil::new, data -> Map.copyOf(data.observed));
    public static final SavedDataType<NaturalSoil> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("prime_ants", "natural_soil"), NaturalSoil::new, CODEC, DataFixTypes.LEVEL);
    private final Map<String, String> observed;
    public NaturalSoil() { this(Map.of()); }
    private NaturalSoil(Map<String, String> values) { observed = new HashMap<>(values); }
    public static NaturalSoil get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public static boolean material(BlockState s) {
        return (s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.COARSE_DIRT)
                || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM))
                && s.getFluidState().isEmpty() && !s.hasBlockEntity() && s.isSolidRender();
    }
    private static String key(BlockPos pos) { return Long.toString(pos.asLong()); }
    private static String type(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(); }
    public boolean eligible(ServerLevel level, BlockPos pos) {
        if (!(level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false) instanceof LevelChunk)) return false;
        BlockState s = level.getBlockState(pos);
        return material(s) && type(s).equals(observed.get(key(pos)));
    }
    public void invalidate(BlockPos pos) { if (observed.remove(key(pos)) != null) setDirty(); }
    /** Called ONLY on main-thread ProtoChunk -> LevelChunk conversion with a genuine new-generation witness.
     * Observes at most the top eight layers; deeper/old/retrogen terrain stays unknown. No world edits. */
    public void observeGenerated(ProtoChunk chunk) {
        if (Boolean.getBoolean("prime_ants.originDiagnostics")) dev.primeants.PrimeAnts.LOGGER.info("T05 origin conversion {} witness={} upgrading={}",chunk.getPos(),((GenerationWitness)chunk).primeAntsGeneratedTerrain(),chunk.isUpgrading());
        if (!((GenerationWitness)chunk).primeAntsGeneratedTerrain() || chunk.isUpgrading() || chunk.getBelowZeroRetrogen() != null) return;
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                if (Boolean.getBoolean("prime_ants.originDiagnostics") && (x & 15) == 0 && (z & 15) == 0) dev.primeants.PrimeAnts.LOGGER.info("T05 origin sample {} top={} state={}",chunk.getPos(),top,chunk.getBlockState(new BlockPos(x,top,z)));
                for (int y = top; y >= Math.max(chunk.getMinY(), top - 8); y--) {
                    BlockPos p = new BlockPos(x, y, z); BlockState s = chunk.getBlockState(p);
                    if (material(s)) observed.put(key(p), type(s));
                }
            }
        setDirty();
    }
}
