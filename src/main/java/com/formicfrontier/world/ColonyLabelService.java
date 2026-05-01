package com.formicfrontier.world;

import com.formicfrontier.network.ColonyUiSnapshot;
import com.formicfrontier.sim.BuildingVisualStage;
import com.formicfrontier.sim.BuildingType;
import com.formicfrontier.sim.ColonyBuilding;
import com.formicfrontier.sim.ColonyData;
import com.formicfrontier.sim.ColonyIdentity;
import com.formicfrontier.sim.ColonyPersonality;
import com.formicfrontier.sim.ColonyRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

public final class ColonyLabelService {
	private static final String LABEL_TAG = "formic_frontier_label";

	private ColonyLabelService() {
	}

	public static void syncLabels(ServerLevel level, ColonyData colony) {
		String colonyTag = colonyTag(colony.id());
		AABB area = new AABB(
				colony.origin().getX() - 80, colony.origin().getY() - 12, colony.origin().getZ() - 80,
				colony.origin().getX() + 80, colony.origin().getY() + 24, colony.origin().getZ() + 80
		);
		for (Display.TextDisplay label : level.getEntitiesOfClass(Display.TextDisplay.class, area, entity -> entity.getTags().contains(colonyTag))) {
			label.discard();
		}
		for (ColonyBuilding building : colony.progress().buildingsView()) {
			createLabel(level, colony, building);
		}
	}

	private static void createLabel(ServerLevel level, ColonyData colony, ColonyBuilding building) {
		Display.TextDisplay label = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (label == null) {
			return;
		}
		BlockPos pos = building.pos();
		double y = pos.getY() + switch (building.type()) {
			case GREAT_MOUND -> 9.4;
			case QUEEN_VAULT -> 4.8;
			case TRADE_HUB -> 4.6;
			case QUEEN_CHAMBER -> 6.2;
			default -> 5.2;
		};
		label.setPos(pos.getX() + 0.5, y, pos.getZ() + 0.5);
		label.setNoGravity(true);
		label.setCustomName(labelText(colony, building));
		label.setCustomNameVisible(true);
		label.addTag(LABEL_TAG);
		label.addTag(colonyTag(colony.id()));
		level.addFreshEntity(label);
	}

	private static Component labelText(ColonyData colony, ColonyBuilding building) {
		if (building.type() == BuildingType.QUEEN_CHAMBER) {
			ColonyPersonality personality = ColonyIdentity.personality(colony);
			return Component.translatable(
					"formic_frontier.label.line",
					Component.literal(colony.progress().name()),
					Component.translatable(
							"formic_frontier.label.identity_status",
							Component.translatable(personality.labelKey()),
							Component.translatable(ColonyIdentity.relationshipKey(colony))
					)
			);
		}
		Component title = Component.translatable(ColonyUiSnapshot.buildingLabelKey(building.type()));
		Component status;
		BuildingVisualStage stage = building.visualStage();
		if (stage == BuildingVisualStage.COMPLETE) {
			status = Component.translatable("formic_frontier.label.complete");
		} else if (stage == BuildingVisualStage.UPGRADED) {
			status = Component.translatable("formic_frontier.label.upgraded");
		} else if (stage == BuildingVisualStage.DAMAGED) {
			status = Component.translatable("formic_frontier.label.damaged");
		} else if (stage == BuildingVisualStage.PLANNED) {
			status = Component.translatable("formic_frontier.label.planned");
		} else if (stage == BuildingVisualStage.REPAIRING) {
			status = Component.translatable("formic_frontier.label.repairing", building.constructionProgress());
		} else {
			ColonyRequest request = colony.progress().requestsView().stream()
					.filter(candidate -> candidate.building() == building.type() && !candidate.complete())
					.findFirst()
					.orElse(null);
			if (request != null) {
				status = Component.translatable("formic_frontier.label.needs", Component.translatable(ColonyUiSnapshot.resourceLabelKey(request.resource())));
			} else {
				status = Component.translatable("formic_frontier.label.building", building.constructionProgress());
			}
		}
		return Component.translatable("formic_frontier.label.line", title, status);
	}

	private static String colonyTag(int colonyId) {
		return LABEL_TAG + "_" + colonyId;
	}
}
