package neofontrender.core.font.cosmic;

import neofontrender.api.text.TextVisualBounds;
import neofontrender.core.font.backend.TextRenderResult;
import net.minecraft.client.renderer.BufferBuilder;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Draw-ready result backed by one shared Cosmic glyph atlas texture. */
final class CosmicAtlasTextResult implements TextRenderResult {
    private final CosmicGlyphAtlas atlas;
    private final List<PlacedGlyph> glyphs;
    private final float advance;
    private final int argb;
    private final float softness;
    private final TextVisualBounds bounds;

    CosmicAtlasTextResult(CosmicGlyphAtlas atlas, List<PlacedGlyph> glyphs,
                          float advance, int argb, float softness, TextVisualBounds bounds) {
        this.atlas = atlas;
        this.glyphs = glyphs;
        this.advance = advance;
        this.argb = argb;
        this.softness = softness;
        this.bounds = bounds;
    }

    @Override
    public float advance() { return advance; }

    @Override
    public TextVisualBounds visualBounds() { return bounds; }

    @Override
    public void draw(float x, float y, float alpha) {
        if (glyphs.isEmpty()) return;
        // Glyph baking is CPU/native work; upload all misses accumulated since the previous
        // draw in one texture operation batch before binding the atlas for sampling.
        atlas.flushUploads();
        try (CosmicSdfPipeline.State state = CosmicSdfPipeline.begin()) {
            if (state.isNoop()) return;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.texture());
            float red = ((argb >>> 16) & 0xFF) / 255.0F;
            float green = ((argb >>> 8) & 0xFF) / 255.0F;
            float blue = (argb & 0xFF) / 255.0F;
            float tint = CosmicTextRenderer.premultipliedOpacity(alpha);
            CosmicSdfPipeline.setUniforms(red, green, blue, tint, softness);
            BufferBuilder buffer;
            // One atlas texture and one tessellator submission cover the complete string.
            buffer = CosmicSdfPipeline.beginBatch();
            for (PlacedGlyph placed : glyphs) {
                CosmicGlyphAtlas.Glyph glyph = placed.glyph;
                if (glyph.width <= 0.0F || glyph.height <= 0.0F) continue;
                CosmicSdfPipeline.addQuad(
                        buffer,
                        FontRenderAlignment.align(x + placed.x + glyph.x),
                        FontRenderAlignment.align(y + glyph.y),
                        glyph.width, glyph.height,
                        glyph.u1, glyph.v1, glyph.u2, glyph.v2);
            }
            CosmicSdfPipeline.endBatch();
        }
    }

    static final class PlacedGlyph {
        final CosmicGlyphAtlas.Glyph glyph;
        final float x;
        PlacedGlyph(CosmicGlyphAtlas.Glyph glyph, float x) {
            this.glyph = glyph;
            this.x = x;
        }
    }

    /** Keeps atlas drawing aligned with the legacy Cosmic text path. */
    private static final class FontRenderAlignment {
        private static float align(float value) {
            return neofontrender.core.font.support.FontRenderTuning.alignToPixel(value);
        }
    }
}
