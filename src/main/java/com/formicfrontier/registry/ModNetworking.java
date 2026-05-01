package com.formicfrontier.registry;

import com.formicfrontier.network.ColonyUiPayload;
import com.formicfrontier.network.ContractRequestPayload;
import com.formicfrontier.network.DiplomacyRequestPayload;
import com.formicfrontier.network.PriorityRequestPayload;
import com.formicfrontier.network.ResearchRequestPayload;
import com.formicfrontier.network.TradeRequestPayload;
import com.formicfrontier.world.ColonyService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.playS2C().register(ColonyUiPayload.TYPE, ColonyUiPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ContractRequestPayload.TYPE, ContractRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TradeRequestPayload.TYPE, TradeRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PriorityRequestPayload.TYPE, PriorityRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DiplomacyRequestPayload.TYPE, DiplomacyRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ResearchRequestPayload.TYPE, ResearchRequestPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ContractRequestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ColonyService.completeContract(context.player(), payload.contractId()))
		);
		ServerPlayNetworking.registerGlobalReceiver(TradeRequestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ColonyService.trade(context.player(), payload.offerId()))
		);
		ServerPlayNetworking.registerGlobalReceiver(PriorityRequestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ColonyService.setTopPriority(context.player(), payload.priorityId()))
		);
		ServerPlayNetworking.registerGlobalReceiver(DiplomacyRequestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ColonyService.performDiplomacy(context.player(), payload.actionId(), payload.targetColonyId()))
		);
		ServerPlayNetworking.registerGlobalReceiver(ResearchRequestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ColonyService.startResearch(context.player(), payload.nodeId()))
		);
	}
}
