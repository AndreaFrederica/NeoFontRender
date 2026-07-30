package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaFontTexturePolicyTest {
    @Test
    void keepsOnlyTheSgaBitmapOutOfModernLinearFiltering() {
        assertTrue(VanillaFontTexturePolicy.forceNearest(
                "textures/font/ascii_sga.png"));
        assertTrue(VanillaFontTexturePolicy.forceNearest(
                "font/ascii_sga.png"));
        assertFalse(VanillaFontTexturePolicy.forceNearest(
                "textures/font/ascii.png"));
        assertFalse(VanillaFontTexturePolicy.forceNearest(
                "textures/font/unicode_page_00.png"));
    }
}
