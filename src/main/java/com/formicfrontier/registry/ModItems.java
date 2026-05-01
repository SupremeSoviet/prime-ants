package com.formicfrontier.registry;

import com.formicfrontier.FormicFrontier;
import com.formicfrontier.item.ColonyTabletItem;
import com.formicfrontier.item.FormicWeaponItem;
import com.formicfrontier.item.QueenEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;
import java.util.function.Function;

public final class ModItems {
	public static final TagKey<Item> REPAIRS_CHITIN_ARMOR = TagKey.create(Registries.ITEM, FormicFrontier.id("repairs_chitin_armor"));
	public static final ResourceKey<EquipmentAsset> CHITIN_EQUIPMENT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, FormicFrontier.id("chitin"));
	public static final ResourceKey<EquipmentAsset> RESIN_CHITIN_EQUIPMENT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, FormicFrontier.id("resin_chitin"));
	public static final ArmorMaterial CHITIN_ARMOR_MATERIAL = new ArmorMaterial(
			15,
			Map.of(
					ArmorType.HELMET, 2,
					ArmorType.CHESTPLATE, 5,
					ArmorType.LEGGINGS, 4,
					ArmorType.BOOTS, 2,
					ArmorType.BODY, 5
			),
			18,
			SoundEvents.ARMOR_EQUIP_TURTLE,
			0.0f,
			0.0f,
			REPAIRS_CHITIN_ARMOR,
			CHITIN_EQUIPMENT_ASSET
	);
	public static final ArmorMaterial RESIN_CHITIN_ARMOR_MATERIAL = new ArmorMaterial(
			21,
			Map.of(
					ArmorType.HELMET, 3,
					ArmorType.CHESTPLATE, 6,
					ArmorType.LEGGINGS, 5,
					ArmorType.BOOTS, 3,
					ArmorType.BODY, 6
			),
			15,
			SoundEvents.ARMOR_EQUIP_TURTLE,
			1.0f,
			0.0f,
			REPAIRS_CHITIN_ARMOR,
			RESIN_CHITIN_EQUIPMENT_ASSET
	);

	public static final Item QUEEN_EGG = register("queen_egg", QueenEggItem::new, new Item.Properties().stacksTo(16));
	public static final Item COLONY_TABLET = register("colony_tablet", ColonyTabletItem::new, new Item.Properties().stacksTo(1));
	public static final Item CHITIN_SHARD = register("chitin_shard", Item::new, new Item.Properties());
	public static final Item CHITIN_FIBER = register("chitin_fiber", Item::new, new Item.Properties());
	public static final Item CHITIN_PLATE = register("chitin_plate", Item::new, new Item.Properties());
	public static final Item RESIN_GLOB = register("resin_glob", Item::new, new Item.Properties());
	public static final Item FUNGUS_CULTURE = register("fungus_culture", Item::new, new Item.Properties());
	public static final Item LEAF_MASH = register("leaf_mash", Item::new, new Item.Properties());
	public static final Item APHID_HONEYDEW = register("aphid_honeydew", Item::new, new Item.Properties().stacksTo(16));
	public static final Item VENOM_SAC = register("venom_sac", Item::new, new Item.Properties().stacksTo(16));
	public static final Item MANDIBLE_PLATE = register("mandible_plate", Item::new, new Item.Properties());
	public static final Item ROYAL_WAX = register("royal_wax", Item::new, new Item.Properties().stacksTo(16));
	public static final Item CHITIN_SPORE = register("chitin_spore", properties -> new BlockItem(ModBlocks.CHITIN_BED, properties), new Item.Properties());
	public static final Item PHEROMONE_TOKEN = register("pheromone_token", Item::new, new Item.Properties());
	public static final Item PHEROMONE_DUST = register("pheromone_dust", Item::new, new Item.Properties());
	public static final Item COLONY_SEAL = register("colony_seal", Item::new, new Item.Properties().stacksTo(16));
	public static final Item WAR_BANNER = register("war_banner", Item::new, new Item.Properties().stacksTo(16));
	public static final Item RAW_BIOMASS = register("raw_biomass", Item::new, new Item.Properties());
	public static final Item ROYAL_JELLY = register("royal_jelly", Item::new, new Item.Properties().stacksTo(16));
	public static final Item CHITIN_HELMET = register("chitin_helmet", Item::new, new Item.Properties().durability(ArmorType.HELMET.getDurability(15)).humanoidArmor(CHITIN_ARMOR_MATERIAL, ArmorType.HELMET));
	public static final Item CHITIN_CHESTPLATE = register("chitin_chestplate", Item::new, new Item.Properties().durability(ArmorType.CHESTPLATE.getDurability(15)).humanoidArmor(CHITIN_ARMOR_MATERIAL, ArmorType.CHESTPLATE));
	public static final Item CHITIN_LEGGINGS = register("chitin_leggings", Item::new, new Item.Properties().durability(ArmorType.LEGGINGS.getDurability(15)).humanoidArmor(CHITIN_ARMOR_MATERIAL, ArmorType.LEGGINGS));
	public static final Item CHITIN_BOOTS = register("chitin_boots", Item::new, new Item.Properties().durability(ArmorType.BOOTS.getDurability(15)).humanoidArmor(CHITIN_ARMOR_MATERIAL, ArmorType.BOOTS));
	public static final Item RESIN_CHITIN_HELMET = register("resin_chitin_helmet", Item::new, new Item.Properties().durability(ArmorType.HELMET.getDurability(21)).humanoidArmor(RESIN_CHITIN_ARMOR_MATERIAL, ArmorType.HELMET));
	public static final Item RESIN_CHITIN_CHESTPLATE = register("resin_chitin_chestplate", Item::new, new Item.Properties().durability(ArmorType.CHESTPLATE.getDurability(21)).humanoidArmor(RESIN_CHITIN_ARMOR_MATERIAL, ArmorType.CHESTPLATE));
	public static final Item RESIN_CHITIN_LEGGINGS = register("resin_chitin_leggings", Item::new, new Item.Properties().durability(ArmorType.LEGGINGS.getDurability(21)).humanoidArmor(RESIN_CHITIN_ARMOR_MATERIAL, ArmorType.LEGGINGS));
	public static final Item RESIN_CHITIN_BOOTS = register("resin_chitin_boots", Item::new, new Item.Properties().durability(ArmorType.BOOTS.getDurability(21)).humanoidArmor(RESIN_CHITIN_ARMOR_MATERIAL, ArmorType.BOOTS));
	public static final Item MANDIBLE_SABER = register("mandible_saber", properties -> new FormicWeaponItem(properties, 5.0f, false), new Item.Properties().durability(420));
	public static final Item VENOM_SPEAR = register("venom_spear", properties -> new FormicWeaponItem(properties, 4.0f, true), new Item.Properties().durability(360));

	private ModItems() {
	}

	public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, FormicFrontier.id(name));
		T item = itemFactory.apply(settings.setId(itemKey));
		return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(tab -> tab.accept(QUEEN_EGG));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(tab -> tab.accept(COLONY_TABLET));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(tab -> {
			tab.accept(CHITIN_SHARD);
			tab.accept(CHITIN_FIBER);
			tab.accept(CHITIN_PLATE);
			tab.accept(RESIN_GLOB);
			tab.accept(FUNGUS_CULTURE);
			tab.accept(LEAF_MASH);
			tab.accept(APHID_HONEYDEW);
			tab.accept(VENOM_SAC);
			tab.accept(MANDIBLE_PLATE);
			tab.accept(ROYAL_WAX);
			tab.accept(CHITIN_SPORE);
			tab.accept(PHEROMONE_TOKEN);
			tab.accept(PHEROMONE_DUST);
			tab.accept(COLONY_SEAL);
			tab.accept(WAR_BANNER);
			tab.accept(RAW_BIOMASS);
			tab.accept(ROYAL_JELLY);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(tab -> {
			tab.accept(CHITIN_HELMET);
			tab.accept(CHITIN_CHESTPLATE);
			tab.accept(CHITIN_LEGGINGS);
			tab.accept(CHITIN_BOOTS);
			tab.accept(RESIN_CHITIN_HELMET);
			tab.accept(RESIN_CHITIN_CHESTPLATE);
			tab.accept(RESIN_CHITIN_LEGGINGS);
			tab.accept(RESIN_CHITIN_BOOTS);
			tab.accept(MANDIBLE_SABER);
			tab.accept(VENOM_SPEAR);
		});
	}
}
