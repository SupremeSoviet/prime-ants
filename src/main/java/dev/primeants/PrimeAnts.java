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
        AntItems.initialize();
        LOGGER.info("Prime Ants adults initialized; egg queens can physically found protected nests. Brood unimplemented.");
    }
}
