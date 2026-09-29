package neofontrender.core.font.cosmic;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** Reusable word composition and oversized-raster splitting at Unicode boundaries. */
final class CosmicTextSegmenter {
    private CosmicTextSegmenter() {
    }

    /**
     * Split plain style-resolved text at reusable word boundaries, independently of UI,
     * numeric content or structured syntax. Keep spaces with the preceding word and
     * punctuation inside Western tokens to preserve kerning/ligatures within a token.
     * Bidi paragraphs and control characters require whole-run layout.
     */
    static List<String> splitReusableWords(String text) {
        if (text == null || text.isEmpty()) return List.of();
        boolean ascii = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029') return List.of(text);
            ascii &= c <= '~';
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        if (ascii) {
            // The common HUD/chat path needs no Unicode iterators or char[] allocation.
            for (int i = 1; i < text.length(); i++) {
                if (text.charAt(i - 1) == ' ' && text.charAt(i) != ' ') {
                    result.add(text.substring(start, i));
                    start = i;
                }
            }
        } else {
            char[] chars = text.toCharArray();
            if (java.text.Bidi.requiresBidi(chars, 0, chars.length)) return List.of(text);
            BreakIterator words = BreakIterator.getWordInstance(Locale.ROOT);
            BreakIterator characters = BreakIterator.getCharacterInstance(Locale.ROOT);
            words.setText(text);
            characters.setText(text);
            for (int end = words.first(); end != BreakIterator.DONE; end = words.next()) {
                if (end <= start || end == text.length() || !characters.isBoundary(end)) continue;
                int before = text.codePointBefore(end);
                int after = text.codePointAt(end);
                boolean spaceBoundary = Character.isWhitespace(before)
                        && !Character.isWhitespace(after);
                boolean ideographicBoundary = !Character.isWhitespace(before)
                        && !Character.isWhitespace(after)
                        && (Character.isIdeographic(before) || Character.isIdeographic(after));
                if (spaceBoundary || ideographicBoundary) {
                    result.add(text.substring(start, end));
                    start = end;
                }
            }
        }
        if (start == 0) return List.of(text);
        result.add(text.substring(start));
        return List.copyOf(result);
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
