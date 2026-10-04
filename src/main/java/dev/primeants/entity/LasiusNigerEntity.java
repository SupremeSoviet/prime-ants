package dev.primeants.entity;

import dev.primeants.founding.QueenFounding;
import dev.primeants.founding.NestPlan;
import dev.primeants.brood.BroodPile;
import dev.primeants.brood.NurseryBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Physical adult. Authorized egg queens own bounded founding; other adults retain debug wandering. */
public final class LasiusNigerEntity extends PathfinderMob {
    private final AntForm form;
    private long elapsedAgeTicks;
    private static final EntityDataAccessor<Integer> CALLOW = SynchedEntityData.defineId(LasiusNigerEntity.class, EntityDataSerializers.INT);
    private long bodyReserve = initialReserve(), callowAgeTicks, callowDuration;
    private UUID broodId, queenId;
    private BlockPos nurseryHome;
    private boolean nurseryClaimed;
    private final dev.primeants.worker.WorkerTasks workerTasks = new dev.primeants.worker.WorkerTasks(this);
    public dev.primeants.worker.WorkerTasks workerTasks() { return workerTasks; }
    public BlockPos nurseryHome() { return nurseryHome; }
    private static long initialReserve() {
        return Math.max(0, Math.min(BroodPile.MAX_RESERVE, Long.parseLong(System.getProperty("prime_ants.queenInitialReserve", Long.toString(BroodPile.MAX_RESERVE)))));
    }
    public long bodyReserve() { return bodyReserve; }
    public long callowAgeTicks() { return callowAgeTicks; }
    public long callowDuration() { return callowDuration; }
    public UUID broodId() { return broodId; }
    public UUID queenId() { return queenId; }
    public int callowVisual() { return entityData.get(CALLOW); }
    public boolean isCallow() { return callowVisual() < 1000; }
    public boolean spendReserve(long units) {
        if (level().isClientSide() || form != AntForm.QUEEN || !isAlive() || units < 0 || units > bodyReserve) return false;
        bodyReserve -= units; return true;
    }
    public void initializeCallow(UUID brood, UUID queen, BlockPos home) {
        if (form != AntForm.WORKER || broodId != null) throw new IllegalStateException("Callow must be a new worker");
        broodId = brood; queenId = queen; nurseryHome = home.immutable(); callowDuration = BroodPile.callowTicks(); callowAgeTicks = 0;
        entityData.set(CALLOW, 0);
    }
    public void prepareNursery(ServerLevel level, NestPlan plan) {
        if (nurseryClaimed || !founding.sealed() || position().distanceToSqr(Vec3.atBottomCenterOf(plan.nursery())) > BroodPile.CARE_REACH_SQUARED
                || bodyReserve < BroodPile.CAPACITY * BroodPile.EGG_COST || !NestPlan.walkable(level, plan.nursery())) return;
        if (!level.setBlock(plan.nursery(), NurseryBlocks.BROOD_PILE.defaultBlockState(), 3)) return;
        if (level.getBlockEntity(plan.nursery()) instanceof BroodPile pile && pile.establish(this, plan)) nurseryClaimed = true;
        else level.setBlock(plan.nursery(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
    private final QueenFounding founding = new QueenFounding(this);
    public QueenFounding founding() { return founding; }

    public LasiusNigerEntity(EntityType<? extends LasiusNigerEntity> type, Level level, AntForm form) {
        super(type, level);
        this.form = form;
        if (form == AntForm.WORKER) bodyReserve = 0;
        setPersistenceRequired();
    }

    public AntForm form() { return form; }
    public long elapsedAgeTicks() { return elapsedAgeTicks; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(CALLOW, 1000); }

    @Override protected PathNavigation createNavigation(Level level) { return new AntGroundNavigation(this, level); }

    public static AttributeSupplier.Builder attributes(AntForm form) {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, form == AntForm.QUEEN ? 20 : 6)
                .add(Attributes.MOVEMENT_SPEED, form == AntForm.QUEEN ? 0.12 : 0.18)
                .add(Attributes.FOLLOW_RANGE, 12);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Persistent debug adults must not permanently stop wandering after vanilla's 100 idle ticks.
        RandomStrollGoal wander = new RandomStrollGoal(this, 1.0, 40, false) {
            @Override public boolean canUse() {
                return getNavigation().isDone() && super.canUse();
            }
            @Override protected Vec3 getPosition() {
                return LandRandomPos.getPos(LasiusNigerEntity.this, 6, 2);
            }
            @Override public void start() {
                // Vanilla's reach range 1 accepts a neighboring destination at the
                // current node. Our small waypoint tolerance cannot follow a node
                // omitted by that search: request the actual chosen ground block.
                getNavigation().moveTo(wantedX, wantedY, wantedZ, 0, speedModifier);
            }
        };
        goalSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return founding != null && founding.ownsMovement() || broodId != null; }
            @Override public boolean canContinueToUse() { return canUse(); }
        });
        goalSelector.addGoal(1, wander);
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        // The only biological clock owner. No daylight-time subtraction or renderer mutation.
        if (!level().isClientSide() && isAlive() && elapsedAgeTicks < Long.MAX_VALUE) {
            elapsedAgeTicks++;
            founding.tick((ServerLevel)level());
            if (broodId != null) {
                if (callowAgeTicks < callowDuration) callowAgeTicks++;
                entityData.set(CALLOW, (int)Math.min(1000, callowAgeTicks * 1000 / Math.max(1, callowDuration)));
                workerTasks.tick((ServerLevel)level());
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("AntForm", form.serializedName());
        output.putLong("AntElapsedAgeTicks", elapsedAgeTicks);
        founding.save(output.child("Founding"));
        workerTasks.save(output.child("WorkerTask"));
        output.putLong("QueenBodyReserve", bodyReserve); output.putBoolean("NurseryClaimed", nurseryClaimed);
        if (broodId != null) {
            output.putString("BroodId", broodId.toString()); output.putString("QueenId", queenId.toString());
            output.putLong("CallowTicks", callowAgeTicks); output.putLong("CallowDuration", callowDuration);
            output.store("NurseryHome", BlockPos.CODEC, nurseryHome);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Form belongs to the registered entity type; NBT cannot turn a worker into a queen.
        String savedForm = input.getStringOr("AntForm", form.serializedName());
        if (!savedForm.equals(form.serializedName())) {
            throw new IllegalArgumentException("AntForm does not match registered entity type");
        }
        elapsedAgeTicks = Math.max(0, input.getLongOr("AntElapsedAgeTicks", 0));
        founding.load(input.childOrEmpty("Founding"));
        workerTasks.load(input.childOrEmpty("WorkerTask"));
        bodyReserve = form == AntForm.QUEEN ? Math.max(0, Math.min(BroodPile.MAX_RESERVE, input.getLongOr("QueenBodyReserve", initialReserve()))) : 0;
        nurseryClaimed = input.getBooleanOr("NurseryClaimed", false);
        if (input.getString("BroodId").isPresent()) {
            if (form != AntForm.WORKER) throw new IllegalArgumentException("Queen cannot be a callow");
            broodId = UUID.fromString(input.getStringOr("BroodId", "")); queenId = UUID.fromString(input.getStringOr("QueenId", ""));
            nurseryHome = input.read("NurseryHome", BlockPos.CODEC).orElseThrow();
            callowDuration = Math.max(1, input.getLongOr("CallowDuration", BroodPile.callowTicks()));
            callowAgeTicks = Math.max(0, Math.min(callowDuration, input.getLongOr("CallowTicks", 0)));
            entityData.set(CALLOW, (int)(callowAgeTicks * 1000 / callowDuration));
        }
        setPersistenceRequired();
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (form == AntForm.QUEEN && reason == EntitySpawnReason.SPAWN_ITEM_USE) founding.request();
        return result;
    }

    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            if (form == AntForm.QUEEN) founding.die(server); else workerTasks.die(server);
        }
        super.die(source);
    }
}
