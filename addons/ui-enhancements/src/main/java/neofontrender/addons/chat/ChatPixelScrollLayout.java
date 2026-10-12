package neofontrender.addons.chat;

import java.util.List;
import java.util.function.ToIntFunction;

/** Converts a bottom-anchored pixel offset into a message index and row remainder. */
public final class ChatPixelScrollLayout {
    private ChatPixelScrollLayout() {}

    /** Builds an immutable prefix-height index, measuring every row exactly once. */
    public static <T> Index index(List<T> rows, ToIntFunction<T> height) {
        int[] offsets = new int[rows.size() + 1];
        for (int row = 0; row < rows.size(); row++) {
            offsets[row + 1] = offsets[row] + Math.max(1, height.applyAsInt(rows.get(row)));
        }
        return new Index(offsets);
    }

    public static <T> float maximum(List<T> rows, int viewportHeight,
                                    ToIntFunction<T> height) {
        int contentHeight = 0;
        for (T row : rows) contentHeight += Math.max(1, height.applyAsInt(row));
        return Math.max(0, contentHeight - Math.max(1, viewportHeight));
    }

    public static <T> float offsetForIndex(List<T> rows, int index,
                                           ToIntFunction<T> height) {
        int end = Math.max(0, Math.min(index, rows.size()));
        int offset = 0;
        for (int row = 0; row < end; row++) {
            offset += Math.max(1, height.applyAsInt(rows.get(row)));
        }
        return offset;
    }

    /** Highest row index that still keeps the oldest content inside the viewport. */
    public static <T> int maximumIndex(List<T> rows, int viewportHeight,
                                       ToIntFunction<T> height) {
        int available = Math.max(1, viewportHeight);
        int used = 0;
        int start = rows.size();
        while (start > 0) {
            int rowHeight = Math.max(1, height.applyAsInt(rows.get(start - 1)));
            if (start < rows.size() && used + rowHeight > available) break;
            used += rowHeight;
            start--;
            if (used >= available) break;
        }
        return start;
    }

    public static <T> Position locate(List<T> rows, float pixelOffset,
                                      ToIntFunction<T> height) {
        float remaining = Math.max(0.0F, pixelOffset);
        int index = 0;
        while (index < rows.size()) {
            int rowHeight = Math.max(1, height.applyAsInt(rows.get(index)));
            if (remaining < rowHeight) break;
            remaining -= rowHeight;
            index++;
        }
        return new Position(index, remaining);
    }

    public static final class Position {
        public final int index;
        public final float remainder;

        private Position(int index, float remainder) {
            this.index = index;
            this.remainder = remainder;
        }
    }

    /** Prefix sums used for constant-time totals and binary-search pixel lookup. */
    public static final class Index {
        private final int[] offsets;

        private Index(int[] offsets) {
            this.offsets = offsets;
        }

        public int size() {
            return offsets.length - 1;
        }

        public int totalHeight() {
            return offsets[offsets.length - 1];
        }

        public int height(int row) {
            if (row < 0 || row >= size()) return 0;
            return offsets[row + 1] - offsets[row];
        }

        public int offsetForIndex(int index) {
            return offsets[Math.max(0, Math.min(index, size()))];
        }

        public float maximum(int viewportHeight) {
            return Math.max(0, totalHeight() - Math.max(1, viewportHeight));
        }

        public Position locate(float pixelOffset) {
            float target = Math.max(0.0F, pixelOffset);
            int low = 0;
            int high = size();
            while (low < high) {
                int middle = (low + high + 1) >>> 1;
                if (offsets[middle] <= target) low = middle;
                else high = middle - 1;
            }
            return new Position(low, target - offsets[low]);
        }
    }
}
