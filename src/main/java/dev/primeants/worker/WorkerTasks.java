package dev.primeants.worker;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.ChamberExcavation;
import dev.primeants.founding.ChamberUpgrade;
import dev.primeants.founding.NestWalls;
import dev.primeants.founding.ColonyPlugs;
import dev.primeants.founding.DigJob;
import dev.primeants.founding.NaturalSoil;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.NestExpansion;
import dev.primeants.founding.QueenFounding;
import dev.primeants.founding.ColonyTerrain;
import java.util.UUID;
import java.util.List;
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
    public enum Phase { NURSERY, OPENING, SOIL_OUT, EXIT, SEARCH, APPROACH, NECTAR_APPROACH, HARVEST, RETURN, DEPOSIT, NURSE_CACHE, NURSE_FEED, NURSE_RETURN, DIG, DIG_OUT, DEAD, UPGRADE_FETCH, UPGRADE_BUILD }
    private final LasiusNigerEntity worker;
    private final CropSharing sharing;
    public CropSharing sharing(){return sharing;}
    private boolean defending;
    private UUID defenseTarget;
    private int biteCooldown,biteTicks;
    private long defenseLastTick=Long.MIN_VALUE,bites;
    public boolean defending(){return defending;}public UUID defenseTarget(){return defenseTarget;}
    public int biteCooldown(){return biteCooldown;}public long bites(){return bites;}
    private final java.util.ArrayList<UUID> droppedQueue=new java.util.ArrayList<>();
    private int droppedCursor;
    public static final int DROPPED_TOTAL=96, DROPPED_PER_PULSE=8;
    private NestPlan plan;
    private Phase phase=Phase.NURSERY;
    private UUID source;
    private BlockPos flowerSource,flowerStand;
    private String flowerExpected;
    private int harvestingTicks,searchTicks,flowerInspections;
    public static final int SEARCH_RADIUS=24, FLOWER_INSPECTIONS_PER_PULSE=1536, FLOWER_INSPECTION_BUDGET=49*49*7;
    private static final List<BlockPos> FLOWER_OFFSETS=flowerOffsets();
    private static List<BlockPos> flowerOffsets(){
        var result=new java.util.ArrayList<BlockPos>();for(int x=-SEARCH_RADIUS;x<=SEARCH_RADIUS;x++)for(int z=-SEARCH_RADIUS;z<=SEARCH_RADIUS;z++)for(int y=-3;y<=3;y++)result.add(new BlockPos(x,y,z));
        result.sort(java.util.Comparator.comparingInt((BlockPos p)->p.getX()*p.getX()+p.getZ()*p.getZ()).thenComparingInt(p->Math.abs(p.getY())).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getY));return List.copyOf(result);
    }
    public BlockPos flowerSource(){return flowerSource;}
    public int harvestingTicks(){return harvestingTicks;}
    public int flowerInspections(){return flowerInspections;}
    public boolean withinSearch(BlockPos p){return plan!=null&&Math.abs(p.getX()-plan.outside().getX())<=SEARCH_RADIUS&&Math.abs(p.getZ()-plan.outside().getZ())<=SEARCH_RADIUS&&Math.abs(p.getY()-plan.outside().getY())<=3;}
    private UUID recipient;
    private int feedingTicks;
    public static final int FEEDING_TICKS=20;
    public UUID recipientId(){return recipient;}
    public int feedingTicks(){return feedingTicks;}
    public boolean nursing(){return phase==Phase.NURSE_CACHE||phase==Phase.NURSE_FEED||phase==Phase.NURSE_RETURN;}
    public boolean foraging(){return switch(phase){case OPENING,SOIL_OUT,EXIT,SEARCH,APPROACH,NECTAR_APPROACH,HARVEST,RETURN,DEPOSIT->true;default->false;};}
    private int opened, placed, phaseTicks, cooldown;
    private String reason="nursery_shelter";
    public WorkerTasks(LasiusNigerEntity worker) { this.worker=worker;this.sharing=new CropSharing(worker); }
    public Phase phase() { return phase; }
    public String reason() { return reason; }
    public int opened() { return opened; }
    public int placed() { return placed; }
    public NestPlan plan() { return plan; }
    public UUID sourceId() { return source; }
    public static boolean food(ItemStack s) { return !s.isEmpty()&&(s.is(Items.APPLE)||s.is(Items.SWEET_BERRIES)||s.is(Items.CHICKEN)||s.is(Items.ROTTEN_FLESH)||s.is(dev.primeants.item.AntItems.FLOWER_NECTAR)||s.is(dev.primeants.item.AntItems.FLOWER_NECTAR_V2)||s.is(dev.primeants.item.AntItems.SMALL_PREY)); }
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
                &&!constructionClaim(l)&&!foragerClaim(l)&&!upgradeClaim(l)
                &&ColonyMembers.get(l).belongs(worker,worker.queenId(),plan.chamber())
                &&l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile p&&p.ownedBy(worker.queenId(),plan)&&p.operational()
                &&plan.nurseryProblem(l,worker.queenId(),true)==null;
    }
    public boolean freeForConstruction(){return !defending&&!sharing.busy()&&(phase==Phase.NURSERY||phase==Phase.NURSE_CACHE);}
    private boolean constructionClaim(ServerLevel l){return DigJob.claimedBy(l,worker)!=null;}
    private boolean upgradeClaim(ServerLevel l){return ChamberUpgrade.get(l).claimedBy(worker)!=null;}
    private boolean foragerClaim(ServerLevel l){var q=queen(l);return q!=null&&q.founding().claimedBy(worker);}
    private boolean eligible(ServerLevel l,NestPlan p){
        var q=queen(l);return !defending&&p!=null&&worker.isAlive()&&!worker.isRemoved()&&!worker.isCallow()&&!worker.isNoAi()
            &&ColonyMembers.get(l).belongs(worker,worker.queenId(),p.chamber())&&q!=null&&q.isAlive()&&q.founding().ready()
            &&q.founding().plan()!=null&&q.founding().plan().entrance().equals(p.entrance())&&q.founding().plan().direction()==p.direction();
    }
    public boolean canForage(NestPlan p){return worker.level() instanceof ServerLevel l&&eligible(l,p)&&freeForConstruction()
        &&worker.getMainHandItem().isEmpty()&&!constructionClaim(l)&&queen(l).founding().workerClaim()==null;}
    public boolean canConstruct(NestPlan p){return worker.level() instanceof ServerLevel l&&eligible(l,p)&&freeForConstruction()
        &&worker.getMainHandItem().isEmpty()&&!constructionClaim(l)&&!foragerClaim(l)
        &&!DigJob.anyClaim(l,worker.queenId());}
    public boolean caregiver(ServerLevel l,NestPlan p){return l.getEntity(worker.getUUID())==worker&&NestPlan.loaded(l,worker.blockPosition())&&l.isPositionEntityTicking(worker.blockPosition())
        &&eligible(l,p)&&nursingAuthorized(l)&&plan.entrance().equals(p.entrance())&&plan.direction()==p.direction();}
    public boolean assignConstruction(NestPlan p){return assignConstruction(p,"assigned_bounded_extension");}
    public boolean assignConstruction(NestPlan p,String why){
        if(!canConstruct(p)||NestExpansion.remainingCaregivers((ServerLevel)worker.level(),worker.queenId(),p,worker)<2)return false;
        plan=p.routeGeometry();next(Phase.DIG,why);return true;
    }
    public boolean construction(){return phase==Phase.DIG||phase==Phase.DIG_OUT;}
    /** Upgrade work (ChamberUpgrade): fetching clay from the store, or ramming it into the next wall cell. */
    public boolean upgrading(){return phase==Phase.UPGRADE_FETCH||phase==Phase.UPGRADE_BUILD;}
    /** The same eligibility as digging: a free, empty-handed mature member, leaving at least two caregivers. */
    public boolean assignUpgrade(NestPlan p){
        if(!canConstruct(p)||NestExpansion.remainingCaregivers((ServerLevel)worker.level(),worker.queenId(),p,worker)<2)return false;
        plan=p.routeGeometry();next(Phase.UPGRADE_FETCH,"assigned_chamber_wall_upgrade");return true;
    }
    /** The claimed builder of one of its colony's upgrade jobs, a living mature member, with a ready queen. */
    public boolean upgradeAuthorized(ServerLevel l){
        var j=worker.queenId()==null?null:ChamberUpgrade.get(l).claimedBy(worker);var q=queen(l);
        return j!=null&&upgrading()&&!foragerClaim(l)&&!constructionClaim(l)&&worker.isAlive()&&!worker.isRemoved()&&!worker.isCallow()&&!worker.isNoAi()&&plan!=null
            &&ColonyMembers.get(l).belongs(worker,worker.queenId(),plan.chamber())&&j.home.entrance().equals(plan.entrance())&&j.home.direction()==plan.direction()&&q!=null&&q.isAlive()&&q.founding().ready();
    }
    private boolean constructionAuthorized(ServerLevel l){
        var j=worker.queenId()==null?null:DigJob.claimedBy(l,worker);var q=queen(l);
        return j!=null&&worker.getUUID().equals(j.claim)&&!foragerClaim(l)&&worker.isAlive()&&!worker.isRemoved()&&!worker.isCallow()&&!worker.isNoAi()&&plan!=null
            &&ColonyMembers.get(l).belongs(worker,worker.queenId(),plan.chamber())&&j.home.entrance().equals(plan.entrance())&&j.home.direction()==plan.direction()&&q!=null&&q.isAlive()&&q.founding().ready();
    }
    public boolean assignNurse(NestPlan p){
        if(sharing.busy()||phase!=Phase.NURSERY||!(worker.level() instanceof ServerLevel l)||!eligible(l,p)||!worker.getMainHandItem().isEmpty()||constructionClaim(l)||foragerClaim(l))return false;
        plan=p.routeGeometry();next(Phase.NURSE_CACHE,"mature_member_nursing");
        return true;
    }
    public boolean assign(NestPlan p) {
        if(!canForage(p))return false;
        plan=p.routeGeometry();
        next(Phase.OPENING,"assigned_free_forager");return true;
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
        if(defense(l))return;
        if(sharing.tick(l))return;
        if(adultMeal(l))return;
        if(worker.isCallow() || phase==Phase.NURSERY) {hold(worker.isCallow()?"callow_shelter":"nursery_shelter");return;}
        if(!(construction()?constructionAuthorized(l):upgrading()?upgradeAuthorized(l):nursing()?nursingAuthorized(l):!constructionClaim(l)&&authorized(l))) {hold("home_unavailable_or_invalid_cargo_retained");feedingTicks=0;harvestingTicks=0;return;}
        // The existing 240-tick SEARCH window ends when a source is found. Approach/action
        // keep the normal 1200-tick controller bound; late discovery cannot shorten harvesting.
        if(phase==Phase.SEARCH) {
            if(++searchTicks>240){clearFlower();next(Phase.RETURN,"bounded_search_finished");cooldown=40;return;}
        }
        if(nursing()&&NestExpansion.get(l).operationalSpace(l,worker.queenId()).contains(worker.blockPosition()))NestExpansion.get(l).used(worker.queenId(),worker,"existing_nurse_traversal");
        if(construction()){var job=DigJob.claimedBy(l,worker);job.ticks++;job.changed(l);}
        if(upgrading()){ChamberUpgrade.get(l).claimedBy(worker).ticks++;ChamberUpgrade.get(l).setDirty();}
        phaseTicks++; if(cooldown>0)cooldown--;
        if(phaseTicks>1200) {
            if(phase==Phase.OPENING || phase==Phase.SOIL_OUT) {hold("opening_route_stalled_retry_no_remote_completion");phaseTicks=0;cooldown=40;}
            else if(construction()){hold("construction_route_stalled_cargo_retained");phaseTicks=0;cooldown=40;}
            else if(upgrading()){hold("upgrade_route_stalled_cargo_retained");phaseTicks=0;cooldown=40;}
            else if(nursing()){hold("nursing_route_stalled_cargo_retained");phaseTicks=0;cooldown=40;}
            else if(worker.getMainHandItem().isEmpty()) {source=null;next(Phase.RETURN,"trip_limit_return");}
            else {hold("physical_route_stalled_cargo_retained_retry");phaseTicks=0;cooldown=40;}
        }
        if(cooldown>0)return;
        switch(phase) {
            case OPENING -> open(l);
            case SOIL_OUT -> soilOut(l);
            case EXIT -> { if(arrive(Vec3.atBottomCenterOf(plan.outside()))){searchTicks=0;flowerInspections=0;clearFlower();next(Phase.SEARCH,"outside_search");} }
            case SEARCH -> search(l);
            case APPROACH -> pickup(l);
            case NECTAR_APPROACH, HARVEST -> nectar(l);
            case RETURN -> {
                var job=NestExpansion.get(l).job(worker.queenId());
                // An empty search return has nothing to deliver. Vacate the single founding stair
                // while its real builder still needs it; loaded collision/pathfinding moves both actors.
                if(worker.getMainHandItem().isEmpty()&&(job!=null&&!job.complete()||ChamberExcavation.get(l).hauling(worker.queenId())))next(Phase.EXIT,"empty_forager_yields_construction_stair");
                else if(arrive(Vec3.atBottomCenterOf(plan.at(3,0,-2))))next(Phase.DEPOSIT,"inside_delivery");
            }
            case DEPOSIT -> deposit(l);
            case NURSE_CACHE -> nurseCache(l);
            case NURSE_FEED -> nurseFeed(l);
            case NURSE_RETURN -> nurseReturn(l);
            case DIG -> dig(l);
            case DIG_OUT -> digOut(l);
            case UPGRADE_FETCH -> upgradeFetch(l);
            case UPGRADE_BUILD -> upgradeBuild(l);
            default -> { }
        }
    }
    private boolean defense(ServerLevel l){
        // Persist this clock/cooldown so re-entry or coherent restore in the same
        // loaded tick cannot decrement twice or duplicate a physical bite.
        if(defenseLastTick==l.getGameTime())return defending;
        defenseLastTick=l.getGameTime();if(biteCooldown>0)biteCooldown--;if(biteTicks>0)biteTicks--;
        worker.setBiteAction(biteTicks>0);
        var alarm=ColonyAlarm.get(l).alarm(worker.queenId());
        var player=alarm==null?null:l.getPlayerByUUID(alarm.player());
        boolean member=worker.queenId()!=null&&worker.nurseryHome()!=null&&ColonyMembers.get(l).belongs(worker,worker.queenId(),worker.nurseryHome());
        var origin=alarm==null?Vec3.ZERO:Vec3.atBottomCenterOf(alarm.origin());
        int radius=defending&&player!=null&&player.getUUID().equals(defenseTarget)?ColonyAlarm.CHASE_RADIUS:ColonyAlarm.RESPONSE_RADIUS;
        boolean eligible=member&&!worker.isCallow()&&!worker.isNoAi()&&!worker.isRemoved()&&ColonyAlarm.validPlayer(l,player)
            &&worker.position().distanceToSqr(origin)<=radius*radius
            &&player.position().distanceToSqr(origin)<=ColonyAlarm.CHASE_RADIUS*ColonyAlarm.CHASE_RADIUS;
        if(!eligible){
            if(defending){defending=false;defenseTarget=null;worker.getNavigation().stop();worker.setBiteAction(false);biteTicks=0;}
            return false;
        }
        if(!defending||!player.getUUID().equals(defenseTarget)){
            defending=true;defenseTarget=player.getUUID();sharing.cancel(l);feedingTicks=0;harvestingTicks=0;worker.adultLife().resetMeal();
            worker.getNavigation().stop();
        }
        // Ignore nest readiness here: a genuine breach is precisely why the
        // normal controller may now be unable to work. Cargo/phase/claims stay.
        var contact=player.position().add(0,.25,0);worker.getLookControl().setLookAt(player.getEyePosition());
        if(reaches(l,worker,contact)){
            worker.getNavigation().stop();
            if(biteCooldown==0){
                biteCooldown=20;biteTicks=5;worker.setBiteAction(true);
                float before=player.getHealth();var damage=worker.damageSources().mobAttack(worker);
                boolean accepted=player.hurtServer(l,damage,1.0F);
                if(accepted){bites++;dev.primeants.PrimeAnts.LOGGER.info("Ant bite ant={} colony={} player={} source={} tick={} healthBefore={} healthAfter={} cooldown={}",worker.getUUID(),worker.queenId(),player.getUUID(),damage.type().msgId(),l.getGameTime(),before,player.getHealth(),biteCooldown);}
            }
        }else if(worker.tickCount%10==0||worker.getNavigation().isDone())worker.getNavigation().moveTo(player.getX(),player.getY(),player.getZ(),0,1.0);
        return true;
    }
    private boolean adultMeal(ServerLevel l){
        var life=worker.adultLife();
        if(!(life.hungry()||sharing.needsCrop(l))||worker.isNoAi()||plan==null||!ColonyMembers.get(l).belongs(worker,worker.queenId(),plan.chamber())
            ||plan.nurseryProblem(l,worker.queenId(),true)!=null){life.resetMeal();return false;}
        if(worker.getMainHandItem().isEmpty()&&l.getBlockEntity(plan.cache()) instanceof NestCache cache&&cache.ownedBy(worker.queenId(),plan)
            &&cache.contents().stream().anyMatch(s->Nutrition.sugarYield(s)>0)){
            if(!reaches(l,worker,Vec3.atBottomCenterOf(plan.cache()).add(0,0.15,0))){life.resetMeal();arrive(Vec3.atBottomCenterOf(plan.at(4,-1,-2)));return true;}
            cache.withdrawMeal(worker,plan);
        }
        var cargo=worker.getMainHandItem();
        if(Nutrition.sugarYield(cargo)==0||!worker.nutrition().accepts(cargo,Nutrition.QUEEN_SUGAR_CAPACITY,0)||!worker.onGround()||worker.isInWater()){life.resetMeal();return false;}
        worker.getNavigation().stop();reason="physical_adult_meal";
        if(!life.mealAction())return true;
        if(worker.nutrition().ingest(cargo,Nutrition.QUEEN_SUGAR_CAPACITY,0)){
            PrimeAnts.LOGGER.info("Physical adult meal worker={} consumed={} age={} fasting={} receipts={}",worker.getUUID(),cargo,worker.elapsedAgeTicks(),life.fasting(),worker.nutrition().consumedUnits());
            worker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        }
        life.resetMeal();return true;
    }
    public static boolean queenNeedsFood(LasiusNigerEntity q,ItemStack food){
        // Protein cannot be exported from the queen's ingested store to larvae. Keep
        // further physical portions in cache for outstanding/future brood once an egg is funded.
        return q.acceptsFood(food)&&(Nutrition.proteinYield(food)==0||q.nutrition().protein()<Nutrition.EGG_PROTEIN);
    }
    private UUID chooseRecipient(ServerLevel l,ItemStack s){
        if(l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile p){
            var larva=p.records().stream().filter(r->p.accepts(r,s)).findFirst();if(larva.isPresent())return larva.get().id();
        }
        var q=queen(l);return q!=null&&q.isAlive()&&!q.isNoAi()&&q.founding().ready()&&queenNeedsFood(q,s)?q.getUUID():null;
    }
    public boolean hasRecipient(ServerLevel l,ItemStack s){return nursingAuthorized(l)&&chooseRecipient(l,s)!=null;}
    private void nurseCache(ServerLevel l){
        if(!worker.getMainHandItem().isEmpty()){next(Phase.NURSE_RETURN,"existing_nurse_cargo_retained");return;}
        if(!(l.getBlockEntity(plan.cache()) instanceof NestCache cache)||!cache.ownedBy(worker.queenId(),plan)){hold("nurse_owned_cache_unavailable");return;}
        if(cache.contents().stream().noneMatch(s->hasRecipient(l,s))){
            var space=new java.util.ArrayList<>(NestExpansion.get(l).circulationSpace(l,worker.queenId()));
            var job=NestExpansion.get(l).job(worker.queenId());
            // With only the first column open, every extension floor is reserved for the builder.
            // Idle nurses must walk back to the opposite original row instead of holding the sole
            // supported work stand indefinitely. Ordinary collision/navigation still own movement.
            if(job!=null&&job.removed()<job.tasks.size())for(int f=3;f<=5;f++)space.add(plan.at(f,-job.side,-2));
            if(!space.isEmpty()){
                var nurses=l.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(plan.chamber()).inflate(6),w->w.isAlive()&&!w.isNoAi()&&worker.queenId().equals(w.queenId())&&w.workerTasks().nursing()).stream().sorted(java.util.Comparator.comparing(w->w.getUUID().toString())).toList();
                int index=java.util.stream.IntStream.range(0,nurses.size()).filter(n->nurses.get(n)==worker).findFirst().orElse(0);
                var floor=space.get(index%space.size());if(NestPlan.walkable(l,floor)){reason="nurse_circulation_space";arriveSupported(l,Vec3.atBottomCenterOf(floor));return;}
            }
            hold("nurse_no_food_or_accepting_recipient");return;
        }
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
        boolean accepts=toQueen?q!=null&&q.isAlive()&&!q.isNoAi()&&q.founding().ready()&&queenNeedsFood(q,worker.getMainHandItem()):p!=null&&p.records().stream().anyMatch(r->r.id().equals(recipient)&&p.accepts(r,worker.getMainHandItem()));
        if(!accepts){feedingTicks=0;next(Phase.NURSE_RETURN,"recipient_refused_food_retained");return;}
        Vec3 target=toQueen?q.position().add(0,0.25,0):Vec3.atBottomCenterOf(plan.nursery()).add(0,0.15,0);
        if(!reaches(l,worker,target)){
            feedingTicks=0;
            var space=NestExpansion.get(l).circulationSpace(l,worker.queenId());
            if(toQueen){
                var stands=new java.util.ArrayList<BlockPos>(space);
                for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++)stands.add(plan.at(f,s,-2));
                var stand=stands.stream().filter(feet->NestPlan.walkable(l,feet)&&dev.primeants.entity.Nestmates.movementClear(l,worker,worker.getBoundingBox().move(Vec3.atBottomCenterOf(feet).subtract(worker.position())))
                    &&Vec3.atBottomCenterOf(feet).add(0,0.25,0).distanceToSqr(target)<=1.6)
                    .min(java.util.Comparator.comparingDouble(feet->worker.position().distanceToSqr(Vec3.atBottomCenterOf(feet)))).orElse(null);
                if(stand!=null){arriveSupported(l,Vec3.atBottomCenterOf(stand));return;}
            }
            // The fallback follows supported chamber rows to reach the brood pile.
            var delta=worker.position().subtract(Vec3.atBottomCenterOf(plan.entrance()));double forward=delta.x*plan.direction().getStepX()+delta.z*plan.direction().getStepZ();
            if(!toQueen&&forward<4.8)arriveSupported(l,Vec3.atBottomCenterOf(plan.at(5,-1,-2)));
            else if(!toQueen&&NestExpansion.get(l).circulationSpace(l,worker.queenId()).contains(plan.at(5,2,-2))
                &&delta.x*plan.direction().getClockWise().getStepX()+delta.z*plan.direction().getClockWise().getStepZ()<1.8)arriveSupported(l,Vec3.atBottomCenterOf(plan.at(5,2,-2)));
            else arriveSupported(l,Vec3.atBottomCenterOf(plan.at(toQueen?4:5,toQueen?-1:1,-2)));
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
    private boolean exposed(ServerLevel l,BlockPos target,BlockPos feet){
        var face=new BlockPos(feet.getX(),target.getY(),feet.getZ());
        return target.distManhattan(face)==1&&l.getBlockState(face).isAir()
            &&Vec3.atCenterOf(target).distanceToSqr(Vec3.atBottomCenterOf(feet).add(0,0.5,0))<=3.0;
    }
    private BlockPos digStand(ServerLevel l,BlockPos target){
        var stands=java.util.stream.StreamSupport.stream(Direction.Plane.HORIZONTAL.spliterator(),false)
            .flatMap(d->java.util.stream.IntStream.rangeClosed(-1,0).mapToObj(y->target.relative(d).offset(0,y,0)))
            .filter(p->NestPlan.loaded(l,p)&&NestPlan.walkable(l,p)&&exposed(l,target,p))
            .sorted(java.util.Comparator.comparingLong(BlockPos::asLong)).toList();
        // Terrain pathfinding does not reserve space among living bodies. Keep the same worker/cargo,
        // but try another exposed supported side after a loaded interval without a successful action.
        if(stands.contains(worker.blockPosition())&&worker.onGround()&&worker.position().distanceToSqr(Vec3.atCenterOf(target))<=5.0)return worker.blockPosition();
        return stands.isEmpty()?null:stands.get((phaseTicks/100)%stands.size());
    }
    private void prepareExposed(ServerLevel l,DigJob j){
        for(var p:j.surfaces())if(worker.position().distanceToSqr(Vec3.atCenterOf(p))<=5.0
            &&j.completed().stream().anyMatch(t->t.distManhattan(p)==1)&&NaturalSoil.get(l).eligible(l,p))ColonyTerrain.get(l).prepare(l,p,worker.queenId());
    }
    private int dirt(){return worker.getMainHandItem().is(Items.DIRT)?worker.getMainHandItem().getCount():0;}
    private boolean arriveSupported(ServerLevel l,Vec3 dest){
        Vec3 delta=dest.subtract(worker.position());
        if(delta.lengthSqr()<=6.25&&Math.abs(delta.y)<0.3&&worker.onGround()){
            // A short clear supported approach need not first center on a crowded vanilla path node.
            boolean clear=true;
            for(int n=1;n<=10;n++){
                var step=delta.scale(n/10.0);var p=BlockPos.containing(worker.position().add(step));
                if(!NestPlan.loaded(l,p)||!l.getBlockState(p.below()).isSolidRender()||!l.getFluidState(p.below()).isEmpty()
                    ||!dev.primeants.entity.Nestmates.movementClear(l,worker,worker.getBoundingBox().move(step))){clear=false;break;}
            }
            if(clear){worker.getNavigation().stop();worker.getMoveControl().setWantedPosition(dest.x,dest.y,dest.z,1.0);return delta.lengthSqr()<0.01;}
        }
        boolean arrived=arrive(dest);
        var path=worker.getNavigation().getPath();
        // A small worker already inside a supported node can follow the next real step when
        // neighboring bodies keep it from that node's exact center. Vanilla collision/gravity still move it.
        if(!arrived&&path!=null&&!path.isDone()&&path.getNextNodePos().equals(worker.blockPosition())
            &&path.getNextNodeIndex()+1<path.getNodeCount()&&NestPlan.walkable(l,worker.blockPosition())){
            var node=path.getNode(path.getNextNodeIndex()+1);var next=new BlockPos(node.x,node.y,node.z);
            if(NestPlan.loaded(l,next)&&NestPlan.walkable(l,next)&&Math.abs(next.getY()-worker.blockPosition().getY())<=1)path.advance();
        }
        return arrived;
    }
    private void dig(ServerLevel l){
        var j=DigJob.claimedBy(l,worker);
        if(j.removed()==j.tasks.size()||j.stopped()){
            if(dirt()>0){next(Phase.DIG_OUT,(j.stopped()?"stopped_":"last_")+j.label()+"_soil_transport");return;}
            var marker=j.stopped()?null:j.pendingMarker(l,worker.queenId());
            if(marker!=null){setUpMarker(l,j,marker);return;}
            j.claim=null;if(!j.stopped())j.reason=j.completionReason(l,worker.queenId());j.changed(l);next(Phase.NURSE_CACHE,j.label()+(j.stopped()?"_stopped":"_complete")+"_return_to_colony");return;
        }
        if(!worker.getMainHandItem().isEmpty()&&dirt()==0){hold("foreign_cargo_retained_no_excavation");return;}
        int progress=j.removed();var target=j.tasks.get(progress);int column=1;
        while(progress+column<j.tasks.size()&&j.tasks.get(progress+column).getX()==target.getX()&&j.tasks.get(progress+column).getZ()==target.getZ())column++;
        if(dirt()+column>QueenFounding.CARRY_CAPACITY){next(Phase.DIG_OUT,"complete_column_before_hauling");return;}
        if(!targetCompatible(l,target,j.expected.get(progress))){
            // Never remove a replaced, player-placed or otherwise ineligible planned cell.
            if(j.stopsOnIncompatibleTarget()){j.stop(l,"target_replaced_unknown_or_foreign_at_"+target.toShortString());return;}
            j.reason="target_replaced_unknown_or_foreign";j.changed(l);hold(j.reason);return;
        }
        if(dev.primeants.founding.SupportSurvival.problem(l,target,Blocks.AIR.defaultBlockState()) instanceof String problem){j.reason=problem;j.changed(l);hold(j.reason);return;}
        var stand=digStand(l,target);if(stand==null){hold("no_supported_exposed_face");return;}
        // A crowded stand need not be monopolized at its exact center. The actor already occupies
        // this supported adjacent floor; the same physical reach/face/state checks still gate its action.
        if(!worker.blockPosition().equals(stand)||!worker.onGround()||worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0){approach(l,j,stand);return;}
        worker.getNavigation().stop();worker.getLookControl().setLookAt(target.getX()+0.5,target.getY()+0.5,target.getZ()+0.5);
        if(!constructionAuthorized(l)||!NestPlan.walkable(l,stand)||!exposed(l,target,stand)||worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0
            ||!targetCompatible(l,target,j.expected.get(progress))||dirt()>=QueenFounding.CARRY_CAPACITY||j.removed()>=j.cap())return;
        if(dev.primeants.founding.SupportSurvival.problem(l,target,Blocks.AIR.defaultBlockState()) instanceof String problem){hold(problem);return;}
        if(!l.setBlock(target,Blocks.AIR.defaultBlockState(),3)){hold(j.label()+"_removal_rejected");return;}
        ColonyTerrain.get(l).removed(target,worker.queenId());j.removed(l,target);
        worker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIRT,dirt()+1));cooldown=QueenFounding.cadence();phaseTicks=0;
        prepareExposed(l,j);j.reason="worker_removed_one_soil_unit";
        PrimeAnts.LOGGER.info("{} excavation queen={} worker={} target={} removed={} carried={} deposited={}",j.title(),worker.queenId(),worker.getUUID(),target,j.removed(),dirt(),j.deposited);
        if((dirt()==QueenFounding.CARRY_CAPACITY||j.removed()==j.tasks.size())&&target.getY()==plan.entrance().getY()-2)next(Phase.DIG_OUT,"soil_in_mandibles_transport");
    }
    /** Outside or on the stairs, a builder bound beyond the founding chamber first walks into the chamber, as returning
     * foragers do; from the chamber's floor the passage leads on. */
    private void approach(ServerLevel l,DigJob j,BlockPos stand){
        if(j.viaFoundingChamber()&&worker.blockPosition().getY()>plan.entrance().getY()-2)arriveSupported(l,Vec3.atBottomCenterOf(plan.at(3,0,-2)));
        else arriveSupported(l,Vec3.atBottomCenterOf(stand));
    }
    /** The claimed builder walks onto an open floor beside the marker cell and sets up the job's marker block. */
    private void setUpMarker(ServerLevel l,DigJob j,BlockPos marker){
        if(!reaches(l,worker,Vec3.atBottomCenterOf(marker).add(0,0.15,0))){
            var completed=j.completed();BlockPos stand=null;
            for(var d:Direction.Plane.HORIZONTAL){var p=marker.relative(d);
                if(completed.contains(p)&&NestPlan.walkable(l,p)&&(stand==null||worker.position().distanceToSqr(Vec3.atBottomCenterOf(p))<worker.position().distanceToSqr(Vec3.atBottomCenterOf(stand))))stand=p;}
            if(stand==null){hold(j.label()+"_marker_stand_unavailable");return;}
            approach(l,j,stand);return;
        }
        worker.getNavigation().stop();worker.getLookControl().setLookAt(marker.getX()+0.5,marker.getY()+0.2,marker.getZ()+0.5);
        if(!constructionAuthorized(l))return;
        if(j.setUp(l,worker))PrimeAnts.LOGGER.info("{} marker set up queen={} worker={} marker={} reason={}",j.title(),worker.queenId(),worker.getUUID(),marker,j.reason);
        else hold(j.label()+"_marker_setup_refused");
    }
    private boolean targetCompatible(ServerLevel l,BlockPos p,net.minecraft.world.level.block.state.BlockState expected){
        return ColonyTerrain.get(l).compatible(l,p,worker.queenId(),expected);
    }
    /** The claimed builder takes one cell's clay out of its colony's confirmed store, in reach of the store block. A job
     * with nothing left to build, or stopped, ends here once a carried unit is back in the store. A store short of clay
     * releases the builder: it returns to the colony and the job resumes when clay arrives (ChamberUpgrade.consider). */
    private void upgradeFetch(ServerLevel l){
        var j=ChamberUpgrade.get(l).claimedBy(worker);var cargo=worker.getMainHandItem();boolean clay=cargo.is(Items.CLAY_BALL);
        if(!cargo.isEmpty()&&!clay){hold("foreign_cargo_retained_no_upgrade");return;}
        if(clay&&!j.complete()&&!j.stopped()){next(Phase.UPGRADE_BUILD,"clay_in_mandibles_to_the_wall");return;}
        if(!clay&&(j.complete()||j.stopped())){
            boolean stopped=j.stopped();j.release(l,worker.queenId(),stopped?j.reason:"completed_tier_"+j.tier);
            next(Phase.NURSE_CACHE,"upgrade_"+(stopped?"stopped":"complete")+"_return_to_colony");return;
        }
        var store=MaterialStore.confirmed(l,worker.queenId(),plan);
        if(!clay&&store!=null&&store.units(MaterialUnits.Material.CLAY)<NestWalls.CLAY_PER_CELL){j.release(l,worker.queenId(),"waiting_for_store_clay");next(Phase.NURSE_CACHE,"upgrade_waits_for_store_clay");return;}
        if(store==null){hold(clay?"upgrade_store_unconfirmed_unit_retained":"upgrade_store_unconfirmed");cooldown=40;return;}
        var target=Vec3.atBottomCenterOf(store.getBlockPos()).add(0,0.15,0);
        if(!reaches(l,worker,target)){
            var stand=store.stand(l,worker);if(stand==null){hold("upgrade_store_stand_unavailable");cooldown=40;return;}
            arriveSupported(l,Vec3.atBottomCenterOf(stand));return;
        }
        worker.getNavigation().stop();worker.getLookControl().setLookAt(target.x,target.y,target.z);
        if(clay){
            // A stopped job's carried unit goes back where it came from.
            if(store.putBack(worker,plan,j))PrimeAnts.LOGGER.info("Chamber upgrade unit returned queen={} worker={} store={} ledger={}",worker.queenId(),worker.getUUID(),store.getBlockPos(),j.ledger());
            else{hold("upgrade_unit_return_refused_retained");cooldown=40;}
            return;
        }
        if(store.takeForUpgrade(worker,plan,j)){
            PrimeAnts.LOGGER.info("Chamber upgrade clay taken queen={} worker={} store={} next={} ledger={}",worker.queenId(),worker.getUUID(),store.getBlockPos(),j.next(),j.ledger());
            next(Phase.UPGRADE_BUILD,"clay_taken_from_store");cooldown=20;
        } else {hold("upgrade_store_withdrawal_refused");cooldown=40;}
    }
    /** The builder carries its clay to a supported stand beside the next wall cell and rams it into the cell's own earth:
     * the cell becomes the colony's packed clay and no soil leaves the nest. A cell that is no longer natural or colony
     * earth, solid and dry (a player placed or changed it) is never converted: it stops the job. */
    private void upgradeBuild(ServerLevel l){
        var j=ChamberUpgrade.get(l).claimedBy(worker);
        if(!worker.getMainHandItem().is(Items.CLAY_BALL)||worker.getMainHandItem().getCount()<NestWalls.CLAY_PER_CELL||j.complete()||j.stopped()){next(Phase.UPGRADE_FETCH,"upgrade_unit_or_job_changed");return;}
        var target=j.next();
        if(!NestPlan.loaded(l,target)){hold("upgrade_wall_cell_unavailable");return;}
        if(!j.convertible(l,target,worker.queenId())){j.stop(l,"wall_cell_not_colony_earth_at_"+target.toShortString());next(Phase.UPGRADE_FETCH,"upgrade_stopped_unit_back_to_store");return;}
        var stand=wallStand(l,target);if(stand==null){hold("no_supported_stand_beside_wall");return;}
        if(!worker.blockPosition().equals(stand)||!worker.onGround()||worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0){arriveSupported(l,Vec3.atBottomCenterOf(stand));return;}
        worker.getNavigation().stop();worker.getLookControl().setLookAt(target.getX()+0.5,target.getY()+0.5,target.getZ()+0.5);
        if(!upgradeAuthorized(l)||!NestPlan.walkable(l,stand)||!wallFaceOpen(l,target,stand)||!j.convertible(l,target,worker.queenId()))return;
        var wall=NurseryBlocks.PACKED_CLAY.defaultBlockState();
        if(dev.primeants.founding.SupportSurvival.problem(l,target,wall) instanceof String problem){hold(problem);return;}
        if(!l.setBlock(target,wall,3)){hold("upgrade_conversion_rejected");return;}
        ColonyTerrain.get(l).built(target,worker.queenId(),NurseryBlocks.PACKED_CLAY);
        int left=worker.getMainHandItem().getCount()-NestWalls.CLAY_PER_CELL;worker.setItemSlot(EquipmentSlot.MAINHAND,left==0?ItemStack.EMPTY:new ItemStack(Items.CLAY_BALL,left));
        j.built(l,target);cooldown=QueenFounding.cadence();phaseTicks=0;
        PrimeAnts.LOGGER.info("Chamber wall rebuilt queen={} worker={} chamber={} cell={} block=packed_clay built={}/{} ledger={}",worker.queenId(),worker.getUUID(),j.chamber,target,j.built().size(),j.cells.size(),j.ledger());
        next(Phase.UPGRADE_FETCH,"wall_cell_rebuilt");
    }
    /** A walkable floor cell beside the wall cell's column, from which the cell's face is open at its own height. */
    private BlockPos wallStand(ServerLevel l,BlockPos target){
        int floor=plan.entrance().getY()-2;
        var stands=java.util.stream.StreamSupport.stream(Direction.Plane.HORIZONTAL.spliterator(),false).map(d->target.relative(d).atY(floor))
            .filter(p->NestPlan.loaded(l,p)&&NestPlan.walkable(l,p)&&wallFaceOpen(l,target,p)).sorted(java.util.Comparator.comparingLong(BlockPos::asLong)).toList();
        if(stands.contains(worker.blockPosition())&&worker.onGround()&&worker.position().distanceToSqr(Vec3.atCenterOf(target))<=5.0)return worker.blockPosition();
        return stands.isEmpty()?null:stands.get((phaseTicks/100)%stands.size());
    }
    private static boolean wallFaceOpen(ServerLevel l,BlockPos target,BlockPos stand){
        var face=new BlockPos(stand.getX(),target.getY(),stand.getZ());return target.distManhattan(face)==1&&l.getBlockState(face).isAir();
    }
    private void digOut(ServerLevel l){
        var j=DigJob.claimedBy(l,worker);
        if(dirt()==0){next(Phase.DIG,"empty_mandibles_next_work");return;}
        BlockPos target=null;Vec3 dest=null;
        for(var p:NestExpansion.deposits(plan)){
            if(!NestExpansion.depositSupport(l,p,worker.queenId())||!l.getBlockState(p).isAir()||!l.getEntities(worker,new AABB(p)).isEmpty())continue;
            for(var d:Direction.Plane.HORIZONTAL)for(int y=-1;y<=0;y++){
                var stand=p.relative(d).offset(0,y,0);if(!NestPlan.loaded(l,stand)||!NestPlan.walkable(l,stand)||Vec3.atCenterOf(p).distanceToSqr(Vec3.atBottomCenterOf(stand).add(0,0.5,0))>3.0)continue;
                var v=Vec3.atBottomCenterOf(stand).add(0.35*d.getStepX(),0,0.35*d.getStepZ());
                if(!dev.primeants.entity.Nestmates.movementClear(l,worker,worker.getBoundingBox().move(v.subtract(worker.position()))))continue;
                target=p;dest=v;break;
            }
            if(target!=null)break;
        }
        if(target==null){j.reason="mound_blocked_soil_retained";hold(j.reason);cooldown=40;return;}
        if(!arriveSupported(l,dest))return;
        if(!constructionAuthorized(l)||!NestExpansion.depositSupport(l,target,worker.queenId())||!l.getBlockState(target).isAir()||!l.getEntities(worker,new AABB(target)).isEmpty()
            ||worker.getBoundingBox().intersects(new AABB(target))||worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0)return;
        if(dev.primeants.founding.SupportSurvival.problem(l,target,NurseryBlocks.NEST_SOIL.defaultBlockState()) instanceof String problem){hold(problem);return;}
        if(l.setBlock(target,NurseryBlocks.NEST_SOIL.defaultBlockState(),3)){
            ColonyTerrain.get(l).deposited(target,worker.queenId());j.deposited++;j.changed(l);
            worker.setItemSlot(EquipmentSlot.MAINHAND,dirt()==1?ItemStack.EMPTY:new ItemStack(Items.DIRT,dirt()-1));cooldown=QueenFounding.cadence();phaseTicks=0;
            PrimeAnts.LOGGER.info("{} deposit queen={} worker={} target={} removed={} carried={} deposited={}",j.title(),worker.queenId(),worker.getUUID(),target,j.removed(),dirt(),j.deposited);
            if(dirt()==0)next(Phase.DIG,"delivered_soil_next_work");
        }
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
                BlockPos stand=p.relative(d);if(!NestPlan.loaded(l,stand)||!NestPlan.walkable(l,stand))continue;
                Vec3 v=Vec3.atBottomCenterOf(stand).add(0.35*d.getStepX(),0,0.35*d.getStepZ());
                if(!dev.primeants.entity.Nestmates.movementClear(l,worker,worker.getBoundingBox().move(v.subtract(worker.position()))))continue;
                target=p;dest=v;break;
            }
            if(target!=null)break;
        }
        if(target==null) {hold("mound_full_or_blocked_soil_retained");cooldown=40;return;}
        if(!arrive(dest))return;
        if(!authorized(l)||!l.getBlockState(target).isAir()||!NaturalSoil.get(l).eligible(l,target.below())||!l.getEntities(worker,new AABB(target)).isEmpty()
                || worker.getBoundingBox().intersects(new AABB(target)) || worker.position().distanceToSqr(Vec3.atCenterOf(target))>5.0) {hold("mound_revalidation_failed");return;}
        if(dev.primeants.founding.SupportSurvival.problem(l,target,NurseryBlocks.NEST_SOIL.defaultBlockState()) instanceof String problem){hold(problem);return;}
        if(l.setBlock(target,NurseryBlocks.NEST_SOIL.defaultBlockState(),3)) {ColonyTerrain.get(l).deposited(target,worker.queenId());worker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);placed++;cooldown=10;next(Phase.OPENING,"plug_soil_on_mound");}
    }
    private void search(ServerLevel l) {
        if(phaseTicks>240) {next(Phase.RETURN,"bounded_search_finished");cooldown=40;return;}
        if(worker.tickCount%20!=0)return;
        // Snapshot at most 96 identities. Continue across pulses and SEARCH windows; removed or
        // changed entries consume one bounded inspection, never restart the nearer eight.
        if(droppedCursor>=droppedQueue.size()){
            droppedQueue.clear();droppedCursor=0;
            var candidates=new java.util.ArrayList<ItemEntity>();
            l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(plan.outside()).inflate(SEARCH_RADIUS,3,SEARCH_RADIUS),
                i->i.isAlive()&&!i.isRemoved()&&!i.hasPickUpDelay()&&food(i.getItem())&&NestPlan.loaded(l,i.blockPosition()),candidates,DROPPED_TOTAL);
            candidates.stream().limit(DROPPED_TOTAL).sorted(java.util.Comparator.comparingDouble(worker::distanceToSqr))
                .forEach(i->droppedQueue.add(i.getUUID()));
        }
        int endDrops=Math.min(droppedQueue.size(),droppedCursor+DROPPED_PER_PULSE);
        while(droppedCursor<endDrops){
            var e=l.getEntity(droppedQueue.get(droppedCursor++));
            if(!(e instanceof ItemEntity item)||!item.isAlive()||item.isRemoved()||item.hasPickUpDelay()||!food(item.getItem())
                ||!withinSearch(item.blockPosition())||!NestPlan.walkable(l,item.blockPosition()))continue;
            var path=worker.getNavigation().createPath(item.blockPosition(),0,48);
            if(path==null||!path.canReach())continue;
            source=item.getUUID();next(Phase.APPROACH,"supported_dropped_food_found");return;
        }
        if(droppedCursor<droppedQueue.size())return; // Drops receive their full bounded turn before native fallback.
        // Discovery only reads loaded cells. At most 1536 inspections and 8 path trials per pulse;
        // 12 pulses cover the full 16807-cell box when no eligible reachable source is found.
        int end=Math.min(FLOWER_INSPECTION_BUDGET,flowerInspections+FLOWER_INSPECTIONS_PER_PULSE);
        var ready=new java.util.ArrayList<BlockPos>();
        while(flowerInspections<end){
            var p=plan.outside().offset(FLOWER_OFFSETS.get(flowerInspections++));
            if(!NestPlan.loaded(l,p))continue;
            boolean prey=NativePrey.flower(l.getBlockState(p));
            if(!harvestRoom(l,prey))continue;
            if(prey?NativePrey.get(l).ready(l,p):FlowerNectar.get(l).ready(l,p))ready.add(p);
        }
        // Rank by recipient deficit after actual cache/cargo, then distance. Drops retain priority.
        ready.sort(java.util.Comparator.comparingDouble((BlockPos p)->-harvestNeed(l,NativePrey.flower(l.getBlockState(p))))
            .thenComparingDouble(p->worker.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        int attempts=0;
        for(var p:ready){
            if(++attempts>8)break;
            var path=worker.getNavigation().createPath(p,0,48);if(path==null||!path.canReach())continue;
            flowerSource=p;flowerStand=p;flowerExpected=l.getBlockState(p).toString();harvestingTicks=0;next(Phase.NECTAR_APPROACH,"supported_ready_native_food_found");return;
        }
        if(flowerInspections>=FLOWER_INSPECTION_BUDGET)materials(l);
    }
    /** Food comes first: dropped natural material is searched only once this trip found no dropped or native food, and
     * dropped food is checked again at the moment of pickup (pickup).
     * It is taken from item entities on the ground in the same search area, one unit at a time, and only while the
     * colony's store is confirmed and has room for it. At most 8 path trials per pulse. */
    private void materials(ServerLevel l){
        var store=MaterialStore.confirmed(l,worker.queenId(),plan);if(store==null)return;
        var candidates=new java.util.ArrayList<ItemEntity>();
        l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(plan.outside()).inflate(SEARCH_RADIUS,3,SEARCH_RADIUS),
            i->i.isAlive()&&!i.isRemoved()&&!i.hasPickUpDelay()&&store.room(i.getItem())&&NestPlan.loaded(l,i.blockPosition()),candidates,DROPPED_TOTAL);
        candidates.sort(java.util.Comparator.comparingDouble(worker::distanceToSqr));
        int attempts=0;
        for(var item:candidates){
            if(!withinSearch(item.blockPosition())||!NestPlan.walkable(l,item.blockPosition()))continue;
            if(++attempts>8)break;
            var path=worker.getNavigation().createPath(item.blockPosition(),0,48);
            if(path==null||!path.canReach())continue;
            source=item.getUUID();next(Phase.APPROACH,"supported_dropped_material_found");return;
        }
    }
    private double harvestNeed(ServerLevel l,boolean prey){
        var q=queen(l);if(q==null)return 0;
        long need=prey?Nutrition.QUEEN_PROTEIN_CAPACITY-q.nutrition().protein():Nutrition.QUEEN_SUGAR_CAPACITY-q.nutrition().sugar();
        if(l.getBlockEntity(plan.nursery()) instanceof dev.primeants.brood.BroodPile pile)
            for(var r:pile.records())if(!r.founding()&&r.stage()==dev.primeants.brood.BroodStage.LARVA)
                need+=prey?Nutrition.LARVA_PROTEIN-r.nutrition().gainedProtein():Nutrition.LARVA_SUGAR-r.nutrition().gainedSugar();
        var units=new java.util.ArrayList<ItemStack>();
        if(l.getBlockEntity(plan.cache()) instanceof NestCache cache)units.addAll(cache.contents());
        for(var a:l.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(plan.outside()).inflate(32,6,32),a->a.isAlive()&&worker.queenId().equals(a.queenId()))){
            units.add(a.getMainHandItem());
            if(!prey)need+=Math.max(0,4000-a.nutrition().sugar()); // one finite future adult meal
        }
        for(var stack:units)need-=prey?Nutrition.proteinYield(stack):Nutrition.sugarYield(stack);
        return Math.max(0,need)/(double)(prey?Nutrition.PREY_PROTEIN:Nutrition.NECTAR_V2_SUGAR);
    }
    public boolean harvestRoom(ServerLevel l,boolean prey){
        if(l.getBlockEntity(plan.cache()) instanceof NestCache cache){
            if(!cache.ownedBy(worker.queenId(),plan)||cache.size()>=NestCache.CAPACITY)return false;
            if(cache.contents().stream().filter(s->prey?Nutrition.proteinYield(s)>0:Nutrition.sugarYield(s)>0).count()>=2)return false;
        }
        return harvestNeed(l,prey)>0;
    }
    private void clearFlower(){flowerSource=null;flowerStand=null;flowerExpected=null;harvestingTicks=0;}
    private void nectar(ServerLevel l){
        if(!worker.getMainHandItem().isEmpty()){clearFlower();next(Phase.RETURN,"existing_cargo_return");return;}
        if(flowerSource==null||flowerStand==null||!withinSearch(flowerSource)||!NestPlan.loaded(l,flowerSource)
            ||!(NativePrey.flower(l.getBlockState(flowerSource))?NativePrey.habitat(l,flowerSource):FlowerNectar.habitat(l,flowerSource))||!l.getBlockState(flowerSource).toString().equals(flowerExpected)
            ||!(NativePrey.flower(l.getBlockState(flowerSource))?NativePrey.get(l).ready(l,flowerSource):FlowerNectar.get(l).ready(l,flowerSource))||!harvestRoom(l,NativePrey.flower(l.getBlockState(flowerSource)))){
            clearFlower();next(Phase.SEARCH,"nectar_source_unready_changed_or_storage_unavailable");return;
        }
        var target=Vec3.atBottomCenterOf(flowerSource).add(0,0.35,0);
        if(!NestPlan.walkable(l,flowerStand)||!reaches(l,worker,target)){
            harvestingTicks=0;arriveSupported(l,Vec3.atBottomCenterOf(flowerStand));return;
        }
        worker.getNavigation().stop();worker.getLookControl().setLookAt(target.x,target.y,target.z);
        if(phase!=Phase.HARVEST)next(Phase.HARVEST,"physical_flower_harvesting");
        if(!authorized(l)||!ColonyMembers.get(l).belongs(worker,worker.queenId(),plan.chamber())
            ||!l.isPositionEntityTicking(flowerSource)||!l.isPositionEntityTicking(worker.blockPosition())
            ||!l.mayInteract(worker,flowerSource)){harvestingTicks=0;return;}
        if(++harvestingTicks<FlowerNectar.ACTION_TICKS)return;
        boolean harvested=NativePrey.flower(l.getBlockState(flowerSource))?NativePrey.get(l).harvest(l,worker,flowerSource,flowerExpected):FlowerNectar.get(l).harvest(l,worker,flowerSource,flowerExpected);clearFlower();
        next(harvested?Phase.RETURN:Phase.SEARCH,harvested?"physical_nectar_in_mandibles":"nectar_commit_revalidation_refused");
    }
    private void pickup(ServerLevel l) {
        var entity=source==null?null:l.getEntity(source);
        if(!(entity instanceof ItemEntity item) || !item.isAlive()||item.isRemoved()||item.hasPickUpDelay()||!(food(item.getItem())||MaterialStore.material(item.getItem()))||!withinSearch(item.blockPosition())||!NestPlan.loaded(l,item.blockPosition())) {source=null;next(Phase.SEARCH,"source_unavailable");return;}
        if(!worker.getMainHandItem().isEmpty()) {next(Phase.RETURN,"existing_cargo_return");return;}
        Vec3 target=item.position().add(0,0.1,0);
        if(!reaches(l,worker,target)) {arrive(Vec3.atBottomCenterOf(item.blockPosition()));return;}
        // A material unit is taken only while the confirmed store has room for it, checked at the moment of pickup.
        if(!authorized(l) || !item.isAlive() || item.hasPickUpDelay() || !reaches(l,worker,target)
            || !(food(item.getItem())||MaterialStore.material(item.getItem())&&MaterialStore.confirmed(l,worker.queenId(),plan) instanceof MaterialStore store&&store.room(item.getItem())))return;
        // Food comes first up to the moment a material leaves the ground: collectable dropped food in the search area
        // takes the forager away from the material, which stays where it lies.
        if(!food(item.getItem())&&collectableFood(l) instanceof ItemEntity food){
            source=food.getUUID();next(Phase.APPROACH,"food_before_material");
            PrimeAnts.LOGGER.info("Worker leaves material for food worker={} material={} food={} position={}",worker.getUUID(),item.getItem(),food.getItem(),food.position());return;
        }
        var remaining=item.getItem().copy();var cargo=remaining.split(1);
        item.setItem(remaining);if(remaining.isEmpty())item.discard();
        worker.setItemSlot(EquipmentSlot.MAINHAND,cargo);source=null;next(Phase.RETURN,food(cargo)?"physical_food_in_mandibles":"physical_material_in_mandibles");
        PrimeAnts.LOGGER.info("Worker pickup worker={} item={} position={}",worker.getUUID(),cargo,worker.position());
    }
    /** Dropped food this trip could collect now, by the search's own rules: in the search area, supported and reachable;
     * nearest first, at most 8 path trials. */
    private ItemEntity collectableFood(ServerLevel l){
        var candidates=new java.util.ArrayList<ItemEntity>();
        l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(plan.outside()).inflate(SEARCH_RADIUS,3,SEARCH_RADIUS),
            i->i.isAlive()&&!i.isRemoved()&&!i.hasPickUpDelay()&&food(i.getItem())&&NestPlan.loaded(l,i.blockPosition()),candidates,DROPPED_TOTAL);
        candidates.sort(java.util.Comparator.comparingDouble(worker::distanceToSqr));
        int attempts=0;
        for(var item:candidates){
            if(!withinSearch(item.blockPosition())||!NestPlan.walkable(l,item.blockPosition()))continue;
            if(++attempts>8)break;
            var path=worker.getNavigation().createPath(item.blockPosition(),0,48);
            if(path!=null&&path.canReach())return item;
        }
        return null;
    }
    private void deposit(ServerLevel l) {
        if(worker.getMainHandItem().isEmpty()) {next(Phase.EXIT,"next_bounded_trip");cooldown=40;return;}
        if(MaterialStore.material(worker.getMainHandItem())) {store(l);return;}
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
    /** From the founding chamber the carrier walks to its colony's store block and puts its one unit in. A missing, full
     * or unconfirmed store leaves the unit in its mandibles, and it waits; then it returns through the chamber. */
    private void store(ServerLevel l){
        var store=MaterialStore.owned(l,worker.queenId(),plan);
        if(store==null){hold("material_store_unavailable_cargo_retained");cooldown=40;return;}
        var target=Vec3.atBottomCenterOf(store.getBlockPos()).add(0,0.15,0);
        if(!reaches(l,worker,target)){
            var stand=store.stand(l,worker);
            if(stand==null){hold("material_store_stand_unavailable_cargo_retained");cooldown=40;return;}
            arriveSupported(l,Vec3.atBottomCenterOf(stand));return;
        }
        worker.getNavigation().stop();worker.getLookControl().setLookAt(target.x,target.y,target.z);
        if(store.deposit(worker,plan)){
            PrimeAnts.LOGGER.info("Material stored worker={} store={} stored={}",worker.getUUID(),store.getBlockPos(),store.contents());next(Phase.RETURN,"material_physically_stored");cooldown=40;
        } else {hold("material_store_full_or_unconfirmed_cargo_retained");cooldown=40;}
    }
    public void die(ServerLevel l) {
        if(phase==Phase.DEAD)return;worker.getNavigation().stop();
        int constructionSoil=constructionClaim(l)&&worker.getMainHandItem().is(Items.DIRT)?worker.getMainHandItem().getCount():0;
        int upgradeClay=upgradeClaim(l)&&worker.getMainHandItem().is(Items.CLAY_BALL)?worker.getMainHandItem().getCount():0;
        if(!worker.getMainHandItem().isEmpty()) {
            var transfer=UUID.nameUUIDFromBytes(("worker-cargo:"+worker.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            TransferCustody.get(l).take(transfer,"worker:"+worker.getUUID(),worker.position(),worker.getMainHandItem());
            worker.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            TransferCustody.get(l).retry(l);
        }
        var job=DigJob.claimedBy(l,worker);if(job!=null)job.release(l,worker,constructionSoil);
        ChamberUpgrade.get(l).release(l,worker,upgradeClay);
        var q=queen(l);if(q!=null)q.founding().releaseWorker(worker);
        ColonyMembers.get(l).died(worker);
        source=null;next(Phase.DEAD,"worker_dead_no_replacement");
        clearFlower();
    }
    public void save(ValueOutput out) {
        out.putBoolean("Defending",defending);if(defenseTarget!=null)out.putString("DefenseTarget",defenseTarget.toString());
        out.putInt("BiteCooldown",biteCooldown);out.putInt("BiteTicks",biteTicks);out.putLong("DefenseLastTick",defenseLastTick);out.putLong("Bites",bites);
        sharing.save(out.child("CropSharing"));
        out.store("DroppedQueue",com.mojang.serialization.Codec.STRING.listOf(),droppedQueue.stream().map(UUID::toString).toList());out.putInt("DroppedCursor",droppedCursor);
        out.putString("Phase",phase.name());out.putString("Reason",reason);out.putInt("Opened",opened);out.putInt("Placed",placed);out.putInt("PhaseTicks",phaseTicks);out.putInt("Cooldown",cooldown);
        if(plan!=null) {out.store("Entrance",BlockPos.CODEC,plan.entrance());out.putString("Direction",plan.direction().getName());
            out.store("SurfaceDeposits",BlockPos.CODEC.listOf(),plan.surfaceDeposits());
            if(plan.exteriorStand()!=null)out.store("ExteriorStand",BlockPos.CODEC,plan.exteriorStand());}
        if(source!=null)out.putString("Source",source.toString());
        if(flowerSource!=null){out.store("FlowerSource",BlockPos.CODEC,flowerSource);out.store("FlowerStand",BlockPos.CODEC,flowerStand);out.putString("FlowerExpected",flowerExpected);}
        out.putInt("HarvestingTicks",harvestingTicks);out.putInt("SearchTicks",searchTicks);out.putInt("FlowerInspections",flowerInspections);
        if(recipient!=null)out.putString("Recipient",recipient.toString());out.putInt("FeedingTicks",feedingTicks);
        // Cargo is canonical vanilla Mob mainhand equipment, not duplicated here.
    }
    public void load(ValueInput in) {
        defending=in.getBooleanOr("Defending",false);defenseTarget=in.getString("DefenseTarget").map(UUID::fromString).orElse(null);
        biteCooldown=in.getIntOr("BiteCooldown",0);biteTicks=in.getIntOr("BiteTicks",0);defenseLastTick=in.getLongOr("DefenseLastTick",Long.MIN_VALUE);bites=in.getLongOr("Bites",0);
        if(biteCooldown<0||biteCooldown>20||biteTicks<0||biteTicks>5||bites<0||defending!=(defenseTarget!=null))throw new IllegalArgumentException("Invalid bounded defense interrupt");
        worker.setBiteAction(biteTicks>0);
        sharing.load(in.childOrEmpty("CropSharing"));
        droppedQueue.clear();in.read("DroppedQueue",com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()).forEach(v->droppedQueue.add(UUID.fromString(v)));droppedCursor=in.getIntOr("DroppedCursor",0);
        if(droppedQueue.size()>DROPPED_TOTAL||droppedCursor<0||droppedCursor>droppedQueue.size())throw new IllegalArgumentException("Invalid dropped continuation");
        phase=Phase.valueOf(in.getStringOr("Phase","NURSERY"));reason=in.getStringOr("Reason","restored");opened=in.getIntOr("Opened",0);placed=in.getIntOr("Placed",0);phaseTicks=in.getIntOr("PhaseTicks",0);cooldown=in.getIntOr("Cooldown",0);
        source=in.getString("Source").map(UUID::fromString).orElse(null);
        flowerSource=in.read("FlowerSource",BlockPos.CODEC).orElse(null);flowerStand=in.read("FlowerStand",BlockPos.CODEC).orElse(null);flowerExpected=in.getString("FlowerExpected").orElse(null);
        harvestingTicks=in.getIntOr("HarvestingTicks",0);searchTicks=in.getIntOr("SearchTicks",0);flowerInspections=in.getIntOr("FlowerInspections",0);
        if(harvestingTicks<0||harvestingTicks>=FlowerNectar.ACTION_TICKS||searchTicks<0||flowerInspections<0||flowerInspections>FLOWER_INSPECTION_BUDGET
            ||(flowerSource!=null&&(flowerStand==null||flowerExpected==null)))throw new IllegalArgumentException("Invalid saved nectar action");
        recipient=in.getString("Recipient").map(UUID::fromString).orElse(null);feedingTicks=in.getIntOr("FeedingTicks",0);
        if(in.read("Entrance",BlockPos.CODEC).isPresent()) {Direction d=Direction.byName(in.getStringOr("Direction",""));if(d==null||d.getAxis().isVertical())throw new IllegalArgumentException("Invalid task home");var g=NestPlan.geometry(in.read("Entrance",BlockPos.CODEC).orElseThrow(),d);
            plan=new NestPlan(g.entrance(),d,g.tasks(),g.expected(),List.of(),List.of(),in.read("SurfaceDeposits",BlockPos.CODEC.listOf()).orElse(List.of()),in.read("ExteriorStand",BlockPos.CODEC).orElse(null));
            if(!plan.validAdaptation())throw new IllegalArgumentException("Invalid saved worker exterior bounds");}
        if(opened<0||opened>2||placed<0||placed>opened||phaseTicks<0||cooldown<0||feedingTicks<0||feedingTicks>=FEEDING_TICKS)throw new IllegalArgumentException("Invalid worker progress");
    }
}
