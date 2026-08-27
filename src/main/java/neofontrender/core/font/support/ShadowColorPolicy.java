package neofontrender.core.font.support;

/** Shared color selection for legacy two-pass and modern composed text shadows. */
public final class ShadowColorPolicy {
    public static final String VANILLA = "vanilla";
    public static final String COLORED = "colored";
    public static final String SOLID = "solid";
    public static final String COLORED_FUNCTION_SRGB = "srgb";
    public static final String COLORED_FUNCTION_LINEAR_LIGHT = "linear_light";
    public static final float DEFAULT_COLORED_RATIO = 0.25F;

    private ShadowColorPolicy() {}

    public static String normalizeMode(String mode) {
        if (COLORED.equalsIgnoreCase(mode)) return COLORED;
        if (SOLID.equalsIgnoreCase(mode)) return SOLID;
        return VANILLA;
    }

    /** Maps an arbitrary foreground color using the vanilla per-channel quarter-brightness rule. */
    public static int darken(int foregroundArgb) {
        return darken(foregroundArgb, DEFAULT_COLORED_RATIO, COLORED_FUNCTION_SRGB);
    }

    /** Maps a foreground color with the configured colored-shadow function and brightness ratio. */
    public static int darken(int foregroundArgb, float ratio, String function) {
        float amount = clampRatio(ratio);
        String normalized = normalizeColoredFunction(function);
        int red = foregroundArgb >> 16 & 255;
        int green = foregroundArgb >> 8 & 255;
        int blue = foregroundArgb & 255;
        if (COLORED_FUNCTION_LINEAR_LIGHT.equals(normalized)) {
            red = scaleLinearLight(red, amount);
            green = scaleLinearLight(green, amount);
            blue = scaleLinearLight(blue, amount);
        } else {
            red = (int) (red * amount);
            green = (int) (green * amount);
            blue = (int) (blue * amount);
        }
        return foregroundArgb & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    /** Resolves an unformatted run's shadow color and then applies explicit overrides. */
    public static int shadowColor(int foregroundArgb, String mode, int configuredArgb,
                                  ShadowColorRemapRules rules, int[] palette) {
        return shadowColor(foregroundArgb, mode, configuredArgb, rules, palette,
                DEFAULT_COLORED_RATIO, COLORED_FUNCTION_SRGB);
    }

    public static int shadowColor(int foregroundArgb, String mode, int configuredArgb,
                                  ShadowColorRemapRules rules, int[] palette,
                                  float ratio, String function) {
        String normalized = normalizeMode(mode);
        int color = SOLID.equals(normalized) ? configuredArgb
                : darken(foregroundArgb, ratio, function);
        return remap(foregroundArgb, color, rules, palette);
    }

    /** Resolves a formatted palette run's foreground or shadow color. */
    public static int paletteColor(int foregroundIndex, int alpha, boolean shadow, String mode,
                                   int configuredArgb, ShadowColorRemapRules rules, int[] palette) {
        return paletteColor(foregroundIndex, alpha, shadow, mode, configuredArgb, rules, palette,
                DEFAULT_COLORED_RATIO, COLORED_FUNCTION_SRGB);
    }

    public static int paletteColor(int foregroundIndex, int alpha, boolean shadow, String mode,
                                   int configuredArgb, ShadowColorRemapRules rules, int[] palette,
                                   float ratio, String function) {
        int[] colors = palette == null || palette.length < 32
                ? neofontrender.api.color.TextColorPaletteRegistry.vanillaColorCodes() : palette;
        int foreground = (alpha & 0xFF000000) | (colors[foregroundIndex & 15] & 0xFFFFFF);
        if (!shadow) return foreground;
        String normalized = normalizeMode(mode);
        int color;
        if (SOLID.equals(normalized)) {
            color = configuredArgb;
        } else if (VANILLA.equals(normalized)) {
            color = (alpha & 0xFF000000) | (colors[(foregroundIndex & 15) + 16] & 0xFFFFFF);
        } else {
            color = darken(foreground, ratio, function);
        }
        return remap(foreground, color, rules, colors);
    }

    /** Resolves a modern per-run shadow color. */
    public static int modernColor(int foregroundArgb, int configuredArgb, String mode,
                                  ShadowColorRemapRules rules, int[] palette) {
        return modernColor(foregroundArgb, configuredArgb, mode, rules, palette,
                DEFAULT_COLORED_RATIO, COLORED_FUNCTION_SRGB);
    }

    public static int modernColor(int foregroundArgb, int configuredArgb, String mode,
                                  ShadowColorRemapRules rules, int[] palette,
                                  float ratio, String function) {
        return shadowColor(foregroundArgb, mode, configuredArgb, rules, palette, ratio, function);
    }

    /** Selects the foreground or vanilla shadow half of a 32-entry Minecraft palette. */
    public static int paletteIndex(int foregroundIndex, boolean shadow, String mode) {
        return foregroundIndex + (shadow && VANILLA.equals(normalizeMode(mode)) ? 16 : 0);
    }

    private static int remap(int foregroundArgb, int shadowArgb,
                             ShadowColorRemapRules rules, int[] palette) {
        return rules == null ? shadowArgb : rules.remap(foregroundArgb, shadowArgb, palette);
    }

    public static String normalizeColoredFunction(String function) {
        return COLORED_FUNCTION_LINEAR_LIGHT.equalsIgnoreCase(function)
                ? COLORED_FUNCTION_LINEAR_LIGHT : COLORED_FUNCTION_SRGB;
    }

    public static float clampRatio(float ratio) {
        if (!Float.isFinite(ratio)) return DEFAULT_COLORED_RATIO;
        return Math.max(0.0F, Math.min(1.0F, ratio));
    }

    private static int scaleLinearLight(int channel, float ratio) {
        float encoded = channel / 255.0F;
        float linear = encoded <= 0.04045F
                ? encoded / 12.92F
                : (float) Math.pow((encoded + 0.055F) / 1.055F, 2.4F);
        float scaled = Math.max(0.0F, Math.min(1.0F, linear * ratio));
        float output = scaled <= 0.0031308F
                ? scaled * 12.92F
                : 1.055F * (float) Math.pow(scaled, 1.0F / 2.4F) - 0.055F;
        return Math.max(0, Math.min(255, Math.round(output * 255.0F)));
    }
}
