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
import net.minecraft.world.item.*;

/** u2: safety on the newly supported material-store walls, using only physical production work and supplies. */
public final class StoreUpgradeSafetyGameTest {
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void storeUpgradeRestoresNonemptyWorkAndCustodySurvivesBuilderDeathAndPreservesPlayerNextWall(GameTestHelper c){safety(c);}
    private static ChamberUpgrade.Job job(GameTestHelper c,LasiusNigerEntity q){return ChamberUpgrade.get(c.getLevel()).job(q.getUUID(),ChamberExcavation.STORE,2);}
    private void safety(GameTestHelper c){
        var player=new TierTwoFixture(24);LasiusNigerEntity[] q={player.fx.start(c)};var stages=StageEvaluations.watch(c,q[0].getUUID());
        int[] step={0},builtAtChange={0};UUID[] killed={null},transfer={null};BlockPos[] changed={null};net.minecraft.world.entity.Entity[] holder={null};
        boolean[] custodyRecovered={false};long[] pendingTicks={0};
        c.onEachTick(()->{
            var l=c.getLevel();player.feed(c,q[0]);player.ledgers(c,q[0]);player.trace(c,q[0],"U2");
            var p=NestPlanFixture.pile(c,q[0]);if(p==null)return;var j=job(c,q[0]);if(j==null)return;
            c.assertTrue(stages.promotion>=0&&NurseryUpgradeGameTest.upgrade(c,q[0]).complete(),"The founding chamber was physically upgraded first");
            var builder=NurseryUpgradeGameTest.builder(c,player.fx,q[0],j);
            if(changed[0]!=null)c.assertTrue(l.getBlockState(changed[0]).is(dev.primeants.brood.NurseryBlocks.PACKED_CLAY)
                &&!ColonyTerrain.get(l).built(l,changed[0],q[0].getUUID(),dev.primeants.brood.NurseryBlocks.PACKED_CLAY)&&ColonyTerrain.get(l).wallTier(l,changed[0],q[0].getUUID())==1,"The player's next wall is preserved and never credited");
            if(transfer[0]!=null){
                long custody=TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])).count();
                boolean dropped=l.getEntity(transfer[0]) instanceof ItemEntity i&&i.isAlive();
                c.assertTrue(custody<=1&&!(custody>0&&dropped)&&j.released()==1&&j.custody(l)<=1,"Exactly-once custody is separate from the cumulative released ledger");
                if(custody>0)pendingTicks[0]++;
            }
            if(step[0]==0){
                if(j.built().size()<2||j.carried()!=1||builder==null)return;
                var store=NestPlanFixture.store(c,q[0]);var cargo=builder.getMainHandItem().copy();var ledger=j.ledger();var built=j.built();var claim=j.claim;var contents=store.contents();long total=NurseryUpgradeGameTest.clay(c,player.fx,q[0]);
                diskRestore(c);var loaded=player.fx.f.restore(c,builder);q[0]=player.fx.f.restore(c,q[0]);
                var marker=store.getBlockPos();var tag=store.saveWithFullMetadata(l.registryAccess());var state=store.getBlockState();l.removeBlockEntity(marker);
                var back=(MaterialStore)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(marker,state,tag,l.registryAccess());c.assertTrue(back!=null,"Normal store restore");l.setBlockEntity(back);
                for(var cell:built){
                    var chunk=l.getChunkAt(cell);var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(l,chunk);
                    var read=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(l,l.palettedContainerFactory(),serial.write()).read(l,l.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",l.dimension(),"chunk"),chunk.getPos());
                    c.assertTrue(read.getBlockState(cell).is(dev.primeants.brood.NurseryBlocks.PACKED_CLAY),"Every converted cell survives a normal chunk reconstruction");
                }
                var after=job(c,q[0]);
                c.assertTrue(after!=j&&after.ledger().equals(ledger)&&after.built().equals(built)&&claim.equals(after.claim)&&ItemStack.matches(loaded.getMainHandItem(),cargo)
                    &&back.contents().size()==contents.size()&&java.util.stream.IntStream.range(0,contents.size()).allMatch(i->ItemStack.matches(contents.get(i),back.contents().get(i)))
                    &&NurseryUpgradeGameTest.clay(c,player.fx,q[0])==total,"Nonempty store job, claim, cargo, all converted cells and every stored unit restore exactly");
                PrimeAnts.LOGGER.info("T08 U2 MID-JOB RELOAD queen={} promotion={} tick={} chamber={} built={} claim={} cargo={} ledger={} store={}",q[0].getUUID(),stages.promotion,c.getTick(),after.chamber,built.size(),claim,cargo,ledger,contents.size());step[0]=1;return;
            }
            if(step[0]==1){
                if(j.built().size()<3||j.carried()!=1||builder==null)return;
                killed[0]=builder.getUUID();transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+builder.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var marker=net.minecraft.world.entity.EntityTypes.MARKER.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);c.assertTrue(marker!=null,"Custody identity holder");
                marker.setUUID(transfer[0]);marker.setPos(builder.position());c.assertTrue(l.addFreshEntity(marker),"The marker holds custody's transfer identity");holder[0]=marker;
                c.assertTrue(builder.hurtServer(l,builder.damageSources().genericKill(),1000)&&!builder.isAlive()&&builder.getMainHandItem().isEmpty()&&ColonyMembers.get(l).member(killed[0]).dead()
                    &&j.claim==null&&j.carried()==0&&j.released()==1&&j.ledger().exact(),"A real builder death releases one unit and ends its claim exactly once");
                PrimeAnts.LOGGER.info("T08 U2 BUILDER DEATH queen={} tick={} worker={} transfer={} ledger={} custody={}",q[0].getUUID(),c.getTick(),killed[0],transfer[0],j.ledger(),j.custody(l));step[0]=2;return;
            }
            c.assertTrue(!killed[0].equals(j.claim),"The dead builder never resumes or duplicates its identity");
            if(step[0]==2){
                c.assertTrue(j.custody(l)==1&&TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])&&t.stack().is(Items.CLAY_BALL)&&t.stack().getCount()==1).count()==1,"Pending custody is genuinely nonempty");
                var ledger=j.ledger();diskRestore(c);var after=job(c,q[0]);
                c.assertTrue(after!=j&&after.ledger().equals(ledger)&&after.custody(l)==1&&NurseryUpgradeGameTest.clay(c,player.fx,q[0])==player.clay,"Pending custody and job ledger survive the disk-backed reload exactly");
                PrimeAnts.LOGGER.info("T08 U2 PENDING CUSTODY RELOAD queen={} tick={} ledger={} pending={} transfer={}",q[0].getUUID(),c.getTick(),after.ledger(),after.custody(l),transfer[0]);step[0]=3;return;
            }
            if(step[0]==3){holder[0].discard();step[0]=4;return;}
            if(step[0]==4&&!custodyRecovered[0]){
                if(j.custody(l)>0)return;
                c.assertTrue(l.getEntity(transfer[0]) instanceof ItemEntity i&&i.isAlive()&&i.getItem().is(Items.CLAY_BALL)&&i.getItem().getCount()==1&&j.released()==1,"The released unit is recovered once, while released remains one");
                custodyRecovered[0]=true;PrimeAnts.LOGGER.info("T08 U2 CUSTODY RECOVERED queen={} tick={} pendingTicks={} ledger={}",q[0].getUUID(),c.getTick(),pendingTicks[0],j.ledger());
            }
            if(step[0]==4){
                if(!custodyRecovered[0]||j.built().size()<6||j.carried()!=1||builder==null)return;
                c.assertTrue(!builder.getUUID().equals(killed[0]),"A different real builder recovered the job and converted further cells");
                changed[0]=j.next();builtAtChange[0]=j.built().size();l.setBlock(changed[0],dev.primeants.brood.NurseryBlocks.PACKED_CLAY.defaultBlockState(),3);step[0]=5;
                PrimeAnts.LOGGER.info("T08 U2 PLAYER CHANGED NEXT WALL queen={} tick={} next={} built={} ledger={}",q[0].getUUID(),c.getTick(),changed[0],builtAtChange[0],j.ledger());return;
            }
            if(!j.stopped()||j.claim!=null)return;
            c.assertTrue(j.reason.startsWith("stopped_wall_cell_not_colony_earth_at_")&&j.built().size()==builtAtChange[0]&&j.built().equals(j.cells.subList(0,builtAtChange[0]))
                &&!j.built().contains(changed[0])&&j.carried()==0&&j.released()==1&&j.taken()==j.built().size()+1&&j.custody(l)==0&&pendingTicks[0]>0&&custodyRecovered[0],"Recovery stops at the player cell, returns its unspent unit and never duplicates conversion or delivery: "+j.ledger());
            c.assertTrue(ColonyDevelopment.confirmedTier(l,q[0].getUUID(),q[0].founding().plan(),ChamberExcavation.STORE,ChamberFunction.MATERIAL_STORE)==1,"Player wall retains tier one for this chamber");
            PrimeAnts.LOGGER.info("T08 U2 DONE queen={} promotion={} end={} built={} stoppedAt={} pendingTicks={} ledger={} stored={} clayTotal={}",q[0].getUUID(),stages.promotion,c.getTick(),j.built().size(),changed[0],pendingTicks[0],j.ledger(),NestPlanFixture.store(c,q[0]).size(),player.clay);
            stages.close();c.succeed();
        });
    }
    private static void diskRestore(GameTestHelper c){
        var l=c.getLevel();l.getDataStorage().saveAndJoin();
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
            var upgrades=disk.get(ChamberUpgrade.TYPE);var registry=disk.get(ChamberRegistry.TYPE);var excavation=disk.get(ChamberExcavation.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);var custody=disk.get(TransferCustody.TYPE);
            c.assertTrue(upgrades!=null&&registry!=null&&excavation!=null&&terrain!=null,"Disk stores every work/ownership registry");
            l.getDataStorage().set(ChamberUpgrade.TYPE,upgrades);l.getDataStorage().set(ChamberRegistry.TYPE,registry);l.getDataStorage().set(ChamberExcavation.TYPE,excavation);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);
            if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
        }
    }
}
