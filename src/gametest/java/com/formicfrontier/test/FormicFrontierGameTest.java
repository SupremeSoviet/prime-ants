package com.formicfrontier.test;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.AntWorkState;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyContract;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ColonyRank;
import com.formicfrontier.sim.ColonyTradeCatalog;
import com.formicfrontier.sim.DiplomacyState;
import com.formicfrontier.sim.DiplomacyAction;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.qa.VisualQaScenes;
import com.formicfrontier.world.ColonyBuilder;
import com.formicfrontier.world.ColonyDiscoveryService;
import com.formicfrontier.world.ColonyRecurringEvents;
import com.formicfrontier.world.ColonySavedState;
import com.formicfrontier.world.ColonyService;
import com.formicfrontier.world.DiplomacyConsequences;
import com.formicfrontier.world.RaidPlanner;
import com.formicfrontier.world.StructurePlacer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class FormicFrontierGameTest {
	@GameTest
	public void createColonyPlacesCoreChambersAndResources(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		helper.assertBlockPresent(ModBlocks.NEST_MOUND, origin);
		helper.assertBlockPresent(ModBlocks.NEST_CORE, origin.below());
		helper.assertBlockPresent(ModBlocks.COLONY_LEDGER, origin.offset(3, 1, 0));
		helper.assertBlockPresent(ModBlocks.FOOD_CHAMBER, ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0));
		helper.assertBlockPresent(ModBlocks.NURSERY_CHAMBER, ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0));
		helper.assertBlockPresent(ModBlocks.MINE_CHAMBER, ColonyBuilder.siteFor(origin, BuildingType.MINE, 0));
		helper.assertBlockPresent(ModBlocks.BARRACKS_CHAMBER, ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0));
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, origin.offset(54, 0, 8));
		helper.assertBlockPresent(ModBlocks.ORE_NODE, origin.offset(8, 0, 54));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, origin.offset(-54, 0, 8));
		helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT_PATH, origin.offset(7, 0, 0));
		helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT_PATH, origin.offset(7, 0, 1));
		if (colony.casteCount(AntCaste.GIANT) != 0) {
			helper.fail("Starter colony should not begin with a giant.");
		}
		helper.succeed();
	}

	@GameTest
	public void starterColonyHasEconomyButNoFreeGiant(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		if (colony.resource(ResourceType.FOOD) <= 0 || colony.resource(ResourceType.ORE) <= 0 || colony.resource(ResourceType.CHITIN) <= 0) {
			helper.fail("Starter colony should seed food, ore, and chitin.");
		}
		if (AntCaste.GIANT.canGrowFrom(colony)) {
			helper.fail("Starter colony should not afford a giant immediately.");
		}
		helper.succeed();
	}

	@GameTest
	public void colonyCreationAnchorsHighRequestsToGround(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.above(8)));

		if (!colony.origin().equals(helper.absolutePos(origin))) {
			helper.fail("Colony origin should snap from an air request down to the surface.");
		}
		helper.assertBlockPresent(ModBlocks.NEST_MOUND, origin);
		if (helper.getLevel().getBlockState(helper.absolutePos(origin).below()).isAir()) {
			helper.fail("Starter colony floor should have solid support below it.");
		}
		helper.succeed();
	}

	@GameTest
	public void starterQueenChamberUsesOrganicMoundShape(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		helper.assertBlockPresent(ModBlocks.NEST_MOUND, origin);
		helper.assertBlockPresent(ModBlocks.NEST_CORE, origin.below());
		helper.assertBlockPresent(ModBlocks.COLONY_LEDGER, origin.offset(3, 1, 0));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, -8));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-6, 1, -1));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(-2, 1, -5));
		helper.assertBlockPresent(Blocks.PACKED_MUD, origin.offset(8, 0, 3));
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(2, 1, -8))).isAir()) {
			helper.fail("Queen mound front approach should keep side entrance air clear.");
		}
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(8, 1, 2));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-9, 1, 5));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(9, 1, -3));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(3, 1, -3));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(0, 0, -6));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(0, 3, -7));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(-3, 1, -9));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(5, 0, 2));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(5, 3, 0));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(-3, 0, -8));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(3, 0, -8));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(-3, 1, -6));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(3, 1, -6));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-4, 1, -7));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(6, 0, 2));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(6, 1, 3));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(0, 0, -5));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(0, 3, -5));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(6, 3, 1));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(-4, 0, -11));
		helper.assertBlockPresent(Blocks.PODZOL, origin.offset(-3, 0, -12));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, -11));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(-2, 0, -12));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(2, 0, -12));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(10, 0, 0));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(10, 0, 2));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(9, 1, -1));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(-2, 0, -10));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(2, 0, -10));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(9, 0, -2));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(9, 0, 3));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(-2, 0, 8));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(2, 0, 8));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-2, 1, -10));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(2, 1, 8));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-4, 0, -9));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(-5, 0, -9));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(6, 0, -8));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-5, 1, -10));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(6, 2, -8));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(-7, 0, -8));
		helper.assertBlockPresent(Blocks.PODZOL, origin.offset(8, 0, -7));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(7, 1, -7));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-9, 0, -5));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(-10, 0, -4));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-9, 2, -6));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(9, 0, -5));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(10, 0, -4));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(9, 2, -6));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-5, 1, 5));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(-5, 1, 7));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(-5, 0, 6));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(5, 1, 6));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(3, 0, 7));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(-5, 0, -14));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(5, 0, -14));
		helper.assertBlockPresent(Blocks.PODZOL, origin.offset(-7, 0, -16));
		helper.assertBlockPresent(Blocks.PODZOL, origin.offset(7, 0, -16));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-3, 1, -14));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(3, 1, -14));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(-5, 1, -13));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(5, 1, -13));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-4, 0, -15));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(4, 0, -15));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(-5, 1, -16));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(5, 1, -16));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(-6, 1, -17));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(6, 1, -17));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, -14));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, -16));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-2, 0, -15));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(2, 0, -15));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-1, 1, -15));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(1, 1, -15));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(0, 0, 10));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(-1, 0, 10));
		helper.assertBlockPresent(Blocks.PODZOL, origin.offset(1, 0, 10));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(0, 3, 8));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-2, 1, 9));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(2, 1, 9));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-2, 1, -6));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(2, 1, -6));
		helper.assertBlockPresent(Blocks.PACKED_MUD, origin.offset(0, 0, -4));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-5, 0, -5));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(5, 0, -5));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(-6, 0, -4));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(6, 0, -4));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(-7, 0, -3));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(7, 0, -3));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-4, 1, -5));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(4, 1, -5));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(-6, 2, -5));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(6, 2, -5));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-10, 0, 0));
		helper.assertBlockPresent(Blocks.MUD, origin.offset(-10, 0, 1));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-13, 0, 0));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-8, 1, -1));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(-12, 1, 2));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(-13, 1, -1));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(8, 1, -1));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(8, 2, 2));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-1, 2, 8));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(1, 2, 8));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(0, 3, 9));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(0, 5, 0));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(0, 6, 0));
		helper.assertBlockPresent(Blocks.MUDDY_MANGROVE_ROOTS, origin.offset(-2, 5, -1));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(2, 5, 1));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, origin.offset(0, 5, 2));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-3, 4, 1));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, origin.offset(3, 4, -1));

		BlockPos mound = helper.absolutePos(origin);
		if (!helper.getLevel().getBlockState(mound.offset(-2, 0, -16)).is(Blocks.MUD)
				|| !helper.getLevel().getBlockState(mound.offset(2, 0, -16)).is(Blocks.PODZOL)
				|| !helper.getLevel().getBlockState(mound.offset(-2, 1, -16)).is(Blocks.HONEYCOMB_BLOCK)
				|| !helper.getLevel().getBlockState(mound.offset(2, 1, -16)).is(Blocks.AMETHYST_BLOCK)
				|| !helper.getLevel().getBlockState(mound.offset(-3, 1, -16)).is(Blocks.MANGROVE_ROOTS)
				|| !helper.getLevel().getBlockState(mound.offset(3, 1, -16)).is(Blocks.MUDDY_MANGROVE_ROOTS)) {
			helper.fail("Queen mound scent trail should show culture markers along the main approach.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 1, -6))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 2, -6))).isAir()) {
			helper.fail("Queen mound should have a readable front tunnel entrance.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 1, 8))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 2, 8))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 1, 10))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 2, 10))).isAir()) {
			helper.fail("Queen mound rear vent should stay open and walkable toward the mine path.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-5, 1, 6))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-5, 2, 6))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(4, 1, 6))).isAir()) {
			helper.fail("Queen mound should have open surface vent chimneys.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-5, 1, -9))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(5, 1, -9))).isAir()) {
			helper.fail("Queen mound forage crawl mouths should stay open beside the front tunnel.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-9, 1, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(9, 1, -5))).isAir()) {
			helper.fail("Queen mound scout porches should keep clear crawl headroom.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(4, 1, 0))).isAir()) {
			helper.fail("Queen mound should have a side tunnel cut into the shell.");
		}
		if (helper.getLevel().getBlockState(helper.absolutePos(origin.offset(6, 1, 6))).is(Blocks.CHISELED_TUFF)
				|| helper.getLevel().getBlockState(helper.absolutePos(origin.offset(6, 1, 6))).is(Blocks.MUD_BRICKS)) {
			helper.fail("Queen mound should not rebuild the old square hall corner supports.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(3, 2, -3))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-7, 2, 3))).isAir()) {
			helper.fail("Queen mound should include asymmetric vents and shoulder breaks.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(10, 1, 0))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(10, 2, 0))).isAir()) {
			helper.fail("Queen mound side approach should keep clear headroom outside the side tunnel.");
		}
		if (helper.getLevel().getBlockState(helper.absolutePos(origin.offset(5, 1, 5))).is(Blocks.CHISELED_TUFF)
				|| helper.getLevel().getBlockState(helper.absolutePos(origin.offset(5, 1, 5))).is(Blocks.MUD_BRICKS)) {
			helper.fail("Queen mound should not use the old rectangular support columns.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(4, 1, -13))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-4, 1, -13))).isAir()) {
			helper.fail("Queen mound erosion runnels should stay low and walkable beside the front entrance.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(5, 1, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-5, 1, -5))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(6, 2, -4))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-6, 2, -4))).isAir()) {
			helper.fail("Queen mound diagonal shoulder clefts should stay open from ground view.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-8, 1, 0))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-8, 2, 0))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(-12, 1, 0))).isAir()) {
			helper.fail("Queen mound west brood crawl should keep clear headroom toward the nursery path.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 1, -15))).isAir()
				|| !helper.getLevel().getBlockState(helper.absolutePos(origin.offset(0, 2, -15))).isAir()) {
			helper.fail("Queen mound front trail fork should stay open and readable from the main entrance.");
		}
		if (!helper.getLevel().getBlockState(mound.offset(0, 1, -16)).isAir()
				|| !helper.getLevel().getBlockState(mound.offset(0, 2, -16)).isAir()) {
			helper.fail("Queen mound scent trail should mark the main entrance path without blocking player view.");
		}
		helper.succeed();
	}

	@GameTest
	public void cultureStarterArchitectureUsesDistinctMaterials(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 150);
		BlockPos amber = origin.offset(-84, 0, -10);
		BlockPos leafcutter = origin.offset(-28, 0, -10);
		BlockPos fire = origin.offset(28, 0, -10);
		BlockPos carpenter = origin.offset(84, 0, -10);
		BlockPos amberFood = amber.offset(0, 0, 28);
		BlockPos leafcutterFood = leafcutter.offset(0, 0, 28);
		BlockPos fireFood = fire.offset(0, 0, 28);
		BlockPos carpenterFood = carpenter.offset(0, 0, 28);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(amber), BuildingType.QUEEN_CHAMBER, BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(leafcutter), BuildingType.QUEEN_CHAMBER, BuildingVisualStage.COMPLETE, ColonyCulture.LEAFCUTTER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(fire), BuildingType.QUEEN_CHAMBER, BuildingVisualStage.COMPLETE, ColonyCulture.FIRE);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(carpenter), BuildingType.QUEEN_CHAMBER, BuildingVisualStage.COMPLETE, ColonyCulture.CARPENTER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(amberFood), BuildingType.FOOD_STORE, BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(leafcutterFood), BuildingType.FOOD_STORE, BuildingVisualStage.COMPLETE, ColonyCulture.LEAFCUTTER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(fireFood), BuildingType.FOOD_STORE, BuildingVisualStage.COMPLETE, ColonyCulture.FIRE);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(carpenterFood), BuildingType.FOOD_STORE, BuildingVisualStage.COMPLETE, ColonyCulture.CARPENTER);
		BlockPos amberSignature = amber.offset(0, 0, 58);
		BlockPos leafcutterSignature = leafcutter.offset(0, 0, 58);
		BlockPos fireSignature = fire.offset(0, 0, 58);
		BlockPos carpenterSignature = carpenter.offset(0, 0, 58);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(amberSignature), BuildingType.DIPLOMACY_SHRINE, BuildingVisualStage.COMPLETE, ColonyCulture.AMBER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(leafcutterSignature), BuildingType.FUNGUS_GARDEN, BuildingVisualStage.COMPLETE, ColonyCulture.LEAFCUTTER);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(fireSignature), BuildingType.WATCH_POST, BuildingVisualStage.COMPLETE, ColonyCulture.FIRE);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(carpenterSignature), BuildingType.RESIN_DEPOT, BuildingVisualStage.COMPLETE, ColonyCulture.CARPENTER);

		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, amber.offset(-5, 1, 4));
		helper.assertBlockPresent(Blocks.MOSS_BLOCK, leafcutter.offset(-5, 1, 4));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, fire.offset(-5, 1, 4));
		helper.assertBlockPresent(Blocks.MANGROVE_PLANKS, carpenter.offset(-5, 1, 4));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, amber.offset(-6, 1, 3));
		helper.assertBlockPresent(Blocks.BROWN_MUSHROOM_BLOCK, leafcutter.offset(-6, 2, 3));
		helper.assertBlockPresent(Blocks.BLACKSTONE, fire.offset(-6, 1, 3));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, carpenter.offset(-6, 2, 3));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, amberFood.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.BROWN_MUSHROOM_BLOCK, leafcutterFood.offset(4, 2, 1));
		helper.assertBlockPresent(Blocks.BLACKSTONE, fireFood.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, carpenterFood.offset(4, 2, 1));
		helper.assertBlockPresent(Blocks.MOSS_BLOCK, leafcutterFood.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, fireFood.offset(4, 2, 1));
		helper.assertBlockPresent(Blocks.MANGROVE_PLANKS, carpenterFood.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, amberSignature.offset(4, 1, 1));
		helper.assertBlockPresent(Blocks.MOSS_BLOCK, leafcutterSignature.offset(-2, 0, -10));
		helper.assertBlockPresent(Blocks.BROWN_MUSHROOM_BLOCK, leafcutterSignature.offset(-2, 1, -10));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, fireSignature.offset(-2, 0, -4));
		helper.assertBlockPresent(Blocks.BLACKSTONE, fireSignature.offset(-2, 1, -4));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, carpenterSignature.offset(4, 1, -3));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, carpenterSignature.offset(-4, 2, 3));
		helper.succeed();
	}

	@GameTest
	public void cultureStarterQueuesAreAppliedToCreatedColonies(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 150);
		ColonySavedState.get(helper.getLevel().getServer()).clearColonies();

		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(-72, 0, 0)), ColonyCulture.AMBER), BuildingType.DIPLOMACY_SHRINE);
		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(-24, 0, 0)), ColonyCulture.LEAFCUTTER), BuildingType.FUNGUS_GARDEN);
		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(24, 0, 0)), ColonyCulture.FIRE), BuildingType.WATCH_POST);
		assertStarterQueueStarts(helper, ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(72, 0, 0)), ColonyCulture.CARPENTER), BuildingType.RESIN_DEPOT);

		helper.succeed();
	}

	@GameTest
	public void starterSideChambersUseLowOrganicPods(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		BlockPos food = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0);
		BlockPos nursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0);
		BlockPos mine = ColonyBuilder.siteFor(origin, BuildingType.MINE, 0);
		BlockPos barracks = ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0);

		helper.assertBlockPresent(ModBlocks.FOOD_CHAMBER, food);
		helper.assertBlockPresent(ModBlocks.NURSERY_CHAMBER, nursery);
		helper.assertBlockPresent(ModBlocks.MINE_CHAMBER, mine);
		helper.assertBlockPresent(Blocks.BROWN_MUSHROOM_BLOCK, food.offset(6, 1, 0));
		helper.assertBlockPresent(Blocks.DIRT_PATH, food.offset(9, 0, 0));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, food.offset(-4, 1, -2));
		helper.assertBlockPresent(ModBlocks.ORE_NODE, mine.offset(-2, 1, 1));
		helper.assertBlockPresent(Blocks.BROWN_MUSHROOM_BLOCK, food.offset(-3, 1, 4));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, nursery.offset(4, 1, -3));
		helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE_WALL, mine.offset(-5, 1, 2));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, barracks.offset(-5, 1, -1));
		if (!helper.getLevel().getBlockState(helper.absolutePos(food.offset(9, 1, 0))).isAir()) {
			helper.fail("Starter food chamber should have an open side tunnel mouth.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(food.offset(4, 2, 3))).isAir()) {
			helper.fail("Starter food chamber should have a broken organic edge, not an unbroken slab.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(mine.offset(2, 1, 5))).isAir()) {
			helper.fail("Starter mine chamber should have an exposed cut face.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(barracks.offset(3, 1, -4))).isAir()) {
			helper.fail("Starter barracks should have a split front profile instead of a flat wall.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(barracks.offset(4, 1, -5))).isAir()) {
			helper.fail("Starter barracks front edge should be visibly broken from ground view.");
		}
		if (helper.getLevel().getBlockState(helper.absolutePos(food.offset(4, 3, 4))).is(Blocks.CHISELED_TUFF)
				|| helper.getLevel().getBlockState(helper.absolutePos(food.offset(4, 3, 4))).is(Blocks.MUD_BRICKS)) {
			helper.fail("Starter side chambers should not keep the old tall square corner wall materials.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(nursery.offset(-4, 4, -4))).isAir()) {
			helper.fail("Starter side chamber roofline should stay low and non-boxy.");
		}
		helper.succeed();
	}

	@GameTest
	public void buildingVisualStagesPlaceDistinctStructures(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		BlockPos planned = origin.offset(8, 0, 8);
		BlockPos construction = origin.offset(30, 0, 8);
		BlockPos complete = origin.offset(52, 0, 8);
		BlockPos upgraded = origin.offset(8, 0, 38);
		BlockPos damaged = origin.offset(30, 0, 38);
		BlockPos repairing = origin.offset(52, 0, 38);

		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(planned), BuildingType.MARKET, BuildingVisualStage.PLANNED);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(construction), BuildingType.MARKET, BuildingVisualStage.CONSTRUCTION);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(complete), BuildingType.MARKET, BuildingVisualStage.COMPLETE);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(upgraded), BuildingType.MARKET, BuildingVisualStage.UPGRADED);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(damaged), BuildingType.MARKET, BuildingVisualStage.DAMAGED);
		StructurePlacer.placeBuilding(helper.getLevel(), helper.absolutePos(repairing), BuildingType.MARKET, BuildingVisualStage.REPAIRING);

		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, planned.offset(-7, 1, -7));
		helper.assertBlockPresent(Blocks.OAK_FENCE, planned.offset(-3, 1, -7));
		helper.assertBlockPresent(Blocks.OAK_FENCE, planned.offset(-7, 1, -3));
		helper.assertBlockPresent(Blocks.OAK_FENCE, planned.offset(3, 1, 7));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, planned.offset(-5, 0, -8));
		helper.assertBlockPresent(Blocks.COARSE_DIRT, planned.offset(5, 0, -8));
		helper.assertBlockPresent(Blocks.DIRT_PATH, planned.offset(-8, 0, 0));
		helper.assertBlockPresent(Blocks.DIRT_PATH, planned.offset(8, 0, 0));
		helper.assertBlockPresent(Blocks.DIRT_PATH, planned.offset(0, 0, 8));
		helper.assertBlockPresent(Blocks.PACKED_MUD, construction.offset(0, 0, 2));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, construction.offset(-2, 1, -8));
		helper.assertBlockPresent(ModBlocks.MARKET_CHAMBER, complete);
		helper.assertBlockPresent(Blocks.DIRT_PATH, complete.offset(0, 0, -8));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, complete.offset(-2, 1, -8));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, complete.offset(2, 1, -8));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, upgraded.above(6));
		if (!helper.getLevel().getBlockState(helper.absolutePos(damaged.offset(-7, 4, -7))).isAir()) {
			helper.fail("Damaged stage should punch a visible hole in the upper shell.");
		}
		if (!helper.getLevel().getBlockState(helper.absolutePos(damaged.offset(-6, 3, -7))).isAir()) {
			helper.fail("Damaged stage should widen the breach below the upper shell.");
		}
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(-5, 1, -8));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(-3, 1, -8));
		helper.assertBlockPresent(Blocks.BLACKSTONE, damaged.offset(-1, 1, -8));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(1, 1, -8));
		helper.assertBlockPresent(Blocks.BLACKSTONE, damaged.offset(3, 1, -8));
		helper.assertBlockPresent(Blocks.BLACKSTONE, damaged.offset(5, 1, -8));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(-1, 2, -8));
		helper.assertBlockPresent(Blocks.BLACKSTONE, damaged.offset(1, 2, -8));
		helper.assertBlockPresent(Blocks.GRAVEL, damaged.offset(0, 0, -8));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, repairing.offset(-1, 1, -4));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, repairing.offset(-5, 1, -8));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, repairing.offset(-1, 1, -8));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(1, 1, -8));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(3, 1, -8));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(5, 1, -8));
		helper.assertBlockPresent(Blocks.OAK_FENCE, repairing.offset(-7, 2, -7));
		helper.assertBlockPresent(Blocks.OAK_FENCE, repairing.offset(7, 2, -7));
		helper.succeed();
	}

	@GameTest
	public void constructionStageSceneShowsMaterialDelivery(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		VisualQaScenes.seedConstructionStages(helper.getLevel(), colony);
		List<BlockPos> centers = VisualQaScenes.constructionStageBuildingCenters(origin);
		BlockPos construction = centers.get(1);
		BlockPos damaged = centers.get(4);
		BlockPos repairing = centers.get(5);
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, construction.offset(-2, 0, -6));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, construction.offset(4, 0, -7));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, construction.offset(5, 1, -5));
		helper.assertBlockPresent(Blocks.BARREL, construction.offset(5, 1, -6));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(-5, 1, -8));
		helper.assertBlockPresent(Blocks.BLACKSTONE, damaged.offset(5, 1, -8));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, repairing.offset(-5, 1, -8));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(4, 1, -5));
		helper.assertBlockPresent(Blocks.BARREL, repairing.offset(4, 1, -6));

		BlockPos absoluteConstruction = helper.absolutePos(construction);
		AABB sceneBounds = new AABB(
				absoluteConstruction.getX() - 10, absoluteConstruction.getY() - 2, absoluteConstruction.getZ() - 12,
				absoluteConstruction.getX() + 10, absoluteConstruction.getY() + 8, absoluteConstruction.getZ() + 6
		);
		List<AntEntity> workers = helper.getLevel().getEntitiesOfClass(AntEntity.class, sceneBounds, ant -> ant.colonyId() == colony.id());
		if (workers.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| workers.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_RESIN)) {
			helper.fail("Construction stage should show a working builder and resin carrier.");
		}
		if (helper.getLevel().getEntitiesOfClass(Display.class, sceneBounds).size() < 2) {
			helper.fail("Construction stage should include visible carried-material markers.");
		}
		BlockPos absoluteDamaged = helper.absolutePos(damaged);
		BlockPos absoluteRepairing = helper.absolutePos(repairing);
		AABB lateStageBounds = new AABB(
				absoluteDamaged.getX() - 8, absoluteDamaged.getY() - 2, absoluteDamaged.getZ() - 10,
				absoluteRepairing.getX() + 8, absoluteRepairing.getY() + 8, absoluteRepairing.getZ() + 4
		);
		List<AntEntity> lateStageAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, lateStageBounds, ant -> ant.colonyId() == colony.id());
		if (lateStageAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.PATROLLING)
				|| lateStageAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| lateStageAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_CHITIN)) {
			helper.fail("Construction stage should show damaged inspection plus active chitin repair cues.");
		}
		helper.succeed();
	}

	@GameTest
	public void starterAntsSpawnAboveSupportedFloor(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		AABB area = new AABB(
				helper.absolutePos(origin).getX() - 16, helper.absolutePos(origin).getY() - 2, helper.absolutePos(origin).getZ() - 16,
				helper.absolutePos(origin).getX() + 16, helper.absolutePos(origin).getY() + 8, helper.absolutePos(origin).getZ() + 16
		);
		for (AntEntity ant : helper.getLevel().getEntitiesOfClass(AntEntity.class, area, ant -> ant.colonyId() == colony.id())) {
			if (helper.getLevel().getBlockState(ant.blockPosition().below()).isAir()) {
				helper.fail("Starter ant should stand above a solid floor at " + ant.blockPosition().toShortString() + ".");
			}
		}
		for (BlockPos spawn : List.of(
				origin.offset(0, 1, -11),
				origin.offset(2, 1, -11),
				origin.offset(-2, 1, -11),
				origin.offset(0, 1, -13),
				origin.offset(3, 1, -13),
				origin.offset(-4, 1, -12),
				origin.offset(4, 1, -12)
		)) {
			if (!helper.getLevel().getBlockState(helper.absolutePos(spawn)).isAir()
					|| !helper.getLevel().getBlockState(helper.absolutePos(spawn.above())).isAir()
					|| !helper.getLevel().getBlockState(helper.absolutePos(spawn.above(2))).isAir()) {
				helper.fail("Starter spawn point should keep open headroom at " + spawn.toShortString());
			}
		}
		helper.succeed();
	}

	@GameTest
	public void rivalColoniesUseStageFourCultures(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), false);

		if (rival.progress().culture() == ColonyCulture.AMBER) {
			helper.fail("Rival colony should use a non-amber culture.");
		}
		if (rival.resource(ResourceType.RESIN) <= 0 || rival.resource(ResourceType.FUNGUS) <= 0 || rival.resource(ResourceType.VENOM) <= 0) {
			helper.fail("Stage 4 colonies should seed advanced resources.");
		}
		helper.succeed();
	}

	@GameTest
	public void rivalRaidLeavesVisibleTrailAndDamagedTarget(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 104);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(-46, 0, 0)), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(46, 0, 0)), false);
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 2);

		if (!RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Rival raid should execute once cooldown and relation allow it.");
		}
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.DIPLOMACY_SCENE)) {
			helper.fail("Visual QA should expose a diplomacy_scene.");
		}

		boolean hasTrail = false;
		boolean hasWarningMarker = false;
		for (int x = -24; x <= 24; x++) {
			BlockPos trail = origin.offset(x, 0, 0);
			var ground = helper.getLevel().getBlockState(helper.absolutePos(trail));
			var marker = helper.getLevel().getBlockState(helper.absolutePos(trail.above()));
			hasTrail |= ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.COARSE_DIRT);
			hasWarningMarker |= marker.is(Blocks.RED_TERRACOTTA) || marker.is(Blocks.BLACKSTONE);
		}
		if (!hasTrail || !hasWarningMarker) {
			helper.fail("Rival raid should leave a readable trail with warning markers between colonies.");
		}
		if (allied.progress().buildingsView().stream().noneMatch(ColonyBuilding::damaged)) {
			helper.fail("Rival raid should visibly damage one allied non-queen building.");
		}
		helper.succeed();
	}

	@GameTest
	public void queenVaultAbsorbsRaidQueenDamage(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 104);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(-46, 0, 0)), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(46, 0, 0)), false);
		allied.progress().buildQueue().clear();
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.addCaste(AntCaste.GIANT, -allied.casteCount(AntCaste.GIANT));
		BlockPos vault = ColonyBuilder.siteFor(allied, BuildingType.QUEEN_VAULT);
		allied.progress().addBuilding(ColonyBuilding.complete(BuildingType.QUEEN_VAULT, vault));
		StructurePlacer.placeBuilding(helper.getLevel(), vault, BuildingType.QUEEN_VAULT, BuildingVisualStage.COMPLETE, allied.progress().culture());
		int queenHealthBeforeRaid = allied.queenHealth();

		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 20);

		if (!RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Rival raid should execute so the Queen Vault protection can be observed.");
		}
		if (allied.queenHealth() != queenHealthBeforeRaid) {
			helper.fail("Completed Queen Vault should absorb raid queen damage; expected " + queenHealthBeforeRaid + ", got " + allied.queenHealth());
		}
		if (!allied.currentTask().contains("Queen Vault absorbed")) {
			helper.fail("Queen Vault protection should be visible in current task, got " + allied.currentTask());
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("Queen Vault absorbed"))) {
			helper.fail("Queen Vault protection should leave an event-log entry.");
		}
		helper.succeed();
	}

	@GameTest
	public void tributeDiplomacyPlacesVisiblePactMarkers(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 120);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData treaty = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), ColonyCulture.CARPENTER);
		allied.progress().setRelation(treaty.id(), DiplomacyState.ALLY);
		treaty.progress().setRelation(allied.id(), DiplomacyState.ALLY);

		if (!DiplomacyConsequences.apply(helper.getLevel(), allied, treaty, DiplomacyAction.TRIBUTE, DiplomacyState.ALLY)) {
			helper.fail("Tribute ending in alliance should place a visible diplomacy consequence.");
		}
		if (allied.progress().relationTo(treaty.id()) != DiplomacyState.ALLY || treaty.progress().relationTo(allied.id()) != DiplomacyState.ALLY) {
			helper.fail("Tribute pact test setup should expose an allied relation in both colonies.");
		}

		BlockPos sourceBeacon = origin.offset(12, 0, 0);
		BlockPos treatyBeacon = origin.offset(52, 0, 0);
		BlockPos cache = origin.offset(32, 0, 0);
		BlockPos caravan = origin.offset(32, 0, -18);
		assertBlockInColumn(helper, sourceBeacon, Blocks.HONEYCOMB_BLOCK, 1, 3, "source tribute beacon");
		assertBlockInColumn(helper, sourceBeacon, Blocks.CANDLE, 2, 4, "source tribute candle");
		assertBlockInColumn(helper, treatyBeacon, Blocks.HONEYCOMB_BLOCK, 1, 3, "target tribute beacon");
		assertBlockInColumn(helper, cache, Blocks.HONEYCOMB_BLOCK, 0, 2, "tribute cache base");
		assertBlockInColumn(helper, cache, Blocks.AMETHYST_BLOCK, 1, 3, "tribute cache gem");
		assertBlockInColumn(helper, cache, Blocks.CANDLE, 2, 4, "tribute cache candle");
		assertBlockInColumn(helper, caravan, Blocks.BARREL, 1, 2, "tribute caravan supply barrel");
		helper.assertBlockPresent(Blocks.HAY_BLOCK, caravan.offset(-1, 1, 0));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, caravan.offset(1, 1, 0));

		BlockPos absoluteCaravan = helper.absolutePos(caravan);
		AABB caravanBounds = new AABB(
				absoluteCaravan.getX() - 5, absoluteCaravan.getY(), absoluteCaravan.getZ() - 5,
				absoluteCaravan.getX() + 5, absoluteCaravan.getY() + 5, absoluteCaravan.getZ() + 5
		);
		List<AntEntity> caravanAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, caravanBounds, ant ->
				ant.caste() == AntCaste.WORKER && (ant.colonyId() == allied.id() || ant.colonyId() == treaty.id()));
		if (caravanAnts.stream().noneMatch(ant -> ant.colonyId() == allied.id() && ant.workState() == AntWorkState.CARRYING_RESIN)
				|| caravanAnts.stream().noneMatch(ant -> ant.colonyId() == treaty.id() && ant.workState() == AntWorkState.CARRYING_FUNGUS)) {
			helper.fail("Tribute pact should leave a visible two-colony caravan carrying resin and fungus.");
		}
		helper.succeed();
	}

	@GameTest
	public void alliedMarketTradeCaravanRunsOutsideTributeDiplomacy(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 120);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData tradeAlly = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), ColonyCulture.CARPENTER);
		for (ColonyData colony : List.of(allied, tradeAlly)) {
			BlockPos market = ColonyBuilder.siteFor(colony, BuildingType.MARKET);
			if (!colony.progress().hasCompleted(BuildingType.MARKET)) {
				colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.MARKET, market));
				StructurePlacer.placeBuilding(helper.getLevel(), market, BuildingType.MARKET, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}
		allied.progress().setRelation(tradeAlly.id(), DiplomacyState.ALLY);
		tradeAlly.progress().setRelation(allied.id(), DiplomacyState.ALLY);
		allied.progress().requests().clear();
		allied.setResource(ResourceType.FOOD, 160);
		allied.setResource(ResourceType.CHITIN, 0);
		tradeAlly.setResource(ResourceType.RESIN, 40);
		int alliedFood = allied.resource(ResourceType.FOOD);
		int alliedResin = allied.resource(ResourceType.RESIN);
		int targetFood = tradeAlly.resource(ResourceType.FOOD);
		int targetResin = tradeAlly.resource(ResourceType.RESIN);
		allied.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Allied market colonies should trigger a standalone recurring trade caravan.");
		}
		if (!allied.currentTask().contains("trade caravan")) {
			helper.fail("Trade caravan should be visible in current task, got " + allied.currentTask());
		}
		if (allied.resource(ResourceType.FOOD) != alliedFood - 8 || allied.resource(ResourceType.RESIN) != alliedResin + 8) {
			helper.fail("Trade caravan should exchange allied food for carpenter resin.");
		}
		if (tradeAlly.resource(ResourceType.FOOD) != targetFood + 8 || tradeAlly.resource(ResourceType.RESIN) != targetResin - 8) {
			helper.fail("Trade caravan should update the partner colony resources too.");
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("trade caravan"))
				|| tradeAlly.progress().eventsView().stream().noneMatch(event -> event.message().contains("trade caravan"))) {
			helper.fail("Trade caravan should leave event-log proof on both colonies.");
		}
		ColonyUiSnapshot tradeSnapshot = ColonyUiSnapshot.from(allied, "Trade", "");
		if (!tradeSnapshot.tradeActivity().contains("8 Food -> 8 Resin")
				|| !tradeSnapshot.tradeActivity().contains("#" + tradeAlly.id())) {
			helper.fail("Trade tab should summarize the latest caravan payoff, got " + tradeSnapshot.tradeActivity());
		}

		BlockPos camp = ColonyRecurringEvents.tradeCaravanCamp(origin, origin.offset(64, 0, 0));
		assertBlockInColumn(helper, camp, Blocks.BARREL, 1, 2, "trade caravan supply barrel");
		helper.assertBlockPresent(Blocks.HAY_BLOCK, camp.offset(-1, 1, 0));
		helper.assertBlockPresent(Blocks.HONEY_BLOCK, camp.offset(1, 1, 0));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, camp.offset(0, 1, 1));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(8, 0, 0));

		BlockPos absoluteCamp = helper.absolutePos(camp);
		AABB caravanBounds = new AABB(
				absoluteCamp.getX() - 5, absoluteCamp.getY(), absoluteCamp.getZ() - 5,
				absoluteCamp.getX() + 5, absoluteCamp.getY() + 6, absoluteCamp.getZ() + 5
		);
		List<AntEntity> carriers = helper.getLevel().getEntitiesOfClass(AntEntity.class, caravanBounds, ant ->
				ant.caste() == AntCaste.WORKER
						&& (ant.workState() == AntWorkState.CARRYING_FOOD || ant.workState() == AntWorkState.CARRYING_RESIN));
		if (carriers.stream().noneMatch(ant -> ant.colonyId() == allied.id() && ant.workState() == AntWorkState.CARRYING_FOOD)
				|| carriers.stream().noneMatch(ant -> ant.colonyId() == tradeAlly.id() && ant.workState() == AntWorkState.CARRYING_RESIN)) {
			helper.fail("Trade caravan should show two market carriers with food and resin cargo.");
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Trade caravan should not repeat until the next recurring-event interval.");
		}
		helper.succeed();
	}

	@GameTest
	public void truceDiplomacyCoolsRaidRouteWithVisibleMarkers(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 120);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), false);
		allied.progress().setRelation(rival.id(), DiplomacyState.NEUTRAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.NEUTRAL);
		allied.progress().setRaidCooldown(0);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 4);

		if (!DiplomacyConsequences.apply(helper.getLevel(), allied, rival, DiplomacyAction.TRUCE, DiplomacyState.NEUTRAL)) {
			helper.fail("Truce ending in neutral relation should place a visible diplomacy consequence.");
		}
		if (allied.progress().raidCooldown() < DiplomacyConsequences.TRUCE_COOLDOWN_TICKS
				|| rival.progress().raidCooldown() < DiplomacyConsequences.TRUCE_COOLDOWN_TICKS) {
			helper.fail("Truce should cool down immediate raids for both colonies.");
		}

		BlockPos sourceSeal = origin.offset(12, 0, 0);
		BlockPos rivalSeal = origin.offset(52, 0, 0);
		BlockPos truceCache = origin.offset(32, 0, 0);
		assertBlockInColumn(helper, sourceSeal, Blocks.CHISELED_TUFF, 1, 3, "source truce seal");
		assertBlockInColumn(helper, rivalSeal, Blocks.CHISELED_TUFF, 1, 3, "target truce seal");
		assertBlockInColumn(helper, truceCache, Blocks.MOSS_BLOCK, 0, 2, "truce cache moss");
		assertBlockInColumn(helper, truceCache, Blocks.CANDLE, 2, 4, "truce cache candle");
		if (RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Neutral truce relation should prevent an immediate raid tick.");
		}
		helper.succeed();
	}

	@GameTest
	public void warPactDiplomacyPlacesVisibleMusterRoute(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 130);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), false);
		allied.progress().setRelation(rival.id(), DiplomacyState.WAR);
		rival.progress().setRelation(allied.id(), DiplomacyState.WAR);
		allied.progress().setRaidCooldown(600);
		rival.progress().setRaidCooldown(600);

		if (!DiplomacyConsequences.apply(helper.getLevel(), allied, rival, DiplomacyAction.WAR_PACT, DiplomacyState.WAR)) {
			helper.fail("War pact ending in war relation should place a visible diplomacy consequence.");
		}
		if (allied.progress().raidCooldown() != 0 || rival.progress().raidCooldown() != 0) {
			helper.fail("War pact should open an immediate raid window for both colonies.");
		}

		BlockPos sourceMuster = origin.offset(12, 0, -14);
		BlockPos rivalMuster = origin.offset(52, 0, -14);
		BlockPos warLine = origin.offset(32, 0, -14);
		assertBlockInColumn(helper, sourceMuster, Blocks.RED_TERRACOTTA, 1, 3, "source war beacon");
		assertBlockInColumn(helper, sourceMuster, Blocks.CANDLE, 3, 5, "source war candle");
		assertBlockInColumn(helper, rivalMuster, Blocks.RED_TERRACOTTA, 1, 3, "target war beacon");
		assertBlockInColumn(helper, warLine, Blocks.BONE_BLOCK, 1, 3, "war pact muster bone");
		assertBlockInColumn(helper, warLine, Blocks.BLACKSTONE, 0, 2, "war pact muster blackstone");
		AABB musterBounds = new AABB(
				helper.absolutePos(sourceMuster).getX() - 5, helper.absolutePos(sourceMuster).getY(), helper.absolutePos(sourceMuster).getZ() - 5,
				helper.absolutePos(sourceMuster).getX() + 5, helper.absolutePos(sourceMuster).getY() + 6, helper.absolutePos(sourceMuster).getZ() + 5
		);
		List<AntEntity> musteredAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, musterBounds, ant -> ant.colonyId() == allied.id() && ant.workState() == AntWorkState.PATROLLING);
		if (musteredAnts.size() < 4) {
			helper.fail("War pact should visibly muster four patrolling source-colony ants, got " + musteredAnts.size());
		}
		helper.succeed();
	}

	@GameTest
	public void alliedDefensivePactSendsVisibleGuardResponse(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 130);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(-46, 0, 0)), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(46, 0, 0)), false);
		ColonyData treaty = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(0, 0, 36)), ColonyCulture.CARPENTER);
		allied.addCaste(AntCaste.SOLDIER, -allied.casteCount(AntCaste.SOLDIER));
		allied.addCaste(AntCaste.MAJOR, -allied.casteCount(AntCaste.MAJOR));
		allied.progress().setRaidCooldown(600);
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		allied.progress().setRelation(treaty.id(), DiplomacyState.ALLY);
		treaty.progress().setRelation(allied.id(), DiplomacyState.ALLY);
		rival.progress().setRaidCooldown(0);
		rival.addCaste(AntCaste.SOLDIER, 2);

		if (!RaidPlanner.tick(helper.getLevel(), savedState)) {
			helper.fail("Rival raid should execute and allow the allied defensive pact to answer.");
		}

		BlockPos rally = origin.offset(-28, 0, 8);
		assertBlockInColumn(helper, rally, Blocks.POLISHED_DEEPSLATE, 1, 3, "defensive pact guard post");
		assertBlockInColumn(helper, rally, Blocks.HONEYCOMB_BLOCK, 2, 4, "defensive pact ally signal");
		assertBlockInColumn(helper, rally, Blocks.CANDLE, 3, 5, "defensive pact candle");
		helper.assertBlockPresent(Blocks.BONE_BLOCK, rally.north());
		helper.assertBlockPresent(Blocks.BONE_BLOCK, rally.south());
		AABB guardBounds = new AABB(
				helper.absolutePos(rally).getX() - 5, helper.absolutePos(rally).getY(), helper.absolutePos(rally).getZ() - 5,
				helper.absolutePos(rally).getX() + 5, helper.absolutePos(rally).getY() + 6, helper.absolutePos(rally).getZ() + 5
		);
		List<AntEntity> guardAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, guardBounds, ant -> ant.colonyId() == treaty.id() && ant.workState() == AntWorkState.PATROLLING);
		if (guardAnts.size() < 3) {
			helper.fail("Defensive pact should place three allied patrolling guards near the raid route, got " + guardAnts.size());
		}
		if (!allied.currentTask().contains("Defensive pact") || !treaty.currentTask().contains("Defensive pact")) {
			helper.fail("Defensive pact response should be surfaced in both allied colony tasks.");
		}
		helper.succeed();
	}

	@GameTest
	public void survivalDiscoveryFindsStableWildEncounterSite(GameTestHelper helper) {
		BlockPos playerPos = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, playerPos, 150);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		long seed = 0x46F06D1CL;

		Optional<BlockPos> first = ColonyDiscoveryService.findEncounterSite(helper.getLevel(), savedState, helper.absolutePos(playerPos), seed);
		Optional<BlockPos> second = ColonyDiscoveryService.findEncounterSite(helper.getLevel(), savedState, helper.absolutePos(playerPos), seed);
		if (first.isEmpty()) {
			helper.fail("Survival discovery should find a nearby flat encounter site.");
		}
		if (!first.equals(second)) {
			helper.fail("Survival discovery should choose the same site for the same seed and player region.");
		}
		double distance = first.get().distSqr(helper.absolutePos(playerPos));
		if (distance < ColonyDiscoveryService.DISCOVERY_DISTANCE_MIN * ColonyDiscoveryService.DISCOVERY_DISTANCE_MIN) {
			helper.fail("Survival discovery should not spawn directly on top of the player.");
		}

		ColonyData colony = ColonyDiscoveryService.spawnEncounter(helper.getLevel(), savedState, helper.absolutePos(playerPos), seed).orElse(null);
		if (colony == null) {
			helper.fail("Survival discovery should spawn the wild colony once a site is found.");
		}
		if (!"wild".equals(colony.progress().faction()) || colony.progress().playerAllied()) {
			helper.fail("Discovered colony should be a non-allied wild colony.");
		}
		if (!helper.getLevel().getBlockState(colony.origin()).is(ModBlocks.NEST_MOUND)) {
			helper.fail("Discovered wild colony should place a visible starter mound.");
		}
		if (!isLandmarkTrail(helper.getLevel().getBlockState(colony.origin().offset(-12, 0, -42)).getBlock())) {
			helper.fail("Discovered wild colony should mark a readable surface trail from the mound.");
		}
		if (!helper.getLevel().getBlockState(colony.origin().offset(0, 0, -58)).is(ModBlocks.FOOD_NODE)) {
			helper.fail("Discovered wild colony should expose a forage patch before the player opens UI.");
		}
		if (!helper.getLevel().getBlockState(colony.origin().offset(54, 1, -16)).is(landmarkMarker(colony.progress().culture()))) {
			helper.fail("Discovered wild colony should place a culture-colored boundary marker.");
		}
		BlockPos ruinedScoutNest = colony.origin().offset(38, 0, 22);
		if (!helper.getLevel().getBlockState(ruinedScoutNest).is(ModBlocks.NEST_MOUND)
				|| !helper.getLevel().getBlockState(ruinedScoutNest.below()).is(ModBlocks.NEST_CORE)
				|| !helper.getLevel().getBlockState(ruinedScoutNest.offset(-2, 1, 0)).is(ModBlocks.CHITIN_NODE)
				|| !helper.getLevel().getBlockState(ruinedScoutNest.offset(2, 1, 0)).is(Blocks.BONE_BLOCK)) {
			helper.fail("Discovered wild colony should place a readable collapsed scout nest landmark.");
		}
		if (!isLandmarkTrail(helper.getLevel().getBlockState(colony.origin().offset(24, 0, 10)).getBlock())) {
			helper.fail("Collapsed scout nest should be connected back to the mound by a surface trail.");
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("Collapsed scout nest"))) {
			helper.fail("Collapsed scout nest landmark should be surfaced in the colony event log.");
		}
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.WORLDGEN_ENCOUNTER)) {
			helper.fail("Visual QA should expose a worldgen_encounter scene.");
		}
		BlockPos qaOrigin = helper.absolutePos(playerPos);
		Vec3 camera = VisualQaScenes.worldgenEncounterCamera(qaOrigin);
		Vec3 target = VisualQaScenes.worldgenEncounterTarget(qaOrigin);
		BlockPos camp = VisualQaScenes.worldgenEncounterCamp(qaOrigin);
		if (Math.abs(camera.x - qaOrigin.getX()) > 22.0 || camera.y > qaOrigin.getY() + 19.0 || camera.z < qaOrigin.getZ() + 36.0 || camera.z > qaOrigin.getZ() + 44.0) {
			helper.fail("Worldgen encounter camera should stay centered and close enough to avoid empty foreground.");
		}
		if (target.z < qaOrigin.getZ() - 24.0 || target.z > qaOrigin.getZ() - 12.0) {
			helper.fail("Worldgen encounter camera target should keep the trail and wild colony in frame.");
		}
		if (camp.getZ() > camera.z - 10.0 || camp.getZ() < qaOrigin.getZ() + 18) {
			helper.fail("Worldgen encounter camp should sit far enough ahead of the camera to stay visible with the trail.");
		}
		BlockPos approachTrailHead = firstApproachTrailBlock(helper, helper.absolutePos(playerPos), colony.origin());
		if (approachTrailHead == null) {
			helper.fail("Discovered wild colony should place a visible approach trail from the player side.");
		}
		if (!helper.getLevel().getBlockState(approachTrailHead.above()).isAir()) {
			helper.fail("Approach trail head should be ground dressing, not an isolated upright marker.");
		}
		if (!hasGroundedTrailHeadDressing(helper, approachTrailHead, colony.origin(), colony.progress().culture())) {
			helper.fail("Approach trail head should be integrated with side dirt/root dressing.");
		}
		helper.succeed();
	}

	@GameTest
	public void colonyOverviewSceneKeepsPreparedGroundBeyondCamera(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.COLONY_OVERVIEW)) {
			helper.fail("Visual QA should expose a colony_overview scene.");
		}
		BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
		int radius = VisualQaScenes.qaRadius(VisualQaScenes.COLONY_OVERVIEW);
		Vec3 camera = VisualQaScenes.colonyOverviewCamera(origin);
		Vec3 target = VisualQaScenes.colonyOverviewTarget(origin);
		int cameraGroundDistance = Math.max(
				Math.abs((int) Math.round(camera.x) - origin.getX()),
				Math.abs((int) Math.round(camera.z) - origin.getZ())
		);
		if (radius - cameraGroundDistance < 40) {
			helper.fail("Colony overview camera needs enough prepared terrain beyond the foreground; radius "
					+ radius + " leaves only " + (radius - cameraGroundDistance) + " blocks.");
		}
		if (target.y < origin.getY() + 4.0 || target.y > origin.getY() + 6.5) {
			helper.fail("Colony overview target should stay on the starter mound instead of drifting to empty terrain.");
		}
		for (BlockPos landmark : List.of(
				origin,
				ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0),
				ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0),
				ColonyBuilder.siteFor(origin, BuildingType.MINE, 0),
				ColonyBuilder.siteFor(origin, BuildingType.BARRACKS, 0),
				origin.offset(54, 0, 8),
				origin.offset(8, 0, 54),
				origin.offset(-54, 0, 8)
		)) {
			int landmarkDistance = Math.max(Math.abs(landmark.getX() - origin.getX()), Math.abs(landmark.getZ() - origin.getZ()));
			if (radius - landmarkDistance < 18) {
				helper.fail("Colony overview prepared area should leave terrain beyond starter landmark "
						+ landmark.toShortString() + ".");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void settlementScaleSceneAndLayoutUseLargeVillageFootprint(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.SETTLEMENT_SCALE)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_RESEARCH_MAP)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_MARKET)
				|| !VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_REQUESTS)) {
			helper.fail("Visual QA should expose the settlement scale and dedicated tablet renovation scenes.");
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		BlockPos food = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0);
		BlockPos nursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0);
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		BlockPos watch = ColonyBuilder.siteFor(origin, BuildingType.WATCH_POST, 0);
		if (Math.abs(food.getX() - origin.getX()) < 36 || Math.abs(nursery.getX() - origin.getX()) < 36) {
			helper.fail("Starter side chambers should move out to the large village ring.");
		}
		if (Math.max(Math.abs(market.getX() - origin.getX()), Math.abs(market.getZ() - origin.getZ())) < 34) {
			helper.fail("Market should live in the larger diagonal village district.");
		}
		if (Math.max(Math.abs(watch.getX() - origin.getX()), Math.abs(watch.getZ() - origin.getZ())) < 50) {
			helper.fail("Watch posts should mark the outer claim edge in the scale pass.");
		}
		if (VisualQaScenes.qaRadius(VisualQaScenes.SETTLEMENT_SCALE) < 120) {
			helper.fail("Settlement scale scene needs a wide prepared area for the enlarged village.");
		}
		helper.succeed();
	}

	@GameTest
	public void archiveCanCompleteFirstResearch(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.PHEROMONE_ARCHIVE, helper.absolutePos(new BlockPos(4, 3, 4))));
		colony.setResource(ResourceType.KNOWLEDGE, 40);
		colony.setResource(ResourceType.RESIN, 40);
		colony.setResource(ResourceType.ORE, 40);

		if (!ColonyLogistics.startResearch(colony, ResearchNode.RESIN_MASONRY.id()).started()) {
			helper.fail("Archive should start Resin Masonry when resources are present.");
		}
		for (int i = 0; i < 6; i++) {
			ColonyLogistics.tick(colony);
		}
		if (!colony.progress().hasResearch(ResearchNode.RESIN_MASONRY.id())) {
			helper.fail("Research should complete after archive ticks.");
		}
		helper.succeed();
	}

	@GameTest
	public void colonyRenovatePreservesEconomyAndPlacesCampus(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.setResource(ResourceType.RESIN, 77);
		colony.progress().completeResearch(ResearchNode.CHITIN_CULTIVATION.id());

		ColonyService.renovateColony(helper.getLevel(), colony);

		if (colony.resource(ResourceType.RESIN) != 77) {
			helper.fail("Renovation should preserve resources.");
		}
		if (!colony.progress().hasResearch(ResearchNode.CHITIN_CULTIVATION.id())) {
			helper.fail("Renovation should preserve research.");
		}
		helper.assertBlockPresent(ModBlocks.COLONY_LEDGER, new BlockPos(5, 4, 2));
		helper.assertBlockPresent(ModBlocks.FOOD_CHAMBER, ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0));
		helper.succeed();
	}

	@GameTest
	public void colonyLabelsAndImportantAntNamesAreVisible(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		AABB area = new AABB(
				helper.absolutePos(origin).getX() - 48, helper.absolutePos(origin).getY() - 4, helper.absolutePos(origin).getZ() - 48,
				helper.absolutePos(origin).getX() + 48, helper.absolutePos(origin).getY() + 16, helper.absolutePos(origin).getZ() + 48
		);
		boolean hasLabel = !helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, area, label -> label.getCustomName() != null).isEmpty();
		if (!hasLabel) {
			helper.fail("Colony should create visible building labels.");
		}
		boolean hasIdentityLabel = !helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, area, label -> label.getCustomName() != null && label.getCustomName().getString().contains(colony.progress().name())).isEmpty();
		if (!hasIdentityLabel) {
			helper.fail("Queen mound label should show the colony name.");
		}
		boolean hasQueenName = helper.getLevel().getEntitiesOfClass(AntEntity.class, area, ant -> ant.getCustomName() != null && ant.getCustomName().getString().contains("Queen")).stream().findAny().isPresent();
		if (!hasQueenName) {
			helper.fail("Queen should have a visible name.");
		}
		helper.succeed();
	}

	@GameTest
	public void workerAssignmentUsesDistantCampusTargets(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		Optional<BlockPos> target = AntEntity.debugWorkTarget(helper.getLevel(), AntCaste.WORKER, colony.id(), helper.absolutePos(origin));
		if (target.isEmpty()) {
			helper.fail("Worker should receive a colony-aware work target.");
		}
		if (target.get().distSqr(colony.origin()) <= 18 * 18) {
			helper.fail("Worker target should reach beyond the old 18 block scan radius.");
		}
		helper.succeed();
	}

	@GameTest
	public void minerAssignmentTargetsOreNodeBeyondOldRadius(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		BlockPos oreNode = helper.absolutePos(origin.offset(8, 0, 54));

		Optional<BlockPos> target = AntEntity.debugWorkTarget(helper.getLevel(), AntCaste.MINER, colony.id(), helper.absolutePos(origin));
		if (target.isEmpty()) {
			helper.fail("Miner should receive an ore node target.");
		}
		if (target.get().distSqr(oreNode) > 9) {
			helper.fail("Miner should target the campus ore node.");
		}
		helper.succeed();
	}

	@GameTest
	public void soldierAssignmentUsesCampusPatrolPoint(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		Optional<BlockPos> target = AntEntity.debugWorkTarget(helper.getLevel(), AntCaste.SOLDIER, colony.id(), helper.absolutePos(origin));
		if (target.isEmpty()) {
			helper.fail("Soldier should receive a patrol point.");
		}
		if (target.get().distSqr(colony.origin()) <= 18 * 18) {
			helper.fail("Soldier should prefer a campus patrol point, such as barracks or watch posts.");
		}
		if (helper.getLevel().getBlockState(target.get()).isAir()
				|| !helper.getLevel().getBlockState(target.get().above()).isAir()
				|| !helper.getLevel().getBlockState(target.get().above(2)).isAir()) {
			helper.fail("Soldier patrol target should be outside with clear headroom.");
		}
		helper.succeed();
	}

	@GameTest
	public void antSpawnPositionUsesRequestedGroundY(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		BlockPos spawn = helper.absolutePos(origin.offset(8, 1, 0));

		AntEntity ant = ColonyService.spawnAnt(helper.getLevel(), spawn, AntCaste.WORKER, colony.id());
		if (ant == null) {
			helper.fail("Worker ant should spawn.");
		}
		if (Math.abs(ant.getY() - spawn.getY()) > 0.01) {
			helper.fail("Ant spawn should use the provided Y without adding one block.");
		}
		helper.succeed();
	}

	@GameTest
	public void workerConstructionDeliveryAdvancesActiveBuilding(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		ColonyBuilding planned = ColonyBuilding.planned(BuildingType.MARKET, helper.absolutePos(ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0)));
		colony.progress().addBuilding(planned);

		boolean delivered = ColonyService.depositConstructionWork(helper.getLevel(), planned.pos(), colony.id(), AntCaste.WORKER, 9);
		if (!delivered) {
			helper.fail("Worker delivery should find the active construction.");
		}
		if (planned.constructionProgress() <= 0) {
			helper.fail("Worker delivery should advance construction progress.");
		}
		helper.succeed();
	}

	@GameTest
	public void damagedBuildingConsumesChitinAndRepairsVisibly(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.setResource(ResourceType.CHITIN, 40);
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding damaged = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		damaged.disableFor(120);
		colony.progress().addBuilding(damaged);
		StructurePlacer.placeBuilding(helper.getLevel(), damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());

		if (damaged.visualStage() != BuildingVisualStage.DAMAGED) {
			helper.fail("Damaged market should begin in the damaged visual stage.");
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		if (damaged.visualStage() != BuildingVisualStage.REPAIRING) {
			helper.fail("Chitin supplies should move a damaged market into the repairing stage.");
		}
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, market.offset(-1, 1, -4));
		for (int i = 0; i < 4 && damaged.disabledTicks() > 0; i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (damaged.disabledTicks() != 0 || damaged.visualStage() != BuildingVisualStage.COMPLETE) {
			helper.fail("Repair ticks should restore the building to complete service, got "
					+ damaged.visualStage().id() + " progress " + damaged.constructionProgress()
					+ " disabled " + damaged.disabledTicks() + " task " + colony.currentTask());
		}
		helper.assertBlockPresent(ModBlocks.MARKET_CHAMBER, market);
		helper.succeed();
	}

	@GameTest
	public void repairContractDeliveryStartsDamagedBuildingRepair(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.setResource(ResourceType.CHITIN, 0);
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding damaged = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		damaged.disableFor(120);
		colony.progress().addBuilding(damaged);
		StructurePlacer.placeBuilding(helper.getLevel(), damaged.pos(), damaged.type(), damaged.visualStage(), colony.progress().culture());

		ColonyBuilder.tick(helper.getLevel(), colony);
		if (damaged.visualStage() != BuildingVisualStage.DAMAGED) {
			helper.fail("Damaged market should wait in the damaged stage while repair chitin is missing.");
		}
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(entry -> entry.reason().equals("repair market"))
				.findFirst()
				.orElse(null);
		if (contract == null || contract.resource() != ResourceType.CHITIN) {
			helper.fail("Missing repair supplies should open a chitin repair contract.");
		}

		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
		if (!result.success() || !result.complete()) {
			helper.fail("Player chitin delivery should complete the repair contract.");
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		if (damaged.visualStage() != BuildingVisualStage.REPAIRING) {
			helper.fail("Completed repair contract should move the building into visible repair, got " + damaged.visualStage().id());
		}
		if (colony.resource(ResourceType.CHITIN) != 0 || !colony.progress().requestsView().isEmpty()) {
			helper.fail("Repair should consume delivered chitin and clear the repair request.");
		}
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, market.offset(-1, 1, -4));
		helper.succeed();
	}

	@GameTest
	public void constructionContractDeliveryStartsVisibleQueuedSite(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.progress().requests().clear();
		colony.progress().buildQueue().add(BuildingType.MARKET);
		colony.setResource(ResourceType.FOOD, BuildingType.MARKET.cost(ResourceType.FOOD));
		colony.setResource(ResourceType.ORE, 0);
		colony.setResource(ResourceType.CHITIN, BuildingType.MARKET.cost(ResourceType.CHITIN));

		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(entry -> entry.reason().equals("construction " + BuildingType.MARKET.id()))
				.filter(entry -> entry.resource() == ResourceType.ORE)
				.findFirst()
				.orElse(null);
		if (contract == null) {
			helper.fail("Missing queued market ore should open a construction contract.");
		}

		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
		if (!result.success() || !result.complete()) {
			helper.fail("Player ore delivery should complete the construction contract.");
		}
		if (colony.resource(ResourceType.ORE) < BuildingType.MARKET.cost(ResourceType.ORE)) {
			helper.fail("Construction contract delivery should place ore into colony stores for the queued build.");
		}
		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding active = colony.progress().firstIncomplete().orElse(null);
		if (active == null || active.type() != BuildingType.MARKET || active.visualStage() != BuildingVisualStage.PLANNED) {
			helper.fail("Completed construction contract should start a visible planned market site, got "
					+ (active == null ? "none" : active.type().id() + " " + active.visualStage().id()));
		}
		if (colony.progress().buildQueueView().contains(BuildingType.MARKET) || !colony.progress().requestsView().isEmpty()) {
			helper.fail("Started market site should consume the queue entry and clear the fulfilled construction request.");
		}
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		helper.assertBlockPresent(Blocks.OAK_FENCE, market.offset(-3, 1, -7));
		helper.assertBlockPresent(Blocks.DIRT_PATH, market.offset(0, 0, -8));
		helper.succeed();
	}

	@GameTest
	public void repairSceneExposesDamagedRepairingAndRestoredBuildings(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.REPAIR_SCENE)) {
			helper.fail("Visual QA should expose a repair_scene.");
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		VisualQaScenes.seedRepairScene(helper.getLevel(), colony);
		List<BlockPos> centers = VisualQaScenes.repairSceneBuildingCenters(origin);
		BlockPos damaged = centers.get(0);
		BlockPos repairing = centers.get(1);
		BlockPos restored = centers.get(2);

		if (!helper.getLevel().getBlockState(helper.absolutePos(damaged.offset(-7, 4, -7))).isAir()) {
			helper.fail("Repair scene should show a damaged shell hole.");
		}
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, damaged.offset(1, 1, -8));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, repairing.offset(-1, 1, -4));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, repairing.offset(4, 0, -5));
		helper.assertBlockPresent(ModBlocks.MARKET_CHAMBER, restored);
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, restored.offset(-2, 1, -8));

		BlockPos absoluteOrigin = helper.absolutePos(origin);
		AABB sceneBounds = new AABB(
				absoluteOrigin.getX() - 36, absoluteOrigin.getY() - 2, absoluteOrigin.getZ() - 44,
				absoluteOrigin.getX() + 36, absoluteOrigin.getY() + 10, absoluteOrigin.getZ() - 8
		);
		List<AntEntity> repairAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, sceneBounds, ant -> ant.colonyId() == colony.id());
		if (repairAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| repairAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_CHITIN)) {
			helper.fail("Repair scene should show a working repair ant and a chitin carrier.");
		}
		helper.succeed();
	}

	@GameTest
	public void idleMatureColonyStartsAndCompletesVisibleUpgrade(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 500);
		}
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding building = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		colony.progress().addBuilding(building);
		StructurePlacer.placeBuilding(helper.getLevel(), building.pos(), building.type(), building.visualStage(), colony.progress().culture());

		ColonyBuilder.tick(helper.getLevel(), colony);
		if (building.level() != 2 || building.constructionProgress() != 0 || building.visualStage() != BuildingVisualStage.CONSTRUCTION) {
			helper.fail("Idle mature colony should begin a visible level 2 upgrade, got level "
					+ building.level() + " progress " + building.constructionProgress()
					+ " stage " + building.visualStage().id() + " task " + colony.currentTask()
					+ " queue " + colony.progress().buildQueueView());
		}
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, market.offset(-7, 1, -7));
		for (int i = 0; i < 5; i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (building.visualStage() != BuildingVisualStage.UPGRADED) {
			helper.fail("Upgrade construction should complete into the upgraded visual stage.");
		}
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, market.above(6));
		helper.succeed();
	}

	@GameTest
	public void queenBroodRecurringEventLeavesVisibleNurseryProof(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		colony.setResource(ResourceType.FOOD, 260);
		colony.setResource(ResourceType.CHITIN, 80);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		int workers = colony.casteCount(AntCaste.WORKER);
		int food = colony.resource(ResourceType.FOOD);
		int chitin = colony.resource(ResourceType.CHITIN);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Ready mature colony should trigger or reschedule a recurring event.");
		}
		if (!colony.currentTask().contains("queen brood bloom")) {
			helper.fail("Recurring event should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("queen brood bloom"))) {
			helper.fail("Recurring event should be visible in the colony event log.");
		}
		if (colony.casteCount(AntCaste.WORKER) != workers + 2) {
			helper.fail("Queen brood bloom should add two workers.");
		}
		if (colony.resource(ResourceType.FOOD) >= food || colony.resource(ResourceType.CHITIN) >= chitin) {
			helper.fail("Queen brood bloom should consume food and chitin stores.");
		}
		BlockPos nursery = ColonyBuilder.siteFor(origin, BuildingType.NURSERY, 0);
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, nursery.offset(0, 1, -2));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, nursery.offset(-1, 1, -1));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, nursery.offset(0, 2, -1));
		helper.assertBlockPresent(Blocks.DIRT_PATH, nursery.offset(0, 0, -9));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, nursery.offset(-2, 1, 1));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, nursery.offset(-7, 1, 6));
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Recurring event should not fire again until another interval passes.");
		}
		helper.succeed();
	}

	@GameTest
	public void famineRecurringEventMarksFoodStoreAndOpensContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().requests().clear();
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		colony.setResource(ResourceType.FOOD, 4);
		int foodBefore = colony.resource(ResourceType.FOOD);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Low-food mature colony should trigger a famine warning recurring event.");
		}
		if (!colony.currentTask().contains("famine warning")) {
			helper.fail("Famine event should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("famine warning"))) {
			helper.fail("Famine event should be visible in the colony event log.");
		}
		if (colony.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.FOOD_STORE
						&& request.resource() == ResourceType.FOOD
						&& request.reason().equals(ColonyRecurringEvents.FAMINE_REASON))) {
			helper.fail("Famine event should open a food-store player help contract.");
		}
		if (colony.resource(ResourceType.FOOD) != foodBefore) {
			helper.fail("Famine warning should not consume the colony's last food.");
		}

		BlockPos food = ColonyBuilder.siteFor(origin, BuildingType.FOOD_STORE, 0);
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, food.offset(0, 1, -2));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, food.offset(1, 1, -1));
		helper.assertBlockPresent(Blocks.RED_MUSHROOM_BLOCK, food.offset(-2, 1, 0));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, food.offset(0, 2, -1));
		helper.assertBlockPresent(Blocks.DIRT_PATH, food.offset(0, 0, -9));
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, food.offset(-2, 1, 1));
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, food.offset(-7, 1, 7));
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Open famine request should prevent duplicate famine warnings.");
		}
		helper.succeed();
	}

	@GameTest
	public void migrationRecurringEventMarksDaughterNestTrail(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 64);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		BlockPos market = ColonyBuilder.siteFor(origin, BuildingType.MARKET, 0);
		ColonyBuilding building = ColonyBuilding.complete(BuildingType.MARKET, helper.absolutePos(market));
		colony.progress().addBuilding(building);
		StructurePlacer.placeBuilding(helper.getLevel(), building.pos(), building.type(), building.visualStage(), colony.progress().culture());
		colony.setResource(ResourceType.FOOD, 240);
		colony.setResource(ResourceType.CHITIN, 90);
		colony.addCaste(AntCaste.WORKER, 26);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Overcrowded mature colony should trigger a migration preparation recurring event.");
		}
		if (!colony.currentTask().contains("migration preparation")) {
			helper.fail("Migration event should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("migration preparation"))) {
			helper.fail("Migration event should be visible in the colony event log.");
		}
		if (colony.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.MARKET
						&& request.resource() == ResourceType.FOOD
						&& request.reason().equals(ColonyRecurringEvents.MIGRATION_REASON))) {
			helper.fail("Migration event should open a market food contract for trail supplies.");
		}

		BlockPos camp = origin.offset(-34, 0, -30);
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-10, 0, -10));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, origin.offset(-14, 1, -14));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, camp.above());
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, camp.above(2));
		helper.assertBlockPresent(Blocks.ROOTED_DIRT, camp.north());
		helper.assertBlockPresent(Blocks.PACKED_MUD, camp.south());

		BlockPos absoluteCamp = helper.absolutePos(camp);
		AABB campBounds = new AABB(
				absoluteCamp.getX() - 4, absoluteCamp.getY(), absoluteCamp.getZ() - 4,
				absoluteCamp.getX() + 5, absoluteCamp.getY() + 6, absoluteCamp.getZ() + 5
		);
		List<AntEntity> scouts = helper.getLevel().getEntitiesOfClass(AntEntity.class, campBounds, ant ->
				ant.colonyId() == colony.id()
						&& ant.caste() == AntCaste.SCOUT
						&& ant.workState() == AntWorkState.PATROLLING);
		if (scouts.size() != 3) {
			helper.fail("Migration camp should show three patrolling scout ants, got " + scouts.size());
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Open migration request should prevent duplicate migration preparation events.");
		}
		helper.succeed();
	}

	@GameTest
	public void invasionWarningRecurringEventMarksApproachAndOpensDefenseContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 112);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData rival = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), false);
		allied.progress().requests().clear();
		allied.progress().setRelation(rival.id(), DiplomacyState.RIVAL);
		rival.progress().setRelation(allied.id(), DiplomacyState.RIVAL);
		rival.progress().setRaidCooldown(120);
		rival.addCaste(AntCaste.SOLDIER, 2);
		allied.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Rival raid window should trigger an invasion warning recurring event.");
		}
		if (!allied.currentTask().contains("invasion warning")) {
			helper.fail("Invasion warning should be visible in the current task, got " + allied.currentTask());
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("invasion warning"))) {
			helper.fail("Invasion warning should be visible in the colony event log.");
		}
		if (allied.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.BARRACKS
						&& request.resource() == ResourceType.CHITIN
						&& request.reason().equals(ColonyRecurringEvents.INVASION_WARNING_REASON))) {
			helper.fail("Invasion warning should open a barracks chitin defense contract.");
		}

		BlockPos rally = origin.offset(36, 0, 0);
		assertBlockInColumn(helper, rally, Blocks.RED_TERRACOTTA, 1, 3, "invasion warning signal");
		assertBlockInColumn(helper, rally, Blocks.BLACKSTONE, 2, 4, "invasion warning cap");
		assertBlockInColumn(helper, rally, Blocks.CANDLE, 3, 5, "invasion warning candle");
		helper.assertBlockPresent(Blocks.BONE_BLOCK, rally.south());
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, rally.offset(0, 0, 2));
		helper.assertBlockPresent(Blocks.PODZOL, rally.offset(3, 0, 0));
		helper.assertBlockPresent(Blocks.RED_TERRACOTTA, rally.offset(3, 1, 0));

		AABB guardBounds = new AABB(
				helper.absolutePos(rally).getX() - 5, helper.absolutePos(rally).getY(), helper.absolutePos(rally).getZ() - 5,
				helper.absolutePos(rally).getX() + 5, helper.absolutePos(rally).getY() + 6, helper.absolutePos(rally).getZ() + 5
		);
		List<AntEntity> guardAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, guardBounds, ant ->
				ant.colonyId() == allied.id()
						&& ant.caste() == AntCaste.SOLDIER
						&& ant.workState() == AntWorkState.PATROLLING);
		if (guardAnts.size() != 3) {
			helper.fail("Invasion warning should place three allied guard ants at the approach marker, got " + guardAnts.size());
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Open invasion defense request should prevent duplicate invasion warnings.");
		}
		helper.succeed();
	}

	@GameTest
	public void treatyOpportunityRecurringEventMarksEnvoyRouteAndOpensResinContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 112);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData allied = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		ColonyData neutral = ColonyService.createWildColony(helper.getLevel(), helper.absolutePos(origin.offset(64, 0, 0)), ColonyCulture.LEAFCUTTER);
		BlockPos shrine = ColonyBuilder.siteFor(allied, BuildingType.DIPLOMACY_SHRINE);
		allied.progress().addBuilding(ColonyBuilding.complete(BuildingType.DIPLOMACY_SHRINE, shrine));
		StructurePlacer.placeBuilding(helper.getLevel(), shrine, BuildingType.DIPLOMACY_SHRINE, BuildingVisualStage.COMPLETE, allied.progress().culture());
		allied.progress().requests().clear();
		allied.progress().setRelation(neutral.id(), DiplomacyState.NEUTRAL);
		neutral.progress().setRelation(allied.id(), DiplomacyState.NEUTRAL);
		allied.setResource(ResourceType.FOOD, 260);
		allied.setResource(ResourceType.CHITIN, 4);
		allied.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Neutral neighbor should trigger a treaty opportunity recurring event.");
		}
		if (!allied.currentTask().contains("treaty opportunity")) {
			helper.fail("Treaty opportunity should be visible in the current task, got " + allied.currentTask());
		}
		if (allied.progress().eventsView().stream().noneMatch(event -> event.message().contains("treaty opportunity"))) {
			helper.fail("Treaty opportunity should be visible in the allied colony event log.");
		}
		if (neutral.progress().eventsView().stream().noneMatch(event -> event.message().contains("treaty opportunity"))) {
			helper.fail("Treaty opportunity should leave evidence on the neutral colony too.");
		}
		if (allied.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.DIPLOMACY_SHRINE
						&& request.resource() == ResourceType.RESIN
						&& request.reason().equals(ColonyRecurringEvents.TREATY_OPPORTUNITY_REASON))) {
			helper.fail("Treaty opportunity should open a diplomacy-shrine resin contract.");
		}
		if (ColonyLogistics.contracts(allied).stream().noneMatch(contract ->
				contract.reason().equals(ColonyRecurringEvents.TREATY_OPPORTUNITY_REASON) && contract.priority() >= 4)) {
			helper.fail("Treaty opportunity contract should be prioritized for the player.");
		}

		BlockPos camp = ColonyRecurringEvents.treatyOpportunityCamp(origin, origin.offset(64, 0, 0));
		assertBlockInColumn(helper, camp, Blocks.HONEYCOMB_BLOCK, 1, 3, "treaty opportunity envoy cache");
		assertBlockInColumn(helper, camp, Blocks.AMETHYST_BLOCK, 2, 4, "treaty opportunity signal gem");
		assertBlockInColumn(helper, camp, Blocks.CANDLE, 3, 5, "treaty opportunity candle");
		helper.assertBlockPresent(Blocks.MOSS_BLOCK, camp.north());
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(8, 0, 0));

		AABB campBounds = new AABB(
				helper.absolutePos(camp).getX() - 5, helper.absolutePos(camp).getY(), helper.absolutePos(camp).getZ() - 5,
				helper.absolutePos(camp).getX() + 5, helper.absolutePos(camp).getY() + 6, helper.absolutePos(camp).getZ() + 5
		);
		List<AntEntity> envoys = helper.getLevel().getEntitiesOfClass(AntEntity.class, campBounds, ant ->
				ant.workState() == AntWorkState.CARRYING_RESIN || ant.workState() == AntWorkState.CARRYING_FUNGUS);
		if (envoys.size() != 2) {
			helper.fail("Treaty opportunity should show two visible envoy ants carrying supplies, got " + envoys.size());
		}
		if (envoys.stream().noneMatch(ant -> ant.colonyId() == allied.id() && ant.caste() == AntCaste.SCOUT && ant.workState() == AntWorkState.CARRYING_RESIN)
				|| envoys.stream().noneMatch(ant -> ant.colonyId() == neutral.id() && ant.caste() == AntCaste.WORKER && ant.workState() == AntWorkState.CARRYING_FUNGUS)) {
			helper.fail("Treaty opportunity should pair an allied resin scout with a neutral fungus worker.");
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), allied)) {
			helper.fail("Open treaty opportunity request should prevent duplicate treaty events.");
		}
		helper.succeed();
	}

	@GameTest
	public void expansionOpportunityRecurringEventMarksClaimEdgeOutpostAndOpensOreContract(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 96);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		colony.progress().buildQueue().clear();
		colony.progress().requests().clear();
		for (BuildingType type : List.of(BuildingType.MARKET, BuildingType.WATCH_POST)) {
			BlockPos site = ColonyBuilder.siteFor(colony, type);
			colony.progress().addBuilding(ColonyBuilding.complete(type, site));
			StructurePlacer.placeBuilding(helper.getLevel(), site, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
		}
		colony.setResource(ResourceType.FOOD, 160);
		colony.setResource(ResourceType.CHITIN, 4);
		colony.setResource(ResourceType.ORE, 0);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Mature watched colony should trigger an expansion opportunity recurring event.");
		}
		if (!colony.currentTask().contains("expansion opportunity")) {
			helper.fail("Expansion opportunity should be visible in the current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("expansion opportunity"))) {
			helper.fail("Expansion opportunity should be visible in the colony event log.");
		}
		if (colony.progress().requestsView().stream().noneMatch(request ->
				request.building() == BuildingType.WATCH_POST
						&& request.resource() == ResourceType.ORE
						&& request.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON))) {
			helper.fail("Expansion opportunity should open a watch-post ore contract.");
		}
		if (ColonyLogistics.contracts(colony).stream().noneMatch(contract ->
				contract.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON) && contract.priority() >= 4)) {
			helper.fail("Expansion opportunity contract should be prioritized for the player.");
		}

		BlockPos outpost = ColonyRecurringEvents.expansionOutpost(origin, ColonyRank.HIVE.claimRadius());
		assertBlockInColumn(helper, outpost, ModBlocks.WATCH_POST, 1, 2, "expansion outpost post");
		assertBlockInColumn(helper, outpost, Blocks.COBBLED_DEEPSLATE_WALL, 2, 3, "expansion outpost brace");
		assertBlockInColumn(helper, outpost, Blocks.OCHRE_FROGLIGHT, 3, 4, "expansion outpost signal");
		if (!isLandmarkTrail(helper.getLevel().getBlockState(helper.absolutePos(origin.offset(8, 0, 8))).getBlock())) {
			helper.fail("Expansion opportunity should keep a readable trail out of the enlarged colony.");
		}
		helper.assertBlockPresent(Blocks.IRON_ORE, outpost.offset(-2, 1, -5));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, outpost.offset(2, 1, -5));
		helper.assertBlockPresent(Blocks.OAK_FENCE, outpost.offset(4, 1, 0));

		BlockPos absoluteOutpost = helper.absolutePos(outpost);
		AABB crewBounds = new AABB(
				absoluteOutpost.getX() - 7, absoluteOutpost.getY(), absoluteOutpost.getZ() - 7,
				absoluteOutpost.getX() + 7, absoluteOutpost.getY() + 8, absoluteOutpost.getZ() + 7
		);
		List<AntEntity> crew = helper.getLevel().getEntitiesOfClass(AntEntity.class, crewBounds, ant ->
				ant.colonyId() == colony.id()
						&& (ant.workState() == AntWorkState.CARRYING_ORE
						|| ant.workState() == AntWorkState.CARRYING_CHITIN
						|| ant.workState() == AntWorkState.PATROLLING));
		if (crew.size() != 3) {
			helper.fail("Expansion opportunity should place three visible outpost crew ants, got " + crew.size());
		}
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Open expansion outpost request should prevent duplicate expansion opportunities.");
		}
		helper.succeed();
	}

	@GameTest
	public void completedExpansionOutpostContractSecuresClaimEdgeWatchPost(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 96);
		ColonySavedState savedState = ColonySavedState.get(helper.getLevel().getServer());
		savedState.clearColonies();
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin), true);
		colony.progress().buildQueue().clear();
		colony.progress().requests().clear();
		for (BuildingType type : List.of(BuildingType.MARKET, BuildingType.WATCH_POST)) {
			BlockPos site = ColonyBuilder.siteFor(colony, type);
			colony.progress().addBuilding(ColonyBuilding.complete(type, site));
			StructurePlacer.placeBuilding(helper.getLevel(), site, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
		}
		colony.setResource(ResourceType.FOOD, 160);
		colony.setResource(ResourceType.CHITIN, 4);
		colony.setResource(ResourceType.ORE, 0);
		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		int oldClaim = colony.progress().claimRadius();

		if (!ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Mature watched colony should open an expansion outpost contract before payoff.");
		}
		ColonyContract contract = ColonyLogistics.contracts(colony).stream()
				.filter(candidate -> candidate.reason().equals(ColonyRecurringEvents.EXPANSION_OPPORTUNITY_REASON))
				.findFirst()
				.orElse(null);
		if (contract == null) {
			helper.fail("Expansion payoff needs the expansion outpost contract to exist.");
		}
		ColonyLogistics.ContractDeliveryResult result = ColonyLogistics.fulfillContract(colony, contract.id(), contract.missing());
		if (!result.success() || !result.complete()) {
			helper.fail("Expansion outpost contract should complete from a full player delivery.");
		}
		if (!ColonyRecurringEvents.completeExpansionOutpost(helper.getLevel(), colony)) {
			helper.fail("Completed expansion outpost contract should secure the claim edge.");
		}

		BlockPos outpost = ColonyRecurringEvents.expansionOutpost(origin, ColonyRank.HIVE.claimRadius());
		helper.assertBlockPresent(ModBlocks.WATCH_POST, outpost);
		helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE_WALL, outpost.above());
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, outpost.above(2));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, outpost.offset(6, 1, 0));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, outpost.offset(5, 1, 5));

		BlockPos absoluteOutpost = helper.absolutePos(outpost);
		if (colony.progress().buildingsView().stream().noneMatch(building ->
				building.type() == BuildingType.WATCH_POST
						&& building.complete()
						&& building.pos().equals(absoluteOutpost))) {
			helper.fail("Secured expansion outpost should be registered as a completed watch post.");
		}
		if (colony.progress().claimRadius() <= oldClaim || colony.progress().claimRadius() < 44) {
			helper.fail("Secured expansion outpost should expand claim radius beyond " + oldClaim + ", got " + colony.progress().claimRadius());
		}
		if (!colony.currentTask().contains("Expansion outpost secured")) {
			helper.fail("Expansion payoff should be visible in current task, got " + colony.currentTask());
		}
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("Expansion complete"))) {
			helper.fail("Expansion payoff should leave an event-log entry.");
		}
		if (!colony.progress().requestsView().isEmpty()) {
			helper.fail("Completed expansion outpost contract should clear the open request.");
		}

		colony.addAgeTicks(ColonyRecurringEvents.EVENT_INTERVAL_TICKS);
		if (ColonyRecurringEvents.tick(helper.getLevel(), colony)) {
			helper.fail("Secured expansion outpost should prevent reopening the same expansion opportunity.");
		}
		helper.succeed();
	}

	@GameTest
	public void citadelColonyCompletesVisibleGreatMoundProject(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 72);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 800);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 12);
		colony.addCaste(AntCaste.SOLDIER, 5);
		for (BuildingType type : List.of(BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE, BuildingType.ARMORY, BuildingType.DIPLOMACY_SHRINE)) {
			if (!colony.progress().hasCompleted(type)) {
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(helper.getLevel(), pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}

		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding greatMound = colony.progress().buildings().stream()
				.filter(building -> building.type() == BuildingType.GREAT_MOUND)
				.findFirst()
				.orElse(null);
		if (greatMound == null) {
			helper.fail("Citadel colony should start the great mound endgame project.");
		}
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.ENDGAME_PROJECT)) {
			helper.fail("Visual QA should expose an endgame_project scene.");
		}

		for (int i = 0; i < 4 && !greatMound.complete(); i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (!greatMound.complete()) {
			helper.fail("Great mound should complete from the prepared endgame resources, got " + greatMound.constructionProgress() + "%.");
		}
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.above(7));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, origin.above(8));
		helper.assertBlockPresent(ModBlocks.PHEROMONE_ARCHIVE, origin.offset(0, 1, -16));
		helper.assertBlockPresent(Blocks.CHISELED_TUFF, origin.offset(16, 0, 0));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-12, 0, 0));
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("great_mound"))) {
			helper.fail("Great mound project should leave a colony event for the player.");
		}
		helper.succeed();
	}

	@GameTest
	public void citadelColonyCompletesVisibleQueenVaultAfterGreatMound(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 76);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 900);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 12);
		colony.addCaste(AntCaste.SOLDIER, 5);
		for (BuildingType type : List.of(BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE, BuildingType.ARMORY, BuildingType.DIPLOMACY_SHRINE, BuildingType.GREAT_MOUND)) {
			if (!colony.progress().hasCompleted(type)) {
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(helper.getLevel(), pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}

		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding vault = colony.progress().buildings().stream()
				.filter(building -> building.type() == BuildingType.QUEEN_VAULT)
				.findFirst()
				.orElse(null);
		if (vault == null) {
			helper.fail("Citadel colony should start the queen vault after the Great Mound.");
		}

		for (int i = 0; i < 4 && !vault.complete(); i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (!vault.complete()) {
			helper.fail("Queen vault should complete from prepared endgame resources, got " + vault.constructionProgress() + "%.");
		}
		helper.assertBlockPresent(ModBlocks.NEST_CORE, origin.below(2));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.below());
		helper.assertBlockPresent(Blocks.CHISELED_TUFF, origin.offset(0, 0, -12));
		helper.assertBlockPresent(Blocks.CANDLE, origin.offset(0, 1, -11));
		helper.assertBlockPresent(Blocks.HONEYCOMB_BLOCK, origin.offset(1, 0, -12));
		helper.assertBlockPresent(Blocks.CHISELED_TUFF, origin.offset(12, 0, 0));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(1, 2, -12));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(-1, 2, -12));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, origin.offset(1, 1, -13));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(12, 2, 1));
		helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, origin.offset(-1, 2, 12));
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("queen_vault"))) {
			helper.fail("Queen vault project should leave a colony event for the player.");
		}
		helper.succeed();
	}

	@GameTest
	public void citadelColonyCompletesVisibleTradeHubAfterQueenVault(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin, 90);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		colony.progress().buildQueue().clear();
		for (ResourceType resource : ResourceType.values()) {
			colony.setResource(resource, 1000);
		}
		colony.progress().addReputation(100);
		colony.addCaste(AntCaste.WORKER, 12);
		colony.addCaste(AntCaste.SOLDIER, 5);
		for (BuildingType type : List.of(BuildingType.CHITIN_FARM, BuildingType.MARKET, BuildingType.PHEROMONE_ARCHIVE, BuildingType.ARMORY, BuildingType.DIPLOMACY_SHRINE, BuildingType.GREAT_MOUND, BuildingType.QUEEN_VAULT)) {
			if (!colony.progress().hasCompleted(type)) {
				BlockPos pos = ColonyBuilder.siteFor(colony, type);
				colony.progress().addBuilding(ColonyBuilding.complete(type, pos));
				StructurePlacer.placeBuilding(helper.getLevel(), pos, type, BuildingVisualStage.COMPLETE, colony.progress().culture());
			}
		}

		ColonyBuilder.tick(helper.getLevel(), colony);
		ColonyBuilding tradeHub = colony.progress().buildings().stream()
				.filter(building -> building.type() == BuildingType.TRADE_HUB)
				.findFirst()
				.orElse(null);
		if (tradeHub == null) {
			helper.fail("Citadel colony should start the trade hub after the Queen Vault.");
		}

		for (int i = 0; i < 4 && !tradeHub.complete(); i++) {
			ColonyBuilder.tick(helper.getLevel(), colony);
		}
		if (!tradeHub.complete()) {
			helper.fail("Trade hub should complete from prepared endgame resources, got " + tradeHub.constructionProgress() + "%.");
		}

		BlockPos hub = ColonyBuilder.siteFor(origin, BuildingType.TRADE_HUB, 0);
		helper.assertBlockPresent(ModBlocks.MARKET_CHAMBER, hub);
		helper.assertBlockPresent(ModBlocks.COLONY_LEDGER, hub.offset(0, 1, 1));
		helper.assertBlockPresent(Blocks.BELL, hub.offset(0, 1, -1));
		helper.assertBlockPresent(Blocks.GOLD_BLOCK, hub.offset(-1, 1, 0));
		helper.assertBlockPresent(Blocks.BARREL, hub.offset(1, 1, 0));
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, hub.offset(-8, 0, -5));
		helper.assertBlockPresent(ModBlocks.ORE_NODE, hub.offset(8, 0, -5));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, hub.offset(-8, 0, 5));
		helper.assertBlockPresent(ModBlocks.RESIN_DEPOT, hub.offset(8, 0, 5));
		helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, hub.above(4));
		helper.assertBlockPresent(Blocks.DIRT_PATH, hub.offset(-22, 0, 14));
		if (colony.progress().eventsView().stream().noneMatch(event -> event.message().contains("trade_hub"))) {
			helper.fail("Trade hub project should leave a colony event for the player.");
		}
		helper.succeed();
	}

	@GameTest
	public void completedTradeHubImprovesTradeTerms(GameTestHelper helper) {
		ColonyData colony = new ColonyData(99, helper.absolutePos(new BlockPos(2, 3, 2)));
		ColonyTradeCatalog.Offer sellWheat = tradeOffer("sell_wheat");
		ColonyTradeCatalog.Offer buySeal = tradeOffer("buy_colony_seal");

		int starterTokenReward = ColonyTradeCatalog.outputCount(colony, sellWheat);
		int starterSealCost = ColonyTradeCatalog.inputCount(colony, buySeal);

		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, ColonyBuilder.siteFor(colony, BuildingType.TRADE_HUB)));
		colony.progress().addReputation(20);

		int hubTokenReward = ColonyTradeCatalog.outputCount(colony, sellWheat);
		int hubSealCost = ColonyTradeCatalog.inputCount(colony, buySeal);
		if (hubTokenReward != starterTokenReward + 1) {
			helper.fail("Trade Hub should add one pheromone token to normal sell offers.");
		}
		if (hubSealCost != 14 || hubSealCost >= starterSealCost) {
			helper.fail("Trade Hub should discount token purchases from 16 to 14, got " + hubSealCost + ".");
		}
		if (!ColonyTradeCatalog.availabilityText(colony, sellWheat).contains("Trade Hub")
				|| !ColonyTradeCatalog.availabilityText(colony, buySeal).contains("Trade Hub")) {
			helper.fail("Trade Hub terms should be visible in trade row status text.");
		}
		helper.succeed();
	}

	@GameTest
	public void tabletTradeSceneShowsTradeHubTerms(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.TABLET_TRADE)) {
			helper.fail("Visual QA should expose a tablet_trade scene.");
		}
		ColonyData colony = new ColonyData(99, helper.absolutePos(new BlockPos(2, 3, 2)));
		colony.progress().addReputation(20);
		colony.progress().addBuilding(ColonyBuilding.complete(BuildingType.TRADE_HUB, ColonyBuilder.siteFor(colony, BuildingType.TRADE_HUB)));

		ColonyUiSnapshot snapshot = ColonyUiSnapshot.from(colony, "Trade", "");
		if (!snapshot.initialTab().equals("Trade")) {
			helper.fail("Tablet trade scene should open the Trade tab.");
		}
		ColonyUiSnapshot.TradeEntry sellWheat = tradeEntry(snapshot, "sell_wheat");
		ColonyUiSnapshot.TradeEntry buySeal = tradeEntry(snapshot, "buy_colony_seal");
		if (sellWheat == null || sellWheat.outputCount() != 2 || !sellWheat.status().contains("Trade Hub")) {
			helper.fail("Trade Hub tablet should show sell_wheat as a +1 token offer.");
		}
		if (buySeal == null || buySeal.inputCount() != 14 || !buySeal.available() || !buySeal.status().contains("Trade Hub")) {
			helper.fail("Trade Hub tablet should show buy_colony_seal as an available discounted token purchase.");
		}
		colony.addEvent("Recurring event: trade caravan exchanged 8 food for 8 resin with colony #7");
		ColonyUiSnapshot payoffSnapshot = ColonyUiSnapshot.from(colony, "Trade", "");
		if (!payoffSnapshot.tradeActivity().contains("8 Food -> 8 Resin with #7")) {
			helper.fail("Tablet trade scene should expose the latest caravan payoff line, got " + payoffSnapshot.tradeActivity());
		}
		helper.succeed();
	}

	@GameTest
	public void workCycleSceneAndWorkStatesAreAvailable(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.WORK_CYCLE)) {
			helper.fail("Visual QA should expose a work_cycle scene.");
		}
		List<BlockPos> jobCenters = VisualQaScenes.workCycleJobCenters(helper.absolutePos(new BlockPos(2, 3, 2)));
		if (jobCenters.size() != 4) {
			helper.fail("Work-cycle scene should expose build, ore, patrol, and food logistics job centers.");
		}
		BlockPos focalPoint = helper.absolutePos(new BlockPos(2, 3, 2).offset(0, 0, -23));
		for (BlockPos jobCenter : jobCenters) {
			if (Math.abs(jobCenter.getX() - focalPoint.getX()) > 10 || Math.abs(jobCenter.getZ() - focalPoint.getZ()) > 4) {
				helper.fail("Work-cycle jobs should stay clustered in the central camera field.");
			}
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyData colony = ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));

		VisualQaScenes.seedWorkCycle(helper.getLevel(), colony);
		AABB demoBounds = VisualQaScenes.workCycleActorCleanupBounds(colony.origin());
		List<AntEntity> demoAnts = helper.getLevel().getEntitiesOfClass(AntEntity.class, demoBounds, ant -> ant.colonyId() == colony.id());
		if (demoAnts.size() != 4) {
			helper.fail("Work-cycle scene should keep only the four purpose-built demo ants in frame, got " + demoAnts.size());
		}
		if (demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.WORKING)
				|| demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_ORE)
				|| demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.CARRYING_FOOD)
				|| demoAnts.stream().noneMatch(ant -> ant.workState() == AntWorkState.PATROLLING)) {
			helper.fail("Work-cycle ants should hold visible build, haul, forage, and patrol states.");
		}
		if (!helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, demoBounds).isEmpty()) {
			helper.fail("Work-cycle scene should remove colony text labels so job markers dominate.");
		}
		helper.assertBlockPresent(Blocks.MANGROVE_ROOTS, origin.offset(-8, 0, -29));
		helper.assertBlockPresent(Blocks.DIRT_PATH, origin.offset(-8, 0, -28));
		helper.assertBlockPresent(Blocks.IRON_ORE, origin.offset(0, 0, -26));
		helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE, origin.offset(0, 0, -24));
		helper.assertBlockPresent(Blocks.POLISHED_DEEPSLATE, origin.offset(9, 0, -29));
		helper.assertBlockPresent(Blocks.BONE_BLOCK, origin.offset(9, 0, -27));
		helper.assertBlockPresent(Blocks.HAY_BLOCK, origin.offset(2, 0, -19));
		helper.assertBlockPresent(Blocks.COMPOSTER, origin.offset(0, 1, -18));
		helper.assertBlockPresent(Blocks.BARREL, origin.offset(4, 1, -20));
		helper.assertBlockPresent(Blocks.OAK_FENCE, origin.offset(2, 1, -17));
		helper.succeed();
	}

	@GameTest
	public void antLineupKeepsSmallCastesNearCameraFocus(GameTestHelper helper) {
		if (!VisualQaScenes.scenes().contains(VisualQaScenes.ANT_LINEUP)) {
			helper.fail("Visual QA should expose an ant_lineup scene.");
		}
		BlockPos origin = new BlockPos(2, 3, 2);
		for (AntCaste caste : List.of(AntCaste.WORKER, AntCaste.SCOUT, AntCaste.MINER)) {
			BlockPos pos = VisualQaScenes.antLineupPosition(origin, caste);
			if (Math.abs(pos.getX() - origin.getX()) > 10 || pos.getZ() > origin.getZ() - 43) {
				helper.fail("Small caste " + caste.id() + " should stay near the center-front of the lineup, got " + pos.toShortString());
			}
		}
		for (AntCaste first : AntCaste.values()) {
			BlockPos firstPos = VisualQaScenes.antLineupPosition(origin, first);
			if (Math.abs(firstPos.getX() - origin.getX()) > 15) {
				helper.fail("Ant lineup caste " + first.id() + " should stay inside the widened camera frame.");
			}
			for (AntCaste second : AntCaste.values()) {
				if (first.ordinal() >= second.ordinal()) {
					continue;
				}
				BlockPos secondPos = VisualQaScenes.antLineupPosition(origin, second);
				int dx = firstPos.getX() - secondPos.getX();
				int dz = firstPos.getZ() - secondPos.getZ();
				if (dx * dx + dz * dz < 25) {
					helper.fail("Ant lineup labels need at least five blocks of horizontal separation between "
							+ first.id() + " and " + second.id() + ".");
				}
			}
		}
		helper.succeed();
	}

	private static void assertStarterQueueStarts(GameTestHelper helper, ColonyData colony, BuildingType expected) {
		if (colony.progress().buildQueueView().isEmpty() || colony.progress().buildQueueView().getFirst() != expected) {
			helper.fail("Expected " + colony.progress().culture().id() + " starter queue to begin with " + expected.id()
					+ ", got " + colony.progress().buildQueueView());
		}
	}

	private static ColonyTradeCatalog.Offer tradeOffer(String id) {
		return ColonyTradeCatalog.offersView().stream()
				.filter(offer -> offer.id().equals(id))
				.findFirst()
				.orElseThrow();
	}

	private static ColonyUiSnapshot.TradeEntry tradeEntry(ColonyUiSnapshot snapshot, String id) {
		return snapshot.trades().stream()
				.filter(entry -> entry.offerId().equals(id))
				.findFirst()
				.orElse(null);
	}

	private static net.minecraft.world.level.block.Block landmarkMarker(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.MOSS_BLOCK;
			case FIRE -> Blocks.RED_TERRACOTTA;
			case CARPENTER -> Blocks.MANGROVE_ROOTS;
			case AMBER -> Blocks.CHISELED_TUFF;
		};
	}

	private static boolean isLandmarkTrail(net.minecraft.world.level.block.Block block) {
		return block == Blocks.DIRT_PATH || block == Blocks.COARSE_DIRT;
	}

	private static BlockPos firstApproachTrailBlock(GameTestHelper helper, BlockPos start, BlockPos end) {
		BlockPos anchoredStart = ColonyService.anchorToSurface(helper.getLevel(), start);
		BlockPos current = anchoredStart;
		int steps = 0;
		while ((current.getX() != end.getX() || current.getZ() != end.getZ()) && steps < 160) {
			if (current.getX() != end.getX()) {
				current = current.offset(Integer.compare(end.getX(), current.getX()), 0, 0);
			} else {
				current = current.offset(0, 0, Integer.compare(end.getZ(), current.getZ()));
			}
			steps++;
			if (horizontalDistanceSquared(current, anchoredStart) < 8 * 8 || horizontalDistanceSquared(current, end) < 13 * 13) {
				continue;
			}
			BlockPos ground = ColonyService.anchorToSurface(helper.getLevel(), current);
			if (isLandmarkTrail(helper.getLevel().getBlockState(ground).getBlock())) {
				return ground;
			}
		}
		return null;
	}

	private static boolean hasGroundedTrailHeadDressing(GameTestHelper helper, BlockPos trailHead, BlockPos end, ColonyCulture culture) {
		int stepX = Integer.compare(end.getX(), trailHead.getX());
		int stepZ = Integer.compare(end.getZ(), trailHead.getZ());
		BlockPos left = ColonyService.anchorToSurface(helper.getLevel(), trailHead.offset(-stepZ, 0, stepX));
		BlockPos right = ColonyService.anchorToSurface(helper.getLevel(), trailHead.offset(stepZ, 0, -stepX));
		return helper.getLevel().getBlockState(left).is(Blocks.ROOTED_DIRT)
				&& helper.getLevel().getBlockState(right).is(trailHeadDressingBlock(culture));
	}

	private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
		int dx = first.getX() - second.getX();
		int dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static net.minecraft.world.level.block.Block trailHeadDressingBlock(ColonyCulture culture) {
		return switch (culture) {
			case LEAFCUTTER -> Blocks.PODZOL;
			case FIRE -> Blocks.COARSE_DIRT;
			case CARPENTER -> Blocks.ROOTED_DIRT;
			case AMBER -> Blocks.PACKED_MUD;
		};
	}

	private static void assertBlockInColumn(GameTestHelper helper, BlockPos base, net.minecraft.world.level.block.Block block, int minOffset, int maxOffset, String label) {
		for (int y = minOffset; y <= maxOffset; y++) {
			if (helper.getLevel().getBlockState(helper.absolutePos(base.above(y))).is(block)) {
				return;
			}
		}
		helper.fail("Expected " + label + " column to contain " + block.getName().getString() + " near " + base.toShortString());
	}

	private static void prepareCampusArea(GameTestHelper helper, BlockPos origin) {
		prepareCampusArea(helper, origin, 72);
	}

	private static void prepareCampusArea(GameTestHelper helper, BlockPos origin, int radius) {
		BlockPos absolute = helper.absolutePos(origin);
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (x * x + z * z > radius * radius) {
					continue;
				}
				helper.getLevel().setBlock(absolute.offset(x, -2, z), Blocks.DIRT.defaultBlockState(), 3);
				helper.getLevel().setBlock(absolute.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
				helper.getLevel().setBlock(absolute.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 12; y++) {
					helper.getLevel().setBlock(absolute.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}
}
