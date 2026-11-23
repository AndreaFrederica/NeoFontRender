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

        default int profileFlags(int flags, float size, float scale) {
            return 0;
        }
    }

    private record Key(String text, int flags, float size, float scale) {}
    private record ProfileKey(int flags, float size, float scale) {}

    // A token bucket limits sustained synchronous probing to 0.3 ms per 16.7 ms.
    // A cold native call cannot be preempted; its entire cost becomes debt before another probe.
    private static final long BUDGET_NANOS = 300_000L;
    private static final long WINDOW_NANOS = 16_666_667L;
    private static final long STABLE_NANOS = 500_000_000L;
    private static final long DRAW_GAP_NANOS = 100_000_000L;

    private static final class Plan {
        final int[] endpoints;
        long firstDraw;
        long lastDraw;
        int draws;
        boolean merged;

        Plan(int[] endpoints) {
            this.endpoints = endpoints;
        }

        List<String> use(String text, long now, boolean rendering) {
            if (!merged && rendering && endpoints.length > 0) {
                if (draws == 0 || now - lastDraw > DRAW_GAP_NANOS) {
                    firstDraw = now;
                    draws = 0;
                }
                lastDraw = now;
                draws++;
                merged = draws >= 8 && now - firstDraw >= STABLE_NANOS;
            }
            if (merged) return List.of(text);
            List<String> pieces = new ArrayList<>(endpoints.length + 1);
            int start = 0;
            for (int end : endpoints) {
                if (end <= start || end > text.length()) return List.of(text);
                pieces.add(text.substring(start, end));
                start = end;
            }
            if (start != text.length()) return List.of(text);
            return List.copyOf(pieces);
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
    private final Map<ProfileKey, Integer> profileFlags = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ProfileKey, Integer> eldest) {
            return size() > 32;
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
        int fixedProfile = 0;
        if (!asynchronous) {
            ProfileKey profileKey = new ProfileKey(flags, size, scale);
            Integer cachedProfile = profileFlags.get(profileKey);
            if (cachedProfile == null) {
                cachedProfile = probe.profileFlags(flags, size, scale);
                profileFlags.put(profileKey, cachedProfile);
            }
            fixedProfile = cachedProfile;
        }
        String keyText = (fixedProfile & CosmicNative.PROFILE_TABULAR_DIGITS) != 0
                ? normalizeDigits(text) : text;
        Key key = new Key(keyText, flags, size, scale);
        Plan cached = plans.get(key);
        long now = clock.getAsLong();
        if (cached != null) return use(cached, text, now, rendering);
        // Keep combining marks, emoji, variation selectors and bidi on the original path.
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            if (!(cp >= ' ' && cp <= '~') && !isSafeSingleCodePoint(cp)) return List.of(text);
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
        int[] endpoints = new int[0];
        if (points != null && points.length > 1 && points[points.length - 1] == text.length()) {
            endpoints = points;
        }
        Plan plan = new Plan(endpoints);
        plans.put(key, plan);
        return use(plan, text, now, rendering);
    }

    private List<String> use(Plan plan, String text, long now, boolean rendering) {
        List<String> result = plan.use(text, now, rendering);
        if (rendering && plan.merged) mergedDraws++;
        return result;
    }

    long probeCalls() { return probeCalls; }
    long probeNanos() { return probeNanos; }
    long deferredProbes() { return deferredProbes; }
    long mergedDraws() { return mergedDraws; }

    private static String normalizeDigits(String text) {
        StringBuilder result = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                if (result == null) result = new StringBuilder(text);
                result.setCharAt(i, '0');
            }
        }
        return result == null ? text : result.toString();
    }

    private static boolean isCjk(int cp) {
        if ((cp >= 0xFF01 && cp <= 0xFF60) || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x3000 && cp <= 0x303F)) return true;
        if ((cp >= 0xFF61 && cp <= 0xFFDC) || cp == 0x3099 || cp == 0x309A) return false;
        return switch (Character.UnicodeScript.of(cp)) {
            case HAN, HIRAGANA, KATAKANA, HANGUL, BOPOMOFO -> true;
            default -> false;
        };
    }

    private static boolean isSafeSingleCodePoint(int cp) {
        if (isCjk(cp)) return true;
        if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) return false;
        if ((cp >= 0x300 && cp <= 0x36F) || (cp >= 0x1AB0 && cp <= 0x1AFF)
                || (cp >= 0x1DC0 && cp <= 0x1DFF) || (cp >= 0x20D0 && cp <= 0x20FF)
                || (cp >= 0xFE00 && cp <= 0xFE0F) || (cp >= 0xE0100 && cp <= 0xE01EF)
                || (cp >= 0x1F000 && cp <= 0x1FAFF) || (cp >= 0x2600 && cp <= 0x27BF)
                || (cp >= 0x1F3FB && cp <= 0x1F3FF)) return false;
        return true;
    }

    void clear() {
        plans.clear();
        profileFlags.clear();
        credit = BUDGET_NANOS;
        budgetTime = clock.getAsLong();
    }
}
