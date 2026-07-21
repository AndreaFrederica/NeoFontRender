package neofontrender.addons.effects;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Arrays;
import java.util.List;

final class ScreenEffectsConfig {
    private static final int[] DEFAULT_COLORS = {0x80101828, 0x80101018, 0xA0101018, 0xA0181028};
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
        file.define("effects.enabled", true, "Replace the in-world GUI background with configurable effects.")
                .define("effects.fade", true, "Fade the background overlay in when a screen opens.")
                .define("effects.fadeDurationMillis", 220, "Fade duration in milliseconds (0-1000).")
                .define("effects.blur", true, "Use Minecraft's two-pass Gaussian post shader while a screen is open.")
                .define("effects.blurRadius", 5.0D, "Gaussian blur radius (1-16).")
                .define("effects.gradient", true, "Draw a four-corner color gradient over the blurred world.")
                .define("effects.gradientColors", colorStrings(DEFAULT_COLORS), "Four ARGB colors: UL, UR, LR, LL.");
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
        UiEnhancementsConfig.file().set("effects.enabled", enabled)
                .set("effects.fade", fade)
                .set("effects.fadeDurationMillis", fadeDurationMillis)
                .set("effects.blur", blur)
                .set("effects.blurRadius", blurRadius)
                .set("effects.gradient", gradient)
                .set("effects.gradientColors", colorStrings(colors)).save();
        ScreenEffectsRenderer.INSTANCE.configChanged();
    }

    private static List<String> colorStrings(int[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) result[i] = String.format("#%08X", values[i]);
        return Arrays.asList(result);
    }

    private static int[] parseColors(List<String> values) {
        if (values == null || values.size() != 4) return DEFAULT_COLORS.clone();
        int[] result = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                String value = values.get(i).trim();
                if (value.startsWith("#")) value = value.substring(1);
                result[i] = (int) Long.parseLong(value, 16);
            }
            return result;
        } catch (RuntimeException exception) {
            return DEFAULT_COLORS.clone();
        }
    }
}
