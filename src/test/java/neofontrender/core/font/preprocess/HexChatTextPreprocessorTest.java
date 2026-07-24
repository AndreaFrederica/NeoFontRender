package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexChatTextPreprocessorTest {

    @Test
    void decodesHexMarkersIntoModernRgbRuns() {
        PreprocessedText result =
                HexChatTextPreprocessor.INSTANCE.process("base#12aBf0color");

        assertTrue(result.transformed());
        assertEquals("basecolor", result.visibleText());
        List<ModernText.Run> runs = result.modernText().runs();
        assertEquals(2, runs.size());
        assertFalse(runs.get(0).hasColorOverride());
        assertEquals("base", runs.get(0).text());
        assertTrue(runs.get(1).hasColorOverride());
        assertEquals(0x12ABF0, runs.get(1).rgb());
        assertEquals("color", runs.get(1).text());
    }

    @Test
    void preservesStylesAndRestoresCallerColorOnReset() {
        PreprocessedText result = HexChatTextPreprocessor.INSTANCE.process(
                "\u00A7lbefore#010203after\u00A7rbase");

        List<ModernText.Run> runs = result.modernText().runs();
        assertEquals(3, runs.size());
        assertEquals("\u00A7lbefore", runs.get(0).text());
        assertEquals("\u00A7lafter", runs.get(1).text());
        assertEquals(0x010203, runs.get(1).rgb());
        assertFalse(runs.get(2).hasColorOverride());
        assertEquals("\u00A7rbase", runs.get(2).text());
    }

    @Test
    void mapsVisibleBoundariesAroundRemovedHexMarker() {
        PreprocessedText result =
                HexChatTextPreprocessor.INSTANCE.process("A#112233B");

        assertEquals("AB", result.visibleText());
        assertEquals(1, result.rawStartForVisibleBoundary(1));
        assertEquals(8, result.rawEndForVisibleBoundary(1));
        assertEquals(9, result.rawEndForVisibleBoundary(2));
    }

    @Test
    void combinesPuaAndHexProtocolsInOnePass() {
        String pua = new String(new char[]{
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + 0xAA),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + 0xBB),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + 0xCC)
        });
        PreprocessedText result = LegacyColorTextParser.process(
                pua + "pua#00FF00hex", true, true);

        List<ModernText.Run> runs = result.modernText().runs();
        assertEquals(2, runs.size());
        assertEquals(0xAABBCC, runs.get(0).rgb());
        assertEquals("pua", runs.get(0).text());
        assertEquals(0x00FF00, runs.get(1).rgb());
        assertEquals("hex", runs.get(1).text());
    }

    @Test
    void leavesInvalidHexTextVisible() {
        PreprocessedText result =
                HexChatTextPreprocessor.INSTANCE.process("literal #12ZZ34");

        assertFalse(result.transformed());
        assertEquals("literal #12ZZ34", result.visibleText());
    }
}
