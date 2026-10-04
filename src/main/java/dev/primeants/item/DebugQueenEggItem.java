package dev.primeants.item;

import dev.primeants.entity.AntEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;

/** Uses vanilla spawn-egg placement after authorization; never configures a spawner. */
public final class DebugQueenEggItem extends SpawnEggItem {
    public DebugQueenEggItem(Properties properties) { super(properties); }

    private boolean authorized(Player player) {
        if (player != null && (player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))) {
            return true;
        }
        if (player != null && !player.level().isClientSide()) {
            player.sendSystemMessage(Component.translatable("message.prime_ants.debug_egg_restricted"));
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!authorized(context.getPlayer()) || getType(context.getItemInHand()) != AntEntities.QUEEN
                || context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Spawner) {
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!authorized(player) || getType(player.getItemInHand(hand)) != AntEntities.QUEEN) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }
}
