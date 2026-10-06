package dev.primeants.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
/** Actual equipment follows the animated head and the production jaw-tip attachment. */
public final class AntSoilLayer extends RenderLayer<AntRenderState, AntModel> {
    public AntSoilLayer(RenderLayerParent<AntRenderState, AntModel> parent) { super(parent); }
    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AntRenderState state, float yaw, float pitch) {
        if (state.carriedSoil.isEmpty()) return;
        pose.pushPose();
        attachToMandibles(pose,getParentModel(),state.queen,state.carriedSoil.getModelBoundingBox(),state.queen ? .75F : .60F);
        state.carriedSoil.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }
    /** Bounds include vanilla's actual display transform. Place the upper grip,
     * rather than the display origin, between the current animated jaw tips. */
    public static void attachToMandibles(PoseStack pose,AntModel model,boolean queen,AABB bounds,float size){
        model.root().getChild("ant").translateAndRotate(pose);
        model.root().getChild("ant").getChild("head").translateAndRotate(pose);
        pose.translate(0,AntModel.MANDIBLE_HEIGHT/16F,AntModel.carriedItemForward(queen)/16F);
        var centre=bounds.getCenter();
        pose.translate(-centre.x*size,-bounds.minY*size,-centre.z*size);
        pose.scale(size,size,size);
    }
}
