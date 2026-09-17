package neofontrender.addons.mixin.compat;

import neofontrender.addons.tooltips.ObscureTooltipCompat;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.TooltipEventHandler", remap = false)
public abstract class MixinObscureTooltipEventHandler {
    @Inject(method = "onRenderTooltipPre", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private void nfrUi$skipNativeRenderer(RenderTooltipEvent.Pre event, CallbackInfo ci) {
        if (ObscureTooltipCompat.shouldBypassObscure()) {
            ci.cancel();
        }
    }
}
