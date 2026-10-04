package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Production eggs, brood, movement and transfers. Interventions are explicit negative cases only. */
public final class WorkerForagingGameTest {
    @GameTest(maxTicks=18000,structure="prime_ants_test:foraging_pair")
    public void competingEggFoundedWorkersCannotDuplicateSingleSourceStack(GameTestHelper c) {
        // Two independent real first generations share one stack. No supplied/teleported adults or forced tasks.
        var records=NaturalSoil.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalSoil.get(c.getLevel())).getOrThrow().getAsJsonObject();
        for(int x=1;x<=30;x++)for(int z=1;z<=30;z++)for(int y=1;y<=4;y++) {
            c.setBlock(x,y,z,Blocks.DIRT);records.addProperty(Long.toString(c.absolutePos(new BlockPos(x,y,z)).asLong()),"minecraft:dirt");
        }
        c.getLevel().getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,records).getOrThrow());
        var a=pairEgg(c,new BlockPos(8,4,10));var b=pairEgg(c,new BlockPos(20,4,10));boolean[] dropped={false};Set<UUID> searching=new HashSet<>(),approached=new HashSet<>();UUID[] source={null};
        c.onEachTick(()->{
            c.assertTrue(a.founding().phase()!=QueenFounding.Phase.FAILED&&b.founding().phase()!=QueenFounding.Phase.FAILED,"Both queens physically found independent nests");
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T08 competition tick={} queens={} workers={}",c.getTick(),List.of(a.position()+" "+a.founding().reason(),b.position()+" "+b.founding().reason()),java.util.stream.Stream.concat(workers(c,a).stream(),workers(c,b).stream()).map(w->w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()).toList());
            if(!dropped[0]&&a.founding().lifecycle()==QueenFounding.Lifecycle.OPEN&&b.founding().lifecycle()==QueenFounding.Lifecycle.OPEN) {
                // Wait until both actual foragers are on the surface before ordinary fixture source insertion.
                var wa=workers(c,a).stream().filter(w->w.getY()>a.founding().plan().entrance().getY()).findFirst();var wb=workers(c,b).stream().filter(w->w.getY()>b.founding().plan().entrance().getY()).findFirst();
                if(wa.isPresent()&&wb.isPresent()) {dropped[0]=true;searching.add(wa.get().getUUID());searching.add(wb.get().getUUID());source[0]=drop(c,c.absolutePos(new BlockPos(14,5,12)),new ItemStack(Items.APPLE)).getUUID();}
            }
            if(dropped[0]) {
                java.util.stream.Stream.concat(workers(c,a).stream(),workers(c,b).stream()).filter(w->source[0].equals(w.workerTasks().sourceId())&&w.workerTasks().phase()==WorkerTasks.Phase.APPROACH).forEach(w->approached.add(w.getUUID()));
                int total=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds(),i->i.isAlive()&&WorkerTasks.food(i.getItem())).stream().mapToInt(i->i.getItem().getCount()).sum()
                        +workers(c,a).stream().filter(w->WorkerTasks.food(w.getMainHandItem())).mapToInt(w->w.getMainHandItem().getCount()).sum()
                        +workers(c,b).stream().filter(w->WorkerTasks.food(w.getMainHandItem())).mapToInt(w->w.getMainHandItem().getCount()).sum()
                        +(cache(c,a)==null?0:cache(c,a).size())+(cache(c,b)==null?0:cache(c,b).size());
                c.assertTrue(total==1,"Single shared stack unit cannot duplicate between competing workers");
                if((cache(c,a)==null?0:cache(c,a).size())+(cache(c,b)==null?0:cache(c,b).size())==1) {
                    c.assertTrue(searching.size()==2&&approached.size()==2&&workers(c,a).size()==3&&workers(c,b).size()==3,"Two genuine mature foragers independently approach the same source identity, six brood-derived adults");c.succeed();
                }
            }
        });
    }
    private LasiusNigerEntity pairEgg(GameTestHelper c,BlockPos relative) {
        var player=new net.minecraft.world.entity.player.Player(c.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"foraging-fixture")) {
            public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.CREATIVE;}
            public boolean isClientAuthoritative(){return false;}
            public net.minecraft.server.permissions.PermissionSet permissions(){return net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS;}
        };
        var egg=new ItemStack(dev.primeants.item.AntItems.DEBUG_QUEEN_EGG);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,egg);BlockPos p=c.absolutePos(relative);
        c.assertTrue(egg.useOn(new net.minecraft.world.item.context.UseOnContext(c.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,egg,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(p).add(0,0.5,0),net.minecraft.core.Direction.UP,p,false))).consumesAction(),"Authorized ordinary queen egg");
        return c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,new AABB(p).inflate(1),a->a.form()==AntForm.QUEEN).getFirst();
    }
    private LasiusNigerEntity start(GameTestHelper c) { var f=new QueenFoundingGameTest();f.terrain(c,Blocks.DIRT.defaultBlockState(),true,true);return f.egg(c); }
    private List<LasiusNigerEntity> workers(GameTestHelper c,LasiusNigerEntity q) { return c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,c.getBounds().inflate(8),w->w.isAlive()&&q.getUUID().equals(w.queenId())); }
    private NestCache cache(GameTestHelper c,LasiusNigerEntity q) { return q.founding().plan()!=null&&c.getLevel().getBlockEntity(q.founding().plan().cache()) instanceof NestCache n?n:null; }
    private ItemEntity drop(GameTestHelper c,BlockPos p,ItemStack stack) { var v=Vec3.atBottomCenterOf(p);var i=new ItemEntity(c.getLevel(),v.x,v.y+0.1,v.z,stack,0,0,0);i.setUnlimitedLifetime();c.assertTrue(c.getLevel().addFreshEntity(i),"Real supported dropped stack insertion");return i; }
    private int foodTotal(GameTestHelper c,LasiusNigerEntity q) {
        int world=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&WorkerTasks.food(i.getItem())).stream().mapToInt(i->i.getItem().getCount()).sum();
        int held=workers(c,q).stream().filter(w->WorkerTasks.food(w.getMainHandItem())).mapToInt(w->w.getMainHandItem().getCount()).sum();
        return world+held+(cache(c,q)==null?0:cache(c,q).size());
    }
    private long mound(GameTestHelper c,NestPlan p) { return p.deposits().stream().filter(b->c.getLevel().getBlockState(b).is(NurseryBlocks.NEST_SOIL)).count(); }
    private void soilBalance(GameTestHelper c,LasiusNigerEntity q) {
        var p=q.founding().plan();if(p==null||q.founding().phase()!=QueenFounding.Phase.SETTLED)return;
        long plugs=p.plugs().stream().filter(b->c.getLevel().getBlockState(b).is(Blocks.DIRT)).count();
        long held=workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum();
        long drops=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum();
        c.assertTrue(mound(c,p)+plugs+held+drops==24,"Actual soil objects conserve 24: mound="+mound(c,p)+" plugs="+plugs+" held="+held+" dropped="+drops);
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void eggFoundedMatureWorkerOpensAndDeliversSugarAndProtein(GameTestHelper c) {
        var q=start(c);boolean[] dropped={false},sugar={false},protein={false},outside={false},callow={false};int[] lastOpen={0};
        c.onEachTick(()->{
            var f=q.founding();c.assertTrue(f.phase()!=QueenFounding.Phase.FAILED,"Founding failure "+f.reason());
            if(f.plan()==null)return;var p=f.plan();var ws=workers(c,q);
            for(var w:ws) {
                if(w.isCallow()) {callow[0]=true;c.assertTrue(w.workerTasks().phase()==WorkerTasks.Phase.NURSERY&&w.getY()<p.entrance().getY(),"Callow is sheltered until darkening");}
                if(w.getMainHandItem().is(Items.APPLE))sugar[0]=true;
                if(w.getMainHandItem().is(Items.CHICKEN))protein[0]=true;
                if(w.getY()>p.entrance().getY())outside[0]=true;
                c.assertTrue(c.getLevel().noCollision(w,w.getBoundingBox().deflate(0.001)),"Normal worker collision throughout trip");
            }
            int opened=ws.stream().mapToInt(w->w.workerTasks().opened()).sum();c.assertTrue(opened-lastOpen[0]<=1,"At most one physical plug action per tick");lastOpen[0]=opened;
            if(!dropped[0]&&f.sealed()) {dropped[0]=true;drop(c,p.at(-3,0,1),new ItemStack(Items.APPLE));drop(c,p.at(-4,0,1),new ItemStack(Items.CHICKEN));}
            if(dropped[0])c.assertTrue(foodTotal(c,q)==2,"World + mandibles + canonical cache conserve two food units");
            soilBalance(c,q);
            if(c.getTick()%300==0)PrimeAnts.LOGGER.info("T08 trace tick={} lifecycle={} ready={} workers={}",c.getTick(),f.lifecycle(),f.ready(),ws.stream().map(w->w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem()).toList());
            var n=cache(c,q);if(n!=null&&n.size()==2) {
                c.assertTrue(callow[0]&&sugar[0]&&protein[0]&&outside[0]&&ws.size()==3,"Observe genuine callows, crossing and both carried foods");
                c.assertTrue(f.ready()&&!f.sealed()&&f.lifecycle()==QueenFounding.Lifecycle.OPEN&&p.nurseryProblem(c.getLevel(),q.getUUID())!=null&&p.nurseryProblem(c.getLevel(),q.getUUID(),true)==null,"Operational opening is admitted; strict claustral validation still rejects open plugs");
                c.assertTrue(mound(c,p)==24&&p.plugs().stream().allMatch(b->c.getLevel().getBlockState(b).isAir()),"24 physical mound units");
                for(int step=0;step<3;step++)c.assertTrue(NestPlan.walkable(c.getLevel(),p.at(step,0,-step)),"Opened two-high supported entrance route");
                c.assertTrue(n.contents().stream().anyMatch(s->s.is(Items.APPLE))&&n.contents().stream().anyMatch(s->s.is(Items.CHICKEN))&&q.bodyReserve()==0,"Canonical sugary/protein storage does not refill reserves");
                PrimeAnts.LOGGER.info("T08 TRIP COMPLETE soil=24 mound/0 plugs/0 held/0 dropped food=0 world/0 held/2 cache capacity={} workers={}",NestCache.CAPACITY,ws.stream().map(LasiusNigerEntity::getUUID).toList());c.succeed();
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void sameStatePlayerPlugReplacementRevokesOpeningThroughDiskReload(GameTestHelper c) { protectedOpening(c,0); }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void missingHistoricalPlugOwnershipFailsClosed(GameTestHelper c) { protectedOpening(c,1); }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void prematurePlayerPlugRemovalCannotAuthorizeOperationalNest(GameTestHelper c) { protectedOpening(c,2); }
    private void protectedOpening(GameTestHelper c,int mode) {
        var q=start(c);boolean[] checked={false};
        c.onEachTick(()->{
            if(!checked[0]&&q.founding().sealed()&&workers(c,q).size()==3&&workers(c,q).stream().allMatch(LasiusNigerEntity::isCallow)) {
                checked[0]=true;var p=q.founding().plan();var target=p.plugs().getLast();
                c.assertTrue(ColonyPlugs.get(c.getLevel()).owned(c.getLevel(),target,q.getUUID()),"Actual successful queen placement owns plug");
                if(mode==0)c.getLevel().setBlock(target,Blocks.DIRT.defaultBlockState(),3);
                if(mode==1)p.plugs().forEach(ColonyPlugs.get(c.getLevel())::invalidate);
                if(mode==2)c.getLevel().setBlock(target,Blocks.AIR.defaultBlockState(),3);
                c.getLevel().getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(
                        net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),
                        net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())) {
                    var saved=disk.get(ColonyPlugs.TYPE);c.assertTrue(saved!=null&&!saved.owned(c.getLevel(),target,q.getUUID())&&!saved.opened(c.getLevel(),target,q.getUUID()),"Disk restoration does not invent missing/revoked ownership");c.getLevel().getDataStorage().set(ColonyPlugs.TYPE,saved);
                }
                c.runAfterDelay(500,()->{
                    c.assertTrue(workers(c,q).size()==3&&workers(c,q).stream().allMatch(w->w.workerTasks().opened()==0&&w.getMainHandItem().isEmpty())&&mound(c,p)==22,"Mature workers never remove unowned plug or mint soil");
                    c.assertTrue(q.founding().lifecycle()==QueenFounding.Lifecycle.CLAUSTRAL&&!ColonyPlugs.get(c.getLevel()).opened(c.getLevel(),target,q.getUUID()),"No inferred operational opening");
                    if(mode==2)c.assertTrue(!q.founding().ready()&&!q.founding().sealed(),"Premature player opening revokes readiness");
                    else c.assertTrue(c.getLevel().getBlockState(target).is(Blocks.DIRT),"Player/legacy dirt retained untouched");c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void operationalNestStillRejectsUnrelatedBreachAndObstruction(GameTestHelper c) {
        var q=start(c);boolean[] checked={false};
        c.onEachTick(()->{
            if(!checked[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN) {
                checked[0]=true;var p=q.founding().plan();c.assertTrue(q.founding().ready(),"Worker-created opening alone is operational");
                var block=p.at(5,1,-1);c.getLevel().setBlock(block,Blocks.STONE.defaultBlockState(),3);
                c.assertTrue(!q.founding().ready()&&q.founding().reason().endsWith("enclosure_chamber_obstructed"),"Open lifecycle retains obstruction guard");
                c.runAfterDelay(50,()->{
                    c.getLevel().setBlock(block,Blocks.AIR.defaultBlockState(),3);c.assertTrue(q.founding().ready(),"Removing test obstacle restores valid recorded opening");
                    c.getLevel().setBlock(p.at(4,2,-2),Blocks.AIR.defaultBlockState(),3);
                    c.runAfterDelay(50,()->{
                        c.assertTrue(!q.founding().ready()&&q.founding().reason().endsWith("enclosure_shell_open")&&mound(c,p)==24,"Unrelated wall breach remains invalid and unrepaired");
                        c.getLevel().setBlock(p.at(4,2,-2),NurseryBlocks.NEST_SOIL.defaultBlockState(),3);c.assertTrue(q.founding().ready(),"Geometry restored only by explicit negative-test intervention");
                        BlockPos floor=p.at(2,0,-3);var floorState=c.getLevel().getBlockState(floor);c.getLevel().setBlock(floor,Blocks.AIR.defaultBlockState(),3);
                        c.assertTrue(!q.founding().ready(),"Missing floor beneath worker-opened throat must invalidate operational walkability");c.getLevel().setBlock(floor,floorState,3);
                        BlockPos corridor=p.at(1,1,-1);var corridorState=c.getLevel().getBlockState(corridor);c.getLevel().setBlock(corridor,Blocks.AIR.defaultBlockState(),3);
                        c.assertTrue(!q.founding().ready(),"An unrelated underground corridor breach is not the recorded worker opening");c.getLevel().setBlock(corridor,corridorState,3);
                        c.getLevel().setBlock(p.plugs().getFirst(),Blocks.AIR.defaultBlockState(),3);
                        c.assertTrue(!q.founding().ready()&&!ColonyPlugs.get(c.getLevel()).opened(c.getLevel(),p.plugs().getFirst(),q.getUUID()),"Even same-air write revokes historical worker opening authorization");c.succeed();
                    });
                });
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void impassableBarrierPreventsRemoteFoodPickup(GameTestHelper c) {
        var q=start(c);boolean[] checked={false};
        c.onEachTick(()->{
            if(!checked[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN) {
                checked[0]=true;var p=q.founding().plan();BlockPos food=p.at(-3,0,1);
                // Closed three-high box with roof; an ordinary reachable stack is not supplied in this negative case.
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=3;y++)if(x!=0||z!=0||y==3)c.getLevel().setBlock(food.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
                drop(c,food,new ItemStack(Items.APPLE,2));
                c.runAfterDelay(1000,()->{c.assertTrue(foodTotal(c,q)==2&&cache(c,q)==null&&workers(c,q).stream().allMatch(w->w.getMainHandItem().isEmpty()),"Enclosed food stays in actual world, no through-wall or remote collection");c.succeed();});
            }
        });
    }
    private LasiusNigerEntity restore(GameTestHelper c,LasiusNigerEntity ant) {
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());c.assertTrue(ant.save(out),"Normal canonical entity serialization");var tag=out.buildResult();UUID id=ant.getUUID();var stack=ant.getMainHandItem().copy();var phase=ant.workerTasks().phase();ant.discard();
        var loaded=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
        c.assertTrue(loaded!=null&&loaded.getUUID().equals(id)&&ItemStack.matches(stack,loaded.getMainHandItem())&&loaded.workerTasks().phase()==phase&&c.getLevel().tryAddFreshEntityWithPassengers(loaded),"One original identity, phase and component-preserving canonical cargo restored");return loaded;
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void blockedCacheRetainsFoodThroughRestoreAndPhysicalRetry(GameTestHelper c) {
        var q=start(c);boolean[] dropped={false},blocked={false},released={false};
        c.onEachTick(()->{
            var p=q.founding().plan();if(p==null)return;
            if(!dropped[0]&&q.founding().sealed()){dropped[0]=true;drop(c,p.at(-3,0,1),new ItemStack(Items.APPLE,2));}
            if(dropped[0])c.assertTrue(foodTotal(c,q)==2,"Blocked/restored cache conserves actual food objects");
            if(!blocked[0]) {
                var carrying=workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.APPLE)).findFirst();
                if(carrying.isPresent()) {
                    blocked[0]=true;var w=restore(c,carrying.get());c.getLevel().setBlock(p.cache(),Blocks.STONE.defaultBlockState(),3);
                    c.runAfterDelay(100,()->{
                        c.assertTrue(w.getMainHandItem().getCount()==1&&cache(c,q)==null&&!q.founding().ready()&&w.workerTasks().reason().equals("home_unavailable_or_invalid_cargo_retained"),"Blocked destination retains restored cargo without remote transfer");
                        c.getLevel().setBlock(p.cache(),Blocks.AIR.defaultBlockState(),3);released[0]=true;
                    });
                }
            }
            var n=cache(c,q);if(released[0]&&n!=null&&n.size()==2) {c.assertTrue(q.founding().ready()&&foodTotal(c,q)==2&&workers(c,q).size()==3,"Physical retry after explicit obstacle removal delivers same two units");c.succeed();}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void restoredOpeningCargoFoodAndCacheConserveAllObjects(GameTestHelper c) {
        LasiusNigerEntity[] q={start(c)};boolean[] soil={false},food={false},saved={false},dropped={false};
        var named=new ItemStack(Items.SWEET_BERRIES,2);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("physical berries"));
        c.onEachTick(()->{
            var p=q[0].founding().plan();if(p==null)return;
            if(!dropped[0]&&q[0].founding().sealed()) {dropped[0]=true;drop(c,p.at(-3,0,1),named.copy());}
            for(var w:workers(c,q[0])) {
                if(!soil[0]&&w.getMainHandItem().is(Items.DIRT)) {
                    soil[0]=true;c.getLevel().getDataStorage().saveAndJoin();
                    try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(
                            net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),
                            net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())) {
                        var plugData=disk.get(ColonyPlugs.TYPE);c.assertTrue(plugData!=null&&plugData.opened(c.getLevel(),p.plugs().getLast(),q[0].getUUID())&&plugData.owned(c.getLevel(),p.plugs().getFirst(),q[0].getUUID()),"Disk retains actual upper opening and lower colony placement, without another plug action");c.getLevel().getDataStorage().set(ColonyPlugs.TYPE,plugData);
                    }
                    restore(c,w);q[0]=restore(c,q[0]);break;
                }
                if(!food[0]&&w.getMainHandItem().is(Items.SWEET_BERRIES)) {food[0]=true;var loaded=restore(c,w);c.assertTrue(ItemStack.isSameItemSameComponents(named,loaded.getMainHandItem()),"Cargo components preserved");q[0]=restore(c,q[0]);break;}
            }
            if(dropped[0])c.assertTrue(foodTotal(c,q[0])==2,"Food conservation through cargo restoration");soilBalance(c,q[0]);
            var n=cache(c,q[0]);if(!saved[0]&&n!=null&&n.size()==2) {
                saved[0]=true;c.assertTrue(soil[0]&&food[0]&&n.contents().stream().allMatch(s->ItemStack.isSameItemSameComponents(named,s)),"Exact original components in physical cache");
                var chunk=c.getLevel().getChunkAt(p.cache());var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(c.getLevel(),chunk);
                var parsed=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(c.getLevel(),c.getLevel().palettedContainerFactory(),serial.write());var read=parsed.read(c.getLevel(),c.getLevel().getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",c.getLevel().dimension(),"chunk"),chunk.getPos());
                c.assertTrue(read.getBlockState(p.cache()).equals(n.getBlockState()),"FULL chunk retains visible inventory projection");
                var tag=n.saveWithFullMetadata(c.getLevel().registryAccess());var state=n.getBlockState();c.getLevel().removeBlockEntity(p.cache());var loaded=(NestCache)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(p.cache(),state,tag,c.getLevel().registryAccess());c.getLevel().setBlockEntity(loaded);
                c.runAfterDelay(80,()->{c.assertTrue(foodTotal(c,q[0])==2&&loaded.size()==2&&mound(c,p)==24&&workers(c,q[0]).size()==3&&q[0].founding().ready(),"Restored cache/home/cargo does not duplicate food, soil, plug actions or workers");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void fullCacheRetainsCargoAndDeathReleasesOnceReassigningLivingWorker(GameTestHelper c) {
        var q=start(c);boolean[] dropped={false},checked={false},done={false};
        c.onEachTick(()->{
            if(done[0])return; // succeed() clears entities before other due callbacks finish this same tick.
            var p=q.founding().plan();if(p==null)return;
            if(!dropped[0]&&q.founding().sealed()) {dropped[0]=true;drop(c,p.at(-3,0,1),new ItemStack(Items.APPLE,7));}
            if(dropped[0])c.assertTrue(foodTotal(c,q)==7,"All seven source units remain physical total="+foodTotal(c,q)+" world="+c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&WorkerTasks.food(i.getItem())).stream().map(i->i.getItem()+" at "+i.position()).toList()+" cache="+(cache(c,q)==null?List.of():cache(c,q).contents())+" workers="+workers(c,q).stream().map(w->w.getMainHandItem()+" at "+w.position()).toList());soilBalance(c,q);
            var n=cache(c,q);if(!checked[0]&&n!=null&&n.size()==6) {
                var actor=workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.APPLE)&&w.workerTasks().reason().equals("cache_full_blocked_or_foreign_cargo_retained")).findFirst();
                if(actor.isEmpty())return;checked[0]=true;UUID dead=actor.get().getUUID();var worker=actor.get();Set<UUID> survivors=new HashSet<>();workers(c,q).stream().filter(w->w!=worker).forEach(w->survivors.add(w.getUUID()));
                c.runAfterDelay(80,()->{
                    c.assertTrue(worker.getMainHandItem().getCount()==1&&n.size()==6&&foodTotal(c,q)==7,"Full cache retains real cargo through bounded retries");worker.hurtServer(c.getLevel(),worker.damageSources().generic(),1000);worker.die(worker.damageSources().generic());
                    c.runAfterDelay(120,()->{c.assertTrue(worker.isRemoved()&&worker.getMainHandItem().isEmpty()&&worker.workerTasks().phase()==WorkerTasks.Phase.DEAD&&foodTotal(c,q)==7&&workers(c,q).size()==2&&workers(c,q).stream().allMatch(w->survivors.contains(w.getUUID()))&&q.founding().workerClaim()!=null&&!dead.equals(q.founding().workerClaim()),"Single death cargo release and assignment to another existing living worker, no replacement spawn");PrimeAnts.LOGGER.info("T08 DEATH BALANCE source=7 actualTotal={} cache={} livingWorkers={} claim={}",foodTotal(c,q),n.size(),workers(c,q).size(),q.founding().workerClaim());done[0]=true;c.succeed();});
                });
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void blockedMoundRetainsRecoveredPlugSoilAndUnavailableHomeRetainsCargo(GameTestHelper c) {
        var q=start(c);boolean[] checked={false};
        c.onEachTick(()->{
            if(checked[0]||q.founding().plan()==null)return;
            var actor=workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.DIRT)).findFirst();if(actor.isEmpty())return;
            checked[0]=true;var w=actor.get();var p=q.founding().plan();for(var b:p.deposits())if(c.getLevel().getBlockState(b).isAir())c.getLevel().setBlock(b,Blocks.STONE.defaultBlockState(),3);
            c.runAfterDelay(100,()->{
                c.assertTrue(w.getMainHandItem().getCount()==1&&w.workerTasks().reason().equals("mound_full_or_blocked_soil_retained")&&mound(c,p)==22,"Blocked bounded mound retains existing plug unit with no normal item dumping");
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess());q.save(out);var tag=out.buildResult();q.discard();
                c.runAfterDelay(100,()->{
                    c.assertTrue(w.getMainHandItem().is(Items.DIRT)&&workers(c,q).size()==3&&w.workerTasks().reason().equals("home_unavailable_or_invalid_cargo_retained"),"Unavailable home cannot complete work remotely or spawn replacements");
                    var restored=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(restored),"Reinsert only original saved queen");soilBalance(c,restored);c.succeed();
                });
            });
        });
    }
}
