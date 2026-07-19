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

    default String[] getFontFamilies() {
        return new String[0];
    }

    default void prewarmBasicLatin() {
    }

    @Override
    void close();
}
