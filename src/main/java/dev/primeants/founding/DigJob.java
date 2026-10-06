package dev.primeants.founding;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.ColonyMembers;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** One bounded excavation of a colony's nest: an ordered dig-task queue worked by one real worker at a time (the DIG and
 * DIG_OUT worker phases). Planned cells, successful worker removals (ColonyTerrain "worker_open" records) and soil
 * custody are distinct, and removed = carried + deposited + released at every point. The 0.1.0 widening
 * (NestExpansion) and nest-plan chambers (ChamberExcavation) share it. */
public abstract class DigJob {
    public final NestPlan home;
    public final List<BlockPos> tasks;
    public final List<BlockState> expected;
    protected final List<BlockPos> completed;
    public UUID claim;
    public int deposited,released;
    public long ticks;
    public String reason;
    protected DigJob(NestPlan home,List<BlockPos> tasks,List<BlockState> expected,List<BlockPos> completed,String claim,int deposited,int released,long ticks,String reason){
        this.home=home;this.tasks=List.copyOf(tasks);this.expected=List.copyOf(expected);this.completed=new ArrayList<>(completed);
        this.claim=claim.isEmpty()?null:UUID.fromString(claim);this.deposited=deposited;this.released=released;this.ticks=ticks;this.reason=reason;
        if(expected.size()!=tasks.size()||completed.size()>tasks.size()||!tasks.subList(0,completed.size()).equals(completed)||deposited<0||released<0||deposited+released>completed.size()||ticks<0)
            throw new IllegalArgumentException("Invalid saved "+label()+" edits/balance");
    }
    /** Names this job in reasons: "extension" for the 0.1.0 widening. */
    public abstract String label();
    /** Names this job in logs. */
    public String title(){return "Extension";}
    /** Upper bound on recorded removals. */
    public abstract int cap();
    /** Marks the owning saved data dirty. */
    public abstract void changed(ServerLevel l);
    /** Live integrity of the job's space. Observed faults beat unavailable cells (Findings). */
    public abstract void findings(ServerLevel l,UUID owner,Findings r);
    /** Cells around the planned work that must stay solid and dry: walls, roof and floor supports. */
    public abstract List<BlockPos> surfaces();
    /** A worker finds a planned cell replaced or no longer eligible. The widening waits (0.1.0); a job that stops
     * instead never removes that cell and releases its worker once the carried soil is delivered. */
    public boolean stopsOnIncompatibleTarget(){return false;}
    public boolean stopped(){return false;}
    /** Work stands beyond the founding chamber are approached through it: ground paths are bounded, and a stand
     * deep beyond the chamber would otherwise end on the ground above it. The widening sits beside the chamber. */
    public boolean viaFoundingChamber(){return false;}
    public void stop(ServerLevel l,String why){throw new UnsupportedOperationException(label()+" jobs wait instead of stopping");}
    /** A marker block the claimed worker still sets up once every removal is delivered, if one can be set up now. */
    public BlockPos pendingMarker(ServerLevel l,UUID owner){return null;}
    /** Sets up the pending marker block; the worker is in physical reach. */
    public boolean setUp(ServerLevel l,LasiusNigerEntity w){return false;}
    public String completionReason(ServerLevel l,UUID owner){return "completed_connected_"+label();}
    public int removed(){return completed.size();}
    public boolean complete(){return removed()==tasks.size()&&deposited+released==removed();}
    public List<BlockPos> completed(){return List.copyOf(completed);}
    public List<BlockPos> floors(){return tasks.stream().filter(p->p.getY()==home.entrance().getY()-2).toList();}
    public String problem(ServerLevel l,UUID owner){var r=new Findings();findings(l,owner,r);return r.problem();}
    /** A completed cell still opened by this colony's own worker: an authorized opening in a neighbouring shell. */
    public boolean opening(ServerLevel l,BlockPos p,UUID owner){return completed.contains(p)&&ColonyTerrain.get(l).opened(l,p,owner);}
    /** Records one successful worker removal, in plan order and below the cap. */
    public void removed(ServerLevel l,BlockPos p){
        if(removed()>=cap()||!tasks.get(removed()).equals(p))throw new IllegalStateException(label()+" order/cap");
        completed.add(p);changed(l);
    }
    /** A dead builder's carried soil went to transfer custody; the job waits for another real worker. */
    public void release(ServerLevel l,LasiusNigerEntity w,int soil){
        if(!w.getUUID().equals(claim))return;
        claim=null;released+=soil;reason="builder_dead_waiting_actual_worker";changed(l);
    }
    // Called per worker and per shell cell every tick: plain lookups, no allocation.
    /** The job a worker is claimed by, if any. Claims are exclusive: assignment requires no claim in any job. */
    public static DigJob claimedBy(ServerLevel l,LasiusNigerEntity w){
        var owner=w.queenId();if(owner==null)return null;
        var widening=NestExpansion.get(l).job(owner);if(widening!=null&&w.getUUID().equals(widening.claim))return widening;
        for(var j:ChamberExcavation.get(l).jobs(owner))if(w.getUUID().equals(j.claim))return j;
        return null;
    }
    public static boolean anyClaim(ServerLevel l,UUID owner){
        if(owner==null)return false;
        var widening=NestExpansion.get(l).job(owner);if(widening!=null&&widening.claim!=null)return true;
        for(var j:ChamberExcavation.get(l).jobs(owner))if(j.claim!=null)return true;
        return false;
    }
    /** An authorized opening of any of the colony's jobs. */
    public static boolean anyOpening(ServerLevel l,BlockPos p,UUID owner){
        if(owner==null)return false;
        var widening=NestExpansion.get(l).job(owner);if(widening!=null&&widening.opening(l,p,owner))return true;
        for(var j:ChamberExcavation.get(l).jobs(owner))if(j.opening(l,p,owner))return true;
        return false;
    }
    /** Loaded conflicts are resolved from the worker's canonical task/cargo. Missing lookup stays unknown. */
    public void reconcile(ServerLevel l,LasiusNigerEntity q){
        if(claim==null)return;
        if(!(l.getEntity(claim) instanceof LasiusNigerEntity w))return;
        if(!ColonyMembers.get(l).belongs(w,q.getUUID(),home.chamber())||w.workerTasks().plan()==null
            ||!w.workerTasks().plan().entrance().equals(home.entrance())||w.workerTasks().plan().direction()!=home.direction()){
            claim=null;reason="quarantined_foreign_builder_claim_cargo_and_edits_retained";changed(l);
            dev.primeants.PrimeAnts.LOGGER.error("Quarantined foreign construction owner queen={} worker={} actualQueen={}",q.getUUID(),w.getUUID(),w.queenId());return;
        }
        if(!w.workerTasks().construction()&&(w.getMainHandItem().is(net.minecraft.world.item.Items.DIRT)||removed()>deposited+released)){
            q.founding().releaseWorker(w);
            if(!reason.startsWith("quarantined_")){
                reason="quarantined_incompatible_task_with_unsettled_soil";changed(l);
                dev.primeants.PrimeAnts.LOGGER.error("Quarantined construction claim queen={} worker={} phase={} cargo={} removed={} deposited={} released={}",q.getUUID(),w.getUUID(),w.workerTasks().phase(),w.getMainHandItem(),removed(),deposited,released);
            }
            return;
        }
        if(q.founding().claimedBy(w)){
            if(w.workerTasks().construction())q.founding().releaseWorker(w);
            else {claim=null;reason="persisted_construction_claim_released_task_and_cargo_retained";changed(l);}
            dev.primeants.PrimeAnts.LOGGER.warn("Resolved overlapping role claims queen={} worker={} phase={} cargo={} removed={} deposited={}",q.getUUID(),w.getUUID(),w.workerTasks().phase(),w.getMainHandItem(),removed(),deposited);
        }
        if(claim!=null&&!w.workerTasks().construction()){
            // Never overwrite nursing/foraging or equipment to recover a historical claim.
            claim=null;reason="persisted_construction_claim_released_incompatible_task";changed(l);
            dev.primeants.PrimeAnts.LOGGER.warn("Released incompatible construction claim queen={} worker={} task={} cargo={}",q.getUUID(),w.getUUID(),w.workerTasks().phase(),w.getMainHandItem());
        }
    }
}
