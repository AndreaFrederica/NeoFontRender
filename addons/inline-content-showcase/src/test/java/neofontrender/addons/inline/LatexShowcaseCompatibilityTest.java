package neofontrender.addons.inline;

import neofontrender.addons.inlinecontent.ShowcaseSamples;
import neofontrender.addons.inlinecontent.CrownEtherSample;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatexShowcaseCompatibilityTest {
    @Test
    void everyMathematicsSampleContainsAParsableRenderableFormula() {
        for (ShowcaseSamples.Sample sample : ShowcaseSamples.MATHEMATICS) {
            int start = firstUnescapedDollar(sample.rendered());
            assertTrue(start >= 0, sample.name() + " has no formula token");
            LatexTokenParser.Match match = LatexTokenParser.match(sample.rendered(), start);
            assertNotNull(match, sample.name() + " is not accepted by the LaTeX token parser");
            BufferedImage image = LatexRasterizer.rasterize(match.formula);
            assertTrue(hasVisiblePixels(image), sample.name() + " produced an empty raster");
        }
    }

    @Test
    void crownEtherTooltipFormulaAndMolarMassAreRenderable() {
        assertRenderable(CrownEtherSample.FORMULA_TOKEN);
        assertRenderable(CrownEtherSample.MOLAR_MASS_TOKEN);
    }

    private static void assertRenderable(String token) {
        LatexTokenParser.Match match = LatexTokenParser.match(token, 0);
        assertNotNull(match);
        assertTrue(hasVisiblePixels(LatexRasterizer.rasterize(match.formula)));
    }

    private static int firstUnescapedDollar(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != '$') continue;
            int slashes = 0;
            for (int previous = index - 1; previous >= 0 && value.charAt(previous) == '\\'; previous--) {
                slashes++;
            }
            if ((slashes & 1) == 0) return index;
        }
        return -1;
    }

    private static boolean hasVisiblePixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }
}
