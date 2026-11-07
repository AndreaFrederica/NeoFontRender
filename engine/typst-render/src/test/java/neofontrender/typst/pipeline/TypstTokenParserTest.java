package neofontrender.typst.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypstTokenParserTest {
    @Test
    void matchesBoundedTypstToken() {
        TypstTokenParser.Match match = TypstTokenParser.match(
                "x <typst:$x^2$> y", 2, 4096);
        assertNotNull(match);
        assertEquals("$x^2$", match.source);
        assertEquals(2, match.start);
        assertEquals(15, match.end);
    }

    @Test
    void ignoresEscapedEmptyAndOversizedTokens() {
        assertNull(TypstTokenParser.match("\\<typst:x>", 1, 4096));
        assertNull(TypstTokenParser.match("<typst:>", 0, 4096));
        assertNull(TypstTokenParser.match("<latex:x>", 0, 4096));
        assertNull(TypstTokenParser.match("<typst:too-long>", 0, 3));
    }

    @Test
    void allowsGreaterThanInsideLongForm() {
        TypstTokenParser.Match match = TypstTokenParser.match(
                "<typst:#let x = 1 > 0</typst>", 0, 4096);
        assertNotNull(match);
        assertTrue(match.source.contains(">"));
    }

    @Test
    void shortTokensDoNotCaptureALaterLongFormClosingTag() {
        String source = "<typst:$a$>\n<typst:$b$>\n<typst:#let x = 1 > 0</typst>";

        TypstTokenParser.Match first = TypstTokenParser.match(source, 0, 4096);
        assertNotNull(first);
        int secondStart = source.indexOf("<typst:", first.end);
        TypstTokenParser.Match second = TypstTokenParser.match(source, secondStart, 4096);
        assertNotNull(second);
        int thirdStart = source.indexOf("<typst:", second.end);
        TypstTokenParser.Match third = TypstTokenParser.match(source, thirdStart, 4096);
        assertNotNull(third);

        assertEquals("$a$", first.source);
        assertEquals("$b$", second.source);
        assertEquals("#let x = 1 > 0", third.source);
    }
}
