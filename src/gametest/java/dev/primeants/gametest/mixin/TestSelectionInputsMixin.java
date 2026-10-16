package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.QueenFounding;
import dev.primeants.gametest.TestIdentities;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(QueenFounding.class)
public abstract class TestSelectionInputsMixin {
    @Shadow @Final private LasiusNigerEntity queen;
    @Redirect(method={"tick","claimForager"},at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <T extends Entity> List<T> inputs(ServerLevel l,Class<T> kind,AABB box,Predicate<? super T> predicate){
        var result=l.getEntitiesOfClass(kind,box,predicate);TestIdentities.selection(queen.getUUID(),"ordinary_founding_query",result);return result;
    }
}
