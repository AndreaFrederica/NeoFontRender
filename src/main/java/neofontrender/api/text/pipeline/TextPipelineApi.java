package neofontrender.api.text.pipeline;

import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Public registration and dispatch API for NFR's staged text middleware pipeline. */
public final class TextPipelineApi {
    public static final int API_VERSION = 1;

    private static final Logger LOGGER = LogManager.getLogger("NeoFontRender/TextPipeline");
    private static final AtomicLong REVISION = new AtomicLong();
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final int STAGE_RAW = 1;
    private static final int STAGE_INLINE = 1 << 1;
    private static final int STAGE_PARAGRAPH = 1 << 2;
    private static final ThreadLocal<Integer> ACTIVE_STAGES = ThreadLocal.withInitial(() -> 0);
    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private TextPipelineApi() {}

    public static synchronized TextMiddlewareRegistration register(TextMiddleware middleware) {
        Objects.requireNonNull(middleware, "middleware");
        String id = validateId(middleware.id());
        if (!(middleware instanceof RawTextMiddleware)
                && !(middleware instanceof InlineContentMiddleware)
                && !(middleware instanceof ParagraphLayoutMiddleware)) {
            throw new IllegalArgumentException(
                    "Middleware must implement a supported stage-specific interface: " + id);
        }
        ENTRIES.removeIf(entry -> entry.id.equals(id));
        Entry entry = new Entry(id, middleware);
        ENTRIES.add(entry);
        rebuildSnapshot();
        return new TextMiddlewareRegistration() {
            private boolean closed;

            @Override
            public synchronized void close() {
                if (closed) return;
                closed = true;
                synchronized (TextPipelineApi.class) {
                    if (ENTRIES.remove(entry)) rebuildSnapshot();
                }
            }
        };
    }

    public static long revision() {
        return REVISION.get();
    }

    /** Invalidates processed-layout caches after a middleware's dynamic configuration changes. */
    public static void invalidate() {
        REVISION.incrementAndGet();
    }

    public static boolean hasInlineContentMiddleware() {
        Snapshot current = snapshot;
        if (!current.hasInline) return false;
        long revision = REVISION.get();
        if (current.enabledRevision == revision) return current.hasEnabledInline;
        synchronized (current) {
            if (current.enabledRevision != revision) {
                boolean anyEnabled = false;
                for (Entry entry : current.inline) {
                    if (enabled(entry)) {
                        anyEnabled = true;
                        break;
                    }
                }
                current.hasEnabledInline = anyEnabled;
                current.enabledRevision = revision;
            }
            return current.hasEnabledInline;
        }
    }

    public static ProcessedText processRaw(String rawText) {
        String source = rawText == null ? "" : rawText;
        Snapshot current = snapshot;
        if (current.raw.length == 0 || !enterStage(STAGE_RAW)) {
            return ProcessedText.unchanged(source);
        }
        try {
            ProcessedText processed = ProcessedText.unchanged(source);
            for (Entry entry : current.raw) {
                RawTextMiddleware middleware = (RawTextMiddleware) entry.middleware;
                if (!enabled(entry)
                        || !containsTrigger(processed.visibleText(), middleware.trigger())) continue;
                try {
                    ProcessedText result = middleware.process(processed);
                    if (result != null) processed = result;
                } catch (RuntimeException | LinkageError error) {
                    reportOnce(entry, "raw transform", error);
                }
            }
            return processed;
        } finally {
            exitStage(STAGE_RAW);
        }
    }

    @Nullable
    static InlineContentMatch matchInline(CharSequence source, int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= source.length()) return null;
        Snapshot current = snapshot;
        char trigger = source.charAt(sourceIndex);
        Entry[] exact = trigger < current.asciiInline.length
                ? current.asciiInline[trigger] : Snapshot.NO_ENTRIES;
        boolean rangedCandidate = hasMatchingTrigger(current.rangedInline, trigger);
        if (exact.length == 0 && !rangedCandidate || !beginInlineDispatch()) return null;
        try {
            InlineContentMatch result = matchEntries(exact, source, sourceIndex, trigger);
            return result != null ? result
                    : matchEntries(current.rangedInline, source, sourceIndex, trigger);
        } finally {
            endInlineDispatch();
        }
    }

    @Nullable
    public static ParagraphLayoutMiddleware.Layout layoutParagraph(
            ParagraphLayoutMiddleware.Request request) {
        if (!enterStage(STAGE_PARAGRAPH)) return null;
        try {
            for (Entry entry : snapshot.paragraph) {
                if (!enabled(entry)) continue;
                try {
                    ParagraphLayoutMiddleware.Layout result =
                            ((ParagraphLayoutMiddleware) entry.middleware).layout(request);
                    if (result != null) return result;
                } catch (RuntimeException | LinkageError error) {
                    reportOnce(entry, "paragraph layout", error);
                }
            }
            return null;
        } finally {
            exitStage(STAGE_PARAGRAPH);
        }
    }

    @Nullable
    public static List<ITextComponent> splitComponents(
            ParagraphLayoutMiddleware.ComponentRequest request) {
        if (!enterStage(STAGE_PARAGRAPH)) return null;
        try {
            for (Entry entry : snapshot.paragraph) {
                if (!enabled(entry)) continue;
                try {
                    List<ITextComponent> result =
                            ((ParagraphLayoutMiddleware) entry.middleware).splitComponents(request);
                    if (result != null) return result;
                } catch (RuntimeException | LinkageError error) {
                    reportOnce(entry, "component split", error);
                }
            }
            return null;
        } finally {
            exitStage(STAGE_PARAGRAPH);
        }
    }

    static boolean beginInlineDispatch() {
        return enterStage(STAGE_INLINE);
    }

    static void endInlineDispatch() {
        exitStage(STAGE_INLINE);
    }

    /** Prevent recursion within one stage while allowing later stages to consume earlier ones. */
    private static boolean enterStage(int stage) {
        int active = ACTIVE_STAGES.get();
        if ((active & stage) != 0) return false;
        ACTIVE_STAGES.set(active | stage);
        return true;
    }

    private static void exitStage(int stage) {
        int active = ACTIVE_STAGES.get() & ~stage;
        if (active == 0) ACTIVE_STAGES.remove();
        else ACTIVE_STAGES.set(active);
    }

    static synchronized void clearForTests() {
        ENTRIES.clear();
        rebuildSnapshot();
    }

    @Nullable
    private static InlineContentMatch matchEntries(Entry[] entries, CharSequence source,
                                                   int sourceIndex, char trigger) {
        for (Entry entry : entries) {
            InlineContentMiddleware middleware = (InlineContentMiddleware) entry.middleware;
            if (!middleware.trigger().matches(trigger) || !enabled(entry)) continue;
            try {
                InlineContentMatch match = middleware.match(source, sourceIndex);
                if (match == null) continue;
                if (match.start() != sourceIndex || match.end() > source.length()) {
                    throw new IllegalStateException("Middleware returned an invalid source range");
                }
                return match;
            } catch (RuntimeException | LinkageError error) {
                reportOnce(entry, "inline match", error);
            }
        }
        return null;
    }

    private static boolean enabled(Entry entry) {
        try {
            return entry.middleware.isEnabled();
        } catch (RuntimeException | LinkageError error) {
            reportOnce(entry, "availability check", error);
            return false;
        }
    }

    private static boolean containsTrigger(CharSequence source, TextTrigger trigger) {
        for (int index = 0; index < source.length(); index++) {
            if (trigger.matches(source.charAt(index))) return true;
        }
        return false;
    }

    private static boolean hasMatchingTrigger(Entry[] entries, char trigger) {
        for (Entry entry : entries) {
            if (((InlineContentMiddleware) entry.middleware).trigger().matches(trigger)) return true;
        }
        return false;
    }

    private static synchronized void rebuildSnapshot() {
        List<Entry> ordered = new ArrayList<>(ENTRIES);
        ordered.sort(Comparator.comparingInt((Entry entry) -> entry.middleware.priority()).reversed()
                .thenComparing(entry -> entry.id));

        @SuppressWarnings("unchecked")
        List<Entry>[] asciiLists = new List[128];
        List<Entry> rangedInline = new ArrayList<>();
        List<Entry> inline = new ArrayList<>();
        List<Entry> raw = new ArrayList<>();
        List<Entry> paragraph = new ArrayList<>();
        for (Entry entry : ordered) {
            if (entry.middleware instanceof InlineContentMiddleware) {
                inline.add(entry);
                TextTrigger trigger = ((InlineContentMiddleware) entry.middleware).trigger();
                if (trigger.hasRange()) {
                    rangedInline.add(entry);
                } else {
                    for (char character : trigger.exactCharacters()) {
                        if (character < asciiLists.length) {
                            if (asciiLists[character] == null) {
                                asciiLists[character] = new ArrayList<>();
                            }
                            asciiLists[character].add(entry);
                        } else if (!rangedInline.contains(entry)) {
                            rangedInline.add(entry);
                        }
                    }
                }
            }
            if (entry.middleware instanceof RawTextMiddleware) raw.add(entry);
            if (entry.middleware instanceof ParagraphLayoutMiddleware) paragraph.add(entry);
        }
        Entry[][] ascii = new Entry[asciiLists.length][];
        for (int index = 0; index < ascii.length; index++) {
            ascii[index] = asciiLists[index] == null
                    ? Snapshot.NO_ENTRIES : asciiLists[index].toArray(new Entry[0]);
        }
        snapshot = new Snapshot(ascii, rangedInline.toArray(new Entry[0]),
                inline.toArray(new Entry[0]),
                raw.toArray(new Entry[0]), paragraph.toArray(new Entry[0]),
                !rangedInline.isEmpty() || hasAsciiEntries(ascii));
        REVISION.incrementAndGet();
    }

    private static boolean hasAsciiEntries(Entry[][] entries) {
        for (Entry[] values : entries) if (values.length != 0) return true;
        return false;
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(
                    "Middleware id must be namespaced, for example modid:feature");
        }
        return id;
    }

    private static synchronized void reportOnce(Entry entry, String operation, Throwable error) {
        if (entry.reportedFailures.add(operation)) {
            LOGGER.warn("Text middleware '{}' failed during {}; continuing with the next provider",
                    entry.id, operation, error);
        }
    }

    private static final class Entry {
        final String id;
        final TextMiddleware middleware;
        final Set<String> reportedFailures = new HashSet<>();

        Entry(String id, TextMiddleware middleware) {
            this.id = id;
            this.middleware = middleware;
        }
    }

    private static final class Snapshot {
        static final Entry[] NO_ENTRIES = new Entry[0];
        static final Snapshot EMPTY = new Snapshot(emptyAscii(), NO_ENTRIES, NO_ENTRIES,
                NO_ENTRIES, NO_ENTRIES, false);

        final Entry[][] asciiInline;
        final Entry[] rangedInline;
        final Entry[] inline;
        final Entry[] raw;
        final Entry[] paragraph;
        final boolean hasInline;
        volatile long enabledRevision = Long.MIN_VALUE;
        volatile boolean hasEnabledInline;

        Snapshot(Entry[][] asciiInline, Entry[] rangedInline, Entry[] inline, Entry[] raw,
                 Entry[] paragraph, boolean hasInline) {
            this.asciiInline = asciiInline;
            this.rangedInline = rangedInline;
            this.inline = inline;
            this.raw = raw;
            this.paragraph = paragraph;
            this.hasInline = hasInline;
        }

        private static Entry[][] emptyAscii() {
            Entry[][] result = new Entry[128][];
            java.util.Arrays.fill(result, NO_ENTRIES);
            return result;
        }
    }
}
