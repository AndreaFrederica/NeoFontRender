package neofontrender.client.gui.model;

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
    public final String originalFontName = NeofontrenderConfig.fontName();
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
    public final boolean originalSkiaAdvancedStringMode = NeofontrenderConfig.skiaAdvancedStringMode();
    public final boolean originalAdaptiveRasterScale = NeofontrenderConfig.adaptiveRasterScale();
    public final boolean originalExcludeIntegerScale = NeofontrenderConfig.excludeIntegerScale();
    public final boolean originalExcludeHighMagnification = NeofontrenderConfig.excludeHighMagnification();
    public final boolean originalAnisotropicFiltering = NeofontrenderConfig.anisotropicFiltering();
    public final boolean originalInterpolation = NeofontrenderConfig.renderingInterpolation();
    public final boolean originalMipmap = NeofontrenderConfig.renderingMipmap();
    public final boolean originalEnhancedTextPipeline = NeofontrenderConfig.enhancedTextPipeline();
    public final boolean originalShaderTextPipeline = NeofontrenderConfig.shaderTextPipeline();
    public final boolean originalSkiaGpuOffscreen = NeofontrenderConfig.skiaGpuOffscreen();
    public final boolean originalSkiaGpuSubmitViaCpuTexture = NeofontrenderConfig.skiaGpuSubmitViaCpuTexture();
    public final boolean originalSkiaMonochromeText = NeofontrenderConfig.skiaMonochromeText();
    public final boolean originalPremultipliedAlpha = NeofontrenderConfig.enablePremultipliedAlpha();
    public final boolean originalDebugRenderStats = NeofontrenderConfig.debugRenderStats();
    public final boolean originalSignModelLod = NeofontrenderConfig.signModelLod();
    public final boolean originalSignCrossTileBatching = NeofontrenderConfig.signCrossTileBatching();
    public final boolean originalSignBlockOcclusionCulling = NeofontrenderConfig.signBlockOcclusionCulling();
    public final String originalBrightness = Float.toString(NeofontrenderConfig.renderingBrightness());
    public final boolean originalTextureEdgeBleed = NeofontrenderConfig.textureEdgeBleed();
    public final String originalShadowMode = NeofontrenderConfig.shadowMode();
    public final String originalShadowMaskFonts = NeofontrenderConfig.shadowMaskFonts();
    public final String originalShadowMaskCodepoints = NeofontrenderConfig.shadowMaskCodepoints();
    public final float originalShadowLength = NeofontrenderConfig.shadowLength();
    public final float originalShadowOpacity = NeofontrenderConfig.shadowOpacity();
    public final boolean originalFixImeInput = NeofontrenderConfig.fixImeInput();
    public final boolean originalFixUnicodeTextDeletion = NeofontrenderConfig.fixUnicodeTextDeletion();
    public final boolean originalAllowSignPaste = NeofontrenderConfig.allowSignPaste();
    public final boolean originalLaboratoryHexChat = NeofontrenderConfig.laboratoryHexChat();
    public final String originalTextCacheMin = Integer.toString(NeofontrenderConfig.skiaTextCacheMinEntries());
    public final String originalTextCacheMax = Integer.toString(NeofontrenderConfig.skiaTextCacheMaxEntries());
    public final String originalTextCacheTtl = Float.toString(NeofontrenderConfig.skiaTextCacheTtlSeconds());
    public final String originalMeasureCacheMax = Integer.toString(NeofontrenderConfig.skiaMeasureCacheMaxEntries());
    public final String originalSegmentCacheMin = Integer.toString(NeofontrenderConfig.skiaSegmentTextureCacheMinEntries());
    public final String originalSegmentCacheMax = Integer.toString(NeofontrenderConfig.skiaSegmentTextureCacheMaxEntries());
    public final String originalSegmentCacheTtl = Float.toString(NeofontrenderConfig.skiaSegmentTextureCacheTtlSeconds());

    public boolean enabled = originalEnabled;
    public String engine = originalEngine;
    public boolean skiaAdvancedStringMode = originalSkiaAdvancedStringMode;
    public boolean adaptiveRasterScale = originalAdaptiveRasterScale;
    public boolean excludeIntegerScale = originalExcludeIntegerScale;
    public boolean excludeHighMagnification = originalExcludeHighMagnification;
    public boolean anisotropicFiltering = originalAnisotropicFiltering;
    public boolean interpolation = originalInterpolation;
    public boolean mipmap = originalMipmap;
    public boolean enhancedTextPipeline = originalEnhancedTextPipeline;
    public boolean shaderTextPipeline = originalShaderTextPipeline;
    public boolean skiaGpuOffscreen = originalSkiaGpuOffscreen;
    public boolean skiaGpuSubmitViaCpuTexture = originalSkiaGpuSubmitViaCpuTexture;
    public boolean skiaMonochromeText = originalSkiaMonochromeText;
    public boolean premultipliedAlpha = originalPremultipliedAlpha;
    public boolean debugRenderStats = originalDebugRenderStats;
    public boolean signModelLod = originalSignModelLod;
    public boolean signCrossTileBatching = originalSignCrossTileBatching;
    public boolean signBlockOcclusionCulling = originalSignBlockOcclusionCulling;
    public String brightness = originalBrightness;
    public boolean textureEdgeBleed = originalTextureEdgeBleed;
    public String shadowMode = originalShadowMode;
    public String shadowMaskFonts = originalShadowMaskFonts;
    public String shadowMaskCodepoints = originalShadowMaskCodepoints;
    public float shadowLength = originalShadowLength;
    public float shadowOpacity = originalShadowOpacity;
    public boolean fixImeInput = originalFixImeInput;
    public boolean fixUnicodeTextDeletion = originalFixUnicodeTextDeletion;
    public boolean allowSignPaste = originalAllowSignPaste;
    public boolean laboratoryHexChat = originalLaboratoryHexChat;
    public int categoryScroll;
    public String fontName = originalFontName;
    public String fontPath = isFontFile(originalFontName) ? originalFontName : "";
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
    public String segmentCacheMin = originalSegmentCacheMin;
    public String segmentCacheMax = originalSegmentCacheMax;
    public String segmentCacheTtl = originalSegmentCacheTtl;

    public String selectedFont() {
        String path = fontPath == null ? "" : fontPath.trim();
        return path.isEmpty() ? fontName.trim() : path;
    }

    public boolean matchesSearch(String font) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return query.isEmpty() || font.toLowerCase(Locale.ROOT).contains(query);
    }

    public boolean isSelected(FontEntry font) {
        String value = fontValue(font);
        switch (fontTarget) {
            case TARGET_FALLBACK:
                return parseFontList(fontFallbacks).contains(value);
            case TARGET_COSMIC_REGULAR:
                return value.equals(cosmicRegular);
            case TARGET_COSMIC_BOLD:
                return value.equals(cosmicBold);
            case TARGET_COSMIC_ITALIC:
                return value.equals(cosmicItalic);
            case TARGET_COSMIC_BOLD_ITALIC:
                return value.equals(cosmicBoldItalic);
            case TARGET_SHADOW_MASK:
                return parseFontList(shadowMaskFonts).contains(value);
            case TARGET_PRIMARY:
            default:
                String path = font.path == null ? "" : font.path;
                return path.isEmpty()
                        ? fontPath.isEmpty() && fontName.equals(font.displayName)
                        : selectedFont().equals(path);
        }
    }

    public void selectFont(FontEntry font) {
        String value = fontValue(font);
        switch (fontTarget) {
            case TARGET_FALLBACK:
                List<String> fonts = parseFontList(fontFallbacks);
                if (fonts.contains(value)) {
                    fonts.remove(value);
                } else {
                    fonts.add(value);
                }
                fontFallbacks = joinFontList(fonts);
                return;
            case TARGET_COSMIC_REGULAR:
                cosmicRegular = toggleSingleFont(cosmicRegular, value);
                return;
            case TARGET_COSMIC_BOLD:
                cosmicBold = toggleSingleFont(cosmicBold, value);
                return;
            case TARGET_COSMIC_ITALIC:
                cosmicItalic = toggleSingleFont(cosmicItalic, value);
                return;
            case TARGET_COSMIC_BOLD_ITALIC:
                cosmicBoldItalic = toggleSingleFont(cosmicBoldItalic, value);
                return;
            case TARGET_SHADOW_MASK:
                List<String> masks = parseFontList(shadowMaskFonts);
                if (masks.contains(value)) masks.remove(value); else masks.add(value);
                shadowMaskFonts = joinFontList(masks);
                return;
            case TARGET_PRIMARY:
            default:
                fontName = font.displayName;
                fontPath = font.path;
        }
    }

    public String toggleSingleFont(String current, String value) {
        String existing = current == null ? "" : current.trim();
        return existing.equals(value) ? "" : value;
    }

    public void writeToConfig(boolean save) {
        NeofontrenderConfig.setEnabled(enabled);
        NeofontrenderConfig.setRenderingEngine(engine);
        NeofontrenderConfig.setSkiaAdvancedStringMode(skiaAdvancedStringMode);
        NeofontrenderConfig.setAdaptiveRasterScale(adaptiveRasterScale);
        NeofontrenderConfig.setExcludeIntegerScale(excludeIntegerScale);
        NeofontrenderConfig.setExcludeHighMagnification(excludeHighMagnification);
        NeofontrenderConfig.setAnisotropicFiltering(anisotropicFiltering);
        NeofontrenderConfig.setRenderingInterpolation(interpolation);
        NeofontrenderConfig.setRenderingMipmap(mipmap);
        NeofontrenderConfig.setEnhancedTextPipeline(enhancedTextPipeline);
        NeofontrenderConfig.setShaderTextPipeline(shaderTextPipeline);
        NeofontrenderConfig.setSkiaGpuOffscreen(skiaGpuOffscreen);
        NeofontrenderConfig.setSkiaGpuSubmitViaCpuTexture(skiaGpuSubmitViaCpuTexture);
        NeofontrenderConfig.setSkiaMonochromeText(skiaMonochromeText);
        NeofontrenderConfig.setEnablePremultipliedAlpha(premultipliedAlpha);
        NeofontrenderConfig.setDebugRenderStats(debugRenderStats);
        NeofontrenderConfig.setSignModelLod(signModelLod);
        NeofontrenderConfig.setSignCrossTileBatching(signCrossTileBatching);
        NeofontrenderConfig.setSignBlockOcclusionCulling(signBlockOcclusionCulling);
        NeofontrenderConfig.setRenderingBrightness(parseFloat(brightness, 3.0F, 0.0F, 12.0F));
        NeofontrenderConfig.setTextureEdgeBleed(textureEdgeBleed);
        NeofontrenderConfig.setShadowMode(shadowMode);
        NeofontrenderConfig.setShadowMaskFonts(shadowMaskFonts);
        NeofontrenderConfig.setShadowMaskCodepoints(shadowMaskCodepoints);
        NeofontrenderConfig.setShadowLength(shadowLength);
        NeofontrenderConfig.setShadowOpacity(shadowOpacity);
        NeofontrenderConfig.setFixImeInput(fixImeInput);
        NeofontrenderConfig.setFixUnicodeTextDeletion(fixUnicodeTextDeletion);
        NeofontrenderConfig.setAllowSignPaste(allowSignPaste);
        NeofontrenderConfig.setLaboratoryHexChat(laboratoryHexChat);
            NeofontrenderConfig.setFontName(selectedFont().isEmpty()
                ? "neofontrender:fonts/sarasa_ui_sc_regular.ttf"
                : selectedFont());
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
        NeofontrenderConfig.setSkiaTextCacheMinEntries(parseInt(textCacheMin, 256, 0, 65536));
        NeofontrenderConfig.setSkiaTextCacheMaxEntries(parseInt(textCacheMax, 2048, 1, 131072));
        NeofontrenderConfig.setSkiaTextCacheTtlSeconds(parseFloat(textCacheTtl, 300.0F, 0.0F, 86400.0F));
        NeofontrenderConfig.setSkiaMeasureCacheMaxEntries(parseInt(measureCacheMax, 4096, 1, 262144));
        NeofontrenderConfig.setSkiaSegmentTextureCacheMinEntries(parseInt(segmentCacheMin, 512, 0, 65536));
        NeofontrenderConfig.setSkiaSegmentTextureCacheMaxEntries(parseInt(segmentCacheMax, 4096, 1, 131072));
        NeofontrenderConfig.setSkiaSegmentTextureCacheTtlSeconds(parseFloat(segmentCacheTtl, 600.0F, 0.0F, 86400.0F));
        if (save) {
            NeofontrenderConfig.save();
        }
    }

    public void restoreOriginal() {
        NeofontrenderConfig.setEnabled(originalEnabled);
        NeofontrenderConfig.setRenderingEngine(originalEngine);
        NeofontrenderConfig.setSkiaAdvancedStringMode(originalSkiaAdvancedStringMode);
        NeofontrenderConfig.setAdaptiveRasterScale(originalAdaptiveRasterScale);
        NeofontrenderConfig.setExcludeIntegerScale(originalExcludeIntegerScale);
        NeofontrenderConfig.setExcludeHighMagnification(originalExcludeHighMagnification);
        NeofontrenderConfig.setAnisotropicFiltering(originalAnisotropicFiltering);
        NeofontrenderConfig.setRenderingInterpolation(originalInterpolation);
        NeofontrenderConfig.setRenderingMipmap(originalMipmap);
        NeofontrenderConfig.setEnhancedTextPipeline(originalEnhancedTextPipeline);
        NeofontrenderConfig.setShaderTextPipeline(originalShaderTextPipeline);
        NeofontrenderConfig.setSkiaGpuOffscreen(originalSkiaGpuOffscreen);
        NeofontrenderConfig.setSkiaGpuSubmitViaCpuTexture(originalSkiaGpuSubmitViaCpuTexture);
        NeofontrenderConfig.setSkiaMonochromeText(originalSkiaMonochromeText);
        NeofontrenderConfig.setEnablePremultipliedAlpha(originalPremultipliedAlpha);
        NeofontrenderConfig.setDebugRenderStats(originalDebugRenderStats);
        NeofontrenderConfig.setSignModelLod(originalSignModelLod);
        NeofontrenderConfig.setSignCrossTileBatching(originalSignCrossTileBatching);
        NeofontrenderConfig.setSignBlockOcclusionCulling(originalSignBlockOcclusionCulling);
        NeofontrenderConfig.setRenderingBrightness(parseFloat(originalBrightness, 3.0F, 0.0F, 12.0F));
        NeofontrenderConfig.setTextureEdgeBleed(originalTextureEdgeBleed);
        NeofontrenderConfig.setShadowMode(originalShadowMode);
        NeofontrenderConfig.setShadowMaskFonts(originalShadowMaskFonts);
        NeofontrenderConfig.setShadowMaskCodepoints(originalShadowMaskCodepoints);
        NeofontrenderConfig.setShadowLength(originalShadowLength);
        NeofontrenderConfig.setShadowOpacity(originalShadowOpacity);
        NeofontrenderConfig.setFixImeInput(originalFixImeInput);
        NeofontrenderConfig.setFixUnicodeTextDeletion(originalFixUnicodeTextDeletion);
        NeofontrenderConfig.setAllowSignPaste(originalAllowSignPaste);
        NeofontrenderConfig.setLaboratoryHexChat(originalLaboratoryHexChat);
        NeofontrenderConfig.setFontName(originalFontName);
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
        NeofontrenderConfig.setSkiaTextCacheMinEntries(parseInt(originalTextCacheMin, 256, 0, 65536));
        NeofontrenderConfig.setSkiaTextCacheMaxEntries(parseInt(originalTextCacheMax, 2048, 1, 131072));
        NeofontrenderConfig.setSkiaTextCacheTtlSeconds(parseFloat(originalTextCacheTtl, 300.0F, 0.0F, 86400.0F));
        NeofontrenderConfig.setSkiaMeasureCacheMaxEntries(parseInt(originalMeasureCacheMax, 4096, 1, 262144));
        NeofontrenderConfig.setSkiaSegmentTextureCacheMinEntries(parseInt(originalSegmentCacheMin, 512, 0, 65536));
        NeofontrenderConfig.setSkiaSegmentTextureCacheMaxEntries(parseInt(originalSegmentCacheMax, 4096, 1, 131072));
        NeofontrenderConfig.setSkiaSegmentTextureCacheTtlSeconds(parseFloat(originalSegmentCacheTtl, 600.0F, 0.0F, 86400.0F));
    }

    private static String fontValue(FontEntry font) {
        String path = font.path == null ? "" : font.path.trim();
        return path.isEmpty() ? font.displayName : path;
    }
}
