package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatexTokenParserTest {
    @Test
    void parsesInlineDisplayAndScaleSuffixes() {
        String inlineSource = "a$x^2$[scale=.5]b";
        LatexTokenParser.Match inline = LatexTokenParser.match(inlineSource, 1);
        assertEquals("x^2", inline.formula);
        assertFalse(inline.display);
        assertEquals(0.5F, inline.scale);
        assertEquals(inlineSource.length() - 1, inline.end);

        String displaySource = "$$\\frac{1}{2}$$[scale=9]";
        LatexTokenParser.Match display = LatexTokenParser.match(displaySource, 0);
        assertEquals("\\frac{1}{2}", display.formula);
        assertTrue(display.display);
        assertEquals(4.0F, display.scale);
        assertEquals(displaySource.length(), display.end);
    }

    @Test
    void leavesInvalidScaleSuffixAsPlainText() {
        LatexTokenParser.Match invalid = LatexTokenParser.match("$x$[scale=nope]", 0);
        assertEquals(3, invalid.end);
        assertEquals(1.0F, invalid.scale);

        LatexTokenParser.Match nan = LatexTokenParser.match("$x$[scale=NaN]", 0);
        assertEquals(3, nan.end);
        assertEquals(1.0F, nan.scale);
    }

    @Test
    void rejectsEscapedEmptyAndUnclosedDelimiters() {
        assertNull(LatexTokenParser.match("\\$x$", 1));
        assertNull(LatexTokenParser.match("$$", 0));
        assertNull(LatexTokenParser.match("$unclosed", 0));
    }

    @Test
    void rasterizesFormulaToTransparentArgbImage() {
        BufferedImage image = LatexRasterizer.rasterize("x^2 + y^2");
        assertEquals(BufferedImage.TYPE_INT_ARGB, image.getType());
        assertTrue(image.getWidth() > 1);
        assertTrue(image.getHeight() > 1);
        assertTrue(hasVisiblePixel(image));
    }

    @Test
    void rejectsFormulaRasterLargerThanTheAllocationBudget() {
        assertThrows(IllegalArgumentException.class,
                () -> LatexRasterizer.rasterize("\\rule{10000}{10000}"));
    }

    private static boolean hasVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }
}
