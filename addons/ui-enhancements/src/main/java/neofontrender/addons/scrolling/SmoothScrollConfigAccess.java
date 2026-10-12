package neofontrender.addons.scrolling;

import neofontrender.addons.chat.ExternalChatCompat;

/** Runtime bridge used by early-loaded mixins without exposing mutable fields. */
public final class SmoothScrollConfigAccess {
    private SmoothScrollConfigAccess() {}
    public static boolean enabled() { return SmoothScrollConfig.enabled; }
    public static boolean vanillaListsEnabled() { return enabled() && SmoothScrollConfig.vanillaLists; }
    public static boolean forgeListsEnabled() { return enabled() && SmoothScrollConfig.forgeLists; }
    public static boolean creativeInventoryEnabled() { return enabled() && SmoothScrollConfig.creativeInventory; }
    public static boolean chatEnabled() { return enabled() && SmoothScrollConfig.chat && !ExternalChatCompat.tabbyChatLoaded(); }
    public static int wheelStep() { return SmoothScrollConfig.wheelStep; }
}
