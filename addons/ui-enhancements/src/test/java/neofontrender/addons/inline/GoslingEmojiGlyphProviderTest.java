package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class GoslingEmojiGlyphProviderTest {
    @Test
    void rawUnicodeEmojiFallsThroughToTheFontPipeline() {
        assertNull(GoslingEmojiGlyphProvider.matchEnabled("A😀️中", 1));
    }
}
