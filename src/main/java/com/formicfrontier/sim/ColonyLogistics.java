package com.formicfrontier.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ColonyLogistics {
	private ColonyLogistics() {
	}

	public static boolean tick(ColonyData colony) {
		boolean changed = fulfillRequests(colony);
		changed |= tickResearch(colony);
		return changed;
	}

	public static void requestResource(ColonyData colony, BuildingType building, ResourceType resource, int needed, String reason) {
		if (needed <= 0) {
			return;
		}
		for (ColonyRequest request : colony.progress().requests()) {
			if (!request.complete() && request.building() == building && request.resource() == resource && request.reason().equals(reason)) {
				return;
			}
		}
		colony.progress().requests().add(new ColonyRequest(building, resource, needed, 0, reason));
		colony.addEvent("Request opened: " + needed + " " + resource.id() + " for " + building.id());
	}

	public static List<ColonyContract> contracts(ColonyData colony) {
		List<ColonyContract> contracts = new ArrayList<>();
		for (ColonyRequest request : colony.progress().requestsView()) {
			if (!request.complete()) {
				contracts.add(ColonyContract.from(request));
			}
		}
		return List.copyOf(contracts);
	}

	public static Optional<ColonyContract> contract(ColonyData colony, String contractId) {
		if (contractId == null || contractId.isBlank()) {
			return Optional.empty();
		}
		return contracts(colony).stream()
				.filter(contract -> contract.id().equals(contractId))
				.findFirst();
	}

	public static ContractDeliveryResult fulfillContract(ColonyData colony, String contractId, int amount) {
		if (amount <= 0) {
			return new ContractDeliveryResult(false, null, 0, 0, 0, false, "No delivery amount.", "");
		}
		for (int index = 0; index < colony.progress().requests().size(); index++) {
			ColonyRequest request = colony.progress().requests().get(index);
			if (request.complete()) {
				continue;
			}
			ColonyContract contract = ColonyContract.from(request);
			if (!contract.id().equals(contractId)) {
				continue;
			}
			int delivered = Math.min(contract.missing(), amount);
			ColonyRequest next = contract.deliver(delivered);
			boolean complete = next.complete();
			if (complete) {
				colony.progress().requests().remove(index);
			} else {
				colony.progress().requests().set(index, next);
			}
			int rewardTokens = contract.rewardTokensFor(delivered);
			int reputation = contract.reputationFor(delivered);
			colony.addResource(contract.resource(), delivered);
			colony.progress().addReputation(reputation);
			colony.setCurrentTask((complete ? "Contract fulfilled: " : "Contract helped: ")
					+ delivered + " " + contract.resource().id() + " for " + contract.building().id());
			colony.addEvent("Contract: delivered " + delivered + " " + contract.resource().id()
					+ " for " + contract.building().id() + " (+" + rewardTokens + " token, +" + reputation + " rep)");
			String payoffMessage = complete ? applyCompletedContractPayoff(colony, contract) : "";
			return new ContractDeliveryResult(true, contract, delivered, rewardTokens, reputation, complete, complete ? "Contract fulfilled." : "Contract delivery received.", payoffMessage);
		}
		return new ContractDeliveryResult(false, null, 0, 0, 0, false, "Contract is no longer open.", "");
	}

	public static StartResearchResult startResearch(ColonyData colony, String nodeId) {
		ResearchNode node = ResearchNode.fromId(nodeId);
		if (colony.progress().hasResearch(node.id())) {
			return new StartResearchResult(false, node.label() + " is already complete.");
		}
		if (colony.progress().activeResearch().isPresent()) {
			return new StartResearchResult(false, "Another research is already active.");
		}
		if (!colony.progress().hasCompleted(BuildingType.PHEROMONE_ARCHIVE)) {
			return new StartResearchResult(false, node.label() + " requires pheromone_archive.");
		}
		if (!colony.progress().hasCompleted(node.requiredBuilding())) {
			return new StartResearchResult(false, node.label() + " requires " + node.requiredBuilding().id() + ".");
		}
		for (String prerequisite : node.prerequisites()) {
			if (!colony.progress().hasResearch(prerequisite)) {
				return new StartResearchResult(false, node.label() + " requires " + prerequisite + ".");
			}
		}
		boolean missing = false;
		for (Map.Entry<ResourceType, Integer> entry : node.costsView().entrySet()) {
			int shortage = entry.getValue() - colony.resource(entry.getKey());
			if (shortage > 0) {
				requestResource(colony, node.requiredBuilding(), entry.getKey(), shortage, "research " + node.id());
				missing = true;
			}
		}
		if (missing) {
			colony.setCurrentTask("Waiting for research resources: " + node.id());
			return new StartResearchResult(false, "Research lacks resources; requests were opened.");
		}
		clearResearchRequests(colony, node.id());
		node.consumeCosts(colony);
		colony.progress().setActiveResearch(Optional.of(new ResearchState(node.id(), 0)));
		colony.setCurrentTask("Researching " + node.label());
		colony.addEvent("Research started: " + node.label());
		return new StartResearchResult(true, "Research started: " + node.label());
	}

	private static boolean fulfillRequests(ColonyData colony) {
		boolean changed = false;
		int throughput = Math.max(1, colony.casteCount(AntCaste.WORKER))
				+ (colony.progress().hasCompleted(BuildingType.RESIN_DEPOT) ? 3 : 0)
				+ colony.progress().culture().workerBias();
		for (int index = 0; index < colony.progress().requests().size(); index++) {
			ColonyRequest request = colony.progress().requests().get(index);
			if (request.complete()) {
				colony.progress().requests().remove(index--);
				changed = true;
				continue;
			}
			if (isPlayerSupplyRequest(request.reason())) {
				continue;
			}
			int available = colony.resource(request.resource());
			if (available <= 0) {
				continue;
			}
			int delivered = Math.min(Math.min(request.missing(), available), throughput);
			if (delivered <= 0) {
				continue;
			}
			colony.addResource(request.resource(), -delivered);
			ColonyRequest next = request.addFulfilled(delivered);
			colony.progress().requests().set(index, next);
			colony.setCurrentTask("Logistics delivered " + delivered + " " + request.resource().id() + " to " + request.building().id());
			if (next.complete()) {
				colony.addEvent("Request fulfilled: " + request.reason());
			}
			changed = true;
			break;
		}
		return changed;
	}

	private static boolean tickResearch(ColonyData colony) {
		Optional<ResearchState> active = colony.progress().activeResearch();
		if (active.isEmpty()) {
			if (colony.progress().hasCompleted(BuildingType.PHEROMONE_ARCHIVE)) {
				colony.addResource(ResourceType.KNOWLEDGE, 1 + completed(colony, BuildingType.PHEROMONE_ARCHIVE));
				return true;
			}
			return false;
		}
		ResearchNode node = ResearchNode.fromId(active.get().nodeId());
		int speed = ColonyEconomy.ECONOMY_TICK_INTERVAL + completed(colony, BuildingType.PHEROMONE_ARCHIVE) * 10;
		ResearchState advanced = active.get().advance(speed);
		if (advanced.progressTicks() >= node.durationTicks()) {
			colony.progress().completeResearch(node.id());
			colony.progress().setActiveResearch(Optional.empty());
			colony.setCurrentTask("Completed research: " + node.label());
			colony.addEvent("Research complete: " + node.label());
		} else {
			colony.progress().setActiveResearch(Optional.of(advanced));
			colony.setCurrentTask("Researching " + node.label() + " " + advanced.progressTicks() + "/" + node.durationTicks());
		}
		return true;
	}

	private static int completed(ColonyData colony, BuildingType type) {
		return (int) colony.progress().buildingsView().stream()
				.filter(building -> building.type() == type && building.complete())
				.count();
	}

	private static boolean isPlayerSupplyRequest(String reason) {
		String normalized = reason.toLowerCase(java.util.Locale.ROOT);
		return normalized.startsWith("construction ")
				|| normalized.startsWith("research ")
				|| normalized.startsWith("repair ")
				|| normalized.startsWith("famine")
				|| normalized.startsWith("migration")
				|| normalized.startsWith("invasion")
				|| normalized.startsWith("treaty")
				|| normalized.startsWith("expansion");
	}

	private static String applyCompletedContractPayoff(ColonyData colony, ColonyContract contract) {
		if (!isResearchRequest(contract.reason())) {
			return "";
		}
		String nodeId = contract.reason().substring("research ".length()).trim();
		try {
			StartResearchResult result = startResearch(colony, nodeId);
			if (result.started()) {
				String message = "Research started: " + ResearchNode.fromId(nodeId).label();
				colony.addEvent("Contract payoff: " + message);
				return message;
			}
		} catch (IllegalArgumentException ignored) {
			// Bad legacy request data should not invalidate an otherwise completed contract.
		}
		return "";
	}

	private static boolean isResearchRequest(String reason) {
		return reason.toLowerCase(java.util.Locale.ROOT).startsWith("research ");
	}

	private static void clearResearchRequests(ColonyData colony, String nodeId) {
		String reason = "research " + nodeId;
		colony.progress().requests().removeIf(request -> request.reason().equals(reason));
	}

	public record StartResearchResult(boolean started, String message) {
	}

	public record ContractDeliveryResult(boolean success, ColonyContract contract, int delivered, int rewardTokens, int reputationDelta, boolean complete, String message, String payoffMessage) {
	}
}
