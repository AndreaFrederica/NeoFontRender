package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class PreviewModelBoundsTest {
    @Test
    void offsetsAndFitKeepDragonSizedOvershootInsideBothMeasuredAndFixedCells() {
        PreviewModelBounds bounds = new PreviewModelBounds(-20.330F, -13.855F, 57.130F, 63).pad(2).includeCell(40, 64);
        assertEquals(83, bounds.width());
        assertEquals(81, bounds.height());
        for (int width : new int[]{40, bounds.width()}) {
            PreviewModelBounds.Placement fit = bounds.fit(width, 64);
            assertTrue(fit.x + bounds.left * fit.scale >= -0.001F);
            assertTrue(fit.y + bounds.top * fit.scale >= -0.001F);
            assertTrue(fit.x + bounds.right * fit.scale <= width + 0.001F);
            assertTrue(fit.y + bounds.bottom * fit.scale <= 64.001F);
        }
    }

    @Test
    void alphaScanConvertsBottomUpRowsAndIncludesLastOpaquePixel() {
        ByteBuffer alpha = ByteBuffer.allocate(8 * 6);
        assertNull(PreviewModelBounds.alpha(alpha, 8, 6));
        alpha.put(1 * 8 + 2, (byte) 255);
        alpha.put(4 * 8 + 6, (byte) 128);
        alpha.put(0, (byte) 1); // Ignore virtually transparent texture noise.
        PreviewModelBounds bounds = PreviewModelBounds.alpha(alpha, 8, 6);
        assertEquals(2, bounds.left); assertEquals(1, bounds.top);
        assertEquals(7, bounds.right); assertEquals(5, bounds.bottom);
    }
}
