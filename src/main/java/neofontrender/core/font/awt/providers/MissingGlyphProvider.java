package neofontrender.core.font.awt.providers;

import neofontrender.core.font.awt.BakedGlyph;
import neofontrender.core.font.awt.FontTexture;
import neofontrender.core.font.awt.GlyphInfo;
import neofontrender.core.font.awt.GlyphProvider;
import neofontrender.core.font.support.FontPixelUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Provides a tofu / missing-character box for any code point.
 * Always queried last in the provider chain.
 */
public class MissingGlyphProvider implements GlyphProvider {

    private static final int BOX_WIDTH = 5;
    private static final int BOX_HEIGHT = 8;

    @Override
    public GlyphInfo getGlyph(int codePoint) {
        return MISSING_GLYPH_INFO;
    }

    @Override
    public Collection<Integer> getSupportedGlyphs() {
        return Collections.emptyList();
    }

    private static final GlyphInfo MISSING_GLYPH_INFO = new GlyphInfo() {
        @Override
        public float getAdvance() {
            return BOX_WIDTH + 1;
        }

        @Override
        public BakedGlyph bake(FontTexture atlas) {
            int[] pixels = new int[BOX_WIDTH * BOX_HEIGHT];
            for (int y = 0; y < BOX_HEIGHT; y++) {
                for (int x = 0; x < BOX_WIDTH; x++) {
                    boolean border = x == 0 || x == BOX_WIDTH - 1 || y == 0 || y == BOX_HEIGHT - 1;
                    pixels[y * BOX_WIDTH + x] = border ? 0xFFFFFFFF : FontPixelUtils.TRANSPARENT_WHITE;
                }
            }
            return atlas.add(pixels, BOX_WIDTH, BOX_HEIGHT,
                    0, BOX_WIDTH + 1, 0, BOX_HEIGHT, 1.0F);
        }
    };
}