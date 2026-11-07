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
import neofontrender.core.font.compat.CompatFontEngine;
import neofontrender.core.font.compat.VanillaAsciiSheet;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.FontEngineScope;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.FontRenderDiagnostics;
import neofontrender.core.font.linebreak.ProtocolTextWrapper;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Bridges vanilla {@link FontRenderer} into the replacement TTF pipeline.
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {

    /** Prevents vanilla's recursive wrapping implementation from re-entering our hook. */
    private static final ThreadLocal<Integer> SFR_WRAP_DEPTH = ThreadLocal.withInitial(() -> 0);


    @Shadow public float posX;
    @Shadow public float posY;
    @Shadow public int FONT_HEIGHT;
    @Shadow private float red;
    @Shadow private float blue;
    @Shadow private float green;
    @Shadow private float alpha;
    @Shadow private int[] colorCode;
    @Shadow private int[] charWidth;
    @Shadow private byte[] glyphWidth;
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
        if (sfr$compatMode()) {
            return;
        }
        // Some Forge model helpers reuse FontRenderer to build quads from a
        // worker thread.  Leave those calls on vanilla's thread-safe model path;
        // neither GL state nor the modern text renderer is valid there.
        if (!sfr$isMinecraftThread()) {
            return;
        }
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
        if (!sfr$isMinecraftThread()) {
            return;
        }
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
    //  Compatibility leaf hooks (vanilla owns layout)
    // ================================================================== //

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void nfr$compatBeginText(String text, boolean shadow, CallbackInfo ci) {
        if (!sfr$compatEligible()) return;
        CompatFontEngine engine = FontManager.INSTANCE.getCompatEngineForRendering();
        if (engine == null || !engine.isReady()) return;
        engine.beginTextPass(shadow);
    }

    @Inject(method = "renderStringAtPos", at = @At("RETURN"))
    private void nfr$compatEndText(String text, boolean shadow, CallbackInfo ci) {
        // CompatBlendScope is scoped around each leaf glyph in CompatFontEngine.draw(). Keeping
        // renderStringAtPos free of a mutable single scope prevents nested/re-entrant text calls
        // from overwriting the outer state and leaving a page bound after RETURN.
    }

    @Inject(method = "renderDefaultChar(IZ)F", at = @At("HEAD"), cancellable = true)
    private void nfr$compatRenderDefaultChar(int index, boolean italic,
                                             CallbackInfoReturnable<Float> cir) {
        if (!sfr$compatEligible()) return;
        CompatFontEngine engine = FontManager.INSTANCE.getCompatEngineForRendering();
        if (engine == null || !engine.isReady() || this.charWidth == null
                || index < 0 || index >= this.charWidth.length) {
            return;
        }
        char character = VanillaAsciiSheet.charAt(index);
        if (character == 0) {
            return;
        }
        if (!engine.ensureMetrics((FontRenderer) (Object) this, character,
                this.charWidth, this.glyphWidth)) {
            return;
        }
        if (!engine.draw((FontRenderer) (Object) this, character, italic)) {
            return;
        }
        cir.setReturnValue((float) this.charWidth[index]);
    }

    @Inject(method = "renderUnicodeChar(CZ)F", at = @At("HEAD"), cancellable = true)
    private void nfr$compatRenderUnicodeChar(char character, boolean italic,
                                             CallbackInfoReturnable<Float> cir) {
        if (!sfr$compatEligible()) return;
        CompatFontEngine engine = FontManager.INSTANCE.getCompatEngineForRendering();
        if (engine == null || !engine.isReady() || this.glyphWidth == null) {
            return;
        }
        if (!engine.ensureMetrics((FontRenderer) (Object) this, character,
                this.charWidth, this.glyphWidth)) {
            return;
        }
        if (!engine.draw((FontRenderer) (Object) this, character, italic)) {
            return;
        }
        byte encoded = this.glyphWidth[character];
        if ((encoded & 0xFF) == 0) {
            return;
        }
        cir.setReturnValue(CompatFontEngine.unicodeAdvance(encoded));
    }

    // ================================================================== //
    //  Width hook
    // ================================================================== //

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$onGetCharWidth(char character, CallbackInfoReturnable<Integer> cir) {
        CompatFontEngine compat = FontManager.INSTANCE.getCompatEngineForRendering();
        if (compat != null && compat.isReady() && sfr$compatEligible()) {
            compat.ensureMetrics((FontRenderer) (Object) this, character,
                    this.charWidth, this.glyphWidth);
        }
        if (!sfr$isAnyActive()) {
            return;
        }

        TextRenderBackend modernBackend = FontManager.INSTANCE.getModernTextBackend();
        if (modernBackend != null && modernBackend.isReady()) {
            FontRenderTuning.updateFromCurrentGlState(false);
            try (FontRenderTuning.RasterScaleScope ignored = FontRenderTuning.pinRasterScale(
                    FontRenderTuning.effectiveRasterScale(NeofontrenderConfig.fontOversample()))) {
                char normalized = character == 160 ? ' ' : character;
                if (normalized == 167) {
                    cir.setReturnValue(-1);
                } else if (Character.isHighSurrogate(normalized)
                        || Character.isLowSurrogate(normalized)) {
                    // FontRenderer receives UTF-16 code units here; the paired code point is
                    // measured by getStringWidth instead of charging either half separately.
                    cir.setReturnValue(0);
                } else {
                    String value = String.valueOf(normalized);
                    cir.setReturnValue((int) Math.ceil(modernBackend.measure(
                            value, this.boldStyle, false)));
                }
            }
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
        if (sfr$compatMode()) return;
        if (text == null || ScopedFontRenderBypass.isActive()) {
            return;
        }
        TextRenderRouteLayout routed = TextRenderRouteApi.layout(
                (FontRenderer) (Object) this, text, 0xFFFFFFFF, false);
        if (routed.handled()) cir.setReturnValue((int) Math.ceil(routed.advance()));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void sfr$onTrimStringToWidth(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (sfr$compatMode()) return;
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
        if (sfr$compatMode()) return;
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

    @Inject(method = "wrapFormattedStringToWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$wrapExtendedColors(String text, int width, CallbackInfoReturnable<String> cir) {
        if (sfr$compatMode()) return;
        if (text == null || ScopedFontRenderBypass.isActive()
                || SFR_WRAP_DEPTH.get() > 0) return;
        if (!ProtocolTextWrapper.enabled(ProtocolTextWrapper.Scope.OTHER)) return;
        if (!ProtocolTextWrapper.hasActiveProtocol(text)) return;
        SFR_WRAP_DEPTH.set(1);
        try {
            cir.setReturnValue(String.join("\n", ProtocolTextWrapper.wrap(
                    (FontRenderer) (Object) this, text, width, true)));
        } finally {
            SFR_WRAP_DEPTH.remove();
        }
    }

    /**
     * The raw implementation of this method slices the source with an index that was measured
     * on a different prefix, so a color protocol can be cut in half and its tail renders as
     * literal markup. Rebuild every line from the parsed draw state instead.
     */
    @Inject(method = "listFormattedStringToWidth", at = @At("HEAD"), cancellable = true)
    private void sfr$listProtocolAware(String text, int wrapWidth,
                                       CallbackInfoReturnable<List<String>> cir) {
        if (text == null || SFR_WRAP_DEPTH.get() > 0) return;
        if (!sfr$isAnyActive()) return;
        if (!ProtocolTextWrapper.enabled(ProtocolTextWrapper.Scope.OTHER)) return;
        if (!ProtocolTextWrapper.hasActiveProtocol(text)) return;
        SFR_WRAP_DEPTH.set(1);
        try {
            cir.setReturnValue(ProtocolTextWrapper.wrap(
                    (FontRenderer) (Object) this, text, wrapWidth, false));
        } finally {
            SFR_WRAP_DEPTH.remove();
        }
    }

    @Inject(method = "drawSplitString", at = @At("HEAD"), cancellable = true)
    private void sfr$drawCjkParagraph(String str, int x, int y, int wrapWidth, int textColor,
                                      CallbackInfo ci) {
        if (sfr$compatMode()) return;
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

    private boolean sfr$isAnyActive() {
        return sfr$shouldHook() && (FontManager.INSTANCE.isSfrActive() || FontManager.INSTANCE.isTextBackendActive());
    }

    /** True when vanilla owns layout and the compat leaf hooks supply glyphs. */
    private boolean sfr$compatMode() {
        FontEngineScope.Mode scope = FontEngineScope.current();
        if (scope == FontEngineScope.Mode.VANILLA || scope == FontEngineScope.Mode.COSMIC) return false;
        if (scope == FontEngineScope.Mode.COMPAT) return true;
        return NeofontrenderConfig.useCompatEngine();
    }

    private boolean sfr$shouldHook() {
        if (!sfr$isMinecraftThread()) return false;
        if (FontEngineScope.current() == FontEngineScope.Mode.VANILLA) return false;
        if (sfr$compatMode()) return false;
        if (ScopedFontRenderBypass.isActive()) return false;
        String className = ((Object) this).getClass().getName();
        return !className.equals("net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer")
                && !className.endsWith("SimpleModelFontRenderer");
    }

    /** Compat keeps vanilla layout but still must not take over splash/model font renderers. */
    private boolean sfr$compatEligible() {
        if (!sfr$isMinecraftThread()) return false;
        FontEngineScope.Mode scope = FontEngineScope.current();
        if (scope == FontEngineScope.Mode.VANILLA || scope == FontEngineScope.Mode.COSMIC) return false;
        if (scope == FontEngineScope.Mode.INHERIT && !NeofontrenderConfig.useCompatEngine()) return false;
        String className = ((Object) this).getClass().getName();
        return !className.equals("net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer")
                && !className.endsWith("SimpleModelFontRenderer");
    }

    private static boolean sfr$isMinecraftThread() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && minecraft.isCallingFromMinecraftThread();
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
