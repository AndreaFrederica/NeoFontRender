package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.addons.tooltips.LegendaryTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.anthonyhilyard.legendarytooltips.LegendaryTooltips", remap = false)
public abstract class MixinLegendaryTooltipEvents {
    @ModifyVariable(method = "onPostTooltipEvent", at = @At("HEAD"), argsOnly = true, require = 0)
    private static RenderTooltipEvent.PostText nfrUi$alignDecorations(RenderTooltipEvent.PostText event) {
        return LegendaryTooltipCompat.align(event);
    }
}
