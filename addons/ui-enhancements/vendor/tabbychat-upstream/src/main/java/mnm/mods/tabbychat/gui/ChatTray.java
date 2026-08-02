package mnm.mods.tabbychat.gui;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import mnm.mods.tabbychat.ChatChannel;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.Channel;
import mnm.mods.tabbychat.api.ChannelStatus;
import mnm.mods.tabbychat.api.gui.IGui;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import mnm.mods.util.Color;
import mnm.mods.util.TexturedModal;
import mnm.mods.util.config.Value;
import mnm.mods.util.gui.BorderLayout;
import mnm.mods.util.gui.FlowLayout;
import mnm.mods.util.gui.GuiComponent;
import mnm.mods.util.gui.GuiPanel;
import mnm.mods.util.gui.ILayout;
import mnm.mods.util.gui.events.ActionPerformedEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;
import neofontrender.addons.chat.ChatKeepOpenPolicy;
import neofontrender.addons.chat.ChatHudWindowController;
import neofontrender.addons.chat.EnhancedChatConfigAccess;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Map;
import javax.annotation.Nonnull;

public class ChatTray extends GuiPanel implements IGui {

    private final static TexturedModal MODAL = new TexturedModal(ChatBox.GUI_LOCATION, 0, 14, 254, 202);

    private GuiPanel tabList = new GuiPanel(new FlowLayout());
    private GuiComponent handle = new ChatHandle();

    private Map<Channel, GuiComponent> map = Maps.newHashMap();


    ChatTray() {
        super(new BorderLayout());
        this.addComponent(tabList, BorderLayout.Position.CENTER);
        ChatPanel controls = new ChatPanel(new FlowLayout());
        controls.addComponent(new ToggleButton());
        controls.addComponent(new DetachButton());
        controls.addComponent(handle);
        this.addComponent(controls, BorderLayout.Position.EAST);

    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, mc.gameSettings.chatOpacity);
        if (ChatHudWindowController.isChatExpanded()) {
            if (ChatStyleConfig.enabled) {
                ChatStyleRenderer.panel(getBounds().width, getBounds().height,
                        ChatStyleConfig.trayBackground, ChatStyleConfig.border, mc.gameSettings.chatOpacity);
            } else {
                drawModalCorners(MODAL);
            }
        }
        super.drawComponent(mouseX, mouseY);
    }

    @Override
    public void updateComponent() {
        super.updateComponent();
        getParent()
                .map(GuiComponent::getSecondaryColorProperty)
                .map(color -> Color.of(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 4 * 3))
                .ifPresent(this::setSecondaryColor);
    }

    public void addChannel(Channel channel) {
        GuiComponent gc = new ChatTab(channel);
        map.put(channel, gc);
        tabList.addComponent(gc);
    }

    public void removeChannel(final Channel channel) {
        GuiComponent gc = map.get(channel);
        this.tabList.removeComponent(gc);
        map.remove(channel);
    }

    public void clear() {
        this.tabList.clearComponents();

        addChannel(ChatChannel.DEFAULT_CHANNEL);
        ChatChannel.DEFAULT_CHANNEL.setStatus(ChannelStatus.ACTIVE);
    }

    @Nonnull
    @Override
    public Dimension getMinimumSize() {
        return tabList.getLayout()
                .map(ILayout::getLayoutSize)
                .orElseGet(super::getMinimumSize);
    }

    boolean isHandleHovered() {
        return handle.isHovered();
    }

    @Override
    public Rectangle getBounds() {
        return this.getLocation().asRectangle();
    }

    private class ToggleButton extends GuiComponent {

        @Override
        public void drawComponent(int mouseX, int mouseY) {
            GlStateManager.enableBlend();
            int opac = (int)(mc.gameSettings.chatOpacity * 255) << 24;
            drawBorders(4, 4, 8, 8, 0x999999 | opac);
            if (ChatKeepOpenPolicy.shouldKeepOpen(TabbyChat.getInstance().getChat().getActiveChannel())) {
                Gui.drawRect(5, 5, 7, 7, 0xaaaaaa | opac);
            }
        }

        @Subscribe
        public void action(ActionPerformedEvent event) {
            Channel active = TabbyChat.getInstance().getChat().getActiveChannel();
            ChatKeepOpenPolicy.set(active, !ChatKeepOpenPolicy.shouldKeepOpen(active));
        }

        @Override
        @Nonnull
        public Dimension getMinimumSize() {
            return new Dimension(8, 8);
        }
    }

    /** Pops the expanded chat out of GuiChat into the persistent HUD compositor. */
    private class DetachButton extends GuiComponent {
        @Override
        public void drawComponent(int mouseX, int mouseY) {
            GlStateManager.enableBlend();
            int alpha = (int) (mc.gameSettings.chatOpacity * 255) << 24;
            int color = (isHovered() ? 0xffffa0 : 0xffffff) | alpha;
            if (EnhancedChatConfigAccess.persistentChatHudEnabled()) {
                // Arrow points back into the window while the chat is detached.
                drawHorizontalLine(2, 8, 4, color);
                drawVerticalLine(2, 4, 9, color);
                drawHorizontalLine(2, 5, 9, color);
                drawVerticalLine(8, 5, 9, color);
                drawHorizontalLine(2, 5, 8, color);
                drawVerticalLine(5, 2, 5, color);
            } else {
                // A window with an arrow leaving through its upper-right corner.
                drawHorizontalLine(2, 8, 9, color);
                drawVerticalLine(2, 4, 9, color);
                drawVerticalLine(8, 7, 9, color);
                drawHorizontalLine(5, 10, 2, color);
                drawVerticalLine(10, 2, 7, color);
                drawHorizontalLine(7, 10, 5, color);
                drawVerticalLine(7, 2, 5, color);
            }
            if (isHovered()) {
                String label = I18n.format(EnhancedChatConfigAccess.persistentChatHudEnabled()
                        ? "neofontrender_ui_enhancements.chat.hud.attach"
                        : "neofontrender_ui_enhancements.chat.hud.detach");
                drawCaption(label, -mc.fontRenderer.getStringWidth(label) - 6, 12);
            }
        }

        @Subscribe
        public void action(ActionPerformedEvent event) {
            ChatHudWindowController.toggleDetached();
        }

        @Override
        @Nonnull
        public Dimension getMinimumSize() {
            return new Dimension(12, 12);
        }
    }

}
