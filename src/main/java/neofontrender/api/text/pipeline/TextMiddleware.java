package neofontrender.api.text.pipeline;

/**
 * Common identity and lifecycle contract for extensions participating in NFR's text pipeline.
 * Implement one of the stage-specific subinterfaces and register it through {@link TextPipelineApi}.
 */
public interface TextMiddleware {
    String id();

    default int priority() {
        return 0;
    }

    default boolean isEnabled() {
        return true;
    }
}
