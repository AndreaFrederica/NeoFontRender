package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import neofontrender.addons.worldcreation.CreateWorldConfig;
import neofontrender.addons.hover.HoverAnimationAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiButton.class)
public abstract class MixinGuiButtonCreateWorldStyle {
    /** 1.7.10 name of GuiButton.hovered. */
    @Shadow protected boolean field_146123_n;

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawCreateWorldButton(Minecraft minecraft, int mouseX, int mouseY,
                                             CallbackInfo ci) {
        if (!CreateWorldConfig.usesModernStyle() || minecraft.currentScreen == null
                || minecraft.currentScreen.getClass() != GuiCreateWorld.class) return;

        GuiButton button = (GuiButton) (Object) this;
        if (!button.visible) {
            ci.cancel();
            return;
        }
        field_146123_n = mouseX >= button.xPosition && mouseY >= button.yPosition
                && mouseX < button.xPosition + button.width && mouseY < button.yPosition + button.height;
        ((HoverAnimationAccess) button).nfrUi$updateHoverAnimation(field_146123_n && button.enabled);
        float hoverProgress = HoverEffectsRenderer.visualProgress(button);
        boolean tab = button.id == 28640 || button.id == 28641;
        boolean selectedTab = tab && !button.enabled;

        int fill;
        int border;
        if (tab) {
            fill = selectedTab ? 0xD022282E
                    : HoverEffectsRenderer.interpolateColor(0x7014171B, 0xB02A3138, hoverProgress);
            border = selectedTab ? 0xFF52E875
                    : HoverEffectsRenderer.interpolateColor(0xFF343B42, 0xFF6D777F, hoverProgress);
        } else {
            fill = button.enabled
                    ? HoverEffectsRenderer.interpolateColor(0xD020272D, 0xE02B343B, hoverProgress)
                    : 0xB0181D22;
            border = button.enabled
                    ? HoverEffectsRenderer.interpolateColor(0xFF4A535B, 0xFF52E875, hoverProgress)
                    : 0xFF4A535B;
        }

        Gui.drawRect(button.xPosition, button.yPosition, button.xPosition + button.width, button.yPosition + button.height, fill);
        Gui.drawRect(button.xPosition, button.yPosition, button.xPosition + button.width, button.yPosition + 1, border);
        Gui.drawRect(button.xPosition, button.yPosition + button.height - (selectedTab ? 2 : 1),
                button.xPosition + button.width, button.yPosition + button.height, border);
        Gui.drawRect(button.xPosition, button.yPosition, button.xPosition + 1, button.yPosition + button.height, border);
        Gui.drawRect(button.xPosition + button.width - 1, button.yPosition,
                button.xPosition + button.width, button.yPosition + button.height, border);

        int color = button.packedFGColour != 0 ? button.packedFGColour
                : !button.enabled && !selectedTab ? 0xFF777D84
                : selectedTab ? 0xFF52E875
                : HoverEffectsRenderer.interpolateColor(0xFFD3D7DC, 0xFFFFFFFF, hoverProgress);
        int textX = button.xPosition + (button.width - minecraft.fontRenderer.getStringWidth(button.displayString)) / 2;
        int textY = button.yPosition + (button.height - minecraft.fontRenderer.FONT_HEIGHT) / 2 + 1;
        minecraft.fontRenderer.drawString(button.displayString, textX, textY, color, false);
        ci.cancel();
    }
}
