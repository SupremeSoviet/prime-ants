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
        // Head-local jaw tips: queen 12/2+7 raw units, worker 7/2+2 raw units.
        pose.translate(0, 1.2F / 16, state.queen ? -0.72F : -0.40F);
        float size = state.queen ? 0.75F : 0.60F; pose.scale(size, size, size);
        state.carriedSoil.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }
}
