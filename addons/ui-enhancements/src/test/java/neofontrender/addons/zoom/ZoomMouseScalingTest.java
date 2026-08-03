package neofontrender.addons.zoom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomMouseScalingTest {
    @AfterEach
    void reset() {
        ZoomMouseScaling.reset();
    }

    @Test
    void preservesTheVanillaSensitivityCurveWhileIncreasingFinalMovement() {
        float configured = 0.5F;
        float baseFov = 70.0F;
        float zoomedFov = 17.5F;
        float expectedScale = ZoomMath.mouseMovementScale(baseFov, zoomedFov, 1.0F);

        // EMA smoothing requires multiple updates to converge
        for (int i = 0; i < 200; i++) {
            ZoomMouseScaling.update(baseFov, zoomedFov, 1.0F);
        }
        float adjusted = ZoomMouseScaling.adjustedSensitivity(configured);

        assertTrue(adjusted > configured);
        assertEquals(vanillaGain(configured) * expectedScale, vanillaGain(adjusted), 0.01F);
    }

    @Test
    void zeroAdjustmentLeavesSensitivityUntouched() {
        ZoomMouseScaling.update(70.0F, 17.5F, 0.0F);
        assertEquals(0.35F, ZoomMouseScaling.adjustedSensitivity(0.35F), 0.0F);
    }

    @Test
    void negativeAdjustmentReducesFinalMovement() {
        float configured = 0.5F;
        float expectedScale = ZoomMath.mouseMovementScale(70.0F, 17.5F, -1.0F);

        // EMA smoothing requires multiple updates to converge
        for (int i = 0; i < 200; i++) {
            ZoomMouseScaling.update(70.0F, 17.5F, -1.0F);
        }
        float adjusted = ZoomMouseScaling.adjustedSensitivity(configured);

        assertTrue(adjusted < configured);
        assertEquals(vanillaGain(configured) * expectedScale, vanillaGain(adjusted), 0.01F);
    }

    private static float vanillaGain(float sensitivity) {
        float input = sensitivity * 0.6F + 0.2F;
        return input * input * input * 8.0F;
    }
}
