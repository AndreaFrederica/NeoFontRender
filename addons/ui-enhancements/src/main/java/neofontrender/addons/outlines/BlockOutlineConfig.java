package neofontrender.addons.outlines;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class BlockOutlineConfig {
    static final String MODE_GEOMETRY = "geometry";
    static final String MODE_NATIVE = "native";
    static final String PATTERN_SOLID = "solid";
    static final String PATTERN_DASHED = "dashed";
    static final String PATTERN_DOTTED = "dotted";
    static final String CAP_SQUARE = "square";
    static final String CAP_ROUND = "round";
    static final String DEPTH_VISIBLE = "visible";
    static final String DEPTH_ALWAYS = "always";
    static final String DEPTH_XRAY = "xray";
    static final String BLEND_ALPHA = "alpha";
    static final String BLEND_ADDITIVE = "additive";

    static boolean enabled = true;
    static String renderMode = MODE_GEOMETRY;
    static float globalLineWidth = 2.0F;
    static int globalColor = 0x66000000;
    static float outlineOpacity = 1.0F;
    static float outlineBrightness = 1.0F;
    static boolean rainbowEnabled;
    static float rainbowCycleMillis = 3000.0F;
    static float rainbowDensity = 1.0F;
    static boolean glowEnabled;
    static float glowRadius = 4.0F;
    static float glowIntensity = 0.65F;
    static float glowFalloff = 2.0F;
    static List<String> blockOverrides = Collections.emptyList();
    static boolean noHarvestEnabled;
    static float noHarvestLineWidth = 2.0F;
    static int noHarvestColor = 0x66FF0000;
    static List<String> noHarvestOverrides = Collections.emptyList();
    static boolean antialias = true;
    static float antialiasWidth = 1.0F;
    static String pattern = PATTERN_SOLID;
    static String cap = CAP_ROUND;
    static float dashLength = 8.0F;
    static float dashGap = 5.0F;
    static String depthMode = DEPTH_VISIBLE;
    static float xrayHiddenOpacity = 0.35F;
    static float expansion = 0.0F;
    static boolean fillEnabled;
    static float fillOpacity = 0.15F;
    static String blendMode = BLEND_ALPHA;
    static boolean pulseEnabled;
    static float pulsePeriodMillis = 1500.0F;
    static float pulseMinimumAlpha = 0.35F;

    private BlockOutlineConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("outlines.enabled", true, "Master switch for configurable block-selection outlines.")
                .define("outlines.renderMode", MODE_GEOMETRY,
                        "geometry uses framebuffer pixels; native uses the driver's GL line-width units.")
                .define("outlines.globalLineWidth", 2.0D, "Default selection-outline width (0.5-64).")
                .define("outlines.globalColor", "#66000000", "Default selection-outline color in #AARRGGBB format.")
                .define("outlines.opacity", 1.0D, "Outline alpha multiplier (0-1).")
                .define("outlines.brightness", 1.0D, "Outline RGB intensity multiplier (0-4).")
                .define("outlines.rainbow.enabled", false,
                        "Animate a spatial rainbow gradient along geometry outlines.")
                .define("outlines.rainbow.cycleMillis", 3000.0D,
                        "Milliseconds for the rainbow gradient to complete one color cycle.")
                .define("outlines.rainbow.density", 1.0D,
                        "Number of rainbow cycles distributed across the selection box (0.25-4).")
                .define("outlines.glow.enabled", false,
                        "Draw an additive distance-field halo around geometry outlines.")
                .define("outlines.glow.radius", 4.0D, "Glow radius in framebuffer pixels (0.5-24).")
                .define("outlines.glow.intensity", 0.65D, "Glow opacity multiplier (0-2).")
                .define("outlines.glow.falloff", 2.0D, "Glow edge falloff exponent (0.5-4).")
                .define("outlines.blockOverrides", Collections.emptyList(),
                        "Per-block rules: modid:block[:meta]=width;#AARRGGBB")
                .define("outlines.noHarvest.enabled", false,
                        "Use a distinct outline when the player cannot harvest the selected block.")
                .define("outlines.noHarvest.lineWidth", 2.0D, "Default non-harvestable outline width (0.5-64).")
                .define("outlines.noHarvest.color", "#66FF0000",
                        "Default non-harvestable outline color in #AARRGGBB format.")
                .define("outlines.noHarvest.blockOverrides", Collections.emptyList(),
                        "Per-block non-harvestable rules: modid:block[:meta]=width;#AARRGGBB")
                .define("outlines.antialias", true, "Apply analytic edge antialiasing in geometry mode.")
                .define("outlines.antialiasWidth", 1.0D, "Geometry edge feather width in framebuffer pixels.")
                .define("outlines.pattern", PATTERN_SOLID, "Outline pattern: solid, dashed, or dotted.")
                .define("outlines.cap", CAP_ROUND, "Geometry line caps: square or round.")
                .define("outlines.dashLength", 8.0D, "Dash or dot spacing length in framebuffer pixels.")
                .define("outlines.dashGap", 5.0D, "Pattern gap in framebuffer pixels.")
                .define("outlines.depthMode", DEPTH_VISIBLE, "Depth mode: visible, always, or xray.")
                .define("outlines.xrayHiddenOpacity", 0.35D, "Opacity multiplier for the hidden x-ray pass.")
                .define("outlines.expansion", 0.0D, "Additional outline expansion in world block units.")
                .define("outlines.fill.enabled", false, "Draw a translucent fill inside the selected block.")
                .define("outlines.fill.opacity", 0.15D, "Fill opacity relative to the outline alpha.")
                .define("outlines.blendMode", BLEND_ALPHA, "Blend mode: alpha or additive.")
                .define("outlines.pulse.enabled", false, "Animate outline and fill opacity.")
                .define("outlines.pulse.periodMillis", 1500.0D, "Pulse cycle duration in milliseconds.")
                .define("outlines.pulse.minimumAlpha", 0.35D, "Minimum pulse alpha multiplier.");
        enabled = file.getBoolean("outlines.enabled", true);
        renderMode = normalizeRenderMode(file.getString("outlines.renderMode", MODE_GEOMETRY));
        globalLineWidth = (float) file.getDouble("outlines.globalLineWidth", 2.0D, 0.5D, 64.0D);
        globalColor = parseColor(file.getString("outlines.globalColor", "#66000000"), 0x66000000);
        outlineOpacity = (float) file.getDouble("outlines.opacity", 1.0D, 0.0D, 1.0D);
        outlineBrightness = (float) file.getDouble("outlines.brightness", 1.0D, 0.0D, 4.0D);
        rainbowEnabled = file.getBoolean("outlines.rainbow.enabled", false);
        rainbowCycleMillis = (float) file.getDouble("outlines.rainbow.cycleMillis", 3000.0D,
                250.0D, 20000.0D);
        rainbowDensity = (float) file.getDouble("outlines.rainbow.density", 1.0D, 0.25D, 4.0D);
        glowEnabled = file.getBoolean("outlines.glow.enabled", false);
        glowRadius = (float) file.getDouble("outlines.glow.radius", 4.0D, 0.5D, 24.0D);
        glowIntensity = (float) file.getDouble("outlines.glow.intensity", 0.65D, 0.0D, 2.0D);
        glowFalloff = (float) file.getDouble("outlines.glow.falloff", 2.0D, 0.5D, 4.0D);
        blockOverrides = mutable(file.getStringList("outlines.blockOverrides", Collections.emptyList()));
        noHarvestEnabled = file.getBoolean("outlines.noHarvest.enabled", false);
        noHarvestLineWidth = (float) file.getDouble("outlines.noHarvest.lineWidth", 2.0D, 0.5D, 64.0D);
        noHarvestColor = parseColor(file.getString("outlines.noHarvest.color", "#66FF0000"), 0x66FF0000);
        noHarvestOverrides = mutable(file.getStringList("outlines.noHarvest.blockOverrides", Collections.emptyList()));
        antialias = file.getBoolean("outlines.antialias", true);
        antialiasWidth = (float) file.getDouble("outlines.antialiasWidth", 1.0D, 0.25D, 4.0D);
        pattern = normalizePattern(file.getString("outlines.pattern", PATTERN_SOLID));
        cap = normalizeCap(file.getString("outlines.cap", CAP_ROUND));
        dashLength = (float) file.getDouble("outlines.dashLength", 8.0D, 1.0D, 64.0D);
        dashGap = (float) file.getDouble("outlines.dashGap", 5.0D, 0.5D, 64.0D);
        depthMode = normalizeDepthMode(file.getString("outlines.depthMode", DEPTH_VISIBLE));
        xrayHiddenOpacity = (float) file.getDouble("outlines.xrayHiddenOpacity", 0.35D, 0.0D, 1.0D);
        expansion = (float) file.getDouble("outlines.expansion", 0.0D, 0.0D, 0.25D);
        fillEnabled = file.getBoolean("outlines.fill.enabled", false);
        fillOpacity = (float) file.getDouble("outlines.fill.opacity", 0.15D, 0.0D, 1.0D);
        blendMode = normalizeBlendMode(file.getString("outlines.blendMode", BLEND_ALPHA));
        pulseEnabled = file.getBoolean("outlines.pulse.enabled", false);
        pulsePeriodMillis = (float) file.getDouble("outlines.pulse.periodMillis", 1500.0D, 250.0D, 10000.0D);
        pulseMinimumAlpha = (float) file.getDouble("outlines.pulse.minimumAlpha", 0.35D, 0.0D, 1.0D);
        BlockOutlineResolver.reload();
        file.save();
    }

    static void save() {
        BlockOutlineResolver.reload();
        UiEnhancementsConfig.file().set("outlines.enabled", enabled)
                .set("outlines.renderMode", normalizeRenderMode(renderMode))
                .set("outlines.globalLineWidth", (double) globalLineWidth)
                .set("outlines.globalColor", formatColor(globalColor))
                .set("outlines.opacity", (double) outlineOpacity)
                .set("outlines.brightness", (double) outlineBrightness)
                .set("outlines.rainbow.enabled", rainbowEnabled)
                .set("outlines.rainbow.cycleMillis", (double) rainbowCycleMillis)
                .set("outlines.rainbow.density", (double) rainbowDensity)
                .set("outlines.glow.enabled", glowEnabled)
                .set("outlines.glow.radius", (double) glowRadius)
                .set("outlines.glow.intensity", (double) glowIntensity)
                .set("outlines.glow.falloff", (double) glowFalloff)
                .set("outlines.blockOverrides", new ArrayList<>(blockOverrides))
                .set("outlines.noHarvest.enabled", noHarvestEnabled)
                .set("outlines.noHarvest.lineWidth", (double) noHarvestLineWidth)
                .set("outlines.noHarvest.color", formatColor(noHarvestColor))
                .set("outlines.noHarvest.blockOverrides", new ArrayList<>(noHarvestOverrides))
                .set("outlines.antialias", antialias)
                .set("outlines.antialiasWidth", (double) antialiasWidth)
                .set("outlines.pattern", normalizePattern(pattern))
                .set("outlines.cap", normalizeCap(cap))
                .set("outlines.dashLength", (double) dashLength)
                .set("outlines.dashGap", (double) dashGap)
                .set("outlines.depthMode", normalizeDepthMode(depthMode))
                .set("outlines.xrayHiddenOpacity", (double) xrayHiddenOpacity)
                .set("outlines.expansion", (double) expansion)
                .set("outlines.fill.enabled", fillEnabled)
                .set("outlines.fill.opacity", (double) fillOpacity)
                .set("outlines.blendMode", normalizeBlendMode(blendMode))
                .set("outlines.pulse.enabled", pulseEnabled)
                .set("outlines.pulse.periodMillis", (double) pulsePeriodMillis)
                .set("outlines.pulse.minimumAlpha", (double) pulseMinimumAlpha)
                .save();
    }

    static Snapshot snapshot() { return new Snapshot(); }

    static List<String> parseEditorRules(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String entry : value.split("[,\\r\\n]+")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    static String editorRules(List<String> values) { return String.join(", ", values); }

    static String normalizeRenderMode(String value) {
        return MODE_NATIVE.equals(normalize(value)) ? MODE_NATIVE : MODE_GEOMETRY;
    }

    static String normalizePattern(String value) {
        String normalized = normalize(value);
        if (PATTERN_DASHED.equals(normalized) || PATTERN_DOTTED.equals(normalized)) return normalized;
        return PATTERN_SOLID;
    }

    static String normalizeCap(String value) {
        return CAP_SQUARE.equals(normalize(value)) ? CAP_SQUARE : CAP_ROUND;
    }

    static String normalizeDepthMode(String value) {
        String normalized = normalize(value);
        if (DEPTH_ALWAYS.equals(normalized) || DEPTH_XRAY.equals(normalized)) return normalized;
        return DEPTH_VISIBLE;
    }

    static String normalizeBlendMode(String value) {
        return BLEND_ADDITIVE.equals(normalize(value)) ? BLEND_ADDITIVE : BLEND_ALPHA;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static List<String> mutable(List<String> values) { return new ArrayList<>(values); }

    private static int parseColor(String value, int fallback) {
        try {
            String hex = value == null ? "" : value.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() != 8) return fallback;
            return (int) Long.parseLong(hex, 16);
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.warn("Invalid block outline color: {}", value);
            return fallback;
        }
    }

    private static String formatColor(int value) { return String.format("#%08X", value); }

    static final class Snapshot {
        private final boolean enabledValue = enabled;
        private final String renderModeValue = renderMode;
        private final float globalLineWidthValue = globalLineWidth;
        private final int globalColorValue = globalColor;
        private final float outlineOpacityValue = outlineOpacity;
        private final float outlineBrightnessValue = outlineBrightness;
        private final boolean rainbowEnabledValue = rainbowEnabled;
        private final float rainbowCycleMillisValue = rainbowCycleMillis;
        private final float rainbowDensityValue = rainbowDensity;
        private final boolean glowEnabledValue = glowEnabled;
        private final float glowRadiusValue = glowRadius;
        private final float glowIntensityValue = glowIntensity;
        private final float glowFalloffValue = glowFalloff;
        private final List<String> blockOverridesValue = mutable(blockOverrides);
        private final boolean noHarvestEnabledValue = noHarvestEnabled;
        private final float noHarvestLineWidthValue = noHarvestLineWidth;
        private final int noHarvestColorValue = noHarvestColor;
        private final List<String> noHarvestOverridesValue = mutable(noHarvestOverrides);
        private final boolean antialiasValue = antialias;
        private final float antialiasWidthValue = antialiasWidth;
        private final String patternValue = pattern;
        private final String capValue = cap;
        private final float dashLengthValue = dashLength;
        private final float dashGapValue = dashGap;
        private final String depthModeValue = depthMode;
        private final float xrayHiddenOpacityValue = xrayHiddenOpacity;
        private final float expansionValue = expansion;
        private final boolean fillEnabledValue = fillEnabled;
        private final float fillOpacityValue = fillOpacity;
        private final String blendModeValue = blendMode;
        private final boolean pulseEnabledValue = pulseEnabled;
        private final float pulsePeriodMillisValue = pulsePeriodMillis;
        private final float pulseMinimumAlphaValue = pulseMinimumAlpha;

        void restore() {
            enabled = enabledValue;
            renderMode = renderModeValue;
            globalLineWidth = globalLineWidthValue;
            globalColor = globalColorValue;
            outlineOpacity = outlineOpacityValue;
            outlineBrightness = outlineBrightnessValue;
            rainbowEnabled = rainbowEnabledValue;
            rainbowCycleMillis = rainbowCycleMillisValue;
            rainbowDensity = rainbowDensityValue;
            glowEnabled = glowEnabledValue;
            glowRadius = glowRadiusValue;
            glowIntensity = glowIntensityValue;
            glowFalloff = glowFalloffValue;
            blockOverrides = mutable(blockOverridesValue);
            noHarvestEnabled = noHarvestEnabledValue;
            noHarvestLineWidth = noHarvestLineWidthValue;
            noHarvestColor = noHarvestColorValue;
            noHarvestOverrides = mutable(noHarvestOverridesValue);
            antialias = antialiasValue;
            antialiasWidth = antialiasWidthValue;
            pattern = patternValue;
            cap = capValue;
            dashLength = dashLengthValue;
            dashGap = dashGapValue;
            depthMode = depthModeValue;
            xrayHiddenOpacity = xrayHiddenOpacityValue;
            expansion = expansionValue;
            fillEnabled = fillEnabledValue;
            fillOpacity = fillOpacityValue;
            blendMode = blendModeValue;
            pulseEnabled = pulseEnabledValue;
            pulsePeriodMillis = pulsePeriodMillisValue;
            pulseMinimumAlpha = pulseMinimumAlphaValue;
            BlockOutlineResolver.reload();
        }
    }
}
