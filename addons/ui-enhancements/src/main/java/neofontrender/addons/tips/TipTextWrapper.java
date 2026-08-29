package neofontrender.addons.tips;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/** Unicode-safe tip wrapping shared by the vanilla and modern text render paths. */
final class TipTextWrapper {
    private TipTextWrapper() {}

    static List<String> wrap(String text, int maximumWidth, ToDoubleFunction<String> measure) {
        String source = text == null ? "" : text;
        int width = Math.max(1, maximumWidth);
        List<String> lines = new ArrayList<>();
        String[] paragraphs = source.split("\\n", -1);
        for (String paragraph : paragraphs) {
            wrapParagraph(paragraph, width, measure, lines);
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static void wrapParagraph(String text, int width, ToDoubleFunction<String> measure,
                                      List<String> lines) {
        if (text.isEmpty() || measure.applyAsDouble(text) <= width) {
            lines.add(text);
            return;
        }

        List<Integer> boundaries = characterBoundaries(text);
        Set<Integer> lineBoundaries = lineBoundaries(text);
        int startBoundary = 0;
        int lastBoundary = boundaries.size() - 1;
        while (startBoundary < lastBoundary) {
            int endBoundary = farthestFittingBoundary(
                    text, boundaries, startBoundary, lastBoundary, width, measure);
            if (endBoundary < lastBoundary) {
                endBoundary = preferredBoundary(
                        text, boundaries, lineBoundaries, startBoundary, endBoundary);
            }

            int start = boundaries.get(startBoundary);
            int end = boundaries.get(endBoundary);
            int visibleEnd = trimTrailingWhitespace(text, start, end);
            lines.add(text.substring(start, visibleEnd));

            int next = skipLeadingWhitespace(text, end);
            startBoundary = boundaryIndexAtOrAfter(boundaries, endBoundary, next);
        }
    }

    private static List<Integer> characterBoundaries(String text) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        List<Integer> boundaries = new ArrayList<>();
        for (int boundary = iterator.first(); boundary != BreakIterator.DONE; boundary = iterator.next()) {
            // Minecraft formatting codes are an indivisible two-character control sequence.
            if (boundary > 0 && boundary < text.length() && text.charAt(boundary - 1) == '\u00A7') continue;
            boundaries.add(boundary);
        }
        return boundaries;
    }

    private static Set<Integer> lineBoundaries(String text) {
        BreakIterator iterator = BreakIterator.getLineInstance(Locale.ROOT);
        iterator.setText(text);
        Set<Integer> boundaries = new HashSet<>();
        for (int boundary = iterator.first(); boundary != BreakIterator.DONE; boundary = iterator.next()) {
            boundaries.add(boundary);
        }
        return boundaries;
    }

    private static int farthestFittingBoundary(String text, List<Integer> boundaries,
                                               int startBoundary, int lastBoundary, int width,
                                               ToDoubleFunction<String> measure) {
        int start = boundaries.get(startBoundary);
        int fitting = startBoundary + 1;
        int low = fitting;
        int high = lastBoundary;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (measure.applyAsDouble(text.substring(start, boundaries.get(middle))) <= width) {
                fitting = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return fitting;
    }

    private static int preferredBoundary(String text, List<Integer> boundaries,
                                         Set<Integer> lineBoundaries, int startBoundary,
                                         int endBoundary) {
        int start = boundaries.get(startBoundary);
        int minimumOffset = start + (boundaries.get(endBoundary) - start) / 2;
        for (int candidate = endBoundary; candidate > startBoundary + 1; candidate--) {
            int offset = boundaries.get(candidate);
            if (offset < minimumOffset) break;
            int previous = text.codePointBefore(offset);
            int next = text.codePointAt(offset);
            if (lineBoundaries.contains(offset)
                    || Character.isWhitespace(previous)
                    || Character.isWhitespace(next)) {
                return candidate;
            }
        }
        return endBoundary;
    }

    private static int trimTrailingWhitespace(String text, int start, int end) {
        int result = end;
        while (result > start) {
            int codePoint = text.codePointBefore(result);
            if (!Character.isWhitespace(codePoint)) break;
            result -= Character.charCount(codePoint);
        }
        return result;
    }

    private static int skipLeadingWhitespace(String text, int start) {
        int result = start;
        while (result < text.length()) {
            int codePoint = text.codePointAt(result);
            if (!Character.isWhitespace(codePoint)) break;
            result += Character.charCount(codePoint);
        }
        return result;
    }

    private static int boundaryIndexAtOrAfter(List<Integer> boundaries, int from, int offset) {
        int index = from;
        while (index < boundaries.size() - 1 && boundaries.get(index) < offset) index++;
        return index;
    }
}
