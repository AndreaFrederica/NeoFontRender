package neofontrender.core.font.backend;

import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.core.font.support.ShadowColorPolicy;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.text.StructuredText;

/**
 * Minimal abstraction for shaped-text backends.
 *
 * <p>Callers depend on this surface so shaped-text engines can reuse the same FontRenderer
 * integration without leaking backend-specific types.
 *
 * <p>Two call surfaces meet here. The 1.7.10 FontRenderer integration passes whole legacy
 * {@code §}-formatted strings through the {@code *Formatted*} family, which parses them with the
 * shared syntax engine and forwards the result to the structured entry points, so per-run colors
 * and styles behave exactly like the structured route. Structured callers hand in an already parsed
 * {@link StructuredText} and pay no parse cost.
 */
public interface TextRenderBackend extends AutoCloseable {

    boolean isReady();

    float measure(String text, boolean bold, boolean italic);

    TextRenderResult render(String text, int argb, boolean bold, boolean italic);

    default TextRenderResult renderSegment(String text, int argb, boolean bold, boolean italic) {
        return render(text, argb, bold, italic);
    }

    /** Updates the 32 legacy formatting colors used by complete-string rendering paths. */
    default void updateLegacyColorCodes(int[] colorCodes) {
    }

    /** Measures a legacy {@code §}-formatted string at the configured UI font size. */
    default float measureFormatted(String text, int baseArgb, boolean shadow) {
        return measureFormattedAtSize(text, baseArgb, shadow, NeofontrenderConfig.fontSize());
    }

    /** Measures a legacy {@code §}-formatted string at a caller-selected logical font size. */
    default float measureFormattedAtSize(String text, int baseArgb, boolean shadow, float fontSize) {
        return measureStructuredAtSize(StructuredTextRuntime.parse(text), baseArgb, shadow, fontSize);
    }

    /** Renders a legacy {@code §}-formatted string at the configured UI font size. */
    default TextRenderResult renderFormatted(String text, int baseArgb, boolean shadow) {
        return renderFormattedAtSize(text, baseArgb, shadow, NeofontrenderConfig.fontSize());
    }

    /** Renders a legacy {@code §}-formatted string at a caller-selected logical font size. */
    default TextRenderResult renderFormattedAtSize(String text, int baseArgb, boolean shadow,
                                                   float fontSize) {
        return renderStructuredAtSize(StructuredTextRuntime.parse(text), baseArgb, shadow, fontSize);
    }

    /**
     * Whether this backend can shape and rasterize a caller-selected logical font size instead of
     * enlarging the normal UI-size texture with a model-view transform.
     */
    default boolean supportsNativeFontSize() {
        return false;
    }

    /** Renders syntax-engine output without asking the backend to parse control codes again. */
    default TextRenderResult renderStructuredAtSize(
            StructuredText text, int baseArgb, boolean shadow, float fontSize) {
        return render(text == null ? "" : text.plainText(), baseArgb, false, false);
    }

    default float measureStructuredAtSize(
            StructuredText text, int baseArgb, boolean shadow, float fontSize) {
        return renderStructuredAtSize(text, baseArgb, shadow, fontSize).advance();
    }

    /**
     * Produces a shadow-only draw result for the modern post-process stage. The default keeps
     * compatibility with backends that only expose a legacy shadow-colored raster.
     */
    default TextRenderResult renderStructuredShadowSourceAtSize(
            StructuredText text, int baseArgb, float fontSize, ShadowRenderSpec spec) {
        return renderStructuredAtSize(text, baseArgb, true, fontSize);
    }

    /**
     * Optionally produces a complete foreground plus soft-shadow result using a backend-native
     * raster operation. Returning {@code null} asks the shadow post-processor to use its generic
     * sampled fallback. The post-processor remains the sole owner of shadow orchestration.
     */
    default TextRenderResult renderStructuredModernShadowAtSize(
            StructuredText text, int baseArgb, float fontSize, ShadowRenderSpec spec) {
        return null;
    }

    /**
     * Whether this backend can compose the complete foreground plus its soft shadow into one draw
     * result. Callers must check this before using
     * {@link #renderFormattedWithShadow(String, int)}.
     */
    default boolean supportsModernShadow() {
        return false;
    }

    /** Renders a legacy {@code §}-formatted string together with its configured soft shadow. */
    default TextRenderResult renderFormattedWithShadow(String text, int baseArgb) {
        return renderFormattedWithShadowAtSize(text, baseArgb, NeofontrenderConfig.fontSize());
    }

    /**
     * Produces one foreground plus shadow result at a caller-selected logical size. Backends that
     * override {@link #renderStructuredModernShadowAtSize} compose the shadow natively; every other
     * backend receives the sampled composition the structured post-processor also uses.
     */
    default TextRenderResult renderFormattedWithShadowAtSize(
            String text, int baseArgb, float requestedFontSize) {
        float fontSize = Math.max(1.0F, requestedFontSize);
        ShadowRenderSpec spec = ShadowRenderSpec.fromConfig();
        StructuredText structured = StructuredTextRuntime.parse(text);
        if (structured.plainText().isEmpty()) {
            return TextRenderResult.EMPTY;
        }
        TextRenderResult nativeComposite = renderStructuredModernShadowAtSize(
                structured, baseArgb, fontSize, spec);
        if (nativeComposite != null) {
            return nativeComposite;
        }
        TextRenderResult foreground = renderStructuredAtSize(structured, baseArgb, false, fontSize);
        if (spec.opacity <= 0.0F || !shouldRenderShadow(structured.plainText())) {
            return foreground;
        }
        TextRenderResult shadow = renderStructuredShadowSourceAtSize(
                structured, baseArgb, fontSize, spec);
        if (shadow == null || shadow == TextRenderResult.EMPTY) {
            return foreground;
        }
        float colorAlpha = ShadowColorPolicy.SOLID.equals(spec.colorMode)
                ? (spec.color >>> 24) / 255.0F : 1.0F;
        float geometryScale = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        return new SampledShadowTextRenderResult(shadow, foreground, spec.offsetX, spec.offsetY,
                spec.blurRadius, spec.opacity * colorAlpha, geometryScale);
    }

    /**
     * Color glyphs already carry their own paint and look like duplicate emoji when Minecraft
     * renders the usual offset shadow pass. Backends that can identify them opt out per run.
     */
    default boolean shouldRenderShadow(String text) {
        return true;
    }

    default String[] getFontFamilies() {
        return new String[0];
    }

    default void prewarmBasicLatin() {
    }

    @Override
    void close();
}
