package dev.primeants.gametest.mixin;
import dev.primeants.gametest.TestIdentities;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class TestEntityIdentityMixin {
    @Inject(method="<init>",at=@At("RETURN")) private void identity(CallbackInfo ci){TestIdentities.constructed((Entity)(Object)this);}
}
