package neofontrender.text;

import java.util.Objects;

/** Renderer-independent logical sizing for one atomic inline object. */
public final class InlineLayout {
    public enum Alignment { BASELINE, TOP, CENTER, BOTTOM }
    public enum Flow { INLINE, BLOCK }

    private final float rows;
    private final float fixedHeight;
    private final float columns;
    private final float maximumColumns;
    private final Alignment alignment;
    private final Flow flow;

    public InlineLayout(float rows, float columns, float maximumColumns,
                        Alignment alignment) {
        this(rows, columns, maximumColumns, alignment, Flow.INLINE);
    }

    public InlineLayout(float rows, float columns, float maximumColumns,
                        Alignment alignment, Flow flow) {
        this(rows, Float.NaN, columns, maximumColumns, alignment, flow);
    }

    private InlineLayout(float rows, float fixedHeight, float columns, float maximumColumns,
                         Alignment alignment, Flow flow) {
        this.rows = bounded(rows, 1.0F, 0.125F, 64.0F);
        this.fixedHeight = optional(fixedHeight, 1.0F, 4096.0F);
        this.columns = optional(columns, 0.125F, 256.0F);
        this.maximumColumns = optional(maximumColumns, 0.125F, 256.0F);
        this.alignment = alignment == null ? Alignment.BASELINE : alignment;
        this.flow = flow == null ? Flow.INLINE : flow;
    }

    public static InlineLayout oneLine() {
        return new InlineLayout(1.0F, Float.NaN, Float.NaN, Alignment.BASELINE);
    }

    public static InlineLayout legacy(int displayHeight, boolean matchLineHeight) {
        if (matchLineHeight) return oneLine();
        float height = Math.max(1, displayHeight);
        return new InlineLayout(height / 18.0F, height, Float.NaN, Float.NaN,
                Alignment.BASELINE, Flow.INLINE);
    }

    public float rows() { return rows; }
    public boolean hasFixedHeight() { return Float.isFinite(fixedHeight); }
    public float fixedHeight() { return fixedHeight; }
    public float columns() { return columns; }
    public float maximumColumns() { return maximumColumns; }
    public Alignment alignment() { return alignment; }
    public Flow flow() { return flow; }
    public boolean automaticWidth() { return !Float.isFinite(columns); }
    public boolean hasMaximumWidth() { return Float.isFinite(maximumColumns); }

    public Size resolve(InlineRaster raster, float emSize) {
        float em = bounded(emSize, 1.0F, 1.0F, 4096.0F);
        float height = hasFixedHeight() ? fixedHeight : Math.max(1.0F, rows * em);
        float width;
        if (automaticWidth()) {
            width = raster == null ? height
                    : Math.max(1.0F, height * raster.width() / raster.height());
        } else {
            width = Math.max(1.0F, columns * em);
        }
        if (hasMaximumWidth() && width > maximumColumns * em) {
            float maximum = maximumColumns * em;
            if (automaticWidth()) height *= maximum / width;
            width = maximum;
        }
        return new Size(width, Math.max(1.0F, height));
    }

    public float top(float lineTop, float baseline, float height) {
        switch (alignment) {
            case TOP: return lineTop;
            case CENTER: return lineTop + (baseline - lineTop - height) * 0.5F;
            case BOTTOM:
            case BASELINE:
            default: return baseline - height;
        }
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InlineLayout)) return false;
        InlineLayout value = (InlineLayout) other;
        return Float.compare(rows, value.rows) == 0
                && Float.compare(columns, value.columns) == 0
                && Float.compare(fixedHeight, value.fixedHeight) == 0
                && Float.compare(maximumColumns, value.maximumColumns) == 0
                && alignment == value.alignment && flow == value.flow;
    }

    @Override public int hashCode() {
        return Objects.hash(rows, fixedHeight, columns, maximumColumns, alignment, flow);
    }

    @Override public String toString() {
        return "InlineLayout{rows=" + rows + (hasFixedHeight() ? ", fixedHeight=" + fixedHeight : "")
                + ", columns="
                + (automaticWidth() ? "auto" : columns) + ", maxColumns="
                + (hasMaximumWidth() ? maximumColumns : "none") + ", align="
                + alignment.name().toLowerCase(java.util.Locale.ROOT) + ", flow="
                + flow.name().toLowerCase(java.util.Locale.ROOT) + '}';
    }

    private static float optional(float value, float minimum, float maximum) {
        return Float.isFinite(value) ? bounded(value, minimum, minimum, maximum) : Float.NaN;
    }

    private static float bounded(float value, float fallback, float minimum, float maximum) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Size {
        private final float width;
        private final float height;

        private Size(float width, float height) {
            this.width = width;
            this.height = height;
        }

        public float width() { return width; }
        public float height() { return height; }
    }
}
