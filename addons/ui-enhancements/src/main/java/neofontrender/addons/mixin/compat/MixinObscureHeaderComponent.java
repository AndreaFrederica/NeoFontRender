package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.tooltips.ObscureTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.component.HeaderComponent", remap = false)
public abstract class MixinObscureHeaderComponent {
    @Shadow public abstract int getWidth(FontRenderer font);

    @Inject(
            method = "renderImage",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/obscuria/tooltips/client/tooltip/particle/GraphicUtils;drawHLine(Ldev/obscuria/tooltips/client/render/GuiGraphics;IIILdev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;)V",
                    ordinal = 0),
            cancellable = true,
            require = 0,
            remap = false)
    private void nfrUi$replaceSeparator(FontRenderer font, int x, int y,
                                        @Coerce Object graphics, CallbackInfo ci) {
        if (ObscureTooltipCompat.replaceSeparator(x, y + 22, getWidth(font))) {
            // At this point Obscure has already rendered the slot/effect/icon. Cancelling only
            // skips its pair of fading separator calls, leaving all custom tooltip content intact.
            ci.cancel();
        }
    }
}
