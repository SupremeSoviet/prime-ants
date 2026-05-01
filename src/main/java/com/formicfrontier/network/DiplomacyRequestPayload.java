package com.formicfrontier.network;

import com.formicfrontier.FormicFrontier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DiplomacyRequestPayload(String actionId, int targetColonyId) implements CustomPacketPayload {
	public static final Identifier ID = FormicFrontier.id("diplomacy_request");
	public static final CustomPacketPayload.Type<DiplomacyRequestPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, DiplomacyRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			DiplomacyRequestPayload::actionId,
			ByteBufCodecs.VAR_INT,
			DiplomacyRequestPayload::targetColonyId,
			DiplomacyRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
