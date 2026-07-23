package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinkersAntiqueTextPreprocessorTest {

    @Test
    void decodesRgbMarkersIntoColoredRuns() {
        String marker = rgb(0x12, 0x80, 0xFE);
        PreprocessedText result =
                TinkersAntiqueTextPreprocessor.INSTANCE.process("base" + marker + "color");

        assertTrue(result.transformed());
        assertEquals("basecolor", result.visibleText());
        List<ModernText.Run> runs = result.modernText().runs();
        assertEquals(2, runs.size());
        assertFalse(runs.get(0).hasColorOverride());
        assertEquals("base", runs.get(0).text());
        assertTrue(runs.get(1).hasColorOverride());
        assertEquals(0x1280FE, runs.get(1).rgb());
        assertEquals("color", runs.get(1).text());
    }

    @Test
    void carriesMinecraftStylesAcrossColorBoundary() {
        PreprocessedText result = TinkersAntiqueTextPreprocessor.INSTANCE.process(
                "\u00A7lBold" + rgb(1, 2, 3) + "Still bold");

        List<ModernText.Run> runs = result.modernText().runs();
        assertEquals(2, runs.size());
        assertEquals("\u00A7lBold", runs.get(0).text());
        assertEquals("\u00A7lStill bold", runs.get(1).text());
        assertEquals(0x010203, runs.get(1).rgb());
    }

    @Test
    void mapsVisibleBoundariesBackToRawMarkers() {
        String marker = rgb(10, 20, 30);
        PreprocessedText result =
                TinkersAntiqueTextPreprocessor.INSTANCE.process("A" + marker + "B");

        assertEquals("AB", result.visibleText());
        assertEquals(1, result.rawStartForVisibleBoundary(1));
        assertEquals(4, result.rawEndForVisibleBoundary(1));
        assertEquals(4, result.rawStartForVisibleBoundary(1) + marker.length());
        assertEquals(5, result.rawEndForVisibleBoundary(2));
    }

    @Test
    void hidesPartialMarkerSequenceAndResetsFollowingTextToWhite() {
        String partial = new String(new char[]{
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + 5),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + 6)
        });
        PreprocessedText result =
                TinkersAntiqueTextPreprocessor.INSTANCE.process(partial + "text");

        assertEquals("text", result.visibleText());
        ModernText.Run run = result.modernText().runs().get(0);
        assertTrue(run.hasColorOverride());
        assertEquals(0xFFFFFF, run.rgb());
    }

    @Test
    void vanillaResetReturnsFromPuaColorToCallerBaseColor() {
        PreprocessedText result = TinkersAntiqueTextPreprocessor.INSTANCE.process(
                rgb(0xAA, 0xBB, 0xCC) + "custom\u00A7rbase");

        List<ModernText.Run> runs = result.modernText().runs();
        assertEquals(2, runs.size());
        assertTrue(runs.get(0).hasColorOverride());
        assertEquals("custom", runs.get(0).text());
        assertFalse(runs.get(1).hasColorOverride());
        assertEquals("\u00A7rbase", runs.get(1).text());
    }

    private static String rgb(int red, int green, int blue) {
        return new String(new char[]{
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + red),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + green),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + blue)
        });
    }
}
