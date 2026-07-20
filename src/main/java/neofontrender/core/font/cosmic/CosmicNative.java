package neofontrender.core.font.cosmic;

/** JNI surface kept deliberately coarse-grained: one call shapes/rasterizes one complete run. */
final class CosmicNative {
    static final int ABI_VERSION = 7;

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

    static native void destroyEngine(long engine);

    static native String primaryFamily(long engine);

    static native String resolvedFace(long engine, int styleFlags);

    static native String resolutionWarnings(long engine);
}
