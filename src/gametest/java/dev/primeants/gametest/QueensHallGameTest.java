package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodStage;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.ColonyMembers;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The queen's hall in real loaded ticks: a Young colony with a confirmed store digs the hall beside the queen's own
 * chamber, on the widening side its widening did not take. The queen and her brood pile never move, so the queen stays
 * ready and no egg misses care from the hall's planning on, and eggs develop during the dig itself; the hall registers
 * once and is confirmed by the colony's own nursery, and a real breach in its loaded shell loses the function.
 * Production founding, dropped food, real brood-derived workers. Laying is food-limited (T22), so the fixture drops more
 * food when the store is planned, thousands of ticks before the hall is dug. */
public final class QueensHallGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void youngColonyDigsTheQueensHallBesideHerChamberWhileEggsDevelopAndLosesItWhenItsShellBreaks(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false},fed={false},planned={false},confirmed={false},broken={false};int[] last={0};long[] confirmedAt={-1};
        Map<UUID,Long> eggs=new HashMap<>();long[] developed={0};Map<BlockPos,BlockState> before=new HashMap<>();ColonyDevelopment.Evaluation[] seen={null};BlockPos[] wall={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);fx.soil(c,q);if(p==null)return;
            var e=p.stageEvaluation();var plan=q.founding().plan();
            // More dropped food once the store is planned: a full cache feeds the next hunger wave, which reopens laying.
            if(!fed[0]&&NestPlanFixture.job(c,q,ChamberExcavation.STORE)!=null){fed[0]=true;fx.food.supply(c,q,12,8);}
            if(c.getTick()%500==0&&!broken[0])PrimeAnts.LOGGER.info("T04 hall brood tick={} pile={} records={} cache={}",c.getTick(),p.condition(),p.records().stream().map(r->r.stage()+":"+r.progress()).toList(),fx.f.cache(c,q)==null?null:fx.f.cache(c,q).size());
            var hall=NestPlanFixture.job(c,q,ChamberExcavation.HALL);if(hall==null)return;
            var widening=NestExpansion.get(l).job(q.getUUID());
            if(!planned[0]){
                planned[0]=true;
                c.assertTrue(hall.removed()==0&&hall.placement.equals("hall_left")&&widening!=null&&widening.complete()&&widening.side==1&&hall.tasks.size()==12&&hall.built.marker()==null
                    &&e.stage()==ColonyStage.YOUNG&&NestPlanFixture.storeConfirmed(e)&&e.result().missing(ColonyStage.MATURE).stream().anyMatch(m->m.requirement().name().equals("queens_hall")&&!m.unknown()),
                    "Planned only for a Young colony with a confirmed store that certainly lacks a hall, on the side its widening did not take: "+hall.placement+" "+e);
                for(int x=-7;x<=13;x++)for(int z=-7;z<=7;z++)for(int y=-4;y<=3;y++){var b=plan.entrance().offset(x,y,z);before.put(b,l.getBlockState(b));}
                PrimeAnts.LOGGER.info("T04 HALL PLANNED queen={} placement={} tasks={} widening={} evaluation={}",q.getUUID(),hall.placement,hall.tasks.size(),widening.side,e);
                return;
            }
            if(!broken[0]){
                // The queen and her pile stay put: from the planning on she is ready and no egg misses care.
                c.assertTrue(q.founding().ready(),"The queen stays ready while her chamber's wall opens: "+q.founding().reason());
                for(var r:p.records()){
                    c.assertTrue(!r.neglectReason().equals("egg_care_missing")&&!(r.stage()==BroodStage.EGG&&r.neglectTicks()>0),"Eggs are cared for while the hall is dug and open: "+r.id()+" "+r.neglectReason());
                    if(hall.removed()==0||hall.complete())continue; // count development during the dig only
                    if(r.stage()==BroodStage.EGG){var was=eggs.put(r.id(),r.progress());if(was!=null&&r.progress()>was)developed[0]++;}
                    else if(eggs.remove(r.id())!=null)developed[0]++; // an egg became a larva
                }
                c.assertTrue(!p.condition().equals("caregiver_absent_or_out_of_reach")&&p.expired().values().stream().noneMatch(v->v.contains("egg_care_missing")),"Egg care never misses the queen: "+p.condition());
            }
            if(!confirmed[0]){
                c.assertTrue(hall.removed()-last[0]<=1&&!hall.stopped(),"At most one removal per loaded tick, never stopped on colony soil");last[0]=hall.removed();
                if(e!=seen[0]){seen[0]=e;c.assertTrue(NestPlanFixture.foundingConfirmed(e)&&NestPlanFixture.storeConfirmed(e)&&e.stage()==ColonyStage.YOUNG,"The hall's cells are authorized openings: the founding functions and the store stay confirmed: "+e);}
                for(var w:fx.f.workers(c,q))if(w.getUUID().equals(hall.claim))
                    c.assertTrue(ColonyMembers.get(l).belongs(w,q.getUUID(),plan.chamber())&&!w.isCallow()&&!q.founding().claimedBy(w)&&(!w.getMainHandItem().is(Items.DIRT)||w.getMainHandItem().getCount()<=QueenFounding.CARRY_CAPACITY),"The builder is one real mature member, never the forager, carrying at most four units");
                if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T04 hall tick={} removed={} deposited={} reason={} pile={} records={}",c.getTick(),hall.removed(),hall.deposited,hall.reason,p.condition(),p.records().stream().map(r->r.stage()+":"+r.progress()).toList());
                if(!hall.complete()||NestPlanFixture.presence(e,ChamberExcavation.HALL,ChamberFunction.QUEENS_HALL)!=ColonyDevelopment.Presence.CONFIRMED)return;
                var colony=ChamberRegistry.get(l).colony(q.getUUID());var halls=colony.chambers().stream().filter(ch->ch.functions().contains(ChamberFunction.QUEENS_HALL)).toList();
                c.assertTrue(halls.size()==1&&halls.getFirst().id().equals(ChamberExcavation.HALL)&&halls.getFirst().tier()==1&&halls.getFirst().markers().isEmpty()
                    &&halls.getFirst().min().equals(hall.built.min())&&halls.getFirst().max().equals(hall.built.max())&&colony.chambers().size()==3,"Exactly one registered tier-1 hall chamber, its own entry with no marker block: "+colony);
                var names=e.result().missing(ColonyStage.MATURE).stream().map(m->m.requirement().name()).toList();
                c.assertTrue(NestPlanFixture.chamber(e,ChamberExcavation.HALL).problem()==null&&e.inputs().tier(ChamberFunction.QUEENS_HALL).known()==1&&!names.contains("queens_hall")
                    &&names.contains("adults")&&e.stage()==ColonyStage.YOUNG,"The evaluator confirms the hall; the colony stays Young: "+e);
                c.assertTrue(hall.removed()==12&&hall.deposited==12&&hall.released==0,"Twelve real removals, all on the mound");
                c.assertTrue(developed[0]>0,"Eggs developed during the dig: "+developed[0]);
                for(var b:hall.tasks)c.assertTrue(ColonyTerrain.get(l).opened(l,b,q.getUUID()),"Every hall cell is this colony's worker opening: "+b);
                var store=NestPlanFixture.job(c,q,ChamberExcavation.STORE);
                Set<BlockPos> allowed=new HashSet<>(hall.tasks);allowed.addAll(hall.surfaces());allowed.addAll(store.tasks);allowed.addAll(store.surfaces());allowed.addAll(NestExpansion.deposits(plan));allowed.add(plan.nursery());allowed.add(plan.cache());
                before.forEach((b,old)->c.assertTrue(old.equals(l.getBlockState(b))||allowed.contains(b),"No edit outside the planned cells, their prepared shell and the mound: "+b+" "+old+" -> "+l.getBlockState(b)));
                confirmed[0]=true;confirmedAt[0]=c.getTick();PrimeAnts.LOGGER.info("T04 HALL CONFIRMED queen={} tick={} eggsDeveloped={} registry={} evaluation={}",q.getUUID(),c.getTick(),developed[0],colony,e);
                return;
            }
            if(!broken[0]){
                c.assertTrue(NestPlanFixture.presence(e,ChamberExcavation.HALL,ChamberFunction.QUEENS_HALL)==ColonyDevelopment.Presence.CONFIRMED,"The hall is still confirmed while eggs develop beside it: "+e);
                PrimeAnts.LOGGER.info("T04 HALL EGGS DEVELOPED queen={} tick={} developed={} sinceConfirmed={}",q.getUUID(),c.getTick(),developed[0],c.getTick()-confirmedAt[0]);
                // A real breach in the hall's loaded outer wall, beside its middle floor cell.
                wall[0]=plan.at(4,-4,-2);
                c.assertTrue(hall.surfaces().contains(wall[0])&&NestPlan.loaded(l,wall[0])&&l.getBlockState(wall[0]).isSolidRender(),"The hall's outer wall is solid loaded shell");
                l.setBlock(wall[0],Blocks.AIR.defaultBlockState(),3);broken[0]=true;seen[0]=e;return;
            }
            if(e==seen[0])return; // the nursery's own next evaluation
            var state=NestPlanFixture.chamber(e,ChamberExcavation.HALL);var colony=ChamberRegistry.get(l).colony(q.getUUID());
            c.assertTrue(state.functions().get(ChamberFunction.QUEENS_HALL)==ColonyDevelopment.Presence.ABSENT&&"queens_hall_shell_or_support_open".equals(state.problem())
                &&"queens_hall_shell_or_support_open".equals(plan.nurseryProblem(l,q.getUUID(),true))&&e.inputs().tier(ChamberFunction.QUEENS_HALL).possible()==0
                &&colony.chambers().stream().filter(ch->ch.id().equals(ChamberExcavation.HALL)).count()==1,"A breach in the hall's loaded shell loses the function; brood care sees the same breach: "+e);
            PrimeAnts.LOGGER.info("T04 HALL BREACH LOST queen={} wall={} evaluation={}",q.getUUID(),wall[0],e);c.succeed();
        });
    }
}
