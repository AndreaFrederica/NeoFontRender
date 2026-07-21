package neofontrender.addons.chat;

public final class EnhancedChatConfigAccess {
    private EnhancedChatConfigAccess() {}

    public static int messageLimit() {
        return !ExternalChatCompat.tabbyChatLoaded() && EnhancedChatConfig.enabled && EnhancedChatConfig.extendedHistory
                ? EnhancedChatConfig.maxMessages : 100;
    }
}
