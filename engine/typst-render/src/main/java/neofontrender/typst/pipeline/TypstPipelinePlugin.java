package neofontrender.typst.pipeline;

import neofontrender.text.InlineContent;
import neofontrender.text.InlineLayout;
import neofontrender.text.InlineRaster;
import neofontrender.text.InlineRenderOptions;
import neofontrender.text.StructuredText;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextRewriter;
import neofontrender.text.pipeline.TextPipelinePlugin;
import neofontrender.typst.TypstEngine;
import neofontrender.typst.TypstRaster;
import neofontrender.typst.TypstEvent;
import neofontrender.typst.TypstPackages;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Complete Minecraft-free Typst contribution for the structured-text pipeline. */
public final class TypstPipelinePlugin implements TextPipelinePlugin, AutoCloseable {
    private static final String ID = "neofontrender_typst_renderer:text";
    private static final int MAXIMUM_TOKEN_LENGTH = 32 * 1024;
    private static final float DEFAULT_OVERSAMPLE = 2.0F;
    private final Config config;
    private final EngineSession session;
    private final RasterCache cache;
    private final StructuredTextMiddleware middleware = new Middleware();
    private final java.util.Queue<TypstEvent> failures = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** ServiceLoader constructor used by the standalone rendering laboratory. */
    public TypstPipelinePlugin() {
        this(new Config(() -> Path.of(System.getProperty("java.io.tmpdir"),
                "neofontrender-typst-lab"), () -> true, () -> 4.0D, () -> 4096, () -> {}));
    }

    public TypstPipelinePlugin(Config config) {
        this.config = Objects.requireNonNull(config, "config");
        this.session = new EngineSession();
        this.cache = new RasterCache(config.invalidation);
    }

    @Override public String id() { return ID; }

    @Override
    public Collection<? extends StructuredTextMiddleware> structuredMiddlewares() {
        return Collections.singletonList(middleware);
    }

    public java.util.List<TypstEvent> pollEvents() {
        ArrayList<TypstEvent> result = new ArrayList<>(session.pollEvents());
        for (TypstEvent event; (event = failures.poll()) != null;) result.add(event);
        return result;
    }

    public java.util.concurrent.CompletableFuture<java.util.List<TypstPackages.Entry>> packages() {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return TypstPackages.list(config.libraryDirectory.get()); }
            catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
        }, cache.workers);
    }

    public java.util.concurrent.CompletableFuture<Void> managePackage(String value, boolean delete) {
        String spec = TypstPackages.validate(value);
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Path directory = config.libraryDirectory.get().toAbsolutePath().normalize();
                if (delete) {
                    failures.addAll(session.pollEvents());
                    session.close();
                    TypstPackages.delete(directory, spec);
                } else {
                    session.open(directory).installPackage(spec);
                }
                failures.add(new TypstEvent(delete ? "deleted" : "installed", spec, 0, -1, ""));
                cache.handles.clear();
                config.invalidation.run();
            } catch (Exception error) {
                failures.add(new TypstEvent(delete ? "delete_failed" : "download_failed", spec, 0, -1,
                        error.toString()));
                throw new java.util.concurrent.CompletionException(error);
            }
        }, cache.workers);
    }

    public void retryFailed() {
        cache.handles.entrySet().removeIf(entry -> entry.getValue().failed);
        config.invalidation.run();
    }

    @Override
    public void close() {
        cache.close();
        session.close();
    }

    public static final class Config {
        private final Supplier<Path> libraryDirectory;
        private final BooleanSupplier enabled;
        private final DoubleSupplier oversample;
        private final IntSupplier maximumTokenLength;
        private final Runnable invalidation;

        public Config(Supplier<Path> libraryDirectory, BooleanSupplier enabled,
                      DoubleSupplier oversample, IntSupplier maximumTokenLength,
                      Runnable invalidation) {
            this.libraryDirectory = Objects.requireNonNull(libraryDirectory, "libraryDirectory");
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            this.oversample = Objects.requireNonNull(oversample, "oversample");
            this.maximumTokenLength = Objects.requireNonNull(
                    maximumTokenLength, "maximumTokenLength");
            this.invalidation = Objects.requireNonNull(invalidation, "invalidation");
        }
    }

    private final class Middleware implements StructuredTextMiddleware {
        @Override public String id() { return "neofontrender_typst_renderer:inline"; }
        @Override public int priority() { return 85; }
        @Override public boolean isEnabled() { return config.enabled.getAsBoolean(); }

        @Override
        public StructuredText process(StructuredText input) {
            String source = input.plainText();
            int maximum = Math.max(128, Math.min(MAXIMUM_TOKEN_LENGTH,
                    config.maximumTokenLength.getAsInt()));
            double configuredSupersample = config.oversample.getAsDouble();
            float defaultSupersample = Double.isFinite(configuredSupersample)
                    ? (float) Math.max(0.25D, Math.min(16.0D, configuredSupersample))
                    : DEFAULT_OVERSAMPLE;
            ArrayList<StructuredTextRewriter.Replacement> replacements = new ArrayList<>();
            for (int index = 0; index < source.length();) {
                TypstTokenParser.Match token = TypstTokenParser.match(source, index, maximum);
                if (token == null) {
                    index += Character.charCount(source.codePointAt(index));
                    continue;
                }
                InlineRenderOptions options = InlineRenderOptions.parse(source, token.end,
                        InlineLayout.oneLine(), defaultSupersample);
                float supersample = options.supersample();
                Path libraryDirectory = config.libraryDirectory.get().toAbsolutePath().normalize();
                String key = "typst\u0000" + supersample + "\u0000" + libraryDirectory
                        + "\u0000" + token.source;
                InlineContent content = cache.content(key, token.source, true, options.layout(),
                        supersample, () -> render(token.source, supersample, libraryDirectory));
                replacements.add(StructuredTextRewriter.inline(
                        token.start, options.end(), content));
                index = options.end();
            }
            return StructuredTextRewriter.rewrite(input, replacements, id());
        }
    }

    private InlineRaster render(String source, float scale, Path libraryDirectory) throws Exception {
        String document = "#set page(width: auto, height: auto, margin: 0pt, fill: none)\n"
                + "#set text(fill: white)\n" + source;
        try {
            TypstRaster raster = session.render(document, scale, libraryDirectory);
            return new InlineRaster(raster.width(), raster.height(), raster.argb());
        } catch (Exception | LinkageError error) {
            failures.add(new TypstEvent("compile_failed", "", 0, -1, error.toString()));
            throw error;
        }
    }

    private static final class EngineSession implements AutoCloseable {
        private volatile TypstEngine engine;
        private Path activeDirectory;
        private long retryAfterNanos;

        synchronized TypstRaster render(String source, float scale, Path requested) throws Exception {
            return open(requested).render(source, scale);
        }

        synchronized TypstEngine open(Path requested) throws Exception {
            if (engine == null || !requested.equals(activeDirectory)) {
                close();
                long now = System.nanoTime();
                if (now < retryAfterNanos) throw new IllegalStateException(
                        "Typst native initialization is cooling down");
                try {
                    engine = TypstEngine.open(requested);
                    activeDirectory = requested;
                    retryAfterNanos = 0L;
                } catch (Exception failure) {
                    retryAfterNanos = now + 5_000_000_000L;
                    throw failure;
                }
            }
            return engine;
        }

        java.util.List<TypstEvent> pollEvents() {
            TypstEngine current = engine;
            return current == null ? java.util.List.of() : current.pollEvents();
        }

        @Override
        public synchronized void close() {
            if (engine != null) engine.close();
            engine = null;
            activeDirectory = null;
        }
    }

    private static final class RasterCache implements AutoCloseable {
        private static final int MAX_ENTRIES = 256;
        private static final long MAX_PIXELS = 32L * 1024L * 1024L;
        private final Runnable invalidation;
        private final ExecutorService workers = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "NFR Typst Rasterizer");
            thread.setDaemon(true);
            return thread;
        });
        private final Map<String, Handle> handles = new ConcurrentHashMap<>();

        RasterCache(Runnable invalidation) { this.invalidation = invalidation; }

        InlineContent content(String key, String description, boolean tint, InlineLayout layout,
                              float supersample, RasterJob job) {
            Handle handle = handles.get(key);
            if (handle == null) {
                trim(MAX_ENTRIES - 1, MAX_PIXELS);
                Handle created = new Handle();
                Handle raced = handles.putIfAbsent(key, created);
                handle = raced == null ? created : raced;
                if (raced == null) workers.execute(() -> rasterize(created, job));
            }
            handle.lastAccess = System.nanoTime();
            InlineRaster raster = handle.raster;
            Map<String, String> attributes = new java.util.LinkedHashMap<>();
            attributes.put("status", raster != null ? "ready" : handle.failed ? "failed" : "loading");
            if (handle.error != null) attributes.put("error", handle.error);
            attributes.put("supersample", Float.toString(supersample));
            return new InlineContent("typst", key, description, tint, raster, attributes, layout);
        }

        private void rasterize(Handle handle, RasterJob job) {
            try {
                InlineRaster raster = job.render();
                if (raster == null) throw new IllegalArgumentException("Typst returned no raster");
                handle.raster = raster;
                trim(MAX_ENTRIES, MAX_PIXELS);
            } catch (Throwable failure) {
                handle.error = failure.toString();
                handle.failed = true;
            } finally {
                try {
                    invalidation.run();
                } catch (RuntimeException ignored) {
                }
            }
        }

        private void trim(int maximumEntries, long maximumPixels) {
            while (handles.size() > maximumEntries || pixels() > maximumPixels) {
                Map.Entry<String, Handle> oldest = null;
                for (Map.Entry<String, Handle> entry : handles.entrySet()) {
                    if (entry.getValue().raster == null && !entry.getValue().failed) continue;
                    if (oldest == null
                            || entry.getValue().lastAccess < oldest.getValue().lastAccess) {
                        oldest = entry;
                    }
                }
                if (oldest == null || !handles.remove(oldest.getKey(), oldest.getValue())) return;
            }
        }

        private long pixels() {
            long total = 0L;
            for (Handle handle : handles.values()) {
                InlineRaster raster = handle.raster;
                if (raster != null) total += (long) raster.width() * raster.height();
            }
            return total;
        }

        @Override public void close() {
            workers.shutdownNow();
            handles.clear();
        }
    }

    @FunctionalInterface
    private interface RasterJob { InlineRaster render() throws Exception; }

    private static final class Handle {
        volatile InlineRaster raster;
        volatile boolean failed;
        volatile String error;
        volatile long lastAccess = System.nanoTime();
    }
}
