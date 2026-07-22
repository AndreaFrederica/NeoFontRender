package neofontrender.addons.vendor.tabbychat.gui;

import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.ChannelStatus;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.util.ChatVisibility;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.TexturedModal;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiButton;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent.MouseEvent;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;

import java.awt.Dimension;
import javax.annotation.Nonnull;

public class ChatTab extends GuiButton {

    private static final TexturedModal ACTIVE = new TexturedModal(ChatBox.GUI_LOCATION, 0, 0, 50, 14);
    private static final TexturedModal UNREAD = new TexturedModal(ChatBox.GUI_LOCATION, 50, 0, 50, 14);
    private static final TexturedModal PINGED = new TexturedModal(ChatBox.GUI_LOCATION, 100, 0, 50, 14);
    private static final TexturedModal HOVERED = new TexturedModal(ChatBox.GUI_LOCATION, 150, 0, 50, 14);
    private static final TexturedModal NONE = new TexturedModal(ChatBox.GUI_LOCATION, 200, 0, 50, 14);

    private final Channel channel;

    public ChatTab(Channel channel) {
        super(channel.getAlias());
        this.channel = channel;
    }

    @Subscribe
    public void tryCommitSudoku(GuiMouseEvent event) {
        if (event.getType() == MouseEvent.CLICK) {
            if (event.getButton() == 0) {
                if (GuiScreen.isShiftKeyDown()) {
                    // Remove channel
                    TabbyChat.getInstance().getChat().removeChannel(this.channel);
                } else {
                    // Enable channel, disable others
                    TabbyChat.getInstance().getChat().setActiveChannel(this.channel);
                }
            } else if (event.getButton() == 1) {
                // Open channel options
                this.channel.openSettings();
            } else if (event.getButton() == 2) {
                // middle click
                TabbyChat.getInstance().getChat().removeChannel(this.channel);
            }
        }
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        ChannelStatus status = channel.getStatus();
        if (GuiNewChatTC.getInstance().getChatOpen()
                || (status != null && (status.compareTo(ChannelStatus.PINGED) > 0) && (TabbyChat.getInstance().settings.general.unreadFlashing.get() == true))
                || TabbyChat.getInstance().settings.advanced.visibility.get() == ChatVisibility.ALWAYS) {
            ILocation loc = getLocation();
            GlState.enableBlend();
            GlState.color(1, 1, 1, mc.gameSettings.chatOpacity);
            drawModalCorners(getStatusModal());

            int txtX = loc.getWidth() / 2;
            int txtY = loc.getHeight() / 2 - 2;

            Color primary = getPrimaryColorProperty();
            int color = Color.getColor(primary.getRed(), primary.getGreen(), primary.getBlue(), (int) (mc.gameSettings.chatOpacity * 255));
            this.drawCenteredString(mc.fontRenderer, this.getText(), txtX, txtY, color);
            GlState.disableBlend();
        }
    }

    @Override
    public String getText() {
        String alias = channel.getAlias();

        if (channel.isPm()) {
            alias = "@" + alias;
        }
        ChannelStatus status = channel.getStatus();
        if (status != null) {
            switch (status) {
                case ACTIVE:
                    alias = "[" + alias + "]";
                    break;
                case UNREAD:
                    alias = "<" + alias + ">";
                    break;
                default:
                    break;
            }
        }
        return alias;
    }

    private TexturedModal getStatusModal() {
        if (isHovered()) {
            return HOVERED;
        }
        ChannelStatus status = channel.getStatus();
        if (status != null) {
            switch (status) {
                case ACTIVE:
                    return ACTIVE;
                case UNREAD:
                    return UNREAD;
                case PINGED:
                    return PINGED;
            }
        }
        return NONE;
    }

    @Nonnull
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(mc.fontRenderer.getStringWidth(getText()) + 8, 14);
    }
}
