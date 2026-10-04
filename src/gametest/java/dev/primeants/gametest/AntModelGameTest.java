package dev.primeants.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.primeants.PrimeAnts;
import dev.primeants.client.AntModel;
import dev.primeants.client.AntRenderState;
import dev.primeants.entity.AntForm;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Tests the factory registered by PrimeAntsClient, using isolated baked models, never capture poses. */
public final class AntModelGameTest implements FabricClientGameTest {
    private final StringBuilder xml = new StringBuilder();
    private int cases;
    private int failures;

    @Override
    public void runTest(ClientGameTestContext context) {
        try {
            context.runOnClient(client -> {
                for (AntForm form : AntForm.values()) checkForm(form);
            });
        } finally {
            try {
                Path path = Path.of(System.getProperty("prime_ants.modelReport"));
                Files.createDirectories(path.getParent());
                Files.writeString(path, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><testsuite name=\"production-ant-model\" tests=\""
                        + cases + "\" failures=\"" + failures + "\" skipped=\"0\">" + xml + "</testsuite>");
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        PrimeAnts.LOGGER.info("T02 production model suite: {} cases, {} failures", cases, failures);
    }

    private void test(AntForm form, String name, Runnable check) {
        cases++;
        xml.append("<testcase classname=\"AntModel.").append(form).append("\" name=\"").append(name).append("\">");
        try { check.run(); }
        catch (Throwable failure) {
            failures++;
            xml.append("<failure message=\"").append(failure.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")).append("\"/>");
            throw failure;
        } finally { xml.append("</testcase>"); }
    }

    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void near(float actual, float expected) { require(Math.abs(actual - expected) < 0.001F, "Expected " + expected + ", got " + actual); }

    private void checkForm(AntForm form) {
        AntModel model = new AntModel(AntModel.createBodyLayer(form).bakeRoot());
        ModelPart ant = model.root().getChild("ant");
        ModelPart head = ant.getChild("head");
        ModelPart mesosoma = ant.getChild("mesosoma");
        Map<String, Integer> cubes = new LinkedHashMap<>();
        model.root().visit(new PoseStack(), (pose, path, index, cube) -> cubes.merge(path, 1, Integer::sum));
        test(form, "sixArticulatedLegsOnMesosoma", () -> {
            require(cubes.keySet().stream().filter(path -> path.matches("/ant/mesosoma/leg_[^/]+$")).count() == 6, "Exactly six geometry-bearing legs");
            for (String leg : AntModel.LEG_NAMES) {
                require(cubes.containsKey("/ant/mesosoma/" + leg + "/tibia/tarsus"), "Femur, tibia and tarsus must contain rendered geometry");
            }
        });
        test(form, "elbowedAntennaeMandiblesAndEyes", () -> {
            for (String side : new String[]{"left", "right"}) {
                ModelPart scape = head.getChild("antenna_" + side + "_scape");
                require(Math.abs(scape.getChild("funiculus").yRot) > 0.8, "Rendered distal antenna must form an elbow");
                require(cubes.containsKey("/ant/head/antenna_" + side + "_scape/funiculus"), "Antenna elbow must have geometry");
                require(cubes.containsKey("/ant/head/mandible_" + side) && cubes.containsKey("/ant/head/eye_" + side), "Mandible and compound-eye geometry");
            }
        });
        test(form, "onePetioleAndBodySegments", () -> {
            require(cubes.keySet().stream().filter(path -> path.contains("petiole")).count() == 1, "Formicine has one petiole segment");
            for (String segment : new String[]{"head", "mesosoma", "petiole", "gaster"}) require(cubes.containsKey("/ant/" + segment), "Missing rendered body segment");
            long scars = cubes.keySet().stream().filter(path -> path.contains("wing_scar")).count();
            require(scars == (form == AntForm.QUEEN ? 4 : 0), "Queen wing scars only");
        });
        test(form, "tripodsAlternateInActualSetupAnim", () -> {
            for (int phaseSign : new int[]{1, -1}) {
                AntRenderState state = new AntRenderState();
                state.moving = true;
                state.walkAnimationSpeed = 1;
                state.walkAnimationPos = phaseSign * (float)Math.PI / 2 / 2.8F;
                model.setupAnim(state);
                for (int i = 0; i < 6; i++) {
                    ModelPart leg = mesosoma.getChild(AntModel.LEG_NAMES[i]);
                    int intendedSign = i == 0 || i == 2 || i == 4 ? 1 : -1;
                    near(leg.yRot - leg.getInitialPose().yRot(), phaseSign * intendedSign * 0.45F);
                    boolean lifted = Math.abs(leg.getChild("tibia").zRot - leg.getChild("tibia").getInitialPose().zRot()) > 0.1;
                    require(lifted == (phaseSign * intendedSign > 0), "Only the swing tripod must lift");
                }
            }
        });
        test(form, "stationaryLegsStopWhileAntennaeAnimate", () -> {
            AntRenderState state = new AntRenderState();
            state.walkAnimationPos = 10;
            state.walkAnimationSpeed = 1;
            state.moving = false;
            state.ageInTicks = 13;
            model.setupAnim(state);
            float antenna = head.getChild("antenna_left_scape").yRot;
            for (String name : AntModel.LEG_NAMES) {
                ModelPart leg = mesosoma.getChild(name);
                near(leg.yRot, leg.getInitialPose().yRot());
                near(leg.zRot, leg.getInitialPose().zRot());
                near(leg.getChild("tibia").zRot, leg.getChild("tibia").getInitialPose().zRot());
            }
            state.ageInTicks = 27;
            model.setupAnim(state);
            require(Math.abs(head.getChild("antenna_left_scape").yRot - antenna) > 0.01, "Antennae must remain active while stationary");
        });
        test(form, "bakedUvDensityIs32TexelsPerWorldBlock", () -> {
            model.resetPose();
            model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
                for (var polygon : cube.polygons) {
                    var vertices = polygon.vertices();
                    for (int i = 0; i < 4; i++) {
                        var a = vertices[i];
                        var b = vertices[(i + 1) % 4];
                        Vector3f wa = pose.pose().transformPosition(new Vector3f(a.worldX(), a.worldY(), a.worldZ()));
                        Vector3f wb = pose.pose().transformPosition(new Vector3f(b.worldX(), b.worldY(), b.worldZ()));
                        double texels = Math.hypot((a.u() - b.u()) * AntModel.TEXTURE_WIDTH, (a.v() - b.v()) * AntModel.TEXTURE_HEIGHT);
                        require(Math.abs(texels / wa.distance(wb) - 32) < 0.02, "Baked UV/world density must be 32: " + path);
                    }
                }
            });
        });
        test(form, "renderedBodyDimensionsMatchForm", () -> {
            model.resetPose();
            double[] bounds = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
            model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
                if (path.contains("antenna") || path.contains("leg_")) return;
                for (var polygon : cube.polygons) for (var vertex : polygon.vertices()) {
                    Vector3f p = pose.pose().transformPosition(new Vector3f(vertex.worldX(), vertex.worldY(), vertex.worldZ()));
                    bounds[0] = Math.min(bounds[0], p.z); bounds[1] = Math.max(bounds[1], p.z);
                    bounds[2] = Math.min(bounds[2], p.y); bounds[3] = Math.max(bounds[3], p.y);
                }
            });
            double length = bounds[1] - bounds[0];
            require(form == AntForm.WORKER ? length > 0.95 && length < 1.1 : length >= 2 && length <= 2.5, "Rendered body length " + length);
            require(bounds[3] - bounds[2] < 1, "Low anatomy compatible with a future two-block-high passage");
            PrimeAnts.LOGGER.info("T02 rendered dimensions {}: bodyLength={}, bodyHeight={}, excludes antenna/leg reach", form, length, bounds[3] - bounds[2]);
        });
        model.resetPose();
    }
}
