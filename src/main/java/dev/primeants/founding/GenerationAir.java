package dev.primeants.founding;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.*;

/** Positive, revocable generation authority for the bounded surface air used ONLY by the initial installer.
 * Ordinary workers gain no permission from this ledger. Even air-to-air writes revoke it. */
public final class GenerationAir extends SavedData {
    public static final Codec<GenerationAir> CODEC=Codec.STRING.listOf().xmap(GenerationAir::new,a->List.copyOf(a.cells));
    public static final SavedDataType<GenerationAir> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","generation_air"),GenerationAir::new,CODEC,DataFixTypes.LEVEL);
    private final Set<String> cells=new HashSet<>();
    public GenerationAir(){} private GenerationAir(List<String> list){cells.addAll(list);}
    public static GenerationAir get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public void invalidate(BlockPos p){if(cells.remove(Long.toString(p.asLong())))setDirty();}
    public boolean eligible(ServerLevel l,BlockPos p){return NestPlan.loaded(l,p)&&l.getBlockState(p).isAir()&&cells.contains(Long.toString(p.asLong()));}
    public void observeGenerated(ProtoChunk chunk){
        if(!((GenerationWitness)chunk).primeAntsGeneratedTerrain()||chunk.isUpgrading()||chunk.getBelowZeroRetrogen()!=null)return;
        for(int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)for(int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++){
            int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
            for(int y=top;y<=Math.min(chunk.getMaxY(),top+6);y++){
                var p=new BlockPos(x,y,z);if(chunk.getBlockState(p).isAir())cells.add(Long.toString(p.asLong()));
            }
        }setDirty();
    }
}
