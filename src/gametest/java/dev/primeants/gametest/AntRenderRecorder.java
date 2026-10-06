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
    private static net.minecraft.core.BlockPos broodPosition;
    private static UUID continuousSubject;
    private static final Map<Long,Map<String,Object>> continuous=new LinkedHashMap<>();
    public static void startContinuous(UUID id){continuousSubject=id;continuous.clear();}
    public static Map<String,Object> latestContinuous(){return continuous.isEmpty()?null:new ArrayList<>(continuous.values()).getLast();}
    public static List<Map<String,Object>> finishContinuous(){continuousSubject=null;return List.copyOf(continuous.values());}
    public static void armBrood(UUID queen,net.minecraft.core.BlockPos pile){arm(queen);broodPosition=pile;}

    private static UUID captureSubject;
    private static Map<String,Object> cameraFrame;
    private static Map<String,Object> screenshotFrame;
    public static void arm(UUID uuid) { broodPosition=null;start(uuid); capturing=true; captureSubject=uuid; cameraFrame=null; screenshotFrame=null; }
    public static void beginFrame() { extracting=capturing||continuousSubject!=null;if(capturing)frames.clear(); }
    public static void endFrame(net.minecraft.client.DeltaTracker delta) {
        extracting=false;
        if(!capturing)return;
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
        data.put("brightness",client.options.gamma().get());
        data.put("observer_position",client.player==null?List.of():List.of(client.player.getX(),client.player.getY(),client.player.getZ()));
        data.put("survival_observer",client.player!=null&&!client.player.isSpectator()&&!client.player.getAbilities().instabuild&&!client.player.getAbilities().flying&&!client.player.noPhysics);
        if(broodPosition!=null&&client.level!=null&&client.player!=null){
            var be=client.level.getBlockEntity(broodPosition);
            boolean actual=be instanceof dev.primeants.brood.BroodPile b&&!b.records().isEmpty();
            var stateAt=client.level.getBlockState(broodPosition);
            data.put("brood_position",List.of(broodPosition.getX(),broodPosition.getY(),broodPosition.getZ()));
            data.put("brood_blockstate",stateAt.toString());
            var live=be instanceof dev.primeants.brood.BroodPile b?b.records().stream().map(r->Map.of("uuid",r.id().toString(),"slot",r.slot(),"stage",r.stage().toString(),"expired",b.expired().containsKey(r.id()))).toList():List.of();
            data.put("active_brood",live);
            data.put("block_light",client.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,broodPosition.above()));
            data.put("sky_light",client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,broodPosition.above()));
            boolean clear=actual&&eye.distanceTo(client.player.getEyePosition())<.05&&client.player.isAlive()&&client.player.onGround()&&client.level.noCollision(client.player,client.player.getBoundingBox())&&client.level.getFluidState(client.player.blockPosition()).isEmpty()&&client.level.getFluidState(client.player.blockPosition().above()).isEmpty();
            for(double dx:new double[]{-.075,.075})for(double dy:new double[]{-.075,.075})for(double dz:new double[]{-.075,.075}){var p=net.minecraft.core.BlockPos.containing(eye.add(dx,dy,dz));clear&=client.level.hasChunkAt(p)&&client.level.getWorldBorder().isWithinBounds(p)&&client.level.getBlockState(p).isAir()&&client.level.getFluidState(p).isEmpty();}
            for(var obj:live){
                var row=(Map<?,?>)obj;
                int slot=(Integer)row.get("slot");var prop=List.of(dev.primeants.brood.BroodPileBlock.A,dev.primeants.brood.BroodPileBlock.B,dev.primeants.brood.BroodPileBlock.C).get(slot);
                clear&=stateAt.getValue(prop).toString().equals(row.get("stage"))&&!Boolean.TRUE.equals(row.get("expired"));
                double[] x={3.5,11.5,7.5},z={4.5,4.5,12};
                var target=new net.minecraft.world.phys.Vec3(broodPosition.getX()+x[slot]/16,broodPosition.getY()+.1,broodPosition.getZ()+z[slot]/16);
                var hit=client.level.clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,client.player));
                clear&=hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS||hit.getBlockPos().equals(broodPosition);
            }
            data.put("clearance",clear);data.put("living_subject",actual);data.put("clearance_policy","Actual survival eye/body, dry loaded air lens, ordinary support, live a/b/c projection and sight ray to each active slot");
        }
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
        if(continuousSubject!=null&&!extracting)return;
        if (!(tracked.contains(ant.getUUID())||ant.getUUID().equals(continuousSubject)) || capturing&&!extracting || (partial != 1F&&continuousSubject==null) || client.getWindow().getWidth() != 1600 || client.getWindow().getHeight() != 1000) return;
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
        var worldFeet=new ArrayList<List<Double>>();var floorHeights=new ArrayList<Double>();var floorClearance=new ArrayList<Double>();
        boolean flat=ant.onGround()&&Math.abs(ant.getY()-ant.yo)<.000001;
        double angle=Math.toRadians(state.bodyRot),cos=Math.cos(angle),sin=Math.sin(angle);
        for(var foot:feet){
            var world=ant.position().add(cos*foot.x+sin*foot.z,1.501-foot.y,sin*foot.x-cos*foot.z);
            worldFeet.add(List.of(world.x,world.y,world.z));
            var hit=client.level.clip(new net.minecraft.world.level.ClipContext(world.add(0,.05,0),new net.minecraft.world.phys.Vec3(world.x,ant.getY()-1.25,world.z),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,ant));
            double floor=hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?hit.getLocation().y:ant.getY()-1.25;
            floorHeights.add(floor);floorClearance.add(world.y-floor);flat&=Math.abs(floor-ant.getY())<.0001;
        }
        frame.put("world_distal_contacts",worldFeet);frame.put("support_floor_heights",floorHeights);frame.put("actual_floor_clearances",floorClearance);frame.put("flat_ground",flat);
        frame.put("body_relative_distal_contacts", contacts);
        frame.put("support_legs", support);
        frame.put("support_tripod", support.equals(List.of(0,2,4)) ? "A" : support.equals(List.of(1,3,5)) ? "B" : "boundary");
        if(ant.getUUID().equals(continuousSubject)){
            frame.put("client_game_time",client.level.getGameTime());
            frame.put("on_ground",ant.onGround());
            frame.put("terrain_class",flat?"flat-ground":"terrain-step-or-edge");
            continuous.put(client.level.getGameTime(),new LinkedHashMap<>(frame));
        }
        if(tracked.contains(ant.getUUID()))frames.add(frame);
    }
}
