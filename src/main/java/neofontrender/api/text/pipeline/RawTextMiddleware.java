package neofontrender.api.text.pipeline;

import javax.annotation.Nullable;

/**
 * Whole-string compatibility stage. Returning {@code null} passes the current representation to
 * the next provider. Implementations must preserve its raw-boundary mapping when transforming it.
 */
public interface RawTextMiddleware extends TextMiddleware {
    TextTrigger trigger();

    @Nullable
    ProcessedText process(ProcessedText input);
}
