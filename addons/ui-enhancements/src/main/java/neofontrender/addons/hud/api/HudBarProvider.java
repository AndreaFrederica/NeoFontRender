package neofontrender.addons.hud.api;

import net.minecraft.entity.player.EntityPlayer;

/** Extensible data source for the Arc3D status-bar renderer. */
public interface HudBarProvider {
    /** Globally unique stable id, normally {@code modid:bar}. */
    String id();
    HudBarElement element();
    HudBarSide side();
    default int order() { return 1000; }
    /** True only when this provider intentionally replaces the corresponding vanilla element. */
    default boolean replacesVanilla() { return false; }
    boolean shouldRender(EntityPlayer player);
    HudBarValue sample(EntityPlayer player, float partialTicks);
}
