package neofontrender.addons.mixin.compat;

import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.tooltips.ObscureTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.TooltipHelper", remap = false)
public abstract class MixinObscureTooltipHelper {
    @Inject(method = "wrapLines", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nfrUi$wrap(@Coerce Object graphics, List<?> components,
                                   FontRenderer font, CallbackInfoReturnable<List<?>> cir) {
        // Each text component now wraps with NFR's width limit, including header text.
        // Do not apply Obscure's second, half-screen vanilla-token wrapping pass.
        if (ObscureTooltipCompat.shouldLayout()) cir.setReturnValue(components);
    }
}
