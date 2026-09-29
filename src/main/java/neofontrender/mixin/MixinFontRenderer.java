package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.api.text.paragraph.TextParagraphProvider;
import neofontrender.api.text.paragraph.TextParagraphApi;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.build.BuildFeatures;
import neofontrender.core.font.support.ScopedFontRenderBypass;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.GlyphInfo;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.FontRenderDiagnostics;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

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
    @Shadow private int[] colorCode;
    @Shadow private boolean boldStyle;
    @Shadow protected abstract void setColor(float red, float green, float blue, float alpha);

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
        // Vanilla FontRenderer.drawString enables alpha before any path can render or return.
        // This HEAD injection may cancel the method, so it must preserve that public contract.
        // Reassert the driver state too because raw GL calls from other renderers can leave
        // GlStateManager's cached flag out of sync and make enableAlpha() a no-op.
        GlStateManager.enableAlpha();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        FontRenderTuning.updateFromCurrentGlState(dropShadow);
        if (BuildFeatures.RENDER_STATS) {
            FontRenderDiagnostics.logFontEntry("font.drawString", text, color, dropShadow,
                    FontRenderTuning.currentDrawContext());
        }
        if (!sfr$shouldHook() || text == null) {
            return;
        }
        sfr$syncTextColorPalette();
        color = sfr$resolveEffectiveColor(color);
        TextRenderRouteLayout routed = TextRenderRouteApi.layout(
                (FontRenderer) (Object) this, text, color, dropShadow);
        if (!routed.handled()) return;
        routed.draw(x, y);
        this.posX = x + routed.advance();
        this.posY = y;
        cir.setReturnValue(sfr$drawStringReturnX(x, routed.advance(), dropShadow));
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"), cancellable = true)
    private void sfr$onRenderStringAtPos(String text, boolean shadow, CallbackInfo ci) {
        FontRenderTuning.updateFromCurrentGlState(shadow);
        if (BuildFeatures.RENDER_STATS) {
            FontRenderDiagnostics.logFontEntry("font.renderStringAtPos", text, this.sfr$renderPassColor,
                    shadow, FontRenderTuning.currentDrawContext());
        }
        if (!sfr$shouldHook() || text == null) {
            return;
        }
        sfr$syncTextColorPalette();
        TextRenderRouteLayout routed = TextRenderRouteApi.layout(
                (FontRenderer) (Object) this, text, sfr$currentArgb(), shadow);
        if (!routed.handled()) return;
        routed.draw(this.posX, this.posY);
        this.posX += routed.advance();
        ci.cancel();
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
        if (text == null || ScopedFontRenderBypass.isActive()) {
            return;
        }
        TextRenderRouteLayout routed = TextRenderRouteApi.layout(
                (FontRenderer) (Object) this, text, 0xFFFFFFFF, false);
        if (routed.handled()) cir.setReturnValue((int) Math.ceil(routed.advance()));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void sfr$onTrimStringToWidth(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (text == null || ScopedFontRenderBypass.isActive()) {
            return;
        }
        TextRenderRouteLayout routed = TextRenderRouteApi.layout(
                (FontRenderer) (Object) this, text, 0xFFFFFFFF, false);
        if (routed.handled()) {
            int boundary = reverse ? routed.sourceStartFittingReverse(width)
                    : routed.sourceIndexFitting(width);
            cir.setReturnValue(reverse ? text.substring(boundary) : text.substring(0, boundary));
        }
    }

    @Inject(method = "sizeStringToWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$onSizeStringToWidth(String str, int wrapWidth, CallbackInfoReturnable<Integer> cir) {
        if (str == null || ScopedFontRenderBypass.isActive()) {
            return;
        }
        TextRenderRouteLayout routed = TextRenderRouteApi.layout(
                (FontRenderer) (Object) this, str, 0xFFFFFFFF, false);
        if (routed.handled()) {
            cir.setReturnValue(routed.sizeToWidth(wrapWidth,
                    NeofontrenderConfig.fixCjkLineBreak()));
            return;
        }
    }

    @Inject(method = "drawSplitString", at = @At("HEAD"), cancellable = true)
    private void sfr$drawCjkParagraph(String str, int x, int y, int wrapWidth, int textColor,
                                      CallbackInfo ci) {
        if (str == null) return;
        TextParagraphProvider.Layout paragraph = sfr$layoutCjkParagraph(str, wrapWidth);
        if (paragraph == null) return;
        FontRenderer self = (FontRenderer) (Object) this;
        for (TextParagraphProvider.Line line : paragraph.lines()) {
            for (TextParagraphProvider.Run run : line.runs()) {
                self.drawString(run.formattedText(), x + run.xOffset(),
                        y + line.yOffset(), textColor, false);
            }
        }
        ci.cancel();
    }

    private TextParagraphProvider.Layout sfr$layoutCjkParagraph(String text, int width) {
        if (!NeofontrenderConfig.fixCjkLineBreak()) return null;
        FontRenderer self = (FontRenderer) (Object) this;
        return TextParagraphApi.layout(new TextParagraphProvider.Request(
                text, width, this.FONT_HEIGHT, sfr$currentLanguageCode(), self::getStringWidth));
    }

    private static String sfr$currentLanguageCode() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.getLanguageManager() == null
                || minecraft.getLanguageManager().getCurrentLanguage() == null
                ? "" : minecraft.getLanguageManager().getCurrentLanguage().getLanguageCode();
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
        neofontrender.core.font.pipeline.StructuredTextRuntime.updateColorCodes(sfr$activeColorCodes);
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
        int g = Math.max(0, Math.min(255, Math.round(this.green * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(this.blue * 255.0F)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static float alphaFromColor(int color) {
        if ((color & 0xFC000000) == 0) {
            return 1.0F;
        }
        return (float) (color >>> 24) / 255.0F;
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

}
