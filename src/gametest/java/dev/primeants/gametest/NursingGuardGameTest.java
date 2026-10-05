package dev.primeants.gametest;

import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;

/** Negative interventions occur only after real production founding/first brood/delivery. No manual ticks. */
public final class NursingGuardGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private final NursingGameTest audit=new NursingGameTest();
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void nursesFeedDisplacedQueenAtHerLivePosition(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},offCenterFeed={false};
        c.onEachTick(()->{
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){
                var p=q.founding().plan();var side=p.direction().getClockWise();
                // One test-only geometry intervention reproduces the genuine saved A2 displacement.
                // Existing nurses/tasks are untouched; only their normal physical feeding can consume food.
                var pos=net.minecraft.world.phys.Vec3.atBottomCenterOf(p.chamber()).add(side.getStepX()*0.4423-p.direction().getStepX()*0.1649,0,side.getStepZ()*0.4423-p.direction().getStepZ()*0.1649);
                q.setPos(pos);q.setOnGround(true);c.assertTrue(q.founding().ready(),"Displaced queen remains wholly in the valid original room");supplied[0]=true;audit.supply(c,q,1,0);
            }
            if(!supplied[0])return;c.assertTrue(audit.total(c,q)==1,"Live-position feeding conserves exactly one real supplied unit");audit.yields(c,q);
            for(var w:f.workers(c,q))if(w.workerTasks().feedingTicks()>0){
                c.assertTrue(WorkerTasks.reaches(c.getLevel(),w,q.position().add(0,0.25,0)),"Loaded feeding action reaches the actual recipient mouth");
                var fixed=net.minecraft.world.phys.Vec3.atBottomCenterOf(q.founding().plan().at(4,-1,-2)).add(0,0.25,0);
                if(fixed.distanceToSqr(q.position().add(0,0.25,0))>1.6)offCenterFeed[0]=true;
            }
            if(q.nutrition().apples()==1){c.assertTrue(offCenterFeed[0]&&q.nutrition().sugar()==4000&&q.nutrition().protein()==0&&q.bodyReserve()==0&&f.workers(c,q).size()==3,"One physical feed succeeds where the old fixed stand cannot reach; no reserve refill or synthetic growth");c.succeed();}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void disabledAbsentOutOfReachAndForeignNursesCannotWithdraw(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},checked={false};
        c.onEachTick(()->{
            if(checked[0])return;f.absentCaregivers(c,q);var p=audit.pile(c,q);if(p==null)return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q,1,1);}
            var n=f.cache(c,q);if(n==null||n.size()!=2)return;checked[0]=true;
            var nurse=f.workers(c,q).stream().filter(w->!q.founding().claimedBy(w)).findFirst().orElseThrow();
            c.assertTrue(!n.withdraw(nurse,q.founding().plan())&&audit.consumed(c,q)==0,"Disabled caregiver cannot withdraw or credit remote food");
            c.runAfterDelay(180,()->{
                c.assertTrue(n.size()==2&&audit.total(c,q)==2&&audit.consumed(c,q)==0&&p.records().isEmpty(),"Real home ticks leave food untouched without an eligible nurse");
                nurse.setNoAi(false);nurse.workerTasks().assignNurse(q.founding().plan());var v=net.minecraft.world.phys.Vec3.atBottomCenterOf(q.founding().plan().outside());nurse.teleportTo(v.x,v.y,v.z);
                c.assertTrue(!n.withdraw(nurse,q.founding().plan())&&n.size()==2,"Out-of-reach caregiver cannot withdraw");
                var o=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());nurse.save(o);var tag=o.buildResult();tag.putString("QueenId",UUID.randomUUID().toString());nurse.discard();
                var wrong=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(wrong),"Negative wrong-colony save fixture restores original body");
                v=net.minecraft.world.phys.Vec3.atBottomCenterOf(q.founding().plan().at(4,-1,-2));wrong.teleportTo(v.x,v.y,v.z);wrong.setOnGround(true);
                c.assertTrue(!n.withdraw(wrong,q.founding().plan())&&!q.feedBy(wrong,q.founding().plan())&&n.size()==2&&audit.consumed(c,q)==0,"Wrong-colony nurse at cache cannot transfer nutrition");c.succeed();
            });
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void sugarAloneCannotFundNewEggsAndFullQueenRetainsStock(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},checked={false};
        c.onEachTick(()->{
            var p=audit.pile(c,q);if(p==null)return;if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q,3,0);}
            if(supplied[0]){c.assertTrue(audit.total(c,q)==3,"Stock plus terminal consumption conserves three units");audit.yields(c,q);}
            var n=f.cache(c,q);if(!checked[0]&&q.nutrition().sugar()==8000&&n!=null&&n.size()==1){checked[0]=true;c.runAfterDelay(200,()->{c.assertTrue(q.nutrition().consumedUnits()==2&&q.nutrition().protein()==0&&q.nutrition().sugar()==8000&&n.size()==1&&p.records().isEmpty()&&f.workers(c,q).size()==3,"Full queen refuses third apple; sugar cannot substitute for egg protein");c.succeed();});}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void proteinAbsentFromFoodFedLarvaeStallsDespiteSugar(GameTestHelper c){stall(c,4,1,true);}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void exhaustedFoodNutritionStopsUnsupportedLarvae(GameTestHelper c){stall(c,1,1,false);}
    private void stall(GameTestHelper c,int apples,int chickens,boolean sugarPresent){
        var q=f.start(c);boolean[] supplied={false},checked={false};
        c.onEachTick(()->{
            var p=audit.pile(c,q);if(p==null)return;if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q,apples,chickens);}
            if(supplied[0]){c.assertTrue(audit.total(c,q)==apples+chickens,"Accounting includes terminal consumption");audit.yields(c,q);}
            var larvae=p.records().stream().filter(r->!r.founding()&&r.stage()==BroodStage.LARVA).toList();
            if(!checked[0]&&larvae.size()==3&&(!sugarPresent||larvae.stream().anyMatch(r->r.nutrition().sugar()>0))&&p.condition().equals("larva_sugar_or_protein_exhausted")){
                checked[0]=true;var ids=larvae.stream().map(BroodRecord::id).toList();long laying=p.lastLayingTick();
                c.runAfterDelay(250,()->{c.assertTrue(p.records().stream().map(BroodRecord::id).toList().equals(ids)&&p.records().stream().allMatch(r->r.progress()==0&&r.stage()==BroodStage.LARVA&&r.nutrition().protein()==0)&&p.lastLayingTick()==laying&&f.workers(c,q).size()==3,"Eggs hatch, but larvae cannot develop without their own protein; full slots preserve IDs");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void interruptedRefusedFeedingAndRestorationConserveCargoOnce(GameTestHelper c){
        LasiusNigerEntity[] q={f.start(c)},nurse={null};boolean[] supplied={false},checked={false},done={false};
        c.onEachTick(()->{
            if(done[0])return;var p=audit.pile(c,q[0]);if(p==null)return;
            if(!supplied[0]&&q[0].founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q[0],1,0);}
            if(supplied[0])c.assertTrue(audit.total(c,q[0])==1,"Interrupted feed conserves original unit");
            if(!checked[0]){
                var w=f.workers(c,q[0]).stream().filter(a->a.workerTasks().nursing()&&a.getMainHandItem().is(Items.APPLE)&&a.workerTasks().feedingTicks()>2).findFirst();if(w.isEmpty())return;checked[0]=true;nurse[0]=w.get();
                var stack=nurse[0].getMainHandItem().copy();long receipts=audit.consumed(c,q[0]);nurse[0].setNoAi(true);nurse[0]=f.restore(c,nurse[0]);q[0]=f.restore(c,q[0]);
                c.runAfterDelay(60,()->{
                    c.assertTrue(ItemStack.matches(stack,nurse[0].getMainHandItem())&&audit.consumed(c,q[0])==receipts&&!q[0].feedBy(nurse[0],q[0].founding().plan()),"Interrupted/restored disabled nurse retains cargo and credits nothing");
                    q[0].setNoAi(true);nurse[0].setNoAi(false);
                    c.runAfterDelay(100,()->{
                        c.assertTrue(audit.consumed(c,q[0])==receipts&&audit.total(c,q[0])==1,"Unavailable recipient retains cargo or receives physical cache return");q[0].setNoAi(false);
                        c.runAfterDelay(300,()->{
                            c.assertTrue(q[0].nutrition().apples()==1&&q[0].nutrition().sugar()==4000&&audit.total(c,q[0])==1&&f.workers(c,q[0]).stream().allMatch(a->a.getMainHandItem().isEmpty()),"Accepted action clears one stack and credits yield once");
                            q[0]=f.restore(c,q[0]);c.runAfterDelay(80,()->{c.assertTrue(q[0].nutrition().apples()==1&&q[0].nutrition().sugar()==4000&&audit.total(c,q[0])==1,"Persisted consumption cannot repeat");done[0]=true;c.succeed();});
                        });
                    });
                });
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void nurseDeathRefusalUsesPersistedCustodyWithoutNutritionCredit(GameTestHelper c){
        var q=f.start(c);String name="T10-nurse-death-"+UUID.randomUUID();boolean[] supplied={false},killed={false};
        c.onEachTick(()->{
            if(killed[0])return;var p=audit.pile(c,q);if(p==null)return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.drop(c,q.founding().plan().at(-3,0,1),TransferGameTest.stock(name,1));}
            var nurse=f.workers(c,q).stream().filter(w->w.workerTasks().nursing()&&w.getMainHandItem().is(Items.APPLE)).findFirst();if(nurse.isEmpty())return;killed[0]=true;var w=nurse.get();var cargo=w.getMainHandItem().copy();TransferFault.block(name,0);f.workers(c,q).stream().filter(a->a!=w&&!q.founding().claimedBy(a)).forEach(a->a.setNoAi(true));
            w.hurtServer(c.getLevel(),w.damageSources().generic(),1000);w.die(w.damageSources().generic());
            c.runAfterDelay(40,()->{
                c.assertTrue(w.isRemoved()&&w.getMainHandItem().isEmpty()&&TransferGameTest.custody(c,name)==1&&TransferGameTest.world(c,name)==0&&audit.consumed(c,q)==0&&audit.total(c,q)==1&&ColonyMembers.get(c.getLevel()).member(w.getUUID()).dead(),"Dead nurse stock remains custody, never nutrition");
                new TransferGameTest().restoreCustody(c,name);c.assertTrue(ItemStack.matches(cargo,TransferGameTest.pending(c,name).getFirst().stack()),"Actual cargo components survive SavedData disk");TransferFault.release(name);
                c.runAfterDelay(80,()->{c.assertTrue(TransferGameTest.custody(c,name)==0&&audit.total(c,q)==1&&audit.consumed(c,q)==0&&f.workers(c,q).size()==2,"Recovery conserves cargo without replacement or remote feeding");c.succeed();});
            });
        });
    }
    private BroodPile restorePile(GameTestHelper c,BroodPile p,net.minecraft.nbt.CompoundTag tag){var pos=p.getBlockPos();var state=p.getBlockState();c.getLevel().removeBlockEntity(pos);var r=(BroodPile)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,tag,c.getLevel().registryAccess());c.assertTrue(r!=null,"Normal reused-pile restore");c.getLevel().setBlockEntity(r);return r;}
    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void foodFedLarvalStoresAndRealMemberRegistryPersist(GameTestHelper c){
        LasiusNigerEntity[] q={f.start(c)};boolean[] supplied={false},restored={false};
        c.onEachTick(()->{
            var p=audit.pile(c,q[0]);if(p==null||restored[0])return;if(!supplied[0]&&q[0].founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q[0],6,8);}
            if(p.records().stream().noneMatch(r->!r.founding()&&r.stage()==BroodStage.LARVA&&r.progress()>5&&r.nutrition().sugar()>0&&r.nutrition().protein()>0))return;
            restored[0]=true;var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());long consumed=audit.consumed(c,q[0]);var ids=p.records().stream().map(BroodRecord::id).toList();var loaded=restorePile(c,p,tag);q[0]=f.restore(c,q[0]);
            c.assertTrue(loaded.saveWithFullMetadata(c.getLevel().registryAccess()).equals(tag)&&audit.consumed(c,q[0])==consumed&&audit.total(c,q[0])==14,"Normal restoration exactly preserves reused IDs, partial progress, ingested stores and terminal receipts");
            var members=ColonyMembers.get(c.getLevel()).members(q[0].getUUID());c.getLevel().getDataStorage().saveAndJoin();
            try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())){
                var saved=disk.get(ColonyMembers.TYPE);c.assertTrue(saved!=null&&saved.members(q[0].getUUID()).equals(members),"Actual SavedData disk restores exact real member identities, home and occupied state");c.getLevel().getDataStorage().set(ColonyMembers.TYPE,saved);
            }
            c.runAfterDelay(200,()->{c.assertTrue(audit.total(c,q[0])==14&&audit.pile(c,q[0]).original().size()==3&&ids.stream().allMatch(id->audit.pile(c,q[0]).records().stream().anyMatch(r->r.id().equals(id))||audit.pile(c,q[0]).consumed().contains(id)),"Loaded growth continues from persisted nutrition without losing or duplicating brood identity");audit.yields(c,q[0]);c.succeed();});
        });
    }
    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void reusedSlotsRestoredRefusalAndDeadWorkerNeverDuplicateIdentities(GameTestHelper c){
        LasiusNigerEntity[] q={f.start(c)};boolean[] supplied={false},armed={false},restored={false},emerged={false};net.minecraft.nbt.CompoundTag[] stale={null};Set<UUID> newBrood=new HashSet<>();
        c.onEachTick(()->{
            var p=audit.pile(c,q[0]);if(p==null||emerged[0])return;if(!supplied[0]&&q[0].founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q[0],6,8);}
            if(supplied[0])c.assertTrue(audit.total(c,q[0])==14,"Restoration retains physical and terminal units");
            var cocoons=p.records().stream().filter(r->!r.founding()&&r.stage()==BroodStage.COCOON).toList();
            if(!armed[0]&&!cocoons.isEmpty()){armed[0]=true;EmergenceFault.block(q[0].getUUID());}
            if(armed[0]&&!restored[0]&&cocoons.size()==3&&EmergenceFault.attempts(q[0].getUUID())>=6){
                restored[0]=true;p.records().forEach(r->newBrood.add(r.id()));q[0].setNoAi(true);f.workers(c,q[0]).forEach(w->w.setNoAi(true));stale[0]=p.saveWithFullMetadata(c.getLevel().registryAccess());restorePile(c,p,stale[0]);q[0]=f.restore(c,q[0]);
                c.getLevel().getDataStorage().saveAndJoin();EmergenceFault.release(q[0].getUUID());
            }
            if(restored[0]&&f.workers(c,q[0]).size()==6){
                emerged[0]=true;var adult=f.workers(c,q[0]).stream().filter(w->newBrood.contains(w.broodId())).findFirst().orElseThrow();UUID dead=adult.getUUID();adult.hurtServer(c.getLevel(),adult.damageSources().generic(),1000);
                c.runAfterDelay(40,()->{
                    c.assertTrue(adult.isRemoved()&&ColonyMembers.get(c.getLevel()).member(dead).dead(),"Only actual death frees known identity capacity");
                    restorePile(c,audit.pile(c,q[0]),stale[0]);c.runAfterDelay(BroodPile.stageTicks()+20,()->{var now=audit.pile(c,q[0]);c.assertTrue(now.records().isEmpty()&&now.consumed().size()==6&&now.original().size()==3&&f.workers(c,q[0]).size()==5&&c.getLevel().getEntity(dead)==null&&audit.total(c,q[0])==14,"Registry reconciles stale cocoons: records="+now.records().stream().map(r->r.stage()+"/"+r.progress()).toList()+" consumed="+now.consumed().size()+" workers="+f.workers(c,q[0]).size()+" food="+audit.total(c,q[0]));c.succeed();});
                });
            }
        });
    }
    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void deadQueenEndsLayingWhileFedOpenNestCocoonsRemainViable(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},killed={false},checked={false};Set<UUID> cocoons=new HashSet<>();long[] laid={0};
        c.onEachTick(()->{
            var p=audit.pile(c,q);if(p==null||checked[0])return;if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;audit.supply(c,q,6,8);}
            if(supplied[0])c.assertTrue(audit.total(c,q)==14,"Queen-death accounting retains consumed receipts");
            if(!killed[0]&&p.records().stream().anyMatch(r->!r.founding()&&r.stage()==BroodStage.COCOON)){killed[0]=true;p.records().stream().filter(r->!r.founding()&&r.stage()==BroodStage.COCOON).forEach(r->cocoons.add(r.id()));laid[0]=p.lastLayingTick();q.hurtServer(c.getLevel(),q.damageSources().generic(),1000);}
            if(killed[0]&&q.isRemoved()&&f.workers(c,q).stream().anyMatch(w->cocoons.contains(w.broodId()))){
                checked[0]=true;c.assertTrue(p.operational()&&p.lastLayingTick()==laid[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN&&q.founding().plan().nurseryProblem(c.getLevel(),q.getUUID(),true)==null,"Open habitat and viable fed cocoons survive queen absence");
                c.runAfterDelay(200,()->{c.assertTrue(p.lastLayingTick()==laid[0]&&p.operational()&&f.workers(c,q).size()>3,"Dead queen lays no new eggs; supported brood remains viable");c.succeed();});
            }
        });
    }
}
