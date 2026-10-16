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
 * Ordinary branch uses native tickNonPassenger and the nursery block ticker at every consecutive time.
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
        var initialLedger=CatchupItemLedger.snapshot(c,q,p,cache);
        var inputs=new JsonObject();inputs.add("baseline",baseline);inputs.addProperty("saved_pile",pileTag.toString());inputs.addProperty("saved_cache",cacheTag.toString());inputs.add("saved_adults",new Gson().toJsonTree(tags.stream().map(CompoundTag::toString).toList()));inputs.add("saved_tick_counts",new Gson().toJsonTree(ticks));
        ReconciliationEvidence.log("t01_finite_inputs","matched_saved_input",inputs);
        long invocations=p.catchup().invocations();String previous="";
        var cadence=new JsonObject();cadence.add("initial",LiveReferenceDriver.counters(c,q));
        for(int offset=1;offset<=400;offset++){
            long now=last+offset;
            LiveReferenceDriver.adults(c,q,now);
            AutomaticCatchupGameTest.tickPile(c,p,now);
            c.assertTrue(ComparisonEvidence.longs(p.catchup().invocations(),invocations,"==","p.catchup().invocations()==invocations")&&ComparisonEvidence.longs(p.catchup().appliedTicks(),0,"==","p.catchup().appliedTicks()==0"),"Matched live control never invokes aggregate catch-up");
            var row=AllocationObservations.state(c,q,p,now);
            String signature=row.get("cache")+":"+row.get("brood")+":"+AutomaticCatchupGameTest.adults(c,q).stream().map(a->a.getUUID()+":"+a.getMainHandItem()+":"+a.nutrition().consumedUnits()+":"+a.nutrition().receivedSugar()+":"+a.workerTasks().recipientId()+":"+a.workerTasks().sharing().target()+":"+a.workerTasks().sharing().actionTicks()+":"+a.workerTasks().sharing().cooldownRemaining()+":"+a.workerTasks().phase()).toList();
            if(!signature.equals(previous)||Set.of(11,40,80,120,200,300,400).contains(offset)){
                row.addProperty("offset",offset);row.addProperty("point","finite_matched_live");ReconciliationEvidence.log("t01_finite_live","ordinary_consecutive_ticks",row);previous=signature;
            }
        }
        cadence.add("final",LiveReferenceDriver.counters(c,q));cadence.addProperty("steps",400);cadence.addProperty("aggregate_invocations",p.catchup().invocations()-invocations);
        ReconciliationEvidence.log("t02_finite_cadence","native_reference_counters",cadence);
        var live=AllocationObservations.state(c,q,p,last+400);live.addProperty("hungry_worker",hungry.getUUID().toString());
        var finalBodies=AutomaticCatchupGameTest.adults(c,q);live.add("final_members",ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE,ColonyMembers.get(l)).getOrThrow());
        long units=cache.size()+finalBodies.stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum()+p.consumedFood();
        boolean exactItems=CatchupItemLedger.compare(initialLedger,CatchupItemLedger.snapshot(c,q,p,cache),"live",live);
        live.add("initial_item_ledger",initialLedger);live.addProperty("accounted_units",units);
        ReconciliationEvidence.log("t04_live_ledger","before_conservation_assertion",live);
        c.assertTrue(ComparisonEvidence.flag(exactItems,"exactItems")&&ComparisonEvidence.longs(units,5,"==","units==5")&&ComparisonEvidence.flag(bodies.stream().allMatch(a->a.nutrition().snapshot().valid()),"bodies.stream().allMatch(a->a.nutrition().snapshot().valid())")&&ComparisonEvidence.flag(p.records().stream().allMatch(r->r.nutrition().snapshot().valid()),"p.records().stream().allMatch(r->r.nutrition().snapshot().valid())"),"Live branch conserves exactly five original units, including two queen receipts, with valid debit equations");
        live.addProperty("accounted_units",units);live.addProperty("aggregate_invocations",p.catchup().invocations()-invocations);ReconciliationEvidence.log("t01_finite_live_final","ordinary_consecutive_ticks",live);
        c.assertTrue(ComparisonEvidence.longs(p.records().getFirst().nutrition().gainedProtein(),CatchupOutcomes.LARVAL_PROTEIN,"==","p.records().getFirst().nutrition().gainedProtein()==CatchupOutcomes.LARVAL_PROTEIN"),"Frozen live larval protein reference is 8000");
        if(liveOnly)return new Result(q,p,hungry,cache,live);
        for(var a:finalBodies)a.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        l.getDataStorage().set(ColonyMembers.TYPE,ColonyMembers.CODEC.parse(JsonOps.INSTANCE,members).getOrThrow());
        l.getDataStorage().set(ChamberRegistry.TYPE,ChamberRegistry.CODEC.parse(JsonOps.INSTANCE,chambers).getOrThrow());
        var restored=new ArrayList<LasiusNigerEntity>();
        for(int i=0;i<tags.size();i++){var a=ReconciliationGameTest.load(c,tags.get(i));a.tickCount=ticks.get(i);c.assertTrue(ComparisonEvidence.flag(l.tryAddFreshEntityWithPassengers(a),"l.tryAddFreshEntityWithPassengers(a)"),"Matched original adult identity restored");restored.add(a);}
        var queen=restored.getFirst();var back=BroodGameTest.restorePile(c,p,pileTag);
        var pos=cache.getBlockPos();var block=cache.getBlockState();l.removeBlockEntity(pos);
        var stock=(NestCache)BlockEntity.loadStatic(pos,block,cacheTag,l.registryAccess());c.assertTrue(ComparisonEvidence.objects(stock,null,"!=","stock!=null"),"Matched cache reconstruction");l.setBlockEntity(stock);stock.refreshDisplay();
        var check=AllocationObservations.state(c,queen,back,last+1);
        c.assertTrue(ComparisonEvidence.equal(check.get("adults"),baseline.get("adults"),"check.get(\"adults\").equals(baseline.get(\"adults\"))")&&ComparisonEvidence.equal(check.get("brood"),baseline.get("brood"),"check.get(\"brood\").equals(baseline.get(\"brood\"))")&&ComparisonEvidence.equal(check.get("cache"),baseline.get("cache"),"check.get(\"cache\").equals(baseline.get(\"cache\"))")&&ComparisonEvidence.equal(check.get("adult_order"),baseline.get("adult_order"),"check.get(\"adult_order\").equals(baseline.get(\"adult_order\"))"),"Matched input equality includes nutrition, fasting, roles, position, cargo, partial actions, cooldown, tick count, brood, supplies and registry order");
        check.addProperty("point","finite_catchup_matched_input");ReconciliationEvidence.log("t01_finite_matched_input","exact_saved_reconstruction",check);
        return new Result(queen,back,restored.stream().filter(a->a.getUUID().equals(hungry.getUUID())).findFirst().orElseThrow(),stock,live);
    }
}
