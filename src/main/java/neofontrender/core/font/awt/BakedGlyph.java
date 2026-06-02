package neofontrender.core.font.awt;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderTuning;

/**
 * A glyph that has been uploaded to a texture atlas and can be rendered with a single quad.
 *
 * <p>Equivalent to 1.20.1 {@code net.minecraft.client.gui.font.glyphs.BakedGlyph},
 * but uses 1.12.2 {@link Tessellator} instead of {@code VertexConsumer}.</p>
 */
public class BakedGlyph {

    private final ResourceLocation textureLocation;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;
    private final float rasterScale;

    public BakedGlyph(ResourceLocation textureLocation,
                      float u0, float u1, float v0, float v1,
                      float left, float right, float up, float down,
                      float rasterScale) {
        this.textureLocation = textureLocation;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
        this.rasterScale = rasterScale;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    /**
     * Render this glyph at the given position using the current OpenGL color.
     *
     * @param italic  whether to apply italic slant
     * @param x      base X position
     * @param y      base Y position
     * @param red    color red component [0,1]
     * @param green  color green component [0,1]
     * @param blue   color blue component [0,1]
     * @param alpha  color alpha component [0,1]
     */
    public void render(boolean italic, float x, float y,
                       float red, float green, float blue, float alpha) {
        float f = FontRenderTuning.alignToPixel(x + this.left);
        float f1 = FontRenderTuning.alignToPixel(x + this.right);
        float f2 = this.up;
        float f3 = this.down;
        float f4 = FontRenderTuning.alignToPixel(y + f2);
        float f5 = FontRenderTuning.alignToPixel(y + f3);
        float slant0 = italic ? 1.0F - 0.25F * f2 : 0.0F;
        float slant1 = italic ? 1.0F - 0.25F * f3 : 0.0F;
        FontRenderTuning.applyBoundTextureFilter(rasterScale);

        try (FontRenderPipeline.State ignored = FontRenderPipeline.begin(rasterScale)) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            buffer.pos(f + slant0, f4, 0.0D)
                  .tex(this.u0, this.v0)
                  .color(red, green, blue, alpha)
                  .endVertex();
            buffer.pos(f + slant1, f5, 0.0D)
                  .tex(this.u0, this.v1)
                  .color(red, green, blue, alpha)
                  .endVertex();
            buffer.pos(f1 + slant1, f5, 0.0D)
                  .tex(this.u1, this.v1)
                  .color(red, green, blue, alpha)
                  .endVertex();
            buffer.pos(f1 + slant0, f4, 0.0D)
                  .tex(this.u1, this.v0)
                  .color(red, green, blue, alpha)
                  .endVertex();

            tessellator.draw();
        }
    }

    /**
     * Render a strikethrough or underline effect using the white glyph texture.
     *
     * @param x0     effect start X
     * @param y0     effect start Y
     * @param x1     effect end X
     * @param y1     effect end Y
     * @param depth  Z depth offset
     * @param red    color red [0,1]
     * @param green  color green [0,1]
     * @param blue   color blue [0,1]
     * @param alpha  color alpha [0,1]
     */
    public void renderEffect(float x0, float y0, float x1, float y1, float depth,
                             float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

        buffer.pos(x0, y0, depth)
              .tex(this.u0, this.v0)
              .color(red, green, blue, alpha)
              .endVertex();
        buffer.pos(x0, y1, depth)
              .tex(this.u0, this.v1)
              .color(red, green, blue, alpha)
              .endVertex();
        buffer.pos(x1, y1, depth)
              .tex(this.u1, this.v1)
              .color(red, green, blue, alpha)
              .endVertex();
        buffer.pos(x1, y0, depth)
              .tex(this.u1, this.v0)
              .color(red, green, blue, alpha)
              .endVertex();

        tessellator.draw();
    }
}
