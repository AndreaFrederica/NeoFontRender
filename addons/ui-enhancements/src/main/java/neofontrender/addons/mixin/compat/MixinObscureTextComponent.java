package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.tooltips.ObscureTooltipCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Share NFR's measurement and drawing contract, including tall inline content. */
@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.component.TextComponent", remap = false)
public abstract class MixinObscureTextComponent implements ObscureTooltipCompat.TextMetrics {
    @Shadow @Final private String text;
    @Unique private int nfrUi$widthInset;

    @Override
    public void nfrUi$setWidthInset(int inset) { nfrUi$widthInset = inset; }

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true, require = 0)
    private void nfrUi$width(FontRenderer font, CallbackInfoReturnable<Integer> cir) {
        if (ObscureTooltipCompat.shouldLayout()) {
            cir.setReturnValue(ObscureTooltipCompat.layoutText(font, text, nfrUi$widthInset).width());
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true, require = 0)
    private void nfrUi$height(CallbackInfoReturnable<Integer> cir) {
        if (ObscureTooltipCompat.shouldLayout()) {
            cir.setReturnValue(ObscureTooltipCompat.layoutText(ObscureTooltipCompat.font(), text, nfrUi$widthInset).height());
        }
    }

    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true, require = 0)
    private void nfrUi$draw(FontRenderer font, int x, int y, @Coerce Object graphics, CallbackInfo ci) {
        if (!ObscureTooltipCompat.shouldLayout()) return;
        ObscureTooltipCompat.layoutText(font, text, nfrUi$widthInset).draw(font, x, y);
        ci.cancel();
    }
}
