package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;

class TooltipLayoutSourceTest {
    @AfterEach
    void resetConfig() {
        TooltipConfig.lineHeight = 10;
    }

    @Test
    void trimsTrailingPlaceholdersForGuiLabelTooltips() {
        List<String> source = Arrays.asList("Building Blocks", "", "   ");
        assertEquals(Arrays.asList("Building Blocks"),
                TooltipLayout.normalizedSource(source, true));
    }

    @Test
    void preservesItemTooltipPlaceholdersForPostTextExtensions() {
        List<String> source = Arrays.asList("Sword", "", "");
        assertSame(source, TooltipLayout.normalizedSource(source, false));
    }

    @Test
    void shortLineUsesWholeScreenInsteadOfMouseSideAsWrapLimit() {
        TooltipVisualExtents extents = TooltipVisualExtents.calculate(
                1.25F, 0.55F, 4.0F, 72, 0.0F, 2.0F);
        assertEquals(178, TooltipLayout.screenWidthLimit(200, 5, extents));
    }

    @Test
    void realTinkersTooltipDoesNotGrowWhenInlineMiddlewareIsOnlyRegistered() {
        List<String> lines = Arrays.asList(
                "\u00a7f\u9ed1\u66dc\u77f3\u5f00\u6398\u94f2",
                "\u00a77\u6301\u4e45",
                "",
                "\u00a7f\u9876\u7aef",
                "\u00a77\u8010\u4e45:        \u00a7f139",
                "\u00a77\u6316\u6398\u7b49\u7ea7:    \u00a7f\u94bb",
                "\u00a77\u6316\u6398\u901f\u5ea6:    \u00a7f7.07",
                "\u00a77\u653b\u51fb\u529b:      \u00a7f4.2",
                "",
                "\u00a77\u6750\u6599\u7531 Tinkers' Antique \u6dfb\u52a0",
                "\u00a79\u7269\u8d28: \u00a7c\u65e0",
                "\u00a79\u00a7oTinkers' Antique");

        // A globally installed LaTeX/SVG middleware reports no inline height for these lines.
        List<Integer> advances = TooltipLayout.lineAdvances(
                lines, Arrays.asList(false, false, false, false, false, false,
                        false, false, false, false, false, false),
                1.0F, line -> 0);

        assertEquals(Arrays.asList(10, 10, 10, 10, 10, 10,
                10, 10, 10, 10, 10, 10), advances);
    }

    @Test
    void onlyTheRealInlineFormulaReservesAdditionalRows() {
        List<String> lines = Arrays.asList(
                "\u00a7f\u6c28\u57fa\u82ef\u5e76-18-\u51a0-6\u6eb6\u6db2",
                "\u00a77\\(C_{16}H_{25}NO_6\\)",
                "\u00a77666 mB");

        List<Integer> advances = TooltipLayout.lineAdvances(
                lines, Arrays.asList(false, false, false), 1.0F,
                line -> line.contains("\\(") ? 23 : 0);

        assertEquals(Arrays.asList(10, 30, 10), advances);
    }
}
