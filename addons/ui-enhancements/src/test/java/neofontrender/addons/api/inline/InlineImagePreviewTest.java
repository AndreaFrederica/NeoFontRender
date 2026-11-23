package neofontrender.addons.api.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class InlineImagePreviewTest {
    @Test
    void smallImagesRemainAtTheirNaturalSize() {
        assertArrayEquals(new int[] { 64, 32 },
                InlineImagePreview.naturalSize(64, 32, 800, 600, 144));
    }

    @Test
    void oversizedImagesShrinkProportionallyToTheScreen() {
        assertArrayEquals(new int[] { 800, 400 },
                InlineImagePreview.naturalSize(2400, 1200, 800, 600, 144));
    }

    @Test
    void unknownImagesUseCompactFallbackWithoutOverflowing() {
        assertArrayEquals(new int[] { 80, 80 },
                InlineImagePreview.naturalSize(-1, -1, 100, 80, 144));
    }

    @Test
    void compactPreviewPreservesWideFormulaAspectRatio() {
        assertArrayEquals(new int[] { 144, 36 },
                InlineImagePreview.naturalSize(800, 200, 144, 144, 144));
    }
}
