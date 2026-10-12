package dev.primeants.founding;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.brood.BroodPile;
import dev.primeants.colony.ColonyStage;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.TransferCustody;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** The owner's finite two-relocation permission. No source search and no structural receipts or cargo copy. */
public final class SurfaceAccess {
    public enum Phase { SOURCE, CARRIED, CUSTODY, DEPOSITED, TERMINAL }
    public static final int CAP=2;
    private static final int[][] SOURCES={{-4,2,2},{-4,2,1}},DESTINATIONS={{-9,8,1},{-10,7,1}},
        REMOVAL_STANDS={{-3,1,2},{-5,2,1}},DEPOSIT_STANDS={{-8,8,1},{-9,6,1}};
    public static final class Operation {
        public final UUID unit; public final BlockPos source,destination,removalStand,depositStand;
        public Phase phase=Phase.SOURCE; public boolean removed,redeposited; public int pickups,deaths;
        public final List<UUID> releases=new ArrayList<>(); public final List<String> transitions=new ArrayList<>();
        Operation(UUID unit,NestPlan home,int index){this.unit=unit;source=at(home,SOURCES[index]);destination=at(home,DESTINATIONS[index]);removalStand=at(home,REMOVAL_STANDS[index]);depositStand=at(home,DEPOSIT_STANDS[index]);}
        public int carried(){return (removed?1:0)+pickups-(redeposited?1:0)-releases.size();}
        public UUID custody(){return releases.isEmpty()?null:releases.getLast();}
    }
    public final UUID owner,binding; public final List<String> prefix; public final List<Operation> operations;
    private Map<String,Integer> excavationBaseline=Map.of();
    public int index,action; public long lastActionTime=Long.MIN_VALUE; public String reason="admitted"; public boolean stopped;
    // Only a motivating-target UNKNOWN introduces this confirmation gate. No permission is inferred on restoration.
    private boolean targetPaused; private int targetConfirmations; private long targetEvaluationAt=Long.MIN_VALUE;
    SurfaceAccess(UUID owner,SurfaceWork.Job j){this(owner,j,j.receipts().subList(0,4));}
    private SurfaceAccess(UUID owner,SurfaceWork.Job j,List<String> prefix){
        this.owner=owner;this.prefix=List.copyOf(prefix);binding=identity(owner+":"+j.home.entrance().asLong()+":"+j.home.direction()+":"+j.plan.description()+":"+prefix);
        var ops=new ArrayList<Operation>();for(int n=0;n<CAP;n++)ops.add(new Operation(identity(binding+":unit:"+n),j.home,n));operations=List.copyOf(ops);
    }
    private static UUID identity(String s){return UUID.nameUUIDFromBytes(s.getBytes(StandardCharsets.UTF_8));}
    private static BlockPos at(NestPlan p,int[] xyz){return p.at(xyz[0],xyz[1],xyz[2]);}
    public boolean complete(){return index==CAP;}
    public Operation current(){return operations.get(Math.min(index,CAP-1));}
    public int removed(){return (int)operations.stream().filter(o->o.removed).count();}
    public int redeposited(){return (int)operations.stream().filter(o->o.redeposited).count();}
    public int pickups(){return operations.stream().mapToInt(o->o.pickups).sum();}
    public int released(){return operations.stream().mapToInt(o->o.releases.size()).sum();}
    public int carried(){return operations.stream().mapToInt(Operation::carried).sum();}
    public static ItemStack unitStack(UUID unit){var stack=new ItemStack(Items.DIRT);var tag=new CompoundTag();tag.putString("SurfaceAccessUnit",unit.toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
    public boolean cargo(LasiusNigerEntity w){return carried()==1&&ItemStack.matches(w.getMainHandItem(),unitStack(current().unit));}
    public boolean matches(SurfaceWork.Job j,UUID owner){return this.owner.equals(owner)&&j.completed()>=4&&prefix.equals(j.receipts().subList(0,4))&&binding.equals(new SurfaceAccess(owner,j,prefix).binding);}
    static boolean prefix(SurfaceWork.Job j){return j.plan.equals(SurfacePlan.bundled(ColonyStage.MATURE))&&j.completed()==4&&j.plan.cells().get(4).key().equals("-4,3,0")
        &&j.ground.values().stream().allMatch(y->y==0)&&j.recovered()==2&&j.placed()==2&&j.released()==0&&j.carried()==0
        &&j.receipts().stream().filter(r->r.startsWith("retained_paid_foundation:")).count()==2;}
    public static SurfaceAccess admit(ServerLevel l,SurfaceWork.Job j,UUID owner,LasiusNigerEntity worker){
        if(j.access!=null||j.claim!=null||!prefix(j)||!worker.workerTasks().canConstruct(j.home)||!j.unlocked(l,owner)
            ||ChamberUpgrade.get(l).awaitingTierTwo(l,owner,j.home)
            ||!ColonyTerrain.get(l).mound(l,j.next(),owner)||SurfaceWork.targetProblem(l,owner,j)!=null)return null;
        var a=new SurfaceAccess(owner,j);var t=ColonyTerrain.get(l);
        if(a.operations.stream().anyMatch(o->!t.mound(l,o.source,owner)||t.surface(l,o.source,owner)||!t.canReserveAccess(l,o.destination)||!destinationReady(l,j,owner,o.destination)
            ||!SurfaceWork.validStand(l,worker,o.source,false,o.removalStand)||!SurfaceWork.validStand(l,worker,o.destination,true,o.depositStand)))return null;
        if(!SurfaceWork.recoverable(l,j.home,owner,a.current().source))return null;
        long stock=MoundSoil.cells(l,j.home,owner).stream().filter(p->t.mound(l,p,owner)||t.surface(l,p,owner)).count();if(stock!=72)return null;
        // Admission witnesses normal routes, never writes waypoints or moves the body.
        for(var o:a.operations)for(var stand:List.of(o.removalStand,o.depositStand)){var path=worker.getNavigation().createPath(stand,0,32);if(path==null||!path.canReach())return null;}
        a.excavationBaseline=excavation(l,owner);
        for(var o:a.operations)if(!t.reserveAccess(l,o.destination,owner,o.unit))throw new IllegalStateException("Access admission reservation changed on server thread");
        return a;
    }
    private static Map<String,Integer> excavation(ServerLevel l,UUID owner){
        var result=new TreeMap<String,Integer>();
        if(l.getEntity(owner) instanceof LasiusNigerEntity q){result.put("founding_removed",q.founding().removed());result.put("founding_deposited",q.founding().deposited());result.put("founding_released",q.founding().released());}
        var widening=NestExpansion.get(l).job(owner);if(widening!=null){result.put("widening_removed",widening.removed());result.put("widening_deposited",widening.deposited);result.put("widening_released",widening.released);}
        for(var job:ChamberExcavation.get(l).jobs(owner)){result.put(job.room+"_removed",job.removed());result.put(job.room+"_deposited",job.deposited);result.put(job.room+"_released",job.released);}
        var mining=Mining.get(l).job(owner);if(mining!=null){result.put("mining_dirt_removed",mining.produced("minecraft:dirt"));result.put("mining_deposited",mining.deposited);result.put("mining_released",mining.released);}
        return Map.copyOf(result);
    }
    private static boolean destinationReady(ServerLevel l,SurfaceWork.Job j,UUID owner,BlockPos p){
        return NestPlan.loaded(l,p)&&l.getBlockState(p).equals(Blocks.AIR.defaultBlockState())&&l.getFluidState(p).isEmpty()&&l.getBlockEntity(p)==null
            &&MoundSoil.slots(l,j.home,owner,ColonyStage.MATURE).stream().anyMatch(s->p.equals(s.pos())&&s.state()==NestMound.Slot.FREE)
            &&MoundSoil.takes(l,p,owner)&&SupportSurvival.problem(l,p,NurseryBlocks.NEST_SOIL.defaultBlockState())==null;
    }
    /** Checks both original units against their current named locations; unavailable is never absence. */
    public String problem(ServerLevel l,SurfaceWork.Job j){
        if(stopped)return "stopped_"+reason;if(complete())return null;if(!matches(j,owner))return "prefix_or_plan_changed";
        var motivating=j.at(4);
        if(!NestPlan.loaded(l,motivating)){
            targetPaused=true;targetConfirmations=0;action=0;lastActionTime=Long.MIN_VALUE;
            targetEvaluationAt=l.getBlockEntity(j.home.nursery()) instanceof BroodPile pile?pile.loadedTicks():Long.MIN_VALUE;
            SurfaceWork.get(l).setDirty();return "unknown_motivating_target";
        }
        if(!ColonyTerrain.get(l).mound(l,motivating,owner))return "motivating_target_ownership_revoked";
        if(l.getEntity(owner) instanceof LasiusNigerEntity q){
            if(!q.isAlive())return "queen_dead";var home=q.founding().plan();if(home==null||!home.entrance().equals(j.home.entrance())||home.direction()!=j.home.direction())return "owner_home_changed";
            if(excavationBaseline.isEmpty()||!excavationBaseline.equals(excavation(l,owner)))return "excavation_history_changed";
        }
        var paid=j.paidProblem(l,owner);if(paid!=null)return paid;
        if(!j.unlocked(l,owner))return "stage_regressed";
        var t=ColonyTerrain.get(l);String unknown=null;
        for(var o:operations){
            boolean loaded=true;for(var p:List.of(o.source,o.destination,o.source.above(),o.destination.below()))for(var d:Direction.values())if(!NestPlan.loaded(l,p)||!NestPlan.loaded(l,p.relative(d)))loaded=false;
            if(!loaded){unknown="unknown_access_cells";continue;}
            if(o.removed?!t.accessOpen(l,o.source,owner,o.unit):!t.mound(l,o.source,owner))return "source_ownership_revoked";
            if(o.redeposited){if(!t.accessMound(l,o.destination,owner,o.unit))return "deposited_destination_revoked";}
            else if(!t.accessReservation(l,o.destination,owner,o.unit))return "destination_reservation_revoked";
            else if(!destinationReady(l,j,owner,o.destination))return "destination_blocked_or_unsupported";
        }
        if(unknown!=null)return unknown;
        if(targetPaused){
            if(l.getBlockEntity(j.home.nursery()) instanceof BroodPile pile&&pile.stageEvaluation()!=null&&pile.stageEvaluatedAt()>targetEvaluationAt){
                targetEvaluationAt=pile.stageEvaluatedAt();
                targetConfirmations=pile.operational()&&pile.stageEvaluation().stage().ordinal()>=ColonyStage.MATURE.ordinal()?targetConfirmations+1:0;
                SurfaceWork.get(l).setDirty();
            }
            if(targetConfirmations<2)return "unknown_motivating_target_fresh_confirmations";
            targetPaused=false;SurfaceWork.get(l).setDirty();
        }
        return null;
    }
    /** No material handoff occurred. Preserve all bound units/endpoints/reservations and named custody. */
    void emptyHandedDeath(SurfaceWork.Job j){
        if(carried()!=0)throw new IllegalStateException("Empty access cleanup cannot own cargo");
        action=0;lastActionTime=Long.MIN_VALUE;j.claim=null;
        if(current().phase==Phase.SOURCE){stopped=true;reason="empty_source_worker_dead";j.reason="stopped_access_"+reason;}
        else if(current().phase==Phase.CUSTODY){reason="empty_named_worker_dead";j.reason="access_"+reason;}
        else {stopped=true;reason="empty_access_worker_dead";j.reason="stopped_access_"+reason;}
    }
    public BlockPos target(ServerLevel l){return current().phase==Phase.SOURCE?current().source:current().phase==Phase.CARRIED?current().destination:
        l.getEntity(current().custody()) instanceof ItemEntity item?item.blockPosition():null;}
    public BlockPos stand(){return current().phase==Phase.SOURCE?current().removalStand:current().phase==Phase.CARRIED?current().depositStand:null;}
    public ItemEntity named(ServerLevel l){var id=current().custody();
        if(id==null||TransferCustody.get(l).contents().stream().anyMatch(p->p.id().equals(id)))return null;
        return l.getEntity(id) instanceof ItemEntity item&&!item.isRemoved()&&item.isAlive()
        &&NestPlan.loaded(l,item.blockPosition())&&l.isPositionEntityTicking(item.blockPosition())&&ItemStack.matches(item.getItem(),unitStack(current().unit))?item:null;}
    public Vec3 point(ServerLevel l,LasiusNigerEntity w){
        if(current().phase!=Phase.CUSTODY)return SurfaceWork.point(l,w,target(l),current().phase==Phase.CARRIED);
        return namedPoint(l,w,w.position());
    }
    private Vec3 namedPoint(ServerLevel l,LasiusNigerEntity w,Vec3 position){
        var item=named(l);if(item==null||item.hasPickUpDelay())return null;var mouth=position.add(0,0.25,0);var point=item.position().add(0,0.125,0);
        if(mouth.distanceToSqr(point)>5.0)return null;var delta=point.subtract(mouth);
        for(int n=0;n<=16;n++)if(!NestPlan.loaded(l,BlockPos.containing(mouth.add(delta.scale(n/16.0)))))return null;
        return l.clip(new net.minecraft.world.level.ClipContext(mouth,point,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.ANY,w)).getType()==HitResult.Type.MISS?point:null;
    }
    /** The ordinary bounded stand geometry/path queries, additionally checking the exact ground unit's low mouth ray. */
    public BlockPos pickupStand(ServerLevel l,LasiusNigerEntity w){
        var item=named(l);if(item==null||item.hasPickUpDelay())return null;var target=item.blockPosition();
        var original=SurfaceWork.stand(l,w,target,true);
        if(original!=null&&namedPoint(l,w,Vec3.atBottomCenterOf(original))!=null)return original;
        var stands=new ArrayList<BlockPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)for(int y=-2;y<=1;y++){
            var p=target.offset(x,y,z);if(SurfaceWork.validStand(l,w,target,true,p)&&namedPoint(l,w,Vec3.atBottomCenterOf(p))!=null)stands.add(p);
        }
        stands.sort(Comparator.<BlockPos>comparingDouble(p->w.position().distanceToSqr(Vec3.atBottomCenterOf(p))).thenComparingLong(BlockPos::asLong));
        for(var p:stands){var path=w.getNavigation().createPath(p,0,32);if(path!=null&&path.canReach()){
            PrimeAnts.LOGGER.info("T16 NAMED STAND queen={} worker={} unit={} entity={} itemPosition={} originalStand={} originalLowRay={} selectedStand={} selectedLowRay=true ordinaryPath=true",owner,w.getUUID(),current().unit,item.getUUID(),item.position(),original,original!=null&&namedPoint(l,w,Vec3.atBottomCenterOf(original))!=null,p);return p;
        }}return null;
    }
    public boolean pickupStandValid(ServerLevel l,LasiusNigerEntity w,BlockPos goal){var item=named(l);return item!=null&&SurfaceWork.validStand(l,w,item.blockPosition(),true,goal)&&namedPoint(l,w,Vec3.atBottomCenterOf(goal))!=null;}
    public boolean earn(ServerLevel l){
        long now=l.getGameTime();if(lastActionTime==now)return false;
        if(action>0&&lastActionTime+1!=now)action=0;lastActionTime=now;action++;return true;
    }
    public String actionProblem(ServerLevel l,SurfaceWork.Job j,LasiusNigerEntity w){
        var problem=problem(l,j);if(problem!=null)return problem;
        if(!NestPlan.loaded(l,w.blockPosition())||!l.isPositionEntityTicking(w.blockPosition())||!w.workerTasks().surfaceAuthorized(l))return "unknown_actor_authority";
        var o=current();if(o.phase==Phase.SOURCE){
            if(!w.getMainHandItem().isEmpty())return "incompatible_cargo";
            if(!SurfaceWork.recoverable(l,j.home,owner,o.source))return "source_not_recoverable";
        }else if(o.phase==Phase.CARRIED){if(!cargo(w))return "incompatible_cargo";}
        else if(o.phase==Phase.CUSTODY){if(o.pickups!=0||o.deaths!=1||!w.getMainHandItem().isEmpty())return "custody_cap_or_cargo";if(named(l)==null)return "unknown_named_custody";}
        else return "terminal_operation";
        var target=target(l);if(target==null)return "unknown_named_custody";
        if(!l.getFluidState(target).isEmpty()||l.getBlockEntity(target)!=null||w.isInWater()||!l.mayInteract(w,target))return "interaction_or_fluid_blocked";
        if(o.phase==Phase.CARRIED&&!l.getEntities(null,new AABB(target)).isEmpty())return "occupied_destination";
        return null;
    }
    /** Called only after the claimed actor earns twenty consecutive eligible loaded ticks. */
    public boolean commit(ServerLevel l,SurfaceWork.Job j,LasiusNigerEntity w){
        if(action!=SurfaceWork.ACTION_TICKS||lastActionTime!=l.getGameTime()||actionProblem(l,j,w)!=null||!w.onGround())return false;
        var o=current();var target=target(l);boolean empty=o.phase!=Phase.SOURCE;
        if(point(l,w)==null||w.getBoundingBox().intersects(new AABB(target))&&o.phase!=Phase.CUSTODY)return false;
        String transition;
        if(o.phase==Phase.SOURCE){
            if(!l.setBlock(o.source,Blocks.AIR.defaultBlockState(),3)||!NestPlan.loaded(l,o.source)||!l.getBlockState(o.source).equals(Blocks.AIR.defaultBlockState()))return false;ColonyTerrain.get(l).accessOpened(o.source,owner,o.unit);
            w.setItemSlot(EquipmentSlot.MAINHAND,unitStack(o.unit));o.removed=true;o.phase=Phase.CARRIED;transition="removed";
        }else if(o.phase==Phase.CARRIED){
            if(!MoundSoil.lay(l,o.destination,owner)||!ColonyTerrain.get(l).mound(l,o.destination,owner))return false;ColonyTerrain.get(l).accessDeposited(o.destination,owner,o.unit);
            w.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);o.redeposited=true;o.phase=Phase.DEPOSITED;index++;transition="redeposited";
        }else{
            var item=named(l);if(item==null||item.hasPickUpDelay())return false;
            w.setItemSlot(EquipmentSlot.MAINHAND,item.getItem());item.discard();o.pickups++;o.phase=Phase.CARRIED;transition="named_pickup";
        }
        o.transitions.add(transition+":"+w.getUUID()+":"+l.getGameTime()+":20");action=0;reason=transition;SurfaceWork.get(l).setDirty();
        PrimeAnts.LOGGER.info("T16 ACCESS ACTION queen={} worker={} operation={} unit={} transition={} source={} destination={} position={} stand={} actionTicks=20 removed={} pickups={} redeposited={} carried={} released={} structuralReceipts={} structuralPlaced={} excavationCredit=false history={}",owner,w.getUUID(),operations.indexOf(o)+1,o.unit,transition,o.source,o.destination,w.position(),w.blockPosition(),removed(),pickups(),redeposited(),carried(),released(),j.completed(),j.placed(),excavation(l,owner));return true;
    }
    public void released(ServerLevel l,SurfaceWork.Job j,LasiusNigerEntity w,UUID transfer,boolean death){
        var o=current();if(o.carried()!=1)throw new IllegalStateException("Access release without the canonical unit");
        o.releases.add(transfer);if(death)o.deaths++;o.phase=death&&o.deaths==1&&o.pickups==0?Phase.CUSTODY:Phase.TERMINAL;
        if(o.phase==Phase.TERMINAL){stopped=true;reason=death?"second_actual_carrier_death":"revoked_unit_in_named_custody";}
        else reason="first_actual_death_named_continuation";
        o.transitions.add("released:"+w.getUUID()+":"+transfer);j.claim=null;action=0;SurfaceWork.get(l).setDirty();
        PrimeAnts.LOGGER.info("T16 ACCESS RELEASE queen={} worker={} operation={} unit={} transfer={} deaths={} pickups={} released={} phase={} actualDeath={}",owner,w.getUUID(),index+1,o.unit,transfer,o.deaths,o.pickups,o.releases.size(),o.phase,death);
    }
    public JsonObject encode(){
        var o=new JsonObject();o.addProperty("cap",CAP);o.addProperty("owner",owner.toString());o.addProperty("binding",binding.toString());var p=new JsonArray();prefix.forEach(p::add);o.add("prefix",p);
        o.addProperty("index",index);o.addProperty("action",action);o.addProperty("lastActionTime",lastActionTime);o.addProperty("reason",reason);o.addProperty("stopped",stopped);var history=new JsonObject();excavationBaseline.forEach(history::addProperty);o.add("excavationBaseline",history);var ops=new JsonArray();
        if(targetPaused||targetConfirmations!=0||targetEvaluationAt!=Long.MIN_VALUE){o.addProperty("targetPaused",targetPaused);o.addProperty("targetConfirmations",targetConfirmations);o.addProperty("targetEvaluationAt",targetEvaluationAt);}
        for(var op:operations){var row=new JsonObject();row.addProperty("unit",op.unit.toString());row.addProperty("source",op.source.asLong());row.addProperty("destination",op.destination.asLong());
            row.addProperty("removalStand",op.removalStand.asLong());row.addProperty("depositStand",op.depositStand.asLong());row.addProperty("expectedSource","owned_mound_nest_soil");row.addProperty("expectedDestination","reserved_canonical_air");
            row.addProperty("phase",op.phase.name());row.addProperty("removed",op.removed);row.addProperty("redeposited",op.redeposited);row.addProperty("pickups",op.pickups);row.addProperty("deaths",op.deaths);
            var r=new JsonArray();op.releases.forEach(id->r.add(id.toString()));row.add("releases",r);var events=new JsonArray();op.transitions.forEach(events::add);row.add("transitions",events);ops.add(row);
        }o.add("operations",ops);return o;
    }
    public static SurfaceAccess decode(JsonObject o,SurfaceWork.Job j){
        var savedPrefix=new ArrayList<String>();o.getAsJsonArray("prefix").forEach(e->savedPrefix.add(e.getAsString()));
        if(savedPrefix.size()!=4||j.completed()<4||!savedPrefix.equals(j.receipts().subList(0,4)))throw new IllegalArgumentException("Access original prefix changed");
        var a=new SurfaceAccess(UUID.fromString(o.get("owner").getAsString()),j,savedPrefix);
        if(o.get("cap").getAsInt()!=CAP||!o.get("binding").getAsString().equals(a.binding.toString())||!o.getAsJsonArray("prefix").toString().equals(a.encode().getAsJsonArray("prefix").toString()))throw new IllegalArgumentException("Access owner/plan/prefix binding");
        a.index=o.get("index").getAsInt();a.action=o.get("action").getAsInt();a.lastActionTime=o.get("lastActionTime").getAsLong();a.reason=o.get("reason").getAsString();a.stopped=o.get("stopped").getAsBoolean();var ops=o.getAsJsonArray("operations");if(ops.size()!=CAP)throw new IllegalArgumentException("Access operation cap");
        if(o.has("targetPaused")){
            a.targetPaused=o.get("targetPaused").getAsBoolean();a.targetConfirmations=o.get("targetConfirmations").getAsInt();a.targetEvaluationAt=o.get("targetEvaluationAt").getAsLong();
        }
        var history=new TreeMap<String,Integer>();o.getAsJsonObject("excavationBaseline").entrySet().forEach(e->history.put(e.getKey(),e.getValue().getAsInt()));if(history.values().stream().anyMatch(n->n<0))throw new IllegalArgumentException("Access excavation snapshot");a.excavationBaseline=Map.copyOf(history);
        for(int n=0;n<CAP;n++){var row=ops.get(n).getAsJsonObject();var op=a.operations.get(n);
            if(!row.get("unit").getAsString().equals(op.unit.toString())||row.get("source").getAsLong()!=op.source.asLong()||row.get("destination").getAsLong()!=op.destination.asLong()
                ||row.get("removalStand").getAsLong()!=op.removalStand.asLong()||row.get("depositStand").getAsLong()!=op.depositStand.asLong()
                ||!row.get("expectedSource").getAsString().equals("owned_mound_nest_soil")||!row.get("expectedDestination").getAsString().equals("reserved_canonical_air"))throw new IllegalArgumentException("Access endpoints/identity changed");
            op.phase=Phase.valueOf(row.get("phase").getAsString());op.removed=row.get("removed").getAsBoolean();op.redeposited=row.get("redeposited").getAsBoolean();op.pickups=row.get("pickups").getAsInt();op.deaths=row.get("deaths").getAsInt();
            row.getAsJsonArray("releases").forEach(id->op.releases.add(UUID.fromString(id.getAsString())));row.getAsJsonArray("transitions").forEach(e->op.transitions.add(e.getAsString()));
        }a.validate();return a;
    }
    public void validate(){
        if(targetConfirmations<0||targetConfirmations>2||targetPaused&&action!=0)throw new IllegalArgumentException("Access target confirmation/action");
        if(index<0||index>CAP||action<0||action>=SurfaceWork.ACTION_TICKS||action>0&&lastActionTime<0||(stopped||complete())&&action!=0||carried()<0||carried()>1)throw new IllegalArgumentException("Access index/action/cargo");
        var releases=new HashSet<UUID>();for(int n=0;n<CAP;n++){var o=operations.get(n);
            if(o.pickups<0||o.pickups>1||o.deaths<0||o.deaths>2||o.releases.size()>2||o.pickups>o.deaths||o.carried()<0||o.carried()>1||o.redeposited&&!o.removed
                ||n<index&&!o.redeposited||n>index&&(o.removed||o.pickups!=0||!o.releases.isEmpty())||o.redeposited!=(o.phase==Phase.DEPOSITED)
                ||o.phase==Phase.SOURCE&&o.removed||o.phase==Phase.CARRIED&&o.carried()!=1||o.phase==Phase.CUSTODY&&(o.carried()!=0||o.deaths!=1||o.pickups!=0)
                ||o.phase==Phase.TERMINAL&&(!stopped||o.carried()!=0)||o.deaths==2&&o.phase!=Phase.TERMINAL||o.releases.stream().anyMatch(id->!releases.add(id))
                ||o.phase==Phase.SOURCE&&(o.pickups!=0||o.deaths!=0||!o.releases.isEmpty())||o.deaths>o.releases.size()||o.releases.size()>o.deaths+1
                ||o.phase==Phase.CARRIED&&(o.deaths!=o.pickups||o.releases.size()!=o.pickups)
                ||o.phase==Phase.DEPOSITED&&(n>=index||o.deaths!=o.pickups||o.releases.size()!=o.pickups)
                ||o.transitions.stream().filter(e->e.startsWith("removed:")).count()!=(o.removed?1:0)
                ||o.transitions.stream().filter(e->e.startsWith("redeposited:")).count()!=(o.redeposited?1:0)
                ||o.transitions.stream().filter(e->e.startsWith("named_pickup:")).count()!=o.pickups
                ||o.transitions.stream().filter(e->e.startsWith("released:")).count()!=o.releases.size())throw new IllegalArgumentException("Access unit history/location/cap");
        }
    }
}
