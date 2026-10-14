package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.brood.*;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Same saved bodies/registry order, brood, cache and controller state in two sequential branches.
 * Ordinary branch uses only each adult's tick and the nursery block ticker at every consecutive time.
 * Rollback is test fixture reconstruction, recorded before any catch-up comparison. */
final class FiniteCacheLiveControl {
    static Map<String,List<Long>> receipts(JsonObject state){
        var out=new TreeMap<String,List<Long>>();
        for(String group:List.of("adults","brood"))for(var value:state.getAsJsonArray(group)){var a=value.getAsJsonObject();var n=a.getAsJsonObject("nutrition");out.put(a.get("id").getAsString(),List.of(n.get("apples").getAsLong(),n.get("berries").getAsLong(),n.get("chickens").getAsLong()));}
        return out;
    }
    record Result(LasiusNigerEntity queen,BroodPile pile,LasiusNigerEntity hungry,NestCache cache,JsonObject live){}
    static Result measure(GameTestHelper c,LasiusNigerEntity q,BroodPile p,LasiusNigerEntity hungry,NestCache cache,long last,boolean liveOnly){
        var l=c.getLevel();var bodies=AutomaticCatchupGameTest.adults(c,q);
        var tags=bodies.stream().map(a->ReconciliationGameTest.save(c,a)).toList();
        var ticks=bodies.stream().map(a->a.tickCount).toList();
        var pileTag=p.saveWithFullMetadata(l.registryAccess());var cacheTag=cache.saveWithFullMetadata(l.registryAccess());
        var members=ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE,ColonyMembers.get(l)).getOrThrow();
        var chambers=ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,ChamberRegistry.get(l)).getOrThrow();
        var baseline=AllocationObservations.state(c,q,p,last+1);
        var inputs=new JsonObject();inputs.add("baseline",baseline);inputs.addProperty("saved_pile",pileTag.toString());inputs.addProperty("saved_cache",cacheTag.toString());inputs.add("saved_adults",new Gson().toJsonTree(tags.stream().map(CompoundTag::toString).toList()));inputs.add("saved_tick_counts",new Gson().toJsonTree(ticks));
        ReconciliationEvidence.log("t01_finite_inputs","matched_saved_input",inputs);
        long invocations=p.catchup().invocations();String previous="";
        for(int offset=1;offset<=400;offset++){
            long now=last+offset;
            ReconciliationGameTest.atTime(c,now,()->AutomaticCatchupGameTest.adults(c,q).forEach(LasiusNigerEntity::tick));
            AutomaticCatchupGameTest.tickPile(c,p,now);
            c.assertTrue(p.catchup().invocations()==invocations&&p.catchup().appliedTicks()==0,"Matched live control never invokes aggregate catch-up");
            var row=AllocationObservations.state(c,q,p,now);
            String signature=row.get("cache")+":"+row.get("brood")+":"+bodies.stream().map(a->a.getUUID()+":"+a.getMainHandItem()+":"+a.nutrition().consumedUnits()+":"+a.nutrition().receivedSugar()+":"+a.workerTasks().recipientId()).toList();
            if(!signature.equals(previous)||Set.of(11,40,80,120,200,300,400).contains(offset)){
                row.addProperty("offset",offset);row.addProperty("point","finite_matched_live");ReconciliationEvidence.log("t01_finite_live","ordinary_consecutive_ticks",row);previous=signature;
            }
        }
        var live=AllocationObservations.state(c,q,p,last+400);live.addProperty("hungry_worker",hungry.getUUID().toString());
        var finalBodies=AutomaticCatchupGameTest.adults(c,q);live.add("final_members",ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE,ColonyMembers.get(l)).getOrThrow());
        long units=cache.size()+finalBodies.stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum()+p.consumedFood();
        c.assertTrue(units==5&&bodies.stream().allMatch(a->a.nutrition().snapshot().valid())&&p.records().stream().allMatch(r->r.nutrition().snapshot().valid()),"Live branch conserves exactly five original units, including two queen receipts, with valid debit equations");
        live.addProperty("accounted_units",units);live.addProperty("aggregate_invocations",p.catchup().invocations()-invocations);ReconciliationEvidence.log("t01_finite_live_final","ordinary_consecutive_ticks",live);
        // The measured ordinary counterexample is specific by recipient and kind, not just total consumption.
        c.assertTrue(q.nutrition().apples()==1&&q.nutrition().chickens()==1&&q.nutrition().berries()==0&&hungry.nutrition().consumedUnits()==0&&p.consumedApples()==0&&p.consumedBerries()==0&&p.consumedChickens()==1,"Ordinary finite counterexample: funding stays with queen, hungry forager gets no food, larva gets chicken only");
        var nurses=bodies.stream().filter(a->a.form()==AntForm.WORKER&&a!=hungry).toList();
        c.assertTrue(nurses.size()==2&&nurses.get(0).nutrition().apples()==1&&nurses.get(0).nutrition().berries()==0&&nurses.get(1).nutrition().apples()==0&&nurses.get(1).nutrition().berries()==1&&nurses.stream().allMatch(a->a.nutrition().chickens()==0),"Original first and second nurses receive apple and berry respectively, with no protein receipt");
        if(liveOnly)return new Result(q,p,hungry,cache,live);
        for(var a:finalBodies)a.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        l.getDataStorage().set(ColonyMembers.TYPE,ColonyMembers.CODEC.parse(JsonOps.INSTANCE,members).getOrThrow());
        l.getDataStorage().set(ChamberRegistry.TYPE,ChamberRegistry.CODEC.parse(JsonOps.INSTANCE,chambers).getOrThrow());
        var restored=new ArrayList<LasiusNigerEntity>();
        for(int i=0;i<tags.size();i++){var a=ReconciliationGameTest.load(c,tags.get(i));a.tickCount=ticks.get(i);c.assertTrue(l.tryAddFreshEntityWithPassengers(a),"Matched original adult identity restored");restored.add(a);}
        var queen=restored.getFirst();var back=BroodGameTest.restorePile(c,p,pileTag);
        var pos=cache.getBlockPos();var block=cache.getBlockState();l.removeBlockEntity(pos);
        var stock=(NestCache)BlockEntity.loadStatic(pos,block,cacheTag,l.registryAccess());c.assertTrue(stock!=null,"Matched cache reconstruction");l.setBlockEntity(stock);stock.refreshDisplay();
        var check=AllocationObservations.state(c,queen,back,last+1);
        c.assertTrue(check.get("adults").equals(baseline.get("adults"))&&check.get("brood").equals(baseline.get("brood"))&&check.get("cache").equals(baseline.get("cache"))&&check.get("adult_order").equals(baseline.get("adult_order")),"Matched input equality includes nutrition, fasting, roles, position, cargo, partial actions, cooldown, tick count, brood, supplies and registry order");
        check.addProperty("point","finite_catchup_matched_input");ReconciliationEvidence.log("t01_finite_matched_input","exact_saved_reconstruction",check);
        return new Result(queen,back,restored.stream().filter(a->a.getUUID().equals(hungry.getUUID())).findFirst().orElseThrow(),stock,live);
    }
}
