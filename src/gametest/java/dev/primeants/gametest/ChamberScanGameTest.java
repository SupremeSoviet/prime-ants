package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** The dug-chamber damage scan in real loaded ticks (T03 review): a completed store cell is reported unavailable
 * (UnavailableCells), and the only loaded shell cell beside it, (9,1,-2) in the plan frame, really breaks. The control
 * comes first: with the shell intact the unavailable cell is unknown and the Young stage holds. Then the breach is
 * loss in brood care's habitat scan and in the stage, for a partly dug and for a finished store. Production founding,
 * dropped food and real brood-derived workers; the stage is only evaluated by the colony's own nursery. */
public final class ChamberScanGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();
    private static final BlockPos HIDDEN = new BlockPos(9, 2, -2), WALL = new BlockPos(9, 1, -2); // forward, side, dy
    private static BlockPos at(LasiusNigerEntity q, BlockPos plan) { return q.founding().plan().at(plan.getX(), plan.getY(), plan.getZ()); }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void partlyDugStoreBesideAnUnavailableCompletedCellLosesItsLoadedBrokenShellInBothScans(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false};int[] step={0},removed={0};ColonyDevelopment.Evaluation[] last={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);fx.soil(c,q);if(p==null||step[0]==3)return;
            var job=NestPlanFixture.job(c,q,ChamberExcavation.STORE);if(job==null)return;
            var e=p.stageEvaluation();var hidden=at(q,HIDDEN);var wall=at(q,WALL);
            if(step[0]==0){
                if(job.removed()<12||job.complete())return;
                c.assertTrue(job.placement.equals("right")&&job.completed().contains(hidden)&&!job.tasks.contains(wall)&&job.surfaces().contains(wall)&&NestPlan.loaded(l,wall)&&l.getBlockState(wall).isSolidRender()
                    &&NestPlanFixture.foundingConfirmed(e)&&e.stage()==ColonyStage.YOUNG,"Partly dug right store: (9,2,-2) completed, (9,1,-2) its solid loaded shell: "+job.completed()+" "+e);
                removed[0]=job.removed();UnavailableCells.hide(c,hidden);last[0]=e;step[0]=1;
                PrimeAnts.LOGGER.info("T04 SCAN PARTIAL HIDDEN queen={} hidden={} wall={} removed={}",q.getUUID(),hidden,wall,removed[0]);return;
            }
            c.assertTrue(job.removed()==removed[0],"The paused job digs nothing while its space is unknown or damaged");
            if(e==last[0])return; // the nursery's own next evaluation
            var state=NestPlanFixture.chamber(e,ChamberRegistry.FOUNDING);last[0]=e;
            var habitat=q.founding().plan().nurseryFindings(l,q.getUUID(),true);var own=new Findings();job.findings(l,q.getUUID(),own);
            if(step[0]==1){
                c.assertTrue(!NestPlan.loaded(l,hidden)&&own.verdict()==Findings.Verdict.UNKNOWN&&"material_store_chunk_unavailable".equals(own.problem())
                    &&habitat.verdict()==Findings.Verdict.UNKNOWN&&state.functions().equals(Map.of(ChamberFunction.NURSERY,ColonyDevelopment.Presence.UNKNOWN,ChamberFunction.FOOD_STORE,ColonyDevelopment.Presence.UNKNOWN))
                    &&e.stage()==ColonyStage.YOUNG&&ChamberRegistry.get(l).colony(q.getUUID()).stage()==ColonyStage.YOUNG,"Control: an unavailable completed cell with an intact shell is unknown and the colony stays Young: "+e);
                PrimeAnts.LOGGER.info("T04 SCAN PARTIAL CONTROL queen={} habitat={} evaluation={}",q.getUUID(),habitat.problem(),e);
                l.setBlock(wall,Blocks.AIR.defaultBlockState(),3);step[0]=2;return;
            }
            c.assertTrue(!NestPlan.loaded(l,hidden)&&NestPlan.loaded(l,wall)&&l.getBlockState(wall).isAir()&&own.verdict()==Findings.Verdict.DAMAGED&&"material_store_shell_or_support_open".equals(own.problem())
                &&habitat.verdict()==Findings.Verdict.DAMAGED&&"material_store_shell_or_support_open".equals(q.founding().plan().nurseryProblem(l,q.getUUID(),true))
                &&state.functions().equals(Map.of(ChamberFunction.NURSERY,ColonyDevelopment.Presence.ABSENT,ChamberFunction.FOOD_STORE,ColonyDevelopment.Presence.ABSENT))
                &&"material_store_shell_or_support_open".equals(state.problem())&&e.stage()==ColonyStage.FOUNDING&&e.result().possible()==ColonyStage.FOUNDING,
                "A loaded broken shell cell beside an unavailable completed cell is loss in brood care and the stage: "+e);
            PrimeAnts.LOGGER.info("T04 SCAN PARTIAL LOSS queen={} habitat={} evaluation={}",q.getUUID(),habitat.problem(),e);step[0]=3;c.succeed();
        });
    }

    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void finishedStoreBesideAnUnavailableCellLosesItsLoadedBrokenShellInBothScans(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false};int[] step={0};ColonyDevelopment.Evaluation[] last={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);fx.grow(c,q,supplied);fx.soil(c,q);if(p==null||step[0]==3)return;
            var job=NestPlanFixture.job(c,q,ChamberExcavation.STORE);if(job==null)return;
            var e=p.stageEvaluation();var hidden=at(q,HIDDEN);var wall=at(q,WALL);var registered=ChamberRegistry.get(l).colony(q.getUUID()).chamber(ChamberExcavation.STORE);
            if(step[0]==0){
                if(!job.complete()||!job.established(l,q.getUUID())||!NestPlanFixture.storeConfirmed(e)||!NestPlanFixture.foundingConfirmed(e))return;
                c.assertTrue(job.placement.equals("right")&&registered!=null&&e.stage()==ColonyStage.YOUNG&&NestPlan.loaded(l,wall)&&l.getBlockState(wall).isSolidRender(),"A confirmed, registered right store: "+e);
                UnavailableCells.hide(c,hidden);last[0]=e;step[0]=1;
                PrimeAnts.LOGGER.info("T04 SCAN FINISHED HIDDEN queen={} hidden={} wall={}",q.getUUID(),hidden,wall);return;
            }
            if(e==last[0])return; // the nursery's own next evaluation
            last[0]=e;var founding=NestPlanFixture.chamber(e,ChamberRegistry.FOUNDING);var store=NestPlanFixture.chamber(e,ChamberExcavation.STORE);
            var habitat=q.founding().plan().nurseryFindings(l,q.getUUID(),true);var own=new Findings();job.findings(l,q.getUUID(),own);
            var room=ChamberExcavation.roomFindings(l,q.getUUID(),q.founding().plan(),registered.min(),registered.max());
            c.assertTrue(own.verdict()==room.verdict()&&Objects.equals(own.problem(),room.problem()),"Brood care's scan of the finished job and the stage's room scan reach the same verdict: "+own.problem()+" / "+room.problem());
            if(step[0]==1){
                c.assertTrue(!NestPlan.loaded(l,hidden)&&room.verdict()==Findings.Verdict.UNKNOWN&&habitat.verdict()==Findings.Verdict.UNKNOWN
                    &&store.functions().get(ChamberFunction.MATERIAL_STORE)==ColonyDevelopment.Presence.UNKNOWN
                    &&founding.functions().equals(Map.of(ChamberFunction.NURSERY,ColonyDevelopment.Presence.UNKNOWN,ChamberFunction.FOOD_STORE,ColonyDevelopment.Presence.UNKNOWN))
                    &&e.stage()==ColonyStage.YOUNG&&e.inputs().tier(ChamberFunction.MATERIAL_STORE).possible()==1&&e.inputs().clay().possible()>=16,
                    "Control: the unavailable cell leaves the store and the founding functions unknown and the colony Young: "+e);
                PrimeAnts.LOGGER.info("T04 SCAN FINISHED CONTROL queen={} room={} evaluation={}",q.getUUID(),room.problem(),e);
                l.setBlock(wall,Blocks.AIR.defaultBlockState(),3);step[0]=2;return;
            }
            c.assertTrue(!NestPlan.loaded(l,hidden)&&l.getBlockState(wall).isAir()&&room.verdict()==Findings.Verdict.DAMAGED&&"material_store_shell_or_support_open".equals(room.problem())
                &&habitat.verdict()==Findings.Verdict.DAMAGED&&"material_store_shell_or_support_open".equals(q.founding().plan().nurseryProblem(l,q.getUUID(),true))
                &&store.functions().get(ChamberFunction.MATERIAL_STORE)==ColonyDevelopment.Presence.ABSENT
                &&founding.functions().equals(Map.of(ChamberFunction.NURSERY,ColonyDevelopment.Presence.ABSENT,ChamberFunction.FOOD_STORE,ColonyDevelopment.Presence.ABSENT))
                &&e.stage()==ColonyStage.FOUNDING&&e.result().possible()==ColonyStage.FOUNDING,"The same breach is loss in both scans: the store and the founding functions are lost: "+e);
            PrimeAnts.LOGGER.info("T04 SCAN FINISHED LOSS queen={} room={} evaluation={}",q.getUUID(),room.problem(),e);step[0]=3;c.succeed();
        });
    }
}
