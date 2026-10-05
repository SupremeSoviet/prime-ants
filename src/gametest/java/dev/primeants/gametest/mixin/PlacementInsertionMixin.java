package dev.primeants.gametest.mixin;
import dev.primeants.gametest.PlacementFault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class PlacementInsertionMixin {
    @Inject(method="addFreshEntity",at=@At("HEAD"),cancellable=true)
    private void refuse(Entity entity,CallbackInfoReturnable<Boolean> cir) {
        if(PlacementFault.refuse(entity.getUUID()))cir.setReturnValue(false);
    }
}
