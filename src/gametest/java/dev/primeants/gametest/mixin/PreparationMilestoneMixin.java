package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.*;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Observe original calls only. No helper/gate/intervention is invoked to inspect it. */
@Mixin(MiningGameTest.class)
public abstract class PreparationMilestoneMixin {
    @Inject(method="upgradesFinished",at=@At("RETURN")) private static void upgrades(GameTestHelper c,LasiusNigerEntity q,CallbackInfoReturnable<Boolean> ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_original_upgradesFinished_return",Map.of("result",ci.getReturnValue()));}
    @Inject(method="miningPreparationOnly",at=@At("RETURN")) private static void preparation(GameTestHelper c,LasiusNigerEntity q,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_original_miningPreparationOnly_return",Map.of("returned",true));}
    @Inject(method="firstUnsafeFace",at=@At("HEAD")) private static void beforeUnsafe(GameTestHelper c,LasiusNigerEntity q,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_original_firstUnsafeFace_entry",Map.of("calledByOriginalFixture",true));}
    @Inject(method="firstUnsafeFace",at=@At("RETURN")) private static void afterUnsafe(GameTestHelper c,LasiusNigerEntity q,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_original_firstUnsafeFace_return",Map.of("returned",true));}
}
