package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import neofontrender.addons.hover.HoverAnimationAccess;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiButton.class)
public abstract class MixinGuiButtonHover implements HoverAnimationAccess {
    // 1.7.10 leaves GuiButton#hovered unmapped (SRG field_146123_n).
    @Shadow protected boolean field_146123_n;
    @Shadow public boolean enabled;

    @Unique private final HoverAnimationState nfrUi$hoverAnimation = new HoverAnimationState();

    // ModularUI adjusts hovered in its getHoverState redirect. This point deliberately runs afterwards.
    // 1.7.10 drawButton has no GlStateManager; the raw GL11.glEnable(GL_BLEND) call sits at the same spot.
    @Inject(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", remap = false))
    private void nfrUi$updateVanillaHover(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci) {
        nfrUi$updateHoverAnimation(field_146123_n && enabled);
    }

    @Redirect(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiButton;drawTexturedModalRect(IIIIII)V"))
    private void nfrUi$crossFadeButtonTexture(GuiButton button, int x, int y, int textureX,
                                               int textureY, int width, int height) {
        HoverEffectsRenderer.drawVanillaButtonPart(button, x, y, textureX, textureY, width, height);
    }

    @Redirect(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiButton;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"))
    private void nfrUi$crossFadeButtonText(GuiButton button, FontRenderer font, String text,
                                            int x, int y, int color) {
        button.drawCenteredString(font, text, x, y, HoverEffectsRenderer.buttonTextColor(button, color));
    }

    @Override
    public void nfrUi$updateHoverAnimation(boolean active) {
        if (!HoverEffectsConfigAccess.buttonsEnabled()) {
            nfrUi$hoverAnimation.reset(active);
            return;
        }
        nfrUi$hoverAnimation.update(active, HoverEffectsConfigAccess.buttonEnterMillis(),
                HoverEffectsConfigAccess.buttonExitMillis());
    }

    @Override
    public float nfrUi$hoverProgress() {
        return nfrUi$hoverAnimation.easedProgress();
    }
}
