package neofontrender.core.font.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.text.InlineContent;
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
        neofontrender.text.InlineLayout.Size size = content.layout().resolve(raster, fontSize);
        this.height = size.height();
        this.width = size.width();
        this.topOffset = content.layout().top(0.0F, 8.0F, height);
        this.argb = normalizeAlpha(argb);
        this.shadow = shadow;
    }

    @Override public float advance() { return width; }
    @Override public float visualTop() { return Math.min(0.0F, topOffset); }
    @Override public float visualBottom() { return Math.max(8.0F, topOffset + height); }

    @Override public void draw(float x, float y, float alpha) {
        InlineRaster raster = content.raster();
        if (raster == null) {
            drawPlaceholder(x, y, alpha);
            return;
        }
        ResourceLocation texture = texture(raster);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // A shadow source must be monochrome even for intrinsically colored SVG/image rasters.
        // The post-process stage has already selected the desired shadow color.
        int color = shadow || content.tint() ? argb : (argb & 0xFF000000) | 0xFFFFFF;
        float red = (color >>> 16 & 255) / 255.0F;
        float green = (color >>> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float opacity = (color >>> 24) / 255.0F * alpha;
        float top = y + topOffset;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(x, top + height, 0).tex(0, 1).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + width, top + height, 0).tex(1, 1).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + width, top, 0).tex(1, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, top, 0).tex(0, 0).color(red, green, blue, opacity).endVertex();
        Tessellator.getInstance().draw();
    }

    private void drawPlaceholder(float x, float y, float alpha) {
        GlStateManager.disableTexture2D();
        int color = argb;
        float red = (color >>> 16 & 255) / 255.0F;
        float green = (color >>> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float opacity = (color >>> 24) / 255.0F * alpha;
        float top = y + topOffset;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, top, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + width, top, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + width, top + height, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, top + height, 0).color(red, green, blue, opacity).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static synchronized ResourceLocation texture(InlineRaster raster) {
        IdentityKey lookup = new IdentityKey(raster);
        TextureEntry cached = TEXTURES.get(lookup);
        if (cached != null) return cached.location;
        BufferedImage image = new BufferedImage(raster.width(), raster.height(),
                BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, raster.width(), raster.height(), raster.argb(), 0, raster.width());
        DynamicTexture texture = new DynamicTexture(image);
        texture.setBlurMipmap(true, false);
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
