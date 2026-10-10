package dev.primeants.gametest.mixin;
import dev.primeants.founding.DigJob;
import dev.primeants.gametest.InitialSurfaceHabitat;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Current initial-habitat connections, with no forged completed DigJob or construction history. */
@Mixin(DigJob.class)
public abstract class InitialHabitatConnectionMixin {
    @Inject(method="anyOpening",at=@At("RETURN"),cancellable=true)
    private static void opening(ServerLevel l,BlockPos p,UUID owner,CallbackInfoReturnable<Boolean> result){
        if(!result.getReturnValue()&&InitialSurfaceHabitat.opening(l,p,owner))result.setReturnValue(true);
    }
}
