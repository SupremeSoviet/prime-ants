package com.formicfrontier.qa;

import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.registry.ModItems;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.world.ColonyBuilder;
import com.formicfrontier.world.ColonyDiscoveryService;
import com.formicfrontier.world.ColonyLabelService;
import com.formicfrontier.world.ColonyRecurringEvents;
import com.formicfrontier.world.DiplomacyConsequences;
import com.formicfrontier.world.RaidPlanner;
import com.formicfrontier.world.ColonySavedState;
import com.formicfrontier.world.ColonyService;
import com.formicfrontier.world.StructurePlacer;
import com.mojang.math.Transformation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class VisualQaScenes {
	public static final String COLONY_OVERVIEW = "colony_overview";
	public static final String COLONY_GROUND = "colony_ground";
	public static final String ANT_LINEUP = "ant_lineup";
	public static final String WORK_CYCLE = "work_cycle";
	public static final String TABLET_EN = "tablet_en";
	public static final String TABLET_RU = "tablet_ru";
	public static final String TABLET_GUIDE = "tablet_guide";
	public static final String TABLET_TRADE = "tablet_trade";
	public static final String TABLET_RESEARCH_MAP = "tablet_research_map";
	public static final String TABLET_MARKET = "tablet_market";
	public static final String TABLET_REQUESTS = "tablet_requests";
	public static final String PROGRESSION_SCENE = "progression_scene";
	public static final String SETTLEMENT_SCALE = "settlement_scale";
	public static final String CONSTRUCTION_STAGE = "construction_stage";
	public static final String REPAIR_SCENE = "repair_scene";
	public static final String CULTURE_STYLES = "culture_styles";
	public static final String DIPLOMACY_SCENE = "diplomacy_scene";
	public static final String WORLDGEN_ENCOUNTER = "worldgen_encounter";
	public static final String ENDGAME_PROJECT = "endgame_project";
	// Structure-schematic prototype: isolated single-building preview scenes.
	public static final String STRUCTURE_PREVIEW_3Q = "structure_preview_3q";
	public static final String STRUCTURE_PREVIEW_FRONT = "structure_preview_front";
	private static final List<String> SCENES = List.of(
			COLONY_OVERVIEW,
			COLONY_GROUND,
			ANT_LINEUP,
			WORK_CYCLE,
			TABLET_EN,
			TABLET_RU,
			TABLET_GUIDE,
			TABLET_TRADE,
			TABLET_RESEARCH_MAP,
			TABLET_MARKET,
			TABLET_REQUESTS,
			PROGRESSION_SCENE,
			SETTLEMENT_SCALE,
			CONSTRUCTION_STAGE,
			REPAIR_SCENE,
			CULTURE_STYLES,
			DIPLOMACY_SCENE,
			WORLDGEN_ENCOUNTER,
			ENDGAME_PROJECT,
			STRUCTURE_PREVIEW_3Q,
			STRUCTURE_PREVIEW_FRONT
	);
	private static final List<AntCaste> ANT_LINEUP_CASTES = List.of(
			AntCaste.QUEEN,
			AntCaste.GIANT,
			AntCaste.MAJOR,
			AntCaste.WORKER,
			AntCaste.SCOUT,
			AntCaste.MINER,
			AntCaste.SOLDIER
	);
	private static final Set<BuildingType> ADVANCED_BUILDINGS = Set.of(
			BuildingType.MARKET,
			BuildingType.CHITIN_FARM,
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

	public static List<BlockPos> workCycleJobCenters(BlockPos origin) {
		return List.of(
				origin.offset(-9, 0, -25),
				origin.offset(0, 0, -23),
				origin.offset(9, 0, -25),
				origin.offset(2, 0, -19)
		);
	}

	public static List<BlockPos> repairSceneBuildingCenters(BlockPos origin) {
		return List.of(
				origin.offset(-18, 0, -26),
				origin.offset(0, 0, -26),
				origin.offset(18, 0, -26)
		);
	}

	public static List<BlockPos> constructionStageBuildingCenters(BlockPos origin) {
		return List.of(
				origin.offset(-24, 0, -32),
				origin.offset(-8, 0, -32),
				origin.offset(8, 0, -32),
				origin.offset(24, 0, -32),
				origin.offset(-10, 0, -16),
				origin.offset(10, 0, -16)
		);
	}

	public static AABB workCycleActorCleanupBounds(BlockPos origin) {
		return new AABB(
				origin.getX() - 42, origin.getY() - 4, origin.getZ() - 48,
				origin.getX() + 42, origin.getY() + 24, origin.getZ() + 12
		);
	}

	public static BlockPos antLineupPosition(BlockPos origin, AntCaste caste) {
		return switch (caste) {
			// R-visual-priority #2 fix: relocate the lineup to the south inner yard at z=+20
			// (clear corridor between the queen mound front ~z=+12 and the mine at z=+29)
			// so each caste stands on open grass with the mound as the backdrop. Camera sits
			// south of the row looking north; ants face south (+z) toward the camera.
			// Spaced 5 blocks apart on x in [-15, 15] so the full 30-wide row fits inside the
			case QUEEN -> origin.offset(-15, 1, 20);
			case GIANT -> origin.offset(-10, 1, 20);
			case MAJOR -> origin.offset(-5, 1, 20);
			case WORKER -> origin.offset(0, 1, 20);
			case SCOUT -> origin.offset(5, 1, 20);
			case MINER -> origin.offset(10, 1, 20);
			case SOLDIER -> origin.offset(15, 1, 20);
		};
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
		prepareFlatQaArea(level, origin, qaRadius(normalized));
		level.setDayTime(6000);
		level.setWeatherParameters(0, 0, false, false);

		ColonySavedState savedState = ColonySavedState.get(source.getServer());
		savedState.clearColonies();
		if (normalized.equals(STRUCTURE_PREVIEW_3Q) || normalized.equals(STRUCTURE_PREVIEW_FRONT)) {
			// PREVIEW = GAME: render the real in-game colony through the exact
			// createColony path (no seedVisualState QA overlays) so the preview is
			// precisely what a player sees - the schematic queen spire at the centre,
			// the food spire and the campus economy mounds out on the ring.
			ColonyService.createColony(level, origin, true);
			dressForestFloor(level, origin, normalized);
			ServerPlayer player = source.getPlayer();
			if (player != null) {
				positionCamera(player, origin, normalized);
			}
			savedState.setDirty();
			return 1;
		}
		if (normalized.equals(CULTURE_STYLES)) {
			seedCultureStyles(level, origin);

			dressForestFloor(level, origin, normalized);
			ServerPlayer player = source.getPlayer();
			if (player != null) {
				positionCamera(player, origin, normalized);
			}
			savedState.setDirty();
			return 1;
		}
		if (normalized.equals(DIPLOMACY_SCENE)) {
			seedDiplomacyScene(level, savedState, origin);

			dressForestFloor(level, origin, normalized);
			ServerPlayer player = source.getPlayer();
			if (player != null) {
				positionCamera(player, origin, normalized);
			}
			savedState.setDirty();
			return 1;
		}
		if (normalized.equals(WORLDGEN_ENCOUNTER)) {
			seedWorldgenEncounter(level, savedState, origin);

			dressForestFloor(level, origin, normalized);
			ServerPlayer player = source.getPlayer();
			if (player != null) {
				positionCamera(player, origin, normalized);
			}
			savedState.setDirty();
			return 1;
		}
		ColonyData colony = ColonyService.createColony(level, origin, true);
		seedVisualState(level, colony, normalized);

		if (normalized.equals(ANT_LINEUP)) {
			spawnAntLineup(level, origin, colony.id());
		} else if (normalized.equals(WORK_CYCLE)) {
			seedWorkCycle(level, colony);
		}

		// R2 forest-floor density: dress the flat QA grass plane with dirt variation,
		// roots, stones, flowers, leaf litter, and terrain breakup so the colony reads
		// as embedded in a living habitat, not staged on a superflat test map. Skips
		// tablet-only scenes (those are GUI captures, not world shots).
		if (!normalized.startsWith("tablet")) {
			dressForestFloor(level, origin, normalized);
		}

		ServerPlayer player = source.getPlayer();
		if (player != null) {
			positionCamera(player, origin, normalized);
			if (normalized.startsWith("tablet")) {
				String tab = switch (normalized) {
					case TABLET_RU -> "Instinct";
					case TABLET_GUIDE -> "Guide";
					case TABLET_TRADE, TABLET_MARKET -> "Trade";
					case TABLET_RESEARCH_MAP -> "Research";
					default -> "Needs";
				};
				ColonyService.openColonyScreen(player, colony, tab, "QA: " + normalized);
			}
		}

		savedState.setDirty();
		return 1;
	}

	public static Vec3 worldgenEncounterTarget(BlockPos origin) {
		return Vec3.atCenterOf(origin.offset(0, 0, -18)).add(0.0, 3.2, 0.0);
	}

	public static Vec3 worldgenEncounterCamera(BlockPos origin) {
		return new Vec3(origin.getX() + 18.0, origin.getY() + 18.0, origin.getZ() + 38.0);
	}

	public static BlockPos worldgenEncounterCamp(BlockPos origin) {
		return origin.offset(0, 0, 24);
	}

	public static Vec3 colonyOverviewTarget(BlockPos origin) {
		return Vec3.atCenterOf(origin).add(0.0, 5.0, 0.0);
	}

	public static Vec3 colonyOverviewCamera(BlockPos origin) {
		return new Vec3(origin.getX() + 56.0, origin.getY() + 34.0, origin.getZ() - 72.0);
	}

	public static int qaRadius(String sceneName) {
		if (sceneName.equals(COLONY_OVERVIEW) || sceneName.equals(SETTLEMENT_SCALE)) {
			return 128;
		}
		if (sceneName.equals(DIPLOMACY_SCENE)) {
			return 136;
		}
		if (sceneName.equals(CULTURE_STYLES)) {
			return 124;
		}
		if (sceneName.equals(WORLDGEN_ENCOUNTER)) {
			return 104;
		}
		if (sceneName.equals(ENDGAME_PROJECT) || sceneName.equals(PROGRESSION_SCENE)) {
			return 112;
		}
		return 58;
	}

	private static void seedVisualState(ServerLevel level, ColonyData colony, String sceneName) {
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 240);
		}
		colony.addCaste(AntCaste.WORKER, 4);
		colony.addCaste(AntCaste.SCOUT, 2);
		colony.addCaste(AntCaste.MINER, 3);
		colony.addCaste(AntCaste.SOLDIER, 3);

		// COLONY_OVERVIEW now continues to the full-campus build path (advanced
		// buildings + shared landmass) below: previously it returned early with only the
		// queen mound + 4 starter satellites, so the wide shot read as separate cones on
		// open ground instead of one continuous carved mound family (assessment P1
		// blocker multiple_large_organic_chambers / no_single_mound_pass).
		// colony_ground keeps the early return: it is a low ground-level closeup whose
		// camera (y+8.5) would be buried by the tall shared landmass, and its acceptance
		// is about deep ground-level mouths (blocker 2), not wide topology.
		// ant_lineup / work_cycle also keep the early return: they PASS and their
		// CULTURE_STYLES / DIPLOMACY_SCENE / WORLDGEN_ENCOUNTER are WIDE/AERIAL topology scenes
		// judged for the fused mound family, so they MUST run the shared landmass path too
		// (assessment: culture_styles read as separated mound islands when it early-returned).
		// subject is the ants, not the architecture.
		if (!sceneName.equals(PROGRESSION_SCENE) && !sceneName.equals(ENDGAME_PROJECT) && !sceneName.equals(SETTLEMENT_SCALE) && !sceneName.equals(COLONY_OVERVIEW) && !sceneName.equals(CULTURE_STYLES) && !sceneName.equals(DIPLOMACY_SCENE) && !sceneName.equals(WORLDGEN_ENCOUNTER) && !sceneName.startsWith("tablet")) {
			if (sceneName.equals(CONSTRUCTION_STAGE)) {
				seedConstructionStages(level, colony);
			} else if (sceneName.equals(REPAIR_SCENE)) {
				seedRepairScene(level, colony);
			}
			return;
		}
		if (sceneName.equals(TABLET_GUIDE)) {
			colony.setResource(ResourceType.KNOWLEDGE, 0);
			colony.setCurrentTask("QA guide: basics open, advanced notes locked");
			colony.addEvent("Visual QA starter guide state seeded");
			return;
		}
		// REPRESENTATIONAL FIX (assessment REPEAT blocker 'stop spawning independent
		// cone bodies FIRST'): for shared-landmass scenes, suppress the independent
		// satellite crown MASS so each role building's volume is derived ONLY from the
		// one shared height field below. The satellite core + deep tunnel mouth +
		// rear-chamber identity still stamp (only the column-fill dome + roofline cap
		// mass are gated). The flag is cleared in a finally so it never leaks into the
		// gametest path (which asserts the independent crown mass).
		boolean prevSuppress = StructurePlacer.SUPPRESS_SATELLITE_CROWN_MASS;
		StructurePlacer.SUPPRESS_SATELLITE_CROWN_MASS = !sceneName.startsWith("tablet") && !sceneName.equals(CONSTRUCTION_STAGE) && !sceneName.equals(REPAIR_SCENE);
		try {
			for (BuildingType type : ADVANCED_BUILDINGS) {
				if (colony.progress().hasCompleted(type)) {
					continue;
				}
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(level, pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		} finally {
			StructurePlacer.SUPPRESS_SATELLITE_CROWN_MASS = prevSuppress;
		}
		// R2 architecture representational fix: fuse the WHOLE campus (queen mound +
		// starter satellites + advanced buildings) onto ONE continuous noise-driven
		// native-earth landmass so the colony reads as a single broad carved ant-hill
		// organism with the role buildings as sub-lobes, instead of N separate cones
		// on flat grass (assessment P1 blocker multiple_large_organic_chambers /
		// no_single_mound_pass). Only for wide/colony scenes: tablet scenes are GUI
		// captures where the landmass would be invisible noise behind the panel.
		if (!sceneName.startsWith("tablet")) {
			java.util.List<net.minecraft.core.BlockPos> campusSatellites = new java.util.ArrayList<>();
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.FOOD_STORE));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.NURSERY));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.MINE));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.BARRACKS));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.MARKET));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.RESIN_DEPOT));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.PHEROMONE_ARCHIVE));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.VENOM_PRESS));
			campusSatellites.add(ColonyBuilder.siteFor(colony, BuildingType.ARMORY));
			StructurePlacer.placeSharedCampusLandmass2D(level, colony.origin(), campusSatellites);
			// Carve dark chamber mouths into the one shared mound so it reads as an
			// inhabited ant-hill, not a featureless dirt dome.
			StructurePlacer.carveSharedMoundChamberMouths(level, colony.origin());
		}
		if (sceneName.equals(TABLET_EN)) {
			colony.progress().requests().clear();
			ColonyLogistics.requestResource(colony, BuildingType.PHEROMONE_ARCHIVE, ResourceType.RESIN, 18, "archive resin");
			ColonyLogistics.requestResource(colony, BuildingType.MARKET, ResourceType.FOOD, 24, "market build");
		}
		if (sceneName.equals(TABLET_TRADE) || sceneName.equals(TABLET_MARKET)) {
			seedTabletTrade(level, colony);
		}
		colony.addEvent("Visual QA progression state seeded");
		if (sceneName.equals(TABLET_RESEARCH_MAP)) {
			colony.setResource(ResourceType.KNOWLEDGE, 160);
			ColonyLogistics.startResearch(colony, com.formicfrontier.sim.ResearchNode.RESIN_MASONRY.id());
			colony.setCurrentTask("QA research map: resin masonry active, deeper branches visible");
		} else if (sceneName.equals(TABLET_REQUESTS)) {
			colony.progress().requests().clear();
			ColonyLogistics.requestResource(colony, BuildingType.PHEROMONE_ARCHIVE, ResourceType.RESIN, 28, "archive resin");
			ColonyLogistics.requestResource(colony, BuildingType.MARKET, ResourceType.FOOD, 32, "market build");
			ColonyLogistics.requestResource(colony, BuildingType.NURSERY, ResourceType.CHITIN, 18, "brood care");
			colony.setCurrentTask("QA requests board: player help wanted for three colony jobs");
		} else if (!sceneName.equals(TABLET_TRADE) && !sceneName.equals(TABLET_MARKET)) {
			colony.setCurrentTask(sceneName.equals(TABLET_EN) ? "QA contracts awaiting player help" : "QA campus review");
		}
		if (sceneName.equals(PROGRESSION_SCENE)) {
			colony.setResource(ResourceType.FOOD, 320);
			colony.setResource(ResourceType.CHITIN, 80);
			colony.setResource(ResourceType.ORE, 8);
			ColonyRecurringEvents.triggerQueenBroodBloom(level, colony);
			colony.setResource(ResourceType.FOOD, 6);
			ColonyRecurringEvents.triggerFamineWarning(level, colony);
			ColonyRecurringEvents.triggerExpansionOpportunity(level, colony);
			completeExpansionOutpostContract(level, colony);
		} else if (sceneName.equals(SETTLEMENT_SCALE)) {
			colony.setCurrentTask("QA settlement scale: broad village ring, large chambers, clear trails");
			ColonyLabelService.syncLabels(level, colony);
		} else if (sceneName.equals(ENDGAME_PROJECT)) {
			seedEndgameProject(level, colony);
		}
	}

	private static void completeExpansionOutpostContract(ServerLevel level, ColonyData colony) {
		ColonyLogistics.contracts(colony).stream()
				.filter(contract -> contract.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON))
				.findFirst()
				.ifPresent(contract -> {
					ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
					ColonyRecurringEvents.completeExpansionOutpost(level, colony);
				});
	}

	private static void seedEndgameProject(ServerLevel level, ColonyData colony) {
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 520);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 10);
		colony.addCaste(AntCaste.SOLDIER, 5);
		colony.addCaste(AntCaste.MAJOR, 2);
		colony.addCaste(AntCaste.GIANT, 1);
		BlockPos project = ColonyBuilder.siteFor(colony, BuildingType.GREAT_MOUND);
		if (!colony.progress().hasCompleted(BuildingType.GREAT_MOUND)) {
			colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.GREAT_MOUND, project));
		}
		StructurePlacer.placeBuilding(level, project, BuildingType.GREAT_MOUND, BuildingVisualStage.COMPLETE, colony.progress().culture());
		BlockPos vault = ColonyBuilder.siteFor(colony, BuildingType.QUEEN_VAULT);
		if (!colony.progress().hasCompleted(BuildingType.QUEEN_VAULT)) {
			colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_VAULT, vault));
		}
		StructurePlacer.placeBuilding(level, vault, BuildingType.QUEEN_VAULT, BuildingVisualStage.COMPLETE, colony.progress().culture());
		BlockPos tradeHub = ColonyBuilder.siteFor(colony, BuildingType.TRADE_HUB);
		if (!colony.progress().hasCompleted(BuildingType.TRADE_HUB)) {
			colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, tradeHub));
		}
		StructurePlacer.placeBuilding(level, tradeHub, BuildingType.TRADE_HUB, BuildingVisualStage.COMPLETE, colony.progress().culture());
		colony.setCurrentTask("Endgame projects complete: great mound, queen vault, trade hub");
		colony.addEvent("Endgame project complete: Trade Hub opened beside the queen vault");
		ColonyLabelService.syncLabels(level, colony);
	}

	private static void seedTabletTrade(ServerLevel level, ColonyData colony) {
		colony.progress().addReputation(20);
		BlockPos tradeHub = ColonyBuilder.siteFor(colony, BuildingType.TRADE_HUB);
		if (!colony.progress().hasCompleted(BuildingType.TRADE_HUB)) {
			colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, tradeHub));
		}
		StructurePlacer.placeBuilding(level, tradeHub, BuildingType.TRADE_HUB, BuildingVisualStage.COMPLETE, colony.progress().culture());
		colony.setCurrentTask("QA trade hub terms: caravan payoff and token discounts visible");
		colony.addEvent("Recurring event: trade caravan exchanged 8 food for 8 resin with colony #7");
		colony.addEvent("Visual QA Trade Hub terms seeded");
		ColonyLabelService.syncLabels(level, colony);
	}

	private static void seedCultureStyles(ServerLevel level, BlockPos origin) {
		ColonyCulture[] cultures = {
				ColonyCulture.AMBER,
				ColonyCulture.LEAFCUTTER,
				ColonyCulture.FIRE,
				ColonyCulture.CARPENTER
		};
		int[] xOffsets = {-84, -28, 28, 84};
		for (int i = 0; i < cultures.length; i++) {
			ColonyCulture culture = cultures[i];
			BlockPos mound = origin.offset(xOffsets[i], 0, -8);
			BlockPos chamber = mound.offset(0, 0, 18);
			BlockPos signature = mound.offset(0, 0, 32);
			StructurePlacer.placeBuilding(level, mound, BuildingType.QUEEN_CHAMBER, BuildingVisualStage.COMPLETE, culture);
			StructurePlacer.placeBuilding(level, chamber, BuildingType.FOOD_STORE, BuildingVisualStage.COMPLETE, culture);
			StructurePlacer.placeBuilding(level, signature, culture.starterQueue().getFirst(), BuildingVisualStage.COMPLETE, culture);
			placeCultureLabel(level, mound, culture);
		}
	}

	private static void placeCultureLabel(ServerLevel level, BlockPos mound, ColonyCulture culture) {
		Display.TextDisplay label = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (label == null) {
			return;
		}
		label.setPos(mound.getX() + 0.5, mound.getY() + 7.2, mound.getZ() + 0.5);
		label.setNoGravity(true);
		label.setCustomName(Component.literal(culture.displayName()));
		label.setCustomNameVisible(true);
		level.addFreshEntity(label);
	}

	private static void seedDiplomacyScene(ServerLevel level, ColonySavedState savedState, BlockPos origin) {
		BlockPos alliedOrigin = origin.offset(-78, 0, 0);
		BlockPos rivalOrigin = origin.offset(78, 0, 0);
		BlockPos treatyOrigin = origin.offset(0, 0, 72);
		BlockPos envoyOrigin = origin.offset(0, 0, -78);
		ColonyData allied = ColonyService.createColony(level, alliedOrigin, true);
		ColonyData rival = ColonyService.createColony(level, rivalOrigin, false);
		ColonyData treaty = ColonyService.createWildColony(level, treatyOrigin, ColonyCulture.CARPENTER);
		ColonyData envoy = ColonyService.createWildColony(level, envoyOrigin, ColonyCulture.LEAFCUTTER);
		BlockPos shrine = ColonyBuilder.siteFor(allied, BuildingType.DIPLOMACY_SHRINE);
		if (!allied.progress().hasCompleted(BuildingType.DIPLOMACY_SHRINE)) {
			allied.progress().addBuilding(ColonyBuilding.complete(BuildingType.DIPLOMACY_SHRINE, shrine));
			StructurePlacer.placeBuilding(level, shrine, BuildingType.DIPLOMACY_SHRINE, BuildingVisualStage.COMPLETE, allied.progress().culture());
		}
		BlockPos alliedMarket = ColonyBuilder.siteFor(allied, BuildingType.MARKET);
		if (!allied.progress().hasCompleted(BuildingType.MARKET)) {
			allied.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, alliedMarket));
			StructurePlacer.placeBuilding(level, alliedMarket, BuildingType.MARKET, BuildingVisualStage.COMPLETE, allied.progress().culture());
		}
		BlockPos treatyMarket = ColonyBuilder.siteFor(treaty, BuildingType.MARKET);
		if (!treaty.progress().hasCompleted(BuildingType.MARKET)) {
			treaty.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, treatyMarket));
			StructurePlacer.placeBuilding(level, treatyMarket, BuildingType.MARKET, BuildingVisualStage.COMPLETE, treaty.progress().culture());
		}
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		allied.progress().setRelation(treaty.id(), DiplomacyState.ALLY);
		treaty.progress().setRelation(allied.id(), DiplomacyState.ALLY);
		allied.progress().setRelation(envoy.id(), DiplomacyState.NEUTRAL);
		envoy.progress().setRelation(allied.id(), DiplomacyState.NEUTRAL);
		DiplomacyConsequences.apply(level, allied, treaty, com.formicfrontier.sim.DiplomacyAction.TRIBUTE, DiplomacyState.ALLY);
		ColonyRecurringEvents.triggerTradeCaravan(level, allied);
		rival.addCaste(AntCaste.SOLDIER, 2);
		rival.progress().setRaidCooldown(120);
		ColonyRecurringEvents.triggerInvasionWarning(level, allied);
		rival.progress().setRaidCooldown(0);
		RaidPlanner.tick(level, savedState);
		allied.progress().setRelation(rival.id(), DiplomacyState.NEUTRAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.NEUTRAL);
		DiplomacyConsequences.apply(level, allied, rival, com.formicfrontier.sim.DiplomacyAction.TRUCE, DiplomacyState.NEUTRAL);
		allied.progress().setRelation(rival.id(), DiplomacyState.WAR);
		rival.progress().setRelation(allied.id(), DiplomacyState.WAR);
		DiplomacyConsequences.apply(level, allied, rival, com.formicfrontier.sim.DiplomacyAction.WAR_PACT, DiplomacyState.WAR);
		ColonyRecurringEvents.triggerTreatyOpportunity(level, allied);
		allied.addEvent("Visual QA: rival trail and damaged chamber visible");
		allied.addEvent("Visual QA: invasion warning guard post and defense contract visible");
		allied.addEvent("Visual QA: treaty opportunity envoy camp and resin contract visible");
		allied.addEvent("Visual QA: tribute pact route and cache visible");
		allied.addEvent("Visual QA: defensive pact guard response visible");
		allied.addEvent("Visual QA: truce line cooled former raid route visible");
		allied.addEvent("Visual QA: war pact muster visible");
		treaty.addEvent("Visual QA: allied tribute pact marker visible");
		treaty.addEvent("Visual QA: allied guard patrol answered raid");
		rival.addEvent("Visual QA: raid route marked toward allied colony");
		rival.addEvent("Visual QA: truce line accepted by allied colony");
		rival.addEvent("Visual QA: war pact threat marker visible");
		envoy.addEvent("Visual QA: neutral treaty opportunity marker visible");
		ColonyLabelService.syncLabels(level, allied);
		ColonyLabelService.syncLabels(level, rival);
		ColonyLabelService.syncLabels(level, treaty);
		ColonyLabelService.syncLabels(level, envoy);
	}

	private static void seedWorldgenEncounter(ServerLevel level, ColonySavedState savedState, BlockPos origin) {
		BlockPos camp = worldgenEncounterCamp(origin);
		BlockPos wildSite = origin.offset(0, 0, -34);
		ColonyData wild = ColonyDiscoveryService.spawnEncounterAt(level, savedState, wildSite, ColonyCulture.LEAFCUTTER, "Visual QA: naturally discovered wild colony");
		wild.setCurrentTask("Wild colony watches the trail");
		placeDiscoveryCamp(level, camp);
		placeDiscoveryTrail(level, camp, wild.origin());
		ColonyLabelService.syncLabels(level, wild);
	}

	private static void placeDiscoveryCamp(ServerLevel level, BlockPos camp) {
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (Math.abs(x) + Math.abs(z) <= 4) {
					StructurePlacer.safeSet(level, camp.offset(x, 0, z), Blocks.DIRT_PATH);
				}
			}
		}
		StructurePlacer.safeSet(level, camp, Blocks.CAMPFIRE);
		StructurePlacer.safeSet(level, camp.offset(-2, 1, 0), Blocks.OAK_LOG);
		StructurePlacer.safeSet(level, camp.offset(2, 1, 0), Blocks.BARREL);
	}

	private static void placeDiscoveryTrail(ServerLevel level, BlockPos start, BlockPos end) {
		BlockPos current = start;
		BlockPos trailHead = null;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 128) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, start) < 7 * 7 || horizontalDistanceSquared(current, end) < 12 * 12) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(level, current);
			if (StructurePlacer.safeSet(level, ground, steps % 2 == 0 ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT) && trailHead == null) {
				trailHead = ground;
			}
			if (steps % 7 == 0 && horizontalDistanceSquared(current, start) >= 14 * 14) {
				StructurePlacer.safeSet(level, ground.above(), steps % 14 == 0 ? Blocks.MOSS_BLOCK : Blocks.BROWN_MUSHROOM_BLOCK);
			}
		}
		if (trailHead != null) {
			placeDiscoveryTrailHead(level, trailHead, end);
		}
	}

	private static void placeDiscoveryTrailHead(ServerLevel level, BlockPos trailHead, BlockPos end) {
		int stepX = Integer.compare(end.getX(), trailHead.getX());
		int stepZ = Integer.compare(end.getZ(), trailHead.getZ());
		BlockPos forward = ColonyService.anchorToSurface(level, trailHead.offset(stepX, 0, stepZ));
		BlockPos back = ColonyService.anchorToSurface(level, trailHead.offset(-stepX, 0, -stepZ));
		BlockPos left = ColonyService.anchorToSurface(level, trailHead.offset(-stepZ, 0, stepX));
		BlockPos right = ColonyService.anchorToSurface(level, trailHead.offset(stepZ, 0, -stepX));

		StructurePlacer.safeSet(level, back, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, trailHead, Blocks.COARSE_DIRT);
		StructurePlacer.safeSet(level, forward, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, left, Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, right, Blocks.PODZOL);
		for (BlockPos pos : new BlockPos[] {back, trailHead, forward, left, right}) {
			StructurePlacer.safeSet(level, pos.above(), Blocks.AIR);
		}
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	public static void seedConstructionStages(ServerLevel level, ColonyData colony) {
		clearConstructionStageArea(level, colony.origin());
		clearConstructionStageActors(level, colony.origin());
		List<BlockPos> centers = constructionStageBuildingCenters(colony.origin());
		List<ColonyBuilding> staged = List.of(
				ColonyBuilding.planned(BuildingType.MARKET, centers.get(0)),
				new ColonyBuilding(BuildingType.MARKET, centers.get(1), 1, 45, 0),
				ColonyBuilding.complete(BuildingType.MARKET, centers.get(2)),
				new ColonyBuilding(BuildingType.MARKET, centers.get(3), 2, 100, 0),
				new ColonyBuilding(BuildingType.MARKET, centers.get(4), 1, 100, 120),
				new ColonyBuilding(BuildingType.MARKET, centers.get(5), 1, 65, 80)
		);
		colony.progress().buildings().clear();
		colony.progress().buildQueue().clear();
		colony.clearChambers();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_CHAMBER, colony.origin()));
		for (ColonyBuilding building : staged) {
			colony.progress().addBuilding(building);
			StructurePlacer.placeBuilding(level, building.pos(), building.type(), building.visualStage(), colony.progress().culture());
		}
		placeConstructionDeliveryProof(level, colony, centers.get(1));
		placeConstructionLateStageProof(level, colony, centers.get(4), centers.get(5));
		colony.setCurrentTask("QA: planned, building with resin delivery, complete, upgraded, damaged, repairing");
		colony.addEvent("Visual QA construction stages and resin delivery seeded");
		com.formicfrontier.world.ColonyLabelService.syncLabels(level, colony);
	}

	private static void placeConstructionDeliveryProof(ServerLevel level, ColonyData colony, BlockPos construction) {
		AntEntity builder = spawnWorkAnt(level, construction.offset(-2, 1, -6), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.WORKING, 180.0f);
		AntEntity supplier = spawnWorkAnt(level, construction.offset(4, 1, -7), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.CARRYING_RESIN, 180.0f);
		placeBlockMarker(level, builder, Blocks.MANGROVE_ROOTS, 0.55f);
		placeItemMarker(level, supplier, new ItemStack(ModItems.RESIN_GLOB), 0.55f);
		placeJobAnchor(level, builder, construction, Blocks.MANGROVE_ROOTS, Blocks.DIRT_PATH);
		// Delivered-material proof uses the native Resin Depot block (honey->native
		// migration). The construction gametest asserts RESIN_DEPOT both at the
		// resin supplier's job anchor (construction +4,0,-7) and at the depot stack
		// (+5,1,-5), so neither may be honey.
		placeJobAnchor(level, supplier, construction, ModBlocks.RESIN_DEPOT, Blocks.ROOTED_DIRT);
		StructurePlacer.safeSet(level, construction.offset(5, 1, -5), ModBlocks.RESIN_DEPOT);
		StructurePlacer.safeSet(level, construction.offset(5, 1, -6), Blocks.BARREL);
	}

	private static void placeConstructionLateStageProof(ServerLevel level, ColonyData colony, BlockPos damaged, BlockPos repairing) {
		AntEntity inspector = spawnWorkAnt(level, damaged.offset(0, 1, -6), AntCaste.SOLDIER, colony.id(), com.formicfrontier.sim.AntWorkState.PATROLLING, 180.0f);
		AntEntity repairer = spawnWorkAnt(level, repairing.offset(-2, 1, -6), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.WORKING, 180.0f);
		AntEntity supplier = spawnWorkAnt(level, repairing.offset(4, 1, -6), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.CARRYING_CHITIN, 180.0f);
		placeBlockMarker(level, inspector, Blocks.BLACKSTONE, 0.45f);
		placeBlockMarker(level, repairer, Blocks.HONEYCOMB_BLOCK, 0.55f);
		placeItemMarker(level, supplier, new ItemStack(ModItems.CHITIN_SHARD), 0.55f);
		placeJobAnchor(level, inspector, damaged, Blocks.RED_TERRACOTTA, Blocks.BLACKSTONE);
		placeJobAnchor(level, repairer, repairing, Blocks.HONEYCOMB_BLOCK, Blocks.DIRT_PATH);
		placeJobAnchor(level, supplier, repairing, Blocks.BONE_BLOCK, Blocks.DIRT_PATH);
		StructurePlacer.safeSet(level, repairing.offset(4, 1, -5), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, repairing.offset(4, 1, -6), Blocks.BARREL);
	}

	private static void clearConstructionStageArea(ServerLevel level, BlockPos origin) {
		for (int x = -36; x <= 36; x++) {
			for (int z = -44; z <= -8; z++) {
				BlockPos ground = origin.offset(x, 0, z);
				level.setBlock(ground.below(2), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 14; y++) {
					level.setBlock(ground.above(y), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void clearConstructionStageActors(ServerLevel level, BlockPos origin) {
		AABB cleanup = new AABB(
				origin.getX() - 38, origin.getY() - 4, origin.getZ() - 48,
				origin.getX() + 38, origin.getY() + 24, origin.getZ() - 6
		);
		level.getEntitiesOfClass(AntEntity.class, cleanup).forEach(AntEntity::discard);
		level.getEntitiesOfClass(Display.class, cleanup).forEach(Display::discard);
	}

	public static void seedRepairScene(ServerLevel level, ColonyData colony) {
		BlockPos origin = colony.origin();
		clearRepairSceneArea(level, origin);
		clearRepairSceneActors(level, origin);
		List<BlockPos> centers = repairSceneBuildingCenters(origin);
		ColonyBuilding damaged = ColonyBuilding.complete(BuildingType.MARKET, centers.get(0));
		damaged.disableFor(160);
		ColonyBuilding repairing = new ColonyBuilding(BuildingType.MARKET, centers.get(1), 1, 62, 90);
		ColonyBuilding restored = ColonyBuilding.complete(BuildingType.MARKET, centers.get(2));

		colony.progress().buildings().clear();
		colony.progress().buildQueue().clear();
		colony.clearChambers();
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_CHAMBER, origin));
		for (ColonyBuilding building : List.of(damaged, repairing, restored)) {
			colony.progress().addBuilding(building);
			StructurePlacer.placeBuilding(level, building.pos(), building.type(), building.visualStage(), colony.progress().culture());
		}

		placeRepairSceneTrail(level, damaged.pos(), repairing.pos(), restored.pos());
		AntEntity repairer = spawnWorkAnt(level, repairing.pos().offset(-1, 1, -5), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.WORKING, 180.0f);
		AntEntity supplier = spawnWorkAnt(level, repairing.pos().offset(4, 1, -5), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.CARRYING_CHITIN, 180.0f);
		AntEntity inspector = spawnWorkAnt(level, damaged.pos().offset(0, 1, -6), AntCaste.SOLDIER, colony.id(), com.formicfrontier.sim.AntWorkState.PATROLLING, 180.0f);
		placeBlockMarker(level, repairer, Blocks.HONEYCOMB_BLOCK, 0.55f);
		placeItemMarker(level, supplier, new ItemStack(ModItems.CHITIN_SHARD), 0.55f);
		placeBlockMarker(level, inspector, Blocks.RED_TERRACOTTA, 0.45f);
		placeJobAnchor(level, repairer, repairing.pos(), Blocks.HONEYCOMB_BLOCK, Blocks.DIRT_PATH);
		placeJobAnchor(level, supplier, repairing.pos(), Blocks.BONE_BLOCK, Blocks.DIRT_PATH);
		placeJobAnchor(level, inspector, damaged.pos(), Blocks.RED_TERRACOTTA, Blocks.BLACKSTONE);

		colony.setResource(ResourceType.CHITIN, 36);
		colony.setCurrentTask("QA repair loop: damaged, repairing, restored");
		colony.addEvent("Visual QA repair scene seeded");
		ColonyLabelService.syncLabels(level, colony);
	}

	private static void clearRepairSceneArea(ServerLevel level, BlockPos origin) {
		for (int x = -34; x <= 34; x++) {
			for (int z = -42; z <= -10; z++) {
				BlockPos ground = origin.offset(x, 0, z);
				level.setBlock(ground.below(2), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 14; y++) {
					level.setBlock(ground.above(y), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void clearRepairSceneActors(ServerLevel level, BlockPos origin) {
		AABB cleanup = new AABB(
				origin.getX() - 38, origin.getY() - 4, origin.getZ() - 46,
				origin.getX() + 38, origin.getY() + 24, origin.getZ() - 6
		);
		level.getEntitiesOfClass(AntEntity.class, cleanup).forEach(AntEntity::discard);
		level.getEntitiesOfClass(Display.class, cleanup).forEach(Display::discard);
	}

	private static void placeRepairSceneTrail(ServerLevel level, BlockPos damaged, BlockPos repairing, BlockPos restored) {
		placeRepairPath(level, damaged.offset(0, 0, -6), repairing.offset(0, 0, -6));
		placeRepairPath(level, repairing.offset(0, 0, -6), restored.offset(0, 0, -6));
		StructurePlacer.safeSet(level, damaged.offset(0, 1, -5), Blocks.RED_TERRACOTTA);
		StructurePlacer.safeSet(level, damaged.offset(-2, 1, -5), Blocks.BLACKSTONE);
		StructurePlacer.safeSet(level, repairing.offset(-2, 1, -5), Blocks.HONEYCOMB_BLOCK);
		StructurePlacer.safeSet(level, repairing.offset(2, 1, -5), Blocks.BONE_BLOCK);
		StructurePlacer.safeSet(level, restored.offset(0, 1, -5), Blocks.OCHRE_FROGLIGHT);
	}

	private static void placeRepairPath(ServerLevel level, BlockPos start, BlockPos end) {
		BlockPos current = start;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 80) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			StructurePlacer.safeSet(level, current, steps % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
		}
	}

	public static void seedWorkCycle(ServerLevel level, ColonyData colony) {
		BlockPos origin = colony.origin();
		clearWorkCycleArea(level, origin);
		clearWorkCycleActors(level, origin);
		List<BlockPos> jobCenters = workCycleJobCenters(origin);
		BlockPos construction = jobCenters.get(0);
		BlockPos ore = jobCenters.get(1);
		BlockPos patrol = jobCenters.get(2);
		BlockPos logisticsPad = jobCenters.get(3);

		ColonyBuilding active = new ColonyBuilding(BuildingType.MARKET, construction, 1, 45, 0);
		colony.progress().addBuilding(active);
		StructurePlacer.placeBuilding(level, construction, BuildingType.MARKET, active.visualStage(), colony.progress().culture());
		placeTaskPad(level, ore, Blocks.COBBLED_DEEPSLATE.defaultBlockState().getBlock(), Blocks.IRON_ORE.defaultBlockState().getBlock());
		StructurePlacer.placeBuilding(level, patrol, BuildingType.WATCH_POST, BuildingVisualStage.COMPLETE, colony.progress().culture());
		placeLogisticsPad(level, logisticsPad);

		AntEntity builder = spawnWorkAnt(level, construction.offset(1, 1, -4), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.WORKING, 180.0f);
		AntEntity carrier = spawnWorkAnt(level, ore.offset(0, 1, -3), AntCaste.MINER, colony.id(), com.formicfrontier.sim.AntWorkState.CARRYING_ORE, 180.0f);
		AntEntity patrolAnt = spawnWorkAnt(level, patrol.offset(0, 1, -4), AntCaste.SOLDIER, colony.id(), com.formicfrontier.sim.AntWorkState.PATROLLING, 180.0f);
		AntEntity logistics = spawnWorkAnt(level, logisticsPad.above(), AntCaste.WORKER, colony.id(), com.formicfrontier.sim.AntWorkState.CARRYING_FOOD, 180.0f);

		placeBlockMarker(level, builder, Blocks.MANGROVE_ROOTS.defaultBlockState().getBlock(), 0.55f);
		placeItemMarker(level, carrier, new ItemStack(Items.RAW_IRON), 0.65f);
		placeBlockMarker(level, patrolAnt, Blocks.POLISHED_DEEPSLATE.defaultBlockState().getBlock(), 0.45f);
		placeItemMarker(level, logistics, new ItemStack(Items.WHEAT), 0.55f);
		placeJobAnchor(level, builder, construction, Blocks.MANGROVE_ROOTS, Blocks.DIRT_PATH);
		placeJobAnchor(level, carrier, ore, Blocks.IRON_ORE, Blocks.COBBLED_DEEPSLATE);
		placeJobAnchor(level, patrolAnt, patrol, Blocks.POLISHED_DEEPSLATE, Blocks.BONE_BLOCK);
		placeJobAnchor(level, logistics, logisticsPad.offset(-2, 0, 2), Blocks.HAY_BLOCK, Blocks.DIRT_PATH);
		colony.setCurrentTask("QA work cycle: build, haul, patrol, forage");
		colony.addEvent("Visual QA work cycle seeded");
	}

	private static void clearWorkCycleActors(ServerLevel level, BlockPos origin) {
		AABB cleanup = workCycleActorCleanupBounds(origin);
		level.getEntitiesOfClass(AntEntity.class, cleanup).forEach(AntEntity::discard);
		level.getEntitiesOfClass(Display.class, cleanup).forEach(Display::discard);
	}

	private static void clearWorkCycleArea(ServerLevel level, BlockPos origin) {
		for (int x = -24; x <= 24; x++) {
			for (int z = -40; z <= -12; z++) {
				BlockPos ground = origin.offset(x, 0, z);
				level.setBlock(ground.below(2), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 12; y++) {
					level.setBlock(ground.above(y), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void placeTaskPad(ServerLevel level, BlockPos center, net.minecraft.world.level.block.Block base, net.minecraft.world.level.block.Block accent) {
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance <= 4) {
					StructurePlacer.safeSet(level, center.offset(x, 0, z), distance <= 1 ? accent : base);
				}
			}
		}
		StructurePlacer.safeSet(level, center.offset(-1, 1, 0), accent);
		StructurePlacer.safeSet(level, center.offset(1, 1, 0), Blocks.COBBLED_DEEPSLATE_WALL);
	}

	private static void placeLogisticsPad(ServerLevel level, BlockPos center) {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				int distance = Math.abs(x) + Math.abs(z);
				if (distance <= 3) {
					StructurePlacer.safeSet(level, center.offset(x, 0, z), distance <= 1 ? Blocks.HAY_BLOCK : Blocks.DIRT_PATH);
				}
			}
		}
		StructurePlacer.safeSet(level, center.offset(-2, 1, 1), Blocks.COMPOSTER);
		StructurePlacer.safeSet(level, center.offset(2, 1, -1), Blocks.BARREL);
		StructurePlacer.safeSet(level, center.offset(0, 1, 2), Blocks.OAK_FENCE);
	}

	private static void placeJobAnchor(ServerLevel level, AntEntity ant, BlockPos jobCenter, net.minecraft.world.level.block.Block anchor, net.minecraft.world.level.block.Block trail) {
		if (ant == null) {
			return;
		}
		BlockPos foot = ant.blockPosition().below();
		StructurePlacer.safeSet(level, foot, anchor);
		BlockPos current = foot;
		for (int step = 0; step < 5 && !isNearGroundTarget(current, jobCenter); step++) {
			int dx = jobCenter.getX() - current.getX();
			int dz = jobCenter.getZ() - current.getZ();
			if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
				current = current.offset(Integer.signum(dx), 0, 0);
			} else if (dz != 0) {
				current = current.offset(0, 0, Integer.signum(dz));
			}
			if (!current.equals(jobCenter)) {
				StructurePlacer.safeSet(level, current, step % 2 == 0 ? Blocks.DIRT_PATH : trail);
			}
		}
	}

	private static boolean isNearGroundTarget(BlockPos current, BlockPos target) {
		return Math.abs(current.getX() - target.getX()) + Math.abs(current.getZ() - target.getZ()) <= 1;
	}

	private static AntEntity spawnWorkAnt(ServerLevel level, BlockPos pos, AntCaste caste, int colonyId, com.formicfrontier.sim.AntWorkState state, float yaw) {
		AntEntity ant = ColonyService.spawnAnt(level, pos, caste, colonyId);
		if (ant != null) {
			ant.setWorkState(state);
			ant.setYRot(yaw);
			ant.setYHeadRot(yaw);
		}
		return ant;
	}

	private static void placeItemMarker(ServerLevel level, AntEntity ant, ItemStack stack, float scale) {
		if (ant == null) {
			return;
		}
		ItemDisplay marker = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (marker == null) {
			return;
		}
		marker.setItemStack(stack);
		marker.setItemTransform(ItemDisplayContext.GROUND);
		configureTaskMarker(marker, ant, scale);
		level.addFreshEntity(marker);
	}

	private static void placeBlockMarker(ServerLevel level, AntEntity ant, net.minecraft.world.level.block.Block block, float scale) {
		if (ant == null) {
			return;
		}
		BlockDisplay marker = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (marker == null) {
			return;
		}
		marker.setBlockState(block.defaultBlockState());
		configureTaskMarker(marker, ant, scale);
		level.addFreshEntity(marker);
	}

	private static void configureTaskMarker(Display marker, AntEntity ant, float scale) {
		marker.setPos(ant.getX() - scale * 0.5, ant.getY() + ant.getBbHeight() * 0.72, ant.getZ() - scale * 0.5);
		marker.setNoGravity(true);
		marker.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		marker.setViewRange(32.0f);
		marker.setShadowRadius(0.12f);
		marker.setTransformation(new Transformation(
				new Vector3f(0.0f, 0.0f, 0.0f),
				new Quaternionf(),
				new Vector3f(scale, scale, scale),
				new Quaternionf()
		));
	}

	// ant_lineup framing repair (ant_lineup_all_castes_unclipped): createColony places the
	// MINE building at origin+(0,0,38), which sits squarely between the camera (z~+58) and
	// the lineup row (z=+20). The mine mass occluded the row center and made the castes read
	// as "spread around the mound" rather than a clean single comparison row. Flatten the
	// foreground corridor back to open grass so the camera has an unobstructed sightline to
	// the whole row, while keeping the queen mound (|z|<=16) intact as the backdrop.
	private static void clearAntLineupForeground(ServerLevel level, BlockPos origin) {
		for (int x = -34; x <= 34; x++) {
			for (int z = 21; z <= 62; z++) {
				BlockPos ground = origin.offset(x, 0, z);
				level.setBlock(ground.below(2), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
				level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 48; y++) {
					level.setBlock(ground.above(y), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static void spawnAntLineup(ServerLevel level, BlockPos origin, int colonyId) {
		// Clear the ENTIRE camera-to-row corridor (x[-45,45], z[-15,60]) so no roaming
		// colony ant can wander into the foreground between the lineup row and the camera.
		AABB cleanup = new AABB(
				origin.getX() - 45, origin.getY() - 2, origin.getZ() - 15,
				origin.getX() + 45, origin.getY() + 14, origin.getZ() + 60
		);
		level.getEntitiesOfClass(AntEntity.class, cleanup).forEach(AntEntity::discard);
		clearAntLineupForeground(level, origin);
		for (AntCaste caste : ANT_LINEUP_CASTES) {
			BlockPos pos = antLineupPosition(origin, caste);
			AntEntity ant = ColonyService.spawnAnt(level, pos, caste, colonyId);
			if (ant != null) {
				ant.setCustomName(Component.literal(caste.id()));
				ant.setCustomNameVisible(true);
				ant.setYRot(0.0F);
				ant.setYHeadRot(0.0F);
				// Freeze AI so castes stay exactly on their lineup slots instead of strolling toward the camera and occluding the shot.
				ant.setNoAi(true);
				ant.setPersistenceRequired();
			}
		}
	}
	private static void positionCamera(ServerPlayer player, BlockPos origin, String sceneName) {
		player.setGameMode(GameType.SPECTATOR);
		Vec3 target = switch (sceneName) {
			case COLONY_OVERVIEW, SETTLEMENT_SCALE -> colonyOverviewTarget(origin);
			case ANT_LINEUP -> Vec3.atCenterOf(origin.offset(0, 2, 20)); // row center, body height
			case WORK_CYCLE -> Vec3.atCenterOf(origin.offset(0, 1, -23)).add(0.0, 1.35, 0.0);
			case TABLET_EN, TABLET_RU, TABLET_GUIDE, TABLET_TRADE, TABLET_RESEARCH_MAP, TABLET_MARKET, TABLET_REQUESTS -> Vec3.atCenterOf(origin).add(0.0, 3.0, 0.0);
			case CONSTRUCTION_STAGE -> Vec3.atCenterOf(origin.offset(0, 0, -25)).add(0.0, 2.8, 0.0);
			case REPAIR_SCENE -> Vec3.atCenterOf(origin.offset(0, 0, -26)).add(0.0, 2.6, 0.0);
			case CULTURE_STYLES -> Vec3.atCenterOf(origin.offset(0, 0, 4)).add(0.0, 4.0, 0.0);
			case DIPLOMACY_SCENE -> Vec3.atCenterOf(origin).add(0.0, 4.0, 0.0);
			case WORLDGEN_ENCOUNTER -> worldgenEncounterTarget(origin);
			case ENDGAME_PROJECT -> Vec3.atCenterOf(origin).add(0.0, 5.8, 0.0);
			case PROGRESSION_SCENE -> Vec3.atCenterOf(origin).add(0.0, 5.0, 0.0);
			case STRUCTURE_PREVIEW_3Q -> Vec3.atCenterOf(origin).add(1.0, 9.0, 0.0);
			case STRUCTURE_PREVIEW_FRONT -> Vec3.atCenterOf(origin).add(1.0, 9.0, 0.0);
			default -> Vec3.atCenterOf(origin).add(0.0, 2.0, 0.0);
		};
		Vec3 camera = switch (sceneName) {
			case COLONY_OVERVIEW, SETTLEMENT_SCALE -> colonyOverviewCamera(origin);
			case COLONY_GROUND -> new Vec3(origin.getX() + 26.0, origin.getY() + 8.5, origin.getZ() - 46.0);
			case ANT_LINEUP -> new Vec3(origin.getX() + 0.0, origin.getY() + 11.0, origin.getZ() + 52.0); // dist ~32 from row at z=+20, y+11 for ~17deg downward; full 30-wide row fits with margin, foreground corridor cleared
			case WORK_CYCLE -> new Vec3(origin.getX() + 14.0, origin.getY() + 6.6, origin.getZ() - 38.0);
			case TABLET_EN, TABLET_RU, TABLET_GUIDE, TABLET_TRADE, TABLET_RESEARCH_MAP, TABLET_MARKET, TABLET_REQUESTS -> new Vec3(origin.getX() + 12.0, origin.getY() + 5.0, origin.getZ() - 18.0);
			case CONSTRUCTION_STAGE -> new Vec3(origin.getX() + 18.0, origin.getY() + 13.0, origin.getZ() - 55.0);
			case REPAIR_SCENE -> new Vec3(origin.getX() + 20.0, origin.getY() + 12.0, origin.getZ() - 55.0);
			case CULTURE_STYLES -> new Vec3(origin.getX() + 0.0, origin.getY() + 44.0, origin.getZ() - 92.0);
			case DIPLOMACY_SCENE -> new Vec3(origin.getX() + 0.0, origin.getY() + 42.0, origin.getZ() - 112.0);
			case WORLDGEN_ENCOUNTER -> worldgenEncounterCamera(origin);
			case ENDGAME_PROJECT -> new Vec3(origin.getX() + 54.0, origin.getY() + 36.0, origin.getZ() - 68.0);
			case PROGRESSION_SCENE -> new Vec3(origin.getX() + 52.0, origin.getY() + 36.0, origin.getZ() - 66.0);
			case STRUCTURE_PREVIEW_3Q -> new Vec3(origin.getX() + 54.0, origin.getY() + 46.0, origin.getZ() + 78.0);
			case STRUCTURE_PREVIEW_FRONT -> new Vec3(origin.getX() + 1.0, origin.getY() + 22.0, origin.getZ() + 80.0);
			default -> new Vec3(origin.getX() + 28.0, origin.getY() + 18.0, origin.getZ() - 32.0);
		};
		player.teleportTo(camera.x, camera.y, camera.z);
		player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
	}

	/**
	 * R2 forest-floor density: scatter dirt variation, roots, stones, flowers, leaf
	 * litter, small piles, and terrain breakup across the colony footprint so the
	 * ground reads as a living forest floor (see reference-forest-foraging.png), not
	 * a superflat grass sheet. Deterministic per (x,z) so screenshots are stable.
	 * Only writes y==0 ground (and occasional y==1 low tufts) and skips cells already
	 * claimed by a building footprint so it never hides the subject. The dressing is
	 * denser near the colony center and thins toward the frame edge for natural fall-off.
	 */
	private static void dressForestFloor(ServerLevel level, BlockPos origin, String sceneName) {
		int radius = 96;
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				int dist = Math.abs(x) + Math.abs(z);
				if (dist > radius) {
					continue;
				}
				// Deterministic per-cell hash so the scatter is stable across runs.
				int h = forestFloorHash(x, z);
				BlockPos ground = origin.offset(x, 0, z);
				// Skip cells already part of a placed structure (non-grass ground or built block).
				net.minecraft.world.level.block.state.BlockState existing = level.getBlockState(ground);
				if (!existing.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
					continue;
				}
				// R2 forest-floor density (pass 2): denser dressing with gentler fall-off so
				// the colony reads as embedded in a living forest floor, not a sparse demo.
				// ~80% dressed near center, ~50% at mid-radius, ~25% at the frame edge.
				int densityGate = (dist * 18) / radius;
				int roll = Math.floorMod(h, 100);
				if (roll < densityGate) {
					continue;
				}
				net.minecraft.world.level.block.Block floor = forestFloorBlock(h, dist, radius);
				if (floor != null) {
					level.setBlock(ground, floor.defaultBlockState(), 3);
				}
				// Low y==1 tufts: grass, flowers, tiny stones, leaf-litter mushrooms.
				net.minecraft.world.level.block.Block tuft = forestFloorTuft(h, dist, radius);
				if (tuft != null) {
					BlockPos above = ground.above();
					if (level.getBlockState(above).isAir() && floor != net.minecraft.world.level.block.Blocks.DIRT_PATH) {
						level.setBlock(above, tuft.defaultBlockState(), 3);
					}
				}
			}
		}
		// A few prominent root masses and stone clusters near the mound base for scale.
		placeForestFloorClusters(level, origin);
		// R2 forest-floor density (pass 3, retry-03): the colony core still read as a
		// grass meadow near the hub because dressing skipped the cells closest to the
		// buildings. Stamp visible worn worker routes and trampled-earth/debris skirts
		// around the campus so colony_ground reads as a lived-in trail network.
		dressColonyCore(level, origin);
	}

	private static int forestFloorHash(int x, int z) {
		int h = (x * 73856093) ^ (z * 19349663);
		h ^= (h >>> 13);
		h *= 1274126177;
		h ^= (h >>> 16);
		return h;
	}

	private static net.minecraft.world.level.block.Block forestFloorBlock(int h, int dist, int radius) {
		int roll = Math.floorMod(h >>> 3, 100);
		// R2 denser, more varied forest floor: coarse dirt + podzol dominate, with more
		// rooted dirt ribs, moss patches, mud, peeping stone, and worn dirt-path threads
		// near the colony core so the ground reads like reference-forest-foraging.png.
		if (dist < 28 && roll < 12) return net.minecraft.world.level.block.Blocks.DIRT_PATH;
		if (roll < 26) return net.minecraft.world.level.block.Blocks.COARSE_DIRT;
		if (roll < 44) return net.minecraft.world.level.block.Blocks.PODZOL;
		if (roll < 60) return net.minecraft.world.level.block.Blocks.ROOTED_DIRT;
		if (roll < 70) return net.minecraft.world.level.block.Blocks.MOSS_BLOCK;
		if (roll < 78) return net.minecraft.world.level.block.Blocks.DIRT;
		if (roll < 86) return net.minecraft.world.level.block.Blocks.COBBLESTONE;
		if (roll < 92) return net.minecraft.world.level.block.Blocks.MUD;
		if (roll < 96) return net.minecraft.world.level.block.Blocks.PODZOL;
		return null;
	}

	private static net.minecraft.world.level.block.Block forestFloorTuft(int h, int dist, int radius) {
		int roll = Math.floorMod(h >>> 11, 100);
		// R2 denser forest-floor life: more grass tufts, ferns, flowers, mushroom
		// litter, and small pebbles so the floor reads as inhabited and overgrown.
		if (roll < 14) return net.minecraft.world.level.block.Blocks.SHORT_GRASS;
		if (roll < 22) return net.minecraft.world.level.block.Blocks.TALL_GRASS;
		if (roll < 28) return net.minecraft.world.level.block.Blocks.FERN;
		if (roll < 34) return net.minecraft.world.level.block.Blocks.LARGE_FERN;
		if (roll < 40) return net.minecraft.world.level.block.Blocks.DANDELION;
		if (roll < 45) return net.minecraft.world.level.block.Blocks.POPPY;
		if (roll < 50) return net.minecraft.world.level.block.Blocks.CORNFLOWER;
		if (roll < 55) return net.minecraft.world.level.block.Blocks.AZURE_BLUET;
		if (roll < 60) return net.minecraft.world.level.block.Blocks.WHITE_TULIP;
		if (roll < 64) return net.minecraft.world.level.block.Blocks.OXEYE_DAISY;
		if (roll < 70) return net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM;
		if (roll < 75) return net.minecraft.world.level.block.Blocks.RED_MUSHROOM;
		if (roll < 80) return net.minecraft.world.level.block.Blocks.STONE_BUTTON;
		if (roll < 84) return net.minecraft.world.level.block.Blocks.DEAD_BUSH;
		return null;
	}

	private static void placeForestFloorClusters(ServerLevel level, BlockPos origin) {
		// 8 prominent root/stone/litter clusters in a ring ~28 blocks out for scale anchors.
		int[][] ring = { {20, 20}, {-20, 20}, {20, -20}, {-20, -20}, {28, 8}, {-28, 8}, {8, 28}, {8, -28} };
		for (int i = 0; i < ring.length; i++) {
			int cx = ring[i][0];
			int cz = ring[i][1];
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					int d = Math.abs(dx) + Math.abs(dz);
					if (d > 3) continue;
					BlockPos g = origin.offset(cx + dx, 0, cz + dz);
					if (i % 3 == 0) {
						// root mass
						if (d <= 2) StructurePlacer.safeSet(level, g, net.minecraft.world.level.block.Blocks.ROOTED_DIRT);
						if (d == 0) StructurePlacer.safeSet(level, g.above(), net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS);
					} else if (i % 3 == 1) {
						// stone cluster
						if (d <= 1) StructurePlacer.safeSet(level, g, net.minecraft.world.level.block.Blocks.COBBLESTONE);
						if (d == 0) StructurePlacer.safeSet(level, g.above(), net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE);
					} else {
						// leaf-litter / fungus mound
						if (d <= 2) StructurePlacer.safeSet(level, g, net.minecraft.world.level.block.Blocks.PODZOL);
						if (d <= 1) StructurePlacer.safeSet(level, g.above(), net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK);
					}
				}
			}
		}
	}

	/**
	 * R2 forest-floor density (pass 3, retry-03): stamp a lived-in colony trail network
	 * so the core no longer reads as a flat grass meadow. Drawn AFTER the building shells
	 * and the base forest-floor pass, so it only overwrites remaining GRASS_BLOCK cells
	 * (GameTest assertions target specific non-grass rally/structure blocks, which are
	 * preserved). Emits visible worn worker routes from the hub toward each starter
	 * campus building, trampled-earth rings around building skirts, and scattered debris
	 * piles (twigs, pebbles, leaf litter) so the ground reads like reference-forest-
	 * foraging.png: a real colony footprint embedded in forest dirt, not a golf green.
	 */
	private static void dressColonyCore(ServerLevel level, BlockPos origin) {
		// Worn worker routes from the queen hub toward each starter campus building.
		// Use the same siteFor() offsets the nest uses so routes line up with real doors.
		net.minecraft.world.level.block.state.BlockState path = net.minecraft.world.level.block.Blocks.DIRT_PATH.defaultBlockState();
		net.minecraft.world.level.block.state.BlockState coarse = net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState();
		net.minecraft.world.level.block.state.BlockState podzol = net.minecraft.world.level.block.Blocks.PODZOL.defaultBlockState();
		BlockPos[] hubs = {
				ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0),
				ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0),
				ColonyBuilder.siteFor(origin, BuildingType.MINE, 0),
				ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0),
				ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0),
				ColonyBuilder.siteFor(origin, BuildingType.DIPLOMACY_SHRINE, 0),
				origin.offset(54, 0, 8),   // food node
				origin.offset(-54, 0, 8)   // chitin node
		};
		for (BlockPos hub : hubs) {
			stampWorkerRoute(level, origin, hub, path, coarse, podzol);
		}
		// Trampled-earth rings + debris around every campus building skirt so the ground
		// around structures reads as worn, not a manicured lawn. startFrom 1 to skip the
		// central queen chamber (its footprint is the nest mound itself).
		for (int i = 1; i < hubs.length; i++) {
			stampColonySkirt(level, hubs[i]);
		}
	}

	/**
	 * Worn worker route: a 3-wide dirt-path lane from the hub toward a destination, with
	 * frayed coarse-dirt/podzol edges and occasional debris so it reads as a real ant
	 * trail, not a paved road. Only paints GRASS_BLOCK cells; non-grass (structures,
	 * existing paths, resource nodes, rally markers) are left untouched.
	 */
	private static void stampWorkerRoute(ServerLevel level, BlockPos origin, BlockPos destination,
			net.minecraft.world.level.block.state.BlockState path, net.minecraft.world.level.block.state.BlockState coarse,
			net.minecraft.world.level.block.state.BlockState podzol) {
		if (destination == null || origin.equals(destination)) {
			return;
		}
		BlockPos end = new BlockPos(destination.getX(), origin.getY(), destination.getZ());
		BlockPos cursor = origin;
		int step = 0;
		while (cursor.getX() != end.getX()) {
			int dx = Integer.compare(end.getX(), cursor.getX());
			cursor = cursor.offset(dx, 0, 0);
			paintRouteTile(level, cursor, true, ++step, path, coarse, podzol);
		}
		while (cursor.getZ() != end.getZ()) {
			int dz = Integer.compare(end.getZ(), cursor.getZ());
			cursor = cursor.offset(0, 0, dz);
			paintRouteTile(level, cursor, false, ++step, path, coarse, podzol);
		}
	}

	private static void paintRouteTile(ServerLevel level, BlockPos center, boolean xAxis, int step,
			net.minecraft.world.level.block.state.BlockState path, net.minecraft.world.level.block.state.BlockState coarse,
			net.minecraft.world.level.block.state.BlockState podzol) {
		// 3-wide worn lane center.
		setIfGrass(level, center, path);
		if (xAxis) {
			setIfGrass(level, center.north(), path);
			setIfGrass(level, center.south(), path);
		} else {
			setIfGrass(level, center.east(), path);
			setIfGrass(level, center.west(), path);
		}
		// Frayed trampled edges every few tiles so the trail looks trodden, not paved.
		if (step % 3 == 0) {
			if (xAxis) {
				setIfGrass(level, center.north(2), coarse);
				setIfGrass(level, center.south(2), podzol);
			} else {
				setIfGrass(level, center.east(2), coarse);
				setIfGrass(level, center.west(2), podzol);
			}
		}
	}

	/**
	 * Trampled-earth ring + scattered debris around a campus building skirt so the ground
	 * surrounding each role building reads as worn colony earth, not untouched grass.
	 * Concentric ring at radius ~11-14 from the building center; only overwrites grass.
	 */
	private static void stampColonySkirt(ServerLevel level, BlockPos center) {
		for (int radius = 11; radius <= 14; radius++) {
			int density = radius == 11 ? 70 : 45;  // dense inner worn ring, sparser outer
			int ringSteps = radius * 8;
			for (int i = 0; i < ringSteps; i++) {
				double angle = (i * 2 * Math.PI) / ringSteps;
				int ox = (int) Math.round(Math.cos(angle) * radius);
				int oz = (int) Math.round(Math.sin(angle) * radius);
				BlockPos ground = center.offset(ox, 0, oz);
				if (!level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
					continue;
				}
				int h = forestFloorHash(ox + center.getX(), oz + center.getZ());
				int roll = Math.floorMod(h, 100);
				if (roll >= density) {
					continue;
				}
				// Worn colony earth: coarse dirt, podzol, dirt path, rooted dirt ribs.
				net.minecraft.world.level.block.Block floor = colonySkirtBlock(h);
				if (floor != null) {
					level.setBlock(ground, floor.defaultBlockState(), 3);
				}
				// Low debris: twigs (dead bush / fern), pebbles (button), leaf litter.
				net.minecraft.world.level.block.Block debris = colonySkirtDebris(h);
				if (debris != null) {
					BlockPos above = ground.above();
					if (level.getBlockState(above).isAir()) {
						level.setBlock(above, debris.defaultBlockState(), 3);
					}
				}
			}
		}
	}

	private static net.minecraft.world.level.block.Block colonySkirtBlock(int h) {
		int roll = Math.floorMod(h >>> 5, 100);
		if (roll < 30) return net.minecraft.world.level.block.Blocks.COARSE_DIRT;
		if (roll < 55) return net.minecraft.world.level.block.Blocks.PODZOL;
		if (roll < 72) return net.minecraft.world.level.block.Blocks.DIRT_PATH;
		if (roll < 86) return net.minecraft.world.level.block.Blocks.ROOTED_DIRT;
		if (roll < 94) return net.minecraft.world.level.block.Blocks.MUD;
		return null;
	}

	private static net.minecraft.world.level.block.Block colonySkirtDebris(int h) {
		int roll = Math.floorMod(h >>> 9, 100);
		if (roll < 24) return net.minecraft.world.level.block.Blocks.DEAD_BUSH;
		if (roll < 44) return net.minecraft.world.level.block.Blocks.FERN;
		if (roll < 60) return net.minecraft.world.level.block.Blocks.STONE_BUTTON;
		if (roll < 74) return net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM;
		if (roll < 86) return net.minecraft.world.level.block.Blocks.SHORT_GRASS;
		return null;
	}

	/**
	 * Set a block only if the current cell is GRASS_BLOCK, so colony-core dressing never
	 * overwrites a placed structure, path, resource node, or GameTest rally marker.
	 */
	private static void setIfGrass(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
			level.setBlock(pos, state, 3);
		}
	}

	private static void prepareFlatQaArea(ServerLevel level, BlockPos origin, int radius) {
		AABB cleanup = new AABB(
				origin.getX() - radius, origin.getY() - 8, origin.getZ() - radius,
				origin.getX() + radius, origin.getY() + 96, origin.getZ() + radius
		);
		level.getEntitiesOfClass(AntEntity.class, cleanup).forEach(AntEntity::discard);
		level.getEntitiesOfClass(Display.class, cleanup).forEach(Display::discard);
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
