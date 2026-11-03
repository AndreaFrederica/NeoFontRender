package neofontrender.core.font.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.OptionalGlProgramBinding;

import java.util.ArrayList;
import java.util.List;

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

    private final List<QuadEntry> entries = new ArrayList<>();
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
        entries.add(new QuadEntry(x0, y0, x1, y1, x2, y2, x3, y3,
                u0, v0, u1, v1, u2, v2, u3, v3, red, green, blue, alpha));
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

        try (FontRenderPipeline.State state = FontRenderPipeline.begin(currentRasterScale, false);
             OptionalGlProgramBinding.ExternalProgramScope ignored = state.preserveExternalProgram()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            for (QuadEntry entry : entries) {
                entry.write(buffer);
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

    private static final class QuadEntry {
        final float x0, y0, x1, y1, x2, y2, x3, y3;
        final float u0, v0, u1, v1, u2, v2, u3, v3;
        final float red, green, blue, alpha;

        QuadEntry(float x0, float y0, float x1, float y1, float x2, float y2,
                  float x3, float y3, float u0, float v0, float u1, float v1,
                  float u2, float v2, float u3, float v3,
                  float red, float green, float blue, float alpha) {
            this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2; this.x3 = x3; this.y3 = y3;
            this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1;
            this.u2 = u2; this.v2 = v2; this.u3 = u3; this.v3 = v3;
            this.red = red; this.green = green; this.blue = blue; this.alpha = alpha;
        }

        void write(BufferBuilder buffer) {
            buffer.pos(x0, y0, 0.0D).tex(u0, v0).color(red, green, blue, alpha).endVertex();
            buffer.pos(x1, y1, 0.0D).tex(u1, v1).color(red, green, blue, alpha).endVertex();
            buffer.pos(x2, y2, 0.0D).tex(u2, v2).color(red, green, blue, alpha).endVertex();
            buffer.pos(x3, y3, 0.0D).tex(u3, v3).color(red, green, blue, alpha).endVertex();
        }
    }
}
