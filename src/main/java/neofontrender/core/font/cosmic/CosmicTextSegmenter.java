package neofontrender.core.font.cosmic;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** Splits oversized shaped-text runs without cutting Java/Unicode character boundaries. */
final class CosmicTextSegmenter {
    private CosmicTextSegmenter() {
    }

    static List<String> split(String text, double maxWidth, ToDoubleFunction<String> measure) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        if (!(maxWidth > 0.0D) || measure.applyAsDouble(text) <= maxWidth) {
            result.add(text);
            return result;
        }

        List<Integer> boundaries = characterBoundaries(text);
        int startBoundary = 0;
        int lastBoundary = boundaries.size() - 1;
        while (startBoundary < lastBoundary) {
            int endBoundary = farthestFittingBoundary(
                    text, boundaries, startBoundary, lastBoundary, maxWidth, measure);
            if (endBoundary < lastBoundary) {
                endBoundary = preferWhitespaceBoundary(text, boundaries, startBoundary, endBoundary);
            }
            int start = boundaries.get(startBoundary);
            int end = boundaries.get(endBoundary);
            result.add(text.substring(start, end));
            startBoundary = endBoundary;
        }
        return result;
    }

    static List<String> splitInHalf(String text) {
        List<String> result = new ArrayList<>(2);
        if (text == null || text.isEmpty()) {
            return result;
        }
        List<Integer> boundaries = characterBoundaries(text);
        if (boundaries.size() <= 2) {
            result.add(text);
            return result;
        }
        int middleBoundary = (boundaries.size() - 1) / 2;
        int middle = boundaries.get(middleBoundary);
        result.add(text.substring(0, middle));
        result.add(text.substring(middle));
        return result;
    }

    private static List<Integer> characterBoundaries(String text) {
        List<Integer> boundaries = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        for (int boundary = iterator.first(); boundary != BreakIterator.DONE; boundary = iterator.next()) {
            boundaries.add(boundary);
        }
        return boundaries;
    }

    private static int farthestFittingBoundary(String text, List<Integer> boundaries,
                                               int startBoundary, int lastBoundary,
                                               double maxWidth, ToDoubleFunction<String> measure) {
        int start = boundaries.get(startBoundary);
        // A single Unicode character is indivisible even if a pathological font makes it wider
        // than the target. The renderer's native 8192-pixel hard limit remains the final guard.
        int fitting = startBoundary + 1;
        int low = fitting;
        int high = lastBoundary;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            double width = measure.applyAsDouble(text.substring(start, boundaries.get(middle)));
            if (width <= maxWidth) {
                fitting = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return fitting;
    }

    private static int preferWhitespaceBoundary(String text, List<Integer> boundaries,
                                                int startBoundary, int endBoundary) {
        int start = boundaries.get(startBoundary);
        int minimumPreferredOffset = start + (boundaries.get(endBoundary) - start) / 2;
        for (int candidate = endBoundary; candidate > startBoundary + 1; candidate--) {
            int offset = boundaries.get(candidate);
            if (offset < minimumPreferredOffset) {
                break;
            }
            int previousCodePoint = text.codePointBefore(offset);
            if (Character.isWhitespace(previousCodePoint)) {
                return candidate;
            }
        }
        return endBoundary;
    }
}
