package com.formicfrontier.qa;

import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.world.ColonyBuilder;
import com.formicfrontier.world.ColonySavedState;
import com.formicfrontier.world.ColonyService;
import com.formicfrontier.world.StructurePlacer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class VisualQaScenes {
	public static final String COLONY_OVERVIEW = "colony_overview";
	public static final String COLONY_GROUND = "colony_ground";
	public static final String ANT_LINEUP = "ant_lineup";
	public static final String TABLET_EN = "tablet_en";
	public static final String TABLET_RU = "tablet_ru";
	public static final String PROGRESSION_SCENE = "progression_scene";
	private static final List<String> SCENES = List.of(COLONY_OVERVIEW, COLONY_GROUND, ANT_LINEUP, TABLET_EN, TABLET_RU, PROGRESSION_SCENE);
	private static final Set<BuildingType> ADVANCED_BUILDINGS = Set.of(
			BuildingType.MARKET,
			BuildingType.PHEROMONE_ARCHIVE,
			BuildingType.RESIN_DEPOT,
			BuildingType.FUNGUS_GARDEN,
			BuildingType.ARMORY,
			BuildingType.WATCH_POST,
			BuildingType.DIPLOMACY_SHRINE,
			BuildingType.VENOM_PRESS
	);

	private VisualQaScenes() {
	}

	public static List<String> scenes() {
		return SCENES;
	}

	public static int run(CommandSourceStack source, String sceneName) {
		String normalized = sceneName.toLowerCase(Locale.ROOT);
		if (!SCENES.contains(normalized)) {
			source.sendFailure(Component.literal("Unknown Formic visual QA scene: " + sceneName));
			return 0;
		}

		ServerLevel level = source.getLevel();
		BlockPos requested = BlockPos.containing(source.getPosition());
		BlockPos origin = ColonyService.anchorToSurface(level, requested);
		prepareFlatQaArea(level, origin, 58);
		level.setDayTime(6000);
		level.setWeatherParameters(0, 0, false, false);

		ColonySavedState savedState = ColonySavedState.get(source.getServer());
		savedState.clearColonies();
		ColonyData colony = ColonyService.createColony(level, origin, true);
		seedVisualState(level, colony, normalized);

		if (normalized.equals(ANT_LINEUP)) {
			spawnAntLineup(level, origin, colony.id());
		}

		ServerPlayer player = source.getPlayer();
		if (player != null) {
			positionCamera(player, origin, normalized);
			if (normalized.equals(TABLET_EN) || normalized.equals(TABLET_RU)) {
				ColonyService.openColonyScreen(player, colony, "Overview", "Visual QA scene: " + normalized);
			}
		}

		savedState.setDirty();
		return 1;
	}

	private static void seedVisualState(ServerLevel level, ColonyData colony, String sceneName) {
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 240);
		}
		colony.addCaste(AntCaste.WORKER, 4);
		colony.addCaste(AntCaste.SCOUT, 2);
		colony.addCaste(AntCaste.MINER, 3);
		colony.addCaste(AntCaste.SOLDIER, 3);

		if (!sceneName.equals(PROGRESSION_SCENE) && !sceneName.startsWith("tablet")) {
			return;
		}
		for (BuildingType type : ADVANCED_BUILDINGS) {
			if (colony.progress().hasCompleted(type)) {
				continue;
			}
			BlockPos pos = ColonyBuilder.siteFor(colony, type);
			colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
			StructurePlacer.placeBuilding(level, pos, type);
		}
		colony.addEvent("Visual QA progression state seeded");
		colony.setCurrentTask("Visual QA: readable tablet and campus review");
	}

	private static void spawnAntLineup(ServerLevel level, BlockPos origin, int colonyId) {
		int index = 0;
		for (AntCaste caste : AntCaste.values()) {
			BlockPos pos = origin.offset(-18 + index * 6, 1, -18);
			AntEntity ant = ColonyService.spawnAnt(level, pos, caste, colonyId);
			if (ant != null) {
				ant.setCustomName(Component.literal(caste.id()));
				ant.setCustomNameVisible(true);
			}
			index++;
		}
	}

	private static void positionCamera(ServerPlayer player, BlockPos origin, String sceneName) {
		player.setGameMode(GameType.SPECTATOR);
		Vec3 target = switch (sceneName) {
			case COLONY_OVERVIEW -> Vec3.atCenterOf(origin).add(0.0, 4.0, 0.0);
			case ANT_LINEUP -> Vec3.atCenterOf(origin.offset(0, 2, -18));
			case TABLET_EN, TABLET_RU -> Vec3.atCenterOf(origin).add(0.0, 3.0, 0.0);
			case PROGRESSION_SCENE -> Vec3.atCenterOf(origin).add(0.0, 5.0, 0.0);
			default -> Vec3.atCenterOf(origin).add(0.0, 2.0, 0.0);
		};
		Vec3 camera = switch (sceneName) {
			case COLONY_GROUND -> new Vec3(origin.getX() + 18.0, origin.getY() + 7.0, origin.getZ() - 32.0);
			case ANT_LINEUP -> new Vec3(origin.getX() + 0.0, origin.getY() + 5.0, origin.getZ() - 36.0);
			case TABLET_EN, TABLET_RU -> new Vec3(origin.getX() + 12.0, origin.getY() + 5.0, origin.getZ() - 18.0);
			case PROGRESSION_SCENE -> new Vec3(origin.getX() + 35.0, origin.getY() + 32.0, origin.getZ() - 38.0);
			default -> new Vec3(origin.getX() + 40.0, origin.getY() + 24.0, origin.getZ() - 44.0);
		};
		player.teleportTo(camera.x, camera.y, camera.z);
		player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
	}

	private static void prepareFlatQaArea(ServerLevel level, BlockPos origin, int radius) {
		AABB cleanup = new AABB(
				origin.getX() - radius, origin.getY() - 8, origin.getZ() - radius,
				origin.getX() + radius, origin.getY() + 96, origin.getZ() + radius
		);
		level.getEntitiesOfClass(AntEntity.class, cleanup).forEach(AntEntity::discard);
		level.getEntitiesOfClass(Display.TextDisplay.class, cleanup).forEach(Display.TextDisplay::discard);
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				BlockPos ground = origin.offset(x, 0, z);
				level.setBlock(ground.below(2), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 96; y++) {
					level.setBlock(ground.above(y), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}
}
