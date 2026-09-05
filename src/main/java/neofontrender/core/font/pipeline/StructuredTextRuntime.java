package neofontrender.core.font.pipeline;

import neofontrender.api.text.ModernText;
import neofontrender.api.text.StructuredTextRegistration;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.text.SourceMap;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.StyledSpan;
import neofontrender.text.TextStyle;
import neofontrender.text.syntax.BrilliantSyntaxProvider;
import neofontrender.text.syntax.MinecraftLegacySyntaxProvider;
import neofontrender.text.syntax.TextSyntaxEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import neofontrender.text.syntax.TextSyntaxProvider;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextPipeline;
import neofontrender.text.pipeline.TextPipelinePlugin;
import neofontrender.text.pipeline.LineBreakOpportunityProvider;
import neofontrender.api.text.route.InlineContentResolver;
import neofontrender.text.InlineSpan;

/** Minecraft configuration adapter for the game-independent structured syntax engine. */
public final class StructuredTextRuntime {
    private static final int PARSED_CACHE_LIMIT = 512;
    private static volatile TextSyntaxEngine engine;
    private static volatile int signature;
    private static volatile int[] colorCodes = neofontrender.api.color.TextColorPaletteRegistry.vanillaColorCodes();
    private static final Map<String, TextSyntaxProvider> EXTERNAL_PROVIDERS = new LinkedHashMap<>();
    private static final Map<String, StructuredTextMiddleware> STRUCTURED_MIDDLEWARE =
            new LinkedHashMap<>();
    private static final Map<String, LineBreakOpportunityProvider> LINE_BREAK_PROVIDERS =
            new LinkedHashMap<>();
    private static final Map<String, InlineContentResolver> INLINE_RESOLVERS =
            new LinkedHashMap<>();
    private static final Map<ParseKey, StructuredText> PARSED_CACHE =
            new LinkedHashMap<>(PARSED_CACHE_LIMIT + 1, 0.75F, true) {
                @Override protected boolean removeEldestEntry(
                        Map.Entry<ParseKey, StructuredText> eldest) {
                    return size() > PARSED_CACHE_LIMIT;
                }
            };
    private static long externalRevision;
    private static volatile Diagnostics lastDiagnostics = Diagnostics.EMPTY;
    private static volatile List<String> lastLineBreakProviderIds = Collections.emptyList();

    private StructuredTextRuntime() {}

    public static synchronized void updateColorCodes(int[] values) {
        int[] normalized = neofontrender.api.color.TextColorPaletteRegistry.normalizeColorCodes(values);
        if (java.util.Arrays.equals(colorCodes, normalized)) return;
        colorCodes = normalized;
        externalRevision++;
        signature = 0;
        engine = null;
        PARSED_CACHE.clear();
    }

    public static synchronized StructuredTextRegistration register(TextSyntaxProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (provider.id().equals("minecraft:legacy_formatting")
                || provider.id().equals("brilliant_text:format_codes")) {
            throw new IllegalArgumentException("Built-in syntax provider id is reserved: " + provider.id());
        }
        TextSyntaxProvider previous = EXTERNAL_PROVIDERS.put(provider.id(), provider);
        try {
            buildEngine(NeofontrenderConfig.brilliantTextBindings(),
                    NeofontrenderConfig.brilliantTextEnabled());
        } catch (RuntimeException error) {
            if (previous == null) EXTERNAL_PROVIDERS.remove(provider.id());
            else EXTERNAL_PROVIDERS.put(provider.id(), previous);
            throw error;
        }
        externalRevision++;
        engine = null;
        PARSED_CACHE.clear();
        return new StructuredTextRegistration() {
            private boolean closed;
            @Override public synchronized void close() {
                if (closed) return;
                closed = true;
                synchronized (StructuredTextRuntime.class) {
                    if (EXTERNAL_PROVIDERS.get(provider.id()) == provider) {
                        EXTERNAL_PROVIDERS.remove(provider.id());
                        externalRevision++;
                        engine = null;
                        PARSED_CACHE.clear();
                    }
                }
            }
        };
    }

    /** Registers a game-independent plugin directly into the modern structured pipeline. */
    public static synchronized StructuredTextRegistration register(TextPipelinePlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        List<String> added = new ArrayList<>();
        for (StructuredTextMiddleware middleware : plugin.structuredMiddlewares()) {
            String id = middleware.id();
            if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Structured middleware id must be namespaced: " + id);
            }
            if (STRUCTURED_MIDDLEWARE.putIfAbsent(id, middleware) != null) {
                for (String rollback : added) STRUCTURED_MIDDLEWARE.remove(rollback);
                throw new IllegalArgumentException("Duplicate structured middleware id: " + id);
            }
            added.add(id);
        }
        List<String> addedBreaks = new ArrayList<>();
        for (LineBreakOpportunityProvider provider : plugin.lineBreakProviders()) {
            String id = provider.id();
            if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                    || LINE_BREAK_PROVIDERS.putIfAbsent(id, provider) != null) {
                for (String rollback : added) STRUCTURED_MIDDLEWARE.remove(rollback);
                for (String rollback : addedBreaks) LINE_BREAK_PROVIDERS.remove(rollback);
                throw new IllegalArgumentException("Duplicate or invalid line-break provider id: " + id);
            }
            addedBreaks.add(id);
        }
        externalRevision++;
        engine = null;
        PARSED_CACHE.clear();
        return new StructuredTextRegistration() {
            private boolean closed;
            @Override public synchronized void close() {
                if (closed) return;
                closed = true;
                synchronized (StructuredTextRuntime.class) {
                    for (String id : added) STRUCTURED_MIDDLEWARE.remove(id);
                    for (String id : addedBreaks) LINE_BREAK_PROVIDERS.remove(id);
                    externalRevision++;
                    engine = null;
                    PARSED_CACHE.clear();
                }
            }
        };
    }

    public static synchronized StructuredTextRegistration register(InlineContentResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        String id = resolver.id();
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Inline resolver id must be namespaced: " + id);
        }
        InlineContentResolver previous = INLINE_RESOLVERS.put(id, resolver);
        externalRevision++;
        engine = null;
        PARSED_CACHE.clear();
        return new StructuredTextRegistration() {
            private boolean closed;
            @Override public synchronized void close() {
                if (closed) return;
                closed = true;
                synchronized (StructuredTextRuntime.class) {
                    if (INLINE_RESOLVERS.get(id) == resolver) {
                        if (previous == null) INLINE_RESOLVERS.remove(id);
                        else INLINE_RESOLVERS.put(id, previous);
                        externalRevision++;
                        engine = null;
                        PARSED_CACHE.clear();
                    }
                }
            }
        };
    }

    public static synchronized List<String> providerIds() {
        return currentEngine().providerIds();
    }

    public static synchronized List<String> middlewareIds() {
        return currentPipeline().middlewareIds();
    }

    public static synchronized List<String> lineBreakProviderIds() {
        return currentPipeline().lineBreakProviderIds();
    }

    public static List<Integer> breakOpportunities(StructuredText text) {
        StructuredTextPipeline pipeline = currentPipeline();
        List<Integer> result = pipeline.breakOpportunities(text);
        lastLineBreakProviderIds = pipeline.lineBreakProviderHits(text);
        return result;
    }

    public static List<String> lastLineBreakProviderIds() {
        return lastLineBreakProviderIds;
    }

    /** Invalidates syntax and structured middleware snapshots after dynamic content changes. */
    public static synchronized void invalidate() {
        externalRevision++;
        engine = null;
        PARSED_CACHE.clear();
    }

    /** Changes whenever parsing, middleware, line-break, or inline-resolution output may change. */
    public static long revision() {
        currentEngine();
        synchronized (StructuredTextRuntime.class) {
            return ((long) signature << 32) ^ externalRevision;
        }
    }

    /** Returns the legacy color code for an exact configured palette color, or {@code null}. */
    public static synchronized Character legacyColorCode(int rgb) {
        int target = rgb & 0xFFFFFF;
        String codes = "0123456789abcdef";
        for (int index = 0; index < 16 && index < colorCodes.length; index++) {
            if ((colorCodes[index] & 0xFFFFFF) == target) return codes.charAt(index);
        }
        return null;
    }

    /** Parses a raw source string through syntax, structured middleware, and inline resolution. */
    public static StructuredText parse(String source) {
        String normalized = source == null ? "" : source;
        ParseKey key = new ParseKey(normalized, revision());
        StructuredText result;
        synchronized (StructuredTextRuntime.class) {
            result = PARSED_CACHE.get(key);
        }
        if (result == null) {
            result = resolveInline(currentPipeline().parse(normalized));
            synchronized (StructuredTextRuntime.class) {
                PARSED_CACHE.put(key, result);
            }
        }
        publishDiagnostics(result);
        return result;
    }

    public static StructuredText parse(ModernText modern) {
        if (modern == null || modern.isEmpty()) return currentEngine().parse("");
        StringBuilder source = new StringBuilder();
        StringBuilder plain = new StringBuilder();
        List<StyledSpan> styles = new ArrayList<>();
        List<StructuredEffectSpan> effects = new ArrayList<>();
        List<neofontrender.text.UnresolvedSyntax> unresolved = new ArrayList<>();
        java.util.LinkedHashSet<String> appliedProviders = new java.util.LinkedHashSet<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        starts.add(0);
        ends.add(0);
        for (ModernText.Run run : modern.runs()) {
            StructuredText parsed = currentEngine().parse(run.text());
            int plainOffset = plain.length();
            int sourceOffset = source.length();
            source.append(run.text());
            plain.append(parsed.plainText());
            for (StyledSpan span : parsed.styles()) {
                TextStyle style = run.hasColorOverride()
                        ? span.style().withColorOverride(run.rgb()) : span.style();
                styles.add(new StyledSpan(plainOffset + span.start(), plainOffset + span.end(), style));
            }
            for (StructuredEffectSpan effect : parsed.effects()) {
                effects.add(new StructuredEffectSpan(plainOffset + effect.start(),
                        plainOffset + effect.end(), effect.effectId(), effect.parameters(),
                        effect.lineWide()));
            }
            for (neofontrender.text.UnresolvedSyntax value : parsed.unresolvedSyntax()) {
                unresolved.add(new neofontrender.text.UnresolvedSyntax(
                        sourceOffset + value.sourceStart(), sourceOffset + value.sourceEnd(), value.source()));
            }
            appliedProviders.addAll(parsed.appliedSyntaxProviderIds());
            if (sourceOffset == 0) {
                starts.set(0, sourceOffset + parsed.sourceMap().sourceStart(0));
            }
            ends.set(plainOffset, sourceOffset + parsed.sourceMap().sourceEnd(0));
            for (int index = 1; index <= parsed.plainText().length(); index++) {
                starts.add(sourceOffset + parsed.sourceMap().sourceStart(index));
                ends.add(sourceOffset + parsed.sourceMap().sourceEnd(index));
            }
        }
        StructuredText result = new StructuredText(source.toString(), plain.toString(), styles, effects, unresolved,
                new SourceMap(source.length(), toArray(starts), toArray(ends)),
                new ArrayList<>(appliedProviders));
        result = resolveInline(currentPipeline().process(result));
        publishDiagnostics(result);
        return result;
    }

    public static Diagnostics lastDiagnostics() {
        return lastDiagnostics;
    }

    private static void publishDiagnostics(StructuredText text) {
        if (text.appliedSyntaxProviderIds().isEmpty() && text.appliedMiddlewareIds().isEmpty()
                && text.effects().isEmpty() && text.inlineSpans().isEmpty()
                && text.unresolvedSyntax().isEmpty() && !text.animated()) return;
        lastDiagnostics = new Diagnostics(text.appliedSyntaxProviderIds(),
                text.appliedMiddlewareIds(), text.styles().size(), text.effects().size(),
                text.inlineSpans().size(), text.unresolvedSyntax().size(), text.animated());
    }

    private static StructuredText resolveInline(StructuredText text) {
        if (text.inlineSpans().isEmpty() || INLINE_RESOLVERS.isEmpty()) return text;
        List<InlineContentResolver> resolvers = new ArrayList<>(INLINE_RESOLVERS.values());
        resolvers.sort(java.util.Comparator.comparingInt(InlineContentResolver::priority).reversed()
                .thenComparing(InlineContentResolver::id));
        List<InlineSpan> spans = new ArrayList<>(text.inlineSpans().size());
        java.util.LinkedHashSet<String> applied =
                new java.util.LinkedHashSet<>(text.appliedMiddlewareIds());
        boolean changed = false;
        for (InlineSpan span : text.inlineSpans()) {
            neofontrender.text.InlineContent content = span.content();
            for (InlineContentResolver resolver : resolvers) {
                try {
                    if (!resolver.isEnabled() || !resolver.supports(content)) continue;
                    neofontrender.text.InlineContent resolved = resolver.resolve(content);
                    if (resolved != null && resolved != content) {
                        content = resolved;
                        applied.add(resolver.id());
                        changed = true;
                    }
                    break;
                } catch (RuntimeException | LinkageError ignored) {
                }
            }
            spans.add(new InlineSpan(span.start(), span.end(), content));
        }
        if (!changed) return text;
        return new StructuredText(text.sourceText(), text.plainText(), text.styles(), text.effects(),
                text.unresolvedSyntax(), text.sourceMap(), text.appliedSyntaxProviderIds(), spans,
                new ArrayList<>(applied));
    }

    public static final class Diagnostics {
        static final Diagnostics EMPTY = new Diagnostics(Collections.emptyList(),
                Collections.emptyList(), 0, 0, 0, 0, false);
        public final List<String> appliedProviderIds;
        public final List<String> appliedMiddlewareIds;
        public final int styleSpanCount;
        public final int effectSpanCount;
        public final int inlineSpanCount;
        public final int unresolvedCount;
        public final boolean animated;

        private Diagnostics(List<String> appliedProviderIds, List<String> appliedMiddlewareIds,
                            int styleSpanCount, int effectSpanCount, int inlineSpanCount,
                            int unresolvedCount, boolean animated) {
            this.appliedProviderIds = Collections.unmodifiableList(new ArrayList<>(appliedProviderIds));
            this.appliedMiddlewareIds = Collections.unmodifiableList(
                    new ArrayList<>(appliedMiddlewareIds));
            this.styleSpanCount = styleSpanCount;
            this.effectSpanCount = effectSpanCount;
            this.inlineSpanCount = inlineSpanCount;
            this.unresolvedCount = unresolvedCount;
            this.animated = animated;
        }
    }

    private static int[] toArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < result.length; index++) result[index] = values.get(index);
        return result;
    }

    private static TextSyntaxEngine currentEngine() {
        List<String> bindings = NeofontrenderConfig.brilliantTextBindings();
        int nextSignature = 31 * java.util.Arrays.hashCode(colorCodes)
                + (NeofontrenderConfig.brilliantTextEnabled() ? bindings.hashCode() : 0)
                + Boolean.hashCode(NeofontrenderConfig.vanillaFormattingCompatibility())
                + Boolean.hashCode(NeofontrenderConfig.laboratoryBrilliantAnyPosition())
                + Long.hashCode(externalRevision);
        TextSyntaxEngine current = engine;
        if (current != null && signature == nextSignature) return current;
        synchronized (StructuredTextRuntime.class) {
            if (engine != null && signature == nextSignature) return engine;
            engine = buildEngine(bindings, NeofontrenderConfig.brilliantTextEnabled());
            signature = nextSignature;
            return engine;
        }
    }

    private static StructuredTextPipeline currentPipeline() {
        return new StructuredTextPipeline(currentEngine(), STRUCTURED_MIDDLEWARE.values(),
                LINE_BREAK_PROVIDERS.values());
    }

    private static TextSyntaxEngine buildEngine(List<String> bindings, boolean brilliantEnabled) {
        TextSyntaxEngine.Builder builder = TextSyntaxEngine.builder();
        if (brilliantEnabled) {
            builder.register(createBrilliantProvider(parseBindings(bindings),
                    NeofontrenderConfig.laboratoryBrilliantAnyPosition()));
        }
        builder.register(new MinecraftLegacySyntaxProvider(colorCodes,
                NeofontrenderConfig.vanillaFormattingCompatibility()));
        for (TextSyntaxProvider provider : EXTERNAL_PROVIDERS.values()) builder.register(provider);
        return builder.build();
    }

    /** Keeps the main mod binary-compatible with an older standalone text-core jar. */
    private static TextSyntaxProvider createBrilliantProvider(
            Map<Character, Map<String, String>> bindings, boolean anyPosition) {
        if (!anyPosition) return new BrilliantSyntaxProvider(bindings);
        try {
            java.lang.reflect.Constructor<BrilliantSyntaxProvider> constructor =
                    BrilliantSyntaxProvider.class.getConstructor(Map.class, boolean.class);
            return constructor.newInstance(bindings, true);
        } catch (ReflectiveOperationException unavailable) {
            return new BrilliantSyntaxProvider(bindings);
        }
    }

    private static Map<Character, Map<String, String>> parseBindings(List<String> bindings) {
        Map<Character, Map<String, String>> result = new LinkedHashMap<>();
        if (bindings == null) return result;
        for (String entry : bindings) {
            try {
                String[] assignment = entry.split("=", 2);
                if (assignment.length != 2 || assignment[0].trim().isEmpty()) continue;
                char code = Character.toLowerCase(assignment[0].trim().charAt(0));
                String[] parts = assignment[1].split("\\|", -1);
                Map<String, String> values = new LinkedHashMap<>();
                values.put("textColor", value(parts, 0, "FFFFFFFF"));
                values.put("outlineColor", value(parts, 1, "00000000"));
                values.put("glowColor", value(parts, 2, "00000000"));
                values.put("particleTexture", value(parts, 3, ""));
                values.put("particleColor", value(parts, 4, "FFFFFFFF"));
                values.put("particleRarity", value(parts, 5, "0"));
                values.put("particleLifetime", value(parts, 6, "0"));
                values.put("particleDimensions", value(parts, 7, "0-0"));
                values.put("particleRotation", value(parts, 8, "0-0"));
                values.put("particleRotationPerFrame", value(parts, 9, "0-0"));
                result.put(code, values);
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private static String value(String[] parts, int index, String fallback) {
        return index < parts.length && !parts[index].trim().isEmpty() ? parts[index].trim() : fallback;
    }

    private static final class ParseKey {
        private final String source;
        private final long revision;

        ParseKey(String source, long revision) {
            this.source = source;
            this.revision = revision;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ParseKey)) return false;
            ParseKey value = (ParseKey) other;
            return revision == value.revision && source.equals(value.source);
        }

        @Override public int hashCode() {
            return 31 * source.hashCode() + Long.hashCode(revision);
        }
    }
}
