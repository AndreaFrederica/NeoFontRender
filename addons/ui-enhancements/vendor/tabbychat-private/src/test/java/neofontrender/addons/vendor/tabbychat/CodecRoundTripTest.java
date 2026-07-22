package neofontrender.addons.vendor.tabbychat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import neofontrender.addons.vendor.tabbychat.api.Message;
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
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodecRoundTripTest {

    @Test
    public void settingsRoundTripPreservesAllValues() {
        GeneralSettings general = new GeneralSettings();
        general.logChat.set(false);
        general.splitLog.set(false);
        general.timestampChat.set(true);
        general.timestampStyle.set(TimeStamps.MILITARY);
        general.timestampColor.set(EnumChatFormatting.DARK_AQUA);
        general.antiSpam.set(true);
        general.antiSpamPrejudice.set(0.42D);
        general.unreadFlashing.set(false);
        general.checkUpdates.set(false);

        GeneralSettings restoredGeneral = SettingsCodec.readGeneral(
                SettingsCodec.writeGeneral(general));
        assertFalse(restoredGeneral.logChat.get());
        assertFalse(restoredGeneral.splitLog.get());
        assertTrue(restoredGeneral.timestampChat.get());
        assertEquals(TimeStamps.MILITARY, restoredGeneral.timestampStyle.get());
        assertEquals(EnumChatFormatting.DARK_AQUA, restoredGeneral.timestampColor.get());
        assertTrue(restoredGeneral.antiSpam.get());
        assertEquals(0.42D, restoredGeneral.antiSpamPrejudice.get(), 0D);
        assertFalse(restoredGeneral.unreadFlashing.get());
        assertFalse(restoredGeneral.checkUpdates.get());

        AdvancedSettings advanced = new AdvancedSettings();
        advanced.chatX.set(11);
        advanced.chatY.set(22);
        advanced.chatW.set(333);
        advanced.chatH.set(144);
        advanced.unfocHeight.set(0.75F);
        advanced.fadeTime.set(345);
        advanced.historyLen.set(678);
        advanced.hideTag.set(true);
        advanced.keepChatOpen.set(true);
        advanced.spelling.set(false);
        advanced.visibility.set(ChatVisibility.HIDDEN);

        AdvancedSettings restoredAdvanced = SettingsCodec.readAdvanced(
                SettingsCodec.writeAdvanced(advanced));
        assertEquals(11, restoredAdvanced.chatX.get().intValue());
        assertEquals(22, restoredAdvanced.chatY.get().intValue());
        assertEquals(333, restoredAdvanced.chatW.get().intValue());
        assertEquals(144, restoredAdvanced.chatH.get().intValue());
        assertEquals(0.75F, restoredAdvanced.unfocHeight.get(), 0F);
        assertEquals(345, restoredAdvanced.fadeTime.get().intValue());
        assertEquals(678, restoredAdvanced.historyLen.get().intValue());
        assertTrue(restoredAdvanced.hideTag.get());
        assertTrue(restoredAdvanced.keepChatOpen.get());
        assertFalse(restoredAdvanced.spelling.get());
        assertEquals(ChatVisibility.HIDDEN, restoredAdvanced.visibility.get());

        GeneralServerSettings server = new GeneralServerSettings();
        server.channelsEnabled.set(false);
        server.pmEnabled.set(false);
        server.channelPattern.set(ChannelPatterns.ANGLES);
        server.messegePattern.set(MessagePatterns.ARROW);
        server.useDefaultTab.set(false);
        server.ignoredChannels.add("staff");
        server.ignoredChannels.add("trade");
        server.defaultChannel.set("global");
        server.channelCommand.set("/channel {}");
        server.messageCommand.set("/message {}");

        GeneralServerSettings restoredServer = SettingsCodec.readServerGeneral(
                SettingsCodec.writeServerGeneral(server));
        assertFalse(restoredServer.channelsEnabled.get());
        assertFalse(restoredServer.pmEnabled.get());
        assertEquals(ChannelPatterns.ANGLES, restoredServer.channelPattern.get());
        assertEquals(MessagePatterns.ARROW, restoredServer.messegePattern.get());
        assertFalse(restoredServer.useDefaultTab.get());
        assertEquals(server.ignoredChannels.get(), restoredServer.ignoredChannels.get());
        assertEquals("global", restoredServer.defaultChannel.get());
        assertEquals("/channel {}", restoredServer.channelCommand.get());
        assertEquals("/message {}", restoredServer.messageCommand.get());
    }

    @Test
    public void filterRoundTripPreservesBehavioralSettings() {
        UserFilter filter = new UserFilter();
        filter.setName("mentions");
        filter.setPattern("^hello (.+)$");
        filter.getSettings().getChannels().add("#alerts");
        filter.getSettings().getChannels().add("@moderator");
        filter.getSettings().setRemove(true);
        filter.getSettings().setRaw(false);
        filter.getSettings().setRegex(true);
        filter.getSettings().setFlags(Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        filter.getSettings().setSoundNotification(true);
        filter.getSettings().setSoundName("random.orb");
        ValueList<UserFilter> filters = ValueObject.list();
        filters.add(filter);

        ValueList<UserFilter> restored = SettingsCodec.readFilters(
                SettingsCodec.writeFilters(filters));
        UserFilter restoredFilter = restored.get(0);
        assertEquals("mentions", restoredFilter.getName());
        assertEquals("^hello (.+)$", restoredFilter.getRawPattern());
        assertEquals(filter.getSettings().getChannels(), restoredFilter.getSettings().getChannels());
        assertTrue(restoredFilter.getSettings().isRemove());
        assertFalse(restoredFilter.getSettings().isRaw());
        assertTrue(restoredFilter.getSettings().isRegex());
        assertEquals(filter.getSettings().getFlags(), restoredFilter.getSettings().getFlags());
        assertTrue(restoredFilter.getSettings().isSoundNotification());
        assertEquals("random.orb", restoredFilter.getSettings().getSoundName().get());
        assertTrue(restoredFilter.getPattern().matcher("HELLO world").matches());
    }

    @Test
    public void channelRoundTripKeepsChannelsAndPrivateMessagesSeparate() {
        ChatChannel global = new ChatChannel("global", false);
        global.setAlias("Global Chat");
        global.setPrefix("[G]");
        global.setPrefixHidden(true);
        global.setCommand("/channel global");
        ChatChannel alice = new ChatChannel("Alice", true);
        alice.setAlias("Alice PM");
        alice.setPrefix("[PM]");
        alice.setCommand("/message Alice");
        ValueMap<ChatChannel> channels = ValueObject.map();
        ValueMap<ChatChannel> pms = ValueObject.map();
        channels.set(global.getName(), global);
        pms.set(alice.getName(), alice);

        JsonObject json = SettingsCodec.writeChannels(channels, pms);
        ValueMap<ChatChannel> restoredChannels = SettingsCodec.readChannels(json, false);
        ValueMap<ChatChannel> restoredPms = SettingsCodec.readChannels(json, true);
        ChatChannel restoredGlobal = restoredChannels.get("global");
        ChatChannel restoredAlice = restoredPms.get("Alice");
        assertEquals("Global Chat", restoredGlobal.getAlias());
        assertEquals("[G]", restoredGlobal.getPrefix());
        assertTrue(restoredGlobal.isPrefixHidden());
        assertEquals("/channel global", restoredGlobal.getCommand());
        assertFalse(restoredGlobal.isPm());
        assertEquals("Alice PM", restoredAlice.getAlias());
        assertEquals("[PM]", restoredAlice.getPrefix());
        assertFalse(restoredAlice.isPrefixHidden());
        assertEquals("/message Alice", restoredAlice.getCommand());
        assertTrue(restoredAlice.isPm());
    }

    @Test
    public void messageRoundTripPreservesComponentIdAndDate() {
        IChatComponent root = new ChatComponentText("prefix ");
        IChatComponent link = new ChatComponentText("documentation");
        link.getChatStyle()
                .setColor(EnumChatFormatting.AQUA)
                .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.invalid"));
        root.appendSibling(link);
        Date date = new Date(1_723_000_000_000L);
        ChatMessage message = ChatMessage.restored(40, root, 17, date);

        JsonArray json = ChatHistoryCodec.writeMessages(Collections.<Message>singletonList(message));
        List<Message> restored = ChatHistoryCodec.readMessages(json, 99);
        Message restoredMessage = restored.get(0);
        assertEquals(99, restoredMessage.getCounter());
        assertEquals(17, restoredMessage.getID());
        assertEquals(date, restoredMessage.getDate());
        assertEquals(root.getFormattedText(), restoredMessage.getMessage().getFormattedText());
        IChatComponent restoredLink = restoredMessage.getMessage().getSiblings().get(0);
        assertEquals(EnumChatFormatting.AQUA, restoredLink.getChatStyle().getColor());
        assertEquals(ClickEvent.Action.OPEN_URL,
                restoredLink.getChatStyle().getChatClickEvent().getAction());
        assertEquals("https://example.invalid",
                restoredLink.getChatStyle().getChatClickEvent().getValue());

        ChatMessage undated = ChatMessage.restored(1, new ChatComponentText("old"), 0, null);
        Message restoredUndated = ChatHistoryCodec.readMessages(
                ChatHistoryCodec.writeMessages(Collections.<Message>singletonList(undated)), 2).get(0);
        assertNull(restoredUndated.getDate());
    }
}
