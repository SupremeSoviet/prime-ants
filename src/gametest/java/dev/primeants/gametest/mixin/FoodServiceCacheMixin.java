package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.FoodServiceProbe;
import dev.primeants.worker.NestCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(NestCache.class)
public abstract class FoodServiceCacheMixin {
    @Inject(method="deposit",at=@At("RETURN"))
    private void deposit(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){if(((BlockEntity)(Object)this).getLevel() instanceof ServerLevel l)admission(l,w,"deposit",ci.getReturnValue());}
    @Inject(method="withdraw",at=@At("RETURN"))
    private void withdraw(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){if(((BlockEntity)(Object)this).getLevel() instanceof ServerLevel l)admission(l,w,"nurseWithdraw",ci.getReturnValue());}
    @Inject(method="withdrawMeal",at=@At("RETURN"))
    private void meal(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){if(((BlockEntity)(Object)this).getLevel() instanceof ServerLevel l)admission(l,w,"mealWithdraw",ci.getReturnValue());}
    private static void admission(ServerLevel l,LasiusNigerEntity w,String method,boolean allowed){FoodServiceProbe.admission(l,w,method,allowed);dev.primeants.gametest.FounderGrowthProbe.cache(l,w,method,allowed);}
}
