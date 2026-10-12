package dev.primeants.gametest;

import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Real controller ticks after a disclosed initial checkpoint. No ingestion/transfer/controller tick calls. */
public final class NursingRecoveryGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private final NursingGameTest audit=new NursingGameTest();
    private static final Gson JSON=new Gson();
    private static final int RECOVERY_BOUND=600, SETTLED=200;
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void personalMealOfCarriedFoodResumesPhysicalServiceByTheSameNurse(GameTestHelper c){run(c,0);}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void savedEmptyFeedingPhaseRecoversWithoutSaveEditing(GameTestHelper c){run(c,1);}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void nonFoodFeedingCargoRemainsCanonicalAndUncredited(GameTestHelper c){run(c,2);}
    private static CompoundTag save(GameTestHelper c,LasiusNigerEntity a){var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());a.save(o);return o.buildResult();}
    private static void balance(GameTestHelper c,LasiusNigerEntity a){var n=a.nutrition();c.assertTrue(n.gainedSugar()+n.receivedSugar()==n.sugar()+n.spentSugar()+n.givenSugar()&&n.gainedProtein()==n.protein()+n.spentProtein(),"Independent finite nutrition receipts balance for "+a.getUUID());}
    private void emit(GameTestHelper c,LasiusNigerEntity q,LasiusNigerEntity w,int mode,String event){
        var t=w.workerTasks();var n=w.nutrition();var s=audit.snapshot(c,q);var row=new LinkedHashMap<String,Object>();
        row.put("mode",mode);row.put("event",event);row.put("helperTick",c.getTick());row.put("nurseryLoaded",audit.pile(c,q).loadedTicks());row.put("worker",w.getUUID());row.put("queen",q.getUUID());row.put("livingCanonical",c.getLevel().getEntity(w.getUUID())==w&&w.isAlive());
        row.put("phase",t.phase());row.put("reason",t.reason());row.put("recipient",t.recipientId());row.put("feedingTicks",t.feedingTicks());row.put("mealTicks",w.adultLife().mealTicks());row.put("fasting",w.adultLife().fasting());row.put("cargo",w.getMainHandItem().toString());row.put("authorized",t.nursingAuthorized(c.getLevel()));row.put("caregiver",t.caregiver(c.getLevel(),q.founding().plan()));
        row.put("workerReceipts",n.consumedUnits());row.put("workerSugar",n.sugar());row.put("workerGainedSugar",n.gainedSugar());row.put("workerSpentSugar",n.spentSugar());row.put("workerGivenSugar",n.givenSugar());row.put("workerReceivedSugar",n.receivedSugar());row.put("queenReceipts",q.nutrition().consumedUnits());row.put("queenSugar",q.nutrition().sugar());row.put("queenGainedSugar",q.nutrition().gainedSugar());row.put("queenSpentSugar",q.nutrition().spentSugar());
        row.put("acceptingRecipient",WorkerTasks.queenNeedsFood(q,new ItemStack(Items.APPLE)));row.put("cache",f.cache(c,q).contents().toString());row.put("position",w.position().toString());row.put("cacheReach",WorkerTasks.reaches(c.getLevel(),w,Vec3.atBottomCenterOf(q.founding().plan().cache()).add(0,.15,0)));row.put("queenReach",WorkerTasks.reaches(c.getLevel(),w,q.position().add(0,.25,0)));
        row.put("food",Map.of("ground",s.ground(),"carried",s.carried(),"cache",s.cache(),"custody",s.custody(),"queen",s.queen(),"brood",s.brood(),"adults",s.adults(),"history",s.history(),"total",s.total()));
        PrimeAnts.LOGGER.info("T04 NURSING RECOVERY {}",JSON.toJson(row));
    }
    private void run(GameTestHelper c,int mode){
        var q=f.start(c);int units=mode==0?2:1;boolean[] supplied={false},enabled={false},initialized={false},meal={false},empty={false},withdrawn={false};
        LasiusNigerEntity[] nurse={null};long[] boundary={-1},fed={-1},firstFeed={-1};int[] contiguous={0},maxFeed={0};UUID[] id={null};Vec3[] start={null};
        c.onEachTick(()->{
            if(!initialized[0]){
                if(!enabled[0])f.absentCaregivers(c,q);
                if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q,units,0);}
                var cache=f.cache(c,q);var ws=f.workers(c,q);var p=q.founding().plan();
                if(!supplied[0]||cache==null||!q.founding().ready())return;
                if(!enabled[0]){
                    if(cache.size()!=units||ws.size()!=3||ws.stream().anyMatch(LasiusNigerEntity::isCallow))return;
                    var w=ws.stream().filter(a->!q.founding().claimedBy(a)).findFirst().orElseThrow();
                    for(var a:ws)if(a!=w){a.setNoAi(true);f.shelterDisabled(c,q,a);}
                    w.setNoAi(false);if(w.workerTasks().phase()==WorkerTasks.Phase.NURSERY)c.assertTrue(w.workerTasks().assignNurse(p),"Initial ordinary eligible nursing assignment");nurse[0]=w;enabled[0]=true;
                }
                var w=nurse[0];var t=w.workerTasks();
                if(mode==0&&(t.phase()!=WorkerTasks.Phase.NURSE_FEED||!w.getMainHandItem().is(Items.APPLE)||cache.size()!=1))return;
                if(mode!=0){
                    // Declared legacy initial checkpoint only; never used to manufacture the meal interruption.
                    var tag=save(c,w).getCompoundOrEmpty("WorkerTask");tag.putString("Phase","NURSE_FEED");tag.putString("Recipient",q.getUUID().toString());tag.putInt("FeedingTicks",11);tag.putInt("Cooldown",0);
                    t.load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag));
                    if(mode==2)w.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIRT));
                }
                c.assertTrue(w.nutrition().consumedUnits()==0&&w.nutrition().sugar()==0&&q.nutrition().consumedUnits()==0,"Initial ingested history is empty; all food is real cache/cargo");
                // Initial hungry history is declared. Carried apple was physically withdrawn by this controller.
                var life=save(c,w).getCompoundOrEmpty("AdultLife");life.putLong("Fasting",mode==0?12000:0);life.putBoolean("Started",true);life.putInt("MealTicks",0);w.adultLife().load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),life),w);
                var pos=Vec3.atBottomCenterOf(p.at(5,1,-2));w.teleportTo(pos.x,pos.y,pos.z);w.setOnGround(true);start[0]=pos;
                c.assertTrue(t.nursingAuthorized(c.getLevel())&&t.recipientId().equals(q.getUUID())&&WorkerTasks.queenNeedsFood(q,new ItemStack(Items.APPLE))&&cache.size()==1&&NestPlan.walkable(c.getLevel(),p.at(4,-1,-2)),"Initial authorized nurse, accepting queen and reachable finite replacement apple");
                boundary[0]=c.getTick();id[0]=w.getUUID();initialized[0]=true;emit(c,q,w,mode,"initial_boundary_no_further_interventions");return;
            }
            var w=nurse[0];var t=w.workerTasks();long elapsed=c.getTick()-boundary[0];
            c.assertTrue(w.getUUID().equals(id[0])&&w.isAlive()&&!w.isRemoved()&&c.getLevel().getEntity(id[0])==w,"Same living canonical nurse through acceptance");
            c.assertTrue(audit.snapshot(c,q).total()==units,"Every finite food unit stays ground/cache/cargo/custody or exactly one terminal consumption");balance(c,w);balance(c,q);
            if(elapsed<=80||elapsed%20==0)emit(c,q,w,mode,"real_tick");
            if(mode==2){
                c.assertTrue(t.phase()==WorkerTasks.Phase.NURSE_FEED&&q.getUUID().equals(t.recipientId())&&w.getMainHandItem().is(Items.DIRT)&&w.getMainHandItem().getCount()==1&&w.nutrition().consumedUnits()==0&&q.nutrition().consumedUnits()==0&&f.cache(c,q).size()==1&&t.nursingAuthorized(c.getLevel()),"Non-food cargo and recipient retained, no withdrawal, meal or nutrition credit");
                if(elapsed>=SETTLED){emit(c,q,w,mode,"accepted_non_food_retention_200");c.succeed();}return;
            }
            if(mode==0&&w.nutrition().apples()==1&&!meal[0]){meal[0]=true;emit(c,q,w,mode,"actual_personal_consumption");c.assertTrue(elapsed>=20&&q.nutrition().consumedUnits()==0&&w.getMainHandItem().isEmpty()&&t.phase()==WorkerTasks.Phase.NURSE_FEED&&q.getUUID().equals(t.recipientId()),"Ordinary 20-tick personal meal interrupts carried nursing food exactly once");}
            if(w.getMainHandItem().isEmpty()&&t.phase()!=WorkerTasks.Phase.NURSE_FEED&&!empty[0]){empty[0]=true;emit(c,q,w,mode,"empty_recovery");c.assertTrue(t.feedingTicks()==0&&t.recipientId()==null,"Empty recovery discards only stale recipient/action");}
            if(empty[0]&&w.getMainHandItem().is(Items.APPLE)&&f.cache(c,q).size()==0&&!withdrawn[0]){withdrawn[0]=true;emit(c,q,w,mode,"replacement_withdrawal");c.assertTrue(t.phase()==WorkerTasks.Phase.NURSE_FEED&&q.getUUID().equals(t.recipientId())&&WorkerTasks.reaches(c.getLevel(),w,Vec3.atBottomCenterOf(q.founding().plan().cache()).add(0,.15,0))&&w.position().distanceToSqr(start[0])>.1,"Same nurse walks and physically withdraws the existing replacement");}
            if(t.feedingTicks()>0){
                c.assertTrue(withdrawn[0]&&t.nursingAuthorized(c.getLevel())&&WorkerTasks.reaches(c.getLevel(),w,q.position().add(0,.25,0)),"Feeding requires actual replacement, authorization and live mouth reach");
                if(firstFeed[0]<0)firstFeed[0]=c.getTick();contiguous[0]++;maxFeed[0]=Math.max(maxFeed[0],contiguous[0]);
            }else contiguous[0]=0;
            if(q.nutrition().apples()==1&&fed[0]<0){fed[0]=c.getTick();emit(c,q,w,mode,"recipient_consumption");c.assertTrue(withdrawn[0]&&empty[0]&&maxFeed[0]>=19&&c.getTick()-firstFeed[0]>=19&&w.getMainHandItem().isEmpty(),"Exactly one ordinary full 20-tick recipient feeding, with no synthetic completion");}
            if(fed[0]>=0){
                c.assertTrue(w.nutrition().apples()==(mode==0?1:0)&&w.nutrition().consumedUnits()==(mode==0?1:0)&&q.nutrition().apples()==1&&q.nutrition().consumedUnits()==1&&t.nursingAuthorized(c.getLevel())&&t.caregiver(c.getLevel(),q.founding().plan()),"Single personal receipt and single recipient receipt remain final; nurse remains authorized ordinary caregiver");
                if(c.getTick()-fed[0]>=SETTLED){c.assertTrue(mode!=0||meal[0],"Actual original personal consumption witnessed");emit(c,q,w,mode,"accepted_physical_service_200_settled");c.succeed();return;}
            }
            if(elapsed>=RECOVERY_BOUND){emit(c,q,w,mode,"recovery_bound_failure");c.assertTrue(fed[0]>=0,"Same nurse did not physically recover within frozen 600 ticks; phase="+t.phase()+" cargo="+w.getMainHandItem()+" recipient="+t.recipientId()+" meal="+meal[0]+" accepting="+WorkerTasks.queenNeedsFood(q,new ItemStack(Items.APPLE))+" cache="+f.cache(c,q).size());}
        });
    }
}
