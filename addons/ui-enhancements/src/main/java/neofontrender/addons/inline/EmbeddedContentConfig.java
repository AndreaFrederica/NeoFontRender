package neofontrender.addons.inline;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Experimental formula and vector-content switches hosted in NFR's Laboratory page. */
public final class EmbeddedContentConfig {
    static boolean latexEnabled;
    static boolean svgEnabled;
    static boolean fullSvgEnabled;
    static boolean markdownEnabled;
    static boolean tooltipLayoutDebug;
    static boolean latexMatchLineHeight;
    static String latexFontFamily;
    static float latexOversample;
    static int rasterCacheEntries = 192;
    static float rasterCacheMegapixels = 24.0F;

    private EmbeddedContentConfig() {}

    public static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("embeddedContent.latex", false,
                        "Render $...$ and $$...$$ formulas through JLaTeXMath.")
                .define("embeddedContent.svg", false,
                        "Render explicit local, resource-pack and external SVG tokens.")
                .define("embeddedContent.fullSvg", false,
                        "Enable all JSVG-supported static features and embedded resources; scripts remain disabled.")
                .define("embeddedContent.markdown", false,
                        "Render bounded single-line Markdown through the standard text pipeline.")
                .define("embeddedContent.tooltipLayoutDebug", false,
                        "Show tooltip layout bounds while the F3 debug overlay is visible.")
                .define("embeddedContent.latexMatchLineHeight", true,
                        "Align LaTeX inline content to the active FontRenderer line height.")
                .define("embeddedContent.latexFontFamily", "",
                        "Optional LaTeX text font family; empty follows NFR primary and fallback families.")
                .define("embeddedContent.latexOversample", 2.0D,
                        "LaTeX raster oversampling factor (1-8).")
                .define("embeddedContent.rasterCacheEntries", 192,
                        "Maximum number of rasterized LaTeX/SVG entries kept by the LRU cache (16-1024).")
                .define("embeddedContent.rasterCacheMegapixels", 24.0D,
                        "Maximum decoded texture pixels kept by the raster cache (4-128 megapixels).");
        latexEnabled = file.getBoolean("embeddedContent.latex", false);
        svgEnabled = file.getBoolean("embeddedContent.svg", false);
        fullSvgEnabled = file.getBoolean("embeddedContent.fullSvg", false);
        markdownEnabled = file.getBoolean("embeddedContent.markdown", false);
        tooltipLayoutDebug = file.getBoolean("embeddedContent.tooltipLayoutDebug", false);
        latexMatchLineHeight = file.getBoolean("embeddedContent.latexMatchLineHeight", true);
        latexFontFamily = file.getString("embeddedContent.latexFontFamily", "").trim();
        latexOversample = clampOversample(file.getDouble("embeddedContent.latexOversample", 2.0D,
                1.0D, 8.0D));
        rasterCacheEntries = clampCacheEntries(file.getInt("embeddedContent.rasterCacheEntries", 192,
                16, 1024));
        rasterCacheMegapixels = clampCacheMegapixels(file.getDouble(
                "embeddedContent.rasterCacheMegapixels", 24.0D, 4.0D, 128.0D));
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("embeddedContent.latex", latexEnabled)
                .set("embeddedContent.svg", svgEnabled)
                .set("embeddedContent.fullSvg", fullSvgEnabled)
                .set("embeddedContent.markdown", markdownEnabled)
                .set("embeddedContent.tooltipLayoutDebug", tooltipLayoutDebug)
                .set("embeddedContent.latexMatchLineHeight", latexMatchLineHeight)
                .set("embeddedContent.latexFontFamily", latexFontFamily)
                .set("embeddedContent.latexOversample", latexOversample)
                .set("embeddedContent.rasterCacheEntries", rasterCacheEntries)
                .set("embeddedContent.rasterCacheMegapixels", rasterCacheMegapixels)
                .save();
    }

    public static boolean latexEnabled() { return latexEnabled; }
    public static boolean svgEnabled() { return svgEnabled; }
    public static boolean fullSvgEnabled() { return svgEnabled && fullSvgEnabled; }
    public static boolean markdownEnabled() { return markdownEnabled; }
    public static boolean tooltipLayoutDebug() { return tooltipLayoutDebug; }
    public static boolean latexMatchLineHeight() { return latexMatchLineHeight; }
    public static String latexFontFamily() { return latexFontFamily == null ? "" : latexFontFamily; }
    public static float latexOversample() { return latexOversample; }
    public static int rasterCacheEntries() { return rasterCacheEntries; }
    public static float rasterCacheMegapixels() { return rasterCacheMegapixels; }
    static float clampOversample(double value) {
        return (float) Math.max(1.0D, Math.min(8.0D, value));
    }

    static int clampCacheEntries(int value) {
        return Math.max(16, Math.min(1024, value));
    }

    static float clampCacheMegapixels(double value) {
        return (float) Math.max(4.0D, Math.min(128.0D, value));
    }
}
