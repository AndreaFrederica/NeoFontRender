package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineImageHandle;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.ShadowColorPolicy;

final class RemoteImageGlyph implements InlineGlyph {
    private static final int MAX_INLINE_IMAGE_WIDTH = 128;
    private static final int MAX_INLINE_IMAGE_HEIGHT = 48;
    private static final int INLINE_PADDING = 2;
    private final InlineImageService.Handle handle;
    private final String description;
    private final boolean compactEmoji;

    RemoteImageGlyph(InlineImageService.Handle handle, String description) {
        this(handle, description, true);
    }

    RemoteImageGlyph(InlineImageService.Handle handle, String description, boolean compactEmoji) {
        this.handle = handle;
        this.description = description;
        this.compactEmoji = compactEmoji;
    }

    @Override public int advance(FontRenderer font) {
        return displaySize(font)[0] + INLINE_PADDING * 2 + 1;
    }

    @Override public int height(FontRenderer font) {
        return displaySize(font)[1] + INLINE_PADDING * 2;
    }

    @Override
    public void draw(float x, float y, int argb, boolean shadow) {
        int[] display = displaySize(Minecraft.getMinecraft().fontRenderer);
        x += INLINE_PADDING;
        y += INLINE_PADDING;
        if (handle.state() != InlineImageHandle.State.READY || handle.texture() == null) {
            Minecraft.getMinecraft().fontRenderer.drawString("□", x, y, argb, false);
            return;
        }
        float alpha = ((argb >>> 24) & 0xff) / 255.0F;
        if (shadow) {
            int shadowArgb = ShadowColorPolicy.shadowColor(argb,
                    NeofontrenderConfig.shadowColorMode(), NeofontrenderConfig.shadowColor(),
                    NeofontrenderConfig.shadowColorOverrides(), null);
            float shadowAlpha = alpha
                    * (ShadowColorPolicy.COLORED.equals(ShadowColorPolicy.normalizeMode(
                            NeofontrenderConfig.shadowColorMode())) ? 0.65F : 0.72F);
            drawTexture(x + 1, y + 1, display[0], display[1],
                    ((shadowArgb >>> 16) & 0xff) / 255.0F,
                    ((shadowArgb >>> 8) & 0xff) / 255.0F,
                    (shadowArgb & 0xff) / 255.0F, shadowAlpha);
        }
        drawTexture(x, y, display[0], display[1], 1.0F, 1.0F, 1.0F, alpha);
    }

    @Override public String description() { return description; }

    @Override public int previewWidth() { return handle.pixelWidth(); }

    @Override public int previewHeight() { return handle.pixelHeight(); }

    @Override
    public void drawPreview(float x, float y, int size, int argb) {
        drawPreview(x, y, size, size, argb);
    }

    @Override
    public void drawPreview(float x, float y, int width, int height, int argb) {
        if (handle.state() == InlineImageHandle.State.READY && handle.texture() != null) {
            drawTexture(x, y, width, height, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            Minecraft.getMinecraft().fontRenderer.drawString("□", x + width / 2.0F - 3,
                    y + height / 2.0F - 4, argb, false);
        }
    }

    @Override
    public boolean copyImageToClipboard() {
        return InlineImageClipboard.copy(handle.image());
    }

    private int[] displaySize(FontRenderer font) {
        int fontSize = font.FONT_HEIGHT + 1;
        if (compactEmoji || handle.pixelWidth() <= 0 || handle.pixelHeight() <= 0) {
            return new int[] { fontSize, fontSize };
        }
        return fit(handle.pixelWidth(), handle.pixelHeight(), MAX_INLINE_IMAGE_WIDTH,
                MAX_INLINE_IMAGE_HEIGHT, fontSize);
    }

    static int[] fit(int pixelWidth, int pixelHeight, int maxWidth, int maxHeight, int minimumHeight) {
        if (pixelWidth <= 0 || pixelHeight <= 0) return new int[] { minimumHeight, minimumHeight };
        float scale = Math.min(1.0F, Math.min(maxWidth / (float) pixelWidth,
                maxHeight / (float) pixelHeight));
        if (pixelHeight * scale < minimumHeight) scale = minimumHeight / (float) pixelHeight;
        if (pixelWidth * scale > maxWidth) scale = maxWidth / (float) pixelWidth;
        return new int[] { Math.max(1, Math.round(pixelWidth * scale)),
                Math.max(1, Math.round(pixelHeight * scale)) };
    }

    private void drawTexture(float x, float y, int boxWidth, int boxHeight,
                             float red, float green, float blue,
                             float alpha) {
        float drawWidth = boxWidth;
        float drawHeight = boxHeight;
        if (handle.pixelWidth() > 0 && handle.pixelHeight() > 0) {
            float aspect = handle.pixelWidth() / (float) handle.pixelHeight();
            float boxAspect = boxWidth / (float) boxHeight;
            if (aspect > boxAspect) {
                drawHeight = boxWidth / aspect;
                y += (boxHeight - drawHeight) * 0.5F;
            } else if (aspect < boxAspect) {
                drawWidth = boxHeight * aspect;
                x += (boxWidth - drawWidth) * 0.5F;
            }
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(handle.texture());
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, Math.max(0.0F, Math.min(1.0F, alpha)));
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + drawHeight, 0).tex(0.0F, 1.0F).endVertex();
        buffer.pos(x + drawWidth, y + drawHeight, 0).tex(1.0F, 1.0F).endVertex();
        buffer.pos(x + drawWidth, y, 0).tex(1.0F, 0.0F).endVertex();
        buffer.pos(x, y, 0).tex(0.0F, 0.0F).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.color(1, 1, 1, 1);
    }
}
