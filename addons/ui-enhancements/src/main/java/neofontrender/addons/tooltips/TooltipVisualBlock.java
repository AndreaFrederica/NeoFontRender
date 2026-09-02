package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;

/** Measured non-text content owned and positioned by the modern tooltip renderer. */
interface TooltipVisualBlock {
    int width();

    int height();

    default TooltipVisualBlock constrain(int maxWidth) {
        return this;
    }

    void draw(int x, int y, FontRenderer font);

    default String debugLabel() {
        return "visual";
    }
}
