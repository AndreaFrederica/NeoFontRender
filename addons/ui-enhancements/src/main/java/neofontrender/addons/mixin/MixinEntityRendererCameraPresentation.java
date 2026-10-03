package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.camera.CameraPresentationPolicy;
import neofontrender.addons.camera.CameraRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps vanilla third-person visibility while UIE supplies the actual camera displacement. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererCameraPresentation {
    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE", ordinal = 0),
            ordinal = 3, require = 1)
    private double nfrUi$zeroDetachedCameraDistance(double original) {
        return CameraPresentationPolicy.vanillaThirdPersonDistance(original,
                CameraRuntime.suppressesVanillaThirdPersonDisplacement());
    }
}
