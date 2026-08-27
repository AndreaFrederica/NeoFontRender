package neofontrender.core.font.support;

import neofontrender.NeoFontRender;
import neofontrender.build.BuildFeatures;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Temporary, opt-in diagnostics for the TC6 ResearchToast font path.
 *
 * <p>The sampler deliberately keys off the resolved research-complete text instead of loading
 * Thaumcraft classes. That keeps the core portable while still allowing the problematic toast to
 * be correlated across the FontRenderer and Cosmic upload/draw stages.</p>
 */
public final class FontRenderDiagnostics {
    private static final long SAMPLE_INTERVAL_NANOS = 25_000_000L;
    private static volatile long lastFontSampleNanos;
    private static volatile long lastRasterSampleNanos;
    private static volatile long lastDrawSampleNanos;
    private static volatile long lastPreparedDrawSampleNanos;

    private FontRenderDiagnostics() {
    }

    public static boolean matchesResearchComplete(String text) {
        return "Research Completed!".equals(text)
                || "Research Completed!".equals(text == null ? null : text.trim())
                || "研究完成!".equals(text)
                || "研究完成！".equals(text);
    }

    public static boolean shouldSample(String text) {
        return shouldSample(text, 0);
    }

    private static boolean shouldSample(String text, int channel) {
        if (!BuildFeatures.RENDER_STATS || !NeofontrenderConfig.debugRenderStats()
                || !matchesResearchComplete(text)) {
            return false;
        }
        long now = System.nanoTime();
        long previous;
        if (channel == 1) previous = lastRasterSampleNanos;
        else if (channel == 2) previous = lastDrawSampleNanos;
        else if (channel == 3) previous = lastPreparedDrawSampleNanos;
        else previous = lastFontSampleNanos;
        if (now - previous < SAMPLE_INTERVAL_NANOS) {
            return false;
        }
        if (channel == 1) lastRasterSampleNanos = now;
        else if (channel == 2) lastDrawSampleNanos = now;
        else if (channel == 3) lastPreparedDrawSampleNanos = now;
        else lastFontSampleNanos = now;
        return true;
    }

    public static void logFontEntry(String phase, String text, int color, boolean shadow,
                                    FontRenderTuning.DrawContext context) {
        if (!shouldSample(text)) return;
        NeoFontRender.LOGGER.info(
                "[NFR-TC-SAMPLE] {} text='{}' color=0x{} shadow={} engine={} advanced={} context={} {}",
                phase, text, Integer.toHexString(color), shadow,
                NeofontrenderConfig.renderingEngine(), NeofontrenderConfig.advancedStringMode(),
                contextSummary(context), glSummary());
    }

    public static void logCosmicRaster(String phase, String text, int argb, int flags,
                                       float fontSize, float rasterScale, boolean cacheHit,
                                       int width, int height, float advance) {
        if (!shouldSample(text, 1)) return;
        NeoFontRender.LOGGER.info(
                "[NFR-TC-SAMPLE] {} text='{}' argb=0x{} flags={} fontSize={} rasterScale={} "
                        + "cache={} raster={}x{} advance={}",
                phase, text, Integer.toHexString(argb), flags, fontSize, rasterScale,
                cacheHit ? "hit" : "miss", width, height, advance);
    }

    public static void logCosmicDraw(String phase, String text,
                                     float x, float y, float width, float height,
                                     float offsetX, float offsetY, float scale) {
        int channel = "cosmic.draw.prepared".equals(phase) ? 3 : 2;
        if (!shouldSample(text, channel)) return;
        NeoFontRender.LOGGER.info(
                "[NFR-TC-SAMPLE] {} text='{}' xy=({}, {}) quad={}x{} offset=({}, {}) "
                        + "scale={} {}",
                phase, text, x, y, width, height, offsetX, offsetY, scale, glSummary());
    }

    private static String contextSummary(FontRenderTuning.DrawContext context) {
        if (context == null) return "context=null";
        return String.format(java.util.Locale.ROOT,
                "context=px%.3f/round%.3f ortho=%s rot=%s frac=%s shadow=%s",
                context.pixelScale(), context.roundedPixelScale(), context.orthographic(),
                context.rotation(), context.fractionalCoordinate(), context.shadow());
    }

    private static String glSummary() {
        try {
            return String.format(java.util.Locale.ROOT,
                    "gl=blend:%s(%d,%d/%d,%d eq=%d/%d) alpha:%s depth:%s(mask=%s func=%d) "
                            + "fog:%s tex:%s(bind=%d env=%d) active=%d client=%d program=%d "
                            + "units=%s light:%s rescale:%s material:%s "
                            + "color=%s",
                    enabled(GL11.GL_BLEND), GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB), GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA), GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),
                    GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA), enabled(GL11.GL_ALPHA_TEST),
                    enabled(GL11.GL_DEPTH_TEST), depthWriteMask(),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC), enabled(GL11.GL_FOG),
                    enabled(GL11.GL_TEXTURE_2D), GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),
                    textureEnvMode(), GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),
                    GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE),
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), textureUnitsSummary(),
                    enabled(GL11.GL_LIGHTING), enabled(GL12.GL_RESCALE_NORMAL),
                    enabled(GL11.GL_COLOR_MATERIAL), currentColor());
        } catch (RuntimeException | LinkageError error) {
            return "gl=unavailable(" + error.getClass().getSimpleName() + ")";
        }
    }

    private static boolean enabled(int capability) {
        return GL11.glIsEnabled(capability);
    }

    private static int textureEnvMode() {
        IntBuffer value = BufferUtils.createIntBuffer(1);
        GL11.glGetTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, value);
        return value.get(0);
    }

    private static String textureUnitsSummary() {
        int originalUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            return textureUnitSummary(GL13.GL_TEXTURE0) + ","
                    + textureUnitSummary(GL13.GL_TEXTURE1);
        } finally {
            GL13.glActiveTexture(originalUnit);
        }
    }

    private static String textureUnitSummary(int unit) {
        GL13.glActiveTexture(unit);
        return String.format(java.util.Locale.ROOT, "u%d:%s/%d/%d",
                unit - GL13.GL_TEXTURE0, enabled(GL11.GL_TEXTURE_2D),
                GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D), textureEnvMode());
    }

    private static boolean depthWriteMask() {
        java.nio.ByteBuffer value = BufferUtils.createByteBuffer(1);
        GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK, value);
        return value.get(0) != 0;
    }

    private static String currentColor() {
        FloatBuffer value = BufferUtils.createFloatBuffer(4);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, value);
        return String.format(java.util.Locale.ROOT, "(%.3f,%.3f,%.3f,%.3f)",
                value.get(0), value.get(1), value.get(2), value.get(3));
    }
}
