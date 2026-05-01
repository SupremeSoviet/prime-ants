package com.formicfrontier.network;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyContract;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyEvent;
import com.formicfrontier.sim.ColonyIdentity;
import com.formicfrontier.sim.ColonyPersonality;
import com.formicfrontier.sim.ColonyRank;
import com.formicfrontier.sim.ColonyRequest;
import com.formicfrontier.sim.ColonyTradeCatalog;
import com.formicfrontier.sim.ContractDeliveryOption;
import com.formicfrontier.sim.DiplomacyAction;
import com.formicfrontier.sim.GuideChapter;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResearchState;
import com.formicfrontier.sim.ResourceType;
import com.formicfrontier.sim.TaskPriority;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public record ColonyUiSnapshot(
		int colonyId,
		String title,
		String initialTab,
		String feedbackMessage,
		String cultureKey,
		String personalityKey,
		String personalityDetailKey,
		String relationshipKey,
		int relationshipColor,
		String rank,
		String currentTask,
		int queenHealth,
		boolean queenAlive,
		int reputation,
		int claimRadius,
		List<OverviewEntry> overview,
		List<Metric> resources,
		List<Metric> population,
		List<BuildingEntry> buildings,
		List<RequestEntry> requests,
		List<ResearchEntry> research,
		List<TradeEntry> trades,
		String tradeActivity,
		List<Metric> instinct,
		List<DiplomacyEntry> diplomacy,
		List<RelationEntry> relations,
		List<GuideEntry> guide,
		List<EventEntry> events
) {
	public static ColonyUiSnapshot from(ColonyData colony, String initialTab, String feedbackMessage) {
		ColonyPersonality personality = ColonyIdentity.personality(colony);
		List<Metric> resources = new ArrayList<>();
		for (ResourceType type : ResourceType.values()) {
			resources.add(new Metric(type.id(), resourceLabelKey(type), colony.resource(type), 0, resourceColor(type)));
		}

		List<Metric> population = new ArrayList<>();
		for (AntCaste caste : AntCaste.values()) {
			population.add(new Metric(caste.id(), casteLabelKey(caste), colony.casteCount(caste), 0, casteColor(caste)));
		}

		List<BuildingEntry> buildings = new ArrayList<>();
		for (ColonyBuilding building : colony.progress().buildingsView()) {
			buildings.add(buildingEntry(colony, building, false));
		}
		for (BuildingType queued : colony.progress().buildQueueView()) {
			buildings.add(new BuildingEntry(
					queued.id(),
					buildingLabelKey(queued),
					"queued",
					1,
					0,
					false,
					"formic_frontier.ui.status.queued",
					buildingCostText(colony, queued)
			));
		}

		List<RequestEntry> requests = new ArrayList<>();
		for (ColonyRequest request : colony.progress().requestsView()) {
			ColonyContract contract = ColonyContract.from(request);
			ContractDeliveryOption delivery = ContractDeliveryOption.forResource(request.resource());
			int deliveryAmount = delivery.deliveredAmount(contract.resourceCost());
			requests.add(new RequestEntry(
					request.building().id(),
					buildingLabelKey(request.building()),
					request.resource().id(),
					resourceLabelKey(request.resource()),
					request.fulfilled(),
					request.needed(),
					request.reason(),
					contract.id(),
					contract.resourceCost(),
					delivery.itemKey(),
					delivery.itemCountFor(deliveryAmount),
					deliveryAmount,
					contract.priority(),
					contract.rewardTokens(),
					contract.reputationDelta()
			));
		}
		requests.sort(ColonyUiSnapshot::compareRequests);

		List<ResearchEntry> research = new ArrayList<>();
		Optional<ResearchState> activeResearch = colony.progress().activeResearch();
		for (ResearchNode node : ResearchNode.values()) {
			boolean complete = colony.progress().hasResearch(node.id());
			boolean active = activeResearch.map(state -> state.nodeId().equals(node.id())).orElse(false);
			int progress = active ? activeResearch.get().progressTicks() : 0;
			research.add(new ResearchEntry(
					node.id(),
					node.label(),
					progress,
					node.durationTicks(),
					complete,
					active,
					node.canStart(colony),
					researchStatus(colony, node)
			));
		}

		List<TradeEntry> trades = new ArrayList<>();
		try {
			for (ColonyTradeCatalog.Offer offer : ColonyTradeCatalog.offersView()) {
				if (!ColonyTradeCatalog.isVisible(colony, offer)) {
					continue;
				}
				trades.add(new TradeEntry(
						offer.id(),
						offer.input().getDescriptionId(),
						ColonyTradeCatalog.inputCount(colony, offer),
						offer.output().getDescriptionId(),
						ColonyTradeCatalog.outputCount(colony, offer),
						ColonyTradeCatalog.isAvailable(colony, offer),
						ColonyTradeCatalog.availabilityText(colony, offer)
				));
			}
		} catch (IllegalArgumentException | LinkageError exception) {
			// Pure unit tests can build snapshots before Minecraft bootstraps item registries.
		}

		List<Metric> instinct = new ArrayList<>();
		int order = colony.prioritiesView().size();
		for (TaskPriority priority : colony.prioritiesView()) {
			instinct.add(new Metric(priority.id(), instinctLabelKey(priority), order--, colony.prioritiesView().size(), instinctColor(priority)));
		}

		List<DiplomacyEntry> diplomacy = new ArrayList<>();
		for (DiplomacyAction action : DiplomacyAction.values()) {
			diplomacy.add(new DiplomacyEntry(action.id(), action.label(), action.tokenCost(), action.dustCost(), action.sealCost(), action.bannerCost(), action.minRank().displayName()));
		}

		List<RelationEntry> relations = new ArrayList<>();
		for (Map.Entry<String, com.formicfrontier.sim.DiplomacyState> entry : colony.progress().knownColoniesView().entrySet()) {
			int id = parseInt(entry.getKey(), 0);
			relations.add(new RelationEntry(id, entry.getValue().id(), relationLabelKey(entry.getValue().id())));
		}

		List<GuideEntry> guide = new ArrayList<>();
		for (GuideChapter chapter : GuideChapter.values()) {
			boolean unlocked = chapter.unlocked(colony);
			guide.add(new GuideEntry(
					chapter.id(),
					chapter.titleKey(),
					unlocked ? chapter.detailKey() : chapter.lockedKey(),
					unlocked,
					chapter.color()
			));
		}

		List<EventEntry> events = new ArrayList<>();
		for (ColonyEvent event : colony.progress().eventsView()) {
			events.add(new EventEntry(event.ageTicks(), event.message()));
		}
		List<OverviewEntry> overview = overviewRows(colony, buildings, requests, research);

		return new ColonyUiSnapshot(
				colony.id(),
				colony.progress().name(),
				initialTab == null || initialTab.isBlank() ? "Overview" : initialTab,
				feedbackMessage == null ? "" : feedbackMessage,
				"formic_frontier.culture." + colony.progress().culture().id(),
				personality.labelKey(),
				personality.detailKey(),
				ColonyIdentity.relationshipKey(colony),
				ColonyIdentity.relationshipColor(colony),
				"formic_frontier.rank." + ColonyRank.current(colony).id(),
				colony.currentTask(),
				colony.queenHealth(),
				colony.queenAlive(),
				colony.progress().reputation(),
				colony.progress().claimRadius(),
				List.copyOf(overview),
				List.copyOf(resources),
				List.copyOf(population),
				List.copyOf(buildings),
				List.copyOf(requests),
				List.copyOf(research),
				List.copyOf(trades),
				tradeActivity(colony),
				List.copyOf(instinct),
				List.copyOf(diplomacy),
				List.copyOf(relations),
				List.copyOf(guide),
				List.copyOf(events)
		);
	}

	public void write(RegistryFriendlyByteBuf buf) {
		writeInt(buf, colonyId);
		writeString(buf, title);
		writeString(buf, initialTab);
		writeString(buf, feedbackMessage);
		writeString(buf, cultureKey);
		writeString(buf, personalityKey);
		writeString(buf, personalityDetailKey);
		writeString(buf, relationshipKey);
		writeInt(buf, relationshipColor);
		writeString(buf, rank);
		writeString(buf, currentTask);
		writeInt(buf, queenHealth);
		writeBoolean(buf, queenAlive);
		writeInt(buf, reputation);
		writeInt(buf, claimRadius);
		writeList(buf, overview, (target, entry) -> entry.write(target));
		writeList(buf, resources, (target, entry) -> entry.write(target));
		writeList(buf, population, (target, entry) -> entry.write(target));
		writeList(buf, buildings, (target, entry) -> entry.write(target));
		writeList(buf, requests, (target, entry) -> entry.write(target));
		writeList(buf, research, (target, entry) -> entry.write(target));
		writeList(buf, trades, (target, entry) -> entry.write(target));
		writeString(buf, tradeActivity);
		writeList(buf, instinct, (target, entry) -> entry.write(target));
		writeList(buf, diplomacy, (target, entry) -> entry.write(target));
		writeList(buf, relations, (target, entry) -> entry.write(target));
		writeList(buf, guide, (target, entry) -> entry.write(target));
		writeList(buf, events, (target, entry) -> entry.write(target));
	}

	public static ColonyUiSnapshot read(RegistryFriendlyByteBuf buf) {
		return new ColonyUiSnapshot(
				readInt(buf),
				readString(buf),
				readString(buf),
				readString(buf),
				readString(buf),
				readString(buf),
				readString(buf),
				readString(buf),
				readInt(buf),
				readString(buf),
				readString(buf),
				readInt(buf),
				readBoolean(buf),
				readInt(buf),
				readInt(buf),
				readList(buf, OverviewEntry::read),
				readList(buf, Metric::read),
				readList(buf, Metric::read),
				readList(buf, BuildingEntry::read),
				readList(buf, RequestEntry::read),
				readList(buf, ResearchEntry::read),
				readList(buf, TradeEntry::read),
				readString(buf),
				readList(buf, Metric::read),
				readList(buf, DiplomacyEntry::read),
				readList(buf, RelationEntry::read),
				readList(buf, GuideEntry::read),
				readList(buf, EventEntry::read)
		);
	}

	public static String buildingLabelKey(BuildingType type) {
		return switch (type) {
			case QUEEN_CHAMBER -> "formic_frontier.building.queen_hall";
			case FOOD_STORE -> "block.formic_frontier.food_chamber";
			case NURSERY -> "block.formic_frontier.nursery_chamber";
			case MINE -> "block.formic_frontier.mine_chamber";
			case CHITIN_FARM -> "block.formic_frontier.chitin_bed";
			case BARRACKS -> "block.formic_frontier.barracks_chamber";
			case MARKET -> "block.formic_frontier.market_chamber";
			case DIPLOMACY_SHRINE -> "block.formic_frontier.diplomacy_shrine";
			case WATCH_POST -> "block.formic_frontier.watch_post";
			case RESIN_DEPOT -> "block.formic_frontier.resin_depot";
			case PHEROMONE_ARCHIVE -> "block.formic_frontier.pheromone_archive";
			case FUNGUS_GARDEN -> "block.formic_frontier.fungus_garden";
			case VENOM_PRESS -> "block.formic_frontier.venom_press";
			case ARMORY -> "block.formic_frontier.armory";
			case GREAT_MOUND -> "formic_frontier.building.great_mound";
			case QUEEN_VAULT -> "formic_frontier.building.queen_vault";
			case TRADE_HUB -> "formic_frontier.building.trade_hub";
			case ROAD -> "formic_frontier.building.road";
		};
	}

	public static String resourceLabelKey(ResourceType type) {
		return "formic_frontier.resource." + type.id();
	}

	private static int compareRequests(RequestEntry first, RequestEntry second) {
		int priority = Integer.compare(second.priority(), first.priority());
		if (priority != 0) {
			return priority;
		}
		int missing = Integer.compare(second.resourceCost(), first.resourceCost());
		if (missing != 0) {
			return missing;
		}
		int reward = Integer.compare(second.rewardTokens(), first.rewardTokens());
		if (reward != 0) {
			return reward;
		}
		return first.buildingId().compareTo(second.buildingId());
	}

	private static BuildingEntry buildingEntry(ColonyData colony, ColonyBuilding building, boolean queued) {
		String statusKey;
		String detail;
		if (queued) {
			statusKey = "formic_frontier.ui.status.queued";
			detail = buildingCostText(colony, building.type());
		} else {
			BuildingVisualStage stage = building.visualStage();
			statusKey = stage.statusKey();
			detail = switch (stage) {
				case PLANNED -> buildingCostText(colony, building.type());
				case CONSTRUCTION, REPAIRING -> missingRequestText(colony, building.type());
				case DAMAGED -> building.disabledTicks() + "t";
				default -> "";
			};
		}
		return new BuildingEntry(
				building.type().id(),
				buildingLabelKey(building.type()),
				building.pos().toShortString(),
				building.level(),
				building.constructionProgress(),
				building.complete(),
				statusKey,
				detail
		);
	}

	private static String missingRequestText(ColonyData colony, BuildingType type) {
		StringJoiner joiner = new StringJoiner(", ");
		for (ColonyRequest request : colony.progress().requestsView()) {
			if (request.building() == type && !request.complete()) {
				joiner.add(humanizeId(request.resource().id()) + " " + request.missing());
			}
		}
		return joiner.length() == 0 ? "" : "Needs " + joiner;
	}

	private static String buildingCostText(ColonyData colony, BuildingType type) {
		StringJoiner joiner = new StringJoiner(", ");
		for (ResourceType resource : ResourceType.values()) {
			int cost = type.cost(resource);
			if (cost > 0) {
				int missing = Math.max(0, cost - colony.resource(resource));
				joiner.add(humanizeId(resource.id()) + " " + cost + (missing > 0 ? " (-" + missing + ")" : ""));
			}
		}
		return joiner.length() == 0 ? "" : joiner.toString();
	}

	private static String researchStatus(ColonyData colony, ResearchNode node) {
		if (colony.progress().hasResearch(node.id())) {
			return "Complete";
		}
		Optional<ResearchState> active = colony.progress().activeResearch();
		if (active.isPresent()) {
			if (active.get().nodeId().equals(node.id())) {
				return "Active";
			}
			return "Another research is active";
		}
		if (!colony.progress().hasCompleted(BuildingType.PHEROMONE_ARCHIVE)) {
			return "Requires Pheromone Archive";
		}
		if (!colony.progress().hasCompleted(node.requiredBuilding())) {
			return "Requires " + humanizeId(node.requiredBuilding().id());
		}
		for (String prerequisite : node.prerequisites()) {
			if (!colony.progress().hasResearch(prerequisite)) {
				return "Requires " + humanizeId(prerequisite);
			}
		}
		StringJoiner missing = new StringJoiner(", ");
		for (Map.Entry<ResourceType, Integer> entry : node.costsView().entrySet()) {
			int shortage = entry.getValue() - colony.resource(entry.getKey());
			if (shortage > 0) {
				missing.add(humanizeId(entry.getKey().id()) + " " + shortage);
			}
		}
		return missing.length() == 0 ? "Ready" : "Needs " + missing;
	}

	private static List<OverviewEntry> overviewRows(ColonyData colony, List<BuildingEntry> buildings, List<RequestEntry> requests, List<ResearchEntry> research) {
		List<OverviewEntry> rows = new ArrayList<>();
		rows.add(new OverviewEntry("formic_frontier.ui.current_task", colony.currentTask(), 0, 0xC9974B));
		recurringEventSummary(colony).ifPresent(message -> rows.add(new OverviewEntry("formic_frontier.ui.tab.events", message, 0, 0xB58BFF)));
		rows.add(new OverviewEntry(
				"formic_frontier.ui.workforce",
				colony.casteCount(AntCaste.WORKER) + " workers, " + colony.casteCount(AntCaste.MINER) + " miners, " + colony.casteCount(AntCaste.SOLDIER) + " guards",
				0,
				0xD8B57A
		));
		buildings.stream()
				.filter(entry -> !entry.complete() || "queued".equals(entry.pos()))
				.findFirst()
				.ifPresent(entry -> rows.add(new OverviewEntry(
						"formic_frontier.ui.active_building",
						activeBuildingSummary(entry),
						entry.progress(),
						entry.complete() ? 0x6DD08E : 0xD69042
				)));
		requests.stream()
				.filter(entry -> entry.fulfilled() < entry.needed())
				.sorted((first, second) -> Integer.compare(second.needed() - second.fulfilled(), first.needed() - first.fulfilled()))
				.findFirst()
				.ifPresent(entry -> rows.add(new OverviewEntry(
						"formic_frontier.ui.top_need",
						humanizeId(entry.resourceId()) + " " + (entry.needed() - entry.fulfilled()) + " -> " + humanizeId(entry.buildingId()),
						percent(entry.fulfilled(), entry.needed()),
						resourceColor(ResourceType.fromId(entry.resourceId()))
				)));
		research.stream()
				.filter(ResearchEntry::active)
				.findFirst()
				.or(() -> research.stream().filter(entry -> !entry.complete()).findFirst())
				.ifPresent(entry -> rows.add(new OverviewEntry(
						"formic_frontier.ui.active_research",
						entry.label() + " | " + entry.status(),
						entry.complete() ? 100 : percent(entry.progress(), entry.duration()),
						entry.active() ? 0xB58BFF : 0x8A6D47
				)));
		rows.add(new OverviewEntry("formic_frontier.caste.queen", colony.queenHealth() + " hp / " + (colony.queenAlive() ? "alive" : "lost"), colony.queenAlive() ? 100 : 0, 0xF0C26E));
		return rows.stream().limit(6).toList();
	}

	private static Optional<String> recurringEventSummary(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.map(ColonyEvent::message)
				.filter(message -> message.startsWith("Recurring event: "))
				.findFirst()
				.map(message -> message.substring("Recurring event: ".length()));
	}

	private static String tradeActivity(ColonyData colony) {
		return colony.progress().eventsView().stream()
				.map(ColonyEvent::message)
				.filter(message -> message.startsWith("Recurring event: trade caravan "))
				.findFirst()
				.map(ColonyUiSnapshot::compactTradeActivity)
				.orElse("");
	}

	private static String compactTradeActivity(String message) {
		String value = message.substring("Recurring event: trade caravan ".length());
		if (value.startsWith("exchanged ")) {
			value = value.substring("exchanged ".length());
		}
		value = value.replace(" for ", " -> ").replace(" with colony #", " with #");
		return "Caravan: " + humanizeTradeTokens(value);
	}

	private static String humanizeTradeTokens(String value) {
		StringJoiner joiner = new StringJoiner(" ");
		for (String token : value.split(" ")) {
			String normalized = token.toLowerCase(Locale.ROOT);
			String display = token;
			for (ResourceType type : ResourceType.values()) {
				if (type.id().equals(normalized)) {
					display = humanizeId(type.id());
					break;
				}
			}
			joiner.add(display);
		}
		return joiner.toString();
	}

	private static String activeBuildingSummary(BuildingEntry entry) {
		StringBuilder builder = new StringBuilder();
		builder.append(humanizeId(entry.typeId())).append(" ").append(entry.progress()).append("%");
		if (!entry.detail().isBlank()) {
			builder.append(" | ").append(entry.detail());
		}
		return builder.toString();
	}

	private static String humanizeId(String id) {
		String[] parts = id.replace('-', '_').split("_");
		StringJoiner joiner = new StringJoiner(" ");
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}
			joiner.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
		}
		return joiner.toString();
	}

	private static int percent(int value, int max) {
		if (max <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(100, value * 100 / max));
	}

	private static String casteLabelKey(AntCaste caste) {
		return "formic_frontier.caste." + caste.id();
	}

	private static String instinctLabelKey(TaskPriority priority) {
		return "formic_frontier.instinct." + priority.id();
	}

	private static String relationLabelKey(String stateId) {
		return "formic_frontier.relation." + stateId.toLowerCase(Locale.ROOT);
	}

	private static int resourceColor(ResourceType type) {
		return switch (type) {
			case FOOD -> 0x91C46C;
			case ORE -> 0xB9B8AC;
			case CHITIN -> 0xD6B16E;
			case RESIN -> 0xD69042;
			case FUNGUS -> 0x9BC76C;
			case VENOM -> 0x7DD66C;
			case KNOWLEDGE -> 0xB58BFF;
		};
	}

	private static int casteColor(AntCaste caste) {
		return switch (caste) {
			case QUEEN -> 0xF0C26E;
			case GIANT -> 0xD06B5D;
			case MAJOR -> 0xD99555;
			case SOLDIER -> 0xC15D48;
			case MINER -> 0xA9A9A9;
			case SCOUT -> 0x8EC8D6;
			case WORKER -> 0xD8B57A;
		};
	}

	private static int instinctColor(TaskPriority priority) {
		return switch (priority) {
			case FOOD -> 0x91C46C;
			case ORE -> 0xB9B8AC;
			case CHITIN -> 0xD6B16E;
			case DEFENSE -> 0xD06B5D;
		};
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static void writeString(RegistryFriendlyByteBuf buf, String value) {
		ByteBufCodecs.STRING_UTF8.encode(buf, value == null ? "" : value);
	}

	private static String readString(RegistryFriendlyByteBuf buf) {
		return ByteBufCodecs.STRING_UTF8.decode(buf);
	}

	private static void writeInt(RegistryFriendlyByteBuf buf, int value) {
		ByteBufCodecs.VAR_INT.encode(buf, value);
	}

	private static int readInt(RegistryFriendlyByteBuf buf) {
		return ByteBufCodecs.VAR_INT.decode(buf);
	}

	private static void writeBoolean(RegistryFriendlyByteBuf buf, boolean value) {
		ByteBufCodecs.BOOL.encode(buf, value);
	}

	private static boolean readBoolean(RegistryFriendlyByteBuf buf) {
		return ByteBufCodecs.BOOL.decode(buf);
	}

	private static <T> void writeList(RegistryFriendlyByteBuf buf, List<T> list, EntryWriter<T> writer) {
		writeInt(buf, list.size());
		for (T entry : list) {
			writer.write(buf, entry);
		}
	}

	private static <T> List<T> readList(RegistryFriendlyByteBuf buf, EntryReader<T> reader) {
		int size = readInt(buf);
		List<T> values = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			values.add(reader.read(buf));
		}
		return List.copyOf(values);
	}

	private interface EntryWriter<T> {
		void write(RegistryFriendlyByteBuf buf, T entry);
	}

	private interface EntryReader<T> {
		T read(RegistryFriendlyByteBuf buf);
	}

	public record Metric(String id, String labelKey, int value, int max, int color) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, id);
			writeString(buf, labelKey);
			writeInt(buf, value);
			writeInt(buf, max);
			writeInt(buf, color);
		}

		private static Metric read(RegistryFriendlyByteBuf buf) {
			return new Metric(readString(buf), readString(buf), readInt(buf), readInt(buf), readInt(buf));
		}
	}

	public record OverviewEntry(String labelKey, String value, int progress, int color) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, labelKey);
			writeString(buf, value);
			writeInt(buf, progress);
			writeInt(buf, color);
		}

		private static OverviewEntry read(RegistryFriendlyByteBuf buf) {
			return new OverviewEntry(readString(buf), readString(buf), readInt(buf), readInt(buf));
		}
	}

	public record BuildingEntry(String typeId, String labelKey, String pos, int level, int progress, boolean complete, String statusKey, String detail) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, typeId);
			writeString(buf, labelKey);
			writeString(buf, pos);
			writeInt(buf, level);
			writeInt(buf, progress);
			writeBoolean(buf, complete);
			writeString(buf, statusKey);
			writeString(buf, detail);
		}

		private static BuildingEntry read(RegistryFriendlyByteBuf buf) {
			return new BuildingEntry(readString(buf), readString(buf), readString(buf), readInt(buf), readInt(buf), readBoolean(buf), readString(buf), readString(buf));
		}
	}

	public record RequestEntry(String buildingId, String buildingKey, String resourceId, String resourceKey, int fulfilled, int needed, String reason, String contractId, int resourceCost, String deliveryItemKey, int deliveryItemCount, int deliveryAmount, int priority, int rewardTokens, int reputationDelta) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, buildingId);
			writeString(buf, buildingKey);
			writeString(buf, resourceId);
			writeString(buf, resourceKey);
			writeInt(buf, fulfilled);
			writeInt(buf, needed);
			writeString(buf, reason);
			writeString(buf, contractId);
			writeInt(buf, resourceCost);
			writeString(buf, deliveryItemKey);
			writeInt(buf, deliveryItemCount);
			writeInt(buf, deliveryAmount);
			writeInt(buf, priority);
			writeInt(buf, rewardTokens);
			writeInt(buf, reputationDelta);
		}

		private static RequestEntry read(RegistryFriendlyByteBuf buf) {
			return new RequestEntry(readString(buf), readString(buf), readString(buf), readString(buf), readInt(buf), readInt(buf), readString(buf), readString(buf), readInt(buf), readString(buf), readInt(buf), readInt(buf), readInt(buf), readInt(buf), readInt(buf));
		}
	}

	public record ResearchEntry(String nodeId, String label, int progress, int duration, boolean complete, boolean active, boolean startable, String status) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, nodeId);
			writeString(buf, label);
			writeInt(buf, progress);
			writeInt(buf, duration);
			writeBoolean(buf, complete);
			writeBoolean(buf, active);
			writeBoolean(buf, startable);
			writeString(buf, status);
		}

		private static ResearchEntry read(RegistryFriendlyByteBuf buf) {
			return new ResearchEntry(readString(buf), readString(buf), readInt(buf), readInt(buf), readBoolean(buf), readBoolean(buf), readBoolean(buf), readString(buf));
		}
	}

	public record TradeEntry(String offerId, String inputKey, int inputCount, String outputKey, int outputCount, boolean available, String status) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, offerId);
			writeString(buf, inputKey);
			writeInt(buf, inputCount);
			writeString(buf, outputKey);
			writeInt(buf, outputCount);
			writeBoolean(buf, available);
			writeString(buf, status);
		}

		private static TradeEntry read(RegistryFriendlyByteBuf buf) {
			return new TradeEntry(readString(buf), readString(buf), readInt(buf), readString(buf), readInt(buf), readBoolean(buf), readString(buf));
		}
	}

	public record DiplomacyEntry(String actionId, String label, int tokenCost, int dustCost, int sealCost, int bannerCost, String minRank) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, actionId);
			writeString(buf, label);
			writeInt(buf, tokenCost);
			writeInt(buf, dustCost);
			writeInt(buf, sealCost);
			writeInt(buf, bannerCost);
			writeString(buf, minRank);
		}

		private static DiplomacyEntry read(RegistryFriendlyByteBuf buf) {
			return new DiplomacyEntry(readString(buf), readString(buf), readInt(buf), readInt(buf), readInt(buf), readInt(buf), readString(buf));
		}
	}

	public record RelationEntry(int colonyId, String stateId, String labelKey) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeInt(buf, colonyId);
			writeString(buf, stateId);
			writeString(buf, labelKey);
		}

		private static RelationEntry read(RegistryFriendlyByteBuf buf) {
			return new RelationEntry(readInt(buf), readString(buf), readString(buf));
		}
	}

	public record GuideEntry(String chapterId, String titleKey, String detailKey, boolean unlocked, int color) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeString(buf, chapterId);
			writeString(buf, titleKey);
			writeString(buf, detailKey);
			writeBoolean(buf, unlocked);
			writeInt(buf, color);
		}

		private static GuideEntry read(RegistryFriendlyByteBuf buf) {
			return new GuideEntry(readString(buf), readString(buf), readString(buf), readBoolean(buf), readInt(buf));
		}
	}

	public record EventEntry(int ageTicks, String message) {
		private void write(RegistryFriendlyByteBuf buf) {
			writeInt(buf, ageTicks);
			writeString(buf, message);
		}

		private static EventEntry read(RegistryFriendlyByteBuf buf) {
			return new EventEntry(readInt(buf), readString(buf));
		}
	}
}
