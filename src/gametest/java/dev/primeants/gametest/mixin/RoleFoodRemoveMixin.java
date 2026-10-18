package dev.primeants.gametest.mixin;
import dev.primeants.gametest.RoleFoodCausalProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class RoleFoodRemoveMixin {
    @Inject(method="remove",at=@At("HEAD")) private void remove(Entity.RemovalReason reason,CallbackInfo ci){if((Object)this instanceof ItemEntity i)RoleFoodCausalProbe.itemChange(i,"remove_entry",reason);}
}
