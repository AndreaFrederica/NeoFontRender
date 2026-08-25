package neofontrender.core.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraft.client.Minecraft;
import neofontrender.NeoFontRender;
import neofontrender.api.color.TextColorPaletteCodec;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.core.font.support.FontFileResolver;
import neofontrender.core.font.support.ShadowColorRemapRules;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * TOML-based configuration manager using NightConfig.
 */
public final class NeofontrenderConfig {

    private static final String CONFIG_NAME = "neofontrender.toml";
    private static final String DEFAULT_FONT_NAME = "Noto Sans SC";
    private static final String DEFAULT_FONT = "neofontrender:fonts/noto_sans_sc-regular.otf";
    private static final String ENCHANTMENT_FONT = "neofontrender:fonts/sric_minecraft_words.ttf";
    private static final String DEFAULT_TEXT_COLOR_PALETTE =
            "000000,0000AA,00AA00,00AAAA,AA0000,AA00AA,FFAA00,AAAAAA,"
                    + "555555,5555FF,55FF55,55FFFF,FF5555,FF55FF,FFFF55,FFFFFF";
    private static Path configPath;
    private static volatile CommentedFileConfig config;
    private static volatile boolean loaded;
    private static volatile boolean earlyLoadFailed;
    private static volatile Snapshot cached = Snapshot.defaults();
    private static volatile boolean cachedDebugRenderStats;
    private static final List<BuiltinFont> BUILTIN_FONTS = Collections.unmodifiableList(Arrays.asList(
            new BuiltinFont("Noto Sans SC", DEFAULT_FONT),
            new BuiltinFont("Noto Color Emoji", "neofontrender:fonts/noto_color_emoji_regular.ttf")
    ));

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Loads the real configuration for renderers that can run before Forge mod initialization.
     * A failed early load leaves the immutable default snapshot active so startup rendering can
     * continue safely; the normal client lifecycle may still retry through {@link #load()}.
     */
    public static boolean ensureLoadedForEarlyRendering() {
        if (loaded) {
            return true;
        }
        if (earlyLoadFailed) {
            return false;
        }
        synchronized (NeofontrenderConfig.class) {
            if (loaded) {
                return true;
            }
            if (earlyLoadFailed) {
                return false;
            }
            try {
                load();
                return loaded;
            } catch (Throwable t) {
                config = null;
                loaded = false;
                earlyLoadFailed = true;
                NeoFontRender.LOGGER.error(
                        "Failed to load configuration during early rendering; using defaults", t);
                return false;
            }
        }
    }

    // ===================== Font =====================
    public static String fontName() {
        String name = config.getOrElse("font.name", DEFAULT_FONT_NAME);
        name = name == null ? "" : name.trim();
        return name.isEmpty() ? DEFAULT_FONT_NAME : name;
    }

    /** Optional byte source for the primary font. The family name is always kept separately. */
    public static String fontPath() {
        return normalizeFontLocation(config.getOrElse("font.path", ""));
    }

    /** Selector used by byte-loading backends. */
    public static String primaryFontLocation() {
        String path = fontPath();
        return path.isEmpty() ? fontName() : path;
    }

    public static List<String> fontFamily() {
        Set<String> fonts = new LinkedHashSet<>();
        addFontNames(fonts, primaryFontLocation());
        fonts.addAll(fontFallbacks());
        if (builtinFallbacksEnabled()) {
            for (BuiltinFont font : builtinFonts()) {
                fonts.add(font.location);
            }
        }
        if (fonts.isEmpty()) {
            fonts.add(DEFAULT_FONT);
        }
        return Collections.unmodifiableList(new ArrayList<>(fonts));
    }

    public static List<String> fontFallbacks() {
        Set<String> fonts = new LinkedHashSet<>();
        Object fallbackValue = config.get("font.fallbacks");
        if (fallbackValue instanceof List) {
            for (Object value : (List<?>) fallbackValue) {
                if (value != null) {
                    addFontNames(fonts, value.toString());
                }
            }
        } else if (fallbackValue != null) {
            addFontNames(fonts, fallbackValue.toString());
        }
        String primaryName = fontName();
        fonts.removeIf(font -> font.equalsIgnoreCase(primaryName));
        return Collections.unmodifiableList(new ArrayList<>(fonts));
    }

    public static int fontStyle() {
        return cached.fontStyle;
    }

    public static int fontVariableWeight() {
        return cached.fontVariableWeight;
    }

    public static String cosmicRegularFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.regular", ""));
    }

    public static String cosmicBoldFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.bold", ""));
    }

    public static String cosmicItalicFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.italic", ""));
    }

    public static String cosmicBoldItalicFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.boldItalic", ""));
    }

    public static boolean cosmicVariantOverridesOnlySwitchFont() {
        return cached.cosmicVariantOverridesOnlySwitchFont;
    }

    public static List<String> cosmicFaceOverrides() {
        return Collections.unmodifiableList(Arrays.asList(
                cosmicRegularFont(), cosmicBoldFont(), cosmicItalicFont(), cosmicBoldItalicFont()));
    }

    public static float fontSize() {
        return cached.fontSize;
    }

    public static boolean adaptiveFontSizeEnabled() {
        return cached.adaptiveFontSize;
    }

    /**
     * Compute the effective font size considering the current GUI scale.
     * When adaptiveFontSize is enabled, the font size is multiplied by the GUI scale factor
     * to ensure consistent visual size across different GUI scale settings.
     *
     * @return the effective font size in pixels
     */
    public static float adaptiveFontSize() {
        float baseSize = cached.fontSize;
        if (!cached.adaptiveFontSize) {
            return baseSize;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.gameSettings != null) {
                int guiScale = mc.gameSettings.guiScale;
                if (guiScale <= 0) {
                    guiScale = 2; // default when auto
                }
                // Scale factor: guiScale 1 = 1x, guiScale 2 = 1.5x, guiScale 3 = 2x, guiScale 4 = 2.5x
                float scaleFactor = 1.0F + (guiScale - 1) * 0.5F;
                return baseSize * scaleFactor;
            }
        } catch (Exception ignored) {
        }
        return baseSize;
    }

    public static float fontOversample() {
        return cached.fontOversample;
    }

    public static boolean fontAutoBaseline() {
        return cached.fontAutoBaseline;
    }

    public static float fontBaselineShift() {
        return cached.fontBaselineShift;
    }

    public static float fontReferenceBaseline() {
        return cached.fontReferenceBaseline;
    }

    public static boolean fontAntialias() {
        return cached.fontAntialias;
    }

    public static String fontAntialiasMode() {
        return cached.fontAntialiasMode;
    }

    public static boolean fontFractionalMetrics() {
        return cached.fontFractionalMetrics;
    }

    public static boolean fontLcdSubpixel() {
        return cached.fontLcdSubpixel;
    }

    public static boolean builtinFallbacksEnabled() {
        return cached.builtinFallbacks;
    }

    public static List<BuiltinFont> builtinFonts() {
        return BUILTIN_FONTS;
    }

    // ===================== Shadow =====================
    public static float shadowLength() {
        return cached.shadowLength;
    }

    public static boolean modernShadowEnabled() { return cached.modernShadow; }
    public static float shadowOffsetX() { return cached.shadowOffsetX; }
    public static float shadowOffsetY() { return cached.shadowOffsetY; }
    public static float shadowBlurRadius() { return cached.shadowBlurRadius; }
    public static int shadowColor() { return cached.shadowColor; }
    public static boolean coloredShadowEnabled() { return cached.coloredShadow; }
    public static ShadowColorRemapRules shadowColorRemapRules() { return cached.shadowColorRemapRules; }
    public static String shadowColorRemapRulesConfig() {
        return cached.shadowColorRemapRules.toConfigString();
    }

    public static float shadowOpacity() {
        return cached.shadowOpacity;
    }

    /** all, mask, emoji, or none. */
    public static String shadowMode() {
        return cached.shadowMode;
    }

    public static String shadowMaskFonts() { return cached.shadowMaskFonts; }
    public static String shadowMaskCodepoints() { return cached.shadowMaskCodepoints; }

    // ===================== Rendering =====================
    public static String renderingEngine() {
        return cached.renderingEngine;
    }

    public static boolean useSfrEngine() {
        return enabled() && "sfr".equals(renderingEngine());
    }

    public static boolean useAwtEngine() {
        return enabled() && ("sfr".equals(renderingEngine()) || "awt".equals(renderingEngine()));
    }

    public static boolean useCosmicEngine() {
        return enabled() && "cosmic".equals(renderingEngine());
    }

    public static boolean advancedStringMode() {
        return cached.advancedStringMode;
    }

    public static boolean segmentCache() {
        return cached.segmentCache;
    }

    public static int segmentCacheMinRunLength() {
        return cached.segmentCacheMinRunLength;
    }

    public static int segmentCacheMaxRunCodePoints() {
        return cached.segmentCacheMaxRunCodePoints;
    }

    public static int segmentCacheMaxSegments() {
        return cached.segmentCacheMaxSegments;
    }

    public static boolean useVanillaEngine() {
        return !enabled() || "vanilla".equals(renderingEngine());
    }

    public static boolean renderingInterpolation() {
        return cached.renderingInterpolation;
    }

    public static boolean renderingMipmap() {
        return cached.renderingMipmap;
    }

    public static boolean adaptiveRasterScale() {
        return cached.adaptiveRasterScale;
    }

    public static float adaptiveRasterMin() {
        return cached.adaptiveRasterMin;
    }

    public static float adaptiveRasterMax() {
        return cached.adaptiveRasterMax;
    }

    public static float adaptiveRasterStep() {
        return cached.adaptiveRasterStep;
    }

    public static boolean excludeIntegerScale() {
        return cached.excludeIntegerScale;
    }

    public static boolean excludeHighMagnification() {
        return cached.excludeHighMagnification;
    }

    public static float limitMagnification() {
        return cached.limitMagnification;
    }

    public static float scaleRoundingToleranceRate() {
        return cached.scaleRoundingToleranceRate;
    }

    public static float mipmapLodBias() {
        return cached.mipmapLodBias;
    }

    public static float overlayMipmapLodBias() {
        return cached.overlayMipmapLodBias;
    }

    public static boolean anisotropicFiltering() {
        return cached.anisotropicFiltering;
    }

    public static float blurReductionThreshold() {
        return cached.blurReductionThreshold;
    }

    public static float smoothShadowThreshold() {
        return cached.smoothShadowThreshold;
    }

    public static boolean enhancedTextPipeline() {
        return cached.enhancedTextPipeline;
    }

    public static boolean shaderTextPipeline() {
        return cached.shaderTextPipeline;
    }

    public static float renderingBrightness() {
        return cached.renderingBrightness;
    }

    public static boolean renderingBrightnessAuto() {
        return cached.renderingBrightnessAuto;
    }

    /**
     * Force GL_BLEND enabled for anti-aliased replacement text. Minecraft disables blending in
     * several bitmap-font paths even though replacement glyph edges contain fractional alpha.
     */
    public static boolean forceBlendForText() {
        return cached.forceBlendForText;
    }

    // ===================== Performance =====================
    public static boolean performanceAsyncInit() {
        return cached.performanceAsyncInit;
    }

    public static boolean performancePrewarmBasicLatin() {
        return cached.performancePrewarmBasicLatin;
    }

    public static boolean signTextLodCulling() {
        return cached.signTextLodCulling;
    }

    public static float signTextMinPixelHeight() {
        return cached.signTextMinPixelHeight;
    }

    public static boolean signTextFrustumCulling() {
        return cached.signTextFrustumCulling;
    }

    public static boolean signModelLod() {
        return cached.signModelLod;
    }

    public static float signModelLodDistance() {
        return cached.signModelLodDistance;
    }

    public static boolean signBlockOcclusionCulling() {
        return cached.signBlockOcclusionCulling;
    }

    public static int signOcclusionChecksPerFrame() {
        return cached.signOcclusionChecksPerFrame;
    }

    public static long signOcclusionCacheMillis() {
        return cached.signOcclusionCacheMillis;
    }

    public static float signOcclusionMinDistance() {
        return cached.signOcclusionMinDistance;
    }

    public static int textCacheMinEntries() {
        return cached.textCacheMinEntries;
    }

    public static int textCacheMaxEntries() {
        return cached.textCacheMaxEntries;
    }

    public static float textCacheTtlSeconds() {
        return cached.textCacheTtlSeconds;
    }

    public static int measureCacheMaxEntries() {
        return cached.measureCacheMaxEntries;
    }

    // ===================== General =====================
    public static boolean enabled() {
        return cached.enabled;
    }

    public static boolean fixImeInput() {
        return cached.fixImeInput;
    }

    public static boolean debugImeInput() {
        return cached.debugImeInput;
    }

    public static boolean debugRenderStats() {
        return cachedDebugRenderStats;
    }

    public static boolean allowSignPaste() {
        return cached.allowSignPaste;
    }

    public static boolean fixUnicodeTextDeletion() {
        return cached.fixUnicodeTextDeletion;
    }

    public static boolean fixCjkLineBreak() {
        return cached.fixCjkLineBreak;
    }

    public static boolean laboratoryHexChat() {
        return cached.laboratoryHexChat;
    }

    public static boolean laboratoryHexChatResetStyles() {
        return cached.laboratoryHexChatResetStyles;
    }

    public static boolean laboratoryTextUndoRedo() {
        return cached.laboratoryTextUndoRedo;
    }

    public static boolean compatModernSplash() {
        return cached.compatModernSplash;
    }

    public static boolean compatTinkersAntique() {
        return cached.compatTinkersAntique;
    }

    public static boolean compatThaumcraftTooltip() {
        return cached.compatThaumcraftTooltip;
    }

    /** auto, vanilla, runtime, custom, or an API-registered provider id. */
    public static String textColorPaletteProvider() {
        CommentedFileConfig current = config;
        if (!loaded || current == null) {
            return TextColorPaletteRegistry.AUTO;
        }
        return TextColorPaletteRegistry.normalizeSelection(
                current.getOrElse("compat.colorPalette.provider", TextColorPaletteRegistry.AUTO));
    }

    /** Editable comma-separated 16/32-entry RRGGBB palette. */
    public static String customTextColorPalette() {
        String value = config.getOrElse("compat.colorPalette.custom", DEFAULT_TEXT_COLOR_PALETTE);
        return value == null || value.trim().isEmpty() ? DEFAULT_TEXT_COLOR_PALETTE : value.trim();
    }

    public static int[] customTextColorCodes() {
        return TextColorPaletteCodec.parse(customTextColorPalette());
    }

    /** vanilla, auto, awt, or cosmic. Only the enchanting-table magic text consumes this. */
    public static String enchantmentFontBackend() {
        String value = config.getOrElse("enchantment.backend", "awt");
        value = value == null ? "awt" : value.trim().toLowerCase(Locale.ROOT);
        return Arrays.asList("vanilla", "auto", "awt", "cosmic").contains(value) ? value : "awt";
    }

    public static List<String> enchantmentFonts() {
        Set<String> fonts = new LinkedHashSet<>();
        Object value = config.get("enchantment.fonts");
        if (value instanceof List) for (Object entry : (List<?>) value) {
            if (entry != null) addFontNames(fonts, entry.toString());
        } else if (value != null) addFontNames(fonts, value.toString());
        if (fonts.isEmpty()) fonts.add(ENCHANTMENT_FONT);
        return Collections.unmodifiableList(new ArrayList<>(fonts));
    }

    public static boolean splashFontOverrideEnabled() {
        return cached.splashFontOverrideEnabled;
    }

    public static void setEnabled(boolean value) {
        setValue("enabled", value);
    }

    public static void setFontName(String value) {
        String name = value == null ? "" : value.trim();
        if (isFontFileLocation(name)) {
            File file = resolveFontFile(name);
            setValue("font.path", normalizeSingleFont(name));
            name = inferFontFamily(file, name);
        } else {
            // The legacy setter selects a complete primary font. A family-only selection must not
            // accidentally retain the byte source belonging to the previously selected font.
            setValue("font.path", "");
        }
        setValue("font.name", name.isEmpty() ? DEFAULT_FONT_NAME : name);
    }

    public static void setFontPath(String value) {
        setValue("font.path", normalizeSingleFont(value));
    }

    public static void setFontFallbacks(List<String> value) {
        List<String> fallbacks = value == null
                ? new ArrayList<>()
                : new ArrayList<>(normalizeFontValues(value));
        String primaryName = fontName();
        fallbacks.removeIf(font -> font.equalsIgnoreCase(primaryName));
        setValue("font.fallbacks", fallbacks);
    }

    public static void setFontStyle(int value) {
        setValue("font.style", value);
    }

    public static void setFontVariableWeight(int value) {
        setValue("font.variableWeight", Math.max(0, Math.min(1000, value)));
    }

    public static void setCosmicRegularFont(String value) {
        setValue("font.cosmic.regular", normalizeSingleFont(value));
    }

    public static void setCosmicBoldFont(String value) {
        setValue("font.cosmic.bold", normalizeSingleFont(value));
    }

    public static void setCosmicItalicFont(String value) {
        setValue("font.cosmic.italic", normalizeSingleFont(value));
    }

    public static void setCosmicBoldItalicFont(String value) {
        setValue("font.cosmic.boldItalic", normalizeSingleFont(value));
    }

    public static void setCosmicVariantOverridesOnlySwitchFont(boolean value) {
        setValue("font.cosmic.variantOverridesOnlySwitchFont", value);
    }

    public static void setFontSize(float value) {
        setValue("font.size", value);
    }

    public static void setAdaptiveFontSize(boolean value) {
        setValue("font.adaptiveSize", value);
    }

    public static void setFontOversample(float value) {
        setValue("font.oversample", value);
    }

    public static void setFontAutoBaseline(boolean value) {
        setValue("font.autoBaseline", value);
    }

    public static void setFontBaselineShift(float value) {
        setValue("font.baselineShift", value);
    }

    public static void setFontReferenceBaseline(float value) {
        setValue("font.referenceBaseline", value);
    }

    public static void setFontAntialias(boolean value) {
        setValue("font.antialias", value);
    }

    public static void setFontAntialiasMode(String value) {
        String mode = normalizeAntialiasMode(value);
        setValue("font.antialiasMode", mode);
        setValue("font.antialias", !"off".equals(mode));
    }

    public static void setFontFractionalMetrics(boolean value) {
        setValue("font.fractionalMetrics", value);
    }

    public static void setFontLcdSubpixel(boolean value) {
        setValue("font.lcdSubpixel", value);
    }

    public static void setBuiltinFallbacksEnabled(boolean value) {
        setValue("font.builtinFallbacks", value);
    }

    public static void setShadowLength(float value) {
        setValue("shadow.length", value);
    }

    public static void setModernShadowEnabled(boolean value) { setValue("shadow.modern", value); }
    public static void setShadowOffsetX(float value) { setValue("shadow.offsetX", Math.max(-8.0F, Math.min(8.0F, value))); }
    public static void setShadowOffsetY(float value) { setValue("shadow.offsetY", Math.max(-8.0F, Math.min(8.0F, value))); }
    public static void setShadowBlurRadius(float value) { setValue("shadow.blurRadius", Math.max(0.0F, Math.min(6.0F, value))); }
    public static void setShadowColor(int value) { setValue("shadow.color", value); }
    public static void setColoredShadowEnabled(boolean value) { setValue("shadow.colored", value); }
    public static void setShadowColorRemapRules(String value) {
        setValue("shadow.coloredRemapRules",
                ShadowColorRemapRules.parse(value).toConfigString());
    }

    public static void setShadowOpacity(float value) {
        setValue("shadow.opacity", value);
    }

    public static void setShadowMode(String value) {
        setValue("shadow.mode", normalizeShadowMode(value));
    }
    public static void setShadowMaskFonts(String value) {
        setValue("shadow.maskFonts", joinFontLocations(normalizeFontValues(
                value == null ? Collections.emptyList() : Collections.singletonList(value))));
    }
    public static void setShadowMaskCodepoints(String value) { setValue("shadow.maskCodepoints", value == null ? "" : value.trim()); }

    public static void setFixImeInput(boolean value) {
        setValue("fixImeInput", value);
    }

    public static void setAllowSignPaste(boolean value) {
        setValue("input.allowSignPaste", value);
    }

    public static void setFixUnicodeTextDeletion(boolean value) {
        setValue("input.fixUnicodeTextDeletion", value);
    }

    public static void setFixCjkLineBreak(boolean value) {
        setValue("fix.cjkLineBreak", value);
    }

    public static void setLaboratoryHexChat(boolean value) {
        setValue("laboratory.hexChat", value);
    }

    public static void setLaboratoryHexChatResetStyles(boolean value) {
        setValue("laboratory.hexChatResetStyles", value);
    }

    public static void setLaboratoryTextUndoRedo(boolean value) {
        setValue("laboratory.textUndoRedo", value);
    }

    public static void setCompatModernSplash(boolean value) {
        setValue("compat.modernsplash.enabled", value);
    }

    public static void setCompatTinkersAntique(boolean value) {
        setValue("compat.tinkersantique.enabled", value);
    }

    public static void setCompatThaumcraftTooltip(boolean value) {
        setValue("compat.thaumcraft.tooltip.enabled", value);
    }

    public static void setTextColorPaletteProvider(String value) {
        setValue("compat.colorPalette.provider",
                TextColorPaletteRegistry.normalizeSelection(value));
    }

    public static void setCustomTextColorPalette(String value) {
        setValue("compat.colorPalette.custom", TextColorPaletteCodec.format(
                TextColorPaletteCodec.parse(value)));
    }

    public static void setCustomTextColorCodes(int[] colorCodes) {
        if (colorCodes == null || (colorCodes.length != 16 && colorCodes.length != 32)) {
            throw new IllegalArgumentException("Custom palette must contain 16 or 32 colors");
        }
        setValue("compat.colorPalette.custom", TextColorPaletteCodec.format(colorCodes));
    }

    public static void setEnchantmentFontBackend(String value) {
        String backend = value == null ? "vanilla" : value.trim().toLowerCase(Locale.ROOT);
        setValue("enchantment.backend", Arrays.asList("vanilla", "auto", "awt", "cosmic").contains(backend)
                ? backend : "vanilla");
    }

    public static void setEnchantmentFonts(List<String> value) {
        List<String> fonts = value == null ? new ArrayList<>() : new ArrayList<>(normalizeFontValues(value));
        if (fonts.isEmpty()) fonts.add(ENCHANTMENT_FONT);
        setValue("enchantment.fonts", fonts);
    }

    public static void setSplashFontOverrideEnabled(boolean value) {
        setValue("splash.enabled", value);
    }

    public static void setRenderingInterpolation(boolean value) {
        setValue("rendering.interpolation", value);
    }

    public static void setRenderingMipmap(boolean value) {
        setValue("rendering.mipmap", value);
    }

    public static void setAdaptiveRasterScale(boolean value) {
        setValue("rendering.adaptiveRasterScale", value);
    }

    public static void setAdaptiveRasterMin(float value) {
        setValue("rendering.adaptiveRasterMin", value);
    }

    public static void setAdaptiveRasterMax(float value) {
        setValue("rendering.adaptiveRasterMax", value);
    }

    public static void setAdaptiveRasterStep(float value) {
        setValue("rendering.adaptiveRasterStep", value);
    }

    public static void setExcludeIntegerScale(boolean value) {
        setValue("rendering.excludeIntegerScale", value);
    }

    public static void setExcludeHighMagnification(boolean value) {
        setValue("rendering.excludeHighMagnification", value);
    }

    public static void setLimitMagnification(float value) {
        setValue("rendering.limitMagnification", value);
    }

    public static void setScaleRoundingTolerance(float value) {
        setValue("rendering.scaleRoundingTolerance", value);
    }

    public static void setMipmapLodBias(float value) {
        setValue("rendering.mipmapLodBias", value);
    }

    public static void setOverlayMipmapLodBias(float value) {
        setValue("rendering.overlayMipmapLodBias", value);
    }

    public static void setAnisotropicFiltering(boolean value) {
        setValue("rendering.anisotropicFiltering", value);
    }

    public static void setBlurReductionThreshold(float value) {
        setValue("rendering.blurReduction", value);
    }

    public static void setSmoothShadowThreshold(float value) {
        setValue("rendering.smoothShadowThreshold", value);
    }

    public static void setEnhancedTextPipeline(boolean value) {
        setValue("rendering.enhancedTextPipeline", value);
    }

    public static void setShaderTextPipeline(boolean value) {
        setValue("rendering.shaderTextPipeline", value);
    }

    public static void setRenderingBrightness(float value) {
        setValue("rendering.brightness", value);
    }

    public static void setRenderingBrightnessAuto(boolean value) {
        setValue("rendering.brightnessAuto", value);
    }

    public static void setForceBlendForText(boolean value) {
        setValue("rendering.forceBlendForText", value);
    }

    public static void setRenderingEngine(String value) {
        setValue("rendering.engine", normalizeRenderingEngine(value));
    }

    public static void setAdvancedStringMode(boolean value) {
        setValue("rendering.advancedStringMode", value);
    }

    public static void setSegmentCache(boolean value) {
        setValue("rendering.segmentCache", value);
    }

    public static void setSegmentCacheMinRunLength(int value) {
        setValue("rendering.segmentCacheMinRunLength", value);
    }

    public static void setSegmentCacheMaxRunCodePoints(int value) {
        setValue("rendering.segmentCacheMaxRunCodePoints", value);
    }

    public static void setSegmentCacheMaxSegments(int value) {
        setValue("rendering.segmentCacheMaxSegments", value);
    }

    public static void setPerformanceAsyncInit(boolean value) {
        setValue("performance.asyncInit", value);
    }

    public static void setPerformancePrewarmBasicLatin(boolean value) {
        setValue("performance.prewarmBasicLatin", value);
    }

    public static void setSignModelLod(boolean value) {
        setValue("performance.signModelLod", value);
    }

    public static void setSignBlockOcclusionCulling(boolean value) {
        setValue("performance.signBlockOcclusionCulling", value);
    }

    public static void setTextCacheMinEntries(int value) {
        setValue("performance.textCacheMinEntries", value);
    }

    public static void setTextCacheMaxEntries(int value) {
        setValue("performance.textCacheMaxEntries", value);
    }

    public static void setTextCacheTtlSeconds(float value) {
        setValue("performance.textCacheTtlSeconds", value);
    }

    public static void setMeasureCacheMaxEntries(int value) {
        setValue("performance.measureCacheMaxEntries", value);
    }

    public static void setDebugRenderStats(boolean value) {
        cachedDebugRenderStats = value;
        setValue("debug.renderStats", value);
    }

    public static void save() {
        if (config != null) {
            config.save();
        }
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized Object getExtensionValue(String key, Object defaultValue) {
        ensureLoadedForExtensionApi();
        return config.getOrElse(key, defaultValue);
    }

    public static synchronized boolean hasExtensionValue(String key) {
        ensureLoadedForExtensionApi();
        return config.contains(key);
    }

    public static synchronized Object removeExtensionValue(String key) {
        ensureLoadedForExtensionApi();
        return config.remove(key);
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized void setExtensionValue(String key, Object value) {
        ensureLoadedForExtensionApi();
        config.set(key, value);
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized void setExtensionComment(String key, String comment) {
        ensureLoadedForExtensionApi();
        config.setComment(key, comment);
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized void saveExtensionValues() {
        ensureLoadedForExtensionApi();
        config.save();
    }

    private static void ensureLoadedForExtensionApi() {
        if (!loaded) load();
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        if (configPath == null) {
            configPath = new File(Minecraft.getMinecraft().gameDir, "config" + File.separator + CONFIG_NAME).toPath();
        }

        File configFile = configPath.toFile();
        boolean needsDefault = !configFile.exists();

        if (needsDefault) {
            try {
                Files.createDirectories(configPath.getParent());
                writeDefaultConfig(configFile);
            } catch (IOException e) {
                NeoFontRender.LOGGER.error("Failed to create default config", e);
            }
        }

        CommentedFileConfig loadedConfig = CommentedFileConfig.builder(configPath, TomlFormat.instance())
                .preserveInsertionOrder()
                .build();
        loadedConfig.load();
        config = loadedConfig;

        boolean migratedFontLocations = migratePortableFontLocations();
        if (needsDefault) {
            addComments();
        }
        if (needsDefault || migratedFontLocations) {
            config.save();
        }
        refreshCachedOptions();
        ensureFontDirectory();
        earlyLoadFailed = false;
        loaded = true;
    }

    private static void refreshCachedOptions() {
        cached = Snapshot.from(config);
        cachedDebugRenderStats = cached.debugRenderStats;
        TextColorPaletteRegistry.setCustomColorCodes(TextColorPaletteCodec.parse(
                config.getOrElse("compat.colorPalette.custom", DEFAULT_TEXT_COLOR_PALETTE)));
    }

    private static void setValue(String key, Object value) {
        config.set(key, value);
        refreshCachedOptions();
    }

    public static File fontDirectory() {
        return new File(Minecraft.getMinecraft().gameDir, "neofontrender" + File.separator + "fonts");
    }

    public static File ensureFontDirectory() {
        File dir = fontDirectory();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            NeoFontRender.LOGGER.warn("Failed to create font directory '{}'", dir);
        }
        return dir;
    }

    public static String portableFontLocation(File file) {
        return FontFileResolver.portableLocation(Minecraft.getMinecraft().gameDir, file);
    }

    public static File resolveFontFile(String location) {
        return FontFileResolver.resolve(Minecraft.getMinecraft().gameDir, location);
    }

    public static List<File> primaryFamilyFiles() {
        return fontFamilyFiles(fontName());
    }

    /** Finds every local face belonging to a configured font family. */
    public static List<File> fontFamilyFiles(String family) {
        return FontFileResolver.familyFiles(Minecraft.getMinecraft().gameDir, family);
    }

    public static String fontFamilyName(File file) {
        return FontFileResolver.familyName(file);
    }

    public static String fontFaceName(File file) {
        return FontFileResolver.faceName(file);
    }

    private static void writeDefaultConfig(File file) throws IOException {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.write("# Revo Font Configuration\n");
            w.write("\n");
            w.write("enabled = true\n");
            w.write("\n");
            w.write("[font]\n");
            w.write("name = \"" + DEFAULT_FONT_NAME + "\"\n");
            w.write("path = \"" + DEFAULT_FONT + "\"\n");
            w.write("fallbacks = [\"Serif\", \"Monospaced\"]\n");
            w.write("style = 0\n");
            w.write("variableWeight = 0\n");
            w.write("size = 8.5\n");
            w.write("oversample = 12.0\n");
            w.write("autoBaseline = true\n");
            w.write("baselineShift = 0.0\n");
            w.write("referenceBaseline = 7.0\n");
            w.write("antialias = true\n");
            w.write("antialiasMode = \"on\"\n");
            w.write("fractionalMetrics = true\n");
            w.write("lcdSubpixel = false\n");
            w.write("builtinFallbacks = true\n");
            w.write("\n");
            w.write("[font.cosmic]\n");
            w.write("regular = \"\"\n");
            w.write("bold = \"\"\n");
            w.write("italic = \"\"\n");
            w.write("boldItalic = \"\"\n");
            w.write("variantOverridesOnlySwitchFont = false\n");
            w.write("\n");
            w.write("[shadow]\n");
            w.write("modern = true\n");
            w.write("length = 1.0\n");
            w.write("offsetX = 1.0\n");
            w.write("offsetY = 1.0\n");
            w.write("blurRadius = 0.5\n");
            w.write("color = -16777216\n");
            w.write("colored = false\n");
            w.write("coloredRemapRules = \"rgb:FFFFFF=000000\"\n");
            w.write("opacity = 0.25\n");
            w.write("mode = \"mask\"\n");
            w.write("maskFonts = \"\"\n");
            w.write("maskCodepoints = \"\"\n");
            w.write("\n");
            w.write("[rendering]\n");
            w.write("engine = \"cosmic\"\n");
            w.write("advancedStringMode = true\n");
            w.write("segmentCache = true\n");
            w.write("segmentCacheMinRunLength = 8\n");
            w.write("segmentCacheMaxRunCodePoints = 24\n");
            w.write("segmentCacheMaxSegments = 96\n");
            w.write("interpolation = true\n");
            w.write("mipmap = true\n");
            w.write("adaptiveRasterScale = true\n");
            w.write("adaptiveRasterMin = 1.5\n");
            w.write("adaptiveRasterMax = 14.0\n");
            w.write("adaptiveRasterStep = 0.5\n");
            w.write("excludeIntegerScale = true\n");
            w.write("excludeHighMagnification = true\n");
            w.write("limitMagnification = 3.0\n");
            w.write("scaleRoundingTolerance = 0.5\n");
            w.write("mipmapLodBias = -0.3\n");
            w.write("overlayMipmapLodBias = -0.5\n");
            w.write("anisotropicFiltering = true\n");
            w.write("blurReduction = 10.0\n");
            w.write("smoothShadowThreshold = 24.0\n");
            w.write("enhancedTextPipeline = false\n");
            w.write("shaderTextPipeline = false\n");
            w.write("brightness = 0.0\n");
            w.write("brightnessAuto = true\n");
            w.write("forceBlendForText = true\n");
            w.write("\n");
            w.write("[performance]\n");
            w.write("asyncInit = true\n");
            w.write("prewarmBasicLatin = true\n");
            w.write("signTextLodCulling = true\n");
            w.write("signTextMinPixelHeight = 4.0\n");
            w.write("signTextFrustumCulling = true\n");
            w.write("signModelLod = false\n");
            w.write("signModelLodDistance = 24.0\n");
            w.write("signBlockOcclusionCulling = true\n");
            w.write("signOcclusionChecksPerFrame = 48\n");
            w.write("signOcclusionCacheMillis = 250\n");
            w.write("signOcclusionMinDistance = 8.0\n");
            w.write("textCacheMinEntries = 256\n");
            w.write("textCacheMaxEntries = 2048\n");
            w.write("textCacheTtlSeconds = 300.0\n");
            w.write("measureCacheMaxEntries = 4096\n");
            w.write("\n");
            w.write("[input]\n");
            w.write("allowSignPaste = true\n");
            w.write("fixUnicodeTextDeletion = true\n");
            w.write("\n");
            w.write("[fix]\n");
            w.write("cjkLineBreak = true\n");
            w.write("\n");
            w.write("[laboratory]\n");
            w.write("hexChat = false\n");
            w.write("hexChatResetStyles = true\n");
            w.write("textUndoRedo = false\n");
            w.write("\n");
            w.write("[compat]\n");
            w.write("modernsplash.enabled = true\n");
            w.write("tinkersantique.enabled = true\n");
            w.write("colorPalette.provider = \"auto\"\n");
            w.write("colorPalette.custom = \"" + DEFAULT_TEXT_COLOR_PALETTE + "\"\n");
            w.write("\n");
            w.write("[enchantment]\n");
            w.write("backend = \"awt\"\n");
            w.write("fonts = [\"" + ENCHANTMENT_FONT + "\"]\n");
            w.write("\n");
            w.write("[splash]\n");
            w.write("enabled = true\n");
            w.write("\n");
            w.write("[debug]\n");
            w.write("imeInput = false\n");
            w.write("renderStats = false\n");
        }
    }

    private static void addComments() {
        config.setComment("enabled", "Enable/disable the entire font replacement pipeline.");
        config.setComment("font", "Font selection and rasterization settings.");
        config.setComment("font.name", "Primary font family/display name. This field never stores a file path.");
        config.setComment("font.path", "Optional primary TTF/OTF/TTC source. Game-folder fonts use a portable neofontrender/fonts/... path.");
        config.setComment("font.fallbacks", "Fallback font names or TTF file paths queried after font.name when a glyph is missing.");
        config.setComment("font.style", "Font style: 0=Plain, 1=Bold, 2=Italic, 3=Bold+Italic.");
        config.setComment("font.variableWeight", "Variable font wght axis for regular text. 0=auto, otherwise 1-1000.");
        config.setComment("font.size", "Font size in pixels. 8.5 is the default.");
        config.setComment("font.adaptiveSize", "Scale font size with GUI scale factor for consistent visual size across different GUI scale settings.");
        config.setComment("font.oversample", "Rasterization oversampling factor. Raster resolution is size * oversample; 8.0 at size 8.0 is a 64px glyph raster.");
        config.setComment("font.autoBaseline", "Align each font's measured AWT baseline to the Minecraft reference baseline before manual shift.");
        config.setComment("font.baselineShift", "Additional vertical glyph shift in Minecraft pixels after automatic baseline alignment. Positive moves glyphs down.");
        config.setComment("font.referenceBaseline", "Minecraft-space baseline used by autoBaseline. Vanilla 8px UI text is approximately 7.0.");
        config.setComment("font.antialias", "Enable AWT anti-aliasing during glyph rasterization.");
        config.setComment("font.antialiasMode", "AWT text anti-aliasing mode: off, on, gasp, lcd_hrgb, lcd_hbgr, lcd_vrgb, lcd_vbgr.");
        config.setComment("font.fractionalMetrics", "Enable fractional font metrics for more precise positioning.");
        config.setComment("font.lcdSubpixel", "Enable LCD subpixel anti-aliasing for compatible AWT rasterization paths. It may show color fringes on unsupported display layouts.");
        config.setComment("font.builtinFallbacks", "Always append bundled fonts, such as Noto Color Emoji, to the fallback family.");
        config.setComment("font.cosmic", "Optional Cosmic face overrides. Empty values use family and variable-weight auto matching.");
        config.setComment("font.cosmic.regular", "Cosmic regular face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.bold", "Cosmic bold face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.italic", "Cosmic italic face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.boldItalic", "Cosmic bold-italic face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.variantOverridesOnlySwitchFont", "For non-regular overrides, select the configured font without additionally requesting bold or italic styling. Empty overrides still use automatic family style matching.");
        config.setComment("enchantment", "Scoped rendering for the three magic-name lines in the enchanting table only.");
        config.setComment("enchantment.backend", "Backend for enchanting-table magic names: vanilla, auto, awt, or cosmic.");
        config.setComment("enchantment.fonts", "Editable ordered font list used only by non-vanilla enchanting-table magic names.");
        config.setComment("shadow", "Text shadow rendering options.");
        config.setComment("shadow.length", "Shadow offset distance in pixels.");
        config.setComment("shadow.modern", "Bake a colored soft shadow into the modern backend texture for a single foreground submission.");
        config.setComment("shadow.offsetX", "Modern shadow horizontal offset in pixels.");
        config.setComment("shadow.offsetY", "Modern shadow vertical offset in pixels.");
        config.setComment("shadow.blurRadius", "Modern shadow blur radius in pixels.");
        config.setComment("shadow.color", "Modern shadow ARGB color stored as a signed 32-bit integer.");
        config.setComment("shadow.colored", "Use each formatted text run's foreground RGB for its shadow instead of the configured/darkened shadow color.");
        config.setComment("shadow.coloredRemapRules", "Colored-shadow RGB remaps, e.g. rgb:FFFFFF=000000;slot:e=6A5200. Rules preserve text alpha and only affect shadows.");
        config.setComment("shadow.opacity", "Shadow opacity multiplier (0.0-1.0).");
        config.setComment("shadow.mode", "Shadow mode: all, mask (skip color glyphs), emoji (skip Unicode emoji), or none.");
        config.setComment("shadow.maskFonts", "Comma-separated font families whose displayable code points skip shadows in mask mode.");
        config.setComment("shadow.maskCodepoints", "Comma-separated Unicode code points/ranges whose shadows are skipped, e.g. 1F300-1FAFF,2600-27BF.");
        config.setComment("input.fixUnicodeTextDeletion", "Delete a whole Unicode code point in text fields instead of half of an emoji surrogate pair.");
        config.setComment("fix", "Compatibility and text-behavior fixes.");
        config.setComment("fix.cjkLineBreak", "Allow CJK wrapping opportunities while enforcing basic line-start and line-end punctuation rules.");
        config.setComment("laboratory.textUndoRedo", "Enable per-field undo/redo history in vanilla and ModularUI text inputs (Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z).");
        config.setComment("laboratory.hexChat", "Experimental #RRGGBB chat rendering for the Cosmic text backend.");
        config.setComment("laboratory.hexChatResetStyles", "Match RGB Chat Vintage by clearing bold/italic/etc. when a #RGB marker starts a new color run.");
        config.setComment("compat", "Compatibility options for third-party mods.");
        config.setComment("compat.modernsplash.enabled", "Allow the loading-screen font override to patch ModernSplash when it is installed. Requires splash.enabled and a restart.");
        config.setComment("compat.tinkersantique.enabled", "Handle Tinkers' Construct / TinkersAntique custom PUA color markers (\\uE700-\\uE7FF) as invisible color-change characters instead of rendering them as glyphs.");
        config.setComment("compat.thaumcraft.tooltip.enabled", "Use UIE's modern tooltip renderer for Thaumcraft 6 custom tooltips and decode its @@ compact lines.");
        config.setComment("compat.colorPalette.provider", "Legacy text color palette: auto, vanilla, runtime, custom, or an API-registered provider id. Runtime reads the final FontRenderer.colorCode modified by other mods.");
        config.setComment("compat.colorPalette.custom", "Custom palette as 16 or 32 comma-separated RRGGBB values. Sixteen entries derive Minecraft-style shadow colors; 32 entries set them explicitly.");
        config.setComment("splash", "Forge loading-screen font replacement options.");
        config.setComment("splash.enabled", "Replace the Forge loading-screen bitmap font with the configured TTF font. Restart required.");
        config.setComment("rendering", "OpenGL texture rendering options.");
        config.setComment("rendering.engine", "Text renderer engine: vanilla, sfr, or cosmic.");
        config.setComment("rendering.advancedStringMode", "Render complete formatted strings through the shaped-text backend so ligatures, kerning, emoji ZWJ, and BiDi can span the full text. Disable to use per-format-run rendering.");
        config.setComment("rendering.segmentCache", "When advancedStringMode=false, split safe text runs into reusable render-cache tokens. Complex shaping text stays on the full-run path.");
        config.setComment("rendering.segmentCacheMinRunLength", "Minimum formatted run length before reusable token segmentation is attempted.");
        config.setComment("rendering.segmentCacheMaxRunCodePoints", "Maximum code points kept in one reusable segment before forcing another token boundary.");
        config.setComment("rendering.segmentCacheMaxSegments", "Maximum reusable segments produced from one formatted run.");
        config.setComment("rendering.interpolation", "Use GL_LINEAR texture filtering instead of GL_NEAREST.");
        config.setComment("rendering.mipmap", "Enable mipmapping for font textures (may help at small sizes).");
        config.setComment("rendering.adaptiveRasterScale", "Use a 1.5x-14x adaptive raster scale based on the current framebuffer text scale, and use nearest filtering for 1:1/integer pixel output to avoid over-downsample blur.");
        config.setComment("rendering.adaptiveRasterMin", "Minimum adaptive raster scale bucket.");
        config.setComment("rendering.adaptiveRasterMax", "Maximum adaptive raster scale bucket.");
        config.setComment("rendering.adaptiveRasterStep", "Adaptive raster scale bucket step.");
        config.setComment("rendering.excludeIntegerScale", "When adaptiveRasterScale is enabled, use nearest filtering for near-integer raster/screen scale ratios.");
        config.setComment("rendering.excludeHighMagnification", "When adaptiveRasterScale is enabled, use nearest filtering when text is magnified far beyond the font texture resolution.");
        config.setComment("rendering.limitMagnification", "Magnification threshold used by excludeHighMagnification.");
        config.setComment("rendering.scaleRoundingTolerance", "Percent tolerance used when rounding the measured framebuffer text scale.");
        config.setComment("rendering.mipmapLodBias", "Mipmap LOD bias for perspective/world text while adaptiveRasterScale is enabled.");
        config.setComment("rendering.overlayMipmapLodBias", "Mipmap LOD bias for orthographic GUI text while adaptiveRasterScale is enabled.");
        config.setComment("rendering.anisotropicFiltering", "Enable anisotropic filtering for perspective/world text while adaptiveRasterScale is enabled.");
        config.setComment("rendering.blurReduction", "If the effective font resolution is at or below this value, upload a 2x nearest-neighbor texture to reduce blur.");
        config.setComment("rendering.smoothShadowThreshold", "Minimum effective font resolution where shadow text is allowed to use smooth filtering.");
        config.setComment("rendering.enhancedTextPipeline", "Use a dedicated text draw pipeline that forces straight-alpha blending and restores previous GL state after rendering. Keep this OFF for color emoji; it can alter emoji colors.");
        config.setComment("rendering.shaderTextPipeline", "Use a tiny fixed-pipeline-compatible shader to compensate thin anti-aliased glyph edges. Automatically falls back if shader compilation fails.");
        config.setComment("rendering.brightness", "Text edge compensation strength used by the enhanced shader pipeline. 0 disables extra alpha boost; 3 is close to SmoothFont-style defaults.");
        config.setComment("rendering.brightnessAuto", "Automatically detect brightness compensation from sample glyph rasterization. When true, rendering.brightness is ignored.");
        config.setComment("rendering.forceBlendForText", "Force GL_BLEND on for anti-aliased replacement text when Minecraft disables it for bitmap-font rendering.");
        config.setComment("performance", "Performance tuning options.");
        config.setComment("performance.asyncInit", "Initialize font rasterization on a background thread.");
        config.setComment("performance.prewarmBasicLatin", "Pre-bake common Basic Latin and Latin-1 glyphs before enabling replacement rendering.");
        config.setComment("performance.signTextLodCulling", "Use projected-size LOD and screen culling for sign text. The sign model is still rendered.");
        config.setComment("performance.signTextMinPixelHeight", "Do not submit a sign text line when its projected height is below this many physical framebuffer pixels.");
        config.setComment("performance.signTextFrustumCulling", "Skip the complete sign renderer when its model bounds are outside the camera frustum.");
        config.setComment("performance.signModelLod", "Replace distant sign board/stick boxes with flat textured geometry using the currently bound sign texture.");
        config.setComment("performance.signModelLodDistance", "Distance in blocks where the low-poly sign model starts.");
        config.setComment("performance.signBlockOcclusionCulling", "Skip the complete sign TESR when cached multi-point rays are all blocked by opaque full cubes.");
        config.setComment("performance.signOcclusionChecksPerFrame", "Maximum signs whose block occlusion is refreshed per frame; remaining signs use safe cached results or stay visible.");
        config.setComment("performance.signOcclusionCacheMillis", "How long a sign occlusion result remains fresh while the camera stays within half a block.");
        config.setComment("performance.signOcclusionMinDistance", "Never block-occlusion-cull signs closer than this many blocks to avoid near-camera popping.");
        config.setComment("performance.textCacheMinEntries", "Minimum number of rendered Cosmic text textures kept when TTL cleanup runs.");
        config.setComment("performance.textCacheMaxEntries", "Maximum number of rendered Cosmic text textures kept in the LRU cache.");
        config.setComment("performance.textCacheTtlSeconds", "Seconds before an unused Cosmic text texture can be evicted. 0 disables TTL cleanup.");
        config.setComment("performance.measureCacheMaxEntries", "Maximum number of Cosmic text measurements kept in memory.");
        config.setComment("input", "Input behavior tweaks.");
        config.setComment("input.allowSignPaste", "Allow Ctrl+V paste in the vanilla sign editor. This is intentionally config-file only.");
        config.setComment("debug", "Debug logging options.");
        config.setComment("debug.imeInput", "Log IME input fix details to game log (for diagnosing emoji input issues).");
        config.setComment("debug.renderStats", "Collect high-frequency font renderer hit/miss/eviction counters, segment counters, and expensive raster pixel statistics for F3/commands. Disable for normal gameplay.");
    }

    private static float getFloat(String key, float defaultValue) {
        return getFloat(config, key, defaultValue);
    }

    private static float getFloat(CommentedFileConfig source, String key, float defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Object val = source.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
    }

    private static int getInt(String key, int defaultValue) {
        return getInt(config, key, defaultValue);
    }

    private static int getInt(CommentedFileConfig source, String key, int defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Object val = source.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private static final class Snapshot {
        private final boolean enabled;
        private final boolean fixImeInput;
        private final boolean debugImeInput;
        private final boolean debugRenderStats;
        private final boolean allowSignPaste;
        private final boolean fixUnicodeTextDeletion;
        private final boolean fixCjkLineBreak;
        private final boolean laboratoryHexChat;
        private final boolean laboratoryHexChatResetStyles;
        private final boolean laboratoryTextUndoRedo;
        private final boolean compatModernSplash;
        private final boolean compatTinkersAntique;
        private final boolean compatThaumcraftTooltip;
        private final boolean splashFontOverrideEnabled;
        private final int fontStyle;
        private final int fontVariableWeight;
        private final boolean cosmicVariantOverridesOnlySwitchFont;
        private final float fontSize;
        private final boolean adaptiveFontSize;
        private final float fontOversample;
        private final boolean fontAutoBaseline;
        private final float fontBaselineShift;
        private final float fontReferenceBaseline;
        private final boolean fontAntialias;
        private final String fontAntialiasMode;
        private final boolean fontFractionalMetrics;
        private final boolean fontLcdSubpixel;
        private final boolean builtinFallbacks;
        private final float shadowLength;
        private final boolean modernShadow;
        private final float shadowOffsetX;
        private final float shadowOffsetY;
        private final float shadowBlurRadius;
        private final int shadowColor;
        private final boolean coloredShadow;
        private final ShadowColorRemapRules shadowColorRemapRules;
        private final float shadowOpacity;
        private final String shadowMode;
        private final String shadowMaskFonts;
        private final String shadowMaskCodepoints;
        private final String renderingEngine;
        private final boolean advancedStringMode;
        private final boolean segmentCache;
        private final int segmentCacheMinRunLength;
        private final int segmentCacheMaxRunCodePoints;
        private final int segmentCacheMaxSegments;
        private final boolean renderingInterpolation;
        private final boolean renderingMipmap;
        private final boolean adaptiveRasterScale;
        private final float adaptiveRasterMin;
        private final float adaptiveRasterMax;
        private final float adaptiveRasterStep;
        private final boolean excludeIntegerScale;
        private final boolean excludeHighMagnification;
        private final float limitMagnification;
        private final float scaleRoundingToleranceRate;
        private final float mipmapLodBias;
        private final float overlayMipmapLodBias;
        private final boolean anisotropicFiltering;
        private final float blurReductionThreshold;
        private final float smoothShadowThreshold;
        private final boolean enhancedTextPipeline;
        private final boolean shaderTextPipeline;
        private final float renderingBrightness;
        private final boolean renderingBrightnessAuto;
        private final boolean forceBlendForText;
        private final boolean performanceAsyncInit;
        private final boolean performancePrewarmBasicLatin;
        private final boolean signTextLodCulling;
        private final float signTextMinPixelHeight;
        private final boolean signTextFrustumCulling;
        private final boolean signModelLod;
        private final float signModelLodDistance;
        private final boolean signBlockOcclusionCulling;
        private final int signOcclusionChecksPerFrame;
        private final long signOcclusionCacheMillis;
        private final float signOcclusionMinDistance;
        private final int textCacheMinEntries;
        private final int textCacheMaxEntries;
        private final float textCacheTtlSeconds;
        private final int measureCacheMaxEntries;

        private Snapshot() {
            enabled = true;
            fixImeInput = true;
            debugImeInput = false;
            debugRenderStats = false;
            allowSignPaste = true;
            fixUnicodeTextDeletion = true;
            fixCjkLineBreak = true;
            laboratoryHexChat = false;
            laboratoryHexChatResetStyles = true;
            laboratoryTextUndoRedo = false;
            compatModernSplash = true;
            compatTinkersAntique = true;
            compatThaumcraftTooltip = true;
            splashFontOverrideEnabled = true;
            fontStyle = 0;
            fontVariableWeight = 0;
            cosmicVariantOverridesOnlySwitchFont = false;
            fontSize = 8.5F;
            adaptiveFontSize = false;
            fontOversample = 12.0F;
            fontAutoBaseline = true;
            fontBaselineShift = 0.0F;
            fontReferenceBaseline = 7.0F;
            fontAntialias = true;
            fontAntialiasMode = "on";
            fontFractionalMetrics = true;
            fontLcdSubpixel = false;
            builtinFallbacks = true;
            shadowLength = 1.0F;
            modernShadow = true;
            shadowOffsetX = 1.0F;
            shadowOffsetY = 1.0F;
            shadowBlurRadius = 0.5F;
            shadowColor = 0xFF000000;
            coloredShadow = false;
            shadowColorRemapRules = ShadowColorRemapRules.parse(
                    ShadowColorRemapRules.DEFAULT_CONFIG);
            shadowOpacity = 0.25F;
            shadowMode = "mask";
            shadowMaskFonts = "";
            shadowMaskCodepoints = "";
            renderingEngine = "cosmic";
            advancedStringMode = true;
            segmentCache = true;
            segmentCacheMinRunLength = 8;
            segmentCacheMaxRunCodePoints = 24;
            segmentCacheMaxSegments = 96;
            renderingInterpolation = false;
            renderingMipmap = true;
            adaptiveRasterScale = true;
            adaptiveRasterMin = 1.5F;
            adaptiveRasterMax = 14.0F;
            adaptiveRasterStep = 0.5F;
            excludeIntegerScale = true;
            excludeHighMagnification = true;
            limitMagnification = 3.0F;
            scaleRoundingToleranceRate = 0.005F;
            mipmapLodBias = -0.3F;
            overlayMipmapLodBias = -0.5F;
            anisotropicFiltering = true;
            blurReductionThreshold = 10.0F;
            smoothShadowThreshold = 24.0F;
            enhancedTextPipeline = false;
            shaderTextPipeline = false;
            renderingBrightness = 0.0F;
            renderingBrightnessAuto = true;
            forceBlendForText = true;
            performanceAsyncInit = true;
            performancePrewarmBasicLatin = true;
            signTextLodCulling = true;
            signTextMinPixelHeight = 4.0F;
            signTextFrustumCulling = true;
            signModelLod = false;
            signModelLodDistance = 24.0F;
            signBlockOcclusionCulling = true;
            signOcclusionChecksPerFrame = 48;
            signOcclusionCacheMillis = 250L;
            signOcclusionMinDistance = 8.0F;
            textCacheMinEntries = 256;
            textCacheMaxEntries = 2048;
            textCacheTtlSeconds = 300.0F;
            measureCacheMaxEntries = 4096;
        }

        private Snapshot(CommentedFileConfig config) {
            enabled = config.getOrElse("enabled", true);
            fixImeInput = config.getOrElse("fixImeInput", true);
            debugImeInput = config.getOrElse("debug.imeInput", false);
            debugRenderStats = config.getOrElse("debug.renderStats", false);
            allowSignPaste = config.getOrElse("input.allowSignPaste", true);
            fixUnicodeTextDeletion = config.getOrElse("input.fixUnicodeTextDeletion", true);
            fixCjkLineBreak = config.getOrElse("fix.cjkLineBreak", true);
            laboratoryHexChat = config.getOrElse("laboratory.hexChat", false);
            laboratoryHexChatResetStyles = config.getOrElse("laboratory.hexChatResetStyles", true);
            laboratoryTextUndoRedo = config.getOrElse("laboratory.textUndoRedo", false);
            compatModernSplash = config.getOrElse("compat.modernsplash.enabled", true);
            compatTinkersAntique = config.getOrElse("compat.tinkersantique.enabled", true);
            compatThaumcraftTooltip = config.getOrElse("compat.thaumcraft.tooltip.enabled", true);
            splashFontOverrideEnabled = config.getOrElse("splash.enabled", true);
            fontStyle = config.getOrElse("font.style", 0);
            fontVariableWeight = Math.max(0, Math.min(1000, getInt(config, "font.variableWeight", 0)));
            cosmicVariantOverridesOnlySwitchFont = config.getOrElse("font.cosmic.variantOverridesOnlySwitchFont", false);
            fontSize = getFloat(config, "font.size", 8.5F);
            adaptiveFontSize = config.getOrElse("font.adaptiveSize", false);
            fontOversample = getFloat(config, "font.oversample", 12.0F);
            fontAutoBaseline = config.getOrElse("font.autoBaseline", true);
            fontBaselineShift = getFloat(config, "font.baselineShift", 0.0F);
            fontReferenceBaseline = getFloat(config, "font.referenceBaseline", 7.0F);
            fontAntialias = config.getOrElse("font.antialias", true);
            fontAntialiasMode = normalizeAntialiasMode(config.getOrElse("font.antialiasMode", fontAntialias ? "on" : "off"));
            fontFractionalMetrics = config.getOrElse("font.fractionalMetrics", true);
            fontLcdSubpixel = config.getOrElse("font.lcdSubpixel", false);
            builtinFallbacks = config.getOrElse("font.builtinFallbacks", true);
            shadowLength = getFloat(config, "shadow.length", 1.0F);
            modernShadow = config.getOrElse("shadow.modern", true);
            shadowOffsetX = getFloat(config, "shadow.offsetX", shadowLength);
            shadowOffsetY = getFloat(config, "shadow.offsetY", shadowLength);
            shadowBlurRadius = Math.max(0.0F, getFloat(config, "shadow.blurRadius", 0.5F));
            shadowColor = getInt(config, "shadow.color", 0xFF000000);
            coloredShadow = config.getOrElse("shadow.colored", false);
            shadowColorRemapRules = ShadowColorRemapRules.parse(config.getOrElse(
                    "shadow.coloredRemapRules", ShadowColorRemapRules.DEFAULT_CONFIG));
            shadowOpacity = getFloat(config, "shadow.opacity", 0.25F);
            shadowMode = normalizeShadowMode(config.getOrElse("shadow.mode", "mask"));
            shadowMaskFonts = config.getOrElse("shadow.maskFonts", "");
            shadowMaskCodepoints = config.getOrElse("shadow.maskCodepoints", "");
            renderingEngine = normalizeRenderingEngine(config.getOrElse("rendering.engine", "cosmic"));
            advancedStringMode = config.getOrElse("rendering.advancedStringMode", true);
            segmentCache = config.getOrElse("rendering.segmentCache", true);
            segmentCacheMinRunLength = Math.max(1, getInt(config, "rendering.segmentCacheMinRunLength", 8));
            segmentCacheMaxRunCodePoints = Math.max(1, getInt(config, "rendering.segmentCacheMaxRunCodePoints", 24));
            segmentCacheMaxSegments = Math.max(2, getInt(config, "rendering.segmentCacheMaxSegments", 96));
            renderingInterpolation = config.getOrElse("rendering.interpolation", true);
            renderingMipmap = config.getOrElse("rendering.mipmap", true);
            adaptiveRasterScale = config.getOrElse("rendering.adaptiveRasterScale", true);
            adaptiveRasterMin = getFloat(config, "rendering.adaptiveRasterMin", 1.5F);
            adaptiveRasterMax = getFloat(config, "rendering.adaptiveRasterMax", 14.0F);
            adaptiveRasterStep = getFloat(config, "rendering.adaptiveRasterStep", 0.5F);
            excludeIntegerScale = config.getOrElse("rendering.excludeIntegerScale", true);
            excludeHighMagnification = config.getOrElse("rendering.excludeHighMagnification", true);
            limitMagnification = getFloat(config, "rendering.limitMagnification", 3.0F);
            scaleRoundingToleranceRate = getFloat(config, "rendering.scaleRoundingTolerance", 0.5F) * 0.01F;
            mipmapLodBias = getFloat(config, "rendering.mipmapLodBias", -0.3F);
            overlayMipmapLodBias = getFloat(config, "rendering.overlayMipmapLodBias", -0.5F);
            anisotropicFiltering = config.getOrElse("rendering.anisotropicFiltering", true);
            blurReductionThreshold = getFloat(config, "rendering.blurReduction", 10.0F);
            smoothShadowThreshold = getFloat(config, "rendering.smoothShadowThreshold", 24.0F);
            enhancedTextPipeline = config.getOrElse("rendering.enhancedTextPipeline", false);
            shaderTextPipeline = config.getOrElse("rendering.shaderTextPipeline", false);
            renderingBrightness = getFloat(config, "rendering.brightness", 0.0F);
            renderingBrightnessAuto = config.getOrElse("rendering.brightnessAuto", true);
            forceBlendForText = config.getOrElse("rendering.forceBlendForText", true);
            performanceAsyncInit = config.getOrElse("performance.asyncInit", true);
            performancePrewarmBasicLatin = config.getOrElse("performance.prewarmBasicLatin", true);
            signTextLodCulling = config.getOrElse("performance.signTextLodCulling", true);
            signTextMinPixelHeight = Math.max(0.0F, getFloat(config, "performance.signTextMinPixelHeight", 4.0F));
            signTextFrustumCulling = config.getOrElse("performance.signTextFrustumCulling", true);
            signModelLod = config.getOrElse("performance.signModelLod", false);
            signModelLodDistance = Math.max(4.0F, getFloat(config, "performance.signModelLodDistance", 24.0F));
            signBlockOcclusionCulling = config.getOrElse("performance.signBlockOcclusionCulling", true);
            signOcclusionChecksPerFrame = Math.max(1, getInt(config, "performance.signOcclusionChecksPerFrame", 48));
            signOcclusionCacheMillis = Math.max(50L, getInt(config, "performance.signOcclusionCacheMillis", 250));
            signOcclusionMinDistance = Math.max(2.0F, getFloat(config, "performance.signOcclusionMinDistance", 8.0F));
            textCacheMinEntries = Math.max(0, getInt(config, "performance.textCacheMinEntries", 256));
            textCacheMaxEntries = Math.max(1, getInt(config, "performance.textCacheMaxEntries", 2048));
            textCacheTtlSeconds = Math.max(0.0F, getFloat(config, "performance.textCacheTtlSeconds", 300.0F));
            measureCacheMaxEntries = Math.max(1, getInt(config, "performance.measureCacheMaxEntries", 4096));
        }

        private static Snapshot defaults() {
            return new Snapshot();
        }

        private static Snapshot from(CommentedFileConfig config) {
            return new Snapshot(config);
        }
    }

    private static void addFontNames(Set<String> fonts, String value) {
        if (value == null) {
            return;
        }
        for (String part : value.split("[,;]")) {
            String font = part.trim();
            if (!font.isEmpty()) {
                fonts.add(normalizeFontLocation(font));
            }
        }
    }

    private static boolean migratePortableFontLocations() {
        boolean changed = migratePrimaryFontFields();
        changed |= migrateSingleFontLocation("font.path");
        changed |= migrateFallbackFontNames();
        changed |= migrateSingleFontLocation("font.cosmic.regular");
        changed |= migrateSingleFontLocation("font.cosmic.bold");
        changed |= migrateSingleFontLocation("font.cosmic.italic");
        changed |= migrateSingleFontLocation("font.cosmic.boldItalic");
        changed |= migrateFontLocationList("shadow.maskFonts", false);
        if (changed) {
            NeoFontRender.LOGGER.info("Migrated game-folder font paths to portable locations");
        }
        return changed;
    }

    private static boolean migrateFallbackFontNames() {
        Object current = config.get("font.fallbacks");
        if (current == null) return false;
        List<?> values = current instanceof List ? (List<?>) current : Collections.singletonList(current);
        List<String> migrated = new ArrayList<>();
        for (Object item : values) {
            if (item == null) continue;
            for (String part : item.toString().split("[,;]")) {
                String value = normalizeSingleFont(part.trim());
                File file = resolveFontFile(value);
                String name = file.isFile() && isFontFileLocation(value)
                        ? inferFontFamily(file, value) : value;
                if (!name.isEmpty() && !migrated.contains(name)) migrated.add(name);
            }
        }
        if (migrated.equals(current)) return false;
        config.set("font.fallbacks", migrated);
        return true;
    }

    private static boolean migratePrimaryFontFields() {
        Object current = config.get("font.name");
        if (!(current instanceof String)) {
            return false;
        }
        String value = ((String) current).trim();
        if (!isFontFileLocation(value)) {
            return false;
        }
        String location = normalizeSingleFont(value);
        config.set("font.path", location);
        config.set("font.name", inferFontFamily(resolveFontFile(location), value));
        return true;
    }

    private static boolean migrateSingleFontLocation(String key) {
        Object current = config.get(key);
        if (!(current instanceof String)) {
            return false;
        }
        String normalized = normalizeSingleFont((String) current);
        if (normalized.equals(current)) {
            return false;
        }
        config.set(key, normalized);
        return true;
    }

    private static boolean migrateFontLocationList(String key, boolean storeAsList) {
        Object current = config.get(key);
        if (current == null) {
            return false;
        }
        List<?> currentValues = current instanceof List
                ? (List<?>) current
                : Collections.singletonList(current);
        List<String> normalized = normalizeFontValues(currentValues);
        Object replacement = storeAsList ? normalized : joinFontLocations(normalized);
        if (replacement.equals(current)) {
            return false;
        }
        config.set(key, replacement);
        return true;
    }

    private static String normalizeSingleFont(String value) {
        return normalizeFontLocation(value == null ? "" : value.trim());
    }

    private static List<String> normalizeFontValues(Iterable<?> values) {
        List<String> normalized = new ArrayList<>();
        for (String value : FontFileResolver.normalizeLocations(Minecraft.getMinecraft().gameDir, values)) {
            String font = normalizeSingleFont(value);
            if (!font.isEmpty() && !normalized.contains(font)) {
                normalized.add(font);
            }
        }
        return normalized;
    }

    private static String joinFontLocations(List<String> fonts) {
        return String.join(", ", fonts);
    }

    private static String normalizeFontLocation(String font) {
        if ("neofontrender:fonts/NotoColorEmoji-Regular.ttf".equals(font)) {
            return "neofontrender:fonts/noto_color_emoji_regular.ttf";
        }
        if ("neofontrender:fonts/IBMPlexSansSC-Regular.ttf".equals(font)) {
            return DEFAULT_FONT;
        }
        return FontFileResolver.normalizeLocation(Minecraft.getMinecraft().gameDir, font);
    }

    private static boolean isFontFileLocation(String value) {
        if (value == null) return false;
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
    }

    private static String inferFontFamily(File file, String fallback) {
        if (file != null && file.isFile()) {
            return FontFileResolver.familyName(file);
        }
        String name = fallback == null ? "" : fallback.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        return name.isEmpty() ? DEFAULT_FONT_NAME : name;
    }

    private static String normalizeAntialiasMode(String value) {
        if (value == null) {
            return "on";
        }
        String mode = value.trim().toLowerCase().replace('-', '_');
        switch (mode) {
            case "false":
            case "none":
            case "off":
                return "off";
            case "true":
            case "default":
            case "on":
                return "on";
            case "gasp":
            case "lcd_hrgb":
            case "lcd_hbgr":
            case "lcd_vrgb":
            case "lcd_vbgr":
                return mode;
            default:
                return "on";
        }
    }

    private static String normalizeRenderingEngine(String value) {
        if (value == null) {
            return "cosmic";
        }
        String mode = value.trim().toLowerCase().replace('-', '_');
        switch (mode) {
            case "off":
            case "original":
            case "default":
            case "minecraft":
            case "vanilla":
                return "vanilla";
            case "smr":
            case "sfr":
            case "awt":
                return "sfr";
            case "cosmic_text":
            case "cosmic":
                return "cosmic";
            default:
                return "cosmic";
        }
    }

    private static String normalizeShadowMode(String value) {
        if (value == null) {
            return "mask";
        }
        String mode = value.trim().toLowerCase(Locale.ROOT);
        return "all".equals(mode) || "mask".equals(mode) || "emoji".equals(mode) || "none".equals(mode)
                ? mode : "mask";
    }

    public static void reload() {
        if (config != null) {
            config.load();
        }
    }

    public static final class BuiltinFont {
        private final String displayName;
        private final String location;

        private BuiltinFont(String displayName, String location) {
            this.displayName = displayName;
            this.location = location;
        }

        public String displayName() {
            return displayName;
        }

        public String location() {
            return location;
        }
    }
}
