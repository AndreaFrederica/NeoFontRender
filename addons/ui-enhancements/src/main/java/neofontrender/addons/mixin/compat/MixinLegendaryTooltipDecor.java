package neofontrender.addons.mixin.compat;

import neofontrender.addons.tooltips.LegendaryTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.anthonyhilyard.legendarytooltips.render.TooltipDecor", remap = false)
public abstract class MixinLegendaryTooltipDecor {
    @Inject(method = "drawSeparator", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nfrUi$useMeasuredSeparator(int x, int y, int width, int color, CallbackInfo ci) {
        // NFR/Obscure has already drawn the separator at the measured title height.
        // Legendary otherwise derives this from the old fixed ten-pixel row count.
        if (LegendaryTooltipCompat.hasLayout()) ci.cancel();
    }
}
