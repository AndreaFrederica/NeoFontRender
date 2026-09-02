package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.tooltips.HeiTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.render.CollapsedGroupRenderer", remap = false)
public abstract class MixinHeiCollapsedGroupTooltip {
    @Inject(method = "drawTooltip", at = @At("HEAD"), require = 0, remap = false)
    private void nfrUi$beginCustomTooltip(Minecraft minecraft, int mouseX, int mouseY,
                                           CallbackInfo ci) {
        HeiTooltipCompat.beginCollapsed(this, minecraft);
    }

    @Inject(method = "drawTooltip", at = @At("RETURN"), require = 0, remap = false)
    private void nfrUi$endCustomTooltip(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci) {
        HeiTooltipCompat.finishCollapsed();
    }

    @Redirect(method = "drawTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/ScaledResolution;getScaledWidth()I",
                    remap = true),
            require = 0, remap = false)
    private int nfrUi$reserveHorizontalVisualExtent(ScaledResolution resolution) {
        return HeiTooltipCompat.availableScreenWidth(resolution.getScaledWidth());
    }

    @Redirect(method = "drawTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/ScaledResolution;getScaledHeight()I",
                    remap = true),
            require = 0, remap = false)
    private int nfrUi$reserveVerticalVisualExtent(ScaledResolution resolution) {
        return HeiTooltipCompat.availableScreenHeight(resolution.getScaledHeight());
    }

    @Redirect(method = "drawTooltip",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/config/GuiUtils;drawGradientRect(IIIIIII)V"),
            require = 0, remap = false)
    private void nfrUi$replaceCustomPanel(int zLevel, int left, int top, int right, int bottom,
                                          int startColor, int endColor) {
        HeiTooltipCompat.drawGradientRect(zLevel, left, top, right, bottom, startColor, endColor);
    }
}
