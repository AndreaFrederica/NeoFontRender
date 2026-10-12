package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModernTooltipScrollBarTest {
    @Test
    void followsHeiTrackGeometryAndClampsOffset() {
        ModernTooltipScrollBar.Geometry geometry = ModernTooltipScrollBar.Geometry.calculate(
                new Rectangle(100, 10, 14, 100), 24, 76, 0.5F);

        assertEquals(110, geometry.trackLeft);
        assertEquals(113, geometry.trackRight);
        assertEquals(11, geometry.trackTop);
        assertEquals(109, geometry.trackBottom);
        assertEquals(110, geometry.thumbLeft);
        assertEquals(113, geometry.thumbRight);
        assertEquals(48, geometry.thumbTop);
        assertEquals(72, geometry.thumbBottom);

        ModernTooltipScrollBar.Geometry bottom = ModernTooltipScrollBar.Geometry.calculate(
                new Rectangle(100, 10, 14, 100), 24, 76, 4.0F);
        assertEquals(85, bottom.thumbTop);
        assertEquals(109, bottom.thumbBottom);
    }

    @Test
    void tinyTrackNeverProducesAnOversizedThumb() {
        ModernTooltipScrollBar.Geometry geometry = ModernTooltipScrollBar.Geometry.calculate(
                new Rectangle(0, 0, 14, 2), 1, 100, 0.0F);

        assertEquals(1, geometry.trackBottom - geometry.trackTop);
        assertEquals(1, geometry.thumbBottom - geometry.thumbTop);
    }
}
