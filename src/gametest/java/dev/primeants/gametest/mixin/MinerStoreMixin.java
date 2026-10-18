package dev.primeants.gametest.mixin;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.MinerCausalProbe;
import dev.primeants.worker.MaterialStore;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MaterialStore.class)
public abstract class MinerStoreMixin {
    @Inject(method="confirmed",at=@At("RETURN")) private static void confirmed(ServerLevel l,UUID owner,NestPlan p,CallbackInfoReturnable<MaterialStore> ci){if(MinerCausalProbe.active()){var s=ci.getReturnValue();MinerCausalProbe.event(MinerCausalProbe.current,"actual_store_confirmed",s==null?null:Map.of("position",s.getBlockPos().toShortString(),"size",s.size(),"capacity",s.capacity(),"contents",s.contents().toString()));}}
    @Inject(method="room",at=@At("RETURN")) private void room(ItemStack s,CallbackInfoReturnable<Boolean> ci){if(MinerCausalProbe.active())MinerCausalProbe.event(MinerCausalProbe.current,"actual_material_room",s+":"+ci.getReturnValue());}
}
