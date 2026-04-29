package com.formicfrontier.client.render;

import com.formicfrontier.sim.AntCaste;
import com.formicfrontier.sim.AntWorkState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class AntEntityRenderState extends LivingEntityRenderState {
	public AntCaste caste = AntCaste.WORKER;
	public AntWorkState workState = AntWorkState.IDLE;
}
