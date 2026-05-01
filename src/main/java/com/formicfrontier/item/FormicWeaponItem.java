package com.formicfrontier.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FormicWeaponItem extends Item {
	private final float bonusDamage;
	private final boolean venom;

	public FormicWeaponItem(Properties properties, float bonusDamage, boolean venom) {
		super(properties);
		this.bonusDamage = bonusDamage;
		this.venom = venom;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (bonusDamage > 0.0f) {
			target.hurt(attacker.damageSources().mobAttack(attacker), bonusDamage);
		}
		if (venom) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
		}
	}
}
