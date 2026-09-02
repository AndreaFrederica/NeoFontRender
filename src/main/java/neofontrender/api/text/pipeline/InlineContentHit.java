package neofontrender.api.text.pipeline;

/** Hit-test result for a laid-out inline object. */
public final class InlineContentHit {
    private final InlineContentMatch match;
    private final int x;
    private final int width;
    private final int y;
    private final int height;

    public InlineContentHit(InlineContentMatch match, int x, int width, int y, int height) {
        this.match = match;
        this.x = x;
        this.width = width;
        this.y = y;
        this.height = height;
    }

    public InlineContentMatch match() { return match; }
    public int x() { return x; }
    public int width() { return width; }
    public int y() { return y; }
    public int height() { return height; }
}
