package neofontrender.core.font.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.awt.providers.MissingGlyphProvider;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.FramebufferAlphaBlend;
import neofontrender.core.font.support.ShadowColorPolicy;
import neofontrender.core.font.support.ShadowRenderSpec;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import neofontrender.text.StructuredText;
import neofontrender.text.InlineSpan;
import neofontrender.core.font.inline.InlineRasterTextRenderResult;
import neofontrender.core.font.backend.CompositeTextRenderResult;
import neofontrender.text.StyledSpan;
import neofontrender.text.TextStyle;
import neofontrender.text.animation.TextAnimationFrame;
import neofontrender.text.animation.TextAnimationEngine;
import neofontrender.text.animation.TextAnimationPlan;
import neofontrender.text.animation.TextAnimationRenderMode;
import neofontrender.text.animation.TextAnimationSampler;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final TextureManager textureManager;
    private final IResourceManager resourceManager;
    private final List<String> selectors;
    private final LinkedHashMap<SizeKey, FontSet> fontSets =
            new LinkedHashMap<>(8, 0.75F, true);
    private int nextAtlasId;
    private volatile int[] legacyColorCodes = TextColorPaletteRegistry.vanillaColorCodes();

    public AwtModernTextRenderer(TextureManager textureManager, IResourceManager resourceManager) {
        this(textureManager, resourceManager, NeofontrenderConfig.fontFamily());
    }

    public AwtModernTextRenderer(TextureManager textureManager, IResourceManager resourceManager,
                                 List<String> selectors) {
        this.textureManager = textureManager;
        this.resourceManager = resourceManager;
        this.selectors = selectors == null || selectors.isEmpty()
                ? NeofontrenderConfig.fontFamily() : new ArrayList<>(selectors);
    }

    @Override
    public boolean isReady() {
        return textureManager != null && resourceManager != null;
    }

    @Override
    public synchronized void updateLegacyColorCodes(int[] colorCodes) {
        int[] normalized = TextColorPaletteRegistry.normalizeColorCodes(colorCodes);
        if (!Arrays.equals(legacyColorCodes, normalized)) {
            legacyColorCodes = normalized;
        }
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
    public TextRenderResult render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) return TextRenderResult.EMPTY;
        float size = NeofontrenderConfig.fontSize();
        FontSet set = fontSet(size);
        return build(set, List.of(new FormattedRun(text, normalizeAlpha(argb), bold, italic,
                false, false, false)), size);
    }

    @Override
    public float measureStructuredAtSize(StructuredText text, int baseArgb, boolean shadow,
                                         float fontSize) {
        if (text == null || text.plainText().isEmpty()) return 0.0F;
        FontSet set = fontSet(fontSize);
        if (!text.inlineSpans().isEmpty()) {
            float advance = 0.0F;
            int cursor = 0;
            for (InlineSpan inline : text.inlineSpans()) {
                if (inline.start() > cursor) {
                    advance += measureStructuredAtSize(text.slice(cursor, inline.start()),
                            baseArgb, shadow, fontSize);
                }
                advance += new InlineRasterTextRenderResult(inline.content(), fontSize,
                        inlineColor(text, inline.start(), baseArgb, shadow,
                                ShadowRenderSpec.fromConfig()), shadow).advance();
                cursor = inline.end();
            }
            if (cursor < text.plainText().length()) {
                advance += measureStructuredAtSize(text.slice(cursor, text.plainText().length()),
                        baseArgb, shadow, fontSize);
            }
            return advance;
        }
        float advance = 0.0F;
        float sizeRatio = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        for (FormattedRun run : structuredRuns(text, baseArgb, shadow)) {
            float[] positions = layoutPositions(set, run.text, run.bold, sizeRatio);
            advance += positions[positions.length - 1];
        }
        return advance;
    }

    @Override
    public synchronized TextRenderResult renderStructuredAtSize(
            StructuredText text, int baseArgb, boolean shadow, float requestedFontSize) {
        if (text == null || text.plainText().isEmpty()) return TextRenderResult.EMPTY;
        float fontSize = Math.max(1.0F, requestedFontSize);
        float rasterScale = currentRasterScale();
        FontSet set = fontSet(fontSize, rasterScale);
        set.flushAtlas();
        if (!text.inlineSpans().isEmpty()) {
            return structuredWithInline(set, text, baseArgb, shadow, fontSize,
                    ShadowRenderSpec.fromConfig());
        }
        float shadowOffset = shadow ? NeofontrenderConfig.shadowLength() : 0.0F;
        return build(set, structuredRuns(text, baseArgb, shadow), fontSize, text, shadow,
                shadowOffset, shadowOffset);
    }

    @Override
    public synchronized TextRenderResult renderStructuredShadowSourceAtSize(
            StructuredText text, int baseArgb, float requestedFontSize, ShadowRenderSpec spec) {
        if (text == null || text.plainText().isEmpty()) return TextRenderResult.EMPTY;
        float fontSize = Math.max(1.0F, requestedFontSize);
        float rasterScale = currentRasterScale();
        FontSet set = fontSet(fontSize, rasterScale);
        set.flushAtlas();
        if (!text.inlineSpans().isEmpty()) {
            return structuredWithInline(set, text, baseArgb, true, fontSize,
                    spec == null ? ShadowRenderSpec.fromConfig() : spec);
        }
        ShadowRenderSpec effectiveSpec = spec == null ? ShadowRenderSpec.fromConfig() : spec;
        return build(set, structuredRuns(text, baseArgb, true, effectiveSpec), fontSize, text, true,
                effectiveSpec.offsetX, effectiveSpec.offsetY);
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
        for (String selector : selectors) {
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
            eldest.getValue().close();
        }
        return created;
    }

    private static TextRenderResult build(FontSet set, List<FormattedRun> runs, float fontSize) {
        return build(set, runs, fontSize, null);
    }

    private static TextRenderResult build(FontSet set, List<FormattedRun> runs, float fontSize,
                                          StructuredText structuredText) {
        return build(set, runs, fontSize, structuredText, false);
    }

    private static TextRenderResult build(FontSet set, List<FormattedRun> runs, float fontSize,
                                          StructuredText structuredText, boolean shadowPass) {
        float shadowOffset = shadowPass ? NeofontrenderConfig.shadowLength() : 0.0F;
        return build(set, runs, fontSize, structuredText, shadowPass,
                shadowOffset, shadowOffset);
    }

    private static TextRenderResult build(FontSet set, List<FormattedRun> runs, float fontSize,
                                          StructuredText structuredText, boolean shadowPass,
                                          float shadowOffsetX, float shadowOffsetY) {
        List<GlyphDraw> glyphs = new ArrayList<>();
        List<EffectDraw> effects = new ArrayList<>();
        float x = 0.0F;
        float visualLeft = 0.0F;
        float visualRight = 0.0F;
        float visualTop = 0.0F;
        float visualBottom = fontSize;
        float sizeRatio = fontSize / Math.max(1.0F, NeofontrenderConfig.fontSize());
        boolean glyphAnimation = structuredText != null && TextAnimationPlan.forText(structuredText,
                TextAnimationRenderMode.AUTO).usesGlyphs();
        TextAnimationEngine.GlyphAnimationFrame animationFrame = !glyphAnimation
                ? TextAnimationEngine.GlyphAnimationFrame.empty(0)
                : new TextAnimationEngine().frame(structuredText, TextAnimationFrame.currentTimeMillis());
        TextAnimationSampler sampler = new TextAnimationSampler();
        int plainIndex = 0;
        for (FormattedRun run : runs) {
            float runStart = x;
            float[] positions = layoutPositions(set, run.text, run.bold, sizeRatio);
            for (int index = 0; index < run.text.length(); ) {
                int codePoint = run.text.codePointAt(index);
                int next = index + Character.charCount(codePoint);
                TextAnimationSampler.Sample animation = animationFrame.glyphs().isEmpty()
                        ? new TextAnimationSampler.Sample()
                        : sampler.sample(animationFrame.glyphs().get(Math.min(plainIndex,
                                animationFrame.glyphs().size() - 1)),
                        animationFrame.timeMillis(), shadowPass, shadowOffsetX, shadowOffsetY,
                        run.argb);
                float characterStart = runStart + positions[index];
                float characterEnd = runStart + positions[next];
                if (glyphAnimation && animation.visible) {
                    int decorationColor = animation.argb;
                    if (run.strikethrough) {
                        float y = fontSize * 0.5F;
                        effects.add(new EffectDraw(characterStart, y, characterEnd,
                                y + Math.max(1.0F, sizeRatio), decorationColor,
                                animation.alpha));
                    }
                    if (run.underline) {
                        float y = fontSize;
                        effects.add(new EffectDraw(characterStart, y, characterEnd,
                                y + Math.max(1.0F, sizeRatio), decorationColor,
                                animation.alpha));
                    }
                }
                if (codePoint != ' ' && codePoint != 160) {
                    BakedGlyph glyph = set.getGlyph(codePoint);
                    if (glyph != null) {
                        float glyphX = runStart + positions[index];
                        if (!animation.visible) {
                            index = next;
                            plainIndex += Character.charCount(codePoint);
                            continue;
                        }
                        GlyphInfo info = set.getGlyphInfo(codePoint);
                        glyphs.add(new GlyphDraw(glyph, glyphX, run.argb, run.bold, run.italic,
                                sizeRatio, run.obfuscated ? set : null,
                                info == null ? 0.0F : info.getAdvance(false), fontSize, animation));
                        for (TextAnimationSampler.Sample layer : animation.layers()) {
                            if (!layer.visible) continue;
                            visualLeft = Math.min(visualLeft,
                                    glyphX + glyph.visualLeft() + (float) layer.x);
                            visualRight = Math.max(visualRight,
                                    glyphX + glyph.visualRight() + (float) layer.x
                                            + (run.bold ? sizeRatio : 0.0F));
                            visualTop = Math.min(visualTop,
                                    glyph.visualTop() + (float) layer.y);
                            visualBottom = Math.max(visualBottom,
                                    glyph.visualBottom() + (float) layer.y);
                        }
                    }
                }
                index = next;
                // TextAnimationEngine indexes the plain string in UTF-16 boundaries so
                // supplementary code points cannot shift the following effect span.
                plainIndex += Character.charCount(codePoint);
            }
            float runWidth = positions[positions.length - 1];
            x += runWidth;
            if (!glyphAnimation && run.strikethrough) {
                float y = fontSize * 0.5F;
                effects.add(new EffectDraw(runStart, y, x, y + Math.max(1.0F, sizeRatio),
                        run.argb));
            }
            if (!glyphAnimation && run.underline) {
                float y = fontSize;
                effects.add(new EffectDraw(runStart, y, x, y + Math.max(1.0F, sizeRatio),
                        run.argb));
            }
        }
        visualRight = Math.max(visualRight, x);
        return new AwtRenderedText(glyphs, effects, x, visualLeft, visualRight,
                visualTop, visualBottom);
    }

    private List<FormattedRun> structuredRuns(StructuredText text, int baseArgb, boolean shadow) {
        return structuredRuns(text, baseArgb, shadow, ShadowRenderSpec.fromConfig());
    }

    private TextRenderResult structuredWithInline(FontSet set, StructuredText text, int baseArgb,
                                                   boolean shadow, float fontSize,
                                                   ShadowRenderSpec spec) {
        List<TextRenderResult> pieces = new ArrayList<>();
        int cursor = 0;
        for (InlineSpan inline : text.inlineSpans()) {
            if (inline.start() > cursor) {
                StructuredText segment = text.slice(cursor, inline.start());
                pieces.add(build(set, structuredRuns(segment, baseArgb, shadow, spec), fontSize,
                        segment, shadow, shadow ? spec.offsetX : 0.0F,
                        shadow ? spec.offsetY : 0.0F));
            }
            pieces.add(new InlineRasterTextRenderResult(inline.content(), fontSize,
                    inlineColor(text, inline.start(), baseArgb, shadow, spec), shadow));
            cursor = inline.end();
        }
        if (cursor < text.plainText().length()) {
            StructuredText segment = text.slice(cursor, text.plainText().length());
            pieces.add(build(set, structuredRuns(segment, baseArgb, shadow, spec), fontSize,
                    segment, shadow, shadow ? spec.offsetX : 0.0F,
                    shadow ? spec.offsetY : 0.0F));
        }
        return CompositeTextRenderResult.of(pieces);
    }

    private int inlineColor(StructuredText text, int index, int baseArgb, boolean shadow,
                            ShadowRenderSpec shadowSpec) {
        TextStyle style = text.styleAt(index);
        int color = style.hasColorOverride()
                ? (normalizeAlpha(baseArgb) & 0xFF000000) | style.rgb() : normalizeAlpha(baseArgb);
        if (!shadow) return color;
        ShadowRenderSpec spec = shadowSpec == null ? ShadowRenderSpec.fromConfig() : shadowSpec;
        return ShadowColorPolicy.modernColor(color, spec.color, spec.colorMode,
                spec.colorOverrides, legacyColorCodes, spec.coloredRatio, spec.coloredFunction);
    }

    private List<FormattedRun> structuredRuns(StructuredText text, int baseArgb, boolean shadow,
                                              ShadowRenderSpec shadowSpec) {
        List<FormattedRun> result = new ArrayList<>();
        int base = normalizeAlpha(baseArgb);
        ShadowRenderSpec spec = shadowSpec == null ? ShadowRenderSpec.fromConfig() : shadowSpec;
        for (StyledSpan span : text.styles()) {
            if (span.end() <= span.start()) continue;
            TextStyle style = span.style();
            int color = style.hasColorOverride()
                    ? (base & 0xFF000000) | style.rgb() : base;
            if (shadow) {
                color = ShadowColorPolicy.modernColor(color, spec.color, spec.colorMode,
                        spec.colorOverrides, legacyColorCodes,
                        spec.coloredRatio, spec.coloredFunction);
            }
            result.add(new FormattedRun(text.plainText().substring(span.start(), span.end()),
                    color, style.bold(), style.italic(), style.underline(),
                    style.strikethrough(), style.obfuscated()));
        }
        return result;
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

    private static int normalizeAlpha(int argb) {
        return (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb;
    }

    private static float currentRasterScale() {
        return Math.max(1.0F,
                FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
    }

    @Override
    public synchronized void close() {
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
            try (StraightAlphaBlendState ignored = new StraightAlphaBlendState()) {
                for (GlyphDraw draw : glyphs) {
                    BakedGlyph glyph = draw.glyph;
                    if (draw.obfuscatedSet != null) {
                        long frame = TextAnimationFrame.current();
                        BakedGlyph random = draw.obfuscatedSet.getRandomGlyph(draw.obfuscatedAdvance,
                                frame * 0x9E3779B97F4A7C15L
                                        ^ Float.floatToIntBits(draw.x) * 0xC2B2AE3D27D4EB4FL);
                        if (random != null) glyph = random;
                    }
                    mc.getTextureManager().bindTexture(glyph.getTextureLocation());
                    TextAnimationSampler.Sample animation = draw.animation;
                    for (TextAnimationSampler.Sample layer : animation.layers()) {
                        if (!layer.visible || layer.alpha == 0.0F) continue;
                        float drawX = x + draw.x + (float) layer.x;
                        float drawY = y + (float) layer.y;
                        float red = (layer.argb >> 16 & 255) / 255.0F;
                        float green = (layer.argb >> 8 & 255) / 255.0F;
                        float blue = (layer.argb & 255) / 255.0F;
                        boolean fractional = layer.x != 0.0 || layer.y != 0.0;
                        try (FontRenderTuning.FractionalPositionScope fractionalScope = fractional
                                ? FontRenderTuning.allowFractionalPosition() : null) {
                            renderGlyph(glyph, draw, drawX, drawY, red, green, blue,
                                    alpha * layer.alpha, layer, layer.maskTop, layer.maskBottom);
                            if (!TextAnimationFrame.neonOverdrawSuppressed()) {
                                for (TextAnimationSampler.Glow glow : animation.glows()) {
                                    if (glow.passes <= 0 || glow.alphaMultiplier <= 0.0F) continue;
                                    for (int pass = 0; pass < glow.passes; pass++) {
                                        double angle = Math.PI * 2.0 * pass / glow.passes;
                                        renderGlyph(glyph, draw,
                                                drawX + (float) Math.cos(angle) * glow.radius,
                                                drawY + (float) Math.sin(angle) * glow.radius,
                                                red, green, blue,
                                                alpha * layer.alpha * glow.alphaMultiplier, layer,
                                                0.0F, 0.0F);
                                    }
                                }
                            }
                        }
                    }
                }
                for (EffectDraw effect : effects) {
                    drawSolidQuad(x + effect.left, y + effect.top, x + effect.right,
                            y + effect.bottom, effect.argb, alpha * effect.alpha);
                }
            }
        }

        private static void renderGlyph(BakedGlyph glyph, GlyphDraw draw, float x, float y,
                                        float red, float green, float blue, float alpha,
                                        TextAnimationSampler.Sample animation,
                                        float maskTop, float maskBottom) {
            double radians = animation.rotation != 0.0
                    ? animation.rotation : animation.pendulumRotation;
            if (radians != 0.0 || animation.scale != 1.0F) {
                float pivotX = draw.obfuscatedAdvance * 0.5F;
                float pivotY = animation.rotation != 0.0 ? draw.lineHeight * 0.5F : 0.0F;
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + pivotX, y + pivotY, 0.0F);
                GlStateManager.rotate((float) Math.toDegrees(radians), 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(animation.scale, animation.scale, 1.0F);
                glyph.renderClipped(draw.italic, -pivotX, -pivotY, red, green, blue, alpha,
                        maskTop, maskBottom);
                if (draw.bold) glyph.renderClipped(draw.italic,
                        -pivotX + draw.boldOffset, -pivotY,
                        red, green, blue, alpha, maskTop, maskBottom);
                GlStateManager.popMatrix();
            } else {
                glyph.renderClipped(draw.italic, x, y, red, green, blue, alpha,
                        maskTop, maskBottom);
                if (draw.bold) glyph.renderClipped(draw.italic, x + draw.boldOffset, y,
                        red, green, blue, alpha, maskTop, maskBottom);
            }
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
        private final FontSet obfuscatedSet;
        private final float obfuscatedAdvance;
        private final float lineHeight;
        private final TextAnimationSampler.Sample animation;

        private GlyphDraw(BakedGlyph glyph, float x, int argb, boolean bold, boolean italic,
                          float boldOffset, FontSet obfuscatedSet, float obfuscatedAdvance,
                          float lineHeight, TextAnimationSampler.Sample animation) {
            this.glyph = glyph;
            this.x = x;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
            this.boldOffset = boldOffset;
            this.obfuscatedSet = obfuscatedSet;
            this.obfuscatedAdvance = obfuscatedAdvance;
            this.lineHeight = lineHeight;
            this.animation = animation == null ? new TextAnimationSampler.Sample() : animation;
        }
    }

    private static final class EffectDraw {
        private final float left;
        private final float top;
        private final float right;
        private final float bottom;
        private final int argb;
        private final float alpha;

        private EffectDraw(float left, float top, float right, float bottom, int argb) {
            this(left, top, right, bottom, argb, 1.0F);
        }

        private EffectDraw(float left, float top, float right, float bottom, int argb,
                           float alpha) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.argb = argb;
            this.alpha = alpha;
        }
    }

    private static final class FormattedRun {
        private final String text;
        private final int argb;
        private final boolean bold;
        private final boolean italic;
        private final boolean underline;
        private final boolean strikethrough;
        private final boolean obfuscated;

        private FormattedRun(String text, int argb, boolean bold, boolean italic,
                             boolean underline, boolean strikethrough, boolean obfuscated) {
            this.text = text;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
            this.obfuscated = obfuscated;
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

}
