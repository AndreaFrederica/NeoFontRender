package neofontrender.addons.tips;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

public final class TipsConfig {
    public static boolean enabled = true;
    public static int cycleTimeMillis = 6000;
    public static boolean showOnWorldLoading = true;
    public static boolean showOnModernSplash = true;
    public static boolean showOnForgeLoading = true;
    public static boolean showOnResourceReload = false;

    private TipsConfig() {}

    public static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("tips.enabled", true, "Show tips on loading screens.")
                .define("tips.cycleTimeMillis", 6000,
                        "Default display duration per tip in milliseconds.")
                .define("tips.showOnWorldLoading", true,
                        "Show tips on the modern world loading screen.")
                .define("tips.showOnModernSplash", true,
                        "Show tips on the ModernSplash startup screen.")
                .define("tips.showOnForgeLoading", true,
                        "Show tips on Forge loading screens (LoadingScreenRenderer, GuiScreenWorking, GuiDownloadTerrain).")
                .define("tips.showOnResourceReload", false,
                        "Show tips on the resource reload screen.");
        enabled = file.getBoolean("tips.enabled", true);
        cycleTimeMillis = file.getInt("tips.cycleTimeMillis", 6000, 2000, 30000);
        showOnWorldLoading = file.getBoolean("tips.showOnWorldLoading", true);
        showOnModernSplash = file.getBoolean("tips.showOnModernSplash", true);
        showOnForgeLoading = file.getBoolean("tips.showOnForgeLoading", true);
        showOnResourceReload = file.getBoolean("tips.showOnResourceReload", false);
        file.save();
    }

    public static void save() {
        cycleTimeMillis = Math.max(2000, Math.min(30000, cycleTimeMillis));
        UiEnhancementsConfig.file()
                .set("tips.enabled", enabled)
                .set("tips.cycleTimeMillis", cycleTimeMillis)
                .set("tips.showOnWorldLoading", showOnWorldLoading)
                .set("tips.showOnModernSplash", showOnModernSplash)
                .set("tips.showOnForgeLoading", showOnForgeLoading)
                .set("tips.showOnResourceReload", showOnResourceReload)
                .save();
    }
}
