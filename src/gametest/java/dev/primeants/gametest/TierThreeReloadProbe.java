package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.TransferCustody;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;

/** Exact read-only reconstruction assertions, including other colonies in this same level. */
final class TierThreeReloadProbe {
    private static final Map<UUID,GameTestHelper> MISSING=new HashMap<>();
    static void watch(GameTestHelper c,LasiusNigerEntity q){
        MISSING.put(q.getUUID(),c);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){MISSING.remove(q.getUUID());}
            public void testFailed(GameTestInfo i,GameTestRunner r){MISSING.remove(q.getUUID());}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){MISSING.remove(q.getUUID());}
        });
    }
    static void sameLogicalJob(GameTestHelper c,LasiusNigerEntity q,ChamberUpgrade.Job original,ChamberUpgrade.Job current){
        c.assertTrue(current==ChamberUpgrade.get(c.getLevel()).job(q.getUUID(),original.chamber,original.tier)
            &&original.home.entrance().equals(current.home.entrance())&&original.home.direction()==current.home.direction()
            &&original.home.chamber().equals(current.home.chamber())&&original.chamber.equals(current.chamber)&&original.tier==current.tier
            &&original.material.equals(current.material)&&original.cells.equals(current.cells)&&current.ledger().exact(),
            "Missing supply resumes the same persisted colony/chamber/tier/home/plan/cells/material job with exact ledger");
    }
    record Snapshot(JsonObject jobs,JsonElement terrain,JsonElement registry,JsonElement custody,Map<UUID,CompoundTag> actors,Map<UUID,JsonElement> active){}
    private static CompoundTag save(GameTestHelper c,LasiusNigerEntity a){var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());a.save(o);return o.buildResult();}
    static Snapshot before(GameTestHelper c){
        var l=c.getLevel();var jobs=ChamberUpgrade.CODEC.encodeStart(JsonOps.INSTANCE,ChamberUpgrade.get(l)).getOrThrow().getAsJsonObject();
        var actors=new LinkedHashMap<UUID,CompoundTag>();
        for(var row:jobs.entrySet()){
            var claim=row.getValue().getAsJsonObject().get("claim").getAsString();
            if(!claim.isEmpty()&&l.getEntity(UUID.fromString(claim)) instanceof LasiusNigerEntity a)actors.put(a.getUUID(),save(c,a));
        }
        var active=new LinkedHashMap<UUID,JsonElement>();
        for(var entry:MISSING.entrySet())if(entry.getValue().getLevel()==l){
            var j=ChamberUpgrade.get(l).job(entry.getKey(),ChamberRegistry.FOUNDING,3);
            if(j!=null&&!j.complete())active.put(entry.getKey(),jobs.get(entry.getKey()+"/"+ChamberRegistry.FOUNDING+"/3").deepCopy());
        }
        return new Snapshot(jobs,ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow(),ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,ChamberRegistry.get(l)).getOrThrow(),TransferCustody.CODEC.encodeStart(JsonOps.INSTANCE,TransferCustody.get(l)).getOrThrow(),actors,active);
    }
    static void after(GameTestHelper c,Snapshot before){
        var l=c.getLevel();var after=ChamberUpgrade.CODEC.encodeStart(JsonOps.INSTANCE,ChamberUpgrade.get(l)).getOrThrow().getAsJsonObject();
        c.assertTrue(before.jobs.equals(after),"Global disk reconstruction preserves every colony's persisted key, home/plan, cells, paid prefix, ledger, claim, ticks, reason, material and transfer identities exactly");
        c.assertTrue(before.terrain.equals(ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow())&&before.registry.equals(ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,ChamberRegistry.get(l)).getOrThrow())&&before.custody.equals(TransferCustody.CODEC.encodeStart(JsonOps.INSTANCE,TransferCustody.get(l)).getOrThrow()),"Global disk reconstruction retains every colony's terrain, chamber and custody state");
        before.actors.forEach((id,tag)->c.assertTrue(l.getEntity(id) instanceof LasiusNigerEntity a&&tag.equals(save(c,a)),"Reload leaves each actual claimed actor's canonical equipment/task/claim state exact: "+id));
        before.active.forEach((id,tag)->{
            var owner=MISSING.get(id);var row=new LinkedHashMap<String,Object>();row.put("missingSupplyQueen",id);row.put("missingHelperTick",owner.getTick());row.put("reloadHelperTick",c.getTick());row.put("sameLevel",owner.getLevel()==l);row.put("activeJob",true);row.put("before",tag);row.put("after",after.get(id+"/"+ChamberRegistry.FOUNDING+"/3"));row.put("allColoniesExact",true);row.put("claimedActorsExact",before.actors.keySet());
            PrimeAnts.LOGGER.info("T04 SAFETY RELOAD OVERLAP {}",new Gson().toJson(row));
        });
    }
}
