package dev.primeants.gametest.mixin;

import dev.primeants.PrimeAnts;
import dev.primeants.gametest.F2CarryProbe;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Development-only lifecycle context for the F2 trace; never schedules or controls a test. */
@Mixin(GameTestInfo.class)
public abstract class F2ContextProbeMixin {
    @Unique private static final boolean primeants$enabled = primeants$manifestHasF2();
    @Unique private static boolean primeants$manifestHasF2() {
        var path=System.getProperty("prime_ants.caseManifest");
        if(path==null)return false;
        try {
            var manifest=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(path))).getAsJsonObject();
            for(var name:manifest.getAsJsonArray("names"))if(name.getAsString().equals("prime_ants_test:forager_scaling_game_test_large_colony_foragers_carry_at_once_with_exact_accounting_and_claims_survive_reload"))return true;
            return false;
        } catch(java.io.IOException e) { return false; }
    }
    @Unique private static final Map<String, Long> primeants$active = new LinkedHashMap<>();
    @Unique private boolean primeants$seen;
    @Unique private boolean primeants$ended;
    @Unique private boolean primeants$f2() {
        return ((GameTestInfo)(Object)this).id().toString().equals("prime_ants_test:forager_scaling_game_test_large_colony_foragers_carry_at_once_with_exact_accounting_and_claims_survive_reload");
    }
    @Inject(method="startTest",at=@At("HEAD"))
    private void start(CallbackInfo ci) {
        if(!primeants$enabled)return;
        if (primeants$seen) return;
        primeants$seen=true;
        var t=(GameTestInfo)(Object)this;
        primeants$active.put(t.id().toString(),t.getLevel().getGameTime());
        PrimeAnts.LOGGER.info("T13 F2 CONTEXT START name={} gameTime={} seed={} dimension={} origin={} rotation={} active={}",t.id(),t.getLevel().getGameTime(),t.getLevel().getSeed(),t.getLevel().dimension().identifier(),t.getTestOrigin(),t.getRotation(),primeants$active);
    }
    @Inject(method="finish",at=@At("HEAD"))
    private void finish(CallbackInfo ci) {
        if(!primeants$enabled)return;
        if(primeants$ended)return;
        primeants$ended=true;
        var t=(GameTestInfo)(Object)this;
        PrimeAnts.LOGGER.info("T13 F2 CONTEXT END name={} tick={} gameTime={} error={} active={}",t.id(),t.getTick(),t.getLevel().getGameTime(),t.getError(),primeants$active);
        primeants$active.remove(t.id().toString());
    }
    @Inject(method="fail(Lnet/minecraft/gametest/framework/GameTestException;)V",at=@At("HEAD"))
    private void fail(GameTestException error,CallbackInfo ci) {
        if(primeants$f2())F2CarryProbe.endpoint("failure_before_finish:"+error);
    }
    @Inject(method="succeed",at=@At("HEAD"))
    private void succeed(CallbackInfo ci) {
        if(primeants$f2())F2CarryProbe.endpoint("original_success_before_cleanup");
    }
}
