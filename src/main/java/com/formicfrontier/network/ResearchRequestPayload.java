package com.formicfrontier.network;

import com.formicfrontier.FormicFrontier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResearchRequestPayload(String nodeId) implements CustomPacketPayload {
	public static final Identifier ID = FormicFrontier.id("research_request");
	public static final CustomPacketPayload.Type<ResearchRequestPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ResearchRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			ResearchRequestPayload::nodeId,
			ResearchRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
