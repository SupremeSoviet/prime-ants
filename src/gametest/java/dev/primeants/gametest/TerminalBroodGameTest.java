package dev.primeants.gametest;

import dev.primeants.entity.*;
import dev.primeants.brood.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

/** Birth-selected policies; all biology advances through actual server tick owners. */
public final class TerminalBroodGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private LasiusNigerEntity start(GameTestHelper c,long neglect,long waiting,long fasting){return start(c,neglect,waiting,fasting,20000);}
    private LasiusNigerEntity start(GameTestHelper c,long neglect,long waiting,long fasting,long lifespan){
        var props=Map.of("prime_ants.broodNeglectTicks",Long.toString(neglect),"prime_ants.cocoonWaitingTicks",Long.toString(waiting),"prime_ants.adultFastingTicks",Long.toString(fasting),"prime_ants.adultLifespanTicks",Long.toString(lifespan));
        var old=new HashMap<String,String>();props.keySet().forEach(k->old.put(k,System.getProperty(k)));
        try{props.forEach(System::setProperty);return f.start(c);}finally{old.forEach((k,v)->{if(v==null)System.clearProperty(k);else System.setProperty(k,v);});}
    }
    private BroodPile pile(GameTestHelper c,LasiusNigerEntity q){return q.founding().plan()!=null&&c.getLevel().getBlockEntity(q.founding().plan().nursery()) instanceof BroodPile b?b:null;}
    private CompoundTag save(GameTestHelper c,LasiusNigerEntity a){var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());c.assertTrue(a.save(o),"Normal live save");return o.buildResult();}
    private LasiusNigerEntity load(GameTestHelper c,CompoundTag tag){return (LasiusNigerEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),EntitySpawnReason.LOAD,e->e);}
    private BroodPile restore(GameTestHelper c,BroodPile b,CompoundTag tag){var pos=b.getBlockPos();var state=b.getBlockState();c.getLevel().removeBlockEntity(pos);var copy=(BroodPile)BlockEntity.loadStatic(pos,state,tag,c.getLevel().registryAccess());c.assertTrue(copy!=null,"Normal nursery restore");c.getLevel().setBlockEntity(copy);return copy;}
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void partialFailedFounderStarvesReleasesRealSoilOnce(GameTestHelper c){
        var q=start(c,600,600,400);boolean[] sabotaged={false},failed={false},finished={false};var terrain=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();int[] removed={0},carried={0};
        c.onEachTick(()->{
            if(finished[0])return;var plan=q.founding().plan();
            if(!sabotaged[0]&&plan!=null&&q.founding().phase()==QueenFounding.Phase.EXCAVATING&&q.founding().carried()>0){
                sabotaged[0]=true;var next=plan.tasks().get(q.founding().removed());c.getLevel().setBlock(next,Blocks.STONE.defaultBlockState(),3); // Explicit negative terrain intervention, never a biological edit.
            }
            if(!failed[0]&&q.founding().phase()==QueenFounding.Phase.FAILED){
                failed[0]=true;removed[0]=q.founding().removed();carried[0]=q.founding().carried();c.assertTrue(carried[0]>0&&removed[0]>0&&q.bodyReserve()==39000,"Actual partial failure retains real excavated cargo and exact unspent reserve");
                for(var p:plan.tasks())terrain.put(p,c.getLevel().getBlockState(p));
                var tag=save(c,q);var copy=load(c,tag);c.assertTrue(copy!=null&&copy.elapsedAgeTicks()==q.elapsedAgeTicks()&&copy.adultLife().fasting()==q.adultLife().fasting(),"Failed queen restoration preserves life clocks");
                c.runAfterDelay(1200,()->c.assertTrue(!q.isAlive(),"FAILED CLAUSTRAL founder must die through ordinary starvation despite historical lifecycle"));
            }
            if(failed[0]&&!q.isAlive()){
                finished[0]=true;c.assertTrue(q.founding().released()==carried[0]&&q.founding().removed()==removed[0]&&q.founding().carried()==0&&q.bodyReserve()==39000,"Death releases actual soil once; no resumed digging/refund/reserve debit");q.founding().die(c.getLevel());c.assertTrue(q.founding().released()==carried[0],"Repeated callback cannot release twice");
                c.runAfterDelay(30,()->{long soil=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum()+TransferCustody.get(c.getLevel()).contents().stream().filter(t->t.stack().is(Items.DIRT)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
                    c.assertTrue(soil==carried[0]&&terrain.entrySet().stream().allMatch(e->c.getLevel().getBlockState(e.getKey()).equals(e.getValue()))&&f.workers(c,q).isEmpty()&&c.getLevel().getEntity(q.getUUID())==null,"Real death removal, once-only physical soil and unchanged failed terrain, no replacement");var row=AdultHistory.get(c.getLevel()).records().get(q.getUUID().toString());c.assertTrue(row!=null&&row.contains("starvation"),"Persistent ordinary starvation receipt");dev.primeants.PrimeAnts.LOGGER.info("T19 FAILED FOUNDER queen={} removed={} release={} soil={} reserve={} terminal={}",q.getUUID(),removed[0],carried[0],soil,q.bodyReserve(),row);c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void queenKilledWithGenuineEggsReachesDependentExtinction(GameTestHelper c){
        var q=start(c,600,600,400);boolean[] killed={false},finished={false};var ids=new HashSet<UUID>();
        c.onEachTick(()->{
            if(finished[0])return;var b=pile(c,q);if(b==null)return;
            if(!killed[0]&&b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.EGG)){
                killed[0]=true;b.records().forEach(r->ids.add(r.id()));c.assertTrue(f.workers(c,q).isEmpty(),"Actual eggs remain at controlled queen damage");q.hurtServer(c.getLevel(),q.damageSources().genericKill(),1000);
                c.runAfterDelay(1000,()->c.assertTrue(b.records().isEmpty(),"Unsupported genuine eggs/larvae must reach terminal expiry through loaded ticks"));
            }
            if(killed[0]&&b.records().isEmpty()){
                finished[0]=true;c.assertTrue(b.consumed().isEmpty()&&ColonyMembers.get(c.getLevel()).members(q.getUUID()).isEmpty()&&f.workers(c,q).isEmpty()&&b.original().equals(ids),"Expired brood must not reconstruct fictitious adults or replacement identities");dev.primeants.PrimeAnts.LOGGER.info("T19 DEPENDENT EXTINCTION queen={} original={} livingAdults=0 viableBrood=0 reserve={}",q.getUUID(),ids,q.bodyReserve());c.succeed();
            }
        });
    }

    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void healthyShortPolicyFounderCompletesExactOriginalBudget(GameTestHelper c){
        var q=start(c,600,600,400);var ids=new HashSet<UUID>();boolean[] cocoon={false};
        c.onEachTick(()->{var b=pile(c,q);if(b==null)return;b.records().forEach(r->ids.add(r.id()));
            if(b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.COCOON)){cocoon[0]=true;c.assertTrue(q.isAlive()&&q.bodyReserve()==0&&!q.adultLife().started()&&q.adultLife().maintenanceTicks()==0,"Healthy founding stays exempt beyond shortened ordinary fasting; exactly 39000 spent once");}
            if(f.workers(c,q).size()==3){c.assertTrue(cocoon[0]&&ids.size()==3&&b.original().equals(ids)&&b.consumed().equals(ids)&&b.expired().isEmpty()&&q.bodyReserve()==0,"Original healthy clutch and costs unchanged, three legitimate workers");c.succeed();}
        });
    }
    private void neglectRestore(GameTestHelper c,boolean habitat){
        var q=start(c,600,600,400);boolean[] interrupted={false},restored={false},finished={false};long[] first={0};var ids=new HashSet<UUID>();
        c.onEachTick(()->{
            if(finished[0])return;var b=pile(c,q);if(b==null)return;
            if(!interrupted[0]&&b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.LARVA&&r.progress()>10)){
                interrupted[0]=true;first[0]=b.loadedTicks();b.records().forEach(r->ids.add(r.id()));
                if(habitat)c.getLevel().setBlock(q.founding().plan().at(5,1,-1),Blocks.STONE.defaultBlockState(),3);else q.setNoAi(true);
            }
            if(interrupted[0]&&!restored[0]&&!b.records().isEmpty()&&b.records().getFirst().neglectTicks()>=200){
                restored[0]=true;var r=b.records().getFirst();long neglected=r.neglectTicks(),progress=r.progress(),nourishment=r.nourishment();var tag=b.saveWithFullMetadata(c.getLevel().registryAccess());var copy=restore(c,b,tag);
                c.assertTrue(copy.records().stream().allMatch(v->v.neglectTicks()==neglected&&v.neglectGrace()==600&&v.progress()==progress&&v.nourishment()==nourishment),"Partway restore preserves independent timer, stalled development and invested nutrition");
                // A detached restored pile is never ticked: no offline compensation.
                var detached=(BroodPile)BlockEntity.loadStatic(b.getBlockPos(),b.getBlockState(),tag,c.getLevel().registryAccess());
                c.runAfterDelay(100,()->c.assertTrue(detached.records().getFirst().neglectTicks()==neglected&&detached.loadedTicks()==copy.loadedTicks()-100,"Detached/unloaded nursery does not advance biology"));
            }
            if(interrupted[0]&&b.records().isEmpty()){
                finished[0]=true;c.assertTrue(restored[0]&&b.loadedTicks()-first[0]==600&&b.expired().keySet().equals(ids)&&b.consumed().isEmpty()&&ColonyMembers.get(c.getLevel()).members(q.getUUID()).isEmpty(),"Exact loaded neglect bound across habitat/care failure and restore; no adult reservations");
                c.assertTrue(b.getBlockState().getValue(BroodPileBlock.A)==BroodStage.EMPTY&&b.getBlockState().getValue(BroodPileBlock.B)==BroodStage.EMPTY&&b.getBlockState().getValue(BroodPileBlock.C)==BroodStage.EMPTY,"Visible slots become empty only at expiry");
                c.runAfterDelay(1000,()->{c.assertTrue(!q.isAlive()&&pile(c,q).records().isEmpty()&&f.workers(c,q).isEmpty(),"Terminal first clutch ends surviving claustral queen exemption; no free replacement");dev.primeants.PrimeAnts.LOGGER.info("T19 NEGLECT restore habitat={} queen={} brood={} loadedExpiry={} reserve={} expired={}",habitat,q.getUUID(),ids,b.loadedTicks()-first[0],q.bodyReserve(),b.expired());c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void invalidHabitatAdvancesNeglectAcrossRestoration(GameTestHelper c){neglectRestore(c,true);}
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void lostCareAdvancesNeglectAcrossRestoration(GameTestHelper c){neglectRestore(c,false);}
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void supportedLarvaAndRefusedCocoonSurviveBriefInterruptions(GameTestHelper c){
        var q=start(c,600,600,400);boolean[] interrupted={false},resumed={false},blocked={false},released={false};var ids=new HashSet<UUID>();long[] reserve={0};
        c.onEachTick(()->{var b=pile(c,q);if(b==null)return;
            if(!interrupted[0]&&b.records().stream().anyMatch(r->r.stage()==BroodStage.LARVA&&r.progress()>10)){
                interrupted[0]=true;b.records().forEach(r->ids.add(r.id()));q.setNoAi(true);reserve[0]=q.bodyReserve();
                c.runAfterDelay(40,()->{var now=pile(c,q);c.assertTrue(now.records().stream().allMatch(r->r.neglectTicks()==40)&&q.bodyReserve()==reserve[0],"Brief genuine lost care consumes grace, no progress or nourishment");var copy=restore(c,now,now.saveWithFullMetadata(c.getLevel().registryAccess()));c.assertTrue(copy.records().getFirst().neglectTicks()==40,"Restore does not renew grace");q.setNoAi(false);resumed[0]=true;});
            }
            if(resumed[0]&&!blocked[0]&&b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.COCOON)){
                blocked[0]=true;EmergenceFault.block(q.getUUID());
            }
            if(blocked[0]&&!released[0]&&b.records().getFirst().waitingTicks()>=40){
                released[0]=true;long waiting=b.records().getFirst().waitingTicks();var copy=restore(c,b,b.saveWithFullMetadata(c.getLevel().registryAccess()));c.assertTrue(copy.records().stream().allMatch(r->r.waitingTicks()==waiting)&&copy.expired().isEmpty(),"Brief real insertion refusal retains viable nourished cocoons and exact waiting timer");EmergenceFault.release(q.getUUID());
            }
            if(released[0]&&f.workers(c,q).size()==3){c.assertTrue(b.records().isEmpty()&&b.consumed().equals(ids)&&b.expired().isEmpty()&&q.bodyReserve()==0,"Interrupted larva/cocoon finishes legitimately with original IDs and exact 39000 cost");c.succeed();}
        });
    }
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void prolongedRefusedCocoonExpiresAtPersistedBound(GameTestHelper c){
        var q=start(c,600,600,400);boolean[] blocked={false},restored={false},finished={false};var ids=new HashSet<UUID>();
        c.onEachTick(()->{if(finished[0])return;var b=pile(c,q);if(b==null)return;
            if(!blocked[0]&&b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.COCOON)){blocked[0]=true;b.records().forEach(r->ids.add(r.id()));EmergenceFault.block(q.getUUID());}
            if(blocked[0]&&!restored[0]&&!b.records().isEmpty()&&b.records().getFirst().waitingTicks()>=200){restored[0]=true;long wait=b.records().getFirst().waitingTicks();var copy=restore(c,b,b.saveWithFullMetadata(c.getLevel().registryAccess()));c.assertTrue(copy.records().stream().allMatch(r->r.waitingTicks()==wait&&r.waitingBound()==600),"Cocoon retry bound survives real restoration");}
            if(blocked[0]&&b.records().isEmpty()){
                finished[0]=true;EmergenceFault.release(q.getUUID());c.assertTrue(restored[0]&&b.expired().keySet().equals(ids)&&b.consumed().isEmpty()&&f.workers(c,q).isEmpty()&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0&&q.bodyReserve()==0,"Prolonged refused viability is finite; funded 39000 lost, no phantom adult/refund");
                c.assertTrue(b.expired().values().stream().allMatch(r->r.contains("cocoon_wait_worker_insertion_failed")&&com.google.gson.JsonParser.parseString(r).getAsJsonObject().get("waitingTicks").getAsLong()==600),"Declared exact terminal cocoon bound/reason persisted");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void confirmedMissingLoadedNurseryEndsQueenExemption(GameTestHelper c){
        var q=start(c,600,600,400);boolean[] missing={false},finished={false};
        c.onEachTick(()->{if(finished[0])return;var b=pile(c,q);
            if(!missing[0]&&b!=null&&b.records().size()==3){missing[0]=true;var pos=b.getBlockPos();c.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);c.runAfterDelay(1000,()->c.assertTrue(!q.isAlive(),"Confirmed missing loaded claimed nursery cannot exempt queen indefinitely"));}
            if(missing[0])c.assertTrue(pile(c,q)==null,"Claimed nursery missing does not create a replacement clutch");
            if(missing[0]&&!q.isAlive()){finished[0]=true;c.assertTrue(q.bodyReserve()==36000&&f.workers(c,q).isEmpty(),"Missing nursery uses normal starvation, no double debit/refill");c.succeed();}
        });
    }
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void legacyFailedQueenPreservesAgeAndFastingAcrossRepeatedRestore(GameTestHelper c){
        LasiusNigerEntity[] q={start(c,600,600,400)};boolean[] changed={false},restored={false};
        c.onEachTick(()->{var a=q[0];var p=a.founding().plan();
            if(!changed[0]&&p!=null&&a.founding().carried()>0){changed[0]=true;c.getLevel().setBlock(p.tasks().get(a.founding().removed()),Blocks.STONE.defaultBlockState(),3);}
            if(!restored[0]&&a.founding().phase()==QueenFounding.Phase.FAILED&&a.adultLife().fasting()>=60){
                restored[0]=true;long age=a.elapsedAgeTicks(),fast=a.adultLife().fasting(),active=a.adultLife().activeTicks(),maint=a.adultLife().maintenanceTicks();var tag=save(c,a);tag.getCompound("AdultLife").orElseThrow().remove("Started");tag.remove("BroodNeglectGrace");tag.remove("CocoonWaitingBound"); // Only absent schema fields, no clock/health/reserve edits.
                var copy=load(c,tag);c.assertTrue(copy!=null&&copy.elapsedAgeTicks()==age&&copy.adultLife().fasting()==fast&&copy.adultLife().maintenanceTicks()==maint&&copy.broodNeglectGrace()==24000,"Legacy failed migration preserves age/fasting/maintenance and stable production brood policy");a.discard();c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(copy),"Original failed identity reinserted");q[0]=copy;
                c.runAfterDelay(30,()->{var now=q[0];c.assertTrue(now.adultLife().fasting()==fast+30&&now.adultLife().activeTicks()==active+30&&now.bodyReserve()==39000,"Legacy CLAUSTRAL failure immediately adopts ordinary persisted fasting without reserve use");var again=load(c,save(c,now));c.assertTrue(again!=null&&again.elapsedAgeTicks()==now.elapsedAgeTicks()&&again.adultLife().fasting()==now.adultLife().fasting()&&again.adultLife().started(),"Repeated restoration cannot renew policy or clocks");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void queenDiesWithUnderfedLarvaeAndAdultsThenColonyExpires(GameTestHelper c){
        var q=start(c,800,800,3500,8000);boolean[] supplied={false},killed={false},finished={false},legacy={false};var deadBrood=new HashSet<UUID>();var adults=new HashSet<UUID>();long[] laying={0};
        c.onEachTick(()->{if(finished[0])return;var b=pile(c,q);if(b==null)return;var ws=f.workers(c,q);ws.forEach(w->adults.add(w.getUUID()));
            if(!supplied[0]&&ws.size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.drop(c,q.founding().plan().at(-3,0,1),new ItemStack(Items.APPLE,6));f.drop(c,q.founding().plan().at(-4,0,1),new ItemStack(Items.CHICKEN));}
            if(supplied[0]&&!legacy[0])legacy[0]=PaidLegacyBrood.restoreEggs(c,q,b,3);
            if(!killed[0]&&b.records().stream().anyMatch(r->!r.founding()&&r.stage()==BroodStage.LARVA&&r.nourishment()<BroodPile.LARVA_COST)){
                killed[0]=true;laying[0]=b.lastLayingTick();b.records().forEach(r->deadBrood.add(r.id()));c.assertTrue(ws.size()==3&&q.nutrition().chickens()==1,"Physically paid legacy egg restoration leaves underfed dependent brood with three genuine living adults");q.hurtServer(c.getLevel(),q.damageSources().genericKill(),1000);
                dev.primeants.PrimeAnts.LOGGER.info("T19 QUEEN KILLED WITH DEPENDENTS queen={} workers={} brood={} receiptsQueen={} receiptsBrood={}",q.getUUID(),adults,deadBrood,q.nutrition().consumedUnits(),b.consumedFood());
            }
            if(killed[0])c.assertTrue(b.lastLayingTick()==laying[0]&&b.records().stream().allMatch(r->deadBrood.contains(r.id())),"Dead queen cannot lay or create replacement brood");
            if(killed[0]&&b.records().isEmpty()&&ws.isEmpty()){
                finished[0]=true;c.assertTrue(b.expired().keySet().containsAll(deadBrood)&&b.consumed().equals(b.original())&&adults.size()==3&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0&&adults.stream().allMatch(id->ColonyMembers.get(c.getLevel()).member(id).dead()),"Three real adults die normally and all unsupported dependents expire, zero viable brood/adults");
                c.runAfterDelay(30,()->{c.assertTrue(adults.stream().allMatch(id->c.getLevel().getEntity(id)==null)&&c.getLevel().getEntity(q.getUUID())==null,"Ordinary terminal body removal");dev.primeants.PrimeAnts.LOGGER.info("T19 UNDERFED EXTINCTION queen={} adults={} dependent={} expired={} adultHistory={}",q.getUUID(),adults,deadBrood,b.expired(),AdultHistory.get(c.getLevel()).records());c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void expiredStaleRecordsStayTerminalAndFreedSlotsRequireFundedNewIdentity(GameTestHelper c){
        var q=start(c,800,800,24000);boolean[] supplied={false},snapshot={false},restored={false},funded={false},finished={false},legacy={false};CompoundTag[] stale={null};var ids=new HashSet<UUID>();long[] receipt={0},paidBeforeFunding={0};
        c.onEachTick(()->{if(finished[0])return;var b=pile(c,q);if(b==null)return;
            if(!supplied[0]&&f.workers(c,q).size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.drop(c,q.founding().plan().at(-3,0,1),new ItemStack(Items.APPLE,4));f.drop(c,q.founding().plan().at(-4,0,1),new ItemStack(Items.CHICKEN));}
            if(supplied[0]&&!legacy[0])legacy[0]=PaidLegacyBrood.restoreEggs(c,q,b,3);
            if(!snapshot[0]&&b.records().size()==3&&b.records().stream().allMatch(r->!r.founding()&&r.stage()==BroodStage.LARVA)&&b.records().stream().anyMatch(r->r.nutrition().consumedUnits()>0)){
                snapshot[0]=true;b.records().forEach(r->ids.add(r.id()));stale[0]=b.saveWithFullMetadata(c.getLevel().registryAccess());q.setNoAi(true); // Declared care/laying interruption isolates the three stale identities.
            }
            if(snapshot[0]&&!restored[0]&&b.records().isEmpty()){
                restored[0]=true;q.setNoAi(true);c.assertTrue(b.expired().keySet().equals(ids)&&b.consumed().equals(b.original()),"Actual expiry releases only brood reservations and preserves emergence set");receipt[0]=b.consumedFood();restore(c,b,stale[0]);
                c.runAfterDelay(2,()->{var now=pile(c,q);c.assertTrue(now.records().isEmpty()&&now.expired().keySet().equals(ids)&&now.consumedFood()==receipt[0]&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3,"Stale older saved live records reconcile against terminal identity/nutrition history without resurrection, phantom adults or duplicate/lost ingestion");restore(c,now,now.saveWithFullMetadata(c.getLevel().registryAccess()));
                    c.runAfterDelay(2,()->{c.assertTrue(pile(c,q).consumedFood()==receipt[0]&&pile(c,q).records().isEmpty(),"Repeated terminal restore is once-only");paidBeforeFunding[0]=q.nutrition().spentProtein();q.setNoAi(false);f.drop(c,q.founding().plan().at(-3,0,1),new ItemStack(Items.APPLE,4));f.drop(c,q.founding().plan().at(-4,0,1),new ItemStack(Items.CHICKEN,3));funded[0]=true;});
                });
            }
            if(funded[0]&&q.nutrition().chickens()>1&&!b.records().isEmpty()){
                finished[0]=true;c.assertTrue(b.records().stream().allMatch(r->!ids.contains(r.id())&&!b.original().contains(r.id())&&!r.founding())&&q.nutrition().spentProtein()>=paidBeforeFunding[0]+Nutrition.EGG_PROTEIN&&b.expired().keySet().containsAll(ids)&&b.records().stream().noneMatch(r->b.expired().containsKey(r.id()))&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3,"Freed capacity needs new physical nursing, real egg debit and new identity; expired identities/investment never refunded");dev.primeants.PrimeAnts.LOGGER.info("T19 TERMINAL RESTORE queen={} expired={} new={} broodReceipts={} queenProteinSpent={}",q.getUUID(),ids,b.records().stream().map(BroodRecord::id).toList(),b.consumedFood(),q.nutrition().spentProtein());c.succeed();
            }
        });
    }

    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void invalidCocoonHabitatExpiresAtSeparateWaitingBound(GameTestHelper c){
        var q=start(c,600,700,400);boolean[] invalid={false},finished={false};var ids=new HashSet<UUID>();long[] first={0};
        c.onEachTick(()->{if(finished[0])return;var b=pile(c,q);if(b==null)return;
            if(!invalid[0]&&b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.COCOON)){invalid[0]=true;first[0]=b.loadedTicks();b.records().forEach(r->ids.add(r.id()));c.getLevel().setBlock(q.founding().plan().at(5,1,-1),Blocks.STONE.defaultBlockState(),3);}
            if(invalid[0]&&b.records().isEmpty()){finished[0]=true;c.assertTrue(b.loadedTicks()-first[0]==700&&b.expired().keySet().equals(ids)&&b.expired().values().stream().allMatch(r->com.google.gson.JsonParser.parseString(r).getAsJsonObject().get("waitingTicks").getAsLong()==700)&&b.consumed().isEmpty()&&f.workers(c,q).isEmpty(),"Invalid habitat advances independent cocoon loaded waiting bound, not frozen emergence or neglect duration");c.succeed();}
        });
    }
    @GameTest(maxTicks=10000,structure="prime_ants_test:idle_ground")
    public void absentQueenLookupKeepsIdentityAndUnloadedQueenClocks(GameTestHelper c){
        LasiusNigerEntity[] q={start(c,600,600,400)};boolean[] removed={false},resumed={false};var ids=new HashSet<UUID>();
        c.onEachTick(()->{var a=q[0];var b=pile(c,a);if(b==null)return;
            if(!removed[0]&&b.records().size()==3&&b.records().stream().allMatch(r->r.stage()==BroodStage.EGG)){
                removed[0]=true;b.records().forEach(r->ids.add(r.id()));var tag=save(c,a);long age=a.elapsedAgeTicks(),reserve=a.bodyReserve();a.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                c.runAfterDelay(200,()->{var now=pile(c,a);c.assertTrue(now.records().size()==3&&now.expired().isEmpty()&&now.records().stream().allMatch(r->r.neglectTicks()==200)&&!AdultHistory.get(c.getLevel()).records().containsKey(a.getUUID().toString()),"Absent queen lookup advances loaded brood neglect but is no proof of queen death or replacement permission");var copy=load(c,tag);c.assertTrue(copy!=null&&copy.elapsedAgeTicks()==age&&copy.bodyReserve()==reserve&&copy.getUUID().equals(a.getUUID())&&c.getLevel().tryAddFreshEntityWithPassengers(copy),"Only same unloaded original queen returns with exact paused clocks and reserve");q[0]=copy;resumed[0]=true;});
            }
            if(resumed[0]&&f.workers(c,q[0]).size()==3){c.assertTrue(b.records().isEmpty()&&b.consumed().equals(ids)&&b.expired().isEmpty()&&q[0].bodyReserve()==0,"Actual resumed care saves original eggs and exact investment, no replacement actors");c.succeed();}
        });
    }

    @GameTest(maxTicks=22000,structure="prime_ants_test:idle_ground")
    public void physicallyNourishedLarvaCompletesAfterQueenDeath(GameTestHelper c){
        var q=start(c,2400,800,24000);boolean[] supplied={false},killed={false};UUID[] supported={null};
        c.onEachTick(()->{var b=pile(c,q);if(b==null)return;
            if(!supplied[0]&&f.workers(c,q).size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.drop(c,q.founding().plan().at(-3,0,1),new ItemStack(Items.APPLE,6));f.drop(c,q.founding().plan().at(-4,0,1),new ItemStack(Items.CHICKEN,4));}
            if(!killed[0])for(var r:b.records())if(!r.founding()&&r.stage()==BroodStage.LARVA&&r.progress()>0&&r.progress()<b.stageDuration()&&r.nutrition().gainedSugar()==Nutrition.LARVA_SUGAR&&r.nutrition().gainedProtein()==Nutrition.LARVA_PROTEIN){
                killed[0]=true;supported[0]=r.id();c.assertTrue(r.nutrition().consumedUnits()==2&&r.nourishment()>0&&r.nourishment()<BroodPile.LARVA_COST,"Genuine physically fed, still developing larva remains at explicit controlled queen death");q.hurtServer(c.getLevel(),q.damageSources().genericKill(),1000);break;
            }
            if(killed[0]&&f.workers(c,q).stream().anyMatch(w->supported[0].equals(w.broodId()))){
                c.assertTrue(!q.isAlive()&&b.consumed().contains(supported[0])&&!b.expired().containsKey(supported[0])&&ColonyMembers.get(c.getLevel()).member(ColonyMembers.workerId(supported[0]))!=null,"Stored real larval nourishment remains viable after queen death and yields a legitimate unique worker");dev.primeants.PrimeAnts.LOGGER.info("T19 NOURISHED LARVA POST-DEATH queen={} brood={} worker={} receipts={}",q.getUUID(),supported[0],ColonyMembers.workerId(supported[0]),b.consumedFood());c.succeed();
            }
        });
    }

    @GameTest(maxTicks=100,structure="prime_ants_test:idle_ground")
    public void replayClosureUsesFrozenSnapshotAfterChunkIoCloses(GameTestHelper c){
        var destination=java.nio.file.Path.of(System.getProperty("fabric-api.gametest.report-file")).toAbsolutePath().getParent().resolve("t19-closure-probe-"+UUID.randomUUID());
        new PlacementReplayExperiment().installClosureProbe(c.getLevel(),destination);
        dev.primeants.PrimeAnts.LOGGER.info("T19 CLOSURE PROBE destination={}",destination);
        c.runAfterDelay(2,c::succeed); // Actual SERVER_STOPPING then SERVER_STOPPED execute when this server exits.
    }
}
