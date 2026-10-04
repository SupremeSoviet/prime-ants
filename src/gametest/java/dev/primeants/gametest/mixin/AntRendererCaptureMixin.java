package dev.primeants.gametest.mixin;

import dev.primeants.client.AntRenderer;
import dev.primeants.client.AntRenderState;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.AntRenderRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AntRenderer.class, remap = false)
public abstract class AntRendererCaptureMixin {
    @Inject(method = "extractRenderState(Ldev/primeants/entity/LasiusNigerEntity;Ldev/primeants/client/AntRenderState;F)V", at = @At("TAIL"))
    private void record(LasiusNigerEntity entity, AntRenderState state, float partial, CallbackInfo ci) {
        AntRenderRecorder.record(entity, state, partial);
    }
}
