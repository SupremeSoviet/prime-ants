package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.NestPlan;
import dev.primeants.time.*;
import dev.primeants.worker.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;

/** Timestamp and detached/removal fixtures explicitly differ from ReconciliationChunkGameTest's native eviction. */
public final class ReconciliationGameTest {
    private final WorkerForagingGameTest f = new WorkerForagingGameTest();
    private void founded(GameTestHelper c, boolean workers, BiConsumer<LasiusNigerEntity, BroodPile> body) {
        var queen = f.start(c); boolean[] done = {false};
        c.onEachTick(() -> {
            if (done[0]) return;
            var pile = new NursingGameTest().pile(c, queen);
            if (pile == null || pile.loadedTicks() < 2 || workers && f.workers(c, queen).size() != 3) return;
            done[0] = true; body.accept(queen, pile);
        });
    }
    private void nursingColony(GameTestHelper c, BiConsumer<LasiusNigerEntity, BroodPile> body) {
        var queen=f.start(c);boolean[] done={false};
        c.onEachTick(()->{
            if(done[0])return;var pile=new NursingGameTest().pile(c,queen);
            if(pile==null||!pile.operational()||f.workers(c,queen).stream().filter(w->!w.isCallow()&&w.workerTasks().nursing()).count()<2)return;
            done[0]=true;body.accept(queen,pile);
        });
    }
    static CompoundTag save(GameTestHelper c, Entity entity) {
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, c.getLevel().registryAccess());
        c.assertTrue(entity.save(out), "Normal live entity serialization"); return out.buildResult();
    }
    static LasiusNigerEntity load(GameTestHelper c, CompoundTag tag) {
        var ant = (LasiusNigerEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,
            c.getLevel().registryAccess(), tag), c.getLevel(), EntitySpawnReason.LOAD, e -> e);
        c.assertTrue(ant != null, "Normal entity reconstruction"); return ant;
    }
    static void atTime(GameTestHelper c, long now, Runnable body) {
        var data = (ServerLevelData)c.getLevel().getLevelData(); long old = c.getLevel().getGameTime();
        try { data.setGameTime(now); body.run(); } finally { data.setGameTime(old); }
    }
    static void ordinary(GameTestHelper c, BroodPile pile, long now) {
        long ticks = pile.loadedTicks();
        atTime(c, now, () -> ((BroodPileBlock)pile.getBlockState().getBlock()).getTicker(c.getLevel(), pile.getBlockState(), NurseryBlocks.BROOD_TYPE)
            .tick(c.getLevel(), pile.getBlockPos(), pile.getBlockState(), pile));
        c.assertTrue(pile.loadedTicks() == ticks + 1 && pile.lastSimulationGameTime().orElseThrow() == now, "Exactly one ordinary nursery step");
    }
    static ColonyClock.Interval interval(long after, long before) {
        return new ColonyClock.Interval(OptionalLong.of(after), before, ColonyClock.missedTicks(after, before));
    }
    static ReloadObservation.Member member(ReloadObservation observation, UUID id) {
        return observation.members().stream().filter(m -> m.adult().equals(id)).findFirst().orElseThrow();
    }
    static void resolved(GameTestHelper c, String label, ReloadObservation.Member row, long simulated, long missed) {
        c.assertTrue(row.availability().equals("OBSERVED") && row.coverage().status().equals("RESOLVED")
            && row.coverage().alreadySimulated().orElseThrow() == simulated && row.coverage().missed().orElseThrow() == missed,
            label + " exact ticks: " + row);
        PrimeAnts.LOGGER.info("T02 RECON label={} row={}", label, row);
        var out=ReconciliationEvidence.member(row);out.addProperty("expected_already_simulated",simulated);out.addProperty("expected_missed",missed);
        ReconciliationEvidence.log(label,"detached_or_timestamp",out);
    }
    static CompoundTag biology(CompoundTag tag) { var copy = tag.copy(); copy.remove(AdultSimulation.FIELD); return copy; }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void nurserySnapshotExcludesIndependentlyTickingQueenAndWorkers(GameTestHelper c) {
        founded(c, true, (q, p) -> {
            var nurseryTag = p.saveWithFullMetadata(c.getLevel().registryAccess());
            long last = p.lastSimulationGameTime().orElseThrow(), queenAge = q.elapsedAgeTicks();
            var workers = f.workers(c, q); var ages = workers.stream().map(LasiusNigerEntity::elapsedAgeTicks).toList();
            c.runAfterDelay(6, () -> {
                c.assertTrue(q.elapsedAgeTicks() == queenAge + 6, "Independent queen actually completed six live steps");
                for (int i=0;i<workers.size();i++) c.assertTrue(workers.get(i).elapsedAgeTicks() == ages.get(i) + 6, "Independent worker six live steps");
                var resumed = BroodGameTest.restorePile(c, p, nurseryTag);
                var beforeQueen = save(c, q); var beforeWorkers = workers.stream().map(w -> save(c, w)).toList();
                ordinary(c, resumed, last + 7);
                var captured = resumed.reloadObservation();
                ReconciliationEvidence.log("live_capture_after_stamp","detached_nursery_with_live_adults",ReconciliationEvidence.observation(captured));
                c.assertTrue(captured.interval().equals(interval(last, last + 7)) && captured.queen().equals(q.getUUID()), "Original exclusive interval survives nursery stamp C");
                resolved(c, "live_queen_six", member(captured, q.getUUID()), 6, 0);
                for (var worker : workers) resolved(c, "live_worker_six", member(captured, worker.getUUID()), 6, 0);
                for (var row : captured.members()) c.assertTrue(row.ownInterval().ticks() == 0 && row.returnBoundary() == null,
                    "Independently ticking captured adult has no return gap; no stale boundary becomes owed work");
                c.assertTrue(beforeQueen.equals(save(c, q)) && beforeWorkers.equals(workers.stream().map(w -> save(c, w)).toList()), "Nursery capture does not simulate adults or alter cargo/health/nutrition/policies");
                ordinary(c, resumed, last + 8);
                c.assertTrue(resumed.reloadObservation() == captured, "Immutable observation retained through consecutive ordinary stamp; no dormant queue");
                c.succeed();
            });
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void distinctAdultAbsenceAndBothCurrentTickOrderingsStayExact(GameTestHelper c) {
        founded(c, false, (q, p) -> {
            long nurseryLast = p.lastSimulationGameTime().orElseThrow();
            var nurseryTag = p.saveWithFullMetadata(c.getLevel().registryAccess());
            long adultLast = nurseryLast + 2, current = nurseryLast + 7;
            // Timestamp fixture: actual production adult tick on a detached, normally reconstructed entity.
            var tag = save(c, q); var detached = load(c, tag);
            atTime(c, adultLast, detached::tick); var adultTag = save(c, detached);
            for (boolean adultFirst : new boolean[]{false, true}) {
                var adult = load(c, adultTag); q.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(adult), "Same queen identity installed for order fixture");
                var resumed = BroodGameTest.restorePile(c, p, nurseryTag);
                if (adultFirst) atTime(c, current, adult::tick);
                ordinary(c, resumed, current);
                var captured = resumed.reloadObservation(); var row = member(captured, adult.getUUID());
                ReconciliationEvidence.log("current_order_" + adultFirst,"timestamp_and_removal",ReconciliationEvidence.observation(captured));
                resolved(c, "distinct_" + (adultFirst ? "adult_first" : "nursery_first"), row, 1, 5);
                c.assertTrue(row.ownInterval().equals(interval(adultLast,current)) && row.returnBoundary()!=null
                    && row.returnBoundary().ownInterval().equals(interval(adultLast,current))
                    && row.returnBoundary().evidence().latest()==adultLast, "Captured member retains original four-tick adult return in both orders");
                c.assertTrue(row.currentTimestampInterval().equals(interval(adultFirst?current:adultLast,current)),
                    "Current timestamp query explicitly differs from original return after adult-first C stamp");
                c.assertTrue(captured.interval().ticks() == 6 && row.evidence().runs().stream().anyMatch(r -> r.last() >= adultLast), "Nursery six ticks differs from adult four-tick own gap");
                if (!adultFirst) atTime(c, current, adult::tick);
                c.assertTrue(adult.lastSimulationBoundary().ownInterval().equals(interval(adultLast, current)), "Adult returning boundary remains inspectable after its stamp");
                c.assertTrue(adult.simulationEvidence().latest() == current && captured.interval().equals(interval(nurseryLast, current)), "Both current-step orderings count C only in ordinary biology");
                c.assertTrue(adult.elapsedAgeTicks() == detached.elapsedAgeTicks() + 1, "Only one current adult step in each ordering");
                adult.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK); p = resumed;
            }
            c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void capturedReturnBeforeNurseryAndEarlierResumptionNeverReuseStaleBoundary(GameTestHelper c) {
        founded(c, false, (q, p) -> {
            long last=p.lastSimulationGameTime().orElseThrow(), adultLast=last-2, current=last+7;
            var tag=save(c,q);var detached=load(c,tag);atTime(c,adultLast,detached::tick);
            var adultTag=save(c,detached);q.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            for(boolean adultFirst:new boolean[]{false,true}){
                var adult=load(c,adultTag);c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(adult),"Original identity installed");
                if(adultFirst)atTime(c,current,adult::tick);
                var capture=ReloadObservation.capture(c.getLevel(),q.getUUID(),p.getBlockPos(),q.founding().plan().chamber(),interval(last,current));
                var row=member(capture,q.getUUID());resolved(c,"pre_nursery_"+adultFirst,row,0,6);
                c.assertTrue(row.ownInterval().equals(interval(adultLast,current))&&row.ownInterval().ticks()==8
                    &&row.returnBoundary().evidence().latest()==adultLast,"Captured original adult return begins before nursery L, in both orders");
                if(!adultFirst)atTime(c,current,adult::tick);
                adult.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            }
            var earlier=load(c,adultTag);c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(earlier),"Earlier resuming original");
            atTime(c,current-1,earlier::tick);
            var capture=ReloadObservation.capture(c.getLevel(),q.getUUID(),p.getBlockPos(),q.founding().plan().chamber(),interval(last,current));
            var row=member(capture,q.getUUID());resolved(c,"earlier_resume",row,1,5);
            c.assertTrue(earlier.lastSimulationBoundary().ownInterval().ticks()==7&&row.ownInterval().ticks()==0
                &&row.ownInterval().equals(interval(current-1,current))&&row.returnBoundary()==null,
                "Captured row never reuses the older seven-tick return as newly owed work");
            ReconciliationEvidence.log("earlier_resume_capture","timestamp_and_removal",ReconciliationEvidence.observation(capture));
            c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void intermittentProductionStepsRetainHolesAndOverflowIsUnresolved(GameTestHelper c) {
        founded(c, false, (q, p) -> {
            long last = p.lastSimulationGameTime().orElseThrow(); var nurseryTag = p.saveWithFullMetadata(c.getLevel().registryAccess());
            var detached = load(c, save(c,q));
            q.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK); // Removal fixture, explicitly not actual chunk eviction.
            atTime(c, last + 2, detached::tick); atTime(c, last + 6, detached::tick);
            c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(load(c, save(c, detached))), "Same intermittent adult reconstructed");
            var resumed = BroodGameTest.restorePile(c, p, nurseryTag); ordinary(c, resumed, last + 7);
            resolved(c, "intermittent_recent_is_not_continuous", member(resumed.reloadObservation(), q.getUUID()), 2, 4);
            var installed = (LasiusNigerEntity)c.getLevel().getEntity(q.getUUID());
            var ant = load(c,save(c,installed)); installed.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            for (int i=1;i<=AdultSimulation.MAX_RUNS+2;i++) { long now = last + 10 + 2L*i; atTime(c, now, ant::tick); }
            var compacted=load(c, save(c, ant));
            c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(compacted),"Compacted original identity installed");
            var evidence = compacted.simulationEvidence();
            c.assertTrue(evidence.runs().size() == AdultSimulation.MAX_RUNS && evidence.knownAfter() > last,
                "Evidence compaction has explicit coverage floor and fixed maximum ranges");
            var unknown = evidence.reconcile(interval(last, evidence.latest() + 1));
            c.assertTrue(unknown.status().startsWith("UNKNOWN_EVIDENCE_BEFORE_") && unknown.missed().isEmpty() && unknown.alreadySimulated().isEmpty(), "No guessed count after bounded evidence loss");
            var captured=ReloadObservation.capture(c.getLevel(),q.getUUID(),resumed.getBlockPos(),q.founding().plan().chamber(),interval(last,evidence.latest()+1));
            var row=member(captured,q.getUUID());
            c.assertTrue(row.availability().equals("OBSERVED")&&row.reserved()&&row.evidence().equals(evidence)
                &&row.coverage().equals(unknown)&&row.coverage().missed().isEmpty(),"Captured member below history floor retains identity/reservation and unresolved counts");
            ReconciliationEvidence.log("captured_history_floor","timestamp_and_removal",ReconciliationEvidence.observation(captured));
            PrimeAnts.LOGGER.info("T02 RECON label=bounded_overflow result={} evidence={}", unknown, evidence); c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void unavailableWorkerKeepsReservationAndLaterReturnKeepsIdentity(GameTestHelper c) {
        founded(c, true, (q, p) -> {
            var worker = f.workers(c, q).getFirst(); var adultTag = save(c, worker);
            var nurseryTag = p.saveWithFullMetadata(c.getLevel().registryAccess()); long last = p.lastSimulationGameTime().orElseThrow();
            long occupied = ColonyMembers.get(c.getLevel()).occupied(q.getUUID()); var brood = p.consumed();
            worker.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var resumed = BroodGameTest.restorePile(c, p, nurseryTag); ordinary(c, resumed, last + 7);
            var captured = resumed.reloadObservation(); var row = member(captured, worker.getUUID());
            ReconciliationEvidence.log("unavailable_capture","manual_unloaded_reason_removal",ReconciliationEvidence.observation(captured));
            c.assertTrue(row.availability().equals("UNKNOWN_UNAVAILABLE") && row.reserved() && row.coverage().missed().isEmpty()
                &&row.evidence()==null&&row.ownInterval()==null&&row.currentTimestampInterval()==null&&row.returnBoundary()==null
                && row.adult().equals(worker.getUUID()) && row.brood().equals(worker.broodId()) && row.home().equals(worker.nurseryHome()), "UNKNOWN retains exact identity and reservation, no guessed ticks");
            c.assertTrue(ColonyMembers.get(c.getLevel()).occupied(q.getUUID()) == occupied && !ColonyMembers.get(c.getLevel()).member(worker.getUUID()).dead()
                && resumed.consumed().equals(brood) && resumed.records().isEmpty() && c.getLevel().getEntity(worker.getUUID()) == null, "No death, replacement, free slot or repeated brood emergence");
            var returning = load(c, adultTag); c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(returning), "Original worker returns later");
            var before = save(c, returning);
            var later = ReloadObservation.capture(c.getLevel(), q.getUUID(), resumed.getBlockPos(), q.founding().plan().chamber(), interval(last, last + 9));
            resolved(c, "worker_later_return_eight", member(later, worker.getUUID()), 0, 8);
            c.assertTrue(before.equals(save(c, returning)) && captured == resumed.reloadObservation() && row.coverage().missed().isEmpty(), "Later inspection does not revise old UNKNOWN or mutate returning adult");
            PrimeAnts.LOGGER.info("T02 RECON label=unavailable_reservation row={} occupied={} consumed={}", row, occupied, brood); c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void entityReconstructionLegacyMigrationBirthAndDuplicateBoundariesPreserveBiology(GameTestHelper c) {
        founded(c, true, (q, p) -> {
            for (var ant : java.util.stream.Stream.concat(java.util.stream.Stream.of(q), f.workers(c,q).stream()).toList()) {
                var tag = save(c, ant); var legacyTag = tag.copy(); legacyTag.remove(AdultSimulation.FIELD);
                var restored = load(c, tag); c.assertTrue(save(c, restored).equals(tag), "All adult metadata/biology round trip exactly");
                var legacy = load(c, legacyTag); long loadTime = c.getLevel().getGameTime();
                c.assertTrue(legacy.simulationEvidence().origin() == AdultSimulation.Origin.LEGACY_LOAD && legacy.simulationEvidence().latest() == loadTime
                    && biology(save(c, legacy)).equals(legacyTag), "Legacy migration at server-level load leaves all biological fields/policies/cargo/identity exact");
                var again = load(c, save(c, legacy)); c.assertTrue(save(c, again).equals(save(c, legacy)), "Repeated same-time migration/reconstruction does not renew boundary or biology");
                var unknown = legacy.simulationEvidence().reconcile(interval(loadTime - 10, loadTime + 1));
                c.assertTrue(unknown.missed().isEmpty(), "Legacy pre-load interval remains unresolved");
                var evidence = restored.simulationEvidence();
                long birth = evidence.boundary();
                long beforeBirth = Math.max(0, birth - 10);
                var preBirth = evidence.reconcile(interval(beforeBirth, birth + 1));
                c.assertTrue(preBirth.missed().orElseThrow() == 0 && preBirth.beforeBoundary() == ColonyClock.missedTicks(beforeBirth,birth+1), "Birth boundary never charges before birth");
                var atLatest = evidence.reconcile(interval(evidence.latest(), evidence.latest()));
                c.assertTrue(atLatest.missed().orElseThrow() == 0, "Duplicate/reversed exclusive interval owes zero");
                PrimeAnts.LOGGER.info("T02 RECON label=migration_birth adult={} birth={} legacy={} beforeBirth={}", ant.getUUID(), evidence, legacy.simulationEvidence(), preBirth);
            }
            c.succeed();
        });
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void partialTerrainCapturePreservesUnknownNursingAndLoadedDamagePrecedence(GameTestHelper c) {
        nursingColony(c, (q, p) -> {
            // Keep actual retained nursing roles under UNKNOWN; no task fixtures or supply changes.
            var plan = q.founding().plan(); var workers = f.workers(c, q);
            var tags = workers.stream().map(w -> save(c,w)).toList(); var registry = ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE, ColonyMembers.get(c.getLevel())).getOrThrow();
            try (var hidden = UnavailableCells.hide(c, plan.at(2,-2,-3))) {
                var before = p.saveWithFullMetadata(c.getLevel().registryAccess());
                var capture = ReloadObservation.capture(c.getLevel(), q.getUUID(), p.getBlockPos(), plan.chamber(), interval(p.lastSimulationGameTime().orElseThrow(), p.lastSimulationGameTime().orElseThrow()+21));
                c.assertTrue(before.equals(p.saveWithFullMetadata(c.getLevel().registryAccess())) && tags.equals(workers.stream().map(w -> save(c,w)).toList())
                    && registry.equals(ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE, ColonyMembers.get(c.getLevel())).getOrThrow()), "Observation preserves stage clocks, retained worker roles and all accounting");
                var evaluation = ColonyDevelopment.evaluate(c.getLevel(), q.getUUID(), plan, p.operational(), p.adultCapacity(), p.loadedTicks());
                c.assertTrue(evaluation.chambers().getFirst().functions().get(ChamberFunction.NURSERY) == ColonyDevelopment.Presence.UNKNOWN, "Existing unavailable enclosure remains UNKNOWN");
                c.assertTrue(workers.stream().filter(w->w.workerTasks().nursing()).count()>=2
                    &&workers.stream().filter(w->w.workerTasks().nursing()).noneMatch(w->w.workerTasks().nursingAuthorized(c.getLevel())),
                    "Real nursing roles retain identity while UNKNOWN pauses their ordinary work authorization");
                var wall = plan.at(6,-1,-1); var state = c.getLevel().getBlockState(wall);
                c.assertTrue(NestPlan.loaded(c.getLevel(),wall)&&state.isSolidRender(),"Actual loaded solid back wall before controlled breach");
                c.getLevel().setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
                var damaged = ColonyDevelopment.evaluate(c.getLevel(), q.getUUID(), plan, p.operational(), p.adultCapacity(), p.loadedTicks());
                c.assertTrue(damaged.chambers().getFirst().functions().get(ChamberFunction.NURSERY) == ColonyDevelopment.Presence.ABSENT,
                    "Loaded broken wall wins over unrelated UNKNOWN: "+damaged);
                c.getLevel().setBlock(wall, state, 3);
                PrimeAnts.LOGGER.info("T02 RECON label=partial_terrain observation={} unknown={} damaged={}", capture, evaluation, damaged);
            }
            c.succeed();
        });
    }

    @GameTest(maxTicks=20)
    public void completedDeathStepAndEarlyReturnsAreObservedButSavingIsNot(GameTestHelper c) {
        var worker = AntEntities.WORKER.create(c.getLevel(), EntitySpawnReason.BREEDING);
        c.assertTrue(worker != null, "New adult factory");
        // Detached fixture must not suffocate at the factory's default world origin before biology begins.
        var safe=net.minecraft.world.phys.Vec3.atCenterOf(c.absolutePos(new net.minecraft.core.BlockPos(1,3,1)));
        worker.setPos(safe.x,safe.y,safe.z);worker.setNoGravity(true);
        worker.initializeCallow(UUID.randomUUID(), UUID.randomUUID(), c.absolutePos(net.minecraft.core.BlockPos.ZERO), 1, 1);
        worker.setNoAi(true); worker.setHealth(1);
        long start = c.getLevel().getGameTime();
        var baseline = worker.simulationEvidence(); var tag = save(c, worker);
        c.assertTrue(worker.simulationEvidence().equals(baseline) && load(c,tag).simulationEvidence().equals(baseline), "Save/inspection/load are no completed step");
        for (int i=1;i<=20;i++) { long now=start+i; atTime(c, now, worker::tick); }
        c.assertTrue(!worker.isAlive() && worker.elapsedAgeTicks() == 20 && worker.simulationEvidence().latest() == start+20,
            "Fatal AdultLife early return records terminal biological step: age="+worker.elapsedAgeTicks()+" health="+worker.getHealth()+" evidence="+worker.simulationEvidence());
        var row = com.google.gson.JsonParser.parseString(AdultHistory.get(c.getLevel()).records().get(worker.getUUID().toString())).getAsJsonObject();
        c.assertTrue(row.get("lastSimulationGameTime").getAsLong() == start+20 && row.get("age").getAsLong() == 20, "Terminal receipt observes same death step before vanilla removes body");
        PrimeAnts.LOGGER.info("T02 RECON label=terminal_step evidence={} receipt={}", worker.simulationEvidence(), row); c.succeed();
    }

    @GameTest(maxTicks=20)
    public void duplicateAndReversedProductionObservationNeverFabricatesTicks(GameTestHelper c) {
        var worker = AntEntities.WORKER.create(c.getLevel(), EntitySpawnReason.BREEDING);
        c.assertTrue(worker != null, "Factory"); worker.setNoAi(true);
        long now = c.getLevel().getGameTime();
        atTime(c, now+1, worker::tick); var evidence = worker.simulationEvidence();
        atTime(c, now+1, worker::tick);
        c.assertTrue(worker.simulationEvidence().equals(evidence), "Duplicate completed timestamp adds no simulation evidence even in timestamp fixture");
        atTime(c, now, worker::tick);
        c.assertTrue(worker.simulationEvidence().origin() == AdultSimulation.Origin.REBASED && worker.simulationEvidence().reconcile(interval(now,now+1)).missed().orElseThrow() == 0, "Reversal rebases observation, no owed ticks");
        c.assertTrue(load(c,save(c,worker)).simulationEvidence().equals(worker.simulationEvidence()), "Rebased representation serializes exactly");
        atTime(c,-1,worker::tick);
        var negative=load(c,save(c,worker)).simulationEvidence();
        c.assertTrue(negative.invalidTime()&&negative.reconcile(interval(now,now+3)).missed().isEmpty(),"Negative domain persists explicit uncertainty, no fabricated owed ticks");
        c.succeed();
    }
}
