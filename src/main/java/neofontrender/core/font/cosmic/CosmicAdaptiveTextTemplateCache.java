package neofontrender.core.font.cosmic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Learns short, numeric text templates across changing render requests.
 *
 * <p>This cache deliberately stores only the template shape. Rendered textures continue to live
 * in {@link CosmicTextRenderer}'s normal caches, so disabling the feature or reloading a font
 * never leaves a stale GPU result behind.</p>
 */
final class CosmicAdaptiveTextTemplateCache {
    private static final int MAX_ENTRIES = 256;
    private static final int HOT_OBSERVATIONS = 8;
    private static final int MAX_TEXT_LENGTH = 128;
    private static final long STABLE_NANOS = 500_000_000L;
    private static final long DRAW_GAP_NANOS = 100_000_000L;

    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Key, Entry> eldest) {
            boolean remove = size() > MAX_ENTRIES;
            if (remove) evictions++;
            return remove;
        }
    };
    private long observations;
    private long compiled;
    private long hits;
    private long rejected;
    private long evictions;

    Plan observe(String text, int argb, int flags, float fontSize, float scale,
                 boolean enabled, long now) {
        if (!enabled) return null;
        Candidate candidate = candidate(text);
        if (candidate == null) {
            rejected++;
            return null;
        }
        Key key = new Key(candidate.skeleton, argb, flags,
                Float.floatToIntBits(fontSize), Float.floatToIntBits(scale));
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry(candidate.skeleton, now);
            entries.put(key, entry);
        }
        observations++;
        if (entry.lastText != null && !entry.lastText.equals(text)) entry.distinctValues++;
        entry.lastText = text;
        if (!entry.compiled) {
            if (entry.draws == 0 || now - entry.lastDraw > DRAW_GAP_NANOS) {
                entry.firstDraw = now;
                entry.draws = 0;
                entry.distinctValues = 0;
            }
            entry.draws++;
            entry.lastDraw = now;
            if (entry.draws >= HOT_OBSERVATIONS
                    && now - entry.firstDraw >= STABLE_NANOS
                    && entry.distinctValues > 0) {
                entry.compiled = true;
                compiled++;
            }
        }
        if (!entry.compiled) return null;
        List<Segment> current = instantiate(entry, text);
        if (current == null) {
            rejected++;
            entries.remove(key);
            return null;
        }
        hits++;
        return new Plan(current);
    }

    Stats stats() {
        int compiledEntries = 0;
        for (Entry entry : entries.values()) if (entry.compiled) compiledEntries++;
        return new Stats(entries.size(), compiledEntries, observations, compiled, hits, rejected, evictions);
    }

    void clear() {
        entries.clear();
        observations = 0L;
        compiled = 0L;
        hits = 0L;
        rejected = 0L;
        evictions = 0L;
    }

    private static List<Segment> instantiate(Entry entry, String text) {
        Candidate candidate = candidate(text);
        if (candidate == null || !entry.skeleton.equals(candidate.skeleton)) return null;
        return candidate.parts;
    }

    private static Candidate candidate(String text) {
        if (text == null || text.length() < 3 || text.length() > MAX_TEXT_LENGTH) return null;
        List<Segment> parts = new ArrayList<>();
        StringBuilder skeleton = new StringBuilder(text.length());
        int staticStart = 0;
        int slotCount = 0;
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current < 0x20 || current > 0x7E) return null;
            if (current < '0' || current > '9') {
                index++;
                continue;
            }
            int start = index;
            while (index < text.length()) {
                char digit = text.charAt(index);
                if (digit < '0' || digit > '9') break;
                index++;
            }
            int end = index;
            if (isIdentifierChar(text, start - 1) || isIdentifierChar(text, end)) return null;
            if (start > staticStart) {
                String value = text.substring(staticStart, start);
                parts.add(new Segment(value, false));
                skeleton.append(value);
            }
            parts.add(new Segment(text.substring(start, end), true));
            skeleton.append('\u0001');
            slotCount++;
            staticStart = end;
        }
        if (slotCount == 0 || staticStart == 0) return null;
        if (staticStart < text.length()) {
            String value = text.substring(staticStart);
            parts.add(new Segment(value, false));
            skeleton.append(value);
        }
        boolean hasStatic = false;
        for (Segment part : parts) if (!part.dynamic && !part.text.isEmpty()) {
            hasStatic = true;
            break;
        }
        if (!hasStatic) return null;
        return new Candidate(skeleton.toString(), List.copyOf(parts));
    }

    private static boolean isIdentifierChar(String text, int index) {
        if (index < 0 || index >= text.length()) return false;
        char value = text.charAt(index);
        return value == '_' || value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z';
    }

    private record Key(String skeleton, int argb, int flags, int fontSize, int scale) {}

    private static final class Entry {
        private final String skeleton;
        private long firstDraw;
        private long lastDraw;
        private int draws;
        private int distinctValues;
        private String lastText;
        private boolean compiled;

        private Entry(String skeleton, long now) {
            this.skeleton = skeleton;
            this.firstDraw = now;
        }
    }

    private record Candidate(String skeleton, List<Segment> parts) {}

    record Segment(String text, boolean dynamic) {}

    static final class Plan {
        private final List<Segment> segments;

        private Plan(List<Segment> segments) {
            this.segments = Collections.unmodifiableList(segments);
        }

        List<Segment> segments() { return segments; }
    }

    static final class Stats {
        final int entries;
        final int compiledEntries;
        final long observations;
        final long compiled;
        final long hits;
        final long rejected;
        final long evictions;

        private Stats(int entries, int compiledEntries, long observations, long compiled,
                      long hits, long rejected, long evictions) {
            this.entries = entries;
            this.compiledEntries = compiledEntries;
            this.observations = observations;
            this.compiled = compiled;
            this.hits = hits;
            this.rejected = rejected;
            this.evictions = evictions;
        }
    }
}
