package dev.primeants.entity;

import dev.primeants.founding.QueenFounding;
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
    private final QueenFounding founding = new QueenFounding(this);
    public QueenFounding founding() { return founding; }

    public LasiusNigerEntity(EntityType<? extends LasiusNigerEntity> type, Level level, AntForm form) {
        super(type, level);
        this.form = form;
        setPersistenceRequired();
    }

    public AntForm form() { return form; }
    public long elapsedAgeTicks() { return elapsedAgeTicks; }

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
            @Override public boolean canUse() { return founding != null && founding.ownsMovement(); }
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
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("AntForm", form.serializedName());
        output.putLong("AntElapsedAgeTicks", elapsedAgeTicks);
        founding.save(output.child("Founding"));
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
        setPersistenceRequired();
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (form == AntForm.QUEEN && reason == EntitySpawnReason.SPAWN_ITEM_USE) founding.request();
        return result;
    }

    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) founding.die(server);
        super.die(source);
    }
}
