package neofontrender.api.text.pipeline;

import net.minecraft.client.gui.FontRenderer;

/** A measured non-text object participating in standard NFR text layout. */
public interface InlineContent {
    int advance(FontRenderer font);
    int height(FontRenderer font);
    void draw(float x, float y, int argb, boolean shadow);
    String description();

    default void drawPreview(float x, float y, int size, int argb) {
        draw(x, y, argb, false);
    }

    default int previewWidth() { return -1; }
    default int previewHeight() { return -1; }

    default void drawPreview(float x, float y, int width, int height, int argb) {
        drawPreview(x, y, Math.min(width, height), argb);
    }

    default boolean copyImageToClipboard() { return false; }
}
