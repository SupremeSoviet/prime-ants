package dev.primeants.gametest.mixin;

import dev.primeants.gametest.TransferFault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class TransferInsertionMixin {
    @Inject(method="addFreshEntity",at=@At("HEAD"),cancellable=true)
    private void primeAntsRefuseItem(Entity e,CallbackInfoReturnable<Boolean> cir) {
        if(e instanceof ItemEntity item && TransferFault.refuse(item))cir.setReturnValue(false);
    }
}
