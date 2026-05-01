package com.formicfrontier.block;

import com.formicfrontier.world.ColonyService;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ColonyInteractBlock extends Block {
	public static final MapCodec<ColonyInteractBlock> CODEC = simpleCodec(ColonyInteractBlock::new);

	public ColonyInteractBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return showColonyStatus(level, pos, player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return showColonyStatus(level, pos, player);
	}

	private static InteractionResult showColonyStatus(Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}
		ColonyService.nearestColony(serverLevel, pos, 96).ifPresentOrElse(colony -> {
			if (player instanceof ServerPlayer serverPlayer) {
				ColonyService.openColonyScreen(serverPlayer, colony, initialTabFor(level.getBlockState(pos)), "");
			}
		}, () -> player.displayClientMessage(Component.literal("No Formic colony is linked to this block."), false));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static String initialTabFor(BlockState state) {
		if (state.is(com.formicfrontier.registry.ModBlocks.COLONY_LEDGER)) {
			return "Overview";
		}
		if (state.is(com.formicfrontier.registry.ModBlocks.MARKET_CHAMBER)) {
			return "Trade";
		}
		if (state.is(com.formicfrontier.registry.ModBlocks.PHEROMONE_ARCHIVE)) {
			return "Research";
		}
		if (state.is(com.formicfrontier.registry.ModBlocks.DIPLOMACY_SHRINE)) {
			return "Diplomacy";
		}
		if (state.is(com.formicfrontier.registry.ModBlocks.NEST_CORE) || state.is(com.formicfrontier.registry.ModBlocks.NEST_MOUND)) {
			return "Overview";
		}
		return "Buildings";
	}
}
