package neofontrender.addons.tooltips;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Persistent modern-tooltip appearance and layout options. */
final class TooltipConfig {
    private static final int[] DEFAULT_FILL = {0xE6101018, 0xE6101018, 0xE6101018, 0xE6101018};
    private static final int[] DEFAULT_BORDER = {0xF0AADCF0, 0xF0DAD0F4, 0xF0DAD0F4, 0xF0AADCF0};
    private static final String DEFAULT_RENDER_STYLE = "modernui";
    private static final String DEFAULT_BORDER_SHADING = "gradient";
    private static final List<String> RENDER_STYLES = Arrays.asList("modernui", "mica", "legacy");
    private static final List<String> BORDER_SHADINGS = Arrays.asList("gradient", "solid", "horizontal", "vertical", "spectrum");

    static boolean enabled = true;
    static String renderStyle = DEFAULT_RENDER_STYLE;
    static boolean yieldToLegendaryTooltips = true;
    static boolean yieldToObscureTooltips = false;
    static boolean heiCustomTooltips = true;
    static boolean modNameEnabled = true;
    static String modNameFormat = "blue italic";
    static boolean rounded = true;
    static boolean centerTitle = true;
    static boolean titleBreak = true;
    static boolean adaptiveBorder = true;
    static String borderShading = DEFAULT_BORDER_SHADING;
    static int borderCycleMillis = 1000;
    static float cornerRadius = 4.0F;
    static float borderWidth = 1.25F;
    static float shadowRadius = 4.0F;
    static int shadowAlpha = 72;
    static int shadowColor = 0xFF000000;
    static float shadowOffsetX = 0.0F;
    static float shadowOffsetY = 2.0F;
    static int shadowSteps = 8;
    static int cornerSegments = 8;
    static float antialiasWidth = 0.55F;
    static boolean textShadow = true;
    static int textColor = 0xFFFFFFFF;
    static int titleColor = 0xFFFFFFFF;
    static int dividerAlpha = 176;
    static int horizontalPadding = 5;
    static int verticalPadding = 5;
    static int lineHeight = 10;
    static int titleGap = 3;
    static int cursorOffset = 12;
    static int maxWidth;
    static int[] fillColors = DEFAULT_FILL.clone();
    static int[] borderColors = DEFAULT_BORDER.clone();

    private TooltipConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        defineDefaults(file);
        enabled = file.getBoolean("tooltip.enabled", true);
        renderStyle = normalizeStyle(file.getString("tooltip.renderStyle", DEFAULT_RENDER_STYLE));
        yieldToLegendaryTooltips = file.getBoolean("tooltip.yieldToLegendaryTooltips", true);
        yieldToObscureTooltips = file.getBoolean("tooltip.yieldToObscureTooltips", false);
        heiCustomTooltips = file.getBoolean("tooltip.heiCustomTooltips", true);
        modNameEnabled = file.getBoolean("tooltip.modName.enabled", true);
        modNameFormat = file.getString("tooltip.modName.format", "blue italic");
        rounded = file.getBoolean("tooltip.rounded", true);
        centerTitle = file.getBoolean("tooltip.centerTitle", true);
        titleBreak = file.getBoolean("tooltip.titleBreak", true);
        adaptiveBorder = file.getBoolean("tooltip.adaptiveBorder", true);
        borderShading = normalizeBorderShading(file.getString("tooltip.borderShading", DEFAULT_BORDER_SHADING));
        borderCycleMillis = file.getInt("tooltip.borderCycleMillis", 1000, 250, 10000);
        cornerRadius = (float) file.getDouble("tooltip.cornerRadius", 4.0D, 0.0D, 16.0D);
        borderWidth = (float) file.getDouble("tooltip.borderWidth", 1.25D, 0.5D, 4.0D);
        shadowRadius = (float) file.getDouble("tooltip.shadowRadius", 4.0D, 0.0D, 12.0D);
        shadowAlpha = file.getInt("tooltip.shadowAlpha", 72, 0, 255);
        shadowColor = parseColor(file.getString("tooltip.shadowColor", "#FF000000"), 0xFF000000);
        shadowOffsetX = (float) file.getDouble("tooltip.shadowOffsetX", 0.0D, -16.0D, 16.0D);
        shadowOffsetY = (float) file.getDouble("tooltip.shadowOffsetY", 2.0D, -16.0D, 16.0D);
        shadowSteps = file.getInt("tooltip.shadowSteps", 8, 1, 64);
        cornerSegments = file.getInt("tooltip.cornerSegments", 8, 1, 64);
        antialiasWidth = (float) file.getDouble("tooltip.antialiasWidth", 0.55D, 0.0D, 4.0D);
        textShadow = file.getBoolean("text.shadow", true);
        textColor = parseColor(file.getString("text.color", "#FFFFFFFF"), 0xFFFFFFFF);
        titleColor = parseColor(file.getString("text.titleColor", "#FFFFFFFF"), 0xFFFFFFFF);
        dividerAlpha = file.getInt("text.dividerAlpha", 176, 0, 255);
        horizontalPadding = file.getInt("layout.horizontalPadding", 5, 1, 24);
        verticalPadding = file.getInt("layout.verticalPadding", 5, 1, 24);
        lineHeight = file.getInt("layout.lineHeight", 10, 8, 24);
        titleGap = file.getInt("layout.titleGap", 3, 0, 16);
        cursorOffset = file.getInt("layout.cursorOffset", 12, 0, 32);
        maxWidth = file.getInt("layout.maxWidth", 0, 0, 1024);
        fillColors = parseColors(file.getStringList("tooltip.fillColors", colorStrings(DEFAULT_FILL)), DEFAULT_FILL);
        borderColors = parseColors(
                file.getStringList("tooltip.borderColors", colorStrings(DEFAULT_BORDER)), DEFAULT_BORDER);
        file.save();
    }

    private static void defineDefaults(NfrConfigFile file) {
        file.define("tooltip.enabled", true, "Replace vanilla tooltip layout and background.")
                .define("tooltip.renderStyle", DEFAULT_RENDER_STYLE, "Tooltip renderer style: modernui, mica, or legacy.")
                .define("tooltip.yieldToLegendaryTooltips", true, "Let Legendary Tooltips render when it is installed.")
                .define("tooltip.yieldToObscureTooltips", false, "Let Obscure Tooltips keep its own panel.")
                .define("tooltip.heiCustomTooltips", true, "Apply NFR's panel to HEI ingredient-grid tooltips.")
                .define("tooltip.modName.enabled", true, "Append the owning mod's display name to item tooltips.")
                .define("tooltip.modName.format", "blue italic", "Space-separated formatting names.")
                .define("tooltip.rounded", true, "Draw rounded corners.")
                .define("tooltip.centerTitle", true, "Center the first tooltip line.")
                .define("tooltip.titleBreak", true, "Draw a divider after the title.")
                .define("tooltip.adaptiveBorder", true, "Derive border colors from item formatting and rarity.")
                .define("tooltip.borderShading", DEFAULT_BORDER_SHADING, "Border shading mode.")
                .define("tooltip.borderCycleMillis", 1000, "Spectrum cycle duration per corner.")
                .define("tooltip.cornerRadius", 4.0D, "Corner radius in GUI pixels.")
                .define("tooltip.borderWidth", 1.25D, "Border width in GUI pixels.")
                .define("tooltip.shadowRadius", 4.0D, "Soft shadow radius in GUI pixels.")
                .define("tooltip.shadowAlpha", 72, "Maximum shadow alpha.")
                .define("tooltip.shadowColor", "#FF000000", "ARGB shadow color.")
                .define("tooltip.shadowOffsetX", 0.0D, "Shadow X offset in GUI pixels.")
                .define("tooltip.shadowOffsetY", 2.0D, "Shadow Y offset in GUI pixels.")
                .define("tooltip.shadowSteps", 8, "Shadow tessellation step count.")
                .define("tooltip.cornerSegments", 8, "Rounded-corner tessellation segment count.")
                .define("tooltip.antialiasWidth", 0.55D, "Analytic shader anti-alias width.")
                .define("tooltip.fillColors", colorStrings(DEFAULT_FILL), "Four ARGB fill colors.")
                .define("tooltip.borderColors", colorStrings(DEFAULT_BORDER), "Four ARGB border colors.")
                .define("text.shadow", true, "Draw text shadows.")
                .define("text.color", "#FFFFFFFF", "ARGB body text color.")
                .define("text.titleColor", "#FFFFFFFF", "ARGB title text color.")
                .define("text.dividerAlpha", 176, "Title divider alpha.")
                .define("layout.horizontalPadding", 5, "Horizontal panel padding.")
                .define("layout.verticalPadding", 5, "Vertical panel padding.")
                .define("layout.lineHeight", 10, "Distance between lines.")
                .define("layout.titleGap", 3, "Additional gap after the title.")
                .define("layout.cursorOffset", 12, "Panel offset from the cursor.")
                .define("layout.maxWidth", 0, "Optional maximum text width; zero is automatic.");
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("tooltip.enabled", enabled)
                .set("tooltip.renderStyle", normalizeStyle(renderStyle))
                .set("tooltip.yieldToLegendaryTooltips", yieldToLegendaryTooltips)
                .set("tooltip.yieldToObscureTooltips", yieldToObscureTooltips)
                .set("tooltip.heiCustomTooltips", heiCustomTooltips)
                .set("tooltip.modName.enabled", modNameEnabled)
                .set("tooltip.modName.format", modNameFormat)
                .set("tooltip.rounded", rounded)
                .set("tooltip.centerTitle", centerTitle)
                .set("tooltip.titleBreak", titleBreak)
                .set("tooltip.adaptiveBorder", adaptiveBorder)
                .set("tooltip.borderShading", normalizeBorderShading(borderShading))
                .set("tooltip.borderCycleMillis", borderCycleMillis)
                .set("tooltip.cornerRadius", (double) cornerRadius)
                .set("tooltip.borderWidth", (double) borderWidth)
                .set("tooltip.shadowRadius", (double) shadowRadius)
                .set("tooltip.shadowAlpha", shadowAlpha)
                .set("tooltip.shadowColor", colorString(shadowColor))
                .set("tooltip.shadowOffsetX", (double) shadowOffsetX)
                .set("tooltip.shadowOffsetY", (double) shadowOffsetY)
                .set("tooltip.shadowSteps", shadowSteps)
                .set("tooltip.cornerSegments", cornerSegments)
                .set("tooltip.antialiasWidth", (double) antialiasWidth)
                .set("tooltip.fillColors", colorStrings(fillColors))
                .set("tooltip.borderColors", colorStrings(borderColors))
                .set("text.shadow", textShadow)
                .set("text.color", colorString(textColor))
                .set("text.titleColor", colorString(titleColor))
                .set("text.dividerAlpha", dividerAlpha)
                .set("layout.horizontalPadding", horizontalPadding)
                .set("layout.verticalPadding", verticalPadding)
                .set("layout.lineHeight", lineHeight)
                .set("layout.titleGap", titleGap)
                .set("layout.cursorOffset", cursorOffset)
                .set("layout.maxWidth", maxWidth)
                .save();
    }

    static Snapshot snapshot() {
        return new Snapshot();
    }

    static String normalizeStyle(String value) {
        return normalize(value, RENDER_STYLES, DEFAULT_RENDER_STYLE);
    }

    static String normalizeBorderShading(String value) {
        return normalize(value, BORDER_SHADINGS, DEFAULT_BORDER_SHADING);
    }

    static int parseColor(String input, int fallback) {
        if (input == null) return fallback;
        try {
            String value = input.trim();
            if (value.startsWith("#")) value = value.substring(1);
            if (value.length() == 6) value = "FF" + value;
            if (value.length() != 8) throw new IllegalArgumentException("Expected RGB or ARGB hexadecimal color");
            return (int) Long.parseLong(value, 16);
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Invalid tooltip color '{}'; using fallback", input, exception);
            return fallback;
        }
    }

    private static int[] parseColors(List<String> values, int[] fallback) {
        if (values == null || values.size() != 4) {
            NfrUiEnhancements.LOGGER.error("Tooltip palette must contain exactly four colors");
            return fallback.clone();
        }
        int[] result = new int[4];
        for (int index = 0; index < result.length; index++) {
            result[index] = parseColor(values.get(index), fallback[index]);
        }
        return result;
    }

    private static List<String> colorStrings(int[] colors) {
        String[] result = new String[colors.length];
        for (int index = 0; index < colors.length; index++) {
            result[index] = colorString(colors[index]);
        }
        return Arrays.asList(result);
    }

    private static String colorString(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    private static String normalize(String value, List<String> allowed, String fallback) {
        if (value == null) return fallback;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (allowed.contains(normalized)) return normalized;
        NfrUiEnhancements.LOGGER.error("Invalid tooltip option '{}'; using {}", value, fallback);
        return fallback;
    }

    static final class Snapshot {
        private final boolean enabled = TooltipConfig.enabled;
        private final String renderStyle = TooltipConfig.renderStyle;
        private final boolean yieldToLegendaryTooltips = TooltipConfig.yieldToLegendaryTooltips;
        private final boolean yieldToObscureTooltips = TooltipConfig.yieldToObscureTooltips;
        private final boolean heiCustomTooltips = TooltipConfig.heiCustomTooltips;
        private final boolean modNameEnabled = TooltipConfig.modNameEnabled;
        private final String modNameFormat = TooltipConfig.modNameFormat;
        private final boolean rounded = TooltipConfig.rounded;
        private final boolean centerTitle = TooltipConfig.centerTitle;
        private final boolean titleBreak = TooltipConfig.titleBreak;
        private final boolean adaptiveBorder = TooltipConfig.adaptiveBorder;
        private final String borderShading = TooltipConfig.borderShading;
        private final int borderCycleMillis = TooltipConfig.borderCycleMillis;
        private final float cornerRadius = TooltipConfig.cornerRadius;
        private final float borderWidth = TooltipConfig.borderWidth;
        private final float shadowRadius = TooltipConfig.shadowRadius;
        private final int shadowAlpha = TooltipConfig.shadowAlpha;
        private final int shadowColor = TooltipConfig.shadowColor;
        private final float shadowOffsetX = TooltipConfig.shadowOffsetX;
        private final float shadowOffsetY = TooltipConfig.shadowOffsetY;
        private final int shadowSteps = TooltipConfig.shadowSteps;
        private final int cornerSegments = TooltipConfig.cornerSegments;
        private final float antialiasWidth = TooltipConfig.antialiasWidth;
        private final boolean textShadow = TooltipConfig.textShadow;
        private final int textColor = TooltipConfig.textColor;
        private final int titleColor = TooltipConfig.titleColor;
        private final int dividerAlpha = TooltipConfig.dividerAlpha;
        private final int horizontalPadding = TooltipConfig.horizontalPadding;
        private final int verticalPadding = TooltipConfig.verticalPadding;
        private final int lineHeight = TooltipConfig.lineHeight;
        private final int titleGap = TooltipConfig.titleGap;
        private final int cursorOffset = TooltipConfig.cursorOffset;
        private final int maxWidth = TooltipConfig.maxWidth;
        private final int[] fillColors = TooltipConfig.fillColors.clone();
        private final int[] borderColors = TooltipConfig.borderColors.clone();

        void restore() {
            TooltipConfig.enabled = enabled;
            TooltipConfig.renderStyle = renderStyle;
            TooltipConfig.yieldToLegendaryTooltips = yieldToLegendaryTooltips;
            TooltipConfig.yieldToObscureTooltips = yieldToObscureTooltips;
            TooltipConfig.heiCustomTooltips = heiCustomTooltips;
            TooltipConfig.modNameEnabled = modNameEnabled;
            TooltipConfig.modNameFormat = modNameFormat;
            TooltipConfig.rounded = rounded;
            TooltipConfig.centerTitle = centerTitle;
            TooltipConfig.titleBreak = titleBreak;
            TooltipConfig.adaptiveBorder = adaptiveBorder;
            TooltipConfig.borderShading = borderShading;
            TooltipConfig.borderCycleMillis = borderCycleMillis;
            TooltipConfig.cornerRadius = cornerRadius;
            TooltipConfig.borderWidth = borderWidth;
            TooltipConfig.shadowRadius = shadowRadius;
            TooltipConfig.shadowAlpha = shadowAlpha;
            TooltipConfig.shadowColor = shadowColor;
            TooltipConfig.shadowOffsetX = shadowOffsetX;
            TooltipConfig.shadowOffsetY = shadowOffsetY;
            TooltipConfig.shadowSteps = shadowSteps;
            TooltipConfig.cornerSegments = cornerSegments;
            TooltipConfig.antialiasWidth = antialiasWidth;
            TooltipConfig.textShadow = textShadow;
            TooltipConfig.textColor = textColor;
            TooltipConfig.titleColor = titleColor;
            TooltipConfig.dividerAlpha = dividerAlpha;
            TooltipConfig.horizontalPadding = horizontalPadding;
            TooltipConfig.verticalPadding = verticalPadding;
            TooltipConfig.lineHeight = lineHeight;
            TooltipConfig.titleGap = titleGap;
            TooltipConfig.cursorOffset = cursorOffset;
            TooltipConfig.maxWidth = maxWidth;
            TooltipConfig.fillColors = fillColors.clone();
            TooltipConfig.borderColors = borderColors.clone();
        }
    }
}
