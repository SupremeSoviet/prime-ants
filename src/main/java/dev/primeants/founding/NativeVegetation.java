package dev.primeants.founding;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.*;

/** Exact short-plant observations from genuine new generation. No historical or player-origin grants. */
public final class NativeVegetation extends SavedData {
    public static final Codec<NativeVegetation> CODEC = Codec.PASSTHROUGH.xmap(d -> {
        var values = new HashMap<String,String>();
        d.getMapValues().getOrThrow().forEach((k,v) -> values.put(k.asString().getOrThrow(),v.asString().getOrThrow()));
        return new NativeVegetation(values);
    }, data -> {
        var object = new com.google.gson.JsonObject(); data.observed.forEach(object::addProperty);
        return new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE, object);
    });
    public static final SavedDataType<NativeVegetation> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("prime_ants","native_vegetation"),NativeVegetation::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,String> observed;
    public NativeVegetation() { this(Map.of()); }
    private NativeVegetation(Map<String,String> values) { observed = new HashMap<>(values); }
    public static NativeVegetation get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public static boolean material(BlockState s) {
        // Deliberate bounded whitelist: no crops, saplings, leaves, trees or double-height plants.
        return s.getFluidState().isEmpty() && !s.hasBlockEntity() && (
            s.is(Blocks.SHORT_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.DANDELION) || s.is(Blocks.POPPY)
            || s.is(Blocks.BLUE_ORCHID) || s.is(Blocks.ALLIUM) || s.is(Blocks.AZURE_BLUET)
            || s.is(Blocks.RED_TULIP) || s.is(Blocks.ORANGE_TULIP) || s.is(Blocks.WHITE_TULIP) || s.is(Blocks.PINK_TULIP)
            || s.is(Blocks.WILDFLOWERS) || s.is(Blocks.PINK_PETALS)
            || s.is(Blocks.OXEYE_DAISY) || s.is(Blocks.CORNFLOWER) || s.is(Blocks.LILY_OF_THE_VALLEY));
    }
    public boolean eligible(ServerLevel l, BlockPos p) {
        return NestPlan.loaded(l,p) && material(l.getBlockState(p))
            && l.getBlockState(p).toString().equals(observed.get(Long.toString(p.asLong())));
    }
    public void invalidate(BlockPos p) { if (observed.remove(Long.toString(p.asLong())) != null) setDirty(); }
    public void observeGenerated(ProtoChunk chunk) {
        if (!((GenerationWitness)chunk).primeAntsGeneratedTerrain() || chunk.isUpgrading() || chunk.getBelowZeroRetrogen()!=null) return;
        for (int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)
            for (int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++) {
                int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
                for (int y=top;y>=Math.max(chunk.getMinY(),top-2);y--) {
                    var p=new BlockPos(x,y,z);var state=chunk.getBlockState(p);
                    if (material(state)) observed.put(Long.toString(p.asLong()),state.toString());
                }
            }
        setDirty();
    }
}
