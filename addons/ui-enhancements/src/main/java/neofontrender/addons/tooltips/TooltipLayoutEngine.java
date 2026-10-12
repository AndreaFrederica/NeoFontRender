package neofontrender.addons.tooltips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small retained geometry engine for tooltip composition; it has no OpenGL side effects. */
final class TooltipLayoutEngine {
    private TooltipLayoutEngine() {}

    static final class Insets {
        final int left, top, right, bottom;
        Insets(int left, int top, int right, int bottom) {
            this.left = Math.max(0, left); this.top = Math.max(0, top);
            this.right = Math.max(0, right); this.bottom = Math.max(0, bottom);
        }
        static Insets none() { return new Insets(0, 0, 0, 0); }
        int horizontal() { return add(left, right); }
        int vertical() { return add(top, bottom); }
    }

    /** Available space for responsive nodes, not permission to truncate fixed-size content. */
    static final class Constraints {
        final int maxWidth, maxHeight;
        Constraints(int maxWidth, int maxHeight) {
            this.maxWidth = Math.max(0, maxWidth); this.maxHeight = Math.max(0, maxHeight);
        }
        static Constraints unbounded() { return new Constraints(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    }

    static final class Rect {
        final int x, y, width, height;
        Rect(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = Math.max(0, width); this.height = Math.max(0, height);
        }
    }

    enum Direction { ROW, COLUMN }
    enum Alignment { START, CENTER, END, STRETCH }

    interface Node {
        Measurement measure(Constraints constraints);
        void place(Rect bounds);
    }

    static final class Measurement {
        final int width, height;
        Measurement(int width, int height) { this.width = Math.max(0, width); this.height = Math.max(0, height); }
    }

    static final class Leaf implements Node {
        private final int width, height;
        private Rect bounds = new Rect(0, 0, 0, 0);
        Leaf(int width, int height) { this.width = Math.max(0, width); this.height = Math.max(0, height); }
        @Override public Measurement measure(Constraints constraints) {
            return new Measurement(width, height);
        }
        @Override public void place(Rect bounds) { this.bounds = bounds; }
        Rect bounds() { return bounds; }
    }

    private static final class Layout {
        final Measurement size;
        final List<Rect> children;
        Layout(int width, int height, List<Rect> children) {
            this.size = new Measurement(width, height);
            this.children = children;
        }
    }

    /** Retains the measured arrangement so fitting a container never measures its children again. */
    private abstract static class Container implements Node {
        final List<Node> children;
        private Layout measured;
        private Rect bounds = new Rect(0, 0, 0, 0);

        Container(List<? extends Node> children) {
            this.children = children == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(children));
        }

        abstract Layout layout(Constraints constraints);

        @Override public final Measurement measure(Constraints constraints) {
            measured = layout(constraints);
            return measured.size;
        }

        @Override public final void place(Rect bounds) {
            this.bounds = bounds;
            if (measured == null || measured.size.width != bounds.width || measured.size.height != bounds.height) {
                measured = layout(new Constraints(bounds.width, bounds.height));
            }
            placeChildren(bounds, measured);
        }

        void placeChildren(Rect bounds, Layout layout) {
            for (int index = 0; index < children.size(); index++) {
                Rect child = layout.children.get(index);
                children.get(index).place(new Rect(bounds.x + child.x, bounds.y + child.y, child.width, child.height));
            }
        }

        final Rect bounds() { return bounds; }
    }

    static final class Flow extends Container {
        private final Direction direction;
        private final Alignment crossAlignment;
        private final int gap;
        private final Insets padding;

        Flow(Direction direction, Alignment crossAlignment, int gap, Insets padding,
             List<? extends Node> children) {
            super(children);
            this.direction = direction == null ? Direction.COLUMN : direction;
            this.crossAlignment = crossAlignment == null ? Alignment.START : crossAlignment;
            this.gap = Math.max(0, gap);
            this.padding = padding == null ? Insets.none() : padding;
        }

        @Override Layout layout(Constraints constraints) {
            boolean row = direction == Direction.ROW;
            Constraints childConstraints = row
                    ? new Constraints(Integer.MAX_VALUE, Math.max(0, constraints.maxHeight - padding.vertical()))
                    : new Constraints(Math.max(0, constraints.maxWidth - padding.horizontal()), Integer.MAX_VALUE);
            int main = 0;
            int cross = 0;
            List<Rect> placements = new ArrayList<>(children.size());
            for (Node child : children) {
                Measurement value = child.measure(childConstraints);
                if (!placements.isEmpty()) main = add(main, gap);
                placements.add(new Rect(row ? main : 0, row ? 0 : main, value.width, value.height));
                main = add(main, row ? value.width : value.height);
                cross = Math.max(cross, row ? value.height : value.width);
            }
            return new Layout(add(row ? main : cross, padding.horizontal()),
                    add(row ? cross : main, padding.vertical()), placements);
        }

        @Override void placeChildren(Rect bounds, Layout layout) {
            boolean row = direction == Direction.ROW;
            int innerCross = Math.max(0, row ? bounds.height - padding.vertical() : bounds.width - padding.horizontal());
            for (int index = 0; index < children.size(); index++) {
                Rect child = layout.children.get(index);
                int naturalCross = row ? child.height : child.width;
                int cross = crossAlignment == Alignment.STRETCH ? Math.max(naturalCross, innerCross) : naturalCross;
                int free = Math.max(0, innerCross - cross);
                int offset = crossAlignment == Alignment.CENTER ? free / 2
                        : crossAlignment == Alignment.END ? free : 0;
                children.get(index).place(new Rect(bounds.x + padding.left + child.x + (row ? 0 : offset),
                        bounds.y + padding.top + child.y + (row ? offset : 0),
                        row ? child.width : cross, row ? cross : child.height));
            }
        }
    }

    /** Places children left-to-right and starts a new row when the available inner width is reached. */
    static final class Wrap extends Container {
        private final int widthLimit;
        private final int gap;
        private final int rowGap;
        private final Insets padding;

        Wrap(int widthLimit, int gap, int rowGap, Insets padding, List<? extends Node> children) {
            super(children);
            this.widthLimit = Math.max(0, widthLimit);
            this.gap = Math.max(0, gap);
            this.rowGap = Math.max(0, rowGap);
            this.padding = padding == null ? Insets.none() : padding;
        }

        @Override Layout layout(Constraints constraints) {
            int limit = Math.max(0, Math.min(widthLimit, constraints.maxWidth) - padding.horizontal());
            Constraints childConstraints = new Constraints(limit, Integer.MAX_VALUE);
            int rowWidth = 0;
            int rowHeight = 0;
            int totalWidth = 0;
            int y = 0;
            boolean hasRow = false;
            List<Rect> placements = new ArrayList<>(children.size());
            for (Node child : children) {
                Measurement value = child.measure(childConstraints);
                int next = hasRow ? add(add(rowWidth, gap), value.width) : value.width;
                if (hasRow && next > limit) {
                    totalWidth = Math.max(totalWidth, rowWidth);
                    y = add(add(y, rowHeight), rowGap);
                    rowWidth = 0;
                    rowHeight = 0;
                    hasRow = false;
                }
                if (hasRow) rowWidth = add(rowWidth, gap);
                placements.add(new Rect(add(padding.left, rowWidth), add(padding.top, y), value.width, value.height));
                rowWidth = add(rowWidth, value.width);
                rowHeight = Math.max(rowHeight, value.height);
                hasRow = true;
            }
            totalWidth = Math.max(totalWidth, rowWidth);
            return new Layout(add(totalWidth, padding.horizontal()),
                    add(add(y, rowHeight), padding.vertical()), placements);
        }
    }

    /** Shared column tracks with fewer columns when the natural tracks exceed the available width. */
    static final class Grid extends Container {
        private final int columns;
        private final int gap;

        Grid(int columns, int gap, List<? extends Node> children) {
            super(children);
            this.columns = Math.max(1, columns);
            this.gap = Math.max(0, gap);
        }

        @Override Layout layout(Constraints constraints) {
            int count = Math.min(columns, children.size());
            if (count == 0) return new Layout(0, 0, Collections.emptyList());
            List<Measurement> sizes = new ArrayList<>(children.size());
            for (Node child : children) sizes.add(child.measure(Constraints.unbounded()));
            int[] widths = columnWidths(sizes, count);
            while (count > 1 && trackSize(widths) > constraints.maxWidth) {
                widths = columnWidths(sizes, --count);
            }
            if (count == 1) {
                sizes.clear();
                Constraints childConstraints = new Constraints(constraints.maxWidth, Integer.MAX_VALUE);
                for (Node child : children) sizes.add(child.measure(childConstraints));
                widths = columnWidths(sizes, count);
            }
            int rows = (children.size() - 1) / count + 1;
            int[] heights = new int[rows];
            for (int index = 0; index < sizes.size(); index++) {
                heights[index / count] = Math.max(heights[index / count], sizes.get(index).height);
            }
            int[] x = new int[count];
            for (int column = 1; column < count; column++) x[column] = add(add(x[column - 1], widths[column - 1]), gap);
            List<Rect> placements = new ArrayList<>(children.size());
            int y = 0;
            for (int index = 0; index < sizes.size(); index++) {
                if (index > 0 && index % count == 0) y = add(add(y, heights[index / count - 1]), gap);
                Measurement value = sizes.get(index);
                placements.add(new Rect(x[index % count], y, value.width, value.height));
            }
            return new Layout(trackSize(widths), trackSize(heights), placements);
        }

        private int[] columnWidths(List<Measurement> sizes, int count) {
            int[] widths = new int[count];
            for (int index = 0; index < sizes.size(); index++) {
                widths[index % count] = Math.max(widths[index % count], sizes.get(index).width);
            }
            return widths;
        }

        private int trackSize(int[] values) {
            int total = 0;
            for (int index = 0; index < values.length; index++) {
                if (index > 0) total = add(total, gap);
                total = add(total, values[index]);
            }
            return total;
        }
    }

    private static int add(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
