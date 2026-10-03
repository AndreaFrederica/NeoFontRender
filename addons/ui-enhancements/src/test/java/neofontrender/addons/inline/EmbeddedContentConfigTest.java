package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbeddedContentConfigTest {
    @Test
    void clampsRasterCacheControls() {
        assertEquals(16, EmbeddedContentConfig.clampCacheEntries(0));
        assertEquals(192, EmbeddedContentConfig.clampCacheEntries(192));
        assertEquals(1024, EmbeddedContentConfig.clampCacheEntries(2048));
        assertEquals(4.0F, EmbeddedContentConfig.clampCacheMegapixels(0.0D));
        assertEquals(24.0F, EmbeddedContentConfig.clampCacheMegapixels(24.0D));
        assertEquals(128.0F, EmbeddedContentConfig.clampCacheMegapixels(256.0D));
    }
}
