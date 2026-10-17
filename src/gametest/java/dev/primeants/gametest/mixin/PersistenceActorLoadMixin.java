package dev.primeants.gametest.mixin;
import dev.primeants.gametest.PersistenceHarness;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class PersistenceActorLoadMixin {
    @Inject(method="load",at=@At("RETURN"))
    private void first(ValueInput input,CallbackInfo ci){PersistenceHarness.actorDecoded((Entity)(Object)this);}
}
