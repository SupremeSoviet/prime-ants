package dev.primeants.brood;

import dev.primeants.PrimeAnts;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.Set;

public final class NurseryBlocks {
    public static final Block NEST_SOIL = soil();
    public static final BroodPileBlock BROOD_PILE = pile();
    public static final dev.primeants.worker.NestCacheBlock NEST_CACHE = Registry.register(BuiltInRegistries.BLOCK,key("nest_cache"),new dev.primeants.worker.NestCacheBlock(BlockBehaviour.Properties.of().setId(key("nest_cache")).noCollision().noOcclusion().strength(0.1F)));
    public static final BlockEntityType<dev.primeants.worker.NestCache> CACHE_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("nest_cache"),new BlockEntityType<>(dev.primeants.worker.NestCache::new,Set.of(NEST_CACHE)));
    public static final dev.primeants.worker.MaterialStoreBlock MATERIAL_STORE = Registry.register(BuiltInRegistries.BLOCK,key("material_store"),new dev.primeants.worker.MaterialStoreBlock(BlockBehaviour.Properties.of().setId(key("material_store")).noCollision().noOcclusion().strength(0.1F)));
    public static final BlockEntityType<dev.primeants.worker.MaterialStore> STORE_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("material_store"),new BlockEntityType<>(dev.primeants.worker.MaterialStore::new,Set.of(MATERIAL_STORE)));
    public static final BlockEntityType<BroodPile> BROOD_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("brood_pile"), new BlockEntityType<>(BroodPile::new, Set.of(BROOD_PILE)));
    private static Identifier id(String n) { return Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID, n); }
    private static ResourceKey<Block> key(String n) { return ResourceKey.create(Registries.BLOCK, id(n)); }
    private static Block soil() { return Registry.register(BuiltInRegistries.BLOCK, key("nest_soil"), new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).setId(key("nest_soil")))); }
    private static BroodPileBlock pile() { return Registry.register(BuiltInRegistries.BLOCK, key("brood_pile"), new BroodPileBlock(BlockBehaviour.Properties.of().setId(key("brood_pile")).noCollision().noOcclusion().strength(0.1F))); }
    public static void initialize() { }
    private NurseryBlocks() { }
}
