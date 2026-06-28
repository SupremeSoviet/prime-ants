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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
	private static final int CARD_BG = 0xE6261D16;
	private static final int CARD_SOFT = 0xCC322519;
	private static final int CARD_EDGE = 0xFF6E5131;
	private static final int TEXT_MAIN = 0xFFFFE0A6;
	private static final int TEXT_SOFT = 0xFFF4E9C8;
	private static final int TEXT_MUTED = 0xFFEAC67D;
	// Depth / gradient palette (R: "redraw the interface to be beautiful"). The flat
	// solid fills are upgraded to warm vertical gradients with beveled edges so the
	// tablet reads as a polished amber-and-chitin surface with real depth, not a set
	// of flat brown rectangles. Layout coordinates are unchanged - only the fills are.
	private static final int PANEL_TOP = 0xF5302417;
	private static final int PANEL_BOTTOM = 0xF514100A;
	private static final int PANEL_BORDER = 0xFF0C0805;
	private static final int PANEL_INNER_GLOW = 0x82E0B264;
	private static final int HEADER_TOP = 0xFF40301E;
	private static final int HEADER_BOTTOM = 0xFF211810;
	private static final int ACCENT = 0xFFE0B05A;
	private static final int CARD_TOP = 0xF0332615;
	private static final int CARD_BOTTOM = 0xF01D150C;
	private static final int CHIP_TOP = 0xFF2E2217;
	private static final int CHIP_BOTTOM = 0xFF1A130C;
	private static final int ROW_TOP = 0xFF2A2015;
	private static final int ROW_BOTTOM = 0xFF1B140D;
	private static final int BEVEL_HI = 0x3EFFE7B2;
	private static final int BEVEL_LO = 0x52000000;
	private static final int TRACK_BG = 0xFF120D09;
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
		} else if ("Needs".equals(selectedTab)) {
			addContractButtons();
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
		// Soft vignette behind the tablet so the panel reads as a focused foreground
		// surface rather than sitting on a uniform grey wash.
		graphics.fillGradient(0, 0, width, height, 0x96100D0A, 0xC6070504);
		int panelX = panelX();
		int panelY = panelY();
		int panelW = panelWidth();
		int panelH = panelHeight();
		// Drop shadow (two stacked translucent layers) gives the panel lift off the world.
		graphics.fill(panelX + 7, panelY + 9, panelX + panelW + 7, panelY + panelH + 9, 0x59000000);
		graphics.fill(panelX + 3, panelY + 4, panelX + panelW + 4, panelY + panelH + 5, 0x40000000);
		// Panel body: warm vertical gradient with a dark outer border and an amber inner
		// glow line, plus a top highlight bevel so the surface catches light.
		graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_TOP, PANEL_BOTTOM);
		graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 2, BEVEL_HI);
		graphics.renderOutline(panelX, panelY, panelW, panelH, PANEL_BORDER);
		graphics.renderOutline(panelX + 1, panelY + 1, panelW - 2, panelH - 2, PANEL_INNER_GLOW);
		// Header band: gradient with a bright accent underline and a thin shadow seam.
		graphics.fillGradient(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 28, HEADER_TOP, HEADER_BOTTOM);
		graphics.fill(panelX + 2, panelY + 28, panelX + panelW - 2, panelY + 30, ACCENT);
		graphics.fill(panelX + 2, panelY + 30, panelX + panelW - 2, panelY + 31, 0x4D000000);
		graphics.drawString(font, ellipsize(snapshot.title(), panelW - 260), panelX + 12, panelY + 8, TEXT_MAIN, true);
		String meta = Component.translatable(snapshot.cultureKey()).getString()
				+ " | " + Component.translatable(snapshot.relationshipKey()).getString();
		graphics.drawString(font, ellipsize(meta, 170), panelX + panelW - 232, panelY + 9, TEXT_MUTED, false);

		if (!"Guide".equals(selectedTab)) {
			drawResourceBar(graphics, panelX + 12, panelY + 53, panelW - 24);
		}
		int contentY = "Guide".equals(selectedTab) ? panelY + 74 : panelY + 103;
		int contentHeight = hasActionRail()
				? Math.max(80, actionRailY() - contentY - 8)
				: "Guide".equals(selectedTab) ? panelH - 112 : panelH - 140;
		drawContent(graphics, panelX + 12, contentY, panelW - 24, contentHeight);
		if (hasActionRail()) {
			drawActionRail(graphics, panelX + 12, actionRailY() - 4, panelW - 24, panelY + panelH - 39 - (actionRailY() - 4));
		}
		drawFooter(graphics, panelX + 12, panelY + panelH - 34, panelW - 84);

		super.render(graphics, mouseX, mouseY, delta);
	}

	private void drawResourceBar(GuiGraphics graphics, int x, int y, int width) {
		int columns = Math.max(4, Math.min(7, width / 112));
		int chipW = Math.max(92, Math.min(126, (width - (columns - 1) * 6) / columns));
		for (int i = 0; i < snapshot.resources().size(); i++) {
			ColonyUiSnapshot.Metric metric = snapshot.resources().get(i);
			int cx = x + (i % columns) * (chipW + 6);
			int cy = y + (i / columns) * 20;
			graphics.fillGradient(cx, cy, cx + chipW, cy + 17, CHIP_TOP, CHIP_BOTTOM);
			graphics.fill(cx, cy, cx + chipW, cy + 1, BEVEL_HI);
			graphics.fill(cx, cy + 16, cx + chipW, cy + 17, BEVEL_LO);
			graphics.renderOutline(cx, cy, chipW, 17, 0xFF503B23);
			drawItemIcon(graphics, itemForResourceId(metric.id()), cx + 2, cy + 1);
			graphics.fill(cx + 20, cy + 3, cx + 23, cy + 14, 0xFF000000 | metric.color());
			graphics.fill(cx + 20, cy + 3, cx + 21, cy + 14, brighten(metric.color()));
			graphics.drawString(font, ellipsize(shortName(metric.labelKey()) + " " + metric.value(), chipW - 30), cx + 27, cy + 5, TEXT_SOFT, false);
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
			case "Guide" -> drawGuide(graphics, x, y, width, height);
			case "Relations" -> drawRelations(graphics, x, y, width, height);
			default -> drawOverview(graphics, x, y, width, height);
		}
	}

	private void drawOverview(GuiGraphics graphics, int x, int y, int width, int height) {
		drawIdentityStrip(graphics, x, y, width);
		int rowY = y + 44;
		int rows = Math.max(2, Math.min(snapshot.overview().size(), (height - 58) / 20));
		for (ColonyUiSnapshot.OverviewEntry entry : snapshot.overview().stream().limit(rows).toList()) {
			drawTableRow(graphics, x, rowY, width, translated(entry.labelKey()), entry.value(), entry.progress(), entry.color());
			rowY += 20;
		}
		if (height - (rowY - y) >= 38) {
			int popY = rowY + 8;
			graphics.drawString(font, translated("formic_frontier.ui.population"), x, popY, 0xFFEAC67D, true);
			int chipW = Math.max(58, Math.min(96, (width - 18) / 4));
			for (int i = 0; i < snapshot.population().size() && i < 4; i++) {
				ColonyUiSnapshot.Metric metric = snapshot.population().get(i);
				int cx = x + (i % 4) * (chipW + 6);
				int cy = popY + 13 + (i / 4) * 16;
				graphics.fillGradient(cx, cy, cx + chipW, cy + 13, CHIP_TOP, CHIP_BOTTOM);
				graphics.fill(cx, cy, cx + chipW, cy + 1, BEVEL_HI);
				graphics.fill(cx, cy, cx + 3, cy + 13, 0xFF000000 | metric.color());
				graphics.drawString(font, ellipsize(shortName(metric.labelKey()) + " " + metric.value(), chipW - 8), cx + 6, cy + 3, 0xFFF4E9C8, false);
			}
		}
	}

	private void drawIdentityStrip(GuiGraphics graphics, int x, int y, int width) {
		int relationshipColor = 0xFF000000 | snapshot.relationshipColor();
		graphics.fillGradient(x, y, x + width, y + 36, ROW_TOP, ROW_BOTTOM);
		graphics.fill(x, y, x + width, y + 1, BEVEL_HI);
		graphics.fill(x, y + 35, x + width, y + 36, BEVEL_LO);
		graphics.renderOutline(x, y, width, 36, 0xFF3E2E1C);
		graphics.fill(x, y, x + 3, y + 36, relationshipColor);
		int mid = Math.max(120, width / 2);
		graphics.drawString(font, translated("formic_frontier.ui.personality"), x + 8, y + 5, 0xFFEAC67D, false);
		graphics.drawString(font, translated("formic_frontier.ui.relationship"), x + mid, y + 5, 0xFFEAC67D, false);
		graphics.drawString(font, ellipsize(translated(snapshot.personalityKey()) + " | " + translated(snapshot.personalityDetailKey()), mid - 16), x + 8, y + 16, 0xFFF4E9C8, false);
		graphics.drawString(font, ellipsize(translated("formic_frontier.ui.identity_rep", translated(snapshot.relationshipKey()), snapshot.reputation()), width - mid - 8), x + mid, y + 16, 0xFFF4E9C8, false);
		graphics.drawString(font, ellipsize(translated("formic_frontier.ui.current_goal") + ": " + snapshot.currentTask(), width - 16), x + 8, y + 27, 0xFFC9974B, false);
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
			drawInfoCard(graphics, x, y, width, 42, translated("formic_frontier.ui.no_requests"), translated("formic_frontier.ui.no_requests_detail"), Items.WRITABLE_BOOK, 0x6DD08E);
			return;
		}
		List<ColonyUiSnapshot.RequestEntry> rows = snapshot.requests().stream()
				.filter(entry -> entry.fulfilled() < entry.needed())
				.sorted((first, second) -> Integer.compare(second.needed() - second.fulfilled(), first.needed() - first.fulfilled()))
				.limit(cardLimit(height, 56))
				.toList();
		int columns = width >= 620 ? 2 : 1;
		int cardW = (width - (columns - 1) * 8) / columns;
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.RequestEntry entry = rows.get(i);
			int cx = x + (i % columns) * (cardW + 8);
			int cy = y + (i / columns) * 58;
			drawRequestCard(graphics, cx, cy, cardW, entry);
		}
	}

	private void drawResearch(GuiGraphics graphics, int x, int y, int width, int height) {
		// R3 Colony Tablet 2.0: render the research tab as a real prerequisite node MAP,
		// not a positional card grid. Nodes are tiered by their actual ResearchNode
		// prerequisites (roots = Tier I on the left, dependents = Tier II on the right)
		// and bold connector lines are drawn along the real parent->child dependency
		// edges. The edges, arrowheads, and tier bands are intentionally high-contrast
		// so the screenshot reads unambiguously as a branched tech-tree graph.
		Map<String, ColonyUiSnapshot.ResearchEntry> byId = new HashMap<>();
		for (ColonyUiSnapshot.ResearchEntry entry : snapshot.research()) {
			byId.put(entry.nodeId(), entry);
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
		int nodeH = 56;
		int rowGap = 10;
		// Wide central channel reserved for the bold directed edges + arrowheads so
		// the parent->child links have room to read between the two tiers.
		int colGap = 64;
		int nodeW = Math.max(132, Math.min(176, (width - (columns - 1) * colGap - 16) / columns));
		// Show every node that fits; the full tree is only 7 nodes (4 + 3) so on the
		// 1600x900 QA panel all of them should render, making the graph look complete.
		int rowCapacity = Math.max(1, (height + rowGap) / (nodeH + rowGap));
		Map<Integer, List<ColonyUiSnapshot.ResearchEntry>> byTier = new HashMap<>();
		for (ColonyUiSnapshot.ResearchEntry entry : snapshot.research()) {
			int t = tier.getOrDefault(entry.nodeId(), 0);
			byTier.computeIfAbsent(t, k -> new ArrayList<>()).add(entry);
		}
		Comparator<ColonyUiSnapshot.ResearchEntry> tierOrder = Comparator
				.comparing(ColonyUiSnapshot.ResearchEntry::complete)
				.thenComparing(entry -> !entry.active())
				.thenComparing(entry -> !entry.startable())
				.thenComparing(ColonyUiSnapshot.ResearchEntry::nodeId);
		// Lay each tier out as a vertically centered stack and remember each node's
		// box so edges can be drawn between exact parent/child centers.
		Map<String, int[]> slot = new HashMap<>();
		int[] tierNodeCount = new int[columns];
		int[] tierStackTop = new int[columns];
		for (int column = 0; column < columns; column++) {
			List<ColonyUiSnapshot.ResearchEntry> entries = byTier.getOrDefault(column, List.of())
					.stream().sorted(tierOrder).limit(rowCapacity).toList();
			int count = entries.size();
			tierNodeCount[column] = count;
			int stackHeight = count * nodeH + Math.max(0, count - 1) * rowGap;
			int top = y + Math.max(0, (height - stackHeight) / 2);
			tierStackTop[column] = top;
			for (int r = 0; r < count; r++) {
				ColonyUiSnapshot.ResearchEntry entry = entries.get(r);
				int cx = x + column * (nodeW + colGap);
				int cy = top + r * (nodeH + rowGap);
				slot.put(entry.nodeId(), new int[] {cx, cy});
			}
		}
		// Faint tier bands + roman-numeral tier labels so the depth structure reads
		// even at a glance, before the eye traces the edges.
		int[] tierColor = {0x332A4A6B, 0x332A6B4A};
		String[] tierLabel = {"Tier I  \u00b7  roots", "Tier II  \u00b7  advanced"};
		for (int column = 0; column < columns; column++) {
			int cx = x + column * (nodeW + colGap);
			int bandTop = tierStackTop[column] - 16;
			int bandH = tierNodeCount[column] * nodeH + Math.max(0, tierNodeCount[column] - 1) * rowGap + 22;
			graphics.fill(cx - 6, bandTop, cx + nodeW + 6, bandTop + bandH, tierColor[column % tierColor.length]);
			graphics.drawString(font, tierLabel[column % tierLabel.length], cx - 2, bandTop + 3, TEXT_MUTED, false);
		}
		// Bold directed prerequisite edges drawn BEFORE nodes. Each edge is a clear
		// 3-segment elbow in the central channel with a solid arrowhead at the child.
		for (ResearchNode child : ResearchNode.values()) {
			int[] childSlot = slot.get(child.id());
			if (childSlot == null) {
				continue;
			}
			for (String prereqId : child.prerequisites()) {
				int[] parentSlot = slot.get(prereqId);
				if (parentSlot == null) {
					continue;
				}
				ColonyUiSnapshot.ResearchEntry parentEntry = byId.get(prereqId);
				boolean unlocked = parentEntry != null && parentEntry.complete();
				drawResearchEdge(graphics,
						parentSlot[0] + nodeW, parentSlot[1] + nodeH / 2,
						childSlot[0], childSlot[1] + nodeH / 2,
						unlocked);
			}
		}
		// Draw the nodes themselves on top of edges and tier bands.
		for (ResearchNode node : ResearchNode.values()) {
			int[] s = slot.get(node.id());
			if (s == null) {
				continue;
			}
			ColonyUiSnapshot.ResearchEntry entry = byId.get(node.id());
			if (entry == null) {
				continue;
			}
			int progress = entry.complete() ? 100 : percent(entry.progress(), entry.duration());
			int color = entry.active() ? 0xB58BFF : entry.startable() ? 0x6DD08E : 0x8A6D47;
			drawResearchNode(graphics, s[0], s[1], nodeW, nodeH, entry, progress, color);
		}
	}

	/** Recursive prerequisite depth for a research node, memoized into {@code cache}. */
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
				// Unknown prerequisite id should not happen, but never break the UI over it.
			}
		}
		cache.put(node.id(), depth);
		return depth;
	}

	/**
	 * Draws a prerequisite map edge as a 3-segment elbow (right -> mid -> left) so the
	 * parent->child dependency reads as a directed tech-tree link. Green when the parent
	 * is complete (the path is open), dim amber when the child is still gated.
	 */
	private static void drawResearchEdge(GuiGraphics graphics, int x1, int y1, int x2, int y2, boolean unlocked) {
		// Bold, high-contrast directed edge so the parent->child prerequisite link is
		// unmistakable in the screenshot. Green when the dependency is satisfied (the
		// child is unlocked), warm gold when the child is still gated behind it.
		int color = unlocked ? 0xFF6FE08F : 0xFFFFC24A;
		int midX = (x1 + x2) / 2;
		// Horizontal stub leaving the parent's right edge.
		graphics.fill(x1, y1 - 2, midX, y1 + 2, color);
		// Vertical jog through the central channel.
		graphics.fill(midX, Math.min(y1, y2), midX + 3, Math.max(y1, y2), color);
		// Horizontal stub entering the child, ending in a solid arrowhead.
		graphics.fill(midX, y2 - 2, x2, y2 + 2, color);
		graphics.fill(x2 - 7, y2 - 4, x2, y2 + 4, color);
		graphics.fill(x2 - 4, y2 - 7, x2, y2 + 7, color);
	}

	private void drawTrades(GuiGraphics graphics, int x, int y, int width, int height) {
		// Trade offer cards are 64px tall and sit on 72px row pitch. The shared
		// Trade tab also hosts an optional "Latest caravan" info card and the
		// bottom action rail (see actionRailY()). Compute how many offer rows
		// genuinely fit in the remaining content height so cards never overflow
		// into the action-button strip (R3 market/trade breathing-room target,
		// shared by tablet_trade and tablet_market).
		int tradeCardHeight = 64;
		int tradeRowPitch = 72;
		int bottom = y + height;
		boolean hasCaravan = !snapshot.tradeActivity().isBlank();
		int afterCaravan = height - (hasCaravan ? 44 : 0);
		// Only show the caravan info card when there is still room for at least
		// one offer row beneath it; offers are the market's reason for being.
		boolean showCaravan = hasCaravan && afterCaravan >= tradeRowPitch;
		int rowY = y;
		if (showCaravan) {
			drawInfoCard(graphics, x, rowY, width, 36, "Latest caravan", snapshot.tradeActivity(), ModItems.PHEROMONE_TOKEN, 0xB58BFF);
			rowY += 44;
		}
		int columns = width >= 690 ? 3 : width >= 460 ? 2 : 1;
		int cardW = (width - (columns - 1) * 8) / columns;
		// max(0, ...) instead of max(1, ...): if no offer row fits, render none
		// rather than forcing a row that collides with the action rail.
		int rowsThatFit = (bottom - rowY) / tradeRowPitch;
		int offerLimit = Math.min(columns * Math.max(0, rowsThatFit), 6);
		List<ColonyUiSnapshot.TradeEntry> rows = tradeRowsForDisplay().stream()
				.limit(offerLimit)
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.TradeEntry entry = rows.get(i);
			int cx = x + (i % columns) * (cardW + 8);
			int cy = rowY + (i / columns) * tradeRowPitch;
			// Final guard: never draw a card that would touch the action rail.
			if (cy + tradeCardHeight > bottom) {
				break;
			}
			drawTradeCard(graphics, cx, cy, cardW, entry);
		}
	}

	private void drawInstinct(GuiGraphics graphics, int x, int y, int width, int height) {
		drawTableRow(graphics, x, y, width, translated("formic_frontier.ui.instinct_help"), translated("formic_frontier.ui.instinct_detail"), 0, 0xC9974B);
		for (int i = 0; i < snapshot.instinct().size() && i < maxRows(height) - 1; i++) {
			ColonyUiSnapshot.Metric metric = snapshot.instinct().get(i);
			drawTableRow(graphics, x, y + 26 + i * 22, width, translated(metric.labelKey()), Component.translatable("formic_frontier.ui.priority", i + 1).getString(), percent(metric.value(), metric.max()), metric.color());
		}
	}

	private void drawGuide(GuiGraphics graphics, int x, int y, int width, int height) {
		int rowHeight = 18;
		List<ColonyUiSnapshot.GuideEntry> rows = snapshot.guide().stream()
				.limit(Math.max(1, height / rowHeight))
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.GuideEntry entry = rows.get(i);
			int color = entry.unlocked() ? entry.color() : 0x8A6D47;
			String state = translated(entry.unlocked() ? "formic_frontier.guide.state.open" : "formic_frontier.guide.state.locked");
			drawGuideRow(graphics, x, y + i * rowHeight, width, translated(entry.titleKey()), translated(entry.detailKey()), state, color);
		}
	}

	private void drawGuideRow(GuiGraphics graphics, int x, int y, int width, String title, String detail, String state, int color) {
		graphics.fillGradient(x, y, x + width, y + 15, ROW_TOP, ROW_BOTTOM);
		graphics.fill(x, y, x + width, y + 1, BEVEL_HI);
		graphics.fill(x, y, x + 3, y + 15, 0xFF000000 | color);
		graphics.fill(x, y, x + 3, y + 2, 0x6BFFFFFF);
		int titleWidth = Math.max(76, Math.min(112, width * 28 / 100));
		int stateWidth = 58;
		graphics.drawString(font, ellipsize(title, titleWidth - 14), x + 7, y + 4, 0xFFFFE0A6, false);
		graphics.drawString(font, ellipsize(detail, Math.max(40, width - titleWidth - stateWidth - 18)), x + titleWidth, y + 4, 0xFFF4E9C8, false);
		graphics.fill(x + width - stateWidth, y + 3, x + width - 5, y + 13, 0xFF120D09);
		graphics.drawString(font, ellipsize(state, stateWidth - 8), x + width - stateWidth + 4, y + 4, 0xFFEAC67D, false);
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

	private void drawInfoCard(GuiGraphics graphics, int x, int y, int width, int height, String title, String detail, Item icon, int color) {
		drawCardSurface(graphics, x, y, width, height, color, CARD_EDGE);
		drawItemIcon(graphics, icon, x + 9, y + Math.max(3, (height - 16) / 2));
		graphics.drawString(font, ellipsize(title, width - 42), x + 32, y + 8, TEXT_MAIN, false);
		graphics.drawString(font, ellipsize(detail, width - 42), x + 32, y + 21, TEXT_SOFT, false);
	}

	private void drawRequestCard(GuiGraphics graphics, int x, int y, int width, ColonyUiSnapshot.RequestEntry entry) {
		int progress = percent(entry.fulfilled(), entry.needed());
		int color = colorForResource(entry.resourceId());
		drawCardSurface(graphics, x, y, width, 52, color, CARD_EDGE);
		drawItemIcon(graphics, itemForResourceId(entry.resourceId()), x + 9, y + 8);
		drawItemIcon(graphics, itemForBuildingId(entry.buildingId()), x + width - 26, y + 8);
		String title = translated("formic_frontier.ui.request.title", shortName(entry.resourceKey()), requestBuildingName(entry));
		String delivery = translated("formic_frontier.ui.request.delivery",
				entry.deliveryItemCount() + " " + shortName(entry.deliveryItemKey()),
				entry.deliveryAmount(), shortName(entry.resourceKey()));
		String reward = translated("formic_frontier.ui.request.reward", entry.rewardTokens(), entry.reputationDelta(), entry.priority());
		graphics.drawString(font, ellipsize(title, width - 76), x + 32, y + 7, TEXT_MAIN, false);
		graphics.drawString(font, ellipsize(delivery, width - 42), x + 32, y + 20, TEXT_SOFT, false);
		graphics.drawString(font, ellipsize(reward, width - 42), x + 32, y + 33, TEXT_MUTED, false);
		drawWideProgress(graphics, x + 8, y + 45, width - 16, progress, color);
	}

	private void drawResearchNode(GuiGraphics graphics, int x, int y, int width, int height, ColonyUiSnapshot.ResearchEntry entry, int progress, int color) {
		int bgTop = entry.complete() ? 0xF03B2E1A : entry.active() ? 0xF0352749 : CARD_TOP;
		int bgBottom = entry.complete() ? 0xF01E160C : entry.active() ? 0xF01B1330 : CARD_BOTTOM;
		graphics.fillGradient(x, y, x + width, y + height, bgTop, bgBottom);
		graphics.fill(x, y, x + 1, y + height, BEVEL_HI);
		graphics.fill(x, y + height - 1, x + width, y + height, BEVEL_LO);
		graphics.fill(x + width - 1, y, x + width, y + height, BEVEL_LO);
		graphics.renderOutline(x, y, width, height, entry.startable() || entry.active() ? 0xFFB9894D : CARD_EDGE);
		graphics.fill(x, y, x + width, y + 3, 0xFF000000 | color);
		graphics.fill(x, y, x + width, y + 1, 0x70FFFFFF);
		drawItemIcon(graphics, itemForResearch(entry.nodeId()), x + 8, y + 9);
		String state = entry.complete() ? "Open" : entry.active() ? "Studying" : entry.startable() ? "Ready" : "Locked";
		graphics.drawString(font, ellipsize(entry.label(), width - 38), x + 30, y + 8, TEXT_MAIN, false);
		graphics.drawString(font, ellipsize(state + " | " + entry.status(), width - 18), x + 8, y + 25, TEXT_SOFT, false);
		drawWideProgress(graphics, x + 8, y + height - 10, width - 16, progress, color);
	}

	private void drawTradeCard(GuiGraphics graphics, int x, int y, int width, ColonyUiSnapshot.TradeEntry entry) {
		int color = entry.available() ? 0x6DD08E : 0x8A6D47;
		drawCardSurface(graphics, x, y, width, 64, color, entry.available() ? 0xFF8BCB86 : CARD_EDGE);
		String title = shortName(entry.inputKey()) + " to " + shortName(entry.outputKey());
		// Layout: a top icon row (input icon -> bold exchange arrow -> output icon) so the
		// give-this-to-get-that direction is instantly scannable, then title/status and
		// counts beneath. The arrow lives ON the icon row, between the two item icons,
		// not as a disconnected bottom band (R3 "trade is clearly a market" criterion).
		int inputIconX = x + 8;
		int outputIconX = x + width - 28;
		int iconY = y + 6;
		drawItemIcon(graphics, itemForKey(entry.inputKey()), inputIconX, iconY);
		drawItemIcon(graphics, itemForKey(entry.outputKey()), outputIconX, iconY);
		// Arrow runs from the right edge of the input icon to the left edge of the output
		// icon, vertically centered on the 16px item icons (center ~ iconY + 8).
		int arrowY = iconY + 8;
		drawTradeExchangeArrow(graphics, inputIconX + 20, arrowY, outputIconX - 4, arrowY, entry.available());
		// Title and status sit below the icon/arrow row.
		graphics.drawString(font, ellipsize(title, width - 16), x + 8, y + 28, TEXT_MAIN, false);
		graphics.drawString(font, ellipsize(entry.status(), width - 16), x + 8, y + 40, entry.available() ? TEXT_SOFT : TEXT_MUTED, false);
		// Counts anchored under their respective icons so the direction stays anchored.
		graphics.drawString(font, entry.inputCount() + "x", inputIconX, y + 50, TEXT_SOFT, false);
		graphics.drawString(font, entry.outputCount() + "x", outputIconX, y + 50, TEXT_SOFT, false);
	}

	private void drawTradeExchangeArrow(GuiGraphics graphics, int x1, int y1, int x2, int y2, boolean available) {
		// Bold, high-contrast directed arrow from the input item (left) to the output
		// item (right). Deliberately mirrors the proven research-connector arrow
		// language so each market card reads instantly as a "give this -> get that"
		// exchange (R3 criterion), even without reading the title text. Green when the
		// offer is available, warm gold when it is spent.
		int color = available ? 0xFF6FE08F : 0xFFEAC67D;
		// 5px-tall shaft ending where the arrowhead base begins.
		graphics.fill(x1, y1 - 2, x2 - 6, y1 + 3, color);
		// Large triangular arrowhead pointing right at the output item: three stacked
		// fills widen toward the tip so it reads as an arrow, not a meter.
		graphics.fill(x2 - 6, y2 - 3, x2, y2 + 4, color);
		graphics.fill(x2 - 4, y2 - 5, x2, y2 + 6, color);
		graphics.fill(x2 - 2, y2 - 8, x2, y2 + 9, color);
	}

	private void drawFooter(GuiGraphics graphics, int x, int y, int width) {
		graphics.drawString(font, Component.translatable("formic_frontier.ui.colony_id", snapshot.colonyId()).getString(), x, y, 0xFFEAC67D, false);
		graphics.drawString(font, Component.translatable("formic_frontier.ui.reputation", snapshot.reputation()).getString(), x + 78, y, 0xFFF4E9C8, false);
		graphics.drawString(font, Component.translatable("formic_frontier.ui.claim", snapshot.claimRadius()).getString(), x + 138, y, 0xFFF4E9C8, false);
		String identity = translated(snapshot.personalityKey()) + " | " + translated(snapshot.relationshipKey());
		graphics.drawString(font, ellipsize(identity, Math.max(40, width - 204)), x + 198, y, 0xFFEAC67D, false);
		if (!snapshot.feedbackMessage().isBlank()) {
			graphics.fillGradient(x, y + 12, x + width, y + 28, 0xFF243A22, 0xFF162616);
			graphics.fill(x, y + 12, x + width, y + 13, 0x55A6F0B0);
			graphics.renderOutline(x, y + 12, width, 16, 0xFF6DD08E);
			graphics.drawString(font, ellipsize(snapshot.feedbackMessage(), width - 10), x + 5, y + 16, 0xFFBFF0C7, false);
		}
	}

	private void drawActionRail(GuiGraphics graphics, int x, int y, int width, int height) {
		if (height <= 0) {
			return;
		}
		graphics.fillGradient(x, y, x + width, y + height, 0xF0241A10, 0xF0130D07);
		graphics.fill(x, y, x + width, y + 1, 0x3EE0B05A);
		graphics.renderOutline(x, y, width, height, 0xFF503B23);
	}

	private void drawTableRow(GuiGraphics graphics, int x, int y, int width, String title, String detail, int progress, int color) {
		graphics.fillGradient(x, y, x + width, y + 19, ROW_TOP, ROW_BOTTOM);
		graphics.fill(x, y, x + width, y + 1, BEVEL_HI);
		graphics.fill(x, y + 18, x + width, y + 19, BEVEL_LO);
		graphics.fill(x, y, x + 3, y + 19, 0xFF000000 | color);
		graphics.fill(x, y, x + 3, y + 2, 0x6BFFFFFF);
		int titleWidth = Math.max(82, Math.min(150, width * 38 / 100));
		graphics.drawString(font, ellipsize(title, titleWidth - 14), x + 7, y + 5, 0xFFFFE0A6, false);
		graphics.drawString(font, ellipsize(detail, Math.max(40, width - titleWidth - 54)), x + titleWidth, y + 5, 0xFFF4E9C8, false);
		drawMiniProgress(graphics, x + width - 42, y + 7, 34, progress, color);
	}

	private void drawRequestRow(GuiGraphics graphics, int x, int y, int width, String title, String detail, int progress, int color) {
		graphics.fillGradient(x, y, x + width, y + 19, ROW_TOP, ROW_BOTTOM);
		graphics.fill(x, y, x + width, y + 1, BEVEL_HI);
		graphics.fill(x, y + 18, x + width, y + 19, BEVEL_LO);
		graphics.fill(x, y, x + 3, y + 19, 0xFF000000 | color);
		graphics.fill(x, y, x + 3, y + 2, 0x6BFFFFFF);
		int titleWidth = Math.max(76, Math.min(126, width * 30 / 100));
		graphics.drawString(font, ellipsize(title, titleWidth - 14), x + 7, y + 5, 0xFFFFE0A6, false);
		graphics.drawString(font, ellipsize(detail, Math.max(40, width - titleWidth - 54)), x + titleWidth, y + 5, 0xFFF4E9C8, false);
		drawMiniProgress(graphics, x + width - 42, y + 7, 34, progress, color);
	}

	private void drawTradeRow(GuiGraphics graphics, int x, int y, int width, String title, String detail, int progress, int color) {
		graphics.fillGradient(x, y, x + width, y + 19, ROW_TOP, ROW_BOTTOM);
		graphics.fill(x, y, x + width, y + 1, BEVEL_HI);
		graphics.fill(x, y + 18, x + width, y + 19, BEVEL_LO);
		graphics.fill(x, y, x + 3, y + 19, 0xFF000000 | color);
		graphics.fill(x, y, x + 3, y + 2, 0x6BFFFFFF);
		int titleWidth = Math.max(148, Math.min(236, width * 46 / 100));
		graphics.drawString(font, ellipsize(title, titleWidth - 14), x + 7, y + 5, 0xFFFFE0A6, false);
		graphics.drawString(font, ellipsize(detail, Math.max(40, width - titleWidth - 54)), x + titleWidth, y + 5, 0xFFF4E9C8, false);
		drawMiniProgress(graphics, x + width - 42, y + 7, 34, progress, color);
	}

	private void drawMiniProgress(GuiGraphics graphics, int x, int y, int width, int progress, int color) {
		graphics.fill(x, y, x + width, y + 4, TRACK_BG);
		graphics.renderOutline(x, y, width, 4, 0xFF0A0705);
		int clamped = Math.max(0, Math.min(100, progress));
		if (clamped <= 0) {
			return;
		}
		int fill = x + width * clamped / 100;
		graphics.fillGradient(x, y, fill, y + 4, brighten(color), 0xFF000000 | color);
		graphics.fill(x, y, fill, y + 1, 0x59FFFFFF);
	}

	private void drawWideProgress(GuiGraphics graphics, int x, int y, int width, int progress, int color) {
		graphics.fill(x, y, x + width, y + 5, TRACK_BG);
		graphics.renderOutline(x, y, width, 5, 0xFF0A0705);
		int clamped = Math.max(0, Math.min(100, progress));
		int fill = x + width * clamped / 100;
		if (clamped > 0) {
			graphics.fillGradient(x, y, fill, y + 5, brighten(color), 0xFF000000 | color);
			graphics.fill(x, y, fill, y + 1, 0x66FFFFFF);
		}
	}

	/**
	 * Shared beveled card surface: a warm vertical gradient with a top/left highlight
	 * and a bottom/right shadow so each card reads as a raised tile, plus a glossed
	 * accent spine down the left edge. {@code accent} is an 0xRRGGBB colour.
	 */
	private void drawCardSurface(GuiGraphics graphics, int x, int y, int width, int height, int accent, int edge) {
		graphics.fillGradient(x, y, x + width, y + height, CARD_TOP, CARD_BOTTOM);
		graphics.fill(x, y, x + width, y + 1, BEVEL_HI);
		graphics.fill(x, y, x + 1, y + height, BEVEL_HI);
		graphics.fill(x, y + height - 1, x + width, y + height, BEVEL_LO);
		graphics.fill(x + width - 1, y, x + width, y + height, BEVEL_LO);
		graphics.renderOutline(x, y, width, height, edge);
		graphics.fill(x, y, x + 4, y + height, 0xFF000000 | accent);
		graphics.fill(x + 1, y, x + 4, y + Math.min(height, 3), 0x73FFFFFF);
	}

	/** Brightens an 0xRRGGBB colour toward white for gradient tops / highlights. */
	private static int brighten(int rgb) {
		int r = Math.min(255, ((rgb >> 16) & 0xFF) + 70);
		int g = Math.min(255, ((rgb >> 8) & 0xFF) + 70);
		int b = Math.min(255, (rgb & 0xFF) + 70);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private void drawItemIcon(GuiGraphics graphics, Item item, int x, int y) {
		graphics.renderItem(new ItemStack(item), x, y);
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

	private void addTradeButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
		int columns = Math.max(2, Math.min(5, (panelWidth() - 86) / 78));
		int buttonW = Math.max(68, Math.min(86, (panelWidth() - 92) / columns));
		List<ColonyUiSnapshot.TradeEntry> rows = tradeRowsForDisplay().stream()
				.limit(5)
				.toList();
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.TradeEntry entry = rows.get(i);
			String label = tradeButtonLabel(entry);
			Button button = Button.builder(Component.literal(ellipsize(label, buttonW - 8)), widget -> ClientPlayNetworking.send(new TradeRequestPayload(entry.offerId())))
					.bounds(x + (i % columns) * (buttonW + 4), y + (i / columns) * 21, buttonW, 18)
					.build();
			button.active = entry.available();
			addRenderableWidget(button);
		}
	}

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

	private void addContractButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
		List<ColonyUiSnapshot.RequestEntry> rows = snapshot.requests().stream()
				.filter(entry -> entry.fulfilled() < entry.needed())
				.limit(4)
				.toList();
		// Cyrillic helper labels (e.g. "Помочь Феромонный") are wider than the
		// English originals, so the request action row needs a larger per-button
		// cap to avoid truncation in localized tablet captures.
		int columns = Math.max(2, Math.min(4, (panelWidth() - 86) / 132));
		int buttonW = Math.max(96, Math.min(168, (panelWidth() - 92) / columns));
		for (int i = 0; i < rows.size(); i++) {
			ColonyUiSnapshot.RequestEntry entry = rows.get(i);
			Button button = Button.builder(Component.literal(ellipsize(translated("formic_frontier.ui.request.help", requestBuildingName(entry)), buttonW - 8)), widget -> ClientPlayNetworking.send(new ContractRequestPayload(entry.contractId())))
					.bounds(x + (i % columns) * (buttonW + 4), y + (i / columns) * 21, buttonW, 18)
					.build();
			button.active = !entry.contractId().isBlank();
			addRenderableWidget(button);
		}
	}

	private void addResearchButtons() {
		int x = panelX() + 12;
		int y = actionRailY();
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
		int y = actionRailY() + 10;
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
		int y = actionRailY();
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
			case "ally" -> 100;
			case "neutral" -> 60;
			case "rival" -> 30;
			case "war" -> 10;
			default -> 0;
		};
	}

	private int maxRows(int height) {
		return Math.max(4, Math.min(8, height / 21));
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
		// Short translated building name for request cards/buttons. buildingKey()
		// already points at the localized block/building lang key, so routing it
		// through shortName keeps request text localized in every locale.
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
		return Math.min(Math.max(380, width - 28), Math.max(560, (int) (width * 0.90f)));
	}

	private int panelHeight() {
		return Math.min(Math.max(270, height - 28), Math.max(320, (int) (height * 0.88f)));
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
		return panelY() + panelHeight() - 78;
	}

	private record Tab(String id, String shortKey, String titleKey) {
	}
}
