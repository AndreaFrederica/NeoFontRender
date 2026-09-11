package neofontrender.core.font.cosmic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/** Caches shaping-verified character plans separately from color/texture caches. */
final class CosmicMonospaceComposer {
    interface Probe {
        int[] splitPoints(String text, int flags, float size, float scale);
    }

    private record Key(String text, int flags, float size, float scale) {}

    // A token bucket limits sustained synchronous probing to 0.3 ms per 16.7 ms.
    // A cold native call cannot be preempted; its entire cost becomes debt before another probe.
    private static final long BUDGET_NANOS = 300_000L;
    private static final long WINDOW_NANOS = 16_666_667L;
    private static final long STABLE_NANOS = 500_000_000L;
    private static final long DRAW_GAP_NANOS = 100_000_000L;

    private static final class Plan {
        final List<String> pieces;
        final List<String> whole;
        long firstDraw;
        long lastDraw;
        int draws;
        boolean merged;

        Plan(String text, List<String> pieces) {
            this.pieces = pieces;
            this.whole = List.of(text);
        }

        List<String> use(long now, boolean rendering) {
            if (!merged && rendering && pieces.size() > 1) {
                if (draws == 0 || now - lastDraw > DRAW_GAP_NANOS) {
                    firstDraw = now;
                    draws = 0;
                }
                lastDraw = now;
                draws++;
                merged = draws >= 8 && now - firstDraw >= STABLE_NANOS;
            }
            return merged ? whole : pieces;
        }
    }

    private final Probe probe;
    private final LongSupplier clock;
    private long budgetTime;
    private double credit = BUDGET_NANOS;
    private long probeCalls;
    private long probeNanos;
    private long deferredProbes;
    private long mergedDraws;
    private final Map<Key, Plan> plans = new LinkedHashMap<>(128, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Plan> eldest) {
            return size() > 2048;
        }
    };

    CosmicMonospaceComposer(Probe probe) {
        this(probe, System::nanoTime);
    }

    CosmicMonospaceComposer(Probe probe, LongSupplier clock) {
        this.probe = probe;
        this.clock = clock;
        this.budgetTime = clock.getAsLong();
    }

    List<String> split(String text, int flags, float size, float scale, boolean enabled) {
        return split(text, flags, size, scale, enabled, true);
    }

    List<String> split(String text, int flags, float size, float scale, boolean enabled, boolean rendering) {
        return split(text, flags, size, scale, enabled, rendering, false);
    }

    List<String> split(String text, int flags, float size, float scale, boolean enabled, boolean rendering,
                       boolean asynchronous) {
        if (!enabled || text.length() < 2 || text.length() > 256) return List.of(text);
        Key key = new Key(text, flags, size, scale);
        Plan cached = plans.get(key);
        long now = clock.getAsLong();
        if (cached != null) return use(cached, now, rendering);
        // Keep combining marks, emoji, variation selectors and bidi on the original path.
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            if (!(cp >= ' ' && cp <= '~') && !isCjk(cp)) return List.of(text);
            i += Character.charCount(cp);
        }
        credit = Math.min(BUDGET_NANOS, credit + Math.max(0L, now - budgetTime)
                * ((double) BUDGET_NANOS / WINDOW_NANOS));
        budgetTime = now;
        if (!asynchronous && credit <= 0) {
            deferredProbes++;
            // Temporary admission failure must not poison the shaping decision cache.
            return List.of(text);
        }
        int[] points;
        long started = clock.getAsLong();
        try {
            probeCalls++;
            points = probe.splitPoints(text, flags, size, scale);
        } finally {
            long elapsed = Math.max(0L, clock.getAsLong() - started);
            probeNanos += elapsed;
            if (!asynchronous) credit -= elapsed;
        }
        // A background request has no answer yet. Never cache it as an unsafe shaping decision.
        if (points == null) {
            deferredProbes++;
            return List.of(text);
        }
        List<String> result = List.of(text);
        if (points != null && points.length > 1 && points[points.length - 1] == text.length()) {
            List<String> pieces = new ArrayList<>(points.length);
            int start = 0;
            for (int end : points) {
                if (end <= start || end > text.length()) {
                    pieces.clear();
                    break;
                }
                pieces.add(text.substring(start, end));
                start = end;
            }
            if (!pieces.isEmpty()) result = List.copyOf(pieces);
        }
        Plan plan = new Plan(text, result);
        plans.put(key, plan);
        return use(plan, now, rendering);
    }

    private List<String> use(Plan plan, long now, boolean rendering) {
        List<String> result = plan.use(now, rendering);
        if (rendering && plan.merged) mergedDraws++;
        return result;
    }

    long probeCalls() { return probeCalls; }
    long probeNanos() { return probeNanos; }
    long deferredProbes() { return deferredProbes; }
    long mergedDraws() { return mergedDraws; }

    private static boolean isCjk(int cp) {
        if ((cp >= 0xFF01 && cp <= 0xFF60) || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x3000 && cp <= 0x303F)) return true;
        if ((cp >= 0xFF61 && cp <= 0xFFDC) || cp == 0x3099 || cp == 0x309A) return false;
        return switch (Character.UnicodeScript.of(cp)) {
            case HAN, HIRAGANA, KATAKANA, HANGUL, BOPOMOFO -> true;
            default -> false;
        };
    }

    void clear() {
        plans.clear();
        credit = BUDGET_NANOS;
        budgetTime = clock.getAsLong();
    }
}
