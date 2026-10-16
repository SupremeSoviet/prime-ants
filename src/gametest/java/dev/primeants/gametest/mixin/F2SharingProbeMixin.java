package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.F2CarryProbe;
import dev.primeants.worker.CropSharing;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(CropSharing.class)
public abstract class F2SharingProbeMixin {
    @Shadow @Final private LasiusNigerEntity donor;
    @Inject(method="tick",at=@At("RETURN"))
    private void shared(ServerLevel l,CallbackInfoReturnable<Boolean> ci){F2CarryProbe.event(donor,"sharing_return",ci.getReturnValue());}
}
