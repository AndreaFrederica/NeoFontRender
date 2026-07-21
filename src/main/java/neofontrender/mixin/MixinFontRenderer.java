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
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.BakedGlyph;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.awt.GlyphInfo;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.StringErrorCorrector;
import neofontrender.core.font.skia.SkiaTextSegmenter;

import java.util.List;
import java.util.Locale;

/**
 * Bridges vanilla {@link FontRenderer} into the replacement TTF pipeline.
 */
@Mixin(FontRenderer.class)
public class MixinFontRenderer {

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

    // ================================================================== //
    //  Render hook
    // ================================================================== //

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void sfr$onDrawString(String text, float x, float y, int color, boolean dropShadow,
                                  CallbackInfoReturnable<Integer> cir) {
        FontRenderTuning.updateFromCurrentGlState(dropShadow);
        if (!sfr$shouldHook() || text == null || !FontManager.INSTANCE.isTextBackendActive()
                || !NeofontrenderConfig.skiaAdvancedStringMode()) {
            return;
        }

        GlStateManager.enableAlpha();
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null) {
            return;
        }
        if (NeofontrenderConfig.laboratoryHexChat() && sfr$hasHexColorMarker(text)) {
            if (dropShadow) {
                sfr$drawHexChat(backend, text, x + NeofontrenderConfig.shadowLength(), y + NeofontrenderConfig.shadowLength(), color, true);
            }
            float advance = sfr$drawHexChat(backend, text, x, y, color, false);
            this.posX = x + advance;
            this.posY = y;
            cir.setReturnValue(sfr$drawStringReturnX(x, advance, dropShadow));
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
        if (FontManager.INSTANCE.isTextBackendActive()) {
            TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
            if (backend != null && NeofontrenderConfig.laboratoryHexChat() && sfr$hasHexColorMarker(text)) {
                float advance = sfr$drawHexChat(backend, text, this.posX, this.posY, sfr$currentArgb(), shadow);
                this.posX += advance;
                ci.cancel();
                return;
            }
            if (shadow && backend != null && !backend.shouldRenderShadow(text)) {
                // Vanilla advances the pen during its shadow pass as well. Preserve that state
                // while omitting the duplicate color-glyph rasterization.
                this.posX += backend.measureFormatted(text, sfr$currentArgb(), false);
                ci.cancel();
                return;
            }
            sfr$renderSkiaFormatted(text, shadow);
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
                if (shadow) {
                    colorIndex += 16;
                }
                int color = this.colorCode[colorIndex];
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
        float startX = this.posX;
        float[] positions = fontSet.layoutPositions(run, this.boldStyle);

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
            Minecraft.getMinecraft().getTextureManager().bindTexture(glyph.getTextureLocation());
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            glyph.render(this.italicStyle, x, this.posY, this.red, this.blue, this.green, this.alpha);
            if (this.boldStyle) {
                glyph.render(this.italicStyle, x + 1.0F, this.posY, this.red, this.blue, this.green, this.alpha);
            }
            i = next;
        }

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

    private void sfr$renderSkiaFormatted(String text, boolean shadow) {
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
                sfr$renderSkiaRun(text.substring(runStart, i), corrector);
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
                if (shadow) {
                    colorIndex += 16;
                }
                int color = this.colorCode[colorIndex];
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
            sfr$renderSkiaRun(text.substring(runStart), corrector);
        }
    }

    private void sfr$renderSkiaRun(String run, StringErrorCorrector corrector) {
        if (run.isEmpty()) {
            return;
        }
        float startX = this.posX;
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null) {
            return;
        }
        List<String> segments = this.randomStyle ? null : SkiaTextSegmenter.segment(run);
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
        if (!sfr$isAnyActive() || text == null) {
            return;
        }
        cir.setReturnValue((int) Math.ceil(sfr$getFormattedStringWidthFloat(text)));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void sfr$onTrimStringToWidth(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (!sfr$isAnyActive() || text == null) {
            return;
        }

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
        if (!sfr$isAnyActive() || str == null) {
            return;
        }

        int len = str.length();
        int pos;
        int breakPos = -1;
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
                    width += sfr$getCharWidthFloat(codePoint, bold);
                    pos++;
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
                    width += sfr$getCharWidthFloat(codePoint, bold);
                    pos += Character.charCount(codePoint);
                    break;
            }

            if (width > wrapWidth) {
                break;
            }
        }

        cir.setReturnValue(pos != len && breakPos != -1 && breakPos < pos ? breakPos : pos);
    }

    private float sfr$getStringWidthFloat(String text) {
        return sfr$getFormattedStringWidthFloat(text);
    }

    private float sfr$getFormattedStringWidthFloat(String text) {
        if (FontManager.INSTANCE.isTextBackendActive() && NeofontrenderConfig.skiaAdvancedStringMode()) {
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
            if (NeofontrenderConfig.skiaAdvancedStringMode()) {
                return backend.measureFormatted(run, 0xFFFFFFFF, false);
            }
            List<String> segments = SkiaTextSegmenter.segment(run);
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
        String className = ((Object) this).getClass().getName();
        return !className.equals("net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer")
                && !className.endsWith("SimpleModelFontRenderer");
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
        return Math.max(main, (int) (x + advance + NeofontrenderConfig.shadowLength()));
    }

    private void sfr$drawSelectiveShadow(TextRenderBackend backend, String text, float x, float y, int color) {
        float offset = NeofontrenderConfig.shadowLength();
        if (backend.shouldRenderShadow(text)) {
            TextRenderResult shadow = backend.renderFormatted(text, color, true);
            shadow.draw(x + offset, y + offset, shadowAlpha(color));
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
                cursor = sfr$drawShadowUnit(backend, unit, cursor, y, color, offset, unitShadow);
            }
            unitShadow = shadow;
            unit.appendCodePoint(codePoint);
            index = next;
        }
        sfr$drawShadowUnit(backend, unit, cursor, y, color, offset, unitShadow);
    }

    private float sfr$drawShadowUnit(TextRenderBackend backend, StringBuilder unit, float cursor, float y,
                                     int color, float offset, boolean shadow) {
        if (unit.length() == 0) {
            return cursor;
        }
        String text = unit.toString();
        unit.setLength(0);
        TextRenderResult rendered = backend.renderFormatted(text, color, false);
        if (shadow) {
            TextRenderResult shadowText = backend.renderFormatted(text, color, true);
            shadowText.draw(cursor + offset, y + offset, shadowAlpha(color));
        }
        return cursor + rendered.advance();
    }

    private float sfr$drawHexChat(TextRenderBackend backend, String text, float x, float y, int baseColor, boolean shadow) {
        int color = baseColor | 0xFF000000;
        float cursor = x;
        StringBuilder run = new StringBuilder();
        for (int index = 0; index < text.length();) {
            if (text.charAt(index) == 167 && index + 1 < text.length()) {
                // Chat siblings carry vanilla style controls; they are not visible glyphs here.
                index += 2;
                continue;
            }
            if (isHexMarker(text, index)) {
                cursor = sfr$drawHexRun(backend, run, cursor, y, color, shadow);
                color = (baseColor & 0xFF000000) | Integer.parseInt(text.substring(index + 1, index + 7), 16);
                index += 7;
                continue;
            }
            int codePoint = text.codePointAt(index);
            run.appendCodePoint(codePoint);
            index += Character.charCount(codePoint);
        }
        return sfr$drawHexRun(backend, run, cursor, y, color, shadow) - x;
    }

    private float sfr$drawHexRun(TextRenderBackend backend, StringBuilder run, float x, float y, int color, boolean shadow) {
        if (run.length() == 0) return x;
        String text = run.toString();
        run.setLength(0);
        if (shadow && !backend.shouldRenderShadow(text)) {
            // Masked glyphs (color emoji, bitmap glyphs) get no shadow, matching the
            // selective-shadow behaviour of the non-hex paths. Still advance the pen.
            return x + backend.measure(text, false, false);
        }
        int argb = shadow ? (color & 0xFF000000) | ((color & 0xFCFCFC) >> 2) : color;
        TextRenderResult rendered = backend.render(text, argb, false, false);
        rendered.draw(x, y, shadow ? shadowAlpha(color) : alphaFromColor(color));
        return x + rendered.advance();
    }

    private static boolean sfr$hasHexColorMarker(String text) {
        for (int index = 0; index + 6 < text.length(); index++) {
            if (isHexMarker(text, index)) return true;
        }
        return false;
    }

    private static boolean isHexMarker(String text, int index) {
        if (text.charAt(index) != '#' || index + 6 >= text.length()) return false;
        for (int digit = index + 1; digit <= index + 6; digit++) {
            if (Character.digit(text.charAt(digit), 16) < 0) return false;
        }
        return true;
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
