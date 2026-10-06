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
    private static boolean capturing, extracting;
    private static UUID captureSubject;
    private static Map<String,Object> cameraFrame;
    private static Map<String,Object> screenshotFrame;
    public static void arm(UUID uuid) { start(uuid); capturing=true; captureSubject=uuid; cameraFrame=null; screenshotFrame=null; }
    public static void beginFrame() { if(capturing) { frames.clear(); extracting=true; } }
    public static void endFrame(net.minecraft.client.DeltaTracker delta) {
        if(!capturing)return;
        extracting=false;
        var client=Minecraft.getInstance();
        var state=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        var eye=state.pos;
        var ant=AppearanceScenario.clientAnt(client,captureSubject);
        var data=new LinkedHashMap<String,Object>();
        data.put("eye",List.of(eye.x,eye.y,eye.z)); data.put("yaw",state.yRot); data.put("pitch",state.xRot);
        data.put("fov",client.gameRenderer.mainCamera().getFov());
        data.put("viewport",List.of(client.gameRenderer.mainRenderTarget().width,client.gameRenderer.mainRenderTarget().height));
        data.put("partial_tick",client.gameRenderer.gameRenderState().levelRenderState.worldPartialTicks);
        data.put("camera_partial_tick",state.cameraEntityPartialTicks);
        data.put("render_extraction_utc",Instant.now().toString());
        data.put("client_game_time",client.level==null?-1:client.level.getGameTime());
        data.put("lens_block",client.level==null?"unloaded":client.level.getBlockState(net.minecraft.core.BlockPos.containing(eye)).toString());
        data.put("clearance",ant!=null&&AppearanceScenario.clear(client,ant,eye));
        data.put("living_subject",ant!=null&&ant.isAlive());
        data.put("living_observer",client.player!=null&&client.player.isAlive());
        data.put("night_vision",client.player!=null&&client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION));
        data.put("rendered_ants",List.copyOf(frames));
        cameraFrame=data;
    }
    public static void bindScreenshot(com.mojang.blaze3d.pipeline.RenderTarget target) {
        if(!capturing||cameraFrame==null||target!=Minecraft.getInstance().gameRenderer.mainRenderTarget())return;
        screenshotFrame=new LinkedHashMap<>(cameraFrame);
        screenshotFrame.put("readback_scheduled_utc",Instant.now().toString());
        capturing=false; extracting=false;
    }
    public static Map<String,Object> finishCapture() {
        capturing=false; extracting=false; tracked=java.util.Set.of();
        if(screenshotFrame==null)throw new AssertionError("Screenshot was not bound to real render extraction");
        return screenshotFrame;
    }
    public static void start(UUID uuid) { start(java.util.Set.of(uuid)); }
    public static void start(java.util.Set<UUID> uuids) { tracked = java.util.Set.copyOf(uuids); frames.clear(); }
    public static List<Map<String, Object>> finish() { tracked = java.util.Set.of(); return List.copyOf(frames); }

    public static void record(LasiusNigerEntity ant, AntRenderState state, float partial) {
        Minecraft client = Minecraft.getInstance();
        if (!tracked.contains(ant.getUUID()) || capturing&&!extracting || partial != 1F || client.getWindow().getWidth() != 1600 || client.getWindow().getHeight() != 1000) return;
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
        frame.put("carried_item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ant.getMainHandItem().getItem()).toString());
        frame.put("carried_soil_rendered", !state.carriedSoil.isEmpty());
        frame.put("client_carried_soil_units", ant.getMainHandItem().getCount());
        if(!state.carriedSoil.isEmpty()){
            var bounds=state.carriedSoil.getModelBoundingBox();
            frame.put("carried_display_bounds",List.of(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ));
        }
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
