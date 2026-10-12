package neofontrender.addons.tips;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipTextWrapperTest {
    @Test
    void keepsTextOnOneLineWhenItFitsTheAvailableWidth() {
        assertEquals(List.of("This tip fits in the available loading-screen space."),
                wrap("This tip fits in the available loading-screen space.", 53));
    }

    @Test
    void prefersEnglishWordBoundaries() {
        assertEquals(List.of("alpha beta", "gamma"), wrap("alpha beta gamma", 10));
    }

    @Test
    void usesCjkBoundariesWithoutStartingWithClosingPunctuation() {
        assertEquals(List.of("你好，", "世界。"), wrap("你好，世界。", 3));
    }

    @Test
    void splitsAnOversizedWordAtUnicodeCharacterBoundaries() {
        assertEquals(List.of("abcd", "efgh", "ij"), wrap("abcdefghij", 4));
    }

    @Test
    void preservesExplicitParagraphBreaks() {
        assertEquals(List.of("first", "second"), wrap("first\nsecond", 20));
    }

    private static List<String> wrap(String text, int width) {
        return TipTextWrapper.wrap(text, width,
                value -> value.codePointCount(0, value.length()));
    }
}
