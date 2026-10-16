package dev.primeants.gametest.mixin;
import dev.primeants.brood.BroodPile;
import dev.primeants.gametest.TestIdentities;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BroodPile.class)
public abstract class TestBroodIdentityMixin {
    @Shadow private UUID queenId;
    @Shadow private long loadedTicks;
    @Redirect(method="establish",at=@At(value="INVOKE",target="Ljava/util/UUID;randomUUID()Ljava/util/UUID;"))
    private UUID founding(){return TestIdentities.founding(queenId);}
    @Inject(method="layingIdentity",at=@At("HEAD"),cancellable=true)
    private void predicted(long tick,int slot,CallbackInfoReturnable<UUID> ci){
        var id=TestIdentities.predictedBirth(queenId,tick,slot);
        if(id!=null){TestIdentities.laying(queenId,tick,slot,id);ci.setReturnValue(id);}
    }
    @Inject(method="lay(Lnet/minecraft/server/level/ServerLevel;Ldev/primeants/entity/LasiusNigerEntity;ZZ)Z",at=@At("RETURN"))
    private void committed(net.minecraft.server.level.ServerLevel level,dev.primeants.entity.LasiusNigerEntity queen,boolean care,boolean aggregate,CallbackInfoReturnable<Boolean> ci){
        if(ci.getReturnValue()){var r=((BroodPile)(Object)this).records().getLast();TestIdentities.committedBirth(queenId,loadedTicks,r.slot(),r.id());}
    }
    @Inject(method="layingIdentity",at=@At("RETURN")) private void laid(long tick,int slot,CallbackInfoReturnable<UUID> ci){TestIdentities.laying(queenId,tick,slot,ci.getReturnValue());}
}
