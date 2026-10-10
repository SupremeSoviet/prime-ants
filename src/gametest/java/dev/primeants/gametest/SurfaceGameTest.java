package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.LevelResource;

/** T12 frozen physical acceptance: ordinary workers, finite declared sources, fixed total bounds. */
public final class SurfaceGameTest {
    /** Read real forager motion through all three columns after construction, without moving an actor. */
    private static final class Walking {
        final Map<UUID,Set<Integer>> visited=new HashMap<>();UUID walker;
        boolean observe(GameTestHelper c,LasiusNigerEntity q){
            var home=q.founding().plan();for(var w:new WorkerForagingGameTest().workers(c,q))if(q.founding().claimedBy(w)&&w.onGround()){
                for(int f:new int[]{-2,-1,0})if(w.blockPosition().equals(home.at(f,0,f==0?0:1)))visited.computeIfAbsent(w.getUUID(),k->new HashSet<>()).add(f);
                if(visited.getOrDefault(w.getUUID(),Set.of()).size()==3){walker=w.getUUID();return true;}
            }return walker!=null;
        }
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void controlledMatureHabitatCompletesEveryPaidConstructionPrefixAndWalkingPassage(GameTestHelper c){
        var fixture=new SurfaceFixture();var q=fixture.habitat(c,false);fixture.snapshot(c,q);var walking=new Walking();long[] settled={-1};boolean[] reloaded={false};
        c.onEachTick(()->{
            var l=c.getLevel();fixture.soil(c,q);fixture.protectedTerrain(c,q,settled[0]>=0);var p=NestPlanFixture.pile(c,q);if(p==null)return;
            var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null)return;
            if(!reloaded[0]&&j.completed()==14&&j.carried()==1&&j.claim!=null){
                var worker=(LasiusNigerEntity)l.getEntity(j.claim);c.assertTrue(worker!=null&&worker.workerTasks().surfaceWorking(),"Actual hauling worker at the fourteen-cell T12 paid prefix");
                var receipts=j.receipts();var claim=j.claim;var source=j.source;var cargo=worker.getMainHandItem().copy();
                var encoded=SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.get(l)).getOrThrow();
                l.getDataStorage().saveAndJoin();
                try(var disk=new SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    l.getDataStorage().set(SurfaceWork.TYPE,Objects.requireNonNull(disk.get(SurfaceWork.TYPE)));l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));
                }
                var loaded=fixture.fx.f.restore(c,worker);var after=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);
                c.assertTrue(encoded.equals(SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.get(l)).getOrThrow())&&after.receipts().equals(receipts)&&after.claim.equals(claim)&&Objects.equals(after.source,source)
                    &&ItemStack.matches(cargo,loaded.getMainHandItem())&&after.carried()==1,"Disk data and canonical actor reload retain every paid key, receipt identity, claim, source and exact unit without reassignment");
                reloaded[0]=true;fixture.soil(c,q);PrimeAnts.LOGGER.info("T13 PAID PREFIX DISK RELOAD queen={} tick={} prefix={} worker={} source={} cargo={} recovered={} placed={} released={} planOrderUnchanged=true",q.getUUID(),c.getTick(),receipts,claim,source,cargo,after.recovered(),after.placed(),after.released());return;
            }
            if(!j.complete()||j.claim!=null)return;SurfaceFixture.completed(c,q,ColonyStage.MATURE);
            c.assertTrue(reloaded[0],"Paid-prefix hauling disk/data and canonical actor reload was physically exercised");
            c.assertTrue(p.stageEvaluation()!=null&&p.stageEvaluation().stage()==ColonyStage.MATURE,"Computed Mature from real registered adults and physical tier-two functions");
            if(!walking.observe(c,q))return;if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            c.assertTrue(j.placed()==19&&j.recovered()==19&&j.released()==0,"Nineteen actual new paid placements conserve the finite source");
            PrimeAnts.LOGGER.info("T13 CONTROLLED MATURE DONE queen={} tick={} uniqueCells=19 placements={} recovered={} released={} settled={} walker={}",q.getUUID(),c.getTick(),j.placed(),j.recovered(),j.released(),p.loadedTicks()-settled[0],walking.walker);c.succeed();
        });
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void aClaimedBuilderWalksOutOfItsFutureCellAndPaysTheSameTaskWithoutDiscardingBodiesOrCargo(GameTestHelper c){
        var fixture=new SurfaceFixture();var q=fixture.habitat(c,false,true);fixture.snapshot(c,q);var probe=SurfaceOccupancyProbe.watch(c,q.getUUID());UUID[] observed={null};long[] settled={-1};BlockPos[] target={null};
        c.onEachTick(()->{
            var l=c.getLevel();fixture.soil(c,q);fixture.protectedTerrain(c,q,settled[0]>=0);var pile=NestPlanFixture.pile(c,q);if(pile==null)return;
            var job=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(job==null)return;
            if(observed[0]==null&&probe.first()!=null){
                var body=probe.first();observed[0]=body.worker();target[0]=body.target();
                c.assertTrue(body.cargo()==0&&body.recovered()==0&&body.placed()==0,"Future-cell intersection precedes source recovery and payment");
                c.assertTrue(body.source().equals(q.founding().plan().at(-6,-5,1)),"The declared first diagonal owned source actually supplies the reproduced approach");
                PrimeAnts.LOGGER.info("T13 SELF BODY START queen={} tick={} worker={} target={} position={} bounds={} source={} existingClaim=true paid=0",q.getUUID(),c.getTick(),observed[0],target[0],body.position(),body.bounds(),body.source());
            }
            if(job.completed()==0)return;
            c.assertTrue(observed[0]!=null,"The actual claimed ordinary worker crossed its future structural cell before placement");
            var actor=l.getEntity(observed[0]);c.assertTrue(actor instanceof LasiusNigerEntity worker&&worker.isAlive(),"The obstructing worker stays alive with its original identity");
            c.assertTrue(job.receipts().getFirst().equals("relocated:"+observed[0]+":-1,-1,0")&&job.placed()>=1&&job.recovered()>=1&&job.released()==0
                &&ColonyTerrain.get(l).surface(l,target[0],q.getUUID())&&l.getBlockState(target[0]).is(NurseryBlocks.NEST_SOIL),"The same claimed body physically clears and pays the same first task, without a fabricated receipt or released unit");
            if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T13 SELF BODY DONE queen={} tick={} worker={} firstTarget={} placed={} recovered={} released=0 soil=80 settled={} ordinaryMovement=true",q.getUUID(),c.getTick(),observed[0],target[0],job.placed(),job.recovered(),pile.loadedTicks()-settled[0]);c.succeed();
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:surface_ground")
    public void suppliedFoundingToMatureRetainsItsSmallMoundAndWorkersBuildLargerCrestsAndAnOpenArch(GameTestHelper c){
        var fixture=new SurfaceFixture();var player=new TierTwoFixture(40,true);var q=fixture.founder(c);
        long[] settled={-1},mature={-1};int[] youngPeak={-1};boolean[] snapshot={false};BlockPos[] protectedPlayer={null};var walking=new Walking();
        c.onEachTick(()->{
            var l=c.getLevel();player.feed(c,q);player.ledgers(c,q);var home=q.founding().plan();if(home==null)return;
            if(protectedPlayer[0]==null){protectedPlayer[0]=home.at(-8,7,1);l.setBlock(protectedPlayer[0],Blocks.GLASS.defaultBlockState(),3);}
            c.assertTrue(l.getBlockState(protectedPlayer[0]).is(Blocks.GLASS),"Player glass outside structural components is preserved");
            var p=NestPlanFixture.pile(c,q);if(p==null)return;var e=p.stageEvaluation();
            if(e!=null&&e.stage()==ColonyStage.YOUNG&&q.founding().ready()){
                var units=MoundGameTest.mound(c,q);if(!units.isEmpty())youngPeak[0]=Math.max(youngPeak[0],units.stream().mapToInt(at->at.getY()-home.entrance().getY()).max().orElseThrow());
            }
            if(e!=null&&e.stage()==ColonyStage.MATURE&&mature[0]<0){mature[0]=c.getTick();PrimeAnts.LOGGER.info("T12 REAL MATURE queen={} tick={} inputs={} youngActualPeak={} supplies={}/{}/{}",q.getUUID(),c.getTick(),e.inputs(),youngPeak[0],player.apples,player.chickens,player.clay);}
            var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null)return;
            if(!snapshot[0]){snapshot[0]=true;fixture.snapshot(c,q);}
            fixture.protectedTerrain(c,q,settled[0]>=0);
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T12 MATURE CURVE queen={} tick={} completed={}/{} recovered={} placed={} cargo={} claim={} reason={}",q.getUUID(),c.getTick(),j.completed(),j.plan.cost(),j.recovered(),j.placed(),j.carried(),j.claim,j.reason);
            if(!j.complete()||j.claim!=null)return;SurfaceFixture.completed(c,q,ColonyStage.MATURE);
            c.assertTrue(youngPeak[0]>0&&youngPeak[0]<=3&&j.cells().stream().mapToInt(at->at.getY()-home.entrance().getY()).max().orElseThrow()==4,"Completed physical Mature silhouette exceeds the observed small Young mound");
            c.assertTrue(mature[0]>=0&&e.stage()==ColonyStage.MATURE,"Real production Mature, never a stage assignment");
            if(!walking.observe(c,q))return;
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T12 MATURE DONE queen={} tick={} matureTick={} youngActualPeak={} actualCrestHeight=4 uniqueCells={} recovered={} relocated={} retainedPaid={} released={} settled={} walker={} excavationDepositsUnchanged=true supplies={}/{}/{}",q.getUUID(),c.getTick(),mature[0],youngPeak[0],j.plan.cost(),j.recovered(),j.placed(),j.plan.cost()-j.placed(),j.released(),p.loadedTicks()-settled[0],walking.walker,player.apples,player.chickens,player.clay);c.succeed();
        });
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void computedGreatHabitatBuildsPaidGateConnectedRampartsAndTwoRaisedWatchPosts(GameTestHelper c){
        var fixture=new SurfaceFixture();var q=fixture.habitat(c,true);fixture.snapshot(c,q);long[] settled={-1};boolean[] computed={false};var walking=new Walking();
        c.onEachTick(()->{
            var l=c.getLevel();fixture.soil(c,q);fixture.protectedTerrain(c,q,settled[0]>=0);
            c.assertTrue(fixture.fx.food.total(c,q)==59,"Declared finite controlled food: fifty ingested apples, one ingested chicken, four cached apples/four chickens remain exactly accounted");
            var material=MaterialStore.owned(l,q.getUUID(),q.founding().plan());c.assertTrue(material!=null&&material.units(MaterialUnits.Material.STONE)==32,"Surface work consumes none of the physical tier-two stone supply");
            var p=NestPlanFixture.pile(c,q);if(p==null)return;var e=p.stageEvaluation();if(e==null)return;
            if(e.result().certain()==ColonyStage.GREAT&&!computed[0]){computed[0]=true;PrimeAnts.LOGGER.info("T12 GREAT COMPUTED queen={} tick={} inputs={} physicalTierTwo={} initialSetupNotConstruction=true",q.getUUID(),c.getTick(),e.inputs(),e.chambers());}
            var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.GREAT);if(j==null)return;c.assertTrue(computed[0]&&e.stage()==ColonyStage.GREAT,"Production computes Great from living bodies, current functions and stock");
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T12 GREAT CURVE queen={} tick={} completed={}/{} recovered={} placed={} cargo={} claim={} reason={}",q.getUUID(),c.getTick(),j.completed(),j.plan.cost(),j.recovered(),j.placed(),j.carried(),j.claim,j.reason);
            if(!j.complete()||j.claim!=null)return;SurfaceFixture.completed(c,q,ColonyStage.GREAT);if(!walking.observe(c,q))return;
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            var mature=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);
            c.assertTrue(mature!=null&&mature.placed()==19&&j.placed()==38&&j.recovered()==38&&mature.recovered()==19&&j.released()==0&&mature.released()==0,"Fifty-seven newly placed worker-paid blocks; two gate conversions keep their original paid soil unit");
            PrimeAnts.LOGGER.info("T12 GREAT DONE queen={} tick={} uniqueCells=57 newWorkerPlacements=57 gateConversions=2 soil=80 stone=32 actualCrestHeight=4 actualPostHeight=5 posts=2 rampartCells=8 settled={} walker={} currentStage={} initialSetupNotConstruction=true",q.getUUID(),c.getTick(),p.loadedTicks()-settled[0],walking.walker,e.stage());c.succeed();
        });
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void unavailableTargetPreservesSourcesThenFreshConfirmationsHaulingDiskReloadAndWorkerDeathConserveEveryUnit(GameTestHelper c){
        var fixture=new SurfaceFixture();LasiusNigerEntity[] queen={fixture.habitat(c,false)};fixture.snapshot(c,queen[0]);
        int[] step={0},evals={0},reloadedRecovery={0};long[] since={-1},settled={-1};UnavailableCells[] hidden={null};ColonyDevelopment.Evaluation[] seen={null};UUID[] reloadWorker={null},deadWorker={null},transfer={null};boolean[] replacement={false};var walking=new Walking();
        var playerBlock=queen[0].founding().plan().at(-8,7,1);c.getLevel().setBlock(playerBlock,Blocks.GLASS.defaultBlockState(),3);fixture.snapshot(c,queen[0]);
        c.onEachTick(()->{
            var q=queen[0];var l=c.getLevel();fixture.soil(c,q);fixture.protectedTerrain(c,q,settled[0]>=0);
            c.assertTrue(l.getBlockState(playerBlock).is(Blocks.GLASS),"Player block and support preserved through unknown/reload/death");
            c.assertTrue(fixture.fx.food.total(c,q)==39,"Thirty declared initial ingested apples, one chicken, and eight cached units conserved through real worker death");
            var p=NestPlanFixture.pile(c,q);if(p==null)return;var e=p.stageEvaluation();var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null)return;
            var worker=j.claim==null?null:fixture.fx.f.workers(c,q).stream().filter(w->w.getUUID().equals(j.claim)).findFirst().orElse(null);
            if(e!=null&&e!=seen[0]){seen[0]=e;if(step[0]==2&&e.stage()==ColonyStage.MATURE&&e.inputs().tier(ChamberFunction.NURSERY).known()==2)evals[0]++;}
            if(step[0]==0){
                c.assertTrue(j.completed()==0&&j.recovered()==0&&j.placed()==0,"Unavailable intervention precedes every surface recovery/placement");
                hidden[0]=UnavailableCells.hide(c,j.next());since[0]=p.loadedTicks();step[0]=1;PrimeAnts.LOGGER.info("T12 SURFACE UNKNOWN START queen={} tick={} loaded={} target={} soil=80 recovered=0 placed=0",q.getUUID(),c.getTick(),since[0],j.next());return;
            }
            if(step[0]==1){
                c.assertTrue(j.completed()==0&&j.recovered()==0&&j.placed()==0&&j.carried()==0,"Six hundred consecutive loaded ticks preserve exact source/cargo balance with no placement");
                if(p.loadedTicks()-since[0]<600)return;hidden[0].close();hidden[0]=null;step[0]=2;evals[0]=0;seen[0]=e;
                PrimeAnts.LOGGER.info("T12 SURFACE UNKNOWN END queen={} tick={} loadedWindow={} soil=80",q.getUUID(),c.getTick(),p.loadedTicks()-since[0]);return;
            }
            if(step[0]==2){
                if(evals[0]<2||j.carried()!=1||worker==null||worker.workerTasks().phase()!=WorkerTasks.Phase.SURFACE_BUILD)return;
                var cargo=worker.getMainHandItem().copy();var encoded=SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.get(l)).getOrThrow();var receipts=j.receipts();var source=j.source;var claim=j.claim;
                l.getDataStorage().saveAndJoin();
                try(var disk=new SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var surface=Objects.requireNonNull(disk.get(SurfaceWork.TYPE));c.assertTrue(SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,surface).getOrThrow().equals(encoded),"Actual disk reload preserves descriptive plan, bound terrain, paid receipts, source and worker claim");l.getDataStorage().set(SurfaceWork.TYPE,surface);
                    l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));l.getDataStorage().set(NaturalSoil.TYPE,Objects.requireNonNull(disk.get(NaturalSoil.TYPE)));
                    l.getDataStorage().set(ChamberRegistry.TYPE,Objects.requireNonNull(disk.get(ChamberRegistry.TYPE)));l.getDataStorage().set(ColonyMembers.TYPE,Objects.requireNonNull(disk.get(ColonyMembers.TYPE)));
                    var custody=disk.get(TransferCustody.TYPE);if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
                }
                var loaded=fixture.fx.f.restore(c,worker);queen[0]=fixture.fx.f.restore(c,q);var after=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);
                c.assertTrue(loaded.getUUID().equals(claim)&&after.claim.equals(claim)&&after.receipts().equals(receipts)&&Objects.equals(after.source,source)&&ItemStack.matches(loaded.getMainHandItem(),cargo)&&after.carried()==1,"The same real hauling identity and canonical one-unit cargo reload with exact receipts");
                reloadWorker[0]=claim;reloadedRecovery[0]=after.recovered();step[0]=3;PrimeAnts.LOGGER.info("T12 SURFACE DISK RELOAD queen={} tick={} worker={} restoredConfirmations={} source={} recovered={} placed={} receipts={}",q.getUUID(),c.getTick(),claim,evals[0],source,after.recovered(),after.placed(),after.receipts());return;
            }
            if(step[0]==3){
                if(j.recovered()<=reloadedRecovery[0]||j.carried()!=1||worker==null)return;
                c.assertTrue(j.placed()>=reloadedRecovery[0],"Reload resumes an actual paid placement before the subsequent death");deadWorker[0]=worker.getUUID();transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+worker.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                worker.hurtServer(l,worker.damageSources().genericKill(),1000);worker.workerTasks().die(l);
                c.assertTrue(!worker.isAlive()&&worker.getMainHandItem().isEmpty()&&j.claim==null&&j.released()==1&&j.transfers().size()==1&&j.transfers().get(transfer[0].toString())==1,"Actual lethal hit and repeated death release the canonical surface unit exactly once");
                PrimeAnts.LOGGER.info("T12 SURFACE DEATH queen={} tick={} worker={} transfer={} recovered={} placed={} released={} reloadedWorker={}",q.getUUID(),c.getTick(),deadWorker[0],transfer[0],j.recovered(),j.placed(),j.released(),reloadWorker[0]);step[0]=4;return;
            }
            if(transfer[0]!=null){
                var entity=l.getEntity(transfer[0]);long pending=TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])&&t.stack().is(Items.DIRT)&&t.stack().getCount()==1).count();
                c.assertTrue(pending+(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&item.isAlive()&&item.getItem().is(Items.DIRT)&&item.getItem().getCount()==1?1:0)==1&&j.released()==1,"One named released surface unit exists in custody or as its actual entity; never duplicated or counted as another excavation receipt");
                if(worker!=null&&j.carried()==1){c.assertTrue(!worker.getUUID().equals(deadWorker[0])&&worker.workerTasks().surfaceAuthorized(l),"A different real ordinary builder resumes physical soil hauling");replacement[0]=true;}
            }
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T12 PROTECTION CURVE queen={} tick={} completed={}/{} recovered={} placed={} released={} cargo={} reason={}",q.getUUID(),c.getTick(),j.completed(),j.plan.cost(),j.recovered(),j.placed(),j.released(),j.carried(),j.reason);
            if(!j.complete()||j.claim!=null)return;SurfaceFixture.completed(c,q,ColonyStage.MATURE);c.assertTrue(replacement[0],"Replacement worker performs actual hauling after the reloaded worker's death");if(!walking.observe(c,q))return;
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T12 SURFACE PROTECTION DONE queen={} tick={} unavailableLoaded=600 restoredConfirmations={} actualDiskReload=true actualDeath=true transfer={} replacement=true uniqueCells=19 soil=80 recovered={} placed={} released={} settled={} walker={}",q.getUUID(),c.getTick(),evals[0],transfer[0],j.recovered(),j.placed(),j.released(),p.loadedTicks()-settled[0],walking.walker);c.succeed();
        });
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStatePlayerWriteAndDiskReloadLeaveAnEssentialComponentHonestlyIncomplete(GameTestHelper c){
        var fixture=new SurfaceFixture();var q=fixture.habitat(c,false);fixture.snapshot(c,q);BlockPos[] target={null};long[] since={-1},settled={-1};boolean[] reload={false};int[] evals={0};ColonyDevelopment.Evaluation[] seen={null};
        c.onEachTick(()->{
            var l=c.getLevel();fixture.soil(c,q);fixture.protectedTerrain(c,q,settled[0]>=0);var p=NestPlanFixture.pile(c,q);if(p==null)return;
            var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null)return;var e=p.stageEvaluation();
            if(target[0]==null){
                c.assertTrue(j.recovered()==0&&j.completed()==0&&SurfaceWork.targetProblem(l,q.getUUID(),j)==null&&ColonyTerrain.get(l).surfacePermission(l,j.next(),q.getUUID()),"A declared ready essential air target has positive plan permission before replacement");
                target[0]=j.next();l.setBlock(target[0],l.getBlockState(target[0]),3);
                c.assertFalse(ColonyTerrain.get(l).surfacePermission(l,target[0],q.getUUID()),"Even air-to-air player writing revokes placement permission");since[0]=p.loadedTicks();seen[0]=e;
                PrimeAnts.LOGGER.info("T12 SURFACE SAME STATE START queen={} tick={} target={} soil=80",q.getUUID(),c.getTick(),target[0]);return;
            }
            c.assertTrue(j.recovered()==0&&j.placed()==0&&j.completed()==0&&j.carried()==0&&!j.complete()&&!ColonyTerrain.get(l).surfacePermission(l,target[0],q.getUUID())&&l.getBlockState(target[0]).isAir(),"A loaded replaced essential component remains honestly incomplete, with no source recovery or free placement");
            c.assertTrue(e!=null&&e.stage()==ColonyStage.MATURE&&q.founding().ready()&&TierTwoFixture.caregivers(c,q)>=2,"Negative observation remains a physically ready supplied Mature habitat");
            if(e!=seen[0]){seen[0]=e;evals[0]++;}
            if(!reload[0]&&p.loadedTicks()-since[0]>=300){
                l.getDataStorage().saveAndJoin();
                try(var disk=new SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    l.getDataStorage().set(SurfaceWork.TYPE,Objects.requireNonNull(disk.get(SurfaceWork.TYPE)));l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));
                }reload[0]=true;c.assertFalse(ColonyTerrain.get(l).surfacePermission(l,target[0],q.getUUID()),"Actual disk reload cannot reauthorize a same-state player write");
            }
            if(p.loadedTicks()-since[0]<600)return;c.assertTrue(reload[0]&&evals[0]>=2&&j.claim==null,"Two fresh confirmations and a full 600 loaded negative ticks release the empty builder, with no forged completion");
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T12 SURFACE SAME STATE DONE queen={} tick={} negativeLoaded=600 settled={} evaluations={} sourceUnits=80 recovered=0 placed=0 complete=false claim=null diskReload=true",q.getUUID(),c.getTick(),p.loadedTicks()-settled[0],evals[0]);c.succeed();
        });
    }
}
