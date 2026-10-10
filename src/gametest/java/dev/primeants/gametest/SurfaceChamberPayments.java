package dev.primeants.gametest;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/** Scoped observation of the original physical upgrade; it never grants production permission. */
public final class SurfaceChamberPayments {
    private static final Map<UUID, SurfaceChamberPayments> active = new HashMap<>();
    record Payment(BlockState before, BlockState after, UUID worker, String chamber, int taken, int built) {}
    public record Before(SurfaceChamberPayments scope, LasiusNigerEntity worker, ChamberUpgrade.Job job,
                         BlockPos cell, BlockState state, int cargo, int built, int taken, int released) {}
    private final Map<BlockPos, Payment> payments = new HashMap<>();
    private final Set<BlockPos> accepted = new HashSet<>();
    static SurfaceChamberPayments watch(net.minecraft.gametest.framework.GameTestHelper c, UUID owner) {
        var scope = new SurfaceChamberPayments(); active.put(owner, scope);
        ((dev.primeants.gametest.mixin.GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new net.minecraft.gametest.framework.GameTestListener() {
            public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo i) {}
            public void testPassed(net.minecraft.gametest.framework.GameTestInfo i, net.minecraft.gametest.framework.GameTestRunner r) { active.remove(owner, scope); }
            public void testFailed(net.minecraft.gametest.framework.GameTestInfo i, net.minecraft.gametest.framework.GameTestRunner r) { active.remove(owner, scope); }
            public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo i, net.minecraft.gametest.framework.GameTestInfo copy, net.minecraft.gametest.framework.GameTestRunner r) { active.remove(owner, scope); }
        });
        return scope;
    }
    public static Before before(LasiusNigerEntity w) {
        var scope = active.get(w.queenId()); if (scope == null || !(w.level() instanceof ServerLevel l)) return null;
        var j = ChamberUpgrade.get(l).claimedBy(w);
        if (j == null || j.complete() || j.stopped() || j.tier != 2 || !j.ledger().exact()
                || !w.getMainHandItem().is(Items.CLAY_BALL) || j.carried() != NestWalls.CLAY_PER_CELL) return null;
        var colony = ChamberRegistry.get(l).colony(w.queenId());
        if (colony == null || colony.chambers().stream().noneMatch(ch -> ch.id().equals(j.chamber)
                && ChamberUpgrade.walls(j.home, ch).contains(j.next())) || !j.convertible(l, j.next(), w.queenId())) return null;
        return new Before(scope, w, j, j.next(), l.getBlockState(j.next()), w.getMainHandItem().getCount(), j.built().size(), j.taken(), j.released());
    }
    public static void after(Before b) {
        if (b == null) return; var w = b.worker(); var l = (ServerLevel)w.level(); var j = b.job();
        int cargo = w.getMainHandItem().is(Items.CLAY_BALL) ? w.getMainHandItem().getCount() : 0;
        var after = l.getBlockState(b.cell());
        if (j.built().size() != b.built()+1 || !j.built().getLast().equals(b.cell()) || j.taken() != b.taken()
                || j.released() != b.released() || !j.ledger().exact() || b.cargo()-cargo != NestWalls.CLAY_PER_CELL
                || !after.equals(NurseryBlocks.PACKED_CLAY.defaultBlockState())
                || !ColonyTerrain.get(l).built(l, b.cell(), w.queenId(), NurseryBlocks.PACKED_CLAY)) return;
        b.scope().payments.put(b.cell(), new Payment(b.state(), after, w.getUUID(), j.chamber, j.taken(), j.built().size()));
        dev.primeants.PrimeAnts.LOGGER.info("T14 CHAMBER PAYMENT queen={} worker={} chamber={} cell={} before={} after={} taken={} built={} consumed=1", w.queenId(), w.getUUID(), j.chamber, b.cell(), b.state(), after, j.taken(), j.built().size());
    }
    boolean permits(ServerLevel l, UUID owner, BlockPos at, BlockState before, BlockState after) {
        var p = payments.get(at);
        boolean result = p != null && p.before().equals(before) && p.after().equals(after)
                && ColonyTerrain.get(l).built(l, at, owner, NurseryBlocks.PACKED_CLAY);
        if(result&&accepted.add(at))dev.primeants.PrimeAnts.LOGGER.info("T14 ORACLE ACCEPTED queen={} worker={} chamber={} cell={} before={} after={} taken={} built={} snapshotUnchanged=true",owner,p.worker(),p.chamber(),at,before,after,p.taken(),p.built());
        return result;
    }
}
