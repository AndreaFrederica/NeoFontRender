package neofontrender.addons.mixin.compat;

import neofontrender.addons.tooltips.ThaumcraftAspectTooltipCompat;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "thaumcraft.client.lib.events.RenderEventHandler", remap = false)
public abstract class MixinThaumcraftAspectTooltip {
    @Inject(method = "tooltipEvent(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void nfrUi$replaceAspectTooltip(ItemTooltipEvent event, CallbackInfo ci) {
        if (ThaumcraftAspectTooltipCompat.replaceOriginal(event)) ci.cancel();
    }

    @Inject(method = "tooltipEvent(Lnet/minecraftforge/client/event/RenderTooltipEvent$PostBackground;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void nfrUi$suppressOriginalAspectDrawing(
            RenderTooltipEvent.PostBackground event, CallbackInfo ci) {
        if (ThaumcraftAspectTooltipCompat.replaceOriginalDrawing(event.getStack())) ci.cancel();
    }
}
