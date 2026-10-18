package dev.primeants.gametest.mixin;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.FullStoreCausalProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BroodPile.class)
public abstract class FullStoreLayMixin {
    @Inject(method="lay(Lnet/minecraft/server/level/ServerLevel;Ldev/primeants/entity/LasiusNigerEntity;ZZ)Z",at=@At("RETURN")) private void observed(ServerLevel l,LasiusNigerEntity q,boolean care,boolean aggregate,CallbackInfoReturnable<Boolean> ci){FullStoreCausalProbe.lay(l,(BroodPile)(Object)this,care,aggregate,ci.getReturnValue());}
}
