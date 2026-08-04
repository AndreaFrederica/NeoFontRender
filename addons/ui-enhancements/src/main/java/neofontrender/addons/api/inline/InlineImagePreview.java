package neofontrender.addons.api.inline;

/** Shared sizing rules for compact and Alt natural-size image previews. */
public final class InlineImagePreview {
    private InlineImagePreview() {}

    /**
     * Fits the natural image size inside the available area without ever enlarging it.
     * Unknown dimensions fall back to the supplied compact square size.
     */
    public static int[] naturalSize(InlineGlyph glyph, int maximumWidth, int maximumHeight,
                                    int fallbackSize) {
        int width = glyph == null ? -1 : glyph.previewWidth();
        int height = glyph == null ? -1 : glyph.previewHeight();
        return naturalSize(width, height, maximumWidth, maximumHeight, fallbackSize);
    }

    public static int[] naturalSize(int width, int height, int maximumWidth, int maximumHeight,
                                    int fallbackSize) {
        if (width <= 0 || height <= 0) {
            int size = Math.max(1, Math.min(fallbackSize,
                    Math.min(maximumWidth, maximumHeight)));
            return new int[] { size, size };
        }
        float scale = Math.min(1.0F, Math.min(maximumWidth / (float) width,
                maximumHeight / (float) height));
        return new int[] { Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale)) };
    }
}
