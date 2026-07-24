package neofontrender.addons.tooltips;

import codechicken.lib.gui.GuiDraw;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeiTooltipCompatTest {
    @Test
    void detectsNeiCustomTooltipLines() {
        assertFalse(NeiTooltipCompat.hasCustomLines(Arrays.asList("Item", "Description")));
        assertTrue(NeiTooltipCompat.hasCustomLines(Arrays.asList(
                "Item", GuiDraw.TOOLTIP_HANDLER + "0")));
    }

    @Test
    void plansEveryNeiTextPageWithStableSelectionOffsets() {
        List<String> lines = Arrays.asList("Title", "Page one", "Page two", "Last line");
        List<List<String>> split = Arrays.asList(
                lines.subList(0, 2), lines.subList(2, 3), lines.subList(3, 4));

        List<NeiTooltipCompat.PageSelection> pages = NeiTooltipCompat.planPages(split);

        assertEquals(3, pages.size());
        List<String> reconstructed = new ArrayList<>();
        int expectedOffset = 0;
        for (int index = 0; index < pages.size(); index++) {
            NeiTooltipCompat.PageSelection page = pages.get(index);
            assertEquals(index, page.index);
            assertEquals(expectedOffset, page.offset);
            assertEquals(index == 0, page.hasTitle());
            reconstructed.addAll(page.lines);
            expectedOffset += page.lines.size();
        }
        assertEquals(lines, reconstructed);
    }

    @Test
    void keepsWrapperPaginationHeightSeparateFromPageNumberSpacing() {
        assertEquals(10, NeiTooltipCompat.paginationTextHeight("Last line", 1, 2));
        assertEquals(12, NeiTooltipCompat.renderedTextHeight("Last line", 1, 2, true));
        assertEquals(12, NeiTooltipCompat.paginationTextHeight(
                "Spaced" + GuiDraw.TOOLTIP_LINESPACE, 0, 2));
        assertEquals(12, NeiTooltipCompat.renderedTextHeight(
                "Spaced" + GuiDraw.TOOLTIP_LINESPACE, 0, 2, true));

        NeiTooltipCompat.PhasedSize size = new NeiTooltipCompat.PhasedSize(
                new Dimension(40, 10), new Dimension(40, 12));
        assertEquals(new Dimension(40, 10), size.next());
        assertEquals(new Dimension(40, 12), size.next());
        assertEquals(new Dimension(40, 12), size.next());
    }
}
