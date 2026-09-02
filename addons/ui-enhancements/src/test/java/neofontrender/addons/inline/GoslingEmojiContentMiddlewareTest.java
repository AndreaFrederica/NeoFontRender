package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class GoslingEmojiContentMiddlewareTest {
    @Test
    void rawUnicodeEmojiFallsThroughToTheFontPipeline() {
        assertNull(GoslingEmojiContentMiddleware.matchEnabled("A😀️中", 1));
    }
}
