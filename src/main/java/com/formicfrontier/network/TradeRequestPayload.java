package com.formicfrontier.network;

import com.formicfrontier.FormicFrontier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TradeRequestPayload(String offerId) implements CustomPacketPayload {
	public static final Identifier ID = FormicFrontier.id("trade_request");
	public static final CustomPacketPayload.Type<TradeRequestPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, TradeRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			TradeRequestPayload::offerId,
			TradeRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
