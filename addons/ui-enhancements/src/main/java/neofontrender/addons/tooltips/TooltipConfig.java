package neofontrender.addons.tooltips;

import neofontrender.api.config.NfrConfigFile;
import neofontrender.addons.ui.UiEnhancementsConfig;

import java.util.Arrays;
import java.util.List;

final class TooltipConfig {
    private static final int[] DEFAULT_FILL = defaults(0xE6101018);
    private static final int[] DEFAULT_BORDER = {0xF0AADCF0, 0xF0DAD0F4, 0xF0DAD0F4, 0xF0AADCF0};
    private static NfrConfigFile config;

    static boolean enabled = true;
    static String renderStyle = "modernui";
    static boolean yieldToLegendaryTooltips = true;
    static boolean yieldToObscureTooltips = false;
    static boolean rounded = true;
    static boolean centerTitle = true;
    static boolean titleBreak = true;
    static boolean adaptiveBorder = true;
    static String borderShading = "gradient";
    static int borderCycleMillis = 1000;
    static float cornerRadius = 4.0F;
    static float borderWidth = 1.25F;
    static float shadowRadius = 4.0F;
    static int shadowAlpha = 72;
    static float shadowOffsetX = 0.0F;
    static float shadowOffsetY = 2.0F;
    static int shadowColor = 0xFF000000;
    static int shadowSteps = 12;
    static int cornerSegments = 12;
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
    static int maxWidth = 0;
    static int[] fillColors = DEFAULT_FILL.clone();
    static int[] borderColors = DEFAULT_BORDER.clone();

    private TooltipConfig() {}

    static void load() {
        config = UiEnhancementsConfig.file();
        defineDefaults();
        enabled = config.getBoolean("tooltip.enabled", true);
        renderStyle = normalizeStyle(config.getString("tooltip.style", "modernui"));
        yieldToLegendaryTooltips = config.getBoolean("tooltip.yieldToLegendaryTooltips", true);
        yieldToObscureTooltips = config.getBoolean("tooltip.yieldToObscureTooltips", false);
        rounded = config.getBoolean("tooltip.rounded", true);
        centerTitle = config.getBoolean("tooltip.centerTitle", true);
        titleBreak = config.getBoolean("tooltip.titleBreak", true);
        adaptiveBorder = config.getBoolean("tooltip.adaptiveBorder", true);
        borderShading = normalizeBorderShading(config.getString("tooltip.borderShading", "gradient"));
        borderCycleMillis = config.getInt("tooltip.borderCycleMillis", 1000, 250, 10000);
        cornerRadius = (float) config.getDouble("tooltip.cornerRadius", 4.0D, 0.0D, 16.0D);
        borderWidth = (float) config.getDouble("tooltip.borderWidth", 1.25D, 0.5D, 4.0D);
        shadowRadius = (float) config.getDouble("tooltip.shadowRadius", 4.0D, 0.0D, 12.0D);
        shadowAlpha = config.getInt("tooltip.shadowAlpha", 72, 0, 255);
        shadowOffsetX = (float) config.getDouble("tooltip.shadowOffsetX", 0.0D, -12.0D, 12.0D);
        shadowOffsetY = (float) config.getDouble("tooltip.shadowOffsetY", 2.0D, -12.0D, 12.0D);
        shadowColor = parseColor(config.getString("tooltip.shadowColor", "#FF000000"), 0xFF000000);
        shadowSteps = config.getInt("quality.shadowSteps", 12, 2, 32);
        cornerSegments = config.getInt("quality.cornerSegments", 12, 3, 32);
        antialiasWidth = (float) config.getDouble("quality.antialiasWidth", 0.55D, 0.0D, 1.5D);
        textShadow = config.getBoolean("text.shadow", true);
        textColor = parseColor(config.getString("text.color", "#FFFFFFFF"), 0xFFFFFFFF);
        titleColor = parseColor(config.getString("text.titleColor", "#FFFFFFFF"), 0xFFFFFFFF);
        dividerAlpha = config.getInt("text.dividerAlpha", 176, 0, 255);
        horizontalPadding = config.getInt("layout.horizontalPadding", 5, 1, 24);
        verticalPadding = config.getInt("layout.verticalPadding", 5, 1, 24);
        lineHeight = config.getInt("layout.lineHeight", 10, 8, 24);
        titleGap = config.getInt("layout.titleGap", 3, 0, 16);
        cursorOffset = config.getInt("layout.cursorOffset", 12, 0, 32);
        maxWidth = config.getInt("layout.maxWidth", 0, 0, 1024);
        fillColors = parseColors(config.getStringList("tooltip.fillColors", colorStrings(DEFAULT_FILL)), DEFAULT_FILL);
        borderColors = parseColors(config.getStringList("tooltip.borderColors", colorStrings(DEFAULT_BORDER)), DEFAULT_BORDER);
        config.save();
    }

    static void save() {
        config.set("tooltip.enabled", enabled)
                .set("tooltip.style", renderStyle)
                .set("tooltip.yieldToLegendaryTooltips", yieldToLegendaryTooltips)
                .set("tooltip.yieldToObscureTooltips", yieldToObscureTooltips)
                .set("tooltip.rounded", rounded)
                .set("tooltip.centerTitle", centerTitle)
                .set("tooltip.titleBreak", titleBreak)
                .set("tooltip.adaptiveBorder", adaptiveBorder)
                .set("tooltip.borderShading", borderShading)
                .set("tooltip.borderCycleMillis", borderCycleMillis)
                .set("tooltip.cornerRadius", cornerRadius)
                .set("tooltip.borderWidth", borderWidth)
                .set("tooltip.shadowRadius", shadowRadius)
                .set("tooltip.shadowAlpha", shadowAlpha)
                .set("tooltip.shadowOffsetX", shadowOffsetX)
                .set("tooltip.shadowOffsetY", shadowOffsetY)
                .set("tooltip.shadowColor", colorString(shadowColor))
                .set("quality.shadowSteps", shadowSteps)
                .set("quality.cornerSegments", cornerSegments)
                .set("quality.antialiasWidth", antialiasWidth)
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
                .set("tooltip.fillColors", colorStrings(fillColors))
                .set("tooltip.borderColors", colorStrings(borderColors));
        config.save();
    }

    static Snapshot snapshot() { return new Snapshot(); }

    private static void defineDefaults() {
        config.define("tooltip.enabled", true, "Replace Forge 1.12.2 tooltip layout and background.")
                .define("tooltip.style", "modernui", "Renderer style: modernui, mica or legacy.")
                .define("tooltip.yieldToLegendaryTooltips", true, "Yield when LegendaryTooltips is installed.")
                .define("tooltip.yieldToObscureTooltips", false, "Let Obscure Tooltips draw its own panel and frame instead of combining its effects with NFR's modern panel.")
                .define("tooltip.rounded", true, "Draw rounded antialiased corners.")
                .define("tooltip.centerTitle", true, "Center the first tooltip line.")
                .define("tooltip.titleBreak", true, "Draw a divider after the title.")
                .define("tooltip.adaptiveBorder", true, "Derive border colors from formatted title colors, rarity and enchantment.")
                .define("tooltip.borderShading", "gradient", "Border shading: gradient, solid, horizontal, vertical or spectrum.")
                .define("tooltip.borderCycleMillis", 1000, "Milliseconds for a spectrum color to move to the next corner.")
                .define("tooltip.cornerRadius", 4.0D, "Corner radius in GUI pixels (0-16).")
                .define("tooltip.borderWidth", 1.25D, "Border width in GUI pixels (0.5-4).")
                .define("tooltip.shadowRadius", 4.0D, "Soft shadow radius in GUI pixels (0-12).")
                .define("tooltip.shadowAlpha", 72, "Maximum shadow alpha (0-255).")
                .define("tooltip.shadowOffsetX", 0.0D, "Horizontal shadow offset in GUI pixels.")
                .define("tooltip.shadowOffsetY", 2.0D, "Vertical shadow offset in GUI pixels.")
                .define("tooltip.shadowColor", "#FF000000", "ARGB shadow color.")
                .define("quality.shadowSteps", 12, "Number of continuous shadow gradient rings (2-32).")
                .define("quality.cornerSegments", 12, "Curve segments per rounded corner (3-32).")
                .define("quality.antialiasWidth", 0.55D, "Outer edge coverage width in GUI pixels.")
                .define("text.shadow", true, "Draw Minecraft's text shadow.")
                .define("text.color", "#FFFFFFFF", "ARGB body text color.")
                .define("text.titleColor", "#FFFFFFFF", "ARGB title text color.")
                .define("text.dividerAlpha", 176, "Title divider alpha (0-255).")
                .define("layout.horizontalPadding", 5, "Horizontal background padding.")
                .define("layout.verticalPadding", 5, "Vertical background padding.")
                .define("layout.lineHeight", 10, "Distance between text baselines.")
                .define("layout.titleGap", 3, "Extra gap after the title.")
                .define("layout.cursorOffset", 12, "Distance from the mouse cursor.")
                .define("layout.maxWidth", 0, "Maximum text width; zero uses Forge/screen limits.")
                .define("tooltip.fillColors", colorStrings(DEFAULT_FILL), "Four ARGB colors: UL, UR, LR, LL.")
                .define("tooltip.borderColors", colorStrings(DEFAULT_BORDER), "Four ARGB colors: UL, UR, LR, LL.");
    }

    private static int[] parseColors(List<String> values, int[] fallback) {
        if (values == null || values.size() != 4) return fallback.clone();
        int[] parsed = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                String value = values.get(i).trim();
                if (value.startsWith("#")) value = value.substring(1);
                long raw = Long.parseLong(value, 16);
                if (value.length() <= 6) raw |= 0xFF000000L;
                parsed[i] = (int) raw;
            }
            return parsed;
        } catch (RuntimeException ignored) {
            return fallback.clone();
        }
    }

    static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        try {
            String normalized = value.trim();
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            long raw = Long.parseLong(normalized, 16);
            if (normalized.length() <= 6) raw |= 0xFF000000L;
            return (int) raw;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static List<String> colorStrings(int[] colors) {
        String[] values = new String[colors.length];
        for (int i = 0; i < colors.length; i++) values[i] = String.format("#%08X", colors[i]);
        return Arrays.asList(values);
    }

    static String colorString(int color) { return String.format("#%08X", color); }

    static String normalizeStyle(String value) {
        if ("legacy".equalsIgnoreCase(value)) return "legacy";
        if ("mica".equalsIgnoreCase(value)) return "mica";
        return "modernui";
    }

    static String normalizeBorderShading(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
            if ("solid".equals(normalized) || "horizontal".equals(normalized)
                    || "vertical".equals(normalized) || "spectrum".equals(normalized)) {
                return normalized;
            }
        }
        return "gradient";
    }

    private static int[] defaults(int color) { return new int[]{color, color, color, color}; }

    static final class Snapshot {
        private final boolean originalEnabled = enabled;
        private final String originalRenderStyle = renderStyle;
        private final boolean originalYield = yieldToLegendaryTooltips;
        private final boolean originalYieldObscure = yieldToObscureTooltips;
        private final boolean originalRounded = rounded;
        private final boolean originalCenterTitle = centerTitle;
        private final boolean originalTitleBreak = titleBreak;
        private final boolean originalAdaptive = adaptiveBorder;
        private final String originalBorderShading = borderShading;
        private final int originalBorderCycleMillis = borderCycleMillis;
        private final float originalCorner = cornerRadius;
        private final float originalBorder = borderWidth;
        private final float originalShadow = shadowRadius;
        private final int originalAlpha = shadowAlpha;
        private final float originalShadowX = shadowOffsetX;
        private final float originalShadowY = shadowOffsetY;
        private final int originalShadowColor = shadowColor;
        private final int originalShadowSteps = shadowSteps;
        private final int originalCornerSegments = cornerSegments;
        private final float originalAa = antialiasWidth;
        private final boolean originalTextShadow = textShadow;
        private final int originalTextColor = textColor;
        private final int originalTitleColor = titleColor;
        private final int originalDividerAlpha = dividerAlpha;
        private final int originalHorizontalPadding = horizontalPadding;
        private final int originalVerticalPadding = verticalPadding;
        private final int originalLineHeight = lineHeight;
        private final int originalTitleGap = titleGap;
        private final int originalCursorOffset = cursorOffset;
        private final int originalMaxWidth = maxWidth;
        private final int[] originalFill = fillColors.clone();
        private final int[] originalBorderColors = borderColors.clone();

        void restore() {
            enabled = originalEnabled; renderStyle = originalRenderStyle;
            yieldToLegendaryTooltips = originalYield; rounded = originalRounded;
            yieldToObscureTooltips = originalYieldObscure;
            centerTitle = originalCenterTitle; titleBreak = originalTitleBreak; adaptiveBorder = originalAdaptive;
            borderShading = originalBorderShading; borderCycleMillis = originalBorderCycleMillis;
            cornerRadius = originalCorner; borderWidth = originalBorder; shadowRadius = originalShadow;
            shadowAlpha = originalAlpha;
            shadowOffsetX = originalShadowX; shadowOffsetY = originalShadowY; shadowColor = originalShadowColor;
            shadowSteps = originalShadowSteps; cornerSegments = originalCornerSegments; antialiasWidth = originalAa;
            textShadow = originalTextShadow; textColor = originalTextColor; titleColor = originalTitleColor;
            dividerAlpha = originalDividerAlpha; horizontalPadding = originalHorizontalPadding;
            verticalPadding = originalVerticalPadding; lineHeight = originalLineHeight; titleGap = originalTitleGap;
            cursorOffset = originalCursorOffset; maxWidth = originalMaxWidth;
            fillColors = originalFill.clone(); borderColors = originalBorderColors.clone();
        }
    }
}
