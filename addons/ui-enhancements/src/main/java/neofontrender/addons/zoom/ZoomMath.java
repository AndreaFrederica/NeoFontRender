package neofontrender.addons.zoom;

final class ZoomMath {
    private static final float MIN_MAGNIFICATION = 2.0F;
    private static final float MAX_MAGNIFICATION = 8.0F;

    private ZoomMath() {}

    static float zoomedFov(float fov, float magnification) {
        return zoomedFov(fov, magnification, 1.0F);
    }

    static float zoomedFov(float fov, float magnification, float transition) {
        if (!Float.isFinite(fov)) return fov;
        float target = fov / clampMagnification(magnification);
        float amount = Float.isFinite(transition)
                ? Math.max(0.0F, Math.min(1.0F, transition)) : 0.0F;
        return fov + (target - fov) * amount;
    }

    static float clampMagnification(float magnification) {
        if (!Float.isFinite(magnification)) return 4.0F;
        return Math.max(MIN_MAGNIFICATION, Math.min(MAX_MAGNIFICATION, magnification));
    }

    static float mouseMovementScale(float baseFov, float zoomedFov, float adjustment) {
        if (!Float.isFinite(baseFov) || !Float.isFinite(zoomedFov)
                || baseFov <= 0.0F || baseFov >= 179.0F
                || zoomedFov <= 0.0F || zoomedFov >= 179.0F) {
            return 1.0F;
        }
        double baseTangent = Math.tan(Math.toRadians(baseFov) * 0.5D);
        double zoomedTangent = Math.tan(Math.toRadians(zoomedFov) * 0.5D);
        float projectedMagnification = (float) (baseTangent / zoomedTangent);
        float strength = Float.isFinite(adjustment)
                ? Math.max(-1.0F, Math.min(1.0F, adjustment)) : 0.0F;
        float scale = (float) Math.pow(projectedMagnification, strength);
        if (!Float.isFinite(scale)) return 1.0F;
        return Math.max(0.05F, Math.min(20.0F, scale));
    }
}
