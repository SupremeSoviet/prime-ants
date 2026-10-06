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
    private final StringBuilder traces = new StringBuilder("form,amplitude,walk_position,cycle_fraction,leg,tripod,interval_role,x,y,z,delta_z,clearance\n");
    private final StringBuilder profiles = new StringBuilder("form,segment,z,width,height,min_y,max_y\n");
    public void writeFootTrace(Path path) {
        try { Files.writeString(path, traces); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        AntModelGameTest suite = new AntModelGameTest();
        try { for (AntForm form : AntForm.values()) {
            if (args.length > 0 && args[0].equals("--profiles-only"))
                suite.test(form, "profilesOnly", () -> suite.checkProfiles(form, new AntModel(AntModel.createBodyLayer(form).bakeRoot())));
            else suite.checkForm(form);
        } }
        finally { suite.save(); }
        if (suite.failures > 0) throw new AssertionError(suite.failures + " production geometry cases failed");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try {
            context.runOnClient(client -> {
                for (AntForm form : AntForm.values()) checkForm(form);
            });
        } finally {
            save();
        }
        if (failures > 0) throw new AssertionError(failures + " production geometry cases failed");
        PrimeAnts.LOGGER.info("T03 production model suite: {} cases, {} failures", cases, failures);
    }

    private void save() {
            try {
                Path path = Path.of(System.getProperty("prime_ants.modelReport"));
                Files.createDirectories(path.getParent());
                Files.writeString(path, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><testsuite name=\"production-ant-model\" tests=\""
                        + cases + "\" failures=\"" + failures + "\" skipped=\"0\"><properties><property name=\"run_id\" value=\""
                        + System.getProperty("prime_ants.runId") + "\"/><property name=\"entrypoint\" value=\"dev.primeants.gametest.AntModelGameTest\"/></properties>" + xml + "</testsuite>");
                Files.writeString(Path.of(System.getProperty("prime_ants.footTrace", path.resolveSibling("foot-motion.csv").toString())), traces);
                Files.writeString(path.resolveSibling(path.getFileName() + "-profiles.csv"), profiles);
            } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void test(AntForm form, String name, Runnable check) {
        cases++;
        xml.append("<testcase classname=\"AntModel.").append(form).append("\" name=\"").append(name).append("\">");
        try { check.run(); }
        catch (Throwable failure) {
            failures++;
            xml.append("<failure message=\"").append(failure.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")).append("\"/>");
            PrimeAnts.LOGGER.error("Geometry regression {} {}: {}", form, name, failure.toString());
        } finally { xml.append("</testcase>"); }
    }

    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void near(float actual, float expected) { require(Math.abs(actual - expected) < 0.001F, "Expected " + expected + ", got " + actual); }

    /** Body-relative +Z rearward, +Y down. visit composes every production
     * T * Rz * Ry * Rx * S including the 0.5 ant scale. Contact is the centre
     * of the rendered tarsus cube's distal max-Y face. Units are world blocks.
     * LivingEntityRenderer flips X/Y, retains Z, then rotates body heading.
     */
    public static Vector3f[] contacts(AntModel model) {
        Vector3f[] points = new Vector3f[6];
        model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
            if (!path.endsWith("/tarsus")) return;
            for (int i = 0; i < 6; i++) if (path.contains("/" + AntModel.LEG_NAMES[i] + "/")) {
                points[i] = pose.pose().transformPosition(new Vector3f(
                        (cube.minX + cube.maxX) / 32F, cube.maxY / 16F, (cube.minZ + cube.maxZ) / 32F));
            }
        });
        for (Vector3f point : points) if (point == null) throw new AssertionError("Missing rendered distal tarsus");
        return points;
    }

    public void checkFootMotion(AntForm form, float amplitude) {
        AntModel model = new AntModel(AntModel.createBodyLayer(form).bakeRoot());
        AntRenderState state = new AntRenderState();
        state.moving = true;
        state.walkAnimationSpeed = amplitude;
        Vector3f[] previous = null;
        float previousWalkPosition = 0;
        StringBuilder errors = new StringBuilder();
        final float tolerance = 0.000002F;
        for (int sample = 0; sample <= 64; sample++) {
            float fraction = sample / 64F;
            state.walkAnimationPos = fraction * (float)(2 * Math.PI / 2.8);
            model.setupAnim(state);
            Vector3f[] feet = contacts(model);
            for (int leg = 0; leg < 6; leg++) {
                boolean tripodA = leg == 0 || leg == 2 || leg == 4;
                boolean stance = (sample <= 32) == tripodA;
                float dz = previous == null ? 0 : feet[leg].z - previous[leg].z;
                float clearance = 1.5F - feet[leg].y;
                require(Float.isFinite(feet[leg].x) && Float.isFinite(feet[leg].y) && Float.isFinite(feet[leg].z), "Nonfinite foot transform");
                traces.append(String.format(java.util.Locale.ROOT, "%s,%.8f,%.8f,%.6f,%s,%s,%s,%.8f,%.8f,%.8f,%.8f,%.8f%n",
                        form, amplitude, state.walkAnimationPos, fraction, AntModel.LEG_NAMES[leg], tripodA ? "A" : "B",
                        stance ? "stance" : "return", feet[leg].x, feet[leg].y, feet[leg].z, dz, clearance));
                if (sample > 0 && (stance ? dz <= tolerance : dz >= -tolerance))
                    errors.append("sample=").append(sample).append(" leg=").append(leg).append(" stance=").append(stance).append(" dz=").append(dz).append("; ");
                // Independent gameplay contract from resolved LivingEntity.updateWalkAnimation:
                // adult walk distance advances by 4 * horizontal body distance. Do not
                // multiply stride by speed again, which produces severe sliding at low speed.
                if (sample > 0 && stance && Math.abs(dz - (state.walkAnimationPos - previousWalkPosition) / 4F) > tolerance)
                    errors.append("stance sweep inconsistent with vanilla walk distance leg=").append(leg).append("; ");
                if (sample != 0 && sample != 32 && sample != 64) {
                    if (stance && Math.abs(clearance) > 0.00001F) errors.append("stance off ground leg=").append(leg).append("; ");
                    if (!stance && clearance < amplitude * 0.0005F) errors.append("return no clearance leg=").append(leg).append("; ");
                }
            }
            previous = feet;
            previousWalkPosition = state.walkAnimationPos;
        }
        require(errors.isEmpty(), "Transformed foot trajectory: " + errors);
    }

    private void checkForm(AntForm form) {
        test(form, "trophallaxisUsesExistingHeadAndMandiblesAndClears", () -> {
            var model=new AntModel(AntModel.createBodyLayer(form).bakeRoot());var state=new AntRenderState();
            model.setupAnim(state);var head=model.root().getChild("ant").getChild("head");float left=head.getChild("mandible_left").yRot,right=head.getChild("mandible_right").yRot;
            state.social=true;state.yRot=30;state.ageInTicks=4;model.setupAnim(state);
            require(head.getChild("mandible_left").yRot>left&&head.getChild("mandible_right").yRot<right,"Actual synchronized social state opens both existing mandibles");near(head.yRot,(float)Math.PI/6);
            state.social=false;state.yRot=0;model.setupAnim(state);near(head.getChild("mandible_left").yRot,left);near(head.getChild("mandible_right").yRot,right);near(head.xRot,0);near(head.yRot,0);
        });
        AntModel model = new AntModel(AntModel.createBodyLayer(form).bakeRoot());
        ModelPart ant = model.root().getChild("ant");
        ModelPart head = ant.getChild("head");
        ModelPart mesosoma = ant.getChild("mesosoma");
        Map<String, Integer> cubes = new LinkedHashMap<>();
        model.root().visit(new PoseStack(), (pose, path, index, cube) -> cubes.merge(path, 1, Integer::sum));
        test(form, "sixArticulatedLegsOnMesosoma", () -> {
            require(cubes.keySet().stream().filter(path -> path.matches("/ant/mesosoma/leg_[^/]+$")).count() == 6, "Exactly six geometry-bearing legs");
            for (String leg : AntModel.LEG_NAMES) {
                require(cubes.containsKey("/ant/mesosoma/" + leg + "/femur") && cubes.containsKey("/ant/mesosoma/" + leg + "/femur/tibia/tarsus"), "Femur, tibia and tarsus must contain rendered geometry");
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
        test(form, "articulatedLegGeometryStaysConnected", () -> {
            for (int sample = 0; sample <= 8; sample++) {
                AntRenderState state = new AntRenderState();
                state.moving = true; state.walkAnimationSpeed = 1;
                state.walkAnimationPos = sample * (float)(2 * Math.PI / 2.8 / 8);
                model.setupAnim(state);
                Map<String, Vector3f[]> endpoints = new LinkedHashMap<>();
                java.util.List<double[]> thoraxBoxes = new java.util.ArrayList<>();
                model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
                    if (path.equals("/ant/mesosoma")) {
                        Vector3f lo = pose.pose().transformPosition(new Vector3f(cube.minX/16F,cube.minY/16F,cube.minZ/16F));
                        Vector3f hi = pose.pose().transformPosition(new Vector3f(cube.maxX/16F,cube.maxY/16F,cube.maxZ/16F));
                        thoraxBoxes.add(new double[]{lo.x,hi.x,lo.y,hi.y,lo.z,hi.z});
                    }
                    if (path.contains("/leg_") && (path.endsWith("/femur") || path.endsWith("/tibia") || path.endsWith("/tarsus"))) {
                        float x = (cube.minX + cube.maxX)/32F, z = (cube.minZ + cube.maxZ)/32F;
                        endpoints.put(path, new Vector3f[]{pose.pose().transformPosition(new Vector3f(x,cube.minY/16F,z)),
                                pose.pose().transformPosition(new Vector3f(x,cube.maxY/16F,z))});
                    }
                });
                for (String name : AntModel.LEG_NAMES) {
                    String base = "/ant/mesosoma/" + name + "/femur";
                    Vector3f hip = endpoints.get(base)[0];
                    require(thoraxBoxes.stream().anyMatch(b -> hip.x >= b[0]-0.000001 && hip.x <= b[1]+0.000001
                            && hip.y >= b[2]-0.000001 && hip.y <= b[3]+0.000001 && hip.z >= b[4]-0.000001 && hip.z <= b[5]+0.000001), "Hip must attach inside rendered mesosoma");
                    require(endpoints.get(base)[1].distance(endpoints.get(base + "/tibia")[0]) < 0.000001F, "Connected knee geometry");
                    require(endpoints.get(base + "/tibia")[1].distance(endpoints.get(base + "/tibia/tarsus")[0]) < 0.000001F, "Connected ankle geometry");
                }
            }
            model.resetPose();
        });
        test(form, "onePetioleAndBodySegments", () -> {
            require(cubes.keySet().stream().filter(path -> path.matches("/ant/[^/]*petiole[^/]*")).count() == 1,
                    "Formicine has one anatomical petiole root; its mesh may contain several primitives/children");
            for (String segment : new String[]{"head", "mesosoma", "petiole", "gaster"}) require(cubes.containsKey("/ant/" + segment), "Missing rendered body segment");
            long scars = cubes.keySet().stream().filter(path -> path.contains("wing_scar")).count();
            require(scars == (form == AntForm.QUEEN ? 4 : 0), "Queen wing scars only");
            java.util.List<Vector3f> attachments = new java.util.ArrayList<>();
            java.util.List<double[]> bodyBoxes = new java.util.ArrayList<>();
            model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
                if (path.equals("/ant/mesosoma") || path.equals("/ant/head")) {
                    Vector3f lo = pose.pose().transformPosition(new Vector3f(cube.minX/16F,cube.minY/16F,cube.minZ/16F));
                    Vector3f hi = pose.pose().transformPosition(new Vector3f(cube.maxX/16F,cube.maxY/16F,cube.maxZ/16F));
                    bodyBoxes.add(new double[]{lo.x,hi.x,lo.y,hi.y,lo.z,hi.z});
                }
                if (index == 0 && (path.contains("wing_scar") || path.endsWith("_scape")))
                    attachments.add(pose.pose().transformPosition(new Vector3f()));
            });
            for (Vector3f p : attachments) require(bodyBoxes.stream().anyMatch(b -> p.x >= b[0]-0.000001 && p.x <= b[1]+0.000001
                    && p.y >= b[2]-0.000001 && p.y <= b[3]+0.000001 && p.z >= b[4]-0.000001 && p.z <= b[5]+0.000001), "Scars/antenna bases must attach to rendered body");
        });
        test(form, "lowAmplitudeTransformedFootMotion", () -> checkFootMotion(form, 0.03F));
        test(form, "normalAmplitudeTransformedFootMotion", () -> checkFootMotion(form, 0.35F));
        test(form, "amplitudeOneMirroringRegression", () -> checkFootMotion(form, 1F));
        test(form, "stanceContactTracksVanillaWalkDistance", () -> checkFootMotion(form, 0.28F));
        test(form, "stationaryLegsStopWhileAntennaeAnimate", () -> {
            AntRenderState state = new AntRenderState();
            state.walkAnimationPos = 10;
            state.walkAnimationSpeed = 1;
            state.moving = true;
            model.setupAnim(state);
            state.moving = false;
            state.ageInTicks = 13;
            model.setupAnim(state);
            Vector3f[] restingContacts = contacts(model);
            float antenna = head.getChild("antenna_left_scape").yRot;
            for (String name : AntModel.LEG_NAMES) {
                ModelPart leg = mesosoma.getChild(name);
                near(leg.yRot, leg.getInitialPose().yRot());
                near(leg.zRot, leg.getInitialPose().zRot());
                ModelPart femur = leg.getChild("femur");
                near(femur.zRot, femur.getInitialPose().zRot());
                near(femur.getChild("tibia").zRot, femur.getChild("tibia").getInitialPose().zRot());
            }
            state.ageInTicks = 27;
            model.setupAnim(state);
            Vector3f[] laterContacts = contacts(model);
            for (int i = 0; i < 6; i++) require(restingContacts[i].distance(laterContacts[i]) < 0.000001F, "Stationary rendered feet must stay reset");
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
            PrimeAnts.LOGGER.info("T03 rendered dimensions {}: bodyLength={}, bodyHeight={}, excludes antenna/leg reach", form, length, bounds[3] - bounds[2]);
        });
        test(form, "taperedProfilesConnectedConstrictedWaist", () -> checkProfiles(form, model));
        model.resetPose();
    }

    private void checkProfiles(AntForm form, AntModel model) {
        model.resetPose();
        Map<String, java.util.List<double[]>> boxes = new LinkedHashMap<>();
        model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
            if (!path.matches("/ant/(head|mesosoma|petiole|gaster)(/neck)?")) return;
            double[] b = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (var polygon : cube.polygons) for (var v : polygon.vertices()) {
                Vector3f p = pose.pose().transformPosition(new Vector3f(v.worldX(), v.worldY(), v.worldZ()));
                b[0] = Math.min(b[0], p.x); b[1] = Math.max(b[1], p.x);
                b[2] = Math.min(b[2], p.y); b[3] = Math.max(b[3], p.y);
                b[4] = Math.min(b[4], p.z); b[5] = Math.max(b[5], p.z);
            }
            boxes.computeIfAbsent(path.split("/")[2], key -> new java.util.ArrayList<>()).add(b);
        });
        Map<String, double[]> dimensions = new LinkedHashMap<>();
        StringBuilder contourErrors = new StringBuilder();
        for (var entry : boxes.entrySet()) {
            double minZ = entry.getValue().stream().mapToDouble(b -> b[4]).min().orElseThrow();
            double maxZ = entry.getValue().stream().mapToDouble(b -> b[5]).max().orElseThrow();
            double maxWidth = 0, firstWidth = 0;
            for (int slice = 0; slice < 25; slice++) {
                double z = minZ + (slice + 0.5) * (maxZ - minZ) / 25;
                double minX = 999, maxX = -999, minY = 999, maxY = -999;
                for (double[] b : entry.getValue()) if (z >= b[4] && z <= b[5]) {
                    minX = Math.min(minX, b[0]); maxX = Math.max(maxX, b[1]);
                    minY = Math.min(minY, b[2]); maxY = Math.max(maxY, b[3]);
                }
                require(maxX >= minX, "Disconnected axial profile " + entry.getKey());
                double width = maxX - minX;
                if (slice == 0) firstWidth = width;
                maxWidth = Math.max(maxWidth, width);
                profiles.append(String.format(java.util.Locale.ROOT, "%s,%s,%.8f,%.8f,%.8f,%.8f,%.8f%n", form, entry.getKey(), z, width, maxY - minY, minY, maxY));
            }
            dimensions.put(entry.getKey(), new double[]{minZ, maxZ, maxWidth});
            if (!entry.getKey().equals("petiole") && firstWidth >= maxWidth * 0.65)
                contourErrors.append(entry.getKey()).append(" does not taper; ");
        }
        require(dimensions.get("petiole")[2] < 0.45 * dimensions.get("mesosoma")[2]
                && dimensions.get("petiole")[2] < 0.45 * dimensions.get("gaster")[2], "Waist must be constricted in baked geometry");
        // Intersecting cuboids make one physically connected body; no floating gap hidden by overall bounds.
        var all = boxes.values().stream().flatMap(java.util.Collection::stream).toList();
        boolean[] reached = new boolean[all.size()]; reached[0] = true;
        for (int pass = 0; pass < all.size(); pass++) for (int a = 0; a < all.size(); a++) if (reached[a]) {
            for (int b = 0; b < all.size(); b++) {
                boolean overlap = true;
                for (int axis = 0; axis < 6; axis += 2)
                    overlap &= all.get(a)[axis] <= all.get(b)[axis + 1] + 0.000001 && all.get(b)[axis] <= all.get(a)[axis + 1] + 0.000001;
                if (overlap) reached[b] = true;
            }
        }
        for (boolean connected : reached) require(connected, "All body primitives must connect physically");
        require(contourErrors.isEmpty(), contourErrors.toString());
    }
}
