package dev.primeants.gametest.mixin;
import dev.primeants.gametest.TestIdentities;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameTestInfo.class)
public abstract class TestIdentityContextMixin {
    @Shadow @Final @Mutable private Object2LongMap<Runnable> runAtTickTimeMap;
    @Inject(method="<init>",at=@At("RETURN")) private void orderedCallbacks(CallbackInfo ci){runAtTickTimeMap=new Object2LongLinkedOpenHashMap<>();}
    @Inject(method="tick",at=@At("HEAD")) private void enter(CallbackInfo ci){TestIdentities.enter((GameTestInfo)(Object)this);}
    @Inject(method="tick",at=@At("RETURN")) private void leave(CallbackInfo ci){var t=(GameTestInfo)(Object)this;if(t.hasStarted()&&!t.isDone()&&(t.getTick()<=1||t.getTick()%1000==0))TestIdentities.snapshot(t,"tick");TestIdentities.leave();}
    @Inject(method="startTest",at=@At("RETURN")) private void started(CallbackInfo ci){TestIdentities.lifecycle((GameTestInfo)(Object)this,"start");}
    @Inject(method="finish",at=@At("HEAD")) private void ended(CallbackInfo ci){TestIdentities.lifecycle((GameTestInfo)(Object)this,"finish");}
}
