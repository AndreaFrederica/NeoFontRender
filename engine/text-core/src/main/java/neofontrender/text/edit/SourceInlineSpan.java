package neofontrender.text.edit;

import neofontrender.text.InlineContent;

import java.util.Objects;

/** A previewable source range. Coordinates refer to the editor's original source string. */
public final class SourceInlineSpan {
    private final int start;
    private final int end;
    private final InlineContent preview;

    public SourceInlineSpan(int start, int end, InlineContent preview) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid source span");
        this.start = start;
        this.end = end;
        this.preview = Objects.requireNonNull(preview, "preview");
    }

    public int start() { return start; }
    public int end() { return end; }
    public InlineContent preview() { return preview; }
    public boolean containsCaret(SourceEditState state) {
        return state != null && state.contains(start, end);
    }
}
