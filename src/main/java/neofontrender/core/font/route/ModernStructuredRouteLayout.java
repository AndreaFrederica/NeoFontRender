package neofontrender.core.font.route;

import neofontrender.api.text.TextVisualBounds;

import neofontrender.api.text.ModernTextLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.postprocess.TextPostProcessPipeline;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.animation.TextAnimationFrame;

/** Draw/measure implementation for the configured modern backend. */
final class ModernStructuredRouteLayout extends AbstractStructuredRouteLayout {
    private final TextRenderBackend backend;
    private final float drawOffset;
    /** The raster bucket captured when this layout's logical advance was measured. */
    private final float rasterScale;

    ModernStructuredRouteLayout(TextRenderRouteRequest request, TextRenderBackend backend) {
        this(request, backend, snapshotRasterScale(request), inlineGeometry(request, request.fontSize(),
                Math.max(1, request.font().FONT_HEIGHT)));
    }

    private ModernStructuredRouteLayout(TextRenderRouteRequest request, TextRenderBackend backend,
                                        float rasterScale, InlineGeometry geometry) {
        super(ModernStructuredTextRoute.ID, request,
                measureAtRasterScale(request, backend, rasterScale),
                geometry.height);
        this.backend = backend;
        this.drawOffset = geometry.drawOffset;
        this.rasterScale = rasterScale;
    }

    @Override
    float measure(StructuredText text) {
        try (FontRenderTuning.RasterScaleScope ignored =
                     FontRenderTuning.pinRasterScale(rasterScale)) {
            return backend.measureStructuredAtSize(text, request.argb(), false, request.fontSize());
        }
    }

    @Override
    float inlineDrawOffset() {
        return drawOffset;
    }

    private boolean shadowEnabled() {
        return request.shadow() && !"none".equals(NeofontrenderConfig.shadowMode())
                && NeofontrenderConfig.shadowOpacity() > 0.0F
                && backend.shouldRenderShadow(request.structuredText().plainText());
    }

    private ModernTextLayout foreground(boolean modernShadow) {
        TextPostProcessPipeline.initialize();
        return TextPostProcessPipeline.renderStructured(backend, request.structuredText(),
                request.argb(), request.fontSize(), modernShadow, ShadowRenderSpec.fromConfig());
    }

    @Override
    public TextVisualBounds visualBounds() {
        FontRenderTuning.updateFromCurrentGlState(request.shadow());
        try (FontRenderTuning.RasterScaleScope ignored =
                     FontRenderTuning.pinRasterScale(this.rasterScale)) {
            boolean shadow = shadowEnabled();
            boolean modernShadow = shadow && NeofontrenderConfig.modernShadowEnabled();
            TextVisualBounds bounds = foreground(modernShadow)
                    .resultForPostProcess().visualBounds();
            if (shadow && !modernShadow) {
                TextRenderResult shadowResult = backend.renderStructuredAtSize(request.structuredText(),
                        request.argb(), true, request.fontSize());
                float offset = NeofontrenderConfig.shadowLength();
                bounds = bounds.union(shadowResult.visualBounds().translate(offset, offset));
            }
            return bounds.translate(0, drawOffset);
        }
    }

    @Override
    public void draw(float x, float y) {
        // Direct route users (notably the chat completion popup) do not pass through
        // FontRenderer.drawString(), where the GL text context is normally refreshed.  Refresh it
        // here and pin the selected bucket so all layers of this draw use one raster geometry.
        FontRenderTuning.updateFromCurrentGlState(request.shadow());
        try (FontRenderTuning.RasterScaleScope ignored =
                     FontRenderTuning.pinRasterScale(this.rasterScale);
             TextAnimationFrame.Scope animation = TextAnimationFrame.openAutomatic(
                     request.source(), x, y + drawOffset)) {
            boolean shadowEnabled = shadowEnabled();
            boolean modernShadow = shadowEnabled && NeofontrenderConfig.modernShadowEnabled();
            if (shadowEnabled && !modernShadow) {
                TextRenderResult shadow = backend.renderStructuredAtSize(request.structuredText(),
                        request.argb(), true, request.fontSize());
                float offset = NeofontrenderConfig.shadowLength();
                shadow.draw(x + offset, y + drawOffset + offset,
                        NeofontrenderConfig.shadowOpacity());
            }
            ModernTextLayout layout = foreground(modernShadow);
            layout.draw(x, y + drawOffset);
        }
    }

    private static float snapshotRasterScale(TextRenderRouteRequest request) {
        FontRenderTuning.updateFromCurrentGlState(request.shadow());
        return FontRenderTuning.effectiveRasterScale(NeofontrenderConfig.fontOversample());
    }

    private static float measureAtRasterScale(TextRenderRouteRequest request,
                                              TextRenderBackend backend,
                                              float rasterScale) {
        try (FontRenderTuning.RasterScaleScope ignored =
                     FontRenderTuning.pinRasterScale(rasterScale)) {
            return backend.measureStructuredAtSize(request.structuredText(), request.argb(), false,
                    request.fontSize());
        }
    }
}
