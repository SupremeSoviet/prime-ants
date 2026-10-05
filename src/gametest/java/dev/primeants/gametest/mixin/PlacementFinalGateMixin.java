package dev.primeants.gametest.mixin;
import dev.primeants.founding.*;
import dev.primeants.gametest.PlacementFault;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Negative fixture: only the second call site, the immediate gate, is refused. */
@Mixin(NaturalPlacement.class)
public abstract class PlacementFinalGateMixin {
    @Redirect(method="evaluateColumn",at=@At(value="INVOKE",target="Ldev/primeants/founding/NestPlan;candidate(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Ldev/primeants/founding/NestPlan;",ordinal=1))
    private NestPlan gate(ServerLevel l,BlockPos p,Direction d) {return PlacementFault.finalGate(l,p)?null:NestPlan.candidate(l,p,d);}
}
