package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

/** Successive synchronous queries must never share live authority, even within the same server tick. */
public final class ValidationIndexGameTest {
    @GameTest(maxTicks=6000)
    public void realDeliveryRechecksOwnershipAfterReadBoundaryWrite(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var cp=NaturalPlacementGameTest.prepare(c,l,7500,ColonyStage.MATURE);
        ReadBoundaryWrite[] scope={null};
        ((dev.primeants.gametest.mixin.GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new net.minecraft.gametest.framework.GameTestListener(){
            public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo i){}
            public void testPassed(net.minecraft.gametest.framework.GameTestInfo i,net.minecraft.gametest.framework.GameTestRunner r){if(scope[0]!=null)scope[0].close();}
            public void testFailed(net.minecraft.gametest.framework.GameTestInfo i,net.minecraft.gametest.framework.GameTestRunner r){if(scope[0]!=null)scope[0].close();}
            public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo i,net.minecraft.gametest.framework.GameTestInfo copy,net.minecraft.gametest.framework.GameTestRunner r){if(scope[0]!=null)scope[0].close();}
        });
        c.onEachTick(()->{
            if(!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;
            var q=EstablishedGenerationGameTest.queen(l,cp);var p=q.founding().plan();
            if(scope[0]==null){
                if(!q.founding().ready()||MaterialStore.confirmed(l,q.getUUID(),p)==null)return;
                var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLAY_BALL,1);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("T11 read-boundary clay"));
                var v=net.minecraft.world.phys.Vec3.atBottomCenterOf(p.at(-4,0,1));var item=new net.minecraft.world.entity.item.ItemEntity(l,v.x,v.y+.1,v.z,stack,0,0,0);item.setDefaultPickUpDelay();c.assertTrue(l.addFreshEntity(item),"One finite ordinary named clay source");
                scope[0]=ReadBoundaryWrite.arm(l,p.at(3,0,-1),q);return;
            }
            var a=scope[0];if(a.carrier==null)return;
            c.assertTrue(a.cargo.getCount()==1&&a.cargo.is(net.minecraft.world.item.Items.CLAY_BALL),"Actual normal forager carried the exact finite material");
            c.assertTrue(net.minecraft.world.item.ItemStack.matches(a.cargo,a.carrier.getMainHandItem())&&a.store.size()==a.stock&&q.founding().claimedBy(a.carrier),"Revocation during final live read refuses delivery, retaining exact cargo and real claim");
            c.assertTrue(Boolean.FALSE.equals(a.lookupAfterWrite),"The lookup that read across the real write must itself observe revoked ownership");
            c.assertTrue(!a.carrier.workerTasks().authorized(l),"Current damaged habitat refuses further service authorization");
            PrimeAnts.LOGGER.info("T11 READ BOUNDARY ACCEPTED actor={} cargo={} stock={}",a.carrier.getUUID(),a.cargo,a.stock);c.succeed();
        });
    }
    private static void refused(GameTestHelper c,ServerLevel l,LasiusNigerEntity q,LasiusNigerEntity nurse,NestCache cache,NestPlan p){
        var cargo=nurse.getMainHandItem().copy();long receipts=q.nutrition().consumedUnits();int stock=cache.size();
        c.assertTrue(!nurse.workerTasks().nursingAuthorized(l)&&!cache.withdraw(nurse,p)&&!q.feedBy(nurse,p),"Unavailable or damaged live habitat refuses nursing authorization, physical withdrawal and feeding");
        c.assertTrue(stock==cache.size()&&receipts==q.nutrition().consumedUnits()&&net.minecraft.world.item.ItemStack.matches(cargo,nurse.getMainHandItem()),"Refusal preserves physical stock, exact cargo and ingestion receipts");
    }
    @GameTest(maxTicks=2000)
    public void sameTickAvailabilityWritesJobsMarkersAndSavedDataReplacementRevalidateActions(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var cp=NaturalPlacementGameTest.prepare(c,l,7200,ColonyStage.YOUNG);
        boolean[] checked={false};long[] settled={-1};
        c.onEachTick(()->{
            if(!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;
            var q=EstablishedGenerationGameTest.queen(l,cp);var p=q.founding().plan();var owner=q.getUUID();var pile=(BroodPile)l.getBlockEntity(p.nursery());
            if(!q.founding().ready()||p.nurseryFindings(l,owner,true).verdict()!=Findings.Verdict.CLEAR)return;
            if(checked[0]){
                if(pile.loadedTicks()-settled[0]<200)return;
                var cell=p.at(3,0,-1);var data=ColonyTerrain.get(l);c.assertTrue(data.generatedOpen(l,cell,owner),"Live own generated opening before same-state write");
                l.setBlock(cell,l.getBlockState(cell),3);
                c.assertTrue(!data.generatedOpen(l,cell,owner)&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Same-state write immediately revokes indexed authority");
                var saved=ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow();l.getDataStorage().set(ColonyTerrain.TYPE,ColonyTerrain.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow());
                c.assertTrue(!ColonyTerrain.get(l).generatedOpen(l,cell,owner)&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Replacement rebuilds from the current revoked ledger, never old index authority");
                PrimeAnts.LOGGER.info("T11 SAME TICK INDEX ACCEPTED queen={} restoredLoadedTicks={} sameStateRevoked=true",owner,pile.loadedTicks()-settled[0]);c.succeed();return;
            }
            var job=NestExpansion.get(l).job(owner);if(job==null||job.removed()==0)return;
            var nurse=ColonyMembers.get(l).members(owner).stream().map(m->l.getEntity(m.worker())).filter(a->a instanceof LasiusNigerEntity w&&w.workerTasks().nursingAuthorized(l)).map(a->(LasiusNigerEntity)a).findFirst().orElse(null);if(nurse==null)return;
            var cache=(NestCache)l.getBlockEntity(p.cache());long tick=l.getGameTime();
            var contaminated=p.nurseryFindings(l,owner,true);contaminated.fault("caller_contamination");c.assertTrue(p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.CLEAR,"Findings results remain independent and caller-owned");
            try(var hidden=UnavailableCells.hide(c,l,p.entrance())){
                c.assertTrue(p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.UNKNOWN,"Previously valid habitat immediately becomes UNKNOWN");refused(c,l,q,nurse,cache,p);
                var shell=p.undergroundSurfaces().stream().filter(a->l.getBlockState(a).isSolidRender()&&!a.equals(p.entrance())).findFirst().orElseThrow();var state=l.getBlockState(shell);l.setBlock(shell,Blocks.AIR.defaultBlockState(),3);
                c.assertTrue(p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Loaded shell damage wins beside unavailable route terrain");refused(c,l,q,nurse,cache,p);
                l.setBlock(shell,state,3);c.assertTrue(p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.UNKNOWN,"Repairing loaded shell restores UNKNOWN without inventing availability");
            }
            c.assertTrue(nurse.workerTasks().nursingAuthorized(l),"Normal live confirmation resumes immediately after availability restoration");
            var jobs=NestExpansion.CODEC.encodeStart(JsonOps.INSTANCE,NestExpansion.get(l)).getOrThrow();var opening=job.completed().getFirst();c.assertTrue(DigJob.anyOpening(l,opening,owner),"Current completion index authorizes its actual owned opening");
            l.getDataStorage().set(NestExpansion.TYPE,new NestExpansion());c.assertTrue(!DigJob.anyOpening(l,opening,owner)&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Removing the live job immediately removes its opening exception");refused(c,l,q,nurse,cache,p);
            l.getDataStorage().set(NestExpansion.TYPE,NestExpansion.CODEC.parse(JsonOps.INSTANCE,jobs).getOrThrow());c.assertTrue(DigJob.anyOpening(l,opening,owner)&&nurse.workerTasks().nursingAuthorized(l),"Decoded completion prefix rebuilds membership and normal action authorization");
            var tag=cache.saveWithFullMetadata(l.registryAccess());var foreign=tag.copy();foreign.putString("Colony",UUID.randomUUID().toString());
            var wrong=BlockEntity.loadStatic(p.cache(),l.getBlockState(p.cache()),foreign,l.registryAccess());l.setBlockEntity(Objects.requireNonNull(wrong));
            c.assertTrue(ColonyDevelopment.presence(l,owner,p,ChamberRegistry.FOUNDING,ChamberFunction.FOOD_STORE)==ColonyDevelopment.Presence.ABSENT&&!((NestCache)wrong).withdraw(nurse,p),"Same-tick foreign marker replacement rejects function and physical withdrawal");
            l.removeBlockEntity(p.cache());c.assertTrue(ColonyDevelopment.presence(l,owner,p,ChamberRegistry.FOUNDING,ChamberFunction.FOOD_STORE)==ColonyDevelopment.Presence.ABSENT,"Missing marker is observed absent in the same tick");
            l.setBlockEntity(Objects.requireNonNull(BlockEntity.loadStatic(p.cache(),l.getBlockState(p.cache()),tag,l.registryAccess())));
            var terrain=ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow();var registry=ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,ChamberRegistry.get(l)).getOrThrow();
            l.getDataStorage().set(ColonyTerrain.TYPE,ColonyTerrain.CODEC.parse(JsonOps.INSTANCE,terrain).getOrThrow());l.getDataStorage().set(ChamberRegistry.TYPE,ChamberRegistry.CODEC.parse(JsonOps.INSTANCE,registry).getOrThrow());
            c.assertTrue(nurse.workerTasks().nursingAuthorized(l)&&ColonyDevelopment.presence(l,owner,p,ChamberRegistry.FOUNDING,ChamberFunction.FOOD_STORE)==ColonyDevelopment.Presence.CONFIRMED,"Normal decoded current marker, registry and terrain confirm after replacement");
            var other=NaturalPlacementGameTest.level(c,"placement_stone");c.assertTrue(!ColonyTerrain.get(other).generatedOpen(other,p.at(3,0,-1),owner)&&!ColonyTerrain.get(l).generatedOpen(l,p.at(3,0,-1).offset(100,0,0),owner),"No authority crosses dimensions or homes");
            c.assertTrue(l.getGameTime()==tick,"Every intervention and query above occurred within one server tick");checked[0]=true;settled[0]=pile.loadedTicks();
        });
    }
    @GameTest(maxTicks=20)
    public void immutableGeometrySeparatesCustomTasksAndRaisedExteriorRoutes(GameTestHelper c){
        var p=NestPlan.geometry(new BlockPos(4,50,9),Direction.NORTH);var original=p.undergroundSurfaces();
        c.assertTrue(original==p.undergroundSurfaces(),"Repeated immutable geometry reuses the bounded derived shape");
        boolean immutable=false;try{original.clear();}catch(UnsupportedOperationException expected){immutable=true;}c.assertTrue(immutable,"Shared derived geometry cannot be contaminated by callers");
        var tasks=new ArrayList<>(p.tasks());tasks.removeLast();var custom=new NestPlan(p.entrance(),p.direction(),tasks,List.of());
        c.assertTrue(!custom.undergroundSurfaces().equals(original),"Different task sets at the same entrance cannot share shell membership");
        var raised=new NestPlan(p.entrance(),p.direction(),p.tasks(),List.of(),List.of(),List.of(),List.of(p.deposits().getFirst().above()),p.outside().above());
        c.assertTrue(raised.routeGeometry().outside().equals(raised.outside())&&raised.routeGeometry().deposits().equals(raised.deposits())&&raised.undergroundSurfaces().equals(original),"Raised exterior routes remain canonical while pure underground coordinates are equivalent");c.succeed();
    }
}
