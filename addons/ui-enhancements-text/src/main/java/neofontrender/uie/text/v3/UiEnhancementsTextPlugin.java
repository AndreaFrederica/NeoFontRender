package neofontrender.uie.text.v3;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neofontrender.text.InlineContent;
import neofontrender.text.InlineLayout;
import neofontrender.text.InlineRaster;
import neofontrender.text.InlineRenderOptions;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextRewriter;
import neofontrender.text.pipeline.TextPipelinePlugin;
import neofontrender.text.pipeline.LineBreakOpportunityProvider;
import neofontrender.text.layout.CjkLineBreakProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minecraft-free implementation of UI Enhancements text contributions. */
public final class UiEnhancementsTextPlugin implements TextPipelinePlugin, AutoCloseable {
    public static final String ID = "neofontrender_ui_enhancements:text";
    private final Config config;
    private final RasterCache rasterCache;
    private final List<StructuredTextMiddleware> middleware;
    private final List<LineBreakOpportunityProvider> lineBreakProviders;

    public UiEnhancementsTextPlugin() {
        this(Config.standaloneDefaults());
    }

    public UiEnhancementsTextPlugin(Config config) {
        this.config = Objects.requireNonNull(config, "config");
        this.rasterCache = new RasterCache(config.rasterCacheEntries,
                config.rasterCacheMegapixels, config.invalidation);
        this.middleware = List.of(
                new ExternalImageMiddleware(config),
                new SvgMiddleware(config, rasterCache),
                new LatexMiddleware(config, rasterCache),
                new LocalImageMiddleware(config, rasterCache),
                new MarkdownMiddleware(config),
                new GoslingMiddleware(config));
        this.lineBreakProviders = List.of(new UiECjkLineBreakProvider());
    }

    @Override public String id() { return ID; }
    @Override public Collection<? extends StructuredTextMiddleware> structuredMiddlewares() {
        return middleware;
    }
    @Override public Collection<? extends LineBreakOpportunityProvider> lineBreakProviders() {
        return lineBreakProviders;
    }

    public Config config() { return config; }

    @Override public void close() { rasterCache.close(); }

    private static final class UiECjkLineBreakProvider implements LineBreakOpportunityProvider {
        private final CjkLineBreakProvider delegate = new CjkLineBreakProvider();
        @Override public String id() { return ID + "/cjk_layout"; }
        @Override public int priority() { return 100; }
        @Override public List<Integer> opportunities(StructuredText text) {
            return delegate.opportunities(text);
        }
    }

    public static final class Config {
        public final BooleanSupplier markdownEnabled;
        public final BooleanSupplier latexEnabled;
        public final BooleanSupplier svgEnabled;
        public final BooleanSupplier localImagesEnabled;
        public final BooleanSupplier externalImagesEnabled;
        public final BooleanSupplier goslingEnabled;
        public final BooleanSupplier latexMatchLineHeight;
        public final Supplier<List<String>> latexFontSelectors;
        public final IntSupplier rasterCacheEntries;
        public final DoubleSupplier rasterCacheMegapixels;
        public final Runnable invalidation;
        public final Path galleryDirectory;
        public final float latexOversample;
        public final boolean fullSvg;

        public Config(BooleanSupplier markdownEnabled, BooleanSupplier latexEnabled,
                      BooleanSupplier svgEnabled, BooleanSupplier localImagesEnabled,
                      BooleanSupplier externalImagesEnabled, BooleanSupplier goslingEnabled,
                      Path galleryDirectory, float latexOversample, boolean fullSvg) {
            this(markdownEnabled, latexEnabled, svgEnabled, localImagesEnabled,
                    externalImagesEnabled, goslingEnabled, galleryDirectory, latexOversample,
                    fullSvg, () -> true, Config::defaultLatexFontSelectors,
                    () -> 192, () -> 24.0D, () -> {});
        }

        public Config(BooleanSupplier markdownEnabled, BooleanSupplier latexEnabled,
                      BooleanSupplier svgEnabled, BooleanSupplier localImagesEnabled,
                      BooleanSupplier externalImagesEnabled, BooleanSupplier goslingEnabled,
                      Path galleryDirectory, float latexOversample, boolean fullSvg,
                      BooleanSupplier latexMatchLineHeight) {
            this(markdownEnabled, latexEnabled, svgEnabled, localImagesEnabled,
                    externalImagesEnabled, goslingEnabled, galleryDirectory, latexOversample,
                    fullSvg, latexMatchLineHeight, Config::defaultLatexFontSelectors,
                    () -> 192, () -> 24.0D, () -> {});
        }

        public Config(BooleanSupplier markdownEnabled, BooleanSupplier latexEnabled,
                      BooleanSupplier svgEnabled, BooleanSupplier localImagesEnabled,
                      BooleanSupplier externalImagesEnabled, BooleanSupplier goslingEnabled,
                      Path galleryDirectory, float latexOversample, boolean fullSvg,
                      BooleanSupplier latexMatchLineHeight,
                      Supplier<List<String>> latexFontSelectors) {
            this(markdownEnabled, latexEnabled, svgEnabled, localImagesEnabled,
                    externalImagesEnabled, goslingEnabled, galleryDirectory, latexOversample,
                    fullSvg, latexMatchLineHeight, latexFontSelectors,
                    () -> 192, () -> 24.0D, () -> {});
        }

        public Config(BooleanSupplier markdownEnabled, BooleanSupplier latexEnabled,
                      BooleanSupplier svgEnabled, BooleanSupplier localImagesEnabled,
                      BooleanSupplier externalImagesEnabled, BooleanSupplier goslingEnabled,
                      Path galleryDirectory, float latexOversample, boolean fullSvg,
                      BooleanSupplier latexMatchLineHeight,
                      Supplier<List<String>> latexFontSelectors,
                      IntSupplier rasterCacheEntries,
                      DoubleSupplier rasterCacheMegapixels,
                      Runnable invalidation) {
            this.markdownEnabled = value(markdownEnabled);
            this.latexEnabled = value(latexEnabled);
            this.svgEnabled = value(svgEnabled);
            this.localImagesEnabled = value(localImagesEnabled);
            this.externalImagesEnabled = value(externalImagesEnabled);
            this.goslingEnabled = value(goslingEnabled);
            this.latexMatchLineHeight = value(latexMatchLineHeight);
            this.latexFontSelectors = latexFontSelectors == null
                    ? Config::defaultLatexFontSelectors : latexFontSelectors;
            this.rasterCacheEntries = rasterCacheEntries == null ? () -> 192 : rasterCacheEntries;
            this.rasterCacheMegapixels = rasterCacheMegapixels == null
                    ? () -> 24.0D : rasterCacheMegapixels;
            this.invalidation = invalidation == null ? () -> {} : invalidation;
            this.galleryDirectory = galleryDirectory;
            this.latexOversample = Math.max(1.0F, Math.min(8.0F, latexOversample));
            this.fullSvg = fullSvg;
        }

        public static Config standaloneDefaults() {
            String configured = System.getProperty("neofontrender.uie.gallery", "").trim();
            Path gallery = configured.isEmpty() ? null : Path.of(configured);
            return new Config(() -> true, () -> true, () -> true, () -> gallery != null,
                    () -> true, () -> true, gallery, 2.0F, false, () -> true,
                    Config::defaultLatexFontSelectors, () -> 192, () -> 24.0D, () -> {});
        }

        private static List<String> defaultLatexFontSelectors() {
            return List.of("neofontrender_ui_enhancements:fonts/fira_math-regular.otf");
        }

        private static BooleanSupplier value(BooleanSupplier supplier) {
            return supplier == null ? () -> false : supplier;
        }
    }

    private abstract static class MatchingMiddleware implements StructuredTextMiddleware {
        private final BooleanSupplier enabled;
        MatchingMiddleware(BooleanSupplier enabled) { this.enabled = enabled; }
        @Override public boolean isEnabled() { return enabled.getAsBoolean(); }

        @Override public final StructuredText process(StructuredText input) {
            List<StructuredTextRewriter.Replacement> replacements = new ArrayList<>();
            String text = input.plainText();
            for (int index = 0; index < text.length();) {
                Match match = match(text, index);
                if (match == null) {
                    index += Character.charCount(text.codePointAt(index));
                } else {
                    replacements.add(match.replacement);
                    index = match.end;
                }
            }
            return StructuredTextRewriter.rewrite(input, replacements, id());
        }

        abstract Match match(String text, int start);
    }

    private static final class MarkdownMiddleware extends MatchingMiddleware {
        MarkdownMiddleware(Config config) { super(config.markdownEnabled); }
        @Override public String id() { return ID + "/markdown"; }
        @Override public int priority() { return 60; }

        @Override Match match(String text, int start) {
            if (escaped(text, start)) return null;
            char marker = text.charAt(start);
            if (marker == '[') return link(text, start);
            if (marker == '`') return delimited(text, start, "`", style -> style);
            if (marker == '~' && startsWith(text, start, "~~")) {
                return delimited(text, start, "~~", style -> style.withStrikethrough(true));
            }
            if (marker == '*' || marker == '_') {
                String strong = marker == '*' ? "**" : "__";
                if (startsWith(text, start, strong)) {
                    return delimited(text, start, strong, style -> style.withBold(true));
                }
                return delimited(text, start, Character.toString(marker),
                        style -> style.withItalic(true));
            }
            return null;
        }

        private static Match delimited(String text, int start, String delimiter,
                                       java.util.function.UnaryOperator<TextStyle> style) {
            int contentStart = start + delimiter.length();
            int limit = Math.min(text.length(), start + 512);
            if (contentStart >= limit || Character.isWhitespace(text.charAt(contentStart))) return null;
            for (int cursor = contentStart; cursor + delimiter.length() <= limit; cursor++) {
                if (text.charAt(cursor) == '\n' || text.charAt(cursor) == '\r') return null;
                if (!escaped(text, cursor) && startsWith(text, cursor, delimiter)) {
                    if (cursor == contentStart) return null;
                    int end = cursor + delimiter.length();
                    String visible = unescape(text.substring(contentStart, cursor));
                    return new Match(end, StructuredTextRewriter.text(start, end, visible, style));
                }
            }
            return null;
        }

        private static Match link(String text, int start) {
            int closeLabel = unescapedIndex(text, ']', start + 1, Math.min(text.length(), start + 512));
            if (closeLabel <= start + 1 || closeLabel + 1 >= text.length()
                    || text.charAt(closeLabel + 1) != '(') return null;
            int closeUrl = unescapedIndex(text, ')', closeLabel + 2,
                    Math.min(text.length(), start + 512));
            if (closeUrl < 0) return null;
            String label = unescape(text.substring(start + 1, closeLabel));
            return new Match(closeUrl + 1, StructuredTextRewriter.text(start, closeUrl + 1,
                    label, style -> style.withUnderline(true)));
        }
    }

    private static final class LatexMiddleware extends MatchingMiddleware {
        private final Config config;
        private final RasterCache cache;
        LatexMiddleware(Config config, RasterCache cache) {
            super(config.latexEnabled);
            this.config = config;
            this.cache = cache;
        }
        @Override public String id() { return ID + "/latex"; }
        @Override public int priority() { return 80; }

        @Override Match match(String text, int start) {
            if (text.charAt(start) != '$' || escaped(text, start)) return null;
            int delimiter = startsWith(text, start, "$$") ? 2 : 1;
            int contentStart = start + delimiter;
            int close = -1;
            for (int index = contentStart; index < Math.min(text.length(), start + 2048); index++) {
                if (text.charAt(index) == '$' && !escaped(text, index)
                        && (delimiter == 1 || startsWith(text, index, "$$"))) { close = index; break; }
            }
            if (close <= contentStart) return null;
            int end = close + delimiter;
            String formula = text.substring(contentStart, close);
            try {
                float rows = config.latexMatchLineHeight.getAsBoolean() ? 1.0F
                        : (delimiter == 2 ? 21.0F : 14.0F) / 18.0F;
                InlineRenderOptions options = InlineRenderOptions.parse(text, end,
                        new InlineLayout(rows, Float.NaN, Float.NaN,
                                InlineLayout.Alignment.BASELINE), config.latexOversample);
                end = options.end();
                List<String> selectors = safeList(config.latexFontSelectors.get());
                float supersample = options.supersample();
                RasterKey key = new RasterKey("latex", formula,
                        Float.floatToIntBits(supersample) + "\u0000" + selectors, 0L, 0L);
                Map<String, String> attributes = Map.of(
                        "display", Boolean.toString(delimiter == 2),
                        "supersample", Float.toString(supersample));
                InlineContent content = cache.content(key, "latex", formula, formula, true,
                        options.layout(), attributes,
                        () -> RasterSupport.latex(formula, supersample, selectors));
                return new Match(end, StructuredTextRewriter.inline(start, end, content));
            } catch (RuntimeException error) {
                return null;
            }
        }
    }

    private static final class SvgMiddleware extends MatchingMiddleware {
        private final Config config;
        private final RasterCache cache;
        SvgMiddleware(Config config, RasterCache cache) {
            super(config.svgEnabled);
            this.config = config;
            this.cache = cache;
        }
        @Override public String id() { return ID + "/svg"; }
        @Override public int priority() { return 90; }

        @Override Match match(String text, int start) {
            int prefix = startsWith(text, start, "<svg:") ? 5
                    : startsWith(text, start, "svg:") ? 4 : 0;
            if (prefix == 0) return null;
            int endMarker = text.indexOf('>', start + prefix);
            if (endMarker < 0 || endMarker - start > 2048) return null;
            String reference = text.substring(start + prefix, endMarker).trim();
            int end = endMarker + 1;
            int displayHeight = 24;
            if (end < text.length() && text.charAt(end) == '[') {
                int optionEnd = text.indexOf(']', end + 1);
                if (optionEnd > end) {
                    Matcher matcher = Pattern.compile("(?i)height\\s*=\\s*([0-9.]+)")
                            .matcher(text.substring(end + 1, optionEnd).trim());
                    if (matcher.matches()) {
                        try { displayHeight = Math.max(4, Math.min(256,
                                Math.round(Float.parseFloat(matcher.group(1))))); end = optionEnd + 1; }
                        catch (NumberFormatException ignored) {}
                    }
                }
            }
            try {
                FileVersion version = referenceVersion(reference);
                RasterKey key = new RasterKey("svg", version.identity,
                        Boolean.toString(config.fullSvg), version.size, version.modified);
                InlineContent content = cache.content(key, "svg", reference,
                        "SVG " + reference, false, InlineLayout.legacy(displayHeight, false),
                        Collections.emptyMap(), () -> RasterSupport.svg(
                                readReference(reference, true), referenceUri(reference), config.fullSvg));
                return new Match(end, StructuredTextRewriter.inline(start, end, content));
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static final class LocalImageMiddleware extends MatchingMiddleware {
        private final Path gallery;
        private final RasterCache cache;
        LocalImageMiddleware(Config config, RasterCache cache) {
            super(config.localImagesEnabled);
            gallery = config.galleryDirectory;
            this.cache = cache;
        }
        @Override public String id() { return ID + "/local_image"; }
        @Override public int priority() { return 75; }

        @Override Match match(String text, int start) {
            if (gallery == null || text.charAt(start) != ':' || escaped(text, start)) return null;
            int end = unescapedIndex(text, ':', start + 1,
                    Math.min(text.length(), start + 129));
            if (end < 0 || end - start > 128) return null;
            String alias = text.substring(start + 1, end);
            if (!safeLocalAlias(alias)) return null;
            for (String extension : new String[]{".png", ".jpg", ".jpeg", ".gif", ".webp"}) {
                try {
                    Path normalizedGallery = gallery.toAbsolutePath().normalize();
                    Path candidate = normalizedGallery.resolve(alias + extension).normalize();
                    if (!candidate.startsWith(normalizedGallery)
                            || !Files.isRegularFile(candidate)) continue;
                    long size = Files.size(candidate);
                    long modified = Files.getLastModifiedTime(candidate).toMillis();
                    RasterKey key = new RasterKey("local_image", candidate.toString(), "",
                            size, modified);
                    InlineContent content = cache.content(key, "local_image",
                            candidate.toString(), ':' + alias + ": local gallery", false,
                            InlineLayout.legacy(18, false), Collections.emptyMap(),
                            () -> RasterSupport.image(Files.readAllBytes(candidate)));
                    return new Match(end + 1,
                            StructuredTextRewriter.inline(start, end + 1, content));
                } catch (RuntimeException | java.io.IOException ignored) { return null; }
            }
            return null;
        }

        private static boolean safeLocalAlias(String alias) {
            if (alias == null || alias.isEmpty()) return false;
            for (int offset = 0; offset < alias.length();) {
                int codePoint = alias.codePointAt(offset);
                if (!Character.isLetterOrDigit(codePoint) && codePoint != '_'
                        && codePoint != '+' && codePoint != '-') return false;
                offset += Character.charCount(codePoint);
            }
            return true;
        }
    }

    private static final class ExternalImageMiddleware extends MatchingMiddleware {
        ExternalImageMiddleware(Config config) { super(config.externalImagesEnabled); }
        @Override public String id() { return ID + "/external_image"; }
        @Override public int priority() { return 100; }

        @Override Match match(String text, int start) {
            if (!startsWith(text, start, "<img:")) return null;
            int close = text.indexOf('>', start + 5);
            if (close < 0 || close - start > 2048) return null;
            try {
                URI uri = URI.create(text.substring(start + 5, close));
                if (!("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null)) return null;
                InlineContent content = new InlineContent("external_image", uri.toASCIIString(),
                        "External image: " + uri.getHost(), 18, false, true, null,
                        Map.of("uri", uri.toASCIIString(), "status", "deferred"));
                return new Match(close + 1,
                        StructuredTextRewriter.inline(start, close + 1, content));
            } catch (IllegalArgumentException ignored) { return null; }
        }
    }

    private static final class GoslingMiddleware extends MatchingMiddleware {
        private static final Pattern DISCORD = Pattern.compile("<a?:([\\w]+):([a-zA-Z0-9+/=]+)>");
        private final Map<String, URI> aliases = loadAliases();
        GoslingMiddleware(Config config) { super(config.goslingEnabled); }
        @Override public String id() { return ID + "/gosling_emoji"; }
        @Override public int priority() { return 50; }

        @Override Match match(String text, int start) {
            if (text.charAt(start) == ':') {
                int end = text.indexOf(':', start + 1);
                if (end > start && end - start <= 96) {
                    String name = text.substring(start + 1, end).toLowerCase(Locale.ROOT);
                    URI uri = aliases.get(name);
                    if (uri != null) return remote(start, end + 1, name, uri);
                }
            }
            if (text.charAt(start) == '<') {
                int end = text.indexOf('>', start + 1);
                if (end > start && end - start <= 96) {
                    Matcher matcher = DISCORD.matcher(text.substring(start, end + 1));
                    if (matcher.matches()) {
                        try {
                            String encoded = matcher.group(2);
                            long id = encoded.matches("[0-9]{13,20}") ? Long.parseLong(encoded)
                                    : new java.math.BigInteger(Base64.getDecoder().decode(encoded)).longValueExact();
                            return remote(start, end + 1, matcher.group(1),
                                    URI.create("https://cdn.discordapp.com/emojis/" + id));
                        } catch (RuntimeException ignored) {}
                    }
                }
            }
            return null;
        }

        private static Match remote(int start, int end, String name, URI uri) {
            InlineContent content = new InlineContent("gosling_emoji", name,
                    ':' + name + ':', 18, false, true, null,
                    Map.of("uri", uri.toASCIIString(), "status", "deferred"));
            return new Match(end, StructuredTextRewriter.inline(start, end, content));
        }

        private static Map<String, URI> loadAliases() {
            Map<String, URI> result = new HashMap<>();
            String resource = "/assets/neofontrender_ui_enhancements/emoji/gosling-emojis.min.json";
            try (InputStream stream = UiEnhancementsTextPlugin.class.getResourceAsStream(resource)) {
                if (stream == null) return result;
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream,
                        StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement groupValue : root.getAsJsonArray("groups")) {
                    JsonObject group = groupValue.getAsJsonObject();
                    String base = group.get("location").getAsString();
                    for (JsonElement emojiValue : group.getAsJsonArray("emojis")) {
                        JsonObject emoji = emojiValue.getAsJsonObject();
                        URI uri = URI.create(base + emoji.get("location").getAsString());
                        String name = emoji.get("name").getAsString().toLowerCase(Locale.ROOT);
                        result.putIfAbsent(name, uri);
                        if (emoji.has("strings")) for (JsonElement alias : emoji.getAsJsonArray("strings")) {
                            result.putIfAbsent(alias.getAsString().toLowerCase(Locale.ROOT), uri);
                        }
                    }
                }
            } catch (Exception ignored) {}
            return Collections.unmodifiableMap(result);
        }
    }

    private static byte[] readReference(String reference, boolean allowClasspath) throws Exception {
        String normalizedReference = reference == null ? "" : reference.trim().replace('\\', '/');
        if (normalizedReference.startsWith("data:image/svg+xml;base64,")) {
            return Base64.getDecoder().decode(normalizedReference.substring(normalizedReference.indexOf(',') + 1));
        }
        if (normalizedReference.startsWith("data:image/svg+xml,")) {
            return java.net.URLDecoder.decode(normalizedReference.substring(normalizedReference.indexOf(',') + 1),
                    StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        }
        Path path;
        try { path = Path.of(normalizedReference); } catch (RuntimeException invalid) { path = null; }
        if (path != null && Files.isRegularFile(path)) return Files.readAllBytes(path);
        if (allowClasspath) {
            String resourcePath = classpathSvgResource(normalizedReference);
            String name = resourcePath != null ? resourcePath
                    : normalizedReference.startsWith("/") ? normalizedReference : '/' + normalizedReference;
            try (InputStream stream = UiEnhancementsTextPlugin.class.getResourceAsStream(name)) {
                if (stream != null) return stream.readAllBytes();
            }
        }
        throw new IllegalArgumentException("Unsupported SVG reference");
    }

    private static URI referenceUri(String reference) {
        String resourcePath = classpathSvgResource(reference);
        if (resourcePath != null) {
            try {
                java.net.URL resource = UiEnhancementsTextPlugin.class.getResource(resourcePath);
                return resource == null ? null : resource.toURI();
            } catch (Exception ignored) { return null; }
        }
        try { return Path.of(reference).toAbsolutePath().toUri(); }
        catch (RuntimeException ignored) { return null; }
    }

    static String classpathSvgResource(String reference) {
        if (reference == null) return null;
        String normalized = reference.trim().replace('\\', '/');
        if (!normalized.startsWith("resource:")) return null;
        String value = normalized.substring("resource:".length());
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':')) return null;
        String namespace = value.substring(0, separator);
        String path = value.substring(separator + 1);
        if (!namespace.matches("[a-z0-9_.-]+")
                || !path.matches("[a-z0-9/._-]+")
                || path.startsWith("/") || path.contains("../") || path.contains("/..")
                || !path.endsWith(".svg")) return null;
        return "/assets/" + namespace + '/' + path;
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : List.copyOf(values);
    }

    private static FileVersion referenceVersion(String reference) {
        String normalized = reference == null ? "" : reference.trim().replace('\\', '/');
        try {
            Path path = Path.of(normalized).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                return new FileVersion(path.toString(), Files.size(path),
                        Files.getLastModifiedTime(path).toMillis());
            }
        } catch (Exception ignored) {
        }
        return new FileVersion(normalized, 0L, 0L);
    }

    private static final class FileVersion {
        final String identity;
        final long size;
        final long modified;

        FileVersion(String identity, long size, long modified) {
            this.identity = identity;
            this.size = size;
            this.modified = modified;
        }
    }

    private static final class RasterKey {
        private final String kind;
        private final String source;
        private final String options;
        private final long size;
        private final long modified;

        RasterKey(String kind, String source, String options, long size, long modified) {
            this.kind = kind;
            this.source = source;
            this.options = options;
            this.size = size;
            this.modified = modified;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RasterKey)) return false;
            RasterKey value = (RasterKey) other;
            return size == value.size && modified == value.modified
                    && kind.equals(value.kind) && source.equals(value.source)
                    && options.equals(value.options);
        }

        @Override public int hashCode() {
            return Objects.hash(kind, source, options, size, modified);
        }
    }

    private static final class RasterCache implements AutoCloseable {
        private final IntSupplier maximumEntries;
        private final DoubleSupplier maximumMegapixels;
        private final Runnable invalidation;
        private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "NFR UIE Rasterizer");
            thread.setDaemon(true);
            return thread;
        });
        private final Map<RasterKey, RasterHandle> handles = new ConcurrentHashMap<>();

        RasterCache(IntSupplier maximumEntries, DoubleSupplier maximumMegapixels,
                    Runnable invalidation) {
            this.maximumEntries = maximumEntries;
            this.maximumMegapixels = maximumMegapixels;
            this.invalidation = invalidation;
        }

        InlineContent content(RasterKey cacheKey, String kind, String contentKey,
                              String description, boolean tint, InlineLayout layout,
                              Map<String, String> baseAttributes, RasterJob job) {
            RasterHandle handle = handles.get(cacheKey);
            if (handle == null) {
                trim(entryLimit() - 1, pixelLimit());
                RasterHandle created = new RasterHandle();
                RasterHandle raced = handles.putIfAbsent(cacheKey, created);
                handle = raced == null ? created : raced;
                if (raced == null) {
                    try {
                        workers.execute(() -> rasterize(created, job));
                    } catch (RuntimeException rejected) {
                        created.failed = true;
                    }
                }
            }
            handle.lastAccess = System.nanoTime();
            InlineRaster raster = handle.raster;
            Map<String, String> attributes = new LinkedHashMap<>(baseAttributes);
            attributes.put("status", raster != null ? "ready" : handle.failed ? "failed" : "loading");
            return new InlineContent(kind, contentKey, description, tint, raster, attributes, layout);
        }

        private void rasterize(RasterHandle handle, RasterJob job) {
            try {
                InlineRaster raster = job.render();
                if (raster == null) throw new IllegalArgumentException("Rasterizer returned no pixels");
                handle.raster = raster;
                trim(entryLimit(), pixelLimit());
            } catch (Throwable failure) {
                handle.failed = true;
            } finally {
                try {
                    invalidation.run();
                } catch (RuntimeException ignored) {
                }
            }
        }

        private void trim(int maximum, long maximumPixels) {
            int entryMaximum = Math.max(1, maximum);
            while (handles.size() > entryMaximum || pixels() > maximumPixels) {
                Map.Entry<RasterKey, RasterHandle> oldest = null;
                for (Map.Entry<RasterKey, RasterHandle> entry : handles.entrySet()) {
                    RasterHandle value = entry.getValue();
                    if (value.raster == null && !value.failed) continue;
                    if (oldest == null || value.lastAccess < oldest.getValue().lastAccess) {
                        oldest = entry;
                    }
                }
                if (oldest == null || !handles.remove(oldest.getKey(), oldest.getValue())) return;
            }
        }

        private long pixels() {
            long result = 0L;
            for (RasterHandle handle : handles.values()) {
                InlineRaster raster = handle.raster;
                if (raster != null) result += (long) raster.width() * raster.height();
            }
            return result;
        }

        private int entryLimit() {
            try {
                return Math.max(8, Math.min(4096, maximumEntries.getAsInt()));
            } catch (RuntimeException ignored) {
                return 192;
            }
        }

        private long pixelLimit() {
            try {
                double value = maximumMegapixels.getAsDouble();
                if (!Double.isFinite(value)) return 24L * 1024L * 1024L;
                return (long) (Math.max(1.0D, Math.min(512.0D, value)) * 1024D * 1024D);
            } catch (RuntimeException ignored) {
                return 24L * 1024L * 1024L;
            }
        }

        @Override public void close() {
            workers.shutdownNow();
            handles.clear();
        }
    }

    @FunctionalInterface
    private interface RasterJob { InlineRaster render() throws Exception; }

    private static final class RasterHandle {
        volatile InlineRaster raster;
        volatile boolean failed;
        volatile long lastAccess = System.nanoTime();
    }

    private static boolean startsWith(String text, int start, String value) {
        return start >= 0 && start + value.length() <= text.length()
                && text.regionMatches(start, value, 0, value.length());
    }

    private static boolean escaped(String text, int index) {
        int slashes = 0;
        for (int scan = index - 1; scan >= 0 && text.charAt(scan) == '\\'; scan--) slashes++;
        return (slashes & 1) != 0;
    }

    private static int unescapedIndex(String text, char value, int start, int limit) {
        for (int index = start; index < limit; index++) {
            if ((text.charAt(index) == '\n' || text.charAt(index) == '\r')) return -1;
            if (text.charAt(index) == value && !escaped(text, index)) return index;
        }
        return -1;
    }

    private static String unescape(String value) {
        return value.replaceAll("\\\\([*_~`\\[\\]()\\\\])", "$1");
    }

    private static final class Match {
        final int end;
        final StructuredTextRewriter.Replacement replacement;
        Match(int end, StructuredTextRewriter.Replacement replacement) {
            this.end = end;
            this.replacement = replacement;
        }
    }
}
