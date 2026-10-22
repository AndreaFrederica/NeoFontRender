package neofontrender.addons.api.content;

import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.addons.inline.RasterGlyphService;

/** Public bridge for optional addons that produce asynchronous raster inline content. */
public final class InlineRasterApi {
    private InlineRasterApi() {}

    public static InlineContent raster(String key, String description, int displayHeight,
                                       boolean tint, boolean matchFontLineHeight,
                                       RasterJob job) {
        if (job == null) throw new IllegalArgumentException("job must not be null");
        return RasterGlyphService.INSTANCE.glyph(key, description, displayHeight, tint,
                matchFontLineHeight, job::render);
    }

    /** Creates the renderer-independent descriptor consumed by the structured text routes. */
    public static neofontrender.text.InlineContent structuredRaster(
            String key, String description, int displayHeight, boolean tint,
            boolean matchFontLineHeight, RasterJob job) {
        if (job == null) throw new IllegalArgumentException("job must not be null");
        return RasterGlyphService.INSTANCE.structuredGlyph(key, description, displayHeight, tint,
                matchFontLineHeight, job::render);
    }

    @FunctionalInterface
    public interface RasterJob {
        java.awt.image.BufferedImage render() throws Exception;
    }
}
