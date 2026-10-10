package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.Mining;
import dev.primeants.gametest.MiningConsiderations;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Calls the original guard exactly once and returns its identical result; never assigns or edits. */
@Mixin(Mining.class)
public abstract class MiningConsiderationMixin {
    @Redirect(method="consider",at=@At(value="INVOKE",target="Ldev/primeants/founding/Mining$Job;removalProblem(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;)Ljava/lang/String;"))
    private String observeGuard(Mining.Job job,ServerLevel level,UUID owner,ServerLevel inputLevel,LasiusNigerEntity queen,List<LasiusNigerEntity> workers){
        String problem=job.removalProblem(level,owner);
        MiningConsiderations.observed(level,queen,workers,job,problem);
        return problem;
    }
}
