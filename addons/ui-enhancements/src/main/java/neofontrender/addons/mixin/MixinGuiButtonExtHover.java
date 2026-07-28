package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import neofontrender.addons.hover.HoverAnimationAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiButtonExt.class, remap = false)
public abstract class MixinGuiButtonExtHover {
    @Redirect(method = "drawButton", remap = true, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/fml/client/config/GuiUtils;drawContinuousTexturedBox(Lnet/minecraft/util/ResourceLocation;IIIIIIIIIIIIF)V"))
    private void nfrUi$crossFadeForgeButton(ResourceLocation texture, int x, int y, int textureX,
                                             int textureY, int width, int height, int textureWidth,
                                             int textureHeight, int topBorder, int bottomBorder,
                                             int leftBorder, int rightBorder, float zLevel) {
        GuiButton button = (GuiButton) (Object) this;
        ((HoverAnimationAccess) button).nfrUi$updateHoverAnimation(button.isMouseOver() && button.enabled);
        HoverEffectsRenderer.drawForgeButton(button, texture, x, y, textureX,
                textureY, width, height, textureWidth, textureHeight, topBorder, bottomBorder,
                leftBorder, rightBorder, zLevel);
    }

    @Redirect(method = "drawButton", remap = true, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraftforge/fml/client/config/GuiButtonExt;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"))
    private void nfrUi$crossFadeForgeButtonText(GuiButtonExt button, FontRenderer font, String text,
                                                 int x, int y, int color) {
        GuiButton guiButton = (GuiButton) button;
        guiButton.drawCenteredString(font, text, x, y,
                HoverEffectsRenderer.buttonTextColor(guiButton, color));
    }
}
