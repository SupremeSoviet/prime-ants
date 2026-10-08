package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.founding.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Early entrance regression and its loaded obstruction control; Mature stability is covered by TierTwoGameTest. */
public final class HallAvailabilityGameTest {
    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void observedQueenKeepsUnknownHallThroughUnavailablePlugThenLoadedObstructionIsRealLoss(GameTestHelper c) {
        var fx = new NestPlanFixture(); var q = fx.start(c); boolean[] supplied = {false};
        int[] step = {0}, restored = {0}; long[] hiddenAt = {0}; UnavailableCells[] hidden = {null};
        ColonyDevelopment.Evaluation[] seen = {null};
        c.onEachTick(() -> {
            fx.grow(c,q,supplied); fx.soil(c,q); var p = NestPlanFixture.pile(c,q); if (p == null) return;
            var e = p.stageEvaluation(); if (e == null || e == seen[0]) return; seen[0] = e;
            var plan = q.founding().plan();
            var hall = NestPlanFixture.presence(e,ChamberExcavation.HALL,ChamberFunction.QUEENS_HALL);
            if (step[0] == 0) {
                if (hall != ColonyDevelopment.Presence.CONFIRMED) return;
                hidden[0] = UnavailableCells.hide(c,plan.plugs().getFirst()); hiddenAt[0] = p.loadedTicks(); step[0] = 1; return;
            }
            c.assertTrue(q.isAlive() && c.getLevel().getEntity(q.getUUID()) == q,"Living queen stays observed");
            if (step[0] == 1) {
                c.assertTrue(hall == ColonyDevelopment.Presence.UNKNOWN && e.result().unmetSince().keySet().stream().noneMatch(k ->
                    k.equals("queens_hall") || k.equals("queens_hall_tier") || k.endsWith(":queens_hall") || k.endsWith(":queens_hall_tier")) && e.stage() == ColonyStage.YOUNG,
                    "An unavailable plug is unknown and starts no hall-loss clock: " + e);
                if (p.loadedTicks() - hiddenAt[0] < 600) return;
                hidden[0].close(); step[0] = 2; return;
            }
            if (step[0] == 2) {
                c.assertTrue(hall == ColonyDevelopment.Presence.CONFIRMED,"Normal confirmation resumes: " + e);
                if (++restored[0] < 2) return;
                hidden[0] = UnavailableCells.hide(c,plan.plugs().getFirst());
                c.getLevel().setBlock(plan.plugs().getLast(),Blocks.STONE.defaultBlockState(),3); step[0] = 3; return;
            }
            c.assertTrue(hall == ColonyDevelopment.Presence.ABSENT && e.stage() == ColonyStage.FOUNDING && e.result().unmetSince().isEmpty(),"Loaded obstruction beats the unavailable plug: " + e);
            PrimeAnts.LOGGER.info("T08 UNKNOWN ENTRANCE CONTROL PASS queen={} tick={} hiddenLoadedTicks={} restoredEvaluations={} evaluation={}",q.getUUID(),c.getTick(),p.loadedTicks()-hiddenAt[0],restored[0],e);
            hidden[0].close(); c.succeed();
        });
    }
}
