package dev.primeants.worker;

import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.ChamberFunction;
import dev.primeants.colony.ChamberRegistry;
import dev.primeants.colony.ColonyDevelopment;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.ChamberExcavation;
import dev.primeants.founding.ChamberUpgrade;
import dev.primeants.founding.NestWalls;
import dev.primeants.founding.NestBlueprint;
import dev.primeants.founding.NestPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A colony's material store (GDD v2 section 2): canonical one-unit stacks of natural material on the floor of a dug
 * store chamber. A worker sets it up empty at the nest plan's marker cell; the colony's forager brings dropped material
 * in, one unit per trip (WorkerTasks). Ownership is the colony plus its nest plan placement, never the material. */
public final class MaterialStore extends BlockEntity {
    /** Tier-1 capacity in units (MaterialUnits): 16 of them are kept for clay. */
    public static final int CAPACITY = MaterialUnits.CAPACITY;
    private final List<ItemStack> contents = new ArrayList<>();
    private UUID colony;
    private NestPlan plan;
    private String placement;
    private UUID releaseId = UUID.randomUUID();
    public MaterialStore(BlockPos p, BlockState s) { super(NurseryBlocks.STORE_TYPE, p, s); }
    /** The material one item is (MaterialUnits.ITEMS), or null. */
    public static MaterialUnits.Material kind(ItemStack s) { return s.isEmpty() ? null : MaterialUnits.of(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()); }
    /** Natural units a store may hold: clay, stone, gravel, sand and ore, one item per unit. */
    public static boolean material(ItemStack s) { return kind(s) != null; }
    public List<ItemStack> contents() { return contents.stream().map(ItemStack::copy).toList(); }
    public int size() { return contents.size(); }
    private List<MaterialUnits.Material> held() { return contents.stream().map(MaterialStore::kind).toList(); }
    /** Held units of one material. */
    public int units(MaterialUnits.Material m) { return (int)MaterialUnits.count(held(), m); }
    /** Room for one unit of this item's material. */
    public boolean room(ItemStack s) { return MaterialUnits.room(held(), kind(s), capacity()); }
    public int capacity() {
        return level instanceof ServerLevel l ? MaterialUnits.capacity(ColonyDevelopment.capacityTier(l,colony,plan,ChamberExcavation.STORE,ChamberFunction.MATERIAL_STORE)) : CAPACITY;
    }
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
    /** The colony's own store block at its registered store chamber's marker, if loaded. */
    public static MaterialStore owned(ServerLevel l, UUID owner, NestPlan p) {
        var colony = owner == null ? null : ChamberRegistry.get(l).colony(owner);
        var chamber = colony == null ? null : colony.chamber(ChamberExcavation.STORE);
        var m = chamber == null ? null : chamber.markers().get(ChamberFunction.MATERIAL_STORE);
        return m != null && NestPlan.loaded(l, m) && l.getBlockEntity(m) instanceof MaterialStore s && s.ownedBy(owner, p) ? s : null;
    }
    /** The colony's own store, only while its chamber is confirmed now by the stage's own live rule. */
    public static MaterialStore confirmed(ServerLevel l, UUID owner, NestPlan p) {
        var s = owned(l, owner, p);
        return s != null && ColonyDevelopment.presence(l, owner, p, ChamberExcavation.STORE, ChamberFunction.MATERIAL_STORE) == ColonyDevelopment.Presence.CONFIRMED ? s : null;
    }
    /** Only the store job's own claimed, living, mature worker sets up a store, in physical reach of the marker cell. */
    public boolean establish(LasiusNigerEntity w, NestPlan p, String placement) {
        if (!(level instanceof ServerLevel l) || colony != null || w.queenId() == null || !getBlockPos().equals(marker(p, placement))
                || !w.isAlive() || w.isNoAi() || w.isCallow() || !WorkerTasks.reaches(l, w, Vec3.atBottomCenterOf(getBlockPos()).add(0, 0.15, 0))) return false;
        var job = ChamberExcavation.get(l).job(w.queenId(), ChamberExcavation.STORE);
        if (job == null || !w.getUUID().equals(job.claim) || !job.placement.equals(placement) || !job.home.entrance().equals(p.entrance()) || job.home.direction() != p.direction()) return false;
        colony = w.queenId(); plan = NestPlan.geometry(p.entrance(), p.direction()); this.placement = placement; changed(); return true;
    }
    /** The colony's authorized forager or claimed miner, in reach, puts its one physical unit into this store, if the store's
     * chamber is confirmed now and has room for that material. Otherwise the unit stays in the mandibles. */
    public boolean deposit(LasiusNigerEntity w, NestPlan p) {
        var cargo = w.getMainHandItem();
        if (!(level instanceof ServerLevel l) || w.queenId() == null || !ownedBy(w.queenId(), p) || !(w.workerTasks().authorized(l)||w.workerTasks().miningDeliveryAuthorized(l)) || cargo.getCount() != 1 || !room(cargo)
                || !WorkerTasks.reaches(l, w, Vec3.atBottomCenterOf(getBlockPos()).add(0, 0.15, 0))
                || ColonyDevelopment.presence(l, colony, p, ChamberExcavation.STORE, ChamberFunction.MATERIAL_STORE) != ColonyDevelopment.Presence.CONFIRMED) return false;
        contents.add(cargo.copy()); w.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY); changed(); return true;
    }
    /** The colony's claimed upgrade builder (ChamberUpgrade), empty-handed and in reach, takes one wall cell's clay out of
     * the store while the store's chamber is confirmed now. The unit goes into its mandibles and the job counts it taken. */
    public boolean takeForUpgrade(LasiusNigerEntity w, NestPlan p, ChamberUpgrade.Job job) {
        if (!(level instanceof ServerLevel l) || !upgradeReach(l, w, p, job) || !w.getMainHandItem().isEmpty() || units(MaterialUnits.Material.CLAY) < NestWalls.CLAY_PER_CELL) return false;
        int index = 0; while (kind(contents.get(index)) != MaterialUnits.Material.CLAY) index++;
        w.setItemSlot(EquipmentSlot.MAINHAND, contents.remove(index)); job.took(l); changed(); return true;
    }
    /** The claimed builder of a job that stopped while it carried clay puts the unit back; the job no longer counts it. */
    public boolean putBack(LasiusNigerEntity w, NestPlan p, ChamberUpgrade.Job job) {
        var cargo = w.getMainHandItem();
        if (!(level instanceof ServerLevel l) || !upgradeReach(l, w, p, job) || !cargo.is(net.minecraft.world.item.Items.CLAY_BALL) || cargo.getCount() != NestWalls.CLAY_PER_CELL
                || job.carried() < NestWalls.CLAY_PER_CELL || !room(cargo)) return false;
        contents.add(cargo.copy()); w.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY); job.putBack(l); changed(); return true;
    }
    private boolean upgradeReach(ServerLevel l, LasiusNigerEntity w, NestPlan p, ChamberUpgrade.Job job) {
        return w.queenId() != null && ownedBy(w.queenId(), p) && job != null && w.getUUID().equals(job.claim) && w.workerTasks().upgradeAuthorized(l)
            && WorkerTasks.reaches(l, w, Vec3.atBottomCenterOf(getBlockPos()).add(0, 0.15, 0))
            && ColonyDevelopment.presence(l, colony, p, ChamberExcavation.STORE, ChamberFunction.MATERIAL_STORE) == ColonyDevelopment.Presence.CONFIRMED;
    }
    /** A walkable store floor cell beside the block, nearest the worker, from which it reaches the block. */
    public BlockPos stand(ServerLevel l, LasiusNigerEntity w) {
        var floors = ChamberExcavation.build(plan, placement).tasks(); BlockPos best = null;
        for (var d : Direction.Plane.HORIZONTAL) {
            var p = getBlockPos().relative(d);
            if (floors.contains(p) && NestPlan.loaded(l, p) && NestPlan.walkable(l, p)
                && (best == null || w.position().distanceToSqr(Vec3.atBottomCenterOf(p)) < w.position().distanceToSqr(Vec3.atBottomCenterOf(best)))) best = p;
        }
        return best;
    }
    /** Restored block palettes can carry an old projection; inventory remains canonical across unavailable terrain/tier loss. */
    public void refreshDisplay() {
        if (level != null && !level.isClientSide() && !MaterialStoreBlock.showing(getBlockState(),contents).equals(getBlockState())) changed();
    }
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState before = getBlockState(), next = MaterialStoreBlock.showing(before, contents);
        if (!next.equals(before)) level.setBlock(getBlockPos(), next, 3);
        level.sendBlockUpdated(getBlockPos(), before, next, 3);
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
        super.saveAdditional(out); out.putInt("InventoryFormat",2); out.putString("ReleaseId", releaseId.toString());
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
        int format=in.getIntOr("InventoryFormat",1);
        if(format!=1&&format!=2)throw new IllegalArgumentException("Unsupported material inventory format");
        var stacks = in.read("Contents", ItemStack.CODEC.listOf()).orElse(List.of());
        var held = new ArrayList<MaterialUnits.Material>();
        for (var s : stacks) {
            if (s.getCount() != 1 || !MaterialUnits.room(held, kind(s), format==1?CAPACITY:MaterialUnits.MAX_CAPACITY)) throw new IllegalArgumentException("Invalid physical material store");
            held.add(kind(s));
        }
        if (!getBlockPos().equals(marker(plan, placement))) throw new IllegalArgumentException("Invalid physical material store");
        stacks.forEach(s -> contents.add(s.copy()));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
