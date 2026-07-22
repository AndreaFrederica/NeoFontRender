package neofontrender.addons.scrolling;

/** Read-only bridge used by early-loaded mixins. */
public final class SmoothScrollConfigAccess {
    private SmoothScrollConfigAccess() {}

    public static boolean vanillaListsEnabled() {
        return SmoothScrollConfig.enabled && SmoothScrollConfig.vanillaLists;
    }

    public static boolean forgeListsEnabled() {
        return SmoothScrollConfig.enabled && SmoothScrollConfig.forgeLists;
    }

    public static boolean chatEnabled() {
        return SmoothScrollConfig.enabled && SmoothScrollConfig.chat;
    }

    public static boolean creativeInventoryEnabled() {
        return SmoothScrollConfig.enabled && SmoothScrollConfig.creativeInventory;
    }

    public static boolean chatConfigured() {
        return SmoothScrollConfig.chat;
    }

    public static void setChatConfigured(boolean enabled) {
        SmoothScrollConfig.chat = enabled;
    }

    public static void save() {
        SmoothScrollConfig.save();
    }

    public static int wheelStep() {
        return SmoothScrollConfig.wheelStep;
    }
}
