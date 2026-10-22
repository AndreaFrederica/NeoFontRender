package neofontrender.core.font.cosmic;

/** JNI ABI shared with NeoFontRender's production Cosmic backend. */
public final class CosmicNative {
    /** ABI 15 is shared with the production native library. */
    public static final int ABI_VERSION = 15;
    public static final int RASTER_MODEL_PAYLOAD_ALPHA8 = 32;
    public static final int RASTER_MODEL_PAYLOAD_RGB8 = 64;

    private CosmicNative() {}

    public static native int abiVersion();

    public static native long createEngine(byte[][] fonts, String[] fontAliases,
                                           String primaryFamily, String[] fallbackFamilies,
                                           String regularOverride, String boldOverride,
                                           String italicOverride, String boldItalicOverride,
                                           boolean variantOverridesOnlySwitchFont,
                                           int variableWeight, float fontSize, String locale);

    public static native float measureSized(long engine, String text, int styleFlags,
                                            float fontSize);

    public static native byte[] renderSized(long engine, String text, int argb, int styleFlags,
                                            float fontSize, float rasterScale);

    public static native byte[] renderOutlineSdfSized(long engine, String text, int styleFlags,
                                                       float fontSize, float rasterScale,
                                                       int distanceRange);

    public static native byte[] renderMsdfSized(long engine, String text, int styleFlags,
                                                 float fontSize, float rasterScale,
                                                 int distanceRange);

    public static native void destroyEngine(long engine);

    public static native String primaryFamily(long engine);

    public static native String resolvedFace(long engine, int styleFlags);

    public static native String resolutionWarnings(long engine);
}
