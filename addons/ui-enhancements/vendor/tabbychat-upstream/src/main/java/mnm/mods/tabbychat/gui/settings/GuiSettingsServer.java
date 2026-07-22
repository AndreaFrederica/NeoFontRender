package mnm.mods.tabbychat.gui.settings;

import static mnm.mods.tabbychat.util.Translation.*;

import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.settings.GeneralServerSettings;
import mnm.mods.tabbychat.settings.ServerSettings;
import mnm.mods.tabbychat.util.ChannelPatterns;
import mnm.mods.tabbychat.util.MessagePatterns;
import mnm.mods.util.Color;
import mnm.mods.util.gui.GuiGridLayout;
import mnm.mods.util.gui.GuiLabel;
import mnm.mods.util.gui.config.GuiSettingBoolean;
import mnm.mods.util.gui.config.GuiSettingEnum;
import mnm.mods.util.gui.config.GuiSettingString;
import mnm.mods.util.gui.config.GuiSettingStringList;
import mnm.mods.util.gui.config.SettingPanel;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;

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
        this.addComponent(new GuiLabel(new TextComponentTranslation(CHANNELS_ENABLED)), new int[]{2, pos});
        GuiSettingBoolean chkChannels = new GuiSettingBoolean(sett.channelsEnabled);
        chkChannels.setCaption(new TextComponentTranslation(CHANNELS_ENABLED_DESC));
        this.addComponent(chkChannels, new int[]{1, pos});

        pos += 1;
        this.addComponent(new GuiLabel(new TextComponentTranslation(PM_ENABLED)), new int[]{2, pos});
        GuiSettingBoolean chkPM = new GuiSettingBoolean(sett.pmEnabled);
        chkPM.setCaption(new TextComponentTranslation(PM_ENABLED_DESC));
        this.addComponent(chkPM, new int[]{1, pos});

        pos += 1;
        addComponent(new GuiLabel(new TextComponentTranslation(USE_DEFAULT)), new int[]{2, pos});
        addComponent(new GuiSettingBoolean(sett.useDefaultTab), new int[]{1, pos});

        pos += 2;
        this.addComponent(new GuiLabel(new TextComponentTranslation(CHANNEL_PATTERN)), new int[]{1, pos});
        GuiSettingEnum<ChannelPatterns> enmChanPat = new GuiSettingEnum<>(sett.channelPattern,
                ChannelPatterns.values());
        enmChanPat.setCaption(new TextComponentTranslation(CHANNEL_PATTERN_DESC));
        this.addComponent(enmChanPat, new int[]{5, pos, 4, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new TextComponentTranslation(MESSAGE_PATTERN)), new int[]{1, pos});
        if (sett.messegePattern.get() == null) {
            sett.messegePattern.set(MessagePatterns.WHISPERS);
        }
        GuiSettingEnum<MessagePatterns> enmMsg = new GuiSettingEnum<>(sett.messegePattern, MessagePatterns.values());
        enmMsg.setCaption(new TextComponentTranslation(MESSAGE_PATTERN_DESC));
        this.addComponent(enmMsg, new int[]{5, pos, 4, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new TextComponentTranslation(IGNORED_CHANNELS)), new int[]{1, pos});
        GuiSettingStringList strIgnored = new GuiSettingStringList(sett.ignoredChannels);
        strIgnored.setCaption(new TextComponentTranslation(IGNORED_CHANNELS_DESC));
        this.addComponent(strIgnored, new int[]{5, pos, 5, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new TextComponentTranslation(DEFAULT_CHANNEL_COMMAND)), new int[]{1, pos});
        GuiSettingString strChannels = new GuiSettingString(sett.channelCommand);
        strChannels.setCaption(new TextComponentTranslation(DEFAULT_CHANNEL_COMMAND_DESC));
        this.addComponent(strChannels, new int[]{5, pos, 5, 1});

        pos += 2;
        this.addComponent(new GuiLabel(new TextComponentTranslation(DEFAULT_CHANNEL)), new int[]{1, pos});
        GuiSettingString strMessages = new GuiSettingString(sett.defaultChannel);
        strMessages.setCaption(new TextComponentTranslation(DEFAULT_CHANNEL_DESC));
        this.addComponent(strMessages, new int[]{5, pos, 5, 1});
    }

    @Override
    public ServerSettings getSettings() {
        return TabbyChat.getInstance().serverSettings;
    }
}
