package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteImageGlyphSizingTest {
    @Test
    void largeImagesPreserveAspectRatioInsideInlineBounds() {
        assertArrayEquals(new int[] { 96, 48 },
                RemoteImageGlyph.fit(4000, 2000, 128, 48, 10));
    }

    @Test
    void ordinaryImagesCanIncreaseTheTextLineHeight() {
        int[] size = RemoteImageGlyph.fit(32, 24, 128, 48, 10);
        assertArrayEquals(new int[] { 32, 24 }, size);
        assertTrue(size[1] > 10);
    }
}
