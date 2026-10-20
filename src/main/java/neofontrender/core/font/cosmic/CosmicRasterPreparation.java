package neofontrender.core.font.cosmic;

import neofontrender.core.font.support.ModernShadowRasterizer;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.core.font.support.FontPixelUtils;
import neofontrender.api.text.TextVisualBounds;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** CPU-only raster decoding, shadow composition and distance-field generation. No config or GL reads. */
final class CosmicRasterPreparation {
    record Options(float sizeRatio, float baselineOffset, boolean sdf, int distanceRange,
                   boolean modernShadow, int shadowArgb, ShadowRenderSpec shadow) {}
    record Layer(int[] rgba, byte[] alpha, byte[] sdf, byte[] msdf,
                 int width, int height, float x, float y, TextVisualBounds bounds, int tintArgb) {
        long bytes() { return (rgba == null ? 0L : rgba.length * 4L)
                + (alpha == null ? 0L : alpha.length)
                + (sdf == null ? 0L : sdf.length) + (msdf == null ? 0L : msdf.length); }
    }
    /**
     * baselineAnchor is the y translation applied before the raster's pixel offset. Keeping it
     * alongside the pixels lets the async per-glyph fallback use the same baseline as the full
     * shaped result when individual glyphs have different raster bounds.
     */
    record Raster(float advance, float scale, float baselineAnchor, Layer foreground, Layer shadow) {
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
        int payloadChannels = (modelFlags & CosmicNative.RASTER_MODEL_PAYLOAD_ALPHA8) != 0 ? 1
                : (modelFlags & CosmicNative.RASTER_MODEL_PAYLOAD_RGB8) != 0 ? 3 : 4;
        if (width < 0 || height < 0 || count > Integer.MAX_VALUE
                || data.remaining() != count * payloadChannels
                || !Float.isFinite(scale) || scale <= 0) {
            throw new IllegalStateException("cosmic-text returned invalid dimensions " + width + "x" + height);
        }
        float baselineAnchor = options.baselineOffset() - baseline / scale;
        if (width == 0 || height == 0) return new Raster(advance, scale, baselineAnchor, null, null);
        int[] pixels = payloadChannels == 4 ? new int[(int) count] : null;
        if (pixels != null) CosmicRasterPixels.copyBaseLayer(data, (int) count, pixels);
        float x = offsetX / scale, y = offsetY / scale + options.baselineOffset - baseline;
        boolean outlineSdf = (modelFlags & CosmicNative.RASTER_MODEL_OUTLINE_SDF) != 0;
        boolean outlineMsdf = (modelFlags & CosmicNative.RASTER_MODEL_OUTLINE_MSDF) != 0;
        boolean sdf = options.sdf && (modelFlags & CosmicNative.RASTER_MODEL_GRADIENT_COLOR) == 0
                && (modelFlags & (CosmicNative.RASTER_MODEL_MASK | CosmicNative.RASTER_MODEL_FLAT_COLOR)) != 0;
        byte[] nativeSdf = outlineSdf && sdf
                ? payloadChannels == 1 ? readPayload(data, (int) count) : extractNativeSdf(pixels)
                : null;
        byte[] nativeMsdf = outlineMsdf && sdf
                ? payloadChannels == 3 ? readPayload(data, (int) count * 3) : extractNativeMsdf(pixels)
                : null;
        // Compact outline payloads do not carry RGBA coverage. Modern shadows still need a
        // binary source mask, which can be recovered from the signed field without restoring a
        // four-byte-per-pixel native payload.
        if (pixels == null && options.modernShadow()) {
            pixels = new int[(int) count];
            for (int index = 0; index < pixels.length; index++) {
                int coverage;
                if (nativeSdf != null) {
                    coverage = (nativeSdf[index] & 0xFF) >= 128 ? 0xFF : 0;
                } else if (nativeMsdf != null) {
                    int offset = index * 3;
                    int median = Math.max(Math.min(nativeMsdf[offset] & 0xFF, nativeMsdf[offset + 1] & 0xFF),
                            Math.min(Math.max(nativeMsdf[offset] & 0xFF, nativeMsdf[offset + 1] & 0xFF), nativeMsdf[offset + 2] & 0xFF));
                    coverage = median >= 128 ? 0xFF : 0;
                } else {
                    coverage = 0;
                }
                pixels[index] = coverage << 24;
            }
        }
        Layer shadow = null;
        if (options.modernShadow) {
            ShadowRenderSpec spec = options.shadow;
            if (pixels == null) throw new IllegalStateException("compact field payload cannot build shadow mask");
            ModernShadowRasterizer.Result result = sdf
                    ? ModernShadowRasterizer.shadow(pixels, width, height, scale,
                        spec.offsetX * options.sizeRatio, spec.offsetY * options.sizeRatio,
                        spec.blurRadius * options.sizeRatio, options.shadowArgb, spec.opacity, false)
                    : ModernShadowRasterizer.compose(pixels, width, height, scale,
                        spec.offsetX * options.sizeRatio, spec.offsetY * options.sizeRatio,
                        spec.blurRadius * options.sizeRatio, options.shadowArgb, spec.opacity, false);
            float shadowX = x - result.originX / scale, shadowY = y - result.originY / scale;
            byte[] shadowAlpha = sdf ? alphaPlane(result.pixels) : null;
            Layer layer = new Layer(sdf ? null : result.pixels, shadowAlpha, null, null,
                    result.width, result.height, shadowX, shadowY,
                    FontPixelUtils.visibleBounds(result.pixels, result.width, result.height,
                            shadowX, shadowY, shadowX + result.width / scale, shadowY + result.height / scale),
                    options.shadowArgb());
            if (!sdf) return new Raster(advance, scale, baselineAnchor, layer, null);
            shadow = layer;
        }
        byte[] compactField = nativeMsdf != null ? nativeMsdf : nativeSdf;
        TextVisualBounds foregroundBounds = pixels != null
                ? FontPixelUtils.visibleBounds(pixels, width, height, x, y, x + width / scale, y + height / scale)
                : FontPixelUtils.visibleBounds(compactField, width, height, x, y,
                        x + width / scale, y + height / scale, nativeMsdf == null ? 128 : 1);
        Layer foreground = new Layer(sdf ? null : pixels, null,
                sdf && nativeMsdf == null ? (nativeSdf != null ? nativeSdf
                        : CosmicSdfGenerator.generate(pixels, width, height, options.distanceRange)) : null,
                sdf ? nativeMsdf : null,
                width, height, x, y,
                foregroundBounds, 0);
        return new Raster(advance, scale, baselineAnchor, foreground, shadow);
    }

    private static byte[] extractNativeSdf(int[] pixels) {
        byte[] sdf = new byte[pixels.length];
        for (int index = 0; index < pixels.length; index++) {
            sdf[index] = (byte) (pixels[index] >>> 24);
        }
        return sdf;
    }

    private static byte[] alphaPlane(int[] pixels) {
        byte[] alpha = new byte[pixels.length];
        for (int index = 0; index < pixels.length; index++) alpha[index] = (byte) (pixels[index] >>> 24);
        return alpha;
    }

    private static byte[] readPayload(ByteBuffer data, int length) {
        byte[] result = new byte[length];
        data.get(result);
        return result;
    }

    private static byte[] extractNativeMsdf(int[] pixels) {
        byte[] msdf = new byte[pixels.length * 3];
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            int target = index * 3;
            msdf[target] = (byte) pixel;
            msdf[target + 1] = (byte) (pixel >>> 8);
            msdf[target + 2] = (byte) (pixel >>> 16);
        }
        return msdf;
    }
}
