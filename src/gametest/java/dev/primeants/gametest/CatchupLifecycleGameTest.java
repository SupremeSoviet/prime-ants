package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.*;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.time.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;

/** Explicit accepted-history/timestamp controls, separate from the native eviction case. No test calls aggregate application. */
public final class CatchupLifecycleGameTest {
    private static void schedulingColony(GameTestHelper c,java.util.function.BiConsumer<LasiusNigerEntity,BroodPile> body){
        var f=new WorkerForagingGameTest();var q=f.start(c);boolean[] done={false};
        c.onEachTick(()->{if(done[0])return;var p=new NursingGameTest().pile(c,q);
            if(p==null||!p.operational()||!p.records().isEmpty()||f.workers(c,q).stream().filter(a->a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;
            done[0]=true;body.accept(q,p);});
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void legacyAggregateIntentionsAreDroppedWithoutReceipts(GameTestHelper c){
        schedulingColony(c,(q,p)->{
            var before=adults(c,q).stream().map(a->a.nutrition().snapshot()).toList();
            var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());var old=tag.getCompoundOrEmpty("Catchup");
            var list=new ListTag();list.add(StringTag.valueOf("{\"source\":\"crop\",\"claimed\":true}"));
            for(var key:List.of("Services","ServiceSearch","ServiceOrigins"))old.put(key,list.copy());
            for(var key:List.of("ServiceReady","ServiceCacheReturned","ServiceCropReady")){var times=new CompoundTag();times.putLong(q.getUUID().toString(),999);old.put(key,times);}
            var restored=BroodGameTest.restorePile(c,p,tag);var saved=restored.saveWithFullMetadata(c.getLevel().registryAccess()).getCompoundOrEmpty("Catchup");
            boolean omitted=List.of("Services","ServiceSearch","ServiceOrigins","ServiceReady","ServiceCacheReturned","ServiceCropReady").stream().noneMatch(saved::contains);
            var row=new JsonObject();row.addProperty("obsolete_fields_omitted",omitted);row.addProperty("nutrition_unchanged",before.equals(adults(c,q).stream().map(a->a.nutrition().snapshot()).toList()));row.addProperty("pending",restored.catchup().pendingServices());evidence("legacy_intention_migration",row);
            c.assertTrue(omitted&&row.get("nutrition_unchanged").getAsBoolean()&&restored.catchup().pendingServices()==0,"Legacy approaches, claims, origins, search and return/cooldown intentions grant no receipts or retained state");
            replay(c,q,restored,restored.lastSimulationGameTime().orElseThrow());c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void fixedDistributionFundsOnlyConfirmedMissingBiologyAndReplaysExactly(GameTestHelper c){
        schedulingColony(c,(q,p)->{
            c.assertTrue(q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)&&q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000),"Real initial queen funding");
            existingLarva(c,q,p,1200,false);p=pile(c,q);var larva=p.records().getFirst();
            c.assertTrue(larva.nutrition().ingest(new ItemStack(Items.CHICKEN),4000,8000),"Real initial larval protein");
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,List.of(new ItemStack(Items.APPLE)));long last=p.lastSimulationGameTime().orElseThrow();
            AutomaticCatchupGameTest.tickPile(c,p,last+31);var result=p.records().getFirst();
            var row=new JsonObject();row.addProperty("missing",30);row.addProperty("ordinary",1);row.addProperty("progress",result.progress());row.addProperty("larval_apples",result.nutrition().apples());row.addProperty("cache",cache.size());evidence("fixed_food_boundary",row);
            replay(c,q,p,last+31);
            c.assertTrue(result.progress()==31&&cache.size()==0&&result.nutrition().apples()==1&&result.nutrition().snapshot().valid()&&p.catchup().pendingServices()==0,"Fixed distribution at first missing boundary funds thirty missing plus one ordinary nursery step, with exact real ingestion");c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void aggregateDistributionPreservesOrdinaryCropControllerState(GameTestHelper c){
        new AutomaticCatchupGameTest().colony(c,true,(q,p)->{
            var workers=new WorkerForagingGameTest().workers(c,q);
            for(var a:workers){var tag=ReconciliationGameTest.save(c,a);tag.getCompoundOrEmpty("WorkerTask").getCompoundOrEmpty("CropSharing").putInt("Cooldown",40);AutomaticCatchupGameTest.replace(c,a,tag);}
            var before=adults(c,q).stream().map(a->ReconciliationGameTest.save(c,a).getCompoundOrEmpty("WorkerTask").getCompoundOrEmpty("CropSharing").copy()).toList();
            long last=p.lastSimulationGameTime().orElseThrow();AutomaticCatchupGameTest.tickPile(c,p,last+53);replay(c,q,p,last+53);
            var after=adults(c,q).stream().map(a->ReconciliationGameTest.save(c,a).getCompoundOrEmpty("WorkerTask").getCompoundOrEmpty("CropSharing").copy()).toList();
            var row=new JsonObject();row.addProperty("controller_state_exact",before.equals(after));evidence("ordinary_crop_state_preserved",row);
            c.assertTrue(before.equals(after),"Aggregate distribution never advances ordinary cooldown, search, action, target or observations");c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void fixedDistributionUsesHungerThenStableIdentityAndLarvalProteinPriority(GameTestHelper c){
        schedulingColony(c,(q,p)->{
            c.assertTrue(q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)&&q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000),"Real queen funding");existingLarva(c,q,p,1200,false);p=pile(c,q);
            var workers=adults(c,q).stream().filter(a->a.form()==AntForm.WORKER).sorted(Comparator.comparing(a->a.getUUID().toString())).toList();
            for(int i=0;i<workers.size();i++){var a=workers.get(i);c.assertTrue(a.nutrition().spend(a.nutrition().sugar(),0),"Declared spending before boundary");var tag=ReconciliationGameTest.save(c,a);tag.getCompoundOrEmpty("AdultLife").putLong("Fasting",a.adultLife().grace()/2+(i==2?100:0));a=AutomaticCatchupGameTest.replace(c,a,tag);a.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(q.founding().plan().at(4,0,-2)));a.setOnGround(true);}
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,List.of(new ItemStack(Items.APPLE),new ItemStack(Items.SWEET_BERRIES),new ItemStack(Items.CHICKEN)));long last=p.lastSimulationGameTime().orElseThrow();
            AutomaticCatchupGameTest.tickPile(c,p,last+2);
            var highest=(LasiusNigerEntity)c.getLevel().getEntity(workers.get(2).getUUID());var tieFirst=(LasiusNigerEntity)c.getLevel().getEntity(workers.get(0).getUUID());
            var row=new JsonObject();row.addProperty("hungriest_apples",highest.nutrition().apples());row.addProperty("stable_tie_berries",tieFirst.nutrition().berries());row.addProperty("larval_protein",p.records().getFirst().nutrition().gainedProtein());row.addProperty("cache",cache.size());evidence("fixed_distribution_priorities",row);
            c.assertTrue(highest.nutrition().apples()==1&&tieFirst.nutrition().berries()==1&&p.records().getFirst().nutrition().gainedProtein()==8000,"Hungriest adult first, stable UUID ties, and real protein to accepting larva before queen");replay(c,q,p,last+2);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void fixedDistributionDefersUnknownForeignCacheAndUnreachableCargo(GameTestHelper c){
        schedulingColony(c,(q,p)->{
            c.assertTrue(q.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)&&q.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000),"Real queen funding");existingLarva(c,q,p,1200,false);p=pile(c,q);
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,List.of(new ItemStack(Items.CHICKEN)));var cacheTag=cache.saveWithFullMetadata(c.getLevel().registryAccess());
            var carrier=adults(c,q).stream().filter(a->a.form()==AntForm.WORKER).findFirst().orElseThrow();carrier.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.CHICKEN));carrier.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(q.founding().plan().at(-5,4,1)));
            long last=p.lastSimulationGameTime().orElseThrow();
            try(var hidden=UnavailableCells.hide(c,q.founding().plan().at(2,-2,-3))){AutomaticCatchupGameTest.tickPile(c,p,last+2);c.assertTrue(p.catchup().pendingTicks()==1,"UNKNOWN defers missing work");}
            cacheTag.putString("Colony",UUID.randomUUID().toString());SurfaceFixture.loadBlock(c,cache,cacheTag);
            AutomaticCatchupGameTest.tickPile(c,p,last+5);
            var foreign=(NestCache)c.getLevel().getBlockEntity(q.founding().plan().cache());
            var row=new JsonObject();row.addProperty("foreign_cache",foreign.size());row.addProperty("unreachable_cargo",carrier.getMainHandItem().getCount());row.addProperty("larval_receipts",p.consumedFood());evidence("source_ownership_and_reachability",row);
            c.assertTrue(foreign.size()==1&&carrier.getMainHandItem().is(Items.CHICKEN)&&p.consumedFood()==0,"Foreign stock, unreachable actual cargo and unknown cells never become aggregate receipts");replay(c,q,p,last+5);c.succeed();
        });
    }
    private static BroodPile pile(GameTestHelper c,LasiusNigerEntity q){return new NursingGameTest().pile(c,q);}
    private static List<LasiusNigerEntity> adults(GameTestHelper c,LasiusNigerEntity q){return AutomaticCatchupGameTest.adults(c,q);}
    private static void tick(GameTestHelper c,LasiusNigerEntity q,long now){
        LiveReferenceDriver.adults(c,q,now);
        AutomaticCatchupGameTest.tickPile(c,pile(c,q),now);
    }
    private static List<ItemStack> food(int each){var units=new ArrayList<ItemStack>();for(int n=0;n<each;n++){units.add(new ItemStack(Items.APPLE));units.add(new ItemStack(Items.CHICKEN));}return units;}
    private static void evidence(String scenario,JsonObject row){row.addProperty("scenario",scenario);row.addProperty("fixture","accepted_history_timestamp_reconstruction");ReconciliationEvidence.log("t06_lifecycle","ordinary_production_hooks",row);}
    private static void replay(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long now){
        var saved=p.saveWithFullMetadata(c.getLevel().registryAccess());var biology=adults(c,q).stream().map(LasiusNigerEntity::biologySnapshot).toList();
        var registry=ChamberRegistry.get(c.getLevel()).colony(q.getUUID());
        var restored=BroodGameTest.restorePile(c,p,saved);AutomaticCatchupGameTest.tickPile(c,restored,now);
        c.assertTrue(restored.saveWithFullMetadata(c.getLevel().registryAccess()).equals(saved)&&biology.equals(adults(c,q).stream().map(LasiusNigerEntity::biologySnapshot).toList())&&registry.equals(ChamberRegistry.get(c.getLevel()).colony(q.getUUID())),"Same-C reconstruction preserves exact brood/adult/clock receipts and identities");
        AutomaticCatchupGameTest.tickPile(c,restored,now-1);
        var replayBefore=new JsonObject();replayBefore.addProperty("pile_nbt",saved.toString());var replayAfter=new JsonObject();replayAfter.addProperty("pile_nbt",restored.saveWithFullMetadata(c.getLevel().registryAccess()).toString());PersistenceDiagnostics.observe(c,"lifecycle_older_C",replayBefore,replayAfter);
        c.assertTrue(restored.saveWithFullMetadata(c.getLevel().registryAccess()).equals(saved)&&biology.equals(adults(c,q).stream().map(LasiusNigerEntity::biologySnapshot).toList()),"Older-C reconstruction cannot replay any completed investment or pending service");
    }
    private static void fundedLarva(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long duration){
        existingLarva(c,q,p,duration,true);
    }
    private static void existingLarva(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long duration,boolean fed){
        c.assertTrue(q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN),"Existing dependent's real egg investment");
        var n=new Nutrition();if(fed)c.assertTrue(n.ingest(new ItemStack(Items.APPLE),Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN)&&n.ingest(new ItemStack(Items.CHICKEN),Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN),"Two declared physical baseline larval receipts");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());n.save(out);
        var r=new CompoundTag();r.putString("Id",UUID.randomUUID().toString());r.putString("Queen",q.getUUID().toString());r.putInt("Slot",0);r.putString("Stage","LARVA");r.putLong("Progress",0);r.putLong("Nourishment",0);r.putBoolean("Founding",false);r.put("Nutrition",out.buildResult());
        var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());var list=new ListTag();list.add(r);tag.put("Brood",list);tag.putLong("StageDuration",duration);tag.putLong("LastLayingTick",2400);tag.putLong("LoadedTicks",2400);
        tag.putLong(ColonyClock.FIELD,c.getLevel().getGameTime());tag.putLong("LastOrdinaryGameTime",c.getLevel().getGameTime());
        // A declared accepted rolling-receipt history, entirely before B. Its endpoint is backed by real
        // ingested baseline units. No sample, rate, receipt or supply is manufactured during absence.
        var flow=new FoodLimitedGrowth();var actual=p.supply(c.getLevel());
        for(long t=0;t<=2400;t+=200)flow.observe(t,new FoodLimitedGrowth.Supply(true,false,actual.foodSugar()*t/2400,actual.foodProtein()*t/2400,actual.storedSugar(),actual.storedProtein(),0,0,actual.adults(),1));
        var history=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());flow.save(history);tag.put("GrowthFlow",history.buildResult());
        BroodGameTest.restorePile(c,p,tag);
    }
    private static JsonObject state(GameTestHelper c,LasiusNigerEntity q){
        var p=pile(c,q);var row=new JsonObject();row.addProperty("adults",adults(c,q).size());row.addProperty("brood",p.records().size());row.addProperty("births",p.consumed().size()-p.original().size());
        row.addProperty("last_laying",p.lastLayingTick());row.add("records",new Gson().toJsonTree(p.records().stream().map(r->r.id()+":"+r.stage()+":"+r.progress()).toList()));
        row.add("nutrition",new Gson().toJsonTree(adults(c,q).stream().map(a->a.nutrition().snapshot()).toList()));row.add("stage",new Gson().toJsonTree(p.stageEvaluation()));return row;
    }
    private static long accounted(GameTestHelper c,LasiusNigerEntity q,NestCache cache){
        return cache.size()+pile(c,q).consumedFood()+adults(c,q).stream().mapToLong(a->a.nutrition().consumedUnits()+(WorkerTasks.food(a.getMainHandItem())?a.getMainHandItem().getCount():0)).sum();
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void competingFiniteResourcesLayingAndVirtualBirthsMatchLiveControl(GameTestHelper c){
        competition(c,true);
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void finiteResourcesWithoutInitialAdultDemandMatchFrozenLiveControl(GameTestHelper c){
        competition(c,false);
    }
    private void competition(GameTestHelper c,boolean adultDemand){
        var live=new TierThreeFixture().start(c,32,false,4,new BlockPos(20,4,26),Direction.EAST,true);
        var away=new TierThreeFixture().start(c,32,false,4,new BlockPos(38,4,26),Direction.EAST,false);boolean[] started={false};
        for(var q:List.of(live,away))AutomaticCatchupGameTest.finiteCache(c,q,List.of());
        c.onEachTick(()->{
            if(started[0])return;
            started[0]=true;long initial=c.getLevel().getGameTime();
            // Identical ordinary pre-boundary maturation, with no cache meals or random receipt imbalance.
            for(long i=1;i<=80;i++){tick(c,live,initial+i);tick(c,away,initial+i);}
            long last=initial+80;long duration=1200,span=1850;
            c.assertTrue(List.of(live,away).stream().allMatch(q->adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()>=2),"Both identical pre-boundary controls retain ordinary authorized nurses");
            ReconciliationGameTest.atTime(c,last,()->{
            for(var q:List.of(live,away)){
                fundedLarva(c,q,pile(c,q),duration);
                var cache=AutomaticCatchupGameTest.finiteCache(c,q,food(4));c.assertTrue(cache.capacity()>=8&&cache.size()==8,"Four actual apple and four chicken units in confirmed valid capacity");
                var target=adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&!a.workerTasks().nursing()).findFirst().orElseThrow();
                if(adultDemand){c.assertTrue(target.nutrition().spend(target.nutrition().sugar(),0),"Declared adult's consumed initial apple is spent before B");
                var tag=ReconciliationGameTest.save(c,target);var life=tag.getCompound("AdultLife").orElseThrow();life.putLong("Fasting",target.adultLife().grace()/2);life.putInt("CoverageRemaining",0);life.putBoolean("Started",true);
                AutomaticCatchupGameTest.replace(c,target,tag);}
            }
            });
            var ca=(NestCache)c.getLevel().getBlockEntity(live.founding().plan().cache());var cb=(NestCache)c.getLevel().getBlockEntity(away.founding().plan().cache());
            long totalA=accounted(c,live,ca),totalB=accounted(c,away,cb);var material=(MaterialStore)c.getLevel().getBlockEntity(ChamberExcavation.build(away.founding().plan(),"right").marker());var materialBefore=material.saveWithFullMetadata(c.getLevel().registryAccess());
            c.assertTrue(totalA==totalB&&ca.size()==cb.size(),"Matched finite baseline has identical real unit totals and stock counts");
            var oldMaterial=material;SurfaceFixture.loadBlock(c,material,materialBefore);
            material=(MaterialStore)c.getLevel().getBlockEntity(oldMaterial.getBlockPos());
            c.assertTrue(material!=oldMaterial&&material.getBlockPos().equals(oldMaterial.getBlockPos())&&material.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore)&&material.contents().stream().filter(s->s.is(Items.STONE)).count()==32,"Different normally deserialized material store at the original marker retains all 32 stone units, ownership, release identity and saved accounting");
            var materialProof=new JsonObject();materialProof.addProperty("phase","before_automatic_catchup");materialProof.addProperty("different_object",true);materialProof.addProperty("marker",material.getBlockPos().toString());materialProof.addProperty("saved_tag",materialBefore.toString());materialProof.add("contents",new Gson().toJsonTree(TierThreeEffectsProbe.contents(material.contents())));evidence("material_deserialization",materialProof);
            var initialAdults=adults(c,away).stream().map(LasiusNigerEntity::getUUID).collect(java.util.stream.Collectors.toSet());
            var liveCadence=new JsonObject();liveCadence.add("initial",LiveReferenceDriver.counters(c,live));long liveInvocations=pile(c,live).catchup().invocations(),liveApplied=pile(c,live).catchup().appliedTicks();
            GrowthObservations.start(c,live,(adultDemand?"original":"no_initial_adult_demand")+"_live");GrowthObservations.start(c,away,(adultDemand?"original":"no_initial_adult_demand")+"_catchup");
            for(long i=1;i<=span+1;i++){tick(c,live,last+i);GrowthObservations.sample(c,live,last+i,"ordinary_after_bodies_and_pile");}
            liveCadence.add("final",LiveReferenceDriver.counters(c,live));liveCadence.addProperty("steps",span+1);liveCadence.addProperty("aggregate_invocations",pile(c,live).catchup().invocations()-liveInvocations);liveCadence.addProperty("aggregate_applied",pile(c,live).catchup().appliedTicks()-liveApplied);evidence((adultDemand?"original":"no_initial_adult_demand")+"_reference_cadence",liveCadence);
            c.assertTrue(pile(c,live).catchup().invocations()==liveInvocations&&pile(c,live).catchup().appliedTicks()==liveApplied,"Frozen growth reference earns no aggregate application");
            AutomaticCatchupGameTest.tickPile(c,pile(c,away),last+span+1);
            GrowthObservations.sample(c,live,last+span+1,"final");GrowthObservations.sample(c,away,last+span+1,"final");
            var livePhysiology=GrowthObservations.snapshot(live);var awayPhysiology=GrowthObservations.snapshot(away);GrowthObservations.stop(live);GrowthObservations.stop(away);
            var a=pile(c,live);var b=pile(c,away);var row=new JsonObject();row.add("live",state(c,live));row.add("catchup",state(c,away));row.addProperty("span",span);row.addProperty("stage_duration",duration);row.addProperty("accounted_live",accounted(c,live,ca));row.addProperty("accounted_catchup",accounted(c,away,cb));row.addProperty("initial_accounted_live",totalA);row.addProperty("initial_accounted_catchup",totalB);
            var stocks=new JsonObject();for(var kind:List.of(Items.APPLE,Items.CHICKEN)){long x=ca.contents().stream().filter(s->s.is(kind)).count(),y=cb.contents().stream().filter(s->s.is(kind)).count();stocks.addProperty(kind.toString()+"_live",x);stocks.addProperty(kind.toString()+"_catchup",y);}row.add("stocks",stocks);evidence("competition",row);

            var physiology=new JsonObject();physiology.addProperty("case",adultDemand?"original":"no_initial_adult_demand");physiology.add("live",livePhysiology);physiology.add("catchup",awayPhysiology);var differences=new JsonArray();boolean physiologicalMatch=true;
            for(int i=0;i<livePhysiology.getAsJsonArray("original_adults").size();i++){
                var x=livePhysiology.getAsJsonArray("original_adults").get(i).getAsJsonObject();var y=awayPhysiology.getAsJsonArray("original_adults").get(i).getAsJsonObject();var n=x.getAsJsonObject("nutrition");var m=y.getAsJsonObject("nutrition");var delta=new JsonObject();delta.add("matched",x.get("matched"));long ds=m.get("sugar").getAsLong()-n.get("sugar").getAsLong(),dp=m.get("protein").getAsLong()-n.get("protein").getAsLong();delta.addProperty("catchup_minus_live_sugar",ds);delta.addProperty("catchup_minus_live_protein",dp);boolean classes=x.get("hungry").equals(y.get("hungry"))&&x.get("starving").equals(y.get("starving"));delta.addProperty("hunger_starvation_match",classes);differences.add(delta);physiologicalMatch&=Math.abs(ds)<=Nutrition.APPLE_SUGAR&&Math.abs(dp)<=Nutrition.CHICKEN_PROTEIN&&classes;
            }
            physiology.add("recipient_differences",differences);physiology.addProperty("within_one_unit_and_classes",physiologicalMatch);physiology.addProperty("queen_affordability_match",livePhysiology.get("queen_next_egg_affordable").equals(awayPhysiology.get("queen_next_egg_affordable")));evidence("cross_run_physiology",physiology);
            c.assertTrue(accounted(c,live,ca)==totalA&&accounted(c,away,cb)==totalB&&adults(c,live).stream().allMatch(a1->a1.nutrition().snapshot().valid())&&adults(c,away).stream().allMatch(a1->a1.nutrition().snapshot().valid())&&a.records().stream().allMatch(r->r.nutrition().snapshot().valid())&&b.records().stream().allMatch(r->r.nutrition().snapshot().valid()),"Exact within-run food/cargo/ingested conservation and nutrition equations");
            c.assertTrue(material.size()==32&&material.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore),"Nonzero material units and normal-save bytes remain exact");
            var liveProgress=a.records().stream().mapToLong(r->(r.stage().ordinal()-BroodStage.EGG.ordinal())*duration+r.progress()).sorted().toArray();
            var awayProgress=b.records().stream().mapToLong(r->(r.stage().ordinal()-BroodStage.EGG.ordinal())*duration+r.progress()).sorted().toArray();
            var born=adults(c,away).stream().filter(w->!initialAdults.contains(w.getUUID())).toList();
            replay(c,away,b,last+span+1);c.assertTrue(material.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore),"Material save/replay exact");
            var marker=material.getBlockPos();var entrance=away.founding().plan().entrance();var previousEntrance=c.getLevel().getBlockState(entrance);
            c.getLevel().setBlock(entrance,net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),3);
            c.assertTrue(material.capacity()==MaterialStore.CAPACITY&&material.size()==32&&material.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore),"Capacity loss retains all canonical stone units and saved release/accounting fields without truncation");
            SurfaceFixture.loadBlock(c,material,materialBefore);var reduced=(MaterialStore)c.getLevel().getBlockEntity(marker);
            c.assertTrue(reduced!=material&&reduced.capacity()==MaterialStore.CAPACITY&&reduced.contents().stream().filter(s->s.is(Items.STONE)).count()==32&&reduced.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore),"Another normal material deserialization under reduced capacity retains all 32 units");
            var reducedProof=new JsonObject();reducedProof.addProperty("capacity",reduced.capacity());reducedProof.addProperty("units",reduced.size());reducedProof.addProperty("different_object",true);reducedProof.addProperty("saved_fields_exact",true);reducedProof.addProperty("automatic_and_nursery_replay_exact",true);reducedProof.add("contents",new Gson().toJsonTree(TierThreeEffectsProbe.contents(reduced.contents())));evidence("material_reconstruction_and_capacity_loss",reducedProof);c.getLevel().setBlock(entrance,previousEntrance,3);
            var exact=new JsonObject();exact.addProperty("material_units",material.size());exact.addProperty("material_save_replay_exact",true);exact.addProperty("food_each",totalA);exact.addProperty("food_conservation_exact",true);exact.add("virtual_adult_births",new Gson().toJsonTree(born.stream().map(w->w.getUUID()+":"+w.simulationEvidence().boundary()+":"+w.elapsedAgeTicks()).toList()));exact.add("live_development",new Gson().toJsonTree(liveProgress));exact.add("catchup_development",new Gson().toJsonTree(awayProgress));evidence("competition_exact_before_timing_verdict",exact);
            var gate=new JsonObject();gate.addProperty("case",adultDemand?"original":"no_initial_adult_demand");gate.add("live_development",new Gson().toJsonTree(liveProgress));gate.add("catchup_development",new Gson().toJsonTree(awayProgress));gate.addProperty("development_limit",CatchupOutcomes.DEVELOPMENT_TOLERANCE);gate.addProperty("food_conservation_exact",accounted(c,live,ca)==totalA&&accounted(c,away,cb)==totalB);gate.addProperty("nutrition_exact",adults(c,live).stream().allMatch(a1->a1.nutrition().snapshot().valid())&&adults(c,away).stream().allMatch(a1->a1.nutrition().snapshot().valid())&&a.records().stream().allMatch(r->r.nutrition().snapshot().valid())&&b.records().stream().allMatch(r->r.nutrition().snapshot().valid()));gate.addProperty("reconstruction_same_and_older_replay_exact",true);evidence("t01_frozen_growth_outcome",gate);
            c.assertTrue(a.consumed().size()>3&&b.consumed().size()>3&&a.lastLayingTick()>2400&&b.lastLayingTick()>2400,"Both controls produce real dependent births and new costed eggs");
            c.assertTrue(Math.abs(adults(c,live).size()-adults(c,away).size())<=1&&Math.abs(a.records().size()-b.records().size())<=1,"Frozen one adult and one occupied brood slot tolerances");
            for(var kind:List.of(Items.APPLE,Items.CHICKEN))c.assertTrue(Math.abs(ca.contents().stream().filter(s->s.is(kind)).count()-cb.contents().stream().filter(s->s.is(kind)).count())<=1,"Frozen one cached unit per represented kind");
            c.assertTrue(!born.isEmpty()&&born.stream().allMatch(w->w.elapsedAgeTicks()>0&&w.simulationEvidence().boundary()<last+span),"Real aggregate births use virtual timestamps and continue aging through the remaining interval");
            c.assertTrue(physiologicalMatch&&livePhysiology.get("queen_next_egg_affordable").equals(awayPhysiology.get("queen_next_egg_affordable")),"Original recipients match hunger/starvation and at most one represented unit of residual nutrition; queen next egg affordability agrees");
            c.assertTrue(liveProgress.length==awayProgress.length,"This matched finite control must retain corresponding occupied brood");
            for(int i=0;i<liveProgress.length;i++)c.assertTrue(Math.abs(liveProgress[i]-awayProgress[i])<=CatchupOutcomes.DEVELOPMENT_TOLERANCE,"Frozen 120-development-step tolerance, including stage transitions: live="+Arrays.toString(liveProgress)+" catchup="+Arrays.toString(awayProgress));c.succeed();
        });
    }
    private static void saveRegistry(GameTestHelper c){
        var registry=ChamberRegistry.get(c.getLevel());var encoded=ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,registry).getOrThrow();
        c.getLevel().getDataStorage().set(ChamberRegistry.TYPE,ChamberRegistry.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
    }
    private static void stageFixture(GameTestHelper c,LasiusNigerEntity q,ColonyStage stage,Map<String,Long> clocks){
        var json=ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,ChamberRegistry.get(c.getLevel())).getOrThrow().getAsJsonArray();
        for(var row:json)if(row.getAsJsonObject().get("queen").getAsString().equals(q.getUUID().toString())){
            row.getAsJsonObject().addProperty("stage",stage.serializedName());row.getAsJsonObject().add("unmet_since",new Gson().toJsonTree(clocks));
        }
        c.getLevel().getDataStorage().set(ChamberRegistry.TYPE,ChamberRegistry.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void stageGraceDeadlineAndClockReloadMatchLiveAndUnknownInterruption(GameTestHelper c){
        stageClockComparison(c,false);
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void unknownRestorationWithPhysicalNurseCargoKeepsFoodClockAtObservationEnd(GameTestHelper c){stageClockComparison(c,true);}
    private void stageClockComparison(GameTestHelper c,boolean cargoReproducer){
        var live=new TierThreeFixture().start(c,32,false,24,new BlockPos(20,4,26),Direction.EAST,true);
        var away=new TierThreeFixture().start(c,32,false,24,new BlockPos(38,4,26),Direction.EAST,false);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||pile(c,live).stageEvaluation()==null||pile(c,away).stageEvaluation()==null
                ||pile(c,live).stageEvaluation().stage()!=ColonyStage.MATURE||pile(c,away).stageEvaluation().stage()!=ColonyStage.MATURE)return;started[0]=true;
            long last=c.getLevel().getGameTime();
            for(var q:List.of(live,away)){
                var p=pile(c,q);var cache=AutomaticCatchupGameTest.finiteCache(c,q,List.of());
                c.assertTrue(cache.size()==0&&p.stageEvaluation().stage()==ColonyStage.MATURE,"Controlled confirmed Mature with newly observed food shortfall");
                var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.putLong("LoadedTicks",StageRules.GRACE-150);tag.putLong("LastLayingTick",StageRules.GRACE-150);tag.putInt("AdultCapacityBound",25);BroodGameTest.restorePile(c,p,tag);
                stageFixture(c,q,ColonyStage.MATURE,Map.of("mature:food",0L));
            }
            saveRegistry(c);for(var q:List.of(live,away))BroodGameTest.restorePile(c,pile(c,q),pile(c,q).saveWithFullMetadata(c.getLevel().registryAccess()));
            for(int i=1;i<=100;i++)tick(c,live,last+i);AutomaticCatchupGameTest.tickPile(c,pile(c,away),last+100);
            c.assertTrue(pile(c,live).stageEvaluation().stage()==ColonyStage.MATURE&&pile(c,away).stageEvaluation().stage()==ColonyStage.MATURE,"Both controls hold before the unchanged 24000-tick grace deadline");
            c.assertTrue(ChamberRegistry.get(c.getLevel()).colony(live.getUUID()).unmetSince().get("mature:food")==0&&ChamberRegistry.get(c.getLevel()).colony(away.getUUID()).unmetSince().get("mature:food")==0,"Saved shortfall clock remains exact before deadline");
            for(int i=101;i<=260;i++)tick(c,live,last+i);AutomaticCatchupGameTest.tickPile(c,pile(c,away),last+260);
            c.assertTrue(pile(c,live).stageEvaluation().stage()==ColonyStage.YOUNG&&pile(c,away).stageEvaluation().stage()==ColonyStage.YOUNG&&Math.abs(pile(c,live).stageEvaluatedAt()-pile(c,away).stageEvaluatedAt())<=ColonyDevelopment.INTERVAL,"Same regression stage and deadline within unchanged 100-tick evaluation interval");
            var row=new JsonObject();row.addProperty("live_evaluated_at",pile(c,live).stageEvaluatedAt());row.addProperty("catchup_evaluated_at",pile(c,away).stageEvaluatedAt());row.addProperty("deadline",StageRules.GRACE);row.addProperty("stage",pile(c,away).stageEvaluation().stage().name());
            for(var q:List.of(live,away)){AutomaticCatchupGameTest.finiteCache(c,q,food(4));tick(c,q,last+360);c.assertTrue(pile(c,q).stageEvaluation().stage()==ColonyStage.MATURE&&ChamberRegistry.get(c.getLevel()).colony(q.getUUID()).unmetSince().isEmpty(),"Restoration promotes and clears deficit clocks through unchanged rules");}
            if(cargoReproducer){
                var cache=(NestCache)c.getLevel().getBlockEntity(away.founding().plan().cache());int withdrawals=0;
                AllocationObservations.log(c,away,pile(c,away),last+360,"clock_cargo_preparation_inputs");
                long cap=Nutrition.QUEEN_SUGAR_CAPACITY-Nutrition.APPLE_SUGAR;
                if(away.nutrition().sugar()>cap)c.assertTrue(away.nutrition().spend(away.nutrition().sugar()-cap,0),"Reproducer declares real spent queen receipts to retain the apple eligibility witnessed in T08; no new supplies");
                for(var a:adults(c,away))if(a.form()==AntForm.WORKER&&a.workerTasks().nursing()&&a.getMainHandItem().isEmpty()){
                    // Explicit accepted partial-service input: the original physical unit is withdrawn
                    // by the ordinary cache hook, then its saved recipient/action state is reconstructed.
                    a.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(away.founding().plan().at(4,-1,-2)));
                    if(!cache.withdraw(a,away.founding().plan()))continue;
                    var tag=ReconciliationGameTest.save(c,a);var task=tag.getCompoundOrEmpty("WorkerTask");task.putString("Phase","NURSE_FEED");task.putString("Recipient",away.getUUID().toString());task.putInt("FeedingTicks",0);
                    AutomaticCatchupGameTest.replace(c,a,tag);withdrawals++;break;
                }
                AllocationObservations.log(c,away,pile(c,away),last+360,"clock_cargo_prepared");
                c.assertTrue(withdrawals>0||adults(c,away).stream().anyMatch(a->WorkerTasks.food(a.getMainHandItem())),"Small reproducer retains actual ordinary withdrawals from the original four apple/chicken pairs, no new supply");
            }
            var p=pile(c,away);AutomaticCatchupGameTest.finiteCache(c,away,List.of());stageFixture(c,away,ColonyStage.MATURE,Map.of("mature:food",p.loadedTicks()-100));
            StageClockObservations.start(away.getUUID());StageClockObservations.sample(c.getLevel(),p,last+360,"immediately_before_unknown");
            try(var hidden=UnavailableCells.hide(c,away.founding().plan().at(2,-2,-3))){AutomaticCatchupGameTest.tickPile(c,p,last+411);StageClockObservations.sample(c.getLevel(),p,last+411,"during_deferral_before_assertion");c.assertTrue(p.catchup().pendingTicks()==50&&p.catchup().unknownTicks()==0&&ChamberRegistry.get(c.getLevel()).colony(away.getUUID()).unmetSince().isEmpty(),"UNKNOWN breaks certainty without migration rebasing or resource mutation");}
            AutomaticCatchupGameTest.tickPile(c,p,last+412);StageClockObservations.sample(c.getLevel(),p,last+412,"restored_before_assertion");long since=ChamberRegistry.get(c.getLevel()).colony(away.getUUID()).unmetSince().get("mature:food");
            c.assertTrue(p.catchup().pendingTicks()==0&&since>=p.loadedTicks()-1,"Restored certainty starts at observation end, never backdates previously unknown deficit");row.addProperty("unknown_restored_since",since);row.addProperty("restored_loaded",p.loadedTicks());evidence("stage_hysteresis",row);replay(c,away,p,last+412);StageClockObservations.stop(away.getUUID());c.succeed();
        });
    }
    private void profile(GameTestHelper c,int workers){
        var q=new TierThreeFixture().start(c,32,false,workers,new BlockPos(32,4,32),Direction.EAST,true);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||pile(c,q).stageEvaluation()==null)return;started[0]=true;var p=pile(c,q);
            AutomaticCatchupGameTest.finiteCache(c,q,List.of());c.assertTrue(q.nutrition().spend(q.nutrition().sugar(),q.nutrition().protein()),"Declared pre-boundary queen receipts fully spent");
            long last=c.getLevel().getGameTime();
            for(int index=0;index<8;index++){
                long now=last+(index+1)*47;AutomaticCatchupGameTest.tickPile(c,p,now);var row=new JsonObject();row.addProperty("sample",index);row.addProperty("temperature",index==0?"cold":"warm");row.addProperty("registered_adults",workers+1);row.addProperty("queen",q.getUUID().toString());row.addProperty("nanos",p.catchup().lastNanos());row.addProperty("capture",p.catchup().captureNanos());row.addProperty("stage",p.catchup().stageNanos());row.addProperty("lifecycle",p.catchup().lifecycleNanos());row.addProperty("commit",p.catchup().commitNanos());row.addProperty("events",p.catchup().lastEvents());row.addProperty("pending",p.catchup().pendingTicks());evidence("profile",row);
                c.assertTrue(p.catchup().lastEvents()<=ColonyCatchup.MAX_EVENTS&&p.catchup().pendingTicks()==0&&adults(c,q).size()==workers+1,"Prospectively selected sample completes and conserves the whole registered population");
                c.assertTrue(p.catchup().captureNanos()+p.catchup().stageNanos()+p.catchup().lifecycleNanos()+p.catchup().commitNanos()<=p.catchup().lastNanos(),"Exclusive phase counters cannot exceed inclusive invocation cost");
            }
            c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground") public void eightProspectiveSmallColonyColdAndWarmSamples(GameTestHelper c){profile(c,3);}
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground") public void eightProspective120adultColdAndWarmSamples(GameTestHelper c){profile(c,119);}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void ordinaryDamageAndTemporaryCooldownRemainOnExistingDamagePath(GameTestHelper c){
        new AutomaticCatchupGameTest().colony(c,true,(q,p)->{
            long last=c.getLevel().getGameTime();AutomaticCatchupGameTest.tickPile(c,p,last);
            var target=adults(c,q).stream().filter(a->a.form()==AntForm.WORKER).findFirst().orElseThrow();
            c.assertTrue(target.nutrition().spend(target.nutrition().sugar(),0),"Finite pre-boundary adult food spent");
            var tag=ReconciliationGameTest.save(c,target);var life=tag.getCompound("AdultLife").orElseThrow();life.putLong("Fasting",target.adultLife().grace());life.putInt("DamageTicks",19);life.putBoolean("Started",true);
            var returning=AutomaticCatchupGameTest.replace(c,target,tag);c.assertTrue(returning.hurtServer(c.getLevel(),returning.damageSources().starve(),1),"Actual prior damage establishes the native remembered hit for cooldown");returning.damageCooldownTime=30;float health=returning.getHealth();
            ReconciliationGameTest.atTime(c,last+1,returning::tick);AutomaticCatchupGameTest.tickPile(c,p,last+1);
            var before=new JsonObject();before.addProperty("health_before",health);before.addProperty("health_after",returning.getHealth());before.addProperty("cooldown",returning.damageCooldownTime);before.addProperty("damage_ticks",returning.adultLife().snapshot().damageTicks());evidence("ordinary_cooldown_diagnosis",before);
            c.assertTrue(returning.getHealth()==health&&returning.adultLife().snapshot().damageTicks()==0&&returning.damageCooldownTime>10,"Ordinary damage still respects temporary cooldown");
            int cooldown=returning.damageCooldownTime;AutomaticCatchupGameTest.tickPile(c,p,last+42);
            c.assertTrue(returning.getHealth()==health-2&&returning.damageCooldownTime==cooldown&&p.catchup().pendingTicks()==0,"Two real historical damage events remain unfurled for temporary protection; loaded cooldown preserved");
            var row=new JsonObject();row.addProperty("health_before",health);row.addProperty("health_after",returning.getHealth());row.addProperty("cooldown_before",cooldown);row.addProperty("cooldown_after",returning.damageCooldownTime);evidence("temporary_cooldown",row);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void layingIdentityRefusalPreservesExactQueenAndFoodReceipts(GameTestHelper c){
        var q=new TierThreeFixture().start(c,32,false,4,new BlockPos(32,4,32),Direction.EAST,true);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||!q.founding().ready()||adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;started[0]=true;
            long last=c.getLevel().getGameTime();fundedLarva(c,q,pile(c,q),1200);var p=pile(c,q);var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.putLong("LastLayingTick",0);p=BroodGameTest.restorePile(c,p,tag);
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,food(4));c.assertTrue(p.growth().allows(p.supply(c.getLevel())),"Existing ordinary rolling-income admission is genuinely funded in this accepted baseline");
            var id=UUID.nameUUIDFromBytes(("prime_ants:egg:"+q.getUUID()+":2401:1").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var conflict=AntEntities.WORKER.create(c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);c.assertTrue(conflict!=null,"Real conflicting body");conflict.initializeCallow(id,UUID.randomUUID(),c.absolutePos(BlockPos.ZERO));conflict.setUUID(ColonyMembers.workerId(id));conflict.setPos(net.minecraft.world.phys.Vec3.atCenterOf(c.absolutePos(new BlockPos(15,6,15))));c.assertTrue(c.getLevel().addFreshEntity(conflict),"Original event's prospective worker identity is already occupied");
            var nutrition=q.nutrition().snapshot();var contents=cache.contents();AutomaticCatchupGameTest.tickPile(c,p,last+1);
            c.assertTrue(p.records().size()==1&&q.nutrition().snapshot().equals(nutrition)&&ItemStack.CODEC.listOf().encodeStart(JsonOps.INSTANCE,contents).getOrThrow().equals(ItemStack.CODEC.listOf().encodeStart(JsonOps.INSTANCE,cache.contents()).getOrThrow()),"Event identity refusal occurs before queen spending or physical food removal");
            conflict.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);AutomaticCatchupGameTest.tickPile(c,p,last+2);
            c.assertTrue(p.records().size()==2&&q.nutrition().spentSugar()==nutrition.spentSugar()+Nutrition.EGG_SUGAR&&q.nutrition().spentProtein()==nutrition.spentProtein()+Nutrition.EGG_PROTEIN,"Later eligible ordinary event lays exactly one real brood with unchanged costs");
            var row=new JsonObject();row.addProperty("refused_event",id.toString());row.addProperty("queen_sugar_debit",Nutrition.EGG_SUGAR);row.addProperty("queen_protein_debit",Nutrition.EGG_PROTEIN);evidence("laying_refusal",row);replay(c,q,p,last+2);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void ownedNonzeroMaterialStoreDeserializesBeforeAutomaticCatchupAndCapacityLoss(GameTestHelper c){
        var q=new TierThreeFixture().start(c,32,false,4,new BlockPos(32,4,32),Direction.EAST,true);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||!q.founding().ready()||pile(c,q).stageEvaluation()==null||adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;started[0]=true;
            var l=c.getLevel();var original=MaterialStore.owned(l,q.getUUID(),q.founding().plan());var saved=original.saveWithFullMetadata(l.registryAccess());var marker=original.getBlockPos();
            SurfaceFixture.loadBlock(c,original,saved);var restored=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
            c.assertTrue(restored!=original&&restored.getBlockPos().equals(marker)&&restored.contents().stream().filter(s->s.is(Items.STONE)).count()==32&&restored.saveWithFullMetadata(l.registryAccess()).equals(saved),"Normal loader installs a different owned store with all 32 canonical stone units and exact release/accounting fields");
            long now=l.getGameTime()+101;AutomaticCatchupGameTest.tickPile(c,pile(c,q),now);replay(c,q,pile(c,q),now);
            c.assertTrue(pile(c,q).catchup().appliedTicks()==100&&MaterialStore.owned(l,q.getUUID(),q.founding().plan())==restored&&restored.saveWithFullMetadata(l.registryAccess()).equals(saved),"Automatic catchup and nursery same/older replay leave the reconstructed canonical store exact");
            var entrance=q.founding().plan().entrance();var oldEntrance=l.getBlockState(entrance);l.setBlock(entrance,net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),3);
            c.assertTrue(restored.capacity()==MaterialStore.CAPACITY&&restored.size()==32&&!restored.room(new ItemStack(Items.STONE)),"Capacity loss refuses new stock without truncating the 32 represented stone units, including the reduced non-clay share");
            SurfaceFixture.loadBlock(c,restored,saved);var reduced=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
            c.assertTrue(reduced!=restored&&reduced.capacity()==MaterialStore.CAPACITY&&reduced.contents().stream().filter(s->s.is(Items.STONE)).count()==32&&reduced.saveWithFullMetadata(l.registryAccess()).equals(saved),"Reconstruction under capacity loss retains exact canonical contents, owner, release identity and accounting");
            var row=new JsonObject();row.addProperty("different_object_before_automatic",true);row.addProperty("different_object_under_capacity_loss",true);row.addProperty("marker",marker.toString());row.addProperty("owner",reduced.componentOwner().toString());row.addProperty("saved_tag",saved.toString());row.addProperty("final_tag",reduced.saveWithFullMetadata(l.registryAccess()).toString());row.addProperty("capacity_after_loss",reduced.capacity());row.addProperty("automatic_ticks",100);row.addProperty("nursery_same_and_older_replay",true);row.add("per_kind",new Gson().toJsonTree(TierThreeEffectsProbe.contents(reduced.contents())));evidence("owned_material_store_normal_loader",row);l.setBlock(entrance,oldEntrance,3);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void physicalCargoActorFirstReloadUnknownAndOlderReplayKeepExactCustody(GameTestHelper c){partialService(c,true);}
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void physicalCargoNurseryFirstReloadUnknownAndOlderReplayKeepExactCustody(GameTestHelper c){partialService(c,false);}
    private void partialService(GameTestHelper c,boolean adultFirst){
        var f=new TierThreeFixture();var q=f.start(c,32,false,4,new BlockPos(32,4,32),Direction.EAST,true);boolean[] initialized={false},started={false};
        c.onEachTick(()->{
            if(started[0]||!q.founding().ready()||adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;
            if(!initialized[0]){existingLarva(c,q,pile(c,q),1200,false);AutomaticCatchupGameTest.finiteCache(c,q,food(1));initialized[0]=true;return;}
            var selected=adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().phase()==WorkerTasks.Phase.NURSE_FEED&&a.workerTasks().feedingTicks()>=5&&WorkerTasks.food(a.getMainHandItem())&&pile(c,q).records().stream().anyMatch(r->r.id().equals(a.workerTasks().recipientId())))
                .max(Comparator.comparingInt(a->a.workerTasks().feedingTicks())).orElse(null);
            if(selected==null)return;started[0]=true;var l=c.getLevel();var p=pile(c,q);var cache=(NestCache)l.getBlockEntity(q.founding().plan().cache());long total=accounted(c,q,cache),last=l.getGameTime();
            var unit=selected.getMainHandItem().copy();var id=selected.getUUID();var recipient=selected.workerTasks().recipientId();int partial=selected.workerTasks().feedingTicks();
            var position=selected.position();var nutrition=p.records().stream().filter(r->r.id().equals(recipient)).findFirst().orElseThrow().nutrition().snapshot();
            try(var hidden=UnavailableCells.hide(c,q.founding().plan().at(2,-2,-3))){AutomaticCatchupGameTest.tickPile(c,p,last+2);}
            c.assertTrue(p.catchup().pendingServices()==0&&ItemStack.matches(selected.getMainHandItem(),unit)&&selected.workerTasks().feedingTicks()==partial&&selected.position().equals(position)&&accounted(c,q,cache)==total,"UNKNOWN preserves actual ordinary partial action and food; aggregate has no intention");
            var saved=p.saveWithFullMetadata(l.registryAccess());BroodPile[] back={p};var original=p;
            ReconciliationGameTest.atTime(c,last+2,()->{
                if(!adultFirst)back[0]=BroodGameTest.restorePile(c,original,saved);
                for(var actor:new ArrayList<>(f.workers(c,q)))f.fx.f.restore(c,actor);
                if(adultFirst)back[0]=BroodGameTest.restorePile(c,original,saved);
            });p=back[0];var restored=(LasiusNigerEntity)l.getEntity(id);
            c.assertTrue(p.saveWithFullMetadata(l.registryAccess()).equals(saved)&&ItemStack.matches(restored.getMainHandItem(),unit)&&restored.workerTasks().feedingTicks()==partial&&restored.position().equals(position),"Normal entity/block loaders preserve service clock, actor identity, exact partial action and original unit in either load order");
            var intentions=p.catchup().serviceObservations();
            try(var hidden=UnavailableCells.hide(c,q.founding().plan().at(2,-2,-3))){
                AutomaticCatchupGameTest.tickPile(c,p,last+5);
                c.assertTrue(p.catchup().pendingTicks()==4&&intentions.equals(p.catchup().serviceObservations())&&ItemStack.matches(restored.getMainHandItem(),unit)&&accounted(c,q,cache)==total,"UNKNOWN preserves exact saved service intentions and physical custody without granting action or a receipt");
            }
            AutomaticCatchupGameTest.tickPile(c,p,last+7);c.assertTrue(p.catchup().pendingTicks()==0&&accounted(c,q,cache)==total,"Restored known geometry resumes the same service state with exact food conservation");
            var larva=p.records().stream().filter(r->r.id().equals(recipient)).findFirst().orElseThrow();
            c.assertTrue(restored.getMainHandItem().isEmpty()&&restored.workerTasks().feedingTicks()==partial&&restored.position().equals(position)&&larva.nutrition().snapshot().valid()&&larva.nutrition().consumedUnits()>nutrition.apples()+nutrition.chickens()&&accounted(c,q,cache)==total,"Fixed distribution consumes actual cargo once while preserving ordinary partial controller state and position");
            var row=new JsonObject();row.addProperty("load_order",adultFirst?"actor_first":"nursery_first");row.addProperty("ordinary_partial_ticks",partial);row.addProperty("original_actor",id.toString());row.addProperty("original_unit",unit.toString());row.addProperty("pending_trip_state",p.catchup().pendingServices());row.addProperty("boundary",last);row.addProperty("accounted_before",total);row.addProperty("accounted_after",accounted(c,q,cache));row.addProperty("physical_actor_not_moved",true);row.add("saved_intentions",new Gson().toJsonTree(intentions));evidence("partial_service_reload_unknown_replay",row);replay(c,q,p,last+7);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void automaticAggregateLayingConflictRefusesBeforeInvestmentAndReplaysExactly(GameTestHelper c){
        var q=new TierThreeFixture().start(c,32,false,4,new BlockPos(32,4,32),Direction.EAST,true);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||!q.founding().ready()||adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;started[0]=true;
            long last=c.getLevel().getGameTime();fundedLarva(c,q,pile(c,q),1200);var p=pile(c,q);var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.putLong("LastLayingTick",0);p=BroodGameTest.restorePile(c,p,tag);
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,food(4));var conflicts=new ArrayList<LasiusNigerEntity>();
            // The one historical decision and resumed ordinary C decision have distinct event identities.
            // Occupy both to observe aggregate refusal independently of C's unavoidable maintenance.
            for(long at:List.of(2401L,2402L)){
                var id=UUID.nameUUIDFromBytes(("prime_ants:egg:"+q.getUUID()+":"+at+":1").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var conflict=AntEntities.WORKER.create(c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);c.assertTrue(conflict!=null,"Real conflicting prospective body");conflict.initializeCallow(id,UUID.randomUUID(),c.absolutePos(BlockPos.ZERO));conflict.setUUID(ColonyMembers.workerId(id));conflict.setPos(net.minecraft.world.phys.Vec3.atCenterOf(c.absolutePos(new BlockPos(15,6,15))));c.assertTrue(c.getLevel().addFreshEntity(conflict),"Prospective event body is occupied before the production hook");conflicts.add(conflict);
            }
            var nutrition=q.nutrition().snapshot();long maintenance=q.adultLife().maintenanceTicks();var contents=ItemStack.CODEC.listOf().encodeStart(JsonOps.INSTANCE,cache.contents()).getOrThrow();var brood=p.records().stream().map(BroodRecord::id).toList();var consumed=p.consumed();long accountedBefore=accounted(c,q,cache);
            c.assertTrue(p.aggregateCare(c.getLevel())&&p.growth().allowsAggregate(p.supply(c.getLevel()))&&q.nutrition().sugar()>=Nutrition.EGG_SUGAR&&q.nutrition().protein()>=Nutrition.EGG_PROTEIN,"Actual refusal baseline has care, finite funding and queen affordability before automatic aggregate dispatch");GrowthObservations.start(c,q,"aggregate_refusal");
            AutomaticCatchupGameTest.tickPile(c,p,last+2);int decisions=GrowthObservations.aggregateLayingDecisions(q);GrowthObservations.stop(q);
            c.assertTrue(p.catchup().appliedTicks()==1&&p.catchup().lastEvents()>0&&p.loadedTicks()==2402&&decisions>0,"Production next-laying hook proves automatic aggregate dispatch reaches a funded laying decision before refusal and resumed C");
            c.assertTrue(p.records().stream().map(BroodRecord::id).toList().equals(brood)&&p.consumed().equals(consumed)&&p.lastLayingTick()==0&&q.nutrition().spentProtein()==nutrition.spentProtein()&&q.nutrition().spentSugar()-nutrition.spentSugar()==q.adultLife().maintenanceTicks()-maintenance&&q.nutrition().consumedUnits()==nutrition.apples()+nutrition.chickens()&&contents.equals(ItemStack.CODEC.listOf().encodeStart(JsonOps.INSTANCE,cache.contents()).getOrThrow()),"Aggregate refusal adds no egg debit, food transfer, new brood identity or investment receipt; only exact adult maintenance is paid");
            c.assertTrue(accounted(c,q,cache)==accountedBefore&&q.nutrition().snapshot().valid(),"Refused automatic investment conserves every represented physical/ingested unit and exact nutrition equation");
            var row=new JsonObject();row.addProperty("aggregate_missed_ticks",1);row.addProperty("resumed_ordinary_ticks",1);row.addProperty("events",p.catchup().lastEvents());row.addProperty("maintenance_sugar",q.adultLife().maintenanceTicks()-maintenance);row.addProperty("egg_spending",0);row.addProperty("food_transfer",0);row.addProperty("new_brood_identity",false);row.addProperty("investment_receipt",false);row.addProperty("accounted_units",accounted(c,q,cache));evidence("aggregate_identity_refusal",row);replay(c,q,p,last+2);conflicts.forEach(LasiusNigerEntity::discard);c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:surface_ground")
    public void newlyLaidBroodEmergesAtVirtualBirthAndAgesThroughRemainingInterval(GameTestHelper c){
        var q=new TierThreeFixture().start(c,32,false,4,new BlockPos(32,4,32),Direction.EAST,true);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||!q.founding().ready()||adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;started[0]=true;
            long last=c.getLevel().getGameTime();var p=pile(c,q);var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.putLong(ColonyClock.FIELD,last);tag.putLong("LastOrdinaryGameTime",last);tag.putLong("LastLayingTick",p.loadedTicks());p=BroodGameTest.restorePile(c,p,tag);
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,food(4));var initial=adults(c,q).stream().map(LasiusNigerEntity::getUUID).collect(java.util.stream.Collectors.toSet());long total=accounted(c,q,cache),now=last+701;
            AutomaticCatchupGameTest.tickPile(c,p,now);var born=adults(c,q).stream().filter(w->!initial.contains(w.getUUID())).toList();
            var completed=p;
            c.assertTrue(!born.isEmpty()&&p.catchup().appliedTicks()==700&&p.catchup().pendingTicks()==0,"Costed eggs laid during absence become real new registered adults in one invocation");
            c.assertTrue(born.stream().allMatch(w->completed.consumed().contains(w.broodId())&&w.elapsedAgeTicks()==now-1-w.simulationEvidence().boundary()&&w.elapsedAgeTicks()>0&&w.nutrition().snapshot().valid()),"Every real new adult ages and pays maintenance from its virtual birth through the remaining interval");
            c.assertTrue(accounted(c,q,cache)==total&&p.records().stream().allMatch(r->r.nutrition().snapshot().valid()),"All new eggs, larvae and adults keep exact finite-unit receipts and nutrition equations");
            var row=new JsonObject();row.addProperty("initial_accounted_units",total);row.addProperty("final_accounted_units",accounted(c,q,cache));row.addProperty("events",p.catchup().lastEvents());row.add("new_births",new Gson().toJsonTree(born.stream().map(w->w.getUUID()+":"+w.simulationEvidence().boundary()+":"+w.elapsedAgeTicks()).toList()));evidence("new_laying_virtual_birth",row);replay(c,q,p,now);c.succeed();
        });
    }
}
