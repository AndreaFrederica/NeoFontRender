package neofontrender.core.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraft.client.Minecraft;
import neofontrender.NeoFontRender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TOML-based configuration manager using NightConfig.
 */
public final class NeofontrenderConfig {

    private static final String CONFIG_NAME = "neofontrender.toml";
    private static Path configPath;
    private static CommentedFileConfig config;
    private static final List<BuiltinFont> BUILTIN_FONTS = Collections.unmodifiableList(Collections.singletonList(
            new BuiltinFont("Noto Color Emoji", "neofontrender:fonts/noto_color_emoji_regular.ttf")
    ));

    public static boolean isLoaded() {
        return config != null;
    }

    // ===================== Font =====================
    public static String fontName() {
        return config.getOrElse("font.name", "SansSerif");
    }

    public static List<String> fontFamily() {
        Set<String> fonts = new LinkedHashSet<>();
        addFontNames(fonts, fontName());
        fonts.addAll(fontFallbacks());
        if (builtinFallbacksEnabled()) {
            for (BuiltinFont font : builtinFonts()) {
                fonts.add(font.location);
            }
        }
        if (fonts.isEmpty()) {
            fonts.add("SansSerif");
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
        return Collections.unmodifiableList(new ArrayList<>(fonts));
    }

    public static int fontStyle() {
        return config.getOrElse("font.style", 0);
    }

    public static float fontSize() {
        return getFloat("font.size", 8.0f);
    }

    public static float fontOversample() {
        return getFloat("font.oversample", 8.0f);
    }

    public static boolean fontAutoBaseline() {
        return config.getOrElse("font.autoBaseline", true);
    }

    public static float fontBaselineShift() {
        return getFloat("font.baselineShift", 0.0f);
    }

    public static float fontReferenceBaseline() {
        return getFloat("font.referenceBaseline", 7.0f);
    }

    public static boolean fontAntialias() {
        return config.getOrElse("font.antialias", true);
    }

    public static String fontAntialiasMode() {
        String mode = config.getOrElse("font.antialiasMode", fontAntialias() ? "on" : "off");
        return normalizeAntialiasMode(mode);
    }

    public static boolean fontFractionalMetrics() {
        return config.getOrElse("font.fractionalMetrics", true);
    }

    public static boolean builtinFallbacksEnabled() {
        return config.getOrElse("font.builtinFallbacks", true);
    }

    public static List<BuiltinFont> builtinFonts() {
        return BUILTIN_FONTS;
    }

    // ===================== Shadow =====================
    public static float shadowLength() {
        return getFloat("shadow.length", 1.0f);
    }

    public static float shadowOpacity() {
        return getFloat("shadow.opacity", 0.25f);
    }

    // ===================== Rendering =====================
    public static String renderingEngine() {
        return normalizeRenderingEngine(config.getOrElse("rendering.engine", "sfr"));
    }

    public static boolean useSfrEngine() {
        return enabled() && "sfr".equals(renderingEngine());
    }

    public static boolean useSkiaEngine() {
        return enabled() && "skia".equals(renderingEngine());
    }

    public static boolean skiaAdvancedStringMode() {
        return config.getOrElse("rendering.skiaAdvancedStringMode", true);
    }

    public static boolean useVanillaEngine() {
        return !enabled() || "vanilla".equals(renderingEngine());
    }

    public static boolean renderingInterpolation() {
        return config.getOrElse("rendering.interpolation", true);
    }

    public static boolean renderingMipmap() {
        return config.getOrElse("rendering.mipmap", false);
    }

    // ===================== Performance =====================
    public static boolean performanceAsyncInit() {
        return config.getOrElse("performance.asyncInit", true);
    }

    public static boolean performancePrewarmBasicLatin() {
        return config.getOrElse("performance.prewarmBasicLatin", true);
    }

    // ===================== General =====================
    public static boolean enabled() {
        return config.getOrElse("enabled", true);
    }

    public static boolean fixImeInput() {
        return config.getOrElse("fixImeInput", true);
    }

    public static boolean debugImeInput() {
        return config.getOrElse("debug.imeInput", false);
    }

    public static boolean allowSignPaste() {
        return config.getOrElse("input.allowSignPaste", true);
    }

    public static void setEnabled(boolean value) {
        config.set("enabled", value);
    }

    public static void setFontName(String value) {
        config.set("font.name", value);
    }

    public static void setFontFallbacks(List<String> value) {
        config.set("font.fallbacks", value == null ? Collections.emptyList() : new ArrayList<>(value));
    }

    public static void setFontStyle(int value) {
        config.set("font.style", value);
    }

    public static void setFontSize(float value) {
        config.set("font.size", value);
    }

    public static void setFontOversample(float value) {
        config.set("font.oversample", value);
    }

    public static void setFontAutoBaseline(boolean value) {
        config.set("font.autoBaseline", value);
    }

    public static void setFontBaselineShift(float value) {
        config.set("font.baselineShift", value);
    }

    public static void setFontReferenceBaseline(float value) {
        config.set("font.referenceBaseline", value);
    }

    public static void setFontAntialias(boolean value) {
        config.set("font.antialias", value);
    }

    public static void setFontAntialiasMode(String value) {
        String mode = normalizeAntialiasMode(value);
        config.set("font.antialiasMode", mode);
        config.set("font.antialias", !"off".equals(mode));
    }

    public static void setFontFractionalMetrics(boolean value) {
        config.set("font.fractionalMetrics", value);
    }

    public static void setBuiltinFallbacksEnabled(boolean value) {
        config.set("font.builtinFallbacks", value);
    }

    public static void setShadowLength(float value) {
        config.set("shadow.length", value);
    }

    public static void setShadowOpacity(float value) {
        config.set("shadow.opacity", value);
    }

    public static void setRenderingInterpolation(boolean value) {
        config.set("rendering.interpolation", value);
    }

    public static void setRenderingMipmap(boolean value) {
        config.set("rendering.mipmap", value);
    }

    public static void setRenderingEngine(String value) {
        config.set("rendering.engine", normalizeRenderingEngine(value));
    }

    public static void setSkiaAdvancedStringMode(boolean value) {
        config.set("rendering.skiaAdvancedStringMode", value);
    }

    public static void setPerformanceAsyncInit(boolean value) {
        config.set("performance.asyncInit", value);
    }

    public static void setPerformancePrewarmBasicLatin(boolean value) {
        config.set("performance.prewarmBasicLatin", value);
    }

    public static void save() {
        if (config != null) {
            config.save();
        }
    }

    public static void load() {
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

        config = CommentedFileConfig.builder(configPath, TomlFormat.instance())
                .preserveInsertionOrder()
                .build();
        config.load();

        if (needsDefault) {
            addComments();
            config.save();
        }
        ensureFontDirectory();
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

    private static void writeDefaultConfig(File file) throws IOException {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.write("# Neo Font Render Configuration\n");
            w.write("\n");
            w.write("enabled = true\n");
            w.write("\n");
            w.write("[font]\n");
            w.write("name = \"SansSerif\"\n");
            w.write("fallbacks = [\"Serif\", \"Monospaced\"]\n");
            w.write("style = 0\n");
            w.write("size = 8.0\n");
            w.write("oversample = 8.0\n");
            w.write("autoBaseline = true\n");
            w.write("baselineShift = 0.0\n");
            w.write("referenceBaseline = 7.0\n");
            w.write("antialias = true\n");
            w.write("antialiasMode = \"on\"\n");
            w.write("fractionalMetrics = true\n");
            w.write("builtinFallbacks = true\n");
            w.write("\n");
            w.write("[shadow]\n");
            w.write("length = 1.0\n");
            w.write("opacity = 0.25\n");
            w.write("\n");
            w.write("[rendering]\n");
            w.write("engine = \"sfr\"\n");
            w.write("skiaAdvancedStringMode = true\n");
            w.write("interpolation = true\n");
            w.write("mipmap = false\n");
            w.write("\n");
            w.write("[performance]\n");
            w.write("asyncInit = true\n");
            w.write("prewarmBasicLatin = true\n");
            w.write("\n");
            w.write("[input]\n");
            w.write("allowSignPaste = true\n");
            w.write("\n");
            w.write("[debug]\n");
            w.write("imeInput = false\n");
        }
    }

    private static void addComments() {
        config.setComment("enabled", "Enable/disable the entire font replacement pipeline.");
        config.setComment("font", "Font selection and rasterization settings.");
        config.setComment("font.name", "Primary font name or TTF file path. Comma/semicolon-separated font family lists are also supported.");
        config.setComment("font.fallbacks", "Fallback font names or TTF file paths queried after font.name when a glyph is missing.");
        config.setComment("font.style", "Font style: 0=Plain, 1=Bold, 2=Italic, 3=Bold+Italic.");
        config.setComment("font.size", "Font size in pixels. 8.0 is close to vanilla 1.12 UI text height.");
        config.setComment("font.oversample", "Rasterization oversampling factor. Raster resolution is size * oversample; 8.0 at size 8.0 is a 64px glyph raster.");
        config.setComment("font.autoBaseline", "Align each font's measured AWT baseline to the Minecraft reference baseline before manual shift.");
        config.setComment("font.baselineShift", "Additional vertical glyph shift in Minecraft pixels after automatic baseline alignment. Positive moves glyphs down.");
        config.setComment("font.referenceBaseline", "Minecraft-space baseline used by autoBaseline. Vanilla 8px UI text is approximately 7.0.");
        config.setComment("font.antialias", "Enable AWT anti-aliasing during glyph rasterization.");
        config.setComment("font.antialiasMode", "AWT text anti-aliasing mode: off, on, gasp, lcd_hrgb, lcd_hbgr, lcd_vrgb, lcd_vbgr.");
        config.setComment("font.fractionalMetrics", "Enable fractional font metrics for more precise positioning.");
        config.setComment("font.builtinFallbacks", "Always append bundled fonts, such as Noto Color Emoji, to the fallback family.");
        config.setComment("shadow", "Text shadow rendering options.");
        config.setComment("shadow.length", "Shadow offset distance in pixels.");
        config.setComment("shadow.opacity", "Shadow opacity multiplier (0.0-1.0).");
        config.setComment("rendering", "OpenGL texture rendering options.");
        config.setComment("rendering.engine", "Text renderer engine: vanilla, sfr, or skia.");
        config.setComment("rendering.skiaAdvancedStringMode", "In Skia mode, render full formatted strings as one paragraph so shaping, ligatures, kerning, emoji ZWJ, and BiDi can work across the whole text. Disable to use legacy per-format-run rendering.");
        config.setComment("rendering.interpolation", "Use GL_LINEAR texture filtering instead of GL_NEAREST.");
        config.setComment("rendering.mipmap", "Enable mipmapping for font textures (may help at small sizes).");
        config.setComment("performance", "Performance tuning options.");
        config.setComment("performance.asyncInit", "Initialize font rasterization on a background thread.");
        config.setComment("performance.prewarmBasicLatin", "Pre-bake common Basic Latin and Latin-1 glyphs before enabling replacement rendering.");
        config.setComment("input", "Input behavior tweaks.");
        config.setComment("input.allowSignPaste", "Allow Ctrl+V paste in the vanilla sign editor. This is intentionally config-file only.");
        config.setComment("debug", "Debug logging options.");
        config.setComment("debug.imeInput", "Log IME input fix details to game log (for diagnosing emoji input issues).");
    }

    private static float getFloat(String key, float defaultValue) {
        Object val = config.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
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

    private static String normalizeFontLocation(String font) {
        if ("neofontrender:fonts/NotoColorEmoji-Regular.ttf".equals(font)) {
            return "neofontrender:fonts/noto_color_emoji_regular.ttf";
        }
        return font;
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
            return "sfr";
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
            case "skija":
            case "skia":
                return "skia";
            default:
                return "sfr";
        }
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
