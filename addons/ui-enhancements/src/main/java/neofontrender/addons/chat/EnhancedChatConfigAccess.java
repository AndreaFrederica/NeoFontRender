package neofontrender.addons.chat;

/** Read-only early-Mixin bridge for runtime chat limits. */
public final class EnhancedChatConfigAccess {
    private EnhancedChatConfigAccess() {}

    public static int messageLimit() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.extendedHistory
                ? EnhancedChatConfig.maxMessages : 100;
    }

    public static boolean tabbedChatEnabled() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.tabbedChat;
    }
}
