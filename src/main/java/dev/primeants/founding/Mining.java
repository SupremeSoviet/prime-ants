package dev.primeants.founding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.brood.BroodPile;
import dev.primeants.colony.ColonyStage;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;

/** One cumulative, bounded mining ledger per colony. Ordered successful edits and physical unit custody. */
public final class Mining extends SavedData {
    public static final class Job extends DigJob {
        static final Codec<Job> CODEC=RecordCodecBuilder.create(i->i.group(
            BlockPos.CODEC.fieldOf("entrance").forGetter(j->j.home.entrance()),
            Codec.STRING.fieldOf("direction").forGetter(j->j.home.direction().getName()),
            BlockState.CODEC.listOf().fieldOf("expected").forGetter(j->j.expected),
            BlockPos.CODEC.listOf().fieldOf("completed").forGetter(DigJob::completed),
            Codec.STRING.fieldOf("claim").forGetter(j->j.claim==null?"":j.claim.toString()),
            Codec.INT.fieldOf("deposited").forGetter(j->j.deposited),
            Codec.INT.fieldOf("released").forGetter(j->j.released),
            Codec.LONG.fieldOf("ticks").forGetter(j->j.ticks),
            Codec.STRING.fieldOf("reason").forGetter(j->j.reason),
            Codec.unboundedMap(Codec.STRING,Codec.INT).fieldOf("deliveries").forGetter(j->Map.copyOf(j.deliveries)),
            Codec.unboundedMap(Codec.STRING,Codec.STRING).fieldOf("release_transfers").forGetter(j->Map.copyOf(j.transfers)),
            BlockPos.CODEC.listOf().optionalFieldOf("targets",List.of()).forGetter(j->j.explicitTargets?j.tasks:List.of()),
            BlockPos.CODEC.listOf().optionalFieldOf("open_access",List.of()).forGetter(j->List.copyOf(j.openAccess))
        ).apply(i,Job::new));
        private final Map<String,Integer> deliveries;
        private final Map<String,String> transfers;
        private final boolean explicitTargets;
        private final Set<BlockPos> openAccess;
        private Job(BlockPos e,String direction,List<BlockState> expected,List<BlockPos> done,String claim,int deposited,int released,long ticks,String reason,Map<String,Integer> deliveries,Map<String,String> transfers,List<BlockPos> targets,List<BlockPos> openAccess){
            super(home(e,direction),targets.isEmpty()?targets(home(e,direction),expected.size()):targets,expected,done,claim,deposited,released,ticks,reason);
            explicitTargets=!targets.isEmpty();
            this.openAccess=Collections.unmodifiableSet(new LinkedHashSet<>(openAccess));
            if(this.openAccess.size()!=openAccess.size()||openAccess.stream().anyMatch(p->tasks.contains(p)||tasks.stream().noneMatch(t->t.distManhattan(p)==1)))throw new IllegalArgumentException("Invalid mining access");
            this.deliveries=new HashMap<>(deliveries);this.transfers=new HashMap<>(transfers);
            if(expected.size()>MiningShape.MAX_EDITS||!explicitTargets&&expected.size()%2!=0||expected.isEmpty()||deposited!=deliveries.values().stream().mapToInt(Integer::intValue).sum()||released!=transfers.size())throw new IllegalArgumentException("Invalid mining ledger");
            for(var p:tasks)if(!bounded(p))throw new IllegalArgumentException("Mining bounds");
            if(new HashSet<>(tasks).size()!=tasks.size())throw new IllegalArgumentException("Duplicate mining target");
            var connected=new HashSet<>(connections());
            for(var p:tasks){if(connected.stream().noneMatch(n->n.distManhattan(p)==1))throw new IllegalArgumentException("Disconnected mining target");connected.add(p);}
            for(var s:expected)if(unit(s)==null)throw new IllegalArgumentException("Unsupported mining target");
            var accounted=new HashMap<String,Integer>(deliveries);
            transfers.forEach((id,item)->{UUID.fromString(id);accounted.merge(item,1,Integer::sum);});
            for(var entry:accounted.entrySet())if(entry.getValue()<0||entry.getValue()>produced(entry.getKey()))throw new IllegalArgumentException("Invalid mining item balance");
        }
        private static NestPlan home(BlockPos e,String direction){var d=Direction.byName(direction);if(d==null||d.getAxis().isVertical())throw new IllegalArgumentException("Mining direction");return NestPlan.geometry(e,d);}
        private static List<BlockPos> targets(NestPlan p,int length){if(length>MiningShape.cells().size())throw new IllegalArgumentException("Gallery length");return MiningShape.cells().subList(0,length).stream().map(c->p.at(c.forward(),c.side(),c.dy())).toList();}
        public boolean bounded(BlockPos p){return MiningShape.within(p.getX()-home.entrance().getX(),p.getY()-home.entrance().getY(),p.getZ()-home.entrance().getZ());}
        public static String unit(BlockState s){return NaturalSoil.material(s)||s.is(dev.primeants.brood.NurseryBlocks.NEST_SOIL)?"minecraft:dirt":NaturalMaterials.unit(s);}
        public ItemStack nextUnit(){var id=unit(expected.get(removed()));return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));}
        public int produced(String item){int count=0;for(int n=0;n<removed();n++)if(item.equals(unit(expected.get(n))))count++;return count;}
        public Map<String,Integer> deliveries(){return Map.copyOf(deliveries);}
        public Map<String,String> transfers(){return Map.copyOf(transfers);}
        private boolean clayAccess(){return expected.subList(removed(),expected.size()).stream().allMatch(s->Set.of("minecraft:dirt","minecraft:clay_ball").contains(unit(s)));}
        public boolean pending(ItemStack cargo){return !cargo.isEmpty()&&cargo.getCount()==1&&removed()==deposited+released+1
            &&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cargo.getItem()).toString().equals(unit(expected.get(removed()-1)));}
        public void delivered(ServerLevel l,ItemStack cargo){if(!pending(cargo))throw new IllegalStateException("Mining delivery has no matching successful edit");String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cargo.getItem()).toString();deliveries.merge(id,1,Integer::sum);deposited++;changed(l);}
        public void released(ServerLevel l,LasiusNigerEntity w,ItemStack cargo,UUID transfer){
            if(!w.getUUID().equals(claim))return;
            if(removed()>deposited+released){if(!pending(cargo))throw new IllegalStateException("Mining release balance");transfers.put(transfer.toString(),net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cargo.getItem()).toString());released++;}
            claim=null;if(!stopped())reason="miner_dead_custody_retained";changed(l);
        }
        @Override public String label(){return "mining";}
        @Override public String title(){return "Mining";}
        @Override public int cap(){return MiningShape.MAX_EDITS;}
        @Override public void changed(ServerLevel l){get(l).setDirty();}
        @Override public boolean viaFoundingChamber(){return openAccess.isEmpty();}
        @Override public boolean stopsOnIncompatibleTarget(){return true;}
        @Override public boolean stopped(){return reason.startsWith("stopped_");}
        @Override public void stop(ServerLevel l,String why){reason="stopped_"+why;changed(l);}
        public Set<BlockPos> connections(){return openAccess.isEmpty()?Set.of(home.at(7,0,-2),home.at(7,0,-1)):openAccess;}
        @Override public List<BlockPos> surfaces(){var cells=new LinkedHashSet<BlockPos>();for(var p:tasks)for(var d:Direction.values()){var n=p.relative(d);if(!tasks.contains(n)&&!connections().contains(n))cells.add(n);}return List.copyOf(cells);}
        @Override public void findings(ServerLevel l,UUID owner,Findings r){
            var terrain=ColonyTerrain.get(l);
            DugSpace.scan(new DugSpace.Ground<BlockPos>(){
                public boolean loaded(BlockPos p){return openAccess.contains(p)?NestPlan.loaded(l,p):terrain.openingLoaded(l,p,owner);}
                public boolean open(BlockPos p){return openAccess.contains(p)?l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty():terrain.openingIntact(l,p,owner);}
                public boolean closed(BlockPos p){return l.getBlockState(p).isSolidRender()&&l.getFluidState(p).isEmpty();}
                public BlockPos face(BlockPos p,int i){return p.relative(Direction.values()[i]);}
            },completed,connections(),tasks,DugSpace.Labels.of("mining"),r);
            for(var p:connections())if(r.cell(openAccess.contains(p)?NestPlan.loaded(l,p):terrain.openingLoaded(l,p,owner),"mining_connection_unavailable")
                &&!(openAccess.contains(p)?l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty():terrain.openingIntact(l,p,owner)))r.fault("mining_connection_revoked");
        }
        public boolean compatible(ServerLevel l,UUID owner,BlockPos p,BlockState expected){
            return bounded(p)&&NestPlan.loaded(l,p)&&l.getBlockState(p).equals(expected)
                &&(ColonyTerrain.get(l).compatible(l,p,owner,expected)||NaturalMaterials.get(l).eligible(l,p));
        }
        public String removalProblem(ServerLevel l,UUID owner){
            if(removed()>=tasks.size()||removed()>=cap())return "mining_edit_budget";
            var p=tasks.get(removed());
            if(!NestPlan.loaded(l,p)||!NestPlan.loaded(l,p.above()))return "mining_target_unavailable";
            if(!compatible(l,owner,p,expected.get(removed())))return "mining_origin_or_state_revoked";
            // Vanilla FallingBlock.canSurvive does not encode its scheduled gravity. Never remove beneath one.
            if(l.getBlockState(p.above()).getBlock() instanceof FallingBlock)return "mining_falling_roof_unsafe";
            for(var face:Direction.values()){
                var neighbor=p.relative(face);if(!NestPlan.loaded(l,neighbor))return "mining_neighbors_unavailable";
                if(!l.getFluidState(neighbor).isEmpty())return "mining_fluid_neighbor";
                if(tasks.contains(neighbor)&&!completed.contains(neighbor)&&!l.getBlockState(neighbor).isSolidRender())return "mining_unauthorized_pending_breach";
            }
            // The complete declared gallery's floor/roof/shell is bounded, and must still support eventual access.
            for(var shell:surfaces())if(!NestPlan.loaded(l,shell))return "mining_support_unavailable";
                else if(!l.getBlockState(shell).isSolidRender()||!l.getFluidState(shell).isEmpty())return "mining_shell_or_support_changed";
            var problem=SupportSurvival.problem(l,p,Blocks.AIR.defaultBlockState());if(problem!=null)return problem;
            var cargo=nextUnit();var store=MaterialStore.confirmed(l,owner,home);
            if(store==null||!store.room(cargo.is(Items.DIRT)?new ItemStack(Items.COBBLESTONE):cargo))return "mining_store_unavailable_or_full";
            if(cargo.is(Items.DIRT)&&MoundSoil.free(l,home,owner).isEmpty())return "mining_mound_full";
            return problem(l,owner);
        }
    }
    public static final Codec<Mining> CODEC=Codec.unboundedMap(Codec.STRING,Job.CODEC).xmap(Mining::new,d->Map.copyOf(d.jobs));
    public static final SavedDataType<Mining> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","mining"),Mining::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,Job> jobs;
    private final Map<UUID,Long> nextAttempt=new HashMap<>();
    public Mining(){this(Map.of());}private Mining(Map<String,Job> jobs){this.jobs=new HashMap<>(jobs);}
    public static Mining get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public Job job(UUID owner){return owner==null?null:jobs.get(owner.toString());}
    public boolean opening(ServerLevel l,BlockPos p,UUID owner){var j=job(owner);return j!=null&&j.opening(l,p,owner);}
    public static boolean unlocked(ServerLevel l,NestPlan home){
        return NestPlan.loaded(l,home.nursery())&&l.getBlockEntity(home.nursery()) instanceof BroodPile pile&&pile.stageEvaluation()!=null
            &&(pile.stageEvaluation().stage()==ColonyStage.MATURE||pile.stageEvaluation().stage()==ColonyStage.GREAT);
    }
    /** Young colonies may only remove clay and permitted soil access; general mining retains its Mature gate. */
    public static boolean unlocked(ServerLevel l,NestPlan home,Job job){
        if(unlocked(l,home))return true;
        return NestPlan.loaded(l,home.nursery())&&l.getBlockEntity(home.nursery()) instanceof BroodPile pile
            &&pile.stageEvaluation()!=null&&pile.stageEvaluation().stage()==ColonyStage.YOUNG
            &&job.removed()<job.tasks.size()&&Set.of("minecraft:dirt","minecraft:clay_ball").contains(Job.unit(job.expected.get(job.removed())))
            &&job.clayAccess();
    }
    /** Promotion clay remains counted in paid walls, so it is not added a second time to the wall recipe. */
    private static boolean needsClay(ServerLevel l,UUID owner,NestPlan home){
        var colony=dev.primeants.colony.ChamberRegistry.get(l).colony(owner);var store=MaterialStore.confirmed(l,owner,home);
        if(colony==null||store==null)return false;
        var walls=new HashSet<BlockPos>();
        for(var chamber:colony.chambers())walls.addAll(ChamberUpgrade.walls(home,chamber));
        long paid=walls.stream().filter(p->ColonyTerrain.get(l).retainedClay(l,p,owner)).count()*NestWalls.CLAY_PER_CELL;
        return store.units(MaterialUnits.Material.CLAY)+paid<Math.max(16,walls.size()*NestWalls.CLAY_PER_CELL);
    }
    public void consider(ServerLevel l,LasiusNigerEntity q,List<LasiusNigerEntity> workers){
        var p=q.founding().plan();var owner=q.getUUID();long now=l.getGameTime();
        if(p==null||now<nextAttempt.getOrDefault(owner,Long.MIN_VALUE))return;
        nextAttempt.put(owner,now+100);
        var j=job(owner);if(j!=null&&(j.claim!=null||j.stopped()||j.reason.startsWith("quarantined_")))return;
        if(DigJob.anyClaim(l,owner))return;
        var widening=NestExpansion.get(l).job(owner);if(widening!=null&&!widening.complete())return;
        for(var dig:ChamberExcavation.get(l).jobs(owner))if(!dig.complete()&&!dig.stopped())return;
        // Active builders retain claim and cargo. An empty material gap admits the supplying miner.
        boolean tierTwoGap=ChamberUpgrade.get(l).jobs(owner).stream().anyMatch(upgrade->upgrade.tier==2&&!upgrade.complete()&&!upgrade.stopped());
        if(ChamberUpgrade.get(l).hasPriority(l,owner,p))return;
        var store=MaterialStore.confirmed(l,owner,p);if(store==null)return;
        if((j==null||j.complete())&&needsClay(l,owner,p)){
            var existing=j==null?List.<BlockPos>of():j.tasks;
            boolean bank=j!=null&&!j.openAccess.isEmpty();
            var access=bank?ClayRoute.bankAccess(l,p):Set.<BlockPos>of();
            var cells=bank?ClayRoute.next(l,owner,p,existing,access):ClayRoute.next(l,owner,p,existing);
            // New clay jobs may use a physical bank opening. Legacy jobs keep their original owned connection.
            if(j==null&&cells.isEmpty()){bank=true;access=ClayRoute.bankAccess(l,p);cells=ClayRoute.next(l,owner,p,existing,access);}
            if(!cells.isEmpty()){
                var positions=new ArrayList<BlockPos>(j==null?List.of():j.tasks);positions.addAll(cells);
                var states=new ArrayList<BlockState>(j==null?List.of():j.expected);cells.forEach(at->states.add(l.getBlockState(at)));
                var connections=new LinkedHashSet<BlockPos>(j==null?Set.of():j.openAccess);
                if(bank)for(var at:cells)for(var face:Direction.values()){
                    var neighbor=at.relative(face);if(!positions.contains(neighbor)&&NestPlan.loaded(l,neighbor)&&l.getBlockState(neighbor).isAir()&&l.getFluidState(neighbor).isEmpty())connections.add(neighbor);
                }
                var extended=new Job(p.entrance(),p.direction().getName(),states,j==null?List.of():j.completed(),"",j==null?0:j.deposited,j==null?0:j.released,j==null?0:j.ticks,"planned_physical_clay_access",j==null?Map.of():j.deliveries,j==null?Map.of():j.transfers,positions,List.copyOf(connections));
                if(unlocked(l,p,extended)&&extended.problem(l,owner)==null){j=extended;jobs.put(owner.toString(),j);setDirty();}
            }
        }
        if(j!=null&&j.complete())return;
        if(j==null){
            if(tierTwoGap)return; // A clay gap does not admit unrelated mineral production.
            if(!unlocked(l,p))return;
            var expected=new ArrayList<BlockState>();int lastResource=-1;
            var all=MiningShape.cells().stream().map(c->p.at(c.forward(),c.side(),c.dy())).toList();
            for(int n=0;n<all.size();n++){
                var at=all.get(n);if(!NestPlan.loaded(l,at))return;
                var state=l.getBlockState(at);
                if(!(ColonyTerrain.get(l).eligible(l,at,owner)||NaturalMaterials.get(l).eligible(l,at)))break;
                expected.add(state);if(NaturalMaterials.get(l).eligible(l,at))lastResource=n;
            }
            if(lastResource<0)return;int length=(lastResource/2+1)*2;if(expected.size()<length)return;
            j=new Job(p.entrance(),p.direction().getName(),List.copyOf(expected.subList(0,length)),List.of(),"",0,0,0,"planned_bounded_gallery",Map.of(),Map.of(),List.of(),List.of());
            // Protect all existing walls/components; only a natural/colony-soil corridor may connect here.
            for(var at:j.surfaces())if(!NestPlan.loaded(l,at)||!l.getBlockState(at).isSolidRender()||!l.getFluidState(at).isEmpty())return;
            if(j.problem(l,owner)!=null)return;
            jobs.put(owner.toString(),j);setDirty();
            dev.primeants.PrimeAnts.LOGGER.info("T09 MINING PLAN queen={} tasks={} radius={} depth={}-{} cap={}",owner,j.tasks.size(),MiningShape.RADIUS,MiningShape.MIN_DEPTH,MiningShape.MAX_DEPTH,MiningShape.MAX_EDITS);
        }
        if(tierTwoGap&&!j.clayAccess())return;
        if(!unlocked(l,p,j))return;
        if(j.removalProblem(l,owner)!=null)return;
        var w=workers.stream().filter(a->a.workerTasks().canConstruct(p)&&NestExpansion.remainingCaregivers(l,owner,p,a)>=2).sorted(Comparator.comparing(a->a.getUUID().toString())).findFirst().orElse(null);
        if(w!=null&&w.workerTasks().assignConstruction(p,"assigned_physical_mining")){j.claim=w.getUUID();j.reason="living_empty_miner_assigned";setDirty();}
    }
}
