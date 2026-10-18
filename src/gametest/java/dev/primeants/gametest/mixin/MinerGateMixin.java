package dev.primeants.gametest.mixin;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.MinerCausalProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(NestPlan.class)
public abstract class MinerGateMixin {
    @Inject(method="walkable",at=@At("RETURN")) private static void walk(ServerLevel l,BlockPos p,CallbackInfoReturnable<Boolean> ci){if(MinerCausalProbe.directGate())MinerCausalProbe.event(MinerCausalProbe.current,"actual_walkable",p.toShortString()+":"+ci.getReturnValue());}
    @Inject(method="loaded",at=@At("RETURN")) private static void loaded(ServerLevel l,BlockPos p,CallbackInfoReturnable<Boolean> ci){if(MinerCausalProbe.directGate()||MinerCausalProbe.predicateCall)MinerCausalProbe.event(MinerCausalProbe.current,"actual_loaded",p.toShortString()+":"+ci.getReturnValue());}
}
