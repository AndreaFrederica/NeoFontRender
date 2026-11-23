package neofontrender.core.font.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.FramebufferAlphaBlend;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small glyph-only fallback used while Cosmic's asynchronous result is pending.
 *
 * <p>It deliberately walks code points one at a time.  There is no GlyphVector shaping or
 * modern line renderer in this path, so a pending label can draw immediately while each glyph
 * is baked into the configured font atlas on first use.</p>
 */
public final class AwtAsyncFallbackGlyphRenderer implements AutoCloseable {
    private static final Tessellator TESSELLATOR = new Tessellator(0x200000);
    private final FontSet fontSet;
    private final float baseFontSize;

    public AwtAsyncFallbackGlyphRenderer(FontSet fontSet, float baseFontSize) {
        this.fontSet = fontSet;
        this.baseFontSize = Math.max(1.0F, baseFontSize);
    }

    public float measure(String text, boolean bold) {
        if (text == null || text.isEmpty()) return 0.0F;
        float advance = 0.0F;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            if (codePoint != '\n' && codePoint != '\r') {
                GlyphInfo info = fontSet.getGlyphInfo(codePoint);
                if (info != null) advance += info.getAdvance(bold);
            }
            index = next;
        }
        return advance;
    }

    /** Draws a plain code-point run at the requested logical size and target advance. */
    public synchronized void draw(String text, float x, float y, float requestedSize,
                                  float targetAdvance, int argb, float alpha,
                                  boolean bold, boolean italic,
                                  boolean underline, boolean strikethrough) {
        if (text == null || text.isEmpty() || alpha <= 0.0F) return;
        float sourceAdvance = measure(text, bold);
        if (!(sourceAdvance > 0.0F) || !(targetAdvance > 0.0F)) return;
        float horizontalScale = targetAdvance / sourceAdvance;
        float verticalScale = Math.max(1.0F, requestedSize) / baseFontSize;
        List<PlacedGlyph> glyphs = new ArrayList<>();
        float cursor = 0.0F;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            GlyphInfo info = fontSet.getGlyphInfo(codePoint);
            if (info != null) {
                BakedGlyph glyph = fontSet.getGlyph(codePoint);
                if (glyph != null && codePoint != '\n' && codePoint != '\r'
                        && !Character.isISOControl(codePoint)) {
                    // Resolve the lazy proxy before grouping by atlas page.
                    ResourceLocation location = glyph.getTextureLocation();
                    glyphs.add(new PlacedGlyph(location, glyph, cursor));
                }
                cursor += info.getAdvance(bold);
            }
            index = next;
        }
        if (glyphs.isEmpty() && !underline && !strikethrough) return;

        float opacity = clamp(alpha);
        if (opacity <= 0.0F) return;
        int rgb = argb & 0xFFFFFF;
        float red = ((rgb >>> 16) & 255) / 255.0F;
        float green = ((rgb >>> 8) & 255) / 255.0F;
        float blue = (rgb & 255) / 255.0F;
        fontSet.flushAtlas();
        GlStateManager.enableTexture2D();
        try (StraightAlphaBlendState ignored = new StraightAlphaBlendState()) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x, y, 0.0F);
                GlStateManager.scale(horizontalScale, verticalScale, 1.0F);
                Map<ResourceLocation, List<PlacedGlyph>> pages = new LinkedHashMap<>();
                for (PlacedGlyph glyph : glyphs) {
                    pages.computeIfAbsent(glyph.location, key -> new ArrayList<>()).add(glyph);
                }
                for (Map.Entry<ResourceLocation, List<PlacedGlyph>> page : pages.entrySet()) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(page.getKey());
                    FontRenderTuning.applyBoundTextureFilter(page.getValue().get(0).glyph.getRasterScale());
                    Tessellator tessellator = TESSELLATOR;
                    BufferBuilder buffer = tessellator.getBuffer();
                    buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                    for (PlacedGlyph placed : page.getValue()) {
                        placed.glyph.writeVertices(buffer, italic, placed.x, 0.0F,
                                red, green, blue, opacity);
                        if (bold) {
                            placed.glyph.writeVertices(buffer, italic, placed.x + 1.0F, 0.0F,
                                    red, green, blue, opacity);
                        }
                    }
                    tessellator.draw();
                }
                if (underline) drawEffect(0.0F, baseFontSize, targetAdvance / horizontalScale,
                        baseFontSize + Math.max(1.0F, 1.0F / verticalScale), red, green, blue, opacity);
                if (strikethrough) drawEffect(0.0F, baseFontSize * 0.5F,
                        targetAdvance / horizontalScale, baseFontSize * 0.5F + Math.max(1.0F, 1.0F / verticalScale),
                        red, green, blue, opacity);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }

    private static void drawEffect(float left, float top, float right, float bottom,
                                   float red, float green, float blue, float alpha) {
        GlStateManager.disableTexture2D();
        Tessellator tessellator = TESSELLATOR;
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(left, top, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(left, bottom, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, bottom, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, top, 0).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    @Override
    public synchronized void close() {
        fontSet.close();
    }

    private static final class PlacedGlyph {
        private final ResourceLocation location;
        private final BakedGlyph glyph;
        private final float x;

        private PlacedGlyph(ResourceLocation location, BakedGlyph glyph, float x) {
            this.location = location;
            this.glyph = glyph;
            this.x = x;
        }
    }

    private static final class StraightAlphaBlendState implements AutoCloseable {
        private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        private StraightAlphaBlendState() {
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    FramebufferAlphaBlend.SOURCE_FACTOR, FramebufferAlphaBlend.DESTINATION_FACTOR);
        }

        @Override
        public void close() {
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (!blendEnabled) GlStateManager.disableBlend();
            if (alphaTestEnabled) GlStateManager.enableAlpha();
            else GlStateManager.disableAlpha();
        }
    }
}
