package neofontrender.core.font.support;

/** Builds a colored soft shadow under an existing ARGB glyph raster. */
public final class ModernShadowRasterizer {
    private ModernShadowRasterizer() {}

    public static Result compose(int[] foreground, int width, int height, float scale,
                                 float offsetX, float offsetY, float blurRadius,
                                 int color, float opacity, boolean premultiplied) {
        int radius = Math.max(0, Math.round(Math.max(0.0F, blurRadius) * scale));
        int dx = Math.round(offsetX * scale);
        int dy = Math.round(offsetY * scale);
        int left = radius + Math.max(0, -dx);
        int top = radius + Math.max(0, -dy);
        int right = radius + Math.max(0, dx);
        int bottom = radius + Math.max(0, dy);
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
        if (radius > 0) mask = boxBlur(mask, outWidth, outHeight, radius);

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
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int source = foreground[y * width + x];
                int index = (top + y) * outWidth + left + x;
                output[index] = sourceOver(output[index], source, premultiplied);
            }
        }
        return new Result(output, outWidth, outHeight, left, top);
    }

    private static int[] boxBlur(int[] source, int width, int height, int radius) {
        int[] horizontal = new int[source.length];
        int[] output = new int[source.length];
        int diameter = radius * 2 + 1;
        for (int y = 0; y < height; y++) {
            int sum = 0;
            for (int x = -radius; x <= radius; x++) {
                if (x >= 0 && x < width) sum += source[y * width + x];
            }
            for (int x = 0; x < width; x++) {
                horizontal[y * width + x] = sum / diameter;
                int remove = x - radius;
                int add = x + radius + 1;
                if (remove >= 0) sum -= source[y * width + remove];
                if (add < width) sum += source[y * width + add];
            }
        }
        for (int x = 0; x < width; x++) {
            int sum = 0;
            for (int y = -radius; y <= radius; y++) {
                if (y >= 0 && y < height) sum += horizontal[y * width + x];
            }
            for (int y = 0; y < height; y++) {
                output[y * width + x] = sum / diameter;
                int remove = y - radius;
                int add = y + radius + 1;
                if (remove >= 0) sum -= horizontal[remove * width + x];
                if (add < height) sum += horizontal[add * width + x];
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
