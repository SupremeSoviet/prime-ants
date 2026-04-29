package com.formicfrontier.test;

import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyCulture;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyLogistics;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.world.ColonyService;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

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
		helper.assertBlockPresent(ModBlocks.FOOD_CHAMBER, origin.offset(22, 0, 0));
		helper.assertBlockPresent(ModBlocks.NURSERY_CHAMBER, origin.offset(-22, 0, 0));
		helper.assertBlockPresent(ModBlocks.MINE_CHAMBER, origin.offset(0, 0, 22));
		helper.assertBlockPresent(ModBlocks.BARRACKS_CHAMBER, origin.offset(0, 0, -22));
		helper.assertBlockPresent(ModBlocks.FOOD_NODE, origin.offset(32, 0, 4));
		helper.assertBlockPresent(ModBlocks.ORE_NODE, origin.offset(4, 0, 32));
		helper.assertBlockPresent(ModBlocks.CHITIN_NODE, origin.offset(-32, 0, 4));
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
		helper.assertBlockPresent(ModBlocks.FOOD_CHAMBER, new BlockPos(24, 3, 2));
		helper.succeed();
	}

	@GameTest
	public void colonyLabelsAndImportantAntNamesAreVisible(GameTestHelper helper) {
		BlockPos origin = new BlockPos(2, 3, 2);
		prepareCampusArea(helper, origin);
		ColonyService.createColony(helper.getLevel(), helper.absolutePos(origin));
		AABB area = new AABB(
				helper.absolutePos(origin).getX() - 48, helper.absolutePos(origin).getY() - 4, helper.absolutePos(origin).getZ() - 48,
				helper.absolutePos(origin).getX() + 48, helper.absolutePos(origin).getY() + 16, helper.absolutePos(origin).getZ() + 48
		);
		boolean hasLabel = !helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, area, label -> label.getCustomName() != null).isEmpty();
		if (!hasLabel) {
			helper.fail("Colony should create visible building labels.");
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
		BlockPos oreNode = helper.absolutePos(origin.offset(4, 0, 32));

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
		ColonyBuilding planned = ColonyBuilding.planned(BuildingType.MARKET, helper.absolutePos(origin.offset(20, 0, -20)));
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

	private static void prepareCampusArea(GameTestHelper helper, BlockPos origin) {
		BlockPos absolute = helper.absolutePos(origin);
		for (int x = -48; x <= 48; x++) {
			for (int z = -48; z <= 48; z++) {
				if (x * x + z * z > 48 * 48) {
					continue;
				}
				helper.getLevel().setBlock(absolute.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				for (int y = 1; y <= 8; y++) {
					helper.getLevel().setBlock(absolute.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}
}
