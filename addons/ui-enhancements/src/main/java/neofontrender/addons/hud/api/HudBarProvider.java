package neofontrender.addons.hud.api;

import net.minecraft.entity.player.EntityPlayer;

/** Supplies one independently ordered HUD bar without owning Forge's overlay event. */
public interface HudBarProvider {
    /** Returns the stable namespaced identifier used for registration and animation state. */
    String id();

    /** Returns the vanilla overlay element whose render pass hosts this bar. */
    HudBarElement element();

    /** Returns the Forge HUD stack on which this bar reserves vertical space. */
    HudBarSide side();

    /** Returns the deterministic sort position within the selected overlay element. */
    default int order() {
        return 1000;
    }

    /** Declares whether a rendered sample replaces and cancels the corresponding vanilla element. */
    default boolean replacesVanilla() {
        return false;
    }

    /** Determines whether the provider has meaningful state for the current player and frame. */
    boolean shouldRender(EntityPlayer player);

    /** Samples the immutable renderer input for the current player and partial tick. */
    HudBarValue sample(EntityPlayer player, float partialTicks);
}
