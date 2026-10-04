package dev.primeants.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Visual snapshot only: no biological age or writable simulation state. */
public final class AntRenderState extends LivingEntityRenderState {
    public boolean moving;
    public int callow = 1000;
    public boolean queen;
    public final net.minecraft.client.renderer.item.ItemStackRenderState carriedSoil = new net.minecraft.client.renderer.item.ItemStackRenderState();
}
