package neofontrender.addons.mainmenu;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

public final class MainMenuConfig {
    public static boolean continueGame = true;
    private static String lastType = "";
    private static String lastIdentifier = "";
    private static String lastDisplayName = "";
    private static String lastAddress = "";

    private MainMenuConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("mainMenu.continueGame.enabled", true,
                        "Show a direct Continue Game button for the last joined world or server.")
                .define("mainMenu.continueGame.lastType", "", "Last successful target type.")
                .define("mainMenu.continueGame.lastIdentifier", "", "World folder or server address.")
                .define("mainMenu.continueGame.lastDisplayName", "", "Name displayed on the button.")
                .define("mainMenu.continueGame.lastAddress", "", "Last multiplayer server address.");
        continueGame = file.getBoolean("mainMenu.continueGame.enabled", true);
        lastType = file.getString("mainMenu.continueGame.lastType", "");
        lastIdentifier = file.getString("mainMenu.continueGame.lastIdentifier", "");
        lastDisplayName = file.getString("mainMenu.continueGame.lastDisplayName", "");
        lastAddress = file.getString("mainMenu.continueGame.lastAddress", "");
        file.save();
    }

    static void saveSetting() {
        UiEnhancementsConfig.file().set("mainMenu.continueGame.enabled", continueGame).save();
    }

    static LastPlayedTarget target() {
        return LastPlayedTarget.persisted(lastType, lastIdentifier, lastDisplayName, lastAddress);
    }

    static void record(LastPlayedTarget target) {
        if (target == null || target.equals(target())) return;
        lastType = target.kind().id();
        lastIdentifier = target.identifier();
        lastDisplayName = target.displayName();
        lastAddress = target.address();
        UiEnhancementsConfig.file()
                .set("mainMenu.continueGame.lastType", lastType)
                .set("mainMenu.continueGame.lastIdentifier", lastIdentifier)
                .set("mainMenu.continueGame.lastDisplayName", lastDisplayName)
                .set("mainMenu.continueGame.lastAddress", lastAddress)
                .save();
    }
}
