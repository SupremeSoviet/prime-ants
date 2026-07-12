package com.formicfrontier.client.screen;

import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.network.ContractRequestPayload;
import com.formicfrontier.network.DiplomacyRequestPayload;
import com.formicfrontier.network.PriorityRequestPayload;
import com.formicfrontier.network.ResearchRequestPayload;
import com.formicfrontier.network.TradeRequestPayload;
import com.formicfrontier.registry.ModItems;
import com.formicfrontier.sim.ResearchNode;
import com.formicfrontier.sim.ResourceType;
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

/**
 * Modern translucent colony workspace. A persistent section rail separates
 * navigation from the content without hiding the world behind an opaque slab;
 * research remains a pannable prerequisite canvas and trade offers are acted
 * on directly. Warm chitin amber is reserved for focus and colony identity,
 * while quiet graphite glass carries the information hierarchy.
 */
public final class ColonyStatusScreen extends Screen {
	private static final Tab[] TABS = {
		new Tab("Overview", "formic_frontier.ui.tab.overview", 0xD6A253),
		new Tab("Build", "formic_frontier.ui.tab.buildings", 0xC68A54),
		new Tab("Needs", "formic_frontier.ui.tab.requests", 0xD9C36A),
		new Tab("Research", "formic_frontier.ui.tab.research", 0xA884E8),
		new Tab("Trade", "formic_frontier.ui.tab.trade", 0x77C891),
		new Tab("Instinct", "formic_frontier.ui.tab.instinct", 0xD17954),
		new Tab("Guide", "formic_frontier.ui.tab.guide", 0x78A9C8),
		new Tab("Relations", "formic_frontier.ui.tab.relations", 0xB78AD6)
	};

	// --- Palette -----------------------------------------------------------
	private static final int SCRIM_TOP = 0x34060A0C;
	private static final int SCRIM_BOTTOM = 0x50030709;
	private static final int PANEL_TOP = 0xB51A2022;
	private static final int PANEL_BOTTOM = 0xC00D1214;
	private static final int PANEL_BORDER = 0xA56F817C;
	private static final int PANEL_GLOW = 0x3286CDBB;
	private static final int HEADER_TOP = 0x8C2B3436;
	private static final int HEADER_BOTTOM = 0x781A2022;
	private static final int ACCENT = 0xFFFFC56B;
	private static final int ACCENT_DIM = 0xFF9D7845;
	private static final int CARD_TOP = 0xB72B3335;
	private static final int CARD_BOTTOM = 0xC0181E20;
	private static final int CARD_EDGE = 0x82798A85;
	private static final int CHIP_TOP = 0x9E30383A;
	private static final int CHIP_BOTTOM = 0xA91B2224;
	private static final int CHIP_EDGE = 0x72748480;
	private static final int ROW_TOP = 0x9F2A3133;
	private static final int ROW_BOTTOM = 0xAD191F21;
	private static final int BEVEL_HI = 0x20FFFFFF;
	private static final int TRACK_BG = 0xA80B1012;
	private static final int TEXT_MAIN = 0xFFF7EBD8;
	private static final int TEXT_SOFT = 0xFFE1D8C8;
	private static final int TEXT_MUTED = 0xFFC1AE8D;
	private static final int TEXT_FAINT = 0xFF8E9893;
	private static final int NAV_TOP = 0x66283133;
	private static final int NAV_BOTTOM = 0x761A2022;
	private static final int VIEWPORT_TOP = 0xA312191B;
	private static final int VIEWPORT_BOTTOM = 0xB40A0F11;
	private static final int RESEARCH_CANVAS_HEIGHT = 292;

	private final ColonyUiSnapshot snapshot;
	private final List<ResearchHitbox> researchHitboxes = new ArrayList<>();
	private final List<TradeHitbox> tradeHitboxes = new ArrayList<>();
	private final List<RequestHitbox> requestHitboxes = new ArrayList<>();
	private String selectedTab;
	private int selectedDiplomacyTargetId;
	private int researchPanX = 16;
	private int researchPanY = 12;
	private boolean researchDragging;
	private int tradeScroll;
	private int tradeMaxScroll;
	private int contentViewportX;
	private int contentViewportY;
	private int contentViewportWidth;
	private int contentViewportHeight;
	private int currentMouseX;
	private int currentMouseY;
	private ColonyUiSnapshot.ResearchEntry hoveredResearch;
	private ColonyUiSnapshot.TradeEntry hoveredTrade;

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
		int tabW = navigationWidth() - 14;
		int tabX = px + 7;
		int tabY = py + 62;
		for (Tab tab : TABS) {
			boolean isActive = tab.id().equals(selectedTab);
			String navigationKey = "Research".equals(tab.id())
					? "formic_frontier.ui.tab.research_short"
					: tab.titleKey();
			FormicButton button = new FormicButton(tabX, tabY, tabW, 22,
					Component.translatable(navigationKey),
					() -> {
						selectedTab = tab.id();
						researchDragging = false;
						rebuildWidgets();
					}, ButtonStyle.TAB);
			button.selected = isActive;
			button.accent = tab.color();
			addRenderableWidget(button);
			tabY += 24;
		}

		addRenderableWidget(new FormicButton(px + pw - 30, py + 8, 20, 18,
				Component.literal("X"), () -> onClose(), ButtonStyle.ACTION));

		switch (selectedTab) {
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
		currentMouseX = mouseX;
		currentMouseY = mouseY;
		hoveredResearch = null;
		hoveredTrade = null;
		researchHitboxes.clear();
		tradeHitboxes.clear();
		requestHitboxes.clear();
		g.fillGradient(0, 0, width, height, SCRIM_TOP, SCRIM_BOTTOM);

		int px = panelX();
		int py = panelY();
		int pw = panelWidth();
		int ph = panelHeight();
		int x = mainContentX();
		int innerW = mainContentWidth();

		// One soft shadow and a cut-corner glass surface leave the world legible.
		fillCutRect(g, px + 6, py + 8, pw, ph, 0x3D000000);
		fillCutGradient(g, px, py, pw, ph, PANEL_TOP, PANEL_BOTTOM);
		outlineCutRect(g, px, py, pw, ph, PANEL_BORDER);
		outlineCutRect(g, px + 1, py + 1, pw - 2, ph - 2, PANEL_GLOW);
		g.fill(px + 4, py + 1, px + pw - 4, py + 2, BEVEL_HI);

		// Compact identity bar: a small colony mark, title and one quiet meta pill.
		fillCutGradient(g, px + 3, py + 3, pw - 6, 30, HEADER_TOP, HEADER_BOTTOM);
		g.fill(px + 4, py + 32, px + pw - 4, py + 33, 0x4AFFFFFF);
		fillCutRect(g, px + 10, py + 8, 18, 18, 0x8A8B5B29);
		outlineCutRect(g, px + 10, py + 8, 18, 18, 0xB8FFC56B);
		g.fill(px + 18, py + 11, px + 20, py + 23, ACCENT);
		g.fill(px + 14, py + 14, px + 24, py + 16, ACCENT);
		g.fill(px + 15, py + 20, px + 23, py + 22, 0xFFC68B3E);
		g.drawString(font, ellipsize(snapshot.title(), Math.max(120, pw - 390)), px + 36, py + 12, TEXT_MAIN, true);
		String meta = translated(snapshot.cultureKey()) + "  ·  " + translated(snapshot.relationshipKey());
		String shownMeta = ellipsize(meta, 204);
		int metaW = font.width(shownMeta);
		int metaX = Math.max(px + pw / 2, px + pw - 42 - metaW - 14);
		fillCutRect(g, metaX, py + 9, metaW + 12, 16, 0x682E3839);
		outlineCutRect(g, metaX, py + 9, metaW + 12, 16, 0x547F918B);
		g.drawString(font, shownMeta, metaX + 6, py + 13, TEXT_MUTED, false);

		int footerY = py + ph - 22;
		int navX = px + 5;
		int navY = py + 41;
		int navW = navigationWidth() - 10;
		fillCutGradient(g, navX, navY, navW, footerY - 7 - navY, NAV_TOP, NAV_BOTTOM);
		outlineCutRect(g, navX, navY, navW, footerY - 7 - navY, 0x50748480);
		g.drawString(font, translated("formic_frontier.ui.navigation").toUpperCase(java.util.Locale.ROOT), navX + 6, navY + 7, TEXT_FAINT, false);
		g.fill(navX + 6, navY + 18, navX + navW - 6, navY + 19, 0x457F918B);

		// Context-heavy workspaces keep their own focused information instead of
		// repeating the global stock ledger above every interaction.
		int cursorY = py + 42;
		if (showsResourceStrip()) {
			cursorY = drawResourceStrip(g, x, cursorY, innerW) + 6;
		} else {
			cursorY = py + 45;
		}

		// Section heading, then the content body.
		boolean rail = hasActionRail();
		int contentBottom = rail ? actionRailY() - 8 : footerY - 8;
		g.drawString(font, tabLabel(selectedTab), x, cursorY, TEXT_MAIN, true);
		g.fill(x, cursorY + 12, x + Math.min(innerW, 38), cursorY + 13, ACCENT);
		int contentTop = cursorY + 18;
		contentViewportX = x;
		contentViewportY = contentTop;
		contentViewportWidth = innerW;
		contentViewportHeight = Math.max(20, contentBottom - contentTop);
		drawContent(g, x, contentTop, innerW, Math.max(20, contentBottom - contentTop));

		if (rail) {
			drawActionRailFrame(g, x - 2, actionRailY() - 6, innerW + 4, footerY - 4 - (actionRailY() - 6));
		}
		drawFooter(g, x, footerY, innerW);

		super.render(g, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mouseX = (int) event.x();
		int mouseY = (int) event.y();
		if ("Research".equals(selectedTab) && pointInsideViewport(mouseX, mouseY)) {
			if (event.button() == 1) {
				researchPanX = 16;
				researchPanY = 12;
				return true;
			}
			if (event.button() == 0) {
				for (ResearchHitbox hitbox : researchHitboxes) {
					if (hitbox.contains(mouseX, mouseY)) {
						if (hitbox.entry().startable()) {
							ClientPlayNetworking.send(new ResearchRequestPayload(hitbox.entry().nodeId()));
						}
						return true;
					}
				}
				researchDragging = true;
				return true;
			}
		}
		if ("Trade".equals(selectedTab) && event.button() == 0 && pointInsideViewport(mouseX, mouseY)) {
			for (TradeHitbox hitbox : tradeHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					if (hitbox.entry().available()) {
						ClientPlayNetworking.send(new TradeRequestPayload(hitbox.entry().offerId()));
					}
					return true;
				}
			}
		}
		if ("Needs".equals(selectedTab) && event.button() == 0 && pointInsideViewport(mouseX, mouseY)) {
			for (RequestHitbox hitbox : requestHitboxes) {
				if (hitbox.contains(mouseX, mouseY) && !hitbox.entry().contractId().isBlank()) {
					ClientPlayNetworking.send(new ContractRequestPayload(hitbox.entry().contractId()));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (researchDragging && "Research".equals(selectedTab) && event.button() == 0) {
			researchPanX += (int) Math.round(dragX);
			researchPanY += (int) Math.round(dragY);
			clampResearchPan(contentViewportWidth, contentViewportHeight);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (researchDragging && event.button() == 0) {
			researchDragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!pointInsideViewport((int) mouseX, (int) mouseY)) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
		if ("Research".equals(selectedTab)) {
			researchPanX += (int) Math.round(horizontalAmount * 22.0);
			researchPanY += (int) Math.round(verticalAmount * 22.0);
			clampResearchPan(contentViewportWidth, contentViewportHeight);
			return true;
		}
		if ("Trade".equals(selectedTab) && verticalAmount != 0.0) {
			tradeScroll = Math.max(0, Math.min(tradeMaxScroll, tradeScroll - (int) Math.signum(verticalAmount)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
			fillCutGradient(g, cx, cy, chipW, rowH, CHIP_TOP, CHIP_BOTTOM);
			outlineCutRect(g, cx, cy, chipW, rowH, CHIP_EDGE);
			drawItemIcon(g, itemForResourceId(m.id()), cx + 3, cy + 2);
			int barX = cx + 22;
			g.fill(barX, cy + 3, barX + 2, cy + rowH - 3, 0xFF000000 | m.color());
			String label = "knowledge".equals(m.id())
					? translated("formic_frontier.ui.resource.knowledge_short")
					: shortName(m.labelKey());
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
			case "Research" -> drawResearchGraph(g, x, y, width, height);
			case "Trade" -> drawTradeExchange(g, x, y, width, height);
			case "Instinct" -> drawInstinct(g, x, y, width, height);
			case "Guide" -> drawGuide(g, x, y, width, height);
			case "Relations" -> drawRelations(g, x, y, width, height);
			default -> drawOverview(g, x, y, width, height);
		}
	}

	private void drawOverview(GuiGraphics g, int x, int y, int width, int height) {
		// Identity banner.
		int relColor = 0xFF000000 | snapshot.relationshipColor();
		fillCutGradient(g, x, y, width, 38, ROW_TOP, ROW_BOTTOM);
		outlineCutRect(g, x, y, width, 38, CARD_EDGE);
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
				fillCutGradient(g, cx, cy, chipW, 16, CHIP_TOP, CHIP_BOTTOM);
				g.fill(cx, cy, cx + 3, cy + 16, 0xFF000000 | m.color());
				outlineCutRect(g, cx, cy, chipW, 16, CHIP_EDGE);
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
			RequestHitbox hitbox = new RequestHitbox(e, cx, cy, cardW, cardH);
			requestHitboxes.add(hitbox);
			boolean hovered = hitbox.contains(currentMouseX, currentMouseY) && pointInsideViewport(currentMouseX, currentMouseY);
			drawCardSurface(g, cx, cy, cardW, cardH, color, hovered ? brighten(color) : CARD_EDGE);
			drawItemIcon(g, itemForResourceId(e.resourceId()), cx + 9, cy + 8);
			drawItemIcon(g, itemForBuildingId(e.buildingId()), cx + cardW - 26, cy + 8);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.request.title", shortName(e.resourceKey()), requestBuildingName(e)), cardW - 78), cx + 32, cy + 7, TEXT_MAIN, false);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.request.delivery", e.deliveryItemCount() + " " + shortName(e.deliveryItemKey()), e.deliveryAmount(), shortName(e.resourceKey())), cardW - 106), cx + 32, cy + 20, TEXT_SOFT, false);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.request.reward", e.rewardTokens(), e.reputationDelta(), e.priority()), cardW - 106), cx + 32, cy + 33, TEXT_MUTED, false);
			int pillW = 58;
			int pillX = cx + cardW - pillW - 9;
			fillCutGradient(g, pillX, cy + 31, pillW, 14, 0xA94B3B1E, 0xB52D2112);
			outlineCutRect(g, pillX, cy + 31, pillW, 14, hovered ? ACCENT : 0xA58E6B33);
			g.drawCenteredString(font, translated("formic_frontier.ui.request.help_action"), pillX + pillW / 2, cy + 34, hovered ? 0xFFFFF0C6 : TEXT_MAIN);
			drawWideProgress(g, cx + 9, cy + cardH - 8, cardW - 18, percent(e.fulfilled(), e.needed()), color);
		}
	}

	private void drawResearchGraph(GuiGraphics g, int x, int y, int width, int height) {
		int viewX = x;
		int viewY = y;
		int viewW = width;
		int viewH = height;
		clampResearchPan(viewW, viewH);

		fillCutGradient(g, viewX, viewY, viewW, viewH, VIEWPORT_TOP, VIEWPORT_BOTTOM);
		outlineCutRect(g, viewX, viewY, viewW, viewH, CHIP_EDGE);
		g.enableScissor(viewX + 1, viewY + 1, viewX + viewW - 1, viewY + viewH - 1);

		// A sparse pheromone-grid gives the graph a sense of navigable space without
		// competing with icons and labels.
		for (int gx = researchPanX % 32; gx < viewW; gx += 32) {
			g.fill(viewX + gx, viewY, viewX + gx + 1, viewY + viewH, 0x182F281E);
		}
		for (int gy = researchPanY % 32; gy < viewH; gy += 32) {
			g.fill(viewX, viewY + gy, viewX + viewW, viewY + gy + 1, 0x182F281E);
		}

		Map<String, ColonyUiSnapshot.ResearchEntry> byId = new HashMap<>();
		for (ColonyUiSnapshot.ResearchEntry entry : snapshot.research()) {
			byId.put(entry.nodeId(), entry);
		}
		int nodeW = Math.max(142, Math.min(184, (viewW - 88) / 2));
		int nodeH = 48;
		int advancedX = viewW - nodeW - 18;
		for (ResearchNode child : ResearchNode.values()) {
			ResearchCanvasPoint childPoint = researchCanvasPoint(child.id(), advancedX);
			for (String prerequisiteId : child.prerequisites()) {
				ResearchCanvasPoint parentPoint = researchCanvasPoint(prerequisiteId, advancedX);
				ColonyUiSnapshot.ResearchEntry parent = byId.get(prerequisiteId);
				int x1 = viewX + researchPanX + parentPoint.x() + nodeW;
				int y1 = viewY + researchPanY + parentPoint.y() + nodeH / 2;
				int x2 = viewX + researchPanX + childPoint.x();
				int y2 = viewY + researchPanY + childPoint.y() + nodeH / 2;
				drawResearchEdge(g, x1, y1, x2, y2, parent != null && parent.complete());
			}
		}

		for (ColonyUiSnapshot.ResearchEntry entry : snapshot.research()) {
			ResearchCanvasPoint point = researchCanvasPoint(entry.nodeId(), advancedX);
			int nx = viewX + researchPanX + point.x();
			int ny = viewY + researchPanY + point.y();
			boolean visible = rectanglesOverlap(nx, ny, nodeW, nodeH, viewX, viewY, viewW, viewH);
			if (!visible) {
				continue;
			}
			ResearchHitbox hitbox = new ResearchHitbox(entry, nx, ny, nodeW, nodeH);
			researchHitboxes.add(hitbox);
			boolean hovered = hitbox.contains(currentMouseX, currentMouseY) && pointInsideViewport(currentMouseX, currentMouseY);
			if (hovered) {
				hoveredResearch = entry;
			}
			drawResearchGraphNode(g, nx, ny, nodeW, nodeH, entry, hovered);
		}
		g.disableScissor();
		drawResearchInspector(g, viewX + 6, viewY + 5, viewW - 12, hoveredResearch);
	}

	private void drawResearchGraphNode(GuiGraphics g, int x, int y, int width, int height,
			ColonyUiSnapshot.ResearchEntry entry, boolean hovered) {
		int progress = entry.complete() ? 100 : percent(entry.progress(), entry.duration());
		int color = entry.complete() ? 0x6DD08E : entry.active() ? 0xB58BFF : entry.startable() ? 0xE0B05A : 0x6C5A43;
		int top = hovered ? 0xC84A4238 : entry.active() ? 0xC538304B : 0xB72B3234;
		int bottom = hovered ? 0xCE252A28 : entry.active() ? 0xCF1A202E : 0xC4181E20;
		fillCutGradient(g, x, y, width, height, top, bottom);
		outlineCutRect(g, x, y, width, height, hovered || entry.startable() || entry.active() ? brighten(color) : CARD_EDGE);
		g.fill(x + 1, y + 3, x + 3, y + height - 3, 0xFF000000 | color);

		fillCutGradient(g, x + 10, y + 8, 26, 26, 0xA83A3430, 0xBB171D1E);
		outlineCutRect(g, x + 9, y + 7, 28, 28, hovered ? ACCENT : CHIP_EDGE);
		drawItemIcon(g, itemForResearch(entry.nodeId()), x + 15, y + 13);
		g.drawString(font, ellipsize(researchLabel(entry), width - 54), x + 44, y + 9, TEXT_MAIN, false);
		g.drawString(font, ellipsize(researchState(entry), width - 58), x + 44, y + 22, entry.startable() ? 0xFFFFD780 : TEXT_MUTED, false);
		if (entry.startable()) {
			g.drawString(font, ">", x + width - 14, y + 22, ACCENT, true);
		}
		drawWideProgress(g, x + 10, y + height - 10, width - 20, progress, color);
	}

	private void drawResearchInspector(GuiGraphics g, int x, int y, int width,
			ColonyUiSnapshot.ResearchEntry entry) {
		fillCutGradient(g, x, y, width, 45, 0xC13A3F3E, 0xCE151B1D);
		g.fill(x + 3, y + 1, x + width - 3, y + 2, entry == null ? ACCENT_DIM : ACCENT);
		outlineCutRect(g, x, y, width, 45, entry == null ? CHIP_EDGE : 0xB99C743A);
		if (entry == null) {
			drawItemIcon(g, ModItems.PHEROMONE_DUST, x + 8, y + 8);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.research.pan_hint"), width - 38), x + 31, y + 8, TEXT_MAIN, false);
			g.drawString(font, ellipsize(translated("formic_frontier.ui.research.hover_hint"), width - 38), x + 31, y + 23, TEXT_MUTED, false);
			return;
		}
		ResearchNode node;
		try {
			node = ResearchNode.fromId(entry.nodeId());
		} catch (IllegalArgumentException ignored) {
			return;
		}
		drawItemIcon(g, itemForResearch(entry.nodeId()), x + 8, y + 8);
		String title = researchLabel(entry) + "  /  " + researchState(entry);
		g.drawString(font, ellipsize(title, width - 38), x + 31, y + 7, TEXT_MAIN, false);
		g.drawString(font, ellipsize(translated("formic_frontier.research." + entry.nodeId() + ".detail"), width - 38), x + 31, y + 19, TEXT_SOFT, false);
		String requirements = translated("formic_frontier.ui.research.requires", translated(ColonyUiSnapshot.buildingLabelKey(node.requiredBuilding())))
				+ "  /  " + translated("formic_frontier.ui.research.costs", researchCosts(node));
		g.drawString(font, ellipsize(requirements, width - 38), x + 31, y + 32, TEXT_MUTED, false);
	}

	private void drawTradeExchange(GuiGraphics g, int x, int y, int width, int height) {
		fillCutGradient(g, x, y, width, height, VIEWPORT_TOP, VIEWPORT_BOTTOM);
		outlineCutRect(g, x, y, width, height, CHIP_EDGE);
		int rowY = y + 7;
		int bannerY = rowY;
		drawTradeContextBanner(g, x + 7, bannerY, width - 14, null);
		rowY += 31;

		List<ColonyUiSnapshot.TradeEntry> sells = tradeRowsForDisplay().stream()
				.filter(entry -> entry.offerId().startsWith("sell_"))
				.toList();
		List<ColonyUiSnapshot.TradeEntry> buys = tradeRowsForDisplay().stream()
				.filter(entry -> !entry.offerId().startsWith("sell_"))
				.toList();
		boolean split = width >= 360;
		int cardH = 42;
		int pitch = cardH + 4;
		int headerH = 17;
		int rowsFit = Math.max(1, (y + height - rowY - headerH - 7) / pitch);
		if (split) {
			tradeMaxScroll = Math.max(0, Math.max(sells.size(), buys.size()) - rowsFit);
			tradeScroll = Math.max(0, Math.min(tradeScroll, tradeMaxScroll));
			int gap = 8;
			int cardW = (width - 22 - gap) / 2;
			int leftX = x + 7;
			int rightX = leftX + cardW + gap;
			drawTradeSection(g, sells, leftX, rowY, cardW, rowsFit, "formic_frontier.ui.trade.colony_buys");
			drawTradeSection(g, buys, rightX, rowY, cardW, rowsFit, "formic_frontier.ui.trade.colony_offers");
		} else {
			List<ColonyUiSnapshot.TradeEntry> all = tradeRowsForDisplay();
			tradeMaxScroll = Math.max(0, all.size() - rowsFit);
			tradeScroll = Math.max(0, Math.min(tradeScroll, tradeMaxScroll));
			drawTradeSection(g, all, x + 7, rowY, width - 14, rowsFit, "formic_frontier.ui.trade.exchange");
		}
		if (tradeMaxScroll > 0) {
			String page = (tradeScroll + 1) + " / " + (tradeMaxScroll + 1);
			g.drawString(font, page, x + width - 8 - font.width(page), y + height - 11, TEXT_FAINT, false);
			g.drawString(font, translated("formic_frontier.ui.trade.scroll_hint"), x + 8, y + height - 11, TEXT_FAINT, false);
		}
		if (hoveredTrade != null) {
			drawTradeContextBanner(g, x + 7, bannerY, width - 14, hoveredTrade);
		}
	}

	private void drawTradeContextBanner(GuiGraphics g, int x, int y, int width,
			ColonyUiSnapshot.TradeEntry entry) {
		fillCutGradient(g, x, y, width, 25, 0xBB343044, 0xC6191B27);
		outlineCutRect(g, x, y, width, 25, entry == null ? 0xB0706387 : 0xC18E72B5);
		drawItemIcon(g, entry == null ? ModItems.PHEROMONE_TOKEN : itemForKey(entry.outputKey()), x + 6, y + 4);
		if (entry == null) {
			String activity = snapshot.tradeActivity().isBlank()
					? translated("formic_frontier.ui.trade.hover_hint")
					: snapshot.tradeActivity();
			g.drawString(font, ellipsize(activity, width - 34), x + 28, y + 8, 0xFFE1D0FF, false);
			return;
		}
		String exchange = translated("formic_frontier.ui.trade.tooltip_exchange",
				entry.inputCount(), translated(entry.inputKey()), entry.outputCount(), translated(entry.outputKey()));
		g.drawString(font, ellipsize(exchange, width - 34), x + 28, y + 4, TEXT_MAIN, false);
		String detail = entry.status() + "  /  "
				+ translated(entry.available() ? "formic_frontier.ui.trade.click" : "formic_frontier.ui.trade.unavailable");
		g.drawString(font, ellipsize(detail, width - 34), x + 28, y + 15, entry.available() ? 0xFFBDECC7 : TEXT_MUTED, false);
	}

	private void drawTradeSection(GuiGraphics g, List<ColonyUiSnapshot.TradeEntry> entries, int x, int y,
			int width, int rowsFit, String titleKey) {
		g.drawString(font, translated(titleKey).toUpperCase(java.util.Locale.ROOT), x + 2, y + 3, TEXT_MUTED, false);
		g.fill(x, y + 14, x + width, y + 15, 0x4C8C6A38);
		int cardY = y + 19;
		for (int i = tradeScroll; i < entries.size() && i < tradeScroll + rowsFit; i++) {
			ColonyUiSnapshot.TradeEntry entry = entries.get(i);
			int cy = cardY + (i - tradeScroll) * 46;
			TradeHitbox hitbox = new TradeHitbox(entry, x, cy, width, 42);
			tradeHitboxes.add(hitbox);
			boolean hovered = hitbox.contains(currentMouseX, currentMouseY) && pointInsideViewport(currentMouseX, currentMouseY);
			if (hovered) {
				hoveredTrade = entry;
			}
			drawInteractiveTradeCard(g, x, cy, width, entry, hovered);
		}
	}

	private void drawInteractiveTradeCard(GuiGraphics g, int x, int y, int width,
			ColonyUiSnapshot.TradeEntry entry, boolean hovered) {
		int color = entry.available() ? 0x6DD08E : 0x6C5A43;
		int edge = hovered ? brighten(color) : entry.available() ? 0xFF6E9D67 : CARD_EDGE;
		drawCardSurface(g, x, y, width, 42, color, edge);
		drawItemIcon(g, itemForKey(entry.inputKey()), x + 8, y + 5);
		drawExchangeArrow(g, x + 27, y + 13, x + 38, entry.available());
		drawItemIcon(g, itemForKey(entry.outputKey()), x + 41, y + 5);
		int textX = x + 64;
		g.drawString(font, ellipsize(shortName(entry.inputKey()) + " > " + shortName(entry.outputKey()), width - 72), textX, y + 5, TEXT_MAIN, false);
		g.drawString(font, entry.inputCount() + "x  >  " + entry.outputCount() + "x", textX, y + 17, TEXT_SOFT, false);
		String state = translated(entry.available() ? "formic_frontier.ui.trade.available" : "formic_frontier.ui.trade.locked");
		g.drawString(font, state, textX, y + 29, entry.available() ? 0xFF9BE7AD : TEXT_FAINT, false);
		int pillW = Math.min(46, Math.max(34, width / 5));
		int pillX = x + width - pillW - 7;
		fillCutGradient(g, pillX, y + 27, pillW, 12, entry.available() ? 0xB3315337 : 0xA8242422, entry.available() ? 0xC41D3824 : 0xB1171B1C);
		outlineCutRect(g, pillX, y + 27, pillW, 12, entry.available() ? 0xD072C884 : CHIP_EDGE);
		g.drawCenteredString(font, translated("formic_frontier.ui.trade.action"), pillX + pillW / 2, y + 29, entry.available() ? 0xFFD9F7DF : TEXT_FAINT);
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
			fillCutGradient(g, x, cy, width, rowH - 2, ROW_TOP, ROW_BOTTOM);
			g.fill(x, cy, x + 3, cy + rowH - 2, 0xFF000000 | color);
			int titleW = Math.max(96, Math.min(150, width * 30 / 100));
			int stateW = 64;
			g.drawString(font, ellipsize(translated(e.titleKey()), titleW - 12), x + 8, cy + 5, TEXT_MAIN, false);
			g.drawString(font, ellipsize(translated(e.detailKey()), width - titleW - stateW - 14), x + titleW, cy + 5, TEXT_SOFT, false);
			String state = translated(e.unlocked() ? "formic_frontier.guide.state.open" : "formic_frontier.guide.state.locked");
			int pillColor = e.unlocked() ? 0xFF2E4A28 : 0xFF2A2017;
			g.fill(x + width - stateW, cy + 3, x + width - 4, cy + rowH - 5, pillColor);
			outlineCutRect(g, x + width - stateW, cy + 3, stateW - 4, rowH - 8, e.unlocked() ? 0xD06DD08E : CHIP_EDGE);
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

	private void drawResearchEdge(GuiGraphics g, int x1, int y1, int x2, int y2, boolean unlocked) {
		int color = unlocked ? 0xFF6FE08F : 0xFFD9A24A;
		int midX = (x1 + x2) / 2;
		g.fill(x1, y1 - 1, midX + 2, y1 + 2, color);
		g.fill(midX, Math.min(y1, y2), midX + 2, Math.max(y1, y2), color);
		g.fill(midX, y2 - 1, x2 - 5, y2 + 2, color);
		g.fill(x2 - 6, y2 - 3, x2 - 1, y2 + 4, color);
		g.fill(x2 - 4, y2 - 5, x2 - 1, y2 + 6, color);
	}

	private void drawExchangeArrow(GuiGraphics g, int x1, int y, int x2, boolean available) {
		int color = available ? 0xFF6FE08F : 0xFF9C8054;
		g.fill(x1, y - 1, x2 - 3, y + 2, color);
		g.fill(x2 - 4, y - 3, x2 - 1, y + 4, color);
		g.fill(x2 - 3, y - 2, x2, y + 3, color);
	}

	private void drawStatRow(GuiGraphics g, int x, int y, int width, String title, String detail, int progress, int color) {
		fillCutGradient(g, x, y, width, 19, ROW_TOP, ROW_BOTTOM);
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
		fillCutGradient(g, x, y, width, height, CARD_TOP, CARD_BOTTOM);
		outlineCutRect(g, x, y, width, height, edge);
		g.fill(x + 1, y + 3, x + 3, y + height - 3, 0xFF000000 | accent);
		g.fill(x + 3, y + 1, x + width - 3, y + 2, 0x16FFFFFF);
	}

	private void drawActionRailFrame(GuiGraphics g, int x, int y, int width, int height) {
		if (height <= 0) {
			return;
		}
		fillCutGradient(g, x, y, width, height, 0x8F2B3233, 0xA3171D1F);
		g.fill(x + 3, y + 1, x + width - 3, y + 2, 0x32FFC56B);
		outlineCutRect(g, x, y, width, height, 0x675F6F6A);
	}

	private static void fillCutRect(GuiGraphics g, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (width < 5 || height < 5) {
			g.fill(x, y, x + width, y + height, color);
			return;
		}
		g.fill(x + 2, y, x + width - 2, y + height, color);
		g.fill(x, y + 2, x + width, y + height - 2, color);
	}

	private static void fillCutGradient(GuiGraphics g, int x, int y, int width, int height, int top, int bottom) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (width < 5 || height < 5) {
			g.fillGradient(x, y, x + width, y + height, top, bottom);
			return;
		}
		g.fillGradient(x + 2, y, x + width - 2, y + height, top, bottom);
		g.fillGradient(x, y + 2, x + width, y + height - 2, top, bottom);
	}

	private static void outlineCutRect(GuiGraphics g, int x, int y, int width, int height, int color) {
		if (width < 5 || height < 5) {
			g.renderOutline(x, y, width, height, color);
			return;
		}
		g.fill(x + 2, y, x + width - 2, y + 1, color);
		g.fill(x + 2, y + height - 1, x + width - 2, y + height, color);
		g.fill(x, y + 2, x + 1, y + height - 2, color);
		g.fill(x + width - 1, y + 2, x + width, y + height - 2, color);
		g.fill(x + 1, y + 1, x + 2, y + 2, color);
		g.fill(x + width - 2, y + 1, x + width - 1, y + 2, color);
		g.fill(x + 1, y + height - 2, x + 2, y + height - 1, color);
		g.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color);
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
	private void addInstinctButtons() {
		int x = mainContentX();
		int y = actionRailY() + 11;
		String[] ids = {"food", "ore", "chitin", "defense"};
		int buttonW = Math.max(48, (mainContentWidth() - (ids.length - 1) * 5) / ids.length);
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			addRenderableWidget(new FormicButton(x + i * (buttonW + 5), y, buttonW, 19,
					Component.translatable("formic_frontier.instinct." + id), () -> ClientPlayNetworking.send(new PriorityRequestPayload(id)), ButtonStyle.ACTION));
		}
	}

	private void addRelationsButtons() {
		int x = mainContentX();
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
		int actionW = Math.max(72, (mainContentWidth() - 10) / 3);
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
		private int accent = ACCENT_DIM;

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
				top = 0xB85F4C31;
				bottom = 0xC3353028;
				border = ACCENT;
				textColor = 0xFFFFF7EA;
			} else if (!active) {
				top = 0x7E242A2B;
				bottom = 0x8E171C1E;
				border = 0x4A59645F;
				textColor = TEXT_FAINT;
			} else if (hovered) {
				top = 0xA5444640;
				bottom = 0xB3242A2A;
				border = 0xD5F1C674;
				textColor = 0xFFFFF7EA;
			} else {
				top = 0x702F3637;
				bottom = 0x861D2325;
				border = 0x506B7873;
				textColor = TEXT_MAIN;
			}
			fillCutGradient(g, x, y, w, h, top, bottom);
			outlineCutRect(g, x, y, w, h, border);
			if (selected && style == ButtonStyle.TAB) {
				g.fill(x + 1, y + 3, x + 3, y + h - 3, 0xFF000000 | accent);
			} else if (style == ButtonStyle.TAB) {
				g.fill(x + 1, y + 5, x + 2, y + h - 5, 0xA0000000 | accent);
			}
			String label = ellipsize(getMessage().getString(), w - (style == ButtonStyle.TAB ? 14 : 8));
			if (style == ButtonStyle.TAB) {
				g.drawString(font, label, x + 8, y + (h - 8) / 2, textColor, false);
			} else {
				g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, textColor);
			}
		}
	}

	private void clampResearchPan(int viewWidth, int viewHeight) {
		int maxX = 16;
		int maxY = 12;
		int canvasWidth = viewWidth + 16;
		int minX = Math.min(maxX, viewWidth - canvasWidth - 16);
		int minY = Math.min(maxY, viewHeight - RESEARCH_CANVAS_HEIGHT - 12);
		researchPanX = Math.max(minX, Math.min(maxX, researchPanX));
		researchPanY = Math.max(minY, Math.min(maxY, researchPanY));
	}

	private ResearchCanvasPoint researchCanvasPoint(String nodeId, int advancedX) {
		return switch (nodeId) {
			case "chitin_cultivation" -> new ResearchCanvasPoint(18, 56);
			case "resin_masonry" -> new ResearchCanvasPoint(18, 114);
			case "fungus_symbiosis" -> new ResearchCanvasPoint(18, 172);
			case "scented_ledger" -> new ResearchCanvasPoint(18, 230);
			case "mandible_plating" -> new ResearchCanvasPoint(advancedX, 114);
			case "venom_drills" -> new ResearchCanvasPoint(advancedX, 172);
			case "treaty_sigils" -> new ResearchCanvasPoint(advancedX, 230);
			default -> new ResearchCanvasPoint(18, 56);
		};
	}

	private String researchLabel(ColonyUiSnapshot.ResearchEntry entry) {
		String key = "formic_frontier.research." + entry.nodeId();
		String localized = translated(key);
		return localized.equals(key) ? entry.label() : localized;
	}

	private String researchState(ColonyUiSnapshot.ResearchEntry entry) {
		String key = entry.complete()
				? "formic_frontier.ui.research.state.complete"
				: entry.active()
						? "formic_frontier.ui.research.state.active"
						: entry.startable()
								? "formic_frontier.ui.research.state.ready"
								: "formic_frontier.ui.research.state.locked";
		return translated(key);
	}

	private String researchCosts(ResearchNode node) {
		List<String> costs = new ArrayList<>();
		for (ResourceType type : ResourceType.values()) {
			int cost = node.cost(type);
			if (cost > 0) {
				costs.add(cost + " " + shortName("formic_frontier.resource." + type.id()));
			}
		}
		return String.join(", ", costs);
	}

	private boolean pointInsideViewport(int x, int y) {
		return x >= contentViewportX && x < contentViewportX + contentViewportWidth
				&& y >= contentViewportY && y < contentViewportY + contentViewportHeight;
	}

	private static boolean rectanglesOverlap(int x1, int y1, int w1, int h1,
			int x2, int y2, int w2, int h2) {
		return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
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
		int available = Math.max(320, width - 12);
		return Math.min(available, Math.max(640, (int) (width * 0.92f)));
	}

	private int panelHeight() {
		int available = Math.max(180, height - 12);
		return Math.min(available, Math.max(300, (int) (height * 0.90f)));
	}

	private int panelX() {
		return (width - panelWidth()) / 2;
	}

	private int panelY() {
		return (height - panelHeight()) / 2;
	}

	private int navigationWidth() {
		return panelWidth() >= 560 ? 118 : 92;
	}

	private int mainContentX() {
		return panelX() + navigationWidth() + 7;
	}

	private int mainContentWidth() {
		return panelWidth() - navigationWidth() - 15;
	}

	private boolean hasActionRail() {
		return switch (selectedTab) {
			case "Instinct", "Relations" -> true;
			default -> false;
		};
	}

	private boolean showsResourceStrip() {
		return switch (selectedTab) {
			case "Overview", "Build" -> true;
			default -> false;
		};
	}

	private int actionRailY() {
		return panelY() + panelHeight() - 76;
	}

	private record Tab(String id, String titleKey, int color) {
	}

	private record ResearchCanvasPoint(int x, int y) {
	}

	private record ResearchHitbox(ColonyUiSnapshot.ResearchEntry entry, int x, int y, int width, int height) {
		private boolean contains(int px, int py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private record TradeHitbox(ColonyUiSnapshot.TradeEntry entry, int x, int y, int width, int height) {
		private boolean contains(int px, int py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private record RequestHitbox(ColonyUiSnapshot.RequestEntry entry, int x, int y, int width, int height) {
		private boolean contains(int px, int py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}
}
