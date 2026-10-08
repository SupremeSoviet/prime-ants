package dev.primeants.founding;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.*;

/** Separate positive mining authority. Soil eligibility is unchanged. Loading and geology never authorize work. */
public final class NaturalMaterials extends SavedData {
    public static final Codec<NaturalMaterials> CODEC=Codec.PASSTHROUGH.xmap(dynamic->{
        var data=new NaturalMaterials();dynamic.getMapValues().getOrThrow().forEach((k,v)->data.observed.put(k.asString().getOrThrow(),v.asString().getOrThrow()));return data;
    },data->new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,data.json()));
    public static final SavedDataType<NaturalMaterials> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","natural_materials"),NaturalMaterials::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,String> observed=new HashMap<>();
    public NaturalMaterials(){}
    private com.google.gson.JsonObject json(){var out=new com.google.gson.JsonObject();observed.forEach(out::addProperty);return out;}
    public static NaturalMaterials get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public static String type(BlockState s){return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();}
    public static String unit(BlockState s){return MiningShape.UNITS.get(type(s));}
    public boolean eligible(ServerLevel l,BlockPos p){
        if(!NestPlan.loaded(l,p)||!(l.getChunkSource().getChunk(p.getX()>>4,p.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false) instanceof net.minecraft.world.level.chunk.LevelChunk))return false;
        var s=l.getBlockState(p);
        return unit(s)!=null&&s.getFluidState().isEmpty()&&!s.hasBlockEntity()&&type(s).equals(observed.get(Long.toString(p.asLong())));
    }
    public void invalidate(BlockPos p){if(observed.remove(Long.toString(p.asLong()))!=null)setDirty();}
    /** Only the genuine new terrain witness, at ProtoChunk -> LevelChunk conversion. Surface through sixteen blocks below it. */
    public void observeGenerated(ProtoChunk chunk){
        if(!((GenerationWitness)chunk).primeAntsGeneratedTerrain()||chunk.isUpgrading()||chunk.getBelowZeroRetrogen()!=null)return;
        for(int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)for(int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++){
            int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
            for(int y=top;y>=Math.max(chunk.getMinY(),top-16);y--){var p=new BlockPos(x,y,z);var s=chunk.getBlockState(p);
                if(unit(s)!=null)observed.put(Long.toString(p.asLong()),type(s));
            }
        }
        setDirty();
    }
}
