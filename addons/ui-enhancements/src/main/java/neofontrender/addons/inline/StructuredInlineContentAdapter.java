package neofontrender.addons.inline;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.core.font.inline.InlineRasterTextRenderResult;
import neofontrender.text.InlineRaster;
import neofontrender.text.InlineLayout;

import java.awt.image.BufferedImage;

/** UI interaction facade for renderer-independent structured inline content. */
public final class StructuredInlineContentAdapter
        implements neofontrender.api.text.pipeline.InlineContent {
    private final neofontrender.text.InlineContent content;

    public StructuredInlineContentAdapter(neofontrender.text.InlineContent content) {
        this.content = content;
    }

    @Override
    public int advance(FontRenderer font) {
        return (int) Math.ceil(new InlineRasterTextRenderResult(content,
                Math.max(1, font.FONT_HEIGHT), 0xFFFFFFFF, false).advance());
    }

    @Override
    public int height(FontRenderer font) {
        return Math.max(1, Math.round(content.layout()
                .resolve(content.raster(), Math.max(1, font.FONT_HEIGHT)).height()));
    }

    @Override
    public void draw(float x, float y, int argb, boolean shadow) {
        float size = Math.max(1, MinecraftFontHolder.height());
        InlineRasterTextRenderResult result = new InlineRasterTextRenderResult(
                content, size, argb, false);
        if (shadow) {
            int normalized = (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb;
            int shadowColor = normalized & 0xFF000000 | (normalized & 0xFCFCFC) >> 2;
            new InlineRasterTextRenderResult(content, size, shadowColor, true)
                    .draw(x + 1, y + 1, 1.0F);
        }
        result.draw(x, y, 1.0F);
    }

    @Override public String description() { return content.description(); }
    @Override public int previewWidth() {
        return content.raster() == null ? -1 : content.raster().width();
    }
    @Override public int previewHeight() {
        return content.raster() == null ? -1 : content.raster().height();
    }

    @Override
    public void drawPreview(float x, float y, int width, int height, int argb) {
        if (content.raster() == null || width <= 0 || height <= 0) return;
        InlineLayout previewLayout = new InlineLayout(1.0F, width / (float) height,
                Float.NaN, InlineLayout.Alignment.TOP);
        neofontrender.text.InlineContent preview = new neofontrender.text.InlineContent(
                content.kind(), content.key(), content.description(), content.tint(),
                content.raster(), content.attributes(), previewLayout);
        new InlineRasterTextRenderResult(preview, height, argb, false).draw(x, y, 1.0F);
    }

    @Override
    public boolean copyImageToClipboard() {
        InlineRaster raster = content.raster();
        if (raster == null) return false;
        BufferedImage image = new BufferedImage(raster.width(), raster.height(),
                BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, raster.width(), raster.height(), raster.argb(), 0, raster.width());
        return InlineImageClipboard.copy(image);
    }

    private static final class MinecraftFontHolder {
        static int height() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
            return minecraft == null || minecraft.fontRenderer == null
                    ? 9 : minecraft.fontRenderer.FONT_HEIGHT;
        }
    }
}
