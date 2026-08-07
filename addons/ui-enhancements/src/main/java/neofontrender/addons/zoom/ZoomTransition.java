package neofontrender.addons.zoom;

final class ZoomTransition {
    private float amount;
    private long lastUpdateNanos = Long.MIN_VALUE;

    float update(boolean zoomRequested, long nowNanos, int durationMillis) {
        if (durationMillis <= 0) {
            amount = zoomRequested ? 1.0F : 0.0F;
            lastUpdateNanos = nowNanos;
            return amount;
        }
        if (lastUpdateNanos == Long.MIN_VALUE) {
            lastUpdateNanos = nowNanos;
            return smoothStep(amount);
        }

        long elapsedNanos = Math.max(0L, nowNanos - lastUpdateNanos);
        lastUpdateNanos = nowNanos;
        // Cap at 3 frames worth (50ms at 60fps) to prevent jumps on lag spikes.
        long cappedNanos = Math.min(elapsedNanos, 50_000_000L);
        float delta = cappedNanos / (durationMillis * 1_000_000.0F);
        amount = clamp01(amount + (zoomRequested ? delta : -delta));
        return smoothStep(amount);
    }

    void reset() {
        amount = 0.0F;
        lastUpdateNanos = Long.MIN_VALUE;
    }

    private static float smoothStep(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
