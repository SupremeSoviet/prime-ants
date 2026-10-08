package dev.primeants.founding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.brood.BroodPile;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.ChamberFunction;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.colony.ColonyStage;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.MaterialStore;
import dev.primeants.worker.WorkerTasks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.Vec3;

/** Nest-plan chambers dug by real workers (NestBlueprint), one job per colony and room, on the shared DigJob machinery.
 * A Young colony that knows it lacks a material store tries the plan's placements against live terrain, digs the first
 * valid one, and its worker sets up the owned store block on the finished floor; the chamber is then registered. Once
 * the store is confirmed, the colony digs its queen's hall the same way, as an extension of the queen's own chamber; the
 * hall registers when its soil is settled. The 0.1.0 widening keeps priority and one real builder works at a time. */
public final class ChamberExcavation extends SavedData {
    public static final String STORE = "material_store", HALL = "queens_hall";
    /** Loaded ticks between placement attempts while no placement validates, and between worker assignment attempts. */
    public static final int RETRY = 100, ASSIGN_RETRY = 20;
    private static final Direction[] FACES = Direction.values();
    /** A placement compiled onto one colony's nest. marker is null for a room without a marker block (the hall). */
    public record Built(NestPlan home, NestBlueprint.Plan plan, List<BlockPos> tasks, List<BlockPos> shell, Set<BlockPos> connections, BlockPos marker, BlockPos min, BlockPos max, DugSpace.Labels labels) {
        static Built of(NestPlan home, NestBlueprint.Plan plan) {
            var tasks = plan.dig().stream().map(c -> at(home, c)).toList();
            var shell = plan.shell().stream().map(c -> at(home, c)).toList();
            var connections = new LinkedHashSet<BlockPos>(); plan.connections().forEach(c -> connections.add(at(home, c)));
            var room = plan.placement().room().cells().stream().map(c -> at(home, c)).toList();
            var min = new BlockPos(room.stream().mapToInt(BlockPos::getX).min().orElseThrow(), room.stream().mapToInt(BlockPos::getY).min().orElseThrow(), room.stream().mapToInt(BlockPos::getZ).min().orElseThrow());
            var max = new BlockPos(room.stream().mapToInt(BlockPos::getX).max().orElseThrow(), room.stream().mapToInt(BlockPos::getY).max().orElseThrow(), room.stream().mapToInt(BlockPos::getZ).max().orElseThrow());
            return new Built(home, plan, tasks, shell, Collections.unmodifiableSet(connections), plan.marker() == null ? null : at(home, plan.marker()), min, max,
                DugSpace.Labels.of(plan.placement().room().id()));
        }
        private static BlockPos at(NestPlan home, NestBlueprint.Cell c) { return home.at(c.forward(), c.side(), c.dy()); }
        public String room() { return plan.placement().room().id(); }
        public ChamberFunction function() { return plan.placement().room().function(); }
    }
    public static Built build(NestPlan home, String placement) { return Built.of(NestPlan.geometry(home.entrance(), home.direction()), NestBlueprint.plan(placement)); }
    /** Live reads of one colony's dug room for the shared scan. The marker cell is open while it is air or holds the
     * colony's own store block. */
    private static DugSpace.Ground<BlockPos> ground(ServerLevel l, UUID owner, Built built) {
        var terrain = ColonyTerrain.get(l);
        return new DugSpace.Ground<>() {
            @Override public boolean loaded(BlockPos p) { return NestPlan.loaded(l, p); }
            @Override public boolean open(BlockPos p) { return p.equals(built.marker()) ? l.getBlockState(p).isAir() || ownedStore(l, owner, built) : terrain.opened(l, p, owner); }
            @Override public boolean closed(BlockPos p) {
                return l.getBlockState(p).isSolidRender() && l.getFluidState(p).isEmpty()
                    || built.room().equals(STORE) && Mining.get(l).opening(l,p,owner);
            }
            @Override public BlockPos face(BlockPos p, int i) { return p.relative(FACES[i]); }
        };
    }
    private static boolean ownedStore(ServerLevel l, UUID owner, Built built) {
        return l.getBlockEntity(built.marker()) instanceof MaterialStore s && s.ownedBy(owner, built.home());
    }

    public static final class Job extends DigJob {
        static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("entrance").forGetter(j -> j.home.entrance()),
            Codec.STRING.fieldOf("direction").forGetter(j -> j.home.direction().getName()),
            Codec.STRING.fieldOf("room").forGetter(j -> j.room),
            Codec.STRING.fieldOf("placement").forGetter(j -> j.placement),
            BlockState.CODEC.listOf().fieldOf("expected").forGetter(j -> j.expected),
            BlockPos.CODEC.listOf().fieldOf("completed").forGetter(DigJob::completed),
            Codec.STRING.fieldOf("claim").forGetter(j -> j.claim == null ? "" : j.claim.toString()),
            Codec.INT.fieldOf("deposited").forGetter(j -> j.deposited),
            Codec.INT.fieldOf("released").forGetter(j -> j.released),
            Codec.LONG.fieldOf("ticks").forGetter(j -> j.ticks),
            Codec.STRING.fieldOf("reason").forGetter(j -> j.reason)
        ).apply(i, Job::new));
        public final String room, placement;
        public final Built built;
        private Job(BlockPos entrance, String direction, String room, String placement, List<BlockState> expected, List<BlockPos> completed, String claim, int deposited, int released, long ticks, String reason) {
            this(built(entrance, direction, room, placement), expected, completed, claim, deposited, released, ticks, reason);
        }
        private Job(Built built, List<BlockState> expected, List<BlockPos> completed, String claim, int deposited, int released, long ticks, String reason) {
            super(built.home(), built.tasks(), expected, completed, claim, deposited, released, ticks, reason);
            this.built = built; this.room = built.room(); this.placement = built.plan().placement().name();
        }
        private static Built built(BlockPos entrance, String direction, String room, String placement) {
            Direction d = Direction.byName(direction);
            if (d == null || d.getAxis().isVertical()) throw new IllegalArgumentException("Invalid chamber excavation home");
            var built = build(NestPlan.geometry(entrance, d), placement);
            if (!built.room().equals(room)) throw new IllegalArgumentException("Chamber excavation room does not match its placement");
            return built;
        }
        @Override public String label() { return room == null ? "chamber" : room; } // the base validates before fields are set
        @Override public String title() { return "Chamber " + room; }
        @Override public int cap() { return tasks.size(); }
        @Override public void changed(ServerLevel l) { get(l).setDirty(); }
        @Override public List<BlockPos> surfaces() { return built.shell(); }
        @Override public boolean stopsOnIncompatibleTarget() { return true; }
        @Override public boolean viaFoundingChamber() { return true; }
        @Override public boolean stopped() { return reason.startsWith("stopped_"); }
        @Override public void stop(ServerLevel l, String why) {
            reason = "stopped_" + why; changed(l);
            dev.primeants.PrimeAnts.LOGGER.info("Chamber excavation stopped room={} placement={} reason={} removed={} deposited={} released={} claim={}", room, placement, reason, removed(), deposited, released, claim);
        }
        /** Only the dug space matters to the nest (DugSpace): pending cells elsewhere are not yet part of it. Each face of
         * a completed cell is read by its own chunk's availability, never skipped because the completed cell is unloaded. */
        @Override public void findings(ServerLevel l, UUID owner, Findings r) {
            DugSpace.scan(ground(l, owner, built), completed, built.connections(), tasks, built.labels(), r);
        }
        public boolean established(ServerLevel l, UUID owner) {
            return built.marker() != null && NestPlan.loaded(l, built.marker()) && ownedStore(l, owner, built);
        }
        @Override public BlockPos pendingMarker(ServerLevel l, UUID owner) {
            var m = built.marker();
            if (m == null || !complete() || stopped() || !NestPlan.loaded(l, m) || established(l, owner)) return null;
            return l.getBlockState(m).isAir() && l.getFluidState(m).isEmpty() && l.getBlockState(m.below()).isSolidRender() ? m : null;
        }
        @Override public String completionReason(ServerLevel l, UUID owner) {
            return built.marker() == null || established(l, owner) ? "completed_connected_" + room : "completed_" + room + "_marker_cell_unavailable";
        }
        /** The claimed worker, in reach, sets up the owned store block on the finished floor and the chamber is registered. */
        @Override public boolean setUp(ServerLevel l, LasiusNigerEntity w) {
            var m = built.marker();var owner = w.queenId();
            if (m == null || owner == null || pendingMarker(l, owner) == null || !w.getUUID().equals(claim) || !WorkerTasks.reaches(l, w, Vec3.atBottomCenterOf(m).add(0, 0.15, 0))) return false;
            if (!l.setBlock(m, NurseryBlocks.MATERIAL_STORE.defaultBlockState(), 3)) return false;
            if (!(l.getBlockEntity(m) instanceof MaterialStore store) || !store.establish(w, home, placement)) { l.setBlock(m, Blocks.AIR.defaultBlockState(), 3); return false; }
            var registry = ChamberRegistry.get(l); registry.found(owner, home);
            boolean registered = registry.register(owner, new ChamberRegistry.Chamber(room, built.min(), built.max(), EnumSet.of(built.function()), 1, Map.of(built.function(), m)));
            reason = registered ? room + "_set_up_and_registered" : room + "_set_up_registry_conflict"; changed(l);
            return true;
        }
    }
    public static final Codec<ChamberExcavation> CODEC = Codec.unboundedMap(Codec.STRING, Job.CODEC).xmap(ChamberExcavation::new, d -> Map.copyOf(d.jobs));
    public static final SavedDataType<ChamberExcavation> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants", "chamber_excavation"), ChamberExcavation::new, CODEC, DataFixTypes.LEVEL);
    private final Map<String, Job> jobs;
    /** The same jobs by colony, rebuilt on every change; read per shell cell and per worker every tick. */
    private final Map<UUID, List<Job>> byColony = new HashMap<>();
    private final Map<UUID, Long> nextAttempt = new HashMap<>();
    /** Keyed by colony and room. */
    private final Map<String, Long> nextPlacement = new HashMap<>();
    private final Map<String, Built> placementFor = new HashMap<>();
    private final Map<String, String> waiting = new HashMap<>();
    public ChamberExcavation() { this(Map.of()); }
    private ChamberExcavation(Map<String, Job> j) { jobs = new HashMap<>(j); index(); }
    private void index() {
        byColony.clear();
        for (var k : jobs.keySet().stream().sorted().toList()) {
            int slash = k.indexOf('/');
            if (slash < 0 || !k.substring(slash + 1).equals(jobs.get(k).room)) throw new IllegalArgumentException("Invalid chamber excavation key " + k);
            byColony.computeIfAbsent(UUID.fromString(k.substring(0, slash)), u -> new ArrayList<>()).add(jobs.get(k));
        }
        byColony.replaceAll((u, list) -> List.copyOf(list));
    }
    public static ChamberExcavation get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(TYPE); }
    private static String key(UUID owner, String room) { return owner + "/" + room; }
    public Job job(UUID owner, String room) { return owner == null ? null : jobs.get(key(owner, room)); }
    public List<Job> jobs(UUID owner) { return owner == null ? List.of() : byColony.getOrDefault(owner, List.of()); }
    public void findings(ServerLevel l, UUID owner, Findings r) { for (var j : jobs(owner)) j.findings(l, owner, r); }
    public void reconcile(ServerLevel l, LasiusNigerEntity q) { for (var j : jobs(q.getUUID())) j.reconcile(l, q); }
    /** A claimed builder is still moving soil through the founding stair. */
    public boolean hauling(UUID owner) { return jobs(owner).stream().anyMatch(j -> j.claim != null && !j.complete()); }

    /** Live confirmation of a registered dug room: the shared dug-space scan over all its planned cells, the same one
     * brood care runs over a job's completed cells, so a finished room gets the same verdict from both. Null when no
     * nest-plan placement has these bounds. */
    public static Findings roomFindings(ServerLevel l, UUID owner, NestPlan home, BlockPos min, BlockPos max) {
        for (var function : List.of(ChamberFunction.MATERIAL_STORE, ChamberFunction.QUEENS_HALL)) for (var placement : NestBlueprint.placements(function)) {
            var built = build(home, placement.name());
            if (!built.min().equals(min) || !built.max().equals(max)) continue;
            var r = new Findings();
            DugSpace.scan(ground(l, owner, built), built.tasks(), built.connections(), built.tasks(), built.labels(), r);
            return r;
        }
        return null;
    }

    /** A placement validates on live terrain: every planned cell is this colony's natural or prepared soil, every shell
     * cell too (witnessed mineral may only support a floor), nothing is fluid, and the free cells of the colony's stage
     * mound (MoundSoil) can take every unit. Observed ineligibility rejects the placement; an unloaded cell only defers it
     * (Findings precedence). */
    static Findings validate(ServerLevel l, UUID owner, NestPlan home, Built built) {
        var r = new Findings(); var terrain = ColonyTerrain.get(l); var soil = NaturalSoil.get(l);
        for (var p : built.tasks())
            if (r.cell(NestPlan.loaded(l, p), "chunk_unavailable_at_" + p.toShortString()) && (!terrain.eligible(l, p, owner) || !l.getFluidState(p).isEmpty()))
                r.fault("planned_cell_not_natural_soil_at_" + p.toShortString());
        for (var p : built.shell()) {
            if (!r.cell(NestPlan.loaded(l, p), "chunk_unavailable_at_" + p.toShortString())) continue;
            boolean support = built.tasks().contains(p.above());
            if (!l.getFluidState(p).isEmpty() || !(terrain.eligible(l, p, owner) || support && soil.floorSupport(l, p))) r.fault("shell_cell_not_natural_soil_at_" + p.toShortString());
        }
        for (var p : built.connections()) if (r.cell(NestPlan.loaded(l, p), "chunk_unavailable_at_" + p.toShortString()) && !l.getFluidState(p).isEmpty()) r.fault("fluid_at_" + p.toShortString());
        var mound = MoundSoil.capacity(MoundSoil.slots(l, home, owner, MoundSoil.stage(l, home, owner)));
        if (mound.free() < built.tasks().size()) { if (mound.unknown() > 0) r.unavailable("mound_cells_unavailable"); else r.fault("mound_capacity_" + mound.free() + "_below_" + built.tasks().size()); }
        return r;
    }
    /** The colony's latest live evaluation says it is Young and certainly lacks the room, not merely unknown. The queen's
     * hall waits until the store is confirmed. */
    static boolean knownMissing(ServerLevel l, NestPlan p, String room) {
        if (!(l.getBlockEntity(p.nursery()) instanceof BroodPile pile) || pile.stageEvaluation() == null || pile.stageEvaluation().stage() != ColonyStage.YOUNG) return false;
        var e = pile.stageEvaluation();
        if (room.equals(HALL) && e.inputs().tier(ChamberFunction.MATERIAL_STORE).known() < 1) return false;
        var function = room.equals(HALL) ? ChamberFunction.QUEENS_HALL : ChamberFunction.MATERIAL_STORE;
        return e.result().missing(ColonyStage.MATURE).stream().anyMatch(m -> m.requirement().name().equals(function.serializedName()) && !m.unknown());
    }
    /** Placements tried for a room, in order. The hall takes the widening side the colony's 0.1.0 widening did not take;
     * without a widening it tries the left side first, leaving the widening's first choice (the right side) free. */
    static List<NestBlueprint.Placement> candidates(ServerLevel l, UUID owner, String room) {
        if (room.equals(STORE)) return NestBlueprint.MATERIAL_STORE;
        var widening = NestExpansion.get(l).job(owner);
        return NestBlueprint.QUEENS_HALL.stream().filter(h -> widening == null || h.widening() == -widening.side).toList();
    }
    /** The first placement that validates now, or null. Rejections are logged when they change; an unloaded cell stops
     * the search so a placement that may be valid is waited for, never skipped. */
    private Built firstValid(ServerLevel l, UUID owner, NestPlan p, String room) {
        var rejected = new ArrayList<String>(); var k = key(owner, room);
        for (var placement : candidates(l, owner, room)) {
            var built = build(p, placement.name()); var check = validate(l, owner, p, built);
            if (check.verdict() == Findings.Verdict.CLEAR) { waiting.put(k, String.join(";", rejected)); return built; }
            rejected.add(placement.name() + ":" + check.problem());
            if (check.verdict() == Findings.Verdict.UNKNOWN) break;
        }
        var why = String.join(";", rejected);
        if (!why.equals(waiting.put(k, why))) dev.primeants.PrimeAnts.LOGGER.info("Chamber excavation waits queen={} room={} reason={}", owner, room, why);
        return null;
    }
    /** A job stopped before any removal leaves no opening, so the room may be planned again at the next valid placement. */
    private Job replanUndug(UUID owner, String room) {
        var job = job(owner, room);
        if (job == null || !job.stopped() || job.removed() > 0 || job.claim != null) return job;
        jobs.remove(key(owner, room)); index(); setDirty();
        dev.primeants.PrimeAnts.LOGGER.info("Chamber excavation replans queen={} room={} after an undug stop", owner, room);
        return null;
    }
    /** A dug hall registers once its soil is settled: one tier-1 chamber with no marker block, because its marker is
     * the living queen in her own space (ColonyDevelopment.confirm). */
    private void register(ServerLevel l, UUID owner, Job hall) {
        var registry = ChamberRegistry.get(l); var colony = registry.colony(owner);
        if (colony != null && colony.chamber(HALL) != null) return;
        registry.found(owner, hall.home);
        boolean registered = registry.register(owner, new ChamberRegistry.Chamber(HALL, hall.built.min(), hall.built.max(), EnumSet.of(ChamberFunction.QUEENS_HALL), 1, Map.of()));
        hall.reason = registered ? HALL + "_dug_and_registered" : HALL + "_registry_conflict"; setDirty();
    }
    public void consider(ServerLevel l, LasiusNigerEntity q, List<LasiusNigerEntity> workers) {
        var p = q.founding().plan(); var owner = q.getUUID();
        if (p == null || q.founding().lifecycle() != QueenFounding.Lifecycle.OPEN || !q.founding().ready()) return;
        var hall = job(owner, HALL);
        if (hall != null && hall.complete() && !hall.stopped()) register(l, owner, hall); // once its soil is settled
        // One room at a time in plan order: the store until it is dug and its block set up, then the queen's hall.
        var store = replanUndug(owner, STORE);
        boolean stands = store != null && store.complete() && !store.stopped() && store.pendingMarker(l, owner) == null;
        var room = stands ? HALL : STORE; var job = stands ? replanUndug(owner, HALL) : store;
        if (job != null && job.room.equals(HALL) && job.complete()) return;
        if (job != null && (job.stopped() || job.reason.startsWith("quarantined_"))) return;
        if (DigJob.anyClaim(l, owner)) return; // one real builder at a time; absent lookups never free a claim
        var widening = NestExpansion.get(l).job(owner);
        if (widening != null && !widening.complete() && !widening.reason.startsWith("quarantined_")) return; // the 0.1.0 widening first
        if (job != null && job.complete() && job.pendingMarker(l, owner) == null) return;
        // Placement reads ~150 cells and caregiver counts read every body: both run on slow cadences, never every tick,
        // and caregivers are counted only once some placement validates (most nests never get here).
        long now = l.getGameTime(); var k = key(owner, room);
        if (job == null) {
            if (now >= nextPlacement.getOrDefault(k, Long.MIN_VALUE)) {
                nextPlacement.put(k, now + RETRY);
                placementFor.put(k, knownMissing(l, p, room) ? firstValid(l, owner, p, room) : null);
            }
            if (placementFor.get(k) == null) return;
        }
        if (now < nextAttempt.getOrDefault(owner, Long.MIN_VALUE)) return;
        nextAttempt.put(owner, now + ASSIGN_RETRY);
        if (!knownMissing(l, p, room)) return;
        // A real eligible worker comes first, so the widening (considered first, same eligibility) keeps priority.
        var eligible = workers.stream().filter(w -> w.workerTasks().canConstruct(p) && NestExpansion.remainingCaregivers(l, owner, p, w) >= 2)
            .sorted(Comparator.comparing(w -> w.getUUID().toString())).toList();
        if (eligible.isEmpty()) return;
        if (job == null) {
            var built = placementFor.remove(k);
            if (validate(l, owner, p, built).verdict() != Findings.Verdict.CLEAR) return; // terrain changed since; try again later
            job = new Job(built, built.tasks().stream().map(l::getBlockState).toList(), List.of(), "", 0, 0, 0, "planned_young_colony_lacks_" + room);
            jobs.put(k, job); index(); setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("Chamber excavation planned queen={} room={} placement={} tasks={} shell={} marker={} radius={} depth={} rejected={}",
                owner, room, job.placement, built.tasks().size(), built.shell().size(), built.marker(), NestBlueprint.RADIUS, NestBlueprint.DEPTH, waiting.remove(k));
        }
        var w = eligible.getFirst();
        if (w.workerTasks().assignConstruction(p, "assigned_" + room + "_excavation")) {
            job.claim = w.getUUID(); job.reason = "living_empty_worker_assigned"; setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("Chamber excavation assigned queen={} worker={} room={} placement={} removed={} tasks={} cadence={}", owner, w.getUUID(), room, job.placement, job.removed(), job.tasks.size(), QueenFounding.cadence());
        }
    }
}
