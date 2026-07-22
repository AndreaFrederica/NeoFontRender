package neofontrender.addons.effects;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Arrays;
import java.util.List;

/** Persistent options for in-world screen backgrounds. */
final class ScreenEffectsConfig {
    private static final int[] DEFAULT_COLORS = {
            0x70000000, 0x70000000, 0x90000000, 0x90000000
    };

    static boolean enabled = true;
    static boolean fade = true;
    static int fadeDurationMillis = 220;
    static boolean blur = true;
    static float blurRadius = 5.0F;
    static boolean gradient = true;
    static int[] colors = DEFAULT_COLORS.clone();

    private ScreenEffectsConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("effects.enabled", true, "Replace in-world GUI backgrounds with configured effects.")
                .define("effects.fade", true, "Fade the overlay in when a screen opens.")
                .define("effects.fadeDurationMillis", 220, "Fade duration in milliseconds (0-1000).")
                .define("effects.blur", true, "Run Minecraft's two-pass Gaussian blur while a screen is open.")
                .define("effects.blurRadius", 5.0D, "Gaussian blur radius (1-16).")
                .define("effects.gradient", true, "Draw a four-corner color gradient over the world.")
                .define("effects.gradientColors", colorStrings(DEFAULT_COLORS), "ARGB colors: UL, UR, LR, LL.");
        enabled = file.getBoolean("effects.enabled", true);
        fade = file.getBoolean("effects.fade", true);
        fadeDurationMillis = file.getInt("effects.fadeDurationMillis", 220, 0, 1000);
        blur = file.getBoolean("effects.blur", true);
        blurRadius = (float) file.getDouble("effects.blurRadius", 5.0D, 1.0D, 16.0D);
        gradient = file.getBoolean("effects.gradient", true);
        colors = parseColors(file.getStringList("effects.gradientColors", colorStrings(DEFAULT_COLORS)));
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("effects.enabled", enabled)
                .set("effects.fade", fade)
                .set("effects.fadeDurationMillis", fadeDurationMillis)
                .set("effects.blur", blur)
                .set("effects.blurRadius", (double) blurRadius)
                .set("effects.gradient", gradient)
                .set("effects.gradientColors", colorStrings(colors))
                .save();
    }

    private static List<String> colorStrings(int[] values) {
        String[] result = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = String.format("#%08X", values[index]);
        }
        return Arrays.asList(result);
    }

    private static int[] parseColors(List<String> values) {
        if (values == null || values.size() != 4) {
            NfrUiEnhancements.LOGGER.error("effects.gradientColors must contain exactly four ARGB colors");
            return DEFAULT_COLORS.clone();
        }
        int[] result = new int[4];
        try {
            for (int index = 0; index < result.length; index++) {
                String value = values.get(index).trim();
                if (value.startsWith("#")) value = value.substring(1);
                if (value.length() != 8) throw new IllegalArgumentException("Expected eight hexadecimal digits");
                result[index] = (int) Long.parseLong(value, 16);
            }
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Invalid effects.gradientColors; restoring defaults", exception);
            return DEFAULT_COLORS.clone();
        }
        return result;
    }
}
