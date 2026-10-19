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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Physical clay progression. Controlled geology is explicitly separate from ordinary terrain evidence. */
public final class NaturalClayGameTest {
    @GameTest(maxTicks=20000)
    public void ordinaryOverworldClayCensus(GameTestHelper c){NativeClaySurvey.run(c);}
    @GameTest(maxTicks=20)
    public void legacyMiningJobsKeepTargetsOrderClaimsAndAccountingThroughDisk(GameTestHelper c){
        var l=c.getLevel();var data=new com.google.gson.JsonObject();int rows=0;
        var entrance=c.absolutePos(new BlockPos(0,0,0));var home=NestPlan.geometry(entrance,net.minecraft.core.Direction.EAST);
        for(int length=2;length<=16;length+=2)for(int done=0;done<=length;done++)for(int cargo=0;cargo<=Math.min(1,done);cargo++){
            var id=UUID.nameUUIDFromBytes(("legacy-clay:"+length+":"+done+":"+cargo).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var row=new com.google.gson.JsonObject();row.add("entrance",BlockPos.CODEC.encodeStart(JsonOps.INSTANCE,entrance).getOrThrow());row.addProperty("direction","east");
            var expected=new com.google.gson.JsonArray();var completed=new com.google.gson.JsonArray();
            for(int n=0;n<length;n++){expected.add(net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(JsonOps.INSTANCE,(n<2?Blocks.DIRT:MiningGameTest.RESOURCES.get((n-2)%7)).defaultBlockState()).getOrThrow());if(n<done){var cell=MiningShape.cells().get(n);completed.add(BlockPos.CODEC.encodeStart(JsonOps.INSTANCE,home.at(cell.forward(),cell.side(),cell.dy())).getOrThrow());}}
            row.add("expected",expected);row.add("completed",completed);row.addProperty("claim",cargo==1||done<length?id.toString():"");row.addProperty("deposited",done-cargo);row.addProperty("released",0);row.addProperty("ticks",37);row.addProperty("reason","legacy_original");
            var deliveries=new com.google.gson.JsonObject();for(int n=0;n<done-cargo;n++){String item=n<2?"minecraft:dirt":net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(MiningGameTest.MATERIALS.get((n-2)%7)).toString();deliveries.addProperty(item,deliveries.has(item)?deliveries.get(item).getAsInt()+1:1);}
            row.add("deliveries",deliveries);row.add("release_transfers",new com.google.gson.JsonObject());data.add(id.toString(),row);rows++;
        }
        var loaded=Mining.CODEC.parse(JsonOps.INSTANCE,data).getOrThrow();
        var folder=l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("legacy-clay-compatibility");
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(folder,net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){disk.set(Mining.TYPE,loaded);disk.saveAndJoin();}
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(folder,net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){loaded=Objects.requireNonNull(disk.get(Mining.TYPE));}
        c.assertTrue(data.equals(Mining.CODEC.encodeStart(JsonOps.INSTANCE,loaded).getOrThrow()),"All legacy partial, completed and cargo-bearing jobs retain their exact original schema and values after disk decoding");
        for(var entry:data.entrySet()){
            var row=entry.getValue().getAsJsonObject();var j=loaded.job(UUID.fromString(entry.getKey()));
            c.assertTrue(j.tasks.equals(MiningShape.cells().subList(0,j.expected.size()).stream().map(cell->home.at(cell.forward(),cell.side(),cell.dy())).toList())&&j.removed()==row.getAsJsonArray("completed").size()&&j.ticks==37,"Legacy implicit gallery target order and accumulated work remain original");
            if(j.removed()>j.deposited)c.assertTrue(j.claim!=null&&j.pending(new ItemStack(j.removed()<=2?Items.DIRT:MiningGameTest.MATERIALS.get((j.removed()-3)%7))),"Every legacy material's one-unit cargo retains its original claim and unsettled accounting");
        }
        var maximum=data.entrySet().iterator().next().getValue().getAsJsonObject().deepCopy();
        var targets=new ArrayList<BlockPos>();
        for(int side=0;side<4;side++)for(int n=0;n<8;n++){int forward=side%2==0?8+n:15-n;targets.add(home.at(forward,side,-2));targets.add(home.at(forward,side,-1));}
        maximum.add("targets",BlockPos.CODEC.listOf().encodeStart(JsonOps.INSTANCE,targets).getOrThrow());maximum.add("completed",maximum.get("targets").deepCopy());
        maximum.add("expected",net.minecraft.world.level.block.state.BlockState.CODEC.listOf().encodeStart(JsonOps.INSTANCE,Collections.nCopies(64,Blocks.CLAY.defaultBlockState())).getOrThrow());
        maximum.addProperty("claim","");maximum.addProperty("deposited",64);var fullDeliveries=new com.google.gson.JsonObject();fullDeliveries.addProperty("minecraft:clay_ball",64);maximum.add("deliveries",fullDeliveries);
        var capData=new com.google.gson.JsonObject();var capOwner=UUID.nameUUIDFromBytes("clay-cap".getBytes(java.nio.charset.StandardCharsets.UTF_8));capData.add(capOwner.toString(),maximum);
        var capped=Mining.CODEC.parse(JsonOps.INSTANCE,capData).getOrThrow().job(capOwner);c.assertTrue(capped.removed()==64&&capped.complete()&&capped.cap()==64,"All connected gallery branches share exactly 64 cumulative successful removals");
        for(var invalid:List.of(home.at(8,4,-2),home.at(17,0,-2),home.at(8,0,-7))){
            var bad=capData.deepCopy();var row=bad.getAsJsonObject(capOwner.toString());
            if(invalid.equals(home.at(8,4,-2))){targets.add(invalid);targets.add(invalid.above());row.add("targets",BlockPos.CODEC.listOf().encodeStart(JsonOps.INSTANCE,targets).getOrThrow());row.add("expected",net.minecraft.world.level.block.state.BlockState.CODEC.listOf().encodeStart(JsonOps.INSTANCE,Collections.nCopies(66,Blocks.CLAY.defaultBlockState())).getOrThrow());}
            else row.getAsJsonArray("targets").set(63,BlockPos.CODEC.encodeStart(JsonOps.INSTANCE,invalid).getOrThrow());
            row.add("completed",row.get("targets").deepCopy());
            boolean rejected=false;try{Mining.CODEC.parse(JsonOps.INSTANCE,bad).getOrThrow();}catch(RuntimeException expected){rejected=true;}
            c.assertTrue(rejected,"Reload rejects cumulative budget overflow, radius escape and seventh-depth targets");
        }
        PrimeAnts.LOGGER.info("CLAY LEGACY ACCEPTED jobs={} lengths=2..16 partial=true completed=true cargo=true disk=true",rows);c.succeed();
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void controlledClayYoungColonyCompletesTierTwoWithoutMaterialGifts(GameTestHelper c){
        var fx=new NestPlanFixture();var q=fx.start(c);boolean[] geology={false};
        var run=new Progress(c,fx,q);
        c.onEachTick(()->{
            if(q.founding().plan()!=null&&!geology[0]){
                geology[0]=true;controlledGeology(c,q);run.census();
            }
            run.tick();
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void youngClayMinerDescendsOnSupportedStepsAndDeliversDeepNaturalUnits(GameTestHelper c){
        var fx=new NestPlanFixture();var run=new Progress(c,fx,fx.start(c));boolean[] geology={false};long[] settled={-1};
        c.onEachTick(()->{
            var q=run.q;var l=c.getLevel();
            if(q.founding().plan()!=null&&!geology[0]){geology[0]=true;controlledGeology(c,q,-4);run.census();}
            run.tick();var j=Mining.get(l).job(q.getUUID());if(j==null)return;
            if(run.delivered<3)return;
            c.assertTrue(j.completed().stream().anyMatch(p->p.getY()<=j.home.entrance().getY()-4)&&run.removed.stream().allMatch(p->p.getY()<=j.home.entrance().getY()-3),"Actual worker edits and clay receipts descend to the declared deep deposit");
            var pile=NestPlanFixture.pile(c,q);c.assertTrue(pile.stageEvaluation().stage()==ColonyStage.YOUNG,"Deep clay acquisition occurs before Mature");
            if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("CLAY DEEP ACCEPTED tick={} removed={} delivered={} tasks={} ledgerRemoved={} spatial=true cumulativeCap={}",c.getTick(),run.removed,run.delivered,j.tasks,j.removed(),j.cap());c.succeed();
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void youngClayMinerUsesPhysicalBankAccessAndKeepsCargoThroughDiskReadback(GameTestHelper c){
        var fx=new NestPlanFixture();var run=new Progress(c,fx,fx.start(c));boolean[] geology={false},readback={false};long[] settled={-1};
        c.onEachTick(()->{
            var l=c.getLevel();var q=run.q;var p=q.founding().plan();
            if(p!=null&&!geology[0]){
                geology[0]=true;controlledGeology(c,q);
                // A declared unowned barrier makes the store connection unavailable for Young access.
                l.setBlock(p.at(9,0,-2),NurseryBlocks.NEST_SOIL.defaultBlockState(),3);l.setBlock(p.at(9,0,-1),NurseryBlocks.NEST_SOIL.defaultBlockState(),3);
                for(int f=16;f<=20;f++)for(int side=-1;side<=1;side++)for(int y=-3;y<=0;y++)l.setBlock(p.at(f,side,y),Blocks.DIRT.defaultBlockState(),3);
                for(int f=16;f<=19;f++)for(int y=f-18;y<=2;y++)l.setBlock(p.at(f,0,y),Blocks.AIR.defaultBlockState(),3);
                run.census();
            }
            run.tick();var j=Mining.get(l).job(q.getUUID());if(j==null)return;
            c.assertTrue(!j.viaFoundingChamber()&&!j.tasks.contains(p.at(9,0,-2))&&!j.tasks.contains(p.at(9,0,-1)),"A real bank route never authorizes or removes the foreign store barrier");
            var w=j.claim==null?null:(LasiusNigerEntity)l.getEntity(j.claim);
            if(w!=null&&c.getTick()%500==0){var path=w.getNavigation().getPath();var end=path==null||path.getNodeCount()==0?null:path.getNode(path.getNodeCount()-1);PrimeAnts.LOGGER.info("CLAY BANK ROUTE tick={} worker={} position={} target={} next={} end={} canReach={}",c.getTick(),w.getUUID(),w.position(),j.removed()<j.tasks.size()?j.tasks.get(j.removed()):null,path==null||path.isDone()?null:path.getNextNodePos(),end==null?null:new BlockPos(end.x,end.y,end.z),path!=null&&path.canReach());}
            if(!readback[0]&&w!=null&&w.getMainHandItem().is(Items.CLAY_BALL)){
                var before=Mining.CODEC.encodeStart(JsonOps.INSTANCE,Mining.get(l)).getOrThrow();var cargo=w.getMainHandItem().copy();l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){l.getDataStorage().set(Mining.TYPE,Objects.requireNonNull(disk.get(Mining.TYPE)));}
                c.assertTrue(before.equals(Mining.CODEC.encodeStart(JsonOps.INSTANCE,Mining.get(l)).getOrThrow()),"Actual disk readback retains bank access, target order, original claim and cumulative accounting");
                var restored=fx.f.restore(c,w);c.assertTrue(restored.getUUID().equals(j.claim)&&ItemStack.matches(cargo,restored.getMainHandItem()),"The original bank carrier retains its exact unit through normal reconstruction");readback[0]=true;
            }
            if(run.delivered<3||!readback[0])return;var pile=NestPlanFixture.pile(c,q);if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("CLAY BANK ACCEPTED tick={} removed={} delivered={} access={} targets={} reload=true foreignBarrierIntact=true",c.getTick(),run.removed,run.delivered,j.connections(),j.tasks);c.succeed();
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void youngClayCargoSurvivesDiskAndActorReadbackThenDeathReleasesExactlyOnce(GameTestHelper c){
        var fx=new NestPlanFixture();var run=new Progress(c,fx,fx.start(c));boolean[] geology={false};int[] phase={0},baseline={0};long[] since={0};
        var recovery=NamedMaterialRecovery.watch(c,run.q.getUUID());
        UUID[] carrier={null},transfer={null};UnavailableCells[] hidden={null};
        c.onEachTick(()->{
            var l=c.getLevel();var q=run.q;
            if(q.founding().plan()!=null&&!geology[0]){geology[0]=true;controlledGeology(c,q);run.census();}
            run.tick();var j=Mining.get(l).job(run.q.getUUID());if(j==null)return;
            var w=j.claim==null?null:(LasiusNigerEntity)l.getEntity(j.claim);
            if(phase[0]==0&&w!=null&&w.getMainHandItem().is(Items.CLAY_BALL)){
                var encoded=Mining.CODEC.encodeStart(JsonOps.INSTANCE,Mining.get(l)).getOrThrow();var cargo=w.getMainHandItem().copy();carrier[0]=w.getUUID();baseline[0]=j.deposited;
                var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());var tag=store.saveWithFullMetadata(l.registryAccess());l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    l.getDataStorage().set(Mining.TYPE,Objects.requireNonNull(disk.get(Mining.TYPE)));l.getDataStorage().set(NaturalMaterials.TYPE,Objects.requireNonNull(disk.get(NaturalMaterials.TYPE)));l.getDataStorage().set(NaturalSoil.TYPE,Objects.requireNonNull(disk.get(NaturalSoil.TYPE)));l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));
                }
                c.assertTrue(encoded.equals(Mining.CODEC.encodeStart(JsonOps.INSTANCE,Mining.get(l)).getOrThrow()),"Actual disk preserves explicit route, successful edits, unit accounting and claim");
                var restored=fx.f.restore(c,w);run.q=fx.f.restore(c,q);MiningGameTest.restoreStore(c,store,tag);
                c.assertTrue(restored.getUUID().equals(carrier[0])&&ItemStack.matches(cargo,restored.getMainHandItem())&&carrier[0].equals(Mining.get(l).job(run.q.getUUID()).claim),"Normal actor reconstruction retains exact canonical one-unit clay cargo and original claim");
                PrimeAnts.LOGGER.info("CLAY RELOAD tick={} carrier={} removed={} deposited={} cargo={}",c.getTick(),carrier[0],j.removed(),j.deposited,cargo);phase[0]=1;return;
            }
            if(phase[0]==1&&j.deposited>baseline[0]&&w!=null&&w.getMainHandItem().is(Items.CLAY_BALL)){
                c.assertTrue(w.getUUID().equals(carrier[0]),"Reloaded original miner delivered and extracted the next physical unit");
                transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+w.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));hidden[0]=UnavailableCells.hide(c,w.blockPosition());since[0]=c.getTick();baseline[0]=j.removed();
                w.hurtServer(l,w.damageSources().genericKill(),1000);w.workerTasks().die(l);
                c.assertTrue(j.released==1&&j.transfers().equals(Map.of(transfer[0].toString(),"minecraft:clay_ball"))&&j.claim==null,"Death hands the named unit to existing custody exactly once");
                phase[0]=2;PrimeAnts.LOGGER.info("CLAY DEATH tick={} carrier={} transfer={} custody={}",c.getTick(),carrier[0],transfer[0],TransferCustody.get(l).contents());return;
            }
            if(phase[0]==2){
                c.assertTrue(j.released==1&&j.transfers().size()==1&&TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])&&t.stack().is(Items.CLAY_BALL)&&t.stack().getCount()==1).count()==1&&l.getEntity(transfer[0])==null,"Unavailable destination retains one exact custody unit after repeated death cleanup");
                if(c.getTick()-since[0]<100)return;hidden[0].close();hidden[0]=null;phase[0]=3;return;
            }
            if(phase[0]==3&&j.removed()>baseline[0]&&j.deposited+j.released==j.removed()){
                var item=l.getEntity(transfer[0]);var pickups=recovery.pickups(transfer[0]);var deposits=recovery.deposits(transfer[0]);
                c.assertTrue(j.released==1&&pickups.size()<=1&&deposits.size()<=1,"Existing custody and pickup never create a second released unit");
                if(!(item instanceof ItemEntity)&&pickups.isEmpty())return;
                if(item instanceof ItemEntity drop)c.assertTrue(drop.getItem().is(Items.CLAY_BALL)&&drop.getItem().getCount()==1,"The named custody drop is exactly one clay unit");
                if(!pickups.isEmpty())c.assertTrue(pickups.getFirst().cargo().is(Items.CLAY_BALL)&&pickups.getFirst().cargo().getCount()==1,"The named physical drop was picked up as exactly one clay unit");
                PrimeAnts.LOGGER.info("CLAY RELOAD DEATH ACCEPTED tick={} originalCarrier={} transfer={} removed={} deposited={} released={} pickups={} deposits={}",c.getTick(),carrier[0],transfer[0],j.removed(),j.deposited,j.released,pickups,deposits);c.succeed();
            }
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void youngClayRefusesUnavailableFluidUnsafeAndSameStateReplacementTargets(GameTestHelper c){
        var fx=new NestPlanFixture();var run=new Progress(c,fx,fx.start(c));boolean[] geology={false};int[] phase={0},baseline={0};long[] since={0};
        BlockPos[] target={null},shell={null};net.minecraft.world.level.block.state.BlockState[] support={null};UnavailableCells[] hidden={null};UUID[] carrier={null};
        c.onEachTick(()->{
            var l=c.getLevel();var q=run.q;
            if(q.founding().plan()!=null&&!geology[0]){geology[0]=true;controlledGeology(c,q);run.census();}
            run.tick();var j=Mining.get(l).job(q.getUUID());if(j==null)return;
            if(phase[0]==0){
                if(run.removed.isEmpty()||j.claim==null||j.removed()>=j.tasks.size()||j.removed()!=j.deposited)return;
                target[0]=j.tasks.get(j.removed());carrier[0]=j.claim;baseline[0]=j.removed();hidden[0]=UnavailableCells.hide(c,target[0]);since[0]=c.getTick();phase[0]=1;return;
            }
            c.assertTrue(j.removed()==baseline[0]&&j.released==0,"Refusal never removes or credits another unit");
            if(phase[0]<4)c.assertTrue(carrier[0].equals(j.claim),"The existing empty miner retains its actual claim while a temporary guard refuses work");
            if(c.getTick()-since[0]<120)return;
            if(phase[0]==1){
                c.assertTrue(j.removalProblem(l,q.getUUID()).contains("unavailable"),"Unavailable target is explicitly refused");hidden[0].close();hidden[0]=null;
                shell[0]=target[0].getY()==j.home.entrance().getY()-2?target[0].below():target[0].above();support[0]=l.getBlockState(shell[0]);l.setBlock(shell[0],Blocks.AIR.defaultBlockState(),3);since[0]=c.getTick();phase[0]=2;return;
            }
            if(phase[0]==2){
                c.assertTrue(j.removalProblem(l,q.getUUID()).contains("support"),"Missing support is refused");l.setBlock(shell[0],support[0],3);l.setBlock(shell[0],Blocks.WATER.defaultBlockState(),3);since[0]=c.getTick();phase[0]=3;return;
            }
            if(phase[0]==3){
                c.assertTrue(j.removalProblem(l,q.getUUID()).contains("fluid"),"Fluid neighbor is refused without draining it");l.setBlock(shell[0],support[0],3);
                var same=l.getBlockState(target[0]);c.assertTrue(NaturalMaterials.get(l).eligible(l,target[0]),"The negative starts with positive clay authority");l.setBlock(target[0],same,3);
                c.assertTrue(!NaturalMaterials.get(l).eligible(l,target[0]),"Same-state ordinary player write revokes natural authority");since[0]=c.getTick();phase[0]=4;return;
            }
            if(phase[0]==4){
                c.assertTrue(j.stopped()&&!j.compatible(l,q.getUUID(),target[0],j.expected.get(j.removed()))&&l.getBlockState(target[0]).equals(j.expected.get(j.removed())),"Same-state replacement permanently stops the exact original target without extracting its unit");
                l.setBlock(target[0],NurseryBlocks.NEST_SOIL.defaultBlockState(),3);since[0]=c.getTick();phase[0]=5;return;
            }
            c.assertTrue(j.stopped()&&!ColonyTerrain.get(l).eligible(l,target[0],q.getUUID())&&!j.compatible(l,q.getUUID(),target[0],l.getBlockState(target[0])),"Foreign unowned colony-soil terrain never gains permission from block type or a saved route");
            PrimeAnts.LOGGER.info("CLAY GUARDS ACCEPTED tick={} worker={} target={} removed={} delivered={} reason={}",c.getTick(),carrier[0],target[0],j.removed(),j.deposited,j.reason);c.succeed();
        });
    }
    static void controlledGeology(GameTestHelper c,LasiusNigerEntity q){controlledGeology(c,q,-2);}
    static void controlledGeology(GameTestHelper c,LasiusNigerEntity q,int floor){
        var l=c.getLevel();var p=q.founding().plan();
        var soil=NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        var materials=NaturalMaterials.CODEC.encodeStart(JsonOps.INSTANCE,NaturalMaterials.get(l)).getOrThrow().getAsJsonObject();
        for(int f=8;f<=16;f++)for(int s=-4;s<=4;s++)for(int y=floor-1;y<=0;y++){
            // The existing room envelope and its wall cells stay untouched.
            if(f<=10&&s!=0)continue;
            var at=p.at(f,s,y);var state=(f>=11&&f<=15&&Math.abs(s)<=3&&(y==floor||y==floor+1)?Blocks.CLAY:Blocks.DIRT).defaultBlockState();
            l.setBlock(at,state,3);var key=Long.toString(at.asLong());soil.remove(key);materials.remove(key);
            if(NaturalSoil.material(state))soil.addProperty(key,NaturalMaterials.type(state));
            if(state.is(Blocks.CLAY))materials.addProperty(key,NaturalMaterials.type(state));
        }
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(JsonOps.INSTANCE,soil).getOrThrow());
        l.getDataStorage().set(NaturalMaterials.TYPE,NaturalMaterials.CODEC.parse(JsonOps.INSTANCE,materials).getOrThrow());
        PrimeAnts.LOGGER.info("CLAY CONTROLLED GEOLOGY fixtureOnly=true queen={}",q.getUUID());
    }
    static final class Progress {
        final GameTestHelper c;final NestPlanFixture fx;LasiusNigerEntity q;
        final net.minecraft.server.level.ServerLevel level;
        final Set<BlockPos> original=new LinkedHashSet<>(),removed=new LinkedHashSet<>(),paid=new LinkedHashSet<>();
        final Map<BlockPos,UUID> carriers=new LinkedHashMap<>();
        long apples,chickens,lastFood,young=-1,mature=-1,settled=-1;int delivered,evals,lastMiningRemoved;boolean resumedForUpgrade;ColonyDevelopment.Evaluation previous;
        Progress(GameTestHelper c,NestPlanFixture fx,LasiusNigerEntity q){this.c=c;this.fx=fx;this.q=q;level=c.getLevel();}
        List<LasiusNigerEntity> workers(){return ColonyMembers.get(level).members(q.getUUID()).stream().filter(m->!m.dead()).map(m->level.getEntity(m.worker())).filter(a->a instanceof LasiusNigerEntity).map(a->(LasiusNigerEntity)a).filter(a->a.isAlive()).toList();}
        net.minecraft.world.phys.AABB bounds(){return c.getBounds().inflate(32);}
        long food(Item item){
            var p=q.founding().plan();var cache=p==null?null:level.getBlockEntity(p.cache()) instanceof NestCache n?n:null;
            return level.getEntitiesOfClass(ItemEntity.class,bounds(),i->i.isAlive()&&i.getItem().is(item)).stream().mapToLong(i->i.getItem().getCount()).sum()
                +workers().stream().filter(w->w.getMainHandItem().is(item)).mapToLong(w->w.getMainHandItem().getCount()).sum()
                +(cache==null?0:cache.contents().stream().filter(s->s.is(item)).count())
                +TransferCustody.get(level).contents().stream().filter(t->t.stack().is(item)&&bounds().contains(t.position())).mapToLong(t->t.stack().getCount()).sum();
        }
        long foodLedger(){
            return fx.food.total(c,q);
        }
        void dropFood(Item item){
            fx.food.supply(c,q,item==Items.APPLE?1:0,item==Items.CHICKEN?1:0);
        }
        void census(){
            var l=level;var p=q.founding().plan();
            for(int f=-16;f<=16;f++)for(int s=-16;s<=16;s++)for(int y=-6;y<=-1;y++){
                var at=p.at(f,s,y);if(MiningShape.within(at.getX()-p.entrance().getX(),y,at.getZ()-p.entrance().getZ())
                    &&l.getBlockState(at).is(Blocks.CLAY)&&NaturalMaterials.get(l).eligible(l,at))original.add(at);
            }
            PrimeAnts.LOGGER.info("CLAY CENSUS seed={} entrance={} direction={} authority=declared_controlled_fixture clay={} positions={}",l.getSeed(),p.entrance(),p.direction(),original.size(),original);
        }
        void feed(){
            if(q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN||c.getTick()-lastFood<200)return;
            long a=food(Items.APPLE),b=food(Items.CHICKEN);
            if(a+b>=5)return;
            if(a<3&&apples<1000){dropFood(Items.APPLE);apples++;lastFood=c.getTick();}
            else if(b<2&&chickens<500){dropFood(Items.CHICKEN);chickens++;lastFood=c.getTick();}
        }
        void tick(){
            var l=level;var p=q.founding().plan();feed();
            c.assertTrue(foodLedger()==apples+chickens,"Finite declared food ledger: supplied="+(apples+chickens)+" observed="+foodLedger());
            if(p==null)return;
            var pile=l.getBlockEntity(p.nursery()) instanceof dev.primeants.brood.BroodPile n?n:null;var e=pile==null?null:pile.stageEvaluation();var j=Mining.get(l).job(q.getUUID());
            var workers=workers();
            var upgrades=ChamberUpgrade.get(l).jobs(q.getUUID());
            c.assertTrue(j==null||j.claim==null||upgrades.stream().noneMatch(a->a.claim!=null),"Acquisition never steals an active builder's claim or cargo");
            for(var upgrade:upgrades)if(upgrade.claim!=null&&upgrade.carried()>0){
                var builder=l.getEntity(upgrade.claim);c.assertTrue(builder instanceof LasiusNigerEntity b&&upgrade.accepts(b.getMainHandItem())&&b.getMainHandItem().getCount()==upgrade.carried()&&upgrade.ledger().exact(),"An active builder retains its exact charged unit while supplying work waits");
            }
            for(var at:original)if(!removed.contains(at)&&l.getBlockState(at).isAir()){
                c.assertTrue(ColonyTerrain.get(l).openingIntact(l,at,q.getUUID())&&j!=null&&j.completed().contains(at),"Observed natural clay removal is a successful owned worker edit");
                var w=workers.stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElseThrow();
                c.assertTrue(w.getMainHandItem().is(Items.CLAY_BALL)&&w.getMainHandItem().getCount()==1&&w.onGround()&&l.getBlockState(w.blockPosition().below()).isSolidRender()&&!w.blockPosition().below().equals(at)&&w.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(at))<=5.0,"Each removed natural block produces one unit at an actual supported worker face within unchanged reach");
                removed.add(at);carriers.put(at,w.getUUID());
                PrimeAnts.LOGGER.info("CLAY RECEIPT queen={} tick={} block={} original=minecraft:clay authority=declared_before_run worker={} cargo={} removals={}",q.getUUID(),c.getTick(),at,w.getUUID(),w.getMainHandItem(),removed.size());
            }
            if(j!=null){
                c.assertTrue(j.removed()<=64&&j.tasks.size()<=64&&j.tasks.stream().allMatch(j::bounded),"Single cumulative ledger retains spatial and 64 successful-edit limits");
                c.assertTrue(j.produced("minecraft:clay_ball")==removed.size(),"Independent block changes equal mining clay receipts");
                int count=j.deliveries().getOrDefault("minecraft:clay_ball",0);
                if(count!=delivered){c.assertTrue(count==delivered+1,"One physical delivery per receipt transition");delivered=count;PrimeAnts.LOGGER.info("CLAY DELIVERY queen={} tick={} worker={} historical={} currentStore={}",q.getUUID(),c.getTick(),j.claim,delivered,MaterialStore.owned(l,q.getUUID(),p).contents());}
            }
            var colony=ChamberRegistry.get(l).colony(q.getUUID());var walls=new LinkedHashSet<BlockPos>();
            if(colony!=null)for(var ch:colony.chambers())walls.addAll(ChamberUpgrade.walls(p,ch));
            for(var at:walls)if(ColonyTerrain.get(l).built(l,at,q.getUUID(),NurseryBlocks.PACKED_CLAY)&&paid.add(at))
                PrimeAnts.LOGGER.info("CLAY WALL PAYMENT queen={} tick={} position={} units={} distinctWalls={}",q.getUUID(),c.getTick(),at,NestWalls.CLAY_PER_CELL,paid.size());
            var store=MaterialStore.owned(l,q.getUUID(),p);long stored=store==null?0:store.units(MaterialUnits.Material.CLAY);
            if(j!=null&&j.removed()>lastMiningRemoved&&mature>=0&&stored==0&&upgrades.stream().anyMatch(a->a.tier==2&&!a.complete()&&!a.stopped())){
                if(!resumedForUpgrade)PrimeAnts.LOGGER.info("CLAY UPGRADE GAP RESUMED tick={} worker={} removed={} upgrades={}",c.getTick(),j.claim,j.removed(),upgrades.stream().map(a->a.chamber+":"+a.reason+":"+a.carried()).toList());resumedForUpgrade=true;
            }
            if(j!=null)lastMiningRemoved=j.removed();
            long carried=workers.stream().filter(w->w.getMainHandItem().is(Items.CLAY_BALL)).mapToLong(w->w.getMainHandItem().getCount()).sum();
            long dropped=l.getEntitiesOfClass(ItemEntity.class,bounds(),i->i.isAlive()&&i.getItem().is(Items.CLAY_BALL)).stream().mapToLong(i->i.getItem().getCount()).sum();
            long custody=TransferCustody.get(l).contents().stream().filter(t->t.stack().is(Items.CLAY_BALL)&&bounds().contains(t.position())).mapToLong(t->t.stack().getCount()).sum();
            c.assertTrue(removed.size()==stored+carried+dropped+custody+paid.size()*NestWalls.CLAY_PER_CELL,"Independent clay conservation removed="+removed.size()+" stored="+stored+" carried="+carried+" dropped="+dropped+" custody="+custody+" distinctWalls="+paid.size());
            if(e==null)return;
            if(young<0&&e.stage()==ColonyStage.YOUNG){young=c.getTick();c.assertTrue(removed.isEmpty()&&stored+carried+dropped+custody+paid.size()==0,"Genuine Young starts with zero clay everywhere");PrimeAnts.LOGGER.info("CLAY YOUNG tick={} loaded={} inputs={}",young,pile.loadedTicks(),e.inputs());}
            if(mature<0&&e.stage()==ColonyStage.MATURE){mature=c.getTick();c.assertTrue(young>=0&&removed.size()>=16&&e.inputs().adults().known()>=25&&e.inputs().food().known()>=4,"Native promotion retains population, food, room and clay requirements");PrimeAnts.LOGGER.info("CLAY MATURE tick={} loaded={} removed={} inputs={} requiredDistinctWalls={} recipe={}",mature,pile.loadedTicks(),removed.size(),e.inputs(),walls.size(),NestWalls.CLAY_PER_CELL);}
            if(c.getTick()%1000==0)PrimeAnts.LOGGER.info("CLAY PROGRESS tick={} stage={} adults={} food={}/{} removed={} paid={}/{} job={} workers={}",c.getTick(),e.stage(),e.inputs().adults(),apples,chickens,removed.size(),paid.size(),walls.size(),j==null?null:j.reason+":"+j.removed()+"/"+j.tasks.size(),workers.stream().filter(w->w.workerTasks().construction()||w.workerTasks().upgrading()).map(w->w.getUUID()+":"+w.position()+":"+w.workerTasks().reason()).toList());
            boolean finished=mature>=0&&Arrays.stream(ChamberFunction.values()).allMatch(f->e.inputs().tier(f).known()>=2)
                &&ChamberUpgrade.get(l).jobs(q.getUUID()).stream().filter(a->a.tier==2).allMatch(a->a.complete()&&a.claim==null&&a.carried()==0&&a.ledger().exact())
                &&upgrades.stream().filter(a->a.tier==2).count()==3&&j!=null&&j.complete()&&j.claim==null&&carried+dropped+custody==0;
            if(!finished){settled=-1;evals=0;return;}
            if(settled<0){settled=pile.loadedTicks();previous=e;}
            if(previous!=e){previous=e;evals++;}
            if(pile.loadedTicks()-settled<200||evals<2)return;
            c.assertTrue(paid.size()==walls.size()&&removed.size()>=Math.max(16,walls.size()*NestWalls.CLAY_PER_CELL),"Count shared nursery/food room once, promotion stock is retained in walls");
            c.assertTrue(resumedForUpgrade,"Ordinary acquisition resumed after an actual tier-two material gap");
            PrimeAnts.LOGGER.info("CLAY ACCEPTED queen={} young={} mature={} tierTwoTick={} evaluations={} settled={} removals={} stored={} carried={} dropped={} custody={} walls={} historicalDeliveries={} foodApples={} foodChicken={} materialGifts=0",q.getUUID(),young,mature,c.getTick(),evals,pile.loadedTicks()-settled,removed.size(),stored,carried,dropped,custody,paid.size(),delivered,apples,chickens);c.succeed();
        }
    }
}
