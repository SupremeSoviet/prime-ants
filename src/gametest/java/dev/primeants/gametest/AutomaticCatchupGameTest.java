package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.time.*;
import dev.primeants.worker.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Small prospective acceptance set. Timestamp fixtures invoke only ordinary entity/block lifecycle methods.
 * Genuine native disappearance/reload is independently exercised by ReconciliationChunkGameTest. */
public final class AutomaticCatchupGameTest {
    void colony(GameTestHelper c,boolean mature,BiConsumer<LasiusNigerEntity,BroodPile> body){
        var f=new WorkerForagingGameTest();var q=f.start(c);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0])return;var p=new NursingGameTest().pile(c,q);
            if(p==null||p.loadedTicks()<2||mature&&(!p.operational()||f.workers(c,q).size()!=3||f.workers(c,q).stream().anyMatch(LasiusNigerEntity::isCallow)))return;
            started[0]=true;body.accept(q,p);
        });
    }
    static void tickPile(GameTestHelper c,BroodPile p,long now){
        ReconciliationGameTest.atTime(c,now,()->((BroodPileBlock)p.getBlockState().getBlock()).getTicker(c.getLevel(),p.getBlockState(),NurseryBlocks.BROOD_TYPE).tick(c.getLevel(),p.getBlockPos(),p.getBlockState(),p));
    }
    static List<LasiusNigerEntity> adults(GameTestHelper c,LasiusNigerEntity q){
        var result=new ArrayList<LasiusNigerEntity>();result.add(q);
        for(var m:ColonyMembers.get(c.getLevel()).members(q.getUUID()))if(!m.dead())result.add((LasiusNigerEntity)c.getLevel().getEntity(m.worker()));
        return result;
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void nativeServerCadenceAgreesWithOrdinaryReferenceAcrossSearchBoundaries(GameTestHelper c){
        var f=new WorkerForagingGameTest();var q=f.start(c);int[] steps={-1};
        Map<UUID,Integer> initial=new LinkedHashMap<>(),previous=new LinkedHashMap<>(),boundaries=new LinkedHashMap<>();
        long[] invocations={0},applied={0};
        c.onEachTick(()->{
            var p=new NursingGameTest().pile(c,q);
            if(steps[0]<0){
                if(p==null||!p.operational()||f.workers(c,q).size()!=3||f.workers(c,q).stream().anyMatch(LasiusNigerEntity::isCallow))return;
                for(var a:adults(c,q)){initial.put(a.getUUID(),a.tickCount);previous.put(a.getUUID(),a.tickCount);boundaries.put(a.getUUID(),0);}
                invocations[0]=p.catchup().invocations();applied[0]=p.catchup().appliedTicks();steps[0]=0;return;
            }
            // No entity or nursery lifecycle calls in this phase: the real server owns these ticks.
            steps[0]++;
            for(var a:adults(c,q)){
                c.assertTrue(ComparisonEvidence.flag(previous.containsKey(a.getUUID()),"previous.containsKey(a.getUUID())")&&ComparisonEvidence.longs(a.tickCount,previous.get(a.getUUID())+1,"==","a.tickCount==previous.get(a.getUUID())+1"),"Genuine server advances every surviving adult once per reference observation");
                previous.put(a.getUUID(),a.tickCount);
                if(a.tickCount%20==0){boundaries.merge(a.getUUID(),1,Integer::sum);var row=new JsonObject();row.addProperty("step",steps[0]);row.addProperty("id",a.getUUID().toString());row.addProperty("tick_count",a.tickCount);row.addProperty("time",c.getLevel().getGameTime());ReconciliationEvidence.log("t02_cadence","genuine_server_boundary",row);}
            }
            c.assertTrue(ComparisonEvidence.longs(p.catchup().invocations(),invocations[0],"==","p.catchup().invocations()==invocations[0]")&&ComparisonEvidence.longs(p.catchup().appliedTicks(),applied[0],"==","p.catchup().appliedTicks()==applied[0]"),"Genuine live colony earns no aggregate application");
            if(steps[0]<60)return;
            var referenceInitial=LiveReferenceDriver.counters(c,q);long last=c.getLevel().getGameTime();
            for(int offset=1;offset<=60;offset++){LiveReferenceDriver.adults(c,q,last+offset);tickPile(c,p,last+offset);}
            var referenceFinal=LiveReferenceDriver.counters(c,q);
            c.assertTrue(ComparisonEvidence.longs(p.catchup().invocations(),invocations[0],"==","p.catchup().invocations()==invocations[0]")&&ComparisonEvidence.longs(p.catchup().appliedTicks(),applied[0],"==","p.catchup().appliedTicks()==applied[0]"),"Reference calibration colony earns no aggregate application");
            for(var a:adults(c,q))c.assertTrue(ComparisonEvidence.longs(previous.get(a.getUUID())-initial.get(a.getUUID()),60,"==","previous.get(a.getUUID())-initial.get(a.getUUID())==60")&&ComparisonEvidence.longs(boundaries.get(a.getUUID()),3,"==","boundaries.get(a.getUUID())==3")&&ComparisonEvidence.longs(referenceFinal.get(a.getUUID().toString()).getAsInt()-referenceInitial.get(a.getUUID().toString()).getAsInt(),60,"==","referenceFinal.get(a.getUUID().toString()).getAsInt()-referenceInitial.get(a.getUUID().toString()).getAsInt()==60"),"Native and explicit reference cadence agree: sixty increments and three twenty-tick boundaries per adult");
            var row=new JsonObject();row.add("native_initial",new Gson().toJsonTree(initial));row.add("native_final",new Gson().toJsonTree(previous));row.add("native_boundaries",new Gson().toJsonTree(boundaries));row.add("reference_initial",referenceInitial);row.add("reference_final",referenceFinal);row.addProperty("native_steps",60);row.addProperty("reference_steps",60);row.addProperty("aggregate_invocations",p.catchup().invocations()-invocations[0]);row.addProperty("aggregate_applied",p.catchup().appliedTicks()-applied[0]);ReconciliationEvidence.log("t02_cadence","native_server_calibration_passed",row);c.succeed();
        });
    }
    private static void log(GameTestHelper c,String scenario,BroodPile p,JsonObject extra){
        extra.addProperty("queen",p.queenId().toString());
        extra.addProperty("scenario",scenario);extra.addProperty("applied_ticks",p.catchup().appliedTicks());extra.addProperty("pending_ticks",p.catchup().pendingTicks());
        extra.addProperty("events",p.catchup().lastEvents());extra.addProperty("application_nanoseconds",p.catchup().lastNanos());
        extra.addProperty("status",p.catchup().status());extra.addProperty("loaded_ticks",p.loadedTicks());extra.addProperty("occupied_adults",ColonyMembers.get(c.getLevel()).occupied(p.queenId()));
        extra.addProperty("brood_slots",p.records().size());extra.add("consumed_brood",new Gson().toJsonTree(p.consumed()));
        ReconciliationEvidence.log("automatic_acceptance","ordinary_production_hooks",extra);
    }
    static LasiusNigerEntity replace(GameTestHelper c,LasiusNigerEntity old,CompoundTag tag){
        old.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);var copy=ReconciliationGameTest.load(c,tag);
        c.assertTrue(ComparisonEvidence.flag(c.getLevel().tryAddFreshEntityWithPassengers(copy),"c.getLevel().tryAddFreshEntityWithPassengers(copy)"),"Original identity restored normally");return copy;
    }
    static NestCache finiteCache(GameTestHelper c,LasiusNigerEntity q,List<ItemStack> units){
        var plan=q.founding().plan();c.getLevel().setBlock(plan.cache(),NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var cache=(NestCache)c.getLevel().getBlockEntity(plan.cache());
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());
        out.putInt("InventoryFormat",3);out.putString("Colony",q.getUUID().toString());out.store("Entrance",net.minecraft.core.BlockPos.CODEC,plan.entrance());out.putString("Direction",plan.direction().getName());out.store("Contents",ItemStack.CODEC.listOf(),units);
        cache.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),out.buildResult()));cache.setChanged();return cache;
    }
    private static long foodUnits(GameTestHelper c,LasiusNigerEntity q,NestCache cache){
        return cache.size()+adults(c,q).stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum();
    }
    private static void boundary(GameTestHelper c,String field){
        var ant=AntEntities.WORKER.create(c.getLevel(),EntitySpawnReason.BREEDING);c.assertTrue(ComparisonEvidence.objects(ant,null,"!=","ant!=null"),"Worker factory");
        ant.initializeCallow(UUID.randomUUID(),UUID.randomUUID(),c.absolutePos(net.minecraft.core.BlockPos.ZERO),1000,1000);
        ant.setNoAi(true);ant.setNoGravity(true);ant.setPos(Vec3.atCenterOf(c.absolutePos(new net.minecraft.core.BlockPos(1,6,1))));
        c.assertTrue(ComparisonEvidence.flag(ant.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000),"ant.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)"),"One accounted initial apple");
        var tag=ReconciliationGameTest.save(c,ant);var life=tag.getCompound("AdultLife").orElseThrow();life.putLong(field,Long.MAX_VALUE);life.putBoolean("Started",true);
        ant=ReconciliationGameTest.load(c,tag);var input=ant.biologySnapshot();ant.tick();
        var saved=ReconciliationGameTest.save(c,ant);
        c.assertTrue(ComparisonEvidence.longs(saved.getCompound("AdultLife").orElseThrow().getLong(field).orElseThrow(),Long.MAX_VALUE,"==","saved.getCompound(\"AdultLife\").orElseThrow().getLong(field).orElseThrow()==Long.MAX_VALUE"),"Accepted maximum saturates without exception: "+field);
        c.assertTrue(ComparisonEvidence.longs(ant.elapsedAgeTicks(),input.age()+1,"==","ant.elapsedAgeTicks()==input.age()+1")&&ComparisonEvidence.longs(ant.nutrition().sugar(),3999,"==","ant.nutrition().sugar()==3999")&&ComparisonEvidence.longs(ant.nutrition().spentSugar(),1,"==","ant.nutrition().spentSugar()==1")&&ComparisonEvidence.longs(ant.adultLife().coverageRemaining(),3,"==","ant.adultLife().coverageRemaining()==3"),"All ordinary non-boundary costs exact");
        c.assertTrue(ComparisonEvidence.equal(saved,ReconciliationGameTest.save(c,ReconciliationGameTest.load(c,saved)),"saved.equals(ReconciliationGameTest.save(c,ReconciliationGameTest.load(c,saved)))"),"Maximum save reconstructs exactly");
        var row=new Gson().toJsonTree(ant.biologySnapshot()).getAsJsonObject();row.addProperty("counter",field);ReconciliationEvidence.log("counter_saturation","accepted_maximum_ordinary_save",row);c.succeed();
    }
    @GameTest(maxTicks=1000) public void acceptedMaintenanceMaximumSaturatesOrdinaryAndReconstructs(GameTestHelper c){boundary(c,"Maintenance");}
    @GameTest(maxTicks=1000) public void acceptedCoveredTicksMaximumSaturatesOrdinaryAndReconstructs(GameTestHelper c){boundary(c,"CoveredTicks");}

    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void shortReloadMatchesLiveTicksAndDoesNotRepeatAfterSave(GameTestHelper c){
        colony(c,false,(q,p)->{
            long last=p.lastSimulationGameTime().orElseThrow(),age=q.elapsedAgeTicks();
            var adultTag=ReconciliationGameTest.save(c,q);var pileTag=p.saveWithFullMetadata(c.getLevel().registryAccess());
            // Matched finite live control: same original identities, 12 ordinary ticks, no new supplies.
            for(int i=1;i<=12;i++){long time=last+i;LiveReferenceDriver.adults(c,q,time);tickPile(c,p,time);}
            var expected=q.biologySnapshot();var expectedBrood=p.records().stream().map(r->r.id()+":"+r.stage()+":"+r.progress()+":"+r.nourishment()).toList();long reserve=q.bodyReserve();
            var returning=replace(c,q,adultTag);var resumed=BroodGameTest.restorePile(c,p,pileTag);
            ReconciliationGameTest.atTime(c,last+12,()->LiveReferenceDriver.adult(c,returning,last+12));tickPile(c,resumed,last+12);
            c.assertTrue(ComparisonEvidence.equal(returning.biologySnapshot(),expected,"returning.biologySnapshot().equals(expected)")&&ComparisonEvidence.longs(returning.elapsedAgeTicks(),age+12,"==","returning.elapsedAgeTicks()==age+12"),"Exact survivor age/maintenance/callow, C once");
            c.assertTrue(ComparisonEvidence.equal(resumed.records().stream().map(r->r.id()+":"+r.stage()+":"+r.progress()+":"+r.nourishment()).toList(),expectedBrood,"resumed.records().stream().map(r->r.id()+\":\"+r.stage()+\":\"+r.progress()+\":\"+r.nourishment()).toList().equals(expectedBrood)")&&ComparisonEvidence.longs(returning.bodyReserve(),reserve,"==","returning.bodyReserve()==reserve"),"Exact short brood identities and reserve conservation against ordinary control");
            c.assertTrue(ComparisonEvidence.longs(resumed.catchup().appliedTicks(),11,"==","resumed.catchup().appliedTicks()==11")&&ComparisonEvidence.longs(resumed.catchup().pendingTicks(),0,"==","resumed.catchup().pendingTicks()==0"),"Eleven absent ticks plus one ordinary C");
            var before=returning.biologySnapshot();var saved=resumed.saveWithFullMetadata(c.getLevel().registryAccess());long loaded=resumed.loadedTicks();
            var replay=BroodGameTest.restorePile(c,resumed,saved);tickPile(c,replay,last+12);
            // Deliberate same-time biological hook probe; this is not another ordinary lifecycle step.
            ReconciliationGameTest.atTime(c,last+12,returning::tick);
            c.assertTrue(ComparisonEvidence.equal(returning.biologySnapshot(),before,"returning.biologySnapshot().equals(before)")&&ComparisonEvidence.longs(replay.catchup().appliedTicks(),11,"==","replay.catchup().appliedTicks()==11")&&ComparisonEvidence.longs(replay.loadedTicks(),loaded,"==","replay.loadedTicks()==loaded")&&ComparisonEvidence.equal(replay.saveWithFullMetadata(c.getLevel().registryAccess()),saved,"replay.saveWithFullMetadata(c.getLevel().registryAccess()).equals(saved)"),"Normal same-time save/reload does not repeat adult C, nursery C, feeding/age/births or progress");
            var row=new JsonObject();row.add("live",new Gson().toJsonTree(expected));row.add("catchup",new Gson().toJsonTree(before));row.addProperty("brood_equal",true);row.addProperty("reserve",reserve);log(c,"short",resumed,row);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void longNoFoodReturnKillsRealWorkersReleasesCargoOnceAndRegressesStage(GameTestHelper c){
        colony(c,true,(q,p)->{
            var ws=new ArrayList<>(new WorkerForagingGameTest().workers(c,q));var original=ws.stream().map(LasiusNigerEntity::getUUID).toList();
            var aging=ws.getFirst();var agingTag=ReconciliationGameTest.save(c,aging);agingTag.getCompound("AdultLife").orElseThrow().putLong("Lifespan",aging.elapsedAgeTicks()+20);agingTag.putLong("CallowTicks",0);agingTag.putLong("CallowDuration",1_000_000);ws.set(0,replace(c,aging,agingTag));ws.getFirst().setHealth(1);
            ws.getFirst().setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CLAY_BALL));
            long last=p.lastSimulationGameTime().orElseThrow(),loaded=p.loadedTicks();
            var resumed=BroodGameTest.restorePile(c,p,p.saveWithFullMetadata(c.getLevel().registryAccess()));
            tickPile(c,resumed,last+1_000_001);
            c.assertTrue(ComparisonEvidence.longs(resumed.catchup().pendingTicks(),0,"==","resumed.catchup().pendingTicks()==0")&&ComparisonEvidence.longs(resumed.catchup().appliedTicks(),1_000_000,"==","resumed.catchup().appliedTicks()==1_000_000")&&ComparisonEvidence.longs(resumed.loadedTicks(),loaded+1_000_001,"==","resumed.loadedTicks()==loaded+1_000_001"),"Full million-tick interval accounted in bounded batches");
            c.assertTrue(ComparisonEvidence.flag(original.stream().allMatch(id->ColonyMembers.get(c.getLevel()).member(id).dead()&&AdultHistory.get(c.getLevel()).recorded(id)),"original.stream().allMatch(id->ColonyMembers.get(c.getLevel()).member(id).dead()&&AdultHistory.get(c.getLevel()).recorded(id))")&&ComparisonEvidence.longs(ColonyMembers.get(c.getLevel()).occupied(q.getUUID()),0,"==","ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0"),"Controlled no-food survivor count is exactly zero real workers");
            c.assertTrue(ComparisonEvidence.flag(ws.stream().allMatch(w->w.getMainHandItem().isEmpty()),"ws.stream().allMatch(w->w.getMainHandItem().isEmpty())"),"Cargo leaves each dead carrier once through existing custody");
            var cause=JsonParser.parseString(AdultHistory.get(c.getLevel()).records().get(original.getFirst().toString())).getAsJsonObject().get("cause").getAsString();
            c.assertTrue(ComparisonEvidence.equal(cause,"age_limit","cause.equals(\"age_limit\")")&&ComparisonEvidence.flag(original.stream().skip(1).allMatch(id->JsonParser.parseString(AdultHistory.get(c.getLevel()).records().get(id.toString())).getAsJsonObject().get("cause").getAsString().equals("starvation")),"original.stream().skip(1).allMatch(id->JsonParser.parseString(AdultHistory.get(c.getLevel()).records().get(id.toString())).getAsJsonObject().get(\"cause\").getAsString().equals(\"starvation\"))"),"Real age death and starvation deaths retain existing cause precedence");
            c.assertTrue(ComparisonEvidence.longs(ws.getFirst().callowAgeTicks(),38,"==","ws.getFirst().callowAgeTicks()==38"),"Thirty-eight nonterminal callow steps in the fatal 39-tick age span; terminal callow action omitted once");
            UUID cargo=UUID.nameUUIDFromBytes(("worker-cargo:"+original.getFirst()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long pendingCargo=TransferCustody.get(c.getLevel()).contents().stream().filter(t->t.id().equals(cargo)).mapToLong(t->t.stack().getCount()).sum();
            long worldCargo=c.getLevel().getEntity(cargo) instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.CLAY_BALL)?item.getItem().getCount():0;
            c.assertTrue(ComparisonEvidence.longs(worldCargo+pendingCargo,1,"==","worldCargo+pendingCargo==1"),"Exactly one original clay unit in its stable custody/destination identity; no material spending/income");
            var history=AdultHistory.get(c.getLevel()).records();var transfers=TransferCustody.get(c.getLevel()).contents().toString();
            var replay=BroodGameTest.restorePile(c,resumed,resumed.saveWithFullMetadata(c.getLevel().registryAccess()));tickPile(c,replay,last+1_000_001);
            c.assertTrue(ComparisonEvidence.equal(history,AdultHistory.get(c.getLevel()).records(),"history.equals(AdultHistory.get(c.getLevel()).records())")&&ComparisonEvidence.equal(transfers,TransferCustody.get(c.getLevel()).contents().toString(),"transfers.equals(TransferCustody.get(c.getLevel()).contents().toString())"),"Replay does not repeat death or cargo custody");
            c.assertTrue(ComparisonEvidence.objects(resumed.stageEvaluation().stage(),ColonyStage.FOUNDING,"==","resumed.stageEvaluation().stage()==ColonyStage.FOUNDING")&&ComparisonEvidence.longs(resumed.catchup().lastEvents(),ColonyCatchup.MAX_EVENTS,"<=","resumed.catchup().lastEvents()<=ColonyCatchup.MAX_EVENTS"),"Existing StageRules reevaluates actual deaths; fixed event bound");
            var row=new JsonObject();row.addProperty("expected_worker_survivors",0);row.addProperty("actual_worker_survivors",0);row.addProperty("stage",resumed.stageEvaluation().stage().name());row.add("death_receipts",new Gson().toJsonTree(history));row.addProperty("cargo_cleared",true);row.addProperty("cargo_kind","minecraft:clay_ball");row.addProperty("initial_material_units",1);row.addProperty("final_material_units",worldCargo+pendingCargo);row.addProperty("cargo_identity",cargo.toString());row.addProperty("world_cargo",worldCargo);row.addProperty("pending_cargo",pendingCargo);log(c,"long_starving_stage",resumed,row);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void fixedEventBoundPersistsAppliedAndPendingWorkUntilNextAutomaticInvocation(GameTestHelper c){
        colony(c,true,(q,p)->{
            var worker=new WorkerForagingGameTest().workers(c,q).getFirst();worker.setPermanentlyInvulnerable(true);
            long last=p.lastSimulationGameTime().orElseThrow();tickPile(c,p,last+1_000_001);
            long applied=p.catchup().appliedTicks(),pending=p.catchup().pendingTicks();
            c.assertTrue(ComparisonEvidence.longs(p.catchup().lastEvents(),ColonyCatchup.MAX_EVENTS,"<=","p.catchup().lastEvents()<=ColonyCatchup.MAX_EVENTS")&&ComparisonEvidence.longs(applied,1_000_000,"==","applied==1_000_000")&&ComparisonEvidence.longs(pending,0,"==","pending==0")&&ComparisonEvidence.flag(worker.isAlive(),"worker.isAlive()"),"Protected no-op damage folds analytically and the entire million ticks complete in one bounded invocation");
            var row=new JsonObject();row.addProperty("first_applied",applied);row.addProperty("first_pending",pending);row.addProperty("first_events",p.catchup().lastEvents());row.addProperty("first_nanoseconds",p.catchup().lastNanos());
            var saved=p.saveWithFullMetadata(c.getLevel().registryAccess());var restored=BroodGameTest.restorePile(c,p,saved);
            c.assertTrue(ComparisonEvidence.longs(restored.catchup().appliedTicks(),applied,"==","restored.catchup().appliedTicks()==applied")&&ComparisonEvidence.longs(restored.catchup().pendingTicks(),pending,"==","restored.catchup().pendingTicks()==pending"),"Normal save/reload retains application and bounded remainder exactly");
            var biology=worker.biologySnapshot();tickPile(c,restored,last+1_000_001);
            c.assertTrue(ComparisonEvidence.equal(worker.biologySnapshot(),biology,"worker.biologySnapshot().equals(biology)")&&ComparisonEvidence.equal(restored.saveWithFullMetadata(c.getLevel().registryAccess()),saved,"restored.saveWithFullMetadata(c.getLevel().registryAccess()).equals(saved)"),"Completed protected application reconstructs and same-C does not replay");
            worker.setPermanentlyInvulnerable(false);tickPile(c,restored,last+1_001_002);
            c.assertTrue(ComparisonEvidence.longs(restored.catchup().pendingTicks(),0,"==","restored.catchup().pendingTicks()==0")&&ComparisonEvidence.longs(restored.catchup().appliedTicks(),1_001_000,"==","restored.catchup().appliedTicks()==1_001_000")&&!ComparisonEvidence.flag(worker.isAlive(),"worker.isAlive()"),"Later unprotected absence applies real damage without replaying prior work");
            row.addProperty("single_invocation_complete",true);log(c,"event_bound_pending_replay",restored,row);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void partialMemberLoadingPersistsPendingAndExcludesWaitingOrdinaryTicks(GameTestHelper c){
        colony(c,true,(q,p)->{
            var absent=new WorkerForagingGameTest().workers(c,q).getFirst();var adultTag=ReconciliationGameTest.save(c,absent);
            long last=p.lastSimulationGameTime().orElseThrow(),loaded=p.loadedTicks(),age=q.elapsedAgeTicks();
            absent.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);tickPile(c,p,last+7);
            c.assertTrue(ComparisonEvidence.longs(p.catchup().pendingTicks(),6,"==","p.catchup().pendingTicks()==6")&&ComparisonEvidence.longs(p.loadedTicks(),loaded,"==","p.loadedTicks()==loaded")&&ComparisonEvidence.longs(p.lastSimulationGameTime().orElseThrow(),last,"==","p.lastSimulationGameTime().orElseThrow()==last")&&!ComparisonEvidence.flag(ColonyMembers.get(c.getLevel()).member(absent.getUUID()).dead(),"ColonyMembers.get(c.getLevel()).member(absent.getUUID()).dead()"),"Unavailable member remains reserved UNKNOWN; six ticks saved pending");
            var restored=BroodGameTest.restorePile(c,p,p.saveWithFullMetadata(c.getLevel().registryAccess()));
            for(int i=7;i<=9;i++){long t=last+i;LiveReferenceDriver.adults(c,q,t);tickPile(c,restored,t);}
            c.assertTrue(ComparisonEvidence.longs(restored.catchup().pendingTicks(),8,"==","restored.catchup().pendingTicks()==8")&&ComparisonEvidence.longs(q.elapsedAgeTicks(),age+3,"==","q.elapsedAgeTicks()==age+3"),"Waiting ordinary adults run without dropping pending nursery work");
            var wrong=adultTag.copy();wrong.putString("QueenId",UUID.randomUUID().toString());var conflicting=ReconciliationGameTest.load(c,wrong);c.assertTrue(ComparisonEvidence.flag(c.getLevel().tryAddFreshEntityWithPassengers(conflicting),"c.getLevel().tryAddFreshEntityWithPassengers(conflicting)"),"Disclosed contradictory saved body uses the original UUID but foreign lineage");
            tickPile(c,restored,last+10);
            c.assertTrue(ComparisonEvidence.longs(restored.catchup().pendingTicks(),9,"==","restored.catchup().pendingTicks()==9")&&ComparisonEvidence.longs(restored.catchup().appliedTicks(),0,"==","restored.catchup().appliedTicks()==0")&&ComparisonEvidence.longs(q.elapsedAgeTicks(),age+3,"==","q.elapsedAgeTicks()==age+3")&&!ComparisonEvidence.flag(ColonyMembers.get(c.getLevel()).member(absent.getUUID()).dead(),"ColonyMembers.get(c.getLevel()).member(absent.getUUID()).dead()"),"Conflicting identity defers before biology/resources and retains original reservation");
            conflicting.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var returning=ReconciliationGameTest.load(c,adultTag);c.assertTrue(ComparisonEvidence.flag(c.getLevel().tryAddFreshEntityWithPassengers(returning),"c.getLevel().tryAddFreshEntityWithPassengers(returning)"),"Original worker restored");
            ReconciliationGameTest.atTime(c,last+11,()->LiveReferenceDriver.adult(c,returning,last+11));tickPile(c,restored,last+11);
            c.assertTrue(ComparisonEvidence.longs(restored.catchup().pendingTicks(),0,"==","restored.catchup().pendingTicks()==0")&&ComparisonEvidence.longs(restored.catchup().appliedTicks(),10,"==","restored.catchup().appliedTicks()==10")&&ComparisonEvidence.longs(q.elapsedAgeTicks(),age+10,"==","q.elapsedAgeTicks()==age+10"),"Waiting ordinary adult ticks excluded exactly; pending completed automatically on original return");
            c.assertTrue(ComparisonEvidence.equal(returning.getUUID(),absent.getUUID(),"returning.getUUID().equals(absent.getUUID())")&&ComparisonEvidence.equal(restored.consumed(),p.consumed(),"restored.consumed().equals(p.consumed())"),"Original membership and brood identities conserved");
            log(c,"partial_restoration",restored,new JsonObject());c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void unknownTerrainDefersBiologyAndRestorationResumesAutomatically(GameTestHelper c){
        colony(c,false,(q,p)->{
            long last=p.lastSimulationGameTime().orElseThrow(),age=q.elapsedAgeTicks(),reserve=q.bodyReserve();var brood=p.records().stream().map(r->r.id()+":"+r.progress()+":"+r.neglectTicks()).toList();
            try(var hidden=UnavailableCells.hide(c,q.founding().plan().at(2,-2,-3))){
                tickPile(c,p,last+51);
                c.assertTrue(ComparisonEvidence.longs(p.catchup().pendingTicks(),50,"==","p.catchup().pendingTicks()==50")&&ComparisonEvidence.longs(q.elapsedAgeTicks(),age,"==","q.elapsedAgeTicks()==age")&&ComparisonEvidence.longs(q.bodyReserve(),reserve,"==","q.bodyReserve()==reserve")&&ComparisonEvidence.equal(brood,p.records().stream().map(r->r.id()+":"+r.progress()+":"+r.neglectTicks()).toList(),"brood.equals(p.records().stream().map(r->r.id()+\":\"+r.progress()+\":\"+r.neglectTicks()).toList())"),"UNKNOWN neither grants aggregate growth/resources nor starvation/neglect");
            }
            tickPile(c,p,last+52);c.assertTrue(ComparisonEvidence.longs(p.catchup().pendingTicks(),0,"==","p.catchup().pendingTicks()==0")&&ComparisonEvidence.longs(p.catchup().appliedTicks(),51,"==","p.catchup().appliedTicks()==51")&&ComparisonEvidence.longs(q.elapsedAgeTicks(),age+51,"==","q.elapsedAgeTicks()==age+51"),"Restored confirmed terrain resumes through ordinary nursery lifecycle");
            log(c,"unknown_terrain_restoration",p,new JsonObject());c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void confirmedFiniteCacheFeedsAdultsAndExistingFoodFedLarvaExactlyOnce(GameTestHelper c){
        finiteComparison(c,false,false);
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void matchedFiniteCacheLiveControlUsesOnlyOrdinaryProductionHooks(GameTestHelper c){finiteComparison(c,true,false);}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void finiteCacheNursesFirstRegistryOrderMatchesIdenticalLiveInput(GameTestHelper c){finiteComparison(c,false,true);}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void captured971FiniteConfigurationMatchesLive(GameTestHelper c){finiteComparison(c,false,false,"captured_971");}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void captured971NursesFirstConfigurationMatchesLive(GameTestHelper c){finiteComparison(c,false,true,"captured_971");}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void captured1500FiniteConfigurationMatchesLive(GameTestHelper c){finiteComparison(c,false,false,"captured_1500");}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void captured1500NursesFirstConfigurationMatchesLive(GameTestHelper c){finiteComparison(c,false,true,"captured_1500");}
    private void finiteComparison(GameTestHelper c,boolean liveOnly,boolean nursesFirst){
        finiteComparison(c,liveOnly,nursesFirst,null);
    }
    private void finiteComparison(GameTestHelper c,boolean liveOnly,boolean nursesFirst,String captured){
        var f=new WorkerForagingGameTest();var founder=f.start(c);boolean[] done={false};
        c.onEachTick(()->{
            var q=founder;
            if(done[0])return;var p=new NursingGameTest().pile(c,q);var ws=f.workers(c,q);
            if(p==null||!p.operational()||!p.records().isEmpty()||ws.stream().filter(w->w.workerTasks().caregiver(c.getLevel(),founder.founding().plan())).count()<2)return;
            done[0]=true;
            // Disclosed accepted-save fixture: two finite ingested egg-funding units, one paid existing dependent.
            c.assertTrue(ComparisonEvidence.flag(q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000),"q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)")&&ComparisonEvidence.flag(q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000),"q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000)")&&ComparisonEvidence.flag(q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN),"q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN)"),"Existing dependent egg costs debited from real finite receipts");
            var id=UUID.randomUUID();var r=new CompoundTag();r.putString("Id",id.toString());r.putString("Queen",q.getUUID().toString());r.putInt("Slot",0);r.putString("Stage","LARVA");r.putLong("Progress",0);r.putLong("Nourishment",0);r.putBoolean("Founding",false);
            var list=new net.minecraft.nbt.ListTag();list.add(r);var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.put("Brood",list);tag.putBoolean("Operational",false);
            var resumed=BroodGameTest.restorePile(c,p,tag);
            var hungry=ws.stream().filter(w->!w.workerTasks().nursing()).findFirst().orElse(ws.getFirst());var adultTag=ReconciliationGameTest.save(c,hungry);adultTag.getCompound("AdultLife").orElseThrow().putLong("Fasting",hungry.adultLife().grace()/2);
            hungry=replace(c,hungry,adultTag);
            var plan=q.founding().plan();c.getLevel().setBlock(plan.cache(),NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var cache=(NestCache)c.getLevel().getBlockEntity(plan.cache());
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());
            out.putInt("InventoryFormat",3);out.putString("Colony",q.getUUID().toString());out.store("Entrance",net.minecraft.core.BlockPos.CODEC,plan.entrance());out.putString("Direction",plan.direction().getName());
            out.store("Contents",ItemStack.CODEC.listOf(),List.of(new ItemStack(Items.APPLE),new ItemStack(Items.SWEET_BERRIES),new ItemStack(Items.CHICKEN)));
            cache.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),out.buildResult()));cache.setChanged();
            if(nursesFirst){
                var members=ColonyMembers.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyMembers.get(c.getLevel())).getOrThrow().getAsJsonArray();
                var ordered=new JsonArray();final UUID hungryId=hungry.getUUID();for(var member:members)if(!member.getAsJsonObject().get("worker").getAsString().equals(hungryId.toString()))ordered.add(member);for(var member:members)if(member.getAsJsonObject().get("worker").getAsString().equals(hungryId.toString()))ordered.add(member);
                c.getLevel().getDataStorage().set(ColonyMembers.TYPE,ColonyMembers.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,ordered).getOrThrow());
            }
            if(captured!=null){var restored=CapturedFiniteControl.restore(c,q,resumed,hungry,cache,captured,nursesFirst);q=restored.queen();resumed=restored.pile();hungry=restored.hungry();cache=restored.cache();}
            long last=resumed.lastSimulationGameTime().orElseThrow();AllocationObservations.log(c,q,resumed,last+1,"finite_baseline");
            var match=FiniteCacheLiveControl.measure(c,q,resumed,hungry,cache,last,liveOnly);q=match.queen();resumed=match.pile();hungry=match.hungry();cache=match.cache();
            if(liveOnly){c.succeed();return;}
            LiveReferenceDriver.adults(c,q,last+11);tickPile(c,resumed,last+11);
            AllocationObservations.log(c,q,resumed,last+11,"finite_eleven");
            var larva=resumed.records().stream().filter(b->b.id().equals(id)).findFirst().orElseThrow();
            long larvalProteinCredit=larva.nutrition().gainedProtein();
            c.assertTrue(ComparisonEvidence.flag(resumed.operational(),"resumed.operational()"),"Already opened queen controller effect survives the nursery's stale saved observation in either hook ordering");
            c.assertTrue(ComparisonEvidence.longs(cache.size()+adults(c,q).stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum()+resumed.consumedFood(),5,"==","cache.size()+adults(c,q).stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum()+resumed.consumedFood()==5")&&ComparisonEvidence.longs(resumed.catchup().pendingServices(),0,"==","resumed.catchup().pendingServices()==0"),"Fixed distribution conserves all five physical units and retains no trip state");
            var pending=resumed.saveWithFullMetadata(c.getLevel().registryAccess());resumed=BroodGameTest.restorePile(c,resumed,pending);tickPile(c,resumed,last+11);c.assertTrue(ComparisonEvidence.equal(resumed.saveWithFullMetadata(c.getLevel().registryAccess()),pending,"resumed.saveWithFullMetadata(c.getLevel().registryAccess()).equals(pending)"),"Normal same-time reconstruction preserves distributed receipts and canonical food");
            // Prospectively fixed horizon and sample schedule. An empty queue during return cooldown
            // is observed separately from a missing wake; no implementation deadline chooses our steps.
            long completedAt=last+400;
            for(long offset:new long[]{40,80,120,200,300,400}){LiveReferenceDriver.adults(c,q,last+offset);tickPile(c,resumed,last+offset);AllocationObservations.log(c,q,resumed,last+offset,"finite_fixed_horizon_"+offset);var existing=resumed.records().stream().filter(b->b.id().equals(id)).findFirst().orElse(null);if(existing!=null)larvalProteinCredit=existing.nutrition().gainedProtein();}
            var finalState=AllocationObservations.state(c,q,resumed,completedAt);
            var row=CatchupOutcomes.compare(match.live(),finalState);
            boolean exactItems=CatchupItemLedger.compare(match.live().getAsJsonObject("initial_item_ledger"),CatchupItemLedger.snapshot(c,q,resumed,cache),"catchup",row);
            row.add("live_item_ledger",match.live().get("live_item_ledger"));
            long units=cache.size()+adults(c,q).stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum()+resumed.consumedFood();
            boolean nutrition=adults(c,q).stream().allMatch(a->a.nutrition().snapshot().valid())&&resumed.records().stream().allMatch(child->child.nutrition().snapshot().valid());
            var saved=resumed.saveWithFullMetadata(c.getLevel().registryAccess());var replay=BroodGameTest.restorePile(c,resumed,saved);tickPile(c,replay,completedAt);tickPile(c,replay,completedAt-1);
            boolean replayExact=replay.saveWithFullMetadata(c.getLevel().registryAccess()).equals(saved)&&finalState.get("adults").equals(AllocationObservations.state(c,q,replay,completedAt).get("adults"))&&finalState.get("brood").equals(AllocationObservations.state(c,q,replay,completedAt).get("brood"));
            row.addProperty("configuration",captured==null?"generated":captured);row.addProperty("nurses_first",nursesFirst);row.addProperty("missing_steps",393);row.addProperty("ordinary_steps",7);row.addProperty("live_steps",400);
            row.addProperty("accounted_units",units);row.addProperty("nutrition_exact",nutrition);row.addProperty("reconstruction_same_and_older_replay_exact",replayExact);
            row.addProperty("existing_larva",id.toString());row.addProperty("existing_larva_emerged",resumed.consumed().contains(id));row.addProperty("larval_protein_credit",larvalProteinCredit);row.addProperty("larval_protein_required",CatchupOutcomes.LARVAL_PROTEIN);row.addProperty("consumption_tolerance_per_kind",CatchupOutcomes.CONSUMPTION_TOLERANCE);
            log(c,"t01_frozen_scarcity_outcome",replay,row);
            c.assertTrue(ComparisonEvidence.flag(exactItems,"exactItems")&&ComparisonEvidence.longs(units,5,"==","units==5")&&ComparisonEvidence.flag(nutrition,"nutrition")&&ComparisonEvidence.flag(replayExact,"replayExact")&&ComparisonEvidence.flag(row.get("live_crop_balanced").getAsBoolean(),"row.get(\"live_crop_balanced\").getAsBoolean()")&&ComparisonEvidence.flag(row.get("catchup_crop_balanced").getAsBoolean(),"row.get(\"catchup_crop_balanced\").getAsBoolean()"),"Exact conservation, nutrition, crop balance, reconstruction and same/older replay remain mandatory");
            c.assertTrue(ComparisonEvidence.flag(row.get("adult_classes_match").getAsBoolean(),"row.get(\"adult_classes_match\").getAsBoolean()"),"Frozen scarcity adult hunger and starvation classes match live");
            c.assertTrue(ComparisonEvidence.longs(larvalProteinCredit,CatchupOutcomes.LARVAL_PROTEIN,"==","larvalProteinCredit==CatchupOutcomes.LARVAL_PROTEIN"),"Existing larva receives live's 8000 protein from real food, including when it subsequently emerges");
            c.assertTrue(ComparisonEvidence.flag(row.get("consumption_within_one_per_kind").getAsBoolean(),"row.get(\"consumption_within_one_per_kind\").getAsBoolean()"),"Frozen physical consumption differs by at most one unit for every food kind, including zeros");
            c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void foodFedExistingBroodPromotesYoungAndLongReturnRegressesThroughStageRules(GameTestHelper c){
        fourUnit(c,false);
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void exactFourUnitLiveControlSettlesExistingEggEmergence(GameTestHelper c){fourUnit(c,true);}
    private void fourUnit(GameTestHelper c,boolean live){
        var f=new WorkerForagingGameTest();var q=f.start(c);boolean[] done={false};
        c.onEachTick(()->{
            if(done[0])return;var p=new NursingGameTest().pile(c,q);var plan=q.founding().plan();
            if(p==null||!p.operational()||!p.records().isEmpty()||f.workers(c,q).stream().filter(w->w.workerTasks().caregiver(c.getLevel(),plan)).count()<2)return;
            done[0]=true;
            c.assertTrue(ComparisonEvidence.flag(q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000),"q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)")&&ComparisonEvidence.flag(q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000),"q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000)"),"Two finite queen units fund the existing saved egg");
            c.assertTrue(ComparisonEvidence.flag(PaidLegacyBrood.restoreEggs(c,q,p,1),"PaidLegacyBrood.restoreEggs(c,q,p,1)"),"Real egg costs, slot and identity; current laying gate remains authoritative");
            var brood=p.records().getFirst().id();
            c.getLevel().setBlock(plan.cache(),NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var cache=(NestCache)c.getLevel().getBlockEntity(plan.cache());
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());
            out.putInt("InventoryFormat",3);out.putString("Colony",q.getUUID().toString());out.store("Entrance",net.minecraft.core.BlockPos.CODEC,plan.entrance());out.putString("Direction",plan.direction().getName());
            out.store("Contents",ItemStack.CODEC.listOf(),List.of(new ItemStack(Items.APPLE),new ItemStack(Items.CHICKEN)));
            cache.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),out.buildResult()));
            long last=p.lastSimulationGameTime().orElseThrow(),span=p.stageDuration()*3+1;
            c.assertTrue(ComparisonEvidence.longs(p.stageDuration(),120,"==","p.stageDuration()==120")&&ComparisonEvidence.longs(span,361,"==","span==361"),"Exact original duration120 and span361, no extra food");
            GrowthObservations.start(c,q,live?"four_unit_live":"four_unit_catchup");AllocationObservations.log(c,q,p,last+1,live?"four_live_baseline":"four_aggregate_baseline");
            if(live)for(long offset=1;offset<=span+1;offset++){long time=last+offset;LiveReferenceDriver.adults(c,q,time);tickPile(c,p,time);GrowthObservations.sample(c,q,time,"ordinary_after_bodies_and_pile");}
            else tickPile(c,p,last+span+1);
            GrowthObservations.sample(c,q,last+span+1,"final");GrowthObservations.stop(q);AllocationObservations.log(c,q,p,last+span+1,live?"four_live_final":"four_aggregate_final");
            var disposition=new JsonObject();disposition.addProperty("requested_span",span);disposition.addProperty("stage_duration",p.stageDuration());disposition.add("queen_nutrition",new Gson().toJsonTree(q.nutrition().snapshot()));disposition.add("records",new Gson().toJsonTree(p.records().stream().map(r->Map.of("stage",r.stage(),"progress",r.progress(),"nutrition",r.nutrition().snapshot())).toList()));disposition.add("cache_contents",new Gson().toJsonTree(cache.contents().stream().map(ItemStack::toString).toList()));disposition.add("service_intentions",new Gson().toJsonTree(p.catchup().serviceObservations()));disposition.addProperty("occupied_workers",ColonyMembers.get(c.getLevel()).occupied(q.getUUID()));log(c,"four_unit_existing_egg_before_promotion_verdict",p,disposition);
            // T08 exact ordinary control disproved the original fifth-adult assumption. Keep this
            // permanent identity and its original archived failure; never create another food unit.
            var dependent=p.records().stream().filter(r->r.id().equals(brood)).findFirst().orElseThrow();
            c.assertTrue(ComparisonEvidence.objects(c.getLevel().getEntity(ColonyMembers.workerId(brood)),null,"==","c.getLevel().getEntity(ColonyMembers.workerId(brood))==null")&&!ComparisonEvidence.flag(p.consumed().contains(brood),"p.consumed().contains(brood)")&&ComparisonEvidence.longs(ColonyMembers.get(c.getLevel()).occupied(q.getUUID()),3,"==","ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3")&&ComparisonEvidence.objects(dependent.stage(),BroodStage.LARVA,"==","dependent.stage()==BroodStage.LARVA")&&ComparisonEvidence.longs(dependent.progress(),0,"==","dependent.progress()==0"),"Exact live-proven four-unit counterexample: unpaid larval sugar prevents fifth adult");
            c.assertTrue(ComparisonEvidence.longs(cache.size(),0,"==","cache.size()==0")&&ComparisonEvidence.longs(p.consumedApples(),0,"==","p.consumedApples()==0")&&ComparisonEvidence.longs(p.consumedChickens(),1,"==","p.consumedChickens()==1")&&ComparisonEvidence.longs(q.nutrition().apples(),2,"==","q.nutrition().apples()==2")&&ComparisonEvidence.longs(q.nutrition().chickens(),1,"==","q.nutrition().chickens()==1")&&ComparisonEvidence.flag(q.nutrition().snapshot().valid(),"q.nutrition().snapshot().valid()")&&ComparisonEvidence.flag(dependent.nutrition().snapshot().valid(),"dependent.nutrition().snapshot().valid()")&&ComparisonEvidence.longs(dependent.nutrition().sugar(),0,"==","dependent.nutrition().sugar()==0")&&ComparisonEvidence.longs(dependent.nutrition().protein(),8000,"==","dependent.nutrition().protein()==8000"),"Exactly four original units: three queen receipts and one larval protein receipt; no income");
            if(live){c.succeed();return;}
            c.assertTrue(ComparisonEvidence.objects(p.stageEvaluation().stage(),ColonyStage.FOUNDING,"==","p.stageEvaluation().stage()==ColonyStage.FOUNDING"),"Unchanged StageRules cannot promote a four-adult colony");
            var row=new JsonObject();row.addProperty("counterexample_stage",p.stageEvaluation().stage().name());row.addProperty("counterexample_adults",4);row.addProperty("initial_physical_units",4);
            var restored=BroodGameTest.restorePile(c,p,p.saveWithFullMetadata(c.getLevel().registryAccess()));
            tickPile(c,restored,last+span+1+1_000_001);
            c.assertTrue(ComparisonEvidence.longs(restored.catchup().pendingTicks(),0,"==","restored.catchup().pendingTicks()==0")&&ComparisonEvidence.longs(ColonyMembers.get(c.getLevel()).occupied(q.getUUID()),0,"==","ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0")&&ComparisonEvidence.objects(restored.stageEvaluation().stage(),ColonyStage.FOUNDING,"==","restored.stageEvaluation().stage()==ColonyStage.FOUNDING"),"Long return causes real deaths and catastrophic regression through unchanged StageRules");
            // Save/reconstruct after an aggregate time jump also validates the ordinary income sampler's discontinuity.
            var saved=restored.saveWithFullMetadata(c.getLevel().registryAccess());var replay=BroodGameTest.restorePile(c,restored,saved);
            c.assertTrue(ComparisonEvidence.longs(replay.catchup().appliedTicks(),span+1_000_000,"==","replay.catchup().appliedTicks()==span+1_000_000")&&ComparisonEvidence.equal(replay.original(),p.original(),"replay.original().equals(p.original())")&&!ComparisonEvidence.flag(replay.consumed().contains(brood),"replay.consumed().contains(brood)"),"Applied interval and unpromoted dependent identity survive normal reconstruction");
            row.addProperty("regressed_stage",restored.stageEvaluation().stage().name());row.addProperty("remaining_workers",0);row.addProperty("laying_deferred",true);log(c,"stage_promotion_regression",restored,row);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:foraging_pair")
    public void pairedFiniteLiveControlAndReloadAgreeOnOriginalBroodGrowth(GameTestHelper c){
        var l=c.getLevel();var origin=dev.primeants.founding.NaturalSoil.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,dev.primeants.founding.NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for(int x=1;x<=30;x++)for(int z=1;z<=30;z++)for(int y=1;y<=4;y++){
            c.setBlock(x,y,z,net.minecraft.world.level.block.Blocks.DIRT);origin.addProperty(Long.toString(c.absolutePos(new net.minecraft.core.BlockPos(x,y,z)).asLong()),"minecraft:dirt");
        }
        l.getDataStorage().set(dev.primeants.founding.NaturalSoil.TYPE,dev.primeants.founding.NaturalSoil.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,origin).getOrThrow());
        var f=new WorkerForagingGameTest();var live=f.pairEgg(c,new net.minecraft.core.BlockPos(8,4,10));var away=f.pairEgg(c,new net.minecraft.core.BlockPos(20,4,10));boolean[] done={false};
        c.onEachTick(()->{
            if(done[0])return;var a=new NursingGameTest().pile(c,live);var b=new NursingGameTest().pile(c,away);if(a==null||b==null)return;
            c.assertTrue(ComparisonEvidence.flag(a.records().stream().allMatch(r->r.stage()==BroodStage.EGG),"a.records().stream().allMatch(r->r.stage()==BroodStage.EGG)")&&ComparisonEvidence.flag(b.records().stream().allMatch(r->r.stage()==BroodStage.EGG),"b.records().stream().allMatch(r->r.stage()==BroodStage.EGG)"),"Matched accepted initial first clutches before larval expenditure");done[0]=true;
            long last=l.getGameTime();
            // Normalize only initial egg progress/boundaries; preserve original brood/queen identities and paid reserves.
            var at=a.saveWithFullMetadata(l.registryAccess());var bt=b.saveWithFullMetadata(l.registryAccess());
            for(var tag:List.of(at,bt)){tag.putLong(ColonyClock.FIELD,last);for(var child:tag.getListOrEmpty("Brood"))((CompoundTag)child).putLong("Progress",0);}
            var control=BroodGameTest.restorePile(c,a,at);var returning=BroodGameTest.restorePile(c,b,bt);long span=control.stageDuration()*3+1;
            var original=returning.original();
            for(long i=1;i<=span+1;i++){
                long time=last+i;
                LiveReferenceDriver.adults(c,live,time);tickPile(c,control,time);
            }
            tickPile(c,returning,last+span+1);
            int liveWorkers=f.workers(c,live).size(),reloadWorkers=f.workers(c,away).size();
            c.assertTrue(ComparisonEvidence.longs(Math.abs(liveWorkers-reloadWorkers),1,"<=","Math.abs(liveWorkers-reloadWorkers)<=1")&&ComparisonEvidence.longs(Math.abs(control.records().size()-returning.records().size()),1,"<=","Math.abs(control.records().size()-returning.records().size())<=1"),"Frozen one-adult/one-slot growing tolerance");
            c.assertTrue(ComparisonEvidence.longs(liveWorkers,3,"==","liveWorkers==3")&&ComparisonEvidence.longs(reloadWorkers,3,"==","reloadWorkers==3")&&ComparisonEvidence.longs(live.bodyReserve(),0,"==","live.bodyReserve()==0")&&ComparisonEvidence.longs(away.bodyReserve(),0,"==","away.bodyReserve()==0")&&ComparisonEvidence.flag(returning.consumed().containsAll(original),"returning.consumed().containsAll(original)"),"Actual original workers and exact finite reserve conservation in both colonies");
            c.assertTrue(ComparisonEvidence.longs(control.consumedFood(),0,"==","control.consumedFood()==0")&&ComparisonEvidence.longs(returning.consumedFood(),0,"==","returning.consumedFood()==0")&&ComparisonEvidence.objects(f.cache(c,live),null,"==","f.cache(c,live)==null")&&ComparisonEvidence.objects(f.cache(c,away),null,"==","f.cache(c,away)==null"),"Identical zero cache kinds, no incoming resource income");
            var liveBirths=f.workers(c,live).stream().map(w->w.simulationEvidence().boundary()-last).sorted().toList();var returnBirths=f.workers(c,away).stream().map(w->w.simulationEvidence().boundary()-last).sorted().toList();long timingDifference=0;
            for(int i=0;i<3;i++)timingDifference=Math.max(timingDifference,Math.abs(liveBirths.get(i)-returnBirths.get(i)));
            c.assertTrue(ComparisonEvidence.doubles(timingDifference,control.stageDuration()*CatchupOutcomes.DEVELOPMENT_STAGE_FRACTION,"<=","timingDifference<=control.stageDuration()*CatchupOutcomes.DEVELOPMENT_STAGE_FRACTION"),"Measured real worker birth times meet the owner-approved 25-percent-stage timing tolerance");
            var row=new JsonObject();row.addProperty("live_workers",liveWorkers);row.addProperty("catchup_workers",reloadWorkers);row.addProperty("live_brood",control.records().size());row.addProperty("catchup_brood",returning.records().size());row.addProperty("adult_difference",reloadWorkers-liveWorkers);row.addProperty("slot_difference",returning.records().size()-control.records().size());row.add("live_birth_ticks",new Gson().toJsonTree(liveBirths));row.add("catchup_birth_ticks",new Gson().toJsonTree(returnBirths));row.addProperty("brood_timing_difference_ticks",timingDifference);row.addProperty("brood_timing_tolerance_ticks",control.stageDuration()*CatchupOutcomes.DEVELOPMENT_STAGE_FRACTION);row.addProperty("initial_reserve_each",BroodPile.MAX_RESERVE);row.addProperty("incoming_supplies",0);log(c,"growing_live_control",returning,row);
            // Continue the two finite controls through callow maturation, then compare nonzero physical food service.
            long foodStart=last+span+1+BroodPile.callowTicks()+2;
            for(long t=last+span+2;t<=foodStart;t++){
                long time=t;LiveReferenceDriver.adults(c,live,time);LiveReferenceDriver.adults(c,away,time);tickPile(c,control,time);tickPile(c,returning,time);
            }
            var ca=finiteCache(c,live,List.of(new ItemStack(Items.APPLE),new ItemStack(Items.SWEET_BERRIES),new ItemStack(Items.CHICKEN)));
            var cb=finiteCache(c,away,List.of(new ItemStack(Items.APPLE),new ItemStack(Items.SWEET_BERRIES),new ItemStack(Items.CHICKEN)));
            for(var colony:List.of(live,away)){
                var hungry=f.workers(c,colony).getFirst();var tag=ReconciliationGameTest.save(c,hungry);tag.getCompound("AdultLife").orElseThrow().putLong("Fasting",hungry.adultLife().grace()/2);replace(c,hungry,tag);
            }
            for(long i=1;i<=201;i++){long time=foodStart+i;LiveReferenceDriver.adults(c,live,time);tickPile(c,control,time);}
            tickPile(c,returning,foodStart+201);
            var food=new JsonObject();var kinds=new JsonArray();
            for(var kind:List.of(Items.APPLE,Items.SWEET_BERRIES,Items.CHICKEN)){
                long liveCount=ca.contents().stream().filter(s->s.is(kind)).count(),awayCount=cb.contents().stream().filter(s->s.is(kind)).count();
                c.assertTrue(ComparisonEvidence.longs(Math.abs(liveCount-awayCount),1,"<=","Math.abs(liveCount-awayCount)<=1"),"Frozen one-unit-per-kind cache tolerance: "+kind);
                var numbers=new JsonObject();numbers.addProperty("kind",kind.toString());numbers.addProperty("live_cache",liveCount);numbers.addProperty("catchup_cache",awayCount);numbers.addProperty("difference",awayCount-liveCount);kinds.add(numbers);
            }
            var diagnostic=new JsonObject();diagnostic.addProperty("live_unfiltered_units",ca.size()+adults(c,live).stream().mapToLong(body->body.nutrition().consumedUnits()+body.getMainHandItem().getCount()).sum());diagnostic.addProperty("catchup_unfiltered_units",cb.size()+adults(c,away).stream().mapToLong(body->body.nutrition().consumedUnits()+body.getMainHandItem().getCount()).sum());
            for(var entry:Map.of("live",live,"catchup",away).entrySet()){
                var bodies=adults(c,entry.getValue());var hands=new JsonArray();for(var body:bodies){var hand=new JsonObject();hand.addProperty("uuid",body.getUUID().toString());hand.addProperty("item",body.getMainHandItem().toString());hand.addProperty("recognized_food",WorkerTasks.food(body.getMainHandItem()));hands.add(hand);}
                diagnostic.add(entry.getKey()+"_hands",hands);diagnostic.addProperty(entry.getKey()+"_food_units",(entry.getKey().equals("live")?ca:cb).size()+bodies.stream().mapToLong(body->body.nutrition().consumedUnits()+(WorkerTasks.food(body.getMainHandItem())?body.getMainHandItem().getCount():0)).sum());
                diagnostic.addProperty(entry.getKey()+"_nutrition_valid",bodies.stream().allMatch(body->body.nutrition().snapshot().valid()));
            }
            log(c,"food_counter_diagnosis",returning,diagnostic);
            c.assertTrue(ComparisonEvidence.longs(foodUnits(c,live,ca),3,"==","foodUnits(c,live,ca)==3")&&ComparisonEvidence.longs(foodUnits(c,away,cb),3,"==","foodUnits(c,away,cb)==3")&&ComparisonEvidence.flag(adults(c,live).stream().allMatch(actor->actor.nutrition().snapshot().valid()),"adults(c,live).stream().allMatch(actor->actor.nutrition().snapshot().valid())")&&ComparisonEvidence.flag(adults(c,away).stream().allMatch(actor->actor.nutrition().snapshot().valid()),"adults(c,away).stream().allMatch(actor->actor.nutrition().snapshot().valid())"),"Exactly three finite units each, cache/cargo/actual ingestion and residual equations; no incoming supplies");
            food.add("kind_comparison",kinds);food.addProperty("initial_cache_each",3);food.addProperty("accounted_units_each",3);food.addProperty("incoming_supplies",0);log(c,"nonzero_food_live_control",returning,food);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void existingOriginalBroodEmergesAsRealWorkersFromAggregateSpans(GameTestHelper c){
        colony(c,false,(q,p)->{
            long last=p.lastSimulationGameTime().orElseThrow();var original=p.original();long stage=p.stageDuration();
            long span=stage*3+1;
            tickPile(c,p,last+span+1);
            c.assertTrue(ComparisonEvidence.longs(p.catchup().pendingTicks(),0,"==","p.catchup().pendingTicks()==0")&&ComparisonEvidence.longs(p.catchup().appliedTicks(),span,"==","p.catchup().appliedTicks()==span")&&ComparisonEvidence.flag(p.consumed().containsAll(original),"p.consumed().containsAll(original)"),"Full original clutch develops with its existing reserve costs");
            c.assertTrue(ComparisonEvidence.flag(original.stream().allMatch(id->c.getLevel().getEntity(ColonyMembers.workerId(id)) instanceof LasiusNigerEntity w&&id.equals(w.broodId())&&q.getUUID().equals(w.queenId())&&q.founding().plan().chamber().equals(w.nurseryHome())),"original.stream().allMatch(id->c.getLevel().getEntity(ColonyMembers.workerId(id)) instanceof LasiusNigerEntity w&&id.equals(w.broodId())&&q.getUUID().equals(w.queenId())&&q.founding().plan().chamber().equals(w.nurseryHome()))"),"Real deterministic original workers, history/membership and home; no numeric births");
            c.assertTrue(ComparisonEvidence.longs(q.bodyReserve(),0,"==","q.bodyReserve()==0")&&ComparisonEvidence.flag(p.records().isEmpty(),"p.records().isEmpty()")&&ComparisonEvidence.longs(ColonyMembers.get(c.getLevel()).occupied(q.getUUID()),3,"==","ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3"),"Exactly three births, exact reserve and occupied-slot conservation");
            var row=new JsonObject();row.addProperty("expected_workers",3);row.addProperty("actual_workers",3);row.addProperty("configured_stage_ticks",stage);row.addProperty("brood_timing_tolerance_ticks",stage*CatchupOutcomes.DEVELOPMENT_STAGE_FRACTION);row.addProperty("reserve",q.bodyReserve());log(c,"existing_brood",p,row);c.succeed();
        });
    }
}
