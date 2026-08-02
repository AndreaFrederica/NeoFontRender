package neofontrender.addons.bundled;

/** Identifies the two upstream-compatible mod entries supplied inside UIE. */
public final class BundledModRegistry {
    private static volatile boolean tabbyChat;
    private static volatile boolean salutation;

    private BundledModRegistry() {}

    public static void markTabbyChat() { tabbyChat = true; }
    public static void markSalutation() { salutation = true; }
    public static boolean isTabbyChatBundled() { return tabbyChat; }
    public static boolean isSalutationBundled() { return salutation; }
}
