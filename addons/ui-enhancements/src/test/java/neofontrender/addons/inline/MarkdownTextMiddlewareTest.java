package neofontrender.addons.inline;

import neofontrender.api.text.pipeline.InlineContentMatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarkdownTextMiddlewareTest {
    private final MarkdownTextMiddleware middleware = new MarkdownTextMiddleware();

    @Test
    void recognizesSupportedInlineConstructs() {
        assertMatch("**bold**", 8, "**bold**");
        assertMatch("_italic_", 8, "_italic_");
        assertMatch("~~strike~~", 10, "~~strike~~");
        assertMatch("`code`", 6, "`code`");

        InlineContentMatch link = middleware.match("[NFR](https://example.invalid)", 0);
        assertNotNull(link);
        assertEquals(30, link.end());
        assertEquals("NFR - https://example.invalid", link.content().description());
    }

    @Test
    void ignoresEscapedMalformedAndMultilineMarkup() {
        assertNull(middleware.match("\\**literal**", 1));
        assertNull(middleware.match("** missing**", 0));
        assertNull(middleware.match("**unterminated", 0));
        assertNull(middleware.match("`line\nbreak`", 0));
        assertNull(middleware.match("[](url)", 0));
    }

    @Test
    void capsWorkForUnterminatedInput() {
        String source = "**" + "a".repeat(700) + "**";
        assertNull(middleware.match(source, 0));
    }

    private void assertMatch(String source, int expectedEnd, String description) {
        InlineContentMatch match = middleware.match(source, 0);
        assertNotNull(match);
        assertEquals(expectedEnd, match.end());
        assertEquals(description, match.content().description());
    }
}
