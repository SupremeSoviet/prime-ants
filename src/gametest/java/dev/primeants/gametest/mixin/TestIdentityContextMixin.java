package dev.primeants.gametest.mixin;
import dev.primeants.gametest.TestIdentities;
import it.unimi.dsi.fastutil.objects.*;
import java.util.LinkedHashMap;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameTestInfo.class)
public abstract class TestIdentityContextMixin {
    @Shadow @Final @Mutable private Object2LongMap<Runnable> runAtTickTimeMap;
    @Unique private boolean primeants$iterating;
    @Unique private final LinkedHashMap<Runnable,Long> primeants$deferred=new LinkedHashMap<>();
    @Inject(method="<init>",at=@At("RETURN")) private void orderedCallbacks(CallbackInfo ci){runAtTickTimeMap=new Object2LongLinkedOpenHashMap<>();}
    @Redirect(method="tickInternal",at=@At(value="INVOKE",target="Lit/unimi/dsi/fastutil/objects/Object2LongMap;object2LongEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;"))
    private ObjectSet<Object2LongMap.Entry<Runnable>> beginIteration(Object2LongMap<Runnable> tasks){primeants$iterating=true;return tasks.object2LongEntrySet();}
    @Inject(method="setRunAtTickTime",at=@At("HEAD"),cancellable=true)
    private void deferStructuralInsertion(long time,Runnable task,CallbackInfo ci){
        if(primeants$iterating&&!runAtTickTimeMap.containsKey(task)){primeants$deferred.put(task,time);ci.cancel();}
    }
    @Inject(method="tickInternal",at=@At("RETURN")) private void flush(CallbackInfo ci){
        primeants$iterating=false;primeants$deferred.forEach((task,time)->runAtTickTimeMap.put(task,time.longValue()));primeants$deferred.clear();
    }
    @Inject(method="tick",at=@At("HEAD")) private void enter(CallbackInfo ci){TestIdentities.enter((GameTestInfo)(Object)this);}
    @Inject(method="tick",at=@At("RETURN")) private void leave(CallbackInfo ci){var t=(GameTestInfo)(Object)this;if(t.hasStarted()&&!t.isDone()&&(t.getTick()<=1||t.getTick()%1000==0))TestIdentities.snapshot(t,"tick");TestIdentities.leave();}
    @Inject(method="startTest",at=@At("HEAD")) private void begin(CallbackInfo ci){TestIdentities.lifecycle((GameTestInfo)(Object)this,"begin");}
    @Inject(method="startTest",at=@At("RETURN")) private void started(CallbackInfo ci){TestIdentities.lifecycle((GameTestInfo)(Object)this,"start");}
    @Inject(method="finish",at=@At("HEAD")) private void ended(CallbackInfo ci){TestIdentities.lifecycle((GameTestInfo)(Object)this,"finish");}
}
