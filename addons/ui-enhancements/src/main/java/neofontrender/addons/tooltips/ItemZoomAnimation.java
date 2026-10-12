package neofontrender.addons.tooltips;

/** A single visible preview timeline; revisiting an item never revives a stale item timeline. */
final class ItemZoomAnimation {
    static final long SESSION_GAP_NANOS = 500_000_000L;
    private String item;
    private long started;
    private long lastSeen;
    private boolean instant;

    float progress(String nextItem, long now, boolean enabled, int durationMillis, String switching) {
        boolean newSession = item == null || now < lastSeen || now - lastSeen > SESSION_GAP_NANOS;
        boolean changed = !nextItem.equals(item);
        if (newSession || (changed && "restart".equals(switching))) {
            started = now;
            instant = false;
        } else if (changed && "instant".equals(switching)) {
            instant = true;
        }
        item = nextItem;
        lastSeen = now;
        if (!enabled || durationMillis <= 0) instant = true;
        if (instant) return 1;
        float t = Math.max(0, Math.min(1, (now - started) / (durationMillis * 1_000_000.0F)));
        return t * t * (3 - 2 * t);
    }

    void reset() {
        item = null;
        instant = false;
    }

    void rendered(long now) {
        if (item != null) lastSeen = now;
    }
}
