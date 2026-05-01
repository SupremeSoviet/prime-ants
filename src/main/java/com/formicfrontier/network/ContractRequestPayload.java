package com.formicfrontier.network;

import com.formicfrontier.FormicFrontier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ContractRequestPayload(String contractId) implements CustomPacketPayload {
	public static final Identifier ID = FormicFrontier.id("contract_request");
	public static final CustomPacketPayload.Type<ContractRequestPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ContractRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			ContractRequestPayload::contractId,
			ContractRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
