package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** u1 + natural b1: production founding, growth, hauling and three physical upgrades. Fixed bound before its first run. */
public final class TierTwoGameTest {
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void naturalMatureColonyUpgradesEveryFunctionAndShowsExpandedBroodAndUnknownEntranceStability(GameTestHelper c){longPath(c);}
    private void longPath(GameTestHelper c){
        var player=new TierTwoFixture(64,true);var q=player.fx.start(c);var scope=StageEvaluations.watch(c,q.getUUID());
        var tiers=new LinkedHashMap<String,Long>();ColonyDevelopment.Evaluation[] seen={null};
        long[] lastEgg={0},reloadedAt={-1},eggProgress={-1},hiddenAt={-1};UUID[] egg={null};
        boolean[] naturalDisplay={false},reloadStep={false},foodEffect={false},materialEffect={false},hallEffect={false};
        int[] mostBrood={0},restored={0};UnavailableCells[] entrance={null},plug={null};
        String[] diagnostic={""};long[] supplyTotals={0,0,0},receipts={-1,-1};
        c.onEachTick(()->{
            var l=c.getLevel();player.feed(c,q);player.ledgers(c,q);player.trace(c,q,"U1/B1");
            var p=NestPlanFixture.pile(c,q);if(p==null)return;var e=p.stageEvaluation();var plan=q.founding().plan();
            var supply=p.supply(l);
            if(player.apples!=supplyTotals[0]||player.chickens!=supplyTotals[1]||player.clay!=supplyTotals[2]){
                PrimeAnts.LOGGER.info("T11 TIER TWO ACCEPTED SUPPLY queen={} tick={} delta={}/{}/{} totals={}/{}/{} feeding={} hallSugarWaves={} lastApple={} lastChicken={} lastWorkMeal={} lastWorkProtein={} materialEnd={} triggerSupply={}",q.getUUID(),c.getTick(),player.apples-supplyTotals[0],player.chickens-supplyTotals[1],player.clay-supplyTotals[2],player.apples,player.chickens,player.clay,player.feeding,player.hallSugarWaves,player.lastApple,player.lastChicken,player.lastWorkMeal,player.lastWorkProtein,player.materialEnd,supply);
                supplyTotals[0]=player.apples;supplyTotals[1]=player.chickens;supplyTotals[2]=player.clay;
            }
            String prerequisites="tiers="+tiers.keySet()+" food="+foodEffect[0]+" material="+materialEffect[0]+" hall="+hallEffect[0]+" naturalDisplay="+naturalDisplay[0]+" reload="+reloadStep[0]+" unknownStarted="+(hiddenAt[0]>=0)+" restored="+restored[0]+" gate="+p.growth().reason();
            if(!prerequisites.equals(diagnostic[0])||c.getTick()%200==0&&(supply.foodSugar()!=receipts[0]||supply.foodProtein()!=receipts[1])||c.getTick()%1000==0){
                diagnostic[0]=prerequisites;receipts[0]=supply.foodSugar();receipts[1]=supply.foodProtein();
                PrimeAnts.LOGGER.info("T11 TIER TWO PHASE DIAGNOSIS queen={} tick={} loaded={} {} brood={} credit={} recentIncome={}/{} observedIncomeTicks={} intakeReceipts={}/{} supply={} groundApple={} groundChicken={} lastApple={} eligibleHallWave={}",q.getUUID(),c.getTick(),p.loadedTicks(),prerequisites,p.records().size(),saved(c,p).getIntOr("DevelopmentCredit",-1),p.growth().recentSugar(),p.growth().recentProtein(),p.growth().observedTicks(),supply.foodSugar(),supply.foodProtein(),supply,NurseryUpgradeGameTest.ground(c,net.minecraft.world.item.Items.APPLE),NurseryUpgradeGameTest.ground(c,net.minecraft.world.item.Items.CHICKEN),player.lastApple,player.feeding&&e!=null&&e.inputs().tier(ChamberFunction.QUEENS_HALL).known()==2&&NurseryUpgradeGameTest.ground(c,net.minecraft.world.item.Items.APPLE)==0&&supply.storedSugar()<24000&&c.getTick()-player.lastApple>=2000);
            }
            c.assertTrue(q.isAlive()&&l.getEntity(q.getUUID())==q,"The living queen remains observed throughout");
            c.assertTrue(scope.loss==null,"Mature at EVERY production evaluation after promotion: "+scope.loss);
            c.assertTrue(scope.falseHallClock==null,"Unknown hall terrain never starts its loss clock: "+scope.falseHallClock);
            if(scope.promotion>=0&&e!=null)c.assertTrue(e.stage()==ColonyStage.MATURE&&ChamberRegistry.get(l).colony(q.getUUID()).stage()==ColonyStage.MATURE,"Mature through completion");
            if(e!=null&&e!=seen[0]){
                seen[0]=e;
                for(var state:e.chambers())if(state.tier()==2&&state.functions().values().stream().allMatch(v->v==ColonyDevelopment.Presence.CONFIRMED)&&!tiers.containsKey(state.id())){
                    tiers.put(state.id(),c.getTick());PrimeAnts.LOGGER.info("T08 U1 CHAMBER TIER TWO queen={} tick={} chamber={} promotion={} state={}",q.getUUID(),c.getTick(),state.id(),scope.promotion,state);
                }
                if(entrance[0]==null&&hiddenAt[0]>=0){
                    c.assertTrue(Arrays.stream(ChamberFunction.values()).allMatch(f->e.inputs().tier(f).known()==2),"Normal confirmation restores all four functions at tier 2: "+e);restored[0]++;
                }
            }
            var cache=player.fx.f.cache(c,q);var store=NestPlanFixture.store(c,q);
            if(cache!=null&&cache.size()>6){
                c.assertTrue(cache.capacity()==12&&cache.contents().stream().filter(NestCache::protein).count()<=8
                    &&cache.contents().stream().filter(s->!NestCache.protein(s)).count()<=8&&l.getBlockState(cache.getBlockPos()).getValue(NestCacheBlock.UNITS)==cache.size(),"Real tier-two food capacity/shares and complete visible total");foodEffect[0]=true;
            }
            if(store!=null&&store.size()>32){c.assertTrue(store.capacity()==64&&store.contents().size()<=64,"More than 32 real material units in the live tier-two store");materialEffect[0]=true;}
            if(p.lastLayingTick()!=lastEgg[0]){
                long interval=p.lastLayingTick()-lastEgg[0];lastEgg[0]=p.lastLayingTick();
                if(tiers.containsKey(ChamberExcavation.HALL)&&interval<BroodPile.layingCadence()){
                    c.assertTrue(interval>=BroodPile.layingCadence(2)&&p.layingInterval(l)==BroodPile.layingCadence(2)&&p.supply(l).complete()
                        &&p.records().size()<=p.nursery().slots()&&p.stageEvaluation().cap()==60,"Hall lays faster than tier one's minimum while respecting its own cadence, food gate, slots and adult cap: "+interval);
                    hallEffect[0]=true;PrimeAnts.LOGGER.info("T08 U1 HALL LAYING EFFECT queen={} tick={} interval={} tierOneMinimum={} tierTwoCadence={} gate={} supply={}",q.getUUID(),c.getTick(),interval,BroodPile.layingCadence(),p.layingInterval(l),p.growth().reason(),p.supply(l));
                }
            }
            var records=p.records();mostBrood[0]=Math.max(mostBrood[0],records.size());
            if(records.size()>3&&!naturalDisplay[0]){
                var block=l.getBlockState(p.getBlockPos());int beyond=(int)records.stream().filter(r->r.slot()>=3).count();
                java.util.function.IntFunction<BroodStage> stage=s->records.stream().filter(r->r.slot()==s).map(BroodRecord::stage).findFirst().orElse(BroodStage.EMPTY);
                c.assertTrue(block.getValue(BroodPileBlock.A)==stage.apply(0)&&block.getValue(BroodPileBlock.B)==stage.apply(1)&&block.getValue(BroodPileBlock.C)==stage.apply(2)
                    &&block.getValue(BroodPileBlock.MORE)==BroodPileBlock.more(beyond)&&beyond>0,"Natural records show all first-three stages and the extra heap: "+block);
                naturalDisplay[0]=true;PrimeAnts.LOGGER.info("T08 B1 NATURAL MORE THAN THREE queen={} tick={} records={} slots={} block={}",q.getUUID(),c.getTick(),records.stream().map(r->r.id()+"@"+r.slot()+":"+r.stage()+":"+r.progress()).toList(),p.nursery(),block);
            }
            if(reloadedAt[0]>=0&&!reloadStep[0]){
                if(p.loadedTicks()==reloadedAt[0])return;
                var r=records.stream().filter(b->b.id().equals(egg[0])).findFirst().orElseThrow();
                c.assertTrue(p.loadedTicks()==reloadedAt[0]+1&&r.stage()==BroodStage.EGG&&r.progress()==eggProgress[0]+2&&p.nursery().speed()==3
                    &&saved(c,p).getIntOr("DevelopmentCredit",-1)==0,"The FIRST post-reload production step spends credit 1 as two steps: "+eggProgress[0]+" -> "+r.progress());
                reloadStep[0]=true;PrimeAnts.LOGGER.info("T08 B1 FIRST RESTORED STEP queen={} tick={} progress={}->{} credit=0 records={}",q.getUUID(),c.getTick(),eggProgress[0],r.progress(),records.size());
            }
            if(naturalDisplay[0]&&records.size()>3&&reloadedAt[0]<0&&p.nursery().speed()==3){
                var mid=records.stream().filter(r->!r.founding()&&r.stage()==BroodStage.EGG&&r.neglectTicks()==0&&r.progress()>0&&r.progress()<p.stageDuration()-4).findFirst().orElse(null);
                var tag=saved(c,p);
                if(mid!=null&&tag.getIntOr("DevelopmentCredit",-1)==1){
                    var pos=p.getBlockPos();var state=p.getBlockState();l.removeBlockEntity(pos);
                    var back=(BroodPile)BlockEntity.loadStatic(pos,state,tag,l.registryAccess());c.assertTrue(back!=null,"Normal natural expanded-pile restore");l.setBlockEntity(back);var again=saved(c,back);
                    c.assertTrue(Objects.equals(again.get("Brood"),tag.get("Brood"))&&back.records().stream().map(r->r.id()+"@"+r.slot()+":"+r.stage()+":"+r.progress()).toList()
                        .equals(records.stream().map(r->r.id()+"@"+r.slot()+":"+r.stage()+":"+r.progress()).toList()),"Every natural record and slot, including full nutrition/survival fields, restores exactly");
                    for(var key:List.of("LoadedTicks","LastLayingTick","StageDuration","DevelopmentCredit","Original","Consumed","GrowthFlow"))c.assertTrue(Objects.equals(again.get(key),tag.get(key)),"Exact natural pile clock/history/credit: "+key);
                    c.assertTrue(back.getBlockState().equals(state),"The aggregate display survives the restore");
                    egg[0]=mid.id();eggProgress[0]=mid.progress();reloadedAt[0]=back.loadedTicks();
                    PrimeAnts.LOGGER.info("T08 B1 NATURAL RELOAD queen={} tick={} loadedTicks={} egg={} progress={} credit=1 records={}",q.getUUID(),c.getTick(),back.loadedTicks(),egg[0],eggProgress[0],again.get("Brood"));return;
                }
            }
            if(tiers.size()!=3||!foodEffect[0]||!materialEffect[0]||!hallEffect[0]||!reloadStep[0])return;
            c.assertTrue(ChamberUpgrade.get(l).jobs(q.getUUID()).size()==3&&ChamberUpgrade.get(l).jobs(q.getUUID()).stream().allMatch(j->j.complete()&&j.claim==null&&j.carried()==0&&j.released()==0),"Three physical jobs serve four functions, with no duplicate founding job");
            if(e!=null&&e.inputs().tier(ChamberFunction.NURSERY).known()==2)c.assertTrue(p.nursery().equals(new BroodCapacity.Nursery(10,3)),"Nursery has ten slots and x1.5 while confirmed");
            c.assertTrue(NurseryBlocks.NEST_CACHE.getStateDefinition().getPossibleStates().size()==832&&NurseryBlocks.MATERIAL_STORE.getStateDefinition().getPossibleStates().size()==2448,"Actual bounded display state counts: food 832, material 2448");
            if(hiddenAt[0]<0){
                entrance[0]=UnavailableCells.hide(c,plan.entrance());plug[0]=UnavailableCells.hide(c,plan.plugs().getFirst());hiddenAt[0]=p.loadedTicks();
                PrimeAnts.LOGGER.info("T08 U1 ENTRANCE UNKNOWN START queen={} tick={} pileTicks={} promotion={} tiers={}",q.getUUID(),c.getTick(),hiddenAt[0],scope.promotion,tiers);return;
            }
            if(entrance[0]!=null){
                if(p.loadedTicks()-hiddenAt[0]<600)return;
                c.assertTrue(scope.unknownHall>=6&&scope.falseHallClock==null&&p.stageEvaluation().chambers().stream().anyMatch(s->s.functions().get(ChamberFunction.QUEENS_HALL)==ColonyDevelopment.Presence.UNKNOWN),"At least 600 nursery loaded ticks of unknown hall with no false clock");
                entrance[0].close();plug[0].close();entrance[0]=null;plug[0]=null;
                PrimeAnts.LOGGER.info("T08 U1 ENTRANCE RESTORED queen={} tick={} hiddenLoadedTicks={} unknownEvaluations={}",q.getUUID(),c.getTick(),p.loadedTicks()-hiddenAt[0],scope.unknownHall);return;
            }
            if(restored[0]<2)return;
            c.assertTrue(scope.loss==null&&scope.held>0&&naturalDisplay[0]&&mostBrood[0]>3,"Complete natural brood and the entire Mature stability window");
            PrimeAnts.LOGGER.info("T08 U1/B1 DONE queen={} promotion={} promotionLoaded={} tiers={} end={} everyMatureEvaluation={} unknownHallEvaluations={} restoredEvaluations={} mostNaturalBrood={} foodEffect={} materialEffect={} hallEffect={} clay={}",
                q.getUUID(),scope.promotion,scope.promotionLoaded,tiers,c.getTick(),scope.held,scope.unknownHall,restored[0],mostBrood[0],foodEffect[0],materialEffect[0],hallEffect[0],player.clay);
            scope.close();c.succeed();
        });
    }
    private static CompoundTag saved(GameTestHelper c,BroodPile pile){return pile.saveWithFullMetadata(c.getLevel().registryAccess());}
}
