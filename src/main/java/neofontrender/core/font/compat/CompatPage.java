package neofontrender.core.font.compat;

import net.minecraft.util.ResourceLocation;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * One 16x16 compat glyph page: 256 code points of the same high byte, laid out in a 16x16 grid.
 *
 * <p>The page image is kept as a deflated 8-bit coverage mask, the same memory trade-off as
 * SmoothFont's compressed pages. Raw masks are only materialised while rasterising/uploading a
 * page; a full 256-page CJK working set therefore costs a few megabytes of heap instead of
 * hundreds. The GPU texture is white with that mask in the alpha channel. Per-cell ink bounds are
 * precomputed so drawing uses a tight quad instead of the full cell. That matters for vanilla's
 * shadow pass: oversized transparent quads let a following glyph's shadow fall inside the
 * previous glyph and produce dark fringes.</p>
 */
final class CompatPage {

    final int index;
    final int side;
    final int cell;
    final int pitch;
    final int gutter;
    /** Distance from the cell top to the baseline, in raster pixels. */
    final int ascentPixels;
    final float rasterScale;
    /** Distance from the cell top to the baseline, in GUI pixels. */
    final float ascentGui;
    final byte[] compressedPixels;
    final ResourceLocation location;
    CompatPageTexture texture;
    /** Per-cell ink box: {minX, minY, maxXExclusive, maxYExclusive}, pen-origin relative. */
    final int[] inkBounds = new int[256 * 4];
    long lastUsed;

    CompatPage(int index, int side, int cell, int pitch, int gutter, int ascentPixels,
               float rasterScale, byte[] pixels, ResourceLocation location, CompatPageTexture texture) {
        this.index = index;
        this.side = side;
        this.cell = cell;
        this.pitch = pitch;
        this.gutter = gutter;
        this.ascentPixels = ascentPixels;
        this.rasterScale = rasterScale;
        this.ascentGui = ascentPixels / rasterScale;
        this.compressedPixels = deflate(pixels);
        this.location = location;
        this.texture = texture;
        this.lastUsed = System.nanoTime();
        computeInkBounds(pixels);
    }

    byte[] decompressPixels() {
        return inflate(compressedPixels, side * side);
    }

    private void computeInkBounds(byte[] pixels) {
        for (int glyph = 0; glyph < 256; glyph++) {
            int cellX = (glyph % 16) * pitch + gutter;
            int cellY = (glyph / 16) * pitch + gutter;
            int minX = cell;
            int minY = cell;
            int maxX = 0;
            int maxY = 0;
            for (int y = 0; y < cell; y++) {
                int row = (cellY + y) * side + cellX;
                for (int x = 0; x < cell; x++) {
                    if (pixels[row + x] != 0) {
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x + 1 > maxX) maxX = x + 1;
                        if (y + 1 > maxY) maxY = y + 1;
                    }
                }
            }
            int base = glyph * 4;
            if (maxX <= minX || maxY <= minY) {
                inkBounds[base] = 0;
                inkBounds[base + 1] = 0;
                inkBounds[base + 2] = 0;
                inkBounds[base + 3] = 0;
            } else {
                inkBounds[base] = Math.max(0, minX - 1);
                inkBounds[base + 1] = Math.max(0, minY - 1);
                inkBounds[base + 2] = Math.min(cell, maxX + 1);
                inkBounds[base + 3] = Math.min(cell, maxY + 1);
            }
        }
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED, true);
        try {
            deflater.setInput(data);
            deflater.finish();
            byte[] buffer = new byte[Math.max(256, data.length / 2)];
            int length = 0;
            while (!deflater.finished()) {
                if (length == buffer.length) {
                    byte[] larger = new byte[buffer.length * 2];
                    System.arraycopy(buffer, 0, larger, 0, length);
                    buffer = larger;
                }
                length += deflater.deflate(buffer, length, buffer.length - length);
            }
            byte[] result = new byte[length];
            System.arraycopy(buffer, 0, result, 0, length);
            return result;
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(byte[] data, int expected) {
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(data);
            byte[] result = new byte[expected];
            int offset = 0;
            while (!inflater.finished() && offset < expected) {
                offset += inflater.inflate(result, offset, expected - offset);
            }
            return offset == expected ? result : new byte[0];
        } catch (DataFormatException error) {
            return new byte[0];
        } finally {
            inflater.end();
        }
    }

    /** The cell origin in page pixels for a code point. */
    int cellX(int codePoint) {
        return ((codePoint & 0xFF) % 16) * pitch + gutter;
    }

    int cellY(int codePoint) {
        return ((codePoint & 0xFF) / 16) * pitch + gutter;
    }
}
