package neofontrender.text.pipeline;

import neofontrender.text.StructuredText;

/** A game-independent post-syntax structural transform. */
public interface StructuredTextMiddleware {
    String id();
    default int priority() { return 0; }
    default boolean isEnabled() { return true; }

    /** Return the input instance when no transformation matched. */
    StructuredText process(StructuredText input);
}
