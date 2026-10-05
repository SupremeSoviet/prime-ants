package dev.primeants.brood;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

/** Once-only terminal brood investment/ingestion receipts. Never adult members, food or refunds. */
public final class BroodHistory extends SavedData {
    public static final Codec<BroodHistory> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING).xmap(BroodHistory::new,h->Map.copyOf(h.records));
    public static final SavedDataType<BroodHistory> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","brood_history"),BroodHistory::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,String> records;
    public BroodHistory(){this(Map.of());}private BroodHistory(Map<String,String> values){records=new HashMap<>(values);}
    public static BroodHistory get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public Map<String,String> records(){return Map.copyOf(records);}
    public String terminal(UUID id){return records.get(id.toString());}
    public String retain(UUID id,String receipt){var row=JsonParser.parseString(receipt).getAsJsonObject();if(!row.get("id").getAsString().equals(id.toString()))throw new IllegalArgumentException("Foreign terminal brood");var old=records.putIfAbsent(id.toString(),receipt);if(old==null)setDirty();return old==null?receipt:old;}
    public String expire(BroodRecord r,BlockPos home,long tick,String reason){
        var row=new JsonObject();row.addProperty("id",r.id().toString());row.addProperty("queen",r.queenId().toString());row.addProperty("home",home.asLong());row.addProperty("slot",r.slot());row.addProperty("stage",r.stage().name());row.addProperty("progress",r.progress());row.addProperty("founding",r.founding());row.addProperty("eggInvestment",r.founding()?BroodPile.EGG_COST:dev.primeants.worker.Nutrition.EGG_SUGAR+dev.primeants.worker.Nutrition.EGG_PROTEIN);row.addProperty("nourishment",r.nourishment());row.addProperty("reason",reason);row.addProperty("pileTick",tick);row.addProperty("neglectTicks",r.neglectTicks());row.addProperty("waitingTicks",r.waitingTicks());row.addProperty("neglectGrace",r.neglectGrace());row.addProperty("waitingBound",r.waitingBound());
        var n=new JsonObject();var v=r.nutrition();n.addProperty("sugar",v.sugar());n.addProperty("protein",v.protein());n.addProperty("spentSugar",v.spentSugar());n.addProperty("spentProtein",v.spentProtein());n.addProperty("apples",v.apples());n.addProperty("berries",v.berries());n.addProperty("chickens",v.chickens());n.addProperty("nectar",v.nectar());n.addProperty("flesh",v.flesh());row.add("nutrition",n);
        String saved=retain(r.id(),row.toString());dev.primeants.PrimeAnts.LOGGER.info("Brood expired receipt={}",saved);return saved;
    }
    public static long units(String receipt,String item){return JsonParser.parseString(receipt).getAsJsonObject().getAsJsonObject("nutrition").get(item).getAsLong();}
}
