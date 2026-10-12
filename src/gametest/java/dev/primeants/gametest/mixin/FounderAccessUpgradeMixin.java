package dev.primeants.gametest.mixin;
import dev.primeants.founding.*;
import dev.primeants.gametest.FounderAccessProbe;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChamberUpgrade.class)
public abstract class FounderAccessUpgradeMixin {
    @Inject(method="prioritySelection",at=@At("RETURN"))
    private void result(ServerLevel l,UUID owner,NestPlan home,CallbackInfoReturnable<ChamberUpgrade.PrioritySelection> ci){FounderAccessProbe.available(owner,ci.getReturnValue());}
}
