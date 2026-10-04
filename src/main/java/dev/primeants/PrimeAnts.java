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
        LOGGER.info("Prime Ants initialized: protected physical founding and bounded claustral first clutch; no external feeding or foraging.");
    }
}
