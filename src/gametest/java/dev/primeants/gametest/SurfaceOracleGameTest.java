package dev.primeants.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import dev.primeants.brood.NurseryBlocks;

/** Negative coverage is separate from the six construction/cohort identities. */
public final class SurfaceOracleGameTest {
    @GameTest(maxTicks=20, structure="prime_ants_test:surface_ground")
    public void offFootprintPlayerAndUnevidencedModEditsAreRejected(GameTestHelper c) {
        var fixture = new SurfaceFixture(); var q = fixture.habitat(c, false); fixture.snapshot(c, q);
        var at = q.founding().plan().at(16, 12, 1);
        c.assertTrue(fixture.protectedCells.containsKey(at), "The negative cell is outside every authorized structural/spoil component and inside the original snapshot");
        c.getLevel().setBlock(at, Blocks.GLASS.defaultBlockState(), 3);
        c.assertTrue(at.equals(fixture.protectionProblem(c, q)), "Unrelated actual off-footprint player edit remains rejected");
        c.getLevel().setBlock(at, NurseryBlocks.PACKED_CLAY.defaultBlockState(), 3);
        dev.primeants.founding.ColonyTerrain.get(c.getLevel()).built(at, q.getUUID(), NurseryBlocks.PACKED_CLAY);
        c.assertTrue(at.equals(fixture.protectionProblem(c, q)), "Mod ownership without exact chamber work/payment evidence is insufficient");
        dev.primeants.PrimeAnts.LOGGER.info("T14 ORACLE NEGATIVE queen={} cell={} playerEditRejected=true arbitraryModEditRejected=true", q.getUUID(), at);
        c.succeed();
    }
}
