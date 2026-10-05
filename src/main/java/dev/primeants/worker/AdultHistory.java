package dev.primeants.worker;

import com.mojang.serialization.Codec;
import dev.primeants.entity.LasiusNigerEntity;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

/** Terminal adult receipts remain after vanilla removes the body. Never food or living members. */
public final class AdultHistory extends SavedData {
    public static final Codec<AdultHistory> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING).xmap(AdultHistory::new,h->Map.copyOf(h.records));
    public static final SavedDataType<AdultHistory> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","adult_history"),AdultHistory::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,String> records;
    public AdultHistory(){this(Map.of());}private AdultHistory(Map<String,String> values){records=new HashMap<>(values);}
    public static AdultHistory get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public Map<String,String> records(){return Map.copyOf(records);}
    public void died(LasiusNigerEntity ant){
        if(records.containsKey(ant.getUUID().toString()))return;
        var row=new com.google.gson.JsonObject();row.addProperty("queen",ant.colonyIdentity()==null?"":ant.colonyIdentity().toString());row.addProperty("age",ant.elapsedAgeTicks());row.addProperty("lifespan",ant.adultLife().lifespan());row.addProperty("fasting",ant.adultLife().fasting());row.addProperty("maintenance",ant.adultLife().maintenanceTicks());row.addProperty("cause",ant.adultLife().death().isEmpty()?"external_damage":ant.adultLife().death());
        row.add("nutrition",com.google.gson.JsonParser.parseString("{}"));var n=row.getAsJsonObject("nutrition");n.addProperty("sugar",ant.nutrition().sugar());n.addProperty("protein",ant.nutrition().protein());n.addProperty("apples",ant.nutrition().apples());n.addProperty("berries",ant.nutrition().berries());n.addProperty("chickens",ant.nutrition().chickens());n.addProperty("nectar",ant.nutrition().nectar());n.addProperty("flesh",ant.nutrition().flesh());n.addProperty("spentSugar",ant.nutrition().spentSugar());n.addProperty("spentProtein",ant.nutrition().spentProtein());
        records.put(ant.getUUID().toString(),row.toString());setDirty();
        dev.primeants.PrimeAnts.LOGGER.info("Adult death uuid={} receipt={}",ant.getUUID(),row);
    }
}
