package neofontrender.core.font.preprocess;

import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.StructuredText;
import neofontrender.text.syntax.MinecraftLegacySyntaxProvider;
import neofontrender.text.syntax.TextSyntaxEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinkersAntiqueTextPreprocessorTest {
    private static final TextSyntaxEngine ENGINE = TextSyntaxEngine.builder()
            .register(TinkersAntiqueSyntaxProvider.INSTANCE)
            .register(MinecraftLegacySyntaxProvider.INSTANCE)
            .build();

    @Test
    void decodesRgbMarkersIntoStructuredStyles() {
        StructuredText result = parse("base" + rgb(0x12, 0x80, 0xFE) + "color");

        assertEquals("basecolor", result.plainText());
        assertFalse(result.styleAt(0).hasColorOverride());
        assertEquals(0x1280FE, result.styleAt(4).rgb());
        assertTrue(result.appliedSyntaxProviderIds().contains("neofontrender:tinkers_rgb"));
    }

    @Test
    void carriesMinecraftStylesAcrossColorBoundary() {
        StructuredText result = parse("\u00A7lBold" + rgb(1, 2, 3) + "Still bold");

        assertTrue(result.styleAt(4).bold());
        assertEquals(0x010203, result.styleAt(4).rgb());
    }

    @Test
    void mapsPlainBoundariesBackToRawMarkers() {
        String marker = rgb(10, 20, 30);
        StructuredText result = parse("A" + marker + "B");

        assertEquals("AB", result.plainText());
        assertEquals(1, result.sourceMap().sourceStart(1));
        assertEquals(4, result.sourceMap().sourceEnd(1));
        assertEquals(5, result.sourceMap().sourceEnd(2));
    }

    @Test
    void hidesPartialMarkerSequenceAndResetsFollowingTextToWhite() {
        String partial = new String(new char[]{
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + 5),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + 6)
        });
        StructuredText result = parse(partial + "text");

        assertEquals("text", result.plainText());
        assertEquals(0xFFFFFF, result.styleAt(0).rgb());
    }

    @Test
    void vanillaResetReturnsFromPuaColorToCallerBaseColor() {
        StructuredText result = parse(rgb(0xAA, 0xBB, 0xCC) + "custom\u00A7rbase");

        assertEquals(0xAABBCC, result.styleAt(0).rgb());
        assertFalse(result.styleAt("custom".length()).hasColorOverride());
    }

    @Test
    void obfuscatedMinecraftStyleMarksTextAnimated() {
        StructuredText result = parse("\u00A7r\u00A70k \u00A7kMinecraft");

        assertEquals("k Minecraft", result.plainText());
        assertFalse(result.styleAt(0).obfuscated());
        assertTrue(result.styleAt(2).obfuscated());
        assertTrue(result.animated());
    }

    private static StructuredText parse(String value) {
        return ENGINE.parse(value);
    }

    private static String rgb(int red, int green, int blue) {
        return new String(new char[]{
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + red),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + green),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + blue)
        });
    }
}
