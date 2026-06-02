package neofontrender.core.font.skia;

import neofontrender.core.config.NeofontrenderConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Conservative tokenization for Skia cache reuse.
 *
 * <p>Only text that does not require cross-token shaping is segmented. Complex
 * scripts, combining marks, bidi controls, ideographic description sequences,
 * and emoji-style variation/ZWJ sequences are kept on the full-run render path.</p>
 */
public final class SkiaTextSegmenter {

    private static volatile long attempts;
    private static volatile long segmentedRuns;
    private static volatile long rejectedRuns;
    private static volatile long emittedSegments;

    private enum Kind {
        LATIN_WORD,
        DIGIT,
        CJK,
        SPACE,
        SIMPLE
    }

    private SkiaTextSegmenter() {
    }

    public static List<String> segment(String text) {
        if (!NeofontrenderConfig.skiaSegmentCache() || text == null) {
            return null;
        }
        if (text.length() < NeofontrenderConfig.skiaSegmentCacheMinRunLength()) {
            return null;
        }
        boolean stats = NeofontrenderConfig.debugRenderStats();
        if (stats) {
            attempts++;
        }

        ArrayList<String> out = new ArrayList<>();
        int segmentStart = 0;
        Kind segmentKind = null;
        int segmentCodePoints = 0;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int next = i + Character.charCount(codePoint);
            Kind kind = classify(codePoint);
            if (kind == null) {
                if (stats) {
                    rejectedRuns++;
                }
                return null;
            }

            boolean split = segmentKind == null
                    || !canMerge(segmentKind, kind)
                    || segmentCodePoints >= NeofontrenderConfig.skiaSegmentCacheMaxRunCodePoints();
            if (split) {
                if (segmentKind != null) {
                    addSegment(out, text.substring(segmentStart, i));
                }
                segmentStart = i;
                segmentKind = kind;
                segmentCodePoints = 0;
            }

            segmentCodePoints++;
            i = next;
        }

        if (segmentKind != null) {
            addSegment(out, text.substring(segmentStart));
        }

        if (out.size() > 1 && out.size() <= NeofontrenderConfig.skiaSegmentCacheMaxSegments()) {
            if (stats) {
                segmentedRuns++;
                emittedSegments += out.size();
            }
            return out;
        }
        if (stats) {
            rejectedRuns++;
        }
        return null;
    }

    public static DebugState debugState() {
        return new DebugState(
                NeofontrenderConfig.skiaSegmentCache(),
                attempts,
                segmentedRuns,
                rejectedRuns,
                emittedSegments);
    }

    private static void addSegment(List<String> out, String segment) {
        if (!segment.isEmpty()) {
            out.add(segment);
        }
    }

    private static boolean canMerge(Kind previous, Kind current) {
        if (previous != current) {
            return false;
        }
        return previous == Kind.LATIN_WORD || previous == Kind.SPACE;
    }

    private static Kind classify(int codePoint) {
        if (isUnsafeForSegmentation(codePoint)) {
            return null;
        }
        if (Character.isWhitespace(codePoint)) {
            return Kind.SPACE;
        }
        if (isAsciiDigit(codePoint)) {
            return Kind.DIGIT;
        }
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        if (isCjkLike(script)) {
            return Kind.CJK;
        }
        if (isLatinWordCodePoint(codePoint, script)) {
            return Kind.LATIN_WORD;
        }
        if (isSimpleStandalone(codePoint)) {
            return Kind.SIMPLE;
        }
        return null;
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
        if (codePoint > 0xFFFF) {
            return false;
        }
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

    private static boolean isAsciiDigit(int codePoint) {
        return codePoint >= '0' && codePoint <= '9';
    }

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

        public boolean enabled() {
            return enabled;
        }

        public long attempts() {
            return attempts;
        }

        public long segmentedRuns() {
            return segmentedRuns;
        }

        public long rejectedRuns() {
            return rejectedRuns;
        }

        public long emittedSegments() {
            return emittedSegments;
        }
    }
}
