package neofontrender.api.text.pipeline;

import javax.annotation.Nullable;

/** Recognizes one atomic inline object beginning at an exact source index. */
public interface InlineContentMiddleware extends TextMiddleware {
    TextTrigger trigger();

    @Nullable
    InlineContentMatch match(CharSequence source, int sourceIndex);
}
