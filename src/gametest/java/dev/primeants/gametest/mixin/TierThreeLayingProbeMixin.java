package dev.primeants.gametest.mixin;

import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.TierThreeEffectsProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BroodPile.class)
public abstract class TierThreeLayingProbeMixin {
    @Shadow protected abstract boolean originalTerminal();
    @Inject(method="lay",at=@At("HEAD"))
    private void before(ServerLevel l,LasiusNigerEntity q,boolean care,CallbackInfoReturnable<Boolean> ci){TierThreeEffectsProbe.beforeLay(l,(BroodPile)(Object)this,q,care,originalTerminal());}
    @Inject(method="lay",at=@At("RETURN"))
    private void after(ServerLevel l,LasiusNigerEntity q,boolean care,CallbackInfoReturnable<Boolean> ci){TierThreeEffectsProbe.afterLay(l,(BroodPile)(Object)this,ci.getReturnValue());}
}
