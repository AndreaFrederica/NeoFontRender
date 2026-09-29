package neofontrender.addons.notifications;

/** JSON-backed compatibility notice definition. */
public final class CompatibilityNotification {
    public String id;
    public String modid;
    public boolean enabled = true;
    public boolean showOnce = true;
    public int priority;
    public String titleKey;
    public String messageKey;

    boolean valid() {
        return text(modid);
    }

    String resolvedId() { return text(id) ? id : modid; }
    String resolvedTitleKey() { return text(titleKey) ? titleKey
            : "neofontrender_ui_enhancements.notification." + resolvedId() + ".title"; }
    String resolvedMessageKey() { return text(messageKey) ? messageKey
            : "neofontrender_ui_enhancements.notification." + resolvedId() + ".message"; }

    private static boolean text(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
