package com.formicfrontier.item;

import com.formicfrontier.world.ColonySavedState;
import com.formicfrontier.world.ColonyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class ColonyTabletItem extends Item {
	public ColonyTabletItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(user instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		var colony = ColonySavedState.get(level.getServer())
				.firstColony()
				.orElse(null);
		if (colony == null) {
			serverPlayer.displayClientMessage(Component.translatable("formic_frontier.feedback.no_colony"), true);
			return InteractionResult.SUCCESS;
		}
		ColonyService.openColonyScreen(serverPlayer, colony, "Overview", Component.translatable("formic_frontier.feedback.open_tablet").getString());
		return InteractionResult.SUCCESS;
	}
}
