package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.TransferCustody;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.TagValueOutput;

/** Read-only phase snapshots; string-backed JSON is expanded only in diagnostic differences. */
final class PersistenceDiagnostics {
    static JsonObject state(GameTestHelper c,LasiusNigerEntity w){
        var l=c.getLevel();var row=new JsonObject();
        row.add("surface",SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.get(l)).getOrThrow());
        row.add("terrain",ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow());
        row.add("custody",TransferCustody.CODEC.encodeStart(JsonOps.INSTANCE,TransferCustody.get(l)).getOrThrow());
        row.addProperty("game_time",l.getGameTime());
        var plan=w==null?null:w.workerTasks().plan();
        if(plan!=null&&NestPlan.loaded(l,plan.nursery())&&l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile pile){
            var catchup=new JsonObject();var interval=pile.missedIntervalBefore(l);
            catchup.addProperty("observed_after",interval.afterGameTime().orElse(-1));catchup.addProperty("observed_before",interval.beforeGameTime());catchup.addProperty("missed_ticks",interval.ticks());
            var saved=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());pile.catchup().save(saved);
            catchup.addProperty("saved_cursor_and_applied_state",saved.buildResult().toString());
            var observation=pile.reloadObservation();if(observation!=null)catchup.addProperty("reload_observation",observation.interval().toString());
            row.add("catchup",catchup);
        }
        if(w!=null){
            var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());w.save(out);row.addProperty("actor_nbt",out.buildResult().toString());row.addProperty("actor_action",w.workerTasks().harvestingTicks());row.addProperty("actor_uuid",w.getUUID().toString());row.addProperty("physical_mainhand",w.getMainHandItem().toString());
            row.addProperty("supplied_actor_is_canonical",l.getEntity(w.getUUID())==w);
            if(l.getEntity(w.getUUID()) instanceof LasiusNigerEntity canonical){
                row.addProperty("canonical_actor_uuid",canonical.getUUID().toString());row.addProperty("canonical_actor_action",canonical.workerTasks().harvestingTicks());row.addProperty("canonical_physical_mainhand",canonical.getMainHandItem().toString());
                var actual=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());canonical.save(actual);row.addProperty("canonical_actor_nbt",actual.buildResult().toString());
            }
        }
        return row;
    }
    private static JsonElement expand(JsonElement e){
        if(e!=null&&e.isJsonPrimitive()&&e.getAsJsonPrimitive().isString()&&e.getAsString().startsWith("{"))try{return JsonParser.parseString(e.getAsString());}catch(RuntimeException ignored){}
        return e;
    }
    private static void diff(String path,JsonElement a,JsonElement b,JsonArray rows){
        if(Objects.equals(a,b))return;a=expand(a);b=expand(b);
        if(a!=null&&b!=null&&a.isJsonObject()&&b.isJsonObject()){
            var keys=new TreeSet<String>();keys.addAll(a.getAsJsonObject().keySet());keys.addAll(b.getAsJsonObject().keySet());for(var k:keys)diff(path+"/"+k,a.getAsJsonObject().get(k),b.getAsJsonObject().get(k),rows);return;
        }
        if(a!=null&&b!=null&&a.isJsonArray()&&b.isJsonArray()&&a.getAsJsonArray().size()==b.getAsJsonArray().size()){
            for(int i=0;i<a.getAsJsonArray().size();i++)diff(path+"/"+i,a.getAsJsonArray().get(i),b.getAsJsonArray().get(i),rows);return;
        }
        if(Objects.equals(a,b))return;var row=new JsonObject();row.addProperty("path",path);row.add("before",a);row.add("after",b);rows.add(row);
    }
    static void observe(GameTestHelper c,String phase,JsonObject baseline,JsonObject current){
        var row=new JsonObject();row.addProperty("phase",phase);row.addProperty("tick",c.getTick());row.add("state",current);var diffs=new JsonArray();if(baseline!=null)diff("",baseline,current,diffs);row.add("differences",diffs);
        // Job codecs contain JSON strings. Preserve their exact unequal operands even when
        // expanding those strings finds equal maps with a different iteration order.
        var encoded=new JsonArray();if(baseline!=null)for(var field:List.of("surface","terrain","custody")){
            var before=baseline.get(field);var after=current.get(field);if(before==null||after==null)continue;
            row.addProperty(field+"_encoded_equal",before.equals(after));
            if(before.isJsonObject()&&after.isJsonObject()){
                var a=before.getAsJsonObject();var b=after.getAsJsonObject();var keys=new TreeSet<String>();keys.addAll(a.keySet());keys.addAll(b.keySet());
                for(var key:keys)if(!Objects.equals(a.get(key),b.get(key))){var d=new JsonObject();d.addProperty("path","/"+field+"/"+key);d.add("before",a.get(key));d.add("after",b.get(key));encoded.add(d);}
            }else if(!before.equals(after)){var d=new JsonObject();d.addProperty("path","/"+field);d.add("before",before);d.add("after",after);encoded.add(d);}
        }row.add("encoded_differences",encoded);ReconciliationEvidence.log("t07_persistence","read_only",row);
    }
}
