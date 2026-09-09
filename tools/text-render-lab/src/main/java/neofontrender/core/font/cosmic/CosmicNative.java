package neofontrender.core.font.cosmic;

/** JNI ABI shared with NeoFontRender's production Cosmic backend. */
public final class CosmicNative {
    public static final int ABI_VERSION = 12;

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

    public static native void destroyEngine(long engine);

    public static native String primaryFamily(long engine);

    public static native String resolvedFace(long engine, int styleFlags);

    public static native String resolutionWarnings(long engine);
}
