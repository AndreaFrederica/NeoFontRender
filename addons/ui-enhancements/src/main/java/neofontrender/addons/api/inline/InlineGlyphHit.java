package neofontrender.addons.api.inline;

/** Hit-test result in local layout coordinates. */
public final class InlineGlyphHit {
    private final InlineGlyphMatch match;
    private final int x;
    private final int width;
    private final int y;
    private final int height;

    public InlineGlyphHit(InlineGlyphMatch match, int x, int width) {
        this(match, x, width, 0, 0);
    }

    public InlineGlyphHit(InlineGlyphMatch match, int x, int width, int y, int height) {
        this.match = match;
        this.x = x;
        this.width = width;
        this.y = y;
        this.height = height;
    }

    public InlineGlyphMatch match() { return match; }

    public int x() { return x; }

    public int width() { return width; }

    public int y() { return y; }

    public int height() { return height; }
}
