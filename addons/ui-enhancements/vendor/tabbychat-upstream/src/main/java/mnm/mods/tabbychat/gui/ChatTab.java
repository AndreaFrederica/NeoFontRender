package mnm.mods.tabbychat.gui;

import com.google.common.eventbus.Subscribe;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.Channel;
import mnm.mods.tabbychat.api.ChannelStatus;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import mnm.mods.tabbychat.util.ChatVisibility;
import mnm.mods.util.Color;
import mnm.mods.util.ILocation;
import mnm.mods.util.TexturedModal;
import mnm.mods.util.gui.GuiButton;
import mnm.mods.util.gui.events.GuiMouseEvent;
import mnm.mods.util.gui.events.GuiMouseEvent.MouseEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;

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
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, mc.gameSettings.chatOpacity);
            if (ChatStyleConfig.enabled) {
                ChatStyleRenderer.panel(loc.getWidth(), loc.getHeight(), styleColor(status),
                        ChatStyleConfig.border, mc.gameSettings.chatOpacity);
            } else {
                drawModalCorners(getStatusModal());
            }

            int txtX = loc.getWidth() / 2;
            int txtY = loc.getHeight() / 2 - 2;

            Color primary = getPrimaryColorProperty();
            int color = ChatStyleConfig.enabled
                    ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity)
                    : Color.getColor(primary.getRed(), primary.getGreen(), primary.getBlue(), (int) (mc.gameSettings.chatOpacity * 255));
            this.drawCenteredString(mc.fontRenderer, this.getText(), txtX, txtY, color);
            GlStateManager.disableBlend();
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

    private int styleColor(ChannelStatus status) {
        if (isHovered()) return ChatStyleConfig.hoveredTab;
        if (status == ChannelStatus.ACTIVE) return ChatStyleConfig.activeTab;
        if (status == ChannelStatus.UNREAD) return ChatStyleConfig.unreadTab;
        if (status == ChannelStatus.PINGED) return ChatStyleConfig.pingedTab;
        return ChatStyleConfig.tabBackground;
    }

    @Nonnull
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(mc.fontRenderer.getStringWidth(getText()) + 8, 14);
    }
}
