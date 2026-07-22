package neofontrender.addons.vendor.tabbychat.gui.settings;

import static neofontrender.addons.vendor.tabbychat.util.Translation.*;

import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.settings.ServerSettings;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.Location;
import neofontrender.addons.vendor.tabbychat.foundation.gui.BorderLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiButton;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiCheckbox;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiGridLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiLabel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiScrollingPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiText;
import neofontrender.addons.vendor.tabbychat.foundation.gui.VerticalLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.ActionPerformedEvent;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;

public class GuiSettingsChannel extends SettingPanel<ServerSettings> {

    private Channel channel;

    private GuiScrollingPanel channels;
    private GuiPanel panel;

    private GuiText alias;
    private GuiText prefix;
    private GuiCheckbox hidePrefix;
    private GuiText command;

    GuiSettingsChannel() {
        this(null);
    }

    public GuiSettingsChannel(Channel channel) {
        this.channel = channel;
        this.setLayout(new BorderLayout());
        this.setDisplayString(I18n.format(CHANNEL_TITLE));
        this.setSecondaryColor(Color.of(0, 15, 100, 65));

    }

    @Override
    public void initGUI() {
        channels = new GuiScrollingPanel();
        channels.setLocation(new Location(0, 0, 60, 200));
        channels.getContentPanel().setLayout(new VerticalLayout());
        for (Channel channel : getSettings().channels.get().values()) {
            channels.getContentPanel().addComponent(new ChannelButton(channel));
        }
        this.addComponent(channels, BorderLayout.Position.WEST);
        panel = new GuiPanel();
        panel.setLayout(new GuiGridLayout(8, 20));
        this.addComponent(panel, BorderLayout.Position.CENTER);

        this.select(channel);
    }

    private void select(Channel channel) {

        for (GuiComponent comp : channels.getContentPanel()) {
            if (((ChannelButton) comp).channel == channel) {
                comp.setEnabled(false);
            } else {
                comp.setEnabled(true);
            }
        }

        int pos = 1;

        this.channel = channel;
        this.panel.clearComponents();
        if (channel == null) {
            if (channels.getContentPanel().getComponentCount() > 0) {
                this.panel.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_SELECT)), new int[] { 1, pos });
            } else {
                this.panel.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_NONE)), new int[] { 1, pos });
            }
            return;
        }
        this.panel.addComponent(
                new GuiLabel(new ChatComponentTranslation(CHANNEL_LABEL, channel.getName())),
                new int[] { 1, pos });

        pos += 3;
        this.panel.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_ALIAS)), new int[] { 1, pos });
        this.panel.addComponent(alias = new GuiText(), new int[] { 3, pos, 4, 1 });
        alias.setValue(channel.getAlias());

        pos += 2;
        this.panel.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_PREFIX)), new int[] { 1, pos });
        this.panel.addComponent(prefix = new GuiText(), new int[] { 3, pos, 4, 1 });
        prefix.setValue(channel.getPrefix());

        pos += 2;
        this.panel.addComponent(hidePrefix = new GuiCheckbox(), new int[] { 1, pos });
        hidePrefix.setValue(channel.isPrefixHidden());
        this.panel.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_HIDE_PREFIX)), new int[] { 2, pos });

        pos += 2;
        this.panel.addComponent(command = new GuiText(), new int[] { 3, pos, 4, 1 });
        command.setValue(channel.getCommand());
        this.panel.addComponent(new GuiLabel(new ChatComponentTranslation(CHANNEL_COMMAND)), new int[] { 1, pos });

        GuiButton accept = new GuiButton(I18n.format("gui.done"));
        accept.getBus().register(new Object() {
            @Subscribe
            public void somebodySaveMe(ActionPerformedEvent event) {
                save();
            }
        });
        this.panel.addComponent(accept, new int[] { 2, 15, 4, 2 });

        GuiButton forget = new GuiButton(I18n.format(CHANNEL_FORGET));
        forget.getBus().register(new Object() {
            @Subscribe
            public void oohShinyObject(ActionPerformedEvent event) {
                Channel channel = GuiSettingsChannel.this.channel;
                // remove from chat
                TabbyChat.getInstance().getChat().removeChannel(channel);
                // remove from settings file
                getSettings().channels.get().remove(channel.getName());
                if (!channel.isPm()) {
                    // don't add this channel again.
                    getSettings().general.ignoredChannels.add(channel.getName());
                }
                // remove from settings gui
                for (GuiComponent comp : channels.getContentPanel()) {
                    if (comp instanceof ChannelButton && ((ChannelButton) comp).channel == channel) {
                        channels.getContentPanel().removeComponent(comp);
                        break;
                    }
                }
                select(null);
            }
        });
        this.panel.addComponent(forget, new int[] { 2, 17, 4, 2 });
    }

    private void save() {
        channel.setAlias(alias.getValue());
        channel.setPrefix(prefix.getValue());
        channel.setPrefixHidden(hidePrefix.getValue());
        channel.setCommand(command.getValue());
    }

    @Override
    public ServerSettings getSettings() {
        return TabbyChat.getInstance().serverSettings;
    }

    public class ChannelButton extends GuiButton {

        private Channel channel;

        ChannelButton(Channel channel) {
            super(channel.getName());
            this.channel = channel;
            setLocation(new Location(0, 0, 60, 15));
        }

        @Subscribe
        public void margeChangeTheChannel(ActionPerformedEvent event) {
            select(channel);
        }
    }
}
