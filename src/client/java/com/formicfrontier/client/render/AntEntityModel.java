package com.formicfrontier.client.render;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.AntWorkState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * Upright ant model built around readable insect anatomy rather than a stack of
 * straight cuboids. The head owns articulated antennae and mandibles, the body
 * carries optional caste plates and queen wing buds, and every leg has a second
 * joint. Caste identity is expressed through proportions and visible parts;
 * overall entity size is applied once by the renderer.
 */
public final class AntEntityModel extends EntityModel<AntEntityRenderState> {
	private static final String TIP = "tip";
	private static final String LOWER = "lower";

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart petiole;
	private final ModelPart postPetiole;
	private final ModelPart abdomen;
	private final ModelPart headCrest;
	private final ModelPart bodyPlate;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart leftAntenna;
	private final ModelPart rightAntenna;
	private final ModelPart leftAntennaTip;
	private final ModelPart rightAntennaTip;
	private final ModelPart leftMandible;
	private final ModelPart rightMandible;
	private final ModelPart leftMandibleTip;
	private final ModelPart rightMandibleTip;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftMiddleLeg;
	private final ModelPart rightMiddleLeg;
	private final ModelPart leftBackLeg;
	private final ModelPart rightBackLeg;
	private final ModelPart leftFrontLowerLeg;
	private final ModelPart rightFrontLowerLeg;
	private final ModelPart leftMiddleLowerLeg;
	private final ModelPart rightMiddleLowerLeg;
	private final ModelPart leftBackLowerLeg;
	private final ModelPart rightBackLowerLeg;

	public AntEntityModel(ModelPart root) {
		super(root);
		this.head = root.getChild(PartNames.HEAD);
		this.body = root.getChild(PartNames.BODY);
		this.petiole = root.getChild("petiole");
		this.postPetiole = root.getChild("post_petiole");
		this.abdomen = root.getChild("abdomen");
		this.headCrest = head.getChild("crest");
		this.leftAntenna = head.getChild("left_antenna");
		this.rightAntenna = head.getChild("right_antenna");
		this.leftAntennaTip = leftAntenna.getChild(TIP);
		this.rightAntennaTip = rightAntenna.getChild(TIP);
		this.leftMandible = head.getChild("left_mandible");
		this.rightMandible = head.getChild("right_mandible");
		this.leftMandibleTip = leftMandible.getChild(TIP);
		this.rightMandibleTip = rightMandible.getChild(TIP);
		this.bodyPlate = body.getChild("plate");
		this.leftWing = body.getChild("left_wing");
		this.rightWing = body.getChild("right_wing");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftMiddleLeg = root.getChild("left_middle_leg");
		this.rightMiddleLeg = root.getChild("right_middle_leg");
		this.leftBackLeg = root.getChild("left_back_leg");
		this.rightBackLeg = root.getChild("right_back_leg");
		this.leftFrontLowerLeg = leftFrontLeg.getChild(LOWER);
		this.rightFrontLowerLeg = rightFrontLeg.getChild(LOWER);
		this.leftMiddleLowerLeg = leftMiddleLeg.getChild(LOWER);
		this.rightMiddleLowerLeg = rightMiddleLeg.getChild(LOWER);
		this.leftBackLowerLeg = leftBackLeg.getChild(LOWER);
		this.rightBackLowerLeg = rightBackLeg.getChild(LOWER);
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild(PartNames.HEAD,
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-4.0f, -5.5f, -4.0f, 8.0f, 7.0f, 8.0f)
						.texOffs(120, 0).addBox(-4.35f, -3.8f, -4.25f, 2.0f, 2.0f, 1.0f)
						.texOffs(120, 0).addBox(2.35f, -3.8f, -4.25f, 2.0f, 2.0f, 1.0f),
				PartPose.offset(0.0f, 7.5f, -2.5f));
		head.addOrReplaceChild("crest",
				CubeListBuilder.create().texOffs(70, 25).addBox(-4.0f, -6.5f, -3.5f, 8.0f, 2.0f, 7.0f),
				PartPose.ZERO);

		PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna",
				CubeListBuilder.create().texOffs(70, 0).addBox(0.0f, -5.0f, -1.0f, 1.0f, 5.0f, 1.0f),
				PartPose.offsetAndRotation(2.1f, -4.6f, -3.2f, -0.34f, 0.18f, -0.48f));
		leftAntenna.addOrReplaceChild(TIP,
				CubeListBuilder.create().texOffs(80, 0).addBox(0.0f, -6.0f, -1.0f, 1.0f, 6.0f, 1.0f),
				PartPose.offsetAndRotation(0.0f, -5.0f, 0.0f, -0.12f, 0.18f, -0.24f));
		PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna",
				CubeListBuilder.create().texOffs(75, 0).addBox(-1.0f, -5.0f, -1.0f, 1.0f, 5.0f, 1.0f),
				PartPose.offsetAndRotation(-2.1f, -4.6f, -3.2f, -0.34f, -0.18f, 0.48f));
		rightAntenna.addOrReplaceChild(TIP,
				CubeListBuilder.create().texOffs(85, 0).addBox(-1.0f, -6.0f, -1.0f, 1.0f, 6.0f, 1.0f),
				PartPose.offsetAndRotation(0.0f, -5.0f, 0.0f, -0.12f, -0.18f, 0.24f));

		PartDefinition leftMandible = head.addOrReplaceChild("left_mandible",
				CubeListBuilder.create().texOffs(70, 9).addBox(0.0f, -1.0f, -4.0f, 2.0f, 2.0f, 4.0f),
				PartPose.offsetAndRotation(1.5f, 0.2f, -3.6f, 0.04f, -0.38f, 0.08f));
		leftMandible.addOrReplaceChild(TIP,
				CubeListBuilder.create().texOffs(70, 17).addBox(0.0f, -1.0f, -4.0f, 1.0f, 1.0f, 4.0f),
				PartPose.offsetAndRotation(1.0f, 0.0f, -4.0f, 0.0f, -0.42f, 0.0f));
		PartDefinition rightMandible = head.addOrReplaceChild("right_mandible",
				CubeListBuilder.create().texOffs(84, 9).addBox(-2.0f, -1.0f, -4.0f, 2.0f, 2.0f, 4.0f),
				PartPose.offsetAndRotation(-1.5f, 0.2f, -3.6f, 0.04f, 0.38f, -0.08f));
		rightMandible.addOrReplaceChild(TIP,
				CubeListBuilder.create().texOffs(82, 17).addBox(-1.0f, -1.0f, -4.0f, 1.0f, 1.0f, 4.0f),
				PartPose.offsetAndRotation(-1.0f, 0.0f, -4.0f, 0.0f, 0.42f, 0.0f));

		PartDefinition body = root.addOrReplaceChild(PartNames.BODY,
				CubeListBuilder.create().texOffs(0, 17).addBox(-3.5f, -5.0f, -3.0f, 7.0f, 8.0f, 6.0f),
				PartPose.offset(0.0f, 14.0f, 0.0f));
		body.addOrReplaceChild("plate",
				CubeListBuilder.create().texOffs(70, 35).addBox(-4.0f, -5.7f, -3.5f, 8.0f, 2.0f, 7.0f),
				PartPose.ZERO);
		body.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(70, 46).addBox(0.0f, -1.0f, -1.0f, 1.0f, 7.0f, 9.0f),
				PartPose.offsetAndRotation(3.1f, -2.0f, 0.0f, 0.12f, -0.2f, -0.22f));
		body.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(92, 46).addBox(-1.0f, -1.0f, -1.0f, 1.0f, 7.0f, 9.0f),
				PartPose.offsetAndRotation(-3.1f, -2.0f, 0.0f, 0.12f, 0.2f, 0.22f));

		root.addOrReplaceChild("petiole",
				CubeListBuilder.create().texOffs(34, 17).addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f),
				PartPose.offset(0.0f, 17.0f, 2.4f));
		root.addOrReplaceChild("post_petiole",
				CubeListBuilder.create().texOffs(48, 17).addBox(-2.0f, -1.5f, -2.0f, 4.0f, 4.0f, 4.0f),
				PartPose.offset(0.0f, 19.0f, 3.5f));
		root.addOrReplaceChild("abdomen",
				CubeListBuilder.create().texOffs(34, 0).addBox(-4.5f, -2.5f, -3.5f, 9.0f, 7.0f, 8.0f),
				PartPose.offset(0.0f, 20.0f, 5.0f));

		addJointedLeg(root, "left_front_leg", 0, 34, 0, 43, 3.3f, 10.5f, -2.1f, -0.16f, -0.40f);
		addJointedLeg(root, "right_front_leg", 9, 34, 5, 43, -3.3f, 10.5f, -2.1f, -0.16f, 0.40f);
		addJointedLeg(root, "left_middle_leg", 18, 34, 10, 43, 3.5f, 12.6f, 0.0f, 0.0f, -0.34f);
		addJointedLeg(root, "right_middle_leg", 27, 34, 15, 43, -3.5f, 12.6f, 0.0f, 0.0f, 0.34f);
		addJointedLeg(root, "left_back_leg", 36, 34, 20, 43, 3.1f, 14.3f, 2.3f, 0.22f, -0.30f);
		addJointedLeg(root, "right_back_leg", 45, 34, 25, 43, -3.1f, 14.3f, 2.3f, 0.22f, 0.30f);

		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void addJointedLeg(PartDefinition root, String name, int upperU, int upperV,
			int lowerU, int lowerV, float x, float y, float z, float xRot, float zRot) {
		PartDefinition upper = root.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(upperU, upperV).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f),
				PartPose.offsetAndRotation(x, y, z, xRot, 0.0f, zRot));
		float lowerBend = zRot < 0.0f ? -0.25f : 0.25f;
		upper.addOrReplaceChild(LOWER,
				CubeListBuilder.create().texOffs(lowerU, lowerV).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 8.0f, 1.0f),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, 0.0f, 0.0f, lowerBend));
	}

	@Override
	public void setupAnim(AntEntityRenderState state) {
		super.setupAnim(state);
		resetAntPose();

		float swing = state.walkAnimationPos;
		float amount = state.walkAnimationSpeed;
		float front = Mth.cos(swing * 0.75f) * 0.48f * amount;
		float middle = Mth.cos(swing * 0.75f + Mth.PI) * 0.42f * amount;
		float back = Mth.cos(swing * 0.75f) * 0.34f * amount;
		leftFrontLeg.xRot += front;
		rightFrontLeg.xRot -= front;
		leftMiddleLeg.xRot += middle;
		rightMiddleLeg.xRot -= middle;
		leftBackLeg.xRot -= back;
		rightBackLeg.xRot += back;
		leftFrontLowerLeg.xRot = -front * 0.42f;
		rightFrontLowerLeg.xRot = front * 0.42f;
		leftMiddleLowerLeg.xRot = -middle * 0.38f;
		rightMiddleLowerLeg.xRot = middle * 0.38f;
		leftBackLowerLeg.xRot = back * 0.34f;
		rightBackLowerLeg.xRot = -back * 0.34f;

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float pulse = Mth.sin(state.ageInTicks * 0.35f) * 0.12f;
		float antennaTwitch = Mth.sin(state.ageInTicks * 0.45f) * 0.11f;
		leftAntenna.zRot -= antennaTwitch;
		rightAntenna.zRot += antennaTwitch;
		leftAntennaTip.zRot -= antennaTwitch * 0.75f;
		rightAntennaTip.zRot += antennaTwitch * 0.75f;

		if (state.workState == AntWorkState.WORKING) {
			head.xRot += Mth.sin(state.ageInTicks * 0.9f) * 0.32f;
			leftMandible.yRot -= 0.14f;
			rightMandible.yRot += 0.14f;
			leftMandibleTip.yRot -= 0.12f;
			rightMandibleTip.yRot += 0.12f;
			leftFrontLeg.xRot = -0.95f + Mth.sin(state.ageInTicks * 0.9f) * 0.28f;
			rightFrontLeg.xRot = -0.95f - Mth.sin(state.ageInTicks * 0.9f) * 0.28f;
			abdomen.xRot = 0.1f + pulse;
		} else if (state.workState.name().startsWith("CARRYING")) {
			head.xRot -= 0.25f;
			body.xRot = -0.08f;
			leftFrontLeg.xRot = -1.2f;
			rightFrontLeg.xRot = -1.2f;
			abdomen.xRot = 0.08f;
		} else if (state.workState == AntWorkState.PATROLLING) {
			head.yRot += Mth.sin(state.ageInTicks * 0.18f) * 0.25f;
			abdomen.yRot = Mth.sin(state.ageInTicks * 0.12f) * 0.07f;
		} else {
			abdomen.xRot = pulse * 0.35f;
		}

		applyCasteSilhouette(state.caste, pulse);
	}

	private void resetAntPose() {
		head.xRot = 0.0f;
		head.yRot = 0.0f;
		body.xRot = 0.0f;
		body.yRot = 0.0f;
		petiole.xRot = 0.0f;
		postPetiole.xRot = 0.0f;
		abdomen.xRot = 0.0f;
		abdomen.yRot = 0.0f;

		leftAntenna.setRotation(-0.34f, 0.18f, -0.48f);
		rightAntenna.setRotation(-0.34f, -0.18f, 0.48f);
		leftAntennaTip.setRotation(-0.12f, 0.18f, -0.24f);
		rightAntennaTip.setRotation(-0.12f, -0.18f, 0.24f);
		leftMandible.setRotation(0.04f, -0.38f, 0.08f);
		rightMandible.setRotation(0.04f, 0.38f, -0.08f);
		leftMandibleTip.setRotation(0.0f, -0.42f, 0.0f);
		rightMandibleTip.setRotation(0.0f, 0.42f, 0.0f);

		leftFrontLeg.setRotation(-0.16f, 0.0f, -0.40f);
		rightFrontLeg.setRotation(-0.16f, 0.0f, 0.40f);
		leftMiddleLeg.setRotation(0.0f, 0.0f, -0.34f);
		rightMiddleLeg.setRotation(0.0f, 0.0f, 0.34f);
		leftBackLeg.setRotation(0.22f, 0.0f, -0.30f);
		rightBackLeg.setRotation(0.22f, 0.0f, 0.30f);
		leftFrontLowerLeg.setRotation(0.0f, 0.0f, -0.25f);
		rightFrontLowerLeg.setRotation(0.0f, 0.0f, 0.25f);
		leftMiddleLowerLeg.setRotation(0.0f, 0.0f, -0.25f);
		rightMiddleLowerLeg.setRotation(0.0f, 0.0f, 0.25f);
		leftBackLowerLeg.setRotation(0.0f, 0.0f, -0.25f);
		rightBackLowerLeg.setRotation(0.0f, 0.0f, 0.25f);
		leftWing.setRotation(0.12f, -0.20f, -0.22f);
		rightWing.setRotation(0.12f, 0.20f, 0.22f);

		resetScale(head);
		resetScale(body);
		resetScale(petiole);
		resetScale(postPetiole);
		resetScale(abdomen);
		resetScale(headCrest);
		resetScale(bodyPlate);
		resetScale(leftWing);
		resetScale(rightWing);
		resetScale(leftAntenna);
		resetScale(rightAntenna);
		resetScale(leftMandible);
		resetScale(rightMandible);
		resetScale(leftFrontLeg);
		resetScale(rightFrontLeg);
		resetScale(leftMiddleLeg);
		resetScale(rightMiddleLeg);
		resetScale(leftBackLeg);
		resetScale(rightBackLeg);
		headCrest.visible = false;
		bodyPlate.visible = false;
		leftWing.visible = false;
		rightWing.visible = false;
	}

	private void applyCasteSilhouette(AntCaste caste, float pulse) {
		switch (caste) {
			case WORKER -> {
				scalePart(abdomen, 1.06f, 1.02f, 1.08f);
				scalePart(leftAntenna, 0.96f, 1.08f, 0.96f);
				scalePart(rightAntenna, 0.96f, 1.08f, 0.96f);
			}
			case SCOUT -> {
				scalePart(head, 0.94f);
				scalePart(body, 0.88f, 0.98f, 0.90f);
				scalePart(petiole, 0.82f);
				scalePart(postPetiole, 0.82f);
				scalePart(abdomen, 0.78f, 0.88f, 0.84f);
				scalePart(leftAntenna, 0.88f, 1.28f, 0.88f);
				scalePart(rightAntenna, 0.88f, 1.28f, 0.88f);
				scaleLegs(0.74f, 1.10f, 0.74f);
			}
			case MINER -> {
				bodyPlate.visible = true;
				scalePart(head, 1.12f);
				scalePart(body, 1.08f, 1.02f, 1.05f);
				scalePart(leftMandible, 1.34f, 1.12f, 1.38f);
				scalePart(rightMandible, 1.34f, 1.12f, 1.38f);
				scalePart(leftFrontLeg, 1.10f);
				scalePart(rightFrontLeg, 1.10f);
			}
			case SOLDIER -> {
				headCrest.visible = true;
				bodyPlate.visible = true;
				scalePart(head, 1.20f, 1.08f, 1.16f);
				scalePart(body, 1.10f);
				scalePart(leftMandible, 1.55f, 1.18f, 1.62f);
				scalePart(rightMandible, 1.55f, 1.18f, 1.62f);
			}
			case MAJOR -> {
				headCrest.visible = true;
				bodyPlate.visible = true;
				scalePart(head, 1.38f, 1.18f, 1.30f);
				scalePart(body, 1.18f, 1.10f, 1.15f);
				scalePart(abdomen, 1.14f);
				scalePart(leftMandible, 1.88f, 1.28f, 2.0f);
				scalePart(rightMandible, 1.88f, 1.28f, 2.0f);
			}
			case GIANT -> {
				headCrest.visible = true;
				bodyPlate.visible = true;
				scalePart(head, 1.28f, 1.18f, 1.25f);
				scalePart(body, 1.25f, 1.18f, 1.24f);
				scalePart(abdomen, 1.34f, 1.22f, 1.38f);
				scalePart(leftMandible, 2.02f, 1.34f, 2.18f);
				scalePart(rightMandible, 2.02f, 1.34f, 2.18f);
				scaleLegs(1.12f, 1.08f, 1.12f);
			}
			case QUEEN -> {
				headCrest.visible = true;
				bodyPlate.visible = true;
				leftWing.visible = true;
				rightWing.visible = true;
				scalePart(head, 1.02f, 1.0f, 1.04f);
				scalePart(body, 1.15f, 1.10f, 1.18f);
				scalePart(petiole, 1.16f);
				scalePart(postPetiole, 1.22f);
				scalePart(abdomen, 1.42f, 1.20f + pulse * 0.08f, 1.58f);
				scaleLegs(0.92f, 0.96f, 0.92f);
				leftWing.zRot -= pulse * 0.08f;
				rightWing.zRot += pulse * 0.08f;
			}
		}
	}

	private void scaleLegs(float xScale, float yScale, float zScale) {
		scalePart(leftFrontLeg, xScale, yScale, zScale);
		scalePart(rightFrontLeg, xScale, yScale, zScale);
		scalePart(leftMiddleLeg, xScale, yScale, zScale);
		scalePart(rightMiddleLeg, xScale, yScale, zScale);
		scalePart(leftBackLeg, xScale, yScale, zScale);
		scalePart(rightBackLeg, xScale, yScale, zScale);
	}

	private static void resetScale(ModelPart part) {
		scalePart(part, 1.0f);
	}

	private static void scalePart(ModelPart part, float scale) {
		scalePart(part, scale, scale, scale);
	}

	private static void scalePart(ModelPart part, float xScale, float yScale, float zScale) {
		part.xScale = xScale;
		part.yScale = yScale;
		part.zScale = zScale;
	}
}
