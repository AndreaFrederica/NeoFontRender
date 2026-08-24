package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.camera.CameraRuntime;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps vanilla third-person visibility while UIE supplies the actual camera displacement. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererCameraPresentation {
    @Shadow private float thirdPersonDistancePrev;

    @ModifyConstant(method = "orientCamera", constant = @Constant(floatValue = 4.0F), require = 1)
    private float nfrUi$zeroDetachedCameraDistanceTarget(float original) {
        return CameraRuntime.suppressesVanillaThirdPersonDisplacement() ? 0.0F : original;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistancePrev:F"),
            require = 2)
    private float nfrUi$zeroDetachedCameraDistancePrevious(EntityRenderer renderer) {
        return CameraRuntime.suppressesVanillaThirdPersonDisplacement()
                ? 0.0F : thirdPersonDistancePrev;
    }
}
