package neofontrender.core.font.support;

import neofontrender.api.text.TextVisualBounds;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class FontPixelUtilsTest {
    @Test void excludesTransparentPaddingAndMapsOversampledPixelsBackToGuiCoordinates() {
        int[] pixels = new int[12 * 8];
        Arrays.fill(pixels, FontPixelUtils.TRANSPARENT_WHITE);
        pixels[2 * 12 + 3] = 0x01FFFFFF; // faint antialiasing coverage still belongs to the glyph
        pixels[5 * 12 + 6] = 0xFF000000; // black ink must not be mistaken for transparency
        TextVisualBounds bounds = FontPixelUtils.visibleBounds(pixels, 12, 8, -1, 2, 5, 6);
        assertEquals(0.5F, bounds.left);
        assertEquals(3, bounds.top);
        assertEquals(2.5F, bounds.right);
        assertEquals(5, bounds.bottom);
        assertEquals(2, bounds.width());
        Arrays.fill(pixels, FontPixelUtils.TRANSPARENT_WHITE);
        assertTrue(FontPixelUtils.visibleBounds(pixels, 12, 8, -1, 2, 5, 6).isEmpty());
    }
}
