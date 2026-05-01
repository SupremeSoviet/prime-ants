package com.formicfrontier.command;

import com.formicfrontier.qa.VisualQaScenes;
import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.ColonyEconomy;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.world.ColonySavedState;
import com.formicfrontier.world.ColonyService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class FormicCommands {
	private FormicCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("formic")
						.then(Commands.literal("colony")
								.then(Commands.literal("create").executes(context -> createColony(context.getSource())))
								.then(Commands.literal("dump").executes(context -> dumpColony(context.getSource())))
								.then(Commands.literal("renovate").executes(context -> renovateColony(context.getSource())))
								.then(Commands.literal("tick")
										.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 10000))
												.executes(context -> tickColony(context.getSource(), IntegerArgumentType.getInteger(context, "ticks")))))
								.then(Commands.literal("priority")
										.then(Commands.argument("priority", StringArgumentType.word())
												.executes(context -> setPriority(context.getSource(), StringArgumentType.getString(context, "priority")))))
								.then(Commands.literal("instinct")
										.then(Commands.argument("instinct", StringArgumentType.word())
												.executes(context -> setPriority(context.getSource(), StringArgumentType.getString(context, "instinct")))))
								.then(Commands.literal("seed-rivals")
										.then(Commands.argument("count", IntegerArgumentType.integer(1, 6))
												.then(Commands.argument("distance", IntegerArgumentType.integer(32, 256))
														.executes(context -> seedRivals(
																context.getSource(),
																IntegerArgumentType.getInteger(context, "count"),
																IntegerArgumentType.getInteger(context, "distance")
														)))))
								.then(Commands.literal("resource")
										.then(Commands.literal("set")
												.then(Commands.argument("type", StringArgumentType.word())
														.then(Commands.argument("amount", IntegerArgumentType.integer(0))
																.executes(context -> setResource(
																		context.getSource(),
																		StringArgumentType.getString(context, "type"),
																		IntegerArgumentType.getInteger(context, "amount")
																)))))))
						.then(Commands.literal("ant")
								.then(Commands.literal("spawn")
										.then(Commands.argument("caste", StringArgumentType.word())
												.executes(context -> spawnAnt(context.getSource(), StringArgumentType.getString(context, "caste"))))))
						.then(Commands.literal("trade")
								.then(Commands.argument("offer", StringArgumentType.word())
										.executes(context -> trade(context.getSource(), StringArgumentType.getString(context, "offer")))))
						.then(Commands.literal("research")
								.then(Commands.argument("node", StringArgumentType.word())
										.executes(context -> research(context.getSource(), StringArgumentType.getString(context, "node")))))
						.then(Commands.literal("diplomacy")
								.then(Commands.argument("action", StringArgumentType.word())
										.executes(context -> diplomacy(context.getSource(), StringArgumentType.getString(context, "action")))))
						.then(Commands.literal("qa")
								.then(Commands.literal("scene")
										.then(Commands.argument("name", StringArgumentType.word())
												.suggests((context, builder) -> SharedSuggestionProvider.suggest(VisualQaScenes.scenes(), builder))
												.executes(context -> visualQaScene(context.getSource(), StringArgumentType.getString(context, "name"))))))
		));
	}

	private static int createColony(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		var colony = ColonyService.createColony(level, BlockPos.containing(source.getPosition()));
		source.sendSuccess(() -> Component.literal("Created Formic colony #" + colony.id()), true);
		return 1;
	}

	private static int dumpColony(CommandSourceStack source) {
		String status = ColonySavedState.get(source.getServer())
				.firstColony()
				.map(colony -> colony.statusText())
				.orElse("No Formic colony exists.");
		source.sendSuccess(() -> Component.literal(status), false);
		return 1;
	}

	private static int renovateColony(CommandSourceStack source) {
		boolean renovated = ColonyService.renovateNearestColony(source.getLevel(), BlockPos.containing(source.getPosition()));
		if (!renovated) {
			source.sendFailure(Component.literal("No nearby Formic colony to renovate."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Renovated nearest Formic colony into Queen Hall campus."), true);
		return 1;
	}

	private static int tickColony(CommandSourceStack source, int ticks) {
		ColonySavedState savedState = ColonySavedState.get(source.getServer());
		int economyTicks = Math.max(1, ticks / ColonyEconomy.ECONOMY_TICK_INTERVAL);
		for (int i = 0; i < economyTicks; i++) {
			savedState.tickEconomy();
			savedState.tickWorld(source.getLevel());
		}
		savedState.setDirty();
		source.sendSuccess(() -> Component.literal("Advanced colony economy by " + economyTicks + " economy ticks."), true);
		return economyTicks;
	}

	private static int seedRivals(CommandSourceStack source, int count, int distance) {
		ServerLevel level = source.getLevel();
		BlockPos center = BlockPos.containing(source.getPosition());
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0 * i) / count;
			BlockPos pos = center.offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
			ColonyService.createColony(level, pos, false);
		}
		source.sendSuccess(() -> Component.literal("Seeded " + count + " rival Formic colonies."), true);
		return count;
	}

	private static int setResource(CommandSourceStack source, String typeName, int amount) {
		ResourceType type = ResourceType.fromId(typeName);
		ColonySavedState savedState = ColonySavedState.get(source.getServer());
		savedState.firstColony().ifPresent(colony -> colony.setResource(type, amount));
		savedState.setDirty();
		source.sendSuccess(() -> Component.literal("Set " + type.id() + " to " + amount), true);
		return 1;
	}

	private static int spawnAnt(CommandSourceStack source, String casteName) {
		AntCaste caste = AntCaste.fromId(casteName);
		ColonyService.spawnAnt(source.getLevel(), BlockPos.containing(source.getPosition()), caste);
		source.sendSuccess(() -> Component.literal("Spawned " + caste.id() + " ant."), true);
		return 1;
	}

	private static int setPriority(CommandSourceStack source, String priority) {
		if (source.getPlayer() == null) {
			source.sendFailure(Component.literal("Priority command requires a player."));
			return 0;
		}
		return ColonyService.setTopPriority(source.getPlayer(), priority) ? 1 : 0;
	}

	private static int trade(CommandSourceStack source, String offer) {
		if (source.getPlayer() == null) {
			source.sendFailure(Component.literal("Trade command requires a player."));
			return 0;
		}
		return ColonyService.trade(source.getPlayer(), offer) ? 1 : 0;
	}

	private static int research(CommandSourceStack source, String node) {
		if (source.getPlayer() == null) {
			source.sendFailure(Component.literal("Research command requires a player."));
			return 0;
		}
		return ColonyService.startResearch(source.getPlayer(), node) ? 1 : 0;
	}

	private static int diplomacy(CommandSourceStack source, String action) {
		if (source.getPlayer() == null) {
			source.sendFailure(Component.literal("Diplomacy command requires a player."));
			return 0;
		}
		return ColonyService.performDiplomacy(source.getPlayer(), action) ? 1 : 0;
	}

	private static int visualQaScene(CommandSourceStack source, String sceneName) {
		return VisualQaScenes.run(source, sceneName);
	}
}
