package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTextTest {

    @Test
    void removesHexMarkerFromLayoutAndRestoresItsDrawState() {
        PreprocessedText source = LegacyColorTextParser.process(
                "A#112233B", false, true, true);
        LayoutText layout = LayoutText.fromPreprocessed(source);

        assertEquals("AB", layout.visibleText());
        assertFalse(layout.stateAt(0).hasRgbOverride());
        assertTrue(layout.stateAt(1).hasRgbOverride());
        assertEquals(0x112233, layout.stateAt(1).rgb());
        assertEquals(1, layout.rawStartBoundary(1));
        assertEquals(8, layout.rawEndBoundary(1));

        PreprocessedText restored = LegacyColorTextParser.process(
                layout.formattedDisplay(1, "B"), true, true, true);
        ModernText.Run run = restored.modernText().runs().get(0);
        assertEquals("B", restored.visibleText());
        assertTrue(run.hasColorOverride());
        assertEquals(0x112233, run.rgb());
    }

    @Test
    void keepsPerCharacterGradientColorsOutsideLayoutText() {
        LayoutText layout = LayoutText.fromPreprocessed(LegacyColorTextParser.process(
                "#FF0000-0000FFAB", false, true, true));

        assertEquals("AB", layout.visibleText());
        assertEquals(0xFF0000, layout.stateAt(0).rgb());
        assertEquals(0x0000FF, layout.stateAt(1).rgb());
    }

    @Test
    void removesTinkersMarkersAndRestoresTheirRgbState() {
        String marker = tinkersRgb(0x12, 0x80, 0xFE);
        LayoutText layout = LayoutText.fromPreprocessed(
                LegacyColorTextParser.process(marker + "字", true, false, true));

        assertEquals("字", layout.visibleText());
        assertTrue(layout.stateAt(0).hasRgbOverride());
        assertEquals(0x1280FE, layout.stateAt(0).rgb());
        assertEquals(0, layout.rawStartBoundary(0));
        assertEquals(3, layout.rawEndBoundary(0));
        assertEquals(4, layout.rawEndBoundary(1));
    }

    @Test
    void leavesTinkersCharactersInLayoutWhenCompatibilityDidNotDecodeThem() {
        String raw = tinkersRgb(1, 2, 3) + "字";
        LayoutText layout = LayoutText.fromPreprocessed(PreprocessedText.unchanged(raw));

        assertFalse(layout.transformed());
        assertEquals(raw, layout.visibleText());
        assertFalse(layout.stateAt(0).hasRgbOverride());
    }

    @Test
    void stripsLegacyFormattingButRetainsItsPerCharacterState() {
        LayoutText layout = LayoutText.fromPreprocessed(
                PreprocessedText.unchanged("\u00A7l粗\u00A7r常"));

        assertEquals("粗常", layout.visibleText());
        assertTrue(layout.stateAt(0).bold());
        assertFalse(layout.stateAt(1).bold());
        assertEquals("\u00A7l粗", layout.formattedDisplay(0, "粗"));
        assertEquals("常", layout.formattedDisplay(1, "常"));
    }

    private static String tinkersRgb(int red, int green, int blue) {
        return new String(new char[]{
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + red),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + green),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + blue)
        });
    }
}
