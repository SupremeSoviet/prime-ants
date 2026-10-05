package dev.primeants.founding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.ColonyMembers;
import dev.primeants.worker.WorkerTasks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.AABB;

/** One bounded circulation widening per colony. Plans, successful edits and live usable space are distinct. */
public final class NestExpansion extends SavedData {
    public static final int HARD_CAP=32, REMOVALS=12, RADIUS=6, DEPTH=3;
    public static final class Job {
        static final Codec<Job> CODEC=RecordCodecBuilder.create(i->i.group(
            BlockPos.CODEC.fieldOf("entrance").forGetter(j->j.home.entrance()),
            Codec.STRING.fieldOf("direction").forGetter(j->j.home.direction().getName()),
            Codec.INT.fieldOf("side").forGetter(j->j.side),
            BlockState.CODEC.listOf().fieldOf("expected").forGetter(j->j.expected),
            BlockPos.CODEC.listOf().fieldOf("completed").forGetter(j->List.copyOf(j.completed)),
            Codec.STRING.fieldOf("claim").forGetter(j->j.claim==null?"":j.claim.toString()),
            Codec.INT.fieldOf("deposited").forGetter(j->j.deposited),
            Codec.INT.fieldOf("released").forGetter(j->j.released),
            Codec.LONG.fieldOf("ticks").forGetter(j->j.ticks),
            Codec.STRING.fieldOf("reason").forGetter(j->j.reason),
            Codec.INT.fieldOf("trigger_workers").forGetter(j->j.triggerWorkers),
            Codec.STRING.fieldOf("used_by").forGetter(j->j.usedBy),
            Codec.STRING.fieldOf("use").forGetter(j->j.use)
        ).apply(i,Job::new));
        public final NestPlan home;
        public final int side,triggerWorkers;
        public final List<BlockPos> tasks;
        public final List<BlockState> expected;
        private final List<BlockPos> completed;
        public UUID claim;
        public int deposited,released;
        public long ticks;
        public String reason,usedBy,use;
        private Job(BlockPos entrance,String direction,int side,List<BlockState> expected,List<BlockPos> completed,String claim,int deposited,int released,long ticks,String reason,int triggerWorkers,String usedBy,String use){
            Direction d=Direction.byName(direction);if(d==null||d.getAxis().isVertical()||Math.abs(side)!=1)throw new IllegalArgumentException("Invalid extension home");
            home=NestPlan.geometry(entrance,d);this.side=side;tasks=targets(home,side);this.expected=List.copyOf(expected);this.completed=new ArrayList<>(completed);
            this.claim=claim.isEmpty()?null:UUID.fromString(claim);this.deposited=deposited;this.released=released;this.ticks=ticks;this.reason=reason;this.triggerWorkers=triggerWorkers;this.usedBy=usedBy;this.use=use;
            if(expected.size()!=REMOVALS||completed.size()>REMOVALS||!tasks.subList(0,completed.size()).equals(completed)||deposited<0||released<0||deposited+released>completed.size()||ticks<0)throw new IllegalArgumentException("Invalid saved extension edits/balance");
        }
        public int removed(){return completed.size();}
        public boolean complete(){return removed()==tasks.size()&&deposited+released==removed();}
        public List<BlockPos> completed(){return List.copyOf(completed);}
        public List<BlockPos> floors(){return tasks.stream().filter(p->p.getY()==home.entrance().getY()-2).toList();}
        public List<BlockPos> surfaces(){
            var out=new LinkedHashSet<BlockPos>();for(var p:tasks)for(var d:Direction.values()){var n=p.relative(d);if(!tasks.contains(n)&&!home.tasks().contains(n))out.add(n);}return List.copyOf(out);
        }
        public List<BlockPos> usable(ServerLevel l,UUID owner){
            if(problem(l,owner)!=null)return List.of();
            return floors().stream().filter(p->completed.contains(p)&&completed.contains(p.above())&&NestPlan.walkable(l,p)).toList();
        }
        public String problem(ServerLevel l,UUID owner){
            for(var p:tasks){
                if(!NestPlan.loaded(l,p))return "extension_chunk_unavailable";
                if(completed.contains(p)){if(!ColonyTerrain.get(l).opened(l,p,owner))return "extension_completed_opening_revoked";}
                else if(!l.getBlockState(p).isSolidRender()||!l.getFluidState(p).isEmpty())return "extension_unauthorized_pending_breach";
            }
            for(var p:surfaces())if(!NestPlan.loaded(l,p))return "extension_chunk_unavailable";
                else if(!l.getBlockState(p).isSolidRender()||!l.getFluidState(p).isEmpty())return "extension_shell_or_support_open";
            return null;
        }
    }
    public static final Codec<NestExpansion> CODEC=Codec.unboundedMap(Codec.STRING,Job.CODEC).xmap(NestExpansion::new,d->Map.copyOf(d.jobs));
    public static final SavedDataType<NestExpansion> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","nest_expansion"),NestExpansion::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,Job> jobs;
    public NestExpansion(){this(Map.of());}private NestExpansion(Map<String,Job> j){jobs=new HashMap<>(j);}
    public static NestExpansion get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public Job job(UUID owner){return owner==null?null:jobs.get(owner.toString());}
    private static List<BlockPos> targets(NestPlan p,int side){
        var out=new ArrayList<BlockPos>();for(int s:new int[]{2,3})for(int f:(s==2?new int[]{5,4,3}:new int[]{3,4,5}))for(int y=-2;y<=-1;y++)out.add(p.at(f,s*side,y));return List.copyOf(out);
    }
    public boolean opening(ServerLevel l,BlockPos p,UUID owner){var j=job(owner);return j!=null&&j.completed.contains(p)&&ColonyTerrain.get(l).opened(l,p,owner);}
    public String problem(ServerLevel l,UUID owner){var j=job(owner);return j==null?null:j.problem(l,owner);}
    public List<BlockPos> usable(ServerLevel l,UUID owner){var j=job(owner);return j==null?List.of():j.usable(l,owner);}
    public List<BlockPos> operationalSpace(ServerLevel l,UUID owner){var j=job(owner);return j!=null&&j.complete()?j.usable(l,owner):List.of();}
    /** A completed lower removal can temporarily fit an adult below its unchanged pending roof.
     * This is physical body containment only; usable space/emergence still require two completed air cells. */
    public List<BlockPos> bodyFloors(ServerLevel l,UUID owner,double bodyTop){
        var j=job(owner);if(j==null||j.problem(l,owner)!=null)return List.of();var terrain=ColonyTerrain.get(l);
        return j.floors().stream().filter(p->j.completed.contains(p)&&terrain.opened(l,p,owner)
            &&l.getBlockState(p.below()).isSolidRender()&&l.getFluidState(p.below()).isEmpty()
            &&(j.completed.contains(p.above())?terrain.opened(l,p.above(),owner)&&bodyTop<=p.getY()+2
                :bodyTop<=p.getY()+1&&terrain.eligible(l,p.above(),owner)&&l.getBlockState(p.above()).equals(j.expected.get(j.tasks.indexOf(p.above()))))).toList();
    }
    public void changed(){setDirty();}
    public void removed(Job j,BlockPos p){if(j.removed()>=HARD_CAP||!j.tasks.get(j.removed()).equals(p))throw new IllegalStateException("Extension order/cap");j.completed.add(p);setDirty();}
    public void release(UUID owner,LasiusNigerEntity w,int soil){var j=job(owner);if(j!=null&&w.getUUID().equals(j.claim)){j.claim=null;j.released+=soil;j.reason="builder_dead_waiting_actual_worker";setDirty();}}
    public void used(UUID owner,LasiusNigerEntity w,String use){var j=job(owner);if(j!=null&&j.usedBy.isEmpty()){j.usedBy=w.getUUID().toString();j.use=use;setDirty();}}
    public static List<BlockPos> deposits(NestPlan p){var out=new ArrayList<>(p.deposits());out.addAll(p.deposits().stream().map(BlockPos::above).toList());return List.copyOf(out);}
    public static boolean depositSupport(ServerLevel l,BlockPos p,UUID owner){return NestPlan.loaded(l,p)&&NestPlan.loaded(l,p.below())&&l.getFluidState(p).isEmpty()&&l.getBlockState(p.below()).isSolidRender()&&(NaturalSoil.get(l).eligible(l,p.below())||ColonyTerrain.get(l).mound(l,p.below(),owner));}
    private boolean candidate(ServerLevel l,Job j,UUID owner){
        var terrain=ColonyTerrain.get(l);
        for(int n=0;n<j.tasks.size();n++)if(!terrain.eligible(l,j.tasks.get(n),owner)||!l.getBlockState(j.tasks.get(n)).equals(j.expected.get(n)))return false;
        for(var p:j.surfaces())if(!terrain.eligible(l,p,owner)||!l.getFluidState(p).isEmpty())return false;
        // Existing footprint first; verified owned base layers can support another exterior layer.
        long capacity=deposits(j.home).stream().filter(p->depositSupport(l,p,owner)&&NestPlan.walkable(l,p)).count();
        return capacity>=REMOVALS&&j.problem(l,owner)==null;
    }
    public void consider(ServerLevel l,LasiusNigerEntity q,List<LasiusNigerEntity> workers){
        var p=q.founding().plan();if(p==null||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN||!q.founding().ready())return;
        var j=job(q.getUUID());
        if(j!=null&&j.claim!=null)return; // Absent/unloaded lookup never frees a claim.
        if(j!=null&&j.complete())return;
        var eligible=workers.stream().filter(w->!w.isCallow()&&!w.isNoAi()&&!q.founding().claimedBy(w)&&w.getMainHandItem().isEmpty()
                &&ColonyMembers.get(l).belongs(w,q.getUUID(),p.chamber())&&w.workerTasks().freeForConstruction()
                &&(!w.workerTasks().nursing()||workers.stream().filter(n->n.workerTasks().nursing()).count()>=3)).sorted(Comparator.comparing(w->w.getUUID().toString())).toList();
        if(workers.stream().filter(w->!w.isCallow()&&!w.isNoAi()).count()<4||eligible.isEmpty()||workers.stream().filter(w->w.workerTasks().nursing()).count()<2)return;
        // Four mature real bodies in a nine-cell chamber, with only seven empty floor cells, trigger one widening.
        if(j==null){
            for(int side:new int[]{1,-1}){
                var tasks=targets(p,side);var trial=new Job(p.entrance(),p.direction().getName(),side,tasks.stream().map(l::getBlockState).toList(),List.of(),"",0,0,0,"planned_mature_nursery_congestion",workers.size(),"","");
                if(candidate(l,trial,q.getUUID())){j=trial;jobs.put(q.getUUID().toString(),j);setDirty();break;}
            }
            if(j==null)return;
        }
        var w=eligible.getFirst();if(w.workerTasks().assignConstruction(p)){j.claim=w.getUUID();j.reason="living_empty_worker_assigned";setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("Extension assigned queen={} worker={} mature={} side={} tasks={} radius={} depth={} cap={} cadence={}",q.getUUID(),w.getUUID(),workers.size(),j.side,j.tasks.size(),RADIUS,DEPTH,HARD_CAP,QueenFounding.cadence());}
    }
}
