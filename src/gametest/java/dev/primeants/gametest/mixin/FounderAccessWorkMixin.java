package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.FounderAccessProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SurfaceWork.class)
public abstract class FounderAccessWorkMixin {
    @Inject(method="stopAccess",at=@At("HEAD"))
    private void stop(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,String reason,CallbackInfo ci){FounderAccessProbe.stopping(l,w,j,reason);}
}
