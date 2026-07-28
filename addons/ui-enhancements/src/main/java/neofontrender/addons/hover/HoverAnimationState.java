package neofontrender.addons.hover;

/** Render-thread animation state that can reverse without restarting or jumping. */
public final class HoverAnimationState {
    private static final long MAX_FRAME_NANOS = 100_000_000L;

    private float progress;
    private long lastNanos;

    public void update(boolean active, int enterMillis, int exitMillis) {
        update(active, enterMillis, exitMillis, System.nanoTime());
    }

    void update(boolean active, int enterMillis, int exitMillis, long now) {
        int durationMillis = active ? enterMillis : exitMillis;
        if (durationMillis <= 0) {
            progress = active ? 1.0F : 0.0F;
            lastNanos = now;
            return;
        }

        if (lastNanos == 0L) {
            lastNanos = now;
            return;
        }

        long elapsedNanos = Math.max(0L, Math.min(now - lastNanos, MAX_FRAME_NANOS));
        lastNanos = now;
        float amount = elapsedNanos / (durationMillis * 1_000_000.0F);
        progress = clamp(progress + (active ? amount : -amount));
    }

    public void reset(boolean active) {
        progress = active ? 1.0F : 0.0F;
        lastNanos = 0L;
    }

    public float progress() {
        return progress;
    }

    public float easedProgress() {
        return smoothStep(progress);
    }

    public boolean isVisible() {
        return progress > 0.001F;
    }

    public static float smoothStep(float value) {
        float clamped = clamp(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
