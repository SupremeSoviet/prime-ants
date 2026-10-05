package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Real founding/brood/work precede disclosed negative scheduling and save fixtures. */
public final class RoleConflictGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private final NursingGameTest food=new NursingGameTest();
    private final ExpansionGameTest soil=new ExpansionGameTest();
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void deadForagerCannotStealEmptyHandedPartialBuilder(GameTestHelper c){conflict(c,0);}
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void restoredPartialBuilderAndQueenKeepReciprocalClaims(GameTestHelper c){conflict(c,1);}
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void conflictingPersistedForagerClaimReleasesWithoutLosingBuilderProgress(GameTestHelper c){conflict(c,2);}
    private void conflict(GameTestHelper c,int restoration){
        LasiusNigerEntity[] queen={f.start(c)};boolean[] supplied={false},killed={false},released={false},replacement={false};
        UUID[] builder={null},dead={null};int[] atDeath={0};long[] deathTick={0};boolean[] formerReleased={false},builderHeld={false};Map<UUID,ItemStack> retained=new HashMap<>();Set<UUID> disabled=new HashSet<>();
        c.onEachTick(()->{
            var q=queen[0];if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,6,8);}
            if(supplied[0]){c.assertTrue(food.total(c,q)==14,"Food/custody/terminal receipts conserved on every loaded tick");food.yields(c,q);}soil.balance(c,q);
            var j=NestExpansion.get(c.getLevel()).job(q.getUUID());if(j==null)return;
            var workers=f.workers(c,q);var claimBefore=j.claim;var b=workers.stream().filter(w->w.getUUID().equals(claimBefore)).findFirst().orElse(null);
            if(c.getTick()%1000==0)PrimeAnts.LOGGER.info("T12 conflict mode={} tick={} killed={} released={} replacement={} removed={} deposited={} builder={} forager={} workers={}",restoration,c.getTick(),killed[0],released[0],replacement[0],j.removed(),j.deposited,j.claim,q.founding().workerClaim(),workers.stream().map(w->w.getUUID()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem()+" enabled="+!w.isNoAi()).toList());
            if(!killed[0]){
                if(b!=null&&j.removed()>0&&j.removed()<12&&b.getMainHandItem().isEmpty()){
                    // Setup-only pause at a genuine empty-handed partial checkpoint. Restore enabled
                    // AI before the death/assignment event; no action/edit/cargo is fabricated.
                    b.setNoAi(true);builderHeld[0]=true;
                }
                // Hold only genuine nurse withdrawals; never manufacture or move food. Additional callows
                // are disabled in this negative fixture so they cannot accidentally hide the defect.
                for(var w:workers)if(!q.founding().claimedBy(w)&&w!=b){
                    if(WorkerTasks.food(w.getMainHandItem())){retained.putIfAbsent(w.getUUID(),w.getMainHandItem().copy());w.setNoAi(true);if(disabled.add(w.getUUID()))f.shelterDisabled(c,q,w);}
                    else if(w.isCallow()){w.setNoAi(true);if(disabled.add(w.getUUID()))f.shelterDisabled(c,q,w);}
                }
                if(retained.size()>=2)for(var w:workers)if(w!=b&&!q.founding().claimedBy(w)){w.setNoAi(true);if(disabled.add(w.getUUID()))f.shelterDisabled(c,q,w);}
                if(b==null||!builderHeld[0]||j.removed()==0||j.removed()==12||!b.getMainHandItem().isEmpty()||retained.size()<2)return;
                if(workers.stream().anyMatch(w->w!=b&&!q.founding().claimedBy(w)&&!w.isNoAi()&&w.getMainHandItem().isEmpty()))return;
                builder[0]=b.getUUID();atDeath[0]=j.removed();deathTick[0]=c.getTick();
                b.setNoAi(false);c.assertTrue(!b.isNoAi(),"Builder is mature, enabled, empty handed with active partial claim at the actual death event");
                var forager=workers.stream().filter(q.founding()::claimedBy).findFirst().orElseThrow();dead[0]=forager.getUUID();forager.hurtServer(c.getLevel(),forager.damageSources().generic(),1000);killed[0]=true;
                c.assertTrue(!forager.isAlive()&&q.founding().workerClaim()==null,"Normal damage releases actual forager claim");
                c.assertTrue(!b.workerTasks().assign(q.founding().plan())&&!b.workerTasks().assignNurse(q.founding().plan()),"Assignment boundaries independently reject an empty active builder");
                for(var w:workers)if(retained.containsKey(w.getUUID()))c.assertTrue(!w.workerTasks().assign(q.founding().plan())&&!w.workerTasks().assignConstruction(q.founding().plan())&&!w.workerTasks().assignNurse(q.founding().plan()),"All assignment boundaries preserve nursing cargo");
                if(restoration>0){
                    for(var w:workers)if(w.isAlive())f.restore(c,w);
                    if(restoration==2){
                        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());q.save(out);var tag=out.buildResult();
                        tag.getCompoundOrEmpty("Founding").putString("WorkerClaim",builder[0].toString());q.discard();
                        queen[0]=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
                        c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(queen[0]),"Restore disclosed conflicting queen claim on same real body");
                    }else queen[0]=f.restore(c,q);
                    c.getLevel().getDataStorage().saveAndJoin();
                    try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())){
                        var saved=disk.get(NestExpansion.TYPE);c.assertTrue(saved!=null&&builder[0].equals(saved.job(queen[0].getUUID()).claim)&&saved.job(queen[0].getUUID()).removed()==atDeath[0],"Actual disk preserves partial edits and builder claim");c.getLevel().getDataStorage().set(NestExpansion.TYPE,saved);
                    }
                }
                PrimeAnts.LOGGER.info("T12 role reproducer mode={} tick={} builder={} removed={} empty=true nurses={} dead={}",restoration,c.getTick(),builder[0],atDeath[0],retained.keySet(),dead[0]);return;
            }
            j=NestExpansion.get(c.getLevel()).job(q.getUUID());workers=f.workers(c,q);
            c.assertTrue(j.removed()>=atDeath[0]&&q.founding().ready()&&j.problem(c.getLevel(),q.getUUID())==null,"Edits do not rewind; shell/origin/habitat remain valid");
            if(j.claim!=null)c.assertTrue(!j.claim.equals(q.founding().workerClaim()),"Active construction claim must never also become replacement forager");
            if(!released[0]){
                c.assertTrue(q.founding().workerClaim()==null,"No suitable worker: foraging defers instead of stealing builder/cargo");
                for(var entry:retained.entrySet()){var w=workers.stream().filter(a->a.getUUID().equals(entry.getKey())).findFirst().orElseThrow();c.assertTrue(ItemStack.matches(entry.getValue(),w.getMainHandItem()),"Disabled genuine nursing cargo retains exact components");}
                if(c.getTick()-deathTick[0]>=40){
                    released[0]=true;var available=retained.keySet().stream().sorted().findFirst().orElseThrow();
                    // Release one actual loaded food nurse, the smallest availability change needed
                    // by this role test. Other explicitly held actors retain their tasks and cargo.
                    workers.stream().filter(w->w.getUUID().equals(available)).findFirst().orElseThrow().setNoAi(false);
                }
            }
            if(released[0]&&q.founding().workerClaim()!=null){var assigned=workers.stream().filter(q.founding()::claimedBy).findFirst().orElseThrow();c.assertTrue(!assigned.isNoAi()&&!assigned.isCallow()&&!assigned.getUUID().equals(dead[0])&&!assigned.getUUID().equals(j.claim),"Replacement is a real suitable worker after availability resumes");replacement[0]=true;}
            if(released[0]&&replacement[0]&&j.complete()){
                c.assertTrue(j.removed()==12&&j.deposited==12&&j.released==0&&workers.stream().noneMatch(w->w.getUUID().equals(dead[0]))&&ColonyMembers.get(c.getLevel()).member(dead[0]).dead(),"Construction finishes with conserved soil and no replacement spawn");
                if(restoration==0){
                    if(j.claim!=null)return;
                    if(!formerReleased[0]){formerReleased[0]=true;for(var w:workers)if(!w.getUUID().equals(builder[0])){if(q.founding().claimedBy(w))w.hurtServer(c.getLevel(),w.damageSources().generic(),1000);else w.setNoAi(true);}return;}
                    if(!builder[0].equals(q.founding().workerClaim()))return;
                    c.assertTrue(workers.stream().anyMatch(w->w.getUUID().equals(builder[0])&&!w.workerTasks().construction()),"Former builder accepts normal foraging after valid completion/release; no permanent role exclusion");
                }
                c.succeed();
            }
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void disabledNursingLabelsCannotReserveCareWithFourOtherMatureWorkers(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},checked={false};Set<UUID> initial=new HashSet<>(),held=new HashSet<>();
        c.onEachTick(()->{
            if(checked[0])return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.workers(c,q).forEach(w->initial.add(w.getUUID()));food.supply(c,q,6,8);}
            if(!supplied[0])return;c.assertTrue(food.total(c,q)==14,"Reservation fixture preserves ordinary food accounting");food.yields(c,q);soil.balance(c,q);
            var ws=f.workers(c,q);for(var w:ws)if(!initial.contains(w.getUUID())){
                w.setNoAi(true);if(held.add(w.getUUID())){
                    // Negative reservation fixture: keep actual new bodies loaded on the existing
                    // supported exterior trail, leaving both original nurses' feeding stands clear.
                    boolean placed=false;for(int forward=-6;forward<=-1&&!placed;forward++)for(int lane=-5;lane<=5;lane++){
                        var feet=q.founding().plan().at(forward,lane,1);var pos=net.minecraft.world.phys.Vec3.atBottomCenterOf(feet);var body=w.getBoundingBox().move(pos.subtract(w.position()));
                        if(!NestPlan.walkable(c.getLevel(),feet)||!c.getLevel().noCollision(w,body)||!c.getLevel().getEntities(w,body).isEmpty())continue;
                        w.teleportTo(pos.x,pos.y,pos.z);w.setOnGround(true);placed=true;break;
                    }
                    c.assertTrue(placed,"Actual disabled callow isolated on existing supported loaded trail; no extra terrain or body");
                }
            }
            if(ws.size()<6||ws.stream().anyMatch(LasiusNigerEntity::isCallow))return;
            c.assertTrue(NestExpansion.get(c.getLevel()).job(q.getUUID())==null,"Disabled callows kept construction below mature enabled trigger while genuine brood grew");
            for(var w:ws)if(initial.contains(w.getUUID())&&!q.founding().claimedBy(w))w.setNoAi(true);else if(held.contains(w.getUUID()))w.setNoAi(false);
            c.assertTrue(ws.stream().filter(w->!w.isNoAi()&&!w.isCallow()).count()==4&&ws.stream().filter(w->w.workerTasks().nursing()&&w.isNoAi()).count()==2,"Four OTHER mature enabled workers and two disabled nursing labels");checked[0]=true;
            c.runAfterDelay(1,()->{c.assertTrue(NestExpansion.get(c.getLevel()).job(q.getUUID())==null,"Construction must defer while two enabled associated caregivers would not remain");c.assertTrue(food.total(c,q)==14,"Deferred scheduler preserves all food");c.succeed();});
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void persistedIncompatibleBuilderCargoIsExplicitlyQuarantined(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},checked={false};
        c.onEachTick(()->{
            if(checked[0])return;if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,6,8);}
            if(supplied[0]){c.assertTrue(food.total(c,q)==14,"Quarantine conserves physical/consumed food");food.yields(c,q);}soil.balance(c,q);
            var j=NestExpansion.get(c.getLevel()).job(q.getUUID());if(j==null||j.removed()<2||j.complete())return;
            var w=f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)&&!a.getMainHandItem().isEmpty()).findFirst();if(w.isEmpty())return;checked[0]=true;var b=w.get();var cargo=b.getMainHandItem().copy();int removed=j.removed(),deposited=j.deposited;UUID id=b.getUUID();
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());b.save(out);var tag=out.buildResult();tag.getCompoundOrEmpty("WorkerTask").putString("Phase","NURSE_RETURN");b.discard();
            var restored=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(restored),"Same actual builder with disclosed corrupt task restored");
            c.runAfterDelay(80,()->{var current=NestExpansion.get(c.getLevel()).job(q.getUUID());c.assertTrue(current.claim.equals(id)&&current.reason.equals("quarantined_incompatible_task_with_unsettled_soil")&&!q.founding().claimedBy(restored)&&current.removed()==removed&&current.deposited==deposited&&ItemStack.matches(cargo,restored.getMainHandItem()),"Explicit quarantine preserves exact cargo, edits and sole job claim; it never silently runs incompatible nursing");soil.balance(c,q);c.assertTrue(food.total(c,q)==14,"Quarantine never drops or refunds food");
                restored.hurtServer(c.getLevel(),restored.damageSources().generic(),1000);
                c.runAfterDelay(20,()->{var released=NestExpansion.get(c.getLevel()).job(q.getUUID());c.assertTrue(released.released==cargo.getCount()&&!id.equals(released.claim)&&restored.getMainHandItem().isEmpty(),"Normal death releases quarantined builder soil through canonical custody despite incompatible persisted task phase");soil.balance(c,q);c.assertTrue(food.total(c,q)==14,"Death preserves all physical/consumed food");c.succeed();});
            });
        });
    }
}
