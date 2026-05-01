package com.formicfrontier.sim;

import java.util.Locale;

public record ColonyContract(
		String id,
		BuildingType building,
		ResourceType resource,
		int needed,
		int fulfilled,
		int resourceCost,
		int priority,
		int rewardTokens,
		int reputationDelta,
		String reason
) {
	public static ColonyContract from(ColonyRequest request) {
		int missing = request.missing();
		int priority = priorityFor(request);
		int rewardTokens = Math.max(1, (missing + 7) / 8 + priority);
		int reputationDelta = Math.max(1, Math.min(6, (missing + 15) / 16 + priority / 2));
		return new ColonyContract(
				idFor(request),
				request.building(),
				request.resource(),
				request.needed(),
				request.fulfilled(),
				missing,
				priority,
				rewardTokens,
				reputationDelta,
				request.reason()
		);
	}

	public static String idFor(ColonyRequest request) {
		String reason = request.reason().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
		if (reason.length() > 24) {
			reason = reason.substring(0, 24);
		}
		String hash = Integer.toUnsignedString(request.reason().hashCode(), 16);
		return request.building().id() + ":" + request.resource().id() + ":" + reason + ":" + hash;
	}

	public boolean complete() {
		return fulfilled >= needed;
	}

	public int missing() {
		return resourceCost;
	}

	public ColonyRequest deliver(int amount) {
		return new ColonyRequest(building, resource, needed, fulfilled + Math.max(0, amount), reason);
	}

	public int rewardTokensFor(int delivered) {
		if (delivered <= 0) {
			return 0;
		}
		return Math.max(1, Math.min(rewardTokens, (delivered + 7) / 8 + priority));
	}

	public int reputationFor(int delivered) {
		if (delivered <= 0) {
			return 0;
		}
		return Math.max(1, Math.min(reputationDelta, (delivered + 15) / 16 + priority / 2));
	}

	private static int priorityFor(ColonyRequest request) {
		String reason = request.reason().toLowerCase(Locale.ROOT);
		if (reason.startsWith("famine")) {
			return 5;
		}
		if (reason.startsWith("invasion")) {
			return 5;
		}
		if (reason.startsWith("treaty")) {
			return 4;
		}
		if (reason.startsWith("expansion")) {
			return 4;
		}
		if (reason.startsWith("repair")) {
			return 4;
		}
		if (reason.startsWith("research")) {
			return 3;
		}
		if (request.building() == BuildingType.QUEEN_CHAMBER || request.building() == BuildingType.BARRACKS) {
			return 3;
		}
		if (reason.startsWith("construction")) {
			return 2;
		}
		return 1;
	}
}
