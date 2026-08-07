package neofontrender.core.font.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderTuning;

import java.util.ArrayList;
import java.util.List;

/**
 * Batches multiple glyph renders into a single draw call when they share the same texture.
 *
 * <p>This reduces the number of GL draw calls from O(n) to O(k) where k is the number of
 * distinct texture pages used by the glyphs.</p>
 */
public class TextRunBatcher {

    private static final int MAX_VERTICES = 65536; // ~16384 quads, well under 2MB buffer limit

    private final List<GlyphEntry> entries = new ArrayList<>();
    private ResourceLocation currentTexture;
    private float currentRasterScale;
    private int vertexCount;

    /**
     * Add a glyph to the current batch.
     *
     * @param glyph    the baked glyph to render
     * @param italic   whether to apply italic slant
     * @param x        base X position
     * @param y        base Y position
     * @param red      color red [0,1]
     * @param green    color green [0,1]
     * @param blue     color blue [0,1]
     * @param alpha    color alpha [0,1]
     * @return true if the batch was flushed due to texture change or buffer limit
     */
    public boolean addGlyph(BakedGlyph glyph, boolean italic, float x, float y,
                            float red, float green, float blue, float alpha) {
        ResourceLocation tex = glyph.getTextureLocation();
        boolean flushed = false;

        if (currentTexture != null && !currentTexture.equals(tex)) {
            flush();
            flushed = true;
        }

        if (currentTexture == null) {
            currentTexture = tex;
            currentRasterScale = glyph.getRasterScale();
        }

        entries.add(new GlyphEntry(glyph, italic, x, y, red, green, blue, alpha));
        vertexCount += 4;

        if (vertexCount >= MAX_VERTICES) {
            flush();
            flushed = true;
        }

        return flushed;
    }

    /**
     * Flush all accumulated glyphs to the GPU in a single draw call.
     */
    public void flush() {
        if (entries.isEmpty()) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(currentTexture);
        FontRenderTuning.applyBoundTextureFilter(currentRasterScale);

        try (FontRenderPipeline.State ignored = FontRenderPipeline.begin(currentRasterScale, false)) {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();

            for (GlyphEntry entry : entries) {
                entry.glyph.writeVertices(tessellator, entry.italic, entry.x, entry.y,
                        entry.red, entry.green, entry.blue, entry.alpha);
            }

            tessellator.draw();
        }

        entries.clear();
        currentTexture = null;
        vertexCount = 0;
    }

    /**
     * Check if there are any pending glyphs.
     */
    public boolean hasPendingGlyphs() {
        return !entries.isEmpty();
    }

    private static class GlyphEntry {
        final BakedGlyph glyph;
        final boolean italic;
        final float x;
        final float y;
        final float red;
        final float green;
        final float blue;
        final float alpha;

        GlyphEntry(BakedGlyph glyph, boolean italic, float x, float y,
                   float red, float green, float blue, float alpha) {
            this.glyph = glyph;
            this.italic = italic;
            this.x = x;
            this.y = y;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }
}
