package neofontrender.addons.chat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ChatSelectionModel<T> {
    private Cursor<T> anchor;
    private Cursor<T> focus;

    public void clear() {
        anchor = null;
        focus = null;
    }

    public void begin(T line, int position) {
        anchor = new Cursor<>(line, position);
        focus = new Cursor<>(line, position);
    }

    public void update(T line, int position) {
        if (anchor != null) focus = new Cursor<>(line, position);
    }

    public boolean hasSelection() {
        return anchor != null && focus != null
                && (anchor.line != focus.line || anchor.position != focus.position);
    }

    public String selectedText(List<T> bottomUpLines, Function<T, String> text) {
        Selection<T> selection = selection(bottomUpLines);
        if (selection == null) return "";
        StringBuilder result = new StringBuilder();
        for (int index = selection.topIndex; index >= selection.bottomIndex; index--) {
            String line = text.apply(bottomUpLines.get(index));
            int start = index == selection.topIndex ? selection.topPosition : 0;
            int end = index == selection.bottomIndex ? selection.bottomPosition : line.length();
            start = clamp(start, line.length());
            end = clamp(end, line.length());
            if (start < end) result.append(line, start, end);
            if (index > selection.bottomIndex) result.append('\n');
        }
        return result.toString();
    }

    public Map<T, Range> ranges(List<T> bottomUpLines, Function<T, String> text) {
        Selection<T> selection = selection(bottomUpLines);
        if (selection == null) return Collections.emptyMap();
        Map<T, Range> ranges = new IdentityHashMap<>();
        for (int index = selection.topIndex; index >= selection.bottomIndex; index--) {
            T item = bottomUpLines.get(index);
            int length = text.apply(item).length();
            int start = index == selection.topIndex ? selection.topPosition : 0;
            int end = index == selection.bottomIndex ? selection.bottomPosition : length;
            ranges.put(item, new Range(clamp(start, length), clamp(end, length)));
        }
        return ranges;
    }

    private Selection<T> selection(List<T> lines) {
        if (!hasSelection()) return null;
        int anchorIndex = lines.indexOf(anchor.line);
        int focusIndex = lines.indexOf(focus.line);
        if (anchorIndex < 0 || focusIndex < 0) return null;
        if (anchorIndex > focusIndex || anchorIndex == focusIndex && anchor.position <= focus.position) {
            return new Selection<>(anchorIndex, anchor.position, focusIndex, focus.position);
        }
        return new Selection<>(focusIndex, focus.position, anchorIndex, anchor.position);
    }

    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(length, value));
    }

    public static final class Range {
        public final int start;
        public final int end;

        private Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class Cursor<T> {
        private final T line;
        private final int position;

        private Cursor(T line, int position) {
            this.line = line;
            this.position = position;
        }
    }

    private static final class Selection<T> {
        private final int topIndex;
        private final int topPosition;
        private final int bottomIndex;
        private final int bottomPosition;

        private Selection(int topIndex, int topPosition, int bottomIndex, int bottomPosition) {
            this.topIndex = topIndex;
            this.topPosition = topPosition;
            this.bottomIndex = bottomIndex;
            this.bottomPosition = bottomPosition;
        }
    }
}
