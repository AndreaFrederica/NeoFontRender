package neofontrender.core.font.linebreak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CjkLineBreakRulesTest {
    @Test
    void allowsOrdinaryCjkBoundaries() {
        assertTrue(CjkLineBreakRules.canBreakBetween('中', '文'));
        assertTrue(CjkLineBreakRules.canBreakBetween('文', 'A'));
        assertTrue(CjkLineBreakRules.canBreakBetween('A', '中'));
    }

    @Test
    void keepsClosingPunctuationOffLineStart() {
        assertFalse(CjkLineBreakRules.canBreakBetween('文', '。'));
        assertFalse(CjkLineBreakRules.canBreakBetween('文', '）'));
        assertFalse(CjkLineBreakRules.canBreakBetween('文', '！'));
    }

    @Test
    void keepsOpeningPunctuationOffLineEnd() {
        assertFalse(CjkLineBreakRules.canBreakBetween('（', '文'));
        assertFalse(CjkLineBreakRules.canBreakBetween('《', '文'));
        assertFalse(CjkLineBreakRules.canBreakBetween('「', '文'));
    }

    @Test
    void doesNotSplitLatinWordsOrUnicodeClusters() {
        assertFalse(CjkLineBreakRules.canBreakBetween('A', 'B'));
        assertFalse(CjkLineBreakRules.canBreakBetween('中', 0xFE0F));
        assertFalse(CjkLineBreakRules.canBreakBetween(0x1F469, 0x200D));
    }
}
