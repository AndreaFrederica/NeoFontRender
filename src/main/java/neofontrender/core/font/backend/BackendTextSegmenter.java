package neofontrender.core.font.backend;

import neofontrender.core.config.NeofontrenderConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Conservative tokenization for reusable backend render-cache entries.
 *
 * <p>Only text that does not require cross-token shaping is segmented. Complex scripts,
 * combining marks, bidi controls, ideographic description sequences, and emoji-style
 * variation/ZWJ sequences stay on the full-run render path.</p>
 */
public final class BackendTextSegmenter {
    private static volatile long attempts;
    private static volatile long segmentedRuns;
    private static volatile long rejectedRuns;
    private static volatile long emittedSegments;

    private enum Kind { LATIN_WORD, DIGIT, CJK, SPACE, SIMPLE }

    private BackendTextSegmenter() {}

    public static List<String> segment(String text) {
        if (!NeofontrenderConfig.segmentCache() || text == null
                || text.length() < NeofontrenderConfig.segmentCacheMinRunLength()) {
            return null;
        }
        return segment(text, NeofontrenderConfig.segmentCacheMaxRunCodePoints(),
                NeofontrenderConfig.segmentCacheMaxSegments(), NeofontrenderConfig.debugRenderStats());
    }

    static List<String> segment(String text, int maxRunCodePoints, int maxSegments, boolean stats) {
        if (stats) attempts++;

        ArrayList<String> out = new ArrayList<>();
        int segmentStart = 0;
        Kind segmentKind = null;
        int segmentCodePoints = 0;
        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            int next = i + Character.charCount(codePoint);
            Kind kind = classify(codePoint);
            if (kind == null) {
                if (stats) rejectedRuns++;
                return null;
            }
            boolean split = segmentKind == null || !canMerge(segmentKind, kind)
                    || segmentCodePoints >= maxRunCodePoints;
            if (split) {
                if (segmentKind != null) addSegment(out, text.substring(segmentStart, i));
                segmentStart = i;
                segmentKind = kind;
                segmentCodePoints = 0;
            }
            segmentCodePoints++;
            i = next;
        }
        if (segmentKind != null) addSegment(out, text.substring(segmentStart));
        if (out.size() > 1 && out.size() <= maxSegments) {
            if (stats) {
                segmentedRuns++;
                emittedSegments += out.size();
            }
            return out;
        }
        if (stats) rejectedRuns++;
        return null;
    }

    public static DebugState debugState() {
        return new DebugState(NeofontrenderConfig.segmentCache(), attempts,
                segmentedRuns, rejectedRuns, emittedSegments);
    }

    private static void addSegment(List<String> out, String segment) {
        if (!segment.isEmpty()) out.add(segment);
    }

    private static boolean canMerge(Kind previous, Kind current) {
        return previous == current && (previous == Kind.LATIN_WORD || previous == Kind.SPACE);
    }

    private static Kind classify(int codePoint) {
        if (isUnsafeForSegmentation(codePoint)) return null;
        if (Character.isWhitespace(codePoint)) return Kind.SPACE;
        if (isAsciiDigit(codePoint)) return Kind.DIGIT;
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        if (isCjkLike(script)) return Kind.CJK;
        if (isLatinWordCodePoint(codePoint, script)) return Kind.LATIN_WORD;
        return isSimpleStandalone(codePoint) ? Kind.SIMPLE : null;
    }

    private static boolean isUnsafeForSegmentation(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || type == Character.FORMAT
                || type == Character.SURROGATE
                || type == Character.PRIVATE_USE
                || type == Character.UNASSIGNED
                || codePoint == 0x200D
                || (codePoint >= 0x2FF0 && codePoint <= 0x2FFF)
                || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                || (codePoint >= 0xE0100 && codePoint <= 0xE01EF)
                || (codePoint >= 0x2066 && codePoint <= 0x2069);
    }

    private static boolean isLatinWordCodePoint(int codePoint, Character.UnicodeScript script) {
        return (script == Character.UnicodeScript.LATIN || script == Character.UnicodeScript.COMMON)
                && (isAsciiLetter(codePoint) || codePoint == '_' || codePoint == '-');
    }

    private static boolean isSimpleStandalone(int codePoint) {
        if (codePoint > 0xFFFF) return false;
        int type = Character.getType(codePoint);
        return type == Character.DECIMAL_DIGIT_NUMBER
                || type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL;
    }

    private static boolean isCjkLike(Character.UnicodeScript script) {
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private static boolean isAsciiDigit(int codePoint) { return codePoint >= '0' && codePoint <= '9'; }

    private static boolean isAsciiLetter(int codePoint) {
        return (codePoint >= 'A' && codePoint <= 'Z') || (codePoint >= 'a' && codePoint <= 'z');
    }

    public static final class DebugState {
        private final boolean enabled;
        private final long attempts;
        private final long segmentedRuns;
        private final long rejectedRuns;
        private final long emittedSegments;

        private DebugState(boolean enabled, long attempts, long segmentedRuns,
                           long rejectedRuns, long emittedSegments) {
            this.enabled = enabled;
            this.attempts = attempts;
            this.segmentedRuns = segmentedRuns;
            this.rejectedRuns = rejectedRuns;
            this.emittedSegments = emittedSegments;
        }

        public boolean enabled() { return enabled; }
        public long attempts() { return attempts; }
        public long segmentedRuns() { return segmentedRuns; }
        public long rejectedRuns() { return rejectedRuns; }
        public long emittedSegments() { return emittedSegments; }
    }
}
