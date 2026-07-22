package neofontrender.addons.vendor.tabbychat.gui;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.ChannelStatus;
import neofontrender.addons.vendor.tabbychat.api.gui.IGui;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.TexturedModal;
import neofontrender.addons.vendor.tabbychat.foundation.config.Value;
import neofontrender.addons.vendor.tabbychat.foundation.gui.BorderLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.FlowLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.ILayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.ActionPerformedEvent;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;

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
        controls.addComponent(handle);
        this.addComponent(controls, BorderLayout.Position.EAST);

    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        GlState.color(1, 1, 1, mc.gameSettings.chatOpacity);
        if (GuiNewChatTC.getInstance().getChatOpen()) {
            drawModalCorners(MODAL);
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

        private Value<Boolean> value;

        ToggleButton() {
            this.value = TabbyChat.getInstance().settings.advanced.keepChatOpen;
        }

        @Override
        public void drawComponent(int mouseX, int mouseY) {
            GlState.enableBlend();
            int opac = (int)(mc.gameSettings.chatOpacity * 255) << 24;
            drawBorders(4, 4, 8, 8, 0x999999 | opac);
            if (value.get()) {
                Gui.drawRect(5, 5, 7, 7, 0xaaaaaa | opac);
            }
        }

        @Subscribe
        public void action(ActionPerformedEvent event) {
            value.set(!value.get());
        }

        @Override
        @Nonnull
        public Dimension getMinimumSize() {
            return new Dimension(8, 8);
        }
    }

}
