package neofontrender.api.text.postprocess;

import neofontrender.api.text.ModernTextLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Registration and dispatch API for modern text post-processors. */
public final class TextPostProcessApi {
    public static final int API_VERSION = 1;

    private static final Logger LOGGER = LogManager.getLogger("NeoFontRender/TextPostProcess");
    private static final AtomicLong REVISION = new AtomicLong();
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private TextPostProcessApi() {
    }

    public static synchronized TextPostProcessRegistration register(TextPostProcessor processor) {
        Objects.requireNonNull(processor, "processor");
        String id = validateId(processor.id());
        ENTRIES.removeIf(entry -> entry.id.equals(id));
        Entry entry = new Entry(id, processor);
        ENTRIES.add(entry);
        rebuildSnapshot();
        return new TextPostProcessRegistration() {
            private boolean closed;

            @Override
            public synchronized void close() {
                if (closed) return;
                closed = true;
                synchronized (TextPostProcessApi.class) {
                    if (ENTRIES.remove(entry)) rebuildSnapshot();
                }
            }
        };
    }

    public static long revision() {
        return REVISION.get();
    }

    /** Immutable IDs of registered post-processors, for diagnostics and settings screens. */
    public static List<String> processorIds() {
        List<String> result = new ArrayList<>();
        for (Entry entry : snapshot.entries) if (enabled(entry)) result.add(entry.id);
        return java.util.Collections.unmodifiableList(result);
    }

    /** Immutable configured and last-executed state for diagnostics UIs. */
    public static List<ProcessorInfo> processorInfos() {
        List<ProcessorInfo> result = new ArrayList<>();
        for (Entry entry : snapshot.entries) {
            result.add(new ProcessorInfo(entry.id, enabled(entry), entry.lastOutcome,
                    entry.applicationCount));
        }
        return java.util.Collections.unmodifiableList(result);
    }

    public static void invalidate() {
        REVISION.incrementAndGet();
    }

    public static boolean hasEnabledProcessor() {
        for (Entry entry : snapshot.entries) {
            if (enabled(entry)) return true;
        }
        return false;
    }

    public static boolean isEnabled(String id) {
        if (id == null) return false;
        for (Entry entry : snapshot.entries) {
            if (entry.id.equals(id)) return enabled(entry);
        }
        return false;
    }

    public static ModernTextLayout process(TextPostProcessContext context,
                                            ModernTextLayout foreground) {
        Objects.requireNonNull(context, "context");
        ModernTextLayout current = Objects.requireNonNull(foreground, "foreground");
        if (snapshot.entries.length == 0 || Boolean.TRUE.equals(ACTIVE.get())) return current;

        ACTIVE.set(true);
        try {
            for (Entry entry : snapshot.entries) {
                if (!enabled(entry)) continue;
                TextPostProcessor processor = entry.processor;
                try {
                    if (!processor.supports(context)) continue;
                    ModernTextLayout result = processor.process(context, current);
                    if (result != null) {
                        current = result;
                        entry.lastOutcome = "applied";
                        entry.applicationCount++;
                    }
                } catch (RuntimeException | LinkageError error) {
                    entry.lastOutcome = "error";
                    reportOnce(entry, error);
                }
            }
            return current;
        } finally {
            ACTIVE.remove();
        }
    }

    static synchronized void clearForTests() {
        ENTRIES.clear();
        rebuildSnapshot();
    }

    private static boolean enabled(Entry entry) {
        try {
            return entry.processor.isEnabled();
        } catch (RuntimeException | LinkageError error) {
            reportOnce(entry, error);
            return false;
        }
    }

    private static synchronized void rebuildSnapshot() {
        List<Entry> ordered = new ArrayList<>(ENTRIES);
        ordered.sort(Comparator.comparingInt((Entry entry) -> entry.processor.priority()).reversed()
                .thenComparing(entry -> entry.id));
        snapshot = new Snapshot(ordered.toArray(new Entry[0]));
        REVISION.incrementAndGet();
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(
                    "Post-processor id must be namespaced, for example modid:effect");
        }
        return id;
    }

    private static synchronized void reportOnce(Entry entry, Throwable error) {
        if (entry.reported) return;
        entry.reported = true;
        LOGGER.warn("Text post-processor '{}' failed; continuing with the next provider",
                entry.id, error);
    }

    private static final class Entry {
        final String id;
        final TextPostProcessor processor;
        boolean reported;
        volatile String lastOutcome = "idle";
        volatile long applicationCount;

        Entry(String id, TextPostProcessor processor) {
            this.id = id;
            this.processor = processor;
        }
    }

    public static final class ProcessorInfo {
        public final String id;
        public final boolean enabled;
        public final String lastOutcome;
        public final long applicationCount;

        private ProcessorInfo(String id, boolean enabled, String lastOutcome,
                              long applicationCount) {
            this.id = id;
            this.enabled = enabled;
            this.lastOutcome = lastOutcome;
            this.applicationCount = applicationCount;
        }
    }

    private static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(new Entry[0]);
        final Entry[] entries;

        Snapshot(Entry[] entries) {
            this.entries = entries;
        }
    }
}
