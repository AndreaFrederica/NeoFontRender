package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.arc3d.Arc3DApi;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.cosmic.CosmicTextRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/** One click benchmark runner and Arc3D-colored result chart for the settings screen. */
public final class NfrBenchmarkPanel extends Widget<NfrBenchmarkPanel> implements ILayoutWidget {
    private static final int TOTAL_STEPS = 15;
    private static final int DEFAULT_ITERATIONS = 200;
    private static final int QUICK_ITERATIONS = 100;
    private static final int MIN_ITERATIONS = 100;
    private static final int MAX_ITERATIONS = 2000;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "NFR-GuiBenchmark");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running;
    private volatile String status = "Ready";
    private volatile List<Bar> bars = Collections.emptyList();
    private volatile String best = "-";
    private volatile int completedSteps;
    private volatile String progressLabel = "Ready";
    private volatile double progressValue;
    private volatile double subProgressValue;
    private volatile String subProgressLabel = "Ready";
    private volatile List<String> logs = Collections.emptyList();
    private volatile String iterationsText = Integer.toString(DEFAULT_ITERATIONS);
    private volatile int activeIterations = DEFAULT_ITERATIONS;
    private volatile boolean activeQuick;

    public int preferredHeight() {
        // Twelve result rows use 22 px each and start below the title/status area. Keep enough
        // room for the final async worker row instead of letting ModularUI clip it.
        return 640;
    }

    @Override
    public boolean layoutWidgets() {
        return true;
    }

    public void start() {
        start(false);
    }

    public void startQuick() {
        start(true);
    }

    public String iterationsText() {
        return iterationsText;
    }

    public void setIterationsText(String value) {
        iterationsText = value == null ? "" : value;
    }

    public void readLastResult() {
        if (running) return;
        Path file = resultsDirectory().resolve("benchmark-last.tsv");
        if (!Files.isRegularFile(file)) {
            appendLog("No saved benchmark result found");
            status = "No saved result";
            return;
        }
        try {
            List<Bar> loaded = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.startsWith("# iterations=")) {
                    iterationsText = line.substring("# iterations=".length()).trim();
                    continue;
                }
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.lastIndexOf('\t');
                if (separator <= 0) continue;
                double millis = Double.parseDouble(line.substring(separator + 1));
                if (Double.isFinite(millis)) loaded.add(new Bar(line.substring(0, separator), millis));
            }
            if (loaded.isEmpty()) throw new IOException("saved result has no measurements");
            bars = Collections.unmodifiableList(loaded);
            best = fastestLabel(loaded);
            completedSteps = TOTAL_STEPS;
            progressValue = TOTAL_STEPS;
            subProgressValue = 1.0;
            progressLabel = "Loaded last result";
            subProgressLabel = "Saved result";
            status = "Loaded last result";
            appendLog("Loaded " + loaded.size() + " measurements from " + file.getFileName());
        } catch (Throwable error) {
            status = "Could not read saved result";
            appendLog("ERROR reading result: " + error.getMessage());
        }
    }

    public void openResultsFolder() {
        Path directory = resultsDirectory();
        try {
            Files.createDirectories(directory);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(directory.toFile());
            else appendLog("Result folder: " + directory.toAbsolutePath());
        } catch (Throwable error) {
            appendLog("ERROR opening result folder: " + error.getMessage());
            neofontrender.NeoFontRender.LOGGER.warn("Unable to open benchmark result directory", error);
        }
    }

    private static Path resultsDirectory() {
        return Minecraft.getMinecraft().gameDir.toPath().resolve("neofontrender").resolve("benchmarks");
    }

    private void start(boolean quick) {
        if (running) return;
        CosmicTextRenderer cosmic = FontManager.INSTANCE.getCosmicTextRenderer();
        if (cosmic == null || !cosmic.isReady()) {
            status = "Cosmic backend is unavailable";
            bars = Collections.emptyList();
            best = "-";
            return;
        }
        int iterations = quick ? QUICK_ITERATIONS : parseIterations(iterationsText);
        activeIterations = iterations;
        activeQuick = quick;
        running = true;
        status = (quick ? "Quick test: " : "Full test: ") + iterations + " samples per combination...";
        bars = Collections.emptyList();
        best = "-";
        completedSteps = 0;
        progressValue = 0.0;
        subProgressValue = 0.0;
        subProgressLabel = quick ? "Quick test" : "Full test";
        logs = Collections.emptyList();
        progressLabel = "Preparing benchmark...";
        appendLog("Benchmark queued (" + iterations + " samples); creating isolated backend");
        EXECUTOR.execute(() -> runIsolated(cosmic, iterations));
    }

    private static int parseIterations(String value) {
        try {
            return Math.max(MIN_ITERATIONS, Math.min(MAX_ITERATIONS, Integer.parseInt(value.trim())));
        } catch (RuntimeException ignored) {
            return DEFAULT_ITERATIONS;
        }
    }

    private void runIsolated(CosmicTextRenderer source, int iterations) {
        CosmicTextRenderer benchmark = null;
        try {
            benchmark = source.createBenchmarkRenderer();
            log("Isolated benchmark backend ready");
            run(benchmark, iterations);
        } catch (Throwable error) {
            reportFailure(error);
        } finally {
            if (benchmark != null) {
                CosmicTextRenderer renderer = benchmark;
                Minecraft.getMinecraft().addScheduledTask(renderer::close);
            }
        }
    }

    private void run(CosmicTextRenderer cosmic, int iterations) {
        try {
            List<Bar> result = new ArrayList<>();
            progressLabel = "SDF preparation";
            log("Starting SDF preparation");
            CosmicTextRenderer.SdfBenchmarkResult sdf = cosmic.benchmarkSdfRaster(iterations,
                    listener(0));
            result.add(new Bar("RGBA preparation", sdf.rgbaMillis()));
            result.add(new Bar("Legacy SDF preparation", sdf.legacySdfNanos / 1_000_000.0));
            result.add(new Bar("SDF preparation", sdf.sdfMillis()));
            result.add(new Bar("SDF memory hit + decode", sdf.sdfCachedNanos / 1_000_000.0));
            result.add(new Bar("MSDF preparation", sdf.msdfMillis()));
            result.add(new Bar("MSDF memory hit + decode", sdf.msdfCachedNanos / 1_000_000.0));
            publishPartial(result);

            progressLabel = "Character cache";
            log("Starting character cache benchmark");
            CosmicTextRenderer.BenchmarkResult character = cosmic.benchmarkCharacterCache(iterations,
                    listener(6));
            result.add(new Bar("Exact text width", character.exactMillis()));
            completedSteps = 7;
            publishPartial(result, "Exact text width measured");
            result.add(new Bar("Probed character width", character.characterMillis()));
            completedSteps = 8;
            publishPartial(result, "Probed character width measured");

            // Atlas construction and upload touch OpenGL, so marshal only this section back to the
            // Minecraft client thread while the rest of the suite stays off the render loop.
            progressLabel = "Glyph atlas (Minecraft thread)";
            log("Starting glyph atlas benchmark on the client thread");
            FutureTask<CosmicTextRenderer.AtlasBenchmarkResult> atlasTask =
                    new FutureTask<>(() -> cosmic.benchmarkGlyphAtlas(iterations, listener(8)));
            Minecraft.getMinecraft().addScheduledTask(atlasTask);
            CosmicTextRenderer.AtlasBenchmarkResult atlas = atlasTask.get();
            result.add(new Bar("Atlas cold", atlas.atlasColdMillis()));
            completedSteps = 10;
            publishPartial(result, "Atlas cold measured");
            result.add(new Bar("Atlas hit", atlas.atlasHitMillis()));
            completedSteps = 11;
            publishPartial(result);

            for (int workers : new int[] {1, 2, 4, 8}) {
                progressLabel = "Async rendering (" + workers + " workers)";
                log("Starting async benchmark with " + workers + " workers");
                CosmicTextRenderer.AsyncBenchmarkResult async =
                        cosmic.benchmarkAsyncRendering(iterations, workers, listener(11 + (workers == 1 ? 0
                                : workers == 2 ? 1 : workers == 4 ? 2 : 3)));
                result.add(new Bar("Async " + workers + " workers", async.asynchronousMillis()));
                completedSteps++;
                publishPartial(result);
            }
            publish(result);
        } catch (Throwable error) {
            reportFailure(error);
        }
    }

    private void reportFailure(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String detail = root.getMessage() == null
                ? root.getClass().getSimpleName() : root.getMessage();
        String rendererSource = null;
        if (error instanceof LinkageError || root instanceof LinkageError) {
            try {
                rendererSource = String.valueOf(CosmicTextRenderer.class.getProtectionDomain()
                        .getCodeSource().getLocation());
            } catch (Throwable ignored) { }
        }
        final String source = rendererSource;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            running = false;
            status = "Benchmark failed: " + detail;
            progressLabel = "Failed";
            appendLog("ERROR: " + detail);
            if (source != null) appendLog("CosmicTextRenderer loaded from " + source);
        });
    }

    private CosmicTextRenderer.BenchmarkListener listener(int baseStep) {
        return new CosmicTextRenderer.BenchmarkListener() {
            @Override
            public void progress(String phase, int completed, int total) {
                double fraction = total <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0,
                        completed / (double) total));
                double step = baseStep + fraction;
                if (phase.equals("RGBA preparation")) step = fraction;
                else if (phase.equals("Legacy SDF preparation")) step = 1 + fraction;
                else if (phase.equals("SDF preparation")) step = 2 + fraction;
                else if (phase.startsWith("SDF memory")) step = 3 + fraction;
                else if (phase.equals("MSDF preparation")) step = 4 + fraction;
                else if (phase.startsWith("MSDF memory")) step = 5 + fraction;
                else if (phase.startsWith("Character exact")) step = 6 + fraction;
                else if (phase.startsWith("Character cache")) step = 7 + fraction;
                else if (phase.startsWith("Atlas complete-string")) step = 8 + fraction;
                else if (phase.startsWith("Atlas cold")) step = 9 + fraction;
                else if (phase.startsWith("Atlas hit")) step = 10 + fraction;
                else if (phase.startsWith("Warmup")) step = progressValue;
                else if (phase.startsWith("Async sync")) step = baseStep + fraction * 0.25;
                updateProgress(step, phase + " " + completed + "/" + total);
            }

            @Override
            public void log(String message) {
                NfrBenchmarkPanel.this.log(message);
            }

            @Override
            public void result(String label, double millis) {
                publishMeasured(new Bar(label, millis));
            }
        };
    }

    private void updateProgress(double value, String label) {
        runOnClient(() -> {
            progressValue = Math.max(progressValue, Math.min(TOTAL_STEPS, value));
            progressLabel = label;
            int separator = label.lastIndexOf(' ');
            subProgressLabel = label;
            if (separator > 0) subProgressValue = parseFraction(label.substring(separator + 1));
            completedSteps = Math.min(TOTAL_STEPS, (int) Math.floor(progressValue));
        });
    }

    private static double parseFraction(String suffix) {
        try {
            int slash = suffix.indexOf('/');
            if (slash <= 0) return 0.0;
            int done = Integer.parseInt(suffix.substring(0, slash));
            int total = Integer.parseInt(suffix.substring(slash + 1));
            return total <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, done / (double) total));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private void log(String message) {
        if (message == null || message.isEmpty()) return;
        runOnClient(() -> appendLog(message));
    }

    private static void runOnClient(Runnable action) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.isCallingFromMinecraftThread()) action.run();
        else minecraft.addScheduledTask(action);
    }

    private void appendLog(String message) {
        List<String> next = new ArrayList<>(logs);
        next.add(message);
        if (next.size() > 32) next = new ArrayList<>(next.subList(next.size() - 32, next.size()));
        logs = Collections.unmodifiableList(next);
    }

    private void publishMeasured(Bar measured) {
        runOnClient(() -> {
            List<Bar> next = new ArrayList<>(bars);
            boolean replaced = false;
            for (int index = 0; index < next.size(); index++) {
                if (next.get(index).label.equals(measured.label)) {
                    next.set(index, measured);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) next.add(measured);
            bars = Collections.unmodifiableList(next);
            best = fastestLabel(next);
        });
    }

    private static String fastestLabel(List<Bar> values) {
        Bar fastest = null;
        for (Bar bar : values) {
            if (bar.millis > 0.0 && (fastest == null || bar.millis < fastest.millis)) fastest = bar;
        }
        return fastest == null ? "-" : fastest.label + String.format(Locale.ROOT,
                " (%.2f ms)", fastest.millis);
    }

    private void publish(List<Bar> result) {
        publishResult(result, true);
    }

    private void publishPartial(List<Bar> result) {
        publishResult(result, false, null);
    }

    private void publishPartial(List<Bar> result, String message) {
        publishResult(result, false, message);
    }

    private void publishResult(List<Bar> result, boolean complete) {
        publishResult(result, complete, null);
    }

    private void publishResult(List<Bar> result, boolean complete, String message) {
        List<Bar> snapshot = new ArrayList<>(result);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            List<Bar> next = new ArrayList<>(bars);
            for (Bar measured : snapshot) {
                boolean replaced = false;
                for (int index = 0; index < next.size(); index++) {
                    if (next.get(index).label.equals(measured.label)) {
                        next.set(index, measured);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) next.add(measured);
            }
            bars = Collections.unmodifiableList(next);
            best = fastestLabel(next);
            if (message != null) appendLog(message);
            if (complete) {
                status = "Complete";
                completedSteps = TOTAL_STEPS;
                progressValue = TOTAL_STEPS;
                subProgressValue = 1.0;
                progressLabel = "Complete";
                subProgressLabel = "Complete";
                running = false;
                saveResult(next);
                appendLog("Benchmark complete");
            }
        });
    }

    private void saveResult(List<Bar> values) {
        try {
            Path directory = resultsDirectory();
            Files.createDirectories(directory);
            List<String> lines = new ArrayList<>();
            lines.add("# NeoFontRender benchmark result");
            lines.add("# mode=" + (activeQuick ? "quick" : "full"));
            lines.add("# iterations=" + activeIterations);
            lines.add("# timestamp=" + System.currentTimeMillis());
            for (Bar value : values) lines.add(value.label + "\t" +
                    String.format(Locale.ROOT, "%.6f", value.millis));
            Files.write(directory.resolve("benchmark-last.tsv"), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            Files.write(directory.resolve("benchmark-" + System.currentTimeMillis() + ".tsv"), lines,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            appendLog("Saved result to " + directory.resolve("benchmark-last.tsv").toAbsolutePath());
        } catch (Throwable error) {
            appendLog("ERROR saving result: " + error.getMessage());
            neofontrender.NeoFontRender.LOGGER.warn("Unable to save benchmark result", error);
        }
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        int width = Math.max(260, getArea().w());
        // Draw only inside the section's declared height. Using getArea().h() here can be much
        // larger than the section when the scroll viewport is laid out, which paints over the
        // Run button placed immediately below this panel.
        int height = preferredHeight();
        Gui.drawRect(4, 4, width - 4, height - 4, 0x66131A24);
        font.drawString("Benchmark combinations", 10, 10, 0xFFE6ECF3);
        font.drawString(status, 10, 25, running ? 0xFFFFD166 : 0xFFB8C4D4);
        font.drawString("Best: " + best, 10, 40, 0xFF8FE3CF);

        int progressY = 53;
        int progressWidth = Math.max(80, width - 20);
        Gui.drawRect(10, progressY, 10 + progressWidth, progressY + 8, 0x503C4656);
        int progress = Math.max(0, Math.min(TOTAL_STEPS, completedSteps));
        double shownProgress = Math.max(0.0, Math.min(TOTAL_STEPS, progressValue));
        int fillWidth = (int) Math.round(progressWidth * (shownProgress / TOTAL_STEPS));
        if (fillWidth > 0) {
            int fill = running ? 0xFFFFD166 : 0xFF66F2B3;
            Gui.drawRect(10, progressY, 10 + fillWidth, progressY + 8, fill);
        }
        font.drawString(progress + " / " + TOTAL_STEPS + "  " + progressLabel,
                10, 65, 0xFFB8C4D4);

        int subY = 79;
        int subWidth = Math.max(80, width - 20);
        Gui.drawRect(10, subY, 10 + subWidth, subY + 6, 0x503C4656);
        int subFill = (int) Math.round(subWidth * Math.max(0.0, Math.min(1.0, subProgressValue)));
        if (subFill > 0) {
            Gui.drawRect(10, subY, 10 + subFill, subY + 6,
                    running ? 0xFFFF9F43 : 0xFF66F2B3);
        }
        font.drawString("Sub: " + subProgressLabel, 10, 89, 0xFF9DAABD);

        int legendY = 98;
        font.drawString("Legend", 10, legendY, 0xFFE6ECF3);
        Gui.drawRect(58, legendY + 2, 68, legendY + 10, 0xFF66F2B3);
        font.drawString("fastest", 72, legendY, 0xFFB8C4D4);
        Gui.drawRect(132, legendY + 2, 142, legendY + 10, 0xFF43C7FF);
        font.drawString("other (gradient)", 146, legendY, 0xFFB8C4D4);

        List<Bar> values = bars;
        if (values.isEmpty()) {
            font.drawString("Click Run to compare SDF, atlas, cache, and async paths.", 10, 105, 0xFF9DAABD);
        }
        double maximum = 0.0;
        for (Bar value : values) maximum = Math.max(maximum, value.millis);
        double minimum = Double.POSITIVE_INFINITY;
        for (Bar value : values) {
            if (value.millis > 0.0) minimum = Math.min(minimum, value.millis);
        }
        int valueWidth = 56;
        for (Bar value : values) {
            valueWidth = Math.max(valueWidth, font.getStringWidth(formatMillis(value.millis)) + 6);
        }
        valueWidth = Math.min(100, valueWidth);
        int chartX = Math.min(Math.max(112, width / 3), Math.max(72, width - valueWidth - 52));
        int chartWidth = Math.max(44, width - chartX - valueWidth - 8);
        int labelWidth = Math.max(44, chartX - 14);
        int rowY = 120;
        boolean logarithmic = minimum > 0.0 && maximum / minimum >= 16.0;
        if (!values.isEmpty()) {
            font.drawString(logarithmic ? "Results (log scale)" : "Results (linear scale)",
                    10, rowY - 13, 0xFF9DAABD);
        }
        for (int index = 0; index < values.size(); index++) {
            Bar value = values.get(index);
            int y = rowY + index * 22;
            boolean fastest = value.millis > 0.0 && value.millis == minimum;
            String label = font.trimStringToWidth(value.label, labelWidth);
            font.drawString(label, 10, y + 3, fastest ? 0xFFFFFFFF : 0xFFD4DCE8);
            double normalized = maximum <= 0.0 ? 0.0
                    : logarithmic
                    ? Math.log1p(Math.max(0.0, value.millis)) / Math.log1p(maximum)
                    : value.millis / maximum;
            int barWidth = (int) Math.round(chartWidth * Math.max(0.0, Math.min(1.0, normalized)));
            int fill = fastest ? 0xFF66F2B3 : Arc3DApi.lerpArgb(0xFF43C7FF, 0xFFD084FF,
                    maximum <= 0.0 ? 0.0F : (float) (value.millis / maximum));
            Gui.drawRect(chartX, y, chartX + chartWidth, y + 14, 0x503C4656);
            if (barWidth > 0) Gui.drawRect(chartX, y, chartX + Math.max(2, barWidth), y + 14, fill);
            String valueText = font.trimStringToWidth(formatMillis(value.millis), valueWidth - 4);
            font.drawString(valueText, chartX + chartWidth + 4, y + 3, 0xFFB8C4D4);
        }

        int logY = rowY + Math.max(1, values.size()) * 22 + 8;
        font.drawString("Log", 10, logY, 0xFFE6ECF3);
        int visible = Math.min(8, logs.size());
        int first = Math.max(0, logs.size() - visible);
        for (int index = first; index < logs.size(); index++) {
            int y = logY + 15 + (index - first) * 15;
            if (y + 10 >= height - 6) break;
            String line = logs.get(index);
            font.drawString(font.trimStringToWidth(line, Math.max(1, width - 20)), 10, y, 0xFF9DAABD);
        }
    }

    private static String formatMillis(double millis) {
        if (millis >= 10000.0) return String.format(Locale.ROOT, "%.1f ms", millis);
        return String.format(Locale.ROOT, "%.2f ms", millis);
    }

    private record Bar(String label, double millis) {}
}

