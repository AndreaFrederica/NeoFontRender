package neofontrender.addons.mixin;

import cpw.mods.fml.client.config.GuiButtonExt;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.hover.HoverAnimationAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiButtonExt.class, remap = false)
public abstract class MixinGuiButtonExtHover {
    @Redirect(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lcpw/mods/fml/client/config/GuiUtils;drawContinuousTexturedBox(Lnet/minecraft/util/ResourceLocation;IIIIIIIIIIIIF)V"))
    private void nfrUi$crossFadeForgeButton(ResourceLocation texture, int x, int y, int textureX,
                                             int textureY, int width, int height, int textureWidth,
                                             int textureHeight, int topBorder, int bottomBorder,
                                             int leftBorder, int rightBorder, float zLevel) {
        GuiButton button = (GuiButton) (Object) this;
        // 1.7.10 leaves GuiButton#isMouseOver unmapped (func_146115_a).
        ((HoverAnimationAccess) button).nfrUi$updateHoverAnimation(button.func_146115_a() && button.enabled);
        HoverEffectsRenderer.drawForgeButton(button, texture, x, y, textureX,
                textureY, width, height, textureWidth, textureHeight, topBorder, bottomBorder,
                leftBorder, rightBorder, zLevel);
    }

    @Redirect(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lcpw/mods/fml/client/config/GuiButtonExt;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"))
    private void nfrUi$crossFadeForgeButtonText(GuiButtonExt button, FontRenderer font, String text,
                                                 int x, int y, int color) {
        GuiButton guiButton = (GuiButton) button;
        guiButton.drawCenteredString(font, text, x, y,
                HoverEffectsRenderer.buttonTextColor(guiButton, color));
    }
}
