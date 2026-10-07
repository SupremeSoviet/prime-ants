package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.ColonyStage;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Stage-1 T07 (p1), the owner's decision: a site whose 0.1.0 founding deposit list is full once the queen has laid her
 * 22 units still opens its nest. The opening forager lays the two plug units on the colony's stage mound plan; a Founding
 * colony has no plan of its own, so they go on Young's, its first mound. Production egg founding on the shared pad; a
 * player's cobblestone on the founding deposit cells beyond the 22 the queen needs makes the tight site (in ordinary
 * worlds 351 of 1,833 surveyed sites have 22 or 23). */
public final class PlugSoilGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();
    /** The site the queen's own search chooses from where she stands (QueenFounding, SEEKING). */
    static NestPlan site(ServerLevel l, LasiusNigerEntity q) {
        BlockPos ground = q.blockPosition().below();
        for (int r = 0; r <= 3; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos column = ground.offset(dx, 0, dz); if (!NestPlan.loaded(l, column)) continue;
                NestPlan candidate = null;
                for (int dy = 0; dy <= 2 && candidate == null; dy++) candidate = NestPlan.candidate(l, column.offset(0, dy == 0 ? 0 : dy == 1 ? 1 : -1, 0), d);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }
    /** A founding deposit cell's distance from the approach lane. */
    static int side(NestPlan p, BlockPos b) {
        var cw = p.direction().getClockWise(); return Math.abs((b.getX() - p.entrance().getX()) * cw.getStepX() + (b.getZ() - p.entrance().getZ()) * cw.getStepZ());
    }

    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void fullFoundingDepositListStillOpensTheNestWithBothPlugUnitsOnTheStageMound(GameTestHelper c) {
        var founding = new QueenFoundingGameTest(); founding.terrain(c, Blocks.DIRT.defaultBlockState(), true, true);
        var q = founding.egg(c); var l = c.getLevel();
        var open = site(l, q);
        c.assertTrue(open != null && open.deposits().size() > NestPlan.HARD_CAP - 2, "The pad offers the queen a site with spare founding deposits: " + (open == null ? null : open.deposits().size()));
        // A tight site: the player's cobblestone on every founding deposit cell beyond the 22 she needs, outermost first.
        var blocked = open.deposits().stream().sorted(Comparator.comparingInt((BlockPos b) -> -side(open, b))).limit(open.deposits().size() - (NestPlan.HARD_CAP - 2)).toList();
        for (var b : blocked) l.setBlock(b, Blocks.COBBLESTONE.defaultBlockState(), 3);
        var tight = site(l, q);
        c.assertTrue(tight != null && tight.entrance().equals(open.entrance()) && tight.direction() == open.direction() && tight.deposits().size() == NestPlan.HARD_CAP - 2,
            "The queen's site stays the same with exactly the 22 founding deposits she needs: " + (tight == null ? null : tight.deposits().size()));
        PrimeAnts.LOGGER.info("T07 P1 TIGHT SITE queen={} entrance={} direction={} deposits={} blocked={}", q.getUUID(), tight.entrance(), tight.direction(), tight.deposits().size(), blocked.size());
        boolean[] full = {false}; List<BlockPos> young = new ArrayList<>(); UUID[] opener = {null};
        c.onEachTick(() -> {
            var p = q.founding().plan(); if (p == null) return;
            for (var b : blocked) c.assertTrue(l.getBlockState(b).is(Blocks.COBBLESTONE), "The player's cobblestone is never covered or removed: " + b);
            c.assertTrue(q.founding().phase() != QueenFounding.Phase.FAILED, "Founding stays valid: " + q.founding().reason());
            if (q.founding().phase() != QueenFounding.Phase.SETTLED) return;
            fx.soil(c, q); // removed = mound + plugs + carried + world + custody, at every tick
            if (!full[0]) {
                c.assertTrue(p.entrance().equals(tight.entrance()) && p.deposits().equals(tight.deposits()) && q.founding().deposited() == NestPlan.HARD_CAP - 2
                    && p.deposits().stream().allMatch(b -> l.getBlockState(b).is(NurseryBlocks.NEST_SOIL) && ColonyTerrain.get(l).mound(l, b, q.getUUID())),
                    "Settled: the queen's 22 units fill every founding deposit cell: deposited=" + q.founding().deposited());
                full[0] = true;
                for (var s : MoundSoil.slots(l, p, q.getUUID(), ColonyStage.YOUNG)) if (s.pos() != null) young.add(s.pos());
                PrimeAnts.LOGGER.info("T07 P1 DEPOSITS FULL queen={} deposits={} youngCells={}", q.getUUID(), p.deposits().size(), young.size());
            }
            if (opener[0] == null) opener[0] = q.founding().workerClaim();
            var onPlan = young.stream().filter(b -> !p.deposits().contains(b) && l.getBlockState(b).is(NurseryBlocks.NEST_SOIL) && ColonyTerrain.get(l).mound(l, b, q.getUUID())).toList();
            var w = opener[0] == null ? null : l.getEntity(opener[0]) instanceof LasiusNigerEntity a ? a : null;
            int placed = w == null ? 0 : w.workerTasks().placed();
            c.assertTrue(onPlan.size() == placed && (w == null || w.workerTasks().placed() <= w.workerTasks().opened()),
                "Each plug unit the opening forager laid is a block of the colony's soil on a Young plan cell beyond the full founding list: onPlan=" + onPlan + " placed=" + placed);
            if (q.founding().lifecycle() != QueenFounding.Lifecycle.OPEN || placed < 2) return;
            c.assertTrue(onPlan.size() == 2 && w.workerTasks().opened() == 2 && p.plugs().stream().allMatch(b -> ColonyPlugs.get(l).opened(l, b, q.getUUID()))
                && !w.getMainHandItem().is(net.minecraft.world.item.Items.DIRT), "The nest is open: both plugs recovered and both units on the stage mound plan: " + onPlan);
            PrimeAnts.LOGGER.info("T07 P1 NEST OPEN queen={} tick={} opener={} plugUnits={} phase={}", q.getUUID(), c.getTick(), w.getUUID(), onPlan, w.workerTasks().phase());
            c.succeed();
        });
    }
}
