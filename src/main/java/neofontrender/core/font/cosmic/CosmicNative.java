package neofontrender.core.font.cosmic;

/** JNI surface kept deliberately coarse-grained: one call shapes/rasterizes one complete run. */
final class CosmicNative {
    static final int ABI_VERSION = 13;

    static final int RASTER_MODEL_MASK = 1;
    static final int RASTER_MODEL_FLAT_COLOR = 2;
    static final int RASTER_MODEL_GRADIENT_COLOR = 4;

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

    /** Flat UTF-16 start/end pairs emitted by cosmic-text's shaped LayoutGlyph clusters. */
    static native int[] clusterRangesSized(long engine, String text, int styleFlags, float fontSize);

    /** Validated UTF-16 split endpoints, or empty when independent characters would change shaping. */
    static native int[] monospaceSplitPointsSized(long engine, String text, int styleFlags,
                                                 float fontSize, float rasterScale);

    static native void destroyEngine(long engine);

    static native String primaryFamily(long engine);

    static native String resolvedFace(long engine, int styleFlags);

    static native String resolutionWarnings(long engine);
}
