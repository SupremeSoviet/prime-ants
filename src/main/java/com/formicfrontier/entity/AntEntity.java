package com.formicfrontier.entity;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.sim.AntWorkState;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyRequest;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.world.ColonyBuilder;
import com.formicfrontier.world.ColonyService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class AntEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> CASTE = SynchedEntityData.defineId(AntEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COLONY_ID = SynchedEntityData.defineId(AntEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WORK_STATE = SynchedEntityData.defineId(AntEntity.class, EntityDataSerializers.INT);

	public AntEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	public static AttributeSupplier.Builder createAntAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, AntCaste.WORKER.health())
				.add(Attributes.MOVEMENT_SPEED, AntCaste.WORKER.speed())
				.add(Attributes.ATTACK_DAMAGE, AntCaste.WORKER.damage())
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new AntWorkGoal(this));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.75));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<AntEntity>(this, AntEntity.class, 10, true, false, (target, serverLevel) -> target instanceof AntEntity other && ColonyService.areHostile(serverLevel, colonyId(), other.colonyId())));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CASTE, AntCaste.WORKER.ordinal());
		builder.define(COLONY_ID, 0);
		builder.define(WORK_STATE, AntWorkState.IDLE.ordinal());
	}

	public AntCaste caste() {
		int ordinal = entityData.get(CASTE);
		AntCaste[] values = AntCaste.values();
		if (ordinal < 0 || ordinal >= values.length) {
			return AntCaste.WORKER;
		}
		return values[ordinal];
	}

	public void setCaste(AntCaste caste) {
		entityData.set(CASTE, caste.ordinal());
		applyCasteAttributes(caste);
		refreshDimensions();
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		AntCaste caste = caste();
		return EntityDimensions.scalable(caste.width(), caste.height());
	}

	public int colonyId() {
		return entityData.get(COLONY_ID);
	}

	public void setColonyId(int colonyId) {
		entityData.set(COLONY_ID, Math.max(0, colonyId));
	}

	public AntWorkState workState() {
		int ordinal = entityData.get(WORK_STATE);
		AntWorkState[] values = AntWorkState.values();
		if (ordinal < 0 || ordinal >= values.length) {
			return AntWorkState.IDLE;
		}
		return values[ordinal];
	}

	public void setWorkState(AntWorkState state) {
		entityData.set(WORK_STATE, state.ordinal());
	}

	private boolean isHostileTo(AntEntity other) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		return ColonyService.areHostile(serverLevel, colonyId(), other.colonyId());
	}

	@Override
	public void tick() {
		super.tick();
		if (tickCount % 40 == 0) {
			applyCasteAttributes(caste());
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide()) {
			if (player instanceof ServerPlayer serverPlayer && ColonyService.handleAntInteraction(serverPlayer, this, hand)) {
				return InteractionResult.SUCCESS_SERVER;
			}
			if (player instanceof ServerPlayer serverPlayer && level() instanceof ServerLevel serverLevel) {
				ColonyService.nearestColony(serverLevel, blockPosition(), 96).ifPresent(colony ->
						ColonyService.openColonyScreen(serverPlayer, colony, "Overview", "Formic " + caste().id() + " | hp " + Math.round(getHealth()) + "/" + Math.round(getMaxHealth()))
				);
			}
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	private void applyCasteAttributes(AntCaste caste) {
		if (getAttribute(Attributes.MAX_HEALTH) != null) {
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(caste.health());
		}
		if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(caste.speed());
		}
		if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(caste.damage());
		}
		if (getHealth() > getMaxHealth()) {
			setHealth(getMaxHealth());
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.putString("caste", caste().id());
		valueOutput.putInt("colonyId", colonyId());
		valueOutput.putString("workState", workState().id());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		setCaste(AntCaste.fromId(valueInput.getStringOr("caste", AntCaste.WORKER.id())));
		setColonyId(valueInput.getIntOr("colonyId", 0));
		setWorkState(AntWorkState.fromId(valueInput.getStringOr("workState", AntWorkState.IDLE.id())));
	}

	public static Optional<BlockPos> debugWorkTarget(ServerLevel level, AntCaste caste, int colonyId, BlockPos antPos) {
		return AntWorkGoal.createAssignment(level, caste, colonyId, antPos, 0).map(AntWorkAssignment::target);
	}

	private static final class AntWorkGoal extends Goal {
		private final AntEntity ant;
		private AntWorkAssignment assignment;
		private BlockPos target;
		private BlockPos deliveryTarget;
		private ResourceType carriedResource;
		private int carriedAmount;
		private int workTicks;
		private int workDuration;
		private boolean delivering;

		private AntWorkGoal(AntEntity ant) {
			this.ant = ant;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (ant.caste() == AntCaste.QUEEN || ant.getRandom().nextInt(6) != 0) {
				return false;
			}
			if (!(ant.level() instanceof ServerLevel serverLevel)) {
				return false;
			}
			Optional<AntWorkAssignment> created = createAssignment(serverLevel, ant.caste(), ant.colonyId(), ant.blockPosition(), ant.getRandom().nextInt(100));
			if (created.isEmpty()) {
				return false;
			}
			assignment = created.get();
			target = assignment.target();
			deliveryTarget = assignment.deliveryTarget();
			carriedResource = assignment.resource();
			carriedAmount = assignment.amount();
			workDuration = assignment.duration();
			workTicks = 0;
			delivering = false;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && assignment != null && ant.isAlive();
		}

		@Override
		public void start() {
			ant.setWorkState(assignment.kind() == AntWorkKind.PATROL ? AntWorkState.PATROLLING : AntWorkState.WORKING);
			moveToTarget();
		}

		@Override
		public void tick() {
			if (target == null || assignment == null) {
				return;
			}
			ant.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.35, target.getZ() + 0.5);
			if (!isNearTarget()) {
				if (ant.getNavigation().isDone()) {
					moveToTarget();
				}
				return;
			}

			ant.getNavigation().stop();
			workTicks++;
			if (workTicks < workDuration) {
				if (workTicks % 8 == 0) {
					ant.swing(InteractionHand.MAIN_HAND);
				}
				return;
			}

			if (assignment.kind() == AntWorkKind.PATROL) {
				setPatrolTask();
				target = null;
				return;
			}
			if (assignment.kind() == AntWorkKind.CONSTRUCTION) {
				depositConstructionWork();
				target = null;
				return;
			}
			if (!delivering && deliveryTarget != null) {
				delivering = true;
				workTicks = 0;
				target = deliveryTarget;
				ant.setWorkState(AntWorkState.carrying(carriedResource));
				moveToTarget();
				return;
			}
			depositCarriedResource();
			ant.setWorkState(AntWorkState.IDLE);
			target = null;
		}

		@Override
		public void stop() {
			assignment = null;
			target = null;
			deliveryTarget = null;
			carriedResource = null;
			carriedAmount = 0;
			workTicks = 0;
			workDuration = 0;
			delivering = false;
			ant.setWorkState(AntWorkState.IDLE);
		}

		private static Optional<AntWorkAssignment> createAssignment(ServerLevel level, AntCaste caste, int colonyId, BlockPos antPos, int selector) {
			Optional<ColonyData> colony = ColonyService.colony(level, colonyId).or(() -> ColonyService.nearestColony(level, antPos, 128));
			if (colony.isEmpty()) {
				return fallbackAssignment(level, caste, antPos, selector);
			}
			return switch (caste) {
				case MINER -> Optional.of(resourceAssignment(level, colony.get(), ResourceType.ORE, BuildingType.MINE, 5, 30));
				case WORKER -> workerAssignment(level, colony.get(), selector);
				case SCOUT -> Optional.of(resourceAssignment(level, colony.get(), ResourceType.FOOD, BuildingType.QUEEN_CHAMBER, 2, 22));
				case SOLDIER, MAJOR, GIANT -> Optional.of(patrolAssignment(colony.get(), selector));
				case QUEEN -> Optional.empty();
			};
		}

		private static Optional<AntWorkAssignment> workerAssignment(ServerLevel level, ColonyData colony, int selector) {
			Optional<ColonyRequest> request = colony.progress().requestsView().stream()
					.filter(entry -> !entry.complete())
					.sorted((first, second) -> Integer.compare(second.missing(), first.missing()))
					.findFirst();
			if (request.isPresent()) {
				ColonyRequest entry = request.get();
				return Optional.of(new AntWorkAssignment(
						AntWorkKind.LOGISTICS,
						resourceSource(level, colony, entry.resource()),
						buildingTarget(colony, entry.building()),
						entry.resource(),
						Math.max(1, Math.min(4, entry.missing())),
						24
				));
			}
			Optional<ColonyBuilding> active = colony.progress().firstIncomplete();
			if (active.isPresent()) {
				return Optional.of(new AntWorkAssignment(AntWorkKind.CONSTRUCTION, active.get().pos(), null, ResourceType.RESIN, 6, 30));
			}
			return Optional.of(switch (selector % 5) {
				case 0 -> resourceAssignment(level, colony, ResourceType.FOOD, BuildingType.FOOD_STORE, 4, 24);
				case 1 -> resourceAssignment(level, colony, ResourceType.CHITIN, BuildingType.NURSERY, 2, 24);
				case 2 -> resourceAssignment(level, colony, ResourceType.RESIN, BuildingType.RESIN_DEPOT, 2, 24);
				case 3 -> resourceAssignment(level, colony, ResourceType.FUNGUS, BuildingType.NURSERY, 2, 24);
				default -> resourceAssignment(level, colony, ResourceType.VENOM, BuildingType.ARMORY, 1, 24);
			});
		}

		private static AntWorkAssignment resourceAssignment(ServerLevel level, ColonyData colony, ResourceType resourceType, BuildingType deliveryType, int amount, int duration) {
			return new AntWorkAssignment(AntWorkKind.RESOURCE, resourceSource(level, colony, resourceType), buildingTarget(colony, deliveryType), resourceType, amount, duration);
		}

		private static AntWorkAssignment patrolAssignment(ColonyData colony, int selector) {
			List<BlockPos> patrols = colony.progress().buildingsView().stream()
					.filter(building -> building.complete() && isPatrolBuilding(building.type()))
					.sorted((first, second) -> Integer.compare(patrolPriority(first.type()), patrolPriority(second.type())))
					.map(ColonyBuilding::pos)
					.toList();
			BlockPos patrol = patrols.isEmpty() ? colony.origin() : patrols.get(Math.floorMod(selector, patrols.size()));
			return new AntWorkAssignment(AntWorkKind.PATROL, patrol, null, null, 0, 18);
		}

		private static boolean isPatrolBuilding(BuildingType type) {
			return switch (type) {
				case QUEEN_CHAMBER, BARRACKS, WATCH_POST, ROAD -> true;
				default -> false;
			};
		}

		private static int patrolPriority(BuildingType type) {
			return switch (type) {
				case BARRACKS -> 0;
				case WATCH_POST -> 1;
				case ROAD -> 2;
				case QUEEN_CHAMBER -> 3;
				default -> 4;
			};
		}

		private static Optional<AntWorkAssignment> fallbackAssignment(ServerLevel level, AntCaste caste, BlockPos antPos, int selector) {
			return switch (caste) {
				case MINER -> fallbackResource(level, antPos, ModBlocks.ORE_NODE, ModBlocks.MINE_CHAMBER, ResourceType.ORE, 5);
				case WORKER, SCOUT -> fallbackResource(level, antPos, ModBlocks.FOOD_NODE, ModBlocks.FOOD_CHAMBER, ResourceType.FOOD, 2);
				case SOLDIER, MAJOR, GIANT -> findTarget(level, antPos, ModBlocks.BARRACKS_CHAMBER, 48)
						.or(() -> findTarget(level, antPos, ModBlocks.NEST_MOUND, 48))
						.map(pos -> new AntWorkAssignment(AntWorkKind.PATROL, pos, null, null, 0, 18));
				case QUEEN -> Optional.empty();
			};
		}

		private static Optional<AntWorkAssignment> fallbackResource(ServerLevel level, BlockPos antPos, Block resourceBlock, Block deliveryBlock, ResourceType resourceType, int amount) {
			return findTarget(level, antPos, resourceBlock, 48)
					.map(resource -> new AntWorkAssignment(AntWorkKind.RESOURCE, resource, findTarget(level, antPos, deliveryBlock, 48).orElse(null), resourceType, amount, 24));
		}

		private static BlockPos resourceSource(ServerLevel level, ColonyData colony, ResourceType resourceType) {
			BlockPos expected = switch (resourceType) {
				case FOOD -> colony.origin().offset(32, 0, 4);
				case ORE -> colony.origin().offset(4, 0, 32);
				case CHITIN -> colony.origin().offset(-32, 0, 4);
				case RESIN -> buildingTarget(colony, BuildingType.RESIN_DEPOT);
				case FUNGUS -> buildingTarget(colony, BuildingType.FUNGUS_GARDEN);
				case VENOM -> buildingTarget(colony, BuildingType.VENOM_PRESS);
				case KNOWLEDGE -> buildingTarget(colony, BuildingType.PHEROMONE_ARCHIVE);
			};
			Block wanted = switch (resourceType) {
				case FOOD -> ModBlocks.FOOD_NODE;
				case ORE -> ModBlocks.ORE_NODE;
				case CHITIN -> ModBlocks.CHITIN_NODE;
				case RESIN -> ModBlocks.RESIN_DEPOT;
				case FUNGUS -> ModBlocks.FUNGUS_GARDEN;
				case VENOM -> ModBlocks.VENOM_PRESS;
				case KNOWLEDGE -> ModBlocks.PHEROMONE_ARCHIVE;
			};
			return findTarget(level, expected, wanted, 6).orElse(expected);
		}

		private static BlockPos buildingTarget(ColonyData colony, BuildingType type) {
			return colony.progress().buildingsView().stream()
					.filter(building -> building.type() == type)
					.findFirst()
					.map(ColonyBuilding::pos)
					.orElseGet(() -> ColonyBuilder.siteFor(colony, type));
		}

		private static Optional<BlockPos> findTarget(ServerLevel level, BlockPos origin, Block wanted, int radius) {
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-radius, -3, -radius), origin.offset(radius, 4, radius))) {
				if (!level.getBlockState(candidate).is(wanted)) {
					continue;
				}
				double distance = candidate.distSqr(origin);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = candidate.immutable();
				}
			}
			return Optional.ofNullable(best);
		}

		private void moveToTarget() {
			if (target != null) {
				ant.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, Math.max(0.8, ant.caste().speed() * 4.0));
			}
		}

		private boolean isNearTarget() {
			return ant.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) <= 3.8;
		}

		private void depositCarriedResource() {
			if (ant.level() instanceof ServerLevel serverLevel && carriedResource != null && carriedAmount > 0) {
				ColonyService.depositWorkedResource(serverLevel, ant.blockPosition(), ant.colonyId(), ant.caste(), carriedResource, carriedAmount);
			}
		}

		private void depositConstructionWork() {
			if (ant.level() instanceof ServerLevel serverLevel) {
				ColonyService.depositConstructionWork(serverLevel, ant.blockPosition(), ant.colonyId(), ant.caste(), carriedAmount);
			}
			ant.setWorkState(AntWorkState.IDLE);
		}

		private void setPatrolTask() {
			if (ant.level() instanceof ServerLevel serverLevel) {
				ColonyService.colony(serverLevel, ant.colonyId())
						.or(() -> ColonyService.nearestColony(serverLevel, ant.blockPosition(), 128))
						.ifPresent(colony -> colony.setCurrentTask(ant.caste().id() + " patrols " + target.toShortString()));
			}
		}
	}

	private enum AntWorkKind {
		RESOURCE,
		LOGISTICS,
		CONSTRUCTION,
		PATROL
	}

	private record AntWorkAssignment(AntWorkKind kind, BlockPos target, BlockPos deliveryTarget, ResourceType resource, int amount, int duration) {
	}
}
