package dev.primeants.worker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.PrimeAnts;
import dev.primeants.founding.NestPlan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/** Canonical undelivered stacks after source cleanup; dimension save owns them until verified insertion.
 * Server-thread handoffs are serialized at normal saves. No claim of atomic abrupt-crash recovery. */
public final class TransferCustody extends SavedData {
    public record Pending(UUID id, String source, Vec3 position, ItemStack stack) {
        static final Codec<Pending> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.xmap(UUID::fromString,UUID::toString).fieldOf("id").forGetter(Pending::id),
                Codec.STRING.fieldOf("source").forGetter(Pending::source),
                Vec3.CODEC.fieldOf("position").forGetter(Pending::position),
                ItemStack.CODEC.fieldOf("stack").forGetter(Pending::stack)).apply(i,Pending::new));
    }
    public static final Codec<TransferCustody> CODEC=Pending.CODEC.listOf().xmap(TransferCustody::new,d->List.copyOf(d.pending.values()));
    public static final SavedDataType<TransferCustody> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","transfer_custody"),TransferCustody::new,CODEC,DataFixTypes.LEVEL);
    private final Map<UUID,Pending> pending=new LinkedHashMap<>();
    public TransferCustody() { }
    private TransferCustody(List<Pending> entries) {
        for(var p:entries) {
            if(p.stack().isEmpty() || !Double.isFinite(p.position().x) || !Double.isFinite(p.position().y) || !Double.isFinite(p.position().z) || pending.putIfAbsent(p.id(),p)!=null)throw new IllegalArgumentException("Invalid transfer custody");
        }
    }
    public static TransferCustody get(ServerLevel l) {return l.getDataStorage().computeIfAbsent(TYPE);}
    public List<Pending> contents() {return pending.values().stream().map(p->new Pending(p.id(),p.source(),p.position(),p.stack().copy())).toList();}
    /** Caller immediately clears its canonical slot after this returns, before retry or vanilla cleanup. */
    public void take(UUID id,String source,Vec3 position,ItemStack stack) {
        if(stack.isEmpty())throw new IllegalArgumentException("Empty custody");
        var previous=pending.get(id);
        if(previous!=null)throw new IllegalStateException("Source attempted a second handoff: "+id);
        pending.put(id,new Pending(id,source,position,stack.copy()));setDirty();
        PrimeAnts.LOGGER.info("Transfer custody id={} source={} stack={} position={}",id,source,stack,position);
    }
    public void retry(ServerLevel level) {
        for(var p:new ArrayList<>(pending.values())) {
            BlockPos pos=BlockPos.containing(p.position());
            // Never force-load a destination or interpret an unloaded entity lookup as evidence of loss.
            if(!NestPlan.loaded(level,pos) || !level.isPositionEntityTicking(pos))continue;
            if(level.getEntity(p.id())!=null) {PrimeAnts.LOGGER.error("Pending transfer identity conflict id={}; custody held",p.id());continue;}
            var item=new ItemEntity(level,p.position().x,p.position().y,p.position().z,p.stack().copy(),0,0,0);
            item.setUUID(p.id());item.setDefaultPickUpDelay();item.setUnlimitedLifetime();
            boolean accepted=level.addFreshEntity(item);
            // Return values and constructed objects alone cannot establish destination ownership.
            if(level.getEntity(p.id())==item && !item.isRemoved() && ItemStack.matches(item.getItem(),p.stack())) {
                pending.remove(p.id());setDirty();
                PrimeAnts.LOGGER.info("Transfer recovered id={} accepted={} stack={}",p.id(),accepted,item.getItem());
            }
        }
    }
    public static void tick(ServerLevel level) {if(level.getServer().getTickCount()%20==0)get(level).retry(level);}
}
