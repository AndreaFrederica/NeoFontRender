package neofontrender.addons.api.ui.navigation;

public final class UiRect {
    public static final UiRect EMPTY = new UiRect(0, 0, 0, 0);

    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public UiRect(int left, int top, int right, int bottom) {
        if (right < left || bottom < top) throw new IllegalArgumentException("invalid rectangle");
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int width() { return right - left; }
    public int height() { return bottom - top; }
    public double centerX() { return (left + right) * 0.5D; }
    public double centerY() { return (top + bottom) * 0.5D; }
    public boolean isEmpty() { return width() <= 0 || height() <= 0; }

    public UiRect intersect(UiRect other) {
        int nextLeft = Math.max(left, other.left);
        int nextTop = Math.max(top, other.top);
        int nextRight = Math.max(nextLeft, Math.min(right, other.right));
        int nextBottom = Math.max(nextTop, Math.min(bottom, other.bottom));
        return new UiRect(nextLeft, nextTop, nextRight, nextBottom);
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof UiRect)) return false;
        UiRect other = (UiRect) object;
        return left == other.left && top == other.top && right == other.right && bottom == other.bottom;
    }
    @Override public int hashCode() {
        int result = left;
        result = 31 * result + top;
        result = 31 * result + right;
        return 31 * result + bottom;
    }
    @Override public String toString() {
        return "UiRect{" + left + "," + top + " -> " + right + "," + bottom + "}";
    }
}
