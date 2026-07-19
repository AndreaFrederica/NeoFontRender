package neofontrender.core.font.awt;

import javax.annotation.Nullable;

/**
 * Information about a single glyph that has not yet been baked into a texture atlas.
 *
 * <p>Equivalent to 1.20.1 {@code com.mojang.blaze3d.font.GlyphInfo}.</p>
 */
public interface GlyphInfo {

    /**
     * The horizontal advance of this glyph in pixels (not scaled by GUI scale).
     */
    float getAdvance();

    /**
     * Advance including bold offset if the glyph is rendered in bold style.
     */
    default float getAdvance(boolean bold) {
        return this.getAdvance() + (bold ? this.getBoldOffset() : 0.0F);
    }

    /**
     * Extra pixels added to the advance when bold is active.
     */
    default float getBoldOffset() {
        return 1.0F;
    }

    /**
     * Pixel offset used for drop-shadow rendering.
     */
    default float getShadowOffset() {
        return 1.0F;
    }

    /**
     * Rasterize this glyph and bake it into the given atlas, returning a {@link BakedGlyph}
     * that can be rendered directly.
     *
     * @param atlas the atlas to allocate texture space from
     * @return the baked glyph, or {@code null} if baking failed
     */
    @Nullable
    BakedGlyph bake(FontTexture atlas);
}