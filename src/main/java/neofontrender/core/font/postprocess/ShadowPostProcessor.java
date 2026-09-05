package neofontrender.core.font.postprocess;

import neofontrender.api.text.ModernTextLayout;
import neofontrender.api.text.postprocess.TextPostProcessContext;
import neofontrender.api.text.postprocess.TextPostProcessor;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.backend.SampledShadowTextRenderResult;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.ShadowColorPolicy;
import neofontrender.core.font.support.ShadowRenderSpec;

/** Built-in modern shadow composition. Legacy FontRenderer shadows remain outside this stage. */
public final class ShadowPostProcessor implements TextPostProcessor {
    public static final ShadowPostProcessor INSTANCE = new ShadowPostProcessor();
    public static final String ID = "neofontrender:shadow";

    private ShadowPostProcessor() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int priority() {
        // Build the complete foreground/shadow layer before visual effects capture it.
        return 200;
    }

    @Override
    public boolean isEnabled() {
        // Availability belongs to the registered node. The route decides whether the current
        // draw requests modern composition, while explicit API previews may provide a draft spec.
        return true;
    }

    @Override
    public boolean supports(TextPostProcessContext context) {
        return context.shadowRequested()
                && context.backend() != null;
    }

    @Override
    public ModernTextLayout process(TextPostProcessContext context, ModernTextLayout current) {
        ShadowRenderSpec spec = context.shadowSpec();
        if (spec.opacity <= 0.0F) return current;
        TextRenderResult nativeComposite = context.backend()
                .renderStructuredModernShadowAtSize(context.structuredText(),
                        context.baseArgb(), context.fontSize(), spec);
        if (nativeComposite != null) {
            return ModernTextLayout.fromPostProcessResult(
                    nativeComposite, current.alphaForPostProcess());
        }
        if (!context.backend().shouldRenderShadow(context.formattedText())) return current;
        TextRenderResult shadow = context.shadowSource();
        if (shadow == null || shadow == TextRenderResult.EMPTY) return current;

        float geometryScale = context.fontSize()
                / Math.max(1.0F, NeofontrenderConfig.fontSize());
        float colorAlpha = ShadowColorPolicy.SOLID.equals(spec.colorMode)
                ? (spec.color >>> 24) / 255.0F : 1.0F;
        TextRenderResult layered = new SampledShadowTextRenderResult(
                shadow, current.resultForPostProcess(), spec.offsetX, spec.offsetY,
                spec.blurRadius, spec.opacity * colorAlpha, geometryScale);
        return ModernTextLayout.fromPostProcessResult(layered, current.alphaForPostProcess());
    }
}
