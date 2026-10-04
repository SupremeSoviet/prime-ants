package dev.primeants.client;

import dev.primeants.PrimeAnts;
import net.fabricmc.api.ClientModInitializer;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.AntForm;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public final class PrimeAntsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(AntRenderer.WORKER_LAYER, () -> AntModel.createBodyLayer(AntForm.WORKER));
        ModelLayerRegistry.registerModelLayer(AntRenderer.QUEEN_LAYER, () -> AntModel.createBodyLayer(AntForm.QUEEN));
        EntityRendererRegistry.register(AntEntities.WORKER, context -> new AntRenderer(context, AntForm.WORKER));
        EntityRendererRegistry.register(AntEntities.QUEEN, context -> new AntRenderer(context, AntForm.QUEEN));
        PrimeAnts.LOGGER.info("Prime Ants client infrastructure initialized.");
    }
}
