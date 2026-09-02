package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import neofontrender.addons.tooltips.QuarkTooltipVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "vazkii.quark.client.feature.VisualStatDisplay", remap = false)
public abstract class MixinQuarkVisualStatDisplay {
    @Redirect(method = "makeTooltip", at = @At(value = "INVOKE",
            target = "Ljava/util/List;add(ILjava/lang/Object;)V"),
            require = 1, remap = false)
    private void nfrUi$replaceVisualStatPlaceholder(List<Object> lines, int index, Object value,
                                                     ItemTooltipEvent event) {
        if (!QuarkTooltipVisuals.replaceVisualStatPlaceholder(event)) lines.add(index, value);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$suppressVisualStatDrawing(
            RenderTooltipEvent.PostText event, CallbackInfo ci) {
        if (QuarkTooltipVisuals.replaceOriginalDrawing()) ci.cancel();
    }
}
