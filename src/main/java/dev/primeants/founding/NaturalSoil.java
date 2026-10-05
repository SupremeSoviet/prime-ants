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
    // DFU BaseMapCodec uses Object2ObjectArrayMap.putIfAbsent: quadratic for a real saved world's
    // positive observations. Keep the identical string-map schema, decode into hash storage.
    public static final Codec<NaturalSoil> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
        Map<String,String> values=new HashMap<>();
        dynamic.getMapValues().getOrThrow().forEach((k,v)->values.put(k.asString().getOrThrow(),v.asString().getOrThrow()));
        return new NaturalSoil(values);
    }, data -> {
        var object=new com.google.gson.JsonObject();data.observed.forEach(object::addProperty);
        return new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,object);
    });
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
    public static boolean mineral(BlockState s) {
        return s.getFluidState().isEmpty() && s.isSolidRender() && !s.hasBlockEntity() &&
            (s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.GRANITE) || s.is(Blocks.DIORITE) || s.is(Blocks.ANDESITE));
    }
    public boolean floorSupport(ServerLevel level, BlockPos pos) {
        return eligible(level,pos) || NestPlan.loaded(level,pos) && mineral(level.getBlockState(pos))
            && ("support:"+type(level.getBlockState(pos))).equals(observed.get(key(pos)));
    }
    public void invalidate(BlockPos pos) { if (observed.remove(key(pos)) != null) setDirty(); }
    public static boolean grassPair(BlockState old,BlockState next) {
        return old.is(Blocks.DIRT) && next.is(Blocks.GRASS_BLOCK) || old.is(Blocks.GRASS_BLOCK) && next.is(Blocks.DIRT);
    }
    /** A stale plan may differ only within the grass/dirt family AND with current positive authority. */
    public boolean compatible(ServerLevel l,BlockPos p,BlockState expected) {
        return eligible(l,p) && (l.getBlockState(p).equals(expected) || grassPair(expected,l.getBlockState(p)));
    }
    public boolean nativeGrassWrite(ServerLevel l,BlockPos p,BlockState old,BlockState next) {
        if(!grassPair(old,next) || !eligible(l,p) || !l.getBlockState(p).equals(old))return false;
        observed.put(key(p),type(next));setDirty();return true;
    }
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
                    else if (mineral(s)) observed.put(key(p), "support:"+type(s));
                }
            }
        setDirty();
    }
}
