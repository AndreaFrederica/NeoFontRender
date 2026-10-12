package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RasterInlineContentTest {
    @Test
    void lineHeightMatchedFormulaDoesNotSpillIntoASecondTooltipRow() {
        assertEquals(9, RasterInlineContent.paddedHeight(9, true));
        assertEquals(11, RasterInlineContent.paddedHeight(9, false));
    }
}
