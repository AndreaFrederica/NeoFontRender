package neofontrender.core.font.support;

/**
 * Pixel cleanup for straight-alpha font textures.
 *
 * <p>Linear filtering samples RGB even when alpha is zero. Keeping transparent
 * pixels black makes glyph edges pick up a dark/gray fringe when the oversized
 * raster is scaled back to GUI space.</p>
 */
public final class FontPixelUtils {

    public static final int TRANSPARENT_WHITE = 0x00FFFFFF;

    private FontPixelUtils() {
    }

    public static void normalizeWhiteStraightAlpha(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            int alpha = pixels[i] >>> 24;
            pixels[i] = alpha == 0 ? TRANSPARENT_WHITE : (alpha << 24) | 0x00FFFFFF;
        }
    }

    public static void normalizeTransparentRgb(int[] pixels, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int[] source = pixels.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((source[index] >>> 24) != 0) {
                    continue;
                }

                int red = 0;
                int green = 0;
                int blue = 0;
                int count = 0;
                for (int oy = -1; oy <= 1; oy++) {
                    int ny = y + oy;
                    if (ny < 0 || ny >= height) {
                        continue;
                    }
                    for (int ox = -1; ox <= 1; ox++) {
                        int nx = x + ox;
                        if ((ox == 0 && oy == 0) || nx < 0 || nx >= width) {
                            continue;
                        }
                        int neighbor = source[ny * width + nx];
                        if ((neighbor >>> 24) == 0) {
                            continue;
                        }
                        red += (neighbor >>> 16) & 0xFF;
                        green += (neighbor >>> 8) & 0xFF;
                        blue += neighbor & 0xFF;
                        count++;
                    }
                }

                if (count > 0) {
                    pixels[index] = (red / count << 16) | (green / count << 8) | (blue / count);
                } else {
                    pixels[index] = TRANSPARENT_WHITE;
                }
            }
        }
    }

    public static int[] scaleNearest(int[] pixels, int width, int height, int scale) {
        if (pixels == null || width <= 0 || height <= 0 || scale <= 1) {
            return pixels;
        }
        int newWidth = width * scale;
        int newHeight = height * scale;
        int[] out = new int[newWidth * newHeight];
        for (int y = 0; y < height; y++) {
            int srcRow = y * width;
            for (int sy = 0; sy < scale; sy++) {
                int dstRow = (y * scale + sy) * newWidth;
                for (int x = 0; x < width; x++) {
                    int value = pixels[srcRow + x];
                    int dst = dstRow + x * scale;
                    for (int sx = 0; sx < scale; sx++) {
                        out[dst + sx] = value;
                    }
                }
            }
        }
        return out;
    }
}
