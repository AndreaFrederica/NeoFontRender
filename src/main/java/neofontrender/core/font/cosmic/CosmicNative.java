package neofontrender.core.font.cosmic;

/** JNI surface kept deliberately coarse-grained: one call shapes/rasterizes one complete run. */
final class CosmicNative {
    /** ABI 15 adds the outline-based glyph pipeline capability; ABI 16 adds per-glyph raster. */
    static final int ABI_VERSION = 17;

    static final int PROFILE_ASCII_MONOSPACE = 1;
    static final int PROFILE_TABULAR_DIGITS = 2;

    static final int RASTER_MODEL_MASK = 1;
    static final int RASTER_MODEL_FLAT_COLOR = 2;
    static final int RASTER_MODEL_GRADIENT_COLOR = 4;
    static final int RASTER_MODEL_OUTLINE_SDF = 8;
    static final int RASTER_MODEL_OUTLINE_MSDF = 16;
    static final int RASTER_MODEL_PAYLOAD_ALPHA8 = 32;
    static final int RASTER_MODEL_PAYLOAD_RGB8 = 64;

    private CosmicNative() {
    }

    static native int abiVersion();

    static native long createEngine(byte[][] fonts, String[] fontAliases, String primaryFamily,
                                    String[] fallbackFamilies,
                                    String regularOverride, String boldOverride,
                                    String italicOverride, String boldItalicOverride,
                                    boolean variantOverridesOnlySwitchFont,
                                    int variableWeight,
                                    float fontSize, String locale);

    static native float measure(long engine, String text, int styleFlags);

    static native byte[] render(long engine, String text, int argb, int styleFlags, float rasterScale);

    static native float measureSized(long engine, String text, int styleFlags, float fontSize);

    static native byte[] renderSized(long engine, String text, int argb, int styleFlags,
                                     float fontSize, float rasterScale);

    /**
     * ABI 16: rasterizes one code point without going through a Java string. Used by the
     * compatibility provider to fill per-glyph pages (monochrome mask or RGBA color).
     */
    static native byte[] renderGlyphSized(long engine, int codePoint, int argb, int styleFlags,
                                          float fontSize, float rasterScale);

    /**
     * ABI 16 batch entry: rasterizes a whole glyph page with one JNI transition and one engine
     * lock acquisition. Returns {@code [count][len][blob]...}, each blob in the normal COSM
     * raster format.
     */
    /** ABI 17: shapes the whole page once, then returns each shaped glyph as a separate blob. */
    static native byte[] renderGlyphPage(long engine, int[] codePoints, int argb, int styleFlags,
                                        float fontSize, float rasterScale);

    static native byte[] renderGlyphBatch(long engine, int[] codePoints, int argb, int styleFlags,
                                          float fontSize, float rasterScale);

    static native byte[] renderOutlineSdfSized(long engine, String text, int styleFlags,
                                               float fontSize, float rasterScale,
                                               int distanceRange);

    static native byte[] renderMsdfSized(long engine, String text, int styleFlags,
                                          float fontSize, float rasterScale, int distanceRange);

    /** Flat UTF-16 start/end pairs emitted by cosmic-text's shaped LayoutGlyph clusters. */
    static native int[] clusterRangesSized(long engine, String text, int styleFlags, float fontSize);

    /** Validated UTF-16 split endpoints, or empty when independent characters would change shaping. */
    static native int[] monospaceSplitPointsSized(long engine, String text, int styleFlags,
                                                 float fontSize, float rasterScale);

    static native int monospaceProfileFlagsSized(long engine, int styleFlags,
                                                 float fontSize, float rasterScale);

    static native void destroyEngine(long engine);

    static native String primaryFamily(long engine);

    static native String resolvedFace(long engine, int styleFlags);

    static native String resolutionWarnings(long engine);
}
