package neofontrender.addons.mixin.compat;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.flight.ShoulderSurfingMatrixFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes actual block/entity picking use the same player-origin ray as UIE's projected crosshair. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererShoulderSurfingMouseOver {
    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void uie$synchronizeShoulderMouseOver(float partialTicks, CallbackInfo callback) {
        ShoulderSurfingMatrixFix.synchronizeMouseOver(partialTicks);
    }
}
