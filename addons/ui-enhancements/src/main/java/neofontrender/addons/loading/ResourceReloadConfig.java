package neofontrender.addons.loading;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class ResourceReloadConfig {
    static boolean enabled = true;
    static boolean languageSwitch = true;
    static boolean resourcePackSwitch = true;
    static boolean progressBar = true;
    static boolean percentage = true;
    static boolean spinner = true;
    static int accentColor = 0xFF52E875;
    static int textColor = 0xFFFFFFFF;

    private ResourceReloadConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("resourceReload.enabled", true,
                        "Show progress while Minecraft reloads client resources.")
                .define("resourceReload.languageSwitch", true, "Show progress when switching languages.")
                .define("resourceReload.resourcePackSwitch", true, "Show progress when applying resource packs.")
                .define("resourceReload.progressBar", true, "Draw the thin progress line along the bottom edge.")
                .define("resourceReload.percentage", true, "Show exact stage progress.")
                .define("resourceReload.spinner", true, "Show the generated rotating indicator.")
                .define("resourceReload.accentColor", "#FF52E875", "ARGB progress-line and spinner color.")
                .define("resourceReload.textColor", "#FFFFFFFF", "ARGB loading-label and percentage color.");
        enabled = file.getBoolean("resourceReload.enabled", true);
        languageSwitch = file.getBoolean("resourceReload.languageSwitch", true);
        resourcePackSwitch = file.getBoolean("resourceReload.resourcePackSwitch", true);
        progressBar = file.getBoolean("resourceReload.progressBar", true);
        percentage = file.getBoolean("resourceReload.percentage", true);
        spinner = file.getBoolean("resourceReload.spinner", true);
        accentColor = parseColor(file.getString("resourceReload.accentColor", "#FF52E875"), 0xFF52E875);
        textColor = parseColor(file.getString("resourceReload.textColor", "#FFFFFFFF"), 0xFFFFFFFF);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("resourceReload.enabled", enabled)
                .set("resourceReload.languageSwitch", languageSwitch)
                .set("resourceReload.resourcePackSwitch", resourcePackSwitch)
                .set("resourceReload.progressBar", progressBar)
                .set("resourceReload.percentage", percentage)
                .set("resourceReload.spinner", spinner)
                .set("resourceReload.accentColor", String.format("#%08X", accentColor))
                .set("resourceReload.textColor", String.format("#%08X", textColor))
                .save();
    }

    private static int parseColor(String value, int fallback) {
        try {
            String normalized = value == null ? "" : value.trim();
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            if (normalized.length() == 6) normalized = "FF" + normalized;
            return (int) Long.parseLong(normalized, 16);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
