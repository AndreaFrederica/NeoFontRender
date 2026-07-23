package neofontrender.core.font.backend;

/**
 * Minimal abstraction for shaped-text backends.
 *
 * <p>Current implementation is Skija-backed, but callers should depend on this
 * surface so future engines can reuse the same FontRenderer integration.
 */
public interface TextRenderBackend extends AutoCloseable {

    boolean isReady();

    float measure(String text, boolean bold, boolean italic);

    float measureFormatted(String text, int baseArgb, boolean shadow);

    TextRenderResult render(String text, int argb, boolean bold, boolean italic);

    default TextRenderResult renderSegment(String text, int argb, boolean bold, boolean italic) {
        return render(text, argb, bold, italic);
    }

    TextRenderResult renderFormatted(String text, int baseArgb, boolean shadow);

    /**
     * Whether this backend can shape and rasterize a caller-selected logical font size instead of
     * enlarging the normal UI-size texture with a model-view transform.
     */
    default boolean supportsNativeFontSize() {
        return false;
    }

    /**
     * Renders at the requested logical font size. Callers must check
     * {@link #supportsNativeFontSize()} before using this method.
     */
    default TextRenderResult renderFormattedAtSize(String text, int baseArgb, boolean shadow,
                                                   float fontSize) {
        return renderFormatted(text, baseArgb, shadow);
    }

    default float measureFormattedAtSize(String text, int baseArgb, boolean shadow,
                                         float fontSize) {
        return renderFormattedAtSize(text, baseArgb, shadow, fontSize).advance();
    }

    default boolean supportsModernShadow() {
        return false;
    }

    default TextRenderResult renderFormattedWithShadow(String text, int baseArgb) {
        return renderFormatted(text, baseArgb, false);
    }

    /**
     * Produces a complete foreground plus modern shadow at a caller-selected logical size.
     * Backends advertising {@link #supportsModernShadow()} should override this when they also
     * advertise native font-size support.
     */
    default TextRenderResult renderFormattedWithShadowAtSize(
            String text, int baseArgb, float fontSize) {
        return renderFormattedWithShadow(text, baseArgb);
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
