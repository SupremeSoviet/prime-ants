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
    @Redirect(method="establish",at=@At(value="INVOKE",target="Ljava/util/UUID;randomUUID()Ljava/util/UUID;"))
    private UUID founding(){return TestIdentities.founding(queenId);}
    @Inject(method="layingIdentity",at=@At("RETURN")) private void laid(long tick,int slot,CallbackInfoReturnable<UUID> ci){TestIdentities.laying(queenId,tick,slot,ci.getReturnValue());}
}
