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
    static boolean animateMessages = true;
    static int messageAnimationDuration = 150;
    static float messageAnimationDistance = 7.0F;
    static String messageAnimationEasing = "sine";
    static boolean animateInput = true;
    static int inputAnimationDuration = 170;
    static float inputAnimationDistance = 8.0F;
    static String inputAnimationEasing = "back";

    private EnhancedChatConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("chat.enabled", true, "Master switch for integrated chat enhancements.")
                .define("chat.tabbedChat", true, "Enable the embedded TabbyChat channel and filter interface.")
                .define("chat.extendedHistory", true, "Increase vanilla's 100-message chat limit.")
                .define("chat.maxMessages", 16384, "Maximum received and sent messages retained (100-32767).")
                .define("chat.persistence", true, "Restore chat across worlds, servers and game restarts.")
                .define("chat.persistReceived", true, "Persist received chat components with formatting and events.")
                .define("chat.persistSent", true, "Persist sent-message command history.")
                .define("chat.animation.messages", true, "Animate newly received messages.")
                .define("chat.animation.messageDuration", 150, "Message entrance duration in milliseconds.")
                .define("chat.animation.messageDistance", 7.0D, "Message entrance distance in GUI pixels.")
                .define("chat.animation.messageEasing", "sine", "Message entrance easing.")
                .define("chat.animation.input", true, "Animate the chat input when opening chat.")
                .define("chat.animation.inputDuration", 170, "Input entrance duration in milliseconds.")
                .define("chat.animation.inputDistance", 8.0D, "Input entrance distance in GUI pixels.")
                .define("chat.animation.inputEasing", "back", "Input entrance easing.");
        enabled = file.getBoolean("chat.enabled", true);
        tabbedChat = file.getBoolean("chat.tabbedChat", true);
        extendedHistory = file.getBoolean("chat.extendedHistory", true);
        maxMessages = file.getInt("chat.maxMessages", 16384, 100, 32767);
        persistence = file.getBoolean("chat.persistence", true);
        persistReceived = file.getBoolean("chat.persistReceived", true);
        persistSent = file.getBoolean("chat.persistSent", true);
        animateMessages = file.getBoolean("chat.animation.messages", true);
        messageAnimationDuration = file.getInt("chat.animation.messageDuration", 150, 10, 1000);
        messageAnimationDistance = (float) file.getDouble("chat.animation.messageDistance", 7.0D, 0.0D, 32.0D);
        messageAnimationEasing = file.getString("chat.animation.messageEasing", "sine");
        animateInput = file.getBoolean("chat.animation.input", true);
        inputAnimationDuration = file.getInt("chat.animation.inputDuration", 170, 10, 1000);
        inputAnimationDistance = (float) file.getDouble("chat.animation.inputDistance", 8.0D, 0.0D, 32.0D);
        inputAnimationEasing = file.getString("chat.animation.inputEasing", "back");
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
                .set("chat.animation.messages", animateMessages)
                .set("chat.animation.messageDuration", messageAnimationDuration)
                .set("chat.animation.messageDistance", messageAnimationDistance)
                .set("chat.animation.messageEasing", messageAnimationEasing)
                .set("chat.animation.input", animateInput)
                .set("chat.animation.inputDuration", inputAnimationDuration)
                .set("chat.animation.inputDistance", inputAnimationDistance)
                .set("chat.animation.inputEasing", inputAnimationEasing)
                .save();
        ChatHistoryManager.INSTANCE.configChanged();
        ChatRuntimeController.sync();
    }
}
