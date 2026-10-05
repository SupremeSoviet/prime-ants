package dev.primeants;

import net.fabricmc.api.ModInitializer;
import dev.primeants.entity.AntEntities;
import dev.primeants.item.AntItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrimeAnts implements ModInitializer {
    public static final String MOD_ID = "prime_ants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AntEntities.initialize();
        dev.primeants.brood.NurseryBlocks.initialize();
        AntItems.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(dev.primeants.worker.TransferCustody::tick);
        LOGGER.info("Prime Ants initialized: physical founding, foraging, nurse feeding and finite food-supported brood growth.");
    }
}
