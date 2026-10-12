package neofontrender.text.layout;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CjkLineBreakProviderTest {
    private final CjkLineBreakProvider provider = new CjkLineBreakProvider();

    @Test
    void usesPlainTextBoundariesAfterFormattingIsParsed() {
        StructuredText text = StandardSyntaxEngines.minecraft().parse("\u00A7l中文，测试");
        List<Integer> breaks = provider.opportunities(text);

        assertEquals("中文，测试", text.plainText());
        assertTrue(breaks.contains(1));
        assertFalse(breaks.contains(2));
        assertTrue(breaks.contains(3));
    }

    @Test
    void protectsOpeningClosingAndUnicodeClusterBoundaries() {
        assertFalse(CjkLineBreakProvider.canBreakBetween('文', '。'));
        assertFalse(CjkLineBreakProvider.canBreakBetween('（', '文'));
        assertFalse(CjkLineBreakProvider.canBreakBetween(0x1F469, 0x200D));
        assertFalse(CjkLineBreakProvider.canBreakBetween('中', 0xFE0F));
    }
}
