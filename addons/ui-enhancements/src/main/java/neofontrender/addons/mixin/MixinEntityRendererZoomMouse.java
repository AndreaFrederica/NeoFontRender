package neofontrender.addons.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
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

    /**
     * Apply zoom smooth camera directly on the mouse delta before it reaches
     * player.turn().  This avoids vanilla MouseFilter cold-start stutter that
     * occurs when smoothCamera transitions from false to true.
     */
    @Redirect(
            method = "updateCameraAndRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"
            ),
            require = 1
    )
    private void nfrUi$smoothZoomCameraTurn(EntityPlayerSP player, float yaw, float pitch) {
        if (ZoomMouseScaling.isSmoothCameraActive()) {
            float[] smoothed = ZoomMouseScaling.smoothCameraDelta(yaw, pitch);
            player.turn(smoothed[0], smoothed[1]);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
