package com.formicfrontier.registry;

import com.formicfrontier.FormicFrontier;
import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.sim.AntCaste;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<AntEntity> ANT = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ResourceKey.create(Registries.ENTITY_TYPE, FormicFrontier.id("ant")),
			EntityType.Builder.of(AntEntity::new, MobCategory.CREATURE)
					.sized(AntCaste.WORKER.width(), AntCaste.WORKER.height())
					.clientTrackingRange(10)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, FormicFrontier.id("ant")))
	);

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(ANT, AntEntity.createAntAttributes());
	}
}
