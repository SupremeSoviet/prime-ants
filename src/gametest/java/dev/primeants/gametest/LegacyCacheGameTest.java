package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodStage;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.QueenFounding;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stage-1 T07 (l1): a food cache saved before the T06 shares, full of six chickens, in a colony that has no protein
 * taker, never keeps apples out. The first apple a forager brings makes the cache shed the two chickens beyond the
 * protein share through transfer custody onto the chamber floor beside it; the apple gets in, the adults are fed, and
 * every food unit stays in the world or is eaten. Production founding, a forager, nurses and dropped food; the only
 * fixture is the saved cache, which can only be obtained by loading one (as LifecycleGameTest's carriers do). */
public final class LegacyCacheGameTest {
    private final WorkerForagingGameTest f = new WorkerForagingGameTest();
    private final NursingGameTest food = new NursingGameTest();
    static long count(NestCache n, net.minecraft.world.item.Item item) { return n == null ? 0 : n.contents().stream().filter(s -> s.is(item)).count(); }

    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void legacySixChickenCacheWithNoProteinTakerLetsApplesInAndEveryUnitIsConserved(GameTestHelper c) {
        var q = f.start(c); int[] step = {0}; long[] base = {0}, before = {0, 0}, meals = {0}, shedAt = {-1};
        c.onEachTick(() -> {
            var l = c.getLevel(); var p = q.founding().plan(); if (p == null || step[0] == 4) return;
            var cache = f.cache(c, q); var pile = food.pile(c, q);
            if (step[0] == 0) {
                // The queen's own protein need first: one chicken, which a nurse feeds her (EGG_PROTEIN); then no taker is left.
                if (q.founding().lifecycle() != QueenFounding.Lifecycle.OPEN) return;
                food.supply(c, q, 0, 1); step[0] = 1; return;
            }
            if (step[0] == 1) {
                boolean carried = f.workers(c, q).stream().anyMatch(w -> WorkerTasks.food(w.getMainHandItem()));
                long ground = l.getEntitiesOfClass(ItemEntity.class, c.getBounds().inflate(8), i -> i.isAlive() && WorkerTasks.food(i.getItem())).size();
                if (cache == null || cache.size() > 0 || carried || ground > 0 || q.nutrition().protein() < Nutrition.EGG_PROTEIN) return;
                // No protein taker: the queen holds an egg's protein and no larva takes protein.
                c.assertTrue(pile == null || pile.records().stream().noneMatch(r -> !r.founding() && r.stage() == BroodStage.LARVA), "No larva takes protein");
                // A save from before the shares: the colony's own cache, full of six chickens.
                var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, l.registryAccess());
                out.putString("Colony", q.getUUID().toString()); out.store("Entrance", net.minecraft.core.BlockPos.CODEC, p.entrance()); out.putString("Direction", p.direction().getName());
                out.store("Contents", ItemStack.CODEC.listOf(), java.util.stream.IntStream.range(0, 6).mapToObj(i -> new ItemStack(Items.CHICKEN)).toList());
                cache.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, l.registryAccess(), out.buildResult())); cache.setChanged();
                c.assertTrue(cache.size() == 6 && count(cache, Items.CHICKEN) == 6 && !cache.admits(new ItemStack(Items.APPLE)), "The loaded cache holds six chickens and admission alone takes no apple");
                food.supply(c, q, 4, 0); base[0] = food.total(c, q); before[1] = adultApples(c, q); step[0] = 2;
                PrimeAnts.LOGGER.info("T07 L1 LEGACY CACHE LOADED queen={} tick={} queenProtein={} total={}", q.getUUID(), c.getTick(), q.nutrition().protein(), base[0]);
                return;
            }
            // Every unit: the one chicken the queen ate, the six loaded and the four apples dropped, on the ground, carried, in
            // the cache, in custody or eaten.
            c.assertTrue(food.total(c, q) == base[0], "Every food unit is on the ground, carried, cached, in custody or eaten: " + food.total(c, q) + " of " + base[0]);
            c.assertTrue(cache != null && count(cache, Items.CHICKEN) <= 6, "The cache stays");
            long apples = count(cache, Items.APPLE), chickens = count(cache, Items.CHICKEN);
            if (step[0] == 2) {
                // An apple in the cache, in a nurse's mandibles from it, or eaten by an adult since the load.
                long admitted = apples + f.workers(c, q).stream().filter(w -> w.workerTasks().nursing() && w.getMainHandItem().is(Items.APPLE)).count() + adultApples(c, q) - before[1];
                if (chickens == 6 && admitted == 0) {
                    // Until the first apple arrives no taker frees a slot: the six chickens stay in the cache.
                    c.assertTrue(q.nutrition().protein() >= Nutrition.EGG_PROTEIN && apples == 0, "No protein taker: six chickens until the first apple: " + cache.contents());
                    before[0] = chickens; return;
                }
                var shed = TransferCustody.get(l).contents().stream().filter(t -> t.source().startsWith("cache-share:") && t.stack().is(Items.CHICKEN)).count();
                long floor = l.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(p.at(4, -1, -2)).inflate(1.5), i -> i.isAlive() && i.getItem().is(Items.CHICKEN)).stream().mapToInt(i -> i.getItem().getCount()).sum();
                c.assertTrue(before[0] == 6 && admitted == 1 && chickens == 4 && shed + floor == 2,
                    "The first apple made the six-chicken cache shed the two chickens beyond its share beside it, and got in: " + cache.contents() + " admitted=" + admitted + " custody=" + shed + " floor=" + floor);
                shedAt[0] = c.getTick(); meals[0] = before[1]; step[0] = 3;
                PrimeAnts.LOGGER.info("T07 L1 SHED AND APPLE IN queen={} tick={} contents={} custody={} floor={}", q.getUUID(), c.getTick(), cache.contents(), shed, floor);
                return;
            }
            // Adults are fed: apples from the cache are eaten by adults, and no adult reaches its fasting grace.
            for (var a : f.workers(c, q)) c.assertTrue(a.adultLife().fasting() < a.adultLife().grace(), "No adult starves: " + a.getUUID());
            c.assertTrue(chickens <= FoodShares.share(NestCache.CAPACITY), "The cache keeps within the protein share once shed: " + cache.contents());
            if (adultApples(c, q) - meals[0] < 2) return;
            PrimeAnts.LOGGER.info("T07 L1 ADULTS FED queen={} tick={} shedAt={} appleMeals={} queenApples={} contents={}", q.getUUID(), c.getTick(), shedAt[0], adultApples(c, q) - meals[0], q.nutrition().apples(), cache.contents());
            step[0] = 4; c.succeed();
        });
    }
    /** Apples adults have eaten: the queen's and every worker's receipts, dead workers' included. */
    static long adultApples(GameTestHelper c, LasiusNigerEntity q) {
        long total = q.nutrition().apples();
        for (var e : c.getLevel().getAllEntities()) if (e instanceof LasiusNigerEntity w && w != q && w.isAlive() && q.getUUID().equals(w.queenId())) total += w.nutrition().apples();
        for (var entry : AdultHistory.get(c.getLevel()).records().entrySet()) {
            var row = com.google.gson.JsonParser.parseString(entry.getValue()).getAsJsonObject();
            if (!entry.getKey().equals(q.getUUID().toString()) && row.get("queen").getAsString().equals(q.getUUID().toString())) total += row.getAsJsonObject("nutrition").has("apples") ? row.getAsJsonObject("nutrition").get("apples").getAsLong() : 0;
        }
        return total;
    }
}
