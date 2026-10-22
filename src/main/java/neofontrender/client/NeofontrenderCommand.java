package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import neofontrender.client.gui.NeofontrenderEmojiTestScreen;
import neofontrender.build.BuildFeatures;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.backend.BackendTextSegmenter;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.cosmic.CosmicTextRenderer;
import neofontrender.core.font.pipeline.StructuredTextRuntime;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Client diagnostics and font-management command. */
public final class NeofontrenderCommand extends CommandBase {
    private static final List<String> SUBCOMMANDS = Arrays.asList("fonts", "info", "reload", "test", "benchmark", "gui", "config");
    private static final AtomicBoolean ASYNC_BENCHMARK_RUNNING = new AtomicBoolean();
    private static final ExecutorService ASYNC_BENCHMARK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NFR Async Benchmark");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    @Override
    public String getName() { return "neofontrender"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/neofontrender fonts|info|reload|test|benchmark [sdf|async|atlas]|gui"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            message(sender, TextFormatting.GOLD, NeofontrenderBranding.displayName() + ": " + getUsage(sender));
            return;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "fonts": fonts(sender); break;
            case "info": info(sender); break;
            case "reload": reload(sender); break;
            case "test": test(sender); break;
            case "benchmark": benchmark(sender, args); break;
            case "gui": gui(sender); break;
            case "config": config(sender, args); break;
            default: message(sender, TextFormatting.RED, "Unknown subcommand: " + args[0]);
        }
    }
    private static void config(ICommandSender sender, String[] args) {
        if (args.length < 2 || !"promote".equalsIgnoreCase(args[1])) {
            message(sender, TextFormatting.YELLOW, "/neofontrender config promote"); return;
        }
        try {
            Class<?> c = Class.forName("neofontrender.addons.ui.UiEnhancementsConfig");
            c.getMethod("promoteUserToPack").invoke(null);
            message(sender, TextFormatting.GREEN, "User settings copied to the integration-pack configuration layer.");
        } catch (Exception e) { message(sender, TextFormatting.RED, "Cannot promote config: " + e.getCause()); }
    }

    private static void fonts(ICommandSender sender) {
        message(sender, TextFormatting.GOLD, "Configured font: " + NeofontrenderConfig.fontFamily());
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null || !backend.isReady()) {
            message(sender, TextFormatting.YELLOW, "Active backend does not expose font families.");
            return;
        }
        String[] families = backend.getFontFamilies();
        message(sender, TextFormatting.AQUA, "Backend families: " + families.length);
        for (String family : families) message(sender, TextFormatting.WHITE, "  " + family);
    }

    private static void info(ICommandSender sender) {
        FontManager manager = FontManager.INSTANCE;
        String engine = manager.isCompatActive() ? "compat"
                : manager.isCosmicActive() ? "cosmic" : manager.isSfrActive() ? "sfr" : "vanilla";
        message(sender, TextFormatting.GOLD, NeofontrenderBranding.displayName() + " engine: " + engine);
        message(sender, TextFormatting.WHITE, "  configured: " + NeofontrenderConfig.renderingEngine());
        message(sender, TextFormatting.WHITE, "  backend: " + manager.getBackendVersion());
        message(sender, TextFormatting.WHITE, "  advanced string mode: " + NeofontrenderConfig.advancedStringMode());
        message(sender, TextFormatting.WHITE, "  oversample: " + NeofontrenderConfig.fontOversample());

        if (manager.isCompatActive()) {
            neofontrender.core.font.compat.CompatFontEngine compat = manager.getCompatEngine();
            if (compat != null) {
                message(sender, TextFormatting.WHITE, "  compat pages: " + compat.pageCount()
                        + " raster: " + NeofontrenderConfig.compatRasterBackend()
                        + " scale: " + NeofontrenderConfig.compatRasterScale());
            }
        }

        CosmicTextRenderer cosmic = manager.getCosmicTextRenderer();
        if (cosmic != null) {
            CosmicTextRenderer.DebugState state = cosmic.debugState();
            message(sender, TextFormatting.WHITE, "  text cache: " + state.renderCacheSize + "/" + state.renderCacheMax
                    + " h/m/e=" + state.renderHits + "/" + state.renderMisses + "/" + state.renderEvictions);
            message(sender, TextFormatting.WHITE, "  measure cache: " + state.measureCacheSize + "/" + state.measureCacheMax
                    + " h/m/e=" + state.measureHits + "/" + state.measureMisses + "/" + state.measureEvictions);
            if (BuildFeatures.RENDER_STATS) {
                BackendTextSegmenter.DebugState segments = BackendTextSegmenter.debugState();
                message(sender, TextFormatting.WHITE, "  segments: " + (segments.enabled() ? "on" : "off")
                        + " attempts=" + segments.attempts() + " runs=" + segments.segmentedRuns()
                        + " rejected=" + segments.rejectedRuns());
            }
        } else if (manager.isSfrActive()) {
            FontSet.DebugState state = manager.getSfrDebugState();
            if (state != null) message(sender, TextFormatting.WHITE,
                    "  glyph cache: " + state.glyphInfoCacheSize() + "/" + state.bakedGlyphCacheSize());
        }
    }

    private static void reload(ICommandSender sender) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> FontManager.INSTANCE.reload(minecraft.getResourceManager()));
        message(sender, TextFormatting.GREEN, "Font reload scheduled.");
    }

    private static void test(ICommandSender sender) {
        TextRenderBackend backend = FontManager.INSTANCE.getModernTextBackend();
        if (backend == null || !backend.isReady()) {
            message(sender, TextFormatting.RED, "No modern text backend is available.");
            return;
        }
        String sample = NeofontrenderBranding.displayName() + " fi العربية 😀";
        message(sender, TextFormatting.AQUA, "Measured sample width: "
                + backend.measureStructuredAtSize(StructuredTextRuntime.parse(sample),
                0xFFFFFFFF, false, NeofontrenderConfig.fontSize()));
    }

    private static void benchmark(ICommandSender sender, String[] args) {
        int iterations = 2000;
        boolean async = args.length >= 2 && "async".equalsIgnoreCase(args[1]);
        boolean atlas = args.length >= 2 && "atlas".equalsIgnoreCase(args[1]);
        boolean sdf = args.length >= 2 && "sdf".equalsIgnoreCase(args[1]);
        int iterationArg = async || atlas || sdf ? 2 : 1;
        if (args.length > iterationArg) {
            try {
                iterations = Integer.parseInt(args[iterationArg]);
            } catch (NumberFormatException ignored) {
                message(sender, TextFormatting.YELLOW, "Usage: /neofontrender benchmark [sdf|async|atlas] [100-20000]");
                return;
            }
            if (iterations < 100 || iterations > 20_000) {
                message(sender, TextFormatting.YELLOW, "Usage: /neofontrender benchmark [sdf|async|atlas] [100-20000]");
                return;
            }
        }
        CosmicTextRenderer cosmic = FontManager.INSTANCE.getCosmicTextRenderer();
        if (cosmic != null && cosmic.isReady()) {
            if (atlas) {
                CosmicTextRenderer.AtlasBenchmarkResult result = cosmic.benchmarkGlyphAtlas(iterations);
                message(sender, TextFormatting.GOLD, "NFR glyph atlas benchmark (" + result.primaryFamily + ")");
                message(sender, TextFormatting.WHITE, "  iterations: " + result.iterations
                        + ", cached glyphs: " + result.glyphs
                        + ", hits/misses: " + result.hits + "/" + result.misses
                        + ", disk hits: " + result.diskHits);
                message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                        "  complete SDF: %.2f ms, atlas cold: %.2f ms, atlas hit: %.2f ms",
                        result.completeStringMillis(), result.atlasColdMillis(), result.atlasHitMillis()));
                message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                        "  speedup cold: %.2fx, hit: %.2fx", result.coldSpeedup(), result.hitSpeedup()));
                return;
            }
            if (sdf) {
                CosmicTextRenderer.SdfBenchmarkResult result = cosmic.benchmarkSdfRaster(iterations);
                message(sender, TextFormatting.GOLD, "NFR distance-field benchmark (" + result.primaryFamily + ")");
                message(sender, TextFormatting.WHITE, "  iterations: " + result.iterations);
                message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                        "  RGBA: %.2f ms, outline SDF: %.2f ms, outline MSDF: %.2f ms",
                        result.rgbaMillis(), result.sdfMillis(), result.msdfMillis()));
                return;
            }
            if (async) {
                if (!ASYNC_BENCHMARK_RUNNING.compareAndSet(false, true)) {
                    message(sender, TextFormatting.YELLOW, "NFR async raster benchmark is already running.");
                    return;
                }
                message(sender, TextFormatting.GOLD,
                        "NFR async raster benchmark started in background (workers 1/2/4/8)");
                int benchmarkIterations = iterations;
                ASYNC_BENCHMARK_EXECUTOR.execute(() -> runAsyncBenchmark(cosmic, sender, benchmarkIterations));
                return;
            }
            CosmicTextRenderer.BenchmarkResult result = cosmic.benchmarkCharacterCache(iterations);
            message(sender, TextFormatting.GOLD, "NFR character benchmark (" + result.primaryFamily + ")");
            message(sender, TextFormatting.WHITE, "  iterations: " + result.iterations);
            message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                    "  exact: %.2f ms, probed character: %.2f ms, speedup: %.2fx",
                    result.exactMillis(), result.characterMillis(), result.speedup()));
            message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                    "  width error: max=%.5f avg=%.5f", result.maxWidthError, result.averageWidthError));
            message(sender, TextFormatting.WHITE,
                    "  character cache restored: " + result.restoredCharacterCache);
            return;
        }
        FontSet awt = FontManager.INSTANCE.getDefaultFontSet();
        if (awt == null || !FontManager.INSTANCE.isSfrActive()) {
            message(sender, TextFormatting.YELLOW, "No active Cosmic or AWT replacement backend is available.");
            message(sender, TextFormatting.WHITE, "Current backend: " + FontManager.INSTANCE.getBackendVersion());
            return;
        }
        FontSet.BenchmarkResult result = awt.benchmarkCharacterLayout(iterations);
        message(sender, TextFormatting.GOLD, "NFR AWT fixed-run benchmark");
        message(sender, TextFormatting.WHITE, "  iterations: " + result.iterations);
        message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                "  exact: %.2f ms, fixed-run: %.2f ms, speedup: %.2fx",
                result.exactMillis(), result.fixedMillis(), result.speedup()));
        message(sender, TextFormatting.WHITE, String.format(java.util.Locale.ROOT,
                "  width error: max=%.5f avg=%.5f", result.maxWidthError, result.averageWidthError));
    }

    private static void runAsyncBenchmark(CosmicTextRenderer cosmic, ICommandSender sender, int iterations) {
        List<String> lines = new ArrayList<>();
        try {
            for (int workers : new int[] {1, 2, 4, 8}) {
                CosmicTextRenderer.AsyncBenchmarkResult result =
                        cosmic.benchmarkAsyncRendering(iterations, workers);
                lines.add(String.format(java.util.Locale.ROOT,
                        "  %d workers: sync %.2f ms, async %.2f ms, speedup %.2fx, mailbox %.2f ms, completed %d/%d",
                        result.workers, result.synchronousMillis(), result.asynchronousMillis(),
                        result.speedup(), result.communicationMillis(), result.completed,
                        result.iterations));
            }
            Minecraft.getMinecraft().addScheduledTask(() -> {
                message(sender, TextFormatting.GOLD, "NFR async raster benchmark completed");
                for (String line : lines) message(sender, TextFormatting.WHITE, line);
            });
        } catch (Throwable error) {
            String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            Minecraft.getMinecraft().addScheduledTask(() ->
                    message(sender, TextFormatting.RED, "NFR async raster benchmark failed: " + detail));
        } finally {
            ASYNC_BENCHMARK_RUNNING.set(false);
        }
    }

    private static void gui(ICommandSender sender) {
        Minecraft.getMinecraft().addScheduledTask(NeofontrenderEmojiTestScreen::open);
        message(sender, TextFormatting.GREEN, "Opening text test screen.");
    }

    private static void message(ICommandSender sender, TextFormatting color, String text) {
        sender.sendMessage(new TextComponentString(color + text));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, SUBCOMMANDS) : Collections.emptyList();
    }
}
