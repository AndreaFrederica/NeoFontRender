package neofontrender.core.font.awt;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderTuning;

import javax.annotation.Nullable;

/**
 * A lazy proxy that defers glyph rasterization until the first render call.
 *
 * <p>This reduces startup time by only baking glyphs that are actually displayed.
 * On first render, it delegates to the real {@link GlyphInfo#bake(FontTexture)} and
 * caches the result.</p>
 */
public class LazyBakedGlyph extends BakedGlyph {

    private static final ResourceLocation PLACEHOLDER = new ResourceLocation("neofontrender", "lazy_placeholder");

    private final GlyphInfo glyphInfo;
    private final FontTexture atlas;
    private final FontSet fontSet;
    private final int codePoint;
    private BakedGlyph delegate;

    public LazyBakedGlyph(GlyphInfo glyphInfo, FontTexture atlas, FontSet fontSet, int codePoint) {
        // Use placeholder UVs - will be replaced on first bake
        super(PLACEHOLDER, 0, 0, 0, 0,
              glyphInfo.getAdvance(false), 0, 0, 0, 1.0F);
        this.glyphInfo = glyphInfo;
        this.atlas = atlas;
        this.fontSet = fontSet;
        this.codePoint = codePoint;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        ensureBaked();
        return delegate != null ? delegate.getTextureLocation() : PLACEHOLDER;
    }

    @Override
    public float getRasterScale() {
        ensureBaked();
        return delegate != null ? delegate.getRasterScale() : 1.0F;
    }

    @Override
    public void render(boolean italic, float x, float y,
                       float red, float green, float blue, float alpha) {
        ensureBaked();
        if (delegate != null) {
            delegate.render(italic, x, y, red, green, blue, alpha);
        }
    }

    @Override
    public void writeVertices(Tessellator tessellator, boolean italic, float x, float y,
                              float red, float green, float blue, float alpha) {
        ensureBaked();
        if (delegate != null) {
            delegate.writeVertices(tessellator, italic, x, y, red, green, blue, alpha);
        }
    }

    @Override
    public void renderEffect(float x0, float y0, float x1, float y1, float depth,
                             float red, float green, float blue, float alpha) {
        ensureBaked();
        if (delegate != null) {
            delegate.renderEffect(x0, y0, x1, y1, depth, red, green, blue, alpha);
        }
    }

    @Override
    public float visualLeft() {
        ensureBaked();
        return delegate != null ? delegate.visualLeft() : 0;
    }

    @Override
    public float visualRight() {
        ensureBaked();
        return delegate != null ? delegate.visualRight() : 0;
    }

    @Override
    public float visualTop() {
        ensureBaked();
        return delegate != null ? delegate.visualTop() : 0;
    }

    @Override
    public float visualBottom() {
        ensureBaked();
        return delegate != null ? delegate.visualBottom() : 0;
    }

    private void ensureBaked() {
        if (delegate == null) {
            delegate = glyphInfo.bake(atlas);
            if (delegate != null) {
                // Replace ourselves in the cache with the real baked glyph
                fontSet.replaceGlyph(codePoint, delegate);
            }
        }
    }
}
