package dev.primeants.gametest.mixin;

import dev.primeants.founding.SurfaceWork;
import dev.primeants.gametest.SurfaceOccupancyProbe;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Invoked only after the original guard once; never changes its result or searches/moves. */
@Mixin(SurfaceWork.class)
public abstract class SurfaceOccupancyProbeMixin {
    @Inject(method="targetProblem",at=@At("RETURN"))
    private static void returned(ServerLevel level,UUID owner,SurfaceWork.Job job,CallbackInfoReturnable<String> result){
        SurfaceOccupancyProbe.returned(level,owner,job,result.getReturnValue());
    }
}
