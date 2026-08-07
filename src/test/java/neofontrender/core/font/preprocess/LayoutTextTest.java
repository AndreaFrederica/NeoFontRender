package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTextTest {

    @Test
    void removesHexMarkerFromLayoutAndRestoresItsDrawState() {
        PreprocessedText source = LegacyColorTextParser.process("A#112233B");
        LayoutText layout = LayoutText.fromPreprocessed(source);

        assertEquals("AB", layout.visibleText());
        assertFalse(layout.stateAt(0).hasRgbOverride());
        assertTrue(layout.stateAt(1).hasRgbOverride());
        assertEquals(0x112233, layout.stateAt(1).rgb());
        assertEquals(1, layout.rawStartBoundary(1));
        assertEquals(8, layout.rawEndBoundary(1));

        PreprocessedText restored = LegacyColorTextParser.process(layout.formattedDisplay(1, "B"));
        ModernText.Run run = restored.modernText().runs().get(0);
        assertEquals("B", restored.visibleText());
        assertTrue(run.hasColorOverride());
        assertEquals(0x112233, run.rgb());
    }

    @Test
    void keepsHexColorStateForEveryVisibleCharacter() {
        LayoutText layout = LayoutText.fromPreprocessed(
                LegacyColorTextParser.process("#FF0000AB"));

        assertEquals("AB", layout.visibleText());
        assertEquals(0xFF0000, layout.stateAt(0).rgb());
        assertEquals(0xFF0000, layout.stateAt(1).rgb());
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
}
