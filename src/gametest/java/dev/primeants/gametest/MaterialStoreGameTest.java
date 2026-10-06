package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The first nest-plan chamber, in real loaded ticks: production egg founding, dropped food, real brood-derived workers.
 * The stage is only evaluated by the colony's own nursery and the store job is only planned by the colony. The fixture
 * is the shared pad plus witnessed natural dirt beyond the founding chamber's back wall, where the plan's "right"
 * placement validates and its "left" one leaves the pad. Interventions are real blocks and entity/SavedData restores. */
public final class MaterialStoreGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private final NursingGameTest food=new NursingGameTest();
    /** The shared 14x14 pad and egg, then six more witnessed dirt columns eastward. Shared helpers are unchanged. */
    private LasiusNigerEntity start(GameTestHelper c){
        var founding=new QueenFoundingGameTest();founding.terrain(c,Blocks.DIRT.defaultBlockState(),true,true);
        var l=c.getLevel();var records=NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for(int x=15;x<=20;x++)for(int z=1;z<=14;z++)for(int y=1;y<=4;y++){c.setBlock(x,y,z,Blocks.DIRT);records.addProperty(Long.toString(c.absolutePos(new BlockPos(x,y,z)).asLong()),"minecraft:dirt");}
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(JsonOps.INSTANCE,records).getOrThrow());
        return founding.egg(c);
    }
    private static ChamberExcavation.Job store(GameTestHelper c,LasiusNigerEntity q){return ChamberExcavation.get(c.getLevel()).job(q.getUUID(),ChamberExcavation.STORE);}
    private static ColonyDevelopment.ChamberState chamber(ColonyDevelopment.Evaluation e,String id){return e==null?null:e.chambers().stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
    private static boolean bothConfirmed(ColonyDevelopment.Evaluation e){
        var s=chamber(e,ChamberRegistry.FOUNDING);
        return s!=null&&s.problem()==null&&s.functions().equals(Map.of(ChamberFunction.NURSERY,ColonyDevelopment.Presence.CONFIRMED,ChamberFunction.FOOD_STORE,ColonyDevelopment.Presence.CONFIRMED));
    }
    private long carried(GameTestHelper c,LasiusNigerEntity q,UUID worker){
        return f.workers(c,q).stream().filter(w->w.getUUID().equals(worker)&&w.getMainHandItem().is(Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum();
    }
    /** Exact soil accounting at every tick: every unit the queen, the widening and the store removed is a mound block, a
     * plug, carried, a dropped item or in transfer custody; and each job's removed = carried + deposited + released. */
    private void soil(GameTestHelper c,LasiusNigerEntity q){
        var l=c.getLevel();var p=q.founding().plan();if(p==null||q.founding().phase()!=QueenFounding.Phase.SETTLED)return;
        var widening=NestExpansion.get(l).job(q.getUUID());var job=store(c,q);
        long mound=NestExpansion.deposits(p).stream().filter(b->l.getBlockState(b).is(NurseryBlocks.NEST_SOIL)).count();
        long plugs=p.plugs().stream().filter(b->ColonyPlugs.material(l.getBlockState(b))).count();
        long held=f.workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum();
        long world=l.getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum();
        long custody=TransferCustody.get(l).contents().stream().filter(t->t.stack().is(Items.DIRT)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
        long removed=24+(widening==null?0:widening.removed())+(job==null?0:job.removed());
        c.assertTrue(removed==mound+plugs+held+world+custody,"Founding, widening and store soil are all physical: removed="+removed+" mound="+mound+" plugs="+plugs+" held="+held+" world="+world+" custody="+custody);
        if(job!=null)c.assertTrue(job.removed()==job.deposited+job.released+(job.claim==null?0:carried(c,q,job.claim)),"Store job removed = carried + deposited + released: "+job.removed()+" "+job.deposited+" "+job.released+" claim="+job.claim);
    }
    private void grow(GameTestHelper c,LasiusNigerEntity q,boolean[] supplied){
        if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,12,10);}
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void youngColonyDigsTheMaterialStoreAndTheEvaluatorConfirmsIt(GameTestHelper c){
        var q=start(c);boolean[] supplied={false},planned={false};int[] last={0};Map<BlockPos,BlockState> before=new HashMap<>();ColonyDevelopment.Evaluation[] seen={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=food.pile(c,q);grow(c,q,supplied);soil(c,q);if(p==null)return;
            var job=store(c,q);if(job==null)return;
            var plan=q.founding().plan();var e=p.stageEvaluation();
            if(!planned[0]){
                planned[0]=true;var widening=NestExpansion.get(l).job(q.getUUID());
                c.assertTrue(job.removed()==0&&job.placement.equals("right")&&job.tasks.size()==24&&e!=null&&e.stage()==ColonyStage.YOUNG&&(widening==null||widening.complete())
                    &&e.result().missing(ColonyStage.MATURE).stream().anyMatch(m->m.requirement().name().equals("material_store")&&!m.unknown()),
                    "Planned only for a Young colony that certainly lacks a store, after the 0.1.0 widening; the left placement leaves the pad: "+job.placement+" "+e);
                for(int x=-7;x<=13;x++)for(int z=-7;z<=7;z++)for(int y=-4;y<=3;y++){var b=plan.entrance().offset(x,y,z);before.put(b,l.getBlockState(b));}
                PrimeAnts.LOGGER.info("T03 STORE PLANNED queen={} placement={} tasks={} marker={} widening={} evaluation={}",q.getUUID(),job.placement,job.tasks.size(),job.built.marker(),widening==null?null:widening.removed(),e);
            }
            c.assertTrue(job.removed()-last[0]<=1&&!job.stopped(),"At most one removal per loaded tick, never stopped on natural soil");last[0]=job.removed();
            if(e!=seen[0]){seen[0]=e;c.assertTrue(bothConfirmed(e)&&e.stage()==ColonyStage.YOUNG,"The tunnel is an authorized opening: nursery and food store stay confirmed while digging: "+e);}
            c.assertTrue(q.founding().ready(),"The queen's habitat stays ready while the tunnel opens: "+q.founding().reason());
            for(var w:f.workers(c,q))if(w.getUUID().equals(job.claim))
                c.assertTrue(ColonyMembers.get(l).belongs(w,q.getUUID(),plan.chamber())&&!w.isCallow()&&!q.founding().claimedBy(w)&&(!w.getMainHandItem().is(Items.DIRT)||w.getMainHandItem().getCount()<=QueenFounding.CARRY_CAPACITY),"The builder is one real mature member, never the forager, carrying at most four units");
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T03 store tick={} removed={} deposited={} reason={} workers={}",c.getTick(),job.removed(),job.deposited,job.reason,f.workers(c,q).stream().map(w->w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem()).toList());
            if(!job.complete()||!job.established(l,q.getUUID()))return;
            var state=chamber(e,ChamberExcavation.STORE);
            if(state==null||state.functions().get(ChamberFunction.MATERIAL_STORE)!=ColonyDevelopment.Presence.CONFIRMED)return; // the nursery's next evaluation
            var colony=ChamberRegistry.get(l).colony(q.getUUID());var marker=job.built.marker();
            var stores=colony.chambers().stream().filter(ch->ch.functions().contains(ChamberFunction.MATERIAL_STORE)).toList();
            c.assertTrue(colony.chambers().size()==2&&stores.size()==1&&stores.getFirst().tier()==1&&stores.getFirst().functions().equals(EnumSet.of(ChamberFunction.MATERIAL_STORE))
                &&stores.getFirst().markers().equals(Map.of(ChamberFunction.MATERIAL_STORE,marker))&&l.getBlockEntity(marker) instanceof MaterialStore s&&s.ownedBy(q.getUUID(),plan)&&s.size()==0,
                "Exactly one tier-1 material-store chamber, its marker at the owned, empty store block: "+colony);
            var names=e.result().missing(ColonyStage.MATURE).stream().map(m->m.requirement().name()).toList();
            c.assertTrue(state.problem()==null&&state.tier()==1&&bothConfirmed(e)&&e.stage()==ColonyStage.YOUNG&&e.inputs().tier(ChamberFunction.MATERIAL_STORE).known()==1
                &&!names.contains("material_store")&&names.containsAll(List.of("queens_hall","clay")),"The evaluator confirms the store; the colony stays Young without a queen's hall or clay: "+e);
            c.assertTrue(job.removed()==24&&job.deposited==24&&job.released==0,"Twenty-four real removals, all on the mound");
            c.assertTrue(q.getUUID().equals(ColonyAlarm.ownedComponent(l,marker)),"The store is an owned colony component: breaking it alarms the colony");
            for(var b:job.tasks)c.assertTrue(b.equals(marker)||ColonyTerrain.get(l).opened(l,b,q.getUUID()),"Every planned cell is this colony's worker opening: "+b);
            Set<BlockPos> allowed=new HashSet<>(job.tasks);allowed.addAll(job.surfaces());allowed.addAll(NestExpansion.deposits(plan));allowed.add(plan.nursery());allowed.add(plan.cache());
            before.forEach((b,old)->c.assertTrue(old.equals(l.getBlockState(b))||allowed.contains(b),"No edit outside the planned cells, their prepared shell and the mound: "+b+" "+old+" -> "+l.getBlockState(b)));
            PrimeAnts.LOGGER.info("T03 STORE CONFIRMED queen={} tick={} registry={} evaluation={}",q.getUUID(),c.getTick(),colony,e);c.succeed();
        });
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void playerPlacedBlockInPlannedCellIsNeverRemovedAndTheJobStops(GameTestHelper c){
        var q=start(c);boolean[] supplied={false};BlockPos[] cell={null};int[] index={-1};long[] stoppedAt={-1};
        c.onEachTick(()->{
            var l=c.getLevel();var p=food.pile(c,q);grow(c,q,supplied);soil(c,q);if(p==null)return;
            var job=store(c,q);if(job==null)return;
            if(cell[0]==null){
                if(job.removed()<2)return;
                // A placed block replaces a pending natural floor cell of the room, beyond the dig front.
                index[0]=8;cell[0]=job.tasks.get(index[0]);c.assertTrue(job.removed()<index[0]&&NaturalSoil.get(l).eligible(l,cell[0]),"The chosen planned cell is still pending natural soil");
                l.setBlock(cell[0],Blocks.COBBLESTONE.defaultBlockState(),3);
                PrimeAnts.LOGGER.info("T03 PLACED BLOCK queen={} cell={} index={} removed={}",q.getUUID(),cell[0],index[0],job.removed());return;
            }
            c.assertTrue(l.getBlockState(cell[0]).is(Blocks.COBBLESTONE)&&!job.completed().contains(cell[0])&&job.removed()<=index[0],"The placed block is never removed and nothing beyond it is dug");
            if(stoppedAt[0]<0){
                if(!job.stopped()||job.claim!=null)return;
                stoppedAt[0]=c.getTick();
                c.assertTrue(job.reason.startsWith("stopped_target_replaced_unknown_or_foreign_at_")&&job.removed()==index[0]&&job.deposited+job.released==job.removed()
                    &&f.workers(c,q).stream().noneMatch(w->w.getMainHandItem().is(Items.DIRT)),"The job stops exactly at the placed cell after delivering its soil: "+job.reason+" removed="+job.removed());
                PrimeAnts.LOGGER.info("T03 STORE STOPPED queen={} reason={} removed={} deposited={}",q.getUUID(),job.reason,job.removed(),job.deposited);return;
            }
            c.assertTrue(job.claim==null&&f.workers(c,q).stream().noneMatch(w->w.workerTasks().construction()),"A stopped job is never reassigned");
            if(c.getTick()-stoppedAt[0]<300)return;
            var e=p.stageEvaluation();
            c.assertTrue(e!=null&&bothConfirmed(e)&&q.founding().ready()&&job.problem(l,q.getUUID())==null&&ChamberRegistry.get(l).colony(q.getUUID()).chambers().size()==1,
                "The dug part stays an authorized, enclosed opening: brood care and the founding functions continue, and no store is registered: "+e);
            PrimeAnts.LOGGER.info("T03 STOPPED JOB SAFE queen={} evaluation={}",q.getUUID(),e);c.succeed();
        });
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void midDigReloadContinuesWithoutDuplicateRemovalsAndTheFinishedStoreReloadsAsOneChamber(GameTestHelper c){
        LasiusNigerEntity[] q={start(c)};boolean[] supplied={false},restored={false},reloaded={false};int[] last={0};UUID[] claim={null};ChamberRegistry.Chamber[] registered={null};long[] reloadTick={-1};
        c.onEachTick(()->{
            var l=c.getLevel();var p=food.pile(c,q[0]);grow(c,q[0],supplied);soil(c,q[0]);if(p==null)return;
            var job=store(c,q[0]);if(job==null)return;
            c.assertTrue(job.removed()-last[0]<=1,"No duplicate or skipped removal across the reload");last[0]=job.removed();
            for(int i=0;i<job.tasks.size();i++){var b=job.tasks.get(i);if(b.equals(job.built.marker())&&job.established(l,q[0].getUUID()))continue;
                c.assertTrue(i<job.removed()?l.getBlockState(b).isAir():!l.getBlockState(b).isAir(),"Exactly the recorded prefix of the queue is open: "+b);}
            if(!restored[0]){
                var builder=f.workers(c,q[0]).stream().filter(w->w.getUUID().equals(job.claim)&&w.getMainHandItem().is(Items.DIRT)).findFirst();
                if(job.removed()<3||builder.isEmpty())return;
                restored[0]=true;claim[0]=job.claim;int removed=job.removed();int held=builder.get().getMainHandItem().getCount();
                f.restore(c,builder.get());q[0]=f.restore(c,q[0]);l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var saved=disk.get(ChamberExcavation.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);
                    var savedJob=saved==null?null:saved.job(q[0].getUUID(),ChamberExcavation.STORE);
                    c.assertTrue(savedJob!=null&&terrain!=null&&savedJob.removed()==removed&&claim[0].equals(savedJob.claim)&&savedJob.placement.equals(job.placement)&&savedJob.completed().equals(job.completed()),"Disk restores the planned queue, completed removals and claim");
                    l.getDataStorage().set(ChamberExcavation.TYPE,saved);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);
                }
                PrimeAnts.LOGGER.info("T03 MID-DIG RELOAD queen={} builder={} removed={} held={}",q[0].getUUID(),claim[0],removed,held);return;
            }
            c.assertTrue(f.workers(c,q[0]).stream().filter(w->w.getUUID().equals(claim[0])).count()<=1,"Never a duplicate builder identity after the reload");
            if(!reloaded[0]){
                if(!job.complete()||!job.established(l,q[0].getUUID()))return;
                var state=chamber(p.stageEvaluation(),ChamberExcavation.STORE);
                if(state==null||state.functions().get(ChamberFunction.MATERIAL_STORE)!=ColonyDevelopment.Presence.CONFIRMED)return;
                c.assertTrue(job.removed()==24&&job.deposited+job.released==24,"The reloaded job finished every removal with all soil settled");
                var colony=ChamberRegistry.get(l).colony(q[0].getUUID());registered[0]=colony.chamber(ChamberExcavation.STORE);
                // A finished chamber and its store reload: saved data from disk, the store block entity and the pile.
                l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var registry=disk.get(ChamberRegistry.TYPE);var excavation=disk.get(ChamberExcavation.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);
                    c.assertTrue(registry!=null&&colony.equals(registry.colony(q[0].getUUID()))&&excavation!=null&&terrain!=null,"Disk restores the registry with its store chamber");
                    l.getDataStorage().set(ChamberRegistry.TYPE,registry);l.getDataStorage().set(ChamberExcavation.TYPE,excavation);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);
                }
                var marker=job.built.marker();var store=(MaterialStore)l.getBlockEntity(marker);var tag=store.saveWithFullMetadata(l.registryAccess());
                l.removeBlockEntity(marker);var loaded=(MaterialStore)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(marker,l.getBlockState(marker),tag,l.registryAccess());
                c.assertTrue(loaded!=null,"Normal store restore");l.setBlockEntity(loaded);
                // The saved format already carries contents: one natural unit loads, a non-material unit is refused.
                var ops=l.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                var clay=tag.copy();clay.put("Contents",net.minecraft.world.item.ItemStack.CODEC.listOf().encodeStart(ops,List.of(new net.minecraft.world.item.ItemStack(Items.CLAY_BALL))).getOrThrow());
                var apple=tag.copy();apple.put("Contents",net.minecraft.world.item.ItemStack.CODEC.listOf().encodeStart(ops,List.of(new net.minecraft.world.item.ItemStack(Items.APPLE))).getOrThrow());
                var stocked=(MaterialStore)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(marker,l.getBlockState(marker),clay,l.registryAccess());
                c.assertTrue(stocked!=null&&stocked.size()==1&&stocked.contents().getFirst().is(Items.CLAY_BALL)&&net.minecraft.world.level.block.entity.BlockEntity.loadStatic(marker,l.getBlockState(marker),apple,l.registryAccess())==null,
                    "Saved contents load as canonical one-unit material stacks; food is not a material");
                var pile=p.getBlockPos();var pileTag=p.saveWithFullMetadata(l.registryAccess());l.removeBlockEntity(pile);
                var pileLoaded=(BroodPile)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pile,l.getBlockState(pile),pileTag,l.registryAccess());
                c.assertTrue(pileLoaded!=null,"Normal pile restore");l.setBlockEntity(pileLoaded);
                reloaded[0]=true;reloadTick[0]=c.getTick();PrimeAnts.LOGGER.info("T03 FINISHED STORE RELOAD queen={} chamber={}",q[0].getUUID(),registered[0]);return;
            }
            var e=food.pile(c,q[0]).stageEvaluation();
            if(e==null||c.getTick()-reloadTick[0]<ColonyDevelopment.INTERVAL+20)return;
            var colony=ChamberRegistry.get(l).colony(q[0].getUUID());var state=chamber(e,ChamberExcavation.STORE);
            c.assertTrue(colony.chambers().stream().filter(ch->ch.functions().contains(ChamberFunction.MATERIAL_STORE)).count()==1&&registered[0].equals(colony.chamber(ChamberExcavation.STORE))
                &&l.getBlockEntity(job.built.marker()) instanceof MaterialStore s&&s.ownedBy(q[0].getUUID(),q[0].founding().plan())&&s.placement().equals(job.placement)
                &&state!=null&&state.functions().get(ChamberFunction.MATERIAL_STORE)==ColonyDevelopment.Presence.CONFIRMED&&bothConfirmed(e)&&e.stage()==ColonyStage.YOUNG,
                "After reload: one registry chamber, the same owned store, confirmed by a fresh evaluation: "+e);
            PrimeAnts.LOGGER.info("T03 STORE RELOAD CONFIRMED queen={} evaluation={}",q[0].getUUID(),e);c.succeed();
        });
    }
}
