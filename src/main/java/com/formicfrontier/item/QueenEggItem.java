package com.formicfrontier.item;

import com.formicfrontier.world.ColonyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class QueenEggItem extends Item {
	public QueenEggItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.PASS;
		}

		var colony = ColonyService.createColony(level, context.getClickedPos().above());
		if (context.getPlayer() != null) {
			context.getPlayer().displayClientMessage(Component.literal("Created Formic colony #" + colony.id()), true);
			if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
				ColonyService.openColonyScreen(serverPlayer, colony, "Overview", "Created Formic colony #" + colony.id());
			}
		}
		if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}
