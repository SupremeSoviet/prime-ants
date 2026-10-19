package dev.primeants.founding;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Actual successful colony writes only. Supported lighting may retain an existing opening; air->air still revokes. */
public final class ColonyPlugs extends SavedData {
    public static final Codec<ColonyPlugs> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(ColonyPlugs::new, d -> Map.copyOf(d.records));
    public static final SavedDataType<ColonyPlugs> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants", "colony_plugs"), ColonyPlugs::new, CODEC, DataFixTypes.LEVEL);
    private final Map<String,String> records;
    public ColonyPlugs() { this(Map.of()); }
    private ColonyPlugs(Map<String,String> values) { records = new HashMap<>(values); }
    public static ColonyPlugs get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    private String key(BlockPos p) { return Long.toString(p.asLong()); }
    public void invalidate(BlockPos p) { if (records.remove(key(p)) != null) setDirty(); }
    public void write(ServerLevel l,BlockPos p,net.minecraft.world.level.block.state.BlockState replacement){
        var previous=records.get(key(p));
        if(previous!=null&&(previous.endsWith(":open")||previous.endsWith(":generated_open"))&&NestPlan.loaded(l,p)
                &&NestLighting.transition(l,p,l.getBlockState(p),replacement)&&previous.equals(get(l).records.get(key(p))))return;
        get(l).invalidate(p);
    }
    public UUID componentOwner(ServerLevel l,BlockPos p){String r=records.get(key(p));if(r==null)return null;UUID id=UUID.fromString(r.substring(0,36));return owned(l,p,id)?id:null;}
    public void placed(BlockPos p, UUID colony) { records.put(key(p), colony + ":plug"); setDirty(); }
    void generatedOpen(ServerLevel l,BlockPos p,UUID owner){if(!l.getBlockState(p).isAir())throw new IllegalStateException("Generated plug not open");records.put(key(p),owner+":generated_open");setDirty();}
    /** Legacy dirt stays readable; newly compacted plugs use the same nest-soil material as the mound. */
    public static boolean material(net.minecraft.world.level.block.state.BlockState state){return state.is(Blocks.DIRT)||state.is(dev.primeants.brood.NurseryBlocks.NEST_SOIL);}
    public boolean owned(ServerLevel l, BlockPos p, UUID colony) { return NestPlan.loaded(l,p) && material(l.getBlockState(p)) && (colony + ":plug").equals(records.get(key(p))); }
    /** Opening integrity only. Entrance route checks still require physical walkability. */
    public boolean opened(ServerLevel l, BlockPos p, UUID colony) {
        var previous=records.get(key(p));if(!(colony+":open").equals(previous)&&!(colony+":generated_open").equals(previous))return false;
        if(!NestPlan.loaded(l,p))return false;var state=l.getBlockState(p);
        return (state.isAir()||NestLighting.supported(l,p,state))&&previous.equals(get(l).records.get(key(p)));
    }
    public boolean openingLoaded(ServerLevel l,BlockPos p,UUID colony){
        if(!NestPlan.loaded(l,p))return false;var previous=records.get(key(p));
        if(!(colony+":open").equals(previous)&&!(colony+":generated_open").equals(previous))return true;
        var state=l.getBlockState(p);return !previous.equals(get(l).records.get(key(p)))||NestLighting.supportLoaded(l,p,state);
    }
    public boolean remove(ServerLevel l, BlockPos p, UUID colony) {
        if (!owned(l,p,colony) || SupportSurvival.problem(l,p,Blocks.AIR.defaultBlockState())!=null || !l.setBlock(p,Blocks.AIR.defaultBlockState(),3)) return false;
        records.put(key(p), colony + ":open"); setDirty(); return true;
    }
}
