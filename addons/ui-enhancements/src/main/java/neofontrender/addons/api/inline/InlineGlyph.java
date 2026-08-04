package neofontrender.addons.api.inline;

import net.minecraft.client.gui.FontRenderer;

/**
 * A non-text object that participates in UIE text measurement and drawing.
 * Implementations must keep {@link #advance(FontRenderer)} stable while a layout is visible.
 */
public interface InlineGlyph {
    int advance(FontRenderer font);

    int height(FontRenderer font);

    /** Draw at the top-left of the glyph's layout box. */
    void draw(float x, float y, int argb, boolean shadow);

    /** Human-readable text used by hover previews and accessibility-oriented callers. */
    String description();

    /** Optional enlarged hover rendering. The default reuses the normal glyph drawing path. */
    default void drawPreview(float x, float y, int size, int argb) {
        draw(x, y, argb, false);
    }

    /** Natural decoded preview width, or a non-positive value when it is not known. */
    default int previewWidth() { return -1; }

    /** Natural decoded preview height, or a non-positive value when it is not known. */
    default int previewHeight() { return -1; }

    /** Draws a complete image inside an arbitrary preview box. */
    default void drawPreview(float x, float y, int width, int height, int argb) {
        drawPreview(x, y, Math.min(width, height), argb);
    }

    /** Copies the decoded image to the operating-system clipboard when this is an image glyph. */
    default boolean copyImageToClipboard() {
        return false;
    }
}
