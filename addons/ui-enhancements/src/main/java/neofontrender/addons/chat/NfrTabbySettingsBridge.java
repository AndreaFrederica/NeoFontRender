package neofontrender.addons.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import mnm.mods.tabbychat.ChatChannel;
import mnm.mods.tabbychat.extra.filters.UserFilter;
import mnm.mods.tabbychat.settings.AdvancedSettings;
import mnm.mods.tabbychat.settings.GeneralServerSettings;
import mnm.mods.tabbychat.settings.GeneralSettings;
import mnm.mods.tabbychat.settings.ServerSettings;
import mnm.mods.tabbychat.settings.TabbySettings;
import mnm.mods.tabbychat.util.ChannelPatterns;
import mnm.mods.tabbychat.util.ChatVisibility;
import mnm.mods.tabbychat.util.MessagePatterns;
import mnm.mods.tabbychat.util.TimeStamps;
import mnm.mods.util.config.ValueList;
import mnm.mods.util.config.ValueMap;
import mnm.mods.util.config.ValueObject;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;
import net.minecraft.util.text.TextFormatting;

/** Stores the embedded TabbyChat settings in UI Enhancements' independent TOML. */
public final class NfrTabbySettingsBridge {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private NfrTabbySettingsBridge() {}

    public static void loadGlobal(TabbySettings settings) {
        NfrConfigFile f = UiEnhancementsConfig.file();
        GeneralSettings g = settings.general;
        AdvancedSettings a = settings.advanced;
        defineGlobal(f);
        g.logChat.set(f.getBoolean("chat.tabby.general.logChat", true));
        g.splitLog.set(f.getBoolean("chat.tabby.general.splitLog", true));
        g.timestampChat.set(f.getBoolean("chat.tabby.general.timestampChat", false));
        g.timestampStyle.set(enumValue(TimeStamps.class, f.getString("chat.tabby.general.timestampStyle", TimeStamps.MILITARYSECONDS.name()), TimeStamps.MILITARYSECONDS));
        g.timestampColor.set(enumValue(TextFormatting.class, f.getString("chat.tabby.general.timestampColor", TextFormatting.WHITE.name()), TextFormatting.WHITE));
        g.antiSpam.set(f.getBoolean("chat.tabby.general.antiSpam", false));
        g.antiSpamPrejudice.set(f.getDouble("chat.tabby.general.antiSpamPrejudice", 0D, 0D, 1D));
        g.unreadFlashing.set(f.getBoolean("chat.tabby.general.unreadFlashing", true));
        g.checkUpdates.set(false);
        a.chatX.set(f.getInt("chat.tabby.layout.x", 5, -8192, 8192));
        a.chatY.set(f.getInt("chat.tabby.layout.y", 17, -8192, 8192));
        a.chatW.set(f.getInt("chat.tabby.layout.width", 300, 40, 8192));
        a.chatH.set(f.getInt("chat.tabby.layout.height", 160, 20, 8192));
        a.unfocHeight.set((float) f.getDouble("chat.tabby.layout.unfocusedHeight", 0.5D, 0.0D, 1.0D));
        a.fadeTime.set(f.getInt("chat.tabby.layout.fadeTime", 200, 0, 36000));
        a.historyLen.set(f.getInt("chat.maxMessages", 16384, 100, 32767));
        a.hideTag.set(f.getBoolean("chat.tabby.layout.hideTag", false));
        a.keepChatOpen.set(f.getBoolean("chat.tabby.layout.keepChatOpen", false));
        a.spelling.set(f.getBoolean("chat.tabby.general.spelling", true));
        a.visibility.set(enumValue(ChatVisibility.class, f.getString("chat.tabby.layout.visibility", ChatVisibility.NORMAL.name()), ChatVisibility.NORMAL));
        f.save();
    }

    public static void saveGlobal(TabbySettings settings) {
        GeneralSettings g = settings.general;
        AdvancedSettings a = settings.advanced;
        UiEnhancementsConfig.file()
                .set("chat.tabby.general.logChat", g.logChat.get())
                .set("chat.tabby.general.splitLog", g.splitLog.get())
                .set("chat.tabby.general.timestampChat", g.timestampChat.get())
                .set("chat.tabby.general.timestampStyle", g.timestampStyle.get().name())
                .set("chat.tabby.general.timestampColor", g.timestampColor.get().name())
                .set("chat.tabby.general.antiSpam", g.antiSpam.get())
                .set("chat.tabby.general.antiSpamPrejudice", g.antiSpamPrejudice.get())
                .set("chat.tabby.general.unreadFlashing", g.unreadFlashing.get())
                .set("chat.tabby.general.spelling", a.spelling.get())
                .set("chat.tabby.layout.x", a.chatX.get())
                .set("chat.tabby.layout.y", a.chatY.get())
                .set("chat.tabby.layout.width", a.chatW.get())
                .set("chat.tabby.layout.height", a.chatH.get())
                .set("chat.tabby.layout.unfocusedHeight", a.unfocHeight.get())
                .set("chat.tabby.layout.fadeTime", a.fadeTime.get())
                .set("chat.maxMessages", a.historyLen.get())
                .set("chat.tabby.layout.hideTag", a.hideTag.get())
                .set("chat.tabby.layout.keepChatOpen", a.keepChatOpen.get())
                .set("chat.tabby.layout.visibility", a.visibility.get().name())
                .save();
    }

    public static void loadServer(ServerSettings settings, String serverKey) {
        NfrConfigFile f = UiEnhancementsConfig.file();
        String p = "chat.tabby.servers." + safeKey(serverKey) + ".";
        GeneralServerSettings g = settings.general;
        g.channelsEnabled.set(f.getBoolean(p + "channelsEnabled", true));
        g.pmEnabled.set(f.getBoolean(p + "pmEnabled", true));
        g.channelPattern.set(enumValue(ChannelPatterns.class, f.getString(p + "channelPattern", ChannelPatterns.BRACKETS.name()), ChannelPatterns.BRACKETS));
        g.messegePattern.set(enumValue(MessagePatterns.class, f.getString(p + "messagePattern", MessagePatterns.WHISPERS.name()), MessagePatterns.WHISPERS));
        g.useDefaultTab.set(f.getBoolean(p + "useDefaultTab", true));
        g.ignoredChannels.set(f.getStringList(p + "ignoredChannels", java.util.Collections.emptyList()));
        g.defaultChannel.set(f.getString(p + "defaultChannel", ""));
        g.channelCommand.set(f.getString(p + "channelCommand", ""));
        g.messageCommand.set(f.getString(p + "messageCommand", ""));
        settings.filters = json(f.getString(p + "filters", "[]"), new TypeToken<ValueList<UserFilter>>(){}.getType(), ValueObject.list());
        settings.channels = json(f.getString(p + "channels", "{}"), new TypeToken<ValueMap<ChatChannel>>(){}.getType(), ValueObject.map());
        settings.pms = json(f.getString(p + "privateMessages", "{}"), new TypeToken<ValueMap<ChatChannel>>(){}.getType(), ValueObject.map());
    }

    public static void saveServer(ServerSettings settings, String serverKey) {
        String p = "chat.tabby.servers." + safeKey(serverKey) + ".";
        GeneralServerSettings g = settings.general;
        UiEnhancementsConfig.file()
                .set(p + "channelsEnabled", g.channelsEnabled.get())
                .set(p + "pmEnabled", g.pmEnabled.get())
                .set(p + "channelPattern", g.channelPattern.get().name())
                .set(p + "messagePattern", g.messegePattern.get().name())
                .set(p + "useDefaultTab", g.useDefaultTab.get())
                .set(p + "ignoredChannels", new java.util.ArrayList<>(g.ignoredChannels.get()))
                .set(p + "defaultChannel", g.defaultChannel.get())
                .set(p + "channelCommand", g.channelCommand.get())
                .set(p + "messageCommand", g.messageCommand.get())
                .set(p + "filters", GSON.toJson(settings.filters))
                .set(p + "channels", GSON.toJson(settings.channels))
                .set(p + "privateMessages", GSON.toJson(settings.pms))
                .save();
    }

    private static void defineGlobal(NfrConfigFile f) {
        f.define("chat.tabby.general.logChat", true, "Write chat logs.")
                .define("chat.tabby.general.splitLog", true, "Split logs by channel.")
                .define("chat.tabby.general.timestampChat", false, "Prefix chat with timestamps.")
                .define("chat.tabby.general.timestampStyle", TimeStamps.MILITARYSECONDS.name(), "Timestamp format.")
                .define("chat.tabby.general.timestampColor", TextFormatting.WHITE.name(), "Timestamp color.")
                .define("chat.tabby.general.antiSpam", false, "Collapse duplicate messages.")
                .define("chat.tabby.general.antiSpamPrejudice", 0D, "Anti-spam comparison tolerance.")
                .define("chat.tabby.general.unreadFlashing", true, "Flash tabs with unread messages.")
                .define("chat.tabby.general.spelling", true, "Enable spelling assistance.")
                .define("chat.tabby.layout.x", 5, "Chat panel X position.")
                .define("chat.tabby.layout.y", 17, "Chat panel Y position.")
                .define("chat.tabby.layout.width", 300, "Chat panel width.")
                .define("chat.tabby.layout.height", 160, "Chat panel height.")
                .define("chat.tabby.layout.unfocusedHeight", 0.5D, "Unfocused chat height ratio.")
                .define("chat.tabby.layout.fadeTime", 200, "Chat fade time in ticks.")
                .define("chat.tabby.layout.hideTag", false, "Hide the active channel tag.")
                .define("chat.tabby.layout.keepChatOpen", false, "Keep chat open after sending.")
                .define("chat.tabby.layout.visibility", ChatVisibility.NORMAL.name(), "Chat visibility mode.");
    }

    private static String safeKey(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try { return Enum.valueOf(type, value); } catch (RuntimeException ignored) { return fallback; }
    }

    private static <T> T json(String value, java.lang.reflect.Type type, T fallback) {
        try { T parsed = GSON.fromJson(value, type); return parsed == null ? fallback : parsed; }
        catch (RuntimeException ignored) { return fallback; }
    }
}
