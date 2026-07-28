package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.addons.tooltips.QuarkMapTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "vazkii.quark.client.feature.MapTooltip", remap = false)
public abstract class MixinQuarkMapTooltip {
    @Shadow(remap = false)
    public static boolean requireShift;

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void nfrUi$renderModernMapTooltip(RenderTooltipEvent.PostText event, CallbackInfo ci) {
        if (QuarkMapTooltipCompat.render(event, requireShift)) ci.cancel();
    }
}
