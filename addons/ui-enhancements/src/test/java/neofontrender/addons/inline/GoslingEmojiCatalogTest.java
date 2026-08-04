package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GoslingEmojiCatalogTest {
    @Test void bundledDictionaryProvidesStableAliasSuggestions() {
        List<String> suggestions = GoslingEmojiCatalog.INSTANCE.suggestions("grinni", 10);
        assertFalse(suggestions.isEmpty());
        assertEquals(":grinning:", suggestions.get(0));
        assertEquals("cdnjs.cloudflare.com",
                GoslingEmojiCatalog.INSTANCE.alias("grinning").uri.getHost());
    }
}
