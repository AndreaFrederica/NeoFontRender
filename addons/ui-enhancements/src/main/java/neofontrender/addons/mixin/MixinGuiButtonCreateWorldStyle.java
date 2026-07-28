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
    @Shadow protected boolean hovered;

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawCreateWorldButton(Minecraft minecraft, int mouseX, int mouseY,
                                             float partialTicks, CallbackInfo ci) {
        if (!CreateWorldConfig.usesModernStyle() || minecraft.currentScreen == null
                || minecraft.currentScreen.getClass() != GuiCreateWorld.class) return;

        GuiButton button = (GuiButton) (Object) this;
        if (!button.visible) {
            ci.cancel();
            return;
        }
        hovered = mouseX >= button.x && mouseY >= button.y
                && mouseX < button.x + button.width && mouseY < button.y + button.height;
        ((HoverAnimationAccess) button).nfrUi$updateHoverAnimation(hovered && button.enabled);
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

        Gui.drawRect(button.x, button.y, button.x + button.width, button.y + button.height, fill);
        Gui.drawRect(button.x, button.y, button.x + button.width, button.y + 1, border);
        Gui.drawRect(button.x, button.y + button.height - (selectedTab ? 2 : 1),
                button.x + button.width, button.y + button.height, border);
        Gui.drawRect(button.x, button.y, button.x + 1, button.y + button.height, border);
        Gui.drawRect(button.x + button.width - 1, button.y,
                button.x + button.width, button.y + button.height, border);

        int color = button.packedFGColour != 0 ? button.packedFGColour
                : !button.enabled && !selectedTab ? 0xFF777D84
                : selectedTab ? 0xFF52E875
                : HoverEffectsRenderer.interpolateColor(0xFFD3D7DC, 0xFFFFFFFF, hoverProgress);
        int textX = button.x + (button.width - minecraft.fontRenderer.getStringWidth(button.displayString)) / 2;
        int textY = button.y + (button.height - minecraft.fontRenderer.FONT_HEIGHT) / 2 + 1;
        minecraft.fontRenderer.drawString(button.displayString, textX, textY, color, false);
        ci.cancel();
    }
}
