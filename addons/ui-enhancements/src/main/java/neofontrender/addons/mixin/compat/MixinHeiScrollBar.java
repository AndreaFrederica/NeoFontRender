package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import neofontrender.addons.tooltips.HeiTooltipCompat;
import neofontrender.addons.tooltips.ModernTooltipScrollBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Rectangle;

/** Replaces only HEI's tooltip scrollbar skin; HEI keeps ownership of all input and state. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.elements.ScrollBar", remap = false)
public abstract class MixinHeiScrollBar {
    @Shadow(remap = false)
    public abstract Rectangle getArea();

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void nfrUi$drawModern(Minecraft minecraft, int visibleAmount, int hiddenAmount,
                                  float scrollOffsetY, CallbackInfo ci) {
        if (!HeiTooltipCompat.ownsCustomTooltip()) return;
        if (ModernTooltipScrollBar.draw(getArea(), visibleAmount, hiddenAmount, scrollOffsetY,
                HeiTooltipCompat.activeStack())) {
            ci.cancel();
        }
    }
}
