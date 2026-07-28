package neofontrender.addons.chat;

public final class EnhancedChatConfigAccess {
    private EnhancedChatConfigAccess() {}

    public static boolean tabbedChatEnabled() {
        return tabbedChatEnabled(ExternalChatCompat.tabbyChatLoaded());
    }

    static boolean tabbedChatEnabled(boolean externalTabbyLoaded) {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.tabbedChat && !externalTabbyLoaded;
    }

    public static int messageLimit() {
        return messageLimit(ExternalChatCompat.tabbyChatLoaded());
    }

    static int messageLimit(boolean externalTabbyLoaded) {
        return !externalTabbyLoaded && EnhancedChatConfig.enabled && EnhancedChatConfig.extendedHistory
                ? EnhancedChatConfig.maxMessages : 100;
    }

    public static boolean persistenceEnabled() {
        return persistenceEnabled(ExternalChatCompat.tabbyChatLoaded());
    }

    static boolean persistenceEnabled(boolean externalTabbyLoaded) {
        return !externalTabbyLoaded && EnhancedChatConfig.enabled && EnhancedChatConfig.persistence;
    }
}
