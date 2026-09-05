package neofontrender.api.text;

import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.core.font.postprocess.TextPostProcessPipeline;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.text.StructuredText;

/**
 * Public engine-independent API for clear, native logical-size text.
 *
 * <p>Third-party mods call this class without checking whether NFR currently uses Cosmic,
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
        TextPostProcessPipeline.initialize();
        return backend != null && backend.isReady()
                && neofontrender.core.config.NeofontrenderConfig.modernShadowEnabled()
                && !"none".equals(neofontrender.core.config.NeofontrenderConfig.shadowMode())
                && neofontrender.core.config.NeofontrenderConfig.shadowOpacity() > 0.0F
                && neofontrender.api.text.postprocess.TextPostProcessApi.isEnabled(
                neofontrender.core.font.postprocess.ShadowPostProcessor.ID);
    }

    public static boolean canRenderModernShadow(ModernText text) {
        if (text == null || text.isEmpty()) return false;
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady() || !isModernShadowAvailable()) {
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
        return layoutStructured(StructuredTextRuntime.parse(text), fontSize, argb, shadow,
                ShadowRenderSpec.fromConfig());
    }

    /**
     * Shapes and rasterizes independently colored formatted runs as one draw-ready layout.
     *
     * <p>Every configured renderer is supported: Cosmic is used directly, while SFR and vanilla
     * selections use the modern AWT adapter chosen by {@link FontManager}.</p>
     */
    public static ModernTextLayout layoutFormatted(
            ModernText text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        return layoutStructured(StructuredTextRuntime.parse(text), fontSize, argb, shadow,
                ShadowRenderSpec.fromConfig());
    }

    private static ModernTextLayout layoutStructured(StructuredText structured, float fontSize,
                                                     int argb, boolean shadow,
                                                     ShadowRenderSpec spec) {
        if (structured == null || structured.plainText().isEmpty()) return ModernTextLayout.EMPTY;
        FontRenderTuning.updateFromCurrentGlState(shadow);
        TextPostProcessPipeline.initialize();
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return ModernTextLayout.EMPTY;
        return TextPostProcessPipeline.renderStructured(backend, structured, argb,
                sanitizeSize(fontSize), shadow,
                spec == null ? ShadowRenderSpec.fromConfig() : spec);
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
     * <p>This is the compatibility facade for the modern post-process shadow stage. Call
     * {@link #canRenderModernShadow(ModernText)} when a legacy fallback is required for the
     * selected backend or for native color glyphs.</p>
     */
    public static ModernTextLayout layoutFormattedWithShadow(
            String text, float fontSize, int argb) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        return layoutStructured(StructuredTextRuntime.parse(text), fontSize, argb, true,
                ShadowRenderSpec.fromConfig());
    }

    public static ModernTextLayout layoutFormattedWithShadow(
            ModernText text, float fontSize, int argb) {
        return layoutFormattedWithShadow(text, fontSize, argb, ShadowRenderSpec.fromConfig());
    }

    /** Same modern-shadow path with draft or caller-owned settings that are not globally applied. */
    public static ModernTextLayout layoutFormattedWithShadow(
            String text, float fontSize, int argb, ShadowRenderSpec spec) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        return layoutStructured(StructuredTextRuntime.parse(text), fontSize, argb, true, spec);
    }

    public static ModernTextLayout layoutFormattedWithShadow(
            ModernText text, float fontSize, int argb, ShadowRenderSpec spec) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        return layoutStructured(StructuredTextRuntime.parse(text), fontSize, argb, true, spec);
    }

    /** Legacy shadow-color pass for runs that cannot use modern post-processing. */
    public static ModernTextLayout layoutFormattedLegacyShadow(
            ModernText text, float fontSize, int argb) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return ModernTextLayout.EMPTY;
        float logicalSize = sanitizeSize(fontSize);
        StructuredText structured = StructuredTextRuntime.parse(text);
        return new ModernTextLayout(backend.renderStructuredAtSize(
                structured, argb, true, logicalSize), alpha(argb));
    }

    /** Produces only the legacy shadow-colored raster using caller-owned preview settings. */
    public static ModernTextLayout layoutFormattedLegacyShadow(
            String text, float fontSize, int argb, ShadowRenderSpec spec) {
        if (text == null || text.isEmpty()) return ModernTextLayout.EMPTY;
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return ModernTextLayout.EMPTY;
        float logicalSize = sanitizeSize(fontSize);
        StructuredText structured = StructuredTextRuntime.parse(text);
        return new ModernTextLayout(backend.renderStructuredShadowSourceAtSize(
                structured, argb, logicalSize,
                spec == null ? ShadowRenderSpec.fromConfig() : spec), alpha(argb));
    }

    public static float measureFormatted(
            String text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return 0.0F;
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return 0.0F;
        FontRenderTuning.updateFromCurrentGlState(shadow);
        return backend.measureStructuredAtSize(StructuredTextRuntime.parse(text), argb,
                shadow, sanitizeSize(fontSize));
    }

    public static float measureFormatted(
            ModernText text, float fontSize, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return 0.0F;
        FontRenderTuning.updateFromCurrentGlState(shadow);
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) return 0.0F;
        return backend.measureStructuredAtSize(StructuredTextRuntime.parse(text), argb,
                shadow, sanitizeSize(fontSize));
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

}
