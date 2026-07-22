package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
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
    @Inject(method = "drawTooltip", at = @At("RETURN"), require = 0, remap = false)
    private void nfrUi$endCustomTooltip(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci) {
        HeiTooltipCompat.end();
    }

    @Redirect(method = "drawTooltip",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/config/GuiUtils;drawGradientRect(IIIIIII)V"),
            require = 0, remap = false)
    private void nfrUi$replaceCustomPanel(int zLevel, int left, int top, int right, int bottom,
                                          int startColor, int endColor) {
        // A one-item collapsed group delegates to HEI's ordinary item tooltip. Start only when
        // this method reaches its hand-drawn panel so that delegated tooltips still use NFR's
        // normal Forge-event renderer.
        HeiTooltipCompat.beginIfAbsent(ItemStack.EMPTY);
        HeiTooltipCompat.drawGradientRect(zLevel, left, top, right, bottom, startColor, endColor);
    }
}
