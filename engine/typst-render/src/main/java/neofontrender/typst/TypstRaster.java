package neofontrender.typst;

/** Immutable straight-alpha ARGB raster returned by the Typst JNI module. */
public final class TypstRaster {
    private final int width;
    private final int height;
    private final int[] argb;

    TypstRaster(int width, int height, int[] argb) {
        this.width = width;
        this.height = height;
        this.argb = argb;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int[] argb() { return argb.clone(); }
}
