package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ThaumcraftAspectTooltipCompatTest {
    @Test
    void sixRealAspectsOccupyOneExplicitVisualRow() {
        assertArrayEquals(new int[]{108, 18},
                ThaumcraftAspectTooltipCompat.dimensionsFor(6, 240));
    }

    @Test
    void visualBlockWrapsByAvailablePixelWidth() {
        assertArrayEquals(new int[]{54, 36},
                ThaumcraftAspectTooltipCompat.dimensionsFor(6, 70));
        assertArrayEquals(new int[]{18, 108},
                ThaumcraftAspectTooltipCompat.dimensionsFor(6, 17));
    }

    @Test
    void guiScaleNeverChangesLogicalAspectDimensions() {
        int[] logical = ThaumcraftAspectTooltipCompat.dimensionsFor(6, 240);
        for (int guiScale = 1; guiScale <= 4; guiScale++) {
            assertArrayEquals(new int[]{108 * guiScale, 18 * guiScale},
                    new int[]{logical[0] * guiScale, logical[1] * guiScale});
        }
    }

    @Test
    void emptyAspectDataCreatesNoVisualBlock() {
        assertArrayEquals(new int[]{0, 0},
                ThaumcraftAspectTooltipCompat.dimensionsFor(0, 240));
    }
}
