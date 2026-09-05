package neofontrender.text;

import java.util.Arrays;

/** Immutable premultiplied-independent ARGB raster owned by an inline text object. */
public final class InlineRaster {
    private final int width;
    private final int height;
    private final int[] argb;

    public InlineRaster(int width, int height, int[] argb) {
        if (width <= 0 || height <= 0 || argb == null || argb.length != width * height) {
            throw new IllegalArgumentException("Invalid inline raster");
        }
        this.width = width;
        this.height = height;
        this.argb = argb.clone();
    }

    public int width() { return width; }
    public int height() { return height; }
    public int[] argb() { return argb.clone(); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InlineRaster)) return false;
        InlineRaster raster = (InlineRaster) other;
        return width == raster.width && height == raster.height && Arrays.equals(argb, raster.argb);
    }

    @Override public int hashCode() {
        return 31 * (31 * width + height) + Arrays.hashCode(argb);
    }
}
