package neofontrender.addons.api.inline;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Public registration point for UIE's standard inline-font middleware. */
public final class InlineGlyphRegistry {
    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final AtomicLong ORDER = new AtomicLong();

    private InlineGlyphRegistry() {}

    /**
     * Registers a provider. Higher priorities run first; equal priorities retain registration
     * order. Closing the returned handle unregisters only this registration.
     */
    public static synchronized AutoCloseable register(InlineGlyphProvider provider, int priority) {
        Entry entry = new Entry(Objects.requireNonNull(provider, "provider"), priority,
                ORDER.getAndIncrement());
        List<Entry> updated = new ArrayList<>(ENTRIES);
        updated.add(entry);
        updated.sort(Comparator.comparingInt((Entry value) -> value.priority).reversed()
                .thenComparingLong(value -> value.order));
        ENTRIES.clear();
        ENTRIES.addAll(updated);
        return () -> ENTRIES.remove(entry);
    }

    @Nullable
    public static InlineGlyphMatch match(CharSequence source, int sourceIndex) {
        for (Entry entry : ENTRIES) {
            InlineGlyphMatch match = entry.provider.match(source, sourceIndex);
            if (match == null) continue;
            if (match.start() != sourceIndex || match.end() > source.length()) {
                throw new IllegalStateException("Inline glyph provider returned an invalid range");
            }
            return match;
        }
        return null;
    }

    private static final class Entry {
        private final InlineGlyphProvider provider;
        private final int priority;
        private final long order;

        private Entry(InlineGlyphProvider provider, int priority, long order) {
            this.provider = provider;
            this.priority = priority;
            this.order = order;
        }
    }
}
