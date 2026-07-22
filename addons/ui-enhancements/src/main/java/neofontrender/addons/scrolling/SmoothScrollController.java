package neofontrender.addons.scrolling;

/** Small decelerating one-dimensional scroller, independent of ModernUI runtime classes. */
public final class SmoothScrollController {
    private float current;
    private float start;
    private float target;
    private long startNanos;
    private boolean initialized;

    public void sync(float value) {
        current = start = target = value;
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
        target = clamp(target, max);
        if (current == target) return current;
        float p = Math.min((System.nanoTime() - startNanos) / (SmoothScrollConfig.durationMillis * 1_000_000.0F), 1.0F);
        float eased = 1.0F - (1.0F - p) * (1.0F - p);
        current = start + (target - start) * eased;
        if (p >= 1.0F) current = target;
        return current;
    }

    public float getTarget() {
        return target;
    }

    private static float clamp(float value, float max) {
        return Math.max(0.0F, Math.min(Math.max(0.0F, max), value));
    }
}
