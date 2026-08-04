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
import mnm.mods.util.gui.VerticalLayout;
import mnm.mods.util.gui.events.ActionPerformedEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;
import neofontrender.addons.chat.ChatKeepOpenPolicy;
import neofontrender.addons.chat.ChatHudWindowController;
import neofontrender.addons.chat.ChatTabPinPolicy;
import neofontrender.addons.chat.EnhancedChatConfigAccess;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public class ChatTray extends GuiPanel implements IGui {

    private final static TexturedModal MODAL = new TexturedModal(ChatBox.GUI_LOCATION, 0, 14, 254, 202);
    public static final int VERTICAL_WIDTH = 92;

    private GuiPanel tabList = new GuiPanel(new FlowLayout());
    private GuiComponent handle = new ChatHandle();
    private GuiPanel controls;
    private final List<Channel> displayOrder = new ArrayList<>();
    private boolean vertical;

    private Map<Channel, GuiComponent> map = Maps.newHashMap();

    ChatTray() {
        super(new BorderLayout());
        this.addComponent(tabList, BorderLayout.Position.CENTER);
        controls = new ChatPanel(new FlowLayout());
        controls.addComponent(new ToggleButton());
        controls.addComponent(new DetachButton());
        controls.addComponent(handle);
        this.addComponent(controls, BorderLayout.Position.EAST);

    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        // Layout switches mutate the component tree, so defer them out of the draw pass
        // (GuiPanel iterates its components while rendering; modifying them here crashes).
        if (vertical != EnhancedChatConfigAccess.verticalTabsEnabled()) {
            mc.addScheduledTask(() -> getParent().filter(parent -> parent instanceof ChatBox)
                    .ifPresent(parent -> ((ChatBox) parent).applyTabLayout()));
        }
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

    /** Switches the tab strip between horizontal (FlowLayout) and vertical (Edge-style) layouts. */
    public void applyVertical(boolean vertical) {
        this.vertical = vertical;
        this.removeComponent(tabList);
        tabList = new GuiPanel(vertical ? new VerticalLayout() : new FlowLayout());
        this.addComponent(tabList, BorderLayout.Position.CENTER);
        reorder();
    }

    /** Moves the controls into a host panel (used to dock them at the window's top-right in vertical mode). */
    public void detachControls(GuiPanel host) {
        this.removeComponent(controls);
        if (host != null) {
            host.clearComponents();
            host.addComponent(controls, BorderLayout.Position.EAST);
        }
    }

    /** Returns the controls to the right edge of the tray (horizontal mode). */
    public void attachControls() {
        controls.getParent().ifPresent(parent -> parent.removeComponent(controls));
        this.addComponent(controls, BorderLayout.Position.EAST);
    }

    public void addChannel(Channel channel) {
        if (mc.isCallingFromMinecraftThread()) {
            doAddChannel(channel);
        } else {
            mc.addScheduledTask(() -> doAddChannel(channel));
        }
    }

    private void doAddChannel(Channel channel) {
        if (!displayOrder.contains(channel)) displayOrder.add(channel);
        GuiComponent gc = new ChatTab(channel);
        map.put(channel, gc);
        reorder();
    }

    public void removeChannel(final Channel channel) {
        if (mc.isCallingFromMinecraftThread()) {
            doRemoveChannel(channel);
        } else {
            mc.addScheduledTask(() -> doRemoveChannel(channel));
        }
    }

    private void doRemoveChannel(Channel channel) {
        GuiComponent gc = map.get(channel);
        this.tabList.removeComponent(gc);
        map.remove(channel);
        displayOrder.remove(channel);
    }

    /** Re-applies pin ordering after a pin state change. */
    public void refreshPins() {
        if (mc.isCallingFromMinecraftThread()) {
            reorder();
        } else {
            mc.addScheduledTask(this::reorder);
        }
    }

    private void reorder() {
        tabList.clearComponents();
        for (Channel channel : ChatTabPinPolicy.ordered(displayOrder)) {
            GuiComponent gc = map.get(channel);
            if (gc != null) tabList.addComponent(gc);
        }
    }

    @Override
    public void updateComponent() {
        super.updateComponent();
        getParent()
                .map(GuiComponent::getSecondaryColorProperty)
                .map(color -> Color.of(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 4 * 3))
                .ifPresent(this::setSecondaryColor);
    }

    public void clear() {
        if (mc.isCallingFromMinecraftThread()) {
            doClear();
        } else {
            mc.addScheduledTask(this::doClear);
        }
    }

    private void doClear() {
        this.tabList.clearComponents();
        displayOrder.clear();

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
