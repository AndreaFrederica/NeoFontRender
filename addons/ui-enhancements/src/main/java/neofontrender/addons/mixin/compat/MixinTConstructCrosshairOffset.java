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

/** Restricts Shoulder Surfing's offset to TiC's own item-crosshair draw call. */
@Pseudo
@Mixin(targets = "slimeknights.tconstruct.library.client.crosshair.CrosshairRenderEvents",
        remap = false)
public abstract class MixinTConstructCrosshairOffset {
    @Unique private boolean uie$crosshairMatrixPushed;

    @Inject(method = "onCrosshairRender", at = @At("HEAD"))
    private void uie$pushItemCrosshairOffset(RenderGameOverlayEvent.Pre event, CallbackInfo callback) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS
                || !CrosshairController.preferModCrosshairs()
                || !ShoulderSurfingMatrixFix.isTakingOver()) return;
        float[] offset = ShoulderSurfingMatrixFix.crosshairOffset();
        if (offset == null) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(offset[0], offset[1], 0.0F);
        uie$crosshairMatrixPushed = true;
    }

    @Inject(method = "onCrosshairRender", at = @At("RETURN"))
    private void uie$popItemCrosshairOffset(RenderGameOverlayEvent.Pre event, CallbackInfo callback) {
        if (!uie$crosshairMatrixPushed) return;
        uie$crosshairMatrixPushed = false;
        GL11.glPopMatrix();
    }
}
