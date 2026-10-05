package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import dev.primeants.brood.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Production queen egg, real brood-derived workers and real loaded ticks. Terrain edits are negative fixtures. */
public final class ExpansionGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private final NursingGameTest food=new NursingGameTest();
    private void balance(GameTestHelper c,LasiusNigerEntity q){
        var p=q.founding().plan();if(p==null||q.founding().phase()!=QueenFounding.Phase.SETTLED)return;
        var j=NestExpansion.get(c.getLevel()).job(q.getUUID());
        long mound=NestExpansion.deposits(p).stream().filter(b->c.getLevel().getBlockState(b).is(NurseryBlocks.NEST_SOIL)).count();
        long plugs=p.plugs().stream().filter(b->ColonyPlugs.material(c.getLevel().getBlockState(b))).count();
        long carried=f.workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum();
        long world=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum();
        long custody=TransferCustody.get(c.getLevel()).contents().stream().filter(t->t.stack().is(Items.DIRT)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
        c.assertTrue(24+(j==null?0:j.removed())==mound+plugs+carried+world+custody,"Actual original + new soil equals mound/plugs/carried/world/custody");
    }
    private void growth(GameTestHelper c,LasiusNigerEntity q,boolean[] supplied){
        if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,6,8);}
        if(supplied[0]){c.assertTrue(food.total(c,q)==14,"Fourteen real supplied units conserved through nursing/growth");food.yields(c,q);}
    }
    @GameTest(maxTicks=100,structure="prime_ants_test:idle_ground")
    public void cargoObserverCanSeeOverheadBetweenOpaqueMoundBlocks(GameTestHelper c){
        // Read-only camera reproducer: controlled opaque terrain around a one-cell air column, no actors.
        for(int x=6;x<=8;x++)for(int z=6;z<=8;z++)if(x!=7||z!=7)c.setBlock(x,2,z,NurseryBlocks.NEST_SOIL);
        var pos=net.minecraft.world.phys.Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(7,2,7)));
        var target=CargoView.target(pos,0);var eye=CargoView.eye(c.getLevel(),pos,0);
        c.assertTrue(eye!=null&&eye.x==pos.x&&eye.z==pos.z&&CargoView.clear(c.getLevel(),eye,target),"Opaque mound blocks reject oblique views; actual unmodified air column supplies the overhead observer view");c.succeed();
    }
    @GameTest(maxTicks=30000,structure="prime_ants_test:idle_ground")
    public void roomColumnsKeepPendingLowerSoilCoveredAcrossRealActions(GameTestHelper c){
        var q=f.start(c);boolean[] observed={false};c.onEachTick(()->{
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Column job remains protected");var p=q.founding().plan();if(p==null)return;
            for(int forward=3;forward<=5;forward++)for(int side=-1;side<=1;side++){
                var lower=p.at(forward,side,-2);if(c.getLevel().getBlockState(lower).is(Blocks.DIRT))c.assertTrue(!c.getLevel().getBlockState(lower.above()).isAir(),"Pending natural lower dirt stays covered; no between-action grass exposure");
            }
            if(q.founding().removed()>6)observed[0]=true;
            if(q.founding().sealed()){c.assertTrue(observed[0]&&q.founding().removed()==24&&q.founding().deposited()==22&&q.founding().plugged()==2,"Normal physical founding retains geometry and 24 soil units");c.succeed();}
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void automaticWorkerBuildsConnectedUsefulExtensionWithConservedSoil(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},snapshot={false},cargo={false},column={false};int[] last={0};Map<BlockPos,BlockState> before=new HashMap<>();Set<UUID> initial=new HashSet<>();
        c.onEachTick(()->{
            growth(c,q,supplied);balance(c,q);var p=q.founding().plan();if(p==null)return;var ws=f.workers(c,q);
            if(!snapshot[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){snapshot[0]=true;ws.forEach(w->initial.add(w.getUUID()));for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-4;y<=3;y++){var b=p.entrance().offset(x,y,z);before.put(b,c.getLevel().getBlockState(b));}}
            var j=NestExpansion.get(c.getLevel()).job(q.getUUID());if(j==null)return;
            c.assertTrue(j.tasks.size()==12&&j.removed()-last[0]<=1&&j.removed()<=32,"Twelve planned tasks, cap 32 and at most one removal per loaded tick");last[0]=j.removed();
            c.assertTrue(q.founding().ready()&&j.problem(c.getLevel(),q.getUUID())==null,"Verified partial shell remains live, no habitat bypass");
            c.assertTrue(ws.stream().filter(w->w.workerTasks().nursing()).count()>=2&&ws.stream().anyMatch(q.founding()::claimedBy),"Construction preserves two nurses and real forager");
            for(var w:ws)if(w.getUUID().equals(j.claim)){
                c.assertTrue(ColonyMembers.get(c.getLevel()).belongs(w,q.getUUID(),p.chamber())&&!w.isCallow()&&!q.founding().claimedBy(w),"Builder is actual mature lineage member, independent single action owner");
                if(w.getMainHandItem().is(Items.DIRT)){cargo[0]=true;c.assertTrue(w.getMainHandItem().getCount()<=4,"Physical mandible capacity");}
                if(w.workerTasks().phase()==WorkerTasks.Phase.DIG_OUT){column[0]=true;c.assertTrue(j.removed()%2==0,"Every complete column precedes outside hauling");}
            }
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T11 extension tick={} removed={} deposited={} usable={} use={} workers={}",c.getTick(),j.removed(),j.deposited,j.usable(c.getLevel(),q.getUUID()).size(),j.use,ws.stream().map(w->w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem()).toList());
            if(j.complete()&&!j.usedBy.isEmpty()){
                c.assertTrue(j.removed()==12&&j.deposited==12&&j.released==0&&j.usable(c.getLevel(),q.getUUID()).size()==6&&cargo[0]&&column[0],"Six usable two-high connected floor cells from twelve physical removals/deposits");
                Set<BlockPos> reachable=new HashSet<>();var queue=new ArrayDeque<BlockPos>();queue.add(p.at(5,j.side,-2));
                while(!queue.isEmpty()){var b=queue.remove();if(!reachable.add(b))continue;for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL){var n=b.relative(d);if((j.floors().contains(n)||n.equals(p.at(5,j.side,-2)))&&NestPlan.walkable(c.getLevel(),n)&&!reachable.contains(n))queue.add(n);}}
                for(var b:j.floors())c.assertTrue(NestPlan.walkable(c.getLevel(),b)&&reachable.contains(b),"Actual connected floor with two-high air and solid support");
                Set<BlockPos> allowed=new HashSet<>(j.tasks);allowed.addAll(j.surfaces());allowed.addAll(NestExpansion.deposits(p));allowed.add(p.nursery());allowed.add(p.cache());
                before.forEach((b,s)->c.assertTrue(s.equals(c.getLevel().getBlockState(b))||allowed.contains(b),"No mutation outside declared excavation/preparation/mound/nursery/cache: "+b));
                c.assertTrue(ws.stream().map(LasiusNigerEntity::getUUID).toList().containsAll(initial)&&q.bodyReserve()==0&&food.consumed(c,q)>0&&ws.size()>3,"Original anchors and identities preserved, finite food-fed growth continues");
                c.assertTrue(List.of("existing_nurse_traversal","brood_emergence").contains(j.use),"Practical extension use is an observed worker event");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void sameStateReplacementAfterPlanningRevokesPreparedTarget(GameTestHelper c){interfere(c,0);}
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void playerHoleInPlannedCellCannotBecomeUsableSpace(GameTestHelper c){interfere(c,1);}
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void unrelatedShellHoleCannotBeLegitimizedByPlan(GameTestHelper c){interfere(c,2);}
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void removedSupportPreventsWorkerActionAndPausesHabitat(GameTestHelper c){interfere(c,3);}
    private void interfere(GameTestHelper c,int mode){
        var q=f.start(c);boolean[] supplied={false},edited={false};
        c.onEachTick(()->{
            if(edited[0])return;growth(c,q,supplied);var j=NestExpansion.get(c.getLevel()).job(q.getUUID());if(j==null)return;edited[0]=true;
            c.assertTrue(j.removed()==0,"Interference precedes first real action");var t=j.tasks.getFirst();
            if(mode==2)t=q.founding().plan().at(6,-1,-2);if(mode==3)t=j.floors().getFirst().below();var target=t;
            var old=c.getLevel().getBlockState(target);c.getLevel().setBlock(target,mode==0?old:Blocks.AIR.defaultBlockState(),3);
            c.runAfterDelay(150,()->{
                c.assertTrue(j.removed()==0&&j.completed().isEmpty()&&j.usable(c.getLevel(),q.getUUID()).isEmpty(),"Unauthorized target/shell edits cannot turn plan into completed geometry");
                if(mode==0)c.assertTrue(!ColonyTerrain.get(c.getLevel()).eligible(c.getLevel(),target,q.getUUID())&&c.getLevel().getBlockState(target).equals(old),"Same-state replacement revokes exact target provenance");
                else c.assertTrue(!q.founding().ready(),"Unauthorized breach/support loss remains invalid despite planned footprint");
                c.assertTrue(food.total(c,q)==14,"Interference retains physical food without remote consumption");c.succeed();
            });
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void blockedExteriorMoundRetainsBuilderCargo(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},blocked={false};
        c.onEachTick(()->{
            if(blocked[0])return;growth(c,q,supplied);balance(c,q);var j=NestExpansion.get(c.getLevel()).job(q.getUUID());if(j==null||j.removed()<4)return;
            var w=f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)&&a.workerTasks().phase()==WorkerTasks.Phase.DIG_OUT).findFirst();if(w.isEmpty())return;blocked[0]=true;
            var builder=w.get();int held=builder.getMainHandItem().getCount(),removed=j.removed(),placed=j.deposited;
            for(var b:NestExpansion.deposits(j.home))if(c.getLevel().getBlockState(b).isAir())c.getLevel().setBlock(b,Blocks.STONE.defaultBlockState(),3);
            // Other real-tick fixtures can restore this dimension's SavedData. Reacquire the canonical job;
            // a historical Java reference is not execution truth after a normal data-storage replacement.
            var encoded=NestExpansion.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NestExpansion.get(c.getLevel())).getOrThrow();
            c.getLevel().getDataStorage().set(NestExpansion.TYPE,NestExpansion.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow());
            c.runAfterDelay(140,()->{var current=NestExpansion.get(c.getLevel()).job(q.getUUID());c.assertTrue(builder.getMainHandItem().is(Items.DIRT)&&builder.getMainHandItem().getCount()==held&&current.removed()==removed&&current.deposited==placed&&current.reason.equals("mound_blocked_soil_retained"),"Blocked deposition retains all cargo, no dumping/clearing/remote placement: held="+builder.getMainHandItem()+" removed="+current.removed()+" placed="+current.deposited+" reason="+current.reason+" worker="+builder.workerTasks().reason()+" home="+q.founding().reason());balance(c,q);c.succeed();});
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void midWorkEntityAndSavedDataRestorationKeepsOneClaimAndSoil(GameTestHelper c){
        LasiusNigerEntity[] q={f.start(c)};boolean[] supplied={false},restored={false};int[] removed={0};UUID[] claim={null};
        c.onEachTick(()->{
            growth(c,q[0],supplied);balance(c,q[0]);var j=NestExpansion.get(c.getLevel()).job(q[0].getUUID());if(j==null)return;
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T11 restore tick={} queen={} ready={} restored={} removed={} deposited={} claim={} workers={}",c.getTick(),q[0].getUUID(),q[0].founding().ready(),restored[0],j.removed(),j.deposited,j.claim,f.workers(c,q[0]).stream().map(w->w.getUUID()+" "+w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem()).toList());
            if(!restored[0]&&j.removed()>=2){restored[0]=true;removed[0]=j.removed();claim[0]=j.claim;var builder=f.workers(c,q[0]).stream().filter(w->w.getUUID().equals(j.claim)).findFirst().orElseThrow();
                f.restore(c,builder);q[0]=f.restore(c,q[0]);c.getLevel().getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())){
                    var saved=disk.get(NestExpansion.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);c.assertTrue(saved!=null&&terrain!=null&&saved.job(q[0].getUUID()).removed()==removed[0]&&saved.job(q[0].getUUID()).claim.equals(claim[0]),"Actual disk restores separate planned/completed work and claim");c.getLevel().getDataStorage().set(NestExpansion.TYPE,saved);c.getLevel().getDataStorage().set(ColonyTerrain.TYPE,terrain);
                }
            }
            if(restored[0]&&j.complete()){c.assertTrue(j.removed()==12&&j.deposited==12&&f.workers(c,q[0]).stream().filter(w->w.getUUID().equals(claim[0])).count()==1,"Normal entity/actual SavedData restore never duplicates builder/edit/soil");c.succeed();}
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void builderDeathUsesCustodyAndActualWorkerReassignment(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},dead={false},custodyRestored={false};UUID[] killed={null};
        c.onEachTick(()->{
            growth(c,q,supplied);balance(c,q);var j=NestExpansion.get(c.getLevel()).job(q.getUUID());if(j==null)return;
            if(!dead[0]&&j.removed()>=2){var w=f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)&&a.getMainHandItem().is(Items.DIRT)).findFirst();if(w.isEmpty())return;dead[0]=true;killed[0]=w.get().getUUID();var position=w.get().position();TransferFault.blockAt(position);w.get().hurtServer(c.getLevel(),w.get().damageSources().generic(),1000);
                c.runAfterDelay(40,()->{
                    c.assertTrue(TransferFault.refusedAt(position)>0&&TransferCustody.get(c.getLevel()).contents().stream().anyMatch(t->t.source().equals("worker:"+killed[0])&&t.stack().is(Items.DIRT)),"Rejected dead-builder drop retains verified custody");
                    c.getLevel().getDataStorage().saveAndJoin();
                    try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())){var saved=disk.get(TransferCustody.TYPE);c.assertTrue(saved!=null&&saved.contents().stream().anyMatch(t->t.source().equals("worker:"+killed[0])),"Actual disk preserves rejected builder soil");c.getLevel().getDataStorage().set(TransferCustody.TYPE,saved);}
                    custodyRestored[0]=true;TransferFault.releaseAt(position);
                });}
            if(dead[0]&&custodyRestored[0]&&j.complete()){c.assertTrue(j.released>0&&j.deposited+j.released==12&&j.removed()==12&&ColonyMembers.get(c.getLevel()).member(killed[0]).dead()&&f.workers(c,q).stream().noneMatch(w->w.getUUID().equals(killed[0])),"Dead builder soil follows custody and no replacement body/duplicate edit is invented");c.succeed();}
        });
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void protectedUnknownForeignAndHistoricalLiningFailClosed(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},revoked={false},finished={false};
        c.onEachTick(()->{
            if(finished[0])return;growth(c,q,supplied);var p=q.founding().plan();if(p==null)return;
            if(!revoked[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){revoked[0]=true;
                for(int side:new int[]{-1,1}){var b=p.at(3,2*side,-1);var state=c.getLevel().getBlockState(b);c.getLevel().setBlock(b,state,3);c.assertTrue(!ColonyTerrain.get(c.getLevel()).eligible(c.getLevel(),b,q.getUUID()),"Historical unrecorded/missing origin or player lining cannot be reconstructed");}
                var records=ColonyTerrain.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyTerrain.get(c.getLevel())).getOrThrow().getAsJsonObject();
                var foreign=p.at(4,2,-1);c.getLevel().setBlock(foreign,NurseryBlocks.NEST_SOIL.defaultBlockState(),3);records.addProperty(Long.toString(foreign.asLong()),UUID.randomUUID()+":prepared:nest_soil");c.getLevel().getDataStorage().set(ColonyTerrain.TYPE,ColonyTerrain.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,records).getOrThrow());
                c.assertTrue(!ColonyTerrain.get(c.getLevel()).prepared(c.getLevel(),foreign,q.getUUID()),"Foreign preparation gives no permission");
            }
            if(revoked[0]&&f.workers(c,q).stream().filter(w->!w.isCallow()).count()>=4){c.runAfterDelay(160,()->{c.assertTrue(NestExpansion.get(c.getLevel()).job(q.getUUID())==null&&q.founding().ready(),"No workable protected candidate leaves terrain unchanged and colony feeding valid");c.succeed();});finished[0]=true;}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void sameStateMoundReplacementCannotAuthorizeSupportOrExcavation(GameTestHelper c){
        var q=f.start(c);boolean[] done={false};c.onEachTick(()->{
            if(done[0]||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return;done[0]=true;var p=q.founding().plan();
            var b=p.deposits().stream().filter(v->ColonyTerrain.get(c.getLevel()).mound(c.getLevel(),v,q.getUUID())).findFirst().orElseThrow();
            c.assertTrue(NestExpansion.depositSupport(c.getLevel(),b.above(),q.getUUID())&&!ColonyTerrain.get(c.getLevel()).eligible(c.getLevel(),b,q.getUUID()),"Owned mound supports layers but never excavation");
            c.getLevel().setBlock(b,c.getLevel().getBlockState(b),3);c.assertTrue(!ColonyTerrain.get(c.getLevel()).mound(c.getLevel(),b,q.getUUID())&&!NestExpansion.depositSupport(c.getLevel(),b.above(),q.getUUID()),"Same-state replacement revokes mound layer authorization");c.succeed();
        });
    }
}
