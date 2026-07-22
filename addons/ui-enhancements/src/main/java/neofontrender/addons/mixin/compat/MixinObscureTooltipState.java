package neofontrender.addons.mixin.compat;

import net.minecraft.item.ItemStack;
import neofontrender.addons.tooltips.ObscureTooltipCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.obscuria.tooltips.client.TooltipState", remap = false)
public abstract class MixinObscureTooltipState {
    @Shadow @Final public ItemStack stack;

    @Inject(method = "renderPanel", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void nfrUi$modernPanel(@Coerce Object graphics, int x, int y, int width, int height, CallbackInfo ci) {
        if (!ObscureTooltipCompat.shouldReplacePanel()) return;
        ObscureTooltipCompat.drawPanel(x, y, width, height, stack);
        ci.cancel();
    }

    @Inject(method = "renderFrame", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void nfrUi$suppressFrame(@Coerce Object graphics, int x, int y, int width, int height, CallbackInfo ci) {
        if (ObscureTooltipCompat.shouldReplacePanel()) ci.cancel();
    }
}
