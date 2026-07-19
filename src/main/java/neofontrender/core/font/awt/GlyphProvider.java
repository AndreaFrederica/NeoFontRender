package neofontrender.core.font.awt;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * A source of glyph information. Providers are queried in order until one returns
 * a non-null glyph for a given code point.
 *
 * <p>Equivalent to 1.20.1 {@code com.mojang.blaze3d.font.GlyphProvider}.</p>
 */
public interface GlyphProvider extends AutoCloseable {

    @Override
    default void close() {
    }

    /**
     * Return the {@link GlyphInfo} for the given code point, or {@code null} if this
     * provider does not support it.
     */
    @Nullable
    GlyphInfo getGlyph(int codePoint);

    /**
     * Return a collection of all code points this provider can potentially serve.
     * This is used by {@link FontSet} to build width-buckets for obfuscated text.
     */
    Collection<Integer> getSupportedGlyphs();
}