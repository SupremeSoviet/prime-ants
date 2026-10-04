package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.EmergenceFault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class EmergenceInsertionMixin {
    @Inject(method="addFreshEntity", at=@At("HEAD"), cancellable=true)
    private void primeAntsRefuseInsertion(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LasiusNigerEntity ant && ant.broodId() != null && EmergenceFault.refuse(ant.queenId())) cir.setReturnValue(false);
    }
}
