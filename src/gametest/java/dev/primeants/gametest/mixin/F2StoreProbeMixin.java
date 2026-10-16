package dev.primeants.gametest.mixin;
import dev.primeants.gametest.F2CarryProbe;
import dev.primeants.founding.NestPlan;
import dev.primeants.worker.MaterialStore;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MaterialStore.class)
public abstract class F2StoreProbeMixin {
    @Inject(method="deposit",at=@At("RETURN"))
    private void deposited(dev.primeants.entity.LasiusNigerEntity worker,NestPlan p,CallbackInfoReturnable<Boolean> ci){F2CarryProbe.event(worker,"material_deposit_return",ci.getReturnValue());}
    @Inject(method="confirmed",at=@At("RETURN"))
    private static void confirmed(ServerLevel l,UUID owner,NestPlan p,CallbackInfoReturnable<MaterialStore> ci){var w=F2CarryProbe.current;if(w!=null){var s=ci.getReturnValue();F2CarryProbe.event(w,"store_confirmation_return",s==null?null:s.getBlockPos());}}
    @Inject(method="room",at=@At("RETURN"))
    private void room(ItemStack s,CallbackInfoReturnable<Boolean> ci){var w=F2CarryProbe.current;if(w!=null)F2CarryProbe.event(w,"material_admission_return",s+":"+ci.getReturnValue());}
}
