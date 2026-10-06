package dev.primeants.gametest.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import dev.primeants.gametest.AntRenderRecorder;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freeze evidence before the GPU readback is scheduled; callback waits may tick. */
@Mixin(Screenshot.class)
public abstract class AppearanceScreenshotMixin {
    @Inject(method="takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at=@At("HEAD"))
    private static void bind(RenderTarget target, int scale, Consumer<NativeImage> callback, CallbackInfo ci) {
        AntRenderRecorder.bindScreenshot(target);
    }
}
