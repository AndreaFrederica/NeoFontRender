package neofontrender.core.font.support;

/** Shared color selection for legacy two-pass and modern composed text shadows. */
public final class ShadowColorPolicy {
    public static final String VANILLA = "vanilla";
    public static final String COLORED = "colored";
    public static final String SOLID = "solid";

    private ShadowColorPolicy() {}

    public static String normalizeMode(String mode) {
        if (COLORED.equalsIgnoreCase(mode)) return COLORED;
        if (SOLID.equalsIgnoreCase(mode)) return SOLID;
        return VANILLA;
    }

    /** Maps an arbitrary foreground color using the vanilla per-channel quarter-brightness rule. */
    public static int darken(int foregroundArgb) {
        return (foregroundArgb & 0xFF000000) | ((foregroundArgb & 0xFCFCFC) >> 2);
    }

    /** Resolves an unformatted run's shadow color and then applies explicit overrides. */
    public static int shadowColor(int foregroundArgb, String mode, int configuredArgb,
                                  ShadowColorRemapRules rules, int[] palette) {
        String normalized = normalizeMode(mode);
        int color = SOLID.equals(normalized) ? configuredArgb : darken(foregroundArgb);
        return remap(foregroundArgb, color, rules, palette);
    }

    /** Resolves a formatted palette run's foreground or shadow color. */
    public static int paletteColor(int foregroundIndex, int alpha, boolean shadow, String mode,
                                   int configuredArgb, ShadowColorRemapRules rules, int[] palette) {
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
            color = darken(foreground);
        }
        return remap(foreground, color, rules, colors);
    }

    /** Resolves a modern per-run shadow color. */
    public static int modernColor(int foregroundArgb, int configuredArgb, String mode,
                                  ShadowColorRemapRules rules, int[] palette) {
        return shadowColor(foregroundArgb, mode, configuredArgb, rules, palette);
    }

    /** Selects the foreground or vanilla shadow half of a 32-entry Minecraft palette. */
    public static int paletteIndex(int foregroundIndex, boolean shadow, String mode) {
        return foregroundIndex + (shadow && VANILLA.equals(normalizeMode(mode)) ? 16 : 0);
    }

    private static int remap(int foregroundArgb, int shadowArgb,
                             ShadowColorRemapRules rules, int[] palette) {
        return rules == null ? shadowArgb : rules.remap(foregroundArgb, shadowArgb, palette);
    }
}
