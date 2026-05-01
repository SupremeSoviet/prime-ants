package com.formicfrontier.client.render;

import com.formicfrontier.FormicFrontier;
import com.formicfrontier.entity.AntEntity;
import com.formicfrontier.sim.AntCaste;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class AntEntityRenderer extends MobRenderer<AntEntity, AntEntityRenderState, AntEntityModel> {
	private static final Identifier WORKER = FormicFrontier.id("textures/entity/ant_worker.png");
	private static final Identifier SCOUT = FormicFrontier.id("textures/entity/ant_scout.png");
	private static final Identifier MINER = FormicFrontier.id("textures/entity/ant_miner.png");
	private static final Identifier SOLDIER = FormicFrontier.id("textures/entity/ant_soldier.png");
	private static final Identifier MAJOR = FormicFrontier.id("textures/entity/ant_major.png");
	private static final Identifier GIANT = FormicFrontier.id("textures/entity/ant_giant.png");
	private static final Identifier QUEEN = FormicFrontier.id("textures/entity/ant_queen.png");

	public AntEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new AntEntityModel(context.bakeLayer(ModEntityModelLayers.ANT)), 0.75f);
	}

	@Override
	public AntEntityRenderState createRenderState() {
		return new AntEntityRenderState();
	}

	@Override
	public void extractRenderState(AntEntity entity, AntEntityRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.caste = entity.caste();
		state.workState = entity.workState();
		this.shadowRadius = Math.max(0.35f, 0.42f * state.caste.visualScale());
	}

	@Override
	public Identifier getTextureLocation(AntEntityRenderState state) {
		if (state.caste == AntCaste.QUEEN) {
			return QUEEN;
		}
		if (state.caste == AntCaste.GIANT || state.caste == AntCaste.MAJOR) {
			return state.caste == AntCaste.MAJOR ? MAJOR : GIANT;
		}
		if (state.caste == AntCaste.MINER) {
			return MINER;
		}
		if (state.caste == AntCaste.SCOUT) {
			return SCOUT;
		}
		if (state.caste == AntCaste.SOLDIER) {
			return SOLDIER;
		}
		return WORKER;
	}
}
