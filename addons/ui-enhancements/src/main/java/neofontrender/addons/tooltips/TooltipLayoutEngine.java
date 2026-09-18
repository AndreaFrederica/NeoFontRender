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
        int horizontal() { return left + right; }
        int vertical() { return top + bottom; }
    }

    static final class Constraints {
        final int maxWidth, maxHeight;
        Constraints(int maxWidth, int maxHeight) {
            this.maxWidth = Math.max(1, maxWidth); this.maxHeight = Math.max(1, maxHeight);
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
            return new Measurement(Math.min(width, constraints.maxWidth), Math.min(height, constraints.maxHeight));
        }
        @Override public void place(Rect bounds) { this.bounds = bounds; }
        Rect bounds() { return bounds; }
    }

    static final class Flow implements Node {
        private final Direction direction;
        private final Alignment crossAlignment;
        private final int gap;
        private final Insets padding;
        private final List<Node> children;
        private Rect bounds = new Rect(0, 0, 0, 0);

        Flow(Direction direction, Alignment crossAlignment, int gap, Insets padding,
             List<? extends Node> children) {
            this.direction = direction == null ? Direction.COLUMN : direction;
            this.crossAlignment = crossAlignment == null ? Alignment.START : crossAlignment;
            this.gap = Math.max(0, gap);
            this.padding = padding == null ? Insets.none() : padding;
            this.children = children == null ? Collections.emptyList() :
                    Collections.unmodifiableList(new ArrayList<>(children));
        }
        @Override public Measurement measure(Constraints constraints) {
            int main = direction == Direction.ROW ? padding.horizontal() : padding.vertical();
            int cross = direction == Direction.ROW ? padding.vertical() : padding.horizontal();
            int count = 0;
            for (Node child : children) {
                Measurement value = child.measure(constraints);
                if (direction == Direction.ROW) { main += value.width; cross = Math.max(cross, value.height); }
                else { main += value.height; cross = Math.max(cross, value.width); }
                count++;
            }
            if (count > 1) main += gap * (count - 1);
            int width = direction == Direction.ROW ? main : cross;
            int height = direction == Direction.ROW ? cross : main;
            return new Measurement(Math.min(width, constraints.maxWidth), Math.min(height, constraints.maxHeight));
        }
        @Override public void place(Rect bounds) {
            this.bounds = bounds;
            int cursor = direction == Direction.ROW ? bounds.x + padding.left : bounds.y + padding.top;
            int innerWidth = Math.max(0, bounds.width - padding.horizontal());
            int innerHeight = Math.max(0, bounds.height - padding.vertical());
            for (Node child : children) {
                Measurement value = child.measure(new Constraints(innerWidth, innerHeight));
                int width = direction == Direction.ROW ? value.width : crossSize(value.width, innerWidth);
                int height = direction == Direction.ROW ? crossSize(value.height, innerHeight) : value.height;
                int cross = direction == Direction.ROW ? innerHeight - height : innerWidth - width;
                int offset = crossAlignment == Alignment.CENTER ? cross / 2
                        : crossAlignment == Alignment.END ? cross : 0;
                if (direction == Direction.ROW) child.place(new Rect(cursor, bounds.y + padding.top + offset, width, height));
                else child.place(new Rect(bounds.x + padding.left + offset, cursor, width, height));
                cursor += (direction == Direction.ROW ? width : height) + gap;
            }
        }
        private int crossSize(int value, int available) { return crossAlignment == Alignment.STRETCH ? available : value; }
        Rect bounds() { return bounds; }
    }

    /** Places children left-to-right and starts a new row when the width limit is reached. */
    static final class Wrap implements Node {
        private final int widthLimit;
        private final int gap;
        private final int rowGap;
        private final Insets padding;
        private final List<Node> children;
        private Rect bounds = new Rect(0, 0, 0, 0);

        Wrap(int widthLimit, int gap, int rowGap, Insets padding, List<? extends Node> children) {
            this.widthLimit = widthLimit == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, widthLimit);
            this.gap = Math.max(0, gap);
            this.rowGap = Math.max(0, rowGap);
            this.padding = padding == null ? Insets.none() : padding;
            this.children = children == null ? Collections.emptyList() :
                    Collections.unmodifiableList(new ArrayList<>(children));
        }

        @Override public Measurement measure(Constraints constraints) {
            int limit = Math.min(widthLimit, Math.max(1, constraints.maxWidth));
            int rowWidth = 0;
            int rowHeight = 0;
            int totalWidth = 0;
            int totalHeight = padding.vertical();
            boolean hasRow = false;
            for (Node child : children) {
                Measurement value = child.measure(new Constraints(limit, constraints.maxHeight));
                int next = rowWidth == 0 ? value.width : rowWidth + gap + value.width;
                if (hasRow && next > limit - padding.horizontal()) {
                    totalWidth = Math.max(totalWidth, rowWidth);
                    totalHeight += rowHeight + rowGap;
                    rowWidth = value.width;
                    rowHeight = value.height;
                } else {
                    rowWidth = next;
                    rowHeight = Math.max(rowHeight, value.height);
                }
                hasRow = true;
            }
            if (hasRow) {
                totalWidth = Math.max(totalWidth, rowWidth);
                totalHeight += rowHeight;
            }
            return new Measurement(Math.min(constraints.maxWidth, totalWidth + padding.horizontal()),
                    Math.min(constraints.maxHeight, totalHeight));
        }

        @Override public void place(Rect bounds) {
            this.bounds = bounds;
            int limit = Math.min(widthLimit, Math.max(1, bounds.width - padding.horizontal()));
            int x = bounds.x + padding.left;
            int y = bounds.y + padding.top;
            int rowHeight = 0;
            for (Node child : children) {
                Measurement value = child.measure(new Constraints(limit, Math.max(1, bounds.height)));
                if (x > bounds.x + padding.left && x - (bounds.x + padding.left) + gap + value.width > limit) {
                    x = bounds.x + padding.left;
                    y += rowHeight + rowGap;
                    rowHeight = 0;
                } else if (x > bounds.x + padding.left) {
                    x += gap;
                }
                child.place(new Rect(x, y, value.width, value.height));
                x += value.width;
                rowHeight = Math.max(rowHeight, value.height);
            }
        }

        Rect bounds() { return bounds; }
    }
}
