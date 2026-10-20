package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.cosmic.CosmicTextRenderer;

import java.awt.Desktop;
import java.io.File;
import java.util.Locale;

/** Cache statistics and bounded, frame-sliced glyph-atlas prewarm controls. */
public final class NfrCacheManagementPanel extends Widget<NfrCacheManagementPanel>
        implements ILayoutWidget {
    private static final int BATCH_LIMIT = 768;
    private static final int RANGE_BATCH_LIMIT = 4096;
    private static final int CHUNK_CODE_POINTS = 24;

    private volatile boolean batchRunning;
    private volatile int batchDone;
    private volatile int batchTotal;
    private volatile String batchStatus = "-";
    private String batchText = "";
    private int batchOffset;
    private long lastSnapshotNanos;
    private CosmicTextRenderer.CacheManagementState snapshot;

    public int preferredHeight() { return 190; }

    @Override
    public boolean layoutWidgets() { return true; }

    public void clear() {
        CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
        if (renderer == null) {
            batchStatus = tr("neofontrender.gui.cache.unavailable");
            return;
        }
        renderer.clearCaches(true);
        batchRunning = false;
        batchStatus = tr("neofontrender.gui.cache.cleared");
        snapshot = null;
    }

    public void openDirectory() {
        CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
        String value = renderer == null ? "" : renderer.cacheManagementState().directory;
        if (value == null || value.isEmpty() || "-".equals(value)) return;
        try {
            File directory = new File(value);
            if (!directory.exists()) directory.mkdirs();
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(directory);
        } catch (Throwable error) {
            neofontrender.NeoFontRender.LOGGER.warn("Unable to open Cosmic cache directory", error);
        }
    }

    /** Starts a bounded prewarm. Each chunk is run by the client queue on a later frame. */
    public void startBatch() {
        startBatch(commonGlyphSet(BATCH_LIMIT));
    }

    /** Starts a bounded prewarm for a user-selected inclusive code-point range. */
    public void startBatch(int firstCodePoint, int lastCodePoint) {
        startBatch(rangeGlyphSet(firstCodePoint, lastCodePoint, RANGE_BATCH_LIMIT));
    }

    private void startBatch(String text) {
        if (batchRunning) return;
        CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
        if (renderer == null || !renderer.isReady()) {
            batchStatus = tr("neofontrender.gui.cache.unavailable");
            return;
        }
        batchText = text == null ? "" : text;
        batchOffset = 0;
        batchDone = 0;
        batchTotal = batchText.codePointCount(0, batchText.length());
        if (batchTotal == 0) {
            batchStatus = tr("neofontrender.gui.cache.range_empty");
            return;
        }
        batchRunning = true;
        batchStatus = tr("neofontrender.gui.cache.batch_running");
        Minecraft.getMinecraft().addScheduledTask(this::runBatchChunk);
    }

    private void runBatchChunk() {
        if (!batchRunning) return;
        CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
        if (renderer == null || !renderer.isReady()) {
            batchRunning = false;
            batchStatus = tr("neofontrender.gui.cache.unavailable");
            return;
        }
        int end = batchOffset;
        int count = 0;
        while (end < batchText.length() && count < CHUNK_CODE_POINTS) {
            end += Character.charCount(batchText.codePointAt(end));
            count++;
        }
        if (end > batchOffset) {
            renderer.prewarmGlyphAtlas(batchText.substring(batchOffset, end));
            batchOffset = end;
            batchDone += count;
        }
        if (batchOffset >= batchText.length()) {
            batchRunning = false;
            batchStatus = tr("neofontrender.gui.cache.batch_complete");
        } else {
            Minecraft.getMinecraft().addScheduledTask(this::runBatchChunk);
        }
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        int width = Math.max(260, getArea().w());
        int height = Math.max(preferredHeight(), getArea().h());
        Gui.drawRect(4, 4, width - 4, height - 4, 0x66131A24);
        font.drawString(tr("neofontrender.gui.cache.management"), 10, 10, 0xFFE6ECF3);
        CosmicTextRenderer.CacheManagementState state = state();
        if (state == null || !state.available) {
            font.drawString(tr("neofontrender.gui.cache.unavailable"), 10, 29, 0xFFFFB4B4);
            return;
        }
        font.drawString(String.format(Locale.ROOT, "%s: %d   %s: %d   %s: %d",
                tr("neofontrender.gui.cache.text_entries"), state.renderEntries,
                tr("neofontrender.gui.cache.measure_entries"), state.measureEntries,
                tr("neofontrender.gui.cache.character_entries"), state.characterEntries),
                10, 29, 0xFFD4DCE8);
        font.drawString(String.format(Locale.ROOT, "%s: %d   hit %d / miss %d / disk %d",
                tr("neofontrender.gui.cache.glyph_entries"), state.glyphEntries,
                state.glyphHits, state.glyphMisses, state.glyphDiskHits),
                10, 47, 0xFFD4DCE8);
        font.drawString(String.format(Locale.ROOT, "%s: %d files, %s",
                tr("neofontrender.gui.cache.disk"), state.diskEntries, state.diskSize()),
                10, 65, 0xFFD4DCE8);
        font.drawString(String.format(Locale.ROOT, "%s: %d files, %s   %s: %d files, %s",
                tr("neofontrender.gui.cache.sdf_mask"),
                state.diskEntries, state.diskSize(),
                tr("neofontrender.gui.cache.sdf_outline"), state.sdfOutlineDiskEntries,
                diskSize(state.sdfOutlineDiskBytes)), 10, 83, 0xFFD4DCE8);
        font.drawString(String.format(Locale.ROOT, "%s: %d files, %s",
                tr("neofontrender.gui.cache.msdf"),
                state.msdfDiskEntries, diskSize(state.msdfDiskBytes)), 10, 101, 0xFFD4DCE8);
        String directory = state.directory;
        if (directory.length() > 68) directory = "..." + directory.substring(directory.length() - 65);
        font.drawString(tr("neofontrender.gui.cache.directory") + ": " + directory,
                10, 119, 0xFF9DAABD);
        String progress = batchRunning
                ? String.format(Locale.ROOT, "%s (%d/%d)", batchStatus, batchDone, batchTotal)
                : batchStatus;
        font.drawString(tr("neofontrender.gui.cache.batch") + ": " + progress,
                10, 137, batchRunning ? 0xFFFFD166 : 0xFF8FE3CF);
        if (batchRunning && batchTotal > 0) {
            int barWidth = Math.max(80, width - 20);
            Gui.drawRect(10, 155, 10 + barWidth, 163, 0x503C4656);
            int fill = (int) Math.round(barWidth * batchDone / (double) batchTotal);
            if (fill > 0) Gui.drawRect(10, 155, 10 + fill, 163, 0xFFFFD166);
        }
    }

    private static String diskSize(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MiB", bytes / (1024.0 * 1024.0));
    }

    private CosmicTextRenderer.CacheManagementState state() {
        long now = System.nanoTime();
        if (snapshot == null || now - lastSnapshotNanos > 500_000_000L) {
            CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
            snapshot = renderer == null ? null : renderer.cacheManagementState();
            lastSnapshotNanos = now;
        }
        return snapshot;
    }

    private static String commonGlyphSet(int limit) {
        StringBuilder result = new StringBuilder(limit);
        appendRange(result, 0x20, 0x7E, limit);
        appendRange(result, 0xA0, 0xFF, limit);
        appendRange(result, 0x2500, 0x257F, limit);
        appendRange(result, 0x3000, 0x303F, limit);
        appendRange(result, 0x4E00, 0x9FFF, limit);
        return result.toString();
    }

    private static String rangeGlyphSet(int first, int last, int limit) {
        int start = Math.max(0, Math.min(0x10FFFF, first));
        int end = Math.max(0, Math.min(0x10FFFF, last));
        if (end < start) {
            int swap = start;
            start = end;
            end = swap;
        }
        StringBuilder result = new StringBuilder(Math.min(limit, Math.max(16, end - start + 1)));
        for (int codePoint = start;
             codePoint <= end && result.codePointCount(0, result.length()) < limit;
             codePoint++) {
            if (codePoint >= 0xD800 && codePoint <= 0xDFFF) continue;
            result.appendCodePoint(codePoint);
        }
        return result.toString();
    }

    private static void appendRange(StringBuilder result, int first, int last, int limit) {
        for (int codePoint = first; codePoint <= last && result.codePointCount(0, result.length()) < limit; codePoint++) {
            result.appendCodePoint(codePoint);
        }
    }

    private static String tr(String key) { return I18n.format(key); }
}
