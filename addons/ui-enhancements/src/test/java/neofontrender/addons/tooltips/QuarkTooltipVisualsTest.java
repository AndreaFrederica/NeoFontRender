package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class QuarkTooltipVisualsTest {
    @Test
    void foodIconsUseTheirRealBoundsAndWrapOnNarrowTooltips() {
        assertArrayEquals(new int[]{45, 10},
                QuarkTooltipVisuals.foodDimensionsFor(5, 240));
        assertArrayEquals(new int[]{18, 30},
                QuarkTooltipVisuals.foodDimensionsFor(5, 20));
    }

    @Test
    void halfScaleBookItemsUseEightPixelBoundsAndNinePixelSteps() {
        assertArrayEquals(new int[]{125, 9},
                QuarkTooltipVisuals.enchantedItemDimensionsFor(14, 240));
        assertArrayEquals(new int[]{17, 63},
                QuarkTooltipVisuals.enchantedItemDimensionsFor(14, 20));
    }
}
