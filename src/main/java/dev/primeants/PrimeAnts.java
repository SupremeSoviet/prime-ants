package dev.primeants;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrimeAnts implements ModInitializer {
    public static final String MOD_ID = "prime_ants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Prime Ants infrastructure initialized; colony not implemented.");
    }
}
