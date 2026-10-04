package dev.primeants.gametest.mixin;

import dev.primeants.gametest.IdleTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.phys.Vec3;
import java.util.function.ToDoubleFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LandRandomPos.class)
public abstract class IdleLandTraceMixin {
    @Redirect(method="generateRandomPosTowardDirection", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ai/util/RandomPos;generateRandomPosTowardDirection(Lnet/minecraft/world/entity/PathfinderMob;DLnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos candidate(PathfinderMob mob, double range, RandomSource random, BlockPos direction) {
        BlockPos pos = RandomPos.generateRandomPosTowardDirection(mob, range, random, direction);
        if (IdleTrace.tracks(mob)) IdleTrace.log(mob, "land_candidate", "direction=" + direction + " raw=" + pos
                + " stable=" + mob.getNavigation().isStableDestination(pos) + " outside=" + GoalUtils.isOutsideLimits(pos, mob)
                + " restricted=" + GoalUtils.isRestricted(GoalUtils.mobRestricted(mob, range), mob, pos)
                + " below=" + mob.level().getBlockState(pos.below()) + " at=" + mob.level().getBlockState(pos));
        return pos;
    }
    @Inject(method="getPos(Lnet/minecraft/world/entity/PathfinderMob;IILjava/util/function/ToDoubleFunction;)Lnet/minecraft/world/phys/Vec3;", at=@At("RETURN"))
    private static void result(PathfinderMob mob, int h, int v, ToDoubleFunction<BlockPos> weight, CallbackInfoReturnable<Vec3> cir) {
        IdleTrace.log(mob, "land_result", cir.getReturnValue());
    }
}
