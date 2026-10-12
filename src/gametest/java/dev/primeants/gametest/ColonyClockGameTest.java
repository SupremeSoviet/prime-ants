package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.ChamberFunction;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.colony.ColonyDevelopment;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.time.ColonyClock;
import dev.primeants.worker.ColonyMembers;
import java.nio.file.Files;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Real egg-founded nurseries and the production block ticker/save/load path. Detached reconstruction and explicit
 * timestamp fixtures stand in for absence; none of these tests claims actual chunk-unload coverage. */
public final class ColonyClockGameTest {
    private void founded(GameTestHelper c, BiConsumer<LasiusNigerEntity, BroodPile> body) {
        var q = new WorkerForagingGameTest().start(c);
        boolean[] started = {false};
        c.onEachTick(() -> {
            if (started[0]) return;
            var p = new NursingGameTest().pile(c, q);
            if (p == null || p.loadedTicks() < 2) return;
            started[0] = true;
            body.accept(q, p);
        });
    }

    private static CompoundTag save(GameTestHelper c, BroodPile p) {
        return p.saveWithFullMetadata(c.getLevel().registryAccess());
    }
    private static BroodPile detached(GameTestHelper c, BroodPile p, CompoundTag tag) {
        var copy = (BroodPile) BlockEntity.loadStatic(p.getBlockPos(), p.getBlockState(), tag, c.getLevel().registryAccess());
        c.assertTrue(copy != null, "Vanilla block entity reconstruction");
        return copy;
    }
    private static void atTime(GameTestHelper c, long gameTime, Runnable body) {
        var level = c.getLevel(); long original = level.getGameTime();
        var data = (net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();
        try { data.setGameTime(gameTime); body.run(); }
        finally { data.setGameTime(original); }
    }
    private static void interval(GameTestHelper c, BroodPile p, long now, long expected, String label) {
        var before = save(c, p);
        atTime(c, now, () -> {
            for (int i = 0; i < 3; i++) {
                var actual = p.missedIntervalBefore(c.getLevel());
                c.assertTrue(actual.ticks() == expected && actual.beforeGameTime() == now
                    && actual.afterGameTime().equals(p.lastSimulationGameTime()), label + ": expected=" + expected + " actual=" + actual);
            }
            PrimeAnts.LOGGER.info("T01 CLOCK label={} after={} before={} expected={} actual={}", label,
                p.lastSimulationGameTime(), now, expected, p.missedIntervalBefore(c.getLevel()).ticks());
        });
        c.assertTrue(save(c, p).equals(before), "Inspection/serialization never alters nursery state: " + label);
    }
    private static void ordinary(GameTestHelper c, BroodPile p, long now) {
        c.assertTrue(c.getLevel().getBlockEntity(p.getBlockPos()) == p, "Ordinary ticker only runs on installed nursery");
        long loaded = p.loadedTicks(),applied=p.catchup().appliedTicks();
        atTime(c, now, () -> ((dev.primeants.brood.BroodPileBlock)p.getBlockState().getBlock()).getTicker(c.getLevel(), p.getBlockState(), NurseryBlocks.BROOD_TYPE)
            .tick(c.getLevel(), p.getBlockPos(), p.getBlockState(), p));
        c.assertTrue(p.catchup().pendingTicks()>0?p.loadedTicks()==loaded:
            p.loadedTicks() == loaded + 1+p.catchup().appliedTicks()-applied && p.lastSimulationGameTime().orElseThrow() == now,
            "Production completion accounts aggregate spans plus C; pending work retains its unstamped clock");
    }
    private static CompoundTag biology(CompoundTag tag) {
        var copy = tag.copy(); copy.remove(ColonyClock.FIELD); return copy;
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void consecutiveOrdinaryTicksHaveNoMissedInterval(GameTestHelper c) {
        founded(c, (q, p) -> {
            long last = p.lastSimulationGameTime().orElseThrow();
            interval(c, p, last, 0, "same_tick");
            interval(c, p, last + 1, 0, "next_live_tick");
            long loaded = p.loadedTicks();
            c.runAfterDelay(5, () -> {
                c.assertTrue(p.loadedTicks() == loaded + 5 && p.lastSimulationGameTime().orElseThrow() == last + 5,
                    "Five actual live nursery steps and five game ticks, exactly");
                interval(c, p, last + 6, 0, "live_control_after_five_steps"); c.succeed();
            });
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void shortDetachedAbsenceKeepsBroodWhileLiveControlAdvances(GameTestHelper c) {
        founded(c, (q, p) -> {
            var tag = save(c, p); var away = detached(c, p, tag); away.setLevel(c.getLevel());
            long last = away.lastSimulationGameTime().orElseThrow(), loaded = p.loadedTicks(), age = q.elapsedAgeTicks();
            c.runAfterDelay(6, () -> {
                c.assertTrue(p.loadedTicks() == loaded + 6 && q.elapsedAgeTicks() == age + 6,
                    "Ordinary live control and independently loaded queen advance six steps");
                interval(c, away, last + 7, 6, "short_absence_six_missed_ticks");
                c.assertTrue(save(c, away).equals(tag), "Detached brood, identity, nutrition and all loaded clocks stay unchanged");
                var resumed = BroodGameTest.restorePile(c, p, save(c, away));
                interval(c, resumed, last + 7, 6, "short_resumed_boundary");
                ordinary(c, resumed, last + 7);
                interval(c, resumed, last + 8, 0, "short_after_one_ordinary_step"); c.succeed();
            });
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void longTimestampAbsenceResumesOnlyOneOrdinaryStep(GameTestHelper c) {
        founded(c, (q, p) -> {
            var tag = save(c, p); long last = p.lastSimulationGameTime().orElseThrow();
            var control = BroodGameTest.restorePile(c, p, tag);
            ordinary(c, control, last + 1); var controlAfter = biology(save(c, control));
            var resumed = BroodGameTest.restorePile(c, control, tag);
            interval(c, resumed, last + 1_000_001, 1_000_000, "long_absence_one_million");
            ordinary(c, resumed, last + 1_000_001);
            c.assertTrue(!biology(save(c, resumed)).equals(controlAfter)&&resumed.catchup().appliedTicks()==1_000_000
                &&resumed.catchup().pendingTicks()==0&&resumed.original().equals(p.original()),
                "Enabled long reload changes biology and accounts the full interval with original identities");
            interval(c, resumed, last + 1_000_002, 0, "long_after_one_ordinary_step"); c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void diskSerializationPreservesExactIntervalBeforeResumedTick(GameTestHelper c) {
        founded(c, (q, p) -> {
            var tag = save(c, p); long last = p.lastSimulationGameTime().orElseThrow();
            CompoundTag disk;
            try {
                var file = c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("clock-fixtures").resolve(q.getUUID() + ".nbt");
                Files.createDirectories(file.getParent()); NbtIo.writeCompressed(tag, file);
                disk = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                c.assertTrue(disk.equals(tag), "Actual nursery NBT disk round trip has identical payload");
            } catch (java.io.IOException e) { throw new IllegalStateException("Nursery disk round trip", e); }
            var away = detached(c, p, disk);
            c.runAfterDelay(9, () -> {
                var resumed = BroodGameTest.restorePile(c, p, save(c, away));
                c.assertTrue(save(c, resumed).equals(disk), "Normal reconstruction retains every saved nursery field");
                interval(c, resumed, last + 10, 9, "disk_serialization_nine_missed_ticks");
                ordinary(c, resumed, last + 10);
                interval(c, resumed, last + 11, 0, "disk_after_resumed_ordinary_step"); c.succeed();
            });
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void legacyBaselineStartsAtLevelAccessBeforeOperationalAndBeforeStep(GameTestHelper c) {
        founded(c, (q, p) -> {
            var legacy = save(c, p); legacy.remove(ColonyClock.FIELD);
            c.assertTrue(!p.operational(), "Real first-clutch nursery is not yet operational");
            var loaded = detached(c, p, legacy);
            c.assertTrue(loaded.lastSimulationGameTime().isEmpty(), "Detached legacy object cannot invent a world timestamp");
            atTime(c, 100, () -> loaded.setLevel(c.getLevel()));
            c.assertTrue(loaded.lastSimulationGameTime().orElseThrow() == 100 && !loaded.operational()
                && biology(save(c, loaded)).equals(legacy), "Migration at first level access preserves biology before nest readiness");
            interval(c, loaded, 100, 0, "legacy_at_load");
            interval(c, loaded, 108, 7, "legacy_delay_before_first_step");
            var tag = save(c, loaded); c.assertTrue(tag.getLong(ColonyClock.FIELD).orElseThrow() == 100, "Migration itself persists");
            var again = detached(c, p, tag); atTime(c, 108, () -> again.setLevel(c.getLevel()));
            interval(c, again, 108, 7, "legacy_repeat_reload_does_not_renew_baseline");
            // Other vanilla ordering: level is assigned before the legacy tag is loaded.
            var levelFirst = new BroodPile(p.getBlockPos(), p.getBlockState());
            atTime(c, 200, () -> {
                levelFirst.setLevel(c.getLevel()); levelFirst.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, c.getLevel().registryAccess(), legacy));
            });
            interval(c, levelFirst, 208, 7, "legacy_level_before_tag_delay");
            var resumed = BroodGameTest.restorePile(c, p, tag); ordinary(c, resumed, 108);
            interval(c, resumed, 109, 0, "legacy_after_first_ordinary_step"); c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void repeatedInspectionAndSameTimeReloadNeverFabricateTime(GameTestHelper c) {
        founded(c, (q, p) -> {
            var tag = save(c, p); long last = p.lastSimulationGameTime().orElseThrow();
            var copy = p;
            for (int i = 0; i < 20; i++) {
                copy = BroodGameTest.restorePile(c, copy, save(c, copy));
                interval(c, copy, last + 41, 40, "same_time_reload_" + i);
                c.assertTrue(save(c, copy).equals(tag), "Twenty inspections/save/reloads preserve exact canonical payload");
            }
            ordinary(c, copy, last + 41);
            interval(c, copy, last + 42, 0, "repeated_reload_after_ordinary_step"); c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void daylightBackwardAndArithmeticBoundariesDoNotInventAbsence(GameTestHelper c) {
        founded(c, (q, p) -> {
            long last = p.lastSimulationGameTime().orElseThrow(), day = c.getLevel().getOverworldClockTime();
            var daylight = c.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
            try {
                c.getLevel().clockManager().setTotalTicks(daylight, day + 240_000);
                interval(c, p, last + 1, 0, "daylight_change_only");
                c.getLevel().clockManager().setTotalTicks(daylight, 0);
                interval(c, p, last + 1, 0, "daylight_backward_only");
            } finally { c.getLevel().clockManager().setTotalTicks(daylight, day); }
            interval(c, p, last - 1, 0, "game_time_backward");
            interval(c, p, Long.MIN_VALUE, 0, "negative_game_time_boundary");
            var tag = save(c, p); tag.putLong(ColonyClock.FIELD, 0);
            var zero = detached(c, p, tag);
            interval(c, zero, Long.MAX_VALUE, Long.MAX_VALUE - 1, "nonnegative_long_max_exact");
            tag.putLong(ColonyClock.FIELD, Long.MAX_VALUE); var future = detached(c, p, tag);
            interval(c, future, 0, 0, "future_saved_timestamp");
            interval(c, future, Long.MAX_VALUE, 0, "equal_long_max");
            tag.putLong(ColonyClock.FIELD, Long.MIN_VALUE); var invalid = detached(c, p, tag);
            atTime(c, 50, () -> invalid.setLevel(c.getLevel()));
            interval(c, invalid, 51, 0, "invalid_negative_payload_rebaselines_on_load");
            ordinary(c, p, 10); interval(c, p, 11, 0, "backward_completed_step_rebases"); c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void partialLoadingObservationPreservesUnknownAndAllLifecycleState(GameTestHelper c) {
        founded(c, (q, p) -> {
            var plan = q.founding().plan();
            // The closed first-clutch nursery checks enclosure corners; its entrance route is checked only once open.
            try (var unavailable = UnavailableCells.hide(c, plan.at(2, -2, -3))) {
                // UNKNOWN is evaluated by its existing owner; clock observation must not call it.
                var evaluation = ColonyDevelopment.evaluate(c.getLevel(), q.getUUID(), plan, p.operational(), p.adultCapacity(), p.loadedTicks());
                var nursery = evaluation.chambers().getFirst().functions().get(ChamberFunction.NURSERY);
                c.assertTrue(nursery == ColonyDevelopment.Presence.UNKNOWN, "Unavailable enclosure corner retains real UNKNOWN semantics: " + evaluation);
                var registry = ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE, ChamberRegistry.get(c.getLevel())).getOrThrow();
                var members = ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE, ColonyMembers.get(c.getLevel())).getOrThrow();
                var tag = save(c, p); long reserve = q.bodyReserve(), age = q.elapsedAgeTicks();
                var supply = p.supply(c.getLevel());
                interval(c, p, p.lastSimulationGameTime().orElseThrow() + 24_001, 24_000, "partial_loading_observation");
                c.assertTrue(save(c, p).equals(tag) && q.bodyReserve() == reserve && q.elapsedAgeTicks() == age
                    && p.supply(c.getLevel()).equals(supply)
                    && registry.equals(ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE, ChamberRegistry.get(c.getLevel())).getOrThrow())
                    && members.equals(ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE, ColonyMembers.get(c.getLevel())).getOrThrow()),
                    "Clock observation preserves brood, inventories/nutrition, identities, reservations and stage clocks");
                c.assertTrue(ColonyDevelopment.presence(c.getLevel(), q.getUUID(), plan, ChamberRegistry.FOUNDING, ChamberFunction.NURSERY)
                    == ColonyDevelopment.Presence.UNKNOWN, "Reading the clock never resolves UNKNOWN");
            }
            c.succeed();
        });
    }

    @GameTest(maxTicks=20, structure="prime_ants_test:idle_ground")
    public void unownedOrdinaryTickerDoesNotStampOrEstablishClock(GameTestHelper c) {
        c.setBlock(2, 5, 2, NurseryBlocks.BROOD_PILE);
        var pos = c.absolutePos(new net.minecraft.core.BlockPos(2, 5, 2));
        var p = (BroodPile)c.getLevel().getBlockEntity(pos); var tag = save(c, p);
        ((dev.primeants.brood.BroodPileBlock)p.getBlockState().getBlock()).getTicker(c.getLevel(), p.getBlockState(), NurseryBlocks.BROOD_TYPE)
            .tick(c.getLevel(), pos, p.getBlockState(), p);
        c.assertTrue(p.lastSimulationGameTime().isEmpty() && p.loadedTicks() == 0 && save(c, p).equals(tag),
            "Ownership guard returns without completing a simulation step or stamping a clock");
        interval(c, p, Long.MAX_VALUE, 0, "unowned_no_boundary"); c.succeed();
    }
}
