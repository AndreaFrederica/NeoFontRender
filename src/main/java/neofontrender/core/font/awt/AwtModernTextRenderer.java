package neofontrender.core.font.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.awt.providers.MissingGlyphProvider;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontRenderTuning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native logical-size adapter for the SFR/AWT renderer.
 *
 * <p>Each distinct logical-size/raster-scale pair owns an atlas created from fonts at that real
 * size. This is intentionally separate from model-view scaling so callers of the public modern
 * text API receive crisp glyphs even while the globally selected engine is SFR or vanilla.</p>
 */
public final class AwtModernTextRenderer implements TextRenderBackend {
    private static final int MAX_SIZE_ATLASES = 16;
    private static final int MAX_LAYOUTS = 2048;
    private static final int[] COLOR_CODES = createColorCodes();

    private final TextureManager textureManager;
    private final IResourceManager resourceManager;
    private final LinkedHashMap<SizeKey, FontSet> fontSets =
            new LinkedHashMap<>(8, 0.75F, true);
    private final LinkedHashMap<LayoutKey, TextRenderResult> layouts =
            new LinkedHashMap<>(128, 0.75F, true);
    private int nextAtlasId;

    public AwtModernTextRenderer(TextureManager textureManager, IResourceManager resourceManager) {
        this.textureManager = textureManager;
        this.resourceManager = resourceManager;
    }

    @Override
    public boolean isReady() {
        return textureManager != null && resourceManager != null;
    }

    @Override
    public boolean supportsNativeFontSize() {
        return true;
    }

    @Override
    public float measure(String text, boolean bold, boolean italic) {
        return measurePlainAtSize(text, bold, italic, NeofontrenderConfig.fontSize());
    }

    @Override
    public float measureFormatted(String text, int baseArgb, boolean shadow) {
        return measureFormattedAtSize(text, baseArgb, shadow, NeofontrenderConfig.fontSize());
    }

    @Override
    public float measureFormattedAtSize(String text, int baseArgb, boolean shadow, float fontSize) {
        if (text == null || text.isEmpty()) return 0.0F;
        FontSet set = fontSet(fontSize);
        float advance = 0.0F;
        float sizeRatio = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        for (FormattedRun run : parseFormatted(text, baseArgb, shadow)) {
            float[] positions = layoutPositions(set, run.text, run.bold, sizeRatio);
            advance += positions[positions.length - 1];
        }
        return advance;
    }

    @Override
    public TextRenderResult render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) return TextRenderResult.EMPTY;
        float size = NeofontrenderConfig.fontSize();
        FontSet set = fontSet(size);
        return build(set, List.of(new FormattedRun(text, normalizeAlpha(argb), bold, italic,
                false, false)), size);
    }

    @Override
    public TextRenderResult renderFormatted(String text, int baseArgb, boolean shadow) {
        return renderFormattedAtSize(text, baseArgb, shadow, NeofontrenderConfig.fontSize());
    }

    @Override
    public synchronized TextRenderResult renderFormattedAtSize(
            String text, int baseArgb, boolean shadow, float requestedFontSize) {
        if (text == null || text.isEmpty()) return TextRenderResult.EMPTY;
        float fontSize = Math.max(1.0F, requestedFontSize);
        float rasterScale = currentRasterScale();
        LayoutKey key = new LayoutKey(text, baseArgb, shadow, fontSize, rasterScale);
        TextRenderResult cached = layouts.get(key);
        if (cached != null) return cached;
        TextRenderResult rendered = build(fontSet(fontSize, rasterScale),
                parseFormatted(text, baseArgb, shadow), fontSize);
        layouts.put(key, rendered);
        while (layouts.size() > MAX_LAYOUTS) {
            Iterator<LayoutKey> iterator = layouts.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        return rendered;
    }

    private float measurePlainAtSize(String text, boolean bold, boolean italic, float fontSize) {
        if (text == null || text.isEmpty()) return 0.0F;
        float sizeRatio = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        float[] positions = layoutPositions(fontSet(fontSize), text, bold, sizeRatio);
        return positions[positions.length - 1];
    }

    private FontSet fontSet(float fontSize) {
        return fontSet(fontSize, currentRasterScale());
    }

    private synchronized FontSet fontSet(float requestedFontSize, float rasterScale) {
        float fontSize = Math.max(1.0F, requestedFontSize);
        SizeKey key = new SizeKey(fontSize, rasterScale);
        FontSet cached = fontSets.get(key);
        if (cached != null) return cached;

        List<GlyphProvider> providers = new ArrayList<>();
        float ratio = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        for (String selector : NeofontrenderConfig.fontFamily()) {
            try {
                AwtTtfGlyphProvider provider = AwtTtfGlyphProvider.load(
                        resourceManager, selector, fontSize, rasterScale, 0.0F, 0.0F,
                        NeofontrenderConfig.fontBaselineShift() * ratio,
                        NeofontrenderConfig.fontAutoBaseline(),
                        NeofontrenderConfig.fontReferenceBaseline() * ratio,
                        NeofontrenderConfig.fontAntialias(),
                        NeofontrenderConfig.fontAntialiasMode(),
                        NeofontrenderConfig.fontFractionalMetrics(),
                        NeofontrenderConfig.fontStyle(),
                        NeofontrenderConfig.fontVariableWeight(), false);
                if (provider != null) providers.add(provider);
            } catch (Exception error) {
                NeoFontRender.LOGGER.warn(
                        "Modern AWT API skipped unavailable font '{}' at {}px",
                        selector, fontSize);
            }
        }
        if (providers.isEmpty()) {
            try {
                AwtTtfGlyphProvider fallback = AwtTtfGlyphProvider.load(
                        resourceManager, null, fontSize, rasterScale, 0.0F, 0.0F,
                        NeofontrenderConfig.fontBaselineShift() * ratio,
                        NeofontrenderConfig.fontAutoBaseline(),
                        NeofontrenderConfig.fontReferenceBaseline() * ratio,
                        NeofontrenderConfig.fontAntialias(),
                        NeofontrenderConfig.fontAntialiasMode(),
                        NeofontrenderConfig.fontFractionalMetrics(),
                        NeofontrenderConfig.fontStyle(),
                        NeofontrenderConfig.fontVariableWeight(), true);
                if (fallback != null) providers.add(fallback);
            } catch (Exception error) {
                throw new IllegalStateException("Unable to create modern AWT font atlas", error);
            }
        }
        providers.add(new MissingGlyphProvider());
        ResourceLocation location = new ResourceLocation("neofontrender",
                "modern_awt/" + nextAtlasId++);
        FontTexture atlas = new FontTexture(textureManager, location,
                rasterScale * FontRenderTuning.textureScale(rasterScale));
        FontSet created = new FontSet(providers, atlas);
        fontSets.put(key, created);
        if (fontSets.size() > MAX_SIZE_ATLASES) {
            Iterator<Map.Entry<SizeKey, FontSet>> iterator = fontSets.entrySet().iterator();
            Map.Entry<SizeKey, FontSet> eldest = iterator.next();
            iterator.remove();
            layouts.clear();
            eldest.getValue().close();
        }
        return created;
    }

    private static TextRenderResult build(FontSet set, List<FormattedRun> runs, float fontSize) {
        List<GlyphDraw> glyphs = new ArrayList<>();
        List<EffectDraw> effects = new ArrayList<>();
        float x = 0.0F;
        float visualLeft = 0.0F;
        float visualRight = 0.0F;
        float visualTop = 0.0F;
        float visualBottom = fontSize;
        float sizeRatio = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        for (FormattedRun run : runs) {
            float runStart = x;
            float[] positions = layoutPositions(set, run.text, run.bold, sizeRatio);
            for (int index = 0; index < run.text.length(); ) {
                int codePoint = run.text.codePointAt(index);
                int next = index + Character.charCount(codePoint);
                if (codePoint != ' ' && codePoint != 160) {
                    BakedGlyph glyph = set.getGlyph(codePoint);
                    if (glyph != null) {
                        float glyphX = runStart + positions[index];
                        glyphs.add(new GlyphDraw(glyph, glyphX, run.argb, run.bold, run.italic,
                                sizeRatio));
                        visualLeft = Math.min(visualLeft, glyphX + glyph.visualLeft());
                        visualRight = Math.max(visualRight, glyphX + glyph.visualRight()
                                + (run.bold ? sizeRatio : 0.0F));
                        visualTop = Math.min(visualTop, glyph.visualTop());
                        visualBottom = Math.max(visualBottom, glyph.visualBottom());
                    }
                }
                index = next;
            }
            float runWidth = positions[positions.length - 1];
            x += runWidth;
            if (run.strikethrough) {
                float y = fontSize * 0.5F;
                effects.add(new EffectDraw(runStart, y, x, y + Math.max(1.0F, sizeRatio),
                        run.argb));
            }
            if (run.underline) {
                float y = fontSize;
                effects.add(new EffectDraw(runStart, y, x, y + Math.max(1.0F, sizeRatio),
                        run.argb));
            }
        }
        visualRight = Math.max(visualRight, x);
        return new AwtRenderedText(glyphs, effects, x, visualLeft, visualRight,
                visualTop, visualBottom);
    }

    /**
     * FontSet's legacy bold advance is fixed at one GUI unit. Native-size API layouts scale that
     * synthetic bold stroke with the requested font size, so its advance must scale as well.
     */
    private static float[] layoutPositions(
            FontSet set, String text, boolean bold, float boldOffset) {
        float[] positions = set.layoutPositions(text, false);
        if (!bold || text.isEmpty()) return positions;
        float accumulated = 0.0F;
        for (int index = 0; index < text.length(); ) {
            int next = index + Character.charCount(text.codePointAt(index));
            accumulated += boldOffset;
            for (int boundary = index + 1; boundary <= next; boundary++) {
                positions[boundary] += accumulated;
            }
            index = next;
        }
        return positions;
    }

    private static List<FormattedRun> parseFormatted(String text, int baseArgb, boolean shadow) {
        List<FormattedRun> runs = new ArrayList<>();
        int color = shadow ? shadowColor(normalizeAlpha(baseArgb)) : normalizeAlpha(baseArgb);
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\u00a7' || i + 1 >= text.length()) continue;
            if (i > start) {
                runs.add(new FormattedRun(text.substring(start, i), color, bold, italic,
                        underline, strikethrough));
            }
            int style = "0123456789abcdefklmnor".indexOf(
                    Character.toLowerCase(text.charAt(++i)));
            if (style >= 0 && style < 16) {
                color = (baseArgb & 0xFF000000)
                        | COLOR_CODES[style + (shadow ? 16 : 0)];
                bold = italic = underline = strikethrough = false;
            } else if (style == 17) {
                bold = true;
            } else if (style == 18) {
                strikethrough = true;
            } else if (style == 19) {
                underline = true;
            } else if (style == 20) {
                italic = true;
            } else if (style == 21) {
                color = shadow ? shadowColor(normalizeAlpha(baseArgb)) : normalizeAlpha(baseArgb);
                bold = italic = underline = strikethrough = false;
            }
            start = i + 1;
        }
        if (start < text.length()) {
            runs.add(new FormattedRun(text.substring(start), color, bold, italic,
                    underline, strikethrough));
        }
        return runs;
    }

    private static int normalizeAlpha(int argb) {
        return (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb;
    }

    private static int shadowColor(int argb) {
        return (argb & 0xFCFCFC) >> 2 | argb & 0xFF000000;
    }

    private static float currentRasterScale() {
        return Math.max(1.0F,
                FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
    }

    private static int[] createColorCodes() {
        int[] codes = new int[32];
        for (int i = 0; i < 32; i++) {
            int j = (i >> 3 & 1) * 85;
            int r = (i >> 2 & 1) * 170 + j;
            int g = (i >> 1 & 1) * 170 + j;
            int b = (i & 1) * 170 + j;
            if (i == 6) r += 85;
            if (i >= 16) {
                r /= 4;
                g /= 4;
                b /= 4;
            }
            codes[i] = r << 16 | g << 8 | b;
        }
        return codes;
    }

    @Override
    public synchronized void close() {
        layouts.clear();
        for (FontSet set : fontSets.values()) set.close();
        fontSets.clear();
    }

    private static final class AwtRenderedText implements TextRenderResult {
        private final List<GlyphDraw> glyphs;
        private final List<EffectDraw> effects;
        private final float advance;
        private final float visualLeft;
        private final float visualRight;
        private final float visualTop;
        private final float visualBottom;

        private AwtRenderedText(List<GlyphDraw> glyphs, List<EffectDraw> effects, float advance,
                                float visualLeft, float visualRight,
                                float visualTop, float visualBottom) {
            this.glyphs = glyphs;
            this.effects = effects;
            this.advance = advance;
            this.visualLeft = visualLeft;
            this.visualRight = visualRight;
            this.visualTop = visualTop;
            this.visualBottom = visualBottom;
        }

        @Override public float advance() { return advance; }
        @Override public float visualLeft() { return visualLeft; }
        @Override public float visualRight() { return visualRight; }
        @Override public float visualTop() { return visualTop; }
        @Override public float visualBottom() { return visualBottom; }

        @Override
        public void draw(float x, float y, float alpha) {
            Minecraft mc = Minecraft.getMinecraft();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            for (GlyphDraw draw : glyphs) {
                mc.getTextureManager().bindTexture(draw.glyph.getTextureLocation());
                float red = (draw.argb >> 16 & 255) / 255.0F;
                float green = (draw.argb >> 8 & 255) / 255.0F;
                float blue = (draw.argb & 255) / 255.0F;
                draw.glyph.render(draw.italic, x + draw.x, y, red, green, blue, alpha);
                if (draw.bold) {
                    draw.glyph.render(draw.italic, x + draw.x + draw.boldOffset, y,
                            red, green, blue, alpha);
                }
            }
            for (EffectDraw effect : effects) {
                drawSolidQuad(x + effect.left, y + effect.top, x + effect.right,
                        y + effect.bottom, effect.argb, alpha);
            }
        }
    }

    private static void drawSolidQuad(float left, float top, float right, float bottom,
                                      int argb, float alpha) {
        net.minecraft.client.renderer.BufferBuilder buffer =
                net.minecraft.client.renderer.Tessellator.getInstance().getBuffer();
        GlStateManager.disableTexture2D();
        buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        float red = (argb >> 16 & 255) / 255.0F;
        float green = (argb >> 8 & 255) / 255.0F;
        float blue = (argb & 255) / 255.0F;
        buffer.pos(right, top, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(left, top, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(left, bottom, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(right, bottom, 0).color(red, green, blue, alpha).endVertex();
        net.minecraft.client.renderer.Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static final class GlyphDraw {
        private final BakedGlyph glyph;
        private final float x;
        private final int argb;
        private final boolean bold;
        private final boolean italic;
        private final float boldOffset;

        private GlyphDraw(BakedGlyph glyph, float x, int argb, boolean bold, boolean italic,
                          float boldOffset) {
            this.glyph = glyph;
            this.x = x;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
            this.boldOffset = boldOffset;
        }
    }

    private static final class EffectDraw {
        private final float left;
        private final float top;
        private final float right;
        private final float bottom;
        private final int argb;

        private EffectDraw(float left, float top, float right, float bottom, int argb) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.argb = argb;
        }
    }

    private static final class FormattedRun {
        private final String text;
        private final int argb;
        private final boolean bold;
        private final boolean italic;
        private final boolean underline;
        private final boolean strikethrough;

        private FormattedRun(String text, int argb, boolean bold, boolean italic,
                             boolean underline, boolean strikethrough) {
            this.text = text;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
        }
    }

    private static final class SizeKey {
        private final int size;
        private final int rasterScale;

        private SizeKey(float size, float rasterScale) {
            this.size = Float.floatToIntBits(size);
            this.rasterScale = Float.floatToIntBits(rasterScale);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof SizeKey && size == ((SizeKey) object).size
                    && rasterScale == ((SizeKey) object).rasterScale;
        }

        @Override
        public int hashCode() {
            return 31 * size + rasterScale;
        }
    }

    private static final class LayoutKey {
        private final String text;
        private final int argb;
        private final boolean shadow;
        private final SizeKey size;

        private LayoutKey(String text, int argb, boolean shadow,
                          float fontSize, float rasterScale) {
            this.text = text;
            this.argb = argb;
            this.shadow = shadow;
            this.size = new SizeKey(fontSize, rasterScale);
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof LayoutKey)) return false;
            LayoutKey other = (LayoutKey) object;
            return argb == other.argb && shadow == other.shadow
                    && text.equals(other.text) && size.equals(other.size);
        }

        @Override
        public int hashCode() {
            int hash = 31 * text.hashCode() + argb;
            hash = 31 * hash + (shadow ? 1 : 0);
            return 31 * hash + size.hashCode();
        }
    }
}
