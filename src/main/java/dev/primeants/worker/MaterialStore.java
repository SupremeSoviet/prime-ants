package dev.primeants.worker;

import dev.primeants.brood.NurseryBlocks;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.ChamberExcavation;
import dev.primeants.founding.NestBlueprint;
import dev.primeants.founding.NestPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A colony's material store (GDD v2 section 2): canonical one-unit stacks of natural material on the floor of a dug
 * store chamber. A worker sets it up empty at the nest plan's marker cell; bringing material in is later work, but the
 * saved format already carries contents. Ownership is the colony plus its nest plan placement, never the material. */
public final class MaterialStore extends BlockEntity {
    /** Tier-1 capacity: the Mature stage's 16 clay units. A starting value for the work that fills stores. */
    public static final int CAPACITY = 16;
    private final List<ItemStack> contents = new ArrayList<>();
    private UUID colony;
    private NestPlan plan;
    private String placement;
    private UUID releaseId = UUID.randomUUID();
    public MaterialStore(BlockPos p, BlockState s) { super(NurseryBlocks.STORE_TYPE, p, s); }
    /** Natural units a store may hold (GDD v2 section 3): clay, stone, gravel and sand, one block or ball per stack. */
    public static boolean material(ItemStack s) {
        return !s.isEmpty() && (s.is(Items.CLAY_BALL) || s.is(Items.COBBLESTONE) || s.is(Items.STONE) || s.is(Items.GRAVEL) || s.is(Items.SAND));
    }
    public List<ItemStack> contents() { return contents.stream().map(ItemStack::copy).toList(); }
    public int size() { return contents.size(); }
    public String placement() { return placement; }
    public static BlockPos marker(NestPlan p, String placement) {
        var c = NestBlueprint.plan(placement).marker();
        return p.at(c.forward(), c.side(), c.dy());
    }
    public UUID componentOwner() { return colony != null && plan != null && level instanceof ServerLevel l && l.getBlockEntity(getBlockPos()) == this && ownedBy(colony, plan) ? colony : null; }
    public boolean ownedBy(UUID id, NestPlan p) {
        return id.equals(colony) && plan != null && placement != null && plan.entrance().equals(p.entrance()) && plan.direction() == p.direction()
            && getBlockPos().equals(marker(p, placement)) && getBlockState().is(NurseryBlocks.MATERIAL_STORE);
    }
    /** Only the store job's own claimed, living, mature worker sets up a store, in physical reach of the marker cell. */
    public boolean establish(LasiusNigerEntity w, NestPlan p, String placement) {
        if (!(level instanceof ServerLevel l) || colony != null || w.queenId() == null || !getBlockPos().equals(marker(p, placement))
                || !w.isAlive() || w.isNoAi() || w.isCallow() || !WorkerTasks.reaches(l, w, Vec3.atBottomCenterOf(getBlockPos()).add(0, 0.15, 0))) return false;
        var job = ChamberExcavation.get(l).job(w.queenId(), ChamberExcavation.STORE);
        if (job == null || !w.getUUID().equals(job.claim) || !job.placement.equals(placement) || !job.home.entrance().equals(p.entrance()) || job.home.direction() != p.direction()) return false;
        colony = w.queenId(); plan = NestPlan.geometry(p.entrance(), p.direction()); this.placement = placement; changed(); return true;
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }
    /** The block itself drops nothing, as the nest cache; any stored units leave through transfer custody as items. */
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel l) {
            int slot = 0;
            while (!contents.isEmpty()) {
                UUID id = UUID.nameUUIDFromBytes((releaseId + ":" + slot++).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                TransferCustody.get(l).take(id, "store:" + releaseId + ":" + pos, Vec3.atBottomCenterOf(pos).add(0, 0.15, 0), contents.getFirst());
                contents.removeFirst(); setChanged();
                TransferCustody.get(l).retry(l);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.putString("ReleaseId", releaseId.toString());
        if (colony == null || plan == null) return;
        out.putString("Colony", colony.toString()); out.store("Entrance", BlockPos.CODEC, plan.entrance()); out.putString("Direction", plan.direction().getName());
        out.putString("Placement", placement); out.store("Contents", ItemStack.CODEC.listOf(), contents);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); contents.clear(); colony = null; plan = null; placement = null;
        releaseId = in.getString("ReleaseId").map(UUID::fromString).orElseGet(UUID::randomUUID);
        if (in.getString("Colony").isEmpty()) return;
        colony = UUID.fromString(in.getStringOr("Colony", "")); Direction d = Direction.byName(in.getStringOr("Direction", ""));
        if (d == null || d.getAxis().isVertical()) throw new IllegalArgumentException("Invalid material store direction");
        plan = NestPlan.geometry(in.read("Entrance", BlockPos.CODEC).orElseThrow(), d); placement = in.getStringOr("Placement", "");
        var stacks = in.read("Contents", ItemStack.CODEC.listOf()).orElse(List.of());
        if (!getBlockPos().equals(marker(plan, placement)) || stacks.size() > CAPACITY || stacks.stream().anyMatch(s -> !material(s) || s.getCount() != 1))
            throw new IllegalArgumentException("Invalid physical material store");
        stacks.forEach(s -> contents.add(s.copy()));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
