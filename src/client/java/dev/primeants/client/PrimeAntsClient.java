package dev.primeants.client;

import dev.primeants.PrimeAnts;
import net.fabricmc.api.ClientModInitializer;

public final class PrimeAntsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PrimeAnts.LOGGER.info("Prime Ants client infrastructure initialized.");
    }
}
