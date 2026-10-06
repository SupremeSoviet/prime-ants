package dev.primeants.client;

import dev.primeants.entity.AntForm;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Geometry is in 32 units/block. The ant subtree is scaled 0.5, with 1 texel/unit. */
public final class AntModel extends EntityModel<AntRenderState> {
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 128;
    public static final String[] LEG_NAMES = {
            "leg_left_front", "leg_left_middle", "leg_left_hind",
            "leg_right_front", "leg_right_middle", "leg_right_hind"
    };
    private final ModelPart[] legs = new ModelPart[6];
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;
    private final boolean queen;

    public AntModel(ModelPart root) {
        super(root);
        ModelPart ant = root.getChild("ant");
        queen = ant.getChild("mesosoma").hasChild("wing_scar_-1_0");
        for (int i = 0; i < legs.length; i++) legs[i] = ant.getChild("mesosoma").getChild(LEG_NAMES[i]);
        leftAntenna = ant.getChild("head").getChild("antenna_left_scape");
        rightAntenna = ant.getChild("head").getChild("antenna_right_scape");
    }

    public static LayerDefinition createBodyLayer(AntForm form) {
        boolean queen = form == AntForm.QUEEN;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition ant = mesh.getRoot().addOrReplaceChild("ant", CubeListBuilder.create(),
                PartPose.offset(0, 24, 0).withScale(0.5F));
        float headZ = queen ? -23 : -10.5F;
        float headY = queen ? -13 : -8;
        float headW = queen ? 12 : 7;
        float headH = queen ? 9 : 6;
        float headD = queen ? 12 : 7;
        PartDefinition head = ant.addOrReplaceChild("head", faceted(0, 0, headW, headH, headD),
                PartPose.offset(0, headY, headZ));
        // A narrow neck physically connects the separate head and mesosoma contours.
        head.addOrReplaceChild("neck", box(40, 90, -headW * 0.14F, -headH * 0.2F, headD * 0.35F,
                headW * 0.28F, headH * 0.4F, queen ? 4 : 2), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            String name = side == 1 ? "left" : "right";
            head.addOrReplaceChild("eye_" + name, box(220, 0, -0.7F, -1, -1.5F, 1.4F, 2, 3),
                    PartPose.offset(side * headW / 2, -0.4F, -headD * 0.18F));
            float jawLength = queen ? 7 : 2;
            PartDefinition jaw = head.addOrReplaceChild("mandible_" + name,
                    box(190, 0, -0.7F, -0.6F, -jawLength, 1.4F, 1.2F, jawLength),
                    PartPose.offsetAndRotation(side * headW * 0.23F, 1.2F, -headD / 2, 0, -side * 0.22F, 0));
            jaw.addOrReplaceChild("tooth", box(190, 0, -0.8F, -0.6F, -0.5F, 1.6F, 1.2F, 1),
                    PartPose.offset(-side * 0.55F, 0, -jawLength + 0.7F));
            float scapeLength = queen ? 11 : 6;
            PartDefinition antenna = head.addOrReplaceChild("antenna_" + name + "_scape",
                    box(160, 48, -0.4F, -0.4F, -scapeLength, 0.8F, 0.8F, scapeLength),
                    PartPose.offsetAndRotation(side * headW * 0.16F, -headH * 0.25F, -headD * 0.42F, -0.15F, -side * 0.6F, 0));
            antenna.addOrReplaceChild("funiculus", box(160, 48, -0.35F, -0.35F, -(queen ? 9 : 5), 0.7F, 0.7F, queen ? 9 : 5),
                    PartPose.offsetAndRotation(0, 0, -scapeLength, 0.25F, side * 1.1F, 0));
        }

        float thoraxY = queen ? -13 : -8;
        float thoraxZ = queen ? -7 : -2;
        float thoraxW = queen ? 13 : 5;
        float thoraxH = queen ? 11 : 4.5F;
        float thoraxD = queen ? 20 : 10;
        PartDefinition mesosoma = ant.addOrReplaceChild("mesosoma",
                faceted(0, 48, thoraxW, thoraxH, thoraxD),
                PartPose.offset(0, thoraxY, thoraxZ));
        if (queen) {
            for (int side : new int[]{-1, 1}) {
                for (int i = 0; i < 2; i++) {
                    mesosoma.addOrReplaceChild("wing_scar_" + side + "_" + i,
                            box(220, 24, -0.5F, -1.1F, -2, 1, 2.2F, 4),
                            PartPose.offsetAndRotation(side * thoraxW * 0.39F, -2, -3 + i * 7, 0, 0, side * 0.12F));
                }
            }
        }
        // One anatomical node, with a slender continuous ventral stalk across the waist.
        ant.addOrReplaceChild("petiole", faceted(40, 90, queen ? 3.5F : 1.8F, queen ? 5 : 3.5F, queen ? 3 : 1.8F)
                .texOffs(40, 100).addBox(queen ? -0.75F : -0.4F, 0, queen ? -4 : -2,
                        queen ? 1.5F : 0.8F, queen ? 1.4F : 0.8F, queen ? 8 : 4),
                PartPose.offset(0, queen ? -11 : -7, queen ? 5 : 4));
        float gasterW = queen ? 18 : 8;
        float gasterH = queen ? 13 : 6.5F;
        float gasterD = queen ? 29 : 11;
        PartDefinition gaster = ant.addOrReplaceChild("gaster",
                faceted(64, 0, gasterW, gasterH, gasterD),
                PartPose.offset(0, queen ? -12 : -7.5F, queen ? 21.5F : 10.5F));
        gaster.addOrReplaceChild("acidopore", box(190, 24, -0.7F, -0.7F, 0, 1.4F, 1.4F, 0.7F),
                PartPose.offset(0, gasterH * 0.25F, gasterD / 2));

        for (int i = 0; i < 6; i++) {
            int side = i < 3 ? 1 : -1;
            int pair = i % 3;
            float femurLength = queen ? 11 : 7;
            float tibiaLength = queen ? 9 : 6;
            float thickness = queen ? 1.4F : 0.8F;
            float[] angles = jointAngles(queen, side, (pair - 1) * (queen ? 5 : 3), 0);
            PartDefinition hip = mesosoma.addOrReplaceChild(LEG_NAMES[i],
                    box(160, 0, -thickness / 2, -thickness / 2, -thickness / 2, thickness, thickness, thickness),
                    PartPose.offsetAndRotation(side * thoraxW * (pair == 1 ? 0.42F : 0.22F), 0, (pair - 1) * (queen ? 6 : 3.5F), 0, angles[0], 0));
            PartDefinition femur = hip.addOrReplaceChild("femur",
                    box(160, 0, -thickness / 2, 0, -thickness / 2, thickness, femurLength, thickness),
                    PartPose.rotation(0, 0, angles[1]));
            PartDefinition tibia = femur.addOrReplaceChild("tibia",
                    box(160, 24, -thickness * 0.4F, 0, -thickness * 0.4F, thickness * 0.8F, tibiaLength, thickness * 0.8F),
                    PartPose.offsetAndRotation(0, femurLength, 0, 0, 0, angles[2]));
            tibia.addOrReplaceChild("tarsus", box(190, 48, -thickness * 0.35F, 0, -thickness * 0.35F,
                    thickness * 0.7F, queen ? 2 : 1, thickness * 0.7F),
                    PartPose.offset(0, tibiaLength, 0));
        }
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    /** Stepped ovoid: narrow ends, broad equator, bevelled top/bottom contours.
     * Overlapping primitives share one anatomical segment and maintain physical continuity. */
    private static CubeListBuilder faceted(int u, int v, float w, float h, float d) {
        CubeListBuilder shape = CubeListBuilder.create().texOffs(u, v);
        float[] widths = {0.48F, 0.80F, 1F, 0.80F, 0.48F};
        float[] heights = {0.55F, 0.85F, 1F, 0.85F, 0.55F};
        for (int i = 0; i < 5; i++) {
            float sw = w * widths[i], sh = h * heights[i];
            float z = -d / 2 + i * d / 5;
            shape.addBox(-sw / 2, -sh * 0.30F, z, sw, sh * 0.60F, d / 5);
            shape.addBox(-sw * 0.38F, -sh / 2, z, sw * 0.76F, sh, d / 5);
        }
        return shape;
    }

    /** Two-link IK in a hip-yawed radial plane. Tibia plus distal tarsus is
     * the second link. Hip never translates, and every joint remains connected. */
    private static float[] jointAngles(boolean queen, int side, float longitudinal, float lift) {
        double lateral = queen ? 12 : 7.5;
        double down = (queen ? 13 : 8) - lift;
        double radial = Math.hypot(lateral, longitudinal);
        double distance = Math.hypot(radial, down);
        double femur = queen ? 11 : 7, distal = queen ? 11 : 7;
        double shoulder = Math.atan2(radial, down) + Math.acos((femur * femur + distance * distance - distal * distal) / (2 * femur * distance));
        double knee = Math.PI - Math.acos((femur * femur + distal * distal - distance * distance) / (2 * femur * distal));
        return new float[]{(float)(side * Math.atan2(-longitudinal, lateral)), (float)(-side * shoulder), (float)(side * knee)};
    }

    /** Tripod A: left front/hind + right middle. B is the complementary group. */
    public static int tripodSign(int leg) {
        return leg == 0 || leg == 2 || leg == 4 ? 1 : -1;
    }

    @Override
    public void setupAnim(AntRenderState state) {
        super.setupAnim(state);
        float amplitude = state.moving ? Math.min(1, state.walkAnimationSpeed) : 0;
        double cycle = state.walkAnimationPos * 2.8 / (2 * Math.PI);
        cycle -= Math.floor(cycle);
        for (int i = 0; i < 6; i++) {
            if (amplitude == 0) continue;
            double t = (cycle + (tripodSign(i) == 1 ? 0 : 0.5)) % 1;
            // Each stance traverses front -> rear monotonically. The other
            // tripod returns rear -> front above ground, then roles exchange.
            // Adult vanilla walk position advances at four times travelled distance.
            // A constant distance-based stride avoids multiplying speed twice;
            // slowing the ant slows cadence instead of making planted feet slide.
            float stride = (float)((t < 0.5 ? -1 + 4 * t : 3 - 4 * t) * (4 * Math.PI / 2.8));
            float lift = t < 0.5 ? 0 : (float)(Math.sin((t - 0.5) * 2 * Math.PI)
                    * (queen ? 0.8 + 0.7 * amplitude : 0.6 + 0.4 * amplitude));
            float[] angles = jointAngles(queen, i < 3 ? 1 : -1, (i % 3 - 1) * (queen ? 5 : 3) + stride, lift);
            legs[i].yRot = angles[0];
            ModelPart femur = legs[i].getChild("femur");
            femur.zRot = angles[1];
            femur.getChild("tibia").zRot = angles[2];
        }
        if(state.social||state.biting){
            var head=root().getChild("ant").getChild("head");
            head.yRot=state.yRot*(float)Math.PI/180;
            head.xRot+=0.08F;
            float pulse=(float)Math.sin(state.ageInTicks*.5F)*.10F;
            head.getChild("mandible_left").yRot+=.18F+pulse;
            head.getChild("mandible_right").yRot-=.18F+pulse;
        }
        leftAntenna.yRot += (float)Math.sin(state.ageInTicks * 0.09F) * 0.12F;
        rightAntenna.yRot -= (float)Math.sin(state.ageInTicks * 0.09F + 0.7F) * 0.12F;
    }
}
