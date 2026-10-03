package neofontrender.core.font.pipeline.builtin;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.MinecraftLegacySyntaxProvider;
import neofontrender.text.syntax.TextSyntaxEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexChatTextPreprocessorTest {
    private static final TextSyntaxEngine ENGINE = TextSyntaxEngine.builder()
            .register(TinkersAntiqueSyntaxProvider.INSTANCE)
            .register(MinecraftLegacySyntaxProvider.INSTANCE)
            .build();

    @Test
    void decodesHexMarkerIntoStructuredColor() {
        StructuredText result = process("base#12aBf0color", true);

        assertEquals("basecolor", result.plainText());
        assertFalse(result.styleAt(0).hasColorOverride());
        assertEquals(0x12ABF0, result.styleAt(4).rgb());
        assertTrue(result.appliedMiddlewareIds().contains("neofontrender:hex_chat"));
    }

    @Test
    void resetModeClearsStylesAtMarker() {
        StructuredText result = process("\u00A7lbefore#010203after", true);

        assertTrue(result.styleAt(0).bold());
        assertFalse(result.styleAt("before".length()).bold());
        assertEquals(0x010203, result.styleAt("before".length()).rgb());
    }

    @Test
    void retainModeCarriesStylesAtMarker() {
        StructuredText result = process("\u00A7lbefore#010203after", false);

        assertTrue(result.styleAt("before".length()).bold());
        assertEquals(0x010203, result.styleAt("before".length()).rgb());
    }

    @Test
    void mapsPlainBoundariesAroundRemovedHexMarker() {
        StructuredText result = process("A#112233B", true);

        assertEquals("AB", result.plainText());
        assertEquals(1, result.sourceMap().sourceStart(1));
        assertEquals(8, result.sourceMap().sourceEnd(1));
        assertEquals(9, result.sourceMap().sourceEnd(2));
    }

    @Test
    void combinesPuaAndHexProtocols() {
        StructuredText result = process(rgb(0xAA, 0xBB, 0xCC) + "pua#00FF00hex", true);

        assertEquals("puahex", result.plainText());
        assertEquals(0xAABBCC, result.styleAt(0).rgb());
        assertEquals(0x00FF00, result.styleAt(3).rgb());
    }

    @Test
    void leavesInvalidHexTextVisible() {
        StructuredText parsed = ENGINE.parse("literal #12ZZ34");
        assertSame(parsed, HexChatStructuredMiddleware.process(parsed, true));
    }

    @Test
    void interpolatesMultiStopGradientByCodePoint() {
        StructuredText result = process("#FF0000-00FF00-0000FFABCDE", true);

        assertEquals("ABCDE", result.plainText());
        assertEquals(0xFF0000, result.styleAt(0).rgb());
        assertEquals(0x808000, result.styleAt(1).rgb());
        assertEquals(0x00FF00, result.styleAt(2).rgb());
        assertEquals(0x008080, result.styleAt(3).rgb());
        assertEquals(0x0000FF, result.styleAt(4).rgb());
    }

    @Test
    void minecraftColorAndResetTerminateHexRange() {
        StructuredText color = process("#FF0000A\u00A7aB", true);
        StructuredText reset = process("#FF0000A\u00A7rB", true);

        assertEquals(0xFF0000, color.styleAt(0).rgb());
        assertEquals(0x55FF55, color.styleAt(1).rgb());
        assertEquals(0xFF0000, reset.styleAt(0).rgb());
        assertFalse(reset.styleAt(1).hasColorOverride());
    }

    private static StructuredText process(String value, boolean resetStyles) {
        return HexChatStructuredMiddleware.process(ENGINE.parse(value), resetStyles);
    }

    private static String rgb(int red, int green, int blue) {
        return new String(new char[]{
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + red),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + green),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + blue)
        });
    }
}
