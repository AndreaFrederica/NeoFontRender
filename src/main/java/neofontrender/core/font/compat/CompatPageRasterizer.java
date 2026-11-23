package neofontrender.core.font.compat;

import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.cosmic.CosmicColorGlyphs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * Rasterizes one compat page: 256 code points drawn into a 16x16 grid of fixed cells.
 *
 * <p>The provider decides how the grid is filled. {@code awt} draws monochrome text through
 * Java2D; {@code cosmic} asks the native per-page entry and copies each glyph mask into its cell,
 * leaving color glyphs to the RGBA cache. Both paths place glyphs on the same baseline and clip to
 * the cell.</p>
 */
final class CompatPageRasterizer {

    private CompatPageRasterizer() {
    }

    /**
     * @return an 8-bit coverage mask of {@code side * side} bytes, or {@code null} when the page has
     *         no covered code points.
     */
    static byte[] raster(CompatFontEngine engine, int pageIndex) {
        return engine.cosmicProvider()
                ? rasterCosmic(engine, pageIndex)
                : rasterAwt(engine, pageIndex);
    }

    private static byte[] rasterAwt(CompatFontEngine engine, int pageIndex) {
        int cell = engine.cellPixels();
        int pitch = engine.pitchPixels();
        int side = engine.pageSidePixels();
        int ascent = engine.ascentPixels();
        int base = pageIndex << 8;

        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        boolean any = false;
        try {
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            for (int offset = 0; offset < 256; offset++) {
                int codePoint = base + offset;
                if (!engine.isCovered(codePoint)) {
                    continue;
                }
                AwtTtfGlyphProvider provider = engine.providerAt(codePoint);
                if (provider == null) {
                    continue;
                }
                int cellX = (offset % 16) * pitch + engine.gutterPixels();
                int cellY = (offset / 16) * pitch + engine.gutterPixels();
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        provider.textAntialiasHint());
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        provider.fractionalMetrics()
                                ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                                : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
                graphics.setFont(provider.awtFont());
                Shape oldClip = graphics.getClip();
                graphics.setClip(cellX, cellY, cell, cell);
                graphics.drawString(new String(Character.toChars(codePoint)),
                        cellX, cellY + ascent);
                graphics.setClip(oldClip);
                any = true;
            }
        } finally {
            graphics.dispose();
        }
        if (!any) {
            return null;
        }
        byte[] mask = new byte[side * side];
        for (int y = 0; y < side; y++) {
            int row = y * side;
            for (int x = 0; x < side; x++) {
                mask[row + x] = (byte) (image.getRGB(x, y) >>> 24);
            }
        }
        return mask;
    }

    private static byte[] rasterCosmic(CompatFontEngine engine, int pageIndex) {
        int cell = engine.cellPixels();
        int pitch = engine.pitchPixels();
        int side = engine.pageSidePixels();
        int ascent = engine.ascentPixels();
        int gutter = engine.gutterPixels();
        int base = pageIndex << 8;
        byte[] mask = new byte[side * side];
        boolean any = false;
        java.util.Map<Integer, CosmicColorGlyphs.Glyph> glyphs = engine.cosmicPage(pageIndex);
        for (int offset = 0; offset < 256; offset++) {
            int codePoint = base + offset;
            CosmicColorGlyphs.Glyph glyph = glyphs.get(codePoint);
            if (glyph == null) {
                continue;
            }
            // Measuring and rasterizing share one native call: the advance recorded here is what
            // ensureMetrics reads later for the same code point.
            engine.recordCosmicAdvance(codePoint, glyph.advance());
            engine.recordCosmicColor(codePoint, glyph.color());
            if (glyph.color() || glyph.mask() == null
                    || glyph.width() <= 0 || glyph.height() <= 0) {
                // Color glyphs are drawn from the RGBA cache; keep the page empty for them.
                any |= glyph.color();
                continue;
            }
            int cellX = (offset % 16) * pitch + gutter;
            int cellY = (offset / 16) * pitch + gutter;
            int left = cellX + Math.round(glyph.offsetX());
            int top = cellY + ascent - Math.round(glyph.baseline() * glyph.scale())
                    + Math.round(glyph.offsetY());
            byte[] source = glyph.mask();
            for (int y = 0; y < glyph.height(); y++) {
                int targetY = top + y;
                if (targetY < cellY || targetY >= cellY + cell) {
                    continue;
                }
                int sourceRow = y * glyph.width();
                int targetRow = targetY * side;
                for (int x = 0; x < glyph.width(); x++) {
                    int targetX = left + x;
                    if (targetX < cellX || targetX >= cellX + cell) {
                        continue;
                    }
                    mask[targetRow + targetX] = source[sourceRow + x];
                }
            }
            any = true;
        }
        return any ? mask : null;
    }
}
