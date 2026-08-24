package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import neofontrender.addons.camera.CameraRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores the vanilla view-entity identity rule that upstream Shoulder Surfing preserves. */
@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBaseCameraViewIdentity {
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$restoreDetachedViewEntityIdentity(EntityLivingBase entity,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (entity == Minecraft.getMinecraft().player
                && CameraRuntime.shouldRenderDetachedLocalPlayer()) {
            cir.setReturnValue(false);
        }
    }
}
