package neofontrender.core.font.cosmic;

import neofontrender.core.font.support.ModernShadowRasterizer;
import neofontrender.core.font.support.ShadowRenderSpec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** CPU-only raster decoding, shadow composition and distance-field generation. No config or GL reads. */
final class CosmicRasterPreparation {
    record Options(float sizeRatio, float baselineOffset, boolean sdf, int distanceRange,
                   boolean modernShadow, int shadowArgb, ShadowRenderSpec shadow) {}
    record Layer(int[] rgba, byte[] sdf, int width, int height, float x, float y) {
        long bytes() { return (rgba == null ? 0L : rgba.length * 4L) + (sdf == null ? 0L : sdf.length); }
    }
    record Raster(float advance, float scale, Layer foreground, Layer shadow) {
        long bytes() { return (foreground == null ? 0 : foreground.bytes()) + (shadow == null ? 0 : shadow.bytes()); }
    }

    static Raster prepare(byte[] encoded, Options options) {
        if (encoded == null || encoded.length < 36) throw new IllegalStateException("cosmic-text returned a truncated raster");
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != 0x434F534D) throw new IllegalStateException("cosmic-text returned an invalid raster header");
        int width = data.getInt(), height = data.getInt();
        int offsetX = data.getInt(), offsetY = data.getInt();
        float advance = data.getFloat(), baseline = data.getFloat(), scale = data.getFloat();
        int modelFlags = data.getInt();
        long count = (long) width * height;
        if (width < 0 || height < 0 || count > Integer.MAX_VALUE || data.remaining() != count * 4L
                || !Float.isFinite(scale) || scale <= 0) {
            throw new IllegalStateException("cosmic-text returned invalid dimensions " + width + "x" + height);
        }
        if (width == 0 || height == 0) return new Raster(advance, scale, null, null);
        int[] pixels = new int[(int) count];
        CosmicRasterPixels.copyBaseLayer(data, (int) count, pixels);
        float x = offsetX / scale, y = offsetY / scale + options.baselineOffset - baseline;
        boolean sdf = options.sdf && (modelFlags & CosmicNative.RASTER_MODEL_GRADIENT_COLOR) == 0
                && (modelFlags & (CosmicNative.RASTER_MODEL_MASK | CosmicNative.RASTER_MODEL_FLAT_COLOR)) != 0;
        Layer shadow = null;
        if (options.modernShadow) {
            ShadowRenderSpec spec = options.shadow;
            ModernShadowRasterizer.Result result = sdf
                    ? ModernShadowRasterizer.shadow(pixels, width, height, scale,
                        spec.offsetX * options.sizeRatio, spec.offsetY * options.sizeRatio,
                        spec.blurRadius * options.sizeRatio, options.shadowArgb, spec.opacity, false)
                    : ModernShadowRasterizer.compose(pixels, width, height, scale,
                        spec.offsetX * options.sizeRatio, spec.offsetY * options.sizeRatio,
                        spec.blurRadius * options.sizeRatio, options.shadowArgb, spec.opacity, false);
            Layer layer = new Layer(result.pixels, null, result.width, result.height,
                    x - result.originX / scale, y - result.originY / scale);
            if (!sdf) return new Raster(advance, scale, layer, null);
            shadow = layer;
        }
        Layer foreground = new Layer(sdf ? null : pixels,
                sdf ? CosmicSdfGenerator.generate(pixels, width, height, options.distanceRange) : null,
                width, height, x, y);
        return new Raster(advance, scale, foreground, shadow);
    }
}
