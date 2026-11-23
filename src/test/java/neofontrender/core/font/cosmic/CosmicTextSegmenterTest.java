package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmicTextSegmenterTest {
    @Test
    void nativeWordCompositionPreservesSpacesAndAgreesWithMeasurement() throws Exception {
        CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
        assertTrue(compatibility.isSupported(), compatibility.getMessage());
        byte[] font;
        try (java.io.InputStream stream = getClass().getResourceAsStream(
                "/assets/neofontrender/fonts/noto_sans_sc-regular.otf")) {
            org.junit.jupiter.api.Assertions.assertNotNull(stream);
            font = stream.readAllBytes();
        }
        long engine = CosmicNative.createEngine(new byte[][] {font}, new String[] {"word-test"},
                "", new String[0], "", "", "", "", false, 0, 9.0F, "en-US");
        try {
            for (String text : List.of("Save game", "office  affinity ", "XYZ: 4.608 / 80.36814 / 274.114",
                    "中文测试 hello 世界", "e\u0301cole café menu")) {
                for (int flags = 0; flags < 4; flags++) {
                    float measured = 0.0F;
                    float rendered = 0.0F;
                    for (String word : CosmicTextSegmenter.splitReusableWords(text)) {
                        measured += CosmicNative.measureSized(engine, word, flags, 9.0F);
                        byte[] raster = CosmicNative.renderSized(engine, word, 0xFFFFFFFF, flags, 9.0F, 12.0F);
                        rendered += java.nio.ByteBuffer.wrap(raster).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                .getFloat(20);
                    }
                    assertEquals(measured, rendered, 0.01F, text);
                    assertEquals(CosmicNative.measureSized(engine, text, flags, 9.0F), measured, 0.01F, text);
                }
            }
        } finally {
            CosmicNative.destroyEngine(engine);
        }
    }

    @Test
    void reusesWordsInAllTextIncludingShortStaticLabels() {
        assertEquals(List.of("Save ", "game"), CosmicTextSegmenter.splitReusableWords("Save game"));
        assertEquals(List.of("alpha  ", "beta ", "gamma"),
                CosmicTextSegmenter.splitReusableWords("alpha  beta gamma"));
        assertEquals(List.of("Minecraft ", "1.12.2 ", "(forge,cleanroom)"),
                CosmicTextSegmenter.splitReusableWords("Minecraft 1.12.2 (forge,cleanroom)"));
    }

    @Test
    void changingCoordinatesReuseLabelsAndOtherCoordinates() {
        List<String> before = CosmicTextSegmenter.splitReusableWords("XYZ: 4.608 / 80.36814 / 274.114");
        List<String> after = CosmicTextSegmenter.splitReusableWords("XYZ: 4.609 / 80.36814 / 274.114");
        assertEquals(1, after.stream().filter(word -> !before.contains(word)).count());
        assertEquals("XYZ: 4.609 / 80.36814 / 274.114", String.join("", after));
    }

    @Test
    void preservesComplexShapingAndParagraphControls() {
        for (String text : List.of("Hello العربية world", "abc\nxyz", "abc\txyz", "a\u00a0b",
                "abc\u2028xyz", "abc\u2029xyz")) {
            assertEquals(List.of(text), CosmicTextSegmenter.splitReusableWords(text));
        }
        assertEquals(List.of("office ", "affinity"),
                CosmicTextSegmenter.splitReusableWords("office affinity"));
    }

    @Test
    void unicodeSegmentsRoundTripAndNeverCutGraphemeClusters() {
        for (String text : List.of("中文测试 hello 世界", "e\u0301cole café menu",
                "Hello 👩‍👩‍👧‍👦 world", "中文👩‍💻测试", "  hello   world  ")) {
            List<String> segments = CosmicTextSegmenter.splitReusableWords(text);
            assertEquals(text, String.join("", segments));
            java.text.BreakIterator characters = java.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT);
            characters.setText(text);
            int offset = 0;
            for (String segment : segments) {
                assertFalse(segment.isEmpty());
                offset += segment.length();
                assertTrue(characters.isBoundary(offset), text + " at " + offset);
                assertEquals(List.of(segment), CosmicTextSegmenter.splitReusableWords(segment));
            }
        }
    }

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
