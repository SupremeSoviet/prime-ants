package dev.primeants.brood;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.time.SimulationTimeScale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/** Canonical first clutch. Only the loaded server block-entity ticker advances brood. */
public final class BroodPile extends BlockEntity {
    public static final int CAPACITY = 3;
    public static final long EGG_COST = 1000, LARVA_COST = 12000, MAX_RESERVE = CAPACITY * (EGG_COST + LARVA_COST);
    public static final double CARE_REACH_SQUARED = 2.25 * 2.25;
    private UUID queenId;
    private NestPlan plan;
    private final List<BroodRecord> records = new ArrayList<>();
    // Bounded consumed identities prevent replacement after death and reconcile saved-cocoon/live-adult overlap.
    private final Set<UUID> consumed = new HashSet<>();
    private long loadedTicks, stageDuration = stageTicks();
    private String condition = "unowned";
    public BroodPile(BlockPos pos, BlockState state) { super(NurseryBlocks.BROOD_TYPE, pos, state); }
    public static double multiplier() { return Double.parseDouble(System.getProperty("prime_ants.broodMultiplier", "1")); }
    public static long stageTicks() {
        long duration = new SimulationTimeScale(multiplier()).ticksForGameDays(0.5);
        if (duration > Long.MAX_VALUE / LARVA_COST) throw new IllegalArgumentException("Brood duration exceeds accounting range");
        return duration;
    }
    public static long callowTicks() { return new SimulationTimeScale(multiplier()).ticksForGameDays(0.2); }
    public List<BroodRecord> records() { return List.copyOf(records); }
    public Set<UUID> consumed() { return Set.copyOf(consumed); }
    public UUID queenId() { return queenId; }
    public long loadedTicks() { return loadedTicks; }
    public long stageDuration() { return stageDuration; }
    public String condition() { return condition; }
    public boolean ownedBy(UUID id, NestPlan p) { return id.equals(queenId) && plan != null && p.entrance().equals(plan.entrance())
            && p.direction() == plan.direction() && getBlockPos().equals(p.nursery()) && getBlockState().is(NurseryBlocks.BROOD_PILE); }
    /** Called exclusively from the settled queen's single founding/nursery controller. */
    public boolean establish(LasiusNigerEntity queen, NestPlan p) {
        if (queenId != null || !getBlockPos().equals(p.nursery())) return false;
        queenId = queen.getUUID(); plan = NestPlan.geometry(p.entrance(), p.direction());
        if (!queen.founding().sealed() || queen.position().distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())) > CARE_REACH_SQUARED
                || !queen.spendReserve(CAPACITY * EGG_COST)) { queenId = null; plan = null; return false; }
        for (int slot = 0; slot < CAPACITY; slot++) records.add(new BroodRecord(UUID.randomUUID(), queenId, slot));
        condition = "eggs_laid"; changed();
        PrimeAnts.LOGGER.info("Nursery established queen={} pile={} brood={} reserve={} multiplier={} stageTicks={}", queenId, getBlockPos(), records.stream().map(BroodRecord::id).toList(), queen.bodyReserve(), multiplier(), stageTicks());
        return true;
    }
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState before = getBlockState(), next = before;
        var properties = List.of(BroodPileBlock.A, BroodPileBlock.B, BroodPileBlock.C);
        for (int slot = 0; slot < CAPACITY; slot++) {
            final int index = slot;
            BroodStage stage = records.stream().filter(r -> r.slot() == index).map(BroodRecord::stage).findFirst().orElse(BroodStage.EMPTY);
            next = next.setValue(properties.get(slot), stage);
        }
        if (!next.equals(before)) level.setBlock(getBlockPos(), next, 3);
        level.sendBlockUpdated(getBlockPos(), before, next, 3);
    }
    void serverTick(ServerLevel server) {
        if (queenId == null || plan == null || !ownedBy(queenId, plan)) return;
        loadedTicks++;
        String habitat = plan.nurseryProblem(server, queenId);
        if (habitat != null) { condition = habitat; setChanged(); return; }
        var actor = server.getEntity(queenId);
        LasiusNigerEntity queen = actor instanceof LasiusNigerEntity q && q.isAlive() && !q.isRemoved() ? q : null;
        boolean care = queen != null && !queen.isNoAi() && queen.founding().sealed()
                && queen.position().distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())) <= CARE_REACH_SQUARED;
        condition = care ? "cared" : "caregiver_absent_or_out_of_reach";
        boolean visual = false;
        for (BroodRecord record : List.copyOf(records)) {
            if (consumed.contains(record.id())) { records.remove(record); visual = true; continue; }
            if (record.stage == BroodStage.LARVA) {
                long next = Math.min(stageDuration, record.progress + 1);
                // Cumulative integer accounting: EXACT 12,000 per larva even when accelerated/rounded.
                long required = (next * LARVA_COST / stageDuration) - record.nourishment;
                if (!care || queen.bodyReserve() == 0 || !queen.spendReserve(required)) { if (care) condition = "queen_reserve_exhausted"; continue; }
                record.nourishment += required;
            }
            if (record.progress < stageDuration) record.progress++;
            if (record.progress < stageDuration) continue;
            if (record.stage == BroodStage.COCOON) { if (emerge(server, record)) visual = true; }
            else {
                if (record.stage == BroodStage.LARVA && record.nourishment != LARVA_COST) continue;
                record.stage = record.stage == BroodStage.EGG ? BroodStage.LARVA : BroodStage.COCOON;
                record.progress = 0; visual = true;
                PrimeAnts.LOGGER.info("Brood stage queen={} brood={} stage={} pileTicks={} nourishment={}", queenId, record.id(), record.stage, loadedTicks, record.nourishment);
            }
        }
        if (visual) changed(); else setChanged();
    }
    private boolean emerge(ServerLevel server, BroodRecord r) {
        var existing = server.getEntity(r.workerId());
        if (existing != null) {
            if (existing instanceof LasiusNigerEntity ant && r.id().equals(ant.broodId()) && queenId.equals(ant.queenId())) {
                consumed.add(r.id()); records.remove(r); condition = "emergence_reconciled"; return true;
            }
            condition = "emergence_identity_conflict"; return false;
        }
        var worker = AntEntities.WORKER.create(server, EntitySpawnReason.BREEDING);
        if (worker == null) { condition = "worker_creation_failed"; return false; }
        worker.setUUID(r.workerId()); worker.initializeCallow(r.id(), queenId, plan.chamber());
        for (int f : new int[]{5, 3, 4}) for (int s : new int[]{-1, 0, 1}) {
            BlockPos p = plan.at(f, s, -2); Vec3 pos = Vec3.atBottomCenterOf(p);
            worker.setPos(pos);
            AABB body = worker.getBoundingBox();
            if (!NestPlan.walkable(server, p) || !server.noCollision(worker, body) || !server.getEntities(worker, body).isEmpty()) continue;
            if (!server.addFreshEntity(worker) || server.getEntity(r.workerId()) != worker) { condition = "worker_insertion_failed"; return false; }
            consumed.add(r.id()); records.remove(r); condition = "callow_emerged";
            PrimeAnts.LOGGER.info("Callow emerged queen={} brood={} worker={} pileTicks={} remaining={}", queenId, r.id(), worker.getUUID(), loadedTicks, records.size());
            return true;
        }
        condition = "emergence_space_blocked"; return false;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (queenId == null || plan == null) return;
        out.putString("Queen", queenId.toString()); out.store("Entrance", BlockPos.CODEC, plan.entrance()); out.putString("Direction", plan.direction().getName());
        out.putLong("LoadedTicks", loadedTicks); out.putString("Condition", condition);
        out.putLong("StageDuration", stageDuration);
        var list = out.childrenList("Brood"); for (BroodRecord r : records) r.save(list.addChild());
        out.store("Consumed", com.mojang.serialization.Codec.STRING.listOf(), consumed.stream().map(UUID::toString).sorted().toList());
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); records.clear(); consumed.clear(); queenId = null; plan = null;
        if (in.getString("Queen").isEmpty()) return;
        UUID id = UUID.fromString(in.getStringOr("Queen", "")); BlockPos entrance = in.read("Entrance", BlockPos.CODEC).orElseThrow();
        Direction direction = Direction.byName(in.getStringOr("Direction", ""));
        if (direction == null || direction.getAxis().isVertical()) throw new IllegalArgumentException("Invalid nursery direction");
        NestPlan p = NestPlan.geometry(entrance, direction);
        stageDuration = in.getLongOr("StageDuration", stageTicks());
        if (stageDuration < 1 || stageDuration > Long.MAX_VALUE / LARVA_COST) throw new IllegalArgumentException("Invalid brood duration");
        if (!p.nursery().equals(getBlockPos())) throw new IllegalArgumentException("Misplaced nursery");
        Set<Integer> slots = new HashSet<>(); Set<UUID> ids = new HashSet<>();
        for (var child : in.childrenListOrEmpty("Brood")) {
            BroodRecord r = BroodRecord.load(child);
            if (!id.equals(r.queenId()) || !slots.add(r.slot()) || !ids.add(r.id()) || r.progress() > stageDuration) throw new IllegalArgumentException("Duplicate, foreign or invalid brood");
            records.add(r);
        }
        for (String s : in.read("Consumed", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of())) consumed.add(UUID.fromString(s));
        if (records.size() > CAPACITY || consumed.size() > CAPACITY || ids.stream().anyMatch(consumed::contains)
                || records.size() + consumed.size() > CAPACITY) throw new IllegalArgumentException("Invalid clutch ownership");
        queenId = id; plan = p; loadedTicks = Math.max(0, in.getLongOr("LoadedTicks", 0)); condition = in.getStringOr("Condition", "restored");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
