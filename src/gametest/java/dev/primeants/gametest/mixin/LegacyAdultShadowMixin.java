package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.LegacyAdultShadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test mod only; observation surrounds the actual ordinary life owner and completed entity step. */
@Mixin(LasiusNigerEntity.class)
public abstract class LegacyAdultShadowMixin {
    @Inject(method="tick",at=@At(value="INVOKE",target="Ldev/primeants/entity/AdultLife;tick(Lnet/minecraft/server/level/ServerLevel;Ldev/primeants/entity/LasiusNigerEntity;)V"))
    private void beforeLife(CallbackInfo ci){LegacyAdultShadow.beforeLife((LasiusNigerEntity)(Object)this);}
    @Inject(method="tick",at=@At(value="INVOKE",target="Ldev/primeants/entity/AdultLife;tick(Lnet/minecraft/server/level/ServerLevel;Ldev/primeants/entity/LasiusNigerEntity;)V",shift=At.Shift.AFTER))
    private void afterLife(CallbackInfo ci){LegacyAdultShadow.afterLife((LasiusNigerEntity)(Object)this);}
    @Inject(method="tick",at=@At("RETURN"))
    private void afterEntity(CallbackInfo ci){LegacyAdultShadow.afterEntity((LasiusNigerEntity)(Object)this);}
}
