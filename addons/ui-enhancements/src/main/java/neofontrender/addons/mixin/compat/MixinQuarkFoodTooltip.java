package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import neofontrender.addons.tooltips.QuarkTooltipVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "vazkii.quark.client.feature.FoodTooltip", remap = false)
public abstract class MixinQuarkFoodTooltip {
    @Inject(method = "makeTooltip", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$replaceFoodPlaceholder(ItemTooltipEvent event, CallbackInfo ci) {
        if (QuarkTooltipVisuals.replaceFoodTooltip(event)) ci.cancel();
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$suppressFoodDrawing(RenderTooltipEvent.PostText event, CallbackInfo ci) {
        if (QuarkTooltipVisuals.replaceOriginalDrawing()) ci.cancel();
    }
}
