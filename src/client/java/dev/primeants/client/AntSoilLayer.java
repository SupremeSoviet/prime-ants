package dev.primeants.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Vanilla item render state at the production queen mandibles. No anatomy or gait changes. */
public final class AntSoilLayer extends RenderLayer<AntRenderState, AntModel> {
    public AntSoilLayer(RenderLayerParent<AntRenderState, AntModel> parent) { super(parent); }
    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AntRenderState state, float yaw, float pitch) {
        if (state.carriedSoil.isEmpty()) return;
        pose.pushPose();
        getParentModel().root().getChild("ant").translateAndRotate(pose);
        getParentModel().root().getChild("ant").getChild("head").translateAndRotate(pose);
        pose.translate(0, 0, -0.72F); pose.scale(0.75F, 0.75F, 0.75F);
        state.carriedSoil.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }
}
