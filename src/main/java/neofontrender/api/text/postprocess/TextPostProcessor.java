package neofontrender.api.text.postprocess;

import neofontrender.api.text.ModernTextLayout;

import javax.annotation.Nullable;

/**
 * A render-time processor for modern, draw-ready text.
 *
 * <p>Unlike raw text middleware, a post-processor runs after shaping and may wrap the result with
 * additional visual layers such as shadows, outlines, or shader effects.</p>
 */
public interface TextPostProcessor {
    String id();

    default int priority() { return 0; }

    default boolean isEnabled() { return true; }

    /** Returns whether this processor applies to the current draw request. */
    default boolean supports(TextPostProcessContext context) {
        return true;
    }

    /**
     * Wraps or replaces the current draw-ready result. Returning {@code null} leaves the current
     * result unchanged so a processor can decline a request after inspecting its context.
     */
    @Nullable
    ModernTextLayout process(TextPostProcessContext context, ModernTextLayout current);
}
