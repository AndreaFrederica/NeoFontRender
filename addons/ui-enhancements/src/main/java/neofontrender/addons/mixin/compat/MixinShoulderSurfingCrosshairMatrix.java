package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import neofontrender.addons.flight.ShoulderSurfingFixConfig;
import neofontrender.addons.flight.ShoulderSurfingMatrixFix;

/** Leaves Shoulder Surfing's coordinate calculation intact without modifying the global HUD matrix. */
@Pseudo
@Mixin(targets = "com.teamderpy.shouldersurfing.client.ShoulderRenderer", remap = false)
public abstract class MixinShoulderSurfingCrosshairMatrix {
    @Inject(method = "offsetCrosshair", at = @At("HEAD"))
    private void uie$markMatrixHookInstalled(ScaledResolution resolution, float partialTicks,
                                             CallbackInfo callback) {
        ShoulderSurfingMatrixFix.markHookInstalled();
    }

    @Redirect(method = "offsetCrosshair", at = @At(value = "INVOKE",
            // Shoulder Surfing 2.9.6 is distributed in SRG form. This optional mixin has
            // remap=false because its target is a third-party class, so name the invoked
            // Minecraft method exactly as it appears in the installed 1.7.10 jar.
            target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V"))
    private void uie$confineCrosshairPush() {
        if (!ShoulderSurfingFixConfig.enabled()) GL11.glPushMatrix();
    }

    @Redirect(method = "offsetCrosshair", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V"))
    private void uie$confineCrosshairTranslation(float x, float y, float z) {
        if (!ShoulderSurfingFixConfig.enabled()) GL11.glTranslatef(x, y, z);
    }

    @Inject(method = "clearCrosshairOffset", at = @At("HEAD"), cancellable = true)
    private void uie$skipGlobalCrosshairPop(CallbackInfo callback) {
        if (ShoulderSurfingFixConfig.enabled()) callback.cancel();
    }
}
