package neofontrender.addons.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.camera.CameraPresentationPolicy;
import neofontrender.addons.camera.CameraRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla third-person visibility while UIE supplies the actual camera displacement. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererCameraPresentation {
    @Inject(method = "orientCamera(F)V", at = @At("HEAD"), require = 1)
    private void nfrUi$beginNativeCameraRenderSample(float partialTicks, CallbackInfo ci) {
        CameraRuntime.beginNativeCameraRenderSample(partialTicks);
    }

    @Inject(method = "orientCamera(F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z",
                    ordinal = 0, shift = At.Shift.BEFORE), require = 1)
    private void nfrUi$captureNativeCameraAnchor(float partialTicks, CallbackInfo ci,
                                                  @Local(index = 2) Entity renderEntity,
                                                  @Local(index = 3) float effectiveEyeHeight) {
        CameraRuntime.captureNativeCameraAnchor(renderEntity, partialTicks, effectiveEyeHeight);
    }

    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE", ordinal = 0),
            ordinal = 3, require = 1)
    private double nfrUi$zeroDetachedCameraDistance(double original) {
        return CameraPresentationPolicy.vanillaThirdPersonDistance(original,
                CameraRuntime.suppressesVanillaThirdPersonDisplacement());
    }
}
