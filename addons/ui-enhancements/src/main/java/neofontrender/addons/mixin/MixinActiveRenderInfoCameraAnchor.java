package neofontrender.addons.mixin;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import neofontrender.addons.camera.CameraRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps fog/material viewpoint sampling on the same world-origin interpolation as rendering. */
@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfoCameraAnchor {
    @Inject(method = "projectViewFromEntity(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/util/math/Vec3d;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private static void nfrUi$alignNativeCameraOrigin(Entity renderEntity, double partialTicks,
                                                      CallbackInfoReturnable<Vec3d> cir) {
        Vec3d adjusted = CameraRuntime.alignActiveRenderInfoPosition(renderEntity, partialTicks,
                cir.getReturnValue());
        if (adjusted != null) cir.setReturnValue(adjusted);
    }
}
