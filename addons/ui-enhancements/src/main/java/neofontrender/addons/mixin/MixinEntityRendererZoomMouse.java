package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import neofontrender.addons.zoom.ZoomMouseScaling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
}
