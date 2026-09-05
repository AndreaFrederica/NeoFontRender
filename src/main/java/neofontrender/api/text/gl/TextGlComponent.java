package neofontrender.api.text.gl;

import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.text.StructuredEffectSpan;

import java.util.List;

/**
 * Render-thread GL component mounted below text post-processing. Implementations own framebuffer,
 * shader and GL-state lifetimes; callers only submit a draw-ready text result and effect spans.
 */
public interface TextGlComponent extends AutoCloseable {
    String id();

    default int priority() { return 0; }

    boolean isAvailable();

    /** Short render-thread status used by diagnostics screens. */
    default String status() {
        return isAvailable() ? "available" : "unavailable";
    }

    TextRenderResult draw(TextRenderResult source, float x, float y, float alpha,
                          List<StructuredEffectSpan> effects);

    @Override
    default void close() {}
}
