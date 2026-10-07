package dev.primeants.founding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.brood.BroodPile;
import dev.primeants.colony.ChamberFunction;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.colony.ColonyDevelopment;
import dev.primeants.colony.StageRules;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.MaterialStore;
import dev.primeants.worker.MaterialUnits;
import dev.primeants.worker.TransferCustody;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.*;

/** Upgrade work (GDD v2 section 2): a colony that has unlocked a higher chamber tier rebuilds a chamber's walls in place,
 * block by block. One real builder at a time takes clay from the colony's confirmed store, carries it in its mandibles
 * to the next wall cell (NestWalls) and rams it into that cell's own earth: packed clay, NestWalls.CLAY_PER_CELL unit a
 * cell, no soil leaves the nest. Mature unlocks tier 2 and the nursery's chamber goes first; T06 adds the other chambers
 * and tier 3. A cell that is no longer natural or colony earth (a player placed or changed it) is never converted: it
 * stops the job, and the builder puts its unit back into the store.
 * <p>Exact accounting at every point: taken = carried + built + released, where taken counts units out of the store net
 * of units put back, built the clay rammed into converted cells, and released the units dead builders handed to transfer
 * custody, counted once and for good. The job also keeps each such transfer's identity, so the units still in custody
 * now (custody), before custody sets them down in the world, are a separate figure that never enters the equation. */
public final class ChamberUpgrade extends SavedData {
    /** Loaded game ticks between builder assignment attempts. */
    public static final int ASSIGN_RETRY = 20;
    public static final class Job {
        static final Codec<Job> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("entrance").forGetter(j -> j.home.entrance()),
            Codec.STRING.fieldOf("direction").forGetter(j -> j.home.direction().getName()),
            Codec.STRING.fieldOf("chamber").forGetter(j -> j.chamber),
            Codec.INT.fieldOf("tier").forGetter(j -> j.tier),
            BlockPos.CODEC.listOf().fieldOf("cells").forGetter(j -> j.cells),
            BlockPos.CODEC.listOf().fieldOf("built").forGetter(j -> j.built),
            Codec.INT.fieldOf("taken").forGetter(j -> j.taken),
            Codec.INT.fieldOf("released").forGetter(j -> j.released),
            Codec.STRING.fieldOf("claim").forGetter(j -> j.claim == null ? "" : j.claim.toString()),
            Codec.LONG.fieldOf("ticks").forGetter(j -> j.ticks),
            Codec.STRING.fieldOf("reason").forGetter(j -> j.reason),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("released_transfers", Map.of()).forGetter(j -> Map.copyOf(j.transfers))
        ).apply(i, Job::new));
        public final NestPlan home;
        public final String chamber;
        public final int tier;
        /** The wall cells this job converts, in order; built is always a prefix of them. */
        public final List<BlockPos> cells;
        private final List<BlockPos> built;
        private int taken, released;
        /** Each dead builder's transfer custody identity and the units it released (WorkerTasks.die). */
        private final Map<String, Integer> transfers;
        public UUID claim;
        public long ticks;
        public String reason;
        private Job(BlockPos entrance, String direction, String chamber, int tier, List<BlockPos> cells, List<BlockPos> built, int taken, int released, String claim, long ticks, String reason, Map<String, Integer> transfers) {
            Direction d = Direction.byName(direction);
            if (d == null || d.getAxis().isVertical()) throw new IllegalArgumentException("Invalid chamber upgrade home");
            this.home = NestPlan.geometry(entrance, d); this.chamber = chamber; this.tier = tier;
            this.cells = List.copyOf(cells); this.built = new ArrayList<>(built); this.taken = taken; this.released = released;
            this.claim = claim.isEmpty() ? null : UUID.fromString(claim); this.ticks = ticks; this.reason = reason;
            this.transfers = new HashMap<>(transfers); transfers.keySet().forEach(UUID::fromString);
            if (transfers.values().stream().anyMatch(n -> n < 1) || transfers.values().stream().mapToInt(Integer::intValue).sum() > released)
                throw new IllegalArgumentException("Invalid saved chamber upgrade releases");
            // Only the founding chamber's walls are upgraded so far (the nursery goes first; T06 adds the others).
            var walls = chamber.equals(ChamberRegistry.FOUNDING) ? walls(home, NestBlueprint.FOUNDING_CHAMBER) : List.<BlockPos>of();
            if (tier < 2 || tier > 3 || cells.isEmpty() || new HashSet<>(cells).size() != cells.size() || !walls.containsAll(cells) || built.size() > cells.size()
                    || !cells.subList(0, built.size()).equals(built) || released < 0 || carried() < 0 || carried() > NestWalls.CLAY_PER_CELL || ticks < 0)
                throw new IllegalArgumentException("Invalid saved chamber upgrade");
        }
        /** Units out of the store that are neither in a wall nor released: the builder carries them. */
        public int carried() { return taken - built.size() * NestWalls.CLAY_PER_CELL - released; }
        public int taken() { return taken; }
        public int released() { return released; }
        public List<BlockPos> built() { return List.copyOf(built); }
        public NestWalls.Ledger ledger() { return new NestWalls.Ledger(taken, carried(), built.size() * NestWalls.CLAY_PER_CELL, released); }
        /** Released units still in transfer custody now, not yet set down in the world: at most released. */
        public int custody(ServerLevel l) {
            return TransferCustody.get(l).contents().stream().filter(p -> transfers.containsKey(p.id().toString()) && p.stack().is(Items.CLAY_BALL)).mapToInt(p -> p.stack().getCount()).sum();
        }
        public boolean complete() { return built.size() == cells.size(); }
        public boolean stopped() { return reason.startsWith("stopped_"); }
        /** The next wall cell to convert. */
        public BlockPos next() { return cells.get(built.size()); }
        /** Still this colony's natural or colony earth, solid and dry: the only cell an upgrade converts. */
        public boolean convertible(ServerLevel l, BlockPos p, UUID owner) {
            return NestPlan.loaded(l, p) && ColonyTerrain.get(l).eligible(l, p, owner) && l.getBlockState(p).isSolidRender() && l.getFluidState(p).isEmpty();
        }
        /** The claimed builder took one cell's units out of the store (MaterialStore). */
        public void took(ServerLevel l) { taken += NestWalls.CLAY_PER_CELL; get(l).setDirty(); }
        /** The claimed builder put its units back into the store (MaterialStore). */
        public void putBack(ServerLevel l) { taken -= NestWalls.CLAY_PER_CELL; get(l).setDirty(); }
        /** The claimed builder rammed its units into the next wall cell. */
        public void built(ServerLevel l, BlockPos p) {
            if (complete() || !next().equals(p) || carried() < NestWalls.CLAY_PER_CELL) throw new IllegalStateException("Chamber upgrade order or units");
            built.add(p.immutable()); get(l).setDirty();
        }
        public void stop(ServerLevel l, String why) {
            reason = "stopped_" + why; get(l).setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("Chamber upgrade stopped chamber={} tier={} reason={} built={}/{} taken={} carried={} released={} claim={}", chamber, tier, reason, built.size(), cells.size(), taken, carried(), released, claim);
        }
        /** Ends the builder's claim: the job is complete or stopped, or waits for more clay. */
        public void release(ServerLevel l, UUID owner, String why) {
            if (carried() != 0) throw new IllegalStateException("A chamber upgrade releases its builder only with empty mandibles");
            claim = null; reason = why; get(l).setDirty();
            if (complete() && !stopped()) ChamberRegistry.get(l).claimTier(owner, chamber, tier);
            dev.primeants.PrimeAnts.LOGGER.info("Chamber upgrade released queen={} chamber={} tier={} reason={} built={}/{} taken={} released={}", owner, chamber, tier, why, built.size(), cells.size(), taken, released);
        }
    }
    public static final Codec<ChamberUpgrade> CODEC = Codec.unboundedMap(Codec.STRING, Job.CODEC).xmap(ChamberUpgrade::new, d -> Map.copyOf(d.jobs));
    public static final SavedDataType<ChamberUpgrade> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants", "chamber_upgrade"), ChamberUpgrade::new, CODEC, DataFixTypes.LEVEL);
    private final Map<String, Job> jobs;
    private final Map<UUID, List<Job>> byColony = new HashMap<>();
    private final Map<UUID, Long> nextAttempt = new HashMap<>();
    public ChamberUpgrade() { this(Map.of()); }
    private ChamberUpgrade(Map<String, Job> j) { jobs = new HashMap<>(j); index(); }
    private void index() {
        byColony.clear();
        for (var k : jobs.keySet().stream().sorted().toList()) {
            var parts = k.split("/");
            var job = jobs.get(k);
            if (parts.length != 3 || !parts[1].equals(job.chamber) || !parts[2].equals(Integer.toString(job.tier))) throw new IllegalArgumentException("Invalid chamber upgrade key " + k);
            byColony.computeIfAbsent(UUID.fromString(parts[0]), u -> new ArrayList<>()).add(job);
        }
        byColony.replaceAll((u, list) -> List.copyOf(list));
        var claims = new HashSet<UUID>();
        for (var list : byColony.values()) for (var j : list) if (j.claim != null && !claims.add(j.claim)) throw new IllegalArgumentException("Duplicate chamber upgrade builder");
    }
    public static ChamberUpgrade get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(TYPE); }
    private static String key(UUID owner, String chamber, int tier) { return owner + "/" + chamber + "/" + tier; }
    public Job job(UUID owner, String chamber, int tier) { return owner == null ? null : jobs.get(key(owner, chamber, tier)); }
    public List<Job> jobs(UUID owner) { return owner == null ? List.of() : byColony.getOrDefault(owner, List.of()); }
    /** The job this worker builds for, if any. Claims are exclusive. */
    public Job claimedBy(LasiusNigerEntity w) {
        for (var j : jobs(w.queenId())) if (w.getUUID().equals(j.claim)) return j;
        return null;
    }
    public boolean anyClaim(UUID owner) { return jobs(owner).stream().anyMatch(j -> j.claim != null); }
    /** Clay units the colony built into a chamber's walls, by its own jobs' records: what an unknown chamber may hold. */
    public int builtClay(UUID owner, String chamber) { return jobs(owner).stream().filter(j -> j.chamber.equals(chamber)).mapToInt(j -> j.built.size() * NestWalls.CLAY_PER_CELL).sum(); }
    /** Clay on its way from the store into the walls: known while the claimed builder is seen carrying it, possible
     * while the builder is unavailable. */
    public StageRules.Bound carried(ServerLevel l, UUID owner) {
        int known = 0, possible = 0;
        for (var j : jobs(owner)) {
            int units = j.carried(); if (units == 0) continue;
            if (j.claim != null && l.getEntity(j.claim) instanceof LasiusNigerEntity w && w.isAlive() && !w.isRemoved()
                    && w.getMainHandItem().is(Items.CLAY_BALL) && w.getMainHandItem().getCount() >= units) known += units;
            else possible += units;
        }
        return new StageRules.Bound(known, known + possible);
    }
    /** A dead builder's carried units went to transfer custody under this identity (WorkerTasks.die); the job waits for
     * another real worker. */
    public void release(ServerLevel l, LasiusNigerEntity w, int units, UUID transfer) {
        var j = claimedBy(w); if (j == null) return;
        j.claim = null; j.released += units; if (units > 0) j.transfers.merge(transfer.toString(), units, Integer::sum); j.reason = "builder_dead_units_in_custody"; setDirty();
        dev.primeants.PrimeAnts.LOGGER.info("Chamber upgrade builder died queen={} worker={} chamber={} released={} transfer={} ledger={} custodyNow={}", w.queenId(), w.getUUID(), j.chamber, units, transfer, j.ledger(), j.custody(l));
    }
    /** A chamber's wall cells in the world (NestWalls), for a registered chamber whose room the nest plan knows. */
    public static List<BlockPos> walls(NestPlan home, ChamberRegistry.Chamber chamber) {
        if (chamber.id().equals(ChamberRegistry.FOUNDING)) return walls(home, NestBlueprint.FOUNDING_CHAMBER);
        for (var function : List.of(ChamberFunction.MATERIAL_STORE, ChamberFunction.QUEENS_HALL)) for (var placement : NestBlueprint.placements(function)) {
            var built = ChamberExcavation.build(home, placement.name());
            if (built.min().equals(chamber.min()) && built.max().equals(chamber.max())) return walls(home, placement.room().cells());
        }
        return List.of();
    }
    static List<BlockPos> walls(NestPlan home, Set<NestBlueprint.Cell> room) {
        return NestWalls.walls(room).stream().map(c -> home.at(c.forward(), c.side(), c.dy())).toList();
    }
    /** A Mature colony whose nursery chamber is confirmed below tier 2 rebuilds its walls once its confirmed store holds a
     * cell's clay, no other builder works, dug rooms are finished, and a free worker leaves at least two caregivers. A
     * claimed, stopped or completed job needs no builder; a job waiting for clay resumes under the same conditions. */
    public void consider(ServerLevel l, LasiusNigerEntity q, List<LasiusNigerEntity> workers) {
        var p = q.founding().plan(); var owner = q.getUUID(); int tier = 2;
        if (p == null || q.founding().lifecycle() != QueenFounding.Lifecycle.OPEN || !q.founding().ready()) return;
        var job = job(owner, ChamberRegistry.FOUNDING, tier);
        if (job != null && (job.claim != null || job.stopped() || job.complete())) return;
        long now = l.getGameTime();
        if (now < nextAttempt.getOrDefault(owner, Long.MIN_VALUE)) return;
        nextAttempt.put(owner, now + ASSIGN_RETRY);
        if (DigJob.anyClaim(l, owner)) return; // one real builder at a time; absent lookups never free a claim
        var widening = NestExpansion.get(l).job(owner);
        if (widening != null && !widening.complete() && !widening.reason.startsWith("quarantined_")) return;
        for (var dig : ChamberExcavation.get(l).jobs(owner)) if (!dig.complete() && !dig.stopped() && !dig.reason.startsWith("quarantined_")) return;
        if (!(l.getBlockEntity(p.nursery()) instanceof BroodPile pile) || pile.stageEvaluation() == null) return;
        var e = pile.stageEvaluation();
        if (NestWalls.unlocked(e.stage()) < tier) return;
        var state = e.chambers().stream().filter(s -> s.id().equals(ChamberRegistry.FOUNDING)).findFirst().orElse(null);
        if (state == null || state.functions().get(ChamberFunction.NURSERY) != ColonyDevelopment.Presence.CONFIRMED || state.tier() >= tier) return;
        var store = MaterialStore.confirmed(l, owner, p);
        if (store == null || store.units(MaterialUnits.Material.CLAY) < NestWalls.CLAY_PER_CELL) return;
        var eligible = workers.stream().filter(w -> w.workerTasks().canConstruct(p) && NestExpansion.remainingCaregivers(l, owner, p, w) >= 2)
            .sorted(Comparator.comparing(w -> w.getUUID().toString())).toList();
        if (eligible.isEmpty()) return;
        if (job == null) {
            var terrain = ColonyTerrain.get(l); var cells = new ArrayList<BlockPos>();
            for (var c : walls(p, NestBlueprint.FOUNDING_CHAMBER)) {
                if (!NestPlan.loaded(l, c)) return;
                if (terrain.wallTier(l, c, owner) < tier) cells.add(c);
            }
            if (cells.isEmpty()) return;
            job = new Job(p.entrance(), p.direction().getName(), ChamberRegistry.FOUNDING, tier, cells, List.of(), 0, 0, "", 0, "planned_mature_colony_nursery_below_tier_" + tier, Map.of());
            jobs.put(key(owner, ChamberRegistry.FOUNDING, tier), job); index(); setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("Chamber upgrade planned queen={} chamber={} tier={} cells={} storeClay={} stage={} chamberTier={}",
                owner, ChamberRegistry.FOUNDING, tier, cells.size(), store.units(MaterialUnits.Material.CLAY), e.stage().serializedName(), state.tier());
        }
        var w = eligible.getFirst();
        if (w.workerTasks().assignUpgrade(p)) {
            job.claim = w.getUUID(); job.reason = "living_empty_worker_assigned"; index(); setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("Chamber upgrade assigned queen={} worker={} chamber={} tier={} built={}/{} ledger={}", owner, w.getUUID(), job.chamber, tier, job.built.size(), job.cells.size(), job.ledger());
        }
    }
}
