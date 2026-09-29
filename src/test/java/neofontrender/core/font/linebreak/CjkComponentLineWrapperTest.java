package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;
import neofontrender.text.syntax.MinecraftLegacySyntaxProvider;
import neofontrender.text.syntax.TextSyntaxEngine;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CjkComponentLineWrapperTest {
    private static final TextSyntaxEngine SYNTAX = TextSyntaxEngine.builder()
            .register(MinecraftLegacySyntaxProvider.INSTANCE).build();
    private static final FontRenderer FONT = createFont();

    @Test
    void carriesInlineColorAndAllDecorationsAcrossRepeatedWraps() {
        assertPreserved(new TextComponentString("\u00a7c\u00a7k\u00a7l\u00a7m\u00a7n\u00a7oABCDEFGHIJKL"), 24);
    }

    @Test
    void carriesFormattingAcrossExplicitAndAutomaticBreaksTogether() {
        assertPreserved(new TextComponentString("\u00a7c\u00a7lABCDEFGH\nIJKLMNOP\nQRST"), 24);
    }

    @Test
    void keepsTheMandatoryBreakAfterASoftWrappedTail() {
        List<ITextComponent> lines = assertPreserved(
                new TextComponentString("\u00a7cABCDEF\nGHIJ"), 20);
        assertEquals(List.of("ABCD", "EF", "GHIJ"), lines.stream()
                .map(line -> SYNTAX.parse(line.getFormattedText()).plainText()).toList());
    }

    @Test
    void preservesResetsAndColorChangesOverComponentStyle() {
        TextComponentString source = new TextComponentString(
                "ABCD\u00a7rEFGHIJKL\u00a7aMNOPQRST\u00a7lUVWX");
        source.getStyle().setColor(TextFormatting.RED).setBold(true);
        assertPreserved(source, 24);
    }

    @Test
    void preservesResetAtExplicitNewline() {
        TextComponentString source = new TextComponentString("AB\u00a7r\nCDEFGHIJ");
        source.getStyle().setColor(TextFormatting.RED).setBold(true);
        assertPreserved(source, 24);
    }

    @Test
    void preservesComponentStylesAndClickEventsWithoutLeakingToSiblings() {
        TextComponentString source = new TextComponentString("\u00a7cABCDEFGHIJ");
        source.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help"));
        source.appendSibling(new TextComponentString("KLMNOPQRST"));
        List<ITextComponent> lines = assertPreserved(source, 20);
        for (ITextComponent line : lines) {
            for (ITextComponent part : line.getSiblings()) {
                assertEquals(source.getStyle().getClickEvent(), part.getStyle().getClickEvent());
            }
        }
    }

    @Test
    void retryingAComponentOnTheNextLineDoesNotDuplicateItsNewlineRemainder() {
        TextComponentString source = new TextComponentString("ABCD");
        source.appendSibling(new TextComponentString("\u00a7cEF\nGHIJ"));
        assertPreserved(source, 20);
    }

    @Test
    void removesLeadingSpaceWithoutRemovingContinuationFormatting() {
        List<ITextComponent> lines = CjkComponentLineWrapper.wrap(
                new TextComponentString("\u00a7c\u00a7lABCD EFGH"), 24, FONT, true, true);
        assertEquals("ABCDEFGH", plain(lines));
        for (ITextComponent line : lines) {
            StructuredText parsed = SYNTAX.parse(line.getFormattedText());
            for (int i = 0; i < parsed.plainText().length(); i++) {
                assertEquals(0xFF5555, parsed.styleAt(i).rgb());
                assertTrue(parsed.styleAt(i).bold());
            }
        }
    }

    @Test
    void narrowLinesConsumeAVisibleCharacterAfterTheFormattingPrefix() {
        List<ITextComponent> lines = assertPreserved(new TextComponentString("\u00a7c\u00a7lAB"), 1);
        assertEquals(List.of("A", "B"), lines.stream()
                .map(line -> SYNTAX.parse(line.getFormattedText()).plainText())
                .filter(text -> !text.isEmpty()).toList());
    }

    private static List<ITextComponent> assertPreserved(ITextComponent source, int width) {
        StructuredText expected = SYNTAX.parse(source.getFormattedText());
        List<ITextComponent> lines = CjkComponentLineWrapper.wrap(source, width, FONT, false, true);
        assertTrue(lines.size() > 1);
        assertEquals(expected.plainText().replace("\n", ""), plain(lines));
        List<TextStyle> expectedStyles = new ArrayList<>();
        for (int i = 0; i < expected.plainText().length(); i++) {
            if (expected.plainText().charAt(i) != '\n') expectedStyles.add(expected.styleAt(i));
        }
        List<TextStyle> actualStyles = new ArrayList<>();
        for (ITextComponent line : lines) {
            StructuredText parsed = SYNTAX.parse(line.getFormattedText());
            for (int i = 0; i < parsed.plainText().length(); i++) actualStyles.add(parsed.styleAt(i));
        }
        assertEquals(expectedStyles, actualStyles);
        return lines;
    }

    private static String plain(List<ITextComponent> lines) {
        return lines.stream().map(line -> SYNTAX.parse(line.getFormattedText()).plainText())
                .reduce("", String::concat);
    }

    private static FontRenderer createFont() {
        try {
            // Skip the constructor's texture/GL setup; retain vanilla measurement and wrapping.
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (FontRenderer) ((Unsafe) field.get(null)).allocateInstance(FixedWidthFont.class);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class FixedWidthFont extends FontRenderer {
        private FixedWidthFont() { super(null, null, null, false); }

        @Override
        public int getCharWidth(char character) {
            return character == '\u00a7' ? -1 : 5;
        }
    }
}
