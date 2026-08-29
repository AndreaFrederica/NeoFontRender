package neofontrender.addons.scrolling;

/** Small decelerating one-dimensional scroller, independent of ModernUI runtime classes. */
public final class SmoothScrollController {
    private float current;
    private float start;
    private float target;
    private long startNanos;
    private long lastUpdateNanos;
    private boolean initialized;

    public void sync(float value) {
        current = start = target = value;
        lastUpdateNanos = System.nanoTime();
        initialized = true;
    }

    public void scrollBy(float delta, float max, float actual) {
        if (max <= 0.0F) {
            sync(0.0F);
            return;
        }
        if (!initialized) sync(actual);
        start = current;
        target = clamp(target + delta, max);
        startNanos = System.nanoTime();
    }

    public float update(float actual, float max) {
        if (max <= 0.0F) {
            sync(0.0F);
            return 0.0F;
        }
        if (!initialized || !SmoothScrollConfig.enabled) {
            sync(clamp(actual, max));
            return current;
        }
        actual = clamp(actual, max);
        if (Math.abs(actual - current) > 0.05F) sync(actual);
        return advance(max);
    }

    /** Advances state owned by a renderer that only exposes an integer projection externally. */
    public float updateOwned(float max) {
        if (max <= 0.0F) {
            sync(0.0F);
            return 0.0F;
        }
        if (!initialized) sync(0.0F);
        if (!SmoothScrollConfig.enabled) {
            sync(clamp(target, max));
            return current;
        }
        return advance(max);
    }

    private float advance(float max) {
        target = clamp(target, max);
        if (current == target) return current;
        float p = Math.min((System.nanoTime() - startNanos) / (SmoothScrollConfig.durationMillis * 1_000_000.0F), 1.0F);
        float eased = 1.0F - (1.0F - p) * (1.0F - p);
        current = start + (target - start) * eased;
        if (p >= 1.0F) current = target;
        return current;
    }

    /** Retargetable interpolation for dense lists; wheel events do not restart the easing curve. */
    public float updateContinuous(float actual, float max) {
        if (max <= 0.0F) {
            sync(0.0F);
            return 0.0F;
        }
        if (!initialized || !SmoothScrollConfig.enabled) {
            sync(clamp(actual, max));
            return current;
        }
        actual = clamp(actual, max);
        if (Math.abs(actual - current) > 0.05F) sync(actual);
        long now = System.nanoTime();
        long elapsed = Math.max(0L, Math.min(50_000_000L, now - lastUpdateNanos));
        lastUpdateNanos = now;
        target = clamp(target, max);
        current = continuousStep(current, target, elapsed, SmoothScrollConfig.durationMillis);
        if (Math.abs(target - current) < 0.01F) current = target;
        return current;
    }

    public float getTarget() {
        return target;
    }

    /** Moves the complete animation frame when content is inserted ahead of the viewport. */
    public void shiftBy(float delta, float max) {
        if (!initialized) sync(0.0F);
        current = clamp(current + delta, max);
        start = clamp(start + delta, max);
        target = clamp(target + delta, max);
    }

    private static float clamp(float value, float max) {
        return Math.max(0.0F, Math.min(Math.max(0.0F, max), value));
    }

    static float continuousStep(float value, float target, long elapsedNanos, int durationMillis) {
        if (elapsedNanos <= 0L || value == target) return value;
        float durationNanos = Math.max(1, durationMillis) * 1_000_000.0F;
        // 4.6 time constants reach approximately 99% at the configured duration.
        float amount = 1.0F - (float) Math.exp(-4.6F * elapsedNanos / durationNanos);
        return value + (target - value) * Math.max(0.0F, Math.min(1.0F, amount));
    }
}
