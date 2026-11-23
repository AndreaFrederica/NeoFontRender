package neofontrender.core.font.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import neofontrender.core.font.cosmic.CosmicColorGlyphs;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Color half of the compatibility renderer's two glyph caches.
 *
 * <p>Compatibility mode keeps its {@code GL_ALPHA} AWT pages for ordinary text and delegates
 * color glyphs (emoji, color fonts) to cosmic-text, which is the only rasterizer here that can
 * read CBDT/COLR tables. Every color glyph gets its own RGBA texture; there is no color page
 * allocator and no interaction with the monochrome pages.</p>
 *
 * <p>Textures are owned here, never registered with Minecraft's {@code TextureManager}, for the
 * same reason the monochrome pages are: vanilla frees names by number.</p>
 */
final class CompatColorCache {

    private static final int MAX_GLYPHS = 256;

    static final class Entry {
        final int width;
        final int height;
        final float offsetX;
        final float offsetY;
        final float advance;
        final float baseline;
        final float scale;
        final DynamicTexture texture;

        Entry(int width, int height, float offsetX, float offsetY, float advance,
              float baseline, float scale, DynamicTexture texture) {
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.advance = advance;
            this.baseline = baseline;
            this.scale = scale;
            this.texture = texture;
        }
    }

    private final LinkedHashMap<Integer, Entry> glyphs =
            new LinkedHashMap<Integer, Entry>(32, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, Entry> eldest) {
                    if (size() > MAX_GLYPHS) {
                        delete(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            };
    private final Set<Integer> notColor = new HashSet<>();
    /** Serves rasters from the engine's page cache; never calls native per glyph. */
    private final IntFunction<CosmicColorGlyphs.Glyph> source;
    private boolean unavailable;

    CompatColorCache(IntFunction<CosmicColorGlyphs.Glyph> source) {
        this.source = source;
    }

    /** Cropped RGBA bitmap and metrics for a color code point, or {@code null}. */
    private Entry resolve(int codePoint) {
        if (unavailable) return null;
        synchronized (glyphs) {
            Entry cached = glyphs.get(codePoint);
            if (cached != null) return cached;
        }
        synchronized (notColor) {
            if (notColor.contains(codePoint)) return null;
        }
        CosmicColorGlyphs.Glyph glyph = source.apply(codePoint);
        if (glyph == null || !glyph.color() || glyph.rgba() == null) {
            synchronized (notColor) {
                notColor.add(codePoint);
            }
            return null;
        }
        DynamicTexture texture = new DynamicTexture(glyph.width(), glyph.height());
        int[] data = texture.getTextureData();
        if (data == null || data.length < glyph.rgba().length) {
            texture.deleteGlTexture();
            synchronized (notColor) {
                notColor.add(codePoint);
            }
            return null;
        }
        System.arraycopy(glyph.rgba(), 0, data, 0, glyph.rgba().length);
        texture.updateDynamicTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        Entry entry = new Entry(glyph.width(), glyph.height(), glyph.offsetX(), glyph.offsetY(),
                glyph.advance(), glyph.baseline(), glyph.scale(), texture);
        synchronized (glyphs) {
            glyphs.put(codePoint, entry);
        }
        return entry;
    }

    boolean covers(int codePoint) {
        return resolve(codePoint) != null;
    }

    float advance(int codePoint) {
        Entry entry = resolve(codePoint);
        return entry == null ? -1.0F : entry.advance;
    }

    /** Draws one color glyph whose baseline sits at {@code baselineY}. */
    boolean draw(int codePoint, float x, float baselineY, int argb) {
        Entry entry = resolve(codePoint);
        if (entry == null) return false;
        float w = entry.width / entry.scale;
        float h = entry.height / entry.scale;
        float x0 = x + entry.offsetX / entry.scale;
        // offsetX/offsetY are raster pixels; baseline is the run-top-to-baseline distance in GUI
        // pixels (the native header encodes it already divided by the raster scale).
        float y0 = baselineY - entry.baseline + entry.offsetY / entry.scale;
        float y1 = y0 + h;

        GlStateManager.bindTexture(entry.texture.getGlTextureId());
        applyColorPageEnv();
        try {
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(x0, y0, 0.0F);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(x0, y1, 0.0F);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(x0 + w, y0, 0.0F);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(x0 + w, y1, 0.0F);
            GL11.glEnd();
        } finally {
            applyMonoPageEnv();
        }
        return true;
    }

    /**
     * Color pages replace RGB with their own pixels; alpha still modulates with the primary
     * colour so the text alpha keeps working. Monochrome pages invert that: RGB from the primary
     * colour, alpha from the page.
     */
    private static void applyColorPageEnv() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
    }

    private static void applyMonoPageEnv() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
    }

    void close() {
        synchronized (glyphs) {
            for (Entry entry : glyphs.values()) delete(entry);
            glyphs.clear();
        }
        synchronized (notColor) {
            notColor.clear();
        }
        CosmicColorGlyphs.close();
    }

    private static void delete(Entry entry) {
        if (entry == null || entry.texture == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && !minecraft.isCallingFromMinecraftThread()) {
            minecraft.addScheduledTask(entry.texture::deleteGlTexture);
            return;
        }
        entry.texture.deleteGlTexture();
    }
}
