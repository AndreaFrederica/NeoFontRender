package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import neofontrender.addons.flight.CrosshairController;
import neofontrender.addons.flight.ShoulderSurfingMatrixFix;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the shoulder offset only around Matter Overdrive's own crosshair method. */
@Pseudo
@Mixin(targets = "matteroverdrive.gui.GuiAndroidHud", remap = false)
public abstract class MixinMatterOverdriveCrosshairOffset {
    @Unique private boolean uie$crosshairMatrixPushed;

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void uie$pushWeaponCrosshairOffset(RenderGameOverlayEvent event, CallbackInfo callback) {
        if (!CrosshairController.preferModCrosshairs()
                || !ShoulderSurfingMatrixFix.isTakingOver()) return;
        float[] offset = ShoulderSurfingMatrixFix.crosshairOffset();
        if (offset == null) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(offset[0], offset[1], 0.0F);
        uie$crosshairMatrixPushed = true;
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void uie$popWeaponCrosshairOffset(RenderGameOverlayEvent event, CallbackInfo callback) {
        if (!uie$crosshairMatrixPushed) return;
        uie$crosshairMatrixPushed = false;
        GL11.glPopMatrix();
    }
}
