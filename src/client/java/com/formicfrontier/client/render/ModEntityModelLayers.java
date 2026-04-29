package com.formicfrontier.client.render;

import com.formicfrontier.FormicFrontier;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModEntityModelLayers {
	public static final ModelLayerLocation ANT = new ModelLayerLocation(FormicFrontier.id("ant"), "main");

	private ModEntityModelLayers() {
	}

	public static void registerModelLayers() {
		EntityModelLayerRegistry.registerModelLayer(ANT, AntEntityModel::getTexturedModelData);
	}
}
