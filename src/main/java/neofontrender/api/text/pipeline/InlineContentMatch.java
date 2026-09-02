package neofontrender.api.text.pipeline;

import java.util.Objects;

/** A middleware result covering the raw source range {@code [start, end)}. */
public final class InlineContentMatch {
    private final int start;
    private final int end;
    private final InlineContent content;

    public InlineContentMatch(int start, int end, InlineContent content) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid content range");
        this.start = start;
        this.end = end;
        this.content = Objects.requireNonNull(content, "content");
    }

    public int start() { return start; }
    public int end() { return end; }
    public InlineContent content() { return content; }
}
