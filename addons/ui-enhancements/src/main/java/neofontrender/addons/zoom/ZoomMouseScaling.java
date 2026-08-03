package neofontrender.addons.zoom;

public final class ZoomMouseScaling {
    private static volatile float movementScale = 1.0F;
    private static float rawScale = 1.0F;
    private static float smoothedScale = 1.0F;

    // Sensitivity smoothing
    private static final float SENSITIVITY_SMOOTHING = 0.15F;

    // Camera delta smoothing (exponential moving average)
    private static float smoothedYaw;
    private static float smoothedPitch;
    private static boolean smoothCameraActive;
    private static final float CAMERA_SMOOTHING = 0.15F;

    private ZoomMouseScaling() {}

    static void update(float baseFov, float zoomedFov, float adjustment) {
        rawScale = ZoomMath.mouseMovementScale(baseFov, zoomedFov, adjustment);
        smoothedScale += (rawScale - smoothedScale) * SENSITIVITY_SMOOTHING;
        movementScale = smoothedScale;
    }

    static void reset() {
        rawScale = 1.0F;
        smoothedScale = 1.0F;
        movementScale = 1.0F;
        resetSmoothCamera();
    }

    static void resetSmoothCamera() {
        smoothedYaw = 0.0F;
        smoothedPitch = 0.0F;
        smoothCameraActive = false;
    }

    static void enableSmoothCamera() {
        if (!smoothCameraActive) {
            smoothCameraActive = true;
            smoothedYaw = 0.0F;
            smoothedPitch = 0.0F;
        }
    }

    static void disableSmoothCamera() {
        smoothCameraActive = false;
    }

    public static boolean isSmoothCameraActive() {
        return smoothCameraActive;
    }

    /**
     * Smooth the camera deltas for a cinematic feel during zoom.
     * Uses exponential moving average to avoid the MouseFilter cold-start
     * problem that vanilla's smoothCamera has.
     */
    public static float[] smoothCameraDelta(float rawYaw, float rawPitch) {
        if (!smoothCameraActive) {
            return new float[]{rawYaw, rawPitch};
        }
        smoothedYaw += (rawYaw - smoothedYaw) * CAMERA_SMOOTHING;
        smoothedPitch += (rawPitch - smoothedPitch) * CAMERA_SMOOTHING;
        return new float[]{smoothedYaw, smoothedPitch};
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
