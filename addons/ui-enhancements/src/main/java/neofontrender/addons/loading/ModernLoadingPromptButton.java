package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** A loading-safe button that does not depend on the vanilla widget texture. */
public final class ModernLoadingPromptButton extends GuiButton {
    private final boolean primary;

    public ModernLoadingPromptButton(int id, int x, int y, int width, String text,
                                     boolean primary) {
        super(id, x, y, width, 22, text);
        this.primary = primary;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int border = !enabled ? 0xFF343A42
                : primary ? (hovered ? 0xFF73F091 : 0xFF52E875)
                : (hovered ? 0xFF77818E : 0xFF525B66);
        int fill = !enabled ? 0xFF252A30
                : primary ? (hovered ? 0xFF285C38 : 0xFF214B30)
                : (hovered ? 0xFF343B44 : 0xFF292F36);
        int text = enabled ? 0xFFF4F7FA : 0xFF858C95;

        GlStateManager.enableBlend();
        Gui.drawRect(x, y, x + width, y + height, border);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        FontRenderer font = minecraft.fontRenderer;
        drawCenteredString(font, displayString, x + width / 2,
                y + (height - font.FONT_HEIGHT) / 2, text);
    }
}
