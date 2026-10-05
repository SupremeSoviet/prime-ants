package dev.primeants.founding;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.time.SimulationTimeScale;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One queen owns one physical job and its soil. Loaded server ticks only; no offline work or population state. */
public final class QueenFounding {
    public enum Phase { NONE, SEEKING, EXCAVATING, TRANSPORTING, ENTERING, SEALING, SETTLED, FAILED, DEAD }
    public static final long BASE_WORK_TICKS = 200;
    public static final int CARRY_CAPACITY = 4;
    private final LasiusNigerEntity queen;
    private Phase phase = Phase.NONE;
    private NestPlan plan;
    private int progress, deposited, released, plugged, cooldown, stalled, converted;
    private long loadedTicks;
    private final java.util.List<BlockPos> removedPlants = new java.util.ArrayList<>();
    public List<BlockPos> removedPlants() { return List.copyOf(removedPlants); }
    private String reason = "not_requested";
    public enum Lifecycle { CLAUSTRAL, OPENING, OPEN }
    private Lifecycle lifecycle = Lifecycle.CLAUSTRAL;
    private java.util.UUID workerClaim;
    public Lifecycle lifecycle() { return lifecycle; }
    public java.util.UUID workerClaim() { return workerClaim; }
    public boolean ready() { return phase == Phase.SETTLED && queen.level() instanceof ServerLevel level && enclosureProblem(level, lifecycle != Lifecycle.CLAUSTRAL) == null; }
    public boolean claimedBy(LasiusNigerEntity worker) { return worker.getUUID().equals(workerClaim) && queen.getUUID().equals(worker.queenId()); }
    public void releaseWorker(LasiusNigerEntity worker) { if (claimedBy(worker)) workerClaim = null; }
    public void opened() {
        if (phase == Phase.SETTLED && plan != null && queen.level() instanceof ServerLevel level
                && plan.plugs().stream().allMatch(p -> ColonyPlugs.get(level).opened(level,p,queen.getUUID()))) lifecycle = Lifecycle.OPEN;
    }
    public QueenFounding(LasiusNigerEntity queen) { this.queen = queen; }
    public static double multiplier() { return Double.parseDouble(System.getProperty("prime_ants.foundingWorkMultiplier", "1")); }
    public static int cadence() { return (int)Math.max(1, Math.min(Integer.MAX_VALUE, new SimulationTimeScale(multiplier()).ticksForGameDays(BASE_WORK_TICKS / 24000.0))); }
    public Phase phase() { return phase; }
    public NestPlan plan() { return plan; }
    public int removed() { return progress; }
    public int deposited() { return deposited; }
    public int released() { return released; }
    public int plugged() { return plugged; }
    public int converted() { return converted; }
    public int carried() { return queen.getMainHandItem().is(Items.DIRT) ? queen.getMainHandItem().getCount() : 0; }
    public long loadedTicks() { return loadedTicks; }
    public String reason() {
        if (phase == Phase.SETTLED && queen.level() instanceof ServerLevel level) return settledReason(level);
        return reason;
    }
    /** SETTLED records historical completion; readiness always describes the live enclosure. */
    public boolean sealed() { return phase == Phase.SETTLED && queen.level() instanceof ServerLevel level
            && enclosureProblem(level) == null; }
    /** Phase-independent physical predicate, shared by completion and readiness. Placement counters are history. */
    private String enclosureProblem(ServerLevel level) {
        return enclosureProblem(level, false);
    }
    private String enclosureProblem(ServerLevel level, boolean operational) {
        if (plan == null) return "enclosure_plan_missing";
        if (operational && lifecycle == Lifecycle.OPEN && plan.plugs().stream().anyMatch(p -> !ColonyPlugs.get(level).opened(level,p,queen.getUUID()))) return "enclosure_operational_opening_incomplete";
        String habitat = plan.nurseryProblem(level, queen.getUUID(), operational);
        if (habitat != null) return habitat;
        BlockPos a = plan.at(3, -1, -2), b = plan.at(5, 1, -1);
        AABB interior = new AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, b.getY() + 1, Math.max(a.getZ(), b.getZ()) + 1);
        AABB body = queen.getBoundingBox();
        boolean inside=body.minX>=interior.minX&&body.minY>=interior.minY&&body.minZ>=interior.minZ
            &&body.maxX<=interior.maxX&&body.maxY<=interior.maxY&&body.maxZ<=interior.maxZ;
        if(!inside&&operational&&body.minY>=interior.minY&&body.maxY<=interior.maxY){
            // A body's entire footprint must be covered by the original room or currently verified
            // completed physical voids that fit this body. Planned cells and unauthorized holes confer no habitat.
            var usable=new java.util.ArrayList<BlockPos>(NestExpansion.get(level).bodyFloors(level,queen.getUUID(),body.maxY));
            if(plan.plugs().stream().allMatch(p->ColonyPlugs.get(level).opened(level,p,queen.getUUID()))
                &&NestPlan.walkable(level,plan.at(2,0,-2)))usable.add(plan.at(2,0,-2));
            inside=!usable.isEmpty();
            for(int x=(int)Math.floor(body.minX);x<Math.ceil(body.maxX)&&inside;x++)for(int z=(int)Math.floor(body.minZ);z<Math.ceil(body.maxZ)&&inside;z++){
                var p=new BlockPos(x,(int)interior.minY,z);
                boolean original=x>=interior.minX&&x+1<=interior.maxX&&z>=interior.minZ&&z+1<=interior.maxZ;
                if(!original&&!usable.contains(p))inside=false;
            }
        }
        if (!queen.isAlive() || queen.isRemoved() || !queen.onGround() || queen.isInWater()
                // AABB.contains is half-open for POINTS and rejects max-face contact. A physical body
                // may touch the wall without crossing it: use inclusive box containment, with no epsilon.
                || !inside
                || !level.noCollision(queen, body.deflate(0.001))) return "enclosure_queen_not_inside";
        return null;
    }
    private String settledReason(ServerLevel level) {
        String problem = enclosureProblem(level, lifecycle != Lifecycle.CLAUSTRAL);
        return problem == null ? (lifecycle == Lifecycle.CLAUSTRAL ? (reason.startsWith("settled_opening_refused") ? reason : "settled_throat_sealed") : "operational_" + lifecycle.name().toLowerCase()) : "settled_not_ready_" + problem;
    }
    public boolean ownsMovement() { return phase != Phase.NONE && phase != Phase.FAILED && phase != Phase.DEAD; }
    public void request() { if (phase == Phase.NONE) { phase = Phase.SEEKING; reason = "seeking_verified_natural_soil"; } }
    private void phase(Phase value) { phase = value; stalled = 0; queen.getNavigation().stop(); }
    private void fail(String why) {
        reason = why; phase(Phase.FAILED);
        PrimeAnts.LOGGER.info("Founding failed queen={} reason={} removed={} carried={} deposited={} released={} plugged={}", queen.getUUID(), why, progress, carried(), deposited, released, plugged);
    }
    private void carry(int count) { queen.setItemSlot(EquipmentSlot.MAINHAND, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.DIRT, count)); }
    public void tick(ServerLevel level) {
        if (!queen.isAlive() || !ownsMovement()) return;
        if (phase == Phase.SETTLED) {
            queen.getNavigation().stop();
            String current = settledReason(level);
            if (!current.equals(reason)) {
                reason = current;
                PrimeAnts.LOGGER.info("Founding readiness queen={} phase={} reason={}", queen.getUUID(), phase, reason);
            }
            if (sealed()) queen.prepareNursery(level, plan);
            if(workerClaim!=null&&level.getEntity(workerClaim) instanceof LasiusNigerEntity claimed
                &&(!dev.primeants.worker.ColonyMembers.get(level).belongs(claimed,queen.getUUID(),plan.chamber())||!claimed.workerTasks().foraging())){
                PrimeAnts.LOGGER.warn("Released incompatible persisted forager claim queen={} worker={} phase={} cargo={}",queen.getUUID(),claimed.getUUID(),claimed.workerTasks().phase(),claimed.getMainHandItem());workerClaim=null;
            }
            NestExpansion.get(level).reconcile(level,queen);
            if (workerClaim == null && ready()) {
                var workers = level.getEntitiesOfClass(LasiusNigerEntity.class, new AABB(plan.chamber()).inflate(4),
                        w -> w.isAlive() && !w.isRemoved() && !w.isCallow() && !w.isNoAi() && queen.getUUID().equals(w.queenId())
                                && plan.chamber().equals(w.nurseryHome()) && w.workerTasks().canForage(plan));
                if (!workers.isEmpty()) {
                    // Prefer a free side-row worker over the central worker boxed in by the two nurses.
                    // This is a physical assignment choice among living adults, never a replacement spawn.
                    var worker = workers.stream().min(java.util.Comparator.<LasiusNigerEntity>comparingDouble(w -> {
                        Vec3 delta = w.position().subtract(Vec3.atBottomCenterOf(plan.chamber()));
                        Direction side = plan.direction().getClockWise();
                        return -Math.abs(delta.x * side.getStepX() + delta.z * side.getStepZ());
                    }).thenComparing(w -> w.getUUID().toString())).orElseThrow();
                    // Closed legacy nests without placement records fail closed; never infer ownership from dirt.
                    if (lifecycle != Lifecycle.CLAUSTRAL || plan.plugs().stream().allMatch(p -> ColonyPlugs.get(level).owned(level,p,queen.getUUID()))) {
                        if(worker.workerTasks().assign(plan)){
                            workerClaim = worker.getUUID(); if (lifecycle == Lifecycle.CLAUSTRAL) lifecycle = Lifecycle.OPENING;
                        }
                    } else reason = "settled_opening_refused_plug_ownership_missing_or_revoked";
                }
            }
            if(lifecycle==Lifecycle.OPEN&&ready()){
                var members=level.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(plan.chamber()).inflate(16),w->w.isAlive()&&!w.isRemoved()&&dev.primeants.worker.ColonyMembers.get(level).belongs(w,queen.getUUID(),plan.chamber()));
                NestExpansion.get(level).consider(level,queen,members);
                for(var w:level.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(plan.chamber()).inflate(4),w->w.isAlive()&&!w.isRemoved()&&!w.isCallow()&&!w.isNoAi()))
                    if(!claimedBy(w)&&!w.workerTasks().construction()&&dev.primeants.worker.ColonyMembers.get(level).belongs(w,queen.getUUID(),plan.chamber()))w.workerTasks().assignNurse(plan);
            }
            return;
        }
        loadedTicks++;
        if (queen.isInWater()) { fail("fluid_at_queen"); return; }
        if (phase == Phase.SEEKING) {
            BlockPos ground = queen.blockPosition().below();
            for (int r = 0; r <= 3 && plan == null; r++) for (int dx = -r; dx <= r && plan == null; dx++)
                for (int dz = -r; dz <= r && plan == null; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos column=ground.offset(dx,0,dz);
                        if(!NestPlan.loaded(level,column))continue;
                        // Founding starts at the physical queen's ground, including legacy underground fixtures.
                        // A heightmap may include an unrelated overhead block; only placement searches the world surface.
                        NestPlan candidate=null;
                        for(int dy=0;dy<=2 && candidate==null;dy++) {
                            int delta=dy==0?0:dy==1?1:-1;
                            candidate=NestPlan.candidate(level,column.offset(0,delta,0),d);
                        }
                        if (candidate != null) { plan = candidate; break; }
                    }
                }
            if (plan == null) { fail("no_verified_supported_soil_site_within_radius_3"); return; }
            reason = "verified_site_work_in_progress"; cooldown = cadence(); phase(Phase.EXCAVATING);
            PrimeAnts.LOGGER.info("Founding planned queen={} entrance={} direction={} tasks={} cap={} cadence={} multiplier={}", queen.getUUID(), plan.entrance(), plan.direction(), plan.tasks().size(), NestPlan.HARD_CAP, cadence(), multiplier());
            return;
        }
        if (++stalled > 600) { fail("blocked_physical_route_" + phase); return; }
        if (cooldown > 0) cooldown--;
        switch (phase) {
            case EXCAVATING -> excavate(level);
            case TRANSPORTING -> transport(level);
            case ENTERING -> {
                if (!plan.openWalkable(level) || !plan.enclosedChamber(level)) { fail("excavated_route_no_longer_walkable"); return; }
                if (arrive(level, plan.at(3, 0, -2)) && cooldown == 0) phase(Phase.SEALING);
            }
            case SEALING -> seal(level);
            default -> { }
        }
    }
    private boolean arrive(ServerLevel level, BlockPos feet) {
        if (!NestPlan.walkable(level, feet)) return false;
        Vec3 dest = Vec3.atBottomCenterOf(feet);
        return arrive(dest);
    }
    private boolean arrive(Vec3 dest) {
        queen.getLookControl().setLookAt(dest.x, dest.y + 0.3, dest.z);
        if (queen.position().distanceToSqr(dest) < 0.09 && queen.onGround()) {
            queen.getNavigation().stop(); stalled = 0; return true;
        }
        if (queen.tickCount % 20 == 0 || queen.getNavigation().isDone()) queen.getNavigation().moveTo(dest.x, dest.y, dest.z, 0, 1.0);
        return false;
    }
    private BlockPos workStand(ServerLevel level, BlockPos target) {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
            BlockPos feet = target.relative(d).offset(0, dy, 0);
            if (!NestPlan.walkable(level, feet) || !exposed(level, target, feet)
                || !dev.primeants.entity.Nestmates.movementClear(level,queen,queen.getBoundingBox().move(Vec3.atBottomCenterOf(feet).subtract(queen.position())))) continue;
            double dist = queen.position().distanceToSqr(Vec3.atBottomCenterOf(feet));
            if (dist < distance) { best = feet; distance = dist; }
        }
        return best;
    }
    private boolean exposed(ServerLevel level, BlockPos target, BlockPos feet) {
        BlockPos face = new BlockPos(feet.getX(), target.getY(), feet.getZ());
        return target.distManhattan(face) == 1
                && (NestPlan.traversable(level,face) || (feet.getY() == target.getY() + 1 && NestPlan.traversable(level,target.above())))
                && Vec3.atCenterOf(target).distanceToSqr(Vec3.atBottomCenterOf(feet).add(0, 0.5, 0)) <= 3.0;
    }
    private Vec3 depositDestination(BlockPos target, BlockPos stand) {
        return Vec3.atBottomCenterOf(stand).add(0.35 * (stand.getX() - target.getX()), 0, 0.35 * (stand.getZ() - target.getZ()));
    }
    private BlockPos depositStand(ServerLevel level, BlockPos target) {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
            BlockPos feet = target.relative(d).offset(0, dy, 0);
            if (!NestPlan.walkable(level, feet) || !exposed(level, target, feet)
                || !dev.primeants.entity.Nestmates.movementClear(level,queen,queen.getBoundingBox().move(Vec3.atBottomCenterOf(feet).subtract(queen.position())))) continue;
            Vec3 dest = depositDestination(target, feet);
            if (!dev.primeants.entity.Nestmates.movementClear(level,queen,queen.getBoundingBox().move(dest.subtract(queen.position())))) continue;
            double dist = queen.position().distanceToSqr(dest);
            if (dist < distance) { best = feet; distance = dist; }
        }
        return best;
    }
    private boolean arrivePlacement(Vec3 dest) {
        double distance = queen.position().distanceToSqr(dest);
        if (distance < 0.0025 && queen.onGround()) { queen.getNavigation().stop(); stalled = 0; return true; }
        // Ground paths terminate at block centers. MoveControl performs the short final fractional approach,
        // with ordinary collision/gravity; no teleport and no second controller.
        if (distance < 1.0 && Math.abs(queen.getY() - dest.y) < 0.3 && queen.onGround()) {
            queen.getNavigation().stop(); queen.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, 1.0); return false;
        }
        if (queen.tickCount % 20 == 0 || queen.getNavigation().isDone()) queen.getNavigation().moveTo(dest.x, dest.y, dest.z, 0, 1.0);
        return false;
    }
    private void excavate(ServerLevel level) {
        if (progress >= plan.tasks().size() || progress >= NestPlan.HARD_CAP) { phase(Phase.ENTERING); return; }
        // Keep enough mandible capacity to finish this vertical column before a long outside trip.
        // Natural grass transitions retain existing soil authority; complete the column before hauling.
        // The final two recovered units still belong to sealing, not exterior deposition.
        if (carried() > 0 && progress < plan.tasks().size() - 2) {
            BlockPos top = plan.tasks().get(progress); int column = 1;
            while (progress + column < plan.tasks().size() - 2) {
                BlockPos p = plan.tasks().get(progress + column);
                if (p.getX() != top.getX() || p.getZ() != top.getZ()) break;
                column++;
            }
            if (carried() + column > CARRY_CAPACITY) { phase(Phase.TRANSPORTING); return; }
        }
        BlockPos target = plan.tasks().get(progress);
        if (!NaturalSoil.get(level).compatible(level, target, plan.expected().get(progress))) {
            fail("planned_soil_replaced_or_origin_revoked_at_" + target); return;
        }
        for(int i=0;i<plan.plants().size();i++) {
            var plant=plan.plants().get(i);if(!plant.below().equals(target)) continue;
            if(removedPlants.contains(plant)) {
                if(!level.getBlockState(plant).isAir()) {fail("cleared_plant_cell_replaced_at_"+plant);return;}
                continue;
            }
            if(!NativeVegetation.get(level).eligible(level,plant) || !level.getBlockState(plant).equals(plan.plantExpected().get(i))) {
                fail("planned_plant_replaced_or_authority_revoked_at_"+plant);return;
            }
            // The supported soil work face may be one block lower and out of reach of its flower.
            // Reach the plant itself from an existing surface stand before descending to dig the soil.
            var plantStand=workStand(level,plant);
            if(plantStand==null) {fail("no_supported_plant_work_face_at_"+plant);return;}
            if(!arrive(level,plantStand) || cooldown>0)return;
            if(queen.form()!=dev.primeants.entity.AntForm.QUEEN || level.getEntity(queen.getUUID())!=queen
                || !queen.isAlive() || !level.mayInteract(queen,plant)
                || queen.position().distanceToSqr(Vec3.atCenterOf(plant))>5.0 || !exposed(level,plant,plantStand)
                || !NativeVegetation.get(level).eligible(level,plant) || !level.getBlockState(plant).equals(plan.plantExpected().get(i))) {
                fail("plant_work_revalidation_failed_at_"+plant);return;
            }
            if(!level.setBlock(plant,Blocks.AIR.defaultBlockState(),3)) {fail("plant_removal_failed_at_"+plant);return;}
            removedPlants.add(plant);cooldown=cadence();stalled=0; // No soil, cargo, drops or nutrition.
            PrimeAnts.LOGGER.info("Founding plant removal queen={} tick={} plant={} declared={} soilRemoved={}",queen.getUUID(),loadedTicks,plant,plan.plants(),progress);
            return;
        }
        if (NativeVegetation.dependentAbove(level,target)) {fail("protected_vegetation_support_at_"+target);return;}
        BlockPos stand = workStand(level, target);
        if (stand == null) { fail("no_exposed_supported_work_face_at_" + target); return; }
        if (!arrive(level, stand) || cooldown > 0) return;
        // Immediate mutation-time checks: a living nearby actor, still-native exact state, air face and hard cap.
        if (!queen.isAlive() || queen.position().distanceToSqr(Vec3.atCenterOf(target)) > 5.0
                || !exposed(level, target, stand) || !NaturalSoil.get(level).eligible(level, target)
                || !NaturalSoil.get(level).compatible(level,target,plan.expected().get(progress)) || carried() >= CARRY_CAPACITY) {
            fail("work_revalidation_failed_at_" + target); return;
        }
        if (NativeVegetation.dependentAbove(level,target)) {fail("protected_vegetation_support_at_"+target);return;}
        if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), 3)) { fail("removal_failed_at_" + target); return; }
        progress++; carry(carried() + 1); cooldown = cadence(); stalled = 0;
        convertExposedSoil(level);
        PrimeAnts.LOGGER.debug("Founding action queen={} tick={} removed={} target={} carried={}", queen.getUUID(), loadedTicks, progress, target, carried());
        // Reserve final two recovered blocks for a two-high throat seal, carried into the chamber.
        if (progress <= plan.tasks().size() - 2 && (carried() == CARRY_CAPACITY || progress == plan.tasks().size() - 2)) phase(Phase.TRANSPORTING);
        else if (progress == plan.tasks().size()) phase(Phase.ENTERING);
    }
    private void transport(ServerLevel level) {
        if (carried() < 1) { fail("transport_stack_empty"); return; }
        BlockPos target = null, stand = null;
        for (BlockPos p : plan.deposits()) {
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolidRender()
                    && NaturalSoil.get(level).eligible(level, p.below()) && level.getFluidState(p).isEmpty()
                    && level.getEntities(queen, new AABB(p)).isEmpty()) {
                BlockPos face = depositStand(level, p);
                if (face != null) { target = p; stand = face; break; }
            }
        }
        if (target == null) { fail("bounded_mound_deposition_blocked"); return; }
        // Navigation's arrival tolerance must not leave the body overlapping the placement cell.
        Vec3 destination = depositDestination(target, stand);
        if (!arrivePlacement(destination) || cooldown > 0) return;
        if (!level.getBlockState(target).isAir() || !NaturalSoil.get(level).eligible(level, target.below())
                || !level.getEntities(queen, new AABB(target)).isEmpty() || queen.getBoundingBox().intersects(new AABB(target))
                || queen.position().distanceToSqr(Vec3.atCenterOf(target)) > 5.0) { fail("mound_placement_revalidation_failed"); return; }
        if (!level.setBlock(target, dev.primeants.brood.NurseryBlocks.NEST_SOIL.defaultBlockState(), 3)) { fail("mound_placement_failed"); return; }
        ColonyTerrain.get(level).deposited(target,queen.getUUID());
        deposited++;
        carry(carried() - 1); cooldown = cadence(); stalled = 0;
        if (carried() == 0) phase(Phase.EXCAVATING);
    }
    private void seal(ServerLevel level) {
        convertExposedSoil(level);
        if (plugged < 2) {
            BlockPos p = plan.plugs().get(plugged);
            if (!arrive(level, plan.at(3, 0, -2)) || cooldown > 0) return;
            if (carried() < 1 || !level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()
                    || !level.getEntities(queen, new AABB(p)).isEmpty() || queen.getBoundingBox().intersects(new AABB(p))
                    || queen.position().distanceToSqr(Vec3.atCenterOf(p)) > 5.0) { fail("plug_revalidation_failed"); return; }
            // One recovered soil unit becomes a compacted plug, just as mound deposition does.
            // Vanilla grass targets minecraft:dirt and must not erase a legitimate claustral seal.
            if (!level.setBlock(p, dev.primeants.brood.NurseryBlocks.NEST_SOIL.defaultBlockState(), 3)) { fail("plug_placement_failed"); return; }
            ColonyPlugs.get(level).placed(p, queen.getUUID());
            carry(carried() - 1); plugged++; cooldown = cadence(); stalled = 0;
            return;
        }
        String problem = enclosureProblem(level);
        if (problem != null) { fail(problem); return; }
        if (arrive(level, plan.chamber()) && cooldown == 0) {
            // Revalidate at the actual final transition, independently of the future phase.
            problem = enclosureProblem(level);
            if (problem != null) { fail(problem); return; }
            reason = "settled_throat_sealed"; phase(Phase.SETTLED);
            PrimeAnts.LOGGER.info("Founding settled queen={} ticks={} multiplier={} soil: removed={} carried={} deposited={} released={} plugged={}",
                    queen.getUUID(), loadedTicks, multiplier(), progress, carried(), deposited, released, plugged);
        }
    }
    private void convertExposedSoil(ServerLevel level) {
        for (BlockPos p : plan.undergroundSurfaces()) {
            // Prepare freshly exposed native dirt too: vanilla grass spreads into ordinary dirt while
            // the open route admits daylight. Nest soil preserves volume and cannot regrow grass.
            if (!(level.getBlockState(p).is(Blocks.GRASS_BLOCK) || level.getBlockState(p).is(Blocks.DIRT)) || !NaturalSoil.get(level).eligible(level, p)
                    || queen.position().distanceToSqr(Vec3.atCenterOf(p)) > 5.0) continue;
            boolean exposed = plan.tasks().stream().anyMatch(t -> t.distManhattan(p) == 1 && level.getBlockState(t).isAir());
            if (exposed && ColonyTerrain.get(level).prepare(level,p,queen.getUUID())) converted++;
        }
    }
    public void die(ServerLevel level) {
        if(phase==Phase.DEAD)return;
        queen.getNavigation().stop();
        int count = carried();
        if (count > 0) {
            var transfer=java.util.UUID.nameUUIDFromBytes(("queen-soil:"+queen.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            dev.primeants.worker.TransferCustody.get(level).take(transfer,"queen:"+queen.getUUID(),queen.position(),queen.getMainHandItem());
            released += count;carry(0);
            dev.primeants.worker.TransferCustody.get(level).retry(level);
        }
        phase = Phase.DEAD; reason = "queen_died_unfinished_terrain_retained";
    }
    public void save(ValueOutput out) {
        out.putString("Lifecycle", lifecycle.name()); if (workerClaim != null) out.putString("WorkerClaim",workerClaim.toString());
        out.putString("Phase", phase.name()); out.putString("Reason", reason());
        out.putInt("Progress", progress); out.putInt("Deposited", deposited); out.putInt("Released", released); out.putInt("Plugged", plugged);
        out.putInt("Cooldown", cooldown); out.putInt("Stalled", stalled); out.putLong("LoadedTicks", loadedTicks);
        out.putInt("Converted", converted);
        out.store("RemovedPlants",BlockPos.CODEC.listOf(),removedPlants);
        if (plan != null) {
            out.store("Entrance", BlockPos.CODEC, plan.entrance()); out.putString("Direction", plan.direction().getName());
            out.store("Tasks", BlockPos.CODEC.listOf(), plan.tasks()); out.store("Expected", BlockState.CODEC.listOf(), plan.expected());
            out.store("Plants",BlockPos.CODEC.listOf(),plan.plants());out.store("PlantExpected",BlockState.CODEC.listOf(),plan.plantExpected());
            out.store("SurfaceDeposits",BlockPos.CODEC.listOf(),plan.surfaceDeposits());
            if(plan.exteriorStand()!=null)out.store("ExteriorStand",BlockPos.CODEC,plan.exteriorStand());
        }
        // Mainhand soil uses vanilla Mob equipment persistence and synchronization.
    }
    public void load(ValueInput in) {
        lifecycle = Lifecycle.valueOf(in.getStringOr("Lifecycle","CLAUSTRAL"));
        workerClaim = in.getString("WorkerClaim").map(java.util.UUID::fromString).orElse(null);
        try { phase = Phase.valueOf(in.getStringOr("Phase", "NONE")); } catch (IllegalArgumentException e) { phase = Phase.FAILED; }
        reason = in.getStringOr("Reason", "restored"); progress = in.getIntOr("Progress", 0);
        deposited = in.getIntOr("Deposited", 0); released = in.getIntOr("Released", 0); plugged = in.getIntOr("Plugged", 0);
        cooldown = in.getIntOr("Cooldown", 0); stalled = in.getIntOr("Stalled", 0); loadedTicks = in.getLongOr("LoadedTicks", 0);
        converted = Math.max(0, in.getIntOr("Converted", 0));
        var e = in.read("Entrance", BlockPos.CODEC);
        if (e.isPresent()) {
            Direction d = Direction.byName(in.getStringOr("Direction", "north"));
            List<BlockPos> targets = in.read("Tasks", BlockPos.CODEC.listOf()).orElse(List.of());
            List<BlockState> expected = in.read("Expected", BlockState.CODEC.listOf()).orElse(List.of());
            if (d == null || d.getAxis().isVertical() || (!targets.equals(NestPlan.geometry(e.get(),d).tasks())&&!targets.equals(NestPlan.bottomFirst(e.get(),d).tasks()))
                    || expected.size() != targets.size() || progress < 0 || progress > targets.size()
                    || deposited < 0 || released < 0 || plugged < 0 || plugged > 2 || carried() > CARRY_CAPACITY
                    || progress != deposited + released + plugged + carried()) { fail("invalid_saved_plan_or_soil_balance"); return; }
            plan = new NestPlan(e.get(), d, targets, expected,
                in.read("Plants",BlockPos.CODEC.listOf()).orElse(List.of()),in.read("PlantExpected",BlockState.CODEC.listOf()).orElse(List.of()),
                in.read("SurfaceDeposits",BlockPos.CODEC.listOf()).orElse(List.of()),in.read("ExteriorStand",BlockPos.CODEC).orElse(null));
            removedPlants.clear();removedPlants.addAll(in.read("RemovedPlants",BlockPos.CODEC.listOf()).orElse(List.of()));
            if(!plan.validAdaptation() || !plan.plants().containsAll(removedPlants) || new java.util.HashSet<>(removedPlants).size()!=removedPlants.size()) fail("invalid_saved_adaptation");
        } else if (phase != Phase.NONE && phase != Phase.SEEKING && phase != Phase.FAILED && phase != Phase.DEAD) fail("missing_saved_plan");
    }
}
