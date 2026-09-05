package neofontrender.core.font.backend;

import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.text.StructuredText;

/**
 * Minimal abstraction for shaped-text backends.
 *
 * <p>Callers depend on this surface so shaped-text engines can reuse the same FontRenderer
 * integration without leaking backend-specific types.
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
