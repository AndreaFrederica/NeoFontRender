package neofontrender.lab;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("golden")
class StandaloneCosmicRendererGoldenTest {
    @Test
    void nativeBackendRendersAllStructuredStylesWithoutMinecraft() throws Exception {
        StructuredText text = StandardSyntaxEngines.minecraftWithBrilliantDefaults().parse(
                "\u00A7g\u00A7lGold\u00A7n underline \u00A7mstrike \u00A7oitalic\u00A7r "
                        + "\u4e2d\u6587 العربية \uD83D\uDE00\u2764\uFE0F");
        try (StandaloneCosmicRenderer renderer = new StandaloneCosmicRenderer()) {
            assertTrue(renderer.isAvailable(), renderer.status());
            StandaloneCosmicRenderer.Result result = renderer.render(text,
                    StandaloneAwtRenderer.Settings.defaults());

            assertTrue(result.available, result.status);
            assertTrue(nonBackgroundPixels(result.image) > 100);
            assertTrue(result.trace.stream().anyMatch(line -> line.contains("cosmic-native")));
            assertTrue(result.trace.stream().anyMatch(line -> line.contains("brilliant-preview")));
            assertTrue(hasChromaticGlyphPixel(result.image));
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName("net.minecraft.client.Minecraft"));
        }
    }

    @Test
    void nativeObfuscationChangesAcrossFrames() {
        StructuredText text = StandardSyntaxEngines.minecraftWithBrilliantDefaults()
                .parse("\u00A7kMinecraft");
        StandaloneAwtRenderer.Settings defaults = StandaloneAwtRenderer.Settings.defaults();
        StandaloneAwtRenderer.Settings next = new StandaloneAwtRenderer.Settings(
                defaults.width, defaults.height, defaults.fontSize, defaults.padding,
                defaults.lineGap, 19L, defaults.fontFamily, defaults.background,
                defaults.foreground, defaults.shadow);
        try (StandaloneCosmicRenderer renderer = new StandaloneCosmicRenderer()) {
            BufferedImage first = renderer.render(text, defaults).image;
            BufferedImage second = renderer.render(text, next).image;
            assertNotEquals(pixelHash(first), pixelHash(second));
        }
    }

    private static int nonBackgroundPixels(BufferedImage image) {
        int background = image.getRGB(0, 0);
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != background) count++;
            }
        }
        return count;
    }

    private static boolean hasChromaticGlyphPixel(BufferedImage image) {
        int background = image.getRGB(0, 0);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if (pixel == background || (pixel >>> 24) == 0) continue;
                int red = pixel >> 16 & 0xFF;
                int green = pixel >> 8 & 0xFF;
                int blue = pixel & 0xFF;
                if (red != green || green != blue) return true;
            }
        }
        return false;
    }

    private static long pixelHash(BufferedImage image) {
        long hash = 1125899906842597L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) hash = 31L * hash + image.getRGB(x, y);
        }
        return hash;
    }
}
