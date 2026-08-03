package neofontrender.addons.zoom;

public final class ZoomMouseScaling {
    private static volatile float movementScale = 1.0F;
    private static float rawScale = 1.0F;
    private static float smoothedScale = 1.0F;

    // Smoothing factor: 0.0 = no change, 1.0 = instant. 0.15 is responsive but stable.
    private static final float SMOOTHING_FACTOR = 0.15F;

    private ZoomMouseScaling() {}

    static void update(float baseFov, float zoomedFov, float adjustment) {
        rawScale = ZoomMath.mouseMovementScale(baseFov, zoomedFov, adjustment);
        smoothedScale += (rawScale - smoothedScale) * SMOOTHING_FACTOR;
        movementScale = smoothedScale;
    }

    static void reset() {
        rawScale = 1.0F;
        smoothedScale = 1.0F;
        movementScale = 1.0F;
    }

    public static float adjustedSensitivity(float configuredSensitivity) {
        float scale = movementScale;
        if (!Float.isFinite(configuredSensitivity) || Math.abs(scale - 1.0F) <= 0.0001F) {
            return configuredSensitivity;
        }

        // Vanilla cubes this value before applying raw mouse deltas. Adjust the
        // cubic input so the final angular movement is multiplied by scale.
        float vanillaInput = configuredSensitivity * 0.6F + 0.2F;
        float adjustedInput = vanillaInput * (float) Math.cbrt(scale);
        return (adjustedInput - 0.2F) / 0.6F;
    }
}
