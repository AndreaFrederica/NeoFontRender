package mnm.mods.tabbychat.gui;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import mnm.mods.tabbychat.ChatChannel;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.Message;
import mnm.mods.tabbychat.api.gui.ReceivedChat;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import mnm.mods.tabbychat.util.ChatTextUtils;
import mnm.mods.tabbychat.util.ChatVisibility;
import mnm.mods.util.Color;
import mnm.mods.util.ILocation;
import mnm.mods.util.TexturedModal;
import mnm.mods.util.gui.GuiComponent;
import mnm.mods.util.gui.events.GuiMouseEvent;
import mnm.mods.util.gui.events.GuiMouseEvent.MouseEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

public class ChatArea extends GuiComponent implements ReceivedChat {

    private static final TexturedModal MODAL = new TexturedModal(ChatBox.GUI_LOCATION, 0, 14, 254, 205);

    private ChatChannel channel;
    private List<Message> messages = Lists.newLinkedList();
    private boolean dirty;
    private int scrollPos = 0;
    private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();
    private float nfrUi$displayScroll;

    public ChatArea() {
        this.setMinimumSize(new Dimension(300, 160));
    }

    @Subscribe
    public void superScrollingAction(GuiMouseEvent event) {
        if (event.getType() == MouseEvent.SCROLL) {
            // Scrolling
            int scroll = event.getScroll();
            // One tick = 120
            if (scroll != 0) {
                if (scroll > 1) {
                    scroll = 1;
                }
                if (scroll < -1) {
                    scroll = -1;
                }
                if (GuiScreen.isShiftKeyDown()) {
                    scroll *= 7;
                }
                scroll(scroll);

            }
        }
    }

    @Override
    public void onClosed() {
        resetScroll();
        super.onClosed();
    }

    @Override
    public ILocation getLocation() {
        List<Message> visible = getVisibleChat();
        int height = visible.size() * mc.fontRenderer.FONT_HEIGHT;
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();

        if (GuiNewChatTC.getInstance().getChatOpen() || vis == ChatVisibility.ALWAYS) {
            return super.getLocation();
        } else if (height != 0) {
            int y = super.getLocation().getHeight() - height;
            return super.getLocation().copy().move(0, y - 2).setHeight(height + 2);
        }
        return super.getLocation();
    }

    @Override
    public boolean isVisible() {

        List<Message> visible = getVisibleChat();
        int height = visible.size() * mc.fontRenderer.FONT_HEIGHT;
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();

        return mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN
                && (GuiNewChatTC.getInstance().getChatOpen() || vis == ChatVisibility.ALWAYS || height != 0);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {

        int maxScroll = Math.max(0, getChat().size() - GuiNewChatTC.getInstance().getLineCount());
        nfrUi$displayScroll = SmoothScrollConfigAccess.chatEnabled()
                ? nfrUi$scroller.update(nfrUi$displayScroll, maxScroll) : scrollPos;

        List<Message> visible = getVisibleChat();
        GlStateManager.enableBlend();
        float opac = mc.gameSettings.chatOpacity;
        GlStateManager.color(1, 1, 1, opac);

        if (ChatStyleConfig.enabled) {
            ChatStyleRenderer.panel(getBounds().width, getBounds().height,
                    ChatStyleConfig.background, ChatStyleConfig.border, mc.gameSettings.chatOpacity);
        } else {
            drawModalCorners(MODAL);
        }

        zLevel = 100;
        // TODO abstracted padding
        int xPos = getBounds().x + 3;
        float fraction = nfrUi$displayScroll - (float) Math.floor(nfrUi$displayScroll);
        int yPos = getBounds().height + Math.round(fraction * mc.fontRenderer.FONT_HEIGHT);
        for (Message line : visible) {
            yPos -= mc.fontRenderer.FONT_HEIGHT;
            drawChatLine(line, xPos, yPos);
        }
        zLevel = 0;
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    private void drawChatLine(Message line, int xPos, int yPos) {
        String text = line.getMessageWithOptionalTimestamp().getFormattedText();
        int configured = ChatStyleConfig.enabled
                ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity) : Color.WHITE.getHex();
        int alpha = Math.min(configured >>> 24, getLineOpacity(line));
        mc.fontRenderer.drawStringWithShadow(text, xPos, yPos, configured & 0x00FFFFFF | alpha << 24);
    }

    public void setChannel(ChatChannel channel) {
        this.channel = channel;
        this.markDirty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public List<Message> getChat() {
        if (!dirty) {
            return this.messages;
        }
        this.dirty = false;
        this.messages = ChatTextUtils.split(channel.getMessages(), getBounds().width - 6);
        return this.messages;

    }

    private List<Message> getVisibleChat() {
        List<Message> lines = getChat();

        List<Message> messages = Lists.newArrayList();
        int length = 0;

        int pos = SmoothScrollConfigAccess.chatEnabled()
                ? MathHelper.floor(nfrUi$displayScroll) : getScrollPos();
        float unfoc = TabbyChat.getInstance().settings.advanced.unfocHeight.get();
        float div = GuiNewChatTC.getInstance().getChatOpen() ? 1 : unfoc;
        while (pos < lines.size() && length < super.getLocation().getHeight() * div - 10) {
            Message line = lines.get(pos);

            if (GuiNewChatTC.getInstance().getChatOpen()) {
                messages.add(line);
            } else if (getLineOpacity(line) > 3) {
                messages.add(line);
            } else {
                break;
            }

            pos++;
            length += mc.fontRenderer.FONT_HEIGHT;
        }

        return messages;
    }

    private int getLineOpacity(Message line) {
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();
        if (vis == ChatVisibility.ALWAYS)
            return 4;
        if (vis == ChatVisibility.HIDDEN && !GuiNewChatTC.getInstance().getChatOpen())
            return 0;
        int opacity = (int) (mc.gameSettings.chatOpacity * 255);

        double age = mc.ingameGUI.getUpdateCounter() - line.getCounter();
        if (!mc.ingameGUI.getChatGUI().getChatOpen()) {
            double opacPerc = age / TabbyChat.getInstance().settings.advanced.fadeTime.get();
            opacPerc = 1.0D - opacPerc;
            opacPerc *= 10.0D;

            opacPerc = Math.max(0, opacPerc);
            opacPerc = Math.min(1, opacPerc);

            opacPerc *= opacPerc;
            opacity = (int) (opacity * opacPerc);
        }
        return opacity;
    }

    @Override
    public void scroll(int scr) {
        if (!SmoothScrollConfigAccess.chatEnabled()) {
            setScrollPos(getScrollPos() + scr);
            return;
        }
        int maxScroll = Math.max(0, getChat().size() - GuiNewChatTC.getInstance().getLineCount());
        nfrUi$scroller.scrollBy(scr, maxScroll, nfrUi$displayScroll);
        scrollPos = MathHelper.clamp(Math.round(nfrUi$scroller.getTarget()), 0, maxScroll);
    }

    @Override
    public void setScrollPos(int scroll) {
        List<Message> list = getChat();
        scroll = Math.min(scroll, list.size() - GuiNewChatTC.getInstance().getLineCount());
        scroll = Math.max(scroll, 0);

        this.scrollPos = scroll;
        this.nfrUi$displayScroll = scroll;
        this.nfrUi$scroller.sync(scroll);
    }

    @Override
    public int getScrollPos() {
        return scrollPos;
    }

    @Override
    public void resetScroll() {
        setScrollPos(0);
    }

    @Override
    public ITextComponent getChatComponent(int clickX, int clickY) {
        if (GuiNewChatTC.getInstance().getChatOpen()) {
            Point point = scalePoint(new Point(clickX, clickY), mc.currentScreen);
            ILocation actual = getActualLocation();
            // check that cursor is in bounds.
            if (point.x > actual.getXPos() && point.y > actual.getYPos()
                    && point.x < actual.getXPos() + actual.getWidth()
                    && point.y < actual.getYPos() + actual.getHeight()) {


                float scale = getActualScale();
                float size = mc.fontRenderer.FONT_HEIGHT * scale;
                float bottom = (actual.getYPos() + actual.getHeight());

                // The line to get
                int linePos = MathHelper.floor((point.y - bottom) / -size)
                        + (SmoothScrollConfigAccess.chatEnabled() ? MathHelper.floor(nfrUi$displayScroll) : scrollPos);

                // Iterate through the chat component, stopping when the desired
                // x is reached.
                List<Message> list = this.getChat();
                if (linePos >= 0 && linePos < list.size()) {
                    Message chatline = list.get(linePos);
                    float x = actual.getXPos() + 3;

                    for (ITextComponent ichatcomponent : chatline.getMessageWithOptionalTimestamp()) {
                        if (ichatcomponent instanceof TextComponentString) {

                            // get the text of the component, no children.
                            String text = ichatcomponent.getUnformattedComponentText();
                            // clean it up
                            String clean = GuiUtilRenderComponents.removeTextColorsIfConfigured(text, false);
                            // get it's width, then scale it.
                            x += this.mc.fontRenderer.getStringWidth(clean) * scale;

                            if (x > point.x) {
                                return ichatcomponent;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Rectangle getBounds() {
        return getLocation().asRectangle();
    }

}
