package neofontrender.lab;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("golden")
class StandaloneAwtRendererGoldenTest {
    @Test
    void formattedRasterIsDeterministicAndNonBlank() {
        StructuredText text = StandardSyntaxEngines.minecraftWithBrilliantDefaults()
                .parse("\u00A7g\u00A7lGold\u00A7r \u4e2d\u6587 \u00A7nunderline");
        StandaloneAwtRenderer renderer = new StandaloneAwtRenderer();
        StandaloneAwtRenderer.Settings settings = StandaloneAwtRenderer.Settings.defaults();

        BufferedImage first = renderer.render(text, settings).image;
        BufferedImage second = renderer.render(text, settings).image;

        assertEquals(pixelHash(first), pixelHash(second));
        assertNotEquals(0L, alphaHash(first));
    }

    @Test
    void obfuscatedFramesChangePixelsWithoutChangingCanvas() {
        StructuredText text = StandardSyntaxEngines.minecraftWithBrilliantDefaults()
                .parse("\u00A7kMinecraft");
        StandaloneAwtRenderer renderer = new StandaloneAwtRenderer();
        StandaloneAwtRenderer.Settings defaults = StandaloneAwtRenderer.Settings.defaults();
        StandaloneAwtRenderer.Settings next = new StandaloneAwtRenderer.Settings(
                defaults.width, defaults.height, defaults.fontSize, defaults.padding,
                defaults.lineGap, 17L, defaults.fontFamily, defaults.background,
                defaults.foreground, defaults.shadow);

        BufferedImage first = renderer.render(text, defaults).image;
        BufferedImage second = renderer.render(text, next).image;

        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(first.getHeight(), second.getHeight());
        assertNotEquals(pixelHash(first), pixelHash(second));
        assertTrue(text.animated());
    }

    private static long pixelHash(BufferedImage image) {
        long hash = 1125899906842597L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) hash = 31L * hash + image.getRGB(x, y);
        }
        return hash;
    }

    private static long alphaHash(BufferedImage image) {
        long hash = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) hash |= image.getRGB(x, y) >>> 24;
        }
        return hash;
    }
}
