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
    public UUID componentOwner(ServerLevel l,BlockPos p){
        String r=records.get(key(p));if(r==null)return null;UUID owner=UUID.fromString(r.substring(0,36));
        return prepared(l,p,owner)||mound(l,p,owner)||surface(l,p,owner)||built(l,p,owner,l.getBlockState(p).getBlock())?owner:null;
    }
    private void record(BlockPos p,UUID owner,String kind){records.put(key(p),owner+":"+kind);setDirty();}
    private boolean matches(ServerLevel l,BlockPos p,UUID owner,String kind,BlockState expected){return NestPlan.loaded(l,p)&&l.getBlockState(p).equals(expected)&&(owner+":"+kind).equals(records.get(key(p)));}
    public boolean prepared(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"prepared:nest_soil",NurseryBlocks.NEST_SOIL.defaultBlockState());}
    public boolean mound(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"mound:nest_soil",NurseryBlocks.NEST_SOIL.defaultBlockState());}
    /** Surface locations own one paid soil unit, including an in-place gate conversion. */
    public boolean surface(ServerLevel l,BlockPos p,UUID owner){
        if(!NestPlan.loaded(l,p))return false;var block=l.getBlockState(p).getBlock();return (block==NurseryBlocks.NEST_SOIL||block==NurseryBlocks.MOUND_GATE)&&matches(l,p,owner,"surface:"+id(block),block.defaultBlockState());
    }
    public void surfaceBuilt(BlockPos p,UUID owner,net.minecraft.world.level.block.Block block){
        if(block!=NurseryBlocks.NEST_SOIL&&block!=NurseryBlocks.MOUND_GATE)throw new IllegalArgumentException("Surface palette");record(p,owner,"surface:"+id(block));
    }
    public void permitSurface(ServerLevel l,BlockPos p,UUID owner){
        if(!NestPlan.loaded(l,p)||records.containsKey(key(p))||!(l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty()||NativeVegetation.get(l).eligible(l,p)))return;
        record(p,owner,"surface_target:"+l.getBlockState(p));
    }
    public boolean surfacePermission(ServerLevel l,BlockPos p,UUID owner){return NestPlan.loaded(l,p)&&(owner+":surface_target:"+l.getBlockState(p)).equals(records.get(key(p)));}
    public boolean opened(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"worker_open:air",Blocks.AIR.defaultBlockState());}
    private static String id(net.minecraft.world.level.block.Block b){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();}
    /** A wall cell the colony itself rebuilt in place with this wall block (NestWalls), still exactly as it left it. */
    public boolean built(ServerLevel l,BlockPos p,UUID owner,net.minecraft.world.level.block.Block block){return NestWalls.TIERS.containsKey(id(block))&&matches(l,p,owner,"built:"+id(block),block.defaultBlockState());}
    /** A loaded wall cell's tier (NestWalls.tier): the colony's own built wall block gives its tier; any other solid wall
     * is 1, whether natural or colony earth, natural stone, or a block a player placed. */
    public int wallTier(ServerLevel l,BlockPos p,UUID owner){var block=l.getBlockState(p).getBlock();return NestWalls.tier(id(block),built(l,p,owner,block));}
    public boolean eligible(ServerLevel l,BlockPos p,UUID owner){return NaturalSoil.get(l).eligible(l,p)||prepared(l,p,owner);}
    public boolean compatible(ServerLevel l,BlockPos p,UUID owner,BlockState expected){return NaturalSoil.get(l).compatible(l,p,expected)||prepared(l,p,owner)&&l.getBlockState(p).equals(expected);}
    public String preparationProblem(ServerLevel l,BlockPos p){
        var survival=SupportSurvival.problem(l,p,NurseryBlocks.NEST_SOIL.defaultBlockState());
        return survival!=null?survival:!NaturalSoil.get(l).eligible(l,p)?"soil_authority_unavailable_at_"+p:null;
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
    /** Records a wall cell the colony's worker has just rebuilt with this block (ChamberUpgrade). */
    public void built(BlockPos p,UUID owner,net.minecraft.world.level.block.Block block){if(!NestWalls.TIERS.containsKey(id(block)))throw new IllegalArgumentException("Not a nest wall block");record(p,owner,"built:"+id(block));}
}
