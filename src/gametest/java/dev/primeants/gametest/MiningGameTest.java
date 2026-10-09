package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Frozen 60,000 total ticks including real Mature preparation. Fixtures declare geology, never worker outcomes. */
public final class MiningGameTest {
    static final List<Item> MATERIALS=List.of(Items.COBBLESTONE,Items.CLAY_BALL,Items.GRAVEL,Items.SAND,Items.COAL,Items.RAW_COPPER,Items.RAW_IRON);
    static final List<Block> RESOURCES=List.of(Blocks.STONE,Blocks.CLAY,Blocks.GRAVEL,Blocks.SAND,Blocks.COAL_ORE,Blocks.COPPER_ORE,Blocks.IRON_ORE);
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void matureWorkerDigsConnectedGalleryAndPhysicallyStoresEveryResourceWithSettledLedgers(GameTestHelper c){gallery(c);}

    static void feedMining(GameTestHelper c,TierTwoFixture player,LasiusNigerEntity q){
        if(!player.opened&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){player.opened=true;player.supply(c,q,3,2);}
        player.feed(c,q);
    }
    static void geology(GameTestHelper c,LasiusNigerEntity q){geology(c,q,false);}
    static void geology(GameTestHelper c,LasiusNigerEntity q,boolean fallingRoof){
        var l=c.getLevel();var p=q.founding().plan();
        var soil=NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        var mineral=NaturalMaterials.CODEC.encodeStart(JsonOps.INSTANCE,NaturalMaterials.get(l)).getOrThrow().getAsJsonObject();
        // Declared one-time controlled geology setup, before any mining work or replacement intervention.
        for(int f=8;f<=16;f++)for(int s=-1;s<=1;s++)for(int dy=-3;dy<=0;dy++){
            var at=p.at(f,s,dy);var state=Blocks.DIRT.defaultBlockState();
            if(s==0&&f>=9&&f<=15&&(dy==-2||dy==-1))state=(dy==-1?Blocks.STONE:RESOURCES.get(f-9)).defaultBlockState();
            if(fallingRoof&&s==0&&f==9&&dy==-1)state=Blocks.SAND.defaultBlockState();
            l.setBlock(at,state,3);var key=Long.toString(at.asLong());soil.remove(key);mineral.remove(key);
            if(NaturalSoil.material(state))soil.addProperty(key,NaturalMaterials.type(state));
            if(NaturalMaterials.unit(state)!=null)mineral.addProperty(key,NaturalMaterials.type(state));
        }
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(JsonOps.INSTANCE,soil).getOrThrow());
        l.getDataStorage().set(NaturalMaterials.TYPE,NaturalMaterials.CODEC.parse(JsonOps.INSTANCE,mineral).getOrThrow());
        PrimeAnts.LOGGER.info("T09 GEOLOGY queen={} gallery={} positiveOrigins=16 fixtureOnly=true",q.getUUID(),MiningShape.cells());
    }
    static void miningLedger(GameTestHelper c,TierTwoFixture player,LasiusNigerEntity q){
        miningLedger(c,player,q,Map.of());
    }
    static void miningLedger(GameTestHelper c,TierTwoFixture player,LasiusNigerEntity q,Map<Item,Integer> extra){
        var j=Mining.get(c.getLevel()).job(q.getUUID());if(j==null)return;
        for(var item:MATERIALS){
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
            long supplied=(item==Items.CLAY_BALL?player.clay:0)+extra.getOrDefault(item,0);
            long incorporated=item==Items.CLAY_BALL?NurseryUpgradeGameTest.allWalls(c,q).stream().filter(b->ColonyTerrain.get(c.getLevel()).built(c.getLevel(),b,q.getUUID(),dev.primeants.brood.NurseryBlocks.PACKED_CLAY)).count():0;
            var ledger=player.fx.ledger(c,q,item);
            c.assertTrue(supplied+j.produced(id)==ledger.total()+incorporated,"Per-item supplied + successful mining = ground + carried + stored + pending + independently incorporated: "+id+" supplied="+supplied+" produced="+j.produced(id)+" "+ledger+" walls="+incorporated);
        }
        var carrier=player.fx.f.workers(c,q).stream().filter(w->w.getUUID().equals(j.claim)).findFirst().orElse(null);
        int held=carrier!=null&&j.pending(carrier.getMainHandItem())?1:0;
        c.assertTrue(held<=1&&j.removed()==j.deposited+j.released+held,"Mining successful edits = cumulative deliveries + releases + current cargo; delivery history is not current stock");
        c.assertTrue(j.removed()<=64&&j.tasks.stream().allMatch(j::bounded),"Radial sixteen, depth one-six and per-colony edit budget 64");
        for(var at:j.completed())c.assertTrue(ColonyTerrain.get(c.getLevel()).opened(c.getLevel(),at,q.getUUID()),"Every successful mining cell is an owned actual opening");
    }
    private void gallery(GameTestHelper c){
        var player=new TierTwoFixture(40,true);var q=player.fx.start(c);
        boolean[] seeded={false};long[] settled={-1},youngReady={-1};int[] observed={0},readyTicks={0};
        var producedSeen=new HashMap<Item,Integer>();ColonyDevelopment.Evaluation[] seen={null};
        c.onEachTick(()->{
            var l=c.getLevel();var plan=q.founding().plan();
            if(plan!=null&&!seeded[0]){geology(c,q);seeded[0]=true;}
            feedMining(c,player,q);player.ledgers(c,q);player.trace(c,q,"MINING");miningLedger(c,player,q);
            var p=NestPlanFixture.pile(c,q);if(p==null)return;var e=p.stageEvaluation();var j=Mining.get(l).job(q.getUUID());
            if(e!=null&&e.stage()==ColonyStage.YOUNG&&NestPlanFixture.storeConfirmed(e)&&q.founding().ready()){
                if(youngReady[0]<0)youngReady[0]=p.loadedTicks();readyTicks[0]++;
                c.assertTrue(j==null||j.removed()==0,"A supplied, ready Young colony with positively witnessed resource faces never removes a mining cell");
            }
            if(j==null)return;
            c.assertTrue(readyTicks[0]>=600,"At least 600 loaded ready Young ticks before real production Mature mining");
            if(e!=null&&seen[0]!=e){seen[0]=e;observed[0]++;c.assertTrue(Arrays.stream(ChamberFunction.values()).allMatch(f->e.inputs().tier(f).known()>=1),"All four chamber functions remain confirmed while mining: "+e);}
            for(var item:MATERIALS){var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();int made=j.produced(id);
                if(made>producedSeen.getOrDefault(item,0)){
                    var w=player.fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElseThrow();
                    c.assertTrue(w.getMainHandItem().is(item)&&w.getMainHandItem().getCount()==1&&w.workerTasks().construction(),"Successful resource removal has one visible mandible cargo in the claimed real worker");
                    producedSeen.put(item,made);PrimeAnts.LOGGER.info("T09 OBSERVED REMOVAL CARGO queen={} tick={} item={} produced={} worker={} position={}",q.getUUID(),c.getTick(),item,made,w.getUUID(),w.position());
                }
            }
            if(!j.complete()||j.claim!=null)return;
            c.assertTrue(j.removed()==16&&j.produced("minecraft:dirt")==2&&j.released==0&&j.deposited==16,"Two connector soil and fourteen exactly-one resource edits, all physically delivered");
            c.assertTrue(producedSeen.size()==7&&observed[0]>=2,"All seven distinct item variants observed in real cargo and fresh production chamber evaluations");
            for(var item:MATERIALS){var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();c.assertTrue(j.deliveries().getOrDefault(id,0)==j.produced(id)&&player.fx.ledger(c,q,item).stored()>=j.produced(id),"Every resource reaches the confirmed physical store; ores stay raw: "+id);}
            for(int f=8;f<=15;f++)c.assertTrue(NestPlan.walkable(l,plan.at(f,0,-2)),"Connected two-high supported gallery column "+f);
            c.assertTrue(plan.nurseryProblem(l,q.getUUID(),true)==null&&MaterialStore.confirmed(l,q.getUUID(),plan)!=null,"Actual gallery preserves all nest integrity checks");
            if(settled[0]<0){settled[0]=p.loadedTicks();PrimeAnts.LOGGER.info("T09 GALLERY SETTLED START queen={} tick={} ledger={} stock={}",q.getUUID(),c.getTick(),j.deliveries(),NestPlanFixture.store(c,q).contents());}
            if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T09 GALLERY DONE queen={} tick={} readyYoungTicks={} chamberEvaluations={} finalLoadedObservation={} removed={} delivered={} released={} stock={}",q.getUUID(),c.getTick(),readyTicks[0],observed[0],p.loadedTicks()-settled[0],j.removed(),j.deliveries(),j.released,NestPlanFixture.store(c,q).contents());c.succeed();
        });
    }
    @GameTest(maxTicks=300)
    public void genuineProtoConversionObservesMiningOriginsAndSameStateWritesAndReloadCannotRestoreThem(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"t16_thin");var chunk=NaturalPlacementGameTest.prepare(c,l,3000);PlacementFault.blockFinal(l,chunk);
        int surface=l.getMinY()+7;
        for(int n=0;n<7;n++){
            var at=new BlockPos(chunk.getMinBlockX()+n+1,surface-4,chunk.getMinBlockZ()+1);
            c.assertTrue(l.getBlockState(at).is(RESOURCES.get(n))&&NaturalMaterials.get(l).eligible(l,at)&&!NaturalSoil.get(l).eligible(l,at),"Actual genuine generation conversion records each mining resource separately from soil authority");
            l.setBlock(at,l.getBlockState(at),3);c.assertTrue(!NaturalMaterials.get(l).eligible(l,at),"Same-state player write immediately revokes mining authority");
        }
        NaturalPlacementGameTest.reloadChunk(l,chunk);l.getDataStorage().saveAndJoin();
        c.runAfterDelay(30,()->{
            for(int n=0;n<7;n++)c.assertTrue(!NaturalMaterials.get(l).eligible(l,new BlockPos(chunk.getMinBlockX()+n+1,surface-4,chunk.getMinBlockZ()+1)),"Historical loading cannot reauthorize a player replacement");
            PrimeAnts.LOGGER.info("T09 GENUINE ORIGIN DONE chunk={} resources=7 sameStateRevoked=true historicalLoadingUnknown=true",chunk);c.succeed();
        });
    }

    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void unavailableTargetAndStoreForeignOwnershipAndSameStateReplacementRetainExactCargo(GameTestHelper c){
        var player=new TierTwoFixture(40,true);var q=player.fx.start(c);boolean[] seeded={false};int[] step={0},evals={0},baseline={0};long[] since={-1},settled={-1};
        UnavailableCells[] hidden={null};ColonyDevelopment.Evaluation[] seen={null};ItemStack[] kept={null};net.minecraft.nbt.CompoundTag[] originalStore={null};BlockPos[] replaced={null};
        c.onEachTick(()->{
            var l=c.getLevel();var plan=q.founding().plan();if(plan!=null&&!seeded[0]){geology(c,q);seeded[0]=true;}
            feedMining(c,player,q);player.ledgers(c,q);miningLedger(c,player,q);traceMining(c,player,q,"AVAILABILITY");
            var p=NestPlanFixture.pile(c,q);var j=Mining.get(l).job(q.getUUID());if(p==null||j==null||j.claim==null&&step[0]<8)return;
            var w=player.fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElse(null);var e=p.stageEvaluation();
            if(e!=null&&seen[0]!=e){seen[0]=e;if((step[0]==2||step[0]==4||step[0]==6)&&e.inputs().tier(ChamberFunction.MATERIAL_STORE).known()>=1)evals[0]++;}
            switch(step[0]){
                case 0->{
                    if(j.removed()!=2||j.deposited!=2||w==null||!w.workerTasks().reason().equals("physical_mining_action")||w.workerTasks().harvestingTicks()<1)return;
                    var target=j.tasks.get(2);c.assertTrue(q.founding().ready()&&Mining.unlocked(l,plan)&&j.compatible(l,q.getUUID(),target,j.expected.get(2))&&MaterialStore.confirmed(l,q.getUUID(),plan)!=null,"The claimed worker is actually acting at a positively authorized exposed supported resource before the unavailable intervention");
                    hidden[0]=UnavailableCells.hide(c,target);baseline[0]=j.removed();since[0]=p.loadedTicks();step[0]=1;
                    PrimeAnts.LOGGER.info("T09 TARGET UNKNOWN START queen={} tick={} loaded={} removed={}",q.getUUID(),c.getTick(),since[0],baseline[0]);
                }
                case 1->{
                    c.assertTrue(j.removed()==baseline[0]&&w!=null&&w.getMainHandItem().isEmpty(),"Unavailable target never becomes a successful removal or cargo");
                    if(p.loadedTicks()-since[0]<600)return;hidden[0].close();hidden[0]=null;evals[0]=0;seen[0]=e;step[0]=2;
                }
                case 2->{
                    if(evals[0]<2||j.removed()<=baseline[0]||w==null||w.getMainHandItem().isEmpty())return;
                    kept[0]=w.getMainHandItem().copy();baseline[0]=j.removed();since[0]=p.loadedTicks();hidden[0]=UnavailableCells.hide(c,NestPlanFixture.store(c,q).getBlockPos());step[0]=3;
                    PrimeAnts.LOGGER.info("T09 STORE UNKNOWN START queen={} tick={} cargo={} restoredTargetEvaluations={}",q.getUUID(),c.getTick(),kept[0],evals[0]);
                }
                case 3->{
                    c.assertTrue(w!=null&&ItemStack.matches(kept[0],w.getMainHandItem())&&j.removed()==baseline[0],"Unavailable store retains the same physical cargo and permits no additional edit");
                    if(p.loadedTicks()-since[0]<600)return;hidden[0].close();hidden[0]=null;evals[0]=0;seen[0]=e;step[0]=4;
                }
                case 4->{
                    if(evals[0]<2||j.deposited<baseline[0]||w==null||w.getMainHandItem().isEmpty())return;
                    var s=NestPlanFixture.store(c,q);kept[0]=w.getMainHandItem().copy();baseline[0]=j.removed();since[0]=p.loadedTicks();originalStore[0]=s.saveWithFullMetadata(l.registryAccess());
                    var foreign=originalStore[0].copy();foreign.putString("Colony",UUID.randomUUID().toString());restoreStore(c,s,foreign);step[0]=5;
                    PrimeAnts.LOGGER.info("T09 FOREIGN STORE START queen={} tick={} cargo={} restoredStoreEvaluations={}",q.getUUID(),c.getTick(),kept[0],evals[0]);
                }
                case 5->{
                    c.assertTrue(w!=null&&ItemStack.matches(kept[0],w.getMainHandItem())&&j.removed()==baseline[0]&&MaterialStore.confirmed(l,q.getUUID(),plan)==null,"Foreign store authorization cannot consume or credit mining cargo");
                    if(p.loadedTicks()-since[0]<600)return;c.assertTrue(e.stage()!=ColonyStage.MATURE&&e.stage()!=ColonyStage.GREAT,"Actual production evaluation regresses after observed foreign store loss");
                    restoreStore(c,NestPlanFixture.store(c,q),originalStore[0]);evals[0]=0;seen[0]=e;step[0]=6;
                }
                case 6->{
                    if(evals[0]<2||j.deposited<baseline[0]||w==null||!w.getMainHandItem().isEmpty()||j.removed()==j.tasks.size())return;
                    replaced[0]=j.tasks.get(j.removed());var state=l.getBlockState(replaced[0]);c.assertTrue(j.compatible(l,q.getUUID(),replaced[0],j.expected.get(j.removed())),"Live next target positive before same-state player replacement");
                    l.setBlock(replaced[0],state,3);c.assertTrue(!NaturalMaterials.get(l).eligible(l,replaced[0])&&!ColonyTerrain.get(l).eligible(l,replaced[0],q.getUUID()),"Same-state player replacement revokes both origins; never relabelled natural");
                    baseline[0]=j.removed();since[0]=p.loadedTicks();step[0]=7;
                    PrimeAnts.LOGGER.info("T09 SAME STATE START queen={} tick={} target={} removed={} restoredForeignEvaluations={}",q.getUUID(),c.getTick(),replaced[0],baseline[0],evals[0]);
                }
                case 7->{
                    c.assertTrue(j.removed()==baseline[0]&&!l.getBlockState(replaced[0]).isAir(),"Ready Mature miner never removes the replaced target");
                    if(p.loadedTicks()-since[0]<600)return;c.assertTrue(j.stopped()&&j.claim==null,"Revoked origin stops the descriptive plan and releases the empty worker");settled[0]=p.loadedTicks();step[0]=8;
                }
                case 8->{
                    c.assertTrue(j.removed()==baseline[0]&&j.deposited==j.removed()&&plan.nurseryProblem(l,q.getUUID(),true)==null,"Stopped connected partial gallery preserves exact custody and integrity");
                    if(p.loadedTicks()-settled[0]<200)return;PrimeAnts.LOGGER.info("T09 AVAILABILITY DONE queen={} tick={} targetUnknown=600 storeUnknown=600 foreign=600 sameState=600 settled={} removed={} deliveries={}",q.getUUID(),c.getTick(),p.loadedTicks()-settled[0],j.removed(),j.deliveries());c.succeed();
                }
            }
        });
    }
    static void restoreStore(GameTestHelper c,MaterialStore store,net.minecraft.nbt.CompoundTag tag){
        var l=c.getLevel();var at=store.getBlockPos();var state=store.getBlockState();l.removeBlockEntity(at);
        var restored=(MaterialStore)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(at,state,tag,l.registryAccess());c.assertTrue(restored!=null,"Normal material-store block entity deserialization");l.setBlockEntity(restored);
    }
    static void traceMining(GameTestHelper c,TierTwoFixture player,LasiusNigerEntity q,String label){
        if(c.getTick()%1000!=0)return;var j=Mining.get(c.getLevel()).job(q.getUUID());
        if(j==null){player.trace(c,q,label);return;}
        var w=player.fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElse(null);
        PrimeAnts.LOGGER.info("T09 {} CURVE queen={} tick={} removed={} delivered={} released={} reason={} worker={} stage={}",label,q.getUUID(),c.getTick(),j.removed(),j.deliveries(),j.transfers(),j.reason,w==null?null:w.getUUID()+" "+w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem(),NestPlanFixture.pile(c,q).stageEvaluation());
    }

    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void fullStorePreventsRemovalThenPhysicalStockReleaseAndTwoEvaluationsResumeMining(GameTestHelper c){
        var player=new TierTwoFixture(40,true);var q=player.fx.start(c);boolean[] seeded={false},supplied={false};int[] step={0},evals={0};long[] since={-1},settled={-1};
        UnavailableCells[] hidden={null};ColonyDevelopment.Evaluation[] seen={null};
        c.onEachTick(()->{
            var l=c.getLevel();var plan=q.founding().plan();if(plan!=null&&!seeded[0]){geology(c,q);seeded[0]=true;hidden[0]=UnavailableCells.hide(c,plan.at(15,0,-2));}
            feedMining(c,player,q);player.ledgers(c,q);traceMining(c,player,q,"FULL STORE");var p=NestPlanFixture.pile(c,q);if(p==null)return;var e=p.stageEvaluation();var s=NestPlanFixture.store(c,q);var j=Mining.get(l).job(q.getUUID());
            if(!supplied[0]&&e!=null&&e.inputs().tier(ChamberFunction.NURSERY).known()==2){supplied[0]=true;player.fx.drop(c,plan.at(-5,0,1),new ItemStack(Items.COBBLESTONE,32));}
            long made=j==null?0:j.produced("minecraft:cobblestone");c.assertTrue(player.fx.ledger(c,q,Items.COBBLESTONE).total()==(supplied[0]?32:0)+made,"Full-store contribution and mined stone are independently conserved");
            if(j!=null)miningLedger(c,player,q,Map.of(Items.COBBLESTONE,32));
            if(e!=null&&seen[0]!=e){seen[0]=e;if(step[0]==3&&e.inputs().tier(ChamberFunction.MATERIAL_STORE).known()>=1)evals[0]++;}
            switch(step[0]){
                case 0->{
                    if(s==null||s.capacity()!=64||s.units(MaterialUnits.Material.STONE)!=32||!ChamberUpgrade.get(l).jobs(q.getUUID()).stream().allMatch(a->a.complete()&&a.claim==null)||ChamberUpgrade.get(l).jobs(q.getUUID()).size()!=3)return;
                    hidden[0].close();hidden[0]=null;step[0]=1;
                }
                case 1->{
                    if(j==null||!Mining.unlocked(l,plan)||MaterialStore.confirmed(l,q.getUUID(),plan)==null)return;
                    c.assertTrue(!s.room(new ItemStack(Items.COBBLESTONE))&&j.removed()==0&&j.claim==null,"Actual full non-clay share prevents the first connector/resource removal");
                    boolean ready=player.fx.f.workers(c,q).stream().anyMatch(w->w.workerTasks().canConstruct(plan)&&NestExpansion.remainingCaregivers(l,q.getUUID(),plan,w)>=2);
                    if(!ready)return;since[0]=p.loadedTicks();step[0]=2;PrimeAnts.LOGGER.info("T09 FULL STORE READY queen={} tick={} stock={} loaded={}",q.getUUID(),c.getTick(),s.contents(),since[0]);
                }
                case 2->{
                    c.assertTrue(j.removed()==0&&j.claim==null&&s.units(MaterialUnits.Material.STONE)==32,"Six hundred loaded ticks of demonstrated ready mining backpressure, no destruction/income");
                    if(p.loadedTicks()-since[0]<600)return;
                    // A declared player break releases every actual stock unit through ordinary store custody.
                    var marker=s.getBlockPos();l.setBlock(marker,Blocks.AIR.defaultBlockState(),3);
                    for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(marker).inflate(2),i->i.isAlive()&&MaterialStore.material(i.getItem())))item.setPickUpDelay(32767);
                    c.assertTrue(player.fx.ledger(c,q,Items.COBBLESTONE).ground()+player.fx.ledger(c,q,Items.COBBLESTONE).custody()==32,"All full-share units remain physical on ground/in custody during restored capacity");
                    evals[0]=0;seen[0]=e;step[0]=3;
                }
                case 3->{
                    if(evals[0]<2||MaterialStore.confirmed(l,q.getUUID(),plan)==null||j.deposited<3)return;
                    if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
                    c.assertTrue(j.removed()>=3&&j.deliveries().getOrDefault("minecraft:cobblestone",0)>0,"A genuine fresh removal, visible cargo and store delivery resume after two fresh evaluations");
                    PrimeAnts.LOGGER.info("T09 FULL STORE DONE queen={} tick={} negativeLoaded=600 evaluations={} settled={} removed={} deliveries={} ledger={}",q.getUUID(),c.getTick(),evals[0],p.loadedTicks()-settled[0],j.removed(),j.deliveries(),player.fx.ledger(c,q,Items.COBBLESTONE));c.succeed();
                }
            }
        });
    }

    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void midHaulDiskReloadResumesThenMinerDeathReleasesOneUnitExactlyOnce(GameTestHelper c){
        var player=new TierTwoFixture(40,true);LasiusNigerEntity[] queen={player.fx.start(c)};boolean[] seeded={false},resumed={false};int[] step={0},baseline={0},evals={0};long[] since={-1},settled={-1};UUID[] worker={null},transfer={null};UnavailableCells[] hidden={null};ColonyDevelopment.Evaluation[] seen={null};
        c.onEachTick(()->{
            var q=queen[0];var l=c.getLevel();var plan=q.founding().plan();if(plan!=null&&!seeded[0]){geology(c,q);seeded[0]=true;}
            feedMining(c,player,q);player.ledgers(c,q);miningLedger(c,player,q);traceMining(c,player,q,"RELOAD DEATH");var p=NestPlanFixture.pile(c,q);var j=Mining.get(l).job(q.getUUID());if(p==null||j==null)return;
            var w=player.fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElse(null);var e=p.stageEvaluation();if(e!=null&&seen[0]!=e){seen[0]=e;if(step[0]==3&&e.inputs().tier(ChamberFunction.MATERIAL_STORE).known()>=1)evals[0]++;}
            if(step[0]==0){
                if(j.removed()!=3||j.deposited!=2||w==null||!w.getMainHandItem().is(Items.COBBLESTONE))return;
                worker[0]=w.getUUID();baseline[0]=j.removed();var cargo=w.getMainHandItem().copy();var encoded=Mining.CODEC.encodeStart(JsonOps.INSTANCE,Mining.get(l)).getOrThrow();
                var before=new HashMap<Item,NestPlanFixture.Ledger>();for(var item:MATERIALS)before.put(item,player.fx.ledger(c,q,item));var store=NestPlanFixture.store(c,q);var storeTag=store.saveWithFullMetadata(l.registryAccess());
                l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var mining=disk.get(Mining.TYPE);c.assertTrue(mining!=null&&Mining.CODEC.encodeStart(JsonOps.INSTANCE,mining).getOrThrow().equals(encoded),"Actual disk reload retains exact descriptive plan, edits, identity claim and delivery/release ledger");l.getDataStorage().set(Mining.TYPE,mining);
                    l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));l.getDataStorage().set(NaturalMaterials.TYPE,Objects.requireNonNull(disk.get(NaturalMaterials.TYPE)));
                    l.getDataStorage().set(NaturalSoil.TYPE,Objects.requireNonNull(disk.get(NaturalSoil.TYPE)));l.getDataStorage().set(ChamberRegistry.TYPE,Objects.requireNonNull(disk.get(ChamberRegistry.TYPE)));
                    l.getDataStorage().set(ChamberExcavation.TYPE,Objects.requireNonNull(disk.get(ChamberExcavation.TYPE)));l.getDataStorage().set(ChamberUpgrade.TYPE,Objects.requireNonNull(disk.get(ChamberUpgrade.TYPE)));
                    var custody=disk.get(TransferCustody.TYPE);if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
                }
                var restored=player.fx.f.restore(c,w);queen[0]=player.fx.f.restore(c,q);restoreStore(c,store,storeTag);
                c.assertTrue(restored.getUUID().equals(worker[0])&&ItemStack.matches(restored.getMainHandItem(),cargo)&&worker[0].equals(Mining.get(l).job(q.getUUID()).claim),"Same worker identity, canonical mandible cargo and persisted mining claim after normal restore");
                for(var item:MATERIALS)c.assertTrue(player.fx.ledger(c,queen[0],item).equals(before.get(item)),"Every item remains in the same physical account across reload");
                step[0]=1;PrimeAnts.LOGGER.info("T09 MINING RELOAD queen={} tick={} worker={} cargo={} removed={} ledger={}",q.getUUID(),c.getTick(),worker[0],cargo,baseline[0],before);return;
            }
            if(step[0]==1){
                if(j.deposited>=3)resumed[0]=true;if(!resumed[0]||j.removed()!=4||j.deposited!=3||w==null||!w.getUUID().equals(worker[0])||!w.getMainHandItem().is(Items.COBBLESTONE)||j.completed().contains(w.blockPosition()))return;
                var cargo=w.getMainHandItem().copy();hidden[0]=UnavailableCells.hide(c,w.blockPosition());transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+w.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));baseline[0]=j.removed();since[0]=p.loadedTicks();
                w.hurtServer(l,w.damageSources().genericKill(),1000);w.workerTasks().die(l);
                c.assertTrue(!w.isAlive()&&w.getMainHandItem().isEmpty()&&j.claim==null&&j.released==1&&j.transfers().get(transfer[0].toString()).equals("minecraft:cobblestone"),"Real lethal hit clears canonical cargo and names one exact mining custody transfer; repeated die is inert");
                c.assertTrue(TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])&&ItemStack.matches(t.stack(),cargo)).count()==1,"Unavailable death position retains exactly one pending unit");step[0]=2;
                PrimeAnts.LOGGER.info("T09 MINER DEATH queen={} tick={} worker={} transfer={} cargo={} released={}",q.getUUID(),c.getTick(),worker[0],transfer[0],cargo,j.transfers());return;
            }
            if(step[0]==2){
                c.assertTrue(j.removed()==baseline[0]&&j.released==1&&TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])).count()==1&&l.getEntity(transfer[0])==null,"Six hundred loaded ticks retain one custody unit without duplicate falling/ground resource");
                if(p.loadedTicks()-since[0]<600)return;hidden[0].close();hidden[0]=null;evals[0]=0;seen[0]=e;step[0]=3;return;
            }
            if(evals[0]<2||j.removed()<=baseline[0]||!j.complete()||j.claim!=null)return;
            c.assertTrue(resumed[0]&&j.released==1&&j.deposited==15&&j.removed()==16&&j.transfers().size()==1,"Reload resumes actual delivery; a fresh real miner completes the remaining gallery after death");
            var releasedEntity=l.getEntity(transfer[0]);var recoveryCache=player.fx.f.cache(c,q);
            PrimeAnts.LOGGER.info("T09 DEATH RECOVERY CHECK queen={} tick={} ledger={} releasedEntity={} pending={} foodCache={} foragers={}",q.getUUID(),c.getTick(),player.fx.ledger(c,q,Items.COBBLESTONE),
                releasedEntity instanceof net.minecraft.world.entity.item.ItemEntity item?item.getItem()+"@"+item.position()+" alive="+item.isAlive()+" removed="+item.isRemoved():releasedEntity,
                TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])).count(),recoveryCache==null?null:recoveryCache.contents(),
                player.fx.f.workers(c,q).stream().filter(a->q.founding().claimedBy(a)).map(a->a.getUUID()+":"+a.workerTasks().phase()+":"+a.workerTasks().reason()+":"+a.getMainHandItem()+":"+a.position()).toList());
            c.assertTrue(TransferCustody.get(l).contents().stream().noneMatch(t->t.id().equals(transfer[0]))&&l.getEntity(transfer[0])==null&&player.fx.ledger(c,q,Items.COBBLESTONE).stored()==j.produced("minecraft:cobblestone"),"The released unit is subsequently physically picked up and stored exactly once");
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T09 RELOAD DEATH DONE queen={} tick={} resumed=true custodyWindow=600 restoredEvaluations={} settled={} deliveries={} releases={}",q.getUUID(),c.getTick(),evals[0],p.loadedTicks()-settled[0],j.deliveries(),j.transfers());c.succeed();
        });
    }

    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void naturalFallingRoofIsRefusedWithoutCargoOrFallingResourceDuplication(GameTestHelper c){
        var player=new TierTwoFixture(40,true);var q=player.fx.start(c);boolean[] seeded={false};long[] since={-1},settled={-1};
        c.onEachTick(()->{
            var l=c.getLevel();var plan=q.founding().plan();if(plan!=null&&!seeded[0]){geology(c,q,true);seeded[0]=true;}
            feedMining(c,player,q);player.ledgers(c,q);miningLedger(c,player,q);traceMining(c,player,q,"GRAVITY");var p=NestPlanFixture.pile(c,q);var j=Mining.get(l).job(q.getUUID());if(p==null||j==null||j.claim==null)return;
            if(j.removed()!=2||j.deposited!=2||!j.reason.equals("mining_falling_roof_unsafe"))return;
            var at=plan.at(9,0,-2);c.assertTrue(Mining.unlocked(l,plan)&&q.founding().ready()&&NestPlan.walkable(l,plan.at(8,0,-2))&&NaturalMaterials.get(l).eligible(l,at)&&NaturalMaterials.get(l).eligible(l,at.above()),"Actual assigned Mature miner at the connected supported access retains positive stone/sand origins but refuses unsafe gravity");
            c.assertTrue(l.getBlockState(at).is(Blocks.STONE)&&l.getBlockState(at.above()).is(Blocks.SAND)&&j.produced("minecraft:cobblestone")==0&&j.produced("minecraft:sand")==0,"Unsafe natural sand support remains intact, no cargo produced");
            c.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,c.getBounds().inflate(8)).isEmpty(),"Ordinary falling blocks stay enabled, with no falling resource from a mining edit");
            if(since[0]<0)since[0]=p.loadedTicks();if(p.loadedTicks()-since[0]<600)return;
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T09 GRAVITY DONE queen={} tick={} negativeLoaded={} settled={} removed={} cargo=0 falling=0",q.getUUID(),c.getTick(),p.loadedTicks()-since[0],p.loadedTicks()-settled[0],j.removed());c.succeed();
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void realPopulationRegressionStopsNewMiningButAuthorizedCarriedUnitStillReachesStore(GameTestHelper c){
        var player=new TierTwoFixture(40,true);var q=player.fx.start(c);boolean[] seeded={false},killed={false},delivered={false};long[] since={-1},restoredSince={-1},settled={-1};int[] evals={0};UUID[] miner={null};UnavailableCells[] hidden={null};ColonyDevelopment.Evaluation[] seen={null};
        c.onEachTick(()->{
            var l=c.getLevel();var plan=q.founding().plan();if(plan!=null&&!seeded[0]){geology(c,q);seeded[0]=true;}
            feedMining(c,player,q);
            if(!killed[0])player.ledgers(c,q);else{
                // Intentional catastrophic deaths may leave too few bodies for the ordinary roles. Exact physical ledgers still apply.
                player.fx.soil(c,q);c.assertTrue(player.fx.food.total(c,q)==player.apples+player.chickens,"Food remains physical or independently consumed through population loss");player.fx.food.yields(c,q);
                var mining=Mining.get(l).job(q.getUUID());c.assertTrue(NurseryUpgradeGameTest.clay(c,player.fx,q)==player.clay+(mining==null?0:mining.produced("minecraft:clay_ball")),"Population loss cannot delete clay");
            }
            miningLedger(c,player,q);traceMining(c,player,q,"REGRESSION");var p=NestPlanFixture.pile(c,q);var j=Mining.get(l).job(q.getUUID());if(p==null||j==null)return;var e=p.stageEvaluation();
            if(!killed[0]){
                var w=player.fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElse(null);
                if(j.removed()!=3||j.deposited!=2||w==null||!w.getMainHandItem().is(Items.COBBLESTONE))return;
                var carers=player.fx.f.workers(c,q).stream().filter(a->a.workerTasks().caregiver(l,plan)).limit(2).toList();if(carers.size()!=2)return;
                miner[0]=w.getUUID();var keep=new HashSet<UUID>();keep.add(miner[0]);carers.forEach(a->keep.add(a.getUUID()));
                hidden[0]=UnavailableCells.hide(c,NestPlanFixture.store(c,q).getBlockPos());
                for(var other:player.fx.f.workers(c,q))if(!keep.contains(other.getUUID()))other.hurtServer(l,other.damageSources().genericKill(),1000);
                killed[0]=true;since[0]=p.loadedTicks();PrimeAnts.LOGGER.info("T09 POPULATION LOSS queen={} tick={} kept={} cargo={}",q.getUUID(),c.getTick(),keep,w.getMainHandItem());return;
            }
            var carrier=player.fx.f.workers(c,q).stream().filter(w->w.getUUID().equals(miner[0])).findFirst().orElseThrow();
            c.assertTrue(j.removed()==3,"A production stage regression never permits another mining removal");
            if(hidden[0]!=null){
                c.assertTrue(carrier.getMainHandItem().is(Items.COBBLESTONE)&&carrier.getMainHandItem().getCount()==1,"The carried unit remains while ordinary store authorization is unavailable");
                if(p.loadedTicks()-since[0]<600)return;c.assertTrue(e.stage()!=ColonyStage.MATURE&&e.stage()!=ColonyStage.GREAT,"Real adult deaths cause an actual production regression");
                hidden[0].close();hidden[0]=null;restoredSince[0]=p.loadedTicks();seen[0]=e;evals[0]=0;return;
            }
            if(e!=null&&seen[0]!=e&&MaterialStore.confirmed(l,q.getUUID(),plan)!=null){seen[0]=e;evals[0]++;}
            if(j.deposited==3){delivered[0]=true;c.assertTrue(carrier.getMainHandItem().isEmpty()&&e.stage()!=ColonyStage.MATURE&&e.stage()!=ColonyStage.GREAT,"Ordinary ownership permits actual cargo delivery while the held production stage remains below Mature");}
            if(evals[0]<2||!delivered[0]||p.loadedTicks()-restoredSince[0]<600)return;
            c.assertTrue(q.founding().ready()&&MaterialStore.confirmed(l,q.getUUID(),plan)!=null&&carrier.workerTasks().reason().equals("mining_stage_below_mature"),"Actual empty miner is ready at the confirmed nest but the held lower stage blocks work for 600 loaded ticks");
            if(settled[0]<0)settled[0]=p.loadedTicks();if(p.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T09 REGRESSION DONE queen={} tick={} stage={} evaluations={} cargoDeliveredBelowMature=true negativeLoaded={} settled={} removed={}",q.getUUID(),c.getTick(),e.stage(),evals[0],p.loadedTicks()-restoredSince[0],p.loadedTicks()-settled[0],j.removed());c.succeed();
        });
    }

}
