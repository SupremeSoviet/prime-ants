package dev.primeants.gametest;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
/** Publish before either genuine claimant scans: SEARCH age alone does not identify unseen cells. */
final class FoodCompetitionWindow {
    static String state(GameTestHelper c,LasiusNigerEntity w,net.minecraft.core.BlockPos source){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());w.workerTasks().save(out);
        var path=w.getNavigation().createPath(source,0,48);
        return w.getUUID()+" age="+w.elapsedAgeTicks()+" pulse="+(w.tickCount%20)+" ticking="+c.getLevel().isPositionEntityTicking(w.blockPosition())
            +" phase="+w.workerTasks().phase()+" why="+w.workerTasks().reason()+" search="+out.buildResult().getIntOr("SearchTicks",-1)
            +" cursor="+w.workerTasks().flowerInspections()+" target="+w.workerTasks().flowerSource()+" path="+(path!=null&&path.canReach())
            +" sharing="+w.workerTasks().sharing().actionTicks()+" harvestRoom="+w.workerTasks().harvestRoom(c.getLevel(),false);
    }
    static boolean fresh(GameTestHelper c,LasiusNigerEntity w){
        return switch(w.workerTasks().phase()){
            case OPENING,SOIL_OUT,EXIT -> true;
            case SEARCH -> w.workerTasks().flowerInspections()==0;
            default -> false;
        };
    }
}
