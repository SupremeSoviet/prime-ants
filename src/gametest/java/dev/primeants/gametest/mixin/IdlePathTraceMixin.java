package dev.primeants.gametest.mixin;

import dev.primeants.gametest.IdleTrace;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigation.class)
public abstract class IdlePathTraceMixin {
    @Shadow @Final protected Mob mob;
    @Inject(method="moveTo(DDDID)Z", at=@At("RETURN"))
    private void exactPath(double x, double y, double z, int reach, double speed, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof PathfinderMob ant) {
            var path = ((PathNavigation)(Object)this).getPath();
            IdleTrace.log(ant, "exact_path", "destination=" + x + "," + y + "," + z + " reachRange=" + reach
                    + " accepted=" + cir.getReturnValue() + " path=" + path + " end=" + (path == null ? null : path.getEndNode()));
        }
    }
}
