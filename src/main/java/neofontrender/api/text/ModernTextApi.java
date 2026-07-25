package neofontrender.api.text;

import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.CompositeTextRenderResult;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.preprocess.TextPreprocessingPipeline;
import neofontrender.core.font.support.FontRenderTuning;

import java.util.ArrayList;
import java.util.List;

/**
 * Public engine-independent API for clear, native logical-size text.
 *
 * <p>Third-party mods call this class without checking whether NFR currently uses Cosmic, Skia,
 * SFR/AWT, or vanilla. The main mod selects the implementation and keeps size-specific raster
 * caches. All methods that create or draw a layout must run on Minecraft's client render thread.</p>
 */
public final class ModernTextApi {
    private ModernTextApi() {
    }

    public static boolean isAvailable() {
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        return backend != null && backend.isReady();
    }

    public static boolean isModernShadowAvailable() {
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        return backend != null && backend.isReady() && backend.supportsModernShadow();
    }

    public static boolean canRenderModernShadow(ModernText text) {
        if (text == null || text.isEmpty()) return false;
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady() || !backend.supportsModernShadow()) {
            return false;
        }
        for (ModernText.Run run : text.runs()) {
            if (!backend.shouldRenderShadow(run.text())) return false;
        }
        return true;
    }

    /**
     * Shapes and rasterizes Minecraft-formatted text at a true logical font size.
     *
     * @param text text containing optional section-sign formatting codes
     * @param fontSize requested logical size in GUI units
     * @param argb base ARGB color
     * @param shadow whether to use Minecraft's shadow-pass colors
     */
    public static ModernTextLayout layoutFormatted(
            String text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        return layoutFormatted(
                TextPreprocessingPipeline.process(text).modernText(),
                fontSize, argb, shadow);
    }

    /**
     * Shapes and rasterizes independently colored formatted runs as one draw-ready layout.
     *
     * <p>Every configured renderer is supported: Cosmic and Skia are used directly, while SFR
     * and vanilla selections use the modern AWT adapter chosen by {@link FontManager}.</p>
     */
    public static ModernTextLayout layoutFormatted(
            ModernText text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        FontRenderTuning.updateFromCurrentGlState(shadow);
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return ModernTextLayout.EMPTY;
        float logicalSize = sanitizeSize(fontSize);
        List<TextRenderResult> results = new ArrayList<>(text.runs().size());
        for (ModernText.Run run : text.runs()) {
            int runArgb = run.hasColorOverride()
                    ? withRgb(argb, run.rgb()) : argb;
            results.add(backend.renderFormattedAtSize(
                    run.text(), runArgb, shadow, logicalSize));
        }
        TextRenderResult result = CompositeTextRenderResult.of(results);
        return new ModernTextLayout(result, alpha(argb));
    }

    public static ModernTextLayout layout(String text, float fontSize, int argb) {
        return layoutFormatted(text, fontSize, argb, false);
    }

    public static ModernTextLayout layout(ModernText text, float fontSize, int argb) {
        return layoutFormatted(text, fontSize, argb, false);
    }

    /**
     * Produces one layout containing both the foreground and the configured modern blurred shadow.
     *
     * <p>This is distinct from {@code layoutFormatted(..., shadow=true)}, which creates only a
     * vanilla shadow-color pass. Call {@link #canRenderModernShadow(ModernText)} when a fallback is
     * required for the selected backend or for native color glyphs.</p>
     */
    public static ModernTextLayout layoutFormattedWithShadow(
            String text, float fontSize, int argb) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        return layoutFormattedWithShadow(
                TextPreprocessingPipeline.process(text).modernText(),
                fontSize, argb);
    }

    public static ModernTextLayout layoutFormattedWithShadow(
            ModernText text, float fontSize, int argb) {
        if (!canRenderModernShadow(text)) return ModernTextLayout.EMPTY;
        FontRenderTuning.updateFromCurrentGlState(true);
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        float logicalSize = sanitizeSize(fontSize);
        List<TextRenderResult> results = new ArrayList<>(text.runs().size());
        for (ModernText.Run run : text.runs()) {
            int runArgb = run.hasColorOverride()
                    ? withRgb(argb, run.rgb()) : argb;
            results.add(backend.renderFormattedWithShadowAtSize(
                    run.text(), runArgb, logicalSize));
        }
        return new ModernTextLayout(
                CompositeTextRenderResult.of(results), alpha(argb));
    }

    public static float measureFormatted(
            String text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return 0.0F;
        return measureFormatted(
                TextPreprocessingPipeline.process(text).modernText(),
                fontSize, argb, shadow);
    }

    public static float measureFormatted(
            ModernText text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return 0.0F;
        FontRenderTuning.updateFromCurrentGlState(shadow);
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return 0.0F;
        float logicalSize = sanitizeSize(fontSize);
        float advance = 0.0F;
        for (ModernText.Run run : text.runs()) {
            int runArgb = run.hasColorOverride()
                    ? withRgb(argb, run.rgb()) : argb;
            advance += backend.measureFormattedAtSize(
                    run.text(), runArgb, shadow, logicalSize);
        }
        return advance;
    }

    public static float measure(String text, float fontSize) {
        return measureFormatted(text, fontSize, 0xFFFFFFFF, false);
    }

    public static float measure(ModernText text, float fontSize) {
        return measureFormatted(text, fontSize, 0xFFFFFFFF, false);
    }

    /**
     * Convenience draw call. It refreshes the framebuffer scale before selecting a raster bucket
     * and returns the logical advance.
     */
    public static float drawFormatted(String text, float x, float y, float fontSize,
                                      int argb, boolean shadow) {
        ModernTextLayout layout = layoutFormatted(text, fontSize, argb, shadow);
        layout.draw(x, y);
        return layout.advance();
    }

    public static float drawFormatted(ModernText text, float x, float y, float fontSize,
                                      int argb, boolean shadow) {
        ModernTextLayout layout = layoutFormatted(text, fontSize, argb, shadow);
        layout.draw(x, y);
        return layout.advance();
    }

    public static float draw(String text, float x, float y, float fontSize, int argb) {
        return drawFormatted(text, x, y, fontSize, argb, false);
    }

    public static float draw(ModernText text, float x, float y, float fontSize, int argb) {
        return drawFormatted(text, x, y, fontSize, argb, false);
    }

    public static float drawFormattedWithShadow(
            String text, float x, float y, float fontSize, int argb) {
        ModernTextLayout layout = layoutFormattedWithShadow(text, fontSize, argb);
        layout.draw(x, y);
        return layout.advance();
    }

    public static float drawFormattedWithShadow(
            ModernText text, float x, float y, float fontSize, int argb) {
        ModernTextLayout layout = layoutFormattedWithShadow(text, fontSize, argb);
        layout.draw(x, y);
        return layout.advance();
    }

    private static float sanitizeSize(float fontSize) {
        return Float.isFinite(fontSize) ? Math.max(1.0F, Math.min(256.0F, fontSize)) : 8.0F;
    }

    private static float alpha(int argb) {
        int value = argb >>> 24;
        return value == 0 ? 1.0F : value / 255.0F;
    }

    private static int withRgb(int argb, int rgb) {
        return (argb & 0xFF000000) | (rgb & 0xFFFFFF);
    }
}
