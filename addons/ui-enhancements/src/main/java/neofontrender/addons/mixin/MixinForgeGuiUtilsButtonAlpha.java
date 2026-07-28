package neofontrender.addons.mixin;

import net.minecraftforge.fml.client.config.GuiUtils;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiUtils.class, remap = false)
public abstract class MixinForgeGuiUtilsButtonAlpha {
    @Inject(method = "drawContinuousTexturedBox(IIIIIIIIIIIIF)V", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V",
                    shift = At.Shift.AFTER))
    private static void nfrUi$applyScopedButtonAlpha(CallbackInfo ci) {
        HoverEffectsRenderer.applyForgeButtonAlpha();
    }
}
