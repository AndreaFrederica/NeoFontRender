package neofontrender.typst.internal;

/** Coarse-grained JNI surface for the isolated Typst engine. */
public final class TypstNative {
    public static final int ABI_VERSION = 1;

    private TypstNative() {}

    public static native int abiVersion();
    public static native long createEngine(String libraryDirectory);
    public static native byte[] render(long engine, String source, float scale);
    public static native void destroyEngine(long engine);
}
