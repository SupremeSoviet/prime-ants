package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;

/** Controlled physical habitat; every construction receipt and access transition must be earned by ordinary workers. */
public final class SurfaceAccessGameTest {
    private enum Mode { RELOAD, DEATH, SECOND_DEATH, SOURCE_WRITE, DESTINATION_WRITE, FOREIGN, REGRESSION, BODY, QUARANTINE }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void exactPairThreeReloadPhasesUnknownAndInterruptedActionExposeOrdinaryPaidTarget(GameTestHelper c){run(c,Mode.RELOAD);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void actualCarrierDeathRequiresExactNamedPickupAndActualRedeposition(GameTestHelper c){run(c,Mode.DEATH);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void secondActualDeathTerminatesTheSameOperationInNamedCustody(GameTestHelper c){run(c,Mode.SECOND_DEATH);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStateSourceWriteSurvivesDiskReloadWithoutRemovalOrRemintedAdmission(GameTestHelper c){run(c,Mode.SOURCE_WRITE);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStateReservedAirWriteWhileCarryingPreservesNamedCargoAfterReload(GameTestHelper c){run(c,Mode.DESTINATION_WRITE);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void foreignBlockedDestinationNeverGrantsAccessFromAppearanceOrReload(GameTestHelper c){run(c,Mode.FOREIGN);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void actualPopulationRegressionStopsThePairAndPreservesTheCarriedUnit(GameTestHelper c){run(c,Mode.REGRESSION);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void blockedWitnessedRouteStandStopsWithoutSelectingAnotherCell(GameTestHelper c){run(c,Mode.BODY);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void incompatibleCanonicalActorCargoIsQuarantinedWithoutRemintingTheUnit(GameTestHelper c){run(c,Mode.QUARANTINE);}

    static void disk(GameTestHelper c){
        var l=c.getLevel();l.getDataStorage().saveAndJoin();
        try(var disk=new SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
            l.getDataStorage().set(SurfaceWork.TYPE,Objects.requireNonNull(disk.get(SurfaceWork.TYPE)));
            l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));
            var custody=disk.get(TransferCustody.TYPE);if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
        }
    }
    static void reload(GameTestHelper c,SurfaceFixture fixture,SurfaceWork.Job j,LasiusNigerEntity w,String phase){
        var l=c.getLevel();var before=SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.get(l)).getOrThrow();var reservation=ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow();var clock=w==null?0:w.workerTasks().harvestingTicks();
        disk(c);if(w!=null)w=fixture.fx.f.restore(c,w);
        c.assertTrue(before.equals(SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.get(l)).getOrThrow())&&reservation.equals(ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow()),"Actual disk data reload preserves exact bound attempt, locations, reservations, claim, partial clock and history");
        if(w!=null)c.assertTrue(w.workerTasks().harvestingTicks()==clock,"Canonical actor reload preserves its validated partial action clock");
        PrimeAnts.LOGGER.info("T16 ACCESS RELOAD phase={} tick={} state={} actor={} actorAction={}",phase,c.getTick(),j.access.encode(),w==null?null:w.getUUID(),clock);
    }
    private static LasiusNigerEntity unavailableActor(GameTestHelper c,LasiusNigerEntity w,SurfaceAccess a){
        var l=c.getLevel();var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(w.save(out),"Normal canonical actor serialization before unavailable lookup");var tag=out.buildResult();var id=w.getUUID();w.discard();
        var restored=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),tag),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
        c.assertTrue(restored!=null&&restored.getUUID().equals(id)&&a.cargo(restored)&&l.getEntity(id)==null,"Same serialized canonical actor and unit, temporarily outside loaded lookup; no death or cargo remint");return restored;
    }
    static void ledger(GameTestHelper c,SurfaceFixture fixture,LasiusNigerEntity q,SurfaceWork.Job j,boolean revokedStage,LasiusNigerEntity unavailable){
        fixture.soil(c,q,!revokedStage,unavailable);if(j.access==null)return;var a=j.access;
        if(unavailable!=null)c.assertTrue(unavailable.getUUID().equals(j.claim)&&a.cargo(unavailable)&&a.action==0,"Exact absent actor's canonical unit/claim pause; no alternative owner is admitted");
        if(revokedStage)c.assertTrue(a.action==0&&a.removed()==1&&a.redeposited()==0&&j.completed()==4,"Observed regression grants no material action, receipt or new worker admission");
        c.assertTrue(a.operations.size()==2&&a.removed()<=2&&a.redeposited()<=2&&a.pickups()<=2&&a.removed()+a.pickups()==a.redeposited()+a.carried()+a.released(),"Separate finite access history balances without structural credit");
        if(!a.complete())c.assertTrue(j.completed()==4&&j.recovered()==2&&j.placed()==2&&j.released()==0&&j.transfers().isEmpty(),"All four receipts and two placements remain unchanged during exclusive access");
        for(int n=0;n<2;n++){var op=a.operations.get(n);int custody=op.releases.size()-op.pickups;
            c.assertTrue(custody>=0&&custody<=1,"Cumulative releases are separate from current named custody");
            if(custody==1){var id=op.custody();long pending=TransferCustody.get(c.getLevel()).contents().stream().filter(p->p.id().equals(id)&&ItemStack.matches(p.stack(),SurfaceAccess.unitStack(op.unit))).count();
                var entity=c.getLevel().getEntity(id);int ground=entity instanceof ItemEntity item&&item.isAlive()&&ItemStack.matches(item.getItem(),SurfaceAccess.unitStack(op.unit))?1:0;
                c.assertTrue(pending+ground==1,"Each released original unit has exactly one verified pending/named entity location: "+id);
            }
            if(op.redeposited&&!a.complete())c.assertTrue(ColonyTerrain.get(c.getLevel()).accessMound(c.getLevel(),op.destination,q.getUUID(),op.unit),"Named original unit actually occupies its owned destination");
            if(op.carried()==1&&j.claim!=null){var w=c.getLevel().getEntity(j.claim);if(w instanceof LasiusNigerEntity ant&&!a.stopped)c.assertTrue(a.cargo(ant),"Canonical mainhand is the same named unit");}
        }
        c.assertTrue(q.founding().removed()==0&&ChamberExcavation.get(c.getLevel()).jobs(q.getUUID()).isEmpty()&&ChamberUpgrade.get(c.getLevel()).jobs(q.getUUID()).isEmpty(),"Declared initial habitat gained no excavation/deposit/upgrade credit from access");
    }
    static void kill(GameTestHelper c,LasiusNigerEntity w,SurfaceWork.Job j){
        var unit=j.access.current().unit;int releases=j.access.released();w.hurtServer(c.getLevel(),w.damageSources().genericKill(),1000);w.workerTasks().die(c.getLevel());w.workerTasks().die(c.getLevel());
        c.assertTrue(!w.isAlive()&&w.getMainHandItem().isEmpty()&&j.claim==null&&j.access.released()==releases+1,"Actual lethal/repeated death hands the canonical unit to custody only once");
        PrimeAnts.LOGGER.info("T16 ACCESS DEATH tick={} worker={} unit={} state={}",c.getTick(),w.getUUID(),unit,j.access.encode());
    }
    private void run(GameTestHelper c,Mode mode){
        var fixture=new SurfaceFixture();var q=fixture.accessHabitat(c);fixture.snapshot(c,q);var l=c.getLevel();
        int[] step={0},confirmations={0};long[] since={-1},settled={-1};UUID[] binding={null};UnavailableCells[] hidden={null};LasiusNigerEntity[] unavailable={null};ColonyDevelopment.Evaluation[] seen={null};String[] last={""};BlockPos[] intervention={null};var otherSoil=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        c.onEachTick(()->{
            var pile=NestPlanFixture.pile(c,q);if(pile==null)return;var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null)return;
            boolean revokedStage=mode==Mode.REGRESSION&&step[0]>=1&&MoundSoil.stage(l,q.founding().plan(),q.getUUID()).ordinal()<ColonyStage.MATURE.ordinal();
            ledger(c,fixture,q,j,revokedStage,unavailable[0]);var evaluation=pile.stageEvaluation();
            if(revokedStage&&!j.access.stopped)c.assertTrue(pile.loadedTicks()-since[0]<=100,"Existing loaded controller must stop finite access promptly after actual regression");
            if(hidden[0]==null&&evaluation!=null&&evaluation!=seen[0]){seen[0]=evaluation;if(step[0]>=2&&evaluation.stage()==ColonyStage.MATURE)confirmations[0]++;}
            if(mode==Mode.FOREIGN&&step[0]==0&&j.completed()==4&&j.access==null){
                intervention[0]=q.founding().plan().at(-9,8,1);l.setBlock(intervention[0],dev.primeants.brood.NurseryBlocks.NEST_SOIL.defaultBlockState(),3);ColonyTerrain.get(l).deposited(intervention[0],UUID.randomUUID());
                since[0]=pile.loadedTicks();step[0]=1;disk(c);PrimeAnts.LOGGER.info("T16 ACCESS FOREIGN START tick={} sourceUnits=72 destination={} originalSnapshotPreserved=true",c.getTick(),intervention[0]);return;
            }
            if(mode==Mode.FOREIGN&&step[0]>0){
                c.assertTrue(j.access==null&&j.completed()==4&&j.recovered()==2&&j.claim==null,"Foreign blocked destination refuses before admission, including after disk reload");
                if(pile.loadedTicks()-since[0]<600)return;if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
                PrimeAnts.LOGGER.info("T16 ACCESS REFUSAL DONE mode={} tick={} negativeLoaded=600 settled=200 access=null",mode,c.getTick());c.succeed();return;
            }
            var a=j.access;if(a==null)return;if(binding[0]==null){binding[0]=a.binding;c.assertTrue(j.completed()==4&&a.removed()==0,"Actual production first earned two placements and two retained foundations before admission");
                var t=ColonyTerrain.get(l);for(var p:MoundSoil.cells(l,j.home,q.getUUID()))if((t.mound(l,p,q.getUUID())||t.surface(l,p,q.getUUID()))&&a.operations.stream().noneMatch(op->op.source.equals(p)))otherSoil.put(p,l.getBlockState(p));
                c.assertTrue(otherSoil.size()==70,"Exactly seventy other current owned units accompany the two named sources");
            }
            if(!a.complete())for(var entry:otherSoil.entrySet())c.assertTrue(l.getBlockState(entry.getKey()).equals(entry.getValue())&&(ColonyTerrain.get(l).mound(l,entry.getKey(),q.getUUID())||ColonyTerrain.get(l).surface(l,entry.getKey(),q.getUUID())),"Every one of the seventy other units keeps its exact physical state and ownership during exclusive access: "+entry.getKey());
            c.assertTrue(binding[0].equals(a.binding),"Reload/cancellation never remints the attempt");
            var w=j.claim!=null&&l.getEntity(j.claim) instanceof LasiusNigerEntity actor?actor:null;
            String state=a.index+":"+a.current().phase+":"+a.removed()+":"+a.redeposited()+":"+a.pickups()+":"+a.released()+":"+a.stopped;
            if(!state.equals(last[0])){last[0]=state;PrimeAnts.LOGGER.info("T16 ACCESS EVIDENCE mode={} tick={} state={} structuralReceipts={} structuralRecovered={} structuralPlaced={} physicalStock=72",mode,c.getTick(),a.encode(),j.receipts(),j.recovered(),j.placed());}
            if(mode==Mode.RELOAD){
                if(step[0]==0&&a.action>=5&&a.removed()==0&&w!=null){reload(c,fixture,j,w,"before_removal");step[0]=1;return;}
                if(step[0]==1&&a.action>=10&&a.removed()==0){hidden[0]=UnavailableCells.hide(c,a.current().source);since[0]=pile.loadedTicks();step[0]=2;return;}
                if(step[0]==2&&hidden[0]!=null){
                    c.assertTrue(a.removed()==0&&a.redeposited()==0&&a.action==0,"Interrupted partial action earns no removal or duplicate credit during 600 loaded UNKNOWN ticks");
                    if(pile.loadedTicks()-since[0]<600)return;hidden[0].close();hidden[0]=null;confirmations[0]=0;seen[0]=evaluation;step[0]=3;return;
                }
                if(step[0]==3&&a.carried()==1&&w!=null){reload(c,fixture,j,w,"carrying");w=(LasiusNigerEntity)l.getEntity(j.claim);unavailable[0]=unavailableActor(c,w,a);since[0]=pile.loadedTicks();step[0]=4;
                    PrimeAnts.LOGGER.info("T16 ACTOR UNAVAILABLE START tick={} worker={} unit={} canonicalEquipment=true lookup=null claimRetained=true",c.getTick(),j.claim,a.current().unit);return;}
                if(step[0]==4&&unavailable[0]!=null){c.assertTrue(a.removed()==1&&a.redeposited()==0&&a.carried()==1&&w==null&&a.cargo(unavailable[0]),"Unavailable actor retains the exact canonical unit and claim for 600 nursery-loaded ticks");
                    if(pile.loadedTicks()-since[0]<600)return;c.assertTrue(l.tryAddFreshEntityWithPassengers(unavailable[0]),"Same normally decoded actor becomes available again at its saved position");unavailable[0]=null;confirmations[0]=0;seen[0]=evaluation;step[0]=5;
                    PrimeAnts.LOGGER.info("T16 ACTOR UNAVAILABLE END tick={} worker={} loadedWindow=600 sameCanonicalUnit=true",c.getTick(),j.claim);return;}
                if(step[0]==5&&a.redeposited()==1){reload(c,fixture,j,w,"after_redeposition");step[0]=6;return;}
            }else if(mode==Mode.DEATH||mode==Mode.SECOND_DEATH){
                if(step[0]==0&&a.carried()==1&&w!=null){
                    hidden[0]=UnavailableCells.hide(c,w.blockPosition());since[0]=pile.loadedTicks();kill(c,w,j);disk(c);step[0]=1;return;
                }
                if(step[0]==1&&hidden[0]!=null){
                    c.assertTrue(j.claim==null&&a.pickups()==0&&a.carried()==0&&a.redeposited()==0&&a.named(l)==null&&TransferCustody.get(l).contents().stream().anyMatch(p->p.id().equals(a.current().custody())&&ItemStack.matches(p.stack(),SurfaceAccess.unitStack(a.current().unit))),"Actual saved pending custody remains the sole canonical owner for 600 loaded ticks; no matching ground shortcut");
                    if(pile.loadedTicks()-since[0]<600)return;hidden[0].close();hidden[0]=null;step[0]=2;
                    PrimeAnts.LOGGER.info("T16 PENDING CUSTODY END mode={} tick={} loadedWindow=600 state={}",mode,c.getTick(),a.encode());return;
                }
                if(mode==Mode.SECOND_DEATH&&step[0]==2&&a.pickups()==1&&a.carried()==1&&w!=null){kill(c,w,j);disk(c);since[0]=pile.loadedTicks();step[0]=3;return;}
            }else if(step[0]==0&&w!=null&&(mode==Mode.SOURCE_WRITE||mode==Mode.BODY?a.action>=5&&a.removed()==0:a.carried()==1)){
                var op=a.current();
                switch(mode){
                    case SOURCE_WRITE -> {intervention[0]=op.source;l.setBlock(op.source,l.getBlockState(op.source),3);}
                    case DESTINATION_WRITE -> {intervention[0]=op.destination;l.setBlock(op.destination,Blocks.AIR.defaultBlockState(),3);}
                    case BODY -> {intervention[0]=op.removalStand;l.setBlock(op.removalStand,Blocks.GLASS.defaultBlockState(),3);}
                    case REGRESSION -> {var keep=new HashSet<UUID>();keep.add(w.getUUID());fixture.fx.f.workers(c,q).stream().filter(ant->ant.workerTasks().caregiver(l,q.founding().plan())).limit(2).forEach(ant->keep.add(ant.getUUID()));
                        c.assertTrue(keep.size()==3,"Two actual nurses remain with the carrier before catastrophic physical regression");for(var ant:fixture.fx.f.workers(c,q))if(!keep.contains(ant.getUUID()))ant.hurtServer(l,ant.damageSources().genericKill(),1000);
                        PrimeAnts.LOGGER.info("T16 REGRESSION RETAINED queen={} tick={} retained={} aliveAdults=4 authorizedCaregivers={} admissionCareGuardUnchanged=true",q.getUUID(),c.getTick(),keep,TierTwoFixture.caregivers(c,q));}
                    case QUARANTINE -> {var cargo=w.getMainHandItem().copy();cargo.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("incompatible existing cargo"));w.setItemSlot(EquipmentSlot.MAINHAND,cargo);}
                    default -> throw new IllegalStateException();
                }reload(c,fixture,j,w,"refusal_after_intervention");since[0]=pile.loadedTicks();step[0]=1;return;
            }
            boolean positive=mode==Mode.RELOAD||mode==Mode.DEATH;
            if(positive){
                c.assertFalse(a.stopped,"The positive controlled endpoint must not hide a safely stopped physical access attempt: "+a.reason);
                if(!a.complete()||j.completed()<5||mode==Mode.RELOAD&&(step[0]<6||confirmations[0]<2))return;
                c.assertTrue(a.removed()==2&&a.redeposited()==2&&a.carried()==0&&a.released()==a.pickups()&&a.operations.stream().allMatch(op->op.transitions.stream().filter(s->s.startsWith("removed:")).count()==1&&op.transitions.stream().filter(s->s.startsWith("redeposited:")).count()==1),"Exactly two ordered physical relocations; no third action or duplicated transition");
                c.assertTrue(j.receipts().get(4).endsWith(":-4,3,0")&&j.receipts().get(4).startsWith("retained_paid_foundation:"),"Ordinary production selector reaches and retains the exposed existing paid target for twenty ticks");
                if(mode==Mode.DEATH)c.assertTrue(a.operations.getFirst().deaths==1&&a.operations.getFirst().pickups==1,"Direct named pickup precedes the same unit's actual redeposition");
                fixture.protectedTerrain(c,q,true);
                if(mode==Mode.RELOAD&&step[0]==7){
                    c.assertFalse(ColonyTerrain.get(l).accessMound(l,intervention[0],q.getUUID(),a.operations.getFirst().unit),"Later same-state destination replacement is not repaired from the saved completed flag");
                    if(pile.loadedTicks()-since[0]<600)return;
                    if(settled[0]<0){settled[0]=pile.loadedTicks();return;}
                }else if(settled[0]<0){settled[0]=pile.loadedTicks();reload(c,fixture,j,w,"completed_pair");return;}
                if(pile.loadedTicks()-settled[0]<200)return;
                if(mode==Mode.RELOAD&&step[0]==6){
                    c.assertTrue(a.operations.stream().allMatch(op->ColonyTerrain.get(l).accessMound(l,op.destination,q.getUUID(),op.unit)),"Each original unit is physically in its exact owned destination at the accepted retained-target endpoint");
                    PrimeAnts.LOGGER.info("T16 ACCESS ENDPOINT BEFORE REPLACEMENT tick={} settled=200 currentOwnedDestinations=2 state={}",c.getTick(),a.encode());
                    intervention[0]=a.operations.getFirst().destination;l.setBlock(intervention[0],l.getBlockState(intervention[0]),3);disk(c);since[0]=pile.loadedTicks();settled[0]=-1;step[0]=7;return;
                }
                if(mode==Mode.RELOAD&&step[0]==7){
                    PrimeAnts.LOGGER.info("T16 COMPLETED DESTINATION REVOCATION tick={} negativeLoaded=600 settled=200 accessActionsUnchanged=true originalUnitAuthority=revoked history={}",c.getTick(),a.encode());
                }
                PrimeAnts.LOGGER.info("T16 ACCESS DONE mode={} tick={} settled=200 physicalStock=72 ordinaryRetention=true state={}",mode,c.getTick(),a.encode());c.succeed();return;
            }
            if(!a.stopped)return;if(since[0]<0)since[0]=pile.loadedTicks();
            c.assertTrue(!a.complete()&&j.completed()==4&&a.redeposited()==0&&a.removed()<2,"Terminal refusal leaves the prefix incomplete without an alternate source or destination");
            if(mode==Mode.SECOND_DEATH)c.assertTrue(a.current().deaths==2&&a.current().pickups==1&&a.current().phase==SurfaceAccess.Phase.TERMINAL&&a.released()==2,"Second actual death terminates continuation with exactly one named current unit");
            if(mode==Mode.QUARANTINE)c.assertTrue(j.claim!=null&&l.getEntity(j.claim) instanceof LasiusNigerEntity ant&&ant.getMainHandItem().is(Items.DIRT)&&ant.getMainHandItem().getCount()==1&&a.released()==0,"Incompatible canonical cargo stays quarantined, with no extra custody stack");
            if(mode==Mode.REGRESSION)c.assertTrue(revokedStage&&a.carried()==0&&a.released()==1&&j.claim==null,"Actual catastrophic stage loss cancels the builder and preserves its exact unit in named custody");
            if(pile.loadedTicks()-since[0]<600)return;if(settled[0]<0){settled[0]=pile.loadedTicks();disk(c);return;}if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T16 ACCESS REFUSAL DONE mode={} tick={} negativeLoaded=600 settled=200 state={}",mode,c.getTick(),a.encode());c.succeed();
        });
    }
}
