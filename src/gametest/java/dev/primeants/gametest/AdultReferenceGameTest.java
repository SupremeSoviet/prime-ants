package dev.primeants.gametest;

import dev.primeants.entity.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Finite, predeclared legacy/live comparisons. Detached bodies use the actual ordinary tick method;
 * the supplied surface and phase case separately exercise normally inserted adults. */
public final class AdultReferenceGameTest {
    private LasiusNigerEntity worker(GameTestHelper c,UUID colony,long age,long lifespan,long grace,
                                     long sugar,int coverage,long fasting,int damage,long active){
        var ant=AntEntities.WORKER.create(c.getLevel(),EntitySpawnReason.BREEDING);
        c.assertTrue(ant!=null,"Worker factory");ant.setNoAi(true);ant.setNoGravity(true);
        var pos=Vec3.atCenterOf(c.absolutePos(new BlockPos(1,6,1)));ant.setPos(pos.x,pos.y,pos.z);
        ant.initializeCallow(dev.primeants.gametest.TestIdentities.next(),colony,c.absolutePos(BlockPos.ZERO),lifespan,grace);
        c.assertTrue(ant.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000)
            &&ant.nutrition().ingest(new ItemStack(Items.CHICKEN),8000,16000)
            &&ant.nutrition().spend(4000-sugar,0),"Finite ingested item receipts");
        var tag=ReconciliationGameTest.save(c,ant);tag.putLong("AntElapsedAgeTicks",age);
        tag.putLong("CallowTicks",8);tag.putLong("CallowDuration",10);
        var life=tag.getCompound("AdultLife").orElseThrow();life.putLong("Fasting",fasting);
        life.putLong("ActiveTicks",active);life.putInt("CoverageRemaining",coverage);life.putInt("DamageTicks",damage);
        life.putLong("Maintenance",7);life.putLong("CoveredTicks",28);life.putInt("MealTicks",3);
        life.putBoolean("Started",true);
        return ReconciliationGameTest.load(c,tag);
    }
    private void ordinary(GameTestHelper c,LasiusNigerEntity ant,LegacyAdultShadow shadow,long time){
        ReconciliationGameTest.atTime(c,time,()->{
            try{ant.tick();}catch(RuntimeException error){shadow.exception(ant,error);}
        });
        shadow.assertAgreement(c);
    }
    @GameTest(maxTicks=1000)
    public void maintenanceExhaustionAndIncomingMealsMatchIndependentLegacy(GameTestHelper c){
        var colony=dev.primeants.gametest.TestIdentities.next();var ant=worker(c,colony,10,1000,50,2,3,7,17,35);
        var shadow=LegacyAdultShadow.watch(c,colony,"detached_actual_ordinary_steps_with_incoming_meals");
        long now=c.getLevel().getGameTime();
        for(int tick=1;tick<=40;tick++){
            if(tick==15)c.assertTrue(ant.nutrition().ingest(new ItemStack(Items.APPLE),8000,16000),"Apple between actual ordinary steps");
            if(tick==20)c.assertTrue(ant.nutrition().ingest(new ItemStack(Items.SWEET_BERRIES),8000,16000),"Berry between actual ordinary steps");
            ordinary(c,ant,shadow,now+tick);
            if(tick==11)c.assertTrue(ant.nutrition().sugar()==0&&ant.adultLife().fasting()==0,"Eleven prepaid plus sugar-covered ticks");
            if(tick==14)c.assertTrue(ant.adultLife().fasting()==3,"Exact sugar exhaustion before new meal");
        }
        c.assertTrue(shadow.steps()==40&&ant.elapsedAgeTicks()==50&&ant.nutrition().apples()==2
            &&ant.nutrition().berries()==1&&ant.nutrition().protein()==8000,"Exact finite input and clocks");
        shadow.close();c.succeed();
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void queenFoundingExemptionAndActivationMatchIndependentLegacy(GameTestHelper c){
        var queen=new WorkerForagingGameTest().start(c);
        var shadow=LegacyAdultShadow.watch(c,queen.getUUID(),"normally_inserted_founding_phase_transition");
        c.onEachTick(()->{
            shadow.assertAgreement(c);
            if(!queen.adultLife().started())return;
            c.assertTrue(shadow.exempt()>0&&shadow.activations(queen.getUUID())==1&&queen.adultLife().activeTicks()>0,
                "Actual reserve-only exemption followed by one ordinary activation");
            shadow.close();c.succeed();
        });
    }
    @GameTest(maxTicks=1000)
    public void terminalAgeAndStarvationStepsMatchIndependentLegacy(GameTestHelper c){
        var colony=dev.primeants.gametest.TestIdentities.next();var shadow=LegacyAdultShadow.watch(c,colony,"detached_actual_terminal_steps");
        var aged=worker(c,colony,19,20,5,0,0,5,19,35);
        var starving=worker(c,colony,10,1000,5,0,0,5,19,35);
        aged.setHealth(1);starving.setHealth(1);long now=c.getLevel().getGameTime();
        ordinary(c,aged,shadow,now+1);ordinary(c,starving,shadow,now+1);
        c.assertTrue(!aged.isAlive()&&!starving.isAlive()&&aged.adultLife().death().equals("age_limit")
            &&starving.adultLife().death().equals("starvation"),"Age wins when both causes qualify; starvation otherwise");
        for(var ant:new LasiusNigerEntity[]{aged,starving}){
            var history=com.google.gson.JsonParser.parseString(dev.primeants.worker.AdultHistory.get(c.getLevel()).records().get(ant.getUUID().toString())).getAsJsonObject();
            c.assertTrue(ant.simulationEvidence().latest()==now+1&&history.get("lastSimulationGameTime").getAsLong()==now+1
                &&history.get("age").getAsLong()==ant.elapsedAgeTicks()&&ant.callowAgeTicks()==8,"Fatal step observed once with no post-death callow action");
        }
        shadow.close();c.succeed();
    }
    @GameTest(maxTicks=1000)
    public void fastingSaturationAgeBoundaryAndZeroPrefixesMatchIndependentLegacy(GameTestHelper c){
        var colony=dev.primeants.gametest.TestIdentities.next();var shadow=LegacyAdultShadow.watch(c,colony,"detached_actual_saturating_boundaries");
        var ant=worker(c,colony,Long.MAX_VALUE-1,Long.MAX_VALUE,Long.MAX_VALUE,0,0,Long.MAX_VALUE-1,0,35);
        long now=c.getLevel().getGameTime();ordinary(c,ant,shadow,now+1);
        c.assertTrue(ant.elapsedAgeTicks()==Long.MAX_VALUE&&ant.adultLife().fasting()==Long.MAX_VALUE
            &&ant.adultLife().snapshot().damageTicks()==1,"Exact age and fasting maximum boundary");
        var before=ant.biologySnapshot();ant.tick();c.assertTrue(ant.biologySnapshot().equals(before)&&shadow.steps()==1,"MAX age does not enter another biological branch");
        var queen=AntEntities.QUEEN.create(c.getLevel(),EntitySpawnReason.BREEDING);c.assertTrue(queen!=null,"Queen factory");
        var tag=ReconciliationGameTest.save(c,queen);var life=tag.getCompound("AdultLife").orElseThrow();
        life.putBoolean("Started",false);life.putLong("FastingGrace",1);life.putLong("Fasting",1);life.putInt("DamageTicks",19);
        queen=ReconciliationGameTest.load(c,tag);var qshadow=LegacyAdultShadow.watch(c,queen.getUUID(),"zero_completed_activation_then_actual_step");
        // A detached control must not suffer unrelated damage at the factory's default world origin.
        var safe=Vec3.atCenterOf(c.absolutePos(new BlockPos(1,6,1)));
        queen.setNoAi(true);queen.setNoGravity(true);queen.setPos(safe.x,safe.y,safe.z);
        var input=queen.biologySnapshot();var zero=LasiusNigerEntity.advanceAdult(input,AdultLife.Phase.ACTIVE,0);
        var boundary=LasiusNigerEntity.advanceAdult(input,AdultLife.Phase.ACTIVE,1);
        c.assertTrue(zero.completed()==0&&zero.state().equals(input)&&boundary.completed()==0&&boundary.state().equals(input)
            &&!queen.adultLife().started(),"Neither zero request nor zero-completed preview activates biology");
        ordinary(c,queen,qshadow,now+1);c.assertTrue(queen.adultLife().started()&&queen.adultLife().activeTicks()==1,"Activation belongs to one actual ordinary step");
        shadow.close();qshadow.close();c.succeed();
    }
    @GameTest(maxTicks=1000)
    public void loadedActiveCounterMaximumMatchesIndependentLegacy(GameTestHelper c){
        var colony=dev.primeants.gametest.TestIdentities.next();var ant=worker(c,colony,10,1000,50,2,0,0,0,Long.MAX_VALUE);
        c.assertTrue(ant.adultLife().snapshot().valid()&&ant.nutrition().snapshot().valid(),"Existing owners accept the boundary save input");
        ReconciliationGameTest.atTime(c,c.getLevel().getGameTime()+1,ant::tick);
        c.assertTrue(ant.adultLife().activeTicks()==Long.MAX_VALUE,"Owner-authorized saturation replaces the historical wrap/exception discrepancy");
        c.assertTrue(ant.elapsedAgeTicks()==11&&ant.adultLife().maintenanceTicks()==8&&ant.adultLife().coveredTicks()==29
            &&ant.adultLife().coverageRemaining()==3&&ant.nutrition().sugar()==1&&ant.nutrition().spentSugar()==3999,
            "All non-boundary effects retain their exact historical values");
        var saved=ReconciliationGameTest.save(c,ant);var reconstructed=ReconciliationGameTest.load(c,saved);
        c.assertTrue(saved.equals(ReconciliationGameTest.save(c,reconstructed)),"Maximum remains accepted after advance/save/reconstruct");
        ReconciliationEvidence.log("counter_active_saturation","accepted_maximum_ordinary_save",new com.google.gson.Gson().toJsonTree(ant.biologySnapshot()).getAsJsonObject());c.succeed();
    }
}
