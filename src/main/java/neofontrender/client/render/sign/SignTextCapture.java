package neofontrender.client.render.sign;

import net.minecraft.client.gui.FontRenderer;

/**
 * Per-render-thread fallback data for batched sign text.
 *
 * This deliberately lives outside {@code neofontrender.mixin}: modern Mixin
 * treats every class in a configured mixin package as a mixin-owned class and
 * rejects direct loading of ordinary helper classes from that package.
 */
public final class SignTextCapture {
    private final FontRenderer renderer;
    private final String[] lines = new String[4];
    private final int[] x = new int[4];
    private final int[] y = new int[4];
    private final int[] color = new int[4];

    public SignTextCapture(FontRenderer renderer) {
        this.renderer = renderer;
    }

    public void capture(int line, String text, int x, int y, int color) {
        lines[line] = text == null ? "" : text;
        this.x[line] = x;
        this.y[line] = y;
        this.color[line] = color;
    }

    public String[] lines() {
        return lines;
    }

    public void replay() {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                renderer.drawString(lines[i], x[i], y[i], color[i]);
            }
        }
    }
}
