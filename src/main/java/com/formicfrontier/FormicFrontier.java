package com.formicfrontier;

import com.formicfrontier.command.FormicCommands;
import com.formicfrontier.registry.ModBlocks;
import com.formicfrontier.registry.ModEntities;
import com.formicfrontier.registry.ModItems;
import com.formicfrontier.registry.ModNetworking;
import com.formicfrontier.sim.ColonyEconomy;
import com.formicfrontier.world.ColonySavedState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FormicFrontier implements ModInitializer {
	public static final String MOD_ID = "formic_frontier";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private int economyTicker;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();
		ModEntities.initialize();
		ModNetworking.initialize();
		FormicCommands.initialize();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++economyTicker < ColonyEconomy.ECONOMY_TICK_INTERVAL) {
				return;
			}
			economyTicker = 0;
			ColonySavedState savedState = ColonySavedState.get(server);
			if (savedState.tickEconomy()) {
				savedState.setDirty();
			}
			var level = server.getLevel(net.minecraft.server.level.ServerLevel.OVERWORLD);
			if (level != null && savedState.tickWorld(level)) {
				savedState.setDirty();
			}
		});

		LOGGER.info("Formic Frontier initialized");
	}
}
