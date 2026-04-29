package com.formicfrontier.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import com.formicfrontier.sim.AntWorkState;

public final class AntEntityModel extends EntityModel<AntEntityRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart abdomen;
	private final ModelPart leftAntenna;
	private final ModelPart rightAntenna;
	private final ModelPart leftMandible;
	private final ModelPart rightMandible;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftMiddleLeg;
	private final ModelPart rightMiddleLeg;
	private final ModelPart leftBackLeg;
	private final ModelPart rightBackLeg;

	public AntEntityModel(ModelPart root) {
		super(root);
		this.head = root.getChild(PartNames.HEAD);
		this.body = root.getChild(PartNames.BODY);
		this.abdomen = root.getChild("abdomen");
		this.leftAntenna = root.getChild("left_antenna");
		this.rightAntenna = root.getChild("right_antenna");
		this.leftMandible = root.getChild("left_mandible");
		this.rightMandible = root.getChild("right_mandible");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftMiddleLeg = root.getChild("left_middle_leg");
		this.rightMiddleLeg = root.getChild("right_middle_leg");
		this.leftBackLeg = root.getChild("left_back_leg");
		this.rightBackLeg = root.getChild("right_back_leg");
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create().texOffs(0, 0).addBox(-4, -7, -4, 8, 8, 7), PartPose.offset(0, 8, -2));
		root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create().texOffs(0, 17).addBox(-3, -6, -3, 6, 9, 6), PartPose.offset(0, 15, 0));
		root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(34, 0).addBox(-4, -1, -3, 8, 7, 7), PartPose.offset(0, 18, 2));
		root.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(24, 0).addBox(0, -8, -1, 1, 9, 1), PartPose.offsetAndRotation(2, 2, -4, -0.42f, 0.18f, -0.62f));
		root.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(28, 0).addBox(-1, -8, -1, 1, 9, 1), PartPose.offsetAndRotation(-2, 2, -4, -0.42f, -0.18f, 0.62f));
		root.addOrReplaceChild("left_mandible", CubeListBuilder.create().texOffs(24, 12).addBox(0, -1, -5, 2, 2, 5), PartPose.offsetAndRotation(2, 9, -4, 0.0f, -0.45f, 0.1f));
		root.addOrReplaceChild("right_mandible", CubeListBuilder.create().texOffs(24, 19).addBox(-2, -1, -5, 2, 2, 5), PartPose.offsetAndRotation(-2, 9, -4, 0.0f, 0.45f, -0.1f));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(44, 0).addBox(-1, 0, -1, 2, 11, 2), PartPose.offsetAndRotation(4, 11, -2, -0.1f, 0.0f, -0.42f));
		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(52, 0).addBox(-1, 0, -1, 2, 11, 2), PartPose.offsetAndRotation(-4, 11, -2, -0.1f, 0.0f, 0.42f));
		root.addOrReplaceChild("left_middle_leg", CubeListBuilder.create().texOffs(60, 0).addBox(-1, 0, -1, 2, 10, 2), PartPose.offsetAndRotation(3, 15, 1, 0.0f, 0.0f, -0.28f));
		root.addOrReplaceChild("right_middle_leg", CubeListBuilder.create().texOffs(68, 0).addBox(-1, 0, -1, 2, 10, 2), PartPose.offsetAndRotation(-3, 15, 1, 0.0f, 0.0f, 0.28f));
		root.addOrReplaceChild("left_back_leg", CubeListBuilder.create().texOffs(76, 0).addBox(-1, 0, -1, 2, 8, 2), PartPose.offsetAndRotation(2, 17, 3, 0.18f, 0.0f, -0.16f));
		root.addOrReplaceChild("right_back_leg", CubeListBuilder.create().texOffs(84, 0).addBox(-1, 0, -1, 2, 8, 2), PartPose.offsetAndRotation(-2, 17, 3, 0.18f, 0.0f, 0.16f));
		return LayerDefinition.create(mesh, 96, 64);
	}

	@Override
	public void setupAnim(AntEntityRenderState state) {
		super.setupAnim(state);
		float swing = state.walkAnimationPos;
		float amount = state.walkAnimationSpeed;
		float front = Mth.cos(swing * 0.75f) * 0.45f * amount;
		float middle = Mth.cos(swing * 0.75f + Mth.HALF_PI) * 0.35f * amount;
		float back = Mth.cos(swing * 0.75f + Mth.PI) * 0.28f * amount;

		head.xRot = 0.0f;
		head.yRot = 0.0f;
		body.xRot = 0.0f;
		body.yRot = 0.0f;
		abdomen.xRot = 0.0f;
		abdomen.yRot = 0.0f;
		leftAntenna.xRot = -0.42f;
		leftAntenna.yRot = 0.18f;
		leftAntenna.zRot = -0.62f;
		rightAntenna.xRot = -0.42f;
		rightAntenna.yRot = -0.18f;
		rightAntenna.zRot = 0.62f;
		leftMandible.yRot = -0.45f;
		rightMandible.yRot = 0.45f;
		leftFrontLeg.xRot = -0.1f + front;
		rightFrontLeg.xRot = -0.1f - front;
		leftMiddleLeg.xRot = -middle;
		rightMiddleLeg.xRot = middle;
		leftBackLeg.xRot = 0.18f + back;
		rightBackLeg.xRot = 0.18f - back;
		leftFrontLeg.zRot = -0.42f;
		rightFrontLeg.zRot = 0.42f;
		leftMiddleLeg.zRot = -0.28f;
		rightMiddleLeg.zRot = 0.28f;
		leftBackLeg.zRot = -0.16f;
		rightBackLeg.zRot = 0.16f;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float pulse = Mth.sin(state.ageInTicks * 0.35f) * 0.12f;
		float antennaTwitch = Mth.sin(state.ageInTicks * 0.45f) * 0.12f;
		leftAntenna.zRot -= antennaTwitch;
		rightAntenna.zRot += antennaTwitch;
		if (state.workState == AntWorkState.WORKING) {
			head.xRot += Mth.sin(state.ageInTicks * 0.9f) * 0.35f;
			leftMandible.yRot -= 0.12f;
			rightMandible.yRot += 0.12f;
			leftFrontLeg.xRot = -0.95f + Mth.sin(state.ageInTicks * 0.9f) * 0.28f;
			rightFrontLeg.xRot = -0.95f - Mth.sin(state.ageInTicks * 0.9f) * 0.28f;
			abdomen.xRot = 0.12f + pulse;
		} else if (state.workState.name().startsWith("CARRYING")) {
			head.xRot -= 0.28f;
			body.xRot = -0.08f;
			leftFrontLeg.xRot = -1.25f;
			rightFrontLeg.xRot = -1.25f;
			abdomen.xRot = 0.08f;
		} else if (state.workState == AntWorkState.PATROLLING) {
			head.yRot += Mth.sin(state.ageInTicks * 0.18f) * 0.25f;
			abdomen.yRot = Mth.sin(state.ageInTicks * 0.12f) * 0.06f;
		} else {
			abdomen.xRot = pulse * 0.35f;
		}
		float scale = state.caste.visualScale();
		scalePart(head, scale);
		scalePart(body, scale);
		scalePart(abdomen, scale);
		scalePart(leftAntenna, scale);
		scalePart(rightAntenna, scale);
		scalePart(leftMandible, scale);
		scalePart(rightMandible, scale);
		scalePart(leftFrontLeg, scale);
		scalePart(rightFrontLeg, scale);
		scalePart(leftMiddleLeg, scale);
		scalePart(rightMiddleLeg, scale);
		scalePart(leftBackLeg, scale);
		scalePart(rightBackLeg, scale);
	}

	private static void scalePart(ModelPart part, float scale) {
		part.xScale = scale;
		part.yScale = scale;
		part.zScale = scale;
	}
}
