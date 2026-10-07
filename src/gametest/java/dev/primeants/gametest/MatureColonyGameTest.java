package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

/** Stage-1 T06 in real loaded ticks: a colony grown to Mature by real play (fed as the long path's player feeds it,
 * NurseryUpgradeGameTest.feed) whose nursery is rebuilt to tier 2, its upgrade's clay counted place by place through a
 * builder's death. Production founding, digging, hauling and upgrade work; nothing assigns a stage, a tier, a job or an
 * inventory. */
public final class MatureColonyGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();
    private final NurseryUpgradeGameTest player = new NurseryUpgradeGameTest();
    /** Every clay ball the player dropped, each place counted on its own. */
    record Clay(long ground, long carried, long stored, long walls, long custody) {
        long total() { return ground + carried + stored + walls + custody; }
    }
    Clay clay(GameTestHelper c, LasiusNigerEntity q) {
        var l = c.getLevel(); var box = c.getBounds().inflate(8); var s = NestPlanFixture.store(c, q);
        return new Clay(l.getEntitiesOfClass(ItemEntity.class, box, i -> i.isAlive() && i.getItem().is(Items.CLAY_BALL)).stream().mapToInt(i -> i.getItem().getCount()).sum(),
            fx.f.workers(c, q).stream().filter(w -> w.getMainHandItem().is(Items.CLAY_BALL)).mapToInt(w -> w.getMainHandItem().getCount()).sum(),
            s == null ? 0 : s.units(MaterialUnits.Material.CLAY),
            NurseryUpgradeGameTest.walls(q.founding().plan()).stream().filter(b -> ColonyTerrain.get(l).built(l, b, q.getUUID(), NurseryBlocks.PACKED_CLAY)).count() * NestWalls.CLAY_PER_CELL,
            TransferCustody.get(l).contents().stream().filter(t -> t.stack().is(Items.CLAY_BALL) && box.contains(t.position())).mapToInt(t -> t.stack().getCount()).sum());
    }

    /** 1c. Eighteen clay balls, counted every tick in each place on its own: on the ground, in a worker's mandibles, in the
     * store, rammed into the colony's own walls, and in transfer custody. The upgrade's builder is killed with a unit in
     * its mandibles: the job's released count becomes one for good, while the unit's custody record lives only until
     * custody sets it down; the job's own custody figure always equals the custody records under its builder's transfer
     * identity. Its equation, taken = carried + built + released, holds at every tick, through to the eighth wall. */
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void upgradeCountsEveryClayUnitInEachPlaceThroughItsBuilderDeath(GameTestHelper c){
        var q=fx.start(c);long[] fed={0,0,0,0,-1};UUID[] killed={null},transfer={null};long[] custodyTicks={0},groundTicks={0};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);player.feed(c,q,18,fed);fx.soil(c,q);if(p==null||fed[0]==0)return;
            var where=clay(c,q);var j=NurseryUpgradeGameTest.upgrade(c,q);
            c.assertTrue(where.total()==18,"Every clay ball is in exactly one place: "+where);
            if(j==null)return;
            var ledger=j.ledger();var builder=NurseryUpgradeGameTest.builder(c,fx,q,j);
            long carried=builder!=null&&builder.getMainHandItem().is(Items.CLAY_BALL)?builder.getMainHandItem().getCount():0;
            long custodyNow=transfer[0]==null?0:TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])).mapToInt(t->t.stack().getCount()).sum();
            c.assertTrue(ledger.exact()&&ledger.carried()==carried&&ledger.built()==where.walls()&&ledger.released()==(killed[0]==null?0:1)&&j.custody(l)==custodyNow&&j.custody(l)<=ledger.released(),
                "Taken = carried + built + released, each figure matching its own place; custody now is its own figure: "+ledger+" custodyNow="+j.custody(l)+" "+where);
            if(transfer[0]!=null){
                boolean entity=l.getEntity(transfer[0]) instanceof ItemEntity i&&i.isAlive();
                c.assertTrue(!(custodyNow>0&&entity),"The released unit is in custody or set down, never both");
                if(custodyNow>0)custodyTicks[0]++;if(entity)groundTicks[0]++;
            }
            if(killed[0]==null){
                if(j.built().size()<2||j.carried()!=1||builder==null)return;
                killed[0]=builder.getUUID();transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+builder.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                builder.hurtServer(l,builder.damageSources().genericKill(),1000);
                c.assertTrue(!builder.isAlive()&&j.claim==null&&j.ledger().released()==1&&j.carried()==0&&j.ledger().exact(),"A real lethal hit releases the builder's unit once: "+j.ledger());
                PrimeAnts.LOGGER.info("T06 LEDGER BUILDER KILLED queen={} tick={} ledger={} custodyNow={} places={}",q.getUUID(),c.getTick(),j.ledger(),j.custody(l),clay(c,q));return;
            }
            c.assertFalse(killed[0].equals(j.claim),"The dead builder never builds again");
            if(!j.complete()||j.claim!=null)return;
            c.assertTrue(j.ledger().equals(new NestWalls.Ledger(9,0,8,1))&&j.custody(l)==0&&where.walls()==8,"Eight walls from nine units taken, one released for good and none left in custody: "+j.ledger());
            PrimeAnts.LOGGER.info("T06 LEDGER DONE queen={} tick={} ledger={} custodyNow={} places={} custodyTicks={} releasedUnitGroundTicks={}",q.getUUID(),c.getTick(),j.ledger(),j.custody(l),where,custodyTicks[0],groundTicks[0]);
            c.succeed();
        });
    }

}
