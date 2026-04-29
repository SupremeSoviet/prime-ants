package com.formicfrontier.network;

import com.formicfrontier.FormicFrontier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ColonyUiPayload(ColonyUiSnapshot snapshot) implements CustomPacketPayload {
	public static final Identifier ID = FormicFrontier.id("colony_ui");
	public static final CustomPacketPayload.Type<ColonyUiPayload> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ColonyUiPayload> CODEC = new StreamCodec<>() {
		@Override
		public ColonyUiPayload decode(RegistryFriendlyByteBuf buf) {
			return new ColonyUiPayload(ColonyUiSnapshot.read(buf));
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, ColonyUiPayload payload) {
			payload.snapshot().write(buf);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
