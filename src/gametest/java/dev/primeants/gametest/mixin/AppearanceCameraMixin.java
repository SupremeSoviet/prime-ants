package dev.primeants.gametest.mixin;

import dev.primeants.gametest.AppearanceScenario;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observer lens only; the living observer remains on checked solid ground. */
@Mixin(Camera.class)
public abstract class AppearanceCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void appearanceLens(float partialTicks,CallbackInfo ci){var view=AppearanceScenario.view();if(view!=null){setPosition(view.eye());setRotation(view.yaw(),view.pitch());}}
}
