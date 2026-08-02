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
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatHudWindowController;
import neofontrender.addons.chat.ChatFadeMath;
import neofontrender.addons.chat.ChatCopyController;
import neofontrender.addons.chat.ChatContextMenu;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatMessageMetadata;
import neofontrender.addons.chat.ChatPlayerLinks;
import neofontrender.addons.chat.ChatSelectionModel;
import neofontrender.addons.chat.EnhancedChatFeatures;
import mnm.mods.tabbychat.ChatMessage;
import org.lwjgl.input.Mouse;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

public class ChatArea extends GuiComponent implements ReceivedChat {

    private static final TexturedModal MODAL = new TexturedModal(ChatBox.GUI_LOCATION, 0, 14, 254, 205);
    private static final int PRIVATE_ROW_GAP = 5;
    private static final int PRIVATE_INCOMING = 0xD8293038;
    private static final int PRIVATE_OUTGOING = 0xD8326B58;

    private ChatChannel channel;
    private List<Message> messages = Lists.newLinkedList();
    private boolean dirty;
    private int scrollPos = 0;
    private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();
    private float nfrUi$displayScroll;
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
        int height = visible.size() * rowHeight();
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();

        if (ChatHudWindowController.isChatExpanded() || vis == ChatVisibility.ALWAYS) {
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
        int height = visible.size() * rowHeight();
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();

        return mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN
                && (ChatHudWindowController.isChatExpanded() || vis == ChatVisibility.ALWAYS || height != 0);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {

        int maxScroll = Math.max(0, getChat().size() - lineCapacity());
        nfrUi$displayScroll = SmoothScrollConfigAccess.chatEnabled()
                ? nfrUi$scroller.update(nfrUi$displayScroll, maxScroll) : scrollPos;

        List<Message> visible = getVisibleChat();
        GlStateManager.enableBlend();
        float opac = mc.gameSettings.chatOpacity;
        boolean chatOpen = ChatHudWindowController.isChatExpanded();

        if (ChatStyleConfig.enabled) {
            if (chatOpen || TabbyChat.getInstance().settings.advanced.visibility.get() == ChatVisibility.ALWAYS) {
                ChatStyleRenderer.panel(getBounds().width, getBounds().height,
                        ChatStyleConfig.background, ChatStyleConfig.border, opac);
            } else {
                float[] rowFade = new float[visible.size()];
                for (int i = 0; i < visible.size(); i++) rowFade[i] = getLineFade(visible.get(i));
                ChatStyleRenderer.fadingPanel(getBounds().width, getBounds().height,
                        rowHeight(), rowFade,
                        ChatStyleConfig.background, ChatStyleConfig.border, opac);
            }
        } else {
            float panelFade = visible.isEmpty() ? 0.0F : getLineFade(visible.get(0));
            GlStateManager.color(1, 1, 1, opac * (chatOpen ? 1.0F : panelFade));
            drawModalCorners(MODAL);
            GlStateManager.color(1, 1, 1, 1);
        }

        zLevel = 100;
        // TODO abstracted padding
        int xPos = getBounds().x + 3;
        float fraction = nfrUi$displayScroll - (float) Math.floor(nfrUi$displayScroll);
        int yPos = getBounds().height + Math.round(fraction * rowHeight());
        float messageOffset = ChatAnimationController.messageOffset(getScrollPos() != 0);
        boolean translated = Math.abs(messageOffset) > 0.001F;
        if (translated) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, messageOffset, 0.0F);
        }
        drawCopySelection(visible, xPos, yPos);
        for (Message line : visible) {
            yPos -= rowHeight();
            drawChatLine(line, xPos, yPos);
        }
        SelectionHit hovered = nfrUi$selectionHit(mouseX, mouseY, false);
        String hoveredPlayer = nfrUi$playerAt(hovered);
        if (hovered != null && hovered.head && hoveredPlayer != null) {
            ILocation actual = getActualLocation();
            float scale = getActualScale();
            ChatPlayerLinks.hoverAvatar(hoveredPlayer,
                    actual.getXPos() + Math.round(mouseX * scale),
                    actual.getYPos() + Math.round(mouseY * scale));
        }
        if (translated) GlStateManager.popMatrix();
        zLevel = 0;
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    private void drawChatLine(Message line, int xPos, int yPos) {
        if (isPrivateView()) {
            drawPrivateLine(line, xPos, yPos);
            return;
        }
        String text = line.getMessageWithOptionalTimestamp().getFormattedText();
        float fade = getLineFade(line);
        if (line instanceof ChatMessage) {
            ChatMessage message = (ChatMessage) line;
            if (EnhancedChatFeatures.playerHeads() && message.nfrUi$isFirstFragment()) {
                ChatHeadRenderer.render(message.nfrUi$getSenderId(), xPos, yPos,
                        mc.gameSettings.chatOpacity * fade);
            }
        }
        int configured = ChatStyleConfig.enabled
                ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity * fade)
                : Color.WHITE.getHex() & 0x00FFFFFF
                    | ChatFadeMath.lineOpacity(mc.gameSettings.chatOpacity, fade) << 24;
        mc.fontRenderer.drawStringWithShadow(text, xPos + ChatHeadRenderer.textOffset(), yPos, configured);
        ChatItemIconRenderer.renderLine(line.getMessageWithOptionalTimestamp(),
                xPos + ChatHeadRenderer.textOffset(), yPos);
    }

    private void drawPrivateLine(Message line, int xPos, int yPos) {
        String text = line.getMessageWithOptionalTimestamp().getFormattedText();
        ChatMessageMetadata metadata = line instanceof ChatMessage
                ? ((ChatMessage) line).nfrUi$getMessageMetadata() : null;
        boolean outgoing = metadata != null && metadata.outgoing;
        float fade = getLineFade(line);
        int avatarSpace = EnhancedChatFeatures.playerHeads() ? ChatHeadRenderer.HEAD_SIZE + 5 : 0;
        int textWidth = mc.fontRenderer.getStringWidth(text);
        int bubbleWidth = Math.min(getBounds().width - avatarSpace - 8, textWidth + 8);
        int bubbleX = outgoing
                ? getBounds().width - 3 - avatarSpace - bubbleWidth
                : xPos + avatarSpace;
        int alpha = Math.max(0, Math.min(255,
                Math.round(255.0F * mc.gameSettings.chatOpacity * fade)));
        int bubble = ((outgoing ? PRIVATE_OUTGOING : PRIVATE_INCOMING) & 0x00FFFFFF)
                | alpha << 24;
        int shadow = 0x000000 | Math.min(alpha, 0x38) << 24;
        drawRect(bubbleX + 1, yPos, bubbleX + bubbleWidth + 1,
                yPos + mc.fontRenderer.FONT_HEIGHT + 4, shadow);
        drawRect(bubbleX, yPos - 1, bubbleX + bubbleWidth,
                yPos + mc.fontRenderer.FONT_HEIGHT + 3, bubble);
        int accent = ((outgoing ? 0x67C89A : 0x8EA1B5) & 0x00FFFFFF) | alpha << 24;
        if (outgoing) {
            drawRect(bubbleX + bubbleWidth - 2, yPos - 1, bubbleX + bubbleWidth,
                    yPos + mc.fontRenderer.FONT_HEIGHT + 3, accent);
        } else {
            drawRect(bubbleX, yPos - 1, bubbleX + 2,
                    yPos + mc.fontRenderer.FONT_HEIGHT + 3, accent);
        }
        int textX = bubbleX + 4;
        int configured = ChatStyleConfig.enabled
                ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity * fade)
                : 0x00FFFFFF | alpha << 24;
        mc.fontRenderer.drawString(text, textX, yPos + 1, configured, false);
        ChatItemIconRenderer.renderLine(line.getMessageWithOptionalTimestamp(), textX, yPos + 1);
        if (line instanceof ChatMessage && ((ChatMessage) line).nfrUi$isFirstFragment()) {
            int headX = outgoing
                    ? getBounds().width - 3 - ChatHeadRenderer.HEAD_SIZE : xPos;
            ChatHeadRenderer.render(((ChatMessage) line).nfrUi$getSenderId(),
                    headX, yPos + 1, mc.gameSettings.chatOpacity * fade);
        }
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
        int width = isPrivateView()
                ? Math.max(16, (getBounds().width - 20) * 3 / 4)
                : getBounds().width - 6 - ChatHeadRenderer.textOffset();
        this.messages = ChatTextUtils.split(channel.getMessages(), width, isPrivateView());
        return this.messages;

    }

    private List<Message> getVisibleChat() {
        List<Message> lines = getChat();

        List<Message> messages = Lists.newArrayList();
        int length = 0;

        int pos = SmoothScrollConfigAccess.chatEnabled()
                ? MathHelper.floor(nfrUi$displayScroll) : getScrollPos();
        float unfoc = TabbyChat.getInstance().settings.advanced.unfocHeight.get();
        float div = ChatHudWindowController.isChatExpanded() ? 1 : unfoc;
        while (pos < lines.size() && length < super.getLocation().getHeight() * div - 10) {
            Message line = lines.get(pos);

            if (ChatHudWindowController.isChatExpanded()) {
                messages.add(line);
            } else if (getLineOpacity(line) > 3) {
                messages.add(line);
            } else {
                break;
            }

            pos++;
            length += rowHeight();
        }

        return messages;
    }

    private int getLineOpacity(Message line) {
        return ChatFadeMath.lineOpacity(mc.gameSettings.chatOpacity, getLineFade(line));
    }

    private float getLineFade(Message line) {
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();
        boolean chatOpen = ChatHudWindowController.isChatExpanded();
        if (chatOpen || vis == ChatVisibility.ALWAYS) return 1.0F;
        if (vis == ChatVisibility.HIDDEN) return 0.0F;
        return ChatFadeMath.lineFade(mc.ingameGUI.getUpdateCounter(), line.getCounter(),
                TabbyChat.getInstance().settings.advanced.fadeTime.get());
    }

    @Subscribe
    public void nfrUi$copySelection(GuiMouseEvent event) {
        if (!ChatHudWindowController.isChatExpanded()) return;
        SelectionHit interactionHit = nfrUi$selectionHit(
                event.getMouseX(), event.getMouseY(), false);
        String interactionPlayer = nfrUi$playerAt(interactionHit);
        if (event.getType() == MouseEvent.CLICK && event.getButton() == 1
                && interactionPlayer != null) {
            ILocation actual = getActualLocation();
            float scale = getActualScale();
            ChatContextMenu.INSTANCE.openPlayer(interactionPlayer,
                    actual.getXPos() + Math.round(event.getMouseX() * scale),
                    actual.getYPos() + Math.round(event.getMouseY() * scale));
            return;
        }
        if (event.getType() == MouseEvent.CLICK && event.getButton() == 0
                && interactionHit != null && interactionHit.head && interactionPlayer != null
                && ChatPlayerLinks.activate(interactionPlayer)) {
            nfrUi$selecting = false;
            return;
        }
        if (!EnhancedChatFeatures.copySelection()) return;
        if (event.getType() == MouseEvent.CLICK && event.getButton() == 1) {
            if (nfrUi$selection.hasSelection()) {
                SelectionHit hit = interactionHit;
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
        float fraction = nfrUi$displayScroll - (float) Math.floor(nfrUi$displayScroll);
        float visualBottom = height + fraction * rowHeight()
                + ChatAnimationController.messageOffset(getScrollPos() != 0);
        if (!clamp && (mouseX < 0 || mouseX >= width || mouseY < visualBottom
                - visible.size() * rowHeight() || mouseY >= visualBottom)) return null;
        int row = (int) Math.floor((visualBottom - 1.0F - mouseY) / rowHeight());
        row = Math.max(0, Math.min(visible.size() - 1, row));
        Message line = visible.get(row);
        String value = messageText(line);
        int localX = Math.max(0, mouseX - textX(line, 3));
        int position = mc.fontRenderer.trimStringToWidth(value, localX).length();
        int headX = isPrivateView() && isOutgoing(line)
                ? width - 3 - ChatHeadRenderer.HEAD_SIZE : 3;
        boolean head = EnhancedChatFeatures.playerHeads()
                && mouseX >= headX && mouseX < headX + ChatHeadRenderer.HEAD_SIZE
                && line instanceof ChatMessage
                && ((ChatMessage) line).nfrUi$isFirstFragment();
        ITextComponent component = nfrUi$componentAt(line, mouseX);
        return new SelectionHit(line, Math.max(0, Math.min(value.length(), position)),
                head, component);
    }

    private ITextComponent nfrUi$componentAt(Message line, int mouseX) {
        int x = textX(line, 3);
        if (mouseX < x) return null;
        for (ITextComponent component : line.getMessageWithOptionalTimestamp()) {
            String text = GuiUtilRenderComponents.removeTextColorsIfConfigured(
                    component.getUnformattedComponentText(), false);
            x += mc.fontRenderer.getStringWidth(text);
            if (x > mouseX) return component;
        }
        return null;
    }

    private String nfrUi$playerAt(SelectionHit hit) {
        if (hit == null) return null;
        String linked = ChatPlayerLinks.playerFrom(hit.component);
        if (linked != null) return linked;
        if (hit.head && hit.line instanceof ChatMessage) {
            ChatMessageMetadata metadata = ((ChatMessage) hit.line).nfrUi$getMessageMetadata();
            if (metadata != null && !metadata.playerName.isEmpty()) return metadata.playerName;
        }
        return null;
    }

    private void drawCopySelection(List<Message> visible, int xPos, int initialY) {
        if (!EnhancedChatFeatures.copySelection() || !nfrUi$selection.hasSelection()) return;
        Map<Message, ChatSelectionModel.Range> ranges =
                nfrUi$selection.ranges(getChat(), ChatArea::messageText);
        int y = initialY;
        for (Message line : visible) {
            y -= rowHeight();
            ChatSelectionModel.Range range = ranges.get(line);
            if (range == null || range.start >= range.end) continue;
            String value = messageText(line);
            int textX = textX(line, xPos);
            int x1 = textX + mc.fontRenderer.getStringWidth(value.substring(0, range.start));
            int x2 = textX + mc.fontRenderer.getStringWidth(value.substring(0, range.end));
            drawRect(x1, y, x2, y + rowHeight(), ChatCopyController.SELECTION_COLOR);
        }
    }

    private static String messageText(Message line) {
        return line.getMessageWithOptionalTimestamp().getFormattedText();
    }

    private static final class SelectionHit {
        private final Message line;
        private final int position;
        private final boolean head;
        private final ITextComponent component;

        private SelectionHit(Message line, int position, boolean head, ITextComponent component) {
            this.line = line;
            this.position = position;
            this.head = head;
            this.component = component;
        }
    }

    @Override
    public void scroll(int scr) {
        if (!SmoothScrollConfigAccess.chatEnabled()) {
            setScrollPos(getScrollPos() + scr);
            return;
        }
        int maxScroll = Math.max(0, getChat().size() - lineCapacity());
        nfrUi$scroller.scrollBy(scr, maxScroll, nfrUi$displayScroll);
        scrollPos = MathHelper.clamp(Math.round(nfrUi$scroller.getTarget()), 0, maxScroll);
    }

    @Override
    public void setScrollPos(int scroll) {
        List<Message> list = getChat();
        scroll = Math.min(scroll, list.size() - lineCapacity());
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
        if (ChatHudWindowController.isChatExpanded()) {
            Point point = scalePoint(new Point(clickX, clickY), mc.currentScreen);
            ILocation actual = getActualLocation();
            // check that cursor is in bounds.
            if (point.x > actual.getXPos() && point.y > actual.getYPos()
                    && point.x < actual.getXPos() + actual.getWidth()
                    && point.y < actual.getYPos() + actual.getHeight()) {


                float scale = getActualScale();
                float size = rowHeight() * scale;
                float bottom = (actual.getYPos() + actual.getHeight());

                // The line to get
                int linePos = MathHelper.floor((point.y - bottom) / -size)
                        + (SmoothScrollConfigAccess.chatEnabled() ? MathHelper.floor(nfrUi$displayScroll) : scrollPos);

                // Iterate through the chat component, stopping when the desired
                // x is reached.
                List<Message> list = this.getChat();
                if (linePos >= 0 && linePos < list.size()) {
                    Message chatline = list.get(linePos);
                    float x = actual.getXPos() + textX(chatline, 3) * scale;
                    if (point.x < x) return null;

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

    private boolean isPrivateView() {
        return channel != null && channel.isPm();
    }

    private int rowHeight() {
        return mc.fontRenderer.FONT_HEIGHT + (isPrivateView() ? PRIVATE_ROW_GAP : 0);
    }

    private int lineCapacity() {
        return Math.max(1, (getBounds().height - 10) / rowHeight());
    }

    public int getVisibleLineCapacity() {
        return lineCapacity();
    }

    private boolean isOutgoing(Message line) {
        if (!(line instanceof ChatMessage)) return false;
        ChatMessageMetadata metadata = ((ChatMessage) line).nfrUi$getMessageMetadata();
        return metadata != null && metadata.outgoing;
    }

    private int textX(Message line, int baseX) {
        if (!isPrivateView()) return baseX + ChatHeadRenderer.textOffset();
        int avatarSpace = EnhancedChatFeatures.playerHeads() ? ChatHeadRenderer.HEAD_SIZE + 5 : 0;
        int bubbleWidth = Math.min(getBounds().width - avatarSpace - 8,
                mc.fontRenderer.getStringWidth(messageText(line)) + 8);
        int bubbleX = isOutgoing(line)
                ? getBounds().width - 3 - avatarSpace - bubbleWidth
                : baseX + avatarSpace;
        return bubbleX + 4;
    }

}
