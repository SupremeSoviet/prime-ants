package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.FounderAccessProbe;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SurfaceAccess.class)
public abstract class FounderAccessAdmissionMixin {
    @Inject(method="admit",at=@At("RETURN"))
    private static void admitted(ServerLevel l,SurfaceWork.Job j,UUID owner,LasiusNigerEntity w,CallbackInfoReturnable<SurfaceAccess> ci){
        FounderAccessProbe.admitted(l,owner,ci.getReturnValue());
    }
}
