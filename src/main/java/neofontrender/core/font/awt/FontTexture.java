package neofontrender.core.font.awt;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontPixelUtils;
import neofontrender.core.font.support.FontRenderTuning;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of dynamic texture pages that form a font atlas.
 * Uses shelf-packing to allocate glyph rectangles efficiently.
 *
 * <p>Each page is a {@link DynamicTexture} registered with the
 * {@link TextureManager} so that vanilla's bindTexture works.</p>
 */
public class FontTexture implements AutoCloseable {

    private static final int PAGE_SIZE = 1024;
    private static final int PADDING = 2;

    private final TextureManager textureManager;
    private final ResourceLocation baseLocation;
    private final float rasterScale;
    private final List<Page> pages = new ArrayList<>();

    public FontTexture(TextureManager textureManager, ResourceLocation baseLocation, float rasterScale) {
        this.textureManager = textureManager;
        this.baseLocation = baseLocation;
        this.rasterScale = rasterScale;
    }

    /**
     * Allocate texture space for a rasterized glyph and return a {@link BakedGlyph}
     * with the correct UV coordinates.
     *
     * @param pixels   ARGB pixel data (row-major)
     * @param pw       pixel width
     * @param ph       pixel height
     * @param left     left bearing (in pixels, scaled)
     * @param right    right bearing
     * @param up       top bearing
     * @param down     bottom bearing
     * @param oversample  oversample factor used when rasterizing
     * @return baked glyph, or {@code null} if no page could fit it
     */
    @Nullable
    public BakedGlyph add(int[] pixels, int pw, int ph,
                          float left, float right, float up, float down,
                          float oversample) {
        if (pw <= 0 || ph <= 0) {
            return null;
        }

        for (Page page : pages) {
            BakedGlyph glyph = page.add(pixels, pw, ph, left, right, up, down, oversample);
            if (glyph != null) {
                return glyph;
            }
        }

        ResourceLocation pageLoc = new ResourceLocation(
                baseLocation.getNamespace(),
                baseLocation.getPath() + "/" + pages.size());
        Page page = new Page(textureManager, pageLoc, PAGE_SIZE, PAGE_SIZE, rasterScale);
        pages.add(page);
        return page.add(pixels, pw, ph, left, right, up, down, oversample);
    }

    @Override
    public void close() {
        for (Page page : pages) {
            page.close();
        }
        pages.clear();
    }

    private static class Page implements AutoCloseable {
        final DynamicTexture texture;
        final ResourceLocation location;
        final int width;
        final int height;
        final float rasterScale;
        final List<Shelf> shelves = new ArrayList<>();
        int usedHeight = 0;

        Page(TextureManager textureManager, ResourceLocation location, int width, int height, float rasterScale) {
            this.texture = new DynamicTexture(width, height);
            this.location = location;
            this.width = width;
            this.height = height;
            this.rasterScale = rasterScale;
            FontRenderTuning.applyFontTextureFilter(this.texture, rasterScale);
            int[] data = this.texture.getTextureData();
            for (int i = 0; i < data.length; i++) {
                data[i] = FontPixelUtils.TRANSPARENT_WHITE;
            }
            this.texture.updateDynamicTexture();
            FontRenderTuning.applyFontTextureFilter(this.texture, rasterScale);
            textureManager.loadTexture(location, this.texture);
            FontRenderTuning.applyFontTextureFilter(this.texture, rasterScale);
        }

        @Nullable
        BakedGlyph add(int[] pixels, int pw, int ph,
                       float left, float right, float up, float down,
                       float oversample) {
            int needW = pw + PADDING;
            int needH = ph + PADDING;

            if (needW > width || needH > height) {
                return null;
            }

            for (Shelf shelf : shelves) {
                if (shelf.height >= needH && shelf.usedWidth + needW <= width) {
                    return place(shelf, pixels, pw, ph, left, right, up, down, oversample);
                }
            }

            if (usedHeight + needH <= height) {
                Shelf shelf = new Shelf(0, usedHeight, needH);
                shelves.add(shelf);
                usedHeight += needH;
                return place(shelf, pixels, pw, ph, left, right, up, down, oversample);
            }

            return null;
        }

        private BakedGlyph place(Shelf shelf, int[] pixels, int pw, int ph,
                                 float left, float right, float up, float down,
                                 float oversample) {
            int x = shelf.usedWidth;
            int y = shelf.y;
            shelf.usedWidth += pw + PADDING;

            int[] dest = this.texture.getTextureData();
            for (int row = 0; row < ph; row++) {
                int destRow = y + row;
                int srcRow = row;
                System.arraycopy(pixels, srcRow * pw, dest, destRow * width + x, pw);
            }
            this.texture.updateDynamicTexture();

            float u0 = (float) x / (float) width;
            float v0 = (float) y / (float) height;
            float u1 = (float) (x + pw) / (float) width;
            float v1 = (float) (y + ph) / (float) height;

            return new BakedGlyph(
                    this.location,
                    u0, u1, v0, v1,
                    left, right, up, down,
                    this.rasterScale
            );
        }

        @Override
        public void close() {
            this.texture.deleteGlTexture();
        }
    }

    private static class Shelf {
        final int x;
        final int y;
        final int height;
        int usedWidth = 0;

        Shelf(int x, int y, int height) {
            this.x = x;
            this.y = y;
            this.height = height;
        }
    }
}