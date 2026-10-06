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
    public static final int TEXTURE_WIDTH = 512;
    public static final int TEXTURE_HEIGHT = 512;
    public static final float MANDIBLE_HEIGHT = 0.65F;
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
        float headZ = queen ? -23 : -10.75F;
        float headY = queen ? -14 : -8.5F;
        float headW = queen ? 10.5F : 7.3F;
        float headH = queen ? 9 : 6.8F;
        float headD = headDepth(queen);
        PartDefinition head = ant.addOrReplaceChild("head", ovoid(0, headW, headH, headD),
                PartPose.offset(0, headY, headZ));
        // A narrow neck physically connects the separate head and mesosoma contours.
        head.addOrReplaceChild("neck", box(320, 400, -headW * 0.14F, -headH * 0.2F, headD * 0.35F,
                headW * 0.28F, headH * 0.4F, queen ? 4 : 2), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            String name = side == 1 ? "left" : "right";
            head.addOrReplaceChild("eye_" + name, eye(queen),
                    PartPose.offset(side * headW * 0.45F, -0.4F, -headD * 0.18F));
            float jawLength = jawLength(queen);
            PartDefinition jaw = head.addOrReplaceChild("mandible_" + name,
                    box(160, 400, -0.7F, -0.6F, -jawLength, 1.4F, 1.2F, jawLength),
                    PartPose.offsetAndRotation(side * headW * 0.12F, MANDIBLE_HEIGHT, -headD / 2, 0, -side * 0.22F, 0));
            jaw.addOrReplaceChild("tooth", box(160, 400, -0.8F, -0.6F, -0.5F, 1.6F, 1.2F, 1),
                    PartPose.offset(-side * 0.55F, 0, -jawLength + 0.7F));
            float scapeLength = queen ? 11 : 6;
            PartDefinition antenna = head.addOrReplaceChild("antenna_" + name + "_scape",
                    box(64, 400, -0.4F, -0.4F, -scapeLength, 0.8F, 0.8F, scapeLength),
                    PartPose.offsetAndRotation(side * headW * 0.16F, -headH * 0.23F, -headD * 0.28F, -0.15F, -side * 0.6F, 0));
            antenna.addOrReplaceChild("funiculus", box(64, 400, -0.35F, -0.35F, -(queen ? 9 : 5), 0.7F, 0.7F, queen ? 9 : 5),
                    PartPose.offsetAndRotation(0, 0, -scapeLength, 0.25F, side * 1.1F, 0));
        }

        float thoraxY = queen ? -14 : -8.5F;
        float thoraxZ = queen ? -8 : -2.25F;
        float thoraxW = queen ? 13 : 5;
        float thoraxH = queen ? 12 : 4.5F;
        float thoraxD = queen ? 19 : 10;
        PartDefinition mesosoma = ant.addOrReplaceChild("mesosoma",
                ovoid(96, thoraxW, thoraxH, thoraxD),
                PartPose.offset(0, thoraxY, thoraxZ));
        if (queen) {
            for (int side : new int[]{-1, 1}) {
                for (int i = 0; i < 2; i++) {
                    mesosoma.addOrReplaceChild("wing_scar_" + side + "_" + i,
                            box(260, 400, -0.5F, -1.1F, -2, 1, 2.2F, 4),
                            PartPose.offsetAndRotation(side * thoraxW * 0.48F, -0.8F, -3 + i * 6, 0, 0, side * 0.12F));
                }
            }
        }
        // One anatomical node, with a slender continuous ventral stalk across the waist.
        ant.addOrReplaceChild("petiole", ovoid(288, queen ? 2.4F : 1.3F, queen ? 5 : 3.1F, queen ? 1.8F : 1.2F)
                 .texOffs(320, 430).addBox(queen ? -0.75F : -0.4F, 0, queen ? -3 : -2,
                        queen ? 1.5F : 0.8F, queen ? 1.4F : 0.8F, queen ? 6 : 4),
                PartPose.offset(0, queen ? -12 : -8.2F, queen ? 4 : 4));
        float gasterW = queen ? 19 : 9;
        float gasterH = queen ? 15 : 7.4F;
        float gasterD = queen ? 31 : 12.5F;
        PartDefinition gaster = ant.addOrReplaceChild("gaster",
                ovoid(192, gasterW, gasterH, gasterD),
                PartPose.offset(0, queen ? -14 : -8.5F, queen ? 22.5F : 11.25F));
        gaster.addOrReplaceChild("acidopore", box(380, 400, -0.7F, -0.7F, 0, 1.4F, 1.4F, 0.7F),
                PartPose.offset(0, gasterH * 0.10F, gasterD / 2));

        for (int i = 0; i < 6; i++) {
            int side = i < 3 ? 1 : -1;
            int pair = i % 3;
            float femurLength = queen ? 9 : 5.8F;
            float tibiaLength = queen ? 14 : 9;
            float thickness = queen ? 1.1F : 0.65F;
            float[] angles = jointAngles(queen, side, (pair - 1) * (queen ? 5 : 3), 0);
            PartDefinition hip = mesosoma.addOrReplaceChild(LEG_NAMES[i],
                    box(0, 400, -thickness / 2, -thickness / 2, -thickness / 2, thickness, thickness, thickness),
                    PartPose.offsetAndRotation(side * thoraxW * (pair == 1 ? 0.42F : 0.22F), 0, (pair - 1) * (queen ? 6 : 3.5F), 0, angles[0], 0));
            PartDefinition femur = hip.addOrReplaceChild("femur",
                    box(0, 400, -thickness / 2, 0, -thickness / 2, thickness, femurLength, thickness),
                    PartPose.rotation(0, 0, angles[1]));
            PartDefinition tibia = femur.addOrReplaceChild("tibia",
                    box(0, 430, -thickness * 0.4F, 0, -thickness * 0.4F, thickness * 0.8F, tibiaLength, thickness * 0.8F),
                    PartPose.offsetAndRotation(0, femurLength, 0, 0, 0, angles[2]));
            tibia.addOrReplaceChild("tarsus", box(32, 430, -thickness * 0.35F, 0, -thickness * 0.35F,
                    thickness * 0.7F, queen ? 2 : 1.2F, thickness * 0.7F),
                    PartPose.offsetAndRotation(0, tibiaLength, 0, 0, 0, -angles[1] - angles[2]));
        }
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    /** Small faceted compound eye, with a rounded outline rather than a rectangular slab. */
    private static CubeListBuilder eye(boolean queen) {
        float size = queen ? 1.3F : 1F;
        return box(220, 400, -.55F * size, -.55F * size, -1.1F * size, 1.1F * size, 1.1F * size, 2.2F * size)
                .addBox(-.45F * size, -.8F * size, -.8F * size, .9F * size, 1.6F * size, 1.6F * size);
    }

    /** Nine axial slices, each with five horizontal tiers: an elliptical cross-section,
     * not overlapping crosses. Each surface owns a UV island, with one texel/raw unit.
     * Cells (52 x 18) agree with generate-ant-textures.py; no UV-scale shortcut. */
    private static CubeListBuilder ovoid(int v, float w, float h, float d) {
        CubeListBuilder shape = CubeListBuilder.create();
        float[] taper = {0.34F, 0.68F, 0.88F, 0.98F, 1F, 0.98F, 0.88F, 0.68F, 0.34F};
        float[] widths = {0.50F, 0.84F, 1F, 0.84F, 0.50F};
        float[] edges = {-0.50F, -0.36F, -0.15F, 0.15F, 0.36F, 0.50F};
        for (int slice = 0; slice < taper.length; slice++) {
            float sw = w * taper[slice], sh = h * taper[slice];
            for (int tier = 0; tier < widths.length; tier++) {
                float width = sw * widths[tier];
                shape.texOffs(slice * 52, v + tier * 18).addBox(-width / 2, sh * edges[tier],
                        -d / 2 + slice * d / 9, width, sh * (edges[tier + 1] - edges[tier]), d / 9);
            }
        }
        return shape;
    }

    public static float headDepth(boolean queen) { return queen ? 11 : 7.5F; }
    public static float jawLength(boolean queen) { return queen ? 4 : 2.3F; }
    /** The midpoint of the real jaw tips, in animated head-local raw model units. */
    public static float carriedItemForward(boolean queen) {
        return -headDepth(queen) / 2 - jawLength(queen) * (float)Math.cos(0.22);
    }

    /** Two links to the ankle, with a vertical tarsus. Femur rises to a knee;
     * tibia descends. The flat distal face stays on the floor during stance. */
    private static float[] jointAngles(boolean queen, int side, float longitudinal, float lift) {
        double lateral = queen ? 8 : 5;
        double down = (queen ? 14 - 2 : 8.5 - 1.2) - lift;
        double radial = Math.hypot(lateral, longitudinal);
        double distance = Math.hypot(radial, down);
        double femur = queen ? 9 : 5.8, distal = queen ? 14 : 9;
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
            ModelPart tibia = femur.getChild("tibia");
            tibia.zRot = angles[2];
            tibia.getChild("tarsus").zRot = -femur.zRot - tibia.zRot;
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
