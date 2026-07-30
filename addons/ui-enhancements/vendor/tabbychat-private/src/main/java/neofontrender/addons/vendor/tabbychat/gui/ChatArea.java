package neofontrender.addons.vendor.tabbychat.gui;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatContextMenu;
import neofontrender.addons.chat.ChatCopyController;
import neofontrender.addons.chat.ChatFadeMath;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatSelectionModel;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.ChatMessage;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Message;
import neofontrender.addons.vendor.tabbychat.api.gui.ReceivedChat;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.util.ChatTextUtils;
import neofontrender.addons.vendor.tabbychat.util.ChatVisibility;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.TexturedModal;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent.MouseEvent;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Mouse;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

public class ChatArea extends GuiComponent implements ReceivedChat {

    private static final TexturedModal MODAL = new TexturedModal(ChatBox.GUI_LOCATION, 0, 14, 254, 205);

    private ChatChannel channel;
    private List<Message> messages = Lists.newLinkedList();
    private boolean dirty;
    private int scrollPos = 0;
    private final ChatSelectionModel<Message> nfrUi$selection = new ChatSelectionModel<>();
    private boolean nfrUi$selecting;

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
        nfrUi$selection.clear();
        nfrUi$selecting = false;
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

        List<Message> visible = getVisibleChat();
        GlState.enableBlend();
        float opac = mc.gameSettings.chatOpacity;
        boolean chatOpen = GuiNewChatTC.getInstance().getChatOpen();

        float panelFade = visible.isEmpty() ? 0.0F : getLineFade(visible.get(0));
        GlState.color(1, 1, 1, opac * (chatOpen ? 1.0F : panelFade));
        drawModalCorners(MODAL);
        GlState.color(1, 1, 1, 1);

        zLevel = 100;
        // TODO abstracted padding
        int xPos = getBounds().x + 3;
        int yPos = getBounds().height;
        float messageOffset = ChatAnimationController.messageOffset(getScrollPos() != 0);
        boolean translated = Math.abs(messageOffset) > 0.001F;
        if (translated) {
            GlState.pushMatrix();
            GlState.translate(0.0F, messageOffset, 0.0F);
        }
        drawCopySelection(visible, xPos, yPos);
        for (Message line : visible) {
            yPos -= mc.fontRenderer.FONT_HEIGHT;
            drawChatLine(line, xPos, yPos);
        }
        if (translated) GlState.popMatrix();
        zLevel = 0;
        GlState.disableAlpha();
        GlState.disableBlend();
    }

    private void drawChatLine(Message line, int xPos, int yPos) {
        String text = line.getMessageWithOptionalTimestamp().getFormattedText();
        float fade = getLineFade(line);
        if (line instanceof ChatMessage) {
            ChatMessage message = (ChatMessage) line;
            if (EnhancedChatFeatures.playerHeads() && message.nfrUi$isFirstFragment()) {
                ChatHeadRenderer.render(message.nfrUi$getSenderName(), xPos, yPos,
                        mc.gameSettings.chatOpacity * fade);
            }
        }
        int color = Color.WHITE.getHex() & 0x00FFFFFF
                | ChatFadeMath.lineOpacity(mc.gameSettings.chatOpacity, fade) << 24;
        mc.fontRenderer.drawStringWithShadow(text, xPos + ChatHeadRenderer.textOffset(), yPos, color);
        ChatItemIconRenderer.renderLine(line.getMessageWithOptionalTimestamp(),
                xPos + ChatHeadRenderer.textOffset(), yPos);
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
        this.messages = ChatTextUtils.split(channel.getMessages(),
                getBounds().width - 6 - ChatHeadRenderer.textOffset());
        return this.messages;

    }

    private List<Message> getVisibleChat() {
        List<Message> lines = getChat();

        List<Message> messages = Lists.newArrayList();
        int length = 0;

        int pos = getScrollPos();
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
        return ChatFadeMath.lineOpacity(mc.gameSettings.chatOpacity, getLineFade(line));
    }

    private float getLineFade(Message line) {
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();
        boolean chatOpen = GuiNewChatTC.getInstance().getChatOpen();
        if (chatOpen || vis == ChatVisibility.ALWAYS) return 1.0F;
        if (vis == ChatVisibility.HIDDEN) return 0.0F;
        return ChatFadeMath.lineFade(mc.ingameGUI.getUpdateCounter(), line.getCounter(),
                TabbyChat.getInstance().settings.advanced.fadeTime.get());
    }

    @Subscribe
    public void nfrUi$copySelection(GuiMouseEvent event) {
        if (!EnhancedChatFeatures.copySelection() || !GuiNewChatTC.getInstance().getChatOpen()) return;
        if (event.getType() == MouseEvent.CLICK && event.getButton() == 1) {
            if (nfrUi$selection.hasSelection()) {
                SelectionHit hit = nfrUi$selectionHit(event.getMouseX(), event.getMouseY(), false);
                ChatSelectionModel.Range range = hit == null ? null
                        : nfrUi$selection.ranges(getChat(), ChatArea::messageText).get(hit.line);
                if (range == null || hit.position < range.start || hit.position > range.end) return;
                ILocation actual = getActualLocation();
                float scale = getActualScale();
                ChatContextMenu.INSTANCE.openHistory(
                        nfrUi$selection.selectedText(getChat(), ChatArea::messageText),
                        actual.getXPos() + Math.round(event.getMouseX() * scale),
                        actual.getYPos() + Math.round(event.getMouseY() * scale));
            }
        } else if (event.getType() == MouseEvent.CLICK && event.getButton() == 0) {
            SelectionHit hit = nfrUi$selectionHit(event.getMouseX(), event.getMouseY(), false);
            if (hit != null) {
                ChatContextMenu.INSTANCE.close();
                ChatCopyController.INSTANCE.setSelectedHistoryText("");
                nfrUi$selection.begin(hit.line, hit.position);
                nfrUi$selecting = true;
            }
        } else if (event.getType() == MouseEvent.DRAG && nfrUi$selecting) {
            SelectionHit hit = nfrUi$selectionHit(event.getMouseX(), event.getMouseY(), true);
            if (hit != null) nfrUi$selection.update(hit.line, hit.position);
        } else if (event.getType() == MouseEvent.RAW && event.getButton() == 0
                && !Mouse.getEventButtonState() && nfrUi$selecting) {
            SelectionHit hit = nfrUi$selectionHit(event.getMouseX(), event.getMouseY(), true);
            if (hit != null) nfrUi$selection.update(hit.line, hit.position);
            nfrUi$selecting = false;
            String selectedText = nfrUi$selection.selectedText(getChat(), ChatArea::messageText);
            ChatCopyController.INSTANCE.setSelectedHistoryText(selectedText);
            ChatCopyController.copyToClipboard(selectedText);
        }
    }

    private SelectionHit nfrUi$selectionHit(int mouseX, int mouseY, boolean clamp) {
        List<Message> visible = getVisibleChat();
        if (visible.isEmpty()) return null;
        int width = getBounds().width;
        int height = getBounds().height;
        float visualBottom = height + ChatAnimationController.messageOffset(getScrollPos() != 0);
        if (!clamp && (mouseX < 0 || mouseX >= width || mouseY < visualBottom
                - visible.size() * mc.fontRenderer.FONT_HEIGHT || mouseY >= visualBottom)) return null;
        int row = (int) Math.floor((visualBottom - 1.0F - mouseY) / mc.fontRenderer.FONT_HEIGHT);
        row = Math.max(0, Math.min(visible.size() - 1, row));
        Message line = visible.get(row);
        String value = messageText(line);
        int localX = Math.max(0, mouseX - 3 - ChatHeadRenderer.textOffset());
        int position = mc.fontRenderer.trimStringToWidth(value, localX).length();
        return new SelectionHit(line, Math.max(0, Math.min(value.length(), position)));
    }

    private void drawCopySelection(List<Message> visible, int xPos, int initialY) {
        if (!EnhancedChatFeatures.copySelection() || !nfrUi$selection.hasSelection()) return;
        Map<Message, ChatSelectionModel.Range> ranges =
                nfrUi$selection.ranges(getChat(), ChatArea::messageText);
        int y = initialY;
        for (Message line : visible) {
            y -= mc.fontRenderer.FONT_HEIGHT;
            ChatSelectionModel.Range range = ranges.get(line);
            if (range == null || range.start >= range.end) continue;
            String value = messageText(line);
            int textX = xPos + ChatHeadRenderer.textOffset();
            int x1 = textX + mc.fontRenderer.getStringWidth(value.substring(0, range.start));
            int x2 = textX + mc.fontRenderer.getStringWidth(value.substring(0, range.end));
            drawRect(x1, y, x2, y + mc.fontRenderer.FONT_HEIGHT, ChatCopyController.SELECTION_COLOR);
        }
    }

    private static String messageText(Message line) {
        return line.getMessageWithOptionalTimestamp().getFormattedText();
    }

    private static final class SelectionHit {
        private final Message line;
        private final int position;

        private SelectionHit(Message line, int position) {
            this.line = line;
            this.position = position;
        }
    }

    @Override
    public void scroll(int scr) {
        setScrollPos(getScrollPos() + scr);
    }

    @Override
    public void setScrollPos(int scroll) {
        List<Message> list = getChat();
        scroll = Math.min(scroll, list.size() - GuiNewChatTC.getInstance().getLineCount());
        scroll = Math.max(scroll, 0);

        this.scrollPos = scroll;
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
    public IChatComponent getChatComponent(int clickX, int clickY) {
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
                int linePos = MathHelper.floor_float((point.y - bottom) / -size) + scrollPos;

                // Iterate through the chat component, stopping when the desired
                // x is reached.
                List<Message> list = this.getChat();
                if (linePos >= 0 && linePos < list.size()) {
                    Message chatline = list.get(linePos);
                    float x = actual.getXPos() + (3 + ChatHeadRenderer.textOffset()) * scale;
                    if (point.x < x) return null;

                    for (Object component : chatline.getMessageWithOptionalTimestamp()) {
                        IChatComponent ichatcomponent = (IChatComponent) component;
                        if (ichatcomponent instanceof ChatComponentText) {

                            // get the text of the component, no children.
                            String text = ichatcomponent.getUnformattedTextForChat();
                            // clean it up
                            String clean = ChatTextUtils.stripFormattingIfDisabled(text);
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
