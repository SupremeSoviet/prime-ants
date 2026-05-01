package com.formicfrontier.network;

import com.formicfrontier.FormicFrontier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PriorityRequestPayload(String priorityId) implements CustomPacketPayload {
	public static final Identifier ID = FormicFrontier.id("priority_request");
	public static final CustomPacketPayload.Type<PriorityRequestPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, PriorityRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			PriorityRequestPayload::priorityId,
			PriorityRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
