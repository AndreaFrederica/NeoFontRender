package neofontrender.core.font.cosmic;

import net.minecraft.client.resources.IResourceManager;
import neofontrender.NeoFontRender;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Per-glyph cosmic rasterizer for the vanilla-layout compatibility provider (ABI 16/17).
 *
 * <p>The compat provider needs glyphs one at a time: monochrome ones go into its
 * {@code GL_ALPHA} pages, color ones into its RGBA cache. This bridge owns a dedicated engine
 * created from the same font definition as the modern renderer ({@link
 * CosmicTextRenderer#createConfiguredEngine}) and calls the per-glyph / per-page entries, which
 * take code points instead of strings.</p>
 */
public final class CosmicColorGlyphs {

    /** Decoded single-glyph raster. {@code rgba} and {@code mask} are mutually exclusive. */
    public record Glyph(int[] rgba, byte[] mask, int width, int height,
                        float offsetX, float offsetY, float advance,
                        float baseline, float scale, boolean color) {
    }

    private static final int HEADER_MAGIC = 0x434F534D;
    private static boolean failed;
    private static long engine;

    private CosmicColorGlyphs() {
    }

    /** Native ABI the bundled library must report. */
    public static int abiVersion() {
        return CosmicNative.ABI_VERSION;
    }

    public static synchronized boolean isAvailable(IResourceManager resourceManager) {
        if (engine != 0L) return true;
        if (failed) return false;
        try {
            if (!CosmicRuntimeSupport.ensureLoaded().isSupported()) {
                failed = true;
                return false;
            }
            engine = CosmicTextRenderer.createConfiguredEngine(resourceManager);
            if (engine == 0L) {
                failed = true;
                NeoFontRender.LOGGER.warn("Compat: cosmic glyph engine could not be created");
                return false;
            }
            return true;
        } catch (Throwable error) {
            failed = true;
            NeoFontRender.LOGGER.warn("Compat: cosmic glyph provider unavailable", error);
            return false;
        }
    }

    /**
     * One shaping pass for a whole page. cosmic resolves faces and fallback for the 256 code
     * points in a single run, then returns each shaped glyph separately, keyed by code point.
     */
    public static java.util.Map<Integer, Glyph> renderPage(int[] codePoints,
                                                           float fontSize, float rasterScale) {
        java.util.Map<Integer, Glyph> result = new java.util.HashMap<>(codePoints.length * 2);
        if (engine == 0L || codePoints.length == 0) return result;
        byte[] encoded;
        try {
            encoded = CosmicNative.renderGlyphPage(engine, codePoints, 0xFFFFFFFF, 0,
                    fontSize, rasterScale);
        } catch (Throwable error) {
            NeoFontRender.LOGGER.warn("Compat: cosmic glyph page failed", error);
            return result;
        }
        if (encoded == null || encoded.length < 4) return result;
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        int count = data.getInt();
        for (int entry = 0; entry < count; entry++) {
            if (data.remaining() < 8) break;
            int index = data.getInt();
            int length = data.getInt();
            if (index < 0 || index >= codePoints.length || length < 0 || data.remaining() < length) {
                break;
            }
            byte[] blob = new byte[length];
            data.get(blob);
            Glyph glyph = decode(blob);
            if (glyph != null) result.put(codePoints[index], glyph);
        }
        return result;
    }

    /**
     * Rasterizes a whole page with one JNI call but one shaping session per code point. Kept for
     * callers that cannot use the single-pass page entry.
     */
    public static java.util.Map<Integer, Glyph> renderBatch(int[] codePoints,
                                                            float fontSize, float rasterScale) {
        java.util.Map<Integer, Glyph> result = new java.util.HashMap<>(codePoints.length * 2);
        if (engine == 0L || codePoints.length == 0) return result;
        byte[] encoded;
        try {
            encoded = CosmicNative.renderGlyphBatch(engine, codePoints, 0xFFFFFFFF, 0,
                    fontSize, rasterScale);
        } catch (Throwable error) {
            NeoFontRender.LOGGER.warn("Compat: cosmic glyph batch failed", error);
            return result;
        }
        if (encoded == null || encoded.length < 4) return result;
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        int count = data.getInt();
        for (int index = 0; index < count && index < codePoints.length; index++) {
            if (data.remaining() < 4) break;
            int length = data.getInt();
            if (length < 0 || data.remaining() < length) break;
            byte[] blob = new byte[length];
            data.get(blob);
            Glyph glyph = decode(blob);
            if (glyph != null) result.put(codePoints[index], glyph);
        }
        return result;
    }

    /** Rasterizes one code point; returns {@code null} when cosmic produced nothing usable. */
    public static Glyph render(int codePoint, float fontSize, float rasterScale) {
        if (codePoint < 0 || codePoint > 0x10FFFF
                || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            return null;
        }
        if (engine == 0L) return null;
        byte[] encoded;
        try {
            encoded = CosmicNative.renderGlyphSized(engine, codePoint, 0xFFFFFFFF, 0,
                    fontSize, rasterScale);
        } catch (Throwable error) {
            NeoFontRender.LOGGER.warn("Compat: cosmic glyph raster failed for U+{}",
                    Integer.toHexString(codePoint), error);
            return null;
        }
        return decode(encoded);
    }

    private static Glyph decode(byte[] encoded) {
        if (encoded == null || encoded.length < 36) return null;
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != HEADER_MAGIC) return null;
        int width = data.getInt();
        int height = data.getInt();
        int offsetX = data.getInt();
        int offsetY = data.getInt();
        float advance = data.getFloat();
        float baseline = data.getFloat();
        float scale = data.getFloat();
        int modelFlags = data.getInt();
        boolean color = (modelFlags
                & (CosmicNative.RASTER_MODEL_FLAT_COLOR | CosmicNative.RASTER_MODEL_GRADIENT_COLOR)) != 0;
        int channels = (modelFlags & CosmicNative.RASTER_MODEL_PAYLOAD_ALPHA8) != 0 ? 1
                : (modelFlags & CosmicNative.RASTER_MODEL_PAYLOAD_RGB8) != 0 ? 3 : 4;
        long count = (long) width * height;
        if (width < 0 || height < 0 || count > Integer.MAX_VALUE
                || !Float.isFinite(scale) || scale <= 0.0F
                || data.remaining() != count * channels) {
            return null;
        }
        int[] rgba = null;
        byte[] mask = null;
        if (count == 0) {
            return new Glyph(null, null, 0, 0, offsetX, offsetY,
                    advance, baseline, scale, color);
        }
        if (channels == 4) {
            rgba = new int[(int) count];
            data.asIntBuffer().get(rgba, 0, rgba.length);
        } else if (channels == 1) {
            mask = new byte[(int) count];
            data.get(mask);
        } else {
            // RGB payloads are MSDF fields, not glyph coverage.
            return null;
        }
        return new Glyph(rgba, mask, width, height, offsetX, offsetY,
                advance, baseline, scale, color);
    }

    public static synchronized void close() {
        if (engine != 0L) {
            try {
                CosmicNative.destroyEngine(engine);
            } catch (Throwable ignored) {
                // The native library may already be gone during shutdown.
            }
            engine = 0L;
        }
    }
}
