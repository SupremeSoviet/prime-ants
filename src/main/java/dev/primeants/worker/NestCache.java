package dev.primeants.worker;

import dev.primeants.brood.NurseryBlocks;
import dev.primeants.entity.LasiusNigerEntity;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Six canonical one-unit stacks. Blockstate is a visible projection, never the inventory. */
public final class NestCache extends BlockEntity {
    public static final int CAPACITY = 6;
    private final List<ItemStack> contents = new ArrayList<>();
    private UUID colony;
    private NestPlan plan;
    private UUID releaseId=UUID.randomUUID();
    public NestCache(BlockPos p, BlockState s) { super(NurseryBlocks.CACHE_TYPE,p,s); }
    public List<ItemStack> contents() { return contents.stream().map(ItemStack::copy).toList(); }
    public int size() { return contents.size(); }
    public boolean ownedBy(UUID id, NestPlan p) { return id.equals(colony) && plan != null && plan.entrance().equals(p.entrance()) && plan.direction()==p.direction() && getBlockPos().equals(p.cache()) && getBlockState().is(NurseryBlocks.NEST_CACHE); }
    public boolean establish(LasiusNigerEntity w, NestPlan p) {
        if (!(level instanceof ServerLevel l) || colony != null || !getBlockPos().equals(p.cache()) || w.queenId()==null
                || !(l.getEntity(w.queenId()) instanceof LasiusNigerEntity q) || !q.founding().claimedBy(w)
                || !w.isAlive() || w.isNoAi() || w.isCallow()
                || !WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(getBlockPos()).add(0,0.15,0))) return false;
        colony=w.queenId(); plan=NestPlan.geometry(p.entrance(),p.direction()); changed(); return true;
    }
    public boolean deposit(LasiusNigerEntity w, NestPlan p) {
        if (!(level instanceof ServerLevel l) || !ownedBy(w.queenId(),p) || !(w.workerTasks().authorized(l)||w.workerTasks().nursingAuthorized(l))
                || contents.size()>=CAPACITY || !WorkerTasks.food(w.getMainHandItem()) || w.getMainHandItem().getCount()!=1
                || !WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(getBlockPos()).add(0,0.15,0)) || p.nurseryProblem(l,colony,true)!=null) return false;
        contents.add(w.getMainHandItem().copy()); w.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY); changed(); return true;
    }
    public boolean withdraw(LasiusNigerEntity w,NestPlan p){
        if(!(level instanceof ServerLevel l)||!ownedBy(w.queenId(),p)||!w.workerTasks().nursingAuthorized(l)||!w.getMainHandItem().isEmpty()
                ||!WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(getBlockPos()).add(0,0.15,0))||p.nurseryProblem(l,colony,true)!=null)return false;
        for(int i=0;i<contents.size();i++)if(w.workerTasks().hasRecipient(l,contents.get(i))){
            w.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,contents.remove(i));changed();return true;
        }
        return false;
    }
    public boolean withdrawMeal(LasiusNigerEntity w,NestPlan p){
        if(!(level instanceof ServerLevel l)||!ownedBy(w.queenId(),p)||!w.isAlive()||w.isNoAi()||!(w.adultLife().hungry()||w.workerTasks().sharing().needsCrop(l))||!w.getMainHandItem().isEmpty()
            ||!ColonyMembers.get(l).belongs(w,w.queenId(),p.chamber())||p.nurseryProblem(l,colony,true)!=null
            ||!WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(getBlockPos()).add(0,0.15,0)))return false;
        for(int i=0;i<contents.size();i++)if(Nutrition.sugarYield(contents.get(i))>0&&w.nutrition().accepts(contents.get(i),Nutrition.QUEEN_SUGAR_CAPACITY,0)){
            w.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,contents.remove(i));changed();return true;
        }
        return false;
    }
    private void changed() {
        setChanged(); if (level==null || level.isClientSide()) return;
        BlockState before=getBlockState(), next=before;
        for(int i=0;i<CAPACITY;i++) {
            int value= i>=contents.size()?0:contents.get(i).is(dev.primeants.item.AntItems.SMALL_PREY)?6:contents.get(i).is(Items.ROTTEN_FLESH)?5:contents.get(i).is(Items.CHICKEN)?2:contents.get(i).is(Items.SWEET_BERRIES)?3:(contents.get(i).is(dev.primeants.item.AntItems.FLOWER_NECTAR)||contents.get(i).is(dev.primeants.item.AntItems.FLOWER_NECTAR_V2))?4:1;
            next=next.setValue(NestCacheBlock.SLOTS.get(i),value);
        }
        if(!next.equals(before)) level.setBlock(getBlockPos(),next,3);
        level.sendBlockUpdated(getBlockPos(),before,next,3);
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel l) {
            int slot=0;
            while(!contents.isEmpty()) {
                UUID id=UUID.nameUUIDFromBytes((releaseId+":"+slot++).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                TransferCustody.get(l).take(id,"cache:"+releaseId+":"+pos,Vec3.atBottomCenterOf(pos).add(0,0.15,0),contents.getFirst());
                contents.removeFirst();setChanged();
                TransferCustody.get(l).retry(l);
            }
        }
        super.preRemoveSideEffects(pos,state);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);out.putString("ReleaseId",releaseId.toString()); if(colony==null || plan==null)return;
        out.putString("Colony",colony.toString()); out.store("Entrance",BlockPos.CODEC,plan.entrance());out.putString("Direction",plan.direction().getName());
        out.store("Contents",ItemStack.CODEC.listOf(),contents);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); contents.clear();colony=null;plan=null;
        releaseId=in.getString("ReleaseId").map(UUID::fromString).orElseGet(UUID::randomUUID);
        if(in.getString("Colony").isEmpty())return;
        colony=UUID.fromString(in.getStringOr("Colony",""));Direction d=Direction.byName(in.getStringOr("Direction",""));
        if(d==null || d.getAxis().isVertical())throw new IllegalArgumentException("Invalid cache direction");
        plan=NestPlan.geometry(in.read("Entrance",BlockPos.CODEC).orElseThrow(),d);
        var stacks=in.read("Contents",ItemStack.CODEC.listOf()).orElse(List.of());
        if(!plan.cache().equals(getBlockPos()) || stacks.size()>CAPACITY || stacks.stream().anyMatch(s->!WorkerTasks.food(s)||s.getCount()!=1))throw new IllegalArgumentException("Invalid physical cache");
        stacks.forEach(s->contents.add(s.copy()));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
