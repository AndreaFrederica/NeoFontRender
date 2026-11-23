package neofontrender.api.text;

/** Visible drawing bounds relative to a text origin, independent of its logical advance. */
public final class TextVisualBounds {
    public static final TextVisualBounds EMPTY = new TextVisualBounds(0, 0, 0, 0);
    public final float left, top, right, bottom;

    public TextVisualBounds(float left, float top, float right, float bottom) {
        boolean valid = Float.isFinite(left) && Float.isFinite(top)
                && Float.isFinite(right) && Float.isFinite(bottom);
        this.left = valid ? left : 0;
        this.top = valid ? top : 0;
        this.right = valid ? Math.max(left, right) : 0;
        this.bottom = valid ? Math.max(top, bottom) : 0;
    }

    public float width() { return right - left; }
    public float height() { return bottom - top; }
    public boolean isEmpty() { return width() <= 0 || height() <= 0; }

    public TextVisualBounds translate(float x, float y) {
        return isEmpty() ? EMPTY : new TextVisualBounds(left + x, top + y, right + x, bottom + y);
    }

    public TextVisualBounds scale(float scale) {
        return new TextVisualBounds(left * scale, top * scale, right * scale, bottom * scale);
    }

    public TextVisualBounds union(TextVisualBounds other) {
        if (other == null || other.isEmpty()) return this;
        if (isEmpty()) return other;
        return new TextVisualBounds(Math.min(left, other.left), Math.min(top, other.top),
                Math.max(right, other.right), Math.max(bottom, other.bottom));
    }
}
