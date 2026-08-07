package neofontrender.addons.chat;

/** Read-only early-Mixin bridge for runtime chat limits. */
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

    public static boolean commandCompletionEnabled() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.commandCompletion;
    }

    public static boolean privateCommandBlockEnabled() {
        return tabbedChatEnabled() && EnhancedChatConfig.privateCommandBlock;
    }

    public static String privateCommandPrefix(String player) {
        String name = player == null ? "" : player;
        String template = EnhancedChatConfig.privateMessageCommand == null
                ? "/msg" : EnhancedChatConfig.privateMessageCommand.trim();
        String prefix = template.contains("{player}")
                ? template.replace("{player}", name).trim() : template + " " + name;
        return prefix.trim().isEmpty() ? "/msg " + name : prefix.trim();
    }

    public static boolean verticalTabsEnabled() {
        return tabbedChatEnabled() && EnhancedChatConfig.verticalTabs;
    }

    public static boolean persistentChatHudEnabled() {
        return tabbedChatEnabled() && EnhancedChatConfig.persistentChatHud;
    }

    public static void setPersistentChatHudEnabled(boolean enabled) {
        EnhancedChatConfig.persistentChatHud = enabled;
        EnhancedChatConfig.save();
    }

    public static boolean closeChatOnDetach() {
        return EnhancedChatConfig.closeChatOnDetach;
    }

    public static boolean chatEnabled() {
        return EnhancedChatConfig.enabled;
    }

    public static boolean forceServerTranslations() {
        return EnhancedChatConfig.salutationForceServerTranslations;
    }

    public static boolean salutationOverrideDisabled() {
        return EnhancedChatConfig.salutationDisableOverride;
    }
}
