package dev.primeants.gametest.mixin;

import dev.primeants.gametest.IdleTrace;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomStrollGoal.class)
public abstract class IdleStrollTraceMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Redirect(method="canUse", at=@At(value="INVOKE", target="Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int opportunity(RandomSource random, int bound) {
        int result = random.nextInt(bound);
        IdleTrace.log(mob, "opportunity", "bound=" + bound + " draw=" + result);
        return result;
    }
    @Inject(method="canUse", at=@At("RETURN"))
    private void outcome(CallbackInfoReturnable<Boolean> cir) {
        IdleTrace.log(mob, "canUse", "accepted=" + cir.getReturnValue() + " passenger=" + mob.hasControllingPassenger() + " idle=" + mob.getNoActionTime());
    }
    @Redirect(method="start", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"))
    private boolean path(PathNavigation navigation, double x, double y, double z, double speed) {
        boolean accepted = navigation.moveTo(x, y, z, speed);
        var path = navigation.getPath();
        IdleTrace.log(mob, "path_start", "destination=" + x + "," + y + "," + z + " accepted=" + accepted + " path=" + path + " reach=" + (path != null && path.canReach()));
        return accepted;
    }
}
