package neofontrender.addons.loading;

/** Maps Forge's nested resource-reload bars onto one monotonic user-facing progress value. */
final class ResourceReloadProgress {
    private float amount;

    void reset() {
        amount = 0.02F;
    }

    float step(String title, int completed, int total) {
        if (total <= 0) return amount;
        float ratio = clamp(completed / (float) total);
        if ("Loading Resources".equals(title)) {
            advance(0.04F + 0.16F * ratio);
        } else if ("Reloading".equals(title)) {
            advance(0.20F + 0.70F * ratio);
        }
        return amount;
    }

    float completeBar(String title) {
        if ("Reloading".equals(title)) advance(0.90F);
        else if ("Loading Resources".equals(title)) advance(0.92F);
        return amount;
    }

    float languageMetadata() {
        advance(0.94F);
        return amount;
    }

    float rendererRefresh() {
        advance(0.97F);
        return amount;
    }

    float complete() {
        amount = 1.0F;
        return amount;
    }

    float amount() {
        return amount;
    }

    private void advance(float target) {
        amount = Math.max(amount, clamp(target));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
