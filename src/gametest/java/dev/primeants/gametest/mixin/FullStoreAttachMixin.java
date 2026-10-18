package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.*;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(NursingGameTest.class)
public abstract class FullStoreAttachMixin {
    @Inject(method="total",at=@At("HEAD")) private void attach(GameTestHelper c,LasiusNigerEntity q,CallbackInfoReturnable<Long> ci){FullStoreCausalProbe.attach(c,q);}
}
