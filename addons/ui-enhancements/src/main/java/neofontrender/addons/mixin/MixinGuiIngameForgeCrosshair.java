package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;
import neofontrender.addons.flight.CrosshairController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Skips only the vanilla draw call while preserving Forge's CROSSHAIRS pre/post lifecycle. */
@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiIngameForgeCrosshair extends GuiIngame {
    protected MixinGuiIngameForgeCrosshair(Minecraft minecraft) {
        super(minecraft);
    }

    @Redirect(method = "renderCrosshairs", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/client/gui/GuiIngame;renderAttackIndicator(FLnet/minecraft/client/gui/ScaledResolution;)V"),
            require = 1)
    private void nfrUi$hideOnlyVanillaCrosshair(GuiIngame vanilla,
                                                 float partialTicks,
                                                 ScaledResolution resolution) {
        if (CrosshairController.suppressVanillaCrosshair()) return;
        if (!CrosshairController.cameraCrosshairVisible(partialTicks)) return;
        float[] offset = CrosshairController.cameraCrosshairOffset(partialTicks);
        if (offset == null) {
            super.renderAttackIndicator(partialTicks, resolution);
            return;
        }
        // Keep the vanilla renderer as the visual implementation when UIE custom
        // crosshairs are disabled, but route its placement through the unified crosshair system.
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(offset[0], offset[1], 0.0F);
            super.renderAttackIndicator(partialTicks, resolution);
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
