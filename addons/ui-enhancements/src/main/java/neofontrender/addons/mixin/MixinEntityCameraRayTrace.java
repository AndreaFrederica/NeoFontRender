package neofontrender.addons.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import neofontrender.addons.camera.CameraPickingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes Entity#rayTrace through the authoritative quaternion camera when a view override is active. */
@Mixin(Entity.class)
public abstract class MixinEntityCameraRayTrace {
    @Inject(method = "rayTrace(DF)Lnet/minecraft/util/math/RayTraceResult;", at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$cameraRay(double reach, float partialTicks,
                                  CallbackInfoReturnable<RayTraceResult> cir) {
        Entity entity = (Entity) (Object) this;
        if (!CameraPickingService.overridesInteractionBlockRay(entity)) return;
        cir.setReturnValue(CameraPickingService.traceInteractionBlocks(entity, reach,
                partialTicks, false, false, true));
    }
}
