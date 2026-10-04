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

/** Actual successful colony writes only. Every subsequent chunk write revokes the record, even air->air/dirt->dirt. */
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
    public void placed(BlockPos p, UUID colony) { records.put(key(p), colony + ":plug"); setDirty(); }
    public boolean owned(ServerLevel l, BlockPos p, UUID colony) { return NestPlan.loaded(l,p) && l.getBlockState(p).is(Blocks.DIRT) && (colony + ":plug").equals(records.get(key(p))); }
    public boolean opened(ServerLevel l, BlockPos p, UUID colony) { return NestPlan.loaded(l,p) && l.getBlockState(p).isAir() && (colony + ":open").equals(records.get(key(p))); }
    public boolean remove(ServerLevel l, BlockPos p, UUID colony) {
        if (!owned(l,p,colony) || !l.setBlock(p,Blocks.AIR.defaultBlockState(),3)) return false;
        records.put(key(p), colony + ":open"); setDirty(); return true;
    }
}
