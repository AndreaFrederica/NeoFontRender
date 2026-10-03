package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.ShadowColorPolicy;

/** Draws a cached formula or SVG raster as one atomic inline run. */
final class RasterInlineContent implements InlineContent {
    private static final int HORIZONTAL_PADDING = 1;
    private static final int VERTICAL_PADDING = 1;
    private final RasterGlyphService.Handle handle;
    private final String description;
    private final int displayHeight;
    private final boolean tint;
    private final boolean matchFontLineHeight;
    private volatile FontRenderer measuredWith;

    RasterInlineContent(RasterGlyphService.Handle handle, String description, int displayHeight,
                      boolean tint, boolean matchFontLineHeight) {
        this.handle = handle;
        this.description = description;
        this.displayHeight = Math.max(1, displayHeight);
        this.tint = tint;
        this.matchFontLineHeight = matchFontLineHeight;
    }

    @Override public int advance(FontRenderer font) {
        measuredWith = font;
        return displaySize(font)[0] + HORIZONTAL_PADDING * 2;
    }
    @Override public int height(FontRenderer font) {
        measuredWith = font;
        return paddedHeight(displaySize(font)[1], matchFontLineHeight);
    }

    @Override
    public void draw(float x, float y, int argb, boolean shadow) {
        int[] size = displaySize(measuredWith);
        x += HORIZONTAL_PADDING;
        y += verticalPadding(matchFontLineHeight);
        if (handle.state != RasterGlyphService.State.READY || handle.location == null) {
            Minecraft.getMinecraft().fontRenderer.drawString("\u25a1", x, y, argb, false);
            return;
        }
        float alpha = ((argb >>> 24) & 0xff) / 255.0F;
        if (shadow) {
            int shadowArgb = ShadowColorPolicy.shadowColor(argb,
                    NeofontrenderConfig.shadowColorMode(), NeofontrenderConfig.shadowColor(),
                    NeofontrenderConfig.shadowColorOverrides(), null);
            drawTexture(x + 1, y + 1, size[0], size[1], shadowArgb,
                    alpha * 0.72F);
        }
        drawTexture(x, y, size[0], size[1], tint ? argb : 0xffffffff, alpha);
    }

    @Override public String description() { return description; }
    @Override public int previewWidth() { return handle.width; }
    @Override public int previewHeight() { return handle.height; }

    @Override
    public void drawPreview(float x, float y, int width, int height, int argb) {
        if (handle.state == RasterGlyphService.State.READY && handle.location != null) {
            drawTexture(x, y, width, height, 0xffffffff, 1.0F);
        }
    }

    private int[] displaySize() {
        return displaySize(null);
    }

    private int[] displaySize(FontRenderer font) {
        int requestedHeight = matchFontLineHeight && font != null
                ? Math.max(1, font.FONT_HEIGHT) : displayHeight;
        if (handle.width <= 0 || handle.height <= 0) {
            return new int[] { requestedHeight, requestedHeight };
        }
        float scale = requestedHeight / (float) handle.height;
        return new int[] { Math.max(1, Math.round(handle.width * scale)), requestedHeight };
    }

    static int paddedHeight(int contentHeight, boolean matchFontLineHeight) {
        return Math.max(1, contentHeight) + verticalPadding(matchFontLineHeight) * 2;
    }

    private static int verticalPadding(boolean matchFontLineHeight) {
        // A line-height-matched formula must fit the same baseline slot as ordinary text.
        return matchFontLineHeight ? 0 : VERTICAL_PADDING;
    }

    private void drawTexture(float x, float y, int width, int height, int color, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(handle.location);
        float red = ((color >>> 16) & 0xff) / 255.0F;
        float green = ((color >>> 8) & 0xff) / 255.0F;
        float blue = (color & 0xff) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + height, 0).tex(0, 1).endVertex();
        buffer.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        buffer.pos(x + width, y, 0).tex(1, 0).endVertex();
        buffer.pos(x, y, 0).tex(0, 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.color(1, 1, 1, 1);
    }
}
