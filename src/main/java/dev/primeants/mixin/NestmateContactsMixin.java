package dev.primeants.mixin;

import dev.primeants.entity.Nestmates;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla LivingEntity uses this SAME contact list for pushing and cramming. */
@Mixin(Level.class)
public abstract class NestmateContactsMixin {
    @Inject(method = "getPushableEntities", at = @At("RETURN"), cancellable = true)
    private void primeAntsContacts(Entity source, AABB box, CallbackInfoReturnable<List<Entity>> cir) {
        if (source instanceof dev.primeants.entity.LasiusNigerEntity)
            cir.setReturnValue(cir.getReturnValue().stream().filter(e -> !Nestmates.matching(source, e)).toList());
    }
}
