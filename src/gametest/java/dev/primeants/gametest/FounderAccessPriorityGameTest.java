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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** New controlled setup, never founder acceptance. All access, clay hauling and hall work use real ticks. */
public final class FounderAccessPriorityGameTest {
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void physicalClayDeliveryBeforeHallUpgradeCannotTerminallyLoseTheFiniteAccessPair(GameTestHelper c){run(c);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void copy1PhysicalClayDeliveryBeforeHallUpgradeCannotTerminallyLoseTheFiniteAccessPair(GameTestHelper c){run(c);}
    @GameTest(maxTicks=12000,structure="prime_ants_test:surface_ground")
    public void copy2PhysicalClayDeliveryBeforeHallUpgradeCannotTerminallyLoseTheFiniteAccessPair(GameTestHelper c){run(c);}
    private void run(GameTestHelper c){
        var f=new SurfaceFixture();var q=f.accessHabitat(c);var l=c.getLevel();var home=q.founding().plan();
        // New declared starting habitat only, completed before any real tick. Original fixture remains byte-unchanged.
        var registry=ChamberRegistry.get(l);var initialHall=registry.colony(q.getUUID()).chamber(ChamberExcavation.HALL);
        var origins=NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        var hallWalls=ChamberUpgrade.walls(home,initialHall);c.assertTrue(hallWalls.size()==14,"Controlled geometry has fourteen hall walls; four remain declared initial paid clay");
        for(var at:hallWalls.subList(0,10)){l.setBlock(at,Blocks.DIRT.defaultBlockState(),3);origins.addProperty(Long.toString(at.asLong()),"minecraft:dirt");}
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(JsonOps.INSTANCE,origins).getOrThrow());
        registry.register(q.getUUID(),new ChamberRegistry.Chamber(initialHall.id(),initialHall.min(),initialHall.max(),initialHall.functions(),1,initialHall.markers()));
        PrimeAnts.LOGGER.info("T05 CAUSAL INITIAL queen={} nurseryAndStoreTier=2 hallTier=1 existingHallClay=4 convertibleHallEarth=10 soil=72 clay=0 laborReceipts=0 beforeFirstTick=true",q.getUUID());
        f.snapshot(c,q);var probe=FounderAccessProbe.watch(c,q,12000);
        long[] prefixAt={-1},settled={-1};int[] supplied={0};UUID[] binding={null};List<UUID> units=new ArrayList<>();List<String> prefix=new ArrayList<>();boolean[] enabled={false},pairPhysical={false};
        c.onEachTick(()->{
            var pile=NestPlanFixture.pile(c,q);if(pile==null)return;
            f.soil(c,q);f.protectedTerrain(c,q,settled[0]>=0);c.assertTrue(f.fx.food.total(c,q)==39,"Exact finite existing food across ordinary service and growth");
            var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());c.assertTrue(store!=null,"Same controlled physical store");
            var hall=ChamberUpgrade.get(l).job(q.getUUID(),ChamberExcavation.HALL,2);
            long ground=l.getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.CLAY_BALL)).stream().mapToInt(i->i.getItem().getCount()).sum();
            long carried=f.fx.f.workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.CLAY_BALL)).mapToInt(w->w.getMainHandItem().getCount()).sum();
            long custody=TransferCustody.get(l).contents().stream().filter(t->t.stack().is(Items.CLAY_BALL)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
            int built=hall==null?0:hall.built().size();c.assertTrue(ground+carried+custody+store.units(MaterialUnits.Material.CLAY)+built==supplied[0],"Every supplied clay unit is in a physical location or an actual paid hall wall");
            if(hall!=null)c.assertTrue(hall.ledger().exact()&&hall.released()==0,"Original hall recipe/custody and exclusive builder remain exact");
            var j=SurfaceWork.get(l).job(q.getUUID(),ColonyStage.MATURE);if(j==null)return;
            if(supplied[0]==0&&j.completed()>=1){
                f.fx.drop(c,j.home.at(-4,0,1),new ItemStack(Items.CLAY_BALL));supplied[0]=1;
                PrimeAnts.LOGGER.info("T05 CAUSAL DROP queen={} tick={} loaded={} receiptPrefix={} units=1 ordinaryPickupRequired=true noCargoFabricated=true",q.getUUID(),c.getTick(),pile.loadedTicks(),j.receipts());return;
            }
            if(j.completed()<4)return;
            if(prefixAt[0]<0){prefixAt[0]=pile.loadedTicks();prefix.addAll(j.receipts().subList(0,4));c.assertTrue(j.recovered()==2&&j.placed()==2,"Ordinary workers earned the four original prefix receipts");}
            c.assertTrue(j.receipts().subList(0,4).equals(prefix),"Original immutable prefix survives ordering and priority");
            var a=j.access;
            if(a!=null){if(binding[0]==null){binding[0]=a.binding;units.addAll(a.operations.stream().map(op->op.unit).toList());}
                c.assertTrue(binding[0].equals(a.binding)&&units.equals(a.operations.stream().map(op->op.unit).toList())&&a.prefix.equals(prefix),"One attempt, same two unit identities and saved prefix");
                c.assertTrue(a.removed()+a.pickups()==a.redeposited()+a.carried()+a.released()&&a.pickups()==0&&a.released()==0&&a.operations.stream().allMatch(op->op.deaths==0),"No death, pickup, released custody or duplicate unit is introduced to solve priority");
            }
            if(!enabled[0]&&supplied[0]==1&&(store.units(MaterialUnits.Material.CLAY)>0||hall!=null)){
                enabled[0]=true;PrimeAnts.LOGGER.info("T05 CAUSAL ENABLED queen={} tick={} loaded={} storeClay={} hall={} access={} actualPhysicalDelivery=true",q.getUUID(),c.getTick(),pile.loadedTicks(),store.units(MaterialUnits.Material.CLAY),hall==null?null:hall.ledger(),a==null?null:a.encode());
                f.fx.drop(c,j.home.at(-4,0,1),new ItemStack(Items.CLAY_BALL,9));supplied[0]=10;
            }
            c.assertTrue(pile.loadedTicks()-prefixAt[0]<6000,"Original finite access and actual target must recover beyond the priority boundary within6000 loaded ticks");
            if(a!=null&&a.complete()&&!pairPhysical[0]){
                c.assertTrue(a.removed()==2&&a.redeposited()==2&&a.carried()==0,"Both exact authorized relocations physically finish");
                for(var op:a.operations)c.assertTrue(ColonyTerrain.get(l).accessOpen(l,op.source,q.getUUID(),op.unit)&&ColonyTerrain.get(l).accessMound(l,op.destination,q.getUUID(),op.unit),"Same named source is actual air and destination holds actual owned soil");
                pairPhysical[0]=true;PrimeAnts.LOGGER.info("T05 CAUSAL PAIR queen={} tick={} state={} physicalBoth=true soil=72",q.getUUID(),c.getTick(),a.encode());
            }
            if(!enabled[0]||!pairPhysical[0]||j.completed()<5||hall==null||!hall.complete()||hall.claim!=null)return;
            c.assertTrue(ColonyTerrain.get(l).surface(l,j.at(4),q.getUUID())&&l.getBlockState(j.at(4)).is(NurseryBlocks.NEST_SOIL),"Ordinary worker physically constructs the exact motivating target after both relocations");
            c.assertTrue(hall.taken()==10&&hall.built().size()==10&&ground==0&&carried==0&&custody==0&&store.units(MaterialUnits.Material.CLAY)==0,"Ten ordinary paid hall cells settle all ten finite clay units");
            if(settled[0]<0)settled[0]=pile.loadedTicks();if(pile.loadedTicks()-settled[0]<200)return;
            PrimeAnts.LOGGER.info("T05 CAUSAL DONE queen={} tick={} loaded={} originalPrefix={} binding={} units={} access={} actualMotivatingTarget={} paidReceipts={} clayBuilt=10 soil=72 food=39 settled={} firstStop={}",q.getUUID(),c.getTick(),pile.loadedTicks(),prefix,binding[0],units,a.encode(),j.at(4),j.receipts(),pile.loadedTicks()-settled[0],probe.firstStop());c.succeed();
        });
    }
}
