package neofontrender.client.gui.model;

import net.minecraft.client.Minecraft;
import neofontrender.client.gui.font.FontEntry;
import neofontrender.core.config.NeofontrenderConfig;
import java.util.List;
import java.util.Locale;
import static neofontrender.client.gui.util.FontCatalog.isFontFile;
import static neofontrender.core.util.ConfigValueParser.joinFontList;
import static neofontrender.core.util.ConfigValueParser.parseFloat;
import static neofontrender.core.util.ConfigValueParser.parseFontList;
import static neofontrender.core.util.ConfigValueParser.parseInt;

public final class NfrSettingsDraft {
    public static final int SOURCE_SYSTEM = 0;
    public static final int SOURCE_FOLDER = 1;
    public static final int SOURCE_BUILTIN = 2;
    public static final int TARGET_PRIMARY = 0;
    public static final int TARGET_FALLBACK = 1;
    public static final int TARGET_COSMIC_REGULAR = 2;
    public static final int TARGET_COSMIC_BOLD = 3;
    public static final int TARGET_COSMIC_ITALIC = 4;
    public static final int TARGET_COSMIC_BOLD_ITALIC = 5;
    public static final int TARGET_SHADOW_MASK = 6;
    public static final int TARGET_COUNT = 7;
    public final boolean originalEnabled = NeofontrenderConfig.enabled();
    public final boolean originalForceUnicodeFont = Minecraft.getMinecraft().gameSettings.forceUnicodeFont;
    public final String originalFontName = NeofontrenderConfig.fontName();
    public final String originalFontPath = NeofontrenderConfig.fontPath();
    public final String originalCosmicRegular = NeofontrenderConfig.cosmicRegularFont();
    public final String originalCosmicBold = NeofontrenderConfig.cosmicBoldFont();
    public final String originalCosmicItalic = NeofontrenderConfig.cosmicItalicFont();
    public final String originalCosmicBoldItalic = NeofontrenderConfig.cosmicBoldItalicFont();
    public final boolean originalCosmicVariantOverridesOnlySwitchFont =
            NeofontrenderConfig.cosmicVariantOverridesOnlySwitchFont();
    public final int originalFontStyle = NeofontrenderConfig.fontStyle();
    public final String originalVariableWeight = Integer.toString(NeofontrenderConfig.fontVariableWeight());
    public final String originalFontSize = Float.toString(NeofontrenderConfig.fontSize());
    public final String originalOversample = Float.toString(NeofontrenderConfig.fontOversample());
    public final boolean originalAutoBaseline = NeofontrenderConfig.fontAutoBaseline();
    public final String originalBaselineShift = Float.toString(NeofontrenderConfig.fontBaselineShift());
    public final boolean originalAntialias = NeofontrenderConfig.fontAntialias();
    public final String originalAntialiasMode = NeofontrenderConfig.fontAntialiasMode();
    public final boolean originalFractionalMetrics = NeofontrenderConfig.fontFractionalMetrics();
    public final String originalFontFallbacks = joinFontList(NeofontrenderConfig.fontFallbacks());
    public final boolean originalBuiltinFallbacks = NeofontrenderConfig.builtinFallbacksEnabled();
    public final String originalEngine = NeofontrenderConfig.renderingEngine();
    public final boolean originalAdvancedStringMode = NeofontrenderConfig.advancedStringMode();
    public final boolean originalAdaptiveRasterScale = NeofontrenderConfig.adaptiveRasterScale();
    public final boolean originalExcludeIntegerScale = NeofontrenderConfig.excludeIntegerScale();
    public final boolean originalExcludeHighMagnification = NeofontrenderConfig.excludeHighMagnification();
    public final boolean originalAnisotropicFiltering = NeofontrenderConfig.anisotropicFiltering();
    public final boolean originalInterpolation = NeofontrenderConfig.renderingInterpolation();
    public final boolean originalMipmap = NeofontrenderConfig.renderingMipmap();
    public final boolean originalEnhancedTextPipeline = NeofontrenderConfig.enhancedTextPipeline();
    public final boolean originalShaderTextPipeline = NeofontrenderConfig.shaderTextPipeline();
    public final boolean originalDebugRenderStats = NeofontrenderConfig.debugRenderStats();
    public final boolean originalSignModelLod = NeofontrenderConfig.signModelLod();
    public final boolean originalSignBlockOcclusionCulling = NeofontrenderConfig.signBlockOcclusionCulling();
    public final String originalBrightness = Float.toString(NeofontrenderConfig.renderingBrightness());
    public final String originalShadowMode = NeofontrenderConfig.shadowMode();
    public final String originalShadowMaskFonts = NeofontrenderConfig.shadowMaskFonts();
    public final String originalShadowMaskCodepoints = NeofontrenderConfig.shadowMaskCodepoints();
    public final float originalShadowLength = NeofontrenderConfig.shadowLength();
    public final float originalShadowOpacity = NeofontrenderConfig.shadowOpacity();
    public final boolean originalModernShadow = NeofontrenderConfig.modernShadowEnabled();
    public final float originalShadowOffsetX = NeofontrenderConfig.shadowOffsetX();
    public final float originalShadowOffsetY = NeofontrenderConfig.shadowOffsetY();
    public final float originalShadowBlurRadius = NeofontrenderConfig.shadowBlurRadius();
    public final int originalShadowColor = NeofontrenderConfig.shadowColor();
    public final boolean originalColoredShadow = NeofontrenderConfig.coloredShadowEnabled();
    public final String originalShadowColorRemapRules =
            NeofontrenderConfig.shadowColorRemapRulesConfig();
    public final boolean originalFixImeInput = NeofontrenderConfig.fixImeInput();
    public final boolean originalFixUnicodeTextDeletion = NeofontrenderConfig.fixUnicodeTextDeletion();
    public final boolean originalFixCjkLineBreak = NeofontrenderConfig.fixCjkLineBreak();
    public final boolean originalAllowSignPaste = NeofontrenderConfig.allowSignPaste();
    public final boolean originalLaboratoryHexChat = NeofontrenderConfig.laboratoryHexChat();
    public final boolean originalLaboratoryHexChatResetStyles = NeofontrenderConfig.laboratoryHexChatResetStyles();
    public final boolean originalLaboratoryTextUndoRedo = NeofontrenderConfig.laboratoryTextUndoRedo();
    public final boolean originalCompatModernSplash = NeofontrenderConfig.compatModernSplash();
    public final boolean originalCompatTinkersAntique = NeofontrenderConfig.compatTinkersAntique();
    public final String originalTextColorPaletteProvider =
            NeofontrenderConfig.textColorPaletteProvider();
    public final String originalCustomTextColorPalette =
            NeofontrenderConfig.customTextColorPalette();
    public final String originalEnchantmentBackend = NeofontrenderConfig.enchantmentFontBackend();
    public final String originalEnchantmentFonts = joinFontList(NeofontrenderConfig.enchantmentFonts());
    public final boolean originalSplashFontOverride = NeofontrenderConfig.splashFontOverrideEnabled();
    public final String originalTextCacheMin = Integer.toString(NeofontrenderConfig.textCacheMinEntries());
    public final String originalTextCacheMax = Integer.toString(NeofontrenderConfig.textCacheMaxEntries());
    public final String originalTextCacheTtl = Float.toString(NeofontrenderConfig.textCacheTtlSeconds());
    public final String originalMeasureCacheMax = Integer.toString(NeofontrenderConfig.measureCacheMaxEntries());

    public boolean enabled = originalEnabled;
    public boolean forceUnicodeFont = originalForceUnicodeFont;
    public String engine = originalEngine;
    public boolean advancedStringMode = originalAdvancedStringMode;
    public boolean adaptiveRasterScale = originalAdaptiveRasterScale;
    public boolean excludeIntegerScale = originalExcludeIntegerScale;
    public boolean excludeHighMagnification = originalExcludeHighMagnification;
    public boolean anisotropicFiltering = originalAnisotropicFiltering;
    public boolean interpolation = originalInterpolation;
    public boolean mipmap = originalMipmap;
    public boolean enhancedTextPipeline = originalEnhancedTextPipeline;
    public boolean shaderTextPipeline = originalShaderTextPipeline;
    public boolean debugRenderStats = originalDebugRenderStats;
    public boolean signModelLod = originalSignModelLod;
    public boolean signBlockOcclusionCulling = originalSignBlockOcclusionCulling;
    public String brightness = originalBrightness;
    public String shadowMode = originalShadowMode;
    public String shadowMaskFonts = originalShadowMaskFonts;
    public String shadowMaskCodepoints = originalShadowMaskCodepoints;
    public float shadowLength = originalShadowLength;
    public float shadowOpacity = originalShadowOpacity;
    public boolean modernShadow = originalModernShadow;
    public float shadowOffsetX = originalShadowOffsetX;
    public float shadowOffsetY = originalShadowOffsetY;
    public float shadowBlurRadius = originalShadowBlurRadius;
    public int shadowColor = originalShadowColor;
    public boolean coloredShadow = originalColoredShadow;
    public String shadowColorRemapRules = originalShadowColorRemapRules;
    public boolean fixImeInput = originalFixImeInput;
    public boolean fixUnicodeTextDeletion = originalFixUnicodeTextDeletion;
    public boolean fixCjkLineBreak = originalFixCjkLineBreak;
    public boolean allowSignPaste = originalAllowSignPaste;
    public boolean laboratoryHexChat = originalLaboratoryHexChat;
    public boolean laboratoryHexChatResetStyles = originalLaboratoryHexChatResetStyles;
    public boolean laboratoryTextUndoRedo = originalLaboratoryTextUndoRedo;
    public boolean compatModernSplash = originalCompatModernSplash;
    public boolean compatTinkersAntique = originalCompatTinkersAntique;
    public String textColorPaletteProvider = originalTextColorPaletteProvider;
    public String customTextColorPalette = originalCustomTextColorPalette;
    public String enchantmentBackend = originalEnchantmentBackend;
    public String enchantmentFonts = originalEnchantmentFonts;
    public boolean splashFontOverride = originalSplashFontOverride;
    public int categoryScroll;
    public String fontName = originalFontName;
    public String fontPath = originalFontPath;
    public String cosmicRegular = originalCosmicRegular;
    public String cosmicBold = originalCosmicBold;
    public String cosmicItalic = originalCosmicItalic;
    public String cosmicBoldItalic = originalCosmicBoldItalic;
    public boolean cosmicVariantOverridesOnlySwitchFont = originalCosmicVariantOverridesOnlySwitchFont;
    public String fontFallbacks = originalFontFallbacks;
    public int fontStyle = originalFontStyle;
    public String variableWeight = originalVariableWeight;
    public String fontSize = originalFontSize;
    public String oversample = originalOversample;
    public boolean autoBaseline = originalAutoBaseline;
    public String baselineShift = originalBaselineShift;
    public boolean antialias = originalAntialias;
    public String antialiasMode = originalAntialiasMode;
    public boolean fractionalMetrics = originalFractionalMetrics;
    public String search = "";
    public int fontSource = SOURCE_SYSTEM;
    public int fontTarget = TARGET_PRIMARY;
    public boolean builtinFallbacks = originalBuiltinFallbacks;
    public String textCacheMin = originalTextCacheMin;
    public String textCacheMax = originalTextCacheMax;
    public String textCacheTtl = originalTextCacheTtl;
    public String measureCacheMax = originalMeasureCacheMax;

    public String selectedFont() {
        String path = fontPath == null ? "" : fontPath.trim();
        return path.isEmpty() ? fontName.trim() : path;
    }

    public boolean matchesSearch(String font) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return query.isEmpty() || font.toLowerCase(Locale.ROOT).contains(query);
    }

    public boolean isSelected(FontEntry font) {
        switch (fontTarget) {
            case TARGET_FALLBACK:
                return parseFontList(fontFallbacks).contains(font.familyName);
            case TARGET_COSMIC_REGULAR:
                return fontValue(font).equals(cosmicRegular);
            case TARGET_COSMIC_BOLD:
                return fontValue(font).equals(cosmicBold);
            case TARGET_COSMIC_ITALIC:
                return fontValue(font).equals(cosmicItalic);
            case TARGET_COSMIC_BOLD_ITALIC:
                return fontValue(font).equals(cosmicBoldItalic);
            case TARGET_SHADOW_MASK:
                return parseFontList(shadowMaskFonts).contains(fontValue(font));
            case TARGET_PRIMARY:
            default:
                String path = font.path == null ? "" : font.path;
                return path.isEmpty()
                        ? fontPath.isEmpty() && fontName.equals(font.displayName)
                        : selectedFont().equals(path);
        }
    }

    public void selectFont(FontEntry font) {
        switch (fontTarget) {
            case TARGET_FALLBACK:
                String value = font.familyName;
                List<String> fonts = parseFontList(fontFallbacks);
                if (fonts.contains(value)) {
                    fonts.remove(value);
                } else {
                    fonts.add(value);
                }
                fontFallbacks = joinFontList(fonts);
                return;
            case TARGET_COSMIC_REGULAR:
                cosmicRegular = toggleSingleFont(cosmicRegular, fontValue(font));
                return;
            case TARGET_COSMIC_BOLD:
                cosmicBold = toggleSingleFont(cosmicBold, fontValue(font));
                return;
            case TARGET_COSMIC_ITALIC:
                cosmicItalic = toggleSingleFont(cosmicItalic, fontValue(font));
                return;
            case TARGET_COSMIC_BOLD_ITALIC:
                cosmicBoldItalic = toggleSingleFont(cosmicBoldItalic, fontValue(font));
                return;
            case TARGET_SHADOW_MASK:
                List<String> masks = parseFontList(shadowMaskFonts);
                String maskValue = fontValue(font);
                if (masks.contains(maskValue)) masks.remove(maskValue); else masks.add(maskValue);
                shadowMaskFonts = joinFontList(masks);
                return;
            case TARGET_PRIMARY:
            default:
                fontName = font.familyName;
                fontPath = font.path;
        }
    }

    public String toggleSingleFont(String current, String value) {
        String existing = current == null ? "" : current.trim();
        return existing.equals(value) ? "" : value;
    }

    public void writeToConfig(boolean save) {
        writeToConfig(save, true);
    }

    /** Applies live-preview settings while keeping the Shadow page isolated until Apply. */
    public void writePreviewToConfig() {
        writeToConfig(false, false);
    }

    private void writeToConfig(boolean save, boolean includeShadowPage) {
        applyForceUnicodeFont(forceUnicodeFont, save);
        NeofontrenderConfig.setEnabled(enabled);
        NeofontrenderConfig.setRenderingEngine(engine);
        NeofontrenderConfig.setAdvancedStringMode(advancedStringMode);
        NeofontrenderConfig.setAdaptiveRasterScale(adaptiveRasterScale);
        NeofontrenderConfig.setExcludeIntegerScale(excludeIntegerScale);
        NeofontrenderConfig.setExcludeHighMagnification(excludeHighMagnification);
        NeofontrenderConfig.setAnisotropicFiltering(anisotropicFiltering);
        NeofontrenderConfig.setRenderingInterpolation(interpolation);
        NeofontrenderConfig.setRenderingMipmap(mipmap);
        NeofontrenderConfig.setEnhancedTextPipeline(enhancedTextPipeline);
        NeofontrenderConfig.setShaderTextPipeline(shaderTextPipeline);
        NeofontrenderConfig.setDebugRenderStats(debugRenderStats);
        NeofontrenderConfig.setSignModelLod(signModelLod);
        NeofontrenderConfig.setSignBlockOcclusionCulling(signBlockOcclusionCulling);
        NeofontrenderConfig.setRenderingBrightness(parseFloat(brightness, 3.0F, 0.0F, 12.0F));
        NeofontrenderConfig.setShadowMaskFonts(shadowMaskFonts);
        NeofontrenderConfig.setShadowMaskCodepoints(shadowMaskCodepoints);
        if (includeShadowPage) {
            NeofontrenderConfig.setShadowMode(shadowMode);
            NeofontrenderConfig.setShadowLength(shadowLength);
            NeofontrenderConfig.setShadowOpacity(shadowOpacity);
            NeofontrenderConfig.setModernShadowEnabled(modernShadow);
            NeofontrenderConfig.setShadowOffsetX(shadowOffsetX);
            NeofontrenderConfig.setShadowOffsetY(shadowOffsetY);
            NeofontrenderConfig.setShadowBlurRadius(shadowBlurRadius);
            NeofontrenderConfig.setShadowColor(shadowColor);
            NeofontrenderConfig.setColoredShadowEnabled(coloredShadow);
            NeofontrenderConfig.setShadowColorRemapRules(shadowColorRemapRules);
        }
        NeofontrenderConfig.setFixImeInput(fixImeInput);
        NeofontrenderConfig.setFixUnicodeTextDeletion(fixUnicodeTextDeletion);
        NeofontrenderConfig.setFixCjkLineBreak(fixCjkLineBreak);
        NeofontrenderConfig.setAllowSignPaste(allowSignPaste);
        NeofontrenderConfig.setLaboratoryHexChat(laboratoryHexChat);
        NeofontrenderConfig.setLaboratoryHexChatResetStyles(laboratoryHexChatResetStyles);
        NeofontrenderConfig.setLaboratoryTextUndoRedo(laboratoryTextUndoRedo);
        NeofontrenderConfig.setCompatModernSplash(compatModernSplash);
        NeofontrenderConfig.setCompatTinkersAntique(compatTinkersAntique);
        NeofontrenderConfig.setTextColorPaletteProvider(textColorPaletteProvider);
        NeofontrenderConfig.setCustomTextColorPalette(customTextColorPalette);
        NeofontrenderConfig.setEnchantmentFontBackend(enchantmentBackend);
        NeofontrenderConfig.setEnchantmentFonts(parseFontList(enchantmentFonts));
        NeofontrenderConfig.setSplashFontOverrideEnabled(splashFontOverride);
        NeofontrenderConfig.setFontName(fontName == null || fontName.trim().isEmpty()
                ? "Noto Sans SC" : fontName.trim());
        NeofontrenderConfig.setFontPath(fontPath);
        NeofontrenderConfig.setFontFallbacks(parseFontList(fontFallbacks));
        NeofontrenderConfig.setCosmicRegularFont(cosmicRegular);
        NeofontrenderConfig.setCosmicBoldFont(cosmicBold);
        NeofontrenderConfig.setCosmicItalicFont(cosmicItalic);
        NeofontrenderConfig.setCosmicBoldItalicFont(cosmicBoldItalic);
        NeofontrenderConfig.setCosmicVariantOverridesOnlySwitchFont(cosmicVariantOverridesOnlySwitchFont);
        NeofontrenderConfig.setFontStyle(fontStyle);
        NeofontrenderConfig.setFontVariableWeight(parseInt(variableWeight, 0, 0, 1000));
        NeofontrenderConfig.setFontSize(parseFloat(fontSize, 8.5F, 4.0F, 64.0F));
        NeofontrenderConfig.setFontOversample(parseFloat(oversample, 8.0F, 1.0F, 16.0F));
        NeofontrenderConfig.setFontAutoBaseline(autoBaseline);
        NeofontrenderConfig.setFontBaselineShift(parseFloat(baselineShift, 0.0F, -16.0F, 16.0F));
        NeofontrenderConfig.setFontAntialias(antialias);
        NeofontrenderConfig.setFontAntialiasMode(antialias ? antialiasMode : "off");
        NeofontrenderConfig.setFontFractionalMetrics(fractionalMetrics);
        NeofontrenderConfig.setBuiltinFallbacksEnabled(builtinFallbacks);
        NeofontrenderConfig.setTextCacheMinEntries(parseInt(textCacheMin, 256, 0, 65536));
        NeofontrenderConfig.setTextCacheMaxEntries(parseInt(textCacheMax, 2048, 1, 131072));
        NeofontrenderConfig.setTextCacheTtlSeconds(parseFloat(textCacheTtl, 300.0F, 0.0F, 86400.0F));
        NeofontrenderConfig.setMeasureCacheMaxEntries(parseInt(measureCacheMax, 4096, 1, 262144));
        if (save) {
            NeofontrenderConfig.save();
        }
    }

    public void restoreOriginal() {
        applyForceUnicodeFont(originalForceUnicodeFont, false);
        NeofontrenderConfig.setEnabled(originalEnabled);
        NeofontrenderConfig.setRenderingEngine(originalEngine);
        NeofontrenderConfig.setAdvancedStringMode(originalAdvancedStringMode);
        NeofontrenderConfig.setAdaptiveRasterScale(originalAdaptiveRasterScale);
        NeofontrenderConfig.setExcludeIntegerScale(originalExcludeIntegerScale);
        NeofontrenderConfig.setExcludeHighMagnification(originalExcludeHighMagnification);
        NeofontrenderConfig.setAnisotropicFiltering(originalAnisotropicFiltering);
        NeofontrenderConfig.setRenderingInterpolation(originalInterpolation);
        NeofontrenderConfig.setRenderingMipmap(originalMipmap);
        NeofontrenderConfig.setEnhancedTextPipeline(originalEnhancedTextPipeline);
        NeofontrenderConfig.setShaderTextPipeline(originalShaderTextPipeline);
        NeofontrenderConfig.setDebugRenderStats(originalDebugRenderStats);
        NeofontrenderConfig.setSignModelLod(originalSignModelLod);
        NeofontrenderConfig.setSignBlockOcclusionCulling(originalSignBlockOcclusionCulling);
        NeofontrenderConfig.setRenderingBrightness(parseFloat(originalBrightness, 3.0F, 0.0F, 12.0F));
        NeofontrenderConfig.setShadowMode(originalShadowMode);
        NeofontrenderConfig.setShadowMaskFonts(originalShadowMaskFonts);
        NeofontrenderConfig.setShadowMaskCodepoints(originalShadowMaskCodepoints);
        NeofontrenderConfig.setShadowLength(originalShadowLength);
        NeofontrenderConfig.setShadowOpacity(originalShadowOpacity);
        NeofontrenderConfig.setModernShadowEnabled(originalModernShadow);
        NeofontrenderConfig.setShadowOffsetX(originalShadowOffsetX);
        NeofontrenderConfig.setShadowOffsetY(originalShadowOffsetY);
        NeofontrenderConfig.setShadowBlurRadius(originalShadowBlurRadius);
        NeofontrenderConfig.setShadowColor(originalShadowColor);
        NeofontrenderConfig.setColoredShadowEnabled(originalColoredShadow);
        NeofontrenderConfig.setShadowColorRemapRules(originalShadowColorRemapRules);
        NeofontrenderConfig.setFixImeInput(originalFixImeInput);
        NeofontrenderConfig.setFixUnicodeTextDeletion(originalFixUnicodeTextDeletion);
        NeofontrenderConfig.setFixCjkLineBreak(originalFixCjkLineBreak);
        NeofontrenderConfig.setAllowSignPaste(originalAllowSignPaste);
        NeofontrenderConfig.setLaboratoryHexChat(originalLaboratoryHexChat);
        NeofontrenderConfig.setLaboratoryHexChatResetStyles(originalLaboratoryHexChatResetStyles);
        NeofontrenderConfig.setLaboratoryTextUndoRedo(originalLaboratoryTextUndoRedo);
        NeofontrenderConfig.setCompatModernSplash(originalCompatModernSplash);
        NeofontrenderConfig.setCompatTinkersAntique(originalCompatTinkersAntique);
        NeofontrenderConfig.setTextColorPaletteProvider(originalTextColorPaletteProvider);
        NeofontrenderConfig.setCustomTextColorPalette(originalCustomTextColorPalette);
        NeofontrenderConfig.setEnchantmentFontBackend(originalEnchantmentBackend);
        NeofontrenderConfig.setEnchantmentFonts(parseFontList(originalEnchantmentFonts));
        NeofontrenderConfig.setSplashFontOverrideEnabled(originalSplashFontOverride);
        NeofontrenderConfig.setFontName(originalFontName);
        NeofontrenderConfig.setFontPath(originalFontPath);
        NeofontrenderConfig.setFontFallbacks(parseFontList(originalFontFallbacks));
        NeofontrenderConfig.setCosmicRegularFont(originalCosmicRegular);
        NeofontrenderConfig.setCosmicBoldFont(originalCosmicBold);
        NeofontrenderConfig.setCosmicItalicFont(originalCosmicItalic);
        NeofontrenderConfig.setCosmicBoldItalicFont(originalCosmicBoldItalic);
        NeofontrenderConfig.setCosmicVariantOverridesOnlySwitchFont(
                originalCosmicVariantOverridesOnlySwitchFont);
        NeofontrenderConfig.setFontStyle(originalFontStyle);
        NeofontrenderConfig.setFontVariableWeight(parseInt(originalVariableWeight, 0, 0, 1000));
        NeofontrenderConfig.setFontSize(parseFloat(originalFontSize, 8.5F, 4.0F, 64.0F));
        NeofontrenderConfig.setFontOversample(parseFloat(originalOversample, 8.0F, 1.0F, 16.0F));
        NeofontrenderConfig.setFontAutoBaseline(originalAutoBaseline);
        NeofontrenderConfig.setFontBaselineShift(parseFloat(originalBaselineShift, 0.0F, -16.0F, 16.0F));
        NeofontrenderConfig.setFontAntialias(originalAntialias);
        NeofontrenderConfig.setFontAntialiasMode(originalAntialias ? originalAntialiasMode : "off");
        NeofontrenderConfig.setFontFractionalMetrics(originalFractionalMetrics);
        NeofontrenderConfig.setBuiltinFallbacksEnabled(originalBuiltinFallbacks);
        NeofontrenderConfig.setTextCacheMinEntries(parseInt(originalTextCacheMin, 256, 0, 65536));
        NeofontrenderConfig.setTextCacheMaxEntries(parseInt(originalTextCacheMax, 2048, 1, 131072));
        NeofontrenderConfig.setTextCacheTtlSeconds(parseFloat(originalTextCacheTtl, 300.0F, 0.0F, 86400.0F));
        NeofontrenderConfig.setMeasureCacheMaxEntries(parseInt(originalMeasureCacheMax, 4096, 1, 262144));
    }

    private static void applyForceUnicodeFont(boolean forceUnicode, boolean save) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.gameSettings.forceUnicodeFont = forceUnicode;
        if (minecraft.fontRenderer != null && minecraft.getLanguageManager() != null) {
            minecraft.fontRenderer.setUnicodeFlag(
                    minecraft.getLanguageManager().isCurrentLocaleUnicode() || forceUnicode);
        }
        if (save) {
            minecraft.gameSettings.saveOptions();
        }
    }

    private static String fontValue(FontEntry font) {
        String path = font.path == null ? "" : font.path.trim();
        return path.isEmpty() ? font.displayName : path;
    }
}
