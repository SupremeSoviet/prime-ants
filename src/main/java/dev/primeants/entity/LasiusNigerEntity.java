package dev.primeants.entity;

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

/** Real debug adult. Wandering is not founding, foraging, or a colony simulation. */
public final class LasiusNigerEntity extends PathfinderMob {
    private final AntForm form;
    private long elapsedAgeTicks;

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
        };
        goalSelector.addGoal(1, wander);
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        // The only biological clock owner. No daylight-time subtraction or renderer mutation.
        if (!level().isClientSide() && isAlive() && elapsedAgeTicks < Long.MAX_VALUE) {
            elapsedAgeTicks++;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("AntForm", form.serializedName());
        output.putLong("AntElapsedAgeTicks", elapsedAgeTicks);
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
        setPersistenceRequired();
    }
}
