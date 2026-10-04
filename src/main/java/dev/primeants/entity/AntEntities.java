package dev.primeants.entity;

import dev.primeants.PrimeAnts;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class AntEntities {
    public static final EntityType<LasiusNigerEntity> WORKER = register("lasius_niger_worker", AntForm.WORKER, 0.6F, 0.4F);
    public static final EntityType<LasiusNigerEntity> QUEEN = register("lasius_niger_queen", AntForm.QUEEN, 0.95F, 0.7F);

    private static EntityType<LasiusNigerEntity> register(String name, AntForm form, float width, float height) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID, name));
        EntityType<LasiusNigerEntity> type = EntityType.Builder.<LasiusNigerEntity>of(
                (entityType, level) -> new LasiusNigerEntity(entityType, level, form), MobCategory.CREATURE)
                .sized(width, height).eyeHeight(height * 0.65F).clientTrackingRange(8).updateInterval(2).noLootTable().build(key);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
        FabricDefaultAttributeRegistry.register(type, LasiusNigerEntity.attributes(form));
        return type;
    }

    public static void initialize() { }
    private AntEntities() { }
}
