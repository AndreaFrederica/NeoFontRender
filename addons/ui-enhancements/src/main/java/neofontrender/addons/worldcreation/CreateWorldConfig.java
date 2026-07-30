package neofontrender.addons.worldcreation;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

public final class CreateWorldConfig {
    private static final String LEGACY_ENABLED_KEY = "createWorld.enabled";
    public static String theme = CreateWorldTheme.TABBED.id();

    private CreateWorldConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("createWorld.theme", CreateWorldTheme.TABBED.id(),
                "Create-world interface theme: vanilla, tabbed or modernui.");
        theme = CreateWorldTheme.parse(file.getString(
                "createWorld.theme", CreateWorldTheme.TABBED.id())).id();
        file.set("createWorld.theme", theme);
        if (file.contains(LEGACY_ENABLED_KEY)) file.remove(LEGACY_ENABLED_KEY);
        file.save();
    }

    static void save() {
        theme = CreateWorldTheme.parse(theme).id();
        UiEnhancementsConfig.file().set("createWorld.theme", theme).save();
    }

    public static CreateWorldTheme currentTheme() {
        return CreateWorldTheme.parse(theme);
    }

    public static boolean usesTabbedLayout() {
        return currentTheme().usesTabbedLayout();
    }

    public static boolean usesModernStyle() {
        return currentTheme().usesModernStyle();
    }
}
