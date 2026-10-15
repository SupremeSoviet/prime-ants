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
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Permanent real-tick evidence. No fixture writes facing, tier claims or receipts after its declared boundary. */
public final class TierThreeGameTest {
    @GameTest(maxTicks=20000,structure="prime_ants_test:surface_ground")
    public void workerBuiltStoneChambersConfirmAllFunctionsAndDeliverEveryTierThreeEffect(GameTestHelper c){
        effects(c,EffectWitness.COMBINED);
    }
    @GameTest(maxTicks=20000,structure="prime_ants_test:surface_ground")
    public void physicalOversizedInventoriesSurviveInMemoryReconstructionAndCapacityLoss(GameTestHelper c){
        effects(c,EffectWitness.INVENTORIES);
    }
    @GameTest(maxTicks=20000,structure="prime_ants_test:surface_ground")
    public void hallHasThreeConsecutiveGateQualifiedLayingIntervals(GameTestHelper c){
        effects(c,EffectWitness.HALL);
    }
    private enum EffectWitness { COMBINED, INVENTORIES, HALL }
    /** Owner-authorized hall feeding applies only to COMBINED/HALL; inventory-only retains its accepted profile. */
    private void effects(GameTestHelper c,EffectWitness witness){
        var fixture=new TierThreeFixture();var q=witness==EffectWitness.INVENTORIES?fixture.start(c,32,true):fixture.hallStart(c);
        FoodServiceProbe.watch(c,q,20000,"tier_three_"+witness);
        var probe=TierThreeEffectsProbe.watch(c,q,fixture);
        probe.emit("effect_witness",Map.of("kind",witness,"deadline",20000));
        long[] completed={-1},previousLoaded={-1};int[] measuredSteps={0},cadences={0},mostBrood={0};
        boolean[] food={false},material={false},foodWave={false};
        Map<UUID,Long> eggs=new HashMap<>();
        c.onEachTick(()->{
            var l=c.getLevel();fixture.feed(c,q);fixture.ledger(c,q);var pile=NestPlanFixture.pile(c,q);var cache=fixture.fx.f.cache(c,q);var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
            probe.sample();
            var jobs=ChamberUpgrade.get(l).jobs(q.getUUID());
            c.assertTrue(fixture.fx.food.total(c,q)==fixture.apples+fixture.chickens,"Independent physical and ingested food conservation");
            if(!fixture.complete(c,q))return;
            if(completed[0]<0){
                completed[0]=pile.loadedTicks();
                probe.completed(completed[0]);
                long payments=jobs.stream().mapToLong(j->j.built().size()).sum();
                c.assertTrue(payments==fixture.initialWalls.size()&&jobs.stream().flatMap(j->j.built().stream()).distinct().count()==payments,"One payment per unique physical wall; three jobs serve four functions");
                c.assertTrue(fixture.initialWalls.stream().allMatch(at->ColonyTerrain.get(l).built(l,at,q.getUUID(),NurseryBlocks.NEST_CUT_STONE)&&ColonyTerrain.get(l).retainedClay(l,at,q.getUUID())),"Every required physical wall converted with clay retained");
                c.assertTrue(jobs.stream().allMatch(j->j.released()==0&&j.taken()==j.built().size()),"All withdrawals physically paid, no cargo or release at completion");
                c.assertTrue(fixture.withdrawals==payments&&fixture.conversions==payments,"Every paid cell independently observed as actual cargo and supported conversion");
                PrimeAnts.LOGGER.info("T3 PHYSICAL COMPLETION queen={} tick={} loaded={} functions=4 jobs=3 cells={} stonePaid={} clayRetained={} claims=0 cargo=0",q.getUUID(),c.getTick(),pile.loadedTicks(),fixture.initialWalls.size(),payments,fixture.initialWalls.size());
                fixture.drop(c,q,Items.CLAY_BALL,72);fixture.drop(c,q,Items.COBBLESTONE,16);
            }
            c.assertTrue(pile.nursery().equals(new BroodCapacity.Nursery(15,4)),"Live fifteen slots and x2");
            mostBrood[0]=Math.max(mostBrood[0],pile.records().size());
            if(previousLoaded[0]>=0&&pile.loadedTicks()==previousLoaded[0]+1)for(var r:pile.records())if(r.stage()==BroodStage.EGG&&eggs.containsKey(r.id())&&r.neglectTicks()==0){
                c.assertTrue(r.progress()==eggs.get(r.id())+2,"Actual consecutive cared egg development spends exactly two steps");measuredSteps[0]++;
            }
            eggs.clear();pile.records().stream().filter(r->r.stage()==BroodStage.EGG&&r.neglectTicks()==0).forEach(r->eggs.put(r.id(),r.progress()));previousLoaded[0]=pile.loadedTicks();
            cadences[0]=probe.maxConsecutive();
            if(cache.size()>12){
                long sugar=cache.contents().stream().filter(s->!NestCache.protein(s)).count(),protein=cache.size()-sugar;
                c.assertTrue(cache.capacity()==18&&sugar<=12&&protein<=12&&sugar>0&&protein>0&&cache.getBlockState().getValue(NestCacheBlock.UNITS)==cache.size(),"Actual expanded food with protected shares and exact display");food[0]=true;
            }
            if(store.size()>64){
                var d=MaterialUnits.display(store.contents().stream().map(MaterialStore::kind).toList());
                c.assertTrue(store.capacity()==96&&store.size()<=96&&store.getBlockState().getValue(MaterialStoreBlock.CLAY)==d.clay()&&store.getBlockState().getValue(MaterialStoreBlock.STOCK)==d.stock(),"Expanded canonical material inventory agrees with visible stock");material[0]=true;
            }
            if(material[0]&&!foodWave[0]){foodWave[0]=true;fixture.drop(c,q,Items.APPLE,18);fixture.drop(c,q,Items.CHICKEN,18);}
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T3 EFFECT STATE queen={} tick={} completedLoaded={} settled={} mostNaturalBrood={} doubleSteps={} exactCadences={} foodAccepted={} materialAccepted={} foodWave={} foodNow={} materialsNow={} gate={}",q.getUUID(),c.getTick(),completed[0],pile.loadedTicks()-completed[0],mostBrood[0],measuredSteps[0],cadences[0],food[0],material[0],foodWave[0],cache.size(),store.size(),pile.growth().reason());
            if(c.getTick()>=20000)return; // Automatic timeout-lifecycle observations cannot become acceptance time.
            if(pile.loadedTicks()-completed[0]<200)return;
            if(witness==EffectWitness.HALL){
                if(cadences[0]<3)return;
                probe.emit("hall_witness_pass",Map.of("consecutiveIntervals",cadences[0],"settledLoaded",pile.loadedTicks()-completed[0]));c.succeed();return;
            }
            // Historical peaks describe behavior; they never authorize a serialization boundary.
            if(cache.size()<=12||store.size()<=64||mostBrood[0]<=10||measuredSteps[0]<20||witness==EffectWitness.COMBINED&&cadences[0]<3)return;
            reconstructionAndLoss(c,q,fixture,probe);
            PrimeAnts.LOGGER.info("T3 EFFECTS queen={} tick={} witness={} completedLoaded={} settled={} naturalBrood={} measuredDoubleSteps={} consecutiveCadences={} currentFood={} currentMaterials={} retainedClay={} scope=in_memory_serialization_reconstruction",q.getUUID(),c.getTick(),witness,completed[0],pile.loadedTicks()-completed[0],mostBrood[0],measuredSteps[0],cadences[0],fixture.fx.f.cache(c,q).size(),MaterialStore.owned(l,q.getUUID(),q.founding().plan()).size(),fixture.initialWalls.size());c.succeed();
        });
    }
    private static boolean equal(List<ItemStack> a,List<ItemStack> b){return a.size()==b.size()&&java.util.stream.IntStream.range(0,a.size()).allMatch(i->ItemStack.matches(a.get(i),b.get(i)));}
    private static void inventoryBoundary(GameTestHelper c,LasiusNigerEntity q,TierThreeFixture f,TierThreeEffectsProbe probe,String phase,List<ItemStack> food,List<ItemStack> material,int foodCapacity,int materialCapacity){
        var cache=f.fx.f.cache(c,q);var store=MaterialStore.owned(c.getLevel(),q.getUUID(),q.founding().plan());
        c.assertTrue(cache.size()>12&&store.size()>64,"Both ACTUAL CURRENT inventories exceed tier-two capacity at "+phase);
        c.assertTrue(equal(food,cache.contents())&&equal(material,store.contents()),"Every exact ordered item/count/component survives without truncation or duplication at "+phase);
        c.assertTrue(q.getUUID().equals(cache.componentOwner())&&q.getUUID().equals(store.componentOwner())&&cache.capacity()==foodCapacity&&store.capacity()==materialCapacity,"Canonical ownership and current live capacities at "+phase);
        c.assertTrue(NestCacheBlock.showing(cache.getBlockState(),cache.contents()).equals(cache.getBlockState()),"Exact current food display at "+phase);
        var d=MaterialUnits.display(store.contents().stream().map(MaterialStore::kind).toList());var s=store.getBlockState();
        c.assertTrue(s.getValue(MaterialStoreBlock.CLAY)==d.clay()&&s.getValue(MaterialStoreBlock.STOCK)==d.stock()&&s.getValue(MaterialStoreBlock.STONE)==d.stone()&&s.getValue(MaterialStoreBlock.GRAVEL)==d.gravel()&&s.getValue(MaterialStoreBlock.SAND)==d.sand()&&s.getValue(MaterialStoreBlock.ORE)==d.ore(),"Exact current material display at "+phase);
        probe.emit("inventory_boundary",Map.of("phase",phase,"scope","in_memory_serialization_reconstruction","inventories",probe.inventories()));
    }
    private static void reconstructionAndLoss(GameTestHelper c,LasiusNigerEntity q,TierThreeFixture f,TierThreeEffectsProbe probe){
        var l=c.getLevel();var cache=f.fx.f.cache(c,q);var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());var food=cache.contents();var material=store.contents();
        inventoryBoundary(c,q,f,probe,"immediately_before_serialization",food,material,18,96);
        reconstruct(c,q,f,probe,"tier_three",cache,store);
        inventoryBoundary(c,q,f,probe,"after_reconstruction",food,material,18,96);
        inventoryBoundary(c,q,f,probe,"immediately_before_capacity_loss",food,material,18,96);
        l.setBlock(q.founding().plan().entrance(),net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),3);
        inventoryBoundary(c,q,f,probe,"loaded_capacity_loss_before_serialization",food,material,6,32);
        cache=f.fx.f.cache(c,q);store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
        c.assertTrue(!cache.admits(new ItemStack(Items.APPLE))&&!cache.admits(new ItemStack(Items.CHICKEN))&&!store.room(new ItemStack(Items.CLAY_BALL))&&!store.room(new ItemStack(Items.COBBLESTONE)),"Reduced live capacities deny new food/material admission while retaining every oversized unit");
        reconstruct(c,q,f,probe,"reduced_capacity",cache,store);
        inventoryBoundary(c,q,f,probe,"after_reconstruction_under_reduced_capacity",food,material,6,32);
    }
    private static void reconstruct(GameTestHelper c,LasiusNigerEntity q,TierThreeFixture f,TierThreeEffectsProbe probe,String phase,NestCache cache,MaterialStore store){
        var l=c.getLevel();var foodTag=cache.saveWithFullMetadata(l.registryAccess());var materialTag=store.saveWithFullMetadata(l.registryAccess());
        probe.emit("serialized_tags",Map.of("phase",phase,"food",foodTag.toString(),"materials",materialTag.toString(),"scope","in_memory_only_no_disk_world_reload"));
        SurfaceFixture.loadBlock(c,cache,foodTag);SurfaceFixture.loadBlock(c,store,materialTag);
        var foodBack=f.fx.f.cache(c,q).saveWithFullMetadata(l.registryAccess());var materialBack=MaterialStore.owned(l,q.getUUID(),q.founding().plan()).saveWithFullMetadata(l.registryAccess());
        for(var key:List.of("Contents","Colony","Entrance","Direction","ReleaseId","InventoryFormat","SharesShed"))c.assertTrue(Objects.equals(foodTag.get(key),foodBack.get(key)),"Exact serialized food field at "+phase+": "+key);
        for(var key:List.of("Contents","Colony","Entrance","Direction","Placement","ReleaseId","InventoryFormat"))c.assertTrue(Objects.equals(materialTag.get(key),materialBack.get(key)),"Exact serialized material field at "+phase+": "+key);
    }
    @GameTest(maxTicks=6000,structure="prime_ants_test:surface_ground")
    public void workerBuiltTierThreeFoodStoreDoesNotOverbookFullFoodTripsAndStillReceivesPhysicalMaterials(GameTestHelper c){
        var fixture=new TierThreeFixture();var q=fixture.start(c,32);boolean[] supplied={false},full={false};long[] settled={-1};
        var probe=TierThreeEffectsProbe.watch(c,q,fixture,6000);
        c.onEachTick(()->{
            var l=c.getLevel();fixture.ledger(c,q);var cache=fixture.fx.f.cache(c,q);var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
            probe.sample();
            c.assertTrue(fixture.fx.food.total(c,q)==fixture.apples+fixture.chickens,"Every initial and newly supplied food unit remains physical or terminally consumed");
            if(!supplied[0]){
                if(ColonyDevelopment.confirmedTier(l,q.getUUID(),q.founding().plan(),ChamberRegistry.FOUNDING,ChamberFunction.FOOD_STORE)!=3)return;
                supplied[0]=true;fixture.drop(c,q,Items.APPLE,18);fixture.drop(c,q,Items.CHICKEN,18);fixture.drop(c,q,Items.CLAY_BALL,6);
                PrimeAnts.LOGGER.info("T3 FULL CACHE CAUSAL BOUNDARY queen={} tick={} actualWorkerBuiltFoundingWalls=8 foodWave=18/18 clayWave=6",q.getUUID(),c.getTick());
            }
            if(cache.size()==18){
                full[0]=true;long protein=cache.contents().stream().filter(NestCache::protein).count();
                c.assertTrue(protein>=6&&protein<=12&&cache.getBlockState().getValue(NestCacheBlock.UNITS)==18,"Full tier-three cache protects both six-unit reservations and its exact visible total");
            }
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T3 FULL CACHE CAUSAL STATE queen={} tick={} everFull={} cache={} storedClay={} groundClay={} groundFood={}/{} foragers={}",q.getUUID(),c.getTick(),full[0],cache.size(),store.units(MaterialUnits.Material.CLAY),NurseryUpgradeGameTest.ground(c,Items.CLAY_BALL),NurseryUpgradeGameTest.ground(c,Items.APPLE),NurseryUpgradeGameTest.ground(c,Items.CHICKEN),fixture.workers(c,q).stream().filter(w->q.founding().claimedBy(w)).map(w->w.getUUID()+":"+w.workerTasks().phase()+":"+w.workerTasks().reason()+":"+w.getMainHandItem()).toList());
            if(!full[0]||store.units(MaterialUnits.Material.CLAY)!=6)return;
            c.assertTrue(NurseryUpgradeGameTest.ground(c,Items.APPLE)+NurseryUpgradeGameTest.ground(c,Items.CHICKEN)>0,"Excess real food remains while materials are physically delivered");
            var pile=NestPlanFixture.pile(c,q);if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T3 FULL CACHE CAUSAL DONE queen={} tick={} fullFood=18 physicallyStoredClay=6 settled=200",q.getUUID(),c.getTick());c.succeed();
        });
    }
    private static ChamberUpgrade.Job founding(GameTestHelper c,LasiusNigerEntity q){return ChamberUpgrade.get(c.getLevel()).job(q.getUUID(),ChamberRegistry.FOUNDING,3);}
    private static LasiusNigerEntity builder(GameTestHelper c,ChamberUpgrade.Job j){return j!=null&&j.claim!=null&&c.getLevel().getEntity(j.claim) instanceof LasiusNigerEntity w?w:null;}
    @GameTest(maxTicks=16000,structure="prime_ants_test:surface_ground")
    public void stoneCargoReloadDeathAndUnknownPausePreserveExactMaterialLineage(GameTestHelper c){
        var fixture=new TierThreeFixture();LasiusNigerEntity[] q={fixture.start(c,32)};int[] step={0};long[] hiddenAt={-1},pendingAt={-1};
        UnavailableCells[] entrance={null},wall={null};UUID[] transfer={null},claim={null};List<BlockPos>[] prefix=new List[]{null};net.minecraft.world.entity.Entity[] holder={null};
        c.onEachTick(()->{
            var l=c.getLevel();fixture.feed(c,q[0]);fixture.ledger(c,q[0]);var pile=NestPlanFixture.pile(c,q[0]);var j=founding(c,q[0]);var w=builder(c,j);if(j==null)return;
            if(step[0]==0){
                if(j.built().size()<2||j.carried()!=1||w==null)return;
                var ledger=j.ledger();var cargo=w.getMainHandItem().copy();var paid=j.built();var actor=j.claim;
                disk(c);w=fixture.fx.f.restore(c,w);q[0]=fixture.fx.f.restore(c,q[0]);var back=founding(c,q[0]);
                c.assertTrue(back!=j&&back.ledger().equals(ledger)&&back.built().equals(paid)&&actor.equals(back.claim)&&ItemStack.matches(cargo,w.getMainHandItem())&&back.material.equals("minecraft:stone"),"Disk/data and canonical actor reload preserves actual carried stone, claim, prefix and material identity");
                PrimeAnts.LOGGER.info("T3 CARRIED RELOAD queen={} tick={} claim={} paid={} cargo={} material={}",q[0].getUUID(),c.getTick(),actor,paid.size(),cargo,back.material);step[0]=1;return;
            }
            if(step[0]==1){
                if(j.built().size()<3||j.carried()!=1||w==null)return;
                claim[0]=j.claim;prefix[0]=j.built();hiddenAt[0]=pile.loadedTicks();entrance[0]=UnavailableCells.hide(c,q[0].founding().plan().entrance());wall[0]=UnavailableCells.hide(c,j.next());step[0]=2;return;
            }
            if(step[0]==2){
                c.assertTrue(claim[0].equals(j.claim)&&j.carried()==1&&j.built().equals(prefix[0])&&w!=null&&j.accepts(w.getMainHandItem())&&!w.workerTasks().upgradeAuthorized(l),"UNKNOWN retains exact claim/cargo/prefix without work authorization");
                if(pile.loadedTicks()-hiddenAt[0]<600)return;
                entrance[0].close();wall[0].close();step[0]=3;PrimeAnts.LOGGER.info("T3 UNKNOWN SETTLED queen={} tick={} pausedLoaded=600 claim={} cargo=stone paid={}",q[0].getUUID(),c.getTick(),j.claim,j.built().size());return;
            }
            if(step[0]==3){
                if(j.built().size()<=prefix[0].size()||j.carried()!=1||w==null)return;
                transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+w.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                holder[0]=net.minecraft.world.entity.EntityTypes.MARKER.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);c.assertTrue(holder[0]!=null,"Named custody holder");holder[0].setUUID(transfer[0]);holder[0].setPos(w.position());c.assertTrue(l.addFreshEntity(holder[0]),"Expose actual pending custody");
                c.assertTrue(w.hurtServer(l,w.damageSources().genericKill(),1000)&&!w.isAlive(),"Real builder death");w.workerTasks().die(l);
                c.assertTrue(j.claim==null&&j.carried()==0&&j.released()==1&&j.custody(l)==1&&w.getMainHandItem().isEmpty(),"Exactly one stone released under one named custody identity, repeated callback idempotent");
                var ledger=j.ledger();disk(c);j=founding(c,q[0]);c.assertTrue(j.ledger().equals(ledger)&&j.custody(l)==1,"Pending stone custody reload retains cumulative release separately");pendingAt[0]=pile.loadedTicks();step[0]=4;
                PrimeAnts.LOGGER.info("T3 STONE DEATH queen={} tick={} transfer={} ledger={} pending=1",q[0].getUUID(),c.getTick(),transfer[0],j.ledger());return;
            }
            if(step[0]==4){
                c.assertTrue(j.released()==1&&j.custody(l)==1&&TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])&&t.stack().is(Items.STONE)&&t.stack().getCount()==1).count()==1,"One genuine pending stone identity");
                if(pile.loadedTicks()-pendingAt[0]<20)return;holder[0].discard();step[0]=5;return;
            }
            if(step[0]==5){
                if(j.custody(l)!=0)return;
                c.assertTrue(l.getEntity(transfer[0]) instanceof ItemEntity i&&i.getItem().is(Items.STONE)&&i.getItem().getCount()==1,"Named physical ground recovery keeps exact material");step[0]=6;return;
            }
            if(step[0]==6&&j.built().size()>prefix[0].size()+1&&j.released()==1){PrimeAnts.LOGGER.info("T3 INTERRUPTION DONE queen={} tick={} ledger={} material={} namedTransfer={}",q[0].getUUID(),c.getTick(),j.ledger(),j.material,transfer[0]);c.succeed();}
        });
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void missingFacingSupplyReleasesBuilderAndResumesTheSamePaidPrefix(GameTestHelper c){
        var fixture=new TierThreeFixture();var q=fixture.start(c,1);long[] wait={-1},settled={-1};ChamberUpgrade.Job[] original={null};boolean[] dropped={false};
        TierThreeReloadProbe.watch(c,q);List<BlockPos>[] previousPrefix=new List[]{List.of()};
        c.onEachTick(()->{
            fixture.feed(c,q);fixture.ledger(c,q);var pile=NestPlanFixture.pile(c,q);var j=founding(c,q);if(j==null)return;
            if(original[0]==null)original[0]=j;TierThreeReloadProbe.sameLogicalJob(c,q,original[0],j);
            c.assertTrue(j.built().size()>=previousPrefix[0].size()&&j.built().subList(0,previousPrefix[0].size()).equals(previousPrefix[0]),"Same job never loses or replaces its exact paid prefix across reconstruction/resumption");previousPrefix[0]=j.built();
            if(!dropped[0]){
                if(j.built().size()!=1||j.carried()!=0||j.claim!=null)return;
                if(wait[0]<0)wait[0]=pile.loadedTicks();
                c.assertTrue(j.taken()==1&&j.released()==0&&!ChamberUpgrade.get(c.getLevel()).hasPriority(c.getLevel(),q.getUUID(),q.founding().plan()),"No duplicate withdrawal/payment or false priority without exact facing supply");
                if(pile.loadedTicks()-wait[0]<600)return;
                fixture.drop(c,q,Items.STONE,43);dropped[0]=true;PrimeAnts.LOGGER.info("T3 MISSING SUPPLY RESUME queen={} tick={} emptyWaitLoaded=600 paidPrefix=1 stockCobble=31",q.getUUID(),c.getTick());
            }
            if(!fixture.complete(c,q))return;
            if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            c.assertTrue(ChamberUpgrade.get(c.getLevel()).jobs(q.getUUID()).stream().allMatch(a->a.released()==0&&a.taken()==a.built().size()),"Resumed completion has exact once-only payments and empty claims/cargo");c.succeed();
        });
    }
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void sameStatePlayerWallWriteReturnsFacingWithoutConversion(GameTestHelper c){protectedWall(c,false);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void foreignPackedClayWallReturnsFacingWithoutConversion(GameTestHelper c){protectedWall(c,true);}
    private void protectedWall(GameTestHelper c,boolean foreign){
        var fixture=new TierThreeFixture();var q=fixture.start(c,32);BlockPos[] changed={null};int[] paid={-1};long[] settled={-1};
        c.onEachTick(()->{
            fixture.feed(c,q);fixture.ledger(c,q);var l=c.getLevel();var j=founding(c,q);var w=builder(c,j);if(j==null)return;
            if(changed[0]==null){
                if(j.built().size()<2||j.carried()!=1||w==null)return;
                changed[0]=j.next();paid[0]=j.built().size();l.setBlock(changed[0],l.getBlockState(changed[0]),3);
                if(foreign)ColonyTerrain.get(l).built(changed[0],UUID.randomUUID(),NurseryBlocks.PACKED_CLAY);
                PrimeAnts.LOGGER.info("T3 TARGET REVOCATION queen={} tick={} foreign={} target={} paid={} cargo={}",q.getUUID(),c.getTick(),foreign,changed[0],paid[0],w.getMainHandItem());return;
            }
            c.assertTrue(l.getBlockState(changed[0]).is(NurseryBlocks.PACKED_CLAY)&&!ColonyTerrain.get(l).built(l,changed[0],q.getUUID(),NurseryBlocks.PACKED_CLAY)&&j.built().size()==paid[0],"Player/foreign target refuses conversion, including same-state write");
            if(!j.stopped()||j.claim!=null||j.carried()!=0)return;
            c.assertTrue(j.taken()==paid[0]&&j.released()==0&&j.ledger().exact(),"Real carried unit returned to its confirmed store with no payment or custody fabrication");
            var pile=NestPlanFixture.pile(c,q);if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T3 PROTECTION DONE queen={} tick={} foreign={} paid={} returned=1 ledger={}",q.getUUID(),c.getTick(),foreign,paid[0],j.ledger());c.succeed();
        });
    }
    private static void disk(GameTestHelper c){
        var l=c.getLevel();var before=TierThreeReloadProbe.before(c);l.getDataStorage().saveAndJoin();
        try(var storage=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
            var upgrades=storage.get(ChamberUpgrade.TYPE);var terrain=storage.get(ColonyTerrain.TYPE);var registry=storage.get(ChamberRegistry.TYPE);var custody=storage.get(TransferCustody.TYPE);
            c.assertTrue(upgrades!=null&&terrain!=null&&registry!=null,"Real disk/data reload");l.getDataStorage().set(ChamberUpgrade.TYPE,upgrades);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);l.getDataStorage().set(ChamberRegistry.TYPE,registry);if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
        }
        TierThreeReloadProbe.after(c,before);
    }
}
