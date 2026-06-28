package com.formicfrontier.client.screen;

import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.network.ContractRequestPayload;
import com.formicfrontier.network.DiplomacyRequestPayload;
import com.formicfrontier.network.PriorityRequestPayload;
import com.formicfrontier.network.ResearchRequestPayload;
import com.formicfrontier.network.TradeRequestPayload;
import com.formicfrontier.registry.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formicfrontier.sim.ResearchNode;

/**
 * Colony tablet UI, redrawn from scratch as a cohesive amber-chitin "field
 * tablet". Everything - the panel, the tab bar, the resource strip, the cards
 * and the action buttons - is custom-drawn with the same warm gradient/bevel
 * language, so the screen no longer mixes vanilla grey stone buttons with a
 * hand-drawn panel. Layout is computed top-down from the panel rect with real
 * spacing between sections, so headings never collide with the resource strip
 * and cards are sized to their content instead of stretching across the panel.
 */
public final class ColonyStatusScreen extends Screen {
	private static final Tab[] TABS = {
			new Tab("Overview", "formic_frontier.ui.tab.overview_short", "formic_frontier.ui.tab.overview"),
			new Tab("Build", "formic_frontier.ui.tab.build_short", "formic_frontier.ui.tab.buildings"),
			new Tab("Needs", "formic_frontier.ui.tab.needs_short", "formic_frontier.ui.tab.requests"),
			new Tab("Research", "formic_frontier.ui.tab.research_short", "formic_frontier.ui.tab.research"),
			new Tab("Trade", "formic_frontier.ui.tab.trade_short", "formic_frontier.ui.tab.trade"),
			new Tab("Instinct", "formic_frontier.ui.tab.instinct_short", "formic_frontier.ui.tab.instinct"),
			new Tab("Guide", "formic_frontier.ui.tab.guide_short", "formic_frontier.ui.tab.guide"),
			new Tab("Relations", "formic_frontier.ui.tab.relations_short", "formic_frontier.ui.tab.relations")
	};

	// --- Palette -----------------------------------------------------------
	private static final int SCRIM_TOP = 0x96100D0A;
	private static final int SCRIM_BOTTOM = 0xC6070504;
	private static final int PANEL_TOP = 0xF5332618;
	private static final int PANEL_BOTTOM = 0xF514100A;
	private static final int PANEL_BORDER = 0xFF0C0805;
	private static final int PANEL_GLOW = 0x82E0B264;
	private static final int HEADER_TOP = 0xFF45331E;
	private static final int HEADER_BOTTOM = 0xFF221810;
	private static final int ACCENT = 0xFFE0B05A;
	private static final int ACCENT_DIM = 0xFF8C6A38;
	private static final int CARD_TOP = 0xF0352815;
	private static final int CARD_BOTTOM = 0xF01D150C;
	private static final int CARD_EDGE = 0xFF6E5131;
	private static final int CHIP_TOP = 0xFF31251A;
	private static final int CHIP_BOTTOM = 0xFF1A130C;
	private static final int CHIP_EDGE = 0xFF503B23;
	private static final int ROW_TOP = 0xFF2C2116;
	private static final int ROW_BOTTOM = 0xFF1B140D;
	private static final int BEVEL_HI = 0x3EFFE7B2;
	private static final int BEVEL_LO = 0x52000000;
	private static final int TRACK_BG = 0xFF120D09;
	private static final int TEXT_MAIN = 0xFFFFE7B4;
	private static final int TEXT_SOFT = 0xFFF1E2C0;
	private static final int TEXT_MUTED = 0xFFD9B574;
	private static final int TEXT_FAINT = 0xFF9C8054;

	private final ColonyUiSnapshot snapshot;
	private String selectedTab;
	private int selectedDiplomacyTargetId;

	public ColonyStatusScreen(ColonyUiSnapshot snapshot) {
		super(Component.translatable("formic_frontier.ui.title"));
		this.snapshot = snapshot;
		this.selectedTab = normalizeTab(snapshot.initialTab());
	}

	// =======================================================================
	// Widget construction
	// =======================================================================
	@Override
	protected void init() {
		selectedTab = normalizeTab(selectedTab);
		int px = panelX();
		int py = panelY();
		int pw = panelWidth();
		int innerW = pw - 24;

		// Tab bar: evenly spaced themed tabs, the active one highlighted.
		int gap = 4;
		int tabW = (innerW - (TABS.length - 1) * gap) / TABS.length;
		int tabX = px + 12;
		int tabY = py + 33;
		for (Tab tab : TABS) {
			boolean isActive = tab.id().equals(selectedTab);
			FormicButton button = new FormicButton(tabX, tabY, tabW, 19,
					Component.translatable(tab.shortKey()),
					() -> {
						selectedTab = tab.id();
						rebuildWidgets();
					}, ButtonStyle.TAB);
			button.selected = isActive;
			addRenderableWidget(button);
			tabX += tabW + gap;
		}

		// Close button lives in the header.
		addRenderableWidget(new FormicButton(px + pw - 58, py + 6, 48, 17,
				Component.translatable("formic_frontier.ui.close"), () -> onClose(), ButtonStyle.ACTION));

		switch (selectedTab) {
			case "Trade" -> addTradeButtons();
			case "Needs" -> addContractButtons();
			case "Research" -> addResearchButtons();
			case "Instinct" -> addInstinctButtons();
			case "Relations" -> addRelationsButtons();
			default -> {
			}
		}
	}

	// =======================================================================
	// Top-level render
	// =======================================================================
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fillGradient(0, 0, width, height, SCRIM_TOP, SCRIM_BOTTOM);

		int px = panelX();
		int py = panelY();
		int pw = panelWidth();
		int ph = panelHeight();
		int x = px + 12;
		int innerW = pw - 24;

		// Panel body: drop shadow, warm gradient, dark outer + glowing inner frame.
		g.fill(px + 7, py + 9, px + pw + 7, py + ph + 9, 0x59000000);
		g.fill(px + 3, py + 4, px + pw + 4, py + ph + 5, 0x40000000);
		g.fillGradient(px, py, px + pw, py + ph, PANEL_TOP, PANEL_BOTTOM);
		g.fill(px + 1, py + 1, px + pw - 1, py + 2, BEVEL_HI);
		g.renderOutline(px, py, pw, ph, PANEL_BORDER);
		g.renderOutline(px + 1, py + 1, pw - 2, ph - 2, PANEL_GLOW);

		// Header band with title + meta and a bright accent seam.
		g.fillGradient(px + 2, py + 2, px + pw - 2, py + 29, HEADER_TOP, HEADER_BOTTOM);
		g.fill(px + 2, py + 29, px + pw - 2, py + 31, ACCENT);
		g.fill(px + 2, py + 31, px + pw - 2, py + 32, 0x4D000000);
		g.drawString(font, ellipsize(snapshot.title(), pw - 280), x, py + 10, TEXT_MAIN, true);
		String meta = translated(snapshot.cultureKey()) + "  ·  " + translated(snapshot.relationshipKey());
		int metaW = font.width(meta);
		g.drawString(font, ellipsize(meta, 220), px + pw - 70 - metaW, py + 11, TEXT_MUTED, false);

		// Resource strip (hidden on the text-heavy Guide tab).
		int cursorY = py + 56;
		if (!"Guide".equals(selectedTab)) {
			cursorY = drawResourceStrip(g, x, cursorY, innerW) + 6;
		} else {
			cursorY = py + 60;
		}

		// Section heading, then the content body.
		int footerY = py + ph - 22;
		boolean rail = hasActionRail();
		int contentBottom = rail ? actionRailY() - 8 : footerY - 8;
		g.drawString(font, tabLabel(selectedTab).toUpperCase(java.util.Locale.ROOT), x, cursorY, ACCENT, false);
		g.fill(x, cursorY + 11, x + Math.min(innerW, 46), cursorY + 12, ACCENT_DIM);
		int contentTop = cursorY + 18;
		drawContent(g, x, contentTop, innerW, Math.max(20, contentBottom - contentTop));

		if (rail) {
			drawActionRailFrame(g, x - 2, actionRailY() - 6, innerW + 4, footerY - 4 - (actionRailY() - 6));
		}
		drawFooter(g, x, footerY, innerW);

		super.render(g, mouseX, mouseY, delta);
	}

	// =======================================================================
	// Resource strip
	// =======================================================================
	private int drawResourceStrip(GuiGraphics g, int x, int y, int width) {
		List<ColonyUiSnapshot.Metric> res = snapshot.resources();
		if (res.isEmpty()) {
			return y;
		}
		int gap = 6;
		// Prefer wider grids (more columns -> fewer rows) so the strip stays at most
		// two rows even at small GUI scales, leaving room for the content body.
		int columns = Math.max(4, Math.min(res.size(), (width + gap) / 116));
		int chipW = (width - (columns - 1) * gap) / columns;
		int rowH = 19;
		int rows = (res.size() + columns - 1) / columns;
		for (int i = 0; i < res.size(); i++) {
			ColonyUiSnapshot.Metric m = res.get(i);
			int cx = x + (i % columns) * (chipW + gap);
			int cy = y + (i / columns) * (rowH + 3);
			g.fillGradient(cx, cy, cx + chipW, cy + rowH, CHIP_TOP, CHIP_BOTTOM);
			g.fill(cx, cy, cx + chipW, cy + 1, BEVEL_HI);
			g.fill(cx, cy + rowH - 1, cx + chipW, cy + rowH, BEVEL_LO);
			g.renderOutline(cx, cy, chipW, rowH, CHIP_EDGE);
			drawItemIcon(g, itemForResourceId(m.id()), cx + 3, cy + 2);
			int barX = cx + 22;
			g.fill(barX, cy + 3, barX + 2, cy + rowH - 3, 0xFF000000 | m.color());
			String label = shortName(m.labelKey());
			String value = String.valueOf(m.value());
			g.drawString(font, ellipsize(label, chipW - 30 - font.width(value) - 4), barX + 6, cy + 6, TEXT_SOFT, false);
			g.drawString(font, value, cx + chipW - 6 - font.width(value), cy + 6, TEXT_MAIN, false);
		}
		return y + rows * (rowH + 3) - 3;
	}

	// =======================================================================
	// Content router
	// =======================================================================
	private void drawContent(GuiGraphics g, int x, int y, int width, int height) {
		switch (selectedTab) {
			case "Build" -> drawBuildings(g, x, y, width, height);
			case "Needs" -> drawRequests(g, x, y, width, height);
			case "Research" -> drawResearch(g, x, y, width, height);
			case "Trade" -> drawTrades(g, x, y, width, height);
			case "Instinct" -> drawInstinct(g, x, y, width, height);
			case "Guide" -> drawGuide(g, x, y, width, height);
			case "Relations" -> drawRelations(g, x, y, width, height);
			default -> drawOverview(g, x, y, width, height);
		}
	}

	private void drawOverview(GuiGraphics g, int x, int y, int width, int height) {
		// Identity banner.
		int relColor = 0xFF000000 | snapshot.relationshipColor();
		g.fillGradient(x, y, x + width, y + 38, ROW_TOP, ROW_BOTTOM);
		g.fill(x, y, x + width, y + 1, BEVEL_HI);
		g.fill(x, y + 37, x + width, y + 38, BEVEL_LO);
		g.renderOutline(x, y, width, 38, CARD_EDGE);
		g.fill(x, y, x + 3, y + 38, relColor);
		int mid = Math.max(150, width / 2);
		g.drawString(font, translated("formic_frontier.ui.personality"), x + 9, y + 5, TEXT_MUTED, false);
		g.drawString(font, translated("formic_frontier.ui.relationship"), x + mid, y + 5, TEXT_MUTED, false);
		g.drawString(font, ellipsize(translated(snapshot.personalityKey()) + " · " + translated(snapshot.personalityDetailKey()), mid - 18), x + 9, y + 16, TEXT_SOFT, false);
		g.drawString(font, ellipsize(translated("formic_frontier.ui.identity_rep", translated(snapshot.relationshipKey()), snapshot.reputation()), width - mid - 10), x + mid, y + 16, TEXT_SOFT, false);
		g.drawString(font, ellipsize(translated("formic_frontier.ui.current_goal") + ": " + snapshot.currentTask(), width - 18), x + 9, y + 27, ACCENT, false);

		int rowY = y + 46;
		int rowsRoom = Math.max(2, (height - 100) / 21);
		for (ColonyUiSnapshot.OverviewEntry entry : snapshot.overview().stream().limit(rowsRoom).toList()) {
			drawStatRow(g, x, rowY, width, translated(entry.labelKey()), entry.value(), entry.progress(), entry.color());
			rowY += 21;
		}
		// Population chips.
		if (y + height - rowY >= 36) {
			int popY = rowY + 8;
			g.drawString(font, translated("formic_frontier.ui.population"), x, popY, TEXT_MUTED, false);
			int cols = Math.min(4, Math.max(1, snapshot.population().size()));
			int gap = 6;
			int chipW = (width - (cols - 1) * gap) / Math.max(1, cols);
			for (int i = 0; i < snapshot.population().size() && i < cols; i++) {
				ColonyUiSnapshot.Metric m = snapshot.population().get(i);
				int cx = x + i * (chipW + gap);
				int cy = popY + 12;
				g.fillGradient(cx, cy, cx + chipW, cy + 16, CHIP_TOP, CHIP_BOTTOM);
				g.fill(cx, cy, cx + chipW, cy + 1, BEVEL_HI);
				g.fill(cx, cy, cx + 3, cy + 16, 0xFF000000 | m.color());
				g.renderOutline(cx, cy, chipW, 16, CHIP_EDGE);
				g.drawString(font, ellipsize(shortName(m.labelKey()) + " " + m.value(), chipW - 10), cx + 7, cy + 4, TEXT_SOFT, false);
			}
		}
	}

	private void drawBuildings(GuiGraphics g, int x, int y, int width, int height) {
		List<ColonyUiSnapshot.BuildingEntry> rows = snapshot.buildings().stream()
				.sorted(Comparator.comparing(ColonyUiSnapshot.BuildingEntry::complete).thenComparing(ColonyUiSnapshot.BuildingEntry::typeId))
				.limit(maxRows(height))
				.toList();
		int cardH = 30;
		int gap = 4;
		int cols = width >= 560 ? 2 : 1;
		int cardW = (width - (cols - 1) * 10) / cols;
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.BuildingEntry e = rows.get(i);
			int cx = x + (i % cols) * (cardW + 10);
			int cy = y + (i / cols) * (cardH + gap);
			int color = e.complete() ? 0x6DD08E : 0xD69042;
			drawCardSurface(g, cx, cy, cardW, cardH, color, CARD_EDGE);
			drawItemIcon(g, itemForBuildingId(e.typeId()), cx + 8, cy + 7);
			g.drawString(font, ellipsize(translated(e.labelKey()) + "  L" + e.level(), cardW - 38), cx + 30, cy + 5, TEXT_MAIN, false);
			g.drawString(font, ellipsize(translated(e.statusKey()) + " · " + e.progress() + "% " + e.detail(), cardW - 38), cx + 30, cy + 17, TEXT_MUTED, false);
			drawWideProgress(g, cx + 30, cy + cardH - 6, cardW - 38, e.progress(), color);
		}
	}

	private void drawRequests(GuiGraphics g, int x, int y, int width, int height) {
		List<ColonyUiSnapshot.RequestEntry> rows = snapshot.requests().stream()
				.filter(e -> e.fulfilled() < e.needed())
				.sorted((a, b) -> Integer.compare(b.needed() - b.fulfilled(), a.needed() - a.fulfilled()))
				.limit(cardLimit(height, 60))
				.toList();
		if (rows.isEmpty()) {
			drawInfoCard(g, x, y, Math.min(width, 420), 40, translated("formic_frontier.ui.no_requests"), translated("formic_frontier.ui.no_requests_detail"), Items.WRITABLE_BOOK, 0x6DD08E);
			return;
		}
		int cols = width >= 600 ? 2 : 1;
		int gap = 8;
		int cardW = (width - (cols - 1) * gap) / cols;
		int cardH = 54;
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.RequestEntry e = rows.get(i);
			int cx = x + (i % cols) * (cardW + gap);
			int cy = y + (i / cols) * (cardH + 6);
			if (i >= cols && cy + cardH > y + height) {
				break;
			}
			int color = colorForResource(e.resourceId());
			drawCardSurface(g, cx, cy, cardW, cardH, color, CARD_EDGE);
			drawItemIcon(g, itemForResourceId(e.resourceId()), cx + 9, cy + 8);
			drawItemIcon(g, itemForBuildingId(e.buildingId()), cx + cardW - 26, cy + 8);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.request.title", shortName(e.resourceKey()), requestBuildingName(e)), cardW - 78), cx + 32, cy + 7, TEXT_MAIN, false);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.request.delivery", e.deliveryItemCount() + " " + shortName(e.deliveryItemKey()), e.deliveryAmount(), shortName(e.resourceKey())), cardW - 44), cx + 32, cy + 20, TEXT_SOFT, false);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.request.reward", e.rewardTokens(), e.reputationDelta(), e.priority()), cardW - 44), cx + 32, cy + 33, TEXT_MUTED, false);
			drawWideProgress(g, cx + 9, cy + cardH - 8, cardW - 18, percent(e.fulfilled(), e.needed()), color);
		}
	}

	private void drawResearch(GuiGraphics g, int x, int y, int width, int height) {
		// Real prerequisite map: nodes grouped into tier columns by their recursive
		// prerequisite depth, connected by directed parent->child edges. Stacks are
		// top-aligned and sized to content so there are no empty filler bands.
		Map<String, ColonyUiSnapshot.ResearchEntry> byId = new HashMap<>();
		for (ColonyUiSnapshot.ResearchEntry e : snapshot.research()) {
			byId.put(e.nodeId(), e);
		}
		Map<String, Integer> tier = new HashMap<>();
		for (ResearchNode node : ResearchNode.values()) {
			tier.put(node.id(), researchTier(node, tier));
		}
		int maxTier = 0;
		for (int t : tier.values()) {
			maxTier = Math.max(maxTier, t);
		}
		int columns = Math.min(maxTier + 1, 2);
		int colGap = 58;
		int nodeW = Math.max(150, Math.min(220, (width - (columns - 1) * colGap) / columns));
		int nodeH = 50;
		int rowGap = 10;

		Map<Integer, List<ColonyUiSnapshot.ResearchEntry>> byTier = new HashMap<>();
		for (ColonyUiSnapshot.ResearchEntry e : snapshot.research()) {
			byTier.computeIfAbsent(tier.getOrDefault(e.nodeId(), 0), k -> new ArrayList<>()).add(e);
		}
		Comparator<ColonyUiSnapshot.ResearchEntry> order = Comparator
				.comparing(ColonyUiSnapshot.ResearchEntry::complete)
				.thenComparing(e -> !e.active())
				.thenComparing(e -> !e.startable())
				.thenComparing(ColonyUiSnapshot.ResearchEntry::nodeId);
		int rowCapacity = Math.max(1, (height - 16) / (nodeH + rowGap));

		Map<String, int[]> slot = new HashMap<>();
		String[] tierLabel = {"TIER I · ROOTS", "TIER II · ADVANCED"};
		for (int col = 0; col < columns; col++) {
			List<ColonyUiSnapshot.ResearchEntry> entries = byTier.getOrDefault(col, List.of())
					.stream().sorted(order).limit(rowCapacity).toList();
			int colX = x + col * (nodeW + colGap);
			g.drawString(font, tierLabel[Math.min(col, tierLabel.length - 1)], colX + 2, y, TEXT_MUTED, false);
			int top = y + 14;
			for (ColonyUiSnapshot.ResearchEntry e : entries) {
				slot.put(e.nodeId(), new int[] {colX, top});
				top += nodeH + rowGap;
			}
		}
		// Directed prerequisite edges first, beneath the node cards.
		for (ResearchNode child : ResearchNode.values()) {
			int[] cs = slot.get(child.id());
			if (cs == null) {
				continue;
			}
			for (String prereqId : child.prerequisites()) {
				int[] ps = slot.get(prereqId);
				if (ps == null) {
					continue;
				}
				ColonyUiSnapshot.ResearchEntry parent = byId.get(prereqId);
				drawResearchEdge(g, ps[0] + nodeW, ps[1] + nodeH / 2, cs[0], cs[1] + nodeH / 2, parent != null && parent.complete());
			}
		}
		for (Map.Entry<String, int[]> s : slot.entrySet()) {
			ColonyUiSnapshot.ResearchEntry e = byId.get(s.getKey());
			if (e == null) {
				continue;
			}
			int progress = e.complete() ? 100 : percent(e.progress(), e.duration());
			int color = e.active() ? 0xB58BFF : e.startable() ? 0x6DD08E : 0x8A6D47;
			drawResearchNode(g, s.getValue()[0], s.getValue()[1], nodeW, nodeH, e, progress, color);
		}
	}

	private void drawTrades(GuiGraphics g, int x, int y, int width, int height) {
		int rowY = y;
		if (!snapshot.tradeActivity().isBlank() && height >= 130) {
			drawInfoCard(g, x, rowY, width, 30, translated("formic_frontier.ui.trade"), snapshot.tradeActivity(), ModItems.PHEROMONE_TOKEN, 0xB58BFF);
			rowY += 36;
		}
		int gap = 8;
		int cardH = 52;
		int pitch = cardH + 8;
		// Compact fixed-width cards in a grid - never one stretched full-width card.
		int cols = Math.max(1, Math.min(3, (width + gap) / (232 + gap)));
		int cardW = (width - (cols - 1) * gap) / cols;
		int rowsFit = Math.max(1, (y + height - rowY + 8) / pitch);
		int limit = Math.min(cols * rowsFit, 6);
		List<ColonyUiSnapshot.TradeEntry> rows = tradeRowsForDisplay().stream().limit(limit).toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.TradeEntry e = rows.get(i);
			int cx = x + (i % cols) * (cardW + gap);
			int cy = rowY + (i / cols) * pitch;
			if (i >= cols && cy + cardH > y + height) {
				break;
			}
			drawTradeCard(g, cx, cy, cardW, e);
		}
	}

	private void drawInstinct(GuiGraphics g, int x, int y, int width, int height) {
		g.drawString(font, ellipsize(translated("formic_frontier.ui.instinct_detail"), width), x, y, TEXT_SOFT, false);
		int rowY = y + 16;
		for (int i = 0; i < snapshot.instinct().size() && i < maxRows(height) - 1; i++) {
			ColonyUiSnapshot.Metric m = snapshot.instinct().get(i);
			drawStatRow(g, x, rowY, width, translated(m.labelKey()), Component.translatable("formic_frontier.ui.priority", i + 1).getString(), percent(m.value(), m.max()), m.color());
			rowY += 22;
		}
	}

	private void drawGuide(GuiGraphics g, int x, int y, int width, int height) {
		int rowH = 19;
		List<ColonyUiSnapshot.GuideEntry> rows = snapshot.guide().stream()
				.limit(Math.max(1, height / rowH))
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.GuideEntry e = rows.get(i);
			int color = e.unlocked() ? e.color() : 0x8A6D47;
			int cy = y + i * rowH;
			g.fillGradient(x, cy, x + width, cy + rowH - 2, ROW_TOP, ROW_BOTTOM);
			g.fill(x, cy, x + width, cy + 1, BEVEL_HI);
			g.fill(x, cy, x + 3, cy + rowH - 2, 0xFF000000 | color);
			int titleW = Math.max(96, Math.min(150, width * 30 / 100));
			int stateW = 64;
			g.drawString(font, ellipsize(translated(e.titleKey()), titleW - 12), x + 8, cy + 5, TEXT_MAIN, false);
			g.drawString(font, ellipsize(translated(e.detailKey()), width - titleW - stateW - 14), x + titleW, cy + 5, TEXT_SOFT, false);
			String state = translated(e.unlocked() ? "formic_frontier.guide.state.open" : "formic_frontier.guide.state.locked");
			int pillColor = e.unlocked() ? 0xFF2E4A28 : 0xFF2A2017;
			g.fill(x + width - stateW, cy + 3, x + width - 4, cy + rowH - 5, pillColor);
			g.renderOutline(x + width - stateW, cy + 3, stateW - 4, rowH - 8, e.unlocked() ? 0xFF6DD08E : CHIP_EDGE);
			g.drawString(font, ellipsize(state, stateW - 12), x + width - stateW + 5, cy + 5, e.unlocked() ? 0xFFBFF0C7 : TEXT_MUTED, false);
		}
	}

	private void drawRelations(GuiGraphics g, int x, int y, int width, int height) {
		if (snapshot.relations().isEmpty()) {
			drawInfoCard(g, x, y, Math.min(width, 420), 36, translated("formic_frontier.ui.no_relations"), "", Items.PAPER, 0xC9974B);
			return;
		}
		int rowY = y;
		for (int i = 0; i < snapshot.relations().size() && i < Math.min(4, maxRows(height)); i++) {
			ColonyUiSnapshot.RelationEntry e = snapshot.relations().get(i);
			boolean sel = e.colonyId() == selectedDiplomacyTargetId;
			drawStatRow(g, x, rowY, width, (sel ? "▸ #" : "#") + e.colonyId(), translated(e.labelKey()), relationProgress(e.stateId()), 0xB58BFF);
			rowY += 22;
		}
		int infoY = rowY + 6;
		g.drawString(font, translated("formic_frontier.ui.selected_target") + " #" + selectedDiplomacyTargetId, x, infoY, ACCENT, false);
		for (int i = 0; i < snapshot.diplomacy().size() && i < 3; i++) {
			ColonyUiSnapshot.DiplomacyEntry e = snapshot.diplomacy().get(i);
			String cost = e.tokenCost() + "T " + e.dustCost() + "D " + e.sealCost() + "S";
			drawStatRow(g, x, infoY + 13 + i * 22, width, e.label(), cost + " · " + e.minRank(), 0, 0xB58BFF);
		}
	}

	// =======================================================================
	// Cards & rows
	// =======================================================================
	private void drawInfoCard(GuiGraphics g, int x, int y, int width, int height, String title, String detail, Item icon, int color) {
		drawCardSurface(g, x, y, width, height, color, CARD_EDGE);
		drawItemIcon(g, icon, x + 9, y + Math.max(3, (height - 16) / 2));
		g.drawString(font, ellipsize(title, width - 42), x + 32, y + 7, TEXT_MAIN, false);
		if (!detail.isBlank()) {
			g.drawString(font, ellipsize(detail, width - 42), x + 32, y + 19, TEXT_SOFT, false);
		}
	}

	private void drawResearchNode(GuiGraphics g, int x, int y, int width, int height, ColonyUiSnapshot.ResearchEntry e, int progress, int color) {
		int top = e.complete() ? 0xF03B2E1A : e.active() ? 0xF0352749 : CARD_TOP;
		int bottom = e.complete() ? 0xF01E160C : e.active() ? 0xF01B1330 : CARD_BOTTOM;
		g.fillGradient(x, y, x + width, y + height, top, bottom);
		g.fill(x, y, x + width, y + 1, BEVEL_HI);
		g.fill(x, y + height - 1, x + width, y + height, BEVEL_LO);
		g.renderOutline(x, y, width, height, e.startable() || e.active() ? ACCENT : CARD_EDGE);
		g.fill(x, y, x + width, y + 3, 0xFF000000 | color);
		g.fill(x, y, x + width, y + 1, 0x70FFFFFF);
		drawItemIcon(g, itemForResearch(e.nodeId()), x + 8, y + 9);
		String state = e.complete() ? "Open" : e.active() ? "Studying" : e.startable() ? "Ready" : "Locked";
		g.drawString(font, ellipsize(e.label(), width - 36), x + 30, y + 9, TEXT_MAIN, false);
		g.drawString(font, ellipsize(state + " · " + e.status(), width - 16), x + 8, y + 26, TEXT_MUTED, false);
		drawWideProgress(g, x + 8, y + height - 9, width - 16, progress, color);
	}

	private void drawResearchEdge(GuiGraphics g, int x1, int y1, int x2, int y2, boolean unlocked) {
		int color = unlocked ? 0xFF6FE08F : 0xFFD9A24A;
		int midX = (x1 + x2) / 2;
		g.fill(x1, y1 - 1, midX + 2, y1 + 2, color);
		g.fill(midX, Math.min(y1, y2), midX + 2, Math.max(y1, y2), color);
		g.fill(midX, y2 - 1, x2 - 5, y2 + 2, color);
		g.fill(x2 - 6, y2 - 3, x2 - 1, y2 + 4, color);
		g.fill(x2 - 4, y2 - 5, x2 - 1, y2 + 6, color);
	}

	private void drawTradeCard(GuiGraphics g, int x, int y, int width, ColonyUiSnapshot.TradeEntry e) {
		int color = e.available() ? 0x6DD08E : 0x8A6D47;
		drawCardSurface(g, x, y, width, 52, color, e.available() ? 0xFF8BCB86 : CARD_EDGE);
		int iconY = y + 7;
		drawItemIcon(g, itemForKey(e.inputKey()), x + 9, iconY);
		int arrowX1 = x + 28;
		int arrowX2 = x + 46;
		drawExchangeArrow(g, arrowX1, iconY + 8, arrowX2, e.available());
		drawItemIcon(g, itemForKey(e.outputKey()), x + 50, iconY);
		int textX = x + 72;
		g.drawString(font, ellipsize(shortName(e.inputKey()) + " → " + shortName(e.outputKey()), width - (textX - x) - 8), textX, y + 7, TEXT_MAIN, false);
		g.drawString(font, ellipsize(e.status(), width - (textX - x) - 8), textX, y + 19, e.available() ? TEXT_SOFT : TEXT_MUTED, false);
		String counts = e.inputCount() + "x  →  " + e.outputCount() + "x";
		g.drawString(font, counts, x + 9, y + 38, TEXT_SOFT, false);
	}

	private void drawExchangeArrow(GuiGraphics g, int x1, int y, int x2, boolean available) {
		int color = available ? 0xFF6FE08F : 0xFF9C8054;
		g.fill(x1, y - 1, x2 - 3, y + 2, color);
		g.fill(x2 - 4, y - 3, x2 - 1, y + 4, color);
		g.fill(x2 - 3, y - 2, x2, y + 3, color);
	}

	private void drawStatRow(GuiGraphics g, int x, int y, int width, String title, String detail, int progress, int color) {
		g.fillGradient(x, y, x + width, y + 19, ROW_TOP, ROW_BOTTOM);
		g.fill(x, y, x + width, y + 1, BEVEL_HI);
		g.fill(x, y + 18, x + width, y + 19, BEVEL_LO);
		g.fill(x, y, x + 3, y + 19, 0xFF000000 | color);
		g.fill(x, y, x + 3, y + 2, 0x6BFFFFFF);
		int titleW = Math.max(90, Math.min(170, width * 38 / 100));
		g.drawString(font, ellipsize(title, titleW - 14), x + 8, y + 6, TEXT_MAIN, false);
		g.drawString(font, ellipsize(detail, Math.max(40, width - titleW - 56)), x + titleW, y + 6, TEXT_SOFT, false);
		drawMiniProgress(g, x + width - 44, y + 8, 36, progress, color);
	}

	private void drawMiniProgress(GuiGraphics g, int x, int y, int width, int progress, int color) {
		g.fill(x, y, x + width, y + 4, TRACK_BG);
		g.renderOutline(x, y, width, 4, 0xFF0A0705);
		int c = Math.max(0, Math.min(100, progress));
		if (c <= 0) {
			return;
		}
		int fill = x + width * c / 100;
		g.fillGradient(x, y, fill, y + 4, brighten(color), 0xFF000000 | color);
		g.fill(x, y, fill, y + 1, 0x59FFFFFF);
	}

	private void drawWideProgress(GuiGraphics g, int x, int y, int width, int progress, int color) {
		g.fill(x, y, x + width, y + 5, TRACK_BG);
		g.renderOutline(x, y, width, 5, 0xFF0A0705);
		int c = Math.max(0, Math.min(100, progress));
		if (c > 0) {
			int fill = x + width * c / 100;
			g.fillGradient(x, y, fill, y + 5, brighten(color), 0xFF000000 | color);
			g.fill(x, y, fill, y + 1, 0x66FFFFFF);
		}
	}

	private void drawCardSurface(GuiGraphics g, int x, int y, int width, int height, int accent, int edge) {
		g.fillGradient(x, y, x + width, y + height, CARD_TOP, CARD_BOTTOM);
		g.fill(x, y, x + width, y + 1, BEVEL_HI);
		g.fill(x, y, x + 1, y + height, BEVEL_HI);
		g.fill(x, y + height - 1, x + width, y + height, BEVEL_LO);
		g.fill(x + width - 1, y, x + width, y + height, BEVEL_LO);
		g.renderOutline(x, y, width, height, edge);
		g.fill(x, y, x + 4, y + height, 0xFF000000 | accent);
		g.fill(x + 1, y, x + 4, y + Math.min(height, 3), 0x73FFFFFF);
	}

	private void drawActionRailFrame(GuiGraphics g, int x, int y, int width, int height) {
		if (height <= 0) {
			return;
		}
		g.fillGradient(x, y, x + width, y + height, 0xF0241A10, 0xF0130D07);
		g.fill(x, y, x + width, y + 1, 0x3EE0B05A);
		g.renderOutline(x, y, width, height, 0xFF503B23);
	}

	private void drawFooter(GuiGraphics g, int x, int y, int width) {
		g.drawString(font, Component.translatable("formic_frontier.ui.colony_id", snapshot.colonyId()).getString(), x, y, TEXT_MUTED, false);
		g.drawString(font, Component.translatable("formic_frontier.ui.reputation", snapshot.reputation()).getString(), x + 82, y, TEXT_SOFT, false);
		g.drawString(font, Component.translatable("formic_frontier.ui.claim", snapshot.claimRadius()).getString(), x + 150, y, TEXT_SOFT, false);
		if (!snapshot.feedbackMessage().isBlank()) {
			// Confirmation toast lives on the footer line itself, right-aligned, so it
			// never overlaps the content area or the action rail above it.
			String shown = ellipsize(snapshot.feedbackMessage(), Math.max(60, width - 230));
			int tw = font.width(shown);
			int fx = x + width - tw;
			g.fill(fx - 9, y - 2, x + width, y + 9, 0x66243A22);
			g.fill(fx - 9, y - 2, fx - 7, y + 9, 0xFF6DD08E);
			g.drawString(font, shown, fx, y, 0xFFBFF0C7, false);
		}
	}

	private void drawItemIcon(GuiGraphics g, Item item, int x, int y) {
		g.renderItem(new ItemStack(item), x, y);
	}

	private static int brighten(int rgb) {
		int r = Math.min(255, ((rgb >> 16) & 0xFF) + 70);
		int gg = Math.min(255, ((rgb >> 8) & 0xFF) + 70);
		int b = Math.min(255, (rgb & 0xFF) + 70);
		return 0xFF000000 | (r << 16) | (gg << 8) | b;
	}

	// =======================================================================
	// Action-rail buttons (themed)
	// =======================================================================
	private void addTradeButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
		int columns = Math.max(2, Math.min(5, (panelWidth() - 86) / 96));
		int buttonW = Math.max(76, Math.min(120, (panelWidth() - 24 - (columns - 1) * 5) / columns));
		List<ColonyUiSnapshot.TradeEntry> rows = tradeRowsForDisplay().stream().limit(5).toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.TradeEntry entry = rows.get(i);
			FormicButton button = new FormicButton(x + (i % columns) * (buttonW + 5), y + (i / columns) * 22, buttonW, 19,
					Component.literal(tradeButtonLabel(entry)), () -> ClientPlayNetworking.send(new TradeRequestPayload(entry.offerId())), ButtonStyle.ACTION);
			button.active = entry.available();
			addRenderableWidget(button);
		}
	}

	private void addContractButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
		List<ColonyUiSnapshot.RequestEntry> rows = snapshot.requests().stream()
				.filter(entry -> entry.fulfilled() < entry.needed())
				.limit(4)
				.toList();
		int columns = Math.max(2, Math.min(4, (panelWidth() - 86) / 150));
		int buttonW = Math.max(110, Math.min(180, (panelWidth() - 24 - (columns - 1) * 5) / columns));
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.RequestEntry entry = rows.get(i);
			FormicButton button = new FormicButton(x + (i % columns) * (buttonW + 5), y + (i / columns) * 22, buttonW, 19,
					Component.literal(translated("formic_frontier.ui.request.help", requestBuildingName(entry))), () -> ClientPlayNetworking.send(new ContractRequestPayload(entry.contractId())), ButtonStyle.ACTION);
			button.active = !entry.contractId().isBlank();
			addRenderableWidget(button);
		}
	}

	private void addResearchButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
		int columns = Math.max(2, Math.min(4, (panelWidth() - 86) / 110));
		int buttonW = Math.max(96, Math.min(130, (panelWidth() - 24 - (columns - 1) * 5) / columns));
		List<ColonyUiSnapshot.ResearchEntry> rows = snapshot.research().stream()
				.filter(entry -> !entry.complete())
				.sorted(Comparator.comparing(ColonyUiSnapshot.ResearchEntry::startable).reversed().thenComparing(ColonyUiSnapshot.ResearchEntry::nodeId))
				.limit(4)
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.ResearchEntry entry = rows.get(i);
			FormicButton button = new FormicButton(x + (i % columns) * (buttonW + 5), y + (i / columns) * 22, buttonW, 19,
					Component.literal(entry.label()), () -> ClientPlayNetworking.send(new ResearchRequestPayload(entry.nodeId())), ButtonStyle.ACTION);
			button.active = entry.startable();
			addRenderableWidget(button);
		}
	}

	private void addInstinctButtons() {
		int x = panelX() + 12;
		int y = actionRailY() + 11;
		String[] ids = {"food", "ore", "chitin", "defense"};
		int buttonW = Math.max(64, Math.min(110, (panelWidth() - 24 - (ids.length - 1) * 5) / ids.length));
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			addRenderableWidget(new FormicButton(x + i * (buttonW + 5), y, buttonW, 19,
					Component.translatable("formic_frontier.instinct." + id), () -> ClientPlayNetworking.send(new PriorityRequestPayload(id)), ButtonStyle.ACTION));
		}
	}

	private void addRelationsButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
		if (selectedDiplomacyTargetId <= 0 && !snapshot.relations().isEmpty()) {
			selectedDiplomacyTargetId = snapshot.relations().getFirst().colonyId();
		}
		for (int i = 0; i < snapshot.relations().size() && i < 5; i++) {
			ColonyUiSnapshot.RelationEntry entry = snapshot.relations().get(i);
			FormicButton button = new FormicButton(x + i * 46, y, 42, 19,
					Component.literal("#" + entry.colonyId()), () -> {
						selectedDiplomacyTargetId = entry.colonyId();
						rebuildWidgets();
					}, ButtonStyle.TAB);
			button.selected = selectedDiplomacyTargetId == entry.colonyId();
			addRenderableWidget(button);
		}
		int actionW = Math.max(86, Math.min(130, (panelWidth() - 24 - 8) / 3));
		for (int i = 0; i < snapshot.diplomacy().size() && i < 3; i++) {
			ColonyUiSnapshot.DiplomacyEntry entry = snapshot.diplomacy().get(i);
			addRenderableWidget(new FormicButton(x + i * (actionW + 5), y + 22, actionW, 19,
					Component.literal(entry.label()), () -> ClientPlayNetworking.send(new DiplomacyRequestPayload(entry.actionId(), selectedDiplomacyTargetId)), ButtonStyle.ACTION));
		}
	}

	// =======================================================================
	// Themed button
	// =======================================================================
	private enum ButtonStyle {
		TAB, ACTION
	}

	private final class FormicButton extends AbstractWidget {
		private final ButtonStyle style;
		private final Runnable onPress;
		private boolean selected;

		private FormicButton(int x, int y, int w, int h, Component msg, Runnable onPress, ButtonStyle style) {
			super(x, y, w, h, msg);
			this.onPress = onPress;
			this.style = style;
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (active) {
				onPress.run();
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			int x = getX();
			int y = getY();
			int w = this.width;
			int h = this.height;
			boolean hovered = isHoveredOrFocused() && active;
			int top;
			int bottom;
			int border;
			int textColor;
			if (selected) {
				top = 0xFF7A521F;
				bottom = 0xFF49300F;
				border = ACCENT;
				textColor = 0xFFFFF3DA;
			} else if (!active) {
				top = 0xFF231A12;
				bottom = 0xFF170F09;
				border = 0xFF38291A;
				textColor = TEXT_FAINT;
			} else if (hovered) {
				top = 0xFF4E3923;
				bottom = 0xFF2C2014;
				border = 0xFFF1C674;
				textColor = 0xFFFFF3DA;
			} else {
				top = 0xFF3A2A1A;
				bottom = 0xFF241A10;
				border = 0xFF5E4528;
				textColor = TEXT_MAIN;
			}
			g.fillGradient(x, y, x + w, y + h, top, bottom);
			g.fill(x, y, x + w, y + 1, BEVEL_HI);
			g.fill(x, y + h - 1, x + w, y + h, BEVEL_LO);
			g.renderOutline(x, y, w, h, border);
			if (selected && style == ButtonStyle.TAB) {
				g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, ACCENT);
			}
			String label = ellipsize(getMessage().getString(), w - 8);
			g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, textColor);
		}
	}

	// =======================================================================
	// Data helpers (unchanged behaviour)
	// =======================================================================
	private List<ColonyUiSnapshot.TradeEntry> tradeRowsForDisplay() {
		return snapshot.trades().stream()
				.sorted(Comparator.comparingInt(this::tradeDisplayPriority).thenComparing(ColonyUiSnapshot.TradeEntry::offerId))
				.toList();
	}

	private int tradeDisplayPriority(ColonyUiSnapshot.TradeEntry entry) {
		if (entry.offerId().equals("sell_wheat")) {
			return 0;
		}
		if (entry.offerId().equals("buy_colony_seal")) {
			return 1;
		}
		if (entry.available() && entry.status().startsWith("Trade Hub")) {
			return 2;
		}
		return entry.available() ? 3 : 4;
	}

	private String tradeButtonLabel(ColonyUiSnapshot.TradeEntry entry) {
		String offerId = entry.offerId();
		String verb = offerId.startsWith("sell_") ? "Sell " : "Buy ";
		String noun = switch (offerId) {
			case "sell_wheat", "sell_biomass" -> "Food";
			case "sell_raw_iron", "sell_iron_ore" -> "Ore";
			case "sell_chitin" -> "Chitin";
			case "sell_resin" -> "Resin";
			case "sell_fungus" -> "Fungus";
			case "sell_venom" -> "Venom";
			case "sell_royal_jelly" -> "Jelly";
			case "buy_colony_seal" -> "Seal";
			case "buy_war_banner" -> "Banner";
			case "buy_chitin_boots", "buy_resin_chitin_boots" -> "Boots";
			case "buy_chitin_helmet", "buy_resin_chitin_helmet" -> "Helmet";
			case "buy_chitin_leggings", "buy_resin_chitin_leggings" -> "Legs";
			case "buy_chitin_chestplate", "buy_resin_chitin_chestplate" -> "Chest";
			case "buy_mandible_saber" -> "Saber";
			case "buy_venom_spear" -> "Spear";
			case "buy_queen_egg" -> "Queen Egg";
			case "buy_chitin_spore" -> "Spore";
			case "buy_pheromone_dust" -> "Dust";
			case "buy_resin_glob" -> "Resin";
			case "buy_fungus_culture" -> "Fungus";
			case "buy_venom_sac" -> "Venom";
			default -> shortName(entry.outputKey());
		};
		return verb + noun;
	}

	private static int researchTier(ResearchNode node, Map<String, Integer> cache) {
		Integer cached = cache.get(node.id());
		if (cached != null) {
			return cached;
		}
		if (node.prerequisites().isEmpty()) {
			cache.put(node.id(), 0);
			return 0;
		}
		int depth = 0;
		for (String prereqId : node.prerequisites()) {
			try {
				depth = Math.max(depth, researchTier(ResearchNode.fromId(prereqId), cache) + 1);
			} catch (IllegalArgumentException ignored) {
				// Unknown prerequisite id should not happen; never break the UI over it.
			}
		}
		cache.put(node.id(), depth);
		return depth;
	}

	private int percent(int value, int max) {
		if (max <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(100, value * 100 / max));
	}

	private int relationProgress(String stateId) {
		return switch (stateId) {
			case "ally" -> 100;
			case "neutral" -> 60;
			case "rival" -> 30;
			case "war" -> 10;
			default -> 0;
		};
	}

	private int maxRows(int height) {
		return Math.max(4, Math.min(10, height / 21));
	}

	private int cardLimit(int height, int cardHeight) {
		return Math.max(2, Math.min(8, height / Math.max(1, cardHeight)));
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

	private String translated(String key, Object... args) {
		return Component.translatable(key, args).getString();
	}

	private String shortName(String key) {
		String value = translated(key);
		int space = value.indexOf(' ');
		return space > 0 && value.length() > 12 ? value.substring(0, space) : value;
	}

	private String requestBuildingName(ColonyUiSnapshot.RequestEntry entry) {
		return shortName(entry.buildingKey());
	}

	private int colorForResource(String id) {
		return switch (id) {
			case "food" -> 0x91C46C;
			case "ore" -> 0xB9B8AC;
			case "chitin" -> 0xD6B16E;
			case "resin" -> 0xD69042;
			case "fungus" -> 0x9BC76C;
			case "venom" -> 0x7DD66C;
			case "knowledge" -> 0xB58BFF;
			default -> 0xC9974B;
		};
	}

	private Item itemForResourceId(String id) {
		return switch (id) {
			case "food" -> Items.WHEAT;
			case "ore" -> Items.RAW_IRON;
			case "chitin" -> ModItems.CHITIN_SHARD;
			case "resin" -> ModItems.RESIN_GLOB;
			case "fungus" -> ModItems.FUNGUS_CULTURE;
			case "venom" -> ModItems.VENOM_SAC;
			case "knowledge" -> ModItems.PHEROMONE_DUST;
			default -> Items.PAPER;
		};
	}

	private Item itemForBuildingId(String id) {
		return switch (id) {
			case "food_store" -> Items.HAY_BLOCK;
			case "nursery", "chitin_farm" -> ModItems.CHITIN_SHARD;
			case "mine" -> Items.IRON_ORE;
			case "barracks", "armory", "watch_post" -> Items.BONE;
			case "market", "trade_hub" -> Items.BELL;
			case "resin_depot" -> ModItems.RESIN_GLOB;
			case "pheromone_archive", "diplomacy_shrine" -> ModItems.PHEROMONE_DUST;
			case "fungus_garden" -> ModItems.FUNGUS_CULTURE;
			case "venom_press" -> ModItems.VENOM_SAC;
			case "queen_chamber", "great_mound", "queen_vault" -> ModItems.QUEEN_EGG;
			default -> ModItems.COLONY_TABLET;
		};
	}

	private Item itemForResearch(String nodeId) {
		if (nodeId.contains("fungus")) {
			return ModItems.FUNGUS_CULTURE;
		}
		if (nodeId.contains("venom")) {
			return ModItems.VENOM_SAC;
		}
		if (nodeId.contains("mandible") || nodeId.contains("chitin")) {
			return ModItems.CHITIN_PLATE;
		}
		if (nodeId.contains("trade") || nodeId.contains("diplomacy")) {
			return ModItems.COLONY_SEAL;
		}
		if (nodeId.contains("resin")) {
			return ModItems.RESIN_GLOB;
		}
		return ModItems.PHEROMONE_DUST;
	}

	private Item itemForKey(String key) {
		return switch (key) {
			case "item.minecraft.wheat" -> Items.WHEAT;
			case "item.minecraft.raw_iron" -> Items.RAW_IRON;
			case "block.minecraft.iron_ore" -> Items.IRON_ORE;
			case "item.formic_frontier.chitin_shard" -> ModItems.CHITIN_SHARD;
			case "item.formic_frontier.raw_biomass" -> ModItems.RAW_BIOMASS;
			case "item.formic_frontier.resin_glob" -> ModItems.RESIN_GLOB;
			case "item.formic_frontier.fungus_culture" -> ModItems.FUNGUS_CULTURE;
			case "item.formic_frontier.venom_sac" -> ModItems.VENOM_SAC;
			case "item.formic_frontier.royal_jelly" -> ModItems.ROYAL_JELLY;
			case "item.formic_frontier.pheromone_token" -> ModItems.PHEROMONE_TOKEN;
			case "item.formic_frontier.pheromone_dust" -> ModItems.PHEROMONE_DUST;
			case "item.formic_frontier.colony_seal" -> ModItems.COLONY_SEAL;
			case "item.formic_frontier.war_banner" -> ModItems.WAR_BANNER;
			case "item.formic_frontier.chitin_spore" -> ModItems.CHITIN_SPORE;
			case "item.formic_frontier.chitin_boots" -> ModItems.CHITIN_BOOTS;
			case "item.formic_frontier.chitin_helmet" -> ModItems.CHITIN_HELMET;
			case "item.formic_frontier.chitin_leggings" -> ModItems.CHITIN_LEGGINGS;
			case "item.formic_frontier.chitin_chestplate" -> ModItems.CHITIN_CHESTPLATE;
			case "item.formic_frontier.resin_chitin_boots" -> ModItems.RESIN_CHITIN_BOOTS;
			case "item.formic_frontier.resin_chitin_helmet" -> ModItems.RESIN_CHITIN_HELMET;
			case "item.formic_frontier.resin_chitin_leggings" -> ModItems.RESIN_CHITIN_LEGGINGS;
			case "item.formic_frontier.resin_chitin_chestplate" -> ModItems.RESIN_CHITIN_CHESTPLATE;
			case "item.formic_frontier.mandible_saber" -> ModItems.MANDIBLE_SABER;
			case "item.formic_frontier.venom_spear" -> ModItems.VENOM_SPEAR;
			case "item.formic_frontier.queen_egg" -> ModItems.QUEEN_EGG;
			default -> Items.PAPER;
		};
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
			case "Research", "Trade", "Instinct", "Guide" -> id;
			default -> "Overview";
		};
	}

	private int panelWidth() {
		return Math.min(Math.max(420, width - 24), Math.max(640, (int) (width * 0.88f)));
	}

	private int panelHeight() {
		return Math.min(Math.max(308, height - 20), Math.max(360, (int) (height * 0.93f)));
	}

	private int panelX() {
		return (width - panelWidth()) / 2;
	}

	private int panelY() {
		return (height - panelHeight()) / 2;
	}

	private boolean hasActionRail() {
		return switch (selectedTab) {
			case "Trade", "Needs", "Research", "Instinct", "Relations" -> true;
			default -> false;
		};
	}

	private int actionRailY() {
		return panelY() + panelHeight() - 76;
	}

	private record Tab(String id, String shortKey, String titleKey) {
	}
}
