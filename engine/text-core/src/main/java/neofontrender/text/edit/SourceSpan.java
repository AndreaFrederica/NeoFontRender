package neofontrender.text.edit;

import neofontrender.text.InlineContent;

/** Source range recognized by the structured-text pipeline. */
public final class SourceSpan {
    private final int start;
    private final int end;
    private final String kind;
    private final InlineContent preview;

    public SourceSpan(int start, int end, String kind, InlineContent preview) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid source span");
        this.start = start;
        this.end = end;
        this.kind = kind == null ? "syntax" : kind;
        this.preview = preview;
    }

    public int start() { return start; }
    public int end() { return end; }
    public String kind() { return kind; }
    public InlineContent preview() { return preview; }
    public boolean contains(SourceEditState state) {
        return state != null && start <= state.cursor() && state.cursor() <= end;
    }
}
