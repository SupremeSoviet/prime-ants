package dev.primeants.founding;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.primeants.brood.NurseryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;

/** Successful in-place preparation, mound deposits and worker removals. Never inferred from geometry/history. */
public final class ColonyTerrain extends SavedData {
    public static final Codec<ColonyTerrain> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING).xmap(ColonyTerrain::new,d->Map.copyOf(d.records));
    public static final SavedDataType<ColonyTerrain> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","colony_terrain"),ColonyTerrain::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,String> records;
    public ColonyTerrain(){this(Map.of());}
    private ColonyTerrain(Map<String,String> r){records=new HashMap<>(r);}
    public static ColonyTerrain get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    private static String key(BlockPos p){return Long.toString(p.asLong());}
    public void invalidate(BlockPos p){if(records.remove(key(p))!=null)setDirty();}
    private void record(BlockPos p,UUID owner,String kind){records.put(key(p),owner+":"+kind);setDirty();}
    private boolean matches(ServerLevel l,BlockPos p,UUID owner,String kind,BlockState expected){return NestPlan.loaded(l,p)&&l.getBlockState(p).equals(expected)&&(owner+":"+kind).equals(records.get(key(p)));}
    public boolean prepared(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"prepared:nest_soil",NurseryBlocks.NEST_SOIL.defaultBlockState());}
    public boolean mound(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"mound:nest_soil",NurseryBlocks.NEST_SOIL.defaultBlockState());}
    public boolean opened(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"worker_open:air",Blocks.AIR.defaultBlockState());}
    public boolean eligible(ServerLevel l,BlockPos p,UUID owner){return NaturalSoil.get(l).eligible(l,p)||prepared(l,p,owner);}
    public boolean compatible(ServerLevel l,BlockPos p,UUID owner,BlockState expected){return NaturalSoil.get(l).compatible(l,p,expected)||prepared(l,p,owner)&&l.getBlockState(p).equals(expected);}
    public String preparationProblem(ServerLevel l,BlockPos p){
        return NativeVegetation.dependentAbove(l,p)?"protected_vegetation_support_at_"+p:!NaturalSoil.get(l).eligible(l,p)?"soil_authority_unavailable_at_"+p:null;
    }
    public boolean prepare(ServerLevel l,BlockPos p,UUID owner){
        var problem=preparationProblem(l,p);
        if(problem!=null) {
            dev.primeants.PrimeAnts.LOGGER.debug("Soil preparation refused owner={} reason={}",owner,problem);
            return false;
        }
        if(!l.setBlock(p,NurseryBlocks.NEST_SOIL.defaultBlockState(),3))return false;
        record(p,owner,"prepared:nest_soil");return true;
    }
    public void deposited(BlockPos p,UUID owner){record(p,owner,"mound:nest_soil");}
    public void removed(BlockPos p,UUID owner){record(p,owner,"worker_open:air");}
}
