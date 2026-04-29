package com.formicfrontier.client.screen;

import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.network.DiplomacyRequestPayload;
import com.formicfrontier.network.PriorityRequestPayload;
import com.formicfrontier.network.ResearchRequestPayload;
import com.formicfrontier.network.TradeRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

public final class ColonyStatusScreen extends Screen {
	private static final Tab[] TABS = {
			new Tab("Overview", "formic_frontier.ui.tab.overview_short", "formic_frontier.ui.tab.overview"),
			new Tab("Build", "formic_frontier.ui.tab.build_short", "formic_frontier.ui.tab.buildings"),
			new Tab("Needs", "formic_frontier.ui.tab.needs_short", "formic_frontier.ui.tab.requests"),
			new Tab("Research", "formic_frontier.ui.tab.research_short", "formic_frontier.ui.tab.research"),
			new Tab("Trade", "formic_frontier.ui.tab.trade_short", "formic_frontier.ui.tab.trade"),
			new Tab("Instinct", "formic_frontier.ui.tab.instinct_short", "formic_frontier.ui.tab.instinct"),
			new Tab("Relations", "formic_frontier.ui.tab.relations_short", "formic_frontier.ui.tab.relations")
	};
	private final ColonyUiSnapshot snapshot;
	private String selectedTab;
	private int selectedDiplomacyTargetId;

	public ColonyStatusScreen(ColonyUiSnapshot snapshot) {
		super(Component.translatable("formic_frontier.ui.title"));
		this.snapshot = snapshot;
		this.selectedTab = normalizeTab(snapshot.initialTab());
	}

	@Override
	protected void init() {
		selectedTab = normalizeTab(selectedTab);
		int panelX = panelX();
		int panelY = panelY();
		int available = panelWidth() - 24;
		int tabW = Math.max(44, (available - (TABS.length - 1) * 3) / TABS.length);
		for (int i = 0; i < TABS.length; i++) {
			Tab tab = TABS[i];
			Button button = Button.builder(Component.translatable(tab.shortKey()), widget -> {
						selectedTab = tab.id();
						rebuildWidgets();
					})
					.bounds(panelX + 12 + i * (tabW + 3), panelY + 32, tabW, 18)
					.build();
			button.active = !tab.id().equals(selectedTab);
			addRenderableWidget(button);
		}

		if ("Trade".equals(selectedTab)) {
			addTradeButtons();
		} else if ("Research".equals(selectedTab)) {
			addResearchButtons();
		} else if ("Instinct".equals(selectedTab)) {
			addInstinctButtons();
		} else if ("Relations".equals(selectedTab)) {
			addRelationsButtons();
		}

		addRenderableWidget(Button.builder(Component.translatable("formic_frontier.ui.close"), button -> onClose())
				.bounds(panelX + panelWidth() - 58, panelY + 7, 46, 18)
				.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0xA8100D0A);
		int panelX = panelX();
		int panelY = panelY();
		int panelW = panelWidth();
		int panelH = panelHeight();
		graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF21A140F);
		graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFFC9974B);
		graphics.fill(panelX, panelY, panelX + panelW, panelY + 28, 0xFF2B1E14);
		graphics.drawString(font, ellipsize(snapshot.title(), panelW - 162), panelX + 12, panelY + 8, 0xFFFFE0A6, true);
		String meta = Component.translatable(snapshot.cultureKey()).getString() + " | " + snapshot.rank();
		graphics.drawString(font, ellipsize(meta, 110), panelX + panelW - 172, panelY + 9, 0xFFEAC67D, false);

		drawResourceBar(graphics, panelX + 12, panelY + 55, panelW - 24);
		drawContent(graphics, panelX + 12, panelY + 101, panelW - 24, panelH - 142);
		drawFooter(graphics, panelX + 12, panelY + panelH - 34, panelW - 84);

		super.render(graphics, mouseX, mouseY, delta);
	}

	private void drawResourceBar(GuiGraphics graphics, int x, int y, int width) {
		int chipW = Math.max(54, Math.min(72, (width - 18) / 4));
		for (int i = 0; i < snapshot.resources().size(); i++) {
			ColonyUiSnapshot.Metric metric = snapshot.resources().get(i);
			int cx = x + (i % 4) * (chipW + 6);
			int cy = y + (i / 4) * 18;
			graphics.fill(cx, cy, cx + chipW, cy + 15, 0xFF241B13);
			graphics.fill(cx, cy, cx + 3, cy + 15, 0xFF000000 | metric.color());
			graphics.drawString(font, ellipsize(shortName(metric.labelKey()) + " " + metric.value(), chipW - 9), cx + 7, cy + 4, 0xFFF4E9C8, false);
		}
	}

	private void drawContent(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.drawString(font, tabLabel(selectedTab), x, y - 14, 0xFFFFE0A6, true);
		switch (selectedTab) {
			case "Build" -> drawBuildings(graphics, x, y, width, height);
			case "Needs" -> drawRequests(graphics, x, y, width, height);
			case "Research" -> drawResearch(graphics, x, y, width, height);
			case "Trade" -> drawTrades(graphics, x, y, width, height);
			case "Instinct" -> drawInstinct(graphics, x, y, width, height);
			case "Relations" -> drawRelations(graphics, x, y, width, height);
			default -> drawOverview(graphics, x, y, width, height);
		}
	}

	private void drawOverview(GuiGraphics graphics, int x, int y, int width, int height) {
		int rowY = y;
		for (ColonyUiSnapshot.OverviewEntry entry : snapshot.overview()) {
			drawTableRow(graphics, x, rowY, width, translated(entry.labelKey()), entry.value(), entry.progress(), entry.color());
			rowY += 22;
		}
		int popY = y + Math.min(138, height - 42);
		graphics.drawString(font, translated("formic_frontier.ui.population"), x, popY, 0xFFEAC67D, true);
		int chipW = Math.max(48, (width - 18) / 4);
		for (int i = 0; i < snapshot.population().size() && i < 8; i++) {
			ColonyUiSnapshot.Metric metric = snapshot.population().get(i);
			int cx = x + (i % 4) * (chipW + 6);
			int cy = popY + 13 + (i / 4) * 16;
			graphics.fill(cx, cy, cx + chipW, cy + 13, 0xFF241B13);
			graphics.fill(cx, cy, cx + 3, cy + 13, 0xFF000000 | metric.color());
			graphics.drawString(font, ellipsize(shortName(metric.labelKey()) + " " + metric.value(), chipW - 8), cx + 6, cy + 3, 0xFFF4E9C8, false);
		}
	}

	private void drawBuildings(GuiGraphics graphics, int x, int y, int width, int height) {
		List<ColonyUiSnapshot.BuildingEntry> rows = snapshot.buildings().stream()
				.sorted(Comparator.comparing(ColonyUiSnapshot.BuildingEntry::complete).thenComparing(ColonyUiSnapshot.BuildingEntry::typeId))
				.limit(maxRows(height))
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.BuildingEntry entry = rows.get(i);
			String title = translated(entry.labelKey()) + " L" + entry.level();
			String detail = translated(entry.statusKey()) + " " + entry.progress() + "% " + entry.detail();
			drawTableRow(graphics, x, y + i * 22, width, title, detail, entry.progress(), entry.complete() ? 0x6DD08E : 0xD69042);
		}
	}

	private void drawRequests(GuiGraphics graphics, int x, int y, int width, int height) {
		if (snapshot.requests().isEmpty()) {
			drawTableRow(graphics, x, y, width, translated("formic_frontier.ui.no_requests"), translated("formic_frontier.ui.no_requests_detail"), 100, 0x6DD08E);
			return;
		}
		List<ColonyUiSnapshot.RequestEntry> rows = snapshot.requests().stream()
				.filter(entry -> entry.fulfilled() < entry.needed())
				.sorted((first, second) -> Integer.compare(second.needed() - second.fulfilled(), first.needed() - first.fulfilled()))
				.limit(maxRows(height))
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.RequestEntry entry = rows.get(i);
			String title = translated(entry.buildingKey()) + " -> " + translated(entry.resourceKey());
			String detail = entry.fulfilled() + "/" + entry.needed() + " | " + entry.reason();
			drawTableRow(graphics, x, y + i * 22, width, title, detail, percent(entry.fulfilled(), entry.needed()), 0xD69042);
		}
	}

	private void drawResearch(GuiGraphics graphics, int x, int y, int width, int height) {
		List<ColonyUiSnapshot.ResearchEntry> rows = snapshot.research().stream()
				.sorted(Comparator.comparing(ColonyUiSnapshot.ResearchEntry::complete)
						.thenComparing(entry -> !entry.active())
						.thenComparing(entry -> !entry.startable())
						.thenComparing(ColonyUiSnapshot.ResearchEntry::nodeId))
				.limit(maxRows(height))
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.ResearchEntry entry = rows.get(i);
			int progress = entry.complete() ? 100 : percent(entry.progress(), entry.duration());
			int color = entry.active() ? 0xB58BFF : entry.startable() ? 0x6DD08E : 0x8A6D47;
			drawTableRow(graphics, x, y + i * 22, width, entry.label(), entry.status(), progress, color);
		}
	}

	private void drawTrades(GuiGraphics graphics, int x, int y, int width, int height) {
		for (int i = 0; i < snapshot.trades().size() && i < maxRows(height); i++) {
			ColonyUiSnapshot.TradeEntry entry = snapshot.trades().get(i);
			String title = entry.inputCount() + " " + shortName(entry.inputKey()) + " -> " + entry.outputCount() + " " + shortName(entry.outputKey());
			drawTableRow(graphics, x, y + i * 22, width, title, entry.status(), entry.available() ? 100 : 0, entry.available() ? 0x6DD08E : 0x8A6D47);
		}
	}

	private void drawInstinct(GuiGraphics graphics, int x, int y, int width, int height) {
		drawTableRow(graphics, x, y, width, translated("formic_frontier.ui.instinct_help"), translated("formic_frontier.ui.instinct_detail"), 0, 0xC9974B);
		for (int i = 0; i < snapshot.instinct().size() && i < maxRows(height) - 1; i++) {
			ColonyUiSnapshot.Metric metric = snapshot.instinct().get(i);
			drawTableRow(graphics, x, y + 26 + i * 22, width, translated(metric.labelKey()), Component.translatable("formic_frontier.ui.priority", i + 1).getString(), percent(metric.value(), metric.max()), metric.color());
		}
	}

	private void drawRelations(GuiGraphics graphics, int x, int y, int width, int height) {
		if (snapshot.relations().isEmpty()) {
			drawTableRow(graphics, x, y, width, translated("formic_frontier.ui.no_relations"), "", 0, 0xC9974B);
			return;
		}
		for (int i = 0; i < snapshot.relations().size() && i < Math.min(4, maxRows(height)); i++) {
			ColonyUiSnapshot.RelationEntry entry = snapshot.relations().get(i);
			drawTableRow(graphics, x, y + i * 22, width, "#" + entry.colonyId(), translated(entry.labelKey()), relationProgress(entry.stateId()), 0xB58BFF);
		}
		int actionY = y + 94;
		graphics.drawString(font, translated("formic_frontier.ui.selected_target") + " #" + selectedDiplomacyTargetId, x, actionY, 0xFFEAC67D, false);
		for (int i = 0; i < snapshot.diplomacy().size() && i < 3; i++) {
			ColonyUiSnapshot.DiplomacyEntry entry = snapshot.diplomacy().get(i);
			String cost = entry.tokenCost() + "T " + entry.dustCost() + "D " + entry.sealCost() + "S";
			drawTableRow(graphics, x, actionY + 13 + i * 22, width, entry.label(), cost + " | " + entry.minRank(), 0, 0xB58BFF);
		}
	}

	private void drawFooter(GuiGraphics graphics, int x, int y, int width) {
		graphics.drawString(font, Component.translatable("formic_frontier.ui.colony_id", snapshot.colonyId()).getString(), x, y, 0xFFEAC67D, false);
		graphics.drawString(font, Component.translatable("formic_frontier.ui.reputation", snapshot.reputation()).getString(), x + 78, y, 0xFFF4E9C8, false);
		graphics.drawString(font, Component.translatable("formic_frontier.ui.claim", snapshot.claimRadius()).getString(), x + 138, y, 0xFFF4E9C8, false);
		if (!snapshot.feedbackMessage().isBlank()) {
			graphics.fill(x, y + 12, x + width, y + 28, 0xFF2C2418);
			graphics.renderOutline(x, y + 12, width, 16, 0xFF6DD08E);
			graphics.drawString(font, ellipsize(snapshot.feedbackMessage(), width - 10), x + 5, y + 16, 0xFFBFF0C7, false);
		}
	}

	private void drawTableRow(GuiGraphics graphics, int x, int y, int width, String title, String detail, int progress, int color) {
		graphics.fill(x, y, x + width, y + 19, 0xFF241B13);
		graphics.fill(x, y, x + 3, y + 19, 0xFF000000 | color);
		graphics.drawString(font, ellipsize(title, Math.max(40, width / 2 - 10)), x + 7, y + 5, 0xFFFFE0A6, false);
		graphics.drawString(font, ellipsize(detail, Math.max(40, width - width / 2 - 48)), x + width / 2, y + 5, 0xFFF4E9C8, false);
		drawMiniProgress(graphics, x + width - 42, y + 7, 34, progress, color);
	}

	private void drawMiniProgress(GuiGraphics graphics, int x, int y, int width, int progress, int color) {
		if (progress <= 0) {
			graphics.fill(x, y, x + width, y + 4, 0xFF120D09);
			return;
		}
		graphics.fill(x, y, x + width, y + 4, 0xFF120D09);
		graphics.fill(x, y, x + width * Math.max(0, Math.min(100, progress)) / 100, y + 4, 0xFF000000 | color);
	}

	private void addTradeButtons() {
		int x = panelX() + 12;
		int y = panelY() + panelHeight() - 58;
		int columns = Math.max(2, Math.min(5, (panelWidth() - 86) / 78));
		int buttonW = Math.max(68, Math.min(86, (panelWidth() - 92) / columns));
		for (int i = 0; i < snapshot.trades().size() && i < 5; i++) {
			ColonyUiSnapshot.TradeEntry entry = snapshot.trades().get(i);
			Button button = Button.builder(Component.literal(ellipsize(shortName(entry.outputKey()), buttonW - 8)), widget -> ClientPlayNetworking.send(new TradeRequestPayload(entry.offerId())))
					.bounds(x + (i % columns) * (buttonW + 4), y + (i / columns) * 21, buttonW, 18)
					.build();
			button.active = entry.available();
			addRenderableWidget(button);
		}
	}

	private void addResearchButtons() {
		int x = panelX() + 12;
		int y = panelY() + panelHeight() - 58;
		int columns = Math.max(2, Math.min(4, (panelWidth() - 86) / 94));
		int buttonW = Math.max(82, Math.min(108, (panelWidth() - 92) / columns));
		List<ColonyUiSnapshot.ResearchEntry> rows = snapshot.research().stream()
				.filter(entry -> !entry.complete())
				.sorted(Comparator.comparing(ColonyUiSnapshot.ResearchEntry::startable).reversed().thenComparing(ColonyUiSnapshot.ResearchEntry::nodeId))
				.limit(4)
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.ResearchEntry entry = rows.get(i);
			Button button = Button.builder(Component.literal(ellipsize(entry.label(), buttonW - 8)), widget -> ClientPlayNetworking.send(new ResearchRequestPayload(entry.nodeId())))
					.bounds(x + (i % columns) * (buttonW + 4), y + (i / columns) * 21, buttonW, 18)
					.build();
			button.active = entry.startable();
			addRenderableWidget(button);
		}
	}

	private void addInstinctButtons() {
		int x = panelX() + 12;
		int y = panelY() + panelHeight() - 37;
		String[] ids = {"food", "ore", "chitin", "defense"};
		int buttonW = Math.max(58, Math.min(78, (panelWidth() - 100) / ids.length));
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			addRenderableWidget(Button.builder(Component.translatable("formic_frontier.instinct." + id), widget -> ClientPlayNetworking.send(new PriorityRequestPayload(id)))
					.bounds(x + i * (buttonW + 4), y, buttonW, 18)
					.build());
		}
	}

	private void addRelationsButtons() {
		int x = panelX() + 12;
		int y = panelY() + panelHeight() - 58;
		if (selectedDiplomacyTargetId <= 0 && !snapshot.relations().isEmpty()) {
			selectedDiplomacyTargetId = snapshot.relations().getFirst().colonyId();
		}
		for (int i = 0; i < snapshot.relations().size() && i < 4; i++) {
			ColonyUiSnapshot.RelationEntry entry = snapshot.relations().get(i);
			Button button = Button.builder(Component.literal("#" + entry.colonyId()), widget -> {
						selectedDiplomacyTargetId = entry.colonyId();
						rebuildWidgets();
					})
					.bounds(x + i * 43, y, 38, 18)
					.build();
			button.active = selectedDiplomacyTargetId != entry.colonyId();
			addRenderableWidget(button);
		}
		int actionW = Math.max(76, Math.min(96, (panelWidth() - 36) / 3));
		for (int i = 0; i < snapshot.diplomacy().size() && i < 3; i++) {
			ColonyUiSnapshot.DiplomacyEntry entry = snapshot.diplomacy().get(i);
			addRenderableWidget(Button.builder(Component.literal(ellipsize(entry.label(), actionW - 8)), widget -> ClientPlayNetworking.send(new DiplomacyRequestPayload(entry.actionId(), selectedDiplomacyTargetId)))
					.bounds(x + i * (actionW + 4), y + 21, actionW, 18)
					.build());
		}
	}

	private int percent(int value, int max) {
		if (max <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(100, value * 100 / max));
	}

	private int relationProgress(String stateId) {
		return switch (stateId) {
			case "ALLY" -> 100;
			case "NEUTRAL" -> 60;
			case "RIVAL" -> 30;
			case "WAR" -> 10;
			default -> 0;
		};
	}

	private int maxRows(int height) {
		return Math.max(4, Math.min(7, height / 22));
	}

	private String tabLabel(String id) {
		for (Tab tab : TABS) {
			if (tab.id().equals(id)) {
				return translated(tab.titleKey());
			}
		}
		return id;
	}

	private String translated(String key) {
		return Component.translatable(key).getString();
	}

	private String shortName(String key) {
		String value = translated(key);
		int space = value.indexOf(' ');
		return space > 0 && value.length() > 12 ? value.substring(0, space) : value;
	}

	private String ellipsize(String value, int pixelWidth) {
		if (value == null || value.isBlank() || pixelWidth <= 0) {
			return "";
		}
		if (font.width(value) <= pixelWidth) {
			return value;
		}
		String ellipsis = "...";
		return font.plainSubstrByWidth(value, Math.max(0, pixelWidth - font.width(ellipsis))) + ellipsis;
	}

	private String normalizeTab(String id) {
		if (id == null || id.isBlank()) {
			return "Overview";
		}
		return switch (id) {
			case "Buildings", "Build" -> "Build";
			case "Requests", "Needs" -> "Needs";
			case "Diplomacy", "Relations", "Events" -> "Relations";
			case "Research", "Trade", "Instinct" -> id;
			default -> "Overview";
		};
	}

	private int panelWidth() {
		return Math.min(560, Math.max(360, (int) (width * 0.74f)));
	}

	private int panelHeight() {
		return Math.min(330, Math.max(250, (int) (height * 0.74f)));
	}

	private int panelX() {
		return (width - panelWidth()) / 2;
	}

	private int panelY() {
		return (height - panelHeight()) / 2;
	}

	private record Tab(String id, String shortKey, String titleKey) {
	}
}
