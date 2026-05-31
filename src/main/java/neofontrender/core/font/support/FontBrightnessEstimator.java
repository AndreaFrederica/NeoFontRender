package neofontrender.core.font.support;

import neofontrender.NeoFontRender;

/**
 * Automatic brightness estimator for antialiased font edges.
 *
 * <p>Ported from SmoothFont's auto-brightness detection. It rasterizes sample
 * characters ('1', '/', 'I'), sums opacity along their horizontal mid-line,
 * and derives a brightness compensation value (0-20) and a boundary scale
 * factor for per-magnification adaptive correction.</p>
 */
public final class FontBrightnessEstimator {

    private static int cachedBrightness = -1;
    private static float cachedBoundaryScaleFactor = 2.0f;
    private static boolean autoDetected = false;

    private FontBrightnessEstimator() {
    }

    /**
     * Computes brightness from a rasterized glyph's pixel array.
     *
     * @param pixels    ARGB pixel array (straight alpha)
     * @param width     image width
     * @param height    image height
     * @param fontRes   effective font resolution in pixels
     * @return true if valid samples were extracted
     */
    public static boolean feedSample(int[] pixels, int width, int height, int fontRes) {
        if (pixels == null || width <= 0 || height <= 0 || fontRes <= 0) {
            return false;
        }
        int posY = Math.min(height - 1, Math.max(0, (int) (height * 0.75f)));
        int opacity = getTotalOpacityPosY(pixels, width, height, posY);
        if (opacity == 0) {
            return false;
        }

        int normalizedOpacity = opacity / fontRes;
        normalizedOpacity = Math.max(1, normalizedOpacity);
        int estimatedBrightness = 60 / normalizedOpacity;

        float boundary = 255.0f / opacity * fontRes / 8.0f;

        synchronized (FontBrightnessEstimator.class) {
            if (!autoDetected) {
                cachedBrightness = 255; // sentinel for first sample
                cachedBoundaryScaleFactor = 2.0f;
                autoDetected = true;
            }
            cachedBrightness = Math.min(cachedBrightness, estimatedBrightness);
            cachedBoundaryScaleFactor = Math.max(cachedBoundaryScaleFactor, boundary);
        }

        NeoFontRender.LOGGER.debug(
                "FontBrightnessEstimator sample: opacity={}, normalized={}, brightness={}, boundary={}",
                opacity, normalizedOpacity, estimatedBrightness, boundary);
        return true;
    }

    public static void reset() {
        synchronized (FontBrightnessEstimator.class) {
            cachedBrightness = -1;
            cachedBoundaryScaleFactor = 2.0f;
            autoDetected = false;
        }
    }

    public static int getBrightness() {
        synchronized (FontBrightnessEstimator.class) {
            if (cachedBrightness < 0 || cachedBrightness > 20) {
                return 3; // default fallback matching SmoothFont
            }
            return cachedBrightness;
        }
    }

    public static float getBoundaryScaleFactor() {
        synchronized (FontBrightnessEstimator.class) {
            return cachedBoundaryScaleFactor;
        }
    }

    public static boolean isAutoDetected() {
        synchronized (FontBrightnessEstimator.class) {
            return autoDetected;
        }
    }

    private static int getTotalOpacityPosY(int[] pixels, int width, int height, int posY) {
        if (posY < 0 || posY >= height) {
            return 0;
        }
        int opacity = 0;
        int offset = posY * width;
        for (int x = 0; x < width; x++) {
            opacity += (pixels[offset + x] >>> 24) & 0xFF;
        }
        return opacity;
    }
}
