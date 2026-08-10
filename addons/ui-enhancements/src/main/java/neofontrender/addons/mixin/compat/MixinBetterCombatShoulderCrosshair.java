package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import neofontrender.addons.flight.CrosshairController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ports Shoulder Surfing's Better Combat crosshair visibility transformer as a late Mixin. */
@Pseudo
@Mixin(targets = "bettercombat.mod.client.gui.GuiCrosshairsBC", remap = false)
public abstract class MixinBetterCombatShoulderCrosshair {
    @Unique private boolean nfrUi$crosshairMatrixPushed;

    @Inject(method = "renderAttackIndicator", at = @At("HEAD"), remap = false, require = 1)
    private void nfrUi$pushShoulderCrosshairOffset(CallbackInfo callback) {
        float[] offset = CrosshairController.preferredModCrosshairOffset(
                Minecraft.getMinecraft().getRenderPartialTicks());
        if (offset == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(offset[0], offset[1], 0.0F);
        nfrUi$crosshairMatrixPushed = true;
    }

    @Inject(method = "renderAttackIndicator", at = @At("RETURN"), remap = false, require = 1)
    private void nfrUi$popShoulderCrosshairOffset(CallbackInfo callback) {
        if (!nfrUi$crosshairMatrixPushed) return;
        nfrUi$crosshairMatrixPushed = false;
        GlStateManager.popMatrix();
    }

    @Redirect(method = "renderAttackIndicator", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/settings/GameSettings;thirdPersonView:I",
            remap = true), remap = false, require = 1)
    private int nfrUi$shoulderCrosshairVisibility(GameSettings settings) {
        return CrosshairController.cameraCrosshairVisible(
                Minecraft.getMinecraft().getRenderPartialTicks()) ? 0 : 1;
    }
}
