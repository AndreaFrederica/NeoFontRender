package neofontrender.core.font.postprocess;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.api.text.ModernTextLayout;
import neofontrender.api.text.gl.TextGlComponentApi;
import neofontrender.api.text.postprocess.TextPostProcessApi;
import neofontrender.api.text.postprocess.TextPostProcessContext;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.text.StructuredText;

import java.util.function.Supplier;

/** Creates a structured foreground result and applies registered post-processors once. */
public final class TextPostProcessPipeline {
    private static boolean initialized;

    private TextPostProcessPipeline() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        TextGlComponentApi.register(BrilliantCaptureRenderer.INSTANCE);
        MinecraftForge.EVENT_BUS.register(BrilliantCaptureRenderer.INSTANCE);
        TextPostProcessApi.register(BrilliantTextPostProcessor.INSTANCE);
        TextPostProcessApi.register(ShadowPostProcessor.INSTANCE);
    }

    public static ModernTextLayout renderStructured(TextRenderBackend backend, StructuredText text,
                                                    int baseArgb, float fontSize,
                                                    boolean shadowRequested,
                                                    ShadowRenderSpec shadowSpec) {
        if (backend == null || !backend.isReady() || text == null || text.plainText().isEmpty()) {
            return ModernTextLayout.fromPostProcessResult(TextRenderResult.EMPTY, alpha(baseArgb));
        }
        float size = Float.isFinite(fontSize) ? Math.max(1.0F, fontSize) : 8.0F;
        ShadowRenderSpec effectiveSpec = shadowSpec == null
                ? ShadowRenderSpec.fromConfig() : shadowSpec;
        TextRenderResult foreground = backend.renderStructuredAtSize(text, baseArgb, false, size);
        Supplier<TextRenderResult> shadowSource = shadowRequested
                ? () -> backend.renderStructuredShadowSourceAtSize(
                text, baseArgb, size, effectiveSpec) : null;
        TextPostProcessContext context = new TextPostProcessContext(
                text, baseArgb, size, shadowRequested, backend, effectiveSpec, shadowSource);
        ModernTextLayout initial = ModernTextLayout.fromPostProcessResult(
                foreground, alpha(baseArgb));
        return TextPostProcessApi.process(context, initial);
    }

    private static float alpha(int argb) {
        int value = argb >>> 24;
        return value == 0 ? 1.0F : value / 255.0F;
    }
}
