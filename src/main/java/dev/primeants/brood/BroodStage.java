package dev.primeants.brood;

import net.minecraft.util.StringRepresentable;

public enum BroodStage implements StringRepresentable {
    EMPTY, EGG, LARVA, COCOON;
    @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
