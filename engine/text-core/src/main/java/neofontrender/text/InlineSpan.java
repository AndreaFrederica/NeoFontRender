package neofontrender.text;

import java.util.Objects;

/** One U+FFFC object-replacement span in plain-text coordinates. */
public final class InlineSpan {
    private final int start;
    private final int end;
    private final InlineContent content;

    public InlineSpan(int start, int end, InlineContent content) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid inline span");
        this.start = start;
        this.end = end;
        this.content = Objects.requireNonNull(content, "content");
    }

    public int start() { return start; }
    public int end() { return end; }
    public InlineContent content() { return content; }

    @Override public String toString() {
        return "InlineSpan{" + start + ".." + end + ", " + content.kind() + ':'
                + content.key() + ", " + content.layout()
                + (content.attributes().isEmpty() ? "" : ", " + content.attributes())
                + (content.resolved() ? ", resolved" : ", unresolved") + '}';
    }
}
