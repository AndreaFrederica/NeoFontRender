package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmicTextSegmenterTest {
    @Test
    void keepsRunsWithinTheLimitWhole() {
        List<String> segments = CosmicTextSegmenter.split("short text", 20.0D, String::length);

        assertEquals(List.of("short text"), segments);
    }

    @Test
    void splitsLongRunsAndPreservesEveryCharacter() {
        String text = "alpha beta gamma delta epsilon";
        List<String> segments = CosmicTextSegmenter.split(text, 12.0D, String::length);

        assertTrue(segments.size() > 1);
        assertEquals(text, String.join("", segments));
        for (String segment : segments) {
            assertTrue(segment.length() <= 12, segment);
        }
        assertTrue(segments.get(0).endsWith(" "));
    }

    @Test
    void splitsTheReported9925PixelRasterBelowTheSafeLimit() {
        String text = "x".repeat(9925);
        List<String> segments = CosmicTextSegmenter.split(text, 7168.0D, String::length);

        assertEquals(text, String.join("", segments));
        assertEquals(2, segments.size());
        for (String segment : segments) {
            assertTrue(segment.length() <= 7168, Integer.toString(segment.length()));
        }
    }

    @Test
    void doesNotSplitSurrogatePairsOrCombiningSequences() {
        String text = "A\uD83D\uDE80e\u0301B\uD83D\uDE80e\u0301C";
        List<String> segments = CosmicTextSegmenter.split(
                text, 2.0D, value -> value.codePointCount(0, value.length()));

        assertEquals(text, String.join("", segments));
        for (String segment : segments) {
            assertFalse(Character.isLowSurrogate(segment.charAt(0)), segment);
            assertFalse(Character.isHighSurrogate(segment.charAt(segment.length() - 1)), segment);
            assertFalse(segment.charAt(0) == '\u0301', segment);
        }
    }

    @Test
    void makesProgressWhenOneCharacterExceedsTheTarget() {
        String text = "\uD83D\uDE80\uD83D\uDE80";
        List<String> segments = CosmicTextSegmenter.split(text, 0.5D, ignored -> 1.0D);

        assertEquals(List.of("\uD83D\uDE80", "\uD83D\uDE80"), segments);
    }

    @Test
    void bisectsAtAUnicodeCharacterBoundaryForRasterFailureRecovery() {
        String text = "A\uD83D\uDE80e\u0301B";
        List<String> segments = CosmicTextSegmenter.splitInHalf(text);

        assertEquals(text, String.join("", segments));
        assertEquals(2, segments.size());
        assertFalse(Character.isHighSurrogate(segments.get(0).charAt(segments.get(0).length() - 1)));
        assertFalse(Character.isLowSurrogate(segments.get(1).charAt(0)));
        assertFalse(segments.get(1).charAt(0) == '\u0301');
    }
}
