package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.QueenFounding;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Stage-1 T07: the owner's stage hysteresis (decision of 2026-10-07) in real loaded ticks. A shortfall in counts, stocks
 * or tiers holds its stage for StageRules.GRACE loaded ticks (ColonyStageGameTest's worker deaths); a catastrophic loss
 * applies at once. Production egg founding and dropped food; the stage is only ever evaluated by the colony's nursery. */
public final class StageHysteresisGameTest {
    private final WorkerForagingGameTest f = new WorkerForagingGameTest();
    private final NursingGameTest food = new NursingGameTest();
    private static ColonyDevelopment.ChamberState founding(ColonyDevelopment.Evaluation e) { return e.chambers().stream().filter(s -> s.id().equals(ChamberRegistry.FOUNDING)).findFirst().orElseThrow(); }
    /** Young by the nursery's latest evaluation, both founding functions confirmed, no callow worker still maturing. */
    private boolean settledYoung(GameTestHelper c, LasiusNigerEntity q, BroodPile p) {
        var colony = ChamberRegistry.get(c.getLevel()).colony(q.getUUID()); var e = p.stageEvaluation();
        return colony != null && colony.stage() == ColonyStage.YOUNG && e != null && e.result().certain() == ColonyStage.YOUNG && founding(e).problem() == null
            && founding(e).functions().equals(Map.of(ChamberFunction.NURSERY, ColonyDevelopment.Presence.CONFIRMED, ChamberFunction.FOOD_STORE, ColonyDevelopment.Presence.CONFIRMED))
            && f.workers(c, q).stream().noneMatch(LasiusNigerEntity::isCallow);
    }

    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void realQueenDeathAloneDropsYoungWithFiveSurvivingWorkersAtNextEvaluation(GameTestHelper c) { queenDeath(c); }

    /** q1: no forged stage, adults or clock; lethal damage is the only loss. */
    private void queenDeath(GameTestHelper c) {
        var q = f.start(c); boolean[] supplied = {false}; boolean[] killed = {false};
        ColonyDevelopment.Evaluation[] before = {null}; long[] at = {0};
        java.util.Set<java.util.UUID> survivors = new java.util.HashSet<>();
        c.onEachTick(() -> {
            var l = c.getLevel(); var p = food.pile(c,q); if (p == null) return;
            if (!supplied[0] && q.founding().lifecycle() == QueenFounding.Lifecycle.OPEN) { supplied[0] = true; food.supply(c,q,10,8); }
            var e = p.stageEvaluation();
            if (!killed[0]) {
                if (!settledYoung(c,q,p) || f.workers(c,q).size() < 5) return;
                f.workers(c,q).forEach(w -> survivors.add(w.getUUID()));
                before[0] = e; at[0] = p.loadedTicks();
                c.assertTrue(q.hurtServer(l,q.damageSources().genericKill(),1000) && !q.isAlive(),"Real lethal damage kills the observed queen");
                killed[0] = true;
                PrimeAnts.LOGGER.info("T08 Q1 QUEEN KILLED queen={} tick={} pileTicks={} workers={}",q.getUUID(),c.getTick(),at[0],survivors.size());
                return;
            }
            if (e == before[0]) return;
            c.assertTrue(survivors.size() >= 5 && survivors.stream().allMatch(id -> l.getEntity(id) instanceof LasiusNigerEntity w && w.isAlive()),"At least five workers survive the lethal hit");
            c.assertTrue(founding(e).functions().equals(Map.of(ChamberFunction.NURSERY,ColonyDevelopment.Presence.CONFIRMED,ChamberFunction.FOOD_STORE,ColonyDevelopment.Presence.CONFIRMED)) && f.cache(c,q) != null,"The nursery and cache remain intact: " + e);
            c.assertTrue(e.inputs().queenDead() && e.inputs().adults().known() >= 5 && e.stage() == ColonyStage.FOUNDING && e.cap() == 5
                && ChamberRegistry.get(l).colony(q.getUUID()).stage() == ColonyStage.FOUNDING,"Queen death alone causes Founding, cap 5: " + e);
            c.assertTrue(p.stageEvaluatedAt() > at[0] && p.stageEvaluatedAt() - at[0] <= ColonyDevelopment.INTERVAL
                && e.result().unmetSince().isEmpty() && e.result().pending().isEmpty() && ChamberRegistry.get(l).colony(q.getUUID()).unmetSince().isEmpty(),"The very next production evaluation has no grace clock");
            PrimeAnts.LOGGER.info("T08 Q1 FOUNDING AT NEXT EVALUATION queen={} tick={} killedAt={} evaluatedAt={} workers={} evaluation={}",q.getUUID(),c.getTick(),at[0],p.stageEvaluatedAt(),survivors.size(),e);
            c.succeed();
        });
    }

    /** (h2) A catastrophic loss applies at once: the food store's cache block of a Young colony is destroyed, and the
     * nursery's very next evaluation, within one evaluation interval, drops the colony to Founding with Founding's cap.
     * No clock holds it, and every adult is still alive: the loss is the chamber function, not a count. */
    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void destroyedCacheDropsYoungColonyToFoundingAtOnce(GameTestHelper c) {
        var q = f.start(c); boolean[] supplied = {false}; int[] step = {0}; ColonyDevelopment.Evaluation[] last = {null}; long[] at = {0}, adults = {0};
        c.onEachTick(() -> {
            var l = c.getLevel(); var p = food.pile(c, q); if (p == null || step[0] == 2) return;
            if (!supplied[0] && q.founding().lifecycle() == QueenFounding.Lifecycle.OPEN) { supplied[0] = true; food.supply(c, q, 10, 8); }
            var e = p.stageEvaluation(); var plan = q.founding().plan();
            if (step[0] == 0) {
                if (!settledYoung(c, q, p) || f.cache(c, q) == null) return;
                c.assertTrue(e.result().unmetSince().isEmpty() && e.result().pending().isEmpty() && ChamberRegistry.get(l).colony(q.getUUID()).unmetSince().isEmpty(), "A settled Young colony has no shortfall and no clock: " + e);
                // The cache block is destroyed in the loaded world: its stored food goes to transfer custody (NestCache).
                adults[0] = e.inputs().adults().known(); l.setBlock(plan.cache(), Blocks.AIR.defaultBlockState(), 3); last[0] = e; at[0] = p.loadedTicks(); step[0] = 1;
                PrimeAnts.LOGGER.info("T07 H2 CACHE DESTROYED queen={} pileTicks={} evaluation={}", q.getUUID(), at[0], e);
                return;
            }
            if (e == last[0]) return; // the nursery's own next evaluation
            var state = founding(e); var colony = ChamberRegistry.get(l).colony(q.getUUID());
            c.assertTrue(f.cache(c, q) == null && state.functions().get(ChamberFunction.FOOD_STORE) == ColonyDevelopment.Presence.ABSENT
                && state.functions().get(ChamberFunction.NURSERY) == ColonyDevelopment.Presence.CONFIRMED && "food_store_marker_missing_or_foreign".equals(state.problem()),
                "The destroyed cache is an observed loss of the food store; the nursery stays confirmed: " + e);
            c.assertTrue(e.stage() == ColonyStage.FOUNDING && colony.stage() == ColonyStage.FOUNDING && e.cap() == ColonyStage.FOUNDING.adultCap()
                && e.result().missing(ColonyStage.YOUNG).toString().equals("[food_store 0/1]") && e.inputs().adults().known() >= 5,
                "The Young colony drops to Founding with Founding's cap at the first evaluation after the loss, with its adults alive: " + e);
            c.assertTrue(p.stageEvaluatedAt() > at[0] && p.stageEvaluatedAt() - at[0] <= ColonyDevelopment.INTERVAL && e.result().unmetSince().isEmpty() && e.result().pending().isEmpty()
                && colony.unmetSince().isEmpty(), "At once: within one evaluation interval of the loss, and no clock holds the stage: lost at " + at[0] + ", evaluated at " + p.stageEvaluatedAt());
            PrimeAnts.LOGGER.info("T07 H2 FOUNDING AT ONCE queen={} lostAt={} evaluatedAt={} evaluation={}", q.getUUID(), at[0], p.stageEvaluatedAt(), e);
            step[0] = 2; c.succeed();
        });
    }
}
