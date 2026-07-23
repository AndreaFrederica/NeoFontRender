package neofontrender.addons.effects;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Arrays;
import java.util.List;

final class ScreenEffectsConfig {
    private static final int[] LEGACY_PURPLE_COLORS = {0x80101828, 0x80101018, 0xA0101018, 0xA0181028};
    private static final int[] DEFAULT_COLORS = {0x70000000, 0x70000000, 0x90000000, 0x90000000};
    static boolean enabled = true;
    static boolean fade = true;
    static int fadeDurationMillis = 220;
    static boolean fadeMenus = true;
    static boolean fadeContainers = true;
    static boolean fadeChat = true;
    static boolean blur = true;
    static boolean blurMenus = true;
    static boolean blurContainers = true;
    static boolean blurChat = true;
    static int blurRadius = 5;
    static boolean gradient = true;
    static boolean gradientMenus = true;
    static boolean gradientContainers = true;
    static boolean gradientChat = true;
    static int[] colors = DEFAULT_COLORS.clone();

    private ScreenEffectsConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        boolean legacyMenus = file.getBoolean("effects.applyToMenus", true);
        boolean legacyContainers = file.getBoolean("effects.applyToContainers", true);
        boolean legacyChat = file.getBoolean("effects.applyToChat", true);
        file.define("effects.enabled", true, "Replace the in-world GUI background with configurable effects.")
                .define("effects.fade", true, "Fade the background overlay in and out at screen-session boundaries.")
                .define("effects.fadeDurationMillis", 220, "Fade-in and fade-out duration in milliseconds (0-1000).")
                .define("effects.fadeMenus", legacyMenus, "Animate transitions for menu screens.")
                .define("effects.fadeContainers", legacyContainers, "Animate transitions for inventory and container screens.")
                .define("effects.fadeChat", legacyChat, "Animate transitions for chat screens.")
                .define("effects.blur", true, "Use Minecraft's two-pass Gaussian post shader while a screen is open.")
                .define("effects.blurMenus", legacyMenus, "Apply blur to menu screens.")
                .define("effects.blurContainers", legacyContainers, "Apply blur to inventory and container screens.")
                .define("effects.blurChat", legacyChat, "Apply blur to chat screens.")
                .define("effects.blurRadius", 5, "Integer Gaussian blur radius (1-16).")
                .define("effects.gradient", true, "Draw a four-corner color gradient over the blurred world.")
                .define("effects.gradientMenus", legacyMenus, "Apply the dark overlay to menu screens.")
                .define("effects.gradientContainers", legacyContainers, "Apply the dark overlay to inventory and container screens.")
                .define("effects.gradientChat", legacyChat, "Apply the dark overlay to chat screens.")
                .define("effects.gradientColors", colorStrings(DEFAULT_COLORS), "Four ARGB colors: UL, UR, LR, LL.");
        enabled = file.getBoolean("effects.enabled", true);
        fade = file.getBoolean("effects.fade", true);
        fadeDurationMillis = file.getInt("effects.fadeDurationMillis", 220, 0, 1000);
        fadeMenus = file.getBoolean("effects.fadeMenus", legacyMenus);
        fadeContainers = file.getBoolean("effects.fadeContainers", legacyContainers);
        fadeChat = file.getBoolean("effects.fadeChat", legacyChat);
        blur = file.getBoolean("effects.blur", true);
        blurMenus = file.getBoolean("effects.blurMenus", legacyMenus);
        blurContainers = file.getBoolean("effects.blurContainers", legacyContainers);
        blurChat = file.getBoolean("effects.blurChat", legacyChat);
        blurRadius = Math.round((float) file.getDouble("effects.blurRadius", 5.0D, 1.0D, 16.0D));
        gradient = file.getBoolean("effects.gradient", true);
        gradientMenus = file.getBoolean("effects.gradientMenus", legacyMenus);
        gradientContainers = file.getBoolean("effects.gradientContainers", legacyContainers);
        gradientChat = file.getBoolean("effects.gradientChat", legacyChat);
        colors = parseColors(file.getStringList("effects.gradientColors", colorStrings(DEFAULT_COLORS)));
        file.remove("effects.applyToMenus");
        file.remove("effects.applyToContainers");
        file.remove("effects.applyToChat");
        // Migrate only the original generated palette. Explicitly customized colors are preserved.
        if (Arrays.equals(colors, LEGACY_PURPLE_COLORS)) {
            colors = DEFAULT_COLORS.clone();
            file.set("effects.gradientColors", colorStrings(colors));
        }
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("effects.enabled", enabled)
                .set("effects.fade", fade)
                .set("effects.fadeDurationMillis", fadeDurationMillis)
                .set("effects.fadeMenus", fadeMenus)
                .set("effects.fadeContainers", fadeContainers)
                .set("effects.fadeChat", fadeChat)
                .set("effects.blur", blur)
                .set("effects.blurMenus", blurMenus)
                .set("effects.blurContainers", blurContainers)
                .set("effects.blurChat", blurChat)
                .set("effects.blurRadius", blurRadius)
                .set("effects.gradient", gradient)
                .set("effects.gradientMenus", gradientMenus)
                .set("effects.gradientContainers", gradientContainers)
                .set("effects.gradientChat", gradientChat)
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
