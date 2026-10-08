package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.brood.BroodPile;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** The T04 nest-plan fixture, used by its scan, hall and hauling tests. It is the shared 14x14 pad and egg
 * (QueenFoundingGameTest), T03's six witnessed dirt columns eastward (the store's right placement), and one more
 * witnessed row at z = 0, which holds the queen's hall's outer wall and a fortieth mound column so the mound can take
 * the hall's soil after founding, widening and store (24 + 12 + 24 + 12 of 80 cells). Shared helpers are unchanged. */
final class NestPlanFixture {
    final WorkerForagingGameTest f = new WorkerForagingGameTest();
    final NursingGameTest food = new NursingGameTest();
    LasiusNigerEntity start(GameTestHelper c) {
        var founding = new QueenFoundingGameTest(); founding.terrain(c, Blocks.DIRT.defaultBlockState(), true, true);
        var l = c.getLevel(); var records = NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE, NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for (int x = 1; x <= 20; x++) for (int z = 0; z <= 14; z++) for (int y = 1; y <= 4; y++) {
            if (x <= 14 && z >= 1) continue; // the shared pad
            c.setBlock(x, y, z, Blocks.DIRT); records.addProperty(Long.toString(c.absolutePos(new BlockPos(x, y, z)).asLong()), "minecraft:dirt");
        }
        l.getDataStorage().set(NaturalSoil.TYPE, NaturalSoil.CODEC.parse(JsonOps.INSTANCE, records).getOrThrow());
        return founding.egg(c);
    }
    /** T03's growth supply once the nest opens: twelve apples and ten chickens dropped behind the entrance. */
    void grow(GameTestHelper c, LasiusNigerEntity q, boolean[] supplied) {
        if (!supplied[0] && q.founding().lifecycle() == QueenFounding.Lifecycle.OPEN) { supplied[0] = true; food.supply(c, q, 12, 10); }
    }
    static ChamberExcavation.Job job(GameTestHelper c, LasiusNigerEntity q, String room) { return ChamberExcavation.get(c.getLevel()).job(q.getUUID(), room); }
    static ColonyDevelopment.ChamberState chamber(ColonyDevelopment.Evaluation e, String id) { return e == null ? null : e.chambers().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null); }
    static ColonyDevelopment.Presence presence(ColonyDevelopment.Evaluation e, String id, ChamberFunction f) { var s = chamber(e, id); return s == null ? null : s.functions().get(f); }
    static boolean foundingConfirmed(ColonyDevelopment.Evaluation e) {
        var s = chamber(e, ChamberRegistry.FOUNDING);
        return s != null && s.problem() == null && s.functions().equals(Map.of(ChamberFunction.NURSERY, ColonyDevelopment.Presence.CONFIRMED, ChamberFunction.FOOD_STORE, ColonyDevelopment.Presence.CONFIRMED));
    }
    /** The store chamber is confirmed by the nursery's own latest evaluation. */
    static boolean storeConfirmed(ColonyDevelopment.Evaluation e) { return presence(e, ChamberExcavation.STORE, ChamberFunction.MATERIAL_STORE) == ColonyDevelopment.Presence.CONFIRMED; }
    static MaterialStore store(GameTestHelper c, LasiusNigerEntity q) { var j = job(c, q, ChamberExcavation.STORE); return j != null && c.getLevel().getBlockEntity(j.built.marker()) instanceof MaterialStore s ? s : null; }
    long carried(GameTestHelper c, LasiusNigerEntity q, UUID worker) {
        return f.workers(c, q).stream().filter(w -> w.getUUID().equals(worker) && w.getMainHandItem().is(Items.DIRT)).mapToInt(w -> w.getMainHandItem().getCount()).sum();
    }
    /** Exact soil accounting at every tick: each unit the queen, the widening, the store and the hall removed is a mound
     * block (the 0.1.0 deposits or the colony's stage mound, MoundSoil.cells), a plug, carried, a dropped item or in
     * transfer custody; and each nest-plan job's removed = carried + deposited + released. */
    void soil(GameTestHelper c, LasiusNigerEntity q) {
        var l = c.getLevel(); var p = q.founding().plan(); if (p == null || q.founding().phase() != QueenFounding.Phase.SETTLED) return;
        var widening = NestExpansion.get(l).job(q.getUUID());
        long mound = MoundSoil.cells(l, p, q.getUUID()).stream().filter(b -> l.getBlockState(b).is(NurseryBlocks.NEST_SOIL)).count();
        long plugs = p.plugs().stream().filter(b -> ColonyPlugs.material(l.getBlockState(b))).count();
        long held = f.workers(c, q).stream().filter(w -> w.getMainHandItem().is(Items.DIRT)).mapToInt(w -> w.getMainHandItem().getCount()).sum();
        long world = l.getEntitiesOfClass(ItemEntity.class, c.getBounds().inflate(8), i -> i.isAlive() && i.getItem().is(Items.DIRT)).stream().mapToInt(i -> i.getItem().getCount()).sum();
        long custody = TransferCustody.get(l).contents().stream().filter(t -> t.stack().is(Items.DIRT) && c.getBounds().inflate(8).contains(t.position())).mapToInt(t -> t.stack().getCount()).sum();
        long removed = 24 + (widening == null ? 0 : widening.removed());
        var mining=Mining.get(l).job(q.getUUID());
        if(mining!=null)removed+=mining.produced("minecraft:dirt");
        for (var j : ChamberExcavation.get(l).jobs(q.getUUID())) {
            removed += j.removed();
            c.assertTrue(j.removed() == j.deposited + j.released + (j.claim == null ? 0 : carried(c, q, j.claim)), "Job " + j.room + " removed = carried + deposited + released: " + j.removed() + " " + j.deposited + " " + j.released + " claim=" + j.claim);
        }
        c.assertTrue(removed == mound + plugs + held + world + custody, "Founding, widening, store and hall soil are all physical: removed=" + removed + " mound=" + mound + " plugs=" + plugs + " held=" + held + " world=" + world + " custody=" + custody);
    }
    /** Material units the test dropped, by item, against where each one is now: on the ground, in a worker's mandibles,
     * in the colony's store or in transfer custody. Fixture drops never despawn (unlimited lifetime, as the food
     * fixtures), so nothing expires by vanilla despawn and the expired term is zero. */
    record Ledger(long ground, long carried, long stored, long custody) {
        long total() { return ground + carried + stored + custody; }
        @Override public String toString() { return "ground=" + ground + " carried=" + carried + " stored=" + stored + " custody=" + custody; }
    }
    Ledger ledger(GameTestHelper c, LasiusNigerEntity q, net.minecraft.world.item.Item item) {
        var l = c.getLevel(); var box = c.getBounds().inflate(8); var s = store(c, q);
        long ground = l.getEntitiesOfClass(ItemEntity.class, box, i -> i.isAlive() && i.getItem().is(item)).stream().mapToInt(i -> i.getItem().getCount()).sum();
        long carried = f.workers(c, q).stream().filter(w -> w.getMainHandItem().is(item)).mapToInt(w -> w.getMainHandItem().getCount()).sum();
        long stored = s == null ? 0 : s.contents().stream().filter(st -> st.is(item)).count();
        long custody = TransferCustody.get(l).contents().stream().filter(t -> t.stack().is(item) && box.contains(t.position())).mapToInt(t -> t.stack().getCount()).sum();
        return new Ledger(ground, carried, stored, custody);
    }
    void accounted(GameTestHelper c, LasiusNigerEntity q, Map<net.minecraft.world.item.Item, Integer> dropped) {
        dropped.forEach((item, n) -> { var ledger = ledger(c, q, item); c.assertTrue(ledger.total() == n, "Dropped " + n + " " + item + " = ground + carried + stored + custody + 0 expired: " + ledger); });
    }
    ItemEntity drop(GameTestHelper c, BlockPos at, ItemStack stack) { return f.drop(c, at, stack); }
    static BroodPile pile(GameTestHelper c, LasiusNigerEntity q) { return new NursingGameTest().pile(c, q); }
}
