package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.flight.FlightRollController;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies tracked aircraft roll after 1.7.10's vanilla camera orientation. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererCameraRoll {
    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void nfrUi$applyTrackedCameraRoll(float partialTicks, CallbackInfo ci) {
        float roll = FlightRollController.cameraRoll(partialTicks);
        if (Math.abs(roll) > 0.001F) GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
    }
}
