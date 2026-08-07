package neofontrender.addons.api.inline;

import javax.annotation.Nullable;

/**
 * Finds an inline glyph beginning exactly at {@code sourceIndex}. Providers should return
 * quickly and must never perform network or disk I/O on the render thread.
 */
@FunctionalInterface
public interface InlineGlyphProvider {
    @Nullable InlineGlyphMatch match(CharSequence source, int sourceIndex);
}
