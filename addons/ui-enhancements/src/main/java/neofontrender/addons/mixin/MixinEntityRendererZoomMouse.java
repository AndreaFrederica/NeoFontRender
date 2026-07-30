package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import neofontrender.addons.zoom.ZoomHandler;
import neofontrender.addons.zoom.ZoomMouseScaling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererZoomMouse {
    @Redirect(
            method = "updateCameraAndRender",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/settings/GameSettings;mouseSensitivity:F"
            ),
            require = 1
    )
    private float nfrUi$compensateZoomMouseSensitivity(GameSettings settings) {
        return ZoomMouseScaling.adjustedSensitivity(settings.mouseSensitivity);
    }

    /**
     * 1.7.10 has no EntityViewRenderEvent.FOVModifier event; adjust the computed FOV at the
     * single place vanilla produces it instead. Runs for both the world and the hand pass.
     */
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void nfrUi$applyZoomFov(float partialTicks, boolean useFovSetting,
                                    CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ZoomHandler.INSTANCE.modifyFov(cir.getReturnValue().floatValue()));
    }
}
