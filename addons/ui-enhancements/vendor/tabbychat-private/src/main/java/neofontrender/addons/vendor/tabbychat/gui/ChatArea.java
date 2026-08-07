package neofontrender.addons.vendor.tabbychat.gui;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import neofontrender.addons.api.inline.InlineGlyphHit;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatContextMenu;
import neofontrender.addons.chat.ChatCopyController;
import neofontrender.addons.chat.ChatFadeMath;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatHudWindowController;
import neofontrender.addons.chat.ChatInlineImageInteraction;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatMessageMetadata;
import neofontrender.addons.chat.ChatPlayerLinks;
import neofontrender.addons.chat.ChatSelectionModel;
import neofontrender.addons.chat.ChatSource;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.addons.cjk.ChatTypographyRenderer;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.ChatMessage;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Message;
import neofontrender.addons.vendor.tabbychat.api.gui.ReceivedChat;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.TexturedModal;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent.MouseEvent;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;
import neofontrender.addons.vendor.tabbychat.util.ChatTextUtils;
import neofontrender.addons.vendor.tabbychat.util.ChatVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.IntBuffer;
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
            int scroll = event.getScroll();
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
        int height = visibleHeight(visible);
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
        int height = visibleHeight(visible);
        ChatVisibility vis = TabbyChat.getInstance().settings.advanced.visibility.get();

        return mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN
                && (ChatHudWindowController.isChatExpanded() || vis == ChatVisibility.ALWAYS || height != 0);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        boolean clipped = beginClip();

        int maxScroll = Math.max(0, getChat().size() - lineCapacity());
        nfrUi$displayScroll = SmoothScrollConfigAccess.chatEnabled()
                ? nfrUi$scroller.update(nfrUi$displayScroll, maxScroll) : scrollPos;

        List<Message> visible = getVisibleChat();
        GlState.enableBlend();
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
                        baseRowHeight(), rowFade,
                        ChatStyleConfig.background, ChatStyleConfig.border, opac);
            }
        } else {
            float panelFade = visible.isEmpty() ? 0.0F : getLineFade(visible.get(0));
            GlState.color(1, 1, 1, opac * (chatOpen ? 1.0F : panelFade));
            drawModalCorners(MODAL);
            GlState.color(1, 1, 1, 1);
        }

        zLevel = 100;
        int xPos = 3;
        float fraction = nfrUi$displayScroll - (float) Math.floor(nfrUi$displayScroll);
        int yPos = getBounds().height + Math.round(fraction * scrollStepHeight());
        float messageOffset = ChatAnimationController.messageOffset(getScrollPos() != 0);
        boolean translated = Math.abs(messageOffset) > 0.001F;
        if (translated) {
            GlState.pushMatrix();
            GlState.translate(0.0F, messageOffset, 0.0F);
        }
        drawCopySelection(visible, xPos, yPos);
        for (Message line : visible) {
            yPos -= rowHeight(line);
            drawChatLine(line, xPos, yPos);
        }
        SelectionHit hovered = nfrUi$selectionHit(mouseX, mouseY, false);
        GlyphHover glyphHover = nfrUi$glyphAt(mouseX, mouseY);
        String hoveredPlayer = nfrUi$playerAt(hovered);
        if (hovered != null && hovered.head && hoveredPlayer != null) {
            ILocation actual = getActualLocation();
            float scale = getActualScale();
            ChatPlayerLinks.hoverAvatar(hoveredPlayer,
                    actual.getXPos() + Math.round(mouseX * scale),
                    actual.getYPos() + Math.round(mouseY * scale));
        }
        if (translated) GlState.popMatrix();
        zLevel = 0;
        GlState.disableAlpha();
        GlState.disableBlend();
        if (clipped) endClip();
        ILocation hoverLocation = getActualLocation();
        float hoverScale = getActualScale();
        int glyphX = glyphHover == null ? 0
                : hoverLocation.getXPos() + Math.round(glyphHover.x * hoverScale);
        int glyphY = glyphHover == null ? 0
                : hoverLocation.getYPos() + Math.round(glyphHover.y * hoverScale);
        int glyphWidth = glyphHover == null ? 0
                : Math.max(1, Math.round(glyphHover.width * hoverScale));
        int glyphHeight = glyphHover == null ? 0
                : Math.max(1, Math.round(glyphHover.height * hoverScale));
        ChatInlineImageInteraction.publishTabbyHover(
                glyphHover == null ? null : glyphHover.hit.match().glyph(),
                glyphX, glyphY, glyphWidth, glyphHeight);
    }

    private final IntBuffer oldScissor = BufferUtils.createIntBuffer(4);
    private boolean scissorWasEnabled;

    private boolean beginClip() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) return false;
        ILocation loc = getActualLocation();
        int factor = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight).getScaleFactor();
        int x = Math.max(0, loc.getXPos() * factor);
        int y = Math.max(0, minecraft.displayHeight - (loc.getYPos() + loc.getHeight()) * factor);
        int width = Math.min(minecraft.displayWidth - x, Math.max(0, loc.getWidth() * factor));
        int height = Math.min(minecraft.displayHeight - y, Math.max(0, loc.getHeight() * factor));
        if (width <= 0 || height <= 0) return false;
        scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissorWasEnabled) GL11.glGetInteger(GL11.GL_SCISSOR_BOX, oldScissor);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
        return true;
    }

    private void endClip() {
        if (scissorWasEnabled) {
            GL11.glScissor(oldScissor.get(0), oldScissor.get(1), oldScissor.get(2), oldScissor.get(3));
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void drawChatLine(Message line, int xPos, int yPos) {
        if (isPrivateView()) {
            drawPrivateLine(line, xPos, yPos);
            return;
        }
        IChatComponent display = line.getMessageWithOptionalTimestamp();
        String text = display.getFormattedText();
        InlineTextLayout inline = InlineTextEngine.layout(mc.fontRenderer, text);
        int contentHeight = inline.height();
        int iconY = yPos + Math.max(0, contentHeight - mc.fontRenderer.FONT_HEIGHT);
        float fade = getLineFade(line);
        if (line instanceof ChatMessage) {
            ChatMessage message = (ChatMessage) line;
            if (EnhancedChatFeatures.playerHeads() && message.nfrUi$isFirstFragment()) {
                ChatHeadRenderer.render(message.nfrUi$getSenderName(), xPos, iconY,
                        mc.gameSettings.chatOpacity * fade);
            }
        }
        int color = ChatStyleConfig.enabled
                ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity * fade)
                : Color.WHITE.getHex() & 0x00FFFFFF
                    | ChatFadeMath.lineOpacity(mc.gameSettings.chatOpacity, fade) << 24;
        if (ChatTypographyRenderer.isPositioned(display) && !inline.hasGlyphs()) {
            ChatTypographyRenderer.draw(mc.fontRenderer, display,
                    xPos + ChatHeadRenderer.textOffset(), yPos, color, true);
        } else {
            inline.draw(mc.fontRenderer, xPos + ChatHeadRenderer.textOffset(),
                    yPos, color, true);
        }
        ChatItemIconRenderer.renderLine(display,
                xPos + ChatHeadRenderer.textOffset(), iconY);
    }

    private String lineText(Message line) {
        IChatComponent component = line.getMessageWithOptionalTimestamp();
        ChatMessageMetadata metadata = line instanceof ChatMessage
                ? ((ChatMessage) line).nfrUi$getMessageMetadata() : null;
        if (metadata != null && metadata.source == ChatSource.PRIVATE) {
            if (!metadata.privateBody.isEmpty()) return metadata.privateBody;
            return stripWhisperPrefix(component.getFormattedText());
        }
        return component.getFormattedText();
    }

    private static String stripWhisperPrefix(String text) {
        if (text == null || text.isEmpty()) return text;
        String plain = text.replaceAll("\u00a7.", "");
        String[] patterns = {
                "^你悄悄地对.+?[说:：]\\s*",
                "^.+?悄悄地对你说[:：]\\s*",
                "^.+?whispers to you[:：]\\s*",
                "^you whisper to .+?[:：]\\s*"
        };
        for (String pattern : patterns) {
            String stripped = plain.replaceFirst(pattern, "");
            if (!stripped.equals(plain)) return stripped;
        }
        return text;
    }

    private void drawPrivateLine(Message line, int xPos, int yPos) {
        IChatComponent display = line.getMessageWithOptionalTimestamp();
        String text = lineText(line);
        InlineTextLayout inline = InlineTextEngine.layout(mc.fontRenderer, text);
        int contentHeight = inline.height();
        ChatMessageMetadata metadata = line instanceof ChatMessage
                ? ((ChatMessage) line).nfrUi$getMessageMetadata() : null;
        boolean outgoing = metadata != null && metadata.outgoing;
        float fade = getLineFade(line);
        int avatarSpace = EnhancedChatFeatures.playerHeads() ? ChatHeadRenderer.HEAD_SIZE + 5 : 0;
        int textWidth = ChatTypographyRenderer.isPositioned(display) && !inline.hasGlyphs()
                ? ChatTypographyRenderer.width(mc.fontRenderer, display) : inline.width();
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
                yPos + contentHeight + 4, shadow);
        drawRect(bubbleX, yPos - 1, bubbleX + bubbleWidth,
                yPos + contentHeight + 3, bubble);
        int accent = ((outgoing ? 0x67C89A : 0x8EA1B5) & 0x00FFFFFF) | alpha << 24;
        if (outgoing) {
            drawRect(bubbleX + bubbleWidth - 2, yPos - 1, bubbleX + bubbleWidth,
                    yPos + contentHeight + 3, accent);
        } else {
            drawRect(bubbleX, yPos - 1, bubbleX + 2,
                    yPos + contentHeight + 3, accent);
        }
        int textX = bubbleX + 4;
        int configured = ChatStyleConfig.enabled
                ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity * fade)
                : 0x00FFFFFF | alpha << 24;
        if (ChatTypographyRenderer.isPositioned(display) && !inline.hasGlyphs()) {
            ChatTypographyRenderer.draw(mc.fontRenderer, display,
                    textX, yPos + 1, configured, false);
        } else {
            inline.draw(mc.fontRenderer, textX, yPos + 1, configured, false);
        }
        int iconY = yPos + 1 + Math.max(0, contentHeight - mc.fontRenderer.FONT_HEIGHT);
        ChatItemIconRenderer.renderLine(display, textX, iconY);
        if (line instanceof ChatMessage && ((ChatMessage) line).nfrUi$isFirstFragment()) {
            int headX = outgoing
                    ? getBounds().width - 3 - ChatHeadRenderer.HEAD_SIZE : xPos;
            ChatHeadRenderer.render(((ChatMessage) line).nfrUi$getSenderName(),
                    headX, iconY, mc.gameSettings.chatOpacity * fade);
        }
    }

    public void setChannel(ChatChannel channel) {
        this.channel = channel;
        this.markDirty();
    }

    public int getVisibleLineCapacity() {
        return lineCapacity();
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
        List<Message> visible = Lists.newArrayList();
        int length = 0;
        int pos = SmoothScrollConfigAccess.chatEnabled()
                ? MathHelper.floor_float(nfrUi$displayScroll) : getScrollPos();
        float unfoc = TabbyChat.getInstance().settings.advanced.unfocHeight.get();
        float div = ChatHudWindowController.isChatExpanded() ? 1 : unfoc;
        while (pos < lines.size() && length < super.getLocation().getHeight() * div - 10) {
            Message line = lines.get(pos);
            if (ChatHudWindowController.isChatExpanded()) {
                visible.add(line);
            } else if (getLineOpacity(line) > 3) {
                visible.add(line);
            } else {
                break;
            }
            pos++;
            length += rowHeight(line);
        }
        return visible;
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
        GlyphHover imageHit = nfrUi$glyphAt(event.getMouseX(), event.getMouseY());
        if (event.getType() == MouseEvent.CLICK && event.getButton() == 1 && imageHit != null) {
            ILocation actual = getActualLocation();
            float scale = getActualScale();
            ChatContextMenu.INSTANCE.openImage(imageHit.hit.match().glyph(),
                    actual.getXPos() + Math.round(event.getMouseX() * scale),
                    actual.getYPos() + Math.round(event.getMouseY() * scale));
            return;
        }
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
            SelectionHit hit = interactionHit;
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
        float visualBottom = height + fraction * scrollStepHeight()
                + ChatAnimationController.messageOffset(getScrollPos() != 0);
        int visibleHeight = visibleHeight(visible);
        if (!clamp && (mouseX < 0 || mouseX >= width || mouseY < visualBottom
                - visibleHeight || mouseY >= visualBottom)) return null;
        int row = rowAt(visible, visualBottom, mouseY);
        row = Math.max(0, Math.min(visible.size() - 1, row));
        Message line = visible.get(row);
        String value = messageText(line);
        int localX = Math.max(0, mouseX - textX(line, 3));
        IChatComponent display = line.getMessageWithOptionalTimestamp();
        InlineTextLayout inline = InlineTextEngine.layout(mc.fontRenderer, value);
        int position = ChatTypographyRenderer.isPositioned(display) && !inline.hasGlyphs()
                ? ChatTypographyRenderer.formattedIndexAt(display, localX)
                : inline.sourceIndexAt(mc.fontRenderer, localX);
        int headX = isPrivateView() && isOutgoing(line)
                ? width - 3 - ChatHeadRenderer.HEAD_SIZE : 3;
        boolean head = EnhancedChatFeatures.playerHeads()
                && mouseX >= headX && mouseX < headX + ChatHeadRenderer.HEAD_SIZE
                && line instanceof ChatMessage
                && ((ChatMessage) line).nfrUi$isFirstFragment();
        IChatComponent component = nfrUi$componentAt(line, mouseX);
        int rowTop = Math.round(visualBottom);
        for (int index = 0; index <= row; index++) rowTop -= rowHeight(visible.get(index));
        return new SelectionHit(line, Math.max(0, Math.min(value.length(), position)),
                head, component, rowTop);
    }

    private IChatComponent nfrUi$componentAt(Message line, int mouseX) {
        int x = textX(line, 3);
        if (mouseX < x) return null;
        IChatComponent display = line.getMessageWithOptionalTimestamp();
        InlineTextLayout inline = InlineTextEngine.layout(
                mc.fontRenderer, display.getFormattedText());
        if (ChatTypographyRenderer.isPositioned(display) && !inline.hasGlyphs()) {
            return ChatTypographyRenderer.componentAt(display, mouseX - x);
        }
        for (Object partObject : display) {
            IChatComponent part = (IChatComponent) partObject;
            String text = ChatTextUtils.stripFormattingIfDisabled(part.getUnformattedTextForChat());
            x += InlineTextEngine.width(mc.fontRenderer, text);
            if (x > mouseX) return part;
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
            int height = rowHeight(line);
            y -= height;
            ChatSelectionModel.Range range = ranges.get(line);
            if (range == null || range.start >= range.end) continue;
            String value = messageText(line);
            int textX = textX(line, xPos);
            InlineTextLayout layout = InlineTextEngine.layout(mc.fontRenderer, value);
            IChatComponent display = line.getMessageWithOptionalTimestamp();
            boolean positioned = ChatTypographyRenderer.isPositioned(display)
                    && !layout.hasGlyphs();
            int x1 = textX + Math.round(positioned
                    ? ChatTypographyRenderer.xAtFormattedIndex(display, range.start)
                    : layout.widthTo(mc.fontRenderer, range.start));
            int x2 = textX + Math.round(positioned
                    ? ChatTypographyRenderer.xAtFormattedIndex(display, range.end)
                    : layout.widthTo(mc.fontRenderer, range.end));
            drawRect(x1, y, x2, y + height, ChatCopyController.SELECTION_COLOR);
        }
    }

    private static String messageText(Message line) {
        return line.getMessageWithOptionalTimestamp().getFormattedText();
    }

    private GlyphHover nfrUi$glyphAt(int mouseX, int mouseY) {
        if (!EnhancedChatFeatures.inlineGlyphs()) return null;
        SelectionHit rowHit = nfrUi$selectionHit(mouseX, mouseY, false);
        if (rowHit == null) return null;
        Message line = rowHit.line;
        int textX = textX(line, 3);
        InlineTextLayout layout = InlineTextEngine.layout(mc.fontRenderer, messageText(line));
        InlineGlyphHit hit = layout.glyphAt(mouseX - textX,
                mouseY - rowHit.rowTop, mc.fontRenderer);
        return hit == null ? null : new GlyphHover(hit,
                textX + hit.x(), rowHit.rowTop + hit.y());
    }

    private static final class GlyphHover {
        private final InlineGlyphHit hit;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private GlyphHover(InlineGlyphHit hit, int x, int y) {
            this.hit = hit;
            this.x = x;
            this.y = y;
            this.width = hit.width();
            this.height = hit.height();
        }
    }

    private static final class SelectionHit {
        private final Message line;
        private final int position;
        private final boolean head;
        private final IChatComponent component;
        private final int rowTop;

        private SelectionHit(Message line, int position, boolean head, IChatComponent component,
                             int rowTop) {
            this.line = line;
            this.position = position;
            this.head = head;
            this.component = component;
            this.rowTop = rowTop;
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
        scrollPos = MathHelper.clamp_int(Math.round(nfrUi$scroller.getTarget()), 0, maxScroll);
    }

    @Override
    public void setScrollPos(int scroll) {
        List<Message> list = getChat();
        scroll = Math.min(scroll, list.size() - lineCapacity());
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
        if (ChatHudWindowController.isChatExpanded()) {
            Point point = scalePoint(new Point(clickX, clickY), mc.currentScreen);
            ILocation actual = getActualLocation();
            if (point.x > actual.getXPos() && point.y > actual.getYPos()
                    && point.x < actual.getXPos() + actual.getWidth()
                    && point.y < actual.getYPos() + actual.getHeight()) {
                float scale = getActualScale();
                float bottom = actual.getYPos() + actual.getHeight();
                List<Message> list = this.getChat();
                int start = SmoothScrollConfigAccess.chatEnabled()
                        ? MathHelper.floor_float(nfrUi$displayScroll) : scrollPos;
                float localY = (bottom - point.y) / scale;
                int linePos = start;
                int consumed = 0;
                while (linePos < list.size()) {
                    consumed += rowHeight(list.get(linePos));
                    if (localY < consumed) break;
                    linePos++;
                }
                if (linePos >= 0 && linePos < list.size()) {
                    Message chatline = list.get(linePos);
                    float x = actual.getXPos() + textX(chatline, 3) * scale;
                    if (point.x < x) return null;
                    for (Object partObject : chatline.getMessageWithOptionalTimestamp()) {
                        IChatComponent component = (IChatComponent) partObject;
                        String text = component.getUnformattedTextForChat();
                        String clean = ChatTextUtils.stripFormattingIfDisabled(text);
                        x += InlineTextEngine.width(this.mc.fontRenderer, clean) * scale;
                        if (x > point.x) {
                            return component;
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

    private int baseRowHeight() {
        return mc.fontRenderer.FONT_HEIGHT + (isPrivateView() ? PRIVATE_ROW_GAP : 0);
    }

    private int rowHeight(Message line) {
        int content = EnhancedChatFeatures.inlineGlyphs()
                ? InlineTextEngine.layout(mc.fontRenderer, messageText(line)).height()
                : mc.fontRenderer.FONT_HEIGHT;
        return Math.max(mc.fontRenderer.FONT_HEIGHT, content)
                + (isPrivateView() ? PRIVATE_ROW_GAP : 0);
    }

    private int visibleHeight(List<Message> visible) {
        int height = 0;
        for (Message line : visible) height += rowHeight(line);
        return height;
    }

    private int rowAt(List<Message> visible, float visualBottom, int mouseY) {
        float fromBottom = visualBottom - 1.0F - mouseY;
        int consumed = 0;
        for (int row = 0; row < visible.size(); row++) {
            consumed += rowHeight(visible.get(row));
            if (fromBottom < consumed) return row;
        }
        return visible.size();
    }

    private int scrollStepHeight() {
        List<Message> list = getChat();
        int index = Math.max(0, Math.min(list.size() - 1,
                SmoothScrollConfigAccess.chatEnabled()
                        ? MathHelper.floor_float(nfrUi$displayScroll) : scrollPos));
        return list.isEmpty() ? baseRowHeight() : rowHeight(list.get(index));
    }

    private int lineCapacity() {
        List<Message> list = getChat();
        int available = Math.max(1, super.getLocation().getHeight() - 10);
        int used = 0;
        int count = 0;
        for (int index = Math.max(0, scrollPos); index < list.size(); index++) {
            int height = rowHeight(list.get(index));
            if (count > 0 && used + height > available) break;
            used += height;
            count++;
            if (used >= available) break;
        }
        return Math.max(1, count);
    }

    private boolean isOutgoing(Message line) {
        if (!(line instanceof ChatMessage)) return false;
        ChatMessageMetadata metadata = ((ChatMessage) line).nfrUi$getMessageMetadata();
        return metadata != null && metadata.outgoing;
    }

    private int textX(Message line, int baseX) {
        if (!isPrivateView()) return baseX + ChatHeadRenderer.textOffset();
        int avatarSpace = EnhancedChatFeatures.playerHeads() ? ChatHeadRenderer.HEAD_SIZE + 5 : 0;
        IChatComponent display = line.getMessageWithOptionalTimestamp();
        int bubbleWidth = Math.min(getBounds().width - avatarSpace - 8,
                nfrUi$textWidth(display) + 8);
        int bubbleX = isOutgoing(line)
                ? getBounds().width - 3 - avatarSpace - bubbleWidth
                : baseX + avatarSpace;
        return bubbleX + 4;
    }

    private int nfrUi$textWidth(IChatComponent display) {
        InlineTextLayout inline = InlineTextEngine.layout(
                mc.fontRenderer, display.getFormattedText());
        return ChatTypographyRenderer.isPositioned(display) && !inline.hasGlyphs()
                ? ChatTypographyRenderer.width(mc.fontRenderer, display) : inline.width();
    }
}
