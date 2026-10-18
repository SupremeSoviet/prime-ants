package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.RoleFoodCausalProbe;
import dev.primeants.worker.NestCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(NestCache.class)
public abstract class RoleFoodCacheMixin {
    @Inject(method="deposit",at=@At("HEAD")) private void d0(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){RoleFoodCausalProbe.cache(w,"deposit_entry",null);}
    @Inject(method="deposit",at=@At("RETURN")) private void d1(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){RoleFoodCausalProbe.cache(w,"deposit_return",ci.getReturnValue());}
    @Inject(method="withdraw",at=@At("HEAD")) private void w0(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){RoleFoodCausalProbe.cache(w,"withdraw_entry",null);}
    @Inject(method="withdraw",at=@At("RETURN")) private void w1(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){RoleFoodCausalProbe.cache(w,"withdraw_return",ci.getReturnValue());}
    @Inject(method="withdrawMeal",at=@At("HEAD")) private void m0(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){RoleFoodCausalProbe.cache(w,"meal_entry",null);}
    @Inject(method="withdrawMeal",at=@At("RETURN")) private void m1(LasiusNigerEntity w,NestPlan p,CallbackInfoReturnable<Boolean> ci){RoleFoodCausalProbe.cache(w,"meal_return",ci.getReturnValue());}
}
