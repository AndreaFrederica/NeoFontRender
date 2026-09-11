package neofontrender.addons.tooltips;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObscureTooltipLayoutTest {
    @Test
    void modernWidthLimitHonorsScreenCallerConfigurationAndHeaderIcon() {
        assertEquals(600, ObscureTooltipCompat.contentWidthLimit(600, -1, 0, 0));
        assertEquals(200, ObscureTooltipCompat.contentWidthLimit(600, 200, 300, 0));
        assertEquals(128, ObscureTooltipCompat.contentWidthLimit(600, 200, 150, 22));
        assertEquals(1, ObscureTooltipCompat.contentWidthLimit(16, -1, 0, 22));
    }

    @Test
    void tallFormulaAndMoleculeReserveSpaceBeforeFollowingText() {
        int previous = TooltipConfig.lineHeight;
        try {
            TooltipConfig.lineHeight = 10;
            assertEquals(Arrays.asList(20, 10, 70, 10), TooltipLayout.lineAdvances(
                    Arrays.asList("formula", "caption", "molecule", "mod name"),
                    Arrays.asList(false, false, false, false), 1.0F,
                    text -> "formula".equals(text) ? 18 : "molecule".equals(text) ? 67 : 0));
            assertEquals(Arrays.asList(30, 15, 105, 15), TooltipLayout.lineAdvances(
                    Arrays.asList("formula", "caption", "molecule", "mod name"),
                    Arrays.asList(false, false, false, false), 1.5F,
                    text -> "formula".equals(text) ? 18 : "molecule".equals(text) ? 67 : 0));
        } finally {
            TooltipConfig.lineHeight = previous;
        }
    }

    @Test
    void headerAccommodatesScaledOrInlineTitleAndKeepsTheIconMinimum() {
        assertEquals(22, ObscureTooltipCompat.headerContentHeight(10, 10));
        assertEquals(22, ObscureTooltipCompat.headerContentHeight(10, 0));
        assertEquals(32, ObscureTooltipCompat.headerContentHeight(15, 15));
        assertEquals(72, ObscureTooltipCompat.headerContentHeight(60, 10));
        assertEquals(67, ObscureTooltipCompat.headerContentHeight(60, 0));
    }
}
