package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import neofontrender.api.text.ModernTextApi;
import neofontrender.api.text.ModernTextLayout;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import neofontrender.api.text.CjkParagraphLayoutRegistry;
import neofontrender.core.font.support.ScopedFontRenderBypass;
import neofontrender.core.font.support.ShadowColorPolicy;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.BakedGlyph;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.awt.GlyphInfo;
import neofontrender.core.font.awt.TextRunBatcher;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.backend.BackendTextSegmenter;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.preprocess.PreprocessedText;
import neofontrender.core.font.preprocess.LayoutText;
import neofontrender.core.font.preprocess.TextPreprocessingPipeline;
import neofontrender.core.font.linebreak.CjkLineBreakRules;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.StringErrorCorrector;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Bridges vanilla {@link FontRenderer} into the replacement TTF pipeline.
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {

    @Shadow public float posX;
    @Shadow public float posY;
    @Shadow public int FONT_HEIGHT;
    @Shadow private float red;
    @Shadow private float blue;
    @Shadow private float green;
    @Shadow private float alpha;
    @Shadow private int textColor;
    @Shadow private int[] colorCode;
    @Shadow private boolean randomStyle;
    @Shadow private boolean boldStyle;
    @Shadow private boolean italicStyle;
    @Shadow private boolean underlineStyle;
    @Shadow private boolean strikethroughStyle;
    @Shadow protected abstract void setColor(float red, float green, float blue, float alpha);

    private final TextRunBatcher sfr$batcher = new TextRunBatcher();
    private final FloatBuffer sfr$colorBuffer = BufferUtils.createFloatBuffer(4);
    private int[] sfr$activeColorCodes = TextColorPaletteRegistry.vanillaColorCodes();
    private int[] sfr$runtimeColorSnapshot = new int[0];
    private String sfr$paletteProvider = "";
    private long sfr$paletteRevision = Long.MIN_VALUE;
    private int sfr$renderPassColor = 0xFFFFFFFF;

    // ================================================================== //
    //  Render hook
    // ================================================================== //

    @Inject(method = "renderString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"))
    private void sfr$captureRenderPassColor(String text, float x, float y, int color,
                                            boolean shadow,
                                            CallbackInfoReturnable<Integer> cir) {
        this.sfr$renderPassColor = color;
    }

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void sfr$onDrawString(String text, float x, float y, int color, boolean dropShadow,
                                  CallbackInfoReturnable<Integer> cir) {
        FontRenderTuning.updateFromCurrentGlState(dropShadow);
        if (!sfr$shouldHook() || text == null) {
            return;
        }
        sfr$syncTextColorPalette();
        PreprocessedText preprocessed = TextPreprocessingPipeline.process(text);
        if (preprocessed.transformed() && ModernTextApi.isAvailable()) {
            color = sfr$resolveEffectiveColor(color);
            float advance = sfr$drawPreprocessedText(
                    preprocessed, x, y, color, dropShadow);
            this.posX = x + advance;
            this.posY = y;
            cir.setReturnValue(sfr$drawStringReturnX(x, advance, dropShadow));
            return;
        }
        if (!FontManager.INSTANCE.isTextBackendActive()
                || !NeofontrenderConfig.advancedStringMode()) {
            return;
        }

        GlStateManager.enableAlpha();
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null) {
            return;
        }
        color = sfr$resolveEffectiveColor(color);
        if (dropShadow && NeofontrenderConfig.modernShadowEnabled()
                && backend.supportsModernShadow() && backend.shouldRenderShadow(text)) {
            TextRenderResult rendered = backend.renderFormattedWithShadow(text, color);
            rendered.draw(x, y, alphaFromColor(color));
            this.posX = x + rendered.advance();
            this.posY = y;
            cir.setReturnValue(sfr$drawStringReturnX(x, rendered.advance(), true));
            return;
        }
        if (dropShadow) {
            sfr$drawSelectiveShadow(backend, text, x, y, color);
        }
        TextRenderResult rendered = backend.renderFormatted(text, color, false);
        rendered.draw(x, y, alphaFromColor(color));
        this.posX = x + rendered.advance();
        this.posY = y;
        cir.setReturnValue(sfr$drawStringReturnX(x, rendered.advance(), dropShadow));
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"), cancellable = true)
    private void sfr$onRenderStringAtPos(String text, boolean shadow, CallbackInfo ci) {
        FontRenderTuning.updateFromCurrentGlState(shadow);
        if (!sfr$shouldHook() || text == null) {
            return;
        }
        sfr$syncTextColorPalette();
        PreprocessedText preprocessed = TextPreprocessingPipeline.process(text);
        if (shadow && !ShadowColorPolicy.VANILLA.equals(NeofontrenderConfig.shadowColorMode())
                && (sfr$isAnyActive()
                    || preprocessed.transformed() && ModernTextApi.isAvailable())) {
            sfr$applyShadowBase();
        }
        if (preprocessed.transformed() && ModernTextApi.isAvailable()) {
            ModernTextLayout layout = ModernTextApi.layoutFormatted(
                    preprocessed.modernText(), NeofontrenderConfig.fontSize(),
                    sfr$currentArgb(), shadow);
            layout.draw(this.posX, this.posY,
                    shadow ? NeofontrenderConfig.shadowOpacity() : 1.0F);
            this.posX += layout.advance();
            ci.cancel();
            return;
        }
        if (FontManager.INSTANCE.isTextBackendActive()) {
            TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
            if (shadow && backend != null && !backend.shouldRenderShadow(text)) {
                // Vanilla advances the pen during its shadow pass as well. Preserve that state
                // while omitting the duplicate color-glyph rasterization.
                this.posX += backend.measureFormatted(text, sfr$currentArgb(), false);
                ci.cancel();
                return;
            }
            sfr$renderBackendFormatted(text, shadow);
            ci.cancel();
            return;
        }
        if (!FontManager.INSTANCE.isSfrActive()) {
            return;
        }

        float baseRed = this.red;
        float baseBlue = this.blue;
        float baseGreen = this.green;
        float baseAlpha = this.alpha;

        int runStart = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != 167 || i + 1 >= text.length()) {
                continue;
            }

            if (i > runStart) {
                sfr$renderRun(text.substring(runStart, i));
            }

            int style = "0123456789abcdefklmnor".indexOf(String.valueOf(text.charAt(i + 1))
                    .toLowerCase(Locale.ROOT).charAt(0));
            if (style < 16) {
                this.randomStyle = false;
                this.boldStyle = false;
                this.strikethroughStyle = false;
                this.underlineStyle = false;
                this.italicStyle = false;

                int colorIndex = style < 0 ? 15 : style;
                int color = ShadowColorPolicy.paletteColor(colorIndex,
                        Math.round(baseAlpha * 255.0F) << 24, shadow,
                        NeofontrenderConfig.shadowColorMode(), NeofontrenderConfig.shadowColor(),
                        NeofontrenderConfig.shadowColorOverrides(), sfr$activeColorCodes);
                this.textColor = color;
                this.red = (float) (color >> 16 & 255) / 255.0F;
                this.blue = (float) (color >> 8 & 255) / 255.0F;
                this.green = (float) (color & 255) / 255.0F;
                this.alpha = baseAlpha;
                GlStateManager.color(this.red, this.blue, this.green, this.alpha);
            } else if (style == 16) {
                this.randomStyle = true;
            } else if (style == 17) {
                this.boldStyle = true;
            } else if (style == 18) {
                this.strikethroughStyle = true;
            } else if (style == 19) {
                this.underlineStyle = true;
            } else if (style == 20) {
                this.italicStyle = true;
            } else if (style == 21) {
                this.randomStyle = false;
                this.boldStyle = false;
                this.strikethroughStyle = false;
                this.underlineStyle = false;
                this.italicStyle = false;
                this.red = baseRed;
                this.blue = baseBlue;
                this.green = baseGreen;
                this.alpha = baseAlpha;
                GlStateManager.color(this.red, this.blue, this.green, this.alpha);
            }

            i++;
            runStart = i + 1;
        }

        if (runStart < text.length()) {
            sfr$renderRun(text.substring(runStart));
        }

        ci.cancel();
    }

    @Inject(method = "renderChar", at = @At("HEAD"), cancellable = true)
    private void sfr$onRenderChar(char ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (!sfr$shouldHook()) {
            return;
        }
        if (FontManager.INSTANCE.isTextBackendActive()) {
            TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
            if (backend == null) {
                return;
            }
            if (Character.isHighSurrogate(ch) || Character.isLowSurrogate(ch)) {
                cir.setReturnValue(0.0F);
                return;
            }
            if (ch == ' ' || ch == 160) {
                cir.setReturnValue(backend.measure(" ", this.boldStyle, italic));
                return;
            }
            String text = String.valueOf(ch);
            TextRenderResult rendered = backend.render(text, sfr$currentArgb(), this.boldStyle, italic);
            rendered.draw(this.posX, this.posY, this.alpha);
            cir.setReturnValue(rendered.advance());
            return;
        }
        if (!FontManager.INSTANCE.isSfrActive()) {
            return;
        }

        if (ch == ' ' || ch == 160) {
            GlyphInfo info = FontManager.INSTANCE.getDefaultFontSet().getGlyphInfo(ch);
            if (info != null) {
                cir.setReturnValue(info.getAdvance(false));
            }
            return;
        }

        BakedGlyph glyph = FontManager.INSTANCE.getDefaultFontSet().getGlyph(ch);
        if (glyph == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(glyph.getTextureLocation());
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        glyph.render(italic, this.posX, this.posY, this.red, this.blue, this.green, this.alpha);

        GlyphInfo info = FontManager.INSTANCE.getDefaultFontSet().getGlyphInfo(ch);
        if (info != null) {
            cir.setReturnValue(info.getAdvance(false));
        }
    }

    private void sfr$renderRun(String run) {
        if (run.isEmpty()) {
            return;
        }

        FontSet fontSet = FontManager.INSTANCE.getDefaultFontSet();
        fontSet.flushAtlas();
        float startX = this.posX;
        float[] positions = fontSet.layoutPositions(run, this.boldStyle);

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        for (int i = 0; i < run.length(); ) {
            int codePoint = run.codePointAt(i);
            int next = i + Character.charCount(codePoint);
            if (codePoint == ' ' || codePoint == 160) {
                i = next;
                continue;
            }

            GlyphInfo info = fontSet.getGlyphInfo(codePoint);
            BakedGlyph glyph = this.randomStyle && info != null
                    ? fontSet.getRandomGlyph(info.getAdvance(false))
                    : fontSet.getGlyph(codePoint);
            if (glyph == null) {
                glyph = fontSet.getGlyph(codePoint);
            }
            if (glyph == null) {
                i = next;
                continue;
            }

            float x = startX + positions[i];
            sfr$batcher.addGlyph(glyph, this.italicStyle, x, this.posY,
                    this.red, this.blue, this.green, this.alpha);
            if (this.boldStyle) {
                sfr$batcher.addGlyph(glyph, this.italicStyle, x + 1.0F, this.posY,
                        this.red, this.blue, this.green, this.alpha);
            }
            i = next;
        }

        sfr$batcher.flush();

        float width = positions[positions.length - 1];
        if (this.strikethroughStyle) {
            sfr$drawEffect(startX, this.posY + (float) (this.FONT_HEIGHT / 2),
                    startX + width, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F);
        }
        if (this.underlineStyle) {
            sfr$drawEffect(startX - 1.0F, this.posY + (float) this.FONT_HEIGHT,
                    startX + width, this.posY + (float) this.FONT_HEIGHT - 1.0F);
        }

        this.posX = startX + width;
    }

    private void sfr$renderBackendFormatted(String text, boolean shadow) {
        float baseRed = this.red;
        float baseBlue = this.blue;
        float baseGreen = this.green;
        float baseAlpha = this.alpha;
        StringErrorCorrector corrector = new StringErrorCorrector();

        int runStart = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != 167 || i + 1 >= text.length()) {
                continue;
            }

            if (i > runStart) {
                sfr$renderBackendRun(text.substring(runStart, i), corrector);
            }

            int style = "0123456789abcdefklmnor".indexOf(String.valueOf(text.charAt(i + 1))
                    .toLowerCase(Locale.ROOT).charAt(0));
            if (style < 16) {
                this.randomStyle = false;
                this.boldStyle = false;
                this.strikethroughStyle = false;
                this.underlineStyle = false;
                this.italicStyle = false;

                int colorIndex = style < 0 ? 15 : style;
                int color = ShadowColorPolicy.paletteColor(colorIndex,
                        Math.round(baseAlpha * 255.0F) << 24, shadow,
                        NeofontrenderConfig.shadowColorMode(), NeofontrenderConfig.shadowColor(),
                        NeofontrenderConfig.shadowColorOverrides(), sfr$activeColorCodes);
                this.textColor = color;
                this.red = (float) (color >> 16 & 255) / 255.0F;
                this.blue = (float) (color >> 8 & 255) / 255.0F;
                this.green = (float) (color & 255) / 255.0F;
                this.alpha = baseAlpha;
            } else if (style == 16) {
                this.randomStyle = true;
            } else if (style == 17) {
                this.boldStyle = true;
            } else if (style == 18) {
                this.strikethroughStyle = true;
            } else if (style == 19) {
                this.underlineStyle = true;
            } else if (style == 20) {
                this.italicStyle = true;
            } else if (style == 21) {
                this.randomStyle = false;
                this.boldStyle = false;
                this.strikethroughStyle = false;
                this.underlineStyle = false;
                this.italicStyle = false;
                this.red = baseRed;
                this.blue = baseBlue;
                this.green = baseGreen;
                this.alpha = baseAlpha;
            }

            i++;
            runStart = i + 1;
        }

        if (runStart < text.length()) {
            sfr$renderBackendRun(text.substring(runStart), corrector);
        }
    }

    private void sfr$renderBackendRun(String run, StringErrorCorrector corrector) {
        if (run.isEmpty()) {
            return;
        }
        float startX = this.posX;
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null) {
            return;
        }
        List<String> segments = this.randomStyle ? null : BackendTextSegmenter.segment(run);
        if (segments != null) {
            float currentX = startX;
            for (String segment : segments) {
                float width;
                if (sfr$isWhitespaceSegment(segment)) {
                    width = backend.measure(segment, this.boldStyle, this.italicStyle);
                    currentX += width;
                    continue;
                }
                TextRenderResult rendered = backend.renderSegment(segment, sfr$currentArgb(), this.boldStyle, this.italicStyle);
                width = rendered.advance();
                rendered.draw(currentX, this.posY, this.alpha);
                currentX += width;
            }

            if (this.strikethroughStyle) {
                sfr$drawEffect(startX, this.posY + (float) (this.FONT_HEIGHT / 2),
                        currentX, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F);
            }
            if (this.underlineStyle) {
                sfr$drawEffect(startX - 1.0F, this.posY + (float) this.FONT_HEIGHT,
                        currentX, this.posY + (float) this.FONT_HEIGHT - 1.0F);
            }

            corrector.reset();
            this.posX = currentX;
            return;
        }

        TextRenderResult rendered = backend.render(run, sfr$currentArgb(), this.boldStyle, this.italicStyle);
        float width = rendered.advance();
        float correctedX = corrector.correct(startX, width);
        rendered.draw(correctedX, this.posY, this.alpha);

        if (this.strikethroughStyle) {
            sfr$drawEffect(correctedX, this.posY + (float) (this.FONT_HEIGHT / 2),
                    correctedX + width, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F);
        }
        if (this.underlineStyle) {
            sfr$drawEffect(correctedX - 1.0F, this.posY + (float) this.FONT_HEIGHT,
                    correctedX + width, this.posY + (float) this.FONT_HEIGHT - 1.0F);
        }

        this.posX = correctedX + width;
    }

    private static boolean sfr$isWhitespaceSegment(String text) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (!Character.isWhitespace(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    private void sfr$drawEffect(float x0, float y0, float x1, float y1) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        buffer.begin(7, DefaultVertexFormats.POSITION);
        buffer.pos(x0, y0, 0.0D).endVertex();
        buffer.pos(x1, y0, 0.0D).endVertex();
        buffer.pos(x1, y1, 0.0D).endVertex();
        buffer.pos(x0, y1, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    // ================================================================== //
    //  Width hook
    // ================================================================== //

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$onGetCharWidth(char character, CallbackInfoReturnable<Integer> cir) {
        if (TextPreprocessingPipeline.isInvisibleControlCharacter(character)) {
            cir.setReturnValue(0);
            return;
        }
        if (!sfr$isAnyActive()) {
            return;
        }

        if (FontManager.INSTANCE.isTextBackendActive()) {
            cir.setReturnValue((int) Math.ceil(sfr$getCharWidthFloat(character == 160 ? ' ' : character, this.boldStyle)));
            return;
        }

        if (character == 160) {
            cir.setReturnValue(4);
            return;
        }
        if (character == ' ') {
            GlyphInfo info = FontManager.INSTANCE.getDefaultFontSet().getGlyphInfo(character);
            if (info != null) {
                cir.setReturnValue((int) Math.ceil(info.getAdvance(false)));
            } else {
                cir.setReturnValue(4);
            }
            return;
        }
        if (character == 167) {
            cir.setReturnValue(-1);
            return;
        }

        GlyphInfo info = FontManager.INSTANCE.getDefaultFontSet().getGlyphInfo(character);
        if (info != null) {
            cir.setReturnValue((int) Math.ceil(info.getAdvance(false)));
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$onGetStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null) {
            return;
        }
        PreprocessedText preprocessed = TextPreprocessingPipeline.process(text);
        if (preprocessed.transformed() && ModernTextApi.isAvailable()) {
            cir.setReturnValue((int) Math.ceil(ModernTextApi.measureFormatted(
                    preprocessed.modernText(), NeofontrenderConfig.fontSize(),
                    0xFFFFFFFF, false)));
            return;
        }
        if (!sfr$isAnyActive()) return;
        cir.setReturnValue((int) Math.ceil(sfr$getFormattedStringWidthFloat(text)));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void sfr$onTrimStringToWidth(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (text == null) {
            return;
        }
        PreprocessedText preprocessed = TextPreprocessingPipeline.process(text);
        if (preprocessed.transformed() && ModernTextApi.isAvailable()) {
            cir.setReturnValue(sfr$trimPreprocessedText(preprocessed, width, reverse));
            return;
        }
        if (!sfr$isAnyActive()) return;

        StringBuilder out = new StringBuilder();
        float currentWidth = 0.0F;
        boolean bold = false;

        if (reverse) {
            boolean[] boldAt = sfr$boldStateByIndex(text);
            for (int i = text.length(); i > 0 && currentWidth < width; ) {
                int codePoint = text.codePointBefore(i);
                int start = i - Character.charCount(codePoint);
                if (start > 0 && text.charAt(start - 1) == 167) {
                    out.insert(0, text.substring(start - 1, i));
                    i = start - 1;
                    continue;
                }
                if (codePoint == 167 && i < text.length()) {
                    out.insert(0, text.substring(start, Math.min(text.length(), i + 1)));
                    i = start;
                    continue;
                }
                currentWidth += sfr$getCharWidthFloat(codePoint, boldAt[start]);
                if (currentWidth > width) {
                    break;
                }
                out.insert(0, text.substring(start, i));
                i = start;
            }
        } else {
            for (int i = 0; i < text.length() && currentWidth < width; ) {
                char ch = text.charAt(i);
                if (ch == 167 && i < text.length() - 1) {
                    char code = text.charAt(i + 1);
                    if (code == 'l' || code == 'L') {
                        bold = true;
                    } else if (code == 'r' || code == 'R' || sfr$isFormatColor(code)) {
                        bold = false;
                    }
                    out.append(ch).append(code);
                    i += 2;
                    continue;
                }

                int codePoint = text.codePointAt(i);
                int next = i + Character.charCount(codePoint);
                currentWidth += sfr$getCharWidthFloat(codePoint, bold);
                if (currentWidth > width) {
                    break;
                }
                out.append(text, i, next);
                i = next;
            }
        }

        cir.setReturnValue(out.toString());
    }

    @Inject(method = "sizeStringToWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$onSizeStringToWidth(String str, int wrapWidth, CallbackInfoReturnable<Integer> cir) {
        if (str == null) {
            return;
        }
        CjkParagraphLayoutProvider.Layout paragraph = sfr$layoutCjkParagraph(str, wrapWidth);
        if (paragraph != null) {
            cir.setReturnValue(Math.min(str.length(),
                    Math.max(0, paragraph.firstRawBoundary(str.length()))));
            return;
        }
        LayoutText layoutText = LayoutText.process(str);
        if (layoutText.transformed() && ModernTextApi.isAvailable()) {
            cir.setReturnValue(sfr$sizeLayoutTextToWidth(layoutText, wrapWidth));
            return;
        }
        boolean rendererActive = sfr$isAnyActive();
        boolean cjkLineBreak = NeofontrenderConfig.fixCjkLineBreak();
        if (!rendererActive && !cjkLineBreak) return;

        int len = str.length();
        int pos;
        int breakPos = -1;
        int previousCodePoint = -1;
        int boundaryAfterPrevious = 0;
        float width = 0.0F;
        boolean bold = false;

        for (pos = 0; pos < len; ) {
            int codePoint = str.codePointAt(pos);
            char ch = str.charAt(pos);
            switch (ch) {
                case '\n':
                    breakPos = pos;
                    cir.setReturnValue(pos != len && breakPos != -1 && breakPos < pos ? breakPos : pos);
                    return;
                case ' ':
                    breakPos = pos;
                    width += sfr$getWrappingCharWidth(codePoint, bold, rendererActive);
                    pos++;
                    previousCodePoint = codePoint;
                    boundaryAfterPrevious = pos;
                    break;
                case 167:
                    if (pos < len - 1) {
                        char code = str.charAt(++pos);
                        if (code == 'l' || code == 'L') {
                            bold = true;
                        } else if (code == 'r' || code == 'R' || sfr$isFormatColor(code)) {
                            bold = false;
                        }
                    }
                    pos++;
                    break;
                default:
                    if (cjkLineBreak && previousCodePoint >= 0
                            && CjkLineBreakRules.canBreakBetween(previousCodePoint, codePoint)) {
                        breakPos = boundaryAfterPrevious;
                    }
                    width += sfr$getWrappingCharWidth(codePoint, bold, rendererActive);
                    pos += Character.charCount(codePoint);
                    previousCodePoint = codePoint;
                    boundaryAfterPrevious = pos;
                    break;
            }

            if (width > wrapWidth) {
                break;
            }
        }

        cir.setReturnValue(pos != len && breakPos != -1 && breakPos < pos ? breakPos : pos);
    }

    @Inject(method = "drawSplitString", at = @At("HEAD"), cancellable = true)
    private void sfr$drawCjkParagraph(String str, int x, int y, int wrapWidth, int textColor,
                                      CallbackInfo ci) {
        if (str == null) return;
        CjkParagraphLayoutProvider.Layout paragraph = sfr$layoutCjkParagraph(str, wrapWidth);
        if (paragraph == null) return;
        FontRenderer self = (FontRenderer) (Object) this;
        for (CjkParagraphLayoutProvider.Line line : paragraph.lines()) {
            for (CjkParagraphLayoutProvider.Run run : line.runs()) {
                self.drawString(run.formattedText(), x + run.xOffset(),
                        y + line.yOffset(), textColor, false);
            }
        }
        ci.cancel();
    }

    private CjkParagraphLayoutProvider.Layout sfr$layoutCjkParagraph(String text, int width) {
        if (!NeofontrenderConfig.fixCjkLineBreak()) return null;
        FontRenderer self = (FontRenderer) (Object) this;
        return CjkParagraphLayoutRegistry.layout(new CjkParagraphLayoutProvider.Request(
                text, width, this.FONT_HEIGHT, sfr$currentLanguageCode(), self::getStringWidth));
    }

    private static String sfr$currentLanguageCode() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.getLanguageManager() == null
                || minecraft.getLanguageManager().getCurrentLanguage() == null
                ? "" : minecraft.getLanguageManager().getCurrentLanguage().getLanguageCode();
    }

    private float sfr$getWrappingCharWidth(int codePoint, boolean bold, boolean rendererActive) {
        if (rendererActive) {
            return sfr$getCharWidthFloat(codePoint, bold);
        }
        FontRenderer self = (FontRenderer) (Object) this;
        float width;
        if (codePoint <= Character.MAX_VALUE) {
            width = self.getCharWidth((char) codePoint);
        } else {
            width = self.getStringWidth(new String(Character.toChars(codePoint)));
        }
        return bold && width > 0.0F ? width + 1.0F : width;
    }

    private float sfr$getStringWidthFloat(String text) {
        return sfr$getFormattedStringWidthFloat(text);
    }

    private float sfr$getFormattedStringWidthFloat(String text) {
        if (FontManager.INSTANCE.isTextBackendActive() && NeofontrenderConfig.advancedStringMode()) {
            TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
            return backend == null ? 0.0F : backend.measureFormatted(text, 0xFFFFFFFF, false);
        }
        float width = 0.0F;
        boolean bold = false;
        int runStart = 0;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == 167 && i < text.length() - 1) {
                if (i > runStart) {
                    width += sfr$getRunWidth(text.substring(runStart, i), bold);
                }
                char code = text.charAt(++i);
                if (code == 'l' || code == 'L') {
                    bold = true;
                } else if (code == 'r' || code == 'R' || sfr$isFormatColor(code)) {
                    bold = false;
                }
                runStart = i + 1;
                continue;
            }
        }
        if (runStart < text.length()) {
            width += sfr$getRunWidth(text.substring(runStart), bold);
        }
        return width;
    }

    private float sfr$getRunWidth(String run, boolean bold) {
        if (run.isEmpty()) {
            return 0.0F;
        }
        if (FontManager.INSTANCE.isTextBackendActive()) {
            TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
            if (backend == null) {
                return 0.0F;
            }
            if (NeofontrenderConfig.advancedStringMode()) {
                return backend.measureFormatted(run, 0xFFFFFFFF, false);
            }
            List<String> segments = BackendTextSegmenter.segment(run);
            if (segments != null) {
                float width = 0.0F;
                for (String segment : segments) {
                    width += backend.measure(segment, bold, false);
                }
                return width;
            }
            return backend.measure(run, bold, false);
        }
        float[] positions = FontManager.INSTANCE.getDefaultFontSet().layoutPositions(run, bold);
        return positions[positions.length - 1];
    }

    private float sfr$getCharWidthFloat(int codePoint, boolean bold) {
        if (codePoint == 167) {
            return -1.0F;
        }
        if (Character.isHighSurrogate((char) codePoint) || Character.isLowSurrogate((char) codePoint)) {
            return 0.0F;
        }
        if (FontManager.INSTANCE.isTextBackendActive()) {
            TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
            return backend == null ? 0.0F
                    : backend.measure(new String(Character.toChars(codePoint == 160 ? ' ' : codePoint)), bold, false);
        }
        GlyphInfo info = FontManager.INSTANCE.getDefaultFontSet().getGlyphInfo(codePoint == 160 ? ' ' : codePoint);
        return info == null ? 0.0F : info.getAdvance(bold);
    }

    private boolean sfr$isAnyActive() {
        return sfr$shouldHook() && (FontManager.INSTANCE.isSfrActive() || FontManager.INSTANCE.isTextBackendActive());
    }

    private boolean sfr$shouldHook() {
        if (ScopedFontRenderBypass.isActive()) return false;
        String className = ((Object) this).getClass().getName();
        return !className.equals("net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer")
                && !className.endsWith("SimpleModelFontRenderer");
    }

    /**
     * Snapshot the final palette on the FontRenderer instance. Other constructor-tail mixins may
     * replace those entries; resolving here observes their completed runtime values.
     */
    private void sfr$syncTextColorPalette() {
        NeofontrenderConfig.ensureLoadedForEarlyRendering();
        String provider = NeofontrenderConfig.textColorPaletteProvider();
        long revision = TextColorPaletteRegistry.revision();
        if (provider.equals(sfr$paletteProvider) && revision == sfr$paletteRevision
                && Arrays.equals(sfr$runtimeColorSnapshot, this.colorCode)) return;
        sfr$paletteProvider = provider;
        sfr$paletteRevision = revision;
        sfr$runtimeColorSnapshot = this.colorCode == null ? new int[0] : this.colorCode.clone();
        sfr$activeColorCodes = TextColorPaletteRegistry.resolve(provider, this.colorCode);
        FontManager.INSTANCE.updateLegacyColorCodes(sfr$activeColorCodes);
    }

    private int sfr$legacyColor(int index) {
        return index >= 0 && index < sfr$activeColorCodes.length
                ? sfr$activeColorCodes[index] & 0xFFFFFF : 0xFFFFFF;
    }

    private int sfr$shadowColor(int foreground, int candidate) {
        return NeofontrenderConfig.shadowColorOverrides().remap(
                foreground, candidate, sfr$activeColorCodes);
    }

    /** Resolves the configured shadow base before vanilla's formatted run parser executes. */
    private void sfr$applyShadowBase() {
        int color = ShadowColorPolicy.shadowColor(
                this.sfr$renderPassColor, NeofontrenderConfig.shadowColorMode(),
                NeofontrenderConfig.shadowColor(), NeofontrenderConfig.shadowColorOverrides(),
                sfr$activeColorCodes);
        this.textColor = color;
        this.red = (color >> 16 & 255) / 255.0F;
        this.blue = (color >> 8 & 255) / 255.0F;
        this.green = (color & 255) / 255.0F;
        this.alpha = alphaFromColor(color);
        GlStateManager.color(this.red, this.blue, this.green, this.alpha);
    }

    /**
     * Preserve FontRenderer subclasses that implement color as a GL multiplier (for example
     * StellarAPI's WrappedFontRenderer) instead of encoding it in drawString's integer argument.
     */
    private int sfr$resolveEffectiveColor(int packedColor) {
        float alpha = alphaFromColor(packedColor);
        float red = (packedColor >> 16 & 255) / 255.0F;
        float green = (packedColor >> 8 & 255) / 255.0F;
        float blue = (packedColor & 255) / 255.0F;
        this.setColor(red, green, blue, alpha);
        this.sfr$colorBuffer.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, this.sfr$colorBuffer);
        int r = Math.max(0, Math.min(255, Math.round(this.sfr$colorBuffer.get(0) * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(this.sfr$colorBuffer.get(1) * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(this.sfr$colorBuffer.get(2) * 255.0F)));
        int a = Math.max(0, Math.min(255, Math.round(this.sfr$colorBuffer.get(3) * 255.0F)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int sfr$currentArgb() {
        int a = Math.max(0, Math.min(255, Math.round(this.alpha * 255.0F)));
        int r = Math.max(0, Math.min(255, Math.round(this.red * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(this.blue * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(this.green * 255.0F)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static float alphaFromColor(int color) {
        if ((color & 0xFC000000) == 0) {
            return 1.0F;
        }
        return (float) (color >>> 24) / 255.0F;
    }

    private static float shadowAlpha(int color) {
        return alphaFromColor(color) * NeofontrenderConfig.shadowOpacity();
    }

    /**
     * Vanilla five-arg drawString returns the max of the shadow-pass and main-pass pen
     * positions, so a drawn shadow adds its positive offset to the returned X. posX itself
     * still keeps the main-pass position.
     */
    private static int sfr$drawStringReturnX(float x, float advance, boolean dropShadow) {
        int main = (int) (x + advance);
        if (!dropShadow) {
            return main;
        }
        float offset = NeofontrenderConfig.modernShadowEnabled()
                ? NeofontrenderConfig.shadowOffsetX() : NeofontrenderConfig.shadowLength();
        return Math.max(main, (int) (x + advance + Math.max(0.0F, offset)));
    }

    private float sfr$drawPreprocessedText(PreprocessedText text, float x, float y,
                                           int color, boolean dropShadow) {
        GlStateManager.enableAlpha();
        float fontSize = NeofontrenderConfig.fontSize();
        if (dropShadow && NeofontrenderConfig.modernShadowEnabled()
                && ModernTextApi.canRenderModernShadow(text.modernText())) {
            ModernTextLayout rendered = ModernTextApi.layoutFormattedWithShadow(
                    text.modernText(), fontSize, color);
            rendered.draw(x, y);
            return rendered.advance();
        }
        if (dropShadow) {
            float offsetX = NeofontrenderConfig.modernShadowEnabled()
                    ? NeofontrenderConfig.shadowOffsetX()
                    : NeofontrenderConfig.shadowLength();
            float offsetY = NeofontrenderConfig.modernShadowEnabled()
                    ? NeofontrenderConfig.shadowOffsetY()
                    : NeofontrenderConfig.shadowLength();
            ModernTextLayout shadow = ModernTextApi.layoutFormatted(
                    text.modernText(), fontSize, color, true);
            shadow.draw(x + offsetX, y + offsetY, NeofontrenderConfig.shadowOpacity());
        }
        ModernTextLayout foreground = ModernTextApi.layoutFormatted(
                text.modernText(), fontSize, color, false);
        foreground.draw(x, y);
        return foreground.advance();
    }

    private String sfr$trimPreprocessedText(PreprocessedText text, int width,
                                            boolean reverse) {
        String raw = text.rawText();
        String visible = text.visibleText();
        if (visible.isEmpty()) return raw;

        if (!reverse) {
            int acceptedRawEnd = text.rawEndForVisibleBoundary(0);
            for (int index = 0; index < visible.length();) {
                int next;
                if (visible.charAt(index) == 167 && index + 1 < visible.length()) {
                    next = index + 2;
                } else {
                    next = index + Character.charCount(visible.codePointAt(index));
                }
                int candidateRawEnd = text.rawEndForVisibleBoundary(next);
                if (sfr$measurePreprocessedRaw(raw.substring(0, candidateRawEnd)) > width) {
                    break;
                }
                acceptedRawEnd = candidateRawEnd;
                index = next;
            }
            return raw.substring(0, acceptedRawEnd);
        }

        int acceptedRawStart = raw.length();
        for (int index = visible.length(); index > 0;) {
            int start = index - Character.charCount(visible.codePointBefore(index));
            if (start > 0 && visible.charAt(start - 1) == 167) {
                start--;
            }
            int candidateRawStart = text.rawStartForVisibleBoundary(start);
            String prefix = sfr$activeFormatPrefix(raw.substring(0, candidateRawStart));
            if (sfr$measurePreprocessedRaw(prefix + raw.substring(candidateRawStart)) > width) {
                break;
            }
            acceptedRawStart = candidateRawStart;
            index = start;
        }
        return raw.substring(acceptedRawStart);
    }

    private int sfr$sizeLayoutTextToWidth(LayoutText text, int wrapWidth) {
        String visible = text.visibleText();
        int breakRaw = -1;
        int previousCodePoint = -1;
        int boundaryAfterPrevious = 0;
        boolean cjkLineBreak = NeofontrenderConfig.fixCjkLineBreak();
        float width = 0.0F;

        for (int index = 0; index < visible.length();) {
            char ch = visible.charAt(index);
            if (ch == '\n') {
                return text.rawStartBoundary(index);
            }

            int codePoint = visible.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            if (ch == ' ') {
                breakRaw = text.rawStartBoundary(index);
            } else if (cjkLineBreak && previousCodePoint >= 0
                    && CjkLineBreakRules.canBreakBetween(previousCodePoint, codePoint)) {
                breakRaw = text.rawStartBoundary(boundaryAfterPrevious);
            }
            width += ModernTextApi.measureFormatted(text.formattedDisplay(index,
                            visible.substring(index, next)), NeofontrenderConfig.fontSize(),
                    0xFFFFFFFF, false);
            int rawEnd = text.rawEndBoundary(next);
            if (width > wrapWidth) {
                return breakRaw != -1 && breakRaw < rawEnd ? breakRaw : rawEnd;
            }
            previousCodePoint = codePoint;
            boundaryAfterPrevious = next;
            index = next;
        }
        return text.rawText().length();
    }

    private float sfr$measurePreprocessedRaw(String raw) {
        return ModernTextApi.measureFormatted(
                raw, NeofontrenderConfig.fontSize(), 0xFFFFFFFF, false);
    }

    private static String sfr$activeFormatPrefix(String text) {
        String color = "";
        StringBuilder styles = new StringBuilder(10);
        for (int index = 0; index + 1 < text.length(); index++) {
            if (text.charAt(index) != 167) continue;
            char code = Character.toLowerCase(text.charAt(++index));
            if (sfr$isFormatColor(code)) {
                color = "\u00A7" + code;
                styles.setLength(0);
            } else if (code == 'r') {
                color = "";
                styles.setLength(0);
            } else if ("klmno".indexOf(code) >= 0
                    && styles.indexOf("\u00A7" + code) < 0) {
                styles.append('\u00A7').append(code);
            }
        }
        return color + styles;
    }

    private void sfr$drawSelectiveShadow(TextRenderBackend backend, String text, float x, float y, int color) {
        float offsetX = NeofontrenderConfig.shadowOffsetX();
        float offsetY = NeofontrenderConfig.shadowOffsetY();
        if (backend.shouldRenderShadow(text)) {
            TextRenderResult shadow = backend.renderFormatted(text, color, true);
            shadow.draw(x + offsetX, y + offsetY, shadowAlpha(color));
            return;
        }
        // Group consecutive code points by shadow eligibility instead of splitting per code
        // point: keeps § format codes intact and preserves shaping (ligatures, kerning, ZWJ
        // emoji, Arabic) inside each shadowed run.
        float cursor = x;
        StringBuilder unit = new StringBuilder();
        boolean unitShadow = false;
        for (int index = 0; index < text.length();) {
            char ch = text.charAt(index);
            if (ch == 167 && index + 1 < text.length()) {
                // § format codes are control sequences, not glyphs; glue them to the unit.
                unit.append(text, index, index + 2);
                index += 2;
                continue;
            }
            int codePoint = text.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            boolean shadow = backend.shouldRenderShadow(text.substring(index, next));
            if (unit.length() > 0 && shadow != unitShadow) {
                cursor = sfr$drawShadowUnit(backend, unit, cursor, y, color, offsetX, offsetY, unitShadow);
            }
            unitShadow = shadow;
            unit.appendCodePoint(codePoint);
            index = next;
        }
        sfr$drawShadowUnit(backend, unit, cursor, y, color, offsetX, offsetY, unitShadow);
    }

    private float sfr$drawShadowUnit(TextRenderBackend backend, StringBuilder unit, float cursor, float y,
                                     int color, float offsetX, float offsetY, boolean shadow) {
        if (unit.length() == 0) {
            return cursor;
        }
        String text = unit.toString();
        unit.setLength(0);
        TextRenderResult rendered = backend.renderFormatted(text, color, false);
        if (shadow) {
            TextRenderResult shadowText = backend.renderFormatted(text, color, true);
            shadowText.draw(cursor + offsetX, y + offsetY, shadowAlpha(color));
        }
        return cursor + rendered.advance();
    }

    private boolean[] sfr$boldStateByIndex(String text) {
        boolean[] boldAt = new boolean[text.length() + 1];
        boolean bold = false;
        for (int i = 0; i < text.length(); ) {
            boldAt[i] = bold;
            char ch = text.charAt(i);
            if (ch == 167 && i < text.length() - 1) {
                char code = text.charAt(i + 1);
                if (code == 'l' || code == 'L') {
                    bold = true;
                } else if (code == 'r' || code == 'R' || sfr$isFormatColor(code)) {
                    bold = false;
                }
                boldAt[i + 1] = bold;
                i += 2;
                continue;
            }
            int next = i + Character.charCount(text.codePointAt(i));
            for (int pos = i; pos <= next && pos < boldAt.length; pos++) {
                boldAt[pos] = bold;
            }
            i = next;
        }
        boldAt[text.length()] = bold;
        return boldAt;
    }

    private static boolean sfr$isFormatColor(char colorChar) {
        return colorChar >= '0' && colorChar <= '9'
                || colorChar >= 'a' && colorChar <= 'f'
                || colorChar >= 'A' && colorChar <= 'F';
    }
}
