package neofontrender.mixin;

import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.skia.SkiaTextSegmenter;
import neofontrender.core.font.skia.SkijaTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {

    private static long sfr$lastSnapshotMillis;
    private static String[] sfr$lastSnapshot = new String[0];

    @Inject(method = "getDebugInfoRight", at = @At("RETURN"))
    private void sfr$appendFontDebug(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();
        if (lines == null || !NeofontrenderConfig.isLoaded()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - sfr$lastSnapshotMillis >= 500L) {
            sfr$lastSnapshotMillis = now;
            sfr$lastSnapshot = sfr$buildSnapshot();
        }

        if (sfr$lastSnapshot.length > 0) {
            lines.add("");
            for (String line : sfr$lastSnapshot) {
                lines.add(line);
            }
        }
    }

    private static String[] sfr$buildSnapshot() {
        String activeEngine = FontManager.INSTANCE.isSkiaActive()
                ? "skia"
                : FontManager.INSTANCE.isSfrActive() ? "sfr" : "vanilla";
        String base = "NFR: " + activeEngine
                + " cfg=" + NeofontrenderConfig.renderingEngine()
                + " adv=" + NeofontrenderConfig.skiaAdvancedStringMode()
                + " scale=" + String.format(java.util.Locale.ROOT, "%.1f", NeofontrenderConfig.fontOversample());

        if (FontManager.INSTANCE.isSfrActive()) {
            FontSet.DebugState state = FontManager.INSTANCE.getSfrDebugState();
            if (state == null) {
                return new String[] {base};
            }
            String glyph = "NFR SFR: glyph info/baked=" + state.glyphInfoCacheSize()
                    + "/" + state.bakedGlyphCacheSize()
                    + " buckets=" + state.glyphWidthBuckets()
                    + " h/m=" + state.glyphInfoHits() + "/" + state.glyphInfoMisses()
                    + " " + state.bakedGlyphHits() + "/" + state.bakedGlyphMisses();
            String layout = "NFR SFR layout: " + state.layoutCacheSize() + "/" + state.layoutCacheMax()
                    + " h/m/e=" + state.layoutCacheHits()
                    + "/" + state.layoutCacheMisses()
                    + "/" + state.layoutCacheEvictions();
            return new String[] {base, glyph, layout};
        }

        if (!FontManager.INSTANCE.isSkiaActive()) {
            return new String[] {base};
        }

        SkijaTextRenderer renderer = FontManager.INSTANCE.getSkijaTextRenderer();
        if (renderer == null) {
            return new String[] {base, "NFR Skia: renderer unavailable"};
        }

        SkijaTextRenderer.DebugState state = renderer.debugState();
        String gpuStatus;
        if (!state.gpuRequested()) {
            gpuStatus = "off";
        } else if (state.gpuUnavailable()) {
            gpuStatus = "fallback";
        } else if (state.gpuRasterCount() > 0L) {
            gpuStatus = "active";
        } else if (state.gpuContextCreated()) {
            gpuStatus = "ready";
        } else {
            gpuStatus = "requested";
        }

        String gpu = "NFR Skia: last=" + state.lastRasterPath()
                + " gpu=" + gpuStatus
                + " mode=" + (state.gpuRequested()
                        ? (NeofontrenderConfig.skiaGpuSubmitViaCpuTexture() ? "isolated+cpu-submit" : "isolated")
                        : "off")
                + " pmul=" + NeofontrenderConfig.enablePremultipliedAlpha()
                + " bleed=" + NeofontrenderConfig.textureEdgeBleed();
        String cache = "NFR cache: text=" + state.renderCacheSize() + "/" + state.renderCacheMax()
                + " seg=" + state.segmentCacheSize() + "/" + state.segmentCacheMax()
                + " measure=" + state.measureCacheSize() + "/" + state.measureCacheMax()
                + " rasters cpu/gpu=" + state.cpuRasterCount() + "/" + state.gpuRasterCount();
        boolean debugStats = NeofontrenderConfig.debugRenderStats();
        String cacheStats = "";
        String segments = "";
        if (debugStats) {
            cacheStats = "NFR cache stats: tex h/m/e=" + state.renderCacheHits()
                    + "/" + state.renderCacheMisses()
                    + "/" + state.renderCacheEvictions()
                    + " seg h/m/e=" + state.segmentCacheHits()
                    + "/" + state.segmentCacheMisses()
                    + "/" + state.segmentCacheEvictions()
                    + " measure h/m/e=" + state.measureCacheHits()
                    + "/" + state.measureCacheMisses()
                    + "/" + state.measureCacheEvictions();
            SkiaTextSegmenter.DebugState segmentState = SkiaTextSegmenter.debugState();
            segments = "NFR seg: " + (segmentState.enabled() ? "on" : "off")
                    + " attempts=" + segmentState.attempts()
                    + " runs=" + segmentState.segmentedRuns()
                    + " reject=" + segmentState.rejectedRuns()
                    + " segs=" + segmentState.emittedSegments();
        }
        String draw = state.lastDrawState();
        String stats = debugStats ? state.lastRasterStats() : "";

        String reason = state.lastGpuFallbackReason();
        if (reason != null && !reason.isEmpty() && state.gpuRequested() && state.gpuUnavailable()) {
            return debugStats
                    ? new String[] {base, gpu, "NFR GPU: " + reason, cache, cacheStats, segments}
                    : new String[] {base, gpu, "NFR GPU: " + reason, cache};
        }
        if (draw != null && !draw.isEmpty()) {
            if (stats != null && !stats.isEmpty()) {
                return new String[] {base, gpu, "NFR draw: " + draw, "NFR raster: " + stats, cache, cacheStats, segments};
            }
            return debugStats
                    ? new String[] {base, gpu, "NFR draw: " + draw, cache, cacheStats, segments}
                    : new String[] {base, gpu, "NFR draw: " + draw, cache};
        }
        return debugStats
                ? new String[] {base, gpu, cache, cacheStats, segments}
                : new String[] {base, gpu, cache};
    }
}
