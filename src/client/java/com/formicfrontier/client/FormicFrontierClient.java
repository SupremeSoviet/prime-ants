package com.formicfrontier.client;

import com.formicfrontier.client.render.AntEntityRenderer;
import com.formicfrontier.client.render.ModEntityModelLayers;
import com.formicfrontier.client.screen.ColonyStatusScreen;
import com.formicfrontier.network.ColonyUiPayload;
import com.formicfrontier.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class FormicFrontierClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModEntityModelLayers.registerModelLayers();
		EntityRenderers.register(ModEntities.ANT, AntEntityRenderer::new);
		VisualQaClient.initialize();

		ClientPlayNetworking.registerGlobalReceiver(ColonyUiPayload.TYPE, (payload, context) ->
				context.client().execute(() -> Minecraft.getInstance().setScreen(
						new ColonyStatusScreen(payload.snapshot())
				))
		);
	}
}
