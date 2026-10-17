package dev.primeants.gametest.mixin;
import dev.primeants.founding.QueenFounding;
import dev.primeants.gametest.PersistenceHarness;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Read the loaded persisted field for passive observations, without querying/creating terrain authority.
 * Ordinary serialization is deliberately unchanged, including its live readiness display. */
@Mixin(QueenFounding.class)
public abstract class PersistenceFoundingCaptureMixin {
    @Shadow private String reason;
    @Redirect(method="save",at=@At(value="INVOKE",target="Ldev/primeants/founding/QueenFounding;reason()Ljava/lang/String;"))
    private String observedReason(QueenFounding owner){return PersistenceHarness.passiveCapture()?reason:owner.reason();}
}
