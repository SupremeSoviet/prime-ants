package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.SurfaceWork;
import dev.primeants.gametest.GreatSurfaceProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SurfaceWork.Job.class)
public abstract class GreatSurfaceReceiptProbeMixin {
    @Inject(method="receipt",at=@At("RETURN"))
    private void receipt(ServerLevel l,String mode,LasiusNigerEntity w,CallbackInfo r){GreatSurfaceProbe.receipt(l,w,(SurfaceWork.Job)(Object)this,mode);}
}
