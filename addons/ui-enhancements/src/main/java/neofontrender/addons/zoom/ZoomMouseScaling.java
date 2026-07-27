package neofontrender.addons.zoom;

public final class ZoomMouseScaling {
    private static volatile float movementScale = 1.0F;

    private ZoomMouseScaling() {}

    static void update(float baseFov, float zoomedFov, float adjustment) {
        movementScale = ZoomMath.mouseMovementScale(baseFov, zoomedFov, adjustment);
    }

    static void reset() {
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
