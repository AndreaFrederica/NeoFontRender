package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Gives the debug style access to Minecraft's protected vanilla crosshair renderer. */
@Mixin(GuiIngame.class)
public interface InvokerGuiIngameCrosshair {
    @Invoker("renderAttackIndicator")
    void nfrUi$renderVanillaCrosshair(float partialTicks, ScaledResolution resolution);
}
