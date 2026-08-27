package neofontrender.core.font.support;

/** Builds a colored soft shadow under an existing ARGB glyph raster. */
public final class ModernShadowRasterizer {
    private ModernShadowRasterizer() {}

    public static Result compose(int[] foreground, int width, int height, float scale,
                                 float offsetX, float offsetY, float blurRadius,
                                 int color, float opacity, boolean premultiplied) {
        Result result = shadow(foreground, width, height, scale, offsetX, offsetY, blurRadius,
                color, opacity, premultiplied);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int source = foreground[y * width + x];
                int index = (result.originY + y) * result.width + result.originX + x;
                result.pixels[index] = sourceOver(result.pixels[index], source, premultiplied);
            }
        }
        return result;
    }

    /** Builds only the shadow layer, preserving the original glyph coverage for a later draw. */
    public static Result shadow(int[] foreground, int width, int height, float scale,
                               float offsetX, float offsetY, float blurRadius,
                               int color, float opacity, boolean premultiplied) {
        float blurPixels = Math.max(0.0F, blurRadius) * Math.max(0.0F, scale);
        int kernelRadius = gaussianKernelRadius(blurPixels);
        int dx = Math.round(offsetX * scale);
        int dy = Math.round(offsetY * scale);
        int left = kernelRadius + Math.max(0, -dx);
        int top = kernelRadius + Math.max(0, -dy);
        int right = kernelRadius + Math.max(0, dx);
        int bottom = kernelRadius + Math.max(0, dy);
        int outWidth = width + left + right;
        int outHeight = height + top + bottom;
        int[] mask = new int[outWidth * outHeight];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int targetX = left + x + dx;
                int targetY = top + y + dy;
                if (targetX >= 0 && targetX < outWidth && targetY >= 0 && targetY < outHeight) {
                    mask[targetY * outWidth + targetX] =
                            Math.max(mask[targetY * outWidth + targetX],
                                    foreground[y * width + x] >>> 24);
                }
            }
        }
        if (blurPixels > 0.0F) mask = gaussianBlur(mask, outWidth, outHeight, blurPixels);

        int[] output = new int[outWidth * outHeight];
        int colorAlpha = color >>> 24;
        int colorR = color >> 16 & 255;
        int colorG = color >> 8 & 255;
        int colorB = color & 255;
        float alphaScale = Math.max(0.0F, Math.min(1.0F, opacity)) * colorAlpha / 255.0F;
        for (int i = 0; i < output.length; i++) {
            int alpha = Math.min(255, Math.round(mask[i] * alphaScale));
            int r = premultiplied ? colorR * alpha / 255 : colorR;
            int g = premultiplied ? colorG * alpha / 255 : colorG;
            int b = premultiplied ? colorB * alpha / 255 : colorB;
            output[i] = alpha << 24 | r << 16 | g << 8 | b;
        }
        return new Result(output, outWidth, outHeight, left, top);
    }

    private static int gaussianKernelRadius(float blurPixels) {
        if (!(blurPixels > 0.0F) || !Float.isFinite(blurPixels)) return 0;
        float sigma = sigmaFor(blurPixels);
        return Math.max(1, (int) Math.ceil(3.0F * sigma));
    }

    private static float sigmaFor(float blurPixels) {
        // Keep very small radii visibly soft while retaining a predictable pixel-space control.
        return Math.max(0.5F, blurPixels / 3.0F);
    }

    private static int[] gaussianBlur(int[] source, int width, int height, float blurPixels) {
        int radius = gaussianKernelRadius(blurPixels);
        float sigma = sigmaFor(blurPixels);
        float[] kernel = new float[radius * 2 + 1];
        float normalizer = 0.0F;
        for (int i = -radius; i <= radius; i++) {
            float weight = (float) Math.exp(-(i * (double) i) / (2.0 * sigma * sigma));
            kernel[i + radius] = weight;
            normalizer += weight;
        }
        for (int i = 0; i < kernel.length; i++) kernel[i] /= normalizer;

        // Keep coverage in floating point through both passes. Rounding after each pass creates
        // visible banding at low opacity and makes the result depend on pass order.
        float[] horizontal = new float[source.length];
        int[] output = new int[source.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float value = 0.0F;
                for (int kernelIndex = -radius; kernelIndex <= radius; kernelIndex++) {
                    int sampleX = x + kernelIndex;
                    if (sampleX >= 0 && sampleX < width) {
                        value += source[y * width + sampleX]
                                * kernel[kernelIndex + radius];
                    }
                }
                horizontal[y * width + x] = value;
            }
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float value = 0.0F;
                for (int kernelIndex = -radius; kernelIndex <= radius; kernelIndex++) {
                    int sampleY = y + kernelIndex;
                    if (sampleY >= 0 && sampleY < height) {
                        value += horizontal[sampleY * width + x]
                                * kernel[kernelIndex + radius];
                    }
                }
                output[y * width + x] = Math.max(0, Math.min(255, Math.round(value)));
            }
        }
        return output;
    }

    private static int sourceOver(int background, int foreground, boolean premultiplied) {
        int fa = foreground >>> 24;
        int ba = background >>> 24;
        int inverse = 255 - fa;
        int outA = fa + ba * inverse / 255;
        if (premultiplied) {
            int r = (foreground >> 16 & 255) + (background >> 16 & 255) * inverse / 255;
            int g = (foreground >> 8 & 255) + (background >> 8 & 255) * inverse / 255;
            int b = (foreground & 255) + (background & 255) * inverse / 255;
            return outA << 24 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
        }
        if (outA == 0) return 0;
        int r = ((foreground >> 16 & 255) * fa
                + (background >> 16 & 255) * ba * inverse / 255) / outA;
        int g = ((foreground >> 8 & 255) * fa
                + (background >> 8 & 255) * ba * inverse / 255) / outA;
        int b = ((foreground & 255) * fa
                + (background & 255) * ba * inverse / 255) / outA;
        return outA << 24 | r << 16 | g << 8 | b;
    }

    public static final class Result {
        public final int[] pixels;
        public final int width;
        public final int height;
        public final int originX;
        public final int originY;

        private Result(int[] pixels, int width, int height, int originX, int originY) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.originX = originX;
            this.originY = originY;
        }
    }
}
