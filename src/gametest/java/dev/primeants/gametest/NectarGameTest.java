package dev.primeants.gametest;

import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.item.AntItems;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import static dev.primeants.gametest.NaturalPlacementGameTest.*;

/** Positive paths use automatically placed queens, generation flowers and genuine emerged workers.
 * Negative cases explicitly interrupt/obstruct production work; they are not native survival evidence. */
public final class NectarGameTest {
    @GameTest(maxTicks=18000)
    public void lateDiscoveredGeneratedFlowerGetsNormalApproachAndFullAction(GameTestHelper c){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,3560);boolean[] late={false};
        l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,chunk,4); // Widened declared diagnostic halo: late far source and worker must actually entity-tick.
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;balance(c,l,q);
            for(var w:workers(l,q))if(w.workerTasks().flowerSource()!=null){
                late[0]=true;c.assertTrue(w.workerTasks().flowerInspections()>14000,"Generated source discovered near end of finite 240-tick SEARCH window");
            }
            if(consumed(l,q)>0){c.assertTrue(late[0]&&NativeVegetation.get(l).eligible(l,flower(l,q)),"Late discovery still permits normal physical approach, full 20-tick action, return and nurse consumption");c.succeed();}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:foraging_pair")
    public void twoGenuineWorkersCompeteForOneReadyFlowerWithoutDuplication(GameTestHelper c){
        var f=new WorkerForagingGameTest();var l=c.getLevel();
        var records=NaturalSoil.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for(int x=1;x<=30;x++)for(int z=1;z<=30;z++)for(int y=1;y<=4;y++) {c.setBlock(x,y,z,Blocks.DIRT);records.addProperty(Long.toString(c.absolutePos(new BlockPos(x,y,z)).asLong()),"minecraft:dirt");}
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,records).getOrThrow());
        var a=f.pairEgg(c,new BlockPos(8,4,10));var b=f.pairEgg(c,new BlockPos(20,4,10));BlockPos[] source={null};
        boolean[] supplied={false};var approached=new HashSet<UUID>();long[] firstHarvest={-1};
        c.onEachTick(()->{
            if(!supplied[0]&&f.workers(c,a).stream().anyMatch(w->a.founding().claimedBy(w)&&FoodCompetitionWindow.fresh(c,w))
                &&f.workers(c,b).stream().anyMatch(w->b.founding().claimedBy(w)&&FoodCompetitionWindow.fresh(c,w))){
                var oa=a.founding().plan().outside();var ob=b.founding().plan().outside();
                source[0]=new BlockPos(Math.floorDiv(oa.getX()+ob.getX(),2),oa.getY(),Math.floorDiv(oa.getZ()+ob.getZ(),2)+2);
                c.assertTrue(oa.getY()==ob.getY()&&NestPlan.walkable(l,source[0]),"One supported controlled source near midpoint of actual search anchors");
                dev.primeants.PrimeAnts.LOGGER.info("T21 competition publication anchors={}/{} source={} distances={}/{}",oa,ob,source[0],oa.distSqr(source[0]),ob.distSqr(source[0]));
                supplied[0]=true;l.setBlock(source[0],Blocks.POPPY.defaultBlockState(),3); // Explicit controlled source, no supplied adults/tasks.
                dev.primeants.PrimeAnts.LOGGER.info("T21 race publication ready={} claimants={}",FlowerNectar.get(l).ready(l,source[0]),java.util.stream.Stream.of(a,b).flatMap(q->f.workers(c,q).stream().filter(w->q.founding().claimedBy(w))).map(w->FoodCompetitionWindow.state(c,w,source[0])).toList());
            }
            if(!supplied[0])return;
            java.util.stream.Stream.concat(f.workers(c,a).stream(),f.workers(c,b).stream()).filter(w->source[0].equals(w.workerTasks().flowerSource())).forEach(w->approached.add(w.getUUID()));
            if(c.getTick()%20==0&&approached.size()<2)dev.primeants.PrimeAnts.LOGGER.info("T21 race diagnosis tick={} ready={} claimants={}",c.getTick(),FlowerNectar.get(l).ready(l,source[0]),java.util.stream.Stream.of(a,b).flatMap(q->f.workers(c,q).stream().filter(w->q.founding().claimedBy(w))).map(w->FoodCompetitionWindow.state(c,w,source[0])).toList());
            long count=FlowerNectar.get(l).harvestedSources().getOrDefault(source[0],0L);if(count>0&&firstHarvest[0]<0)firstHarvest[0]=l.getGameTime();
            long held=java.util.stream.Stream.concat(f.workers(c,a).stream(),f.workers(c,b).stream()).filter(w->w.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2)).mapToInt(w->w.getMainHandItem().getCount()).sum();
            long cached=java.util.stream.Stream.of(f.cache(c,a),f.cache(c,b)).filter(Objects::nonNull).flatMap(n->n.contents().stream()).filter(s->s.is(AntItems.FLOWER_NECTAR_V2)).mapToInt(ItemStack::getCount).sum();
            long world=l.getEntitiesOfClass(ItemEntity.class,c.getBounds(),i->i.isAlive()&&i.getItem().is(AntItems.FLOWER_NECTAR_V2)).stream().mapToInt(i->i.getItem().getCount()).sum();
            c.assertTrue(count==held+cached+world+consumed(l,a)+consumed(l,b),"One shared source is counted once against both real colonies' cargo/cache/consumption");
            if(firstHarvest[0]>=0&&l.getGameTime()-firstHarvest[0]>100){c.assertTrue(count==1&&approached.size()==2,"Two genuinely emerged mature foragers independently approached same ready source; only one wins before cooldown: approaches="+approached+" harvests="+count);l.setBlock(source[0],Blocks.AIR.defaultBlockState(),3);c.succeed();}
        });
    }
    @GameTest(maxTicks=18000)
    public void fullStorageRestorationAndOrdinaryDeathConserveHarvestedCargo(GameTestHelper c){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,3520);boolean[] filled={false},killed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null||killed[0])return;balance(c,l,q);
            var ws=workers(l,q);var p=q.founding().plan();
            if(!filled[0]&&ws.stream().anyMatch(w->w.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2))){
                filled[0]=true;ws.stream().filter(w->!q.founding().claimedBy(w)).forEach(w->w.setNoAi(true));
                // Explicit negative full-storage fixture, separate from nectar accounting: a cache full within the food shares
                // (four sugar units at their share, two protein units). T07: a saved cache over a share sheds for the other kind.
                l.setBlock(p.cache(),dev.primeants.brood.NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var n=cache(l,q);
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());
                out.putString("Colony",q.getUUID().toString());out.store("Entrance",BlockPos.CODEC,p.entrance());out.putString("Direction",p.direction().getName());
                out.store("Contents",ItemStack.CODEC.listOf(),java.util.stream.IntStream.range(0,6).mapToObj(i->new ItemStack(i<4?Items.APPLE:Items.CHICKEN)).toList());
                n.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()));n.setChanged();
            }
            if(filled[0])for(var w:ws)if(w.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2)&&w.workerTasks().reason().equals("cache_full_blocked_or_foreign_cargo_retained")){
                killed[0]=true;var source=flower(l,q);int cooldown=FlowerNectar.get(l).remaining(source);var loaded=restore(c,l,w);reloadSources(l);
                c.assertTrue(cache(l,q).size()==6&&physical(l,q)==1&&FlowerNectar.get(l).remaining(source)==cooldown,"Full storage and disk/entity restoration retain sole nectar cargo and exact source state");
                c.runAfterDelay(40,()->{
                    c.assertTrue(loaded.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2)&&physical(l,q)==1,"Full storage retries retain cargo");
                    loaded.hurtServer(l,loaded.damageSources().generic(),1000);
                    c.runAfterDelay(40,()->{balance(c,l,q);c.assertTrue(physical(l,q)==1&&harvests(l,q)==1&&loaded.getMainHandItem().isEmpty()&&loaded.workerTasks().phase()==WorkerTasks.Phase.DEAD,"Ordinary death releases one canonical cargo through normal world/custody, no source refund or worker replacement");c.succeed();});
                });return;
            }
        });
    }
    static List<LasiusNigerEntity> workers(ServerLevel l,LasiusNigerEntity q){return l.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(q.founding().plan().chamber()).inflate(32),w->w.isAlive()&&q.getUUID().equals(w.queenId()));}
    static BroodPile pile(ServerLevel l,LasiusNigerEntity q){return l.getBlockEntity(q.founding().plan().nursery()) instanceof BroodPile p?p:null;}
    static NestCache cache(ServerLevel l,LasiusNigerEntity q){return l.getBlockEntity(q.founding().plan().cache()) instanceof NestCache n?n:null;}
    static long consumed(ServerLevel l,LasiusNigerEntity q){var p=pile(l,q);return q.nutrition().nectar()+(p==null?0:p.consumedNectar())+AdultReceipts.nectar(l,q);}
    static long physical(ServerLevel l,LasiusNigerEntity q){
        var box=new AABB(q.founding().plan().outside()).inflate(32);var n=cache(l,q);
        return workers(l,q).stream().filter(w->w.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2)).mapToInt(w->w.getMainHandItem().getCount()).sum()
            +(n==null?0:n.contents().stream().filter(s->s.is(AntItems.FLOWER_NECTAR_V2)).mapToInt(ItemStack::getCount).sum())
            +l.getEntitiesOfClass(ItemEntity.class,box,i->i.isAlive()&&i.getItem().is(AntItems.FLOWER_NECTAR_V2)).stream().mapToInt(i->i.getItem().getCount()).sum()
            +TransferCustody.get(l).contents().stream().filter(t->box.contains(t.position())&&t.stack().is(AntItems.FLOWER_NECTAR_V2)).mapToInt(t->t.stack().getCount()).sum();
    }
    static long harvests(ServerLevel l,LasiusNigerEntity q){var box=new AABB(q.founding().plan().outside()).inflate(24,3,24);return FlowerNectar.get(l).harvestedSources().entrySet().stream().filter(e->box.contains(net.minecraft.world.phys.Vec3.atCenterOf(e.getKey()))).mapToLong(Map.Entry::getValue).sum();}
    static void balance(GameTestHelper c,ServerLevel l,LasiusNigerEntity q){c.assertTrue(harvests(l,q)==physical(l,q)+consumed(l,q),"Source harvests = actual held/cache/world/custody + terminal consumption; readiness and receipts are NOT stock");}
    static BlockPos flower(ServerLevel l,LasiusNigerEntity q){
        var origin=q.founding().plan().outside();
        for(var p:BlockPos.betweenClosed(origin.offset(-24,-3,-24),origin.offset(24,3,24)))if(NestPlan.loaded(l,p)&&FlowerNectar.habitat(l,p))return p.immutable();
        throw new AssertionError("One actual generation flower expected in controlled fixture");
    }
    static void reloadSources(ServerLevel l){
        l.getDataStorage().saveAndJoin();
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
            var data=disk.get(FlowerNectar.TYPE);if(data==null)throw new AssertionError("Actual disk nectar record");l.getDataStorage().set(FlowerNectar.TYPE,data);
        }
    }
    static LasiusNigerEntity restore(GameTestHelper c,ServerLevel l,LasiusNigerEntity w){
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(w.save(out),"Normal worker serialization");
        var tag=out.buildResult();var id=w.getUUID();var cargo=w.getMainHandItem().copy();w.discard();
        var loaded=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),tag),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
        c.assertTrue(loaded!=null&&id.equals(loaded.getUUID())&&ItemStack.matches(cargo,loaded.getMainHandItem())&&l.tryAddFreshEntityWithPassengers(loaded),"Only original saved identity/cargo reinserted");return loaded;
    }
    @GameTest(maxTicks=18000)
    public void generatedFlowerGenuineWorkerHarvestsReturnsAndNurseConsumes(GameTestHelper c){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,3200);boolean[] carried={false},delivered={false},callow={false};var starts=new HashMap<UUID,Long>();
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Uninterrupted controlled generation founding: "+q.founding().reason());balance(c,l,q);
            var p=pile(l,q);for(var w:workers(l,q)){
                callow[0]|=w.isCallow();if(w.workerTasks().harvestingTicks()>0)starts.putIfAbsent(w.getUUID(),w.elapsedAgeTicks());
                if(w.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2)){
                    c.assertTrue(!w.isCallow()&&p!=null&&p.original().contains(w.broodId()),"Only genuinely emerged mature members carry nectar");
                    if(!w.workerTasks().nursing()){carried[0]=true;c.assertTrue(starts.containsKey(w.getUUID())&&w.elapsedAgeTicks()-starts.get(w.getUUID())>=19,"Forager performs 20 actual loaded action ticks before one canonical item; nurse may later carry same item from cache");}
                }
                c.assertTrue(w.workerTasks().flowerInspections()<=WorkerTasks.FLOWER_INSPECTION_BUDGET,"Finite production inspection budget");
            }
            var n=cache(l,q);delivered[0]|=n!=null&&n.contents().stream().anyMatch(s->s.is(AntItems.FLOWER_NECTAR_V2));
            if(consumed(l,q)>0){
                var f=flower(l,q);c.assertTrue(carried[0]&&delivered[0]&&callow[0]&&NativeVegetation.get(l).eligible(l,f)&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN,"Intact witnessed generation flower, real carried item, physical cache delivery and accepted normal nurse feeding");
                c.assertTrue(q.nutrition().protein()==0&&q.nutrition().chickens()==0&&p.consumedChickens()==0,"Nectar cannot fall through chicken receipt or supply protein");
                c.assertTrue(q.nutrition().spentProtein()==0&&p.records().stream().allMatch(BroodRecord::founding),"Sugar alone cannot fund protein-requiring new brood");
                c.assertTrue(!q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN),"Actual nectar-fed sugar store cannot pay missing protein; failed spend changes nothing");
                dev.primeants.PrimeAnts.LOGGER.info("T17 nectar positive queen={} workers={} flower={} harvests={} physical={} consumed={}",q.getUUID(),workers(l,q).stream().map(LasiusNigerEntity::getUUID).toList(),f,harvests(l,q),physical(l,q),consumed(l,q));c.succeed();
            }
        });
    }
    @GameTest(maxTicks=18000)
    public void cooldownDiskRestoreKeepsCargoAndLoadedProgressAllowsNextHarvest(GameTestHelper c){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,3240);boolean[] saved={false},resumed={false};LasiusNigerEntity[] frozen={null};BlockPos[] source={null};int[] remaining={0};long[] started={0};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;balance(c,l,q);
            if(!saved[0])for(var w:workers(l,q))if(w.getMainHandItem().is(AntItems.FLOWER_NECTAR_V2)){
                saved[0]=true;source[0]=flower(l,q);remaining[0]=FlowerNectar.get(l).remaining(source[0]);started[0]=l.getGameTime();w.setNoAi(true);frozen[0]=restore(c,l,w);reloadSources(l);
                c.assertTrue(remaining[0]>0&&FlowerNectar.get(l).remaining(source[0])==remaining[0]&&!FlowerNectar.get(l).ready(l,source[0])&&physical(l,q)==1,"Real disk restoration retains unavailable source and exactly one interrupted carried unit");
            }
            if(saved[0]&&!resumed[0]){
                if(l.getGameTime()-started[0]<remaining[0])c.assertTrue(!FlowerNectar.get(l).ready(l,source[0])&&harvests(l,q)==1,"No early reharvest or offline catch-up after disk restore");
                if(FlowerNectar.get(l).remaining(source[0])==0){resumed[0]=true;c.assertTrue(l.getGameTime()-started[0]>=remaining[0],"Actual loaded source progression, production duration unaccelerated");frozen[0].setNoAi(false);}
            }
            if(resumed[0]&&harvests(l,q)>=2){balance(c,l,q);c.succeed();}
        });
    }
    @GameTest(maxTicks=18000)
    public void removedFlowerDuringRealActionYieldsNothing(GameTestHelper c){negative(c,3280,0);}
    @GameTest(maxTicks=18000)
    public void changedFlowerDuringRealActionYieldsNothing(GameTestHelper c){negative(c,3320,1);}
    @GameTest(maxTicks=18000)
    public void unreadyIdenticallyReplacedFlowerYieldsNothing(GameTestHelper c){negative(c,3360,2);}
    @GameTest(maxTicks=18000)
    public void inaccessibleFlowerYieldsNothing(GameTestHelper c){negative(c,3400,3);}
    private void negative(GameTestHelper c,int base,int kind){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,base);boolean[] changed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null||changed[0])return;
            for(var w:workers(l,q))if(kind<2?w.workerTasks().harvestingTicks()>=5:w.workerTasks().phase()==WorkerTasks.Phase.SEARCH){
                changed[0]=true;var f=flower(l,q);c.assertTrue(harvests(l,q)==0,"Intervention precedes first harvest");
                if(kind==0)l.setBlock(f,Blocks.AIR.defaultBlockState(),3);
                else if(kind==1)l.setBlock(f,Blocks.DANDELION.defaultBlockState(),3);
                else if(kind==2){c.assertTrue(FlowerNectar.get(l).ready(l,f),"One initially ready portion");l.setBlock(f,l.getBlockState(f),3);reloadSources(l);}
                else for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=3;y++)if(x!=0||z!=0||y==3)l.setBlock(f.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
                c.runAfterDelay(300,()->{c.assertTrue(harvests(l,q)==0&&physical(l,q)==0&&consumed(l,q)==0,"Removed, changed, unready or inaccessible source yields no remote food: kind="+kind);c.succeed();});return;
            }
        });
    }
    @GameTest(maxTicks=18000)
    public void interruptedActionRestoresWithoutPortionThenCompletesNormally(GameTestHelper c){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,3440);boolean[] interrupted={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;balance(c,l,q);
            if(!interrupted[0])for(var w:workers(l,q))if(w.workerTasks().harvestingTicks()>=5){
                interrupted[0]=true;var f=w.workerTasks().flowerSource();w.setNoAi(true);
                c.runAfterDelay(30,()->{
                    c.assertTrue(w.getMainHandItem().isEmpty()&&w.workerTasks().harvestingTicks()==0&&FlowerNectar.get(l).ready(l,f)&&harvests(l,q)==0,"Interrupted loaded work grants nothing and resets continuous action");
                    var loaded=restore(c,l,w);reloadSources(l);loaded.setNoAi(false);
                });
            }
            if(interrupted[0]&&consumed(l,q)>0){balance(c,l,q);c.succeed();}
        });
    }
    @GameTest(maxTicks=18000)
    public void outOfReachInterruptedActionCannotCompleteRemotely(GameTestHelper c){
        var l=level(c,"t17_nectar");var chunk=prepare(c,l,3480);boolean[] moved={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null||moved[0])return;
            for(var w:workers(l,q))if(w.workerTasks().harvestingTicks()>=5){
                moved[0]=true;var dest=q.founding().plan().outside().offset(-8,0,0);c.assertTrue(NestPlan.walkable(l,dest),"Explicit negative positioning uses existing supported exterior");
                var pos=net.minecraft.world.phys.Vec3.atBottomCenterOf(dest);w.teleportTo(pos.x,pos.y,pos.z);w.setOnGround(true);
                c.runAfterDelay(2,()->{c.assertTrue(harvests(l,q)==0&&w.getMainHandItem().isEmpty()&&w.workerTasks().harvestingTicks()==0,"Loss of reach resets real loaded action and prevents remote completion");c.succeed();});return;
            }
        });
    }
}
