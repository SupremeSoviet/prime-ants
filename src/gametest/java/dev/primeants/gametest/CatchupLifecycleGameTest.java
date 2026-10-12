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
    private static BroodPile pile(GameTestHelper c,LasiusNigerEntity q){return new NursingGameTest().pile(c,q);}
    private static List<LasiusNigerEntity> adults(GameTestHelper c,LasiusNigerEntity q){return AutomaticCatchupGameTest.adults(c,q);}
    private static void tick(GameTestHelper c,LasiusNigerEntity q,long now){
        var bodies=adults(c,q);ReconciliationGameTest.atTime(c,now,()->bodies.forEach(LasiusNigerEntity::tick));
        AutomaticCatchupGameTest.tickPile(c,pile(c,q),now);
    }
    private static List<ItemStack> food(int each){var units=new ArrayList<ItemStack>();for(int n=0;n<each;n++){units.add(new ItemStack(Items.APPLE));units.add(new ItemStack(Items.CHICKEN));}return units;}
    private static void evidence(String scenario,JsonObject row){row.addProperty("scenario",scenario);row.addProperty("fixture","accepted_history_timestamp_reconstruction");ReconciliationEvidence.log("t06_lifecycle","ordinary_production_hooks",row);}
    private static void replay(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long now){
        var saved=p.saveWithFullMetadata(c.getLevel().registryAccess());var biology=adults(c,q).stream().map(LasiusNigerEntity::biologySnapshot).toList();
        var registry=ChamberRegistry.get(c.getLevel()).colony(q.getUUID());
        var restored=BroodGameTest.restorePile(c,p,saved);AutomaticCatchupGameTest.tickPile(c,restored,now);
        c.assertTrue(restored.saveWithFullMetadata(c.getLevel().registryAccess()).equals(saved)&&biology.equals(adults(c,q).stream().map(LasiusNigerEntity::biologySnapshot).toList())&&registry.equals(ChamberRegistry.get(c.getLevel()).colony(q.getUUID())),"Same-C reconstruction preserves exact brood/adult/clock receipts and identities");
    }
    private static void fundedLarva(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long duration){
        c.assertTrue(q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN),"Existing dependent's real egg investment");
        var n=new Nutrition();c.assertTrue(n.ingest(new ItemStack(Items.APPLE),Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN)&&n.ingest(new ItemStack(Items.CHICKEN),Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN),"Two declared physical baseline larval receipts");
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
                c.assertTrue(target.nutrition().spend(target.nutrition().sugar(),0),"Declared adult's consumed initial apple is spent before B");
                var tag=ReconciliationGameTest.save(c,target);var life=tag.getCompound("AdultLife").orElseThrow();life.putLong("Fasting",target.adultLife().grace()/2);life.putInt("CoverageRemaining",0);life.putBoolean("Started",true);
                AutomaticCatchupGameTest.replace(c,target,tag);
            }
            });
            var ca=(NestCache)c.getLevel().getBlockEntity(live.founding().plan().cache());var cb=(NestCache)c.getLevel().getBlockEntity(away.founding().plan().cache());
            long totalA=accounted(c,live,ca),totalB=accounted(c,away,cb);var material=(MaterialStore)c.getLevel().getBlockEntity(ChamberExcavation.build(away.founding().plan(),"right").marker());var materialBefore=material.saveWithFullMetadata(c.getLevel().registryAccess());
            c.assertTrue(totalA==totalB&&ca.size()==cb.size(),"Matched finite baseline has identical real unit totals and stock counts");
            var initialAdults=adults(c,away).stream().map(LasiusNigerEntity::getUUID).collect(java.util.stream.Collectors.toSet());
            for(long i=1;i<=span+1;i++)tick(c,live,last+i);
            AutomaticCatchupGameTest.tickPile(c,pile(c,away),last+span+1);
            var a=pile(c,live);var b=pile(c,away);var row=new JsonObject();row.add("live",state(c,live));row.add("catchup",state(c,away));row.addProperty("span",span);row.addProperty("stage_duration",duration);row.addProperty("accounted_live",accounted(c,live,ca));row.addProperty("accounted_catchup",accounted(c,away,cb));row.addProperty("initial_accounted_live",totalA);row.addProperty("initial_accounted_catchup",totalB);
            var stocks=new JsonObject();for(var kind:List.of(Items.APPLE,Items.CHICKEN)){long x=ca.contents().stream().filter(s->s.is(kind)).count(),y=cb.contents().stream().filter(s->s.is(kind)).count();stocks.addProperty(kind.toString()+"_live",x);stocks.addProperty(kind.toString()+"_catchup",y);}row.add("stocks",stocks);evidence("competition",row);
            c.assertTrue(a.consumed().size()>3&&b.consumed().size()>3&&a.lastLayingTick()>2400&&b.lastLayingTick()>2400,"Both controls produce real dependent births and new costed eggs");
            c.assertTrue(Math.abs(adults(c,live).size()-adults(c,away).size())<=1&&Math.abs(a.records().size()-b.records().size())<=1,"Frozen one adult and one occupied brood slot tolerances");
            for(var kind:List.of(Items.APPLE,Items.CHICKEN))c.assertTrue(Math.abs(ca.contents().stream().filter(s->s.is(kind)).count()-cb.contents().stream().filter(s->s.is(kind)).count())<=1,"Frozen one cached unit per represented kind");
            c.assertTrue(accounted(c,live,ca)==totalA&&accounted(c,away,cb)==totalB&&adults(c,live).stream().allMatch(a1->a1.nutrition().snapshot().valid())&&adults(c,away).stream().allMatch(a1->a1.nutrition().snapshot().valid())&&a.records().stream().allMatch(r->r.nutrition().snapshot().valid())&&b.records().stream().allMatch(r->r.nutrition().snapshot().valid()),"Exact within-run food/cargo/ingested conservation and nutrition equations");
            c.assertTrue(material.size()==32&&material.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore),"Nonzero material units and normal-save bytes remain exact");
            var liveProgress=a.records().stream().mapToLong(r->(r.stage().ordinal()-BroodStage.EGG.ordinal())*duration+r.progress()).sorted().toArray();
            var awayProgress=b.records().stream().mapToLong(r->(r.stage().ordinal()-BroodStage.EGG.ordinal())*duration+r.progress()).sorted().toArray();
            var born=adults(c,away).stream().filter(w->!initialAdults.contains(w.getUUID())).toList();
            c.assertTrue(!born.isEmpty()&&born.stream().allMatch(w->w.elapsedAgeTicks()>0&&w.simulationEvidence().boundary()<last+span),"Real aggregate births use virtual timestamps and continue aging through the remaining interval");
            replay(c,away,b,last+span+1);c.assertTrue(material.saveWithFullMetadata(c.getLevel().registryAccess()).equals(materialBefore),"Material save/replay exact");
            var exact=new JsonObject();exact.addProperty("material_units",material.size());exact.addProperty("material_save_replay_exact",true);exact.addProperty("food_each",totalA);exact.addProperty("food_conservation_exact",true);exact.add("virtual_adult_births",new Gson().toJsonTree(born.stream().map(w->w.getUUID()+":"+w.simulationEvidence().boundary()+":"+w.elapsedAgeTicks()).toList()));exact.add("live_development",new Gson().toJsonTree(liveProgress));exact.add("catchup_development",new Gson().toJsonTree(awayProgress));evidence("competition_exact_before_timing_verdict",exact);
            c.assertTrue(liveProgress.length==awayProgress.length,"This matched finite control must retain corresponding occupied brood");
            for(int i=0;i<liveProgress.length;i++)c.assertTrue(Math.abs(liveProgress[i]-awayProgress[i])<=duration*.1,"Frozen brood timing tolerance, including stage transitions: live="+Arrays.toString(liveProgress)+" catchup="+Arrays.toString(awayProgress));c.succeed();
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
            var p=pile(c,away);AutomaticCatchupGameTest.finiteCache(c,away,List.of());stageFixture(c,away,ColonyStage.MATURE,Map.of("mature:food",p.loadedTicks()-100));
            try(var hidden=UnavailableCells.hide(c,away.founding().plan().at(2,-2,-3))){AutomaticCatchupGameTest.tickPile(c,p,last+411);c.assertTrue(p.catchup().pendingTicks()==50&&p.catchup().unknownTicks()==0&&ChamberRegistry.get(c.getLevel()).colony(away.getUUID()).unmetSince().isEmpty(),"UNKNOWN breaks certainty without migration rebasing or resource mutation");}
            AutomaticCatchupGameTest.tickPile(c,p,last+412);long since=ChamberRegistry.get(c.getLevel()).colony(away.getUUID()).unmetSince().get("mature:food");
            c.assertTrue(p.catchup().pendingTicks()==0&&since>=p.loadedTicks()-1,"Restored certainty starts at observation end, never backdates previously unknown deficit");row.addProperty("unknown_restored_since",since);row.addProperty("restored_loaded",p.loadedTicks());evidence("stage_hysteresis",row);replay(c,away,p,last+412);c.succeed();
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
    public void newlyLaidBroodEmergesAtVirtualBirthAndAgesThroughRemainingInterval(GameTestHelper c){
        var q=new TierThreeFixture().start(c,32,false,4,new BlockPos(32,4,32),Direction.EAST,true);boolean[] started={false};
        c.onEachTick(()->{
            if(started[0]||!q.founding().ready()||adults(c,q).stream().filter(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(c.getLevel(),q.founding().plan())).count()<2)return;started[0]=true;
            long last=c.getLevel().getGameTime();var p=pile(c,q);var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.putLong(ColonyClock.FIELD,last);tag.putLong("LastOrdinaryGameTime",last);tag.putLong("LastLayingTick",p.loadedTicks());p=BroodGameTest.restorePile(c,p,tag);
            var cache=AutomaticCatchupGameTest.finiteCache(c,q,food(4));var initial=adults(c,q).stream().map(LasiusNigerEntity::getUUID).collect(java.util.stream.Collectors.toSet());long total=accounted(c,q,cache),now=last+701;
            AutomaticCatchupGameTest.tickPile(c,p,now);var born=adults(c,q).stream().filter(w->!initial.contains(w.getUUID())).toList();
            c.assertTrue(!born.isEmpty()&&p.catchup().appliedTicks()==700&&p.catchup().pendingTicks()==0,"Costed eggs laid during absence become real new registered adults in one invocation");
            var completed=p;
            c.assertTrue(born.stream().allMatch(w->completed.consumed().contains(w.broodId())&&w.elapsedAgeTicks()==now-1-w.simulationEvidence().boundary()&&w.elapsedAgeTicks()>0&&w.nutrition().snapshot().valid()),"Every real new adult ages and pays maintenance from its virtual birth through the remaining interval");
            c.assertTrue(accounted(c,q,cache)==total&&p.records().stream().allMatch(r->r.nutrition().snapshot().valid()),"All new eggs, larvae and adults keep exact finite-unit receipts and nutrition equations");
            var row=new JsonObject();row.addProperty("initial_accounted_units",total);row.addProperty("final_accounted_units",accounted(c,q,cache));row.addProperty("events",p.catchup().lastEvents());row.add("new_births",new Gson().toJsonTree(born.stream().map(w->w.getUUID()+":"+w.simulationEvidence().boundary()+":"+w.elapsedAgeTicks()).toList()));evidence("new_laying_virtual_birth",row);replay(c,q,p,now);c.succeed();
        });
    }
}
