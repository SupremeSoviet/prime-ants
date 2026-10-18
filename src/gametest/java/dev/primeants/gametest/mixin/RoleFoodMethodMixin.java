package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.*;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(NursingGameTest.class)
public abstract class RoleFoodMethodMixin {
    @Inject(method="total",at=@At("HEAD")) private void before(GameTestHelper c,LasiusNigerEntity q,CallbackInfoReturnable<Long> ci){RoleFoodCausalProbe.enter(c,q);}
    @Inject(method="total",at=@At("RETURN")) private void after(GameTestHelper c,LasiusNigerEntity q,CallbackInfoReturnable<Long> ci){RoleFoodCausalProbe.total((NursingGameTest)(Object)this,c,q,ci.getReturnValue());}
    @Redirect(method="total",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <T extends Entity> List<T> ground(ServerLevel l,Class<T> kind,AABB box,Predicate<? super T> predicate){var result=l.getEntitiesOfClass(kind,box,predicate);RoleFoodCausalProbe.query(box,result);return result;}
}
