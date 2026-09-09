package neofontrender.api.text.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.core.font.route.TextRenderRoutes;
import neofontrender.core.font.support.ScopedFontRenderBypass;
import neofontrender.text.edit.SourceEditProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Public registry and single dispatch point for FontRenderer text routes. */
public final class TextRenderRouteApi {
    public static final int API_VERSION = 1;

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final int CACHE_LIMIT_PER_FONT = 512;
    private static final Map<FontRenderer, LayoutCache> CACHES = new WeakHashMap<>();
    private static volatile List<Entry> snapshot = Collections.emptyList();
    private static volatile RouteInfo lastRoute = RouteInfo.EMPTY;

    private TextRenderRouteApi() {}

    public static synchronized TextRenderRouteRegistration register(TextRenderRoute route) {
        Objects.requireNonNull(route, "route");
        String id = validateId(route.id());
        ENTRIES.removeIf(entry -> entry.id.equals(id));
        Entry entry = new Entry(id, route);
        ENTRIES.add(entry);
        rebuild();
        return new TextRenderRouteRegistration() {
            private boolean closed;

            @Override
            public synchronized void close() {
                if (closed) return;
                closed = true;
                synchronized (TextRenderRouteApi.class) {
                    if (ENTRIES.remove(entry)) rebuild();
                }
            }
        };
    }

    public static TextRenderRouteLayout layout(FontRenderer font, String source,
                                                int argb, boolean shadow) {
        TextRenderRoutes.initialize();
        String text = source == null ? "" : source;
        if (ScopedFontRenderBypass.isActive() || Boolean.TRUE.equals(ACTIVE.get())) {
            return TextRenderRoutes.passthrough(font, text, argb, shadow, "reentrant_bypass");
        }
        long revision = REVISION.get();
        long structuredRevision = StructuredTextRuntime.revision();
        CacheKey cacheKey = new CacheKey(text, argb, shadow,
                Float.floatToIntBits(NeofontrenderConfig.fontSize()), revision,
                structuredRevision);
        TextRenderRouteLayout cached = cached(font, cacheKey);
        if (cached != null) {
            lastRoute = new RouteInfo(cached.routeId(), "cached", 0,
                    TextRenderRouteRequest.requiresStructuredRendering(
                            cached.structuredText(), false));
            return cached;
        }
        ACTIVE.set(true);
        try {
            TextRenderRouteRequest request = new TextRenderRouteRequest(font, text,
                    StructuredTextRuntime.parse(text), NeofontrenderConfig.fontSize(),
                    argb, shadow, false);
            for (Entry entry : snapshot) {
                TextRenderRoute route = entry.route;
                boolean enabled;
                try {
                    enabled = route.isEnabled();
                } catch (RuntimeException | LinkageError error) {
                    entry.lastOutcome = "enable_error";
                    continue;
                }
                if (!enabled) continue;
                try {
                    if (!route.supports(request)) continue;
                    TextRenderRouteLayout result = route.layout(request);
                    if (result == null) continue;
                    entry.lastOutcome = result.handled() ? "handled" : "passthrough";
                    entry.selectionCount++;
                    lastRoute = new RouteInfo(entry.id, entry.lastOutcome,
                            entry.selectionCount, request.requiresStructuredRendering());
                    if (!result.structuredText().animated()) cache(font, cacheKey, result);
                    return result;
                } catch (RuntimeException | LinkageError error) {
                    entry.lastOutcome = "route_error";
                }
            }
            TextRenderRouteLayout fallback = TextRenderRoutes.passthrough(
                    font, text, argb, shadow, "no_route");
            lastRoute = new RouteInfo(fallback.routeId(), "passthrough", 0,
                    request.requiresStructuredRendering());
            cache(font, cacheKey, fallback);
            return fallback;
        } finally {
            ACTIVE.remove();
        }
    }

    public static TextRenderRouteLayout layout(FontRenderer font, String source) {
        return layout(font, source, 0xFFFFFFFF, false);
    }

    public static int width(FontRenderer font, String source) {
        return (int) Math.ceil(layout(font, source).advance());
    }

    public static int height(FontRenderer font, String source) {
        return (int) Math.ceil(layout(font, source).height());
    }

    /** Source ranges recognized by the same parser used by structured rendering. */
    public static SourceEditProjection sourceProjection(String source) {
        neofontrender.text.StructuredText parsed =
                StructuredTextRuntime.parse(source == null ? "" : source);
        // A disabled provider must make its syntax literal and must not leave an
        // unresolved span that can activate Tabby's source editor. Other route
        // protocols remain eligible when they have their own recognized spans.
        if (parsed.appliedSyntaxProviderIds().isEmpty()
                && parsed.inlineSpans().isEmpty()
                && parsed.effects().isEmpty()) {
            return SourceEditProjection.empty();
        }
        return SourceEditProjection.of(parsed);
    }

    /** Wraps at route-owned atomic boundaries and carries vanilla formatting into continuation lines. */
    public static List<String> wrap(FontRenderer font, String source, int maximumWidth) {
        String remaining = source == null ? "" : source;
        if (remaining.isEmpty()) return Collections.singletonList("");
        List<String> lines = new ArrayList<>();
        String carriedFormat = "";
        int sourceRemaining = remaining.length();
        int guard = 0;
        while (!remaining.isEmpty() && guard++ <= sourceRemaining + 1) {
            String candidate = carriedFormat + remaining;
            TextRenderRouteLayout current = layout(font, candidate);
            int boundary = current.handled()
                    ? current.sizeToWidth(Math.max(1, maximumWidth), true)
                    : ScopedFontRenderBypass.call(() -> font.sizeStringToWidth(
                    candidate, Math.max(1, maximumWidth)));
            int minimum = carriedFormat.length() + Character.charCount(remaining.codePointAt(0));
            boundary = Math.max(minimum, Math.min(candidate.length(), boundary));
            int consumed = Math.max(1, boundary - carriedFormat.length());
            consumed = Math.min(remaining.length(), consumed);
            String line = carriedFormat + remaining.substring(0, consumed);
            lines.add(line);
            int next = consumed;
            if (next < remaining.length() && (remaining.charAt(next) == '\n'
                    || remaining.charAt(next) == ' ')) next++;
            carriedFormat = FontRenderer.getFormatFromString(line);
            remaining = remaining.substring(next);
        }
        if (!remaining.isEmpty()) lines.add(carriedFormat + remaining);
        return Collections.unmodifiableList(lines);
    }

    /**
     * Editing wrap: preserves source coordinates and allows a structured token to
     * split while the user is editing it. Preview layout may keep tokens atomic,
     * but an editor must never make caret positions unreachable.
     */
    public static List<String> wrapEditable(FontRenderer font, String source, int maximumWidth) {
        String text = source == null ? "" : source;
        if (text.isEmpty()) return Collections.singletonList("");
        int width = Math.max(1, maximumWidth);
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            int lastBreak = -1;
            while (end < text.length()) {
                int next = end + Character.charCount(text.codePointAt(end));
                if (text.charAt(end) == '\n') { end = next; break; }
                String candidate = text.substring(start, next);
                if (ScopedFontRenderBypass.call(() -> font.getStringWidth(candidate)) > width
                        && end > start) break;
                if (Character.isWhitespace(text.charAt(end))) lastBreak = next;
                end = next;
            }
            if (end == start) end += Character.charCount(text.codePointAt(start));
            int cut = lastBreak > start && end < text.length() ? lastBreak : end;
            lines.add(text.substring(start, cut));
            // Keep every source character in the visual line mapping. Skipping
            // wrapping spaces makes caret and hit-test offsets drift.
            start = cut;
        }
        return Collections.unmodifiableList(lines);
    }

    public static long revision() {
        return REVISION.get();
    }

    public static void invalidate() {
        StructuredTextRuntime.invalidate();
        clearCache();
        REVISION.incrementAndGet();
    }

    public static List<RouteInfo> routes() {
        TextRenderRoutes.initialize();
        List<RouteInfo> result = new ArrayList<>();
        for (Entry entry : snapshot) {
            boolean enabled;
            try {
                enabled = entry.route.isEnabled();
            } catch (RuntimeException | LinkageError ignored) {
                enabled = false;
            }
            result.add(new RouteInfo(entry.id, enabled ? entry.lastOutcome : "disabled",
                    entry.selectionCount, false));
        }
        return Collections.unmodifiableList(result);
    }

    public static RouteInfo lastRoute() {
        return lastRoute;
    }

    private static synchronized void rebuild() {
        List<Entry> ordered = new ArrayList<>(ENTRIES);
        ordered.sort(Comparator.comparingInt((Entry entry) -> entry.route.priority()).reversed()
                .thenComparing(entry -> entry.id));
        snapshot = Collections.unmodifiableList(ordered);
        clearCache();
        REVISION.incrementAndGet();
    }

    private static synchronized TextRenderRouteLayout cached(FontRenderer font, CacheKey key) {
        LayoutCache cache = CACHES.get(font);
        return cache == null ? null : cache.get(key);
    }

    private static synchronized void cache(FontRenderer font, CacheKey key,
                                           TextRenderRouteLayout layout) {
        CACHES.computeIfAbsent(font, ignored -> new LayoutCache()).put(key, layout);
    }

    private static synchronized void clearCache() {
        CACHES.clear();
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Route id must be namespaced: " + id);
        }
        return id;
    }

    private static final class Entry {
        final String id;
        final TextRenderRoute route;
        volatile String lastOutcome = "idle";
        volatile long selectionCount;

        Entry(String id, TextRenderRoute route) {
            this.id = id;
            this.route = route;
        }
    }

    private static final class CacheKey {
        final String source;
        final int argb;
        final boolean shadow;
        final int fontSizeBits;
        final long revision;
        final long structuredRevision;

        CacheKey(String source, int argb, boolean shadow, int fontSizeBits, long revision,
                 long structuredRevision) {
            this.source = source;
            this.argb = argb;
            this.shadow = shadow;
            this.fontSizeBits = fontSizeBits;
            this.revision = revision;
            this.structuredRevision = structuredRevision;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CacheKey)) return false;
            CacheKey key = (CacheKey) other;
            return argb == key.argb && shadow == key.shadow && fontSizeBits == key.fontSizeBits
                    && revision == key.revision && structuredRevision == key.structuredRevision
                    && source.equals(key.source);
        }

        @Override public int hashCode() {
            return Objects.hash(source, argb, shadow, fontSizeBits, revision, structuredRevision);
        }
    }

    private static final class LayoutCache extends LinkedHashMap<CacheKey, TextRenderRouteLayout> {
        LayoutCache() {
            super(CACHE_LIMIT_PER_FONT + 1, 0.75F, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, TextRenderRouteLayout> eldest) {
            return size() > CACHE_LIMIT_PER_FONT;
        }
    }

    public static final class RouteInfo {
        static final RouteInfo EMPTY = new RouteInfo("", "idle", 0, false);
        public final String id;
        public final String outcome;
        public final long selectionCount;
        public final boolean structuredContribution;

        private RouteInfo(String id, String outcome, long selectionCount,
                          boolean structuredContribution) {
            this.id = id;
            this.outcome = outcome;
            this.selectionCount = selectionCount;
            this.structuredContribution = structuredContribution;
        }
    }
}
