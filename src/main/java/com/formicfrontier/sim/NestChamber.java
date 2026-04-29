package com.formicfrontier.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record NestChamber(String type, BlockPos pos, int level) {
	public static final Codec<NestChamber> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("type").forGetter(NestChamber::type),
			BlockPos.CODEC.fieldOf("pos").forGetter(NestChamber::pos),
			Codec.INT.fieldOf("level").forGetter(NestChamber::level)
	).apply(instance, NestChamber::new));

	public static NestChamber core(BlockPos pos) {
		return new NestChamber("nest_core", pos, 1);
	}
}
