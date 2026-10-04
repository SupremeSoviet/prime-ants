package dev.primeants.gametest;

import dev.primeants.client.AntModel;
import dev.primeants.client.AntRenderState;
import dev.primeants.entity.LasiusNigerEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

/** Development-only passive recorder. Never writes production entity/model state. */
public final class AntRenderRecorder {
    private static java.util.Set<UUID> tracked = java.util.Set.of();
    private static final List<Map<String, Object>> frames = new ArrayList<>();
    public static void start(UUID uuid) { start(java.util.Set.of(uuid)); }
    public static void start(java.util.Set<UUID> uuids) { tracked = java.util.Set.copyOf(uuids); frames.clear(); }
    public static List<Map<String, Object>> finish() { tracked = java.util.Set.of(); return List.copyOf(frames); }

    public static void record(LasiusNigerEntity ant, AntRenderState state, float partial) {
        Minecraft client = Minecraft.getInstance();
        if (!tracked.contains(ant.getUUID()) || partial != 1F || client.getWindow().getWidth() != 1600 || client.getWindow().getHeight() != 1000) return;
        AntModel model = new AntModel(AntModel.createBodyLayer(ant.form()).bakeRoot());
        model.setupAnim(state);
        var feet = AntModelGameTest.contacts(model);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("uuid", ant.getUUID().toString()); frame.put("form", ant.form().serializedName()); frame.put("callow_visual", state.callow);
        frame.put("render_extraction_utc", Instant.now().toString());
        frame.put("render_entity_tick", ant.tickCount);
        frame.put("render_position", List.of(ant.getX(), ant.getY(), ant.getZ()));
        frame.put("body_yaw", state.bodyRot);
        frame.put("walk_animation_position", state.walkAnimationPos);
        frame.put("walk_animation_speed", state.walkAnimationSpeed);
        frame.put("visual_age_ticks", state.ageInTicks);
        frame.put("moving", state.moving);
        frame.put("carried_soil_rendered", !state.carriedSoil.isEmpty());
        frame.put("client_carried_soil_units", ant.getMainHandItem().getCount());
        frame.put("partial_ticks", partial);
        List<List<Float>> contacts = new ArrayList<>();
        List<Integer> support = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            contacts.add(List.of(feet[i].x, feet[i].y, feet[i].z));
            if (Math.abs(feet[i].y - 1.5F) < 0.00001F) support.add(i);
        }
        frame.put("body_relative_distal_contacts", contacts);
        frame.put("support_legs", support);
        frame.put("support_tripod", support.equals(List.of(0,2,4)) ? "A" : support.equals(List.of(1,3,5)) ? "B" : "boundary");
        frames.add(frame);
    }
}
