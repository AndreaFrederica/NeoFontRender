package neofontrender.core.font.cosmic;

import java.nio.ByteBuffer;

/**
 * Copies the base-color layer returned by cosmic-text into Minecraft's dynamic texture storage.
 *
 * <p>Vanilla allocates exactly {@code width * height} entries. OptiFine may allocate three times
 * that amount in the same array for base, normal, and specular layers. Only the first layer belongs
 * to the cosmic-text raster; the additional layers must retain the defaults supplied by
 * OptiFine.</p>
 */
final class CosmicRasterPixels {
    private CosmicRasterPixels() {}

    static void copyBaseLayer(ByteBuffer source, int pixelCount, int[] target) {
        if (pixelCount < 0) {
            throw new IllegalStateException("cosmic-text returned a negative pixel count");
        }
        if (target == null || target.length < pixelCount) {
            int actual = target == null ? 0 : target.length;
            throw new IllegalStateException("dynamic texture storage is too small: "
                    + actual + " < " + pixelCount);
        }
        long requiredBytes = pixelCount * 4L;
        if (requiredBytes > Integer.MAX_VALUE || source.remaining() < requiredBytes) {
            throw new IllegalStateException("cosmic-text pixel payload is truncated: "
                    + source.remaining() + " < " + requiredBytes);
        }

        source.asIntBuffer().get(target, 0, pixelCount);
        source.position(source.position() + (int) requiredBytes);
    }
}
