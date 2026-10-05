package dev.primeants.worker;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.ColonyPlugs;
import dev.primeants.founding.NaturalSoil;
import dev.primeants.founding.NestPlan;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Sole movement/action owner for brood workers. Mutations run sequentially on the level's server thread. */
public final class WorkerTasks {
    public enum Phase { NURSERY, OPENING, SOIL_OUT, EXIT, SEARCH, APPROACH, RETURN, DEPOSIT, NURSE_CACHE, NURSE_FEED, NURSE_RETURN, DEAD }
    private final LasiusNigerEntity worker;
    private NestPlan plan;
    private Phase phase=Phase.NURSERY;
    private UUID source;
    private UUID recipient;
    private int feedingTicks;
    public static final int FEEDING_TICKS=20;
    public UUID recipientId(){return recipient;}
    public int feedingTicks(){return feedingTicks;}
    public boolean nursing(){return phase==Phase.NURSE_CACHE||phase==Phase.NURSE_FEED||phase==Phase.NURSE_RETURN;}
    private int opened, placed, phaseTicks, cooldown;
    private String reason="nursery_shelter";
    public WorkerTasks(LasiusNigerEntity worker) { this.worker=worker; }
    public Phase phase() { return phase; }
    public String reason() { return reason; }
    public int opened() { return opened; }
    public int placed() { return placed; }
    public NestPlan plan() { return plan; }
    public UUID sourceId() { return source; }
    public static boolean food(ItemStack s) { return !s.isEmpty()&&(s.is(Items.APPLE)||s.is(Items.SWEET_BERRIES)||s.is(Items.CHICKEN)); }
    public static boolean reaches(ServerLevel l,LasiusNigerEntity w,Vec3 target) {
        Vec3 mouth=w.position().add(0,0.25,0);
        return w.isAlive() && w.onGround() && !w.isInWater() && NestPlan.loaded(l,BlockPos.containing(target))
                && mouth.distanceToSqr(target)<=1.6 && l.clip(new ClipContext(mouth,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,w)).getType()==HitResult.Type.MISS;
    }
    private LasiusNigerEntity queen(ServerLevel l) { return worker.queenId()!=null && l.getEntity(worker.queenId()) instanceof LasiusNigerEntity q ? q:null; }
    public boolean authorized(ServerLevel l) {
        var q=queen(l);return !worker.isCallow() && !worker.isNoAi() && worker.isAlive() && plan!=null && worker.nurseryHome()!=null && worker.nurseryHome().equals(plan.chamber())
                && q!=null && q.isAlive() && q.founding().claimedBy(worker) && q.founding().plan()!=null
                && q.founding().plan().entrance().equals(plan.entrance()) && q.founding().plan().direction()==plan.direction() && q.founding().ready();
    }
    public boolean nursingAuthorized(ServerLevel l){
        return nursing()&&!worker.isCallow()&&!worker.isNoAi()&&worker.isAlive()&&!worker.isRemoved()&&plan!=null
                &&ColonyMembers.get(l).belongs(worker,worker.queenId(),plan.chamber())
                &&l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile p&&p.ownedBy(worker.queenId(),plan)&&p.operational()
                &&plan.nurseryProblem(l,worker.queenId(),true)==null;
    }
    public void assignNurse(NestPlan p){
        if(phase!=Phase.NURSERY||worker.isCallow())return;
        plan=NestPlan.geometry(p.entrance(),p.direction());next(Phase.NURSE_CACHE,"mature_member_nursing");
    }
    public void assign(NestPlan p) {
        if(phase==Phase.DEAD || worker.isCallow()||!worker.getMainHandItem().isEmpty()&&nursing())return;
        plan=NestPlan.geometry(p.entrance(),p.direction());
        phase=worker.getMainHandItem().is(Items.DIRT)?Phase.SOIL_OUT:food(worker.getMainHandItem())?Phase.RETURN:Phase.OPENING;phaseTicks=0;
    }
    private void next(Phase p,String why) { phase=p;phaseTicks=0;reason=why;worker.getNavigation().stop(); }
    private void hold(String why) { worker.getNavigation().stop(); if(!reason.equals(why)) {reason=why;PrimeAnts.LOGGER.info("Worker waits worker={} phase={} reason={} cargo={}",worker.getUUID(),phase,why,worker.getMainHandItem());} }
    private boolean arrive(Vec3 dest) {
        worker.getLookControl().setLookAt(dest.x,dest.y+0.2,dest.z);
        double distance=worker.position().distanceToSqr(dest);
        if(distance<0.01 && worker.onGround()) {worker.getNavigation().stop();return true;}
        if(distance<0.8 && Math.abs(worker.getY()-dest.y)<0.3 && worker.onGround()) {worker.getNavigation().stop();worker.getMoveControl().setWantedPosition(dest.x,dest.y,dest.z,1.0);}
        else if(worker.tickCount%20==0 || worker.getNavigation().isDone())worker.getNavigation().moveTo(dest.x,dest.y,dest.z,0,1.0);
        return false;
    }
    private boolean insideWorkStand() {
        Vec3 relative=worker.position().subtract(Vec3.atBottomCenterOf(plan.entrance()));
        double forward=relative.x*plan.direction().getStepX()+relative.z*plan.direction().getStepZ();
        Direction side=plan.direction().getClockWise();double lateral=relative.x*side.getStepX()+relative.z*side.getStepZ();
        int lane=lateral<0?-1:1;
        // Approach along a side row, then the throat. The queen occupies the central nursery cell;
        // a shortest central path would physically shove her through the opening and invalidate readiness.
        if(forward>3.25) {
            if(Math.abs(lateral)<0.85) {arrive(Vec3.atBottomCenterOf(plan.at(5,lane,-2)));return false;}
            arrive(Vec3.atBottomCenterOf(plan.at(3,lane,-2)));return false;
        }
        return arrive(Vec3.atBottomCenterOf(plan.at(3,0,-2)));
    }
    public void tick(ServerLevel l) {
        if(phase==Phase.DEAD || !worker.isAlive())return;
        if(worker.isCallow() || phase==Phase.NURSERY) {hold(worker.isCallow()?"callow_shelter":"nursery_shelter");return;}
        if(!(nursing()?nursingAuthorized(l):authorized(l))) {hold("home_unavailable_or_invalid_cargo_retained");feedingTicks=0;return;}
        phaseTicks++; if(cooldown>0)cooldown--;
        if(phaseTicks>1200) {
            if(phase==Phase.OPENING || phase==Phase.SOIL_OUT) {hold("opening_route_stalled_retry_no_remote_completion");phaseTicks=0;cooldown=40;}
            else if(nursing()){hold("nursing_route_stalled_cargo_retained");phaseTicks=0;cooldown=40;}
            else if(worker.getMainHandItem().isEmpty()) {source=null;next(Phase.RETURN,"trip_limit_return");}
            else {hold("physical_route_stalled_cargo_retained_retry");phaseTicks=0;cooldown=40;}
        }
        if(cooldown>0)return;
        switch(phase) {
            case OPENING -> open(l);
            case SOIL_OUT -> soilOut(l);
            case EXIT -> { if(arrive(Vec3.atBottomCenterOf(plan.outside())))next(Phase.SEARCH,"outside_search"); }
            case SEARCH -> search(l);
            case APPROACH -> pickup(l);
            case RETURN -> { if(arrive(Vec3.atBottomCenterOf(plan.at(3,0,-2))))next(Phase.DEPOSIT,"inside_delivery"); }
            case DEPOSIT -> deposit(l);
            case NURSE_CACHE -> nurseCache(l);
            case NURSE_FEED -> nurseFeed(l);
            case NURSE_RETURN -> nurseReturn(l);
            default -> { }
        }
    }
    private UUID chooseRecipient(ServerLevel l,ItemStack s){
        if(l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile p){
            var larva=p.records().stream().filter(r->p.accepts(r,s)).findFirst();if(larva.isPresent())return larva.get().id();
        }
        var q=queen(l);return q!=null&&q.isAlive()&&!q.isNoAi()&&q.founding().ready()&&q.acceptsFood(s)?q.getUUID():null;
    }
    public boolean hasRecipient(ServerLevel l,ItemStack s){return nursingAuthorized(l)&&chooseRecipient(l,s)!=null;}
    private void nurseCache(ServerLevel l){
        if(!worker.getMainHandItem().isEmpty()){next(Phase.NURSE_RETURN,"existing_nurse_cargo_retained");return;}
        if(!(l.getBlockEntity(plan.cache()) instanceof NestCache cache)||!cache.ownedBy(worker.queenId(),plan)){hold("nurse_owned_cache_unavailable");return;}
        if(cache.contents().stream().noneMatch(s->hasRecipient(l,s))){hold("nurse_no_food_or_accepting_recipient");return;}
        var target=Vec3.atBottomCenterOf(plan.cache()).add(0,0.15,0);
        if(!reaches(l,worker,target)){arrive(Vec3.atBottomCenterOf(plan.at(4,-1,-2)));return;}
        if(cache.withdraw(worker,plan)){
            recipient=chooseRecipient(l,worker.getMainHandItem());feedingTicks=0;next(Phase.NURSE_FEED,"cache_food_in_nurse_mandibles");
            PrimeAnts.LOGGER.info("Nurse withdrawal nurse={} cargo={} recipient={} position={}",worker.getUUID(),worker.getMainHandItem(),recipient,worker.position());
        }
    }
    private void nurseFeed(ServerLevel l){
        if(!food(worker.getMainHandItem())){hold("nurse_cargo_invalid_retained");return;}
        // Revalidate the exact recipient; a changed/refusing recipient never consumes cargo.
        var q=queen(l);boolean toQueen=recipient!=null&&recipient.equals(worker.queenId());
        var p=l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile b?b:null;
        boolean accepts=toQueen?q!=null&&q.isAlive()&&!q.isNoAi()&&q.founding().ready()&&q.acceptsFood(worker.getMainHandItem()):p!=null&&p.records().stream().anyMatch(r->r.id().equals(recipient)&&p.accepts(r,worker.getMainHandItem()));
        if(!accepts){feedingTicks=0;next(Phase.NURSE_RETURN,"recipient_refused_food_retained");return;}
        Vec3 target=toQueen?q.position().add(0,0.25,0):Vec3.atBottomCenterOf(plan.nursery()).add(0,0.15,0);
        if(!reaches(l,worker,target)){
            feedingTicks=0;
            // Walk around the queen along the rear row instead of pushing through her body.
            var delta=worker.position().subtract(Vec3.atBottomCenterOf(plan.entrance()));double forward=delta.x*plan.direction().getStepX()+delta.z*plan.direction().getStepZ();
            if(!toQueen&&forward<4.8)arrive(Vec3.atBottomCenterOf(plan.at(5,-1,-2)));
            else arrive(Vec3.atBottomCenterOf(plan.at(toQueen?4:5,toQueen?-1:1,-2)));
            return;
        }
        worker.getNavigation().stop();worker.getLookControl().setLookAt(target.x,target.y,target.z);reason="physical_feeding";
        if(++feedingTicks<FEEDING_TICKS)return;
        boolean accepted=toQueen?q.feedBy(worker,plan):p.feedBy(worker,recipient);
        feedingTicks=0;if(accepted){recipient=null;next(Phase.NURSE_CACHE,"carried_food_consumed_once");cooldown=20;}else next(Phase.NURSE_RETURN,"feeding_action_refused_cargo_retained");
    }
    private void nurseReturn(ServerLevel l){
        if(worker.getMainHandItem().isEmpty()){recipient=null;next(Phase.NURSE_CACHE,"nurse_empty");return;}
        // Another recipient may now accept, but the nurse must still walk to it before feeding.
        var nextRecipient=chooseRecipient(l,worker.getMainHandItem());if(nextRecipient!=null){recipient=nextRecipient;feedingTicks=0;next(Phase.NURSE_FEED,"nurse_recipient_reselected");return;}
        if(!(l.getBlockEntity(plan.cache()) instanceof NestCache cache)){hold("nurse_return_cache_unavailable_cargo_retained");return;}
        if(!reaches(l,worker,Vec3.atBottomCenterOf(plan.cache()).add(0,0.15,0))){arrive(Vec3.atBottomCenterOf(plan.at(4,-1,-2)));return;}
        if(cache.deposit(worker,plan)){recipient=null;next(Phase.NURSE_CACHE,"nurse_food_physically_returned");cooldown=40;}else hold("nurse_return_refused_cargo_retained");
    }
    private void open(ServerLevel l) {
        var plugs=ColonyPlugs.get(l);
        var pending=plan.plugs().reversed().stream().filter(p->!plugs.opened(l,p,worker.queenId())).toList();
        if(pending.isEmpty()) {queen(l).founding().opened();next(Phase.EXIT,"authorized_exit_open");return;}
        BlockPos target=pending.getFirst();
        if(!plugs.owned(l,target,worker.queenId())) {hold("plug_ownership_missing_or_revoked");return;}
        if(!worker.getMainHandItem().isEmpty()) {hold("opening_cargo_conflict");return;}
        if(!insideWorkStand())return;
        if(!authorized(l) || !plugs.owned(l,target,worker.queenId()) || worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0
                || !NestPlan.walkable(l,plan.at(3,0,-2)) || !l.getEntities(worker,new AABB(target)).isEmpty()) {hold("plug_action_revalidation_failed");return;}
        if(plugs.remove(l,target,worker.queenId())) {
            worker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIRT));opened++;cooldown=10;next(Phase.SOIL_OUT,"recovered_owned_plug");
        }
    }
    private void soilOut(ServerLevel l) {
        if(!worker.getMainHandItem().is(Items.DIRT)||worker.getMainHandItem().getCount()!=1) {hold("soil_cargo_invalid");return;}
        BlockPos target=null;Vec3 dest=null;
        for(BlockPos p:plan.deposits()) {
            if(!NestPlan.loaded(l,p) || !l.getBlockState(p).isAir() || !l.getBlockState(p.below()).isSolidRender() || !NaturalSoil.get(l).eligible(l,p.below()) || !l.getFluidState(p).isEmpty() || !l.getEntities(worker,new AABB(p)).isEmpty())continue;
            for(Direction d:Direction.Plane.HORIZONTAL) {
                BlockPos stand=p.relative(d);if(!NestPlan.walkable(l,stand))continue;
                Vec3 v=Vec3.atBottomCenterOf(stand).add(0.35*d.getStepX(),0,0.35*d.getStepZ());
                if(!l.noCollision(worker,worker.getBoundingBox().move(v.subtract(worker.position()))))continue;
                target=p;dest=v;break;
            }
            if(target!=null)break;
        }
        if(target==null) {hold("mound_full_or_blocked_soil_retained");cooldown=40;return;}
        if(!arrive(dest))return;
        if(!authorized(l)||!l.getBlockState(target).isAir()||!NaturalSoil.get(l).eligible(l,target.below())||!l.getEntities(worker,new AABB(target)).isEmpty()
                || worker.getBoundingBox().intersects(new AABB(target)) || worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0) {hold("mound_revalidation_failed");return;}
        if(l.setBlock(target,NurseryBlocks.NEST_SOIL.defaultBlockState(),3)) {worker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);placed++;cooldown=10;next(Phase.OPENING,"plug_soil_on_mound");}
    }
    private void search(ServerLevel l) {
        if(phaseTicks>240) {next(Phase.RETURN,"bounded_search_finished");cooldown=40;return;}
        if(worker.tickCount%20!=0)return;
        var candidates=l.getEntitiesOfClass(ItemEntity.class,new AABB(plan.outside()).inflate(10,3,10),
                i->i.isAlive() && !i.isRemoved() && !i.hasPickUpDelay() && food(i.getItem()) && NestPlan.loaded(l,i.blockPosition()));
        candidates.sort(java.util.Comparator.comparingDouble(i->worker.distanceToSqr(i)));
        for(var item:candidates) {
            if(!NestPlan.walkable(l,item.blockPosition()))continue;
            var path=worker.getNavigation().createPath(item.blockPosition(),0);
            if(path==null || !path.canReach())continue;
            source=item.getUUID();next(Phase.APPROACH,"supported_dropped_food_found");return;
        }
    }
    private void pickup(ServerLevel l) {
        var entity=source==null?null:l.getEntity(source);
        if(!(entity instanceof ItemEntity item) || !item.isAlive()||item.isRemoved()||item.hasPickUpDelay()||!food(item.getItem())||!NestPlan.loaded(l,item.blockPosition())) {source=null;next(Phase.SEARCH,"source_unavailable");return;}
        if(!worker.getMainHandItem().isEmpty()) {next(Phase.RETURN,"existing_cargo_return");return;}
        Vec3 target=item.position().add(0,0.1,0);
        if(!reaches(l,worker,target)) {arrive(Vec3.atBottomCenterOf(item.blockPosition()));return;}
        if(!authorized(l) || !item.isAlive() || item.hasPickUpDelay() || !food(item.getItem()) || !reaches(l,worker,target))return;
        var remaining=item.getItem().copy();var cargo=remaining.split(1);
        item.setItem(remaining);if(remaining.isEmpty())item.discard();
        worker.setItemSlot(EquipmentSlot.MAINHAND,cargo);source=null;next(Phase.RETURN,"physical_food_in_mandibles");
        PrimeAnts.LOGGER.info("Worker pickup worker={} item={} position={}",worker.getUUID(),cargo,worker.position());
    }
    private void deposit(ServerLevel l) {
        if(worker.getMainHandItem().isEmpty()) {next(Phase.EXIT,"next_bounded_trip");cooldown=40;return;}
        if(!food(worker.getMainHandItem())) {hold("unsupported_cargo_retained");return;}
        BlockPos p=plan.cache();
        if(!reaches(l,worker,Vec3.atBottomCenterOf(p).add(0,0.15,0))) {arrive(Vec3.atBottomCenterOf(plan.at(3,-1,-2)).add(0.65,0,0));return;}
        if(l.getBlockState(p).isAir() && l.getBlockState(p.below()).isSolidRender() && l.getFluidState(p).isEmpty() && authorized(l)) {
            if(l.setBlock(p,NurseryBlocks.NEST_CACHE.defaultBlockState(),3) && l.getBlockEntity(p) instanceof NestCache cache)cache.establish(worker,plan);
        }
        if(l.getBlockEntity(p) instanceof NestCache cache && cache.deposit(worker,plan)) {
            PrimeAnts.LOGGER.info("Worker delivery worker={} cache={} stored={}",worker.getUUID(),p,cache.contents());next(Phase.EXIT,"food_physically_stored");cooldown=40;
        } else {hold("cache_full_blocked_or_foreign_cargo_retained");cooldown=40;}
    }
    public void die(ServerLevel l) {
        if(phase==Phase.DEAD)return;worker.getNavigation().stop();
        if(!worker.getMainHandItem().isEmpty()) {
            var transfer=UUID.nameUUIDFromBytes(("worker-cargo:"+worker.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            TransferCustody.get(l).take(transfer,"worker:"+worker.getUUID(),worker.position(),worker.getMainHandItem());
            worker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            TransferCustody.get(l).retry(l);
        }
        var q=queen(l);if(q!=null)q.founding().releaseWorker(worker);
        ColonyMembers.get(l).died(worker);
        source=null;next(Phase.DEAD,"worker_dead_no_replacement");
    }
    public void save(ValueOutput out) {
        out.putString("Phase",phase.name());out.putString("Reason",reason);out.putInt("Opened",opened);out.putInt("Placed",placed);out.putInt("PhaseTicks",phaseTicks);out.putInt("Cooldown",cooldown);
        if(plan!=null) {out.store("Entrance",BlockPos.CODEC,plan.entrance());out.putString("Direction",plan.direction().getName());}
        if(source!=null)out.putString("Source",source.toString());
        if(recipient!=null)out.putString("Recipient",recipient.toString());out.putInt("FeedingTicks",feedingTicks);
        // Cargo is canonical vanilla Mob mainhand equipment, not duplicated here.
    }
    public void load(ValueInput in) {
        phase=Phase.valueOf(in.getStringOr("Phase","NURSERY"));reason=in.getStringOr("Reason","restored");opened=in.getIntOr("Opened",0);placed=in.getIntOr("Placed",0);phaseTicks=in.getIntOr("PhaseTicks",0);cooldown=in.getIntOr("Cooldown",0);
        source=in.getString("Source").map(UUID::fromString).orElse(null);
        recipient=in.getString("Recipient").map(UUID::fromString).orElse(null);feedingTicks=in.getIntOr("FeedingTicks",0);
        if(in.read("Entrance",BlockPos.CODEC).isPresent()) {Direction d=Direction.byName(in.getStringOr("Direction",""));if(d==null||d.getAxis().isVertical())throw new IllegalArgumentException("Invalid task home");plan=NestPlan.geometry(in.read("Entrance",BlockPos.CODEC).orElseThrow(),d);}
        if(opened<0||opened>2||placed<0||placed>opened||phaseTicks<0||cooldown<0||feedingTicks<0||feedingTicks>=FEEDING_TICKS)throw new IllegalArgumentException("Invalid worker progress");
    }
}
