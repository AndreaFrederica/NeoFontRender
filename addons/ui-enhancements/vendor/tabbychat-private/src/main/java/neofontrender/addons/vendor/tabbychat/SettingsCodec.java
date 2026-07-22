package neofontrender.addons.vendor.tabbychat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import neofontrender.addons.vendor.tabbychat.api.filters.FilterSettings;
import neofontrender.addons.vendor.tabbychat.extra.filters.UserFilter;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueList;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueMap;
import neofontrender.addons.vendor.tabbychat.foundation.config.ValueObject;
import neofontrender.addons.vendor.tabbychat.settings.AdvancedSettings;
import neofontrender.addons.vendor.tabbychat.settings.GeneralServerSettings;
import neofontrender.addons.vendor.tabbychat.settings.GeneralSettings;
import neofontrender.addons.vendor.tabbychat.util.ChannelPatterns;
import neofontrender.addons.vendor.tabbychat.util.ChatVisibility;
import neofontrender.addons.vendor.tabbychat.util.MessagePatterns;
import neofontrender.addons.vendor.tabbychat.util.TimeStamps;
import net.minecraft.util.EnumChatFormatting;

import java.util.Map;

/** Explicit JSON codec for settings whose generic Value wrappers cannot be decoded safely by reflection. */
public final class SettingsCodec {

    private SettingsCodec() {}

    public static JsonObject writeGeneral(GeneralSettings settings) {
        JsonObject json = new JsonObject();
        json.addProperty("logChat", settings.logChat.get());
        json.addProperty("splitLog", settings.splitLog.get());
        json.addProperty("timestampChat", settings.timestampChat.get());
        json.addProperty("timestampStyle", settings.timestampStyle.get().name());
        json.addProperty("timestampColor", settings.timestampColor.get().name());
        json.addProperty("antiSpam", settings.antiSpam.get());
        json.addProperty("antiSpamPrejudice", settings.antiSpamPrejudice.get());
        json.addProperty("unreadFlashing", settings.unreadFlashing.get());
        json.addProperty("checkUpdates", settings.checkUpdates.get());
        return json;
    }

    public static GeneralSettings readGeneral(JsonElement element) {
        JsonObject json = object(element, "general settings");
        GeneralSettings settings = new GeneralSettings();
        settings.logChat.set(bool(json, "logChat"));
        settings.splitLog.set(bool(json, "splitLog"));
        settings.timestampChat.set(bool(json, "timestampChat"));
        settings.timestampStyle.set(TimeStamps.valueOf(string(json, "timestampStyle")));
        settings.timestampColor.set(EnumChatFormatting.valueOf(string(json, "timestampColor")));
        settings.antiSpam.set(bool(json, "antiSpam"));
        settings.antiSpamPrejudice.set(number(json, "antiSpamPrejudice").doubleValue());
        settings.unreadFlashing.set(bool(json, "unreadFlashing"));
        settings.checkUpdates.set(bool(json, "checkUpdates"));
        return settings;
    }

    public static JsonObject writeAdvanced(AdvancedSettings settings) {
        JsonObject json = new JsonObject();
        json.addProperty("chatX", settings.chatX.get());
        json.addProperty("chatY", settings.chatY.get());
        json.addProperty("chatW", settings.chatW.get());
        json.addProperty("chatH", settings.chatH.get());
        json.addProperty("unfocHeight", settings.unfocHeight.get());
        json.addProperty("fadeTime", settings.fadeTime.get());
        json.addProperty("historyLen", settings.historyLen.get());
        json.addProperty("hideTag", settings.hideTag.get());
        json.addProperty("keepChatOpen", settings.keepChatOpen.get());
        json.addProperty("spelling", settings.spelling.get());
        json.addProperty("visibility", settings.visibility.get().name());
        return json;
    }

    public static AdvancedSettings readAdvanced(JsonElement element) {
        JsonObject json = object(element, "advanced settings");
        AdvancedSettings settings = new AdvancedSettings();
        settings.chatX.set(number(json, "chatX").intValue());
        settings.chatY.set(number(json, "chatY").intValue());
        settings.chatW.set(number(json, "chatW").intValue());
        settings.chatH.set(number(json, "chatH").intValue());
        settings.unfocHeight.set(number(json, "unfocHeight").floatValue());
        settings.fadeTime.set(number(json, "fadeTime").intValue());
        settings.historyLen.set(number(json, "historyLen").intValue());
        settings.hideTag.set(bool(json, "hideTag"));
        settings.keepChatOpen.set(bool(json, "keepChatOpen"));
        settings.spelling.set(bool(json, "spelling"));
        settings.visibility.set(ChatVisibility.valueOf(string(json, "visibility")));
        return settings;
    }

    public static JsonObject writeServerGeneral(GeneralServerSettings settings) {
        JsonObject json = new JsonObject();
        json.addProperty("channelsEnabled", settings.channelsEnabled.get());
        json.addProperty("pmEnabled", settings.pmEnabled.get());
        json.addProperty("channelPattern", settings.channelPattern.get().name());
        json.addProperty("messagePattern", settings.messegePattern.get().name());
        json.addProperty("useDefaultTab", settings.useDefaultTab.get());
        JsonArray ignored = new JsonArray();
        for (String channel : settings.ignoredChannels) {
            ignored.add(new JsonPrimitive(channel));
        }
        json.add("ignoredChannels", ignored);
        json.addProperty("defaultChannel", settings.defaultChannel.get());
        json.addProperty("channelCommand", settings.channelCommand.get());
        json.addProperty("messageCommand", settings.messageCommand.get());
        return json;
    }

    public static GeneralServerSettings readServerGeneral(JsonElement element) {
        JsonObject json = object(element, "server settings");
        GeneralServerSettings settings = new GeneralServerSettings();
        settings.channelsEnabled.set(bool(json, "channelsEnabled"));
        settings.pmEnabled.set(bool(json, "pmEnabled"));
        settings.channelPattern.set(ChannelPatterns.valueOf(string(json, "channelPattern")));
        settings.messegePattern.set(MessagePatterns.valueOf(string(json, "messagePattern")));
        settings.useDefaultTab.set(bool(json, "useDefaultTab"));
        for (JsonElement channel : array(json, "ignoredChannels")) {
            settings.ignoredChannels.add(channel.getAsString());
        }
        settings.defaultChannel.set(string(json, "defaultChannel"));
        settings.channelCommand.set(string(json, "channelCommand"));
        settings.messageCommand.set(string(json, "messageCommand"));
        return settings;
    }

    public static JsonArray writeFilters(ValueList<UserFilter> filters) {
        JsonArray array = new JsonArray();
        for (UserFilter filter : filters) {
            JsonObject json = new JsonObject();
            json.addProperty("name", filter.getName());
            json.addProperty("pattern", filter.getRawPattern());
            FilterSettings source = filter.getSettings();
            JsonObject settings = new JsonObject();
            JsonArray channels = new JsonArray();
            for (String channel : source.getChannels()) {
                channels.add(new JsonPrimitive(channel));
            }
            settings.add("channels", channels);
            settings.addProperty("remove", source.isRemove());
            settings.addProperty("raw", source.isRaw());
            settings.addProperty("regex", source.isRegex());
            settings.addProperty("flags", source.getFlags());
            settings.addProperty("soundNotification", source.isSoundNotification());
            settings.addProperty("soundName", source.getSoundName().orElse(""));
            json.add("settings", settings);
            array.add(json);
        }
        return array;
    }

    public static ValueList<UserFilter> readFilters(JsonElement element) {
        ValueList<UserFilter> filters = ValueObject.list();
        for (JsonElement item : requiredArray(element, "filters")) {
            JsonObject json = object(item, "filter");
            UserFilter filter = new UserFilter();
            filter.setName(string(json, "name"));
            filter.setPattern(string(json, "pattern"));
            JsonObject settings = object(required(json, "settings"), "filter settings");
            FilterSettings target = filter.getSettings();
            for (JsonElement channel : array(settings, "channels")) {
                target.getChannels().add(channel.getAsString());
            }
            target.setRemove(bool(settings, "remove"));
            target.setRaw(bool(settings, "raw"));
            target.setRegex(bool(settings, "regex"));
            target.setFlags(number(settings, "flags").intValue());
            target.setSoundNotification(bool(settings, "soundNotification"));
            target.setSoundName(string(settings, "soundName"));
            filters.add(filter);
        }
        return filters;
    }

    public static JsonObject writeChannels(ValueMap<ChatChannel> channels, ValueMap<ChatChannel> pms) {
        JsonObject root = new JsonObject();
        root.add("channels", writeChannelMap(channels));
        root.add("pms", writeChannelMap(pms));
        return root;
    }

    public static ValueMap<ChatChannel> readChannels(JsonElement element, boolean pms) {
        JsonObject root = object(element, "channels");
        JsonObject json = object(required(root, pms ? "pms" : "channels"), "channel map");
        ValueMap<ChatChannel> channels = ValueObject.map();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonObject item = object(entry.getValue(), "channel");
            ChatChannel channel = new ChatChannel(entry.getKey(), pms);
            channel.setAlias(string(item, "alias"));
            channel.setPrefix(string(item, "prefix"));
            channel.setPrefixHidden(bool(item, "prefixHidden"));
            channel.setCommand(string(item, "command"));
            channels.set(entry.getKey(), channel);
        }
        return channels;
    }

    private static JsonObject writeChannelMap(ValueMap<ChatChannel> channels) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, ChatChannel> entry : channels.get().entrySet()) {
            ChatChannel channel = entry.getValue();
            JsonObject item = new JsonObject();
            item.addProperty("alias", channel.getAlias());
            item.addProperty("prefix", channel.getPrefix());
            item.addProperty("prefixHidden", channel.isPrefixHidden());
            item.addProperty("command", channel.getCommand());
            json.add(entry.getKey(), item);
        }
        return json;
    }

    private static JsonObject object(JsonElement element, String label) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(label + " must be a JSON object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonElement element, String label) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException(label + " must be a JSON array");
        }
        return element.getAsJsonArray();
    }

    private static JsonElement required(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Missing JSON field " + key);
        }
        return element;
    }

    private static JsonArray array(JsonObject object, String key) {
        return requiredArray(required(object, key), key);
    }

    private static String string(JsonObject object, String key) {
        return required(object, key).getAsString();
    }

    private static boolean bool(JsonObject object, String key) {
        return required(object, key).getAsBoolean();
    }

    private static Number number(JsonObject object, String key) {
        return required(object, key).getAsNumber();
    }
}
