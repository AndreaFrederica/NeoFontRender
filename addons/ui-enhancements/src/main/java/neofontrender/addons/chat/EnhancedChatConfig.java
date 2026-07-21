package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class EnhancedChatConfig {
    static boolean enabled = true;
    static boolean tabbedChat = true;
    static boolean extendedHistory = true;
    static int maxMessages = 16384;
    static boolean persistence = true;
    static boolean persistReceived = true;
    static boolean persistSent = true;

    private EnhancedChatConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("chat.enabled", true, "Master switch for integrated chat enhancements.")
                .define("chat.tabbedChat", true, "Enable the embedded TabbyChat channel and filter interface.")
                .define("chat.extendedHistory", true, "Increase vanilla's 100-message chat limit.")
                .define("chat.maxMessages", 16384, "Maximum received and sent messages retained (100-32767).")
                .define("chat.persistence", true, "Restore chat across worlds, servers and game restarts.")
                .define("chat.persistReceived", true, "Persist received chat components with formatting and events.")
                .define("chat.persistSent", true, "Persist sent-message command history.");
        enabled = file.getBoolean("chat.enabled", true);
        tabbedChat = file.getBoolean("chat.tabbedChat", true);
        extendedHistory = file.getBoolean("chat.extendedHistory", true);
        maxMessages = file.getInt("chat.maxMessages", 16384, 100, 32767);
        persistence = file.getBoolean("chat.persistence", true);
        persistReceived = file.getBoolean("chat.persistReceived", true);
        persistSent = file.getBoolean("chat.persistSent", true);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("chat.enabled", enabled)
                .set("chat.tabbedChat", tabbedChat)
                .set("chat.extendedHistory", extendedHistory)
                .set("chat.maxMessages", maxMessages)
                .set("chat.persistence", persistence)
                .set("chat.persistReceived", persistReceived)
                .set("chat.persistSent", persistSent)
                .save();
        ChatHistoryManager.INSTANCE.configChanged();
    }
}
