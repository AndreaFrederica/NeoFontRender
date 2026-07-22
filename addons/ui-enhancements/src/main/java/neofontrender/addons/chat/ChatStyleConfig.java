package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

public final class ChatStyleConfig {
    public static boolean enabled = true;
    public static int background = 0x98000000;
    public static int border = 0x80484848;
    public static int inputBackground = 0xB0000000;
    public static int trayBackground = 0xA0000000;
    public static int tabBackground = 0x90101010;
    public static int activeTab = 0xC0282828;
    public static int unreadTab = 0xC03A3420;
    public static int pingedTab = 0xC0502028;
    public static int hoveredTab = 0xB0383838;
    public static int scrollbar = 0xC0A0A0A0;
    public static int text = 0xFFFFFFFF;
    public static int borderWidth = 1;
    public static int opacityPercent = 100;

    private ChatStyleConfig() {}

    public static void load() {
        NfrConfigFile f = UiEnhancementsConfig.file();
        f.define("chat.style.enabled", true, "Replace TabbyChat's fixed texture with configurable colors.")
                .define("chat.style.background", color(background), "Main chat background ARGB color.")
                .define("chat.style.border", color(border), "Chat border ARGB color.")
                .define("chat.style.inputBackground", color(inputBackground), "Input area background ARGB color.")
                .define("chat.style.trayBackground", color(trayBackground), "Channel tray background ARGB color.")
                .define("chat.style.tabBackground", color(tabBackground), "Inactive channel tab ARGB color.")
                .define("chat.style.activeTab", color(activeTab), "Active channel tab ARGB color.")
                .define("chat.style.unreadTab", color(unreadTab), "Unread channel tab ARGB color.")
                .define("chat.style.pingedTab", color(pingedTab), "Mentioned channel tab ARGB color.")
                .define("chat.style.hoveredTab", color(hoveredTab), "Hovered channel tab ARGB color.")
                .define("chat.style.scrollbar", color(scrollbar), "Chat scrollbar ARGB color.")
                .define("chat.style.text", color(text), "Chat and input text ARGB color.")
                .define("chat.style.borderWidth", 1, "Panel border width in pixels (0-8).")
                .define("chat.style.opacityPercent", 100, "Additional style opacity multiplier (10-100).");
        enabled = f.getBoolean("chat.style.enabled", true);
        background = parse(f.getString("chat.style.background", color(background)), background);
        border = parse(f.getString("chat.style.border", color(border)), border);
        inputBackground = parse(f.getString("chat.style.inputBackground", color(inputBackground)), inputBackground);
        trayBackground = parse(f.getString("chat.style.trayBackground", color(trayBackground)), trayBackground);
        tabBackground = parse(f.getString("chat.style.tabBackground", color(tabBackground)), tabBackground);
        activeTab = parse(f.getString("chat.style.activeTab", color(activeTab)), activeTab);
        unreadTab = parse(f.getString("chat.style.unreadTab", color(unreadTab)), unreadTab);
        pingedTab = parse(f.getString("chat.style.pingedTab", color(pingedTab)), pingedTab);
        hoveredTab = parse(f.getString("chat.style.hoveredTab", color(hoveredTab)), hoveredTab);
        scrollbar = parse(f.getString("chat.style.scrollbar", color(scrollbar)), scrollbar);
        text = parse(f.getString("chat.style.text", color(text)), text);
        borderWidth = f.getInt("chat.style.borderWidth", 1, 0, 8);
        opacityPercent = f.getInt("chat.style.opacityPercent", 100, 10, 100);
        migrateOriginalBlueDefaults();
        f.save();
    }

    public static void save() {
        UiEnhancementsConfig.file()
                .set("chat.style.enabled", enabled)
                .set("chat.style.background", color(background))
                .set("chat.style.border", color(border))
                .set("chat.style.inputBackground", color(inputBackground))
                .set("chat.style.trayBackground", color(trayBackground))
                .set("chat.style.tabBackground", color(tabBackground))
                .set("chat.style.activeTab", color(activeTab))
                .set("chat.style.unreadTab", color(unreadTab))
                .set("chat.style.pingedTab", color(pingedTab))
                .set("chat.style.hoveredTab", color(hoveredTab))
                .set("chat.style.scrollbar", color(scrollbar))
                .set("chat.style.text", color(text))
                .set("chat.style.borderWidth", borderWidth)
                .set("chat.style.opacityPercent", opacityPercent)
                .save();
    }

    static int withOpacity(int color, float minecraftOpacity) {
        int alpha = color >>> 24;
        alpha = Math.round(alpha * (opacityPercent / 100.0F) * minecraftOpacity);
        return color & 0x00FFFFFF | Math.max(0, Math.min(255, alpha)) << 24;
    }

    private static String color(int value) { return String.format("#%08X", value); }

    /** The addon was unpublished when this palette changed, so only its exact old defaults migrate. */
    private static void migrateOriginalBlueDefaults() {
        if (background != 0xB012151B || border != 0xD08094AC || inputBackground != 0xD00B0D12
                || trayBackground != 0xC0101319 || tabBackground != 0xB0181C24
                || activeTab != 0xD0255363 || unreadTab != 0xD04A3D24
                || pingedTab != 0xD0682D38 || hoveredTab != 0xD02B3442
                || scrollbar != 0xE0B8C2D0) return;
        background = 0x98000000;
        border = 0x80484848;
        inputBackground = 0xB0000000;
        trayBackground = 0xA0000000;
        tabBackground = 0x90101010;
        activeTab = 0xC0282828;
        unreadTab = 0xC03A3420;
        pingedTab = 0xC0502028;
        hoveredTab = 0xB0383838;
        scrollbar = 0xC0A0A0A0;
        save();
    }

    private static int parse(String value, int fallback) {
        try {
            String clean = value.startsWith("#") ? value.substring(1) : value;
            return (int) Long.parseLong(clean, 16);
        } catch (RuntimeException ignored) { return fallback; }
    }
}
