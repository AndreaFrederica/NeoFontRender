package neofontrender.core.font.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.text.InlineContent;
import neofontrender.text.InlineLayout.Size;
import neofontrender.text.InlineRaster;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** GL adapter for the renderer-independent CPU raster stored in an inline span. */
public final class InlineRasterTextRenderResult implements TextRenderResult {
    private static final int MAX_TEXTURES = 256;
    private static final long MAX_TEXTURE_PIXELS = 32L * 1024L * 1024L;
    private static final Map<IdentityKey, TextureEntry> TEXTURES =
            new LinkedHashMap<>(MAX_TEXTURES + 1, 0.75F, true);
    private final InlineContent content;
    private final float height;
    private final float width;
    private final float topOffset;
    private final int argb;
    private final boolean shadow;

    public InlineRasterTextRenderResult(InlineContent content, float fontSize, int argb,
                                        boolean shadow) {
        this.content = content;
        InlineRaster raster = content.raster();
        Size size = content.layout().resolve(raster, fontSize);
        this.height = size.height();
        this.width = size.width();
        this.topOffset = content.layout().top(0.0F, 8.0F, height);
        this.argb = normalizeAlpha(argb);
        this.shadow = shadow;
    }

    @Override public float advance() { return width; }
    @Override public float visualTop() { return Math.min(0.0F, topOffset); }
    @Override public float visualBottom() { return Math.max(8.0F, topOffset + height); }

    @Override
    public void draw(float x, float y, float alpha) {
        InlineRaster raster = content.raster();
        if (raster == null) {
            drawPlaceholder(x, y, alpha);
            return;
        }
        ResourceLocation texture = texture(raster);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // A shadow source must be monochrome even for intrinsically colored SVG/image rasters.
        // The post-process stage has already selected the desired shadow color.
        int color = shadow || content.tint() ? argb : (argb & 0xFF000000) | 0xFFFFFF;
        float red = (color >>> 16 & 255) / 255.0F;
        float green = (color >>> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float opacity = (color >>> 24) / 255.0F * alpha;
        float top = y + topOffset;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(red, green, blue, opacity);
        tessellator.addVertexWithUV(x, top + height, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x + width, top + height, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x + width, top, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x, top, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    private void drawPlaceholder(float x, float y, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        int color = argb;
        float red = (color >>> 16 & 255) / 255.0F;
        float green = (color >>> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float opacity = (color >>> 24) / 255.0F * alpha;
        float top = y + topOffset;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINE_LOOP);
        tessellator.setColorRGBA_F(red, green, blue, opacity);
        tessellator.addVertex(x, top, 0.0D);
        tessellator.addVertex(x + width, top, 0.0D);
        tessellator.addVertex(x + width, top + height, 0.0D);
        tessellator.addVertex(x, top + height, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static synchronized ResourceLocation texture(InlineRaster raster) {
        IdentityKey lookup = new IdentityKey(raster);
        TextureEntry cached = TEXTURES.get(lookup);
        if (cached != null) return cached.location;
        BufferedImage image = new BufferedImage(raster.width(), raster.height(),
                BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, raster.width(), raster.height(), raster.argb(), 0, raster.width());
        DynamicTexture texture = new DynamicTexture(image);
        // 1.7.10 AbstractTexture has no setBlurMipmap, and DynamicTexture uploads with GL_NEAREST.
        // Blurred, mipmap-free filtering is the 1.12.2 setBlurMipmap(true, false) equivalent.
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("nfr_structured_inline", texture);
        TEXTURES.put(lookup, new TextureEntry(location,
                (long) raster.width() * raster.height()));
        trimTextures();
        return location;
    }

    private static void trimTextures() {
        long pixels = 0L;
        for (TextureEntry entry : TEXTURES.values()) pixels += entry.pixels;
        Iterator<Map.Entry<IdentityKey, TextureEntry>> iterator = TEXTURES.entrySet().iterator();
        while ((TEXTURES.size() > MAX_TEXTURES
                || (TEXTURES.size() > 1 && pixels > MAX_TEXTURE_PIXELS))
                && iterator.hasNext()) {
            TextureEntry entry = iterator.next().getValue();
            iterator.remove();
            pixels -= entry.pixels;
            Minecraft.getMinecraft().getTextureManager().deleteTexture(entry.location);
        }
    }

    private static final class IdentityKey {
        private final InlineRaster raster;

        IdentityKey(InlineRaster raster) { this.raster = raster; }

        @Override public boolean equals(Object other) {
            return other instanceof IdentityKey && raster == ((IdentityKey) other).raster;
        }

        @Override public int hashCode() { return System.identityHashCode(raster); }
    }

    private static final class TextureEntry {
        final ResourceLocation location;
        final long pixels;

        TextureEntry(ResourceLocation location, long pixels) {
            this.location = location;
            this.pixels = pixels;
        }
    }

    private static int normalizeAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
