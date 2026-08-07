package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineImageHandle;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

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
            Minecraft.getMinecraft().fontRenderer.drawString("□",
                    Math.round(x), Math.round(y), argb, false);
            return;
        }
        float alpha = ((argb >>> 24) & 0xff) / 255.0F;
        if (shadow) {
            if (NeofontrenderConfig.modernShadowEnabled()) {
                drawTexture(x + 1, y + 1, display[0], display[1], 1.0F, 1.0F, 1.0F,
                        alpha * 0.65F);
            } else {
                drawTexture(x + 1, y + 1, display[0], display[1], 0.12F, 0.12F, 0.12F,
                        alpha * 0.72F);
            }
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
            Minecraft.getMinecraft().fontRenderer.drawString("□",
                    Math.round(x + width / 2.0F - 3),
                    Math.round(y + height / 2.0F - 4), argb, false);
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
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(red, green, blue,
                Math.max(0.0F, Math.min(1.0F, alpha)));
        tessellator.addVertexWithUV(x, y + drawHeight, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x + drawWidth, y + drawHeight, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x + drawWidth, y, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x, y, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
