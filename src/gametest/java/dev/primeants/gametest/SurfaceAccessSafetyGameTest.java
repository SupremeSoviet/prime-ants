package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** T17 physical safety: real deaths, ordinary same-state writes and normal disk/actor reconciliation. */
public final class SurfaceAccessSafetyGameTest {
    private enum Mode { SOURCE_DEATH, PICKUP_DEATH, SOURCE_REVOKE, CARRIED_REVOKE, PICKUP_REVOKE, TARGET_UNKNOWN }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void emptyHandedDeathAfterFiveRemovalTicksLoadsWithoutNewAdmission(GameTestHelper c){run(c,Mode.SOURCE_DEATH);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void emptyReplacementDeathAfterFiveNamedTicksKeepsOnePickupAndLoads(GameTestHelper c){run(c,Mode.PICKUP_DEATH);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStatePaidTargetWriteDuringPartialRemovalCancelsAcrossDisk(GameTestHelper c){run(c,Mode.SOURCE_REVOKE);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStatePaidTargetWriteWhileCarryingCancelsWithExactCustody(GameTestHelper c){run(c,Mode.CARRIED_REVOKE);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStatePaidTargetWriteDuringNamedPickupCancelsWithoutAcquisition(GameTestHelper c){run(c,Mode.PICKUP_REVOKE);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void unavailablePaidTargetResetsPartialActionThenRequiresTwoFreshConfirmations(GameTestHelper c){run(c,Mode.TARGET_UNKNOWN);}

    private static void normalDisk(GameTestHelper c){
        // A short GameTest's ordinary shutdown may only queue entity autosaves. Flush the normal level save now.
        c.getLevel().save(null,true,false);
        SurfaceAccessGameTest.disk(c);
        PrimeAnts.LOGGER.info("T17 NORMAL LEVEL SAVE AND NEW STORAGE READ tick={} entityAndChunkFlush=true",c.getTick());
    }
    private static void namedDisk(GameTestHelper c,SurfaceWork.Job j){
        var l=c.getLevel();
        for(var op:j.access.operations)if(op.releases.size()-op.pickups==1){
            var id=op.custody();c.assertTrue(l.getEntity(id) instanceof ItemEntity,"Original named ground entity remains available for normal saved-entity read");
            var item=(ItemEntity)l.getEntity(id);var folder=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("entities");
            var chunk=new net.minecraft.world.level.ChunkPos(item.blockPosition().getX()>>4,item.blockPosition().getZ()>>4);int found=0;
            try(var storage=new net.minecraft.world.level.chunk.storage.RegionFileStorage(new net.minecraft.world.level.chunk.storage.RegionStorageInfo("T17-read-only",l.dimension(),"entities"),folder,true)){
                var record=storage.read(chunk);c.assertTrue(record!=null,"Normal flushed entity region exists before GameTest success cleanup");
                for(var entry:record.getListOrEmpty("Entities"))if(entry instanceof net.minecraft.nbt.CompoundTag tag&&net.minecraft.core.UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,tag.get("UUID")).getOrThrow().equals(id)){
                    found++;var stack=ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,tag.getCompoundOrEmpty(tag.contains("Item")?"Item":"item")).getOrThrow();
                    c.assertTrue(ItemStack.matches(stack,SurfaceAccess.unitStack(op.unit)),"Actual disk named UUID owns exactly the unchanged original unit, without a second release or pickup");
                    PrimeAnts.LOGGER.info("T17 NAMED ENTITY DISK VERIFIED tick={} binding={} unit={} custody={} chunk={} savedEntity={}",c.getTick(),j.access.binding,op.unit,id,chunk,tag);
                }
            }catch(java.io.IOException error){throw new IllegalStateException("Normal named entity disk read failed",error);}
            c.assertTrue(found==1,"Exactly one original named UUID in its actual saved entity chunk");
        }
    }
    private static void emptyDeath(GameTestHelper c,LasiusNigerEntity w,SurfaceWork.Job j){
        var a=j.access;var op=a.current();var before=op.transitions.toString();int deaths=op.deaths;
        c.assertTrue(w.getMainHandItem().isEmpty()&&a.carried()==0&&a.action==5&&w.workerTasks().harvestingTicks()==5,"Five actual consecutive loaded empty-handed action ticks before lethal damage");
        long last=a.lastActionTime;var id=w.getUUID();var custody=op.custody();
        PrimeAnts.LOGGER.info("T17 EMPTY DEATH BEFORE tick={} gameTime={} lastAction={} worker={} binding={} unit={} custody={} state={}",c.getTick(),c.getLevel().getGameTime(),last,id,a.binding,op.unit,custody,a.encode());
        w.hurtServer(c.getLevel(),w.damageSources().genericKill(),1000);w.workerTasks().die(c.getLevel());w.workerTasks().die(c.getLevel());
        c.assertTrue(!w.isAlive()&&w.getMainHandItem().isEmpty()&&j.claim==null&&op.transitions.toString().equals(before)&&op.deaths==deaths&&Objects.equals(custody,op.custody()),"Repeated empty-handed death adds no removal, pickup, release or actual carrier-death credit");
        PrimeAnts.LOGGER.info("T17 EMPTY DEATH AFTER tick={} dead={} actorAction={} state={}",c.getTick(),id,w.workerTasks().harvestingTicks(),a.encode());
        // Deliberately read a new SavedDataStorage before checking the reset: baseline failure is a real unloadable save.
        try {normalDisk(c);namedDisk(c,j);} catch(RuntimeException error){PrimeAnts.LOGGER.error("T17 DISK LOAD FAILURE after empty death binding={} worker={}",a.binding,id,error);throw error;}
        var restored=SurfaceWork.get(c.getLevel()).job(a.owner,ColonyStage.MATURE);
        c.assertTrue(restored!=null&&restored.claim==null&&restored.access.action==0&&restored.access.lastActionTime==Long.MIN_VALUE&&w.workerTasks().harvestingTicks()==0,"Normal saved job loads with no dead claim or inherited partial action clock");
        c.assertTrue(restored.access.binding.equals(a.binding)&&restored.access.current().unit.equals(op.unit)&&Objects.equals(restored.access.current().custody(),custody),"Disk read retains the original attempt/unit/custody identity");
        PrimeAnts.LOGGER.info("T17 EMPTY DEATH DISK LOADABLE tick={} dead={} state={}",c.getTick(),id,restored.access.encode());
    }
    private static void reconcileSurvivors(GameTestHelper c,SurfaceFixture f,LasiusNigerEntity q){
        var job=SurfaceWork.get(c.getLevel()).job(q.getUUID(),ColonyStage.MATURE);
        var observedActor=job!=null&&job.claim!=null&&c.getLevel().getEntity(job.claim) instanceof LasiusNigerEntity w?w:null;
        var baseline=observedActor==null?null:PersistenceDiagnostics.state(c,observedActor);
        if(baseline!=null)PersistenceDiagnostics.observe(c,"T17_survivors:before_reconcile",null,baseline);
        for(var actor:new ArrayList<>(f.fx.f.workers(c,q)))f.fx.f.restore(c,actor);
        if(baseline!=null)PersistenceDiagnostics.observe(c,"T17_survivors:actors_reconstructed",baseline,PersistenceDiagnostics.state(c,observedActor));
        SurfaceWork.get(c.getLevel()).reconcile(c.getLevel(),q);
        if(baseline!=null)PersistenceDiagnostics.observe(c,"T17_survivors:ordinary_reconciled",baseline,PersistenceDiagnostics.state(c,observedActor));
        PrimeAnts.LOGGER.info("T17 SURVIVING ACTORS RECONCILED tick={} queen={} living={} claim={}",c.getTick(),q.getUUID(),f.fx.f.workers(c,q).size(),SurfaceWork.get(c.getLevel()).job(q.getUUID(),ColonyStage.MATURE).claim);
    }
    private void run(GameTestHelper c,Mode mode){
        var f=new SurfaceFixture();var q=f.accessHabitat(c);f.snapshot(c,q);var l=c.getLevel();
        int[] step={0},confirmations={0};long[] since={-1},settled={-1},resumed={-1};UUID[] binding={null},unit={null},custody={null},dead={null};
        UnavailableCells[] hidden={null};ColonyDevelopment.Evaluation[] seen={null};var other=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();String[] events={null};
        c.onEachTick(()->{
            var pile=NestPlanFixture.pile(c,q);if(pile==null)return;var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null||j.access==null)return;
            var a=j.access;var target=j.at(4);var evaluation=pile.stageEvaluation();
            SurfaceAccessGameTest.ledger(c,f,q,j,false,null);
            boolean revoke=mode==Mode.SOURCE_REVOKE||mode==Mode.CARRIED_REVOKE||mode==Mode.PICKUP_REVOKE;
            if(binding[0]==null){binding[0]=a.binding;unit[0]=a.current().unit;var terrain=ColonyTerrain.get(l);
                for(var p:MoundSoil.cells(l,j.home,q.getUUID()))if((terrain.mound(l,p,q.getUUID())||terrain.surface(l,p,q.getUUID()))&&!p.equals(target)&&a.operations.stream().noneMatch(op->op.source.equals(p)))other.put(p,l.getBlockState(p));
                c.assertTrue(other.size()==69&&terrain.mound(l,target,q.getUUID()),"Two exact sources, one motivating paid target, and sixty-nine other positively owned physical units");
            }
            c.assertTrue(binding[0].equals(a.binding)&&unit[0].equals(a.operations.getFirst().unit),"Original binding and unit never remint");
            if(!a.complete())for(var e:other.entrySet())c.assertTrue(l.getBlockState(e.getKey()).equals(e.getValue())&&(ColonyTerrain.get(l).mound(l,e.getKey(),q.getUUID())||ColonyTerrain.get(l).surface(l,e.getKey(),q.getUUID())),"Unaffected unit retains its physical state and positive ownership: "+e.getKey());
            var w=j.claim!=null&&l.getEntity(j.claim) instanceof LasiusNigerEntity actor?actor:null;
            if(mode==Mode.SOURCE_DEATH&&step[0]==0&&a.action==5&&a.removed()==0&&w!=null){
                dead[0]=w.getUUID();emptyDeath(c,w,j);reconcileSurvivors(c,f,q);since[0]=pile.loadedTicks();step[0]=2;return;
            }
            if((mode==Mode.PICKUP_DEATH||mode==Mode.PICKUP_REVOKE)&&step[0]==0&&a.carried()==1&&w!=null){
                SurfaceAccessGameTest.kill(c,w,j);custody[0]=a.current().custody();SurfaceAccessGameTest.disk(c);step[0]=1;return;
            }
            if(mode==Mode.PICKUP_DEATH&&step[0]==1&&a.current().phase==SurfaceAccess.Phase.CUSTODY&&a.action==5&&w!=null){
                dead[0]=w.getUUID();emptyDeath(c,w,j);reconcileSurvivors(c,f,q);step[0]=2;resumed[0]=l.getGameTime();return;
            }
            boolean source=a.current().phase==SurfaceAccess.Phase.SOURCE&&a.action==5&&w!=null;
            boolean carried=a.current().phase==SurfaceAccess.Phase.CARRIED&&w!=null;
            boolean pickup=a.current().phase==SurfaceAccess.Phase.CUSTODY&&a.action==5&&w!=null;
            if(revoke&&step[0]<2&&(mode==Mode.SOURCE_REVOKE&&source||mode==Mode.CARRIED_REVOKE&&carried||mode==Mode.PICKUP_REVOKE&&step[0]==1&&pickup)){
                var block=l.getBlockState(target);var before=a.encode();events[0]=a.current().transitions.toString();
                c.assertTrue(ColonyTerrain.get(l).mound(l,target,q.getUUID()),"Target is positively owned immediately before ordinary same-state write");
                l.setBlock(target,block,3);
                c.assertTrue(l.getBlockState(target).equals(block)&&!ColonyTerrain.get(l).mound(l,target,q.getUUID())&&!ColonyTerrain.get(l).surface(l,target,q.getUUID()),"Same-state write preserves the physical target and geometry but revokes ownership");
                PrimeAnts.LOGGER.info("T17 TARGET WRITE mode={} tick={} gameTime={} target={} sameState=true physicalPresent=true positivelyOwned=false worker={} binding={} state={}",mode,c.getTick(),l.getGameTime(),target,w.getUUID(),a.binding,before);
                c.assertTrue("motivating_target_ownership_revoked".equals(a.problem(l,j))&&"motivating_target_ownership_revoked".equals(a.actionProblem(l,j,w)),"Active guard and worker action refuse motivating-target ownership loss immediately");
                c.assertFalse(a.commit(l,j,w),"Final material commit is refused immediately after the write");
                SurfaceAccessGameTest.reload(c,f,j,w,"T17_target_write_"+mode);reconcileSurvivors(c,f,q);since[0]=pile.loadedTicks();step[0]=2;return;
            }
            if(mode==Mode.TARGET_UNKNOWN){
                if(step[0]==0&&source){hidden[0]=UnavailableCells.hide(c,target);since[0]=pile.loadedTicks();step[0]=1;
                    PrimeAnts.LOGGER.info("T17 TARGET UNKNOWN START tick={} loaded={} state={}",c.getTick(),pile.loadedTicks(),a.encode());return;}
                if(step[0]==1){
                    c.assertTrue(!a.stopped&&a.removed()==0&&a.pickups()==0&&a.redeposited()==0&&a.action==0&&j.claim!=null&&w!=null&&w.getMainHandItem().isEmpty(),"UNKNOWN target pauses, resets partial ticks and retains the canonical empty actor without ownership loss");
                    if(pile.loadedTicks()-since[0]<600)return;
                    SurfaceAccessGameTest.reload(c,f,j,w,"T17_unknown_target_600");hidden[0].close();hidden[0]=null;confirmations[0]=0;seen[0]=evaluation;step[0]=2;
                    PrimeAnts.LOGGER.info("T17 TARGET UNKNOWN END tick={} observedLoaded=600 state={}",c.getTick(),a.encode());return;
                }
                if(step[0]==2){
                    if(evaluation!=null&&evaluation!=seen[0]){seen[0]=evaluation;if(evaluation.stage()==ColonyStage.MATURE)confirmations[0]++;}
                    if(confirmations[0]<2)c.assertTrue(a.action==0&&a.removed()==0,"Two fresh production confirmations are required before restored continuation");
                    if(confirmations[0]>=2){step[0]=3;resumed[0]=l.getGameTime();PrimeAnts.LOGGER.info("T17 TARGET RESTORED CONFIRMED tick={} confirmations={} state={}",c.getTick(),confirmations[0],a.encode());}
                }
            }
            if(revoke&&step[0]==2){
                c.assertTrue(a.current().transitions.stream().noneMatch(e->e.startsWith("redeposited:")||e.startsWith("named_pickup:"))&&a.removed()==(mode==Mode.SOURCE_REVOKE?0:1)&&j.completed()==4,"No subsequent removal/pickup/redeposit or structural receipt after known revocation");
                c.assertTrue(!ColonyTerrain.get(l).mound(l,target,q.getUUID())&&!ColonyTerrain.get(l).surface(l,target,q.getUUID())&&l.getBlockState(target).is(dev.primeants.brood.NurseryBlocks.NEST_SOIL),"Target remains physical but never counts as positively owned after reload");
                if(!a.stopped){c.assertTrue(pile.loadedTicks()-since[0]<=100,"Known target loss cancels within one hundred nursery-loaded ticks");return;}
                c.assertTrue(j.claim==null&&a.action==0,"Persistently stopped attempt releases its empty claim and partial work");
                if(mode==Mode.PICKUP_REVOKE)c.assertTrue(a.current().deaths==1&&a.pickups()==0&&a.released()==1&&Objects.equals(custody[0],a.current().custody()),"Named custody UUID/unit and carrier-death count remain exactly the original values");
                if(mode==Mode.CARRIED_REVOKE)c.assertTrue(a.current().deaths==0&&a.released()==1&&a.carried()==0,"Carried original unit enters exact named custody without carrier-death credit");
            }
            if(mode==Mode.SOURCE_DEATH&&step[0]==2){
                c.assertTrue(a.stopped&&a.removed()==0&&a.pickups()==0&&a.redeposited()==0&&a.released()==0&&a.current().deaths==0&&j.claim==null&&a.action==0&&ColonyTerrain.get(l).mound(l,a.current().source,q.getUUID()),"Empty source death safely stops this bound attempt without any cargo event, remint or source loss");
            }
            boolean positive=mode==Mode.PICKUP_DEATH||mode==Mode.TARGET_UNKNOWN;
            if(positive){
                c.assertFalse(a.stopped,"Positive recovery must physically continue within existing authority: "+a.reason);
                if(mode==Mode.PICKUP_DEATH&&step[0]>=2){c.assertTrue(!Objects.equals(j.claim,dead[0])&&a.current().deaths<=1&&a.released()==1,"Dead replacement never restores as claimant or earns second carrier-death/release credit");
                    if(a.pickups()==0&&w!=null)c.assertTrue(a.action==w.workerTasks().harvestingTicks(),"Replacement starts an independent consecutive action clock");}
                if(!a.complete()||j.completed()<5||mode==Mode.TARGET_UNKNOWN&&step[0]<3)return;
                c.assertTrue(a.removed()==2&&a.redeposited()==2&&a.carried()==0&&a.operations.stream().allMatch(op->op.transitions.stream().filter(e->e.startsWith("removed:")||e.startsWith("named_pickup:")||e.startsWith("redeposited:")).allMatch(e->e.endsWith(":20"))),"Every actual material action earns twenty fresh consecutive loaded ticks");
                if(mode==Mode.PICKUP_DEATH){var op=a.operations.getFirst();c.assertTrue(op.deaths==1&&op.pickups==1&&Objects.equals(op.custody(),custody[0])&&ColonyTerrain.get(l).accessMound(l,op.destination,q.getUUID(),unit[0]),"Exact original named unit physically picked once and redeposited, with one actual carrier death only");
                    var event=op.transitions.stream().filter(e->e.startsWith("named_pickup:")).findFirst().orElseThrow();c.assertTrue(Long.parseLong(event.split(":")[2])-resumed[0]>=20,"Empty replacement's old five ticks cannot contribute to the subsequent pickup");}
                if(mode==Mode.TARGET_UNKNOWN)c.assertTrue(confirmations[0]>=2&&resumed[0]>=0,"Two fresh production evaluations preceded restored continuation");
            }else if(step[0]!=2||!a.stopped||pile.loadedTicks()-since[0]<600)return;
            f.protectedTerrain(c,q,true);
            if(settled[0]<0){settled[0]=pile.loadedTicks();normalDisk(c);namedDisk(c,j);reconcileSurvivors(c,f,q);return;}
            if(pile.loadedTicks()-settled[0]<200)return;
            normalDisk(c);
            namedDisk(c,j);
            PrimeAnts.LOGGER.info("T17 SAFETY DONE mode={} tick={} binding={} dead={} custody={} refusalLoaded={} settled=200 physicalStock=72 targetOwned={} structuralReceipts={} state={}",mode,c.getTick(),a.binding,dead[0],custody[0],positive?0:pile.loadedTicks()-since[0],ColonyTerrain.get(l).mound(l,target,q.getUUID())||ColonyTerrain.get(l).surface(l,target,q.getUUID()),j.receipts(),a.encode());c.succeed();
        });
    }
}
