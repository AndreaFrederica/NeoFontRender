package neofontrender.addons.zoom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomMathTest {
    @Test
    void appliesConfiguredMagnificationToFov() {
        assertEquals(20.0F, ZoomMath.zoomedFov(80.0F, 4.0F), 0.0001F);
        assertEquals(50.0F, ZoomMath.zoomedFov(80.0F, 4.0F, 0.5F), 0.0001F);
    }

    @Test
    void clampsInvalidMagnification() {
        assertEquals(35.0F, ZoomMath.zoomedFov(70.0F, 1.0F), 0.0001F);
        assertEquals(8.75F, ZoomMath.zoomedFov(70.0F, 20.0F), 0.0001F);
        assertEquals(17.5F, ZoomMath.zoomedFov(70.0F, Float.NaN), 0.0001F);
        assertTrue(Float.isNaN(ZoomMath.zoomedFov(Float.NaN, 4.0F)));
    }

    @Test
    void appliesBidirectionalMouseAdjustmentAroundZero() {
        float baseFov = 70.0F;
        float zoomedFov = ZoomMath.zoomedFov(baseFov, 4.0F);

        float projectedMagnification = (float) (Math.tan(Math.toRadians(baseFov) * 0.5D)
                / Math.tan(Math.toRadians(zoomedFov) * 0.5D));
        assertEquals(1.0F, ZoomMath.mouseMovementScale(baseFov, zoomedFov, 0.0F), 0.0001F);
        assertEquals(projectedMagnification,
                ZoomMath.mouseMovementScale(baseFov, zoomedFov, 1.0F), 0.0001F);
        assertEquals(1.0F / projectedMagnification,
                ZoomMath.mouseMovementScale(baseFov, zoomedFov, -1.0F), 0.0001F);
        assertEquals((float) Math.sqrt(projectedMagnification),
                ZoomMath.mouseMovementScale(baseFov, zoomedFov, 0.5F), 0.0001F);
    }
}
