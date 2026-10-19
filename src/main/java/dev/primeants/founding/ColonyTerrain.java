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
    /** Derived lookup only. The serialized strings retain their exact legacy representation. Each replacement/read
     * builds its own index; writes and same-state revocations update both maps synchronously. */
    private record Ownership(UUID owner,String kind) { }
    private final it.unimi.dsi.fastutil.longs.Long2ObjectMap<Ownership> ownership=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    public ColonyTerrain(){this(Map.of());}
    private ColonyTerrain(Map<String,String> r){
        records=new HashMap<>(r);
        r.forEach((key,value)->{
            try{long pos=Long.parseLong(key);if(key.equals(Long.toString(pos)))index(pos,value);}
            catch(IllegalArgumentException ignored){/* Unreachable/noncanonical old keys stay serialized, never gain authority. */}
        });
    }
    private void index(long pos,String value){
        ownership.remove(pos);
        int colon=value.indexOf(':');if(colon<0)return;
        String name=value.substring(0,colon);UUID owner;
        try{owner=name.equals("null")?null:UUID.fromString(name);}catch(IllegalArgumentException ignored){return;}
        if(!name.equals(String.valueOf(owner)))return;
        ownership.put(pos,new Ownership(owner,value.substring(colon+1)));
    }
    private boolean owns(BlockPos p,UUID owner,String kind){var o=ownership.get(p.asLong());return o!=null&&java.util.Objects.equals(owner,o.owner())&&kind.equals(o.kind());}
    public static ColonyTerrain get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    private static String key(BlockPos p){return Long.toString(p.asLong());}
    public void invalidate(BlockPos p){ownership.remove(p.asLong());if(records.remove(key(p))!=null)setDirty();}
    /** Preserve only an existing opening through a dry, supported light edit. Air-to-air writes and every
     * other replacement still revoke authority. No plan, edit history or geometry can recreate a lost record. */
    public void write(ServerLevel l,BlockPos p,BlockState replacement){
        var prior=ownership.get(p.asLong());
        if(prior!=null&&(prior.kind().equals("worker_open:air")||prior.kind().equals("generated:open"))&&NestPlan.loaded(l,p)){
            var before=l.getBlockState(p);
            if(NestLighting.transition(l,p,before,replacement)
                    &&java.util.Objects.equals(prior,get(l).ownership.get(p.asLong())))return;
        }
        get(l).invalidate(p);
    }
    public UUID componentOwner(ServerLevel l,BlockPos p){
        String r=records.get(key(p));if(r==null)return null;UUID owner=UUID.fromString(r.substring(0,36));
        return prepared(l,p,owner)||mound(l,p,owner)||surface(l,p,owner)||built(l,p,owner,l.getBlockState(p).getBlock())?owner:null;
    }
    private void record(BlockPos p,UUID owner,String kind){records.put(key(p),owner+":"+kind);ownership.put(p.asLong(),new Ownership(owner,kind));setDirty();}
    private boolean matches(ServerLevel l,BlockPos p,UUID owner,String kind,BlockState expected){
        // Negative authority avoids unnecessary world reads. Positive authority must still be current after them:
        // a world read can cross a write/revocation boundary, including replacement of this SavedData instance.
        return owns(p,owner,kind)&&NestPlan.loaded(l,p)&&l.getBlockState(p).equals(expected)&&get(l).owns(p,owner,kind);
    }
    public boolean prepared(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"prepared:nest_soil",NurseryBlocks.NEST_SOIL.defaultBlockState());}
    public boolean mound(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"mound:nest_soil",NurseryBlocks.NEST_SOIL.defaultBlockState())
        ||accessMoundKind(p,owner)&&NestPlan.loaded(l,p)&&l.getBlockState(p).equals(NurseryBlocks.NEST_SOIL.defaultBlockState())&&get(l).accessMoundKind(p,owner);}
    private boolean accessMoundKind(BlockPos p,UUID owner){var o=ownership.get(p.asLong());return o!=null&&java.util.Objects.equals(owner,o.owner())&&o.kind().startsWith("access_mound:");}
    boolean reserveAccess(ServerLevel l,BlockPos p,UUID owner,UUID unit){
        if(!canReserveAccess(l,p))return false;
        record(p,owner,"access_destination:"+unit);return true;
    }
    boolean canReserveAccess(ServerLevel l,BlockPos p){return NestPlan.loaded(l,p)&&!records.containsKey(key(p))&&l.getBlockState(p).equals(Blocks.AIR.defaultBlockState())&&l.getFluidState(p).isEmpty();}
    public boolean accessReservation(ServerLevel l,BlockPos p,UUID owner,UUID unit){return matches(l,p,owner,"access_destination:"+unit,Blocks.AIR.defaultBlockState());}
    void accessOpened(BlockPos p,UUID owner,UUID unit){record(p,owner,"access_open:"+unit);}
    public boolean accessOpen(ServerLevel l,BlockPos p,UUID owner,UUID unit){return matches(l,p,owner,"access_open:"+unit,Blocks.AIR.defaultBlockState());}
    void accessDeposited(BlockPos p,UUID owner,UUID unit){record(p,owner,"access_mound:"+unit);}
    public boolean accessMound(ServerLevel l,BlockPos p,UUID owner,UUID unit){return matches(l,p,owner,"access_mound:"+unit,NurseryBlocks.NEST_SOIL.defaultBlockState());}
    /** Surface locations own one paid soil unit, including an in-place gate conversion. */
    public boolean surface(ServerLevel l,BlockPos p,UUID owner){
        if(!NestPlan.loaded(l,p))return false;var block=l.getBlockState(p).getBlock();return (block==NurseryBlocks.NEST_SOIL||block==NurseryBlocks.MOUND_GATE)&&matches(l,p,owner,"surface:"+id(block),block.defaultBlockState())||generatedSurface(l,p,owner);
    }
    public void surfaceBuilt(BlockPos p,UUID owner,net.minecraft.world.level.block.Block block){
        if(block!=NurseryBlocks.NEST_SOIL&&block!=NurseryBlocks.MOUND_GATE)throw new IllegalArgumentException("Surface palette");record(p,owner,"surface:"+id(block));
    }
    public void permitSurface(ServerLevel l,BlockPos p,UUID owner){
        if(!NestPlan.loaded(l,p)||records.containsKey(key(p))||!(l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty()||NativeVegetation.get(l).eligible(l,p)))return;
        record(p,owner,"surface_target:"+l.getBlockState(p));
    }
    public boolean surfacePermission(ServerLevel l,BlockPos p,UUID owner){return NestPlan.loaded(l,p)&&(owner+":surface_target:"+l.getBlockState(p)).equals(records.get(key(p)));}
    public boolean opened(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"worker_open:air",Blocks.AIR.defaultBlockState())||generatedOpen(l,p,owner);}
    public boolean generatedOpen(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"generated:open",Blocks.AIR.defaultBlockState());}
    /** Live ownership/integrity, independent of physical walking space. Lantern collision remains real. */
    public boolean openingIntact(ServerLevel l,BlockPos p,UUID owner){return opened(l,p,owner)||litOpening(l,p,owner,"worker_open:air")||litOpening(l,p,owner,"generated:open");}
    public boolean generatedOpeningIntact(ServerLevel l,BlockPos p,UUID owner){return generatedOpen(l,p,owner)||litOpening(l,p,owner,"generated:open");}
    /** Preserve Findings.UNKNOWN for a retained light whose support cannot be observed. Revoked/foreign cells
     * remain known faults, even when a nearby support is unavailable. */
    public boolean openingLoaded(ServerLevel l,BlockPos p,UUID owner){
        if(!NestPlan.loaded(l,p))return false;
        if(!owns(p,owner,"worker_open:air")&&!owns(p,owner,"generated:open"))return true;
        var state=l.getBlockState(p);var current=get(l);
        return !current.owns(p,owner,"worker_open:air")&&!current.owns(p,owner,"generated:open")||NestLighting.supportLoaded(l,p,state);
    }
    private boolean litOpening(ServerLevel l,BlockPos p,UUID owner,String kind){
        if(!owns(p,owner,kind)||!NestPlan.loaded(l,p))return false;
        var state=l.getBlockState(p);
        return NestLighting.supported(l,p,state)&&get(l).owns(p,owner,kind);
    }
    public boolean generatedSurface(ServerLevel l,BlockPos p,UUID owner){return matches(l,p,owner,"generated:surface",NurseryBlocks.NEST_SOIL.defaultBlockState());}
    void generated(BlockPos p,UUID owner,String kind){record(p,owner,"generated:"+kind);}
    private static String id(net.minecraft.world.level.block.Block b){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();}
    /** A wall cell the colony itself rebuilt in place with this wall block (NestWalls), still exactly as it left it. */
    public boolean built(ServerLevel l,BlockPos p,UUID owner,net.minecraft.world.level.block.Block block){return NestWalls.TIERS.containsKey(id(block))&&matches(l,p,owner,"built:"+id(block),block.defaultBlockState()) || block==NurseryBlocks.NEST_CUT_STONE && matches(l,p,owner,"faced:packed_clay:"+id(block),block.defaultBlockState());}
    /** A loaded wall cell's tier (NestWalls.tier): the colony's own built wall block gives its tier; any other solid wall
     * is 1, whether natural or colony earth, natural stone, or a block a player placed. */
    public int wallTier(ServerLevel l,BlockPos p,UUID owner){var block=l.getBlockState(p).getBlock();return NestWalls.tier(id(block),built(l,p,owner,block));}
    public boolean retainedClay(ServerLevel l,BlockPos p,UUID owner){return built(l,p,owner,NurseryBlocks.PACKED_CLAY)||matches(l,p,owner,"faced:packed_clay:"+id(NurseryBlocks.NEST_CUT_STONE),NurseryBlocks.NEST_CUT_STONE.defaultBlockState());}
    public void faced(BlockPos p,UUID owner){record(p,owner,"faced:packed_clay:"+id(NurseryBlocks.NEST_CUT_STONE));}
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
