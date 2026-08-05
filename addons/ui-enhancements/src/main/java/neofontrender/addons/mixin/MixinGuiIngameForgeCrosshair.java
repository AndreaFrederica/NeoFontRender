package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
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
        if (!CrosshairController.suppressVanillaCrosshair()) {
            super.renderAttackIndicator(partialTicks, resolution);
        }
    }
}
