package neofontrender.text;

import java.util.Objects;

/** Half-open plain-text range with a fully reduced style snapshot. */
public final class StyledSpan {
    private final int start;
    private final int end;
    private final TextStyle style;

    public StyledSpan(int start, int end, TextStyle style) {
        if (start < 0 || end < start) throw new IllegalArgumentException("Invalid style range");
        this.start = start;
        this.end = end;
        this.style = Objects.requireNonNull(style, "style");
    }

    public int start() { return start; }
    public int end() { return end; }
    public TextStyle style() { return style; }

    @Override public String toString() {
        return '[' + Integer.toString(start) + ',' + end + ") " + style;
    }
}
