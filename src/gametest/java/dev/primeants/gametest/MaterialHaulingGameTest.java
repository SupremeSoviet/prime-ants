package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Material hauling in real loaded ticks: materials a player drops behind the entrance are carried, one unit per trip
 * in a forager's mandibles, into the colony's confirmed store, after any collectable food. Every tick, each dropped
 * unit is on the ground, carried, stored or in transfer custody (fixture drops never despawn). Production founding,
 * dropped food and real brood-derived workers; nothing assigns a task, a stage or an inventory. */
public final class MaterialHaulingGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();
    /** Behind the entrance on the central route: in the forager's search area, clear of every mound column. */
    private static BlockPos drops(LasiusNigerEntity q) { return q.founding().plan().at(-5, 0, 1); }
    private static boolean onGround(GameTestHelper c, java.util.function.Predicate<ItemStack> kind) {
        return !c.getLevel().getEntitiesOfClass(ItemEntity.class, c.getBounds().inflate(8), i -> i.isAlive() && kind.test(i.getItem())).isEmpty();
    }
    private List<LasiusNigerEntity> carriers(GameTestHelper c, LasiusNigerEntity q) { return fx.f.workers(c, q).stream().filter(w -> MaterialStore.material(w.getMainHandItem())).toList(); }
    private static long count(MaterialStore s, Item item) { return s == null ? 0 : s.contents().stream().filter(st -> st.is(item)).count(); }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void droppedClayCobblestoneAndCoalReachTheConfirmedStoreAfterTheFoodWithExactAccounting(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false},dropped={false};Map<Item,Integer> drops=new LinkedHashMap<>();Map<UUID,ItemStack> held=new HashMap<>();long[] pickups={0};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);if(p==null)return;
            var e=p.stageEvaluation();
            if(!dropped[0]){
                if(!NestPlanFixture.storeConfirmed(e))return;
                // Food and materials together, after the store stands.
                dropped[0]=true;drops.put(Items.CLAY_BALL,3);drops.put(Items.COBBLESTONE,2);drops.put(Items.COAL,1);
                drops.forEach((item,n)->fx.drop(c,drops(q),new ItemStack(item,n)));fx.drop(c,q.founding().plan().at(-4,0,1),new ItemStack(Items.APPLE,2));
                PrimeAnts.LOGGER.info("T04 HAUL DROPPED queen={} drops={} evaluation={}",q.getUUID(),drops,e);return;
            }
            fx.accounted(c,q,drops);
            for(var w:fx.f.workers(c,q)){
                var now=w.getMainHandItem();var was=held.put(w.getUUID(),now.copy());
                if(was!=null&&was.isEmpty()&&MaterialStore.material(now)){
                    pickups[0]++;c.assertTrue(now.getCount()==1&&!onGround(c,WorkerTasks::food),"Food first: a material leaves the ground only once no dropped food lies there: "+now);
                    PrimeAnts.LOGGER.info("T04 HAUL PICKUP queen={} worker={} unit={} tick={}",q.getUUID(),w.getUUID(),now,c.getTick());
                }
            }
            var s=NestPlanFixture.store(c,q);
            if(s==null||s.size()!=6||!carriers(c,q).isEmpty())return;
            c.assertTrue(count(s,Items.CLAY_BALL)==3&&count(s,Items.COBBLESTONE)==2&&count(s,Items.COAL)==1&&!onGround(c,MaterialStore::material)&&!onGround(c,st->st.is(Items.APPLE))&&pickups[0]==6,
                "Every dropped unit is in the store, each carried once, and the dropped food was collected: "+s.contents());
            var state=s.getBlockState();
            c.assertTrue(state.getValue(MaterialStoreBlock.CLAY)==1&&state.getValue(MaterialStoreBlock.STOCK)==1&&state.getValue(MaterialStoreBlock.STONE)&&state.getValue(MaterialStoreBlock.ORE)
                &&!state.getValue(MaterialStoreBlock.GRAVEL)&&!state.getValue(MaterialStoreBlock.SAND),"The store shows a clay heap, a heap of other stock, and stone and ore lumps for its cobblestone and coal: "+state);
            if(e==null||e.inputs().clay().known()!=3)return; // the nursery's next evaluation
            c.assertTrue(e.inputs().clay().equals(new StageRules.Bound(3,3))&&e.inputs().stone().equals(new StageRules.Bound(2,2))&&NestPlanFixture.storeConfirmed(e),"The evaluator counts clay and stone from the confirmed store; coal is ore: "+e.inputs());
            PrimeAnts.LOGGER.info("T04 HAUL STORED queen={} tick={} contents={} state={} evaluation={}",q.getUUID(),c.getTick(),s.contents(),state,e);c.succeed();
        });
    }

    /** T05 review fix: food comes first up to the moment a material leaves the ground. An apple dropped while the forager
     * walks to the clay takes it away from the clay, which stays on the ground until the apple is collected. */
    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void foodDroppedWhileTheForagerWalksToClayIsCollectedBeforeTheClay(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false},dropped={false},switched={false};Map<Item,Integer> drops=new LinkedHashMap<>();UUID[] forager={null},clay={null},apple={null};ItemStack[] first={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);if(p==null)return;
            var e=p.stageEvaluation();
            if(!dropped[0]){
                // Clay alone, once the store stands and the growth food is gone.
                if(!NestPlanFixture.storeConfirmed(e)||onGround(c,WorkerTasks::food))return;
                dropped[0]=true;drops.put(Items.CLAY_BALL,2);clay[0]=fx.drop(c,drops(q),new ItemStack(Items.CLAY_BALL,2)).getUUID();return;
            }
            fx.accounted(c,q,drops);
            if(apple[0]==null){
                var w=fx.f.workers(c,q).stream().filter(a->a.workerTasks().phase()==WorkerTasks.Phase.APPROACH&&clay[0].equals(a.workerTasks().sourceId())&&a.getMainHandItem().isEmpty()).findFirst().orElse(null);
                if(w==null)return;
                // The forager is on its way to the clay, mandibles empty: a player drops an apple.
                forager[0]=w.getUUID();apple[0]=fx.drop(c,q.founding().plan().at(-3,0,1),new ItemStack(Items.APPLE,1)).getUUID();
                PrimeAnts.LOGGER.info("T05 FOOD DROPPED DURING MATERIAL APPROACH queen={} forager={} position={} tick={}",q.getUUID(),w.getUUID(),w.position(),c.getTick());return;
            }
            var w=fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(forager[0])).findFirst().orElse(null);
            if(w!=null&&w.workerTasks().reason().equals("food_before_material"))switched[0]=true;
            if(w!=null&&first[0]==null&&!w.getMainHandItem().isEmpty())first[0]=w.getMainHandItem().copy();
            boolean appleOnGround=l.getEntity(apple[0]) instanceof ItemEntity i&&i.isAlive();
            if(appleOnGround)c.assertTrue(fx.ledger(c,q,Items.CLAY_BALL).ground()==2&&carriers(c,q).isEmpty(),"No clay leaves the ground while the apple lies there: "+fx.ledger(c,q,Items.CLAY_BALL));
            if(first[0]!=null)c.assertTrue(first[0].is(Items.APPLE),"The forager's next cargo is the apple, not the clay: "+first[0]);
            var s=NestPlanFixture.store(c,q);
            if(count(s,Items.CLAY_BALL)<2)return;
            c.assertTrue(switched[0]&&first[0]!=null&&!appleOnGround&&fx.ledger(c,q,Items.CLAY_BALL).stored()==2,"The forager left the clay for the apple, and both clay units reached the store afterwards");
            PrimeAnts.LOGGER.info("T05 FOOD BEFORE MATERIAL queen={} forager={} first={} tick={} contents={}",q.getUUID(),forager[0],first[0],c.getTick(),s.contents());c.succeed();
        });
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void aCarrierKeepsItsUnitWhileItsStoreIsGoneAndFullShareLeavesStoneOnTheGround(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false},dropped={false},broken={false},restored={false};Map<Item,Integer> drops=new LinkedHashMap<>();UUID[] carrier={null};ItemStack[] unit={null};long[] fullSince={-1},waited={0};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);if(p==null)return;
            var e=p.stageEvaluation();var job=NestPlanFixture.job(c,q,ChamberExcavation.STORE);
            if(!dropped[0]){
                if(!NestPlanFixture.storeConfirmed(e))return;
                dropped[0]=true;drops.put(Items.COBBLESTONE,17);drops.put(Items.CLAY_BALL,1);
                drops.forEach((item,n)->fx.drop(c,drops(q),new ItemStack(item,n)));return;
            }
            fx.accounted(c,q,drops);var s=NestPlanFixture.store(c,q);
            if(!broken[0]){
                var first=carriers(c,q);if(first.isEmpty())return;
                c.assertTrue(s!=null&&s.size()==0,"The first unit is carried to an empty store");
                // The store block is broken while its first unit is on the way: the store is unconfirmed.
                carrier[0]=first.getFirst().getUUID();unit[0]=first.getFirst().getMainHandItem().copy();broken[0]=true;
                l.setBlock(job.built.marker(),Blocks.AIR.defaultBlockState(),3);
                PrimeAnts.LOGGER.info("T04 HAUL STORE BROKEN queen={} carrier={} unit={}",q.getUUID(),carrier[0],unit[0]);return;
            }
            if(!restored[0]){
                var w=fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(carrier[0])).findFirst().orElse(null);
                if(s==null||!job.established(l,q.getUUID())){
                    c.assertTrue(w!=null&&ItemStack.matches(w.getMainHandItem(),unit[0])&&w.workerTasks().foraging(),"While its store is gone the carrier keeps its one unit and waits: "+(w==null?null:w.getMainHandItem()+" "+w.workerTasks().reason()));
                    waited[0]++;return;
                }
                if(count(s,unit[0].getItem())==0){c.assertTrue(w!=null&&ItemStack.matches(w.getMainHandItem(),unit[0]),"The kept unit is not lost before the new store takes it");return;}
                restored[0]=true;
                c.assertTrue(waited[0]>40&&s.size()==1&&w!=null&&w.getMainHandItem().isEmpty(),"Once the colony set up its store again, the waiting carrier stored the same unit: waited="+waited[0]+" "+s.contents());
                PrimeAnts.LOGGER.info("T04 HAUL KEPT AND STORED queen={} carrier={} waited={} store={}",q.getUUID(),carrier[0],waited[0],s.contents());return;
            }
            if(fullSince[0]<0){
                if(s==null||count(s,Items.COBBLESTONE)<16||count(s,Items.CLAY_BALL)<1)return;
                var ground=fx.ledger(c,q,Items.COBBLESTONE);
                c.assertTrue(count(s,Items.COBBLESTONE)==16&&!s.room(new ItemStack(Items.COBBLESTONE))&&s.room(new ItemStack(Items.CLAY_BALL)),"Sixteen other units fill their share; clay still fits: "+s.contents());
                if(ground.carried()>0)return;
                c.assertTrue(ground.ground()==1,"The seventeenth cobblestone stays on the ground: "+ground);
                fullSince[0]=c.getTick();return;
            }
            c.assertTrue(carriers(c,q).stream().noneMatch(w->w.getMainHandItem().is(Items.COBBLESTONE))&&fx.ledger(c,q,Items.COBBLESTONE).ground()==1&&count(s,Items.COBBLESTONE)==16,
                "With its share full nobody takes the last cobblestone: it stays on the ground");
            if(c.getTick()-fullSince[0]<1200)return;
            var state=s.getBlockState();
            c.assertTrue(state.getValue(MaterialStoreBlock.STOCK)==4&&state.getValue(MaterialStoreBlock.CLAY)==1&&state.getValue(MaterialStoreBlock.STONE)
                &&!state.getValue(MaterialStoreBlock.GRAVEL)&&!state.getValue(MaterialStoreBlock.SAND)&&!state.getValue(MaterialStoreBlock.ORE),"The full share shows a full stock heap with only a stone lump: "+state);
            PrimeAnts.LOGGER.info("T04 HAUL FULL SHARE HELD queen={} tick={} contents={}",q.getUUID(),c.getTick(),s.contents());c.succeed();
        });
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void aCarrierKilledMidTripReleasesItsUnitThroughCustodyExactlyOnce(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false},dropped={false},killed={false};Map<Item,Integer> drops=new LinkedHashMap<>();UUID[] transfer={null};long[] seenPending={0};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);if(p==null)return;
            var e=p.stageEvaluation();
            if(!dropped[0]){
                if(!NestPlanFixture.storeConfirmed(e))return;
                dropped[0]=true;drops.put(Items.CLAY_BALL,2);fx.drop(c,drops(q),new ItemStack(Items.CLAY_BALL,2));return;
            }
            fx.accounted(c,q,drops);
            if(!killed[0]){
                var w=carriers(c,q).stream().filter(a->a.workerTasks().phase()==WorkerTasks.Phase.RETURN&&a.getY()>q.founding().plan().entrance().getY()).findFirst().orElse(null);
                if(w==null)return;
                killed[0]=true;transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+w.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                c.assertTrue(TransferCustody.get(l).contents().stream().noneMatch(t->t.id().equals(transfer[0]))&&l.getEntity(transfer[0])==null,"No custody record before the death");
                w.hurtServer(l,w.damageSources().genericKill(),1000);
                c.assertTrue(!w.isAlive()&&w.getMainHandItem().isEmpty()&&ColonyMembers.get(l).member(w.getUUID()).dead(),"A real lethal hit kills the carrier and empties its mandibles");
                PrimeAnts.LOGGER.info("T04 HAUL CARRIER KILLED queen={} worker={} transfer={}",q.getUUID(),w.getUUID(),transfer[0]);return;
            }
            // The unit is released once: either still pending in custody or a single item entity with the transfer id.
            boolean pending=TransferCustody.get(l).contents().stream().anyMatch(t->t.id().equals(transfer[0]));
            boolean entity=l.getEntity(transfer[0]) instanceof ItemEntity i&&i.isAlive();
            c.assertTrue(!(pending&&entity)&&TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])).count()<=1,"Released exactly once, through custody");
            if(pending)seenPending[0]++;
            var s=NestPlanFixture.store(c,q);
            if(count(s,Items.CLAY_BALL)<2)return;
            c.assertTrue(!pending&&!entity&&fx.ledger(c,q,Items.CLAY_BALL).stored()==2,"The released unit and the other one both reach the store, each once");
            PrimeAnts.LOGGER.info("T04 HAUL RELEASED UNIT STORED queen={} tick={} pendingTicks={} contents={}",q.getUUID(),c.getTick(),seenPending[0],s.contents());c.succeed();
        });
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void aMidHaulSaveAndReloadKeepsEveryUnitExactlyOnce(GameTestHelper c){
        LasiusNigerEntity[] q={fx.start(c)};boolean[] supplied={false},dropped={false},reloaded={false};Map<Item,Integer> drops=new LinkedHashMap<>();UUID[] carrier={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q[0]);fx.grow(c,q[0],supplied);if(p==null)return;
            var e=p.stageEvaluation();
            if(!dropped[0]){
                if(!NestPlanFixture.storeConfirmed(e))return;
                dropped[0]=true;drops.put(Items.CLAY_BALL,3);drops.put(Items.COBBLESTONE,1);
                drops.forEach((item,n)->fx.drop(c,drops(q[0]),new ItemStack(item,n)));return;
            }
            fx.accounted(c,q[0],drops);var s=NestPlanFixture.store(c,q[0]);
            if(!reloaded[0]){
                var w=carriers(c,q[0]);if(s==null||s.size()<1||w.isEmpty())return;
                reloaded[0]=true;carrier[0]=w.getFirst().getUUID();
                var before=new LinkedHashMap<Item,NestPlanFixture.Ledger>();drops.keySet().forEach(item->before.put(item,fx.ledger(c,q[0],item)));var contents=s.contents();var cargo=w.getFirst().getMainHandItem().copy();
                // Saved data from disk, then the carrier, the queen, every dropped stack and the store block entity.
                l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var registry=disk.get(ChamberRegistry.TYPE);var excavation=disk.get(ChamberExcavation.TYPE);var custody=disk.get(TransferCustody.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);
                    c.assertTrue(registry!=null&&excavation!=null&&terrain!=null&&registry.colony(q[0].getUUID()).equals(ChamberRegistry.get(l).colony(q[0].getUUID())),"Disk restores the registry with its store chamber");
                    l.getDataStorage().set(ChamberRegistry.TYPE,registry);l.getDataStorage().set(ChamberExcavation.TYPE,excavation);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);
                    if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
                }
                var loaded=fx.f.restore(c,w.getFirst());q[0]=fx.f.restore(c,q[0]);
                for(var item:l.getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&MaterialStore.material(i.getItem()))){
                    var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(item.save(out),"Normal item entity save");var id=item.getUUID();item.discard();
                    var back=net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,x->x);
                    c.assertTrue(back instanceof ItemEntity&&back.getUUID().equals(id)&&l.tryAddFreshEntityWithPassengers(back),"The same dropped stack returns");
                }
                var marker=s.getBlockPos();var chunk=l.getChunkAt(marker);var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(l,chunk);
                var parsed=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(l,l.palettedContainerFactory(),serial.write());var read=parsed.read(l,l.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",l.dimension(),"chunk"),chunk.getPos());
                c.assertTrue(read.getBlockState(marker).equals(s.getBlockState()),"The saved chunk keeps the store's visible projection");
                var tag=s.saveWithFullMetadata(l.registryAccess());var state=s.getBlockState();l.removeBlockEntity(marker);
                var store=(MaterialStore)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(marker,state,tag,l.registryAccess());c.assertTrue(store!=null,"Normal store restore");l.setBlockEntity(store);
                c.assertTrue(ItemStack.matches(loaded.getMainHandItem(),cargo)&&store.contents().size()==contents.size()&&store.ownedBy(q[0].getUUID(),q[0].founding().plan()),"The carried unit and the stored units reload as they were");
                drops.keySet().forEach(item->c.assertTrue(fx.ledger(c,q[0],item).equals(before.get(item)),"Every unit is where it was before the reload: "+item+" "+before.get(item)+" -> "+fx.ledger(c,q[0],item)));
                PrimeAnts.LOGGER.info("T04 HAUL MID-HAUL RELOAD queen={} carrier={} cargo={} stored={} ledger={}",q[0].getUUID(),carrier[0],cargo,contents,before);return;
            }
            c.assertTrue(fx.f.workers(c,q[0]).stream().filter(w->w.getUUID().equals(carrier[0])).count()<=1,"Never a duplicate carrier identity after the reload");
            if(s==null||s.size()<4)return;
            c.assertTrue(s.size()==4&&count(s,Items.CLAY_BALL)==3&&count(s,Items.COBBLESTONE)==1&&!onGround(c,MaterialStore::material)&&carriers(c,q[0]).isEmpty(),"After the reload every unit reaches the store exactly once: "+s.contents());
            PrimeAnts.LOGGER.info("T04 HAUL RELOAD COMPLETE queen={} tick={} contents={}",q[0].getUUID(),c.getTick(),s.contents());c.succeed();
        });
    }
}
