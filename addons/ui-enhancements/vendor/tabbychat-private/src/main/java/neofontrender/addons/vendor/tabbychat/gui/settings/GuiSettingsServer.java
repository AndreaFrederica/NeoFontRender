package neofontrender.addons.vendor.tabbychat.gui.settings;

import static neofontrender.addons.vendor.tabbychat.util.Translation.*;

import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.settings.GeneralServerSettings;
import neofontrender.addons.vendor.tabbychat.settings.ServerSettings;
import neofontrender.addons.vendor.tabbychat.util.ChannelPatterns;
import neofontrender.addons.vendor.tabbychat.util.MessagePatterns;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiGridLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiLabel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingBoolean;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingEnum;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingString;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingStringList;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;

public class GuiSettingsServer extends SettingPanel<ServerSettings> {

    GuiSettingsServer() {
        this.setLayout(new GuiGridLayout(10, 20));
        this.setDisplayString(I18n.format(SETTINGS_SERVER));
        this.setSecondaryColor(Color.of(255, 215, 0, 64));
    }

    @Override
    public void initGUI() {
        GeneralServerSettings sett = getSettings().general;

        int pos = 1;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNELS_ENABLED)), new int[]{2, pos});
        GuiSettingBoolean chkChannels = new GuiSettingBoolean(sett.channelsEnabled);
        chkChannels.setCaption(new ChatComponentTranslation(CHANNELS_ENABLED_DESC));
        this.addComponent(chkChannels, new int[]{1, pos});

        pos += 1;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(PM_ENABLED)), new int[]{2, pos});
        GuiSettingBoolean chkPM = new GuiSettingBoolean(sett.pmEnabled);
        chkPM.setCaption(new ChatComponentTranslation(PM_ENABLED_DESC));
        this.addComponent(chkPM, new int[]{1, pos});

        pos += 1;
        addComponent(new GuiLabel(new ChatComponentTranslation(USE_DEFAULT)), new int[]{2, pos});
        addComponent(new GuiSettingBoolean(sett.useDefaultTab), new int[]{1, pos});

        pos += 2;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_PATTERN)), new int[]{1, pos});
        GuiSettingEnum<ChannelPatterns> enmChanPat = new GuiSettingEnum<>(sett.channelPattern,
                ChannelPatterns.values());
        enmChanPat.setCaption(new ChatComponentTranslation(CHANNEL_PATTERN_DESC));
        this.addComponent(enmChanPat, new int[]{5, pos, 4, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(MESSAGE_PATTERN)), new int[]{1, pos});
        if (sett.messegePattern.get() == null) {
            sett.messegePattern.set(MessagePatterns.WHISPERS);
        }
        GuiSettingEnum<MessagePatterns> enmMsg = new GuiSettingEnum<>(sett.messegePattern, MessagePatterns.values());
        enmMsg.setCaption(new ChatComponentTranslation(MESSAGE_PATTERN_DESC));
        this.addComponent(enmMsg, new int[]{5, pos, 4, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(IGNORED_CHANNELS)), new int[]{1, pos});
        GuiSettingStringList strIgnored = new GuiSettingStringList(sett.ignoredChannels);
        strIgnored.setCaption(new ChatComponentTranslation(IGNORED_CHANNELS_DESC));
        this.addComponent(strIgnored, new int[]{5, pos, 5, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(DEFAULT_CHANNEL_COMMAND)), new int[]{1, pos});
        GuiSettingString strChannels = new GuiSettingString(sett.channelCommand);
        strChannels.setCaption(new ChatComponentTranslation(DEFAULT_CHANNEL_COMMAND_DESC));
        this.addComponent(strChannels, new int[]{5, pos, 5, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new ChatComponentTranslation(DEFAULT_CHANNEL)), new int[]{1, pos});
        GuiSettingString strMessages = new GuiSettingString(sett.defaultChannel);
        strMessages.setCaption(new ChatComponentTranslation(DEFAULT_CHANNEL_DESC));
        this.addComponent(strMessages, new int[]{5, pos, 5, 1});
    }

    @Override
    public ServerSettings getSettings() {
        return TabbyChat.getInstance().serverSettings;
    }
}
