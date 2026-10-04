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

    public AntModel(ModelPart root) {
        super(root);
        ModelPart ant = root.getChild("ant");
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
        PartDefinition head = ant.addOrReplaceChild("head", box(0, 0, -headW / 2, -headH / 2, -headD / 2, headW, headH, headD),
                PartPose.offset(0, headY, headZ));
        // Smaller crown and cheeks soften the cuboid silhouette while retaining a Minecraft mesh.
        head.addOrReplaceChild("crown", box(0, 0, -headW * 0.38F, -headH / 2 - 0.5F, -headD * 0.35F,
                headW * 0.76F, 1, headD * 0.7F), PartPose.ZERO);
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
                    PartPose.offsetAndRotation(side * headW * 0.27F, -headH * 0.25F, -headD * 0.42F, -0.15F, -side * 0.6F, 0));
            antenna.addOrReplaceChild("funiculus", box(160, 48, -0.35F, -0.35F, -(queen ? 9 : 5), 0.7F, 0.7F, queen ? 9 : 5),
                    PartPose.offsetAndRotation(0, 0, -scapeLength, 0.25F, side * 1.1F, 0));
        }

        float thoraxY = queen ? -13 : -8;
        float thoraxZ = queen ? -7 : -2;
        float thoraxW = queen ? 13 : 5;
        float thoraxH = queen ? 11 : 4.5F;
        float thoraxD = queen ? 20 : 10;
        PartDefinition mesosoma = ant.addOrReplaceChild("mesosoma",
                box(0, 48, -thoraxW / 2, -thoraxH / 2, -thoraxD / 2, thoraxW, thoraxH, thoraxD),
                PartPose.offset(0, thoraxY, thoraxZ));
        mesosoma.addOrReplaceChild("pronotum", box(0, 48, -thoraxW * 0.35F, -thoraxH / 2 - 1,
                -thoraxD * 0.35F, thoraxW * 0.7F, 2, thoraxD * 0.55F), PartPose.ZERO);
        if (queen) {
            for (int side : new int[]{-1, 1}) {
                for (int i = 0; i < 2; i++) {
                    mesosoma.addOrReplaceChild("wing_scar_" + side + "_" + i,
                            box(220, 24, -0.5F, -1.1F, -2, 1, 2.2F, 4),
                            PartPose.offsetAndRotation(side * 6.6F, -2, -3 + i * 7, 0, 0, side * 0.12F));
                }
            }
        }
        ant.addOrReplaceChild("petiole", box(40, 90, queen ? -2 : -1, queen ? -3 : -2, queen ? -2 : -1,
                queen ? 4 : 2, queen ? 6 : 4, queen ? 4 : 2), PartPose.offset(0, queen ? -11 : -7, queen ? 5 : 4));
        float gasterW = queen ? 18 : 8;
        float gasterH = queen ? 13 : 6.5F;
        float gasterD = queen ? 29 : 11;
        PartDefinition gaster = ant.addOrReplaceChild("gaster",
                box(64, 0, -gasterW / 2, -gasterH / 2, -gasterD / 2, gasterW, gasterH, gasterD),
                PartPose.offset(0, queen ? -12 : -7.5F, queen ? 21.5F : 10.5F));
        gaster.addOrReplaceChild("dorsum", box(64, 0, -gasterW * 0.38F, -gasterH / 2 - 0.6F, -gasterD * 0.36F,
                gasterW * 0.76F, 1.2F, gasterD * 0.72F), PartPose.ZERO);
        gaster.addOrReplaceChild("acidopore", box(190, 24, -0.7F, -0.7F, 0, 1.4F, 1.4F, 0.7F),
                PartPose.offset(0, gasterH * 0.25F, gasterD / 2));

        for (int i = 0; i < 6; i++) {
            int side = i < 3 ? 1 : -1;
            int pair = i % 3;
            float femurLength = queen ? 10 : 6;
            float tibiaLength = queen ? 8 : 5;
            float thickness = queen ? 1.4F : 0.8F;
            PartDefinition femur = mesosoma.addOrReplaceChild(LEG_NAMES[i],
                    box(160, 0, -thickness / 2, 0, -thickness / 2, thickness, femurLength, thickness),
                    PartPose.offsetAndRotation(side * thoraxW * 0.42F, 0, (pair - 1) * (queen ? 6 : 3.5F),
                            0, side * (1 - pair) * 0.55F, -side * 1.2F));
            PartDefinition tibia = femur.addOrReplaceChild("tibia",
                    box(160, 24, -thickness * 0.4F, 0, -thickness * 0.4F, thickness * 0.8F, tibiaLength, thickness * 0.8F),
                    PartPose.offsetAndRotation(0, femurLength, 0, 0, 0, side * 0.9F));
            tibia.addOrReplaceChild("tarsus", box(190, 48, -thickness * 0.35F, 0, -thickness * 0.35F,
                    thickness * 0.7F, queen ? 2 : 1, thickness * 0.7F),
                    PartPose.offsetAndRotation(0, tibiaLength, 0, 0, 0, side * 0.3F));
        }
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    /** Tripod A: left front/hind + right middle. B is the complementary group. */
    public static int tripodSign(int leg) {
        return leg == 0 || leg == 2 || leg == 4 ? 1 : -1;
    }

    @Override
    public void setupAnim(AntRenderState state) {
        super.setupAnim(state);
        float amplitude = state.moving ? Math.min(1, state.walkAnimationSpeed) : 0;
        float phase = state.walkAnimationPos * 2.8F;
        for (int i = 0; i < 6; i++) {
            float swing = (float)Math.sin(phase) * tripodSign(i) * amplitude;
            legs[i].yRot += swing * 0.45F;
            // The swing tripod lifts while the opposite tripod bears weight.
            legs[i].zRot += (i < 3 ? -1 : 1) * Math.max(0, swing) * 0.25F;
            legs[i].getChild("tibia").zRot += (i < 3 ? 1 : -1) * Math.max(0, swing) * 0.3F;
        }
        leftAntenna.yRot += (float)Math.sin(state.ageInTicks * 0.09F) * 0.12F;
        rightAntenna.yRot -= (float)Math.sin(state.ageInTicks * 0.09F + 0.7F) * 0.12F;
    }
}
