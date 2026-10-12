package neofontrender.core.font.cosmic;

/**
 * HDR texture boundary for Cosmic text.
 *
 * <p>Main selects RGBA16F glyph textures only while Cleanroom's Kirino render delegate owns the
 * world framebuffer, because that delegate is the only place where text is composited at float
 * precision. 1.7.10 has neither Kirino nor an HDR world delegate, so HDR is never available here
 * and every cosmic raster is uploaded as a premultiplied RGBA8 texture.</p>
 *
 * <p>The class keeps main's name and call site so {@code CosmicTextRenderer} stays identical across
 * branches; this is the single place to change if a 1.7.10 HDR pipeline ever appears.</p>
 */
public final class KirinoHdrCompat {
    private KirinoHdrCompat() {
    }

    public static boolean useHdrTexture() {
        return false;
    }
}
