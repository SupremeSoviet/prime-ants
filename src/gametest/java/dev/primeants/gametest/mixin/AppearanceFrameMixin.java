package dev.primeants.gametest.mixin;

import dev.primeants.gametest.AntRenderRecorder;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bracket one real extraction, rather than a later reconstruction of the lens. */
@Mixin(GameRenderer.class)
public abstract class AppearanceFrameMixin {
    @Inject(method="extract", at=@At("HEAD"))
    private void begin(DeltaTracker delta, boolean advance, CallbackInfo ci) { AntRenderRecorder.beginFrame(); }
    @Inject(method="extract", at=@At("TAIL"))
    private void end(DeltaTracker delta, boolean advance, CallbackInfo ci) { AntRenderRecorder.endFrame(delta); }
}
