package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.tooltips.ObscureTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.component.HeaderComponent", remap = false)
public abstract class MixinObscureHeaderComponent {
    @Shadow public abstract int getWidth(FontRenderer font);
    @Unique private ObscureTooltipCompat.TextMetrics nfrUi$title;
    @Unique private ObscureTooltipCompat.TextMetrics nfrUi$label;
    @Unique private boolean nfrUi$separator;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void nfrUi$captureHeader(@Coerce Object state, @Coerce Object title,
                                    @Coerce Object label, boolean separator,
                                    @Coerce Object color, CallbackInfo ci) {
        nfrUi$title = title instanceof ObscureTooltipCompat.TextMetrics
                ? (ObscureTooltipCompat.TextMetrics) title : null;
        nfrUi$label = label instanceof ObscureTooltipCompat.TextMetrics
                ? (ObscureTooltipCompat.TextMetrics) label : null;
        nfrUi$separator = separator;
        if (nfrUi$title != null) nfrUi$title.nfrUi$setWidthInset(22);
        if (nfrUi$label != null) nfrUi$label.nfrUi$setWidthInset(22);
    }

    @Unique
    private int nfrUi$contentHeight() {
        int title = nfrUi$title == null ? 10 : nfrUi$title.getHeight();
        int label = nfrUi$label == null ? 0 : nfrUi$label.getHeight();
        return ObscureTooltipCompat.headerContentHeight(title, label);
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true, require = 0)
    private void nfrUi$height(CallbackInfoReturnable<Integer> cir) {
        if (ObscureTooltipCompat.shouldLayout()) {
            cir.setReturnValue(nfrUi$contentHeight() + (nfrUi$separator ? 3 : 0));
        }
    }

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
        if (ObscureTooltipCompat.replaceSeparator(x, y + nfrUi$contentHeight(), getWidth(font))) {
            // At this point Obscure has already rendered the slot/effect/icon. Cancelling only
            // skips its pair of fading separator calls, leaving all custom tooltip content intact.
            ci.cancel();
        }
    }
}
