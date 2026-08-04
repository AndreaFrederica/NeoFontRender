package neofontrender.addons.chat;

public final class EnhancedChatConfigAccess {
    private EnhancedChatConfigAccess() {}

    public static boolean tabbedChatEnabled() {
        return tabbedChatEnabled(ExternalChatCompat.tabbyChatLoaded());
    }

    public static boolean chatEnabled() {
        return EnhancedChatConfig.enabled;
    }

    public static boolean commandCompletionEnabled() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.commandCompletion;
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

    public static boolean privateCommandBlockEnabled() {
        return tabbedChatEnabled() && EnhancedChatConfig.privateCommandBlock;
    }

    public static String privateCommandPrefix(String player) {
        String name = player == null ? "" : player;
        String template = EnhancedChatConfig.privateMessageCommand == null
                ? "/msg {player}" : EnhancedChatConfig.privateMessageCommand.trim();
        String prefix = template.replace("{player}", name).trim();
        return prefix.isEmpty() ? "/msg " + name : prefix;
    }

    public static boolean persistentChatHudEnabled() {
        return tabbedChatEnabled() && EnhancedChatConfig.persistentChatHud;
    }

    public static void setPersistentChatHudEnabled(boolean enabled) {
        if (EnhancedChatConfig.persistentChatHud == enabled) return;
        EnhancedChatConfig.persistentChatHud = enabled;
        EnhancedChatConfig.save();
    }

    public static boolean closeChatOnDetach() {
        return EnhancedChatConfig.closeChatOnDetach;
    }

    public static boolean verticalTabsEnabled() {
        return tabbedChatEnabled() && EnhancedChatConfig.verticalTabs;
    }

    /** Values used by the embedded Salutation command and translation implementation. */
    public static boolean forceServerTranslations() {
        return EnhancedChatConfig.salutationForceServerTranslations;
    }

    public static boolean salutationOverrideDisabled() {
        return EnhancedChatConfig.salutationDisableOverride;
    }
}
