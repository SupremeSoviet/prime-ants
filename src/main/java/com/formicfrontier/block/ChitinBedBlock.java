package com.formicfrontier.block;

import com.formicfrontier.registry.ModItems;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.world.ColonyService;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ChitinBedBlock extends Block {
	public static final MapCodec<ChitinBedBlock> CODEC = simpleCodec(ChitinBedBlock::new);
	public static final IntegerProperty AGE = BlockStateProperties.AGE_4;

	public ChitinBedBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AGE, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = state.getValue(AGE);
		if (age < 4 && random.nextInt(3) == 0) {
			level.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		int age = state.getValue(AGE);
		if (age < 4) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Chitin bed growth: " + age + "/4"), true);
			return InteractionResult.SUCCESS_SERVER;
		}
		int bonus = level instanceof ServerLevel serverLevel && ColonyService.nearestColony(serverLevel, pos, 96)
				.map(colony -> colony.progress().hasResearch(ResearchNode.CHITIN_CULTIVATION.id()) ? 1 : 0)
				.orElse(0) > 0 ? 1 : 0;
		Block.popResource(level, pos, new ItemStack(ModItems.CHITIN_SHARD, 1 + level.random.nextInt(3) + bonus));
		if (level.random.nextFloat() < 0.45f) {
			Block.popResource(level, pos, new ItemStack(ModItems.CHITIN_SPORE));
		}
		level.setBlockAndUpdate(pos, state.setValue(AGE, 0));
		return InteractionResult.SUCCESS_SERVER;
	}
}
