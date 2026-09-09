package neofontrender.core.font.route;

import neofontrender.api.text.ModernTextLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.postprocess.TextPostProcessPipeline;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.animation.TextAnimationFrame;

/** Draw/measure implementation for the configured modern backend. */
final class ModernStructuredRouteLayout extends AbstractStructuredRouteLayout {
    private final TextRenderBackend backend;
    private final float drawOffset;

    ModernStructuredRouteLayout(TextRenderRouteRequest request, TextRenderBackend backend) {
        this(request, backend, inlineGeometry(request, request.fontSize(),
                Math.max(1, request.font().FONT_HEIGHT)));
    }

    private ModernStructuredRouteLayout(TextRenderRouteRequest request, TextRenderBackend backend,
                                        InlineGeometry geometry) {
        super(ModernStructuredTextRoute.ID, request,
                backend.measureStructuredAtSize(request.structuredText(), request.argb(), false,
                        request.fontSize()),
                geometry.height);
        this.backend = backend;
        this.drawOffset = geometry.drawOffset;
    }

    @Override
    float measure(StructuredText text) {
        return backend.measureStructuredAtSize(text, request.argb(), false, request.fontSize());
    }

    @Override
    float inlineDrawOffset() {
        return drawOffset;
    }

    @Override
    public void draw(float x, float y) {
        try (TextAnimationFrame.Scope ignored = TextAnimationFrame.openAutomatic(
                request.source(), x, y + drawOffset)) {
            boolean shadowEnabled = request.shadow()
                    && !"none".equals(NeofontrenderConfig.shadowMode())
                    && NeofontrenderConfig.shadowOpacity() > 0.0F
                    && backend.shouldRenderShadow(request.structuredText().plainText());
            boolean modernShadow = shadowEnabled && NeofontrenderConfig.modernShadowEnabled();
            if (shadowEnabled && !modernShadow) {
                TextRenderResult shadow = backend.renderStructuredAtSize(request.structuredText(),
                        request.argb(), true, request.fontSize());
                float offset = NeofontrenderConfig.shadowLength();
                shadow.draw(x + offset, y + drawOffset + offset,
                        NeofontrenderConfig.shadowOpacity());
            }
            TextPostProcessPipeline.initialize();
            ModernTextLayout layout = TextPostProcessPipeline.renderStructured(backend,
                    request.structuredText(), request.argb(), request.fontSize(), modernShadow,
                    ShadowRenderSpec.fromConfig());
            layout.draw(x, y + drawOffset);
        }
    }
}
