package dev.primeants.gametest.mixin;
import dev.primeants.gametest.TestIdentities;
import dev.primeants.entity.LasiusNigerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LasiusNigerEntity.class)
public abstract class TestAntIdentityContextMixin {
    @Inject(method="tick",at=@At("HEAD")) private void enter(CallbackInfo ci){TestIdentities.enter((Entity)(Object)this);}
    @Inject(method="tick",at=@At("RETURN")) private void leave(CallbackInfo ci){TestIdentities.leave();}
}
