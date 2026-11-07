package neofontrender.core.font.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderPerfStats;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.OptionalGlProgramBinding;

/**
 * Batches multiple glyph renders into a single draw call when they share the same texture.
 *
 * <p>The class intentionally submits through the ordinary 1.12 Tessellator.  When Actinium is
 * installed its MixinTessellator routes this draw into the persistent streaming uploader; without
 * Actinium the same class remains a self-contained NFR path and can be disabled by policy.</p>
 *
 * <p>This reduces the number of GL draw calls from O(n) to O(k) where k is the number of
 * distinct texture pages used by the glyphs.</p>
 */
public class TextRunBatcher {

    private static final int MAX_VERTICES = 65536; // ~16384 quads, well under 2MB buffer limit

    // x, y, u, v, r, g, b, a per vertex. Reusing this storage avoids one heap object per glyph.
    private final float[] vertexData = new float[MAX_VERTICES * 8];
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
        float f = FontRenderTuning.alignToPixel(x + glyph.getLeft());
        float f1 = FontRenderTuning.alignToPixel(x + glyph.getRight());
        float f4 = FontRenderTuning.alignToPixel(y + glyph.getUp());
        float f5 = FontRenderTuning.alignToPixel(y + glyph.getDown());
        float slant0 = italic ? 1.0F - 0.25F * glyph.getUp() : 0.0F;
        float slant1 = italic ? 1.0F - 0.25F * glyph.getDown() : 0.0F;
        return addQuad(glyph.getTextureLocation(), glyph.getRasterScale(),
                f + slant0, f4, f + slant1, f5,
                f1 + slant1, f5, f1 + slant0, f4,
                glyph.getU0(), glyph.getV0(), glyph.getU0(), glyph.getV1(),
                glyph.getU1(), glyph.getV1(), glyph.getU1(), glyph.getV0(),
                red, green, blue, alpha);
    }

    /** Adds an already transformed quad while preserving submission order. */
    public boolean addQuad(ResourceLocation texture, float rasterScale,
                           float x0, float y0, float x1, float y1,
                           float x2, float y2, float x3, float y3,
                           float u0, float v0, float u1, float v1,
                           float u2, float v2, float u3, float v3,
                           float red, float green, float blue, float alpha) {
        boolean flushed = false;
        if (currentTexture != null && (!currentTexture.equals(texture)
                || Float.compare(currentRasterScale, rasterScale) != 0)) {
            flush();
            flushed = true;
        }
        if (currentTexture == null) {
            currentTexture = texture;
            currentRasterScale = rasterScale;
        }
        writeVertex(vertexCount, x0, y0, u0, v0, red, green, blue, alpha);
        writeVertex(vertexCount + 1, x1, y1, u1, v1, red, green, blue, alpha);
        writeVertex(vertexCount + 2, x2, y2, u2, v2, red, green, blue, alpha);
        writeVertex(vertexCount + 3, x3, y3, u3, v3, red, green, blue, alpha);
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
        if (vertexCount <= 0) {
            return;
        }

        final int flushedVertices = vertexCount;
        final long started = FontRenderPerfStats.enabled() ? System.nanoTime() : 0L;
        Minecraft.getMinecraft().getTextureManager().bindTexture(currentTexture);
        FontRenderTuning.applyBoundTextureFilter(currentRasterScale);

        try (FontRenderPipeline.State state = FontRenderPipeline.begin(currentRasterScale, false);
             OptionalGlProgramBinding.ExternalProgramScope ignored = state.preserveExternalProgram()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            for (int vertex = 0; vertex < vertexCount; vertex++) {
                int offset = vertex * 8;
                buffer.pos(vertexData[offset], vertexData[offset + 1], 0.0D)
                        .tex(vertexData[offset + 2], vertexData[offset + 3])
                        .color(vertexData[offset + 4], vertexData[offset + 5],
                                vertexData[offset + 6], vertexData[offset + 7])
                        .endVertex();
            }

            tessellator.draw();
        }
        if (FontRenderPerfStats.enabled()) {
            FontRenderPerfStats.recordBatchFlush(flushedVertices, System.nanoTime() - started);
        }

        currentTexture = null;
        vertexCount = 0;
    }

    /**
     * Check if there are any pending glyphs.
     */
    public boolean hasPendingGlyphs() {
        return vertexCount > 0;
    }

    private void writeVertex(int vertex, float x, float y, float u, float v,
                             float red, float green, float blue, float alpha) {
        int offset = vertex * 8;
        vertexData[offset] = x;
        vertexData[offset + 1] = y;
        vertexData[offset + 2] = u;
        vertexData[offset + 3] = v;
        vertexData[offset + 4] = red;
        vertexData[offset + 5] = green;
        vertexData[offset + 6] = blue;
        vertexData[offset + 7] = alpha;
    }
}
