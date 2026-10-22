package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipVisualExtentsTest {
    @Test
    void reservesDirectionalBorderAndShadowAreaForDefaults() {
        TooltipVisualExtents extents = TooltipVisualExtents.calculate(
                1.25F, 0.55F, 4.0F, 72, 0.0F, 2.0F);

        assertEquals(6, extents.left);
        assertEquals(4, extents.top);
        assertEquals(6, extents.right);
        assertEquals(8, extents.bottom);
    }

    @Test
    void disabledShadowOnlyReservesTheAntialiasedBorder() {
        TooltipVisualExtents extents = TooltipVisualExtents.calculate(
                1.25F, 0.55F, 4.0F, 0, 0.0F, 2.0F);

        assertEquals(2, extents.left);
        assertEquals(2, extents.top);
        assertEquals(2, extents.right);
        assertEquals(2, extents.bottom);
    }
}
