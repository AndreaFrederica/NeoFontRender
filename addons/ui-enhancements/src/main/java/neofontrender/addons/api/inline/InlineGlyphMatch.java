package neofontrender.addons.api.inline;

import java.util.Objects;

/** A provider result covering the raw source range {@code [start, end)}. */
public final class InlineGlyphMatch {
    private final int start;
    private final int end;
    private final InlineGlyph glyph;

    public InlineGlyphMatch(int start, int end, InlineGlyph glyph) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid glyph range");
        this.start = start;
        this.end = end;
        this.glyph = Objects.requireNonNull(glyph, "glyph");
    }

    public int start() { return start; }

    public int end() { return end; }

    public InlineGlyph glyph() { return glyph; }
}
