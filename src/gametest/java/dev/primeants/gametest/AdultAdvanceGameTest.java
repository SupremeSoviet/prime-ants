package dev.primeants.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.primeants.entity.*;
import dev.primeants.worker.Nutrition;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.UUID;

/** Frozen exact integer controls: ordinary server ticks, finite ingested stores, no incoming meals. */
public final class AdultAdvanceGameTest {
    // Input age/callow=10/8 of 10, maintenance/active/covered=7/35/28, damage=0 unless stated.
    // expected: completed, sugar, coverage, fasting, maintenance debit, covered increment, damage, age.
    private record Scenario(String name,AntForm form,long age,long lifespan,long grace,long sugar,int coverage,long fasting,
                            long request,long completed,long finalSugar,int finalCoverage,long finalFasting,long debit,
                            long covered,int damage,long finalAge,String event) { }
    private static final Scenario PREPAID=new Scenario("prepaid",AntForm.WORKER,10,1000,50,2,3,0,2,2,2,1,0,0,2,0,12,"NONE");
    private static final Scenario COVERAGE=new Scenario("coverage_exhaustion",AntForm.WORKER,10,1000,50,2,3,0,5,5,1,2,0,1,5,0,15,"NONE");
    private static final Scenario SUGAR=new Scenario("sugar_exhaustion",AntForm.WORKER,10,1000,50,2,0,0,10,10,0,0,2,2,8,0,20,"NONE");
    private static final Scenario FASTING=new Scenario("nonzero_fasting",AntForm.WORKER,10,1000,50,0,0,7,5,5,0,0,12,0,0,0,15,"NONE");
    private static final Scenario CALLOW=new Scenario("callow_saturation",AntForm.WORKER,10,1000,50,2,0,0,4,4,1,0,0,1,4,0,14,"NONE");
    private static final Scenario AGE=new Scenario("worker_age_boundary",AntForm.WORKER,17,20,50,10,0,0,1000000,21,4,3,0,6,21,19,38,"AGE_DAMAGE");
    private static final Scenario STARVE=new Scenario("long_starvation_boundary",AntForm.WORKER,10,1000000,5,2,3,0,Long.MAX_VALUE,34,0,0,23,2,11,19,44,"STARVATION_DAMAGE");
    private static final Scenario QUEEN=new Scenario("started_queen",AntForm.QUEEN,10,1000,50,2,0,0,7,7,0,1,0,2,7,0,17,"NONE");
    private static final Scenario PENDING=new Scenario("pending_starvation_damage",AntForm.WORKER,10,1000,5,0,0,10,Long.MAX_VALUE,2,0,0,12,0,0,19,12,"STARVATION_DAMAGE");
    private static final Scenario RESET=new Scenario("prepaid_resets_pending_starvation",AntForm.WORKER,10,1000,5,0,3,10,2,2,0,1,0,0,2,0,12,"NONE");

    private void compare(GameTestHelper c,Scenario s){compare(c,s,0);}
    private void compare(GameTestHelper c,Scenario s,int initialDamage){
        var factory=(s.form()==AntForm.WORKER?AntEntities.WORKER:AntEntities.QUEEN).create(c.getLevel(),EntitySpawnReason.BREEDING);
        c.assertTrue(factory!=null,"Adult factory");factory.setNoAi(true);factory.setNoGravity(true);
        var pos=net.minecraft.world.phys.Vec3.atCenterOf(c.absolutePos(new BlockPos(1,6,1)));factory.setPos(pos.x,pos.y,pos.z);
        if(s.form()==AntForm.WORKER)factory.initializeCallow(UUID.randomUUID(),UUID.randomUUID(),c.absolutePos(BlockPos.ZERO),s.lifespan(),s.grace());
        c.assertTrue(factory.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)
            &&factory.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000)&&factory.nutrition().spend(4000-s.sugar(),0),"Declared finite ingested fixture with physical item receipts");
        factory.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        var tag=ReconciliationGameTest.save(c,factory);tag.putLong("AntElapsedAgeTicks",s.age());
        if(s.form()==AntForm.WORKER){tag.putLong("CallowTicks",8);tag.putLong("CallowDuration",10);}
        var life=tag.getCompound("AdultLife").orElseThrow();life.putLong("Lifespan",s.lifespan());life.putLong("FastingGrace",s.grace());
        life.putBoolean("Started",true);life.putLong("Fasting",s.fasting());life.putLong("Maintenance",7);life.putLong("ActiveTicks",35);
        life.putLong("CoveredTicks",28);life.putInt("CoverageRemaining",s.coverage());life.putInt("DamageTicks",initialDamage);life.putInt("MealTicks",0);
        var live=ReconciliationGameTest.load(c,tag);var initial=live.biologySnapshot();var cargo=live.getMainHandItem().copy();
        long start=System.nanoTime();var actual=LasiusNigerEntity.advanceAdult(initial,AdultLife.Phase.ACTIVE,s.request());long nanos=System.nanoTime()-start;
        var expectedFood=new Nutrition.State(s.finalSugar(),8000,1,0,1,0,0,0,0,4000-s.finalSugar(),0,0,0);
        var expectedLife=new AdultLife.State(s.lifespan(),s.grace(),s.finalFasting(),7+s.debit(),35+s.completed(),s.damage(),0,s.finalCoverage(),28+s.covered(),true,"");
        var expected=new LasiusNigerEntity.Biology(s.form(),s.finalAge(),s.form()==AntForm.WORKER,s.form()==AntForm.WORKER?10:0,s.form()==AntForm.WORKER?10:0,expectedLife,expectedFood);
        c.assertTrue(live.biologySnapshot().equals(initial)&&ItemStack.matches(cargo,live.getMainHandItem()),"Pure primitive leaves original body/cargo unchanged");
        c.assertTrue(actual.state().equals(expected)&&actual.completed()==s.completed()&&actual.remaining()==s.request()-s.completed()
            &&actual.event().equals(s.event())&&actual.eventTick().orElse(0)==(s.event().equals("NONE")?0:s.completed()+1),"Frozen exact primitive result: "+s+" actual="+actual);
        c.assertTrue(actual.status().equals(s.event().equals("NONE")?"COMPLETED":"DAMAGE_BOUNDARY"),"Explicit completion/event status");
        float health=live.getHealth();c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(live),"Control inserted for ordinary server ticks");
        boolean[] compared={false};
        c.onEachTick(()->{
            long ticks=live.elapsedAgeTicks()-s.age();
            if(!compared[0]&&ticks==s.completed()){
                compared[0]=true;c.assertTrue(live.biologySnapshot().equals(expected)&&live.getHealth()==health
                    &&ItemStack.matches(cargo,live.getMainHandItem()),"Ordinary no-incoming-meal control agrees exactly immediately before event: "+live.biologySnapshot());
                var out=new JsonObject();out.addProperty("scenario",s.name());out.addProperty("control","ordinary_server_ticks_no_incoming_meals");
                out.addProperty("requested",s.request());out.addProperty("completed",actual.completed());out.addProperty("remaining",actual.remaining());out.addProperty("event",actual.event());
                out.addProperty("event_tick",actual.eventTick().orElse(0));out.addProperty("primitive_nanoseconds",nanos);
                var gson=new Gson();out.add("input",gson.toJsonTree(initial));out.add("expected",gson.toJsonTree(expected));out.add("actual",gson.toJsonTree(actual.state()));out.add("live_before_event",gson.toJsonTree(live.biologySnapshot()));
                ReconciliationEvidence.log("adult_advance","finite_ingested_no_incoming_meals",out);
                if(s.event().equals("NONE"))c.succeed();
            }else if(compared[0]&&!s.event().equals("NONE")&&ticks==s.completed()+1){
                c.assertTrue(live.getHealth()==health-1&&live.adultLife().snapshot().damageTicks()==0,"Next ordinary tick reaches exact expected health-damage event");
                var out=new JsonObject();out.addProperty("scenario",s.name());out.addProperty("ordinary_ticks",ticks);out.addProperty("health_before",health);out.addProperty("health_after",live.getHealth());out.addProperty("event",s.event());
                ReconciliationEvidence.log("adult_next_damage","ordinary_server_ticks_no_incoming_meals",out);c.succeed();
            }else c.assertTrue(ticks<=s.completed()+1,"Control may not skip the frozen comparison/event boundary");
        });
    }
    @GameTest(maxTicks=1000) public void prepaidCoverageMatchesOrdinaryTicks(GameTestHelper c){compare(c,PREPAID);}
    @GameTest(maxTicks=1000) public void coverageExhaustionMatchesOrdinaryTicks(GameTestHelper c){compare(c,COVERAGE);}
    @GameTest(maxTicks=1000) public void finiteSugarExhaustionMatchesOrdinaryTicks(GameTestHelper c){compare(c,SUGAR);}
    @GameTest(maxTicks=1000) public void nonzeroFastingMatchesOrdinaryTicks(GameTestHelper c){compare(c,FASTING);}
    @GameTest(maxTicks=1000) public void callowSaturationMatchesOrdinaryTicks(GameTestHelper c){compare(c,CALLOW);}
    @GameTest(maxTicks=1000) public void workerAgeBoundaryStopsBeforeOrdinaryDamage(GameTestHelper c){compare(c,AGE);}
    @GameTest(maxTicks=1000) public void maximumLongRequestStopsBeforeOrdinaryStarvationDamage(GameTestHelper c){compare(c,STARVE);}
    @GameTest(maxTicks=1000) public void startedQueenMatchesOrdinaryTicks(GameTestHelper c){compare(c,QUEEN);}
    @GameTest(maxTicks=1000) public void pendingStarvationDamageStopsBeforeNextOrdinaryEvent(GameTestHelper c){compare(c,PENDING,17);}
    @GameTest(maxTicks=1000) public void prepaidCoverageResetsPendingStarvationDamage(GameTestHelper c){compare(c,RESET,18);}

    @GameTest(maxTicks=1000,structure="prime_ants_test:idle_ground")
    public void reserveOnlyQueenExemptionMatchesOrdinaryTicks(GameTestHelper c){
        var q=new WorkerForagingGameTest().start(c);boolean[] done={false};
        c.onEachTick(()->{
            if(done[0]||q.founding().plan()==null||q.adultLife().started())return;done[0]=true;q.setNoAi(true);
            var initial=q.biologySnapshot();c.assertTrue(q.founding().reserveOnlyFounding(c.getLevel()),"Explicit live founding exemption established");
            var result=LasiusNigerEntity.advanceAdult(initial,AdultLife.Phase.RESERVE_ONLY,10);
            var expected=new LasiusNigerEntity.Biology(AntForm.QUEEN,initial.age()+10,false,0,0,initial.life(),initial.nutrition());
            c.assertTrue(result.completed()==10&&result.remaining()==0&&result.state().equals(expected),"Founding exemption advances only elapsed age");
            c.runAfterDelay(10,()->{c.assertTrue(q.biologySnapshot().equals(expected)&&q.founding().reserveOnlyFounding(c.getLevel()),"Ordinary exempt queen agrees exactly with unchanged maintenance/nutrition");
                var out=new JsonObject();out.addProperty("scenario","reserve_only_queen");out.addProperty("completed",10);out.addProperty("remaining",0);out.add("expected",new Gson().toJsonTree(expected));out.add("actual",new Gson().toJsonTree(q.biologySnapshot()));ReconciliationEvidence.log("adult_advance","ordinary_server_ticks_no_incoming_meals",out);c.succeed();});
        });
    }
    @GameTest(maxTicks=1000)
    public void unsupportedPhaseAndOverflowLeaveExplicitInputUnchanged(GameTestHelper c){
        var q=AntEntities.QUEEN.create(c.getLevel(),EntitySpawnReason.BREEDING);c.assertTrue(q!=null,"Queen factory");
        var initial=q.biologySnapshot();var unknown=LasiusNigerEntity.advanceAdult(initial,AdultLife.Phase.UNKNOWN,100);
        var zero=LasiusNigerEntity.advanceAdult(initial,AdultLife.Phase.ACTIVE,0);
        c.assertTrue(zero.status().equals("COMPLETED")&&zero.state()==initial&&zero.completed()==0&&zero.remaining()==0,"Zero request does not latch a new active policy");
        c.assertTrue(unknown.status().equals("UNRESOLVED_QUEEN_PHASE")&&unknown.state()==initial&&unknown.completed()==0&&unknown.remaining()==100,"Unknown phase leaves input unchanged and entire request owed");
        var s=initial.life();var over=new AdultLife.State(s.lifespan(),s.grace(),0,0,Long.MAX_VALUE,0,0,0,0,true,"");
        var input=new LasiusNigerEntity.Biology(AntForm.QUEEN,10,false,0,0,over,initial.nutrition());
        var overflow=LasiusNigerEntity.advanceAdult(input,AdultLife.Phase.ACTIVE,1);
        c.assertTrue(overflow.status().equals("UNRESOLVED_ARITHMETIC")&&overflow.state()==input&&overflow.completed()==0&&overflow.remaining()==1,"Counter overflow is explicit, atomic and unchanged");
        var ageInput=new LasiusNigerEntity.Biology(AntForm.QUEEN,Long.MAX_VALUE,false,0,0,s,initial.nutrition());
        var age=LasiusNigerEntity.advanceAdult(ageInput,AdultLife.Phase.RESERVE_ONLY,1);
        c.assertTrue(age.status().equals("UNRESOLVED_AGE_DOMAIN")&&age.state()==ageInput&&age.remaining()==1,"No silent maximum-age clamp");
        // A structurally valid restored activation state whose very first ordinary tick reaches damage.
        q.setNoAi(true);q.setNoGravity(true);
        var safe=net.minecraft.world.phys.Vec3.atCenterOf(c.absolutePos(new BlockPos(1,6,1)));q.setPos(safe.x,safe.y,safe.z);
        var tag=ReconciliationGameTest.save(c,q);var savedLife=tag.getCompound("AdultLife").orElseThrow();
        savedLife.putBoolean("Started",false);savedLife.putLong("FastingGrace",1);savedLife.putLong("Fasting",1);savedLife.putInt("DamageTicks",19);
        var live=ReconciliationGameTest.load(c,tag);var before=live.biologySnapshot();
        c.assertTrue(!live.founding().reserveOnlyFounding(c.getLevel()),"Existing owner establishes ACTIVE policy before its first activation tick");
        long start=System.nanoTime();var boundary=LasiusNigerEntity.advanceAdult(before,AdultLife.Phase.ACTIVE,1);long nanos=System.nanoTime()-start;
        var out=new JsonObject();out.addProperty("scenario","zero_completed_activation_damage_boundary");out.addProperty("control","ordinary_server_ticks_no_incoming_meals");
        out.addProperty("requested",1);out.addProperty("completed",boundary.completed());out.addProperty("remaining",boundary.remaining());
        out.addProperty("event",boundary.event());out.addProperty("event_tick",boundary.eventTick().orElse(0));out.addProperty("primitive_nanoseconds",nanos);
        var gson=new Gson();out.add("input",gson.toJsonTree(before));out.add("expected",gson.toJsonTree(before));out.add("actual",gson.toJsonTree(boundary.state()));
        out.add("live_before_event",gson.toJsonTree(live.biologySnapshot()));ReconciliationEvidence.log("adult_advance","finite_ingested_no_incoming_meals",out);
        c.assertTrue(boundary.status().equals("DAMAGE_BOUNDARY")&&boundary.completed()==0&&boundary.remaining()==1
            &&boundary.eventTick().orElseThrow()==1&&boundary.event().equals("STARVATION_DAMAGE")&&boundary.state().equals(before),
            "Zero completed ticks preserve pre-activation state: expected="+before+" actual="+boundary);
        float health=live.getHealth();c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(live),"Zero-prefix control inserted normally");
        c.onEachTick(()->{
            if(live.elapsedAgeTicks()==before.age())return;
            c.assertTrue(live.elapsedAgeTicks()==before.age()+1&&live.adultLife().started()&&live.adultLife().activeTicks()==1
                &&live.adultLife().fasting()==2&&live.adultLife().snapshot().damageTicks()==0&&live.getHealth()==health-1,
                "First ordinary tick activates and reaches the expected damage event; none of it belonged to completed zero ticks");
            var next=new JsonObject();next.addProperty("scenario","zero_completed_activation_damage_boundary");next.addProperty("ordinary_ticks",1);
            next.addProperty("health_before",health);next.addProperty("health_after",live.getHealth());next.addProperty("event","STARVATION_DAMAGE");
            ReconciliationEvidence.log("adult_next_damage","ordinary_server_ticks_no_incoming_meals",next);c.succeed();
        });
    }
}
