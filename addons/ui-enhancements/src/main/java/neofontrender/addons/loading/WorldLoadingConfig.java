package neofontrender.addons.loading;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class WorldLoadingConfig {
    static boolean enabled = true;
    static boolean worldJoin = true;
    static boolean dimensionChange = true;
    static boolean singleplayerServerProgress = true;
    static boolean lastExitSnapshot = true;
    static boolean bottomShade = true;
    static boolean progressBar = true;
    static boolean percentage = true;
    static boolean spinner = true;
    static boolean fadeOut = true;
    static int fadeOutDurationMillis = 360;
    static int accentColor = 0xFF52E875;
    static int textColor = 0xFFFFFFFF;

    private WorldLoadingConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("worldLoading.enabled", true,
                        "Replace the 1.12 terrain-download screen with a modern loading overlay.")
                .define("worldLoading.worldJoin", true, "Use the overlay when entering a world.")
                .define("worldLoading.dimensionChange", true, "Use the overlay while changing dimensions.")
                .define("worldLoading.singleplayerServerProgress", true,
                        "Use integrated-server spawn preparation for singleplayer world-entry progress.")
                .define("worldLoading.lastExitSnapshot", true,
                        "Save the last clean singleplayer frame and use it on the next world entry.")
                .define("worldLoading.bottomShade", true, "Draw a bottom-up dark gradient behind the loading UI.")
                .define("worldLoading.progressBar", true, "Draw the thin progress line along the bottom edge.")
                .define("worldLoading.percentage", true, "Show the smoothed client-readiness estimate.")
                .define("worldLoading.spinner", true, "Show the programmatically drawn rotating indicator.")
                .define("worldLoading.fadeOut", true, "Fade the loading UI away after terrain becomes available.")
                .define("worldLoading.fadeOutDurationMillis", 360, "Loading UI fade-out duration (0-1500 ms).")
                .define("worldLoading.accentColor", "#FF52E875", "ARGB progress-line and spinner color.")
                .define("worldLoading.textColor", "#FFFFFFFF", "ARGB loading-label and percentage color.");
        enabled = file.getBoolean("worldLoading.enabled", true);
        worldJoin = file.getBoolean("worldLoading.worldJoin", true);
        dimensionChange = file.getBoolean("worldLoading.dimensionChange", true);
        singleplayerServerProgress = file.getBoolean("worldLoading.singleplayerServerProgress", true);
        lastExitSnapshot = file.getBoolean("worldLoading.lastExitSnapshot", true);
        bottomShade = file.getBoolean("worldLoading.bottomShade", true);
        progressBar = file.getBoolean("worldLoading.progressBar", true);
        percentage = file.getBoolean("worldLoading.percentage", true);
        spinner = file.getBoolean("worldLoading.spinner", true);
        fadeOut = file.getBoolean("worldLoading.fadeOut", true);
        fadeOutDurationMillis = file.getInt("worldLoading.fadeOutDurationMillis", 360, 0, 1500);
        accentColor = parseColor(file.getString("worldLoading.accentColor", "#FF52E875"), 0xFF52E875);
        textColor = parseColor(file.getString("worldLoading.textColor", "#FFFFFFFF"), 0xFFFFFFFF);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("worldLoading.enabled", enabled)
                .set("worldLoading.worldJoin", worldJoin)
                .set("worldLoading.dimensionChange", dimensionChange)
                .set("worldLoading.singleplayerServerProgress", singleplayerServerProgress)
                .set("worldLoading.lastExitSnapshot", lastExitSnapshot)
                .set("worldLoading.bottomShade", bottomShade)
                .set("worldLoading.progressBar", progressBar)
                .set("worldLoading.percentage", percentage)
                .set("worldLoading.spinner", spinner)
                .set("worldLoading.fadeOut", fadeOut)
                .set("worldLoading.fadeOutDurationMillis", fadeOutDurationMillis)
                .set("worldLoading.accentColor", String.format("#%08X", accentColor))
                .set("worldLoading.textColor", String.format("#%08X", textColor))
                .save();
        WorldLoadingRenderer.INSTANCE.configChanged();
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
