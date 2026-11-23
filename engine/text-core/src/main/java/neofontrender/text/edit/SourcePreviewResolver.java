package neofontrender.text.edit;

/** Chooses whether an inline source span is exposed while the editor caret is inside it. */
@FunctionalInterface
public interface SourcePreviewResolver {
    SourcePreviewMode mode(SourceInlineSpan span, SourceEditState state);

    SourcePreviewResolver CARET_REVEAL = (span, state) ->
            span.containsCaret(state) ? SourcePreviewMode.RAW : SourcePreviewMode.PREVIEW;
}
