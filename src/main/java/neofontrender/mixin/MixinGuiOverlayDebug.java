package neofontrender.mixin;

import net.minecraft.client.gui.GuiOverlayDebug;
import neofontrender.Tags;
import neofontrender.build.BuildFeatures;
import neofontrender.client.render.sign.SignOcclusionCuller;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.backend.BackendTextSegmenter;
import neofontrender.core.font.cosmic.CosmicTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {
    private static long sfr$lastSnapshotMillis;
    private static String[] sfr$lastSnapshot = new String[0];

    @Inject(method = "getDebugInfoRight", at = @At("RETURN"))
    private void sfr$appendFontDebug(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();
        if (lines == null || !NeofontrenderConfig.isLoaded()) return;
        long now = System.currentTimeMillis();
        if (now - sfr$lastSnapshotMillis >= 500L) {
            sfr$lastSnapshotMillis = now;
            sfr$lastSnapshot = sfr$buildSnapshot();
        }
        if (sfr$lastSnapshot.length == 0) return;
        lines.add("");
        java.util.Collections.addAll(lines, sfr$lastSnapshot);
    }

    private static String[] sfr$buildSnapshot() {
        FontManager manager = FontManager.INSTANCE;
        String activeEngine = manager.isCosmicActive() ? "cosmic" : manager.isSfrActive() ? "sfr" : "vanilla";
        List<String> lines = new ArrayList<>();
        lines.add("NFR: " + activeEngine + " mod=" + Tags.VERSION + " core=" + manager.getBackendVersion()
                + " cfg=" + NeofontrenderConfig.renderingEngine()
                + " adv=" + NeofontrenderConfig.advancedStringMode()
                + " scale=" + String.format(Locale.ROOT, "%.1f", NeofontrenderConfig.fontOversample()));

        if (manager.isSfrActive()) {
            FontSet.DebugState state = manager.getSfrDebugState();
            if (state != null) {
                lines.add("NFR SFR: glyph info/baked=" + state.glyphInfoCacheSize() + "/" + state.bakedGlyphCacheSize()
                        + " h/m=" + state.glyphInfoHits() + "/" + state.glyphInfoMisses()
                        + " " + state.bakedGlyphHits() + "/" + state.bakedGlyphMisses());
                lines.add("NFR SFR layout: " + state.layoutCacheSize() + "/" + state.layoutCacheMax()
                        + " h/m/e=" + state.layoutCacheHits() + "/" + state.layoutCacheMisses()
                        + "/" + state.layoutCacheEvictions());
            }
        } else if (manager.isCosmicActive()) {
            CosmicTextRenderer renderer = manager.getCosmicTextRenderer();
            if (renderer == null) {
                lines.add("NFR Cosmic: renderer unavailable");
            } else {
                CosmicTextRenderer.DebugState state = renderer.debugState();
                lines.add("NFR Cosmic tex: " + state.renderCacheSize + "/" + state.renderCacheMax
                        + " h/m/e=" + state.renderHits + "/" + state.renderMisses + "/" + state.renderEvictions
                        + " native=" + state.nativeRasterCount);
                lines.add("NFR Cosmic measure: " + state.measureCacheSize + "/" + state.measureCacheMax
                        + " h/m/e=" + state.measureHits + "/" + state.measureMisses + "/" + state.measureEvictions
                        + " font=" + state.primaryFamily);
                if (BuildFeatures.RENDER_STATS && NeofontrenderConfig.debugRenderStats()) {
                    BackendTextSegmenter.DebugState segments = BackendTextSegmenter.debugState();
                    lines.add("NFR seg: " + (segments.enabled() ? "on" : "off")
                            + " attempts=" + segments.attempts() + " runs=" + segments.segmentedRuns()
                            + " reject=" + segments.rejectedRuns() + " segs=" + segments.emittedSegments());
                    lines.add(SignOcclusionCuller.debugLine());
                }
            }
        }
        return lines.toArray(new String[0]);
    }
}
