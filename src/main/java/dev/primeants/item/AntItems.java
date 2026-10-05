package dev.primeants.item;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.AntEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;

public final class AntItems {
    public static final Item DEBUG_QUEEN_EGG;
    public static final Item FLOWER_NECTAR;
    public static final Item FLOWER_NECTAR_V2, SMALL_PREY;

    static {
        var nectarKey=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID,"flower_nectar"));
        FLOWER_NECTAR=Registry.register(BuiltInRegistries.ITEM,nectarKey,new Item(new Item.Properties().setId(nectarKey)));
        var richKey=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID,"flower_nectar_v2"));
        FLOWER_NECTAR_V2=Registry.register(BuiltInRegistries.ITEM,richKey,new Item(new Item.Properties().setId(richKey)));
        var preyKey=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID,"small_prey"));
        SMALL_PREY=Registry.register(BuiltInRegistries.ITEM,preyKey,new Item(new Item.Properties().setId(preyKey)));
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID, "debug_lasius_niger_queen_egg"));
        DEBUG_QUEEN_EGG = Registry.register(BuiltInRegistries.ITEM, key,
                new DebugQueenEggItem(new Item.Properties().setId(key).spawnEgg(AntEntities.QUEEN)));
        DispenserBlock.registerBehavior(DEBUG_QUEEN_EGG, DispenseItemBehavior.NOOP);
    }

    public static void initialize() { }
    private AntItems() { }
}
