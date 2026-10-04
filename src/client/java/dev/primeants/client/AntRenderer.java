package dev.primeants.client;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class AntRenderer extends MobRenderer<LasiusNigerEntity, AntRenderState, AntModel> {
    public static final ModelLayerLocation WORKER_LAYER = layer("worker");
    public static final ModelLayerLocation QUEEN_LAYER = layer("queen");
    private final Identifier texture;

    private static ModelLayerLocation layer(String form) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID, "lasius_niger_" + form), "main");
    }

    public AntRenderer(EntityRendererProvider.Context context, AntForm form) {
        super(context, new AntModel(context.bakeLayer(form == AntForm.QUEEN ? QUEEN_LAYER : WORKER_LAYER)),
                form == AntForm.QUEEN ? 0.5F : 0.25F);
        texture = Identifier.fromNamespaceAndPath(PrimeAnts.MOD_ID, "textures/entity/lasius_niger_" + form.serializedName() + ".png");
    }

    @Override public AntRenderState createRenderState() { return new AntRenderState(); }
    @Override public Identifier getTextureLocation(AntRenderState state) { return texture; }

    @Override
    public void extractRenderState(LasiusNigerEntity entity, AntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        state.moving = entity.isAlive() && dx * dx + dz * dz > 0.000001;
    }
}
