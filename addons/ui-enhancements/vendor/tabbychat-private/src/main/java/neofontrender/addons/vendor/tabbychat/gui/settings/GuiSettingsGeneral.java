package neofontrender.addons.vendor.tabbychat.gui.settings;

import static neofontrender.addons.vendor.tabbychat.util.Translation.*;

import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.settings.GeneralSettings;
import neofontrender.addons.vendor.tabbychat.settings.TabbySettings;
import neofontrender.addons.vendor.tabbychat.util.TimeStamps;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiGridLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiLabel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingBoolean;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingEnum;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSettingNumber.GuiSettingDouble;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import java.text.NumberFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiSettingsGeneral extends SettingPanel<TabbySettings> {

    public GuiSettingsGeneral() {
        setLayout(new GuiGridLayout(10, 20));
        setDisplayString(I18n.format(SETTINGS_GENERAL));
        setSecondaryColor(Color.of(255, 0, 255, 64));
    }

    @Override
    public void initGUI() {
        GeneralSettings sett = getSettings().general;

        int pos = 1;
        addComponent(new GuiLabel(new ChatComponentTranslation(LOG_CHAT)), new int[] { 2, pos });
        GuiSettingBoolean chkLogChat = new GuiSettingBoolean(sett.logChat);
        chkLogChat.setCaption(new ChatComponentTranslation(LOG_CHAT_DESC));
        addComponent(chkLogChat, new int[] { 1, pos });

        addComponent(new GuiLabel(new ChatComponentTranslation(SPLIT_LOG)), new int[] { 7, pos });
        GuiSettingBoolean chkSplitLog = new GuiSettingBoolean(sett.splitLog);
        chkSplitLog.setCaption(new ChatComponentTranslation(SPLIT_LOG_DESC));
        addComponent(chkSplitLog, new int[] { 6, pos });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(TIMESTAMP)), new int[] { 2, pos });
        addComponent(new GuiSettingBoolean(sett.timestampChat), new int[] { 1, pos });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(TIMESTAMP_STYLE)), new int[] { 3, pos });
        addComponent(new GuiSettingEnum<>(sett.timestampStyle, TimeStamps.values()), new int[] { 5, pos, 4, 1 });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(TIMESTAMP_COLOR)), new int[] { 3, pos });
        addComponent(new GuiSettingEnum<>(sett.timestampColor, getColors(), GuiSettingsGeneral::getColorName), new int[] { 5, pos, 4, 1 });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(ANTI_SPAM)), new int[] { 2, pos });
        GuiSettingBoolean chkSpam = new GuiSettingBoolean(sett.antiSpam);
        chkSpam.setCaption(new ChatComponentTranslation(ANTI_SPAM_DESC));
        addComponent(chkSpam, new int[] { 1, pos });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(SPAM_PREJUDICE)), new int[] { 3, pos });
        GuiSettingDouble nud = new GuiSettingDouble(sett.antiSpamPrejudice);
        nud.getComponent().setMin(0);
        nud.getComponent().setMax(1);
        nud.getComponent().setInterval(0.05);
        nud.getComponent().setFormat(NumberFormat.getPercentInstance());
        nud.setCaption(new ChatComponentTranslation(SPAM_PREJUDICE_DESC));
        addComponent(nud, new int[] { 6, pos, 2, 1 });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(UNREAD_FLASHING)), new int[] { 2, pos });
        addComponent(new GuiSettingBoolean(sett.unreadFlashing), new int[] { 1, pos });

        pos += 2;
        addComponent(new GuiLabel(new ChatComponentTranslation(CHECK_UPDATES)), new int[] { 2, pos });
        addComponent(new GuiSettingBoolean(sett.checkUpdates), new int[] { 1, pos });
    }

    private static List<EnumChatFormatting> getColors() {
        return Stream.of(EnumChatFormatting.values())
                .filter(EnumChatFormatting::isColor)
                .collect(Collectors.toList());
    }

    private static String getColorName(EnumChatFormatting input) {
        return "colors." + input.getFriendlyName();
    }

    @Override
    public TabbySettings getSettings() {
        return TabbyChat.getInstance().settings;
    }

}
