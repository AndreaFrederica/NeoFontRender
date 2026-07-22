package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Persistent options for vanilla chat history and session restoration. */
final class EnhancedChatConfig {
    static boolean enabled = true;
    static boolean extendedHistory = true;
    static int maxMessages = 16384;
    static boolean persistence = true;
    static boolean persistReceived = true;
    static boolean persistSent = true;
    static boolean tabbedChat = true;

    private EnhancedChatConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("chat.enabled", true, "Master switch for vanilla chat enhancements.")
                .define("chat.extendedHistory", true, "Increase vanilla's 100-message history limit.")
                .define("chat.maxMessages", 16384, "Maximum retained received and sent messages (100-32767).")
                .define("chat.persistence", true, "Restore chat across worlds, servers, and game restarts.")
                .define("chat.persistReceived", true, "Persist received components with formatting and events.")
                .define("chat.persistSent", true, "Persist sent-message and command history.")
                .define("chat.tabbedChat", true, "Use the embedded TabbyChat interface when no external TabbyChat is loaded.");
        enabled = file.getBoolean("chat.enabled", true);
        extendedHistory = file.getBoolean("chat.extendedHistory", true);
        maxMessages = file.getInt("chat.maxMessages", 16384, 100, 32767);
        persistence = file.getBoolean("chat.persistence", true);
        persistReceived = file.getBoolean("chat.persistReceived", true);
        persistSent = file.getBoolean("chat.persistSent", true);
        tabbedChat = file.getBoolean("chat.tabbedChat", true);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("chat.enabled", enabled)
                .set("chat.extendedHistory", extendedHistory)
                .set("chat.maxMessages", maxMessages)
                .set("chat.persistence", persistence)
                .set("chat.persistReceived", persistReceived)
                .set("chat.persistSent", persistSent)
                .set("chat.tabbedChat", tabbedChat)
                .save();
        ChatHistoryManager.INSTANCE.configChanged();
    }
}
