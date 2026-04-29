package com.formicfrontier.registry;

import com.formicfrontier.FormicFrontier;
import com.formicfrontier.block.ChitinBedBlock;
import com.formicfrontier.block.ColonyInteractBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class ModBlocks {
	public static final Block NEST_MOUND = register("nest_mound", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(1.4f).sound(SoundType.GRAVEL), true);
	public static final Block NEST_CORE = register("nest_core", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.2f).sound(SoundType.ROOTED_DIRT), true);
	public static final Block COLONY_LEDGER = register("colony_ledger", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(1.8f).sound(SoundType.WOOD), true);
	public static final Block FOOD_CHAMBER = register("food_chamber", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(1.6f).sound(SoundType.FUNGUS), true);
	public static final Block NURSERY_CHAMBER = register("nursery_chamber", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(1.6f).sound(SoundType.HONEY_BLOCK), true);
	public static final Block MINE_CHAMBER = register("mine_chamber", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.8f).sound(SoundType.DEEPSLATE), true);
	public static final Block BARRACKS_CHAMBER = register("barracks_chamber", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.4f).sound(SoundType.NETHERITE_BLOCK), true);
	public static final Block MARKET_CHAMBER = register("market_chamber", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(1.8f).sound(SoundType.HONEY_BLOCK), true);
	public static final Block DIPLOMACY_SHRINE = register("diplomacy_shrine", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.AMETHYST), true);
	public static final Block WATCH_POST = register("watch_post", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.2f).sound(SoundType.BONE_BLOCK), true);
	public static final Block RESIN_DEPOT = register("resin_depot", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.HONEY_BLOCK), true);
	public static final Block PHEROMONE_ARCHIVE = register("pheromone_archive", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.3f).sound(SoundType.AMETHYST), true);
	public static final Block FUNGUS_GARDEN = register("fungus_garden", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(1.2f).sound(SoundType.FUNGUS), true);
	public static final Block VENOM_PRESS = register("venom_press", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.6f).sound(SoundType.SLIME_BLOCK), true);
	public static final Block ARMORY = register("armory", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.8f).sound(SoundType.NETHERITE_BLOCK), true);
	public static final Block FOOD_NODE = register("food_node", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(0.8f).sound(SoundType.MOSS), true);
	public static final Block ORE_NODE = register("ore_node", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.STONE), true);
	public static final Block CHITIN_NODE = register("chitin_node", ColonyInteractBlock::new, BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.BONE_BLOCK), true);
	public static final Block CHITIN_BED = register("chitin_bed", ChitinBedBlock::new, BlockBehaviour.Properties.of().strength(0.4f).sound(SoundType.BONE_BLOCK).randomTicks(), false);

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, FormicFrontier.id(name));
		Block block = blockFactory.apply(settings.setId(blockKey));

		if (shouldRegisterItem) {
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, FormicFrontier.id(name));
			BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
			Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		}

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(tab -> {
			tab.accept(NEST_MOUND.asItem());
			tab.accept(NEST_CORE.asItem());
			tab.accept(COLONY_LEDGER.asItem());
			tab.accept(FOOD_CHAMBER.asItem());
			tab.accept(NURSERY_CHAMBER.asItem());
			tab.accept(MINE_CHAMBER.asItem());
			tab.accept(BARRACKS_CHAMBER.asItem());
			tab.accept(MARKET_CHAMBER.asItem());
			tab.accept(DIPLOMACY_SHRINE.asItem());
			tab.accept(WATCH_POST.asItem());
			tab.accept(RESIN_DEPOT.asItem());
			tab.accept(PHEROMONE_ARCHIVE.asItem());
			tab.accept(FUNGUS_GARDEN.asItem());
			tab.accept(VENOM_PRESS.asItem());
			tab.accept(ARMORY.asItem());
			tab.accept(FOOD_NODE.asItem());
			tab.accept(ORE_NODE.asItem());
			tab.accept(CHITIN_NODE.asItem());
		});
	}
}
