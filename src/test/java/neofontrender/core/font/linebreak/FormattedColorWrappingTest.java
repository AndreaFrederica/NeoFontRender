package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentString;
import neofontrender.api.text.StructuredTextApi;
import neofontrender.api.text.StructuredTextRegistration;
import neofontrender.core.font.pipeline.LayoutText;
import neofontrender.core.font.pipeline.builtin.HexChatStructuredMiddleware;
import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.TextPipelinePlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormattedColorWrappingTest {
    private StructuredTextRegistration hex;
    private StructuredTextRegistration tinkers;
    private FontRenderer font;

    @BeforeEach
    void enableColorProtocols() throws ReflectiveOperationException {
        tinkers = StructuredTextApi.register(TinkersAntiqueSyntaxProvider.INSTANCE);
        hex = StructuredTextApi.register(new TextPipelinePlugin() {
            @Override public String id() { return "test:hex_wrapping"; }
            @Override public Collection<? extends StructuredTextMiddleware> structuredMiddlewares() {
                return List.of(new StructuredTextMiddleware() {
                    @Override public String id() { return HexChatStructuredMiddleware.ID; }
                    @Override public StructuredText process(StructuredText input) {
                        return HexChatStructuredMiddleware.INSTANCE.process(input);
                    }
                });
            }
        });
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        font = (FontRenderer) ((Unsafe) field.get(null)).allocateInstance(StructuredFont.class);
    }

    @AfterEach
    void resetProtocols() {
        if (hex != null) hex.close();
        if (tinkers != null) tinkers.close();
    }

    @Test
    void gradientKeepsOriginalPerCharacterColorsAcrossWidths() {
        String source = "#FFFF00-7FFF00-40E0D0-00BFFFInfinity";
        for (int width : new int[] {1, 10, 20, 35, 100}) {
            assertPreserved(source, FormattedColorWrapping.wrap(font, source, width));
        }
    }

    @Test
    void componentWrappingKeepsGradientColorsAcrossExplicitNewlines() {
        String source = "\u00a7bInput: #FF0000-0000FFABCD\nEFGHIJKL\u00a7r End";
        List<String> lines = CjkComponentLineWrapper.wrap(
                new TextComponentString(source), 20, font, false, true).stream()
                .map(component -> component.getFormattedText()).toList();
        assertPreserved(source, lines);
    }

    @Test
    void constantRgbAndDecorationsContinueAfterWrapping() {
        String source = "#1280FE\u00a7l\u00a7oABCDEFGHIJKL";
        assertPreserved(source, FormattedColorWrapping.wrap(font, source, 20));
    }

    @Test
    void tinkersRgbContinuesAfterWrapping() {
        String source = "\uE712\uE780\uE7FEABCDEFGHIJKL";
        assertPreserved(source, FormattedColorWrapping.wrap(font, source, 20));
        String prepared = FormattedColorWrapping.prepare(source);
        assertPreserved(source, FormattedColorWrapping.wrap(font, prepared, 20));
    }

    @Test
    void keepsSupplementaryCharactersWholeOnNarrowLines() {
        String source = "#FF0000-0000FFA\uD83D\uDE00B";
        assertPreserved(source, FormattedColorWrapping.wrap(font, source, 1));
    }

    @Test
    void ordinaryTextAndInvalidMarkersStayOnExistingPath() {
        assertNull(FormattedColorWrapping.wrap(font, "\u00a7cOrdinary text", 20));
        assertNull(FormattedColorWrapping.wrap(font, "literal #12ZZ34", 20));
    }

    private static void assertPreserved(String source, List<String> lines) {
        assertNotNull(lines);
        StructuredText expected = StructuredTextApi.parse(source);
        List<TextStyle> expectedStyles = new ArrayList<>();
        for (int i = 0; i < expected.plainText().length(); i++) {
            if (expected.plainText().charAt(i) != '\n') expectedStyles.add(expected.styleAt(i));
        }
        StringBuilder visible = new StringBuilder();
        List<TextStyle> actualStyles = new ArrayList<>();
        for (String line : lines) {
            StructuredText parsed = StructuredTextApi.parse(line);
            visible.append(parsed.plainText());
            for (int i = 0; i < parsed.plainText().length(); i++) actualStyles.add(parsed.styleAt(i));
        }
        assertEquals(expected.plainText().replace("\n", ""), visible.toString());
        assertEquals(expectedStyles, actualStyles);
    }

    private static final class StructuredFont extends FontRenderer {
        private StructuredFont() { super(null, null, null, false); }

        @Override public int getStringWidth(String source) {
            LayoutText parsed = LayoutText.process(source);
            return parsed.visibleText().codePointCount(0, parsed.visibleText().length()) * 5;
        }

        @Override public int sizeStringToWidth(String source, int width) {
            LayoutText parsed = LayoutText.process(source);
            int count = Math.min(width / 5,
                    parsed.visibleText().codePointCount(0, parsed.visibleText().length()));
            return parsed.rawStartBoundary(parsed.visibleText().offsetByCodePoints(0, count));
        }
    }
}
