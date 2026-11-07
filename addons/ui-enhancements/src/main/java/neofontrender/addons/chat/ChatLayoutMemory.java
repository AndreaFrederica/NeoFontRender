package neofontrender.addons.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolution profiles and debounce state, independent of Minecraft and disk IO. */
final class ChatLayoutMemory {
    static final long SETTLE_NANOS = 750_000_000L;
    record Viewport(int width, int height, int guiScale, int chatScale) {
        Viewport {
            if (width <= 0 || height <= 0 || guiScale <= 0 || chatScale <= 0)
                throw new IllegalArgumentException("Invalid viewport");
        }
    }
    record Bounds(int x, int y, int width, int height) {
        boolean valid() {
            return width > 0 && height > 0 && width <= 8192 && height <= 8192
                    && Math.abs((long) x) <= 8192 && Math.abs((long) y) <= 8192;
        }
    }

    private final Map<Viewport, Bounds> profiles = new LinkedHashMap<>();
    private Viewport active;
    private Bounds observed;
    private long changedAt;
    private boolean pending;

    Map<Viewport, Bounds> snapshot() { return new LinkedHashMap<>(profiles); }

    void replace(Map<Viewport, Bounds> replacement) {
        profiles.clear();
        profiles.putAll(replacement);
        // Deleting the current profile must not immediately recreate it while idle.
        pending = false;
    }

    void resetViewport() { active = null; observed = null; pending = false; }

    boolean needsLayout(Viewport viewport, Bounds bounds) {
        return !viewport.equals(active) || !bounds.equals(observed);
    }

    boolean rememberNow(Bounds bounds) {
        if (active == null || !bounds.valid()) return false;
        observed = bounds;
        pending = false;
        return !bounds.equals(profiles.put(active, bounds));
    }

    Bounds enter(Viewport viewport) {
        if (viewport.equals(active)) return null;
        active = viewport;
        observed = null;
        pending = false;
        return profiles.get(viewport);
    }

    boolean observe(Bounds bounds, long now, boolean dragging) {
        if (active == null || !bounds.valid()) return false;
        if (!Objects.equals(observed, bounds)) {
            observed = bounds;
            changedAt = now;
            pending = true;
        }
        if (dragging) changedAt = now;
        if (!pending || dragging || now - changedAt < SETTLE_NANOS) return false;
        pending = false;
        return !bounds.equals(profiles.put(active, bounds));
    }

    // A TOML string list keeps each profile atomic in the layered config store.
    static List<String> encode(Map<Viewport, Bounds> profiles) {
        List<String> rows = new ArrayList<>();
        profiles.forEach((v, b) -> rows.add(v.width + ":" + v.height + ":" + v.guiScale + ":"
                + v.chatScale + ":" + b.x + ":" + b.y + ":" + b.width + ":" + b.height));
        return rows;
    }

    static Map<Viewport, Bounds> decode(List<String> rows) {
        Map<Viewport, Bounds> result = new LinkedHashMap<>();
        for (String row : rows) {
            try {
                String[] parts = row.split(":", -1);
                if (parts.length != 8) continue;
                int[] n = new int[8];
                for (int i = 0; i < n.length; i++) n[i] = Integer.parseInt(parts[i]);
                Bounds bounds = new Bounds(n[4], n[5], n[6], n[7]);
                if (bounds.valid()) result.put(new Viewport(n[0], n[1], n[2], n[3]), bounds);
            } catch (IllegalArgumentException ignored) {
                // One damaged/manual entry must not discard the other layouts.
            }
        }
        return result;
    }
}
