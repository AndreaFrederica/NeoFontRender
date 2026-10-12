package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

/** A loading-safe button that does not depend on the vanilla widget texture. */
public final class ModernLoadingPromptButton extends GuiButton {
    private final boolean primary;

    public ModernLoadingPromptButton(int id, int x, int y, int width, String text,
                                     boolean primary) {
        super(id, x, y, width, 22, text);
        this.primary = primary;
    }

    /**
     * 1.7.10's {@code GuiButton.drawButton} takes no partial-tick argument, the hover flag keeps its
     * SRG name, and {@code Gui.drawRect} already sets up source-over blending, which is why no
     * blending state has to be enabled here.
     */
    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        int border = !enabled ? 0xFF343A42
                : primary ? (field_146123_n ? 0xFF73F091 : 0xFF52E875)
                : (field_146123_n ? 0xFF77818E : 0xFF525B66);
        int fill = !enabled ? 0xFF252A30
                : primary ? (field_146123_n ? 0xFF285C38 : 0xFF214B30)
                : (field_146123_n ? 0xFF343B44 : 0xFF292F36);
        int text = enabled ? 0xFFF4F7FA : 0xFF858C95;

        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, border);
        Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1,
                fill);
        FontRenderer font = minecraft.fontRenderer;
        drawCenteredString(font, displayString, xPosition + width / 2,
                yPosition + (height - font.FONT_HEIGHT) / 2, text);
    }
}
