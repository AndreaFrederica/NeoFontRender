package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
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

/**
 * Regression coverage for protocol-aware wrapping: a color token must never be split and
 * every returned line must be independently parseable with the original gradient styles.
 */
class ProtocolTextWrapperTest {
    private static final String TOOLTIP_LINE =
            "\u00a7b\u8f93\u5165\u8f93\u51fa\u901f\u5ea6:"
                    + "#FF6347-FFA54F-FFFF00-7FFF00-40E0D0-00BFFFInfinity";
    private static final String PASTED_SIGN =
            "\u00a7b\u8f93\u5165DDDD\u8f93\u51fa\u901f\u5ea6:"
                    + "#FF6347-FFA54F-FFFF00-7FFF00-40E0D0-00BFFFInfinity"
                    + "\u4e86\u4e5f\u6ca1\u88ab\u5207\u65ad";
    private static final String REPEATED_STOPS =
            "b\u8f93\u5165DDDDD\u8f93\u51fa\u901f\u5ea6:#FF6347-FFA54F-FFFF00-7FFF00-40E0D0-00BFFF"
                    + "-FFA54F-FFFF00-7FFF00-40E0D0-00BFFF-FFA54F-FFFF00-7FFF00-40E0D0-00BFFF"
                    + "-FFA54F-FFFF00-7FFF00-40E0D0-00BFFF-FFA54F-FFFF00-7FFF00-40E0D0-00BFFFInfinity";

    private StructuredTextRegistration hex;
    private StructuredTextRegistration tinkers;
    private FontRenderer font;

    @BeforeEach
    void enableColorProtocols() throws ReflectiveOperationException {
        tinkers = StructuredTextApi.register(TinkersAntiqueSyntaxProvider.INSTANCE);
        hex = StructuredTextApi.register(new TextPipelinePlugin() {
            @Override public String id() { return "test:hex_protocol_wrapping"; }
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
    void tooltipGradientNeverSplitsAcrossEveryWidth() {
        for (int width = 10; width <= 200; width += 5) {
            List<String> lines = ProtocolTextWrapper.wrap(font, TOOLTIP_LINE, width, true);
            assertPreserved(TOOLTIP_LINE, lines);
            assertEachLineFits(lines, width);
            assertNoMarkupLeaked(lines);
        }
    }

    @Test
    void signPasteDoesNotStoreLiteralMarkup() {
        List<String> lines = SignPasteLayout.layout(font, "", PASTED_SIGN, 90,
                SignPasteLayout.LINE_COUNT);
        assertFalse(lines.isEmpty());
        assertTrue(lines.size() <= SignPasteLayout.LINE_COUNT);
        // Pasted section signs are filtered by the sign editor, so compare against the
        // clipboard without its leading colour code.
        String expected = PASTED_SIGN.substring(2);
        List<String> visible = new ArrayList<>();
        for (String line : lines) visible.add(LayoutText.process(line).visibleText());
        assertEquals(LayoutText.process(expected).visibleText(), String.join("", visible));
        assertEachLineFits(lines, SignPasteLayout.LINE_WIDTH);
        assertNoMarkupLeaked(lines);
        assertPreserved(expected, lines);
    }

    @Test
    void repeatedGradientStopsStayIndependentlyParseable() {
        for (int width : new int[] {40, 90, 140, 240}) {
            List<String> lines = ProtocolTextWrapper.wrap(font, REPEATED_STOPS, width, true);
            assertPreserved(REPEATED_STOPS, lines);
            assertEachLineFits(lines, width);
            assertNoMarkupLeaked(lines);
        }
    }

    @Test
    void explicitNewlinesAndLeadingSpaceRemovalMatchVanillaContract() {
        List<String> lines = ProtocolTextWrapper.wrap(font, "aaaa bbbb\ncccc", 25, true);
        assertEquals("aaaa", lines.get(0));
        assertEquals("bbbb", lines.get(1));
        assertEquals("cccc", lines.get(2));
    }

    @Test
    void breaksAfterTrailingPunctuationInsteadOfInsideTheWord() {
        // "输入输出速度:" is 35px and "Infinity" is 40px with the 5px test font.
        List<String> lines = ProtocolTextWrapper.wrap(font, TOOLTIP_LINE, 60, true);
        assertEquals(List.of("输入输出速度:", "Infinity"), visibleLines(lines));
        assertPreserved(TOOLTIP_LINE, lines);
        assertEachLineFits(lines, 60);
    }

    @Test
    void keepsTheWholeLineWhenItFits() {
        List<String> lines = ProtocolTextWrapper.wrap(font, TOOLTIP_LINE, 80, true);
        assertEquals(List.of("输入输出速度:Infinity"), visibleLines(lines));
        assertPreserved(TOOLTIP_LINE, lines);
    }

    @Test
    void everyScopeIsEnabledByDefault() {
        for (ProtocolTextWrapper.Scope scope : ProtocolTextWrapper.Scope.values()) {
            assertTrue(ProtocolTextWrapper.enabled(scope), "scope " + scope + " should default to enabled");
        }
    }

    private static List<String> visibleLines(List<String> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) result.add(LayoutText.process(line).visibleText());
        return result;
    }

    private void assertEachLineFits(List<String> lines, int width) {
        for (String line : lines) {
            LayoutText parsed = LayoutText.process(line);
            int codePoints = parsed.visibleText().codePointCount(0, parsed.visibleText().length());
            assertTrue(font.getStringWidth(line) <= width || codePoints <= 1,
                    "line wider than " + width + ": '" + line + "' = " + font.getStringWidth(line));
        }
    }

    private void assertNoMarkupLeaked(List<String> lines) {
        for (String line : lines) {
            String visible = LayoutText.process(line).visibleText();
            assertFalse(visible.matches(".*[0-9A-Fa-f]{6}.*"),
                    "color markup leaked into visible text: '" + visible + "'");
            assertFalse(visible.endsWith("#"), "dangling marker at line end: '" + visible + "'");
        }
    }

    private static void assertPreserved(String source, List<String> lines) {
        StructuredText expected = StructuredTextApi.parse(source);
        List<TextStyle> expectedStyles = styles(expected);
        StringBuilder visible = new StringBuilder();
        List<TextStyle> actualStyles = new ArrayList<>();
        for (String line : lines) {
            StructuredText parsed = StructuredTextApi.parse(line);
            visible.append(parsed.plainText());
            actualStyles.addAll(styles(parsed));
        }
        assertEquals(expected.plainText().replace("\n", ""), visible.toString());
        assertEquals(expectedStyles, actualStyles);
    }

    private static List<TextStyle> styles(StructuredText text) {
        List<TextStyle> result = new ArrayList<>();
        for (int i = 0; i < text.plainText().length(); i++) {
            if (text.plainText().charAt(i) != '\n') result.add(text.styleAt(i));
        }
        return result;
    }

    /** Five pixels per visible code point, parsed through the same protocol pipeline. */
    private static final class StructuredFont extends FontRenderer {
        private StructuredFont() { super(null, null, null, false); }

        @Override public int getStringWidth(String source) {
            LayoutText parsed = LayoutText.process(source);
            return parsed.visibleText().codePointCount(0, parsed.visibleText().length()) * 5;
        }
    }
}
