package neofontrender.core.font.preprocess;

import neofontrender.core.font.pipeline.LayoutText;
import neofontrender.core.font.pipeline.builtin.HexChatStructuredMiddleware;
import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.StructuredText;
import neofontrender.text.syntax.MinecraftLegacySyntaxProvider;
import neofontrender.text.syntax.TextSyntaxEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTextTest {
    private static final TextSyntaxEngine ENGINE = TextSyntaxEngine.builder()
            .register(TinkersAntiqueSyntaxProvider.INSTANCE)
            .register(MinecraftLegacySyntaxProvider.INSTANCE)
            .build();

    @Test
    void removesHexMarkerFromLayoutAndRestoresItsDrawState() {
        LayoutText layout = LayoutText.fromStructured(hex("A#112233B"));

        assertEquals("AB", layout.visibleText());
        assertFalse(layout.stateAt(0).hasRgbOverride());
        assertTrue(layout.stateAt(1).hasRgbOverride());
        assertEquals(0x112233, layout.stateAt(1).rgb());
        assertEquals(1, layout.rawStartBoundary(1));
        assertEquals(8, layout.rawEndBoundary(1));

        StructuredText restored = hex(layout.formattedDisplay(1, "B"));
        assertEquals("B", restored.plainText());
        assertEquals(0x112233, restored.styleAt(0).rgb());
    }

    @Test
    void keepsPerCharacterGradientColorsOutsideLayoutText() {
        LayoutText layout = LayoutText.fromStructured(hex("#FF0000-0000FFAB"));

        assertEquals("AB", layout.visibleText());
        assertEquals(0xFF0000, layout.stateAt(0).rgb());
        assertEquals(0x0000FF, layout.stateAt(1).rgb());
    }

    @Test
    void removesTinkersMarkersAndRestoresTheirRgbState() {
        String marker = tinkersRgb(0x12, 0x80, 0xFE);
        LayoutText layout = LayoutText.fromStructured(ENGINE.parse(marker + "字"));

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
        TextSyntaxEngine minecraftOnly = TextSyntaxEngine.builder()
                .register(MinecraftLegacySyntaxProvider.INSTANCE).build();
        LayoutText layout = LayoutText.fromStructured(minecraftOnly.parse(raw));

        assertFalse(layout.transformed());
        assertEquals(raw, layout.visibleText());
        assertFalse(layout.stateAt(0).hasRgbOverride());
    }

    @Test
    void stripsLegacyFormattingButRetainsItsPerCharacterState() {
        LayoutText layout = LayoutText.fromStructured(ENGINE.parse("\u00A7l粗\u00A7r常"));

        assertEquals("粗常", layout.visibleText());
        assertTrue(layout.stateAt(0).bold());
        assertFalse(layout.stateAt(1).bold());
        assertEquals("\u00A7l粗", layout.formattedDisplay(0, "粗"));
        assertEquals("常", layout.formattedDisplay(1, "常"));
    }

    private static String tinkersRgb(int red, int green, int blue) {
        return new String(new char[]{
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + red),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + green),
                (char) (TinkersAntiqueSyntaxProvider.MARKER_START + blue)
        });
    }

    private static StructuredText hex(String source) {
        return HexChatStructuredMiddleware.INSTANCE.process(ENGINE.parse(source));
    }
}
